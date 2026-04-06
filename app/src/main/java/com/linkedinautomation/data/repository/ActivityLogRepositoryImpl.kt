package com.linkedinautomation.data.repository

import com.linkedinautomation.data.local.db.dao.ActivityLogDao
import com.linkedinautomation.data.local.db.entity.ActivityLogEntity
import com.linkedinautomation.domain.model.ActivityAction
import com.linkedinautomation.domain.model.ActivityLog
import com.linkedinautomation.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogRepositoryImpl @Inject constructor(
    private val dao: ActivityLogDao
) : ActivityLogRepository {

    override fun observeAll(): Flow<List<ActivityLog>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun log(
        action: ActivityAction,
        details: String,
        url: String?,
        screenshotPath: String?
    ) {
        dao.insert(
            ActivityLogEntity(
                action = action.name,
                details = details,
                url = url,
                timestamp = System.currentTimeMillis(),
                screenshotPath = screenshotPath
            )
        )
    }

    override suspend fun pruneOlderThan(timestamp: Long) =
        dao.deleteOlderThan(timestamp)
}
