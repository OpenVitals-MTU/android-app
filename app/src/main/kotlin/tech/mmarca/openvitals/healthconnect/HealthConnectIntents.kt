package tech.mmarca.openvitals.healthconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build

private const val ACTION_MANAGE_HEALTH_PERMISSIONS =
    "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"
private const val ACTION_PLATFORM_HEALTH_CONNECT_SETTINGS =
    "android.health.connect.action.HEALTH_HOME_SETTINGS"
private const val ACTION_APK_HEALTH_CONNECT_SETTINGS =
    "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
private const val ACTION_PLATFORM_MANAGE_HEALTH_DATA =
    "android.health.connect.action.MANAGE_HEALTH_DATA"
private const val ACTION_APK_MANAGE_HEALTH_DATA =
    "androidx.health.ACTION_MANAGE_HEALTH_DATA"
private const val HC_PACKAGE = "com.google.android.apps.healthdata"

fun openHealthConnectPermissionSettings(context: Context): Boolean {
    return healthConnectPermissionSettingsIntents(context).any { intent ->
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.isSuccess
    }
}

private fun healthConnectPermissionSettingsIntents(context: Context): List<Intent> {
    val appPermissionsIntent = Intent(ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
    }
    val settingsIntent = Intent(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ACTION_PLATFORM_HEALTH_CONNECT_SETTINGS
        } else {
            ACTION_APK_HEALTH_CONNECT_SETTINGS
        }
    )
    val manageDataIntent = Intent(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ACTION_PLATFORM_MANAGE_HEALTH_DATA
        } else {
            ACTION_APK_MANAGE_HEALTH_DATA
        }
    )
    val launchIntent = context.packageManager.getLaunchIntentForPackage(HC_PACKAGE)

    return listOfNotNull(
        appPermissionsIntent,
        settingsIntent,
        manageDataIntent,
        launchIntent,
    ).map { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
}
