// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepDisplay {

/// The period the display was built for (the same one the scaffold shows).
 DatePeriod get period; bool get isDay; List<SleepData> get dailySessions; SleepData? get dailySummary; List<SleepDurationPoint> get durationPoints; List<SleepDurationPoint> get previousDurationPoints;/// The 90 days before the period, for the personal-baseline stats.
 List<SleepDurationPoint> get baselineDurationPoints; SleepOverviewSummary get overviewSummary;/// The sessions of each night in the period, keyed by the night's date.
 Map<LocalDate, List<SleepData>> get sessionsByDate;/// Every session inside the selected period, newest night last.
 List<SleepData> get periodSessions;/// Daily HRV over the same period, for the sleep-vs-HRV correlation.
 List<CrossMetricValue> get crossMetricHrvValues;/// The nights that actually recorded sleep.
 List<SleepDurationPoint> get nights; double get totalHours; double get averageHours; double get longestHours; double get previousAverageHours; List<PeriodChartValue> get chartValues; List<SleepScheduleDay> get scheduleDays; bool get useScheduleChart; List<SleepStageShare> get stageShares; DailyGoalProgress get goalProgress; PeriodComparison get periodComparison; PersonalBaselineInsight? get baselineInsight; SleepTargetInterpretation? get targetInterpretation; CrossMetricInsight? get hrvInsight; DataConfidence get dataConfidence;/// The entry lists, newest night first.
 Map<LocalDate, List<SleepData>> get sortedSessionsByDate; List<SleepData> get sortedDailySessions; List<SleepData> get sortedPeriodSessions; String? get dayTimeRangeText;
/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepDisplayCopyWith<SleepDisplay> get copyWith => _$SleepDisplayCopyWithImpl<SleepDisplay>(this as SleepDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepDisplay&&(identical(other.period, period) || other.period == period)&&(identical(other.isDay, isDay) || other.isDay == isDay)&&const DeepCollectionEquality().equals(other.dailySessions, dailySessions)&&(identical(other.dailySummary, dailySummary) || other.dailySummary == dailySummary)&&const DeepCollectionEquality().equals(other.durationPoints, durationPoints)&&const DeepCollectionEquality().equals(other.previousDurationPoints, previousDurationPoints)&&const DeepCollectionEquality().equals(other.baselineDurationPoints, baselineDurationPoints)&&(identical(other.overviewSummary, overviewSummary) || other.overviewSummary == overviewSummary)&&const DeepCollectionEquality().equals(other.sessionsByDate, sessionsByDate)&&const DeepCollectionEquality().equals(other.periodSessions, periodSessions)&&const DeepCollectionEquality().equals(other.crossMetricHrvValues, crossMetricHrvValues)&&const DeepCollectionEquality().equals(other.nights, nights)&&(identical(other.totalHours, totalHours) || other.totalHours == totalHours)&&(identical(other.averageHours, averageHours) || other.averageHours == averageHours)&&(identical(other.longestHours, longestHours) || other.longestHours == longestHours)&&(identical(other.previousAverageHours, previousAverageHours) || other.previousAverageHours == previousAverageHours)&&const DeepCollectionEquality().equals(other.chartValues, chartValues)&&const DeepCollectionEquality().equals(other.scheduleDays, scheduleDays)&&(identical(other.useScheduleChart, useScheduleChart) || other.useScheduleChart == useScheduleChart)&&const DeepCollectionEquality().equals(other.stageShares, stageShares)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.targetInterpretation, targetInterpretation) || other.targetInterpretation == targetInterpretation)&&(identical(other.hrvInsight, hrvInsight) || other.hrvInsight == hrvInsight)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence)&&const DeepCollectionEquality().equals(other.sortedSessionsByDate, sortedSessionsByDate)&&const DeepCollectionEquality().equals(other.sortedDailySessions, sortedDailySessions)&&const DeepCollectionEquality().equals(other.sortedPeriodSessions, sortedPeriodSessions)&&(identical(other.dayTimeRangeText, dayTimeRangeText) || other.dayTimeRangeText == dayTimeRangeText));
}


@override
int get hashCode => Object.hashAll([runtimeType,period,isDay,const DeepCollectionEquality().hash(dailySessions),dailySummary,const DeepCollectionEquality().hash(durationPoints),const DeepCollectionEquality().hash(previousDurationPoints),const DeepCollectionEquality().hash(baselineDurationPoints),overviewSummary,const DeepCollectionEquality().hash(sessionsByDate),const DeepCollectionEquality().hash(periodSessions),const DeepCollectionEquality().hash(crossMetricHrvValues),const DeepCollectionEquality().hash(nights),totalHours,averageHours,longestHours,previousAverageHours,const DeepCollectionEquality().hash(chartValues),const DeepCollectionEquality().hash(scheduleDays),useScheduleChart,const DeepCollectionEquality().hash(stageShares),goalProgress,periodComparison,baselineInsight,targetInterpretation,hrvInsight,dataConfidence,const DeepCollectionEquality().hash(sortedSessionsByDate),const DeepCollectionEquality().hash(sortedDailySessions),const DeepCollectionEquality().hash(sortedPeriodSessions),dayTimeRangeText]);

