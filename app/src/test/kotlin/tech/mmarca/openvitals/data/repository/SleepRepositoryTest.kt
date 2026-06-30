package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepReadData
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class SleepRepositoryTest {

    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `loadSleepSessions merges split sessions before filtering by ending day`() = runTest {
        val zone = ZoneId.systemDefault()
        val day = LocalDate.of(2026, 5, 6)
        val beforeMidnight = sleep(
            id = "before-midnight",
            start = day.minusDays(1).atTime(LocalTime.of(22, 45)).atZone(zone).toInstant(),
            end = day.minusDays(1).atTime(LocalTime.of(23, 59)).atZone(zone).toInstant(),
        )
        val afterMidnight = sleep(
            id = "after-midnight",
            start = day.atTime(LocalTime.of(0, 3)).atZone(zone).toInstant(),
            end = day.atTime(LocalTime.of(6, 50)).atZone(zone).toInstant(),
        )
        val hc = hc(grantedPermissions = setOf(sleepPermission))
        coEvery { hc.readSleepSessions(any(), any()) } returns listOf(afterMidnight, beforeMidnight)
        val repository = SleepRepositoryImpl(hc)

        val sessions = repository.loadSleepSessions(day, day)

        assertEquals(1, sessions.size)
        assertEquals(beforeMidnight.startTime, sessions.single().startTime)
        assertEquals(afterMidnight.endTime, sessions.single().endTime)
        assertEquals(beforeMidnight.durationMs + afterMidnight.durationMs, sessions.single().durationMs)
    }

    @Test fun `loadSleepPeriod includes Health Connect aggregate sleep durations`() = runTest {
        val day = LocalDate.of(2026, 5, 6)
        val aggregateDurationMs = Duration.ofHours(8).toMillis()
        val hc = hc(grantedPermissions = setOf(sleepPermission))
        coEvery { hc.readSleepData(any(), any(), any()) } returns SleepReadData(
            dailyAggregateDurations = listOf(
                DailySleepDuration(
                    date = day,
                    durationMs = aggregateDurationMs,
                )
            ),
        )
        val repository = SleepRepositoryImpl(hc)

        val periodData = repository.loadSleepPeriod(
            query = PeriodLoadQuery(range = TimeRange.DAY, anchorDate = day),
            sleepRangeMode = SleepRangeMode.EVENING_18H,
        )

        assertEquals(aggregateDurationMs, periodData.dailyDurations.single().durationMs)
    }

    private fun hc(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String>,
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns availability
            coEvery { hc.grantedPermissions() } returns grantedPermissions
        }

    private fun sleep(
        id: String,
        start: Instant,
        end: Instant,
        source: String = "gadgetbridge",
    ) = SleepData(
        id = id,
        startTime = start,
        endTime = end,
        durationMs = Duration.between(start, end).toMillis(),
        source = source,
    )
}
