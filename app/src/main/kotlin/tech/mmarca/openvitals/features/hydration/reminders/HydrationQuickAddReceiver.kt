package tech.mmarca.openvitals.features.hydration.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * One-tap water logging from the hydration reminder notification's action
 * buttons. Logs the carried volume silently, without opening the app; a failed
 * tap simply logs nothing — the action already dismissed its notification, so
 * there is no UI to report a refusal to.
 */
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
