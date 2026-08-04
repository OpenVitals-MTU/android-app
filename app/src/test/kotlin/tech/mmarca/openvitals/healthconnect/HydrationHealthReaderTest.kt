package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HydrationHealthReaderTest {

    @Test fun `daily hydration series keeps aggregate totals by date`() {
        val start = LocalDate.of(2026, 6, 10)
        val end = LocalDate.of(2026, 6, 12)

        val result = dailyHydrationSeries(
            startDate = start,
            endDate = end,
            hydrationByDate = mapOf(
                LocalDate.of(2026, 6, 10) to 0.5,
                LocalDate.of(2026, 6, 12) to 1.0,
            ),
        )

        assertEquals(0.5, result[0].liters, 0.0001)
        assertEquals(0.0, result[1].liters, 0.0001)
        assertEquals(1.0, result[2].liters, 0.0001)
    }
}
