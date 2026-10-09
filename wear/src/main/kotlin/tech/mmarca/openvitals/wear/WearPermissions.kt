package tech.mmarca.openvitals.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Which permission gates the heart rate sensor on this watch. Android 16
 * replaced `BODY_SENSORS` with the Health Connect `READ_HEART_RATE`
 * permission: the sensor service refuses an app targeting 36 that holds only
 * the old one. Older releases know only the old one.
 */
object WearPermissions {

    private const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
    private const val READ_HEALTH_DATA_IN_BACKGROUND =
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    /** The runtime permission the heart rate sensor needs on this release. */
    val heartRate: String
        get() = if (Build.VERSION.SDK_INT >= 36) READ_HEART_RATE else Manifest.permission.BODY_SENSORS

    /**
     * Keeps the sensor readable once the app leaves the screen. Asked only
     * after [heartRate] is granted, as Android requires. Null before API 33,
     * where the foreground grant already covers the background.
     */
    val heartRateInBackground: String?
        get() = when {
            Build.VERSION.SDK_INT >= 36 -> READ_HEALTH_DATA_IN_BACKGROUND
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Manifest.permission.BODY_SENSORS_BACKGROUND
            else -> null
        }

    /** The link needs BLUETOOTH_CONNECT from API 31; before that it is install-time. */
    fun hasBluetooth(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            isGranted(context, Manifest.permission.BLUETOOTH_CONNECT)

    fun hasHeartRate(context: Context): Boolean = isGranted(context, heartRate)

    fun hasHeartRateInBackground(context: Context): Boolean =
        heartRateInBackground?.let { isGranted(context, it) } ?: true

    private fun isGranted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
