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
 * the `WorkManager` periodic work that honours it.
 *
 * One unique periodic work per watch rather than one ticker for all of them,
 * because the interval is a per-watch choice and two watches on different
 * schedules must not be forced onto the slower one. The unique name is derived
 * from the device id, so re-choosing an interval replaces the schedule instead
 * of stacking a second one on top of it.
 *
 * `WorkManager` is durable across reboots and app updates on its own, which is
 * why this feature is periodic work rather than an alarm: nothing here has to
 * be re-armed from a boot receiver. [restoreAll] exists for the cases
 * `WorkManager` does not cover — a force-stop, a cleared work database, a
 * restore onto a new phone — and it KEEPs any live schedule so opening the app
 * never pushes the next run further away.
 */
@Singleton
class WatchAutoSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun interval(deviceId: String): AutoSyncInterval = stateStore.autoSyncInterval(deviceId)

    /**
     * Stores the choice and re-plans the watch's schedule to match it.
     *
     * The period restarts from now rather than being amended in place, which
     * is what someone switching from two hours to thirty minutes is asking
     * for: the next run inside half an hour, not at the end of a two-hour
     * window that started before they changed their mind.
     */
    fun setInterval(deviceId: String, interval: AutoSyncInterval) {
        stateStore.setAutoSyncInterval(deviceId, interval)
        if (interval.isOn) {
            enqueue(deviceId, interval, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
        } else {
            cancel(deviceId)
        }
    }

    /**
     * Re-plans every paired watch's schedule from what is stored. Idempotent,
     * and cheap enough for an app start: one preference read per watch, then a
     * KEEP that leaves an existing schedule exactly where it was.
     */
    fun restoreAll() {
        // Runs from Application.onCreate. Whatever WorkManager makes of the
        // request, a watch schedule is not worth failing app start over: the
        // next start tries again, and a scheduled sync is a convenience over
        // the Sync button, never the only way data arrives.
        runCatching {
            deviceRepository.devices.forEach { device ->
                val interval = stateStore.autoSyncInterval(device.id)
                if (interval.isOn && device.isGarminGfdi) {
                    enqueue(device.id, interval, ExistingPeriodicWorkPolicy.KEEP)
                } else {
                    // Covers a watch that stopped being syncable (re-registered
                    // as a live sensor) and one whose schedule outlived its
                    // preference, both of which would otherwise wake the radio
                    // for a sync nobody has asked for since.
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
            // Battery only. There is no network constraint to set: the whole
            // sync is Bluetooth, and the app holds no internet permission.
            // A phone below its low-battery mark has better things to spend
            // the radio on than data that will still be on the watch later.
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(uniqueWorkName(deviceId), policy, request)
    }

    companion object {
        /**
         * A watch that was out of range is usually back within minutes, so the
         * first retry comes well before the next scheduled run rather than
         * writing the period off.
         */
        private const val RETRY_BACKOFF_MINUTES = 10L

        fun uniqueWorkName(deviceId: String): String = "watch-auto-sync-$deviceId"
    }
}
