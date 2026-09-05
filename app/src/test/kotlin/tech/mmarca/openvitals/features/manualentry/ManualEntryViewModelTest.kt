package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import io.mockk.coEvery
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
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.VitalsRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `manual entry uses default widget order when preferences are empty`() = runTest {
        val vm = viewModel()

        assertEquals(DefaultManualEntryWidgetIds, vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget order loads from preferences`() = runTest {
        val vm = viewModel(
            preferencesRepository = prefs(
                storedWidgetOrder = listOf(ManualEntryWidgetId.HYDRATION.name),
            ),
        )

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget edit toggles`() = runTest {
        val vm = viewModel()

        vm.toggleWidgetEdit()

        assertTrue(vm.uiState.value.isEditingWidgets)
    }

    @Test fun `removing manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs()
        val vm = viewModel(preferencesRepository = preferencesRepository)

        vm.removeWidget(ManualEntryWidgetId.HYDRATION)

        val expected = DefaultManualEntryWidgetIds - ManualEntryWidgetId.HYDRATION
        assertEquals(expected, vm.uiState.value.widgets)
        verify { preferencesRepository.setManualEntryWidgetOrder(expected.map { it.name }) }
    }

    @Test fun `adding manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs(storedWidgetOrder = emptyList())
        val vm = viewModel(preferencesRepository = preferencesRepository)

        vm.addWidget(ManualEntryWidgetId.HYDRATION)

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
        verify { preferencesRepository.setManualEntryWidgetOrder(listOf(ManualEntryWidgetId.HYDRATION.name)) }
    }

    // ── hydration ───────────────────────────────────────────────────────────

    @Test fun `hydration tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(hydrationRepository = hydrationRepo(canWrite = true))

        vm.onHydrationWidgetTapped()

        assertFalse(vm.uiState.value.pendingHydrationWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `hydration tap requests the hydration write permission when missing`() = runTest {
        val vm = viewModel(hydrationRepository = hydrationRepo(canWrite = false))

        vm.onHydrationWidgetTapped()

        assertTrue(vm.uiState.value.pendingHydrationWritePermissionRequest)
        assertEquals(setOf(WriteHydrationPermission), vm.uiState.value.hydrationWritePermissions)
        assertFalse(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `launching the hydration request clears the trigger without navigating`() = runTest {
        val vm = viewModel(hydrationRepository = hydrationRepo(canWrite = false))

        vm.onHydrationWidgetTapped()
        vm.onHydrationWritePermissionRequestLaunched()

        assertFalse(vm.uiState.value.pendingHydrationWritePermissionRequest)
        assertFalse(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `a denied hydration request still opens the entry form`() = runTest {
        val vm = viewModel(hydrationRepository = hydrationRepo(canWrite = false))

        vm.onHydrationWidgetTapped()
        vm.onHydrationWritePermissionRequestLaunched()
        vm.onHydrationWritePermissionResult()

        assertFalse(vm.uiState.value.canWriteHydration)
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `hydration asks again on every tap while the permission is missing`() = runTest {
        val vm = viewModel(hydrationRepository = hydrationRepo(canWrite = false))

        vm.onHydrationWidgetTapped()
        vm.onHydrationWritePermissionRequestLaunched()
        vm.onHydrationWritePermissionResult()
        vm.onHydrationEntryNavigationHandled()
        vm.onHydrationWidgetTapped()

        assertTrue(vm.uiState.value.pendingHydrationWritePermissionRequest)
        assertFalse(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    // ── carbs ───────────────────────────────────────────────────────────────

    @Test fun `carbs tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(nutritionRepository = nutritionRepo(canWrite = true))

        vm.onCarbsWidgetTapped()

        assertFalse(vm.uiState.value.pendingCarbsWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingCarbsEntryNavigation)
    }

    @Test fun `carbs tap requests the nutrition write permission when missing`() = runTest {
        val vm = viewModel(nutritionRepository = nutritionRepo(canWrite = false))

        vm.onCarbsWidgetTapped()

        assertTrue(vm.uiState.value.pendingCarbsWritePermissionRequest)
        assertEquals(setOf(WriteNutritionPermission), vm.uiState.value.nutritionWritePermissions)
        assertFalse(vm.uiState.value.pendingCarbsEntryNavigation)
    }

    @Test fun `a carbs permission result opens the entry form`() = runTest {
        val vm = viewModel(nutritionRepository = nutritionRepo(canWrite = false))

        vm.onCarbsWidgetTapped()
        vm.onCarbsWritePermissionRequestLaunched()
        vm.onNutritionWritePermissionResult()

        assertFalse(vm.uiState.value.pendingCarbsWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingCarbsEntryNavigation)
    }

    // ── activity ────────────────────────────────────────────────────────────

    @Test fun `activity tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(activityRepository = activityRepo(canWrite = true))

        vm.onActivityWidgetTapped()

        assertFalse(vm.uiState.value.pendingActivityWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingActivityEntryNavigation)
    }

    @Test fun `activity tap requests the activity write permissions when missing`() = runTest {
        val vm = viewModel(activityRepository = activityRepo(canWrite = false))

        vm.onActivityWidgetTapped()

        assertTrue(vm.uiState.value.pendingActivityWritePermissionRequest)
        assertEquals(ActivityWritePermissions, vm.uiState.value.activityWritePermissions)
        assertFalse(vm.uiState.value.pendingActivityEntryNavigation)
    }

    @Test fun `an activity permission result opens the entry form`() = runTest {
        val vm = viewModel(activityRepository = activityRepo(canWrite = false))

        vm.onActivityWidgetTapped()
        vm.onActivityWritePermissionRequestLaunched()
        vm.onActivityWritePermissionResult()

        assertFalse(vm.uiState.value.pendingActivityWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingActivityEntryNavigation)
    }

    // ── body ────────────────────────────────────────────────────────────────

    @Test fun `body measurement tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(bodyRepository = bodyRepo(canWrite = true))

        vm.onBodyMeasurementWidgetTapped(BodyMeasurementType.WEIGHT)

        assertNull(vm.uiState.value.pendingBodyWritePermissionRequest)
        assertEquals(BodyMeasurementType.WEIGHT, vm.uiState.value.pendingBodyEntryNavigation)
    }

    @Test fun `body measurement tap requests the write permission for that type when missing`() = runTest {
        val vm = viewModel(bodyRepository = bodyRepo(canWrite = false))

        vm.onBodyMeasurementWidgetTapped(BodyMeasurementType.WEIGHT)

        assertEquals(BodyMeasurementType.WEIGHT, vm.uiState.value.pendingBodyWritePermissionRequest)
        assertEquals(BodyMeasurementType.WEIGHT, vm.uiState.value.bodyWritePermissionRequestType)
        assertEquals(setOf(WriteWeightPermission), vm.uiState.value.bodyWritePermissions)
        assertNull(vm.uiState.value.pendingBodyEntryNavigation)
    }

    @Test fun `a body permission result opens the entry for the requested type`() = runTest {
        val vm = viewModel(bodyRepository = bodyRepo(canWrite = false))

        vm.onBodyMeasurementWidgetTapped(BodyMeasurementType.WEIGHT)
        vm.onBodyWritePermissionRequestLaunched()
        vm.onBodyWritePermissionResult()

        assertNull(vm.uiState.value.pendingBodyWritePermissionRequest)
        assertNull(vm.uiState.value.bodyWritePermissionRequestType)
        assertEquals(BodyMeasurementType.WEIGHT, vm.uiState.value.pendingBodyEntryNavigation)
    }

    // ── vitals ──────────────────────────────────────────────────────────────

    @Test fun `vitals measurement tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(vitalsRepository = vitalsRepo(canWrite = true))

        vm.onVitalsMeasurementWidgetTapped(VitalsMeasurementType.BLOOD_PRESSURE)

        assertNull(vm.uiState.value.pendingVitalsWritePermissionRequest)
        assertEquals(VitalsMeasurementType.BLOOD_PRESSURE, vm.uiState.value.pendingVitalsEntryNavigation)
    }

    @Test fun `vitals measurement tap requests the write permission for that type when missing`() = runTest {
        val vm = viewModel(vitalsRepository = vitalsRepo(canWrite = false))

        vm.onVitalsMeasurementWidgetTapped(VitalsMeasurementType.BLOOD_PRESSURE)

        assertEquals(VitalsMeasurementType.BLOOD_PRESSURE, vm.uiState.value.pendingVitalsWritePermissionRequest)
        assertEquals(VitalsMeasurementType.BLOOD_PRESSURE, vm.uiState.value.vitalsWritePermissionRequestType)
        assertEquals(setOf(WriteBloodPressurePermission), vm.uiState.value.vitalsWritePermissions)
        assertNull(vm.uiState.value.pendingVitalsEntryNavigation)
    }

    @Test fun `a vitals permission result opens the entry for the requested type`() = runTest {
        val vm = viewModel(vitalsRepository = vitalsRepo(canWrite = false))

        vm.onVitalsMeasurementWidgetTapped(VitalsMeasurementType.BLOOD_PRESSURE)
        vm.onVitalsWritePermissionRequestLaunched()
        vm.onVitalsWritePermissionResult()

        assertNull(vm.uiState.value.pendingVitalsWritePermissionRequest)
        assertNull(vm.uiState.value.vitalsWritePermissionRequestType)
        assertEquals(VitalsMeasurementType.BLOOD_PRESSURE, vm.uiState.value.pendingVitalsEntryNavigation)
    }

    // ── mindfulness ─────────────────────────────────────────────────────────

    @Test fun `mindfulness tap opens entry when write permission is granted`() = runTest {
        val vm = viewModel(mindfulnessRepository = mindfulnessRepo(canWrite = true))

        vm.onMindfulnessWidgetTapped()

        assertFalse(vm.uiState.value.pendingMindfulnessWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingMindfulnessEntryNavigation)
    }

    @Test fun `mindfulness tap requests the mindfulness write permission when missing`() = runTest {
        val vm = viewModel(mindfulnessRepository = mindfulnessRepo(canWrite = false))

        vm.onMindfulnessWidgetTapped()

        assertTrue(vm.uiState.value.pendingMindfulnessWritePermissionRequest)
        assertFalse(vm.uiState.value.pendingMindfulnessEntryNavigation)
    }

    @Test fun `mindfulness tap skips the request when provider exposes no write permission`() = runTest {
        val vm = viewModel(
            mindfulnessRepository = mindfulnessRepo(
                canWrite = false,
                writePermissions = emptySet(),
            ),
        )

        vm.onMindfulnessWidgetTapped()

        assertFalse(vm.uiState.value.pendingMindfulnessWritePermissionRequest)
        assertEquals(emptySet<String>(), vm.uiState.value.mindfulnessWritePermissions)
        assertTrue(vm.uiState.value.pendingMindfulnessEntryNavigation)
    }

    @Test fun `a mindfulness permission result opens the entry form`() = runTest {
        val vm = viewModel(mindfulnessRepository = mindfulnessRepo(canWrite = false))

        vm.onMindfulnessWidgetTapped()
        vm.onMindfulnessWritePermissionRequestLaunched()
        vm.onMindfulnessWritePermissionResult()

        assertFalse(vm.uiState.value.pendingMindfulnessWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingMindfulnessEntryNavigation)
    }

    // ── cycle ───────────────────────────────────────────────────────────────

    @Test fun `cycle tap opens entry when any cycle write permission is granted`() = runTest {
        val vm = viewModel(cycleRepository = cycleRepo(canWrite = true))

        vm.onCycleWidgetTapped()

        assertFalse(vm.uiState.value.pendingCycleWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingCycleEntryNavigation)
    }

    @Test fun `cycle tap requests the union of the cycle write permissions when missing`() = runTest {
        val vm = viewModel(cycleRepository = cycleRepo(canWrite = false))

        vm.onCycleWidgetTapped()

        assertTrue(vm.uiState.value.pendingCycleWritePermissionRequest)
        assertEquals(setOf(WriteCyclePermission), vm.uiState.value.cycleWritePermissions)
        assertFalse(vm.uiState.value.pendingCycleEntryNavigation)
    }

    @Test fun `a cycle permission result opens the entry form`() = runTest {
        val vm = viewModel(cycleRepository = cycleRepo(canWrite = false))

        vm.onCycleWidgetTapped()
        vm.onCycleWritePermissionRequestLaunched()
        vm.onCycleWritePermissionResult()

        assertFalse(vm.uiState.value.pendingCycleWritePermissionRequest)
        assertTrue(vm.uiState.value.pendingCycleEntryNavigation)
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    private fun viewModel(
        hydrationRepository: HydrationRepository = hydrationRepo(),
        nutritionRepository: NutritionRepository = nutritionRepo(),
        activityRepository: ActivityRepository = activityRepo(),
        bodyRepository: BodyRepository = bodyRepo(),
        vitalsRepository: VitalsRepository = vitalsRepo(),
        mindfulnessRepository: MindfulnessRepository = mindfulnessRepo(),
        cycleRepository: CycleRepository = cycleRepo(),
        preferencesRepository: PreferencesRepository = prefs(),
    ): ManualEntryViewModel = ManualEntryViewModel(
        hydrationRepository = hydrationRepository,
        nutritionRepository = nutritionRepository,
        activityRepository = activityRepository,
        bodyRepository = bodyRepository,
        vitalsRepository = vitalsRepository,
        mindfulnessRepository = mindfulnessRepository,
        cycleRepository = cycleRepository,
        preferencesRepository = preferencesRepository,
    )

    // Strict mock: any leftover read of the removed "acknowledged permissions"
    // preference fails loudly instead of silently suppressing a request.
    private fun prefs(
        storedWidgetOrder: List<String>? = null,
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.manualEntryWidgetOrder() } returns storedWidgetOrder
            every { prefs.setManualEntryWidgetOrder(any()) } returns Unit
        }

    private fun hydrationRepo(
        canWrite: Boolean = false,
    ): HydrationRepository =
        mockk<HydrationRepository>().also { repo ->
            every { repo.hydrationWritePermissions } returns setOf(WriteHydrationPermission)
            coEvery { repo.hasHydrationWritePermission() } returns canWrite
        }

    private fun nutritionRepo(
        canWrite: Boolean = false,
    ): NutritionRepository =
        mockk<NutritionRepository>().also { repo ->
            every { repo.nutritionWritePermissions } returns setOf(WriteNutritionPermission)
            coEvery { repo.hasNutritionWritePermission() } returns canWrite
        }

    private fun activityRepo(
        canWrite: Boolean = false,
    ): ActivityRepository =
        mockk<ActivityRepository>().also { repo ->
            every { repo.activityWritePermissions() } returns ActivityWritePermissions
            coEvery { repo.hasActivityWritePermission() } returns canWrite
        }

    private fun bodyRepo(
        canWrite: Boolean = false,
    ): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            every { repo.bodyWritePermissions(any()) } returns setOf(WriteWeightPermission)
            coEvery { repo.hasBodyWritePermission(any()) } returns canWrite
        }

    private fun vitalsRepo(
        canWrite: Boolean = false,
    ): VitalsRepository =
        mockk<VitalsRepository>().also { repo ->
            every { repo.vitalsWritePermissions(any()) } returns setOf(WriteBloodPressurePermission)
            coEvery { repo.hasVitalsWritePermission(any()) } returns canWrite
        }

    private fun cycleRepo(
        canWrite: Boolean = false,
        writePermissions: Set<String> = setOf(WriteCyclePermission),
    ): CycleRepository =
        mockk<CycleRepository>().also { repo ->
            every { repo.cycleWritePermissions(any()) } returns writePermissions
            coEvery { repo.hasCycleWritePermission(any()) } returns canWrite
        }

    private fun mindfulnessRepo(
        canWrite: Boolean = false,
        writePermissions: Set<String> = setOf(WriteMindfulnessPermission),
    ): MindfulnessRepository =
        mockk<MindfulnessRepository>().also { repo ->
            every { repo.mindfulnessWritePermissions } returns writePermissions
            coEvery { repo.hasMindfulnessWritePermission() } returns canWrite
        }

    private companion object {
        private const val WriteHydrationPermission = "write_hydration"
        private const val WriteNutritionPermission = "write_nutrition"
        private const val WriteMindfulnessPermission = "write_mindfulness"
        private const val WriteCyclePermission = "write_cycle"
        private const val WriteWeightPermission = "write_weight"
        private const val WriteBloodPressurePermission = "write_blood_pressure"
        private val ActivityWritePermissions = setOf(
            "write_activity",
            "write_route",
            "write_distance",
            "write_elevation",
            "write_active_calories",
            "write_total_calories",
        )
    }
}
