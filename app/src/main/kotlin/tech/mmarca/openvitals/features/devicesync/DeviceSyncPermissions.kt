package tech.mmarca.openvitals.features.devicesync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime Bluetooth permission gate for phone-to-phone sync.
 *
 * Sync needs SCAN + CONNECT (like the BLE sensor stack) plus ADVERTISE for the
 * host's discoverable request (API 31+). Pre-31 the runtime permissions are
 * the location ones: classic discovery requires fine location there.
 */
object DeviceSyncPermissions {

    /** The runtime permissions the wizard must hold before scanning/hosting. */
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun allGranted(context: Context): Boolean = required().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
