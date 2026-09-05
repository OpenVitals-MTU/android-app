package tech.mmarca.openvitals.devices.garmin

import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Subscription hold/flush, announcements, the chunked attribute upload, and acting from the wrist. */
class GarminGncsHandlerTest {

    /** What the handler put on the wire, decoded back into frames. */
    private class Wire {
        val frames = mutableListOf<GarminGfdiFrame>()

        suspend fun send(frame: ByteArray) {
            frames.add(GarminGfdiFrame.parse(frame))
        }

        fun ofType(messageType: Int): List<GarminGfdiFrame> =
            frames.filter { it.messageType == messageType }

        val last: GarminGfdiFrame get() = frames.last()

        fun clear() = frames.clear()
    }

    private fun notification(
        id: Long,
        body: String = "hello",
        category: GarminNotificationCategory = GarminNotificationCategory.SOCIAL,
    ) = GarminNotification(
        id = id,
        packageName = "com.example.chat",
        title = "Ada",
        body = body,
        category = category,
        postedAt = LocalDateTime.of(2026, 7, 28, 9, 5, 3),
    )

    /** The watch asking for a notification's text. */
    private fun attributeRequest(
        id: Long,
        attributes: Map<GarminNotificationAttribute, Int> = linkedMapOf(
            GarminNotificationAttribute.TITLE to 0,
            GarminNotificationAttribute.MESSAGE to 0,
        ),
    ) = GarminNotificationControl(
        command = GarminNotificationCommand.GET_NOTIFICATION_ATTRIBUTES,
        notificationId = id,
        attributes = attributes,
    )

    private fun transferStatus(status: GarminNotificationTransferStatus) =
        GarminNotificationDataStatus(
            status = GarminStatus.ACK,
            transferStatus = status,
        )

    private val ok = transferStatus(GarminNotificationTransferStatus.OK)

    private class Chunk(
        val totalSize: Int,
        val crc: Int,
        val offset: Int,
        val chunk: ByteArray,
    )

    /** Reassembles a NOTIFICATION_DATA frame into its parts. */
    private fun chunkOf(frame: GarminGfdiFrame): Chunk {
        val p = frame.payload
        fun u8(at: Int) = p[at].toInt() and 0xFF
        return Chunk(
            totalSize = u8(0) or (u8(1) shl 8),
            crc = u8(2) or (u8(3) shl 8),
            offset = u8(4) or (u8(5) shl 8),
            chunk = p.copyOfRange(6, p.size),
        )
    }

    private fun announcedId(frame: GarminGfdiFrame): Int =
        (frame.payload[4].toInt() and 0xFF) or ((frame.payload[5].toInt() and 0xFF) shl 8)

    /** Builds a handler the watch has already subscribed to. */
    private fun enabledHandler(maxQueued: Int = 10): Pair<GarminGncsHandler, Wire> {
        val wire = Wire()
        val handler = GarminGncsHandler(send = wire::send, maxQueued = maxQueued)
        handler.setEnabled(enabled = true)
        return handler to wire
    }

    // Before the watch has subscribed.

    @Test
    fun `nothing is announced, so a sync session sends no notification traffic`() = runTest {
        val wire = Wire()
        val handler = GarminGncsHandler(send = wire::send)

        handler.post(notification(1))
        handler.remove(1)
        handler.handleControl(attributeRequest(1))

        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `a notification that arrives before the subscription is announced as soon as it lands`() =
        runTest {
            // This app opens the link to say something, so it is ready before the watch subscribes.
            val wire = Wire()
            val handler = GarminGncsHandler(send = wire::send)
            handler.post(notification(7))
            assertTrue(wire.frames.isEmpty())

            handler.setEnabled(enabled = true)
            handler.flushHeld()

            val announcement = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).single()
            assertEquals(
                GarminNotificationUpdateType.ADD.ordinal,
                announcement.payload[0].toInt(),
            )
        }

