package tech.mmarca.openvitals.devices.garmin

import android.provider.CalendarContract
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The docs and the privacy policy say what never reaches the watch. The filter must agree. */
class GarminCalendarSourceTest {

    private val confirmed = CalendarContract.Instances.STATUS_CONFIRMED
    private val accepted = CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED

    @Test
    fun `a cancelled event is not sent`() {
        assertFalse(
            GarminCalendarSource.isOnTheWearersDay(CalendarContract.Instances.STATUS_CANCELED, accepted),
        )
    }

    @Test
    fun `a meeting the wearer declined is not sent`() {
        assertFalse(
            GarminCalendarSource.isOnTheWearersDay(confirmed, CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED),
        )
    }

    @Test
    fun `an accepted, a tentative, an unanswered and the wearer's own event are sent`() {
        for (answer in listOf(
            accepted,
            CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE,
            CalendarContract.Attendees.ATTENDEE_STATUS_INVITED,
            CalendarContract.Attendees.ATTENDEE_STATUS_NONE,
        )) {
            assertTrue(GarminCalendarSource.isOnTheWearersDay(confirmed, answer))
            assertTrue(GarminCalendarSource.isOnTheWearersDay(CalendarContract.Instances.STATUS_TENTATIVE, answer))
        }
    }
}
