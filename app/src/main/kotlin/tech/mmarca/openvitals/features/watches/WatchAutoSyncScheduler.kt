package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval
import tech.mmarca.openvitals.devices.garmin.GarminDeviceStateStore
import tech.mmarca.openvitals.devices.garmin.GarminLog

/**
 * Owns the automatic watch sync schedule: the stored per-watch interval and
 * the `WorkManager` periodic work. One unique work per watch, named by
 * device id, so a new interval replaces the schedule. [restoreAll] covers
 * what WorkManager does not, and keeps any live schedule.
 */
@Singleton
class WatchAutoSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun interval(deviceId: String): AutoSyncInterval = stateStore.autoSyncInterval(deviceId)

    /** Stores the choice and re-plans from now, so a shorter interval runs sooner. */
    fun setInterval(deviceId: String, interval: AutoSyncInterval) {
        stateStore.setAutoSyncInterval(deviceId, interval)
        if (interval.isOn) {
            enqueue(deviceId, interval, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
        } else {
            cancel(deviceId)
        }
    }

    /** Re-plans every paired watch from what is stored. Idempotent; KEEP leaves live schedules alone. */
    fun restoreAll() {
        // Runs from Application.onCreate. Not worth failing app start over.
        runCatching {
            deviceRepository.devices.forEach { device ->
                val interval = stateStore.autoSyncInterval(device.id)
                if (interval.isOn && device.isGarminGfdi) {
                    enqueue(device.id, interval, ExistingPeriodicWorkPolicy.KEEP)
                } else {
                    // A watch that stopped being syncable, or a schedule that outlived its preference.
                    cancel(device.id)
                }
            }
        }.onFailure { GarminLog.log("[GARMIN-AUTOSYNC] could not restore the schedules: $it") }
    }

    /** Stops a watch's schedule without touching the stored interval. */
    fun cancel(deviceId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(deviceId))
    }

    /** Removal: the schedule goes with the watch, and so does the choice. */
    fun forget(deviceId: String) {
        cancel(deviceId)
        stateStore.setAutoSyncInterval(deviceId, AutoSyncInterval.OFF)
    }

    private fun enqueue(
        deviceId: String,
        interval: AutoSyncInterval,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        val request = PeriodicWorkRequestBuilder<WatchAutoSyncWorker>(
            interval.minutes.toLong(),
            TimeUnit.MINUTES,
        )
            .setInputData(WatchAutoSyncWorker.inputData(deviceId))
            // Battery only: the sync is Bluetooth, and the app has no internet permission.
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(uniqueWorkName(deviceId), policy, request)
    }

    companion object {
        /** A watch out of range is usually back within minutes. */
        private const val RETRY_BACKOFF_MINUTES = 10L

        fun uniqueWorkName(deviceId: String): String = "watch-auto-sync-$deviceId"
    }
}
