package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** The GNCS vocabulary, the attribute-blob encoder, the builders and the decoders. */
class GarminNotificationMessagesTest {

    private fun b(vararg xs: Int) = ByteArray(xs.size) { xs[it].toByte() }

    /** Round-trips an outgoing message through the frame layer: build, parse, decode. */
    private fun roundTrip(wire: ByteArray): GarminInboundMessage =
        decodeGarminMessage(GarminGfdiFrame.parse(wire))

    /** The payload of a built frame, without the length, type and CRC around it. */
    private fun payloadOf(wire: ByteArray): ByteArray =
        GarminGfdiFrame.parse(wire).payload

    /** Builds an inbound NOTIFICATION_CONTROL frame the way a watch would. */
    private fun controlFrame(vararg payload: Int): ByteArray =
        GarminGfdiFrame.build(GarminMessageId.NOTIFICATION_CONTROL, b(*payload))

    /** Builds the RESPONSE envelope a watch sends about a NOTIFICATION_DATA chunk. */
    private fun dataStatusFrame(status: Int = 0, transferStatus: Int = 0): ByteArray {
        val writer = GarminByteWriter()
            .writeShort(GarminMessageId.NOTIFICATION_DATA)
            .writeByte(status)
            .writeByte(transferStatus)
        return GarminGfdiFrame.build(GarminMessageId.RESPONSE, writer.toBytes())
    }

    private fun notification(
        id: Long = 0x11223344L,
        packageName: String = "com.example.chat",
        title: String = "Ada",
        subtitle: String = "",
        body: String = "On my way",
        category: GarminNotificationCategory = GarminNotificationCategory.SMS,
    ) = GarminNotification(
        id = id,
        packageName = packageName,
        title = title,
        subtitle = subtitle,
        body = body,
        category = category,
        postedAt = LocalDateTime.of(2026, 7, 28, 9, 5, 3),
    )

    // garminNotificationDate.

    @Test
    fun `formats as yyyyMMddTHHmmss with every field zero-padded`() {
        assertEquals(
            "20260102T030405",
            garminNotificationDate(LocalDateTime.of(2026, 1, 2, 3, 4, 5)),
        )
    }

    // garminNotificationAttributeBytes.

    @Test
    fun `MESSAGE_SIZE counts the body characters, not its bytes`() {
        // Three characters, but five bytes once the accents are UTF-8 encoded.
        val value = garminNotificationAttributeBytes(
            notification(body = "áéí"),
            GarminNotificationAttribute.MESSAGE_SIZE,
        )
        assertEquals("3", value.toString(Charsets.UTF_8))
    }

    @Test
    fun `a maxLength of zero means no limit`() {
        val value = garminNotificationAttributeBytes(
            notification(body = "a long enough body"),
            GarminNotificationAttribute.MESSAGE,
        )
        assertEquals("a long enough body", value.toString(Charsets.UTF_8))
    }

    @Test
    fun `a body longer than maxLength is cut to that many characters`() {
        val value = garminNotificationAttributeBytes(
            notification(body = "abcdefghij"),
            GarminNotificationAttribute.MESSAGE,
            maxLength = 4,
        )
        assertEquals("abcd", value.toString(Charsets.UTF_8))
    }

    @Test
    fun `a cut that would split an emoji drops it rather than half of it`() {
        // '👍' is two UTF-16 code units, so a cut at 3 leaves a lone surrogate.
        val value = garminNotificationAttributeBytes(
            notification(body = "ok👍!"),
            GarminNotificationAttribute.MESSAGE,
            maxLength = 3,
        )
        assertEquals("ok", value.toString(Charsets.UTF_8))
    }

    @Test
    fun `ACTIONS is the four-zero-byte none sentinel, not an empty value`() {
        val value = garminNotificationAttributeBytes(
            notification(),
            GarminNotificationAttribute.ACTIONS,
        )
        assertArrayEquals(b(0, 0, 0, 0), value)
    }

    // encodeGarminNotificationAttributes.

