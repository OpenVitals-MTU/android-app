package tech.mmarca.openvitals.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice

class BleDeviceRepositoryTest {

    /**
     * One prefs instance that survives across repository instances, so a
     * second [newRepository] over it IS the storage round-trip: it re-reads
     * the JSON the first one wrote.
     */
    private val prefs = FakeSharedPreferences()

    private val context: Context = mockk<Context>().also { context ->
        every { context.getSharedPreferences(any(), any()) } returns prefs
    }

    private fun newRepository() = BleDeviceRepository(context)

    private fun BleDeviceRepository.addStrap(): BleSensorDevice = addDevice(
        displayName = "Strap",
        address = "AA:BB:CC:DD:EE:FF",
        bluetoothName = "Strap",
        capabilities = setOf(BleSensorCapability.HEART_RATE),
    )

    @Test
    fun resolveAssignments_usesFirstEnabledDevicePerCapability() {
        val repo = newRepository()
        val hr = repo.addStrap()
        val power = repo.addDevice(
            displayName = "Power",
            address = "AA:BB:CC:DD:EE:02",
            bluetoothName = null,
            capabilities = setOf(BleSensorCapability.CYCLING_POWER),
        )

        val assignments = repo.resolveCapabilityAssignments()

        assertEquals(hr.id, assignments[BleSensorCapability.HEART_RATE]?.id)
        assertEquals(power.id, assignments[BleSensorCapability.CYCLING_POWER]?.id)
    }

    @Test
    fun disabledDevicesAreExcludedFromAssignments() {
        val repo = newRepository()
        val strap = repo.addStrap()
        repo.setDeviceEnabled(strap.id, enabled = false)

        assertEquals(0, repo.resolveCapabilityAssignments().size)
    }

    @Test
    fun `updateBatteryLevel stores a changed value`() {
        val repo = newRepository()
        val device = repo.addStrap()

        repo.updateBatteryLevel(device.id, 80)

        val stored = repo.devices.single()
        assertEquals(80, stored.batteryPercent)
        assertNotNull(stored.batteryUpdatedAt)
    }

    @Test
    fun `updateBatteryLevel clamps out-of-range values`() {
        val repo = newRepository()
        val device = repo.addStrap()

        repo.updateBatteryLevel(device.id, 150)
        assertEquals(100, repo.devices.single().batteryPercent)

        repo.updateBatteryLevel(device.id, -10)
        assertEquals(0, repo.devices.single().batteryPercent)
    }

    @Test
    fun `an identical battery reading does not re-persist or advance the stamp`() {
        val repo = newRepository()
        val device = repo.addStrap()
        repo.updateBatteryLevel(device.id, 75)
        val firstStamp = repo.devices.single().batteryUpdatedAt
        val firstPublished = repo.devicesFlow.value

        repo.updateBatteryLevel(device.id, 75)

        // A repeated identical read is a no-op: nothing is written, nothing is
        // published, and the stamp still says when the level last MOVED.
        assertEquals(firstStamp, repo.devices.single().batteryUpdatedAt)
        assertSame(firstPublished, repo.devicesFlow.value)
    }

    @Test
    fun `updateBatteryLevel ignores an unknown device id`() {
        val repo = newRepository()
        repo.addStrap()

        repo.updateBatteryLevel("does-not-exist", 50)

        assertNull(repo.devices.single().batteryPercent)
    }

    @Test
    fun `re-adding the same MAC updates the existing sensor`() {
        val repo = newRepository()
        val first = repo.addStrap()

        val second = repo.addDevice(
            displayName = "My strap",
            address = "aa:bb:cc:dd:ee:ff",
            bluetoothName = "TICKR",
            capabilities = setOf(BleSensorCapability.HEART_RATE, BleSensorCapability.CYCLING_CADENCE),
        )

        assertEquals(first.id, second.id)
        assertEquals(1, repo.devices.size)
        assertEquals("My strap", second.displayName)
        assertEquals("TICKR", second.bluetoothName)
        assertEquals(
            setOf(BleSensorCapability.HEART_RATE, BleSensorCapability.CYCLING_CADENCE),
            second.capabilities,
        )
    }

    @Test
    fun `a device with no capabilities stays out of assignments`() {
        val repo = newRepository()
        repo.addDevice(
            displayName = "Empty",
            address = "AA:BB:CC:DD:EE:03",
            bluetoothName = null,
            capabilities = emptySet(),
        )
        repo.addStrap()

        val assignments = repo.resolveCapabilityAssignments()

        assertEquals(1, assignments.size)
        assertTrue(BleSensorCapability.HEART_RATE in assignments)
    }

