// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cardio_load.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CardioLoadEstimate {

 int get score; CardioLoadConfidence get confidence; CardioLoadMethod get method; double? get trimpScore; double get coveredMinutes; double get expectedMinutes; int? get restingHeartRateBpm; bool get restingHeartRateObserved; int? get maxHeartRateBpm; bool get maxHeartRateObserved; int get heartRateSampleCount; int get activityWindowCount; double get activityWindowMinutes; int get movementFallbackScore;
/// Create a copy of CardioLoadEstimate
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CardioLoadEstimateCopyWith<CardioLoadEstimate> get copyWith => _$CardioLoadEstimateCopyWithImpl<CardioLoadEstimate>(this as CardioLoadEstimate, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CardioLoadEstimate&&(identical(other.score, score) || other.score == score)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.method, method) || other.method == method)&&(identical(other.trimpScore, trimpScore) || other.trimpScore == trimpScore)&&(identical(other.coveredMinutes, coveredMinutes) || other.coveredMinutes == coveredMinutes)&&(identical(other.expectedMinutes, expectedMinutes) || other.expectedMinutes == expectedMinutes)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.restingHeartRateObserved, restingHeartRateObserved) || other.restingHeartRateObserved == restingHeartRateObserved)&&(identical(other.maxHeartRateBpm, maxHeartRateBpm) || other.maxHeartRateBpm == maxHeartRateBpm)&&(identical(other.maxHeartRateObserved, maxHeartRateObserved) || other.maxHeartRateObserved == maxHeartRateObserved)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.activityWindowCount, activityWindowCount) || other.activityWindowCount == activityWindowCount)&&(identical(other.activityWindowMinutes, activityWindowMinutes) || other.activityWindowMinutes == activityWindowMinutes)&&(identical(other.movementFallbackScore, movementFallbackScore) || other.movementFallbackScore == movementFallbackScore));
}


@override
int get hashCode => Object.hash(runtimeType,score,confidence,method,trimpScore,coveredMinutes,expectedMinutes,restingHeartRateBpm,restingHeartRateObserved,maxHeartRateBpm,maxHeartRateObserved,heartRateSampleCount,activityWindowCount,activityWindowMinutes,movementFallbackScore);

@override
String toString() {
  return 'CardioLoadEstimate(score: $score, confidence: $confidence, method: $method, trimpScore: $trimpScore, coveredMinutes: $coveredMinutes, expectedMinutes: $expectedMinutes, restingHeartRateBpm: $restingHeartRateBpm, restingHeartRateObserved: $restingHeartRateObserved, maxHeartRateBpm: $maxHeartRateBpm, maxHeartRateObserved: $maxHeartRateObserved, heartRateSampleCount: $heartRateSampleCount, activityWindowCount: $activityWindowCount, activityWindowMinutes: $activityWindowMinutes, movementFallbackScore: $movementFallbackScore)';
}


}

