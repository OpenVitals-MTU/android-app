package tech.mmarca.openvitals.data.model

import java.time.Instant

data class WeightEntry(
    val time: Instant,
    val weightKg: Double,
    val source: String,
)

data class BodyFatEntry(
    val time: Instant,
    val percent: Double,
    val source: String,
)
