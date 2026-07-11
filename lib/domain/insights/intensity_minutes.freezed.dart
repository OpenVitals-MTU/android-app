// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'intensity_minutes.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$IntensityWorkoutInput {

 double get durationMinutes; double? get activeCaloriesKcal;
/// Create a copy of IntensityWorkoutInput
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$IntensityWorkoutInputCopyWith<IntensityWorkoutInput> get copyWith => _$IntensityWorkoutInputCopyWithImpl<IntensityWorkoutInput>(this as IntensityWorkoutInput, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is IntensityWorkoutInput&&(identical(other.durationMinutes, durationMinutes) || other.durationMinutes == durationMinutes)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal));
}


@override
int get hashCode => Object.hash(runtimeType,durationMinutes,activeCaloriesKcal);

@override
String toString() {
  return 'IntensityWorkoutInput(durationMinutes: $durationMinutes, activeCaloriesKcal: $activeCaloriesKcal)';
}


}

/// @nodoc
abstract mixin class $IntensityWorkoutInputCopyWith<$Res>  {
  factory $IntensityWorkoutInputCopyWith(IntensityWorkoutInput value, $Res Function(IntensityWorkoutInput) _then) = _$IntensityWorkoutInputCopyWithImpl;
@useResult
$Res call({
 double durationMinutes, double? activeCaloriesKcal
});




}
/// @nodoc
class _$IntensityWorkoutInputCopyWithImpl<$Res>
    implements $IntensityWorkoutInputCopyWith<$Res> {
  _$IntensityWorkoutInputCopyWithImpl(this._self, this._then);

  final IntensityWorkoutInput _self;
  final $Res Function(IntensityWorkoutInput) _then;

/// Create a copy of IntensityWorkoutInput
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? durationMinutes = null,Object? activeCaloriesKcal = freezed,}) {
  return _then(_self.copyWith(
durationMinutes: null == durationMinutes ? _self.durationMinutes : durationMinutes // ignore: cast_nullable_to_non_nullable
as double,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [IntensityWorkoutInput].
extension IntensityWorkoutInputPatterns on IntensityWorkoutInput {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _IntensityWorkoutInput value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _IntensityWorkoutInput() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _IntensityWorkoutInput value)  $default,){
final _that = this;
switch (_that) {
case _IntensityWorkoutInput():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _IntensityWorkoutInput value)?  $default,){
final _that = this;
switch (_that) {
case _IntensityWorkoutInput() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double durationMinutes,  double? activeCaloriesKcal)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _IntensityWorkoutInput() when $default != null:
return $default(_that.durationMinutes,_that.activeCaloriesKcal);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double durationMinutes,  double? activeCaloriesKcal)  $default,) {final _that = this;
switch (_that) {
case _IntensityWorkoutInput():
return $default(_that.durationMinutes,_that.activeCaloriesKcal);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double durationMinutes,  double? activeCaloriesKcal)?  $default,) {final _that = this;
switch (_that) {
case _IntensityWorkoutInput() when $default != null:
return $default(_that.durationMinutes,_that.activeCaloriesKcal);case _:
  return null;

}
}

}

/// @nodoc


class _IntensityWorkoutInput implements IntensityWorkoutInput {
  const _IntensityWorkoutInput({required this.durationMinutes, this.activeCaloriesKcal});
  

@override final  double durationMinutes;
@override final  double? activeCaloriesKcal;

/// Create a copy of IntensityWorkoutInput
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$IntensityWorkoutInputCopyWith<_IntensityWorkoutInput> get copyWith => __$IntensityWorkoutInputCopyWithImpl<_IntensityWorkoutInput>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _IntensityWorkoutInput&&(identical(other.durationMinutes, durationMinutes) || other.durationMinutes == durationMinutes)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal));
}


@override
int get hashCode => Object.hash(runtimeType,durationMinutes,activeCaloriesKcal);

@override
String toString() {
  return 'IntensityWorkoutInput(durationMinutes: $durationMinutes, activeCaloriesKcal: $activeCaloriesKcal)';
}


}

