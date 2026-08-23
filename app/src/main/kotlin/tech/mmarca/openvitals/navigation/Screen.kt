package tech.mmarca.openvitals.navigation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate

const val ACTIVITY_DETAIL_ID_ARG = "activityId"

/**
 * Optional query argument carrying the day a metric screen should open on —
 * a tile tapped on the dashboard's "yesterday" opens yesterday. It anchors the
 * DATE only: the screen's persisted range is untouched, so a screen left on
 * Week opens the week containing that day.
 */
const val SELECTED_DAY_ARG = "day"

/** The `?day={day}` suffix a metric route pattern declares to accept the argument. */
const val SELECTED_DAY_QUERY_PATTERN = "?$SELECTED_DAY_ARG={$SELECTED_DAY_ARG}"

/**
 * Appends the selected day to a navigation target. Today is deliberately
 * omitted so the ordinary location stays clean.
 */
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
const val ACTIVITY_ENTRY_TYPE_ARG = "activityTypeId"
const val SLEEP_DETAIL_ID_ARG = "sleepId"
const val METRIC_ID_ARG = "metricId"
const val BODY_MEASUREMENT_TYPE_ARG = "bodyMeasurementType"
const val BODY_ENTRY_ID_ARG = "bodyEntryId"
const val HYDRATION_ENTRY_ID_ARG = "hydrationEntryId"
const val HYDRATION_DRINK_ID_ARG = "hydrationDrinkId"
const val MINDFULNESS_ENTRY_ID_ARG = "mindfulnessEntryId"
const val CAFFEINE_ENTRY_ID_ARG = "caffeineEntryId"
const val VITALS_MEASUREMENT_TYPE_ARG = "vitalsMeasurementType"
const val VITALS_ENTRY_ID_ARG = "vitalsEntryId"
const val CYCLE_ENTRY_KIND_ARG = "cycleEntryKind"
const val CYCLE_ENTRY_ID_ARG = "cycleEntryId"
const val STRESS_DATE_ARG = "stressDate"
const val WATCH_DEVICE_ID_ARG = "watchDeviceId"
const val WATCH_SETTINGS_SCREEN_ID_ARG = "screenId"
const val BODY_ENERGY_DATE_ARG = "bodyEnergyDate"
const val TRAINING_READINESS_DATE_ARG = "trainingReadinessDate"

