package com.linkedinautomation.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.presentation.dashboard.JobApplicationCard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
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
            0 -> ApplicationsTab(applications)
            1 -> ActivityTab(activityLogs)
            2 -> ClaudeAiTab(claudeLogs, totalTokens)
        }
    }
}

@Composable
private fun ApplicationsTab(apps: List<com.linkedinautomation.domain.model.JobApplication>) {
    if (apps.isEmpty()) {
        EmptyState("No applications yet")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.id }) { app ->
                JobApplicationCard(app)
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(log.action.displayName, style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(fmt.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (log.details.isNotBlank()) {
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!log.url.isNullOrBlank()) {
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
