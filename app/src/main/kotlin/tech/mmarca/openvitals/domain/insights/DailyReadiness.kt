package tech.mmarca.openvitals.domain.insights

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes

enum class ReadinessState {
    READY,
    MODERATE,
    RECOVER,
    REST,
    UNKNOWN,
}

enum class ReadinessRecommendationType {
    HARD_TRAINING,
    MODERATE_TRAINING,
    LIGHT_ACTIVITY,
    MOBILITY,
    REST,
    CHECK_SYMPTOMS,
}

enum class ReadinessConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class ReadinessFactorImpact {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    WARNING,
}

enum class ReadinessFactorKind {
    SLEEP_BELOW_BASELINE,
    SLEEP_ABOVE_BASELINE,
    RESTING_HR_ELEVATED,
    RESTING_HR_NORMAL,
    HRV_BELOW_BASELINE,
    HRV_ABOVE_BASELINE,
    HRV_NORMAL,
    STRESS_HIGH,
    STRESS_LOW,
    TRAINING_LOAD_HIGH,
    TRAINING_LOAD_NORMAL,
    INTENSITY_MINUTES_ON_TARGET,
    INTENSITY_MINUTES_BEHIND,
    MISSING_INTENSITY_MINUTES,
    PHYSIOLOGICAL_STRESS_HIGH,
    PHYSIOLOGICAL_STRESS_LOW,
    MISSING_STRESS_DATA,
    TEMPERATURE_ELEVATED,
    HYDRATION_LOW,
    NUTRITION_LOGGED,
    MISSING_SLEEP_DATA,
    MISSING_HRV_DATA,
    NEW_USER_NOT_ENOUGH_BASELINE,
}

enum class HrvStatus {
    BALANCED,
    LOW,
    HIGH,
    UNUSUALLY_LOW,
    UNUSUALLY_HIGH,
    NEEDS_MORE_HRV,
}

data class HrvStatusInsight(
    val status: HrvStatus,
    val label: String,
    val detail: String,
    val currentRmssdMs: Double?,
    val baselineRmssdMs: Double?,
    val percentFromBaseline: Int?,
)

enum class IntensityMinutesStatus {
    GOAL_MET,
    ON_TRACK,
    BEHIND,
    LOW,
    NEEDS_MORE_DATA,
}

data class IntensityMinutesReadinessInsight(
    val status: IntensityMinutesStatus,
    val label: String,
    val detail: String,
    val moderateEquivalentMinutes: Int?,
    val targetMinutes: Int,
    val todayModerateEquivalentMinutes: Int?,
    val progressPercent: Int,
    val confidence: IntensityMinutesConfidence,
)

data class DailyReadinessGoalInputs(
    val stepsGoal: Double = 8_000.0,
    val hydrationLitersGoal: Double = 2.0,
    val activeMinutesGoal: Double = 45.0,
)

data class DailyReadinessFactor(
    val kind: ReadinessFactorKind,
    val label: String,
    val detail: String,
    val impact: ReadinessFactorImpact,
)

data class DailyReadinessInsight(
    val state: ReadinessState,
    val score: Int,
    val bodyEnergyScore: Int,
    val trainingReadinessScore: Int,
    val recommendationType: ReadinessRecommendationType,
    val statusTitle: String,
    val recommendation: String,
    val explanation: String,
    val alternative: String,
    val suggestedWorkout: String,
    val avoid: String,
    val strainTarget: String,
    val currentStrain: String?,
    val adaptiveGoal: String,
    val confidence: ReadinessConfidence,
    val confidenceReason: String,
    val hrvStatus: HrvStatusInsight,
    val intensityMinutes: IntensityMinutesReadinessInsight,
    val physiologicalStress: PhysiologicalStressEstimate,
    val factors: List<DailyReadinessFactor>,
    val recoveryModeSuggested: Boolean,
)