/// @nodoc
abstract mixin class _$IntensityWorkoutInputCopyWith<$Res> implements $IntensityWorkoutInputCopyWith<$Res> {
  factory _$IntensityWorkoutInputCopyWith(_IntensityWorkoutInput value, $Res Function(_IntensityWorkoutInput) _then) = __$IntensityWorkoutInputCopyWithImpl;
@override @useResult
$Res call({
 double durationMinutes, double? activeCaloriesKcal
});




}
/// @nodoc
class __$IntensityWorkoutInputCopyWithImpl<$Res>
    implements _$IntensityWorkoutInputCopyWith<$Res> {
  __$IntensityWorkoutInputCopyWithImpl(this._self, this._then);

  final _IntensityWorkoutInput _self;
  final $Res Function(_IntensityWorkoutInput) _then;

/// Create a copy of IntensityWorkoutInput
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? durationMinutes = null,Object? activeCaloriesKcal = freezed,}) {
  return _then(_IntensityWorkoutInput(
durationMinutes: null == durationMinutes ? _self.durationMinutes : durationMinutes // ignore: cast_nullable_to_non_nullable
as double,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$IntensityMinutesEstimate {

 int get moderateMinutes; int get vigorousMinutes; int get moderateEquivalentMinutes; IntensityMinutesConfidence get confidence; IntensityMinutesMethod get method; double get coveredHeartRateMinutes; double get expectedHeartRateMinutes; int get heartRateSampleCount;
/// Create a copy of IntensityMinutesEstimate
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$IntensityMinutesEstimateCopyWith<IntensityMinutesEstimate> get copyWith => _$IntensityMinutesEstimateCopyWithImpl<IntensityMinutesEstimate>(this as IntensityMinutesEstimate, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is IntensityMinutesEstimate&&(identical(other.moderateMinutes, moderateMinutes) || other.moderateMinutes == moderateMinutes)&&(identical(other.vigorousMinutes, vigorousMinutes) || other.vigorousMinutes == vigorousMinutes)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.method, method) || other.method == method)&&(identical(other.coveredHeartRateMinutes, coveredHeartRateMinutes) || other.coveredHeartRateMinutes == coveredHeartRateMinutes)&&(identical(other.expectedHeartRateMinutes, expectedHeartRateMinutes) || other.expectedHeartRateMinutes == expectedHeartRateMinutes)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount));
}


@override
int get hashCode => Object.hash(runtimeType,moderateMinutes,vigorousMinutes,moderateEquivalentMinutes,confidence,method,coveredHeartRateMinutes,expectedHeartRateMinutes,heartRateSampleCount);

@override
String toString() {
  return 'IntensityMinutesEstimate(moderateMinutes: $moderateMinutes, vigorousMinutes: $vigorousMinutes, moderateEquivalentMinutes: $moderateEquivalentMinutes, confidence: $confidence, method: $method, coveredHeartRateMinutes: $coveredHeartRateMinutes, expectedHeartRateMinutes: $expectedHeartRateMinutes, heartRateSampleCount: $heartRateSampleCount)';
}


}

