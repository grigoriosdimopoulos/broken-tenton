package com.linkedinautomation.presentation.activitydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.domain.model.ActivityLog
import com.linkedinautomation.domain.repository.ActivityLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepo: ActivityLogRepository
) : ViewModel() {

    private val logId: Long = checkNotNull(savedStateHandle["logId"])

    private val _log = MutableStateFlow<ActivityLog?>(null)
    val log: StateFlow<ActivityLog?> = _log

    init {
        viewModelScope.launch {
            _log.value = activityRepo.getById(logId)
        }
    }
}
