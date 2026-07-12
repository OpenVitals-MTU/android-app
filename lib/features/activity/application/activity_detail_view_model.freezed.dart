// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_detail_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityDetailState {

 bool get isLoading; ScreenError? get error; ExerciseData? get workout; List<HeartRateSample> get heartRateSamples; List<SpeedSample> get speedSamples; List<ActivityCadenceSample> get cadenceSamples; ActivitySplits get splits;
/// Create a copy of ActivityDetailState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityDetailStateCopyWith<ActivityDetailState> get copyWith => _$ActivityDetailStateCopyWithImpl<ActivityDetailState>(this as ActivityDetailState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityDetailState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.workout, workout) || other.workout == workout)&&const DeepCollectionEquality().equals(other.heartRateSamples, heartRateSamples)&&const DeepCollectionEquality().equals(other.speedSamples, speedSamples)&&const DeepCollectionEquality().equals(other.cadenceSamples, cadenceSamples)&&(identical(other.splits, splits) || other.splits == splits));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,error,workout,const DeepCollectionEquality().hash(heartRateSamples),const DeepCollectionEquality().hash(speedSamples),const DeepCollectionEquality().hash(cadenceSamples),splits);

@override
String toString() {
  return 'ActivityDetailState(isLoading: $isLoading, error: $error, workout: $workout, heartRateSamples: $heartRateSamples, speedSamples: $speedSamples, cadenceSamples: $cadenceSamples, splits: $splits)';
}


}

/// @nodoc
abstract mixin class $ActivityDetailStateCopyWith<$Res>  {
  factory $ActivityDetailStateCopyWith(ActivityDetailState value, $Res Function(ActivityDetailState) _then) = _$ActivityDetailStateCopyWithImpl;
@useResult
$Res call({
 bool isLoading, ScreenError? error, ExerciseData? workout, List<HeartRateSample> heartRateSamples, List<SpeedSample> speedSamples, List<ActivityCadenceSample> cadenceSamples, ActivitySplits splits
});


$ExerciseDataCopyWith<$Res>? get workout;

}
/// @nodoc
class _$ActivityDetailStateCopyWithImpl<$Res>
    implements $ActivityDetailStateCopyWith<$Res> {
  _$ActivityDetailStateCopyWithImpl(this._self, this._then);

  final ActivityDetailState _self;
  final $Res Function(ActivityDetailState) _then;

/// Create a copy of ActivityDetailState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isLoading = null,Object? error = freezed,Object? workout = freezed,Object? heartRateSamples = null,Object? speedSamples = null,Object? cadenceSamples = null,Object? splits = null,}) {
  return _then(_self.copyWith(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,heartRateSamples: null == heartRateSamples ? _self.heartRateSamples : heartRateSamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,speedSamples: null == speedSamples ? _self.speedSamples : speedSamples // ignore: cast_nullable_to_non_nullable
as List<SpeedSample>,cadenceSamples: null == cadenceSamples ? _self.cadenceSamples : cadenceSamples // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceSample>,splits: null == splits ? _self.splits : splits // ignore: cast_nullable_to_non_nullable
as ActivitySplits,
  ));
}
/// Create a copy of ActivityDetailState
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


/// Adds pattern-matching-related methods to [ActivityDetailState].
extension ActivityDetailStatePatterns on ActivityDetailState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityDetailState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityDetailState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityDetailState value)  $default,){
final _that = this;
switch (_that) {
case _ActivityDetailState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityDetailState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityDetailState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isLoading,  ScreenError? error,  ExerciseData? workout,  List<HeartRateSample> heartRateSamples,  List<SpeedSample> speedSamples,  List<ActivityCadenceSample> cadenceSamples,  ActivitySplits splits)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityDetailState() when $default != null:
return $default(_that.isLoading,_that.error,_that.workout,_that.heartRateSamples,_that.speedSamples,_that.cadenceSamples,_that.splits);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isLoading,  ScreenError? error,  ExerciseData? workout,  List<HeartRateSample> heartRateSamples,  List<SpeedSample> speedSamples,  List<ActivityCadenceSample> cadenceSamples,  ActivitySplits splits)  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailState():
return $default(_that.isLoading,_that.error,_that.workout,_that.heartRateSamples,_that.speedSamples,_that.cadenceSamples,_that.splits);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isLoading,  ScreenError? error,  ExerciseData? workout,  List<HeartRateSample> heartRateSamples,  List<SpeedSample> speedSamples,  List<ActivityCadenceSample> cadenceSamples,  ActivitySplits splits)?  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailState() when $default != null:
return $default(_that.isLoading,_that.error,_that.workout,_that.heartRateSamples,_that.speedSamples,_that.cadenceSamples,_that.splits);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityDetailState extends ActivityDetailState {
  const _ActivityDetailState({this.isLoading = true, this.error, this.workout, final  List<HeartRateSample> heartRateSamples = const <HeartRateSample>[], final  List<SpeedSample> speedSamples = const <SpeedSample>[], final  List<ActivityCadenceSample> cadenceSamples = const <ActivityCadenceSample>[], this.splits = const ActivitySplits.none()}): _heartRateSamples = heartRateSamples,_speedSamples = speedSamples,_cadenceSamples = cadenceSamples,super._();
  

@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  ExerciseData? workout;
 final  List<HeartRateSample> _heartRateSamples;
@override@JsonKey() List<HeartRateSample> get heartRateSamples {
  if (_heartRateSamples is EqualUnmodifiableListView) return _heartRateSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_heartRateSamples);
}

 final  List<SpeedSample> _speedSamples;
