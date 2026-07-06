package tech.mmarca.openvitals.navigation

import android.net.Uri

const val ACTIVITY_DETAIL_ID_ARG = "activityId"
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
const val VITALS_MEASUREMENT_TYPE_ARG = "vitalsMeasurementType"
const val VITALS_ENTRY_ID_ARG = "vitalsEntryId"
const val STRESS_DATE_ARG = "stressDate"
const val BODY_ENERGY_DATE_ARG = "bodyEnergyDate"
const val TRAINING_READINESS_DATE_ARG = "trainingReadinessDate"

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object DailyReadiness : Screen("daily_readiness")
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
    data object Calories : Screen("calories")
    data object Nutrition : Screen("nutrition")
    data object Body : Screen("body")
    data object HeartVitals : Screen("heart_vitals")
    data object Activity : Screen("activity")
    data object ActivityDetail : Screen("activity_detail/{$ACTIVITY_DETAIL_ID_ARG}") {
        fun createRoute(activityId: String): String = "activity_detail/${Uri.encode(activityId)}"
    }
    data object Sleep : Screen("sleep")
    data object SleepDetail : Screen("sleep_detail/{$SLEEP_DETAIL_ID_ARG}") {
        fun createRoute(sleepId: String): String = "sleep_detail/${Uri.encode(sleepId)}"
    }
    data object Metric : Screen("metric/{$METRIC_ID_ARG}") {
        fun createRoute(metricId: String): String = "metric/${Uri.encode(metricId)}"
    }
    data object Settings : Screen("settings")
    data object SettingsDisplay : Screen("settings/display")
    data object SettingsActivities : Screen("settings/activities")
    data object SettingsSensors : Screen("settings/sensors")
    data object SettingsNutrition : Screen("settings/nutrition")
    data object SettingsCalories : Screen("settings/calories")
    data object SettingsCaffeine : Screen("settings/caffeine")
    data object SettingsRecovery : Screen("settings/recovery")
    data object SettingsSleep : Screen("settings/sleep")
    data object SettingsBodyEnergy : Screen("settings/body_energy")
    data object SettingsDataImport : Screen("settings/data_import")
    data object SettingsHealthConnect : Screen("settings/health_connect")
    data object SettingsPermissions : Screen("settings/permissions")
    data object SettingsDebugDiagnostics : Screen("settings/debug_diagnostics")
    data object Achievements : Screen("achievements")
}