/// @nodoc
abstract mixin class $CardioLoadEstimateCopyWith<$Res>  {
  factory $CardioLoadEstimateCopyWith(CardioLoadEstimate value, $Res Function(CardioLoadEstimate) _then) = _$CardioLoadEstimateCopyWithImpl;
@useResult
$Res call({
 int score, CardioLoadConfidence confidence, CardioLoadMethod method, double? trimpScore, double coveredMinutes, double expectedMinutes, int? restingHeartRateBpm, bool restingHeartRateObserved, int? maxHeartRateBpm, bool maxHeartRateObserved, int heartRateSampleCount, int activityWindowCount, double activityWindowMinutes, int movementFallbackScore
});




}
/// @nodoc
class _$CardioLoadEstimateCopyWithImpl<$Res>
    implements $CardioLoadEstimateCopyWith<$Res> {
  _$CardioLoadEstimateCopyWithImpl(this._self, this._then);

  final CardioLoadEstimate _self;
  final $Res Function(CardioLoadEstimate) _then;

/// Create a copy of CardioLoadEstimate
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? score = null,Object? confidence = null,Object? method = null,Object? trimpScore = freezed,Object? coveredMinutes = null,Object? expectedMinutes = null,Object? restingHeartRateBpm = freezed,Object? restingHeartRateObserved = null,Object? maxHeartRateBpm = freezed,Object? maxHeartRateObserved = null,Object? heartRateSampleCount = null,Object? activityWindowCount = null,Object? activityWindowMinutes = null,Object? movementFallbackScore = null,}) {
  return _then(_self.copyWith(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,method: null == method ? _self.method : method // ignore: cast_nullable_to_non_nullable
as CardioLoadMethod,trimpScore: freezed == trimpScore ? _self.trimpScore : trimpScore // ignore: cast_nullable_to_non_nullable
as double?,coveredMinutes: null == coveredMinutes ? _self.coveredMinutes : coveredMinutes // ignore: cast_nullable_to_non_nullable
as double,expectedMinutes: null == expectedMinutes ? _self.expectedMinutes : expectedMinutes // ignore: cast_nullable_to_non_nullable
as double,restingHeartRateBpm: freezed == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,restingHeartRateObserved: null == restingHeartRateObserved ? _self.restingHeartRateObserved : restingHeartRateObserved // ignore: cast_nullable_to_non_nullable
as bool,maxHeartRateBpm: freezed == maxHeartRateBpm ? _self.maxHeartRateBpm : maxHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateObserved: null == maxHeartRateObserved ? _self.maxHeartRateObserved : maxHeartRateObserved // ignore: cast_nullable_to_non_nullable
as bool,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,activityWindowCount: null == activityWindowCount ? _self.activityWindowCount : activityWindowCount // ignore: cast_nullable_to_non_nullable
as int,activityWindowMinutes: null == activityWindowMinutes ? _self.activityWindowMinutes : activityWindowMinutes // ignore: cast_nullable_to_non_nullable
as double,movementFallbackScore: null == movementFallbackScore ? _self.movementFallbackScore : movementFallbackScore // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [CardioLoadEstimate].
extension CardioLoadEstimatePatterns on CardioLoadEstimate {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CardioLoadEstimate value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CardioLoadEstimate() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CardioLoadEstimate value)  $default,){
final _that = this;
switch (_that) {
case _CardioLoadEstimate():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CardioLoadEstimate value)?  $default,){
final _that = this;
switch (_that) {
case _CardioLoadEstimate() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int score,  CardioLoadConfidence confidence,  CardioLoadMethod method,  double? trimpScore,  double coveredMinutes,  double expectedMinutes,  int? restingHeartRateBpm,  bool restingHeartRateObserved,  int? maxHeartRateBpm,  bool maxHeartRateObserved,  int heartRateSampleCount,  int activityWindowCount,  double activityWindowMinutes,  int movementFallbackScore)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CardioLoadEstimate() when $default != null:
return $default(_that.score,_that.confidence,_that.method,_that.trimpScore,_that.coveredMinutes,_that.expectedMinutes,_that.restingHeartRateBpm,_that.restingHeartRateObserved,_that.maxHeartRateBpm,_that.maxHeartRateObserved,_that.heartRateSampleCount,_that.activityWindowCount,_that.activityWindowMinutes,_that.movementFallbackScore);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int score,  CardioLoadConfidence confidence,  CardioLoadMethod method,  double? trimpScore,  double coveredMinutes,  double expectedMinutes,  int? restingHeartRateBpm,  bool restingHeartRateObserved,  int? maxHeartRateBpm,  bool maxHeartRateObserved,  int heartRateSampleCount,  int activityWindowCount,  double activityWindowMinutes,  int movementFallbackScore)  $default,) {final _that = this;
switch (_that) {
case _CardioLoadEstimate():
return $default(_that.score,_that.confidence,_that.method,_that.trimpScore,_that.coveredMinutes,_that.expectedMinutes,_that.restingHeartRateBpm,_that.restingHeartRateObserved,_that.maxHeartRateBpm,_that.maxHeartRateObserved,_that.heartRateSampleCount,_that.activityWindowCount,_that.activityWindowMinutes,_that.movementFallbackScore);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int score,  CardioLoadConfidence confidence,  CardioLoadMethod method,  double? trimpScore,  double coveredMinutes,  double expectedMinutes,  int? restingHeartRateBpm,  bool restingHeartRateObserved,  int? maxHeartRateBpm,  bool maxHeartRateObserved,  int heartRateSampleCount,  int activityWindowCount,  double activityWindowMinutes,  int movementFallbackScore)?  $default,) {final _that = this;
switch (_that) {
case _CardioLoadEstimate() when $default != null:
return $default(_that.score,_that.confidence,_that.method,_that.trimpScore,_that.coveredMinutes,_that.expectedMinutes,_that.restingHeartRateBpm,_that.restingHeartRateObserved,_that.maxHeartRateBpm,_that.maxHeartRateObserved,_that.heartRateSampleCount,_that.activityWindowCount,_that.activityWindowMinutes,_that.movementFallbackScore);case _:
  return null;

}
}

}

