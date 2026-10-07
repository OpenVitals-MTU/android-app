package tech.mmarca.openvitals.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.model.MedicalCategory
import tech.mmarca.openvitals.domain.model.MedicalRecordRef

const val ACTIVITY_DETAIL_ID_ARG = "activityId"

/** Optional query argument: the day a metric screen opens on. Anchors the date, not the range. */
const val SELECTED_DAY_ARG = "day"

/** The `?day={day}` suffix a metric route pattern declares to accept the argument. */
const val SELECTED_DAY_QUERY_PATTERN = "?$SELECTED_DAY_ARG={$SELECTED_DAY_ARG}"

/** Appends the selected day. Today is omitted so the ordinary location stays clean. */
fun String.withSelectedDay(day: LocalDate): String =
    if (day == LocalDate.now()) {
        this
    } else {
        this + (if ('?' in this) "&" else "?") + "$SELECTED_DAY_ARG=$day"
    }

/** The pinned day carried by the route, if any; malformed values read as absent. */
fun SavedStateHandle.selectedDayOrNull(): LocalDate? =
    get<String>(SELECTED_DAY_ARG)?.let { raw ->
        runCatching { LocalDate.parse(raw) }.getOrNull()
    }
const val ACTIVITY_ENTRY_ID_ARG = "activityEntryId"
const val ACTIVITY_ENTRY_MODE_ARG = "mode"
const val ACTIVITY_ENTRY_PLAN_ID_ARG = "planId"
const val WORKOUT_PLAN_ID_ARG = "workoutPlanId"
/** SavedStateHandle key the builder sets on the previous entry when it saved a plan. */
const val WORKOUT_PLAN_SAVED_RESULT = "workoutPlanSavedId"
const val ACTIVITY_ENTRY_TYPE_ARG = "activityTypeId"
const val SLEEP_DETAIL_ID_ARG = "sleepId"
const val MEDICAL_CATEGORY_ARG = "category"
const val MEDICAL_SOURCE_ARG = "sourceId"
const val MEDICAL_TYPE_ARG = "resourceType"
const val MEDICAL_ID_ARG = "resourceId"
const val MEDICAL_ENTRY_KIND_ARG = "kind"
const val MEDICAL_ENTRY_ID_ARG = "id"
const val MEDICAL_IMPORT_URI_ARG = "uri"
const val METRIC_ID_ARG = "metricId"
const val BODY_MEASUREMENT_TYPE_ARG = "bodyMeasurementType"
const val BODY_ENTRY_ID_ARG = "bodyEntryId"
const val HYDRATION_ENTRY_ID_ARG = "hydrationEntryId"
const val HYDRATION_DRINK_ID_ARG = "hydrationDrinkId"
const val NUTRITION_ENTRY_ID_ARG = "nutritionEntryId"
const val MINDFULNESS_ENTRY_ID_ARG = "mindfulnessEntryId"
const val CAFFEINE_ENTRY_ID_ARG = "caffeineEntryId"
const val VITALS_MEASUREMENT_TYPE_ARG = "vitalsMeasurementType"
const val VITALS_ENTRY_ID_ARG = "vitalsEntryId"
const val CYCLE_ENTRY_DATE_ARG = "cycleEntryDate"
const val CYCLE_ENTRY_SECTION_ARG = "cycleEntrySection"

/** The optional query the day-log route declares: the day it opens on and the one section to show. */
const val CYCLE_ENTRY_QUERY_PATTERN =
    "?$CYCLE_ENTRY_DATE_ARG={$CYCLE_ENTRY_DATE_ARG}&$CYCLE_ENTRY_SECTION_ARG={$CYCLE_ENTRY_SECTION_ARG}"
const val STRESS_DATE_ARG = "stressDate"
const val WATCH_DEVICE_ID_ARG = "watchDeviceId"
const val WATCH_SETTINGS_SCREEN_ID_ARG = "screenId"
const val WATCH_POINT_LATITUDE_ARG = "lat"
const val WATCH_POINT_LONGITUDE_ARG = "lon"
const val WATCH_POINT_NAME_ARG = "name"

/** Set when another app shared something that held no position. */
const val WATCH_POINT_UNREADABLE_ARG = "unreadable"
const val BODY_ENERGY_DATE_ARG = "bodyEnergyDate"
const val TRAINING_READINESS_DATE_ARG = "trainingReadinessDate"

