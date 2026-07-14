package com.linkedinautomation.presentation.linkedinlogin

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

// LinkedIn URLs that indicate a successful login (user is now on the logged-in app)
private val LOGGED_IN_PATHS = listOf(
    "/feed", "/jobs", "/mynetwork", "/messaging",
    "/notifications", "/in/", "/home", "/checkpoint/post-login"
)

private const val LOGIN_URL = "https://www.linkedin.com/login"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LinkedInLoginViewModel = hiltViewModel()
) {
    var loading by remember { mutableStateOf(true) }
    var diag by remember { mutableStateOf("Starting…") }
    val saved by viewModel.saved.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(saved) {
        if (saved) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to LinkedIn") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        diag = "Reloading…"
                        forceCleanLoad(webViewRef)
                    }) {
                        Icon(Icons.Default.Refresh, "Reload")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // On-screen diagnostics banner — shows URL / errors / body state so we can
            // see what the WebView is actually doing without a debugger.
            Surface(color = Color(0xFF1C1C1E)) {
                Text(
                    text = diag,
                    color = Color(0xFF00E676),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 90.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(false)
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/137.0.0.0 Mobile Safari/537.36"
                            }
                            // LinkedIn strips the login form when it sees the WebView's
                            // X-Requested-With header (our package name). Suppress it.
                            suppressRequestedWithHeader(settings)
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    loading = true
                                    diag = "Loading: ${url?.take(70)}"
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    url?.let { checkForLogin(it, viewModel) }
                                    // Probe the DOM so we can see if the page is empty
                                    view?.evaluateJavascript(
                                        "(document.body?document.body.childElementCount:-1)+'|'+" +
                                        "(document.querySelectorAll('input').length)+'|'+" +
                                        "document.title"
                                    ) { r ->
                                        val clean = r?.trim('"') ?: "?"
                                        diag = "DONE ${url?.take(55)}\n" +
                                               "bodyKids|inputs|title = $clean"
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    // Only care about main-frame errors
                                    if (request?.isForMainFrame == true) {
                                        diag = "NET ERROR ${error?.errorCode}: " +
                                               "${error?.description}\n@ ${request.url}"
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        diag = "HTTP ${errorResponse?.statusCode} " +
                                               "@ ${request.url.toString().take(70)}"
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (isLoggedInUrl(url)) checkForLogin(url, viewModel)
                                    return false
                                }
                            }

                            // Popups (window.open / target=_blank, e.g. "Sign in with
                            // Google") must load in THIS WebView — no handler = dead tap.
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onCreateWindow(
                                    view: WebView?, isDialog: Boolean,
                                    isUserGesture: Boolean, resultMsg: android.os.Message?
                                ): Boolean {
                                    if (resultMsg == null) return false
                                    val transport = resultMsg.obj
                                            as? WebView.WebViewTransport ?: return false
                                    val temp = WebView(ctx)
                                    temp.settings.javaScriptEnabled = true
                                    temp.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            v: WebView?,
                                            request: android.webkit.WebResourceRequest?
                                        ): Boolean {
                                            request?.url?.toString()?.let { view?.loadUrl(it) }
                                            temp.post { temp.destroy() }
                                            return true
                                        }
                                    }
                                    transport.webView = temp
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }

                            forceCleanLoad(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

/**
 * Clears the app-global cookie jar (a stale li_at throws linkedin.com/login into an
 * infinite redirect loop → blank page), THEN loads the login page. The load is only
 * fired after the async clear reports completion, so the old cookie is truly gone
 * before the request. A one-shot guard prevents the callback + reload path from
 * double-loading.
 */
private fun forceCleanLoad(webView: WebView?) {
    val wv = webView ?: return
    val cm = CookieManager.getInstance()
    var fired = false
    fun go() {
        if (fired) return
        fired = true
        wv.post { wv.loadUrl(LOGIN_URL) }
    }
    cm.removeAllCookies {
        cm.flush()
        go()
    }
    // Fallback ONLY for the case where the callback never fires: clear synchronously-ish
    // (removeSessionCookies) then load, so we still don't ride the stale cookie.
    wv.postDelayed({
        if (!fired) {
            cm.removeSessionCookies { cm.flush(); go() }
            // absolute last resort if even that callback is silent
            wv.postDelayed({ go() }, 800L)
        }
    }, 1_500L)
}

/**
 * Stops the WebView from sending the X-Requested-With header (which carries the app's
 * package name). LinkedIn — like Google and others — uses that header to detect embedded
 * WebViews and serve a stripped page with no login form. An empty allow-list means the
 * header is sent to no origin. No-op on WebView versions that don't support the API.
 */
private fun suppressRequestedWithHeader(settings: android.webkit.WebSettings) {
    runCatching {
        if (androidx.webkit.WebViewFeature.isFeatureSupported(
                androidx.webkit.WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            androidx.webkit.WebSettingsCompat.setRequestedWithHeaderOriginAllowList(
                settings, emptySet())
        }
    }
}

private fun isLoggedInUrl(url: String): Boolean {
    val lower = url.lowercase()
    if (!lower.contains("linkedin.com")) return false
    return LOGGED_IN_PATHS.any { path -> lower.contains("linkedin.com$path") }
}

private fun checkForLogin(url: String, viewModel: LinkedInLoginViewModel) {
    if (!isLoggedInUrl(url)) return
    val cookieManager = CookieManager.getInstance()
    cookieManager.flush()
    val cookies = cookieManager.getCookie(".linkedin.com")
        ?: cookieManager.getCookie("https://www.linkedin.com")
        ?: return
    if (cookies.isNotBlank() && cookies.contains("li_at")) {
        viewModel.saveCookies(cookies)
    }
}
