import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

part 'metric_interpretations.freezed.dart';

const double _percentageEpsilon = 0.0001;

enum InterpretationSeverity {
  positive,
  info,
  caution,
  alert,
}

enum BloodPressureCategory {
  normal,
  elevated,
  stage1,
  stage2,
  severeReference,
}

@freezed
abstract class BloodPressureInterpretation with _$BloodPressureInterpretation {
  const factory BloodPressureInterpretation({
    required BloodPressureCategory category,
    required InterpretationSeverity severity,
  }) = _BloodPressureInterpretation;
}

enum BmiCategory {
  underweight,
  healthy,
  overweight,
  obesityClass1,
  obesityClass2,
  obesityClass3,
}

enum FfmiCategory {
  belowAverage,
  average,
  aboveAverage,
  excellent,
  superior,
  exceptional,
  elite,
}

@freezed
abstract class BmiInterpretation with _$BmiInterpretation {
  const factory BmiInterpretation({
    required BmiCategory category,
    required InterpretationSeverity severity,
  }) = _BmiInterpretation;
}

@freezed
abstract class FfmiInterpretation with _$FfmiInterpretation {
  const factory FfmiInterpretation({
    required FfmiCategory category,
    required InterpretationSeverity severity,
  }) = _FfmiInterpretation;
}

enum SleepTargetStatus {
  belowTarget,
  nearTarget,
  metTarget,
}

@freezed
abstract class SleepTargetInterpretation with _$SleepTargetInterpretation {
  const factory SleepTargetInterpretation({
    required SleepTargetStatus status,
    required double averageHours,
    required double targetHours,
    required double gapHours,
    required InterpretationSeverity severity,
  }) = _SleepTargetInterpretation;
}

enum MacroRangeStatus {
  low,
  within,
  high,
}

@freezed
abstract class MacroSplitInterpretation with _$MacroSplitInterpretation {
  const MacroSplitInterpretation._();

  const factory MacroSplitInterpretation({
    required double proteinPercent,
    required double carbsPercent,
    required double fatPercent,
    required MacroRangeStatus proteinStatus,
    required MacroRangeStatus carbsStatus,
    required MacroRangeStatus fatStatus,
    required InterpretationSeverity severity,
  }) = _MacroSplitInterpretation;

  bool get isWithinReference =>
      proteinStatus == MacroRangeStatus.within &&
      carbsStatus == MacroRangeStatus.within &&
      fatStatus == MacroRangeStatus.within;
}

enum WorkoutGuidelineStatus {
  noLoggedMinutes,
  belowReference,
  approachingReference,
  meetsReference,
}

@freezed
abstract class WorkoutGuidelineProgress with _$WorkoutGuidelineProgress {
  const factory WorkoutGuidelineProgress({
    required double loggedMinutes,
    required double referenceMinutes,
    required double percentOfReference,
    required WorkoutGuidelineStatus status,
    required InterpretationSeverity severity,
  }) = _WorkoutGuidelineProgress;
}

enum VitalContextStatus {
  withinReference,
  belowReference,
  aboveReference,
  belowTypicalOxygen,
  lowOxygenReference,
  veryLowOxygenReference,
}

@freezed
abstract class VitalContextInterpretation with _$VitalContextInterpretation {
  const factory VitalContextInterpretation({
    required VitalContextStatus status,
    required InterpretationSeverity severity,
  }) = _VitalContextInterpretation;
}

BloodPressureInterpretation? bloodPressureInterpretation(
  int systolicMmHg,
  int diastolicMmHg,
) {
  if (systolicMmHg <= 0 || diastolicMmHg <= 0) return null;
  final BloodPressureCategory category;
  if (systolicMmHg > 180 || diastolicMmHg > 120) {
    category = BloodPressureCategory.severeReference;
  } else if (systolicMmHg >= 140 || diastolicMmHg >= 90) {
    category = BloodPressureCategory.stage2;
  } else if (systolicMmHg >= 130 || diastolicMmHg >= 80) {
    category = BloodPressureCategory.stage1;
  } else if (systolicMmHg >= 120 && diastolicMmHg < 80) {
    category = BloodPressureCategory.elevated;
  } else {
    category = BloodPressureCategory.normal;
  }
  final InterpretationSeverity severity;
  switch (category) {
    case BloodPressureCategory.normal:
      severity = InterpretationSeverity.positive;
    case BloodPressureCategory.elevated:
      severity = InterpretationSeverity.info;
    case BloodPressureCategory.stage1:
    case BloodPressureCategory.stage2:
      severity = InterpretationSeverity.caution;
    case BloodPressureCategory.severeReference:
      severity = InterpretationSeverity.alert;
  }
  return BloodPressureInterpretation(
    category: category,
    severity: severity,
  );
}

