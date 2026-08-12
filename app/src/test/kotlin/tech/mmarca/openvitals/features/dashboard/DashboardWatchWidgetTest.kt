package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.garmin.GarminRealtimeState
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * The dashboard's watch tile. Device state rather than a metric, so it is built
 * straight from the registry instead of a loaded day.
 */
class DashboardWatchWidgetTest {

    private fun device(
        name: String,
        kind: BleDeviceKind,
        address: String = "AA:BB:CC:DD:EE:FF",
        batteryPercent: Int? = null,
        lastSyncedAt: Instant? = null,
        capabilities: Set<BleSensorCapability> = emptySet(),
    ) = BleSensorDevice(
        id = name,
        displayName = name,
        address = address,
        bluetoothName = name,
        capabilities = capabilities,
        enabled = true,
        wheelCircumferenceMm = null,
        batteryPercent = batteryPercent,
        addedAt = Instant.EPOCH,
        kind = kind,
        integration = DeviceIntegration.GARMIN,
        lastSyncedAt = lastSyncedAt,
    )

    @Test
    fun `no watch paired means no tile at all`() {
        val sensors = listOf(device("TICKR", BleDeviceKind.SENSOR))

        // An empty "no data" watch tile would be noise on the dashboard of
        // someone who owns no watch.
        assertNull(sensors.toWatchWidgetDisplay())
    }

    @Test
    fun `the watch being synced owns the tile and shows it`() {
        val idle = device("fēnix 7", BleDeviceKind.WATCH, address = "AA:BB:CC:DD:EE:01",
            lastSyncedAt = Instant.parse("2026-08-12T10:26:00Z"))
        val syncing = device("vívoactive 5", BleDeviceKind.WATCH, address = "AA:BB:CC:DD:EE:02")

        val display = requireNotNull(
            listOf(idle, syncing).toWatchWidgetDisplay(syncingDeviceId = "vívoactive 5"),
        )

        // Otherwise the spinner would sit on a watch that is not the one
        // actually working.
        assertEquals("vívoactive 5", display.name)
        assertTrue(display.isSyncing)
    }

    @Test
    fun `a paired watch fills the tile`() {
        val watch = device(
            "vívoactive 5",
            BleDeviceKind.WATCH,
            batteryPercent = 62,
            lastSyncedAt = Instant.parse("2026-08-12T10:26:00Z"),
        )

        val display = requireNotNull(listOf(watch).toWatchWidgetDisplay())

        assertEquals("vívoactive 5", display.name)
        assertEquals(62, display.batteryPercent)
        assertEquals(Instant.parse("2026-08-12T10:26:00Z"), display.lastSyncedAt)
        assertEquals(0, display.additionalCount)
    }

    @Test
    fun `the most recently synced watch takes the tile`() {
        val older = device(
            "fēnix 7",
            BleDeviceKind.WATCH,
            address = "AA:BB:CC:DD:EE:01",
            lastSyncedAt = Instant.parse("2026-08-10T08:00:00Z"),
        )
        val newer = device(
            "vívoactive 5",
            BleDeviceKind.WATCH,
            address = "AA:BB:CC:DD:EE:02",
            lastSyncedAt = Instant.parse("2026-08-12T10:26:00Z"),
        )

        val display = requireNotNull(listOf(older, newer).toWatchWidgetDisplay())

        // The tile follows the watch actually in use, not registry order.
        assertEquals("vívoactive 5", display.name)
        assertEquals(1, display.additionalCount)
    }

    @Test
    fun `a never-synced watch does not outrank one that has synced`() {
        val neverSynced = device("fēnix 7", BleDeviceKind.WATCH, address = "AA:BB:CC:DD:EE:01")
        val synced = device(
            "vívoactive 5",
            BleDeviceKind.WATCH,
            address = "AA:BB:CC:DD:EE:02",
            lastSyncedAt = Instant.parse("2026-08-12T10:26:00Z"),
        )

        val display = requireNotNull(listOf(neverSynced, synced).toWatchWidgetDisplay())

        assertEquals("vívoactive 5", display.name)
    }

    @Test
    fun `live readings from the held link reach the tile`() {
        val watch = device("vívoactive 5", BleDeviceKind.WATCH, batteryPercent = 62)
        val now = Instant.parse("2026-08-12T10:26:00Z")
        val live = GarminRealtimeState(
            heartRateBpm = 71,
            heartRateAt = now,
            steps = 4200,
            stepsAt = now,
        )

        val display = requireNotNull(
            listOf(watch).toWatchWidgetDisplay(live = live, now = now.plusSeconds(30)),
        )

        // Only one watch can hold a link at a time, so the live values belong
        // to the watch on the tile or to nothing.
        assertEquals(71, display.liveHeartRateBpm)
        assertEquals(4200, display.liveSteps)
    }

    @Test
    fun `a live reading that stopped arriving leaves the tile`() {
        val watch = device("vívoactive 5", BleDeviceKind.WATCH)
        val live = GarminRealtimeState(
            heartRateBpm = 71,
            heartRateAt = Instant.parse("2020-01-01T00:00:00Z"),
        )

        val display = requireNotNull(
            listOf(watch).toWatchWidgetDisplay(
                live = live,
                now = Instant.parse("2020-01-01T01:00:00Z"),
            ),
        )

        // "Now: 71 bpm" from an hour ago is a lie the tile would keep telling.
        assertNull(display.liveHeartRateBpm)
    }

    @Test
    fun `a bike computer is not a watch`() {
        val edge = device("Edge 840", BleDeviceKind.BIKE_COMPUTER)

        // An Edge has its own place among the sensors; it is not a watch.
        assertNull(listOf(edge).toWatchWidgetDisplay())
    }
}
