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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
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

/** The screen's own decisions. The listening and the saving are the device layer's, and tested there. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScalesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val store = XiaomiScaleStore(FakeSharedPreferences())
    private val listener = mockk<XiaomiScaleListener>(relaxed = true) {
        every { status } returns MutableStateFlow(ScaleListenerStatus.LISTENING)
        every { useKey(any()) } answers { store.setBindKey(firstArg()) }
    }
    private val writer = mockk<ScaleWeighInWriter>(relaxed = true) {
        coEvery { missingProfileInputs() } returns setOf(BodyCompositionInput.SEX)
    }
    private val healthRepository = mockk<HealthRepository> {
        coEvery { grantedPermissions() } returns setOf(ScaleRecordKind.WEIGHT.writePermission)
    }

    private fun viewModel() = ScalesViewModel(
        store = store,
        listener = listener,
        writer = writer,
        weighIns = FakeScaleWeighInRepository(),
        healthRepository = healthRepository,
        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
        dateTimeFormatters = DateTimeFormatterProvider(),
    )

    @Test
    fun `text that is not a key is flagged and never reaches the listener`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.onKeyInputChange("not a key")
        viewModel.saveKey()

        assertTrue(viewModel.uiState.value.keyInputInvalid)
        assertTrue(viewModel.uiState.value.showKeyField)
        verify(exactly = 0) { listener.useKey(any()) }

        // Typing again withdraws the complaint.
        viewModel.onKeyInputChange("0")
        assertFalse(viewModel.uiState.value.keyInputInvalid)
    }

    @Test
    fun `a key starts the listening, leaves the field, and shows what is still missing`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.onKeyInputChange("0728974d657a4b60964c1b1677f35f7c")
        viewModel.saveKey()

        val state = viewModel.uiState.value
        verify(exactly = 1) { listener.useKey(any()) }
        assertTrue(state.hasKey)
        assertFalse(state.showKeyField)
        // The key does not linger in screen state.
        assertEquals("", state.keyInput)
        assertEquals(ScaleWritePermissions - ScaleRecordKind.WEIGHT.writePermission, state.missingWritePermissions)
        assertEquals(setOf(BodyCompositionInput.SEX), state.missingProfileInputs)
    }
}
