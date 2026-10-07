package tech.mmarca.openvitals.features.vitals

import java.time.Instant
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/** Skin temperature is a variation from a per-record baseline; the screen keeps the two apart. */
class SkinTemperatureSectionsTest {

    private val formatter = UnitFormatter(
        unitSystemProvider = { UnitSystem.METRIC },
        localeProvider = { Locale.US },
    )

    private fun entry(hour: Int, delta: Double?, baseline: Double? = 33.0) = SkinTemperatureEntry(
        startTime = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        endTime = Instant.parse("2026-03-02T%02d:00:00Z".format(hour)),
        baselineCelsius = baseline,
        averageDeltaCelsius = delta,
        minDeltaCelsius = delta,
        maxDeltaCelsius = delta,
        measurementLocation = 0,
        source = "Test",
    )

    private fun label(entry: SkinTemperatureEntry) = skinTemperatureRowLabel(
        entry = entry,
        unitFormatter = formatter,
        withBaselineTemplate = "%1\$s from baseline · baseline %2\$s",
        withoutBaselineTemplate = "%1\$s from baseline",
        noVariationLabel = "No variation recorded",
    )

    @Test fun `a row says the variation and the record's own baseline`() {
        assertEquals("-0.3 deg C from baseline · baseline 33.0 deg C", label(entry(8, -0.3)))
        assertEquals("+0.2 deg C from baseline", label(entry(8, 0.2, baseline = null)))
    }

    @Test fun `a delta-less record never prints its baseline as a reading`() {
        // It used to print "33.0 deg C", an absolute temperature nobody measured.
        assertEquals("No variation recorded", label(entry(8, null)))
    }

    @Test fun `the period baseline is the one the records share`() {
        val baseline = listOf(entry(8, 0.1, 33.0), entry(9, 0.2, 33.04)).skinTemperaturePeriodBaseline()!!

        assertEquals(33.04, baseline.celsius, 1e-9)
        assertTrue(baseline.shared)
    }

    @Test fun `records that disagree fall back to the newest baseline, flagged`() {
        val baseline = listOf(entry(9, 0.1, 32.6), entry(8, 0.2, 33.0)).skinTemperaturePeriodBaseline()!!

        assertEquals(32.6, baseline.celsius, 1e-9)
        assertFalse(baseline.shared)
    }

    @Test fun `no record with a baseline means no period baseline`() {
        assertNull(listOf(entry(8, 0.1, baseline = null)).skinTemperaturePeriodBaseline())
        assertNull(emptyList<SkinTemperatureEntry>().skinTemperaturePeriodBaseline())
    }
}
