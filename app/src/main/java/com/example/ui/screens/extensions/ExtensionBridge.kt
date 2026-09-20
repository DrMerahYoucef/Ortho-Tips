package com.example.ui.screens.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import androidx.activity.result.contract.ActivityResultContract
import com.example.data.extensions.ExtensionStorage
import org.json.JSONArray

/**
 * The object behind `window.OrthoTipsNative`. The page never calls it directly:
 * `assets/extension_host/ortho-bridge.js` wraps it into `window.OrthoTips`.
 *
 * WebView calls these methods on its own JavaBridge thread, so anything that
 * touches the UI is posted to the main thread.
 */
internal class ExtensionBridge(
    private val storage: ExtensionStorage,
    private val infoJson: () -> String,
    private val onToast: (String) -> Unit,
    private val onClose: () -> Unit,
    private val onSaveFile: (requestId: String, fileName: String, mimeType: String, bytes: ByteArray?) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun info(): String = infoJson()

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

    @JavascriptInterface
    fun toast(message: String?) {
        val text = message?.take(300)?.takeIf { it.isNotBlank() } ?: return
        main.post { onToast(text) }
    }

    @JavascriptInterface
    fun close() {
        main.post { onClose() }
    }

    @JavascriptInterface
    fun saveFile(requestId: String?, fileName: String?, mimeType: String?, base64: String?) {
        val id = requestId ?: return
        val bytes = runCatching { Base64.decode(base64.orEmpty(), Base64.DEFAULT) }.getOrNull()
        val name = cleanFileName(fileName)
        val mime = mimeType?.trim()?.takeIf { it.contains('/') } ?: "application/octet-stream"
        main.post { onSaveFile(id, name, mime, bytes) }
    }

    private fun cleanFileName(name: String?): String =
        name.orEmpty()
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim()
            .take(120)
            .ifEmpty { "fichier" }
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
