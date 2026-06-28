package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric

enum class PhysiologicalStressLevel {
    RESTING,
    LOW,
    MEDIUM,
    HIGH,
    NEEDS_MORE_DATA,
}

enum class PhysiologicalStressConfidence {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA,
}

data class PhysiologicalStressEstimate(
    val level: PhysiologicalStressLevel,
    val label: String,
    val score: Int?,
    val summary: String,
    val detail: String,
    val confidence: PhysiologicalStressConfidence,
    val confidenceReason: String,
    val hrvPercentFromBaseline: Int?,
    val restingHeartRateDeltaBpm: Int?,
    val averageHeartRateDeltaFromRestingBpm: Int?,
    val hasWorkoutInfluence: Boolean,
    val contributingFactors: List<String>,
    val dataCoverage: List<String>,
    val caveats: List<String>,
) {
    companion object {
        val NeedsMoreData = PhysiologicalStressEstimate(
            level = PhysiologicalStressLevel.NEEDS_MORE_DATA,
            label = "Needs more data",
            score = null,
            summary = "Stress estimate needs HRV or heart-rate baseline data.",
            detail = "OpenVitals needs HRV, resting heart rate, or average heart rate context to estimate physiological stress.",
            confidence = PhysiologicalStressConfidence.NO_DATA,
            confidenceReason = "no_stress_signals",
            hrvPercentFromBaseline = null,
            restingHeartRateDeltaBpm = null,
            averageHeartRateDeltaFromRestingBpm = null,
            hasWorkoutInfluence = false,
            contributingFactors = emptyList(),
            dataCoverage = emptyList(),
            caveats = defaultStressCaveats(),
        )
    }
}

