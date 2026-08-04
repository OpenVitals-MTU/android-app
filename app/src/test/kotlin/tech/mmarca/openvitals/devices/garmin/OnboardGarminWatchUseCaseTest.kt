package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.pairing.WatchBondResult
import tech.mmarca.openvitals.devices.core.pairing.WatchPairingPort
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorCapability

/**
 * Records what the use case asked the platform to do, and answers with
 * whatever the test set up. No radio, no Activity, no plugin.
 */
private class FakePairing : WatchPairingPort {
    var bondResult = WatchBondResult.BONDED
    var associateResult = true

    val calls = mutableListOf<String>()

    override suspend fun bond(address: String): WatchBondResult {
        calls.add("bond:$address")
        return bondResult
    }

    override suspend fun removeBond(address: String) {
        calls.add("removeBond:$address")
    }

    override suspend fun associateCompanion(address: String, displayName: String?): Boolean {
        calls.add("associate:$address")
        return associateResult
    }

    override suspend fun disassociateCompanion(address: String) {
        calls.add("disassociate:$address")
    }
}

/** Answers with a canned GATT verdict; never opens a connection. */
private class FakeProbe : GarminTransportProbe {
    var variant = GarminTransportVariant.V1
    val calls = mutableListOf<String>()

    override suspend fun probe(address: String): GarminGattReport {
        calls.add("probe:$address")
        return GarminGattReport(
            address = address,
            variant = variant,
            services = emptyList(),
        )
    }
}

class OnboardGarminWatchUseCaseTest {

    private lateinit var repo: BleDeviceRepository
    private lateinit var pairing: FakePairing
    private lateinit var probe: FakeProbe
    private lateinit var useCase: OnboardGarminWatchUseCase

