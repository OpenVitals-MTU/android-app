// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activities_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivitiesState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; double get dailyGoalMinutes; int? get selectedActivityType; List<int> get availableActivityTypes; List<ExerciseData> get workouts; List<PlannedExerciseData> get plannedWorkouts; List<ExerciseData> get previousWorkouts; List<ExerciseData> get baselineWorkouts; List<ActivityTypeAggregate> get activityTypeAggregates; List<ActivityOverviewDay> get overviewDays; List<DailyRestingHR> get crossDailyRestingHR; ActivitiesDisplay? get display;
/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivitiesStateCopyWith<ActivitiesState> get copyWith => _$ActivitiesStateCopyWithImpl<ActivitiesState>(this as ActivitiesState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivitiesState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalMinutes, dailyGoalMinutes) || other.dailyGoalMinutes == dailyGoalMinutes)&&(identical(other.selectedActivityType, selectedActivityType) || other.selectedActivityType == selectedActivityType)&&const DeepCollectionEquality().equals(other.availableActivityTypes, availableActivityTypes)&&const DeepCollectionEquality().equals(other.workouts, workouts)&&const DeepCollectionEquality().equals(other.plannedWorkouts, plannedWorkouts)&&const DeepCollectionEquality().equals(other.previousWorkouts, previousWorkouts)&&const DeepCollectionEquality().equals(other.baselineWorkouts, baselineWorkouts)&&const DeepCollectionEquality().equals(other.activityTypeAggregates, activityTypeAggregates)&&const DeepCollectionEquality().equals(other.overviewDays, overviewDays)&&const DeepCollectionEquality().equals(other.crossDailyRestingHR, crossDailyRestingHR)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalMinutes,selectedActivityType,const DeepCollectionEquality().hash(availableActivityTypes),const DeepCollectionEquality().hash(workouts),const DeepCollectionEquality().hash(plannedWorkouts),const DeepCollectionEquality().hash(previousWorkouts),const DeepCollectionEquality().hash(baselineWorkouts),const DeepCollectionEquality().hash(activityTypeAggregates),const DeepCollectionEquality().hash(overviewDays),const DeepCollectionEquality().hash(crossDailyRestingHR),display);

@override
String toString() {
  return 'ActivitiesState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalMinutes: $dailyGoalMinutes, selectedActivityType: $selectedActivityType, availableActivityTypes: $availableActivityTypes, workouts: $workouts, plannedWorkouts: $plannedWorkouts, previousWorkouts: $previousWorkouts, baselineWorkouts: $baselineWorkouts, activityTypeAggregates: $activityTypeAggregates, overviewDays: $overviewDays, crossDailyRestingHR: $crossDailyRestingHR, display: $display)';
}


}

/// @nodoc
abstract mixin class $ActivitiesStateCopyWith<$Res>  {
  factory $ActivitiesStateCopyWith(ActivitiesState value, $Res Function(ActivitiesState) _then) = _$ActivitiesStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalMinutes, int? selectedActivityType, List<int> availableActivityTypes, List<ExerciseData> workouts, List<PlannedExerciseData> plannedWorkouts, List<ExerciseData> previousWorkouts, List<ExerciseData> baselineWorkouts, List<ActivityTypeAggregate> activityTypeAggregates, List<ActivityOverviewDay> overviewDays, List<DailyRestingHR> crossDailyRestingHR, ActivitiesDisplay? display
});


$ActivitiesDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$ActivitiesStateCopyWithImpl<$Res>
    implements $ActivitiesStateCopyWith<$Res> {
  _$ActivitiesStateCopyWithImpl(this._self, this._then);

  final ActivitiesState _self;
  final $Res Function(ActivitiesState) _then;

/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalMinutes = null,Object? selectedActivityType = freezed,Object? availableActivityTypes = null,Object? workouts = null,Object? plannedWorkouts = null,Object? previousWorkouts = null,Object? baselineWorkouts = null,Object? activityTypeAggregates = null,Object? overviewDays = null,Object? crossDailyRestingHR = null,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalMinutes: null == dailyGoalMinutes ? _self.dailyGoalMinutes : dailyGoalMinutes // ignore: cast_nullable_to_non_nullable
as double,selectedActivityType: freezed == selectedActivityType ? _self.selectedActivityType : selectedActivityType // ignore: cast_nullable_to_non_nullable
as int?,availableActivityTypes: null == availableActivityTypes ? _self.availableActivityTypes : availableActivityTypes // ignore: cast_nullable_to_non_nullable
as List<int>,workouts: null == workouts ? _self.workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,plannedWorkouts: null == plannedWorkouts ? _self.plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,previousWorkouts: null == previousWorkouts ? _self.previousWorkouts : previousWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,baselineWorkouts: null == baselineWorkouts ? _self.baselineWorkouts : baselineWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,activityTypeAggregates: null == activityTypeAggregates ? _self.activityTypeAggregates : activityTypeAggregates // ignore: cast_nullable_to_non_nullable
as List<ActivityTypeAggregate>,overviewDays: null == overviewDays ? _self.overviewDays : overviewDays // ignore: cast_nullable_to_non_nullable
as List<ActivityOverviewDay>,crossDailyRestingHR: null == crossDailyRestingHR ? _self.crossDailyRestingHR : crossDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as ActivitiesDisplay?,
  ));
}
/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivitiesDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $ActivitiesDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivitiesState].
extension ActivitiesStatePatterns on ActivitiesState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivitiesState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivitiesState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivitiesState value)  $default,){
final _that = this;
switch (_that) {
case _ActivitiesState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivitiesState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivitiesState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalMinutes,  int? selectedActivityType,  List<int> availableActivityTypes,  List<ExerciseData> workouts,  List<PlannedExerciseData> plannedWorkouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<ActivityTypeAggregate> activityTypeAggregates,  List<ActivityOverviewDay> overviewDays,  List<DailyRestingHR> crossDailyRestingHR,  ActivitiesDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivitiesState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalMinutes,_that.selectedActivityType,_that.availableActivityTypes,_that.workouts,_that.plannedWorkouts,_that.previousWorkouts,_that.baselineWorkouts,_that.activityTypeAggregates,_that.overviewDays,_that.crossDailyRestingHR,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalMinutes,  int? selectedActivityType,  List<int> availableActivityTypes,  List<ExerciseData> workouts,  List<PlannedExerciseData> plannedWorkouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<ActivityTypeAggregate> activityTypeAggregates,  List<ActivityOverviewDay> overviewDays,  List<DailyRestingHR> crossDailyRestingHR,  ActivitiesDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _ActivitiesState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalMinutes,_that.selectedActivityType,_that.availableActivityTypes,_that.workouts,_that.plannedWorkouts,_that.previousWorkouts,_that.baselineWorkouts,_that.activityTypeAggregates,_that.overviewDays,_that.crossDailyRestingHR,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalMinutes,  int? selectedActivityType,  List<int> availableActivityTypes,  List<ExerciseData> workouts,  List<PlannedExerciseData> plannedWorkouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<ActivityTypeAggregate> activityTypeAggregates,  List<ActivityOverviewDay> overviewDays,  List<DailyRestingHR> crossDailyRestingHR,  ActivitiesDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _ActivitiesState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalMinutes,_that.selectedActivityType,_that.availableActivityTypes,_that.workouts,_that.plannedWorkouts,_that.previousWorkouts,_that.baselineWorkouts,_that.activityTypeAggregates,_that.overviewDays,_that.crossDailyRestingHR,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _ActivitiesState extends ActivitiesState {
  const _ActivitiesState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.dailyGoalMinutes = 30.0, this.selectedActivityType, final  List<int> availableActivityTypes = const <int>[], final  List<ExerciseData> workouts = const <ExerciseData>[], final  List<PlannedExerciseData> plannedWorkouts = const <PlannedExerciseData>[], final  List<ExerciseData> previousWorkouts = const <ExerciseData>[], final  List<ExerciseData> baselineWorkouts = const <ExerciseData>[], final  List<ActivityTypeAggregate> activityTypeAggregates = const <ActivityTypeAggregate>[], final  List<ActivityOverviewDay> overviewDays = const <ActivityOverviewDay>[], final  List<DailyRestingHR> crossDailyRestingHR = const <DailyRestingHR>[], this.display}): _availableActivityTypes = availableActivityTypes,_workouts = workouts,_plannedWorkouts = plannedWorkouts,_previousWorkouts = previousWorkouts,_baselineWorkouts = baselineWorkouts,_activityTypeAggregates = activityTypeAggregates,_overviewDays = overviewDays,_crossDailyRestingHR = crossDailyRestingHR,super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override@JsonKey() final  double dailyGoalMinutes;
@override final  int? selectedActivityType;
 final  List<int> _availableActivityTypes;
@override@JsonKey() List<int> get availableActivityTypes {
  if (_availableActivityTypes is EqualUnmodifiableListView) return _availableActivityTypes;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_availableActivityTypes);
}

 final  List<ExerciseData> _workouts;
