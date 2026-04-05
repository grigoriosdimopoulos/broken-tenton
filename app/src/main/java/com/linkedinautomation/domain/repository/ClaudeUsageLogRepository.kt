package com.linkedinautomation.domain.repository

import com.linkedinautomation.domain.model.ClaudeUsageLog
import kotlinx.coroutines.flow.Flow

interface ClaudeUsageLogRepository {
    fun observeAll(): Flow<List<ClaudeUsageLog>>
    fun observeTotalTokens(): Flow<Int>
    suspend fun save(log: ClaudeUsageLog)
}
