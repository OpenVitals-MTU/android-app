package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HydrationRepositoryTest {

    private val hydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)

    @Test
    fun `DAY hydration uses raw full entries for selected day total`() = runTest {
        val date = LocalDate.of(2026, 6, 1)
        val entries = listOf(
            HydrationEntry(
                startTime = Instant.parse("2026-06-01T07:45:00Z"),
                endTime = Instant.parse("2026-06-01T07:46:00Z"),
                liters = 0.35,
                source = "test.source",
            ),
            HydrationEntry(
                startTime = Instant.parse("2026-06-01T13:10:00Z"),
                endTime = Instant.parse("2026-06-01T13:11:00Z"),
                liters = 0.50,
                source = "test.source",
            ),
        )
        val aggregate = listOf(DailyHydration(date = date, liters = 9.99))
        val hc = hc(entries = entries, dailyHydration = aggregate)

        val result = HydrationRepositoryImpl(hc).loadHydrationPeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = date)
        )

        assertEquals(entries, result.hydrationEntries)
        assertEquals(listOf(DailyHydration(date = date, liters = 0.85)), result.dailyHydration)
        coVerify(exactly = 0) { hc.readDailyHydration(date, date) }
    }

    private fun hc(
        entries: List<HydrationEntry>,
        dailyHydration: List<DailyHydration> = emptyList(),
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
            coEvery { hc.grantedPermissions() } returns setOf(hydrationPermission)
            coEvery { hc.readHydrationEntries(any(), any()) } returns entries
            coEvery { hc.readDailyHydration(any(), any()) } returns dailyHydration
        }
}
