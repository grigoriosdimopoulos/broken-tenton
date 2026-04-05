package com.linkedinautomation.automation.ai

import com.linkedinautomation.domain.model.ClaudePersona
import com.linkedinautomation.domain.model.ClaudeUsageLog
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeAnswerGenerator @Inject constructor(
    private val usageLogRepo: ClaudeUsageLogRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()

    suspend fun generateAnswer(
        question: String,
        userBio: String,
        jobTitle: String,
        company: String,
        persona: ClaudePersona,
        apiKey: String
    ): String {
        val personaBlock = buildPersonaBlock(persona)

        val prompt = """
You are filling out a job application on behalf of a real person. Write naturally, like a human — not like AI.

$personaBlock

Applicant background:
$userBio

Applying for: $jobTitle at $company

Answer this question in 2-3 sentences:
"$question"

Return ONLY the answer text. No preamble, no quotation marks, no extra commentary.
        """.trimIndent()

        val requestBody = buildJsonRequest(prompt)
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string() ?: throw RuntimeException("Empty response")

        if (!response.isSuccessful) {
            throw RuntimeException("Claude API error ${response.code}: $bodyStr")
        }

        val parsed = parseResponse(bodyStr)
        val answer = parsed.content.firstOrNull()?.text?.trim() ?: ""
        val inputTokens = parsed.usage?.inputTokens ?: 0
        val outputTokens = parsed.usage?.outputTokens ?: 0

        // Log usage
        usageLogRepo.save(
            ClaudeUsageLog(
                question = question,
                answer = answer,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                jobTitle = jobTitle,
                company = company,
                timestamp = System.currentTimeMillis()
            )
        )

        return answer
    }

    private fun buildPersonaBlock(persona: ClaudePersona): String = buildString {
        append("Writing style: ${persona.tone.displayName} tone.")
        if (persona.styleNotes.isNotBlank()) append(" ${persona.styleNotes}.")
        if (persona.avoidPhrases.isNotEmpty()) {
            append(" Do NOT use these phrases or words: ${persona.avoidPhrases.joinToString(", ")}.")
        }
        if (persona.customInstructions.isNotBlank()) {
            append(" ${persona.customInstructions}")
        }
    }

    private fun buildJsonRequest(prompt: String): String {
        return """
{
  "model": "claude-haiku-4-5-20251001",
  "max_tokens": 300,
  "messages": [
    {
      "role": "user",
      "content": ${moshi.adapter(String::class.java).toJson(prompt)}
    }
  ]
}
        """.trimIndent()
    }

    private fun parseResponse(json: String): ClaudeResponse {
        val adapter = moshi.adapter(ClaudeResponse::class.java)
        return adapter.fromJson(json) ?: throw RuntimeException("Failed to parse response")
    }
}

@JsonClass(generateAdapter = true)
data class ClaudeResponse(
    val content: List<ContentBlock> = emptyList(),
    val usage: UsageInfo? = null
)

@JsonClass(generateAdapter = true)
data class ContentBlock(
    val type: String = "",
    val text: String = ""
)

@JsonClass(generateAdapter = true)
data class UsageInfo(
    @Json(name = "input_tokens") val inputTokens: Int = 0,
    @Json(name = "output_tokens") val outputTokens: Int = 0
)
