package tech.mmarca.openvitals.devices.core.pairing

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * [WatchPairingPort] over the two platform layers it actually takes: bonding
 * is `BluetoothDevice.createBond` plus the `ACTION_BOND_STATE_CHANGED`
 * broadcast, association is [CompanionDevicePairing]. The domain sees one
 * port.
 *
 * Port of the Flutter build's `ble_watch_pairing.dart`, with the
 * flutter_blue_plus bonding plumbing replaced by the platform APIs it wrapped.
 * Nothing here logs the device address — a Bluetooth MAC is a stable
 * identifier for the person carrying it.
 */
@Singleton
class BleWatchPairing @Inject constructor(
    @ApplicationContext private val context: Context,
    private val companionPairing: CompanionDevicePairing,
) : WatchPairingPort {

    @SuppressLint("MissingPermission")
    override suspend fun bond(address: String): WatchBondResult {
        val adapter = adapter() ?: return WatchBondResult.UNREACHABLE
        if (!adapter.isEnabled) return WatchBondResult.UNREACHABLE
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            ?: return WatchBondResult.UNREACHABLE

        // Checked BEFORE prompting: a bonded watch needs no dialog.
        if (runCatching { device.bondState }.getOrNull() == BluetoothDevice.BOND_BONDED) {
            return WatchBondResult.ALREADY_BONDED
        }

        val settled = CompletableDeferred<WatchBondResult>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val changed: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (changed?.address?.equals(address, ignoreCase = true) != true) return
                when (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)) {
                    BluetoothDevice.BOND_BONDED -> settled.complete(WatchBondResult.BONDED)
                    // NONE after BONDING means the dialog was dismissed, the
                    // code mismatched, or the watch said no. All the same to
                    // the caller: no bond, no onboarding.
                    BluetoothDevice.BOND_NONE -> settled.complete(WatchBondResult.REFUSED)
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
        try {
            val started = runCatching { device.createBond() }.getOrDefault(false)
            if (!started) return WatchBondResult.UNREACHABLE
            // Generous: the user has to find the watch, wake it and confirm a
            // six-digit code on its screen.
            return withTimeoutOrNull(BOND_TIMEOUT) { settled.await() }
                ?: WatchBondResult.REFUSED
        } finally {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun removeBond(address: String) {
        val adapter = adapter() ?: return
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        try {
            // Hidden API, the same one every pairing app (Gadgetbridge
            // included) calls: the platform offers no public unbond.
            device.javaClass.getMethod("removeBond").invoke(device)
        } catch (error: Exception) {
            // Forgetting a watch must not fail because the OS had no bond to
            // drop.
            Log.i(TAG, "removeBond failed: ${error.message}")
        }
    }

    override suspend fun associateCompanion(address: String, displayName: String?): Boolean =
        companionPairing.associate(address, displayName)

    override suspend fun disassociateCompanion(address: String) {
        companionPairing.disassociate(address)
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private companion object {
        const val TAG = "BleWatchPairing"
        val BOND_TIMEOUT = 90.seconds
    }
}
