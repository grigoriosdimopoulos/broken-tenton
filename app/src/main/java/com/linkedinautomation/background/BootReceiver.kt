package com.linkedinautomation.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.linkedinautomation.background.worker.WorkScheduler
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var prefsRepo: UserPreferencesRepository
    @Inject lateinit var workScheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        CoroutineScope(Dispatchers.IO).launch {
            val prefs = prefsRepo.get()
            if (prefs.isSetupComplete && prefs.automationEnabled) {
                workScheduler.schedule(prefs.scanIntervalMinutes)
            }
        }
    }
}
