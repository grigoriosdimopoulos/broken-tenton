package com.linkedinautomation.data.repository

import com.linkedinautomation.data.local.db.dao.JobApplicationDao
import com.linkedinautomation.data.local.db.entity.JobApplicationEntity
import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.repository.JobApplicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobApplicationRepositoryImpl @Inject constructor(
    private val dao: JobApplicationDao
) : JobApplicationRepository {

    override fun observeAll(): Flow<List<JobApplication>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePending(): Flow<List<JobApplication>> =
        dao.observePending().map { list -> list.map { it.toDomain() } }

    override fun observeAppliedCount(): Flow<Int> = dao.observeAppliedCount()

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override fun observeAppliedSince(since: Long): Flow<Int> = dao.observeAppliedSince(since)

    override suspend fun getById(id: Long): JobApplication? =
        dao.getById(id)?.toDomain()

    override suspend fun getByJobId(jobId: String): JobApplication? =
        dao.getByJobId(jobId)?.toDomain()

    override suspend fun existsByJobId(jobId: String): Boolean =
        dao.existsByJobId(jobId)

    override suspend fun save(job: JobApplication): Long =
        dao.insert(JobApplicationEntity.from(job))

    override suspend fun updateStatus(id: Long, status: ApplicationStatus) =
        dao.updateStatus(id, status.name)
}
