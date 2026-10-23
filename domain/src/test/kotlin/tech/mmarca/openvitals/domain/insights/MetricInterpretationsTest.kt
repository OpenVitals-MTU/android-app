package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BloodPressureGuideline

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
    fun accAhaBoundariesMatchTheGuideline() {
        val guideline = BloodPressureGuideline.ACC_AHA_2017
        assertEquals(BloodPressureCategory.NORMAL, category(119, 79, guideline))
        assertEquals(BloodPressureCategory.ELEVATED, category(120, 79, guideline))
        assertEquals(BloodPressureCategory.STAGE_1, category(119, 80, guideline))
        assertEquals(BloodPressureCategory.STAGE_1, category(130, 70, guideline))
        assertEquals(BloodPressureCategory.STAGE_2, category(140, 70, guideline))
        assertEquals(BloodPressureCategory.STAGE_2, category(180, 120, guideline))
        assertEquals(BloodPressureCategory.SEVERE_REFERENCE, category(120, 121, guideline))
    }

    @Test
    fun eshGradesStartAt140Over90() {
        val guideline = BloodPressureGuideline.ESH_2023
        assertEquals(BloodPressureCategory.OPTIMAL, category(119, 79, guideline))
        assertEquals(BloodPressureCategory.NORMAL, category(120, 70, guideline))
        assertEquals(BloodPressureCategory.NORMAL, category(110, 84, guideline))
        assertEquals(BloodPressureCategory.HIGH_NORMAL, category(135, 82, guideline))
        assertEquals(BloodPressureCategory.HIGH_NORMAL, category(110, 85, guideline))
        assertEquals(BloodPressureCategory.GRADE_1, category(140, 70, guideline))
        assertEquals(BloodPressureCategory.GRADE_1, category(110, 90, guideline))
        assertEquals(BloodPressureCategory.GRADE_2, category(160, 70, guideline))
        assertEquals(BloodPressureCategory.GRADE_2, category(110, 100, guideline))
        assertEquals(BloodPressureCategory.GRADE_3, category(180, 70, guideline))
        assertEquals(BloodPressureCategory.GRADE_3, category(110, 110, guideline))
    }

    @Test
    fun escUsesThreeCategoriesPlusTheSevereReference() {
        val guideline = BloodPressureGuideline.ESC_2024
        assertEquals(BloodPressureCategory.NON_ELEVATED, category(119, 69, guideline))
        assertEquals(BloodPressureCategory.ELEVATED, category(110, 70, guideline))
        assertEquals(BloodPressureCategory.ELEVATED, category(139, 89, guideline))
        assertEquals(BloodPressureCategory.HYPERTENSION, category(140, 60, guideline))
        assertEquals(BloodPressureCategory.HYPERTENSION, category(110, 90, guideline))
        assertEquals(BloodPressureCategory.SEVERE_REFERENCE, category(181, 60, guideline))
    }

    @Test
    fun ishTreatsBelow130Over85AsNormal() {
        val guideline = BloodPressureGuideline.ISH_2020
        assertEquals(BloodPressureCategory.NORMAL, category(129, 84, guideline))
        assertEquals(BloodPressureCategory.HIGH_NORMAL, category(130, 70, guideline))
        assertEquals(BloodPressureCategory.HIGH_NORMAL, category(110, 85, guideline))
        assertEquals(BloodPressureCategory.GRADE_1, category(159, 99, guideline))
        assertEquals(BloodPressureCategory.GRADE_2, category(160, 70, guideline))
        assertEquals(BloodPressureCategory.GRADE_2, category(110, 100, guideline))
        assertEquals(BloodPressureCategory.SEVERE_REFERENCE, category(150, 121, guideline))
    }

    @Test
    fun theSameReadingChangesNameAcrossGuidelines() {
        // 134/82 is the case behind the setting: high in the US, not in Europe.
        assertEquals(InterpretationSeverity.CAUTION, severity(134, 82, BloodPressureGuideline.ACC_AHA_2017))
        assertEquals(InterpretationSeverity.INFO, severity(134, 82, BloodPressureGuideline.ESH_2023))
        assertEquals(InterpretationSeverity.INFO, severity(134, 82, BloodPressureGuideline.ESC_2024))
        assertEquals(InterpretationSeverity.INFO, severity(134, 82, BloodPressureGuideline.ISH_2020))
    }

    @Test
    fun everyGuidelineAlertsOnAVeryHighReading() {
        BloodPressureGuideline.entries.forEach { guideline ->
            assertEquals(guideline.name, InterpretationSeverity.ALERT, severity(190, 125, guideline))
        }
    }

    @Test
    fun boundsListTheTableLowestFirst() {
        val bounds = bloodPressureCategoryBounds(BloodPressureGuideline.ACC_AHA_2017)

        assertEquals(
            listOf(
                BloodPressureCategoryBounds(BloodPressureCategory.NORMAL, 120, 80, isLowest = true),
                BloodPressureCategoryBounds(BloodPressureCategory.ELEVATED, 120, null, isLowest = false),
                BloodPressureCategoryBounds(BloodPressureCategory.STAGE_1, 130, 80, isLowest = false),
                BloodPressureCategoryBounds(BloodPressureCategory.STAGE_2, 140, 90, isLowest = false),
                BloodPressureCategoryBounds(BloodPressureCategory.SEVERE_REFERENCE, 181, 121, isLowest = false),
            ),
            bounds,
        )
    }

    @Test
    fun boundsAgreeWithTheClassifier() {
        BloodPressureGuideline.entries.forEach { guideline ->
            bloodPressureCategoryBounds(guideline).filterNot { it.isLowest }.forEach { row ->
                // A systolic floor alone, with a low diastolic, must land in that row.
                assertEquals("${guideline.name} ${row.category}", row.category, category(row.systolicMmHg, 50, guideline))
            }
        }
    }

    private fun category(systolic: Int, diastolic: Int, guideline: BloodPressureGuideline) =
        bloodPressureInterpretation(systolic, diastolic, guideline)?.category

    private fun severity(systolic: Int, diastolic: Int, guideline: BloodPressureGuideline) =
        bloodPressureInterpretation(systolic, diastolic, guideline)?.severity

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
