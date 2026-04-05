package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.ClaudeUsageLog
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClaudeUsageLogUseCase @Inject constructor(
    private val repo: ClaudeUsageLogRepository
) {
    operator fun invoke(): Flow<List<ClaudeUsageLog>> = repo.observeAll()
    fun totalTokens(): Flow<Int> = repo.observeTotalTokens()
}
