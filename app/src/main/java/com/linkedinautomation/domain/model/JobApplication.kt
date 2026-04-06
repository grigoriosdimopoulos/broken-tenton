package com.linkedinautomation.domain.model

data class JobApplication(
    val id: Long = 0,
    val jobId: String,
    val title: String,
    val company: String,
    val jobUrl: String,
    val applicationType: ApplicationType,
    val source: String,
    val status: ApplicationStatus,
    val appliedAt: Long,
    val errorMessage: String? = null,
    val screenshotPath: String? = null,
    val location: String? = null
)
