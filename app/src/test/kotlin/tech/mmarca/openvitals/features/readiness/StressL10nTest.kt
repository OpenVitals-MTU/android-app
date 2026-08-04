package tech.mmarca.openvitals.features.readiness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressConfidence
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressLevel
import tech.mmarca.openvitals.domain.insights.StressItemTemplate
import tech.mmarca.openvitals.domain.insights.StressListItem

/**
 * The parts of the stress localizer that do not need a resource table: the
 * template→resource mapping (which must be total and collision-free) and the
 * NaN-as-absent argument handling the sentences depend on.
 */
class StressL10nTest {

    @Test
    fun `every template maps to a distinct string resource`() {
        val ids = StressItemTemplate.entries.associateWith { stressTemplateRes(it) }

        ids.forEach { (template, id) ->
            assertNotEquals("$template has no resource", 0, id)
        }
        // MINDFULNESS_LOGGED deliberately reuses the readiness catalog's own
        // sentence; everything else gets its own.
        val reused = ids.values.groupBy { it }.filterValues { it.size > 1 }
        assertTrue("templates share a resource: $reused", reused.isEmpty())
    }

    @Test
    fun `every level has a label and a detail`() {
        PhysiologicalStressLevel.entries.forEach { level ->
            assertNotEquals("$level has no label", 0, stressLevelLabelRes(level))
            assertNotEquals("$level has no detail", 0, stressLevelDetailRes(level))
        }
    }

    @Test
    fun `every confidence has a label`() {
        PhysiologicalStressConfidence.entries.forEach { confidence ->
            assertNotEquals("$confidence has no label", 0, stressConfidenceLabelRes(confidence))
        }
    }

    @Test
    fun `known confidence reasons map to their own sentences, unknown ones fall back`() {
        val known = listOf(
            "hrv_resting_hr_average_hr",
            "partial_hrv_or_heart_rate_context",
            "activity_may_influence",
            "single_signal",
        ).map(::stressConfidenceReasonRes)

        assertEquals(known.size, known.toSet().size)
        // `no_stress_signals` is a real token the domain writes and has no
        // sentence of its own; so is anything a future version invents.
        assertEquals(stressConfidenceReasonRes("anything else"), stressConfidenceReasonRes("no_stress_signals"))
        assertTrue(stressConfidenceReasonRes("no_stress_signals") !in known)
    }

    @Test
    fun `a coverage window with no timestamps covers the whole day`() {
        val item = StressListItem(
            template = StressItemTemplate.COVERAGE_HR_SAMPLES,
            args = listOf(12.0, Double.NaN, Double.NaN),
        )

        assertEquals(StressWindow.Day, stressWindow(item.args))
        assertEquals(12, item.args.intArg(0))
    }

    @Test
    fun `a coverage window with one instant reads as a point in time`() {
        val args = listOf(1.0, 1_700_000_000_000.0, 1_700_000_000_000.0)

        assertEquals(StressWindow.At(1_700_000_000_000L), stressWindow(args))
    }

    @Test
    fun `a coverage window with two instants reads as a range`() {
        val args = listOf(9.0, 1_700_000_000_000.0, 1_700_003_600_000.0)

        assertEquals(
            StressWindow.Range(1_700_000_000_000L, 1_700_003_600_000L),
            stressWindow(args),
        )
    }

    @Test
    fun `a missing window arg is treated the same as an absent one`() {
        assertEquals(StressWindow.Day, stressWindow(listOf(4.0)))
        assertEquals(StressWindow.Day, stressWindow(emptyList()))
    }

    @Test
    fun `an absent temperature reading is dropped rather than printed as zero`() {
        val bodyOnly = stressTemperature(listOf(37.4, Double.NaN))
        assertEquals(37.4, bodyOnly.bodyCelsius!!, 0.0001)
        assertNull(bodyOnly.skinDeltaCelsius)

        val skinOnly = stressTemperature(listOf(Double.NaN, 0.6))
        assertNull(skinOnly.bodyCelsius)
        assertEquals(0.6, skinOnly.skinDeltaCelsius!!, 0.0001)

        val neither = stressTemperature(emptyList())
        assertNull(neither.bodyCelsius)
        assertNull(neither.skinDeltaCelsius)
    }

    @Test
    fun `a NaN or missing integer arg reads as zero`() {
        assertEquals(0, listOf(Double.NaN).intArg(0))
        assertEquals(0, emptyList<Double>().intArg(0))
        assertEquals(7, listOf(6.6).intArg(0))
        assertEquals(-3, listOf(-2.5, -3.4).intArg(1))
    }
}
