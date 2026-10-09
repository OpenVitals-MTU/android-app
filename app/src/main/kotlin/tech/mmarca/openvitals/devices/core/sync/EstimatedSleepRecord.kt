package tech.mmarca.openvitals.devices.core.sync

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import tech.mmarca.openvitals.domain.insights.EstimatedSleepSession
import tech.mmarca.openvitals.domain.insights.EstimatedStage

/**
 * The Health Connect shape of a night the phone estimated from per-minute
 * movement and heart rate, for watches that do not stage sleep themselves.
 * Shared by every integration that feeds `SleepStageEstimator`; the id and
 * the wording are the integration's.
 */

/** The window a night is estimated from: the evening before through the next early afternoon. */
fun sleepNightWindow(night: LocalDate, offset: ZoneOffset): Pair<Instant, Instant> = Pair(
    night.minusDays(1).atTime(NightWindowStart).toInstant(offset),
    night.atTime(NightWindowEnd).toInstant(offset),
)

/**
 * The record for an estimated night, or null when nothing would count as
 * sleep. Stages are clamped into the session and merged so Health Connect's
 * overlap check cannot reject them. A higher [version] replaces the earlier
 * estimate under the same [clientRecordId].
 */
fun estimatedSleepSessionRecord(
    session: EstimatedSleepSession,
    offset: ZoneOffset,
    clientRecordId: String,
    version: Long,
    notes: String,
): SleepSessionRecord? {
    val stages = mutableListOf<SleepSessionRecord.Stage>()
    for (span in session.stages.sortedBy { it.start }) {
        val start = maxOf(span.start, session.onset, stages.lastOrNull()?.endTime ?: session.onset)
        val end = minOf(span.end, session.end)
        if (!start.isBefore(end)) continue
        val stage = healthConnectStageFor(span.stage)
        val previous = stages.lastOrNull()
        if (previous != null && previous.stage == stage && previous.endTime == start) {
            stages[stages.size - 1] = SleepSessionRecord.Stage(previous.startTime, end, stage)
        } else {
            stages.add(SleepSessionRecord.Stage(start, end, stage))
        }
    }
    if (stages.none { it.stage in SleepingStages }) return null
    return SleepSessionRecord(
        startTime = session.onset,
        startZoneOffset = offset,
        endTime = session.end,
        endZoneOffset = offset,
        metadata = Metadata.manualEntry(
            clientRecordId = clientRecordId,
            clientRecordVersion = version,
            device = Device(type = Device.TYPE_PHONE),
        ),
        title = EstimatedSleepTitle,
        notes = notes,
        stages = stages,
    )
}

private fun healthConnectStageFor(stage: EstimatedStage): Int = when (stage) {
    EstimatedStage.AWAKE -> SleepSessionRecord.STAGE_TYPE_AWAKE
    EstimatedStage.LIGHT -> SleepSessionRecord.STAGE_TYPE_LIGHT
    EstimatedStage.DEEP -> SleepSessionRecord.STAGE_TYPE_DEEP
    EstimatedStage.REM -> SleepSessionRecord.STAGE_TYPE_REM
    EstimatedStage.UNKNOWN -> SleepSessionRecord.STAGE_TYPE_UNKNOWN
}

private val NightWindowStart: LocalTime = LocalTime.of(18, 0)
private val NightWindowEnd: LocalTime = LocalTime.of(14, 0)
private const val EstimatedSleepTitle = "Sleep"
private val SleepingStages = setOf(
    SleepSessionRecord.STAGE_TYPE_LIGHT,
    SleepSessionRecord.STAGE_TYPE_DEEP,
    SleepSessionRecord.STAGE_TYPE_REM,
)