/// @nodoc


class _CardioLoadEstimate implements CardioLoadEstimate {
  const _CardioLoadEstimate({this.score = 0, this.confidence = CardioLoadConfidence.noData, this.method = CardioLoadMethod.noData, this.trimpScore, this.coveredMinutes = 0.0, this.expectedMinutes = 0.0, this.restingHeartRateBpm, this.restingHeartRateObserved = false, this.maxHeartRateBpm, this.maxHeartRateObserved = false, this.heartRateSampleCount = 0, this.activityWindowCount = 0, this.activityWindowMinutes = 0.0, this.movementFallbackScore = 0});
  

@override@JsonKey() final  int score;
@override@JsonKey() final  CardioLoadConfidence confidence;
@override@JsonKey() final  CardioLoadMethod method;
@override final  double? trimpScore;
@override@JsonKey() final  double coveredMinutes;
@override@JsonKey() final  double expectedMinutes;
@override final  int? restingHeartRateBpm;
@override@JsonKey() final  bool restingHeartRateObserved;
@override final  int? maxHeartRateBpm;
@override@JsonKey() final  bool maxHeartRateObserved;
@override@JsonKey() final  int heartRateSampleCount;
@override@JsonKey() final  int activityWindowCount;
@override@JsonKey() final  double activityWindowMinutes;
@override@JsonKey() final  int movementFallbackScore;

/// Create a copy of CardioLoadEstimate
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CardioLoadEstimateCopyWith<_CardioLoadEstimate> get copyWith => __$CardioLoadEstimateCopyWithImpl<_CardioLoadEstimate>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CardioLoadEstimate&&(identical(other.score, score) || other.score == score)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.method, method) || other.method == method)&&(identical(other.trimpScore, trimpScore) || other.trimpScore == trimpScore)&&(identical(other.coveredMinutes, coveredMinutes) || other.coveredMinutes == coveredMinutes)&&(identical(other.expectedMinutes, expectedMinutes) || other.expectedMinutes == expectedMinutes)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.restingHeartRateObserved, restingHeartRateObserved) || other.restingHeartRateObserved == restingHeartRateObserved)&&(identical(other.maxHeartRateBpm, maxHeartRateBpm) || other.maxHeartRateBpm == maxHeartRateBpm)&&(identical(other.maxHeartRateObserved, maxHeartRateObserved) || other.maxHeartRateObserved == maxHeartRateObserved)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.activityWindowCount, activityWindowCount) || other.activityWindowCount == activityWindowCount)&&(identical(other.activityWindowMinutes, activityWindowMinutes) || other.activityWindowMinutes == activityWindowMinutes)&&(identical(other.movementFallbackScore, movementFallbackScore) || other.movementFallbackScore == movementFallbackScore));
}


@override
int get hashCode => Object.hash(runtimeType,score,confidence,method,trimpScore,coveredMinutes,expectedMinutes,restingHeartRateBpm,restingHeartRateObserved,maxHeartRateBpm,maxHeartRateObserved,heartRateSampleCount,activityWindowCount,activityWindowMinutes,movementFallbackScore);

