// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_metric_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityIntradayPoint {

 DateTime get time; double get value;
/// Create a copy of ActivityIntradayPoint
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityIntradayPointCopyWith<ActivityIntradayPoint> get copyWith => _$ActivityIntradayPointCopyWithImpl<ActivityIntradayPoint>(this as ActivityIntradayPoint, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityIntradayPoint&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,time,value);

@override
String toString() {
  return 'ActivityIntradayPoint(time: $time, value: $value)';
}


}

/// @nodoc
abstract mixin class $ActivityIntradayPointCopyWith<$Res>  {
  factory $ActivityIntradayPointCopyWith(ActivityIntradayPoint value, $Res Function(ActivityIntradayPoint) _then) = _$ActivityIntradayPointCopyWithImpl;
@useResult
$Res call({
 DateTime time, double value
});




}
/// @nodoc
class _$ActivityIntradayPointCopyWithImpl<$Res>
    implements $ActivityIntradayPointCopyWith<$Res> {
  _$ActivityIntradayPointCopyWithImpl(this._self, this._then);

  final ActivityIntradayPoint _self;
  final $Res Function(ActivityIntradayPoint) _then;

/// Create a copy of ActivityIntradayPoint
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? value = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityIntradayPoint].
extension ActivityIntradayPointPatterns on ActivityIntradayPoint {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityIntradayPoint value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityIntradayPoint() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityIntradayPoint value)  $default,){
final _that = this;
switch (_that) {
case _ActivityIntradayPoint():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityIntradayPoint value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityIntradayPoint() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double value)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityIntradayPoint() when $default != null:
return $default(_that.time,_that.value);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double value)  $default,) {final _that = this;
switch (_that) {
case _ActivityIntradayPoint():
return $default(_that.time,_that.value);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double value)?  $default,) {final _that = this;
switch (_that) {
case _ActivityIntradayPoint() when $default != null:
return $default(_that.time,_that.value);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityIntradayPoint implements ActivityIntradayPoint {
  const _ActivityIntradayPoint({required this.time, required this.value});
  

@override final  DateTime time;
@override final  double value;

/// Create a copy of ActivityIntradayPoint
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityIntradayPointCopyWith<_ActivityIntradayPoint> get copyWith => __$ActivityIntradayPointCopyWithImpl<_ActivityIntradayPoint>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityIntradayPoint&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,time,value);

@override
String toString() {
  return 'ActivityIntradayPoint(time: $time, value: $value)';
}


}

/// @nodoc
abstract mixin class _$ActivityIntradayPointCopyWith<$Res> implements $ActivityIntradayPointCopyWith<$Res> {
  factory _$ActivityIntradayPointCopyWith(_ActivityIntradayPoint value, $Res Function(_ActivityIntradayPoint) _then) = __$ActivityIntradayPointCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double value
});




}
/// @nodoc
class __$ActivityIntradayPointCopyWithImpl<$Res>
    implements _$ActivityIntradayPointCopyWith<$Res> {
  __$ActivityIntradayPointCopyWithImpl(this._self, this._then);

  final _ActivityIntradayPoint _self;
  final $Res Function(_ActivityIntradayPoint) _then;

/// Create a copy of ActivityIntradayPoint
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? value = null,}) {
  return _then(_ActivityIntradayPoint(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$ActivityMetricDisplay {

 bool get hasData; List<double> get values; List<DailyGoalValue> get goalValues; List<LocalDate> get trackedDates; int get sampleCount; double get previousTotal; List<BaselineValue> get baselineValues; int get activeDays; DailyGoalProgress? get goalProgress; PeriodComparison? get periodComparison; double get baselineCurrentValue; List<ActivityIntradayPoint> get intradayPoints; double get dayTotal;/// The period total, its best day and its per-active-day average — folded
/// once here, not on every rebuild of the statistics grid.
 double get total; double get best; double get dailyAverage;/// The baseline comparison the statistics grid prints. Null when the
/// baseline window is too thin to say anything.
 PersonalBaselineInsight? get baselineInsight;/// The day rows the entries section lists — the days that carry a value, in
/// the order the list prints them (newest first).
 List<DailyGoalValue> get entryValues;/// The dated bar series for the period chart.
 List<PeriodChartValue> get chartValues;/// How much of the period the readings actually cover.
 DataConfidence? get dataConfidence;
/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityMetricDisplayCopyWith<ActivityMetricDisplay> get copyWith => _$ActivityMetricDisplayCopyWithImpl<ActivityMetricDisplay>(this as ActivityMetricDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityMetricDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&const DeepCollectionEquality().equals(other.values, values)&&const DeepCollectionEquality().equals(other.goalValues, goalValues)&&const DeepCollectionEquality().equals(other.trackedDates, trackedDates)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount)&&(identical(other.previousTotal, previousTotal) || other.previousTotal == previousTotal)&&const DeepCollectionEquality().equals(other.baselineValues, baselineValues)&&(identical(other.activeDays, activeDays) || other.activeDays == activeDays)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineCurrentValue, baselineCurrentValue) || other.baselineCurrentValue == baselineCurrentValue)&&const DeepCollectionEquality().equals(other.intradayPoints, intradayPoints)&&(identical(other.dayTotal, dayTotal) || other.dayTotal == dayTotal)&&(identical(other.total, total) || other.total == total)&&(identical(other.best, best) || other.best == best)&&(identical(other.dailyAverage, dailyAverage) || other.dailyAverage == dailyAverage)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&const DeepCollectionEquality().equals(other.entryValues, entryValues)&&const DeepCollectionEquality().equals(other.chartValues, chartValues)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence));
}


