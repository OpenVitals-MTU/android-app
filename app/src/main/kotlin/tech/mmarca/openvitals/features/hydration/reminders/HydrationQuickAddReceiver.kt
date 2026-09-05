package tech.mmarca.openvitals.features.hydration.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** One-tap water logging from the reminder's actions. A failed tap logs nothing. */
@AndroidEntryPoint
class HydrationQuickAddReceiver : BroadcastReceiver() {

    @Inject
    lateinit var controller: HydrationReminderController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK_ADD) return
        val milliliters = intent.getDoubleExtra(EXTRA_AMOUNT_MILLILITERS, Double.NaN)
        if (milliliters.isNaN()) return
        val pending = goAsync()
        controller.handleQuickAdd(milliliters) { pending.finish() }
    }

    companion object {
        const val ACTION_QUICK_ADD = "tech.mmarca.openvitals.HYDRATION_QUICK_ADD"
        const val EXTRA_AMOUNT_MILLILITERS = "amount_milliliters"
    }
}