fun calculateHrvStatus(
    hrvRmssdMs: Double?,
    baselineRmssdMs: Double?,
    hasHrvData: Boolean,
): HrvStatusInsight {
    if (!hasHrvData || hrvRmssdMs == null || hrvRmssdMs <= 0.0) {
        return HrvStatusInsight(
            status = HrvStatus.NEEDS_MORE_HRV,
            label = "Needs more HRV",
            detail = "HRV was not available for this day.",
            currentRmssdMs = hrvRmssdMs,
            baselineRmssdMs = baselineRmssdMs,
            percentFromBaseline = null,
        )
    }
    if (baselineRmssdMs == null || baselineRmssdMs <= 0.0) {
        return HrvStatusInsight(
            status = HrvStatus.NEEDS_MORE_HRV,
            label = "Needs more HRV",
            detail = "HRV is recorded, but there is not enough history yet for a personal baseline.",
            currentRmssdMs = hrvRmssdMs,
            baselineRmssdMs = baselineRmssdMs,
            percentFromBaseline = null,
        )
    }

    val percent = ((hrvRmssdMs - baselineRmssdMs) / baselineRmssdMs * 100.0).roundToInt()
    val status = when {
        percent <= -30 -> HrvStatus.UNUSUALLY_LOW
        percent <= -15 -> HrvStatus.LOW
        percent >= 30 -> HrvStatus.UNUSUALLY_HIGH
        percent >= 15 -> HrvStatus.HIGH
        else -> HrvStatus.BALANCED
    }
    val label = when (status) {
        HrvStatus.BALANCED -> "Balanced"
        HrvStatus.LOW -> "Low"
        HrvStatus.HIGH -> "High"
        HrvStatus.UNUSUALLY_LOW -> "Unusually low"
        HrvStatus.UNUSUALLY_HIGH -> "Unusually high"
        HrvStatus.NEEDS_MORE_HRV -> "Needs more HRV"
    }
    val comparison = when {
        percent == 0 -> "near your usual baseline"
        percent > 0 -> "$percent% above your usual baseline"
        else -> "${abs(percent)}% below your usual baseline"
    }
    val detail = when (status) {
        HrvStatus.BALANCED -> "HRV is $comparison."
        HrvStatus.LOW -> "HRV is $comparison, which can point to incomplete recovery."
        HrvStatus.HIGH -> "HRV is $comparison. Higher HRV can be positive when other signals agree."
        HrvStatus.UNUSUALLY_LOW -> "HRV is $comparison, outside your usual range."
        HrvStatus.UNUSUALLY_HIGH -> "HRV is $comparison, outside your usual range."
        HrvStatus.NEEDS_MORE_HRV -> "HRV status needs more data."
    }
    return HrvStatusInsight(
        status = status,
        label = label,
        detail = detail,
        currentRmssdMs = hrvRmssdMs,
        baselineRmssdMs = baselineRmssdMs,
        percentFromBaseline = percent,
    )
}

fun calculateIntensityMinutesReadiness(
    weeklyIntensityMinutes: DashboardWeeklyIntensityMinutes?,
    hasIntensityData: Boolean,
): IntensityMinutesReadinessInsight {
    if (!hasIntensityData || weeklyIntensityMinutes == null ||
        weeklyIntensityMinutes.confidence == IntensityMinutesConfidence.NO_DATA
    ) {
        return IntensityMinutesReadinessInsight(
            status = IntensityMinutesStatus.NEEDS_MORE_DATA,
            label = "Needs more data",
            detail = "Intensity minutes need workouts, heart rate, active calories, or activity load history.",
            moderateEquivalentMinutes = null,
            targetMinutes = DefaultWeeklyIntensityMinutesTarget,
            todayModerateEquivalentMinutes = null,
            progressPercent = 0,
            confidence = IntensityMinutesConfidence.NO_DATA,
        )
    }

    val minutes = weeklyIntensityMinutes.moderateEquivalentMinutes
    val target = weeklyIntensityMinutes.targetMinutes
    val status = when {
        target > 0 && minutes >= target -> IntensityMinutesStatus.GOAL_MET
        weeklyIntensityMinutes.isOnPace -> IntensityMinutesStatus.ON_TRACK
        target > 0 && minutes >= target * 0.5 -> IntensityMinutesStatus.BEHIND
        else -> IntensityMinutesStatus.LOW
    }
    val label = when (status) {
        IntensityMinutesStatus.GOAL_MET -> "Goal met"
        IntensityMinutesStatus.ON_TRACK -> "On track"
        IntensityMinutesStatus.BEHIND -> "Behind pace"
        IntensityMinutesStatus.LOW -> "Low"
        IntensityMinutesStatus.NEEDS_MORE_DATA -> "Needs more data"
    }
    val confidenceText = when (weeklyIntensityMinutes.confidence) {
        IntensityMinutesConfidence.HIGH -> "high confidence"
        IntensityMinutesConfidence.MEDIUM -> "medium confidence"
        IntensityMinutesConfidence.LOW -> "low confidence estimate"
        IntensityMinutesConfidence.NO_DATA -> "no data"
    }
    val todayText = if (weeklyIntensityMinutes.todayModerateEquivalentMinutes > 0) {
        " Today added ${weeklyIntensityMinutes.todayModerateEquivalentMinutes}."
    } else {
        ""
    }
    val detail = when (status) {
        IntensityMinutesStatus.GOAL_MET -> {
            "$minutes/$target moderate-equivalent min this week; vigorous minutes count double.$todayText"
        }
        IntensityMinutesStatus.ON_TRACK -> {
            "$minutes/$target moderate-equivalent min this week, on pace for day ${weeklyIntensityMinutes.daysElapsed}."
        }
        IntensityMinutesStatus.BEHIND -> {
            "$minutes/$target moderate-equivalent min this week; expected about ${weeklyIntensityMinutes.expectedByNowMinutes} by now."
        }
        IntensityMinutesStatus.LOW -> {
            "$minutes/$target moderate-equivalent min this week; add easy aerobic work if recovery allows."
        }
        IntensityMinutesStatus.NEEDS_MORE_DATA -> "Intensity minutes need more data."
    }
    return IntensityMinutesReadinessInsight(
        status = status,
        label = label,
        detail = "$detail $confidenceText.",
        moderateEquivalentMinutes = minutes,
        targetMinutes = target,
        todayModerateEquivalentMinutes = weeklyIntensityMinutes.todayModerateEquivalentMinutes,
        progressPercent = weeklyIntensityMinutes.progressPercent,
        confidence = weeklyIntensityMinutes.confidence,
    )
}

