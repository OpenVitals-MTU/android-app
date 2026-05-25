package tech.mmarca.openvitals.features.manualentry

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `manual entry uses default widget order when preferences are empty`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(),
            preferencesRepository = prefs(),
        )

        assertEquals(DefaultManualEntryWidgetIds, vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget order loads from preferences`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(),
            preferencesRepository = prefs(
                storedWidgetOrder = listOf(ManualEntryWidgetId.HYDRATION.name),
            ),
        )

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
    }

    @Test fun `manual entry widget edit toggles`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(),
            preferencesRepository = prefs(),
        )

        vm.toggleWidgetEdit()

        assertTrue(vm.uiState.value.isEditingWidgets)
    }

    @Test fun `removing manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs()
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(),
            preferencesRepository = preferencesRepository,
        )

        vm.removeWidget(ManualEntryWidgetId.HYDRATION)

        assertTrue(vm.uiState.value.widgets.isEmpty())
        verify { preferencesRepository.setManualEntryWidgetOrder(emptyList()) }
    }

    @Test fun `adding manual entry widget persists order`() = runTest {
        val preferencesRepository = prefs(storedWidgetOrder = emptyList())
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(),
            preferencesRepository = preferencesRepository,
        )

        vm.addWidget(ManualEntryWidgetId.HYDRATION)

        assertEquals(listOf(ManualEntryWidgetId.HYDRATION), vm.uiState.value.widgets)
        verify { preferencesRepository.setManualEntryWidgetOrder(listOf(ManualEntryWidgetId.HYDRATION.name)) }
    }

    @Test fun `hydration tap opens entry when write permission is granted`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(canWrite = true),
            preferencesRepository = prefs(),
        )

        vm.onHydrationWidgetTapped()

        assertFalse(vm.uiState.value.showHydrationWritePermissionPrompt)
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `hydration tap shows one time write permission prompt when missing and unacknowledged`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(canWrite = false),
            preferencesRepository = prefs(acknowledgedPermissions = emptySet()),
        )

        vm.onHydrationWidgetTapped()

        assertTrue(vm.uiState.value.showHydrationWritePermissionPrompt)
        assertFalse(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `hydration tap skips prompt when write permission was already acknowledged`() = runTest {
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(canWrite = false),
            preferencesRepository = prefs(acknowledgedPermissions = setOf(WriteHydrationPermission)),
        )

        vm.onHydrationWidgetTapped()

        assertFalse(vm.uiState.value.showHydrationWritePermissionPrompt)
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)
    }

    @Test fun `opening entry from prompt acknowledges write permission`() = runTest {
        val preferencesRepository = prefs()
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(canWrite = false),
            preferencesRepository = preferencesRepository,
        )

        vm.onHydrationWidgetTapped()
        vm.continueHydrationEntryFromWritePermissionPrompt()

        assertFalse(vm.uiState.value.showHydrationWritePermissionPrompt)
        assertTrue(vm.uiState.value.pendingHydrationEntryNavigation)
        verify { preferencesRepository.acknowledgePermissions(setOf(WriteHydrationPermission)) }
    }

    @Test fun `granting from prompt acknowledges write permission before request`() = runTest {
        val preferencesRepository = prefs()
        val vm = ManualEntryViewModel(
            hydrationRepository = hydrationRepo(canWrite = false),
            preferencesRepository = preferencesRepository,
        )

        vm.onHydrationWidgetTapped()
        vm.grantHydrationWritePermissionFromPrompt()

        assertFalse(vm.uiState.value.showHydrationWritePermissionPrompt)
        verify { preferencesRepository.acknowledgePermissions(setOf(WriteHydrationPermission)) }
    }

    private fun prefs(
        storedWidgetOrder: List<String>? = null,
        acknowledgedPermissions: Set<String> = emptySet(),
    ): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.manualEntryWidgetOrder() } returns storedWidgetOrder
            every { prefs.setManualEntryWidgetOrder(any()) } returns Unit
            every { prefs.acknowledgedPermissions() } returns acknowledgedPermissions
            every { prefs.acknowledgePermissions(any()) } returns Unit
        }

    private fun hydrationRepo(
        canWrite: Boolean = false,
    ): HydrationRepository =
        mockk<HydrationRepository>().also { repo ->
            every { repo.hydrationWritePermissions } returns setOf(WriteHydrationPermission)
            coEvery { repo.hasHydrationWritePermission() } returns canWrite
        }

    private companion object {
        private const val WriteHydrationPermission = "write_hydration"
    }
}
