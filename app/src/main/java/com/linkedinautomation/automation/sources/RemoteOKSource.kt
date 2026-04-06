package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class RemoteOKSource : JobSource {
    override val sourceName = "RemoteOK"
    override val scanScriptAsset = ScriptRegistry.REMOTEOK_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        // RemoteOK uses slug-style URLs: /remote-KEYWORD-jobs
        val keyword = prefs.jobKeywords.firstOrNull()?.lowercase()
            ?.replace(" ", "-") ?: "developer"
        return "https://remoteok.com/remote-${URLEncoder.encode(keyword, "UTF-8")}-jobs"
    }
}
