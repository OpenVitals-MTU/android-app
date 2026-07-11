// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityPeriodData {

 List<DailySteps> get dailySteps; List<DailySteps> get previousDailySteps; List<DailySteps> get baselineDailySteps; List<DailyNutrition> get nutrition; List<DailyNutrition> get previousNutrition; List<DailyNutrition> get baselineNutrition; List<ActivityProgressPoint> get activityProgress;
/// Create a copy of ActivityPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityPeriodDataCopyWith<ActivityPeriodData> get copyWith => _$ActivityPeriodDataCopyWithImpl<ActivityPeriodData>(this as ActivityPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityPeriodData&&const DeepCollectionEquality().equals(other.dailySteps, dailySteps)&&const DeepCollectionEquality().equals(other.previousDailySteps, previousDailySteps)&&const DeepCollectionEquality().equals(other.baselineDailySteps, baselineDailySteps)&&const DeepCollectionEquality().equals(other.nutrition, nutrition)&&const DeepCollectionEquality().equals(other.previousNutrition, previousNutrition)&&const DeepCollectionEquality().equals(other.baselineNutrition, baselineNutrition)&&const DeepCollectionEquality().equals(other.activityProgress, activityProgress));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(dailySteps),const DeepCollectionEquality().hash(previousDailySteps),const DeepCollectionEquality().hash(baselineDailySteps),const DeepCollectionEquality().hash(nutrition),const DeepCollectionEquality().hash(previousNutrition),const DeepCollectionEquality().hash(baselineNutrition),const DeepCollectionEquality().hash(activityProgress));

@override
String toString() {
  return 'ActivityPeriodData(dailySteps: $dailySteps, previousDailySteps: $previousDailySteps, baselineDailySteps: $baselineDailySteps, nutrition: $nutrition, previousNutrition: $previousNutrition, baselineNutrition: $baselineNutrition, activityProgress: $activityProgress)';
}


}

/// @nodoc
abstract mixin class $ActivityPeriodDataCopyWith<$Res>  {
  factory $ActivityPeriodDataCopyWith(ActivityPeriodData value, $Res Function(ActivityPeriodData) _then) = _$ActivityPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<DailySteps> dailySteps, List<DailySteps> previousDailySteps, List<DailySteps> baselineDailySteps, List<DailyNutrition> nutrition, List<DailyNutrition> previousNutrition, List<DailyNutrition> baselineNutrition, List<ActivityProgressPoint> activityProgress
});




}
/// @nodoc
class _$ActivityPeriodDataCopyWithImpl<$Res>
    implements $ActivityPeriodDataCopyWith<$Res> {
  _$ActivityPeriodDataCopyWithImpl(this._self, this._then);

  final ActivityPeriodData _self;
  final $Res Function(ActivityPeriodData) _then;

/// Create a copy of ActivityPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? dailySteps = null,Object? previousDailySteps = null,Object? baselineDailySteps = null,Object? nutrition = null,Object? previousNutrition = null,Object? baselineNutrition = null,Object? activityProgress = null,}) {
  return _then(_self.copyWith(
dailySteps: null == dailySteps ? _self.dailySteps : dailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,previousDailySteps: null == previousDailySteps ? _self.previousDailySteps : previousDailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,baselineDailySteps: null == baselineDailySteps ? _self.baselineDailySteps : baselineDailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,nutrition: null == nutrition ? _self.nutrition : nutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,previousNutrition: null == previousNutrition ? _self.previousNutrition : previousNutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,baselineNutrition: null == baselineNutrition ? _self.baselineNutrition : baselineNutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,activityProgress: null == activityProgress ? _self.activityProgress : activityProgress // ignore: cast_nullable_to_non_nullable
as List<ActivityProgressPoint>,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityPeriodData].
extension ActivityPeriodDataPatterns on ActivityPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _ActivityPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<DailySteps> dailySteps,  List<DailySteps> previousDailySteps,  List<DailySteps> baselineDailySteps,  List<DailyNutrition> nutrition,  List<DailyNutrition> previousNutrition,  List<DailyNutrition> baselineNutrition,  List<ActivityProgressPoint> activityProgress)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityPeriodData() when $default != null:
return $default(_that.dailySteps,_that.previousDailySteps,_that.baselineDailySteps,_that.nutrition,_that.previousNutrition,_that.baselineNutrition,_that.activityProgress);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<DailySteps> dailySteps,  List<DailySteps> previousDailySteps,  List<DailySteps> baselineDailySteps,  List<DailyNutrition> nutrition,  List<DailyNutrition> previousNutrition,  List<DailyNutrition> baselineNutrition,  List<ActivityProgressPoint> activityProgress)  $default,) {final _that = this;
switch (_that) {
case _ActivityPeriodData():
return $default(_that.dailySteps,_that.previousDailySteps,_that.baselineDailySteps,_that.nutrition,_that.previousNutrition,_that.baselineNutrition,_that.activityProgress);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<DailySteps> dailySteps,  List<DailySteps> previousDailySteps,  List<DailySteps> baselineDailySteps,  List<DailyNutrition> nutrition,  List<DailyNutrition> previousNutrition,  List<DailyNutrition> baselineNutrition,  List<ActivityProgressPoint> activityProgress)?  $default,) {final _that = this;
switch (_that) {
case _ActivityPeriodData() when $default != null:
return $default(_that.dailySteps,_that.previousDailySteps,_that.baselineDailySteps,_that.nutrition,_that.previousNutrition,_that.baselineNutrition,_that.activityProgress);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityPeriodData implements ActivityPeriodData {
  const _ActivityPeriodData({final  List<DailySteps> dailySteps = const <DailySteps>[], final  List<DailySteps> previousDailySteps = const <DailySteps>[], final  List<DailySteps> baselineDailySteps = const <DailySteps>[], final  List<DailyNutrition> nutrition = const <DailyNutrition>[], final  List<DailyNutrition> previousNutrition = const <DailyNutrition>[], final  List<DailyNutrition> baselineNutrition = const <DailyNutrition>[], final  List<ActivityProgressPoint> activityProgress = const <ActivityProgressPoint>[]}): _dailySteps = dailySteps,_previousDailySteps = previousDailySteps,_baselineDailySteps = baselineDailySteps,_nutrition = nutrition,_previousNutrition = previousNutrition,_baselineNutrition = baselineNutrition,_activityProgress = activityProgress;
  

 final  List<DailySteps> _dailySteps;
@override@JsonKey() List<DailySteps> get dailySteps {
  if (_dailySteps is EqualUnmodifiableListView) return _dailySteps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailySteps);
}

 final  List<DailySteps> _previousDailySteps;
@override@JsonKey() List<DailySteps> get previousDailySteps {
  if (_previousDailySteps is EqualUnmodifiableListView) return _previousDailySteps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailySteps);
}

 final  List<DailySteps> _baselineDailySteps;
@override@JsonKey() List<DailySteps> get baselineDailySteps {
  if (_baselineDailySteps is EqualUnmodifiableListView) return _baselineDailySteps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailySteps);
}

 final  List<DailyNutrition> _nutrition;
