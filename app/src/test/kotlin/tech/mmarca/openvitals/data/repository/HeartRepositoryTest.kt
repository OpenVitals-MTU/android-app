package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HeartRepositoryTest {

    private val hrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    @Test fun `DAY HRV uses daily summaries and skips previous day raw HRV`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val query = PeriodLoadQuery(
            range = TimeRange.DAY,
            anchorDate = date,
            today = date,
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(hrvPermission)
        coEvery { hc.readHrvRmssd(any()) } returns 99.0
        coEvery { hc.readDailyHRV(any(), any()) } answers {
            val start = firstArg<LocalDate>()
            val end = secondArg<LocalDate>()
            if (start == date && end == date) {
                listOf(DailyHrv(date, 44.0))
            } else {
                emptyList()
            }
        }

        val data = HeartRepository(hc).loadHeartPeriod(query, HeartPeriodMetric.HRV)

        assertEquals(44.0, data.dayHrvMs ?: 0.0, 0.01)
        assertNull(data.previousDayHrvMs)
        coVerify(exactly = 1) { hc.readDailyHRV(date, date) }
        coVerify(exactly = 1) { hc.readDailyHRV(query.windows.baseline.start, query.windows.baseline.end) }
        coVerify(exactly = 0) { hc.readHrvRmssd(any()) }
    }
}
