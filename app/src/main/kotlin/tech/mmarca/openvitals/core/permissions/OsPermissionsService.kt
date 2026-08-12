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
 * Builds the OS-permission catalog for THIS device and opens the settings
 * screens the runtime dialog cannot cover.
 *
 * Every row is filtered by what the OS version actually has, so the list never
 * shows a permission that cannot be granted here — an ungrantable row would
 * leave it permanently incomplete with no way to finish it.
 */
@Singleton
class OsPermissionsService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * What pairing and running a watch needs, asked at the point the user adds
     * one rather than during first-run onboarding: at that moment the reason
     * for each is on screen, and a user who never pairs a watch is never asked.
     *
     * Scoped to watches on purpose. Physical activity and (on modern Android)
     * location belong to workout recording, and are asked for there.
     */
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
                // Below API 31 there is no runtime Bluetooth permission at
                // all: a BLE scan is gated on location instead.
                add(row(OsPermissionId.LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(row(OsPermissionId.NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS))
            }
            // A companion association already carries the right to run in the
            // background, so asking for a doze exemption on top buys nothing
            // and is exactly the kind of request app stores push back on.
            if (!hasCompanionAssociation()) {
                add(special(OsPermissionId.BATTERY_OPTIMIZATION, isIgnoringBatteryOptimizations()))
            }
            add(special(OsPermissionId.NOTIFICATION_FORWARDING, isNotificationAccessGranted()))
        },
    )

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Opens the settings screen for a special row. False when the device has
     * no such screen, so the caller can say so instead of failing silently.
     */
    fun openSettingsFor(id: OsPermissionId): Boolean {
        val intent = when (id) {
            // ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS would put the direct
            // dialog one tap away, but it needs a store-restricted permission.
            // The settings list costs one extra tap and no declaration.
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
