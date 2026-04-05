package com.linkedinautomation.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.linkedinautomation.domain.model.ClaudeUsageLog

@Entity(tableName = "claude_usage_logs")
data class ClaudeUsageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val jobTitle: String,
    val company: String,
    val timestamp: Long
) {
    fun toDomain() = ClaudeUsageLog(
        id = id,
        question = question,
        answer = answer,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        jobTitle = jobTitle,
        company = company,
        timestamp = timestamp
    )

    companion object {
        fun from(log: ClaudeUsageLog) = ClaudeUsageLogEntity(
            id = log.id,
            question = log.question,
            answer = log.answer,
            inputTokens = log.inputTokens,
            outputTokens = log.outputTokens,
            jobTitle = log.jobTitle,
            company = log.company,
            timestamp = log.timestamp
        )
    }
}
