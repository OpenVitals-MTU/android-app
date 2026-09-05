package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectChanges
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class CaloriesHistorySyncServiceTest {

    private val today = LocalDate.now()
    private val caloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun hc(): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        coEvery { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns setOf(caloriesPermission)
        coEvery { hc.getChangesToken(any()) } returns "token-1"
        coEvery { hc.readDailyNutrition(any(), any(), any(), any(), any()) } returns emptyList()
        return hc
    }

    private fun dao(cursorToken: String? = null): VitalsDailyCacheDao {
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(VitalsCacheKeys.CALORIES_BURNED) } answers {
            cursorToken?.let { VitalsSyncCursorEntity(VitalsCacheKeys.CALORIES_BURNED, it, null) }
        }
        coEvery { dao.purgeMetric(any()) } just Runs
        coEvery { dao.replaceMetric(any(), any()) } just Runs
        coEvery { dao.writeFullSync(any()) } just Runs
        coEvery { dao.writeToken(any(), any()) } just Runs
        coEvery { dao.upsertDay(any()) } just Runs
        coEvery { dao.deleteDay(any(), any()) } just Runs
        return dao
    }

    @Test fun `full sync purges legacy keys and stores only positive-burn days`() = runTest {
        val hc = hc()
        coEvery { hc.readDailyNutrition(any(), any(), any(), any(), any()) } answers {
            val start = firstArg<LocalDate>()
            if (start == today.minusDays(364)) {
                listOf(
                    DailyNutrition(date = today.minusDays(1), hydrationLiters = 0.0, caloriesBurnedKcal = 2100.0),
                    DailyNutrition(date = today.minusDays(2), hydrationLiters = 0.0, caloriesBurnedKcal = 0.0),
                )
            } else {
                emptyList()
            }
        }
        val dao = dao(cursorToken = null)
        val replaced = slot<List<VitalsDailyAggregateEntity>>()
        coEvery { dao.replaceMetric(VitalsCacheKeys.CALORIES_BURNED, capture(replaced)) } just Runs

        CaloriesHistorySyncService(hc, dao).syncAll()

        coVerify { dao.purgeMetric("totalCaloriesBurned") }
        val row = replaced.captured.single()
        assertEquals(today.minusDays(1).toEpochDay(), row.epochDay)
        assertEquals(2100.0, row.valueSum, 0.0001)
        assertEquals(1L, row.sampleCount)
        coVerify {
            dao.writeFullSync(
                match { it.metric == VitalsCacheKeys.CALORIES_BURNED && it.changesToken == "token-1" },
            )
        }
    }

    @Test fun `duplicate dates from a DST-clipped tail bucket sum instead of colliding`() = runTest {
        val hc = hc()
        val day = today.minusDays(5)
        coEvery { hc.readDailyNutrition(any(), any(), any(), any(), any()) } answers {
            if (firstArg<LocalDate>() == today.minusDays(364)) {
                listOf(
                    DailyNutrition(date = day, hydrationLiters = 0.0, caloriesBurnedKcal = 1800.0),
                    DailyNutrition(date = day, hydrationLiters = 0.0, caloriesBurnedKcal = 150.0),
                )
            } else {
                emptyList()
            }
        }
        val dao = dao(cursorToken = null)
        val replaced = slot<List<VitalsDailyAggregateEntity>>()
        coEvery { dao.replaceMetric(VitalsCacheKeys.CALORIES_BURNED, capture(replaced)) } just Runs

        CaloriesHistorySyncService(hc, dao).syncAll()

        assertEquals(1950.0, replaced.captured.single().valueSum, 0.0001)
    }

    @Test fun `expired token abandons the delta and rebuilds`() = runTest {
        val hc = hc()
        coEvery { hc.getChanges("old-token") } returns HealthConnectChanges(
            upsertedDays = emptyList(),
            hasDeletions = false,
            nextToken = "next",
            tokenExpired = true,
            hasMore = false,
        )
        val dao = dao(cursorToken = "old-token")

        CaloriesHistorySyncService(hc, dao).syncIncremental()

        coVerify { dao.replaceMetric(VitalsCacheKeys.CALORIES_BURNED, any()) }
    }

    @Test fun `incremental recompute deletes a day that dropped to zero burn`() = runTest {
        val hc = hc()
        val day = today.minusDays(1)
        coEvery { hc.getChanges("old-token") } returns HealthConnectChanges(
            upsertedDays = listOf(day),
            hasDeletions = false,
            nextToken = "new-token",
            tokenExpired = false,
            hasMore = false,
        )
        val dao = dao(cursorToken = "old-token")

        CaloriesHistorySyncService(hc, dao).syncIncremental()

        coVerify { dao.deleteDay(VitalsCacheKeys.CALORIES_BURNED, day.toEpochDay()) }
        coVerify { dao.writeToken(VitalsCacheKeys.CALORIES_BURNED, "new-token") }
    }

    /** A first full sync covers the whole [HistoryLookbackDays] lookback, newest-first, in chunks of at most a year. */
    @Test fun `full sync backfills the whole lookback window in bounded newest-first chunks`() = runTest {
        val hc = hc()
        val readRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
        coEvery { hc.readDailyNutrition(any(), any(), any(), any(), any()) } answers {
            readRanges += firstArg<LocalDate>() to secondArg<LocalDate>()
            emptyList()
        }
        val dao = dao(cursorToken = null)

        CaloriesHistorySyncService(hc, dao).syncAll()

        val earliest = today.minusDays(HistoryLookbackDays)
        // Newest chunk first, ending today; oldest chunk starts at the lookback edge.
        assertEquals(today, readRanges.first().second)
        assertEquals(earliest, readRanges.last().first)
        readRanges.forEach { (start, end) ->
            assertTrue("chunk $start..$end inverted", !end.isBefore(start))
            assertTrue(
                "chunk $start..$end exceeds a year",
                java.time.temporal.ChronoUnit.DAYS.between(start, end) < 365,
            )
        }
        // Chunks tile the window: each older chunk ends the day before its newer neighbour starts.
        readRanges.zipWithNext().forEach { (newer, older) ->
            assertEquals(newer.first.minusDays(1), older.second)
        }
    }

    @Test fun `ungranted permission is a no-op`() = runTest {
        val hc = hc()
        coEvery { hc.grantedPermissions() } returns emptySet()
        val dao = dao(cursorToken = null)

        CaloriesHistorySyncService(hc, dao).syncAll()

        coVerify(exactly = 0) { dao.replaceMetric(any(), any()) }
        coVerify(exactly = 0) { hc.readDailyNutrition(any(), any(), any(), any(), any()) }
    }
}