@override
int get hashCode => Object.hashAll([runtimeType,hasData,const DeepCollectionEquality().hash(values),const DeepCollectionEquality().hash(goalValues),const DeepCollectionEquality().hash(trackedDates),sampleCount,previousTotal,const DeepCollectionEquality().hash(baselineValues),activeDays,goalProgress,periodComparison,baselineCurrentValue,const DeepCollectionEquality().hash(intradayPoints),dayTotal,total,best,dailyAverage,baselineInsight,const DeepCollectionEquality().hash(entryValues),const DeepCollectionEquality().hash(chartValues),dataConfidence]);

@override
String toString() {
  return 'ActivityMetricDisplay(hasData: $hasData, values: $values, goalValues: $goalValues, trackedDates: $trackedDates, sampleCount: $sampleCount, previousTotal: $previousTotal, baselineValues: $baselineValues, activeDays: $activeDays, goalProgress: $goalProgress, periodComparison: $periodComparison, baselineCurrentValue: $baselineCurrentValue, intradayPoints: $intradayPoints, dayTotal: $dayTotal, total: $total, best: $best, dailyAverage: $dailyAverage, baselineInsight: $baselineInsight, entryValues: $entryValues, chartValues: $chartValues, dataConfidence: $dataConfidence)';
}


}

