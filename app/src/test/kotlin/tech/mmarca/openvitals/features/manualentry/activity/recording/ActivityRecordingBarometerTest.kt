package tech.mmarca.openvitals.features.manualentry.activity.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The barometer's running climb, one altitude reading at a time. */
class ActivityRecordingBarometerTest {

    private fun ActivityRecordingState.read(vararg altitudes: Double): ActivityRecordingState =
        altitudes.fold(this) { state, altitude -> state.withBarometerAltitude(altitude) }

    @Test fun `the first reading anchors and banks nothing`() {
        val state = ActivityRecordingState().read(512.0)
        assertTrue(state.hasBarometerElevation)
        assertEquals(512.0, state.lastBarometerAltitudeMeters!!, 0.0)
        assertEquals(0.0, state.barometerElevationGainedMeters, 0.0)
    }

    @Test fun `a steady climb is banked`() {
        val climb = DoubleArray(600) { i -> 500.0 + i * 0.1 }
        val state = ActivityRecordingState().read(*climb)
        // 60 m up; the anchor may trail the last reading by under 10 m.
        assertEquals(60.0, state.barometerElevationGainedMeters, 10.0)
        assertEquals(0.0, state.barometerElevationLostMeters, 0.0)
    }

    @Test fun `a glitched reading re-anchors instead of banking a mountain`() {
        // A 0 hPa reading converts to ~44 km. It used to bank 30 % of the jump: 13 km of climb.
        val state = ActivityRecordingState().read(500.0, 500.0, 44_330.0, 500.0, 500.0)
        assertEquals(0.0, state.barometerElevationGainedMeters, 0.0)
        assertEquals(0.0, state.barometerElevationLostMeters, 0.0)
        assertEquals(500.0, state.lastBarometerAltitudeMeters!!, 0.0)
    }

    @Test fun `jitter under the step never accumulates`() {
        val jitter = DoubleArray(1_000) { i -> if (i % 2 == 0) 502.0 else 498.0 }
        val state = ActivityRecordingState().read(500.0, *jitter)
        assertEquals(0.0, state.barometerElevationGainedMeters, 0.0)
    }
}
