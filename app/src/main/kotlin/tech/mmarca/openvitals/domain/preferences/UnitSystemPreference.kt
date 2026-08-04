package tech.mmarca.openvitals.domain.preferences

/**
 * The stored unit-system choice. [SYSTEM] follows the OS measurement-system
 * regional preference and resolves to a concrete [UnitSystem] at display time;
 * [METRIC] and [IMPERIAL] are explicit overrides. The explicit storage values
 * match the [UnitSystem] names the app stored before [SYSTEM] existed, so a
 * previously saved choice keeps reading back as that explicit choice.
 */
enum class UnitSystemPreference(val storageValue: String) {
    SYSTEM("SYSTEM"),
    METRIC("METRIC"),
    IMPERIAL("IMPERIAL"),
    ;

    /** [systemUnitSystem] is only consulted for [SYSTEM]. */
    fun resolve(systemUnitSystem: () -> UnitSystem): UnitSystem =
        when (this) {
            SYSTEM -> systemUnitSystem()
            METRIC -> UnitSystem.METRIC
            IMPERIAL -> UnitSystem.IMPERIAL
        }

    companion object {
        fun fromStorageValue(value: String?): UnitSystemPreference? =
            entries.firstOrNull { it.storageValue == value }
    }
}
