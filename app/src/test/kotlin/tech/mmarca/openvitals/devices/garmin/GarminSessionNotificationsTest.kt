package tech.mmarca.openvitals.devices.garmin

import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The notification-conversation suite of the Flutter build's
 * `garmin_session_test.dart`, deferred from 7b because it needs the concrete
 * handler ([GarminGncsHandler]): the subscription handshake with a session
 * that carries one, and the announce → request → chunked answer conversation
 * end to end, all through real wire bytes.
 *
 * `GarminSessionTest` keeps the sync suites and the no-handler subscription
 * behaviour.
 */
class GarminSessionNotificationsTest {

    /**
     * A watch that only cares about notifications: it subscribes, asks for a
     * notification's text, and acknowledges each chunk.
     *
     * Separate from `GarminSessionTest`'s FakeWatch because a notification
     * session runs with `syncFiles = false` and never touches the directory —
     * mixing the two would make both harder to read.
     */
    private class NotifyWatch {
        /** Everything the session put on the wire, decoded. */
        val received = mutableListOf<GarminGfdiFrame>()

        suspend fun send(frame: ByteArray) {
            received.add(GarminGfdiFrame.parse(frame))
        }

        fun ofType(messageType: Int): List<GarminGfdiFrame> =
            received.filter { it.messageType == messageType }

        /** Responses the session sent, keyed by the message they name. */
        fun responsesAbout(messageType: Int): List<GarminGfdiFrame> =
            ofType(GarminMessageId.RESPONSE).filter {
                (it.payload[0].toInt() and 0xFF) or
                    ((it.payload[1].toInt() and 0xFF) shl 8) == messageType
            }

        companion object {
            /** The watch asking whether the phone will forward notifications. */
            fun subscription(enable: Boolean): ByteArray {
                val w = GarminByteWriter()
                    .writeByte(if (enable) 1 else 0)
                    .writeByte(0)
                return GarminGfdiFrame.build(
                    GarminMessageId.NOTIFICATION_SUBSCRIPTION,
                    w.toBytes(),
                )
            }

            /** The watch asking for a notification's text. */
            fun attributeRequest(notificationId: Long): ByteArray {
                val w = GarminByteWriter()
                    .writeByte(0) // GET_NOTIFICATION_ATTRIBUTES
                    .writeInt(notificationId)
                    .writeByte(1) // TITLE
                    .writeShort(0)
                    .writeByte(3) // MESSAGE
                    .writeShort(0)
                return GarminGfdiFrame.build(
                    GarminMessageId.NOTIFICATION_CONTROL,
                    w.toBytes(),
                )
            }

            /** The watch acknowledging a chunk. */
            fun chunkAccepted(): ByteArray {
                val w = GarminByteWriter()
                    .writeShort(GarminMessageId.NOTIFICATION_DATA)
                    .writeByte(GarminStatus.ACK.code)
                    .writeByte(0) // TransferStatus.OK
                return GarminGfdiFrame.build(GarminMessageId.RESPONSE, w.toBytes())
            }
        }
    }

    private fun phoneNotification(body: String = "On my way") = GarminNotification(
        id = 0x11223344,
        packageName = "com.example.chat",
        title = "Ada",
        body = body,
        category = GarminNotificationCategory.SMS,
        postedAt = LocalDateTime.of(2026, 7, 28, 9, 5, 3),
    )

    private class NotificationSession(
        val watch: NotifyWatch,
        val session: GarminSession,
        val handler: GarminGncsHandler?,
    )

    /**
     * Builds a notification session and its watch. [forwarding] false gives a
     * session with no handler — a sync, find or settings session.
     */
    private fun notificationSession(
        scope: CoroutineScope,
        forwarding: Boolean = true,
    ): NotificationSession {
        val watch = NotifyWatch()
        val handler = if (forwarding) GarminGncsHandler(send = watch::send) else null
        val session = GarminSession(
            scope = scope,
            send = watch::send,
            bluetoothName = "Pixel 6 Pro",
            manufacturer = "Google",
            model = "raven",
            syncFiles = false,
            notifications = handler,
        ).also { it.start() }
        return NotificationSession(watch, session, handler)
    }

