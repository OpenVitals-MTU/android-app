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

    /**
     * A paired watch. Device state, not a metric — it has no
     * [DashboardMetric], and materialises only once a watch exists.
     */
    WATCH,
}

const val DashboardWidgetGridColumns = 2
const val DashboardFixedWidgetRows = 2
const val DashboardCarouselWidgetRows = 3
const val DashboardFixedWidgetCount = DashboardWidgetGridColumns * DashboardFixedWidgetRows

val DefaultDashboardWidgetIds: List<DashboardWidgetId> = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    // First tile after the two hero rings: it is the one the user taps to
    // sync, so it belongs on the first page rather than the last.
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
 * Every id that existed before [dashboardKnownWidgetIds] started being
 * recorded. Seeded on the first migration so the ids introduced alongside it —
 * [DashboardWidgetId.WATCH] — read as NEW rather than as widgets the user had
 * already been offered and chosen to drop.
 *
 * A later release adds nothing here: from now on the stored set is maintained,
 * so any newly added id is automatically new to every install.
 */
private val WidgetIdsKnownBeforeTracking: Set<String> =
    (DashboardWidgetId.entries - DashboardWidgetId.WATCH).mapTo(mutableSetOf()) { it.name }

/**
 * The saved layout with any widget this install has never offered appended.
 *
 * The saved order is the user's arrangement, so a widget missing from it is
 * normally one they removed on purpose — but a widget added by an app update is
 * missing for a completely different reason, and returning the stored list
 * verbatim hid it from every user who had ever edited their dashboard. The
 * known-ids set is what separates the two, so a deliberate removal stays
 * removed while a genuinely new tile appears at the end.
 *
 * Returns the ids to render, and persists both lists when something was added.
 */
fun dashboardWidgetIdsWithNewOnesAppended(
    storedIds: List<String>?,
    knownIds: Set<String>?,
    /** A null order leaves the saved arrangement untouched. */
    persist: (order: List<String>?, known: Set<String>) -> Unit,
): List<DashboardWidgetId> {
    val allIds = DashboardWidgetId.entries.map { it.name }.toSet()
    val recordKnown = { order: List<String>? -> if (knownIds != allIds) persist(order, allIds) }
    // An install that has never edited its dashboard follows the default order,
    // which already carries every id. Record the baseline for the next update,
    // but write NO order: an empty one reads as "the user removed everything"
    // and would leave the dashboard blank.
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

/**
 * Places each new id where the DEFAULT order puts it relative to the widgets
 * the user kept, rather than at the end.
 *
 * Appending buried a new tile on the last carousel page, which for something
 * like the watch — the tile you tap to sync — is the wrong end of the
 * dashboard. The user's own arrangement is untouched: only the new id is
 * positioned, and only against the tiles still present.
 */
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
        // Already in the layout: the known-ids set says it is new, the saved
        // order says otherwise. Trust the order — inserting again would write
        // the id twice, and only the render-time distinct() would hide it.
        .filterNot { it.name in stored }
        .forEach { id ->
        val target = positionOf(id.name)
        // Straight after the last kept widget that PRECEDES it in the default
        // order, rather than before the first that follows: an id the default
        // order does not mention sorts as "after everything", and scanning
        // forwards would let the first of those block the insertion.
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
 * Whether the installed Health Connect provider can serve this widget's metric
 * at all (see [tech.mmarca.openvitals.domain.model.DashboardData.supportedMetrics]).
 *
 * BODY_ENERGY is derived rather than read, so it has no [DashboardMetric] of its
 * own; it follows heart rate, the reading it is computed from. A widget with no
 * metric behind it at all (WORKOUT renders through the activities section) is
 * never gated.
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
    // Device state: nothing in Health Connect backs it, so it is never gated.
    DashboardWidgetId.WATCH -> null
}
