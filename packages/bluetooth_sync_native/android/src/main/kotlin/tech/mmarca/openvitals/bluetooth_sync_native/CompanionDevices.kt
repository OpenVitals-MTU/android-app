package tech.mmarca.openvitals.bluetooth_sync_native

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

/**
 * CompanionDeviceManager association, used when onboarding a Garmin watch.
 *
 * Ported from Gadgetbridge's `util/BondingUtil.java` (`companionDeviceManagerBond`,
 * `Disassociate`, `StartObserving`) — AGPLv3, the same licence as this app.
 *
 * WHY THIS IS NATIVE: `flutter_blue_plus` covers scanning and `createBond`, but
 * CompanionDeviceManager has no Flutter plugin. The association is what lets the
 * OS raise this app's process priority while the watch is in range (see
 * [CompanionDeviceService]), which a long BLE file sync needs.
 *
 * WHY EVERYTHING DEGRADES QUIETLY: association is optional. The user can decline
 * the system dialog, and presence observation needs API 31+ while the app's
 * minSdk is 26. Neither is an error — a watch that is bonded but not associated
 * still syncs, just without the priority boost. So every method here returns
 * "no association" rather than throwing.
 *
 * WHAT IS NOT LOGGED: the device address. `Log` is not stripped from a release
 * build, and a Bluetooth MAC is a stable identifier for the person carrying it.
 * These lines say what happened, not to whom.
 *
 * Association is scoped to an Activity: the OS hands back an [IntentSender] that
 * must be launched for result. With no Activity attached (a background isolate),
 * there is no dialog to show and the call resolves false.
 */
