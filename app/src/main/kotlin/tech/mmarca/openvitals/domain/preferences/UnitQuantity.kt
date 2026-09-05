package tech.mmarca.openvitals.domain.preferences

/** A quantity whose display units can be overridden. Stored data stays metric. */
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
