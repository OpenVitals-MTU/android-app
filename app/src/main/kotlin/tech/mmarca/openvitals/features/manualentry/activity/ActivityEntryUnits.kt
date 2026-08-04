package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * The effective input units for the activity entry form. Distance and
 * elevation resolve independently — each quantity's display override applies
 * to its own field — so the form threads this pair where a single
 * [UnitSystem] used to serve both.
 */
data class ActivityEntryUnits(
    val distance: UnitSystem,
    val elevation: UnitSystem,
) {
    companion object {
        fun from(unitFormatter: UnitFormatter): ActivityEntryUnits =
            ActivityEntryUnits(
                distance = unitFormatter.unitSystem(UnitQuantity.DISTANCE),
                elevation = unitFormatter.unitSystem(UnitQuantity.ELEVATION),
            )

        /** Both quantities in one system — for tests and metric-only fallbacks. */
        fun uniform(unitSystem: UnitSystem): ActivityEntryUnits =
            ActivityEntryUnits(distance = unitSystem, elevation = unitSystem)
    }
}
