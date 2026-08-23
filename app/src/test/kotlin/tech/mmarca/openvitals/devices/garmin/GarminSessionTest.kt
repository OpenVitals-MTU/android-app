package tech.mmarca.openvitals.devices.garmin

import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Port of the Flutter build's `garmin_session_test.dart` — the sync happy
 * path and resilience suites, exercised against a fake watch that speaks the
 * real wire format (every frame through [GarminGfdiFrame.build]/`parse`).
 *
 * The Dart file's notification-conversation tests need the concrete
 * notifications handler, which is sub-milestone 7e; they move there with it.
 * The no-handler subscription behaviour (every current session) is covered
 * here.
 */
class GarminSessionTest {

    /**
     * A fake vívoactive 5 on the other end of the pipe.
     *
     * Speaks the real wire format — every frame it emits goes through
     * [GarminGfdiFrame.build] and everything it receives through `.parse` —
     * so the session is exercised against bytes, not against a mock of
     * itself.
     */
    private open class FakeWatch(
        /** fileIndex -> contents the watch will serve. */
        val files: Map<Int, ByteArray>,
        /** Indexes the watch answers with a non-OK download status. */
        val refuseIndexes: Set<Int> = emptySet(),
    ) {
        /** Frames the session sent us, decoded. */
        val received = mutableListOf<GarminGfdiFrame>()

        /** Frames to hand back to the session, in order. */
        val outbox = mutableListOf<ByteArray>()

        /**
         * Chunk size the watch streams at — small on purpose, so multi-chunk
         * reassembly is exercised.
         */
        var chunkSize = 8

        fun onFrame(frame: GarminGfdiFrame) {
            received.add(frame)
            when (frame.messageType) {
                GarminMessageId.RESPONSE -> Unit // Our ACKs; nothing to say back.
                GarminMessageId.SUPPORTED_FILE_TYPES_REQUEST ->
                    outbox.add(supportedTypes())
                GarminMessageId.DOWNLOAD_REQUEST -> {
                    val index = (frame.payload[0].toInt() and 0xFF) or
                        ((frame.payload[1].toInt() and 0xFF) shl 8)
                    startServing(index)
                }
                GarminMessageId.SET_FILE_FLAGS, GarminMessageId.SYSTEM_EVENT -> Unit
            }
        }

        protected open fun startServing(index: Int) {
            if (index in refuseIndexes) {
                outbox.add(downloadStatus(ok = false, size = 0))
                return
            }
            val content = files[index]
            if (content == null) {
                outbox.add(downloadStatus(ok = false, size = 0))
                return
            }

            outbox.add(downloadStatus(ok = true, size = content.size))
            // Stream it as chunks with a running CRC, exactly as the watch does.
            var offset = 0
            var runningCrc = 0
            while (offset < content.size) {
                val end = (offset + chunkSize).coerceAtMost(content.size)
                val chunk = content.copyOfRange(offset, end)
                runningCrc = GarminCrc.compute(chunk, initialCrc = runningCrc)
                outbox.add(fileChunk(offset = offset, crc = runningCrc, data = chunk))
                offset = end
            }
        }

        fun deviceInformation(): ByteArray {
            val w = GarminByteWriter()
                .writeShort(120) // protocol version
                .writeShort(4315) // product number
                .writeInt(123456) // unit number
                .writeShort(1915) // software version -> 19.15
                .writeShort(500) // max packet size
                .writeString("vívoactive 5")
                .writeString("vivoactive5")
                .writeString("vívoactive 5")
            return GarminGfdiFrame.build(GarminMessageId.DEVICE_INFORMATION, w.toBytes())
        }

        fun authNegotiation(): ByteArray {
            val w = GarminByteWriter()
                .writeByte(0x07)
                .writeInt(0x000000FF)
            return GarminGfdiFrame.build(GarminMessageId.AUTH_NEGOTIATION, w.toBytes())
        }

        private fun supportedTypes(): ByteArray {
            val w = GarminByteWriter()
                .writeShort(GarminMessageId.SUPPORTED_FILE_TYPES_REQUEST)
                .writeByte(GarminStatus.ACK.code)
                .writeByte(2)
                .writeByte(128)
                .writeByte(49)
                .writeString("SLEEP")
                .writeByte(128)
                .writeByte(32)
                .writeString("MONITOR")
            return GarminGfdiFrame.build(GarminMessageId.RESPONSE, w.toBytes())
        }

        protected fun downloadStatus(ok: Boolean, size: Int): ByteArray {
            val w = GarminByteWriter()
                .writeShort(GarminMessageId.DOWNLOAD_REQUEST)
                .writeByte(GarminStatus.ACK.code)
                .writeByte(if (ok) 0 else 1) // OK / INDEX_UNKNOWN
                .writeInt(size)
            return GarminGfdiFrame.build(GarminMessageId.RESPONSE, w.toBytes())
        }

        protected fun fileChunk(offset: Int, crc: Int, data: ByteArray): ByteArray {
            val w = GarminByteWriter()
                .writeByte(0)
                .writeShort(crc)
                .writeInt(offset)
                .writeBytes(data)
            return GarminGfdiFrame.build(GarminMessageId.FILE_TRANSFER_DATA, w.toBytes())
        }
    }

