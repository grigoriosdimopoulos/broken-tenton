package com.linkedinautomation.domain.model

data class UserPreferences(
    val isSetupComplete: Boolean = false,
    val sourceMode: SourceMode = SourceMode.LINKEDIN,
    // LinkedIn credentials
    val linkedInEmail: String = "",
    val linkedInPassword: String = "",
    // Direct mode: selected job boards
    val selectedJobBoards: List<JobBoardSource> = listOf(JobBoardSource.INDEED, JobBoardSource.GLASSDOOR),
    // Job search
    val jobKeywords: List<String> = emptyList(),
    val location: String = "",
    val remoteOnly: Boolean = false,
    val hybridOk: Boolean = true,
    val onsiteOk: Boolean = true,
    // Filters
    val jobTypes: List<String> = listOf("FULL_TIME"),
    val experienceLevels: List<String> = listOf("MID_SENIOR"),
    val targetIndustries: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val excludeCompanies: List<String> = emptyList(),
    // Experience bio for Claude context
    val experienceBio: String = "",
    // Claude
    val claudeApiKey: String = "",
    val claudePersona: ClaudePersona = ClaudePersona(),
    // Resume
    val resumeFileName: String = "",
    // Automation settings
    val automationEnabled: Boolean = false,
    val requireApproval: Boolean = false,
    val scanIntervalMinutes: Int = 30,
    // Personal info for application form auto-fill
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val city: String = "",
    val country: String = "",
    val linkedInUrl: String = "",
    val currentJobTitle: String = "",
    val yearsOfExperience: Int = 0,
    // Salary filter (minimum, in thousands USD; 0 = no minimum)
    val minSalary: Int = 0,
    // Email for application forms
    val email: String = ""
)
