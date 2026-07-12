// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activities_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityOverviewTotals {

 int get steps; double get distanceMeters; double get energyBurnedKcal; bool get hasEnergyBurned; int get cardioLoad; bool get hasCardioLoad; CardioLoadConfidence get cardioLoadConfidence; double? get hrvRmssdMs;
/// Create a copy of ActivityOverviewTotals
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityOverviewTotalsCopyWith<ActivityOverviewTotals> get copyWith => _$ActivityOverviewTotalsCopyWithImpl<ActivityOverviewTotals>(this as ActivityOverviewTotals, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityOverviewTotals&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.energyBurnedKcal, energyBurnedKcal) || other.energyBurnedKcal == energyBurnedKcal)&&(identical(other.hasEnergyBurned, hasEnergyBurned) || other.hasEnergyBurned == hasEnergyBurned)&&(identical(other.cardioLoad, cardioLoad) || other.cardioLoad == cardioLoad)&&(identical(other.hasCardioLoad, hasCardioLoad) || other.hasCardioLoad == hasCardioLoad)&&(identical(other.cardioLoadConfidence, cardioLoadConfidence) || other.cardioLoadConfidence == cardioLoadConfidence)&&(identical(other.hrvRmssdMs, hrvRmssdMs) || other.hrvRmssdMs == hrvRmssdMs));
}


@override
int get hashCode => Object.hash(runtimeType,steps,distanceMeters,energyBurnedKcal,hasEnergyBurned,cardioLoad,hasCardioLoad,cardioLoadConfidence,hrvRmssdMs);

@override
String toString() {
  return 'ActivityOverviewTotals(steps: $steps, distanceMeters: $distanceMeters, energyBurnedKcal: $energyBurnedKcal, hasEnergyBurned: $hasEnergyBurned, cardioLoad: $cardioLoad, hasCardioLoad: $hasCardioLoad, cardioLoadConfidence: $cardioLoadConfidence, hrvRmssdMs: $hrvRmssdMs)';
}


}

/// @nodoc
abstract mixin class $ActivityOverviewTotalsCopyWith<$Res>  {
  factory $ActivityOverviewTotalsCopyWith(ActivityOverviewTotals value, $Res Function(ActivityOverviewTotals) _then) = _$ActivityOverviewTotalsCopyWithImpl;
@useResult
$Res call({
 int steps, double distanceMeters, double energyBurnedKcal, bool hasEnergyBurned, int cardioLoad, bool hasCardioLoad, CardioLoadConfidence cardioLoadConfidence, double? hrvRmssdMs
});




}
/// @nodoc
class _$ActivityOverviewTotalsCopyWithImpl<$Res>
    implements $ActivityOverviewTotalsCopyWith<$Res> {
  _$ActivityOverviewTotalsCopyWithImpl(this._self, this._then);

  final ActivityOverviewTotals _self;
  final $Res Function(ActivityOverviewTotals) _then;

/// Create a copy of ActivityOverviewTotals
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? steps = null,Object? distanceMeters = null,Object? energyBurnedKcal = null,Object? hasEnergyBurned = null,Object? cardioLoad = null,Object? hasCardioLoad = null,Object? cardioLoadConfidence = null,Object? hrvRmssdMs = freezed,}) {
  return _then(_self.copyWith(
steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,energyBurnedKcal: null == energyBurnedKcal ? _self.energyBurnedKcal : energyBurnedKcal // ignore: cast_nullable_to_non_nullable
as double,hasEnergyBurned: null == hasEnergyBurned ? _self.hasEnergyBurned : hasEnergyBurned // ignore: cast_nullable_to_non_nullable
as bool,cardioLoad: null == cardioLoad ? _self.cardioLoad : cardioLoad // ignore: cast_nullable_to_non_nullable
as int,hasCardioLoad: null == hasCardioLoad ? _self.hasCardioLoad : hasCardioLoad // ignore: cast_nullable_to_non_nullable
as bool,cardioLoadConfidence: null == cardioLoadConfidence ? _self.cardioLoadConfidence : cardioLoadConfidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,hrvRmssdMs: freezed == hrvRmssdMs ? _self.hrvRmssdMs : hrvRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityOverviewTotals].
extension ActivityOverviewTotalsPatterns on ActivityOverviewTotals {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityOverviewTotals value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityOverviewTotals() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityOverviewTotals value)  $default,){
final _that = this;
switch (_that) {
case _ActivityOverviewTotals():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityOverviewTotals value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityOverviewTotals() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int steps,  double distanceMeters,  double energyBurnedKcal,  bool hasEnergyBurned,  int cardioLoad,  bool hasCardioLoad,  CardioLoadConfidence cardioLoadConfidence,  double? hrvRmssdMs)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityOverviewTotals() when $default != null:
return $default(_that.steps,_that.distanceMeters,_that.energyBurnedKcal,_that.hasEnergyBurned,_that.cardioLoad,_that.hasCardioLoad,_that.cardioLoadConfidence,_that.hrvRmssdMs);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int steps,  double distanceMeters,  double energyBurnedKcal,  bool hasEnergyBurned,  int cardioLoad,  bool hasCardioLoad,  CardioLoadConfidence cardioLoadConfidence,  double? hrvRmssdMs)  $default,) {final _that = this;
switch (_that) {
case _ActivityOverviewTotals():
return $default(_that.steps,_that.distanceMeters,_that.energyBurnedKcal,_that.hasEnergyBurned,_that.cardioLoad,_that.hasCardioLoad,_that.cardioLoadConfidence,_that.hrvRmssdMs);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int steps,  double distanceMeters,  double energyBurnedKcal,  bool hasEnergyBurned,  int cardioLoad,  bool hasCardioLoad,  CardioLoadConfidence cardioLoadConfidence,  double? hrvRmssdMs)?  $default,) {final _that = this;
switch (_that) {
case _ActivityOverviewTotals() when $default != null:
return $default(_that.steps,_that.distanceMeters,_that.energyBurnedKcal,_that.hasEnergyBurned,_that.cardioLoad,_that.hasCardioLoad,_that.cardioLoadConfidence,_that.hrvRmssdMs);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityOverviewTotals implements ActivityOverviewTotals {
  const _ActivityOverviewTotals({required this.steps, required this.distanceMeters, required this.energyBurnedKcal, required this.hasEnergyBurned, required this.cardioLoad, required this.hasCardioLoad, required this.cardioLoadConfidence, required this.hrvRmssdMs});
  

@override final  int steps;
@override final  double distanceMeters;
@override final  double energyBurnedKcal;
@override final  bool hasEnergyBurned;
@override final  int cardioLoad;
@override final  bool hasCardioLoad;
@override final  CardioLoadConfidence cardioLoadConfidence;
@override final  double? hrvRmssdMs;

/// Create a copy of ActivityOverviewTotals
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityOverviewTotalsCopyWith<_ActivityOverviewTotals> get copyWith => __$ActivityOverviewTotalsCopyWithImpl<_ActivityOverviewTotals>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityOverviewTotals&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.energyBurnedKcal, energyBurnedKcal) || other.energyBurnedKcal == energyBurnedKcal)&&(identical(other.hasEnergyBurned, hasEnergyBurned) || other.hasEnergyBurned == hasEnergyBurned)&&(identical(other.cardioLoad, cardioLoad) || other.cardioLoad == cardioLoad)&&(identical(other.hasCardioLoad, hasCardioLoad) || other.hasCardioLoad == hasCardioLoad)&&(identical(other.cardioLoadConfidence, cardioLoadConfidence) || other.cardioLoadConfidence == cardioLoadConfidence)&&(identical(other.hrvRmssdMs, hrvRmssdMs) || other.hrvRmssdMs == hrvRmssdMs));
}


