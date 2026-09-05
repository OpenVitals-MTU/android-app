package tech.mmarca.openvitals.features.activity

import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * The split distance as a label, in the user's units. Not
 * [UnitFormatter.distance]: a chosen setting is a round number, not "1.0 km".
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

/** As few decimals as the value needs, up to two. */
private fun splitDistanceLabelDecimals(value: Double): Int =
    when {
        abs(value - value.roundToInt()) < 1e-6 -> 0
        abs(value * 10 - (value * 10).roundToInt()) < 1e-6 -> 1
        else -> 2
    }