fun calculateDailyReadiness(
    data: DashboardData,
    goals: DailyReadinessGoalInputs = DailyReadinessGoalInputs(),
): DailyReadinessInsight {
    var score = 68
    var bodyEnergyScore = 64
    var trainingReadinessScore = 66
    var availableSignals = 0
    var baselineSignals = 0
    var elevatedBodySignals = 0
    var unusualVitals = false
    val missingReasons = mutableListOf<String>()
    val factors = mutableListOf<DailyReadinessFactor>()

    fun addFactor(
        kind: ReadinessFactorKind,
        label: String,
        detail: String,
        impact: ReadinessFactorImpact,
    ) {
        factors += DailyReadinessFactor(kind, label, detail, impact)
    }

    if (DashboardMetric.SLEEP in data.loadedMetrics && data.sleepScore.confidence != SleepScoreConfidence.NO_DATA) {
        availableSignals += 1
        val sleepScore = data.sleepScore.score
        val sleepHours = data.sleepScore.sleepDurationMinutes / 60.0
        val sleepDetail = if (sleepHours > 0.0) {
            "Sleep scored $sleepScore/100 after ${formatHours(sleepHours)}."
        } else {
            "Sleep scored $sleepScore/100."
        }
        when {
            sleepScore >= 82 -> {
                score += 12
                bodyEnergyScore += 16
                trainingReadinessScore += 8
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
                    label = "Sleep helped recovery",
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.POSITIVE,
                )
            }
            sleepScore >= 65 -> {
                score += 5
                bodyEnergyScore += 7
                trainingReadinessScore += 3
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_ABOVE_BASELINE,
                    label = "Sleep was usable",
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.NEUTRAL,
                )
            }
            sleepScore >= 45 -> {
                score -= 8
                bodyEnergyScore -= 12
                trainingReadinessScore -= 6
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_BELOW_BASELINE,
                    label = "Sleep may limit recovery",
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.NEGATIVE,
                )
            }
            else -> {
                score -= 20
                bodyEnergyScore -= 24
                trainingReadinessScore -= 14
                elevatedBodySignals += 1
                addFactor(
                    kind = ReadinessFactorKind.SLEEP_BELOW_BASELINE,
                    label = "Sleep was low",
                    detail = sleepDetail,
                    impact = ReadinessFactorImpact.WARNING,
                )
            }
        }
    } else {
        score -= 6
        missingReasons += "missing_sleep_data"
        addFactor(
            kind = ReadinessFactorKind.MISSING_SLEEP_DATA,
            label = "Sleep data missing",
            detail = "Sleep data was not available, so today's recommendation is a rough guide.",
            impact = ReadinessFactorImpact.NEUTRAL,
        )
    }

    val hrv = data.hrvRmssdMs
    val hrvBaseline = data.hrvBaselineRmssdMs
    val hrvStatus = calculateHrvStatus(
        hrvRmssdMs = hrv,
        baselineRmssdMs = hrvBaseline,
        hasHrvData = DashboardMetric.HRV in data.loadedMetrics,
    )
    if (DashboardMetric.HRV in data.loadedMetrics && hrv != null && hrv > 0.0) {
        availableSignals += 1
        if (hrvBaseline != null && hrvBaseline > 0.0) {
            baselineSignals += 1
            when (hrvStatus.status) {
                HrvStatus.UNUSUALLY_LOW -> {
                    score -= 17
                    bodyEnergyScore -= 11
                    trainingReadinessScore -= 19
                    elevatedBodySignals += 1
                    addFactor(
                        kind = ReadinessFactorKind.HRV_BELOW_BASELINE,
                        label = "HRV Status: ${hrvStatus.label}",
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.WARNING,
                    )
                }
                HrvStatus.LOW -> {
                    score -= 8
                    bodyEnergyScore -= 5
                    trainingReadinessScore -= 10
                    addFactor(
                        kind = ReadinessFactorKind.HRV_BELOW_BASELINE,
                        label = "HRV Status: ${hrvStatus.label}",
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                HrvStatus.UNUSUALLY_HIGH -> {
                    score -= 4
                    bodyEnergyScore -= 2
                    trainingReadinessScore -= 3
                    addFactor(
                        kind = ReadinessFactorKind.HRV_ABOVE_BASELINE,
                        label = "HRV Status: ${hrvStatus.label}",
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                HrvStatus.HIGH -> {
                    score += 3
                    bodyEnergyScore += 2
                    trainingReadinessScore += 3
                    addFactor(
                        kind = ReadinessFactorKind.HRV_ABOVE_BASELINE,
                        label = "HRV Status: ${hrvStatus.label}",
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                HrvStatus.BALANCED -> {
                    score += 5
                    bodyEnergyScore += 3
                    trainingReadinessScore += 6
                    addFactor(
                        kind = ReadinessFactorKind.HRV_NORMAL,
                        label = "HRV Status: ${hrvStatus.label}",
                        detail = hrvStatus.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                HrvStatus.NEEDS_MORE_HRV -> Unit
            }
        } else {
            missingReasons += "new_user_not_enough_baseline"
            addFactor(
                kind = ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
                label = "HRV baseline building",
                detail = "HRV is recorded, but there is not enough history yet for a personal baseline.",
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    } else {
        missingReasons += "missing_hrv_data"
        addFactor(
            kind = ReadinessFactorKind.MISSING_HRV_DATA,
            label = "HRV data missing",
            detail = "HRV was not available, which lowers recommendation confidence.",
            impact = ReadinessFactorImpact.NEUTRAL,
        )
    }

    if (DashboardMetric.RESTING_HEART_RATE in data.loadedMetrics && data.restingHeartRateBpm > 0) {
        availableSignals += 1
        val baseline = data.restingHeartRateBaselineBpm
        if (baseline != null && baseline > 0) {
            baselineSignals += 1
            val delta = data.restingHeartRateBpm - baseline
            val detail = when {
                delta > 0 -> "Resting heart rate is +$delta bpm compared with your usual baseline."
                delta < 0 -> "Resting heart rate is ${abs(delta)} bpm below your usual baseline."
                else -> "Resting heart rate is near your usual baseline."
            }
            when {
                delta >= 8 -> {
                    score -= 16
                    bodyEnergyScore -= 12
                    trainingReadinessScore -= 12
                    elevatedBodySignals += 1
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_ELEVATED,
                        label = "Resting HR is elevated",
                        detail = detail,
                        impact = ReadinessFactorImpact.WARNING,
                    )
                }
                delta >= 4 -> {
                    score -= 8
                    bodyEnergyScore -= 5
                    trainingReadinessScore -= 6
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_ELEVATED,
                        label = "Resting HR is slightly elevated",
                        detail = detail,
                        impact = ReadinessFactorImpact.NEGATIVE,
                    )
                }
                else -> {
                    score += 4
                    bodyEnergyScore += 3
                    trainingReadinessScore += 3
                    addFactor(
                        kind = ReadinessFactorKind.RESTING_HR_NORMAL,
                        label = "Resting HR looks normal",
                        detail = detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
            }
        } else {
            missingReasons += "new_user_not_enough_baseline"
            addFactor(
                kind = ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE,
                label = "Resting HR baseline building",
                detail = "Resting heart rate is available, but there is not enough history yet for a personal baseline.",
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }

    val intensityMinutes = calculateIntensityMinutesReadiness(
        weeklyIntensityMinutes = data.weeklyIntensityMinutes,
        hasIntensityData = DashboardMetric.INTENSITY_MINUTES in data.loadedMetrics,
    )
    val physiologicalStress = calculatePhysiologicalStress(data)

    data.weeklyCardioLoad?.let { load ->
        availableSignals += 1
        val ratio = if (load.targetScore > 0) {
            load.currentScore / load.targetScore.toDouble()
        } else {
            null
        }
        when {
            ratio == null -> Unit
            ratio > 1.35 -> {
                score -= 12
                bodyEnergyScore -= 8
                trainingReadinessScore -= 13
                elevatedBodySignals += 1
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_HIGH,
                    label = "Training load is high",
                    detail = "This week is ${(ratio * 100.0).roundToInt()}% of your current load target.",
                    impact = ReadinessFactorImpact.WARNING,
                )
            }
            ratio in 0.75..1.20 -> {
                score += 4
                trainingReadinessScore += 5
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_NORMAL,
                    label = "Training load is stable",
                    detail = "This week is ${(ratio * 100.0).roundToInt()}% of your current load target.",
                    impact = ReadinessFactorImpact.POSITIVE,
                )
            }
            else -> {
                addFactor(
                    kind = ReadinessFactorKind.TRAINING_LOAD_NORMAL,
                    label = "Training load is light",
                    detail = "This week is ${(ratio * 100.0).roundToInt()}% of your current load target.",
                    impact = ReadinessFactorImpact.NEUTRAL,
                )
            }
        }
    }

    if (DashboardMetric.INTENSITY_MINUTES in data.loadedMetrics) {
        if (data.weeklyIntensityMinutes != null &&
            data.weeklyIntensityMinutes.confidence != IntensityMinutesConfidence.NO_DATA
        ) {
            availableSignals += 1
            when (intensityMinutes.status) {
                IntensityMinutesStatus.GOAL_MET -> {
                    score += 3
                    trainingReadinessScore += 6
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET,
                        label = "Intensity minutes goal met",
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                IntensityMinutesStatus.ON_TRACK -> {
                    score += 2
                    trainingReadinessScore += 4
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_ON_TARGET,
                        label = "Intensity minutes on track",
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.POSITIVE,
                    )
                }
                IntensityMinutesStatus.BEHIND -> {
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_BEHIND,
                        label = "Intensity minutes behind pace",
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.NEUTRAL,
                    )
                }
                IntensityMinutesStatus.LOW -> {
                    trainingReadinessScore -= 2
                    addFactor(
                        kind = ReadinessFactorKind.INTENSITY_MINUTES_BEHIND,
                        label = "Intensity minutes are low",
                        detail = intensityMinutes.detail,
                        impact = ReadinessFactorImpact.NEUTRAL,
                    )
                }
                IntensityMinutesStatus.NEEDS_MORE_DATA -> Unit
            }
        } else {
            addFactor(
                kind = ReadinessFactorKind.MISSING_INTENSITY_MINUTES,
                label = "Intensity minutes need more data",
                detail = intensityMinutes.detail,
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }

    when (physiologicalStress.level) {
        PhysiologicalStressLevel.HIGH -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
                label = "Physiological stress: ${physiologicalStress.label}",
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.WARNING,
            )
        }
        PhysiologicalStressLevel.MEDIUM -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_HIGH,
                label = "Physiological stress: ${physiologicalStress.label}",
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.NEGATIVE,
            )
        }
        PhysiologicalStressLevel.RESTING,
        PhysiologicalStressLevel.LOW -> {
            addFactor(
                kind = ReadinessFactorKind.PHYSIOLOGICAL_STRESS_LOW,
                label = "Physiological stress: ${physiologicalStress.label}",
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.POSITIVE,
            )
        }
        PhysiologicalStressLevel.NEEDS_MORE_DATA -> {
            addFactor(
                kind = ReadinessFactorKind.MISSING_STRESS_DATA,
                label = "Physiological stress needs more data",
                detail = physiologicalStress.summary,
                impact = ReadinessFactorImpact.NEUTRAL,
            )
        }
    }

    val bodyTemperature = data.latestBodyTemperatureCelsius
    val skinDelta = data.latestSkinTemperatureDeltaCelsius
    if (
        (DashboardMetric.BODY_TEMPERATURE in data.loadedMetrics && bodyTemperature != null) ||
        (DashboardMetric.SKIN_TEMPERATURE in data.loadedMetrics && skinDelta != null)
    ) {
        availableSignals += 1
        val tempWarning = (bodyTemperature != null && bodyTemperature >= 37.7) ||
            (skinDelta != null && skinDelta >= 1.0)
        val tempElevated = tempWarning ||
            (bodyTemperature != null && bodyTemperature >= 37.2) ||
            (skinDelta != null && skinDelta >= 0.5)
        if (tempElevated) {
            val detail = buildList {
                bodyTemperature?.let { add("body temperature ${formatOneDecimal(it)} C") }
                skinDelta?.let { add("skin temperature ${formatSignedOneDecimal(it)} C") }
            }.joinToString(separator = ", ")
            score -= if (tempWarning) 20 else 10
            bodyEnergyScore -= if (tempWarning) 18 else 8
            trainingReadinessScore -= if (tempWarning) 18 else 9
            elevatedBodySignals += 1
            unusualVitals = tempWarning
            addFactor(
                kind = ReadinessFactorKind.TEMPERATURE_ELEVATED,
                label = "Temperature signal elevated",
                detail = "Some temperature signals look elevated ($detail). If you feel unwell, consider resting.",
                impact = if (tempWarning) ReadinessFactorImpact.WARNING else ReadinessFactorImpact.NEGATIVE,
            )
        }
    }

    if (DashboardMetric.HYDRATION in data.loadedMetrics && goals.hydrationLitersGoal > 0.0) {
        availableSignals += 1
        val hydrationRatio = data.hydrationLiters / goals.hydrationLitersGoal
        if (hydrationRatio < 0.35) {
            score -= 4
            bodyEnergyScore -= 4
            addFactor(
                kind = ReadinessFactorKind.HYDRATION_LOW,
                label = "Hydration is behind",
                detail = "Hydration is ${(hydrationRatio * 100.0).roundToInt()}% of today's goal.",
                impact = ReadinessFactorImpact.NEGATIVE,
            )
        }
    }

    val nutritionLogged = data.caloriesInKcal != null ||
        data.proteinGrams != null ||
        data.carbsGrams != null ||
        data.fatGrams != null
    if (nutritionLogged) {
        availableSignals += 1
        bodyEnergyScore += 2
        addFactor(
            kind = ReadinessFactorKind.NUTRITION_LOGGED,
            label = "Nutrition is logged",
            detail = "Meal data is available for today's energy context.",
            impact = ReadinessFactorImpact.POSITIVE,
        )
    }

    if ((data.mindfulnessMinutes ?: 0) >= 5) {
        availableSignals += 1
        score += 2
        bodyEnergyScore += 3
        addFactor(
            kind = ReadinessFactorKind.STRESS_LOW,
            label = "Recovery moment recorded",
            detail = "${data.mindfulnessMinutes} min of mindfulness is logged today.",
            impact = ReadinessFactorImpact.POSITIVE,
        )
    }

    if (elevatedBodySignals >= 2) {
        score -= 6
        bodyEnergyScore -= 6
        trainingReadinessScore -= 8
        addFactor(
            kind = ReadinessFactorKind.STRESS_HIGH,
            label = "Body signals look elevated",
            detail = "Several recovery signals are outside your usual range.",
            impact = ReadinessFactorImpact.WARNING,
        )
    }

    val clampedScore = if (availableSignals == 0) 0 else score.coerceIn(0, 100)
    val state = when {
        availableSignals == 0 -> ReadinessState.UNKNOWN
        unusualVitals && clampedScore < 55 -> ReadinessState.REST
        clampedScore >= 80 -> ReadinessState.READY
        clampedScore >= 60 -> ReadinessState.MODERATE
        clampedScore >= 40 -> ReadinessState.RECOVER
        else -> ReadinessState.REST
    }
    val recommendationType = when {
        unusualVitals && state == ReadinessState.REST -> ReadinessRecommendationType.CHECK_SYMPTOMS
        state == ReadinessState.READY -> ReadinessRecommendationType.HARD_TRAINING
        state == ReadinessState.MODERATE -> ReadinessRecommendationType.MODERATE_TRAINING
        state == ReadinessState.RECOVER -> ReadinessRecommendationType.LIGHT_ACTIVITY
        state == ReadinessState.REST -> ReadinessRecommendationType.REST
        else -> ReadinessRecommendationType.MOBILITY
    }
    val confidence = when {
        availableSignals >= 5 && baselineSignals >= 2 && missingReasons.isEmpty() -> ReadinessConfidence.HIGH
        availableSignals >= 3 && baselineSignals >= 1 -> ReadinessConfidence.MEDIUM
        else -> ReadinessConfidence.LOW
    }
    val confidenceReason = when {
        confidence == ReadinessConfidence.HIGH -> "complete_data"
        "missing_sleep_data" in missingReasons -> "missing_sleep_data"
        "missing_hrv_data" in missingReasons -> "missing_hrv_data"
        "new_user_not_enough_baseline" in missingReasons -> "new_user_not_enough_baseline"
        else -> "partial_data"
    }

    return DailyReadinessInsight(
        state = state,
        score = clampedScore,
        bodyEnergyScore = if (availableSignals == 0) 0 else bodyEnergyScore.coerceIn(0, 100),
        trainingReadinessScore = if (availableSignals == 0) 0 else trainingReadinessScore.coerceIn(0, 100),
        recommendationType = recommendationType,
        statusTitle = statusTitleFor(state),
        recommendation = recommendationFor(state, recommendationType),
        explanation = explanationFor(state, factors),
        alternative = alternativeFor(state),
        suggestedWorkout = suggestedWorkoutFor(state),
        avoid = avoidFor(state),
        strainTarget = strainTargetFor(state),
        currentStrain = currentStrainFor(data),
        adaptiveGoal = adaptiveGoalFor(state, goals),
        confidence = confidence,
        confidenceReason = confidenceReason,
        hrvStatus = hrvStatus,
        intensityMinutes = intensityMinutes,
        physiologicalStress = physiologicalStress,
        factors = factors.sortedWith(compareByDescending<DailyReadinessFactor> { it.impact.priority }.thenBy { it.label }),
        recoveryModeSuggested = state == ReadinessState.REST || clampedScore < 35,
    )
}

private val ReadinessFactorImpact.priority: Int
    get() = when (this) {
        ReadinessFactorImpact.WARNING -> 3
        ReadinessFactorImpact.NEGATIVE -> 2
        ReadinessFactorImpact.POSITIVE -> 1
        ReadinessFactorImpact.NEUTRAL -> 0
    }

private fun statusTitleFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> "Ready to train"
        ReadinessState.MODERATE -> "Train, but keep it controlled"
        ReadinessState.RECOVER -> "Recovery day"
        ReadinessState.REST -> "Take it easy"
        ReadinessState.UNKNOWN -> "Needs more data"
    }

private fun recommendationFor(
    state: ReadinessState,
    type: ReadinessRecommendationType,
): String =
    when (type) {
        ReadinessRecommendationType.HARD_TRAINING -> "Good day for hard training if you feel normal."
        ReadinessRecommendationType.MODERATE_TRAINING -> "Do moderate training today, but avoid maximal effort."
        ReadinessRecommendationType.LIGHT_ACTIVITY -> "Keep it light today with easy movement or mobility."
        ReadinessRecommendationType.REST -> "Avoid intense training today and focus on recovery."
        ReadinessRecommendationType.CHECK_SYMPTOMS -> {
            "Some signals are outside your usual range. If you feel unwell, prioritize rest and hydration."
        }
        ReadinessRecommendationType.MOBILITY -> when (state) {
            ReadinessState.UNKNOWN -> "Connect sleep, HRV, resting heart rate, and workouts for a better guide."
            else -> "Choose mobility or an easy walk until more data is available."
        }
    }

private fun explanationFor(
    state: ReadinessState,
    factors: List<DailyReadinessFactor>,
): String {
    val meaningful = factors
        .filterNot {
            it.kind == ReadinessFactorKind.MISSING_SLEEP_DATA ||
                it.kind == ReadinessFactorKind.MISSING_HRV_DATA ||
                it.kind == ReadinessFactorKind.MISSING_INTENSITY_MINUTES ||
                it.kind == ReadinessFactorKind.MISSING_STRESS_DATA ||
                it.kind == ReadinessFactorKind.NEW_USER_NOT_ENOUGH_BASELINE
        }
        .sortedByDescending { it.impact.priority }
        .take(3)
    if (meaningful.isEmpty()) {
        return if (state == ReadinessState.UNKNOWN) {
            "There is not enough local data yet to explain readiness with confidence."
        } else {
            "Your recommendation is based on the local signals available today."
        }
    }
    return "Your signals suggest this mainly because " +
        meaningful.joinToNaturalText { it.detail.replaceFirstChar { char -> char.lowercase(Locale.US) } }
}

private fun alternativeFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> "If you feel sore, reduce the plan to 30-45 min easy cardio or mobility."
        ReadinessState.MODERATE -> "If you feel tired, choose a 30 min easy walk or easy ride instead."
        ReadinessState.RECOVER -> "If you still want movement, keep it conversational and stop if it feels harder than expected."
        ReadinessState.REST -> "If you feel unwell, rest or seek medical advice when needed."
        ReadinessState.UNKNOWN -> "Start with a walk, mobility, or breathing session until more recovery data is available."
    }

private fun suggestedWorkoutFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> "Strength training, intervals, a long bike ride, or your normal planned workout."
        ReadinessState.MODERATE -> "Zone 2 cardio, moderate strength, technique work, or an easy bike ride."
        ReadinessState.RECOVER -> "Walk, stretching, mobility, breathing, or an early night."
        ReadinessState.REST -> "Rest, gentle movement only, and symptom monitoring if you feel unwell."
        ReadinessState.UNKNOWN -> "Easy walk, mobility, or light stretching."
    }

private fun avoidFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> "Overreaching if you feel sore."
        ReadinessState.MODERATE -> "Max effort, HIIT, and very long sessions."
        ReadinessState.RECOVER -> "Hard cardio, heavy lifting, and long intense sessions."
        ReadinessState.REST -> "Intense training today."
        ReadinessState.UNKNOWN -> "Making a hard training decision from incomplete data."
    }

private fun strainTargetFor(state: ReadinessState): String =
    when (state) {
        ReadinessState.READY -> "Today's strain target: 10-14"
        ReadinessState.MODERATE -> "Today's strain target: 7-10"
        ReadinessState.RECOVER -> "Today's strain target: 3-6"
        ReadinessState.REST -> "Today's strain target: 0-3"
        ReadinessState.UNKNOWN -> "Today's strain target: 3-6"
    }

private fun currentStrainFor(data: DashboardData): String? {
    val load = data.weeklyCardioLoad ?: return null
    if (load.targetScore <= 0) return null
    val dailyTarget = (load.targetScore / 7.0).coerceAtLeast(1.0)
    val strain = (load.todayScore / dailyTarget * 7.0).coerceIn(0.0, 15.0)
    return "Current strain: ${formatOneDecimal(strain)}"
}

private fun adaptiveGoalFor(
    state: ReadinessState,
    goals: DailyReadinessGoalInputs,
): String {
    val normalSteps = goals.stepsGoal.roundToNearestHundred()
    return when (state) {
        ReadinessState.READY -> {
            "Adaptive goal: ${(goals.stepsGoal * 1.1).roundToNearestHundred()} steps + workout"
        }
        ReadinessState.MODERATE -> {
            "Adaptive goal: $normalSteps steps + ${goals.activeMinutesGoal.roundToInt()} active minutes"
        }
        ReadinessState.RECOVER -> {
            "Adaptive goal: ${(goals.stepsGoal * 0.5).roundToNearestHundred()} steps + 15 min mobility"
        }
        ReadinessState.REST -> "Adaptive goal: goals are reduced; focus on rest and hydration"
        ReadinessState.UNKNOWN -> "Adaptive goal: keep movement light until more data is available"
    }
}

private fun Double.roundToNearestHundred(): Int =
    ((this / 100.0).roundToInt() * 100).coerceAtLeast(0)

private fun formatHours(hours: Double): String {
    val totalMinutes = (hours * 60.0).roundToInt().coerceAtLeast(0)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}h ${m.toString().padStart(2, '0')}m"
}

private fun formatOneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)

private fun formatSignedOneDecimal(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return prefix + formatOneDecimal(value)
}

private fun <T> List<T>.joinToNaturalText(transform: (T) -> String): String =
    when (size) {
        0 -> ""
        1 -> transform(first())
        2 -> "${transform(this[0])} and ${transform(this[1])}"
        else -> dropLast(1).joinToString(separator = ", ", transform = transform) + ", and " + transform(last())
    }
