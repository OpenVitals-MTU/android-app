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
 * The Android-touching edge of watch notifications, behind an interface so
 * the view-model's gate logic is JVM-testable.
 */
interface WatchNotificationsGateway {

    /** Whether Android granted notification access. Only the settings screen can, so it is polled. */
    fun isNotificationAccessGranted(): Boolean

    /** Opens Android's own notification-access settings screen. */
    fun openNotificationAccessSettings()

    /** Every launchable app, via the manifest `<queries>`, never QUERY_ALL_PACKAGES. */
    fun listLaunchableApps(): List<InstalledApp>

    /** Mirrors the configuration to the listener's filter. The one place the two copies can drift. */
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
        // No watch means capture nothing.
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
