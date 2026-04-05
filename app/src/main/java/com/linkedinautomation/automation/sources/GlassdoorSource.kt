package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class GlassdoorSource : JobSource {
    override val sourceName = "Glassdoor"
    override val scanScriptAsset = ScriptRegistry.GLASSDOOR_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val kw = prefs.jobKeywords.joinToString("-").replace(" ", "-")
        val loc = prefs.location.replace(" ", "-")
        return "https://www.glassdoor.com/Job/${URLEncoder.encode(loc, "UTF-8").lowercase()}" +
                "-${URLEncoder.encode(kw, "UTF-8").lowercase()}-jobs-SRCH_IL.0,${loc.length}" +
                "_IN1_KO${loc.length + 1},${loc.length + 1 + kw.length}.htm?sortBy=date_desc"
    }
}
