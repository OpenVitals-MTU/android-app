package tech.mmarca.openvitals.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider

/**
 * Removes what the device layer left on disk when it moved to OpenWatch: the
 * watch entries in the sensor registry and the preference files of the Garmin,
 * Wear OS and scale stores. Health Connect is not touched; the records the
 * devices wrote stay. Idempotent: a missing file is a no-op.
 */
@Singleton
class LegacyDeviceDataCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bleDeviceRepository: BleDeviceRepository,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun clean() = withContext(dispatchers.io) {
        bleDeviceRepository.removeWatchEntries()
        LegacyPrefsFiles.forEach { file -> context.deleteSharedPreferences(file) }
    }

    companion object {
        /** The preference files of the removed stores, by the names they used. */
        val LegacyPrefsFiles: List<String> = listOf(
            "xiaomi_scale", // XiaomiScaleStore: the scale's address and bind key
            "wearos_link", // WearOsLinkStore: the watch trust tokens
            "wearos_sync_cursors", // WearOsSyncCursorStore
            "garmin_device_state", // GarminDeviceStateStore
            "garmin_agps", // GarminAgpsStore
            "garmin_counter_watermarks", // GarminCounterWatermarkStore
            "garmin_watch_notifications", // WatchNotificationPrefsStore
            "openvitals_notification_listener", // NotificationStore
            "weather_store", // WeatherStore
        )
    }
}
