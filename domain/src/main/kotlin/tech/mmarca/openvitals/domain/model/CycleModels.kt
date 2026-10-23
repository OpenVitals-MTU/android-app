package tech.mmarca.openvitals.domain.model

import java.time.Instant

data class CycleData(
    val menstruationFlows: List<MenstruationFlowEntry> = emptyList(),
    val menstruationPeriods: List<MenstruationPeriodEntry> = emptyList(),
    val ovulationTests: List<OvulationTestEntry> = emptyList(),
    val cervicalMucus: List<CervicalMucusEntry> = emptyList(),
    val basalBodyTemperature: List<BasalBodyTemperatureEntry> = emptyList(),
    val intermenstrualBleeding: List<IntermenstrualBleedingEntry> = emptyList(),
    val sexualActivity: List<SexualActivityEntry> = emptyList(),
) {
    val hasData: Boolean
        get() = menstruationFlows.isNotEmpty() ||
            menstruationPeriods.isNotEmpty() ||
            ovulationTests.isNotEmpty() ||
            cervicalMucus.isNotEmpty() ||
            basalBodyTemperature.isNotEmpty() ||
            intermenstrualBleeding.isNotEmpty() ||
            sexualActivity.isNotEmpty()
}

data class MenstruationFlowEntry(
    val time: Instant,
    val flow: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class MenstruationPeriodEntry(
    val startTime: Instant,
    val endTime: Instant,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
) {
    val durationMs: Long get() = endTime.toEpochMilli() - startTime.toEpochMilli()
}

data class OvulationTestEntry(
    val time: Instant,
    val result: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class CervicalMucusEntry(
    val time: Instant,
    val appearance: Int,
    val sensation: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BasalBodyTemperatureEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val measurementLocation: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class IntermenstrualBleedingEntry(
    val time: Instant,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class SexualActivityEntry(
    val time: Instant,
    val protectionUsed: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

/** The six Health Connect record types a user can log manually. */
enum class CycleEntryKind {
    MENSTRUATION_FLOW,
    SPOTTING,
    SEXUAL_ACTIVITY,
    OVULATION_TEST,
    CERVICAL_MUCUS,
    BASAL_BODY_TEMPERATURE,
}

/** One manual cycle observation. Only [kind]'s payload fields are read. */
data class CycleEntryWriteRequest(
    val kind: CycleEntryKind,
    val time: Instant,
    val flow: Int? = null,
    val protectionUsed: Int? = null,
    val ovulationTestResult: Int? = null,
    val mucusAppearance: Int? = null,
    val mucusSensation: Int? = null,
    val temperatureCelsius: Double? = null,
    val measurementLocation: Int? = null,
)

/** A single cycle record loaded by uid for the edit flow. */
data class CycleEntry(
    val id: String,
    val kind: CycleEntryKind,
    val time: Instant,
    val flow: Int? = null,
    val protectionUsed: Int? = null,
    val ovulationTestResult: Int? = null,
    val mucusAppearance: Int? = null,
    val mucusSensation: Int? = null,
    val temperatureCelsius: Double? = null,
    val measurementLocation: Int? = null,
    val source: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

/** Integer constants of the cycle records, mirrored so the domain has no androidx dependency. */
object CycleRecordValues {
    const val FLOW_UNKNOWN = 0
    const val FLOW_LIGHT = 1
    const val FLOW_MEDIUM = 2
    const val FLOW_HEAVY = 3

    const val OVULATION_INCONCLUSIVE = 0
    const val OVULATION_POSITIVE = 1
    const val OVULATION_HIGH = 2
    const val OVULATION_NEGATIVE = 3

    const val MUCUS_APPEARANCE_UNKNOWN = 0
    const val MUCUS_APPEARANCE_DRY = 1
    const val MUCUS_APPEARANCE_STICKY = 2
    const val MUCUS_APPEARANCE_CREAMY = 3
    const val MUCUS_APPEARANCE_WATERY = 4
    const val MUCUS_APPEARANCE_EGG_WHITE = 5
    const val MUCUS_APPEARANCE_UNUSUAL = 6

    const val MUCUS_SENSATION_UNKNOWN = 0
    const val MUCUS_SENSATION_LIGHT = 1
    const val MUCUS_SENSATION_MEDIUM = 2
    const val MUCUS_SENSATION_HEAVY = 3

    const val PROTECTION_UNKNOWN = 0
    const val PROTECTION_PROTECTED = 1
    const val PROTECTION_UNPROTECTED = 2

    const val MEASUREMENT_LOCATION_UNKNOWN = 0
    val MeasurementLocationRange = 0..10
}