@override@JsonKey() List<ExerciseData> get workouts {
  if (_workouts is EqualUnmodifiableListView) return _workouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_workouts);
}

 final  List<PlannedExerciseData> _plannedWorkouts;
@override@JsonKey() List<PlannedExerciseData> get plannedWorkouts {
  if (_plannedWorkouts is EqualUnmodifiableListView) return _plannedWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_plannedWorkouts);
}

 final  List<ExerciseData> _previousWorkouts;
@override@JsonKey() List<ExerciseData> get previousWorkouts {
  if (_previousWorkouts is EqualUnmodifiableListView) return _previousWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousWorkouts);
}

 final  List<ExerciseData> _baselineWorkouts;
@override@JsonKey() List<ExerciseData> get baselineWorkouts {
  if (_baselineWorkouts is EqualUnmodifiableListView) return _baselineWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineWorkouts);
}

 final  List<ActivityTypeAggregate> _activityTypeAggregates;
@override@JsonKey() List<ActivityTypeAggregate> get activityTypeAggregates {
  if (_activityTypeAggregates is EqualUnmodifiableListView) return _activityTypeAggregates;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_activityTypeAggregates);
}

 final  List<ActivityOverviewDay> _overviewDays;
@override@JsonKey() List<ActivityOverviewDay> get overviewDays {
  if (_overviewDays is EqualUnmodifiableListView) return _overviewDays;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_overviewDays);
}

 final  List<DailyRestingHR> _crossDailyRestingHR;
@override@JsonKey() List<DailyRestingHR> get crossDailyRestingHR {
  if (_crossDailyRestingHR is EqualUnmodifiableListView) return _crossDailyRestingHR;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_crossDailyRestingHR);
}

@override final  ActivitiesDisplay? display;

/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivitiesStateCopyWith<_ActivitiesState> get copyWith => __$ActivitiesStateCopyWithImpl<_ActivitiesState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivitiesState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalMinutes, dailyGoalMinutes) || other.dailyGoalMinutes == dailyGoalMinutes)&&(identical(other.selectedActivityType, selectedActivityType) || other.selectedActivityType == selectedActivityType)&&const DeepCollectionEquality().equals(other._availableActivityTypes, _availableActivityTypes)&&const DeepCollectionEquality().equals(other._workouts, _workouts)&&const DeepCollectionEquality().equals(other._plannedWorkouts, _plannedWorkouts)&&const DeepCollectionEquality().equals(other._previousWorkouts, _previousWorkouts)&&const DeepCollectionEquality().equals(other._baselineWorkouts, _baselineWorkouts)&&const DeepCollectionEquality().equals(other._activityTypeAggregates, _activityTypeAggregates)&&const DeepCollectionEquality().equals(other._overviewDays, _overviewDays)&&const DeepCollectionEquality().equals(other._crossDailyRestingHR, _crossDailyRestingHR)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalMinutes,selectedActivityType,const DeepCollectionEquality().hash(_availableActivityTypes),const DeepCollectionEquality().hash(_workouts),const DeepCollectionEquality().hash(_plannedWorkouts),const DeepCollectionEquality().hash(_previousWorkouts),const DeepCollectionEquality().hash(_baselineWorkouts),const DeepCollectionEquality().hash(_activityTypeAggregates),const DeepCollectionEquality().hash(_overviewDays),const DeepCollectionEquality().hash(_crossDailyRestingHR),display);

