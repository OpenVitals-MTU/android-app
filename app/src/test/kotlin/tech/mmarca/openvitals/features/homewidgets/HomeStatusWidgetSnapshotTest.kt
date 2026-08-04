package tech.mmarca.openvitals.features.homewidgets

import java.time.Instant
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.DailyReadinessGoalInputs
import tech.mmarca.openvitals.domain.insights.calculateDailyReadiness
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.navigation.Screen

/**
 * The three shared status widgets — Daily Readiness, Body Energy and Today.
 *
 * What they show is the whole product surface of a widget: there is no screen
 * behind it, so the snapshot IS the feature.
 */
class HomeStatusWidgetSnapshotTest {
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

    // --- Daily Readiness -----------------------------------------------------

    @Test
    fun `daily readiness reports the score, status and recommendation`() {
        val data = dataWithReadiness()
        val insight = calculateDailyReadiness(data)

        val snapshot = buildDailyReadinessSnapshot(context, insight, date)

        assertEquals(context.getString(R.string.screen_daily_readiness), snapshot.title)
        assertEquals(insight.score.toString(), snapshot.value)
        assertEquals("", snapshot.unit)
        assertEquals(insight.statusTitle, snapshot.subtitle)
        // The readiness verdict lives on the Body Energy screen since the merge.
        assertEquals("daily_readiness/body_energy/2026-07-10", snapshot.route)
        assertEquals(1, snapshot.rows.size)
        assertEquals(
            context.getString(R.string.dashboard_readiness_recommended),
            snapshot.rows.single().label,
        )
        assertEquals(insight.recommendation, snapshot.rows.single().value)
    }

    @Test
    fun `daily readiness falls back to dashes when no signal is loaded`() {
        // Nothing loaded → availableSignals == 0 → ReadinessState.UNKNOWN.
        val insight = calculateDailyReadiness(DashboardData(date = date))

        val snapshot = buildDailyReadinessSnapshot(context, insight, date)

        assertEquals("--", snapshot.value)
        assertEquals(
            context.getString(R.string.home_metric_widget_open_for_details),
            snapshot.subtitle,
        )
        // Still routes into the app, where the user can see why it is empty.
        assertEquals("daily_readiness/body_energy/2026-07-10", snapshot.route)
        assertEquals(emptyList<HomeMetricWidgetRow>(), snapshot.rows)
    }

    @Test
    fun `daily readiness falls back to dashes when nothing loaded at all`() {
        val snapshot = buildDailyReadinessSnapshot(context, insight = null, date = date)

        assertEquals("--", snapshot.value)
        assertEquals(emptyList<HomeMetricWidgetRow>(), snapshot.rows)
    }

    @Test
    fun `daily readiness honours the caller's goals`() {
        val data = DashboardData(
            date = date,
            hydrationLiters = 0.2,
            loadedMetrics = setOf(DashboardMetric.HYDRATION),
        )

        // A 4 L goal leaves 0.2 L far behind; a 0.25 L goal does not.
        val behind = buildDailyReadinessSnapshot(
            context,
            calculateDailyReadiness(data, DailyReadinessGoalInputs(hydrationLitersGoal = 4.0)),
            date,
        )
        val met = buildDailyReadinessSnapshot(
            context,
            calculateDailyReadiness(data, DailyReadinessGoalInputs(hydrationLitersGoal = 0.25)),
            date,
        )

        assertTrue(
            "${behind.value} should be below ${met.value}",
            behind.value.toInt() < met.value.toInt(),
        )
    }

    // --- Body Energy ---------------------------------------------------------

    @Test
    fun `body energy reports the score with start, charged and drained rows`() {
        val snapshot = buildBodyEnergySnapshot(context, bodyEnergyTimeline(currentScore = 88), date)

        assertEquals(context.getString(R.string.screen_body_energy), snapshot.title)
        assertEquals("88", snapshot.value)
        assertEquals(context.getString(R.string.home_widget_body_energy_charged), snapshot.subtitle)
        assertEquals("daily_readiness/body_energy/2026-07-10", snapshot.route)
        assertEquals(
            listOf(
                context.getString(R.string.body_energy_timeline_start) to "70",
                context.getString(R.string.body_energy_timeline_charged) to "+30",
                context.getString(R.string.body_energy_timeline_drained) to "-12",
            ),
            snapshot.rows.map { row -> row.label to row.value },
        )
    }

    @Test
    fun `body energy maps every status threshold`() {
        fun statusFor(score: Int): String =
            buildBodyEnergySnapshot(context, bodyEnergyTimeline(currentScore = score), date).subtitle

        assertEquals(context.getString(R.string.home_widget_body_energy_charged), statusFor(80))
        assertEquals(context.getString(R.string.home_widget_body_energy_steady), statusFor(79))
        assertEquals(context.getString(R.string.home_widget_body_energy_steady), statusFor(60))
        assertEquals(context.getString(R.string.home_widget_body_energy_limited), statusFor(59))
        assertEquals(context.getString(R.string.home_widget_body_energy_limited), statusFor(40))
        assertEquals(context.getString(R.string.home_widget_body_energy_low), statusFor(39))
        assertEquals(context.getString(R.string.home_widget_body_energy_low), statusFor(0))
    }

