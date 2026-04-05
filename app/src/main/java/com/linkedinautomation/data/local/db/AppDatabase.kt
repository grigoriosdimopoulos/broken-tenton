package com.linkedinautomation.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.linkedinautomation.data.local.db.dao.ActivityLogDao
import com.linkedinautomation.data.local.db.dao.ClaudeUsageLogDao
import com.linkedinautomation.data.local.db.dao.JobApplicationDao
import com.linkedinautomation.data.local.db.entity.ActivityLogEntity
import com.linkedinautomation.data.local.db.entity.ClaudeUsageLogEntity
import com.linkedinautomation.data.local.db.entity.JobApplicationEntity

@Database(
    entities = [
        JobApplicationEntity::class,
        ActivityLogEntity::class,
        ClaudeUsageLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun claudeUsageLogDao(): ClaudeUsageLogDao
}
