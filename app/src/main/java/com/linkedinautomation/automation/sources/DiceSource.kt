package com.linkedinautomation.automation.sources

import com.linkedinautomation.automation.scripts.ScriptRegistry
import com.linkedinautomation.domain.model.UserPreferences
import java.net.URLEncoder

class DiceSource : JobSource {
    override val sourceName = "Dice"
    override val scanScriptAsset = ScriptRegistry.DICE_SCAN

    override fun buildSearchUrl(prefs: UserPreferences): String {
        val q = prefs.jobKeywords.joinToString(" ")
        val location = prefs.location
        val remote = if (prefs.remoteOnly) "&filters.workplaceTypes=Remote" else ""
        return "https://www.dice.com/jobs" +
                "?q=${URLEncoder.encode(q, "UTF-8")}" +
                "&location=${URLEncoder.encode(location, "UTF-8")}" +
                "&radius=30&radiusUnit=mi" +
                "&filters.postedDate=ONE" + // last 24h
                remote
    }
}
