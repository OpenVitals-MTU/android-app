// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_recording.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartRateRecoveryTestConfig implements DiagnosticableTreeMixin {

/// 0 to skip the warmup and go straight to the effort.
 int get warmupSeconds;/// Ends the effort when the heart rate reaches this, if it is set. The user can
/// always end it by hand instead.
 int? get targetHeartRateBpm; int get recoverySeconds;
/// Create a copy of HeartRateRecoveryTestConfig
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateRecoveryTestConfigCopyWith<HeartRateRecoveryTestConfig> get copyWith => _$HeartRateRecoveryTestConfigCopyWithImpl<HeartRateRecoveryTestConfig>(this as HeartRateRecoveryTestConfig, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'HeartRateRecoveryTestConfig'))
    ..add(DiagnosticsProperty('warmupSeconds', warmupSeconds))..add(DiagnosticsProperty('targetHeartRateBpm', targetHeartRateBpm))..add(DiagnosticsProperty('recoverySeconds', recoverySeconds));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateRecoveryTestConfig&&(identical(other.warmupSeconds, warmupSeconds) || other.warmupSeconds == warmupSeconds)&&(identical(other.targetHeartRateBpm, targetHeartRateBpm) || other.targetHeartRateBpm == targetHeartRateBpm)&&(identical(other.recoverySeconds, recoverySeconds) || other.recoverySeconds == recoverySeconds));
}


@override
int get hashCode => Object.hash(runtimeType,warmupSeconds,targetHeartRateBpm,recoverySeconds);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'HeartRateRecoveryTestConfig(warmupSeconds: $warmupSeconds, targetHeartRateBpm: $targetHeartRateBpm, recoverySeconds: $recoverySeconds)';
}


}

/// @nodoc
abstract mixin class $HeartRateRecoveryTestConfigCopyWith<$Res>  {
  factory $HeartRateRecoveryTestConfigCopyWith(HeartRateRecoveryTestConfig value, $Res Function(HeartRateRecoveryTestConfig) _then) = _$HeartRateRecoveryTestConfigCopyWithImpl;
@useResult
$Res call({
 int warmupSeconds, int? targetHeartRateBpm, int recoverySeconds
});




}
/// @nodoc
class _$HeartRateRecoveryTestConfigCopyWithImpl<$Res>
    implements $HeartRateRecoveryTestConfigCopyWith<$Res> {
  _$HeartRateRecoveryTestConfigCopyWithImpl(this._self, this._then);

  final HeartRateRecoveryTestConfig _self;
  final $Res Function(HeartRateRecoveryTestConfig) _then;

/// Create a copy of HeartRateRecoveryTestConfig
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? warmupSeconds = null,Object? targetHeartRateBpm = freezed,Object? recoverySeconds = null,}) {
  return _then(_self.copyWith(
warmupSeconds: null == warmupSeconds ? _self.warmupSeconds : warmupSeconds // ignore: cast_nullable_to_non_nullable
as int,targetHeartRateBpm: freezed == targetHeartRateBpm ? _self.targetHeartRateBpm : targetHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,recoverySeconds: null == recoverySeconds ? _self.recoverySeconds : recoverySeconds // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateRecoveryTestConfig].
extension HeartRateRecoveryTestConfigPatterns on HeartRateRecoveryTestConfig {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateRecoveryTestConfig value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateRecoveryTestConfig value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateRecoveryTestConfig value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int warmupSeconds,  int? targetHeartRateBpm,  int recoverySeconds)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig() when $default != null:
return $default(_that.warmupSeconds,_that.targetHeartRateBpm,_that.recoverySeconds);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int warmupSeconds,  int? targetHeartRateBpm,  int recoverySeconds)  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig():
return $default(_that.warmupSeconds,_that.targetHeartRateBpm,_that.recoverySeconds);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int warmupSeconds,  int? targetHeartRateBpm,  int recoverySeconds)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryTestConfig() when $default != null:
return $default(_that.warmupSeconds,_that.targetHeartRateBpm,_that.recoverySeconds);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateRecoveryTestConfig with DiagnosticableTreeMixin implements HeartRateRecoveryTestConfig {
  const _HeartRateRecoveryTestConfig({this.warmupSeconds = 180, this.targetHeartRateBpm, this.recoverySeconds = 300});
  

/// 0 to skip the warmup and go straight to the effort.
@override@JsonKey() final  int warmupSeconds;
/// Ends the effort when the heart rate reaches this, if it is set. The user can
/// always end it by hand instead.
@override final  int? targetHeartRateBpm;
@override@JsonKey() final  int recoverySeconds;

/// Create a copy of HeartRateRecoveryTestConfig
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateRecoveryTestConfigCopyWith<_HeartRateRecoveryTestConfig> get copyWith => __$HeartRateRecoveryTestConfigCopyWithImpl<_HeartRateRecoveryTestConfig>(this, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'HeartRateRecoveryTestConfig'))
    ..add(DiagnosticsProperty('warmupSeconds', warmupSeconds))..add(DiagnosticsProperty('targetHeartRateBpm', targetHeartRateBpm))..add(DiagnosticsProperty('recoverySeconds', recoverySeconds));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateRecoveryTestConfig&&(identical(other.warmupSeconds, warmupSeconds) || other.warmupSeconds == warmupSeconds)&&(identical(other.targetHeartRateBpm, targetHeartRateBpm) || other.targetHeartRateBpm == targetHeartRateBpm)&&(identical(other.recoverySeconds, recoverySeconds) || other.recoverySeconds == recoverySeconds));
}


@override
int get hashCode => Object.hash(runtimeType,warmupSeconds,targetHeartRateBpm,recoverySeconds);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'HeartRateRecoveryTestConfig(warmupSeconds: $warmupSeconds, targetHeartRateBpm: $targetHeartRateBpm, recoverySeconds: $recoverySeconds)';
}


}

/// @nodoc
abstract mixin class _$HeartRateRecoveryTestConfigCopyWith<$Res> implements $HeartRateRecoveryTestConfigCopyWith<$Res> {
  factory _$HeartRateRecoveryTestConfigCopyWith(_HeartRateRecoveryTestConfig value, $Res Function(_HeartRateRecoveryTestConfig) _then) = __$HeartRateRecoveryTestConfigCopyWithImpl;
@override @useResult
$Res call({
 int warmupSeconds, int? targetHeartRateBpm, int recoverySeconds
});




}
/// @nodoc
class __$HeartRateRecoveryTestConfigCopyWithImpl<$Res>
    implements _$HeartRateRecoveryTestConfigCopyWith<$Res> {
  __$HeartRateRecoveryTestConfigCopyWithImpl(this._self, this._then);

  final _HeartRateRecoveryTestConfig _self;
  final $Res Function(_HeartRateRecoveryTestConfig) _then;

/// Create a copy of HeartRateRecoveryTestConfig
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? warmupSeconds = null,Object? targetHeartRateBpm = freezed,Object? recoverySeconds = null,}) {
  return _then(_HeartRateRecoveryTestConfig(
warmupSeconds: null == warmupSeconds ? _self.warmupSeconds : warmupSeconds // ignore: cast_nullable_to_non_nullable
as int,targetHeartRateBpm: freezed == targetHeartRateBpm ? _self.targetHeartRateBpm : targetHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,recoverySeconds: null == recoverySeconds ? _self.recoverySeconds : recoverySeconds // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$ActivityRecordedRepetitionSet implements DiagnosticableTreeMixin {

 int get repetitions; int get restSeconds; int get activeMillis;
/// Create a copy of ActivityRecordedRepetitionSet
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordedRepetitionSetCopyWith<ActivityRecordedRepetitionSet> get copyWith => _$ActivityRecordedRepetitionSetCopyWithImpl<ActivityRecordedRepetitionSet>(this as ActivityRecordedRepetitionSet, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordedRepetitionSet'))
    ..add(DiagnosticsProperty('repetitions', repetitions))..add(DiagnosticsProperty('restSeconds', restSeconds))..add(DiagnosticsProperty('activeMillis', activeMillis));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordedRepetitionSet&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.restSeconds, restSeconds) || other.restSeconds == restSeconds)&&(identical(other.activeMillis, activeMillis) || other.activeMillis == activeMillis));
}


@override
int get hashCode => Object.hash(runtimeType,repetitions,restSeconds,activeMillis);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordedRepetitionSet(repetitions: $repetitions, restSeconds: $restSeconds, activeMillis: $activeMillis)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordedRepetitionSetCopyWith<$Res>  {
  factory $ActivityRecordedRepetitionSetCopyWith(ActivityRecordedRepetitionSet value, $Res Function(ActivityRecordedRepetitionSet) _then) = _$ActivityRecordedRepetitionSetCopyWithImpl;
@useResult
$Res call({
 int repetitions, int restSeconds, int activeMillis
});




}
/// @nodoc
class _$ActivityRecordedRepetitionSetCopyWithImpl<$Res>
    implements $ActivityRecordedRepetitionSetCopyWith<$Res> {
  _$ActivityRecordedRepetitionSetCopyWithImpl(this._self, this._then);

  final ActivityRecordedRepetitionSet _self;
  final $Res Function(ActivityRecordedRepetitionSet) _then;

/// Create a copy of ActivityRecordedRepetitionSet
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? repetitions = null,Object? restSeconds = null,Object? activeMillis = null,}) {
  return _then(_self.copyWith(
repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,restSeconds: null == restSeconds ? _self.restSeconds : restSeconds // ignore: cast_nullable_to_non_nullable
as int,activeMillis: null == activeMillis ? _self.activeMillis : activeMillis // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityRecordedRepetitionSet].
extension ActivityRecordedRepetitionSetPatterns on ActivityRecordedRepetitionSet {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordedRepetitionSet value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordedRepetitionSet value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordedRepetitionSet value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int repetitions,  int restSeconds,  int activeMillis)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet() when $default != null:
return $default(_that.repetitions,_that.restSeconds,_that.activeMillis);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int repetitions,  int restSeconds,  int activeMillis)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet():
return $default(_that.repetitions,_that.restSeconds,_that.activeMillis);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int repetitions,  int restSeconds,  int activeMillis)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordedRepetitionSet() when $default != null:
return $default(_that.repetitions,_that.restSeconds,_that.activeMillis);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordedRepetitionSet with DiagnosticableTreeMixin implements ActivityRecordedRepetitionSet {
  const _ActivityRecordedRepetitionSet({required this.repetitions, required this.restSeconds, required this.activeMillis});
  

@override final  int repetitions;
@override final  int restSeconds;
@override final  int activeMillis;

/// Create a copy of ActivityRecordedRepetitionSet
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordedRepetitionSetCopyWith<_ActivityRecordedRepetitionSet> get copyWith => __$ActivityRecordedRepetitionSetCopyWithImpl<_ActivityRecordedRepetitionSet>(this, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordedRepetitionSet'))
    ..add(DiagnosticsProperty('repetitions', repetitions))..add(DiagnosticsProperty('restSeconds', restSeconds))..add(DiagnosticsProperty('activeMillis', activeMillis));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordedRepetitionSet&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.restSeconds, restSeconds) || other.restSeconds == restSeconds)&&(identical(other.activeMillis, activeMillis) || other.activeMillis == activeMillis));
}


