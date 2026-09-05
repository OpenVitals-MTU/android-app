package tech.mmarca.openvitals.features.homewidgets

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.isSupportedOpenVitalsRoute
import tech.mmarca.openvitals.migratedOpenVitalsRoute
import tech.mmarca.openvitals.navigation.EXTRA_OPENVITALS_ROUTE
import tech.mmarca.openvitals.navigation.Screen
import tech.mmarca.openvitals.openVitalsRoute

/** The launch extra a widget carries into MainActivity. Anything not on the allow-list is dropped. */
class HomeWidgetLaunchRouteTest {
    @Before
    fun setUp() {
        mockUriCodec()
    }

    @After
    fun tearDown() {
        unmockUriCodec()
    }

    @Test
    fun `maps every allowed widget route to its destination`() {
        assertTrue(isSupportedOpenVitalsRoute(Screen.Dashboard.route))
        assertTrue(isSupportedOpenVitalsRoute("daily_readiness/body_energy/2026-07-10"))
        assertTrue(isSupportedOpenVitalsRoute("metric/STEPS"))
        assertTrue(isSupportedOpenVitalsRoute(Screen.HydrationEntry.route))
        assertTrue(isSupportedOpenVitalsRoute("manual_entry/hydration/log/coffee"))
        assertTrue(isSupportedOpenVitalsRoute(Screen.ActivityEntry.createRoute()))
    }

    @Test
    fun `every route the snapshot builders write is on the allow-list`() {
        // The builders and the allow-list are written apart; if one drifts, the tap is a no-op.
        val today = LocalDate.of(2026, 7, 10)
        DashboardWidgetId.entries.forEach { metricId ->
            val route = homeMetricWidgetRoute(metricId, today)
            assertTrue(route, isSupportedOpenVitalsRoute(route))
        }
        assertTrue(isSupportedOpenVitalsRoute(Screen.BodyEnergyDetails.createRoute(today.toString())))
        assertTrue(isSupportedOpenVitalsRoute(Screen.HydrationEntryLogDrink.createRoute("coffee")))
    }

    @Test
    fun `rejects a route that is not on the allow-list`() {
        assertFalse(isSupportedOpenVitalsRoute(Screen.Settings.route))
        assertFalse(isSupportedOpenVitalsRoute("settings/permissions"))
        assertFalse(isSupportedOpenVitalsRoute("settings/debug_diagnostics"))
        assertFalse(isSupportedOpenVitalsRoute("nonsense"))
        assertFalse(isSupportedOpenVitalsRoute(""))
    }

    @Test
    fun `rejects a malformed argument`() {
        // Not a known metric id.
        assertFalse(isSupportedOpenVitalsRoute("metric/NOT_A_METRIC"))
        // Not an ISO date.
        assertFalse(isSupportedOpenVitalsRoute("daily_readiness/body_energy/yesterday"))
        // A lenient parse rolls this over into 2027; a strict one refuses it.
        assertFalse(isSupportedOpenVitalsRoute("daily_readiness/body_energy/2026-13-45"))
        assertFalse(isSupportedOpenVitalsRoute("daily_readiness/body_energy/"))
        // Missing argument entirely: `metric` is not `metric/<id>`.
        assertFalse(isSupportedOpenVitalsRoute("metric"))
        assertFalse(isSupportedOpenVitalsRoute("metric/"))
        // A drink-log route with nothing to log.
        assertFalse(isSupportedOpenVitalsRoute("manual_entry/hydration/log/"))
    }

    @Test
    fun `rejects nothing to open`() {
        assertNull(intentWithRoute(null).openVitalsRoute())
        assertNull(intentWithRoute("").openVitalsRoute())
        assertNull(intentWithRoute("settings").openVitalsRoute())
    }

    @Test
    fun `maps the raw route string the snapshot carries`() {
        assertEquals(
            "metric/RESTING_HEART_RATE",
            intentWithRoute("metric/RESTING_HEART_RATE").openVitalsRoute(),
        )
        assertEquals("dashboard", intentWithRoute("dashboard").openVitalsRoute())
        assertNull(intentWithRoute("nonsense").openVitalsRoute())
    }

    @Test
    fun `a readiness widget placed before the merge lands on Body Energy`() {
        // The Daily Readiness screen merged into Body Energy. The stored route cannot change, so it is retargeted to today.
        val expected = Screen.BodyEnergyDetails.createRoute(LocalDate.now().toString())

        assertEquals(expected, migratedOpenVitalsRoute("daily_readiness"))
        assertEquals(expected, intentWithRoute("daily_readiness").openVitalsRoute())
        // Every other route is carried through untouched.
        assertEquals("dashboard", migratedOpenVitalsRoute("dashboard"))
        assertEquals("metric/STEPS", migratedOpenVitalsRoute("metric/STEPS"))
    }

    private fun intentWithRoute(route: String?): Intent = mockk<Intent>().also { intent ->
        every { intent.getStringExtra(EXTRA_OPENVITALS_ROUTE) } returns route
    }
}
