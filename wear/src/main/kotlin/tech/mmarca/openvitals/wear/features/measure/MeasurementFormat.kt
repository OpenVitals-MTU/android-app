package tech.mmarca.openvitals.wear.features.measure

import androidx.annotation.StringRes
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.ui.components.formatCount

private const val FeetPerMeter = 3.28084

fun formatMeasurement(value: Double): String = formatCount(value.roundToInt())

@StringRes
fun measurementUnit(measurement: Measurement): Int = when (measurement) {
    Measurement.HEART_RATE -> R.string.unit_bpm
    Measurement.HRV -> R.string.unit_ms
    Measurement.AIR_PRESSURE -> R.string.unit_hpa
}

/** The altitude that goes with a pressure reading, in the unit system's length unit. */
fun formatAltitude(meters: Double, unitSystem: UnitSystem): Pair<String, Int> = when (unitSystem) {
    UnitSystem.METRIC -> formatCount(meters.roundToInt()) to R.string.unit_m
    UnitSystem.IMPERIAL -> formatCount((meters * FeetPerMeter).roundToInt()) to R.string.unit_ft
}
