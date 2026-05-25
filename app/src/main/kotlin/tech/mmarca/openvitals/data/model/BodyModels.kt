package tech.mmarca.openvitals.data.model

import java.time.Instant

enum class BodyMeasurementType {
    WEIGHT,
    HEIGHT,
    BODY_FAT,
}

data class BodyMeasurementWriteRequest(
    val type: BodyMeasurementType,
    val time: Instant,
    val value: Double,
)

data class WeightEntry(
    val time: Instant,
    val weightKg: Double,
    val source: String,
)

data class HeightEntry(
    val time: Instant,
    val heightCm: Double,
    val source: String,
)

data class BodyFatEntry(
    val time: Instant,
    val percent: Double,
    val source: String,
)

data class LeanBodyMassEntry(
    val time: Instant,
    val massKg: Double,
    val source: String,
)

data class BmrEntry(
    val time: Instant,
    val kcalPerDay: Double,
    val source: String,
)

data class BoneMassEntry(
    val time: Instant,
    val massKg: Double,
    val source: String,
)
