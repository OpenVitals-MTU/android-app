package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** The form's input units. Distance and elevation resolve independently. */
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
