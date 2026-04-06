package com.linkedinautomation.automation.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.*
import com.linkedinautomation.domain.model.SourceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
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

    // When true, bypasses URL allowlist so we can navigate to any ATS domain
    @Volatile private var applyModeEnabled = false

    fun enableApplyMode() { applyModeEnabled = true }
    fun disableApplyMode() { applyModeEnabled = false }

    /**
     * Runs [script] (expected to trigger a page navigation via click / window.location)
     * and waits until [onPageFinished] fires for the new page.
     * Apply mode is kept enabled for the whole duration so ATS redirects are allowed.
     * Returns the final URL, or empty string on timeout / no navigation.
     */
    suspend fun runJsAndWaitForNavigation(script: String, timeoutMs: Long = 25_000): String {
        enableApplyMode()
        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    pageLoadedCallback = { url ->
                        pageLoadedCallback = null
                        if (!cont.isCompleted) cont.resume(url)
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        webView?.evaluateJavascript(script, null)
                    }
                    cont.invokeOnCancellation { pageLoadedCallback = null }
                }
            }
        } catch (e: Exception) {
            ""
        } finally {
            disableApplyMode()
        }
    }

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
                // During apply mode, allow navigation to any external ATS URL
                if (applyModeEnabled) return false
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

    /**
     * Captures the current WebView content as a PNG screenshot.
     * Returns the absolute file path on success, null on failure.
     */
    suspend fun takeScreenshot(tag: String, screenshotsDir: File): String? =
        withContext(Dispatchers.Main) {
            runCatching {
                val wv = webView ?: return@runCatching null
                screenshotsDir.mkdirs()
                val targetW = 1080
                val targetH = 1920
                // Measure and layout if WebView has no dimensions (headless)
                if (wv.width == 0 || wv.height == 0) {
                    wv.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(targetW, android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(targetH, android.view.View.MeasureSpec.EXACTLY)
                    )
                    wv.layout(0, 0, targetW, targetH)
                }
                val w = wv.width.coerceAtLeast(targetW)
                val h = wv.height.coerceAtLeast(targetH)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                // Software rendering required for WebView.draw() in background/headless contexts
                wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                wv.draw(canvas)
                wv.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                val file = File(screenshotsDir, "${tag}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
                bitmap.recycle()
                file.absolutePath
            }.getOrNull()
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