    @Test
    fun `body energy falls back to dashes with no rows when the timeline is absent`() {
        val snapshot = buildBodyEnergySnapshot(context, timeline = null, date = date)

        assertEquals("--", snapshot.value)
        assertEquals(
            context.getString(R.string.home_metric_widget_open_for_details),
            snapshot.subtitle,
        )
        assertEquals("daily_readiness/body_energy/2026-07-10", snapshot.route)
        assertEquals(emptyList<HomeMetricWidgetRow>(), snapshot.rows)
    }

    // --- Today ---------------------------------------------------------------

    @Test
    fun `today lists the rows in order, values joined with their unit`() {
        val data = dataWithReadiness().copy(
            steps = 8_432L,
            distanceMeters = 6_200.0,
            hydrationLiters = 1.5,
            hrvRmssdMs = 42.5,
            sleep = SleepData(
                id = "sleep-1",
                startTime = Instant.parse("2026-07-09T23:00:00Z"),
                endTime = Instant.parse("2026-07-10T06:30:00Z"),
                durationMs = 27_000_000L,
                source = "test",
            ),
            weeklyCardioLoad = CardioLoad,
        )

        val snapshot = buildTodayVitalsSnapshot(
            context = context,
            data = data,
            unitFormatter = unitFormatter(),
            readinessInsight = calculateDailyReadiness(data),
            bodyEnergyTimeline = bodyEnergyTimeline(currentScore = 64),
        )

        assertEquals(context.getString(R.string.home_widget_today_title), snapshot.title)
        assertEquals("", snapshot.value)
        assertEquals(Screen.Dashboard.route, snapshot.route)
        assertEquals(
            listOf(
                context.getString(R.string.screen_daily_readiness),
                context.getString(R.string.screen_body_energy),
                context.getString(R.string.metric_sleep),
                context.getString(R.string.metric_steps),
                context.getString(R.string.metric_distance),
                context.getString(R.string.metric_resting_heart_rate),
                context.getString(R.string.home_widget_hrv_short),
                context.getString(R.string.metric_weekly_cardio_load),
                context.getString(R.string.metric_hydration),
            ),
            snapshot.rows.map(HomeMetricWidgetRow::label),
        )

        val rows = snapshot.rows.associateBy(HomeMetricWidgetRow::label)
        fun row(labelRes: Int): HomeMetricWidgetRow = rows.getValue(context.getString(labelRes))

        assertEquals("64", row(R.string.screen_body_energy).value)
        assertEquals("+30 / -12", row(R.string.screen_body_energy).subtitle)
        assertEquals("7h 30m", row(R.string.metric_sleep).value)
        assertEquals("8,432", row(R.string.metric_steps).value)
        assertEquals("6.2 km", row(R.string.metric_distance).value)
        assertEquals("52 bpm", row(R.string.metric_resting_heart_rate).value)
        assertEquals("42.5 ms", row(R.string.home_widget_hrv_short).value)
        assertEquals("1.50 L", row(R.string.metric_hydration).value)
        // Weekly cardio keeps its own subtitle; the rest drop the "Today" one.
        assertEquals(
            context.getString(R.string.dashboard_weekly_cardio_load_progress, 180, 300),
            row(R.string.metric_weekly_cardio_load).value,
        )
        assertEquals(
            context.getString(R.string.dashboard_cardio_load_percent, 60),
            row(R.string.metric_weekly_cardio_load).subtitle,
        )
        assertEquals("", row(R.string.metric_steps).subtitle)
    }

    @Test
    fun `today drops the readiness row and shows no-data rows when empty`() {
        val snapshot = buildTodayVitalsSnapshot(
            context = context,
            data = DashboardData(date = date),
            unitFormatter = unitFormatter(),
            readinessInsight = calculateDailyReadiness(DashboardData(date = date)),
            bodyEnergyTimeline = null,
        )

        // Readiness is UNKNOWN with no signals: the row is omitted entirely.
        assertFalse(
            snapshot.rows.any { row -> row.label == context.getString(R.string.screen_daily_readiness) },
        )
        val bodyEnergy = snapshot.rows.first()
        assertEquals(context.getString(R.string.screen_body_energy), bodyEnergy.label)
        assertEquals("--", bodyEnergy.value)
        assertEquals(context.getString(R.string.no_data), bodyEnergy.subtitle)

        val sleep = snapshot.rows[1]
        assertEquals("--", sleep.value)
        assertEquals(context.getString(R.string.no_data), sleep.subtitle)

        // Steps reads a real zero, so it is never "no data"; hydration is not
        // stored as a zero reading, so it is.
        val rows = snapshot.rows.associateBy(HomeMetricWidgetRow::label)
        assertEquals("0", rows.getValue(context.getString(R.string.metric_steps)).value)
        assertEquals("--", rows.getValue(context.getString(R.string.metric_hydration)).value)
    }

    /**
     * Readiness reports UNKNOWN until at least one signal is loaded; a resting
     * heart rate with a baseline is the cheapest way to get a real score.
     */
    private fun dataWithReadiness(): DashboardData =
        DashboardData(
            date = date,
            restingHeartRateBpm = 52L,
            restingHeartRateBaselineBpm = 54L,
            loadedMetrics = setOf(DashboardMetric.RESTING_HEART_RATE),
        )

    private companion object {
        val CardioLoad = DashboardWeeklyCardioLoad(
            currentScore = 180,
            targetScore = 300,
            todayScore = 20,
            confidence = CardioLoadConfidence.HIGH,
            targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
        )
    }
}
