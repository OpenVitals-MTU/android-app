package tech.mmarca.openvitals.core.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.ChannelId as HydrationChannelId
import tech.mmarca.openvitals.features.hydration.reminders.NotificationId as HydrationNotificationId
import tech.mmarca.openvitals.features.mindfulness.reminders.ChannelId as MindfulnessChannelId
import tech.mmarca.openvitals.features.mindfulness.reminders.NotificationId as MindfulnessNotificationId

/** Every reminder feature is listed here so the uniqueness guards cover it. */
class ReminderNotificationSpecTest {

    private val allNotificationIds = listOf(HydrationNotificationId, MindfulnessNotificationId)
    private val allChannelIds = listOf(HydrationChannelId, MindfulnessChannelId)

    @Test
    fun `base notification ids are unique across reminder features`() {
        assertEquals(allNotificationIds.size, allNotificationIds.toSet().size)
    }

    @Test
    fun `channel ids are unique across reminder features`() {
        assertEquals(allChannelIds.size, allChannelIds.toSet().size)
    }
}
