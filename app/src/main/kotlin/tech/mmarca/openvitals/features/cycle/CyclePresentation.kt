package tech.mmarca.openvitals.features.cycle

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.data.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.data.model.CervicalMucusEntry
import tech.mmarca.openvitals.data.model.CycleData
import tech.mmarca.openvitals.data.model.MenstruationFlowEntry
import tech.mmarca.openvitals.data.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.data.model.OvulationTestEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal data class CycleDay(
    val date: LocalDate,
    val inSelectedPeriod: Boolean,
    val periodActive: Boolean,
    val flows: List<MenstruationFlowEntry>,
    val ovulationTests: List<OvulationTestEntry>,
    val basalBodyTemperature: BasalBodyTemperatureEntry?,
)

internal data class CycleObservation(
    val time: Instant,
    val title: String,
    val value: String,
    val source: String,
)

internal fun cycleDays(period: DatePeriod, data: CycleData, zone: ZoneId): List<CycleDay> {
    val gridStart = period.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridEnd = period.end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val flowsByDate = data.menstruationFlows.groupBy { it.time.atZone(zone).toLocalDate() }
    val ovulationByDate = data.ovulationTests.groupBy { it.time.atZone(zone).toLocalDate() }
    val bbtByDate = data.basalBodyTemperature
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .mapValues { (_, readings) -> readings.maxByOrNull { it.time } }
    val menstruationDates = data.menstruationPeriods.flatMap { it.dates(zone) }.toSet()

    return datesBetween(gridStart, gridEnd).map { date ->
        CycleDay(
            date = date,
            inSelectedPeriod = !date.isBefore(period.start) && !date.isAfter(period.end),
            periodActive = date in menstruationDates,
            flows = flowsByDate[date].orEmpty(),
            ovulationTests = ovulationByDate[date].orEmpty(),
            basalBodyTemperature = bbtByDate[date],
        )
    }
}

internal fun observationsFor(data: CycleData): List<CycleObservation> {
    val zone = ZoneId.systemDefault()
    return buildList {
        data.menstruationPeriods.forEach { period ->
            val days = period.dates(zone).size.toLong().coerceAtLeast(1)
            add(
                CycleObservation(
                    time = period.startTime,
                    title = "Menstruation period",
                    value = "$days ${if (days == 1L) "day" else "days"}",
                    source = period.source,
                )
            )
        }
        data.menstruationFlows.forEach { flow ->
            add(
                CycleObservation(
                    time = flow.time,
                    title = "Menstruation flow",
                    value = flowLabel(flow.flow),
                    source = flow.source,
                )
            )
        }
        data.ovulationTests.forEach { test ->
            add(
                CycleObservation(
                    time = test.time,
                    title = "Ovulation test",
                    value = ovulationResultLabel(test.result),
                    source = test.source,
                )
            )
        }
        data.cervicalMucus.forEach { mucus ->
            add(
                CycleObservation(
                    time = mucus.time,
                    title = "Cervical mucus",
                    value = mucusLabel(mucus),
                    source = mucus.source,
                )
            )
        }
    }.sortedByDescending { it.time }
}

internal fun measurementLocationLabel(location: Int): String = when (location) {
    1 -> "Armpit"
    2 -> "Finger"
    3 -> "Forehead"
    4 -> "Mouth"
    5 -> "Rectum"
    6 -> "Temporal artery"
    7 -> "Toe"
    8 -> "Ear"
    9 -> "Wrist"
    10 -> "Vagina"
    else -> "Measurement location unknown"
}

private fun MenstruationPeriodEntry.dates(zone: ZoneId): List<LocalDate> {
    val startDate = startTime.atZone(zone).toLocalDate()
    val endDate = endTime.minusMillis(1).atZone(zone).toLocalDate()
    return datesBetween(startDate, endDate)
}

private fun datesBetween(start: LocalDate, endInclusive: LocalDate): List<LocalDate> =
    generateSequence(start) { date ->
        val next = date.plusDays(1)
        if (next.isAfter(endInclusive)) null else next
    }.toList()

private fun flowLabel(flow: Int): String = when (flow) {
    FLOW_LIGHT -> "Light"
    FLOW_MEDIUM -> "Medium"
    FLOW_HEAVY -> "Heavy"
    else -> "Unknown"
}

private fun ovulationResultLabel(result: Int): String = when (result) {
    OVULATION_POSITIVE -> "Positive"
    OVULATION_HIGH -> "High"
    OVULATION_NEGATIVE -> "Negative"
    else -> "Inconclusive"
}

private fun mucusLabel(mucus: CervicalMucusEntry): String {
    val appearance = when (mucus.appearance) {
        MUCUS_DRY -> "Dry"
        MUCUS_STICKY -> "Sticky"
        MUCUS_CREAMY -> "Creamy"
        MUCUS_WATERY -> "Watery"
        MUCUS_EGG_WHITE -> "Egg white"
        MUCUS_UNUSUAL -> "Unusual"
        else -> "Unknown"
    }
    val sensation = when (mucus.sensation) {
        MUCUS_LIGHT -> "light"
        MUCUS_MEDIUM -> "medium"
        MUCUS_HEAVY -> "heavy"
        else -> "unknown"
    }
    return "$appearance, $sensation"
}

internal const val FLOW_UNKNOWN = 0
internal const val FLOW_LIGHT = 1
internal const val FLOW_MEDIUM = 2
internal const val FLOW_HEAVY = 3

private const val OVULATION_POSITIVE = 1
private const val OVULATION_HIGH = 2
private const val OVULATION_NEGATIVE = 3

private const val MUCUS_DRY = 1
private const val MUCUS_STICKY = 2
private const val MUCUS_CREAMY = 3
private const val MUCUS_WATERY = 4
private const val MUCUS_EGG_WHITE = 5
private const val MUCUS_UNUSUAL = 6

private const val MUCUS_LIGHT = 1
private const val MUCUS_MEDIUM = 2
private const val MUCUS_HEAVY = 3
