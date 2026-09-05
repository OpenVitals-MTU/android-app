package tech.mmarca.openvitals.di

import org.junit.Assert.assertNotEquals
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.NotificationId as HydrationNotificationId
import tech.mmarca.openvitals.features.mindfulness.reminders.NotificationId as MindfulnessNotificationId

/** Sharing an alarm id would make one feature's cancel wipe the other's pending reminder. */
class ReminderSchedulerWiringTest {

    @Test
    fun `the two reminders use distinct notification id ranges`() {
        assertNotEquals(HydrationNotificationId, MindfulnessNotificationId)
    }
}
