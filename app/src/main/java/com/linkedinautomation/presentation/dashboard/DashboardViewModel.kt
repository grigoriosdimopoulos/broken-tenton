package com.linkedinautomation.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.linkedinautomation.domain.repository.JobApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repo: JobApplicationRepository
) : ViewModel() {
    val totalApplied = repo.observeAppliedCount()
    val pendingCount = repo.observePendingCount()
    val recentApplications = repo.observeAll().map { it.take(10) }
    val thisWeekCount = repo.observeAppliedSince(weekStart())

    private fun weekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        return cal.timeInMillis
    }
}
