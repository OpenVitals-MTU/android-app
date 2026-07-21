package tech.mmarca.openvitals.bluetooth_sync_native

import android.companion.CompanionDeviceService
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Bound by the OS while an associated device (a Garmin watch) is in range.
 *
 * Ported from Gadgetbridge's `service/GBCompanionDeviceService.java` (AGPLv3),
 * minus its reconnect logic — this app has no background auto-connect, and
 * inventing one here would start BLE work the user never asked for.
 *
 * SO WHY REGISTER IT AT ALL? The binding itself is the feature. From the
 * platform docs:
 *
 * > The system binding CompanionDeviceService elevates the priority of the
 * > process that the service is running in, and thus may prevent the Low-memory
 * > killer from killing the process at expense of other processes with lower
 * > priority.
 *
 * A FIT-file sync over BLE runs for minutes; that priority bump is what stops it
 * being killed halfway through. The callbacks below are observation points only.
 *
 * API 31+. Below that the OS never binds this and nothing is lost but the boost.
 */
@RequiresApi(Build.VERSION_CODES.S)
class OpenVitalsCompanionDeviceService : CompanionDeviceService() {
    override fun onDeviceAppeared(address: String) {
        Log.d(SyncBluetooth.TAG, "companion device appeared: $address")
    }

    override fun onDeviceDisappeared(address: String) {
        Log.d(SyncBluetooth.TAG, "companion device disappeared: $address")
    }
}
