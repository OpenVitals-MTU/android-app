package tech.mmarca.openvitals.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** What the user records about bleeding on one day. Absent means not recorded. */
sealed interface DayBleedingChoice {
    data object None : DayBleedingChoice

    data object Spotting : DayBleedingChoice

    /** Light, medium or heavy, as `CycleRecordValues.FLOW_*`. */
    data class Flow(val level: Int) : DayBleedingChoice
}

/**
 * Everything recorded for one day: the app's own Health Connect records, a
 * summary of other apps' records, and the journal entry.
 */
data class CycleDayLog(
    val date: LocalDate,
    val journal: CycleJournalEntry? = null,
    val ownFlow: MenstruationFlowEntry? = null,
    val ownSpotting: IntermenstrualBleedingEntry? = null,
    val ownBasalBodyTemperature: BasalBodyTemperatureEntry? = null,
    val ownCervicalMucus: CervicalMucusEntry? = null,
    val ownOvulationTest: OvulationTestEntry? = null,
    val ownSexualActivity: SexualActivityEntry? = null,
    /** The heaviest flow another app recorded that day, or null. */
    val foreignFlowLevel: Int? = null,
    val foreignSpotting: Boolean = false,
) {
    /** The bleeding the day log shows, from the app's own records and the journal. */
    val bleeding: DayBleedingChoice?
        get() = when {
            ownFlow != null -> DayBleedingChoice.Flow(ownFlow.flow)
            ownSpotting != null -> DayBleedingChoice.Spotting
            journal?.bleedingNone == true -> DayBleedingChoice.None
            else -> null
        }
}

/**
 * One save of the day log. Each Health Connect field replaces the app's own
 * record of that kind for the day; null removes it. The journal replaces the
 * day's row, or deletes it when empty.
 */
data class CycleDayLogWrite(
    val bleeding: DayBleedingChoice? = null,
    val basalBodyTemperatureCelsius: Double? = null,
    val basalBodyTemperatureLocation: Int? = null,
    /** The clock time of the temperature. Null keeps the record's time, or uses the day's default. */
    val basalBodyTemperatureTime: LocalTime? = null,
    val mucusAppearance: Int? = null,
    val mucusAmount: Int? = null,
    val ovulationTestResult: Int? = null,
    val sexualActivityProtection: Int? = null,
    val journal: CycleJournalEntry,
) {
    val hasBasalBodyTemperature: Boolean
        get() = basalBodyTemperatureCelsius != null

    val hasCervicalMucus: Boolean
        get() = (mucusAppearance != null && mucusAppearance != CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN) ||
            (mucusAmount != null && mucusAmount != CycleRecordValues.MUCUS_SENSATION_UNKNOWN)
}
