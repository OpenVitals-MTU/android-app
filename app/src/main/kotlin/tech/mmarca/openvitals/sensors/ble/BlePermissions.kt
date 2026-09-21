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

/**
 * What a recording start still has to ask for. Notifications are required.
 * Bluetooth is asked for only when a sensor is saved, and a refusal does not
 * stop the recording: the sensor stays disconnected.
 */
// The names are inlined by the compiler, and sdkInt guards each one. Lint cannot see a guard on a parameter.
@SuppressLint("InlinedApi")
fun recordingRuntimePermissionsToRequest(
    sdkInt: Int,
    hasNotificationPermission: Boolean,
    hasBluetoothConnectPermission: Boolean,
    hasSavedBleSensors: Boolean,
): List<String> = buildList {
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (sdkInt >= Build.VERSION_CODES.S && hasSavedBleSensors && !hasBluetoothConnectPermission) {
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
}
