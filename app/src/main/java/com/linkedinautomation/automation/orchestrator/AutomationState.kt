package com.linkedinautomation.automation.orchestrator

sealed class AutomationState {
    object Idle : AutomationState()
    data class Running(val message: String) : AutomationState()
    object WaitingApproval : AutomationState()
    object Paused : AutomationState()
    data class Error(val message: String) : AutomationState()
}

data class ScrapedJob(
    val id: String,
    val title: String,
    val company: String,
    val isEasyApply: Boolean,
    val url: String,
    val source: String,
    val location: String = ""   // as shown on the job card
)