@override@JsonKey() List<DailyNutrition> get nutrition {
  if (_nutrition is EqualUnmodifiableListView) return _nutrition;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_nutrition);
}

 final  List<DailyNutrition> _previousNutrition;
@override@JsonKey() List<DailyNutrition> get previousNutrition {
  if (_previousNutrition is EqualUnmodifiableListView) return _previousNutrition;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousNutrition);
}

 final  List<DailyNutrition> _baselineNutrition;
@override@JsonKey() List<DailyNutrition> get baselineNutrition {
  if (_baselineNutrition is EqualUnmodifiableListView) return _baselineNutrition;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineNutrition);
}

 final  List<ActivityProgressPoint> _activityProgress;
@override@JsonKey() List<ActivityProgressPoint> get activityProgress {
  if (_activityProgress is EqualUnmodifiableListView) return _activityProgress;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_activityProgress);
}


/// Create a copy of ActivityPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityPeriodDataCopyWith<_ActivityPeriodData> get copyWith => __$ActivityPeriodDataCopyWithImpl<_ActivityPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityPeriodData&&const DeepCollectionEquality().equals(other._dailySteps, _dailySteps)&&const DeepCollectionEquality().equals(other._previousDailySteps, _previousDailySteps)&&const DeepCollectionEquality().equals(other._baselineDailySteps, _baselineDailySteps)&&const DeepCollectionEquality().equals(other._nutrition, _nutrition)&&const DeepCollectionEquality().equals(other._previousNutrition, _previousNutrition)&&const DeepCollectionEquality().equals(other._baselineNutrition, _baselineNutrition)&&const DeepCollectionEquality().equals(other._activityProgress, _activityProgress));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_dailySteps),const DeepCollectionEquality().hash(_previousDailySteps),const DeepCollectionEquality().hash(_baselineDailySteps),const DeepCollectionEquality().hash(_nutrition),const DeepCollectionEquality().hash(_previousNutrition),const DeepCollectionEquality().hash(_baselineNutrition),const DeepCollectionEquality().hash(_activityProgress));

@override
String toString() {
  return 'ActivityPeriodData(dailySteps: $dailySteps, previousDailySteps: $previousDailySteps, baselineDailySteps: $baselineDailySteps, nutrition: $nutrition, previousNutrition: $previousNutrition, baselineNutrition: $baselineNutrition, activityProgress: $activityProgress)';
}


}

