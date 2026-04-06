package com.linkedinautomation.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.linkedinautomation.domain.model.ApplicationStatus
import com.linkedinautomation.domain.model.ApplicationType
import com.linkedinautomation.domain.model.JobApplication

@Entity(tableName = "job_applications", indices = [Index(value = ["jobId"], unique = true)])
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: String,
    val title: String,
    val company: String,
    val jobUrl: String,
    val applicationTypeString: String,
    val source: String,
    val status: String,
    val appliedAt: Long,
    val errorMessage: String? = null,
    val screenshotPath: String? = null,
    val location: String? = null
) {
    fun toDomain() = JobApplication(
        id = id,
        jobId = jobId,
        title = title,
        company = company,
        jobUrl = jobUrl,
        applicationType = ApplicationType.fromTypeString(applicationTypeString),
        source = source,
        status = ApplicationStatus.valueOf(status),
        appliedAt = appliedAt,
        errorMessage = errorMessage,
        screenshotPath = screenshotPath,
        location = location
    )

    companion object {
        fun from(job: JobApplication) = JobApplicationEntity(
            id = job.id,
            jobId = job.jobId,
            title = job.title,
            company = job.company,
            jobUrl = job.jobUrl,
            applicationTypeString = job.applicationType.toTypeString(),
            source = job.source,
            status = job.status.name,
            appliedAt = job.appliedAt,
            errorMessage = job.errorMessage,
            screenshotPath = job.screenshotPath,
            location = job.location
        )
    }
}
