package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.OxygenSaturationRecord
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectChanges
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class VitalsHistorySyncServiceTest {

    private val today = LocalDate.now()
    private val spO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)

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

    private fun hc(granted: Set<String> = setOf(spO2Permission)): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        coEvery { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns granted
        coEvery { hc.isSkinTemperatureAvailable() } returns false
        coEvery { hc.getChangesToken(any()) } returns "token-1"
        coEvery { hc.readDailyBloodPressure(any(), any()) } returns emptyList()
        coEvery { hc.readDailySpO2(any(), any()) } returns emptyList()
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } returns emptyList()
        coEvery { hc.readDailyBodyTemperature(any(), any()) } returns emptyList()
        coEvery { hc.readDailyVo2Max(any(), any()) } returns emptyList()
        coEvery { hc.readDailyBloodGlucose(any(), any()) } returns emptyList()
        coEvery { hc.readDailySkinTemperature(any(), any()) } returns emptyList()
        return hc
    }

    private fun dao(cursorToken: String? = null): VitalsDailyCacheDao {
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } answers {
            cursorToken?.let { VitalsSyncCursorEntity(firstArg(), it, null) }
        }
        coEvery { dao.replaceMetric(any(), any()) } just Runs
        coEvery { dao.writeFullSync(any()) } just Runs
        coEvery { dao.writeToken(any(), any()) } just Runs
        coEvery { dao.upsertDay(any()) } just Runs
        coEvery { dao.deleteDay(any(), any()) } just Runs
        return dao
    }

    @Test fun `first sync registers the token before the read and replaces the metric`() = runTest {
        val hc = hc()
        val point = DailyVitalPoint(date = today.minusDays(2), value = 97.0, count = 4)
        coEvery { hc.readDailySpO2(any(), any()) } returns listOf(point)
        val dao = dao(cursorToken = null)
        val written = slot<List<VitalsDailyAggregateEntity>>()
        coEvery { dao.replaceMetric(VitalsCacheKeys.SPO2, capture(written)) } just Runs

        VitalsHistorySyncService(hc, dao).syncAll()

        val row = written.captured.single()
        assertEquals(today.minusDays(2).toEpochDay(), row.epochDay)
        assertEquals(97.0 * 4, row.valueSum, 0.0001)
        assertEquals(4L, row.sampleCount)
        assertNull(row.secondarySum)
        coVerify { dao.writeFullSync(match { it.metric == VitalsCacheKeys.SPO2 && it.changesToken == "token-1" }) }
    }

    @Test fun `incremental sync recomputes only the upserted days`() = runTest {
        val hc = hc()
        val day = today.minusDays(1)
        coEvery { hc.getChanges("old-token") } returns HealthConnectChanges(
            upsertedDays = listOf(day),
            hasDeletions = false,
            nextToken = "new-token",
            tokenExpired = false,
            hasMore = false,
        )
        coEvery { hc.readDailySpO2(any(), any()) } returns listOf(DailyVitalPoint(day, 96.0, 2))
        val dao = dao(cursorToken = "old-token")

        VitalsHistorySyncService(hc, dao).syncIncremental()

        coVerify { dao.upsertDay(match { it.metric == VitalsCacheKeys.SPO2 && it.epochDay == day.toEpochDay() }) }
        coVerify { dao.writeToken(VitalsCacheKeys.SPO2, "new-token") }
        coVerify(exactly = 0) { dao.replaceMetric(any(), any()) }
    }

    @Test fun `a day whose recompute comes back empty is deleted`() = runTest {
        val hc = hc()
        val day = today.minusDays(3)
        coEvery { hc.getChanges("old-token") } returns HealthConnectChanges(
            upsertedDays = listOf(day),
            hasDeletions = false,
            nextToken = "new-token",
            tokenExpired = false,
            hasMore = false,
        )
        val dao = dao(cursorToken = "old-token")

        VitalsHistorySyncService(hc, dao).syncIncremental()

        coVerify { dao.deleteDay(VitalsCacheKeys.SPO2, day.toEpochDay()) }
    }

    @Test fun `deletions force a full rebuild because they carry no date`() = runTest {
        val hc = hc()
        coEvery { hc.getChanges("old-token") } returns HealthConnectChanges(
            upsertedDays = emptyList(),
            hasDeletions = true,
            nextToken = "new-token",
            tokenExpired = false,
            hasMore = false,
        )
        val dao = dao(cursorToken = "old-token")

        VitalsHistorySyncService(hc, dao).syncIncremental()

        coVerify { dao.replaceMetric(VitalsCacheKeys.SPO2, any()) }
        coVerify { dao.writeFullSync(match { it.metric == VitalsCacheKeys.SPO2 }) }
    }

    @Test fun `incremental-only sync never pays for a first full sync`() = runTest {
        val hc = hc()
        val dao = dao(cursorToken = null)

        VitalsHistorySyncService(hc, dao).syncIncremental()

        coVerify(exactly = 0) { dao.replaceMetric(any(), any()) }
        coVerify(exactly = 0) { hc.getChangesToken(any()) }
    }

    @Test fun `ungranted metric is skipped entirely`() = runTest {
        val hc = hc(granted = emptySet())
        val dao = dao(cursorToken = null)

        VitalsHistorySyncService(hc, dao).syncAll()

        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
        coVerify(exactly = 0) { dao.replaceMetric(any(), any()) }
    }

    @Test fun `patchDays is a no-op without a cursor`() = runTest {
        val hc = hc()
        val dao = dao(cursorToken = null)

        VitalsHistorySyncService(hc, dao).patchDays(VitalsCacheKeys.SPO2, setOf(today))

        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
        coVerify(exactly = 0) { dao.upsertDay(any()) }
    }

    @Test fun `patchDays recomputes the given day when a cursor exists`() = runTest {
        val hc = hc()
        coEvery { hc.readDailySpO2(any(), any()) } returns listOf(DailyVitalPoint(today, 95.0, 1))
        val dao = dao(cursorToken = "token")

        VitalsHistorySyncService(hc, dao).patchDays(VitalsCacheKeys.SPO2, setOf(today))

        coVerify { dao.upsertDay(match { it.epochDay == today.toEpochDay() && it.valueSum == 95.0 }) }
    }
}
