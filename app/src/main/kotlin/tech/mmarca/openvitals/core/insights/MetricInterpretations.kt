package tech.mmarca.openvitals.core.insights

import kotlin.math.max

enum class InterpretationSeverity {
    POSITIVE,
    INFO,
    CAUTION,
    ALERT,
}

enum class BloodPressureCategory {
    NORMAL,
    ELEVATED,
    STAGE_1,
    STAGE_2,
    SEVERE_REFERENCE,
}

data class BloodPressureInterpretation(
    val category: BloodPressureCategory,
    val severity: InterpretationSeverity,
)

enum class BmiCategory {
    UNDERWEIGHT,
    HEALTHY,
    OVERWEIGHT,
    OBESITY_CLASS_1,
    OBESITY_CLASS_2,
    OBESITY_CLASS_3,
}

data class BmiInterpretation(
    val category: BmiCategory,
    val severity: InterpretationSeverity,
)

enum class SleepTargetStatus {
    BELOW_TARGET,
    NEAR_TARGET,
    MET_TARGET,
}

data class SleepTargetInterpretation(
    val status: SleepTargetStatus,
    val averageHours: Double,
    val targetHours: Double,
    val gapHours: Double,
    val severity: InterpretationSeverity,
)

enum class MacroRangeStatus {
    LOW,
    WITHIN,
    HIGH,
}

data class MacroSplitInterpretation(
    val proteinPercent: Double,
    val carbsPercent: Double,
    val fatPercent: Double,
    val proteinStatus: MacroRangeStatus,
    val carbsStatus: MacroRangeStatus,
    val fatStatus: MacroRangeStatus,
    val severity: InterpretationSeverity,
) {
    val isWithinReference: Boolean =
        proteinStatus == MacroRangeStatus.WITHIN &&
            carbsStatus == MacroRangeStatus.WITHIN &&
            fatStatus == MacroRangeStatus.WITHIN
}

enum class WorkoutGuidelineStatus {
    NO_LOGGED_MINUTES,
    BELOW_REFERENCE,
    APPROACHING_REFERENCE,
    MEETS_REFERENCE,
}

data class WorkoutGuidelineProgress(
    val loggedMinutes: Double,
    val referenceMinutes: Double,
    val percentOfReference: Double,
    val status: WorkoutGuidelineStatus,
    val severity: InterpretationSeverity,
)

enum class VitalContextStatus {
    WITHIN_REFERENCE,
    BELOW_REFERENCE,
    ABOVE_REFERENCE,
    BELOW_TYPICAL_OXYGEN,
    LOW_OXYGEN_REFERENCE,
    VERY_LOW_OXYGEN_REFERENCE,
}

data class VitalContextInterpretation(
    val status: VitalContextStatus,
    val severity: InterpretationSeverity,
)

fun bloodPressureInterpretation(
    systolicMmHg: Int,
    diastolicMmHg: Int,
): BloodPressureInterpretation? {
    if (systolicMmHg <= 0 || diastolicMmHg <= 0) return null
    val category = when {
        systolicMmHg > 180 || diastolicMmHg > 120 -> BloodPressureCategory.SEVERE_REFERENCE
        systolicMmHg >= 140 || diastolicMmHg >= 90 -> BloodPressureCategory.STAGE_2
        systolicMmHg >= 130 || diastolicMmHg >= 80 -> BloodPressureCategory.STAGE_1
        systolicMmHg >= 120 && diastolicMmHg < 80 -> BloodPressureCategory.ELEVATED
        else -> BloodPressureCategory.NORMAL
    }
    return BloodPressureInterpretation(
        category = category,
        severity = when (category) {
            BloodPressureCategory.NORMAL -> InterpretationSeverity.POSITIVE
            BloodPressureCategory.ELEVATED -> InterpretationSeverity.INFO
            BloodPressureCategory.STAGE_1,
            BloodPressureCategory.STAGE_2 -> InterpretationSeverity.CAUTION
            BloodPressureCategory.SEVERE_REFERENCE -> InterpretationSeverity.ALERT
        },
    )
}

