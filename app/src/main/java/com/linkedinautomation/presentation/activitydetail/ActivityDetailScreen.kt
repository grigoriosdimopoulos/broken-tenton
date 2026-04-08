package com.linkedinautomation.presentation.activitydetail

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.ActivityAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val ACTION_COLOR = mapOf(
    ActivityAction.LOGIN_SUCCESS        to Color(0xFF00C853),
    ActivityAction.LOGIN_FAILURE        to Color(0xFFFF1744),
    ActivityAction.LOGIN_ATTEMPT        to Color(0xFF82B1FF),
    ActivityAction.RUN_STARTED          to Color(0xFF82B1FF),
    ActivityAction.RUN_COMPLETED        to Color(0xFF82B1FF),
    ActivityAction.SEARCH_EXECUTED      to Color(0xFF80D8FF),
    ActivityAction.JOB_FOUND           to Color(0xFFCCFF90),
    ActivityAction.JOB_VIEWED           to Color(0xFFCCFF90),
    ActivityAction.EASY_APPLY_STARTED   to Color(0xFFFFD740),
    ActivityAction.EASY_APPLY_STEP      to Color(0xFFFFAB40),
    ActivityAction.EASY_APPLY_SUBMITTED to Color(0xFF00E676),
    ActivityAction.EASY_APPLY_FAILED    to Color(0xFFFF1744),
    ActivityAction.MODAL_CHECK          to Color(0xFFFFAB40),
    ActivityAction.APPLICATION_SUBMITTED to Color(0xFF00E676),
    ActivityAction.APPLICATION_FAILED   to Color(0xFFFF1744),
    ActivityAction.LOCATION_SKIPPED     to Color(0xFFBCAAA4),
    ActivityAction.SCREENING_QUESTION   to Color(0xFFEA80FC),
    ActivityAction.BLOCKED_NAVIGATION   to Color(0xFFFF6D00),
    ActivityAction.EXTERNAL_URL_OPENED  to Color(0xFFFFFF00),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onBack: () -> Unit,
    viewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val log by viewModel.log.collectAsState()
    val context = LocalContext.current
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val entry = log
        if (entry == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val accentColor = ACTION_COLOR[entry.action] ?: MaterialTheme.colorScheme.primary

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card with colored stripe
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .heightIn(min = 72.dp)
                            .background(accentColor)
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            entry.action.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            fmt.format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Details card
            if (entry.details.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            entry.details,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // URL card — clickable button to open in browser
            val url = entry.url
            if (!url.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Browser")
                        }
                    }
                }
            }

            // Screenshot or text snapshot
            val ssPath = entry.screenshotPath
            if (!ssPath.isNullOrBlank() && File(ssPath).exists()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Page Capture",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        when {
                            ssPath.endsWith(".html") -> {
                                // Render the saved HTML page proof in a mini WebView
                                val htmlContent = remember(ssPath) {
                                    runCatching { File(ssPath).readText() }.getOrNull()
                                }
                                if (!htmlContent.isNullOrBlank()) {
                                    @SuppressLint("SetJavaScriptEnabled")
                                    AndroidView(
                                        factory = { ctx ->
                                            android.webkit.WebView(ctx).apply {
                                                settings.javaScriptEnabled = false
                                                settings.loadWithOverviewMode = true
                                                settings.useWideViewPort = true
                                                // Load with the original page URL as base so relative links resolve
                                                loadDataWithBaseURL(
                                                    entry.url ?: "about:blank",
                                                    htmlContent,
                                                    "text/html",
                                                    "UTF-8",
                                                    null
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(480.dp)
                                    )
                                }
                            }
                            ssPath.endsWith(".txt") -> {
                                val text = remember(ssPath) {
                                    runCatching { File(ssPath).readText() }.getOrNull()
                                }
                                if (!text.isNullOrBlank()) {
                                    Text(text, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            else -> {
                                val bitmap = remember(ssPath) {
                                    runCatching {
                                        BitmapFactory.decodeFile(ssPath)?.asImageBitmap()
                                    }.getOrNull()
                                }
                                if (bitmap != null) {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = "Page screenshot",
                                            modifier = Modifier.fillMaxWidth(),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                } else {
                                    Text(
                                        "Screenshot file exists but could not be decoded.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
