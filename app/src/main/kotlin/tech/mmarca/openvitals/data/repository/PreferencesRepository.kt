package tech.mmarca.openvitals.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.toWeekPeriodMode
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.domain.model.MindfulnessBellSound
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessTimerConfig
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val _unitSystem = MutableStateFlow(readUnitSystem())
    private val _appLanguage = MutableStateFlow(readAppLanguage())
    private val _appThemeMode = MutableStateFlow(readAppThemeMode())
    private val _sleepRangeMode = MutableStateFlow(readSleepRangeMode())
    private val _activityWeekMode = MutableStateFlow(readActivityWeekMode())
    private val _showOpenVitalsCalculatedCalories = MutableStateFlow(readShowOpenVitalsCalculatedCalories())
    private val _healthConnectSyncEnabled = MutableStateFlow(readHealthConnectSyncEnabled())
    private val _bodyEnergyCalibration = MutableStateFlow(readBodyEnergyCalibration())
    val unitSystemFlow: StateFlow<UnitSystem> = _unitSystem.asStateFlow()
    val appLanguageFlow: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    val appThemeModeFlow: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()
    val sleepRangeModeFlow: StateFlow<SleepRangeMode> = _sleepRangeMode.asStateFlow()
    val activityWeekModeFlow: StateFlow<ActivityWeekMode> = _activityWeekMode.asStateFlow()
    val weekPeriodModeFlow = activityWeekModeFlow.map { it.toWeekPeriodMode() }
    val showOpenVitalsCalculatedCaloriesFlow: StateFlow<Boolean> = _showOpenVitalsCalculatedCalories.asStateFlow()
    val healthConnectSyncEnabledFlow: StateFlow<Boolean> = _healthConnectSyncEnabled.asStateFlow()
    val bodyEnergyCalibrationFlow: StateFlow<BodyEnergyCalibration> = _bodyEnergyCalibration.asStateFlow()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) { prefs.edit { putBoolean(KEY_ONBOARDING_DONE, value) } }

    var unitSystem: UnitSystem
        get() = _unitSystem.value
        set(value) {
            prefs.edit { putString(KEY_UNIT_SYSTEM, value.name) }
            _unitSystem.value = value
        }

    var appLanguage: AppLanguage
        get() = _appLanguage.value
        set(value) {
            prefs.edit { putString(KEY_APP_LANGUAGE, value.name) }
            _appLanguage.value = value
        }

    var appThemeMode: AppThemeMode
        get() = _appThemeMode.value
        set(value) {
            prefs.edit { putString(KEY_APP_THEME_MODE, value.name) }
            _appThemeMode.value = value
        }

    var sleepRangeMode: SleepRangeMode
        get() = _sleepRangeMode.value
        set(value) {
            prefs.edit { putString(KEY_SLEEP_RANGE_MODE, value.name) }
            _sleepRangeMode.value = value
        }

    var activityWeekMode: ActivityWeekMode
        get() = _activityWeekMode.value
        set(value) {
            prefs.edit { putString(KEY_ACTIVITY_WEEK_MODE, value.name) }
            _activityWeekMode.value = value
        }

    val weekPeriodMode: WeekPeriodMode
        get() = activityWeekMode.toWeekPeriodMode()

    var showOpenVitalsCalculatedCalories: Boolean
        get() = _showOpenVitalsCalculatedCalories.value
        set(value) {
            prefs.edit { putBoolean(KEY_SHOW_OPENVITALS_CALCULATED_CALORIES, value) }
            _showOpenVitalsCalculatedCalories.value = value
        }

    var healthConnectSyncEnabled: Boolean
        get() = _healthConnectSyncEnabled.value
        set(value) {
            prefs.edit { putBoolean(KEY_HEALTH_CONNECT_SYNC_ENABLED, value) }
            _healthConnectSyncEnabled.value = value
        }

    var healthConnectPermissionCancelCount: Int
        get() = prefs.getInt(KEY_HEALTH_CONNECT_PERMISSION_CANCEL_COUNT, 0)
        set(value) {
            prefs.edit { putInt(KEY_HEALTH_CONNECT_PERMISSION_CANCEL_COUNT, value.coerceAtLeast(0)) }
        }

    var acceptedPrivacyPolicyVersion: String?
        get() = prefs.getString(KEY_ACCEPTED_PRIVACY_POLICY_VERSION, null)
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_ACCEPTED_PRIVACY_POLICY_VERSION)
                } else {
                    putString(KEY_ACCEPTED_PRIVACY_POLICY_VERSION, value)
                }
            }
        }

    var privacyPolicyAcceptedAtMillis: Long
        get() = prefs.getLong(KEY_PRIVACY_POLICY_ACCEPTED_AT, 0L)
        set(value) {
            prefs.edit { putLong(KEY_PRIVACY_POLICY_ACCEPTED_AT, value) }
        }

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_APP_LOCK_ENABLED, value) }
        }

    var lastActivityExerciseType: Int?
        get() = prefs.getInt(KEY_LAST_ACTIVITY_EXERCISE_TYPE, MISSING_EXERCISE_TYPE)
            .takeIf { it != MISSING_EXERCISE_TYPE }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_LAST_ACTIVITY_EXERCISE_TYPE)
                } else {
                    putInt(KEY_LAST_ACTIVITY_EXERCISE_TYPE, value)
                }
            }
        }

    var favoriteActivityExerciseType: Int?
        get() = prefs.getInt(KEY_FAVORITE_ACTIVITY_EXERCISE_TYPE, MISSING_EXERCISE_TYPE)
            .takeIf { it != MISSING_EXERCISE_TYPE }
        set(value) {
            prefs.edit {
                if (value == null) {
                    remove(KEY_FAVORITE_ACTIVITY_EXERCISE_TYPE)
                } else {
                    putInt(KEY_FAVORITE_ACTIVITY_EXERCISE_TYPE, value)
                }
            }
        }

    var hydrationDailyGoalLiters: Double
        get() = prefs.getFloat(
            KEY_HYDRATION_DAILY_GOAL_LITERS,
            DEFAULT_HYDRATION_DAILY_GOAL_LITERS.toFloat(),
        ).toDouble()
        set(value) {
            prefs.edit {
                putFloat(
                    KEY_HYDRATION_DAILY_GOAL_LITERS,
                    value.coerceIn(MIN_HYDRATION_DAILY_GOAL_LITERS, MAX_HYDRATION_DAILY_GOAL_LITERS).toFloat(),
                )
            }
        }

    var highHeartRateThresholdBpm: Int
        get() = prefs.getInt(
            KEY_HIGH_HEART_RATE_THRESHOLD_BPM,
            DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
        ).coerceIn(MIN_HIGH_HEART_RATE_THRESHOLD_BPM, MAX_HIGH_HEART_RATE_THRESHOLD_BPM)
        set(value) {
            prefs.edit {
                putInt(
                    KEY_HIGH_HEART_RATE_THRESHOLD_BPM,
                    value.coerceIn(MIN_HIGH_HEART_RATE_THRESHOLD_BPM, MAX_HIGH_HEART_RATE_THRESHOLD_BPM),
                )
            }
        }

    var lowHeartRateThresholdBpm: Int
        get() = prefs.getInt(
            KEY_LOW_HEART_RATE_THRESHOLD_BPM,
            DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
        ).coerceIn(MIN_LOW_HEART_RATE_THRESHOLD_BPM, MAX_LOW_HEART_RATE_THRESHOLD_BPM)
        set(value) {
            prefs.edit {
                putInt(
                    KEY_LOW_HEART_RATE_THRESHOLD_BPM,
                    value.coerceIn(MIN_LOW_HEART_RATE_THRESHOLD_BPM, MAX_LOW_HEART_RATE_THRESHOLD_BPM),
                )
            }
        }

    fun bodyEnergyCalibration(): BodyEnergyCalibration = _bodyEnergyCalibration.value

    fun setBodyEnergyCalibration(calibration: BodyEnergyCalibration) {
        val normalized = calibration.normalized()
        prefs.edit {
            putBoolean(KEY_BODY_ENERGY_USE_MANUAL_ZONES, normalized.useManualZones)
            normalized.birthYear?.let { putInt(KEY_BODY_ENERGY_BIRTH_YEAR, it) }
                ?: remove(KEY_BODY_ENERGY_BIRTH_YEAR)
            normalized.manualMaxHeartRateBpm?.let { putInt(KEY_BODY_ENERGY_MAX_HR_BPM, it) }
                ?: remove(KEY_BODY_ENERGY_MAX_HR_BPM)
            normalized.manualRestingHeartRateBpm?.let { putInt(KEY_BODY_ENERGY_RESTING_HR_BPM, it) }
                ?: remove(KEY_BODY_ENERGY_RESTING_HR_BPM)
            normalized.manualZoneThresholdsBpm?.let {
                putString(KEY_BODY_ENERGY_ZONE_THRESHOLDS_BPM, it.toPreferenceString())
            } ?: remove(KEY_BODY_ENERGY_ZONE_THRESHOLDS_BPM)
        }
        _bodyEnergyCalibration.value = normalized
    }

    fun timeRangeFor(key: PeriodRangePreferenceKey): TimeRange =
        prefs.getString(key.storageKey, null)
            ?.let { value -> runCatching { TimeRange.valueOf(value) }.getOrNull() }
            ?: key.defaultRange

    fun setTimeRangeFor(key: PeriodRangePreferenceKey, range: TimeRange) {
        prefs.edit { putString(key.storageKey, range.name) }
    }

    fun activityRecordingPreferences(): ActivityRecordingPreferences =
        ActivityRecordingPreferences(
            autoIdleEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_AUTO_IDLE_ENABLED,
                ActivityRecordingPreferences.DefaultAutoIdleEnabled,
            ),
            autoIdleTimeoutSeconds = prefs.getInt(
                KEY_ACTIVITY_RECORDING_AUTO_IDLE_TIMEOUT_SECONDS,
                ActivityRecordingPreferences.DefaultAutoIdleTimeoutSeconds,
            ),
            keepScreenOnDuringRecording = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_KEEP_SCREEN_ON,
                ActivityRecordingPreferences.DefaultKeepScreenOnDuringRecording,
            ),
            requiredGpsAccuracyMeters = prefs.getInt(
                KEY_ACTIVITY_RECORDING_REQUIRED_GPS_ACCURACY_METERS,
                ActivityRecordingPreferences.DefaultRequiredGpsAccuracyMeters,
            ),
            routeGapMeters = prefs.getInt(
                KEY_ACTIVITY_RECORDING_ROUTE_GAP_METERS,
                ActivityRecordingPreferences.DefaultRouteGapMeters ?: ROUTE_GAP_OFF,
            ).takeIf { it != ROUTE_GAP_OFF },
            barometerClimbEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_BAROMETER_CLIMB_ENABLED,
                ActivityRecordingPreferences.DefaultBarometerClimbEnabled,
            ),
            recordingDistanceIntervalMeters = prefs.getInt(
                KEY_ACTIVITY_RECORDING_DISTANCE_INTERVAL_METERS,
                ActivityRecordingPreferences.DefaultRecordingDistanceIntervalMeters ?: RECORDING_INTERVAL_OFF,
            ).takeIf { it != RECORDING_INTERVAL_OFF },
            recordingTimeIntervalMillis = prefs.getInt(
                KEY_ACTIVITY_RECORDING_TIME_INTERVAL_MILLIS,
                ActivityRecordingPreferences.DefaultRecordingTimeIntervalMillis,
            ),
            voiceAnnouncementsEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_VOICE_ENABLED,
                ActivityRecordingPreferences.DefaultVoiceAnnouncementsEnabled,
            ),
            voiceAnnouncementTimeIntervalMinutes = prefs.getInt(
                KEY_ACTIVITY_RECORDING_VOICE_TIME_INTERVAL_MINUTES,
                ActivityRecordingPreferences.DefaultVoiceAnnouncementTimeIntervalMinutes ?: RECORDING_INTERVAL_OFF,
            ).takeIf { it != RECORDING_INTERVAL_OFF },
            voiceAnnouncementDistanceIntervalMeters = prefs.getInt(
                KEY_ACTIVITY_RECORDING_VOICE_DISTANCE_INTERVAL_METERS,
                ActivityRecordingPreferences.DefaultVoiceAnnouncementDistanceIntervalMeters ?: RECORDING_INTERVAL_OFF,
            ).takeIf { it != RECORDING_INTERVAL_OFF },
            voiceIdleAnnouncementsEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_VOICE_IDLE_ENABLED,
                ActivityRecordingPreferences.DefaultVoiceIdleAnnouncementsEnabled,
            ),
            voiceLapAnnouncementsEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_VOICE_LAP_ENABLED,
                ActivityRecordingPreferences.DefaultVoiceLapAnnouncementsEnabled,
            ),
            restTimerBellEnabled = prefs.getBoolean(
                KEY_ACTIVITY_RECORDING_REST_TIMER_BELL_ENABLED,
                ActivityRecordingPreferences.DefaultRestTimerBellEnabled,
            ),
        ).normalized()

    fun setActivityRecordingPreferences(preferences: ActivityRecordingPreferences) {
        val normalized = preferences.normalized()
        prefs.edit {
            putBoolean(KEY_ACTIVITY_RECORDING_AUTO_IDLE_ENABLED, normalized.autoIdleEnabled)
            putInt(KEY_ACTIVITY_RECORDING_AUTO_IDLE_TIMEOUT_SECONDS, normalized.autoIdleTimeoutSeconds)
            putBoolean(KEY_ACTIVITY_RECORDING_KEEP_SCREEN_ON, normalized.keepScreenOnDuringRecording)
            putInt(KEY_ACTIVITY_RECORDING_REQUIRED_GPS_ACCURACY_METERS, normalized.requiredGpsAccuracyMeters)
            putInt(KEY_ACTIVITY_RECORDING_ROUTE_GAP_METERS, normalized.routeGapMeters ?: ROUTE_GAP_OFF)
            putBoolean(KEY_ACTIVITY_RECORDING_BAROMETER_CLIMB_ENABLED, normalized.barometerClimbEnabled)
            putInt(
                KEY_ACTIVITY_RECORDING_DISTANCE_INTERVAL_METERS,
                normalized.recordingDistanceIntervalMeters ?: RECORDING_INTERVAL_OFF,
            )
            putInt(KEY_ACTIVITY_RECORDING_TIME_INTERVAL_MILLIS, normalized.recordingTimeIntervalMillis)
            putBoolean(KEY_ACTIVITY_RECORDING_VOICE_ENABLED, normalized.voiceAnnouncementsEnabled)
            putInt(
                KEY_ACTIVITY_RECORDING_VOICE_TIME_INTERVAL_MINUTES,
                normalized.voiceAnnouncementTimeIntervalMinutes ?: RECORDING_INTERVAL_OFF,
            )
            putInt(
                KEY_ACTIVITY_RECORDING_VOICE_DISTANCE_INTERVAL_METERS,
                normalized.voiceAnnouncementDistanceIntervalMeters ?: RECORDING_INTERVAL_OFF,
            )
            putBoolean(KEY_ACTIVITY_RECORDING_VOICE_IDLE_ENABLED, normalized.voiceIdleAnnouncementsEnabled)
            putBoolean(KEY_ACTIVITY_RECORDING_VOICE_LAP_ENABLED, normalized.voiceLapAnnouncementsEnabled)
            putBoolean(KEY_ACTIVITY_RECORDING_REST_TIMER_BELL_ENABLED, normalized.restTimerBellEnabled)
        }
    }

    fun activityRecordingDashboardLayout(activityTypeId: String): ActivityRecordingDashboardLayout =
        prefs.getString(activityRecordingDashboardLayoutKey(activityTypeId), null)
            ?.toActivityRecordingDashboardLayout()
            ?: ActivityRecordingDashboardLayout()

    fun setActivityRecordingDashboardLayout(
        activityTypeId: String,
        layout: ActivityRecordingDashboardLayout,
    ) {
        if (activityTypeId.isBlank()) return
        prefs.edit {
            putString(
                activityRecordingDashboardLayoutKey(activityTypeId),
                layout.normalized().toPreferenceString(),
            )
        }
    }

    fun dailyGoalFor(key: MetricDailyGoalKey): Double =
        prefs.getFloat(key.storageKey, key.defaultValue.toFloat()).toDouble()
            .let(key::normalize)

    fun setDailyGoalFor(key: MetricDailyGoalKey, value: Double) {
        prefs.edit {
            putFloat(key.storageKey, key.normalize(value).toFloat())
        }
    }

    fun hydrationReminderConfig(): HydrationReminderConfig =
        HydrationReminderConfig(
            enabled = prefs.getBoolean(KEY_HYDRATION_REMINDERS_ENABLED, false),
            intervalMinutes = prefs.getInt(
                KEY_HYDRATION_REMINDER_INTERVAL_MINUTES,
                HydrationReminderConfig.DefaultIntervalMinutes,
            ),
            activeStartTime = prefs.getString(KEY_HYDRATION_REMINDER_ACTIVE_START_TIME, null)
                .toReminderTimeOrDefault(HydrationReminderConfig.DefaultActiveStartTime),
            activeEndTime = prefs.getString(KEY_HYDRATION_REMINDER_ACTIVE_END_TIME, null)
                .toReminderTimeOrDefault(HydrationReminderConfig.DefaultActiveEndTime),
        ).normalized()

    fun setHydrationReminderConfig(config: HydrationReminderConfig) {
        val normalized = config.normalized()
        prefs.edit {
            putBoolean(KEY_HYDRATION_REMINDERS_ENABLED, normalized.enabled)
            putInt(KEY_HYDRATION_REMINDER_INTERVAL_MINUTES, normalized.intervalMinutes)
            putString(KEY_HYDRATION_REMINDER_ACTIVE_START_TIME, normalized.activeStartTime.toString())
            putString(KEY_HYDRATION_REMINDER_ACTIVE_END_TIME, normalized.activeEndTime.toString())
        }
    }

    fun hydrationContainerVolumeMilliliters(): Map<String, Double> =
        prefs.getStringSet(KEY_HYDRATION_CONTAINER_VOLUME_MILLILITERS, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separatorIndex = entry.indexOf(KEY_VALUE_PAIR_SEPARATOR)
                if (separatorIndex <= 0 || separatorIndex == entry.lastIndex) {
                    null
                } else {
                    val key = entry.substring(0, separatorIndex)
                    val value = entry.substring(separatorIndex + 1).toDoubleOrNull()
                    value?.takeIf { it > 0.0 && it.isFinite() }?.let { key to it }
                }
            }
            .toMap()

    fun setHydrationContainerVolumeMilliliters(containerId: String, milliliters: Double) {
        if (containerId.isBlank() || milliliters <= 0.0 || !milliliters.isFinite()) return

        val values = hydrationContainerVolumeMilliliters().toMutableMap()
        values[containerId] = milliliters
        prefs.edit {
            putStringSet(
                KEY_HYDRATION_CONTAINER_VOLUME_MILLILITERS,
                values.mapTo(mutableSetOf()) { (key, value) ->
                    "$key$KEY_VALUE_PAIR_SEPARATOR$value"
                },
            )
        }
    }

    fun lastCustomHydrationAmountMilliliters(): Double? {
        val milliliters = prefs.getFloat(
            KEY_LAST_CUSTOM_HYDRATION_AMOUNT_MILLILITERS,
            MISSING_HYDRATION_AMOUNT_MILLILITERS,
        )
        return milliliters
            .takeIf { it != MISSING_HYDRATION_AMOUNT_MILLILITERS && it > 0.0f && it.isFinite() }
            ?.toDouble()
    }

    fun setLastCustomHydrationAmountMilliliters(milliliters: Double) {
        if (milliliters <= 0.0 || !milliliters.isFinite()) return

        prefs.edit {
            putFloat(KEY_LAST_CUSTOM_HYDRATION_AMOUNT_MILLILITERS, milliliters.toFloat())
        }
    }

    fun customHydrationDrinks(): List<CustomHydrationDrink> {
        val drinks = prefs.getStringSet(KEY_CUSTOM_HYDRATION_DRINKS, emptySet())
            .orEmpty()
            .mapNotNull { it.toCustomHydrationDrink() }
        if (drinks.isEmpty()) return emptyList()

        val drinksById = drinks.associateBy { it.id }
        val orderedIds = customHydrationDrinkOrder()
            .filter { it in drinksById }
            .distinct()
        val orderedDrinks = orderedIds.mapNotNull(drinksById::get)
        val orderedIdSet = orderedIds.toSet()
        val missingOrderDrinks = drinks
            .filterNot { it.id in orderedIdSet }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        return orderedDrinks + missingOrderDrinks
    }

    fun saveCustomHydrationDrink(drink: CustomHydrationDrink) {
        val normalized = drink.normalizedCustomHydrationDrink() ?: return
        val current = customHydrationDrinks()
        val existingIndex = current.indexOfFirst {
            it.id == normalized.id || it.name.equals(normalized.name, ignoreCase = true)
        }
        val values = current
            .filterNot { it.id == normalized.id || it.name.equals(normalized.name, ignoreCase = true) }
            .toMutableList()
            .apply {
                if (existingIndex >= 0) {
                    add(existingIndex.coerceIn(0, size), normalized)
                } else {
                    add(normalized)
                }
            }
            .takeLast(MAX_CUSTOM_HYDRATION_DRINKS)
        persistCustomHydrationDrinks(values)
    }

    fun deleteCustomHydrationDrink(drinkId: String) {
        if (drinkId.isBlank()) return
        persistCustomHydrationDrinks(customHydrationDrinks().filterNot { it.id == drinkId })
    }

    fun reorderCustomHydrationDrinks(drinkIds: List<String>) {
        val current = customHydrationDrinks()
        val drinksById = current.associateBy { it.id }
        val orderedIds = drinkIds
            .filter { it in drinksById }
            .distinct()
        val orderedDrinks = orderedIds.mapNotNull(drinksById::get)
        val orderedIdSet = orderedIds.toSet()
        persistCustomHydrationDrinks(orderedDrinks + current.filterNot { it.id in orderedIdSet })
    }

    fun mindfulnessReminderConfig(): MindfulnessReminderConfig =
        MindfulnessReminderConfig(
            enabled = prefs.getBoolean(KEY_MINDFULNESS_REMINDERS_ENABLED, false),
            reminderTime = prefs.getString(KEY_MINDFULNESS_REMINDER_TIME, null)
                .toReminderTimeOrDefault(MindfulnessReminderConfig.DefaultReminderTime),
        ).normalized()

    fun setMindfulnessReminderConfig(config: MindfulnessReminderConfig) {
        val normalized = config.normalized()
        prefs.edit {
            putBoolean(KEY_MINDFULNESS_REMINDERS_ENABLED, normalized.enabled)
            putString(KEY_MINDFULNESS_REMINDER_TIME, normalized.reminderTime.toString())
        }
    }

    fun dashboardWidgetOrder(): List<String>? =
        prefs.getString(KEY_DASHBOARD_WIDGET_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.filter { it.isNotBlank() }

    fun setDashboardWidgetOrder(widgetIds: List<String>) {
        prefs.edit {
            putString(KEY_DASHBOARD_WIDGET_ORDER, widgetIds.joinToString(KEY_VALUE_SEPARATOR))
        }
    }

    fun manualEntryWidgetOrder(): List<String>? =
        prefs.getString(KEY_MANUAL_ENTRY_WIDGET_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.filter { it.isNotBlank() }

    fun setManualEntryWidgetOrder(widgetIds: List<String>) {
        prefs.edit {
            putString(KEY_MANUAL_ENTRY_WIDGET_ORDER, widgetIds.joinToString(KEY_VALUE_SEPARATOR))
        }
    }

    fun metricDetailSectionOrder(): List<String>? =
        prefs.getString(KEY_METRIC_DETAIL_SECTION_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.filter { it.isNotBlank() }

    fun setMetricDetailSectionOrder(sectionIds: List<String>) {
        prefs.edit {
            putString(KEY_METRIC_DETAIL_SECTION_ORDER, sectionIds.joinToString(KEY_VALUE_SEPARATOR))
        }
    }

    fun acknowledgedPermissions(): Set<String> =
        prefs.getStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, emptySet()) ?: emptySet()

    fun acknowledgePermissions(permissions: Set<String>) {
        prefs.edit {
            putStringSet(KEY_ACKNOWLEDGED_PERMISSIONS, acknowledgedPermissions() + permissions)
        }
    }

    fun acknowledgedPermissionsFor(feature: HealthConnectFeature): Set<String> =
        prefs.getStringSet(acknowledgedFeatureKey(feature), emptySet()) ?: emptySet()

    fun acknowledgePermissionsFor(
        feature: HealthConnectFeature,
        permissions: Set<String>,
    ) {
        if (permissions.isEmpty()) return
        prefs.edit {
            putStringSet(
                acknowledgedFeatureKey(feature),
                acknowledgedPermissionsFor(feature) + permissions,
            )
        }
    }

    var lastPromptedPermissionSetVersion: Int
        get() = prefs.getInt(KEY_LAST_PROMPTED_PERMISSION_SET_VERSION, 0)
        set(value) {
            prefs.edit { putInt(KEY_LAST_PROMPTED_PERMISSION_SET_VERSION, value) }
        }

    private fun acknowledgedFeatureKey(feature: HealthConnectFeature): String =
        "$KEY_ACKNOWLEDGED_FEATURE_PREFIX${feature.name}"

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
        prefs.edit {
            putInt(KEY_MINDFULNESS_TIMER_DURATION_MINUTES, duration)
            putInt(KEY_MINDFULNESS_TIMER_INTERVAL_MINUTES, interval ?: 0)
            putString(KEY_MINDFULNESS_TIMER_BELL_SOUND, config.bellSound.name)
            putString(KEY_MINDFULNESS_TIMER_BACKGROUND_SOUND, config.backgroundSound.name)
        }
    }

    private fun readUnitSystem(): UnitSystem =
        prefs.getString(KEY_UNIT_SYSTEM, null)
            ?.let { value -> runCatching { UnitSystem.valueOf(value) }.getOrNull() }
            ?: defaultUnitSystem()

    private fun readAppLanguage(): AppLanguage =
        prefs.getString(KEY_APP_LANGUAGE, null)
            ?.let { value -> runCatching { AppLanguage.valueOf(value) }.getOrNull() }
            ?: AppLanguage.SYSTEM

    private fun readAppThemeMode(): AppThemeMode =
        prefs.getString(KEY_APP_THEME_MODE, null)
            ?.let { value -> runCatching { AppThemeMode.valueOf(value) }.getOrNull() }
            ?: AppThemeMode.SYSTEM

    private fun readSleepRangeMode(): SleepRangeMode =
        prefs.getString(KEY_SLEEP_RANGE_MODE, null)
            ?.let { value -> runCatching { SleepRangeMode.valueOf(value) }.getOrNull() }
            ?: SleepRangeMode.EVENING_18H

    private fun readActivityWeekMode(): ActivityWeekMode =
        prefs.getString(KEY_ACTIVITY_WEEK_MODE, null)
            ?.let { value -> runCatching { ActivityWeekMode.valueOf(value) }.getOrNull() }
            ?: ActivityWeekMode.MONDAY_TO_SUNDAY

    private fun readShowOpenVitalsCalculatedCalories(): Boolean =
        prefs.getBoolean(KEY_SHOW_OPENVITALS_CALCULATED_CALORIES, false)

    private fun readHealthConnectSyncEnabled(): Boolean =
        prefs.getBoolean(KEY_HEALTH_CONNECT_SYNC_ENABLED, true)

    private fun readBodyEnergyCalibration(): BodyEnergyCalibration =
        BodyEnergyCalibration(
            birthYear = prefs.getInt(KEY_BODY_ENERGY_BIRTH_YEAR, MISSING_BODY_ENERGY_INT)
                .takeIf { it != MISSING_BODY_ENERGY_INT },
            manualMaxHeartRateBpm = prefs.getInt(KEY_BODY_ENERGY_MAX_HR_BPM, MISSING_BODY_ENERGY_INT)
                .takeIf { it != MISSING_BODY_ENERGY_INT },
            manualRestingHeartRateBpm = prefs.getInt(KEY_BODY_ENERGY_RESTING_HR_BPM, MISSING_BODY_ENERGY_INT)
                .takeIf { it != MISSING_BODY_ENERGY_INT },
            manualZoneThresholdsBpm = HeartZoneThresholds.fromPreferenceString(
                prefs.getString(KEY_BODY_ENERGY_ZONE_THRESHOLDS_BPM, null),
            ),
            useManualZones = prefs.getBoolean(KEY_BODY_ENERGY_USE_MANUAL_ZONES, false),
        ).normalized()

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

    private fun String?.toReminderTimeOrDefault(default: LocalTime): LocalTime =
        this?.let { value -> runCatching { LocalTime.parse(value) }.getOrNull() } ?: default

    private fun CustomHydrationDrink.normalizedCustomHydrationDrink(): CustomHydrationDrink? {
        val normalizedName = name.trim()
        if (id.isBlank() || normalizedName.isBlank()) return null
        if (volumeMilliliters <= 0.0 || !volumeMilliliters.isFinite()) return null
        if (hydrationMultiplier < 0.0 || hydrationMultiplier > 1.0 || !hydrationMultiplier.isFinite()) return null
        val normalizedNutrients = nutrientValues
            .filterValues { it > 0.0 && it.isFinite() }
            .toSortedMap(compareBy { it.name })
        return copy(
            name = normalizedName,
            nutrientValues = normalizedNutrients,
        )
    }

    private fun CustomHydrationDrink.toPreferenceString(): String =
        listOf(
            id.encodePreferenceValue(),
            name.encodePreferenceValue(),
            volumeMilliliters.toString(),
            hydrationMultiplier.toString(),
            nutrientValues.entries.joinToString(KEY_NUTRIENT_SEPARATOR) { (nutrient, value) ->
                "${nutrient.name}$KEY_VALUE_PAIR_SEPARATOR$value"
            }.encodePreferenceValue(),
        ).joinToString(KEY_LAYOUT_SECTION_SEPARATOR)

    private fun String.toCustomHydrationDrink(): CustomHydrationDrink? {
        val parts = split(KEY_LAYOUT_SECTION_SEPARATOR, limit = 5)
        if (parts.size < 4) return null
        val id = parts[0].decodePreferenceValue().takeIf { it.isNotBlank() } ?: return null
        val name = parts[1].decodePreferenceValue().takeIf { it.isNotBlank() } ?: return null
        val volumeMilliliters = parts[2].toDoubleOrNull()
            ?.takeIf { it > 0.0 && it.isFinite() }
            ?: return null
        val hydrationMultiplier = parts[3].toDoubleOrNull()
            ?.takeIf { it >= 0.0 && it <= 1.0 && it.isFinite() }
            ?: 1.0
        val nutrientValues = parts.getOrNull(4)
            ?.decodePreferenceValue()
            ?.split(KEY_NUTRIENT_SEPARATOR)
            .orEmpty()
            .mapNotNull { value ->
                val sections = value.split(KEY_VALUE_PAIR_SEPARATOR, limit = 2)
                val nutrient = sections.getOrNull(0)
                    ?.let { runCatching { NutritionNutrient.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val amount = sections.getOrNull(1)
                    ?.toDoubleOrNull()
                    ?.takeIf { it > 0.0 && it.isFinite() }
                    ?: return@mapNotNull null
                nutrient to amount
            }
            .toMap()
        return CustomHydrationDrink(
            id = id,
            name = name,
            volumeMilliliters = volumeMilliliters,
            hydrationMultiplier = hydrationMultiplier,
            nutrientValues = nutrientValues,
        )
    }

    private fun customHydrationDrinkOrder(): List<String> =
        prefs.getString(KEY_CUSTOM_HYDRATION_DRINK_ORDER, null)
            ?.split(KEY_VALUE_SEPARATOR)
            .orEmpty()
            .map { it.decodePreferenceValue() }
            .filter { it.isNotBlank() }

    private fun persistCustomHydrationDrinks(drinks: List<CustomHydrationDrink>) {
        prefs.edit {
            putStringSet(
                KEY_CUSTOM_HYDRATION_DRINKS,
                drinks.mapTo(mutableSetOf()) { it.toPreferenceString() },
            )
            putString(
                KEY_CUSTOM_HYDRATION_DRINK_ORDER,
                drinks.joinToString(KEY_VALUE_SEPARATOR) { it.id.encodePreferenceValue() },
            )
        }
    }

    private fun String.encodePreferenceValue(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    private fun String.decodePreferenceValue(): String =
        URLDecoder.decode(this, Charsets.UTF_8.name())

    private fun activityRecordingDashboardLayoutKey(activityTypeId: String): String =
        "$KEY_ACTIVITY_RECORDING_DASHBOARD_LAYOUT_PREFIX$activityTypeId"

    private fun ActivityRecordingDashboardLayout.toPreferenceString(): String {
        val normalized = normalized()
        return normalized.template.name + KEY_LAYOUT_SECTION_SEPARATOR +
            normalized.items.joinToString(KEY_VALUE_SEPARATOR) { item ->
                item.field.name + KEY_VALUE_PAIR_SEPARATOR + item.size.toPreferenceString()
            }
    }

    private fun String.toActivityRecordingDashboardLayout(): ActivityRecordingDashboardLayout? {
        val sections = split(KEY_LAYOUT_SECTION_SEPARATOR, limit = 2)
        val template = sections.firstOrNull()
            ?.let { value ->
                runCatching { ActivityRecordingDashboardTemplate.valueOf(value) }.getOrNull()
            }
            ?: return null
        val items = sections.getOrNull(1)
            ?.split(KEY_VALUE_SEPARATOR)
            ?.mapNotNull { value ->
                val itemSections = value.split(KEY_VALUE_PAIR_SEPARATOR, limit = 2)
                val field = itemSections.firstOrNull()
                    ?.let { fieldName ->
                        runCatching { ActivityRecordingDashboardField.valueOf(fieldName) }.getOrNull()
                    }
                    ?: return@mapNotNull null
                val size = itemSections.getOrNull(1)
                    ?.let { sizeName ->
                        ActivityRecordingDashboardItemSize.fromPreferenceString(sizeName)
                    }
                field to size
            }
            .orEmpty()
        return ActivityRecordingDashboardLayout(
            template = template,
            fields = items.map { it.first },
            sizes = items.mapNotNull { (field, size) -> size?.let { field to it } }.toMap(),
        ).normalized()
    }

    companion object {
        const val PREFS_FILE = "openvitals_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ACKNOWLEDGED_PERMISSIONS = "acknowledged_permissions"
        private const val KEY_ACKNOWLEDGED_FEATURE_PREFIX = "acknowledged_feature_permissions_"
        private const val KEY_LAST_PROMPTED_PERMISSION_SET_VERSION = "last_prompted_permission_set_version"
        private const val KEY_UNIT_SYSTEM = "unit_system"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_APP_THEME_MODE = "app_theme_mode"
        private const val KEY_SLEEP_RANGE_MODE = "sleep_range_mode"
        private const val KEY_ACTIVITY_WEEK_MODE = "activity_week_mode"
        private const val KEY_ACTIVITY_RECORDING_AUTO_IDLE_ENABLED = "activity_recording_auto_idle_enabled"
        private const val KEY_ACTIVITY_RECORDING_AUTO_IDLE_TIMEOUT_SECONDS = "activity_recording_auto_idle_timeout_seconds"
        private const val KEY_ACTIVITY_RECORDING_KEEP_SCREEN_ON = "activity_recording_keep_screen_on"
        private const val KEY_ACTIVITY_RECORDING_REQUIRED_GPS_ACCURACY_METERS = "activity_recording_required_gps_accuracy_meters"
        private const val KEY_ACTIVITY_RECORDING_ROUTE_GAP_METERS = "activity_recording_route_gap_meters"
        private const val KEY_ACTIVITY_RECORDING_BAROMETER_CLIMB_ENABLED = "activity_recording_barometer_climb_enabled"
        private const val KEY_ACTIVITY_RECORDING_DISTANCE_INTERVAL_METERS = "activity_recording_distance_interval_meters"
        private const val KEY_ACTIVITY_RECORDING_TIME_INTERVAL_MILLIS = "activity_recording_time_interval_millis"
        private const val KEY_ACTIVITY_RECORDING_VOICE_ENABLED = "activity_recording_voice_enabled"
        private const val KEY_ACTIVITY_RECORDING_VOICE_TIME_INTERVAL_MINUTES = "activity_recording_voice_time_interval_minutes"
        private const val KEY_ACTIVITY_RECORDING_VOICE_DISTANCE_INTERVAL_METERS = "activity_recording_voice_distance_interval_meters"
        private const val KEY_ACTIVITY_RECORDING_VOICE_IDLE_ENABLED = "activity_recording_voice_idle_enabled"
        private const val KEY_ACTIVITY_RECORDING_VOICE_LAP_ENABLED = "activity_recording_voice_lap_enabled"
        private const val KEY_ACTIVITY_RECORDING_REST_TIMER_BELL_ENABLED = "activity_recording_rest_timer_bell_enabled"
        private const val KEY_ACTIVITY_RECORDING_DASHBOARD_LAYOUT_PREFIX = "activity_recording_dashboard_layout_"
        private const val KEY_SHOW_OPENVITALS_CALCULATED_CALORIES = "show_openvitals_calculated_calories"
        private const val KEY_HEALTH_CONNECT_SYNC_ENABLED = "health_connect_sync_enabled"
        private const val KEY_HEALTH_CONNECT_PERMISSION_CANCEL_COUNT = "health_connect_permission_cancel_count"
        private const val KEY_ACCEPTED_PRIVACY_POLICY_VERSION = "accepted_privacy_policy_version"
        private const val KEY_PRIVACY_POLICY_ACCEPTED_AT = "privacy_policy_accepted_at"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val CURRENT_PRIVACY_POLICY_VERSION = "1.0"
        private const val KEY_LAST_ACTIVITY_EXERCISE_TYPE = "last_activity_exercise_type"
        private const val KEY_FAVORITE_ACTIVITY_EXERCISE_TYPE = "favorite_activity_exercise_type"
        private const val KEY_DASHBOARD_WIDGET_ORDER = "dashboard_widget_order"
        private const val KEY_MANUAL_ENTRY_WIDGET_ORDER = "manual_entry_widget_order"
        private const val KEY_METRIC_DETAIL_SECTION_ORDER = "metric_detail_section_order"
        private const val KEY_HYDRATION_DAILY_GOAL_LITERS = "hydration_daily_goal_liters"
        private const val KEY_HYDRATION_CONTAINER_VOLUME_MILLILITERS = "hydration_container_volume_milliliters"
        private const val KEY_LAST_CUSTOM_HYDRATION_AMOUNT_MILLILITERS =
            "last_custom_hydration_amount_milliliters"
        private const val KEY_CUSTOM_HYDRATION_DRINKS = "custom_hydration_drinks"
        private const val KEY_CUSTOM_HYDRATION_DRINK_ORDER = "custom_hydration_drink_order"
        private const val KEY_HYDRATION_REMINDERS_ENABLED = "hydration_reminders_enabled"
        private const val KEY_HYDRATION_REMINDER_INTERVAL_MINUTES = "hydration_reminder_interval_minutes"
        private const val KEY_HYDRATION_REMINDER_ACTIVE_START_TIME = "hydration_reminder_active_start_time"
        private const val KEY_HYDRATION_REMINDER_ACTIVE_END_TIME = "hydration_reminder_active_end_time"
        private const val KEY_HIGH_HEART_RATE_THRESHOLD_BPM = "high_heart_rate_threshold_bpm"
        private const val KEY_LOW_HEART_RATE_THRESHOLD_BPM = "low_heart_rate_threshold_bpm"
        private const val KEY_BODY_ENERGY_BIRTH_YEAR = "body_energy_birth_year"
        private const val KEY_BODY_ENERGY_MAX_HR_BPM = "body_energy_max_hr_bpm"
        private const val KEY_BODY_ENERGY_RESTING_HR_BPM = "body_energy_resting_hr_bpm"
        private const val KEY_BODY_ENERGY_ZONE_THRESHOLDS_BPM = "body_energy_zone_thresholds_bpm"
        private const val KEY_BODY_ENERGY_USE_MANUAL_ZONES = "body_energy_use_manual_zones"
        private const val KEY_MINDFULNESS_TIMER_DURATION_MINUTES = "mindfulness_timer_duration_minutes"
        private const val KEY_MINDFULNESS_TIMER_INTERVAL_MINUTES = "mindfulness_timer_interval_minutes"
        private const val KEY_MINDFULNESS_TIMER_BELL_SOUND = "mindfulness_timer_bell_sound"
        private const val KEY_MINDFULNESS_TIMER_BACKGROUND_SOUND = "mindfulness_timer_background_sound"
        private const val KEY_MINDFULNESS_REMINDERS_ENABLED = "mindfulness_reminders_enabled"
        private const val KEY_MINDFULNESS_REMINDER_TIME = "mindfulness_reminder_time"
        private const val KEY_VALUE_SEPARATOR = ","
        private const val KEY_VALUE_PAIR_SEPARATOR = "="
        private const val KEY_NUTRIENT_SEPARATOR = ";"
        private const val KEY_LAYOUT_SECTION_SEPARATOR = "|"
        private const val DEFAULT_HYDRATION_DAILY_GOAL_LITERS = 2.0
        private const val MIN_HYDRATION_DAILY_GOAL_LITERS = 0.25
        private const val MAX_HYDRATION_DAILY_GOAL_LITERS = 10.0
        const val DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM = 120
        const val DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM = 50
        const val MIN_HIGH_HEART_RATE_THRESHOLD_BPM = 80
        const val MAX_HIGH_HEART_RATE_THRESHOLD_BPM = 220
        const val MIN_LOW_HEART_RATE_THRESHOLD_BPM = 30
        const val MAX_LOW_HEART_RATE_THRESHOLD_BPM = 100
        private const val DEFAULT_MINDFULNESS_TIMER_DURATION_MINUTES = 10
        private const val MIN_MINDFULNESS_TIMER_MINUTES = 1
        private const val MAX_MINDFULNESS_TIMER_MINUTES = 24 * 60
        private const val MISSING_EXERCISE_TYPE = Int.MIN_VALUE
        private const val MISSING_HYDRATION_AMOUNT_MILLILITERS = -1.0f
        private const val MISSING_BODY_ENERGY_INT = Int.MIN_VALUE
        private const val MAX_CUSTOM_HYDRATION_DRINKS = 25
        private const val ROUTE_GAP_OFF = 0
        private const val RECORDING_INTERVAL_OFF = 0
        private val IMPERIAL_COUNTRIES = setOf("US", "LR", "MM")
    }
}
