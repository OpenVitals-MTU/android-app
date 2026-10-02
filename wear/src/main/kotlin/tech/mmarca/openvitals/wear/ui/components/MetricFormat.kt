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
        else -> formatCount(value.roundToInt())
    }

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
    }
