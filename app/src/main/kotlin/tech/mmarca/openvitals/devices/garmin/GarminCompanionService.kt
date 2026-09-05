package tech.mmarca.openvitals.devices.garmin

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The OS's half of companion mode: bound when the associated watch comes
 * into range, waking the app. Relays presence to the bridge.
 */
@RequiresApi(Build.VERSION_CODES.S)
@AndroidEntryPoint
class GarminCompanionService : CompanionDeviceService() {

    @Inject
    lateinit var bridge: GarminNotificationBridge

    @Deprecated("Deprecated in API 33, still delivered on 31-32")
    override fun onDeviceAppeared(address: String) {
        bridge.onWatchAppeared(address)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        val address = associationInfo.deviceMacAddress?.toString()
        if (address != null) {
            bridge.onWatchAppeared(address)
        }
    }

    @Deprecated("Deprecated in API 33, still delivered on 31-32")
    override fun onDeviceDisappeared(address: String) {
        // Nothing to tear down: the forwarder's backoff idles cheaply and keeps the queue.
        GarminLog.log("[GARMIN-COMPANION] $address went out of range")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        GarminLog.log(
            "[GARMIN-COMPANION] ${associationInfo.deviceMacAddress} went out of range",
        )
    }
}
