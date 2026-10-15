package tech.mmarca.openvitals.domain.preferences

/**
 * The stored unit-system choice. [SYSTEM] follows the OS preference. The
 * explicit values match the old [UnitSystem] names, so saved choices survive.
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
