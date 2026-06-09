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

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
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
    data object Steps : Screen("steps")
    data object Calories : Screen("calories")
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
    data object Heart : Screen("heart")
    data object Body : Screen("body")
    data object Hydration : Screen("hydration")
    data object Nutrition : Screen("nutrition")
    data object Mindfulness : Screen("mindfulness")
    data object Cycle : Screen("cycle")
    data object Settings : Screen("settings")
    data object Achievements : Screen("achievements")
}
