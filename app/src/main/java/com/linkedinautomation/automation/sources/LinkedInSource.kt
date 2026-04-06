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
        val workType = when {
            prefs.remoteOnly -> "&f_WT=2"
            !prefs.hybridOk && !prefs.onsiteOk -> "&f_WT=2" // fallback to remote if nothing else
            !prefs.hybridOk -> "&f_WT=1"                   // on-site only
            !prefs.onsiteOk -> "&f_WT=3"                   // hybrid only
            else -> "&f_WT=1%2C3"                          // on-site + hybrid (exclude remote)
        }

        // Distance parameter (25 miles / ~40 km from location)
        val distance = if (!prefs.remoteOnly && location.isNotBlank()) "&distance=25" else ""

        return "https://www.linkedin.com/jobs/search/" +
                "?keywords=${URLEncoder.encode(keywords, "UTF-8")}" +
                "&location=${URLEncoder.encode(location, "UTF-8")}" +
                workType +
                distance +
                "&sortBy=DD" +
                "&f_TPR=r86400" // posted in last 24 hours
    }
}
