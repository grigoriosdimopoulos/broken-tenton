package com.linkedinautomation.data.local.db.dao

import androidx.room.*
import com.linkedinautomation.data.local.db.entity.ClaudeUsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaudeUsageLogDao {

    @Query("SELECT * FROM claude_usage_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ClaudeUsageLogEntity>>

    @Query("SELECT SUM(inputTokens + outputTokens) FROM claude_usage_logs")
    fun observeTotalTokens(): Flow<Int?>

    @Insert
    suspend fun insert(entity: ClaudeUsageLogEntity): Long
}
