package tech.mmarca.openvitals.features.activity

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.ActivitySplitDistance
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** The split distance as the settings chip and the splits-card header print it. */
class ActivitySplitDistanceLabelTest {

    @Test fun `metric presets print as the round numbers the user picked`() {
        val formatter = formatter(UnitSystem.METRIC)

        // Not UnitFormatter.distance: a chosen setting is a round number, not a measurement.
        assertEquals("1 km", splitDistanceLabel(formatter, 1_000.0))
        assertEquals("0.5 km", splitDistanceLabel(formatter, 500.0))
        assertEquals("0.25 km", splitDistanceLabel(formatter, 250.0))
        assertEquals("5 km", splitDistanceLabel(formatter, 5_000.0))
    }

    @Test fun `imperial units re-express the derived header in miles`() {
        val formatter = formatter(UnitSystem.IMPERIAL)

        // Storage stays metric; the presets are exact mile fractions, so they come back as round miles.
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
