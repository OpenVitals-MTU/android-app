package tech.mmarca.openvitals.core.reminders

import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.features.hydration.reminders.isWithinHydrationReminderActiveHours

/** `IntervalWindowReminderSchedule.isWithinActiveHours`. */
class ReminderScheduleTest {

    @Test
    fun `an equal start and end means always active`() {
        val config = HydrationReminderConfig(
            intervalMinutes = 120,
            activeStartTime = LocalTime.of(0, 0),
            activeEndTime = LocalTime.of(0, 0),
        )

        assertTrue(isWithinHydrationReminderActiveHours(LocalTime.of(3, 0), config))
    }
}
