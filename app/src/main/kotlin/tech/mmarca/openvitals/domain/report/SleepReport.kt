package tech.mmarca.openvitals.domain.report

import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import tech.mmarca.openvitals.domain.model.AwakeStageTypes
import tech.mmarca.openvitals.domain.model.ReportSleepDetail
import tech.mmarca.openvitals.domain.model.ReportSleepNight
import tech.mmarca.openvitals.domain.model.ReportSleepStageMix
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.durationMsForTypes
import tech.mmarca.openvitals.domain.model.sleepSessionHasReliableStages

/** Sessions shorter than this are naps, not nights — excluded throughout. */
private const val MinNightDurationMs = 3 * 60 * 60 * 1000L

private const val MinutesPerDay = 24 * 60

/**
 * The sleep section: one row per night, schedule averages and the stage
 * mix. Bedtime averages are circular means. Stages count only reliable nights.
 */
fun sleepDetail(
    sessions: List<SleepData>,
    zone: ZoneId,
): ReportSleepDetail? {
    val nights = sessions
        .filter { it.durationMs >= MinNightDurationMs }
        .sortedBy { it.startTime }
    if (nights.isEmpty()) return null

    val nightRows = nights.map { session ->
        val reliable = sleepSessionHasReliableStages(session)
        ReportSleepNight(
            date = session.endTime.atZone(zone).toLocalDate(),
            bedtime = session.startTime,
            wake = session.endTime,
            asleepMs = session.durationMs,
            deepMs = session.stages.durationMsForTypes(setOf(SleepStage.STAGE_DEEP)).takeIf { reliable },
            remMs = session.stages.durationMsForTypes(setOf(SleepStage.STAGE_REM)).takeIf { reliable },
        )
    }

    val staged = nights.filter { sleepSessionHasReliableStages(it) }
    val stageMix = stageMix(staged)

    fun minutesOfDay(instant: java.time.Instant): Int =
        instant.atZone(zone).let { it.hour * 60 + it.minute }

    return ReportSleepDetail(
        nights = nightRows,
        averageBedtimeMinutes = circularMeanMinutes(nights.map { minutesOfDay(it.startTime) }),
        averageWakeMinutes = circularMeanMinutes(nights.map { minutesOfDay(it.endTime) }),
        stageMix = stageMix,
        nightsWithData = nights.size,
    )
}

private fun stageMix(staged: List<SleepData>): ReportSleepStageMix? {
    if (staged.isEmpty()) return null
    val deep = staged.sumOf { it.stages.durationMsForTypes(setOf(SleepStage.STAGE_DEEP)) }
    val rem = staged.sumOf { it.stages.durationMsForTypes(setOf(SleepStage.STAGE_REM)) }
    val light = staged.sumOf {
        it.stages.durationMsForTypes(setOf(SleepStage.STAGE_LIGHT, SleepStage.STAGE_SLEEPING))
    }
    val awake = staged.sumOf { it.stages.durationMsForTypes(AwakeStageTypes) }
    val total = deep + rem + light + awake
    if (total <= 0L) return null
    return ReportSleepStageMix(
        deepPct = deep * 100.0 / total,
        remPct = rem * 100.0 / total,
        lightPct = light * 100.0 / total,
        awakePct = awake * 100.0 / total,
    )
}

/** The circular mean of minutes of day, so 23:30 and 00:30 average to 00:00. */
internal fun circularMeanMinutes(minutesOfDay: List<Int>): Int? {
    if (minutesOfDay.isEmpty()) return null
    val sinSum = minutesOfDay.sumOf { sin(it * 2.0 * PI / MinutesPerDay) }
    val cosSum = minutesOfDay.sumOf { cos(it * 2.0 * PI / MinutesPerDay) }
    val meanAngle = atan2(sinSum / minutesOfDay.size, cosSum / minutesOfDay.size)
    val minutes = (meanAngle * MinutesPerDay / (2.0 * PI)).roundToInt()
    return ((minutes % MinutesPerDay) + MinutesPerDay) % MinutesPerDay
}
