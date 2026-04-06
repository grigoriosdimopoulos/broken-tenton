package com.linkedinautomation.automation.orchestrator

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.linkedinautomation.automation.ai.ClaudeAnswerGenerator
import com.linkedinautomation.automation.engine.AutomationWebEngine
import com.linkedinautomation.automation.engine.UrlAllowlist
import com.linkedinautomation.automation.scripts.JsScriptLoader
import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.automation.sources.*
import com.linkedinautomation.automation.sources.DiceSource
import com.linkedinautomation.automation.sources.RemoteOKSource
import com.linkedinautomation.automation.sources.WeWorkRemotelySource
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
    private val MAX_EASY_APPLY_ATTEMPTS = 5
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

            // Filter excluded + location mismatch + easy-apply-only mode
            val filteredJobs = allJobs.filter { job ->
                val titleLower = job.title.lowercase()
                val companyLower = job.company.lowercase()
                if (prefs.easyApplyOnly && !job.isEasyApply) {
                    log("SKIP [Easy Apply Only] ${job.title} @ ${job.company}")
                    return@filter false
                }
                if (prefs.excludeKeywords.any { titleLower.contains(it.lowercase()) }) {
                    log("SKIP [Excluded keyword] ${job.title} @ ${job.company}")
                    return@filter false
                }
                if (prefs.excludeCompanies.any { companyLower.contains(it.lowercase()) }) {
                    log("SKIP [Excluded company] ${job.company}")
                    return@filter false
                }
                val locOk = locationMatches(job, prefs)
                if (!locOk) {
                    val reason = if (job.location.isBlank()) "no location extracted" else "location='${job.location}'"
                    log("SKIP [Location mismatch] ${job.title} @ ${job.company} — $reason (want '${prefs.location}')")
                    activityRepo.log(
                        ActivityAction.LOCATION_SKIPPED,
                        "${job.title} @ ${job.company}\nScraped location: ${job.location.ifBlank { "(blank)" }}\nWanted: ${prefs.location}",
                        job.url
                    )
                }
                locOk
            }

            val skippedLocation = allJobs.size - filteredJobs.size
            if (skippedLocation > 0) log("Filtered $skippedLocation/${allJobs.size} jobs — see SKIP lines above for details")

            log("Processing ${filteredJobs.size} jobs...")
            var appliedCount = 0
            var queuedCount = 0

            val screenshotsDir = java.io.File(context.filesDir, "screenshots")

            for (job in filteredJobs) {
                if (_state.value is AutomationState.Paused) break
                try {
                    if (prefs.requireApproval) {
                        // Stage for approval — save with location so the queue shows where the job is
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
                                appliedAt = System.currentTimeMillis(),
                                location = job.location.ifBlank { null }
                            )
                        )
                        queuedCount++
                    } else {
                        val success = applyToJob(engine, job, prefs)
                        // Screenshot of the final page (confirmation or error)
                        val ssPath = engine?.takeScreenshot("job_${job.id.take(12)}", screenshotsDir)
                        // Attach screenshot to the most recent activity entry (the submit/fail log)
                        if (ssPath != null) {
                            activityRepo.log(
                                if (success) ActivityAction.EASY_APPLY_SUBMITTED else ActivityAction.EASY_APPLY_FAILED,
                                if (success) "Applied: ${job.title} @ ${job.company}"
                                else "Failed: ${job.title} @ ${job.company}",
                                job.url,
                                ssPath
                            )
                            jobRepo.getByJobId(job.id)?.let { saved ->
                                jobRepo.save(saved.copy(screenshotPath = ssPath))
                            }
                        }
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
        if (prefs.linkedInCookies.isBlank()) {
            activityRepo.log(ActivityAction.LOGIN_FAILURE, "No LinkedIn session cookies stored. Please sign in via Settings → Account.")
            throw RuntimeException("No LinkedIn cookies — open Settings → Account to sign in")
        }

        _state.value = AutomationState.Running("Restoring LinkedIn session...")
        log("Injecting LinkedIn session cookies...")
        activityRepo.log(ActivityAction.LOGIN_ATTEMPT, "Injecting stored LinkedIn cookies")

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        // Inject each cookie for .linkedin.com
        prefs.linkedInCookies.split(";").map { it.trim() }.filter { it.isNotBlank() }.forEach { cookie ->
            cookieManager.setCookie(".linkedin.com", cookie)
            cookieManager.setCookie("https://www.linkedin.com", cookie)
        }
        cookieManager.flush()

        // Navigate to LinkedIn feed to verify the session is actually active
        val landedUrl = engine.navigateTo("https://www.linkedin.com/feed/", 15_000)
        delay(2000)

        // If we ended up back on a login/checkpoint page the cookies are expired
        val isLoginPage = landedUrl.contains("/login") ||
            landedUrl.contains("/uas/login") ||
            landedUrl.contains("/checkpoint/lg/")
        if (isLoginPage) {
            activityRepo.log(
                ActivityAction.LOGIN_FAILURE,
                "LinkedIn session expired — please re-authenticate in Settings → Account"
            )
            throw RuntimeException("LinkedIn session expired. Open the app and go to Settings → Account to sign in again.")
        }

        activityRepo.log(
            ActivityAction.LOGIN_SUCCESS,
            "LinkedIn session active\nLanded URL: $landedUrl\nCookies present: ${prefs.linkedInCookies.take(80)}…"
        )
        log("LinkedIn session active (landed: $landedUrl)")
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

    /**
     * Attempts LinkedIn Easy Apply up to [MAX_EASY_APPLY_ATTEMPTS] times.
     *
     * Each "attempt" is one full pass through the multi-page Easy Apply modal.
     * If all attempts fail AND [UserPreferences.aiAssistFallback] is enabled the
     * orchestrator calls Claude to analyse the last-seen form and generate answers,
     * then makes one final try with those AI-generated answers.
     *
     * Key fix vs previous version: after clicking Submit we run a modal-presence
     * check (linkedin_modal_check.js). If the modal is still open the application
     * was NOT submitted (validation error); we do NOT record it as applied.
     */
    private suspend fun performEasyApply(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Boolean {
        activityRepo.log(ActivityAction.EASY_APPLY_STARTED, "${job.title} at ${job.company}", job.url)

        var lastFailReason = "Easy Apply did not complete"
        var aiAnswers: Map<String, String> = emptyMap() // populated by AI assist on final attempt

        for (attempt in 1..MAX_EASY_APPLY_ATTEMPTS) {
            // On every retry, re-navigate to the job page so the modal resets
            if (attempt > 1) {
                log("Easy Apply attempt $attempt/${MAX_EASY_APPLY_ATTEMPTS} for ${job.title}")
                engine.navigateTo(job.url, 15_000)
                delay(2000)
            }

            val success = runEasyApplyPass(engine, job, prefs, aiAnswers)
            if (success != null) {
                // success == true  → confirmed submitted
                // success == false → submitted button clicked but modal stayed open (validation error)
                if (success) return true
                lastFailReason = "Submit clicked but form validation failed (modal stayed open)"
            }
            // null = couldn't complete the form; try again
        }

        // All attempts exhausted — try AI assist if enabled
        if (prefs.aiAssistFallback && prefs.claudeApiKey.isNotBlank()) {
            log("All Easy Apply attempts failed — trying AI assist fallback for ${job.title}")
            activityRepo.log(ActivityAction.SCREENING_QUESTION, "AI assist fallback triggered for ${job.title}")

            aiAnswers = generateAiAnswersForForm(engine, job, prefs)
            if (aiAnswers.isNotEmpty()) {
                engine.navigateTo(job.url, 15_000)
                delay(2000)
                val aiSuccess = runEasyApplyPass(engine, job, prefs, aiAnswers)
                if (aiSuccess == true) return true
            }
        }

        recordFailed(job, lastFailReason)
        return false
    }

    /**
     * One complete pass through the Easy Apply modal pages.
     * Returns:
     *   true  → confirmed submitted (modal closed, success text visible)
     *   false → submit was clicked but modal stayed open (validation error)
     *   null  → could not complete the form (no submit/next button found or JS error)
     */
    private suspend fun runEasyApplyPass(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences,
        aiAnswers: Map<String, String>
    ): Boolean? {
        val maxPages = 15 // maximum form pages within a single attempt
        var pagesNavigated = 0

        while (pagesNavigated < maxPages) {
            pagesNavigated++
            val rawResult = runCatching {
                engine.runJs("easy_apply", jsLoader.load(ScriptRegistry.LINKEDIN_EASY_APPLY), 10_000)
            }.getOrNull() ?: run {
                log("Easy Apply JS timed out / error on page $pagesNavigated")
                return null
            }

            log("Easy Apply page $pagesNavigated raw result: ${rawResult.take(200)}")
            activityRepo.log(
                ActivityAction.EASY_APPLY_STEP,
                "Page $pagesNavigated — JS result: ${rawResult.take(300)}",
                job.url
            )

            val data = parseJson(rawResult) ?: run {
                log("Easy Apply: could not parse JSON on page $pagesNavigated")
                return null
            }

            when (val action = data["action"] as? String) {
                "external" -> {
                    val url = data["url"] as? String ?: return null
                    log("Easy Apply: redirecting to external URL: $url")
                    return if (performExternalApply(engine, job.copy(url = url, isEasyApply = false), prefs)) true else null
                }
                "opened_modal" -> {
                    log("Easy Apply: modal opened, waiting...")
                    delay(1200)
                    continue
                }
                "form_page" -> {
                    val hasSubmit = data["hasSubmit"] as? Boolean ?: false
                    @Suppress("UNCHECKED_CAST")
                    val fields = data["fields"] as? List<Map<String, Any>> ?: emptyList()
                    log("Easy Apply: form page, ${fields.size} fields, hasSubmit=$hasSubmit")

                    fillScreeningQuestions(engine, job, prefs, aiAnswers)
                    delay(500)

                    val btnAction = if (hasSubmit) "submit" else "next"
                    val actionScript = jsLoader.loadAndSubstitute(
                        ScriptRegistry.LINKEDIN_SUBMIT,
                        mapOf("ACTION" to btnAction, "FILL_ID" to "", "FILL_VALUE" to "")
                    )
                    val actionResult = runCatching {
                        engine.runJs("submit", actionScript, 8_000)
                    }.getOrNull() ?: run {
                        log("Easy Apply: submit/next JS timed out")
                        return null
                    }

                    log("Easy Apply: $btnAction result = $actionResult")
                    activityRepo.log(
                        ActivityAction.EASY_APPLY_STEP,
                        "Clicked $btnAction — result: $actionResult\nJob: ${job.title} @ ${job.company}",
                        job.url
                    )

                    if (actionResult.contains("error", ignoreCase = true)) {
                        log("Easy Apply: $btnAction button not found — ${actionResult.take(100)}")
                        return null
                    }

                    delay(1500)

                    if (hasSubmit) {
                        val modalCheckScript = jsLoader.load(ScriptRegistry.LINKEDIN_MODAL_CHECK)
                        val modalCheck = runCatching {
                            engine.runJs("modal_check", modalCheckScript, 6_000)
                        }.getOrNull()
                        val modalData = modalCheck?.let { parseJson(it) }
                        val modalStillOpen = modalData?.get("modalOpen") as? Boolean ?: true
                        val errors = (modalData?.get("errors") as? List<*>)?.joinToString("; ") ?: ""

                        log("Modal check: open=$modalStillOpen errors='$errors'")
                        activityRepo.log(
                            ActivityAction.MODAL_CHECK,
                            "Modal open=$modalStillOpen\nValidation errors: ${errors.ifBlank { "none" }}\nJob: ${job.title} @ ${job.company}",
                            job.url
                        )

                        return if (!modalStillOpen) {
                            delay(1500)
                            val verified = verifySubmission(engine)
                            recordApplied(job, ApplicationType.EasyApply, verified)
                            log("Applied (Easy Apply${if (!verified) " — unverified" else ""}): ${job.title}")
                            true
                        } else {
                            log("Easy Apply submit REJECTED — modal still open. Errors: $errors")
                            false
                        }
                    }
                    // Clicked "Next" — continue to next page
                }
                else -> {
                    log("Easy Apply: unknown action '$action' on page $pagesNavigated")
                    return null
                }
            }
        }
        log("Easy Apply: exceeded $maxPages page limit for ${job.title}")
        return null
    }

    /** Uses Claude to generate answers for all visible Easy Apply form fields. */
    private suspend fun generateAiAnswersForForm(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Map<String, String> {
        return runCatching {
            val qScript = jsLoader.load(ScriptRegistry.LINKEDIN_SCREENING)
            val qResult = engine.runJs("screening_ai", qScript, 8_000)
            val questions = parseJsonList(qResult) ?: return emptyMap()

            val answers = mutableMapOf<String, String>()
            for (q in questions) {
                val questionText = q["question"] as? String ?: continue
                val elementId = q["elementId"] as? String ?: continue
                val currentValue = q["currentValue"] as? String ?: ""
                if (currentValue.isNotBlank()) continue

                val answer = claudeGenerator.generateAnswer(
                    question = questionText,
                    userBio = prefs.experienceBio,
                    jobTitle = job.title,
                    company = job.company,
                    persona = prefs.claudePersona,
                    apiKey = prefs.claudeApiKey
                )
                answers[elementId] = answer
            }
            answers
        }.getOrElse { emptyMap() }
    }

    private suspend fun performExternalApply(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences
    ): Boolean {
        activityRepo.log(ActivityAction.EXTERNAL_URL_OPENED, "${job.title} at ${job.company}", job.url)

        var resolvedAtsUrl: String = if (!job.url.contains("linkedin.com")) job.url else ""

        // Try up to 3 times with different navigation / submit strategies
        for (attempt in 1..3) {
            log("External apply attempt $attempt/3: ${job.title} @ ${job.company}")
            try {
                // ── Step 1: Resolve / navigate to the real ATS page ──────────────
                when {
                    !job.url.contains("linkedin.com") -> {
                        // Already on ATS page from applyToJob navigation
                        if (attempt > 1) delay(3000) // extra wait for SPA on re-attempts
                    }
                    attempt == 1 -> {
                        // Click Apply button and follow redirect (apply mode stays on through redirect chain)
                        log("Attempt 1: clicking Apply button to follow redirect...")
                        val clickScript = jsLoader.load(ScriptRegistry.LINKEDIN_CLICK_APPLY)
                        val navigatedUrl = engine.runJsAndWaitForNavigation(clickScript, 22_000)
                        if (navigatedUrl.isNotBlank() && !navigatedUrl.contains("linkedin.com")) {
                            resolvedAtsUrl = navigatedUrl
                            delay(2000) // let SPA settle
                        } else {
                            log("Attempt 1: click did not navigate to ATS, falling through")
                            continue // skip form fill for this attempt
                        }
                    }
                    attempt == 2 -> {
                        // Extract URL from page source and navigate directly
                        log("Attempt 2: extracting ATS URL from page data...")
                        if (!engine.currentUrl.orEmpty().contains("linkedin.com"))
                            engine.navigateTo(job.url, 15_000)
                        delay(1500)
                        val extractScript = jsLoader.load(ScriptRegistry.LINKEDIN_GET_EXTERNAL_URL)
                        val extracted = runCatching { engine.runJs("get_ext_url", extractScript, 8_000) }.getOrElse { "" }
                        if (extracted.isNotBlank() && extracted.startsWith("http") && !extracted.contains("linkedin.com")) {
                            resolvedAtsUrl = extracted
                            engine.enableApplyMode()
                            try { engine.navigateTo(extracted, 20_000) } finally { engine.disableApplyMode() }
                            delay(3000) // SPAs need time to render
                        } else if (resolvedAtsUrl.isBlank()) {
                            log("Attempt 2: could not extract ATS URL"); continue
                        } else {
                            // Re-use URL from attempt 1 if we somehow ended up on ATS
                            delay(3000)
                        }
                    }
                    else -> {
                        // Attempt 3: re-navigate to resolved URL with extra wait
                        if (resolvedAtsUrl.isNotBlank() && !resolvedAtsUrl.contains("linkedin.com")) {
                            engine.enableApplyMode()
                            try { engine.navigateTo(resolvedAtsUrl, 20_000) } finally { engine.disableApplyMode() }
                        }
                        delay(5000) // heaviest wait — gives Workday/Angular time to load
                    }
                }

                val currentAtsUrl = resolvedAtsUrl.ifBlank { engine.currentUrl ?: job.url }

                // ── Step 2: Detect + fill form ────────────────────────────────────
                val detectScript = jsLoader.load(ScriptRegistry.EXTERNAL_APPLY_DETECT)
                val detectResult = runCatching { engine.runJs("ext_detect_$attempt", detectScript, 12_000) }.getOrElse { "{}" }
                val data = parseJson(detectResult) ?: emptyMap<String, Any>()

                @Suppress("UNCHECKED_CAST")
                val fields = data["fields"] as? List<Map<String, Any>> ?: emptyList()
                fillExternalFormFields(engine, fields, prefs)
                fillScreeningQuestions(engine, job, prefs)
                delay(600)
                activityRepo.log(ActivityAction.RESUME_UPLOADED, "Resume provided for ${job.title}")

                // ── Step 3: Submit with attempt-specific strategy ─────────────────
                @Suppress("UNCHECKED_CAST")
                val submitBtns = data["submitButtons"] as? List<Map<String, Any>> ?: emptyList()
                val submitScript = buildExternalSubmitScript(submitBtns, attempt)
                val submitResult = runCatching { engine.runJs("ext_submit_$attempt", submitScript, 10_000) }.getOrElse { "" }

                if (submitResult.contains("clicked", ignoreCase = true) ||
                    submitResult.contains("submit", ignoreCase = true)) {
                    delay(2000)
                    val verified = verifySubmission(engine)
                    val atsName = UrlAllowlist.detectAtsName(currentAtsUrl)
                    recordApplied(job, ApplicationType.External(atsName, currentAtsUrl), verified)
                    activityRepo.log(ActivityAction.APPLICATION_SUBMITTED, "${job.title} at ${job.company}", currentAtsUrl)
                    log("Applied (External/$atsName${if (!verified) " — unverified" else ""}) attempt $attempt: ${job.title} @ ${job.company}")
                    return true
                }
                log("Attempt $attempt submit result: $submitResult — retrying...")
                delay(2000)

            } catch (e: Exception) {
                log("Attempt $attempt error: ${e.message}")
                delay(2000)
            }
        }

        recordFailed(job, "External apply failed after 3 attempts — no submit button found on ATS page")
        return false
    }

    /** Builds an increasingly aggressive submit-button script for each attempt. */
    private fun buildExternalSubmitScript(
        submitBtns: List<Map<String, Any>>,
        attempt: Int
    ): String {
        val firstId = (submitBtns.firstOrNull()?.get("id") as? String)?.takeIf { it.isNotBlank() }
        val idClause = if (firstId != null)
            "var byId = document.getElementById('${firstId.replace("'","\\'")}'); if(byId && byId.offsetParent!==null){ byId.click(); AndroidBridge.onResult('ext_submit_$attempt','clicked_id'); return; }"
        else ""

        return when (attempt) {
            1 -> """
                (function(){
                  $idClause
                  var s = document.querySelector('button[type=submit], input[type=submit]');
                  if(s){ s.click(); AndroidBridge.onResult('ext_submit_1','clicked_std'); return; }
                  AndroidBridge.onError('ext_submit_1','No submit button');
                })();
            """.trimIndent()

            2 -> """
                (function(){
                  $idClause
                  // Workday
                  var wd = document.querySelector(
                    '[data-automation-id="bottom-navigation-next-button"],' +
                    '[data-automation-id="bottom-navigation-send-it-button"],' +
                    '[data-automation-id="pageFooter"] button');
                  if(wd){ wd.click(); AndroidBridge.onResult('ext_submit_2','clicked_workday'); return; }
                  // Greenhouse / Lever / SmartRecruiters
                  var ats = document.querySelector('#submit_app, #app-submit-btn, .btn-submit, [data-qa="btn-submit"], [data-testid="submit-app-button"]');
                  if(ats){ ats.click(); AndroidBridge.onResult('ext_submit_2','clicked_ats'); return; }
                  // type=submit
                  var s = document.querySelector('[type=submit]');
                  if(s){ s.click(); AndroidBridge.onResult('ext_submit_2','clicked_type'); return; }
                  // Any button with submit/apply text
                  var btns = Array.from(document.querySelectorAll('button')).filter(function(b){
                    return /submit|apply|send/i.test(b.textContent) && b.offsetParent!==null && !b.disabled;
                  });
                  if(btns.length>0){ btns[0].click(); AndroidBridge.onResult('ext_submit_2','clicked_text'); return; }
                  AndroidBridge.onError('ext_submit_2','No submit button attempt 2');
                })();
            """.trimIndent()

            else -> """
                (function(){
                  $idClause
                  // All previous strategies
                  var sel = '[data-automation-id="bottom-navigation-next-button"],[data-automation-id="bottom-navigation-send-it-button"],' +
                    '#submit_app,#app-submit-btn,.btn-submit,[type=submit],button[class*=submit],button[class*=apply]';
                  var el = document.querySelector(sel);
                  if(el){ el.click(); AndroidBridge.onResult('ext_submit_3','clicked_broad'); return; }
                  // Any visible button with submit/apply/next/continue/send text
                  var btns = Array.from(document.querySelectorAll('button')).filter(function(b){
                    return /submit|apply|send|next|continue/i.test(b.textContent) && b.offsetParent!==null && !b.disabled;
                  });
                  if(btns.length>0){ btns[0].click(); AndroidBridge.onResult('ext_submit_3','clicked_any'); return; }
                  // Last resort: form.submit()
                  var forms = document.querySelectorAll('form');
                  if(forms.length>0){
                    try{ forms[forms.length-1].submit(); AndroidBridge.onResult('ext_submit_3','form_submit'); return; } catch(e){}
                  }
                  AndroidBridge.onError('ext_submit_3','Exhausted all submit strategies');
                })();
            """.trimIndent()
        }
    }

    private suspend fun fillScreeningQuestions(
        engine: AutomationWebEngine,
        job: ScrapedJob,
        prefs: UserPreferences,
        aiAnswers: Map<String, String> = emptyMap()
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

            // Use AI-generated answer from fallback pass if available; otherwise ask Claude now
            val answer = aiAnswers[elementId] ?: run {
                activityRepo.log(ActivityAction.SCREENING_QUESTION, "Q: $questionText (${job.title})")
                log("Answering: $questionText")
                runCatching {
                    claudeGenerator.generateAnswer(
                        question = questionText,
                        userBio = prefs.experienceBio,
                        jobTitle = job.title,
                        company = job.company,
                        persona = prefs.claudePersona,
                        apiKey = prefs.claudeApiKey
                    )
                }.getOrElse { "I am very interested in this opportunity and believe my experience aligns well with your requirements." }
            }

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

    private suspend fun verifySubmission(engine: AutomationWebEngine): Boolean {
        return runCatching {
            val script = jsLoader.load(ScriptRegistry.VERIFY_SUBMISSION)
            val result = engine.runJs("verify_submit", script, 6_000)
            val data = parseJson(result)
            val verified = data?.get("verified") as? Boolean ?: false
            val indicator = data?.get("indicator") as? String ?: "none"
            val pageTitle = data?.get("pageTitle") as? String ?: ""
            val snippet = data?.get("bodySnippet") as? String ?: ""
            log("Verify submission: verified=$verified indicator='$indicator' title='$pageTitle'")
            log("Page snippet: ${snippet.take(150)}")
            activityRepo.log(
                ActivityAction.MODAL_CHECK,
                "Submit verified=$verified\nIndicator: $indicator\nPage: $pageTitle\nSnippet: ${snippet.take(200)}"
            )
            verified
        }.getOrElse { false }
    }

    private suspend fun recordApplied(job: ScrapedJob, type: ApplicationType, verified: Boolean = true) {
        jobRepo.save(
            JobApplication(
                jobId = job.id,
                title = job.title,
                company = job.company,
                jobUrl = job.url,
                applicationType = type,
                source = job.source,
                status = if (verified) ApplicationStatus.APPLIED else ApplicationStatus.SUBMITTED_UNVERIFIED,
                appliedAt = System.currentTimeMillis(),
                errorMessage = if (!verified) "Submitted — success page not detected. Tap 'Open Apply Site' to verify manually." else null
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
                com.linkedinautomation.domain.model.JobBoardSource.SIMPLYHIRED -> ZipRecruiterSource()
                com.linkedinautomation.domain.model.JobBoardSource.DICE -> DiceSource()
                com.linkedinautomation.domain.model.JobBoardSource.REMOTEOK -> RemoteOKSource()
                com.linkedinautomation.domain.model.JobBoardSource.WEWORKREMOTELY -> WeWorkRemotelySource()
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
                    source = source,
                    location = m["location"] as? String ?: ""
                )
            }?.filter { it.id.isNotBlank() && it.url.isNotBlank() } ?: emptyList()
        }.getOrElse { emptyList() }
    }

    /**
     * Returns true if the job's listed location is compatible with the user's preferred location.
     *
     * Strategy:
     * - Blank job location + LinkedIn → ALLOW: LinkedIn already filtered by location in URL;
     *   if the card didn't render a location element that's a scraping gap, not a country mismatch.
     * - Blank job location + other boards → REJECT unless it's a remote-only board.
     * - Remote/worldwide keywords → only accept if user has remoteOnly or hybridOk.
     * - Multi-component "City, Country" format → match ONLY the LAST component so that
     *   "Greece, NY" does NOT match a user searching for "Greece" (the country).
     * - Single component "Greece" → full-string match.
     */
    private fun locationMatches(job: ScrapedJob, prefs: UserPreferences): Boolean {
        if (prefs.location.isBlank()) return true  // No preference → all match

        val jobLoc = job.location.lowercase().trim()

        if (jobLoc.isBlank()) {
            // LinkedIn does server-side location filtering via the `location=` URL parameter —
            // if a card came back from that search but the JS couldn't extract a location element,
            // trust LinkedIn's filter rather than rejecting the job.
            if (job.source == "LinkedIn") return true
            // Remote-only boards are inherently location-agnostic
            val isRemoteBoard = job.source == "RemoteOK" || job.source == "WeWorkRemotely"
            return isRemoteBoard && (prefs.remoteOnly || prefs.hybridOk)
            // For all other direct-scrape boards, unknown location = unknown country → reject
        }

        // Remote/anywhere keywords
        if (jobLoc.contains("remote") || jobLoc.contains("anywhere") ||
            jobLoc.contains("worldwide") || jobLoc.contains("global")) {
            return prefs.remoteOnly || prefs.hybridOk
        }

        // Build user location word set (3+ chars, e.g. "Greece" → ["greece"])
        val userWords = prefs.location.lowercase()
            .split(",", " ", "-")
            .map { it.trim() }
            .filter { it.length >= 3 }

        // Split job location by comma: "Athens, Greece" → ["athens", "greece"]
        //                               "Greece, NY"    → ["greece", "ny"]
        //                               "Attica, Athens, Greece" → ["attica", "athens", "greece"]
        val jobParts = jobLoc.split(",").map { it.trim() }

        // PRIMARY: match the LAST component (country/state).
        // "Athens, Greece"  → last="greece"  → matches user "Greece" ✓
        // "Greece, NY"      → last="ny"       → NO match for user "Greece" ✓
        // "Athens, Attica, Greece" → last="greece" ✓
        val lastPart = jobParts.last()
        if (userWords.any { word -> lastPart.contains(word) }) return true

        // SECONDARY: single-component location (just "Greece" or just "Athens")
        if (jobParts.size == 1 && userWords.any { word -> jobLoc.contains(word) }) return true

        return false
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
