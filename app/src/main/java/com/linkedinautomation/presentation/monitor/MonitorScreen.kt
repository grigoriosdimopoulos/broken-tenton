package com.linkedinautomation.presentation.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.automation.orchestrator.AutomationState
import com.linkedinautomation.presentation.components.SectionHeader
import com.linkedinautomation.presentation.components.ToggleRow

@Composable
fun MonitorScreen(viewModel: MonitorViewModel = hiltViewModel()) {
    val state by viewModel.automationState.collectAsState()
    val logs by viewModel.liveLog.collectAsState()
    val prefs by viewModel.prefs.collectAsState(null)
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Monitor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("AUTOMATION")
                    ToggleRow(
                        label = "Enabled",
                        subtext = "Automatically search and apply for jobs in the background",
                        checked = prefs?.automationEnabled ?: false,
                        onCheckedChange = { viewModel.setAutomationEnabled(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SectionHeader("APPROVAL MODE")
                    ToggleRow(
                        label = "Require approval before applying",
                        subtext = if (prefs?.requireApproval == true)
                            "Jobs will be queued — you review and approve each one"
                        else
                            "Auto-approve: all matching jobs applied immediately",
                        checked = prefs?.requireApproval ?: false,
                        onCheckedChange = { viewModel.setRequireApproval(it) }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("CURRENT STATUS")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (dot, text) = when (state) {
                            is AutomationState.Running -> Color(0xFF057642) to (state as AutomationState.Running).message
                            is AutomationState.Paused -> Color(0xFFE67E22) to "Paused"
                            is AutomationState.Error -> Color(0xFFB00020) to (state as AutomationState.Error).message
                            else -> Color.Gray to "Idle — waiting for next scheduled run"
                        }
                        Box(modifier = Modifier.size(10.dp).background(dot, RoundedCornerShape(50)))
                        Spacer(Modifier.width(8.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            SectionHeader("LIVE LOG")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                if (logs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Waiting for automation to run...", color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logs) { line ->
                            Text(line, color = Color(0xFF00FF88), fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
