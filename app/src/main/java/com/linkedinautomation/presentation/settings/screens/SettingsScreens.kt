package com.linkedinautomation.presentation.settings.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.presentation.settings.SettingsViewModel
import com.linkedinautomation.presentation.setup.SetupViewModel

@Composable
fun SettingsScreen(
    onNavigateToPersona: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToJobPrefs: () -> Unit,
    onNavigateToResume: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SettingsSection("Account & Source") {
            SettingsItem("Source Mode & Credentials", "LinkedIn or Job Boards", Icons.Default.ManageAccounts, onNavigateToAccount)
        }
        SettingsSection("Job Search") {
            SettingsItem("Job Preferences", "Keywords, location, filters", Icons.Default.Work, onNavigateToJobPrefs)
        }
        SettingsSection("Resume") {
            SettingsItem("Resume PDF", "Update your resume file", Icons.Default.UploadFile, onNavigateToResume)
        }
        SettingsSection("Claude AI") {
            SettingsItem("Writing Style & Persona", "Tone, style, custom instructions", Icons.Default.Psychology, onNavigateToPersona)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        content()
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

// ─── Claude Persona Settings ──────────────────────────────────────────────────
@Composable
fun ClaudePersonaScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState(null)
    val persona = prefs?.claudePersona ?: ClaudePersona()
    var current by remember(persona) { mutableStateOf(persona) }
    var avoidInput by remember(persona.avoidPhrases) { mutableStateOf(persona.avoidPhrases.joinToString(", ")) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Claude Writing Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(value = current.tone.displayName, onValueChange = {}, readOnly = true,
                label = { Text("Tone") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PersonaTone.entries.forEach { tone ->
                    DropdownMenuItem(text = { Text(tone.displayName) }, onClick = {
                        current = current.copy(tone = tone); expanded = false
                    })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = current.styleNotes, onValueChange = { current = current.copy(styleNotes = it) },
            label = { Text("Style Notes") }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Be concise. Show genuine interest.") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = avoidInput, onValueChange = {
            avoidInput = it
            current = current.copy(avoidPhrases = it.split(",").map { p -> p.trim() }.filter { p -> p.isNotBlank() })
        }, label = { Text("Phrases to Avoid") }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("passionate about, synergy, leverage") },
            supportingText = { Text("Comma-separated") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = current.customInstructions, onValueChange = { current = current.copy(customInstructions = it) },
            label = { Text("Custom Instructions") }, modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("e.g. Always mention my 5 years of Python experience. Refer to my work at XYZ Corp.") },
            maxLines = 6)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.savePersona(current); onBack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}