@override
int get hashCode => Object.hash(runtimeType,repetitions,restSeconds,activeMillis);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordedRepetitionSet(repetitions: $repetitions, restSeconds: $restSeconds, activeMillis: $activeMillis)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordedRepetitionSetCopyWith<$Res> implements $ActivityRecordedRepetitionSetCopyWith<$Res> {
  factory _$ActivityRecordedRepetitionSetCopyWith(_ActivityRecordedRepetitionSet value, $Res Function(_ActivityRecordedRepetitionSet) _then) = __$ActivityRecordedRepetitionSetCopyWithImpl;
@override @useResult
$Res call({
 int repetitions, int restSeconds, int activeMillis
});




}
/// @nodoc
class __$ActivityRecordedRepetitionSetCopyWithImpl<$Res>
    implements _$ActivityRecordedRepetitionSetCopyWith<$Res> {
  __$ActivityRecordedRepetitionSetCopyWithImpl(this._self, this._then);

  final _ActivityRecordedRepetitionSet _self;
  final $Res Function(_ActivityRecordedRepetitionSet) _then;

/// Create a copy of ActivityRecordedRepetitionSet
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? repetitions = null,Object? restSeconds = null,Object? activeMillis = null,}) {
  return _then(_ActivityRecordedRepetitionSet(
repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,restSeconds: null == restSeconds ? _self.restSeconds : restSeconds // ignore: cast_nullable_to_non_nullable
as int,activeMillis: null == activeMillis ? _self.activeMillis : activeMillis // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$ActivityRecordingState implements DiagnosticableTreeMixin {

 ActivityRecordingStatus get status; ActivityRecordingKind get recordingKind; String? get activityTypeId; int? get exerciseType; DateTime? get startTime; DateTime? get endTime; DateTime? get pausedStartedAt; int get totalPausedMillis; List<ActivityPauseInterval> get pauseIntervals; List<ExerciseRoutePoint> get points; List<int> get routeBreakIndexes; List<ActivityRecordingLap> get manualLaps; List<ActivityRecordingMarker> get markers; ExerciseRoutePoint? get latestUiPoint; double get distanceMeters; double get elevationGainedMeters; double get elevationLostMeters; double get barometerElevationGainedMeters; double get barometerElevationLostMeters; bool get hasBarometerElevation; double? get lastBarometerAltitudeMeters; double get currentSpeedMetersPerSecond; double get maxSpeedMetersPerSecond; ActivityGpsStatus get gpsStatus; bool get keepScreenOnDuringRecording; bool get autoIdleEnabled; int get autoIdleTimeoutMillis; DateTime? get lastMovementAt; int get totalIdleMillis; int get repetitionCount; int get currentSetRepetitionCount; List<ActivityRecordedRepetitionSet> get repetitionSets; int get repetitionRestSeconds; DateTime? get currentSetStartedAt; DateTime? get restStartedAt; int get accumulatedRestMillis; double? get lastAccuracyMeters; DateTime? get lastLocationTime; int get droppedPointCount; String? get errorMessage; int? get currentHeartRateBpm; int? get currentCyclingCadenceRpm; double? get currentPowerWatts; double? get currentSensorSpeedMetersPerSecond; int? get currentRunningCadenceRpm; bool get bleHeartRateNoSignal; List<BleDeviceConnectionStatus> get bleDeviceStatuses; ActivityRecordingDashboardLayout get dashboardLayout; ActivityRecordingHrrPhase get hrrPhase; HeartRateRecoveryTestConfig get hrrConfig;/// The instant the effort stopped — the one the whole measurement hangs on.
///
/// Deliberately NOT persisted to the draft store (see
/// `activity_recording_serialization.dart`). The heart-rate samples live in memory,
/// in the BLE coordinator, and do not survive the process being killed; a restored
/// recording therefore comes back as an ordinary one, with no phase and no recovery
/// mark. That loses the test, which is a pity — but the alternative is saving a
/// session that carries a rest segment claiming a recovery, with no heart rate behind
/// it to measure. A lost measurement is recoverable. A fabricated one is not.
 DateTime? get hrrEffortEndedAt;
/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingStateCopyWith<ActivityRecordingState> get copyWith => _$ActivityRecordingStateCopyWithImpl<ActivityRecordingState>(this as ActivityRecordingState, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordingState'))
    ..add(DiagnosticsProperty('status', status))..add(DiagnosticsProperty('recordingKind', recordingKind))..add(DiagnosticsProperty('activityTypeId', activityTypeId))..add(DiagnosticsProperty('exerciseType', exerciseType))..add(DiagnosticsProperty('startTime', startTime))..add(DiagnosticsProperty('endTime', endTime))..add(DiagnosticsProperty('pausedStartedAt', pausedStartedAt))..add(DiagnosticsProperty('totalPausedMillis', totalPausedMillis))..add(DiagnosticsProperty('pauseIntervals', pauseIntervals))..add(DiagnosticsProperty('points', points))..add(DiagnosticsProperty('routeBreakIndexes', routeBreakIndexes))..add(DiagnosticsProperty('manualLaps', manualLaps))..add(DiagnosticsProperty('markers', markers))..add(DiagnosticsProperty('latestUiPoint', latestUiPoint))..add(DiagnosticsProperty('distanceMeters', distanceMeters))..add(DiagnosticsProperty('elevationGainedMeters', elevationGainedMeters))..add(DiagnosticsProperty('elevationLostMeters', elevationLostMeters))..add(DiagnosticsProperty('barometerElevationGainedMeters', barometerElevationGainedMeters))..add(DiagnosticsProperty('barometerElevationLostMeters', barometerElevationLostMeters))..add(DiagnosticsProperty('hasBarometerElevation', hasBarometerElevation))..add(DiagnosticsProperty('lastBarometerAltitudeMeters', lastBarometerAltitudeMeters))..add(DiagnosticsProperty('currentSpeedMetersPerSecond', currentSpeedMetersPerSecond))..add(DiagnosticsProperty('maxSpeedMetersPerSecond', maxSpeedMetersPerSecond))..add(DiagnosticsProperty('gpsStatus', gpsStatus))..add(DiagnosticsProperty('keepScreenOnDuringRecording', keepScreenOnDuringRecording))..add(DiagnosticsProperty('autoIdleEnabled', autoIdleEnabled))..add(DiagnosticsProperty('autoIdleTimeoutMillis', autoIdleTimeoutMillis))..add(DiagnosticsProperty('lastMovementAt', lastMovementAt))..add(DiagnosticsProperty('totalIdleMillis', totalIdleMillis))..add(DiagnosticsProperty('repetitionCount', repetitionCount))..add(DiagnosticsProperty('currentSetRepetitionCount', currentSetRepetitionCount))..add(DiagnosticsProperty('repetitionSets', repetitionSets))..add(DiagnosticsProperty('repetitionRestSeconds', repetitionRestSeconds))..add(DiagnosticsProperty('currentSetStartedAt', currentSetStartedAt))..add(DiagnosticsProperty('restStartedAt', restStartedAt))..add(DiagnosticsProperty('accumulatedRestMillis', accumulatedRestMillis))..add(DiagnosticsProperty('lastAccuracyMeters', lastAccuracyMeters))..add(DiagnosticsProperty('lastLocationTime', lastLocationTime))..add(DiagnosticsProperty('droppedPointCount', droppedPointCount))..add(DiagnosticsProperty('errorMessage', errorMessage))..add(DiagnosticsProperty('currentHeartRateBpm', currentHeartRateBpm))..add(DiagnosticsProperty('currentCyclingCadenceRpm', currentCyclingCadenceRpm))..add(DiagnosticsProperty('currentPowerWatts', currentPowerWatts))..add(DiagnosticsProperty('currentSensorSpeedMetersPerSecond', currentSensorSpeedMetersPerSecond))..add(DiagnosticsProperty('currentRunningCadenceRpm', currentRunningCadenceRpm))..add(DiagnosticsProperty('bleHeartRateNoSignal', bleHeartRateNoSignal))..add(DiagnosticsProperty('bleDeviceStatuses', bleDeviceStatuses))..add(DiagnosticsProperty('dashboardLayout', dashboardLayout))..add(DiagnosticsProperty('hrrPhase', hrrPhase))..add(DiagnosticsProperty('hrrConfig', hrrConfig))..add(DiagnosticsProperty('hrrEffortEndedAt', hrrEffortEndedAt));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingState&&(identical(other.status, status) || other.status == status)&&(identical(other.recordingKind, recordingKind) || other.recordingKind == recordingKind)&&(identical(other.activityTypeId, activityTypeId) || other.activityTypeId == activityTypeId)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.pausedStartedAt, pausedStartedAt) || other.pausedStartedAt == pausedStartedAt)&&(identical(other.totalPausedMillis, totalPausedMillis) || other.totalPausedMillis == totalPausedMillis)&&const DeepCollectionEquality().equals(other.pauseIntervals, pauseIntervals)&&const DeepCollectionEquality().equals(other.points, points)&&const DeepCollectionEquality().equals(other.routeBreakIndexes, routeBreakIndexes)&&const DeepCollectionEquality().equals(other.manualLaps, manualLaps)&&const DeepCollectionEquality().equals(other.markers, markers)&&(identical(other.latestUiPoint, latestUiPoint) || other.latestUiPoint == latestUiPoint)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.elevationLostMeters, elevationLostMeters) || other.elevationLostMeters == elevationLostMeters)&&(identical(other.barometerElevationGainedMeters, barometerElevationGainedMeters) || other.barometerElevationGainedMeters == barometerElevationGainedMeters)&&(identical(other.barometerElevationLostMeters, barometerElevationLostMeters) || other.barometerElevationLostMeters == barometerElevationLostMeters)&&(identical(other.hasBarometerElevation, hasBarometerElevation) || other.hasBarometerElevation == hasBarometerElevation)&&(identical(other.lastBarometerAltitudeMeters, lastBarometerAltitudeMeters) || other.lastBarometerAltitudeMeters == lastBarometerAltitudeMeters)&&(identical(other.currentSpeedMetersPerSecond, currentSpeedMetersPerSecond) || other.currentSpeedMetersPerSecond == currentSpeedMetersPerSecond)&&(identical(other.maxSpeedMetersPerSecond, maxSpeedMetersPerSecond) || other.maxSpeedMetersPerSecond == maxSpeedMetersPerSecond)&&(identical(other.gpsStatus, gpsStatus) || other.gpsStatus == gpsStatus)&&(identical(other.keepScreenOnDuringRecording, keepScreenOnDuringRecording) || other.keepScreenOnDuringRecording == keepScreenOnDuringRecording)&&(identical(other.autoIdleEnabled, autoIdleEnabled) || other.autoIdleEnabled == autoIdleEnabled)&&(identical(other.autoIdleTimeoutMillis, autoIdleTimeoutMillis) || other.autoIdleTimeoutMillis == autoIdleTimeoutMillis)&&(identical(other.lastMovementAt, lastMovementAt) || other.lastMovementAt == lastMovementAt)&&(identical(other.totalIdleMillis, totalIdleMillis) || other.totalIdleMillis == totalIdleMillis)&&(identical(other.repetitionCount, repetitionCount) || other.repetitionCount == repetitionCount)&&(identical(other.currentSetRepetitionCount, currentSetRepetitionCount) || other.currentSetRepetitionCount == currentSetRepetitionCount)&&const DeepCollectionEquality().equals(other.repetitionSets, repetitionSets)&&(identical(other.repetitionRestSeconds, repetitionRestSeconds) || other.repetitionRestSeconds == repetitionRestSeconds)&&(identical(other.currentSetStartedAt, currentSetStartedAt) || other.currentSetStartedAt == currentSetStartedAt)&&(identical(other.restStartedAt, restStartedAt) || other.restStartedAt == restStartedAt)&&(identical(other.accumulatedRestMillis, accumulatedRestMillis) || other.accumulatedRestMillis == accumulatedRestMillis)&&(identical(other.lastAccuracyMeters, lastAccuracyMeters) || other.lastAccuracyMeters == lastAccuracyMeters)&&(identical(other.lastLocationTime, lastLocationTime) || other.lastLocationTime == lastLocationTime)&&(identical(other.droppedPointCount, droppedPointCount) || other.droppedPointCount == droppedPointCount)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage)&&(identical(other.currentHeartRateBpm, currentHeartRateBpm) || other.currentHeartRateBpm == currentHeartRateBpm)&&(identical(other.currentCyclingCadenceRpm, currentCyclingCadenceRpm) || other.currentCyclingCadenceRpm == currentCyclingCadenceRpm)&&(identical(other.currentPowerWatts, currentPowerWatts) || other.currentPowerWatts == currentPowerWatts)&&(identical(other.currentSensorSpeedMetersPerSecond, currentSensorSpeedMetersPerSecond) || other.currentSensorSpeedMetersPerSecond == currentSensorSpeedMetersPerSecond)&&(identical(other.currentRunningCadenceRpm, currentRunningCadenceRpm) || other.currentRunningCadenceRpm == currentRunningCadenceRpm)&&(identical(other.bleHeartRateNoSignal, bleHeartRateNoSignal) || other.bleHeartRateNoSignal == bleHeartRateNoSignal)&&const DeepCollectionEquality().equals(other.bleDeviceStatuses, bleDeviceStatuses)&&(identical(other.dashboardLayout, dashboardLayout) || other.dashboardLayout == dashboardLayout)&&(identical(other.hrrPhase, hrrPhase) || other.hrrPhase == hrrPhase)&&(identical(other.hrrConfig, hrrConfig) || other.hrrConfig == hrrConfig)&&(identical(other.hrrEffortEndedAt, hrrEffortEndedAt) || other.hrrEffortEndedAt == hrrEffortEndedAt));
}


