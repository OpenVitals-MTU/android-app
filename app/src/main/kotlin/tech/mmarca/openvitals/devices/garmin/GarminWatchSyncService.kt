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
 * Drives one end-to-end sync with a Garmin watch: take the radio lease, open
 * the link, run the GFDI session, import what it downloaded, remember what was
 * taken and stamp the device.
 *
 * The Garmin implementation of [DeviceSyncPort], folding together the Flutter
 * build's `garmin_watch_sync_service.dart` (radio + protocol) and
 * `garmin_device_sync_port.dart` (the app-level sequence). Everything below it
 * is radio-free and unit-tested; everything above it deals in registered
 * devices and knows nothing about handles or COBS.
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
     * Outlives one sync on purpose: the chain walk has its own time budget and
     * a user who closes the sync screen the moment it says "done" must not
     * cancel the rebuild their sync just made necessary.
     */
    private val rebuildScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Keeps a copy of every download before the watch is told to archive it.
     * Archiving stops the watch ever offering the file again, so without this
     * an importer bug would make the data unrecoverable.
     */
    private val fileStore = GarminFileStore(
        resolveDirectory = { File(context.filesDir, FILE_STORE_DIRECTORY) },
    )

    init {
        // Protocol logging for the whole GFDI stack. Idempotent, and a no-op
        // outside debug builds — the redaction policy lives in [GarminLog].
        GarminLog.installLogcatSink()
    }

    /** A watch OR an Edge bike computer — both pull recorded FIT files over GFDI. */
    override fun canSync(device: BleSensorDevice): Boolean = device.isGarminGfdi

    override suspend fun sync(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): DeviceSyncResult {
        // A live activity recording holds the app's foreground slot AND the
        // radio discipline — refuse to sync until it is finished or discarded,
        // the same gate the phone-to-phone sync applies.
        if (recordingController.state.value.isActive) {
            return DeviceSyncResult.Failed(
                "An activity recording is in progress. Finish or discard it " +
                    "before syncing the watch.",
            )
        }

        val downloaded = try {
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

        if (downloaded.isNotEmpty()) {
            try {
                importer.import(downloaded)
                // Recorded exercises take the same path a hand-picked FIT
                // folder does. Per-file failures are tolerated inside the
                // importer (the raw bytes stay in the file store as the safety
                // net), so this cannot fail the sync the wellness import
                // survived.
                activityImporter.import(downloaded)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // The importer tolerates a bad FILE internally, so reaching
                // here means the write path itself is unavailable (no Health
                // Connect, permissions revoked mid-run).
                GarminLog.log("[GARMIN-SYNC] import failed: $error")
                // Neither the keys nor the sync stamp are written: nothing
                // reached Health Connect, so the next run must fetch these
                // files again.
                return DeviceSyncResult.Failed(describe(error))
            }

            // Recorded AFTER the import, so a run that died mid-import
            // re-downloads rather than skipping files that never reached
            // Health Connect. Files with no stable key are not recorded — they
            // are re-fetched every sync by design rather than skipped on a key
            // that identifies nothing.
            stateStore.recordSyncedFileKeys(
                device.id,
                downloaded.mapNotNull { it.entry.dedupKey },
            )

            refreshBodyEnergy(downloaded)
        }

        bleDeviceRepository.markSynced(device.id, Instant.now())
        // The tiles read stored data, and a sync that just landed a night of
        // sleep must not leave the home screen on its pre-sync numbers until
        // the system's next periodic tick — which Doze can defer for hours.
        refreshPlacedHomeWidgets(context)
        return DeviceSyncResult.Succeeded(downloaded.size)
    }

    /**
     * Drops the Body Energy days this sync just back-filled, and rebuilds them.
     *
     * Body Energy chains across midnight, so a watch handing over a week of
     * sleep and heart-rate data invalidates not just those days but every day
     * after them — their seeds came from scores computed without it. The
     * chain's settling window is a time-based guess at "data might still
     * arrive"; a completed sync is the precise signal, so use it. Without this
     * a back-filled day older than the window stays frozen at whatever it
     * scored before the watch was ever synced, permanently: the staleness rule
     * stops revisiting a day once it is settled.
     *
     * Best-effort. The watch data has already reached Health Connect, and
     * failing the sync over a cache rebuild would tell the user their sync
     * failed when it did not.
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

        // Rebuild rather than leave holes: a foreground open only fills two
        // missing days inline, so a week of back-fill would otherwise seed
        // today neutrally until the background pass caught up. Forced past the
        // throttle because a sync is precisely the event it should react to,
        // and off the sync's own coroutine because the walk has its own budget
        // — the Body Energy screen joins this same run when it opens.
        rebuildScope.launch {
            runCatching { bodyEnergyChainSync.syncAll(force = true) }
                .onFailure { GarminLog.log("[GARMIN-SYNC] body-energy rebuild failed: $it") }
            // Only now teach the gains. The fit asks "where did the watch and
            // the model disagree", and a model that has not yet seen the sleep
            // this sync just delivered disagrees for a reason that is not a
            // mis-set gain — it would learn from an artefact of when data
            // arrived. Flutter fits before the rebuild and takes that on;
            // ordering it after costs nothing, since both are already
            // fire-and-forget, and the watch evidence keeps until the next run
            // if the rebuild fails.
            runCatching { fitBodyEnergyFromWatch() }
                .onFailure { GarminLog.log("[GARMIN-SYNC] body-energy calibration skipped: $it") }
        }
    }

    /**
     * Makes the watch at [address] alert, and keeps the link open while it
     * does. Port of the Flutter build's `GarminWatchSyncService.findWatch`.
     *
     * Find is a TOGGLE with a timeout, not a one-shot: the watch alerts for
     * [timeout] unless cancelled, so the link has to stay open for the
     * duration — a sync closes it in about a second, which would end the
     * alert with it. Completing [cancelled] stops the alert early.
     *
     * Returns whether the watch ACCEPTED the request. That is not the same as
     * "the watch is ringing": the protocol answers OK (100) or ERROR (200),
     * and only the first means anything happened.
     *
     * Throws [RadioLeaseBusyException] when something else holds the radio,
     * and [GarminGattClientException] when the watch cannot be reached.
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
        var transport: GarminMlTransport? = null
        val ready = CompletableDeferred<Unit>()
        // The watch narrates a find it has ended itself — dismissed on the
        // wrist, or run out. Without listening for that, the phone holds
        // "Stop" for the full minute after the alert has already stopped,
        // which is what a real wearer hit first.
        val endedOnWatch = CompletableDeferred<Unit>()
        val session = GarminSession(
            scope = this,
            send = { frame ->
                (transport ?: throw GarminGattClientException("Not connected")).sendFrame(frame)
            },
            bluetoothName = phone.bluetoothName,
            manufacturer = phone.manufacturer,
            model = phone.model,
            // A session opened to DO something, not to collect something —
            // dragging a file sync along would die mid-transfer when the link
            // closes under it.
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
            // The watch ignores anything sent before it has finished
            // introducing itself, so wait for the handshake rather than
            // racing it.
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
            // Only an explicit ERROR is a refusal. A reply this app cannot
            // read is NOT: the watch was seen ringing while an unparsed reply
            // was being treated as failure, and bailing here is what left it
            // ringing with no way to stop it.
            if (outcome.declined) return@coroutineScope false

            ringing = true
            // Hold the link for the alert, or until the user stops it or the
            // watch reports it over.
            withTimeoutOrNull(timeout) {
                select<Unit> {
                    endedOnWatch.onAwait { }
                    cancelled?.onAwait { }
                }
            }
            true
        } finally {
            // ALWAYS cancel a started alert, on every path out — including a
            // thrown error and a user who backed out. A buzzing watch that the
            // phone has forgotten about is the one outcome worth writing code
            // to prevent.
            if (ringing) {
                try {
                    // NonCancellable: the cancel must still go out when this
                    // coroutine is itself being torn down — that teardown is
                    // exactly the "user backed out" path.
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
     * The radio half: connect, run the GFDI session to completion, hand back
     * whatever it downloaded.
     *
     * Throws [GarminGattClientException] when the watch cannot be reached or
     * speaks a transport this app does not implement. A link that drops
     * mid-sync is NOT an error: the session aborts and returns what it already
     * has, because a night of sleep already on the phone should not be thrown
     * away because the user walked out of range.
     */
    private suspend fun pullFiles(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): List<GarminDownloadedFile> = coroutineScope {
        val client = GarminGattClient(context, device.address)
        var transport: GarminMlTransport? = null
        val handshakeReady = CompletableDeferred<Unit>()
        val session = GarminSession(
            scope = this,
            // Bound after the transport opens; the lookup is deferred so the
            // session can be constructed first and wired in one place.
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
            keepAnsweringAfterSync = listenAfter > Duration.ZERO,
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

        // Frames land on the binder thread as they arrive; a channel keeps
        // their order while handing them to the session's suspending handler.
        val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
        val pump = launch { for (frame in frames) session.handleFrame(frame) }
        // A dropped link ends the sync with what it has rather than hanging on
        // `done` forever waiting for frames that will never arrive.
        val dropWatch = launch { client.onDisconnected.collect { session.abort(it) } }
        try {
            // Logged BEFORE connecting: a sync that wedged inside connect
            // produced no output whatsoever, which read as "nothing happened"
            // rather than "stuck on the radio".
            GarminLog.log("[GARMIN-SYNC] connecting to the watch")
            transport = client.connect(onFrame = { frame -> frames.trySend(frame) })
            session.start()
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
            val files = session.done.await()
            // Bounded: the sync result must not wait on a battery answer that
            // is not coming.
            withTimeoutOrNull(BATTERY_TIMEOUT) { batteryJob.join() }
            batteryJob.cancel()
            stateStore.recordCapabilities(device.id, session.capabilities)
            if (listenAfter > Duration.ZERO) {
                // Diagnostic pass: the sync itself takes about a second, so
                // holding the link open is the only way to see what the watch
                // sends unprompted. Whatever arrives is logged by the session.
                GarminLog.log(
                    "[GARMIN-LISTEN] holding the link open for " +
                        "${listenAfter.inWholeMinutes}m — touch the watch now",
                )
                delay(listenAfter)
                GarminLog.log("[GARMIN-LISTEN] window closed")
            }
            files
        } finally {
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("[GARMIN-SYNC] link closed")
        }
    }

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

        /** A best-effort cancel must not hold the link hostage. */
        val FIND_CANCEL_TIMEOUT = 3.seconds
        val BATTERY_TIMEOUT = 5.seconds
    }
}

/**
 * The earliest local day a set of downloaded watch files carries data for.
 *
 * Null when the watch dated none of them — its "no date" sentinel is real and
 * observed on a vívoactive 5 — in which case nothing is invalidated and the
 * chain's settling window stays the only safety net, which is what it is for.
 *
 * Top-level and pure so it can be tested without the sync service, which needs
 * a radio, a lease and a live GFDI session to build.
 */
fun garminEarliestAffectedDay(
    downloaded: List<GarminDownloadedFile>,
    zone: ZoneId = ZoneId.systemDefault(),
): LocalDate? = downloaded
    .mapNotNull { it.entry.fileDate }
    .minOrNull()
    ?.atZone(zone)
    ?.toLocalDate()