@override
int get hashCode => Object.hash(runtimeType,steps,distanceMeters,energyBurnedKcal,hasEnergyBurned,cardioLoad,hasCardioLoad,cardioLoadConfidence,hrvRmssdMs);

@override
String toString() {
  return 'ActivityOverviewTotals(steps: $steps, distanceMeters: $distanceMeters, energyBurnedKcal: $energyBurnedKcal, hasEnergyBurned: $hasEnergyBurned, cardioLoad: $cardioLoad, hasCardioLoad: $hasCardioLoad, cardioLoadConfidence: $cardioLoadConfidence, hrvRmssdMs: $hrvRmssdMs)';
}


}

/// @nodoc
abstract mixin class _$ActivityOverviewTotalsCopyWith<$Res> implements $ActivityOverviewTotalsCopyWith<$Res> {
  factory _$ActivityOverviewTotalsCopyWith(_ActivityOverviewTotals value, $Res Function(_ActivityOverviewTotals) _then) = __$ActivityOverviewTotalsCopyWithImpl;
@override @useResult
$Res call({
 int steps, double distanceMeters, double energyBurnedKcal, bool hasEnergyBurned, int cardioLoad, bool hasCardioLoad, CardioLoadConfidence cardioLoadConfidence, double? hrvRmssdMs
});




}
/// @nodoc
class __$ActivityOverviewTotalsCopyWithImpl<$Res>
    implements _$ActivityOverviewTotalsCopyWith<$Res> {
  __$ActivityOverviewTotalsCopyWithImpl(this._self, this._then);

  final _ActivityOverviewTotals _self;
  final $Res Function(_ActivityOverviewTotals) _then;

/// Create a copy of ActivityOverviewTotals
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? steps = null,Object? distanceMeters = null,Object? energyBurnedKcal = null,Object? hasEnergyBurned = null,Object? cardioLoad = null,Object? hasCardioLoad = null,Object? cardioLoadConfidence = null,Object? hrvRmssdMs = freezed,}) {
  return _then(_ActivityOverviewTotals(
steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,energyBurnedKcal: null == energyBurnedKcal ? _self.energyBurnedKcal : energyBurnedKcal // ignore: cast_nullable_to_non_nullable
as double,hasEnergyBurned: null == hasEnergyBurned ? _self.hasEnergyBurned : hasEnergyBurned // ignore: cast_nullable_to_non_nullable
as bool,cardioLoad: null == cardioLoad ? _self.cardioLoad : cardioLoad // ignore: cast_nullable_to_non_nullable
as int,hasCardioLoad: null == hasCardioLoad ? _self.hasCardioLoad : hasCardioLoad // ignore: cast_nullable_to_non_nullable
as bool,cardioLoadConfidence: null == cardioLoadConfidence ? _self.cardioLoadConfidence : cardioLoadConfidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,hrvRmssdMs: freezed == hrvRmssdMs ? _self.hrvRmssdMs : hrvRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$ActivityStripMarker {

 LocalDate get date; ExerciseData? get workout;
/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityStripMarkerCopyWith<ActivityStripMarker> get copyWith => _$ActivityStripMarkerCopyWithImpl<ActivityStripMarker>(this as ActivityStripMarker, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityStripMarker&&(identical(other.date, date) || other.date == date)&&(identical(other.workout, workout) || other.workout == workout));
}


@override
int get hashCode => Object.hash(runtimeType,date,workout);

@override
String toString() {
  return 'ActivityStripMarker(date: $date, workout: $workout)';
}


}

/// @nodoc
abstract mixin class $ActivityStripMarkerCopyWith<$Res>  {
  factory $ActivityStripMarkerCopyWith(ActivityStripMarker value, $Res Function(ActivityStripMarker) _then) = _$ActivityStripMarkerCopyWithImpl;
@useResult
$Res call({
 LocalDate date, ExerciseData? workout
});


$ExerciseDataCopyWith<$Res>? get workout;

}
/// @nodoc
class _$ActivityStripMarkerCopyWithImpl<$Res>
    implements $ActivityStripMarkerCopyWith<$Res> {
  _$ActivityStripMarkerCopyWithImpl(this._self, this._then);

  final ActivityStripMarker _self;
  final $Res Function(ActivityStripMarker) _then;

/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? workout = freezed,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,
  ));
}
/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDataCopyWith<$Res>? get workout {
    if (_self.workout == null) {
    return null;
  }

  return $ExerciseDataCopyWith<$Res>(_self.workout!, (value) {
    return _then(_self.copyWith(workout: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityStripMarker].
extension ActivityStripMarkerPatterns on ActivityStripMarker {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityStripMarker value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityStripMarker() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityStripMarker value)  $default,){
final _that = this;
switch (_that) {
case _ActivityStripMarker():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityStripMarker value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityStripMarker() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  ExerciseData? workout)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityStripMarker() when $default != null:
return $default(_that.date,_that.workout);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  ExerciseData? workout)  $default,) {final _that = this;
switch (_that) {
case _ActivityStripMarker():
return $default(_that.date,_that.workout);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  ExerciseData? workout)?  $default,) {final _that = this;
switch (_that) {
case _ActivityStripMarker() when $default != null:
return $default(_that.date,_that.workout);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityStripMarker implements ActivityStripMarker {
  const _ActivityStripMarker({required this.date, this.workout});
  

@override final  LocalDate date;
@override final  ExerciseData? workout;

/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityStripMarkerCopyWith<_ActivityStripMarker> get copyWith => __$ActivityStripMarkerCopyWithImpl<_ActivityStripMarker>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityStripMarker&&(identical(other.date, date) || other.date == date)&&(identical(other.workout, workout) || other.workout == workout));
}


@override
int get hashCode => Object.hash(runtimeType,date,workout);

@override
String toString() {
  return 'ActivityStripMarker(date: $date, workout: $workout)';
}


}

/// @nodoc
abstract mixin class _$ActivityStripMarkerCopyWith<$Res> implements $ActivityStripMarkerCopyWith<$Res> {
  factory _$ActivityStripMarkerCopyWith(_ActivityStripMarker value, $Res Function(_ActivityStripMarker) _then) = __$ActivityStripMarkerCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, ExerciseData? workout
});


@override $ExerciseDataCopyWith<$Res>? get workout;

}
/// @nodoc
class __$ActivityStripMarkerCopyWithImpl<$Res>
    implements _$ActivityStripMarkerCopyWith<$Res> {
  __$ActivityStripMarkerCopyWithImpl(this._self, this._then);

  final _ActivityStripMarker _self;
  final $Res Function(_ActivityStripMarker) _then;

/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? workout = freezed,}) {
  return _then(_ActivityStripMarker(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,
  ));
}