@override
String toString() {
  return 'SleepDisplay(period: $period, isDay: $isDay, dailySessions: $dailySessions, dailySummary: $dailySummary, durationPoints: $durationPoints, previousDurationPoints: $previousDurationPoints, baselineDurationPoints: $baselineDurationPoints, overviewSummary: $overviewSummary, sessionsByDate: $sessionsByDate, periodSessions: $periodSessions, crossMetricHrvValues: $crossMetricHrvValues, nights: $nights, totalHours: $totalHours, averageHours: $averageHours, longestHours: $longestHours, previousAverageHours: $previousAverageHours, chartValues: $chartValues, scheduleDays: $scheduleDays, useScheduleChart: $useScheduleChart, stageShares: $stageShares, goalProgress: $goalProgress, periodComparison: $periodComparison, baselineInsight: $baselineInsight, targetInterpretation: $targetInterpretation, hrvInsight: $hrvInsight, dataConfidence: $dataConfidence, sortedSessionsByDate: $sortedSessionsByDate, sortedDailySessions: $sortedDailySessions, sortedPeriodSessions: $sortedPeriodSessions, dayTimeRangeText: $dayTimeRangeText)';
}


}

/// @nodoc
abstract mixin class $SleepDisplayCopyWith<$Res>  {
  factory $SleepDisplayCopyWith(SleepDisplay value, $Res Function(SleepDisplay) _then) = _$SleepDisplayCopyWithImpl;
@useResult
$Res call({
 DatePeriod period, bool isDay, List<SleepData> dailySessions, SleepData? dailySummary, List<SleepDurationPoint> durationPoints, List<SleepDurationPoint> previousDurationPoints, List<SleepDurationPoint> baselineDurationPoints, SleepOverviewSummary overviewSummary, Map<LocalDate, List<SleepData>> sessionsByDate, List<SleepData> periodSessions, List<CrossMetricValue> crossMetricHrvValues, List<SleepDurationPoint> nights, double totalHours, double averageHours, double longestHours, double previousAverageHours, List<PeriodChartValue> chartValues, List<SleepScheduleDay> scheduleDays, bool useScheduleChart, List<SleepStageShare> stageShares, DailyGoalProgress goalProgress, PeriodComparison periodComparison, PersonalBaselineInsight? baselineInsight, SleepTargetInterpretation? targetInterpretation, CrossMetricInsight? hrvInsight, DataConfidence dataConfidence, Map<LocalDate, List<SleepData>> sortedSessionsByDate, List<SleepData> sortedDailySessions, List<SleepData> sortedPeriodSessions, String? dayTimeRangeText
});


$SleepDataCopyWith<$Res>? get dailySummary;$DailyGoalProgressCopyWith<$Res> get goalProgress;$PeriodComparisonCopyWith<$Res> get periodComparison;$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;$SleepTargetInterpretationCopyWith<$Res>? get targetInterpretation;$CrossMetricInsightCopyWith<$Res>? get hrvInsight;$DataConfidenceCopyWith<$Res> get dataConfidence;

}
/// @nodoc
class _$SleepDisplayCopyWithImpl<$Res>
    implements $SleepDisplayCopyWith<$Res> {
  _$SleepDisplayCopyWithImpl(this._self, this._then);

  final SleepDisplay _self;
  final $Res Function(SleepDisplay) _then;

/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? period = null,Object? isDay = null,Object? dailySessions = null,Object? dailySummary = freezed,Object? durationPoints = null,Object? previousDurationPoints = null,Object? baselineDurationPoints = null,Object? overviewSummary = null,Object? sessionsByDate = null,Object? periodSessions = null,Object? crossMetricHrvValues = null,Object? nights = null,Object? totalHours = null,Object? averageHours = null,Object? longestHours = null,Object? previousAverageHours = null,Object? chartValues = null,Object? scheduleDays = null,Object? useScheduleChart = null,Object? stageShares = null,Object? goalProgress = null,Object? periodComparison = null,Object? baselineInsight = freezed,Object? targetInterpretation = freezed,Object? hrvInsight = freezed,Object? dataConfidence = null,Object? sortedSessionsByDate = null,Object? sortedDailySessions = null,Object? sortedPeriodSessions = null,Object? dayTimeRangeText = freezed,}) {
  return _then(_self.copyWith(
period: null == period ? _self.period : period // ignore: cast_nullable_to_non_nullable
as DatePeriod,isDay: null == isDay ? _self.isDay : isDay // ignore: cast_nullable_to_non_nullable
as bool,dailySessions: null == dailySessions ? _self.dailySessions : dailySessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailySummary: freezed == dailySummary ? _self.dailySummary : dailySummary // ignore: cast_nullable_to_non_nullable
as SleepData?,durationPoints: null == durationPoints ? _self.durationPoints : durationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,previousDurationPoints: null == previousDurationPoints ? _self.previousDurationPoints : previousDurationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,baselineDurationPoints: null == baselineDurationPoints ? _self.baselineDurationPoints : baselineDurationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,overviewSummary: null == overviewSummary ? _self.overviewSummary : overviewSummary // ignore: cast_nullable_to_non_nullable
as SleepOverviewSummary,sessionsByDate: null == sessionsByDate ? _self.sessionsByDate : sessionsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<SleepData>>,periodSessions: null == periodSessions ? _self.periodSessions : periodSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,crossMetricHrvValues: null == crossMetricHrvValues ? _self.crossMetricHrvValues : crossMetricHrvValues // ignore: cast_nullable_to_non_nullable
as List<CrossMetricValue>,nights: null == nights ? _self.nights : nights // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,totalHours: null == totalHours ? _self.totalHours : totalHours // ignore: cast_nullable_to_non_nullable
as double,averageHours: null == averageHours ? _self.averageHours : averageHours // ignore: cast_nullable_to_non_nullable
as double,longestHours: null == longestHours ? _self.longestHours : longestHours // ignore: cast_nullable_to_non_nullable
as double,previousAverageHours: null == previousAverageHours ? _self.previousAverageHours : previousAverageHours // ignore: cast_nullable_to_non_nullable
as double,chartValues: null == chartValues ? _self.chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,scheduleDays: null == scheduleDays ? _self.scheduleDays : scheduleDays // ignore: cast_nullable_to_non_nullable
as List<SleepScheduleDay>,useScheduleChart: null == useScheduleChart ? _self.useScheduleChart : useScheduleChart // ignore: cast_nullable_to_non_nullable
as bool,stageShares: null == stageShares ? _self.stageShares : stageShares // ignore: cast_nullable_to_non_nullable
as List<SleepStageShare>,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,periodComparison: null == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,targetInterpretation: freezed == targetInterpretation ? _self.targetInterpretation : targetInterpretation // ignore: cast_nullable_to_non_nullable
as SleepTargetInterpretation?,hrvInsight: freezed == hrvInsight ? _self.hrvInsight : hrvInsight // ignore: cast_nullable_to_non_nullable
as CrossMetricInsight?,dataConfidence: null == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,sortedSessionsByDate: null == sortedSessionsByDate ? _self.sortedSessionsByDate : sortedSessionsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<SleepData>>,sortedDailySessions: null == sortedDailySessions ? _self.sortedDailySessions : sortedDailySessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,sortedPeriodSessions: null == sortedPeriodSessions ? _self.sortedPeriodSessions : sortedPeriodSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dayTimeRangeText: freezed == dayTimeRangeText ? _self.dayTimeRangeText : dayTimeRangeText // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}
/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get dailySummary {
    if (_self.dailySummary == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.dailySummary!, (value) {
    return _then(_self.copyWith(dailySummary: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get periodComparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight {
    if (_self.baselineInsight == null) {
    return null;
  }

  return $PersonalBaselineInsightCopyWith<$Res>(_self.baselineInsight!, (value) {
    return _then(_self.copyWith(baselineInsight: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepTargetInterpretationCopyWith<$Res>? get targetInterpretation {
    if (_self.targetInterpretation == null) {
    return null;
  }

  return $SleepTargetInterpretationCopyWith<$Res>(_self.targetInterpretation!, (value) {
    return _then(_self.copyWith(targetInterpretation: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CrossMetricInsightCopyWith<$Res>? get hrvInsight {
    if (_self.hrvInsight == null) {
    return null;
  }

  return $CrossMetricInsightCopyWith<$Res>(_self.hrvInsight!, (value) {
    return _then(_self.copyWith(hrvInsight: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get dataConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.dataConfidence, (value) {
    return _then(_self.copyWith(dataConfidence: value));
  });
}
}


/// Adds pattern-matching-related methods to [SleepDisplay].
extension SleepDisplayPatterns on SleepDisplay {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepDisplay() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepDisplay value)  $default,){
final _that = this;
switch (_that) {
case _SleepDisplay():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _SleepDisplay() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DatePeriod period,  bool isDay,  List<SleepData> dailySessions,  SleepData? dailySummary,  List<SleepDurationPoint> durationPoints,  List<SleepDurationPoint> previousDurationPoints,  List<SleepDurationPoint> baselineDurationPoints,  SleepOverviewSummary overviewSummary,  Map<LocalDate, List<SleepData>> sessionsByDate,  List<SleepData> periodSessions,  List<CrossMetricValue> crossMetricHrvValues,  List<SleepDurationPoint> nights,  double totalHours,  double averageHours,  double longestHours,  double previousAverageHours,  List<PeriodChartValue> chartValues,  List<SleepScheduleDay> scheduleDays,  bool useScheduleChart,  List<SleepStageShare> stageShares,  DailyGoalProgress goalProgress,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  SleepTargetInterpretation? targetInterpretation,  CrossMetricInsight? hrvInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<SleepData>> sortedSessionsByDate,  List<SleepData> sortedDailySessions,  List<SleepData> sortedPeriodSessions,  String? dayTimeRangeText)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepDisplay() when $default != null:
return $default(_that.period,_that.isDay,_that.dailySessions,_that.dailySummary,_that.durationPoints,_that.previousDurationPoints,_that.baselineDurationPoints,_that.overviewSummary,_that.sessionsByDate,_that.periodSessions,_that.crossMetricHrvValues,_that.nights,_that.totalHours,_that.averageHours,_that.longestHours,_that.previousAverageHours,_that.chartValues,_that.scheduleDays,_that.useScheduleChart,_that.stageShares,_that.goalProgress,_that.periodComparison,_that.baselineInsight,_that.targetInterpretation,_that.hrvInsight,_that.dataConfidence,_that.sortedSessionsByDate,_that.sortedDailySessions,_that.sortedPeriodSessions,_that.dayTimeRangeText);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DatePeriod period,  bool isDay,  List<SleepData> dailySessions,  SleepData? dailySummary,  List<SleepDurationPoint> durationPoints,  List<SleepDurationPoint> previousDurationPoints,  List<SleepDurationPoint> baselineDurationPoints,  SleepOverviewSummary overviewSummary,  Map<LocalDate, List<SleepData>> sessionsByDate,  List<SleepData> periodSessions,  List<CrossMetricValue> crossMetricHrvValues,  List<SleepDurationPoint> nights,  double totalHours,  double averageHours,  double longestHours,  double previousAverageHours,  List<PeriodChartValue> chartValues,  List<SleepScheduleDay> scheduleDays,  bool useScheduleChart,  List<SleepStageShare> stageShares,  DailyGoalProgress goalProgress,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  SleepTargetInterpretation? targetInterpretation,  CrossMetricInsight? hrvInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<SleepData>> sortedSessionsByDate,  List<SleepData> sortedDailySessions,  List<SleepData> sortedPeriodSessions,  String? dayTimeRangeText)  $default,) {final _that = this;
switch (_that) {
case _SleepDisplay():
return $default(_that.period,_that.isDay,_that.dailySessions,_that.dailySummary,_that.durationPoints,_that.previousDurationPoints,_that.baselineDurationPoints,_that.overviewSummary,_that.sessionsByDate,_that.periodSessions,_that.crossMetricHrvValues,_that.nights,_that.totalHours,_that.averageHours,_that.longestHours,_that.previousAverageHours,_that.chartValues,_that.scheduleDays,_that.useScheduleChart,_that.stageShares,_that.goalProgress,_that.periodComparison,_that.baselineInsight,_that.targetInterpretation,_that.hrvInsight,_that.dataConfidence,_that.sortedSessionsByDate,_that.sortedDailySessions,_that.sortedPeriodSessions,_that.dayTimeRangeText);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DatePeriod period,  bool isDay,  List<SleepData> dailySessions,  SleepData? dailySummary,  List<SleepDurationPoint> durationPoints,  List<SleepDurationPoint> previousDurationPoints,  List<SleepDurationPoint> baselineDurationPoints,  SleepOverviewSummary overviewSummary,  Map<LocalDate, List<SleepData>> sessionsByDate,  List<SleepData> periodSessions,  List<CrossMetricValue> crossMetricHrvValues,  List<SleepDurationPoint> nights,  double totalHours,  double averageHours,  double longestHours,  double previousAverageHours,  List<PeriodChartValue> chartValues,  List<SleepScheduleDay> scheduleDays,  bool useScheduleChart,  List<SleepStageShare> stageShares,  DailyGoalProgress goalProgress,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  SleepTargetInterpretation? targetInterpretation,  CrossMetricInsight? hrvInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<SleepData>> sortedSessionsByDate,  List<SleepData> sortedDailySessions,  List<SleepData> sortedPeriodSessions,  String? dayTimeRangeText)?  $default,) {final _that = this;
switch (_that) {
case _SleepDisplay() when $default != null:
return $default(_that.period,_that.isDay,_that.dailySessions,_that.dailySummary,_that.durationPoints,_that.previousDurationPoints,_that.baselineDurationPoints,_that.overviewSummary,_that.sessionsByDate,_that.periodSessions,_that.crossMetricHrvValues,_that.nights,_that.totalHours,_that.averageHours,_that.longestHours,_that.previousAverageHours,_that.chartValues,_that.scheduleDays,_that.useScheduleChart,_that.stageShares,_that.goalProgress,_that.periodComparison,_that.baselineInsight,_that.targetInterpretation,_that.hrvInsight,_that.dataConfidence,_that.sortedSessionsByDate,_that.sortedDailySessions,_that.sortedPeriodSessions,_that.dayTimeRangeText);case _:
  return null;

}
}

}

/// @nodoc


class _SleepDisplay extends SleepDisplay {
  const _SleepDisplay({required this.period, required this.isDay, required final  List<SleepData> dailySessions, required this.dailySummary, required final  List<SleepDurationPoint> durationPoints, required final  List<SleepDurationPoint> previousDurationPoints, required final  List<SleepDurationPoint> baselineDurationPoints, required this.overviewSummary, required final  Map<LocalDate, List<SleepData>> sessionsByDate, required final  List<SleepData> periodSessions, required final  List<CrossMetricValue> crossMetricHrvValues, required final  List<SleepDurationPoint> nights, required this.totalHours, required this.averageHours, required this.longestHours, required this.previousAverageHours, required final  List<PeriodChartValue> chartValues, required final  List<SleepScheduleDay> scheduleDays, required this.useScheduleChart, required final  List<SleepStageShare> stageShares, required this.goalProgress, required this.periodComparison, required this.baselineInsight, required this.targetInterpretation, required this.hrvInsight, required this.dataConfidence, required final  Map<LocalDate, List<SleepData>> sortedSessionsByDate, required final  List<SleepData> sortedDailySessions, required final  List<SleepData> sortedPeriodSessions, required this.dayTimeRangeText}): _dailySessions = dailySessions,_durationPoints = durationPoints,_previousDurationPoints = previousDurationPoints,_baselineDurationPoints = baselineDurationPoints,_sessionsByDate = sessionsByDate,_periodSessions = periodSessions,_crossMetricHrvValues = crossMetricHrvValues,_nights = nights,_chartValues = chartValues,_scheduleDays = scheduleDays,_stageShares = stageShares,_sortedSessionsByDate = sortedSessionsByDate,_sortedDailySessions = sortedDailySessions,_sortedPeriodSessions = sortedPeriodSessions,super._();
  

/// The period the display was built for (the same one the scaffold shows).
@override final  DatePeriod period;
@override final  bool isDay;
 final  List<SleepData> _dailySessions;
@override List<SleepData> get dailySessions {
  if (_dailySessions is EqualUnmodifiableListView) return _dailySessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailySessions);
}

@override final  SleepData? dailySummary;
 final  List<SleepDurationPoint> _durationPoints;
@override List<SleepDurationPoint> get durationPoints {
  if (_durationPoints is EqualUnmodifiableListView) return _durationPoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_durationPoints);
}

 final  List<SleepDurationPoint> _previousDurationPoints;
@override List<SleepDurationPoint> get previousDurationPoints {
  if (_previousDurationPoints is EqualUnmodifiableListView) return _previousDurationPoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDurationPoints);
}

/// The 90 days before the period, for the personal-baseline stats.
 final  List<SleepDurationPoint> _baselineDurationPoints;
/// The 90 days before the period, for the personal-baseline stats.
@override List<SleepDurationPoint> get baselineDurationPoints {
  if (_baselineDurationPoints is EqualUnmodifiableListView) return _baselineDurationPoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDurationPoints);
}

@override final  SleepOverviewSummary overviewSummary;
/// The sessions of each night in the period, keyed by the night's date.
 final  Map<LocalDate, List<SleepData>> _sessionsByDate;
/// The sessions of each night in the period, keyed by the night's date.
@override Map<LocalDate, List<SleepData>> get sessionsByDate {
  if (_sessionsByDate is EqualUnmodifiableMapView) return _sessionsByDate;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_sessionsByDate);
}

/// Every session inside the selected period, newest night last.
 final  List<SleepData> _periodSessions;
/// Every session inside the selected period, newest night last.
@override List<SleepData> get periodSessions {
  if (_periodSessions is EqualUnmodifiableListView) return _periodSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_periodSessions);
}

/// Daily HRV over the same period, for the sleep-vs-HRV correlation.
 final  List<CrossMetricValue> _crossMetricHrvValues;
/// Daily HRV over the same period, for the sleep-vs-HRV correlation.
@override List<CrossMetricValue> get crossMetricHrvValues {
  if (_crossMetricHrvValues is EqualUnmodifiableListView) return _crossMetricHrvValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_crossMetricHrvValues);
}

/// The nights that actually recorded sleep.
 final  List<SleepDurationPoint> _nights;
/// The nights that actually recorded sleep.
@override List<SleepDurationPoint> get nights {
  if (_nights is EqualUnmodifiableListView) return _nights;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_nights);
}

@override final  double totalHours;
@override final  double averageHours;
@override final  double longestHours;
@override final  double previousAverageHours;
 final  List<PeriodChartValue> _chartValues;
@override List<PeriodChartValue> get chartValues {
  if (_chartValues is EqualUnmodifiableListView) return _chartValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_chartValues);
}

 final  List<SleepScheduleDay> _scheduleDays;
@override List<SleepScheduleDay> get scheduleDays {
  if (_scheduleDays is EqualUnmodifiableListView) return _scheduleDays;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_scheduleDays);
}

@override final  bool useScheduleChart;
 final  List<SleepStageShare> _stageShares;
@override List<SleepStageShare> get stageShares {
  if (_stageShares is EqualUnmodifiableListView) return _stageShares;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stageShares);
}

@override final  DailyGoalProgress goalProgress;
@override final  PeriodComparison periodComparison;
@override final  PersonalBaselineInsight? baselineInsight;
@override final  SleepTargetInterpretation? targetInterpretation;
@override final  CrossMetricInsight? hrvInsight;
@override final  DataConfidence dataConfidence;
/// The entry lists, newest night first.
 final  Map<LocalDate, List<SleepData>> _sortedSessionsByDate;
/// The entry lists, newest night first.
@override Map<LocalDate, List<SleepData>> get sortedSessionsByDate {
  if (_sortedSessionsByDate is EqualUnmodifiableMapView) return _sortedSessionsByDate;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_sortedSessionsByDate);
}

 final  List<SleepData> _sortedDailySessions;
@override List<SleepData> get sortedDailySessions {
  if (_sortedDailySessions is EqualUnmodifiableListView) return _sortedDailySessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sortedDailySessions);
}

 final  List<SleepData> _sortedPeriodSessions;
@override List<SleepData> get sortedPeriodSessions {
  if (_sortedPeriodSessions is EqualUnmodifiableListView) return _sortedPeriodSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sortedPeriodSessions);
}

@override final  String? dayTimeRangeText;

/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepDisplayCopyWith<_SleepDisplay> get copyWith => __$SleepDisplayCopyWithImpl<_SleepDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepDisplay&&(identical(other.period, period) || other.period == period)&&(identical(other.isDay, isDay) || other.isDay == isDay)&&const DeepCollectionEquality().equals(other._dailySessions, _dailySessions)&&(identical(other.dailySummary, dailySummary) || other.dailySummary == dailySummary)&&const DeepCollectionEquality().equals(other._durationPoints, _durationPoints)&&const DeepCollectionEquality().equals(other._previousDurationPoints, _previousDurationPoints)&&const DeepCollectionEquality().equals(other._baselineDurationPoints, _baselineDurationPoints)&&(identical(other.overviewSummary, overviewSummary) || other.overviewSummary == overviewSummary)&&const DeepCollectionEquality().equals(other._sessionsByDate, _sessionsByDate)&&const DeepCollectionEquality().equals(other._periodSessions, _periodSessions)&&const DeepCollectionEquality().equals(other._crossMetricHrvValues, _crossMetricHrvValues)&&const DeepCollectionEquality().equals(other._nights, _nights)&&(identical(other.totalHours, totalHours) || other.totalHours == totalHours)&&(identical(other.averageHours, averageHours) || other.averageHours == averageHours)&&(identical(other.longestHours, longestHours) || other.longestHours == longestHours)&&(identical(other.previousAverageHours, previousAverageHours) || other.previousAverageHours == previousAverageHours)&&const DeepCollectionEquality().equals(other._chartValues, _chartValues)&&const DeepCollectionEquality().equals(other._scheduleDays, _scheduleDays)&&(identical(other.useScheduleChart, useScheduleChart) || other.useScheduleChart == useScheduleChart)&&const DeepCollectionEquality().equals(other._stageShares, _stageShares)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.targetInterpretation, targetInterpretation) || other.targetInterpretation == targetInterpretation)&&(identical(other.hrvInsight, hrvInsight) || other.hrvInsight == hrvInsight)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence)&&const DeepCollectionEquality().equals(other._sortedSessionsByDate, _sortedSessionsByDate)&&const DeepCollectionEquality().equals(other._sortedDailySessions, _sortedDailySessions)&&const DeepCollectionEquality().equals(other._sortedPeriodSessions, _sortedPeriodSessions)&&(identical(other.dayTimeRangeText, dayTimeRangeText) || other.dayTimeRangeText == dayTimeRangeText));
}


