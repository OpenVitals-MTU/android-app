package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.domain.model.DashboardMetric

enum class DashboardWidgetId {
    STEPS,
    DISTANCE,
    CALORIES_OUT,
    ACTIVE_CALORIES,
    FLOORS,
    ELEVATION,
    WHEELCHAIR_PUSHES,
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
    FFMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
    BODY_WATER_MASS,
    AVG_HEART_RATE,
    RESTING_HEART_RATE,
    HRV,
    BLOOD_PRESSURE,
    SPO2,
    VO2_MAX,
    RESPIRATORY_RATE,
    BODY_TEMPERATURE,
    BLOOD_GLUCOSE,
    SKIN_TEMPERATURE,
    WEEKLY_CARDIO_LOAD,
    CARDIO_LOAD,
    MINDFULNESS,
    CYCLE,
}

const val DashboardWidgetGridColumns = 2
const val DashboardFixedWidgetRows = 2
const val DashboardCarouselWidgetRows = 3
const val DashboardFixedWidgetCount = DashboardWidgetGridColumns * DashboardFixedWidgetRows

val DefaultDashboardWidgetIds: List<DashboardWidgetId> = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.FLOORS,
    DashboardWidgetId.ELEVATION,
    DashboardWidgetId.WHEELCHAIR_PUSHES,
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
    DashboardWidgetId.FFMI,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.LEAN_MASS,
    DashboardWidgetId.BMR,
    DashboardWidgetId.BONE_MASS,
    DashboardWidgetId.BODY_WATER_MASS,
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

fun customizableDashboardWidgetIds(widgetIds: List<DashboardWidgetId>): List<DashboardWidgetId> =
    widgetIds.distinct()

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

fun DashboardWidgetId.dashboardWidgetRowSpan(): Int = when (this) {
    DashboardWidgetId.STEPS -> 2
    DashboardWidgetId.WEEKLY_CARDIO_LOAD -> 2
    else -> 1
}

fun dashboardWidgetIdsThatFitRows(
    widgetIds: List<DashboardWidgetId>,
    rows: Int,
    columns: Int = DashboardWidgetGridColumns,
): List<DashboardWidgetId> {
    val usedRows = IntArray(columns)
    return buildList {
        widgetIds.forEach { widgetId ->
            val rowSpan = widgetId.dashboardWidgetRowSpan().coerceIn(1, rows)
            val column = usedRows.indices.firstOrNull { usedRows[it] + rowSpan <= rows }
            if (column != null) {
                usedRows[column] += rowSpan
                add(widgetId)
            }
        }
    }
}

fun dashboardWidgetIdsInGridPages(
    widgetIds: List<DashboardWidgetId>,
    rows: Int,
    columns: Int = DashboardWidgetGridColumns,
): List<List<DashboardWidgetId>> {
    val pages = mutableListOf<List<DashboardWidgetId>>()
    var remaining = widgetIds
    while (remaining.isNotEmpty()) {
        val page = dashboardWidgetIdsThatFitRows(
            widgetIds = remaining,
            rows = rows,
            columns = columns,
        ).ifEmpty { listOf(remaining.first()) }
        pages += page
        val pageIds = page.toSet()
        remaining = remaining.filterNot { it in pageIds }
    }
    return pages
}

fun DashboardWidgetId.toDashboardMetricOrNull(): DashboardMetric? = when (this) {
    DashboardWidgetId.STEPS -> DashboardMetric.STEPS
    DashboardWidgetId.DISTANCE -> DashboardMetric.DISTANCE
    DashboardWidgetId.CALORIES_OUT -> DashboardMetric.CALORIES_OUT
    DashboardWidgetId.ACTIVE_CALORIES -> DashboardMetric.ACTIVE_CALORIES
    DashboardWidgetId.FLOORS -> DashboardMetric.FLOORS
    DashboardWidgetId.ELEVATION -> DashboardMetric.ELEVATION
    DashboardWidgetId.WHEELCHAIR_PUSHES -> DashboardMetric.WHEELCHAIR_PUSHES
    DashboardWidgetId.WORKOUT -> DashboardMetric.WORKOUT
    DashboardWidgetId.SLEEP -> DashboardMetric.SLEEP
    DashboardWidgetId.HYDRATION -> DashboardMetric.HYDRATION
    DashboardWidgetId.CALORIES_IN -> DashboardMetric.CALORIES_IN
    DashboardWidgetId.PROTEIN -> DashboardMetric.PROTEIN
    DashboardWidgetId.CARBS -> DashboardMetric.CARBS
    DashboardWidgetId.FAT -> DashboardMetric.FAT
    DashboardWidgetId.WEIGHT -> DashboardMetric.WEIGHT
    DashboardWidgetId.HEIGHT -> DashboardMetric.HEIGHT
    DashboardWidgetId.BMI -> DashboardMetric.BMI
    DashboardWidgetId.FFMI -> DashboardMetric.FFMI
    DashboardWidgetId.BODY_FAT -> DashboardMetric.BODY_FAT
    DashboardWidgetId.LEAN_MASS -> DashboardMetric.LEAN_MASS
    DashboardWidgetId.BMR -> DashboardMetric.BMR
    DashboardWidgetId.BONE_MASS -> DashboardMetric.BONE_MASS
    DashboardWidgetId.BODY_WATER_MASS -> DashboardMetric.BODY_WATER_MASS
    DashboardWidgetId.AVG_HEART_RATE -> DashboardMetric.AVG_HEART_RATE
    DashboardWidgetId.RESTING_HEART_RATE -> DashboardMetric.RESTING_HEART_RATE
    DashboardWidgetId.HRV -> DashboardMetric.HRV
    DashboardWidgetId.BLOOD_PRESSURE -> DashboardMetric.BLOOD_PRESSURE
    DashboardWidgetId.SPO2 -> DashboardMetric.SPO2
    DashboardWidgetId.VO2_MAX -> DashboardMetric.VO2_MAX
    DashboardWidgetId.RESPIRATORY_RATE -> DashboardMetric.RESPIRATORY_RATE
    DashboardWidgetId.BODY_TEMPERATURE -> DashboardMetric.BODY_TEMPERATURE
    DashboardWidgetId.BLOOD_GLUCOSE -> DashboardMetric.BLOOD_GLUCOSE
    DashboardWidgetId.SKIN_TEMPERATURE -> DashboardMetric.SKIN_TEMPERATURE
    DashboardWidgetId.WEEKLY_CARDIO_LOAD -> DashboardMetric.WEEKLY_CARDIO_LOAD
    DashboardWidgetId.CARDIO_LOAD -> DashboardMetric.WEEKLY_CARDIO_LOAD
    DashboardWidgetId.MINDFULNESS -> DashboardMetric.MINDFULNESS
    DashboardWidgetId.CYCLE -> DashboardMetric.CYCLE
}
