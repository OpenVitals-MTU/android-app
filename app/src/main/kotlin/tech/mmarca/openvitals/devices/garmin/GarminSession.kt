package tech.mmarca.openvitals.devices.garmin

import java.io.ByteArrayOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
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
    /** How long a FILTER or download request may go unanswered. */
    private val responseTimeout: Duration = 10.seconds,
    /**
     * Wider than [responseTimeout]: an Instinct 2X spends about 16 s on
     * startup protobuf talk before it lists.
     */
    private val directoryResponseTimeout: Duration = 30.seconds,
    /** How long a transfer may go without a chunk before it is given up. */
    private val transferInactivityTimeout: Duration = 15.seconds,
    /**
     * Keep answering the watch after the sync finished. An unanswered message
     * is retransmitted on a timer and eventually takes the link down.
     */
    private val keepAnsweringAfterSync: Boolean = false,
    /** A held link's owner. Null on a sync, settings or find session: they never hand off. */
    private val heldSyncOwner: GarminHeldSyncOwner? = null,
    /** What the owner provides for the watch's asks. */
    private val hooks: GarminSessionHooks = GarminSessionHooks(),
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

    /** Whether the post-capabilities start-up sequence has gone out. */
    private var initialised = false

    /** Whether weather has been pushed this session. */
    private var weatherPushed = false

    /**
     * Pushes weather on the first capability exchange that declares it. The
     * watch only asks while connected, links are short, and it caches the push.
     */
    private suspend fun pushWeatherOnce() {
        if (weatherPushed || GarminCapability.WEATHER_CONDITIONS !in capabilities) return
        weatherPushed = true
        responders.pushWeatherIfSupported(capabilities)
    }

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

    /**
     * The last legacy directory parsed as whole 16-byte records. A FileSync
     * failure is then non-fatal.
     */
    var hasValidDirectoryListing = false
        private set

    /** Records in the last legacy directory, wanted or not. Non-zero proves legacy works. */
    var directoryRecordCount = 0
        private set

    /** A held link is mid-transfer. The forwarder keeps the radio for it, up to its own limit. */
    val isSynchronizationTransferActive: Boolean
        get() = handoffAfterDirectory

    private var awaitingFilterAck = false

    /** The FILTER before the first listing is outstanding. Refusal or silence lists anyway. */
    private var initialFilterPending = false

    private var handoffAfterDirectory = false

    private var finished = false

    /**
     * Why the sync ended early, or null. Also recorded after [finished], so a
     * link drop during the FileSync fallback still reads as an interruption.
     */
    var abortReason: String? = null
        private set
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

    private val responders by lazy { GarminSessionResponders(scope, send, protobuf, hooks) }

    fun start() {
        protobuf.onServiceRequest = { requestId, payload ->
            if (GarminFileSyncProtocol.isSyncAnnouncement(payload)) {
                // This hook runs inside frame handling, which holds the mutex already.
                scope.launch { mutex.withLock { onFileSyncAnnouncement() } }
            }
            responders.handleServiceRequest(requestId, payload)
        }
        report(GarminSyncPhase.HANDSHAKE)
    }

    /** Serialises frame handling. Dispatch suspends on sends, so frames must not interleave. */
    private val mutex = Mutex()

    private val timers = GarminStageTimers(scope, mutex)

    /** Feeds one decoded frame in. Late frames after completion are ignored. */
    suspend fun handleFrame(frame: GarminGfdiFrame) {
        mutex.withLock { handleFrameSerially(frame) }
    }

    private suspend fun handleFrameSerially(frame: GarminGfdiFrame) {
        if (finished && !keepAnsweringAfterSync) return
        try {
            // Ack first, or the watch retransmits.
            // Self-acknowledged types get their own response instead.
            if (frame.messageType !in garminSelfAcknowledgedTypes) {
                send(buildGenericAck(frame.messageType))
            }
            if (protobuf.handleInbound(frame)) return
            val message = decodeGarminMessage(frame)
            // A sealed result must not be reopened by a late listing or announcement.
            if ((finished || abortReason != null) && message.drivesFileSync) return
            dispatch(message)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (finished) {
                // Past the sync there is no result to fail, but a listening pass must log this.
                GarminLog.log("[GARMIN-LISTEN] frame ${frame.messageType} threw: $error")
                return
            }
            if (!syncFiles) {
                // A held link outlives one bad frame; only its transfer is lost.
                GarminLog.log("[GARMIN-LISTEN] frame ${frame.messageType} threw: $error")
                cancelTransfer("frame ${frame.messageType} threw: $error")
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
                // Nothing else yet: older firmware ignores requests sent before
                // the capability exchange. The rest goes out on CONFIGURATION.
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
                // Some watches send CONFIGURATION again mid-session. Every one is
                // answered; the start-up sequence below runs once.
                if (initialised) {
                    pushWeatherOnce()
                    return
                }
                initialised = true
                // The companion's post-capabilities sequence, in its order: the file
                // types ask, the settings, the clock, then SYNC_READY. The watch
                // serves no files until it knows what we support.
                send(buildSupportedFileTypesRequest())
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
                if (hooks.setupWizardPending || GarminCapability.REQUEST_PAIR_FLOW in capabilities) {
                    // Fresh watch on the pairing wizard: the trio a companion sends on first connect.
                    GarminLog.log("[GARMIN-SYNC] fresh watch asked for the pair flow; completing setup")
                    send(buildSystemEvent(GarminSystemEventType.PAIR_COMPLETE))
                    send(buildSystemEvent(GarminSystemEventType.SYNC_COMPLETE))
                    send(buildSystemEvent(GarminSystemEventType.SETUP_WIZARD_COMPLETE))
                    hooks.onSetupWizardCompleted?.invoke()
                }
                // The clock nudge a companion sends on every connection.
                send(buildSystemEvent(GarminSystemEventType.TIME_UPDATED))
                // Unconditional: older firmware waits for it before it subscribes
                // for notifications or lists files, whatever it said about file types.
                send(buildSystemEvent(GarminSystemEventType.SYNC_READY))
                if (hooks.hostForeground?.invoke() == true) {
                    GarminLog.log("[GARMIN-SYNC] telling the watch the app is in the foreground")
                    notifyHostForeground(true)
                }
                hooks.onHandshakeReady?.invoke()
                pushWeatherOnce()
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
                // FILTER first: without it a vívoactive 5 listed nothing while holding sleep data.
                // The listing waits for the answer, so the two cannot race.
                if (!syncFiles) return
                report(GarminSyncPhase.LISTING)
                awaitingFilterAck = true
                initialFilterPending = true
                armStageTimeout(responseTimeout, GarminSyncTimeout.filter(responseTimeout))
                send(buildFilterMessage())
            }

            is GarminDownloadRequestStatus -> onDownloadStatus(message)

            is GarminFileTransferData -> onFileChunk(message)

            is GarminSynchronization -> {
                // The watch announcing what it holds. Filter, then list.
                GarminLog.log(
                    "[GARMIN-SYNC] synchronization type=${message.syncType} " +
                        "bits=${message.setBits} proceed=${message.shouldProceed}",
                )
                if (message.shouldProceed) {
                    if (!syncFiles) {
                        announceOnHeldLink()
                        return
                    }
                    // The watch holds something: drop the give-up timer and list again.
                    timers.cancelGrace()
                    if (!awaitingFilterAck) {
                        awaitingFilterAck = true
                        armStageTimeout(responseTimeout, GarminSyncTimeout.filter(responseTimeout))
                        send(buildFilterMessage())
                    }
                }
            }

            is GarminGenericStatus -> {
                // A NAK is the only visible sign the watch rejected a request.
                if (message.status != GarminStatus.ACK) {
                    GarminLog.log(
                        "[GARMIN-SYNC] NAK ${message.status.name} for " +
                            "message ${message.originalMessageType}",
                    )
                    if (message.originalMessageType == GarminMessageId.RESPONSE) {
                        protobuf.handleAckRejected(message.status)
                    }
                    if (message.originalMessageType == GarminMessageId.FILTER && awaitingFilterAck) {
                        awaitingFilterAck = false
                        cancelStageTimeout()
                        if (initialFilterPending) {
                            // A watch that refuses the first FILTER still lists.
                            initialFilterPending = false
                            GarminLog.log("[GARMIN-SYNC] filter refused; listing anyway")
                            requestDirectory()
                        } else if (syncFiles) {
                            complete()
                        }
                    }
                    return
                }
                if (message.originalMessageType == GarminMessageId.FILTER) {
                    cancelStageTimeout()
                    GarminLog.log("[GARMIN-SYNC] filter accepted")
                    if (awaitingFilterAck) {
                        awaitingFilterAck = false
                        initialFilterPending = false
                        if (!syncFiles) {
                            GarminLog.log(
                                "[GARMIN-LISTEN] filter accepted; reading directory before handoff",
                            )
                            handoffAfterDirectory = true
                            directoryFetched = false
                            requestDirectory()
                            return
                        }
                        directoryFetched = false
                        report(GarminSyncPhase.LISTING)
                        requestDirectory()
                    }
                }
                if (message.originalMessageType == GarminMessageId.FIT_DEFINITION) {
                    responders.onFitDefinitionAccepted()
                }
            }

            is GarminProtobufStatus -> protobuf.handleStatus(message)

            is GarminCurrentTimeRequest -> responders.handleCurrentTime(message)

            is GarminWeatherRequest -> responders.handleWeatherRequest(message)

            is GarminFindMyPhoneRequest -> responders.handleFindPhone(message)

            is GarminFindMyPhoneCancel -> responders.handleFindPhoneCancel()

            is GarminFileAvailable -> onFileAvailable(message.entry)

            is GarminUnhandledMessage -> {
                // Type and length only. The payload can carry notification text, locations or credentials.
                GarminLog.log(
                    "[GARMIN-SYNC] unhandled message " +
                        "${message.messageType} (${message.payload.size}B)",
                )
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
        armStageTimeout(directoryResponseTimeout, GarminSyncTimeout.directory(directoryResponseTimeout))
        send(buildDownloadRequest(fileIndex = 0))
    }

    private suspend fun onDownloadStatus(status: GarminDownloadRequestStatus) {
        val current = active ?: return // Status for a transfer we already abandoned.
        cancelStageTimeout()

        if (!status.canProceed) {
            GarminLog.log(
                "[GARMIN-SYNC] download refused for index " +
                    "${current.entry.fileIndex}: ${status.downloadStatus.name}",
            )
            // One unreadable file must not end the sync. A refused root directory would:
            // nothing else sets directoryFetched, so the sync would hang until its global timeout.
            active = null
            if (current.entry.type == GarminFileType.DIRECTORY) {
                directoryFetched = true
                if (handoffAfterDirectory) {
                    // A watch that refuses its own directory cannot be listed here.
                    handoffAfterDirectory = false
                    heldSyncOwner?.needsFullSync()
                } else {
                    complete()
                }
            } else {
                next()
            }
            return
        }
        current.begin(status.maxFileSize)
        // A zero-length file completes as soon as its size is known.
        if (current.isComplete) {
            active = null
            onFileComplete(current)
        } else {
            armTransferInactivityTimeout(current)
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
            cancelStageTimeout()
            next()
            return
        }

        armTransferInactivityTimeout(current)
        send(buildFileTransferDataAck(current.received))
        if (!current.isComplete) return

        active = null
        cancelStageTimeout()
        onFileComplete(current)
    }

    private suspend fun onFileComplete(finishedDownload: ActiveDownload) {
        val bytes = finishedDownload.bytes

        if (finishedDownload.entry.type == GarminFileType.DIRECTORY) {
            directoryFetched = true
            val listing = GarminDirectory.parseWithDiagnostics(bytes)
            hasValidDirectoryListing = listing.isStructurallyValid
            directoryRecordCount = listing.totalRecords
            val owner = heldSyncOwner
            if (handoffAfterDirectory && owner != null) {
                GarminLog.log(
                    "[GARMIN-LISTEN] handoff directory ${bytes.size}B " +
                        listing.describe(),
                )
                // Read now, not at link open: a manual sync may have run since.
                val held = owner.alreadySyncedKeys()
                val fresh = listing.entries.filter { entry ->
                    val key = entry.dedupKey
                    key == null || key !in held
                }
                queue.clear()
                queue.addAll(fresh)
                filesTotal = fresh.size
                GarminLog.log("[GARMIN-LISTEN] downloading ${fresh.size} file(s) on filtered link")
                report(GarminSyncPhase.DOWNLOADING)
                next()
                return
            }
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
        val owner = heldSyncOwner
        val keep: (suspend (GarminDownloadedFile) -> Unit)? =
            if (syncFiles || owner == null) onFileDownloaded else owner::keep
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
     * A file announced mid-session. Pulled now while the sync is live,
     * otherwise left for the next sync.
     */
    private suspend fun onFileAvailable(entry: GarminDirectoryEntry) {
        if (finished || !syncFiles) {
            GarminLog.log(
                "[GARMIN-LISTEN] watch announced ${entry.type.label} " +
                    "index=${entry.fileIndex}",
            )
            heldSyncOwner?.needsFullSync()
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
        timers.cancelGrace()
        if (directoryFetched && active == null) {
            next()
        }
    }

    /** Starts the next queued file, or finishes the sync when the queue is empty. */
    private suspend fun next() {
        if (!directoryFetched) return // Still waiting on the listing.
        if (queue.isEmpty()) {
            if (handoffAfterDirectory) {
                handoffAfterDirectory = false
                directoryFetched = false
                val batch = downloaded.toList()
                downloaded.clear()
                filesTotal = 0
                GarminLog.log(
                    "[GARMIN-LISTEN] filtered sync complete: ${batch.size} files",
                )
                if (batch.isEmpty() && directoryRecordCount == 0) {
                    // The watch lists nothing the legacy way: a full sync can fall back to FileSync.
                    GarminLog.log("[GARMIN-LISTEN] empty legacy listing; handing to owner for a full sync")
                    heldSyncOwner?.needsFullSync()
                } else {
                    heldSyncOwner?.imported(batch)
                }
                return
            }
            complete()
            return
        }
        val entry = queue.removeAt(0)
        active = ActiveDownload(entry = entry)
        report(GarminSyncPhase.DOWNLOADING, file = entry.type.label)
        armStageTimeout(responseTimeout, GarminSyncTimeout.download(entry.type.label, responseTimeout))
        send(buildDownloadRequest(fileIndex = entry.fileIndex))
    }

    private var graceUsed = false

    private fun armTransferInactivityTimeout(download: ActiveDownload) {
        armStageTimeout(
            transferInactivityTimeout,
            GarminSyncTimeout.transfer(download.entry.type.label, transferInactivityTimeout),
        )
    }

    private fun armStageTimeout(duration: Duration, reason: String) {
        timers.armStage(duration) { onStageTimeout(reason) }
    }

    private suspend fun onStageTimeout(reason: String) {
        if (finished) return
        if (syncFiles && awaitingFilterAck && initialFilterPending) {
            // A watch that ignores the first FILTER still lists.
            awaitingFilterAck = false
            initialFilterPending = false
            GarminLog.log("[GARMIN-SYNC] filter unanswered; listing anyway")
            requestDirectory()
            return
        }
        // A sync ends on a stall. A held link only drops the transfer:
        // it must keep answering the watch, or the link dies with it.
        if (syncFiles) abort(reason) else cancelTransfer(reason)
    }

    private fun cancelStageTimeout() = timers.cancelStage()

    /** The watch said it holds new data. Filter, then list, on this same link. */
    private suspend fun announceOnHeldLink() {
        if (heldSyncOwner == null) {
            // A settings or find session: nobody to hand the files to.
            GarminLog.log("[GARMIN-LISTEN] sync data announced; no owner to hand it to")
            return
        }
        GarminLog.log("[GARMIN-LISTEN] sync data announced on held link; filtering before handoff")
        if (awaitingFilterAck || handoffAfterDirectory) return
        awaitingFilterAck = true
        armStageTimeout(responseTimeout, GarminSyncTimeout.filter(responseTimeout))
        send(buildFilterMessage())
    }

    /** A FileSyncService "new files" or "start sync" notice, the newer firmware's announcement. */
    private suspend fun onFileSyncAnnouncement() {
        if (finished || abortReason != null) return
        if (syncFiles) {
            // A running sync lists the directory itself.
            GarminLog.log("[GARMIN-SYNC] watch announced new files over FileSync")
            return
        }
        announceOnHeldLink()
    }

    /**
     * Ends a held-link transfer without ending the session. Files already
     * persisted are handed to the owner; the link stays open and listening.
     */
    private fun cancelTransfer(reason: String) {
        val wasActive = handoffAfterDirectory || awaitingFilterAck || active != null
        val partial = downloaded.toList()
        handoffAfterDirectory = false
        awaitingFilterAck = false
        initialFilterPending = false
        directoryFetched = false
        active = null
        queue.clear()
        downloaded.clear()
        filesTotal = 0
        cancelStageTimeout()
        if (!wasActive) return
        GarminLog.log("[GARMIN-LISTEN] transfer cancelled: $reason")
        if (partial.isNotEmpty()) {
            GarminLog.log(
                "[GARMIN-LISTEN] importing ${partial.size} persisted file(s) after interruption",
            )
            heldSyncOwner?.imported(partial)
        }
    }

    private suspend fun complete() {
        if (finished) return
        if (downloaded.isEmpty() && !graceUsed) {
            graceUsed = true
            GarminLog.log(
                "[GARMIN-SYNC] nothing listed; waiting " +
                    "${emptyGrace.inWholeSeconds}s in case the watch announces",
            )
            // Settle the result here either way, or a failure would leave [done] pending.
            timers.armGrace(emptyGrace) {
                try {
                    finish()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    GarminLog.log("[GARMIN-SYNC] could not close out an empty sync: $error")
                    finished = true
                    doneDeferred.complete(downloaded.toList())
                }
            }
            return
        }
        finish()
    }

    private suspend fun finish() {
        if (finished) return
        finished = true
        timers.cancelAll()
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
        handoffAfterDirectory = false
        awaitingFilterAck = false
        initialFilterPending = false
        active = null
        cancelStageTimeout()
        report(GarminSyncPhase.FAILED)
        GarminLog.log("[GARMIN-SYNC] failed: $error")
        doneDeferred.completeExceptionally(error)
    }

    /** Ends the sync early. What was already downloaded is still returned. */
    fun abort(reason: Any? = null) {
        protobuf.abort()
        val message = reason?.toString() ?: "The Garmin session ended unexpectedly."
        if (finished) {
            if (abortReason == null) abortReason = message
            return
        }
        // A held link's partial batch is still imported; a sync's is returned below.
        if (!syncFiles) cancelTransfer(message)
        finished = true
        abortReason = message
        handoffAfterDirectory = false
        awaitingFilterAck = false
        initialFilterPending = false
        active = null
        queue.clear()
        timers.cancelAll()
        report(GarminSyncPhase.FAILED)
        GarminLog.log("[GARMIN-SYNC] aborted: $abortReason")
        doneDeferred.complete(downloaded.toList())
    }

}

private val GarminInboundMessage.drivesFileSync: Boolean
    get() = this is GarminDownloadRequestStatus ||
        this is GarminFileTransferData ||
        this is GarminSynchronization ||
        this is GarminFileAvailable

/**
 * One file being received: the expected size, the bytes so far, and the
 * running CRC the watch checks each chunk against.
 */
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
