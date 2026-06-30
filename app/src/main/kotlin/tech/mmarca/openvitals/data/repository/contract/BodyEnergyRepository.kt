package tech.mmarca.openvitals.data.repository.contract

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.model.RefreshMode

data class BodyEnergyTimelineQuery(
    val period: DatePeriod,
    val range: TimeRange,
    val refreshMode: RefreshMode = RefreshMode.NORMAL,
)

data class BodyEnergyTimelineResult(
    val query: BodyEnergyTimelineQuery,
    val days: List<BodyEnergyTimeline>,
) {
    val latestDay: BodyEnergyTimeline?
        get() = days.lastOrNull { it.points.isNotEmpty() } ?: days.lastOrNull()

    val currentScore: Int?
        get() = latestDay?.currentScore

    val charged: Int
        get() = days.sumOf { it.charged }

    val drained: Int
        get() = days.sumOf { it.drained }
}

interface BodyEnergyRepository {
    suspend fun loadTimeline(query: BodyEnergyTimelineQuery): BodyEnergyTimelineResult
}
