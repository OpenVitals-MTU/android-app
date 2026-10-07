package tech.mmarca.openvitals.devices.core.pairing

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.devices.core.ServiceDataFilter

/** What the companion dialog said the user picked: the address, and the name it advertised. */
data class CompanionDevice(val address: String, val name: String?)

/**
 * CompanionDeviceManager association. For a watch it lets the OS raise
 * this app's priority while the watch is in range, which a long sync needs;
 * for a scale it is how the OS wakes the app when someone steps on. Optional
 * for a watch: the user can decline, and presence needs API 31+, so every
 * method returns "no association" rather than throwing. The device address
 * is never logged. Association needs an attached Activity.
 */
@Singleton
class CompanionDevicePairing @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {
    /** Launcher + pending continuation for the association dialog's result. */
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var attachedActivityId: Int? = null
    /** Completed with the dialog's result Intent on consent, null on anything else. */
    private var pendingOutcome: CompletableDeferred<Intent?>? = null

    /** The address being associated, so a success can start presence observation. */
    private var pendingAddress: String? = null

    private fun manager(): CompanionDeviceManager? =
        applicationContext.getSystemService(CompanionDeviceManager::class.java)

    // Activity lifecycle.

    /** MainActivity attaches in onCreate. Without it [associate] can never show its dialog. */
    fun attachToActivity(activity: Activity) {
        val componentActivity = activity as? ComponentActivity
        if (componentActivity == null) {
            Log.w(TAG, "activity is not a ComponentActivity; companion association unavailable")
            return
        }
        detachFromActivity()
        attachedActivityId = System.identityHashCode(activity)
        launcher =
            componentActivity.activityResultRegistry.register(
                "tech.mmarca.openvitals.devices.core.pairing.companion",
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result: ActivityResult ->
                val address = pendingAddress
                val allowed = result.resultCode == Activity.RESULT_OK
                if (allowed && address != null) startObservingPresence(address)
                Log.i(TAG, "companion association allowed=$allowed")
                // A consent with no data still counts: the address was known beforehand.
                resolvePending(if (allowed) result.data ?: Intent() else null)
            }
    }

    /** Detaches only the Activity that is attached, so a late onDestroy cannot unhook a newer one. */
    fun detachFromActivity(activity: Activity) {
        if (attachedActivityId == System.identityHashCode(activity)) detachFromActivity()
    }

    fun detachFromActivity() {
        attachedActivityId = null
        launcher?.unregister()
        launcher = null
        // A dialog in flight when the Activity goes away can never report back.
        resolvePending(null)
    }

    // API.

