package tech.mmarca.openvitals.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Steps : Screen("steps")
    data object Activity : Screen("activity")
    data object Sleep : Screen("sleep")
    data object Heart : Screen("heart")
    data object Body : Screen("body")
    data object Hydration : Screen("hydration")
    data object Browse : Screen("browse")
    data object Settings : Screen("settings")
}
