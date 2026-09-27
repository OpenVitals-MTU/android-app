package tech.mmarca.openvitals.features.cycle

import android.content.res.Resources
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.compose.runtime.Composable
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
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
    /** Inside the estimated window of the next period, and not a recorded period day. */
    val predictedPeriod: Boolean = false,
    val spotting: Boolean = false,
    val isCycleStart: Boolean = false,
    val journal: CycleJournalEntry? = null,
    val hasOtherObservations: Boolean = false,
) {
    /** Anything recorded besides bleeding: a journal entry, a test, a temperature. */
    val hasObservations: Boolean
        get() = journal?.hasObservations == true || ovulationTests.isNotEmpty() ||
            basalBodyTemperature != null || hasOtherObservations
}

internal data class CycleObservation(
    val time: Instant,
    val title: String,
    val value: String,
    val source: String,
    val id: String = "",
    val kind: CycleEntryKind? = null,
    val isOpenVitalsEntry: Boolean = false,
    /** Set for a journal row: it opens that day's log instead of a record editor. */
    val dayLogDate: LocalDate? = null,
)

internal fun cycleDays(
    period: DatePeriod,
    data: CycleData,
    zone: ZoneId,
    predictedWindows: List<ClosedRange<LocalDate>> = emptyList(),
    journalByDate: Map<LocalDate, CycleJournalEntry> = emptyMap(),
    cycleStarts: Set<LocalDate> = emptySet(),
    today: LocalDate = LocalDate.now(),
): List<CycleDay> {
    val gridStart = period.start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridEnd = period.end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val flowsByDate = data.menstruationFlows.groupBy { it.time.atZone(zone).toLocalDate() }
    val ovulationByDate = data.ovulationTests.groupBy { it.time.atZone(zone).toLocalDate() }
    val bbtByDate = data.basalBodyTemperature
        .groupBy { it.time.atZone(zone).toLocalDate() }
        .mapValues { (_, readings) -> readings.maxByOrNull { it.time } }
    val menstruationDates = data.menstruationPeriods.flatMap { it.dates(zone) }.toSet()
    val spottingDates = data.intermenstrualBleeding.map { it.time.atZone(zone).toLocalDate() }.toSet()
    val otherDates = (data.cervicalMucus.map { it.time } + data.sexualActivity.map { it.time })
        .map { it.atZone(zone).toLocalDate() }
        .toSet()

    return datesBetween(gridStart, gridEnd).map { date ->
        val hasPeriod = date in menstruationDates || date in flowsByDate
        CycleDay(
            date = date,
            inSelectedPeriod = !date.isBefore(period.start) && !date.isAfter(period.end),
            periodActive = date in menstruationDates,
            flows = flowsByDate[date].orEmpty(),
            ovulationTests = ovulationByDate[date].orEmpty(),
            basalBodyTemperature = bbtByDate[date],
            predictedPeriod = !hasPeriod && !date.isBefore(today) &&
                predictedWindows.any { !date.isBefore(it.start) && !date.isAfter(it.endInclusive) },
            spotting = date in spottingDates,
            isCycleStart = date in cycleStarts,
            journal = journalByDate[date],
            hasOtherObservations = date in otherDates,
        )
    }
}

internal fun observationsFor(
    data: CycleData,
    resources: Resources,
    journalEntries: List<CycleJournalEntry>,
    unitFormatter: UnitFormatter,
): List<CycleObservation> {
    val zone = ZoneId.systemDefault()
    return buildList {
        data.menstruationPeriods.forEach { period ->
            val days = period.dates(zone).size.coerceAtLeast(1)
            add(
                CycleObservation(
                    time = period.startTime,
                    title = resources.getString(R.string.cycle_observation_menstruation_period),
                    value = resources.getQuantityString(R.plurals.cycle_days_value, days, days),
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
                    id = flow.id,
                    kind = CycleEntryKind.MENSTRUATION_FLOW,
                    isOpenVitalsEntry = flow.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(flow.isOpenVitalsEntry, flow.time, zone),
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
                    id = test.id,
                    kind = CycleEntryKind.OVULATION_TEST,
                    isOpenVitalsEntry = test.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(test.isOpenVitalsEntry, test.time, zone),
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
                    id = mucus.id,
                    kind = CycleEntryKind.CERVICAL_MUCUS,
                    isOpenVitalsEntry = mucus.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(mucus.isOpenVitalsEntry, mucus.time, zone),
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
                        unitFormatter.temperature(temperature.temperatureCelsius, decimals = BbtDecimals).text,
                        resources.getString(measurementLocationLabelRes(temperature.measurementLocation)),
                    ),
                    source = temperature.source,
                    id = temperature.id,
                    kind = CycleEntryKind.BASAL_BODY_TEMPERATURE,
                    isOpenVitalsEntry = temperature.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(temperature.isOpenVitalsEntry, temperature.time, zone),
                )
            )
        }
        data.intermenstrualBleeding.forEach { bleeding ->
            add(
                CycleObservation(
                    time = bleeding.time,
                    title = resources.getString(R.string.cycle_observation_intermenstrual_bleeding),
                    value = resources.getString(R.string.cycle_entry_section_spotting),
                    source = bleeding.source,
                    id = bleeding.id,
                    kind = CycleEntryKind.SPOTTING,
                    isOpenVitalsEntry = bleeding.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(bleeding.isOpenVitalsEntry, bleeding.time, zone),
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
                    id = activity.id,
                    kind = CycleEntryKind.SEXUAL_ACTIVITY,
                    isOpenVitalsEntry = activity.isOpenVitalsEntry,
                    dayLogDate = ownDayLogDate(activity.isOpenVitalsEntry, activity.time, zone),
                )
            )
        }
        journalEntries.filter { it.hasObservations }.forEach { entry ->
            add(
                CycleObservation(
                    time = entry.date.atStartOfDay(zone).toInstant(),
                    title = resources.getString(R.string.cycle_observation_day_log),
                    value = journalSummary(entry, resources),
                    source = resources.getString(R.string.app_name),
                    id = entry.date.toString(),
                    isOpenVitalsEntry = true,
                    dayLogDate = entry.date,
                )
            )
        }
    }.sortedByDescending { it.time }
}

