package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncPhase
import tech.mmarca.openvitals.devices.core.sync.DeviceSyncProgress
import tech.mmarca.openvitals.devices.weather.WeatherStore
import tech.mmarca.openvitals.domain.model.BleSensorDevice

/** What a pull landed, and why it stopped early if it did. */
data class GarminPullResult(
    val files: List<GarminDownloadedFile>,
    val incompleteReason: String? = null,
)

/**
 * The radio half of a Garmin sync: open a link, run a GFDI session, hand back
 * what it landed. Everything above it is radio-free, so a test can drive a whole
 * sync by handing the service one of these.
 */
interface GarminRadio {
    /** Connects, syncs, and returns the files. Throws [GarminGattClientException] out of range. */
    suspend fun pull(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): GarminPullResult

    /** Makes the watch alert. Returns whether it accepted. */
    suspend fun find(
        address: String,
        timeout: Duration,
        cancelled: CompletableDeferred<Unit>?,
    ): Boolean

    /** Sends one file on a link opened for it. Never throws for a watch that is away. */
    suspend fun upload(
        device: BleSensorDevice,
        type: GarminUploadFileType,
        bytes: ByteArray,
        onStage: (GarminSendStage) -> Unit,
    ): GarminSendFileResult
}

/** The real radio. The one class here that opens a Bluetooth link. */
@Singleton
class GarminGattRadio @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleDeviceRepository: BleDeviceRepository,
    private val stateStore: GarminDeviceStateStore,
    private val fileStore: GarminFileStore,
    private val findPhoneRinger: GarminFindPhoneRinger,
    private val weatherStore: WeatherStore,
    private val locationSource: GarminPhoneLocationSource,
    private val agpsStore: GarminAgpsStore,
    private val calendarSource: GarminCalendarSource,
) : GarminRadio {

    private val phone = GarminPhoneIdentity()

    /**
     * A session's coroutines run one at a time. [GarminProtobufTransport] keeps plain maps
     * that the frame pump and the requests share, and `abort()` comes from a third coroutine.
     * The callers arrive on the default dispatcher, where those three ran in parallel.
     */
    private val sessionDispatcher = Dispatchers.Default.limitedParallelism(1)

    private suspend fun <T> onSessionThread(block: suspend CoroutineScope.() -> T): T =
        withContext(sessionDispatcher) { coroutineScope(block) }

    /**
     * Connect, run the GFDI session, return what it downloaded. Throws
     * [GarminGattClientException] when the watch cannot be reached. A dropped
     * link is not an error: the session returns what it already has.
     */
    override suspend fun pull(
        device: BleSensorDevice,
        listenAfter: Duration,
        onProgress: ((DeviceSyncProgress) -> Unit)?,
    ): GarminPullResult = onSessionThread {
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
            onFileDownloaded = { file -> fileStore.save(file, now = Instant.now(), deviceId = device.id) },
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
                                keep = { file -> fileStore.save(file, now = Instant.now(), deviceId = device.id) },
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
            GarminPullResult(files = files, incompleteReason = legacyReason ?: fallbackReason)
        } finally {
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("[GARMIN-SYNC] link closed")
        }
    }

    override suspend fun find(
        address: String,
        timeout: Duration,
        cancelled: CompletableDeferred<Unit>?,
    ): Boolean = onSessionThread {
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
                return@onSessionThread false
            }

            val reply = session.protobuf.request(
                GarminFindMyWatch.start(timeout),
                label = "find start",
            )
            val outcome = GarminFindMyWatch.outcome(reply)
            GarminLog.log("[GARMIN-FIND] ${outcome.name}")
            // Only an explicit ERROR is a refusal. An unreadable reply is not:
            // the watch was seen ringing while one was treated as failure.
            if (outcome.declined) return@onSessionThread false

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

    override suspend fun upload(
        device: BleSensorDevice,
        type: GarminUploadFileType,
        bytes: ByteArray,
        onStage: (GarminSendStage) -> Unit,
    ): GarminSendFileResult {
        val result = withOneShotSession(device.address, tag = "[GARMIN-SEND]") { session ->
            // Free here, and a watch that never synced has no list yet.
            stateStore.recordCapabilities(device.id, session.capabilities)
            onStage(GarminSendStage.SENDING)
            session.uploads.upload(type, bytes, session.deviceInformation?.maxPacketSize).also {
                // Closing at once could cut off the closing system event.
                if (it == GarminUploadResult.Sent) delay(SEND_FLUSH_DELAY)
            }
        }
        return when (result) {
            null -> GarminSendFileResult.HandshakeTimeout
            GarminUploadResult.Sent -> GarminSendFileResult.Sent
            is GarminUploadResult.Refused -> GarminSendFileResult.Refused(result.reason)
            is GarminUploadResult.NoAnswer -> GarminSendFileResult.NoAnswer
            GarminUploadResult.LinkLost -> GarminSendFileResult.LinkLost
            GarminUploadResult.Busy -> GarminSendFileResult.SyncRunning
        }
    }

    /**
     * Opens a link for one short exchange and always closes it. [body] runs
     * once the handshake is done. Null when the watch never finished it.
     */
    private suspend fun <T : Any> withOneShotSession(
        address: String,
        tag: String,
        body: suspend (GarminSession) -> T,
    ): T? = onSessionThread {
        val client = GarminGattClient(context, address)
        var transport: GarminFrameTransport? = null
        val ready = CompletableDeferred<Unit>()
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
        val frames = Channel<GarminGfdiFrame>(Channel.UNLIMITED)
        val pump = launch { for (frame in frames) session.handleFrame(frame) }
        val dropWatch = launch { client.onDisconnected.collect { session.abort(it) } }
        try {
            transport = client.connect(onFrame = { frame -> frames.trySend(frame) })
            session.start()
            // The watch ignores anything sent before the handshake finishes.
            if (withTimeoutOrNull(HANDSHAKE_TIMEOUT) { ready.await() } == null) {
                GarminLog.log("$tag the watch never finished its handshake")
                return@onSessionThread null
            }
            body(session)
        } finally {
            dropWatch.cancel()
            pump.cancel()
            frames.close()
            client.close()
            GarminLog.log("$tag link closed")
        }
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
        /** How long a watch gets to finish its handshake before a find gives up. */
        val HANDSHAKE_TIMEOUT = 15.seconds

        /** A whole-sync safety net; the session's stage timers fail sooner. */
        val SYNC_TIMEOUT = 3.minutes

        /** A best-effort cancel must not hold the link hostage. */
        val FIND_CANCEL_TIMEOUT = 3.seconds

        /** Lets the last frame leave before the link closes. */
        val SEND_FLUSH_DELAY = 1.seconds

        /** The sync result must not wait on a battery answer that is not coming. */
        val BATTERY_TIMEOUT = 5.seconds
    }
}