/// @nodoc
abstract mixin class $IntensityMinutesEstimateCopyWith<$Res>  {
  factory $IntensityMinutesEstimateCopyWith(IntensityMinutesEstimate value, $Res Function(IntensityMinutesEstimate) _then) = _$IntensityMinutesEstimateCopyWithImpl;
@useResult
$Res call({
 int moderateMinutes, int vigorousMinutes, int moderateEquivalentMinutes, IntensityMinutesConfidence confidence, IntensityMinutesMethod method, double coveredHeartRateMinutes, double expectedHeartRateMinutes, int heartRateSampleCount
});




}
/// @nodoc
class _$IntensityMinutesEstimateCopyWithImpl<$Res>
    implements $IntensityMinutesEstimateCopyWith<$Res> {
  _$IntensityMinutesEstimateCopyWithImpl(this._self, this._then);

  final IntensityMinutesEstimate _self;
  final $Res Function(IntensityMinutesEstimate) _then;

/// Create a copy of IntensityMinutesEstimate
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? moderateMinutes = null,Object? vigorousMinutes = null,Object? moderateEquivalentMinutes = null,Object? confidence = null,Object? method = null,Object? coveredHeartRateMinutes = null,Object? expectedHeartRateMinutes = null,Object? heartRateSampleCount = null,}) {
  return _then(_self.copyWith(
moderateMinutes: null == moderateMinutes ? _self.moderateMinutes : moderateMinutes // ignore: cast_nullable_to_non_nullable
as int,vigorousMinutes: null == vigorousMinutes ? _self.vigorousMinutes : vigorousMinutes // ignore: cast_nullable_to_non_nullable
as int,moderateEquivalentMinutes: null == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,method: null == method ? _self.method : method // ignore: cast_nullable_to_non_nullable
as IntensityMinutesMethod,coveredHeartRateMinutes: null == coveredHeartRateMinutes ? _self.coveredHeartRateMinutes : coveredHeartRateMinutes // ignore: cast_nullable_to_non_nullable
as double,expectedHeartRateMinutes: null == expectedHeartRateMinutes ? _self.expectedHeartRateMinutes : expectedHeartRateMinutes // ignore: cast_nullable_to_non_nullable
as double,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [IntensityMinutesEstimate].
extension IntensityMinutesEstimatePatterns on IntensityMinutesEstimate {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _IntensityMinutesEstimate value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _IntensityMinutesEstimate() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _IntensityMinutesEstimate value)  $default,){
final _that = this;
switch (_that) {
case _IntensityMinutesEstimate():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _IntensityMinutesEstimate value)?  $default,){
final _that = this;
switch (_that) {
case _IntensityMinutesEstimate() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  IntensityMinutesConfidence confidence,  IntensityMinutesMethod method,  double coveredHeartRateMinutes,  double expectedHeartRateMinutes,  int heartRateSampleCount)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _IntensityMinutesEstimate() when $default != null:
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.confidence,_that.method,_that.coveredHeartRateMinutes,_that.expectedHeartRateMinutes,_that.heartRateSampleCount);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  IntensityMinutesConfidence confidence,  IntensityMinutesMethod method,  double coveredHeartRateMinutes,  double expectedHeartRateMinutes,  int heartRateSampleCount)  $default,) {final _that = this;
switch (_that) {
case _IntensityMinutesEstimate():
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.confidence,_that.method,_that.coveredHeartRateMinutes,_that.expectedHeartRateMinutes,_that.heartRateSampleCount);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  IntensityMinutesConfidence confidence,  IntensityMinutesMethod method,  double coveredHeartRateMinutes,  double expectedHeartRateMinutes,  int heartRateSampleCount)?  $default,) {final _that = this;
switch (_that) {
case _IntensityMinutesEstimate() when $default != null:
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.confidence,_that.method,_that.coveredHeartRateMinutes,_that.expectedHeartRateMinutes,_that.heartRateSampleCount);case _:
  return null;

}
}

}

/// @nodoc