@override
int get hashCode => Object.hashAll([runtimeType,status,recordingKind,activityTypeId,exerciseType,startTime,endTime,pausedStartedAt,totalPausedMillis,const DeepCollectionEquality().hash(pauseIntervals),const DeepCollectionEquality().hash(points),const DeepCollectionEquality().hash(routeBreakIndexes),const DeepCollectionEquality().hash(manualLaps),const DeepCollectionEquality().hash(markers),latestUiPoint,distanceMeters,elevationGainedMeters,elevationLostMeters,barometerElevationGainedMeters,barometerElevationLostMeters,hasBarometerElevation,lastBarometerAltitudeMeters,currentSpeedMetersPerSecond,maxSpeedMetersPerSecond,gpsStatus,keepScreenOnDuringRecording,autoIdleEnabled,autoIdleTimeoutMillis,lastMovementAt,totalIdleMillis,repetitionCount,currentSetRepetitionCount,const DeepCollectionEquality().hash(repetitionSets),repetitionRestSeconds,currentSetStartedAt,restStartedAt,accumulatedRestMillis,lastAccuracyMeters,lastLocationTime,droppedPointCount,errorMessage,currentHeartRateBpm,currentCyclingCadenceRpm,currentPowerWatts,currentSensorSpeedMetersPerSecond,currentRunningCadenceRpm,bleHeartRateNoSignal,const DeepCollectionEquality().hash(bleDeviceStatuses),dashboardLayout,hrrPhase,hrrConfig,hrrEffortEndedAt]);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordingState(status: $status, recordingKind: $recordingKind, activityTypeId: $activityTypeId, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, pausedStartedAt: $pausedStartedAt, totalPausedMillis: $totalPausedMillis, pauseIntervals: $pauseIntervals, points: $points, routeBreakIndexes: $routeBreakIndexes, manualLaps: $manualLaps, markers: $markers, latestUiPoint: $latestUiPoint, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, elevationLostMeters: $elevationLostMeters, barometerElevationGainedMeters: $barometerElevationGainedMeters, barometerElevationLostMeters: $barometerElevationLostMeters, hasBarometerElevation: $hasBarometerElevation, lastBarometerAltitudeMeters: $lastBarometerAltitudeMeters, currentSpeedMetersPerSecond: $currentSpeedMetersPerSecond, maxSpeedMetersPerSecond: $maxSpeedMetersPerSecond, gpsStatus: $gpsStatus, keepScreenOnDuringRecording: $keepScreenOnDuringRecording, autoIdleEnabled: $autoIdleEnabled, autoIdleTimeoutMillis: $autoIdleTimeoutMillis, lastMovementAt: $lastMovementAt, totalIdleMillis: $totalIdleMillis, repetitionCount: $repetitionCount, currentSetRepetitionCount: $currentSetRepetitionCount, repetitionSets: $repetitionSets, repetitionRestSeconds: $repetitionRestSeconds, currentSetStartedAt: $currentSetStartedAt, restStartedAt: $restStartedAt, accumulatedRestMillis: $accumulatedRestMillis, lastAccuracyMeters: $lastAccuracyMeters, lastLocationTime: $lastLocationTime, droppedPointCount: $droppedPointCount, errorMessage: $errorMessage, currentHeartRateBpm: $currentHeartRateBpm, currentCyclingCadenceRpm: $currentCyclingCadenceRpm, currentPowerWatts: $currentPowerWatts, currentSensorSpeedMetersPerSecond: $currentSensorSpeedMetersPerSecond, currentRunningCadenceRpm: $currentRunningCadenceRpm, bleHeartRateNoSignal: $bleHeartRateNoSignal, bleDeviceStatuses: $bleDeviceStatuses, dashboardLayout: $dashboardLayout, hrrPhase: $hrrPhase, hrrConfig: $hrrConfig, hrrEffortEndedAt: $hrrEffortEndedAt)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingStateCopyWith<$Res>  {
  factory $ActivityRecordingStateCopyWith(ActivityRecordingState value, $Res Function(ActivityRecordingState) _then) = _$ActivityRecordingStateCopyWithImpl;
@useResult
$Res call({
 ActivityRecordingStatus status, ActivityRecordingKind recordingKind, String? activityTypeId, int? exerciseType, DateTime? startTime, DateTime? endTime, DateTime? pausedStartedAt, int totalPausedMillis, List<ActivityPauseInterval> pauseIntervals, List<ExerciseRoutePoint> points, List<int> routeBreakIndexes, List<ActivityRecordingLap> manualLaps, List<ActivityRecordingMarker> markers, ExerciseRoutePoint? latestUiPoint, double distanceMeters, double elevationGainedMeters, double elevationLostMeters, double barometerElevationGainedMeters, double barometerElevationLostMeters, bool hasBarometerElevation, double? lastBarometerAltitudeMeters, double currentSpeedMetersPerSecond, double maxSpeedMetersPerSecond, ActivityGpsStatus gpsStatus, bool keepScreenOnDuringRecording, bool autoIdleEnabled, int autoIdleTimeoutMillis, DateTime? lastMovementAt, int totalIdleMillis, int repetitionCount, int currentSetRepetitionCount, List<ActivityRecordedRepetitionSet> repetitionSets, int repetitionRestSeconds, DateTime? currentSetStartedAt, DateTime? restStartedAt, int accumulatedRestMillis, double? lastAccuracyMeters, DateTime? lastLocationTime, int droppedPointCount, String? errorMessage, int? currentHeartRateBpm, int? currentCyclingCadenceRpm, double? currentPowerWatts, double? currentSensorSpeedMetersPerSecond, int? currentRunningCadenceRpm, bool bleHeartRateNoSignal, List<BleDeviceConnectionStatus> bleDeviceStatuses, ActivityRecordingDashboardLayout dashboardLayout, ActivityRecordingHrrPhase hrrPhase, HeartRateRecoveryTestConfig hrrConfig, DateTime? hrrEffortEndedAt
});


$ExerciseRoutePointCopyWith<$Res>? get latestUiPoint;$HeartRateRecoveryTestConfigCopyWith<$Res> get hrrConfig;

}
/// @nodoc
class _$ActivityRecordingStateCopyWithImpl<$Res>
    implements $ActivityRecordingStateCopyWith<$Res> {
  _$ActivityRecordingStateCopyWithImpl(this._self, this._then);

  final ActivityRecordingState _self;
  final $Res Function(ActivityRecordingState) _then;

/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? recordingKind = null,Object? activityTypeId = freezed,Object? exerciseType = freezed,Object? startTime = freezed,Object? endTime = freezed,Object? pausedStartedAt = freezed,Object? totalPausedMillis = null,Object? pauseIntervals = null,Object? points = null,Object? routeBreakIndexes = null,Object? manualLaps = null,Object? markers = null,Object? latestUiPoint = freezed,Object? distanceMeters = null,Object? elevationGainedMeters = null,Object? elevationLostMeters = null,Object? barometerElevationGainedMeters = null,Object? barometerElevationLostMeters = null,Object? hasBarometerElevation = null,Object? lastBarometerAltitudeMeters = freezed,Object? currentSpeedMetersPerSecond = null,Object? maxSpeedMetersPerSecond = null,Object? gpsStatus = null,Object? keepScreenOnDuringRecording = null,Object? autoIdleEnabled = null,Object? autoIdleTimeoutMillis = null,Object? lastMovementAt = freezed,Object? totalIdleMillis = null,Object? repetitionCount = null,Object? currentSetRepetitionCount = null,Object? repetitionSets = null,Object? repetitionRestSeconds = null,Object? currentSetStartedAt = freezed,Object? restStartedAt = freezed,Object? accumulatedRestMillis = null,Object? lastAccuracyMeters = freezed,Object? lastLocationTime = freezed,Object? droppedPointCount = null,Object? errorMessage = freezed,Object? currentHeartRateBpm = freezed,Object? currentCyclingCadenceRpm = freezed,Object? currentPowerWatts = freezed,Object? currentSensorSpeedMetersPerSecond = freezed,Object? currentRunningCadenceRpm = freezed,Object? bleHeartRateNoSignal = null,Object? bleDeviceStatuses = null,Object? dashboardLayout = null,Object? hrrPhase = null,Object? hrrConfig = null,Object? hrrEffortEndedAt = freezed,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ActivityRecordingStatus,recordingKind: null == recordingKind ? _self.recordingKind : recordingKind // ignore: cast_nullable_to_non_nullable
as ActivityRecordingKind,activityTypeId: freezed == activityTypeId ? _self.activityTypeId : activityTypeId // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: freezed == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int?,startTime: freezed == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime?,endTime: freezed == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime?,pausedStartedAt: freezed == pausedStartedAt ? _self.pausedStartedAt : pausedStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,totalPausedMillis: null == totalPausedMillis ? _self.totalPausedMillis : totalPausedMillis // ignore: cast_nullable_to_non_nullable
as int,pauseIntervals: null == pauseIntervals ? _self.pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,points: null == points ? _self.points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,routeBreakIndexes: null == routeBreakIndexes ? _self.routeBreakIndexes : routeBreakIndexes // ignore: cast_nullable_to_non_nullable
as List<int>,manualLaps: null == manualLaps ? _self.manualLaps : manualLaps // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingLap>,markers: null == markers ? _self.markers : markers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,latestUiPoint: freezed == latestUiPoint ? _self.latestUiPoint : latestUiPoint // ignore: cast_nullable_to_non_nullable
as ExerciseRoutePoint?,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationGainedMeters: null == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,elevationLostMeters: null == elevationLostMeters ? _self.elevationLostMeters : elevationLostMeters // ignore: cast_nullable_to_non_nullable
as double,barometerElevationGainedMeters: null == barometerElevationGainedMeters ? _self.barometerElevationGainedMeters : barometerElevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,barometerElevationLostMeters: null == barometerElevationLostMeters ? _self.barometerElevationLostMeters : barometerElevationLostMeters // ignore: cast_nullable_to_non_nullable
as double,hasBarometerElevation: null == hasBarometerElevation ? _self.hasBarometerElevation : hasBarometerElevation // ignore: cast_nullable_to_non_nullable
as bool,lastBarometerAltitudeMeters: freezed == lastBarometerAltitudeMeters ? _self.lastBarometerAltitudeMeters : lastBarometerAltitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,currentSpeedMetersPerSecond: null == currentSpeedMetersPerSecond ? _self.currentSpeedMetersPerSecond : currentSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,maxSpeedMetersPerSecond: null == maxSpeedMetersPerSecond ? _self.maxSpeedMetersPerSecond : maxSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,gpsStatus: null == gpsStatus ? _self.gpsStatus : gpsStatus // ignore: cast_nullable_to_non_nullable
as ActivityGpsStatus,keepScreenOnDuringRecording: null == keepScreenOnDuringRecording ? _self.keepScreenOnDuringRecording : keepScreenOnDuringRecording // ignore: cast_nullable_to_non_nullable
as bool,autoIdleEnabled: null == autoIdleEnabled ? _self.autoIdleEnabled : autoIdleEnabled // ignore: cast_nullable_to_non_nullable
as bool,autoIdleTimeoutMillis: null == autoIdleTimeoutMillis ? _self.autoIdleTimeoutMillis : autoIdleTimeoutMillis // ignore: cast_nullable_to_non_nullable
as int,lastMovementAt: freezed == lastMovementAt ? _self.lastMovementAt : lastMovementAt // ignore: cast_nullable_to_non_nullable
as DateTime?,totalIdleMillis: null == totalIdleMillis ? _self.totalIdleMillis : totalIdleMillis // ignore: cast_nullable_to_non_nullable
as int,repetitionCount: null == repetitionCount ? _self.repetitionCount : repetitionCount // ignore: cast_nullable_to_non_nullable
as int,currentSetRepetitionCount: null == currentSetRepetitionCount ? _self.currentSetRepetitionCount : currentSetRepetitionCount // ignore: cast_nullable_to_non_nullable
as int,repetitionSets: null == repetitionSets ? _self.repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordedRepetitionSet>,repetitionRestSeconds: null == repetitionRestSeconds ? _self.repetitionRestSeconds : repetitionRestSeconds // ignore: cast_nullable_to_non_nullable
as int,currentSetStartedAt: freezed == currentSetStartedAt ? _self.currentSetStartedAt : currentSetStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,restStartedAt: freezed == restStartedAt ? _self.restStartedAt : restStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,accumulatedRestMillis: null == accumulatedRestMillis ? _self.accumulatedRestMillis : accumulatedRestMillis // ignore: cast_nullable_to_non_nullable
as int,lastAccuracyMeters: freezed == lastAccuracyMeters ? _self.lastAccuracyMeters : lastAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,lastLocationTime: freezed == lastLocationTime ? _self.lastLocationTime : lastLocationTime // ignore: cast_nullable_to_non_nullable
as DateTime?,droppedPointCount: null == droppedPointCount ? _self.droppedPointCount : droppedPointCount // ignore: cast_nullable_to_non_nullable
as int,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,currentHeartRateBpm: freezed == currentHeartRateBpm ? _self.currentHeartRateBpm : currentHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,currentCyclingCadenceRpm: freezed == currentCyclingCadenceRpm ? _self.currentCyclingCadenceRpm : currentCyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,currentPowerWatts: freezed == currentPowerWatts ? _self.currentPowerWatts : currentPowerWatts // ignore: cast_nullable_to_non_nullable
as double?,currentSensorSpeedMetersPerSecond: freezed == currentSensorSpeedMetersPerSecond ? _self.currentSensorSpeedMetersPerSecond : currentSensorSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,currentRunningCadenceRpm: freezed == currentRunningCadenceRpm ? _self.currentRunningCadenceRpm : currentRunningCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,bleHeartRateNoSignal: null == bleHeartRateNoSignal ? _self.bleHeartRateNoSignal : bleHeartRateNoSignal // ignore: cast_nullable_to_non_nullable
as bool,bleDeviceStatuses: null == bleDeviceStatuses ? _self.bleDeviceStatuses : bleDeviceStatuses // ignore: cast_nullable_to_non_nullable
as List<BleDeviceConnectionStatus>,dashboardLayout: null == dashboardLayout ? _self.dashboardLayout : dashboardLayout // ignore: cast_nullable_to_non_nullable
as ActivityRecordingDashboardLayout,hrrPhase: null == hrrPhase ? _self.hrrPhase : hrrPhase // ignore: cast_nullable_to_non_nullable
as ActivityRecordingHrrPhase,hrrConfig: null == hrrConfig ? _self.hrrConfig : hrrConfig // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryTestConfig,hrrEffortEndedAt: freezed == hrrEffortEndedAt ? _self.hrrEffortEndedAt : hrrEffortEndedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}
/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseRoutePointCopyWith<$Res>? get latestUiPoint {
    if (_self.latestUiPoint == null) {
    return null;
  }

  return $ExerciseRoutePointCopyWith<$Res>(_self.latestUiPoint!, (value) {
    return _then(_self.copyWith(latestUiPoint: value));
  });
}/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartRateRecoveryTestConfigCopyWith<$Res> get hrrConfig {
  
  return $HeartRateRecoveryTestConfigCopyWith<$Res>(_self.hrrConfig, (value) {
    return _then(_self.copyWith(hrrConfig: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityRecordingState].
extension ActivityRecordingStatePatterns on ActivityRecordingState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingState value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ActivityRecordingStatus status,  ActivityRecordingKind recordingKind,  String? activityTypeId,  int? exerciseType,  DateTime? startTime,  DateTime? endTime,  DateTime? pausedStartedAt,  int totalPausedMillis,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseRoutePoint> points,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  ExerciseRoutePoint? latestUiPoint,  double distanceMeters,  double elevationGainedMeters,  double elevationLostMeters,  double barometerElevationGainedMeters,  double barometerElevationLostMeters,  bool hasBarometerElevation,  double? lastBarometerAltitudeMeters,  double currentSpeedMetersPerSecond,  double maxSpeedMetersPerSecond,  ActivityGpsStatus gpsStatus,  bool keepScreenOnDuringRecording,  bool autoIdleEnabled,  int autoIdleTimeoutMillis,  DateTime? lastMovementAt,  int totalIdleMillis,  int repetitionCount,  int currentSetRepetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  int repetitionRestSeconds,  DateTime? currentSetStartedAt,  DateTime? restStartedAt,  int accumulatedRestMillis,  double? lastAccuracyMeters,  DateTime? lastLocationTime,  int droppedPointCount,  String? errorMessage,  int? currentHeartRateBpm,  int? currentCyclingCadenceRpm,  double? currentPowerWatts,  double? currentSensorSpeedMetersPerSecond,  int? currentRunningCadenceRpm,  bool bleHeartRateNoSignal,  List<BleDeviceConnectionStatus> bleDeviceStatuses,  ActivityRecordingDashboardLayout dashboardLayout,  ActivityRecordingHrrPhase hrrPhase,  HeartRateRecoveryTestConfig hrrConfig,  DateTime? hrrEffortEndedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingState() when $default != null:
return $default(_that.status,_that.recordingKind,_that.activityTypeId,_that.exerciseType,_that.startTime,_that.endTime,_that.pausedStartedAt,_that.totalPausedMillis,_that.pauseIntervals,_that.points,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.latestUiPoint,_that.distanceMeters,_that.elevationGainedMeters,_that.elevationLostMeters,_that.barometerElevationGainedMeters,_that.barometerElevationLostMeters,_that.hasBarometerElevation,_that.lastBarometerAltitudeMeters,_that.currentSpeedMetersPerSecond,_that.maxSpeedMetersPerSecond,_that.gpsStatus,_that.keepScreenOnDuringRecording,_that.autoIdleEnabled,_that.autoIdleTimeoutMillis,_that.lastMovementAt,_that.totalIdleMillis,_that.repetitionCount,_that.currentSetRepetitionCount,_that.repetitionSets,_that.repetitionRestSeconds,_that.currentSetStartedAt,_that.restStartedAt,_that.accumulatedRestMillis,_that.lastAccuracyMeters,_that.lastLocationTime,_that.droppedPointCount,_that.errorMessage,_that.currentHeartRateBpm,_that.currentCyclingCadenceRpm,_that.currentPowerWatts,_that.currentSensorSpeedMetersPerSecond,_that.currentRunningCadenceRpm,_that.bleHeartRateNoSignal,_that.bleDeviceStatuses,_that.dashboardLayout,_that.hrrPhase,_that.hrrConfig,_that.hrrEffortEndedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ActivityRecordingStatus status,  ActivityRecordingKind recordingKind,  String? activityTypeId,  int? exerciseType,  DateTime? startTime,  DateTime? endTime,  DateTime? pausedStartedAt,  int totalPausedMillis,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseRoutePoint> points,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  ExerciseRoutePoint? latestUiPoint,  double distanceMeters,  double elevationGainedMeters,  double elevationLostMeters,  double barometerElevationGainedMeters,  double barometerElevationLostMeters,  bool hasBarometerElevation,  double? lastBarometerAltitudeMeters,  double currentSpeedMetersPerSecond,  double maxSpeedMetersPerSecond,  ActivityGpsStatus gpsStatus,  bool keepScreenOnDuringRecording,  bool autoIdleEnabled,  int autoIdleTimeoutMillis,  DateTime? lastMovementAt,  int totalIdleMillis,  int repetitionCount,  int currentSetRepetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  int repetitionRestSeconds,  DateTime? currentSetStartedAt,  DateTime? restStartedAt,  int accumulatedRestMillis,  double? lastAccuracyMeters,  DateTime? lastLocationTime,  int droppedPointCount,  String? errorMessage,  int? currentHeartRateBpm,  int? currentCyclingCadenceRpm,  double? currentPowerWatts,  double? currentSensorSpeedMetersPerSecond,  int? currentRunningCadenceRpm,  bool bleHeartRateNoSignal,  List<BleDeviceConnectionStatus> bleDeviceStatuses,  ActivityRecordingDashboardLayout dashboardLayout,  ActivityRecordingHrrPhase hrrPhase,  HeartRateRecoveryTestConfig hrrConfig,  DateTime? hrrEffortEndedAt)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingState():
return $default(_that.status,_that.recordingKind,_that.activityTypeId,_that.exerciseType,_that.startTime,_that.endTime,_that.pausedStartedAt,_that.totalPausedMillis,_that.pauseIntervals,_that.points,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.latestUiPoint,_that.distanceMeters,_that.elevationGainedMeters,_that.elevationLostMeters,_that.barometerElevationGainedMeters,_that.barometerElevationLostMeters,_that.hasBarometerElevation,_that.lastBarometerAltitudeMeters,_that.currentSpeedMetersPerSecond,_that.maxSpeedMetersPerSecond,_that.gpsStatus,_that.keepScreenOnDuringRecording,_that.autoIdleEnabled,_that.autoIdleTimeoutMillis,_that.lastMovementAt,_that.totalIdleMillis,_that.repetitionCount,_that.currentSetRepetitionCount,_that.repetitionSets,_that.repetitionRestSeconds,_that.currentSetStartedAt,_that.restStartedAt,_that.accumulatedRestMillis,_that.lastAccuracyMeters,_that.lastLocationTime,_that.droppedPointCount,_that.errorMessage,_that.currentHeartRateBpm,_that.currentCyclingCadenceRpm,_that.currentPowerWatts,_that.currentSensorSpeedMetersPerSecond,_that.currentRunningCadenceRpm,_that.bleHeartRateNoSignal,_that.bleDeviceStatuses,_that.dashboardLayout,_that.hrrPhase,_that.hrrConfig,_that.hrrEffortEndedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ActivityRecordingStatus status,  ActivityRecordingKind recordingKind,  String? activityTypeId,  int? exerciseType,  DateTime? startTime,  DateTime? endTime,  DateTime? pausedStartedAt,  int totalPausedMillis,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseRoutePoint> points,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  ExerciseRoutePoint? latestUiPoint,  double distanceMeters,  double elevationGainedMeters,  double elevationLostMeters,  double barometerElevationGainedMeters,  double barometerElevationLostMeters,  bool hasBarometerElevation,  double? lastBarometerAltitudeMeters,  double currentSpeedMetersPerSecond,  double maxSpeedMetersPerSecond,  ActivityGpsStatus gpsStatus,  bool keepScreenOnDuringRecording,  bool autoIdleEnabled,  int autoIdleTimeoutMillis,  DateTime? lastMovementAt,  int totalIdleMillis,  int repetitionCount,  int currentSetRepetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  int repetitionRestSeconds,  DateTime? currentSetStartedAt,  DateTime? restStartedAt,  int accumulatedRestMillis,  double? lastAccuracyMeters,  DateTime? lastLocationTime,  int droppedPointCount,  String? errorMessage,  int? currentHeartRateBpm,  int? currentCyclingCadenceRpm,  double? currentPowerWatts,  double? currentSensorSpeedMetersPerSecond,  int? currentRunningCadenceRpm,  bool bleHeartRateNoSignal,  List<BleDeviceConnectionStatus> bleDeviceStatuses,  ActivityRecordingDashboardLayout dashboardLayout,  ActivityRecordingHrrPhase hrrPhase,  HeartRateRecoveryTestConfig hrrConfig,  DateTime? hrrEffortEndedAt)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingState() when $default != null:
return $default(_that.status,_that.recordingKind,_that.activityTypeId,_that.exerciseType,_that.startTime,_that.endTime,_that.pausedStartedAt,_that.totalPausedMillis,_that.pauseIntervals,_that.points,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.latestUiPoint,_that.distanceMeters,_that.elevationGainedMeters,_that.elevationLostMeters,_that.barometerElevationGainedMeters,_that.barometerElevationLostMeters,_that.hasBarometerElevation,_that.lastBarometerAltitudeMeters,_that.currentSpeedMetersPerSecond,_that.maxSpeedMetersPerSecond,_that.gpsStatus,_that.keepScreenOnDuringRecording,_that.autoIdleEnabled,_that.autoIdleTimeoutMillis,_that.lastMovementAt,_that.totalIdleMillis,_that.repetitionCount,_that.currentSetRepetitionCount,_that.repetitionSets,_that.repetitionRestSeconds,_that.currentSetStartedAt,_that.restStartedAt,_that.accumulatedRestMillis,_that.lastAccuracyMeters,_that.lastLocationTime,_that.droppedPointCount,_that.errorMessage,_that.currentHeartRateBpm,_that.currentCyclingCadenceRpm,_that.currentPowerWatts,_that.currentSensorSpeedMetersPerSecond,_that.currentRunningCadenceRpm,_that.bleHeartRateNoSignal,_that.bleDeviceStatuses,_that.dashboardLayout,_that.hrrPhase,_that.hrrConfig,_that.hrrEffortEndedAt);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingState extends ActivityRecordingState with DiagnosticableTreeMixin {
  const _ActivityRecordingState({this.status = ActivityRecordingStatus.idle, this.recordingKind = ActivityRecordingKind.gpsRoute, this.activityTypeId, this.exerciseType, this.startTime, this.endTime, this.pausedStartedAt, this.totalPausedMillis = 0, final  List<ActivityPauseInterval> pauseIntervals = const <ActivityPauseInterval>[], final  List<ExerciseRoutePoint> points = const <ExerciseRoutePoint>[], final  List<int> routeBreakIndexes = const <int>[], final  List<ActivityRecordingLap> manualLaps = const <ActivityRecordingLap>[], final  List<ActivityRecordingMarker> markers = const <ActivityRecordingMarker>[], this.latestUiPoint, this.distanceMeters = 0.0, this.elevationGainedMeters = 0.0, this.elevationLostMeters = 0.0, this.barometerElevationGainedMeters = 0.0, this.barometerElevationLostMeters = 0.0, this.hasBarometerElevation = false, this.lastBarometerAltitudeMeters, this.currentSpeedMetersPerSecond = 0.0, this.maxSpeedMetersPerSecond = 0.0, this.gpsStatus = ActivityGpsStatus.waitingForFix, this.keepScreenOnDuringRecording = ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording, this.autoIdleEnabled = ActivityRecordingPreferences.defaultAutoIdleEnabled, this.autoIdleTimeoutMillis = ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds * 1000, this.lastMovementAt, this.totalIdleMillis = 0, this.repetitionCount = 0, this.currentSetRepetitionCount = 0, final  List<ActivityRecordedRepetitionSet> repetitionSets = const <ActivityRecordedRepetitionSet>[], this.repetitionRestSeconds = 0, this.currentSetStartedAt, this.restStartedAt, this.accumulatedRestMillis = 0, this.lastAccuracyMeters, this.lastLocationTime, this.droppedPointCount = 0, this.errorMessage, this.currentHeartRateBpm, this.currentCyclingCadenceRpm, this.currentPowerWatts, this.currentSensorSpeedMetersPerSecond, this.currentRunningCadenceRpm, this.bleHeartRateNoSignal = false, final  List<BleDeviceConnectionStatus> bleDeviceStatuses = const <BleDeviceConnectionStatus>[], this.dashboardLayout = const ActivityRecordingDashboardLayout(), this.hrrPhase = ActivityRecordingHrrPhase.none, this.hrrConfig = const HeartRateRecoveryTestConfig(), this.hrrEffortEndedAt}): _pauseIntervals = pauseIntervals,_points = points,_routeBreakIndexes = routeBreakIndexes,_manualLaps = manualLaps,_markers = markers,_repetitionSets = repetitionSets,_bleDeviceStatuses = bleDeviceStatuses,super._();
  

@override@JsonKey() final  ActivityRecordingStatus status;
@override@JsonKey() final  ActivityRecordingKind recordingKind;
@override final  String? activityTypeId;
@override final  int? exerciseType;
@override final  DateTime? startTime;
@override final  DateTime? endTime;
@override final  DateTime? pausedStartedAt;
@override@JsonKey() final  int totalPausedMillis;
 final  List<ActivityPauseInterval> _pauseIntervals;
@override@JsonKey() List<ActivityPauseInterval> get pauseIntervals {
  if (_pauseIntervals is EqualUnmodifiableListView) return _pauseIntervals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_pauseIntervals);
}

 final  List<ExerciseRoutePoint> _points;
@override@JsonKey() List<ExerciseRoutePoint> get points {
  if (_points is EqualUnmodifiableListView) return _points;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_points);
}

 final  List<int> _routeBreakIndexes;
@override@JsonKey() List<int> get routeBreakIndexes {
  if (_routeBreakIndexes is EqualUnmodifiableListView) return _routeBreakIndexes;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_routeBreakIndexes);
}

 final  List<ActivityRecordingLap> _manualLaps;
@override@JsonKey() List<ActivityRecordingLap> get manualLaps {
  if (_manualLaps is EqualUnmodifiableListView) return _manualLaps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_manualLaps);
}

 final  List<ActivityRecordingMarker> _markers;
@override@JsonKey() List<ActivityRecordingMarker> get markers {
  if (_markers is EqualUnmodifiableListView) return _markers;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_markers);
}

@override final  ExerciseRoutePoint? latestUiPoint;
@override@JsonKey() final  double distanceMeters;
@override@JsonKey() final  double elevationGainedMeters;
@override@JsonKey() final  double elevationLostMeters;
@override@JsonKey() final  double barometerElevationGainedMeters;
@override@JsonKey() final  double barometerElevationLostMeters;
@override@JsonKey() final  bool hasBarometerElevation;
@override final  double? lastBarometerAltitudeMeters;
@override@JsonKey() final  double currentSpeedMetersPerSecond;
@override@JsonKey() final  double maxSpeedMetersPerSecond;
@override@JsonKey() final  ActivityGpsStatus gpsStatus;
@override@JsonKey() final  bool keepScreenOnDuringRecording;
@override@JsonKey() final  bool autoIdleEnabled;
@override@JsonKey() final  int autoIdleTimeoutMillis;
@override final  DateTime? lastMovementAt;
@override@JsonKey() final  int totalIdleMillis;
@override@JsonKey() final  int repetitionCount;
@override@JsonKey() final  int currentSetRepetitionCount;
 final  List<ActivityRecordedRepetitionSet> _repetitionSets;
@override@JsonKey() List<ActivityRecordedRepetitionSet> get repetitionSets {
  if (_repetitionSets is EqualUnmodifiableListView) return _repetitionSets;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_repetitionSets);
}

