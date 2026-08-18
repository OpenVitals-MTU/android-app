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
 * The sync session state machine: handshake → authenticate → bidirectional
 * record exchange → report. Pure Kotlin over a [SyncByteTransport], so the
 * whole protocol is testable over an in-memory pipe with no Bluetooth.
 *
 * The session is SYMMETRIC — both phones run the same code, differing only in
 * [SyncRole] (which fixes the nonce order for the shared key). Records flow
 * both ways over the one socket at once: a sender loop pushes our records and
 * waits for per-batch acks (stop-and-wait backpressure), while a receiver loop
 * dedups and writes the peer's records and acks them. Inbound frames are
 * demultiplexed by type to whichever loop is waiting.
 */

/**
 * Which side of the pairing this phone is. Fixes the (host, guest) nonce order
 * so both derive the same session key.
 */
enum class SyncRole { HOST, GUEST }

/**
 * Thrown when a session ends early — bad protocol version, failed auth, a
 * dropped link, or a peer/local abort.
 */
class SyncAborted(val reason: String) : Exception(reason)

/**
 * The Health Connect side of a session, injected so the protocol stays free of
 * health dependencies (and testable with an in-memory fake).
 */
interface SyncRecordStore {
    /**
     * Streams the dedup key of every local record for [types] in the session's
     * window. These keys are the session's dedup baseline: an incoming peer
     * record is a duplicate iff we already hold a record with the same content
     * fingerprint. Keys only — the session hashes and discards each one, so
     * the baseline never holds the records themselves.
     */
    fun readKeys(types: Set<String>): Flow<String>

    /**
     * Streams this phone's records for [types] in the window as chunks of at
     * most [chunkSize] items, each carrying its dedup [SyncItem.key]. A cold
     * flow read lazily: the session sends each chunk and drops it before the
     * next is produced, so the whole window is never in memory at once — a
     * data-dense year materialized up front used to GC-thrash small heaps
     * until the link timed out.
     */
    fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>>

    /**
     * Writes [items] to Health Connect (post-dedup) and returns the
     * [SyncItem.key]s that actually landed. A key missing from the result was
     * received but not written (an undecodable item, or a type whose batch
     * insert was rejected) — the session then does NOT count it as imported.
     */
    suspend fun writeItems(items: List<SyncItem>): Set<String>
}

