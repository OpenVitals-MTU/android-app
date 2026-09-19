package tech.mmarca.openvitals.devices.garmin

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The three music messages, byte for byte, and the session's answers to the watch. */
class GarminMusicMessagesTest {

    /** One 5049 attribute, as the watch reads it. */
    private data class Attribute(val entity: Int, val attribute: Int, val text: String)

    private fun attributes(frame: ByteArray): List<Attribute> {
        val parsed = GarminGfdiFrame.parse(frame)
        assertEquals(GarminMessageId.MUSIC_CONTROL_ENTITY_UPDATE, parsed.messageType)
        val reader = GarminByteReader(parsed.payload)
        val out = mutableListOf<Attribute>()
        var read = 0
        while (read < parsed.payload.size) {
            val length = reader.readByte()
            val entity = reader.readByte()
            val attribute = reader.readByte()
            assertEquals(0, reader.readByte()) // flags
            out += Attribute(entity, attribute, String(reader.readBytes(length - 3), Charsets.UTF_8))
            read += length + 1
        }
        return out
    }

    private val song = GarminMusicState(
        playerName = "Player",
        artist = "Artist",
        album = "Album",
        title = "Title",
        durationSeconds = 215,
        playing = true,
        positionSeconds = 42,
        volume = 0.5f,
    )

    @Test fun `the capabilities answer is an ACK that carries the commands`() {
        val frame = GarminGfdiFrame.parse(buildMusicCapabilitiesResponse(GarminMusicCommand.entries))

        assertEquals(GarminMessageId.RESPONSE, frame.messageType)
        // [u16 5042][ACK][count][ordinals...]
        assertArrayEquals(
            byteArrayOf(0xB2.toByte(), 0x13, 0, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8),
            frame.payload,
        )
    }

    @Test fun `no commands is a count of zero, not a bare ACK`() {
        val frame = GarminGfdiFrame.parse(buildMusicCapabilitiesResponse(emptyList()))

        assertArrayEquals(byteArrayOf(0xB2.toByte(), 0x13, 0, 0), frame.payload)
    }

    @Test fun `an update names the player, its playback, the volume and the track`() {
        assertEquals(
            listOf(
                Attribute(0, 0, "Player"),
                Attribute(0, 1, "1,1.0,42.000"),
                Attribute(0, 2, "0.50"),
                Attribute(2, 0, "Artist"),
                Attribute(2, 1, "Album"),
                Attribute(2, 2, "Title"),
                Attribute(2, 3, "215"),
            ),
            attributes(buildMusicEntityUpdate(song)),
        )
    }

    @Test fun `a paused player has rate zero and an unknown volume is left out`() {
        val update = attributes(buildMusicEntityUpdate(song.copy(playing = false, volume = null)))

        assertEquals("0,0.0,42.000", update.single { it.entity == 0 && it.attribute == 1 }.text)
        assertTrue(update.none { it.entity == 0 && it.attribute == 2 })
    }

    @Test fun `nothing playing clears every field`() {
        val update = attributes(buildMusicEntityUpdate(GarminMusicState()))

        assertEquals("", update.single { it.entity == 2 && it.attribute == 2 }.text)
        assertEquals("0", update.single { it.entity == 2 && it.attribute == 3 }.text)
    }

    @Test fun `a long title is cut to 252 bytes on a character boundary`() {
        val update = attributes(buildMusicEntityUpdate(song.copy(title = "é".repeat(200)))) // 400 bytes

        assertEquals("é".repeat(126), update.single { it.entity == 2 && it.attribute == 2 }.text)
    }

    @Test fun `a button press decodes to its command and an unknown one to null`() {
        fun decode(vararg payload: Byte) =
            decodeGarminMessage(GarminGfdiFrame(GarminMessageId.MUSIC_CONTROL, payload)) as GarminMusicControl

        assertEquals(GarminMusicCommand.SKIP_TO_NEXT_ITEM, decode(1).command)
        assertEquals(GarminMusicCommand.SKIP_BACKWARDS, decode(8).command)
        assertNull(decode(40).command)
        assertNull(decode().command)
    }

    // The session's side.

    private class FakePort(override var enabled: Boolean = true) : GarminMusicPort {
        var playing: GarminMusicState? = GarminMusicState(title = "Title")
        val performed = mutableListOf<GarminMusicCommand>()
        override fun state(): GarminMusicState? = playing
        override fun perform(command: GarminMusicCommand) {
            performed += command
        }
    }

    private fun kotlinx.coroutines.test.TestScope.responders(port: GarminMusicPort?, sent: MutableList<ByteArray>) =
        GarminSessionResponders(
            scope = backgroundScope,
            send = { sent += it },
            protobuf = GarminProtobufTransport(send = {}),
            hooks = GarminSessionHooks(music = port),
        )

    @Test fun `the watch's ask is answered with the commands, then what is playing`() = runTest {
        val sent = mutableListOf<ByteArray>()

        responders(FakePort(), sent).handleMusicCapabilities()

        assertEquals(
            listOf(GarminMessageId.RESPONSE, GarminMessageId.MUSIC_CONTROL_ENTITY_UPDATE),
            sent.map { GarminGfdiFrame.parse(it).messageType },
        )
        assertEquals(9, GarminGfdiFrame.parse(sent[0]).payload[3].toInt())
    }

    @Test fun `with the switch off, or no port, the watch is offered no commands`() = runTest {
        for (port in listOf(FakePort(enabled = false), null)) {
            val sent = mutableListOf<ByteArray>()

            responders(port, sent).handleMusicCapabilities()

            assertEquals(1, sent.size)
            assertEquals(0, GarminGfdiFrame.parse(sent[0]).payload[3].toInt())
        }
    }

    @Test fun `a button press reaches the player only while the switch is on`() = runTest {
        val port = FakePort()
        val responders = responders(port, mutableListOf())

        responders.handleMusicControl(GarminMusicControl(GarminMusicCommand.PAUSE))
        responders.handleMusicControl(GarminMusicControl(null))
        port.enabled = false
        responders.handleMusicControl(GarminMusicControl(GarminMusicCommand.PLAY))

        assertEquals(listOf(GarminMusicCommand.PAUSE), port.performed)
    }

    @Test fun `the capabilities ask is not acked twice`() {
        assertTrue(GarminMessageId.MUSIC_CONTROL_CAPABILITIES in garminSelfAcknowledgedTypes)
        assertTrue(GarminMessageId.MUSIC_CONTROL !in garminSelfAcknowledgedTypes)
    }
}
