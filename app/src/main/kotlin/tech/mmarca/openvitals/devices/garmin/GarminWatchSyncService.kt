package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.fit.FitDecoder
import tech.mmarca.openvitals.core.fit.fitInstant
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.devices.core.RadioLeaseBusyException
import tech.mmarca.openvitals.devices.core.RadioLeaseOwner
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPort
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncResult
import tech.mmarca.openvitals.devices.core.withRadioLease
import tech.mmarca.openvitals.devices.garmin.wellness.FitWellnessImporter
import tech.mmarca.openvitals.devices.weather.WeatherStore
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
    private val importer: FitWellnessImporter,
    private val activityImporter: GarminActivityImporter,
    private val recordingController: ActivityRecordingController,
    private val bodyEnergyTimelineStore: BodyEnergyTimelineStore,
    private val bodyEnergyChainSync: BodyEnergyChainSyncService,
    private val fitBodyEnergyFromWatch: FitBodyEnergyFromWatchUseCase,
    private val findPhoneRinger: GarminFindPhoneRinger,
    private val weatherStore: WeatherStore,
    private val locationSource: GarminPhoneLocationSource,
    private val agpsStore: GarminAgpsStore,
    private val calendarSource: GarminCalendarSource,
) : DeviceSyncPort {

    private val phone = GarminPhoneIdentity()

    private val syncMutex = Mutex()

    /** Outlives one sync: closing the sync screen must not cancel the chain rebuild it started. */
    private val rebuildScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Keeps a copy of every download before the watch archives it. */
    private val fileStore = GarminFileStore(
        resolveDirectory = { File(context.filesDir, FILE_STORE_DIRECTORY) },
    )

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
                fileStore.save(file, now = Instant.now())
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
                importer.import(files)
                activityImporter.import(files)
                stateStore.recordSyncedFileKeys(
                    device.id,
                    files.mapNotNull { it.entry.dedupKey },
                )
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

        val pull = try {
            withRadioLease(device.address, RadioLeaseOwner.SYNC) {
                pullFiles(device, listenAfter, onProgress)
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
        if (downloaded.isNotEmpty()) {
            try {
                importer.import(downloaded)
                // Same path as a hand-picked FIT folder. Per-file failures are
                // tolerated inside the importer.
                activityImporter.import(downloaded)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Reaching here means the write path itself is unavailable.
                GarminLog.log("[GARMIN-SYNC] import failed: $error")
                // Nothing reached Health Connect, so the next run must fetch again.
                return DeviceSyncResult.Failed(describe(error))
            }

            // Recorded after the import, so a run that died mid-import re-downloads.
            // Files with no stable key are re-fetched every sync by design.
            stateStore.recordSyncedFileKeys(
                device.id,
                downloaded.mapNotNull { it.entry.dedupKey },
            )

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
        runFind(address, timeout, cancelled)
    }

    private suspend fun runFind(
        address: String,
        timeout: Duration,
        cancelled: CompletableDeferred<Unit>?,
    ): Boolean = coroutineScope {
        val client = GarminGattClient(context, address)
        var transport: GarminFrameTransport? = null
        val ready = CompletableDeferred<Unit>()
        // The watch reports a find it ended itself. Without this the phone shows
        // "Stop" for the full minute.
        val endedOnWatch = CompletableDeferred<Unit>()
        val session = GarminSession(
            scope = this,
            send = { frame ->
                (transport ?: throw GarminGattClientException("Not connected")).sendFrame(frame)
            },
            bluetoothName = phone.bluetoothName,
            manufacturer = phone.manufacturer,
            model = phone.model,
            // A file sync would die mid-transfer when the link closes.
            syncFiles = false,
            hooks = GarminSessionHooks(onHandshakeReady = { ready.complete(Unit) }),
        )
        session.protobuf.onUnsolicited = { payload ->
            if (GarminFindMyWatch.isFindMessage(payload)) {
                GarminLog.log("[GARMIN-FIND] the watch says the alert ended")
                endedOnWatch.complete(Unit)
            }
        }

        val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
        val pump = launch { for (frame in frames) session.handleFrame(frame) }
        val dropWatch = launch { client.onDisconnected.collect { session.abort(it) } }
        var ringing = false
        try {
            transport = client.connect(onFrame = { frame -> frames.trySend(frame) })
            session.start()
            // The watch ignores anything sent before the handshake finishes.
            if (withTimeoutOrNull(HANDSHAKE_TIMEOUT) { ready.await() } == null) {
                GarminLog.log("[GARMIN-FIND] the watch never finished its handshake")
                return@coroutineScope false
            }

            val reply = session.protobuf.request(
                GarminFindMyWatch.start(timeout),
                label = "find start",
            )
            val outcome = GarminFindMyWatch.outcome(reply)
            GarminLog.log("[GARMIN-FIND] ${outcome.name}")
            // Only an explicit ERROR is a refusal. An unreadable reply is not:
            // the watch was seen ringing while one was treated as failure.
            if (outcome.declined) return@coroutineScope false

            ringing = true
            // Hold the link for the alert, or until stopped.
            withTimeoutOrNull(timeout) {
                select<Unit> {
                    endedOnWatch.onAwait { }
                    cancelled?.onAwait { }
                }
            }
            true
        } finally {
            // Always cancel a started alert, on every path out.
            if (ringing) {
                try {
                    // The cancel must still go out when this coroutine is being torn down.
                    withContext(NonCancellable) {
                        session.protobuf.request(
                            GarminFindMyWatch.cancel(),
                            label = "find cancel",
                            timeout = FIND_CANCEL_TIMEOUT,
                        )
                    }
                } catch (error: Exception) {
                    GarminLog.log("[GARMIN-FIND] could not cancel: $error")
                }
            }
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("[GARMIN-FIND] link closed")
        }
    }

    /**
     * Connect, run the GFDI session, return what it downloaded. Throws
     * [GarminGattClientException] when the watch cannot be reached. A dropped
     * link is not an error: the session returns what it already has.
     */
    private suspend fun pullFiles(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): PullResult = coroutineScope {
        val client = GarminGattClient(context, device.address)
        var transport: GarminFrameTransport? = null
        val handshakeReady = CompletableDeferred<Unit>()
        val session = GarminSession(
            scope = this,
            // Bound after the transport opens.
            send = { frame ->
                (transport ?: throw GarminGattClientException("Not connected")).sendFrame(frame)
            },
            bluetoothName = phone.bluetoothName,
            manufacturer = phone.manufacturer,
            model = phone.model,
            alreadySynced = stateStore.syncedFileKeys(device.id),
            onProgress = { progress ->
                onProgress?.invoke(
                    DeviceSyncProgress(
                        phase = devicePhase(progress.phase),
                        filesTotal = progress.filesTotal,
                        filesDone = progress.filesDone,
                    ),
                )
            },
            onFileDownloaded = { file -> fileStore.save(file, now = Instant.now()) },
            keepAnsweringAfterSync = true,
            hooks = GarminSessionHooks(
                onFindPhone = { seconds -> findPhoneRinger.start(seconds) },
                onFindPhoneCancel = { findPhoneRinger.stop() },
                weatherProvider = { weatherStore.freshSnapshot() },
                locationProvider = { locationSource.lastKnown() },
                agpsSource = agpsStore.source(),
                calendarProvider = { begin, end ->
                    if (stateStore.calendarSync(device.id)) calendarSource.events(begin, end) else null
                },
                setupWizardPending = stateStore.setupWizardPending(device.id),
                onSetupWizardCompleted = { stateStore.setSetupWizardPending(device.id, false) },
                onHandshakeReady = { handshakeReady.complete(Unit) },
            ),
        )

        // Housekeeping before the link opens, so it cannot delay the sync.
        fileStore.prune(now = Instant.now())

        // Frames land on the binder thread; a channel keeps their order.
        val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
        val pump = launch { for (frame in frames) session.handleFrame(frame) }
        // A dropped link ends the sync with what it has.
        val dropWatch = launch { client.onDisconnected.collect { session.abort(it) } }
        try {
            // Logged before connecting: a wedge inside connect read as "nothing happened".
            GarminLog.log("[GARMIN-SYNC] connecting to the watch")
            transport = client.connect(onFrame = { frame -> frames.trySend(frame) })
            session.start()
            if (withTimeoutOrNull(HANDSHAKE_TIMEOUT) { handshakeReady.await() } == null) {
                session.abort(
                    "Watch did not complete the Garmin handshake within " +
                        "${HANDSHAKE_TIMEOUT.inWholeSeconds}s",
                )
            }
            // Battery percentage rides the same link via the protobuf DeviceStatusService.
            // Collected beside the pull; bounded by a timeout.
            val batteryJob = launch {
                runCatching {
                    if (withTimeoutOrNull(HANDSHAKE_TIMEOUT) { handshakeReady.await() } == null) {
                        return@launch
                    }
                    val reply = session.protobuf.request(
                        GarminDeviceStatus.batteryRequest(),
                        label = "battery",
                        timeout = BATTERY_TIMEOUT,
                    )
                    GarminDeviceStatus.batteryLevel(reply)?.let { level ->
                        GarminLog.log("[GARMIN-SYNC] battery $level%")
                        bleDeviceRepository.updateBatteryLevel(device.id, level)
                    }
                }
            }
            val started = TimeSource.Monotonic.markNow()
            val timedOut = GarminSyncTimeout.whole(SYNC_TIMEOUT)
            var files = withTimeoutOrNull(SYNC_TIMEOUT) { session.done.await() }
                ?: run {
                    session.abort(timedOut)
                    session.done.await()
                }
            // Read now: a link drop after a finished sync must not fail it.
            val legacyReason = session.abortReason
            var fallbackReason: String? = null
            val knownProtocol = stateStore.syncProtocol(device.id)
            // Any listed record proves the legacy directory works, even with nothing new.
            if (session.directoryRecordCount > 0 && knownProtocol != GarminSyncProtocol.FILE_SYNC) {
                stateStore.recordSyncProtocol(device.id, GarminSyncProtocol.LEGACY)
            }
            if (files.isEmpty() &&
                legacyReason == null &&
                knownProtocol != GarminSyncProtocol.LEGACY
            ) {
                val ml = transport as? GarminMlTransport
                if (ml != null) {
                    // The fallback shares the sync's time budget.
                    val budget = SYNC_TIMEOUT - started.elapsedNow()
                    val newer = try {
                        withTimeoutOrNull(budget) {
                            GarminFileSyncTransfer(
                                protobuf = session.protobuf,
                                transport = ml,
                                keep = { file -> fileStore.save(file, now = Instant.now()) },
                                onProtocolProven = {
                                    stateStore.recordSyncProtocol(device.id, GarminSyncProtocol.FILE_SYNC)
                                },
                                onProgress = { total, done ->
                                    onProgress?.invoke(
                                        DeviceSyncProgress(
                                            phase = DeviceSyncPhase.DOWNLOADING,
                                            filesTotal = total,
                                            filesDone = done,
                                        ),
                                    )
                                },
                            ).pull(stateStore.syncedFileKeys(device.id))
                        } ?: run {
                            fallbackReason = timedOut
                            emptyList()
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        GarminLog.log("[GARMIN-SYNC] new file sync interrupted: $error")
                        val failure = "Garmin FileSyncService failed: " +
                            (error.message ?: error::class.java.simpleName)
                        if (!session.hasValidDirectoryListing) {
                            session.abort(failure)
                        } else if (knownProtocol == GarminSyncProtocol.FILE_SYNC) {
                            // A proven FileSync watch that fails is interrupted, not empty.
                            fallbackReason = failure
                        }
                        emptyList()
                    }
                    if (newer.isNotEmpty()) files = newer
                    // A drop during the fallback is a real interruption.
                    if (fallbackReason == null) fallbackReason = session.abortReason
                }
            }
            // Bounded: the sync result must not wait on a battery answer that
            // is not coming.
            withTimeoutOrNull(BATTERY_TIMEOUT) { batteryJob.join() }
            batteryJob.cancel()
            stateStore.recordCapabilities(device.id, session.capabilities)
            if (listenAfter > Duration.ZERO) {
                // Diagnostic pass: hold the link to see what the watch sends unprompted.
                GarminLog.log(
                    "[GARMIN-LISTEN] holding the link open for " +
                        "${listenAfter.inWholeMinutes}m — touch the watch now",
                )
                delay(listenAfter)
                GarminLog.log("[GARMIN-LISTEN] window closed")
            }
            PullResult(files = files, incompleteReason = legacyReason ?: fallbackReason)
        } finally {
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("[GARMIN-SYNC] link closed")
        }
    }

    /** What a pull landed, and why it stopped early if it did. */
    private data class PullResult(
        val files: List<GarminDownloadedFile>,
        val incompleteReason: String? = null,
    )

    private fun describe(error: Throwable): String {
        if (error is GarminGattClientException) {
            return error.message ?: "The watch could not be synced."
        }
        val text = error.message ?: error.toString()
        return text.ifBlank { "The watch could not be synced." }
    }

    /** Maps a Garmin protocol phase onto the generic [DeviceSyncPhase] (1:1 today). */
    private fun devicePhase(phase: GarminSyncPhase): DeviceSyncPhase = when (phase) {
        GarminSyncPhase.HANDSHAKE -> DeviceSyncPhase.HANDSHAKE
        GarminSyncPhase.LISTING -> DeviceSyncPhase.LISTING
        GarminSyncPhase.DOWNLOADING -> DeviceSyncPhase.DOWNLOADING
        GarminSyncPhase.COMPLETE -> DeviceSyncPhase.COMPLETE
        GarminSyncPhase.FAILED -> DeviceSyncPhase.FAILED
    }

    private companion object {
        /** Under the app's files dir, matching the Flutter build's `garmin/`. */
        const val FILE_STORE_DIRECTORY = "garmin"

        /** How long a watch gets to finish its handshake before a find gives up. */
        val HANDSHAKE_TIMEOUT = 15.seconds

        /** A whole-sync safety net; the session's stage timers fail sooner. */
        val SYNC_TIMEOUT = 3.minutes

        /** A best-effort cancel must not hold the link hostage. */
        val FIND_CANCEL_TIMEOUT = 3.seconds
        val BATTERY_TIMEOUT = 5.seconds
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
