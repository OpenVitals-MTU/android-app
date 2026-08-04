package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

/**
 * One reading from a watch that computes its own body-energy score (Garmin Body
 * Battery), paired with what this app's model predicted for that moment.
 *
 * The only evidence the gains are fitted from. It is another vendor's MODEL
 * rather than ground truth, which is why one reading nudges a gain so little:
 * the fit is meant to converge over days of agreement, not to chase a watch that
 * disagrees for an hour.
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
 * The generation of the watch-fit machinery, bumped when a fix means the
 * evidence already consumed has to be read again.
 *
 * Distinct from the algorithm version, which describes the MODEL. A bug in the
 * fit leaves the model untouched, so an algorithm bump to force a refit would
 * both misdescribe the change and discard the stored chain for nothing.
 *
 * 1 — the watch fit watermark was advanced past days whose timelines had not
 * been computed yet, and was never rewound when the gains reset. Between them,
 * an install could sit on thousands of stored samples with a watch observation
 * count of 2 and every gain at exactly 1.00.
 */
const val BodyEnergyWatchFitEpoch = 1

/**
 * A watch reading moves a gain less than a lived-experience check-in would.
 *
 * A watch reading is another model's OUTPUT, not the user's experience. The rate
 * is deliberately modest so the watch converges the gains in days rather than
 * months.
 *
 * The trade-off that buys: a day of readings that disagree hard and consistently
 * CAN reach a gain's clamp. That is judged acceptable — such a day means the
 * model is badly wrong and a large correction is the right answer — and the
 * hourly downsampling plus the [BodyEnergyCalibration] bounds still stop it
 * running away.
 */
const val DefaultWatchLearningRate = 0.1

/**
 * Fits the personal gains from watch readings — transparently.
 *
 * Each reading says "the model predicted P, the watch says O". A gap means the
 * model moved the score too much or too little in the direction its dominant
 * driver was pushing. We nudge exactly that one gain by a small step, bounded to
 * [BodyEnergyCalibration.MinGain]..[BodyEnergyCalibration.MaxGain], so the
 * outcome is always one legible number the user can read and override — not a
 * hidden optimiser.
 *
 * A drain driver (activity, basal, stress): if the watch reads *lower* than
 * predicted, the user was drained harder than modelled, so raise that drain
 * gain; if it reads *higher*, lower it. A charge driver (sleep recovery) is the
 * mirror: higher → raise the charge gain.
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
        // Normalised error in [-1, 1]: positive means the observation was higher
        // than predicted, negative means lower.
        val error = (reading.observedScore - reading.predictedScore) / 100.0
        if (error == 0.0) continue
        val step = watchLearningRate * error

        // An observation must move the gain that scales the drain it is blaming.
        // Anything else is either impotent — no gain scales that component — or
        // aimed at a component that was not responsible, and both show up as a
        // gain drifting to answer for something it does not control.
        when (reading.dominantInfluence) {
            // Felt better → sleep recharged more than modelled → raise the gain.
            BodyEnergyPrimaryInfluence.SLEEP_RECOVERY -> sleep += step
            // Felt worse → activity drained more than modelled → raise the gain.
            // Recovery debt belongs here too: it is scaled by `activityDrainGain`
            // like the other two, being the tail of the same effort. It used to
            // move `basalDrainGain`, which scales the waking floor and not
            // recovery debt at all — so it could not fix the error it aimed at,
            // and corrupted the basal figure while failing to.
            BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
            BodyEnergyPrimaryInfluence.EXERTION,
            BodyEnergyPrimaryInfluence.RECOVERY_DEBT,
            -> activity -= step
            BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE -> stress -= step
            // The one influence basal should answer for: the timeline reports
            // steady exactly when every competing drain is zero, which leaves the
            // basal floor as the only thing that moved the score.
            BodyEnergyPrimaryInfluence.STEADY -> basal -= step
            // A charge with no sleep in the bucket. The waking-rest charge is
            // scaled by `sleepChargeGain` like sleep itself, so that is the gain
            // to move. If the two ever need to diverge, that is a fifth gain
            // rather than a different routing here.
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
