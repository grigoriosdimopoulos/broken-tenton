package com.linkedinautomation.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.linkedinautomation.domain.model.ActivityAction
import com.linkedinautomation.domain.model.ActivityLog

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val details: String,
    val url: String? = null,
    val timestamp: Long
) {
    fun toDomain() = ActivityLog(
        id = id,
        action = ActivityAction.valueOf(action),
        details = details,
        url = url,
        timestamp = timestamp
    )

    companion object {
        fun from(log: ActivityLog) = ActivityLogEntity(
            id = log.id,
            action = log.action.name,
            details = log.details,
            url = log.url,
            timestamp = log.timestamp
        )
    }
}
