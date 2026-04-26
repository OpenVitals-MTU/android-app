package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.data.repository.HealthRepository
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
    }

    @Test fun `checkState tracks granted permissions and phases separately`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = setOf("steps", "vitals")),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isCheckingPermissions)
        assertEquals(setOf("steps", "vitals"), vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.mindfulnessAvailable)
        assertTrue(vm.uiState.value.phase1Granted)
        assertFalse(vm.uiState.value.phase2Granted)
        assertTrue(vm.uiState.value.phase3Granted)
    }

    @Test fun `checkState marks every phase granted when all permissions are granted`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = allPermissions),
        )
        advanceUntilIdle()

        assertEquals(allPermissions, vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.phase1Granted)
        assertTrue(vm.uiState.value.phase2Granted)
        assertTrue(vm.uiState.value.phase3Granted)
    }

    @Test fun `checkState handles unsupported Health Connect without reading permissions`() = runTest {
        val repository = repo(
            availability = HealthConnectAvailability.NOT_SUPPORTED,
            grantedPermissions = allPermissions,
        )

        val vm = OnboardingViewModel(repository)
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

        val vm = OnboardingViewModel(repository)
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
        val vm = OnboardingViewModel(repository)
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
        val vm = OnboardingViewModel(repository)
        advanceUntilIdle()

        vm.onPermissionsResult(emptySet())
        advanceUntilIdle()

        assertEquals(setOf("steps"), vm.uiState.value.grantedPermissions)
        assertTrue(vm.uiState.value.phase1Granted)
    }

    @Test fun `onboardingPermissions exposes the full selector request set`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
        )
        advanceUntilIdle()

        assertEquals(
            setOf("steps", "heart", "body", "activity", "nutrition", "mindfulness", "vitals"),
            vm.onboardingPermissions,
        )
    }

    @Test fun `permissionCategories exposes user-facing permission groups`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet()),
        )
        advanceUntilIdle()

        val categories = vm.permissionCategories
        assertEquals(
            listOf(
                "Activity & sleep",
                "Heart & recovery",
                "Body",
                "Activity extras",
                "Nutrition & hydration",
                "Mindfulness",
                "Vitals",
            ),
            categories.map { it.title },
        )
        assertTrue(categories.first().required)
        assertEquals("activity_sleep", categories.first().id)
        assertEquals(setOf("steps"), categories.first().permissions)
        assertFalse(categories.drop(1).any { it.required })
        assertTrue(categories.all { it.description.isNotBlank() })
    }

    @Test fun `permissionCategories filters empty optional permission groups`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(
                grantedPermissions = emptySet(),
                bodyPermissions = emptySet(),
                mindfulnessPermissions = emptySet(),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                "activity_sleep",
                "heart_recovery",
                "activity_extras",
                "nutrition_hydration",
                "vitals",
            ),
            vm.permissionCategories.map { it.id },
        )
    }

    @Test fun `mindfulness category is marked unavailable when Health Connect lacks the feature`() = runTest {
        val repository = repo(
            grantedPermissions = emptySet(),
            mindfulnessAvailable = false,
            phase2Permissions = setOf("heart", "body", "activity", "nutrition"),
            onboardingPermissions = setOf("steps", "heart", "body", "activity", "nutrition", "vitals"),
        )
        val vm = OnboardingViewModel(repository)
        advanceUntilIdle()

        val mindfulness = vm.permissionCategories.single { it.id == "mindfulness" }
        assertFalse(mindfulness.available)
        assertEquals("Mindfulness sessions require a newer Health Connect version.", mindfulness.unavailableReason)
        assertEquals(setOf("mindfulness"), mindfulness.permissions)
        assertFalse(vm.uiState.value.phase2Granted)
        assertFalse("mindfulness" in vm.onboardingPermissions)
    }

    @Test fun `mindfulness category is grantable when Health Connect supports the feature`() = runTest {
        val vm = OnboardingViewModel(
            repository = repo(grantedPermissions = emptySet(), mindfulnessAvailable = true),
        )
        advanceUntilIdle()

        val mindfulness = vm.permissionCategories.single { it.id == "mindfulness" }
        assertTrue(mindfulness.available)
        assertEquals(setOf("mindfulness"), mindfulness.permissions)
        assertTrue("mindfulness" in vm.onboardingPermissions)
    }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String>,
        mindfulnessAvailable: Boolean = true,
        phase2Permissions: Set<String> = setOf("heart", "body", "activity", "nutrition", "mindfulness"),
        bodyPermissions: Set<String> = setOf("body"),
        mindfulnessPermissions: Set<String> = setOf("mindfulness"),
        onboardingPermissions: Set<String> = allPermissions,
    ): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns availability
            every { repo.phase1Permissions } returns setOf("steps")
            every { repo.phase2Permissions } returns phase2Permissions
            every { repo.phase3Permissions } returns setOf("vitals")
            every { repo.corePermissions } returns setOf("steps")
            every { repo.heartPermissions } returns setOf("heart")
            every { repo.bodyPermissions } returns bodyPermissions
            every { repo.activityExtrasPermissions } returns setOf("activity")
            every { repo.nutritionHydrationPermissions } returns setOf("nutrition")
            every { repo.mindfulnessPermissions } returns mindfulnessPermissions
            every { repo.vitalsPermissions } returns setOf("vitals")
            every { repo.isMindfulnessAvailable() } returns mindfulnessAvailable
            every { repo.onboardingPermissions } returns onboardingPermissions
            coEvery { repo.grantedPermissions() } returns grantedPermissions
        }

    companion object {
        private val allPermissions = setOf(
            "steps",
            "heart",
            "body",
            "activity",
            "nutrition",
            "mindfulness",
            "vitals",
        )
    }
}
