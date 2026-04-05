package com.linkedinautomation.presentation.approvalqueue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.automation.orchestrator.AutomationOrchestrator
import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import com.linkedinautomation.domain.usecase.ApproveJobApplicationUseCase
import com.linkedinautomation.domain.usecase.RejectJobApplicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ApprovalQueueViewModel @Inject constructor(
    repo: JobApplicationRepository,
    private val approveUseCase: ApproveJobApplicationUseCase,
    private val rejectUseCase: RejectJobApplicationUseCase,
    private val orchestrator: AutomationOrchestrator,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    val pending: Flow<List<JobApplication>> = repo.observePending()

    fun approve(job: JobApplication, filesDir: java.io.File) {
        viewModelScope.launch {
            approveUseCase(job.id)
            val prefs = prefsRepo.get()
            val resumePath = File(filesDir, "resume.pdf").takeIf { it.exists() }?.absolutePath
            orchestrator.applyApproved(job.id, prefs, resumePath)
        }
    }

    fun reject(id: Long) {
        viewModelScope.launch { rejectUseCase(id) }
    }
}
