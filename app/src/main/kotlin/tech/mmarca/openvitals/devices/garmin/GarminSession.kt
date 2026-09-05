package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One downloaded file and the directory entry it came from. */
class GarminDownloadedFile(
    val entry: GarminDirectoryEntry,
    val bytes: ByteArray,
)

/** Progress of a running sync, for the UI. */
data class GarminSyncProgress(
    val phase: GarminSyncPhase,
    val filesTotal: Int = 0,
    val filesDone: Int = 0,
    val currentFile: String? = null,
)

enum class GarminSyncPhase { HANDSHAKE, LISTING, DOWNLOADING, COMPLETE, FAILED }

/**
 * Drives one GFDI sync: handshake, directory listing, downloads, archive.
 * Transport-free: it takes decoded frames and emits frames to send.
 * Ported from Gadgetbridge (AGPLv3) via the Flutter build, read-only.
 */
class GarminSession(
    /** Owns the grace timer. Inject a test scope for virtual time. */
    private val scope: CoroutineScope,
    /** Sends one GFDI frame to the transport. */
    private val send: suspend (ByteArray) -> Unit,
    private val bluetoothName: String,
    private val manufacturer: String,
    private val model: String,
    /** Dedup keys already imported. A stale set only costs airtime. */
    private val alreadySynced: Set<String> = emptySet(),
    private val onProgress: ((GarminSyncProgress) -> Unit)? = null,
    /**
     * Called with each file before it is archived on the watch.
     * If this throws the file is not archived, so the next sync retries it.
     */
    private val onFileDownloaded: (suspend (GarminDownloadedFile) -> Unit)? = null,
    /** How long an empty sync waits for a late SYNCHRONIZATION announcement. */
    private val emptyGrace: Duration = 6.seconds,
    /** Diagnostic: keep decoding and acking frames after the sync ends. */
    private val keepAnsweringAfterSync: Boolean = false,
    /** Called after the capabilities exchange, when the watch accepts requests. */
    private val onHandshakeReady: (() -> Unit)? = null,
    /**
     * The watch still needs the pair-flow trio. OR'd with the capability flag,
     * which the watch advertises only briefly.
     */
    private val setupWizardPending: Boolean = false,
    /** The trio went out; the owner clears its pending flag. */
    private val onSetupWizardCompleted: (() -> Unit)? = null,
    /** Whether the phone's app is in the foreground, read at handshake time. */
    private val hostForeground: (() -> Boolean)? = null,
    /** The watch asked the phone to ring, or to stop. Ringing is the owner's job. */
    private val onFindPhone: ((durationSeconds: Int) -> Unit)? = null,
    private val onFindPhoneCancel: (() -> Unit)? = null,
    /** The watch announced a file this session cannot download. The owner decides. */
    private val onFileAnnounced: ((GarminDirectoryEntry) -> Unit)? = null,
    /** Weather for the watch's ask, or null to ack it and leave it unanswered. */
    private val weatherProvider: (() -> tech.mmarca.openvitals.devices.weather.WeatherSnapshot?)? = null,
    /** Last-known phone position. Without it the watch never fetches weather. */
    private val locationProvider: (() -> GarminPhoneLocation?)? = null,
    /** GPS ephemeris for the watch. Null refuses the ask. */
    private val agpsSource: GarminAgpsSource? = null,
    /**
     * Calendar events in the asked window, or null when sync is off.
     * Off still answers with no events, or the watch re-asks forever.
     */
    private val calendarProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
    /** False for sessions that act rather than collect, such as find-my-watch. */
    private val syncFiles: Boolean = true,
    /**
     * Forwards phone notifications, or null. Null keeps the subscription reply
     * DISABLED. Pass syncFiles = false with it: a file transfer dies when the
     * notification link closes.
     */
    private val notifications: GarminNotificationsHandler? = null,
) {

    /** What the watch can do. Empty until the handshake reaches CONFIGURATION. */
    var capabilities: Set<GarminCapability> = emptySet()
        private set

    /** Protobuf exchanges on the same link. Lazy, so unused sessions pay nothing. */
    val protobuf: GarminProtobufTransport by lazy { GarminProtobufTransport(send = send) }

    private val doneDeferred = CompletableDeferred<List<GarminDownloadedFile>>()

    /** Files fetched this run, handed to the importer when the sync completes. */
    private val downloaded = mutableListOf<GarminDownloadedFile>()

    /** Entries still to fetch, filled from the directory. */
    private val queue = mutableListOf<GarminDirectoryEntry>()

    /** How the watch introduced itself, once it has. */
    var deviceInformation: GarminDeviceInformation? = null
        private set

    /** The file types the watch offered. */
    var supportedTypes: List<GarminSupportedFileType> = emptyList()
        private set

    /** The transfer in flight, or null between files. */
    private var active: ActiveDownload? = null

    /** True once the directory is fetched. */
    private var directoryFetched = false

    private var finished = false
    private var filesTotal = 0

    /** Everything downloaded, or the protocol error that ended the sync. */
    val done: Deferred<List<GarminDownloadedFile>> get() = doneDeferred

    private fun report(phase: GarminSyncPhase, file: String? = null) {
        onProgress?.invoke(
            GarminSyncProgress(
                phase = phase,
                filesTotal = filesTotal,
                filesDone = downloaded.size,
                currentFile = file,
            ),
        )
    }

    /**
     * Answers the watch's HTTP-proxy fetches (weather, ephemeris).
     * The first interceptor to claim a URL wins.
     */
    private val http: GarminHttpProxy? by lazy {
        val interceptors = buildList {
            weatherProvider?.let { add(GarminWeatherInterceptor(it)) }
            agpsSource?.let { add(GarminAgpsInterceptor(it)) }
            // OAuth last: it claims by path alone, across every domain.
            if (isNotEmpty()) add(GarminOauthInterceptor())
        }
        interceptors.takeIf { it.isNotEmpty() }?.let { GarminHttpProxy(it) }
    }

    private val coreLocation: GarminCoreLocation? by lazy {
        locationProvider?.let { GarminCoreLocation(it) }
    }

    /** Always present: an unanswered calendar ask is re-sent forever. */
    private val calendar = GarminCalendarResponder(calendarProvider)

    fun start() {
        run {
            protobuf.onServiceRequest = { requestId, payload ->
                val httpReply = http?.handle(payload)
                    ?: calendar.handle(payload)
                    ?: GarminServiceResponders.handle(payload)
                if (httpReply != null) {
                    scope.launch { protobuf.respond(requestId, httpReply) }
                } else {
                    val coreReply = coreLocation?.handle(payload)
                    if (coreReply != null) {
                        scope.launch {
                            protobuf.respond(requestId, coreReply.payload)
                            coreReply.followUp?.let { followUp ->
                                protobuf.request(
                                    followUp,
                                    label = "location update",
                                    timeout = 5.seconds,
                                )
                            }
                        }
                    }
                }
            }
        }
        report(GarminSyncPhase.HANDSHAKE)
    }

    /** Serialises frame handling. Dispatch suspends on sends, so frames must not interleave. */
    private val mutex = Mutex()

    /** Feeds one decoded frame in. Late frames after completion are ignored. */
    suspend fun handleFrame(frame: GarminGfdiFrame) {
        mutex.withLock { handleFrameSerially(frame) }
    }

    private suspend fun handleFrameSerially(frame: GarminGfdiFrame) {
        if (finished && !keepAnsweringAfterSync) return
        try {
            // Ack first, as Gadgetbridge does, or the watch retransmits.
            // Self-acknowledged types get their own response instead.
            if (frame.messageType !in garminSelfAcknowledgedTypes) {
                send(buildGenericAck(frame.messageType))
            }
            if (protobuf.handleInbound(frame)) return
            dispatch(decodeGarminMessage(frame))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (finished) {
                // Past the sync there is no result to fail, but a listening pass must log this.
                GarminLog.log("[GARMIN-LISTEN] frame ${frame.messageType} threw: $error")
                return
            }
            fail(error)
        }
    }

    private suspend fun dispatch(message: GarminInboundMessage) {
        when (message) {
            is GarminDeviceInformation -> {
                deviceInformation = message
                GarminLog.log(
                    "[GARMIN-SYNC] device ${message.deviceName} " +
                        "${message.deviceModel} sw=${message.softwareVersionText} " +
                        "maxPacket=${message.maxPacketSize}",
                )
                send(
                    buildDeviceInformationResponse(
                        incoming = message,
                        bluetoothName = bluetoothName,
                        manufacturer = manufacturer,
                        model = model,
                    ),
                )
                // The watch serves no files until it knows what we support.
                send(buildSupportedFileTypesRequest())
            }

            is GarminAuthNegotiation -> send(buildAuthNegotiationResponse(message))

            is GarminConfiguration -> {
                // Answer the capabilities exchange; a bare ACK stalls the watch.
                // The bitmap decides whether settings live on the watch or in a FIT file.
                capabilities = GarminCapability.decode(message.capabilityBits)
                GarminLog.log(
                    "[GARMIN-SYNC] configuration: " +
                        "${message.capabilityBits.size}B, " +
                        "${capabilities.size} capabilities",
                )
                GarminLog.log(
                    "[GARMIN-CAPS] ${capabilities.joinToString(", ") { it.wireName }}",
                )
                send(buildConfigurationResponse())
                // Sent on every connection. The weather flag enables the watch's weather feature.
                send(
                    buildDeviceSettings(
                        listOf(
                            GarminDeviceSetting.AUTO_UPLOAD_ENABLED to true,
                            GarminDeviceSetting.WEATHER_CONDITIONS_ENABLED to true,
                            GarminDeviceSetting.WEATHER_ALERTS_ENABLED to false,
                        ),
                    ),
                )
                if (setupWizardPending || GarminCapability.REQUEST_PAIR_FLOW in capabilities) {
                    // Fresh watch on the pairing wizard. Gadgetbridge sends this trio on first connect.
                    GarminLog.log("[GARMIN-SYNC] fresh watch asked for the pair flow; completing setup")
                    send(buildSystemEvent(GarminSystemEventType.PAIR_COMPLETE))
                    send(buildSystemEvent(GarminSystemEventType.SYNC_COMPLETE))
                    send(buildSystemEvent(GarminSystemEventType.SETUP_WIZARD_COMPLETE))
                    onSetupWizardCompleted?.invoke()
                }
                // The clock nudge Gadgetbridge sends on every connection.
                send(buildSystemEvent(GarminSystemEventType.TIME_UPDATED))
                if (hostForeground?.invoke() == true) {
                    GarminLog.log("[GARMIN-SYNC] telling the watch the app is in the foreground")
                    notifyHostForeground(true)
                }
                onHandshakeReady?.invoke()
                // Push weather now: the watch only asks while connected, and links are short.
                // The watch caches what is pushed.
                maybePushWeather()
            }

            is GarminNotificationSubscription -> {
                // Always answered; the watch re-asks until it gets a status.
                // message.enable is the watch's state and drives the handler.
                // The reply is the phone's willingness and must not echo it:
                // a watch never told a phone would forward sends enable=false.
                val handler = notifications
                handler?.setEnabled(enabled = message.enable)
                val willing = handler != null
                GarminLog.log(
                    "[GARMIN-SYNC] notification subscription: watch " +
                        "enable=${message.enable}, replying " +
                        if (willing) "enabled" else "disabled",
                )
                send(buildNotificationSubscriptionStatus(message, enabled = willing))
                // Held announcements go out after the status, so the watch is listening for them.
                handler?.flushHeld()
            }

            is GarminNotificationControl -> {
                // Status before answer. Safe only while no control request can fail.
                send(buildNotificationControlStatus())
                notifications?.handleControl(message)
            }

            is GarminNotificationDataStatus -> notifications?.handleDataStatus(message)

            is GarminSupportedFileTypes -> {
                supportedTypes = message.types
                // The raw pairs are the ground truth for which file type codes a watch offers.
                GarminLog.log(
                    "[GARMIN-SYNC] watch supports ${message.types.size} types: " +
                        message.types.joinToString(", ") {
                            "${it.dataType}/${it.subType}:${it.name}"
                        },
                )
                send(buildSystemEvent(GarminSystemEventType.SYNC_READY))
                // Filter before listing: an unfiltered listing came back empty on a watch
                // that held sleep data.
                if (!syncFiles) return
                send(buildFilterMessage())
                report(GarminSyncPhase.LISTING)
                requestDirectory()
            }

            is GarminDownloadRequestStatus -> onDownloadStatus(message)

            is GarminFileTransferData -> onFileChunk(message)

            is GarminSynchronization -> {
                // The watch announcing what it holds. Filter, then list, as Gadgetbridge does.
                GarminLog.log(
                    "[GARMIN-SYNC] synchronization type=${message.syncType} " +
                        "bits=${message.setBits} proceed=${message.shouldProceed}",
                )
                if (message.shouldProceed) {
                    // The watch holds something: cancel the give-up and re-read the listing.
                    if (!syncFiles) return
                    graceJob?.cancel()
                    graceJob = null
                    send(buildFilterMessage())
                    directoryFetched = false
                    requestDirectory()
                }
            }

            is GarminGenericStatus -> {
                // A NAK is the only visible sign the watch rejected a request.
                if (message.status != GarminStatus.ACK) {
                    GarminLog.log(
                        "[GARMIN-SYNC] NAK ${message.status.name} for " +
                            "message ${message.originalMessageType}",
                    )
                    return
                }
                if (message.originalMessageType == GarminMessageId.FILTER) {
                    GarminLog.log("[GARMIN-SYNC] filter accepted")
                }
                if (message.originalMessageType == GarminMessageId.FIT_DEFINITION) {
                    val data = pendingWeatherData ?: return
                    pendingWeatherData = null
                    GarminLog.log("[GARMIN-WEATHER] definitions accepted; sending records")
                    send(GarminGfdiFrame.build(GarminMessageId.FIT_DATA, data))
                }
            }

            is GarminCurrentTimeRequest -> {
                // This app is the watch's only clock source: reply with time, zone and DST.
                GarminLog.log("[GARMIN-SYNC] watch asked for the time")
                send(buildCurrentTimeResponse(referenceId = message.referenceId))
            }

            is GarminWeatherRequest -> {
                val weather = weatherProvider?.invoke()
                if (weather == null) {
                    GarminLog.log("[GARMIN-WEATHER] watch asked; nothing fresh to serve")
                    return
                }
                GarminLog.log(
                    "[GARMIN-WEATHER] watch asked (format=${message.format}, " +
                        "${message.hoursOfForecast}h); sending " +
                        "${weather.hourly.size}h/${weather.daily.size}d " +
                        "for \"${weather.location}\"",
                )
                pendingWeatherData = GarminFitWeather.dataPayload(weather)
                send(
                    GarminGfdiFrame.build(
                        GarminMessageId.FIT_DEFINITION,
                        GarminFitWeather.definitionPayload(),
                    ),
                )
            }

            is GarminFindMyPhoneRequest -> {
                GarminLog.log(
                    "[GARMIN-SYNC] find-my-phone for ${message.durationSeconds}s",
                )
                onFindPhone?.invoke(message.durationSeconds)
            }

            is GarminFindMyPhoneCancel -> {
                GarminLog.log("[GARMIN-SYNC] find-my-phone cancelled from the watch")
                onFindPhoneCancel?.invoke()
            }

            is GarminFileAvailable -> onFileAvailable(message.entry)

            is GarminUnhandledMessage -> {
                // Log unexpected chatter: it is the evidence a stalled sync needs.
                // Whole while listening, truncated otherwise.
                GarminLog.logLazy {
                    "[GARMIN-SYNC] unhandled message " +
                        "${message.messageType} (${message.payload.size}B) " +
                        hex(message.payload, max = if (keepAnsweringAfterSync) 512 else 32)
                }
            }
        }
    }

    private suspend fun requestDirectory() {
        active = ActiveDownload(
            entry = GarminDirectoryEntry(
                fileIndex = 0,
                type = GarminFileType.DIRECTORY,
                fileNumber = 0,
                specificFlags = 0,
                fileFlags = 0,
                fileSize = 0,
                fileDate = null,
            ),
        )
        send(buildDownloadRequest(fileIndex = 0))
    }

    private suspend fun onDownloadStatus(status: GarminDownloadRequestStatus) {
        val current = active ?: return // Transfer already abandoned.

        if (!status.canProceed) {
            GarminLog.log(
                "[GARMIN-SYNC] download refused for index " +
                    "${current.entry.fileIndex}: ${status.downloadStatus.name}",
            )
            // One unreadable file must not end the sync.
            active = null
            next()
            return
        }
        current.begin(status.maxFileSize)
        // A zero-length file completes as soon as its size is known.
        if (current.isComplete) {
            active = null
            onFileComplete(current)
        }
    }

    private suspend fun onFileChunk(chunk: GarminFileTransferData) {
        val current = active ?: return

        val appended = current.append(chunk)
        if (!appended) {
            // CRC or offset mismatch: abandon this file, keep the sync.
            GarminLog.log(
                "[GARMIN-SYNC] chunk rejected for index " +
                    "${current.entry.fileIndex}; skipping file",
            )
            active = null
            next()
            return
        }

        send(buildFileTransferDataAck(current.received))
        if (!current.isComplete) return

        active = null
        onFileComplete(current)
    }

    private suspend fun onFileComplete(finishedDownload: ActiveDownload) {
        val bytes = finishedDownload.bytes

        if (finishedDownload.entry.type == GarminFileType.DIRECTORY) {
            directoryFetched = true
            val listing = GarminDirectory.parseWithDiagnostics(bytes)
            // Skip what a previous sync imported. A null dedup key is always fetched.
            // Never re-send the archive flag for a held file: the key may collide
            // and drop a file nobody downloaded.
            val fresh = listing.entries.filter { entry ->
                val key = entry.dedupKey
                key == null || key !in alreadySynced
            }
            queue.clear()
            queue.addAll(fresh)
            filesTotal = fresh.size
            // Raw counts and rejected type codes tell the causes of "0 files" apart.
            GarminLog.log(
                "[GARMIN-SYNC] directory ${bytes.size}B " +
                    "${listing.describe()} new=${fresh.size}",
            )
            if (listing.entries.isEmpty() && bytes.isNotEmpty()) {
                // The raw listing separates "nothing" from "answers elsewhere".
                GarminLog.logLazy { "[GARMIN-SYNC] raw directory: ${hex(bytes)}" }
            }
            report(GarminSyncPhase.DOWNLOADING)
            next()
            return
        }

        val file = GarminDownloadedFile(entry = finishedDownload.entry, bytes = bytes)
        downloaded.add(file)
        GarminLog.log(
            "[GARMIN-SYNC] got ${finishedDownload.entry.type.label} " +
                "index=${finishedDownload.entry.fileIndex} bytes=${bytes.size}",
        )

        // Persist first, archive second. Archiving is irreversible.
        var safeToArchive = true
        val keep = onFileDownloaded
        if (keep != null) {
            try {
                keep(file)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                safeToArchive = false
                GarminLog.log(
                    "[GARMIN-SYNC] not archiving index=" +
                        "${finishedDownload.entry.fileIndex}: could not keep a copy ($error)",
                )
            }
        }
        if (safeToArchive) {
            send(buildSetFileFlags(finishedDownload.entry.fileIndex, GarminFileFlag.ARCHIVE))
        }
        next()
    }

    /**
     * Tells the watch whether the app is in the foreground. The watch defers
     * online errands, weather included, until it believes someone is listening.
     */
    suspend fun notifyHostForeground(foreground: Boolean) {
        send(
            buildSystemEvent(
                if (foreground) {
                    GarminSystemEventType.HOST_DID_ENTER_FOREGROUND
                } else {
                    GarminSystemEventType.HOST_DID_ENTER_BACKGROUND
                },
            ),
        )
    }

    /**
     * Pushes weather after the capabilities exchange. The records wait for
     * the watch's status on the definitions.
     */
    private suspend fun maybePushWeather() {
        // Only watches with the capability have a weather glance; others NAK.
        if (GarminCapability.WEATHER_CONDITIONS !in capabilities) return
        val weather = weatherProvider?.invoke() ?: return
        GarminLog.log(
            "[GARMIN-WEATHER] pushing ${weather.hourly.size}h/" +
                "${weather.daily.size}d for \"${weather.location}\"",
        )
        pendingWeatherData = GarminFitWeather.dataPayload(weather)
        send(
            GarminGfdiFrame.build(
                GarminMessageId.FIT_DEFINITION,
                GarminFitWeather.definitionPayload(),
            ),
        )
    }

    /**
     * A file announced mid-session. Pulled now while the sync is live,
     * otherwise left for the next sync.
     */
    private suspend fun onFileAvailable(entry: GarminDirectoryEntry) {
        if (finished || !syncFiles) {
            GarminLog.log(
                "[GARMIN-LISTEN] watch announced ${entry.type.label} " +
                    "index=${entry.fileIndex}",
            )
            onFileAnnounced?.invoke(entry)
            return
        }
        val key = entry.dedupKey
        if (key != null && key in alreadySynced) {
            GarminLog.log(
                "[GARMIN-SYNC] announced ${entry.type.label} already held; not downloading",
            )
            return
        }
        if (queue.any { it.fileIndex == entry.fileIndex } ||
            active?.entry?.fileIndex == entry.fileIndex
        ) {
            return
        }
        GarminLog.log(
            "[GARMIN-SYNC] watch announced ${entry.type.label} index=${entry.fileIndex}",
        )
        queue.add(entry)
        filesTotal += 1
        // This is what the grace wait was for; the timer must not seal the result mid-download.
        graceJob?.cancel()
        graceJob = null
        if (directoryFetched && active == null) {
            next()
        }
    }

    /** Starts the next queued file, or finishes the sync when the queue is empty. */
    private suspend fun next() {
        if (!directoryFetched) return // Still waiting on the listing.
        if (queue.isEmpty()) {
            complete()
            return
        }
        val entry = queue.removeAt(0)
        active = ActiveDownload(entry = entry)
        report(GarminSyncPhase.DOWNLOADING, file = entry.type.label)
        send(buildDownloadRequest(fileIndex = entry.fileIndex))
    }

    /**
     * Weather records waiting for the watch to accept the definitions.
     * Sent back-to-back, the watch dropped them.
     */
    private var pendingWeatherData: ByteArray? = null

    private var graceJob: Job? = null
    private var graceUsed = false

    private suspend fun complete() {
        if (finished) return
        if (downloaded.isEmpty() && !graceUsed) {
            graceUsed = true
            GarminLog.log(
                "[GARMIN-SYNC] nothing listed; waiting " +
                    "${emptyGrace.inWholeSeconds}s in case the watch announces",
            )
            // Settle the result here either way, or a failure would leave [done] pending.
            graceJob = scope.launch {
                delay(emptyGrace)
                graceJob = null // Fired: finish() must not cancel this job under itself.
                mutex.withLock {
                    try {
                        finish()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        GarminLog.log(
                            "[GARMIN-SYNC] could not close out an empty sync: $error",
                        )
                        finished = true
                        doneDeferred.complete(downloaded.toList())
                    }
                }
            }
            return
        }
        finish()
    }

    private suspend fun finish() {
        if (finished) return
        finished = true
        graceJob?.cancel()
        graceJob = null
        send(buildSystemEvent(GarminSystemEventType.SYNC_COMPLETE))
        report(GarminSyncPhase.COMPLETE)
        GarminLog.log("[GARMIN-SYNC] complete: ${downloaded.size} files")
        if (keepAnsweringAfterSync) {
            GarminLog.log("[GARMIN-LISTEN] sync done; still answering the watch")
        }
        doneDeferred.complete(downloaded.toList())
    }

    private fun fail(error: Exception) {
        if (finished) return
        finished = true
        report(GarminSyncPhase.FAILED)
        GarminLog.log("[GARMIN-SYNC] failed: $error")
        doneDeferred.completeExceptionally(error)
    }

    /** Ends the sync early. What was already downloaded is still returned. */
    fun abort(reason: Any? = null) {
        protobuf.abort()
        if (finished) return
        finished = true
        graceJob?.cancel()
        graceJob = null
        report(GarminSyncPhase.FAILED)
        GarminLog.log("[GARMIN-SYNC] aborted: ${reason ?: "no reason given"}")
        doneDeferred.complete(downloaded.toList())
    }

    private companion object {
        /** Bytes as hex, capped to keep logs short. */
        fun hex(bytes: ByteArray, max: Int = 256): String {
            val shown = if (bytes.size > max) bytes.copyOf(max) else bytes
            val text = shown.joinToString(" ") {
                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
            return if (bytes.size > max) "$text … (+${bytes.size - max}B)" else text
        }
    }
}

/** One file being received: expected size, bytes so far, running CRC. */
private class ActiveDownload(val entry: GarminDirectoryEntry) {

    private var size = 0L
    private var runningCrc = 0

    /** Whether the size is known. Zero is a valid size, so [size] alone cannot tell. */
    private var begun = false

    private val data = ByteArrayOutputStream()

    val received: Int get() = data.size()
    val isComplete: Boolean get() = begun && received >= size
    val bytes: ByteArray get() = data.toByteArray()

    /** The download status carries the real size; the directory's is not authoritative. */
    fun begin(newSize: Long) {
        begun = true
        size = newSize
    }

    /** Appends a chunk after checking offset and CRC. False means skip this file. */
    fun append(chunk: GarminFileTransferData): Boolean {
        if (chunk.dataOffset != received.toLong()) return false
        val crc = GarminCrc.compute(chunk.data, initialCrc = runningCrc)
        if (crc != chunk.crc) return false
        runningCrc = crc
        data.write(chunk.data, 0, chunk.data.size)
        return true
    }
}