class _IntensityMinutesEstimate implements IntensityMinutesEstimate {
  const _IntensityMinutesEstimate({this.moderateMinutes = 0, this.vigorousMinutes = 0, this.moderateEquivalentMinutes = 0, this.confidence = IntensityMinutesConfidence.noData, this.method = IntensityMinutesMethod.noData, this.coveredHeartRateMinutes = 0.0, this.expectedHeartRateMinutes = 0.0, this.heartRateSampleCount = 0});
  

@override@JsonKey() final  int moderateMinutes;
@override@JsonKey() final  int vigorousMinutes;
@override@JsonKey() final  int moderateEquivalentMinutes;
@override@JsonKey() final  IntensityMinutesConfidence confidence;
@override@JsonKey() final  IntensityMinutesMethod method;
@override@JsonKey() final  double coveredHeartRateMinutes;
@override@JsonKey() final  double expectedHeartRateMinutes;
@override@JsonKey() final  int heartRateSampleCount;

/// Create a copy of IntensityMinutesEstimate
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$IntensityMinutesEstimateCopyWith<_IntensityMinutesEstimate> get copyWith => __$IntensityMinutesEstimateCopyWithImpl<_IntensityMinutesEstimate>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _IntensityMinutesEstimate&&(identical(other.moderateMinutes, moderateMinutes) || other.moderateMinutes == moderateMinutes)&&(identical(other.vigorousMinutes, vigorousMinutes) || other.vigorousMinutes == vigorousMinutes)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.method, method) || other.method == method)&&(identical(other.coveredHeartRateMinutes, coveredHeartRateMinutes) || other.coveredHeartRateMinutes == coveredHeartRateMinutes)&&(identical(other.expectedHeartRateMinutes, expectedHeartRateMinutes) || other.expectedHeartRateMinutes == expectedHeartRateMinutes)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount));
}


@override
int get hashCode => Object.hash(runtimeType,moderateMinutes,vigorousMinutes,moderateEquivalentMinutes,confidence,method,coveredHeartRateMinutes,expectedHeartRateMinutes,heartRateSampleCount);

@override
String toString() {
  return 'IntensityMinutesEstimate(moderateMinutes: $moderateMinutes, vigorousMinutes: $vigorousMinutes, moderateEquivalentMinutes: $moderateEquivalentMinutes, confidence: $confidence, method: $method, coveredHeartRateMinutes: $coveredHeartRateMinutes, expectedHeartRateMinutes: $expectedHeartRateMinutes, heartRateSampleCount: $heartRateSampleCount)';
}


}

/// @nodoc
abstract mixin class _$IntensityMinutesEstimateCopyWith<$Res> implements $IntensityMinutesEstimateCopyWith<$Res> {
  factory _$IntensityMinutesEstimateCopyWith(_IntensityMinutesEstimate value, $Res Function(_IntensityMinutesEstimate) _then) = __$IntensityMinutesEstimateCopyWithImpl;
@override @useResult
$Res call({
 int moderateMinutes, int vigorousMinutes, int moderateEquivalentMinutes, IntensityMinutesConfidence confidence, IntensityMinutesMethod method, double coveredHeartRateMinutes, double expectedHeartRateMinutes, int heartRateSampleCount
});




}
/// @nodoc
class __$IntensityMinutesEstimateCopyWithImpl<$Res>
    implements _$IntensityMinutesEstimateCopyWith<$Res> {
  __$IntensityMinutesEstimateCopyWithImpl(this._self, this._then);

  final _IntensityMinutesEstimate _self;
  final $Res Function(_IntensityMinutesEstimate) _then;

/// Create a copy of IntensityMinutesEstimate
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? moderateMinutes = null,Object? vigorousMinutes = null,Object? moderateEquivalentMinutes = null,Object? confidence = null,Object? method = null,Object? coveredHeartRateMinutes = null,Object? expectedHeartRateMinutes = null,Object? heartRateSampleCount = null,}) {
  return _then(_IntensityMinutesEstimate(
moderateMinutes: null == moderateMinutes ? _self.moderateMinutes : moderateMinutes // ignore: cast_nullable_to_non_nullable
as int,vigorousMinutes: null == vigorousMinutes ? _self.vigorousMinutes : vigorousMinutes // ignore: cast_nullable_to_non_nullable
as int,moderateEquivalentMinutes: null == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,method: null == method ? _self.method : method // ignore: cast_nullable_to_non_nullable
as IntensityMinutesMethod,coveredHeartRateMinutes: null == coveredHeartRateMinutes ? _self.coveredHeartRateMinutes : coveredHeartRateMinutes // ignore: cast_nullable_to_non_nullable
as double,expectedHeartRateMinutes: null == expectedHeartRateMinutes ? _self.expectedHeartRateMinutes : expectedHeartRateMinutes // ignore: cast_nullable_to_non_nullable
as double,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on