@override
int get hashCode => Object.hashAll([runtimeType,period,isDay,const DeepCollectionEquality().hash(_dailySessions),dailySummary,const DeepCollectionEquality().hash(_durationPoints),const DeepCollectionEquality().hash(_previousDurationPoints),const DeepCollectionEquality().hash(_baselineDurationPoints),overviewSummary,const DeepCollectionEquality().hash(_sessionsByDate),const DeepCollectionEquality().hash(_periodSessions),const DeepCollectionEquality().hash(_crossMetricHrvValues),const DeepCollectionEquality().hash(_nights),totalHours,averageHours,longestHours,previousAverageHours,const DeepCollectionEquality().hash(_chartValues),const DeepCollectionEquality().hash(_scheduleDays),useScheduleChart,const DeepCollectionEquality().hash(_stageShares),goalProgress,periodComparison,baselineInsight,targetInterpretation,hrvInsight,dataConfidence,const DeepCollectionEquality().hash(_sortedSessionsByDate),const DeepCollectionEquality().hash(_sortedDailySessions),const DeepCollectionEquality().hash(_sortedPeriodSessions),dayTimeRangeText]);

@override
String toString() {
  return 'SleepDisplay(period: $period, isDay: $isDay, dailySessions: $dailySessions, dailySummary: $dailySummary, durationPoints: $durationPoints, previousDurationPoints: $previousDurationPoints, baselineDurationPoints: $baselineDurationPoints, overviewSummary: $overviewSummary, sessionsByDate: $sessionsByDate, periodSessions: $periodSessions, crossMetricHrvValues: $crossMetricHrvValues, nights: $nights, totalHours: $totalHours, averageHours: $averageHours, longestHours: $longestHours, previousAverageHours: $previousAverageHours, chartValues: $chartValues, scheduleDays: $scheduleDays, useScheduleChart: $useScheduleChart, stageShares: $stageShares, goalProgress: $goalProgress, periodComparison: $periodComparison, baselineInsight: $baselineInsight, targetInterpretation: $targetInterpretation, hrvInsight: $hrvInsight, dataConfidence: $dataConfidence, sortedSessionsByDate: $sortedSessionsByDate, sortedDailySessions: $sortedDailySessions, sortedPeriodSessions: $sortedPeriodSessions, dayTimeRangeText: $dayTimeRangeText)';
}


}

