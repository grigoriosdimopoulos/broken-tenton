package com.linkedinautomation.presentation.visibleapply

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.automation.ai.ClaudePageNavigator
import com.linkedinautomation.automation.engine.AutomationWebEngine
import com.linkedinautomation.automation.scripts.JsScriptLoader
import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            // WebView settings must be set on the Main thread — prefsRepo.get() may resume
            // on a DataStore/IO thread, so we switch back to Main explicitly.
            withContext(Dispatchers.Main) {
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
    }

    /** Called from screen's LaunchedEffect(engineReady) once the engine is ready. */
    fun startApply() {
        if (applyJob?.isActive == true) return
        val eng = engine ?: return
        applyJob = viewModelScope.launch {
            // Nothing inside the loop may crash the app — surface errors in the status bar
            try {
                runApplyLoop(eng)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                addLog("Error: ${e.javaClass.simpleName}: ${e.message?.take(120)}")
                _statusText.value = "Error: ${e.message?.take(80) ?: e.javaClass.simpleName}"
                runCatching { jobRepo.updateStatus(jobId, ApplicationStatus.FAILED) }
                _isDone.value = true
            }
        }
    }

    fun cancel() {
        applyJob?.cancel()
        // Do NOT destroy the WebView here — it is still attached to the composition;
        // destroying an attached WebView crashes. The engine only clears its callbacks.
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

        // Restore the stored LinkedIn session before loading any linkedin.com page —
        // same injection the background scan flow uses.
        if (prefs.linkedInCookies.isNotBlank()) {
            withContext(Dispatchers.Main) {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                prefs.linkedInCookies.split(";").map { it.trim() }.filter { it.isNotBlank() }
                    .forEach { cookie ->
                        cm.setCookie(".linkedin.com", cookie)
                        cm.setCookie("https://www.linkedin.com", cookie)
                    }
                cm.flush()
            }
            addLog("LinkedIn session cookies restored")
        }

        eng.enableApplyMode()
        _statusText.value = "Loading job page…"
        runCatching { eng.navigateTo(jobUrl, timeoutMs = 20_000) }
        delay(1500)

        val contextScript = jsLoader.load(ScriptRegistry.EXTRACT_PAGE_CONTEXT)
        var lastFingerprint = ""
        var sameCount = 0

        for (step in 1..15) {
            _statusText.value = "Step $step — reading page…"
            addLog("── Step $step ──")

            // Tag MUST match AndroidBridge.onResult('extract_ctx', ...) in the JS
            val pageCtx = runCatching {
                eng.runJs("extract_ctx", contextScript, 10_000)
            }.getOrElse { e ->
                addLog("Page context error: ${e.message?.take(80)}")
                "{}"
            }

            _statusText.value = "Step $step — asking Claude…"
            val action = runCatching {
                claudeNavigator.getNextAction(
                    pageContextJson = pageCtx,
                    jobTitle = jobTitle,
                    company = company,
                    step = step,
                    prefs = prefs,
                    apiKey = apiKey
                )
            }.getOrElse { e -> "DONE:FAILED:Claude API error — ${e.message?.take(80)}" }
            addLog(action.take(120))

            // Stuck detection: same action AND same page fingerprint = nothing is changing
            if (!action.startsWith("DONE:") && action.isNotBlank()) {
                val fingerprint = action.take(60) + pageCtx.length + pageCtx.take(120)
                if (fingerprint == lastFingerprint) {
                    sameCount++
                    if (sameCount >= 3) {
                        _statusText.value = "Stuck — page not changing, stopped"
                        addLog("Stuck — same action repeated $sameCount times")
                        jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
                        _isDone.value = true
                        return
                    }
                } else {
                    sameCount = 0
                    lastFingerprint = fingerprint
                }
            }

            when {
                action.startsWith("DONE:APPLIED") -> {
                    _statusText.value = "Applied successfully!"
                    addLog("Done — application submitted")
                    jobRepo.updateStatus(jobId, ApplicationStatus.APPLIED)
                    _isDone.value = true
                    return
                }
                action.startsWith("DONE:FAILED") -> {
                    val reason = action.removePrefix("DONE:FAILED:").trim()
                    _statusText.value = "Failed: $reason"
                    addLog("Done — failed: $reason")
                    jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
                    _isDone.value = true
                    return
                }
                action.startsWith("NAVIGATE:") -> {
                    val url = action.removePrefix("NAVIGATE:").trim()
                    _statusText.value = "Step $step — navigating…"
                    runCatching { eng.navigateTo(url, timeoutMs = 20_000) }
                    delay(1500)
                }
                action.isBlank() -> {
                    addLog("Empty action from Claude — skipping step")
                }
                looksLikeExplanation(action) -> {
                    addLog("Claude returned text instead of JS — skipping")
                }
                else -> {
                    _statusText.value = "Step $step — executing…"
                    // Encode as a JSON string so new Function() turns syntax errors
                    // into catchable runtime errors instead of killing the flow
                    val encoded = action
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                    val safeJs = """(function(){try{(new Function("$encoded"))();}catch(e){}})();"""
                    val result = eng.executeJsAndWaitForNavigation(safeJs, navWaitMs = 4_000)
                    delay(if (result.startsWith("navigated:")) 1500L else 800L)
                }
            }
        }

        // Ran out of steps
        _statusText.value = "Reached step limit without completing"
        jobRepo.updateStatus(jobId, ApplicationStatus.FAILED)
        _isDone.value = true
    }

    /** True if Claude returned narrative text instead of JavaScript (mirrors orchestrator). */
    private fun looksLikeExplanation(action: String): Boolean {
        val jsStarters = listOf(
            "document.", "(function", "function ", "var ", "let ", "const ",
            "window.", "location.", "history.", "navigator.",
            "DONE:", "NAVIGATE:", "(",
            "document[", "arguments", "return ", "if (", "if(", "try {"
        )
        val trimmed = action.trimStart()
        if (jsStarters.any { trimmed.startsWith(it) }) return false
        val explanationWords = listOf(
            "I ", "I'm ", "The ", "This ", "There ", "Click ", "Fill ",
            "Looking ", "Based ", "Since ", "It ", "We ", "You ",
            "Step ", "Now ", "First ", "Next ", "Let ", "Please "
        )
        return explanationWords.any { trimmed.startsWith(it) }
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
