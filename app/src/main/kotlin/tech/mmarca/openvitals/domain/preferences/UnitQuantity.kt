package tech.mmarca.openvitals.domain.preferences

/**
 * A measured quantity whose display units can be overridden independently of
 * the base unit system. Overrides act only at the formatting boundary — stored
 * health data stays metric. Quantities the formatter renders identically in
 * both systems (heart rate, energy, blood pressure, ...) have no entry here.
 */
enum class UnitQuantity(val storageKey: String) {
    /** Distances and the speed/pace units derived from them. */
    DISTANCE("distance"),
    ELEVATION("elevation"),
    WEIGHT("weight"),
    HEIGHT("height"),
    TEMPERATURE("temperature"),
    HYDRATION("hydration"),
    BLOOD_GLUCOSE("blood_glucose"),
}