@override
String toString() {
  return 'CardioLoadEstimate(score: $score, confidence: $confidence, method: $method, trimpScore: $trimpScore, coveredMinutes: $coveredMinutes, expectedMinutes: $expectedMinutes, restingHeartRateBpm: $restingHeartRateBpm, restingHeartRateObserved: $restingHeartRateObserved, maxHeartRateBpm: $maxHeartRateBpm, maxHeartRateObserved: $maxHeartRateObserved, heartRateSampleCount: $heartRateSampleCount, activityWindowCount: $activityWindowCount, activityWindowMinutes: $activityWindowMinutes, movementFallbackScore: $movementFallbackScore)';
}


}

/// @nodoc
abstract mixin class _$CardioLoadEstimateCopyWith<$Res> implements $CardioLoadEstimateCopyWith<$Res> {
  factory _$CardioLoadEstimateCopyWith(_CardioLoadEstimate value, $Res Function(_CardioLoadEstimate) _then) = __$CardioLoadEstimateCopyWithImpl;
@override @useResult
$Res call({
 int score, CardioLoadConfidence confidence, CardioLoadMethod method, double? trimpScore, double coveredMinutes, double expectedMinutes, int? restingHeartRateBpm, bool restingHeartRateObserved, int? maxHeartRateBpm, bool maxHeartRateObserved, int heartRateSampleCount, int activityWindowCount, double activityWindowMinutes, int movementFallbackScore
});




}
/// @nodoc
class __$CardioLoadEstimateCopyWithImpl<$Res>
    implements _$CardioLoadEstimateCopyWith<$Res> {
  __$CardioLoadEstimateCopyWithImpl(this._self, this._then);

  final _CardioLoadEstimate _self;
  final $Res Function(_CardioLoadEstimate) _then;

/// Create a copy of CardioLoadEstimate
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? score = null,Object? confidence = null,Object? method = null,Object? trimpScore = freezed,Object? coveredMinutes = null,Object? expectedMinutes = null,Object? restingHeartRateBpm = freezed,Object? restingHeartRateObserved = null,Object? maxHeartRateBpm = freezed,Object? maxHeartRateObserved = null,Object? heartRateSampleCount = null,Object? activityWindowCount = null,Object? activityWindowMinutes = null,Object? movementFallbackScore = null,}) {
  return _then(_CardioLoadEstimate(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,method: null == method ? _self.method : method // ignore: cast_nullable_to_non_nullable
as CardioLoadMethod,trimpScore: freezed == trimpScore ? _self.trimpScore : trimpScore // ignore: cast_nullable_to_non_nullable
as double?,coveredMinutes: null == coveredMinutes ? _self.coveredMinutes : coveredMinutes // ignore: cast_nullable_to_non_nullable
as double,expectedMinutes: null == expectedMinutes ? _self.expectedMinutes : expectedMinutes // ignore: cast_nullable_to_non_nullable
as double,restingHeartRateBpm: freezed == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,restingHeartRateObserved: null == restingHeartRateObserved ? _self.restingHeartRateObserved : restingHeartRateObserved // ignore: cast_nullable_to_non_nullable
as bool,maxHeartRateBpm: freezed == maxHeartRateBpm ? _self.maxHeartRateBpm : maxHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateObserved: null == maxHeartRateObserved ? _self.maxHeartRateObserved : maxHeartRateObserved // ignore: cast_nullable_to_non_nullable
as bool,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,activityWindowCount: null == activityWindowCount ? _self.activityWindowCount : activityWindowCount // ignore: cast_nullable_to_non_nullable
as int,activityWindowMinutes: null == activityWindowMinutes ? _self.activityWindowMinutes : activityWindowMinutes // ignore: cast_nullable_to_non_nullable
as double,movementFallbackScore: null == movementFallbackScore ? _self.movementFallbackScore : movementFallbackScore // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$CardioLoadTimeWindow {

 DateTime get start; DateTime get end;
/// Create a copy of CardioLoadTimeWindow
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CardioLoadTimeWindowCopyWith<CardioLoadTimeWindow> get copyWith => _$CardioLoadTimeWindowCopyWithImpl<CardioLoadTimeWindow>(this as CardioLoadTimeWindow, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CardioLoadTimeWindow&&(identical(other.start, start) || other.start == start)&&(identical(other.end, end) || other.end == end));
}


@override
int get hashCode => Object.hash(runtimeType,start,end);

@override
String toString() {
  return 'CardioLoadTimeWindow(start: $start, end: $end)';
}


}

/// @nodoc
abstract mixin class $CardioLoadTimeWindowCopyWith<$Res>  {
  factory $CardioLoadTimeWindowCopyWith(CardioLoadTimeWindow value, $Res Function(CardioLoadTimeWindow) _then) = _$CardioLoadTimeWindowCopyWithImpl;
@useResult
$Res call({
 DateTime start, DateTime end
});




}
/// @nodoc
class _$CardioLoadTimeWindowCopyWithImpl<$Res>
    implements $CardioLoadTimeWindowCopyWith<$Res> {
  _$CardioLoadTimeWindowCopyWithImpl(this._self, this._then);

  final CardioLoadTimeWindow _self;
  final $Res Function(CardioLoadTimeWindow) _then;

/// Create a copy of CardioLoadTimeWindow
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? start = null,Object? end = null,}) {
  return _then(_self.copyWith(
start: null == start ? _self.start : start // ignore: cast_nullable_to_non_nullable
as DateTime,end: null == end ? _self.end : end // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [CardioLoadTimeWindow].
extension CardioLoadTimeWindowPatterns on CardioLoadTimeWindow {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CardioLoadTimeWindow value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CardioLoadTimeWindow() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CardioLoadTimeWindow value)  $default,){
final _that = this;
switch (_that) {
case _CardioLoadTimeWindow():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CardioLoadTimeWindow value)?  $default,){
final _that = this;
switch (_that) {
case _CardioLoadTimeWindow() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime start,  DateTime end)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CardioLoadTimeWindow() when $default != null:
return $default(_that.start,_that.end);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime start,  DateTime end)  $default,) {final _that = this;
switch (_that) {
case _CardioLoadTimeWindow():
return $default(_that.start,_that.end);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime start,  DateTime end)?  $default,) {final _that = this;
switch (_that) {
case _CardioLoadTimeWindow() when $default != null:
return $default(_that.start,_that.end);case _:
  return null;

}
}

}