    private suspend fun GarminSession.handleBytes(frame: ByteArray) {
        handleFrame(GarminGfdiFrame.parse(frame))
    }

    /**
     * The status byte a NOTIFICATION_SUBSCRIPTION response carries: 0 is
     * ENABLED, 1 is DISABLED.
     */
    private fun subscriptionStatus(watch: NotifyWatch): Int =
        watch.responsesAbout(GarminMessageId.NOTIFICATION_SUBSCRIPTION)
            .last()
            .payload[3]
            .toInt()

    private fun blobOf(watch: NotifyWatch): ByteArray =
        watch.ofType(GarminMessageId.NOTIFICATION_DATA)
            .flatMap { it.payload.copyOfRange(6, it.payload.size).toList() }
            .toByteArray()

    // ── notification subscription ───────────────────────────────────────────

    @Test
    fun `a session carrying a notifications handler replies ENABLED`() = runTest {
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = true))

        assertEquals(0, subscriptionStatus(s.watch))
        assertTrue(s.handler!!.enabled)
    }

    @Test
    fun `a session with NO handler still replies DISABLED, so sync find and settings sessions are unchanged`() =
        runTest {
            val s = notificationSession(this, forwarding = false)
            s.session.handleBytes(NotifyWatch.subscription(enable = true))

            assertEquals(1, subscriptionStatus(s.watch))
        }

    @Test
    fun `a watch that is not yet accepting notifications is STILL told the phone is willing`() =
        runTest {
            // The reply is the phone's willingness, not the conjunction of both
            // flags. A watch that has never been told a phone would forward
            // sends enable=false, so answering DISABLED confirms it and the
            // watch never flips — which is exactly what kept a real vívoactive
            // 5 silent.
            val s = notificationSession(this)
            s.session.handleBytes(NotifyWatch.subscription(enable = false))

            assertEquals(0, subscriptionStatus(s.watch))
        }

    @Test
    fun `the watch's own flag drives whether anything is announced`() = runTest {
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = false))
        assertEquals(false, s.handler!!.enabled)
        s.watch.received.clear()

        s.handler.post(phoneNotification())

        assertTrue(
            "a watch not accepting notifications must not be announced to",
            s.watch.ofType(GarminMessageId.NOTIFICATION_UPDATE).isEmpty(),
        )
    }

    @Test
    fun `the subscription gets its purpose-built status and no generic ACK`() = runTest {
        // The watch asks about once a second until the reply is the right
        // shape.
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = true))

        val replies = s.watch.responsesAbout(GarminMessageId.NOTIFICATION_SUBSCRIPTION)
        assertEquals(1, replies.size)
        assertEquals(6, replies.single().payload.size)
    }

    // ── the notification conversation end to end ────────────────────────────

    @Test
    fun `announce, answer the request, and acknowledge the whole blob`() = runTest {
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = true))

        s.handler!!.post(phoneNotification())
        // The announcement carries no text — only the id and the category.
        val announcement = s.watch.ofType(GarminMessageId.NOTIFICATION_UPDATE).single()
        assertEquals(9, announcement.payload.size)

        // The watch asks, then acknowledges chunks until the phone stops
        // sending.
        s.session.handleBytes(NotifyWatch.attributeRequest(0x11223344))
        var guard = 0
        while (s.watch.responsesAbout(GarminMessageId.NOTIFICATION_DATA).isEmpty()) {
            if (guard++ > 100) fail("the transfer never finished")
            s.session.handleBytes(NotifyWatch.chunkAccepted())
        }

        // The blob reassembles into the attributes the watch asked for.
        val blob = String(blobOf(s.watch), Charsets.ISO_8859_1)
        assertTrue(blob.contains("Ada"))
        assertTrue(blob.contains("On my way"))
    }

    @Test
    fun `a control request is answered with a control status BEFORE the first chunk`() = runTest {
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = true))
        s.handler!!.post(phoneNotification())
        s.watch.received.clear()

        s.session.handleBytes(NotifyWatch.attributeRequest(0x11223344))

        assertEquals(
            "the control status must be the first thing back",
            GarminMessageId.RESPONSE,
            s.watch.received.first().messageType,
        )
        assertEquals(
            GarminMessageId.NOTIFICATION_CONTROL,
            (s.watch.received.first().payload[0].toInt() and 0xFF) or
                ((s.watch.received.first().payload[1].toInt() and 0xFF) shl 8),
        )
        assertEquals(
            GarminMessageId.NOTIFICATION_DATA,
            s.watch.received[1].messageType,
        )
    }

    @Test
    fun `a multi-chunk body arrives in order and reassembles exactly`() = runTest {
        val s = notificationSession(this)
        s.session.handleBytes(NotifyWatch.subscription(enable = true))
        val body = buildString {
            repeat(700) { append(('a' + it % 26)) }
        }
        s.handler!!.post(phoneNotification(body = body))

        s.session.handleBytes(NotifyWatch.attributeRequest(0x11223344))
        var guard = 0
        while (s.watch.responsesAbout(GarminMessageId.NOTIFICATION_DATA).isEmpty()) {
            if (guard++ > 100) fail("the transfer never finished")
            s.session.handleBytes(NotifyWatch.chunkAccepted())
        }

        val chunks = s.watch.ofType(GarminMessageId.NOTIFICATION_DATA)
        assertTrue(chunks.size > 1)
        // Offsets are contiguous and start at zero.
        var expected = 0
        for (chunk in chunks) {
            val offset = (chunk.payload[4].toInt() and 0xFF) or
                ((chunk.payload[5].toInt() and 0xFF) shl 8)
            assertEquals(expected, offset)
            expected += chunk.payload.size - 6
        }
        val blob = String(blobOf(s.watch), Charsets.ISO_8859_1)
        assertTrue(blob.contains(body))
    }

    @Test
    fun `a held notification is announced AFTER the subscription status, never before`() =
        runTest {
            // The ordering that decided whether the feature worked at all.
            // Announcing inside the enable handling — before the status went
            // out — put a NOTIFICATION_UPDATE in front of a watch that had
            // asked to subscribe and not yet been told it was accepted. It
            // acknowledged the frame and then never requested the text, which
            // looks identical to a watch that has notifications switched off.
            val s = notificationSession(this)
            s.handler!!.post(phoneNotification()) // held: not yet subscribed
            assertTrue(s.watch.ofType(GarminMessageId.NOTIFICATION_UPDATE).isEmpty())
            s.watch.received.clear()

            s.session.handleBytes(NotifyWatch.subscription(enable = true))

            val statusIndex = s.watch.received.indexOfFirst {
                it.messageType == GarminMessageId.RESPONSE &&
                    (it.payload[0].toInt() and 0xFF) or
                    ((it.payload[1].toInt() and 0xFF) shl 8) ==
                    GarminMessageId.NOTIFICATION_SUBSCRIPTION
            }
            val announcementIndex = s.watch.received.indexOfFirst {
                it.messageType == GarminMessageId.NOTIFICATION_UPDATE
            }
            assertTrue(statusIndex >= 0)
            assertTrue(announcementIndex >= 0)
            assertTrue(statusIndex < announcementIndex)
        }

    @Test
    fun `a control request that arrives before any notification sends only the status`() =
        runTest {
            val s = notificationSession(this)
            s.session.handleBytes(NotifyWatch.subscription(enable = true))
            s.watch.received.clear()

            s.session.handleBytes(NotifyWatch.attributeRequest(1))

            assertTrue(s.watch.ofType(GarminMessageId.NOTIFICATION_DATA).isEmpty())
            assertEquals(
                1,
                s.watch.responsesAbout(GarminMessageId.NOTIFICATION_CONTROL).size,
            )
        }
}
