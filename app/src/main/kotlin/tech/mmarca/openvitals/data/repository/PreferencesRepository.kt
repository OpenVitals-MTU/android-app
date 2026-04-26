package tech.mmarca.openvitals.data.repository

import android.content.Context
import tech.mmarca.openvitals.core.preferences.UnitSystem
import java.util.Locale

class PreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) { prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply() }

    var unitSystem: UnitSystem
        get() = prefs.getString(KEY_UNIT_SYSTEM, null)
            ?.let { value -> runCatching { UnitSystem.valueOf(value) }.getOrNull() }
            ?: defaultUnitSystem()
        set(value) {
            prefs.edit().putString(KEY_UNIT_SYSTEM, value.name).apply()
        }

    var trackCycle: Boolean
        get() = prefs.getBoolean(KEY_TRACK_CYCLE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACK_CYCLE, value).apply()
        }

    fun acknowledgedPermissions(): Set<String> =
        prefs.getStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, emptySet()) ?: emptySet()

    fun acknowledgePermissions(permissions: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, acknowledgedPermissions() + permissions)
            .apply()
    }

    private fun defaultUnitSystem(): UnitSystem {
        val country = Locale.getDefault().country.uppercase(Locale.US)
        return if (country in IMPERIAL_COUNTRIES) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }

    companion object {
        const val PREFS_FILE = "openvitals_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ACKNOWLEDGED_PERMISSIONS = "acknowledged_permissions"
        private const val KEY_UNIT_SYSTEM = "unit_system"
        private const val KEY_TRACK_CYCLE = "track_cycle"
        private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")
    }
}
