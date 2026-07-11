// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_score.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepScoreEstimate {

 int get score; SleepScoreConfidence get confidence; double get durationPoints; double get efficiencyPoints; double get continuityPoints; double get regularityPoints; double get sleepDurationMinutes; double get timeInBedMinutes; double get sleepEfficiencyPercent; double get wakeAfterSleepOnsetMinutes; double? get regularityDifferenceMinutes; int get regularityBaselineNights; int get sleepStageCount; bool get usesSleepStages; bool get usesExplicitAwakeStages;
/// Create a copy of SleepScoreEstimate
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepScoreEstimateCopyWith<SleepScoreEstimate> get copyWith => _$SleepScoreEstimateCopyWithImpl<SleepScoreEstimate>(this as SleepScoreEstimate, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepScoreEstimate&&(identical(other.score, score) || other.score == score)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.durationPoints, durationPoints) || other.durationPoints == durationPoints)&&(identical(other.efficiencyPoints, efficiencyPoints) || other.efficiencyPoints == efficiencyPoints)&&(identical(other.continuityPoints, continuityPoints) || other.continuityPoints == continuityPoints)&&(identical(other.regularityPoints, regularityPoints) || other.regularityPoints == regularityPoints)&&(identical(other.sleepDurationMinutes, sleepDurationMinutes) || other.sleepDurationMinutes == sleepDurationMinutes)&&(identical(other.timeInBedMinutes, timeInBedMinutes) || other.timeInBedMinutes == timeInBedMinutes)&&(identical(other.sleepEfficiencyPercent, sleepEfficiencyPercent) || other.sleepEfficiencyPercent == sleepEfficiencyPercent)&&(identical(other.wakeAfterSleepOnsetMinutes, wakeAfterSleepOnsetMinutes) || other.wakeAfterSleepOnsetMinutes == wakeAfterSleepOnsetMinutes)&&(identical(other.regularityDifferenceMinutes, regularityDifferenceMinutes) || other.regularityDifferenceMinutes == regularityDifferenceMinutes)&&(identical(other.regularityBaselineNights, regularityBaselineNights) || other.regularityBaselineNights == regularityBaselineNights)&&(identical(other.sleepStageCount, sleepStageCount) || other.sleepStageCount == sleepStageCount)&&(identical(other.usesSleepStages, usesSleepStages) || other.usesSleepStages == usesSleepStages)&&(identical(other.usesExplicitAwakeStages, usesExplicitAwakeStages) || other.usesExplicitAwakeStages == usesExplicitAwakeStages));
}


@override
int get hashCode => Object.hash(runtimeType,score,confidence,durationPoints,efficiencyPoints,continuityPoints,regularityPoints,sleepDurationMinutes,timeInBedMinutes,sleepEfficiencyPercent,wakeAfterSleepOnsetMinutes,regularityDifferenceMinutes,regularityBaselineNights,sleepStageCount,usesSleepStages,usesExplicitAwakeStages);

@override
String toString() {
  return 'SleepScoreEstimate(score: $score, confidence: $confidence, durationPoints: $durationPoints, efficiencyPoints: $efficiencyPoints, continuityPoints: $continuityPoints, regularityPoints: $regularityPoints, sleepDurationMinutes: $sleepDurationMinutes, timeInBedMinutes: $timeInBedMinutes, sleepEfficiencyPercent: $sleepEfficiencyPercent, wakeAfterSleepOnsetMinutes: $wakeAfterSleepOnsetMinutes, regularityDifferenceMinutes: $regularityDifferenceMinutes, regularityBaselineNights: $regularityBaselineNights, sleepStageCount: $sleepStageCount, usesSleepStages: $usesSleepStages, usesExplicitAwakeStages: $usesExplicitAwakeStages)';
}


}

