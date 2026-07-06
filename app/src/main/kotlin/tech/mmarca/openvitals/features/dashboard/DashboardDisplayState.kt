package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad

@Immutable
data class DashboardDisplayState(
    val widgets: Map<DashboardWidgetId, DashboardWidgetDisplayModel> = emptyMap(),
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
    val showTitle: Boolean = true,
    val requiresNoDataMessage: Boolean = false,
    val isNotSetUp: Boolean = false,
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
