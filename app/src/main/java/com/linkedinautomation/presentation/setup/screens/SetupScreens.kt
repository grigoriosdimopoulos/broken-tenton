package com.linkedinautomation.presentation.setup.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.presentation.components.ToggleRow

// ─── Welcome Screen ───────────────────────────────────────────────────────────
@Composable
fun WelcomeScreen(
    onNext: () -> Unit,
    onImportRequest: ((Uri) -> Unit)? = null,
    importState: Boolean? = null  // null=idle, true=success, false=failed
) {
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onImportRequest?.invoke(it) } }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Work, contentDescription = null,
            modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Broken Tenton", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Apply to hundreds of jobs automatically. Set up once, never worry again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { importLauncher.launch("application/json") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Restore from Backup")
        }
        if (importState == false) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Import failed — check the file is a valid Broken Tenton backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Source Mode Screen ───────────────────────────────────────────────────────
@Composable
fun SourceModeScreen(selected: SourceMode, onSelect: (SourceMode) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("How should we find jobs?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose your job source. You can change this later in Settings.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        SourceCard(
            title = "LinkedIn",
            description = "Search LinkedIn jobs and use Easy Apply. Requires your LinkedIn credentials.",
            icon = Icons.Default.Business,
            selected = selected == SourceMode.LINKEDIN,
            onClick = { onSelect(SourceMode.LINKEDIN) }
        )
        Spacer(Modifier.height(16.dp))
        SourceCard(
            title = "Job Boards (No Account Needed)",
            description = "Scan Indeed, Glassdoor, ZipRecruiter, and more. No LinkedIn account required.",
            icon = Icons.Default.Search,
            selected = selected == SourceMode.DIRECT,
            onClick = { onSelect(SourceMode.DIRECT) }
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

@Composable
private fun SourceCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) RadioButton(selected = true, onClick = null)
        }
    }
}

