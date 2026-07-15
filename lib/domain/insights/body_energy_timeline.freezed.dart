// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_energy_timeline.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyEnergyTimelinePoint {

 DateTime get time; int get score; double get delta; BodyEnergyBucketState get state; BodyEnergyConfidence get confidence; double get charge; double get intensityDrain; double get activityEnergyDrain; double get basalDrain; double get stressDrain; double get recoveryDebtDrain; BodyEnergyPrimaryInfluence get primaryInfluence;
/// Create a copy of BodyEnergyTimelinePoint
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyEnergyTimelinePointCopyWith<BodyEnergyTimelinePoint> get copyWith => _$BodyEnergyTimelinePointCopyWithImpl<BodyEnergyTimelinePoint>(this as BodyEnergyTimelinePoint, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyEnergyTimelinePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.score, score) || other.score == score)&&(identical(other.delta, delta) || other.delta == delta)&&(identical(other.state, state) || other.state == state)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.charge, charge) || other.charge == charge)&&(identical(other.intensityDrain, intensityDrain) || other.intensityDrain == intensityDrain)&&(identical(other.activityEnergyDrain, activityEnergyDrain) || other.activityEnergyDrain == activityEnergyDrain)&&(identical(other.basalDrain, basalDrain) || other.basalDrain == basalDrain)&&(identical(other.stressDrain, stressDrain) || other.stressDrain == stressDrain)&&(identical(other.recoveryDebtDrain, recoveryDebtDrain) || other.recoveryDebtDrain == recoveryDebtDrain)&&(identical(other.primaryInfluence, primaryInfluence) || other.primaryInfluence == primaryInfluence));
}


@override
int get hashCode => Object.hash(runtimeType,time,score,delta,state,confidence,charge,intensityDrain,activityEnergyDrain,basalDrain,stressDrain,recoveryDebtDrain,primaryInfluence);

@override
String toString() {
  return 'BodyEnergyTimelinePoint(time: $time, score: $score, delta: $delta, state: $state, confidence: $confidence, charge: $charge, intensityDrain: $intensityDrain, activityEnergyDrain: $activityEnergyDrain, basalDrain: $basalDrain, stressDrain: $stressDrain, recoveryDebtDrain: $recoveryDebtDrain, primaryInfluence: $primaryInfluence)';
}


}

/// @nodoc
abstract mixin class $BodyEnergyTimelinePointCopyWith<$Res>  {
  factory $BodyEnergyTimelinePointCopyWith(BodyEnergyTimelinePoint value, $Res Function(BodyEnergyTimelinePoint) _then) = _$BodyEnergyTimelinePointCopyWithImpl;
@useResult
$Res call({
 DateTime time, int score, double delta, BodyEnergyBucketState state, BodyEnergyConfidence confidence, double charge, double intensityDrain, double activityEnergyDrain, double basalDrain, double stressDrain, double recoveryDebtDrain, BodyEnergyPrimaryInfluence primaryInfluence
});




}
/// @nodoc
class _$BodyEnergyTimelinePointCopyWithImpl<$Res>
    implements $BodyEnergyTimelinePointCopyWith<$Res> {
  _$BodyEnergyTimelinePointCopyWithImpl(this._self, this._then);

  final BodyEnergyTimelinePoint _self;
  final $Res Function(BodyEnergyTimelinePoint) _then;

/// Create a copy of BodyEnergyTimelinePoint
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? score = null,Object? delta = null,Object? state = null,Object? confidence = null,Object? charge = null,Object? intensityDrain = null,Object? activityEnergyDrain = null,Object? basalDrain = null,Object? stressDrain = null,Object? recoveryDebtDrain = null,Object? primaryInfluence = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,delta: null == delta ? _self.delta : delta // ignore: cast_nullable_to_non_nullable
as double,state: null == state ? _self.state : state // ignore: cast_nullable_to_non_nullable
as BodyEnergyBucketState,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as BodyEnergyConfidence,charge: null == charge ? _self.charge : charge // ignore: cast_nullable_to_non_nullable
as double,intensityDrain: null == intensityDrain ? _self.intensityDrain : intensityDrain // ignore: cast_nullable_to_non_nullable
as double,activityEnergyDrain: null == activityEnergyDrain ? _self.activityEnergyDrain : activityEnergyDrain // ignore: cast_nullable_to_non_nullable
as double,basalDrain: null == basalDrain ? _self.basalDrain : basalDrain // ignore: cast_nullable_to_non_nullable
as double,stressDrain: null == stressDrain ? _self.stressDrain : stressDrain // ignore: cast_nullable_to_non_nullable
as double,recoveryDebtDrain: null == recoveryDebtDrain ? _self.recoveryDebtDrain : recoveryDebtDrain // ignore: cast_nullable_to_non_nullable
as double,primaryInfluence: null == primaryInfluence ? _self.primaryInfluence : primaryInfluence // ignore: cast_nullable_to_non_nullable
as BodyEnergyPrimaryInfluence,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyEnergyTimelinePoint].
extension BodyEnergyTimelinePointPatterns on BodyEnergyTimelinePoint {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _BodyEnergyTimelinePoint value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _BodyEnergyTimelinePoint value)  build,}){
final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint():
return build(_that);case _:
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _BodyEnergyTimelinePoint value)?  build,}){
final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( DateTime time,  int score,  double delta,  BodyEnergyBucketState state,  BodyEnergyConfidence confidence,  double charge,  double intensityDrain,  double activityEnergyDrain,  double basalDrain,  double stressDrain,  double recoveryDebtDrain,  BodyEnergyPrimaryInfluence primaryInfluence)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint() when build != null:
return build(_that.time,_that.score,_that.delta,_that.state,_that.confidence,_that.charge,_that.intensityDrain,_that.activityEnergyDrain,_that.basalDrain,_that.stressDrain,_that.recoveryDebtDrain,_that.primaryInfluence);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( DateTime time,  int score,  double delta,  BodyEnergyBucketState state,  BodyEnergyConfidence confidence,  double charge,  double intensityDrain,  double activityEnergyDrain,  double basalDrain,  double stressDrain,  double recoveryDebtDrain,  BodyEnergyPrimaryInfluence primaryInfluence)  build,}) {final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint():
return build(_that.time,_that.score,_that.delta,_that.state,_that.confidence,_that.charge,_that.intensityDrain,_that.activityEnergyDrain,_that.basalDrain,_that.stressDrain,_that.recoveryDebtDrain,_that.primaryInfluence);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( DateTime time,  int score,  double delta,  BodyEnergyBucketState state,  BodyEnergyConfidence confidence,  double charge,  double intensityDrain,  double activityEnergyDrain,  double basalDrain,  double stressDrain,  double recoveryDebtDrain,  BodyEnergyPrimaryInfluence primaryInfluence)?  build,}) {final _that = this;
switch (_that) {
case _BodyEnergyTimelinePoint() when build != null:
return build(_that.time,_that.score,_that.delta,_that.state,_that.confidence,_that.charge,_that.intensityDrain,_that.activityEnergyDrain,_that.basalDrain,_that.stressDrain,_that.recoveryDebtDrain,_that.primaryInfluence);case _:
  return null;

}
}

}