@override@JsonKey() List<SpeedSample> get speedSamples {
  if (_speedSamples is EqualUnmodifiableListView) return _speedSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_speedSamples);
}

 final  List<ActivityCadenceSample> _cadenceSamples;
@override@JsonKey() List<ActivityCadenceSample> get cadenceSamples {
  if (_cadenceSamples is EqualUnmodifiableListView) return _cadenceSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cadenceSamples);
}

@override@JsonKey() final  ActivitySplits splits;

/// Create a copy of ActivityDetailState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityDetailStateCopyWith<_ActivityDetailState> get copyWith => __$ActivityDetailStateCopyWithImpl<_ActivityDetailState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityDetailState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.workout, workout) || other.workout == workout)&&const DeepCollectionEquality().equals(other._heartRateSamples, _heartRateSamples)&&const DeepCollectionEquality().equals(other._speedSamples, _speedSamples)&&const DeepCollectionEquality().equals(other._cadenceSamples, _cadenceSamples)&&(identical(other.splits, splits) || other.splits == splits));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,error,workout,const DeepCollectionEquality().hash(_heartRateSamples),const DeepCollectionEquality().hash(_speedSamples),const DeepCollectionEquality().hash(_cadenceSamples),splits);

@override
String toString() {
  return 'ActivityDetailState(isLoading: $isLoading, error: $error, workout: $workout, heartRateSamples: $heartRateSamples, speedSamples: $speedSamples, cadenceSamples: $cadenceSamples, splits: $splits)';
}


}

/// @nodoc
abstract mixin class _$ActivityDetailStateCopyWith<$Res> implements $ActivityDetailStateCopyWith<$Res> {
  factory _$ActivityDetailStateCopyWith(_ActivityDetailState value, $Res Function(_ActivityDetailState) _then) = __$ActivityDetailStateCopyWithImpl;
@override @useResult
$Res call({
 bool isLoading, ScreenError? error, ExerciseData? workout, List<HeartRateSample> heartRateSamples, List<SpeedSample> speedSamples, List<ActivityCadenceSample> cadenceSamples, ActivitySplits splits
});


@override $ExerciseDataCopyWith<$Res>? get workout;

}
/// @nodoc
class __$ActivityDetailStateCopyWithImpl<$Res>
    implements _$ActivityDetailStateCopyWith<$Res> {
  __$ActivityDetailStateCopyWithImpl(this._self, this._then);

  final _ActivityDetailState _self;
  final $Res Function(_ActivityDetailState) _then;

/// Create a copy of ActivityDetailState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isLoading = null,Object? error = freezed,Object? workout = freezed,Object? heartRateSamples = null,Object? speedSamples = null,Object? cadenceSamples = null,Object? splits = null,}) {
  return _then(_ActivityDetailState(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,heartRateSamples: null == heartRateSamples ? _self._heartRateSamples : heartRateSamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,speedSamples: null == speedSamples ? _self._speedSamples : speedSamples // ignore: cast_nullable_to_non_nullable
as List<SpeedSample>,cadenceSamples: null == cadenceSamples ? _self._cadenceSamples : cadenceSamples // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceSample>,splits: null == splits ? _self.splits : splits // ignore: cast_nullable_to_non_nullable
as ActivitySplits,
  ));
}

/// Create a copy of ActivityDetailState
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

// dart format on
