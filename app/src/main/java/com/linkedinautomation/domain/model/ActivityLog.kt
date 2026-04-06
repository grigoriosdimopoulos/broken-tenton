package com.linkedinautomation.domain.model

data class ActivityLog(
    val id: Long = 0,
    val action: ActivityAction,
    val details: String,
    val url: String? = null,
    val timestamp: Long,
    val screenshotPath: String? = null
)

enum class ActivityAction(val displayName: String) {
    LOGIN_ATTEMPT("Login Attempt"),
    LOGIN_SUCCESS("Login Success"),
    LOGIN_FAILURE("Login Failed"),
    SEARCH_EXECUTED("Search Executed"),
    JOB_VIEWED("Job Viewed"),
    EASY_APPLY_STARTED("Easy Apply Started"),
    EASY_APPLY_SUBMITTED("Easy Apply Submitted"),
    EASY_APPLY_FAILED("Easy Apply Failed"),
    EASY_APPLY_STEP("Easy Apply Step"),
    EXTERNAL_URL_OPENED("External Site Opened"),
    SCREENING_QUESTION("Screening Question"),
    RESUME_UPLOADED("Resume Uploaded"),
    APPLICATION_SUBMITTED("Application Submitted"),
    APPLICATION_FAILED("Application Failed"),
    BLOCKED_NAVIGATION("Blocked Navigation"),
    RUN_STARTED("Automation Run Started"),
    RUN_COMPLETED("Automation Run Completed"),
    LOCATION_SKIPPED("Job Skipped — Location"),
    MODAL_CHECK("Submit Verification")
}