BmiInterpretation? bmiInterpretation(double bmi) {
  if (bmi <= 0.0) return null;
  final BmiCategory category;
  if (bmi < 18.5) {
    category = BmiCategory.underweight;
  } else if (bmi < 25.0) {
    category = BmiCategory.healthy;
  } else if (bmi < 30.0) {
    category = BmiCategory.overweight;
  } else if (bmi < 35.0) {
    category = BmiCategory.obesityClass1;
  } else if (bmi < 40.0) {
    category = BmiCategory.obesityClass2;
  } else {
    category = BmiCategory.obesityClass3;
  }
  final InterpretationSeverity severity;
  switch (category) {
    case BmiCategory.healthy:
      severity = InterpretationSeverity.positive;
    case BmiCategory.underweight:
    case BmiCategory.overweight:
      severity = InterpretationSeverity.info;
    case BmiCategory.obesityClass1:
    case BmiCategory.obesityClass2:
    case BmiCategory.obesityClass3:
      severity = InterpretationSeverity.caution;
  }
  return BmiInterpretation(
    category: category,
    severity: severity,
  );
}

FfmiInterpretation? ffmiInterpretation(double adjustedFfmi) {
  if (adjustedFfmi <= 0.0) return null;
  final FfmiCategory category;
  if (adjustedFfmi < 18.0) {
    category = FfmiCategory.belowAverage;
  } else if (adjustedFfmi < 20.0) {
    category = FfmiCategory.average;
  } else if (adjustedFfmi < 22.0) {
    category = FfmiCategory.aboveAverage;
  } else if (adjustedFfmi < 24.0) {
    category = FfmiCategory.excellent;
  } else if (adjustedFfmi < 26.0) {
    category = FfmiCategory.superior;
  } else if (adjustedFfmi < 28.0) {
    category = FfmiCategory.exceptional;
  } else {
    category = FfmiCategory.elite;
  }
  final InterpretationSeverity severity;
  switch (category) {
    case FfmiCategory.excellent:
    case FfmiCategory.superior:
      severity = InterpretationSeverity.positive;
    case FfmiCategory.exceptional:
    case FfmiCategory.elite:
      severity = InterpretationSeverity.caution;
    case FfmiCategory.belowAverage:
    case FfmiCategory.average:
    case FfmiCategory.aboveAverage:
      severity = InterpretationSeverity.info;
  }
  return FfmiInterpretation(
    category: category,
    severity: severity,
  );
}

SleepTargetInterpretation? sleepTargetInterpretation(
  double averageHours,
  double targetHours,
) {
  if (averageHours <= 0.0 || targetHours <= 0.0) return null;
  final gap = targetHours - averageHours;
  final SleepTargetStatus status;
  if (gap <= 0.0) {
    status = SleepTargetStatus.metTarget;
  } else if (gap <= 0.5) {
    status = SleepTargetStatus.nearTarget;
  } else {
    status = SleepTargetStatus.belowTarget;
  }
  final InterpretationSeverity severity;
  switch (status) {
    case SleepTargetStatus.metTarget:
      severity = InterpretationSeverity.positive;
    case SleepTargetStatus.nearTarget:
      severity = InterpretationSeverity.info;
    case SleepTargetStatus.belowTarget:
      severity = InterpretationSeverity.caution;
  }
  return SleepTargetInterpretation(
    status: status,
    averageHours: averageHours,
    targetHours: targetHours,
    gapHours: gap < 0.0 ? 0.0 : gap,
    severity: severity,
  );
}

MacroSplitInterpretation? macroSplitInterpretation(
  double proteinGrams,
  double carbsGrams,
  double fatGrams,
) {
  final proteinKcal = math.max(0.0, proteinGrams) * 4.0;
  final carbsKcal = math.max(0.0, carbsGrams) * 4.0;
  final fatKcal = math.max(0.0, fatGrams) * 9.0;
  final totalMacroKcal = proteinKcal + carbsKcal + fatKcal;
  if (totalMacroKcal <= 0.0) return null;

  final proteinPercent = proteinKcal / totalMacroKcal * 100.0;
  final carbsPercent = carbsKcal / totalMacroKcal * 100.0;
  final fatPercent = fatKcal / totalMacroKcal * 100.0;
  final proteinStatus = _rangeStatus(proteinPercent, 10.0, 35.0);
  final carbsStatus = _rangeStatus(carbsPercent, 45.0, 65.0);
  final fatStatus = _rangeStatus(fatPercent, 20.0, 35.0);

  final withinReference = proteinStatus == MacroRangeStatus.within &&
      carbsStatus == MacroRangeStatus.within &&
      fatStatus == MacroRangeStatus.within;
  return MacroSplitInterpretation(
    proteinPercent: proteinPercent,
    carbsPercent: carbsPercent,
    fatPercent: fatPercent,
    proteinStatus: proteinStatus,
    carbsStatus: carbsStatus,
    fatStatus: fatStatus,
    severity: withinReference
        ? InterpretationSeverity.positive
        : InterpretationSeverity.info,
  );
}

