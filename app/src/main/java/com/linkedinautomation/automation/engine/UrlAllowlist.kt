package com.linkedinautomation.automation.engine

import com.linkedinautomation.domain.model.SourceMode

object UrlAllowlist {

    // ATS domains always permitted (apply destinations)
    private val ATS_DOMAINS = setOf(
        "greenhouse.io",
        "lever.co",
        "workday.com",
        "myworkdayjobs.com",
        "taleo.net",
        "bamboohr.com",
        "icims.com",
        "smartrecruiters.com",
        "jobvite.com",
        "successfactors.com",
        "applytojob.com",
        "recruitee.com",
        "pinpointhq.com",
        "ashbyhq.com",
        "rippling.com"
    )

    // Job board domains for Direct mode
    private val JOB_BOARD_DOMAINS = setOf(
        "indeed.com",
        "glassdoor.com",
        "ziprecruiter.com",
        "monster.com",
        "simplyhired.com"
    )

    // LinkedIn paths allowed in LinkedIn mode
    private val LINKEDIN_ALLOWED_PATH_PREFIXES = listOf(
        "/jobs/",
        "/login",
        "/checkpoint/",
        "/uas/"
    )

    fun isAllowed(url: String, sourceMode: SourceMode): Boolean {
        val lowerUrl = url.lowercase()
        val host = extractHost(lowerUrl) ?: return false

        // Always allow ATS domains
        if (ATS_DOMAINS.any { host.endsWith(it) }) return true

        return when (sourceMode) {
            SourceMode.LINKEDIN -> isLinkedInAllowed(lowerUrl, host)
            SourceMode.DIRECT -> isDirectAllowed(host)
        }
    }

    fun isBlockedLinkedInUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        val host = extractHost(lowerUrl) ?: return false
        if (!host.contains("linkedin.com")) return false
        return !isLinkedInAllowed(lowerUrl, host)
    }

    fun detectAtsName(url: String): String {
        val host = extractHost(url.lowercase()) ?: return "External"
        return when {
            host.contains("greenhouse.io") -> "Greenhouse"
            host.contains("lever.co") -> "Lever"
            host.contains("workday.com") || host.contains("myworkdayjobs.com") -> "Workday"
            host.contains("taleo.net") -> "Taleo"
            host.contains("bamboohr.com") -> "BambooHR"
            host.contains("icims.com") -> "iCIMS"
            host.contains("smartrecruiters.com") -> "SmartRecruiters"
            host.contains("jobvite.com") -> "Jobvite"
            host.contains("successfactors.com") -> "SAP SuccessFactors"
            host.contains("ashbyhq.com") -> "Ashby"
            host.contains("rippling.com") -> "Rippling"
            else -> host.removePrefix("www.").substringBefore(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    private fun isLinkedInAllowed(url: String, host: String): Boolean {
        if (!host.contains("linkedin.com")) return false
        return LINKEDIN_ALLOWED_PATH_PREFIXES.any { prefix ->
            val path = url.substringAfter("linkedin.com")
            path.startsWith(prefix)
        }
    }

    private fun isDirectAllowed(host: String): Boolean =
        JOB_BOARD_DOMAINS.any { host.endsWith(it) }

    private fun extractHost(url: String): String? = runCatching {
        url.removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore("?")
    }.getOrNull()
}