fun bmiInterpretation(bmi: Double): BmiInterpretation? {
    if (bmi <= 0.0) return null
    val category = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.HEALTHY
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        bmi < 35.0 -> BmiCategory.OBESITY_CLASS_1
        bmi < 40.0 -> BmiCategory.OBESITY_CLASS_2
        else -> BmiCategory.OBESITY_CLASS_3
    }
    return BmiInterpretation(
        category = category,
        severity = when (category) {
            BmiCategory.HEALTHY -> InterpretationSeverity.POSITIVE
            BmiCategory.UNDERWEIGHT,
            BmiCategory.OVERWEIGHT -> InterpretationSeverity.INFO
            BmiCategory.OBESITY_CLASS_1,
            BmiCategory.OBESITY_CLASS_2,
            BmiCategory.OBESITY_CLASS_3 -> InterpretationSeverity.CAUTION
        },
    )
}

fun sleepTargetInterpretation(
    averageHours: Double,
    targetHours: Double,
): SleepTargetInterpretation? {
    if (averageHours <= 0.0 || targetHours <= 0.0) return null
    val gap = targetHours - averageHours
    val status = when {
        gap <= 0.0 -> SleepTargetStatus.MET_TARGET
        gap <= 0.5 -> SleepTargetStatus.NEAR_TARGET
        else -> SleepTargetStatus.BELOW_TARGET
    }
    return SleepTargetInterpretation(
        status = status,
        averageHours = averageHours,
        targetHours = targetHours,
        gapHours = gap.coerceAtLeast(0.0),
        severity = when (status) {
            SleepTargetStatus.MET_TARGET -> InterpretationSeverity.POSITIVE
            SleepTargetStatus.NEAR_TARGET -> InterpretationSeverity.INFO
            SleepTargetStatus.BELOW_TARGET -> InterpretationSeverity.CAUTION
        },
    )
}

fun macroSplitInterpretation(
    proteinGrams: Double,
    carbsGrams: Double,
    fatGrams: Double,
): MacroSplitInterpretation? {
    val proteinKcal = max(0.0, proteinGrams) * 4.0
    val carbsKcal = max(0.0, carbsGrams) * 4.0
    val fatKcal = max(0.0, fatGrams) * 9.0
    val totalMacroKcal = proteinKcal + carbsKcal + fatKcal
    if (totalMacroKcal <= 0.0) return null

    val proteinPercent = proteinKcal / totalMacroKcal * 100.0
    val carbsPercent = carbsKcal / totalMacroKcal * 100.0
    val fatPercent = fatKcal / totalMacroKcal * 100.0
    val proteinStatus = rangeStatus(proteinPercent, 10.0, 35.0)
    val carbsStatus = rangeStatus(carbsPercent, 45.0, 65.0)
    val fatStatus = rangeStatus(fatPercent, 20.0, 35.0)

    return MacroSplitInterpretation(
        proteinPercent = proteinPercent,
        carbsPercent = carbsPercent,
        fatPercent = fatPercent,
        proteinStatus = proteinStatus,
        carbsStatus = carbsStatus,
        fatStatus = fatStatus,
        severity = if (
            proteinStatus == MacroRangeStatus.WITHIN &&
            carbsStatus == MacroRangeStatus.WITHIN &&
            fatStatus == MacroRangeStatus.WITHIN
        ) {
            InterpretationSeverity.POSITIVE
        } else {
            InterpretationSeverity.INFO
        },
    )
}

