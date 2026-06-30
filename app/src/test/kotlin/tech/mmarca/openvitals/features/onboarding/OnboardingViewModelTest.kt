package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PermissionGrantMode
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

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

    @Test fun `initial state is loading`() {
        val state = OnboardingUiState()

        assertTrue(state.isCheckingPermissions)
        assertEquals(HealthConnectAvailability.AVAILABLE, state.availability)
        assertTrue(state.grantedPermissions.isEmpty())
        assertFalse(state.phase1Granted)
        assertFalse(state.phase2Granted)
        assertFalse(state.phase3Granted)
        assertFalse(state.phase4Granted)
        assertEquals(AppLanguage.SYSTEM, state.appLanguage)
    }

    @Test fun `checkState tracks granted permissions and phases separately`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = setOf("steps", "vitals")),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermissions)
        assertEquals(setOf("steps", "vitals"), vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.mindfulnessAvailable)
        assertTrue(vm.uiState.value.phase1Granted)
        assertFalse(vm.uiState.value.phase2Granted)
        assertTrue(vm.uiState.value.phase3Granted)
        assertFalse(vm.uiState.value.phase4Granted)
    }

    @Test fun `checkState marks every phase granted when all permissions are granted`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = allPermissions),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertEquals(allPermissions, vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.phase1Granted)
        assertTrue(vm.uiState.value.phase2Granted)
        assertTrue(vm.uiState.value.phase3Granted)
        assertTrue(vm.uiState.value.phase4Granted)
    }

    @Test fun `checkState handles unsupported Health Connect without reading permissions`() = runTest {
        val repository = repo(
            availability = HealthConnectAvailability.NOT_SUPPORTED,
            grantedPermissions = allPermissions,
        )

        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertFalse(vm.uiState.value.isCheckingPermissions)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        verify(exactly = 0) { repository.isMindfulnessAvailable() }
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    @Test fun `checkState handles Health Connect update requirement without reading permissions`() = runTest {
        val repository = repo(
            availability = HealthConnectAvailability.NEEDS_PROVIDER_UPDATE,
            grantedPermissions = allPermissions,
        )

        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        assertEquals(HealthConnectAvailability.NEEDS_PROVIDER_UPDATE, vm.uiState.value.availability)
        assertFalse(vm.uiState.value.isCheckingPermissions)
        assertTrue(vm.uiState.value.grantedPermissions.isEmpty())
        verify(exactly = 0) { repository.isMindfulnessAvailable() }
        coVerify(exactly = 0) { repository.grantedPermissions() }
    }

    @Test fun `onPermissionsResult refreshes phase 3 permission state`() = runTest {
        val repository = repo(
            grantedPermissions = setOf(
                "steps",
                "heart",
                "body",
                "activity",
                "nutrition",
                "mindfulness",
                "vitals",
            ),
        )
        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        vm.onPermissionsResult(setOf("vitals"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.phase1Granted)
        assertTrue(vm.uiState.value.phase2Granted)
        assertTrue(vm.uiState.value.phase3Granted)
    }

    @Test fun `onPermissionsResult re-queries granted permissions instead of trusting callback`() = runTest {
        val repository = repo(grantedPermissions = emptySet())
        coEvery { repository.grantedPermissions() } returnsMany listOf(emptySet(), setOf("steps"))
        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        vm.onPermissionsResult(emptySet())
        advanceUntilIdle()

        assertEquals(setOf("steps"), vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.phase1Granted)
    }

    @Test fun `onboardingPermissions exposes one tap onboarding request set`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertEquals(
            setOf(
                "steps",
                "heart",
                "body",
                "activity",
                "nutrition",
                "mindfulness",
                "history",
                "background",
                "vitals",
                "cycle",
                "write",
                "import_write",
            ),
            vm.onboardingPermissions,
        )
    }

    @Test fun `permissionCategories exposes user-facing permission groups`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        val categories = vm.permissionCategories
        assertEquals(
            listOf(
                R.string.onboarding_category_activity_sleep,
                R.string.onboarding_category_heart_recovery,
                R.string.onboarding_category_vitals,
                R.string.onboarding_category_body,
                R.string.onboarding_category_activity_extras,
                R.string.onboarding_category_nutrition_hydration,
                R.string.onboarding_category_manual_entry_write,
                R.string.onboarding_category_data_import_write,
                R.string.onboarding_category_mindfulness,
                R.string.onboarding_category_additional_data_access,
                R.string.onboarding_category_cycle_tracking,
            ),
            categories.map { it.titleRes },
        )
        assertTrue(categories.first().required)
        assertEquals("activity_sleep", categories.first().id)
        assertEquals(setOf("steps"), categories.first().permissions)
        assertEquals(
            setOf("history", "background", "route"),
            categories.single { it.id == "additional_data_access" }.permissions,
        )
        assertEquals(setOf("write"), categories.single { it.id == "manual_entry_write" }.permissions)
        assertEquals(setOf("import_write"), categories.single { it.id == "data_import_write" }.permissions)
        assertEquals(setOf("route"), categories.single { it.id == "additional_data_access" }.manualPermissions)
        assertEquals(
            setOf("activity_sleep", "heart_recovery", "vitals"),
            categories.filter { it.required }.map { it.id }.toSet(),
        )
        assertEquals("cycle_tracking", categories.last().id)
        assertTrue(categories.all { it.descriptionRes != 0 })
    }

    @Test fun `permissionCategories filters empty permission groups`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(
                grantedPermissions = emptySet(),
                bodyPermissions = emptySet(),
                mindfulnessPermissions = emptySet(),
            ),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                "activity_sleep",
                "heart_recovery",
                "vitals",
                "activity_extras",
                "nutrition_hydration",
                "manual_entry_write",
                "data_import_write",
                "additional_data_access",
                "cycle_tracking",
            ),
            vm.permissionCategories.map { it.id },
        )
    }

    @Test fun `mindfulness category is marked unavailable when Health Connect lacks the feature`() = runTest {
        val repository = repo(
            grantedPermissions = emptySet(),
            mindfulnessAvailable = false,
            phase2Permissions = setOf("heart", "body", "activity", "nutrition"),
            onboardingPermissions = setOf("steps", "heart", "body", "activity", "nutrition", "vitals", "cycle"),
        )
        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        val mindfulness = vm.permissionCategories.single { it.id == "mindfulness" }
        assertFalse(mindfulness.available)
        assertFalse(mindfulness.required)
        assertEquals(R.string.onboarding_category_mindfulness_unavailable, mindfulness.unavailableReasonRes)
        assertEquals(setOf("mindfulness"), mindfulness.permissions)
        assertFalse(vm.uiState.value.phase2Granted)
        assertFalse("mindfulness" in vm.onboardingPermissions)
    }

    @Test fun `mindfulness category is grantable when Health Connect supports the feature`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet(), mindfulnessAvailable = true),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        val mindfulness = vm.permissionCategories.single { it.id == "mindfulness" }
        assertTrue(mindfulness.available)
        assertEquals(setOf("mindfulness"), mindfulness.permissions)
        assertTrue("mindfulness" in vm.onboardingPermissions)
    }

    @Test fun `cycle category is included in grant all request set`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        val cycle = vm.permissionCategories.single { it.id == "cycle_tracking" }
        assertEquals(setOf("cycle"), cycle.permissions)
        assertTrue("cycle" in vm.onboardingPermissions)
    }

    @Test fun `data import write category is included in grant all request set`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        val dataImport = vm.permissionCategories.single { it.id == "data_import_write" }
        assertEquals(setOf("import_write"), dataImport.permissions)
        assertTrue("import_write" in vm.onboardingPermissions)
    }

    @Test fun `route permission is grouped with additional data access and excluded from grant all request set`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        val additionalDataAccess = vm.permissionCategories.single { it.id == "additional_data_access" }
        assertEquals(setOf("history", "background", "route"), additionalDataAccess.permissions)
        assertEquals(setOf("route"), additionalDataAccess.manualPermissions)
        assertFalse("route" in vm.onboardingPermissions)
    }

    @Test fun `cycle category is hidden when no cycle permissions are available`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(
                grantedPermissions = emptySet(),
                cyclePermissions = emptySet(),
            ),
            preferencesRepository = prefs(),
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        assertFalse(vm.permissionCategories.any { it.id == "cycle_tracking" })
    }

    @Test fun `onPermissionsResult refreshes phase 4 permission state`() = runTest {
        val repository = repo(grantedPermissions = emptySet())
        coEvery { repository.grantedPermissions() } returnsMany listOf(emptySet(), setOf("cycle"))
        val vm = OnboardingViewModel(repository, prefs(), permissionUxState())
        advanceUntilIdle()

        vm.onPermissionsResult(setOf("cycle"))
        advanceUntilIdle()

        assertEquals(setOf("cycle"), vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.phase4Granted)
    }

    @Test fun `selectAppLanguage persists preference and updates ui state`() = runTest {
        val prefs = prefs(appLanguage = AppLanguage.SYSTEM)
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
            preferencesRepository = prefs,
            permissionUxState = permissionUxState(),
        )
        advanceUntilIdle()

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `androidx mindfulness permission matches platform permission`() {
        assertEquals(
            "android.permission.health.READ_MINDFULNESS",
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
        )
    }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String>,
        mindfulnessAvailable: Boolean = true,
        phase2Permissions: Set<String> = setOf("heart", "body", "activity", "nutrition", "mindfulness"),
        bodyPermissions: Set<String> = setOf("body"),
        routePermissions: Set<String> = setOf("route"),
        manualOnlyPermissions: Set<String> = routePermissions,
        mindfulnessPermissions: Set<String> = setOf("mindfulness"),
        additionalDataAccessPermissions: Set<String> = setOf("history", "background"),
        requestableWritePermissions: Set<String> = setOf("write"),
        dataImportWritePermissions: Set<String> = setOf("import_write"),
        cyclePermissions: Set<String> = setOf("cycle"),
        onboardingPermissions: Set<String> = standardPermissions,
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.phase1Permissions } returns setOf("steps")
            every { repo.minimumOnboardingPermissions } returns setOf("steps", "heart")
            every { repo.phase2Permissions } returns phase2Permissions
            every { repo.phase3Permissions } returns setOf("vitals")
            every { repo.phase4Permissions } returns cyclePermissions
            every { repo.corePermissions } returns setOf("steps")
            every { repo.routePermissions } returns routePermissions
            every { repo.manualOnlyPermissions } returns manualOnlyPermissions
            every { repo.grantModeFor(any()) } answers {
                if (firstArg<String>() in manualOnlyPermissions) {
                    PermissionGrantMode.MANUAL
                } else {
                    PermissionGrantMode.REQUESTABLE
                }
            }
            every { repo.heartPermissions } returns setOf("heart")
            every { repo.bodyPermissions } returns bodyPermissions
            every { repo.activityExtrasPermissions } returns setOf("activity")
            every { repo.nutritionHydrationPermissions } returns setOf("nutrition")
            every { repo.requestableWritePermissions } returns requestableWritePermissions
            every { repo.dataImportWritePermissions } returns dataImportWritePermissions
            every { repo.mindfulnessPermissions } returns mindfulnessPermissions
            every { repo.additionalDataAccessPermissions } returns additionalDataAccessPermissions
            every { repo.vitalsPermissions } returns setOf("vitals")
            every { repo.cyclePermissions } returns cyclePermissions
            every { repo.isMindfulnessAvailable() } returns mindfulnessAvailable
            every { repo.onboardingPermissions } returns onboardingPermissions
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

    private fun permissionUxState(): HealthConnectPermissionUxState =
        mockk<HealthConnectPermissionUxState>(relaxed = true)

    private fun prefs(
        appLanguage: AppLanguage = AppLanguage.SYSTEM,
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.appLanguage } returns appLanguage
            every { prefs.appLanguage = any() } just runs
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.setBodyEnergyCalibration(any()) } just runs
            every { prefs.acceptedPrivacyPolicyVersion = any() } just runs
            every { prefs.privacyPolicyAcceptedAtMillis = any() } just runs
            every { prefs.onboardingDone = any() } just runs
        }

    companion object {
        private val standardPermissions = setOf(
            "steps",
            "heart",
            "body",
            "activity",
            "nutrition",
            "mindfulness",
            "history",
            "background",
            "vitals",
            "cycle",
            "write",
            "import_write",
        )
        private val allPermissions = standardPermissions
    }
}
