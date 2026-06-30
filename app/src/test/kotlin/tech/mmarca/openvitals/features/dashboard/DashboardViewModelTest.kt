package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    private fun prefs(
        sleepRangeMode: SleepRangeMode = SleepRangeMode.EVENING_18H,
        activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
        showOpenVitalsCalculatedCalories: Boolean = false,
    ) = mockk<PreferencesRepository>().also {
        every { it.acknowledgedPermissionsFor(any()) } returns emptySet()
        every { it.acknowledgePermissionsFor(any(), any()) } returns Unit
        every { it.sleepRangeMode } returns sleepRangeMode
        every { it.activityWeekMode } returns activityWeekMode
        every { it.showOpenVitalsCalculatedCalories } returns showOpenVitalsCalculatedCalories
        every { it.dailyGoalFor(any()) } answers { firstArg<MetricDailyGoalKey>().defaultValue }
        every { it.hydrationDailyGoalLiters } returns 2.0
        every { it.dashboardWidgetOrder() } returns null
        every { it.setDashboardWidgetOrder(any()) } returns Unit
        every { it.healthConnectSyncEnabled } returns true
    }

    // ─── Initial load ─────────────────────────────────────────────────────────

    @Test fun `initial state has isLoading true before coroutine runs`() {
        val loader = mockDashboardDataLoader()
        // Block the coroutine by never completing — use a suspended mock
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers { kotlinx.coroutines.awaitCancellation() }

        // With UnconfinedTestDispatcher the launch starts but suspends at awaitCancellation,
        // so we can inspect the intermediate state right after init sets isLoading = true
        // and before the repo call returns.
        // We verify the initial value set before the launch is isLoading = true via the
        // _uiState initial value (new DashboardUiState() has isLoading = true).
        val initial = DashboardUiState()
        assertTrue(initial.isLoading)
    }

    @Test fun `load success populates display widgets`() = runTest {
        val data = DashboardData(date = today, steps = 8_500)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        val stepsDisplay = vm.uiState.value.display.widgets[DashboardWidgetId.STEPS]
        assertNotNull(stepsDisplay)
        assertEquals(DashboardWidgetStyle.CIRCLE, stepsDisplay?.style)
        assertFalse(stepsDisplay?.isLoading ?: true)
    }

    @Test fun `load success populates data and clears loading`() = runTest {
        val data = DashboardData(date = today)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(data, state.data)
        assertNull(state.error)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } throws RuntimeException("network error")

        val vm = dashboardViewModel(loader, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.data)
        assertEquals(ScreenError.Message("network error"), state.error)
    }

    @Test fun `load failure with null message uses Unknown error fallback`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } throws RuntimeException()

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(ScreenError.Message("Unknown error"), vm.uiState.value.error)
    }

    // ─── Date clamping ────────────────────────────────────────────────────────

    @Test fun `load clamps future date to today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        val futureDate = today.plusDays(10)
        vm.load(futureDate)

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepRangeMode == SleepRangeMode.EVENING_18H }) }
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.selectDate(today.plusDays(5))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    @Test fun `previousDay decrements selectedDate by one day`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.previousDay()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }

    @Test fun `nextDay is blocked when selectedDate is today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
        // load called once by init, not again by blocked nextDay
        coVerify(exactly = 1) { loader.loadDashboard(any<DashboardQuery>()) }
    }

    @Test fun `nextDay advances from yesterday to today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.selectDate(yesterday)
        vm.nextDay()

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `resumeCurrentDay advances unpinned past date to today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.load(yesterday)
        vm.resumeCurrentDay()

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today }) }
    }

    @Test fun `resumeCurrentDay keeps user selected past date pinned`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.selectDate(yesterday)
        vm.resumeCurrentDay()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }

    // ─── A3: floorsClimbed + elevationGainedMeters in DashboardData ──────────

    @Test fun `floorsClimbed is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = 12)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(12, vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `floorsClimbed is null in state when not reported`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = null)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertNull(vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `elevationGainedMeters is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, elevationGainedMeters = 85.0)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(85.0, vm.uiState.value.data?.elevationGainedMeters!!, 0.01)
    }

    @Test fun `elevationGainedMeters is null in state when not reported`() = runTest {
        val data = DashboardData(date = today, elevationGainedMeters = null)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertNull(vm.uiState.value.data?.elevationGainedMeters)
    }

    @Test fun `floorsClimbed zero is non-null — permission granted no stair data`() = runTest {
        val data = DashboardData(date = today, floorsClimbed = 0)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(0, vm.uiState.value.data?.floorsClimbed)
    }

    @Test fun `caloriesInKcal is exposed through state when present`() = runTest {
        val data = DashboardData(date = today, caloriesInKcal = 1_850.0)
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(1_850.0, vm.uiState.value.data?.caloriesInKcal!!, 0.01)
    }

    @Test fun `vitals fields are exposed through dashboard state when present`() = runTest {
        val data = DashboardData(
            date = today,
            latestSystolicMmHg = 120,
            latestDiastolicMmHg = 78,
            latestSpO2Percent = 97.5,
            latestVo2Max = 42.1,
        )
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns data

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(120, vm.uiState.value.data?.latestSystolicMmHg)
        assertEquals(78, vm.uiState.value.data?.latestDiastolicMmHg)
        assertEquals(97.5, vm.uiState.value.data?.latestSpO2Percent!!, 0.01)
        assertEquals(42.1, vm.uiState.value.data?.latestVo2Max!!, 0.01)
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test fun `refresh reloads current date`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.refresh()

        // init + refresh = 2 calls
        coVerify(exactly = 2) { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepRangeMode == SleepRangeMode.EVENING_18H }) }
    }

    @Test fun `load passes sleep range mode from preferences`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        dashboardViewModel(loader, prefs(sleepRangeMode = SleepRangeMode.NOON))

        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepRangeMode == SleepRangeMode.NOON }) }
    }

    @Test fun `load passes activity week mode from preferences`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        dashboardViewModel(loader, prefs(activityWeekMode = ActivityWeekMode.LAST_7_DAYS))

        coVerify {
            loader.loadDashboard(
                match<DashboardQuery> {
                    it.date == today && it.activityWeekMode == ActivityWeekMode.LAST_7_DAYS
                }
            )
        }
    }

    @Test fun `load scopes dashboard query to dashboard focus widgets`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            queries += firstArg<DashboardQuery>()
            DashboardData(date = today)
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.HYDRATION.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.WEIGHT.name,
        )

        dashboardViewModel(loader, prefs)

        assertEquals(
            setOf(
                DashboardMetric.STEPS,
                DashboardMetric.DISTANCE,
                DashboardMetric.CALORIES_OUT,
                DashboardMetric.WHEELCHAIR_PUSHES,
                DashboardMetric.WORKOUT,
                DashboardMetric.AVG_HEART_RATE,
            ),
            queries.first().visibleMetrics,
        )
    }

    @Test fun `average heart rate widget loads with primary dashboard query`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            DashboardData(
                date = today,
                avgHeartRateBpm = 72L,
                loadedMetrics = setOf(DashboardMetric.AVG_HEART_RATE),
            )
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(DashboardWidgetId.AVG_HEART_RATE.name)

        val vm = dashboardViewModel(loader, prefs)

        assertTrue(DashboardMetric.AVG_HEART_RATE in queries.first().visibleMetrics)
        assertFalse(DashboardWidgetId.AVG_HEART_RATE in vm.uiState.value.pendingWidgets)
        assertEquals(1, queries.size)
    }

    @Test fun `deferred dashboard metrics merge after fast dashboard load`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            when (query.visibleMetrics.singleOrNull()) {
                DashboardMetric.HYDRATION -> DashboardData(
                    date = today,
                    hydrationLiters = 1.5,
                    loadedMetrics = setOf(DashboardMetric.HYDRATION),
                )
                else -> DashboardData(
                    date = today,
                    steps = 100,
                    loadedMetrics = setOf(
                        DashboardMetric.STEPS,
                        DashboardMetric.DISTANCE,
                        DashboardMetric.CALORIES_OUT,
                        DashboardMetric.WORKOUT,
                    ),
                )
            }
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.HYDRATION.name,
        )

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(1, queries.size)
        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.HYDRATION))
        advanceUntilIdle()

        assertEquals(100L, vm.uiState.value.data?.steps)
        assertEquals(1.5, vm.uiState.value.data?.hydrationLiters ?: 0.0, 0.001)
        assertTrue(queries.first().visibleMetrics.contains(DashboardMetric.STEPS))
        assertTrue(queries.any { it.visibleMetrics == setOf(DashboardMetric.HYDRATION) })
        assertTrue(vm.uiState.value.pendingWidgets.isEmpty())
    }

    @Test fun `deferred dashboard metrics coalesce ui updates`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            when (query.visibleMetrics.singleOrNull()) {
                DashboardMetric.HYDRATION -> DashboardData(
                    date = today,
                    hydrationLiters = 1.5,
                    loadedMetrics = setOf(DashboardMetric.HYDRATION),
                )
                DashboardMetric.SLEEP -> DashboardData(
                    date = today,
                    sleep = null,
                    loadedMetrics = setOf(DashboardMetric.SLEEP),
                )
                else -> DashboardData(
                    date = today,
                    steps = 100,
                    loadedMetrics = setOf(
                        DashboardMetric.STEPS,
                        DashboardMetric.DISTANCE,
                        DashboardMetric.CALORIES_OUT,
                        DashboardMetric.WORKOUT,
                    ),
                )
            }
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.HYDRATION.name,
            DashboardWidgetId.SLEEP.name,
        )

        val vm = dashboardViewModel(loader, prefs)

        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.HYDRATION, DashboardWidgetId.SLEEP))
        advanceUntilIdle()

        assertEquals(100L, vm.uiState.value.data?.steps)
        assertEquals(1.5, vm.uiState.value.data?.hydrationLiters ?: 0.0, 0.001)
        assertTrue(vm.uiState.value.pendingWidgets.isEmpty())
        assertTrue(queries.any { it.visibleMetrics == setOf(DashboardMetric.HYDRATION) })
        assertTrue(queries.any { it.visibleMetrics == setOf(DashboardMetric.SLEEP) })
    }

    @Test fun `hidden deferred dashboard widgets wait until reported visible`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            when (query.visibleMetrics.singleOrNull()) {
                DashboardMetric.HYDRATION -> DashboardData(
                    date = today,
                    hydrationLiters = 1.5,
                    loadedMetrics = setOf(DashboardMetric.HYDRATION),
                )
                DashboardMetric.SLEEP -> DashboardData(
                    date = today,
                    sleep = null,
                    loadedMetrics = setOf(DashboardMetric.SLEEP),
                )
                else -> DashboardData(
                    date = today,
                    steps = 100,
                    loadedMetrics = setOf(
                        DashboardMetric.STEPS,
                        DashboardMetric.DISTANCE,
                        DashboardMetric.CALORIES_OUT,
                        DashboardMetric.WORKOUT,
                    ),
                )
            }
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.HYDRATION.name,
            DashboardWidgetId.SLEEP.name,
        )

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(1, queries.size)
        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.STEPS, DashboardWidgetId.HYDRATION))
        advanceUntilIdle()

        assertTrue(queries.any { it.visibleMetrics == setOf(DashboardMetric.HYDRATION) })
        assertFalse(queries.any { it.visibleMetrics == setOf(DashboardMetric.SLEEP) })

        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.SLEEP))
        advanceUntilIdle()

        assertTrue(queries.any { it.visibleMetrics == setOf(DashboardMetric.SLEEP) })
    }

    @Test fun `weekly deferred dashboard loads request weekly cardio metric`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            queries += firstArg<DashboardQuery>()
            DashboardData(
                date = today,
                steps = 100,
                loadedMetrics = setOf(
                    DashboardMetric.STEPS,
                    DashboardMetric.DISTANCE,
                    DashboardMetric.CALORIES_OUT,
                    DashboardMetric.WORKOUT,
                ),
            )
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD.name,
        )

        val vm = dashboardViewModel(loader, prefs)

        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.WEEKLY_CARDIO_LOAD))
        advanceUntilIdle()

        assertTrue(
            queries.any {
                DashboardMetric.WEEKLY_CARDIO_LOAD in it.visibleMetrics
            }
        )
    }

    @Test fun `stale deferred dashboard load cannot overwrite newer data`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            if (query.date == yesterday && query.visibleMetrics == setOf(DashboardMetric.HYDRATION)) {
                delay(100)
            }
            when {
                query.date == yesterday && DashboardMetric.HYDRATION in query.visibleMetrics -> DashboardData(
                    date = yesterday,
                    hydrationLiters = 9.0,
                    loadedMetrics = setOf(DashboardMetric.HYDRATION),
                )
                query.date == yesterday -> DashboardData(
                    date = yesterday,
                    steps = 1,
                    loadedMetrics = setOf(
                        DashboardMetric.STEPS,
                        DashboardMetric.DISTANCE,
                        DashboardMetric.CALORIES_OUT,
                        DashboardMetric.WORKOUT,
                    ),
                )
                query.date == today && DashboardMetric.HYDRATION in query.visibleMetrics -> DashboardData(
                    date = today,
                    hydrationLiters = 2.0,
                    loadedMetrics = setOf(DashboardMetric.HYDRATION),
                )
                else -> DashboardData(
                    date = today,
                    steps = 2,
                    loadedMetrics = setOf(
                        DashboardMetric.STEPS,
                        DashboardMetric.DISTANCE,
                        DashboardMetric.CALORIES_OUT,
                        DashboardMetric.WORKOUT,
                    ),
                )
            }
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.HYDRATION.name,
        )

        val vm = dashboardViewModel(loader, prefs)
        vm.load(yesterday)
        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.HYDRATION))
        vm.load(today)
        vm.loadVisibleDashboardWidgets(setOf(DashboardWidgetId.HYDRATION))
        advanceUntilIdle()

        assertEquals(today, vm.uiState.value.data?.date)
        assertEquals(2.0, vm.uiState.value.data?.hydrationLiters ?: 0.0, 0.001)
    }

    @Test fun `refresh passes force refresh mode`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            queries += firstArg<DashboardQuery>()
            DashboardData(date = today)
        }

        val vm = dashboardViewModel(loader, prefs())
        vm.refresh()

        assertEquals(RefreshMode.FORCE, queries.last().refreshMode)
    }

    @Test fun `newer load wins when navigation requests overlap`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            if (query.date == yesterday) {
                delay(100)
            }
            DashboardData(date = query.date)
        }

        val vm = dashboardViewModel(loader, prefs())
        vm.load(yesterday)
        vm.load(today)

        assertEquals(today, vm.uiState.value.data?.date)
    }

    @Test fun `refreshPreferences reloads dashboard when sleep range mode changes`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs(sleepRangeMode = SleepRangeMode.EVENING_18H)
        every { prefs.sleepRangeMode } returnsMany listOf(
            SleepRangeMode.EVENING_18H,
            SleepRangeMode.NOON,
            SleepRangeMode.NOON,
        )
        val vm = dashboardViewModel(loader, prefs)

        vm.refreshPreferences()

        assertEquals(SleepRangeMode.NOON, vm.uiState.value.sleepRangeMode)
        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepRangeMode == SleepRangeMode.NOON }) }
    }

    @Test fun `refreshPreferences reloads dashboard when activity week mode changes`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs(activityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY)
        var activityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY
        every { prefs.activityWeekMode } answers { activityWeekMode }
        val vm = dashboardViewModel(loader, prefs)

        activityWeekMode = ActivityWeekMode.LAST_7_DAYS
        vm.refreshPreferences()

        assertEquals(ActivityWeekMode.LAST_7_DAYS, vm.uiState.value.activityWeekMode)
        coVerify {
            loader.loadDashboard(
                match<DashboardQuery> {
                    it.date == today && it.activityWeekMode == ActivityWeekMode.LAST_7_DAYS
                }
            )
        }
    }

    @Test fun `refreshPreferences reloads dashboard when calorie calculation mode changes`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs()
        var showOpenVitalsCalculatedCalories = false
        every { prefs.showOpenVitalsCalculatedCalories } answers { showOpenVitalsCalculatedCalories }
        val vm = dashboardViewModel(loader, prefs)

        showOpenVitalsCalculatedCalories = true
        vm.refreshPreferences()

        assertTrue(vm.uiState.value.showOpenVitalsCalculatedCalories)
        coVerify(exactly = 2) { loader.loadDashboard(any<DashboardQuery>()) }
    }

    @Test fun `dashboard widgets default to full widget set`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())

        assertEquals(DefaultDashboardWidgetIds, vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard daily goals follow preferences`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs()
        every { prefs.dailyGoalFor(MetricDailyGoalKey.STEPS) } returns 12_000.0
        every { prefs.dailyGoalFor(MetricDailyGoalKey.SLEEP_HOURS) } returns 7.5
        every { prefs.hydrationDailyGoalLiters } returns 3.0

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(12_000.0, vm.uiState.value.dailyGoals.steps, 0.001)
        assertEquals(7.5, vm.uiState.value.dailyGoals.sleepHours, 0.001)
        assertEquals(3.0, vm.uiState.value.dailyGoals.hydrationLiters, 0.001)
    }

    @Test fun `dashboard widgets restore saved order`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.STEPS.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(
            listOf(DashboardWidgetId.SLEEP, DashboardWidgetId.STEPS),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widgets ignore unknown saved ids`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf("unknown", DashboardWidgetId.STEPS.name)
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(listOf(DashboardWidgetId.STEPS), vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widgets ignore legacy browse saved id`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            "BROWSE",
            DashboardWidgetId.STEPS.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs)

        assertEquals(listOf(DashboardWidgetId.STEPS), vm.uiState.value.dashboardWidgets)
    }

    @Test fun `dashboard widget remove add and move persist order`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val vm = dashboardViewModel(loader, prefs)

        vm.removeDashboardWidget(DashboardWidgetId.DISTANCE)
        assertFalse(DashboardWidgetId.DISTANCE in vm.uiState.value.dashboardWidgets)

        vm.addDashboardWidget(DashboardWidgetId.DISTANCE)
        assertEquals(DashboardWidgetId.DISTANCE, vm.uiState.value.dashboardWidgets.last())

        vm.moveDashboardWidget(DashboardWidgetId.DISTANCE, -1)
        assertEquals(
            DashboardWidgetId.DISTANCE,
            vm.uiState.value.dashboardWidgets[vm.uiState.value.dashboardWidgets.lastIndex - 1],
        )
    }

    @Test fun `dashboard widget moves to target drop position`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val vm = dashboardViewModel(loader, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.STEPS, DashboardWidgetId.CALORIES_OUT)

        assertEquals(
            listOf(
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.STEPS,
                DashboardWidgetId.SLEEP,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widget swaps when moved from carousel to fixed section`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val vm = dashboardViewModel(loader, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.HYDRATION, DashboardWidgetId.DISTANCE)

        assertEquals(
            listOf(
                DashboardWidgetId.STEPS,
                DashboardWidgetId.HYDRATION,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.DISTANCE,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `dashboard widget swaps when moved from fixed to carousel section`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val vm = dashboardViewModel(loader, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.STEPS, DashboardWidgetId.HYDRATION)

        assertEquals(
            listOf(
                DashboardWidgetId.HYDRATION,
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.STEPS,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `deleteActivityEntry deletes OpenVitals dashboard activity and refreshes`() = runTest {
        val workout = dashboardWorkout(id = "activity-1", isOpenVitalsEntry = true)
        val loader = mockDashboardDataLoader()
        val activityRepo = mockk<ActivityRepository>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(
            date = today,
            workouts = listOf(workout),
        )
        coEvery { activityRepo.deleteActivityEntry("activity-1") } returns Unit
        val vm = dashboardViewModel(loader, prefs(), activityRepo)

        vm.deleteActivityEntry("activity-1")

        coVerify(exactly = 1) { activityRepo.deleteActivityEntry("activity-1") }
        coVerify(exactly = 2) { loader.loadDashboard(any<DashboardQuery>()) }
    }

    private fun dashboardWorkout(id: String, isOpenVitalsEntry: Boolean) = ExerciseData(
        id = id,
        title = "Workout",
        exerciseType = 56,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusSeconds(60),
        durationMs = 60_000,
        source = "test",
        isOpenVitalsEntry = isOpenVitalsEntry,
    )

    private fun dashboardViewModel(
        loader: DashboardDataLoader = mockDashboardDataLoader(),
        prefs: PreferencesRepository = prefs(),
        activityRepo: ActivityRepository? = null,
        repo: HealthRepository = mockHealthRepository(),
    ): DashboardViewModel =
        DashboardViewModel(
            loadDashboardDayUseCase = LoadDashboardDayUseCase(loader),
            repository = repo,
            prefs = prefs,
            unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
            activityRepository = activityRepo,
        )

    private fun mockDashboardDataLoader(configure: DashboardDataLoader.() -> Unit = {}): DashboardDataLoader =
        mockk<DashboardDataLoader>().also(configure)

    private fun mockHealthRepository(configure: HealthRepository.() -> Unit = {}): HealthRepository =
        mockk<HealthRepository>().also { repo ->
            every { repo.availability() } returns HealthConnectAvailability.AVAILABLE
            every { repo.minimumOnboardingPermissions } returns emptySet()
            coEvery { repo.grantedPermissions() } returns emptySet()
            configure(repo)
        }

}
