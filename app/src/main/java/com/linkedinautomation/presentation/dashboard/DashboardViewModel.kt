package com.linkedinautomation.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repo: JobApplicationRepository,
    prefsRepo: UserPreferencesRepository
) : ViewModel() {
    val totalApplied = repo.observeAppliedCount()
    val pendingCount = repo.observePendingCount()
    val recentApplications = repo.observeAll().map { it.take(10) }
    val thisWeekCount = repo.observeAppliedSince(weekStart())

    /** True when the user hasn't filled in personal info (name) yet */
    val personalInfoMissing = prefsRepo.observe().map { it.firstName.isBlank() || it.lastName.isBlank() }

    private fun weekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }
}
