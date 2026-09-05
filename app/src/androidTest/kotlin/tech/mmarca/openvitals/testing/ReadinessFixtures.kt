package tech.mmarca.openvitals.testing

import tech.mmarca.openvitals.domain.insights.DailyReadinessFactor
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.HrvStatus
import tech.mmarca.openvitals.domain.insights.HrvStatusInsight
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesReadinessInsight
import tech.mmarca.openvitals.domain.insights.IntensityMinutesStatus
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressConfidence
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressEstimate
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressLevel
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence
import tech.mmarca.openvitals.domain.insights.ReadinessFactorImpact
import tech.mmarca.openvitals.domain.insights.ReadinessFactorKind
import tech.mmarca.openvitals.domain.insights.ReadinessRecommendationType
import tech.mmarca.openvitals.domain.insights.ReadinessState

/**
 * A whole, plausible readiness insight, so a test can name the one field it cares about.
 * The defaults are an unremarkable day: moderate readiness, balanced HRV, complete data.
 */
fun readinessInsight(
    state: ReadinessState = ReadinessState.MODERATE,
    score: Int = 68,
    bodyEnergyScore: Int = 62,
    trainingReadinessScore: Int = 71,
    confidence: ReadinessConfidence = ReadinessConfidence.HIGH,
    confidenceReason: String = "complete_data",
    factors: List<DailyReadinessFactor> = listOf(readinessFactor()),
    statusTitle: String = "Moderate",
    recommendation: String = "A steady session is well within reach today.",
    currentStrain: String? = "Strain 8.2",
): DailyReadinessInsight = DailyReadinessInsight(
    state = state,
    score = score,
    bodyEnergyScore = bodyEnergyScore,
    trainingReadinessScore = trainingReadinessScore,
    recommendationType = ReadinessRecommendationType.MODERATE_TRAINING,
    statusTitle = statusTitle,
    recommendation = recommendation,
    explanation = "Sleep and HRV both sat close to your baseline.",
    alternative = "An easy walk if you would rather hold back.",
    suggestedWorkout = "45 min endurance ride",
    avoid = "Maximal intervals",
    strainTarget = "Strain 9-11",
    currentStrain = currentStrain,
    adaptiveGoal = "8,000 steps",
    confidence = confidence,
    confidenceReason = confidenceReason,
    hrvStatus = HrvStatusInsight(
        status = HrvStatus.BALANCED,
        label = "Balanced",
        detail = "Within your usual range.",
        currentRmssdMs = 48.0,
        baselineRmssdMs = 46.0,
        percentFromBaseline = 4,
    ),
    intensityMinutes = IntensityMinutesReadinessInsight(
        status = IntensityMinutesStatus.entries.first(),
        label = "On target",
        detail = "You are on pace for the week.",
        moderateEquivalentMinutes = 120,
        targetMinutes = 150,
        todayModerateEquivalentMinutes = 20,
        progressPercent = 80,
        confidence = IntensityMinutesConfidence.HIGH,
    ),
    physiologicalStress = PhysiologicalStressEstimate(
        level = PhysiologicalStressLevel.LOW,
        label = "Low",
        score = 28,
        summary = "Your body looks settled.",
        detail = "HRV and resting heart rate are both near baseline.",
        confidence = PhysiologicalStressConfidence.HIGH,
        confidenceReason = "complete_data",
        hrvPercentFromBaseline = 4,
        restingHeartRateDeltaBpm = -1,
        averageHeartRateDeltaFromRestingBpm = 12,
        hasWorkoutInfluence = false,
        contributingFactors = emptyList(),
        dataCoverage = emptyList(),
        caveats = emptyList(),
    ),
    factors = factors,
    recoveryModeSuggested = false,
)

fun readinessFactor(
    kind: ReadinessFactorKind = ReadinessFactorKind.HRV_NORMAL,
    label: String = "HRV",
    detail: String = "Within your usual range",
    impact: ReadinessFactorImpact = ReadinessFactorImpact.NEUTRAL,
): DailyReadinessFactor = DailyReadinessFactor(
    kind = kind,
    label = label,
    detail = detail,
    impact = impact,
)
