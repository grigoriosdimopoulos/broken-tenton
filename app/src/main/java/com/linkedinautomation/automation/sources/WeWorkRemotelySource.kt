package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences

class WeWorkRemotelySource : JobSource {
    override val sourceName = "WeWorkRemotely"
    override val scanScriptAsset = ScriptRegistry.WEWORKREMOTELY_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val keyword = prefs.jobKeywords.firstOrNull() ?: "developer"
        return "https://weworkremotely.com/remote-jobs/search?term=${java.net.URLEncoder.encode(keyword, "UTF-8")}"
    }
}
