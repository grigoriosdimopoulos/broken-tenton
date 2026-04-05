package com.linkedinautomation.domain.model

data class ActivityLog(
    val id: Long = 0,
    val action: ActivityAction,
    val details: String,
    val url: String? = null,
    val timestamp: Long
)

enum class ActivityAction(val displayName: String) {
    LOGIN_ATTEMPT("Login Attempt"),
    LOGIN_SUCCESS("Login Success"),
    LOGIN_FAILURE("Login Failed"),
    SEARCH_EXECUTED("Search Executed"),
    JOB_VIEWED("Job Viewed"),
    EASY_APPLY_STARTED("Easy Apply Started"),
    EASY_APPLY_SUBMITTED("Easy Apply Submitted"),
    EXTERNAL_URL_OPENED("External Site Opened"),
    SCREENING_QUESTION("Screening Question Detected"),
    RESUME_UPLOADED("Resume Uploaded"),
    APPLICATION_SUBMITTED("Application Submitted"),
    APPLICATION_FAILED("Application Failed"),
    BLOCKED_NAVIGATION("Blocked Navigation"),
    RUN_STARTED("Automation Run Started"),
    RUN_COMPLETED("Automation Run Completed")
}
