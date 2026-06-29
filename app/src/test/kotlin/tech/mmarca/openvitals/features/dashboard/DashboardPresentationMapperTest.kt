package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.preferences.UnitSystem
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
    fun build_pendingWidget_marksLoading() {
        val data = DashboardData(date = LocalDate.now(), steps = 1_000)

        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = dailyGoals,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            pendingWidgets = setOf(DashboardWidgetId.SLEEP),
        )

        assertTrue(display.widgets[DashboardWidgetId.SLEEP]?.isLoading == true)
        assertEquals(false, display.widgets[DashboardWidgetId.STEPS]?.isLoading)
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