@override@JsonKey() final  int repetitionRestSeconds;
@override final  DateTime? currentSetStartedAt;
@override final  DateTime? restStartedAt;
@override@JsonKey() final  int accumulatedRestMillis;
@override final  double? lastAccuracyMeters;
@override final  DateTime? lastLocationTime;
@override@JsonKey() final  int droppedPointCount;
@override final  String? errorMessage;
@override final  int? currentHeartRateBpm;
@override final  int? currentCyclingCadenceRpm;
@override final  double? currentPowerWatts;
@override final  double? currentSensorSpeedMetersPerSecond;
@override final  int? currentRunningCadenceRpm;
@override@JsonKey() final  bool bleHeartRateNoSignal;
 final  List<BleDeviceConnectionStatus> _bleDeviceStatuses;
@override@JsonKey() List<BleDeviceConnectionStatus> get bleDeviceStatuses {
  if (_bleDeviceStatuses is EqualUnmodifiableListView) return _bleDeviceStatuses;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_bleDeviceStatuses);
}

@override@JsonKey() final  ActivityRecordingDashboardLayout dashboardLayout;
@override@JsonKey() final  ActivityRecordingHrrPhase hrrPhase;
@override@JsonKey() final  HeartRateRecoveryTestConfig hrrConfig;
/// The instant the effort stopped — the one the whole measurement hangs on.
///
/// Deliberately NOT persisted to the draft store (see
/// `activity_recording_serialization.dart`). The heart-rate samples live in memory,
/// in the BLE coordinator, and do not survive the process being killed; a restored
/// recording therefore comes back as an ordinary one, with no phase and no recovery
/// mark. That loses the test, which is a pity — but the alternative is saving a
/// session that carries a rest segment claiming a recovery, with no heart rate behind
/// it to measure. A lost measurement is recoverable. A fabricated one is not.
@override final  DateTime? hrrEffortEndedAt;

