package tech.mmarca.openvitals.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.mmarca.openvitals.core.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.preferences.ActivityWeekMode
import tech.mmarca.openvitals.core.preferences.AppLanguage
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.data.model.HydrationReminderConfig
import tech.mmarca.openvitals.data.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.data.model.MindfulnessBellSound
import tech.mmarca.openvitals.data.model.MindfulnessTimerConfig
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val _unitSystem = MutableStateFlow(readUnitSystem())
    private val _appLanguage = MutableStateFlow(readAppLanguage())
    private val _sleepRangeMode = MutableStateFlow(readSleepRangeMode())
    private val _activityWeekMode = MutableStateFlow(readActivityWeekMode())
    val unitSystemFlow: StateFlow<UnitSystem> = _unitSystem.asStateFlow()
    val appLanguageFlow: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    val sleepRangeModeFlow: StateFlow<SleepRangeMode> = _sleepRangeMode.asStateFlow()
    val activityWeekModeFlow: StateFlow<ActivityWeekMode> = _activityWeekMode.asStateFlow()

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

    var activityWeekMode: ActivityWeekMode
        get() = _activityWeekMode.value
        set(value) {
            prefs.edit().putString(KEY_ACTIVITY_WEEK_MODE, value.name).apply()
            _activityWeekMode.value = value
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

    var hydrationContainerMilliliters: Double
        get() = prefs.getFloat(
            KEY_HYDRATION_CONTAINER_MILLILITERS,
            DEFAULT_HYDRATION_CONTAINER_MILLILITERS.toFloat(),
        ).toDouble()
            .coerceIn(MIN_HYDRATION_CONTAINER_MILLILITERS, MAX_HYDRATION_CONTAINER_MILLILITERS)
        set(value) {
            prefs.edit()
                .putFloat(
                    KEY_HYDRATION_CONTAINER_MILLILITERS,
                    value.coerceIn(
                        MIN_HYDRATION_CONTAINER_MILLILITERS,
                        MAX_HYDRATION_CONTAINER_MILLILITERS,
                    ).toFloat(),
                )
                .apply()
        }

    var highHeartRateThresholdBpm: Int
        get() = prefs.getInt(
            KEY_HIGH_HEART_RATE_THRESHOLD_BPM,
            DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
        ).coerceIn(MIN_HIGH_HEART_RATE_THRESHOLD_BPM, MAX_HIGH_HEART_RATE_THRESHOLD_BPM)
        set(value) {
            prefs.edit()
                .putInt(
                    KEY_HIGH_HEART_RATE_THRESHOLD_BPM,
                    value.coerceIn(MIN_HIGH_HEART_RATE_THRESHOLD_BPM, MAX_HIGH_HEART_RATE_THRESHOLD_BPM),
                )
                .apply()
        }

    var lowHeartRateThresholdBpm: Int
        get() = prefs.getInt(
            KEY_LOW_HEART_RATE_THRESHOLD_BPM,
            DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
        ).coerceIn(MIN_LOW_HEART_RATE_THRESHOLD_BPM, MAX_LOW_HEART_RATE_THRESHOLD_BPM)
        set(value) {
            prefs.edit()
                .putInt(
                    KEY_LOW_HEART_RATE_THRESHOLD_BPM,
                    value.coerceIn(MIN_LOW_HEART_RATE_THRESHOLD_BPM, MAX_LOW_HEART_RATE_THRESHOLD_BPM),
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

    fun hydrationReminderConfig(): HydrationReminderConfig =
        HydrationReminderConfig(
            enabled = prefs.getBoolean(KEY_HYDRATION_REMINDERS_ENABLED, false),
            intervalMinutes = prefs.getInt(
                KEY_HYDRATION_REMINDER_INTERVAL_MINUTES,
                HydrationReminderConfig.DefaultIntervalMinutes,
            ),
            activeStartTime = prefs.getString(KEY_HYDRATION_REMINDER_ACTIVE_START_TIME, null)
                .toHydrationReminderTimeOrDefault(HydrationReminderConfig.DefaultActiveStartTime),
            activeEndTime = prefs.getString(KEY_HYDRATION_REMINDER_ACTIVE_END_TIME, null)
                .toHydrationReminderTimeOrDefault(HydrationReminderConfig.DefaultActiveEndTime),
        ).normalized()

    fun setHydrationReminderConfig(config: HydrationReminderConfig) {
        val normalized = config.normalized()
        prefs.edit()
            .putBoolean(KEY_HYDRATION_REMINDERS_ENABLED, normalized.enabled)
            .putInt(KEY_HYDRATION_REMINDER_INTERVAL_MINUTES, normalized.intervalMinutes)
            .putString(KEY_HYDRATION_REMINDER_ACTIVE_START_TIME, normalized.activeStartTime.toString())
            .putString(KEY_HYDRATION_REMINDER_ACTIVE_END_TIME, normalized.activeEndTime.toString())
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

    fun manualEntryWidgetOrder(): List<String>? =
        prefs.getString(KEY_MANUAL_ENTRY_WIDGET_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.filter { it.isNotBlank() }

    fun setManualEntryWidgetOrder(widgetIds: List<String>) {
        prefs.edit()
            .putString(KEY_MANUAL_ENTRY_WIDGET_ORDER, widgetIds.joinToString(KEY_VALUE_SEPARATOR))
            .apply()
    }

    fun acknowledgedPermissions(): Set<String> =
        prefs.getStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, emptySet()) ?: emptySet()

    fun acknowledgePermissions(permissions: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, acknowledgedPermissions() + permissions)
            .apply()
    }

    fun mindfulnessTimerConfig(): MindfulnessTimerConfig =
        MindfulnessTimerConfig(
            durationMinutes = prefs.getInt(
                KEY_MINDFULNESS_TIMER_DURATION_MINUTES,
                DEFAULT_MINDFULNESS_TIMER_DURATION_MINUTES,
            ).coerceIn(MIN_MINDFULNESS_TIMER_MINUTES, MAX_MINDFULNESS_TIMER_MINUTES),
            intervalMinutes = prefs.getInt(KEY_MINDFULNESS_TIMER_INTERVAL_MINUTES, 0)
                .takeIf { it > 0 }
                ?.coerceIn(MIN_MINDFULNESS_TIMER_MINUTES, MAX_MINDFULNESS_TIMER_MINUTES),
            bellSound = prefs.getString(KEY_MINDFULNESS_TIMER_BELL_SOUND, null)
                ?.toMindfulnessBellSound()
                ?: MindfulnessBellSound.STRUCK,
            backgroundSound = prefs.getString(KEY_MINDFULNESS_TIMER_BACKGROUND_SOUND, null)
                ?.toMindfulnessBackgroundSound()
                ?: MindfulnessBackgroundSound.NONE,
        ).let { config ->
            config.copy(intervalMinutes = config.intervalMinutes?.takeIf { it < config.durationMinutes })
        }

    fun setMindfulnessTimerConfig(config: MindfulnessTimerConfig) {
        val duration = config.durationMinutes.coerceIn(
            MIN_MINDFULNESS_TIMER_MINUTES,
            MAX_MINDFULNESS_TIMER_MINUTES,
        )
        val interval = config.intervalMinutes
            ?.coerceIn(MIN_MINDFULNESS_TIMER_MINUTES, (duration - 1).coerceAtLeast(MIN_MINDFULNESS_TIMER_MINUTES))
            ?.takeIf { duration > MIN_MINDFULNESS_TIMER_MINUTES }
        prefs.edit()
            .putInt(KEY_MINDFULNESS_TIMER_DURATION_MINUTES, duration)
            .putInt(KEY_MINDFULNESS_TIMER_INTERVAL_MINUTES, interval ?: 0)
            .putString(KEY_MINDFULNESS_TIMER_BELL_SOUND, config.bellSound.name)
            .putString(KEY_MINDFULNESS_TIMER_BACKGROUND_SOUND, config.backgroundSound.name)
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

    private fun readActivityWeekMode(): ActivityWeekMode =
        prefs.getString(KEY_ACTIVITY_WEEK_MODE, null)
            ?.let { value -> runCatching { ActivityWeekMode.valueOf(value) }.getOrNull() }
            ?: ActivityWeekMode.MONDAY_TO_SUNDAY

    private fun defaultUnitSystem(): UnitSystem {
        val country = Locale.getDefault().country.uppercase(Locale.US)
        return if (country in IMPERIAL_COUNTRIES) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }

    private fun String.toMindfulnessBellSound(): MindfulnessBellSound? =
        when (this) {
            "SOFT" -> MindfulnessBellSound.STRUCK
            "DEEP" -> MindfulnessBellSound.TEMPLE
            else -> runCatching { MindfulnessBellSound.valueOf(this) }.getOrNull()
        }

    private fun String.toMindfulnessBackgroundSound(): MindfulnessBackgroundSound? =
        runCatching { MindfulnessBackgroundSound.valueOf(this) }.getOrNull()

    private fun String?.toHydrationReminderTimeOrDefault(default: LocalTime): LocalTime =
        this?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() } ?: default

    companion object {
        const val PREFS_FILE = "openvitals_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ACKNOWLEDGED_PERMISSIONS = "acknowledged_permissions"
        private const val KEY_UNIT_SYSTEM = "unit_system"
        private const val KEY_TRACK_CYCLE = "track_cycle"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SLEEP_RANGE_MODE = "sleep_range_mode"
        private const val KEY_ACTIVITY_WEEK_MODE = "activity_week_mode"
        private const val KEY_DASHBOARD_WIDGET_ORDER = "dashboard_widget_order"
        private const val KEY_MANUAL_ENTRY_WIDGET_ORDER = "manual_entry_widget_order"
        private const val KEY_HYDRATION_DAILY_GOAL_LITERS = "hydration_daily_goal_liters"
        private const val KEY_HYDRATION_CONTAINER_MILLILITERS = "hydration_container_milliliters"
        private const val KEY_HYDRATION_REMINDERS_ENABLED = "hydration_reminders_enabled"
        private const val KEY_HYDRATION_REMINDER_INTERVAL_MINUTES = "hydration_reminder_interval_minutes"
        private const val KEY_HYDRATION_REMINDER_ACTIVE_START_TIME = "hydration_reminder_active_start_time"
        private const val KEY_HYDRATION_REMINDER_ACTIVE_END_TIME = "hydration_reminder_active_end_time"
        private const val KEY_HIGH_HEART_RATE_THRESHOLD_BPM = "high_heart_rate_threshold_bpm"
        private const val KEY_LOW_HEART_RATE_THRESHOLD_BPM = "low_heart_rate_threshold_bpm"
        private const val KEY_MINDFULNESS_TIMER_DURATION_MINUTES = "mindfulness_timer_duration_minutes"
        private const val KEY_MINDFULNESS_TIMER_INTERVAL_MINUTES = "mindfulness_timer_interval_minutes"
        private const val KEY_MINDFULNESS_TIMER_BELL_SOUND = "mindfulness_timer_bell_sound"
        private const val KEY_MINDFULNESS_TIMER_BACKGROUND_SOUND = "mindfulness_timer_background_sound"
        private const val KEY_VALUE_SEPARATOR = ","
        private const val DEFAULT_HYDRATION_DAILY_GOAL_LITERS = 2.0
        private const val MIN_HYDRATION_DAILY_GOAL_LITERS = 0.25
        private const val MAX_HYDRATION_DAILY_GOAL_LITERS = 10.0
        private const val DEFAULT_HYDRATION_CONTAINER_MILLILITERS = 350.0
        private const val MIN_HYDRATION_CONTAINER_MILLILITERS = 1.0
        private const val MAX_HYDRATION_CONTAINER_MILLILITERS = 100_000.0
        const val DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM = 120
        const val DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM = 50
        const val MIN_HIGH_HEART_RATE_THRESHOLD_BPM = 80
        const val MAX_HIGH_HEART_RATE_THRESHOLD_BPM = 220
        const val MIN_LOW_HEART_RATE_THRESHOLD_BPM = 30
        const val MAX_LOW_HEART_RATE_THRESHOLD_BPM = 100
        private const val DEFAULT_MINDFULNESS_TIMER_DURATION_MINUTES = 10
        private const val MIN_MINDFULNESS_TIMER_MINUTES = 1
        private const val MAX_MINDFULNESS_TIMER_MINUTES = 24 * 60
        private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")
    }
}
