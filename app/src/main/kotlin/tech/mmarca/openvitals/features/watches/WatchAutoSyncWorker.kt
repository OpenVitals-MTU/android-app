package tech.mmarca.openvitals.features.watches

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.garmin.GarminLog

/**
 * One scheduled sync of one watch, run by `WorkManager` off
 * [WatchAutoSyncScheduler]'s periodic request.
 *
 * Deliberately NOT a foreground worker. The app allows itself a single
 * foreground service at a time (activity recording, the Apple Health import,
 * phone-to-phone sync), and a sync the user did not ask for must not be able
 * to take that slot — nor post a notification saying it is happening. A plain
 * worker gets ten minutes of execution, which is far more than a watch that
 * has been syncing every half hour needs; the pairing flow's optional
 * companion-device association is what keeps the process alive long enough for
 * a bigger catch-up run.
 *
 * Everything happens through [DeviceSyncController], not the port: it holds
 * the one-sync-at-a-time rule and the state every watch surface reads, so a
 * scheduled run and a tapped one cannot collide, and a sync started here shows
 * as busy on a screen the user opens mid-run.
 */
class WatchAutoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val deviceId = inputData.getString(KeyDeviceId)
        if (deviceId.isNullOrEmpty()) return Result.failure()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WatchAutoSyncWorkerEntryPoint::class.java,
        )
        val scheduler = entryPoint.watchAutoSyncScheduler()

        // The stored interval is the authority, not the fact that this work
        // exists: work that outlived the preference (a restore, a downgrade)
        // stops here rather than syncing forever.
        if (!scheduler.interval(deviceId).isOn) {
            scheduler.cancel(deviceId)
            return Result.success()
        }

        val device = entryPoint.bleDeviceRepository().devices.firstOrNull { it.id == deviceId }
        if (device == null) {
            // The watch was removed while this run was queued.
            scheduler.cancel(deviceId)
            return Result.success()
        }
        // A watch switched off stays paired and keeps its schedule; it is the
        // individual runs that stop. Turning it back on resumes them without
        // the user having to re-pick an interval.
        if (!device.enabled) return Result.success()

        // Null means something else holds the sync — a tap, or the previous
        // run overrunning. Skipping is right: the next tick is minutes away.
        val running = entryPoint.deviceSyncController().syncDevice(deviceId, silent = true)
            ?: return Result.success()

        return when (val result = running.await()) {
            is DeviceSyncResult.Succeeded -> Result.success()

            is DeviceSyncResult.Failed -> {
                GarminLog.log("[GARMIN-AUTOSYNC] run failed: ${result.message}")
                // A watch out of range, a busy radio, a recording in progress:
                // all ordinary, and all likely to clear soon. Retry a couple
                // of times on the backoff, then leave it to the next period
                // rather than spending the battery on a watch that is not
                // there.
                if (runAttemptCount < MaxAttempts) Result.retry() else Result.success()
            }
        }
    }

    companion object {
        private const val KeyDeviceId = "device_id"

        /** Attempts per period, the first included. */
        private const val MaxAttempts = 3

        fun inputData(deviceId: String): Data =
            Data.Builder().putString(KeyDeviceId, deviceId).build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WatchAutoSyncWorkerEntryPoint {
    fun deviceSyncController(): DeviceSyncController

    fun bleDeviceRepository(): BleDeviceRepository

    fun watchAutoSyncScheduler(): WatchAutoSyncScheduler
}