/// @nodoc
abstract mixin class $SleepScoreEstimateCopyWith<$Res>  {
  factory $SleepScoreEstimateCopyWith(SleepScoreEstimate value, $Res Function(SleepScoreEstimate) _then) = _$SleepScoreEstimateCopyWithImpl;
@useResult
$Res call({
 int score, SleepScoreConfidence confidence, double durationPoints, double efficiencyPoints, double continuityPoints, double regularityPoints, double sleepDurationMinutes, double timeInBedMinutes, double sleepEfficiencyPercent, double wakeAfterSleepOnsetMinutes, double? regularityDifferenceMinutes, int regularityBaselineNights, int sleepStageCount, bool usesSleepStages, bool usesExplicitAwakeStages
});




}
/// @nodoc
class _$SleepScoreEstimateCopyWithImpl<$Res>
    implements $SleepScoreEstimateCopyWith<$Res> {
  _$SleepScoreEstimateCopyWithImpl(this._self, this._then);

  final SleepScoreEstimate _self;
  final $Res Function(SleepScoreEstimate) _then;

/// Create a copy of SleepScoreEstimate
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? score = null,Object? confidence = null,Object? durationPoints = null,Object? efficiencyPoints = null,Object? continuityPoints = null,Object? regularityPoints = null,Object? sleepDurationMinutes = null,Object? timeInBedMinutes = null,Object? sleepEfficiencyPercent = null,Object? wakeAfterSleepOnsetMinutes = null,Object? regularityDifferenceMinutes = freezed,Object? regularityBaselineNights = null,Object? sleepStageCount = null,Object? usesSleepStages = null,Object? usesExplicitAwakeStages = null,}) {
  return _then(_self.copyWith(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as SleepScoreConfidence,durationPoints: null == durationPoints ? _self.durationPoints : durationPoints // ignore: cast_nullable_to_non_nullable
as double,efficiencyPoints: null == efficiencyPoints ? _self.efficiencyPoints : efficiencyPoints // ignore: cast_nullable_to_non_nullable
as double,continuityPoints: null == continuityPoints ? _self.continuityPoints : continuityPoints // ignore: cast_nullable_to_non_nullable
as double,regularityPoints: null == regularityPoints ? _self.regularityPoints : regularityPoints // ignore: cast_nullable_to_non_nullable
as double,sleepDurationMinutes: null == sleepDurationMinutes ? _self.sleepDurationMinutes : sleepDurationMinutes // ignore: cast_nullable_to_non_nullable
as double,timeInBedMinutes: null == timeInBedMinutes ? _self.timeInBedMinutes : timeInBedMinutes // ignore: cast_nullable_to_non_nullable
as double,sleepEfficiencyPercent: null == sleepEfficiencyPercent ? _self.sleepEfficiencyPercent : sleepEfficiencyPercent // ignore: cast_nullable_to_non_nullable
as double,wakeAfterSleepOnsetMinutes: null == wakeAfterSleepOnsetMinutes ? _self.wakeAfterSleepOnsetMinutes : wakeAfterSleepOnsetMinutes // ignore: cast_nullable_to_non_nullable
as double,regularityDifferenceMinutes: freezed == regularityDifferenceMinutes ? _self.regularityDifferenceMinutes : regularityDifferenceMinutes // ignore: cast_nullable_to_non_nullable
as double?,regularityBaselineNights: null == regularityBaselineNights ? _self.regularityBaselineNights : regularityBaselineNights // ignore: cast_nullable_to_non_nullable
as int,sleepStageCount: null == sleepStageCount ? _self.sleepStageCount : sleepStageCount // ignore: cast_nullable_to_non_nullable
as int,usesSleepStages: null == usesSleepStages ? _self.usesSleepStages : usesSleepStages // ignore: cast_nullable_to_non_nullable
as bool,usesExplicitAwakeStages: null == usesExplicitAwakeStages ? _self.usesExplicitAwakeStages : usesExplicitAwakeStages // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepScoreEstimate].
extension SleepScoreEstimatePatterns on SleepScoreEstimate {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepScoreEstimate value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepScoreEstimate() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepScoreEstimate value)  $default,){
final _that = this;
switch (_that) {
case _SleepScoreEstimate():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepScoreEstimate value)?  $default,){
final _that = this;
switch (_that) {
case _SleepScoreEstimate() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int score,  SleepScoreConfidence confidence,  double durationPoints,  double efficiencyPoints,  double continuityPoints,  double regularityPoints,  double sleepDurationMinutes,  double timeInBedMinutes,  double sleepEfficiencyPercent,  double wakeAfterSleepOnsetMinutes,  double? regularityDifferenceMinutes,  int regularityBaselineNights,  int sleepStageCount,  bool usesSleepStages,  bool usesExplicitAwakeStages)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepScoreEstimate() when $default != null:
return $default(_that.score,_that.confidence,_that.durationPoints,_that.efficiencyPoints,_that.continuityPoints,_that.regularityPoints,_that.sleepDurationMinutes,_that.timeInBedMinutes,_that.sleepEfficiencyPercent,_that.wakeAfterSleepOnsetMinutes,_that.regularityDifferenceMinutes,_that.regularityBaselineNights,_that.sleepStageCount,_that.usesSleepStages,_that.usesExplicitAwakeStages);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int score,  SleepScoreConfidence confidence,  double durationPoints,  double efficiencyPoints,  double continuityPoints,  double regularityPoints,  double sleepDurationMinutes,  double timeInBedMinutes,  double sleepEfficiencyPercent,  double wakeAfterSleepOnsetMinutes,  double? regularityDifferenceMinutes,  int regularityBaselineNights,  int sleepStageCount,  bool usesSleepStages,  bool usesExplicitAwakeStages)  $default,) {final _that = this;
switch (_that) {
case _SleepScoreEstimate():
return $default(_that.score,_that.confidence,_that.durationPoints,_that.efficiencyPoints,_that.continuityPoints,_that.regularityPoints,_that.sleepDurationMinutes,_that.timeInBedMinutes,_that.sleepEfficiencyPercent,_that.wakeAfterSleepOnsetMinutes,_that.regularityDifferenceMinutes,_that.regularityBaselineNights,_that.sleepStageCount,_that.usesSleepStages,_that.usesExplicitAwakeStages);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int score,  SleepScoreConfidence confidence,  double durationPoints,  double efficiencyPoints,  double continuityPoints,  double regularityPoints,  double sleepDurationMinutes,  double timeInBedMinutes,  double sleepEfficiencyPercent,  double wakeAfterSleepOnsetMinutes,  double? regularityDifferenceMinutes,  int regularityBaselineNights,  int sleepStageCount,  bool usesSleepStages,  bool usesExplicitAwakeStages)?  $default,) {final _that = this;
switch (_that) {
case _SleepScoreEstimate() when $default != null:
return $default(_that.score,_that.confidence,_that.durationPoints,_that.efficiencyPoints,_that.continuityPoints,_that.regularityPoints,_that.sleepDurationMinutes,_that.timeInBedMinutes,_that.sleepEfficiencyPercent,_that.wakeAfterSleepOnsetMinutes,_that.regularityDifferenceMinutes,_that.regularityBaselineNights,_that.sleepStageCount,_that.usesSleepStages,_that.usesExplicitAwakeStages);case _:
  return null;

}
}

}

