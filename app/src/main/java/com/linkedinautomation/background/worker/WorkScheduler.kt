package com.linkedinautomation.background.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)
    private val WORK_TAG = "automation_periodic"

    fun schedule(intervalMinutes: Int = 30) {
        val request = PeriodicWorkRequestBuilder<AutomationWorker>(
            intervalMinutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES
        )
            .addTag(WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    fun isScheduled(): Boolean {
        val infos = workManager.getWorkInfosByTag(WORK_TAG).get()
        return infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    }
}
