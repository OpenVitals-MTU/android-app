package tech.mmarca.openvitals.devices.xiaomi

import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.sensors.ble.hasBluetoothScanPermission

/** Whether the phone is listening for the scale, and if not, what stands in the way. */
enum class ScaleListenerStatus {
    /** No scale is set up. */
    OFF,
    LISTENING,
    BLUETOOTH_OFF,

    /** The Bluetooth scan permission is not granted. */
    PERMISSION_MISSING,

    /** No Bluetooth LE, or the system refused the scan. */
    UNAVAILABLE,
}

/**
 * The scan that hears the scale. A port, so everything deciding when to
 * listen is testable without a radio.
 */
interface ScaleScanRadio {

    /**
     * Asks the system to deliver the scale's broadcasts, narrowed to
     * [address] once it is known. [restart] replaces a scan already running.
     * Never throws.
     */
    fun arm(address: String?, restart: Boolean): ScaleListenerStatus

    fun disarm()
}

/**
 * The system scan handed a `PendingIntent`: it outlives the process, and the
 * system starts [ScaleScanReceiver] for every broadcast that passes the
 * filter. Nothing here connects, so no radio lease is taken.
 *
 * The filter is matched in the Bluetooth chip, so a phone full of other
 * `0xFE95` beacons wakes nobody. Low latency is asked for and granted only
 * while the app is on screen: in the background the system scans a tenth of
 * the time or less.
 */
@Singleton
class SystemScaleScanRadio @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ScaleScanRadio {

    private val scanner: BluetoothLeScanner?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?.takeIf { it.isEnabled }
            ?.bluetoothLeScanner

    override fun arm(address: String?, restart: Boolean): ScaleListenerStatus {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return ScaleListenerStatus.UNAVAILABLE
        if (!hasBluetoothScanPermission(context)) return ScaleListenerStatus.PERMISSION_MISSING
        if (!adapter.isEnabled) return ScaleListenerStatus.BLUETOOTH_OFF
        val scanner = adapter.bluetoothLeScanner ?: return ScaleListenerStatus.BLUETOOTH_OFF
        return try {
            // Before Android 10 a second start with the same intent adds a second scan.
            if (restart || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) scanner.stopScan(pendingIntent())
            val error = scanner.startScan(filters(address), Settings, pendingIntent())
            if (error == 0) {
                ScaleListenerStatus.LISTENING
            } else {
                Log.w(TAG, "The scale scan was refused: $error")
                ScaleListenerStatus.UNAVAILABLE
            }
        } catch (error: SecurityException) {
            ScaleListenerStatus.PERMISSION_MISSING
        } catch (error: IllegalStateException) {
            // Bluetooth went off between the check and the call.
            ScaleListenerStatus.BLUETOOTH_OFF
        }
    }

    override fun disarm() {
        try {
            scanner?.stopScan(pendingIntent())
        } catch (error: SecurityException) {
            // The permission is gone, and the system dropped the scan with it.
        } catch (error: IllegalStateException) {
            // Bluetooth is off: there is no scan to stop.
        }
    }

    private fun filters(address: String?): List<ScanFilter> =
        S400Beacon.scanFilters.map { bytes ->
            ScanFilter.Builder()
                .setServiceData(ParcelUuid.fromString(S400Beacon.SERVICE_UUID), bytes.data, bytes.mask)
                .apply { if (address != null) setDeviceAddress(address) }
                .build()
        }

    /** Explicit, so only this app's receiver gets it. Mutable, because the system adds the results. */
    private fun pendingIntent(): PendingIntent {
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ScaleScanReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
    }

    private companion object {
        const val TAG = "ScaleScan"
        const val REQUEST_CODE = 0x5400

        val Settings: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
    }
}