/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingStateCopyWith<_ActivityRecordingState> get copyWith => __$ActivityRecordingStateCopyWithImpl<_ActivityRecordingState>(this, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordingState'))
    ..add(DiagnosticsProperty('status', status))..add(DiagnosticsProperty('recordingKind', recordingKind))..add(DiagnosticsProperty('activityTypeId', activityTypeId))..add(DiagnosticsProperty('exerciseType', exerciseType))..add(DiagnosticsProperty('startTime', startTime))..add(DiagnosticsProperty('endTime', endTime))..add(DiagnosticsProperty('pausedStartedAt', pausedStartedAt))..add(DiagnosticsProperty('totalPausedMillis', totalPausedMillis))..add(DiagnosticsProperty('pauseIntervals', pauseIntervals))..add(DiagnosticsProperty('points', points))..add(DiagnosticsProperty('routeBreakIndexes', routeBreakIndexes))..add(DiagnosticsProperty('manualLaps', manualLaps))..add(DiagnosticsProperty('markers', markers))..add(DiagnosticsProperty('latestUiPoint', latestUiPoint))..add(DiagnosticsProperty('distanceMeters', distanceMeters))..add(DiagnosticsProperty('elevationGainedMeters', elevationGainedMeters))..add(DiagnosticsProperty('elevationLostMeters', elevationLostMeters))..add(DiagnosticsProperty('barometerElevationGainedMeters', barometerElevationGainedMeters))..add(DiagnosticsProperty('barometerElevationLostMeters', barometerElevationLostMeters))..add(DiagnosticsProperty('hasBarometerElevation', hasBarometerElevation))..add(DiagnosticsProperty('lastBarometerAltitudeMeters', lastBarometerAltitudeMeters))..add(DiagnosticsProperty('currentSpeedMetersPerSecond', currentSpeedMetersPerSecond))..add(DiagnosticsProperty('maxSpeedMetersPerSecond', maxSpeedMetersPerSecond))..add(DiagnosticsProperty('gpsStatus', gpsStatus))..add(DiagnosticsProperty('keepScreenOnDuringRecording', keepScreenOnDuringRecording))..add(DiagnosticsProperty('autoIdleEnabled', autoIdleEnabled))..add(DiagnosticsProperty('autoIdleTimeoutMillis', autoIdleTimeoutMillis))..add(DiagnosticsProperty('lastMovementAt', lastMovementAt))..add(DiagnosticsProperty('totalIdleMillis', totalIdleMillis))..add(DiagnosticsProperty('repetitionCount', repetitionCount))..add(DiagnosticsProperty('currentSetRepetitionCount', currentSetRepetitionCount))..add(DiagnosticsProperty('repetitionSets', repetitionSets))..add(DiagnosticsProperty('repetitionRestSeconds', repetitionRestSeconds))..add(DiagnosticsProperty('currentSetStartedAt', currentSetStartedAt))..add(DiagnosticsProperty('restStartedAt', restStartedAt))..add(DiagnosticsProperty('accumulatedRestMillis', accumulatedRestMillis))..add(DiagnosticsProperty('lastAccuracyMeters', lastAccuracyMeters))..add(DiagnosticsProperty('lastLocationTime', lastLocationTime))..add(DiagnosticsProperty('droppedPointCount', droppedPointCount))..add(DiagnosticsProperty('errorMessage', errorMessage))..add(DiagnosticsProperty('currentHeartRateBpm', currentHeartRateBpm))..add(DiagnosticsProperty('currentCyclingCadenceRpm', currentCyclingCadenceRpm))..add(DiagnosticsProperty('currentPowerWatts', currentPowerWatts))..add(DiagnosticsProperty('currentSensorSpeedMetersPerSecond', currentSensorSpeedMetersPerSecond))..add(DiagnosticsProperty('currentRunningCadenceRpm', currentRunningCadenceRpm))..add(DiagnosticsProperty('bleHeartRateNoSignal', bleHeartRateNoSignal))..add(DiagnosticsProperty('bleDeviceStatuses', bleDeviceStatuses))..add(DiagnosticsProperty('dashboardLayout', dashboardLayout))..add(DiagnosticsProperty('hrrPhase', hrrPhase))..add(DiagnosticsProperty('hrrConfig', hrrConfig))..add(DiagnosticsProperty('hrrEffortEndedAt', hrrEffortEndedAt));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingState&&(identical(other.status, status) || other.status == status)&&(identical(other.recordingKind, recordingKind) || other.recordingKind == recordingKind)&&(identical(other.activityTypeId, activityTypeId) || other.activityTypeId == activityTypeId)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.pausedStartedAt, pausedStartedAt) || other.pausedStartedAt == pausedStartedAt)&&(identical(other.totalPausedMillis, totalPausedMillis) || other.totalPausedMillis == totalPausedMillis)&&const DeepCollectionEquality().equals(other._pauseIntervals, _pauseIntervals)&&const DeepCollectionEquality().equals(other._points, _points)&&const DeepCollectionEquality().equals(other._routeBreakIndexes, _routeBreakIndexes)&&const DeepCollectionEquality().equals(other._manualLaps, _manualLaps)&&const DeepCollectionEquality().equals(other._markers, _markers)&&(identical(other.latestUiPoint, latestUiPoint) || other.latestUiPoint == latestUiPoint)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.elevationLostMeters, elevationLostMeters) || other.elevationLostMeters == elevationLostMeters)&&(identical(other.barometerElevationGainedMeters, barometerElevationGainedMeters) || other.barometerElevationGainedMeters == barometerElevationGainedMeters)&&(identical(other.barometerElevationLostMeters, barometerElevationLostMeters) || other.barometerElevationLostMeters == barometerElevationLostMeters)&&(identical(other.hasBarometerElevation, hasBarometerElevation) || other.hasBarometerElevation == hasBarometerElevation)&&(identical(other.lastBarometerAltitudeMeters, lastBarometerAltitudeMeters) || other.lastBarometerAltitudeMeters == lastBarometerAltitudeMeters)&&(identical(other.currentSpeedMetersPerSecond, currentSpeedMetersPerSecond) || other.currentSpeedMetersPerSecond == currentSpeedMetersPerSecond)&&(identical(other.maxSpeedMetersPerSecond, maxSpeedMetersPerSecond) || other.maxSpeedMetersPerSecond == maxSpeedMetersPerSecond)&&(identical(other.gpsStatus, gpsStatus) || other.gpsStatus == gpsStatus)&&(identical(other.keepScreenOnDuringRecording, keepScreenOnDuringRecording) || other.keepScreenOnDuringRecording == keepScreenOnDuringRecording)&&(identical(other.autoIdleEnabled, autoIdleEnabled) || other.autoIdleEnabled == autoIdleEnabled)&&(identical(other.autoIdleTimeoutMillis, autoIdleTimeoutMillis) || other.autoIdleTimeoutMillis == autoIdleTimeoutMillis)&&(identical(other.lastMovementAt, lastMovementAt) || other.lastMovementAt == lastMovementAt)&&(identical(other.totalIdleMillis, totalIdleMillis) || other.totalIdleMillis == totalIdleMillis)&&(identical(other.repetitionCount, repetitionCount) || other.repetitionCount == repetitionCount)&&(identical(other.currentSetRepetitionCount, currentSetRepetitionCount) || other.currentSetRepetitionCount == currentSetRepetitionCount)&&const DeepCollectionEquality().equals(other._repetitionSets, _repetitionSets)&&(identical(other.repetitionRestSeconds, repetitionRestSeconds) || other.repetitionRestSeconds == repetitionRestSeconds)&&(identical(other.currentSetStartedAt, currentSetStartedAt) || other.currentSetStartedAt == currentSetStartedAt)&&(identical(other.restStartedAt, restStartedAt) || other.restStartedAt == restStartedAt)&&(identical(other.accumulatedRestMillis, accumulatedRestMillis) || other.accumulatedRestMillis == accumulatedRestMillis)&&(identical(other.lastAccuracyMeters, lastAccuracyMeters) || other.lastAccuracyMeters == lastAccuracyMeters)&&(identical(other.lastLocationTime, lastLocationTime) || other.lastLocationTime == lastLocationTime)&&(identical(other.droppedPointCount, droppedPointCount) || other.droppedPointCount == droppedPointCount)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage)&&(identical(other.currentHeartRateBpm, currentHeartRateBpm) || other.currentHeartRateBpm == currentHeartRateBpm)&&(identical(other.currentCyclingCadenceRpm, currentCyclingCadenceRpm) || other.currentCyclingCadenceRpm == currentCyclingCadenceRpm)&&(identical(other.currentPowerWatts, currentPowerWatts) || other.currentPowerWatts == currentPowerWatts)&&(identical(other.currentSensorSpeedMetersPerSecond, currentSensorSpeedMetersPerSecond) || other.currentSensorSpeedMetersPerSecond == currentSensorSpeedMetersPerSecond)&&(identical(other.currentRunningCadenceRpm, currentRunningCadenceRpm) || other.currentRunningCadenceRpm == currentRunningCadenceRpm)&&(identical(other.bleHeartRateNoSignal, bleHeartRateNoSignal) || other.bleHeartRateNoSignal == bleHeartRateNoSignal)&&const DeepCollectionEquality().equals(other._bleDeviceStatuses, _bleDeviceStatuses)&&(identical(other.dashboardLayout, dashboardLayout) || other.dashboardLayout == dashboardLayout)&&(identical(other.hrrPhase, hrrPhase) || other.hrrPhase == hrrPhase)&&(identical(other.hrrConfig, hrrConfig) || other.hrrConfig == hrrConfig)&&(identical(other.hrrEffortEndedAt, hrrEffortEndedAt) || other.hrrEffortEndedAt == hrrEffortEndedAt));
}