fun calculatePhysiologicalStress(data: DashboardData): PhysiologicalStressEstimate {
    var rawScore = 32
    var signalCount = 0
    var contextSignalCount = 0
    val factors = mutableListOf<String>()
    val dataCoverage = stressDataCoverage(data)

    val hrvPercent = hrvPercentFromBaseline(data)
    if (hrvPercent != null) {
        signalCount += 1
        rawScore += when {
            hrvPercent <= -30 -> 34
            hrvPercent <= -15 -> 22
            hrvPercent <= -7 -> 10
            hrvPercent >= 20 -> -8
            hrvPercent >= 8 -> -5
            else -> -3
        }
        factors += when {
            hrvPercent < 0 -> "HRV is ${abs(hrvPercent)}% below your usual baseline."
            hrvPercent > 0 -> "HRV is $hrvPercent% above your usual baseline."
            else -> "HRV is near your usual baseline."
        }
    }

    val restingHeartRateDelta = restingHeartRateDelta(data)
    if (restingHeartRateDelta != null) {
        signalCount += 1
        rawScore += when {
            restingHeartRateDelta >= 10 -> 26
            restingHeartRateDelta >= 6 -> 17
            restingHeartRateDelta >= 3 -> 8
            restingHeartRateDelta <= -4 -> -6
            else -> 0
        }
        factors += when {
            restingHeartRateDelta > 0 -> "Resting heart rate is +$restingHeartRateDelta bpm versus baseline."
            restingHeartRateDelta < 0 -> "Resting heart rate is ${abs(restingHeartRateDelta)} bpm below baseline."
            else -> "Resting heart rate is near baseline."
        }
    }

    val averageHeartRateDelta = averageHeartRateDeltaFromResting(data)
    if (averageHeartRateDelta != null) {
        signalCount += 1
        rawScore += when {
            averageHeartRateDelta >= 40 -> 22
            averageHeartRateDelta >= 28 -> 15
            averageHeartRateDelta >= 18 -> 8
            averageHeartRateDelta <= 8 -> -4
            else -> 0
        }
        factors += "Average heart rate today is $averageHeartRateDelta bpm above resting heart rate."
    }

    val hasWorkoutInfluence = data.workouts.isNotEmpty() ||
        ((data.weeklyIntensityMinutes?.todayModerateEquivalentMinutes ?: 0) >= 20)
    if (hasWorkoutInfluence) {
        factors += "Recorded activity today may raise this physiological estimate."
        val todayIntensity = data.weeklyIntensityMinutes?.todayModerateEquivalentMinutes ?: 0
        rawScore += when {
            todayIntensity >= 60 -> 8
            todayIntensity >= 30 -> 5
            else -> 3
        }
        contextSignalCount += 1
    }

    if (signalCount == 0) {
        return PhysiologicalStressEstimate.NeedsMoreData.copy(dataCoverage = dataCoverage)
    }

    if (DashboardMetric.SLEEP in data.loadedMetrics && data.sleepScore.confidence != SleepScoreConfidence.NO_DATA) {
        contextSignalCount += 1
        val sleepScore = data.sleepScore.score
        rawScore += when {
            sleepScore < 45 -> 12
            sleepScore < 65 -> 6
            sleepScore >= 82 -> -5
            else -> 0
        }
        factors += when {
            sleepScore < 45 -> "Sleep score is $sleepScore/100, which can raise physiological strain today."
            sleepScore < 65 -> "Sleep score is $sleepScore/100, so recovery context is mixed."
            sleepScore >= 82 -> "Sleep score is $sleepScore/100, which supports lower strain."
            else -> "Sleep score is $sleepScore/100."
        }
    }

    if (DashboardMetric.HYDRATION in data.loadedMetrics) {
        contextSignalCount += 1
        rawScore += when {
            data.hydrationLiters <= 0.0 -> 4
            data.hydrationLiters < 1.0 -> 3
            else -> 0
        }
        factors += when {
            data.hydrationLiters <= 0.0 -> "No hydration is logged for today; dehydration can raise heart-rate strain."
            data.hydrationLiters < 1.0 -> "Hydration is ${formatOneDecimal(data.hydrationLiters)} L so far today."
            else -> "Hydration is ${formatOneDecimal(data.hydrationLiters)} L so far today."
        }
    }

    nutritionContext(data)?.let { factor ->
        contextSignalCount += 1
        rawScore += factor.scoreDelta
        factors += factor.text
    }

    temperatureContext(data)?.let { factor ->
        contextSignalCount += 1
        rawScore += factor.scoreDelta
        factors += factor.text
    }

    data.weeklyCardioLoad?.let { load ->
        if (load.targetScore > 0) {
            val ratio = load.currentScore / load.targetScore.toDouble()
            when {
                ratio > 1.35 -> {
                    contextSignalCount += 1
                    rawScore += 7
                    factors += "Weekly training load is ${(ratio * 100.0).roundToInt()}% of target."
                }
                ratio in 0.75..1.20 -> {
                    contextSignalCount += 1
                    factors += "Weekly training load is near target."
                }
            }
        }
    }

    if ((data.mindfulnessMinutes ?: 0) >= 5) {
        contextSignalCount += 1
        rawScore -= 4
        factors += "${data.mindfulnessMinutes} min of mindfulness is logged today."
    }

    val score = rawScore.coerceIn(0, 100)
    val level = stressLevelForScore(score)
    val label = stressLabel(level)
    val sparseHrv = data.hrvRmssdMs != null && data.hrvSampleCount == 1
    val confidence = stressConfidence(
        signalCount = signalCount,
        contextSignalCount = contextSignalCount,
        hasWorkoutInfluence = hasWorkoutInfluence,
        sparseHrv = sparseHrv,
    )
    val summary = when (level) {
        PhysiologicalStressLevel.RESTING -> "Signals look restful right now."
        PhysiologicalStressLevel.LOW -> "Signals suggest low physiological stress."
        PhysiologicalStressLevel.MEDIUM -> "Signals suggest elevated physiological strain."
        PhysiologicalStressLevel.HIGH -> "Signals suggest high physiological strain."
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> "Stress estimate needs more data."
    }
    val detail = when (level) {
        PhysiologicalStressLevel.RESTING -> "The estimate is in the resting range because HRV and heart-rate signals look calm."
        PhysiologicalStressLevel.LOW -> "The estimate is in the low range; this usually means no strong strain signal is visible."
        PhysiologicalStressLevel.MEDIUM -> "The estimate is elevated. Low HRV, higher resting heart rate, activity, illness, caffeine, food, heat, or alcohol can all contribute."
        PhysiologicalStressLevel.HIGH -> "The estimate is high. Treat this as physiological strain, especially if it follows exercise, poor sleep, illness, caffeine, alcohol, heat, or dehydration."
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> "OpenVitals needs more local signals to estimate physiological stress."
    }
    val caveats = buildList {
        addAll(defaultStressCaveats())
        if (hasWorkoutInfluence) {
            add("Recorded workouts or active minutes are present today, so this should not be read as a quiet resting stress score.")
        }
        if (confidence == PhysiologicalStressConfidence.LOW) {
            add("Confidence is low because only part of the HRV/resting-heart-rate context is available.")
        }
        if (sparseHrv) {
            add("Only one HRV point is available for this day, so HRV context is thin.")
        }
    }

    return PhysiologicalStressEstimate(
        level = level,
        label = label,
        score = score,
        summary = summary,
        detail = detail,
        confidence = confidence,
        confidenceReason = stressConfidenceReason(confidence, signalCount, hasWorkoutInfluence),
        hrvPercentFromBaseline = hrvPercent,
        restingHeartRateDeltaBpm = restingHeartRateDelta,
        averageHeartRateDeltaFromRestingBpm = averageHeartRateDelta,
        hasWorkoutInfluence = hasWorkoutInfluence,
        contributingFactors = factors,
        dataCoverage = dataCoverage,
        caveats = caveats,
    )
}

