package tech.mmarca.openvitals.features.devicesync.protocol

import java.security.SecureRandom
import java.util.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The sync session state machine: handshake, key exchange, code check,
 * bidirectional record exchange, report. Pure Kotlin over a [SyncByteTransport].
 *
 * Both phones run the same code, differing only in [SyncRole]. No record moves
 * until both users said the codes match. From the key exchange on, every frame
 * but an abort is sealed; see [SyncFrameCipher].
 *
 * A sender loop pushes records with stop-and-wait acks while a receiver
 * loop dedups, writes and acks the peer's.
 */

/** Which side of the pairing this phone is. The host commits to its key first. */
enum class SyncRole { HOST, GUEST }

/** Thrown when a session ends early. */
class SyncAborted(val reason: String) : Exception(reason)

/** The Health Connect side of a session, injected so the protocol stays testable. */
interface SyncRecordStore {
    /**
     * Streams the dedup key of every local record for [types] in the window.
     * The session hashes and discards each one.
     */
    fun readKeys(types: Set<String>): Flow<String>

    /**
     * Streams this phone's records as chunks of at most [chunkSize]. A cold
     * flow, so the whole window is never in memory at once.
     */
    fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>>

    /** Writes [items] and returns the keys that landed. A missing key is not counted as imported. */
    suspend fun writeItems(items: List<SyncItem>): Set<String>
}

/** Static configuration for a session. */
class SyncSessionConfig(
    val role: SyncRole,
    /**
     * Shows the six digits and returns whether the user said they match the other
     * phone's. Cancelled when the session ends first.
     */
    val confirmCode: suspend (code: String) -> Boolean,
    val deviceName: String,
    /** Record types this device supports. */
    val supportedTypes: List<String>,
    /** Types the user chose to sync; defaults to all [supportedTypes]. */
    val selectedTypes: List<String>? = null,
    val hcProviderVersion: Long? = null,
    /** Records per batch (stop-and-wait unit). */
    val batchSize: Int = 200,
    /**
     * How long the other user may take to press Start sync. Each phone starts its
     * session on its own tap, so this waits for a person, not for a radio. A phone
     * that leaves the wizard drops the link, which ends the wait at once.
     */
    val peerStartTimeoutMillis: Long = 600_000,
    /** How long the other phone may take to answer a handshake frame. */
    val handshakeTimeoutMillis: Long = 30_000,
    /** How long a user may take to compare the codes. */
    val confirmTimeoutMillis: Long = 120_000,
    val batchTimeoutMillis: Long = 60_000,
    /** Pre-seeded nonce for deterministic tests; production generates one. */
    val nonce: ByteArray? = null,
)

