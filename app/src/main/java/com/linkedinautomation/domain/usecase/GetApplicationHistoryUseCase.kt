package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.repository.JobApplicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetApplicationHistoryUseCase @Inject constructor(
    private val repo: JobApplicationRepository
) {
    operator fun invoke(): Flow<List<JobApplication>> = repo.observeAll()
    fun pending(): Flow<List<JobApplication>> = repo.observePending()
}
