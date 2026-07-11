import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/metric_interpretations.dart';

void main() {
  test('classifiesBloodPressureUsingHighestApplicableCategory', () {
    expect(
      bloodPressureInterpretation(118, 76)?.category,
      BloodPressureCategory.normal,
    );
    expect(
      bloodPressureInterpretation(124, 78)?.category,
      BloodPressureCategory.elevated,
    );
    expect(
      bloodPressureInterpretation(128, 84)?.category,
      BloodPressureCategory.stage1,
    );
    expect(
      bloodPressureInterpretation(142, 82)?.category,
      BloodPressureCategory.stage2,
    );
    expect(
      bloodPressureInterpretation(181, 82)?.category,
      BloodPressureCategory.severeReference,
    );
  });

  test('classifiesAdultBmiBoundaries', () {
    expect(bmiInterpretation(18.4)?.category, BmiCategory.underweight);
    expect(bmiInterpretation(18.5)?.category, BmiCategory.healthy);
    expect(bmiInterpretation(25.0)?.category, BmiCategory.overweight);
    expect(bmiInterpretation(30.0)?.category, BmiCategory.obesityClass1);
    expect(bmiInterpretation(35.0)?.category, BmiCategory.obesityClass2);
    expect(bmiInterpretation(40.0)?.category, BmiCategory.obesityClass3);
  });

  test('classifiesAdjustedFfmiBoundaries', () {
    expect(ffmiInterpretation(17.9)?.category, FfmiCategory.belowAverage);
    expect(ffmiInterpretation(18.0)?.category, FfmiCategory.average);
    expect(ffmiInterpretation(20.0)?.category, FfmiCategory.aboveAverage);
    expect(ffmiInterpretation(22.0)?.category, FfmiCategory.excellent);
    expect(ffmiInterpretation(24.0)?.category, FfmiCategory.superior);
    expect(ffmiInterpretation(26.0)?.category, FfmiCategory.exceptional);
    expect(ffmiInterpretation(28.0)?.category, FfmiCategory.elite);
  });

  test('interpretsSleepAgainstUserTarget', () {
    expect(
      sleepTargetInterpretation(6.0, 7.5)?.status,
      SleepTargetStatus.belowTarget,
    );
    expect(
      sleepTargetInterpretation(7.1, 7.5)?.status,
      SleepTargetStatus.nearTarget,
    );
    expect(
      sleepTargetInterpretation(7.5, 7.5)?.status,
      SleepTargetStatus.metTarget,
    );
  });

  test('calculatesMacroSplitFromLoggedMacroCalories', () {
    final split = macroSplitInterpretation(75.0, 250.0, 77.7778)!;

    expect(split.proteinPercent, closeTo(15.0, 0.01));
    expect(split.carbsPercent, closeTo(50.0, 0.01));
    expect(split.fatPercent, closeTo(35.0, 0.01));
    expect(split.isWithinReference, isTrue);
  });

  test('flagsMacroSplitOutsideReferenceWithoutRejectingData', () {
    final split = macroSplitInterpretation(200.0, 20.0, 10.0)!;

    expect(split.isWithinReference, isFalse);
    expect(split.proteinStatus, MacroRangeStatus.high);
    expect(split.carbsStatus, MacroRangeStatus.low);
  });

  test('interpretsWorkoutProgressAgainstWeeklyReference', () {
    expect(
      workoutGuidelineProgress(0.0)?.status,
      WorkoutGuidelineStatus.noLoggedMinutes,
    );
    expect(
      workoutGuidelineProgress(120.0)?.status,
      WorkoutGuidelineStatus.approachingReference,
    );
    expect(
      workoutGuidelineProgress(150.0)?.status,
      WorkoutGuidelineStatus.meetsReference,
    );
  });

  test('interpretsVitalsWithBroadAdultReferenceRanges', () {
    expect(restingHeartRateContext(60)?.status, VitalContextStatus.withinReference);
    expect(restingHeartRateContext(50)?.status, VitalContextStatus.belowReference);
    expect(respiratoryRateContext(12.0)?.status, VitalContextStatus.withinReference);
    expect(respiratoryRateContext(20.0)?.status, VitalContextStatus.aboveReference);
    expect(bodyTemperatureContext(36.8)?.status, VitalContextStatus.withinReference);
    expect(bodyTemperatureContext(38.0)?.status, VitalContextStatus.aboveReference);
  });

  test('interpretsOxygenSaturationSeparatelyFromSimpleReferenceRanges', () {
    expect(
      oxygenSaturationContext(97.0)?.status,
      VitalContextStatus.withinReference,
    );
    expect(
      oxygenSaturationContext(94.0)?.status,
      VitalContextStatus.belowTypicalOxygen,
    );
    expect(
      oxygenSaturationContext(92.0)?.status,
      VitalContextStatus.lowOxygenReference,
    );
    expect(
      oxygenSaturationContext(88.0)?.status,
      VitalContextStatus.veryLowOxygenReference,
    );
  });

  test('returnsNullForInvalidInputs', () {
    expect(bloodPressureInterpretation(0, 80), isNull);
    expect(bmiInterpretation(0.0), isNull);
    expect(ffmiInterpretation(0.0), isNull);
    expect(sleepTargetInterpretation(0.0, 8.0), isNull);
    expect(macroSplitInterpretation(0.0, 0.0, 0.0), isNull);
    expect(workoutGuidelineProgress(10.0, 0.0), isNull);
    expect(restingHeartRateContext(0), isNull);
  });
}
