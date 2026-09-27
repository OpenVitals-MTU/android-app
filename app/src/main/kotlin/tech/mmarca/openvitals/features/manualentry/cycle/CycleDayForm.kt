package tech.mmarca.openvitals.features.manualentry.cycle

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import tech.mmarca.openvitals.core.presentation.TemperatureUnits
import tech.mmarca.openvitals.domain.cycle.CycleSymptom
import tech.mmarca.openvitals.domain.model.BbtDisturbance
import tech.mmarca.openvitals.domain.model.CervicalSensation
import tech.mmarca.openvitals.domain.model.CycleDayLog
import tech.mmarca.openvitals.domain.model.CycleDayLogWrite
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.domain.model.DayBleedingChoice
import tech.mmarca.openvitals.domain.model.HcgTestResult
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** The bleeding scale as the day log shows it. Null means not recorded. */
enum class BleedingOption {
    NONE,
    SPOTTING,
    LIGHT,
    MEDIUM,
    HEAVY,
    ;

    fun toChoice(): DayBleedingChoice = when (this) {
        NONE -> DayBleedingChoice.None
        SPOTTING -> DayBleedingChoice.Spotting
        LIGHT -> DayBleedingChoice.Flow(CycleRecordValues.FLOW_LIGHT)
        MEDIUM -> DayBleedingChoice.Flow(CycleRecordValues.FLOW_MEDIUM)
        HEAVY -> DayBleedingChoice.Flow(CycleRecordValues.FLOW_HEAVY)
    }

    val isPeriodFlow: Boolean
        get() = this == LIGHT || this == MEDIUM || this == HEAVY

    companion object {
        fun fromChoice(choice: DayBleedingChoice?): BleedingOption? = when (choice) {
            null -> null
            DayBleedingChoice.None -> NONE
            DayBleedingChoice.Spotting -> SPOTTING
            is DayBleedingChoice.Flow -> when (choice.level) {
                CycleRecordValues.FLOW_LIGHT -> LIGHT
                CycleRecordValues.FLOW_HEAVY -> HEAVY
                else -> MEDIUM
            }
        }
    }
}

