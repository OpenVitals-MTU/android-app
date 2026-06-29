package tech.mmarca.openvitals.domain.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
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
}
