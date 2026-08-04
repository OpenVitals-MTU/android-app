package tech.mmarca.openvitals.navigation

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedDayArgTest {

    @Test fun `today is omitted so the ordinary location stays clean`() {
        assertEquals("calories", "calories".withSelectedDay(LocalDate.now()))
    }

    @Test fun `a past day is appended as a query parameter`() {
        val day = LocalDate.now().minusDays(2)
        assertEquals("calories?day=$day", "calories".withSelectedDay(day))
    }

    @Test fun `joins an existing query string with an ampersand`() {
        val day = LocalDate.now().minusDays(1)
        assertEquals("metric/STEPS?x=1&day=$day", "metric/STEPS?x=1".withSelectedDay(day))
    }

    @Test fun `parses the day from the saved state handle`() {
        val handle = SavedStateHandle(mapOf(SELECTED_DAY_ARG to "2026-07-13"))

        assertEquals(LocalDate.of(2026, 7, 13), handle.selectedDayOrNull())
    }

    @Test fun `absent or malformed values read as no pin`() {
        assertNull(SavedStateHandle().selectedDayOrNull())
        assertNull(SavedStateHandle(mapOf(SELECTED_DAY_ARG to "not-a-date")).selectedDayOrNull())
    }
}
