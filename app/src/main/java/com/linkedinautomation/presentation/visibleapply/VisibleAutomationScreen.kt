package com.linkedinautomation.presentation.visibleapply

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisibleAutomationScreen(
    viewModel: VisibleApplyViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val statusText by viewModel.statusText.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isDone by viewModel.isDone.collectAsState()
    val engineReady by viewModel.engineReady.collectAsState()
    val logListState = rememberLazyListState()

    // Navigate back 2 seconds after completion so user can see the result
    LaunchedEffect(isDone) {
        if (isDone) {
            delay(2_000)
            onBack()
        }
    }

    // Start the apply loop as soon as the engine is configured on the WebView
    LaunchedEffect(engineReady) {
        if (engineReady) {
            viewModel.startApply()
        }
    }

    // Auto-scroll log to bottom when new entries arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.jobTitle.ifBlank { "Applying…" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = viewModel.company,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancel()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.cancel()
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDone && statusText.startsWith("Applied"))
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDone && statusText.startsWith("Applied"))
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Live WebView — user watches the automation happen (65% of height)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            userAgentString =
                                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
                        }
                        // Reuse LinkedIn session cookies from the login screen
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        viewModel.attachWebView(this, ctx)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
            )

            HorizontalDivider()

            // Step log panel (35% of height) — tail of automation steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
            ) {
                Text(
                    "Automation log",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp)
                )
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(logs) { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
