package com.linkedinautomation.background.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.linkedinautomation.automation.orchestrator.AutomationOrchestrator
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import com.linkedinautomation.notification.NotificationChannels
import com.linkedinautomation.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AutomationForegroundService : Service() {

    @Inject lateinit var orchestrator: AutomationOrchestrator
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var prefsRepo: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        orchestrator.onNotifyApplied = { title, company ->
            notificationHelper.notifyApplied(title, company)
        }
        orchestrator.onNotifyPendingApproval = { count ->
            notificationHelper.notifyPendingApproval(count)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NotificationChannels.FOREGROUND_NOTIFICATION_ID,
            notificationHelper.buildForegroundNotification("Starting automation...")
        )

        scope.launch {
            val prefs = prefsRepo.get()
            if (!prefs.automationEnabled) {
                stopSelf()
                return@launch
            }
            val resumePath = File(filesDir, "resume.pdf")
                .takeIf { it.exists() }?.absolutePath
            orchestrator.run(prefs, resumePath)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