    @Test
    fun registryJson_oldJsonWithoutWatchFieldsRoundTripsAsSensor() {
        val oldJson = """
            [{"id":"hr","displayName":"HR Strap","address":"AA:BB:CC:DD:EE:01",
              "bluetoothName":"HRM","capabilities":["HEART_RATE"],"enabled":true,
              "wheelCircumferenceMm":null,"batteryPercent":80,
              "batteryUpdatedAt":1700000000000,"addedAt":1690000000000}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(oldJson)

        assertEquals(1, decoded.size)
        val device = decoded.single()
        assertEquals(setOf(BleSensorCapability.HEART_RATE), device.capabilities)
        assertEquals(80, device.batteryPercent)

        assertEquals(decoded, BleDeviceRegistryJson.decode(BleDeviceRegistryJson.encode(decoded)))
    }

    @Test
    fun registryJson_dropsWatchEraEntries() {
        // Retired watch-integration leftovers are not sensors: they are skipped
        // on read so they never reappear in Sensors settings.
        val watchEraJson = """
            [{"id":"ble-1753000000000000","displayName":"vívoactive 5",
              "address":"AA:BB:CC:DD:EE:05","bluetoothName":"vívoactive 5",
              "capabilities":[],"enabled":true,"wheelCircumferenceMm":null,
              "batteryPercent":null,"batteryUpdatedAt":null,
              "addedAt":1753000000000,"kind":"WATCH","integration":"GARMIN",
              "lastSyncedAt":1753100000000},
             {"id":"hr","displayName":"HR Strap","address":"AA:BB:CC:DD:EE:01",
              "bluetoothName":"HRM","capabilities":["HEART_RATE"],"enabled":true,
              "wheelCircumferenceMm":null,"batteryPercent":null,"batteryUpdatedAt":null,
              "addedAt":1690000000000}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(watchEraJson)

        assertEquals(1, decoded.size)
        assertEquals("hr", decoded.single().id)
        assertEquals(setOf(BleSensorCapability.HEART_RATE), decoded.single().capabilities)
    }

    @Test
    fun registryJson_ignoresLegacyKindFieldsOnSensors() {
        // Bike-computer / sensor rows from older builds still load; kind and
        // integration are ignored and not written back.
        val legacyJson = """
            [{"id":"edge","displayName":"Edge 840","address":"E0:48:24:D5:F7:20",
              "bluetoothName":"Edge 840","capabilities":["CYCLING_POWER"],"enabled":true,
              "wheelCircumferenceMm":null,"batteryPercent":null,"batteryUpdatedAt":null,
              "addedAt":1753000000000,"kind":"BIKE_COMPUTER","integration":"GARMIN",
              "lastSyncedAt":null}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(legacyJson)
        assertEquals(1, decoded.size)
        assertEquals(setOf(BleSensorCapability.CYCLING_POWER), decoded.single().capabilities)

        val encoded = BleDeviceRegistryJson.encode(decoded)
        assertTrue("kind" !in encoded)
        assertTrue("integration" !in encoded)
        assertTrue("lastSyncedAt" !in encoded)
    }

    @Test
    fun registryJson_unknownKindIsTreatedAsSensor() {
        val futureJson = """
            [{"id":"x","displayName":"Future Device","address":"AA:BB:CC:DD:EE:09",
              "bluetoothName":null,"capabilities":["HEART_RATE"],"enabled":true,
              "wheelCircumferenceMm":null,"addedAt":1753000000000,
              "kind":"HOLOGRAPH","integration":"NEURALINK"}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(futureJson)

        assertEquals(1, decoded.size)
        assertEquals(setOf(BleSensorCapability.HEART_RATE), decoded.single().capabilities)
    }

    @Test
    fun `rewriting the registry does not resurrect dropped watch entries`() {
        prefs.edit().putString(
            "devices",
            """[{"id":"w","displayName":"vívoactive 5","address":"E0:48:24:D5:F7:10",
              "bluetoothName":"vívoactive 5","capabilities":[],"enabled":true,
              "wheelCircumferenceMm":null,"batteryPercent":null,"batteryUpdatedAt":null,
              "addedAt":1753000000000,"kind":"WATCH","integration":"GARMIN",
              "lastSyncedAt":1753100000000}]""",
        ).commit()

        val repo = newRepository()
        assertTrue(repo.devices.isEmpty())

        repo.addStrap()

        val reloaded = newRepository().devices
        assertEquals(1, reloaded.size)
        assertEquals("Strap", reloaded.single().displayName)
    }
}
