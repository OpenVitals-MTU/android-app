package tech.mmarca.openvitals.domain.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.CardioLoadEstimate
import tech.mmarca.openvitals.domain.insights.CardioLoadMethod
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.dashboard.DashboardAggregator.medianLongOrNull
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import java.time.LocalDate

class DashboardAggregatorTest {

    @Test fun `weekly cardio target prefers recent history median`() {
        val target = DashboardAggregator.weeklyCardioTarget(
            currentScore = 120,
            daysElapsed = 3,
            previousWeekScores = listOf(0, 100, 110, 105),
        )

        assertEquals(105, target?.score)
        assertEquals(DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY, target?.source)
    }

    @Test fun `merge derived projection keeps base calories unless estimated projection loaded`() {
        val base = DashboardData(
            date = LocalDate.of(2026, 6, 1),
            caloriesKcal = 100.0,
            caloriesKcalSource = CaloriesBurnedSource.NO_DATA,
        )
        val projection = DashboardData(
            date = LocalDate.of(2026, 6, 1),
            caloriesKcal = 456.0,
            caloriesKcalSource = CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR,
            loadedMetrics = setOf(DashboardMetric.CALORIES_OUT),
        )

        val merged = DashboardAggregator.mergeDerivedDashboardProjection(base, projection)

        assertEquals(456.0, merged.caloriesKcal, 0.01)
        assertEquals(CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR, merged.caloriesKcalSource)
        assertEquals(setOf(DashboardMetric.CALORIES_OUT), merged.loadedMetrics)
    }

    @Test fun `median long returns middle value`() {
        assertEquals(5L, listOf(1L, 5L, 9L).medianLongOrNull())
        assertNull(emptyList<Long>().medianLongOrNull())
    }

    private fun trimpDay(score: Int) = CardioLoadEstimate(
        score = score,
        confidence = CardioLoadConfidence.MEDIUM,
        method = CardioLoadMethod.TRIMP_ELEVATED_HEART_RATE,
    )

    private fun fallbackDay(score: Int) = CardioLoadEstimate(
        score = score,
        confidence = CardioLoadConfidence.LOW,
        method = CardioLoadMethod.MOVEMENT_FALLBACK,
    )

    @Test fun `fallback-scored weeks do not set the target for a TRIMP week`() {
        // Older weeks score from steps only. Their median once stood in as the target for a
        // TRIMP-scored week, and readiness announced "2640% of your current load target".
        val currentWeek = List(3) { trimpDay(40) }
        val previousWeeks = listOf(
            List(7) { trimpDay(20) },      // comparable: also HR-scored
            List(7) { fallbackDay(2) },    // steps-only, different yardstick
            List(7) { fallbackDay(1) },
            List(7) { fallbackDay(2) },
        )

        val scores = DashboardAggregator.comparablePreviousWeekScores(currentWeek, previousWeeks)

        assertEquals(listOf(140), scores)
    }

    @Test fun `a movement-only user keeps their fallback baseline`() {
        // No heart rate anywhere: fallback weeks compare with fallback weeks.
        val currentWeek = List(3) { fallbackDay(3) }
        val previousWeeks = listOf(
            List(7) { fallbackDay(2) },
            List(7) { fallbackDay(3) },
        )

        val scores = DashboardAggregator.comparablePreviousWeekScores(currentWeek, previousWeeks)

        assertEquals(listOf(14, 21), scores)
    }

    @Test fun `with no comparable weeks the target falls back to current pace`() {
        val target = DashboardAggregator.weeklyCardioTarget(
            currentScore = 120,
            daysElapsed = 3,
            previousWeekScores = emptyList(),
        )

        // 120 over 3 days paces to 280 for the week: the ratio stays near daysElapsed/7.
        assertEquals(DashboardWeeklyCardioLoadTargetSource.CURRENT_PACE, target?.source)
    }
}
