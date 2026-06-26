package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricInterpretationsTest {

    @Test
    fun classifiesBloodPressureUsingHighestApplicableCategory() {
        assertEquals(
            BloodPressureCategory.NORMAL,
            bloodPressureInterpretation(118, 76)?.category,
        )
        assertEquals(
            BloodPressureCategory.ELEVATED,
            bloodPressureInterpretation(124, 78)?.category,
        )
        assertEquals(
            BloodPressureCategory.STAGE_1,
            bloodPressureInterpretation(128, 84)?.category,
        )
        assertEquals(
            BloodPressureCategory.STAGE_2,
            bloodPressureInterpretation(142, 82)?.category,
        )
        assertEquals(
            BloodPressureCategory.SEVERE_REFERENCE,
            bloodPressureInterpretation(181, 82)?.category,
        )
    }

    @Test
    fun classifiesAdultBmiBoundaries() {
        assertEquals(BmiCategory.UNDERWEIGHT, bmiInterpretation(18.4)?.category)
        assertEquals(BmiCategory.HEALTHY, bmiInterpretation(18.5)?.category)
        assertEquals(BmiCategory.OVERWEIGHT, bmiInterpretation(25.0)?.category)
        assertEquals(BmiCategory.OBESITY_CLASS_1, bmiInterpretation(30.0)?.category)
        assertEquals(BmiCategory.OBESITY_CLASS_2, bmiInterpretation(35.0)?.category)
        assertEquals(BmiCategory.OBESITY_CLASS_3, bmiInterpretation(40.0)?.category)
    }

    @Test
    fun classifiesAdjustedFfmiBoundaries() {
        assertEquals(FfmiCategory.BELOW_AVERAGE, ffmiInterpretation(17.9)?.category)
        assertEquals(FfmiCategory.AVERAGE, ffmiInterpretation(18.0)?.category)
        assertEquals(FfmiCategory.ABOVE_AVERAGE, ffmiInterpretation(20.0)?.category)
        assertEquals(FfmiCategory.EXCELLENT, ffmiInterpretation(22.0)?.category)
        assertEquals(FfmiCategory.SUPERIOR, ffmiInterpretation(24.0)?.category)
        assertEquals(FfmiCategory.EXCEPTIONAL, ffmiInterpretation(26.0)?.category)
        assertEquals(FfmiCategory.ELITE, ffmiInterpretation(28.0)?.category)
    }

    @Test
    fun interpretsSleepAgainstUserTarget() {
        assertEquals(
            SleepTargetStatus.BELOW_TARGET,
            sleepTargetInterpretation(6.0, 7.5)?.status,
        )
        assertEquals(
            SleepTargetStatus.NEAR_TARGET,
            sleepTargetInterpretation(7.1, 7.5)?.status,
        )
        assertEquals(
            SleepTargetStatus.MET_TARGET,
            sleepTargetInterpretation(7.5, 7.5)?.status,
        )
    }

    @Test
    fun calculatesMacroSplitFromLoggedMacroCalories() {
        val split = checkNotNull(
            macroSplitInterpretation(
                proteinGrams = 75.0,
                carbsGrams = 250.0,
                fatGrams = 77.7778,
            ),
        )

        assertEquals(15.0, split.proteinPercent, 0.01)
        assertEquals(50.0, split.carbsPercent, 0.01)
        assertEquals(35.0, split.fatPercent, 0.01)
        assertTrue(split.isWithinReference)
    }

    @Test
    fun flagsMacroSplitOutsideReferenceWithoutRejectingData() {
        val split = checkNotNull(
            macroSplitInterpretation(
                proteinGrams = 200.0,
                carbsGrams = 20.0,
                fatGrams = 10.0,
            ),
        )

        assertFalse(split.isWithinReference)
        assertEquals(MacroRangeStatus.HIGH, split.proteinStatus)
        assertEquals(MacroRangeStatus.LOW, split.carbsStatus)
    }

    @Test
    fun interpretsWorkoutProgressAgainstWeeklyReference() {
        assertEquals(
            WorkoutGuidelineStatus.NO_LOGGED_MINUTES,
            workoutGuidelineProgress(0.0)?.status,
        )
        assertEquals(
            WorkoutGuidelineStatus.APPROACHING_REFERENCE,
            workoutGuidelineProgress(120.0)?.status,
        )
        assertEquals(
            WorkoutGuidelineStatus.MEETS_REFERENCE,
            workoutGuidelineProgress(150.0)?.status,
        )
    }

    @Test
    fun interpretsVitalsWithBroadAdultReferenceRanges() {
        assertEquals(VitalContextStatus.WITHIN_REFERENCE, restingHeartRateContext(60)?.status)
        assertEquals(VitalContextStatus.BELOW_REFERENCE, restingHeartRateContext(50)?.status)
        assertEquals(VitalContextStatus.WITHIN_REFERENCE, respiratoryRateContext(12.0)?.status)
        assertEquals(VitalContextStatus.ABOVE_REFERENCE, respiratoryRateContext(20.0)?.status)
        assertEquals(VitalContextStatus.WITHIN_REFERENCE, bodyTemperatureContext(36.8)?.status)
        assertEquals(VitalContextStatus.ABOVE_REFERENCE, bodyTemperatureContext(38.0)?.status)
    }

    @Test
    fun interpretsOxygenSaturationSeparatelyFromSimpleReferenceRanges() {
        assertEquals(VitalContextStatus.WITHIN_REFERENCE, oxygenSaturationContext(97.0)?.status)
        assertEquals(VitalContextStatus.BELOW_TYPICAL_OXYGEN, oxygenSaturationContext(94.0)?.status)
        assertEquals(VitalContextStatus.LOW_OXYGEN_REFERENCE, oxygenSaturationContext(92.0)?.status)
        assertEquals(VitalContextStatus.VERY_LOW_OXYGEN_REFERENCE, oxygenSaturationContext(88.0)?.status)
    }

    @Test
    fun returnsNullForInvalidInputs() {
        assertNull(bloodPressureInterpretation(0, 80))
        assertNull(bmiInterpretation(0.0))
        assertNull(ffmiInterpretation(0.0))
        assertNull(sleepTargetInterpretation(0.0, 8.0))
        assertNull(macroSplitInterpretation(0.0, 0.0, 0.0))
        assertNull(workoutGuidelineProgress(10.0, 0.0))
        assertNull(restingHeartRateContext(0))
    }
}
