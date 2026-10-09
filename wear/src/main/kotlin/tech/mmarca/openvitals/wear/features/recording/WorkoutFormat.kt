package tech.mmarca.openvitals.wear.features.recording

import androidx.annotation.StringRes
import java.util.Locale
import kotlin.math.roundToInt
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem

private const val MetersPerKilometer = 1000.0
private const val MetersPerMile = 1609.344
private const val SecondsPerMinute = 60

/** Minutes and seconds per kilometre or mile, e.g. "5:42". Null when standing still. */
fun formatPace(speedMetersPerSecond: Double, unitSystem: UnitSystem): String? {
    if (speedMetersPerSecond <= 0.1) return null
    val meters = if (unitSystem == UnitSystem.IMPERIAL) MetersPerMile else MetersPerKilometer
    val seconds = (meters / speedMetersPerSecond).roundToInt()
    return String.format(Locale.getDefault(), "%d:%02d", seconds / SecondsPerMinute, seconds % SecondsPerMinute)
}

@StringRes
fun paceUnit(unitSystem: UnitSystem): Int =
    if (unitSystem == UnitSystem.IMPERIAL) R.string.unit_pace_mi else R.string.unit_pace_km

/** Kilometres or miles per hour, one decimal. */
fun formatSpeed(speedMetersPerSecond: Double, unitSystem: UnitSystem): String {
    val meters = if (unitSystem == UnitSystem.IMPERIAL) MetersPerMile else MetersPerKilometer
    return String.format(Locale.getDefault(), "%.1f", speedMetersPerSecond * 3600 / meters)
}

@StringRes
fun speedUnit(unitSystem: UnitSystem): Int =
    if (unitSystem == UnitSystem.IMPERIAL) R.string.unit_mph else R.string.unit_kmh
