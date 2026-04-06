package com.linkedinautomation.data.local.db.dao

import androidx.room.*
import com.linkedinautomation.data.local.db.entity.JobApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobApplicationDao {

    @Query("SELECT * FROM job_applications ORDER BY appliedAt DESC")
    fun observeAll(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE status = 'PENDING_APPROVAL' ORDER BY appliedAt DESC")
    fun observePending(): Flow<List<JobApplicationEntity>>

    @Query("SELECT * FROM job_applications WHERE id = :id")
    suspend fun getById(id: Long): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId LIMIT 1")
    suspend fun getByJobId(jobId: String): JobApplicationEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM job_applications WHERE jobId = :jobId)")
    suspend fun existsByJobId(jobId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: JobApplicationEntity): Long

    @Update
    suspend fun update(entity: JobApplicationEntity)

    @Query("UPDATE job_applications SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM job_applications WHERE status = 'APPLIED'")
    fun observeAppliedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM job_applications WHERE status = 'PENDING_APPROVAL'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM job_applications WHERE status = 'APPLIED' AND appliedAt >= :since")
    fun observeAppliedSince(since: Long): Flow<Int>
}