    private val watch = BleDiscoveredDevice(
        address = "E0:48:24:D5:F7:10",
        name = "vívoactive 5",
        rssi = -55,
        suggestedCapabilities = emptySet(),
        advertisesSyncService = true,
    )

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
        repo = BleDeviceRepository(context)
        pairing = FakePairing()
        probe = FakeProbe()
        useCase = OnboardGarminWatchUseCase(pairing, repo, probe)
    }

    @Test
    fun `registers a bonded watch with no capabilities`() = runTest {
        val outcome = useCase(watch, displayName = "My watch")

        assertTrue(outcome is GarminOnboardOutcome.Succeeded)
        val registered = (outcome as GarminOnboardOutcome.Succeeded).device
        assertEquals(BleDeviceKind.WATCH, registered.kind)
        assertTrue(registered.isWatch)
        assertTrue(registered.capabilities.isEmpty())
        assertEquals("My watch", registered.displayName)
        assertEquals("E0:48:24:D5:F7:10", registered.address)
        assertTrue(outcome.associated)
    }

    @Test
    fun `registers an Edge as a bike computer with no capabilities`() = runTest {
        val outcome = useCase(
            watch,
            displayName = "Edge 840",
            kind = BleDeviceKind.BIKE_COMPUTER,
        )

        assertTrue(outcome is GarminOnboardOutcome.Succeeded)
        val registered = (outcome as GarminOnboardOutcome.Succeeded).device
        assertEquals(BleDeviceKind.BIKE_COMPUTER, registered.kind)
        assertTrue(registered.isBikeComputer)
        assertFalse(registered.isWatch)
        assertTrue(registered.isGarminGfdi)
        // Live capabilities are opted in later from the device card, so it
        // starts capability-less and out of the recording coordinator's
        // assignment.
        assertTrue(registered.capabilities.isEmpty())
    }

    @Test
    fun `a registered watch never takes part in capability assignment`() = runTest {
        // A real sensor owning heart rate, so the assignment map is not empty
        // for the wrong reason.
        repo.addDevice(
            displayName = "Chest strap",
            address = "AA:BB:CC:DD:EE:FF",
            bluetoothName = "Wahoo TICKR",
            capabilities = setOf(BleSensorCapability.HEART_RATE),
        )

        useCase(watch, displayName = "vívoactive 5")

        val assignments = repo.resolveCapabilityAssignments()
        assertEquals(
            "Chest strap",
            assignments[BleSensorCapability.HEART_RATE]?.displayName,
        )
        assertFalse(
            "a watch in the assignment map would be connected to and polled " +
                "by the recording coordinator, which it cannot answer",
            assignments.values.any { it.isWatch },
        )
    }

    @Test
    fun `a refused pairing writes nothing to the registry`() = runTest {
        pairing.bondResult = WatchBondResult.REFUSED

        val outcome = useCase(watch, displayName = "vívoactive 5")

        assertTrue(outcome is GarminOnboardOutcome.Failed)
        assertEquals(GarminOnboardStep.BONDING, (outcome as GarminOnboardOutcome.Failed).step)
        assertTrue(repo.devices.isEmpty())
        // The association is never even attempted — there is nothing to
        // associate.
        assertEquals(listOf("bond:E0:48:24:D5:F7:10"), pairing.calls)
    }

    @Test
    fun `an unreachable watch writes nothing to the registry`() = runTest {
        pairing.bondResult = WatchBondResult.UNREACHABLE

        val outcome = useCase(watch, displayName = "vívoactive 5")

        assertEquals(
            WatchBondResult.UNREACHABLE,
            (outcome as GarminOnboardOutcome.Failed).reason,
        )
        assertTrue(repo.devices.isEmpty())
    }

    @Test
    fun `a declined companion association still onboards the watch`() = runTest {
        pairing.associateResult = false

        val outcome = useCase(watch, displayName = "vívoactive 5")

        // The whole point: the association buys background priority, not
        // access.
        assertTrue(outcome is GarminOnboardOutcome.Succeeded)
        assertFalse((outcome as GarminOnboardOutcome.Succeeded).associated)
        assertEquals(1, repo.devices.size)
    }

    @Test
    fun `an already-bonded watch is registered without re-prompting`() = runTest {
        pairing.bondResult = WatchBondResult.ALREADY_BONDED

        val outcome = useCase(watch, displayName = "vívoactive 5")

        assertTrue(outcome is GarminOnboardOutcome.Succeeded)
        assertEquals(1, repo.devices.size)
    }

    @Test
    fun `reports each platform step before it shows its dialog`() = runTest {
        val steps = mutableListOf<GarminOnboardStep>()

        useCase(watch, displayName = "vívoactive 5", onStep = steps::add)

        assertEquals(
            listOf(
                GarminOnboardStep.BONDING,
                GarminOnboardStep.ASSOCIATING,
                GarminOnboardStep.PROBING,
            ),
            steps,
        )
    }

    @Test
    fun `the probe runs only after bonding succeeds`() = runTest {
        pairing.bondResult = WatchBondResult.REFUSED

        useCase(watch, displayName = "vívoactive 5")

        // Probing an unbonded watch enumerates a link with no encryption, so
        // the GFDI characteristics are absent and the verdict would read
        // "unknown" — a false negative that looks exactly like an unsupported
        // device.
        assertTrue(probe.calls.isEmpty())
    }

    @Test
    fun `an unsupported transport still onboards, and is reported`() = runTest {
        probe.variant = GarminTransportVariant.UNKNOWN

        val outcome = useCase(watch, displayName = "vívoactive 5")

        // Refusing a pairing the user just confirmed on the watch would be
        // worse than registering one that cannot sync yet and saying so.
        assertTrue(outcome is GarminOnboardOutcome.Succeeded)
        val succeeded = outcome as GarminOnboardOutcome.Succeeded
        assertEquals(GarminTransportVariant.UNKNOWN, succeeded.transport.variant)
        assertFalse(succeeded.transport.isSupported)
        assertEquals(1, repo.devices.size)
    }

    @Test
    fun `a v2 watch reports as supported`() = runTest {
        probe.variant = GarminTransportVariant.V2

        val outcome = useCase(watch, displayName = "vívoactive 5")

        assertTrue((outcome as GarminOnboardOutcome.Succeeded).transport.isSupported)
    }

    @Test
    fun `forget drops the association and the bond, in that order`() = runTest {
        useCase.forget("E0:48:24:D5:F7:10")

        assertEquals(
            listOf(
                "disassociate:E0:48:24:D5:F7:10",
                "removeBond:E0:48:24:D5:F7:10",
            ),
            pairing.calls,
        )
    }
}
