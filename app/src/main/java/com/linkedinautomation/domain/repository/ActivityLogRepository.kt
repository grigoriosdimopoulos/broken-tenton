package com.linkedinautomation.domain.repository

import com.linkedinautomation.domain.model.ActivityAction
import com.linkedinautomation.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityLogRepository {
    fun observeAll(): Flow<List<ActivityLog>>
    suspend fun getById(id: Long): ActivityLog?
    suspend fun log(action: ActivityAction, details: String, url: String? = null, screenshotPath: String? = null)
    suspend fun pruneOlderThan(timestamp: Long)
}
