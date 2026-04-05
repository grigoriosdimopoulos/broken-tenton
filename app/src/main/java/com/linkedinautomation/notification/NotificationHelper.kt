package com.linkedinautomation.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.linkedinautomation.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        val channels = listOf(
            NotificationChannel(
                NotificationChannels.AUTOMATION_STATUS,
                "Automation Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows automation running status" },
            NotificationChannel(
                NotificationChannels.JOB_APPLIED,
                "Job Applications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when a job is applied to" },
            NotificationChannel(
                NotificationChannels.PENDING_APPROVAL,
                "Pending Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when jobs need your approval" }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun buildForegroundNotification(status: String = "Running..."): Notification {
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, NotificationChannels.AUTOMATION_STATUS)
            .setContentTitle("Job Automator")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(intent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun notifyApplied(jobTitle: String, company: String) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.JOB_APPLIED)
            .setContentTitle("Applied!")
            .setContentText("$jobTitle at $company")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun notifyPendingApproval(count: Int) {
        val intent = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).putExtra("tab", "approval"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.PENDING_APPROVAL)
            .setContentTitle("$count jobs waiting for approval")
            .setContentText("Tap to review and approve")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        manager.notify(NotificationChannels.APPROVAL_NOTIFICATION_ID, notification)
    }
}