    @Test
    fun `writes the command byte and the notification id first`() {
        val blob = encodeGarminNotificationAttributes(
            notification = notification(id = 0x11223344L),
            requested = linkedMapOf(GarminNotificationAttribute.TITLE to 0),
        )
        // GET_NOTIFICATION_ATTRIBUTES, then the id little-endian.
        assertArrayEquals(b(0x00, 0x44, 0x33, 0x22, 0x11), blob.copyOfRange(0, 5))
    }

    @Test
    fun `each attribute is a code, a 16-bit byte length, then the value`() {
        val blob = encodeGarminNotificationAttributes(
            notification = notification(title = "Ada"),
            requested = linkedMapOf(GarminNotificationAttribute.TITLE to 0),
        )
        assertArrayEquals(
            b(0x01, 0x03, 0x00, 0x41, 0x64, 0x61),
            blob.copyOfRange(5, blob.size),
        )
    }

    @Test
    fun `MESSAGE_SIZE is encoded last even when the watch asked for it first`() {
        val blob = encodeGarminNotificationAttributes(
            notification = notification(title = "Ada", body = "hey"),
            requested = linkedMapOf(
                GarminNotificationAttribute.MESSAGE_SIZE to 0,
                GarminNotificationAttribute.TITLE to 0,
                GarminNotificationAttribute.MESSAGE to 0,
            ),
        )
        // Attribute codes in the order they landed on the wire.
        val codes = mutableListOf<Int>()
        var i = 5
        while (i < blob.size) {
            codes.add(blob[i].toInt() and 0xFF)
            val length = (blob[i + 1].toInt() and 0xFF) or
                ((blob[i + 2].toInt() and 0xFF) shl 8)
            i += 3 + length
        }
        assertEquals(
            listOf(
                GarminNotificationAttribute.TITLE.code,
                GarminNotificationAttribute.MESSAGE.code,
                GarminNotificationAttribute.MESSAGE_SIZE.code,
            ),
            codes,
        )
    }

    @Test
    fun `a value length is the BYTE count, not the character count`() {
        val blob = encodeGarminNotificationAttributes(
            notification = notification(title = "áé"),
            requested = linkedMapOf(GarminNotificationAttribute.TITLE to 0),
        )
        // Two characters, four bytes.
        assertEquals(
            4,
            (blob[6].toInt() and 0xFF) or ((blob[7].toInt() and 0xFF) shl 8),
        )
    }

    // buildNotificationUpdate.

    @Test
    fun `carries the update type, category and id with no text at all`() {
        val wire = buildNotificationUpdate(
            updateType = GarminNotificationUpdateType.ADD,
            category = GarminNotificationCategory.SMS,
            count = 2,
            notificationId = 0x11223344L,
        )
        val frame = GarminGfdiFrame.parse(wire)
        assertEquals(GarminMessageId.NOTIFICATION_UPDATE, frame.messageType)
        assertArrayEquals(
            b(
                0x00, // ADD
                0x12, // FOREGROUND | ACTION_DECLINE
                0x0C, // SMS
                0x02, // count
                0x44, 0x33, 0x22, 0x11, // id, little-endian
                0x00, // no actions, no attachments
            ),
            frame.payload,
        )
    }

    @Test
    fun `MODIFY and REMOVE use ordinals 1 and 2`() {
        fun updateTypeByte(type: GarminNotificationUpdateType): Int =
            payloadOf(
                buildNotificationUpdate(
                    updateType = type,
                    category = GarminNotificationCategory.OTHER,
                    count = 1,
                    notificationId = 1,
                ),
            )[0].toInt()
        assertEquals(1, updateTypeByte(GarminNotificationUpdateType.MODIFY))
        assertEquals(2, updateTypeByte(GarminNotificationUpdateType.REMOVE))
    }

    @Test
    fun `the phone flags byte announces actions and attachments separately`() {
        fun payloadFor(actions: Boolean = false, attachments: Boolean = false): ByteArray =
            payloadOf(
                buildNotificationUpdate(
                    updateType = GarminNotificationUpdateType.ADD,
                    category = GarminNotificationCategory.OTHER,
                    count = 1,
                    notificationId = 1,
                    hasActions = actions,
                    hasAttachments = attachments,
                ),
            )
        assertEquals(0x02, payloadFor(actions = true).last().toInt()) // NEW_ACTIONS
        assertEquals(0x04, payloadFor(attachments = true).last().toInt()) // HAS_ATTACHMENTS
    }

