package com.linkedinautomation.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.presentation.components.StatusBadge
import com.linkedinautomation.presentation.components.StatsCard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onApplicationClick: (Long) -> Unit = {},
    onGoToPersonalInfo: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val totalApplied by viewModel.totalApplied.collectAsState(0)
    val pendingCount by viewModel.pendingCount.collectAsState(0)
    val thisWeekCount by viewModel.thisWeekCount.collectAsState(0)
    val recentApps by viewModel.recentApplications.collectAsState(emptyList())
    val personalInfoMissing by viewModel.personalInfoMissing.collectAsState(false)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
        }

        // Personal info missing warning
        if (personalInfoMissing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonOff, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personal info not set",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                            Text("Application forms need your name, phone, etc. Go to Settings → Personal Information.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onGoToPersonalInfo) { Text("Fix") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatsCard("Total Applied", totalApplied.toString(), Modifier.weight(1f))
                StatsCard("This Week", thisWeekCount.toString(), Modifier.weight(1f))
                StatsCard("Pending", pendingCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Recent Applications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (recentApps.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(24.dp)) {
                        Text("No applications yet. The automator will start applying soon.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(recentApps) { app ->
                JobApplicationCard(app, onClick = { onApplicationClick(app.id) })
            }
        }
    }
}

@Composable
fun JobApplicationCard(app: JobApplication, onClick: (() -> Unit)? = null) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(app.company, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(app.status)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(app.applicationType.displayName(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(fmt.format(Date(app.appliedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Show error reason inline for failed applications
            if (app.status == ApplicationStatus.FAILED && !app.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        app.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
