package tech.mmarca.openvitals.domain.model

import java.time.Instant

/**
 * A body value the app can write. Each one is its own Health Connect record type, held in a
 * canonical unit: kilograms for the masses, centimetres for height, percent for body fat,
 * kilocalories per day for the basal metabolic rate.
 */
enum class BodyMeasurementType {
    WEIGHT,
    HEIGHT,
    BODY_FAT,
    LEAN_MASS,
    BODY_WATER_MASS,
    BONE_MASS,
    BMR,
}

/** Whether [value], in this type's canonical unit, is one Health Connect would take. */
fun BodyMeasurementType.accepts(value: Double): Boolean =
    when (this) {
        BodyMeasurementType.HEIGHT -> value > 0.0 && value <= MaxHeightCm
        BodyMeasurementType.BODY_FAT -> value >= 0.0 && value <= MaxPercent
        BodyMeasurementType.BMR -> value > 0.0 && value <= MaxBmrKcalPerDay
        BodyMeasurementType.WEIGHT,
        BodyMeasurementType.LEAN_MASS,
        BodyMeasurementType.BODY_WATER_MASS,
        BodyMeasurementType.BONE_MASS,
        -> value > 0.0 && value <= MaxMassKg
    }

private const val MaxMassKg = 1000.0
private const val MaxHeightCm = 300.0
private const val MaxPercent = 100.0
private const val MaxBmrKcalPerDay = 10_000.0

data class BodyMeasurementWriteRequest(
    val type: BodyMeasurementType,
    val time: Instant,
    val value: Double,
)

data class WeightEntry(
    val time: Instant,
    val weightKg: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class HeightEntry(
    val time: Instant,
    val heightCm: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BodyFatEntry(
    val time: Instant,
    val percent: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class LeanBodyMassEntry(
    val time: Instant,
    val massKg: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BmrEntry(
    val time: Instant,
    val kcalPerDay: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BoneMassEntry(
    val time: Instant,
    val massKg: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BodyWaterMassEntry(
    val time: Instant,
    val massKg: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BodyMeasurementEntry(
    val id: String,
    val type: BodyMeasurementType,
    val time: Instant,
    val value: Double,
    val source: String,
    val isOpenVitalsEntry: Boolean,
)