/// @nodoc
abstract mixin class _$ActivityPeriodDataCopyWith<$Res> implements $ActivityPeriodDataCopyWith<$Res> {
  factory _$ActivityPeriodDataCopyWith(_ActivityPeriodData value, $Res Function(_ActivityPeriodData) _then) = __$ActivityPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<DailySteps> dailySteps, List<DailySteps> previousDailySteps, List<DailySteps> baselineDailySteps, List<DailyNutrition> nutrition, List<DailyNutrition> previousNutrition, List<DailyNutrition> baselineNutrition, List<ActivityProgressPoint> activityProgress
});




}
/// @nodoc
class __$ActivityPeriodDataCopyWithImpl<$Res>
    implements _$ActivityPeriodDataCopyWith<$Res> {
  __$ActivityPeriodDataCopyWithImpl(this._self, this._then);

  final _ActivityPeriodData _self;
  final $Res Function(_ActivityPeriodData) _then;

/// Create a copy of ActivityPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? dailySteps = null,Object? previousDailySteps = null,Object? baselineDailySteps = null,Object? nutrition = null,Object? previousNutrition = null,Object? baselineNutrition = null,Object? activityProgress = null,}) {
  return _then(_ActivityPeriodData(
dailySteps: null == dailySteps ? _self._dailySteps : dailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,previousDailySteps: null == previousDailySteps ? _self._previousDailySteps : previousDailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,baselineDailySteps: null == baselineDailySteps ? _self._baselineDailySteps : baselineDailySteps // ignore: cast_nullable_to_non_nullable
as List<DailySteps>,nutrition: null == nutrition ? _self._nutrition : nutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,previousNutrition: null == previousNutrition ? _self._previousNutrition : previousNutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,baselineNutrition: null == baselineNutrition ? _self._baselineNutrition : baselineNutrition // ignore: cast_nullable_to_non_nullable
as List<DailyNutrition>,activityProgress: null == activityProgress ? _self._activityProgress : activityProgress // ignore: cast_nullable_to_non_nullable
as List<ActivityProgressPoint>,
  ));
}


}

/// @nodoc
mixin _$ActivitiesPeriodData {

 List<ExerciseData> get workouts; List<ExerciseData> get previousWorkouts; List<ExerciseData> get baselineWorkouts; List<PlannedExerciseData> get plannedWorkouts;
/// Create a copy of ActivitiesPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivitiesPeriodDataCopyWith<ActivitiesPeriodData> get copyWith => _$ActivitiesPeriodDataCopyWithImpl<ActivitiesPeriodData>(this as ActivitiesPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivitiesPeriodData&&const DeepCollectionEquality().equals(other.workouts, workouts)&&const DeepCollectionEquality().equals(other.previousWorkouts, previousWorkouts)&&const DeepCollectionEquality().equals(other.baselineWorkouts, baselineWorkouts)&&const DeepCollectionEquality().equals(other.plannedWorkouts, plannedWorkouts));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(workouts),const DeepCollectionEquality().hash(previousWorkouts),const DeepCollectionEquality().hash(baselineWorkouts),const DeepCollectionEquality().hash(plannedWorkouts));

@override
String toString() {
  return 'ActivitiesPeriodData(workouts: $workouts, previousWorkouts: $previousWorkouts, baselineWorkouts: $baselineWorkouts, plannedWorkouts: $plannedWorkouts)';
}


}

/// @nodoc
abstract mixin class $ActivitiesPeriodDataCopyWith<$Res>  {
  factory $ActivitiesPeriodDataCopyWith(ActivitiesPeriodData value, $Res Function(ActivitiesPeriodData) _then) = _$ActivitiesPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<ExerciseData> workouts, List<ExerciseData> previousWorkouts, List<ExerciseData> baselineWorkouts, List<PlannedExerciseData> plannedWorkouts
});




}
/// @nodoc
class _$ActivitiesPeriodDataCopyWithImpl<$Res>
    implements $ActivitiesPeriodDataCopyWith<$Res> {
  _$ActivitiesPeriodDataCopyWithImpl(this._self, this._then);

  final ActivitiesPeriodData _self;
  final $Res Function(ActivitiesPeriodData) _then;

/// Create a copy of ActivitiesPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? workouts = null,Object? previousWorkouts = null,Object? baselineWorkouts = null,Object? plannedWorkouts = null,}) {
  return _then(_self.copyWith(
workouts: null == workouts ? _self.workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,previousWorkouts: null == previousWorkouts ? _self.previousWorkouts : previousWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,baselineWorkouts: null == baselineWorkouts ? _self.baselineWorkouts : baselineWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,plannedWorkouts: null == plannedWorkouts ? _self.plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivitiesPeriodData].
extension ActivitiesPeriodDataPatterns on ActivitiesPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivitiesPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivitiesPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivitiesPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _ActivitiesPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivitiesPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _ActivitiesPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<ExerciseData> workouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<PlannedExerciseData> plannedWorkouts)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivitiesPeriodData() when $default != null:
return $default(_that.workouts,_that.previousWorkouts,_that.baselineWorkouts,_that.plannedWorkouts);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<ExerciseData> workouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<PlannedExerciseData> plannedWorkouts)  $default,) {final _that = this;
switch (_that) {
case _ActivitiesPeriodData():
return $default(_that.workouts,_that.previousWorkouts,_that.baselineWorkouts,_that.plannedWorkouts);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<ExerciseData> workouts,  List<ExerciseData> previousWorkouts,  List<ExerciseData> baselineWorkouts,  List<PlannedExerciseData> plannedWorkouts)?  $default,) {final _that = this;
switch (_that) {
case _ActivitiesPeriodData() when $default != null:
return $default(_that.workouts,_that.previousWorkouts,_that.baselineWorkouts,_that.plannedWorkouts);case _:
  return null;

}
}

}

