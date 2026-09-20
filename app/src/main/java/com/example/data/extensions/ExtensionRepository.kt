package com.example.data.extensions

import android.content.Context
import com.example.data.crypto.CryptoKeys
import com.example.data.crypto.StreamCrypto
import com.example.data.crypto.WrappedKeyRepository
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/** Metadata and storage for the private, encrypted .OTips extension format. */
data class OrthoExtension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val icon: String,
    val file: File
)

class ExtensionRepository(
    context: Context,
    private val crypto: StreamCrypto,
    private val keys: WrappedKeyRepository
) {
    private val directory = File(context.filesDir, "extensions")

    fun list(): List<OrthoExtension> = directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("OTips", ignoreCase = true) }
        ?.mapNotNull { file -> runCatching { metadata(file, decrypt(file.readBytes())) }.getOrNull() }
        ?.sortedBy { it.name.lowercase() }
        ?: emptyList()

    fun read(id: String): String {
        val file = fileFor(id)
        require(file.exists()) { "Extension not found" }
        return decrypt(file.readBytes()).toString(StandardCharsets.UTF_8)
    }

    /** Imports only an app-encrypted .OTips file and replaces the same id atomically. */
    fun importEncrypted(bytes: ByteArray): OrthoExtension {
        require(bytes.size <= MAX_FILE_BYTES) { "Extension too large" }
        val html = decrypt(bytes).toString(StandardCharsets.UTF_8)
        val parsed = parseMetadata(html)
        require(parsed.id.isNotBlank()) { "Missing extension id" }
        require(HTML_MARKER.containsMatchIn(html)) { "Not an HTML extension" }
        val destination = fileFor(parsed.id)
        directory.mkdirs()
        val temporary = File(directory, ".${parsed.id}.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IllegalStateException("Could not install extension")
        }
        return metadata(destination, html)
    }

    fun encryptedBytes(id: String): ByteArray = fileFor(id).readBytes()

    fun delete(id: String) {
        fileFor(id).delete()
    }

    fun encryptHtml(html: String): ByteArray {
        val metadata = parseMetadata(html)
        require(HTML_MARKER.containsMatchIn(html)) { "Not an HTML extension" }
        require(metadata.id.isNotBlank()) { "Missing extension id" }
        val plaintext = html.toByteArray(StandardCharsets.UTF_8)
        require(plaintext.size <= MAX_FILE_BYTES) { "Extension too large" }
        return FILE_MAGIC + crypto.encryptBytes(keys.getOrCreate(CryptoKeys.EXTENSIONS), plaintext)
    }

    private fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size > FILE_MAGIC.size && bytes.copyOf(FILE_MAGIC.size).contentEquals(FILE_MAGIC)) {
            "Not an Ortho Tips extension"
        }
        return crypto.decryptBytes(keys.getOrCreate(CryptoKeys.EXTENSIONS), bytes.copyOfRange(FILE_MAGIC.size, bytes.size))
    }

    private fun metadata(file: File, html: String): OrthoExtension {
        val parsed = parseMetadata(html)
        return parsed.copy(file = file)
    }

    private fun parseMetadata(html: String): OrthoExtension = OrthoExtension(
        id = meta(html, "ortho-ext-id"),
        name = meta(html, "ortho-ext-name").ifBlank { "Extension" },
        description = meta(html, "ortho-ext-description"),
        version = meta(html, "ortho-ext-version").ifBlank { "1.0" },
        icon = meta(html, "ortho-ext-icon").ifBlank { "🧩" },
        file = File("")
    )

    private fun meta(html: String, name: String): String {
        val pattern = Pattern.compile(
            "<meta\\s+[^>]*name=\\\"$name\\\"[^>]*content=\\\"([^\\\"]*)\\\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        return pattern.matcher(html).let { if (it.find()) it.group(1).orEmpty().trim() else "" }
    }

    private fun fileFor(id: String): File {
        require(ID_PATTERN.matches(id)) { "Invalid extension id" }
        return File(directory, "$id.OTips")
    }

    companion object {
        private val FILE_MAGIC = byteArrayOf(0x4F, 0x54, 0x49, 0x50, 0x53, 0x31)
        private val ID_PATTERN = Regex("[a-z0-9-]{1,48}")
        private val HTML_MARKER = Regex("<html\\b", RegexOption.IGNORE_CASE)
        const val MAX_FILE_BYTES = 15 * 1024 * 1024
    }
}
