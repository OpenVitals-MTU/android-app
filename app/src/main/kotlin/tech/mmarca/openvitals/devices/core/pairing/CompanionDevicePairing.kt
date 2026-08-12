package tech.mmarca.openvitals.devices.core.pairing

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * CompanionDeviceManager association, used when onboarding a Garmin watch.
 *
 * Ported from the Flutter build's `CompanionDevices.kt` (itself ported from
 * Gadgetbridge's `util/BondingUtil.java` — AGPLv3, the same licence as this
 * app), with the plugin's callback API converted to a suspend function. The
 * association is what lets the OS raise this app's process priority while the
 * watch is in range (see [OpenVitalsCompanionDeviceService]), which a long BLE
 * file sync needs.
 *
 * WHY EVERYTHING DEGRADES QUIETLY: association is optional. The user can
 * decline the system dialog, and presence observation needs API 31+ while the
 * app's minSdk is lower. Neither is an error — a watch that is bonded but not
 * associated still syncs, just without the priority boost. So every method
 * here returns "no association" rather than throwing.
 *
 * WHAT IS NOT LOGGED: the device address. `Log` is not stripped from a release
 * build, and a Bluetooth MAC is a stable identifier for the person carrying
 * it. These lines say what happened, not to whom.
 *
 * Association is scoped to an Activity: the OS hands back an [IntentSender]
 * that must be launched for result. With no Activity attached there is no
 * dialog to show and [associate] resolves false.
 */
@Singleton
class CompanionDevicePairing @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {
    /** Launcher + pending continuation for the association dialog's result. */
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var pendingContinuation: CancellableContinuation<Boolean>? = null

    /** The address being associated, so a success can start presence observation. */
    private var pendingAddress: String? = null

    private fun manager(): CompanionDeviceManager? =
        applicationContext.getSystemService(CompanionDeviceManager::class.java)

    // -------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------

    fun attachToActivity(activity: Activity) {
        val componentActivity = activity as? ComponentActivity
        if (componentActivity == null) {
            Log.w(TAG, "activity is not a ComponentActivity; companion association unavailable")
            return
        }
        detachFromActivity()
        launcher =
            componentActivity.activityResultRegistry.register(
                "tech.mmarca.openvitals.devices.core.pairing.companion",
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result: ActivityResult ->
                val address = pendingAddress
                val allowed = result.resultCode == Activity.RESULT_OK
                if (allowed && address != null) startObservingPresence(address)
                Log.i(TAG, "companion association allowed=$allowed")
                resolvePending(allowed)
            }
    }

    fun detachFromActivity() {
        launcher?.unregister()
        launcher = null
        // A dialog in flight when the Activity goes away can never report
        // back. Resolve it as declined rather than leaking the caller forever.
        resolvePending(false)
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Asks the OS to associate [address] with this app, showing the system
     * "Allow OpenVitals to access <watch>?" dialog. True when the user allows
     * it; false on decline and on every degraded path (no activity attached,
     * invalid address, CDM unavailable, a request already in flight, or the
     * platform refusing the request).
     */
    suspend fun associate(address: String, @Suppress("UNUSED_PARAMETER") displayName: String? = null): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.w(TAG, "associate: invalid address")
            return false
        }
        val manager = manager()
        if (manager == null) {
            Log.i(TAG, "associate: CompanionDeviceManager unavailable")
            return false
        }
        // Already associated: the OS never invokes the callback for a repeat
        // request, so short-circuiting is what keeps re-onboarding from
        // hanging (Gadgetbridge hits the same trap, BondingUtil.java:377).
        if (isAssociated(address)) {
            Log.i(TAG, "associate: already associated")
            startObservingPresence(address)
            return true
        }
        val activeLauncher = launcher
        if (activeLauncher == null) {
            Log.w(TAG, "associate: no activity attached")
            return false
        }
        if (pendingContinuation != null) {
            Log.w(TAG, "associate: a request is already in flight")
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            pendingContinuation = continuation
            pendingAddress = address
            continuation.invokeOnCancellation {
                synchronized(this) {
                    if (pendingContinuation === continuation) {
                        pendingContinuation = null
                        pendingAddress = null
                    }
                }
            }

            // A Garmin watch is reached over BLE, so it is filtered by scan
            // filter rather than by classic MAC — the classic filter would
            // never match and the dialog would sit on "searching" forever.
            val request =
                AssociationRequest.Builder()
                    .addDeviceFilter(
                        BluetoothLeDeviceFilter.Builder()
                            .setScanFilter(ScanFilter.Builder().setDeviceAddress(address).build())
                            .build(),
                    )
                    .setSingleDevice(true)
                    .build()

            Log.i(TAG, "associate: requesting association")
            try {
                manager.associate(
                    request,
                    object : CompanionDeviceManager.Callback() {
                        override fun onDeviceFound(intentSender: IntentSender) {
                            try {
                                activeLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            } catch (e: Exception) {
                                Log.w(TAG, "associate: launch failed: ${e.message}")
                                resolvePending(false)
                            }
                        }

                        override fun onFailure(error: CharSequence?) {
                            // Most often the watch simply was not seen within
                            // the OS's scan window. Not fatal: onboarding
                            // continues unassociated.
                            Log.w(TAG, "associate: failed: $error")
                            resolvePending(false)
                        }
                    },
                    null,
                )
            } catch (e: Exception) {
                // `associate` is a binder call that throws SYNCHRONOUSLY when
                // the platform refuses the request outright — e.g.
                // IllegalStateException "Must declare uses-feature
                // android.software.companion_device_setup" if that declaration
                // is ever dropped from the manifest. Without this catch the
                // throw escapes instead of the quiet `false` this API
                // promises, and the pending continuation is never resolved.
                Log.w(TAG, "associate: request refused: ${e.message}")
                resolvePending(false)
            }
        }
    }

    fun isAssociated(address: String): Boolean {
        val manager = manager() ?: return false
        return try {
            @Suppress("DEPRECATION")
            manager.associations.any { it.equals(address, ignoreCase = true) }
        } catch (e: Exception) {
            Log.w(TAG, "isAssociated: ${e.message}")
            false
        }
    }

    fun disassociate(address: String) {
        val manager = manager() ?: return
        try {
            stopObservingPresence(address)
            @Suppress("DEPRECATION")
            manager.disassociate(address)
            Log.i(TAG, "disassociated")
        } catch (e: Exception) {
            // Nothing associated, or already gone. Forgetting a device must
            // not fail because the OS had nothing to forget.
            Log.i(TAG, "disassociate: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Presence observation (API 31+) — what wakes OpenVitalsCompanionDeviceService.
    // -------------------------------------------------------------------------

    private fun startObservingPresence(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = manager() ?: return
        try {
            @Suppress("DEPRECATION")
            manager.startObservingDevicePresence(address)
        } catch (e: Exception) {
            Log.w(TAG, "startObservingDevicePresence: ${e.message}")
        }
    }

    private fun stopObservingPresence(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = manager() ?: return
        try {
            @Suppress("DEPRECATION")
            manager.stopObservingDevicePresence(address)
        } catch (e: Exception) {
            Log.i(TAG, "stopObservingDevicePresence: ${e.message}")
        }
    }

    private fun resolvePending(allowed: Boolean) {
        val continuation: CancellableContinuation<Boolean>?
        synchronized(this) {
            continuation = pendingContinuation
            pendingContinuation = null
            pendingAddress = null
        }
        if (continuation?.isActive == true) continuation.resume(allowed)
    }

    internal companion object {
        const val TAG = "OpenVitalsCompanion"
    }
}
