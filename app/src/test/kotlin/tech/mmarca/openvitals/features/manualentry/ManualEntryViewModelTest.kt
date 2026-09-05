package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `manual entry uses default widget order when preferences are empty`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        assertEquals(DefaultManualEntryWidgetIds, vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget order loads from preferences`() = runTest {
        val vm = ManualEntryViewModel(prefs(storedWidgetOrder = listOf(ManualEntryWidgetId.HYDRATION.name)))

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget edit toggles`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        vm.toggleWidgetEdit()

        assertTrue(vm.uiState.value.isEditingWidgets)
    }

    @Test fun `removing manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs()
        val vm = ManualEntryViewModel(preferencesRepository)

        vm.removeWidget(ManualEntryWidgetId.HYDRATION)

        val expected = DefaultManualEntryWidgetIds - ManualEntryWidgetId.HYDRATION
        assertEquals(expected, vm.uiState.value.widgets)
        verify { preferencesRepository.setManualEntryWidgetOrder(expected.map { it.name }) }
    }

    @Test fun `adding manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs(storedWidgetOrder = emptyList())
        val vm = ManualEntryViewModel(preferencesRepository)

        vm.addWidget(ManualEntryWidgetId.HYDRATION)

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
        verify { preferencesRepository.setManualEntryWidgetOrder(listOf(ManualEntryWidgetId.HYDRATION.name)) }
    }

    // A tile opens its screen without consulting Health Connect: the screen's
    // Grant button is the one place that asks, so the strict preferences mock
    // (no permission stubs) and the absence of any repository here are the
    // guard against a request creeping back into the grid.

    @Test fun `hydration tap opens the entry screen`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        vm.onHydrationWidgetTapped()
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)

        vm.onHydrationEntryNavigationHandled()
        assertFalse(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `carbs, activity, mindfulness and cycle taps open their screens`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        vm.onCarbsWidgetTapped()
        vm.onActivityWidgetTapped()
        vm.onMindfulnessWidgetTapped()
        vm.onCycleWidgetTapped()

        assertTrue(vm.uiState.value.pendingCarbsEntryNavigation)
        assertTrue(vm.uiState.value.pendingActivityEntryNavigation)
        assertTrue(vm.uiState.value.pendingMindfulnessEntryNavigation)
        assertTrue(vm.uiState.value.pendingCycleEntryNavigation)
    }

    @Test fun `body measurement tap opens the entry for that type`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        vm.onBodyMeasurementWidgetTapped(BodyMeasurementType.WEIGHT)
        assertEquals(BodyMeasurementType.WEIGHT, vm.uiState.value.pendingBodyEntryNavigation)

        vm.onBodyEntryNavigationHandled()
        assertNull(vm.uiState.value.pendingBodyEntryNavigation)
    }

    @Test fun `vitals measurement tap opens the entry for that type`() = runTest {
        val vm = ManualEntryViewModel(prefs())

        vm.onVitalsMeasurementWidgetTapped(VitalsMeasurementType.BLOOD_PRESSURE)
        assertEquals(VitalsMeasurementType.BLOOD_PRESSURE, vm.uiState.value.pendingVitalsEntryNavigation)

        vm.onVitalsEntryNavigationHandled()
        assertNull(vm.uiState.value.pendingVitalsEntryNavigation)
    }

    private fun prefs(
        storedWidgetOrder: List<String>? = null,
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.manualEntryWidgetOrder() } returns storedWidgetOrder
            every { prefs.setManualEntryWidgetOrder(any()) } returns Unit
        }
}