/// Create a copy of ActivityStripMarker
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDataCopyWith<$Res>? get workout {
    if (_self.workout == null) {
    return null;
  }

  return $ExerciseDataCopyWith<$Res>(_self.workout!, (value) {
    return _then(_self.copyWith(workout: value));
  });
}
}

/// @nodoc
mixin _$ActivitiesDisplay {

/// Whether the period has anything at all to show (Kotlin's empty gate).
 bool get hasAnyData; bool get hasOverviewDays;/// The activity-type dropdown's options, ordered by their label.
 List<int> get filterOptions;/// Null when the period has no overview day at all — which is what hides the
/// key-metrics section.
 ActivityOverviewTotals? get totals;/// The representative date of each sparkline bucket, in order. The view turns
/// these into (localized) labels; it does not bucket.
 List<LocalDate> get bucketDates;/// The week strip's markers. Empty for every range but WEEK.
 List<ActivityStripMarker> get stripMarkers; List<double> get cardioLoadSeries; List<double> get energyBurnedSeries; List<double> get stepsSeries; List<double> get distanceSeries; List<double> get hrvSeries;/// True when any day's energy is our own active+BMR estimate rather than a
/// recorded total — the calories card says so out loud.
 bool get energyEstimated;/// The workout-minutes bar series and its summary total.
 List<PeriodChartValue> get chartValues; int get totalDurationMs; DailyGoalProgress get goalProgress; int get workoutCount; int get averageDurationMs; int get longestDurationMs; PeriodComparison get periodComparison; PersonalBaselineInsight? get baselineInsight;/// The HHS 150-minute guideline. Null when there is nothing to compare.
 WorkoutGuidelineProgress? get guideline; bool get guidelineUsesWeeklyAverage; CrossMetricInsight? get crossInsight; DataConfidence get dataConfidence;/// The period's workouts indexed by their local start date — the chart's
/// selected-day list is a lookup, not a scan.
 Map<LocalDate, List<ExerciseData>> get workoutsByDay;/// The planned workouts, in the order the card lists them (earliest first).
 List<PlannedExerciseData> get sortedPlannedWorkouts;
/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivitiesDisplayCopyWith<ActivitiesDisplay> get copyWith => _$ActivitiesDisplayCopyWithImpl<ActivitiesDisplay>(this as ActivitiesDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivitiesDisplay&&(identical(other.hasAnyData, hasAnyData) || other.hasAnyData == hasAnyData)&&(identical(other.hasOverviewDays, hasOverviewDays) || other.hasOverviewDays == hasOverviewDays)&&const DeepCollectionEquality().equals(other.filterOptions, filterOptions)&&(identical(other.totals, totals) || other.totals == totals)&&const DeepCollectionEquality().equals(other.bucketDates, bucketDates)&&const DeepCollectionEquality().equals(other.stripMarkers, stripMarkers)&&const DeepCollectionEquality().equals(other.cardioLoadSeries, cardioLoadSeries)&&const DeepCollectionEquality().equals(other.energyBurnedSeries, energyBurnedSeries)&&const DeepCollectionEquality().equals(other.stepsSeries, stepsSeries)&&const DeepCollectionEquality().equals(other.distanceSeries, distanceSeries)&&const DeepCollectionEquality().equals(other.hrvSeries, hrvSeries)&&(identical(other.energyEstimated, energyEstimated) || other.energyEstimated == energyEstimated)&&const DeepCollectionEquality().equals(other.chartValues, chartValues)&&(identical(other.totalDurationMs, totalDurationMs) || other.totalDurationMs == totalDurationMs)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.workoutCount, workoutCount) || other.workoutCount == workoutCount)&&(identical(other.averageDurationMs, averageDurationMs) || other.averageDurationMs == averageDurationMs)&&(identical(other.longestDurationMs, longestDurationMs) || other.longestDurationMs == longestDurationMs)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.guideline, guideline) || other.guideline == guideline)&&(identical(other.guidelineUsesWeeklyAverage, guidelineUsesWeeklyAverage) || other.guidelineUsesWeeklyAverage == guidelineUsesWeeklyAverage)&&(identical(other.crossInsight, crossInsight) || other.crossInsight == crossInsight)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence)&&const DeepCollectionEquality().equals(other.workoutsByDay, workoutsByDay)&&const DeepCollectionEquality().equals(other.sortedPlannedWorkouts, sortedPlannedWorkouts));
}


