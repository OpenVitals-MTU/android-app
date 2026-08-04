package tech.mmarca.openvitals.core.reminders

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderAlarmManager
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderNotificationService
import tech.mmarca.openvitals.util.MainDispatcherRule

/**
 * Ported from the Flutter `test/core/reminders/reminder_controller_test.dart`.
 * Kotlin has no shared reminder controller — the per-feature controllers carry
 * the same rules — so the hydration one stands in for the shared Dart type.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val hydrationRepository = mockk<HydrationRepository>()
    private val nutritionRepository = mockk<NutritionRepository>(relaxed = true)
    private val notificationService = mockk<HydrationReminderNotificationService>(relaxed = true)
    private val alarmManager = mockk<HydrationReminderAlarmManager>(relaxed = true)

    @Test
    fun `goal progress - a zero or absent target is never met, however much is logged`() = runTest {
        // No goal set: the reminder still fires, however much has been drunk.
        val config = HydrationReminderConfig(
            enabled = true,
            activeStartTime = LocalTime.MIDNIGHT,
            activeEndTime = LocalTime.MIDNIGHT,
        )
        every { preferencesRepository.hydrationReminderConfig() } returns config
        every { preferencesRepository.hydrationDailyGoalLiters } returns 0.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns listOf(
            DailyHydration(LocalDate.now(), 99.0)
        )
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()
        val controller = controller()

        controller.handleReminderAlarm()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify { notificationService.showHydrationReminder(99.0, 0.0) }
    }

    @Test
    fun `apply - missing notification permission clears, even when enabled`() = runTest {
        // POST_NOTIFICATIONS is refused: an enabled reminder is cleared rather
        // than armed for a notification that could never be posted.
        mockkObject(HydrationReminderController.Companion)
        every { HydrationReminderController.hasNotificationPermission(any()) } returns false
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns emptyList()
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()
        val controller = controller()

        controller.applyConfig(HydrationReminderConfig(enabled = true))
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { alarmManager.cancel() }
        verify { notificationService.cancelReminderNotification() }
        verify(exactly = 0) { alarmManager.schedule(any()) }
        unmockkObject(HydrationReminderController.Companion)
    }

    @Test
    fun `restoreSchedule re-plans an enabled reminder`() = runTest {
        every { preferencesRepository.hydrationReminderConfig() } returns
            HydrationReminderConfig(enabled = true)
        every { preferencesRepository.hydrationDailyGoalLiters } returns 2.0
        coEvery { hydrationRepository.loadDailyHydration(any(), any()) } returns emptyList()
        coEvery { hydrationRepository.loadHydrationEntries(any(), any()) } returns emptyList()
        val controller = controller()

        controller.restoreSchedule()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { alarmManager.schedule(any()) }
        verify(exactly = 0) { alarmManager.cancel() }
    }

    @Test
    fun `restoreSchedule clears a disabled one`() = runTest {
        every { preferencesRepository.hydrationReminderConfig() } returns
            HydrationReminderConfig(enabled = false)
        val controller = controller()

        controller.restoreSchedule()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { alarmManager.cancel() }
        verify { notificationService.cancelReminderNotification() }
        verify(exactly = 0) { alarmManager.schedule(any()) }
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
