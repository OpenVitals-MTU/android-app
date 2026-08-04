package tech.mmarca.openvitals.di

import org.junit.Assert.assertNotEquals
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.NotificationId as HydrationNotificationId
import tech.mmarca.openvitals.features.mindfulness.reminders.NotificationId as MindfulnessNotificationId

/**
 * Ported from the Flutter `test/di/reminder_scheduler_wiring_test.dart`. Kotlin
 * arms one alarm per feature instead of a notification batch, but the portable
 * invariant holds: sharing an id would make one feature's cancel wipe the
 * other's pending reminder.
 */
class ReminderSchedulerWiringTest {

    @Test
    fun `the two reminders use distinct notification id ranges`() {
        assertNotEquals(HydrationNotificationId, MindfulnessNotificationId)
    }
}
