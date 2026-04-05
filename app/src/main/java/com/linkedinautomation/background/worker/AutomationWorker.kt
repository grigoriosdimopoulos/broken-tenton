package com.linkedinautomation.background.worker

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.linkedinautomation.background.service.AutomationForegroundService
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AutomationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prefsRepo: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = prefsRepo.get()
        if (!prefs.automationEnabled) return Result.success()
        if (prefs.claudeApiKey.isBlank() || prefs.jobKeywords.isEmpty()) return Result.failure()

        val intent = Intent(applicationContext, AutomationForegroundService::class.java)
        applicationContext.startForegroundService(intent)
        return Result.success()
    }
}
