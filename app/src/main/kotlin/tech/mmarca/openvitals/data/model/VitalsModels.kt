package tech.mmarca.openvitals.data.model

import java.time.Instant

data class BloodPressureEntry(
    val time: Instant,
    val systolicMmHg: Int,
    val diastolicMmHg: Int,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class SpO2Entry(
    val time: Instant,
    val percent: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class RespiratoryRateEntry(
    val time: Instant,
    val breathsPerMinute: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class BodyTempEntry(
    val time: Instant,
    val temperatureCelsius: Double,
    val source: String,
    val id: String = "",
    val isOpenVitalsEntry: Boolean = false,
)

data class Vo2MaxEntry(
    val time: Instant,
    val vo2MaxMlPerKgPerMin: Double,
    val source: String,
)

enum class VitalsMeasurementType {
    BLOOD_PRESSURE,
    SPO2,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
}

data class VitalsMeasurementWriteRequest(
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
)

data class VitalsMeasurementEntry(
    val id: String,
    val type: VitalsMeasurementType,
    val time: Instant,
    val value: Double,
    val secondaryValue: Double? = null,
    val source: String,
    val isOpenVitalsEntry: Boolean,
)
