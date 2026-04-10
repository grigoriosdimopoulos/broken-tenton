package com.linkedinautomation.presentation.visibleapply

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.automation.ai.ClaudePageNavigator
import com.linkedinautomation.automation.engine.AutomationWebEngine
import com.linkedinautomation.automation.scripts.JsScriptLoader
import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VisibleApplyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val claudeNavigator: ClaudePageNavigator,
    private val jsLoader: JsScriptLoader,
    private val jobRepo: JobApplicationRepository,
    private val prefsRepo: UserPreferencesRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val jobId: Long = savedStateHandle["jobId"] ?: 0L
    val jobTitle: String = savedStateHandle["jobTitle"] ?: ""
    val company: String = savedStateHandle["company"] ?: ""
    private val jobUrl: String = savedStateHandle["jobUrl"] ?: ""

    private val _statusText = MutableStateFlow("Opening job page…")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    // Set to true once engine is configured on the existing WebView
    private val _engineReady = MutableStateFlow(false)
    val engineReady: StateFlow<Boolean> = _engineReady.asStateFlow()

    private var engine: AutomationWebEngine? = null
    private var applyJob: Job? = null

    /**
     * Called from the Compose AndroidView factory — hands us the Activity-context WebView
     * so it renders properly (hardware-accelerated, properly attached to a window).
     * Engine creation is async because we need to fetch prefs for sourceMode.
     */
    fun attachWebView(wv: WebView, activityCtx: Context) {
        viewModelScope.launch {
            val prefs = prefsRepo.get()
            engine = AutomationWebEngine(
                context = activityCtx,
                sourceMode = prefs.sourceMode,
                onLog = { msg -> addLog(msg) },
                onBlockedNavigation = { /* apply mode bypasses URL allowlist */ },
                onFileChooserRequested = { callback -> callback(resumePath()) }
            ).also { it.createWithExistingWebView(wv) }
            _engineReady.value = true
        }
    }

    /** Called from screen's LaunchedEffect(engineReady) once the engine is ready. */
    fun startApply() {
        val eng = engine ?: return
        applyJob = viewModelScope.launch {
            runApplyLoop(eng)
        }
    }

    fun cancel() {
        applyJob?.cancel()
        engine?.destroy()
        engine = null
    }

    private suspend fun runApplyLoop(eng: AutomationWebEngine) {
        val prefs = prefsRepo.get()
        val apiKey = prefs.claudeApiKey

        if (apiKey.isBlank()) {
            _statusText.value = "Claude API key not set — add it in Settings"
            _isDone.value = true
            return
        }
        if (jobUrl.isBlank()) {
            _statusText.value = "No job URL — cannot open page"
            _isDone.value = true
            return
        }

        eng.enableApplyMode()
        _statusText.value = "Loading job page…"
        runCatching { eng.navigateTo(jobUrl, timeoutMs = 20_000) }
        delay(1500)

        val contextScript = jsLoader.load("extract_page_context.js")
        var lastFingerprint = ""
        var sameCount = 0

        for (step in 1..15) {
            _statusText.value = "Step $step — reading page…"
            addLog("── Step $step ──")

            val pageCtxResult = runCatching { eng.runJs("ctx_$step", contextScript, 8_000) }
            if (pageCtxResult.isFailure) {
                addLog("Page context error: ${pageCtxResult.exceptionOrNull()?.message?.take(80)}")
                break
            }
            val pageCtx = pageCtxResult.getOrThrow()

            _statusText.value = "Step $step — asking Claude…"
            val action = claudeNavigator.getNextAction(
                pageContextJson = pageCtx,
                jobTitle = jobTitle,
                company = company,
                step = step,
                prefs = prefs,
                apiKey = apiKey
            )
            addLog(action.take(120))

            when {
                action.startsWith("DONE:APPLIED") -> {
                    _statusText.value = "Applied successfully!"
                    addLog("Done — application submitted")
                    jobRepo.updateStatus(jobId, ApplicationStatus.APPLIED)
                    _isDone.value = true
                    return
                }
                action.startsWith("DONE:FAILED:") -> {
                    val reason = action.removePrefix("DONE:FAILED:")
                    _statusText.value = "Failed: $reason"
                    addLog("Done — failed: $reason")
                    jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
                    _isDone.value = true
                    return
                }
                action.startsWith("NAVIGATE:") -> {
                    val url = action.removePrefix("NAVIGATE:")
                    _statusText.value = "Step $step — navigating…"
                    runCatching { eng.navigateTo(url, timeoutMs = 20_000) }
                    delay(1500)
                }
                else -> {
                    // Stuck detection: same action + same page context fingerprint
                    val fingerprint = action.take(60) + pageCtx.take(60)
                    if (fingerprint == lastFingerprint) {
                        sameCount++
                    } else {
                        sameCount = 0
                        lastFingerprint = fingerprint
                    }
                    if (sameCount >= 3) {
                        _statusText.value = "Stuck — stopped after $step steps"
                        addLog("Stuck — same action repeated $sameCount times")
                        jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
                        _isDone.value = true
                        return
                    }

                    _statusText.value = "Step $step — executing…"
                    val result = eng.executeJsAndWaitForNavigation(action)
                    delay(if (result.startsWith("navigated:")) 1500L else 800L)
                }
            }
        }

        // Ran out of steps
        _statusText.value = "Reached step limit without completing"
        jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
        _isDone.value = true
    }

    private fun addLog(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _logs.value = (_logs.value + "$ts  $msg").takeLast(200)
    }

    private fun resumePath(): String? {
        val f = File(appContext.filesDir, "resume.pdf")
        return if (f.exists()) f.absolutePath else null
    }

    override fun onCleared() {
        super.onCleared()
        engine?.destroy()
    }
}
