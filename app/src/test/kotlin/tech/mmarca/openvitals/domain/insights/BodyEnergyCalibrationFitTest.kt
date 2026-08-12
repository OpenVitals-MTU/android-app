package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

/**
 * The routing half of the fit: which gain an observation is allowed to move.
 *
 * An influence must move the gain that scales the component it blames, or the
 * gain drifts to answer for something it does not control.
 */
class BodyEnergyCalibrationFitTest {

    private val now: Instant = Instant.parse("2026-07-15T20:00:00Z")

    /** A watch reading disagreeing with the model, blamed on [influence]. */
    private fun reading(
        observed: Int,
        predicted: Int,
        influence: BodyEnergyPrimaryInfluence,
    ): BodyEnergyWatchReading =
        BodyEnergyWatchReading(
            time = now,
            observedScore = observed,
            predictedScore = predicted,
            dominantInfluence = influence,
        )

    /**
     * Enough identical readings to clear the per-reading step, which is small on
     * purpose — one watch reading is not meant to swing the model.
     */
    private fun repeated(
        count: Int,
        observed: Int,
        predicted: Int,
        influence: BodyEnergyPrimaryInfluence,
    ): List<BodyEnergyWatchReading> = List(count) { reading(observed, predicted, influence) }

    @Test
    fun `no readings leaves the gains at their defaults`() {
        val fitted = fitBodyEnergyGains(BodyEnergyCalibration())

        assertEquals(1.0, fitted.activityDrainGain, 0.0)
        assertEquals(0, fitted.watchObservationCount)
    }

    @Test
    fun `reading lower than predicted after activity raises the activity gain`() {
        // Predicted 70, the watch says 30 after a big walk.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 30, 70, BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY),
        )

        assertTrue(fitted.activityDrainGain > 1.0)
        assertEquals(10, fitted.watchObservationCount)
    }

    @Test
    fun `reading higher than predicted after sleep raises the sleep gain`() {
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 90, 60, BodyEnergyPrimaryInfluence.SLEEP_RECOVERY),
        )

        assertTrue(fitted.sleepChargeGain > 1.0)
    }

    @Test
    fun `gains never escape the bounded range`() {
        // Many extreme mismatches all pushing the same way.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(200, 0, 100, BodyEnergyPrimaryInfluence.EXERTION),
        )

        assertTrue(fitted.activityDrainGain <= BodyEnergyCalibration.MaxGain)
        assertTrue(fitted.activityDrainGain >= BodyEnergyCalibration.MinGain)
    }

    @Test
    fun `recovery debt moves the activity gain, not basal`() {
        // Recovery-debt drain is scaled by activityDrainGain. Routing it to
        // basal — as this used to — aimed at a gain that scales the waking floor
        // and not recovery debt at all, so it could never fix the error while
        // corrupting the basal figure trying.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 30, 70, BodyEnergyPrimaryInfluence.RECOVERY_DEBT),
        )

        assertTrue(fitted.activityDrainGain > 1.0)
        assertEquals(1.0, fitted.basalDrainGain, 0.0)
    }

    @Test
    fun `steady still moves basal, the one influence it answers for`() {
        // The timeline reports steady exactly when every competing drain is
        // zero, which leaves the basal floor as the only thing that moved.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 30, 70, BodyEnergyPrimaryInfluence.STEADY),
        )

        assertTrue(fitted.basalDrainGain > 1.0)
        assertEquals(1.0, fitted.activityDrainGain, 0.0)
    }

    @Test
    fun `elevated heart rate moves the stress gain alone`() {
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 30, 70, BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE),
        )

        assertTrue(fitted.stressDrainGain > 1.0)
        assertEquals(1.0, fitted.activityDrainGain, 0.0)
        assertEquals(1.0, fitted.basalDrainGain, 0.0)
    }

    @Test
    fun `quiet rest moves the sleep gain, which scales the rest charge`() {
        // The waking-rest charge is scaled by sleepChargeGain, so that is the
        // gain a quiet-rest mismatch has to move. Read HIGHER than predicted, so
        // resting recharged more than modelled and the gain goes up.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 90, 60, BodyEnergyPrimaryInfluence.QUIET_REST),
        )

        assertTrue(fitted.sleepChargeGain > 1.0)
        assertEquals(1.0, fitted.activityDrainGain, 0.0)
        assertEquals(1.0, fitted.basalDrainGain, 0.0)
        assertEquals(1.0, fitted.stressDrainGain, 0.0)
        assertEquals(10, fitted.watchObservationCount)
    }

    @Test
    fun `a no-data influence teaches nothing`() {
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = repeated(10, 30, 70, BodyEnergyPrimaryInfluence.NO_DATA),
        )

        assertEquals(1.0, fitted.sleepChargeGain, 0.0)
        assertEquals(1.0, fitted.activityDrainGain, 0.0)
        assertEquals(1.0, fitted.basalDrainGain, 0.0)
        assertEquals(1.0, fitted.stressDrainGain, 0.0)
    }
}
