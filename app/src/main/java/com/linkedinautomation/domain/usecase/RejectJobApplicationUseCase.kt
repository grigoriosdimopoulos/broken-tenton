package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.repository.JobApplicationRepository
import javax.inject.Inject

class RejectJobApplicationUseCase @Inject constructor(
    private val repo: JobApplicationRepository
) {
    suspend operator fun invoke(id: Long) = repo.updateStatus(id, ApplicationStatus.REJECTED)
}
