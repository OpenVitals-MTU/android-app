package tech.mmarca.openvitals.devices.garmin.wellness

import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.EstimatedSleepSession
import tech.mmarca.openvitals.domain.insights.EstimatedStage
import tech.mmarca.openvitals.domain.insights.EstimatedStageSpan

class GarminSleepEstimationTest {

    private val zone: ZoneOffset = ZoneOffset.ofHours(2)
    private val night: LocalDate = LocalDate.of(2024, 3, 5)
    private val onset: Instant = night.minusDays(1).atTime(23, 0).toInstant(zone)
    private val end: Instant = night.atTime(7, 0).toInstant(zone)

    private fun span(fromMinutes: Long, toMinutes: Long, stage: EstimatedStage) =
        EstimatedStageSpan(onset.plusSeconds(fromMinutes * 60), onset.plusSeconds(toMinutes * 60), stage)

    private fun session(spans: List<EstimatedStageSpan>) = EstimatedSleepSession(
        onset = onset,
        end = end,
        inProgress = false,
        stages = spans,
        sleepMinutes = 400,
        awakeMinutes = 20,
        unknownMinutes = 0,
        deepThreshold = 0.6f,
        remThreshold = 0.6f,
    )

    @Test
    fun `the window runs from the evening before to the early afternoon`() {
        val (from, to) = sleepNightWindow(night, zone)

        assertEquals(night.minusDays(1).atTime(18, 0).toInstant(zone), from)
        assertEquals(night.atTime(14, 0).toInstant(zone), to)
    }

    @Test
    fun `ids are keyed on the night and tell estimates from watch stages`() {
        assertEquals("garmin_fit_sleep_est_2024-03-05", estimatedSleepClientRecordId(night))
        assertTrue(isWatchStagedSleepId("garmin_fit_sleep_1709592000000"))
        assertFalse(isWatchStagedSleepId("garmin_fit_sleep_est_2024-03-05"))
        assertFalse(isWatchStagedSleepId("apple_health_sleep_1"))
    }

    @Test
    fun `the record carries the session, its offsets, a version and the estimate note`() {
        val record = estimatedSleepImportRecord(
            session(listOf(span(0, 300, EstimatedStage.LIGHT), span(300, 480, EstimatedStage.DEEP))),
            night,
            zone,
            version = 42L,
        )

        assertNotNull(record)
        record!!
        assertEquals(onset, record.startTime)
        assertEquals(end, record.endTime)
        assertEquals(zone, record.startZoneOffset)
        assertEquals(zone, record.endZoneOffset)
        assertEquals("garmin_fit_sleep_est_2024-03-05", record.metadata.clientRecordId)
        assertEquals(42L, record.metadata.clientRecordVersion)
        assertEquals("Sleep", record.title)
        assertTrue(record.notes!!.contains("estimated"))
        assertEquals(
            listOf(SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_DEEP),
            record.stages.map { it.stage },
        )
    }

    @Test
    fun `stages are clamped into the session, merged, and mapped`() {
        val record = estimatedSleepImportRecord(
            session(
                listOf(
                    span(-10, 60, EstimatedStage.AWAKE),
                    span(60, 100, EstimatedStage.LIGHT),
                    span(100, 140, EstimatedStage.LIGHT),
                    span(140, 160, EstimatedStage.UNKNOWN),
                    span(160, 300, EstimatedStage.REM),
                    span(300, 900, EstimatedStage.LIGHT),
                ),
            ),
            night,
            zone,
            version = 1L,
        )!!

        assertEquals(onset, record.stages.first().startTime)
        assertEquals(end, record.stages.last().endTime)
        assertEquals(
            listOf(
                SleepSessionRecord.STAGE_TYPE_AWAKE,
                SleepSessionRecord.STAGE_TYPE_LIGHT,
                SleepSessionRecord.STAGE_TYPE_UNKNOWN,
                SleepSessionRecord.STAGE_TYPE_REM,
                SleepSessionRecord.STAGE_TYPE_LIGHT,
            ),
            record.stages.map { it.stage },
        )
        // The two light spans became one.
        assertEquals(onset.plusSeconds(60 * 60), record.stages[1].startTime)
        assertEquals(onset.plusSeconds(140 * 60), record.stages[1].endTime)
    }

    @Test
    fun `no record without a sleeping stage`() {
        assertNull(
            estimatedSleepImportRecord(
                session(listOf(span(0, 200, EstimatedStage.AWAKE), span(200, 480, EstimatedStage.UNKNOWN))),
                night,
                zone,
                version = 1L,
            ),
        )
    }

    @Test
    fun `decoded minutes map onto the domain model`() {
        val minute = FitSleepMinute(
            time = onset,
            kind = FitSleepMinuteKind.RAW,
            heartRate = 50.0,
            movement = 2.0,
            activity = -0.5,
            zoneOffset = zone,
            features = FloatArray(10) { it.toFloat() },
        ).toGarminSleepMinute()

        assertEquals(onset, minute.time)
        assertEquals(50.0, minute.heartRate!!, 0.0)
        assertEquals(2.0, minute.movement!!, 0.0)
        assertEquals(zone, minute.zoneOffset)
        assertEquals(9f, minute.features!![9], 0f)
        val estimatorInput = minute.toSleepMinute()
        assertEquals(50f, estimatorInput.heartRate!!, 0f)
        assertEquals(2f, estimatorInput.movement, 0f)
    }
}
