package tech.mmarca.openvitals.data.repository

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HrvSample
import tech.mmarca.openvitals.domain.model.RestingHeartRateSample
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class HeartRepositoryTest {

    private val heartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val restingHeartRatePermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val hrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)

    @Test fun `instant range includes samples from a heart rate series starting before the workout`() = runTest {
        val start = Instant.parse("2026-07-11T08:03:00Z")
        val end = Instant.parse("2026-07-11T08:35:00Z")
        val beforeWorkout = HeartRateSample(start.minusSeconds(1), 90L, "gadgetbridge")
        val firstWorkoutSample = HeartRateSample(start, 120L, "gadgetbridge")
        val laterWorkoutSample = HeartRateSample(start.plusSeconds(12 * 60), 150L, "gadgetbridge")
        val afterWorkout = HeartRateSample(end, 100L, "gadgetbridge")
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(heartRatePermission)
        coEvery { hc.readRawHeartRateSamples(any(), any()) } returns listOf(
            afterWorkout,
            laterWorkoutSample,
            beforeWorkout,
            firstWorkoutSample,
        )

        val samples = HeartRepositoryImpl(hc).loadHeartRateSamples(start, end)

        assertEquals(listOf(firstWorkoutSample, laterWorkoutSample), samples)
        coVerify(exactly = 1) {
            hc.readRawHeartRateSamples(start.minus(Duration.ofHours(1)), end)
        }
        coVerify(exactly = 0) { hc.readHeartRateSamples(any(), any()) }
    }

    @Test fun `DAY average heart rate uses raw full samples for selected day graph`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val rawSamples = listOf(
            HeartRateSample(Instant.parse("2026-06-27T00:00:00Z"), 70L, "sensor"),
            HeartRateSample(Instant.parse("2026-06-27T00:00:30Z"), 72L, "sensor"),
            HeartRateSample(Instant.parse("2026-06-27T00:01:00Z"), 74L, "sensor"),
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(heartRatePermission)
        coEvery { hc.readRawHeartRateSamples(any(), any()) } returns rawSamples
        coEvery { hc.readHeartRateSamples(any(), any()) } returns emptyList()
        coEvery { hc.readDailyHeartRateSummaries(any(), any()) } returns emptyList()

        val data = HeartRepositoryImpl(hc).loadHeartPeriod(
            query = PeriodLoadQuery(
                range = TimeRange.DAY,
                anchorDate = date,
                today = date,
            ),
            metric = HeartPeriodMetric.AVERAGE_HEART_RATE,
        )

        assertEquals(rawSamples, data.daySamples)
        coVerify(exactly = 1) { hc.readRawHeartRateSamples(start, end) }
        coVerify(exactly = 0) { hc.readHeartRateSamples(start, end) }
    }

    @Test fun `WEEK average heart rate uses daily aggregate summaries without raw day samples`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val query = PeriodLoadQuery(
            range = TimeRange.WEEK,
            anchorDate = date,
            today = date,
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(heartRatePermission)
        coEvery { hc.readDailyHeartRateSummaries(any(), any()) } returns emptyList()

        HeartRepositoryImpl(hc).loadHeartPeriod(query, HeartPeriodMetric.AVERAGE_HEART_RATE)

        coVerify(exactly = 1) {
            hc.readDailyHeartRateSummaries(query.windows.current.start, query.windows.current.end)
        }
        coVerify(exactly = 0) { hc.readRawHeartRateSamples(any(), any()) }
        coVerify(exactly = 0) { hc.readHeartRateSamples(any(), any()) }
    }

    @Test fun `DAY resting heart rate uses raw full samples for selected day graph`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val rawSamples = listOf(
            RestingHeartRateSample(Instant.parse("2026-06-27T00:00:00Z"), 60L, "sensor"),
            RestingHeartRateSample(Instant.parse("2026-06-27T03:00:00Z"), 62L, "sensor"),
        )
        val query = PeriodLoadQuery(
            range = TimeRange.DAY,
            anchorDate = date,
            today = date,
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(restingHeartRatePermission)
        coEvery { hc.readRestingHeartRateSamples(any(), any()) } returns rawSamples
        coEvery { hc.readRestingHeartRate(any()) } returns null
        coEvery { hc.readDailyRestingHR(any(), any()) } returns emptyList()

        val data = HeartRepositoryImpl(hc).loadHeartPeriod(query, HeartPeriodMetric.RESTING_HEART_RATE)

        assertEquals(rawSamples, data.dayRestingSamples)
        assertEquals(61L, data.dayRestingBpm)
        coVerify(exactly = 1) { hc.readRestingHeartRateSamples(start, end) }
        coVerify(exactly = 0) { hc.readRestingHeartRate(date) }
    }

    @Test fun `DAY HRV uses raw full samples for selected day graph`() = runTest {
        val date = LocalDate.of(2026, 6, 27)
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val rawSamples = listOf(
            HrvSample(Instant.parse("2026-06-27T00:00:00Z"), 44.0, "sensor"),
            HrvSample(Instant.parse("2026-06-27T00:05:00Z"), 46.0, "sensor"),
        )
        val query = PeriodLoadQuery(
            range = TimeRange.DAY,
            anchorDate = date,
            today = date,
        )
        val hc = mockk<HealthConnectManager>()
        every { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(hrvPermission)
        coEvery { hc.readHrvSamples(any(), any()) } returns rawSamples
        coEvery { hc.readDailyHRV(any(), any()) } returns emptyList()
        coEvery { hc.readHrvRmssd(any()) } returns 99.0

        val data = HeartRepositoryImpl(hc).loadHeartPeriod(query, HeartPeriodMetric.HRV)

        assertEquals(rawSamples, data.dayHrvSamples)
        assertEquals(45.0, data.dayHrvMs ?: 0.0, 0.01)
        assertNull(data.previousDayHrvMs)
        coVerify(exactly = 1) { hc.readHrvSamples(start, end) }
        coVerify(exactly = 0) { hc.readDailyHRV(date, date) }
        coVerify(exactly = 1) { hc.readDailyHRV(query.windows.baseline.start, query.windows.baseline.end) }
        coVerify(exactly = 0) { hc.readHrvRmssd(any()) }
    }
}
