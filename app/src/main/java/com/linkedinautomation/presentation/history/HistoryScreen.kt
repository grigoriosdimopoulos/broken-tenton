package com.linkedinautomation.presentation.history

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.ActivityAction
import com.linkedinautomation.domain.model.ActivityLog
import com.linkedinautomation.presentation.dashboard.JobApplicationCard
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onApplicationClick: (Long) -> Unit = {},
    onActivityClick: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val tabs = listOf("Applications", "Activity", "Claude AI")
    var selectedTab by remember { mutableIntStateOf(0) }

    val applications by viewModel.applications.collectAsState(emptyList())
    val activityLogs by viewModel.activityLogs.collectAsState(emptyList())
    val claudeLogs by viewModel.claudeLogs.collectAsState(emptyList())
    val totalTokens by viewModel.totalTokens.collectAsState(0)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "History", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> ApplicationsTab(applications, onApplicationClick)
            1 -> ActivityTab(activityLogs, onActivityClick)
            2 -> ClaudeAiTab(claudeLogs, totalTokens)
        }
    }
}

// ─── Applications tab ─────────────────────────────────────────────────────────

@Composable
private fun ApplicationsTab(
    apps: List<com.linkedinautomation.domain.model.JobApplication>,
    onApplicationClick: (Long) -> Unit
) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    "No applications yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "When the automator applies to jobs they'll appear here. Tap any card to see full details and screenshots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Tap any card for full details and screenshot",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(apps, key = { it.id }) { app ->
                JobApplicationCard(app, onClick = { onApplicationClick(app.id) })
            }
        }
    }
}

// ─── Activity tab ──────────────────────────────────────────────────────────────

private val ACTION_COLOR = mapOf(
    ActivityAction.LOGIN_SUCCESS        to Color(0xFF00C853),
    ActivityAction.LOGIN_FAILURE        to Color(0xFFFF1744),
    ActivityAction.LOGIN_ATTEMPT        to Color(0xFF82B1FF),
    ActivityAction.RUN_STARTED          to Color(0xFF82B1FF),
    ActivityAction.RUN_COMPLETED        to Color(0xFF82B1FF),
    ActivityAction.SEARCH_EXECUTED      to Color(0xFF80D8FF),
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
    ActivityAction.JOB_FOUND           to Color(0xFFCCFF90),
)

@Composable
private fun ActivityTab(logs: List<ActivityLog>, onActivityClick: (Long) -> Unit = {}) {
    val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
    if (logs.isEmpty()) {
        EmptyState("No activity yet — automation will log every step here")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                "${logs.size} events — tap to open detail",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        items(logs, key = { it.id }) { log ->
            ActivityCard(log, fmt, onActivityClick)
        }
    }
}

@Composable
private fun ActivityCard(
    log: ActivityLog,
    fmt: SimpleDateFormat,
    onActivityClick: (Long) -> Unit = {}
) {
    val accentColor = ACTION_COLOR[log.action] ?: MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivityClick(log.id) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row {
            // Colored left border strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 56.dp)
                    .background(accentColor)
            )
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).weight(1f)) {

                // Header row: action name + timestamp + chevron
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        log.action.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        fmt.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Summary (first line, truncated)
                if (log.details.isNotBlank()) {
                    val summary = log.details.lines().first().take(80)
                    Text(
                        summary + if (log.details.length > 80 || log.details.lines().size > 1) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // URL chip — always visible when url is present, opens browser on tap
                if (!log.url.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(log.url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open URL",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            log.url!!.take(60) + if (log.url.length > 60) "…" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Screenshot indicator
                val ssPath = log.screenshotPath
                if (!ssPath.isNullOrBlank() && File(ssPath).exists()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (ssPath.endsWith(".txt")) "Page snapshot available — tap to view" else "Screenshot available — tap to view",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

// ─── Claude AI tab ────────────────────────────────────────────────────────────

@Composable
private fun ClaudeAiTab(logs: List<com.linkedinautomation.domain.model.ClaudeUsageLog>, totalTokens: Int) {
    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    Column(modifier = Modifier.fillMaxSize()) {
        if (totalTokens > 0) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Total tokens used: $totalTokens",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (logs.isEmpty()) {
            EmptyState("No Claude queries yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${log.jobTitle} @ ${log.company}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    fmt.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Q: ${log.question}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("A: ${log.answer}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${log.totalTokens} tokens (${log.inputTokens} in / ${log.outputTokens} out)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp))
    }
}
