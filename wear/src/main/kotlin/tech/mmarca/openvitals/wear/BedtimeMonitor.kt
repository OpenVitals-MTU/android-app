package tech.mmarca.openvitals.wear

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import android.util.Log

/**
 * Tells whether the wearer has bedtime mode on, and calls [onChange] on
 * [handler]'s thread when that flips.
 *
 * Samsung's Wear OS keeps the mode in the global setting
 * `setting_bedtime_mode_running_state` (1 while it runs). Android does not
 * promise a third-party app may read a setting it does not document, so
 * when the read fails or the key is absent the monitor falls back to Do
 * Not Disturb, which bedtime mode switches on and every app may read. The
 * fallback also counts theater mode and a manual Do Not Disturb as
 * bedtime; both are short or deliberate.
 */
class BedtimeMonitor(
    private val context: Context,
    private val handler: Handler,
    private val onChange: (Boolean) -> Unit,
) {
    var isBedtime: Boolean = false
        private set

    private var source: String? = null

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = publish()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publish()
    }

    fun start() {
        runCatching {
            context.contentResolver.registerContentObserver(Settings.Global.getUriFor(BEDTIME_SETTING), false, observer)
        }.onFailure { Log.w(TAG, "Cannot observe the bedtime setting", it) }
        context.registerReceiver(receiver, IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED), null, handler)
        isBedtime = read()
        Log.i(TAG, "Bedtime mode ${if (isBedtime) "on" else "off"} (from $source)")
    }

    fun stop() {
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun publish() {
        val now = read()
        if (now == isBedtime) return
        isBedtime = now
        Log.i(TAG, "Bedtime mode ${if (now) "on" else "off"} (from $source)")
        onChange(now)
    }

    private fun read(): Boolean {
        val setting = runCatching { Settings.Global.getInt(context.contentResolver, BEDTIME_SETTING, -1) }.getOrDefault(-1)
        if (setting >= 0) {
            source = BEDTIME_SETTING
            return setting == 1
        }
        source = "do not disturb"
        val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
            ?: return false
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private companion object {
        const val TAG = "BedtimeMonitor"
        const val BEDTIME_SETTING = "setting_bedtime_mode_running_state"
    }
}
