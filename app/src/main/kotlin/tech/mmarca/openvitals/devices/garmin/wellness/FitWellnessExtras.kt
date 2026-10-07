package tech.mmarca.openvitals.devices.garmin.wellness

import java.time.Instant
import tech.mmarca.openvitals.core.fit.FitMessage
import tech.mmarca.openvitals.core.fit.fitInstant

/** `functional_metrics` (356): the thresholds the watch currently holds. */
data class FitFunctionalMetrics(
    val time: Instant,
    val functionalThresholdPowerWatts: Int? = null,
    val runningLactateThresholdPowerWatts: Int? = null,
    val runningLactateThresholdHeartRateBpm: Int? = null,
) {
    val isEmpty: Boolean
        get() = functionalThresholdPowerWatts == null &&
            runningLactateThresholdPowerWatts == null &&
            runningLactateThresholdHeartRateBpm == null
}

/** `hill_score` (402): the score and its two parts, each 0..100. */
data class FitHillScore(
    val time: Instant,
    val score: Int? = null,
    val strength: Int? = null,
    val endurance: Int? = null,
) {
    val isEmpty: Boolean
        get() = score == null && strength == null && endurance == null
}

/** `endurance_score` (403), on Garmin's own scale. */
data class FitEnduranceScore(
    val time: Instant,
    val score: Int,
)

/**
 * The series the wellness decoder once skipped:
 * Pulse Ox in the monitor file, the five-minute HRV values, the thresholds
 * and scores in the metrics file, and a night's restless moments.
 */
data class FitWellnessExtras(
    /** Pulse Ox `(time, percent)`: the all-day and sleep readings of the monitor file. */
    val spo2: List<Pair<Instant, Int>> = emptyList(),
    /** Five-minute RMSSD values `(time, ms)` from the HRV file; the summary averages them. */
    val hrvValues: List<Pair<Instant, Double>> = emptyList(),
    val functionalMetrics: FitFunctionalMetrics? = null,
    val hillScore: FitHillScore? = null,
    val enduranceScore: FitEnduranceScore? = null,
    /** `sleep_restless_moments` count. The importer keys it to the night. */
    val restlessMoments: Int? = null,
) {
    val isEmpty: Boolean
        get() = spo2.isEmpty() &&
            hrvValues.isEmpty() &&
            functionalMetrics == null &&
            hillScore == null &&
            enduranceScore == null &&
            restlessMoments == null

    /** Series concatenate; the one-per-file snapshots take the later file's. */
    fun merge(other: FitWellnessExtras): FitWellnessExtras = FitWellnessExtras(
        spo2 = spo2 + other.spo2,
        hrvValues = hrvValues + other.hrvValues,
        functionalMetrics = other.functionalMetrics ?: functionalMetrics,
        hillScore = other.hillScore ?: hillScore,
        enduranceScore = other.enduranceScore ?: enduranceScore,
        restlessMoments = other.restlessMoments ?: restlessMoments,
    )
}

/** Interprets the extras' messages for one file. Field numbers from the FIT profile. */
internal class FitExtrasInterpreter {
    private val spo2 = mutableListOf<Pair<Instant, Int>>()
    private val hrvValues = mutableListOf<Pair<Instant, Double>>()
    private var functionalMetrics: FitFunctionalMetrics? = null
    private var hillScore: FitHillScore? = null
    private var enduranceScore: FitEnduranceScore? = null
    private var restlessMoments: Int? = null

    /** Reads [message] if it is one of the extras; false means it was not. */
    fun interpret(message: FitMessage): Boolean {
        val values = message.values
        val at = message.timestamp?.let(::fitInstant)
        when (message.globalMessageNumber) {
            FitSpo2MessageNumber -> {
                val reading = values[FitSpo2ReadingFieldNumber]
                if (at != null && reading != null && reading in 1..100) spo2.add(at to reading.toInt())
            }

            FitHrvValueMessageNumber -> {
                val raw = values[FitHrvValueFieldNumber]
                if (at != null && raw != null && raw in 1 until FitUint16Invalid) {
                    hrvValues.add(at to raw / FitHrvValueScale)
                }
            }

            FitFunctionalMetricsMessageNumber -> if (at != null) {
                val read = FitFunctionalMetrics(
                    time = at,
                    functionalThresholdPowerWatts = values[FitFtpFieldNumber].uint16OrNull(),
                    runningLactateThresholdPowerWatts = values[FitLactatePowerFieldNumber].uint16OrNull(),
                    runningLactateThresholdHeartRateBpm = values[FitLactateHeartRateFieldNumber].uint8OrNull(),
                )
                if (!read.isEmpty) functionalMetrics = read
            }

            FitHillScoreMessageNumber -> if (at != null) {
                val read = FitHillScore(
                    time = at,
                    score = values[FitHillScoreFieldNumber].scoreOrNull(),
                    strength = values[FitHillStrengthFieldNumber].scoreOrNull(),
                    endurance = values[FitHillEnduranceFieldNumber].scoreOrNull(),
                )
                if (!read.isEmpty) hillScore = read
            }

            FitEnduranceScoreMessageNumber -> if (at != null) {
                values[FitEnduranceScoreFieldNumber].uint16OrNull()?.let { enduranceScore = FitEnduranceScore(at, it) }
            }

            FitSleepRestlessMomentsMessageNumber -> {
                values[FitRestlessMomentsCountFieldNumber].uint8OrNull()?.let { restlessMoments = it }
            }

            else -> return false
        }
        return true
    }

    fun result(): FitWellnessExtras = FitWellnessExtras(
        spo2 = spo2.toList(),
        hrvValues = hrvValues.toList(),
        functionalMetrics = functionalMetrics,
        hillScore = hillScore,
        enduranceScore = enduranceScore,
        restlessMoments = restlessMoments,
    )

    /** A uint16 that is set: zero is "not computed" for every field read here. */
    private fun Long?.uint16OrNull(): Int? = this?.takeIf { it in 1 until FitUint16Invalid }?.toInt()

    private fun Long?.uint8OrNull(): Int? = this?.takeIf { it in 1 until FitUint8Invalid }?.toInt()

    /** A 0..100 score; the uint8 sentinel is absent, zero is a real score. */
    private fun Long?.scoreOrNull(): Int? = this?.takeIf { it in 0..100 }?.toInt()

    private companion object {
        const val FitSpo2MessageNumber = 269
        const val FitSpo2ReadingFieldNumber = 0 // uint8, percent
        const val FitHrvValueMessageNumber = 371
        const val FitHrvValueFieldNumber = 0 // uint16, scale 128, ms
        const val FitHrvValueScale = 128.0
        const val FitFunctionalMetricsMessageNumber = 356
        const val FitFtpFieldNumber = 4 // uint16, W
        const val FitLactatePowerFieldNumber = 7 // uint16, W
        const val FitLactateHeartRateFieldNumber = 8 // uint8, bpm
        const val FitHillScoreMessageNumber = 402
        const val FitHillScoreFieldNumber = 0
        const val FitHillStrengthFieldNumber = 1
        const val FitHillEnduranceFieldNumber = 2
        const val FitEnduranceScoreMessageNumber = 403
        const val FitEnduranceScoreFieldNumber = 0 // uint16
        const val FitSleepRestlessMomentsMessageNumber = 382
        const val FitRestlessMomentsCountFieldNumber = 1 // uint8
        const val FitUint16Invalid = 0xFFFFL
        const val FitUint8Invalid = 0xFFL
    }
}