/// @nodoc
abstract mixin class $ActivityMetricDisplayCopyWith<$Res>  {
  factory $ActivityMetricDisplayCopyWith(ActivityMetricDisplay value, $Res Function(ActivityMetricDisplay) _then) = _$ActivityMetricDisplayCopyWithImpl;
@useResult
$Res call({
 bool hasData, List<double> values, List<DailyGoalValue> goalValues, List<LocalDate> trackedDates, int sampleCount, double previousTotal, List<BaselineValue> baselineValues, int activeDays, DailyGoalProgress? goalProgress, PeriodComparison? periodComparison, double baselineCurrentValue, List<ActivityIntradayPoint> intradayPoints, double dayTotal, double total, double best, double dailyAverage, PersonalBaselineInsight? baselineInsight, List<DailyGoalValue> entryValues, List<PeriodChartValue> chartValues, DataConfidence? dataConfidence
});


$DailyGoalProgressCopyWith<$Res>? get goalProgress;$PeriodComparisonCopyWith<$Res>? get periodComparison;$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;$DataConfidenceCopyWith<$Res>? get dataConfidence;

}
/// @nodoc
class _$ActivityMetricDisplayCopyWithImpl<$Res>
    implements $ActivityMetricDisplayCopyWith<$Res> {
  _$ActivityMetricDisplayCopyWithImpl(this._self, this._then);

  final ActivityMetricDisplay _self;
  final $Res Function(ActivityMetricDisplay) _then;

/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? hasData = null,Object? values = null,Object? goalValues = null,Object? trackedDates = null,Object? sampleCount = null,Object? previousTotal = null,Object? baselineValues = null,Object? activeDays = null,Object? goalProgress = freezed,Object? periodComparison = freezed,Object? baselineCurrentValue = null,Object? intradayPoints = null,Object? dayTotal = null,Object? total = null,Object? best = null,Object? dailyAverage = null,Object? baselineInsight = freezed,Object? entryValues = null,Object? chartValues = null,Object? dataConfidence = freezed,}) {
  return _then(_self.copyWith(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,values: null == values ? _self.values : values // ignore: cast_nullable_to_non_nullable
as List<double>,goalValues: null == goalValues ? _self.goalValues : goalValues // ignore: cast_nullable_to_non_nullable
as List<DailyGoalValue>,trackedDates: null == trackedDates ? _self.trackedDates : trackedDates // ignore: cast_nullable_to_non_nullable
as List<LocalDate>,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,previousTotal: null == previousTotal ? _self.previousTotal : previousTotal // ignore: cast_nullable_to_non_nullable
as double,baselineValues: null == baselineValues ? _self.baselineValues : baselineValues // ignore: cast_nullable_to_non_nullable
as List<BaselineValue>,activeDays: null == activeDays ? _self.activeDays : activeDays // ignore: cast_nullable_to_non_nullable
as int,goalProgress: freezed == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress?,periodComparison: freezed == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison?,baselineCurrentValue: null == baselineCurrentValue ? _self.baselineCurrentValue : baselineCurrentValue // ignore: cast_nullable_to_non_nullable
as double,intradayPoints: null == intradayPoints ? _self.intradayPoints : intradayPoints // ignore: cast_nullable_to_non_nullable
as List<ActivityIntradayPoint>,dayTotal: null == dayTotal ? _self.dayTotal : dayTotal // ignore: cast_nullable_to_non_nullable
as double,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,best: null == best ? _self.best : best // ignore: cast_nullable_to_non_nullable
as double,dailyAverage: null == dailyAverage ? _self.dailyAverage : dailyAverage // ignore: cast_nullable_to_non_nullable
as double,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,entryValues: null == entryValues ? _self.entryValues : entryValues // ignore: cast_nullable_to_non_nullable
as List<DailyGoalValue>,chartValues: null == chartValues ? _self.chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,dataConfidence: freezed == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence?,
  ));
}
/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res>? get goalProgress {
    if (_self.goalProgress == null) {
    return null;
  }

  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress!, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res>? get periodComparison {
    if (_self.periodComparison == null) {
    return null;
  }

  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison!, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of ActivityMetricDisplay
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
}/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res>? get dataConfidence {
    if (_self.dataConfidence == null) {
    return null;
  }

  return $DataConfidenceCopyWith<$Res>(_self.dataConfidence!, (value) {
    return _then(_self.copyWith(dataConfidence: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityMetricDisplay].
extension ActivityMetricDisplayPatterns on ActivityMetricDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityMetricDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityMetricDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityMetricDisplay value)  $default,){
final _that = this;
switch (_that) {
case _ActivityMetricDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityMetricDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityMetricDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool hasData,  List<double> values,  List<DailyGoalValue> goalValues,  List<LocalDate> trackedDates,  int sampleCount,  double previousTotal,  List<BaselineValue> baselineValues,  int activeDays,  DailyGoalProgress? goalProgress,  PeriodComparison? periodComparison,  double baselineCurrentValue,  List<ActivityIntradayPoint> intradayPoints,  double dayTotal,  double total,  double best,  double dailyAverage,  PersonalBaselineInsight? baselineInsight,  List<DailyGoalValue> entryValues,  List<PeriodChartValue> chartValues,  DataConfidence? dataConfidence)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityMetricDisplay() when $default != null:
return $default(_that.hasData,_that.values,_that.goalValues,_that.trackedDates,_that.sampleCount,_that.previousTotal,_that.baselineValues,_that.activeDays,_that.goalProgress,_that.periodComparison,_that.baselineCurrentValue,_that.intradayPoints,_that.dayTotal,_that.total,_that.best,_that.dailyAverage,_that.baselineInsight,_that.entryValues,_that.chartValues,_that.dataConfidence);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool hasData,  List<double> values,  List<DailyGoalValue> goalValues,  List<LocalDate> trackedDates,  int sampleCount,  double previousTotal,  List<BaselineValue> baselineValues,  int activeDays,  DailyGoalProgress? goalProgress,  PeriodComparison? periodComparison,  double baselineCurrentValue,  List<ActivityIntradayPoint> intradayPoints,  double dayTotal,  double total,  double best,  double dailyAverage,  PersonalBaselineInsight? baselineInsight,  List<DailyGoalValue> entryValues,  List<PeriodChartValue> chartValues,  DataConfidence? dataConfidence)  $default,) {final _that = this;
switch (_that) {
case _ActivityMetricDisplay():
return $default(_that.hasData,_that.values,_that.goalValues,_that.trackedDates,_that.sampleCount,_that.previousTotal,_that.baselineValues,_that.activeDays,_that.goalProgress,_that.periodComparison,_that.baselineCurrentValue,_that.intradayPoints,_that.dayTotal,_that.total,_that.best,_that.dailyAverage,_that.baselineInsight,_that.entryValues,_that.chartValues,_that.dataConfidence);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool hasData,  List<double> values,  List<DailyGoalValue> goalValues,  List<LocalDate> trackedDates,  int sampleCount,  double previousTotal,  List<BaselineValue> baselineValues,  int activeDays,  DailyGoalProgress? goalProgress,  PeriodComparison? periodComparison,  double baselineCurrentValue,  List<ActivityIntradayPoint> intradayPoints,  double dayTotal,  double total,  double best,  double dailyAverage,  PersonalBaselineInsight? baselineInsight,  List<DailyGoalValue> entryValues,  List<PeriodChartValue> chartValues,  DataConfidence? dataConfidence)?  $default,) {final _that = this;
switch (_that) {
case _ActivityMetricDisplay() when $default != null:
return $default(_that.hasData,_that.values,_that.goalValues,_that.trackedDates,_that.sampleCount,_that.previousTotal,_that.baselineValues,_that.activeDays,_that.goalProgress,_that.periodComparison,_that.baselineCurrentValue,_that.intradayPoints,_that.dayTotal,_that.total,_that.best,_that.dailyAverage,_that.baselineInsight,_that.entryValues,_that.chartValues,_that.dataConfidence);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityMetricDisplay implements ActivityMetricDisplay {
  const _ActivityMetricDisplay({this.hasData = false, final  List<double> values = const <double>[], final  List<DailyGoalValue> goalValues = const <DailyGoalValue>[], final  List<LocalDate> trackedDates = const <LocalDate>[], this.sampleCount = 0, this.previousTotal = 0.0, final  List<BaselineValue> baselineValues = const <BaselineValue>[], this.activeDays = 0, this.goalProgress, this.periodComparison, this.baselineCurrentValue = 0.0, final  List<ActivityIntradayPoint> intradayPoints = const <ActivityIntradayPoint>[], this.dayTotal = 0.0, this.total = 0.0, this.best = 0.0, this.dailyAverage = 0.0, this.baselineInsight, final  List<DailyGoalValue> entryValues = const <DailyGoalValue>[], final  List<PeriodChartValue> chartValues = const <PeriodChartValue>[], this.dataConfidence}): _values = values,_goalValues = goalValues,_trackedDates = trackedDates,_baselineValues = baselineValues,_intradayPoints = intradayPoints,_entryValues = entryValues,_chartValues = chartValues;
  

@override@JsonKey() final  bool hasData;
 final  List<double> _values;
@override@JsonKey() List<double> get values {
  if (_values is EqualUnmodifiableListView) return _values;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_values);
}

 final  List<DailyGoalValue> _goalValues;
@override@JsonKey() List<DailyGoalValue> get goalValues {
  if (_goalValues is EqualUnmodifiableListView) return _goalValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_goalValues);
}

 final  List<LocalDate> _trackedDates;
@override@JsonKey() List<LocalDate> get trackedDates {
  if (_trackedDates is EqualUnmodifiableListView) return _trackedDates;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_trackedDates);
}

@override@JsonKey() final  int sampleCount;
@override@JsonKey() final  double previousTotal;
 final  List<BaselineValue> _baselineValues;
@override@JsonKey() List<BaselineValue> get baselineValues {
  if (_baselineValues is EqualUnmodifiableListView) return _baselineValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineValues);
}

@override@JsonKey() final  int activeDays;
@override final  DailyGoalProgress? goalProgress;
@override final  PeriodComparison? periodComparison;
@override@JsonKey() final  double baselineCurrentValue;
 final  List<ActivityIntradayPoint> _intradayPoints;
@override@JsonKey() List<ActivityIntradayPoint> get intradayPoints {
  if (_intradayPoints is EqualUnmodifiableListView) return _intradayPoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_intradayPoints);
}

