package com.linkedinautomation.automation.ai

import com.linkedinautomation.domain.model.ClaudeUsageLog
import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import com.squareup.moshi.Moshi
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
 * Instead of fragile hardcoded DOM selectors, this class sends the page structure
 * (buttons, inputs, visible text) to Claude and asks it what JavaScript to execute
 * to advance the job application. Claude returns either:
 *   - JavaScript to run (clicks a button, fills an input, etc.)
 *   - "DONE:APPLIED"   — application confirmed submitted
 *   - "DONE:FAILED:reason" — cannot proceed
 */
@Singleton
class ClaudePageNavigator @Inject constructor(
    private val usageLogRepo: ClaudeUsageLogRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    /**
     * Given a JSON page context snapshot, return the next JavaScript action.
     *
     * @param pageContextJson  Output of extract_page_context.js
     * @param jobTitle         Job being applied for
     * @param company          Company name
     * @param step             Current step number (for logging)
     * @param prefs            User preferences (for personal info)
     * @param apiKey           Claude API key
     * @return JavaScript string to execute, or "DONE:APPLIED" / "DONE:FAILED:reason"
     */
    suspend fun getNextAction(
        pageContextJson: String,
        jobTitle: String,
        company: String,
        step: Int,
        prefs: UserPreferences,
        apiKey: String
    ): String {
        val systemPrompt = buildSystemPrompt(prefs, jobTitle, company)
        val userMessage = "Step $step.\nPage context:\n$pageContextJson"

        val requestJson = buildRequest(systemPrompt, userMessage)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = runCatching { client.newCall(request).execute() }.getOrElse { e ->
            return "DONE:FAILED:Claude API unreachable: ${e.message}"
        }
        val bodyStr = response.body?.string() ?: return "DONE:FAILED:empty Claude response"
        if (!response.isSuccessful) return "DONE:FAILED:Claude API error ${response.code}"

        val parsed = runCatching {
            moshi.adapter(ClaudeResponse::class.java).fromJson(bodyStr)
        }.getOrNull() ?: return "DONE:FAILED:could not parse Claude response"

        val rawAction = parsed.content.firstOrNull()?.text?.trim() ?: ""
        val action = cleanAction(rawAction)

        // Log usage
        usageLogRepo.save(ClaudeUsageLog(
            question = "SmartApply step $step for $jobTitle @ $company",
            answer = action.take(200),
            inputTokens = parsed.usage?.inputTokens ?: 0,
            outputTokens = parsed.usage?.outputTokens ?: 0,
            jobTitle = jobTitle,
            company = company,
            timestamp = System.currentTimeMillis()
        ))

        return action
    }

    /** Strip markdown code fences if Claude wrapped the JS in them */
    private fun cleanAction(raw: String): String {
        if (raw.startsWith("DONE:")) return raw
        return raw
            .removePrefix("```javascript").removePrefix("```js").removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun buildSystemPrompt(prefs: UserPreferences, jobTitle: String, company: String): String = """
You are an expert web automation agent. Your task: apply for a job on behalf of the user by controlling a WebView browser.

JOB TARGET: "$jobTitle" at "$company"

USER DETAILS (use these to fill forms):
- Full name: ${prefs.firstName} ${prefs.lastName}
- Email: ${prefs.email.ifBlank { prefs.linkedInEmail }}
- Phone: ${prefs.phone}
- City: ${prefs.city}
- Country: ${prefs.country}
- LinkedIn URL: ${prefs.linkedInUrl}
- Current title: ${prefs.currentJobTitle}
- Years of experience: ${prefs.yearsOfExperience}
- Bio summary: ${prefs.experienceBio.take(300)}

Each step you receive a JSON snapshot of the current page (buttons, inputs, headings, alerts, URL).
Respond with ONE of:
1. Plain JavaScript (no markdown fences) to execute in the page — must advance the application
2. Exactly "DONE:APPLIED" when you detect a success/confirmation message
3. Exactly "DONE:FAILED:reason" if stuck (CAPTCHA, already applied, no apply button, fatal error)

JAVASCRIPT RULES:
- Use the `sel` selectors from the JSON — they are already computed for each element
- To click a button: `document.querySelector('<sel>').click()`
- To fill a text input: var el=document.querySelector('<sel>'); el.value='<val>'; el.dispatchEvent(new Event('input',{bubbles:true})); el.dispatchEvent(new Event('change',{bubbles:true}));
- To select a dropdown option: var s=document.querySelector('<sel>'); s.value='<val>'; s.dispatchEvent(new Event('change',{bubbles:true}));
- For checkboxes/radio buttons: use .click() if not already checked
- For experience years questions: use ${prefs.yearsOfExperience} unless asked for a specific skill
- Answer "yes" to "Are you authorized to work?" type questions; answer honestly for salary (${prefs.minSalary}k+)
- If a form page has BOTH unfilled required inputs AND a Next/Submit button: fill all inputs FIRST, then click Next
- If all required inputs are filled and there's a Submit button: click Submit
- If there's an Easy Apply button and no modal: click it to open the modal
- Never click Dismiss, Cancel, or Close unless truly stuck

SUCCESS DETECTION (return DONE:APPLIED if you see any of these):
- "Application submitted", "Applied", "Your application was sent", "Done!", "You've applied"
- URL contains "/apply/success" or similar
- A green checkmark confirmation screen

FAILURE DETECTION (return DONE:FAILED:reason if you see):
- "Already applied", "You've already applied" → DONE:FAILED:already applied
- CAPTCHA or "verify you're human" → DONE:FAILED:captcha required
- "This job is no longer accepting applications" → DONE:FAILED:job closed
- After 3+ steps on same page with same buttons and no progress → DONE:FAILED:stuck on page

Return ONLY the JavaScript or DONE: string. No explanation, no markdown.
    """.trimIndent()

    private fun buildRequest(system: String, userMsg: String): String {
        val adapter = moshi.adapter(String::class.java)
        return """
{
  "model": "claude-sonnet-4-6",
  "max_tokens": 1024,
  "system": ${adapter.toJson(system)},
  "messages": [
    {"role": "user", "content": ${adapter.toJson(userMsg)}}
  ]
}
        """.trimIndent()
    }
}