    @Test
    fun `several held notifications are all announced, oldest first`() = runTest {
        val wire = Wire()
        val handler = GarminGncsHandler(send = wire::send)
        handler.post(notification(1))
        handler.post(notification(2))

        handler.setEnabled(enabled = true)
        handler.flushHeld()

        val ids = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).map { announcedId(it) }
        assertEquals(listOf(1, 2), ids)
    }

    @Test
    fun `one held notification edited twice is announced once`() = runTest {
        val wire = Wire()
        val handler = GarminGncsHandler(send = wire::send)
        handler.post(notification(7, body = "first"))
        handler.post(notification(7, body = "edited"))

        handler.setEnabled(enabled = true)
        handler.flushHeld()

        assertEquals(1, wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).size)
    }

    @Test
    fun `a held notification that aged out of the queue is not announced`() = runTest {
        // Announcing it would invite an attribute request with nothing to answer.
        val wire = Wire()
        val handler = GarminGncsHandler(send = wire::send, maxQueued = 1)
        handler.post(notification(1))
        handler.post(notification(2)) // evicts 1

        handler.setEnabled(enabled = true)
        handler.flushHeld()

        val ids = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).map { announcedId(it) }
        assertEquals(listOf(2), ids)
    }

    // Announcing.

    @Test
    fun `a new notification is announced as ADD`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7))

        val frame = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).single()
        assertEquals(GarminNotificationUpdateType.ADD.ordinal, frame.payload[0].toInt())
    }

    @Test
    fun `a second notification with the same id is announced as MODIFY, so the watch updates instead of buzzing again`() =
        runTest {
            val (handler, wire) = enabledHandler()
            handler.post(notification(7, body = "first"))
            wire.clear()
            handler.post(notification(7, body = "edited"))

            assertEquals(
                GarminNotificationUpdateType.MODIFY.ordinal,
                wire.last.payload[0].toInt(),
            )
            assertEquals(1, handler.queued.size)
        }

    @Test
    fun `the count names how many of that category are outstanding`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(1, category = GarminNotificationCategory.SOCIAL))
        handler.post(notification(2, category = GarminNotificationCategory.SOCIAL))
        handler.post(notification(3, category = GarminNotificationCategory.EMAIL))

        assertEquals(2, wire.frames[1].payload[3].toInt()) // second social
        assertEquals(1, wire.last.payload[3].toInt()) // first email
    }

    @Test
    fun `dismissing a notification announces REMOVE and drops it`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7))
        wire.clear()
        handler.remove(7)

        assertEquals(
            GarminNotificationUpdateType.REMOVE.ordinal,
            wire.last.payload[0].toInt(),
        )
        assertTrue(handler.queued.isEmpty())
    }

    @Test
    fun `dismissing an id the queue no longer holds sends nothing`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.remove(999)
        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `the eleventh notification evicts the oldest`() = runTest {
        val (handler, _) = enabledHandler()
        for (id in 1L..11L) {
            handler.post(notification(id))
        }
        assertEquals(
            listOf(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L),
            handler.queued.map { it.id },
        )
    }

    // Answering an attribute request.

    @Test
    fun `an id that has aged out of the queue sends nothing at all`() = runTest {
        val (handler, wire) = enabledHandler(maxQueued = 1)
        handler.post(notification(1))
        handler.post(notification(2)) // evicts 1
        wire.clear()

        handler.handleControl(attributeRequest(1))

        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `a short body goes out as one chunk`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7, body = "hey"))
        wire.clear()

        handler.handleControl(attributeRequest(7))

        val frames = wire.ofType(GarminMessageId.NOTIFICATION_DATA)
        assertEquals(1, frames.size)
        val chunk = chunkOf(frames.single())
        assertEquals(0, chunk.offset)
        assertEquals(chunk.totalSize, chunk.chunk.size)
    }

    @Test
    fun `a body too long for one chunk is split at 300 bytes with a cumulative CRC`() = runTest {
        val (handler, wire) = enabledHandler()
        val body = "x".repeat(700)
        handler.post(notification(7, body = body))
        wire.clear()

        handler.handleControl(attributeRequest(7))
        // The watch takes each chunk in turn.
        handler.handleDataStatus(ok)
        handler.handleDataStatus(ok)

        val chunks = wire.ofType(GarminMessageId.NOTIFICATION_DATA).map { chunkOf(it) }
        assertEquals(3, chunks.size)
        assertEquals(listOf(300, 300), chunks.take(2).map { it.chunk.size })
        assertTrue(chunks[2].chunk.size < 300)
        assertEquals(listOf(0, 300, 600), chunks.map { it.offset })

        // Every chunk's CRC covers everything sent so far, not just itself.
        var running = 0
        for (chunk in chunks) {
            running = GarminCrc.compute(chunk.chunk, initialCrc = running)
            assertEquals(running, chunk.crc)
        }
        // And the parts reassemble into exactly what was declared.
        val assembled = chunks.flatMap { it.chunk.toList() }
        assertEquals(chunks.first().totalSize, assembled.size)
    }

    @Test
    fun `the final acknowledgement is sent once the blob has drained`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7, body = "hey"))
        wire.clear()

        handler.handleControl(attributeRequest(7))
        handler.handleDataStatus(ok)

        // A RESPONSE naming NOTIFICATION_DATA with ACK/OK.
        val ack = wire.ofType(GarminMessageId.RESPONSE).single()
        assertArrayEquals(
            byteArrayOf(0xAB.toByte(), 0x13, 0x00, 0x00),
            ack.payload,
        )
    }

    @Test
    fun `a second request for the same notification restarts the transfer from offset zero`() =
        runTest {
            // The watch asks again with a larger limit when the wearer scrolls into the body.
            val (handler, wire) = enabledHandler()
            handler.post(notification(7, body = "hey"))
            handler.handleControl(attributeRequest(7))
            wire.clear()

            handler.handleControl(attributeRequest(7))

            assertEquals(
                0,
                chunkOf(wire.ofType(GarminMessageId.NOTIFICATION_DATA).single()).offset,
            )
        }

    @Test
    fun `an action request is not answered, because none were announced`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7))
        wire.clear()

        handler.handleControl(
            GarminNotificationControl(
                command = GarminNotificationCommand.PERFORM_NOTIFICATION_ACTION,
                notificationId = 7,
                actionCode = 98,
            ),
        )

        assertTrue(wire.frames.isEmpty())
    }

    // Transfer flow control.

    private suspend fun midTransfer(): Pair<GarminGncsHandler, Wire> {
        val (handler, wire) = enabledHandler()
        handler.post(notification(7, body = "x".repeat(700)))
        handler.handleControl(attributeRequest(7))
        wire.clear()
        return handler to wire
    }

    @Test
    fun `a chunk answered with RESEND is sent again at the same offset with the same CRC`() =
        runTest {
            val (handler, wire) = midTransfer()

            handler.handleDataStatus(ok) // chunk at offset 300
            val first = chunkOf(wire.last)
            handler.handleDataStatus(transferStatus(GarminNotificationTransferStatus.RESEND))
            val repeat = chunkOf(wire.last)

            assertEquals(first.offset, repeat.offset)
            assertEquals(first.crc, repeat.crc)
            assertArrayEquals(first.chunk, repeat.chunk)
        }

    @Test
    fun `the transfer continues normally after a honoured RESEND`() = runTest {
        val (handler, wire) = midTransfer()

        handler.handleDataStatus(transferStatus(GarminNotificationTransferStatus.RESEND))
        handler.handleDataStatus(ok)

        assertEquals(300, chunkOf(wire.last).offset)
    }

    @Test
    fun `a second RESEND for the same chunk abandons the transfer`() = runTest {
        val (handler, wire) = midTransfer()

        handler.handleDataStatus(transferStatus(GarminNotificationTransferStatus.RESEND))
        wire.clear()
        handler.handleDataStatus(transferStatus(GarminNotificationTransferStatus.RESEND))

        assertTrue(wire.frames.isEmpty())
        // And nothing is left in flight to answer a later status with.
        handler.handleDataStatus(ok)
        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `ABORT stops the transfer without sending anything further`() = runTest {
        val (handler, wire) = midTransfer()
        handler.handleDataStatus(transferStatus(GarminNotificationTransferStatus.ABORT))
        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `a CRC mismatch abandons rather than retrying, because retrying would send the same bytes`() =
        runTest {
            val (handler, wire) = midTransfer()
            handler.handleDataStatus(
                transferStatus(GarminNotificationTransferStatus.CRC_MISMATCH),
            )
            assertTrue(wire.frames.isEmpty())
        }

    @Test
    fun `an OFFSET_MISMATCH abandons, because the status names no offset to recover to`() =
        runTest {
            val (handler, wire) = midTransfer()
            handler.handleDataStatus(
                transferStatus(GarminNotificationTransferStatus.OFFSET_MISMATCH),
            )
            assertTrue(wire.frames.isEmpty())
        }

    @Test
    fun `a transfer status arriving with nothing in flight is ignored`() = runTest {
        val (handler, wire) = enabledHandler()
        handler.handleDataStatus(ok)
        assertTrue(wire.frames.isEmpty())
    }

    @Test
    fun `unsubscribing mid-transfer drops it`() = runTest {
        val (handler, wire) = midTransfer()
        handler.setEnabled(enabled = false)
        handler.setEnabled(enabled = true)

        handler.handleDataStatus(ok)

        assertTrue(wire.frames.isEmpty())
    }

    // Actions.

    /** Actions the handler resolved and handed up. */
    private class ActionSink {
        val requests = mutableListOf<GarminNotificationActionRequest>()

        suspend fun handle(request: GarminNotificationActionRequest) {
            requests.add(request)
        }
    }

    private val dismissAction = GarminNotificationAction(
        kind = GarminNotificationActionKind.DISMISS,
        label = "Dismiss",
        androidIndex = -1,
    )

    private val replyAction = GarminNotificationAction(
        kind = GarminNotificationActionKind.REPLY,
        label = "Reply",
        androidIndex = 0,
        isReply = true,
    )

    private val customAction = GarminNotificationAction(
        kind = GarminNotificationActionKind.CUSTOM_1,
        label = "Mark as read",
        androidIndex = 1,
    )

    private fun actionable(
        id: Long = 7,
        actions: List<GarminNotificationAction> = listOf(
            dismissAction,
            replyAction,
            customAction,
        ),
    ) = GarminNotification(
        id = id,
        packageName = "com.example.chat",
        title = "Ada",
        body = "On my way",
        category = GarminNotificationCategory.SMS,
        postedAt = LocalDateTime.of(2026, 7, 28, 9, 5, 3),
        actions = actions,
    )

    /** A handler the watch has subscribed to, with actions wired up. */
    private fun actionHandler(): Triple<GarminGncsHandler, Wire, ActionSink> {
        val wire = Wire()
        val sink = ActionSink()
        val handler = GarminGncsHandler(send = wire::send, onAction = sink::handle)
        handler.setEnabled(enabled = true)
        return Triple(handler, wire, sink)
    }

    private fun invoke(
        id: Long,
        actionCode: Int,
        text: String? = null,
        command: GarminNotificationCommand =
            GarminNotificationCommand.PERFORM_NOTIFICATION_ACTION,
    ) = GarminNotificationControl(
        command = command,
        notificationId = id,
        actionCode = actionCode,
        actionText = text,
    )

    @Test
    fun `a notification with actions sets the NEW_ACTIONS phone flag`() = runTest {
        // Without it the watch draws no controls; the announcement is where it decides.
        val (handler, wire, _) = actionHandler()
        handler.post(actionable())

        val announcement = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).single()
        assertTrue(
            announcement.payload.last().toInt() and
                GarminNotificationPhoneFlag.NEW_ACTIONS.bit != 0,
        )
    }

    @Test
    fun `a notification with no actions does not claim any`() = runTest {
        val (handler, wire, _) = actionHandler()
        handler.post(actionable(actions = emptyList()))

        val announcement = wire.ofType(GarminMessageId.NOTIFICATION_UPDATE).single()
        assertEquals(0, announcement.payload.last().toInt())
    }

    @Test
    fun `the ACTIONS attribute carries every offered action`() = runTest {
        val (handler, wire, _) = actionHandler()
        handler.post(actionable())
        wire.clear()

        handler.handleControl(
            attributeRequest(7, attributes = linkedMapOf(GarminNotificationAttribute.ACTIONS to 0)),
        )

        val blob = wire.ofType(GarminMessageId.NOTIFICATION_DATA)
            .flatMap { it.payload.copyOfRange(6, it.payload.size).toList() }
        // command + id + {code, u16 length, value}; the value starts at 8.
        assertEquals("three actions offered", 3, blob[8].toInt())
    }

    @Test
    fun `a custom action resolves to the Android index it came from`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.post(actionable())

        handler.handleControl(invoke(7, GarminNotificationActionKind.CUSTOM_1.code))

        assertEquals(1, sink.requests.single().action.androidIndex)
        assertEquals(7L, sink.requests.single().notificationId)
    }

    @Test
    fun `a reply carries the text the wearer dictated`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.post(actionable())

        handler.handleControl(
            invoke(7, GarminNotificationActionKind.REPLY.code, text = "on my way"),
        )

        assertEquals("on my way", sink.requests.single().replyText)
        assertTrue(sink.requests.single().action.isReply)
    }

    @Test
    fun `dismiss resolves to the synthetic action, not one of the app's`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.post(actionable())

        handler.handleControl(invoke(7, GarminNotificationActionKind.DISMISS.code))

        assertTrue(sink.requests.single().action.isSynthetic)
    }

    @Test
    fun `the legacy refuse control maps onto dismiss`() = runTest {
        // The button the watch draws from ACTION_DECLINE, which a wearer presses to dismiss. It was dead.
        val (handler, _, sink) = actionHandler()
        handler.post(actionable())

        handler.handleControl(
            invoke(
                7,
                1, // LegacyNotificationAction.REFUSE
                command = GarminNotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION,
            ),
        )

        assertEquals(
            GarminNotificationActionKind.DISMISS,
            sink.requests.single().action.kind,
        )
    }

    @Test
    fun `the legacy accept control does nothing, because nothing offers it`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.post(actionable())

        handler.handleControl(
            invoke(
                7,
                0, // ACCEPT
                command = GarminNotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION,
            ),
        )

        assertTrue(sink.requests.isEmpty())
    }

    @Test
    fun `an action code that was never offered is ignored`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.post(actionable(actions = listOf(dismissAction)))

        handler.handleControl(invoke(7, GarminNotificationActionKind.CUSTOM_3.code))

        assertTrue(sink.requests.isEmpty())
    }

    @Test
    fun `an action on a notification that has aged out is ignored`() = runTest {
        val (handler, _, sink) = actionHandler()
        handler.handleControl(invoke(999, GarminNotificationActionKind.DISMISS.code))

        assertTrue(sink.requests.isEmpty())
    }

    @Test
    fun `actions are ignored entirely before the watch subscribes`() = runTest {
        val wire = Wire()
        val sink = ActionSink()
        val handler = GarminGncsHandler(send = wire::send, onAction = sink::handle)
        handler.post(actionable())

        handler.handleControl(invoke(7, GarminNotificationActionKind.DISMISS.code))

        assertTrue(sink.requests.isEmpty())
    }
}