@override@JsonKey() final  double dayTotal;
/// The period total, its best day and its per-active-day average — folded
/// once here, not on every rebuild of the statistics grid.
@override@JsonKey() final  double total;
@override@JsonKey() final  double best;
@override@JsonKey() final  double dailyAverage;
/// The baseline comparison the statistics grid prints. Null when the
/// baseline window is too thin to say anything.
@override final  PersonalBaselineInsight? baselineInsight;
/// The day rows the entries section lists — the days that carry a value, in
/// the order the list prints them (newest first).
 final  List<DailyGoalValue> _entryValues;
/// The day rows the entries section lists — the days that carry a value, in
/// the order the list prints them (newest first).
@override@JsonKey() List<DailyGoalValue> get entryValues {
  if (_entryValues is EqualUnmodifiableListView) return _entryValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entryValues);
}

/// The dated bar series for the period chart.
 final  List<PeriodChartValue> _chartValues;
/// The dated bar series for the period chart.
@override@JsonKey() List<PeriodChartValue> get chartValues {
  if (_chartValues is EqualUnmodifiableListView) return _chartValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_chartValues);
}

/// How much of the period the readings actually cover.
@override final  DataConfidence? dataConfidence;

/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityMetricDisplayCopyWith<_ActivityMetricDisplay> get copyWith => __$ActivityMetricDisplayCopyWithImpl<_ActivityMetricDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityMetricDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&const DeepCollectionEquality().equals(other._values, _values)&&const DeepCollectionEquality().equals(other._goalValues, _goalValues)&&const DeepCollectionEquality().equals(other._trackedDates, _trackedDates)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount)&&(identical(other.previousTotal, previousTotal) || other.previousTotal == previousTotal)&&const DeepCollectionEquality().equals(other._baselineValues, _baselineValues)&&(identical(other.activeDays, activeDays) || other.activeDays == activeDays)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineCurrentValue, baselineCurrentValue) || other.baselineCurrentValue == baselineCurrentValue)&&const DeepCollectionEquality().equals(other._intradayPoints, _intradayPoints)&&(identical(other.dayTotal, dayTotal) || other.dayTotal == dayTotal)&&(identical(other.total, total) || other.total == total)&&(identical(other.best, best) || other.best == best)&&(identical(other.dailyAverage, dailyAverage) || other.dailyAverage == dailyAverage)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&const DeepCollectionEquality().equals(other._entryValues, _entryValues)&&const DeepCollectionEquality().equals(other._chartValues, _chartValues)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence));
}


