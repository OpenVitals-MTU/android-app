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
import tech.mmarca.openvitals.core.permissions.OsPermissionsService
import tech.mmarca.openvitals.data.repository.contract.FakeScaleWeighInRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.devices.core.pairing.CompanionDevice
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleListener
import tech.mmarca.openvitals.devices.xiaomi.XiaomiScaleStore
import tech.mmarca.openvitals.domain.model.OsPermissionCatalog
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.domain.model.OsPermissionRow
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The add flow's decisions. The listening and the saving are the device layer's, and tested there. */
@OptIn(ExperimentalCoroutinesApi::class)
class ScalesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val scale = CompanionDevice("8C:D0:B2:F6:BE:EF", "Xiaomi Scale S400 BEEF")
    private val store = XiaomiScaleStore(FakeSharedPreferences())
    private var found: CompanionDevice? = scale
    private val listener = mockk<XiaomiScaleListener>(relaxed = true) {
        every { status } returns MutableStateFlow(ScaleListenerStatus.LISTENING)
        coEvery { findScale() } answers { found }
        every { setUp(any(), any(), any()) } answers { store.setUp(firstArg<CompanionDevice>().address, secondArg(), thirdArg()) }
    }
    private var scanGranted = true
    private val osPermissions = mockk<OsPermissionsService> {
        every { scaleSetupCatalog() } answers {
            OsPermissionCatalog(listOf(OsPermissionRow(OsPermissionId.BLUETOOTH, listOf("scan"), granted = scanGranted)))
        }
    }

    private fun viewModel() = ScalesViewModel(
        store = store,
        listener = listener,
        weighIns = FakeScaleWeighInRepository(),
        osPermissionsService = osPermissions,
    )

    @Test
    fun `a missing grant puts the checklist before the add dialog`() = runTest {
        scanGranted = false
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }

        viewModel.startAdd()
        assertTrue(viewModel.uiState.value.showPermissionsGate)
        assertFalse(viewModel.uiState.value.showAddFlow)

        viewModel.openAddFlow()
        assertFalse(viewModel.uiState.value.showPermissionsGate)
        assertTrue(viewModel.uiState.value.showAddFlow)
    }

    @Test
    fun `a scale Android did not find leaves the first step with its complaint`() = runTest {
        found = null
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        viewModel.startAdd()

        viewModel.findScale()

        val state = viewModel.uiState.value
        assertEquals(AddScaleStep.FIND, state.addStep)
        assertTrue(state.findFailed)
        assertFalse(state.isFinding)
    }

    @Test
    fun `a found scale opens the key step under its advertised name, and a bad key is refused`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        viewModel.startAdd()

        viewModel.findScale()
        assertEquals(AddScaleStep.KEY, viewModel.uiState.value.addStep)
        assertEquals("Xiaomi Scale S400 BEEF", viewModel.uiState.value.nameInput)

        viewModel.onKeyInputChange("not a key")
        viewModel.saveScale()

        assertTrue(viewModel.uiState.value.keyInputInvalid)
        assertTrue(viewModel.uiState.value.showAddFlow)
        verify(exactly = 0) { listener.setUp(any(), any(), any()) }
    }

    @Test
    fun `saving adds the scale under the name given and closes the dialog`() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        viewModel.startAdd()
        viewModel.findScale()

        viewModel.onNameInputChange("  Bathroom ")
        viewModel.onKeyInputChange("0728974d657a4b60964c1b1677f35f7c")
        viewModel.saveScale()

        val state = viewModel.uiState.value
        verify(exactly = 1) { listener.setUp(scale, "Bathroom", any()) }
        assertFalse(state.showAddFlow)
        assertEquals("Bathroom", state.scale?.name)
        // The key does not linger in screen state.
        assertEquals("", state.keyInput)
        assertNull(state.found)
    }
}
