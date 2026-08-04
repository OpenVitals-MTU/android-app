package tech.mmarca.openvitals.features.watches

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.WatchNotificationPrefsStore
import tech.mmarca.openvitals.devices.garmin.GarminLog
import tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge
import tech.mmarca.openvitals.devices.notifications.NotificationFilter
import tech.mmarca.openvitals.devices.notifications.NotificationStore

/** One app the user can silence. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * The Android-touching edge of the watch-notifications feature, behind an
 * interface so [WatchNotificationAppsViewModel]'s gate logic — the part that
 * decides when the disclosure shows and when the system screen opens — is
 * testable on the JVM.
 *
 * In the Flutter build these were the Pigeon host-API calls
 * (`isNotificationAccessGranted`, `openNotificationAccessSettings`,
 * `listLaunchableApps`, `setForwardingConfig`); here they are ordinary
 * platform calls.
 */
interface WatchNotificationsGateway {

    /**
     * Whether Android has granted notification access. There is no runtime
     * prompt for it — the only way to grant it is the system settings screen,
     * so this is polled rather than awaited.
     */
    fun isNotificationAccessGranted(): Boolean

    /** Opens Android's own notification-access settings screen. */
    fun openNotificationAccessSettings()

    /**
     * Every app with a launcher entry, resolved through the `<queries>`
     * MAIN/LAUNCHER declaration — never QUERY_ALL_PACKAGES, which is a
     * Play-restricted permission whose mere presence blocks upload.
     */
    fun listLaunchableApps(): List<InstalledApp>

    /**
     * Mirrors the configuration to the listener's filter.
     *
     * The ONE place this happens, because it is the one place the two copies
     * can drift — and if they do, the filter drops everything before the
     * forwarder is ever poked and the watch stays silent with no error
     * anywhere.
     */
    fun pushConfiguration()
}

/** The real gateway. */
class AndroidWatchNotificationsGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: WatchNotificationPrefsStore,
    private val deviceRepository: BleDeviceRepository,
    private val bridge: GarminNotificationBridge,
) : WatchNotificationsGateway {

    override fun isNotificationAccessGranted(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    override fun openNotificationAccessSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-NOTIFY] could not open notification settings: $error")
        }
    }

    override fun listLaunchableApps(): List<InstalledApp> {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(intent, 0)
            .mapNotNull { resolved ->
                val info = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                InstalledApp(
                    packageName = info.packageName,
                    label = manager.getApplicationLabel(info).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override fun pushConfiguration() {
        // No watch means nowhere to send anything, and the filter treats that
        // as "capture nothing" rather than buffering for a watch that may
        // never be paired.
        val watch = deviceRepository.devices.firstOrNull { it.isGarminGfdi }
        NotificationStore.writeConfig(
            context,
            NotificationFilter.Config(
                enabled = store.enabled,
                blockedPackages = store.blockedPackages,
                watchAddress = watch?.address,
            ),
        )
        bridge.onConfigChanged()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface WatchNotificationsModule {
    @Binds
    @Singleton
    fun bindWatchNotificationsGateway(
        implementation: AndroidWatchNotificationsGateway,
    ): WatchNotificationsGateway
}
