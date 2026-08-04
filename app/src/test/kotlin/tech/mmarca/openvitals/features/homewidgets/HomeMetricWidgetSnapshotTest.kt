package tech.mmarca.openvitals.features.homewidgets

import androidx.datastore.preferences.core.mutablePreferencesOf
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.navigation.Screen

/** The per-metric tile widget: one reading, its unit, and where a tap lands. */
class HomeMetricWidgetSnapshotTest {
    private val context = stringResourceContext()
    private val date = LocalDate.of(2026, 7, 10)

    @Before
    fun setUp() {
        mockUriCodec()
    }

    @After
    fun tearDown() {
        unmockUriCodec()
    }

    @Test
    fun `formats a metric with its unit and routes to the metric screen`() {
        val snapshot = buildMetricWidgetSnapshot(
            context = context,
            metricId = DashboardWidgetId.DISTANCE,
            data = DashboardData(date = date, distanceMeters = 6_200.0),
            unitFormatter = unitFormatter(),
        )

        assertEquals(context.getString(R.string.metric_distance), snapshot.title)
        assertEquals("6.2", snapshot.value)
        assertEquals("km", snapshot.unit)
        assertEquals(context.getString(R.string.period_today), snapshot.subtitle)
        assertEquals("metric/DISTANCE", snapshot.route)
    }

    @Test
    fun `body energy routes to its dated detail screen, not the metric screen`() {
        assertEquals(
            "daily_readiness/body_energy/2026-07-10",
            homeMetricWidgetRoute(DashboardWidgetId.BODY_ENERGY, date),
        )

        val snapshot = buildBodyEnergyMetricSnapshot(
            context = context,
            timeline = bodyEnergyTimeline(currentScore = 64),
            title = context.getString(R.string.metric_body_energy),
            route = homeMetricWidgetRoute(DashboardWidgetId.BODY_ENERGY, date),
        )

        assertEquals("64", snapshot.value)
        assertEquals("daily_readiness/body_energy/2026-07-10", snapshot.route)
        assertEquals(
            listOf("70", "+30", "-12"),
            snapshot.rows.map(HomeMetricWidgetRow::value),
        )
    }

    @Test
    fun `a body energy tile with no timeline reports dashes`() {
        val snapshot = buildBodyEnergyMetricSnapshot(
            context = context,
            timeline = null,
            title = context.getString(R.string.metric_body_energy),
            route = homeMetricWidgetRoute(DashboardWidgetId.BODY_ENERGY, date),
        )

        assertEquals("--", snapshot.value)
        assertEquals(
            context.getString(R.string.home_metric_widget_open_for_details),
            snapshot.subtitle,
        )
        assertEquals(emptyList<HomeMetricWidgetRow>(), snapshot.rows)
    }

    @Test
    fun `reports dashes and no data for an absent reading`() {
        val snapshot = buildMetricWidgetSnapshot(
            context = context,
            metricId = DashboardWidgetId.HRV,
            data = DashboardData(date = date),
            unitFormatter = unitFormatter(),
        )

        assertEquals("--", snapshot.value)
        assertEquals("", snapshot.unit)
        assertEquals(context.getString(R.string.no_data), snapshot.subtitle)
        assertEquals("metric/HRV", snapshot.route)
    }

    @Test
    fun `a missing permission wins over the reading`() {
        val snapshot = buildMetricWidgetSnapshot(
            context = context,
            metricId = DashboardWidgetId.STEPS,
            data = DashboardData(
                date = date,
                steps = 8_432L,
                missingPermissions = setOf("android.permission.health.READ_STEPS"),
            ),
            unitFormatter = unitFormatter(),
        )

        assertEquals("--", snapshot.value)
        assertEquals(
            context.getString(R.string.home_metric_widget_permission_needed),
            snapshot.subtitle,
        )
        assertEquals("metric/STEPS", snapshot.route)
    }

    @Test
    fun `every catalog metric has a title, a route and a no-data snapshot`() {
        val empty = DashboardData(date = date)

        homeMetricWidgetCatalog().forEach { metricId ->
            val snapshot = buildMetricWidgetSnapshot(
                context = context,
                metricId = metricId,
                data = empty,
                unitFormatter = unitFormatter(),
                route = homeMetricWidgetRoute(metricId, date),
            )

            assertTrue(metricId.name, snapshot.title.isNotEmpty())
            assertTrue(metricId.name, snapshot.route.isNotEmpty())
            assertTrue(metricId.name, snapshot.value.isNotEmpty())
        }
    }