/// @nodoc


class _BodyEnergyTimelinePoint extends BodyEnergyTimelinePoint {
  const _BodyEnergyTimelinePoint({required this.time, required this.score, required this.delta, required this.state, required this.confidence, required this.charge, required this.intensityDrain, required this.activityEnergyDrain, required this.basalDrain, required this.stressDrain, required this.recoveryDebtDrain, required this.primaryInfluence}): super._();
  

@override final  DateTime time;
@override final  int score;
@override final  double delta;
@override final  BodyEnergyBucketState state;
@override final  BodyEnergyConfidence confidence;
@override final  double charge;
@override final  double intensityDrain;
@override final  double activityEnergyDrain;
@override final  double basalDrain;
@override final  double stressDrain;
@override final  double recoveryDebtDrain;
@override final  BodyEnergyPrimaryInfluence primaryInfluence;

/// Create a copy of BodyEnergyTimelinePoint
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyEnergyTimelinePointCopyWith<_BodyEnergyTimelinePoint> get copyWith => __$BodyEnergyTimelinePointCopyWithImpl<_BodyEnergyTimelinePoint>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyEnergyTimelinePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.score, score) || other.score == score)&&(identical(other.delta, delta) || other.delta == delta)&&(identical(other.state, state) || other.state == state)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.charge, charge) || other.charge == charge)&&(identical(other.intensityDrain, intensityDrain) || other.intensityDrain == intensityDrain)&&(identical(other.activityEnergyDrain, activityEnergyDrain) || other.activityEnergyDrain == activityEnergyDrain)&&(identical(other.basalDrain, basalDrain) || other.basalDrain == basalDrain)&&(identical(other.stressDrain, stressDrain) || other.stressDrain == stressDrain)&&(identical(other.recoveryDebtDrain, recoveryDebtDrain) || other.recoveryDebtDrain == recoveryDebtDrain)&&(identical(other.primaryInfluence, primaryInfluence) || other.primaryInfluence == primaryInfluence));
}


@override
int get hashCode => Object.hash(runtimeType,time,score,delta,state,confidence,charge,intensityDrain,activityEnergyDrain,basalDrain,stressDrain,recoveryDebtDrain,primaryInfluence);

@override
String toString() {
  return 'BodyEnergyTimelinePoint.build(time: $time, score: $score, delta: $delta, state: $state, confidence: $confidence, charge: $charge, intensityDrain: $intensityDrain, activityEnergyDrain: $activityEnergyDrain, basalDrain: $basalDrain, stressDrain: $stressDrain, recoveryDebtDrain: $recoveryDebtDrain, primaryInfluence: $primaryInfluence)';
}


}

