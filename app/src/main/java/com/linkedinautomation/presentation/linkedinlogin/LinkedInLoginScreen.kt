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
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                loading = true
                                url?.let { pageUrl ->
                                    if (pageUrl.contains("linkedin.com/feed") ||
                                        pageUrl.contains("linkedin.com/jobs") ||
                                        pageUrl.contains("linkedin.com/checkpoint/post-login")) {
                                        val cookies = CookieManager.getInstance().getCookie(".linkedin.com") ?: ""
                                        if (cookies.isNotBlank()) {
                                            viewModel.saveCookies(cookies)
                                        }
                                    }
                                }
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                            }
                        }
                        loadUrl("https://www.linkedin.com/login")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp).align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }
        }
    }
}
