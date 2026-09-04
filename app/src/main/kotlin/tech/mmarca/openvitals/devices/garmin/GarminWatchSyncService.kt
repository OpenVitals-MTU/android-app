package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.domain.usecase.FitBodyEnergyFromWatchUseCase
import tech.mmarca.openvitals.features.homewidgets.refreshPlacedHomeWidgets
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

    /**
     * The radio lease is keyed by owner tag, so two callers both using SYNC
     * look re-entrant to it. Serialize them here before either opens GATT;
     * otherwise a manual tap racing an announced-file sync gives two sessions
     * the same watch frames and corrupts both protocol conversations.
     */
    private val syncMutex = Mutex()

    /**
     * Outlives one sync on purpose: the chain walk has its own time budget and
     * a user who closes the sync screen the moment it says "done" must not
     * cancel the rebuild their sync just made necessary.
     */
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

    internal fun syncedFileKeys(deviceId: String): Set<String> =
        stateStore.syncedFileKeys(deviceId)

    /** Raw bytes must be durable before the held session archives the file. */
    internal suspend fun storeAnnouncedFile(file: GarminDownloadedFile) {
        fileStore.save(file, now = Instant.now())
    }

    /**
     * Imports files pulled by the companion link without reopening BLE. The
     * link already persisted every file before archiving it.
     */
    internal fun importAnnouncedFiles(
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

        // A dropped or timed-out link may still have yielded useful files.
        // Import those, but do not stamp the run as successful: otherwise a
        // zero-file disconnect looks exactly like "nothing new" and hides the
        // transport failure from both the user and auto-sync retry policy.
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

        if (earliest != null && !earliest.isAfter(today)) {
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
            onHandshakeReady = { ready.complete(Unit) },
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
    private data class GarminPullResult(
        val files: List<GarminDownloadedFile>,
        val incompleteReason: String? = null,
    )

    private suspend fun pullFiles(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): GarminPullResult = coroutineScope {
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
            // The legacy directory may finish empty before we try the newer
            // protobuf FileSyncService on the same link. Keep dispatching
            // frames until the GATT client closes so those replies are not
            // discarded merely because the legacy result was sealed.
            keepAnsweringAfterSync = true,
            onFindPhone = { seconds -> findPhoneRinger.start(seconds) },
            onFindPhoneCancel = { findPhoneRinger.stop() },
            weatherProvider = { weatherStore.freshSnapshot() },
            locationProvider = { locationSource.lastKnown() },
            agpsSource = agpsStore.source(),
            calendarProvider = { begin, end ->
                if (stateStore.calendarSync(device.id)) {
                    calendarSource.events(begin, end)
                } else {
                    null
                }
            },
            setupWizardPending = stateStore.setupWizardPending(device.id),
            onSetupWizardCompleted = { stateStore.setSetupWizardPending(device.id, false) },
            onHandshakeReady = { handshakeReady.complete(Unit) },
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
                session.abort("Watch did not complete the Garmin handshake within ${HANDSHAKE_TIMEOUT.inWholeSeconds}s")
            }
            // The battery percentage rides the same link, via the protobuf
            // DeviceStatusService — the one place the watch reports a real
            // percentage (the GFDI battery message is only good/ok/low).
            // Fire-and-collect beside the file pull; a watch that never
            // answers costs the timeout and nothing else.
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
            var files = withTimeoutOrNull(SYNC_TIMEOUT) { session.done.await() }
                ?: run {
                    session.abort("Garmin file sync timed out after ${SYNC_TIMEOUT.inWholeMinutes} minutes")
                    session.done.await()
                }
            val knownProtocol = stateStore.syncProtocol(device.id)
            if (files.isNotEmpty() && knownProtocol != GarminSyncProtocol.FILE_SYNC) {
                stateStore.recordSyncProtocol(device.id, GarminSyncProtocol.LEGACY)
            }
            if (files.isEmpty() &&
                session.abortReason == null &&
                knownProtocol != GarminSyncProtocol.LEGACY
            ) {
                val ml = transport as? GarminMlTransport
                if (ml != null) {
                    val newer = try {
                        pullFileSyncServiceFiles(
                            device = device,
                            session = session,
                            transport = ml,
                            onProgress = onProgress,
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        GarminLog.log("[GARMIN-SYNC] new file sync interrupted: $error")
                        // FileSync is an optional fallback on a watch whose
                        // legacy directory was empty. A timeout proves
                        // nothing about support and must not turn a valid
                        // legacy empty listing into a user-visible failure.
                        // Keep protocol UNKNOWN so a later sync may probe
                        // again; only fail when no valid legacy listing was
                        // received either.
                        if (!session.hasValidDirectoryListing) {
                            session.abort(
                                "Garmin FileSyncService failed: " +
                                    (error.message ?: error::class.java.simpleName),
                            )
                        }
                        emptyList()
                    }
                    if (newer.isNotEmpty()) files = newer
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
            GarminPullResult(files = files, incompleteReason = session.abortReason)
        } finally {
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("[GARMIN-SYNC] link closed")
        }
    }

    private suspend fun pullFileSyncServiceFiles(
        device: BleSensorDevice,
        session: GarminSession,
        transport: GarminMlTransport,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): List<GarminDownloadedFile> {
        val remote = mutableListOf<GarminFileSyncProtocol.RemoteFile>()
        var cursorId: Long? = null
        var startPageId: Long? = null
        var pageCount = 0
        val seenPages = mutableSetOf<Pair<Long?, Long?>>()
        do {
            val pageToken = cursorId to startPageId
            if (!seenPages.add(pageToken)) {
                GarminLog.log("[GARMIN-SYNC] file list repeated page token $pageToken")
                break
            }
            val reply = session.protobuf.request(
                GarminFileSyncProtocol.listRequest(
                    cursorId = cursorId,
                    startPageId = startPageId,
                ),
                label = "file-sync list",
                timeout = FILE_SYNC_REPLY_TIMEOUT,
                acceptUnmatched = { GarminFileSyncProtocol.parseListResponse(it) != null },
            )
            if (reply == null) {
                GarminLog.log("[GARMIN-SYNC] FileSyncService list timed out; support remains unknown")
                throw GarminFileSyncException("file-list response timed out")
            }
            val page = GarminFileSyncProtocol.parseListResponse(reply)
            if (page == null) {
                GarminLog.log("[GARMIN-SYNC] FileSyncService returned an invalid list")
                throw GarminFileSyncException("file-list response was invalid")
            }
            stateStore.recordSyncProtocol(device.id, GarminSyncProtocol.FILE_SYNC)
            remote += page.files
            // cursor_id means there are more chunks in the current listing.
            // Once that is exhausted, next_page_id advances to the next
            // listing. Some watches only expose SPORTS files on those later
            // pages, so stopping after the cursor chain silently loses every
            // workout while still reporting a successful sync.
            if (page.cursorId != null) {
                cursorId = page.cursorId
                startPageId = null
            } else {
                cursorId = null
                startPageId = page.nextPageId
            }
            pageCount += 1
        } while ((cursorId != null || startPageId != null) && pageCount < MAX_FILE_SYNC_PAGES)

        val already = stateStore.syncedFileKeys(device.id)
        val wanted = remote.filter { file ->
            file.type?.wanted == true && file.dedupKey !in already
        }
        if (wanted.isEmpty()) return emptyList()

        val downloaded = mutableListOf<GarminDownloadedFile>()
        for ((index, remoteFile) in wanted.withIndex()) {
            try {
                onProgress?.invoke(
                    DeviceSyncProgress(
                        phase = DeviceSyncPhase.DOWNLOADING,
                        filesTotal = wanted.size,
                        filesDone = index,
                    ),
                )
                val reply = session.protobuf.request(
                    GarminFileSyncProtocol.fileRequest(remoteFile),
                    label = "file-sync file ${remoteFile.typeName}",
                    timeout = FILE_SYNC_REPLY_TIMEOUT,
                    acceptUnmatched = { GarminFileSyncProtocol.parseFileResponse(it) != null },
                ) ?: continue
                val response = GarminFileSyncProtocol.parseFileResponse(reply) ?: continue
                val handle = response.handle
                if (response.status != 0 || handle == null) continue
                val bytes = downloadCompressedFile(transport, handle) ?: continue
                val type = remoteFile.type ?: continue
                val file = GarminDownloadedFile(
                    entry = GarminDirectoryEntry(
                        fileIndex = handle,
                        type = type,
                        fileNumber = GarminDirectoryEntry.UNSET_FILE_NUMBER,
                        specificFlags = 0,
                        fileFlags = 0,
                        fileSize = bytes.size.toLong(),
                        fileDate = null,
                        remoteDedupKey = remoteFile.dedupKey,
                    ),
                    bytes = bytes,
                )
                fileStore.save(file, now = Instant.now())
                downloaded += file
                session.protobuf.sendUnanswered(
                    GarminFileSyncProtocol.markSynced(remoteFile),
                    label = "mark file synced",
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                GarminLog.log("[GARMIN-SYNC] file transfer interrupted: $error")
                break
            }
        }
        return downloaded
    }

    private suspend fun downloadCompressedFile(
        transport: GarminMlTransport,
        fileHandle: Int,
    ): ByteArray? {
        val serviceCode = FILE_TRANSFER_SERVICES.firstOrNull {
            !transport.isServiceOpen(it)
        } ?: return null
        val compressed = ByteArrayOutputStream()
        val header = ByteArrayOutputStream(3)
        val closed = CompletableDeferred<Unit>()
        var receivedHeader = false
        var valid = true
        transport.setServiceHandler(
            serviceCode = serviceCode,
            onData = { payload ->
                if (!receivedHeader) {
                    val needed = 3 - header.size()
                    val headerBytes = needed.coerceAtMost(payload.size)
                    header.write(payload, 0, headerBytes)
                    if (header.size() == 3) {
                        receivedHeader = true
                        valid = header.toByteArray().all { it == 0.toByte() }
                        if (valid && headerBytes < payload.size) {
                            compressed.write(payload, headerBytes, payload.size - headerBytes)
                        }
                    }
                } else if (valid) {
                    compressed.write(payload)
                }
            },
            onClosed = { closed.complete(Unit) },
        )
        return try {
            transport.openService(serviceCode, reliable = true)
            transport.sendServiceData(
                serviceCode,
                GarminByteWriter(6)
                    .writeByte(0)
                    .writeByte(0)
                    .writeShort(fileHandle)
                    .writeByte(0)
                    .writeByte(0)
                    .toBytes(),
            )
            if (withTimeoutOrNull(FILE_TRANSFER_TIMEOUT) { closed.await() } == null || !valid) {
                null
            } else {
                inflate(compressed.toByteArray())
            }
        } finally {
            runCatching { transport.closeService(serviceCode) }
            transport.clearServiceHandler(serviceCode)
        }
    }

    private fun inflate(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        return try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream(bytes.size.coerceAtLeast(1024))
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }

    private fun describe(error: Throwable): String {
        if (error is GarminGattClientException) {
            return error.message ?: "The watch could not be synced."
        }
        val text = error.message ?: error.toString()
        return text.ifBlank { "The watch could not be synced." }
    }

    private class GarminFileSyncException(message: String) : Exception(message)

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

        /** Final safety net for a missing directory response or stalled file. */
        val SYNC_TIMEOUT = 3.minutes
        val FILE_SYNC_REPLY_TIMEOUT = 8.seconds
        val FILE_TRANSFER_TIMEOUT = 90.seconds
        const val MAX_FILE_SYNC_PAGES = 20
        val FILE_TRANSFER_SERVICES = intArrayOf(
            0x2018,
            0x4018,
            0x6018,
            0xA018,
            0xC018,
            0xE018,
        )

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
    .mapNotNull { it.entry.fileDate }
    .minOrNull()
    ?.atZone(zone)
    ?.toLocalDate()