    /** A watch that corrupts the CRC of one file's first chunk. */
    private class CorruptingWatch(
        files: Map<Int, ByteArray>,
        val corruptIndex: Int,
    ) : FakeWatch(files) {
        override fun startServing(index: Int) {
            if (index != corruptIndex) {
                super.startServing(index)
                return
            }
            val content = files.getValue(index)
            outbox.add(downloadStatus(ok = true, size = content.size))
            // Deliberately wrong running CRC.
            outbox.add(fileChunk(offset = 0, crc = 0xDEAD, data = content))
        }
    }

    /**
     * A watch that serves an EMPTY listing first, then announces it holds
     * sleep data — the shape observed on a real vívoactive 5.
     */
    private class AnnouncingWatch(
        files: Map<Int, ByteArray>,
        /** Bit 26 is SLEEP in SynchronizationMessage.FileType. */
        val bitmask: Long = 1L shl 26,
    ) : FakeWatch(files) {

        private var announced = false

        override fun startServing(index: Int) {
            if (index == 0 && !announced) {
                // The first listing is empty; the announcement follows it.
                announced = true
                outbox.add(downloadStatus(ok = true, size = 0))
                outbox.add(synchronization())
                return
            }
            super.startServing(index)
        }

        private fun synchronization(): ByteArray {
            val w = GarminByteWriter()
                .writeByte(0) // TYPE_0
                .writeByte(8) // 8-byte bitmask
                .writeLong(bitmask)
            return GarminGfdiFrame.build(GarminMessageId.SYNCHRONIZATION, w.toBytes())
        }
    }

    /**
     * A watch that also emits the chatter a real vívoactive 5 sends during
     * the handshake — configuration, protobuf requests, notification
     * subscription — none of which this app answers with a response of its
     * own.
     */
    private class ChattyWatch(files: Map<Int, ByteArray>) : FakeWatch(files) {

        private var chattered = false

        override fun startServing(index: Int) {
            if (index == 0 && !chattered) {
                chattered = true
                // Queued BEFORE the listing, as observed on the device.
                // CONFIGURATION: [length][15 capability bytes], as the real
                // watch sends.
                outbox.add(
                    GarminGfdiFrame.build(
                        GarminMessageId.CONFIGURATION,
                        b(15) + ByteArray(15) { 0xAA.toByte() },
                    ),
                )
                outbox.add(GarminGfdiFrame.build(5043, b(0x8F, 0x03, 0x00, 0x00)))
                outbox.add(
                    GarminGfdiFrame.build(
                        GarminMessageId.NOTIFICATION_SUBSCRIPTION,
                        b(0x00, 0x00),
                    ),
                )
            }
            super.startServing(index)
        }
    }

    /** Builds a directory file listing entries as `(index, dataType, subType, number)`. */
    private fun directory(vararg entries: IntArray): ByteArray {
        val w = GarminByteWriter()
        for ((index, dataType, subType, number) in entries.map {
            listOf(it[0], it[1], it[2], it[3])
        }) {
            w.writeShort(index)
                .writeByte(dataType)
                .writeByte(subType)
                .writeShort(number)
                .writeByte(0)
                .writeByte(0)
                .writeInt(64)
                .writeInt(DIRECTORY_FILE_TIMESTAMP)
        }
        return w.toBytes()
    }

    /** Every listed file carries this date; a key needs one to exist at all. */
    private val DIRECTORY_FILE_TIMESTAMP = 1000L

    /** The dedup key [directory] gives a listed file of 64 bytes dated [DIRECTORY_FILE_TIMESTAMP]. */
    private fun key(dataType: Int, subType: Int, number: Int): String =
        "$dataType/$subType/$number/${GarminTime.GARMIN_EPOCH_SECONDS + DIRECTORY_FILE_TIMESTAMP}/64"

