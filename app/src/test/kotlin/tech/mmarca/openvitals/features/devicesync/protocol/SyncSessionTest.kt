package tech.mmarca.openvitals.features.devicesync.protocol

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The full session state machine over an in-memory [SyncPipe], no Bluetooth. */
class SyncSessionTest {

    /** In-memory stand-in for Health Connect: a keyed set of records. Reads snapshot at collection; `writeItems` upserts. */
    private class FakeRecordStore(initial: Iterable<SyncItem> = emptyList()) : SyncRecordStore {
        private val byKey = linkedMapOf<String, SyncItem>()

        init {
            initial.forEach { byKey[it.key] = it }
        }

        val keys: Set<String> get() = byKey.keys.toSet()

        override fun readKeys(types: Set<String>): Flow<String> = flow {
            byKey.values.filter { it.recordType in types }.forEach { emit(it.key) }
        }

        override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> =
            flow {
                byKey.values.filter { it.recordType in types }
                    .chunked(chunkSize)
                    .forEach { emit(it) }
            }

        override suspend fun writeItems(items: List<SyncItem>): Set<String> {
            items.forEach { byKey[it.key] = it }
            return items.map { it.key }.toSet()
        }
    }

    /** A store whose reads yield a caller-specified key list (allows dup keys). */
    private class DupReadingStore(private val keys: List<String>) : SyncRecordStore {
        override fun readKeys(types: Set<String>): Flow<String> = flow {
            keys.forEach { emit(it) }
        }

        override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> =
            flow {
                keys.map { item(it) }.chunked(chunkSize).forEach { emit(it) }
            }

        override suspend fun writeItems(items: List<SyncItem>): Set<String> =
            items.map { it.key }.toSet()
    }

    /** A store that reads nothing and fails every write. */
    private class WriteFailingStore : SyncRecordStore {
        override fun readKeys(types: Set<String>): Flow<String> = emptyFlow()
        override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> =
            emptyFlow()
        override suspend fun writeItems(items: List<SyncItem>): Set<String> = emptySet()
    }

    private fun configFor(
        role: SyncRole,
        confirmCode: suspend (String) -> Boolean = { true },
        types: List<String> = listOf("StepsRecord", "HeartRateRecord"),
        selected: List<String>? = null,
    ) = SyncSessionConfig(
        role = role,
        confirmCode = confirmCode,
        deviceName = if (role == SyncRole.HOST) "Host phone" else "Guest phone",
        supportedTypes = types,
        selectedTypes = selected,
        // Fixed, distinct nonces keep the test deterministic.
        nonce = ByteArray(SYNC_NONCE_BYTES) { if (role == SyncRole.HOST) 0x11 else 0x22 },
        handshakeTimeoutMillis = 5_000,
        confirmTimeoutMillis = 5_000,
        batchTimeoutMillis = 5_000,
        batchSize = 2,
    )

    private suspend fun CoroutineScope.runPair(
        hostStore: SyncRecordStore,
        guestStore: SyncRecordStore,
        hostConfirms: suspend (String) -> Boolean = { true },
        guestConfirms: suspend (String) -> Boolean = { true },
        hostSelected: List<String>? = null,
        guestSelected: List<String>? = null,
    ): Pair<SyncReport, SyncReport> {
        val (hostPipe, guestPipe) = SyncPipe.create()
        val host = SyncSession(
            transport = hostPipe,
            store = hostStore,
            config = configFor(SyncRole.HOST, confirmCode = hostConfirms, selected = hostSelected),
        )
        val guest = SyncSession(
            transport = guestPipe,
            store = guestStore,
            config = configFor(SyncRole.GUEST, confirmCode = guestConfirms, selected = guestSelected),
        )
        val reports = awaitAll(async { host.run() }, async { guest.run() })
        return reports[0] to reports[1]
    }

