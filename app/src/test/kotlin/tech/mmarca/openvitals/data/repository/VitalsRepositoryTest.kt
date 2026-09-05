package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.data.sync.VitalsCacheKeys
import tech.mmarca.openvitals.data.sync.HistoryLookbackDays
import tech.mmarca.openvitals.data.sync.VitalsHistorySyncService
import tech.mmarca.openvitals.domain.model.DailyBloodPressurePoint
import tech.mmarca.openvitals.domain.model.DailyVitalPoint
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementEntry
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementWriteRequest
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

class VitalsRepositoryTest {

    private val today = LocalDate.of(2026, 6, 15)

    private val allVitalsPermissions = setOf(
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(BodyTemperatureRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
    )

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

    private fun hc(granted: Set<String> = allVitalsPermissions): HealthConnectManager {
        val hc = mockk<HealthConnectManager>()
        every { hc.phase3Permissions } returns allVitalsPermissions
        coEvery { hc.availability() } returns HealthConnectAvailability.AVAILABLE
        coEvery { hc.grantedPermissions() } returns granted
        coEvery { hc.isSkinTemperatureAvailable() } returns true
        coEvery { hc.readDailyBloodPressure(any(), any()) } returns emptyList()
        coEvery { hc.readDailySpO2(any(), any()) } returns emptyList()
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } returns emptyList()
        coEvery { hc.readDailyBodyTemperature(any(), any()) } returns emptyList()
        coEvery { hc.readDailyVo2Max(any(), any()) } returns emptyList()
        coEvery { hc.readDailyBloodGlucose(any(), any()) } returns emptyList()
        coEvery { hc.readDailySkinTemperature(any(), any()) } returns emptyList()
        coEvery { hc.readLatestBloodPressureInWindow(any(), any()) } returns null
        coEvery { hc.readLatestSpO2InWindow(any(), any()) } returns null
        coEvery { hc.readLatestRespiratoryRateInWindow(any(), any()) } returns null
        coEvery { hc.readLatestBodyTemperatureInWindow(any(), any()) } returns null
        coEvery { hc.readLatestVo2MaxInWindow(any(), any()) } returns null
        coEvery { hc.readLatestBloodGlucoseInWindow(any(), any()) } returns null
        coEvery { hc.readLatestSkinTemperatureInWindow(any(), any()) } returns null
        return hc
    }

    private fun weekQuery() = PeriodLoadQuery(
        range = TimeRange.WEEK,
        anchorDate = today,
        today = today,
    )

    @Test fun `non-day ALL load returns daily points and window latest instead of raw entries`() = runTest {
        val hc = hc()
        val point = DailyVitalPoint(date = today.minusDays(1), value = 97.5, count = 12)
        val latest = SpO2Entry(time = Instant.parse("2026-06-14T08:00:00Z"), percent = 98.0, source = "watch")
        coEvery { hc.readDailySpO2(any(), any()) } returns listOf(point)
        coEvery { hc.readLatestSpO2InWindow(any(), any()) } returns latest
        val repository = VitalsRepositoryImpl(hc)

        val result = repository.loadVitalsPeriod(weekQuery(), VitalsPeriodMetric.ALL)

        assertEquals(listOf(point), result.spO2Daily)
        assertEquals(latest, result.latestSpO2)
        assertTrue(result.spO2.isEmpty())
        assertTrue(result.timedOutMetrics.isEmpty())
        coVerify(exactly = 0) { hc.readSpO2Entries(any(), any()) }
    }

    @Test fun `daily read that blows its budget lands in timedOutMetrics and stays empty`() = runTest {
        val hc = hc()
        coEvery { hc.readDailySpO2(any(), any()) } coAnswers {
            delay(60_000)
            listOf(DailyVitalPoint(date = today, value = 97.0, count = 1))
        }
        val repository = VitalsRepositoryImpl(hc)

        val result = repository.loadVitalsPeriod(weekQuery(), VitalsPeriodMetric.ALL)

        assertEquals(setOf(VitalsPeriodMetric.SPO2), result.timedOutMetrics)
        assertTrue(result.spO2Daily.isEmpty())
    }

