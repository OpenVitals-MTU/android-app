package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardPresentationMapperTest {

    private val unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    private val dateTimeFormatterProvider = DateTimeFormatterProvider()
    private val dailyGoals = DashboardDailyGoals()

    @Test
    fun build_stepsWidget_usesCircleStyleAndProgress() {
        val data = DashboardData(date = LocalDate.now(), steps = 5_000)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val steps = display.widgets[DashboardWidgetId.STEPS]
        assertNotNull(steps)
        assertEquals(DashboardWidgetStyle.CIRCLE, steps?.style)
        assertNotNull(steps?.progress)
        assertTrue(steps?.progress?.fraction ?: 0f > 0f)
    }

    @Test
    fun build_caloriesOutWithoutData_hasNoValue() {
        val data = DashboardData(
            date = LocalDate.now(),
            caloriesKcalSource = CaloriesBurnedSource.NO_DATA,
        )

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val calories = display.widgets[DashboardWidgetId.CALORIES_OUT]
        assertNotNull(calories)
        assertEquals(false, calories?.hasValue)
    }

    @Test
    fun build_recentHistoryMetrics_reachTheirWidgetsAsHasRecentHistory() {
        val data = DashboardData(
            date = LocalDate.now(),
            recentHistoryMetrics = setOf(
                DashboardMetric.SLEEP,
                DashboardMetric.CYCLE,
                DashboardMetric.WEEKLY_CARDIO_LOAD,
            ),
        )

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        assertEquals(true, display.widgets[DashboardWidgetId.SLEEP]?.hasRecentHistory)
        assertEquals(true, display.widgets[DashboardWidgetId.CYCLE]?.hasRecentHistory)
        assertEquals(true, display.widgets[DashboardWidgetId.WEEKLY_CARDIO_LOAD]?.hasRecentHistory)
        assertEquals(true, display.widgets[DashboardWidgetId.CARDIO_LOAD]?.hasRecentHistory)
        assertEquals(false, display.widgets[DashboardWidgetId.HYDRATION]?.hasRecentHistory)
        // The empty-but-recently-used sleep tile must not sink.
        assertEquals(false, display.widgets[DashboardWidgetId.SLEEP]?.isDemotableEmptyTile())
    }

    @Test
    fun build_cycleWidget_usesMenstruationDaysWhenPresent() {
        val data = DashboardData(date = LocalDate.now(), menstruationPeriodDays = 5)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val cycle = display.widgets[DashboardWidgetId.CYCLE]?.cycle
        assertEquals(CycleWidgetDisplay.MenstruationDays(5), cycle)
    }

    @Test
    fun build_caffeineWidget_usesAdaptiveMassDisplayWithoutProgress() {
        val data = DashboardData(date = LocalDate.now(), caffeineGrams = 0.095)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val caffeine = display.widgets[DashboardWidgetId.CAFFEINE]
        assertNotNull(caffeine)
        assertEquals("95", caffeine?.value?.value)
        assertEquals("mg", caffeine?.value?.unit)
        assertNull(caffeine?.progress)
    }

    @Test
    fun build_caffeineWidget_headlinesActiveCaffeineWithConsumedSubtitle() {
        val data = DashboardData(
            date = LocalDate.now(),
            caffeineGrams = 0.095,
            activeCaffeineMg = 62.4,
        )

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val caffeine = display.widgets[DashboardWidgetId.CAFFEINE]
        assertEquals("62", caffeine?.value?.value)
        assertEquals("mg", caffeine?.value?.unit)
        assertEquals(95L, caffeine?.caffeineConsumedTodayMg)
    }

    @Test
    fun build_caffeineWidget_activeOnlyHeadlinesWithoutASubtitle() {
        // Morning carryover, nothing consumed yet: the active amount is the tile.
        val data = DashboardData(date = LocalDate.now(), activeCaffeineMg = 21.0)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val caffeine = display.widgets[DashboardWidgetId.CAFFEINE]
        assertEquals("21", caffeine?.value?.value)
        assertEquals("mg", caffeine?.value?.unit)
        assertNull(caffeine?.caffeineConsumedTodayMg)
        assertEquals(false, caffeine?.showsNoDataMessage())
    }

    @Test
    fun build_caffeineWidget_withNeitherFigureShowsTheEmptyMessage() {
        val display = DashboardPresentationMapper.build(
            data = DashboardData(date = LocalDate.now()),
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val caffeine = display.widgets[DashboardWidgetId.CAFFEINE]
        assertNotNull(caffeine)
        assertNull(caffeine?.value)
        assertNull(caffeine?.caffeineConsumedTodayMg)
        assertEquals(true, caffeine?.showsNoDataMessage())
    }

    @Test
    fun build_stepsRing_fillsAgainstTheUsersGoalNotTheDefault() {
        // The reported bug: goal set to 6,000, the ring filled to 8,000. Both are asserted.
        val data = DashboardData(date = LocalDate.now(), steps = 3_000)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = DashboardDailyGoals(steps = 6_000.0),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val steps = display.widgets[DashboardWidgetId.STEPS]
        assertEquals(0.5f, steps?.progress?.fraction ?: 0f, 1e-6f)
        assertEquals(unitFormatter.count(6_000), steps?.progress?.goalLabelValue?.value)
    }

    @Test
    fun build_sleepWidget_countsTimeAsleepNotTimeInBed() {
        val start = java.time.Instant.parse("2026-06-10T22:00:00Z")
        val sleep = tech.mmarca.openvitals.domain.model.SleepData(
            id = "s1",
            startTime = start,
            endTime = start.plusSeconds(8 * 3600),
            durationMs = 8 * 3600_000L,
            source = "watch",
            stages = listOf(
                tech.mmarca.openvitals.domain.model.SleepStage(
                    startTime = start,
                    endTime = start.plusSeconds(7 * 3600),
                    stageType = tech.mmarca.openvitals.domain.model.SleepStage.STAGE_LIGHT,
                ),
                tech.mmarca.openvitals.domain.model.SleepStage(
                    startTime = start.plusSeconds(7 * 3600),
                    endTime = start.plusSeconds(8 * 3600),
                    stageType = tech.mmarca.openvitals.domain.model.SleepStage.STAGE_AWAKE,
                ),
            ),
        )
        val data = DashboardData(date = LocalDate.now(), sleep = sleep)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val value = display.widgets[DashboardWidgetId.SLEEP]?.value?.value
        assertEquals(unitFormatter.duration(7 * 3600_000L), value)
    }

    @Test
    fun showsNoDataMessage_sinksEmptyTilesButNotLoadingOnes() {
        val data = DashboardData(
            date = LocalDate.now(),
            caloriesKcalSource = CaloriesBurnedSource.NO_DATA,
        )
        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            loadingWidgets = setOf(DashboardWidgetId.HRV),
        )

        assertEquals(true, display.widgets[DashboardWidgetId.CALORIES_OUT]?.showsNoDataMessage())
        assertEquals(false, display.widgets[DashboardWidgetId.STEPS]?.showsNoDataMessage())
        assertEquals(false, display.widgets[DashboardWidgetId.HRV]?.showsNoDataMessage())
    }

    @Test
    fun build_pairedWatch_materialisesTheWatchTile() {
        val data = DashboardData(date = LocalDate.now(), steps = 8_000)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            watch = WatchWidgetDisplay(
                deviceId = "watch-1",
                name = "v\u00edvoactive 5",
                batteryPercent = 62,
                lastSyncedAt = Instant.parse("2026-08-12T10:26:00Z"),
            ),
        )

        val widget = requireNotNull(display.widgets[DashboardWidgetId.WATCH])
        assertEquals("v\u00edvoactive 5", widget.watch?.name)
        // It has content, so it must not sort to the back of the carousel.
        assertEquals(false, widget.showsNoDataMessage())
    }

    @Test
    fun build_fullySupportedDay_mapsBothRingsAndEveryTile() {
        val data = DashboardData(date = LocalDate.now(), steps = 8_000)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        // Every known widget materialises except WORKOUT (its own section) and WATCH (no watch passed).
        assertEquals(DashboardWidgetStyle.CIRCLE, display.widgets[DashboardWidgetId.STEPS]?.style)
        assertEquals(
            DashboardWidgetStyle.CIRCLE,
            display.widgets[DashboardWidgetId.WEEKLY_CARDIO_LOAD]?.style,
        )
        assertEquals(
            DashboardWidgetId.entries - DashboardWidgetId.WORKOUT - DashboardWidgetId.WATCH,
            display.widgets.keys.toList(),
        )
        // Including the ones a narrower mapper used to drop entirely.
        listOf(
            DashboardWidgetId.BLOOD_GLUCOSE,
            DashboardWidgetId.SKIN_TEMPERATURE,
            DashboardWidgetId.BMR,
            DashboardWidgetId.BONE_MASS,
            DashboardWidgetId.BODY_WATER_MASS,
        ).forEach { id -> assertNotNull(display.widgets[id]) }
    }

    @Test
    fun build_emptyDay_stillRendersTheRingsAndTheEmptyTiles() {
        val display = DashboardPresentationMapper.build(
            data = DashboardData(date = LocalDate.now()),
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        // No readings: the tiles are still there, empty, and the hero rings never disappear.
        assertEquals(
            DashboardWidgetId.entries - DashboardWidgetId.WORKOUT - DashboardWidgetId.WATCH,
            display.widgets.keys.toList(),
        )
        assertNotNull(display.widgets[DashboardWidgetId.STEPS])
        val cardio = display.widgets[DashboardWidgetId.WEEKLY_CARDIO_LOAD]
        assertNotNull(cardio)
        assertNull(cardio?.weeklyCardioLoad)
        assertEquals(true, cardio?.showsNoDataMessage())
        // An empty tile carries no progress to fill against.
        assertNull(display.widgets[DashboardWidgetId.SPO2]?.progress)
        assertEquals(true, display.widgets[DashboardWidgetId.SPO2]?.showsNoDataMessage())
    }

    @Test
    fun build_requiredMetrics_showAZeroReadingRatherThanANoDataMessage() {
        // A counter the device always answers reads zero; "No data" for zero steps would look like a broken permission.
        val display = DashboardPresentationMapper.build(
            data = DashboardData(
                date = LocalDate.now(),
                steps = 0,
                caloriesKcalSource = CaloriesBurnedSource.NO_DATA,
            ),
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        val steps = display.widgets[DashboardWidgetId.STEPS]
        assertEquals("0", steps?.value?.value)
        assertEquals(true, steps?.hasValue)
        assertEquals(false, steps?.showsNoDataMessage())

        val calories = display.widgets[DashboardWidgetId.CALORIES_OUT]
        assertEquals(false, calories?.hasValue)
        assertEquals(true, calories?.showsNoDataMessage())
    }

    // Body Energy tile. The detail screen has its own mapper.

    @Test
    fun build_bodyEnergyWidget_rendersCurrentScoreAndStartChargedDrainedSubtitle() {
        val data = DashboardData(
            date = LocalDate.of(2026, 1, 2),
            bodyEnergyTimeline = bodyEnergyTimeline(),
        )

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            bodyEnergySetupCompleted = true,
        )

        val bodyEnergy = display.widgets[DashboardWidgetId.BODY_ENERGY]
        assertNotNull(bodyEnergy)
        assertEquals("74", bodyEnergy?.value?.value)
        assertEquals(
            BodyEnergyTileSubtitle(startScore = 60, charged = 30, drained = 16),
            bodyEnergy?.bodyEnergySubtitle,
        )
        assertEquals(false, bodyEnergy?.isNotSetUp)
        assertEquals(false, bodyEnergy?.showsNoDataMessage())
    }

    @Test
    fun build_bodyEnergyWidget_isNotSetUpUntilCalibrationCompletes() {
        // The tile is gated on the calibration flag, not the timeline's presence.
        val data = DashboardData(
            date = LocalDate.of(2026, 1, 2),
            bodyEnergyTimeline = bodyEnergyTimeline(),
        )

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            bodyEnergySetupCompleted = false,
        )

        val bodyEnergy = display.widgets[DashboardWidgetId.BODY_ENERGY]
        assertNotNull(bodyEnergy)
        assertNull(bodyEnergy?.value)
        assertNull(bodyEnergy?.bodyEnergySubtitle)
        assertEquals(true, bodyEnergy?.isNotSetUp)
        assertEquals(true, bodyEnergy?.showsNoDataMessage())
    }

    @Test
    fun build_bodyEnergyWidget_showsNoDataWhenTimelineIsAbsent() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 2))

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            bodyEnergySetupCompleted = true,
        )

        val bodyEnergy = display.widgets[DashboardWidgetId.BODY_ENERGY]
        assertNotNull(bodyEnergy)
        assertNull(bodyEnergy?.value)
        assertNull(bodyEnergy?.bodyEnergySubtitle)
        assertEquals(false, bodyEnergy?.hasValue)
        assertEquals(true, bodyEnergy?.showsNoDataMessage())
    }

    private fun bodyEnergyTimeline() = BodyEnergyTimeline(
        date = LocalDate.of(2026, 1, 2),
        startScore = 60,
        currentScore = 74,
        charged = 30,
        drained = 16,
        points = emptyList(),
        confidence = BodyEnergyConfidence.MEDIUM,
        confidenceReason = "",
    )

    // Device-support gating. `supportedMetrics` is what the provider can serve at all;
    // null means "not established" and gates nothing.

    @Test
    fun build_supportedMetricWithNoReading_stillGetsAnEmptyTile() {
        val display = build(
            DashboardData(
                date = LocalDate.now(),
                supportedMetrics = setOf(DashboardMetric.SPO2),
            ),
        )

        val spo2 = display.widgets[DashboardWidgetId.SPO2]
        assertNotNull(spo2)
        assertNull(spo2?.value)
        assertNull(spo2?.progress)
        assertEquals(true, spo2?.showsNoDataMessage())
    }

    @Test
    fun build_supportedMetricWithAReading_rendersItsValueNotAMessage() {
        val display = build(
            DashboardData(
                date = LocalDate.now(),
                latestSpO2Percent = 97.0,
                supportedMetrics = setOf(DashboardMetric.SPO2),
            ),
        )

        val spo2 = display.widgets[DashboardWidgetId.SPO2]
        assertNotNull(spo2?.value)
        assertEquals(false, spo2?.showsNoDataMessage())
    }

    @Test
    fun build_unsupportedMetric_getsNoTileAtAll() {
        // Everything except blood oxygen.
        val display = build(
            DashboardData(
                date = LocalDate.now(),
                supportedMetrics = DashboardMetric.entries.toSet() - DashboardMetric.SPO2,
            ),
        )

        assertNull(display.widgets[DashboardWidgetId.SPO2])
        // …but its supported neighbours are still there, empty.
        val vo2Max = display.widgets[DashboardWidgetId.VO2_MAX]
        assertNotNull(vo2Max)
        assertEquals(true, vo2Max?.showsNoDataMessage())
        assertEquals(emptySet<DashboardWidgetId>(), display.unsupportedIds)
    }

    @Test
    fun build_deviceSupportsNothing_producesNoTilesAtAll() {
        // The hero rings are ordinary widgets, so a device that serves nothing renders nothing.
        val display = build(
            DashboardData(date = LocalDate.now(), supportedMetrics = emptySet()),
        )

        assertEquals(emptyMap<DashboardWidgetId, DashboardWidgetDisplayModel>(), display.widgets)
        assertEquals(emptySet<DashboardWidgetId>(), display.unsupportedIds)
    }

    @Test
    fun build_bodyEnergy_followsHeartRateSupport() {
        // Body Energy is derived, so it follows the reading it is computed from.
        val withHeartRate = build(
            DashboardData(
                date = LocalDate.now(),
                supportedMetrics = setOf(DashboardMetric.AVG_HEART_RATE),
            ),
        )
        val withoutHeartRate = build(
            DashboardData(date = LocalDate.now(), supportedMetrics = setOf(DashboardMetric.STEPS)),
        )

        assertNotNull(withHeartRate.widgets[DashboardWidgetId.BODY_ENERGY])
        assertNull(withoutHeartRate.widgets[DashboardWidgetId.BODY_ENERGY])
    }

    @Test
    fun build_includeUnsupported_materialisesMetricsAbsentFromSupportedMetrics() {
        val display = build(
            DashboardData(
                date = LocalDate.now(),
                supportedMetrics = DashboardMetric.entries.toSet() - DashboardMetric.SPO2,
            ),
            includeUnsupported = true,
        )

        assertNotNull(display.widgets[DashboardWidgetId.SPO2])
        assertEquals(setOf(DashboardWidgetId.SPO2), display.unsupportedIds)
        // An unsupported tile is empty, like any other metric with no reading.
        assertNull(display.widgets[DashboardWidgetId.SPO2]?.value)
    }

    @Test
    fun build_includeUnsupported_materialisesEveryMetricWhenTheDeviceSupportsNothing() {
        val display = build(
            DashboardData(date = LocalDate.now(), supportedMetrics = emptySet()),
            includeUnsupported = true,
        )

        assertEquals(
            DashboardWidgetId.entries - DashboardWidgetId.WORKOUT - DashboardWidgetId.WATCH,
            display.widgets.keys.toList(),
        )
        assertEquals(display.widgets.keys, display.unsupportedIds)
    }

    @Test
    fun build_includeUnsupported_defaultsToFalseSoUnsupportedMetricsStayDropped() {
        val display = build(
            DashboardData(
                date = LocalDate.now(),
                supportedMetrics = DashboardMetric.entries.toSet() - DashboardMetric.SPO2,
            ),
        )

        assertNull(display.widgets[DashboardWidgetId.SPO2])
        assertEquals(emptySet<DashboardWidgetId>(), display.unsupportedIds)
    }

    private fun build(
        data: DashboardData,
        includeUnsupported: Boolean = false,
    ): DashboardDisplayState =
        DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            includeUnsupported = includeUnsupported,
        )

    @Test
    fun build_excludesWorkoutWidget() {
        val data = DashboardData(date = LocalDate.now())

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        assertNull(display.widgets[DashboardWidgetId.WORKOUT])
    }
}
