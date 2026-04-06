package com.linkedinautomation.presentation.navigation

sealed class Screen(val route: String) {
    // Setup flow
    object Welcome : Screen("setup/welcome")
    object SourceMode : Screen("setup/source_mode")
    object Credentials : Screen("setup/credentials")
    object JobBoardSelection : Screen("setup/job_boards")
    object PersonalInfo : Screen("setup/personal_info")
    object JobPreferences : Screen("setup/job_preferences")
    object Exclusions : Screen("setup/exclusions")
    object ExperienceBio : Screen("setup/experience_bio")
    object ClaudeSetup : Screen("setup/claude_setup")
    object Resume : Screen("setup/resume")
    object SetupComplete : Screen("setup/complete")

    // Main tabs
    object Dashboard : Screen("main/dashboard")
    object ApprovalQueue : Screen("main/approval_queue")
    object Monitor : Screen("main/monitor")
    object History : Screen("main/history")
    object Settings : Screen("main/settings")

    // LinkedIn cookie login
    object LinkedInLogin : Screen("linkedin/login")

    // Settings sub-screens
    object ClaudePersona : Screen("settings/claude_persona")
    object SettingsPersonalInfo : Screen("settings/personal_info")
    object SettingsCredentials : Screen("settings/credentials")
    object SettingsJobPrefs : Screen("settings/job_prefs")
    object SettingsResume : Screen("settings/resume")
    object SettingsBackup : Screen("settings/backup")

    // Application detail
    object ApplicationDetail : Screen("application/{appId}") {
        fun route(appId: Long) = "application/$appId"
    }
}
