package tech.mmarca.openvitals.devices.wearos

import tech.mmarca.openvitals.devices.core.pairing.CompanionFilter
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/** Records what the use case asked of the pairing layer, and can force a declined or throwing association. */
private class FakePairing : WatchPairingPort {
    var associateResult = true
    var associateError: Exception? = null
    var seenAssociateAddress: String? = null
    var seenAssociateFilter: CompanionFilter? = null
    var seenDisassociateAddress: String? = null

    override suspend fun associateCompanion(
        address: String,
        displayName: String?,
        filter: CompanionFilter,
    ): Boolean {
        seenAssociateAddress = address
        seenAssociateFilter = filter
        associateError?.let { throw it }
        return associateResult
    }

    override suspend fun disassociateCompanion(address: String) {
        seenDisassociateAddress = address
    }

    // A WearOS watch never bonds: these must never be called.
    override suspend fun bond(address: String): WatchBondResult =
        error("WearOS onboarding must not bond")

    override suspend fun removeBond(address: String): Unit =
        error("WearOS onboarding must not touch bonds")
}

/** Answers the bond lookup with [bondAddress], or throws [bondError]. */
private class FakeNodePort : WearOsNodePort {
    var bondAddress: String? = null
    var bondError: Exception? = null
    var seenBondLookup: Pair<String, String?>? = null

    override suspend fun checkStatus(targetAddress: String?, targetName: String?): WearOsCompanionStatus =
        error("Onboarding must not ping")

    override suspend fun findBondAddress(address: String, name: String?): String? {
        seenBondLookup = address to name
        bondError?.let { throw it }
        return bondAddress
    }
}

class OnboardWearOsWatchUseCaseTest {

    private lateinit var repo: BleDeviceRepository
    private lateinit var pairing: FakePairing
    private lateinit var nodePort: FakeNodePort
    private lateinit var useCase: OnboardWearOsWatchUseCase

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        pairing = FakePairing()
        nodePort = FakeNodePort()
        useCase = OnboardWearOsWatchUseCase(pairing, repo, nodePort)
    }

    private fun watch() = BleDiscoveredDevice(
        address = "A8:D1:62:BE:3A:3B",
        name = "Galaxy Watch8 (89FZ)",
        rssi = -50,
        suggestedCapabilities = emptySet(),
    )

    @Test
    fun `registers a (watch, wearos) device, no bond`() = runTest {
        val outcome = useCase(watch(), displayName = "My Watch")

        assertTrue(outcome.associated)
        assertEquals("A8:D1:62:BE:3A:3B", pairing.seenAssociateAddress)
        // The address may be the Classic bond's; a BLE-only dialog would search forever.
        assertEquals(CompanionFilter.BLE_OR_CLASSIC, pairing.seenAssociateFilter)
        val device = repo.devices.single()
        assertEquals(BleDeviceKind.WATCH, device.kind)
        assertEquals(DeviceIntegration.WEAROS, device.integration)
        assertTrue(device.isWearosWatch)
        assertFalse(device.isGarminWatch)
        assertTrue(device.capabilities.isEmpty())
    }

    @Test
    fun `a declined association still onboards the watch`() = runTest {
        pairing.associateResult = false

        val outcome = useCase(watch(), displayName = "My Watch")

        assertFalse(outcome.associated)
        assertTrue(repo.devices.single().isWearosWatch)
    }

    @Test
    fun `a thrown association is swallowed - the watch still registers`() = runTest {
        pairing.associateError = IllegalStateException("no companion service")

        val outcome = useCase(watch(), displayName = "My Watch")

        assertFalse(outcome.associated)
        assertTrue(repo.devices.single().isWearosWatch)
    }

    @Test
    fun `onboarding the same watch twice does not duplicate the registry entry`() = runTest {
        useCase(watch(), displayName = "My Watch")
        useCase(watch(), displayName = "Renamed Watch")

        val device = repo.devices.single()
        assertEquals("Renamed Watch", device.displayName)
        assertTrue(device.isWearosWatch)
    }

    @Test
    fun `stores the bonded Classic address found for the scanned watch`() = runTest {
        nodePort.bondAddress = "a8:d1:62:be:3a:3b"
        val scanned = watch().copy(address = "7F:12:34:56:78:9A", name = "Galaxy Watch8 (89FZ) LE")

        val outcome = useCase(scanned, displayName = "My Watch")

        assertEquals("7F:12:34:56:78:9A" to "Galaxy Watch8 (89FZ) LE", nodePort.seenBondLookup)
        // The registry keeps the scan address; the ping goes to the bond.
        assertEquals("7F:12:34:56:78:9A", outcome.device.address)
        assertEquals("A8:D1:62:BE:3A:3B", outcome.device.classicAddress)
        assertEquals("A8:D1:62:BE:3A:3B", repo.devices.single().classicAddress)
    }

    @Test
    fun `no matching bond leaves the Classic address empty`() = runTest {
        val outcome = useCase(watch(), displayName = "My Watch")

        assertNull(outcome.device.classicAddress)
        assertNull(repo.devices.single().classicAddress)
    }

    @Test
    fun `a failed bond lookup still onboards the watch`() = runTest {
        nodePort.bondError = SecurityException("BLUETOOTH_CONNECT")

        val outcome = useCase(watch(), displayName = "My Watch")

        assertNull(outcome.device.classicAddress)
        assertTrue(repo.devices.single().isWearosWatch)
    }

    @Test
    fun `forget drops the companion association`() = runTest {
        useCase.forget("A8:D1:62:BE:3A:3B")
        assertEquals("A8:D1:62:BE:3A:3B", pairing.seenDisassociateAddress)
    }

    @Test
    fun `reports the associating step before the dialog`() = runTest {
        val steps = mutableListOf<WearOsOnboardStep>()

        useCase(watch(), displayName = "My Watch") { steps.add(it) }

        assertEquals(listOf(WearOsOnboardStep.ASSOCIATING), steps)
    }
}