/// @nodoc


class _SleepScoreEstimate implements SleepScoreEstimate {
  const _SleepScoreEstimate({this.score = 0, this.confidence = SleepScoreConfidence.noData, this.durationPoints = 0.0, this.efficiencyPoints = 0.0, this.continuityPoints = 0.0, this.regularityPoints = 0.0, this.sleepDurationMinutes = 0.0, this.timeInBedMinutes = 0.0, this.sleepEfficiencyPercent = 0.0, this.wakeAfterSleepOnsetMinutes = 0.0, this.regularityDifferenceMinutes, this.regularityBaselineNights = 0, this.sleepStageCount = 0, this.usesSleepStages = false, this.usesExplicitAwakeStages = false});
  

@override@JsonKey() final  int score;
@override@JsonKey() final  SleepScoreConfidence confidence;
@override@JsonKey() final  double durationPoints;
@override@JsonKey() final  double efficiencyPoints;
@override@JsonKey() final  double continuityPoints;
@override@JsonKey() final  double regularityPoints;
@override@JsonKey() final  double sleepDurationMinutes;
@override@JsonKey() final  double timeInBedMinutes;
@override@JsonKey() final  double sleepEfficiencyPercent;
@override@JsonKey() final  double wakeAfterSleepOnsetMinutes;
@override final  double? regularityDifferenceMinutes;
@override@JsonKey() final  int regularityBaselineNights;
@override@JsonKey() final  int sleepStageCount;
@override@JsonKey() final  bool usesSleepStages;
@override@JsonKey() final  bool usesExplicitAwakeStages;

/// Create a copy of SleepScoreEstimate
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepScoreEstimateCopyWith<_SleepScoreEstimate> get copyWith => __$SleepScoreEstimateCopyWithImpl<_SleepScoreEstimate>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepScoreEstimate&&(identical(other.score, score) || other.score == score)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.durationPoints, durationPoints) || other.durationPoints == durationPoints)&&(identical(other.efficiencyPoints, efficiencyPoints) || other.efficiencyPoints == efficiencyPoints)&&(identical(other.continuityPoints, continuityPoints) || other.continuityPoints == continuityPoints)&&(identical(other.regularityPoints, regularityPoints) || other.regularityPoints == regularityPoints)&&(identical(other.sleepDurationMinutes, sleepDurationMinutes) || other.sleepDurationMinutes == sleepDurationMinutes)&&(identical(other.timeInBedMinutes, timeInBedMinutes) || other.timeInBedMinutes == timeInBedMinutes)&&(identical(other.sleepEfficiencyPercent, sleepEfficiencyPercent) || other.sleepEfficiencyPercent == sleepEfficiencyPercent)&&(identical(other.wakeAfterSleepOnsetMinutes, wakeAfterSleepOnsetMinutes) || other.wakeAfterSleepOnsetMinutes == wakeAfterSleepOnsetMinutes)&&(identical(other.regularityDifferenceMinutes, regularityDifferenceMinutes) || other.regularityDifferenceMinutes == regularityDifferenceMinutes)&&(identical(other.regularityBaselineNights, regularityBaselineNights) || other.regularityBaselineNights == regularityBaselineNights)&&(identical(other.sleepStageCount, sleepStageCount) || other.sleepStageCount == sleepStageCount)&&(identical(other.usesSleepStages, usesSleepStages) || other.usesSleepStages == usesSleepStages)&&(identical(other.usesExplicitAwakeStages, usesExplicitAwakeStages) || other.usesExplicitAwakeStages == usesExplicitAwakeStages));
}