/// @nodoc
abstract mixin class _$BodyEnergyTimelinePointCopyWith<$Res> implements $BodyEnergyTimelinePointCopyWith<$Res> {
  factory _$BodyEnergyTimelinePointCopyWith(_BodyEnergyTimelinePoint value, $Res Function(_BodyEnergyTimelinePoint) _then) = __$BodyEnergyTimelinePointCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int score, double delta, BodyEnergyBucketState state, BodyEnergyConfidence confidence, double charge, double intensityDrain, double activityEnergyDrain, double basalDrain, double stressDrain, double recoveryDebtDrain, BodyEnergyPrimaryInfluence primaryInfluence
});




}
/// @nodoc
class __$BodyEnergyTimelinePointCopyWithImpl<$Res>
    implements _$BodyEnergyTimelinePointCopyWith<$Res> {
  __$BodyEnergyTimelinePointCopyWithImpl(this._self, this._then);

  final _BodyEnergyTimelinePoint _self;
  final $Res Function(_BodyEnergyTimelinePoint) _then;

/// Create a copy of BodyEnergyTimelinePoint
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? score = null,Object? delta = null,Object? state = null,Object? confidence = null,Object? charge = null,Object? intensityDrain = null,Object? activityEnergyDrain = null,Object? basalDrain = null,Object? stressDrain = null,Object? recoveryDebtDrain = null,Object? primaryInfluence = null,}) {
  return _then(_BodyEnergyTimelinePoint(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,delta: null == delta ? _self.delta : delta // ignore: cast_nullable_to_non_nullable
as double,state: null == state ? _self.state : state // ignore: cast_nullable_to_non_nullable
as BodyEnergyBucketState,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as BodyEnergyConfidence,charge: null == charge ? _self.charge : charge // ignore: cast_nullable_to_non_nullable
as double,intensityDrain: null == intensityDrain ? _self.intensityDrain : intensityDrain // ignore: cast_nullable_to_non_nullable
as double,activityEnergyDrain: null == activityEnergyDrain ? _self.activityEnergyDrain : activityEnergyDrain // ignore: cast_nullable_to_non_nullable
as double,basalDrain: null == basalDrain ? _self.basalDrain : basalDrain // ignore: cast_nullable_to_non_nullable
as double,stressDrain: null == stressDrain ? _self.stressDrain : stressDrain // ignore: cast_nullable_to_non_nullable
as double,recoveryDebtDrain: null == recoveryDebtDrain ? _self.recoveryDebtDrain : recoveryDebtDrain // ignore: cast_nullable_to_non_nullable
as double,primaryInfluence: null == primaryInfluence ? _self.primaryInfluence : primaryInfluence // ignore: cast_nullable_to_non_nullable
as BodyEnergyPrimaryInfluence,
  ));
}


}

/// @nodoc
mixin _$BodyEnergyInputSummary {

 int get algorithmVersion; int get bucketMinutes; int get heartRateSampleCount; int get hrvSampleCount; int get sleepSessionCount; int get workoutCount; int get respiratorySampleCount; bool get hasRestingHeartRate; bool get hasBaselineRestingHeartRate; bool get hasObservedMaxHeartRate; bool get hasHrvBaseline; bool get hasRespiratoryBaseline; int? get previousEndScore; BodyEnergyCalibrationMode get calibrationMode;
/// Create a copy of BodyEnergyInputSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyEnergyInputSummaryCopyWith<BodyEnergyInputSummary> get copyWith => _$BodyEnergyInputSummaryCopyWithImpl<BodyEnergyInputSummary>(this as BodyEnergyInputSummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyEnergyInputSummary&&(identical(other.algorithmVersion, algorithmVersion) || other.algorithmVersion == algorithmVersion)&&(identical(other.bucketMinutes, bucketMinutes) || other.bucketMinutes == bucketMinutes)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.hrvSampleCount, hrvSampleCount) || other.hrvSampleCount == hrvSampleCount)&&(identical(other.sleepSessionCount, sleepSessionCount) || other.sleepSessionCount == sleepSessionCount)&&(identical(other.workoutCount, workoutCount) || other.workoutCount == workoutCount)&&(identical(other.respiratorySampleCount, respiratorySampleCount) || other.respiratorySampleCount == respiratorySampleCount)&&(identical(other.hasRestingHeartRate, hasRestingHeartRate) || other.hasRestingHeartRate == hasRestingHeartRate)&&(identical(other.hasBaselineRestingHeartRate, hasBaselineRestingHeartRate) || other.hasBaselineRestingHeartRate == hasBaselineRestingHeartRate)&&(identical(other.hasObservedMaxHeartRate, hasObservedMaxHeartRate) || other.hasObservedMaxHeartRate == hasObservedMaxHeartRate)&&(identical(other.hasHrvBaseline, hasHrvBaseline) || other.hasHrvBaseline == hasHrvBaseline)&&(identical(other.hasRespiratoryBaseline, hasRespiratoryBaseline) || other.hasRespiratoryBaseline == hasRespiratoryBaseline)&&(identical(other.previousEndScore, previousEndScore) || other.previousEndScore == previousEndScore)&&(identical(other.calibrationMode, calibrationMode) || other.calibrationMode == calibrationMode));
}


@override
int get hashCode => Object.hash(runtimeType,algorithmVersion,bucketMinutes,heartRateSampleCount,hrvSampleCount,sleepSessionCount,workoutCount,respiratorySampleCount,hasRestingHeartRate,hasBaselineRestingHeartRate,hasObservedMaxHeartRate,hasHrvBaseline,hasRespiratoryBaseline,previousEndScore,calibrationMode);

@override
String toString() {
  return 'BodyEnergyInputSummary(algorithmVersion: $algorithmVersion, bucketMinutes: $bucketMinutes, heartRateSampleCount: $heartRateSampleCount, hrvSampleCount: $hrvSampleCount, sleepSessionCount: $sleepSessionCount, workoutCount: $workoutCount, respiratorySampleCount: $respiratorySampleCount, hasRestingHeartRate: $hasRestingHeartRate, hasBaselineRestingHeartRate: $hasBaselineRestingHeartRate, hasObservedMaxHeartRate: $hasObservedMaxHeartRate, hasHrvBaseline: $hasHrvBaseline, hasRespiratoryBaseline: $hasRespiratoryBaseline, previousEndScore: $previousEndScore, calibrationMode: $calibrationMode)';
}


}

