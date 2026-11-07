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
 * One scheduled sync of one watch, run by `WorkManager`. Not a foreground
 * worker: an unasked-for sync must not take the app's single foreground
 * slot. Goes through [DeviceSyncController], so a scheduled run and a
 * tapped one cannot collide.
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

        // The stored interval is the authority: work that outlived it stops here.
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
        // A watch switched off keeps its schedule; the runs stop.
        if (!device.enabled) return Result.success()

        // Null means something else holds the sync. The next tick is minutes away.
        val running = entryPoint.deviceSyncController().syncDevice(deviceId, silent = true)
            ?: return Result.success()

        return when (val result = running.await()) {
            is DeviceSyncResult.Succeeded -> Result.success()

            is DeviceSyncResult.Failed -> {
                GarminLog.log("[GARMIN-AUTOSYNC] run failed: ${result.message}")
                // Out of range, busy radio, recording: retry on the backoff, then wait for the next period.
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