private fun hrvPercentFromBaseline(data: DashboardData): Int? {
    val hrv = data.hrvRmssdMs ?: return null
    val baseline = data.hrvBaselineRmssdMs ?: return null
    if (DashboardMetric.HRV !in data.loadedMetrics || hrv <= 0.0 || baseline <= 0.0) return null
    return ((hrv - baseline) / baseline * 100.0).roundToInt()
}

private fun restingHeartRateDelta(data: DashboardData): Int? {
    val baseline = data.restingHeartRateBaselineBpm ?: return null
    if (
        DashboardMetric.RESTING_HEART_RATE !in data.loadedMetrics ||
        data.restingHeartRateBpm <= 0 ||
        baseline <= 0
    ) {
        return null
    }
    return (data.restingHeartRateBpm - baseline).toInt()
}

private fun averageHeartRateDeltaFromResting(data: DashboardData): Int? {
    if (
        DashboardMetric.AVG_HEART_RATE !in data.loadedMetrics ||
        data.avgHeartRateBpm <= 0 ||
        data.restingHeartRateBpm <= 0
    ) {
        return null
    }
    return (data.avgHeartRateBpm - data.restingHeartRateBpm)
        .toInt()
        .takeIf { it >= 0 }
}

private fun stressLevelForScore(score: Int): PhysiologicalStressLevel =
    when (score) {
        in 0..25 -> PhysiologicalStressLevel.RESTING
        in 26..50 -> PhysiologicalStressLevel.LOW
        in 51..75 -> PhysiologicalStressLevel.MEDIUM
        else -> PhysiologicalStressLevel.HIGH
    }

private fun stressLabel(level: PhysiologicalStressLevel): String =
    when (level) {
        PhysiologicalStressLevel.RESTING -> "Resting"
        PhysiologicalStressLevel.LOW -> "Low"
        PhysiologicalStressLevel.MEDIUM -> "Medium"
        PhysiologicalStressLevel.HIGH -> "High"
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> "Needs more data"
    }

private fun stressConfidence(
    signalCount: Int,
    contextSignalCount: Int,
    hasWorkoutInfluence: Boolean,
    sparseHrv: Boolean,
): PhysiologicalStressConfidence =
    when {
        signalCount >= 3 && !hasWorkoutInfluence && !sparseHrv -> PhysiologicalStressConfidence.HIGH
        signalCount >= 2 || contextSignalCount >= 2 -> PhysiologicalStressConfidence.MEDIUM
        else -> PhysiologicalStressConfidence.LOW
    }

private fun stressConfidenceReason(
    confidence: PhysiologicalStressConfidence,
    signalCount: Int,
    hasWorkoutInfluence: Boolean,
): String =
    when {
        confidence == PhysiologicalStressConfidence.NO_DATA -> "no_stress_signals"
        hasWorkoutInfluence -> "activity_may_influence"
        signalCount >= 3 -> "hrv_resting_hr_average_hr"
        signalCount == 2 -> "partial_hrv_or_heart_rate_context"
        else -> "single_signal"
    }

