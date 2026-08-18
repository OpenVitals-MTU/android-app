package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.CustomHydrationDrink

/**
 * A source of preloaded hydration drinks. Each catalog owns its domain
 * (caffeinated drinks, alcoholic drinks, ...) and hands the seed layer
 * ready-made presets, so the seeder never needs to know catalog internals.
 */
interface HydrationDrinkCatalog {
    fun beveragePresets(): List<CustomHydrationDrink>
}
