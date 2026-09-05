package tech.mmarca.openvitals.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId

/**
 * A tapped tile has to reach the screen that shows the metric. Heart, vitals and body ids
 * each own their `/metric/{id}` screen. Instrumentation only because `Screen.Metric.createRoute`
 * percent-encodes through `android.net.Uri`, a throwing stub off-device.
 */
@RunWith(AndroidJUnit4::class)
class DashboardTileDestinationTest {

    private val selectedDay = LocalDate.of(2026, 3, 14)

    @Test
    fun heartAndVitalsTilesEachOpenTheirOwnMetricScreen() {
        listOf(
            DashboardWidgetId.AVG_HEART_RATE,
            DashboardWidgetId.RESTING_HEART_RATE,
            DashboardWidgetId.HRV,
            DashboardWidgetId.BLOOD_PRESSURE,
            DashboardWidgetId.SPO2,
            DashboardWidgetId.VO2_MAX,
            DashboardWidgetId.RESPIRATORY_RATE,
            DashboardWidgetId.BODY_TEMPERATURE,
            DashboardWidgetId.BLOOD_GLUCOSE,
            DashboardWidgetId.SKIN_TEMPERATURE,
        ).forEach { metricId ->
            assertEquals(
                metricId.name,
                "metric/${metricId.name}?day=2026-03-14",
                dashboardTileDestination(metricId, selectedDay),
            )
        }
    }

    @Test
    fun bodyTilesEachOpenTheirOwnMetricScreen() {
        listOf(
            DashboardWidgetId.WEIGHT,
            DashboardWidgetId.HEIGHT,
            DashboardWidgetId.BMI,
            DashboardWidgetId.FFMI,
            DashboardWidgetId.BODY_FAT,
            DashboardWidgetId.LEAN_MASS,
            DashboardWidgetId.BONE_MASS,
            DashboardWidgetId.BODY_WATER_MASS,
        ).forEach { metricId ->
            assertEquals(
                metricId.name,
                "metric/${metricId.name}?day=2026-03-14",
                dashboardTileDestination(metricId, selectedDay),
            )
        }
    }

    @Test
    fun hydrationMindfulnessAndCaffeineTilesOpenTheirDetailViews() {
        assertEquals(
            "metric/HYDRATION?day=2026-03-14",
            dashboardTileDestination(DashboardWidgetId.HYDRATION, selectedDay),
        )
        assertEquals(
            "metric/MINDFULNESS?day=2026-03-14",
            dashboardTileDestination(DashboardWidgetId.MINDFULNESS, selectedDay),
        )
        assertEquals(
            "metric/CAFFEINE?day=2026-03-14",
            dashboardTileDestination(DashboardWidgetId.CAFFEINE, selectedDay),
        )
    }

    @Test
    fun everyDashboardTileHasADestination() {
        DashboardWidgetId.entries.forEach { metricId ->
            assertTrue(
                "$metricId has no destination",
                dashboardTileDestination(metricId, selectedDay).isNotBlank(),
            )
        }
    }
}
