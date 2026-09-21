package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.fit.FitDecoder
import tech.mmarca.openvitals.core.fit.fitInstant
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.devices.core.RadioLeaseBusyException
import tech.mmarca.openvitals.devices.core.RadioLeaseOwner
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.core.withRadioLease
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.usecase.FitBodyEnergyFromWatchUseCase
import tech.mmarca.openvitals.features.homewidgets.refreshPlacedHomeWidgets
import tech.mmarca.openvitals.features.manualentry.activity.recording.ActivityRecordingController

/**
 * Drives one sync with a Garmin watch: take the radio lease, open the link,
 * run the GFDI session, import, record what was taken, stamp the device.
 * Everything below it is radio-free; everything above knows nothing of COBS.
 */
@Singleton
class GarminWatchSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleDeviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val downloadImport: GarminDownloadImport,
    /** Keeps a copy of every download before the watch archives it. */
    private val fileStore: GarminFileStore,
    private val recordingController: ActivityRecordingController,
    private val bodyEnergyTimelineStore: BodyEnergyTimelineStore,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val fitBodyEnergyFromWatch: FitBodyEnergyFromWatchUseCase,
    /** Everything that touches Bluetooth. A test hands over a fake and drives a whole sync. */
    private val radio: GarminRadio,
) : DeviceSyncPort {

    private val syncMutex = Mutex()

    /** Outlives one sync: closing the sync screen must not cancel the chain rebuild it started. */
    private val rebuildScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Idempotent, and a no-op outside debug builds.
        GarminLog.installLogcatSink()
    }

    /** A watch OR an Edge bike computer — both pull recorded FIT files over GFDI. */
    override fun canSync(device: BleSensorDevice): Boolean = device.isGarminGfdi

    /**
     * The held link's owner for [device], backed by this service's stores.
     * [onFullSyncNeeded] stays with the caller, which guards against overlapping syncs.
     */
    fun heldSyncOwner(device: BleSensorDevice, onFullSyncNeeded: () -> Unit): GarminHeldSyncOwner =
        object : GarminHeldSyncOwner {
            override fun alreadySyncedKeys(): Set<String> = stateStore.syncedFileKeys(device.id)

            override suspend fun keep(file: GarminDownloadedFile) {
                fileStore.save(file, now = Instant.now(), deviceId = device.id)
            }

            override fun imported(files: List<GarminDownloadedFile>) {
                if (files.isNotEmpty()) importAnnouncedFiles(device, files)
            }

            override fun needsFullSync() = onFullSyncNeeded()
        }

    private fun importAnnouncedFiles(
        device: BleSensorDevice,
        files: List<GarminDownloadedFile>,
    ) {
        rebuildScope.launch {
            try {
                downloadImport.import(device.id, files)
                refreshBodyEnergy(files)
                bleDeviceRepository.markSynced(device.id, Instant.now())
                refreshPlacedHomeWidgets(context)
                GarminLog.log("[GARMIN-COMPANION] imported ${files.size} filtered file(s)")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-COMPANION] filtered import failed: $error")
            }
        }
    }

    override suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult = syncMutex.withLock {
        syncSerially(device, listenAfter, onProgress)
    }

    private suspend fun syncSerially(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult {
        // A live recording holds the foreground slot and the radio. Refuse.
        if (recordingController.state.value.isActive) {
            return DeviceSyncResult.Failed(
                "An activity recording is in progress. Finish or discard it " +
                    "before syncing the watch.",
            )
        }

        // Files a run saved but never imported: a crash, or an import that threw. The watch
        // has archived them, so this is their only way in. Before the pull, so their keys are
        // recorded before the listing is filtered and they are not fetched twice.
        try {
            importPendingFiles(device)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The write path is down. Pulling more now would only queue more behind it.
            GarminLog.log("[GARMIN-SYNC] pending import failed: $error")
            return DeviceSyncResult.Failed(describe(error))
        }

        val pull = try {
            withRadioLease(device.address, RadioLeaseOwner.SYNC) {
                radio.pull(device, listenAfter, onProgress)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: RadioLeaseBusyException) {
            return DeviceSyncResult.Failed("The watch is busy (${error.holder}). Try again in a moment.")
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-SYNC] failed: $error")
            return DeviceSyncResult.Failed(describe(error))
        }

        val downloaded = pull.files
        var activities = GarminActivityImportResult()
        if (downloaded.isNotEmpty()) {
            try {
                // Records the keys after the import, so a run that died mid-import downloads again.
                activities = downloadImport.import(device.id, downloaded)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Reaching here means the write path itself is unavailable.
                GarminLog.log("[GARMIN-SYNC] import failed: $error")
                // Nothing reached Health Connect, so the next run must fetch again.
                return DeviceSyncResult.Failed(describe(error))
            }

            refreshBodyEnergy(downloaded)
        }

        val incompleteReason = pull.incompleteReason
        if (incompleteReason != null) {
            return DeviceSyncResult.Failed(
                if (downloaded.isEmpty()) incompleteReason
                else "Imported ${downloaded.size} file(s), but sync was interrupted: $incompleteReason",
            )
        }

        bleDeviceRepository.markSynced(device.id, Instant.now())
        // A sync that landed a night of sleep must not leave the widgets stale.
        refreshPlacedHomeWidgets(context)
        // Saying "synced" here hid workouts that never reached Health Connect.
        notImportedMessage(activities)?.let { return DeviceSyncResult.Failed(it) }
        return DeviceSyncResult.Succeeded(downloaded.size)
    }

    /**
     * Drops and rebuilds the Body Energy days this sync back-filled. Body
     * Energy chains across midnight, so new data invalidates every later day,
     * and the staleness rule never revisits a settled day. Best-effort.
     */
    private suspend fun refreshBodyEnergy(downloaded: List<GarminDownloadedFile>) {
        val earliest = try {
            garminEarliestAffectedDay(downloaded)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-SYNC] could not date the downloaded files: $error")
            null
        }
        val today = LocalDate.now()

        if (garminNeedsFullBodyEnergyInvalidation(downloaded)) {
            try {
                bodyEnergyTimelineStore.purgeAll()
                GarminLog.log("[GARMIN-SYNC] body-energy chain invalidated: downloaded file date unavailable")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SYNC] body-energy chain invalidate skipped: $error")
            }
        } else if (earliest != null && !earliest.isAfter(today)) {
            try {
                bodyEnergyTimelineStore.invalidateForward(earliest, today)
                GarminLog.log("[GARMIN-SYNC] body-energy chain invalidated from $earliest")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SYNC] body-energy chain invalidate skipped: $error")
            }
        }

        // Rebuild rather than leave holes. Forced past the throttle, and off the
        // sync's coroutine because the walk has its own budget.
        rebuildScope.launch {
            runCatching { bodyEnergyChainSync.syncAll(force = true) }
                .onFailure { GarminLog.log("[GARMIN-SYNC] body-energy rebuild failed: $it") }
            // Fit the gains only after the rebuild, or the model learns from
            // data that had not arrived yet. The evidence keeps if the rebuild fails.
            runCatching { fitBodyEnergyFromWatch() }
                .onFailure { GarminLog.log("[GARMIN-SYNC] body-energy calibration skipped: $it") }
        }
    }

    /**
     * Makes the watch at [address] alert and keeps the link open while it
     * does. Find is a toggle with a timeout; completing [cancelled] stops it.
     * Returns whether the watch accepted the request. Throws
     * [RadioLeaseBusyException] or [GarminGattClientException].
     */
    suspend fun findWatch(
        address: String,
        timeout: Duration = GarminFindMyWatch.defaultTimeout,
        cancelled: CompletableDeferred<Unit>? = null,
    ): Boolean = withRadioLease(address, RadioLeaseOwner.FIND) {
        radio.find(address, timeout, cancelled)
    }


    /**
     * Writes [bytes] to the watch as a new [type] file, on a link opened for
     * this one send. Never throws for a watch that is busy, away or unwilling:
     * the result says which.
     */
    suspend fun uploadFile(
        device: BleSensorDevice,
        type: GarminUploadFileType,
        bytes: ByteArray,
        onStage: (GarminSendStage) -> Unit = {},
    ): GarminSendFileResult {
        // As for a sync: the recording owns the radio.
        if (recordingController.state.value.isActive) return GarminSendFileResult.RecordingActive
        // The SYNC lease is shared with a sync, so the mutex is what keeps the two apart.
        if (!syncMutex.tryLock()) return GarminSendFileResult.SyncRunning
        return try {
            onStage(GarminSendStage.CONNECTING)
            withTimeoutOrNull(SEND_TIMEOUT) {
                withRadioLease(device.address, RadioLeaseOwner.SYNC) {
                    radio.upload(device, type, bytes, onStage)
                }
            } ?: GarminSendFileResult.NoAnswer
        } catch (error: CancellationException) {
            throw error
        } catch (error: RadioLeaseBusyException) {
            GarminSendFileResult.Busy(error.holder)
        } catch (error: Exception) {
            GarminLog.log("[GARMIN-SEND] could not reach the watch: $error")
            GarminSendFileResult.Unreachable
        } finally {
            syncMutex.unlock()
        }
    }




    /** Imports what [GarminFileStore.pending] holds for [device]. Throws when the write path is down. */
    private suspend fun importPendingFiles(device: BleSensorDevice) {
        val pending = fileStore.pending(device.id)
        if (pending.isEmpty()) return
        GarminLog.log("[GARMIN-SYNC] importing ${pending.size} file(s) an earlier run saved but never imported")
        downloadImport.import(device.id, pending)
        refreshBodyEnergy(pending)
    }

    private fun notImportedMessage(activities: GarminActivityImportResult): String? = when {
        activities.missingPermission.isNotEmpty() ->
            "${activities.missingPermission.size} workout(s) were not imported. Allow OpenVitals to " +
                "write exercise data in Health Connect, then sync again."
        activities.failedWrites.isNotEmpty() ->
            "${activities.failedWrites.size} workout(s) could not be saved to Health Connect. " +
                "The next sync fetches them again."
        else -> null
    }

    private fun describe(error: Throwable): String {
        if (error is GarminGattClientException) {
            return error.message ?: "The watch could not be synced."
        }
        val text = error.message ?: error.toString()
        return text.ifBlank { "The watch could not be synced." }
    }


    private companion object {
        /** A whole-send safety net: handover, handshake and a small file fit well inside. */
        val SEND_TIMEOUT = 45.seconds

    }
}

