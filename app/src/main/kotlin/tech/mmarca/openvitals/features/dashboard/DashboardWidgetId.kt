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
    BODY_ENERGY,
    HYDRATION,
    CALORIES_IN,
    PROTEIN,
    CARBS,
    FAT,
    CAFFEINE,
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

    /** A paired watch. Device state, not a metric. */
    WATCH,
}

const val DashboardWidgetGridColumns = 2
const val DashboardFixedWidgetRows = 2
const val DashboardCarouselWidgetRows = 3
const val DashboardFixedWidgetCount = DashboardWidgetGridColumns * DashboardFixedWidgetRows

val DefaultDashboardWidgetIds: List<DashboardWidgetId> = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    // First after the hero rings: it is the tile the user taps to sync.
    DashboardWidgetId.WATCH,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.FLOORS,
    DashboardWidgetId.ELEVATION,
    DashboardWidgetId.WHEELCHAIR_PUSHES,
    DashboardWidgetId.WORKOUT,
    DashboardWidgetId.SLEEP,
    DashboardWidgetId.BODY_ENERGY,
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.CALORIES_IN,
    DashboardWidgetId.PROTEIN,
    DashboardWidgetId.CARBS,
    DashboardWidgetId.FAT,
    DashboardWidgetId.CAFFEINE,
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

/**
 * Every id that existed before known ids were recorded, so the ids added
 * alongside read as new. Nothing is added here later.
 */
private val WidgetIdsKnownBeforeTracking: Set<String> =
    (DashboardWidgetId.entries - DashboardWidgetId.WATCH).mapTo(mutableSetOf()) { it.name }

/**
 * The saved layout with any never-offered widget appended. The known-ids
 * set tells a deliberate removal from a widget added by an update.
 * Persists both lists when something was added.
 */
fun dashboardWidgetIdsWithNewOnesAppended(
    storedIds: List<String>?,
    knownIds: Set<String>?,
    /** A null order leaves the saved arrangement untouched. */
    persist: (order: List<String>?, known: Set<String>) -> Unit,
): List<DashboardWidgetId> {
    val allIds = DashboardWidgetId.entries.map { it.name }.toSet()
    val recordKnown = { order: List<String>? -> if (knownIds != allIds) persist(order, allIds) }
    // No saved order follows the default. Record the baseline but write no
    // order: an empty one reads as "removed everything".
    if (storedIds == null) {
        recordKnown(null)
        return DefaultDashboardWidgetIds
    }
    val fresh = DashboardWidgetId.entries
        .filterNot { it.name in (knownIds ?: WidgetIdsKnownBeforeTracking) }
    if (fresh.isEmpty()) {
        recordKnown(null)
        return dashboardWidgetIdsFromStored(storedIds)
    }
    val appended = insertByDefaultOrder(storedIds, fresh)
    persist(appended, allIds)
    return dashboardWidgetIdsFromStored(appended)
}

/** Places each new id where the default order puts it relative to the kept widgets. */
private fun insertByDefaultOrder(
    stored: List<String>,
    fresh: List<DashboardWidgetId>,
): List<String> {
    val defaultIndex = DefaultDashboardWidgetIds
        .withIndex()
        .associate { (index, id) -> id.name to index }
    val positionOf = { name: String -> defaultIndex[name] ?: Int.MAX_VALUE }
    val result = stored.toMutableList()
    fresh.sortedBy { positionOf(it.name) }
        // Already in the layout: trust the order over the known-ids set.
        .filterNot { it.name in stored }
        .forEach { id ->
        val target = positionOf(id.name)
        // After the last kept widget that precedes it, so an unlisted id sorts last.
        result.add(result.indexOfLast { positionOf(it) < target } + 1, id.name)
    }
    return result
}

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

/**
 * Whether the provider can serve this widget's metric. BODY_ENERGY follows
 * heart rate. A widget with no metric is never gated.
 */
internal fun DashboardWidgetId.isSupportedBy(supportedMetrics: Set<DashboardMetric>): Boolean =
    when (this) {
        DashboardWidgetId.BODY_ENERGY -> DashboardMetric.AVG_HEART_RATE in supportedMetrics
        else -> toDashboardMetricOrNull()?.let { it in supportedMetrics } ?: true
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
    DashboardWidgetId.BODY_ENERGY -> null
    DashboardWidgetId.HYDRATION -> DashboardMetric.HYDRATION
    DashboardWidgetId.CALORIES_IN -> DashboardMetric.CALORIES_IN
    DashboardWidgetId.PROTEIN -> DashboardMetric.PROTEIN
    DashboardWidgetId.CARBS -> DashboardMetric.CARBS
    DashboardWidgetId.FAT -> DashboardMetric.FAT
    DashboardWidgetId.CAFFEINE -> DashboardMetric.CAFFEINE
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
    // Device state, never gated.
    DashboardWidgetId.WATCH -> null
}
