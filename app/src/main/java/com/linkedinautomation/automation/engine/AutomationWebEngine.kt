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
        // Software rendering is required for WebView.draw(canvas) to work in a background
        // Foreground Service context (hardware-accelerated views render blank).
        // Must be set BEFORE any content loads — setting it just before draw() is too late.
        wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        configureWebView(wv)
    }

    /**
     * Attaches to an existing WebView (e.g. created by a Compose AndroidView in Activity context).
     * Does NOT set LAYER_TYPE_SOFTWARE — an Activity-attached View uses hardware acceleration,
     * which renders correctly and is actually preferred for the visible apply screen.
     */
    fun createWithExistingWebView(wv: WebView) {
        webView = wv
        configureWebView(wv)
    }

    private fun configureWebView(wv: WebView) {
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
                this@AutomationWebEngine.onFileChooserRequested(callback)
            }
            override fun onBlockedNavigation(url: String) {
                onLog("BLOCKED: $url")
                this@AutomationWebEngine.onBlockedNavigation(url)
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
                        onLog("Blocked navigation to: $url")
                        this@AutomationWebEngine.onBlockedNavigation(url)
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
                this@AutomationWebEngine.onFileChooserRequested { path ->
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
     * Captures the current WebView content as a PNG.
     *
     * A headless WebView (not attached to a real window) never paints — draw() and
     * capturePicture() both return blank bitmaps. The fix is to momentarily attach
     * the WebView to an invisible WindowManager overlay so the GPU/SW renderer fires,
     * capture the bitmap, then detach.
     *
     * Falls back gracefully if SYSTEM_ALERT_WINDOW is not granted: saves an HTML
     * text snapshot (.txt) instead so there's still a record of what was shown.
     */
    @Suppress("DEPRECATION")
    suspend fun takeScreenshot(tag: String, screenshotsDir: File): String? =
        withContext(Dispatchers.Main) {
            runCatching {
                val wv = webView ?: return@runCatching null
                screenshotsDir.mkdirs()

                val targetW = 1080
                val targetH = 1920

                wv.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(targetW, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(targetH, android.view.View.MeasureSpec.EXACTLY)
                )
                wv.layout(0, 0, targetW, targetH)

                // Strategy 1: attach to WindowManager overlay so the view actually paints
                val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE)
                        as? android.view.WindowManager
                var attachedToWindow = false
                if (wm != null && android.provider.Settings.canDrawOverlays(context)) {
                    try {
                        val params = android.view.WindowManager.LayoutParams(
                            targetW, targetH,
                            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            android.graphics.PixelFormat.TRANSLUCENT
                        ).apply { x = -targetW * 2; y = -targetH * 2 } // off-screen
                        wm.addView(wv, params)
                        attachedToWindow = true
                        // Give the renderer one frame to paint
                        kotlinx.coroutines.delay(300)
                    } catch (_: Exception) { }
                }

                return@runCatching try {
                    val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    wv.draw(canvas)

                    val isBlank = run {
                        val sample = IntArray(100)
                        bitmap.getPixels(sample, 0, 10, 0, 0, 10, 10)
                        sample.all { it == sample[0] }
                    }

                    if (isBlank) {
                        // Can't get a real screenshot — save a text page snapshot instead
                        bitmap.recycle()
                        saveTextSnapshot(wv, tag, screenshotsDir)
                    } else {
                        val file = File(screenshotsDir, "${tag}_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
                        bitmap.recycle()
                        file.absolutePath
                    }
                } finally {
                    if (attachedToWindow && wm != null) {
                        runCatching { wm.removeView(wv) }
                    }
                }
            }.getOrNull()
        }

    /** When a real screenshot isn't possible, save the page's URL + visible body text as a .txt file. */
    private suspend fun saveTextSnapshot(wv: WebView, tag: String, dir: File): String? {
        return runCatching {
            val url = wv.url ?: "unknown"
            val title = wv.title ?: "unknown"

            // Capture visible page text via JS on Main thread — no special permission needed
            val bodyText: String = withContext(Dispatchers.Main) {
                runCatching {
                    withTimeout(2_000) {
                        suspendCancellableCoroutine { cont ->
                            wv.evaluateJavascript(
                                "(document.body ? document.body.innerText : '').replace(/\\s+/g,' ').substring(0,2000)"
                            ) { result ->
                                val text = result
                                    ?.removeSurrounding("\"")
                                    ?.replace("\\n", "\n")
                                    ?.replace("\\\"", "\"")
                                    ?: ""
                                cont.resume(text)
                            }
                        }
                    }
                }.getOrDefault("")
            }

            val file = File(dir, "${tag}_${System.currentTimeMillis()}.txt")
            file.writeText(buildString {
                appendLine("URL: $url")
                appendLine("Title: $title")
                appendLine("---")
                if (bodyText.isNotBlank()) {
                    appendLine(bodyText)
                } else {
                    appendLine("[No page content captured — grant 'Display over other apps' in Android Settings for real screenshots]")
                }
            })
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

    /**
     * Executes [js] and waits up to [navWaitMs] for the page to finish loading
     * (i.e. a navigation occurred). If the page loads within the timeout, returns
     * "navigated:url". If no navigation in time (in-page DOM update, modal change
     * etc.), returns "done".
     *
     * This replaces the old bridge-callback approach for SmartApply action steps.
     * The bridge callback was unreliable because the old page's JS context is
     * destroyed on navigation, so setTimeout-based onResult never fired → 12s timeout.
     */
    suspend fun executeJsAndWaitForNavigation(js: String, navWaitMs: Long = 4_000): String =
        runCatching {
            withTimeout(navWaitMs) {
                suspendCancellableCoroutine { cont ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        pageLoadedCallback = { url ->
                            pageLoadedCallback = null
                            if (!cont.isCompleted) cont.resume("navigated:$url")
                        }
                        webView?.evaluateJavascript(js, null)
                        cont.invokeOnCancellation { pageLoadedCallback = null }
                    }
                }
            }
        }.getOrElse { "done" }
}
