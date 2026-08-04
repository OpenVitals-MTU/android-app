package tech.mmarca.openvitals.core.performance

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController

/**
 * Bringing the app to the foreground re-plans both reminder schedules.
 *
 * The failure this recovers from is a permanent silence the app inflicts on
 * itself: a reminder that fires without `POST_NOTIFICATIONS` cancels its own
 * alarm and returns before rescheduling, and since Android 12 that permission
 * is auto-revoked for unused apps by default. Re-granting notifications in
 * system settings arms nothing, and the toggle still reads "on".
 *
 * Both calls have to happen even when the first one throws — a hydration
 * controller failing must not be why mindfulness reminders stayed dead.
 */
class ReminderRestoreBootstrapTest {

    private val hydration = mockk<HydrationReminderController>(relaxed = true)
    private val mindfulness = mockk<MindfulnessReminderController>(relaxed = true)

    @Test
    fun `coming to the foreground restores both schedules`() {
        bootstrap().onStart(mockk(relaxed = true))

        verify(exactly = 1) { hydration.restoreSchedule(any()) }
        verify(exactly = 1) { mindfulness.restoreSchedule(any()) }
    }

    @Test
    fun `a controller that throws does not starve the other`() {
        every { hydration.restoreSchedule(any()) } throws IllegalStateException("no alarm manager")

        bootstrap().onStart(mockk(relaxed = true))

        verify(exactly = 1) { mindfulness.restoreSchedule(any()) }
    }

    @Test
    fun `every foreground restores again, because the alarm may have been lost since`() {
        // A force-stop, an OEM battery killer or an auto-revoke can happen at
        // any point between two foregrounds, so this is not a once-per-process
        // bootstrap.
        val bootstrap = bootstrap()
        val owner = mockk<androidx.lifecycle.LifecycleOwner>(relaxed = true)

        bootstrap.onStart(owner)
        bootstrap.onStart(owner)

        verify(exactly = 2) { hydration.restoreSchedule(any()) }
        verify(exactly = 2) { mindfulness.restoreSchedule(any()) }
    }

    private fun bootstrap() = ReminderRestoreBootstrap(
        hydrationReminderController = hydration,
        mindfulnessReminderController = mindfulness,
    )
}
