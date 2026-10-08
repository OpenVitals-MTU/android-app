package tech.mmarca.openvitals.domain.insights

import kotlin.math.sqrt

/** Whether the watch was on a wrist during a minute. */
enum class WearState { WORN, NOT_WORN }

/** Why a minute is [WearState.NOT_WORN], in the order the rules fire. */
enum class NotWornReason {
    /** The watch recorded nothing for the minute. */
    ABSENT,

    /** On the charger. Definitive. */
    CHARGING,

    /** The off-body sensor said so. */
    OFF_BODY,

    /** Heart rate was being recorded and the sensor saw no pulse for longer than a dropout. */
    NO_PULSE,

    /** Still for an hour by van Hees' rule, and no pulse anywhere in that hour. */
    STILL_NO_PULSE,
}

/** The verdict per grid minute, with counts for the night summary. */
class WearStates(val states: Array<WearState>, val reasons: Array<NotWornReason?>) {
    val size: Int get() = states.size
    val wornMinutes: Int get() = states.count { it == WearState.WORN }
    val notWornMinutes: Int get() = size - wornMinutes
    val reasonCounts: Map<NotWornReason, Int>
        get() = reasons.filterNotNull().groupingBy { it }.eachCount()

    fun isWorn(index: Int): Boolean = states[index] == WearState.WORN
}

/**
 * Decides, per minute, whether the watch was worn, so a watch on a table
 * or a charger can never be scored as sleep. Instantaneous evidence first
 * (charging, off-body, no pulse), then the published accelerometer rule:
 * van Hees 2013 / GGIR call a window non-wear when the acceleration's
 * standard deviation stays under 13 mg on at least two of three axes for an
 * hour. A wrist in deep sleep can be that still too, so here the rule
 * fires only when no minute of the hour carried a pulse: a table has no
 * pulse, a sleeper does. Without the heart rate grant the rule stands
 * alone, as published.
 *
 * The window's standard deviation is pooled exactly from the per-minute
 * means and standard deviations the watch sends, weighted by sample count.
 * Pure and deterministic.
 */
object WearStateDetector {

    data class Config(
        /** The van Hees window, minutes, centred on the minute. GGIR's default hour. */
        val windowMinutes: Int = 60,
        /** GGIR's default: 13 mg. */
        val stillSdG: Float = 0.013f,
        /** "At least two of three axes." */
        val stillAxes: Int = 2,
        /** A pulse gap this short inside a worn stretch is a sensor dropout, not a bare wrist. */
        val pulseGapToleranceMinutes: Int = 2,
    )

    fun detect(grid: WearMinuteGrid, config: Config = Config()): WearStates {
        val n = grid.size
        val states = Array(n) { WearState.WORN }
        val reasons = arrayOfNulls<NotWornReason>(n)

        for (index in 0 until n) {
            val reason = when {
                !grid.present[index] -> NotWornReason.ABSENT
                grid.flags[index].isCharging -> NotWornReason.CHARGING
                grid.flags[index].isOffBody -> NotWornReason.OFF_BODY
                else -> null
            }
            if (reason != null) {
                states[index] = WearState.NOT_WORN
                reasons[index] = reason
            }
        }
        markPulseGaps(grid, states, reasons, config)
        markStillWithoutPulse(grid, states, reasons, config)
        return WearStates(states, reasons)
    }

    /** Runs of recorded-but-pulseless minutes longer than the tolerance, while heart rate was being recorded. */
    private fun markPulseGaps(grid: WearMinuteGrid, states: Array<WearState>, reasons: Array<NotWornReason?>, config: Config) {
        var runStart = -1
        for (index in 0..grid.size) {
            val pulseless = index < grid.size && states[index] == WearState.WORN &&
                grid.flags[index].heartRateRecording && !grid.hasPulse(index)
            if (pulseless) {
                if (runStart < 0) runStart = index
                continue
            }
            if (runStart >= 0 && index - runStart > config.pulseGapToleranceMinutes) {
                for (at in runStart until index) {
                    states[at] = WearState.NOT_WORN
                    reasons[at] = NotWornReason.NO_PULSE
                }
            }
            runStart = -1
        }
    }

    /**
     * The accelerometer rule, with prefix sums so every window is O(1): per
     * axis `var = Σn(sd² + mean²)/Σn − (Σn·mean/Σn)²` over the recorded
     * minutes of the window.
     */
    private fun markStillWithoutPulse(grid: WearMinuteGrid, states: Array<WearState>, reasons: Array<NotWornReason?>, config: Config) {
        val n = grid.size
        val half = config.windowMinutes / 2
        val samples = LongArray(n + 1)
        val weightedMean = Array(3) { DoubleArray(n + 1) }
        val weightedSquare = Array(3) { DoubleArray(n + 1) }
        val pulses = IntArray(n + 1)
        for (index in 0 until n) {
            val count = if (grid.present[index]) grid.sampleCount[index] else 0
            samples[index + 1] = samples[index] + count
            pulses[index + 1] = pulses[index] + if (grid.hasPulse(index)) 1 else 0
            for (axis in 0 until 3) {
                val mean = grid.meanG[axis][index].toDouble()
                val sd = grid.sdG[axis][index].toDouble()
                weightedMean[axis][index + 1] = weightedMean[axis][index] + count * mean
                weightedSquare[axis][index + 1] = weightedSquare[axis][index] + count * (sd * sd + mean * mean)
            }
        }
        for (index in 0 until n) {
            if (states[index] != WearState.WORN) continue
            val from = maxOf(0, index - half)
            val to = minOf(n, index + half)
            val count = samples[to] - samples[from]
            if (count == 0L || pulses[to] - pulses[from] > 0) continue
            var stillAxes = 0
            for (axis in 0 until 3) {
                val mean = (weightedMean[axis][to] - weightedMean[axis][from]) / count
                val variance = (weightedSquare[axis][to] - weightedSquare[axis][from]) / count - mean * mean
                val sd = if (variance > 0.0) sqrt(variance) else 0.0
                if (sd < config.stillSdG) stillAxes++
            }
            if (stillAxes >= config.stillAxes) {
                states[index] = WearState.NOT_WORN
                reasons[index] = NotWornReason.STILL_NO_PULSE
            }
        }
    }
}
