package tech.mmarca.openvitals.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitSystemPreferenceTest {

    // region measurement-system token mapping

    @Test fun `the US system maps to imperial`() {
        assertEquals(UnitSystem.IMPERIAL, unitSystemForMeasurementSystem("ussystem"))
        assertEquals(UnitSystem.IMPERIAL, unitSystemForMeasurementSystem("US"))
    }

    @Test fun `SI maps to metric`() {
        assertEquals(UnitSystem.METRIC, unitSystemForMeasurementSystem("metric"))
        assertEquals(UnitSystem.METRIC, unitSystemForMeasurementSystem("SI"))
    }

    @Test fun `the UK system maps to metric because it is metric-first`() {
        assertEquals(UnitSystem.METRIC, unitSystemForMeasurementSystem("uksystem"))
        assertEquals(UnitSystem.METRIC, unitSystemForMeasurementSystem("UK"))
    }

    @Test fun `an unknown or absent token maps to nothing`() {
        assertNull(unitSystemForMeasurementSystem(null))
        assertNull(unitSystemForMeasurementSystem(""))
        assertNull(unitSystemForMeasurementSystem("cadence"))
    }

    // endregion

    // region preference resolution

    @Test fun `SYSTEM resolves through the provider`() {
        assertEquals(
            UnitSystem.IMPERIAL,
            UnitSystemPreference.SYSTEM.resolve { UnitSystem.IMPERIAL },
        )
        assertEquals(
            UnitSystem.METRIC,
            UnitSystemPreference.SYSTEM.resolve { UnitSystem.METRIC },
        )
    }

    @Test fun `explicit choices never consult the provider`() {
        val untouchable: () -> UnitSystem = {
            throw AssertionError("an explicit choice consulted the OS")
        }
        assertEquals(UnitSystem.METRIC, UnitSystemPreference.METRIC.resolve(untouchable))
        assertEquals(UnitSystem.IMPERIAL, UnitSystemPreference.IMPERIAL.resolve(untouchable))
    }

    // endregion

    // region storage values

    @Test fun `every preference round-trips through its storage value`() {
        UnitSystemPreference.entries.forEach { preference ->
            assertEquals(
                preference,
                UnitSystemPreference.fromStorageValue(preference.storageValue),
            )
        }
    }

    @Test fun `explicit storage values match the pre-SYSTEM enum names`() {
        // These are what the app wrote before "Follow system" existed; an
        // existing user's stored choice must keep decoding as that choice.
        assertEquals(UnitSystemPreference.METRIC, UnitSystemPreference.fromStorageValue("METRIC"))
        assertEquals(UnitSystemPreference.IMPERIAL, UnitSystemPreference.fromStorageValue("IMPERIAL"))
    }

    @Test fun `an unknown storage value decodes to nothing`() {
        assertNull(UnitSystemPreference.fromStorageValue(null))
        assertNull(UnitSystemPreference.fromStorageValue("metric"))
        assertNull(UnitSystemPreference.fromStorageValue("NAUTICAL"))
    }

    // endregion
}
