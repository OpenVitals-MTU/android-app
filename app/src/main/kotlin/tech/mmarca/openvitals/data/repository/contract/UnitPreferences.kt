package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** What values display in. Stored health data stays metric. */
interface UnitPreferences {

    /** Already resolved: never the SYSTEM preference itself. */
    val unitSystem: UnitSystem

    /** The override for one quantity, or null when it follows [unitSystem]. */
    fun unitOverride(quantity: UnitQuantity): UnitSystem?
}
