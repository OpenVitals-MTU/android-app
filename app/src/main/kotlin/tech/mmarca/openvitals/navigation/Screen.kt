package tech.mmarca.openvitals.navigation

import android.net.Uri

const val ACTIVITY_DETAIL_ID_ARG = "activityId"
const val SLEEP_DETAIL_ID_ARG = "sleepId"
const val METRIC_ID_ARG = "metricId"

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
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
