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

        // Build work-type filter
        // f_WT=1 → On-site, f_WT=2 → Remote, f_WT=3 → Hybrid
        // When the user specifies a location and doesn't want remote, exclude remote listings
        val workType = when {
            prefs.remoteOnly -> "&f_WT=2"
            prefs.hybridOk && prefs.onsiteOk -> "&f_WT=1%2C3"  // on-site + hybrid; remote filtered in Kotlin
            prefs.hybridOk -> "&f_WT=3"                         // hybrid only
            prefs.onsiteOk -> "&f_WT=1"                         // on-site only
            else -> "&f_WT=1%2C3"
        }

        return "https://www.linkedin.com/jobs/search/" +
                "?keywords=${URLEncoder.encode(keywords, "UTF-8")}" +
                "&location=${URLEncoder.encode(location, "UTF-8")}" +
                workType +
                "&sortBy=DD" +
                "&f_TPR=r86400" // last 24 h
    }
}