WorkoutGuidelineProgress? workoutGuidelineProgress(
  double loggedMinutes, [
  double referenceMinutes = 150.0,
]) {
  if (referenceMinutes <= 0.0) return null;
  final safeMinutes = math.max(0.0, loggedMinutes);
  final percent = safeMinutes / referenceMinutes * 100.0;
  final WorkoutGuidelineStatus status;
  if (safeMinutes == 0.0) {
    status = WorkoutGuidelineStatus.noLoggedMinutes;
  } else if (percent >= 100.0) {
    status = WorkoutGuidelineStatus.meetsReference;
  } else if (percent >= 75.0) {
    status = WorkoutGuidelineStatus.approachingReference;
  } else {
    status = WorkoutGuidelineStatus.belowReference;
  }
  final InterpretationSeverity severity;
  switch (status) {
    case WorkoutGuidelineStatus.meetsReference:
      severity = InterpretationSeverity.positive;
    case WorkoutGuidelineStatus.approachingReference:
      severity = InterpretationSeverity.info;
    case WorkoutGuidelineStatus.noLoggedMinutes:
    case WorkoutGuidelineStatus.belowReference:
      severity = InterpretationSeverity.caution;
  }
  return WorkoutGuidelineProgress(
    loggedMinutes: safeMinutes,
    referenceMinutes: referenceMinutes,
    percentOfReference: percent,
    status: status,
    severity: severity,
  );
}

VitalContextInterpretation? restingHeartRateContext(int bpm) {
  if (bpm <= 0) return null;
  return _referenceRangeContext(bpm.toDouble(), 60.0, 100.0);
}

VitalContextInterpretation? respiratoryRateContext(double breathsPerMinute) =>
    breathsPerMinute <= 0.0
        ? null
        : _referenceRangeContext(breathsPerMinute, 12.0, 18.0);

VitalContextInterpretation? bodyTemperatureContext(double celsius) =>
    celsius <= 0.0 ? null : _referenceRangeContext(celsius, 36.1, 37.2);

VitalContextInterpretation? oxygenSaturationContext(double percent) {
  if (percent <= 0.0) return null;
  final VitalContextStatus status;
  if (percent <= 88.0) {
    status = VitalContextStatus.veryLowOxygenReference;
  } else if (percent <= 92.0) {
    status = VitalContextStatus.lowOxygenReference;
  } else if (percent < 95.0) {
    status = VitalContextStatus.belowTypicalOxygen;
  } else {
    status = VitalContextStatus.withinReference;
  }
  final InterpretationSeverity severity;
  switch (status) {
    case VitalContextStatus.withinReference:
      severity = InterpretationSeverity.positive;
    case VitalContextStatus.belowTypicalOxygen:
      severity = InterpretationSeverity.info;
    case VitalContextStatus.lowOxygenReference:
      severity = InterpretationSeverity.caution;
    case VitalContextStatus.veryLowOxygenReference:
      severity = InterpretationSeverity.alert;
    case VitalContextStatus.belowReference:
    case VitalContextStatus.aboveReference:
      severity = InterpretationSeverity.caution;
  }
  return VitalContextInterpretation(
    status: status,
    severity: severity,
  );
}

VitalContextInterpretation _referenceRangeContext(
  double value,
  double lowInclusive,
  double highInclusive,
) {
  if (value < lowInclusive) {
    return const VitalContextInterpretation(
      status: VitalContextStatus.belowReference,
      severity: InterpretationSeverity.caution,
    );
  }
  if (value > highInclusive) {
    return const VitalContextInterpretation(
      status: VitalContextStatus.aboveReference,
      severity: InterpretationSeverity.caution,
    );
  }
  return const VitalContextInterpretation(
    status: VitalContextStatus.withinReference,
    severity: InterpretationSeverity.positive,
  );
}

MacroRangeStatus _rangeStatus(
  double value,
  double lowInclusive,
  double highInclusive,
) {
  if (value < lowInclusive - _percentageEpsilon) return MacroRangeStatus.low;
  if (value > highInclusive + _percentageEpsilon) return MacroRangeStatus.high;
  return MacroRangeStatus.within;
}
