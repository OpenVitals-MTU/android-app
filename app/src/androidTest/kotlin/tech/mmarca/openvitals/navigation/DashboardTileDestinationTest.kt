package tech.mmarca.openvitals.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId

/**
 * Ported from the `tile destinations match Kotlin` group of Flutter's
 * `test/features/dashboard/dashboard_summary_visibility_test.dart`.
 *
 * That group exists in the Dart suite purely to pin parity WITH this app, so it
 * is the one place worth asserting twice: a tapped tile has to reach the screen
 * that actually shows the metric. The ten heart/vitals ids share one overview
 * rather than each owning a `/metric/{id}` screen, and the three consumption
 * metrics do the opposite — they keep their own detail views.
 *
 * The day rides along as the optional `?day=` argument (Flutter's locations
 * carry no day at all), so a fixed past date is used rather than today, whose
 * suffix `withSelectedDay` deliberately omits.
 *
 * Instrumentation rather than a JVM unit test only because
 * `Screen.Metric.createRoute` percent-encodes through `android.net.Uri`, which
 * is a throwing stub off-device; the function itself needs no device.
 */
@RunWith(AndroidJUnit4::class)
class DashboardTileDestinationTest {

    private val selectedDay = LocalDate.of(2026, 3, 14)

    @Test
    fun heartAndVitalsTilesAllOpenTheHeartVitalsOverview() {
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
                "heart_vitals?day=2026-03-14",
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
