package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.testing.FakeHealthConnectClient
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.DailySleepDuration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepReadData
import tech.mmarca.openvitals.domain.model.mergeSleepSessions
import tech.mmarca.openvitals.domain.model.mergedSleepSessionComponentIds
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.healthconnect.AggregatingFakeHealthConnectClient
import tech.mmarca.openvitals.healthconnect.HealthConnectDiagnostics
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectRateLimitBackoff
import tech.mmarca.openvitals.healthconnect.HealthConnectReaderSupport
import tech.mmarca.openvitals.healthconnect.SleepHealthReader

class SleepRepositoryTest {

    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        HealthConnectRateLimitBackoff.resetForTest()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        HealthConnectRateLimitBackoff.resetForTest()
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
            sleepWindow = SleepWindow.Default,
        )

        assertEquals(aggregateDurationMs, periodData.dailyDurations.single().durationMs)
    }

    @Test fun `loadDailySleepDurations passes the range through without a session fetch`() = runTest {
        val day = LocalDate.of(2026, 5, 6)
        val durations = listOf(DailySleepDuration(date = day, durationMs = Duration.ofHours(7).toMillis()))
        val hc = hc(grantedPermissions = setOf(sleepPermission))
        coEvery { hc.readDailySleepDurations(any(), any(), any()) } returns durations
        val repository = SleepRepositoryImpl(hc)

        val result = repository.loadDailySleepDurations(day.minusDays(30), day, SleepWindow.Default)

        assertEquals(durations, result)
        coVerify(exactly = 1) { hc.readDailySleepDurations(day.minusDays(30), day, SleepWindow.Default) }
        coVerify(exactly = 0) { hc.readSleepData(any(), any(), any()) }
        coVerify(exactly = 0) { hc.readSleepSessions(any(), any()) }
    }

    @Test fun `loadDailySleepDurations returns empty without reading when the permission is missing`() = runTest {
        val hc = hc(grantedPermissions = emptySet())
        val repository = SleepRepositoryImpl(hc)

        val result = repository.loadDailySleepDurations(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 6),
            SleepWindow.Default,
        )

        assertEquals(emptyList<DailySleepDuration>(), result)
        coVerify(exactly = 0) { hc.readDailySleepDurations(any(), any(), any()) }
    }

    private fun hc(
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        grantedPermissions: Set<String>,
    ): HealthConnectManager =
        mockk<HealthConnectManager>().also { hc ->
            every { hc.availability() } returns availability
            coEvery { hc.grantedPermissions() } returns grantedPermissions
        }

    @Test fun `loadSleepSession reads a plain record id straight through`() = runTest {
        val record = sleep(
            id = "s1",
            start = Instant.parse("2026-07-20T23:00:00Z"),
            end = Instant.parse("2026-07-21T07:00:00Z"),
        )
        val hc = hc(grantedPermissions = setOf(sleepPermission))
        coEvery { hc.readSleepSession("s1") } returns record

        val loaded = SleepRepositoryImpl(hc).loadSleepSession("s1")

        assertEquals("s1", loaded?.id)
        coVerify(exactly = 1) { hc.readSleepSession("s1") }
    }

    /**
     * A merged night is reconstructed by reading each component record back by
     * id (SleepHealthReader.readSleepSession). When Health Connect no longer
     * holds them — the user deleted the night from another app while the detail
     * screen was open — the lookup must answer not-found rather than a partial
     * or crashed session.
     *
     * Dart counterpart: sleep_repository_impl_test.dart, "is not-found when
     * every component record has since vanished". Flutter's repository does the
     * reconstruction itself; here it lives one layer down, so the mocked manager
     * delegates to the real reader over an empty Health Connect.
     */
    @Test fun `loadSleepSession is not-found when every component record has since vanished`() =
        runTest {
            val first = sleep(
                id = "gone-1",
                start = Instant.parse("2026-07-20T22:30:00Z"),
                end = Instant.parse("2026-07-21T02:00:00Z"),
            )
            val second = sleep(
                id = "gone-2",
                start = Instant.parse("2026-07-21T02:30:00Z"),
                end = Instant.parse("2026-07-21T07:00:00Z"),
            )
            val mergedId = mergeSleepSessions(listOf(first, second)).single().id
            assertEquals(listOf("gone-1", "gone-2"), mergedSleepSessionComponentIds(mergedId))

            // Health Connect holds nothing: both component records are gone.
            val reader = SleepHealthReader(
                HealthConnectReaderSupport(
                    clientProvider = { AggregatingFakeHealthConnectClient(FakeHealthConnectClient()) },
                    diagnostics = mockk<HealthConnectDiagnostics>().also {
                        every { it.summary() } returns "test"
                    },
                    rateLimitMessage = { "rate limited" },
                ),
            )
            val hc = hc(grantedPermissions = setOf(sleepPermission))
            coEvery { hc.readSleepSession(any<String>()) } coAnswers {
                reader.readSleepSession(firstArg<String>())
            }

            val loaded = SleepRepositoryImpl(hc).loadSleepSession(mergedId)

            assertNull(loaded)
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
