package com.linkedinautomation.automation.sources

import com.linkedinautomation.domain.model.UserPreferences

interface JobSource {
    val sourceName: String
    val scanScriptAsset: String
    fun buildSearchUrl(prefs: UserPreferences): String
}
