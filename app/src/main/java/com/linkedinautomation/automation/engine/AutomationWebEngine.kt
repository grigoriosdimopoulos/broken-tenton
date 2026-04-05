package com.linkedinautomation.automation.engine

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import com.linkedinautomation.domain.model.SourceMode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("SetJavaScriptEnabled")
class AutomationWebEngine(
    private val context: Context,
    private val sourceMode: SourceMode,
    private val onLog: (String) -> Unit,
    private val onBlockedNavigation: (String) -> Unit,
    private val onFileChooserRequested: ((String?) -> Unit) -> Unit
) {
    private var webView: WebView? = null
    private val pendingCallbacks = ConcurrentHashMap<String, (String) -> Unit>()
    private val pendingErrors = ConcurrentHashMap<String, (String) -> Unit>()
    private var pageLoadedCallback: ((String) -> Unit)? = null

    val currentUrl: String? get() = webView?.url

    fun create() {
        val wv = WebView(context).also { webView = it }
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
        }

        val bridge = JavaScriptBridge(object : WebEngineCallback {
            override fun onPageLoaded(url: String) {
                pageLoadedCallback?.invoke(url)
            }
            override fun onJsResult(tag: String, result: String) {
                pendingCallbacks.remove(tag)?.invoke(result)
            }
            override fun onJsError(tag: String, error: String) {
                pendingErrors.remove(tag)?.invoke(error)
                pendingCallbacks.remove(tag)?.invoke("__error__:$error")
            }
            override fun onFileChooserRequested(callback: (String?) -> Unit) {
                onFileChooserRequested(callback)
            }
            override fun onBlockedNavigation(url: String) {
                onLog("BLOCKED: $url")
                onBlockedNavigation(url)
            }
        })
        wv.addJavascriptInterface(bridge, "AndroidBridge")

        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (!UrlAllowlist.isAllowed(url, sourceMode)) {
                    if (UrlAllowlist.isBlockedLinkedInUrl(url)) {
                        bridge.log("Blocked navigation to: $url")
                        onBlockedNavigation(url)
                    }
                    return true // block
                }
                return false // allow
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                pageLoadedCallback?.invoke(url)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<android.net.Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                onFileChooserRequested { path ->
                    if (path != null) {
                        val uri = android.net.Uri.fromFile(java.io.File(path))
                        filePathCallback.onReceiveValue(arrayOf(uri))
                    } else {
                        filePathCallback.onReceiveValue(null)
                    }
                }
                return true
            }
        }
    }

    fun destroy() {
        webView?.destroy()
        webView = null
        pendingCallbacks.clear()
        pendingErrors.clear()
    }

    suspend fun navigateTo(url: String, timeoutMs: Long = 15_000): String =
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                pageLoadedCallback = { loadedUrl ->
                    pageLoadedCallback = null
                    cont.resume(loadedUrl)
                }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    webView?.loadUrl(url)
                }
                cont.invokeOnCancellation { pageLoadedCallback = null }
            }
        }

    suspend fun runJs(tag: String, script: String, timeoutMs: Long = 10_000): String =
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                pendingCallbacks[tag] = { result -> cont.resume(result) }
                pendingErrors[tag] = { err -> cont.resumeWithException(RuntimeException(err)) }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    webView?.evaluateJavascript(script, null)
                }
                cont.invokeOnCancellation {
                    pendingCallbacks.remove(tag)
                    pendingErrors.remove(tag)
                }
            }
        }

    fun runJsFireAndForget(script: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            webView?.evaluateJavascript(script, null)
        }
    }
}
