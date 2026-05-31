package tech.mmarca.openvitals.features.hydration.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HydrationReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: HydrationReminderController

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        controller.handleReminderAlarm {
            pendingResult.finish()
        }
    }
}