sealed class Screen(val route: String) {
    /**
     * The route without its optional query parameters.
     *
     * `AppNavigation` matches the live destination with `route.substringBefore('?')`, because a
     * destination registered with optional arguments reports them as part of its pattern. Most
     * routes carry their arguments in the path and are their own base path; [ActivityEntry] bakes
     * `?mode=...&planId=...&activityTypeId=...` into its pattern, so comparing the stripped live
     * route against the full [route] never matched and every app-bar decision keyed on that screen
     * silently fell through. Match on this, not on [route].
     */
    val basePath: String get() = route.substringBefore('?')

    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object StressDetails : Screen("daily_readiness/stress/{$STRESS_DATE_ARG}") {
        fun createRoute(date: String): String = "daily_readiness/stress/${Uri.encode(date)}"
    }
    data object BodyEnergyDetails : Screen("daily_readiness/body_energy/{$BODY_ENERGY_DATE_ARG}") {
        fun createRoute(date: String): String = "daily_readiness/body_energy/${Uri.encode(date)}"
    }
    data object TrainingReadinessDetails :
        Screen("daily_readiness/training_readiness/{$TRAINING_READINESS_DATE_ARG}") {
        fun createRoute(date: String): String = "daily_readiness/training_readiness/${Uri.encode(date)}"
    }
    data object ManualEntry : Screen("manual_entry")
    data object HydrationEntry : Screen("manual_entry/hydration")
    data object HydrationEntryEdit : Screen("manual_entry/hydration/edit/{$HYDRATION_ENTRY_ID_ARG}") {
        fun createRoute(entryId: String): String = "manual_entry/hydration/edit/${Uri.encode(entryId)}"
    }
    data object HydrationEntryLogDrink : Screen("manual_entry/hydration/log/{$HYDRATION_DRINK_ID_ARG}") {
        fun createRoute(drinkId: String): String = "manual_entry/hydration/log/${Uri.encode(drinkId)}"
    }
    data object CarbsEntry : Screen("manual_entry/carbs")
    data object ActivityEntry : Screen(
        "manual_entry/activity" +
            "?$ACTIVITY_ENTRY_MODE_ARG={$ACTIVITY_ENTRY_MODE_ARG}" +
            "&$ACTIVITY_ENTRY_PLAN_ID_ARG={$ACTIVITY_ENTRY_PLAN_ID_ARG}" +
            "&$ACTIVITY_ENTRY_TYPE_ARG={$ACTIVITY_ENTRY_TYPE_ARG}",
    ) {
        /**
         * Builds a concrete navigation target for the activity entry screen, carrying the
         * caller's intent as optional query arguments. With no arguments this resolves to the
         * bare `manual_entry/activity` path, which still matches the route pattern above.
         */
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
    data object ActivityEntryEdit : Screen("manual_entry/activity/edit/{$ACTIVITY_ENTRY_ID_ARG}") {
        fun createRoute(entryId: String): String = "manual_entry/activity/edit/${Uri.encode(entryId)}"
    }
    data object MindfulnessEntry : Screen("manual_entry/mindfulness")
    data object MindfulnessEntryEdit : Screen("manual_entry/mindfulness/edit/{$MINDFULNESS_ENTRY_ID_ARG}") {
        fun createRoute(entryId: String): String = "manual_entry/mindfulness/edit/${Uri.encode(entryId)}"
    }
    data object BodyMeasurementEntry : Screen("manual_entry/body/{$BODY_MEASUREMENT_TYPE_ARG}") {
        fun createRoute(type: String): String = "manual_entry/body/${Uri.encode(type)}"
    }
    data object BodyMeasurementEntryEdit :
        Screen("manual_entry/body/{$BODY_MEASUREMENT_TYPE_ARG}/edit/{$BODY_ENTRY_ID_ARG}") {
        fun createRoute(type: String, entryId: String): String =
            "manual_entry/body/${Uri.encode(type)}/edit/${Uri.encode(entryId)}"
    }
    data object VitalsMeasurementEntry : Screen("manual_entry/vitals/{$VITALS_MEASUREMENT_TYPE_ARG}") {
        fun createRoute(type: String): String = "manual_entry/vitals/${Uri.encode(type)}"
    }
    data object VitalsMeasurementEntryEdit :
        Screen("manual_entry/vitals/{$VITALS_MEASUREMENT_TYPE_ARG}/edit/{$VITALS_ENTRY_ID_ARG}") {
        fun createRoute(type: String, entryId: String): String =
            "manual_entry/vitals/${Uri.encode(type)}/edit/${Uri.encode(entryId)}"
    }
    data object CycleEntry : Screen("manual_entry/cycle")
    data object CycleEntryEdit :
        Screen("manual_entry/cycle/edit/{$CYCLE_ENTRY_KIND_ARG}/{$CYCLE_ENTRY_ID_ARG}") {
        fun createRoute(kind: String, entryId: String): String =
            "manual_entry/cycle/edit/${Uri.encode(kind)}/${Uri.encode(entryId)}"
    }
    data object Calories : Screen("calories")
    data object Nutrition : Screen("nutrition")
    data object Activity : Screen("activity")
    data object ActivityDetail : Screen("activity_detail/{$ACTIVITY_DETAIL_ID_ARG}") {
        fun createRoute(activityId: String): String = "activity_detail/${Uri.encode(activityId)}"
    }
    data object Sleep : Screen("sleep")
    data object SleepDetail : Screen("sleep_detail/{$SLEEP_DETAIL_ID_ARG}") {
        fun createRoute(sleepId: String): String = "sleep_detail/${Uri.encode(sleepId)}"
    }
    data object CaffeineDrink : Screen("caffeine/drink/{$CAFFEINE_ENTRY_ID_ARG}") {
        fun createRoute(entryId: String): String = "caffeine/drink/${Uri.encode(entryId)}"
    }
    data object Metric : Screen("metric/{$METRIC_ID_ARG}") {
        fun createRoute(metricId: String): String = "metric/${Uri.encode(metricId)}"
    }
    data object Settings : Screen("settings")
    data object SettingsDisplay : Screen("settings/display")
    data object SettingsActivities : Screen("settings/activities")
    data object SettingsSensors : Screen("settings/sensors")
    data object SettingsWatches : Screen("settings/watches")
    data object SettingsNutrition : Screen("settings/nutrition")
    data object SettingsCalories : Screen("settings/calories")
    data object SettingsCaffeine : Screen("settings/caffeine")
    data object SettingsBodyProfile : Screen("settings/body_profile")
    data object SettingsRecovery : Screen("settings/recovery")
    data object SettingsSleep : Screen("settings/sleep")
    data object SettingsBodyEnergy : Screen("settings/body_energy")
    data object SettingsDataImport : Screen("settings/data_import")
    data object SettingsCsvImport : Screen("settings/data_import/csv")
    data object SettingsReportExport : Screen("settings/data_import/report")
    data object SettingsDeviceSync : Screen("settings/device_sync")
    data object SettingsHealthConnect : Screen("settings/health_connect")
    data object SettingsPermissions : Screen("settings/permissions")
    data object SettingsDebugDiagnostics : Screen("settings/debug_diagnostics")
    data object WatchDevice : Screen("watch/{$WATCH_DEVICE_ID_ARG}") {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}"
    }
    data object WatchData : Screen("watch/{$WATCH_DEVICE_ID_ARG}/data") {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}/data"
    }
    data object WatchNotifications : Screen("watch/{$WATCH_DEVICE_ID_ARG}/notifications") {
        fun createRoute(watchDeviceId: String): String =
            "watch/${Uri.encode(watchDeviceId)}/notifications"
    }

    /**
     * The watch's own settings tree. The route pattern is registered ahead of
     * sub-milestone 7f, which supplies the browser; until then it resolves to
     * a placeholder and nothing links to it (see `WatchSettingsTreeAvailable`).
     */
    data object WatchSettings :
        Screen("watch/{$WATCH_DEVICE_ID_ARG}/settings/{$WATCH_SETTINGS_SCREEN_ID_ARG}") {
        fun createRoute(watchDeviceId: String, screenId: Int): String =
            "watch/${Uri.encode(watchDeviceId)}/settings/$screenId"
    }
    data object Achievements : Screen("achievements")
}