sealed class Screen(
    val route: String,
    /** The app bar title. Null when the screen titles itself or shows no app bar. */
    @param:StringRes val titleRes: Int? = null,
) {
    /**
     * The route without its query parameters. `AppNavigation` matches the
     * live destination against this, not [route].
     */
    val basePath: String get() = route.substringBefore('?')

    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard", R.string.app_name)
    data object StressDetails : Screen("daily_readiness/stress/{$STRESS_DATE_ARG}", R.string.screen_stress_tracking) {
        fun createRoute(date: String): String = "daily_readiness/stress/${Uri.encode(date)}"
    }
    data object BodyEnergyDetails : Screen("daily_readiness/body_energy/{$BODY_ENERGY_DATE_ARG}", R.string.screen_body_energy) {
        fun createRoute(date: String): String = "daily_readiness/body_energy/${Uri.encode(date)}"
    }
    data object TrainingReadinessDetails :
        Screen("daily_readiness/training_readiness/{$TRAINING_READINESS_DATE_ARG}", R.string.screen_training_readiness) {
        fun createRoute(date: String): String = "daily_readiness/training_readiness/${Uri.encode(date)}"
    }
    data object ManualEntry : Screen("manual_entry", R.string.screen_manual_entry)
    data object HydrationEntry : Screen("manual_entry/hydration", R.string.screen_hydration_entry)
    data object HydrationEntryEdit : Screen("manual_entry/hydration/edit/{$HYDRATION_ENTRY_ID_ARG}", R.string.screen_hydration_entry) {
        fun createRoute(entryId: String): String = "manual_entry/hydration/edit/${Uri.encode(entryId)}"
    }
    data object HydrationEntryLogDrink : Screen("manual_entry/hydration/log/{$HYDRATION_DRINK_ID_ARG}", R.string.screen_hydration_entry) {
        fun createRoute(drinkId: String): String = "manual_entry/hydration/log/${Uri.encode(drinkId)}"
    }
    data object NutritionEntry : Screen("manual_entry/nutrition", R.string.screen_nutrition_entry)
    data object NutritionEntryEdit : Screen("manual_entry/nutrition/edit/{$NUTRITION_ENTRY_ID_ARG}", R.string.screen_nutrition_entry) {
        fun createRoute(entryId: String): String = "manual_entry/nutrition/edit/${Uri.encode(entryId)}"
    }
    data object FoodEntry : Screen("manual_entry/food", R.string.screen_food_entry)
    data object ActivityEntry : Screen(
        "manual_entry/activity" +
            "?$ACTIVITY_ENTRY_MODE_ARG={$ACTIVITY_ENTRY_MODE_ARG}" +
            "&$ACTIVITY_ENTRY_PLAN_ID_ARG={$ACTIVITY_ENTRY_PLAN_ID_ARG}" +
            "&$ACTIVITY_ENTRY_TYPE_ARG={$ACTIVITY_ENTRY_TYPE_ARG}",
        R.string.screen_activity_entry,
    ) {
        /** A concrete target for the activity entry screen, with the intent as query arguments. */
        fun createRoute(
            mode: String? = null,
            planId: String? = null,
            activityTypeId: String? = null,
        ): String {
            val params = buildList {
                mode?.let { add("$ACTIVITY_ENTRY_MODE_ARG=${Uri.encode(it)}") }
                planId?.let { add("$ACTIVITY_ENTRY_PLAN_ID_ARG=${Uri.encode(it)}") }
                activityTypeId?.let { add("$ACTIVITY_ENTRY_TYPE_ARG=${Uri.encode(it)}") }
            }
            return if (params.isEmpty()) {
                "manual_entry/activity"
            } else {
                "manual_entry/activity?" + params.joinToString("&")
            }
        }
    }

    /** Intent values understood by [ActivityEntry]'s `mode` argument. */
    object ActivityEntryMode {
        const val RECORD = "record"
        const val MANUAL = "manual"
        const val PLAN = "plan"
    }
    data object WorkoutPlans : Screen("workout_plans", R.string.screen_workout_plans)
    data object WorkoutPlanBuilder : Screen("workout_plans/edit?$WORKOUT_PLAN_ID_ARG={$WORKOUT_PLAN_ID_ARG}", R.string.screen_workout_plan_builder) {
        /** No id opens the builder on a fresh plan; an id loads that plan for editing. */
        fun createRoute(planId: String? = null): String =
            if (planId == null) {
                "workout_plans/edit"
            } else {
                "workout_plans/edit?$WORKOUT_PLAN_ID_ARG=${Uri.encode(planId)}"
            }
    }
    data object ActivityEntryEdit : Screen("manual_entry/activity/edit/{$ACTIVITY_ENTRY_ID_ARG}", R.string.screen_activity_entry) {
        fun createRoute(entryId: String): String = "manual_entry/activity/edit/${Uri.encode(entryId)}"
    }
    data object MindfulnessEntry : Screen("manual_entry/mindfulness", R.string.screen_mindfulness_entry)
    data object MindfulnessEntryEdit : Screen("manual_entry/mindfulness/edit/{$MINDFULNESS_ENTRY_ID_ARG}", R.string.screen_mindfulness_entry) {
        fun createRoute(entryId: String): String = "manual_entry/mindfulness/edit/${Uri.encode(entryId)}"
    }
    data object BodyMeasurementEntry : Screen("manual_entry/body/{$BODY_MEASUREMENT_TYPE_ARG}", R.string.screen_body_measurement_entry) {
        fun createRoute(type: String): String = "manual_entry/body/${Uri.encode(type)}"
    }
    data object BodyMeasurementEntryEdit :
        Screen("manual_entry/body/{$BODY_MEASUREMENT_TYPE_ARG}/edit/{$BODY_ENTRY_ID_ARG}", R.string.screen_body_measurement_entry) {
        fun createRoute(type: String, entryId: String): String =
            "manual_entry/body/${Uri.encode(type)}/edit/${Uri.encode(entryId)}"
    }
    data object VitalsMeasurementEntry : Screen("manual_entry/vitals/{$VITALS_MEASUREMENT_TYPE_ARG}", R.string.screen_vitals_measurement_entry) {
        fun createRoute(type: String): String = "manual_entry/vitals/${Uri.encode(type)}"
    }
    data object VitalsMeasurementEntryEdit :
        Screen("manual_entry/vitals/{$VITALS_MEASUREMENT_TYPE_ARG}/edit/{$VITALS_ENTRY_ID_ARG}", R.string.screen_vitals_measurement_entry) {
        fun createRoute(type: String, entryId: String): String =
            "manual_entry/vitals/${Uri.encode(type)}/edit/${Uri.encode(entryId)}"
    }
    /** What to log, one card each. A card opens [CycleEntry] on that section alone. */
    data object CycleEntryChooser : Screen("manual_entry/cycle/choose", R.string.screen_cycle_entry)
    data object CycleEntry : Screen("manual_entry/cycle", R.string.screen_cycle_entry) {
        /** Without arguments the day log opens on today with every section. */
        fun createRoute(date: LocalDate? = null, section: String? = null): String {
            val query = listOfNotNull(
                date?.let { "$CYCLE_ENTRY_DATE_ARG=${Uri.encode(it.toString())}" },
                section?.let { "$CYCLE_ENTRY_SECTION_ARG=${Uri.encode(it)}" },
            )
            return if (query.isEmpty()) route else "$route?${query.joinToString("&")}"
        }
    }
    data object Calories : Screen("calories", R.string.screen_calories)
    data object Nutrition : Screen("nutrition", R.string.screen_nutrition)
    data object Activity : Screen("activity", R.string.screen_activities)
    data object ActivityDetail : Screen("activity_detail/{$ACTIVITY_DETAIL_ID_ARG}", R.string.screen_activity_detail) {
        fun createRoute(activityId: String): String = "activity_detail/${Uri.encode(activityId)}"
    }
    data object Sleep : Screen("sleep", R.string.screen_sleep)
    data object SleepDetail : Screen("sleep_detail/{$SLEEP_DETAIL_ID_ARG}", R.string.screen_sleep_detail) {
        fun createRoute(sleepId: String): String = "sleep_detail/${Uri.encode(sleepId)}"
    }
    data object CaffeineDrink : Screen("caffeine/drink/{$CAFFEINE_ENTRY_ID_ARG}", R.string.caffeine_drink_title) {
        /** [day] is the drink's own date: the drink screen loads only that day. */
        fun createRoute(entryId: String, day: LocalDate): String =
            "caffeine/drink/${Uri.encode(entryId)}".withSelectedDay(day)
    }
    data object Metric : Screen("metric/{$METRIC_ID_ARG}") {
        fun createRoute(metricId: String): String = "metric/${Uri.encode(metricId)}"
    }
    data object Settings : Screen("settings", R.string.screen_settings)
    data object SettingsDisplay : Screen("settings/display", R.string.settings_display_group_title)
    data object SettingsActivities : Screen("settings/activities", R.string.settings_activities_group_title)
    data object SettingsSensors : Screen("settings/sensors", R.string.settings_sensors_group_title)
    data object SettingsWatches : Screen("settings/watches", R.string.settings_watches_group_title)
    data object SettingsScales : Screen("settings/scales", R.string.settings_scales_group_title)

    /** The one scale's device view. Titled by the scale's name, like a watch. */
    data object ScaleDevice : Screen("scale")
    data object SettingsNutrition : Screen("settings/nutrition", R.string.settings_nutrition_group_title)
    data object SettingsCalories : Screen("settings/calories", R.string.settings_nutrition_group_title)
    data object SettingsCaffeine : Screen("settings/caffeine", R.string.settings_nutrition_group_title)
    data object SettingsBodyProfile : Screen("settings/body_profile", R.string.settings_body_profile_group_title)
    data object SettingsVitals : Screen("settings/vitals", R.string.settings_vitals_group_title)
    data object SettingsRecovery : Screen("settings/recovery", R.string.settings_recovery_group_title)
    data object SettingsCycle : Screen("settings/cycle", R.string.settings_cycle_group_title)
    data object SettingsSleep : Screen("settings/sleep", R.string.settings_recovery_group_title)
    data object SettingsBodyEnergy : Screen("settings/body_energy", R.string.settings_recovery_group_title)
    data object SettingsDataImport : Screen("settings/data_import", R.string.settings_data_transfer_group_title)
    data object SettingsCsvImport : Screen("settings/data_import/csv", R.string.settings_csv_import_screen_title)
    data object SettingsMedicalImport : Screen("settings/data_import/medical", R.string.medical_import_title) {
        /** The route with an optional file to read straight away, such as an analysed Apple Health export. */
        val routePattern: String = "$route?$MEDICAL_IMPORT_URI_ARG={$MEDICAL_IMPORT_URI_ARG}"

        fun createRoute(uri: String? = null): String = if (uri == null) route else "$route?$MEDICAL_IMPORT_URI_ARG=${Uri.encode(uri)}"
    }
    data object SettingsReportExport : Screen("settings/data_import/report", R.string.report_builder_title)
    data object SettingsDeviceSync : Screen("settings/device_sync", R.string.settings_device_sync_group_title)
    data object SettingsHealthConnect : Screen("settings/health_connect", R.string.settings_health_connect_group_title)
    data object SettingsPermissions : Screen("settings/permissions", R.string.settings_health_connect_group_title)
    data object SettingsDebugDiagnostics : Screen("settings/debug_diagnostics", R.string.settings_debug_diagnostics_group_title)
    data object MedicalRecords : Screen("medical_records", R.string.medical_records_title)

    /** The screen titles itself with the category's name. */
    data object MedicalRecordCategory : Screen("medical_records/category/{$MEDICAL_CATEGORY_ARG}", R.string.medical_records_title) {
        fun createRoute(category: MedicalCategory): String = "medical_records/category/${category.name}"
    }
    data object MedicalRecordDetail : Screen(
        "medical_records/record/{$MEDICAL_SOURCE_ARG}/{$MEDICAL_TYPE_ARG}/{$MEDICAL_ID_ARG}",
        R.string.medical_record_title,
    ) {
        fun createRoute(ref: MedicalRecordRef): String =
            "medical_records/record/${Uri.encode(ref.dataSourceId)}/${Uri.encode(ref.resourceType)}/${Uri.encode(ref.resourceId)}"
    }
    data object MedicalSources : Screen("medical_records/sources", R.string.medical_sources_title)
    data object MedicalDocuments : Screen("medical_records/documents", R.string.medical_documents_title)

    /** Adds a manual record, or edits one when [MEDICAL_ENTRY_ID_ARG] names it. The screen titles itself. */
    data object MedicalRecordEntry : Screen("medical_records/entry/{$MEDICAL_ENTRY_KIND_ARG}", R.string.medical_entry_add_action) {
        val routePattern: String = "$route?$MEDICAL_ENTRY_ID_ARG={$MEDICAL_ENTRY_ID_ARG}"

        fun createRoute(kind: ManualRecordKind, id: String? = null): String =
            "medical_records/entry/${kind.name}" + (id?.let { "?$MEDICAL_ENTRY_ID_ARG=${Uri.encode(it)}" } ?: "")
    }
    data object WatchDevice : Screen("watch/{$WATCH_DEVICE_ID_ARG}") {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}"
    }
    data object WatchData : Screen("watch/{$WATCH_DEVICE_ID_ARG}/data", R.string.settings_watch_data_title) {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}/data"
    }
    data object WatchNotifications : Screen("watch/{$WATCH_DEVICE_ID_ARG}/notifications", R.string.screen_watch_notifications) {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}/notifications"
    }

    /** Alarms kept on the phone, for a watch with no settings tree. */
    data object WatchAlarms : Screen("watch/{$WATCH_DEVICE_ID_ARG}/alarms", R.string.settings_watch_action_alarms) {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}/alarms"
    }

    /** The watch's own settings tree. */
    data object WatchSettings :
        Screen("watch/{$WATCH_DEVICE_ID_ARG}/settings/{$WATCH_SETTINGS_SCREEN_ID_ARG}", R.string.settings_watch_on_device_settings) {
        fun createRoute(watchDeviceId: String, screenId: Int): String =
            "watch/${Uri.encode(watchDeviceId)}/settings/$screenId"
    }

    /**
     * Send a point to a watch. Every argument is optional: a shared location
     * arrives with a position and no watch, the watch screen with a watch only.
     */
    data object WatchSendPoint : Screen(
        "watch_send_point?$WATCH_DEVICE_ID_ARG={$WATCH_DEVICE_ID_ARG}" +
            "&$WATCH_POINT_LATITUDE_ARG={$WATCH_POINT_LATITUDE_ARG}" +
            "&$WATCH_POINT_LONGITUDE_ARG={$WATCH_POINT_LONGITUDE_ARG}" +
            "&$WATCH_POINT_NAME_ARG={$WATCH_POINT_NAME_ARG}" +
            "&$WATCH_POINT_UNREADABLE_ARG={$WATCH_POINT_UNREADABLE_ARG}",
        R.string.settings_watch_point_title,
    ) {
        fun createRoute(
            watchDeviceId: String? = null,
            latitude: Double? = null,
            longitude: Double? = null,
            name: String? = null,
            unreadable: Boolean = false,
        ): String {
            val query = buildList {
                watchDeviceId?.let { add("$WATCH_DEVICE_ID_ARG=${Uri.encode(it)}") }
                if (latitude != null && longitude != null) {
                    add("$WATCH_POINT_LATITUDE_ARG=$latitude")
                    add("$WATCH_POINT_LONGITUDE_ARG=$longitude")
                }
                name?.let { add("$WATCH_POINT_NAME_ARG=${Uri.encode(it)}") }
                if (unreadable) add("$WATCH_POINT_UNREADABLE_ARG=true")
            }
            return if (query.isEmpty()) basePath else "$basePath?${query.joinToString("&")}"
        }
    }
    data object Achievements : Screen("achievements", R.string.screen_achievements)

    companion object {
        /** Every screen. `ScreenTitleTest` fails when one is missing. */
        val all: List<Screen> by lazy {
            listOf(
                Onboarding,
                Dashboard,
                StressDetails,
                BodyEnergyDetails,
                TrainingReadinessDetails,
                ManualEntry,
                HydrationEntry,
                HydrationEntryEdit,
                HydrationEntryLogDrink,
                NutritionEntry,
                NutritionEntryEdit,
                FoodEntry,
                ActivityEntry,
                WorkoutPlans,
                WorkoutPlanBuilder,
                ActivityEntryEdit,
                MindfulnessEntry,
                MindfulnessEntryEdit,
                BodyMeasurementEntry,
                BodyMeasurementEntryEdit,
                VitalsMeasurementEntry,
                VitalsMeasurementEntryEdit,
                CycleEntryChooser,
                CycleEntry,
                Calories,
                Nutrition,
                Activity,
                ActivityDetail,
                Sleep,
                SleepDetail,
                CaffeineDrink,
                Metric,
                Settings,
                SettingsDisplay,
                SettingsActivities,
                SettingsSensors,
                SettingsWatches,
                SettingsScales,
                ScaleDevice,
                SettingsNutrition,
                SettingsCalories,
                SettingsCaffeine,
                SettingsBodyProfile,
                SettingsVitals,
                SettingsRecovery,
                SettingsCycle,
                SettingsSleep,
                SettingsBodyEnergy,
                SettingsDataImport,
                SettingsCsvImport,
                SettingsReportExport,
                SettingsDeviceSync,
                SettingsHealthConnect,
                SettingsPermissions,
                SettingsDebugDiagnostics,
                MedicalRecords,
                MedicalRecordCategory,
                MedicalRecordDetail,
                MedicalSources,
                MedicalDocuments,
                MedicalRecordEntry,
                SettingsMedicalImport,
                WatchDevice,
                WatchData,
                WatchNotifications,
                WatchAlarms,
                WatchSettings,
                WatchSendPoint,
                Achievements,
            )
        }

        private val titlesByBasePath: Map<String, Int?> by lazy {
            all.associate { it.basePath to it.titleRes }
        }

        /** The title of the screen at [basePath]. Null when that screen titles itself. */
        @StringRes
        fun titleResFor(basePath: String?): Int? = titlesByBasePath[basePath]
    }
}
