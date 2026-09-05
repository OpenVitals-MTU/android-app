package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

/**
 * One watch body-energy reading paired with this app's prediction. Another
 * vendor's model, not ground truth, so one reading nudges a gain little.
 */
data class BodyEnergyWatchReading(
    val time: Instant,
    /** The watch's own 0-100 body-energy score. */
    val observedScore: Int,
    /** This app's score at [time] under the current gains. */
    val predictedScore: Int,
    val dominantInfluence: BodyEnergyPrimaryInfluence,
)

/**
 * The generation of the watch-fit machinery. Bump when a fix means the
 * consumed evidence must be read again. Distinct from the algorithm version.
 */
const val BodyEnergyWatchFitEpoch = 1

/**
 * How much one watch reading moves a gain. Modest, so the fit converges in
 * days; a day of hard disagreement can reach a clamp, which is acceptable.
 */
const val DefaultWatchLearningRate = 0.1

/**
 * Fits the personal gains from watch readings. Each reading nudges the one
 * gain behind its dominant driver by a bounded step. A drain driver reading
 * lower than predicted raises that drain gain; a charge driver is the mirror.
 */
fun fitBodyEnergyGains(
    current: BodyEnergyCalibration,
    watchReadings: List<BodyEnergyWatchReading> = emptyList(),
    watchLearningRate: Double = DefaultWatchLearningRate,
): BodyEnergyCalibration {
    if (watchReadings.isEmpty()) return current.normalized()

    var sleep = current.sleepChargeGain
    var activity = current.activityDrainGain
    var basal = current.basalDrainGain
    var stress = current.stressDrainGain

    for (reading in watchReadings) {
        // Normalised error in [-1, 1]: positive means the watch read higher.
        val error = (reading.observedScore - reading.predictedScore) / 100.0
        if (error == 0.0) continue
        val step = watchLearningRate * error

        // An observation must move the gain that scales the component it blames.
        when (reading.dominantInfluence) {
            // Felt better → sleep recharged more than modelled → raise the gain.
            BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> sleep += step
            // Recovery debt is scaled by the activity gain, so it moves that one.
            BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
            BodyEnergyPrimaryInfluence.EXERTION,
            BodyEnergyPrimaryInfluence.RECOVERY_DEBT,
            -> activity -= step
            BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> stress -= step
            // Steady means every competing drain was zero, so basal moved the score.
            BodyEnergyPrimaryInfluence.STEADY -> basal -= step
            // Waking rest is scaled by the sleep gain, so that is the gain to move.
            BodyEnergyPrimaryInfluence.QUIET_REST -> sleep += step
            BodyEnergyPrimaryInfluence.NO_DATA -> Unit
        }
    }

    val lo = BodyEnergyCalibration.MinGain
    val hi = BodyEnergyCalibration.MaxGain
    return current.copy(
        sleepChargeGain = sleep.coerceIn(lo, hi),
        activityDrainGain = activity.coerceIn(lo, hi),
        basalDrainGain = basal.coerceIn(lo, hi),
        stressDrainGain = stress.coerceIn(lo, hi),
        watchObservationCount = current.watchObservationCount + watchReadings.size,
    ).normalized()
}
