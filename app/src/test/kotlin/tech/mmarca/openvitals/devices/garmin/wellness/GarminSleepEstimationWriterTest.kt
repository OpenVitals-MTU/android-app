package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.AppleHealthImportRepository
import tech.mmarca.openvitals.data.repository.contract.GarminSleepMinuteRepository
import tech.mmarca.openvitals.domain.model.GarminSleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/** One record per night, replaced as the night grows; the watch's own stages always win. */
class GarminSleepEstimationWriterTest {

    private val zone: ZoneOffset = ZoneOffset.ofHours(2)
    private val night: LocalDate = LocalDate.of(2024, 3, 5)
    private val bedtime: Instant = night.minusDays(1).atTime(22, 30).toInstant(zone)

    private val importRepository = mockk<AppleHealthImportRepository>(relaxed = true)
    private val minuteRepository = mockk<GarminSleepMinuteRepository>(relaxed = true)
    private val healthConnect = mockk<HealthConnectManager>(relaxed = true)
    private var now: Long = 1_000L
    private val writer = GarminSleepEstimationWriter(importRepository, minuteRepository, healthConnect) {
        Instant.ofEpochMilli(now)
    }

    private fun minute(index: Int, kind: SleepMinuteKind, movement: Double = 0.0, heartRate: Double = 52.0) =
        GarminSleepMinute(
            time = bedtime.plusSeconds(60L * index),
            kind = kind,
            heartRate = if (kind == SleepMinuteKind.RAW) heartRate else null,
            movement = if (kind == SleepMinuteKind.RAW) movement else null,
            activity = null,
            zoneOffset = zone,
            features = null,
        )

    /** Thirty restless minutes, then [stillMinutes] of sleep-shaped stillness, then thirty restless. */
    private fun nightMinutes(stillMinutes: Int): List<GarminSleepMinute> = buildList {
        repeat(30) { add(minute(it, SleepMinuteKind.RAW, movement = 10.0, heartRate = 64.0)) }
        repeat(stillMinutes) { index ->
            val heartRate = 52.0 + (if (index < 60) (60 - index) / 8.0 else 0.0) + 4 * Math.sin(2 * Math.PI * index / 90.0)
            add(minute(30 + index, SleepMinuteKind.RAW, heartRate = heartRate))
        }
        repeat(30) { add(minute(30 + stillMinutes + it, SleepMinuteKind.RAW, movement = 10.0, heartRate = 64.0)) }
    }

    private fun writtenRecords(): List<SleepSessionRecord> {
        val captured = mutableListOf<List<Record>>()
        coVerify { importRepository.insertImportedRecords(capture(captured)) }
        return captured.flatten().filterIsInstance<SleepSessionRecord>()
    }

    @Test
    fun `a night is written once, keyed on its date`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(420)

        writer.estimateNights(setOf(night), mapOf(night to zone))

        val record = writtenRecords().single()
        assertEquals("garmin_fit_sleep_est_2024-03-05", record.metadata.clientRecordId)
        assertEquals(1_000L, record.metadata.clientRecordVersion)
        assertEquals(zone, record.startZoneOffset)
        assertTrue(record.stages.isNotEmpty())
    }

    @Test
    fun `a longer night rewrites the same id with a higher version`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(240)
        writer.estimateNights(setOf(night), mapOf(night to zone))
        now = 2_000L
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(420)

        writer.estimateNights(setOf(night), mapOf(night to zone))

        val records = writtenRecords()
        assertEquals(2, records.size)
        assertEquals(records[0].metadata.clientRecordId, records[1].metadata.clientRecordId)
        assertEquals(1_000L, records[0].metadata.clientRecordVersion)
        assertEquals(2_000L, records[1].metadata.clientRecordVersion)
        assertTrue(records[1].endTime.isAfter(records[0].endTime))
    }

    @Test
    fun `a duplicate rejection deletes the old estimate and retries once`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(420)
        var calls = 0
        coEvery { importRepository.insertImportedRecords(any()) } coAnswers {
            if (calls++ == 0) throw IllegalStateException("Duplicate clientRecordId record")
        }

        writer.estimateNights(setOf(night), mapOf(night to zone))

        val ids = slot<List<String>>()
        coVerify { healthConnect.deleteImportedRecordsByClientIds(SleepSessionRecord::class, capture(ids)) }
        assertEquals(listOf("garmin_fit_sleep_est_2024-03-05"), ids.captured)
        assertEquals(2, calls)
    }

    @Test
    fun `a night the watch staged is left alone`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(420)

        writer.estimateNights(setOf(night), mapOf(night to zone), nightsWithRealStages = setOf(night))

        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
    }

    @Test
    fun `a watch-staged session already in Health Connect wins`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(420)
        val staged = SleepSessionRecord(
            startTime = bedtime,
            startZoneOffset = null,
            endTime = bedtime.plusSeconds(6 * 3600),
            endZoneOffset = null,
            metadata = Metadata.manualEntry(clientRecordId = "garmin_fit_sleep_${bedtime.toEpochMilli()}"),
        )
        coEvery { healthConnect.forEachSyncRecordPage(any(), any(), any(), any()) } coAnswers {
            arg<suspend (List<Record>) -> Unit>(3).invoke(listOf(staged))
        }

        writer.estimateNights(setOf(night), mapOf(night to zone))

        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
    }

    @Test
    fun `nothing is written when no night is found`() = runTest {
        coEvery { minuteRepository.minutesBetween(any(), any()) } returns nightMinutes(60)

        writer.estimateNights(setOf(night), mapOf(night to zone))

        coVerify(exactly = 0) { importRepository.insertImportedRecords(any()) }
    }
}
