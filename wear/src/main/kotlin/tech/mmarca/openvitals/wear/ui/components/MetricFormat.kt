package tech.mmarca.openvitals.wear.ui.components

import androidx.annotation.StringRes
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric

private const val MetersPerKilometer = 1000.0
private const val MetersPerMile = 1609.344
private const val FeetPerMeter = 3.28084
private const val PoundsPerKilogram = 2.20462
private const val MillilitresPerFluidOunce = 29.5735
private const val MinutesPerHour = 60

fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

/** [value] is in the metric's base unit. The matching unit label is [metricUnitLabel]. */
fun formatMetricValue(metric: WearMetric, value: Double, unitSystem: UnitSystem): String =
    when (metric) {
        WearMetric.DISTANCE -> {
            val metersPerUnit = when (unitSystem) {
                UnitSystem.METRIC -> MetersPerKilometer
                UnitSystem.IMPERIAL -> MetersPerMile
            }
            String.format(Locale.getDefault(), "%.1f", value / metersPerUnit)
        }
        WearMetric.ELEVATION -> when (unitSystem) {
            UnitSystem.METRIC -> formatCount(value.roundToInt())
            UnitSystem.IMPERIAL -> formatCount((value * FeetPerMeter).roundToInt())
        }
        WearMetric.SLEEP -> formatHoursMinutes(value)
        WearMetric.WEIGHT -> when (unitSystem) {
            UnitSystem.METRIC -> formatOneDecimal(value)
            UnitSystem.IMPERIAL -> formatOneDecimal(value * PoundsPerKilogram)
        }
        WearMetric.HYDRATION -> when (unitSystem) {
            UnitSystem.METRIC -> formatCount(value.roundToInt())
            UnitSystem.IMPERIAL -> formatCount((value / MillilitresPerFluidOunce).roundToInt())
        }
        else -> formatCount(value.roundToInt())
    }

/** [kilograms] in the unit system's body weight unit, one decimal. */
fun formatWeight(kilograms: Double, unitSystem: UnitSystem): String =
    formatMetricValue(WearMetric.WEIGHT, kilograms, unitSystem)

/** "7:05" for 425 minutes. */
fun formatHoursMinutes(minutes: Double): String {
    val total = minutes.roundToInt()
    return String.format(Locale.getDefault(), "%d:%02d", total / MinutesPerHour, total % MinutesPerHour)
}

fun formatOneDecimal(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

@StringRes
fun metricUnitLabel(metric: WearMetric, unitSystem: UnitSystem): Int =
    when (metric) {
        WearMetric.STEPS -> R.string.unit_steps
        WearMetric.HEART_RATE -> R.string.unit_bpm
        WearMetric.DISTANCE -> when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_km
            UnitSystem.IMPERIAL -> R.string.unit_mi
        }
        WearMetric.ACTIVE_CALORIES -> R.string.unit_kcal
        WearMetric.FLOORS -> R.string.unit_floors
        WearMetric.ELEVATION -> when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_m
            UnitSystem.IMPERIAL -> R.string.unit_ft
        }
        WearMetric.BLOOD_OXYGEN -> R.string.unit_percent
        WearMetric.VO2_MAX -> R.string.unit_vo2_max
        WearMetric.RESTING_HEART_RATE -> R.string.unit_bpm
        WearMetric.HRV -> R.string.unit_ms
        WearMetric.SLEEP -> R.string.unit_hours
        WearMetric.HYDRATION -> when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_ml
            UnitSystem.IMPERIAL -> R.string.unit_fl_oz
        }
        WearMetric.WEIGHT -> when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kg
            UnitSystem.IMPERIAL -> R.string.unit_lb
        }
    }
