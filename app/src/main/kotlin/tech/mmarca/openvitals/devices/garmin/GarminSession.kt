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
 * Drives one GFDI sync: handshake, list the directory, download each wanted
 * file, archive it, finish.
 *
 * **Transport-free by construction.** It consumes decoded GFDI frames and
 * emits frames to send, so it is exercised end to end over an in-memory pipe
 * with no Bluetooth. The ML/BLE layer below it only moves bytes.
 *
 * Ported from Gadgetbridge's `GarminSupport` + `FileTransferHandler`
 * (AGPLv3), via the Flutter build's `garmin_session.dart`, narrowed to a
 * read-only sync: no uploads, no realtime. Dart streams and timers became
 * coroutines: frame handling is serialised by a [Mutex] instead of a future
 * chain, the grace timer is a [Job] on the injected [scope], and [done] is a
 * [Deferred].
 */
class GarminSession(
    /**
     * Owns the empty-sync grace timer. Inject the caller's scope (a test's
     * `TestScope`) so delayed work stays cancellable and virtual-time
     * testable.
     */
    private val scope: CoroutineScope,
    /**
     * Hands one built GFDI frame to the transport below. The session never
     * sees COBS, handles or characteristics — that is the ML/BLE layer's job.
     */
    private val send: suspend (ByteArray) -> Unit,
    private val bluetoothName: String,
    private val manufacturer: String,
    private val model: String,
    /**
     * Dedup keys ([GarminDirectoryEntry.dedupKey]) already imported by a
     * previous sync. Purely a bandwidth optimisation — Health Connect's
     * `clientRecordId` already makes a re-import idempotent — so a stale set
     * costs airtime, never correctness.
     */
    private val alreadySynced: Set<String> = emptySet(),
    private val onProgress: ((GarminSyncProgress) -> Unit)? = null,
    /**
     * Called with each completed file BEFORE it is archived on the watch.
     *
     * Archiving is destructive: once flagged, the watch never offers that
     * file again. So if this throws, the file is deliberately NOT archived —
     * better to re-download it next sync than to lose it because the copy
     * that was supposed to outlive the download never landed.
     */
    private val onFileDownloaded: (suspend (GarminDownloadedFile) -> Unit)? = null,
    /**
     * How long to keep a fruitless sync open before giving up.
     *
     * The watch may announce what it holds (SYNCHRONIZATION) a moment after
     * the listing is served. Finishing the instant an empty directory arrives
     * races that announcement and throws it away, which looks identical to
     * "the watch has nothing". Injectable so tests need not wait it out.
     */
    private val emptyGrace: Duration = 6.seconds,
    /**
     * Diagnostic only: keep decoding and acknowledging what the watch sends
     * after the sync has finished, instead of ignoring it.
     *
     * A sync lasts about a second, so anything the watch volunteers on its
     * own schedule — or in response to being touched — lands long after the
     * session is done and is normally dropped on the floor by
     * [handleFrameSerially]. Acknowledging still matters while listening: an
     * unanswered message is retransmitted on a timer and eventually takes the
     * link down with it.
     */
    private val keepAnsweringAfterSync: Boolean = false,
    /**
     * Called once the capabilities exchange is answered — the first moment
     * the watch will accept anything this app initiates.
     *
     * Earlier than that it is still introducing itself and drops what it is
     * sent; later would mean waiting for a whole file sync to finish.
     */
    private val onHandshakeReady: (() -> Unit)? = null,
    /**
     * The watch was just onboarded and still needs the pair-flow completion
     * trio. OR'd with the REQUEST_PAIR_FLOW capability, because the watch
     * only advertises that flag for its first minutes on the wizard screen —
     * a session arriving later would miss it.
     */
    private val setupWizardPending: Boolean = false,
    /** The trio went out; the owner clears its pending flag. */
    private val onSetupWizardCompleted: (() -> Unit)? = null,
    /** Whether the phone's app is in the foreground, read at handshake time. */
    private val hostForeground: (() -> Boolean)? = null,
    /**
     * The watch asked the phone to ring ([GarminFindMyPhoneRequest]), and to
     * stop. Callbacks rather than behaviour: ringing is an app concern, and a
     * session without them (settings, a plain sync) still acks the request.
     */
    private val onFindPhone: ((durationSeconds: Int) -> Unit)? = null,
    private val onFindPhoneCancel: (() -> Unit)? = null,
    /**
     * The watch announced a freshly written file to a session that cannot
     * download it (a held link, or a sync already sealed). The owner decides
     * — the companion bridge starts a background sync, which takes the radio
     * over the normal lease handoff.
     */
    private val onFileAnnounced: ((GarminDirectoryEntry) -> Unit)? = null,
    /**
     * The weather to serve when the watch asks ([GarminWeatherRequest]), or
     * null when there is none fresh enough — the ask is then acked and left
     * unanswered, exactly as a phone with no weather app would.
     */
    private val weatherProvider: (() -> tech.mmarca.openvitals.devices.weather.WeatherSnapshot?)? = null,
    /**
     * The phone's last-known position, for the watch's `CoreService` location
     * asks. The prerequisite for weather: with no answer the glance never
     * even attempts its fetch.
     */
    private val locationProvider: (() -> GarminPhoneLocation?)? = null,
    /**
     * GPS ephemeris to hand the watch when it asks. Null leaves the ask
     * refused, which is what a phone with no ephemeris file has to say.
     */
    private val agpsSource: GarminAgpsSource? = null,
    /**
     * The calendar events overlapping the watch's asked window, or null when
     * calendar sync is off for this watch. Off still ANSWERS — an OK with no
     * events — because the watch re-asks an unanswered request forever.
     */
    private val calendarProvider: ((beginEpochSeconds: Long, endEpochSeconds: Long) -> List<GarminCalendarEvent>?)? = null,
    /**
     * Whether to pull files at all.
     *
     * False for a session opened to DO something rather than to collect
     * something — finding the watch, say. Without this the find session
     * dragged a full sync along behind it and then failed mid-transfer when
     * the link closed under it, which is noise at best and a lost file at
     * worst.
     */
    private val syncFiles: Boolean = true,
    /**
     * Forwards phone notifications to the watch, or null for a session that
     * forwards nothing.
     *
     * Null is the default and keeps the subscription reply DISABLED, which is
     * exactly what every session did before this existed — so the sync, find
     * and settings paths are unchanged by construction rather than by
     * inspection.
     *
     * A session that carries one should also pass `syncFiles = false`, for
     * the reason on that field: a notification link is held open for tens of
     * seconds and then closed, and a file transfer dragged along behind it
     * dies mid-flight.
     */
    private val notifications: GarminNotificationsHandler? = null,
) {

    /**
     * What the watch said it can do, once the handshake has reached
     * CONFIGURATION. Empty before that.
     */
    var capabilities: Set<GarminCapability> = emptySet()
        private set

    /**
     * Protobuf exchanges ride the same link. Constructed lazily so a session
     * that never sends one costs nothing.
     */
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

    /** Set once the directory has been fetched, so its own transfer is recognised. */
    private var directoryFetched = false

    private var finished = false
    private var filesTotal = 0

    /**
     * Resolves with everything downloaded once the sync completes, or rejects
     * on an unrecoverable protocol error.
     */
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
     * Starts the sync. The watch speaks first (device information), so this
     * only arms the state machine; everything else is driven by [handleFrame].
     */
    /**
     * Answers the watch's own HTTP-proxy fetches. Modern watches ask for
     * weather and ephemeris this way rather than over FIT, treating the phone
     * as their internet — see [GarminHttpProxy]. Order matters only in that
     * the first interceptor claiming a URL wins, and none of these overlap.
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

    /**
     * Serialises frame handling. Frames arrive from the transport as they
     * land, but the dispatch suspends on its sends — so without this a second
     * notification could enter the state machine while the first is still
     * mid-flight and mutate [active] underneath it, appending chunks out of
     * order. The mutex is FIFO, so frames are processed strictly in arrival
     * order, however fast they arrive.
     */
    private val mutex = Mutex()

    /**
     * Feeds one decoded GFDI frame in. Safe to call after completion — late
     * frames from a watch that is still talking are ignored rather than
     * throwing.
     */
    suspend fun handleFrame(frame: GarminGfdiFrame) {
        mutex.withLock { handleFrameSerially(frame) }
    }

    private suspend fun handleFrameSerially(frame: GarminGfdiFrame) {
        if (finished && !keepAnsweringAfterSync) return
        try {
            // Acknowledge FIRST, as Gadgetbridge does: an unacknowledged
            // message is treated as lost, and the watch retransmits it on a
            // timer instead of moving on. Types that get their own response
            // envelope are excluded — that response IS their acknowledgement.
            if (frame.messageType !in garminSelfAcknowledgedTypes) {
                send(buildGenericAck(frame.messageType))
            }
            if (protobuf.handleInbound(frame)) return
            dispatch(decodeGarminMessage(frame))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (finished) {
                // Past the sync, so there is no result left to fail — but
                // fail() would return silently here and a listening pass whose
                // whole purpose is to see what the watch sends must not
                // swallow the one frame it choked on.
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
                // The watch will not serve files until it knows what we
                // support, so the request goes out as soon as the
                // introduction is answered.
                send(buildSupportedFileTypesRequest())
            }

            is GarminAuthNegotiation -> send(buildAuthNegotiationResponse(message))

            is GarminConfiguration -> {
                // The capabilities exchange. The watch has told us what it can
                // do and is waiting to hear what WE can do; a bare ACK left it
                // re-sending this and never listing any files.
                //
                // Decoded, not just counted: this bitmap is the only thing
                // that says whether a watch has FIND_MY_WATCH or
                // REALTIME_SETTINGS, and the latter decides whether alarms
                // live in the watch's settings tree or in an uploaded FIT file
                // — two completely different implementations.
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
                // Upstream sends these on every connection. The weather flag
                // is the load-bearing one: it is the switch for the watch's
                // whole weather feature — see buildDeviceSettings.
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
                    // A factory-fresh watch: its screen is sitting on the
                    // "connect to the app" wizard, waiting for the phone to
                    // declare the pairing done. Gadgetbridge sends exactly
                    // this trio on first connect; without it the watch shows
                    // "connecting" forever.
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
                // Weather is PUSHED here rather than only served on request:
                // the watch asks (5014) only while connected, and this app's
                // links live seconds — the glance's ask almost always came
                // while there was nobody to hear it, leaving the wrist showing
                // "reconnect to phone". Pushed weather is cached on the watch,
                // so the glance works offline until the next connection.
                maybePushWeather()
            }

            is GarminNotificationSubscription -> {
                // Always answered, whatever the answer — the watch asks
                // roughly once a second until it gets a properly shaped
                // status.
                //
                // The two flags here mean different things and must NOT be
                // conflated:
                //
                // * `message.enable` is the WATCH's current state — whether it
                //   is presently accepting notifications. It drives the
                //   handler.
                // * the reply is the PHONE's willingness — whether this
                //   session is prepared to forward at all. It is ours alone to
                //   decide.
                //
                // Answering the conjunction is self-defeating, and was: a
                // watch that has never been told a phone would forward sends
                // `enable=false`, so replying DISABLED confirms it and the
                // watch never flips. Announcing willingness is precisely how
                // it is told otherwise. Gadgetbridge separates them the same
                // way.
                val handler = notifications
                handler?.setEnabled(enabled = message.enable)
                val willing = handler != null
                GarminLog.log(
                    "[GARMIN-SYNC] notification subscription: watch " +
                        "enable=${message.enable}, replying " +
                        if (willing) "enabled" else "disabled",
                )
                send(buildNotificationSubscriptionStatus(message, enabled = willing))
                // Held announcements go out AFTER the status, never before.
                // Garmin's own ordering is status-for-the-inbound-message
                // first, follow-up second — and a watch that has just asked to
                // subscribe has not yet been told the subscription was
                // accepted, so anything sent ahead of that status is addressed
                // to a watch that is not listening for it.
                handler?.flushHeld()
            }

            is GarminNotificationControl -> {
                // The status goes out BEFORE the answer, unlike Gadgetbridge,
                // which computes its follow-up first so a handler can
                // downgrade the status after inspecting the payload. Safe only
                // because nothing here can fail a control request: an unknown
                // notification id produces no data, not an error. That stops
                // being true the day a validating handler is added.
                send(buildNotificationControlStatus())
                notifications?.handleControl(message)
            }

            is GarminNotificationDataStatus -> notifications?.handleDataStatus(message)

            is GarminSupportedFileTypes -> {
                supportedTypes = message.types
                // The raw pairs, not just a count: they are the ground truth
                // for which GarminFileType codes a real watch actually offers.
                GarminLog.log(
                    "[GARMIN-SYNC] watch supports ${message.types.size} types: " +
                        message.types.joinToString(", ") {
                            "${it.dataType}/${it.subType}:${it.name}"
                        },
                )
                send(buildSystemEvent(GarminSystemEventType.SYNC_READY))
                // FILTER before the listing. Gadgetbridge only ever sends this
                // in reply to a SYNCHRONIZATION announcement, but an
                // unfiltered listing came back empty from a watch that
                // demonstrably held a night of sleep — and the watch processes
                // our writes in order, so by the time it answers the directory
                // request it has already seen the filter.
                if (!syncFiles) return
                send(buildFilterMessage())
                report(GarminSyncPhase.LISTING)
                requestDirectory()
            }

            is GarminDownloadRequestStatus -> onDownloadStatus(message)

            is GarminFileTransferData -> onFileChunk(message)

            is GarminSynchronization -> {
                // The watch announcing what it holds. Gadgetbridge answers
                // this with a FILTER and only then lists files — and an
                // unfiltered listing came back empty on a watch that
                // demonstrably had a night of sleep on it, so this exchange
                // looks like what actually populates the directory.
                GarminLog.log(
                    "[GARMIN-SYNC] synchronization type=${message.syncType} " +
                        "bits=${message.setBits} proceed=${message.shouldProceed}",
                )
                if (message.shouldProceed) {
                    // Cancel any pending give-up: the watch has just told us
                    // it holds something, so re-read the listing rather than
                    // finishing empty.
                    if (!syncFiles) return
                    graceJob?.cancel()
                    graceJob = null
                    send(buildFilterMessage())
                    directoryFetched = false
                    requestDirectory()
                }
            }

            is GarminGenericStatus -> {
                // ACKs for our own sends. A NAK is logged because it is the
                // only visible sign the watch rejected something we asked for.
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
                // Without Garmin Connect this app is the watch's only clock
                // source — the reply carries the time, the zone offset and the
                // next DST transitions, so the wrist clock follows travel and
                // DST instead of drifting.
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
                // Logged, not silent: a read-only sync ignores music and
                // notification chatter, but "the watch said something we did
                // not expect" is exactly the evidence a stalled sync needs,
                // and swallowing it hid whether the watch was talking to us at
                // all.
                // Truncated normally, whole while listening: 32 bytes is
                // enough to tell a stalled sync what the watch is repeating,
                // but a diagnostic pass is trying to decode the thing and half
                // a protobuf decodes to nothing.
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
        val current = active ?: return // Status for a transfer we already abandoned.

        if (!status.canProceed) {
            GarminLog.log(
                "[GARMIN-SYNC] download refused for index " +
                    "${current.entry.fileIndex}: ${status.downloadStatus.name}",
            )
            // One unreadable file must not end the sync — skip to the next,
            // exactly as the bulk importer tolerates one bad file in a batch.
            active = null
            next()
            return
        }
        current.begin(status.maxFileSize)
        // A zero-length file is complete the moment its size is known — no
        // chunk will ever arrive to trigger the completion path below.
        if (current.isComplete) {
            active = null
            onFileComplete(current)
        }
    }

    private suspend fun onFileChunk(chunk: GarminFileTransferData) {
        val current = active ?: return

        val appended = current.append(chunk)
        if (!appended) {
            // A CRC or offset mismatch means the stream desynchronised.
            // Abandoning this file (rather than the sync) keeps the rest of
            // the night's data.
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
            // Skip what a previous sync already imported — bandwidth only.
            // A null dedup key means the file cannot be identified across
            // syncs, so it is always fetched rather than guessed at — see
            // [GarminDirectoryEntry.dedupKey].
            //
            // Skipped is ALL a held file gets. This used to also re-send the
            // archive flag for it, on the theory that a watch still offering
            // a held file had missed the flag the first time. But "held" only
            // means its key is in a list; the copy is long pruned, and a key
            // collision — which the old key had — turned that into telling
            // the watch to drop a file nobody had downloaded. The archive
            // flag follows a download in THIS session or it is not sent.
            val fresh = listing.entries.filter { entry ->
                val key = entry.dedupKey
                key == null || key !in alreadySynced
            }
            queue.clear()
            queue.addAll(fresh)
            filesTotal = fresh.size
            // Full diagnostics: "0 files" has several very different causes
            // and only the raw record counts and rejected type codes tell them
            // apart.
            GarminLog.log(
                "[GARMIN-SYNC] directory ${bytes.size}B " +
                    "${listing.describe()} new=${fresh.size}",
            )
            if (listing.entries.isEmpty() && bytes.isNotEmpty()) {
                // Nothing usable came back. The raw listing is small (16 bytes
                // a record) and is the only thing that separates "the watch
                // has nothing" from "the watch answers somewhere else" — dump
                // it rather than guess.
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

        // Persist first, archive second. Archiving is irreversible from our
        // side, so a file we could not keep must stay on offer rather than
        // vanish.
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
     * Tells the watch whether the phone's app is in the foreground.
     *
     * Gadgetbridge broadcasts this on every app foreground/background change,
     * and it is the nearest thing to a "the companion is paying attention
     * now" signal — Garmin watches defer online-flavoured errands (the
     * weather fetch among them) until they believe someone is listening.
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
     * Pushes fresh weather to a watch that can render it. Called once per
     * session after the capabilities exchange; the watch answers the
     * definitions with a status, which is what releases the records
     * ([pendingWeatherData]).
     */
    private suspend fun maybePushWeather() {
        // The capability bitmap is the only sign this watch HAS a weather
        // glance; pushing at one that does not just earns a NAK.
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
     * A file the watch announced on an already-open link — a save that
     * happened mid-session. Pulled immediately while the sync is live; once
     * the sync has completed its result is sealed, so it is only noted and
     * the next sync picks it up from the directory.
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
        // An empty sync may be inside its grace wait — this announcement is
        // exactly what the wait was for, and the timer firing mid-download
        // would seal the result under it.
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
     * The FIT weather data waiting for the watch to accept its definitions.
     * FIT is schema-then-records: the definitions (5011) go first, and the
     * records (5012) only after the watch has answered — sending them
     * back-to-back raced the watch's schema apply and dropped the records.
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
            // Total by construction: finish() sends a frame, and the link can
            // drop during the seconds this waits — which is exactly when a
            // watch walks out of range. A failure inside the launched job
            // would be an unhandled error that leaves [done] pending forever,
            // so the result is settled here either way.
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

    /**
     * Ends the sync early (link dropped, user cancelled). Whatever was
     * already downloaded is still returned — a night of sleep already on the
     * phone should not be thrown away because the walk home ended the
     * connection.
     */
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
        /**
         * Renders bytes as space-separated hex, capped so a stray large
         * buffer cannot flood the log.
         */
        fun hex(bytes: ByteArray, max: Int = 256): String {
            val shown = if (bytes.size > max) bytes.copyOf(max) else bytes
            val text = shown.joinToString(" ") {
                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
            return if (bytes.size > max) "$text … (+${bytes.size - max}B)" else text
        }
    }
}

/**
 * One file being received: the expected size, the bytes so far, and the
 * running CRC the watch checks each chunk against.
 */
private class ActiveDownload(val entry: GarminDirectoryEntry) {

    private var size = 0L
    private var runningCrc = 0

    /**
     * Whether the watch has reported the size yet. Tracked separately from
     * [size] because a size of ZERO is legitimate — a watch with nothing new
     * serves an empty directory — and keying completion off `size > 0` left
     * that sync waiting forever for a chunk that was never coming.
     */
    private var begun = false

    private val data = ByteArrayOutputStream()

    val received: Int get() = data.size()
    val isComplete: Boolean get() = begun && received >= size
    val bytes: ByteArray get() = data.toByteArray()

    /**
     * The watch reports the real size in the download status; the directory's
     * size field is not authoritative.
     */
    fun begin(newSize: Long) {
        begun = true
        size = newSize
    }

    /**
     * Appends a chunk after verifying its offset and running CRC. Returns
     * false when either check fails, which the caller treats as "skip this
     * file".
     */
    fun append(chunk: GarminFileTransferData): Boolean {
        if (chunk.dataOffset != received.toLong()) return false
        val crc = GarminCrc.compute(chunk.data, initialCrc = runningCrc)
        if (crc != chunk.crc) return false
        runningCrc = crc
        data.write(chunk.data, 0, chunk.data.size)
        return true
    }
}