// ─── Credentials Screen ───────────────────────────────────────────────────────
@Composable
fun CredentialsScreen(email: String, password: String, onEmailChange: (String) -> Unit, onPasswordChange: (String) -> Unit, onNext: () -> Unit) {
    var showPassword by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("LinkedIn Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your credentials are encrypted and stored only on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("LinkedIn Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = onPasswordChange, label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) {
                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }},
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, enabled = email.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Job Board Selection Screen ───────────────────────────────────────────────
@Composable
fun JobBoardSelectionScreen(selected: List<JobBoardSource>, onToggle: (JobBoardSource) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Select Job Boards", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Pick which sites to scan for jobs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(JobBoardSource.entries.size) { idx ->
                val board = JobBoardSource.entries[idx]
                Row(modifier = Modifier.fillMaxWidth().clickable { onToggle(board) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected.contains(board), onCheckedChange = { onToggle(board) })
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(board.displayName, style = MaterialTheme.typography.bodyLarge)
                        Text(board.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(onClick = onNext, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Personal Info Screen ─────────────────────────────────────────────────────
@Composable
fun PersonalInfoScreen(
    firstName: String, lastName: String, phone: String,
    city: String, country: String, linkedInUrl: String,
    currentJobTitle: String, yearsOfExperience: Int,
    onSave: (String, String, String, String, String, String, String, Int) -> Unit,
    onNext: () -> Unit
) {
    var fn by remember { mutableStateOf(firstName) }
    var ln by remember { mutableStateOf(lastName) }
    var ph by remember { mutableStateOf(phone) }
    var ct by remember { mutableStateOf(city) }
    var co by remember { mutableStateOf(country) }
    var li by remember { mutableStateOf(linkedInUrl) }
    var jt by remember { mutableStateOf(currentJobTitle) }
    var yoe by remember { mutableStateOf(if (yearsOfExperience > 0) yearsOfExperience.toString() else "") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Personal Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Used to auto-fill application forms (name, phone, LinkedIn URL, etc.).",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = fn, onValueChange = { fn = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
                label = { Text("First Name") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = ln, onValueChange = { ln = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
                label = { Text("Last Name") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = ph, onValueChange = { ph = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
            label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            placeholder = { Text("+1 555 000 0000") })
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = ct, onValueChange = { ct = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
                label = { Text("City") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = co, onValueChange = { co = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
                label = { Text("Country") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = li, onValueChange = { li = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
            label = { Text("LinkedIn Profile URL") }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://linkedin.com/in/yourname") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = jt, onValueChange = { jt = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
            label = { Text("Current / Most Recent Job Title") }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Senior Software Engineer") })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = yoe, onValueChange = { yoe = it; onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0) },
            label = { Text("Years of Experience") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onSave(fn, ln, ph, ct, co, li, jt, yoe.toIntOrNull() ?: 0); onNext() },
            enabled = fn.isNotBlank() && ln.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Continue") }
    }
}

// ─── Job Preferences Screen ───────────────────────────────────────────────────
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun JobPreferencesScreen(
    keywords: List<String>, location: String, remoteOnly: Boolean, hybridOk: Boolean, onsiteOk: Boolean,
    minSalary: Int = 0,
    onKeywordsChange: (List<String>) -> Unit, onLocationChange: (String) -> Unit,
    onRemoteChange: (Boolean) -> Unit, onHybridChange: (Boolean) -> Unit, onOnsiteChange: (Boolean) -> Unit,
    onMinSalaryChange: (Int) -> Unit = {},
    onNext: () -> Unit
) {
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
    var keywordInput by remember { mutableStateOf(keywords.joinToString(", ")) }
    var salaryExpanded by remember { mutableStateOf(false) }
    val selectedSalaryLabel = salaryOptions.firstOrNull { it.first == minSalary }?.second ?: "No minimum"

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Job Preferences", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = keywordInput, onValueChange = {
            keywordInput = it
            onKeywordsChange(it.split(",").map { k -> k.trim() }.filter { k -> k.isNotBlank() })
        }, label = { Text("Job Titles / Keywords") }, placeholder = { Text("e.g. Software Engineer, Android Developer") },
            modifier = Modifier.fillMaxWidth(), supportingText = { Text("Separate multiple keywords with commas") })
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = location, onValueChange = onLocationChange, label = { Text("Location") },
            placeholder = { Text("e.g. New York, NY or London, UK") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("Work Style", style = MaterialTheme.typography.titleSmall)
        ToggleRow("Remote Only", checked = remoteOnly, onCheckedChange = onRemoteChange)
        if (!remoteOnly) {
            ToggleRow("Hybrid OK", checked = hybridOk, onCheckedChange = onHybridChange)
            ToggleRow("On-site OK", checked = onsiteOk, onCheckedChange = onOnsiteChange)
        }
        Spacer(Modifier.height(16.dp))
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
                        onMinSalaryChange(value); salaryExpanded = false
                    })
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, enabled = keywords.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Exclusions Screen ────────────────────────────────────────────────────────
@Composable
fun ExclusionsScreen(
    excludeKeywords: List<String>, excludeCompanies: List<String>,
    onExcludeKeywordsChange: (List<String>) -> Unit, onExcludeCompaniesChange: (List<String>) -> Unit,
    onNext: () -> Unit
) {
    var kwInput by remember { mutableStateOf(excludeKeywords.joinToString(", ")) }
    var coInput by remember { mutableStateOf(excludeCompanies.joinToString(", ")) }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Exclusions (Optional)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Skip jobs with these keywords or from these companies.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = kwInput, onValueChange = {
            kwInput = it
            onExcludeKeywordsChange(it.split(",").map { k -> k.trim() }.filter { k -> k.isNotBlank() })
        }, label = { Text("Exclude keywords in job title") }, placeholder = { Text("e.g. Senior, Manager, Intern") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = coInput, onValueChange = {
            coInput = it
            onExcludeCompaniesChange(it.split(",").map { k -> k.trim() }.filter { k -> k.isNotBlank() })
        }, label = { Text("Exclude companies") }, placeholder = { Text("e.g. Acme Corp, Bad Co") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Experience Bio Screen ────────────────────────────────────────────────────
@Composable
fun ExperienceBioScreen(bio: String, onBioChange: (String) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Your Background", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Claude uses this to answer screening questions on your behalf. Be specific about your experience, skills, and accomplishments.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = bio, onValueChange = onBioChange,
            label = { Text("Professional Background") },
            placeholder = { Text("e.g. 5 years of Android development using Kotlin and Jetpack Compose. Led a team of 3 at XYZ Corp building financial apps with 500k+ users. Strong in MVVM, Coroutines, and CI/CD pipelines...") },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            maxLines = 15
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, enabled = bio.length >= 50, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Claude Setup Screen ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaudeSetupScreen(
    apiKey: String, persona: ClaudePersona,
    onApiKeyChange: (String) -> Unit, onPersonaChange: (ClaudePersona) -> Unit,
    onNext: () -> Unit
) {
    var showKey by remember { mutableStateOf(false) }
    var styleNotes by remember { mutableStateOf(persona.styleNotes) }
    var avoidInput by remember { mutableStateOf(persona.avoidPhrases.joinToString(", ")) }
    var customInstructions by remember { mutableStateOf(persona.customInstructions) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Claude AI Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Claude answers screening questions in your voice.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = apiKey, onValueChange = onApiKeyChange,
            label = { Text("Anthropic API Key") }, placeholder = { Text("sk-ant-...") },
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showKey = !showKey }) {
                Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
            }}, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Text("Writing Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = persona.tone.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tone") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PersonaTone.entries.forEach { tone ->
                    DropdownMenuItem(
                        text = { Text(tone.displayName) },
                        onClick = { onPersonaChange(persona.copy(tone = tone)); expanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = styleNotes, onValueChange = {
            styleNotes = it; onPersonaChange(persona.copy(styleNotes = it))
        }, label = { Text("Style Notes (optional)") }, placeholder = { Text("e.g. Be concise. Show enthusiasm. Use examples.") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = avoidInput, onValueChange = {
            avoidInput = it
            onPersonaChange(persona.copy(avoidPhrases = it.split(",").map { p -> p.trim() }.filter { p -> p.isNotBlank() }))
        }, label = { Text("Phrases to avoid") }, placeholder = { Text("passionate about, synergy, team player") }, modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Comma-separated") })

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = customInstructions, onValueChange = {
            customInstructions = it; onPersonaChange(persona.copy(customInstructions = it))
        }, label = { Text("Custom Instructions (optional)") }, placeholder = { Text("e.g. Always mention my Python experience.") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, enabled = apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Resume Screen ────────────────────────────────────────────────────────────
@Composable
fun ResumeScreen(resumeFileName: String, onResumePicked: (Uri) -> Unit, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onResumePicked(it) }
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Your Resume", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Upload your resume PDF. It will be used for external application forms.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (resumeFileName.isBlank()) "Select PDF Resume" else "Change Resume")
        }
        if (resumeFileName.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(resumeFileName, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, enabled = resumeFileName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

// ─── Setup Complete Screen ────────────────────────────────────────────────────
@Composable
fun SetupCompleteScreen(prefs: com.linkedinautomation.domain.model.UserPreferences, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null,
            modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(24.dp))
        Text("All Set!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Your automation is configured.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        SummaryRow("Mode", if (prefs.sourceMode == SourceMode.LINKEDIN) "LinkedIn" else "Direct (Job Boards)")
        SummaryRow("Job Keywords", prefs.jobKeywords.take(3).joinToString(", "))
        SummaryRow("Location", prefs.location.ifBlank { "Any" })
        SummaryRow("Claude Tone", prefs.claudePersona.tone.displayName)
        SummaryRow("Resume", prefs.resumeFileName)

        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Automation")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(0.4f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(0.6f), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}
