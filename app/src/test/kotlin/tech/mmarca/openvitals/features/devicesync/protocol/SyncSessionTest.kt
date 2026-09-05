package tech.mmarca.openvitals.features.devicesync.protocol

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        code: String = "424242",
        types: List<String> = listOf("StepsRecord", "HeartRateRecord"),
        selected: List<String>? = null,
    ) = SyncSessionConfig(
        role = role,
        code = code,
        deviceName = if (role == SyncRole.HOST) "Host phone" else "Guest phone",
        supportedTypes = types,
        selectedTypes = selected,
        // Fixed, distinct nonces keep the test deterministic.
        nonce = ByteArray(SYNC_NONCE_BYTES) { if (role == SyncRole.HOST) 0x11 else 0x22 },
        handshakeTimeoutMillis = 5_000,
        batchTimeoutMillis = 5_000,
        batchSize = 2,
    )

    private suspend fun kotlinx.coroutines.CoroutineScope.runPair(
        hostStore: SyncRecordStore,
        guestStore: SyncRecordStore,
        hostCode: String = "424242",
        guestCode: String = "424242",
        hostSelected: List<String>? = null,
        guestSelected: List<String>? = null,
    ): Pair<SyncReport, SyncReport> {
        val (hostPipe, guestPipe) = SyncPipe.create()
        val host = SyncSession(
            transport = hostPipe,
            store = hostStore,
            config = configFor(SyncRole.HOST, code = hostCode, selected = hostSelected),
        )
        val guest = SyncSession(
            transport = guestPipe,
            store = guestStore,
            config = configFor(SyncRole.GUEST, code = guestCode, selected = guestSelected),
        )
        val reports = awaitAll(async { host.run() }, async { guest.run() })
        return reports[0] to reports[1]
    }

    /** Drives one real session against a manual endpoint sending whatever raw frames [attack] dictates. */
    private suspend fun kotlinx.coroutines.CoroutineScope.runAgainstAttacker(
        attack: suspend (SyncByteTransport) -> Unit,
    ): SyncReport {
        val (hostPipe, attackerPipe) = SyncPipe.create()
        val host = SyncSession(
            transport = hostPipe,
            store = FakeRecordStore(listOf(item("a"))),
            config = configFor(SyncRole.HOST),
        )
        val report = async { host.run() }
        attack(attackerPipe)
        return report.await()
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

    // Authentication.

    @Test
    fun `mismatched codes abort both sides before any data moves`() = runTest {
        val hostStore = FakeRecordStore(listOf(item("a")))
        val guestStore = FakeRecordStore(listOf(item("b")))

        val (hostReport, guestReport) = runPair(
            hostStore,
            guestStore,
            hostCode = "111111",
            guestCode = "222222",
        )

        assertFalse(hostReport.completed)
        assertFalse(guestReport.completed)
        assertTrue(hostReport.abortReason.orEmpty().contains("code"))
        // No records crossed.
        assertEquals(setOf("a"), hostStore.keys)
        assertEquals(setOf("b"), guestStore.keys)
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
    fun `a record frame before authentication aborts the session`() = runTest {
        val report = runAgainstAttacker { attacker ->
            // No handshake/auth — just push a batch straight away.
            attacker.send(
                SyncFrame(SyncFrameType.BATCH, SyncBatch(seq = 1, items = emptyList()).encode())
                    .encode(),
            )
        }

        assertFalse(report.completed)
        assertTrue(report.abortReason.orEmpty().contains("before authentication"))
    }

    @Test
    fun `a malformed hello frame aborts cleanly instead of crashing`() = runTest {
        // Valid JSON, wrong shape: `v` is a string where an int is required.
        val badHello = "{\"v\":\"not-an-int\"}".toByteArray(Charsets.UTF_8)

        val report = runAgainstAttacker { attacker ->
            attacker.send(SyncFrame(SyncFrameType.HELLO, badHello).encode())
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