    // buildNotificationData.

    @Test
    fun `declares the total size, the running CRC and the offset`() {
        val wire = buildNotificationData(
            chunk = b(0xAA, 0xBB),
            totalSize = 0x0102,
            dataOffset = 0x0304,
            runningCrc = 0x0506,
        )
        val frame = GarminGfdiFrame.parse(wire)
        assertEquals(GarminMessageId.NOTIFICATION_DATA, frame.messageType)
        assertArrayEquals(
            b(0x02, 0x01, 0x06, 0x05, 0x04, 0x03, 0xAA, 0xBB),
            frame.payload,
        )
    }

    // buildNotificationSubscriptionStatus.

    private val incomingSubscription =
        GarminNotificationSubscription(enable = true, unknown = 7)

    @Test
    fun `reports ENABLED as 0 and echoes the watch back`() {
        assertArrayEquals(
            // 5036 little-endian, ACK, ENABLED, then the watch's own two bytes.
            b(0xAC, 0x13, 0x00, 0x00, 0x01, 0x07),
            payloadOf(
                buildNotificationSubscriptionStatus(incomingSubscription, enabled = true),
            ),
        )
    }

    @Test
    fun `reports DISABLED as 1`() {
        assertEquals(
            1,
            payloadOf(
                buildNotificationSubscriptionStatus(incomingSubscription, enabled = false),
            )[3].toInt(),
        )
    }

    // buildNotificationControlStatus.

    @Test
    fun `names NOTIFICATION_CONTROL with ACK, chunk OK and no error`() {
        assertArrayEquals(
            // 5034 little-endian, ACK, chunk OK, no error.
            b(0xAA, 0x13, 0x00, 0x00, 0x00),
            payloadOf(buildNotificationControlStatus()),
        )
    }

    // Decoding NOTIFICATION_CONTROL.

    @Test
    fun `an attribute request reads the id and every requested field`() {
        val message = roundTrip(
            controlFrame(
                0x00, // GET_NOTIFICATION_ATTRIBUTES
                0x44, 0x33, 0x22, 0x11, // id
                0x00, // APP_IDENTIFIER, no length param
                0x01, 0x20, 0x00, // TITLE, max 32
                0x03, 0x00, 0x01, // MESSAGE, max 256
                0x04, // MESSAGE_SIZE
            ),
        ) as GarminNotificationControl

        assertEquals(
            GarminNotificationCommand.GET_NOTIFICATION_ATTRIBUTES,
            message.command,
        )
        assertEquals(0x11223344L, message.notificationId)
        assertEquals(
            mapOf(
                GarminNotificationAttribute.APP_IDENTIFIER to 0,
                GarminNotificationAttribute.TITLE to 32,
                GarminNotificationAttribute.MESSAGE to 256,
                GarminNotificationAttribute.MESSAGE_SIZE to 0,
            ),
            message.attributes,
        )
    }

    @Test
    fun `the requested order is preserved, because the answer reproduces it`() {
        val message = roundTrip(
            controlFrame(
                0x00,
                0x01, 0x00, 0x00, 0x00,
                0x04, // MESSAGE_SIZE first
                0x01, 0x10, 0x00, // then TITLE
            ),
        ) as GarminNotificationControl
        assertEquals(
            listOf(
                GarminNotificationAttribute.MESSAGE_SIZE,
                GarminNotificationAttribute.TITLE,
            ),
            message.attributes.keys.toList(),
        )
    }

