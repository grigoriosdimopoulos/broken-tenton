package com.linkedinautomation.automation.orchestrator

import android.content.Context
import android.util.Log
import com.linkedinautomation.automation.ai.ClaudeAnswerGenerator
import com.linkedinautomation.automation.engine.AutomationWebEngine
import com.linkedinautomation.automation.engine.UrlAllowlist
import com.linkedinautomation.automation.scripts.JsScriptLoader
import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.automation.sources.*
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.domain.repository.ActivityLogRepository
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jsLoader: JsScriptLoader,
    private val claudeGenerator: ClaudeAnswerGenerator,
    private val jobRepo: JobApplicationRepository,
    private val activityRepo: ActivityLogRepository
) {
    private val TAG = "AutomationOrchestrator"
    private val _state = MutableStateFlow<AutomationState>(AutomationState.Idle)
    val state: StateFlow<AutomationState> = _state

    private val _liveLog = MutableStateFlow<List<String>>(emptyList())
    val liveLog: StateFlow<List<String>> = _liveLog

    private var resumePath: String? = null
    var onNotifyApplied: ((String, String) -> Unit)? = null
    var onNotifyPendingApproval: ((Int) -> Unit)? = null

    suspend fun run(prefs: UserPreferences, resumeFilePath: String?) {
        if (_state.value is AutomationState.Running) return
        resumePath = resumeFilePath

        log("Starting automation run (${prefs.sourceMode.name})")
        activityRepo.log(ActivityAction.RUN_STARTED, "Mode: ${prefs.sourceMode.name}")
        _state.value = AutomationState.Running("Initializing...")

        var engine: AutomationWebEngine? = null
        try {
            engine = createEngine(prefs)
            engine.create()

            val sources = getSources(prefs)
            val allJobs = mutableListOf<ScrapedJob>()

            // LinkedIn: login first
            if (prefs.sourceMode == SourceMode.LINKEDIN) {
                performLogin(engine, prefs)
            }

            // Scan each source
            for (source in sources) {
                _state.value = AutomationState.Running("Scanning ${source.sourceName}...")
                log("Scanning ${source.sourceName}...")
                val searchUrl = source.buildSearchUrl(prefs)
                try {
                    engine.navigateTo(searchUrl, 20_000)
                    delay(2000)
                    val script = jsLoader.load(source.scanScriptAsset)
                    val result = engine.runJs("scan_jobs", script, 15_000)
                    val jobs = parseJobList(result, source.sourceName)
                    val newJobs = jobs.filter { !jobRepo.existsByJobId(it.id) }
                    log("${source.sourceName}: found ${jobs.size} jobs, ${newJobs.size} new")
                    activityRepo.log(
                        ActivityAction.SEARCH_EXECUTED,
                        "${source.sourceName}: ${newJobs.size} new jobs",
                        searchUrl
                    )
                    allJobs.addAll(newJobs)
                } catch (e: Exception) {
                    log("Error scanning ${source.sourceName}: ${e.message}")
                }
            }

            // Filter excluded
            val filteredJobs = allJobs.filter { job ->
                val titleLower = job.title.lowercase()
                val companyLower = job.company.lowercase()
                prefs.excludeKeywords.none { titleLower.contains(it.lowercase()) } &&
                prefs.excludeCompanies.none { companyLower.contains(it.lowercase()) }
            }

            log("Processing ${filteredJobs.size} jobs...")
            var appliedCount = 0
            var queuedCount = 0

            for (job in filteredJobs) {
                if (_state.value is AutomationState.Paused) break
                try {
                    if (prefs.requireApproval) {
                        // Stage for approval
                        jobRepo.save(
                            JobApplication(
                                jobId = job.id,
                                title = job.title,
                                company = job.company,
                                jobUrl = job.url,
                                applicationType = if (job.isEasyApply) ApplicationType.EasyApply
                                else ApplicationType.External(UrlAllowlist.detectAtsName(job.url), job.url),
                                source = job.source,
                                status = ApplicationStatus.PENDING_APPROVAL,
                                appliedAt = System.currentTimeMillis()
                            )
                        )
                        queuedCount++
                    } else {
                        val success = applyToJob(engine, job, prefs)
                        if (success) {
                            appliedCount++
                            onNotifyApplied?.invoke(job.title, job.company)
                        }
                    }
                } catch (e: Exception) {
                    log("Error on job ${job.title}: ${e.message}")
                    recordFailed(job, e.message)
                }
                delay(1500)
            }

            if (queuedCount > 0) onNotifyPendingApproval?.invoke(queuedCount)

            val summary = "Run complete — Applied: $appliedCount, Queued: $queuedCount, Skipped: ${allJobs.size - filteredJobs.size}"
            log(summary)
            activityRepo.log(ActivityAction.RUN_COMPLETED, summary)
            _state.value = AutomationState.Idle

        } catch (e: Exception) {
            Log.e(TAG, "Orchestrator error", e)
            log("Error: ${e.message}")
            activityRepo.log(ActivityAction.APPLICATION_FAILED, "Run error: ${e.message}")
            _state.value = AutomationState.Error(e.message ?: "Unknown error")
        } finally {
            engine?.destroy()
        }
    }

    suspend fun applyApproved(jobId: Long, prefs: UserPreferences, resumeFilePath: String?) {
        resumePath = resumeFilePath
        val job = jobRepo.getById(jobId) ?: return
        val engine = createEngine(prefs)
        engine.create()
        try {
            if (prefs.sourceMode == SourceMode.LINKEDIN) performLogin(engine, prefs)
            val scraped = ScrapedJob(
                id = job.jobId, title = job.title, company = job.company,
                isEasyApply = job.applicationType is ApplicationType.EasyApply,
                url = job.jobUrl, source = job.source
            )
            val success = applyToJob(engine, scraped, prefs)
            if (success) {
                jobRepo.updateStatus(jobId, ApplicationStatus.APPLIED)
                onNotifyApplied?.invoke(job.title, job.company)
            } else {
                jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
            }
        } finally {
            engine.destroy()
        }
    }

    fun pause() { _state.value = AutomationState.Paused }
    fun resume() { if (_state.value is AutomationState.Paused) _state.value = AutomationState.Idle }

    private suspend fun performLogin(engine: AutomationWebEngine, prefs: UserPreferences) {
        _state.value = AutomationState.Running("Logging in to LinkedIn...")
        log("Logging in to LinkedIn...")
        activityRepo.log(ActivityAction.LOGIN_ATTEMPT, "LinkedIn login attempt")
        engine.navigateTo("https://www.linkedin.com/login", 15_000)
        delay(1000)
        val script = jsLoader.loadAndSubstitute(
            ScriptRegistry.LINKEDIN_LOGIN,
            mapOf("EMAIL" to prefs.linkedInEmail, "PASSWORD" to prefs.linkedInPassword)
        )
        val result = engine.runJs("login", script, 10_000)
        if (result.contains("error", ignoreCase = true)) {
            activityRepo.log(ActivityAction.LOGIN_FAILURE, result)
            throw RuntimeException("LinkedIn login failed: $result")
        }
        delay(3000) // wait for redirect
        activityRepo.log(ActivityAction.LOGIN_SUCCESS, "Login successful")
        log("Login successful")
    }

    private suspend fun applyToJob(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Boolean {
        log("Applying to: ${job.title} @ ${job.company}")
        activityRepo.log(ActivityAction.JOB_VIEWED, "${job.title} at ${job.company}", job.url)

        engine.navigateTo(job.url, 15_000)
        delay(2000)

        return if (job.isEasyApply) {
            performEasyApply(engine, job, prefs)
        } else {
            performExternalApply(engine, job, prefs)
        }
    }

    private suspend fun performEasyApply(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Boolean {
        activityRepo.log(ActivityAction.EASY_APPLY_STARTED, "${job.title} at ${job.company}", job.url)
        var attempts = 0
        val maxPages = 10
        while (attempts < maxPages) {
            attempts++
            val script = jsLoader.load(ScriptRegistry.LINKEDIN_EASY_APPLY)
            val result = engine.runJs("easy_apply", script, 10_000)

            val data = parseJson(result) ?: break

            when (data["action"] as? String) {
                "external" -> {
                    val url = data["url"] as? String ?: return false
                    return performExternalApply(engine, job.copy(url = url, isEasyApply = false), prefs)
                }
                "opened_modal" -> { delay(1000); continue }
                "form_page" -> {
                    // Handle screening questions
                    fillScreeningQuestions(engine, job, prefs)
                    delay(500)

                    // Determine next action
                    val hasSubmit = data["hasSubmit"] as? Boolean ?: false
                    val actionScript = jsLoader.loadAndSubstitute(
                        ScriptRegistry.LINKEDIN_SUBMIT,
                        mapOf("ACTION" to if (hasSubmit) "submit" else "next", "FILL_ID" to "", "FILL_VALUE" to "")
                    )
                    val actionResult = engine.runJs("submit", actionScript, 8_000)
                    if (actionResult == "submitted" || actionResult.contains("submitted")) {
                        delay(2000)
                        recordApplied(job, ApplicationType.EasyApply)
                        activityRepo.log(ActivityAction.EASY_APPLY_SUBMITTED, "${job.title} at ${job.company}")
                        log("Applied (Easy Apply): ${job.title} @ ${job.company}")
                        return true
                    }
                    delay(1500)
                }
                else -> break
            }
        }
        recordFailed(job, "Easy Apply did not complete")
        return false
    }

    private suspend fun performExternalApply(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Boolean {
        // If the job URL is a LinkedIn page, extract the real ATS apply URL first
        var applyUrl = job.url
        if (applyUrl.contains("linkedin.com")) {
            engine.navigateTo(applyUrl, 15_000)
            delay(1500)
            val extractScript = jsLoader.load(ScriptRegistry.LINKEDIN_GET_EXTERNAL_URL)
            val extracted = runCatching { engine.runJs("get_ext_url", extractScript, 8_000) }.getOrNull()
            if (!extracted.isNullOrBlank() && extracted.startsWith("http") && !extracted.contains("linkedin.com")) {
                applyUrl = extracted
                log("Extracted external apply URL: $applyUrl")
            } else {
                log("Could not extract external URL from LinkedIn page, trying job.url directly")
            }
        }

        activityRepo.log(ActivityAction.EXTERNAL_URL_OPENED, "${job.title} at ${job.company}", applyUrl)
        // Enable apply mode to bypass URL allowlist for ATS navigation
        engine.enableApplyMode()
        try {
            engine.navigateTo(applyUrl, 20_000)
            delay(2000)
        } finally {
            engine.disableApplyMode()
        }

        val script = jsLoader.load(ScriptRegistry.EXTERNAL_APPLY_DETECT)
        val result = engine.runJs("external_detect", script, 10_000)
        val data = parseJson(result) ?: return false

        @Suppress("UNCHECKED_CAST")
        val fields = data["fields"] as? List<Map<String, Any>> ?: emptyList()
        fillExternalFormFields(engine, fields, prefs)
        fillScreeningQuestions(engine, job, prefs)
        delay(500)

        // Fill resume via file chooser (handled by engine callback)
        activityRepo.log(ActivityAction.RESUME_UPLOADED, "Resume provided for ${job.title}")

        // Click submit
        @Suppress("UNCHECKED_CAST")
        val submitBtns = data["submitButtons"] as? List<Map<String, Any>> ?: emptyList()
        val submitScript = if (submitBtns.isNotEmpty()) {
            val btnId = submitBtns.first()["id"] as? String ?: ""
            if (btnId.isNotBlank()) {
                "document.getElementById('$btnId')?.click(); AndroidBridge.onResult('ext_submit', 'clicked');"
            } else {
                "var btns = document.querySelectorAll('button[type=submit], input[type=submit]'); if(btns.length>0){btns[0].click(); AndroidBridge.onResult('ext_submit','clicked');}else{AndroidBridge.onError('ext_submit','No submit button');}"
            }
        } else {
            "var btns = document.querySelectorAll('button[type=submit], input[type=submit]'); if(btns.length>0){btns[0].click(); AndroidBridge.onResult('ext_submit','clicked');}else{AndroidBridge.onError('ext_submit','No submit button');}"
        }

        val submitResult = engine.runJs("ext_submit", submitScript, 8_000)
        if (submitResult.contains("clicked") || submitResult.contains("submit")) {
            delay(2000)
            val atsName = UrlAllowlist.detectAtsName(job.url)
            recordApplied(job, ApplicationType.External(atsName, job.url))
            activityRepo.log(ActivityAction.APPLICATION_SUBMITTED, "${job.title} at ${job.company}", job.url)
            log("Applied (External/$atsName): ${job.title} @ ${job.company}")
            return true
        }
        recordFailed(job, "External submit failed: $submitResult")
        return false
    }

    private suspend fun fillScreeningQuestions(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ) {
        val qScript = jsLoader.load(ScriptRegistry.LINKEDIN_SCREENING)
        val qResult = runCatching { engine.runJs("screening", qScript, 8_000) }.getOrNull() ?: return

        @Suppress("UNCHECKED_CAST")
        val questions = parseJsonList(qResult) ?: return
        for (q in questions) {
            val questionText = q["question"] as? String ?: continue
            val elementId = q["elementId"] as? String ?: continue
            val currentValue = q["currentValue"] as? String ?: ""
            if (currentValue.isNotBlank()) continue // already filled

            activityRepo.log(ActivityAction.SCREENING_QUESTION, "Q: $questionText (${job.title})")
            log("Answering: $questionText")

            val answer = runCatching {
                claudeGenerator.generateAnswer(
                    question = questionText,
                    userBio = prefs.experienceBio,
                    jobTitle = job.title,
                    company = job.company,
                    persona = prefs.claudePersona,
                    apiKey = prefs.claudeApiKey
                )
            }.getOrElse { "I am very interested in this opportunity and believe my experience aligns well with your requirements." }

            val fillScript = jsLoader.loadAndSubstitute(
                ScriptRegistry.LINKEDIN_SUBMIT,
                mapOf("FILL_ID" to elementId, "FILL_VALUE" to answer, "ACTION" to "")
            )
            runCatching { engine.runJs("fill_field", fillScript, 5_000) }
            delay(300)
        }
    }

    private fun fillExternalFormFields(
        engine: AutomationWebEngine,
        fields: List<Map<String, Any>>,
        prefs: UserPreferences
    ) {
        fields.forEach { field ->
            val label = ((field["label"] as? String) ?: "").lowercase()
            val id = field["id"] as? String ?: ""
            val name = ((field["name"] as? String) ?: "").lowercase()
            val type = field["type"] as? String ?: "text"

            val value = when {
                label.contains("first name") || name.contains("first_name") || name.contains("firstname") || name == "first" ->
                    prefs.firstName
                label.contains("last name") || name.contains("last_name") || name.contains("lastname") || name == "last" ->
                    prefs.lastName
                label.contains("full name") || name.contains("full_name") || name.contains("fullname") ->
                    "${prefs.firstName} ${prefs.lastName}".trim()
                label.contains("email") || type == "email" || name.contains("email") ->
                    prefs.linkedInEmail
                label.contains("phone") || type == "tel" || name.contains("phone") || name.contains("mobile") ->
                    prefs.phone
                label.contains("city") || name.contains("city") ->
                    prefs.city
                label.contains("country") || name.contains("country") ->
                    prefs.country
                (label.contains("linkedin") || name.contains("linkedin")) && !label.contains("email") ->
                    prefs.linkedInUrl
                label.contains("current title") || label.contains("job title") || name.contains("title") ->
                    prefs.currentJobTitle
                label.contains("years") && label.contains("experience") ->
                    prefs.yearsOfExperience.toString()
                label.contains("website") || label.contains("portfolio") -> ""
                else -> null
            } ?: return@forEach

            if (value.isBlank()) return@forEach

            val selector = when {
                id.isNotBlank() -> "#${id.replace("\"", "\\\"")}"
                name.isNotBlank() -> "[name='${name.replace("'", "\\'")}']"
                else -> return@forEach
            }
            val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
            val script = """
                (function(){
                  var el = document.querySelector("$selector");
                  if(el){ el.value="$escaped";
                    el.dispatchEvent(new Event('input',{bubbles:true}));
                    el.dispatchEvent(new Event('change',{bubbles:true}));
                  }
                })();
            """.trimIndent()
            engine.runJsFireAndForget(script)
        }
    }

    private suspend fun recordApplied(job: ScrapedJob, type: ApplicationType) {
        jobRepo.save(
            JobApplication(
                jobId = job.id,
                title = job.title,
                company = job.company,
                jobUrl = job.url,
                applicationType = type,
                source = job.source,
                status = ApplicationStatus.APPLIED,
                appliedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun recordFailed(job: ScrapedJob, reason: String?) {
        jobRepo.save(
            JobApplication(
                jobId = job.id,
                title = job.title,
                company = job.company,
                jobUrl = job.url,
                applicationType = ApplicationType.External("Unknown", job.url),
                source = job.source,
                status = ApplicationStatus.FAILED,
                appliedAt = System.currentTimeMillis(),
                errorMessage = reason
            )
        )
        activityRepo.log(ActivityAction.APPLICATION_FAILED, "${job.title}: $reason")
    }

    private fun getSources(prefs: UserPreferences): List<JobSource> = when (prefs.sourceMode) {
        SourceMode.LINKEDIN -> listOf(LinkedInSource())
        SourceMode.DIRECT -> prefs.selectedJobBoards.map { board ->
            when (board) {
                com.linkedinautomation.domain.model.JobBoardSource.INDEED -> IndeedSource()
                com.linkedinautomation.domain.model.JobBoardSource.GLASSDOOR -> GlassdoorSource()
                com.linkedinautomation.domain.model.JobBoardSource.ZIPRECRUITER -> ZipRecruiterSource()
                com.linkedinautomation.domain.model.JobBoardSource.MONSTER -> MonsterSource()
                com.linkedinautomation.domain.model.JobBoardSource.SIMPLYHIRED -> ZipRecruiterSource() // fallback
            }
        }
    }

    private fun createEngine(prefs: UserPreferences): AutomationWebEngine =
        AutomationWebEngine(
            context = context,
            sourceMode = prefs.sourceMode,
            onLog = { log(it) },
            onBlockedNavigation = { url ->
                kotlinx.coroutines.runBlocking {
                    activityRepo.log(ActivityAction.BLOCKED_NAVIGATION, "Blocked: $url", url)
                }
                log("BLOCKED: $url")
            },
            onFileChooserRequested = { callback ->
                val path = resumePath
                if (path != null && java.io.File(path).exists()) {
                    activityRepo.let {
                        kotlinx.coroutines.runBlocking {
                            it.log(ActivityAction.RESUME_UPLOADED, "Resume file provided")
                        }
                    }
                    callback(path)
                } else {
                    callback(null)
                }
            }
        )

    private fun log(message: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val line = "$ts  $message"
        Log.d(TAG, line)
        val current = _liveLog.value.toMutableList()
        current.add(0, line)
        if (current.size > 100) current.removeAt(current.size - 1)
        _liveLog.value = current
    }

    private val moshi = Moshi.Builder().build()

    @Suppress("UNCHECKED_CAST")
    private fun parseJobList(json: String, source: String): List<ScrapedJob> {
        val adapter = moshi.adapter<List<Map<String, Any>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        )
        return runCatching {
            adapter.fromJson(json)?.map { m ->
                ScrapedJob(
                    id = m["id"] as? String ?: "",
                    title = m["title"] as? String ?: "Unknown",
                    company = m["company"] as? String ?: "Unknown",
                    isEasyApply = m["isEasyApply"] as? Boolean ?: false,
                    url = m["url"] as? String ?: "",
                    source = source
                )
            }?.filter { it.id.isNotBlank() && it.url.isNotBlank() } ?: emptyList()
        }.getOrElse { emptyList() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJson(json: String): Map<String, Any>? {
        val adapter = moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJsonList(json: String): List<Map<String, Any>>? {
        val adapter = moshi.adapter<List<Map<String, Any>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        )
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }
}