@override
int get hashCode => Object.hashAll([runtimeType,status,recordingKind,activityTypeId,exerciseType,startTime,endTime,pausedStartedAt,totalPausedMillis,const DeepCollectionEquality().hash(_pauseIntervals),const DeepCollectionEquality().hash(_points),const DeepCollectionEquality().hash(_routeBreakIndexes),const DeepCollectionEquality().hash(_manualLaps),const DeepCollectionEquality().hash(_markers),latestUiPoint,distanceMeters,elevationGainedMeters,elevationLostMeters,barometerElevationGainedMeters,barometerElevationLostMeters,hasBarometerElevation,lastBarometerAltitudeMeters,currentSpeedMetersPerSecond,maxSpeedMetersPerSecond,gpsStatus,keepScreenOnDuringRecording,autoIdleEnabled,autoIdleTimeoutMillis,lastMovementAt,totalIdleMillis,repetitionCount,currentSetRepetitionCount,const DeepCollectionEquality().hash(_repetitionSets),repetitionRestSeconds,currentSetStartedAt,restStartedAt,accumulatedRestMillis,lastAccuracyMeters,lastLocationTime,droppedPointCount,errorMessage,currentHeartRateBpm,currentCyclingCadenceRpm,currentPowerWatts,currentSensorSpeedMetersPerSecond,currentRunningCadenceRpm,bleHeartRateNoSignal,const DeepCollectionEquality().hash(_bleDeviceStatuses),dashboardLayout,hrrPhase,hrrConfig,hrrEffortEndedAt]);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordingState(status: $status, recordingKind: $recordingKind, activityTypeId: $activityTypeId, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, pausedStartedAt: $pausedStartedAt, totalPausedMillis: $totalPausedMillis, pauseIntervals: $pauseIntervals, points: $points, routeBreakIndexes: $routeBreakIndexes, manualLaps: $manualLaps, markers: $markers, latestUiPoint: $latestUiPoint, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, elevationLostMeters: $elevationLostMeters, barometerElevationGainedMeters: $barometerElevationGainedMeters, barometerElevationLostMeters: $barometerElevationLostMeters, hasBarometerElevation: $hasBarometerElevation, lastBarometerAltitudeMeters: $lastBarometerAltitudeMeters, currentSpeedMetersPerSecond: $currentSpeedMetersPerSecond, maxSpeedMetersPerSecond: $maxSpeedMetersPerSecond, gpsStatus: $gpsStatus, keepScreenOnDuringRecording: $keepScreenOnDuringRecording, autoIdleEnabled: $autoIdleEnabled, autoIdleTimeoutMillis: $autoIdleTimeoutMillis, lastMovementAt: $lastMovementAt, totalIdleMillis: $totalIdleMillis, repetitionCount: $repetitionCount, currentSetRepetitionCount: $currentSetRepetitionCount, repetitionSets: $repetitionSets, repetitionRestSeconds: $repetitionRestSeconds, currentSetStartedAt: $currentSetStartedAt, restStartedAt: $restStartedAt, accumulatedRestMillis: $accumulatedRestMillis, lastAccuracyMeters: $lastAccuracyMeters, lastLocationTime: $lastLocationTime, droppedPointCount: $droppedPointCount, errorMessage: $errorMessage, currentHeartRateBpm: $currentHeartRateBpm, currentCyclingCadenceRpm: $currentCyclingCadenceRpm, currentPowerWatts: $currentPowerWatts, currentSensorSpeedMetersPerSecond: $currentSensorSpeedMetersPerSecond, currentRunningCadenceRpm: $currentRunningCadenceRpm, bleHeartRateNoSignal: $bleHeartRateNoSignal, bleDeviceStatuses: $bleDeviceStatuses, dashboardLayout: $dashboardLayout, hrrPhase: $hrrPhase, hrrConfig: $hrrConfig, hrrEffortEndedAt: $hrrEffortEndedAt)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingStateCopyWith<$Res> implements $ActivityRecordingStateCopyWith<$Res> {
  factory _$ActivityRecordingStateCopyWith(_ActivityRecordingState value, $Res Function(_ActivityRecordingState) _then) = __$ActivityRecordingStateCopyWithImpl;
@override @useResult
$Res call({
 ActivityRecordingStatus status, ActivityRecordingKind recordingKind, String? activityTypeId, int? exerciseType, DateTime? startTime, DateTime? endTime, DateTime? pausedStartedAt, int totalPausedMillis, List<ActivityPauseInterval> pauseIntervals, List<ExerciseRoutePoint> points, List<int> routeBreakIndexes, List<ActivityRecordingLap> manualLaps, List<ActivityRecordingMarker> markers, ExerciseRoutePoint? latestUiPoint, double distanceMeters, double elevationGainedMeters, double elevationLostMeters, double barometerElevationGainedMeters, double barometerElevationLostMeters, bool hasBarometerElevation, double? lastBarometerAltitudeMeters, double currentSpeedMetersPerSecond, double maxSpeedMetersPerSecond, ActivityGpsStatus gpsStatus, bool keepScreenOnDuringRecording, bool autoIdleEnabled, int autoIdleTimeoutMillis, DateTime? lastMovementAt, int totalIdleMillis, int repetitionCount, int currentSetRepetitionCount, List<ActivityRecordedRepetitionSet> repetitionSets, int repetitionRestSeconds, DateTime? currentSetStartedAt, DateTime? restStartedAt, int accumulatedRestMillis, double? lastAccuracyMeters, DateTime? lastLocationTime, int droppedPointCount, String? errorMessage, int? currentHeartRateBpm, int? currentCyclingCadenceRpm, double? currentPowerWatts, double? currentSensorSpeedMetersPerSecond, int? currentRunningCadenceRpm, bool bleHeartRateNoSignal, List<BleDeviceConnectionStatus> bleDeviceStatuses, ActivityRecordingDashboardLayout dashboardLayout, ActivityRecordingHrrPhase hrrPhase, HeartRateRecoveryTestConfig hrrConfig, DateTime? hrrEffortEndedAt
});


@override $ExerciseRoutePointCopyWith<$Res>? get latestUiPoint;@override $HeartRateRecoveryTestConfigCopyWith<$Res> get hrrConfig;

}
/// @nodoc
class __$ActivityRecordingStateCopyWithImpl<$Res>
    implements _$ActivityRecordingStateCopyWith<$Res> {
  __$ActivityRecordingStateCopyWithImpl(this._self, this._then);

  final _ActivityRecordingState _self;
  final $Res Function(_ActivityRecordingState) _then;

/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? recordingKind = null,Object? activityTypeId = freezed,Object? exerciseType = freezed,Object? startTime = freezed,Object? endTime = freezed,Object? pausedStartedAt = freezed,Object? totalPausedMillis = null,Object? pauseIntervals = null,Object? points = null,Object? routeBreakIndexes = null,Object? manualLaps = null,Object? markers = null,Object? latestUiPoint = freezed,Object? distanceMeters = null,Object? elevationGainedMeters = null,Object? elevationLostMeters = null,Object? barometerElevationGainedMeters = null,Object? barometerElevationLostMeters = null,Object? hasBarometerElevation = null,Object? lastBarometerAltitudeMeters = freezed,Object? currentSpeedMetersPerSecond = null,Object? maxSpeedMetersPerSecond = null,Object? gpsStatus = null,Object? keepScreenOnDuringRecording = null,Object? autoIdleEnabled = null,Object? autoIdleTimeoutMillis = null,Object? lastMovementAt = freezed,Object? totalIdleMillis = null,Object? repetitionCount = null,Object? currentSetRepetitionCount = null,Object? repetitionSets = null,Object? repetitionRestSeconds = null,Object? currentSetStartedAt = freezed,Object? restStartedAt = freezed,Object? accumulatedRestMillis = null,Object? lastAccuracyMeters = freezed,Object? lastLocationTime = freezed,Object? droppedPointCount = null,Object? errorMessage = freezed,Object? currentHeartRateBpm = freezed,Object? currentCyclingCadenceRpm = freezed,Object? currentPowerWatts = freezed,Object? currentSensorSpeedMetersPerSecond = freezed,Object? currentRunningCadenceRpm = freezed,Object? bleHeartRateNoSignal = null,Object? bleDeviceStatuses = null,Object? dashboardLayout = null,Object? hrrPhase = null,Object? hrrConfig = null,Object? hrrEffortEndedAt = freezed,}) {
  return _then(_ActivityRecordingState(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ActivityRecordingStatus,recordingKind: null == recordingKind ? _self.recordingKind : recordingKind // ignore: cast_nullable_to_non_nullable
as ActivityRecordingKind,activityTypeId: freezed == activityTypeId ? _self.activityTypeId : activityTypeId // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: freezed == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int?,startTime: freezed == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime?,endTime: freezed == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime?,pausedStartedAt: freezed == pausedStartedAt ? _self.pausedStartedAt : pausedStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,totalPausedMillis: null == totalPausedMillis ? _self.totalPausedMillis : totalPausedMillis // ignore: cast_nullable_to_non_nullable
as int,pauseIntervals: null == pauseIntervals ? _self._pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,points: null == points ? _self._points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,routeBreakIndexes: null == routeBreakIndexes ? _self._routeBreakIndexes : routeBreakIndexes // ignore: cast_nullable_to_non_nullable
as List<int>,manualLaps: null == manualLaps ? _self._manualLaps : manualLaps // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingLap>,markers: null == markers ? _self._markers : markers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,latestUiPoint: freezed == latestUiPoint ? _self.latestUiPoint : latestUiPoint // ignore: cast_nullable_to_non_nullable
as ExerciseRoutePoint?,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationGainedMeters: null == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,elevationLostMeters: null == elevationLostMeters ? _self.elevationLostMeters : elevationLostMeters // ignore: cast_nullable_to_non_nullable
as double,barometerElevationGainedMeters: null == barometerElevationGainedMeters ? _self.barometerElevationGainedMeters : barometerElevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,barometerElevationLostMeters: null == barometerElevationLostMeters ? _self.barometerElevationLostMeters : barometerElevationLostMeters // ignore: cast_nullable_to_non_nullable
as double,hasBarometerElevation: null == hasBarometerElevation ? _self.hasBarometerElevation : hasBarometerElevation // ignore: cast_nullable_to_non_nullable
as bool,lastBarometerAltitudeMeters: freezed == lastBarometerAltitudeMeters ? _self.lastBarometerAltitudeMeters : lastBarometerAltitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,currentSpeedMetersPerSecond: null == currentSpeedMetersPerSecond ? _self.currentSpeedMetersPerSecond : currentSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,maxSpeedMetersPerSecond: null == maxSpeedMetersPerSecond ? _self.maxSpeedMetersPerSecond : maxSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,gpsStatus: null == gpsStatus ? _self.gpsStatus : gpsStatus // ignore: cast_nullable_to_non_nullable
as ActivityGpsStatus,keepScreenOnDuringRecording: null == keepScreenOnDuringRecording ? _self.keepScreenOnDuringRecording : keepScreenOnDuringRecording // ignore: cast_nullable_to_non_nullable
as bool,autoIdleEnabled: null == autoIdleEnabled ? _self.autoIdleEnabled : autoIdleEnabled // ignore: cast_nullable_to_non_nullable
as bool,autoIdleTimeoutMillis: null == autoIdleTimeoutMillis ? _self.autoIdleTimeoutMillis : autoIdleTimeoutMillis // ignore: cast_nullable_to_non_nullable
as int,lastMovementAt: freezed == lastMovementAt ? _self.lastMovementAt : lastMovementAt // ignore: cast_nullable_to_non_nullable
as DateTime?,totalIdleMillis: null == totalIdleMillis ? _self.totalIdleMillis : totalIdleMillis // ignore: cast_nullable_to_non_nullable
as int,repetitionCount: null == repetitionCount ? _self.repetitionCount : repetitionCount // ignore: cast_nullable_to_non_nullable
as int,currentSetRepetitionCount: null == currentSetRepetitionCount ? _self.currentSetRepetitionCount : currentSetRepetitionCount // ignore: cast_nullable_to_non_nullable
as int,repetitionSets: null == repetitionSets ? _self._repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordedRepetitionSet>,repetitionRestSeconds: null == repetitionRestSeconds ? _self.repetitionRestSeconds : repetitionRestSeconds // ignore: cast_nullable_to_non_nullable
as int,currentSetStartedAt: freezed == currentSetStartedAt ? _self.currentSetStartedAt : currentSetStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,restStartedAt: freezed == restStartedAt ? _self.restStartedAt : restStartedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,accumulatedRestMillis: null == accumulatedRestMillis ? _self.accumulatedRestMillis : accumulatedRestMillis // ignore: cast_nullable_to_non_nullable
as int,lastAccuracyMeters: freezed == lastAccuracyMeters ? _self.lastAccuracyMeters : lastAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,lastLocationTime: freezed == lastLocationTime ? _self.lastLocationTime : lastLocationTime // ignore: cast_nullable_to_non_nullable
as DateTime?,droppedPointCount: null == droppedPointCount ? _self.droppedPointCount : droppedPointCount // ignore: cast_nullable_to_non_nullable
as int,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,currentHeartRateBpm: freezed == currentHeartRateBpm ? _self.currentHeartRateBpm : currentHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,currentCyclingCadenceRpm: freezed == currentCyclingCadenceRpm ? _self.currentCyclingCadenceRpm : currentCyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,currentPowerWatts: freezed == currentPowerWatts ? _self.currentPowerWatts : currentPowerWatts // ignore: cast_nullable_to_non_nullable
as double?,currentSensorSpeedMetersPerSecond: freezed == currentSensorSpeedMetersPerSecond ? _self.currentSensorSpeedMetersPerSecond : currentSensorSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,currentRunningCadenceRpm: freezed == currentRunningCadenceRpm ? _self.currentRunningCadenceRpm : currentRunningCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,bleHeartRateNoSignal: null == bleHeartRateNoSignal ? _self.bleHeartRateNoSignal : bleHeartRateNoSignal // ignore: cast_nullable_to_non_nullable
as bool,bleDeviceStatuses: null == bleDeviceStatuses ? _self._bleDeviceStatuses : bleDeviceStatuses // ignore: cast_nullable_to_non_nullable
as List<BleDeviceConnectionStatus>,dashboardLayout: null == dashboardLayout ? _self.dashboardLayout : dashboardLayout // ignore: cast_nullable_to_non_nullable
as ActivityRecordingDashboardLayout,hrrPhase: null == hrrPhase ? _self.hrrPhase : hrrPhase // ignore: cast_nullable_to_non_nullable
as ActivityRecordingHrrPhase,hrrConfig: null == hrrConfig ? _self.hrrConfig : hrrConfig // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryTestConfig,hrrEffortEndedAt: freezed == hrrEffortEndedAt ? _self.hrrEffortEndedAt : hrrEffortEndedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}

/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseRoutePointCopyWith<$Res>? get latestUiPoint {
    if (_self.latestUiPoint == null) {
    return null;
  }

  return $ExerciseRoutePointCopyWith<$Res>(_self.latestUiPoint!, (value) {
    return _then(_self.copyWith(latestUiPoint: value));
  });
}/// Create a copy of ActivityRecordingState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartRateRecoveryTestConfigCopyWith<$Res> get hrrConfig {
  
  return $HeartRateRecoveryTestConfigCopyWith<$Res>(_self.hrrConfig, (value) {
    return _then(_self.copyWith(hrrConfig: value));
  });
}
}

/// @nodoc
mixin _$ActivityRecordingSnapshot implements DiagnosticableTreeMixin {

 int get exerciseType; ActivityRecordingKind get recordingKind; String? get activityTypeId; DateTime get startTime; DateTime get endTime; List<ExerciseRoutePoint> get points; List<ActivityPauseInterval> get pauseIntervals; List<int> get routeBreakIndexes; List<ActivityRecordingLap> get manualLaps; List<ActivityRecordingMarker> get markers; double get distanceMeters; double get elevationGainedMeters; int get repetitionCount; List<ActivityRecordedRepetitionSet> get repetitionSets; BleRecordingSampleBuffer get bleSamples;/// When the effort stopped, for a heart-rate-recovery test. This is what gets written
/// as a trailing rest segment, and it is the only thing that tells anyone reading the
/// session back where the recovery began.
 DateTime? get hrrEffortEndedAt;
/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingSnapshotCopyWith<ActivityRecordingSnapshot> get copyWith => _$ActivityRecordingSnapshotCopyWithImpl<ActivityRecordingSnapshot>(this as ActivityRecordingSnapshot, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordingSnapshot'))
    ..add(DiagnosticsProperty('exerciseType', exerciseType))..add(DiagnosticsProperty('recordingKind', recordingKind))..add(DiagnosticsProperty('activityTypeId', activityTypeId))..add(DiagnosticsProperty('startTime', startTime))..add(DiagnosticsProperty('endTime', endTime))..add(DiagnosticsProperty('points', points))..add(DiagnosticsProperty('pauseIntervals', pauseIntervals))..add(DiagnosticsProperty('routeBreakIndexes', routeBreakIndexes))..add(DiagnosticsProperty('manualLaps', manualLaps))..add(DiagnosticsProperty('markers', markers))..add(DiagnosticsProperty('distanceMeters', distanceMeters))..add(DiagnosticsProperty('elevationGainedMeters', elevationGainedMeters))..add(DiagnosticsProperty('repetitionCount', repetitionCount))..add(DiagnosticsProperty('repetitionSets', repetitionSets))..add(DiagnosticsProperty('bleSamples', bleSamples))..add(DiagnosticsProperty('hrrEffortEndedAt', hrrEffortEndedAt));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingSnapshot&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.recordingKind, recordingKind) || other.recordingKind == recordingKind)&&(identical(other.activityTypeId, activityTypeId) || other.activityTypeId == activityTypeId)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&const DeepCollectionEquality().equals(other.points, points)&&const DeepCollectionEquality().equals(other.pauseIntervals, pauseIntervals)&&const DeepCollectionEquality().equals(other.routeBreakIndexes, routeBreakIndexes)&&const DeepCollectionEquality().equals(other.manualLaps, manualLaps)&&const DeepCollectionEquality().equals(other.markers, markers)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.repetitionCount, repetitionCount) || other.repetitionCount == repetitionCount)&&const DeepCollectionEquality().equals(other.repetitionSets, repetitionSets)&&(identical(other.bleSamples, bleSamples) || other.bleSamples == bleSamples)&&(identical(other.hrrEffortEndedAt, hrrEffortEndedAt) || other.hrrEffortEndedAt == hrrEffortEndedAt));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,recordingKind,activityTypeId,startTime,endTime,const DeepCollectionEquality().hash(points),const DeepCollectionEquality().hash(pauseIntervals),const DeepCollectionEquality().hash(routeBreakIndexes),const DeepCollectionEquality().hash(manualLaps),const DeepCollectionEquality().hash(markers),distanceMeters,elevationGainedMeters,repetitionCount,const DeepCollectionEquality().hash(repetitionSets),bleSamples,hrrEffortEndedAt);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordingSnapshot(exerciseType: $exerciseType, recordingKind: $recordingKind, activityTypeId: $activityTypeId, startTime: $startTime, endTime: $endTime, points: $points, pauseIntervals: $pauseIntervals, routeBreakIndexes: $routeBreakIndexes, manualLaps: $manualLaps, markers: $markers, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, repetitionCount: $repetitionCount, repetitionSets: $repetitionSets, bleSamples: $bleSamples, hrrEffortEndedAt: $hrrEffortEndedAt)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingSnapshotCopyWith<$Res>  {
  factory $ActivityRecordingSnapshotCopyWith(ActivityRecordingSnapshot value, $Res Function(ActivityRecordingSnapshot) _then) = _$ActivityRecordingSnapshotCopyWithImpl;
@useResult
$Res call({
 int exerciseType, ActivityRecordingKind recordingKind, String? activityTypeId, DateTime startTime, DateTime endTime, List<ExerciseRoutePoint> points, List<ActivityPauseInterval> pauseIntervals, List<int> routeBreakIndexes, List<ActivityRecordingLap> manualLaps, List<ActivityRecordingMarker> markers, double distanceMeters, double elevationGainedMeters, int repetitionCount, List<ActivityRecordedRepetitionSet> repetitionSets, BleRecordingSampleBuffer bleSamples, DateTime? hrrEffortEndedAt
});


$BleRecordingSampleBufferCopyWith<$Res> get bleSamples;

}
/// @nodoc
class _$ActivityRecordingSnapshotCopyWithImpl<$Res>
    implements $ActivityRecordingSnapshotCopyWith<$Res> {
  _$ActivityRecordingSnapshotCopyWithImpl(this._self, this._then);

  final ActivityRecordingSnapshot _self;
  final $Res Function(ActivityRecordingSnapshot) _then;

/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? exerciseType = null,Object? recordingKind = null,Object? activityTypeId = freezed,Object? startTime = null,Object? endTime = null,Object? points = null,Object? pauseIntervals = null,Object? routeBreakIndexes = null,Object? manualLaps = null,Object? markers = null,Object? distanceMeters = null,Object? elevationGainedMeters = null,Object? repetitionCount = null,Object? repetitionSets = null,Object? bleSamples = null,Object? hrrEffortEndedAt = freezed,}) {
  return _then(_self.copyWith(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,recordingKind: null == recordingKind ? _self.recordingKind : recordingKind // ignore: cast_nullable_to_non_nullable
as ActivityRecordingKind,activityTypeId: freezed == activityTypeId ? _self.activityTypeId : activityTypeId // ignore: cast_nullable_to_non_nullable
as String?,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,points: null == points ? _self.points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,pauseIntervals: null == pauseIntervals ? _self.pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,routeBreakIndexes: null == routeBreakIndexes ? _self.routeBreakIndexes : routeBreakIndexes // ignore: cast_nullable_to_non_nullable
as List<int>,manualLaps: null == manualLaps ? _self.manualLaps : manualLaps // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingLap>,markers: null == markers ? _self.markers : markers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationGainedMeters: null == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,repetitionCount: null == repetitionCount ? _self.repetitionCount : repetitionCount // ignore: cast_nullable_to_non_nullable
as int,repetitionSets: null == repetitionSets ? _self.repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordedRepetitionSet>,bleSamples: null == bleSamples ? _self.bleSamples : bleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,hrrEffortEndedAt: freezed == hrrEffortEndedAt ? _self.hrrEffortEndedAt : hrrEffortEndedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}
/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get bleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.bleSamples, (value) {
    return _then(_self.copyWith(bleSamples: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityRecordingSnapshot].
extension ActivityRecordingSnapshotPatterns on ActivityRecordingSnapshot {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingSnapshot value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingSnapshot value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingSnapshot value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int exerciseType,  ActivityRecordingKind recordingKind,  String? activityTypeId,  DateTime startTime,  DateTime endTime,  List<ExerciseRoutePoint> points,  List<ActivityPauseInterval> pauseIntervals,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  double distanceMeters,  double elevationGainedMeters,  int repetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  BleRecordingSampleBuffer bleSamples,  DateTime? hrrEffortEndedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot() when $default != null:
return $default(_that.exerciseType,_that.recordingKind,_that.activityTypeId,_that.startTime,_that.endTime,_that.points,_that.pauseIntervals,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.distanceMeters,_that.elevationGainedMeters,_that.repetitionCount,_that.repetitionSets,_that.bleSamples,_that.hrrEffortEndedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int exerciseType,  ActivityRecordingKind recordingKind,  String? activityTypeId,  DateTime startTime,  DateTime endTime,  List<ExerciseRoutePoint> points,  List<ActivityPauseInterval> pauseIntervals,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  double distanceMeters,  double elevationGainedMeters,  int repetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  BleRecordingSampleBuffer bleSamples,  DateTime? hrrEffortEndedAt)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot():
return $default(_that.exerciseType,_that.recordingKind,_that.activityTypeId,_that.startTime,_that.endTime,_that.points,_that.pauseIntervals,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.distanceMeters,_that.elevationGainedMeters,_that.repetitionCount,_that.repetitionSets,_that.bleSamples,_that.hrrEffortEndedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int exerciseType,  ActivityRecordingKind recordingKind,  String? activityTypeId,  DateTime startTime,  DateTime endTime,  List<ExerciseRoutePoint> points,  List<ActivityPauseInterval> pauseIntervals,  List<int> routeBreakIndexes,  List<ActivityRecordingLap> manualLaps,  List<ActivityRecordingMarker> markers,  double distanceMeters,  double elevationGainedMeters,  int repetitionCount,  List<ActivityRecordedRepetitionSet> repetitionSets,  BleRecordingSampleBuffer bleSamples,  DateTime? hrrEffortEndedAt)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingSnapshot() when $default != null:
return $default(_that.exerciseType,_that.recordingKind,_that.activityTypeId,_that.startTime,_that.endTime,_that.points,_that.pauseIntervals,_that.routeBreakIndexes,_that.manualLaps,_that.markers,_that.distanceMeters,_that.elevationGainedMeters,_that.repetitionCount,_that.repetitionSets,_that.bleSamples,_that.hrrEffortEndedAt);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingSnapshot with DiagnosticableTreeMixin implements ActivityRecordingSnapshot {
  const _ActivityRecordingSnapshot({required this.exerciseType, this.recordingKind = ActivityRecordingKind.gpsRoute, this.activityTypeId, required this.startTime, required this.endTime, required final  List<ExerciseRoutePoint> points, required final  List<ActivityPauseInterval> pauseIntervals, final  List<int> routeBreakIndexes = const <int>[], final  List<ActivityRecordingLap> manualLaps = const <ActivityRecordingLap>[], final  List<ActivityRecordingMarker> markers = const <ActivityRecordingMarker>[], required this.distanceMeters, required this.elevationGainedMeters, this.repetitionCount = 0, final  List<ActivityRecordedRepetitionSet> repetitionSets = const <ActivityRecordedRepetitionSet>[], this.bleSamples = const BleRecordingSampleBuffer(), this.hrrEffortEndedAt}): _points = points,_pauseIntervals = pauseIntervals,_routeBreakIndexes = routeBreakIndexes,_manualLaps = manualLaps,_markers = markers,_repetitionSets = repetitionSets;
  

@override final  int exerciseType;
@override@JsonKey() final  ActivityRecordingKind recordingKind;
@override final  String? activityTypeId;
@override final  DateTime startTime;
@override final  DateTime endTime;
 final  List<ExerciseRoutePoint> _points;
@override List<ExerciseRoutePoint> get points {
  if (_points is EqualUnmodifiableListView) return _points;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_points);
}

 final  List<ActivityPauseInterval> _pauseIntervals;
@override List<ActivityPauseInterval> get pauseIntervals {
  if (_pauseIntervals is EqualUnmodifiableListView) return _pauseIntervals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_pauseIntervals);
}

 final  List<int> _routeBreakIndexes;
@override@JsonKey() List<int> get routeBreakIndexes {
  if (_routeBreakIndexes is EqualUnmodifiableListView) return _routeBreakIndexes;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_routeBreakIndexes);
}

 final  List<ActivityRecordingLap> _manualLaps;
@override@JsonKey() List<ActivityRecordingLap> get manualLaps {
  if (_manualLaps is EqualUnmodifiableListView) return _manualLaps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_manualLaps);
}

 final  List<ActivityRecordingMarker> _markers;
@override@JsonKey() List<ActivityRecordingMarker> get markers {
  if (_markers is EqualUnmodifiableListView) return _markers;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_markers);
}

@override final  double distanceMeters;
@override final  double elevationGainedMeters;
@override@JsonKey() final  int repetitionCount;
 final  List<ActivityRecordedRepetitionSet> _repetitionSets;
@override@JsonKey() List<ActivityRecordedRepetitionSet> get repetitionSets {
  if (_repetitionSets is EqualUnmodifiableListView) return _repetitionSets;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_repetitionSets);
}

