package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DailyHydration

class HydrationHealthReaderTest {

    @Test fun `daily hydration prefers precise record totals over aggregate buckets`() {
        val date = LocalDate.of(2026, 6, 11)
        val recordTotal = 0.35 + 0.35 + 0.2

        val result = hydrationByDateForDailySeries(
            recordHydrationByDate = mapOf(date to recordTotal),
            aggregateBuckets = listOf(DailyHydration(date, 1.0)),
        )

        assertEquals(0.9, result.getValue(date), 0.0001)
    }

    @Test fun `daily hydration falls back to aggregate buckets without positive record totals`() {
        val date = LocalDate.of(2026, 6, 11)

        val result = hydrationByDateForDailySeries(
            recordHydrationByDate = emptyMap(),
            aggregateBuckets = listOf(DailyHydration(date, 1.0)),
        )

        assertEquals(1.0, result.getValue(date), 0.0001)
    }
}