@override
String toString() {
  return 'ActivitiesState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalMinutes: $dailyGoalMinutes, selectedActivityType: $selectedActivityType, availableActivityTypes: $availableActivityTypes, workouts: $workouts, plannedWorkouts: $plannedWorkouts, previousWorkouts: $previousWorkouts, baselineWorkouts: $baselineWorkouts, activityTypeAggregates: $activityTypeAggregates, overviewDays: $overviewDays, crossDailyRestingHR: $crossDailyRestingHR, display: $display)';
}


}

/// @nodoc
abstract mixin class _$ActivitiesStateCopyWith<$Res> implements $ActivitiesStateCopyWith<$Res> {
  factory _$ActivitiesStateCopyWith(_ActivitiesState value, $Res Function(_ActivitiesState) _then) = __$ActivitiesStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalMinutes, int? selectedActivityType, List<int> availableActivityTypes, List<ExerciseData> workouts, List<PlannedExerciseData> plannedWorkouts, List<ExerciseData> previousWorkouts, List<ExerciseData> baselineWorkouts, List<ActivityTypeAggregate> activityTypeAggregates, List<ActivityOverviewDay> overviewDays, List<DailyRestingHR> crossDailyRestingHR, ActivitiesDisplay? display
});


@override $ActivitiesDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$ActivitiesStateCopyWithImpl<$Res>
    implements _$ActivitiesStateCopyWith<$Res> {
  __$ActivitiesStateCopyWithImpl(this._self, this._then);

  final _ActivitiesState _self;
  final $Res Function(_ActivitiesState) _then;

/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalMinutes = null,Object? selectedActivityType = freezed,Object? availableActivityTypes = null,Object? workouts = null,Object? plannedWorkouts = null,Object? previousWorkouts = null,Object? baselineWorkouts = null,Object? activityTypeAggregates = null,Object? overviewDays = null,Object? crossDailyRestingHR = null,Object? display = freezed,}) {
  return _then(_ActivitiesState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalMinutes: null == dailyGoalMinutes ? _self.dailyGoalMinutes : dailyGoalMinutes // ignore: cast_nullable_to_non_nullable
as double,selectedActivityType: freezed == selectedActivityType ? _self.selectedActivityType : selectedActivityType // ignore: cast_nullable_to_non_nullable
as int?,availableActivityTypes: null == availableActivityTypes ? _self._availableActivityTypes : availableActivityTypes // ignore: cast_nullable_to_non_nullable
as List<int>,workouts: null == workouts ? _self._workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,plannedWorkouts: null == plannedWorkouts ? _self._plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,previousWorkouts: null == previousWorkouts ? _self._previousWorkouts : previousWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,baselineWorkouts: null == baselineWorkouts ? _self._baselineWorkouts : baselineWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,activityTypeAggregates: null == activityTypeAggregates ? _self._activityTypeAggregates : activityTypeAggregates // ignore: cast_nullable_to_non_nullable
as List<ActivityTypeAggregate>,overviewDays: null == overviewDays ? _self._overviewDays : overviewDays // ignore: cast_nullable_to_non_nullable
as List<ActivityOverviewDay>,crossDailyRestingHR: null == crossDailyRestingHR ? _self._crossDailyRestingHR : crossDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as ActivitiesDisplay?,
  ));
}

/// Create a copy of ActivitiesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivitiesDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $ActivitiesDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
