package com.linkedinautomation.domain.repository

import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.model.JobApplication
import kotlinx.coroutines.flow.Flow

interface JobApplicationRepository {
    fun observeAll(): Flow<List<JobApplication>>
    fun observePending(): Flow<List<JobApplication>>
    fun observeAppliedCount(): Flow<Int>
    fun observePendingCount(): Flow<Int>
    fun observeAppliedSince(since: Long): Flow<Int>
    suspend fun getById(id: Long): JobApplication?
    suspend fun getByJobId(jobId: String): JobApplication?
    suspend fun existsByJobId(jobId: String): Boolean
    suspend fun save(job: JobApplication): Long
    suspend fun updateStatus(id: Long, status: ApplicationStatus)
}
