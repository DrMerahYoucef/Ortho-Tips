package com.example.data.extensions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Installed extensions, one folder each under `filesDir/extensions/<id>/`:
 *
 *     index.html   the program, exactly as imported
 *
 * Their saved data lives separately in [ExtensionStorage]. Nothing here touches
 * the patient database, and the drive sync never sees this folder (it only
 * syncs `patients/`).
 */
class ExtensionRepository(private val context: Context) {

    sealed interface ImportResult {
        data class Installed(val extension: OrthoExtension, val previousVersion: String?) : ImportResult
        data object Unreadable : ImportResult
        data object NotHtml : ImportResult
        data object TooLarge : ImportResult
    }

    private val root: File get() = File(context.filesDir, DIR_NAME)
    private val mutex = Mutex()

    private val _extensions = MutableStateFlow<List<OrthoExtension>>(emptyList())
    val extensions: StateFlow<List<OrthoExtension>> = _extensions.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { _extensions.value = scan() }
    }

    suspend fun get(id: String): OrthoExtension? = withContext(Dispatchers.IO) {
        htmlFile(id)?.takeIf { it.isFile }?.let { describe(id, it) }
    }

    /**
     * The page as the runner loads it: the extension's HTML with the host bridge
     * placed before any of its own scripts.
     */
    suspend fun loadPage(id: String): String? = withContext(Dispatchers.IO) {
        val file = htmlFile(id)?.takeIf { it.isFile } ?: return@withContext null
        val html = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return@withContext null
        val bridge = context.assets.open(BRIDGE_ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        injectBridge(html, bridge)
    }

    /**
     * Installs the HTML file at [uri]. A file whose id is already installed
     * replaces it; its saved data is kept, which is how an extension is updated.
     */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { readAtMost(it, MAX_FILE_BYTES + 1) }
        } catch (e: Exception) {
            null
        } ?: return@withContext ImportResult.Unreadable
        if (bytes.size > MAX_FILE_BYTES) return@withContext ImportResult.TooLarge

        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        if (!LOOKS_LIKE_HTML.containsMatchIn(text.take(HTML_SNIFF_CHARS))) {
            return@withContext ImportResult.NotHtml
        }

        val meta = ExtensionMetadata.parse(text)
        val id = meta.id
            ?: meta.name?.let(ExtensionMetadata::slug)?.takeIf { it.isNotEmpty() }
            ?: displayName(uri)?.substringBeforeLast('.')?.let(ExtensionMetadata::slug)?.takeIf { it.isNotEmpty() }
            ?: ("extension-" + Integer.toHexString(text.hashCode()).trimStart('-'))

        mutex.withLock {
            try {
                val dir = File(root, id).apply { mkdirs() }
                val target = File(dir, HTML_NAME)
                val previousVersion = target.takeIf { it.isFile }?.let { describe(id, it).version }
                val staging = File(dir, "$HTML_NAME.tmp")
                staging.writeText(text, Charsets.UTF_8)
                if (!staging.renameTo(target)) {
                    target.delete()
                    if (!staging.renameTo(target)) {
                        staging.delete()
                        return@withLock ImportResult.Unreadable
                    }
                }
                _extensions.value = scan()
                ImportResult.Installed(describe(id, target), previousVersion)
            } catch (e: Exception) {
                ImportResult.Unreadable
            }
        }
    }

    /** Removes the extension and everything it saved. */
    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                extensionDir(id)?.deleteRecursively()
                _extensions.value = scan()
            }
        }
        clearData(id)
    }

    /** Erases what the extension saved; the program itself stays installed. */
    suspend fun clearData(id: String) {
        if (!ExtensionMetadata.isValidId(id)) return
        withContext(Dispatchers.IO) { ExtensionStorage.clear(context, id) }
        // Anything that bypassed the bridge (IndexedDB, a refused localStorage swap).
        withContext(Dispatchers.Main) {
            runCatching { WebStorage.getInstance().deleteOrigin(origin(id)) }
        }
    }

    /** Copies the installed file to [destination], for sharing or keeping a copy. */
    suspend fun export(id: String, destination: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = htmlFile(id)?.takeIf { it.isFile } ?: return@withContext false
        runCatching {
            context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }

    // ---- internals ---------------------------------------------------------

    private fun extensionDir(id: String): File? =
        if (ExtensionMetadata.isValidId(id)) File(root, id) else null

    private fun htmlFile(id: String): File? = extensionDir(id)?.let { File(it, HTML_NAME) }

    private fun scan(): List<OrthoExtension> =
        root.listFiles()
            ?.filter { it.isDirectory && ExtensionMetadata.isValidId(it.name) }
            ?.mapNotNull { dir -> File(dir, HTML_NAME).takeIf { it.isFile }?.let { describe(dir.name, it) } }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    private fun describe(id: String, file: File): OrthoExtension {
        val meta = ExtensionMetadata.parse(readPrefix(file, METADATA_CHARS))
        return OrthoExtension(
            id = id,
            name = meta.name ?: id,
            description = meta.description.orEmpty(),
            version = meta.version ?: "1.0",
            icon = meta.icon ?: DEFAULT_ICON,
            author = meta.author.orEmpty(),
            allowNetwork = meta.allowNetwork,
            sizeBytes = file.length(),
            updatedAt = file.lastModified()
        )
    }

    private fun readPrefix(file: File, maxChars: Int): String =
        file.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(maxChars)
            var total = 0
            while (total < maxChars) {
                val n = reader.read(buffer, total, maxChars - total)
                if (n < 0) break
                total += n
            }
            String(buffer, 0, total)
        }

    private fun readAtMost(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (total < limit) {
            val n = input.read(buffer, 0, minOf(buffer.size, limit - total))
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
        }
        return out.toByteArray()
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment

    companion object {
        const val DEFAULT_ICON = "🧩"
        const val MAX_FILE_BYTES = 15 * 1024 * 1024

        private const val DIR_NAME = "extensions"
        private const val HTML_NAME = "index.html"
        private const val BRIDGE_ASSET = "extension_host/ortho-bridge.js"
        private const val METADATA_CHARS = 256 * 1024
        private const val HTML_SNIFF_CHARS = 200_000

        private val LOOKS_LIKE_HTML = Regex(
            "<\\s*(!doctype\\s+html|html|head|body|script|style|div|main|section|svg)\\b",
            RegexOption.IGNORE_CASE
        )
        private val HEAD_OPEN = Regex("<head\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val HTML_OPEN = Regex("<html\\b[^>]*>", RegexOption.IGNORE_CASE)

        /**
         * Each extension gets its own origin, so whatever the page keeps outside
         * the bridge stays separate from other extensions. `.invalid` never
         * resolves, and network loads are blocked unless the file allows them.
         */
        fun origin(id: String): String = "https://$id.ext.orthotips.invalid"

        fun baseUrl(id: String): String = origin(id) + "/"

        internal fun injectBridge(html: String, bridgeJs: String): String {
            val tag = "<script>\n$bridgeJs\n</script>\n"
            val anchor = HEAD_OPEN.find(html) ?: HTML_OPEN.find(html)
                ?: return tag + html
            val at = anchor.range.last + 1
            return html.substring(0, at) + tag + html.substring(at)
        }
    }
}
