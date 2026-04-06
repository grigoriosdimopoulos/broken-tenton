package com.linkedinautomation.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.presentation.dashboard.JobApplicationCard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onApplicationClick: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val tabs = listOf("Applications", "Activity", "Claude AI")
    var selectedTab by remember { mutableIntStateOf(0) }

    val applications by viewModel.applications.collectAsState(emptyList())
    val activityLogs by viewModel.activityLogs.collectAsState(emptyList())
    val claudeLogs by viewModel.claudeLogs.collectAsState(emptyList())
    val totalTokens by viewModel.totalTokens.collectAsState(0)

    Column(modifier = Modifier.fillMaxSize()) {
        Text("History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        when (selectedTab) {
            0 -> ApplicationsTab(applications, onApplicationClick)
            1 -> ActivityTab(activityLogs)
            2 -> ClaudeAiTab(claudeLogs, totalTokens)
        }
    }
}

@Composable
private fun ApplicationsTab(
    apps: List<com.linkedinautomation.domain.model.JobApplication>,
    onApplicationClick: (Long) -> Unit
) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Text("No applications yet", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("When the automator applies to jobs they'll appear here. Tap any card to see full details and screenshots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Tap any card for full details and screenshot",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
            }
            items(apps, key = { it.id }) { app ->
                JobApplicationCard(app, onClick = { onApplicationClick(app.id) })
            }
        }
    }
}

@Composable
private fun ActivityTab(logs: List<com.linkedinautomation.domain.model.ActivityLog>) {
    val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
    if (logs.isEmpty()) {
        EmptyState("No activity yet")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs, key = { it.id }) { log ->
                var expanded by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(log.action.displayName, style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fmt.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Always show summary line
                        if (log.details.isNotBlank()) {
                            val summary = log.details.lines().first().take(80)
                            Text(
                                if (expanded) log.details else summary + if (log.details.length > 80) "…" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        // Full URL only when expanded
                        if (expanded && !log.url.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(log.url, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaudeAiTab(logs: List<com.linkedinautomation.domain.model.ClaudeUsageLog>, totalTokens: Int) {
    val fmt = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    Column(modifier = Modifier.fillMaxSize()) {
        if (totalTokens > 0) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Total tokens used: $totalTokens",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        if (logs.isEmpty()) {
            EmptyState("No Claude queries yet")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(logs, key = { it.id }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${log.jobTitle} @ ${log.company}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(fmt.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Q: ${log.question}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("A: ${log.answer}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text("${log.totalTokens} tokens (${log.inputTokens} in / ${log.outputTokens} out)",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
