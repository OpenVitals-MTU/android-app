package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.media.PhoneMediaCommand
import tech.mmarca.openvitals.devices.media.PhoneMediaSource
import tech.mmarca.openvitals.devices.media.PhoneMediaState
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/** The player is followed only while a watch wants it, and position ticks stay off the link. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GarminMusicRelayTest {

    private class FakeMedia : PhoneMediaSource {
        var access = true
        var now: PhoneMediaState? = null
        var listener: ((PhoneMediaState?) -> Unit)? = null
        val performed = mutableListOf<PhoneMediaCommand>()

        override fun hasAccess(): Boolean = access
        override fun start(onChanged: (PhoneMediaState?) -> Unit): Boolean {
            if (!access) return false
            listener = onChanged
            return true
        }
        override fun stop() {
            listener = null
        }
        override fun current(): PhoneMediaState? = now
        override fun perform(command: PhoneMediaCommand) {
            performed += command
        }

        fun change(state: PhoneMediaState?) {
            now = state
            listener?.invoke(state)
        }
    }

    private lateinit var repo: BleDeviceRepository
    private lateinit var stateStore: GarminDeviceStateStore
    private lateinit var watch: BleSensorDevice
    private val media = FakeMedia()
    private val relayed = mutableListOf<GarminMusicState>()
    private var nowMillis = 0L

    private val song = PhoneMediaState(title = "Title", artist = "Artist", playing = true, positionSeconds = 10)

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        stateStore = GarminDeviceStateStore(FakeSharedPreferences())
        watch = repo.addDevice(
            displayName = "vívoactive 5",
            address = "E0:48:24:D5:F7:10",
            bluetoothName = "vívoactive 5",
            capabilities = emptySet(),
            kind = BleDeviceKind.WATCH,
            integration = DeviceIntegration.GARMIN,
        )
    }

    private fun TestScope.relay(): GarminMusicRelay =
        GarminMusicRelay(repo, stateStore, media, backgroundScope) { nowMillis }.also {
            it.onState = { state -> relayed += state }
            it.start()
            runCurrent()
        }

    @Test
    fun `off by default - the player is not followed and the watch gets no controls`() = runTest {
        val relay = relay()

        assertFalse(relay.enabled)
        assertNull(relay.state())
        assertNull(media.listener)

        relay.perform(GarminMusicCommand.PLAY)
        assertTrue(media.performed.isEmpty())
    }

    @Test
    fun `switching on follows the player and relays its changes`() = runTest {
        val relay = relay()

        relay.onEnabledChanged(watch.id, true)
        media.change(song)

        assertTrue(relay.enabled)
        assertEquals(listOf("Title"), relayed.map { it.title })
        assertTrue(relayed.single().playing)
    }

    @Test
    fun `a position tick is not news, a seek and a pause are`() = runTest {
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)
        media.change(song)

        // Five seconds on, the player reports where the watch already thinks it is.
        nowMillis = 5_000
        media.change(song.copy(positionSeconds = 15))
        assertEquals(1, relayed.size)

        media.change(song.copy(positionSeconds = 90))
        assertEquals(2, relayed.size)

        media.change(song.copy(positionSeconds = 90, playing = false))
        assertEquals(3, relayed.size)
    }

    @Test
    fun `no player is an empty state, so the wrist clears`() = runTest {
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)
        media.change(song)

        media.change(null)

        assertEquals(GarminMusicState(), relayed.last())
        assertEquals(GarminMusicState(), relay.state())
    }

    @Test
    fun `what the watch was told when it asked is not sent again`() = runTest {
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)
        media.now = song

        assertEquals("Title", relay.state()?.title)
        media.change(song)

        assertTrue(relayed.isEmpty())
    }

    @Test
    fun `the wrist's buttons map onto the player's commands`() = runTest {
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)

        GarminMusicCommand.entries.forEach(relay::perform)

        assertEquals(
            listOf(
                PhoneMediaCommand.TOGGLE_PLAY_PAUSE,
                PhoneMediaCommand.NEXT,
                PhoneMediaCommand.PREVIOUS,
                PhoneMediaCommand.VOLUME_UP,
                PhoneMediaCommand.VOLUME_DOWN,
                PhoneMediaCommand.PLAY,
                PhoneMediaCommand.PAUSE,
                PhoneMediaCommand.FAST_FORWARD,
                PhoneMediaCommand.REWIND,
            ),
            media.performed,
        )
    }

    @Test
    fun `switching off, or forgetting the watch, stops following the player`() = runTest {
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)
        assertTrue(media.listener != null)

        relay.onEnabledChanged(watch.id, false)
        assertNull(media.listener)

        relay.onEnabledChanged(watch.id, true)
        repo.removeDevice(watch.id)
        runCurrent()
        assertNull(media.listener)
        assertFalse(relay.enabled)
    }

    @Test
    fun `access granted later is picked up when asked to look again`() = runTest {
        media.access = false
        val relay = relay()
        relay.onEnabledChanged(watch.id, true)
        assertNull(media.listener)

        media.access = true
        relay.refreshAccess()

        assertTrue(media.listener != null)
    }
}
