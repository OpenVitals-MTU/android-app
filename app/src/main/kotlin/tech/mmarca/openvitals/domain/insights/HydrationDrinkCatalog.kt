package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

/** A source of preloaded hydration drinks. Each catalog owns its domain. */
interface HydrationDrinkCatalog {
    fun beveragePresets(): List<CustomHydrationDrink>
}