/**
 * The earliest local day the downloaded files carry data for, or null when
 * the watch dated none of them. Pure, so it is testable without the service.
 */
fun garminEarliestAffectedDay(
    downloaded: List<GarminDownloadedFile>,
    zone: ZoneId = ZoneId.systemDefault(),
): LocalDate? = downloaded
    .mapNotNull(::garminFileDate)
    .minOrNull()
    ?.atZone(zone)
    ?.toLocalDate()

/** True when a wanted file cannot be dated at all, so no day is safe to keep. */
fun garminNeedsFullBodyEnergyInvalidation(downloaded: List<GarminDownloadedFile>): Boolean =
    downloaded.any { it.entry.type.wanted && garminFileDate(it) == null }

/** The listing's date, or the earliest FIT timestamp when the listing has none. */
fun garminFileDate(file: GarminDownloadedFile): Instant? =
    file.entry.fileDate ?: runCatching { garminFitFileDate(file.bytes) }.getOrNull()

/** The earliest timestamp across the chained FIT files in [bytes], or null when none carries one. */
fun garminFitFileDate(bytes: ByteArray): Instant? {
    var offset = 0
    var earliest: Long? = null
    while (offset < bytes.size && FitDecoder.isFitFileAt(bytes, offset)) {
        val file = FitDecoder.readFile(bytes, offset)
        file.messages.mapNotNull { it.timestamp }.minOrNull()?.let { timestamp ->
            earliest = earliest?.coerceAtMost(timestamp) ?: timestamp
        }
        if (file.nextOffset <= offset) break
        offset = file.nextOffset
    }
    return earliest?.let(::fitInstant)
}
