package tech.mmarca.openvitals.devices.core.pairing

import android.companion.CompanionDeviceService
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Bound by the OS while an associated device is in range. For a watch the
 * binding is the feature: it raises the process priority, which keeps a
 * minutes-long sync from being killed. It is also the app's primary
 * companion service, the only one Android tells that a device appeared, so
 * it passes that on to the [CompanionPresenceObserver]s. API 31+.
 */
@RequiresApi(Build.VERSION_CODES.S)
@AndroidEntryPoint
class OpenVitalsCompanionDeviceService : CompanionDeviceService() {

    @Inject
    lateinit var observers: Set<@JvmSuppressWildcards CompanionPresenceObserver>

    // The address is not logged: a MAC identifies the person.
    override fun onDeviceAppeared(address: String) {
        Log.d(CompanionDevicePairing.TAG, "companion device appeared")
        observers.forEach { it.onCompanionDeviceAppeared(address) }
    }

    override fun onDeviceDisappeared(address: String) {
        Log.d(CompanionDevicePairing.TAG, "companion device disappeared")
    }
}
