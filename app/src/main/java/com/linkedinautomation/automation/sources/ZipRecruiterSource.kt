package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class ZipRecruiterSource : JobSource {
    override val sourceName = "ZipRecruiter"
    override val scanScriptAsset = ScriptRegistry.ZIPRECRUITER_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val q = prefs.jobKeywords.joinToString(" ")
        val l = prefs.location
        val remote = if (prefs.remoteOnly) "&refine_by_location_type=only_remote" else ""
        return "https://www.ziprecruiter.com/jobs-search" +
                "?search=${URLEncoder.encode(q, "UTF-8")}" +
                "&location=${URLEncoder.encode(l, "UTF-8")}" +
                "$remote&days=1"
    }
}
