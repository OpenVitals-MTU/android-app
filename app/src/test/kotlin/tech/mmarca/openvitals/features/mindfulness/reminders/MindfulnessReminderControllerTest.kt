package tech.mmarca.openvitals.features.mindfulness.reminders

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class MindfulnessReminderControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val mindfulnessRepository = mockk<MindfulnessRepository>()
    private val notificationService = mockk<MindfulnessReminderNotificationService>(relaxed = true)
    private val alarmManager = mockk<MindfulnessReminderAlarmManager>(relaxed = true)

    @Test fun `disabled config clears alarm and notification`() = runTest {
        val controller = controller()

        controller.applyConfig(MindfulnessReminderConfig(enabled = false))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { alarmManager.cancel() }
        verify { notificationService.cancelReminderNotification() }
        verify(exactly = 0) { alarmManager.schedule(any()) }
    }

    @Test fun `enabled config schedules next reminder`() = runTest {
        every { preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES) } returns 10.0
        coEvery { mindfulnessRepository.loadMindfulnessSessions(any(), any()) } returns listOf(
            mindfulnessSession(durationMinutes = 5)
        )
        val controller = controller()

        controller.applyConfig(MindfulnessReminderConfig(enabled = true))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { alarmManager.schedule(any()) }
        verify(exactly = 0) { notificationService.showMindfulnessReminder(any(), any()) }
    }

    @Test fun `alarm trigger shows notification when goal is not met`() = runTest {
        every { preferencesRepository.mindfulnessReminderConfig() } returns MindfulnessReminderConfig(
            enabled = true,
            reminderTime = LocalTime.MIDNIGHT,
        )
        every { preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES) } returns 10.0
        coEvery { mindfulnessRepository.loadMindfulnessSessions(any(), any()) } returns listOf(
            mindfulnessSession(durationMinutes = 5)
        )
        val controller = controller()

        controller.handleReminderAlarm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { notificationService.showMindfulnessReminder(5.0, 10.0) }
        verify { alarmManager.schedule(any()) }
    }

    @Test fun `alarm trigger does not notify after goal is met`() = runTest {
        every { preferencesRepository.mindfulnessReminderConfig() } returns MindfulnessReminderConfig(
            enabled = true,
            reminderTime = LocalTime.MIDNIGHT,
        )
        every { preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES) } returns 10.0
        coEvery { mindfulnessRepository.loadMindfulnessSessions(any(), any()) } returns listOf(
            mindfulnessSession(durationMinutes = 10)
        )
        val controller = controller()

        controller.handleReminderAlarm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { notificationService.showMindfulnessReminder(any(), any()) }
        verify { alarmManager.schedule(any()) }
    }

    @Test fun `concurrent applies serialize instead of interleaving`() = runTest {
        // The hydration twin already holds a mutex across "read today's progress"
        // and "arm the alarm" for exactly this reason. Both applies suspend on
        // the same repository read between choosing a config and arming, so
        // interleaved they race: whichever arms last owns the single
        // ReminderRequestCode PendingIntent, and that is as likely to be the
        // time the user just moved away from as the one they settled on.
        every { preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES) } returns 10.0

        var inFlight = 0
        var overlapped = false
        coEvery { mindfulnessRepository.loadMindfulnessSessions(any(), any()) } coAnswers {
            inFlight++
            if (inFlight > 1) overlapped = true
            // A real suspension: `yield()` is a no-op on an unconfined
            // dispatcher with an empty queue, so it would never expose the race.
            delay(10)
            inFlight--
            listOf(mindfulnessSession(durationMinutes = 5))
        }

        val scheduled = mutableListOf<ZonedDateTime>()
        every { alarmManager.schedule(capture(scheduled)) } returns true

        val controller = controller()
        controller.applyConfig(
            MindfulnessReminderConfig(enabled = true, reminderTime = LocalTime.of(8, 0)),
        )
        controller.applyConfig(
            MindfulnessReminderConfig(enabled = true, reminderTime = LocalTime.of(9, 0)),
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse("an apply ran while another was mid-read", overlapped)
        assertEquals(2, scheduled.size)
        assertEquals(
            "the later config must be the one left armed",
            LocalTime.of(9, 0),
            scheduled.last().toLocalTime(),
        )
    }

    private fun controller(): MindfulnessReminderController =
        MindfulnessReminderController(
            context = context,
            preferencesRepository = preferencesRepository,
            mindfulnessRepository = mindfulnessRepository,
            notificationService = notificationService,
            alarmManager = alarmManager,
            dispatcherProvider = mainDispatcherRule.dispatcherProvider,
        )

    private fun mindfulnessSession(durationMinutes: Long): MindfulnessSession {
        val end = Instant.now()
        return MindfulnessSession(
            id = durationMinutes.toString(),
            title = null,
            startTime = end.minusSeconds(durationMinutes * 60),
            endTime = end,
            durationMs = durationMinutes * 60_000,
            source = "test",
        )
    }
}
