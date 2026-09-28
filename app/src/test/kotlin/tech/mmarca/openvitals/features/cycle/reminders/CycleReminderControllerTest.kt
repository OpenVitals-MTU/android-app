package tech.mmarca.openvitals.features.cycle.reminders

import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.model.PillPlan
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * The controller plans from the stored config and the recorded history. A
 * read it cannot trust must never cancel alarms that were right.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleReminderControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val preferences = FakePreferences()
    private val cycleRepository = mockk<CycleRepository>()
    private val journal = FakeCycleJournalRepository()
    private val pillIntakes = FakePillIntakeRepository()
    private val hc = mockk<HealthConnectManager>()
    private val notificationService = mockk<CycleReminderNotificationService>(relaxed = true)
    private val alarmManager = mockk<CycleReminderAlarmManager>(relaxed = true)
    private val today = LocalDate.now()
    private val all = CycleReminderConfig(
        enabled = true,
        dailyCheckInEnabled = true,
        dailyCheckInTime = LocalTime.of(21, 0),
        periodWindowEnabled = true,
        lateCycleEnabled = true,
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        coEvery { hc.readsOtherAppsDataNow() } returns true
        coEvery { cycleRepository.loadDayLog(any()) } answers { CycleDayLog(date = firstArg()) }
        coEvery { cycleRepository.loadCycleStatistics(any()) } returns CycleStatistics(estimate = futureEstimate())
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `disabled cancels every alarm and notification`() = runTest {
        controller().applyConfig(all.copy(enabled = false))
        advance()

        CycleReminderType.entries.forEach { verify { alarmManager.cancel(it) } }
        verify { notificationService.cancelCycleReminders() }
        verify(exactly = 0) { alarmManager.schedule(any(), any()) }
    }

    @Test
    fun `the pill alarm follows its own switch, not the master`() = runTest {
        preferences.setPillPlan(PillPlan(enabled = true, packStart = today.minusDays(3)))

        controller().applyConfig(all.copy(enabled = false))
        advance()

        verify { alarmManager.schedule(CycleReminderType.PILL, any()) }
        verify { alarmManager.cancel(CycleReminderType.DAILY_CHECK_IN) }
    }

    @Test
    fun `the pill alarm shows the reminder on a taking day that is not taken`() = runTest {
        preferences.setPillPlan(PillPlan(enabled = true, packStart = today.minusDays(3)))

        controller().handleReminderAlarm(CycleReminderType.PILL)
        advance()

        verify { notificationService.show(CycleReminderType.PILL, any(), any()) }
    }

    @Test
    fun `the pill alarm stays quiet once today is taken and re-plans for tomorrow`() = runTest {
        preferences.setPillPlan(PillPlan(enabled = true, packStart = today.minusDays(3)))
        pillIntakes.setTaken(today, true)

        controller().handleReminderAlarm(CycleReminderType.PILL)
        advance()

        verify(exactly = 0) { notificationService.show(CycleReminderType.PILL, any(), any()) }
        verify { alarmManager.schedule(CycleReminderType.PILL, match { it.toLocalDate() == today.plusDays(1) }) }
    }

    @Test
    fun `a pause day never shows the pill reminder`() = runTest {
        preferences.setPillPlan(PillPlan(enabled = true, activeDays = 21, pauseDays = 7, packStart = today.minusDays(22)))

        controller().handleReminderAlarm(CycleReminderType.PILL)
        advance()

        verify(exactly = 0) { notificationService.show(CycleReminderType.PILL, any(), any()) }
    }

    @Test
    fun `the Taken action marks today and dismisses the notification`() = runTest {
        preferences.setPillPlan(PillPlan(enabled = true, packStart = today.minusDays(3)))

        controller().markPillTaken(today)
        advance()

        org.junit.Assert.assertTrue(pillIntakes.isTaken(today))
        verify { notificationService.cancel(CycleReminderType.PILL) }
    }

    @Test
    fun `an available estimate arms the daily, window and late alarms`() = runTest {
        controller().applyConfig(all)
        advance()

        verify { alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, any()) }
        verify { alarmManager.schedule(CycleReminderType.PERIOD_WINDOW, any()) }
        verify { alarmManager.schedule(CycleReminderType.LATE_CYCLE, any()) }
    }

    @Test
    fun `too little history cancels the estimate alarms and keeps the daily one`() = runTest {
        coEvery { cycleRepository.loadCycleStatistics(any()) } returns CycleStatistics(estimate = CycleEstimateResult.NeedsMoreHistory)

        controller().applyConfig(all)
        advance()

        verify { alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, any()) }
        verify { alarmManager.cancel(CycleReminderType.PERIOD_WINDOW) }
        verify { alarmManager.cancel(CycleReminderType.LATE_CYCLE) }
    }

    @Test
    fun `a failed read leaves the estimate alarms as they are`() = runTest {
        coEvery { cycleRepository.loadCycleStatistics(any()) } throws IllegalStateException("rate limited")

        controller().applyConfig(all)
        advance()

        verify { alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, any()) }
        verify(exactly = 0) { alarmManager.cancel(CycleReminderType.PERIOD_WINDOW) }
        verify(exactly = 0) { alarmManager.cancel(CycleReminderType.LATE_CYCLE) }
        verify(exactly = 0) { alarmManager.schedule(CycleReminderType.PERIOD_WINDOW, any()) }
    }

    @Test
    fun `without the background grant the estimate is not read at all`() = runTest {
        // Health Connect would answer with this app's records only, so a period logged elsewhere would read as late.
        coEvery { hc.readsOtherAppsDataNow() } returns false

        controller().applyConfig(all)
        advance()

        coVerify(exactly = 0) { cycleRepository.loadCycleStatistics(any()) }
        verify(exactly = 0) { alarmManager.cancel(CycleReminderType.PERIOD_WINDOW) }
        verify(exactly = 0) { alarmManager.schedule(CycleReminderType.PERIOD_WINDOW, any()) }
        verify { alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, any()) }
    }

    @Test
    fun `a switched-off alarm is cancelled without touching the others`() = runTest {
        controller().applyConfig(all.copy(periodWindowEnabled = false))
        advance()

        verify { alarmManager.cancel(CycleReminderType.PERIOD_WINDOW) }
        verify { alarmManager.schedule(CycleReminderType.LATE_CYCLE, any()) }
    }

    @Test
    fun `a logged day moves the daily reminder to tomorrow`() = runTest {
        coEvery { cycleRepository.loadDayLog(any()) } answers { CycleDayLog(date = firstArg(), ownFlow = ownFlow()) }
        val at = slot<ZonedDateTime>()

        controller().applyConfig(all.copy(periodWindowEnabled = false, lateCycleEnabled = false))
        advance()

        verify { alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, capture(at)) }
        assertEquals(today.plusDays(1), at.captured.toLocalDate())
    }

    @Test
    fun `the daily alarm shows nothing once the day is logged`() = runTest {
        preferences.setCycleReminderConfig(all)
        coEvery { cycleRepository.loadDayLog(any()) } answers { CycleDayLog(date = firstArg(), ownFlow = ownFlow()) }

        controller().handleReminderAlarm(CycleReminderType.DAILY_CHECK_IN)
        advance()

        verify(exactly = 0) { notificationService.show(any(), any()) }
    }

    @Test
    fun `the daily alarm shows the reminder on an unlogged day`() = runTest {
        preferences.setCycleReminderConfig(all)

        controller().handleReminderAlarm(CycleReminderType.DAILY_CHECK_IN)
        advance()

        verify { notificationService.show(CycleReminderType.DAILY_CHECK_IN, any()) }
    }

    @Test
    fun `the test reminder posts the daily one at once`() = runTest {
        controller().showTestReminder()
        advance()

        verify { notificationService.show(CycleReminderType.DAILY_CHECK_IN, any()) }
        verify(exactly = 0) { alarmManager.schedule(any(), any()) }
    }

    private fun controller() = CycleReminderController(
        context = context,
        preferences = preferences,
        cycleRepository = cycleRepository,
        journal = journal,
        pillIntakes = pillIntakes,
        hc = hc,
        notificationService = notificationService,
        alarmManager = alarmManager,
        dispatcherProvider = mainDispatcherRule.dispatcherProvider,
    )

    private fun advance() = mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

    private fun futureEstimate() = CycleEstimateResult.Available(
        CycleEstimate(
            earliestDate = today.plusDays(10),
            centralDate = today.plusDays(12),
            latestDate = today.plusDays(14),
            cycleCount = 3,
            variabilityDays = 2,
        ),
    )

    private fun ownFlow() = MenstruationFlowEntry(
        time = today.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(),
        flow = CycleRecordValues.FLOW_MEDIUM,
        source = "OpenVitals",
        id = "own",
        isOpenVitalsEntry = true,
    )
}
