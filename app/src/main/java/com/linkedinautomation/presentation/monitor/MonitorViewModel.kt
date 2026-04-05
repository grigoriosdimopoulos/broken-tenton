package com.linkedinautomation.presentation.monitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.automation.orchestrator.AutomationOrchestrator
import com.linkedinautomation.automation.orchestrator.AutomationState
import com.linkedinautomation.background.worker.WorkScheduler
import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val orchestrator: AutomationOrchestrator,
    private val prefsRepo: UserPreferencesRepository,
    private val workScheduler: WorkScheduler
) : ViewModel() {

    val automationState: StateFlow<AutomationState> = orchestrator.state
    val liveLog: StateFlow<List<String>> = orchestrator.liveLog
    val prefs = prefsRepo.observe()

    fun setAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutomationEnabled(enabled)
            if (enabled) {
                val p = prefsRepo.get()
                workScheduler.schedule(p.scanIntervalMinutes)
            } else {
                workScheduler.cancel()
            }
        }
    }

    fun setRequireApproval(require: Boolean) {
        viewModelScope.launch { prefsRepo.setRequireApproval(require) }
    }

    /** Emergency stop: pause orchestrator + cancel all scheduled work */
    fun stopAll() {
        orchestrator.pause()
        workScheduler.cancel()
        viewModelScope.launch { prefsRepo.setAutomationEnabled(false) }
    }
}
