package tech.mmarca.openvitals.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the phone link back after a watch restart or an app update, so the
 * phone's ping is answered without opening the app first. Both broadcasts may
 * start a `connectedDevice` foreground service from the background.
 */
class WearBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> WearAppService.startIfPermitted(context)
        }
    }
}