    /** Drives one real session against a manual endpoint sending whatever raw frames [attack] dictates. */
    private suspend fun CoroutineScope.runAgainstAttacker(
        role: SyncRole = SyncRole.HOST,
        confirmCode: suspend (String) -> Boolean = { true },
        attack: suspend (ManualPeer) -> Unit,
    ): SyncReport {
        val (pipe, attackerPipe) = SyncPipe.create()
        val session = SyncSession(
            transport = pipe,
            store = FakeRecordStore(listOf(item("a"))),
            config = configFor(role, confirmCode = confirmCode),
        )
        val report = async { session.run() }
        attack(ManualPeer(attackerPipe))
        return report.await()
    }

    /** A peer driven by hand, one frame at a time. */
    private class ManualPeer(private val transport: SyncByteTransport) {
        private val reader = SyncFrameReader()
        private val frames = ArrayDeque<SyncFrame>()
        val hello: ByteArray = SyncHello(
            protocolVersion = SYNC_PROTOCOL_VERSION,
            deviceName = "Attacker",
            hcProviderVersion = null,
            supportedTypes = listOf("StepsRecord"),
            nonce = ByteArray(SYNC_NONCE_BYTES) { 0x33 },
        ).encode()

        suspend fun send(type: SyncFrameType, payload: ByteArray) =
            transport.send(SyncFrame(type, payload).encode())

        suspend fun next(): SyncFrame {
            while (frames.isEmpty()) frames += reader.addChunk(transport.inbound.receive())
            return frames.removeFirst()
        }

        suspend fun next(type: SyncFrameType): SyncFrame =
            next().also { assertEquals(type, it.type) }

        /** Every frame that has arrived and was not read yet. Does not wait. */
        fun arrived(): List<SyncFrame> {
            while (true) frames += reader.addChunk(transport.inbound.tryReceive().getOrNull() ?: break)
            return frames.toList()
        }

        /** Plays an honest guest up to the point where both sides hold the keys. */
        suspend fun exchangeKeysAsGuest(): SyncFrameCipher {
            val own = generateSyncKeyPair()
            send(SyncFrameType.HELLO, hello)
            val hostHello = next(SyncFrameType.HELLO).payload
            next(SyncFrameType.KEY_COMMIT) // An honest guest would keep it.
            send(SyncFrameType.KEY_SHARE, SyncKeyShare(own.publicKey).encode())
            val reveal = SyncKeyReveal.decode(next(SyncFrameType.KEY_REVEAL).payload)
            val transcript = syncTranscriptHash(hostHello, hello, reveal.publicKey, own.publicKey, reveal.salt)
            return SyncFrameCipher.forRole(SyncRole.GUEST, deriveSyncSessionKeys(own, reveal.publicKey, transcript))
        }
    }

    /** Passes frames from [from] to [to], letting [change] rewrite each one. */
    private fun CoroutineScope.relay(
        from: SyncByteTransport,
        to: SyncByteTransport,
        change: (SyncFrame) -> SyncFrame = { it },
    ) = launch {
        val reader = SyncFrameReader()
        for (chunk in from.inbound) {
            reader.addChunk(chunk).forEach { to.send(change(it).encode()) }
        }
    }

    // Bidirectional merge.

    @Test
    fun `each side imports what it lacked and skips shared records`() = runTest {
        // Shared key 'c'; host-only a,b; guest-only d,e.
        val hostStore = FakeRecordStore(listOf(item("a"), item("b"), item("c")))
        val guestStore = FakeRecordStore(listOf(item("c"), item("d"), item("e")))

        val (hostReport, guestReport) = runPair(hostStore, guestStore)

        // Both converge to the union.
        assertEquals(setOf("a", "b", "c", "d", "e"), hostStore.keys)
        assertEquals(setOf("a", "b", "c", "d", "e"), guestStore.keys)

        assertTrue(hostReport.completed)
        assertEquals(3, hostReport.itemsSent) // a,b,c
        assertEquals(3, hostReport.itemsReceived) // c,d,e
        assertEquals(2, hostReport.imported) // d,e
        assertEquals(1, hostReport.duplicateSkipped) // c

        assertEquals(2, guestReport.imported) // a,b
        assertEquals(1, guestReport.duplicateSkipped) // c
        assertEquals("Host phone", guestReport.peerDeviceName)
    }

