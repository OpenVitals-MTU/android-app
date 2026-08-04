package tech.mmarca.openvitals.features.activity

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * The split distance as the settings chip and the splits-card header print it.
 *
 * Flutter counterpart: the "imperial units re-express the derived header in
 * miles" case in test/features/activity/activity_detail_splits_test.dart.
 */
class ActivitySplitDistanceLabelTest {

    @Test fun `metric presets print as the round numbers the user picked`() {
        val formatter = formatter(UnitSystem.METRIC)

        // Not UnitFormatter.distance, which would print "1.0 km": a chosen
        // setting is a round number, not a measurement.
        assertEquals("1 km", splitDistanceLabel(formatter, 1_000.0))
        assertEquals("0.5 km", splitDistanceLabel(formatter, 500.0))
        assertEquals("0.25 km", splitDistanceLabel(formatter, 250.0))
        assertEquals("5 km", splitDistanceLabel(formatter, 5_000.0))
    }

    @Test fun `imperial units re-express the derived header in miles`() {
        val formatter = formatter(UnitSystem.IMPERIAL)

        // Storage stays metric; only the label converts. The presets are exact
        // mile fractions, so they must come back out as round mile numbers.
        assertEquals("1 mi", splitDistanceLabel(formatter, 1_609.344))
        assertEquals("0.5 mi", splitDistanceLabel(formatter, 1_609.344 / 2))
        assertEquals("0.25 mi", splitDistanceLabel(formatter, 1_609.344 / 4))
    }

    @Test fun `every imperial preset labels as a round mile fraction`() {
        val formatter = formatter(UnitSystem.IMPERIAL)

        assertEquals(
            listOf("0.25 mi", "0.5 mi", "1 mi", "5 mi"),
            ActivitySplitDistance.presetsFor(UnitSystem.IMPERIAL)
                .map { splitDistanceLabel(formatter, it) },
        )
    }

    private fun formatter(unitSystem: UnitSystem): UnitFormatter =
        UnitFormatter(
            unitSystemProvider = { unitSystem },
            localeProvider = { Locale.US },
        )
}
