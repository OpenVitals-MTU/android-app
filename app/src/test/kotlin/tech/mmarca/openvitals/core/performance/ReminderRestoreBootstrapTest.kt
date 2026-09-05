package tech.mmarca.openvitals.core.performance

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.HydrationReminderController
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController

/**
 * Foregrounding re-plans both reminder schedules. A reminder firing without `POST_NOTIFICATIONS`
 * cancels its own alarm and never reschedules, and Android auto-revokes that permission.
 * Both calls must happen even when the first throws.
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
        // A force-stop or auto-revoke can happen between any two foregrounds, so this is not once per process.
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