/// @nodoc
abstract mixin class _$SleepDisplayCopyWith<$Res> implements $SleepDisplayCopyWith<$Res> {
  factory _$SleepDisplayCopyWith(_SleepDisplay value, $Res Function(_SleepDisplay) _then) = __$SleepDisplayCopyWithImpl;
@override @useResult
$Res call({
 DatePeriod period, bool isDay, List<SleepData> dailySessions, SleepData? dailySummary, List<SleepDurationPoint> durationPoints, List<SleepDurationPoint> previousDurationPoints, List<SleepDurationPoint> baselineDurationPoints, SleepOverviewSummary overviewSummary, Map<LocalDate, List<SleepData>> sessionsByDate, List<SleepData> periodSessions, List<CrossMetricValue> crossMetricHrvValues, List<SleepDurationPoint> nights, double totalHours, double averageHours, double longestHours, double previousAverageHours, List<PeriodChartValue> chartValues, List<SleepScheduleDay> scheduleDays, bool useScheduleChart, List<SleepStageShare> stageShares, DailyGoalProgress goalProgress, PeriodComparison periodComparison, PersonalBaselineInsight? baselineInsight, SleepTargetInterpretation? targetInterpretation, CrossMetricInsight? hrvInsight, DataConfidence dataConfidence, Map<LocalDate, List<SleepData>> sortedSessionsByDate, List<SleepData> sortedDailySessions, List<SleepData> sortedPeriodSessions, String? dayTimeRangeText
});


@override $SleepDataCopyWith<$Res>? get dailySummary;@override $DailyGoalProgressCopyWith<$Res> get goalProgress;@override $PeriodComparisonCopyWith<$Res> get periodComparison;@override $PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;@override $SleepTargetInterpretationCopyWith<$Res>? get targetInterpretation;@override $CrossMetricInsightCopyWith<$Res>? get hrvInsight;@override $DataConfidenceCopyWith<$Res> get dataConfidence;

}
/// @nodoc
class __$SleepDisplayCopyWithImpl<$Res>
    implements _$SleepDisplayCopyWith<$Res> {
  __$SleepDisplayCopyWithImpl(this._self, this._then);

  final _SleepDisplay _self;
  final $Res Function(_SleepDisplay) _then;

/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? period = null,Object? isDay = null,Object? dailySessions = null,Object? dailySummary = freezed,Object? durationPoints = null,Object? previousDurationPoints = null,Object? baselineDurationPoints = null,Object? overviewSummary = null,Object? sessionsByDate = null,Object? periodSessions = null,Object? crossMetricHrvValues = null,Object? nights = null,Object? totalHours = null,Object? averageHours = null,Object? longestHours = null,Object? previousAverageHours = null,Object? chartValues = null,Object? scheduleDays = null,Object? useScheduleChart = null,Object? stageShares = null,Object? goalProgress = null,Object? periodComparison = null,Object? baselineInsight = freezed,Object? targetInterpretation = freezed,Object? hrvInsight = freezed,Object? dataConfidence = null,Object? sortedSessionsByDate = null,Object? sortedDailySessions = null,Object? sortedPeriodSessions = null,Object? dayTimeRangeText = freezed,}) {
  return _then(_SleepDisplay(
period: null == period ? _self.period : period // ignore: cast_nullable_to_non_nullable
as DatePeriod,isDay: null == isDay ? _self.isDay : isDay // ignore: cast_nullable_to_non_nullable
as bool,dailySessions: null == dailySessions ? _self._dailySessions : dailySessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailySummary: freezed == dailySummary ? _self.dailySummary : dailySummary // ignore: cast_nullable_to_non_nullable
as SleepData?,durationPoints: null == durationPoints ? _self._durationPoints : durationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,previousDurationPoints: null == previousDurationPoints ? _self._previousDurationPoints : previousDurationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,baselineDurationPoints: null == baselineDurationPoints ? _self._baselineDurationPoints : baselineDurationPoints // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,overviewSummary: null == overviewSummary ? _self.overviewSummary : overviewSummary // ignore: cast_nullable_to_non_nullable
as SleepOverviewSummary,sessionsByDate: null == sessionsByDate ? _self._sessionsByDate : sessionsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<SleepData>>,periodSessions: null == periodSessions ? _self._periodSessions : periodSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,crossMetricHrvValues: null == crossMetricHrvValues ? _self._crossMetricHrvValues : crossMetricHrvValues // ignore: cast_nullable_to_non_nullable
as List<CrossMetricValue>,nights: null == nights ? _self._nights : nights // ignore: cast_nullable_to_non_nullable
as List<SleepDurationPoint>,totalHours: null == totalHours ? _self.totalHours : totalHours // ignore: cast_nullable_to_non_nullable
as double,averageHours: null == averageHours ? _self.averageHours : averageHours // ignore: cast_nullable_to_non_nullable
as double,longestHours: null == longestHours ? _self.longestHours : longestHours // ignore: cast_nullable_to_non_nullable
as double,previousAverageHours: null == previousAverageHours ? _self.previousAverageHours : previousAverageHours // ignore: cast_nullable_to_non_nullable
as double,chartValues: null == chartValues ? _self._chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,scheduleDays: null == scheduleDays ? _self._scheduleDays : scheduleDays // ignore: cast_nullable_to_non_nullable
as List<SleepScheduleDay>,useScheduleChart: null == useScheduleChart ? _self.useScheduleChart : useScheduleChart // ignore: cast_nullable_to_non_nullable
as bool,stageShares: null == stageShares ? _self._stageShares : stageShares // ignore: cast_nullable_to_non_nullable
as List<SleepStageShare>,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,periodComparison: null == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,targetInterpretation: freezed == targetInterpretation ? _self.targetInterpretation : targetInterpretation // ignore: cast_nullable_to_non_nullable
as SleepTargetInterpretation?,hrvInsight: freezed == hrvInsight ? _self.hrvInsight : hrvInsight // ignore: cast_nullable_to_non_nullable
as CrossMetricInsight?,dataConfidence: null == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,sortedSessionsByDate: null == sortedSessionsByDate ? _self._sortedSessionsByDate : sortedSessionsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<SleepData>>,sortedDailySessions: null == sortedDailySessions ? _self._sortedDailySessions : sortedDailySessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,sortedPeriodSessions: null == sortedPeriodSessions ? _self._sortedPeriodSessions : sortedPeriodSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dayTimeRangeText: freezed == dayTimeRangeText ? _self.dayTimeRangeText : dayTimeRangeText // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get dailySummary {
    if (_self.dailySummary == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.dailySummary!, (value) {
    return _then(_self.copyWith(dailySummary: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get periodComparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight {
    if (_self.baselineInsight == null) {
    return null;
  }

  return $PersonalBaselineInsightCopyWith<$Res>(_self.baselineInsight!, (value) {
    return _then(_self.copyWith(baselineInsight: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepTargetInterpretationCopyWith<$Res>? get targetInterpretation {
    if (_self.targetInterpretation == null) {
    return null;
  }

  return $SleepTargetInterpretationCopyWith<$Res>(_self.targetInterpretation!, (value) {
    return _then(_self.copyWith(targetInterpretation: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CrossMetricInsightCopyWith<$Res>? get hrvInsight {
    if (_self.hrvInsight == null) {
    return null;
  }

  return $CrossMetricInsightCopyWith<$Res>(_self.hrvInsight!, (value) {
    return _then(_self.copyWith(hrvInsight: value));
  });
}/// Create a copy of SleepDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get dataConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.dataConfidence, (value) {
    return _then(_self.copyWith(dataConfidence: value));
  });
}
}

// dart format on