@override
int get hashCode => Object.hashAll([runtimeType,hasAnyData,hasOverviewDays,const DeepCollectionEquality().hash(filterOptions),totals,const DeepCollectionEquality().hash(bucketDates),const DeepCollectionEquality().hash(stripMarkers),const DeepCollectionEquality().hash(cardioLoadSeries),const DeepCollectionEquality().hash(energyBurnedSeries),const DeepCollectionEquality().hash(stepsSeries),const DeepCollectionEquality().hash(distanceSeries),const DeepCollectionEquality().hash(hrvSeries),energyEstimated,const DeepCollectionEquality().hash(chartValues),totalDurationMs,goalProgress,workoutCount,averageDurationMs,longestDurationMs,periodComparison,baselineInsight,guideline,guidelineUsesWeeklyAverage,crossInsight,dataConfidence,const DeepCollectionEquality().hash(workoutsByDay),const DeepCollectionEquality().hash(sortedPlannedWorkouts)]);

@override
String toString() {
  return 'ActivitiesDisplay(hasAnyData: $hasAnyData, hasOverviewDays: $hasOverviewDays, filterOptions: $filterOptions, totals: $totals, bucketDates: $bucketDates, stripMarkers: $stripMarkers, cardioLoadSeries: $cardioLoadSeries, energyBurnedSeries: $energyBurnedSeries, stepsSeries: $stepsSeries, distanceSeries: $distanceSeries, hrvSeries: $hrvSeries, energyEstimated: $energyEstimated, chartValues: $chartValues, totalDurationMs: $totalDurationMs, goalProgress: $goalProgress, workoutCount: $workoutCount, averageDurationMs: $averageDurationMs, longestDurationMs: $longestDurationMs, periodComparison: $periodComparison, baselineInsight: $baselineInsight, guideline: $guideline, guidelineUsesWeeklyAverage: $guidelineUsesWeeklyAverage, crossInsight: $crossInsight, dataConfidence: $dataConfidence, workoutsByDay: $workoutsByDay, sortedPlannedWorkouts: $sortedPlannedWorkouts)';
}


}

