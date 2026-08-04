package tech.mmarca.openvitals.core.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.features.hydration.reminders.ChannelId as HydrationChannelId
import tech.mmarca.openvitals.features.hydration.reminders.NotificationId as HydrationNotificationId
import tech.mmarca.openvitals.features.mindfulness.reminders.ChannelId as MindfulnessChannelId
import tech.mmarca.openvitals.features.mindfulness.reminders.NotificationId as MindfulnessNotificationId

/**
 * Ported from the Flutter `test/core/reminders/reminder_notification_spec_test.dart`.
 * Every reminder feature registered in the app is listed here, so the uniqueness
 * guards below cover it — a new feature adds its ids to these lists.
 */
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
