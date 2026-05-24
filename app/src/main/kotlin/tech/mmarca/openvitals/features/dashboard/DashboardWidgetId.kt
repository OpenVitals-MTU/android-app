package tech.mmarca.openvitals.features.dashboard

enum class DashboardWidgetId {
    STEPS,
    DISTANCE,
    CALORIES_OUT,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WORKOUT,
    SLEEP,
    HYDRATION,
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    AVG_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    MINDFULNESS,
    CYCLE,
    BROWSE,
}

const val DashboardFixedWidgetCount = 4

val DefaultDashboardWidgetIds: List<DashboardWidgetId> = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.FLOORS,
    DashboardWidgetId.ELEVATION,
    DashboardWidgetId.WORKOUT,
    DashboardWidgetId.SLEEP,
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.CALORIES_IN,
    DashboardWidgetId.PROTEIN,
    DashboardWidgetId.CARBS,
    DashboardWidgetId.FAT,
    DashboardWidgetId.WEIGHT,
    DashboardWidgetId.HEIGHT,
    DashboardWidgetId.BMI,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.LEAN_MASS,
    DashboardWidgetId.BMR,
    DashboardWidgetId.BONE_MASS,
    DashboardWidgetId.AVG_HEART_RATE,
    DashboardWidgetId.RESTING_HEART_RATE,
    DashboardWidgetId.HRV,
    DashboardWidgetId.BLOOD_PRESSURE,
    DashboardWidgetId.SPO2,
    DashboardWidgetId.VO2_MAX,
    DashboardWidgetId.RESPIRATORY_RATE,
    DashboardWidgetId.BODY_TEMPERATURE,
    DashboardWidgetId.MINDFULNESS,
    DashboardWidgetId.CYCLE,
)

private val FixedDashboardWidgetIds = setOf(DashboardWidgetId.BROWSE)

fun customizableDashboardWidgetIds(widgetIds: List<DashboardWidgetId>): List<DashboardWidgetId> =
    widgetIds.filterNot { it in FixedDashboardWidgetIds }.distinct()

fun dashboardWidgetIdsFromStored(storedIds: List<String>?): List<DashboardWidgetId> {
    if (storedIds == null) return DefaultDashboardWidgetIds
    if (storedIds.isEmpty()) return emptyList()

    val parsedIds = storedIds
        .mapNotNull { storedId ->
            runCatching { DashboardWidgetId.valueOf(storedId) }.getOrNull()
        }
        .let(::customizableDashboardWidgetIds)

    return parsedIds.ifEmpty { DefaultDashboardWidgetIds }
}
