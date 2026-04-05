package com.linkedinautomation.presentation.history

import androidx.lifecycle.ViewModel
import com.linkedinautomation.domain.usecase.GetActivityLogUseCase
import com.linkedinautomation.domain.usecase.GetApplicationHistoryUseCase
import com.linkedinautomation.domain.usecase.GetClaudeUsageLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getApplicationHistory: GetApplicationHistoryUseCase,
    getActivityLog: GetActivityLogUseCase,
    getClaudeUsageLog: GetClaudeUsageLogUseCase
) : ViewModel() {
    val applications = getApplicationHistory()
    val activityLogs = getActivityLog()
    val claudeLogs = getClaudeUsageLog()
    val totalTokens = getClaudeUsageLog.totalTokens()
}
