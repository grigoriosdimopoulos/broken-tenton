package com.linkedinautomation.presentation.approvalqueue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.model.UserPreferences

@Composable
fun ApprovalQueueScreen(viewModel: ApprovalQueueViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val pending by viewModel.pending.collectAsState(emptyList())
    val prefs by viewModel.prefs.collectAsState(null)
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddManualJobDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, company, url ->
                viewModel.addManual(title, company, url)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Job") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        Text(
            "Approval Queue (${pending.size})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Review each job before the app applies on your behalf.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (pending.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No jobs waiting for approval", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Enable 'Manual Review Mode' in Monitor to use this queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(pending, key = { it.id }) { job ->
                    ApprovalCard(
                        job = job,
                        prefs = prefs,
                        onApprove = { viewModel.approve(job, context.filesDir) },
                        onReject = { viewModel.reject(job.id) }
                    )
                }
            }
        }
    }
    } // end Scaffold
}

@Composable
private fun AddManualJobDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Job Manually") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("The app will apply to this job when you tap Apply.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Job Title *") }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Android Engineer") })
                OutlinedTextField(value = company, onValueChange = { company = it },
                    label = { Text("Company") }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Acme Inc") })
                OutlinedTextField(value = url, onValueChange = { url = it },
                    label = { Text("Application URL *") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    placeholder = { Text("https://company.com/apply/...") })
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, company, url) },
                enabled = title.isNotBlank() && url.startsWith("http")
            ) { Text("Add to Queue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ApprovalCard(
    job: JobApplication,
    prefs: UserPreferences?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(job.company, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        job.applicationType.displayName(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ── Meta chips ───────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!job.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(job.location, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(job.source, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── URL ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(6.dp))
            Text(
                job.jobUrl.take(60) + if (job.jobUrl.length > 60) "…" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Fields that will be filled ───────────────────────────────────
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fields that will be submitted",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (expanded) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (expanded && prefs != null) {
                Spacer(Modifier.height(6.dp))
                val fields = buildList {
                    add("Name" to "${prefs.firstName} ${prefs.lastName}".trim().ifBlank { "— not set —" })
                    add("Email" to prefs.email.ifBlank { "— not set —" })
                    add("Phone" to prefs.phone.ifBlank { "— not set —" })
                    add("Location" to listOf(prefs.city, prefs.country).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "— not set —" })
                    add("LinkedIn URL" to prefs.linkedInUrl.ifBlank { "— not set —" })
                    add("Job Title" to prefs.currentJobTitle.ifBlank { "— not set —" })
                    add("Years Exp." to if (prefs.yearsOfExperience > 0) "${prefs.yearsOfExperience}" else "— not set —")
                    add("Resume" to prefs.resumeFileName.ifBlank { "— no resume —" })
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        fields.forEach { (label, value) ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(90.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = MaterialTheme.typography.bodySmall,
                                    color = if (value.startsWith("—")) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Actions ──────────────────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply Now")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Skip")
                }
            }
        }
    }
}