@override
int get hashCode => Object.hashAll([runtimeType,hasData,const DeepCollectionEquality().hash(_values),const DeepCollectionEquality().hash(_goalValues),const DeepCollectionEquality().hash(_trackedDates),sampleCount,previousTotal,const DeepCollectionEquality().hash(_baselineValues),activeDays,goalProgress,periodComparison,baselineCurrentValue,const DeepCollectionEquality().hash(_intradayPoints),dayTotal,total,best,dailyAverage,baselineInsight,const DeepCollectionEquality().hash(_entryValues),const DeepCollectionEquality().hash(_chartValues),dataConfidence]);

@override
String toString() {
  return 'ActivityMetricDisplay(hasData: $hasData, values: $values, goalValues: $goalValues, trackedDates: $trackedDates, sampleCount: $sampleCount, previousTotal: $previousTotal, baselineValues: $baselineValues, activeDays: $activeDays, goalProgress: $goalProgress, periodComparison: $periodComparison, baselineCurrentValue: $baselineCurrentValue, intradayPoints: $intradayPoints, dayTotal: $dayTotal, total: $total, best: $best, dailyAverage: $dailyAverage, baselineInsight: $baselineInsight, entryValues: $entryValues, chartValues: $chartValues, dataConfidence: $dataConfidence)';
}


}