@override
int get hashCode => Object.hash(runtimeType,score,confidence,durationPoints,efficiencyPoints,continuityPoints,regularityPoints,sleepDurationMinutes,timeInBedMinutes,sleepEfficiencyPercent,wakeAfterSleepOnsetMinutes,regularityDifferenceMinutes,regularityBaselineNights,sleepStageCount,usesSleepStages,usesExplicitAwakeStages);

@override
String toString() {
  return 'SleepScoreEstimate(score: $score, confidence: $confidence, durationPoints: $durationPoints, efficiencyPoints: $efficiencyPoints, continuityPoints: $continuityPoints, regularityPoints: $regularityPoints, sleepDurationMinutes: $sleepDurationMinutes, timeInBedMinutes: $timeInBedMinutes, sleepEfficiencyPercent: $sleepEfficiencyPercent, wakeAfterSleepOnsetMinutes: $wakeAfterSleepOnsetMinutes, regularityDifferenceMinutes: $regularityDifferenceMinutes, regularityBaselineNights: $regularityBaselineNights, sleepStageCount: $sleepStageCount, usesSleepStages: $usesSleepStages, usesExplicitAwakeStages: $usesExplicitAwakeStages)';
}


}

/// @nodoc
abstract mixin class _$SleepScoreEstimateCopyWith<$Res> implements $SleepScoreEstimateCopyWith<$Res> {
  factory _$SleepScoreEstimateCopyWith(_SleepScoreEstimate value, $Res Function(_SleepScoreEstimate) _then) = __$SleepScoreEstimateCopyWithImpl;
@override @useResult
$Res call({
 int score, SleepScoreConfidence confidence, double durationPoints, double efficiencyPoints, double continuityPoints, double regularityPoints, double sleepDurationMinutes, double timeInBedMinutes, double sleepEfficiencyPercent, double wakeAfterSleepOnsetMinutes, double? regularityDifferenceMinutes, int regularityBaselineNights, int sleepStageCount, bool usesSleepStages, bool usesExplicitAwakeStages
});




}
/// @nodoc
class __$SleepScoreEstimateCopyWithImpl<$Res>
    implements _$SleepScoreEstimateCopyWith<$Res> {
  __$SleepScoreEstimateCopyWithImpl(this._self, this._then);

  final _SleepScoreEstimate _self;
  final $Res Function(_SleepScoreEstimate) _then;

/// Create a copy of SleepScoreEstimate
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? score = null,Object? confidence = null,Object? durationPoints = null,Object? efficiencyPoints = null,Object? continuityPoints = null,Object? regularityPoints = null,Object? sleepDurationMinutes = null,Object? timeInBedMinutes = null,Object? sleepEfficiencyPercent = null,Object? wakeAfterSleepOnsetMinutes = null,Object? regularityDifferenceMinutes = freezed,Object? regularityBaselineNights = null,Object? sleepStageCount = null,Object? usesSleepStages = null,Object? usesExplicitAwakeStages = null,}) {
  return _then(_SleepScoreEstimate(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as SleepScoreConfidence,durationPoints: null == durationPoints ? _self.durationPoints : durationPoints // ignore: cast_nullable_to_non_nullable
as double,efficiencyPoints: null == efficiencyPoints ? _self.efficiencyPoints : efficiencyPoints // ignore: cast_nullable_to_non_nullable
as double,continuityPoints: null == continuityPoints ? _self.continuityPoints : continuityPoints // ignore: cast_nullable_to_non_nullable
as double,regularityPoints: null == regularityPoints ? _self.regularityPoints : regularityPoints // ignore: cast_nullable_to_non_nullable
as double,sleepDurationMinutes: null == sleepDurationMinutes ? _self.sleepDurationMinutes : sleepDurationMinutes // ignore: cast_nullable_to_non_nullable
as double,timeInBedMinutes: null == timeInBedMinutes ? _self.timeInBedMinutes : timeInBedMinutes // ignore: cast_nullable_to_non_nullable
as double,sleepEfficiencyPercent: null == sleepEfficiencyPercent ? _self.sleepEfficiencyPercent : sleepEfficiencyPercent // ignore: cast_nullable_to_non_nullable
as double,wakeAfterSleepOnsetMinutes: null == wakeAfterSleepOnsetMinutes ? _self.wakeAfterSleepOnsetMinutes : wakeAfterSleepOnsetMinutes // ignore: cast_nullable_to_non_nullable
as double,regularityDifferenceMinutes: freezed == regularityDifferenceMinutes ? _self.regularityDifferenceMinutes : regularityDifferenceMinutes // ignore: cast_nullable_to_non_nullable
as double?,regularityBaselineNights: null == regularityBaselineNights ? _self.regularityBaselineNights : regularityBaselineNights // ignore: cast_nullable_to_non_nullable
as int,sleepStageCount: null == sleepStageCount ? _self.sleepStageCount : sleepStageCount // ignore: cast_nullable_to_non_nullable
as int,usesSleepStages: null == usesSleepStages ? _self.usesSleepStages : usesSleepStages // ignore: cast_nullable_to_non_nullable
as bool,usesExplicitAwakeStages: null == usesExplicitAwakeStages ? _self.usesExplicitAwakeStages : usesExplicitAwakeStages // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
