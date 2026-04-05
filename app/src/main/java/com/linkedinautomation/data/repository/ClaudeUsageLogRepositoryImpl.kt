package com.linkedinautomation.data.repository

import com.linkedinautomation.data.local.db.dao.ClaudeUsageLogDao
import com.linkedinautomation.data.local.db.entity.ClaudeUsageLogEntity
import com.linkedinautomation.domain.model.ClaudeUsageLog
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClaudeUsageLogRepositoryImpl @Inject constructor(
    private val dao: ClaudeUsageLogDao
) : ClaudeUsageLogRepository {

    override fun observeAll(): Flow<List<ClaudeUsageLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTotalTokens(): Flow<Int> =
        dao.observeTotalTokens().map { it ?: 0 }

    override suspend fun save(log: ClaudeUsageLog) {
        dao.insert(ClaudeUsageLogEntity.from(log))
    }
}
