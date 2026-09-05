package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.notifications.NotificationActionMsg
import tech.mmarca.openvitals.devices.notifications.NotificationMsg

/** Mapping an Android notification's actions onto GNCS's fixed slots, and encoding the ACTIONS attribute. */
class GarminNotificationActionsTest {

    private fun message(
        actions: List<NotificationActionMsg> = emptyList(),
        dismissable: Boolean = true,
    ) = NotificationMsg(
        id = 7,
        packageName = "com.example.chat",
        appLabel = "Chat",
        title = "Ada",
        body = "On my way",
        whenEpochMillis = 0,
        categoryOrdinal = GarminNotificationCategory.SMS.ordinal,
        removed = false,
        actions = actions,
        dismissable = dismissable,
    )

    private fun action(
        index: Int,
        title: String,
        reply: Boolean = false,
        fireable: Boolean = true,
    ) = NotificationActionMsg(
        index = index,
        title = title,
        isReply = reply,
        fireableFromBackground = fireable,
    )

    // Mapping Android actions onto the watch.

    @Test
    fun `every dismissable notification gets a dismiss the app did not provide`() {
        // Android has no "clear this" action, so it is synthesised and marked so the performer cancels.
        val actions = garminActionsFor(message())

        assertEquals(GarminNotificationActionKind.DISMISS, actions.single().kind)
        assertTrue(actions.single().isSynthetic)
        assertEquals(-1, actions.single().androidIndex)
    }

    @Test
    fun `an ongoing notification gets no dismiss, because clearing it would fail`() {
        assertTrue(garminActionsFor(message(dismissable = false)).isEmpty())
    }

    @Test
    fun `the app's own buttons land in the numbered custom slots, in order`() {
        val actions = garminActionsFor(
            message(
                actions = listOf(
                    action(0, "Mark as read"),
                    action(1, "Snooze"),
                ),
            ),
        )

        assertEquals(
            listOf(
                GarminNotificationActionKind.DISMISS,
                GarminNotificationActionKind.CUSTOM_1,
                GarminNotificationActionKind.CUSTOM_2,
            ),
            actions.map { it.kind },
        )
        assertEquals("Mark as read", actions[1].label)
        assertEquals(0, actions[1].androidIndex)
    }

    @Test
    fun `a reply action takes the reply slot, not a custom one`() {
        val actions = garminActionsFor(
            message(
                actions = listOf(
                    action(0, "Reply", reply = true),
                    action(1, "Mark as read"),
                ),
            ),
        )

        val reply = actions.first { it.kind == GarminNotificationActionKind.REPLY }
        assertTrue(reply.isReply)
        assertEquals(0, reply.androidIndex)
        assertTrue(actions.any { it.kind == GarminNotificationActionKind.CUSTOM_1 })
    }

    @Test
    fun `a second reply becomes a plain button rather than overwriting the first`() {
        // The watch has one reply control, so a second would replace it.
        val actions = garminActionsFor(
            message(
                actions = listOf(
                    action(0, "Reply", reply = true),
                    action(1, "Reply privately", reply = true),
                ),
            ),
        )

        assertEquals(1, actions.count { it.kind == GarminNotificationActionKind.REPLY })
        val second = actions.first { it.androidIndex == 1 }
        assertEquals(GarminNotificationActionKind.CUSTOM_1, second.kind)
        assertFalse(second.isReply)
    }

    @Test
    fun `more buttons than there are slots are dropped, not crammed in`() {
        // Reusing a slot would make two controls invoke the same thing.
        val actions = garminActionsFor(
            message(actions = (0 until 8).map { action(it, "Action $it") }),
        )

        val customs = actions.filter { it.kind in GarminNotificationActionKind.customSlots }
        assertEquals(MAX_GARMIN_CUSTOM_ACTIONS, customs.size)
        assertEquals(listOf(0, 1, 2, 3, 4), customs.map { it.androidIndex })
    }

