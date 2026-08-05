package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad

@Immutable
data class DashboardDisplayState(
    val widgets: Map<DashboardWidgetId, DashboardWidgetDisplayModel> = emptyMap(),
    /**
     * The ids that only exist because `includeUnsupported` materialised them —
     * metrics the installed provider cannot serve. Empty outside edit mode,
     * where an unsupported metric simply has no widget at all. The carousel
     * keeps them out; the edit-mode add tray is where they belong, so a metric
     * the device cannot serve can still be placed rather than vanishing.
     */
    val unsupportedIds: Set<DashboardWidgetId> = emptySet(),
)

@Immutable
data class DashboardWidgetDisplayModel(
    val id: DashboardWidgetId,
    val style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    val value: DisplayValue? = null,
    val hasValue: Boolean = true,
    val progress: DashboardWidgetProgressModel? = null,
    val isLoading: Boolean = false,
    val caloriesSubtitle: CaloriesBurnedSource? = null,
    val sleepScore: SleepScoreDisplay? = null,
    val weeklyCardioLoad: DashboardWeeklyCardioLoad? = null,
    val cycle: CycleWidgetDisplay? = null,
    val measurementSubtitle: String? = null,
    /** Consumed-today figure shown under the active-caffeine headline. */
    val caffeineConsumedTodayMg: Long? = null,
    val bodyEnergySubtitle: BodyEnergyTileSubtitle? = null,
    val showTitle: Boolean = true,
    val requiresNoDataMessage: Boolean = false,
    val isNotSetUp: Boolean = false,
    /** The metric has records in its recent lookback, just none for the selected day. */
    val hasRecentHistory: Boolean = false,
)

@Immutable
data class DashboardWidgetProgressModel(
    val fraction: Float,
    val goalLabelValue: DisplayValue,
)

@Immutable
data class SleepScoreDisplay(
    val score: Int,
    val confidence: SleepScoreConfidence,
    val rating: SleepScoreRating,
)

enum class SleepScoreRating {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
}

@Immutable
sealed interface CycleWidgetDisplay {
    data class MenstruationDays(val days: Int) : CycleWidgetDisplay
    data class OvulationTests(val count: Int) : CycleWidgetDisplay
    data class BasalTemperature(val celsius: Double) : CycleWidgetDisplay
}

internal fun sleepScoreRatingFor(score: Int): SleepScoreRating =
    when {
        score >= 90 -> SleepScoreRating.EXCELLENT
        score >= 80 -> SleepScoreRating.GOOD
        score >= 60 -> SleepScoreRating.FAIR
        else -> SleepScoreRating.POOR
    }

internal fun goalProgressModel(
    current: Double,
    target: Double,
    goalLabelValue: DisplayValue,
): DashboardWidgetProgressModel? {
    if (target <= 0.0) return null
    return DashboardWidgetProgressModel(
        fraction = (current / target).toFloat().coerceIn(0f, 1f),
        goalLabelValue = goalLabelValue,
    )
}

/**
 * Whether this tile renders its no-data message instead of a value. Outside
 * edit mode such tiles sink below tiles with data — display-time only, the
 * saved order is untouched. A tile still loading holds its place: it is about
 * to become either.
 */
data class BodyEnergyTileSubtitle(
    val startScore: Int,
    val charged: Int,
    val drained: Int,
)

/**
 * Whether this tile may be sorted to the back of the carousel.
 *
 * Only a tile whose metric shows nothing today AND has nothing in its recent
 * lookback is worth demoting: the feature exists to sink metrics the user does
 * not use at all, not ones that simply have not happened yet today. A sleep
 * tile that is empty every morning until the night syncs must hold the spot
 * the user gave it (see [DashboardWidgetDisplayModel.hasRecentHistory]);
 * demoting it made every pencil toggle look like the arrangement was ignored.
 *
 * A tile that says "not set up" is the opposite. It is the invitation to turn a
 * feature on, and the dashboard tile is the only place in the app that offers
 * it — so demoting it hides the entry point behind every populated tile, on the
 * exact grounds that the user has not used the feature yet. Body Energy sat
 * eleventh in the default order and rendered on the last page of the carousel
 * for anyone who had not already set it up, which is everyone who needs to.
 */
internal fun DashboardWidgetDisplayModel.isDemotableEmptyTile(): Boolean =
    showsNoDataMessage() && !isNotSetUp && !hasRecentHistory

internal fun DashboardWidgetDisplayModel.showsNoDataMessage(): Boolean = when {
    isLoading -> false
    isNotSetUp -> true
    id == DashboardWidgetId.CYCLE -> cycle == null
    id == DashboardWidgetId.WEEKLY_CARDIO_LOAD || id == DashboardWidgetId.CARDIO_LOAD ->
        weeklyCardioLoad == null
    else -> value == null || !hasValue
}
