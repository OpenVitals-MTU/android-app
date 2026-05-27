package tech.mmarca.openvitals.navigation

import android.net.Uri

const val ACTIVITY_DETAIL_ID_ARG = "activityId"
const val SLEEP_DETAIL_ID_ARG = "sleepId"
const val METRIC_ID_ARG = "metricId"
const val BODY_MEASUREMENT_TYPE_ARG = "bodyMeasurementType"
const val VITALS_MEASUREMENT_TYPE_ARG = "vitalsMeasurementType"

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object ManualEntry : Screen("manual_entry")
    data object HydrationEntry : Screen("manual_entry/hydration")
    data object ActivityEntry : Screen("manual_entry/activity")
    data object MindfulnessEntry : Screen("manual_entry/mindfulness")
    data object BodyMeasurementEntry : Screen("manual_entry/body/{$BODY_MEASUREMENT_TYPE_ARG}") {
        fun createRoute(type: String): String = "manual_entry/body/${Uri.encode(type)}"
    }
    data object VitalsMeasurementEntry : Screen("manual_entry/vitals/{$VITALS_MEASUREMENT_TYPE_ARG}") {
        fun createRoute(type: String): String = "manual_entry/vitals/${Uri.encode(type)}"
    }
    data object Steps : Screen("steps")
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
    data object Browse : Screen("browse")
    data object Settings : Screen("settings")
}