    @Test
    fun `the catalog offers a distinct title for every metric it lists`() {
        val catalog = homeMetricWidgetCatalog()

        assertEquals(catalog.toSet().size, catalog.size)
        assertTrue(DashboardWidgetId.BODY_ENERGY in catalog)
        assertTrue(DashboardWidgetId.WEEKLY_CARDIO_LOAD in catalog)
        catalog.forEach { metricId ->
            assertNotEquals(0, metricId.homeMetricTitleRes())
        }
    }

    @Test
    fun `caffeine has no detail screen, so its tile opens the dashboard`() {
        assertEquals(
            Screen.Dashboard.route,
            homeMetricWidgetRoute(DashboardWidgetId.CAFFEINE, date),
        )
    }

    // --- The Glance state the snapshot is written into -----------------------

    @Test
    fun `writing a snapshot caps the rows the widget can draw`() {
        val snapshot = HomeMetricWidgetSnapshot(
            title = "Today",
            value = "",
            unit = "",
            subtitle = "",
            route = Screen.Dashboard.route,
            rows = (0 until MaxHomeWidgetRows + 5).map { index ->
                HomeMetricWidgetRow(label = "label$index", value = "value$index")
            },
        )

        val preferences = mutablePreferencesOf()
        preferences.putHomeWidgetSnapshot(metricId = null, snapshot = snapshot)

        assertEquals(MaxHomeWidgetRows, preferences[HomeMetricWidgetState.rowCountKey] ?: 0)
        val restored = preferences.toWidgetSnapshot(context)
        assertEquals(MaxHomeWidgetRows, restored?.rows?.size ?: 0)
        assertEquals("label0", restored?.rows?.first()?.label)
        assertEquals("label${MaxHomeWidgetRows - 1}", restored?.rows?.last()?.label)
    }

    @Test
    fun `writing a snapshot round-trips the tile through the widget state`() {
        val snapshot = HomeMetricWidgetSnapshot(
            title = "Distance",
            value = "6.2",
            unit = "km",
            subtitle = "Today",
            route = "metric/DISTANCE",
            rows = listOf(HomeMetricWidgetRow(label = "Steps", value = "8,432", subtitle = "Today")),
        )

        val preferences = mutablePreferencesOf()
        preferences.putHomeWidgetSnapshot(metricId = "DISTANCE", snapshot = snapshot)

        assertEquals("DISTANCE", preferences[HomeMetricWidgetState.metricIdKey])
        assertEquals(snapshot, preferences.toWidgetSnapshot(context))
    }

    @Test
    fun `writing a snapshot round-trips the plot series through the widget state`() {
        val snapshot = HomeMetricWidgetSnapshot(
            title = "Body Energy",
            value = "74",
            unit = "",
            subtitle = "Steady",
            route = "daily_readiness/body_energy/2026-07-10",
            series = listOf(70, 68, 72, 75, 74),
        )

        val preferences = mutablePreferencesOf()
        preferences.putHomeWidgetSnapshot(metricId = "BODY_ENERGY", snapshot = snapshot)

        assertEquals("70,68,72,75,74", preferences[HomeMetricWidgetState.seriesKey])
        assertEquals(snapshot, preferences.toWidgetSnapshot(context))
    }

    @Test
    fun `a snapshot with no series clears the one a previous write left`() {
        val preferences = mutablePreferencesOf(HomeMetricWidgetState.seriesKey to "70,68,72")

        preferences.putHomeWidgetSnapshot(
            metricId = "STEPS",
            snapshot = HomeMetricWidgetSnapshot(
                title = "Steps",
                value = "8,432",
                unit = "",
                subtitle = "Today",
                route = "metric/STEPS",
            ),
        )

        assertNull(preferences[HomeMetricWidgetState.seriesKey])
        assertEquals(emptyList<Int>(), preferences.toWidgetSnapshot(context)?.series)
    }

    @Test
    fun `an unparseable series value is dropped, not defaulted to zero`() {
        // A zero is a legitimate score, so substituting one would draw a cliff
        // to the floor the day never had.
        val preferences = mutablePreferencesOf(
            HomeMetricWidgetState.titleKey to "Body Energy",
            HomeMetricWidgetState.seriesKey to "70,not-a-score, 72 ,",
        )

        assertEquals(listOf(70, 72), preferences.toWidgetSnapshot(context)?.series)
    }

    @Test
    fun `an unconfigured instance drops the stored metric id`() {
        val preferences = mutablePreferencesOf(HomeMetricWidgetState.metricIdKey to "STEPS")

        preferences.putHomeWidgetSnapshot(
            metricId = null,
            snapshot = HomeMetricWidgetSnapshot(
                title = "Today",
                value = "--",
                unit = "",
                subtitle = "",
                route = Screen.Dashboard.route,
            ),
        )

        assertNull(preferences[HomeMetricWidgetState.metricIdKey])
    }
}
