package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.ActivityLog
import com.linkedinautomation.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActivityLogUseCase @Inject constructor(
    private val repo: ActivityLogRepository
) {
    operator fun invoke(): Flow<List<ActivityLog>> = repo.observeAll()
}