    @Test
    fun `per-type summaries split the tallies correctly`() = runTest {
        val hostStore = FakeRecordStore(
            listOf(item("s1", type = "StepsRecord"), item("h1", type = "HeartRateRecord")),
        )
        val guestStore = FakeRecordStore(
            listOf(
                item("s1", type = "StepsRecord"), // dup vs host
                item("h2", type = "HeartRateRecord"),
            ),
        )

        val (hostReport, _) = runPair(hostStore, guestStore)

        val steps = hostReport.typeSummaries.first { it.recordType == "StepsRecord" }
        val heart = hostReport.typeSummaries.first { it.recordType == "HeartRateRecord" }
        assertEquals(1, steps.received)
        assertEquals(1, steps.duplicateSkipped) // s1 already on host
        assertEquals(1, heart.imported) // h2 new
    }

    // Idempotency.

    @Test
    fun `a second sync writes nothing new`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a"), item("b")))
        val guestStore = FakeRecordStore(listOf(item("b"), item("c")))

        runPair(hostStore, guestStore)
        // Both now hold {a,b,c}. Re-run.
        val (hostReport, guestReport) = runPair(hostStore, guestStore)

        assertEquals(0, hostReport.imported)
        assertEquals(0, guestReport.imported)
        assertEquals(3, hostReport.itemsReceived)
        assertEquals(3, hostReport.duplicateSkipped)
        assertEquals(setOf("a", "b", "c"), hostStore.keys)
        assertEquals(setOf("a", "b", "c"), guestStore.keys)
    }

    // Within-session dedup.

    @Test
    fun `a key sent twice in one direction is written once`() = runTest {
        // The host reads a duplicate key 'x' across two batches: ['x','x'] then ['y'].
        val hostStore = DupReadingStore(listOf("x", "x", "y"))
        val guestStore = FakeRecordStore()

        val (hostReport, guestReport) = runPair(hostStore, guestStore)

        assertEquals(setOf("x", "y"), guestStore.keys) // written once each
        assertEquals(2, guestReport.imported)
        assertEquals(1, guestReport.duplicateSkipped) // the repeated 'x'
        assertTrue(hostReport.completed)
    }

    // The compared code.

    @Test
    fun `both phones show the same six digits`() = runTest {
        var hostCode = ""
        var guestCode = ""

        val (hostReport, guestReport) = runPair(
            FakeRecordStore(listOf(item("a"))),
            FakeRecordStore(),
            hostConfirms = { hostCode = it; true },
            guestConfirms = { guestCode = it; true },
        )

        assertTrue(hostReport.completed)
        assertTrue(guestReport.completed)
        assertTrue(hostCode, Regex("^\\d{6}$").matches(hostCode))
        assertEquals(hostCode, guestCode)
    }

    @Test
    fun `a user who says the codes differ ends both sides before any data moves`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore(listOf(item("b")))

        val (hostReport, guestReport) = runPair(hostStore, guestStore, guestConfirms = { false })

        assertFalse(hostReport.completed)
        assertFalse(guestReport.completed)
        assertEquals(CODES_DIFFER_REASON, guestReport.abortReason)
        assertTrue(hostReport.abortReason.orEmpty().contains(CODES_DIFFER_REASON))
        // No records crossed.
        assertEquals(setOf("a"), hostStore.keys)
        assertEquals(setOf("b"), guestStore.keys)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `no record moves until both users confirmed`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore(listOf(item("b")))
        val hostAnswer = CompletableDeferred<Boolean>()
        val (hostPipe, guestPipe) = SyncPipe.create()
        val host = SyncSession(hostPipe, hostStore, configFor(SyncRole.HOST, confirmCode = { hostAnswer.await() }))
        val guest = SyncSession(guestPipe, guestStore, configFor(SyncRole.GUEST))

        val runs = listOf(async { host.run() }, async { guest.run() })
        // The guest has confirmed. The host's user is still looking at the code.
        runCurrent()

        assertEquals(setOf("a"), hostStore.keys)
        assertEquals(setOf("b"), guestStore.keys)

        hostAnswer.complete(true)
        val reports = runs.awaitAll()

        assertTrue(reports.all { it.completed })
        assertEquals(setOf("a", "b"), hostStore.keys)
        assertEquals(setOf("a", "b"), guestStore.keys)
    }

    @Test
    fun `a user who never answers ends the session`() = runTest {
        val (hostReport, _) = runPair(
            FakeRecordStore(listOf(item("a"))),
            FakeRecordStore(),
            hostConfirms = { awaitCancellation() },
        )

        assertFalse(hostReport.completed)
        assertTrue(hostReport.abortReason.orEmpty().contains("in time"))
    }

    @Test
    fun `someone in the middle makes the two phones show different codes`() = runTest {
        // The attacker runs a full, honest-looking session with each phone.
        var hostCode = ""
        var guestCode = ""
        val (hostPipe, attackerAsGuestPipe) = SyncPipe.create()
        val (attackerAsHostPipe, guestPipe) = SyncPipe.create()
        val sessions = listOf(
            SyncSession(hostPipe, FakeRecordStore(), configFor(SyncRole.HOST, confirmCode = { hostCode = it; false })),
            SyncSession(attackerAsGuestPipe, FakeRecordStore(), configFor(SyncRole.GUEST)),
            SyncSession(attackerAsHostPipe, FakeRecordStore(), configFor(SyncRole.HOST)),
            SyncSession(guestPipe, FakeRecordStore(), configFor(SyncRole.GUEST, confirmCode = { guestCode = it; false })),
        )

        sessions.map { async { it.run() } }.awaitAll()

        // The attacker cannot steer either code. They agree by chance once in a million runs.
        assertTrue(hostCode.isNotEmpty() && guestCode.isNotEmpty())
        assertNotEquals(hostCode, guestCode)
    }

    @Test
    fun `a changed hello makes the two phones show different keys`() = runTest {
        // Someone on the link rewrites the guest's hello but passes the key frames through.
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore(listOf(item("b")))
        val (hostPipe, towardsHost) = SyncPipe.create()
        val (towardsGuest, guestPipe) = SyncPipe.create()
        val relays = listOf(
            relay(towardsHost, towardsGuest),
            relay(towardsGuest, towardsHost) { frame ->
                if (frame.type != SyncFrameType.HELLO) return@relay frame
                val hello = SyncHello.decode(frame.payload)
                SyncFrame(
                    SyncFrameType.HELLO,
                    SyncHello(hello.protocolVersion, hello.deviceName, null, listOf("StepsRecord"), hello.nonce).encode(),
                )
            },
        )
        val host = SyncSession(hostPipe, hostStore, configFor(SyncRole.HOST))
        val guest = SyncSession(guestPipe, guestStore, configFor(SyncRole.GUEST))

        // Both users tap "match" without looking. The keys still differ, so the first sealed frame fails.
        val reports = listOf(async { host.run() }, async { guest.run() }).awaitAll()
        relays.forEach { it.cancel() }

        assertTrue(reports.none { it.completed })
        assertEquals(setOf("a"), hostStore.keys)
        assertEquals(setOf("b"), guestStore.keys)
    }

    @Test
    fun `a changed record frame ends the session and is not written`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore()
        val (hostPipe, towardsHost) = SyncPipe.create()
        val (towardsGuest, guestPipe) = SyncPipe.create()
        val relays = listOf(
            relay(towardsGuest, towardsHost),
            relay(towardsHost, towardsGuest) { frame ->
                if (frame.type != SyncFrameType.BATCH) return@relay frame
                SyncFrame(frame.type, frame.payload.copyOf().also { it[0] = (it[0] + 1).toByte() })
            },
        )
        val host = SyncSession(hostPipe, hostStore, configFor(SyncRole.HOST))
        val guest = SyncSession(guestPipe, guestStore, configFor(SyncRole.GUEST))

        val reports = listOf(async { host.run() }, async { guest.run() }).awaitAll()
        relays.forEach { it.cancel() }

        assertFalse(reports[1].completed)
        assertTrue(reports[1].abortReason.orEmpty().contains("integrity"))
        assertEquals(emptySet<String>(), guestStore.keys)
    }

    // Two users, two taps.

    @Test
    fun `the phone that starts first waits for the other user`() = runTest {
        // Each session starts on its own user's tap. The second user takes two minutes over the pickers.
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore(listOf(item("b")))
        val (hostPipe, guestPipe) = SyncPipe.create()
        val host = SyncSession(hostPipe, hostStore, configFor(SyncRole.HOST))
        val guest = SyncSession(guestPipe, guestStore, configFor(SyncRole.GUEST))

        val reports = listOf(
            async { host.run() },
            async {
                delay(120_000)
                guest.run()
            },
        ).awaitAll()

        assertTrue(reports.all { it.completed })
        assertEquals(setOf("a", "b"), guestStore.keys)
    }

    @Test
    fun `a phone whose peer never starts gives up and says so`() = runTest {
        val report = runAgainstAttacker { }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("did not start the sync in time"))
    }

    // Link failure.

    @Test
    fun `a dropped transport ends the session as an abort`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a"), item("b")))
        val guestStore = FakeRecordStore(listOf(item("c")))

        val (hostPipe, guestPipe) = SyncPipe.create()
        val host = SyncSession(
            transport = hostPipe,
            store = hostStore,
            config = configFor(SyncRole.HOST),
        )
        val guest = SyncSession(
            transport = guestPipe,
            store = guestStore,
            config = configFor(SyncRole.GUEST),
        )

        val hostRun = async { host.run() }
        val guestRun = async { guest.run() }
        // Drop the link before the sessions run: both see their inbound close mid-handshake.
        guestPipe.close()

        val reports = awaitAll(hostRun, guestRun)
        assertFalse(reports[0].completed)
        assertFalse(reports[1].completed)
        assertNotNull(reports[0].abortReason)
        // No records crossed a dead link.
        assertEquals(setOf("c"), guestStore.keys)
    }

    // Read failure mid-stream.

    @Test
    fun `a store read failure aborts both sides with the reason`() = runTest {
        // The host's stream dies after one chunk, as a rate-limited read would.
        // The session must abort with the reason rather than claim a complete transfer.
        val hostStore = object : SyncRecordStore {
            override fun readKeys(types: Set<String>): Flow<String> = emptyFlow()
            override fun readItemChunks(types: Set<String>, chunkSize: Int): Flow<List<SyncItem>> =
                flow {
                    emit(listOf(item("a")))
                    throw SyncAborted("reading StepsRecord from Health Connect failed: rate limited")
                }
            override suspend fun writeItems(items: List<SyncItem>): Set<String> =
                items.map { it.key }.toSet()
        }
        val guestStore = FakeRecordStore()

        val (hostReport, guestReport) = runPair(hostStore, guestStore)

        assertFalse(hostReport.completed)
        assertTrue(hostReport.abortReason.orEmpty().contains("rate limited"))
        assertFalse(guestReport.completed)
        assertTrue(guestReport.abortReason.orEmpty().contains("rate limited"))
    }

    // Type negotiation.

    @Test
    fun `only the intersection of supported+selected types syncs`() = runTest {
        val hostStore = FakeRecordStore(
            listOf(item("s1", type = "StepsRecord"), item("h1", type = "HeartRateRecord")),
        )
        val guestStore = FakeRecordStore()

        // The guest selects only StepsRecord; the host still sends everything for negotiated types.
        val (hostReport, _) = runPair(
            hostStore,
            guestStore,
            guestSelected = listOf("StepsRecord"),
        )

        assertTrue(hostReport.completed)
        assertTrue("s1" in guestStore.keys)
    }

    // Write accounting.

    @Test
    fun `a received record whose write fails is not counted as imported`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a"), item("b")))
        val guestStore = WriteFailingStore()

        val (_, guestReport) = runPair(hostStore, guestStore)

        // Received but not written, so the report must not overcount.
        assertEquals(2, guestReport.itemsReceived)
        assertEquals(0, guestReport.imported)
    }

    // Hostile peer.

    @Test
    fun `a record frame before the key exchange aborts the session`() = runTest {
        val report = runAgainstAttacker { attacker ->
            // No handshake at all: just push a batch straight away.
            attacker.send(SyncFrameType.BATCH, SyncBatch(seq = 1, items = emptyList()).encode())
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("before the key exchange"))
    }

    @Test
    fun `a record frame before authentication aborts the session`() = runTest {
        // The peer holds real keys, but this phone's user has not confirmed the code yet.
        val report = runAgainstAttacker(confirmCode = { awaitCancellation() }) { attacker ->
            val cipher = attacker.exchangeKeysAsGuest()
            attacker.send(SyncFrameType.CONFIRM, cipher.seal(SyncFrameType.CONFIRM, ByteArray(0)))
            val batch = SyncBatch(seq = 1, items = listOf(item("evil"))).encode()
            attacker.send(SyncFrameType.BATCH, cipher.seal(SyncFrameType.BATCH, batch))
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("before authentication"))
    }

    @Test
    fun `a host key that does not match its commitment aborts the session`() = runTest {
        val report = runAgainstAttacker(role = SyncRole.GUEST) { attacker ->
            attacker.send(SyncFrameType.HELLO, attacker.hello)
            val committed = generateSyncKeyPair()
            val salt = ByteArray(SYNC_NONCE_BYTES) { 0x44 }
            attacker.send(
                SyncFrameType.KEY_COMMIT,
                SyncKeyCommit(commitToHostKey(committed.publicKey, salt)).encode(),
            )
            // Wait for the guest's key, then reveal a key picked after seeing it.
            while (attacker.next().type != SyncFrameType.KEY_SHARE) Unit
            attacker.send(
                SyncFrameType.KEY_REVEAL,
                SyncKeyReveal(generateSyncKeyPair().publicKey, salt).encode(),
            )
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("commitment"))
    }

    @Test
    fun `a host that commits twice aborts the session`() = runTest {
        // A second commitment, made after seeing the guest's key, would let a fake host pick the code.
        val report = runAgainstAttacker(role = SyncRole.GUEST) { attacker ->
            attacker.send(SyncFrameType.HELLO, attacker.hello)
            val salt = ByteArray(SYNC_NONCE_BYTES) { 0x44 }
            val first = generateSyncKeyPair()
            attacker.send(SyncFrameType.KEY_COMMIT, SyncKeyCommit(commitToHostKey(first.publicKey, salt)).encode())
            while (attacker.next().type != SyncFrameType.KEY_SHARE) Unit
            val steered = generateSyncKeyPair()
            attacker.send(SyncFrameType.KEY_COMMIT, SyncKeyCommit(commitToHostKey(steered.publicKey, salt)).encode())
            attacker.send(SyncFrameType.KEY_REVEAL, SyncKeyReveal(steered.publicKey, salt).encode())
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("unexpected keyCommit"))
    }

    @Test
    fun `a commitment before the hello aborts the session`() = runTest {
        val report = runAgainstAttacker(role = SyncRole.GUEST) { attacker ->
            attacker.send(
                SyncFrameType.KEY_COMMIT,
                SyncKeyCommit(commitToHostKey(generateSyncKeyPair().publicKey, ByteArray(SYNC_NONCE_BYTES))).encode(),
            )
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("unexpected keyCommit"))
    }

    @Test
    fun `a guest that sends a second key aborts the session`() = runTest {
        val report = runAgainstAttacker(confirmCode = { awaitCancellation() }) { attacker ->
            attacker.send(SyncFrameType.HELLO, attacker.hello)
            attacker.next(SyncFrameType.HELLO)
            attacker.next(SyncFrameType.KEY_COMMIT)
            attacker.send(SyncFrameType.KEY_SHARE, SyncKeyShare(generateSyncKeyPair().publicKey).encode())
            attacker.send(SyncFrameType.KEY_SHARE, SyncKeyShare(generateSyncKeyPair().publicKey).encode())
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("unexpected keyShare"))
    }

    @Test
    fun `the host keeps its key back until the guest has sent one`() = runTest {
        // Revealing early would let a fake guest choose its key after seeing the host's.
        var seen = emptyList<SyncFrame>()
        val report = runAgainstAttacker { attacker ->
            attacker.send(SyncFrameType.HELLO, attacker.hello)
            attacker.next(SyncFrameType.HELLO)
            attacker.next(SyncFrameType.KEY_COMMIT)
            // Send nothing more. The host must give up without revealing.
            seen = attacker.arrived()
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("timed out waiting for the peer key"))
        assertTrue(seen.none { it.type == SyncFrameType.KEY_REVEAL })
    }

    @Test
    fun `a second confirm aborts the session`() = runTest {
        val report = runAgainstAttacker(confirmCode = { awaitCancellation() }) { attacker ->
            val cipher = attacker.exchangeKeysAsGuest()
            attacker.send(SyncFrameType.CONFIRM, cipher.seal(SyncFrameType.CONFIRM, ByteArray(0)))
            attacker.send(SyncFrameType.CONFIRM, cipher.seal(SyncFrameType.CONFIRM, ByteArray(0)))
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("confirm sent twice"))
    }

    @Test
    fun `a long abort reason from the peer is cut`() = runTest {
        // An abort is not sealed, so its text is whatever the sender likes.
        val report = runAgainstAttacker { attacker ->
            attacker.send(SyncFrameType.ABORT, SyncAbort("x".repeat(100_000)).encode())
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().length < SyncAbort.MAX_REASON_CHARS + 50)
    }

    @Test
    fun `a host that reveals before it committed aborts the session`() = runTest {
        val report = runAgainstAttacker(role = SyncRole.GUEST) { attacker ->
            attacker.send(SyncFrameType.HELLO, attacker.hello)
            attacker.send(
                SyncFrameType.KEY_REVEAL,
                SyncKeyReveal(generateSyncKeyPair().publicKey, ByteArray(SYNC_NONCE_BYTES)).encode(),
            )
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("unexpected keyReveal"))
    }

    @Test
    fun `a peer on the old protocol is refused`() = runTest {
        val report = runAgainstAttacker { attacker ->
            attacker.send(
                SyncFrameType.HELLO,
                SyncHello(1, "Old phone", null, listOf("StepsRecord"), ByteArray(SYNC_NONCE_BYTES) { 0x33 }).encode(),
            )
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("incompatible protocol version 1"))
    }

    @Test
    fun `a malformed hello frame aborts cleanly instead of crashing`() = runTest {
        // Valid JSON, wrong shape: `v` is a string where an int is required.
        val badHello = "{\"v\":\"not-an-int\"}".toByteArray(Charsets.UTF_8)

        val report = runAgainstAttacker { attacker ->
            attacker.send(SyncFrameType.HELLO, badHello)
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("hello"))
    }

    private companion object {
        fun item(key: String, type: String = "StepsRecord"): SyncItem = SyncItem(
            key = key,
            recordType = type,
            payload = key.toByteArray(Charsets.UTF_8),
        )
    }
}