fun workoutGuidelineProgress(
    loggedMinutes: Double,
    referenceMinutes: Double = 150.0,
): WorkoutGuidelineProgress? {
    if (referenceMinutes <= 0.0) return null
    val safeMinutes = loggedMinutes.coerceAtLeast(0.0)
    val percent = safeMinutes / referenceMinutes * 100.0
    val status = when {
        safeMinutes == 0.0 -> WorkoutGuidelineStatus.NO_LOGGED_MINUTES
        percent >= 100.0 -> WorkoutGuidelineStatus.MEETS_REFERENCE
        percent >= 75.0 -> WorkoutGuidelineStatus.APPROACHING_REFERENCE
        else -> WorkoutGuidelineStatus.BELOW_REFERENCE
    }
    return WorkoutGuidelineProgress(
        loggedMinutes = safeMinutes,
        referenceMinutes = referenceMinutes,
        percentOfReference = percent,
        status = status,
        severity = when (status) {
            WorkoutGuidelineStatus.MEETS_REFERENCE -> InterpretationSeverity.POSITIVE
            WorkoutGuidelineStatus.APPROACHING_REFERENCE -> InterpretationSeverity.INFO
            WorkoutGuidelineStatus.NO_LOGGED_MINUTES,
            WorkoutGuidelineStatus.BELOW_REFERENCE -> InterpretationSeverity.CAUTION
        },
    )
}

fun restingHeartRateContext(bpm: Long): VitalContextInterpretation? {
    if (bpm <= 0L) return null
    return referenceRangeContext(bpm.toDouble(), 60.0, 100.0)
}

fun respiratoryRateContext(breathsPerMinute: Double): VitalContextInterpretation? =
    if (breathsPerMinute <= 0.0) {
        null
    } else {
        referenceRangeContext(breathsPerMinute, 12.0, 18.0)
    }

fun bodyTemperatureContext(celsius: Double): VitalContextInterpretation? =
    if (celsius <= 0.0) {
        null
    } else {
        referenceRangeContext(celsius, 36.1, 37.2)
    }

fun oxygenSaturationContext(percent: Double): VitalContextInterpretation? {
    if (percent <= 0.0) return null
    val status = when {
        percent <= 88.0 -> VitalContextStatus.VERY_LOW_OXYGEN_REFERENCE
        percent <= 92.0 -> VitalContextStatus.LOW_OXYGEN_REFERENCE
        percent < 95.0 -> VitalContextStatus.BELOW_TYPICAL_OXYGEN
        else -> VitalContextStatus.WITHIN_REFERENCE
    }
    return VitalContextInterpretation(
        status = status,
        severity = when (status) {
            VitalContextStatus.WITHIN_REFERENCE -> InterpretationSeverity.POSITIVE
            VitalContextStatus.BELOW_TYPICAL_OXYGEN -> InterpretationSeverity.INFO
            VitalContextStatus.LOW_OXYGEN_REFERENCE -> InterpretationSeverity.CAUTION
            VitalContextStatus.VERY_LOW_OXYGEN_REFERENCE -> InterpretationSeverity.ALERT
            VitalContextStatus.BELOW_REFERENCE,
            VitalContextStatus.ABOVE_REFERENCE -> InterpretationSeverity.CAUTION
        },
    )
}

private fun referenceRangeContext(
    value: Double,
    lowInclusive: Double,
    highInclusive: Double,
): VitalContextInterpretation =
    when {
        value < lowInclusive -> VitalContextInterpretation(
            status = VitalContextStatus.BELOW_REFERENCE,
            severity = InterpretationSeverity.CAUTION,
        )
        value > highInclusive -> VitalContextInterpretation(
            status = VitalContextStatus.ABOVE_REFERENCE,
            severity = InterpretationSeverity.CAUTION,
        )
        else -> VitalContextInterpretation(
            status = VitalContextStatus.WITHIN_REFERENCE,
            severity = InterpretationSeverity.POSITIVE,
        )
    }

private fun rangeStatus(
    value: Double,
    lowInclusive: Double,
    highInclusive: Double,
): MacroRangeStatus =
    when {
        value < lowInclusive - PercentageEpsilon -> MacroRangeStatus.LOW
        value > highInclusive + PercentageEpsilon -> MacroRangeStatus.HIGH
        else -> MacroRangeStatus.WITHIN
    }

private const val PercentageEpsilon = 0.0001
