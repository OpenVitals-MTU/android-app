package tech.mmarca.openvitals.features.scales

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.contract.FakeScaleWeighInRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.ScaleRecordKind
import tech.mmarca.openvitals.devices.xiaomi.ScaleWeighInWriter
import tech.mmarca.openvitals.devices.xiaomi.ScaleWritePermissions
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleListener
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleStore
import tech.mmarca.openvitals.domain.insights.BodyCompositionInput
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The device screen's own decisions over one set-up scale. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScaleDeviceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val key = "0728974d657a4b60964c1b1677f35f7c".hexToByteArray()
    private val store = XiaomiScaleStore(FakeSharedPreferences()).also { it.setUp("8C:D0:B2:F6:BE:EF", "Bathroom", key) }
    private val listener = mockk<XiaomiScaleListener>(relaxed = true) {
        every { status } returns MutableStateFlow(ScaleListenerStatus.LISTENING)
        every { isWokenBySystem() } returns true
        every { rename(any()) } answers { store.rename(firstArg()) }
        every { changeKey(any()) } answers { store.changeKey(firstArg()) }
        every { forget() } answers { store.clear() }
    }
    private val writer = mockk<ScaleWeighInWriter>(relaxed = true) {
        coEvery { missingProfileInputs() } returns setOf(BodyCompositionInput.SEX)
    }
    private val healthRepository = mockk<HealthRepository> {
        coEvery { grantedPermissions() } returns setOf(ScaleRecordKind.WEIGHT.writePermission)
    }

    private fun viewModel() = ScaleDeviceViewModel(
        store = store,
        listener = listener,
        writer = writer,
        weighIns = FakeScaleWeighInRepository(),
        healthRepository = healthRepository,
        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
    )

    @Test
    fun `the screen shows the scale as stored, and what Health Connect and the profile still lack`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals("Bathroom" to "8C:D0:B2:F6:BE:EF", state.name to state.address)
        assertNull("no weigh-in yet", state.profile)
        assertTrue(state.wokenBySystem)
        assertEquals(ScaleWritePermissions - ScaleRecordKind.WEIGHT.writePermission, state.missingWritePermissions)
        assertEquals(setOf(BodyCompositionInput.SEX), state.missingProfileInputs)
    }

    @Test
    fun `renaming trims and ignores an empty name, a key change keeps the scale, and removal forgets it`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.rename("  Hall scale ")
        viewModel.rename("   ")
        assertEquals("Hall scale", viewModel.uiState.value.name)

        assertFalse(viewModel.changeKey("nope"))
        assertTrue(viewModel.changeKey("ffffffffffffffffffffffffffffffff"))
        verify(exactly = 1) { listener.changeKey(any()) }
        assertEquals("8C:D0:B2:F6:BE:EF", viewModel.uiState.value.address)

        viewModel.remove()
        assertNull(viewModel.uiState.value.name)
    }

    @Test
    fun `a delete Health Connect refuses for a missing grant shows the grant affordance`() = runTest {
        val repository = FakeScaleWeighInRepository()
        repository.merge(1744250605, 1, tech.mmarca.openvitals.domain.model.ScaleReading(weightKg = 69.9),
            java.time.Instant.ofEpochSecond(1744250605), java.time.Instant.ofEpochSecond(1744250625))
        coEvery { writer.delete(any()) } throws SecurityException("no grant")
        val viewModel = ScaleDeviceViewModel(store, listener, writer, repository, healthRepository,
            UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }))
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.deleteLastWeighIn()

        assertEquals(ScreenError.PermissionDenied, viewModel.uiState.value.screenError)
    }
}
