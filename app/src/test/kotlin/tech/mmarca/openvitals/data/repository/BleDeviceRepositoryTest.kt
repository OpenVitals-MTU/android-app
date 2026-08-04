package tech.mmarca.openvitals.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

class BleDeviceRepositoryTest {

    /**
     * One prefs instance that survives across repository instances, so a
     * second [newRepository] over it IS the storage round-trip: it re-reads
     * the JSON the first one wrote. Tests can also seed it directly with
     * registry JSON, which is how watch-era entries (no longer creatable
     * through the API) get into storage.
     */
    private val prefs = FakeSharedPreferences()

    private val context: Context = mockk<Context>().also { context ->
        every { context.getSharedPreferences(any(), any()) } returns prefs
    }

    private fun newRepository() = BleDeviceRepository(context)

    private fun seedRegistry(devices: List<BleSensorDevice>) {
        prefs.edit().putString("devices", BleDeviceRegistryJson.encode(devices)).commit()
    }

    private fun BleDeviceRepository.addStrap(): BleSensorDevice = addDevice(
        displayName = "Strap",
        address = "AA:BB:CC:DD:EE:FF",
        bluetoothName = "Strap",
        capabilities = setOf(BleSensorCapability.HEART_RATE),
    )

    private fun storedDevice(
        id: String,
        displayName: String,
        address: String,
        capabilities: Set<BleSensorCapability>,
        kind: BleDeviceKind,
        integration: DeviceIntegration? = null,
        lastSyncedAt: Instant? = null,
    ): BleSensorDevice = BleSensorDevice(
        id = id,
        displayName = displayName,
        address = address,
        bluetoothName = displayName,
        capabilities = capabilities,
        enabled = true,
        wheelCircumferenceMm = null,
        addedAt = Instant.parse("2026-01-01T00:00:00Z"),
        kind = kind,
        integration = integration,
        lastSyncedAt = lastSyncedAt,
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
    fun `a stored watch-era entry is kept out of capability assignment`() {
        // Migrated registries can still contain watch entries the retired
        // watch integration wrote. Defensive: even if such an entry somehow
        // carried capabilities, it must not be handed to the recording
        // coordinator, which would connect to it and wait for notifications
        // it never sends.
        seedRegistry(
            listOf(
                storedDevice(
                    id = "w",
                    displayName = "vívoactive 5",
                    address = "E0:48:24:D5:F7:10",
                    capabilities = setOf(BleSensorCapability.HEART_RATE),
                    kind = BleDeviceKind.WATCH,
                    integration = DeviceIntegration.GARMIN,
                ),
            ),
        )
        val repo = newRepository()
        val sensor = repo.addStrap()

        val assignments = repo.resolveCapabilityAssignments()

        assertEquals(sensor.id, assignments[BleSensorCapability.HEART_RATE]?.id)
        assertFalse(assignments.values.any { it.kind == BleDeviceKind.WATCH })
    }

    @Test
    fun `an Edge bike computer with capabilities DOES take part`() {
        seedRegistry(
            listOf(
                storedDevice(
                    id = "edge",
                    displayName = "Edge 840",
                    address = "E0:48:24:D5:F7:20",
                    capabilities = setOf(BleSensorCapability.CYCLING_POWER),
                    kind = BleDeviceKind.BIKE_COMPUTER,
                    integration = DeviceIntegration.GARMIN,
                ),
            ),
        )
        val repo = newRepository()
        repo.addStrap()

        val assignments = repo.resolveCapabilityAssignments()

        // Unlike a watch entry, a bike computer broadcasting standard GATT is
        // a live source the recording coordinator should connect to.
        assertEquals("edge", assignments[BleSensorCapability.CYCLING_POWER]?.id)
    }

    @Test
    fun `a bike computer with NO capabilities stays out`() {
        seedRegistry(
            listOf(
                storedDevice(
                    id = "edge",
                    displayName = "Edge 840",
                    address = "E0:48:24:D5:F7:20",
                    capabilities = emptySet(),
                    kind = BleDeviceKind.BIKE_COMPUTER,
                    integration = DeviceIntegration.GARMIN,
                ),
            ),
        )
        val repo = newRepository()
        repo.addStrap()

        val assignments = repo.resolveCapabilityAssignments()

        assertFalse(assignments.values.any { it.kind == BleDeviceKind.BIKE_COMPUTER })
    }

    @Test
    fun `watch-era fields survive a rewrite of the registry`() {
        // The repository no longer writes watch entries itself, but a rewrite
        // triggered by an ordinary sensor edit must not corrupt the stored
        // watch-era metadata riding alongside.
        val syncedAt = Instant.parse("2026-07-21T09:30:00Z")
        seedRegistry(
            listOf(
                storedDevice(
                    id = "w",
                    displayName = "vívoactive 5",
                    address = "E0:48:24:D5:F7:10",
                    capabilities = emptySet(),
                    kind = BleDeviceKind.WATCH,
                    integration = DeviceIntegration.GARMIN,
                    lastSyncedAt = syncedAt,
                ),
            ),
        )
        val repo = newRepository()
        repo.addStrap() // Forces a full registry rewrite.

        // A second repository over the same prefs IS the round-trip: it
        // re-reads the JSON the first one wrote.
        val reloaded = newRepository().devices.first { it.id == "w" }

        assertEquals(BleDeviceKind.WATCH, reloaded.kind)
        assertEquals(DeviceIntegration.GARMIN, reloaded.integration)
        assertEquals(syncedAt, reloaded.lastSyncedAt)
        assertFalse(reloaded.isLiveSensorCapable)
    }

    @Test
    fun registryJson_oldJsonWithoutWatchFieldsRoundTripsAsSensor() {
        // The exact shape the Kotlin build wrote before the retired watch
        // integration existed — no kind, integration or lastSyncedAt keys.
        val oldJson = """
            [{"id":"hr","displayName":"HR Strap","address":"AA:BB:CC:DD:EE:01",
              "bluetoothName":"HRM","capabilities":["HEART_RATE"],"enabled":true,
              "wheelCircumferenceMm":null,"batteryPercent":80,
              "batteryUpdatedAt":1700000000000,"addedAt":1690000000000}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(oldJson)

        assertEquals(1, decoded.size)
        val device = decoded.single()
        assertEquals(BleDeviceKind.SENSOR, device.kind)
        assertNull(device.integration)
        assertNull(device.lastSyncedAt)
        assertEquals(setOf(BleSensorCapability.HEART_RATE), device.capabilities)

        // And it round-trips: re-encoding then re-decoding keeps the device
        // meaning what it meant.
        assertEquals(decoded, BleDeviceRegistryJson.decode(BleDeviceRegistryJson.encode(decoded)))
    }

    @Test
    fun registryJson_decodesWatchEraJsonWithoutCrashing() {
        // What the retired watch integration wrote for an onboarded Garmin
        // watch: storageName strings for kind/integration. Still decodes
        // losslessly — the entry is ignored by the sensors UI, never dropped.
        val watchEraJson = """
            [{"id":"ble-1753000000000000","displayName":"vívoactive 5",
              "address":"AA:BB:CC:DD:EE:05","bluetoothName":"vívoactive 5",
              "capabilities":[],"enabled":true,"wheelCircumferenceMm":null,
              "batteryPercent":null,"batteryUpdatedAt":null,
              "addedAt":1753000000000,"kind":"WATCH","integration":"GARMIN",
              "lastSyncedAt":1753100000000}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(watchEraJson)

        assertEquals(1, decoded.size)
        val device = decoded.single()
        assertEquals(BleDeviceKind.WATCH, device.kind)
        assertEquals(DeviceIntegration.GARMIN, device.integration)
        assertEquals(Instant.ofEpochMilli(1_753_100_000_000), device.lastSyncedAt)
        assertFalse(device.isLiveSensorCapable)

        // And it round-trips unchanged.
        assertEquals(decoded, BleDeviceRegistryJson.decode(BleDeviceRegistryJson.encode(decoded)))
    }

    @Test
    fun registryJson_wearosIntegrationStillDecodes() {
        seedRegistry(
            listOf(
                storedDevice(
                    id = "gw",
                    displayName = "Galaxy Watch8",
                    address = "A8:D1:62:BE:3A:3B",
                    capabilities = setOf(BleSensorCapability.HEART_RATE),
                    kind = BleDeviceKind.WATCH,
                    integration = DeviceIntegration.WEAROS,
                ),
            ),
        )

        val reloaded = newRepository().devices.single()

        assertEquals(DeviceIntegration.WEAROS, reloaded.integration)
        assertEquals(BleDeviceKind.WATCH, reloaded.kind)
    }

    @Test
    fun registryJson_unknownKindDegradesToSensorInsteadOfCrashing() {
        val futureJson = """
            [{"id":"x","displayName":"Future Device","address":"AA:BB:CC:DD:EE:09",
              "bluetoothName":null,"capabilities":[],"enabled":true,
              "wheelCircumferenceMm":null,"addedAt":1753000000000,
              "kind":"HOLOGRAPH","integration":"NEURALINK"}]
        """.trimIndent()

        val decoded = BleDeviceRegistryJson.decode(futureJson)

        assertEquals(1, decoded.size)
        assertEquals(BleDeviceKind.SENSOR, decoded.single().kind)
        assertNull(decoded.single().integration)
    }

    @Test
    fun `a watch-era entry stays out of the assignments even when it is the only device`() {
        seedRegistry(
            listOf(
                storedDevice(
                    id = "w",
                    displayName = "vívoactive 5",
                    address = "E0:48:24:D5:F7:10",
                    capabilities = setOf(BleSensorCapability.HEART_RATE),
                    kind = BleDeviceKind.WATCH,
                ),
            ),
        )

        assertTrue(newRepository().resolveCapabilityAssignments().isEmpty())
    }
}
