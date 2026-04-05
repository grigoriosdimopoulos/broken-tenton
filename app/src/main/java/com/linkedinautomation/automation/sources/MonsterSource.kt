package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class MonsterSource : JobSource {
    override val sourceName = "Monster"
    override val scanScriptAsset = ScriptRegistry.MONSTER_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val q = prefs.jobKeywords.joinToString("-")
        val l = prefs.location
        return "https://www.monster.com/jobs/search" +
                "?q=${URLEncoder.encode(q, "UTF-8")}" +
                "&where=${URLEncoder.encode(l, "UTF-8")}" +
                "&tm=1"
    }
}