/** Every field of the day log, as typed. Compared with the loaded values to find what changed. */
@Immutable
data class CycleDayForm(
    val bleeding: BleedingOption? = null,
    val pain: Int? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val symptoms: Set<CycleSymptom> = emptySet(),
    val notes: String = "",
    val bbtInputText: String = "",
    val bbtLocation: Int? = null,
    val bbtTime: LocalTime? = null,
    val bbtDisturbances: Set<BbtDisturbance> = emptySet(),
    val cervicalSensation: CervicalSensation? = null,
    val mucusAppearance: Int? = null,
    val mucusAmount: Int? = null,
    val ovulationResult: Int? = null,
    val hcgTest: HcgTestResult? = null,
    val sexualActivityProtection: Int? = null,
) {
    val isEmpty: Boolean
        get() = this == CycleDayForm()

    val hasCervicalMucus: Boolean
        get() = mucusAppearance != null || mucusAmount != null

    fun bbtCelsius(unitSystem: UnitSystem): Double? {
        if (bbtInputText.isBlank()) return null
        val value = bbtInputText.replace(',', '.').toDoubleOrNull() ?: return null
        return TemperatureUnits.toCelsius(value, unitSystem)
    }

    /** The Health Connect kinds whose record would change if this form were saved over [baseline]. */
    fun changedKinds(baseline: CycleDayForm, unitSystem: UnitSystem): Set<CycleEntryKind> = buildSet {
        val flowChanged = (bleeding?.takeIf { it.isPeriodFlow }) != (baseline.bleeding?.takeIf { it.isPeriodFlow })
        if (flowChanged) add(CycleEntryKind.MENSTRUATION_FLOW)
        if ((bleeding == BleedingOption.SPOTTING) != (baseline.bleeding == BleedingOption.SPOTTING)) {
            add(CycleEntryKind.SPOTTING)
        }
        val celsius = bbtCelsius(unitSystem)
        val baselineCelsius = baseline.bbtCelsius(unitSystem)
        val bbtChanged = when {
            celsius == null || baselineCelsius == null -> celsius != baselineCelsius
            else -> abs(celsius - baselineCelsius) >= TemperatureEpsilon ||
                bbtLocation != baseline.bbtLocation || bbtTime != baseline.bbtTime
        }
        if (bbtChanged) add(CycleEntryKind.BASAL_BODY_TEMPERATURE)
        if (mucusAppearance != baseline.mucusAppearance || mucusAmount != baseline.mucusAmount) {
            add(CycleEntryKind.CERVICAL_MUCUS)
        }
        if (ovulationResult != baseline.ovulationResult) add(CycleEntryKind.OVULATION_TEST)
        if (sexualActivityProtection != baseline.sexualActivityProtection) add(CycleEntryKind.SEXUAL_ACTIVITY)
    }

    fun toWrite(date: LocalDate, unitSystem: UnitSystem): CycleDayLogWrite = CycleDayLogWrite(
        bleeding = bleeding?.toChoice(),
        basalBodyTemperatureCelsius = bbtCelsius(unitSystem),
        basalBodyTemperatureLocation = bbtLocation,
        basalBodyTemperatureTime = bbtTime,
        mucusAppearance = mucusAppearance,
        mucusAmount = mucusAmount,
        ovulationTestResult = ovulationResult,
        sexualActivityProtection = sexualActivityProtection,
        journal = CycleJournalEntry(
            date = date,
            bleedingNone = bleeding == BleedingOption.NONE,
            painLevel = pain,
            moodLevel = mood,
            energyLevel = energy,
            symptoms = symptoms,
            notes = notes.trim(),
            hcgTest = hcgTest,
            bbtDisturbances = if (bbtInputText.isBlank()) emptySet() else bbtDisturbances,
            cervicalSensation = cervicalSensation,
        ),
    )

    companion object {
        const val MinBbtCelsius = 35.0
        const val MaxBbtCelsius = 39.0
        private const val TemperatureEpsilon = 0.005

        fun fromLog(log: CycleDayLog, unitSystem: UnitSystem, zone: ZoneId = ZoneId.systemDefault()): CycleDayForm {
            val journal = log.journal
            val bbt = log.ownBasalBodyTemperature
            return CycleDayForm(
                bleeding = BleedingOption.fromChoice(log.bleeding),
                pain = journal?.painLevel,
                mood = journal?.moodLevel,
                energy = journal?.energyLevel,
                symptoms = journal?.symptoms.orEmpty(),
                notes = journal?.notes.orEmpty(),
                bbtInputText = bbt?.temperatureCelsius?.toBbtInput(unitSystem).orEmpty(),
                bbtLocation = bbt?.measurementLocation?.takeIf { it != CycleRecordValues.MEASUREMENT_LOCATION_UNKNOWN },
                bbtTime = bbt?.time?.atZone(zone)?.toLocalTime()?.withSecond(0)?.withNano(0),
                bbtDisturbances = journal?.bbtDisturbances.orEmpty(),
                cervicalSensation = journal?.cervicalSensation,
                mucusAppearance = log.ownCervicalMucus?.appearance?.takeIf { it != CycleRecordValues.MUCUS_APPEARANCE_UNKNOWN },
                mucusAmount = log.ownCervicalMucus?.sensation?.takeIf { it != CycleRecordValues.MUCUS_SENSATION_UNKNOWN },
                ovulationResult = log.ownOvulationTest?.result,
                hcgTest = journal?.hcgTest,
                sexualActivityProtection = log.ownSexualActivity?.protectionUsed,
            )
        }

        fun Double.toBbtInput(unitSystem: UnitSystem): String {
            val display = TemperatureUnits.fromCelsius(this, unitSystem)
            return "%.2f".format(java.util.Locale.US, display).trimEnd('0').trimEnd('.')
        }
    }
}