    @Test fun `slow metric does not block the others`() = runTest {
        val hc = hc()
        coEvery { hc.readDailySpO2(any(), any()) } coAnswers {
            delay(60_000)
            emptyList()
        }
        val point = DailyVitalPoint(date = today, value = 16.0, count = 4)
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } returns listOf(point)
        val repository = VitalsRepositoryImpl(hc)

        val result = repository.loadVitalsPeriod(weekQuery(), VitalsPeriodMetric.ALL)

        assertEquals(listOf(point), result.respiratoryRateDaily)
        assertEquals(setOf(VitalsPeriodMetric.SPO2), result.timedOutMetrics)
    }

    @Test fun `missing permission skips the daily and latest reads for that metric`() = runTest {
        val spO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
        val hc = hc(granted = allVitalsPermissions - spO2Permission)
        val repository = VitalsRepositoryImpl(hc)

        val result = repository.loadVitalsPeriod(weekQuery(), VitalsPeriodMetric.ALL)

        assertTrue(result.spO2Daily.isEmpty())
        assertEquals(setOf(spO2Permission), result.missingVitalsPermissions)
        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
        coVerify(exactly = 0) { hc.readLatestSpO2InWindow(any(), any()) }
    }

    @Test fun `cached daily points are served without hitting Health Connect`() = runTest {
        val hc = hc()
        val dao = mockk<tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } answers {
            tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity(firstArg(), "token", null)
        }
        coEvery { dao.aggregatesBetween(any(), any(), any()) } returns emptyList()
        coEvery {
            dao.aggregatesBetween(tech.mmarca.openvitals.data.sync.VitalsCacheKeys.SPO2, any(), any())
        } returns listOf(
            tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity(
                metric = tech.mmarca.openvitals.data.sync.VitalsCacheKeys.SPO2,
                epochDay = today.minusDays(1).toEpochDay(),
                valueSum = 97.0 * 4,
                secondarySum = null,
                sampleCount = 4,
            ),
        )
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao)

        val result = repository.loadVitalsPeriod(weekQuery(), VitalsPeriodMetric.ALL)

        val point = result.spO2Daily.single()
        assertEquals(today.minusDays(1), point.date)
        assertEquals(97.0, point.value, 0.0001)
        assertEquals(4, point.count)
        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
    }

    @Test fun `day ALL load keeps the raw entry path`() = runTest {
        val hc = hc()
        val entry = SpO2Entry(time = Instant.parse("2026-06-15T08:00:00Z"), percent = 98.0, source = "watch")
        coEvery { hc.readBloodPressureEntries(any(), any()) } returns emptyList()
        coEvery { hc.readSpO2Entries(any(), any()) } returns listOf(entry)
        coEvery { hc.readRespiratoryRateEntries(any(), any()) } returns emptyList()
        coEvery { hc.readBodyTemperatureEntries(any(), any()) } returns emptyList()
        coEvery { hc.readVo2MaxEntries(any(), any()) } returns emptyList()
        coEvery { hc.readBloodGlucoseEntries(any(), any()) } returns emptyList()
        coEvery { hc.readSkinTemperatureEntries(any(), any()) } returns emptyList()
        val repository = VitalsRepositoryImpl(hc)

        val result = repository.loadVitalsPeriod(
            PeriodLoadQuery(range = TimeRange.DAY, anchorDate = today, today = today),
            VitalsPeriodMetric.ALL,
        )

        assertEquals(listOf(entry), result.spO2)
        assertTrue(result.spO2Daily.isEmpty())
        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
    }

    // The range reads behind the health report.

    @Test fun `loadDailyVitals serves the cache when the sync cursor covers the range`() = runTest {
        val hc = hc()
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } answers { VitalsSyncCursorEntity(firstArg(), "token", null) }
        coEvery { dao.aggregatesBetween(VitalsCacheKeys.SPO2, any(), any()) } returns listOf(
            VitalsDailyAggregateEntity(
                metric = VitalsCacheKeys.SPO2,
                epochDay = LocalDate.now().minusDays(3).toEpochDay(),
                valueSum = 96.0 * 2,
                secondarySum = null,
                sampleCount = 2,
            ),
        )
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao)

        val points = repository.loadDailyVitals(
            VitalsPeriodMetric.SPO2,
            LocalDate.now().minusDays(30),
            LocalDate.now(),
        )

        assertEquals(96.0, points.single().value, 0.0001)
        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
    }

    @Test fun `loadDailyVitals falls through to a live read when the cursor is missing`() = runTest {
        val hc = hc()
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } returns null
        val live = DailyVitalPoint(date = LocalDate.now().minusDays(1), value = 15.5, count = 3)
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } returns listOf(live)
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao)

        val points = repository.loadDailyVitals(
            VitalsPeriodMetric.RESPIRATORY_RATE,
            LocalDate.now().minusDays(30),
            LocalDate.now(),
        )

        assertEquals(listOf(live), points)
        coVerify(exactly = 0) { dao.aggregatesBetween(any(), any(), any()) }
    }

    @Test fun `loadDailyVitals ignores the cache for ranges older than the sync lookback`() = runTest {
        val hc = hc()
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } answers { VitalsSyncCursorEntity(firstArg(), "token", null) }
        coEvery { hc.readDailySpO2(any(), any()) } returns emptyList()
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao)

        repository.loadDailyVitals(
            VitalsPeriodMetric.SPO2,
            LocalDate.now().minusDays(HistoryLookbackDays + 10),
            LocalDate.now(),
        )

        coVerify(exactly = 1) { hc.readDailySpO2(any(), any()) }
        coVerify(exactly = 0) { dao.aggregatesBetween(any(), any(), any()) }
    }

    @Test fun `loadDailyVitals returns empty without reading when the permission is missing`() = runTest {
        val spO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
        val hc = hc(granted = allVitalsPermissions - spO2Permission)
        val repository = VitalsRepositoryImpl(hc)

        val points = repository.loadDailyVitals(VitalsPeriodMetric.SPO2, today.minusDays(30), today)

        assertTrue(points.isEmpty())
        coVerify(exactly = 0) { hc.readDailySpO2(any(), any()) }
    }

    @Test fun `loadDailyVitals returns empty when the provider lacks skin temperature`() = runTest {
        val hc = hc()
        coEvery { hc.isSkinTemperatureAvailable() } returns false
        val repository = VitalsRepositoryImpl(hc)

        val points = repository.loadDailyVitals(VitalsPeriodMetric.SKIN_TEMPERATURE, today.minusDays(30), today)

        assertTrue(points.isEmpty())
        coVerify(exactly = 0) { hc.readDailySkinTemperature(any(), any()) }
    }

    @Test fun `loadDailyVitals rejects the pseudo metrics`() = runTest {
        val repository = VitalsRepositoryImpl(hc())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.loadDailyVitals(VitalsPeriodMetric.ALL, today.minusDays(7), today) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.loadDailyVitals(VitalsPeriodMetric.BLOOD_PRESSURE, today.minusDays(7), today) }
        }
    }

    @Test fun `loadDailyBloodPressure maps secondarySum into diastolic from the cache`() = runTest {
        val hc = hc()
        val dao = mockk<VitalsDailyCacheDao>()
        coEvery { dao.cursor(any()) } answers { VitalsSyncCursorEntity(firstArg(), "token", null) }
        coEvery { dao.aggregatesBetween(VitalsCacheKeys.BLOOD_PRESSURE, any(), any()) } returns listOf(
            VitalsDailyAggregateEntity(
                metric = VitalsCacheKeys.BLOOD_PRESSURE,
                epochDay = LocalDate.now().minusDays(2).toEpochDay(),
                valueSum = 120.0 * 2,
                secondarySum = 80.0 * 2,
                sampleCount = 2,
            ),
        )
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao)

        val points = repository.loadDailyBloodPressure(LocalDate.now().minusDays(30), LocalDate.now())

        val point = points.single()
        assertEquals(120.0, point.systolic, 0.0001)
        assertEquals(80.0, point.diastolic, 0.0001)
        coVerify(exactly = 0) { hc.readDailyBloodPressure(any(), any()) }
    }

    @Test fun `loadDailyBloodPressure returns empty without reading when the permission is missing`() = runTest {
        val bpPermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
        val hc = hc(granted = allVitalsPermissions - bpPermission)
        val repository = VitalsRepositoryImpl(hc)

        val points = repository.loadDailyBloodPressure(today.minusDays(30), today)

        assertTrue(points.isEmpty())
        coVerify(exactly = 0) { hc.readDailyBloodPressure(any(), any()) }
    }

    // Daily-cache write-through. The recompute lives in VitalsHistorySyncService.patchDays,
    // so the real service is wired behind the repository.

    private val anchor = LocalDate.of(2026, 7, 16)

    private val allVitalsWritePermissions = setOf(
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(RespiratoryRateRecord::class),
        HealthPermission.getWritePermission(BodyTemperatureRecord::class),
    )

    /** An in-memory stand-in for the Room DAO the cache patch writes through. */
    private class FakeVitalsCacheDao : VitalsDailyCacheDao {
        val rows = mutableMapOf<Pair<String, Long>, VitalsDailyAggregateEntity>()
        val cursors = mutableMapOf<String, VitalsSyncCursorEntity>()

        override suspend fun aggregatesBetween(
            metric: String,
            fromEpochDay: Long,
            toEpochDay: Long,
        ): List<VitalsDailyAggregateEntity> = rows.values
            .filter { it.metric == metric && it.epochDay in fromEpochDay..toEpochDay }
            .sortedBy { it.epochDay }

        override suspend fun upsertDay(row: VitalsDailyAggregateEntity) {
            rows[row.metric to row.epochDay] = row
        }

        override suspend fun deleteDay(metric: String, epochDay: Long) {
            rows.remove(metric to epochDay)
        }

        override suspend fun cursor(metric: String): VitalsSyncCursorEntity? = cursors[metric]

        override suspend fun writeFullSync(cursor: VitalsSyncCursorEntity) {
            cursors[cursor.metric] = cursor
        }

        override suspend fun deleteMetricRows(metric: String) {
            rows.keys.removeAll { it.first == metric }
        }

        override suspend fun insertRows(rows: List<VitalsDailyAggregateEntity>) {
            rows.forEach { upsertDay(it) }
        }

        override suspend fun deleteCursor(metric: String) {
            cursors.remove(metric)
        }

        override suspend fun updateToken(metric: String, token: String): Int {
            val existing = cursors[metric] ?: return 0
            cursors[metric] = existing.copy(changesToken = token)
            return 1
        }
    }

    private fun writeHc(granted: Set<String> = allVitalsPermissions + allVitalsWritePermissions) =
        hc(granted = granted).also { hc ->
            coEvery { hc.writeVitalsMeasurementEntry(any()) } returns "new-id"
            coEvery { hc.updateVitalsMeasurementEntry(any(), any()) } returns Unit
            coEvery { hc.deleteVitalsMeasurementEntry(any(), any()) } returns Unit
            coEvery { hc.readVitalsMeasurementEntry(any(), any()) } returns null
        }

    private fun req(
        type: VitalsMeasurementType,
        day: LocalDate,
        value: Double = 18.0,
        secondary: Double? = null,
    ) = VitalsMeasurementWriteRequest(
        type = type,
        time = day.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(),
        value = value,
        secondaryValue = secondary,
    )

    private fun Instant.localDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

    @Test fun `an edit across midnight recomputes both the old and new day`() = runTest {
        val oldDay = anchor.minusDays(1)
        val dao = FakeVitalsCacheDao()
        dao.writeFullSync(VitalsSyncCursorEntity(VitalsCacheKeys.RESPIRATORY_RATE, "tok", 0))
        // A stale cached mean on the old day the reading is moving away from.
        dao.upsertDay(
            VitalsDailyAggregateEntity(
                metric = VitalsCacheKeys.RESPIRATORY_RATE,
                epochDay = oldDay.toEpochDay(),
                valueSum = 999.0,
                secondarySum = null,
                sampleCount = 1,
            ),
        )
        val hc = writeHc()
        coEvery { hc.readVitalsMeasurementEntry(VitalsMeasurementType.RESPIRATORY_RATE, "e1") } returns
            VitalsMeasurementEntry(
                id = "e1",
                type = VitalsMeasurementType.RESPIRATORY_RATE,
                time = oldDay.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant(),
                value = 18.0,
                source = "tech.mmarca.openvitals",
                isOpenVitalsEntry = true,
            )
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } coAnswers {
            // The old day is empty once the reading moved off it; the new day has two.
            if (firstArg<Instant>().localDate() == anchor) {
                listOf(DailyVitalPoint(date = anchor, value = 12.0, count = 2))
            } else {
                emptyList()
            }
        }
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao, vitalsSync = VitalsHistorySyncService(hc, dao))

        repository.updateVitalsMeasurementEntry("e1", req(VitalsMeasurementType.RESPIRATORY_RATE, anchor))

        assertTrue(
            "the vacated old day is recomputed away",
            dao.aggregatesBetween(VitalsCacheKeys.RESPIRATORY_RATE, oldDay.toEpochDay(), oldDay.toEpochDay()).isEmpty(),
        )
        val newRow = dao
            .aggregatesBetween(VitalsCacheKeys.RESPIRATORY_RATE, anchor.toEpochDay(), anchor.toEpochDay())
            .single()
        assertEquals(24.0, newRow.valueSum, 0.0001) // 12 × 2
    }

    @Test fun `a blood-pressure write carries diastolic into secondarySum`() = runTest {
        val dao = FakeVitalsCacheDao()
        dao.writeFullSync(VitalsSyncCursorEntity(VitalsCacheKeys.BLOOD_PRESSURE, "tok", 0))
        val hc = writeHc()
        coEvery { hc.readDailyBloodPressure(any(), any()) } returns listOf(
            DailyBloodPressurePoint(date = anchor, systolic = 120.0, diastolic = 80.0, count = 2),
        )
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao, vitalsSync = VitalsHistorySyncService(hc, dao))

        repository.writeVitalsMeasurementEntry(
            req(VitalsMeasurementType.BLOOD_PRESSURE, anchor, value = 120.0, secondary = 80.0),
        )

        val row = dao
            .aggregatesBetween(VitalsCacheKeys.BLOOD_PRESSURE, anchor.toEpochDay(), anchor.toEpochDay())
            .single()
        assertEquals(240.0, row.valueSum, 0.0001) // 120 × 2
        assertEquals(160.0, row.secondarySum!!, 0.0001) // 80 × 2
        assertEquals(2L, row.sampleCount)
    }

    @Test fun `with no cache wired, a write still succeeds`() = runTest {
        val hc = writeHc()
        val repository = VitalsRepositoryImpl(hc)

        val id = repository.writeVitalsMeasurementEntry(req(VitalsMeasurementType.RESPIRATORY_RATE, anchor))

        assertEquals("new-id", id)
        coVerify(exactly = 1) { hc.writeVitalsMeasurementEntry(any()) }
    }

    @Test fun `a cache-patch failure never fails the write`() = runTest {
        val dao = FakeVitalsCacheDao()
        dao.writeFullSync(VitalsSyncCursorEntity(VitalsCacheKeys.RESPIRATORY_RATE, "tok", 0))
        val hc = writeHc()
        coEvery { hc.readDailyRespiratoryRate(any(), any()) } throws IllegalStateException("daily read failed")
        val repository = VitalsRepositoryImpl(hc, cacheDao = dao, vitalsSync = VitalsHistorySyncService(hc, dao))

        // The write succeeded; the drain will reconcile the cache.
        val id = repository.writeVitalsMeasurementEntry(req(VitalsMeasurementType.RESPIRATORY_RATE, anchor))

        assertEquals("new-id", id)
        assertTrue(
            dao.aggregatesBetween(VitalsCacheKeys.RESPIRATORY_RATE, anchor.toEpochDay(), anchor.toEpochDay()).isEmpty(),
        )
    }
}