/** Static configuration for a session. */
class SyncSessionConfig(
    val role: SyncRole,
    /** The 6-digit pairing code (host shows it, guest types it). Both feed it in. */
    val code: String,
    val deviceName: String,
    /** Record types this device supports. */
    val supportedTypes: List<String>,
    /** Types the user chose to sync; defaults to all [supportedTypes]. */
    val selectedTypes: List<String>? = null,
    val hcProviderVersion: Long? = null,
    /** Records per batch (stop-and-wait unit). */
    val batchSize: Int = 200,
    val handshakeTimeoutMillis: Long = 30_000,
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

    // Hashed, not the key strings themselves: the baseline covers every local
    // record in the window, and 16 bytes an entry is the difference between a
    // large sync fitting a small heap or not. Confinement per [SyncKeyHasher]:
    // seeded before the exchange loops start, then touched only by the receiver.
    private val seenKeys = HashSet<SyncKeyHash>()
    private val keyHasher = SyncKeyHasher()

    private val _progress = MutableStateFlow(SyncProgress(phase = SyncPhase.HANDSHAKE))

    /** Live progress for the UI. */
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    // Handshake deferreds + exchange plumbing, driven by [dispatch].
    private val peerHello = CompletableDeferred<SyncHello>()
    private val peerAuth = CompletableDeferred<SyncAuthProof>()
    private val incomingBatches = Channel<SyncBatch>(Channel.UNLIMITED)

    @Volatile private var pendingAck: CompletableDeferred<Int>? = null

    @Volatile private var receivedHello: SyncHello? = null

    @Volatile private var authenticated = false
    private var abortSent = false
    private var abortReason: String? = null

    private val frameReader = SyncFrameReader()

    // The sender and receiver loops both write to the ONE full-duplex link;
    // unordered concurrent writes would corrupt frames, so every outbound frame
    // takes this lock (the counterpart of the Dart transport's send chain).
    private val sendMutex = Mutex()

    /**
     * Runs the session and resolves with the report (both success and clean
     * abort produce a report; cancellation propagates as cancellation).
     */
    suspend fun run(): SyncReport = coroutineScope {
        val readerJob = launch { readLoop() }
        try {
            val peer = handshake()
            authenticate(peer)
            // Only now may record frames be processed. Batch/ack/sendDone
            // arriving before this are a protocol violation (a peer that hasn't
            // proved the code trying to push data) and abort the session — see
            // [dispatch].
            authenticated = true
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
        send(
            SyncFrameType.HELLO,
            SyncHello(
                protocolVersion = SYNC_PROTOCOL_VERSION,
                deviceName = config.deviceName,
                hcProviderVersion = config.hcProviderVersion,
                supportedTypes = config.supportedTypes,
                nonce = nonce,
            ).encode(),
        )
        val peer = await(peerHello, config.handshakeTimeoutMillis, "timed out waiting for peer hello")
        if (peer.protocolVersion != SYNC_PROTOCOL_VERSION) {
            throw SyncAborted(
                "incompatible protocol version ${peer.protocolVersion} " +
                    "(this app speaks $SYNC_PROTOCOL_VERSION)",
            )
        }
        return peer
    }

    private suspend fun authenticate(peer: SyncHello) {
        emit(phase = SyncPhase.AUTHENTICATING)
        // A peer that echoes OUR nonce back could reflect our own proof to pass
        // auth without knowing the code (host==guest nonce makes both proofs
        // identical). Two independently-generated 256-bit nonces are never
        // equal by chance, so a match means reflection — reject it.
        if (constantTimeEquals(peer.nonce, nonce)) {
            throw SyncAborted("peer reflected our nonce")
        }
        // Fix nonce order by role so both phones derive the same key.
        val hostNonce = if (config.role == SyncRole.HOST) nonce else peer.nonce
        val guestNonce = if (config.role == SyncRole.HOST) peer.nonce else nonce
        val sessionKey = deriveSessionKey(config.code, hostNonce, guestNonce)
        val myRole = if (config.role == SyncRole.HOST) AUTH_ROLE_HOST else AUTH_ROLE_GUEST
        val peerRole = if (config.role == SyncRole.HOST) AUTH_ROLE_GUEST else AUTH_ROLE_HOST
        // Prove over the peer's nonce with OUR role; verify their proof over
        // our nonce with THEIR role. The role binding is what defeats
        // reflection.
        val myProof = computeAuthProof(sessionKey, challengeNonce = peer.nonce, roleByte = myRole)
        send(SyncFrameType.AUTH, SyncAuthProof(myProof).encode())
        val peerProof = await(peerAuth, config.handshakeTimeoutMillis, "timed out waiting for auth")
        val expected = computeAuthProof(sessionKey, challengeNonce = nonce, roleByte = peerRole)
        if (!constantTimeEquals(peerProof.proof, expected)) {
            throw SyncAborted("pairing code did not match")
        }
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
        // Seed the dedup baseline from a keys-only pass over our own records.
        // A peer record is a duplicate iff we already hold one with the same
        // content fingerprint, so our own keys ARE that set — dedup is then an
        // in-memory lookup instead of a per-batch Health Connect re-read. It is
        // also stricter: it catches records we hold natively (no
        // clientRecordId), which a clientRecordId-only check would miss and
        // re-import. The baseline must be COMPLETE before the receiver writes
        // its first incoming batch, which is why this pass runs before the
        // loops rather than folding into the sender's stream.
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
        // Stop-and-wait per chunk, and the chunk is dropped before the store
        // produces the next one — peak sender memory is one batch, not the
        // window.
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
            // Defense in depth for a link that goes silent WITHOUT a disconnect
            // event (out of range, a wedged peer): a gap longer than the batch
            // timeout during an active stop-and-wait exchange means the peer is
            // gone. A clean close (sendDone) closes the channel first, so this
            // never fires on a healthy sync.
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
                // seenKeys holds every record we already had (seeded from
                // readKeys) plus everything written earlier this session, so
                // this one lookup covers both cross-device and within-session
                // dedup.
                val keyHash = keyHasher.hash(item.key)
                if (keyHash in seenKeys) {
                    report.recordReceived(item.recordType, duplicate = true)
                } else {
                    fresh += item
                    seenKeys += keyHash
                }
            }
            // Count `imported` from what actually landed, not from what we
            // tried to write — a failed batch insert must not be reported as
            // imported.
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
                        peerHello.complete(hello)
                    }
                }
                SyncFrameType.AUTH -> {
                    if (!peerAuth.isCompleted) {
                        peerAuth.complete(SyncAuthProof.decode(frame.payload))
                    }
                }
                SyncFrameType.BATCH,
                SyncFrameType.BATCH_ACK,
                SyncFrameType.SEND_DONE,
                -> {
                    // Record frames before auth are a protocol violation —
                    // reject without decoding (a gzip batch is never inflated
                    // for an unauthenticated peer, closing the pre-auth OOM
                    // vector).
                    if (!authenticated) {
                        failPending("${frame.type.wireName} before authentication")
                        return
                    }
                    when (frame.type) {
                        SyncFrameType.BATCH ->
                            incomingBatches.trySend(SyncBatch.decode(frame.payload))
                        SyncFrameType.BATCH_ACK -> {
                            val ack = pendingAck
                            pendingAck = null
                            ack?.complete(SyncBatchAck.decode(frame.payload).seq)
                        }
                        SyncFrameType.SEND_DONE -> incomingBatches.close()
                        else -> Unit
                    }
                }
                SyncFrameType.ABORT -> handleAbort(SyncAbort.decode(frame.payload).reason)
            }
        } catch (e: Exception) {
            // A frame from a hostile/buggy peer can parse as JSON but have the
            // wrong shape (a missing field, an unknown enum, a wrong-typed
            // value). Turn any decode failure into a clean session abort with a
            // reason instead of letting it escape the read loop.
            failPending("bad ${frame.type.wireName} frame: ${e.message}")
        }
    }

    private fun handleAbort(reason: String) {
        if (abortReason == null) abortReason = reason
        failPending("peer aborted: $reason")
    }

    /** Propagates a fatal condition to whichever awaiter is live so [run] unwinds. */
    private fun failPending(reason: String) {
        val error = SyncAborted(reason)
        peerHello.completeExceptionally(error)
        peerAuth.completeExceptionally(error)
        val ack = pendingAck
        pendingAck = null
        ack?.completeExceptionally(error)
        // Closing the batch channel with a cause makes a parked receiver loop
        // rethrow it; pre-exchange there is no receiver and the deferreds above
        // are what unblock the handshake/auth awaits.
        incomingBatches.close(error)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun send(type: SyncFrameType, payload: ByteArray) {
        sendMutex.withLock {
            try {
                transport.send(SyncFrame(type, payload).encode())
            } catch (e: CancellationException) {
                throw e
            } catch (e: SyncAborted) {
                throw e
            } catch (e: Exception) {
                // A dead carrier (socket closed mid-write) must surface as a
                // clean session abort with a report, not an escaping I/O error.
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
    }