    @Test
    fun `ACTIONS consumes its length AND its extra byte, so the next attribute still parses`() {
        // Attribute 127 is followed by a u16 and one unidentified byte.
        val message = roundTrip(
            controlFrame(
                0x00,
                0x01, 0x00, 0x00, 0x00,
                0x7F, 0x40, 0x00, 0x02, // ACTIONS, max 64, extra byte
                0x01, 0x10, 0x00, // TITLE, max 16
            ),
        ) as GarminNotificationControl

        assertEquals(
            mapOf(
                GarminNotificationAttribute.ACTIONS to 64,
                GarminNotificationAttribute.TITLE to 16,
            ),
            message.attributes,
        )
    }

    @Test
    fun `an unknown attribute stops the walk instead of mis-parsing the rest`() {
        // 0x63 is unknown and nothing says how many bytes follow it, so decoding stops.
        val message = roundTrip(
            controlFrame(
                0x00,
                0x01, 0x00, 0x00, 0x00,
                0x01, 0x10, 0x00, // TITLE
                0x63, 0xFF, 0xFF, // unknown
                0x03, 0x10, 0x00, // MESSAGE — deliberately not reached
            ),
        ) as GarminNotificationControl

        assertEquals(
            mapOf(GarminNotificationAttribute.TITLE to 16),
            message.attributes,
        )
    }

    @Test
    fun `an app-attributes request reads the NUL-terminated package name`() {
        val message = roundTrip(
            controlFrame(
                0x01, // GET_APP_ATTRIBUTES
                0x61, 0x2E, 0x62, 0x00, // "a.b\0"
                0x00, // APP_NAME
            ),
        ) as GarminNotificationControl

        assertEquals(GarminNotificationCommand.GET_APP_ATTRIBUTES, message.command)
        assertEquals("a.b", message.appIdentifier)
        assertEquals(listOf(0), message.appAttributes)
    }

    @Test
    fun `an action with no text is decoded, not treated as a short frame`() {
        // Recent firmware omits the string entirely for non-reply actions.
        val message = roundTrip(
            controlFrame(
                0x80, // PERFORM_NOTIFICATION_ACTION
                0x44, 0x33, 0x22, 0x11,
                0x62, // DISMISS_NOTIFICATION
            ),
        ) as GarminNotificationControl

        assertEquals(
            GarminNotificationCommand.PERFORM_NOTIFICATION_ACTION,
            message.command,
        )
        assertEquals(0x11223344L, message.notificationId)
        assertEquals(0x62, message.actionCode)
        assertNull(message.actionText)
    }

    @Test
    fun `an unknown command decodes to unhandled, not an error`() {
        val message = roundTrip(controlFrame(0x7A, 0x00))
        assertTrue(message is GarminUnhandledMessage)
    }

    // Decoding a NOTIFICATION_DATA transfer status.

    @Test
    fun `OK can proceed`() {
        val message = roundTrip(dataStatusFrame()) as GarminNotificationDataStatus
        assertEquals(GarminNotificationTransferStatus.OK, message.transferStatus)
        assertTrue(message.canProceed)
    }

    @Test
    fun `each non-OK transfer status is named, so the upload can tell them apart`() {
        fun statusFor(code: Int): GarminNotificationTransferStatus =
            (roundTrip(dataStatusFrame(transferStatus = code))
                as GarminNotificationDataStatus)
                .transferStatus
        assertEquals(GarminNotificationTransferStatus.RESEND, statusFor(1))
        assertEquals(GarminNotificationTransferStatus.ABORT, statusFor(2))
        assertEquals(GarminNotificationTransferStatus.CRC_MISMATCH, statusFor(3))
        assertEquals(GarminNotificationTransferStatus.OFFSET_MISMATCH, statusFor(4))
    }

    @Test
    fun `a NAK cannot proceed even when the transfer status says OK`() {
        val message = roundTrip(dataStatusFrame(status = 1))
            as GarminNotificationDataStatus
        assertFalse(message.canProceed)
    }

    // Acknowledgement policy.

    @Test
    fun `NOTIFICATION_CONTROL is self-acknowledged, so no generic ACK is sent`() {
        // It gets a three-byte control status; a generic ACK too would be a second reply.
        assertTrue(
            garminSelfAcknowledgedTypes.contains(GarminMessageId.NOTIFICATION_CONTROL),
        )
    }
}
