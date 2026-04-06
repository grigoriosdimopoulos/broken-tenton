package com.linkedinautomation.presentation.settings.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.presentation.components.ToggleRow
import com.linkedinautomation.presentation.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateToPersona: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToJobPrefs: () -> Unit,
    onNavigateToResume: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SettingsSection("Personal Info") {
            SettingsItem("Your Details", "Name, phone, location — used in application forms", Icons.Default.Person, onNavigateToPersonalInfo)
        }
        SettingsSection("Account & Source") {
            SettingsItem("LinkedIn Credentials", "Update email and password", Icons.Default.ManageAccounts, onNavigateToAccount)
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
        SettingsSection("Backup & Restore") {
            SettingsItem("Export / Import Settings", "Save or restore all settings + resume", Icons.Default.CloudSync, onNavigateToBackup)
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
@OptIn(ExperimentalMaterial3Api::class)
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

// ─── Settings: Personal Info ──────────────────────────────────────────────────
@Composable
fun SettingsPersonalInfoScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState(null)
    var fn by remember(prefs) { mutableStateOf(prefs?.firstName ?: "") }
    var ln by remember(prefs) { mutableStateOf(prefs?.lastName ?: "") }
    var em by remember(prefs) { mutableStateOf(prefs?.email ?: "") }
    var ph by remember(prefs) { mutableStateOf(prefs?.phone ?: "") }
    var ct by remember(prefs) { mutableStateOf(prefs?.city ?: "") }
    var co by remember(prefs) { mutableStateOf(prefs?.country ?: "") }
    var li by remember(prefs) { mutableStateOf(prefs?.linkedInUrl ?: "") }
    var jt by remember(prefs) { mutableStateOf(prefs?.currentJobTitle ?: "") }
    var yoe by remember(prefs) { mutableStateOf(prefs?.yearsOfExperience?.takeIf { it > 0 }?.toString() ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Personal Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text("Used to auto-fill application forms.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 48.dp, bottom = 16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = fn, onValueChange = { fn = it }, label = { Text("First Name") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = ln, onValueChange = { ln = it }, label = { Text("Last Name") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = em, onValueChange = { em = it }, label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            placeholder = { Text("your@email.com") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = ct, onValueChange = { ct = it }, label = { Text("City") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = co, onValueChange = { co = it }, label = { Text("Country") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = li, onValueChange = { li = it }, label = { Text("LinkedIn Profile URL") },
            modifier = Modifier.fillMaxWidth(), placeholder = { Text("https://linkedin.com/in/yourname") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = jt, onValueChange = { jt = it }, label = { Text("Current Job Title") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = yoe, onValueChange = { yoe = it }, label = { Text("Years of Experience") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            viewModel.updatePersonalInfo(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0, em)
            onBack()
        }, enabled = fn.isNotBlank() && ln.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}

// ─── Settings: Credentials ────────────────────────────────────────────────────
@Composable
fun SettingsCredentialsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState(null)
    var email by remember(prefs) { mutableStateOf(prefs?.linkedInEmail ?: "") }
    var password by remember(prefs) { mutableStateOf(prefs?.linkedInPassword ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("LinkedIn Credentials", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("LinkedIn Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }}, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.updateCredentials(email, password); onBack() },
            enabled = email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}

// ─── Settings: Job Preferences ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsJobPrefsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState(null)
    var keywords by remember(prefs) { mutableStateOf(prefs?.jobKeywords?.joinToString(", ") ?: "") }
    var location by remember(prefs) { mutableStateOf(prefs?.location ?: "") }
    var remoteOnly by remember(prefs) { mutableStateOf(prefs?.remoteOnly ?: false) }
    var hybridOk by remember(prefs) { mutableStateOf(prefs?.hybridOk ?: true) }
    var onsiteOk by remember(prefs) { mutableStateOf(prefs?.onsiteOk ?: true) }
    var exclKw by remember(prefs) { mutableStateOf(prefs?.excludeKeywords?.joinToString(", ") ?: "") }
    var exclCo by remember(prefs) { mutableStateOf(prefs?.excludeCompanies?.joinToString(", ") ?: "") }
    var minSalary by remember(prefs) { mutableStateOf(prefs?.minSalary ?: 0) }
    var salaryExpanded by remember { mutableStateOf(false) }

    val salaryOptions = listOf(
        0 to "No minimum",
        40 to "\$40,000+",
        60 to "\$60,000+",
        80 to "\$80,000+",
        100 to "\$100,000+",
        120 to "\$120,000+",
        140 to "\$140,000+",
        160 to "\$160,000+",
        180 to "\$180,000+",
        200 to "\$200,000+"
    )
    val selectedSalaryLabel = salaryOptions.firstOrNull { it.first == minSalary }?.second ?: "No minimum"

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Job Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = keywords, onValueChange = { keywords = it }, label = { Text("Job Keywords") },
            modifier = Modifier.fillMaxWidth(), supportingText = { Text("Comma-separated") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. Athens, Greece") })
        Spacer(Modifier.height(12.dp))
        Text("Work Style", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Remote Only", checked = remoteOnly, onCheckedChange = { remoteOnly = it })
        if (!remoteOnly) {
            ToggleRow("Hybrid OK", checked = hybridOk, onCheckedChange = { hybridOk = it })
            ToggleRow("On-site OK", checked = onsiteOk, onCheckedChange = { onsiteOk = it })
        }
        Spacer(Modifier.height(12.dp))
        Text("Minimum Salary", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = salaryExpanded, onExpandedChange = { salaryExpanded = it }) {
            OutlinedTextField(
                value = selectedSalaryLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Minimum Annual Salary") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(salaryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = salaryExpanded, onDismissRequest = { salaryExpanded = false }) {
                salaryOptions.forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        minSalary = value; salaryExpanded = false
                    })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = exclKw, onValueChange = { exclKw = it }, label = { Text("Exclude keywords in title") },
            modifier = Modifier.fillMaxWidth(), supportingText = { Text("Comma-separated") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = exclCo, onValueChange = { exclCo = it }, label = { Text("Exclude companies") },
            modifier = Modifier.fillMaxWidth(), supportingText = { Text("Comma-separated") })
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            viewModel.updateJobPrefs(
                keywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
                location, remoteOnly, hybridOk, onsiteOk,
                exclKw.split(",").map { it.trim() }.filter { it.isNotBlank() },
                exclCo.split(",").map { it.trim() }.filter { it.isNotBlank() },
                minSalary
            )
            onBack()
        }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}

// ─── Settings: Resume ─────────────────────────────────────────────────────────
@Composable
fun SettingsResumeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsState(null)
    val context = LocalContext.current
    var picked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { picked = viewModel.copyResume(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Resume PDF", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current resume:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (prefs?.resumeFileName?.isNotBlank() == true) prefs!!.resumeFileName else "No resume uploaded",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (picked) {
            Text("Resume updated successfully!", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        }
        Button(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.UploadFile, null)
            Spacer(Modifier.width(8.dp))
            Text("Pick New Resume PDF")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

// ─── Settings: Backup & Restore ───────────────────────────────────────────────
@Composable
fun SettingsBackupScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exportUri by viewModel.exportUri.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val busy by viewModel.backupBusy.collectAsState()
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    // Launch share sheet when export URI is ready
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.clearBackupState() }

    LaunchedEffect(exportUri) {
        val uri = exportUri ?: return@LaunchedEffect
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Broken Tenton Settings Backup")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        shareLauncher.launch(android.content.Intent.createChooser(intent, "Save backup via…"))
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingImportUri = it; showImportConfirm = true }
    }

    // Import confirmation dialog
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImportUri = null },
            title = { Text("Overwrite all settings?") },
            text = { Text("This will replace ALL current settings, credentials, personal info, and resume with the backup file. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImportUri?.let { viewModel.importSettings(it) }
                        showImportConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Import & Overwrite") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImportUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        // Result banners
        if (importResult == true) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Settings imported successfully!", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        if (importResult == false) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text("Import failed. Make sure you selected a valid Broken Tenton backup file.",
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Export card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Export Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Creates a .json backup containing all your settings, personal info, Claude key, and resume PDF. Share it to Google Drive, email, or any storage app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.exportSettings() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Export & Share Backup")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Import card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Import Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Restores all settings from a previously exported backup file. Your resume PDF will also be restored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.clearBackupState(); importLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick Backup File to Import")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("What's included in the backup:", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Personal info (name, phone, location)",
                    "LinkedIn credentials",
                    "Job search preferences & exclusions",
                    "Experience bio & Claude API key",
                    "Claude persona / writing style",
                    "Resume PDF (embedded in file)"
                ).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
