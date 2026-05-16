package tech.mmarca.openvitals.features.settings

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `refresh keeps cycle permissions hidden when cycle tracking is off`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs(trackCycle = false),
        )

        assertEquals(setOf("steps", "route"), vm.uiState.value.visiblePermissions)
        assertFalse(vm.uiState.value.trackCycle)
    }

    @Test fun `refresh includes cycle permissions when cycle tracking is on`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(setOf("steps", "route", "cycle"), vm.uiState.value.visiblePermissions)
        assertTrue(vm.uiState.value.trackCycle)
    }

    @Test fun `missingVisiblePermissions excludes already granted visible permissions`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(grantedPermissions = setOf("steps")),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(setOf("route", "cycle"), vm.uiState.value.missingVisiblePermissions)
        assertEquals(setOf("cycle"), vm.uiState.value.missingRequestableVisiblePermissions)
        assertEquals(setOf("route"), vm.uiState.value.missingManualVisiblePermissions)
    }

    @Test fun `missingVisiblePermissions is empty when all visible permissions are granted`() = runTest {
        val vm = SettingsViewModel(
            repository = repo(grantedPermissions = setOf("steps", "route", "cycle")),
            preferencesRepository = prefs(trackCycle = true),
        )

        assertTrue(vm.uiState.value.missingVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingRequestableVisiblePermissions.isEmpty())
        assertTrue(vm.uiState.value.missingManualVisiblePermissions.isEmpty())
    }

    @Test fun `setTrackCycle persists preference and updates visible permissions`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.setTrackCycle(true)

        verify { prefs.trackCycle = true }
        assertTrue(vm.uiState.value.trackCycle)
        assertEquals(setOf("steps", "route", "cycle"), vm.uiState.value.visiblePermissions)
    }

    @Test fun `selectAppLanguage persists preference and updates ui state`() = runTest {
        val prefs = prefs(trackCycle = false)
        val vm = SettingsViewModel(
            repository = repo(),
            preferencesRepository = prefs,
        )

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `refresh skips granted permissions when Health Connect is unsupported`() = runTest {
        val repository = repo(availability = HealthConnectAvailability.NOT_SUPPORTED)

        val vm = SettingsViewModel(
            repository = repository,
            preferencesRepository = prefs(trackCycle = true),
        )

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String> = emptySet(),
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.corePermissions } returns setOf("steps")
            every { repo.routePermissions } returns setOf("route")
            every { repo.heartPermissions } returns emptySet()
            every { repo.bodyPermissions } returns emptySet()
            every { repo.activityExtrasPermissions } returns emptySet()
            every { repo.nutritionHydrationPermissions } returns emptySet()
            every { repo.mindfulnessPermissions } returns emptySet()
            every { repo.additionalDataAccessPermissions } returns emptySet()
            every { repo.vitalsPermissions } returns emptySet()
            every { repo.isMindfulnessAvailable() } returns false
            every { repo.allPermissions } returns setOf("steps", "route")
            every { repo.cyclePermissions } returns setOf("cycle")
            every { repo.manualOnlyPermissions } returns setOf("route")
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

    private fun prefs(trackCycle: Boolean): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.trackCycle } returns trackCycle
            every { prefs.appLanguage = any() } just runs
            every { prefs.trackCycle = any() } just runs
        }
}