    @Test
    fun `an action that only opens the app is not offered at all`() {
        // A stock SMS "Reply" prefills a compose screen, which Android blocks in the background. Offering it puts back a dead button.
        val actions = garminActionsFor(
            message(
                actions = listOf(
                    action(0, "Reply", reply = true, fireable = false),
                    action(1, "Mark as read"),
                ),
            ),
        )

        assertFalse(actions.any { it.kind == GarminNotificationActionKind.REPLY })
        assertTrue(actions.map { it.label }.contains("Mark as read"))
    }

    @Test
    fun `a blocked reply does not consume the reply slot, so a later usable one still gets it`() {
        val actions = garminActionsFor(
            message(
                actions = listOf(
                    action(0, "Reply", reply = true, fireable = false),
                    action(1, "Quick reply", reply = true),
                ),
            ),
        )

        val reply = actions.first { it.kind == GarminNotificationActionKind.REPLY }
        assertEquals(1, reply.androidIndex)
    }

    @Test
    fun `an index survives the round trip, so the phone never re-derives which button was meant`() {
        val actions = garminActionsFor(message(actions = listOf(action(3, "Archive"))))
        assertEquals(3, actions.last().androidIndex)
    }

    // Encoding the ACTIONS attribute.

    @Test
    fun `no actions encodes as the four-zero-byte sentinel`() {
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 0),
            encodeGarminNotificationActions(emptyList()),
        )
    }

    @Test
    fun `each action is a code, an icon position, a length and a label`() {
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.CUSTOM_1,
                    label = "Ok",
                    androidIndex = 0,
                ),
            ),
        )

        assertArrayEquals(
            byteArrayOf(
                1, // one action
                1, // CUSTOM_ACTION_1
                0, // no fixed icon position
                2, // label length
                0x4F, 0x6B, // "Ok"
            ),
            bytes,
        )
    }

    @Test
    fun `dismiss carries the LEFT icon position, which is where the watch draws it`() {
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.DISMISS,
                    label = "X",
                    androidIndex = -1,
                ),
            ),
        )
        assertEquals(98, bytes[1].toInt() and 0xFF) // DISMISS_NOTIFICATION
        assertEquals(GarminActionIconPosition.LEFT.bit, bytes[2].toInt())
    }

    @Test
    fun `reply carries the BOTTOM icon position`() {
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.REPLY,
                    label = "R",
                    androidIndex = 0,
                    isReply = true,
                ),
            ),
        )
        assertEquals(95, bytes[1].toInt() and 0xFF) // REPLY_MESSAGES
        assertEquals(GarminActionIconPosition.BOTTOM.bit, bytes[2].toInt())
    }

    @Test
    fun `the label length is BYTES, so a non-ASCII label still parses`() {
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.CUSTOM_1,
                    label = "áé",
                    androidIndex = 0,
                ),
            ),
        )
        assertEquals("two characters, four UTF-8 bytes", 4, bytes[3].toInt())
    }

    @Test
    fun `an absurdly long label is trimmed rather than wrapping the length byte`() {
        // A length byte cannot carry more than 255; wrapping would read the next code as label text.
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.CUSTOM_1,
                    label = "x".repeat(400),
                    androidIndex = 0,
                ),
            ),
        )
        assertEquals(255, bytes[3].toInt() and 0xFF)
        assertEquals(4 + 255, bytes.size)
    }

    @Test
    fun `several actions pack one after another`() {
        val bytes = encodeGarminNotificationActions(
            listOf(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.DISMISS,
                    label = "a",
                    androidIndex = -1,
                ),
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.CUSTOM_1,
                    label = "bb",
                    androidIndex = 0,
                ),
            ),
        )
        assertEquals(2, bytes[0].toInt())
        // count + (3 + 1) + (3 + 2)
        assertEquals(1 + 4 + 5, bytes.size)
    }
}