/// @nodoc
abstract mixin class $BodyEnergyInputSummaryCopyWith<$Res>  {
  factory $BodyEnergyInputSummaryCopyWith(BodyEnergyInputSummary value, $Res Function(BodyEnergyInputSummary) _then) = _$BodyEnergyInputSummaryCopyWithImpl;
@useResult
$Res call({
 int algorithmVersion, int bucketMinutes, int heartRateSampleCount, int hrvSampleCount, int sleepSessionCount, int workoutCount, int respiratorySampleCount, bool hasRestingHeartRate, bool hasBaselineRestingHeartRate, bool hasObservedMaxHeartRate, bool hasHrvBaseline, bool hasRespiratoryBaseline, int? previousEndScore, BodyEnergyCalibrationMode calibrationMode
});




}
/// @nodoc
class _$BodyEnergyInputSummaryCopyWithImpl<$Res>
    implements $BodyEnergyInputSummaryCopyWith<$Res> {
  _$BodyEnergyInputSummaryCopyWithImpl(this._self, this._then);

  final BodyEnergyInputSummary _self;
  final $Res Function(BodyEnergyInputSummary) _then;

/// Create a copy of BodyEnergyInputSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? algorithmVersion = null,Object? bucketMinutes = null,Object? heartRateSampleCount = null,Object? hrvSampleCount = null,Object? sleepSessionCount = null,Object? workoutCount = null,Object? respiratorySampleCount = null,Object? hasRestingHeartRate = null,Object? hasBaselineRestingHeartRate = null,Object? hasObservedMaxHeartRate = null,Object? hasHrvBaseline = null,Object? hasRespiratoryBaseline = null,Object? previousEndScore = freezed,Object? calibrationMode = null,}) {
  return _then(_self.copyWith(
algorithmVersion: null == algorithmVersion ? _self.algorithmVersion : algorithmVersion // ignore: cast_nullable_to_non_nullable
as int,bucketMinutes: null == bucketMinutes ? _self.bucketMinutes : bucketMinutes // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,hrvSampleCount: null == hrvSampleCount ? _self.hrvSampleCount : hrvSampleCount // ignore: cast_nullable_to_non_nullable
as int,sleepSessionCount: null == sleepSessionCount ? _self.sleepSessionCount : sleepSessionCount // ignore: cast_nullable_to_non_nullable
as int,workoutCount: null == workoutCount ? _self.workoutCount : workoutCount // ignore: cast_nullable_to_non_nullable
as int,respiratorySampleCount: null == respiratorySampleCount ? _self.respiratorySampleCount : respiratorySampleCount // ignore: cast_nullable_to_non_nullable
as int,hasRestingHeartRate: null == hasRestingHeartRate ? _self.hasRestingHeartRate : hasRestingHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasBaselineRestingHeartRate: null == hasBaselineRestingHeartRate ? _self.hasBaselineRestingHeartRate : hasBaselineRestingHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasObservedMaxHeartRate: null == hasObservedMaxHeartRate ? _self.hasObservedMaxHeartRate : hasObservedMaxHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasHrvBaseline: null == hasHrvBaseline ? _self.hasHrvBaseline : hasHrvBaseline // ignore: cast_nullable_to_non_nullable
as bool,hasRespiratoryBaseline: null == hasRespiratoryBaseline ? _self.hasRespiratoryBaseline : hasRespiratoryBaseline // ignore: cast_nullable_to_non_nullable
as bool,previousEndScore: freezed == previousEndScore ? _self.previousEndScore : previousEndScore // ignore: cast_nullable_to_non_nullable
as int?,calibrationMode: null == calibrationMode ? _self.calibrationMode : calibrationMode // ignore: cast_nullable_to_non_nullable
as BodyEnergyCalibrationMode,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyEnergyInputSummary].
extension BodyEnergyInputSummaryPatterns on BodyEnergyInputSummary {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyEnergyInputSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyEnergyInputSummary() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyEnergyInputSummary value)  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyInputSummary():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyEnergyInputSummary value)?  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyInputSummary() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int algorithmVersion,  int bucketMinutes,  int heartRateSampleCount,  int hrvSampleCount,  int sleepSessionCount,  int workoutCount,  int respiratorySampleCount,  bool hasRestingHeartRate,  bool hasBaselineRestingHeartRate,  bool hasObservedMaxHeartRate,  bool hasHrvBaseline,  bool hasRespiratoryBaseline,  int? previousEndScore,  BodyEnergyCalibrationMode calibrationMode)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyEnergyInputSummary() when $default != null:
return $default(_that.algorithmVersion,_that.bucketMinutes,_that.heartRateSampleCount,_that.hrvSampleCount,_that.sleepSessionCount,_that.workoutCount,_that.respiratorySampleCount,_that.hasRestingHeartRate,_that.hasBaselineRestingHeartRate,_that.hasObservedMaxHeartRate,_that.hasHrvBaseline,_that.hasRespiratoryBaseline,_that.previousEndScore,_that.calibrationMode);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int algorithmVersion,  int bucketMinutes,  int heartRateSampleCount,  int hrvSampleCount,  int sleepSessionCount,  int workoutCount,  int respiratorySampleCount,  bool hasRestingHeartRate,  bool hasBaselineRestingHeartRate,  bool hasObservedMaxHeartRate,  bool hasHrvBaseline,  bool hasRespiratoryBaseline,  int? previousEndScore,  BodyEnergyCalibrationMode calibrationMode)  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyInputSummary():
return $default(_that.algorithmVersion,_that.bucketMinutes,_that.heartRateSampleCount,_that.hrvSampleCount,_that.sleepSessionCount,_that.workoutCount,_that.respiratorySampleCount,_that.hasRestingHeartRate,_that.hasBaselineRestingHeartRate,_that.hasObservedMaxHeartRate,_that.hasHrvBaseline,_that.hasRespiratoryBaseline,_that.previousEndScore,_that.calibrationMode);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int algorithmVersion,  int bucketMinutes,  int heartRateSampleCount,  int hrvSampleCount,  int sleepSessionCount,  int workoutCount,  int respiratorySampleCount,  bool hasRestingHeartRate,  bool hasBaselineRestingHeartRate,  bool hasObservedMaxHeartRate,  bool hasHrvBaseline,  bool hasRespiratoryBaseline,  int? previousEndScore,  BodyEnergyCalibrationMode calibrationMode)?  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyInputSummary() when $default != null:
return $default(_that.algorithmVersion,_that.bucketMinutes,_that.heartRateSampleCount,_that.hrvSampleCount,_that.sleepSessionCount,_that.workoutCount,_that.respiratorySampleCount,_that.hasRestingHeartRate,_that.hasBaselineRestingHeartRate,_that.hasObservedMaxHeartRate,_that.hasHrvBaseline,_that.hasRespiratoryBaseline,_that.previousEndScore,_that.calibrationMode);case _:
  return null;

}
}

}