/// @nodoc
abstract mixin class _$ActivityMetricDisplayCopyWith<$Res> implements $ActivityMetricDisplayCopyWith<$Res> {
  factory _$ActivityMetricDisplayCopyWith(_ActivityMetricDisplay value, $Res Function(_ActivityMetricDisplay) _then) = __$ActivityMetricDisplayCopyWithImpl;
@override @useResult
$Res call({
 bool hasData, List<double> values, List<DailyGoalValue> goalValues, List<LocalDate> trackedDates, int sampleCount, double previousTotal, List<BaselineValue> baselineValues, int activeDays, DailyGoalProgress? goalProgress, PeriodComparison? periodComparison, double baselineCurrentValue, List<ActivityIntradayPoint> intradayPoints, double dayTotal, double total, double best, double dailyAverage, PersonalBaselineInsight? baselineInsight, List<DailyGoalValue> entryValues, List<PeriodChartValue> chartValues, DataConfidence? dataConfidence
});


@override $DailyGoalProgressCopyWith<$Res>? get goalProgress;@override $PeriodComparisonCopyWith<$Res>? get periodComparison;@override $PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;@override $DataConfidenceCopyWith<$Res>? get dataConfidence;

}
/// @nodoc
class __$ActivityMetricDisplayCopyWithImpl<$Res>
    implements _$ActivityMetricDisplayCopyWith<$Res> {
  __$ActivityMetricDisplayCopyWithImpl(this._self, this._then);

  final _ActivityMetricDisplay _self;
  final $Res Function(_ActivityMetricDisplay) _then;

/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? hasData = null,Object? values = null,Object? goalValues = null,Object? trackedDates = null,Object? sampleCount = null,Object? previousTotal = null,Object? baselineValues = null,Object? activeDays = null,Object? goalProgress = freezed,Object? periodComparison = freezed,Object? baselineCurrentValue = null,Object? intradayPoints = null,Object? dayTotal = null,Object? total = null,Object? best = null,Object? dailyAverage = null,Object? baselineInsight = freezed,Object? entryValues = null,Object? chartValues = null,Object? dataConfidence = freezed,}) {
  return _then(_ActivityMetricDisplay(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,values: null == values ? _self._values : values // ignore: cast_nullable_to_non_nullable
as List<double>,goalValues: null == goalValues ? _self._goalValues : goalValues // ignore: cast_nullable_to_non_nullable
as List<DailyGoalValue>,trackedDates: null == trackedDates ? _self._trackedDates : trackedDates // ignore: cast_nullable_to_non_nullable
as List<LocalDate>,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,previousTotal: null == previousTotal ? _self.previousTotal : previousTotal // ignore: cast_nullable_to_non_nullable
as double,baselineValues: null == baselineValues ? _self._baselineValues : baselineValues // ignore: cast_nullable_to_non_nullable
as List<BaselineValue>,activeDays: null == activeDays ? _self.activeDays : activeDays // ignore: cast_nullable_to_non_nullable
as int,goalProgress: freezed == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress?,periodComparison: freezed == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison?,baselineCurrentValue: null == baselineCurrentValue ? _self.baselineCurrentValue : baselineCurrentValue // ignore: cast_nullable_to_non_nullable
as double,intradayPoints: null == intradayPoints ? _self._intradayPoints : intradayPoints // ignore: cast_nullable_to_non_nullable
as List<ActivityIntradayPoint>,dayTotal: null == dayTotal ? _self.dayTotal : dayTotal // ignore: cast_nullable_to_non_nullable
as double,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,best: null == best ? _self.best : best // ignore: cast_nullable_to_non_nullable
as double,dailyAverage: null == dailyAverage ? _self.dailyAverage : dailyAverage // ignore: cast_nullable_to_non_nullable
as double,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,entryValues: null == entryValues ? _self._entryValues : entryValues // ignore: cast_nullable_to_non_nullable
as List<DailyGoalValue>,chartValues: null == chartValues ? _self._chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,dataConfidence: freezed == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence?,
  ));
}

/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res>? get goalProgress {
    if (_self.goalProgress == null) {
    return null;
  }

  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress!, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res>? get periodComparison {
    if (_self.periodComparison == null) {
    return null;
  }

  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison!, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of ActivityMetricDisplay
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
}/// Create a copy of ActivityMetricDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res>? get dataConfidence {
    if (_self.dataConfidence == null) {
    return null;
  }

  return $DataConfidenceCopyWith<$Res>(_self.dataConfidence!, (value) {
    return _then(_self.copyWith(dataConfidence: value));
  });
}
}

// dart format on
