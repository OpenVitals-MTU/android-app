package tech.mmarca.openvitals.wear.ui.components

import androidx.annotation.StringRes
import java.text.NumberFormat
import java.util.Locale
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem

private const val MetersPerKilometer = 1000.0
private const val MetersPerMile = 1609.344

fun formatCount(value: Int): String = NumberFormat.getIntegerInstance().format(value)

/** Kilometres or miles with one decimal. The unit label is [distanceUnitLabel]. */
fun formatDistance(meters: Int, unitSystem: UnitSystem): String {
    val metersPerUnit = when (unitSystem) {
        UnitSystem.METRIC -> MetersPerKilometer
        UnitSystem.IMPERIAL -> MetersPerMile
    }
    return String.format(Locale.getDefault(), "%.1f", meters / metersPerUnit)
}

@StringRes
fun distanceUnitLabel(unitSystem: UnitSystem): Int =
    when (unitSystem) {
        UnitSystem.METRIC -> R.string.unit_km
        UnitSystem.IMPERIAL -> R.string.unit_mi
    }
