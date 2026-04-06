package com.linkedinautomation.data.local.db.dao

import androidx.room.*
import com.linkedinautomation.data.local.db.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 500")
    fun observeAll(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActivityLogEntity?

    @Insert
    suspend fun insert(entity: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