internal class CompanionDevices(
    private val applicationContext: Context?,
) {
    /** Launcher + pending callback for the association dialog's result. */
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var pendingCallback: ((Result<Boolean>) -> Unit)? = null

    /** The address being associated, so a success can start presence observation. */
    private var pendingAddress: String? = null

    /** Null only when the plugin is detached; CDM itself is API 26 = the minSdk. */
    private fun manager(): CompanionDeviceManager? =
        applicationContext?.getSystemService(CompanionDeviceManager::class.java)

    // -------------------------------------------------------------------------
    // Activity lifecycle — mirrors the discoverable launcher in the plugin.
    // -------------------------------------------------------------------------

    fun attachToActivity(activity: Activity) {
        val componentActivity = activity as? ComponentActivity
        if (componentActivity == null) {
            Log.w(SyncBluetooth.TAG, "activity is not a ComponentActivity; companion association unavailable")
            return
        }
        detachFromActivity()
        launcher =
            componentActivity.activityResultRegistry.register(
                "tech.mmarca.openvitals.bluetooth_sync_native.companion",
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result: ActivityResult ->
                val callback = pendingCallback
                val address = pendingAddress
                pendingCallback = null
                pendingAddress = null
                val allowed = result.resultCode == Activity.RESULT_OK
                if (allowed && address != null) startObservingPresence(address)
                Log.i(SyncBluetooth.TAG, "companion association allowed=$allowed")
                callback?.invoke(Result.success(allowed))
            }
    }

    fun detachFromActivity() {
        launcher?.unregister()
        launcher = null
        // A dialog in flight when the Activity goes away can never report back.
        // Resolve it as declined rather than leaking the Dart future forever.
        pendingCallback?.invoke(Result.success(false))
        pendingCallback = null
        pendingAddress = null
    }

    // -------------------------------------------------------------------------
    // Host API
    // -------------------------------------------------------------------------

    fun associate(address: String, displayName: String?, callback: (Result<Boolean>) -> Unit) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.w(SyncBluetooth.TAG, "associate: invalid address")
            callback(Result.success(false))
            return
        }
        val manager = manager()
        if (manager == null) {
            Log.i(SyncBluetooth.TAG, "associate: CompanionDeviceManager unavailable")
            callback(Result.success(false))
            return
        }
        // Already associated: the OS never invokes the callback for a repeat
        // request, so short-circuiting is what keeps re-onboarding from hanging
        // (Gadgetbridge hits the same trap, BondingUtil.java:377).
        if (isAssociated(address)) {
            Log.i(SyncBluetooth.TAG, "associate: already associated")
            startObservingPresence(address)
            callback(Result.success(true))
            return
        }
        val activeLauncher = launcher
        if (activeLauncher == null) {
            Log.w(SyncBluetooth.TAG, "associate: no activity attached")
            callback(Result.success(false))
            return
        }
        if (pendingCallback != null) {
            Log.w(SyncBluetooth.TAG, "associate: a request is already in flight")
            callback(Result.success(false))
            return
        }

        // A Garmin watch is reached over BLE, so it is filtered by scan filter
        // rather than by classic MAC — the classic filter would never match and
        // the dialog would sit on "searching" forever.
        val request =
            AssociationRequest.Builder()
                .addDeviceFilter(
                    BluetoothLeDeviceFilter.Builder()
                        .setScanFilter(ScanFilter.Builder().setDeviceAddress(address).build())
                        .build(),
                )
                .setSingleDevice(true)
                .build()

        pendingCallback = callback
        pendingAddress = address
        Log.i(SyncBluetooth.TAG, "associate: requesting association")
        try {
            manager.associate(
                request,
                object : CompanionDeviceManager.Callback() {
                    override fun onDeviceFound(intentSender: IntentSender) {
                        try {
                            activeLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        } catch (e: Exception) {
                            Log.w(SyncBluetooth.TAG, "associate: launch failed: ${e.message}")
                            resolvePending(false)
                        }
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Most often the watch simply was not seen within the OS's
                        // scan window. Not fatal: onboarding continues unassociated.
                        Log.w(SyncBluetooth.TAG, "associate: failed: $error")
                        resolvePending(false)
                    }
                },
                null,
            )
        } catch (e: Exception) {
            // `associate` is a binder call that throws SYNCHRONOUSLY when the
            // platform refuses the request outright -- e.g. IllegalStateException
            // "Must declare uses-feature android.software.companion_device_setup"
            // if that declaration is ever dropped from the host manifest. Without
            // this catch the throw crosses the Pigeon boundary as a channel error
            // instead of the quiet `false` this API promises, and the pending
            // callback is never resolved.
            Log.w(SyncBluetooth.TAG, "associate: request refused: ${e.message}")
            resolvePending(false)
        }
    }

    fun isAssociated(address: String): Boolean {
        val manager = manager() ?: return false
        return try {
            @Suppress("DEPRECATION")
            manager.associations.any { it.equals(address, ignoreCase = true) }
        } catch (e: Exception) {
            Log.w(SyncBluetooth.TAG, "isAssociated: ${e.message}")
            false
        }
    }

    fun disassociate(address: String) {
        val manager = manager() ?: return
        try {
            stopObservingPresence(address)
            @Suppress("DEPRECATION")
            manager.disassociate(address)
            Log.i(SyncBluetooth.TAG, "disassociated")
        } catch (e: Exception) {
            // Nothing associated, or already gone. Forgetting a device must not
            // fail because the OS had nothing to forget.
            Log.i(SyncBluetooth.TAG, "disassociate: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Presence observation (API 31+) — what wakes CompanionDeviceService.
    // -------------------------------------------------------------------------

    private fun startObservingPresence(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = manager() ?: return
        try {
            @Suppress("DEPRECATION")
            manager.startObservingDevicePresence(address)
        } catch (e: Exception) {
            Log.w(SyncBluetooth.TAG, "startObservingDevicePresence: ${e.message}")
        }
    }

    private fun stopObservingPresence(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val manager = manager() ?: return
        try {
            @Suppress("DEPRECATION")
            manager.stopObservingDevicePresence(address)
        } catch (e: Exception) {
            Log.i(SyncBluetooth.TAG, "stopObservingDevicePresence: ${e.message}")
        }
    }

    private fun resolvePending(allowed: Boolean) {
        val callback = pendingCallback
        pendingCallback = null
        pendingAddress = null
        callback?.invoke(Result.success(allowed))
    }
}