/// @nodoc


class _CardioLoadTimeWindow extends CardioLoadTimeWindow {
  const _CardioLoadTimeWindow({required this.start, required this.end}): super._();
  

@override final  DateTime start;
@override final  DateTime end;

/// Create a copy of CardioLoadTimeWindow
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CardioLoadTimeWindowCopyWith<_CardioLoadTimeWindow> get copyWith => __$CardioLoadTimeWindowCopyWithImpl<_CardioLoadTimeWindow>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CardioLoadTimeWindow&&(identical(other.start, start) || other.start == start)&&(identical(other.end, end) || other.end == end));
}


@override
int get hashCode => Object.hash(runtimeType,start,end);

@override
String toString() {
  return 'CardioLoadTimeWindow(start: $start, end: $end)';
}


}

/// @nodoc
abstract mixin class _$CardioLoadTimeWindowCopyWith<$Res> implements $CardioLoadTimeWindowCopyWith<$Res> {
  factory _$CardioLoadTimeWindowCopyWith(_CardioLoadTimeWindow value, $Res Function(_CardioLoadTimeWindow) _then) = __$CardioLoadTimeWindowCopyWithImpl;
@override @useResult
$Res call({
 DateTime start, DateTime end
});




}
/// @nodoc
class __$CardioLoadTimeWindowCopyWithImpl<$Res>
    implements _$CardioLoadTimeWindowCopyWith<$Res> {
  __$CardioLoadTimeWindowCopyWithImpl(this._self, this._then);

  final _CardioLoadTimeWindow _self;
  final $Res Function(_CardioLoadTimeWindow) _then;

/// Create a copy of CardioLoadTimeWindow
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? start = null,Object? end = null,}) {
  return _then(_CardioLoadTimeWindow(
start: null == start ? _self.start : start // ignore: cast_nullable_to_non_nullable
as DateTime,end: null == end ? _self.end : end // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}

// dart format on