    private fun session(
        scope: CoroutineScope,
        watch: FakeWatch,
        alreadySynced: Set<String> = emptySet(),
        progress: MutableList<GarminSyncProgress>? = null,
        emptyGrace: Duration = Duration.ZERO,
        onFileDownloaded: (suspend (GarminDownloadedFile) -> Unit)? = null,
        send: (suspend (ByteArray) -> Unit)? = null,
        keepAnsweringAfterSync: Boolean = false,
    ): GarminSession = GarminSession(
        scope = scope,
        send = send ?: { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
        bluetoothName = "Pixel 6 Pro",
        manufacturer = "Google",
        model = "raven",
        alreadySynced = alreadySynced,
        onProgress = progress?.let { list -> { list.add(it) } },
        onFileDownloaded = onFileDownloaded,
        emptyGrace = emptyGrace,
        keepAnsweringAfterSync = keepAnsweringAfterSync,
    ).also { it.start() }

    /** Runs a session against [watch] until it settles, pumping the pipe both ways. */
    private suspend fun TestScope.runSync(
        watch: FakeWatch,
        alreadySynced: Set<String> = emptySet(),
        progress: MutableList<GarminSyncProgress>? = null,
    ): List<GarminDownloadedFile> {
        val session = session(
            this,
            watch,
            alreadySynced = alreadySynced,
            progress = progress,
        )
        pump(watch, session)
        return session.done.await()
    }

    /** Pumps queued frames only — no re-introduction; for follow-up traffic. */
    private suspend fun drain(watch: FakeWatch, session: GarminSession) {
        var guard = 0
        while (watch.outbox.isNotEmpty()) {
            if (guard++ > 10000) fail("conversation did not settle")
            session.handleFrame(GarminGfdiFrame.parse(watch.outbox.removeAt(0)))
        }
    }

    /** The watch speaks first; then the pipe is pumped until it settles. */
    private suspend fun pump(watch: FakeWatch, session: GarminSession) {
        watch.outbox.add(watch.deviceInformation())
        watch.outbox.add(watch.authNegotiation())
        var guard = 0
        while (watch.outbox.isNotEmpty()) {
            if (guard++ > 10000) fail("sync did not settle")
            val frame = watch.outbox.removeAt(0)
            session.handleFrame(GarminGfdiFrame.parse(frame))
        }
    }

    private fun payloadShort(frame: GarminGfdiFrame, at: Int = 0): Int =
        (frame.payload[at].toInt() and 0xFF) or
            ((frame.payload[at + 1].toInt() and 0xFF) shl 8)

    private fun responsesAbout(watch: FakeWatch, messageType: Int): List<GarminGfdiFrame> =
        watch.received
            .filter { it.messageType == GarminMessageId.RESPONSE }
            .filter { payloadShort(it) == messageType }

    private fun happyWatch(): FakeWatch {
        val sleep = ByteArray(20) { (0xA0 + (it % 16)).toByte() }
        val monitor = ByteArray(35) { it.toByte() }
        return FakeWatch(
            files = mapOf(
                0 to directory(
                    intArrayOf(5, 128, 49, 1), // sleep
                    intArrayOf(6, 128, 32, 2), // monitor
                ),
                5 to sleep,
                6 to monitor,
            ),
        )
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    fun `downloads every wanted file byte-exact across chunks`() = runTest {
        val watch = happyWatch()
        val files = runSync(watch)

        assertEquals(2, files.size)
        assertEquals(GarminFileType.SLEEP, files[0].entry.type)
        assertArrayEquals(watch.files.getValue(5), files[0].bytes)
        assertEquals(GarminFileType.MONITOR, files[1].entry.type)
        assertArrayEquals(watch.files.getValue(6), files[1].bytes)
    }

    @Test
    fun `answers the introduction and the auth challenge`() = runTest {
        val watch = happyWatch()
        runSync(watch)

        val responses = watch.received
            .filter { it.messageType == GarminMessageId.RESPONSE }
            .map { payloadShort(it) }
        assertTrue(GarminMessageId.DEVICE_INFORMATION in responses)
        assertTrue(GarminMessageId.AUTH_NEGOTIATION in responses)
    }

    @Test
    fun `records what the watch said about itself`() = runTest {
        val watch = happyWatch()
        val files = runSync(watch)

        // The transport needs maxPacketSize; the rest is for diagnostics.
        assertEquals(GarminMessageId.RESPONSE, watch.received.first().messageType)
        assertTrue(files.isNotEmpty())
    }

    @Test
    fun `archives each downloaded file so it is not re-offered`() = runTest {
        val watch = happyWatch()
        runSync(watch)

        val archived = watch.received
            .filter { it.messageType == GarminMessageId.SET_FILE_FLAGS }
            .map { payloadShort(it) }
        assertEquals(listOf(5, 6), archived)
        // The directory itself is never archived.
        assertFalse(0 in archived)
    }

    @Test
    fun `brackets the sync with SYNC_READY and SYNC_COMPLETE`() = runTest {
        val watch = happyWatch()
        runSync(watch)

        val events = watch.received
            .filter { it.messageType == GarminMessageId.SYSTEM_EVENT }
            .map { it.payload[0].toInt() }
        assertEquals(GarminSystemEventType.SYNC_READY.ordinal, events.first())
        assertEquals(GarminSystemEventType.SYNC_COMPLETE.ordinal, events.last())
    }

    @Test
    fun `acknowledges every data chunk with the offset reached`() = runTest {
        val watch = happyWatch()
        runSync(watch)

        val acks = responsesAbout(watch, GarminMessageId.FILE_TRANSFER_DATA)
        // 20 bytes at 8/chunk = 3, 35 bytes = 5, directory 32 bytes = 4.
        assertEquals(12, acks.size)
    }

    @Test
    fun `reports progress through every phase`() = runTest {
        val watch = happyWatch()
        val progress = mutableListOf<GarminSyncProgress>()
        runSync(watch, progress = progress)

        val phases = progress.map { it.phase }.toSet()
        assertTrue(GarminSyncPhase.HANDSHAKE in phases)
        assertTrue(GarminSyncPhase.LISTING in phases)
        assertTrue(GarminSyncPhase.DOWNLOADING in phases)
        assertTrue(GarminSyncPhase.COMPLETE in phases)
        assertEquals(2, progress.last().filesDone)
    }

    // ── resilience ───────────────────────────────────────────────────────────

    @Test
    fun `skips a file the watch refuses and still gets the others`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1), intArrayOf(6, 128, 32, 2)),
                5 to b(1, 2, 3),
                6 to b(4, 5, 6),
            ),
            refuseIndexes = setOf(5),
        )

        val files = runSync(watch)

        // One unreadable file must not cost the night's other data.
        assertEquals(1, files.size)
        assertEquals(GarminFileType.MONITOR, files.single().entry.type)
    }

    @Test
    fun `skips a file whose chunk CRC is wrong`() = runTest {
        val watch = CorruptingWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1), intArrayOf(6, 128, 32, 2)),
                5 to b(1, 2, 3, 4, 5),
                6 to b(9, 9, 9),
            ),
            corruptIndex = 5,
        )

        val files = runSync(watch)

        assertEquals(listOf(6), files.map { it.entry.fileIndex })
    }

    @Test
    fun `files with no dedup key are always fetched never skipped`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 0xFFFF)), // sleep, unset number
                5 to b(1, 2, 3),
            ),
        )

        // Even with a key that WOULD match if one existed, an unkeyed file
        // must still be fetched — the alternative is losing every future
        // sleep file.
        val files = runSync(watch, alreadySynced = setOf(key(128, 49, 65535)))

        assertEquals(1, files.size)
    }

    @Test
    fun `a directory with nothing new completes without downloading`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
            ),
        )

        val files = runSync(watch, alreadySynced = setOf(key(128, 49, 1)))

        assertTrue(files.isEmpty())
        // Still a clean, bracketed sync.
        val events = watch.received
            .filter { it.messageType == GarminMessageId.SYSTEM_EVENT }
            .map { it.payload[0].toInt() }
        assertEquals(GarminSystemEventType.SYNC_COMPLETE.ordinal, events.last())
        // And nothing was requested beyond the directory itself.
        val requested = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
            .map { payloadShort(it) }
        assertEquals(listOf(0), requested)
    }

    @Test
    fun `a held file the watch still offers is NOT archived unread`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
            ),
        )

        runSync(watch, alreadySynced = setOf(key(128, 49, 1)))

        // "Held" is a key in a list, not a copy on disk — and a key collision
        // once turned this into telling the watch to drop a day and a half of
        // monitoring nobody had downloaded. The archive flag follows a
        // download in this session or it is not sent.
        val archived = watch.received
            .filter { it.messageType == GarminMessageId.SET_FILE_FLAGS }
            .map { payloadShort(it) }
        assertTrue(archived.isEmpty())
        val requested = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
            .map { payloadShort(it) }
        assertEquals(listOf(0), requested)
    }

    @Test
    fun `the directory listing itself is never archived`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory(intArrayOf(0, 0, 0, 1))))

        runSync(watch, alreadySynced = setOf(key(0, 0, 1)))

        // Archiving the directory would cost the listing every sync depends on.
        val archived = watch.received
            .filter { it.messageType == GarminMessageId.SET_FILE_FLAGS }
            .map { payloadShort(it) }
        assertTrue(archived.isEmpty())
    }

    @Test
    fun `the watch asking for the time gets a time-bearing reply not a bare ack`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = session(this, watch)
        pump(watch, session)

        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.CURRENT_TIME_REQUEST,
                GarminByteWriter().writeInt(0x00b3d43fL).toBytes(),
            ),
        )
        pump(watch, session)

        // Exactly one reply: the response envelope carrying the time. A bare
        // ACK as well would be a second answer to the same ask.
        val replies = watch.received.filter {
            it.messageType == GarminMessageId.RESPONSE &&
                payloadShort(it) == GarminMessageId.CURRENT_TIME_REQUEST
        }
        assertEquals(1, replies.size)
        val reader = GarminByteReader(replies.single().payload)
        reader.readShort() // original message id
        reader.readByte() // status
        assertEquals(0x00b3d43fL, reader.readInt())
        // The time itself: a plausible Garmin timestamp, not zero.
        assertTrue(reader.readInt() > 0L)
    }

    @Test
    fun `a mid-sync file announcement is downloaded like a listed one`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(),
                9 to b(1, 2, 3),
            ),
        )
        val session = session(this, watch, emptyGrace = 10.seconds)
        pump(watch, session)

        // The empty directory leaves the session in its grace wait — exactly
        // where an announcement arrives after a save on the wrist.
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.FILE_AVAILABLE,
                GarminByteWriter()
                    .writeShort(9).writeByte(128).writeByte(32).writeShort(5)
                    .writeByte(0).writeByte(0).writeInt(3L).writeInt(0L)
                    .toBytes(),
            ),
        )
        pump(watch, session)

        val files = session.done.await()
        assertEquals(1, files.size)
        assertEquals(9, files.single().entry.fileIndex)
        // And it was archived like any downloaded file.
        val archived = watch.received
            .filter { it.messageType == GarminMessageId.SET_FILE_FLAGS }
            .map { payloadShort(it) }
        assertEquals(listOf(9), archived)
    }

    @Test
    fun `an announced file already held is not downloaded again`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
                9 to b(4, 5, 6),
            ),
        )
        val session = session(this, watch, alreadySynced = setOf("128/32/7/${GarminTime.GARMIN_EPOCH_SECONDS + 1000}/3"))
        pump(watch, session)
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.FILE_AVAILABLE,
                GarminByteWriter()
                    .writeShort(9).writeByte(128).writeByte(32).writeShort(7)
                    .writeByte(0).writeByte(0).writeInt(3L).writeInt(1000L)
                    .toBytes(),
            ),
        )
        pump(watch, session)

        val files = session.done.await()
        // Only the listed sleep file: the announced monitor was already held.
        assertEquals(listOf(5), files.map { it.entry.fileIndex })
    }

    @Test
    fun `a weather ask is answered with definitions then records`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val weather = tech.mmarca.openvitals.devices.weather.WeatherSnapshot(
            timestamp = 1_786_600_800L,
            location = "Valencia",
            currentTempKelvin = 303,
            todayMinTempKelvin = 295,
            todayMaxTempKelvin = 306,
            currentConditionCode = 800,
            currentHumidity = 45,
            windSpeedKmh = 10.0,
            windDirectionDegrees = 180,
            uvIndex = 7.5,
            precipProbability = 5,
            dewPointKelvin = 289,
            feelsLikeTempKelvin = 305,
            latitude = 39.47,
            longitude = -0.376,
        )
        val session = GarminSession(
            scope = this,
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            weatherProvider = { weather },
            keepAnsweringAfterSync = true,
        ).also { it.start() }
        pump(watch, session)

        // The watch opens its glance: format 0, position, 12 hours, please.
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.WEATHER_REQUEST,
                GarminByteWriter().writeByte(0).writeInt(0L).writeInt(0L).writeByte(12).toBytes(),
            ),
        )
        drain(watch, session)
        // Definitions went out; records wait for the watch to accept them.
        assertEquals(
            1,
            watch.received.count { it.messageType == GarminMessageId.FIT_DEFINITION },
        )
        assertEquals(
            0,
            watch.received.count { it.messageType == GarminMessageId.FIT_DATA },
        )

        // The watch accepts the definitions; the records follow.
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.RESPONSE,
                GarminByteWriter()
                    .writeShort(GarminMessageId.FIT_DEFINITION)
                    .writeByte(GarminStatus.ACK.code)
                    .toBytes(),
            ),
        )
        drain(watch, session)
        val data = watch.received.filter { it.messageType == GarminMessageId.FIT_DATA }
        assertEquals(1, data.size)
        // Slot 0, current report, 30°C — the records really carry the weather.
        assertEquals(0, data.single().payload[0].toInt())
        assertEquals(0, data.single().payload[1].toInt())
        assertEquals(30, data.single().payload[2].toInt())
    }

    @Test
    fun `the capabilities exchange turns the watch's weather feature on`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = session(this, watch, keepAnsweringAfterSync = true)
        pump(watch, session)
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.CONFIGURATION,
                b(8) + ByteArray(8) { 0xFF.toByte() },
            ),
        )
        drain(watch, session)

        val settings = watch.received
            .filter { it.messageType == GarminMessageId.DEVICE_SETTINGS }
        assertEquals(1, settings.size)
        val payload = settings.single().payload
        assertEquals(3, payload[0].toInt()) // three settings
        // [ordinal, len=1, value] triplets: auto-upload on, weather
        // conditions ON (the switch for the whole glance), alerts off.
        assertEquals(listOf(6, 1, 1), payload.slice(1..3).map { it.toInt() })
        assertEquals(listOf(7, 1, 1), payload.slice(4..6).map { it.toInt() })
        assertEquals(listOf(8, 1, 0), payload.slice(7..9).map { it.toInt() })
    }

    @Test
    fun `fresh weather is pushed after the capabilities exchange`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = GarminSession(
            scope = this,
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            weatherProvider = {
                tech.mmarca.openvitals.devices.weather.WeatherSnapshot(
                    timestamp = 1_786_600_800L,
                    location = "Tallinn",
                    currentTempKelvin = 291,
                    todayMinTempKelvin = 286,
                    todayMaxTempKelvin = 293,
                    currentConditionCode = 803,
                    currentHumidity = 70,
                    windSpeedKmh = 20.0,
                    windDirectionDegrees = 250,
                    uvIndex = 3.0,
                    precipProbability = 30,
                    dewPointKelvin = 284,
                    feelsLikeTempKelvin = 289,
                    latitude = 59.437,
                    longitude = 24.7536,
                )
            },
            keepAnsweringAfterSync = true,
        ).also { it.start() }
        pump(watch, session)

        // The watch announces its capabilities — every bit set includes the
        // weather glance — and the push follows with NO 5014 ask: the watch
        // only asks while connected, and this link will be gone in seconds.
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.CONFIGURATION,
                b(8) + ByteArray(8) { 0xFF.toByte() },
            ),
        )
        drain(watch, session)

        assertEquals(
            1,
            watch.received.count { it.messageType == GarminMessageId.FIT_DEFINITION },
        )
    }

    @Test
    fun `no weather push at a watch without the glance`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = GarminSession(
            scope = this,
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            weatherProvider = { error("must not even be consulted") },
            keepAnsweringAfterSync = true,
        ).also { it.start() }
        pump(watch, session)

        // No capability bits at all: pushing would only earn a NAK.
        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.CONFIGURATION,
                b(8) + ByteArray(8),
            ),
        )
        drain(watch, session)

        assertEquals(
            0,
            watch.received.count { it.messageType == GarminMessageId.FIT_DEFINITION },
        )
    }

    @Test
    fun `a weather ask with nothing fresh is left unanswered`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = GarminSession(
            scope = this,
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            weatherProvider = { null },
            keepAnsweringAfterSync = true,
        ).also { it.start() }
        pump(watch, session)

        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.WEATHER_REQUEST,
                GarminByteWriter().writeByte(0).writeInt(0L).writeInt(0L).writeByte(12).toBytes(),
            ),
        )
        pump(watch, session)

        // Acked (the ask is never left to retransmit) but no stale weather sent.
        assertEquals(
            0,
            watch.received.count { it.messageType == GarminMessageId.FIT_DEFINITION },
        )
    }

    @Test
    fun `a held link hands an announced file to its owner instead of downloading`() = runTest {
        val watch = FakeWatch(files = mapOf(9 to b(1, 2, 3)))
        var announced = 0
        val session = GarminSession(
            scope = this,
            send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            // The held notification link: no file syncing of its own.
            syncFiles = false,
            onFileAnnounced = { announced++ },
        ).also { it.start() }
        pump(watch, session)

        watch.outbox.add(
            GarminGfdiFrame.build(
                GarminMessageId.FILE_AVAILABLE,
                GarminByteWriter()
                    .writeShort(9).writeByte(128).writeByte(32).writeShort(5)
                    .writeByte(0).writeByte(0).writeInt(3L).writeInt(0L)
                    .toBytes(),
            ),
        )
        drain(watch, session)

        // The owner was told (it starts a proper background sync); the held
        // link itself downloaded nothing — a transfer dragged behind it would
        // die when the link yields the radio.
        assertEquals(1, announced)
        val requested = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
            .map { payloadShort(it) }
        assertTrue(9 !in requested)
    }

    @Test
    fun `an empty directory completes cleanly`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        assertTrue(runSync(watch).isEmpty())
    }

    @Test
    fun `unmapped file types are never requested`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(
                    intArrayOf(5, 128, 55, 1), // golf scorecard — unmapped
                    intArrayOf(6, 128, 49, 2), // sleep — wanted
                ),
                6 to b(1, 2, 3),
            ),
        )

        val files = runSync(watch)

        assertEquals(listOf(6), files.map { it.entry.fileIndex })
        val requested = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
            .map { payloadShort(it) }
        assertEquals(listOf(0, 6), requested)
    }

    @Test
    fun `the capabilities exchange is answered with our own bitmap`() = runTest {
        val watch = ChattyWatch(files = mapOf(0 to directory()))

        runSync(watch)

        // Not just an ACK: the watch waits for a CONFIGURATION of our own,
        // and without it a real device re-sent its own and listed nothing.
        val config = watch.received
            .filter { it.messageType == GarminMessageId.CONFIGURATION }
        assertEquals(1, config.size)
        // [byte length][bitmap] — 15 bytes, matching what the watch sends.
        assertEquals(15, config.single().payload.first().toInt())
        assertEquals(16, config.single().payload.size)
    }

    @Test
    fun `notification subscription gets a full status not a bare ACK`() = runTest {
        val watch = ChattyWatch(files = mapOf(0 to directory()))

        runSync(watch)

        val replies = responsesAbout(watch, GarminMessageId.NOTIFICATION_SUBSCRIPTION)
        assertEquals(1, replies.size)
        // [short type][status][notificationStatus][enable][unk] — the short
        // form is what made the watch ask again every second.
        assertEquals(6, replies.single().payload.size)
        // DISABLED — we forward none.
        assertEquals(1, replies.single().payload[3].toInt())
    }

    @Test
    fun `every unanswered inbound message gets a generic ACK`() = runTest {
        // The watch retransmits anything it thinks was lost and will not move
        // on, which is exactly how a real vívoactive 5 stalled with an empty
        // directory while re-sending its CONFIGURATION message.
        val watch = ChattyWatch(files = mapOf(0 to directory()))

        runSync(watch)

        val acked = watch.received
            .filter { it.messageType == GarminMessageId.RESPONSE }
            .map { payloadShort(it) }
        assertTrue(GarminMessageId.CONFIGURATION in acked)
        assertTrue(5043 in acked) // PROTOBUF_REQUEST
    }

    @Test
    fun `an ACK is never itself ACKed`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))

        runSync(watch)

        // A RESPONSE naming RESPONSE would bounce between the two forever.
        val acked = watch.received
            .filter { it.messageType == GarminMessageId.RESPONSE }
            .map { payloadShort(it) }
        assertFalse(GarminMessageId.RESPONSE in acked)
    }

    @Test
    fun `messages with their own response are not double-acked`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))

        runSync(watch)

        // Device information and auth each get exactly one reply — the
        // response that carries our details IS the acknowledgement.
        for (type in listOf(
            GarminMessageId.DEVICE_INFORMATION,
            GarminMessageId.AUTH_NEGOTIATION,
        )) {
            assertEquals("type $type", 1, responsesAbout(watch, type).size)
        }
    }

    @Test
    fun `a FILTER is sent before the directory is requested`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))

        runSync(watch)

        val order = watch.received.map { it.messageType }
        val filterAt = order.indexOf(GarminMessageId.FILTER)
        val directoryAt = order.indexOf(GarminMessageId.DOWNLOAD_REQUEST)
        assertTrue("the filter must be sent", filterAt >= 0)
        // The watch processes writes in order, so the filter has to land
        // before the listing is asked for.
        assertTrue(filterAt < directoryAt)
    }

    @Test
    fun `a synchronization announcement re-reads the listing`() = runTest {
        // First listing empty, then the watch announces it holds sleep data.
        val watch = AnnouncingWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
            ),
        )

        val files = runSync(watch)

        // The re-read must find the file the first pass could not.
        assertEquals(listOf(5), files.map { it.entry.fileIndex })
        // Two directory requests: the initial one and the post-announcement one.
        val directoryRequests = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
            .filter { payloadShort(it) == 0 }
        assertEquals(2, directoryRequests.size)
    }

    @Test
    fun `an announcement with nothing we want does not re-read`() = runTest {
        val watch = AnnouncingWatch(
            files = mapOf(0 to directory()),
            // Bit 1 is SETTINGS — not one of the categories worth acting on.
            bitmask = 1L shl 1,
        )

        runSync(watch)

        val directoryRequests = watch.received
            .filter { it.messageType == GarminMessageId.DOWNLOAD_REQUEST }
        assertEquals(1, directoryRequests.size)
    }

    @Test
    fun `a link that dies during the empty grace still settles the sync`() = runTest {
        // The grace window waits out a watch that announces late — and it is
        // exactly when a watch walks out of range. The send inside the timer
        // throws then, and an unhandled failure there would leave `done`
        // pending forever.
        val watch = FakeWatch(files = mapOf(0 to directory()))
        var connected = true
        val session = session(
            this,
            watch,
            emptyGrace = 20.milliseconds,
            send = { frame ->
                if (!connected) throw IllegalStateException("link dropped")
                watch.onFrame(GarminGfdiFrame.parse(frame))
            },
        )

        pump(watch, session)
        // The listing came back empty, so the grace timer is now armed. Take
        // the link away before it fires.
        connected = false

        assertTrue(session.done.await().isEmpty())
    }

    @Test
    fun `a file is kept before it is archived`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
            ),
        )
        val order = mutableListOf<String>()

        val session = session(
            this,
            watch,
            onFileDownloaded = { order.add("kept") },
            send = { frame ->
                val parsed = GarminGfdiFrame.parse(frame)
                if (parsed.messageType == GarminMessageId.SET_FILE_FLAGS) {
                    order.add("archive")
                }
                watch.onFrame(parsed)
            },
        )
        pump(watch, session)
        session.done.await()

        // Archiving is irreversible from our side, so the copy must land first.
        assertEquals(listOf("kept", "archive"), order)
    }

    @Test
    fun `a file that could not be kept is NOT archived`() = runTest {
        val watch = FakeWatch(
            files = mapOf(
                0 to directory(intArrayOf(5, 128, 49, 1)),
                5 to b(1, 2, 3),
            ),
        )

        val session = session(
            this,
            watch,
            onFileDownloaded = { throw IOException("disk") },
        )
        pump(watch, session)
        val files = session.done.await()

        // The file is still returned for import, but the watch keeps offering
        // it — better a redundant download than data we can never fetch again.
        assertEquals(1, files.size)
        assertTrue(
            watch.received.none { it.messageType == GarminMessageId.SET_FILE_FLAGS },
        )
    }

    @Test
    fun `abort keeps what was already downloaded`() = runTest {
        val session = GarminSession(
            scope = this,
            send = { },
            bluetoothName = "Pixel",
            manufacturer = "Google",
            model = "raven",
        ).also { it.start() }

        session.abort("link dropped")

        assertTrue(session.done.await().isEmpty())
    }

    @Test
    fun `ignores frames that arrive after completion`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = session(this, watch)
        pump(watch, session)
        session.done.await()

        // A watch that keeps chattering must not throw or reopen the sync.
        session.handleFrame(GarminGfdiFrame.parse(watch.authNegotiation()))
        assertTrue(session.done.await().isEmpty())
    }

    @Test
    fun `keeps acknowledging after completion when listening`() = runTest {
        val watch = FakeWatch(files = mapOf(0 to directory()))
        val session = session(this, watch, keepAnsweringAfterSync = true)
        pump(watch, session)
        session.done.await()

        val before = watch.received.size
        session.handleFrame(GarminGfdiFrame.parse(watch.authNegotiation()))

        // The point of the diagnostic window: a frame arriving after the sync
        // is still answered. Silence here would make the watch retransmit on
        // a timer and eventually drop the link the pass depends on.
        assertTrue(watch.received.size > before)
        assertTrue(session.done.await().isEmpty())
    }

    // ── notification subscription without a handler (7e ports the rest) ──────

    @Test
    fun `a session with NO handler still replies DISABLED so sync find and settings sessions are unchanged`() =
        runTest {
            val watch = FakeWatch(files = emptyMap())
            val session = GarminSession(
                scope = this,
                send = { frame -> watch.onFrame(GarminGfdiFrame.parse(frame)) },
                bluetoothName = "Pixel 6 Pro",
                manufacturer = "Google",
                model = "raven",
                syncFiles = false,
            ).also { it.start() }

            val subscription = GarminByteWriter()
                .writeByte(1) // enable
                .writeByte(0)
            session.handleFrame(
                GarminGfdiFrame.parse(
                    GarminGfdiFrame.build(
                        GarminMessageId.NOTIFICATION_SUBSCRIPTION,
                        subscription.toBytes(),
                    ),
                ),
            )

            val replies = responsesAbout(watch, GarminMessageId.NOTIFICATION_SUBSCRIPTION)
            assertEquals(1, replies.size)
            assertEquals(6, replies.single().payload.size)
            // The status byte: 0 is ENABLED, 1 is DISABLED.
            assertEquals(1, replies.single().payload[3].toInt())
        }
}

private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }
