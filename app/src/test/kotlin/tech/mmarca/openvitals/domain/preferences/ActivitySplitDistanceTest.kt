package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitySplitDistanceTest {

    @Test
    fun `normalize falls back to the default for non-finite or non-positive values`() {
        assertEquals(1000.0, ActivitySplitDistance.normalize(0.0), 0.0)
        assertEquals(1000.0, ActivitySplitDistance.normalize(-5.0), 0.0)
        assertEquals(1000.0, ActivitySplitDistance.normalize(Double.NaN), 0.0)
        assertEquals(1000.0, ActivitySplitDistance.normalize(Double.POSITIVE_INFINITY), 0.0)
    }

    @Test
    fun `normalize clamps to the allowed range`() {
        assertEquals(100.0, ActivitySplitDistance.normalize(1.0), 0.0)
        assertEquals(50000.0, ActivitySplitDistance.normalize(1e9), 0.0)
        assertEquals(1609.344, ActivitySplitDistance.normalize(1609.344), 0.0)
    }

    @Test
    fun `imperial presets are exact mile fractions, not rounded meters`() {
        // A user who picks "1 mi" and switches to metric sees 1.609 km of splits, not a rounded 1600 m.
        assertEquals(
            listOf(0.25 * 1609.344, 0.5 * 1609.344, 1609.344, 5 * 1609.344),
            ActivitySplitDistance.imperialPresetMeters,
        )
    }

    @Test
    fun `nearestPreset highlights the closest chip after a unit-system switch`() {
        // A stored metric 1000 m has no exact imperial preset; the nearest is 0.5 mi.
        assertEquals(
            0.5 * 1609.344,
            ActivitySplitDistance.nearestPreset(1000.0, ActivitySplitDistance.imperialPresetMeters),
            0.0,
        )
        assertEquals(
            1000.0,
            ActivitySplitDistance.nearestPreset(1200.0, ActivitySplitDistance.metricPresetMeters),
            0.0,
        )
        assertEquals(
            2000.0,
            ActivitySplitDistance.nearestPreset(1609.344, UnitSystem.METRIC),
            0.0,
        )
    }
}