/// @nodoc
abstract mixin class $ActivitiesDisplayCopyWith<$Res>  {
  factory $ActivitiesDisplayCopyWith(ActivitiesDisplay value, $Res Function(ActivitiesDisplay) _then) = _$ActivitiesDisplayCopyWithImpl;
@useResult
$Res call({
 bool hasAnyData, bool hasOverviewDays, List<int> filterOptions, ActivityOverviewTotals? totals, List<LocalDate> bucketDates, List<ActivityStripMarker> stripMarkers, List<double> cardioLoadSeries, List<double> energyBurnedSeries, List<double> stepsSeries, List<double> distanceSeries, List<double> hrvSeries, bool energyEstimated, List<PeriodChartValue> chartValues, int totalDurationMs, DailyGoalProgress goalProgress, int workoutCount, int averageDurationMs, int longestDurationMs, PeriodComparison periodComparison, PersonalBaselineInsight? baselineInsight, WorkoutGuidelineProgress? guideline, bool guidelineUsesWeeklyAverage, CrossMetricInsight? crossInsight, DataConfidence dataConfidence, Map<LocalDate, List<ExerciseData>> workoutsByDay, List<PlannedExerciseData> sortedPlannedWorkouts
});


$ActivityOverviewTotalsCopyWith<$Res>? get totals;$DailyGoalProgressCopyWith<$Res> get goalProgress;$PeriodComparisonCopyWith<$Res> get periodComparison;$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;$WorkoutGuidelineProgressCopyWith<$Res>? get guideline;$CrossMetricInsightCopyWith<$Res>? get crossInsight;$DataConfidenceCopyWith<$Res> get dataConfidence;

}
/// @nodoc
class _$ActivitiesDisplayCopyWithImpl<$Res>
    implements $ActivitiesDisplayCopyWith<$Res> {
  _$ActivitiesDisplayCopyWithImpl(this._self, this._then);

  final ActivitiesDisplay _self;
  final $Res Function(ActivitiesDisplay) _then;

/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? hasAnyData = null,Object? hasOverviewDays = null,Object? filterOptions = null,Object? totals = freezed,Object? bucketDates = null,Object? stripMarkers = null,Object? cardioLoadSeries = null,Object? energyBurnedSeries = null,Object? stepsSeries = null,Object? distanceSeries = null,Object? hrvSeries = null,Object? energyEstimated = null,Object? chartValues = null,Object? totalDurationMs = null,Object? goalProgress = null,Object? workoutCount = null,Object? averageDurationMs = null,Object? longestDurationMs = null,Object? periodComparison = null,Object? baselineInsight = freezed,Object? guideline = freezed,Object? guidelineUsesWeeklyAverage = null,Object? crossInsight = freezed,Object? dataConfidence = null,Object? workoutsByDay = null,Object? sortedPlannedWorkouts = null,}) {
  return _then(_self.copyWith(
hasAnyData: null == hasAnyData ? _self.hasAnyData : hasAnyData // ignore: cast_nullable_to_non_nullable
as bool,hasOverviewDays: null == hasOverviewDays ? _self.hasOverviewDays : hasOverviewDays // ignore: cast_nullable_to_non_nullable
as bool,filterOptions: null == filterOptions ? _self.filterOptions : filterOptions // ignore: cast_nullable_to_non_nullable
as List<int>,totals: freezed == totals ? _self.totals : totals // ignore: cast_nullable_to_non_nullable
as ActivityOverviewTotals?,bucketDates: null == bucketDates ? _self.bucketDates : bucketDates // ignore: cast_nullable_to_non_nullable
as List<LocalDate>,stripMarkers: null == stripMarkers ? _self.stripMarkers : stripMarkers // ignore: cast_nullable_to_non_nullable
as List<ActivityStripMarker>,cardioLoadSeries: null == cardioLoadSeries ? _self.cardioLoadSeries : cardioLoadSeries // ignore: cast_nullable_to_non_nullable
as List<double>,energyBurnedSeries: null == energyBurnedSeries ? _self.energyBurnedSeries : energyBurnedSeries // ignore: cast_nullable_to_non_nullable
as List<double>,stepsSeries: null == stepsSeries ? _self.stepsSeries : stepsSeries // ignore: cast_nullable_to_non_nullable
as List<double>,distanceSeries: null == distanceSeries ? _self.distanceSeries : distanceSeries // ignore: cast_nullable_to_non_nullable
as List<double>,hrvSeries: null == hrvSeries ? _self.hrvSeries : hrvSeries // ignore: cast_nullable_to_non_nullable
as List<double>,energyEstimated: null == energyEstimated ? _self.energyEstimated : energyEstimated // ignore: cast_nullable_to_non_nullable
as bool,chartValues: null == chartValues ? _self.chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,totalDurationMs: null == totalDurationMs ? _self.totalDurationMs : totalDurationMs // ignore: cast_nullable_to_non_nullable
as int,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,workoutCount: null == workoutCount ? _self.workoutCount : workoutCount // ignore: cast_nullable_to_non_nullable
as int,averageDurationMs: null == averageDurationMs ? _self.averageDurationMs : averageDurationMs // ignore: cast_nullable_to_non_nullable
as int,longestDurationMs: null == longestDurationMs ? _self.longestDurationMs : longestDurationMs // ignore: cast_nullable_to_non_nullable
as int,periodComparison: null == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,guideline: freezed == guideline ? _self.guideline : guideline // ignore: cast_nullable_to_non_nullable
as WorkoutGuidelineProgress?,guidelineUsesWeeklyAverage: null == guidelineUsesWeeklyAverage ? _self.guidelineUsesWeeklyAverage : guidelineUsesWeeklyAverage // ignore: cast_nullable_to_non_nullable
as bool,crossInsight: freezed == crossInsight ? _self.crossInsight : crossInsight // ignore: cast_nullable_to_non_nullable
as CrossMetricInsight?,dataConfidence: null == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,workoutsByDay: null == workoutsByDay ? _self.workoutsByDay : workoutsByDay // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<ExerciseData>>,sortedPlannedWorkouts: null == sortedPlannedWorkouts ? _self.sortedPlannedWorkouts : sortedPlannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,
  ));
}
/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityOverviewTotalsCopyWith<$Res>? get totals {
    if (_self.totals == null) {
    return null;
  }

  return $ActivityOverviewTotalsCopyWith<$Res>(_self.totals!, (value) {
    return _then(_self.copyWith(totals: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get periodComparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of ActivitiesDisplay
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
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$WorkoutGuidelineProgressCopyWith<$Res>? get guideline {
    if (_self.guideline == null) {
    return null;
  }

  return $WorkoutGuidelineProgressCopyWith<$Res>(_self.guideline!, (value) {
    return _then(_self.copyWith(guideline: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CrossMetricInsightCopyWith<$Res>? get crossInsight {
    if (_self.crossInsight == null) {
    return null;
  }

  return $CrossMetricInsightCopyWith<$Res>(_self.crossInsight!, (value) {
    return _then(_self.copyWith(crossInsight: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get dataConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.dataConfidence, (value) {
    return _then(_self.copyWith(dataConfidence: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivitiesDisplay].
extension ActivitiesDisplayPatterns on ActivitiesDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivitiesDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivitiesDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivitiesDisplay value)  $default,){
final _that = this;
switch (_that) {
case _ActivitiesDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivitiesDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _ActivitiesDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool hasAnyData,  bool hasOverviewDays,  List<int> filterOptions,  ActivityOverviewTotals? totals,  List<LocalDate> bucketDates,  List<ActivityStripMarker> stripMarkers,  List<double> cardioLoadSeries,  List<double> energyBurnedSeries,  List<double> stepsSeries,  List<double> distanceSeries,  List<double> hrvSeries,  bool energyEstimated,  List<PeriodChartValue> chartValues,  int totalDurationMs,  DailyGoalProgress goalProgress,  int workoutCount,  int averageDurationMs,  int longestDurationMs,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  WorkoutGuidelineProgress? guideline,  bool guidelineUsesWeeklyAverage,  CrossMetricInsight? crossInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<ExerciseData>> workoutsByDay,  List<PlannedExerciseData> sortedPlannedWorkouts)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivitiesDisplay() when $default != null:
return $default(_that.hasAnyData,_that.hasOverviewDays,_that.filterOptions,_that.totals,_that.bucketDates,_that.stripMarkers,_that.cardioLoadSeries,_that.energyBurnedSeries,_that.stepsSeries,_that.distanceSeries,_that.hrvSeries,_that.energyEstimated,_that.chartValues,_that.totalDurationMs,_that.goalProgress,_that.workoutCount,_that.averageDurationMs,_that.longestDurationMs,_that.periodComparison,_that.baselineInsight,_that.guideline,_that.guidelineUsesWeeklyAverage,_that.crossInsight,_that.dataConfidence,_that.workoutsByDay,_that.sortedPlannedWorkouts);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool hasAnyData,  bool hasOverviewDays,  List<int> filterOptions,  ActivityOverviewTotals? totals,  List<LocalDate> bucketDates,  List<ActivityStripMarker> stripMarkers,  List<double> cardioLoadSeries,  List<double> energyBurnedSeries,  List<double> stepsSeries,  List<double> distanceSeries,  List<double> hrvSeries,  bool energyEstimated,  List<PeriodChartValue> chartValues,  int totalDurationMs,  DailyGoalProgress goalProgress,  int workoutCount,  int averageDurationMs,  int longestDurationMs,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  WorkoutGuidelineProgress? guideline,  bool guidelineUsesWeeklyAverage,  CrossMetricInsight? crossInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<ExerciseData>> workoutsByDay,  List<PlannedExerciseData> sortedPlannedWorkouts)  $default,) {final _that = this;
switch (_that) {
case _ActivitiesDisplay():
return $default(_that.hasAnyData,_that.hasOverviewDays,_that.filterOptions,_that.totals,_that.bucketDates,_that.stripMarkers,_that.cardioLoadSeries,_that.energyBurnedSeries,_that.stepsSeries,_that.distanceSeries,_that.hrvSeries,_that.energyEstimated,_that.chartValues,_that.totalDurationMs,_that.goalProgress,_that.workoutCount,_that.averageDurationMs,_that.longestDurationMs,_that.periodComparison,_that.baselineInsight,_that.guideline,_that.guidelineUsesWeeklyAverage,_that.crossInsight,_that.dataConfidence,_that.workoutsByDay,_that.sortedPlannedWorkouts);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool hasAnyData,  bool hasOverviewDays,  List<int> filterOptions,  ActivityOverviewTotals? totals,  List<LocalDate> bucketDates,  List<ActivityStripMarker> stripMarkers,  List<double> cardioLoadSeries,  List<double> energyBurnedSeries,  List<double> stepsSeries,  List<double> distanceSeries,  List<double> hrvSeries,  bool energyEstimated,  List<PeriodChartValue> chartValues,  int totalDurationMs,  DailyGoalProgress goalProgress,  int workoutCount,  int averageDurationMs,  int longestDurationMs,  PeriodComparison periodComparison,  PersonalBaselineInsight? baselineInsight,  WorkoutGuidelineProgress? guideline,  bool guidelineUsesWeeklyAverage,  CrossMetricInsight? crossInsight,  DataConfidence dataConfidence,  Map<LocalDate, List<ExerciseData>> workoutsByDay,  List<PlannedExerciseData> sortedPlannedWorkouts)?  $default,) {final _that = this;
switch (_that) {
case _ActivitiesDisplay() when $default != null:
return $default(_that.hasAnyData,_that.hasOverviewDays,_that.filterOptions,_that.totals,_that.bucketDates,_that.stripMarkers,_that.cardioLoadSeries,_that.energyBurnedSeries,_that.stepsSeries,_that.distanceSeries,_that.hrvSeries,_that.energyEstimated,_that.chartValues,_that.totalDurationMs,_that.goalProgress,_that.workoutCount,_that.averageDurationMs,_that.longestDurationMs,_that.periodComparison,_that.baselineInsight,_that.guideline,_that.guidelineUsesWeeklyAverage,_that.crossInsight,_that.dataConfidence,_that.workoutsByDay,_that.sortedPlannedWorkouts);case _:
  return null;

}
}

}

/// @nodoc


class _ActivitiesDisplay implements ActivitiesDisplay {
  const _ActivitiesDisplay({required this.hasAnyData, required this.hasOverviewDays, required final  List<int> filterOptions, required this.totals, required final  List<LocalDate> bucketDates, required final  List<ActivityStripMarker> stripMarkers, required final  List<double> cardioLoadSeries, required final  List<double> energyBurnedSeries, required final  List<double> stepsSeries, required final  List<double> distanceSeries, required final  List<double> hrvSeries, required this.energyEstimated, required final  List<PeriodChartValue> chartValues, required this.totalDurationMs, required this.goalProgress, required this.workoutCount, required this.averageDurationMs, required this.longestDurationMs, required this.periodComparison, required this.baselineInsight, required this.guideline, required this.guidelineUsesWeeklyAverage, required this.crossInsight, required this.dataConfidence, required final  Map<LocalDate, List<ExerciseData>> workoutsByDay, required final  List<PlannedExerciseData> sortedPlannedWorkouts}): _filterOptions = filterOptions,_bucketDates = bucketDates,_stripMarkers = stripMarkers,_cardioLoadSeries = cardioLoadSeries,_energyBurnedSeries = energyBurnedSeries,_stepsSeries = stepsSeries,_distanceSeries = distanceSeries,_hrvSeries = hrvSeries,_chartValues = chartValues,_workoutsByDay = workoutsByDay,_sortedPlannedWorkouts = sortedPlannedWorkouts;
  

/// Whether the period has anything at all to show (Kotlin's empty gate).
@override final  bool hasAnyData;
@override final  bool hasOverviewDays;
/// The activity-type dropdown's options, ordered by their label.
 final  List<int> _filterOptions;
/// The activity-type dropdown's options, ordered by their label.
@override List<int> get filterOptions {
  if (_filterOptions is EqualUnmodifiableListView) return _filterOptions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_filterOptions);
}

/// Null when the period has no overview day at all — which is what hides the
/// key-metrics section.
@override final  ActivityOverviewTotals? totals;
/// The representative date of each sparkline bucket, in order. The view turns
/// these into (localized) labels; it does not bucket.
 final  List<LocalDate> _bucketDates;
/// The representative date of each sparkline bucket, in order. The view turns
/// these into (localized) labels; it does not bucket.
@override List<LocalDate> get bucketDates {
  if (_bucketDates is EqualUnmodifiableListView) return _bucketDates;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_bucketDates);
}

/// The week strip's markers. Empty for every range but WEEK.
 final  List<ActivityStripMarker> _stripMarkers;
/// The week strip's markers. Empty for every range but WEEK.
@override List<ActivityStripMarker> get stripMarkers {
  if (_stripMarkers is EqualUnmodifiableListView) return _stripMarkers;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stripMarkers);
}

 final  List<double> _cardioLoadSeries;
@override List<double> get cardioLoadSeries {
  if (_cardioLoadSeries is EqualUnmodifiableListView) return _cardioLoadSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cardioLoadSeries);
}

 final  List<double> _energyBurnedSeries;
@override List<double> get energyBurnedSeries {
  if (_energyBurnedSeries is EqualUnmodifiableListView) return _energyBurnedSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_energyBurnedSeries);
}

 final  List<double> _stepsSeries;
@override List<double> get stepsSeries {
  if (_stepsSeries is EqualUnmodifiableListView) return _stepsSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stepsSeries);
}

 final  List<double> _distanceSeries;
@override List<double> get distanceSeries {
  if (_distanceSeries is EqualUnmodifiableListView) return _distanceSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_distanceSeries);
}

 final  List<double> _hrvSeries;
@override List<double> get hrvSeries {
  if (_hrvSeries is EqualUnmodifiableListView) return _hrvSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_hrvSeries);
}

/// True when any day's energy is our own active+BMR estimate rather than a
/// recorded total — the calories card says so out loud.
@override final  bool energyEstimated;
/// The workout-minutes bar series and its summary total.
 final  List<PeriodChartValue> _chartValues;
/// The workout-minutes bar series and its summary total.
@override List<PeriodChartValue> get chartValues {
  if (_chartValues is EqualUnmodifiableListView) return _chartValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_chartValues);
}

@override final  int totalDurationMs;
@override final  DailyGoalProgress goalProgress;
@override final  int workoutCount;
@override final  int averageDurationMs;
@override final  int longestDurationMs;
@override final  PeriodComparison periodComparison;
@override final  PersonalBaselineInsight? baselineInsight;
/// The HHS 150-minute guideline. Null when there is nothing to compare.
@override final  WorkoutGuidelineProgress? guideline;
@override final  bool guidelineUsesWeeklyAverage;
@override final  CrossMetricInsight? crossInsight;
@override final  DataConfidence dataConfidence;
/// The period's workouts indexed by their local start date — the chart's
/// selected-day list is a lookup, not a scan.
 final  Map<LocalDate, List<ExerciseData>> _workoutsByDay;
/// The period's workouts indexed by their local start date — the chart's
/// selected-day list is a lookup, not a scan.
@override Map<LocalDate, List<ExerciseData>> get workoutsByDay {
  if (_workoutsByDay is EqualUnmodifiableMapView) return _workoutsByDay;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_workoutsByDay);
}

/// The planned workouts, in the order the card lists them (earliest first).
 final  List<PlannedExerciseData> _sortedPlannedWorkouts;
/// The planned workouts, in the order the card lists them (earliest first).
@override List<PlannedExerciseData> get sortedPlannedWorkouts {
  if (_sortedPlannedWorkouts is EqualUnmodifiableListView) return _sortedPlannedWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sortedPlannedWorkouts);
}


/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivitiesDisplayCopyWith<_ActivitiesDisplay> get copyWith => __$ActivitiesDisplayCopyWithImpl<_ActivitiesDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivitiesDisplay&&(identical(other.hasAnyData, hasAnyData) || other.hasAnyData == hasAnyData)&&(identical(other.hasOverviewDays, hasOverviewDays) || other.hasOverviewDays == hasOverviewDays)&&const DeepCollectionEquality().equals(other._filterOptions, _filterOptions)&&(identical(other.totals, totals) || other.totals == totals)&&const DeepCollectionEquality().equals(other._bucketDates, _bucketDates)&&const DeepCollectionEquality().equals(other._stripMarkers, _stripMarkers)&&const DeepCollectionEquality().equals(other._cardioLoadSeries, _cardioLoadSeries)&&const DeepCollectionEquality().equals(other._energyBurnedSeries, _energyBurnedSeries)&&const DeepCollectionEquality().equals(other._stepsSeries, _stepsSeries)&&const DeepCollectionEquality().equals(other._distanceSeries, _distanceSeries)&&const DeepCollectionEquality().equals(other._hrvSeries, _hrvSeries)&&(identical(other.energyEstimated, energyEstimated) || other.energyEstimated == energyEstimated)&&const DeepCollectionEquality().equals(other._chartValues, _chartValues)&&(identical(other.totalDurationMs, totalDurationMs) || other.totalDurationMs == totalDurationMs)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.workoutCount, workoutCount) || other.workoutCount == workoutCount)&&(identical(other.averageDurationMs, averageDurationMs) || other.averageDurationMs == averageDurationMs)&&(identical(other.longestDurationMs, longestDurationMs) || other.longestDurationMs == longestDurationMs)&&(identical(other.periodComparison, periodComparison) || other.periodComparison == periodComparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.guideline, guideline) || other.guideline == guideline)&&(identical(other.guidelineUsesWeeklyAverage, guidelineUsesWeeklyAverage) || other.guidelineUsesWeeklyAverage == guidelineUsesWeeklyAverage)&&(identical(other.crossInsight, crossInsight) || other.crossInsight == crossInsight)&&(identical(other.dataConfidence, dataConfidence) || other.dataConfidence == dataConfidence)&&const DeepCollectionEquality().equals(other._workoutsByDay, _workoutsByDay)&&const DeepCollectionEquality().equals(other._sortedPlannedWorkouts, _sortedPlannedWorkouts));
}


