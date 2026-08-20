package tech.mmarca.openvitals.devices.garmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval

class GarminDeviceStateStoreTest {

    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: GarminDeviceStateStore

    private val deviceId = "ble-watch-1"

    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = GarminDeviceStateStore(prefs)
    }

    @Test
    fun `synced file keys start empty and round-trip through storage`() {
        assertTrue(store.syncedFileKeys(deviceId).isEmpty())

        store.recordSyncedFileKeys(deviceId, listOf("128/49/1", "128/32/2"))

        // A second store over the same prefs is the real round-trip — this is
        // what proves the exact key/format survives a restart.
        assertEquals(
            setOf("128/49/1", "128/32/2"),
            GarminDeviceStateStore(prefs).syncedFileKeys(deviceId),
        )
    }

    @Test
    fun `synced file keys merge without duplicating across runs`() {
        store.recordSyncedFileKeys(deviceId, listOf("128/49/1"))
        store.recordSyncedFileKeys(deviceId, listOf("128/49/1", "128/32/2"))

        assertEquals(setOf("128/49/1", "128/32/2"), store.syncedFileKeys(deviceId))
    }

    @Test
    fun `synced file keys are scoped per device`() {
        store.recordSyncedFileKeys(deviceId, listOf("128/49/1"))

        assertTrue(store.syncedFileKeys("ble-watch-2").isEmpty())
    }

    @Test
    fun `an empty synced-keys write is a no-op`() {
        store.recordSyncedFileKeys(deviceId, emptyList())
        assertTrue(store.syncedFileKeys(deviceId).isEmpty())
    }

    @Test
    fun `the synced-keys set is capped, dropping the oldest keys first`() {
        // Push past the 4000 cap in two batches so ordering is observable.
        store.recordSyncedFileKeys(deviceId, (0 until 3999).map { "old/$it" })
        store.recordSyncedFileKeys(deviceId, listOf("new/a", "new/b"))

        val keys = store.syncedFileKeys(deviceId)
        assertEquals(4000, keys.size)
        // Newest survive; the very oldest was dropped.
        assertTrue("new/a" in keys)
        assertTrue("new/b" in keys)
        assertFalse("old/0" in keys)
    }

    @Test
    fun `capabilities round-trip through storage by wire name`() {
        assertTrue(store.capabilities(deviceId).isEmpty())

        store.recordCapabilities(
            deviceId,
            setOf(GarminCapability.SYNC, GarminCapability.FIND_MY_WATCH),
        )

        // Second store over the same prefs — proves the wireName format
        // persists.
        assertEquals(
            setOf(GarminCapability.SYNC, GarminCapability.FIND_MY_WATCH),
            GarminDeviceStateStore(prefs).capabilities(deviceId),
        )
    }

    @Test
    fun `an empty capabilities write is a no-op`() {
        store.recordCapabilities(deviceId, emptySet())
        assertTrue(store.capabilities(deviceId).isEmpty())
    }

    @Test
    fun `clear drops both capabilities and synced-file history`() {
        // What forgetting a watch must do: a re-pairing starts clean,
        // re-learning capabilities from a fresh handshake and re-fetching
        // files rather than trusting a record of a device that is no longer
        // here.
        store.recordSyncedFileKeys(deviceId, listOf("128/49/1"))
        store.recordCapabilities(deviceId, setOf(GarminCapability.SYNC))

        store.clear(deviceId)

        assertTrue(store.syncedFileKeys(deviceId).isEmpty())
        assertTrue(store.capabilities(deviceId).isEmpty())
        // And it survives a reload — the keys are gone from storage, not just
        // the in-memory view.
        val reloaded = GarminDeviceStateStore(prefs)
        assertTrue(reloaded.syncedFileKeys(deviceId).isEmpty())
        assertTrue(reloaded.capabilities(deviceId).isEmpty())
    }

    @Test
    fun `automatic sync is off until it is chosen, and survives a restart`() {
        assertEquals(AutoSyncInterval.OFF, store.autoSyncInterval(deviceId))

        store.setAutoSyncInterval(deviceId, AutoSyncInterval.HOURLY)

        assertEquals(
            AutoSyncInterval.HOURLY,
            GarminDeviceStateStore(prefs).autoSyncInterval(deviceId),
        )
    }

    @Test
    fun `automatic sync is scoped per device`() {
        store.setAutoSyncInterval(deviceId, AutoSyncInterval.EVERY_30_MINUTES)

        assertEquals(AutoSyncInterval.OFF, store.autoSyncInterval("ble-watch-2"))
    }

    @Test
    fun `automatic sync is stored as minutes, not as an ordinal`() {
        // The stored value has to mean the same thing to a build that adds or
        // drops an interval, which an enum position would not.
        store.setAutoSyncInterval(deviceId, AutoSyncInterval.EVERY_2_HOURS)

        assertEquals(120, prefs.getInt("garmin_auto_sync_minutes_$deviceId", 0))
    }

    @Test
    fun `stay connected is on until the wearer says otherwise`() {
        assertTrue(store.stayConnected(deviceId))

        store.setStayConnected(deviceId, false)

        // The point of the default is that it loses to a choice: a wearer who
        // turned the link off must not have it handed back by a later build.
        assertFalse(GarminDeviceStateStore(prefs).stayConnected(deviceId))
    }

    @Test
    fun `forgetting a watch takes its stay-connected choice with it`() {
        store.setStayConnected(deviceId, false)

        store.clear(deviceId)

        // Re-pairing is a fresh watch, and a fresh watch gets the default.
        assertTrue(GarminDeviceStateStore(prefs).stayConnected(deviceId))
    }

    @Test
    fun `clear drops the automatic sync schedule too`() {
        store.setAutoSyncInterval(deviceId, AutoSyncInterval.EVERY_2_HOURS)

        store.clear(deviceId)

        assertEquals(AutoSyncInterval.OFF, GarminDeviceStateStore(prefs).autoSyncInterval(deviceId))
    }
}
