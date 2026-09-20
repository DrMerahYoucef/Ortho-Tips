package com.example.ui.screens.extensions

import android.app.Activity
import android.content.SharedPreferences
import android.webkit.JavascriptInterface
import android.widget.Toast

class ExtensionWebBridge(
    private val activity: Activity,
    private val storage: SharedPreferences,
    private val extensionId: String
) {
    @JavascriptInterface fun get(key: String): String? = storage.getString(key, null)
    @JavascriptInterface fun set(key: String, value: String) { storage.edit().putString(key, value).apply() }
    @JavascriptInterface fun remove(key: String) { storage.edit().remove(key).apply() }
    @JavascriptInterface fun keys(prefix: String): String =
        storage.all.keys.filter { it.startsWith(prefix) }.sorted().joinToString(",", "[", "]") { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" }
    @JavascriptInterface fun toast(message: String) = activity.runOnUiThread {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
    @JavascriptInterface fun close() = activity.runOnUiThread { activity.finish() }
    fun id(): String = extensionId
}