/// @nodoc


class _BodyEnergyInputSummary implements BodyEnergyInputSummary {
  const _BodyEnergyInputSummary({this.algorithmVersion = bodyEnergyTimelineAlgorithmVersion, this.bucketMinutes = bodyEnergyTimelineBucketMinutes, this.heartRateSampleCount = 0, this.hrvSampleCount = 0, this.sleepSessionCount = 0, this.workoutCount = 0, this.respiratorySampleCount = 0, this.hasRestingHeartRate = false, this.hasBaselineRestingHeartRate = false, this.hasObservedMaxHeartRate = false, this.hasHrvBaseline = false, this.hasRespiratoryBaseline = false, this.previousEndScore, this.calibrationMode = BodyEnergyCalibrationMode.automatic});
  

@override@JsonKey() final  int algorithmVersion;
@override@JsonKey() final  int bucketMinutes;
@override@JsonKey() final  int heartRateSampleCount;
@override@JsonKey() final  int hrvSampleCount;
@override@JsonKey() final  int sleepSessionCount;
@override@JsonKey() final  int workoutCount;
@override@JsonKey() final  int respiratorySampleCount;
@override@JsonKey() final  bool hasRestingHeartRate;
@override@JsonKey() final  bool hasBaselineRestingHeartRate;
@override@JsonKey() final  bool hasObservedMaxHeartRate;
@override@JsonKey() final  bool hasHrvBaseline;
@override@JsonKey() final  bool hasRespiratoryBaseline;
@override final  int? previousEndScore;
@override@JsonKey() final  BodyEnergyCalibrationMode calibrationMode;

/// Create a copy of BodyEnergyInputSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyEnergyInputSummaryCopyWith<_BodyEnergyInputSummary> get copyWith => __$BodyEnergyInputSummaryCopyWithImpl<_BodyEnergyInputSummary>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyEnergyInputSummary&&(identical(other.algorithmVersion, algorithmVersion) || other.algorithmVersion == algorithmVersion)&&(identical(other.bucketMinutes, bucketMinutes) || other.bucketMinutes == bucketMinutes)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.hrvSampleCount, hrvSampleCount) || other.hrvSampleCount == hrvSampleCount)&&(identical(other.sleepSessionCount, sleepSessionCount) || other.sleepSessionCount == sleepSessionCount)&&(identical(other.workoutCount, workoutCount) || other.workoutCount == workoutCount)&&(identical(other.respiratorySampleCount, respiratorySampleCount) || other.respiratorySampleCount == respiratorySampleCount)&&(identical(other.hasRestingHeartRate, hasRestingHeartRate) || other.hasRestingHeartRate == hasRestingHeartRate)&&(identical(other.hasBaselineRestingHeartRate, hasBaselineRestingHeartRate) || other.hasBaselineRestingHeartRate == hasBaselineRestingHeartRate)&&(identical(other.hasObservedMaxHeartRate, hasObservedMaxHeartRate) || other.hasObservedMaxHeartRate == hasObservedMaxHeartRate)&&(identical(other.hasHrvBaseline, hasHrvBaseline) || other.hasHrvBaseline == hasHrvBaseline)&&(identical(other.hasRespiratoryBaseline, hasRespiratoryBaseline) || other.hasRespiratoryBaseline == hasRespiratoryBaseline)&&(identical(other.previousEndScore, previousEndScore) || other.previousEndScore == previousEndScore)&&(identical(other.calibrationMode, calibrationMode) || other.calibrationMode == calibrationMode));
}


