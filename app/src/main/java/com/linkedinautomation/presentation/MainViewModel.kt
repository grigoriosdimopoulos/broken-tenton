package com.linkedinautomation.presentation

import androidx.lifecycle.ViewModel
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    prefsRepo: UserPreferencesRepository,
    jobRepo: JobApplicationRepository
) : ViewModel() {
    val isSetupComplete = prefsRepo.observe().map { it.isSetupComplete }
    val pendingCount = jobRepo.observePendingCount()
}
