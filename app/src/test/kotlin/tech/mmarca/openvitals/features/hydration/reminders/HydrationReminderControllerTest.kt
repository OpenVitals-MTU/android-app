package tech.mmarca.openvitals.features.hydration.reminders

import android.content.Context
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationReminderControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val hydrationRepository = mockk<HydrationRepository>()
    private val nutritionRepository = mockk<NutritionRepository>(relaxed = true)
    private val notificationService = mockk<HydrationReminderNotificationService>(relaxed = true)
    private val alarmManager = mockk<HydrationReminderAlarmManager>(relaxed = true)

    @Test fun `disabled config clears alarm and notification`() = runTest {
        val controller = controller()

        controller.applyConfig(HydrationReminderConfig(enabled = false))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { alarmManager.cancel() }
        verify { notificationService.cancelReminderNotification() }
        verify(exactly = 0) { alarmManager.schedule(any()) }
    }

    @Test fun `enabled config schedules next reminder`() = runTest {
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns listOf(
            DailyHydration(LocalDate.now(), 1.0)
        )
        val controller = controller()

        controller.applyConfig(HydrationReminderConfig(enabled = true))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { alarmManager.schedule(any()) }
        verify(exactly = 0) { notificationService.showHydrationReminder(any(), any()) }
    }

    @Test fun `the anchor read spans back into yesterday, not just today`() = runTest {
        // A drink at 23:50 must still anchor the schedule after midnight.
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns emptyList()
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()
        val controller = controller()

        controller.applyConfig(HydrationReminderConfig(enabled = true))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val today = LocalDate.now()
        coVerify { hydrationRepository.loadHydrationEntries(today.minusDays(1), today) }
    }

    @Test fun `an intake read failure counts as zero and still schedules`() = runTest {
        // The user still gets reminded when Health Connect cannot be read. Log has no JVM body.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery {
            hydrationRepository.loadDailyHydration(any(), any())
        } throws RuntimeException("Health Connect is gone")
        coEvery {
            hydrationRepository.loadHydrationEntries(any(), any())
        } throws RuntimeException("Health Connect is gone")
        val controller = controller()

        controller.applyConfig(HydrationReminderConfig(enabled = true))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { alarmManager.schedule(any()) }
        unmockkStatic(Log::class)
    }

    @Test fun `logging a drink re-anchors and reschedules`() = runTest {
        // The entry screen re-applies the persisted config after a save.
        every { preferencesRepository.hydrationReminderConfig() } returns
            HydrationReminderConfig(enabled = true)
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns listOf(
            DailyHydration(LocalDate.now(), 1.0)
        )
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()
        val controller = controller()

        controller.applyConfig()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { alarmManager.schedule(any()) }
    }

    @Test fun `alarm trigger shows notification when goal is not met and active hours allow it`() = runTest {
        val config = HydrationReminderConfig(
            enabled = true,
            activeStartTime = LocalTime.MIDNIGHT,
            activeEndTime = LocalTime.MIDNIGHT,
        )
        every { preferencesRepository.hydrationReminderConfig() } returns config
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns listOf(
            DailyHydration(LocalDate.now(), 1.0)
        )
        val controller = controller()

        controller.handleReminderAlarm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { notificationService.showHydrationReminder(1.0, 2.0) }
        verify { alarmManager.schedule(any()) }
    }

    @Test fun `alarm trigger does not notify after goal is met`() = runTest {
        val config = HydrationReminderConfig(
            enabled = true,
            activeStartTime = LocalTime.MIDNIGHT,
            activeEndTime = LocalTime.MIDNIGHT,
        )
        every { preferencesRepository.hydrationReminderConfig() } returns config
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns listOf(
            DailyHydration(LocalDate.now(), 2.0)
        )
        val controller = controller()

        controller.handleReminderAlarm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { notificationService.showHydrationReminder(any(), any()) }
        verify { alarmManager.schedule(any()) }
    }

    @Test fun `quick add logs water and reschedules after a real write`() = runTest {
        every { preferencesRepository.hydrationReminderConfig() } returns
            HydrationReminderConfig(enabled = true)
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        every { hydrationRepository.setLastCustomHydrationAmountMilliliters(any()) } returns Unit
        every { hydrationRepository.recordRecentHydrationAmountMilliliters(any()) } returns Unit
        coEvery { hydrationRepository.hasHydrationWritePermission() } returns true
        coEvery { hydrationRepository.writeHydrationEntry(any()) } returns "client-id"
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns emptyList()
        val controller = controller()

        controller.handleQuickAdd(350.0)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { hydrationRepository.setLastCustomHydrationAmountMilliliters(350.0) }
        verify { hydrationRepository.recordRecentHydrationAmountMilliliters(350.0) }
        coVerify { hydrationRepository.writeHydrationEntry(match { it.volumeLiters == 0.35 }) }
        verify { notificationService.cancelReminderNotification() }
        verify { alarmManager.schedule(any()) }
    }

    @Test fun `quick add refused by missing permission does not reschedule`() = runTest {
        every { hydrationRepository.setLastCustomHydrationAmountMilliliters(any()) } returns Unit
        every { hydrationRepository.recordRecentHydrationAmountMilliliters(any()) } returns Unit
        coEvery { hydrationRepository.hasHydrationWritePermission() } returns false
        val controller = controller()

        controller.handleQuickAdd(350.0)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // The size is still remembered, but a refused write must not re-anchor the countdown.
        verify { hydrationRepository.recordRecentHydrationAmountMilliliters(350.0) }
        coVerify(exactly = 0) { hydrationRepository.writeHydrationEntry(any()) }
        verify(exactly = 0) { alarmManager.schedule(any()) }
    }

    @Test fun `a failing re-anchor never fails the logged drink`() = runTest {
        // The re-anchor logs and swallows; Log has no JVM body.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { hydrationRepository.setLastCustomHydrationAmountMilliliters(any()) } returns Unit
        every { hydrationRepository.recordRecentHydrationAmountMilliliters(any()) } returns Unit
        coEvery { hydrationRepository.hasHydrationWritePermission() } returns true
        coEvery { hydrationRepository.writeHydrationEntry(any()) } returns "client-id"
        every { preferencesRepository.hydrationReminderConfig() } throws
            IllegalStateException("no scheduler here")
        val controller = controller()

        controller.handleQuickAdd(350.0)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // The drink landed; the reminder is a nicety that must not undo it.
        coVerify(exactly = 1) { hydrationRepository.writeHydrationEntry(any()) }
        unmockkStatic(Log::class)
    }

    @Test fun `quick add ignores invalid volumes`() = runTest {
        val controller = controller()

        controller.handleQuickAdd(0.0)
        controller.handleQuickAdd(Double.NaN)
        // A stale schedule can carry a volume no container could hold.
        controller.handleQuickAdd(-250.0)
        controller.handleQuickAdd(1_000_000.0)
        controller.handleQuickAdd(Double.POSITIVE_INFINITY)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { hydrationRepository.recordRecentHydrationAmountMilliliters(any()) }
        verify(exactly = 0) { alarmManager.schedule(any()) }
    }

    @Test fun `concurrent applies serialize instead of interleaving`() = runTest {
        // Both applies suspend on the same reads. Interleaved, whichever finishes last arms its alarm.
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()

        var inFlight = 0
        var overlapped = false
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } coAnswers {
            inFlight++
            if (inFlight > 1) overlapped = true
            // A real suspension: `yield()` is a no-op on an empty unconfined queue.
            delay(10)
            inFlight--
            listOf(DailyHydration(LocalDate.now(), 0.5))
        }

        val scheduled = mutableListOf<ZonedDateTime>()
        every { alarmManager.schedule(capture(scheduled)) } returns true

        val controller = controller()
        controller.applyConfig(HydrationReminderConfig(enabled = true, intervalMinutes = 240))
        controller.applyConfig(HydrationReminderConfig(enabled = true, intervalMinutes = 30))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("an apply ran while another was mid-read", overlapped)
        // The 30-minute config was applied last, so it is the one left armed.
        assertEquals(2, scheduled.size)
        assertTrue(
            "the later, shorter interval must win",
            scheduled.last().isBefore(scheduled.first()),
        )
    }

    private fun controller(): HydrationReminderController =
        HydrationReminderController(
            context = context,
            preferencesRepository = preferencesRepository,
            hydrationRepository = hydrationRepository,
            nutritionRepository = nutritionRepository,
            notificationService = notificationService,
            alarmManager = alarmManager,
            dispatcherProvider = mainDispatcherRule.dispatcherProvider,
        )
}
