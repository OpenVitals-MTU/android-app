package tech.mmarca.openvitals.data.repository

import android.content.Context
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.core.preferences.UnitSystem
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val _unitSystem = MutableStateFlow(readUnitSystem())
    private val _appLanguage = MutableStateFlow(readAppLanguage())
    private val _sleepRangeMode = MutableStateFlow(readSleepRangeMode())
    val unitSystemFlow: StateFlow<UnitSystem> = _unitSystem.asStateFlow()
    val appLanguageFlow: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    val sleepRangeModeFlow: StateFlow<SleepRangeMode> = _sleepRangeMode.asStateFlow()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) { prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply() }

    var unitSystem: UnitSystem
        get() = _unitSystem.value
        set(value) {
            prefs.edit().putString(KEY_UNIT_SYSTEM, value.name).apply()
            _unitSystem.value = value
        }

    var trackCycle: Boolean
        get() = prefs.getBoolean(KEY_TRACK_CYCLE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACK_CYCLE, value).apply()
        }

    var appLanguage: AppLanguage
        get() = _appLanguage.value
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()
            _appLanguage.value = value
        }

    var sleepRangeMode: SleepRangeMode
        get() = _sleepRangeMode.value
        set(value) {
            prefs.edit().putString(KEY_SLEEP_RANGE_MODE, value.name).apply()
            _sleepRangeMode.value = value
        }

    var hydrationDailyGoalLiters: Double
        get() = prefs.getFloat(
            KEY_HYDRATION_DAILY_GOAL_LITERS,
            DEFAULT_HYDRATION_DAILY_GOAL_LITERS.toFloat(),
        ).toDouble()
        set(value) {
            prefs.edit()
                .putFloat(
                    KEY_HYDRATION_DAILY_GOAL_LITERS,
                    value.coerceIn(MIN_HYDRATION_DAILY_GOAL_LITERS, MAX_HYDRATION_DAILY_GOAL_LITERS).toFloat(),
                )
                .apply()
        }

    fun timeRangeFor(key: PeriodRangePreferenceKey): TimeRange =
        prefs.getString(key.storageKey, null)
            ?.let { value -> runCatching { TimeRange.valueOf(value) }.getOrNull() }
            ?: key.defaultRange

    fun setTimeRangeFor(key: PeriodRangePreferenceKey, range: TimeRange) {
        prefs.edit().putString(key.storageKey, range.name).apply()
    }

    fun dailyGoalFor(key: MetricDailyGoalKey): Double =
        prefs.getFloat(key.storageKey, key.defaultValue.toFloat()).toDouble()
            .let(key::normalize)

    fun setDailyGoalFor(key: MetricDailyGoalKey, value: Double) {
        prefs.edit()
            .putFloat(key.storageKey, key.normalize(value).toFloat())
            .apply()
    }

    fun dashboardWidgetOrder(): List<String>? =
        prefs.getString(KEY_DASHBOARD_WIDGET_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.filter { it.isNotBlank() }

    fun setDashboardWidgetOrder(widgetIds: List<String>) {
        prefs.edit()
            .putString(KEY_DASHBOARD_WIDGET_ORDER, widgetIds.joinToString(KEY_VALUE_SEPARATOR))
            .apply()
    }

    fun acknowledgedPermissions(): Set<String> =
        prefs.getStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, emptySet()) ?: emptySet()

    fun acknowledgePermissions(permissions: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, acknowledgedPermissions() + permissions)
            .apply()
    }

    private fun readUnitSystem(): UnitSystem =
        prefs.getString(KEY_UNIT_SYSTEM, null)
            ?.let { value -> runCatching { UnitSystem.valueOf(value) }.getOrNull() }
            ?: defaultUnitSystem()

    private fun readAppLanguage(): AppLanguage =
        prefs.getString(KEY_APP_LANGUAGE, null)
            ?.let { value -> runCatching { AppLanguage.valueOf(value) }.getOrNull() }
            ?: AppLanguage.SYSTEM

    private fun readSleepRangeMode(): SleepRangeMode =
        prefs.getString(KEY_SLEEP_RANGE_MODE, null)
            ?.let { value -> runCatching { SleepRangeMode.valueOf(value) }.getOrNull() }
            ?: SleepRangeMode.EVENING_18H

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
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SLEEP_RANGE_MODE = "sleep_range_mode"
        private const val KEY_DASHBOARD_WIDGET_ORDER = "dashboard_widget_order"
        private const val KEY_HYDRATION_DAILY_GOAL_LITERS = "hydration_daily_goal_liters"
        private const val KEY_VALUE_SEPARATOR = ","
        private const val DEFAULT_HYDRATION_DAILY_GOAL_LITERS = 2.0
        private const val MIN_HYDRATION_DAILY_GOAL_LITERS = 0.25
        private const val MAX_HYDRATION_DAILY_GOAL_LITERS = 10.0
        private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")
    }
}
