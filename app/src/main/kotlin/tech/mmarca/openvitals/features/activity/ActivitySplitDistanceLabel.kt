package tech.mmarca.openvitals.features.activity

import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * "1 km" / "0.5 km" / "0.25 mi" — the split distance as a chip label or a card
 * header, in the user's unit system.
 *
 * NOT [UnitFormatter.distance]: that always prints one decimal ("1.0 km"),
 * which is right for a measured distance and wrong for a chosen setting. The
 * split distance is a round number the user picked, so it is printed as one.
 *
 * Shared by the settings chips and the splits-card header so the two can never
 * disagree about what "every 1 km" means.
 */
internal fun splitDistanceLabel(unitFormatter: UnitFormatter, meters: Double): String =
    when (unitFormatter.unitSystem(UnitQuantity.DISTANCE)) {
        UnitSystem.METRIC -> {
            val kilometers = meters / 1000.0
            "${unitFormatter.decimal(kilometers, splitDistanceLabelDecimals(kilometers))} km"
        }
        UnitSystem.IMPERIAL -> {
            val miles = meters / 1609.344
            "${unitFormatter.decimal(miles, splitDistanceLabelDecimals(miles))} mi"
        }
    }

/**
 * As few decimals as the value can be written in, up to two: 1 -> "1",
 * 0.5 -> "0.5", 0.25 -> "0.25".
 */
private fun splitDistanceLabelDecimals(value: Double): Int =
    when {
        abs(value - value.roundToInt()) < 1e-6 -> 0
        abs(value * 10 - (value * 10).roundToInt()) < 1e-6 -> 1
        else -> 2
    }