    /**
     * Asks the OS to associate [address], showing the system dialog. True when
     * allowed; false on decline and on every degraded path.
     */
    suspend fun associate(
        address: String,
        @Suppress("UNUSED_PARAMETER") displayName: String? = null,
        filter: CompanionFilter = CompanionFilter.BLE,
    ): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.w(TAG, "associate: invalid address")
            return false
        }
        val manager = manager()
        if (manager == null) {
            Log.i(TAG, "associate: CompanionDeviceManager unavailable")
            return false
        }
        // Already associated: the OS never calls back for a repeat request.
        if (isAssociated(address)) {
            Log.i(TAG, "associate: already associated")
            startObservingPresence(address)
            return true
        }
        // A Garmin watch is reached over BLE, so filter by scan, not classic MAC.
        // A Wear OS watch may be registered under its Classic bond address,
        // which no BLE scan sees: without the Classic filter the dialog searches forever.
        val builder =
            AssociationRequest.Builder()
                .addDeviceFilter(
                    BluetoothLeDeviceFilter.Builder()
                        .setScanFilter(ScanFilter.Builder().setDeviceAddress(address).build())
                        .build(),
                )
                .setSingleDevice(true)
        if (filter == CompanionFilter.BLE_OR_CLASSIC) {
            builder.addDeviceFilter(BluetoothDeviceFilter.Builder().setAddress(address).build())
        }
        pendingAddress = address
        return request(builder.build()) != null
    }

    /**
     * Shows the system dialog listing every nearby device whose service data
     * under [serviceUuid] passes one of [filters], and returns the one the
     * user picked, now associated and watched for presence. Null when the
     * user declines, when nothing is found, and on every degraded path. The
     * device has to be advertising while the dialog looks.
     */
    suspend fun discover(serviceUuid: String, filters: List<ServiceDataFilter>): CompanionDevice? {
        if (manager() == null || launcher == null) {
            Log.w(TAG, "discover: companion association unavailable")
            return null
        }
        val uuid = ParcelUuid.fromString(serviceUuid)
        val builder = AssociationRequest.Builder().setSingleDevice(false)
        for (filter in filters) {
            builder.addDeviceFilter(
                BluetoothLeDeviceFilter.Builder()
                    .setScanFilter(ScanFilter.Builder().setServiceData(uuid, filter.data, filter.mask).build())
                    .build(),
            )
        }
        pendingAddress = null
        val result = request(builder.build()) ?: return null
        val device = result.chosenDevice()
        if (device == null) {
            Log.w(TAG, "discover: the result names no device")
            return null
        }
        startObservingPresence(device.address)
        return device
    }

    /**
     * Runs one association request through the system dialog. Null unless
     * the user consented. The wait for the dialog is bounded: Android 12
     * never calls back when its scan finds nothing. The dialog itself is not,
     * since the user is in it.
     */
    private suspend fun request(request: AssociationRequest): Intent? {
        val manager = manager() ?: return null
        val activeLauncher = launcher
        if (activeLauncher == null) {
            Log.w(TAG, "associate: no activity attached")
            return null
        }
        if (pendingOutcome != null) {
            Log.w(TAG, "associate: a request is already in flight")
            return null
        }

        val found = CompletableDeferred<Boolean>()
        val outcome = CompletableDeferred<Intent?>()
        pendingOutcome = outcome
        Log.i(TAG, "associate: requesting association")
        try {
            manager.associate(
                request,
                object : CompanionDeviceManager.Callback() {
                    override fun onDeviceFound(intentSender: IntentSender) {
                        // A find after the caller gave up must not pop the dialog for nobody.
                        if (pendingOutcome !== outcome) return
                        found.complete(true)
                        try {
                            activeLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        } catch (e: Exception) {
                            Log.w(TAG, "associate: launch failed: ${e.message}")
                            resolvePending(null)
                        }
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Usually the device was not seen in the scan window. Not fatal.
                        Log.w(TAG, "associate: failed: $error")
                        found.complete(false)
                        resolvePending(null)
                    }
                },
                null,
            )
        } catch (e: Exception) {
            // `associate` throws synchronously when the platform refuses outright.
            Log.w(TAG, "associate: request refused: ${e.message}")
            resolvePending(null)
            return null
        }

        try {
            val wasFound = withTimeoutOrNull(FIND_TIMEOUT_MILLIS) { found.await() }
            if (wasFound != true) {
                resolvePending(null)
                return null
            }
            return outcome.await()
        } catch (error: CancellationException) {
            resolvePending(null)
            throw error
        }
    }

    /** The device the dialog's result names. The scan result first; it says the advertised name too. */
    private fun Intent.chosenDevice(): CompanionDevice? {
        @Suppress("DEPRECATION")
        val scanResult = IntentCompat.getParcelableExtra(this, CompanionDeviceManager.EXTRA_DEVICE, ScanResult::class.java)
        if (scanResult != null) {
            return CompanionDevice(
                address = scanResult.device.address.uppercase(),
                name = scanResult.scanRecord?.deviceName?.takeIf { it.isNotBlank() },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val association = IntentCompat.getParcelableExtra(
                this,
                CompanionDeviceManager.EXTRA_ASSOCIATION,
                AssociationInfo::class.java,
            )
            val address = association?.deviceMacAddress?.toString() ?: return null
            return CompanionDevice(address = address.uppercase(), name = association.displayName?.toString())
        }
        return null
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
            // Nothing to forget is not a failure.
            Log.i(TAG, "disassociate: ${e.message}")
        }
    }

    // Presence observation (API 31+): what wakes OpenVitalsCompanionDeviceService.

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

    private fun resolvePending(result: Intent?) {
        val outcome: CompletableDeferred<Intent?>?
        synchronized(this) {
            outcome = pendingOutcome
            pendingOutcome = null
            pendingAddress = null
        }
        outcome?.complete(result)
    }

    internal companion object {
        const val TAG = "OpenVitalsCompanion"

        /** Android's own scan gives up after 20 s; Android 12 then says nothing at all. */
        private const val FIND_TIMEOUT_MILLIS = 30_000L
    }
}