@override
int get hashCode => Object.hashAll([runtimeType,hasAnyData,hasOverviewDays,const DeepCollectionEquality().hash(_filterOptions),totals,const DeepCollectionEquality().hash(_bucketDates),const DeepCollectionEquality().hash(_stripMarkers),const DeepCollectionEquality().hash(_cardioLoadSeries),const DeepCollectionEquality().hash(_energyBurnedSeries),const DeepCollectionEquality().hash(_stepsSeries),const DeepCollectionEquality().hash(_distanceSeries),const DeepCollectionEquality().hash(_hrvSeries),energyEstimated,const DeepCollectionEquality().hash(_chartValues),totalDurationMs,goalProgress,workoutCount,averageDurationMs,longestDurationMs,periodComparison,baselineInsight,guideline,guidelineUsesWeeklyAverage,crossInsight,dataConfidence,const DeepCollectionEquality().hash(_workoutsByDay),const DeepCollectionEquality().hash(_sortedPlannedWorkouts)]);

@override
String toString() {
  return 'ActivitiesDisplay(hasAnyData: $hasAnyData, hasOverviewDays: $hasOverviewDays, filterOptions: $filterOptions, totals: $totals, bucketDates: $bucketDates, stripMarkers: $stripMarkers, cardioLoadSeries: $cardioLoadSeries, energyBurnedSeries: $energyBurnedSeries, stepsSeries: $stepsSeries, distanceSeries: $distanceSeries, hrvSeries: $hrvSeries, energyEstimated: $energyEstimated, chartValues: $chartValues, totalDurationMs: $totalDurationMs, goalProgress: $goalProgress, workoutCount: $workoutCount, averageDurationMs: $averageDurationMs, longestDurationMs: $longestDurationMs, periodComparison: $periodComparison, baselineInsight: $baselineInsight, guideline: $guideline, guidelineUsesWeeklyAverage: $guidelineUsesWeeklyAverage, crossInsight: $crossInsight, dataConfidence: $dataConfidence, workoutsByDay: $workoutsByDay, sortedPlannedWorkouts: $sortedPlannedWorkouts)';
}


}