@override
int get hashCode => Object.hash(runtimeType,algorithmVersion,bucketMinutes,heartRateSampleCount,hrvSampleCount,sleepSessionCount,workoutCount,respiratorySampleCount,hasRestingHeartRate,hasBaselineRestingHeartRate,hasObservedMaxHeartRate,hasHrvBaseline,hasRespiratoryBaseline,previousEndScore,calibrationMode);

@override
String toString() {
  return 'BodyEnergyInputSummary(algorithmVersion: $algorithmVersion, bucketMinutes: $bucketMinutes, heartRateSampleCount: $heartRateSampleCount, hrvSampleCount: $hrvSampleCount, sleepSessionCount: $sleepSessionCount, workoutCount: $workoutCount, respiratorySampleCount: $respiratorySampleCount, hasRestingHeartRate: $hasRestingHeartRate, hasBaselineRestingHeartRate: $hasBaselineRestingHeartRate, hasObservedMaxHeartRate: $hasObservedMaxHeartRate, hasHrvBaseline: $hasHrvBaseline, hasRespiratoryBaseline: $hasRespiratoryBaseline, previousEndScore: $previousEndScore, calibrationMode: $calibrationMode)';
}


}

/// @nodoc
abstract mixin class _$BodyEnergyInputSummaryCopyWith<$Res> implements $BodyEnergyInputSummaryCopyWith<$Res> {
  factory _$BodyEnergyInputSummaryCopyWith(_BodyEnergyInputSummary value, $Res Function(_BodyEnergyInputSummary) _then) = __$BodyEnergyInputSummaryCopyWithImpl;
@override @useResult
$Res call({
 int algorithmVersion, int bucketMinutes, int heartRateSampleCount, int hrvSampleCount, int sleepSessionCount, int workoutCount, int respiratorySampleCount, bool hasRestingHeartRate, bool hasBaselineRestingHeartRate, bool hasObservedMaxHeartRate, bool hasHrvBaseline, bool hasRespiratoryBaseline, int? previousEndScore, BodyEnergyCalibrationMode calibrationMode
});




}
/// @nodoc
class __$BodyEnergyInputSummaryCopyWithImpl<$Res>
    implements _$BodyEnergyInputSummaryCopyWith<$Res> {
  __$BodyEnergyInputSummaryCopyWithImpl(this._self, this._then);

  final _BodyEnergyInputSummary _self;
  final $Res Function(_BodyEnergyInputSummary) _then;

/// Create a copy of BodyEnergyInputSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? algorithmVersion = null,Object? bucketMinutes = null,Object? heartRateSampleCount = null,Object? hrvSampleCount = null,Object? sleepSessionCount = null,Object? workoutCount = null,Object? respiratorySampleCount = null,Object? hasRestingHeartRate = null,Object? hasBaselineRestingHeartRate = null,Object? hasObservedMaxHeartRate = null,Object? hasHrvBaseline = null,Object? hasRespiratoryBaseline = null,Object? previousEndScore = freezed,Object? calibrationMode = null,}) {
  return _then(_BodyEnergyInputSummary(
algorithmVersion: null == algorithmVersion ? _self.algorithmVersion : algorithmVersion // ignore: cast_nullable_to_non_nullable
as int,bucketMinutes: null == bucketMinutes ? _self.bucketMinutes : bucketMinutes // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,hrvSampleCount: null == hrvSampleCount ? _self.hrvSampleCount : hrvSampleCount // ignore: cast_nullable_to_non_nullable
as int,sleepSessionCount: null == sleepSessionCount ? _self.sleepSessionCount : sleepSessionCount // ignore: cast_nullable_to_non_nullable
as int,workoutCount: null == workoutCount ? _self.workoutCount : workoutCount // ignore: cast_nullable_to_non_nullable
as int,respiratorySampleCount: null == respiratorySampleCount ? _self.respiratorySampleCount : respiratorySampleCount // ignore: cast_nullable_to_non_nullable
as int,hasRestingHeartRate: null == hasRestingHeartRate ? _self.hasRestingHeartRate : hasRestingHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasBaselineRestingHeartRate: null == hasBaselineRestingHeartRate ? _self.hasBaselineRestingHeartRate : hasBaselineRestingHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasObservedMaxHeartRate: null == hasObservedMaxHeartRate ? _self.hasObservedMaxHeartRate : hasObservedMaxHeartRate // ignore: cast_nullable_to_non_nullable
as bool,hasHrvBaseline: null == hasHrvBaseline ? _self.hasHrvBaseline : hasHrvBaseline // ignore: cast_nullable_to_non_nullable
as bool,hasRespiratoryBaseline: null == hasRespiratoryBaseline ? _self.hasRespiratoryBaseline : hasRespiratoryBaseline // ignore: cast_nullable_to_non_nullable
as bool,previousEndScore: freezed == previousEndScore ? _self.previousEndScore : previousEndScore // ignore: cast_nullable_to_non_nullable
as int?,calibrationMode: null == calibrationMode ? _self.calibrationMode : calibrationMode // ignore: cast_nullable_to_non_nullable
as BodyEnergyCalibrationMode,
  ));
}


}

