package com.linkedinautomation.automation.engine

import android.webkit.JavascriptInterface

class JavaScriptBridge(private val callback: WebEngineCallback) {

    @JavascriptInterface
    fun onResult(tag: String, result: String) {
        callback.onJsResult(tag, result)
    }

    @JavascriptInterface
    fun onError(tag: String, error: String) {
        callback.onJsError(tag, error)
    }

    @JavascriptInterface
    fun log(message: String) {
        android.util.Log.d("JSBridge", message)
    }
}