private fun stressDataCoverage(data: DashboardData): List<String> =
    buildList {
        if (DashboardMetric.AVG_HEART_RATE in data.loadedMetrics) {
            add(
                when {
                    data.heartRateSampleCount > 0 -> {
                        "Heart rate used ${data.heartRateSampleCount} samples " +
                            sampleWindowText(data.heartRateSampleStartTime, data.heartRateSampleEndTime) +
                            "."
                    }
                    data.avgHeartRateBpm > 0 -> {
                        "Average heart rate is available, but raw same-day sample coverage was not available."
                    }
                    else -> "No heart-rate samples were available for this day."
                }
            )
        }
        if (DashboardMetric.HRV in data.loadedMetrics) {
            add(
                when {
                    data.hrvSampleCount > 1 -> {
                        "HRV used ${data.hrvSampleCount} RMSSD points " +
                            sampleWindowText(data.hrvSampleStartTime, data.hrvSampleEndTime) +
                            "."
                    }
                    data.hrvSampleCount == 1 -> {
                        "HRV used 1 RMSSD point " +
                            sampleWindowText(data.hrvSampleStartTime, data.hrvSampleEndTime) +
                            "; confidence is lower until more points arrive."
                    }
                    data.hrvRmssdMs != null -> {
                        "HRV average is available, but raw same-day sample coverage was not available."
                    }
                    else -> "No HRV points were available for this day."
                }
            )
        }
    }

private data class StressContextFactor(
    val scoreDelta: Int,
    val text: String,
)

private fun nutritionContext(data: DashboardData): StressContextFactor? {
    val hasNutrition = (data.caloriesInKcal != null && data.caloriesInKcal > 0.0) ||
        (data.proteinGrams != null && data.proteinGrams > 0.0) ||
        (data.carbsGrams != null && data.carbsGrams > 0.0) ||
        (data.fatGrams != null && data.fatGrams > 0.0)
    if (!hasNutrition) return null
    val calories = data.caloriesInKcal
    return if (calories != null && calories >= 1_000.0) {
        StressContextFactor(
            scoreDelta = 3,
            text = "Nutrition is logged; larger meals and digestion can raise heart-rate strain.",
        )
    } else {
        StressContextFactor(
            scoreDelta = 0,
            text = "Nutrition is logged for today's context.",
        )
    }
}

private fun temperatureContext(data: DashboardData): StressContextFactor? {
    val bodyTemperature = data.latestBodyTemperatureCelsius
    val skinDelta = data.latestSkinTemperatureDeltaCelsius
    val bodyLoaded = DashboardMetric.BODY_TEMPERATURE in data.loadedMetrics && bodyTemperature != null
    val skinLoaded = DashboardMetric.SKIN_TEMPERATURE in data.loadedMetrics && skinDelta != null
    if (!bodyLoaded && !skinLoaded) return null

    val warning = (bodyTemperature != null && bodyTemperature >= 37.7) ||
        (skinDelta != null && skinDelta >= 1.0)
    val elevated = warning ||
        (bodyTemperature != null && bodyTemperature >= 37.2) ||
        (skinDelta != null && skinDelta >= 0.5)
    val detail = buildList {
        bodyTemperature?.let { add("body temperature ${formatOneDecimal(it)} C") }
        skinDelta?.let { add("skin temperature ${formatSignedOneDecimal(it)} C") }
    }.joinToString(separator = ", ")
    return when {
        warning -> StressContextFactor(
            scoreDelta = 18,
            text = "Temperature context is elevated ($detail).",
        )
        elevated -> StressContextFactor(
            scoreDelta = 9,
            text = "Temperature context is slightly elevated ($detail).",
        )
        else -> StressContextFactor(
            scoreDelta = 0,
            text = "Temperature context is available and not elevated ($detail).",
        )
    }
}

private fun sampleWindowText(start: Instant?, end: Instant?): String {
    if (start == null || end == null) return "for this day"
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        .withZone(ZoneId.systemDefault())
    val startText = formatter.format(start)
    val endText = formatter.format(end)
    return if (start == end) {
        "at $startText"
    } else {
        "from $startText to $endText"
    }
}

private fun formatOneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)

private fun formatSignedOneDecimal(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return prefix + formatOneDecimal(value)
}

private fun defaultStressCaveats(): List<String> =
    listOf(
        "This estimate does not diagnose mental stress.",
        "Health Connect does not provide a stress score, so OpenVitals estimates physiological strain locally.",
        "Food, caffeine, alcohol, illness, heat, dehydration, exercise, and emotional excitement can all move the estimate.",
        "A true all-day stress model should use inactive-period HRV samples; this screen uses the local signals currently available.",
    )
