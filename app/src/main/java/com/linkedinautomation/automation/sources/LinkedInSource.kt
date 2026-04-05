package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class LinkedInSource : JobSource {
    override val sourceName = "LinkedIn"
    override val scanScriptAsset = ScriptRegistry.LINKEDIN_SCAN_JOBS

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val keywords = prefs.jobKeywords.joinToString(" ")
        val location = prefs.location
        val remote = if (prefs.remoteOnly) "&f_WT=2" else ""
        return "https://www.linkedin.com/jobs/search/" +
                "?keywords=${URLEncoder.encode(keywords, "UTF-8")}" +
                "&location=${URLEncoder.encode(location, "UTF-8")}" +
                "$remote&sortBy=DD"
    }
}