/// @nodoc
mixin _$BodyEnergyTimeline {

 LocalDate get date; int get startScore; int get currentScore; int get charged; int get drained; List<BodyEnergyTimelinePoint> get points; BodyEnergyConfidence get confidence; String get confidenceReason; BodyEnergyInputSummary get inputSummary; DateTime? get generatedAt; String get signature;
/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyEnergyTimelineCopyWith<BodyEnergyTimeline> get copyWith => _$BodyEnergyTimelineCopyWithImpl<BodyEnergyTimeline>(this as BodyEnergyTimeline, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyEnergyTimeline&&(identical(other.date, date) || other.date == date)&&(identical(other.startScore, startScore) || other.startScore == startScore)&&(identical(other.currentScore, currentScore) || other.currentScore == currentScore)&&(identical(other.charged, charged) || other.charged == charged)&&(identical(other.drained, drained) || other.drained == drained)&&const DeepCollectionEquality().equals(other.points, points)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.inputSummary, inputSummary) || other.inputSummary == inputSummary)&&(identical(other.generatedAt, generatedAt) || other.generatedAt == generatedAt)&&(identical(other.signature, signature) || other.signature == signature));
}


@override
int get hashCode => Object.hash(runtimeType,date,startScore,currentScore,charged,drained,const DeepCollectionEquality().hash(points),confidence,confidenceReason,inputSummary,generatedAt,signature);

@override
String toString() {
  return 'BodyEnergyTimeline(date: $date, startScore: $startScore, currentScore: $currentScore, charged: $charged, drained: $drained, points: $points, confidence: $confidence, confidenceReason: $confidenceReason, inputSummary: $inputSummary, generatedAt: $generatedAt, signature: $signature)';
}


}

/// @nodoc
abstract mixin class $BodyEnergyTimelineCopyWith<$Res>  {
  factory $BodyEnergyTimelineCopyWith(BodyEnergyTimeline value, $Res Function(BodyEnergyTimeline) _then) = _$BodyEnergyTimelineCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int startScore, int currentScore, int charged, int drained, List<BodyEnergyTimelinePoint> points, BodyEnergyConfidence confidence, String confidenceReason, BodyEnergyInputSummary inputSummary, DateTime? generatedAt, String signature
});


$BodyEnergyInputSummaryCopyWith<$Res> get inputSummary;

}
/// @nodoc
class _$BodyEnergyTimelineCopyWithImpl<$Res>
    implements $BodyEnergyTimelineCopyWith<$Res> {
  _$BodyEnergyTimelineCopyWithImpl(this._self, this._then);

  final BodyEnergyTimeline _self;
  final $Res Function(BodyEnergyTimeline) _then;

/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? startScore = null,Object? currentScore = null,Object? charged = null,Object? drained = null,Object? points = null,Object? confidence = null,Object? confidenceReason = null,Object? inputSummary = null,Object? generatedAt = freezed,Object? signature = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,startScore: null == startScore ? _self.startScore : startScore // ignore: cast_nullable_to_non_nullable
as int,currentScore: null == currentScore ? _self.currentScore : currentScore // ignore: cast_nullable_to_non_nullable
as int,charged: null == charged ? _self.charged : charged // ignore: cast_nullable_to_non_nullable
as int,drained: null == drained ? _self.drained : drained // ignore: cast_nullable_to_non_nullable
as int,points: null == points ? _self.points : points // ignore: cast_nullable_to_non_nullable
as List<BodyEnergyTimelinePoint>,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as BodyEnergyConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,inputSummary: null == inputSummary ? _self.inputSummary : inputSummary // ignore: cast_nullable_to_non_nullable
as BodyEnergyInputSummary,generatedAt: freezed == generatedAt ? _self.generatedAt : generatedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,signature: null == signature ? _self.signature : signature // ignore: cast_nullable_to_non_nullable
as String,
  ));
}
/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyEnergyInputSummaryCopyWith<$Res> get inputSummary {
  
  return $BodyEnergyInputSummaryCopyWith<$Res>(_self.inputSummary, (value) {
    return _then(_self.copyWith(inputSummary: value));
  });
}
}


/// Adds pattern-matching-related methods to [BodyEnergyTimeline].
extension BodyEnergyTimelinePatterns on BodyEnergyTimeline {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyEnergyTimeline value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyEnergyTimeline() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyEnergyTimeline value)  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyTimeline():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyEnergyTimeline value)?  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyTimeline() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int startScore,  int currentScore,  int charged,  int drained,  List<BodyEnergyTimelinePoint> points,  BodyEnergyConfidence confidence,  String confidenceReason,  BodyEnergyInputSummary inputSummary,  DateTime? generatedAt,  String signature)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyEnergyTimeline() when $default != null:
return $default(_that.date,_that.startScore,_that.currentScore,_that.charged,_that.drained,_that.points,_that.confidence,_that.confidenceReason,_that.inputSummary,_that.generatedAt,_that.signature);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int startScore,  int currentScore,  int charged,  int drained,  List<BodyEnergyTimelinePoint> points,  BodyEnergyConfidence confidence,  String confidenceReason,  BodyEnergyInputSummary inputSummary,  DateTime? generatedAt,  String signature)  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyTimeline():
return $default(_that.date,_that.startScore,_that.currentScore,_that.charged,_that.drained,_that.points,_that.confidence,_that.confidenceReason,_that.inputSummary,_that.generatedAt,_that.signature);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int startScore,  int currentScore,  int charged,  int drained,  List<BodyEnergyTimelinePoint> points,  BodyEnergyConfidence confidence,  String confidenceReason,  BodyEnergyInputSummary inputSummary,  DateTime? generatedAt,  String signature)?  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyTimeline() when $default != null:
return $default(_that.date,_that.startScore,_that.currentScore,_that.charged,_that.drained,_that.points,_that.confidence,_that.confidenceReason,_that.inputSummary,_that.generatedAt,_that.signature);case _:
  return null;

}
}

}

