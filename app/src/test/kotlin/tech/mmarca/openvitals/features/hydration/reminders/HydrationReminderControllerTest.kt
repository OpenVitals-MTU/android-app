package tech.mmarca.openvitals.features.hydration.reminders

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.data.model.HydrationReminderConfig
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HydrationReminderControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val hydrationRepository = mockk<HydrationRepository>()
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

    private fun controller(): HydrationReminderController =
        HydrationReminderController(
            context = context,
            preferencesRepository = preferencesRepository,
            hydrationRepository = hydrationRepository,
            notificationService = notificationService,
            alarmManager = alarmManager,
            dispatcherProvider = mainDispatcherRule.dispatcherProvider,
        )
}
