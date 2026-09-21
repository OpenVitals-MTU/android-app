package tech.mmarca.openvitals.sensors.ble

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Android 12 put connecting behind the Nearby devices grant. Before that the manifest was enough. */
fun hasBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

/** Android 12 gave scanning its own grant. Before that a scan finds nothing without precise location. */
fun hasBluetoothScanPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Manifest.permission.BLUETOOTH_SCAN
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * What a recording start still has to ask for. Notifications are required.
 * Bluetooth is asked for only when a sensor is saved, and a refusal does not
 * stop the recording: the sensor stays disconnected. Scan and connect share
 * one system dialog. The scan finds a sensor the phone forgot on a reboot.
 */
// The names are inlined by the compiler, and sdkInt guards each one. Lint cannot see a guard on a parameter.
@SuppressLint("InlinedApi")
fun recordingRuntimePermissionsToRequest(
    sdkInt: Int,
    hasNotificationPermission: Boolean,
    hasBluetoothConnectPermission: Boolean,
    hasBluetoothScanPermission: Boolean,
    hasSavedBleSensors: Boolean,
): List<String> = buildList {
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (sdkInt >= Build.VERSION_CODES.S && hasSavedBleSensors) {
        if (!hasBluetoothConnectPermission) add(Manifest.permission.BLUETOOTH_CONNECT)
        if (!hasBluetoothScanPermission) add(Manifest.permission.BLUETOOTH_SCAN)
    }
}