@override@JsonKey() final  BleRecordingSampleBuffer bleSamples;
/// When the effort stopped, for a heart-rate-recovery test. This is what gets written
/// as a trailing rest segment, and it is the only thing that tells anyone reading the
/// session back where the recovery began.
@override final  DateTime? hrrEffortEndedAt;

/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingSnapshotCopyWith<_ActivityRecordingSnapshot> get copyWith => __$ActivityRecordingSnapshotCopyWithImpl<_ActivityRecordingSnapshot>(this, _$identity);


@override
void debugFillProperties(DiagnosticPropertiesBuilder properties) {
  properties
    ..add(DiagnosticsProperty('type', 'ActivityRecordingSnapshot'))
    ..add(DiagnosticsProperty('exerciseType', exerciseType))..add(DiagnosticsProperty('recordingKind', recordingKind))..add(DiagnosticsProperty('activityTypeId', activityTypeId))..add(DiagnosticsProperty('startTime', startTime))..add(DiagnosticsProperty('endTime', endTime))..add(DiagnosticsProperty('points', points))..add(DiagnosticsProperty('pauseIntervals', pauseIntervals))..add(DiagnosticsProperty('routeBreakIndexes', routeBreakIndexes))..add(DiagnosticsProperty('manualLaps', manualLaps))..add(DiagnosticsProperty('markers', markers))..add(DiagnosticsProperty('distanceMeters', distanceMeters))..add(DiagnosticsProperty('elevationGainedMeters', elevationGainedMeters))..add(DiagnosticsProperty('repetitionCount', repetitionCount))..add(DiagnosticsProperty('repetitionSets', repetitionSets))..add(DiagnosticsProperty('bleSamples', bleSamples))..add(DiagnosticsProperty('hrrEffortEndedAt', hrrEffortEndedAt));
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingSnapshot&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.recordingKind, recordingKind) || other.recordingKind == recordingKind)&&(identical(other.activityTypeId, activityTypeId) || other.activityTypeId == activityTypeId)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&const DeepCollectionEquality().equals(other._points, _points)&&const DeepCollectionEquality().equals(other._pauseIntervals, _pauseIntervals)&&const DeepCollectionEquality().equals(other._routeBreakIndexes, _routeBreakIndexes)&&const DeepCollectionEquality().equals(other._manualLaps, _manualLaps)&&const DeepCollectionEquality().equals(other._markers, _markers)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.repetitionCount, repetitionCount) || other.repetitionCount == repetitionCount)&&const DeepCollectionEquality().equals(other._repetitionSets, _repetitionSets)&&(identical(other.bleSamples, bleSamples) || other.bleSamples == bleSamples)&&(identical(other.hrrEffortEndedAt, hrrEffortEndedAt) || other.hrrEffortEndedAt == hrrEffortEndedAt));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,recordingKind,activityTypeId,startTime,endTime,const DeepCollectionEquality().hash(_points),const DeepCollectionEquality().hash(_pauseIntervals),const DeepCollectionEquality().hash(_routeBreakIndexes),const DeepCollectionEquality().hash(_manualLaps),const DeepCollectionEquality().hash(_markers),distanceMeters,elevationGainedMeters,repetitionCount,const DeepCollectionEquality().hash(_repetitionSets),bleSamples,hrrEffortEndedAt);

