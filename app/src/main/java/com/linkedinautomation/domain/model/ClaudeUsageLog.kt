package com.linkedinautomation.domain.model

data class ClaudeUsageLog(
    val id: Long = 0,
    val question: String,
    val answer: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val jobTitle: String,
    val company: String,
    val timestamp: Long
) {
    val totalTokens: Int get() = inputTokens + outputTokens
}
