package tech.mmarca.openvitals.devices.wearos

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.core.presentation.isPermissionFailure
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController

/**
 * Pulls what the OpenVitals Wear OS app recorded and writes it to Health
 * Connect. The [DeviceSyncPort] for `(WATCH, WEAROS)` devices.
 *
 * One page at a time from the watch's cursor, each page written before the
 * next is asked for, and the cursor advanced after each write. A sync that
 * dies mid-way therefore loses nothing: the next one resumes at the last
 * page written, and a page written twice upserts.
 *
 * [DeviceSyncResult.Succeeded.fileCount] carries the number of samples; the
 * Wear OS card words it as samples.
 */
@Singleton
class WearOsWatchSyncService @Inject constructor(
    private val nodePort: WearOsNodePort,
    private val cursors: WearOsSyncCursorStore,
    private val importRepository: AppleHealthImportRepository,
    private val bleDeviceRepository: BleDeviceRepository,
    private val recordingController: ActivityRecordingController,
) : DeviceSyncPort {

    private val syncMutex = Mutex()

    override fun canSync(device: BleSensorDevice): Boolean = device.isWearosWatch

    override suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult = syncMutex.withLock {
        syncSerially(device, onProgress)
    }

    private suspend fun syncSerially(
        device: BleSensorDevice,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult {
        // A live recording holds the foreground slot. Refuse, as the Garmin sync does.
        if (recordingController.state.value.isActive) {
            return DeviceSyncResult.Failed(
                "An activity recording is in progress. Finish or discard it before syncing the watch.",
            )
        }

        onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.HANDSHAKE))
        var cursor = cursors.heartRateCursor(device.id)
        var written = 0
        var pages = 0
        try {
            while (true) {
                val page = nodePort.pullHeartRate(device.address, device.bluetoothName, cursor)
                    ?: return DeviceSyncResult.Failed(
                        "No paired Wear OS watch matches this one. Pair it in Android's Bluetooth settings.",
                    )
                pages++
                onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.DOWNLOADING, filesTotal = pages, filesDone = pages - 1))
                val newest = page.samples.maxOfOrNull { it.time }
                if (newest != null) {
                    importRepository.insertImportedRecords(WearOsHeartRateImport.records(page.samples))
                    written += page.samples.size
                    cursor = newest
                    cursors.setHeartRateCursor(device.id, cursor)
                }
                if (!page.hasMore || newest == null) break
                if (pages >= MAX_PAGES) break
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: WearOsLinkException) {
            return DeviceSyncResult.Failed(
                if (written == 0) {
                    "The watch did not answer. Make sure it is nearby and the OpenVitals app is running on it."
                } else {
                    "Imported $written sample(s), but the watch stopped answering: ${error.message}"
                },
            )
        } catch (error: SecurityException) {
            return DeviceSyncResult.Failed(permissionMessage(error))
        } catch (error: Exception) {
            if (error.isPermissionFailure()) return DeviceSyncResult.Failed(permissionMessage(error))
            return DeviceSyncResult.Failed(error.message?.ifBlank { null } ?: "The watch could not be synced.")
        }

        onProgress?.invoke(DeviceSyncProgress(DeviceSyncPhase.COMPLETE, filesTotal = pages, filesDone = pages))
        bleDeviceRepository.markSynced(device.id, Instant.now())
        return DeviceSyncResult.Succeeded(written)
    }

    private fun permissionMessage(error: Throwable): String =
        if (error is SecurityException && error.message?.contains("BLUETOOTH", ignoreCase = true) == true) {
            "OpenVitals needs the Nearby devices permission to reach the watch."
        } else {
            "Allow OpenVitals to write heart rate in Health Connect, then sync again."
        }

    private companion object {
        /** A safety net against a watch that always says "more": two million samples. */
        const val MAX_PAGES = 1000
    }
}
