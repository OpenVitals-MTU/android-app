package tech.mmarca.openvitals.features.onboarding

import android.util.Log
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.OnboardingCategoryId
import tech.mmarca.openvitals.domain.model.OnboardingPermissionCatalog
import tech.mmarca.openvitals.domain.model.OnboardingPermissionCategory
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.healthconnect.HealthConnectPermissionUxState
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

    private fun catalog(
        mindfulnessSupported: Boolean = true,
        includeCycle: Boolean = true,
        includeAdditional: Boolean = true,
    ): OnboardingPermissionCatalog {
        val categories = buildList {
            add(OnboardingPermissionCategory(OnboardingCategoryId.ACTIVITY, setOf("activity_r", "activity_w"), required = true))
            add(OnboardingPermissionCategory(OnboardingCategoryId.BODY, setOf("body_r", "body_w")))
            add(OnboardingPermissionCategory(OnboardingCategoryId.NUTRITION, setOf("nutrition_r")))
            add(OnboardingPermissionCategory(OnboardingCategoryId.SLEEP, setOf("sleep_r", "sleep_w"), required = true))
            add(OnboardingPermissionCategory(OnboardingCategoryId.VITALS, setOf("vitals_r")))
            if (includeCycle) add(OnboardingPermissionCategory(OnboardingCategoryId.CYCLE_TRACKING, setOf("cycle_r")))
            if (mindfulnessSupported) add(OnboardingPermissionCategory(OnboardingCategoryId.MINDFULNESS, setOf("mindfulness_r")))
            if (includeAdditional) add(OnboardingPermissionCategory(OnboardingCategoryId.ADDITIONAL_ACCESS, setOf("history", "background")))
        }
        return OnboardingPermissionCatalog(
            categories = categories,
            requiredPermissions = setOf("activity_r", "activity_w", "sleep_r", "sleep_w"),
            routeReadPermission = "route_read",
            mindfulnessSupportedByDevice = mindfulnessSupported,
        )
    }

    private fun repo(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        granted: Set<String> = emptySet(),
        catalog: OnboardingPermissionCatalog = catalog(),
    ): HealthRepository = mockk<HealthRepository>().also { repo ->
        every { repo.availability() } returns availability
        every { repo.onboardingPermissionCatalog() } returns catalog
        coEvery { repo.grantedPermissions() } returns granted
    }

    /**
     * The two mindfulness keys are backed by real vars rather than constants.
     * The view-model now re-reads them after a toggle — the catalog derives the
     * mindfulness permissions from them, so a cached copy goes stale the moment
     * they flip — and a stub that always answers `false` would report the write
     * as having no effect.
     */
    private fun prefs(optedIn: Boolean = false): PreferencesRepository = mockk<PreferencesRepository>().also { prefs ->
        var legacyOptIn = optedIn
        var integrationEnabled = optedIn
        every { prefs.appLanguage } returns AppLanguage.SYSTEM
        every { prefs.mindfulnessOptIn } answers { legacyOptIn }
        every { prefs.mindfulnessOptIn = any() } answers { legacyOptIn = firstArg() }
        every { prefs.healthConnectMindfulnessEnabled } answers { integrationEnabled }
        every { prefs.healthConnectMindfulnessEnabled = any() } answers { integrationEnabled = firstArg() }
        every { prefs.onboardingDone = any() } just Runs
        every { prefs.acceptedPrivacyPolicyVersion = any() } just Runs
        every { prefs.privacyPolicyAcceptedAtMillis = any() } just Runs
    }

    private fun uxState(): HealthConnectPermissionUxState = mockk(relaxed = true)

    private fun viewModel(
        repo: HealthRepository = repo(),
        prefs: PreferencesRepository = prefs(),
        ux: HealthConnectPermissionUxState = uxState(),
    ) = OnboardingViewModel(repo, prefs, ux)

    @Test fun `checkState loads catalog and granted permissions`() = runTest {
        val vm = viewModel(repo = repo(granted = setOf("activity_r")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isCheckingPermissions)
        assertEquals(setOf("activity_r"), state.grantedPermissions)
        // Step one's rows, complete and in Health Connect's own category order.
        assertEquals(
            listOf(
                OnboardingCategoryId.ACTIVITY,
                OnboardingCategoryId.BODY,
                OnboardingCategoryId.NUTRITION,
                OnboardingCategoryId.SLEEP,
                OnboardingCategoryId.VITALS,
            ),
            state.categoryRows.map { it.id },
        )
    }

    @Test fun `a fresh install derives every row outstanding`() = runTest {
        val vm = viewModel(repo = repo(granted = emptySet()))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.requiredGranted)
        assertFalse(state.canAdvance)
        for (row in state.categoryRows) {
            assertEquals(row.id.name, 0, row.grantedCount)
            assertFalse(row.id.name, row.fullyGranted)
            assertFalse(row.id.name, row.partial)
        }
        val activity = state.categoryRows.first { it.id == OnboardingCategoryId.ACTIVITY }
        assertTrue(activity.required)
        assertEquals(2, activity.total)
        assertEquals(setOf("activity_r", "activity_w"), vm.missingRequestableFor(OnboardingCategoryId.ACTIVITY))
        // The opt-in rows are NOT folded into the required set — there is no
        // second "grant the rest" request for them to feed.
        val required = requireNotNull(state.catalog).requiredPermissions
        assertEquals(setOf("activity_r", "activity_w", "sleep_r", "sleep_w"), required)
        assertFalse("mindfulness_r" in required)
        assertFalse("cycle_r" in required)
        assertFalse("route_read" in required)
    }

    @Test fun `an unsupported category is never granted, whatever is in the set`() = runTest {
        // The device does not do mindfulness: the row is locked, not complete.
        val base = catalog()
        val vm = viewModel(
            repo = repo(
                granted = setOf("mindfulness_r"),
                catalog = base.copy(
                    categories = base.categories.map { category ->
                        if (category.id == OnboardingCategoryId.MINDFULNESS) {
                            category.copy(available = false)
                        } else {
                            category
                        }
                    },
                ),
            ),
        )
        advanceUntilIdle()

        val row = requireNotNull(vm.uiState.value.mindfulnessRow)
        assertEquals(1, row.grantedCount)
        assertFalse(row.fullyGranted)
        assertFalse(row.partial)
    }

    @Test fun `the catalog is Health Connect's categories, in wizard order`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val catalog = requireNotNull(vm.uiState.value.catalog)
        assertEquals(
            listOf(
                OnboardingCategoryId.ACTIVITY,
                OnboardingCategoryId.BODY,
                OnboardingCategoryId.NUTRITION,
                OnboardingCategoryId.SLEEP,
                OnboardingCategoryId.VITALS,
                OnboardingCategoryId.CYCLE_TRACKING,
                OnboardingCategoryId.MINDFULNESS,
                OnboardingCategoryId.ADDITIONAL_ACCESS,
            ),
            catalog.categories.map { it.id },
        )
        // Every category carries a non-empty permission set; an empty one would
        // render as a row whose button grants nothing.
        assertTrue(catalog.categories.all { it.permissions.isNotEmpty() })
    }

    @Test fun `a manual-only permission cannot be granted by the runtime dialog`() = runTest {
        val vm = viewModel(repo = repo(granted = emptySet()))
        advanceUntilIdle()

        // Route reads belong to no category: the additional-access row's button
        // can only ask for what the row itself lists.
        val row = requireNotNull(vm.uiState.value.additionalAccessRow)
        assertEquals(setOf("history", "background"), row.permissions)
        assertFalse("route_read" in row.permissions)
        assertEquals(
            setOf("history", "background"),
            vm.missingRequestableFor(OnboardingCategoryId.ADDITIONAL_ACCESS),
        )
        // The step still applies, and stays unsatisfied, because the manual
        // grant is outstanding — that is what sends the user to settings.
        assertTrue(vm.uiState.value.routesOutstanding)
    }

    @Test fun `the additional-access row counts only what its button can grant`() = runTest {
        // Counting the route read here made the row read "2 of 3" forever: the
        // third could never be granted from anywhere the button leads.
        val vm = viewModel(
            repo = repo(granted = setOf("history", "background")),
        )
        advanceUntilIdle()

        val row = requireNotNull(vm.uiState.value.additionalAccessRow)
        assertEquals(2, row.total)
        assertEquals(2, row.grantedCount)
        assertTrue(row.fullyGranted)
        assertTrue(vm.missingRequestableFor(OnboardingCategoryId.ADDITIONAL_ACCESS).isEmpty())
        // Fully granted as a row, yet the walkthrough is still owed — so the
        // last step keeps applying and stays unsatisfied.
        assertTrue(vm.uiState.value.routesOutstanding)
        assertTrue(vm.uiState.value.stepApplies(OnboardingStep.ADDITIONAL_ACCESS))
    }

    @Test fun `back walks the steps, and only exits from the first`() = runTest {
        val vm = viewModel(
            repo = repo(granted = setOf("activity_r", "activity_w", "sleep_r", "sleep_w")),
        )
        advanceUntilIdle()

        // Nowhere behind step one: the screen exits instead of showing Back.
        assertTrue(vm.uiState.value.isFirstStep)
        vm.back()
        assertEquals(OnboardingStep.CATEGORIES, vm.uiState.value.step)

        vm.next()
        vm.next()
        assertEquals(OnboardingStep.CYCLE_TRACKING, vm.uiState.value.step)
        assertFalse(vm.uiState.value.isFirstStep)

        vm.back()
        assertEquals(OnboardingStep.MINDFULNESS, vm.uiState.value.step)
        vm.back()
        assertEquals(OnboardingStep.CATEGORIES, vm.uiState.value.step)
        assertTrue(vm.uiState.value.isFirstStep)
        vm.back()
        assertEquals(OnboardingStep.CATEGORIES, vm.uiState.value.step)
    }

    @Test fun `an empty catalog derives an empty display`() = runTest {
        val vm = viewModel(
            repo = repo(
                catalog = OnboardingPermissionCatalog(
                    categories = emptyList(),
                    requiredPermissions = emptySet(),
                    routeReadPermission = "route_read",
                    mindfulnessSupportedByDevice = false,
                ),
            ),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.categoryRows.isEmpty())
        assertNull(state.mindfulnessRow)
        assertNull(state.cycleRow)
        assertNull(state.additionalAccessRow)
        // Nothing required is missing, so onboarding can be finished.
        assertTrue(state.requiredGranted)
        assertTrue(state.canAdvance)
    }

    @Test fun `unavailable short-circuits without reading the catalog`() = runTest {
        val repo = mockk<HealthRepository>()
        every { repo.availability() } returns HealthConnectAvailability.NOT_SUPPORTED
        val vm = viewModel(repo = repo)
        advanceUntilIdle()

        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, vm.uiState.value.availability)
        assertFalse(vm.uiState.value.isCheckingPermissions)
        verify(exactly = 0) { repo.onboardingPermissionCatalog() }
    }

    @Test fun `step one gates advancing on the required set`() = runTest {
        val vm = viewModel(repo = repo(granted = setOf("activity_r")))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canAdvance)
    }

    @Test fun `next walks the applicable steps in order`() = runTest {
        val granted = setOf("activity_r", "activity_w", "sleep_r", "sleep_w")
        val vm = viewModel(repo = repo(granted = granted))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.canAdvance)
        vm.next()
        assertEquals(OnboardingStep.MINDFULNESS, vm.uiState.value.step)
        vm.next()
        assertEquals(OnboardingStep.CYCLE_TRACKING, vm.uiState.value.step)
        vm.next()
        assertEquals(OnboardingStep.ADDITIONAL_ACCESS, vm.uiState.value.step)
        assertTrue(vm.uiState.value.isLastStep)
        vm.back()
        assertEquals(OnboardingStep.CYCLE_TRACKING, vm.uiState.value.step)
    }

    @Test fun `steps that do not apply on this device are skipped`() = runTest {
        val vm = viewModel(
            repo = repo(
                granted = setOf("activity_r", "activity_w", "sleep_r", "sleep_w"),
                catalog = catalog(mindfulnessSupported = false, includeCycle = false),
            ),
        )
        advanceUntilIdle()

        vm.next()
        assertEquals(OnboardingStep.ADDITIONAL_ACCESS, vm.uiState.value.step)
        assertTrue(vm.uiState.value.isLastStep)
    }

    @Test fun `additional access step still applies for the routes walkthrough alone`() = runTest {
        val vm = viewModel(
            repo = repo(
                granted = setOf("activity_r", "activity_w", "sleep_r", "sleep_w"),
                catalog = catalog(mindfulnessSupported = false, includeCycle = false, includeAdditional = false),
            ),
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.routesOutstanding)
        vm.next()
        assertEquals(OnboardingStep.ADDITIONAL_ACCESS, vm.uiState.value.step)
    }

    @Test fun `a request that gains nothing raises the open-settings event`() = runTest {
        val repo = repo(granted = emptySet())
        val vm = viewModel(repo = repo)
        advanceUntilIdle()

        vm.beginPermissionRequest(setOf("activity_r"))
        vm.onPermissionsResult(emptySet())
        advanceUntilIdle()

        assertEquals(1L, vm.uiState.value.openSettingsEvent)
    }

    @Test fun `a request that gains permissions does not open settings`() = runTest {
        val repo = repo(granted = emptySet())
        val vm = viewModel(repo = repo)
        advanceUntilIdle()

        coEvery { repo.grantedPermissions() } returns setOf("activity_r")
        vm.beginPermissionRequest(setOf("activity_r"))
        vm.onPermissionsResult(setOf("activity_r"))
        advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.openSettingsEvent)
        assertEquals(setOf("activity_r"), vm.uiState.value.grantedPermissions)
    }

    @Test fun `an already-granted request does not open settings`() = runTest {
        val granted = setOf("activity_r", "activity_w")
        val vm = viewModel(repo = repo(granted = granted))
        advanceUntilIdle()

        vm.beginPermissionRequest(setOf("activity_r"))
        vm.onPermissionsResult(emptySet())
        advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.openSettingsEvent)
    }

    @Test fun `cancelled and granted results are recorded for permission ux`() = runTest {
        val ux = uxState()
        val vm = viewModel(ux = ux)
        advanceUntilIdle()

        vm.onPermissionsResult(emptySet())
        advanceUntilIdle()
        verify { ux.recordPermissionRequestCancelled() }

        vm.onPermissionsResult(setOf("activity_r"))
        advanceUntilIdle()
        verify { ux.recordPermissionRequestGranted() }
    }

    @Test fun `missingRequestableFor subtracts the granted set`() = runTest {
        val vm = viewModel(repo = repo(granted = setOf("activity_r")))
        advanceUntilIdle()

        assertEquals(setOf("activity_w"), vm.missingRequestableFor(OnboardingCategoryId.ACTIVITY))
    }

    @Test fun `mindfulness opt-in persists and updates state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(prefs = prefs)
        advanceUntilIdle()

        vm.setMindfulnessOptIn(true)

        verify { prefs.mindfulnessOptIn = true }
        assertTrue(vm.uiState.value.mindfulnessOptIn)
    }

    @Test fun `mindfulness step stays unsatisfied with the opt-in off`() = runTest {
        val vm = viewModel(
            repo = repo(granted = setOf("activity_r", "activity_w", "sleep_r", "sleep_w")),
        )
        advanceUntilIdle()

        vm.next()
        assertEquals(OnboardingStep.MINDFULNESS, vm.uiState.value.step)
        assertFalse(vm.uiState.value.currentStepSatisfied)
    }

    @Test fun `partial rows report their counts`() = runTest {
        val vm = viewModel(repo = repo(granted = setOf("body_r")))
        advanceUntilIdle()

        val bodyRow = vm.uiState.value.categoryRows.first { it.id == OnboardingCategoryId.BODY }
        assertTrue(bodyRow.partial)
        assertEquals(1, bodyRow.grantedCount)
        assertEquals(2, bodyRow.total)
    }

    @Test fun `selectAppLanguage persists and updates state`() = runTest {
        val prefs = prefs()
        every { prefs.appLanguage = any() } just Runs
        val vm = viewModel(prefs = prefs)
        advanceUntilIdle()

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `completeOnboarding stamps privacy policy and the done flag`() = runTest {
        val prefs = prefs()
        val vm = viewModel(prefs = prefs)
        advanceUntilIdle()

        vm.completeOnboarding()

        verify { prefs.acceptedPrivacyPolicyVersion = PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION }
        verify { prefs.privacyPolicyAcceptedAtMillis = any() }
        verify { prefs.onboardingDone = true }
    }

    // ── The mindfulness opt-in deadlock ──────────────────────────────────────

    @Test fun `a supporting device is offered the mindfulness step before opting in`() = runTest {
        // The deadlock this guards: the step carries the ONLY opt-in toggle, and
        // it used to be shown only when mindfulness was already enabled. On a
        // fresh install the step was therefore skipped forever and the
        // permission was never offered at all.
        //
        // So the two flags must stay independent — the device supports the
        // feature (step offered) while the catalog carries no mindfulness
        // category (the user has not opted in yet).
        val vm = viewModel(
            repo = repo(catalog = catalog(mindfulnessSupported = false).copy(
                mindfulnessSupportedByDevice = true,
            )),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull("not opted in, so no permissions to show yet", state.mindfulnessRow)
        assertTrue("the step must still be offered", state.stepApplies(OnboardingStep.MINDFULNESS))
        assertEquals(OnboardingStep.MINDFULNESS, state.stepAfter(OnboardingStep.CATEGORIES))
    }

    @Test fun `a device without the feature is not offered a toggle it cannot honour`() = runTest {
        val vm = viewModel(
            repo = repo(catalog = catalog(mindfulnessSupported = false).copy(
                mindfulnessSupportedByDevice = false,
            )),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.stepApplies(OnboardingStep.MINDFULNESS))
        assertEquals(OnboardingStep.CYCLE_TRACKING, state.stepAfter(OnboardingStep.CATEGORIES))
    }

    @Test fun `opting in rebuilds the catalog so the permission becomes requestable`() = runTest {
        // The second half of the bug: the catalog derives its mindfulness
        // permissions from the very preference the toggle writes, so the cached
        // copy goes stale the moment it flips. Without a rebuild the row stays
        // missing and Grant launches an empty request, which does nothing.
        val repo = mockk<HealthRepository>()
        every { repo.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { repo.grantedPermissions() } returns emptySet()
        var optedIn = false
        every { repo.onboardingPermissionCatalog() } answers {
            catalog(mindfulnessSupported = optedIn).copy(mindfulnessSupportedByDevice = true)
        }
        val prefs = prefs()
        every { prefs.healthConnectMindfulnessEnabled = any() } answers { optedIn = firstArg() }
        every { prefs.healthConnectMindfulnessEnabled } answers { optedIn }

        val vm = viewModel(repo = repo, prefs = prefs)
        advanceUntilIdle()
        assertNull(vm.uiState.value.mindfulnessRow)

        vm.setMindfulnessOptIn(true)
        advanceUntilIdle()

        val row = vm.uiState.value.mindfulnessRow
        assertEquals(setOf("mindfulness_r"), row?.permissions)
        assertEquals(setOf("mindfulness_r"), vm.missingRequestableFor(OnboardingCategoryId.MINDFULNESS))
    }

    @Test fun `opting back out drops the mindfulness permissions again`() = runTest {
        val repo = mockk<HealthRepository>()
        every { repo.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { repo.grantedPermissions() } returns emptySet()
        var optedIn = true
        every { repo.onboardingPermissionCatalog() } answers {
            catalog(mindfulnessSupported = optedIn).copy(mindfulnessSupportedByDevice = true)
        }
        val prefs = prefs(optedIn = true)
        every { prefs.healthConnectMindfulnessEnabled = any() } answers { optedIn = firstArg() }
        every { prefs.healthConnectMindfulnessEnabled } answers { optedIn }

        val vm = viewModel(repo = repo, prefs = prefs)
        advanceUntilIdle()
        assertEquals(setOf("mindfulness_r"), vm.uiState.value.mindfulnessRow?.permissions)

        vm.setMindfulnessOptIn(false)
        advanceUntilIdle()

        assertNull(vm.uiState.value.mindfulnessRow)
        // The step stays available: the toggle has to remain reachable to be
        // turned back on.
        assertTrue(vm.uiState.value.stepApplies(OnboardingStep.MINDFULNESS))
    }
}