/** Drives one sync session to completion. Create per session; not reusable. */
class SyncSession(
    private val transport: SyncByteTransport,
    private val store: SyncRecordStore,
    val config: SyncSessionConfig,
    random: Random = SecureRandom(),
) {
    private val nonce: ByteArray = config.nonce ?: generateSyncNonce(random)

    private val report = SyncReportBuilder()

    // Hashed, not the key strings: 16 bytes an entry is what fits a small heap.
    // Seeded before the loops start, then touched only by the receiver.
    private val seenKeys = HashSet<SyncKeyHash>()
    private val keyHasher = SyncKeyHasher()

    private val _progress = MutableStateFlow(SyncProgress(phase = SyncPhase.HANDSHAKE))

    /** Live progress for the UI. */
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    // Handshake deferreds + exchange plumbing, driven by [dispatch].
    private val peerHello = CompletableDeferred<SyncHello>()
    private val peerCommit = CompletableDeferred<Unit>()
    private val sessionKeys = CompletableDeferred<SyncSessionKeys>()
    private val peerConfirmed = CompletableDeferred<Unit>()
    private val failure = CompletableDeferred<SyncAborted>()
    private val incomingBatches = Channel<SyncBatch>(Channel.UNLIMITED)

    @Volatile private var pendingAck: CompletableDeferred<Int>? = null

    @Volatile private var receivedHello: SyncHello? = null

    // Key exchange state. The reader derives the keys itself, so the cipher is
    // in place before the next frame is read.
    private lateinit var keyPair: SyncKeyPair
    private val hostSalt: ByteArray = generateSyncNonce(random)

    @Volatile private var ownHelloBytes: ByteArray? = null

    @Volatile private var peerHelloBytes: ByteArray? = null

    @Volatile private var commitment: ByteArray? = null

    @Volatile private var commitSent = false

    @Volatile private var shareSent = false

    @Volatile private var cipher: SyncFrameCipher? = null

    // Set before our CONFIRM leaves, so it is true before the peer can answer with records.
    @Volatile private var localConfirmed = false
    private var abortSent = false
    private var abortReason: String? = null

    private val frameReader = SyncFrameReader()

    // Both loops write to the one link; every outbound frame takes this lock.
    private val sendMutex = Mutex()

    /** Runs the session and resolves with the report. Cancellation propagates. */
    suspend fun run(): SyncReport = coroutineScope {
        // Fresh for every session, and made before the reader can need it.
        keyPair = generateSyncKeyPair()
        val readerJob = launch { readLoop() }
        try {
            val peer = handshake()
            val keys = exchangeKeys(peer)
            // Record frames before both users agreed are a protocol violation; see [dispatch].
            confirmCode(keys.code)
            val negotiated = negotiateTypes(peer)
            exchange(negotiated)
            emit(phase = SyncPhase.COMPLETE)
            report.build(
                completed = true,
                peerDeviceName = peer.deviceName,
                negotiatedTypes = negotiated,
            )
        } catch (e: SyncAborted) {
            sendAbort(e.reason)
            emit(phase = SyncPhase.ABORTED)
            report.build(
                completed = false,
                peerDeviceName = receivedHello?.deviceName ?: "unknown",
                negotiatedTypes = emptyList(),
                abortReason = e.reason,
            )
        } finally {
            readerJob.cancel()
            incomingBatches.close()
        }
    }

    // ── Phases ───────────────────────────────────────────────────────────────

    private suspend fun handshake(): SyncHello {
        emit(phase = SyncPhase.HANDSHAKE)
        val hello = SyncHello(
            protocolVersion = SYNC_PROTOCOL_VERSION,
            deviceName = config.deviceName,
            hcProviderVersion = config.hcProviderVersion,
            supportedTypes = config.supportedTypes,
            nonce = nonce,
        ).encode()
        ownHelloBytes = hello
        send(SyncFrameType.HELLO, hello)
        val peer = await(peerHello, config.peerStartTimeoutMillis, "the other phone did not start the sync in time")
        if (peer.protocolVersion != SYNC_PROTOCOL_VERSION) {
            throw SyncAborted(
                "incompatible protocol version ${peer.protocolVersion} " +
                    "(this app speaks $SYNC_PROTOCOL_VERSION)",
            )
        }
        return peer
    }

    private suspend fun exchangeKeys(peer: SyncHello): SyncSessionKeys {
        emit(phase = SyncPhase.AUTHENTICATING)
        // An echo of our own hello is not a second phone.
        if (constantTimeEquals(peer.nonce, nonce)) {
            throw SyncAborted("peer reflected our nonce")
        }
        when (config.role) {
            SyncRole.HOST -> {
                // Set first: the guest may answer before send returns.
                commitSent = true
                send(
                    SyncFrameType.KEY_COMMIT,
                    SyncKeyCommit(commitToHostKey(keyPair.publicKey, hostSalt)).encode(),
                )
                // The reader derives the keys when the guest's key arrives.
                val keys = await(sessionKeys, config.handshakeTimeoutMillis, "timed out waiting for the peer key")
                send(SyncFrameType.KEY_REVEAL, SyncKeyReveal(keyPair.publicKey, hostSalt).encode())
                return keys
            }
            SyncRole.GUEST -> {
                await(peerCommit, config.handshakeTimeoutMillis, "timed out waiting for the host commitment")
                shareSent = true
                send(SyncFrameType.KEY_SHARE, SyncKeyShare(keyPair.publicKey).encode())
                return await(sessionKeys, config.handshakeTimeoutMillis, "timed out waiting for the host key")
            }
        }
    }

    private suspend fun confirmCode(code: String) {
        if (!askUser(code)) throw SyncAborted(CODES_DIFFER_REASON)
        localConfirmed = true
        emit(phase = SyncPhase.CONFIRMING)
        send(SyncFrameType.CONFIRM, ByteArray(0))
        await(peerConfirmed, config.confirmTimeoutMillis, "timed out waiting for the other phone to confirm")
    }

    /** Asks the user, but gives up as soon as the link or the peer ends the session. */
    private suspend fun askUser(code: String): Boolean = coroutineScope {
        val watchdog = launch { throw failure.await() }
        val answer = withTimeoutOrNull(config.confirmTimeoutMillis) { config.confirmCode(code) }
        watchdog.cancel()
        answer ?: throw SyncAborted("nobody confirmed the code in time")
    }

    private fun negotiateTypes(peer: SyncHello): List<String> {
        val peerTypes = peer.supportedTypes.toSet()
        val selected = (config.selectedTypes ?: config.supportedTypes).toSet()
        // Order-stable intersection: keep this phone's declared order.
        return config.supportedTypes.filter { it in peerTypes && it in selected }
    }

    private suspend fun exchange(negotiated: List<String>) {
        emit(phase = SyncPhase.EXCHANGING)
        val types = negotiated.toSet()
        // Seed the dedup baseline from our own keys, so dedup is an in-memory
        // lookup. It also catches records held natively with no clientRecordId.
        // Must be complete before the receiver writes its first batch.
        store.readKeys(types).collect { key -> seenKeys += keyHasher.hash(key) }
        // Sender and receiver run concurrently over the one full-duplex link.
        coroutineScope {
            launch { runSender(types) }
            launch { runReceiver() }
        }
    }

    private suspend fun runSender(types: Set<String>) {
        var seq = 0
        var sent = 0
        // Stop-and-wait per chunk; peak sender memory is one batch.
        store.readItemChunks(types, config.batchSize).collect { chunk ->
            if (chunk.isEmpty()) return@collect
            seq += 1
            val ack = CompletableDeferred<Int>()
            pendingAck = ack
            send(SyncFrameType.BATCH, SyncBatch(seq, chunk).encode())
            await(ack, config.batchTimeoutMillis, "timed out waiting for ack")
            sent += chunk.size
            report.itemsSent = sent
            emit(itemsSent = sent)
        }
        send(SyncFrameType.SEND_DONE, ByteArray(0))
    }

    private suspend fun runReceiver() {
        var received = 0
        var written = 0
        while (true) {
            // A link that goes silent without a disconnect event: a gap longer
            // than the batch timeout means the peer is gone.
            val result = withTimeoutOrNull(config.batchTimeoutMillis) {
                incomingBatches.receiveCatching()
            } ?: throw SyncAborted("timed out waiting for the next batch")
            if (result.isClosed) {
                result.exceptionOrNull()?.let { throw it }
                break
            }
            val batch = result.getOrThrow()
            val fresh = mutableListOf<SyncItem>()
            for (item in batch.items) {
                // seenKeys covers cross-device and within-session dedup.
                val keyHash = keyHasher.hash(item.key)
                if (keyHash in seenKeys) {
                    report.recordReceived(item.recordType, duplicate = true)
                } else {
                    fresh += item
                    seenKeys += keyHash
                }
            }
            // Count imported from what landed, not what was tried.
            var writtenKeys: Set<String> = emptySet()
            if (fresh.isNotEmpty()) {
                emit(phase = SyncPhase.WRITING)
                writtenKeys = store.writeItems(fresh)
            }
            fresh.forEach { item ->
                report.recordReceived(item.recordType, imported = item.key in writtenKeys)
            }
            received += batch.items.size
            written += writtenKeys.size
            emit(phase = SyncPhase.EXCHANGING, itemsReceived = received, itemsWritten = written)
            send(SyncFrameType.BATCH_ACK, SyncBatchAck(batch.seq).encode())
        }
    }

    // ── Frame dispatch ───────────────────────────────────────────────────────

    private suspend fun readLoop() {
        try {
            for (chunk in transport.inbound) {
                onChunk(chunk)
            }
            failPending("connection lost")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failPending("link error: ${e.message}")
        }
    }

    private fun onChunk(chunk: ByteArray) {
        val frames = try {
            frameReader.addChunk(chunk)
        } catch (e: SyncFrameFormatException) {
            failPending("malformed frame: ${e.message}")
            return
        }
        frames.forEach(::dispatch)
    }

    private fun dispatch(frame: SyncFrame) {
        try {
            when (frame.type) {
                SyncFrameType.HELLO -> {
                    if (!peerHello.isCompleted) {
                        val hello = SyncHello.decode(frame.payload)
                        receivedHello = hello
                        peerHelloBytes = frame.payload
                        peerHello.complete(hello)
                    }
                }
                // Only a version 1 peer sends this. The hello check ends that session.
                SyncFrameType.AUTH -> Unit
                SyncFrameType.KEY_COMMIT -> onKeyCommit(frame.payload)
                SyncFrameType.KEY_SHARE -> onKeyShare(frame.payload)
                SyncFrameType.KEY_REVEAL -> onKeyReveal(frame.payload)
                SyncFrameType.CONFIRM,
                SyncFrameType.BATCH,
                SyncFrameType.BATCH_ACK,
                SyncFrameType.SEND_DONE,
                -> {
                    val opener = cipher
                    if (opener == null) {
                        failPending("${frame.type.wireName} before the key exchange")
                        return
                    }
                    // Opened in arrival order, even when rejected below: the counter must advance.
                    val payload = opener.open(frame.type, frame.payload)
                    if (frame.type == SyncFrameType.CONFIRM) {
                        if (!peerConfirmed.complete(Unit)) failPending("confirm sent twice")
                        return
                    }
                    // Records before both users agreed are rejected without decoding, so
                    // an unconfirmed peer cannot trigger a gzip inflate.
                    if (!localConfirmed || !peerConfirmed.isCompleted) {
                        failPending("${frame.type.wireName} before authentication")
                        return
                    }
                    when (frame.type) {
                        SyncFrameType.BATCH ->
                            incomingBatches.trySend(SyncBatch.decode(payload))
                        SyncFrameType.BATCH_ACK -> {
                            val ack = pendingAck
                            pendingAck = null
                            ack?.complete(SyncBatchAck.decode(payload).seq)
                        }
                        SyncFrameType.SEND_DONE -> incomingBatches.close()
                        else -> Unit
                    }
                }
                SyncFrameType.ABORT -> handleAbort(SyncAbort.decode(frame.payload).reason)
            }
        } catch (e: Exception) {
            // A frame can parse as JSON with the wrong shape. Abort cleanly.
            failPending("bad ${frame.type.wireName} frame: ${e.message}")
        }
    }

    private fun onKeyCommit(payload: ByteArray) {
        if (config.role != SyncRole.GUEST || peerHelloBytes == null || commitment != null) {
            failPending("unexpected keyCommit")
            return
        }
        commitment = SyncKeyCommit.decode(payload).commitment
        peerCommit.complete(Unit)
    }

    private fun onKeyShare(payload: ByteArray) {
        if (config.role != SyncRole.HOST || !commitSent || sessionKeys.isCompleted) {
            failPending("unexpected keyShare")
            return
        }
        val guestKey = SyncKeyShare.decode(payload).publicKey
        useKeys(hostKey = keyPair.publicKey, guestKey = guestKey, salt = hostSalt, peerKey = guestKey)
    }

    private fun onKeyReveal(payload: ByteArray) {
        val committed = commitment
        if (config.role != SyncRole.GUEST || committed == null || !shareSent || sessionKeys.isCompleted) {
            failPending("unexpected keyReveal")
            return
        }
        val reveal = SyncKeyReveal.decode(payload)
        if (!hostKeyMatchesCommitment(committed, reveal.publicKey, reveal.salt)) {
            failPending("the host key does not match its commitment")
            return
        }
        useKeys(hostKey = reveal.publicKey, guestKey = keyPair.publicKey, salt = reveal.salt, peerKey = reveal.publicKey)
    }

    private fun useKeys(hostKey: ByteArray, guestKey: ByteArray, salt: ByteArray, peerKey: ByteArray) {
        val own = checkNotNull(ownHelloBytes)
        val peer = checkNotNull(peerHelloBytes)
        val isHost = config.role == SyncRole.HOST
        val transcript = syncTranscriptHash(
            hostHello = if (isHost) own else peer,
            guestHello = if (isHost) peer else own,
            hostPublicKey = hostKey,
            guestPublicKey = guestKey,
            hostSalt = salt,
        )
        val keys = deriveSyncSessionKeys(keyPair, peerKey, transcript)
        cipher = SyncFrameCipher.forRole(config.role, keys)
        sessionKeys.complete(keys)
    }

    private fun handleAbort(reason: String) {
        if (abortReason == null) abortReason = reason
        failPending("peer aborted: $reason")
    }

    /** Propagates a fatal condition to whichever awaiter is live so [run] unwinds. */
    private fun failPending(reason: String) {
        val error = SyncAborted(reason)
        failure.complete(error)
        peerHello.completeExceptionally(error)
        peerCommit.completeExceptionally(error)
        sessionKeys.completeExceptionally(error)
        peerConfirmed.completeExceptionally(error)
        val ack = pendingAck
        pendingAck = null
        ack?.completeExceptionally(error)
        // Closing the channel with a cause makes a parked receiver rethrow it.
        incomingBatches.close(error)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun send(type: SyncFrameType, payload: ByteArray) {
        sendMutex.withLock {
            try {
                // Sealed under the lock: the cipher counts frames in send order.
                val body = if (type.isSealed) checkNotNull(cipher).seal(type, payload) else payload
                transport.send(SyncFrame(type, body).encode())
            } catch (e: CancellationException) {
                throw e
            } catch (e: SyncAborted) {
                throw e
            } catch (e: Exception) {
                // A dead carrier must surface as a clean abort with a report.
                throw SyncAborted("link error: ${e.message}")
            }
        }
    }

    private suspend fun sendAbort(reason: String) {
        if (abortSent) return
        abortSent = true
        try {
            send(SyncFrameType.ABORT, SyncAbort(reason).encode())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Link may already be gone; nothing more to do.
        }
    }

    private suspend fun <T> await(
        deferred: CompletableDeferred<T>,
        timeoutMillis: Long,
        message: String,
    ): T = withTimeoutOrNull(timeoutMillis) { deferred.await() } ?: throw SyncAborted(message)

    private fun emit(
        phase: SyncPhase? = null,
        itemsSent: Int? = null,
        itemsReceived: Int? = null,
        itemsWritten: Int? = null,
    ) {
        _progress.value = _progress.value.let { current ->
            current.copy(
                phase = phase ?: current.phase,
                itemsSent = itemsSent ?: current.itemsSent,
                itemsReceived = itemsReceived ?: current.itemsReceived,
                itemsWritten = itemsWritten ?: current.itemsWritten,
            )
        }
    }
}

/** The Dart-era wire name, used in abort reasons ("batch before authentication"). */
private val SyncFrameType.wireName: String
    get() = when (this) {
        SyncFrameType.HELLO -> "hello"
        SyncFrameType.AUTH -> "auth"
        SyncFrameType.BATCH -> "batch"
        SyncFrameType.BATCH_ACK -> "batchAck"
        SyncFrameType.SEND_DONE -> "sendDone"
        SyncFrameType.ABORT -> "abort"
        SyncFrameType.KEY_COMMIT -> "keyCommit"
        SyncFrameType.KEY_SHARE -> "keyShare"
        SyncFrameType.KEY_REVEAL -> "keyReveal"
        SyncFrameType.CONFIRM -> "confirm"
    }

/** The abort reason when this phone's user said the codes differ. */
const val CODES_DIFFER_REASON: String = "the codes did not match"