/// @nodoc
abstract mixin class _$ActivitiesDisplayCopyWith<$Res> implements $ActivitiesDisplayCopyWith<$Res> {
  factory _$ActivitiesDisplayCopyWith(_ActivitiesDisplay value, $Res Function(_ActivitiesDisplay) _then) = __$ActivitiesDisplayCopyWithImpl;
@override @useResult
$Res call({
 bool hasAnyData, bool hasOverviewDays, List<int> filterOptions, ActivityOverviewTotals? totals, List<LocalDate> bucketDates, List<ActivityStripMarker> stripMarkers, List<double> cardioLoadSeries, List<double> energyBurnedSeries, List<double> stepsSeries, List<double> distanceSeries, List<double> hrvSeries, bool energyEstimated, List<PeriodChartValue> chartValues, int totalDurationMs, DailyGoalProgress goalProgress, int workoutCount, int averageDurationMs, int longestDurationMs, PeriodComparison periodComparison, PersonalBaselineInsight? baselineInsight, WorkoutGuidelineProgress? guideline, bool guidelineUsesWeeklyAverage, CrossMetricInsight? crossInsight, DataConfidence dataConfidence, Map<LocalDate, List<ExerciseData>> workoutsByDay, List<PlannedExerciseData> sortedPlannedWorkouts
});


@override $ActivityOverviewTotalsCopyWith<$Res>? get totals;@override $DailyGoalProgressCopyWith<$Res> get goalProgress;@override $PeriodComparisonCopyWith<$Res> get periodComparison;@override $PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;@override $WorkoutGuidelineProgressCopyWith<$Res>? get guideline;@override $CrossMetricInsightCopyWith<$Res>? get crossInsight;@override $DataConfidenceCopyWith<$Res> get dataConfidence;

}
/// @nodoc
class __$ActivitiesDisplayCopyWithImpl<$Res>
    implements _$ActivitiesDisplayCopyWith<$Res> {
  __$ActivitiesDisplayCopyWithImpl(this._self, this._then);

  final _ActivitiesDisplay _self;
  final $Res Function(_ActivitiesDisplay) _then;

/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? hasAnyData = null,Object? hasOverviewDays = null,Object? filterOptions = null,Object? totals = freezed,Object? bucketDates = null,Object? stripMarkers = null,Object? cardioLoadSeries = null,Object? energyBurnedSeries = null,Object? stepsSeries = null,Object? distanceSeries = null,Object? hrvSeries = null,Object? energyEstimated = null,Object? chartValues = null,Object? totalDurationMs = null,Object? goalProgress = null,Object? workoutCount = null,Object? averageDurationMs = null,Object? longestDurationMs = null,Object? periodComparison = null,Object? baselineInsight = freezed,Object? guideline = freezed,Object? guidelineUsesWeeklyAverage = null,Object? crossInsight = freezed,Object? dataConfidence = null,Object? workoutsByDay = null,Object? sortedPlannedWorkouts = null,}) {
  return _then(_ActivitiesDisplay(
hasAnyData: null == hasAnyData ? _self.hasAnyData : hasAnyData // ignore: cast_nullable_to_non_nullable
as bool,hasOverviewDays: null == hasOverviewDays ? _self.hasOverviewDays : hasOverviewDays // ignore: cast_nullable_to_non_nullable
as bool,filterOptions: null == filterOptions ? _self._filterOptions : filterOptions // ignore: cast_nullable_to_non_nullable
as List<int>,totals: freezed == totals ? _self.totals : totals // ignore: cast_nullable_to_non_nullable
as ActivityOverviewTotals?,bucketDates: null == bucketDates ? _self._bucketDates : bucketDates // ignore: cast_nullable_to_non_nullable
as List<LocalDate>,stripMarkers: null == stripMarkers ? _self._stripMarkers : stripMarkers // ignore: cast_nullable_to_non_nullable
as List<ActivityStripMarker>,cardioLoadSeries: null == cardioLoadSeries ? _self._cardioLoadSeries : cardioLoadSeries // ignore: cast_nullable_to_non_nullable
as List<double>,energyBurnedSeries: null == energyBurnedSeries ? _self._energyBurnedSeries : energyBurnedSeries // ignore: cast_nullable_to_non_nullable
as List<double>,stepsSeries: null == stepsSeries ? _self._stepsSeries : stepsSeries // ignore: cast_nullable_to_non_nullable
as List<double>,distanceSeries: null == distanceSeries ? _self._distanceSeries : distanceSeries // ignore: cast_nullable_to_non_nullable
as List<double>,hrvSeries: null == hrvSeries ? _self._hrvSeries : hrvSeries // ignore: cast_nullable_to_non_nullable
as List<double>,energyEstimated: null == energyEstimated ? _self.energyEstimated : energyEstimated // ignore: cast_nullable_to_non_nullable
as bool,chartValues: null == chartValues ? _self._chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,totalDurationMs: null == totalDurationMs ? _self.totalDurationMs : totalDurationMs // ignore: cast_nullable_to_non_nullable
as int,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,workoutCount: null == workoutCount ? _self.workoutCount : workoutCount // ignore: cast_nullable_to_non_nullable
as int,averageDurationMs: null == averageDurationMs ? _self.averageDurationMs : averageDurationMs // ignore: cast_nullable_to_non_nullable
as int,longestDurationMs: null == longestDurationMs ? _self.longestDurationMs : longestDurationMs // ignore: cast_nullable_to_non_nullable
as int,periodComparison: null == periodComparison ? _self.periodComparison : periodComparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,guideline: freezed == guideline ? _self.guideline : guideline // ignore: cast_nullable_to_non_nullable
as WorkoutGuidelineProgress?,guidelineUsesWeeklyAverage: null == guidelineUsesWeeklyAverage ? _self.guidelineUsesWeeklyAverage : guidelineUsesWeeklyAverage // ignore: cast_nullable_to_non_nullable
as bool,crossInsight: freezed == crossInsight ? _self.crossInsight : crossInsight // ignore: cast_nullable_to_non_nullable
as CrossMetricInsight?,dataConfidence: null == dataConfidence ? _self.dataConfidence : dataConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,workoutsByDay: null == workoutsByDay ? _self._workoutsByDay : workoutsByDay // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<ExerciseData>>,sortedPlannedWorkouts: null == sortedPlannedWorkouts ? _self._sortedPlannedWorkouts : sortedPlannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,
  ));
}

/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityOverviewTotalsCopyWith<$Res>? get totals {
    if (_self.totals == null) {
    return null;
  }

  return $ActivityOverviewTotalsCopyWith<$Res>(_self.totals!, (value) {
    return _then(_self.copyWith(totals: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get periodComparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.periodComparison, (value) {
    return _then(_self.copyWith(periodComparison: value));
  });
}/// Create a copy of ActivitiesDisplay
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
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$WorkoutGuidelineProgressCopyWith<$Res>? get guideline {
    if (_self.guideline == null) {
    return null;
  }

  return $WorkoutGuidelineProgressCopyWith<$Res>(_self.guideline!, (value) {
    return _then(_self.copyWith(guideline: value));
  });
}/// Create a copy of ActivitiesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CrossMetricInsightCopyWith<$Res>? get crossInsight {
    if (_self.crossInsight == null) {
    return null;
  }

  return $CrossMetricInsightCopyWith<$Res>(_self.crossInsight!, (value) {
    return _then(_self.copyWith(crossInsight: value));
  });
}/// Create a copy of ActivitiesDisplay
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
