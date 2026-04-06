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

        // Map minimum salary (in $k) to LinkedIn's f_SB2 salary bucket
        // f_SB2=1→$40k+, 2→$60k+, 3→$80k+, 4→$100k+, 5→$120k+, 6→$140k+, 7→$160k+, 8→$180k+, 9→$200k+
        val salaryFilter = when {
            prefs.minSalary >= 200 -> "&f_SB2=9"
            prefs.minSalary >= 180 -> "&f_SB2=8"
            prefs.minSalary >= 160 -> "&f_SB2=7"
            prefs.minSalary >= 140 -> "&f_SB2=6"
            prefs.minSalary >= 120 -> "&f_SB2=5"
            prefs.minSalary >= 100 -> "&f_SB2=4"
            prefs.minSalary >= 80  -> "&f_SB2=3"
            prefs.minSalary >= 60  -> "&f_SB2=2"
            prefs.minSalary >= 40  -> "&f_SB2=1"
            else -> ""
        }

        return "https://www.linkedin.com/jobs/search/" +
                "?keywords=${URLEncoder.encode(keywords, "UTF-8")}" +
                "&location=${URLEncoder.encode(location, "UTF-8")}" +
                workType +
                salaryFilter +
                "&sortBy=DD" +
                "&f_TPR=r86400" // last 24 h
    }
}
