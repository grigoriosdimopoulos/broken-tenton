package com.linkedinautomation.presentation.applicationdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.domain.model.JobApplication
import com.linkedinautomation.domain.repository.JobApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationDetailViewModel @Inject constructor(
    private val jobRepo: JobApplicationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val appId: Long = checkNotNull(savedStateHandle["appId"]).toString().toLong()

    private val _application = MutableStateFlow<JobApplication?>(null)
    val application: StateFlow<JobApplication?> = _application

    init {
        viewModelScope.launch {
            _application.value = jobRepo.getById(appId)
        }
    }
}
