package tech.mmarca.openvitals.navigation

import android.net.Uri

const val ACTIVITY_DETAIL_ID_ARG = "activityId"
const val ACTIVITY_ENTRY_ID_ARG = "activityEntryId"
const val SLEEP_DETAIL_ID_ARG = "sleepId"
const val METRIC_ID_ARG = "metricId"
const val BODY_MEASUREMENT_TYPE_ARG = "bodyMeasurementType"
const val BODY_ENTRY_ID_ARG = "bodyEntryId"
const val HYDRATION_ENTRY_ID_ARG = "hydrationEntryId"
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
    data object ActivityEntry : Screen("manual_entry/activity")
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
    data object SettingsCalories : Screen("settings/calories")
    data object SettingsSleep : Screen("settings/sleep")
    data object SettingsCycle : Screen("settings/cycle")
    data object SettingsDataImport : Screen("settings/data_import")
    data object SettingsPermissions : Screen("settings/permissions")
    data object Achievements : Screen("achievements")
}
