package com.linkedinautomation.domain.model

enum class JobBoardSource(val displayName: String, val baseUrl: String) {
    INDEED("Indeed", "https://www.indeed.com"),
    GLASSDOOR("Glassdoor", "https://www.glassdoor.com"),
    ZIPRECRUITER("ZipRecruiter", "https://www.ziprecruiter.com"),
    MONSTER("Monster", "https://www.monster.com"),
    SIMPLYHIRED("SimplyHired", "https://www.simplyhired.com"),
    DICE("Dice (Tech)", "https://www.dice.com"),
    REMOTEOK("RemoteOK", "https://remoteok.com"),
    WEWORKREMOTELY("WeWorkRemotely", "https://weworkremotely.com")
}