@override
String toString({ DiagnosticLevel minLevel = DiagnosticLevel.info }) {
  return 'ActivityRecordingSnapshot(exerciseType: $exerciseType, recordingKind: $recordingKind, activityTypeId: $activityTypeId, startTime: $startTime, endTime: $endTime, points: $points, pauseIntervals: $pauseIntervals, routeBreakIndexes: $routeBreakIndexes, manualLaps: $manualLaps, markers: $markers, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, repetitionCount: $repetitionCount, repetitionSets: $repetitionSets, bleSamples: $bleSamples, hrrEffortEndedAt: $hrrEffortEndedAt)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingSnapshotCopyWith<$Res> implements $ActivityRecordingSnapshotCopyWith<$Res> {
  factory _$ActivityRecordingSnapshotCopyWith(_ActivityRecordingSnapshot value, $Res Function(_ActivityRecordingSnapshot) _then) = __$ActivityRecordingSnapshotCopyWithImpl;
@override @useResult
$Res call({
 int exerciseType, ActivityRecordingKind recordingKind, String? activityTypeId, DateTime startTime, DateTime endTime, List<ExerciseRoutePoint> points, List<ActivityPauseInterval> pauseIntervals, List<int> routeBreakIndexes, List<ActivityRecordingLap> manualLaps, List<ActivityRecordingMarker> markers, double distanceMeters, double elevationGainedMeters, int repetitionCount, List<ActivityRecordedRepetitionSet> repetitionSets, BleRecordingSampleBuffer bleSamples, DateTime? hrrEffortEndedAt
});


@override $BleRecordingSampleBufferCopyWith<$Res> get bleSamples;

}
/// @nodoc
class __$ActivityRecordingSnapshotCopyWithImpl<$Res>
    implements _$ActivityRecordingSnapshotCopyWith<$Res> {
  __$ActivityRecordingSnapshotCopyWithImpl(this._self, this._then);

  final _ActivityRecordingSnapshot _self;
  final $Res Function(_ActivityRecordingSnapshot) _then;

/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? exerciseType = null,Object? recordingKind = null,Object? activityTypeId = freezed,Object? startTime = null,Object? endTime = null,Object? points = null,Object? pauseIntervals = null,Object? routeBreakIndexes = null,Object? manualLaps = null,Object? markers = null,Object? distanceMeters = null,Object? elevationGainedMeters = null,Object? repetitionCount = null,Object? repetitionSets = null,Object? bleSamples = null,Object? hrrEffortEndedAt = freezed,}) {
  return _then(_ActivityRecordingSnapshot(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,recordingKind: null == recordingKind ? _self.recordingKind : recordingKind // ignore: cast_nullable_to_non_nullable
as ActivityRecordingKind,activityTypeId: freezed == activityTypeId ? _self.activityTypeId : activityTypeId // ignore: cast_nullable_to_non_nullable
as String?,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,points: null == points ? _self._points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,pauseIntervals: null == pauseIntervals ? _self._pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,routeBreakIndexes: null == routeBreakIndexes ? _self._routeBreakIndexes : routeBreakIndexes // ignore: cast_nullable_to_non_nullable
as List<int>,manualLaps: null == manualLaps ? _self._manualLaps : manualLaps // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingLap>,markers: null == markers ? _self._markers : markers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationGainedMeters: null == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double,repetitionCount: null == repetitionCount ? _self.repetitionCount : repetitionCount // ignore: cast_nullable_to_non_nullable
as int,repetitionSets: null == repetitionSets ? _self._repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordedRepetitionSet>,bleSamples: null == bleSamples ? _self.bleSamples : bleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,hrrEffortEndedAt: freezed == hrrEffortEndedAt ? _self.hrrEffortEndedAt : hrrEffortEndedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}

/// Create a copy of ActivityRecordingSnapshot
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get bleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.bleSamples, (value) {
    return _then(_self.copyWith(bleSamples: value));
  });
}
}

// dart format on
