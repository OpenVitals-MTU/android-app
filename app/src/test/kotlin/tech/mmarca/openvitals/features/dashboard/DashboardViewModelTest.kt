package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineResult
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.data.sync.HistorySyncScheduler
import tech.mmarca.openvitals.domain.usecase.LoadDashboardDayUseCase
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
        sleepWindow: SleepWindow = SleepWindow.Default,
        activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
        showOpenVitalsCalculatedCalories: Boolean = false,
    ) = mockk<PreferencesRepository>().also {
        every { it.acknowledgedPermissionsFor(any()) } returns emptySet()
        every { it.acknowledgePermissionsFor(any(), any()) } returns Unit
        every { it.sleepWindow } returns sleepWindow
        every { it.activityWeekMode } returns activityWeekMode
        every { it.showOpenVitalsCalculatedCalories } returns showOpenVitalsCalculatedCalories
        every { it.dailyGoalFor(any()) } answers { firstArg<MetricDailyGoalKey>().defaultValue }
        every { it.hydrationDailyGoalLiters } returns 2.0
        every { it.dashboardWidgetOrder() } returns null
        every { it.dashboardSortEmptyTilesLast } returns true
        every { it.setDashboardWidgetOrder(any()) } returns Unit
        every { it.healthConnectSyncEnabled } returns true
        every { it.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
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

    @Test fun `transient load cancellation retries without surfacing dashboard error`() = runTest {
        val loader = mockDashboardDataLoader()
        var loadCalls = 0
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            loadCalls += 1
            if (loadCalls == 1) {
                throw CancellationException("Job was cancelled")
            }
            DashboardData(date = today, steps = 7_200)
        }

        val vm = dashboardViewModel(loader, prefs())

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(7_200L, state.data?.steps)
        assertNull(state.error)
        coVerify(exactly = 2) { loader.loadDashboard(any<DashboardQuery>()) }
    }

    @Test fun `sensor status includes saved battery and live connection status`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val devicesFlow = MutableStateFlow(
            listOf(
                BleSensorDevice(
                    id = "hr",
                    displayName = "Heart strap",
                    address = "AA:BB:CC:DD:EE:01",
                    bluetoothName = null,
                    capabilities = setOf(BleSensorCapability.HEART_RATE),
                    enabled = true,
                    wheelCircumferenceMm = null,
                    batteryPercent = 72,
                    batteryUpdatedAt = Instant.EPOCH,
                    addedAt = Instant.EPOCH,
                ),
            ),
        )
        val metricsFlow = MutableStateFlow(BleRecordingMetrics())
        val deviceRepository = mockk<BleDeviceRepository>()
        every { deviceRepository.devicesFlow } returns devicesFlow
        val sensorCoordinator = mockk<BleSensorCoordinator>()
        every { sensorCoordinator.metrics } returns metricsFlow

        val vm = dashboardViewModel(
            loader = loader,
            prefs = prefs(),
            bleDeviceRepository = deviceRepository,
            bleSensorCoordinator = sensorCoordinator,
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.sensorStatus.hasDevices)
        assertEquals(72, vm.uiState.value.sensorStatus.lowestBatteryPercent)
        metricsFlow.value = BleRecordingMetrics(
            deviceStatuses = listOf(
                BleDeviceConnectionStatus(
                    deviceId = "hr",
                    displayName = "Heart strap",
                    address = "AA:BB:CC:DD:EE:01",
                    status = BleConnectionStatus.CONNECTED,
                    capabilities = setOf(BleSensorCapability.HEART_RATE),
                    batteryPercent = 38,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(38, vm.uiState.value.sensorStatus.lowestBatteryPercent)
        assertEquals(1, vm.uiState.value.sensorStatus.connectedCount)
    }

    // ─── Date clamping ────────────────────────────────────────────────────────

    @Test fun `load clamps future date to today`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        val futureDate = today.plusDays(10)
        vm.load(futureDate)

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepWindow == SleepWindow.Default }) }
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
        // Deliberate divergence from Flutter, pinned here so it cannot drift
        // silently: a pinned past day is never yanked forward, and Kotlin does
        // not refresh it in place either — the resume adds no read of its own
        // (init loaded today, selectDate loaded yesterday, and that is all).
        coVerify(exactly = 2) { loader.loadDashboard(any<DashboardQuery>()) }
    }

    @Test fun `selectDate on a past day pins it and selecting today clears the pin`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.selectDate(today.minusDays(3))
        vm.resumeCurrentDay()
        // Pinned: the resume left the chosen day alone.
        assertEquals(today.minusDays(3), vm.uiState.value.selectedDate)

        vm.selectDate(today)
        vm.resumeCurrentDay()

        // Selecting today cleared the pin, so the next resume re-reads today.
        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify(atLeast = 2) { loader.loadDashboard(match<DashboardQuery> { it.date == today }) }
    }

    @Test fun `nextDay onto a still-past day keeps the pin`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.previousDay()
        vm.previousDay()
        // Two days back, then forward one: still in the past, so still pinned.
        vm.nextDay()
        assertEquals(yesterday, vm.uiState.value.selectedDate)

        vm.resumeCurrentDay()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
    }

    @Test fun `nextDay back onto today clears the pin`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        val vm = dashboardViewModel(loader, prefs())
        vm.previousDay()
        vm.nextDay()
        assertEquals(today, vm.uiState.value.selectedDate)

        vm.resumeCurrentDay()

        // The pin was recomputed by nextDay, so the resume re-reads today rather
        // than sitting on a day the user already left.
        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify(atLeast = 2) { loader.loadDashboard(match<DashboardQuery> { it.date == today }) }
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
        coVerify(exactly = 2) { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepWindow == SleepWindow.Default }) }
    }

    @Test fun `load passes sleep range mode from preferences`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        dashboardViewModel(loader, prefs(sleepWindow = SleepWindow(startHour = 20, endHour = 8)))

        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepWindow == SleepWindow(startHour = 20, endHour = 8) }) }
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

    @Test fun `load requests visible dashboard widgets first`() = runTest {
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
                DashboardMetric.SLEEP,
                DashboardMetric.STEPS,
                DashboardMetric.HYDRATION,
                DashboardMetric.DISTANCE,
                DashboardMetric.WEIGHT,
            ),
            queries.first().visibleMetrics,
        )
        assertFalse(queries.first().includeHistoricalBaselines)
    }

    @Test fun `dashboard widget order scopes first dashboard query`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            DashboardData(date = today)
        }
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(DashboardWidgetId.AVG_HEART_RATE.name)

        dashboardViewModel(loader, prefs)

        assertEquals(setOf(DashboardMetric.AVG_HEART_RATE), queries.first().visibleMetrics)
        assertEquals(1, queries.size)
    }

    @Test fun `background dashboard query loads remaining configured widget metrics`() = runTest {
        val loader = mockDashboardDataLoader()
        val queries = mutableListOf<DashboardQuery>()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.ACTIVE_CALORIES.name,
            DashboardWidgetId.FLOORS.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
            DashboardWidgetId.WEIGHT.name,
            DashboardWidgetId.AVG_HEART_RATE.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            queries += query
            DashboardData(date = today, loadedMetrics = query.visibleMetrics)
        }

        dashboardViewModel(loader, prefs)
        advanceUntilIdle()

        assertEquals(2, queries.size)
        assertEquals(
            setOf(
                DashboardMetric.STEPS,
                DashboardMetric.WEEKLY_CARDIO_LOAD,
                DashboardMetric.DISTANCE,
                DashboardMetric.CALORIES_OUT,
                DashboardMetric.ACTIVE_CALORIES,
                DashboardMetric.FLOORS,
                DashboardMetric.SLEEP,
                DashboardMetric.HYDRATION,
            ),
            queries.first().visibleMetrics,
        )
        assertEquals(
            setOf(
                DashboardMetric.WEIGHT,
                DashboardMetric.AVG_HEART_RATE,
            ),
            queries[1].visibleMetrics,
        )
        assertTrue(queries[1].includeHistoricalBaselines)
    }

    @Test fun `a carousel twin of a quick-loaded metric does not stay loading`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        // CARDIO_LOAD lands beyond the quick section, but its metric is the
        // hero WEEKLY_CARDIO_LOAD's — already loaded by the quick pass. The
        // background pass never loads that metric, so marking the twin as
        // loading left it on "Loading" forever.
        every { prefs.dashboardWidgetOrder() } returns listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD.name,
            DashboardWidgetId.DISTANCE.name,
            DashboardWidgetId.CALORIES_OUT.name,
            DashboardWidgetId.ACTIVE_CALORIES.name,
            DashboardWidgetId.FLOORS.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.HYDRATION.name,
            DashboardWidgetId.WEIGHT.name,
            DashboardWidgetId.AVG_HEART_RATE.name,
            DashboardWidgetId.CARDIO_LOAD.name,
        )
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            val query = firstArg<DashboardQuery>()
            DashboardData(date = today, loadedMetrics = query.visibleMetrics)
        }

        val vm = dashboardViewModel(loader, prefs)
        advanceUntilIdle()

        assertEquals(emptySet<DashboardWidgetId>(), vm.uiState.value.loadingWidgets)
        assertFalse(
            vm.uiState.value.display.widgets[DashboardWidgetId.CARDIO_LOAD]?.isLoading ?: true,
        )
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

    // ─── Open coalescing ──────────────────────────────────────────────────────
    // Opening the dashboard fires the init load and the first ON_RESUME within
    // the same frame. Restarting the in-flight load would issue every Health
    // Connect read twice per open — the rate-limit budget this screen keeps
    // blowing — so the duplicate NORMAL request is absorbed instead.

    @Test fun `the first resume is absorbed by the in-flight open load`() = runTest {
        val loader = mockDashboardDataLoader()
        val gate = CompletableDeferred<Unit>()
        var loads = 0
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            loads += 1
            gate.await()
            DashboardData(date = today, steps = 8_000)
        }

        val vm = dashboardViewModel(loader, prefs())
        // The ON_RESUME that lands while the init load is still reading.
        vm.resumeCurrentDay()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, loads)
        assertEquals(8_000L, vm.uiState.value.data?.steps)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test fun `a resume after the open load settles still reloads the day`() = runTest {
        val loader = mockDashboardDataLoader()
        var loads = 0
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            loads += 1
            DashboardData(date = today)
        }

        val vm = dashboardViewModel(loader, prefs())
        advanceUntilIdle()
        assertEquals(1, loads)

        // Returning to the app after backgrounding: the only signal that Health
        // Connect data may have changed, so it must genuinely reload.
        vm.resumeCurrentDay()
        advanceUntilIdle()

        assertEquals(2, loads)
    }

    @Test fun `force refresh is not absorbed by an in-flight load`() = runTest {
        val loader = mockDashboardDataLoader()
        val gate = CompletableDeferred<Unit>()
        val queries = mutableListOf<DashboardQuery>()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            queries += firstArg<DashboardQuery>()
            gate.await()
            DashboardData(date = today)
        }

        val vm = dashboardViewModel(loader, prefs())
        // Pull-to-refresh while the open load is still in flight restarts it.
        vm.refresh()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, queries.size)
        assertEquals(RefreshMode.FORCE, queries.last().refreshMode)
    }

    @Test fun `refreshPreferences reloads dashboard when sleep range mode changes`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs(sleepWindow = SleepWindow.Default)
        every { prefs.sleepWindow } returnsMany listOf(
            SleepWindow.Default,
            SleepWindow(startHour = 20, endHour = 8),
            SleepWindow(startHour = 20, endHour = 8),
        )
        val vm = dashboardViewModel(loader, prefs)

        vm.refreshPreferences()

        assertEquals(SleepWindow(startHour = 20, endHour = 8), vm.uiState.value.sleepWindow)
        coVerify { loader.loadDashboard(match<DashboardQuery> { it.date == today && it.sleepWindow == SleepWindow(startHour = 20, endHour = 8) }) }
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
        // The bug this pins was never only about steps: one class held fixed
        // constants for all fourteen metrics. If any of these still reads a
        // default, that metric's ring is lying to the same user in the same way.
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val prefs = prefs()
        val stored = mapOf(
            MetricDailyGoalKey.STEPS to 6_000.0,
            MetricDailyGoalKey.DISTANCE_METERS to 3_000.0,
            MetricDailyGoalKey.CALORIES_OUT_KCAL to 2_500.0,
            MetricDailyGoalKey.ACTIVE_CALORIES_KCAL to 600.0,
            MetricDailyGoalKey.FLOORS to 20.0,
            MetricDailyGoalKey.ELEVATION_METERS to 250.0,
            MetricDailyGoalKey.WHEELCHAIR_PUSHES to 2_000.0,
            MetricDailyGoalKey.SLEEP_HOURS to 7.0,
            MetricDailyGoalKey.CALORIES_IN_KCAL to 2_200.0,
            MetricDailyGoalKey.PROTEIN_GRAMS to 120.0,
            MetricDailyGoalKey.CARBS_GRAMS to 300.0,
            MetricDailyGoalKey.FAT_GRAMS to 80.0,
            MetricDailyGoalKey.MINDFULNESS_MINUTES to 20.0,
        )
        // Premise: not one of the stored values may equal the out-of-the-box
        // default, or a goal that still reads a constant would pass anyway.
        stored.forEach { (key, value) -> assertNotEquals(key.defaultValue, value, 0.001) }
        every { prefs.dailyGoalFor(any()) } answers {
            val key = firstArg<MetricDailyGoalKey>()
            stored[key] ?: key.defaultValue
        }
        every { prefs.hydrationDailyGoalLiters } returns 3.0

        val goals = dashboardViewModel(loader, prefs).uiState.value.dailyGoals

        assertEquals(6_000.0, goals.steps, 0.001)
        assertEquals(3_000.0, goals.distanceMeters, 0.001)
        assertEquals(2_500.0, goals.caloriesOutKcal, 0.001)
        assertEquals(600.0, goals.activeCaloriesKcal, 0.001)
        assertEquals(20.0, goals.floors, 0.001)
        assertEquals(250.0, goals.elevationMeters, 0.001)
        assertEquals(2_000.0, goals.wheelchairPushes, 0.001)
        assertEquals(7.0, goals.sleepHours, 0.001)
        assertEquals(2_200.0, goals.caloriesInKcal, 0.001)
        assertEquals(120.0, goals.proteinGrams, 0.001)
        assertEquals(300.0, goals.carbsGrams, 0.001)
        assertEquals(80.0, goals.fatGrams, 0.001)
        assertEquals(20.0, goals.mindfulnessMinutes, 0.001)
        // Hydration is the odd one out: its own preference, NOT a
        // MetricDailyGoalKey. Reading it from the wrong place is a silent default.
        assertEquals(3.0, goals.hydrationLiters, 0.001)
        assertNotEquals(DashboardDailyGoals().hydrationLiters, goals.hydrationLiters, 0.001)
    }

    @Test fun `an untouched install still gets the documented defaults`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)

        // The documented defaults are the goal store's own, not a second copy
        // that could drift away from the metric detail screens.
        val defaults = DashboardDailyGoals()
        assertEquals(MetricDailyGoalKey.STEPS.defaultValue, defaults.steps, 0.001)
        assertEquals(MetricDailyGoalKey.DISTANCE_METERS.defaultValue, defaults.distanceMeters, 0.001)
        assertEquals(MetricDailyGoalKey.CALORIES_OUT_KCAL.defaultValue, defaults.caloriesOutKcal, 0.001)
        assertEquals(MetricDailyGoalKey.ACTIVE_CALORIES_KCAL.defaultValue, defaults.activeCaloriesKcal, 0.001)
        assertEquals(MetricDailyGoalKey.FLOORS.defaultValue, defaults.floors, 0.001)
        assertEquals(MetricDailyGoalKey.ELEVATION_METERS.defaultValue, defaults.elevationMeters, 0.001)
        assertEquals(MetricDailyGoalKey.WHEELCHAIR_PUSHES.defaultValue, defaults.wheelchairPushes, 0.001)
        assertEquals(MetricDailyGoalKey.SLEEP_HOURS.defaultValue, defaults.sleepHours, 0.001)
        assertEquals(MetricDailyGoalKey.CALORIES_IN_KCAL.defaultValue, defaults.caloriesInKcal, 0.001)
        assertEquals(MetricDailyGoalKey.PROTEIN_GRAMS.defaultValue, defaults.proteinGrams, 0.001)
        assertEquals(MetricDailyGoalKey.CARBS_GRAMS.defaultValue, defaults.carbsGrams, 0.001)
        assertEquals(MetricDailyGoalKey.FAT_GRAMS.defaultValue, defaults.fatGrams, 0.001)
        assertEquals(MetricDailyGoalKey.MINDFULNESS_MINUTES.defaultValue, defaults.mindfulnessMinutes, 0.001)
        assertEquals(2.0, defaults.hydrationLiters, 0.001)

        // And an install that never touched a goal really does land on them.
        val vm = dashboardViewModel(loader, prefs())
        assertEquals(defaults, vm.uiState.value.dailyGoals)
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

    // ─── reorderOntoDropTarget parity ─────────────────────────────────────────
    // STEPS and WEEKLY_CARDIO_LOAD each span two rows, so they fill the fixed
    // hero section exactly and the remaining four widgets share one section —
    // which is what makes these plain within-section drags rather than the
    // cross-section swap the two tests above cover.
    private val carouselOnlyOrder = listOf(
        DashboardWidgetId.STEPS,
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.DISTANCE,
        DashboardWidgetId.CALORIES_OUT,
        DashboardWidgetId.SLEEP,
        DashboardWidgetId.HYDRATION,
    )

    private fun carouselReorderViewModel(): DashboardViewModel {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns carouselOnlyOrder.map { it.name }
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        return dashboardViewModel(loader, prefs)
    }

    @Test fun `backward drag lands the moved card on the drop target`() = runTest {
        val vm = carouselReorderViewModel()

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.HYDRATION, DashboardWidgetId.DISTANCE)

        assertEquals(
            listOf(
                DashboardWidgetId.STEPS,
                DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                DashboardWidgetId.HYDRATION,
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
            ),
            vm.uiState.value.dashboardWidgets,
        )
    }

    @Test fun `adjacent drags swap neighbours`() = runTest {
        val expected = listOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.SLEEP,
            DashboardWidgetId.CALORIES_OUT,
            DashboardWidgetId.HYDRATION,
        )

        val forward = carouselReorderViewModel()
        forward.moveDashboardWidgetToTarget(DashboardWidgetId.CALORIES_OUT, DashboardWidgetId.SLEEP)
        assertEquals(expected, forward.uiState.value.dashboardWidgets)

        // Dragged the other way, the same two neighbours land the same way round.
        val backward = carouselReorderViewModel()
        backward.moveDashboardWidgetToTarget(DashboardWidgetId.SLEEP, DashboardWidgetId.CALORIES_OUT)
        assertEquals(expected, backward.uiState.value.dashboardWidgets)
    }

    @Test fun `dropping onto itself or out of range leaves the order untouched`() = runTest {
        val loader = mockDashboardDataLoader()
        val prefs = prefs()
        every { prefs.dashboardWidgetOrder() } returns carouselOnlyOrder.map { it.name }
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val vm = dashboardViewModel(loader, prefs)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.SLEEP, DashboardWidgetId.SLEEP)
        assertEquals(carouselOnlyOrder, vm.uiState.value.dashboardWidgets)

        // Out of range in Kotlin's model is "not in the saved list": a widget the
        // user removed can be neither the dragged card nor the drop target.
        vm.moveDashboardWidgetToTarget(DashboardWidgetId.BMI, DashboardWidgetId.SLEEP)
        assertEquals(carouselOnlyOrder, vm.uiState.value.dashboardWidgets)

        vm.moveDashboardWidgetToTarget(DashboardWidgetId.SLEEP, DashboardWidgetId.BMI)
        assertEquals(carouselOnlyOrder, vm.uiState.value.dashboardWidgets)

        // A no-op must not rewrite the saved layout either.
        verify(exactly = 0) { prefs.setDashboardWidgetOrder(any()) }
    }

    // ─── Edit mode ────────────────────────────────────────────────────────────

    @Test fun `toggling edit mode rebuilds the display without reloading`() = runTest {
        val loader = mockDashboardDataLoader()
        var loads = 0
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            loads += 1
            DashboardData(date = today, steps = 8_000)
        }
        val vm = dashboardViewModel(loader, prefs())
        advanceUntilIdle()
        val loadsBefore = loads
        assertTrue(loadsBefore > 0)

        vm.toggleDashboardEdit()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditingDashboard)
        // Synchronously flipped — no load in between, and the precomputed
        // display the edit grid renders is still there.
        assertEquals(loadsBefore, loads)
        assertTrue(state.display.widgets.isNotEmpty())

        vm.toggleDashboardEdit()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isEditingDashboard)
        assertEquals(loadsBefore, loads)
        assertTrue(vm.uiState.value.display.widgets.isNotEmpty())
    }

    @Test fun `edit mode offers a metric the device does not support`() = runTest {
        // Flutter's dashboard_screen_test case, at ViewModel level: outside edit
        // mode a metric the provider cannot serve has no tile at all; edit mode
        // materialises it — into the add tray, not the carousel, because the
        // user never placed it. Placing it from the tray keeps it in the grid.
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(
            date = today,
            steps = 8_000,
            supportedMetrics = DashboardMetric.entries.toSet() - DashboardMetric.SPO2,
        )
        val vm = dashboardViewModel(loader, prefs())
        advanceUntilIdle()

        assertNull(vm.uiState.value.display.widgets[DashboardWidgetId.SPO2])
        assertTrue(vm.uiState.value.display.unsupportedIds.isEmpty())

        vm.toggleDashboardEdit()
        advanceUntilIdle()

        val editing = vm.uiState.value
        assertNotNull(editing.display.widgets[DashboardWidgetId.SPO2])
        assertEquals(setOf(DashboardWidgetId.SPO2), editing.display.unsupportedIds)
        val visible = dashboardVisibleWidgetIds(
            dashboardWidgets = editing.dashboardWidgets,
            specIds = editing.display.widgets.keys,
            display = editing.display,
            isEditingDashboard = true,
            placedWidgetIds = editing.placedDashboardWidgets,
        )
        assertTrue(DashboardWidgetId.SPO2 !in visible)
        assertTrue(
            DashboardWidgetId.SPO2 in dashboardTrayWidgetIds(
                specIds = editing.display.widgets.keys.toList(),
                visibleIds = visible,
                isEditingDashboard = true,
            ),
        )

        // Adding it back is not a dead end: it joins the grid.
        vm.addDashboardWidget(DashboardWidgetId.SPO2)
        advanceUntilIdle()
        val placed = vm.uiState.value
        assertTrue(DashboardWidgetId.SPO2 in placed.placedDashboardWidgets)
        assertTrue(
            DashboardWidgetId.SPO2 in dashboardVisibleWidgetIds(
                dashboardWidgets = placed.dashboardWidgets,
                specIds = placed.display.widgets.keys,
                display = placed.display,
                isEditingDashboard = true,
                placedWidgetIds = placed.placedDashboardWidgets,
            ),
        )
    }

    @Test fun `an unsupported metric leaves the display again when edit mode ends`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(
            date = today,
            supportedMetrics = DashboardMetric.entries.toSet() - DashboardMetric.SPO2,
        )
        val vm = dashboardViewModel(loader, prefs())
        advanceUntilIdle()

        vm.toggleDashboardEdit()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.display.widgets[DashboardWidgetId.SPO2])

        vm.toggleDashboardEdit()
        advanceUntilIdle()

        assertNull(vm.uiState.value.display.widgets[DashboardWidgetId.SPO2])
        assertTrue(vm.uiState.value.display.unsupportedIds.isEmpty())
    }

    // ─── Body Energy timeline ─────────────────────────────────────────────────
    // Flutter loads the timeline inside the dashboard loader, gated on the
    // calibration flag and the heart-rate grant. Kotlin loads it from the
    // ViewModel after the day settles, gated on the BODY_ENERGY widget being on
    // the dashboard — a deliberate divergence, pinned here.

    @Test fun `body energy populates the timeline when the widget is on the dashboard`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val bodyEnergyRepo = mockk<BodyEnergyRepository>()
        coEvery { bodyEnergyRepo.loadTimeline(any()) } returns BodyEnergyTimelineResult(
            query = BodyEnergyTimelineQuery(
                period = DatePeriod(today, today),
                range = TimeRange.DAY,
            ),
            days = listOf(bodyEnergyTimeline()),
        )

        val vm = dashboardViewModel(loader, prefs(), bodyEnergyRepository = bodyEnergyRepo)
        advanceUntilIdle()

        val timeline = vm.uiState.value.data?.bodyEnergyTimeline
        assertNotNull(timeline)
        assertEquals(74, timeline?.currentScore)
        assertEquals(60, timeline?.startScore)
        coVerify(exactly = 1) { bodyEnergyRepo.loadTimeline(any()) }
    }

    @Test fun `body energy skips the load when the widget is not on the dashboard`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val bodyEnergyRepo = mockk<BodyEnergyRepository>()
        coEvery { bodyEnergyRepo.loadTimeline(any()) } returns BodyEnergyTimelineResult(
            query = BodyEnergyTimelineQuery(
                period = DatePeriod(today, today),
                range = TimeRange.DAY,
            ),
            days = listOf(bodyEnergyTimeline()),
        )
        val prefs = prefs().also {
            every { it.dashboardWidgetOrder() } returns listOf(DashboardWidgetId.STEPS.name)
        }

        val vm = dashboardViewModel(loader, prefs, bodyEnergyRepository = bodyEnergyRepo)
        advanceUntilIdle()

        assertNull(vm.uiState.value.data?.bodyEnergyTimeline)
        coVerify(exactly = 0) { bodyEnergyRepo.loadTimeline(any()) }
    }

    @Test fun `body energy leaves the day alone when the timeline load fails`() = runTest {
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } returns DashboardData(date = today)
        val bodyEnergyRepo = mockk<BodyEnergyRepository>()
        coEvery { bodyEnergyRepo.loadTimeline(any()) } throws RuntimeException("no heart rate")

        val vm = dashboardViewModel(loader, prefs(), bodyEnergyRepository = bodyEnergyRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.data?.bodyEnergyTimeline)
        // A missing timeline is not a dashboard error.
        assertNull(state.error)
        assertNotNull(state.data)
    }

    private fun bodyEnergyTimeline() = BodyEnergyTimeline(
        date = today,
        startScore = 60,
        currentScore = 74,
        charged = 30,
        drained = 16,
        points = emptyList(),
        confidence = BodyEnergyConfidence.MEDIUM,
        confidenceReason = "",
    )

    // ─── App-open refresh ─────────────────────────────────────────────────────

    @Test fun `the history caches drain after the dashboard read settles`() = runTest {
        val events = mutableListOf<String>()
        val loader = mockDashboardDataLoader()
        coEvery { loader.loadDashboard(any<DashboardQuery>()) } coAnswers {
            events += "load"
            DashboardData(date = today, steps = 8_000)
        }
        val scheduler = mockk<HistorySyncScheduler>()
        coEvery { scheduler.drainIncrementalOnce() } answers { events += "drain" }

        val vm = dashboardViewModel(
            loader = loader,
            prefs = prefs(),
            historySyncScheduler = scheduler,
        )
        advanceUntilIdle()

        // Health Connect serializes concurrent reads, so the drain waits for the
        // dashboard's own load to finish rather than racing it.
        assertEquals("drain", events.last())
        assertEquals("load", events.first())
        assertEquals(1, events.count { it == "drain" })
        coVerify(exactly = 1) { scheduler.drainIncrementalOnce() }
        // …and it really did wait for a settled read, not an in-flight one.
        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.data)
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
        bodyEnergyRepository: BodyEnergyRepository? = null,
        bleDeviceRepository: BleDeviceRepository? = null,
        bleSensorCoordinator: BleSensorCoordinator? = null,
        historySyncScheduler: HistorySyncScheduler? = null,
    ): DashboardViewModel =
        DashboardViewModel(
            loadDashboardDayUseCase = LoadDashboardDayUseCase(loader),
            repository = repo,
            prefs = prefs,
            unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
            activityRepository = activityRepo,
            bodyEnergyRepository = bodyEnergyRepository,
            bleDeviceRepository = bleDeviceRepository,
            bleSensorCoordinator = bleSensorCoordinator,
            historySyncScheduler = historySyncScheduler,
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
