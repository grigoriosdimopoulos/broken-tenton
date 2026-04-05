package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class IndeedSource : JobSource {
    override val sourceName = "Indeed"
    override val scanScriptAsset = ScriptRegistry.INDEED_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val q = prefs.jobKeywords.joinToString(" ")
        val l = prefs.location
        val remote = if (prefs.remoteOnly) "&remotejob=032b3046-06a3-4876-8dfd-474eb5e7ed11" else ""
        return "https://www.indeed.com/jobs" +
                "?q=${URLEncoder.encode(q, "UTF-8")}" +
                "&l=${URLEncoder.encode(l, "UTF-8")}" +
                "$remote&sort=date"
    }
}
