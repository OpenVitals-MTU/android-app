package tech.mmarca.openvitals.features.homewidgets

import androidx.datastore.preferences.core.mutablePreferencesOf
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.features.cycle.CycleEstimateSummary
import tech.mmarca.openvitals.features.cycle.cycleEstimateSummary
import tech.mmarca.openvitals.navigation.Screen

/** The widget keeps ISO dates and computes the day when it draws, so a snapshot survives midnight. */
class HomeCycleWidgetStateTest {
    private val start = LocalDate.of(2026, 7, 1)
    private val estimate = CycleEstimate(
        earliestDate = LocalDate.of(2026, 7, 27),
        centralDate = LocalDate.of(2026, 7, 29),
        latestDate = LocalDate.of(2026, 7, 31),
        cycleCount = 4,
        variabilityDays = 2,
    )

    @Test
    fun `a snapshot round-trips through the widget state with its dates only`() {
        val snapshot = HomeCycleSnapshot(
            status = HomeCycleWidgetState.StatusAvailable,
            cycleStart = start,
            estimate = CycleEstimateResult.Available(estimate),
            loggedToday = true,
        )
        val preferences = mutablePreferencesOf()

        preferences.putCycleSnapshot(snapshot)
        val restored = preferences.toCycleSnapshot()

        assertEquals(HomeCycleWidgetState.StatusAvailable, restored.status)
        assertEquals(start, restored.cycleStart)
        assertTrue(restored.loggedToday)
        val window = (restored.estimate as CycleEstimateResult.Available).estimate
        assertEquals(estimate.earliestDate, window.earliestDate)
        assertEquals(estimate.centralDate, window.centralDate)
        assertEquals(estimate.latestDate, window.latestDate)
        // The history behind the estimate is not the widget's to keep.
        assertEquals(0, window.cycleCount)
    }

    @Test
    fun `the day is counted when the widget draws, not when it refreshed`() {
        assertEquals(1, cycleDayFor(start, start))
        assertEquals(10, cycleDayFor(start, start.plusDays(9)))
        assertNull(cycleDayFor(start, start.minusDays(1)))
        assertNull(cycleDayFor(start, start.plusDays(120)))
    }

    @Test
    fun `missing permission is told apart from no cycle`() {
        assertEquals(HomeCycleWidgetState.StatusPermission, HomeCycleSnapshot.from(null, loggedToday = false).status)

        val noCycle = HomeCycleSnapshot.from(CycleStatistics(), loggedToday = true)
        assertEquals(HomeCycleWidgetState.StatusNoCycle, noCycle.status)
        assertEquals(CycleEstimateResult.NeedsMoreHistory, noCycle.estimate)
        assertTrue(noCycle.loggedToday)

        val current = CycleStatistics(
            currentCycleDay = 3,
            cycles = listOf(RecordedCycle(startDate = start, endDate = null, flowDays = mapOf(start to 2))),
            estimate = CycleEstimateResult.IntervalsOutOfRange,
        )
        val available = HomeCycleSnapshot.from(current, loggedToday = false)
        assertEquals(HomeCycleWidgetState.StatusAvailable, available.status)
        assertEquals(start, available.cycleStart)
        assertEquals(CycleEstimateResult.IntervalsOutOfRange, available.estimate)
    }

    @Test
    fun `clearing leaves no data behind, only the hide flag`() {
        val preferences = mutablePreferencesOf(HomeCycleWidgetState.concealedKey to true)
        preferences.putCycleSnapshot(HomeCycleSnapshot(HomeCycleWidgetState.StatusAvailable, start, CycleEstimateResult.Available(estimate)))

        preferences.clearCycleSnapshot()

        assertEquals(setOf(HomeCycleWidgetState.concealedKey), preferences.asMap().keys)
        assertEquals(HomeCycleWidgetState.StatusUnavailable, preferences.toCycleSnapshot().status)
    }

    @Test
    fun `the estimate line follows where today sits in the window`() {
        val result = CycleEstimateResult.Available(estimate)

        assertEquals(CycleEstimateSummary.Range(estimate.earliestDate, estimate.latestDate), cycleEstimateSummary(result, LocalDate.of(2026, 7, 20)))
        assertEquals(CycleEstimateSummary.InProgress, cycleEstimateSummary(result, estimate.centralDate))
        assertEquals(CycleEstimateSummary.PastWindow(3), cycleEstimateSummary(result, estimate.latestDate.plusDays(3)))
        assertEquals(CycleEstimateSummary.NeedsHistory, cycleEstimateSummary(CycleEstimateResult.NeedsMoreHistory, start))
        assertEquals(CycleEstimateSummary.OutOfRange, cycleEstimateSummary(CycleEstimateResult.IntervalsOutOfRange, start))
    }

    @Test
    fun `the log link is the bare day-log route`() {
        assertEquals(Screen.CycleEntry.route, homeCycleWidgetEntryRoute())
    }
}
