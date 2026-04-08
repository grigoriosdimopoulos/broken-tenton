package com.linkedinautomation.automation.ai

import com.linkedinautomation.domain.model.ClaudeUsageLog
import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses Claude to analyze the current page state and return a JavaScript action.
 *
 * Instead of fragile hardcoded DOM selectors, each step sends the live page
 * structure (buttons, inputs, visible text) to Claude Sonnet. Claude returns
 * plain JavaScript to execute — no hardcoded selectors that break on DOM changes.
 *
 * Returns one of:
 *  - JavaScript string to execute
 *  - "DONE:APPLIED"     — application confirmed submitted
 *  - "DONE:FAILED:why"  — cannot proceed
 */
@Singleton
class ClaudePageNavigator @Inject constructor(
    private val usageLogRepo: ClaudeUsageLogRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    suspend fun getNextAction(
        pageContextJson: String,
        jobTitle: String,
        company: String,
        step: Int,
        prefs: UserPreferences,
        apiKey: String
    ): String {
        if (apiKey.isBlank()) {
            return "DONE:FAILED:Claude API key not set — add it in Settings → Claude AI"
        }

        val model = prefs.smartApplyModel.ifBlank { "claude-haiku-4-5-20251001" }
        val system = buildSystemPrompt(prefs, jobTitle, company)
        val userMsg = "Step $step. Page context:\n$pageContextJson"
        val bodyJson = buildRequestBody(system, userMsg, model)

        // Retry once on transient network failure
        var lastError = "unknown"
        for (attempt in 1..2) {
            if (attempt > 1) delay(3000L)

            val httpResult: Result<okhttp3.Response> = withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder()
                        .url("https://api.anthropic.com/v1/messages")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .header("content-type", "application/json")
                        .post(bodyJson.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(req).execute()
                }
            }

            val ex = httpResult.exceptionOrNull()
            if (ex != null) {
                lastError = "${ex.javaClass.simpleName}: ${ex.message ?: "(no message)"}"
                continue
            }

            val response = httpResult.getOrNull() ?: continue
            val rawBody = withContext(Dispatchers.IO) {
                runCatching { response.body?.string() }.getOrNull()
            }
            if (rawBody.isNullOrBlank()) {
                lastError = "HTTP ${response.code}: empty body"
                continue
            }
            if (!response.isSuccessful) {
                lastError = "HTTP ${response.code}: ${rawBody.take(150)}"
                continue
            }

            val parsed = runCatching {
                moshi.adapter(ClaudeResponse::class.java).fromJson(rawBody)
            }.getOrElse { e ->
                lastError = "JSON parse error: ${e.message}"
                null
            } ?: continue

            val raw = parsed.content.firstOrNull()?.text?.trim() ?: ""
            val action = stripFences(raw)

            // Log usage asynchronously (don't fail the apply if this fails)
            runCatching {
                usageLogRepo.save(ClaudeUsageLog(
                    question = "SmartApply step $step — $jobTitle @ $company",
                    answer = action.take(200),
                    inputTokens = parsed.usage?.inputTokens ?: 0,
                    outputTokens = parsed.usage?.outputTokens ?: 0,
                    jobTitle = jobTitle,
                    company = company,
                    timestamp = System.currentTimeMillis()
                ))
            }

            return action
        }

        return "DONE:FAILED:Claude API error after ${ if (lastError.length > 100) lastError.take(100) + "…" else lastError }"
    }

    /** Remove markdown code fences Claude sometimes wraps JS in */
    private fun stripFences(raw: String): String {
        if (raw.startsWith("DONE:")) return raw
        return raw
            .trimStart()
            .removePrefix("```javascript")
            .removePrefix("```js")
            .removePrefix("```")
            .trimEnd()
            .removeSuffix("```")
            .trim()
    }

    private fun buildSystemPrompt(prefs: UserPreferences, jobTitle: String, company: String): String {
        val email = prefs.email.ifBlank { prefs.linkedInEmail }
        return """
You are a browser automation agent applying for a job on behalf of a user.

TARGET JOB: "$jobTitle" at "$company"

USER DETAILS — use to fill forms:
- Name: ${prefs.firstName} ${prefs.lastName}
- Email: $email
- Phone: ${prefs.phone}
- City/Country: ${prefs.city}, ${prefs.country}
- LinkedIn: ${prefs.linkedInUrl}
- Job title: ${prefs.currentJobTitle}
- Years experience: ${prefs.yearsOfExperience}
- Bio: ${prefs.experienceBio.take(250)}
- Min salary: ${if (prefs.minSalary > 0) "${prefs.minSalary}k+" else "flexible"}

You receive a JSON snapshot of the live page each step (buttons with CSS selectors, inputs with labels/types, page text, URL). Return EXACTLY one of:
A) Plain JavaScript to execute (no markdown, no explanation)
B) The string: DONE:APPLIED
C) The string: DONE:FAILED:reason

LINKEDIN APPLY FLOW — CRITICAL:
- If you see an "Easy Apply" button: click it. It opens a MODAL DIALOG — the URL NEVER changes while you fill the form. A stable URL is completely normal.
- If you see an "Apply" button (NOT "Easy Apply") that has an href: DO NOT click it (it would open a new tab). Instead return NAVIGATE:href to go directly to the external application page.
- If you see an "Apply" button with no href: click it, then check if a new URL loaded.
- Steps after clicking Easy Apply: you are inside the multi-step modal. Each page has inputs to fill and a Next/Submit button.
- Fill ALL visible inputs on each modal page BEFORE clicking Next/Submit. Do it all in ONE JS block.
- The Next/Submit button in the modal often keeps the same ember ID (e.g. #ember56) across form pages — that is fine, reuse it.
- Typical Easy Apply pages: contact info, resume, screening questions (yes/no, experience), work authorization, salary, review/submit.
- On the final REVIEW page, click "Submit application" to confirm. After that, return DONE:APPLIED.
- On external ATS pages (Greenhouse, Lever, Workday etc.): fill all visible form fields and click Next/Submit to progress.

JAVASCRIPT RULES:
- Buttons: document.querySelector('<sel>').click()
- Fill text input: (function(){var e=document.querySelector('<sel>');e.value='<v>';e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));})()
- Select dropdown: (function(){var s=document.querySelector('<sel>');s.value='<v>';s.dispatchEvent(new Event('change',{bubbles:true}));})()
- Checkbox/radio: .click() to toggle
- Combine fill+click into ONE self-invoking function block
- Use ${prefs.yearsOfExperience} for any years-of-experience numeric inputs
- Work authorization: yes. Sponsorship required: no.
- Salary expectation: ${if (prefs.minSalary > 0) prefs.minSalary * 1000 else "negotiable"}
- NEVER click Cancel, Dismiss, or Close
- If a file-upload input for resume is present and no file is selected, skip it (LinkedIn uses profile resume)

RETURN DONE:APPLIED if body text contains "application was sent", "you've applied", "application submitted", or "done!" on a confirmation, or URL contains "/apply/success"
RETURN DONE:FAILED:already applied — body says "you've already applied"
RETURN DONE:FAILED:job closed — "no longer accepting applications"
RETURN DONE:FAILED:captcha — CAPTCHA visible
RETURN DONE:FAILED:no apply button — step 1, no Easy Apply or Apply button found

OUTPUT FORMAT — MANDATORY:
Your entire response must be ONE of the following. Nothing else. No preamble. No explanation. No markdown.

CORRECT examples:
document.querySelector('#ember56').click()
(function(){var e=document.querySelector('#phoneNumber');e.value='${prefs.phone}';e.dispatchEvent(new Event('input',{bubbles:true}));document.querySelector('#ember22').click();})()
DONE:APPLIED
DONE:FAILED:no apply button
NAVIGATE:https://jobs.greenhouse.io/example/123

WRONG — never do this:
"I can see this is a job posting..."
"The page shows an Apply button..."
"```javascript..."
        """.trimIndent()
    }

    private fun buildRequestBody(system: String, userMsg: String, model: String): String {
        val adapter = moshi.adapter(String::class.java)
        return """{"model":${adapter.toJson(model)},"max_tokens":600,"system":${adapter.toJson(system)},"messages":[{"role":"user","content":${adapter.toJson(userMsg)}}]}"""
    }
}
