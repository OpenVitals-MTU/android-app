package tech.mmarca.openvitals.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * The wearer's answer to a phone that asked to be allowed: a notification
 * with Allow and Block, and the same two choices on the status screen. The
 * notification is posted at most once a minute per phone.
 */
object WearPhoneRequests {

    const val CHANNEL_ID = "phone_requests"
    const val NOTIFICATION_ID = 2
    const val ACTION_ALLOW = "tech.mmarca.openvitals.wear.ALLOW_PHONE"
    const val ACTION_BLOCK = "tech.mmarca.openvitals.wear.BLOCK_PHONE"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_TOKEN = "token"
    const val EXTRA_NAME = "name"

    private val lastNotified = HashMap<String, Long>()

    fun notify(context: Context, pending: PendingPhone, nowMillis: Long = System.currentTimeMillis()) {
        synchronized(lastNotified) {
            val last = lastNotified[pending.address] ?: 0L
            if (nowMillis - last < RENOTIFY_MILLIS) return
            lastNotified[pending.address] = nowMillis
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.link_requests_channel_name), NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.status_phone_pending, pending.name))
            .setContentText(context.getString(R.string.phone_request_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(action(context, ACTION_ALLOW, R.string.action_allow, pending, 1))
            .addAction(action(context, ACTION_BLOCK, R.string.action_block, pending, 2))
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
            .onFailure { Log.w(TAG, "Cannot post the request: ${it.message}") }
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun action(context: Context, action: String, label: Int, pending: PendingPhone, code: Int): Notification.Action {
        val intent = Intent(context, WearPhoneConfirmReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_ADDRESS, pending.address)
            .putExtra(EXTRA_TOKEN, pending.token)
            .putExtra(EXTRA_NAME, pending.name)
        val pendingIntent = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(null, context.getString(label), pendingIntent).build()
    }

    private const val TAG = "WearPhoneRequests"
    private const val RENOTIFY_MILLIS = 60_000L
}

/** Allow or Block from the notification. The screen's buttons call the store directly. */
class WearPhoneConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(WearPhoneRequests.EXTRA_ADDRESS) ?: return
        val store = WearTrustStore(context)
        when (intent.action) {
            WearPhoneRequests.ACTION_ALLOW -> {
                val token = intent.getStringExtra(WearPhoneRequests.EXTRA_TOKEN) ?: return
                store.trust(address, token, intent.getStringExtra(WearPhoneRequests.EXTRA_NAME) ?: address)
            }
            WearPhoneRequests.ACTION_BLOCK -> store.block(address)
            else -> return
        }
        WearPhoneRequests.cancel(context)
    }
}
