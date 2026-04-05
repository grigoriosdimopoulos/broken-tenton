package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.repository.JobApplicationRepository
import javax.inject.Inject

class RecordJobApplicationUseCase @Inject constructor(
    private val repo: JobApplicationRepository
) {
    suspend operator fun invoke(job: JobApplication): Long = repo.save(job)
}