/// @nodoc


class _BodyEnergyTimeline implements BodyEnergyTimeline {
  const _BodyEnergyTimeline({required this.date, required this.startScore, required this.currentScore, required this.charged, required this.drained, required final  List<BodyEnergyTimelinePoint> points, required this.confidence, required this.confidenceReason, this.inputSummary = const BodyEnergyInputSummary(), this.generatedAt, this.signature = ''}): _points = points;
  

@override final  LocalDate date;
@override final  int startScore;
@override final  int currentScore;
@override final  int charged;
@override final  int drained;
 final  List<BodyEnergyTimelinePoint> _points;
@override List<BodyEnergyTimelinePoint> get points {
  if (_points is EqualUnmodifiableListView) return _points;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_points);
}

@override final  BodyEnergyConfidence confidence;
@override final  String confidenceReason;
@override@JsonKey() final  BodyEnergyInputSummary inputSummary;
@override final  DateTime? generatedAt;
@override@JsonKey() final  String signature;

/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyEnergyTimelineCopyWith<_BodyEnergyTimeline> get copyWith => __$BodyEnergyTimelineCopyWithImpl<_BodyEnergyTimeline>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyEnergyTimeline&&(identical(other.date, date) || other.date == date)&&(identical(other.startScore, startScore) || other.startScore == startScore)&&(identical(other.currentScore, currentScore) || other.currentScore == currentScore)&&(identical(other.charged, charged) || other.charged == charged)&&(identical(other.drained, drained) || other.drained == drained)&&const DeepCollectionEquality().equals(other._points, _points)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.inputSummary, inputSummary) || other.inputSummary == inputSummary)&&(identical(other.generatedAt, generatedAt) || other.generatedAt == generatedAt)&&(identical(other.signature, signature) || other.signature == signature));
}


@override
int get hashCode => Object.hash(runtimeType,date,startScore,currentScore,charged,drained,const DeepCollectionEquality().hash(_points),confidence,confidenceReason,inputSummary,generatedAt,signature);

@override
String toString() {
  return 'BodyEnergyTimeline(date: $date, startScore: $startScore, currentScore: $currentScore, charged: $charged, drained: $drained, points: $points, confidence: $confidence, confidenceReason: $confidenceReason, inputSummary: $inputSummary, generatedAt: $generatedAt, signature: $signature)';
}


}

/// @nodoc
abstract mixin class _$BodyEnergyTimelineCopyWith<$Res> implements $BodyEnergyTimelineCopyWith<$Res> {
  factory _$BodyEnergyTimelineCopyWith(_BodyEnergyTimeline value, $Res Function(_BodyEnergyTimeline) _then) = __$BodyEnergyTimelineCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int startScore, int currentScore, int charged, int drained, List<BodyEnergyTimelinePoint> points, BodyEnergyConfidence confidence, String confidenceReason, BodyEnergyInputSummary inputSummary, DateTime? generatedAt, String signature
});


@override $BodyEnergyInputSummaryCopyWith<$Res> get inputSummary;

}
/// @nodoc
class __$BodyEnergyTimelineCopyWithImpl<$Res>
    implements _$BodyEnergyTimelineCopyWith<$Res> {
  __$BodyEnergyTimelineCopyWithImpl(this._self, this._then);

  final _BodyEnergyTimeline _self;
  final $Res Function(_BodyEnergyTimeline) _then;

/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? startScore = null,Object? currentScore = null,Object? charged = null,Object? drained = null,Object? points = null,Object? confidence = null,Object? confidenceReason = null,Object? inputSummary = null,Object? generatedAt = freezed,Object? signature = null,}) {
  return _then(_BodyEnergyTimeline(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,startScore: null == startScore ? _self.startScore : startScore // ignore: cast_nullable_to_non_nullable
as int,currentScore: null == currentScore ? _self.currentScore : currentScore // ignore: cast_nullable_to_non_nullable
as int,charged: null == charged ? _self.charged : charged // ignore: cast_nullable_to_non_nullable
as int,drained: null == drained ? _self.drained : drained // ignore: cast_nullable_to_non_nullable
as int,points: null == points ? _self._points : points // ignore: cast_nullable_to_non_nullable
as List<BodyEnergyTimelinePoint>,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as BodyEnergyConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,inputSummary: null == inputSummary ? _self.inputSummary : inputSummary // ignore: cast_nullable_to_non_nullable
as BodyEnergyInputSummary,generatedAt: freezed == generatedAt ? _self.generatedAt : generatedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,signature: null == signature ? _self.signature : signature // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

/// Create a copy of BodyEnergyTimeline
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyEnergyInputSummaryCopyWith<$Res> get inputSummary {
  
  return $BodyEnergyInputSummaryCopyWith<$Res>(_self.inputSummary, (value) {
    return _then(_self.copyWith(inputSummary: value));
  });
}
}

// dart format on
