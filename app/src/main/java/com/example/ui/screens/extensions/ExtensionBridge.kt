package com.example.ui.screens.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.activity.result.contract.ActivityResultContract
import com.example.data.extensions.ExtensionDatabaseAccess
import com.example.data.extensions.ExtensionFiles
import com.example.data.extensions.ExtensionManifest
import com.example.data.extensions.ExtensionPermission
import com.example.data.extensions.ExtensionStorage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The object behind `window.OrthoTipsNative`. The page never calls it directly:
 * `assets/extension_host/ortho-bridge.js` wraps it into `window.OrthoTips`.
 *
 * Permissions are enforced here, on every call (extension guide, section 4.4):
 * the JavaScript side checks too, for a quick and clear rejection, but a page
 * that bypasses it still gets nothing it was not granted. Anything not listed
 * in [call] is refused, so a new capability is denied until it is wired here.
 *
 * WebView calls these methods on its own JavaBridge thread; slow work goes to a
 * private background thread, and anything touching the UI is posted to main.
 */
internal class ExtensionBridge(
    private val storage: ExtensionStorage,
    private val granted: Set<ExtensionPermission>,
    private val files: ExtensionFiles?,
    private val contentDir: File?,
    private val database: ExtensionDatabaseAccess,
    private val infoJson: () -> String,
    private val onToast: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onSaveFile: (requestId: String, fileName: String, mimeType: String, bytes: ByteArray?) -> Unit,
    /** Settles a promise from [call]: `value` is a JSON literal, or an error message when `ok` is false. */
    private val onSettle: (requestId: String, ok: Boolean, value: String) -> Unit,
    /** A refused call, shown in the extension's console. */
    private val onDenied: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()

    fun dispose() {
        worker.shutdownNow()
    }

    @JavascriptInterface
    fun info(): String = infoJson()

    // ---- key/value storage: no permission needed ---------------------------------

    @JavascriptInterface
    fun storageGet(key: String?): String? = key?.let(storage::get)

    @JavascriptInterface
    fun storageSet(key: String?, value: String?): Boolean =
        if (key == null || value == null) false else storage.set(key, value)

    @JavascriptInterface
    fun storageDelete(key: String?) {
        key?.let(storage::delete)
    }

    @JavascriptInterface
    fun storageKeys(prefix: String?): String = JSONArray(storage.keys(prefix.orEmpty())).toString()

    // ---- misc ------------------------------------------------------------------

    @JavascriptInterface
    fun toast(message: String?) {
        val text = message?.take(300)?.takeIf { it.isNotBlank() } ?: return
        main.post { onToast(text) }
    }

    @JavascriptInterface
    fun close() {
        main.post { onClose() }
    }

    /** `OrthoTips.saveFile`: needs `import_export`. Resolves false when refused. */
    @JavascriptInterface
    fun saveFile(requestId: String?, fileName: String?, mimeType: String?, base64: String?) {
        val id = requestId ?: return
        if (!allowed(ExtensionPermission.IMPORT_EXPORT)) {
            main.post { onSaveFile(id, "", "", null) }
            return
        }
        val bytes = runCatching { Base64.decode(base64.orEmpty(), Base64.DEFAULT) }.getOrNull()
        val name = cleanFileName(fileName)
        val mime = mimeType?.trim()?.takeIf { it.contains('/') } ?: "application/octet-stream"
        main.post { onSaveFile(id, name, mime, bytes) }
    }

    // ---- gated, asynchronous capabilities ----------------------------------------

    /**
     * One entry point for every promise-based capability. [method] picks the
     * capability, [argsJson] carries its arguments; the answer arrives through
     * [onSettle] under [requestId].
     */
    @JavascriptInterface
    fun call(requestId: String?, method: String?, argsJson: String?) {
        val id = requestId ?: return
        val name = method.orEmpty()
        val required = REQUIRED[name]
        if (required == null) {
            settle(id, false, "Unknown method '$name'")
            return
        }
        if (!allowed(required)) {
            settle(id, false, "PermissionError: '${required.key}' not granted")
            return
        }
        worker.execute {
            val result = runCatching {
                val args = if (argsJson.isNullOrBlank()) JSONObject() else JSONObject(argsJson)
                dispatch(name, args)
            }
            result.fold(
                onSuccess = { settle(id, true, it) },
                onFailure = { settle(id, false, it.message ?: it.javaClass.simpleName) }
            )
        }
    }

    /** Returns a JSON literal. */
    private fun dispatch(method: String, args: JSONObject): String = when (method) {
        "files.write" -> {
            val bytes = Base64.decode(args.optString("base64"), Base64.DEFAULT)
            requireFiles().write(args.getString("path"), bytes)
            "true"
        }
        "files.read" -> requireFiles().read(args.getString("path"))?.let(::base64Literal) ?: "null"
        "files.list" -> JSONArray(requireFiles().list(args.optString("prefix"))).toString()
        "files.delete" -> requireFiles().delete(args.getString("path")).toString()
        "assets.read" -> readAsset(args.getString("path"))?.let(::base64Literal) ?: "null"
        "database.query" -> database.query(args).toString()
        else -> throw IllegalArgumentException("Unknown method '$method'")
    }

    private fun readAsset(path: String): ByteArray? {
        val root = contentDir ?: return null
        val clean = path.trim().trimStart('/')
        require(ExtensionManifest.isSafeRelativePath(clean)) { "Invalid path '$path'" }
        return File(root, clean).takeIf { it.isFile }?.readBytes()
    }

    private fun requireFiles(): ExtensionFiles = files ?: throw IllegalStateException("Storage is unavailable")

    private fun allowed(permission: ExtensionPermission): Boolean {
        if (permission in granted && permission.implemented) return true
        val reason = if (permission.implemented) "not granted" else "not available yet"
        main.post { onDenied("PermissionError: '${permission.key}' $reason") }
        return false
    }

    private fun settle(id: String, ok: Boolean, value: String) {
        main.post { onSettle(id, ok, value) }
    }

    private fun base64Literal(bytes: ByteArray): String =
        JSONObject.quote(Base64.encodeToString(bytes, Base64.NO_WRAP))

    private fun cleanFileName(name: String?): String =
        name.orEmpty()
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim()
            .take(120)
            .ifEmpty { "fichier" }

    companion object {
        /** Bridge API version exposed as `OrthoTips.bridgeVersion`. */
        const val BRIDGE_VERSION = 2

        /** Method → permission it needs. Anything absent is refused. */
        private val REQUIRED = mapOf(
            "files.write" to ExtensionPermission.STORAGE,
            "files.read" to ExtensionPermission.STORAGE,
            "files.list" to ExtensionPermission.STORAGE,
            "files.delete" to ExtensionPermission.STORAGE,
            "assets.read" to ExtensionPermission.ASSETS,
            "database.query" to ExtensionPermission.DATABASE
        )
    }
}

/** "Save as…" with a file name and type chosen at launch time. */
internal class CreateDocumentWithType : ActivityResultContract<CreateDocumentWithType.Input, android.net.Uri?>() {

    data class Input(val fileName: String, val mimeType: String)

    override fun createIntent(context: Context, input: Input): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): android.net.Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}
