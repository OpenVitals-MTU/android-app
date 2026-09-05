package tech.mmarca.openvitals.core.permissions

import android.Manifest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.domain.model.OsPermissionRow

/**
 * Builds the OS-permission catalog for this device, filtered by what the
 * OS version has, so no row is ungrantable.
 */
@Singleton
class OsPermissionsService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** What a watch needs, asked when one is added. Recording permissions are asked elsewhere. */
    fun watchSetupCatalog(): OsPermissionCatalog = OsPermissionCatalog(
        rows = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    row(
                        OsPermissionId.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    ),
                )
            } else {
                // Below API 31 a BLE scan is gated on location.
                add(row(OsPermissionId.LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(row(OsPermissionId.NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS))
            }
            // A companion association already runs in the background; a doze exemption buys nothing.
            if (!hasCompanionAssociation()) {
                add(special(OsPermissionId.BATTERY_OPTIMIZATION, isIgnoringBatteryOptimizations()))
            }
            add(special(OsPermissionId.NOTIFICATION_FORWARDING, isNotificationAccessGranted()))
        },
    )

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Opens the settings screen for a special row. False when the device has none. */
    fun openSettingsFor(id: OsPermissionId): Boolean {
        val intent = when (id) {
            // The direct dialog needs a store-restricted permission; the list costs one tap.
            OsPermissionId.BATTERY_OPTIMIZATION ->
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

            OsPermissionId.NOTIFICATION_FORWARDING ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

            else -> return false
        }
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    private fun row(id: OsPermissionId, vararg permissions: String) = OsPermissionRow(
        id = id,
        permissions = permissions.toList(),
        granted = permissions.all(::isGranted),
    )

    private fun special(id: OsPermissionId, granted: Boolean) =
        OsPermissionRow(id = id, permissions = emptyList(), granted = granted)

    private fun isNotificationAccessGranted(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Not every device ships companion-device support, hence the guard. */
    @Suppress("DEPRECATION")
    private fun hasCompanionAssociation(): Boolean = runCatching {
        val manager = context.getSystemService(CompanionDeviceManager::class.java)
            ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.myAssociations.isNotEmpty()
        } else {
            manager.associations.isNotEmpty()
        }
    }.getOrDefault(false)
}
