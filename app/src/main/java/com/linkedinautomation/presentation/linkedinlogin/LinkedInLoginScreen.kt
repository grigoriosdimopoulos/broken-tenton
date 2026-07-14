package com.linkedinautomation.presentation.linkedinlogin

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

// LinkedIn URLs that indicate a successful login (user is now on the logged-in app)
private val LOGGED_IN_PATHS = listOf(
    "/feed", "/jobs", "/mynetwork", "/messaging",
    "/notifications", "/in/", "/home", "/checkpoint/post-login"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInLoginScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LinkedInLoginViewModel = hiltViewModel()
) {
    var loading by remember { mutableStateOf(true) }
    val saved by viewModel.saved.collectAsState()

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
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(false)
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/137.0.0.0 Mobile Safari/537.36"
                        }
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?, url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                // Check AFTER page fully loaded — cookies are ready now
                                url?.let { checkForLogin(it, viewModel) }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                // Capture cookies as soon as a post-login URL is detected
                                // so we don't miss fast redirects
                                if (isLoggedInUrl(url)) {
                                    checkForLogin(url, viewModel)
                                }
                                return false // let WebView follow the redirect
                            }
                        }

                        // Popups (window.open / target=_blank, e.g. "Sign in with Google")
                        // must load in THIS WebView — with no handler the tap dead-ends.
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?, isDialog: Boolean,
                                isUserGesture: Boolean, resultMsg: android.os.Message?
                            ): Boolean {
                                if (resultMsg == null) return false
                                val transport = resultMsg.obj
                                        as? WebView.WebViewTransport ?: return false
                                // Temporary WebView just to capture the popup URL,
                                // then redirect it into the main WebView
                                val temp = WebView(ctx)
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

                        // Start from a clean slate: stale/expired session cookies make
                        // linkedin.com/login redirect into a checkpoint that renders as
                        // a blank page. The clear MUST finish before we load, otherwise
                        // the old li_at cookie still rides the request (blank page) or the
                        // async clear wipes the login page's fresh cookies mid-load.
                        //
                        // Load only after the clear completes (callback), and post the
                        // load onto the main looper. A one-shot fallback guarantees the
                        // page still loads if the callback never fires, so it can never
                        // hang blank.
                        val loginUrl = "https://www.linkedin.com/login"
                        val loaded = java.util.concurrent.atomic.AtomicBoolean(false)
                        fun loadOnce() {
                            if (loaded.compareAndSet(false, true)) {
                                post { loadUrl(loginUrl) }
                            }
                        }
                        cookieManager.removeAllCookies {
                            cookieManager.flush()
                            loadOnce()
                        }
                        // Fallback: if the removeAllCookies callback doesn't fire within
                        // 1.2s (happens on some WebView builds), load anyway.
                        postDelayed({ loadOnce() }, 1_200L)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
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
    // Flush pending writes then read
    val cookieManager = CookieManager.getInstance()
    cookieManager.flush()
    val cookies = cookieManager.getCookie(".linkedin.com")
        ?: cookieManager.getCookie("https://www.linkedin.com")
        ?: return
    if (cookies.isNotBlank() && cookies.contains("li_at")) {
        // li_at is LinkedIn's session auth cookie — if present, user is logged in
        viewModel.saveCookies(cookies)
    }
}
