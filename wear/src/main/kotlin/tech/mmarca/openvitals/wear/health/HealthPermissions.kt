package tech.mmarca.openvitals.wear.health

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * The runtime permission that unlocks heart rate and single beats. Wear OS 6
 * (API 36) split BODY_SENSORS into per-type health permissions.
 */
val HeartRatePermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        "android.permission.health.READ_HEART_RATE"
    } else {
        Manifest.permission.BODY_SENSORS
    }

fun Context.hasHeartRatePermission(): Boolean =
    checkSelfPermission(HeartRatePermission) == PackageManager.PERMISSION_GRANTED