/// @nodoc


class _ActivitiesPeriodData implements ActivitiesPeriodData {
  const _ActivitiesPeriodData({final  List<ExerciseData> workouts = const <ExerciseData>[], final  List<ExerciseData> previousWorkouts = const <ExerciseData>[], final  List<ExerciseData> baselineWorkouts = const <ExerciseData>[], final  List<PlannedExerciseData> plannedWorkouts = const <PlannedExerciseData>[]}): _workouts = workouts,_previousWorkouts = previousWorkouts,_baselineWorkouts = baselineWorkouts,_plannedWorkouts = plannedWorkouts;
  

 final  List<ExerciseData> _workouts;
@override@JsonKey() List<ExerciseData> get workouts {
  if (_workouts is EqualUnmodifiableListView) return _workouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_workouts);
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

 final  List<PlannedExerciseData> _plannedWorkouts;
@override@JsonKey() List<PlannedExerciseData> get plannedWorkouts {
  if (_plannedWorkouts is EqualUnmodifiableListView) return _plannedWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_plannedWorkouts);
}


/// Create a copy of ActivitiesPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivitiesPeriodDataCopyWith<_ActivitiesPeriodData> get copyWith => __$ActivitiesPeriodDataCopyWithImpl<_ActivitiesPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivitiesPeriodData&&const DeepCollectionEquality().equals(other._workouts, _workouts)&&const DeepCollectionEquality().equals(other._previousWorkouts, _previousWorkouts)&&const DeepCollectionEquality().equals(other._baselineWorkouts, _baselineWorkouts)&&const DeepCollectionEquality().equals(other._plannedWorkouts, _plannedWorkouts));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_workouts),const DeepCollectionEquality().hash(_previousWorkouts),const DeepCollectionEquality().hash(_baselineWorkouts),const DeepCollectionEquality().hash(_plannedWorkouts));

@override
String toString() {
  return 'ActivitiesPeriodData(workouts: $workouts, previousWorkouts: $previousWorkouts, baselineWorkouts: $baselineWorkouts, plannedWorkouts: $plannedWorkouts)';
}


}

/// @nodoc
abstract mixin class _$ActivitiesPeriodDataCopyWith<$Res> implements $ActivitiesPeriodDataCopyWith<$Res> {
  factory _$ActivitiesPeriodDataCopyWith(_ActivitiesPeriodData value, $Res Function(_ActivitiesPeriodData) _then) = __$ActivitiesPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<ExerciseData> workouts, List<ExerciseData> previousWorkouts, List<ExerciseData> baselineWorkouts, List<PlannedExerciseData> plannedWorkouts
});




}
/// @nodoc
class __$ActivitiesPeriodDataCopyWithImpl<$Res>
    implements _$ActivitiesPeriodDataCopyWith<$Res> {
  __$ActivitiesPeriodDataCopyWithImpl(this._self, this._then);

  final _ActivitiesPeriodData _self;
  final $Res Function(_ActivitiesPeriodData) _then;

/// Create a copy of ActivitiesPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? workouts = null,Object? previousWorkouts = null,Object? baselineWorkouts = null,Object? plannedWorkouts = null,}) {
  return _then(_ActivitiesPeriodData(
workouts: null == workouts ? _self._workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,previousWorkouts: null == previousWorkouts ? _self._previousWorkouts : previousWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,baselineWorkouts: null == baselineWorkouts ? _self._baselineWorkouts : baselineWorkouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,plannedWorkouts: null == plannedWorkouts ? _self._plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,
  ));
}


}

// dart format on