/** "Pain 3/5 · Mood 4/5 · 2 symptoms · note", from what the day holds. */
internal fun journalSummary(entry: CycleJournalEntry, resources: Resources): String = buildList {
    entry.painLevel?.let { add(resources.getString(R.string.cycle_today_pain, it)) }
    entry.moodLevel?.let { add(resources.getString(R.string.cycle_today_mood, it)) }
    entry.energyLevel?.let { add(resources.getString(R.string.cycle_today_energy, it)) }
    if (entry.symptoms.isNotEmpty()) {
        add(resources.getQuantityString(R.plurals.cycle_today_symptoms, entry.symptoms.size, entry.symptoms.size))
    }
    if (entry.bleedingNone) add(resources.getString(R.string.cycle_bleeding_none))
    if (entry.hcgTest != null) add(resources.getString(R.string.cycle_entry_section_hcg))
    if (entry.notes.isNotBlank()) add(resources.getString(R.string.cycle_journal_summary_note))
}.joinToString(separator = " · ")

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

internal fun flowLabel(flow: Int, resources: Resources): String = resources.getString(flowLabelRes(flow))

internal fun flowLabelRes(flow: Int): Int = when (flow) {
    FLOW_LIGHT -> R.string.cycle_flow_light
    FLOW_MEDIUM -> R.string.cycle_flow_medium
    FLOW_HEAVY -> R.string.cycle_flow_heavy
    else -> R.string.recording_unknown
}

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

internal const val FLOW_UNKNOWN = CycleRecordValues.FLOW_UNKNOWN
internal const val FLOW_LIGHT = CycleRecordValues.FLOW_LIGHT
internal const val FLOW_MEDIUM = CycleRecordValues.FLOW_MEDIUM
internal const val FLOW_HEAVY = CycleRecordValues.FLOW_HEAVY

private const val OVULATION_POSITIVE = CycleRecordValues.OVULATION_POSITIVE
private const val OVULATION_HIGH = CycleRecordValues.OVULATION_HIGH
private const val OVULATION_NEGATIVE = CycleRecordValues.OVULATION_NEGATIVE

private const val MUCUS_DRY = CycleRecordValues.MUCUS_APPEARANCE_DRY
private const val MUCUS_STICKY = CycleRecordValues.MUCUS_APPEARANCE_STICKY
private const val MUCUS_CREAMY = CycleRecordValues.MUCUS_APPEARANCE_CREAMY
private const val MUCUS_WATERY = CycleRecordValues.MUCUS_APPEARANCE_WATERY
private const val MUCUS_EGG_WHITE = CycleRecordValues.MUCUS_APPEARANCE_EGG_WHITE
private const val MUCUS_UNUSUAL = CycleRecordValues.MUCUS_APPEARANCE_UNUSUAL

private const val MUCUS_LIGHT = CycleRecordValues.MUCUS_SENSATION_LIGHT
private const val MUCUS_MEDIUM = CycleRecordValues.MUCUS_SENSATION_MEDIUM
private const val MUCUS_HEAVY = CycleRecordValues.MUCUS_SENSATION_HEAVY

/** An own Health Connect record is edited through its day's log; another app's is not. */
private fun ownDayLogDate(isOwn: Boolean, time: Instant, zone: ZoneId): LocalDate? =
    if (isOwn) time.atZone(zone).toLocalDate() else null

/** Basal temperatures are read to the hundredth. */
private const val BbtDecimals = 2
