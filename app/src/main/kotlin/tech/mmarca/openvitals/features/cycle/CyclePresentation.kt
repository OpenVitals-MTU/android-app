package tech.mmarca.openvitals.features.cycle

import android.content.res.Resources
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class CycleDay(
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

internal fun observationsFor(data: CycleData, resources: Resources): List<CycleObservation> {
    val zone = ZoneId.systemDefault()
    return buildList {
        data.menstruationPeriods.forEach { period ->
            val days = period.dates(zone).size.toLong().coerceAtLeast(1)
            add(
                CycleObservation(
                    time = period.startTime,
                    title = resources.getString(R.string.cycle_observation_menstruation_period),
                    value = resources.getString(
                        R.string.cycle_days_value,
                        days,
                        resources.getString(
                            if (days == 1L) R.string.cycle_day_singular else R.string.cycle_day_plural
                        ),
                    ),
                    source = period.source,
                )
            )
        }
        data.menstruationFlows.forEach { flow ->
            add(
                CycleObservation(
                    time = flow.time,
                    title = resources.getString(R.string.cycle_observation_menstruation_flow),
                    value = flowLabel(flow.flow, resources),
                    source = flow.source,
                )
            )
        }
        data.ovulationTests.forEach { test ->
            add(
                CycleObservation(
                    time = test.time,
                    title = resources.getString(R.string.cycle_observation_ovulation_test),
                    value = ovulationResultLabel(test.result, resources),
                    source = test.source,
                )
            )
        }
        data.cervicalMucus.forEach { mucus ->
            add(
                CycleObservation(
                    time = mucus.time,
                    title = resources.getString(R.string.cycle_observation_cervical_mucus),
                    value = mucusLabel(mucus, resources),
                    source = mucus.source,
                )
            )
        }
        data.basalBodyTemperature.forEach { temperature ->
            add(
                CycleObservation(
                    time = temperature.time,
                    title = resources.getString(R.string.cycle_observation_basal_body_temperature),
                    value = resources.getString(
                        R.string.cycle_basal_temperature_value,
                        temperature.temperatureCelsius,
                        resources.getString(measurementLocationLabelRes(temperature.measurementLocation)),
                    ),
                    source = temperature.source,
                )
            )
        }
        data.intermenstrualBleeding.forEach { bleeding ->
            add(
                CycleObservation(
                    time = bleeding.time,
                    title = resources.getString(R.string.cycle_observation_intermenstrual_bleeding),
                    value = resources.getString(R.string.recording_actively_recorded),
                    source = bleeding.source,
                )
            )
        }
        data.sexualActivity.forEach { activity ->
            add(
                CycleObservation(
                    time = activity.time,
                    title = resources.getString(R.string.cycle_observation_sexual_activity),
                    value = sexualActivityProtectionLabel(activity.protectionUsed, resources),
                    source = activity.source,
                )
            )
        }
    }.sortedByDescending { it.time }
}

@Composable
internal fun measurementLocationLabel(location: Int): String = stringResource(
    measurementLocationLabelRes(location)
)

internal fun measurementLocationLabelRes(location: Int): Int = when (location) {
    1 -> R.string.measurement_location_armpit
    2 -> R.string.measurement_location_finger
    3 -> R.string.measurement_location_forehead
    4 -> R.string.measurement_location_mouth
    5 -> R.string.measurement_location_rectum
    6 -> R.string.measurement_location_temporal_artery
    7 -> R.string.measurement_location_toe
    8 -> R.string.measurement_location_ear
    9 -> R.string.measurement_location_wrist
    10 -> R.string.measurement_location_vagina
    else -> R.string.measurement_location_unknown
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

private fun flowLabel(flow: Int, resources: Resources): String = resources.getString(
    when (flow) {
        FLOW_LIGHT -> R.string.cycle_flow_light
        FLOW_MEDIUM -> R.string.cycle_flow_medium
        FLOW_HEAVY -> R.string.cycle_flow_heavy
        else -> R.string.recording_unknown
    }
)

private fun ovulationResultLabel(result: Int, resources: Resources): String = resources.getString(
    when (result) {
        OVULATION_POSITIVE -> R.string.cycle_ovulation_positive
        OVULATION_HIGH -> R.string.cycle_ovulation_high
        OVULATION_NEGATIVE -> R.string.cycle_ovulation_negative
        else -> R.string.cycle_ovulation_inconclusive
    }
)

private fun mucusLabel(mucus: CervicalMucusEntry, resources: Resources): String {
    val appearance = when (mucus.appearance) {
        MUCUS_DRY -> R.string.cycle_mucus_dry
        MUCUS_STICKY -> R.string.cycle_mucus_sticky
        MUCUS_CREAMY -> R.string.cycle_mucus_creamy
        MUCUS_WATERY -> R.string.cycle_mucus_watery
        MUCUS_EGG_WHITE -> R.string.cycle_mucus_egg_white
        MUCUS_UNUSUAL -> R.string.cycle_mucus_unusual
        else -> R.string.recording_unknown
    }
    val sensation = when (mucus.sensation) {
        MUCUS_LIGHT -> R.string.cycle_mucus_light
        MUCUS_MEDIUM -> R.string.cycle_mucus_medium
        MUCUS_HEAVY -> R.string.cycle_mucus_heavy
        else -> R.string.recording_unknown
    }
    return resources.getString(
        R.string.cycle_mucus_value,
        resources.getString(appearance),
        resources.getString(sensation),
    )
}

private fun sexualActivityProtectionLabel(protectionUsed: Int, resources: Resources): String = resources.getString(
    when (protectionUsed) {
        SexualActivityRecord.PROTECTION_USED_PROTECTED -> R.string.cycle_protection_protected
        SexualActivityRecord.PROTECTION_USED_UNPROTECTED -> R.string.cycle_protection_unprotected
        else -> R.string.cycle_protection_unknown
    }
)

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
