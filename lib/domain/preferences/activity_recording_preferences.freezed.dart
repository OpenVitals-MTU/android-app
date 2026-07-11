// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_recording_preferences.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityRecordingPreferences {

 bool get autoIdleEnabled; int get autoIdleTimeoutSeconds; bool get keepScreenOnDuringRecording; int get requiredGpsAccuracyMeters; int? get routeGapMeters; bool get barometerClimbEnabled; int? get recordingDistanceIntervalMeters; int get recordingTimeIntervalMillis; bool get voiceAnnouncementsEnabled; int? get voiceAnnouncementTimeIntervalMinutes; int? get voiceAnnouncementDistanceIntervalMeters; bool get voiceIdleAnnouncementsEnabled; bool get voiceLapAnnouncementsEnabled; bool get restTimerBellEnabled;
/// Create a copy of ActivityRecordingPreferences
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingPreferencesCopyWith<ActivityRecordingPreferences> get copyWith => _$ActivityRecordingPreferencesCopyWithImpl<ActivityRecordingPreferences>(this as ActivityRecordingPreferences, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingPreferences&&(identical(other.autoIdleEnabled, autoIdleEnabled) || other.autoIdleEnabled == autoIdleEnabled)&&(identical(other.autoIdleTimeoutSeconds, autoIdleTimeoutSeconds) || other.autoIdleTimeoutSeconds == autoIdleTimeoutSeconds)&&(identical(other.keepScreenOnDuringRecording, keepScreenOnDuringRecording) || other.keepScreenOnDuringRecording == keepScreenOnDuringRecording)&&(identical(other.requiredGpsAccuracyMeters, requiredGpsAccuracyMeters) || other.requiredGpsAccuracyMeters == requiredGpsAccuracyMeters)&&(identical(other.routeGapMeters, routeGapMeters) || other.routeGapMeters == routeGapMeters)&&(identical(other.barometerClimbEnabled, barometerClimbEnabled) || other.barometerClimbEnabled == barometerClimbEnabled)&&(identical(other.recordingDistanceIntervalMeters, recordingDistanceIntervalMeters) || other.recordingDistanceIntervalMeters == recordingDistanceIntervalMeters)&&(identical(other.recordingTimeIntervalMillis, recordingTimeIntervalMillis) || other.recordingTimeIntervalMillis == recordingTimeIntervalMillis)&&(identical(other.voiceAnnouncementsEnabled, voiceAnnouncementsEnabled) || other.voiceAnnouncementsEnabled == voiceAnnouncementsEnabled)&&(identical(other.voiceAnnouncementTimeIntervalMinutes, voiceAnnouncementTimeIntervalMinutes) || other.voiceAnnouncementTimeIntervalMinutes == voiceAnnouncementTimeIntervalMinutes)&&(identical(other.voiceAnnouncementDistanceIntervalMeters, voiceAnnouncementDistanceIntervalMeters) || other.voiceAnnouncementDistanceIntervalMeters == voiceAnnouncementDistanceIntervalMeters)&&(identical(other.voiceIdleAnnouncementsEnabled, voiceIdleAnnouncementsEnabled) || other.voiceIdleAnnouncementsEnabled == voiceIdleAnnouncementsEnabled)&&(identical(other.voiceLapAnnouncementsEnabled, voiceLapAnnouncementsEnabled) || other.voiceLapAnnouncementsEnabled == voiceLapAnnouncementsEnabled)&&(identical(other.restTimerBellEnabled, restTimerBellEnabled) || other.restTimerBellEnabled == restTimerBellEnabled));
}


@override
int get hashCode => Object.hash(runtimeType,autoIdleEnabled,autoIdleTimeoutSeconds,keepScreenOnDuringRecording,requiredGpsAccuracyMeters,routeGapMeters,barometerClimbEnabled,recordingDistanceIntervalMeters,recordingTimeIntervalMillis,voiceAnnouncementsEnabled,voiceAnnouncementTimeIntervalMinutes,voiceAnnouncementDistanceIntervalMeters,voiceIdleAnnouncementsEnabled,voiceLapAnnouncementsEnabled,restTimerBellEnabled);

@override
String toString() {
  return 'ActivityRecordingPreferences(autoIdleEnabled: $autoIdleEnabled, autoIdleTimeoutSeconds: $autoIdleTimeoutSeconds, keepScreenOnDuringRecording: $keepScreenOnDuringRecording, requiredGpsAccuracyMeters: $requiredGpsAccuracyMeters, routeGapMeters: $routeGapMeters, barometerClimbEnabled: $barometerClimbEnabled, recordingDistanceIntervalMeters: $recordingDistanceIntervalMeters, recordingTimeIntervalMillis: $recordingTimeIntervalMillis, voiceAnnouncementsEnabled: $voiceAnnouncementsEnabled, voiceAnnouncementTimeIntervalMinutes: $voiceAnnouncementTimeIntervalMinutes, voiceAnnouncementDistanceIntervalMeters: $voiceAnnouncementDistanceIntervalMeters, voiceIdleAnnouncementsEnabled: $voiceIdleAnnouncementsEnabled, voiceLapAnnouncementsEnabled: $voiceLapAnnouncementsEnabled, restTimerBellEnabled: $restTimerBellEnabled)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingPreferencesCopyWith<$Res>  {
  factory $ActivityRecordingPreferencesCopyWith(ActivityRecordingPreferences value, $Res Function(ActivityRecordingPreferences) _then) = _$ActivityRecordingPreferencesCopyWithImpl;
@useResult
$Res call({
 bool autoIdleEnabled, int autoIdleTimeoutSeconds, bool keepScreenOnDuringRecording, int requiredGpsAccuracyMeters, int? routeGapMeters, bool barometerClimbEnabled, int? recordingDistanceIntervalMeters, int recordingTimeIntervalMillis, bool voiceAnnouncementsEnabled, int? voiceAnnouncementTimeIntervalMinutes, int? voiceAnnouncementDistanceIntervalMeters, bool voiceIdleAnnouncementsEnabled, bool voiceLapAnnouncementsEnabled, bool restTimerBellEnabled
});




}
/// @nodoc
class _$ActivityRecordingPreferencesCopyWithImpl<$Res>
    implements $ActivityRecordingPreferencesCopyWith<$Res> {
  _$ActivityRecordingPreferencesCopyWithImpl(this._self, this._then);

  final ActivityRecordingPreferences _self;
  final $Res Function(ActivityRecordingPreferences) _then;

/// Create a copy of ActivityRecordingPreferences
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? autoIdleEnabled = null,Object? autoIdleTimeoutSeconds = null,Object? keepScreenOnDuringRecording = null,Object? requiredGpsAccuracyMeters = null,Object? routeGapMeters = freezed,Object? barometerClimbEnabled = null,Object? recordingDistanceIntervalMeters = freezed,Object? recordingTimeIntervalMillis = null,Object? voiceAnnouncementsEnabled = null,Object? voiceAnnouncementTimeIntervalMinutes = freezed,Object? voiceAnnouncementDistanceIntervalMeters = freezed,Object? voiceIdleAnnouncementsEnabled = null,Object? voiceLapAnnouncementsEnabled = null,Object? restTimerBellEnabled = null,}) {
  return _then(_self.copyWith(
autoIdleEnabled: null == autoIdleEnabled ? _self.autoIdleEnabled : autoIdleEnabled // ignore: cast_nullable_to_non_nullable
as bool,autoIdleTimeoutSeconds: null == autoIdleTimeoutSeconds ? _self.autoIdleTimeoutSeconds : autoIdleTimeoutSeconds // ignore: cast_nullable_to_non_nullable
as int,keepScreenOnDuringRecording: null == keepScreenOnDuringRecording ? _self.keepScreenOnDuringRecording : keepScreenOnDuringRecording // ignore: cast_nullable_to_non_nullable
as bool,requiredGpsAccuracyMeters: null == requiredGpsAccuracyMeters ? _self.requiredGpsAccuracyMeters : requiredGpsAccuracyMeters // ignore: cast_nullable_to_non_nullable
as int,routeGapMeters: freezed == routeGapMeters ? _self.routeGapMeters : routeGapMeters // ignore: cast_nullable_to_non_nullable
as int?,barometerClimbEnabled: null == barometerClimbEnabled ? _self.barometerClimbEnabled : barometerClimbEnabled // ignore: cast_nullable_to_non_nullable
as bool,recordingDistanceIntervalMeters: freezed == recordingDistanceIntervalMeters ? _self.recordingDistanceIntervalMeters : recordingDistanceIntervalMeters // ignore: cast_nullable_to_non_nullable
as int?,recordingTimeIntervalMillis: null == recordingTimeIntervalMillis ? _self.recordingTimeIntervalMillis : recordingTimeIntervalMillis // ignore: cast_nullable_to_non_nullable
as int,voiceAnnouncementsEnabled: null == voiceAnnouncementsEnabled ? _self.voiceAnnouncementsEnabled : voiceAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,voiceAnnouncementTimeIntervalMinutes: freezed == voiceAnnouncementTimeIntervalMinutes ? _self.voiceAnnouncementTimeIntervalMinutes : voiceAnnouncementTimeIntervalMinutes // ignore: cast_nullable_to_non_nullable
as int?,voiceAnnouncementDistanceIntervalMeters: freezed == voiceAnnouncementDistanceIntervalMeters ? _self.voiceAnnouncementDistanceIntervalMeters : voiceAnnouncementDistanceIntervalMeters // ignore: cast_nullable_to_non_nullable
as int?,voiceIdleAnnouncementsEnabled: null == voiceIdleAnnouncementsEnabled ? _self.voiceIdleAnnouncementsEnabled : voiceIdleAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,voiceLapAnnouncementsEnabled: null == voiceLapAnnouncementsEnabled ? _self.voiceLapAnnouncementsEnabled : voiceLapAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,restTimerBellEnabled: null == restTimerBellEnabled ? _self.restTimerBellEnabled : restTimerBellEnabled // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityRecordingPreferences].
extension ActivityRecordingPreferencesPatterns on ActivityRecordingPreferences {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingPreferences value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingPreferences() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingPreferences value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingPreferences():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingPreferences value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingPreferences() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool autoIdleEnabled,  int autoIdleTimeoutSeconds,  bool keepScreenOnDuringRecording,  int requiredGpsAccuracyMeters,  int? routeGapMeters,  bool barometerClimbEnabled,  int? recordingDistanceIntervalMeters,  int recordingTimeIntervalMillis,  bool voiceAnnouncementsEnabled,  int? voiceAnnouncementTimeIntervalMinutes,  int? voiceAnnouncementDistanceIntervalMeters,  bool voiceIdleAnnouncementsEnabled,  bool voiceLapAnnouncementsEnabled,  bool restTimerBellEnabled)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingPreferences() when $default != null:
return $default(_that.autoIdleEnabled,_that.autoIdleTimeoutSeconds,_that.keepScreenOnDuringRecording,_that.requiredGpsAccuracyMeters,_that.routeGapMeters,_that.barometerClimbEnabled,_that.recordingDistanceIntervalMeters,_that.recordingTimeIntervalMillis,_that.voiceAnnouncementsEnabled,_that.voiceAnnouncementTimeIntervalMinutes,_that.voiceAnnouncementDistanceIntervalMeters,_that.voiceIdleAnnouncementsEnabled,_that.voiceLapAnnouncementsEnabled,_that.restTimerBellEnabled);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool autoIdleEnabled,  int autoIdleTimeoutSeconds,  bool keepScreenOnDuringRecording,  int requiredGpsAccuracyMeters,  int? routeGapMeters,  bool barometerClimbEnabled,  int? recordingDistanceIntervalMeters,  int recordingTimeIntervalMillis,  bool voiceAnnouncementsEnabled,  int? voiceAnnouncementTimeIntervalMinutes,  int? voiceAnnouncementDistanceIntervalMeters,  bool voiceIdleAnnouncementsEnabled,  bool voiceLapAnnouncementsEnabled,  bool restTimerBellEnabled)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingPreferences():
return $default(_that.autoIdleEnabled,_that.autoIdleTimeoutSeconds,_that.keepScreenOnDuringRecording,_that.requiredGpsAccuracyMeters,_that.routeGapMeters,_that.barometerClimbEnabled,_that.recordingDistanceIntervalMeters,_that.recordingTimeIntervalMillis,_that.voiceAnnouncementsEnabled,_that.voiceAnnouncementTimeIntervalMinutes,_that.voiceAnnouncementDistanceIntervalMeters,_that.voiceIdleAnnouncementsEnabled,_that.voiceLapAnnouncementsEnabled,_that.restTimerBellEnabled);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool autoIdleEnabled,  int autoIdleTimeoutSeconds,  bool keepScreenOnDuringRecording,  int requiredGpsAccuracyMeters,  int? routeGapMeters,  bool barometerClimbEnabled,  int? recordingDistanceIntervalMeters,  int recordingTimeIntervalMillis,  bool voiceAnnouncementsEnabled,  int? voiceAnnouncementTimeIntervalMinutes,  int? voiceAnnouncementDistanceIntervalMeters,  bool voiceIdleAnnouncementsEnabled,  bool voiceLapAnnouncementsEnabled,  bool restTimerBellEnabled)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingPreferences() when $default != null:
return $default(_that.autoIdleEnabled,_that.autoIdleTimeoutSeconds,_that.keepScreenOnDuringRecording,_that.requiredGpsAccuracyMeters,_that.routeGapMeters,_that.barometerClimbEnabled,_that.recordingDistanceIntervalMeters,_that.recordingTimeIntervalMillis,_that.voiceAnnouncementsEnabled,_that.voiceAnnouncementTimeIntervalMinutes,_that.voiceAnnouncementDistanceIntervalMeters,_that.voiceIdleAnnouncementsEnabled,_that.voiceLapAnnouncementsEnabled,_that.restTimerBellEnabled);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingPreferences extends ActivityRecordingPreferences {
  const _ActivityRecordingPreferences({this.autoIdleEnabled = ActivityRecordingPreferences.defaultAutoIdleEnabled, this.autoIdleTimeoutSeconds = ActivityRecordingPreferences.defaultAutoIdleTimeoutSeconds, this.keepScreenOnDuringRecording = ActivityRecordingPreferences.defaultKeepScreenOnDuringRecording, this.requiredGpsAccuracyMeters = ActivityRecordingPreferences.defaultRequiredGpsAccuracyMeters, this.routeGapMeters = ActivityRecordingPreferences.defaultRouteGapMeters, this.barometerClimbEnabled = ActivityRecordingPreferences.defaultBarometerClimbEnabled, this.recordingDistanceIntervalMeters, this.recordingTimeIntervalMillis = ActivityRecordingPreferences.defaultRecordingTimeIntervalMillis, this.voiceAnnouncementsEnabled = ActivityRecordingPreferences.defaultVoiceAnnouncementsEnabled, this.voiceAnnouncementTimeIntervalMinutes = ActivityRecordingPreferences.defaultVoiceAnnouncementTimeIntervalMinutes, this.voiceAnnouncementDistanceIntervalMeters = ActivityRecordingPreferences.defaultVoiceAnnouncementDistanceIntervalMeters, this.voiceIdleAnnouncementsEnabled = ActivityRecordingPreferences.defaultVoiceIdleAnnouncementsEnabled, this.voiceLapAnnouncementsEnabled = ActivityRecordingPreferences.defaultVoiceLapAnnouncementsEnabled, this.restTimerBellEnabled = ActivityRecordingPreferences.defaultRestTimerBellEnabled}): super._();
  

@override@JsonKey() final  bool autoIdleEnabled;
@override@JsonKey() final  int autoIdleTimeoutSeconds;
@override@JsonKey() final  bool keepScreenOnDuringRecording;
@override@JsonKey() final  int requiredGpsAccuracyMeters;
@override@JsonKey() final  int? routeGapMeters;
@override@JsonKey() final  bool barometerClimbEnabled;
@override final  int? recordingDistanceIntervalMeters;
@override@JsonKey() final  int recordingTimeIntervalMillis;
@override@JsonKey() final  bool voiceAnnouncementsEnabled;
@override@JsonKey() final  int? voiceAnnouncementTimeIntervalMinutes;
@override@JsonKey() final  int? voiceAnnouncementDistanceIntervalMeters;
@override@JsonKey() final  bool voiceIdleAnnouncementsEnabled;
@override@JsonKey() final  bool voiceLapAnnouncementsEnabled;
@override@JsonKey() final  bool restTimerBellEnabled;

/// Create a copy of ActivityRecordingPreferences
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingPreferencesCopyWith<_ActivityRecordingPreferences> get copyWith => __$ActivityRecordingPreferencesCopyWithImpl<_ActivityRecordingPreferences>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingPreferences&&(identical(other.autoIdleEnabled, autoIdleEnabled) || other.autoIdleEnabled == autoIdleEnabled)&&(identical(other.autoIdleTimeoutSeconds, autoIdleTimeoutSeconds) || other.autoIdleTimeoutSeconds == autoIdleTimeoutSeconds)&&(identical(other.keepScreenOnDuringRecording, keepScreenOnDuringRecording) || other.keepScreenOnDuringRecording == keepScreenOnDuringRecording)&&(identical(other.requiredGpsAccuracyMeters, requiredGpsAccuracyMeters) || other.requiredGpsAccuracyMeters == requiredGpsAccuracyMeters)&&(identical(other.routeGapMeters, routeGapMeters) || other.routeGapMeters == routeGapMeters)&&(identical(other.barometerClimbEnabled, barometerClimbEnabled) || other.barometerClimbEnabled == barometerClimbEnabled)&&(identical(other.recordingDistanceIntervalMeters, recordingDistanceIntervalMeters) || other.recordingDistanceIntervalMeters == recordingDistanceIntervalMeters)&&(identical(other.recordingTimeIntervalMillis, recordingTimeIntervalMillis) || other.recordingTimeIntervalMillis == recordingTimeIntervalMillis)&&(identical(other.voiceAnnouncementsEnabled, voiceAnnouncementsEnabled) || other.voiceAnnouncementsEnabled == voiceAnnouncementsEnabled)&&(identical(other.voiceAnnouncementTimeIntervalMinutes, voiceAnnouncementTimeIntervalMinutes) || other.voiceAnnouncementTimeIntervalMinutes == voiceAnnouncementTimeIntervalMinutes)&&(identical(other.voiceAnnouncementDistanceIntervalMeters, voiceAnnouncementDistanceIntervalMeters) || other.voiceAnnouncementDistanceIntervalMeters == voiceAnnouncementDistanceIntervalMeters)&&(identical(other.voiceIdleAnnouncementsEnabled, voiceIdleAnnouncementsEnabled) || other.voiceIdleAnnouncementsEnabled == voiceIdleAnnouncementsEnabled)&&(identical(other.voiceLapAnnouncementsEnabled, voiceLapAnnouncementsEnabled) || other.voiceLapAnnouncementsEnabled == voiceLapAnnouncementsEnabled)&&(identical(other.restTimerBellEnabled, restTimerBellEnabled) || other.restTimerBellEnabled == restTimerBellEnabled));
}


@override
int get hashCode => Object.hash(runtimeType,autoIdleEnabled,autoIdleTimeoutSeconds,keepScreenOnDuringRecording,requiredGpsAccuracyMeters,routeGapMeters,barometerClimbEnabled,recordingDistanceIntervalMeters,recordingTimeIntervalMillis,voiceAnnouncementsEnabled,voiceAnnouncementTimeIntervalMinutes,voiceAnnouncementDistanceIntervalMeters,voiceIdleAnnouncementsEnabled,voiceLapAnnouncementsEnabled,restTimerBellEnabled);

@override
String toString() {
  return 'ActivityRecordingPreferences(autoIdleEnabled: $autoIdleEnabled, autoIdleTimeoutSeconds: $autoIdleTimeoutSeconds, keepScreenOnDuringRecording: $keepScreenOnDuringRecording, requiredGpsAccuracyMeters: $requiredGpsAccuracyMeters, routeGapMeters: $routeGapMeters, barometerClimbEnabled: $barometerClimbEnabled, recordingDistanceIntervalMeters: $recordingDistanceIntervalMeters, recordingTimeIntervalMillis: $recordingTimeIntervalMillis, voiceAnnouncementsEnabled: $voiceAnnouncementsEnabled, voiceAnnouncementTimeIntervalMinutes: $voiceAnnouncementTimeIntervalMinutes, voiceAnnouncementDistanceIntervalMeters: $voiceAnnouncementDistanceIntervalMeters, voiceIdleAnnouncementsEnabled: $voiceIdleAnnouncementsEnabled, voiceLapAnnouncementsEnabled: $voiceLapAnnouncementsEnabled, restTimerBellEnabled: $restTimerBellEnabled)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingPreferencesCopyWith<$Res> implements $ActivityRecordingPreferencesCopyWith<$Res> {
  factory _$ActivityRecordingPreferencesCopyWith(_ActivityRecordingPreferences value, $Res Function(_ActivityRecordingPreferences) _then) = __$ActivityRecordingPreferencesCopyWithImpl;
@override @useResult
$Res call({
 bool autoIdleEnabled, int autoIdleTimeoutSeconds, bool keepScreenOnDuringRecording, int requiredGpsAccuracyMeters, int? routeGapMeters, bool barometerClimbEnabled, int? recordingDistanceIntervalMeters, int recordingTimeIntervalMillis, bool voiceAnnouncementsEnabled, int? voiceAnnouncementTimeIntervalMinutes, int? voiceAnnouncementDistanceIntervalMeters, bool voiceIdleAnnouncementsEnabled, bool voiceLapAnnouncementsEnabled, bool restTimerBellEnabled
});




}
/// @nodoc
class __$ActivityRecordingPreferencesCopyWithImpl<$Res>
    implements _$ActivityRecordingPreferencesCopyWith<$Res> {
  __$ActivityRecordingPreferencesCopyWithImpl(this._self, this._then);

  final _ActivityRecordingPreferences _self;
  final $Res Function(_ActivityRecordingPreferences) _then;

/// Create a copy of ActivityRecordingPreferences
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? autoIdleEnabled = null,Object? autoIdleTimeoutSeconds = null,Object? keepScreenOnDuringRecording = null,Object? requiredGpsAccuracyMeters = null,Object? routeGapMeters = freezed,Object? barometerClimbEnabled = null,Object? recordingDistanceIntervalMeters = freezed,Object? recordingTimeIntervalMillis = null,Object? voiceAnnouncementsEnabled = null,Object? voiceAnnouncementTimeIntervalMinutes = freezed,Object? voiceAnnouncementDistanceIntervalMeters = freezed,Object? voiceIdleAnnouncementsEnabled = null,Object? voiceLapAnnouncementsEnabled = null,Object? restTimerBellEnabled = null,}) {
  return _then(_ActivityRecordingPreferences(
autoIdleEnabled: null == autoIdleEnabled ? _self.autoIdleEnabled : autoIdleEnabled // ignore: cast_nullable_to_non_nullable
as bool,autoIdleTimeoutSeconds: null == autoIdleTimeoutSeconds ? _self.autoIdleTimeoutSeconds : autoIdleTimeoutSeconds // ignore: cast_nullable_to_non_nullable
as int,keepScreenOnDuringRecording: null == keepScreenOnDuringRecording ? _self.keepScreenOnDuringRecording : keepScreenOnDuringRecording // ignore: cast_nullable_to_non_nullable
as bool,requiredGpsAccuracyMeters: null == requiredGpsAccuracyMeters ? _self.requiredGpsAccuracyMeters : requiredGpsAccuracyMeters // ignore: cast_nullable_to_non_nullable
as int,routeGapMeters: freezed == routeGapMeters ? _self.routeGapMeters : routeGapMeters // ignore: cast_nullable_to_non_nullable
as int?,barometerClimbEnabled: null == barometerClimbEnabled ? _self.barometerClimbEnabled : barometerClimbEnabled // ignore: cast_nullable_to_non_nullable
as bool,recordingDistanceIntervalMeters: freezed == recordingDistanceIntervalMeters ? _self.recordingDistanceIntervalMeters : recordingDistanceIntervalMeters // ignore: cast_nullable_to_non_nullable
as int?,recordingTimeIntervalMillis: null == recordingTimeIntervalMillis ? _self.recordingTimeIntervalMillis : recordingTimeIntervalMillis // ignore: cast_nullable_to_non_nullable
as int,voiceAnnouncementsEnabled: null == voiceAnnouncementsEnabled ? _self.voiceAnnouncementsEnabled : voiceAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,voiceAnnouncementTimeIntervalMinutes: freezed == voiceAnnouncementTimeIntervalMinutes ? _self.voiceAnnouncementTimeIntervalMinutes : voiceAnnouncementTimeIntervalMinutes // ignore: cast_nullable_to_non_nullable
as int?,voiceAnnouncementDistanceIntervalMeters: freezed == voiceAnnouncementDistanceIntervalMeters ? _self.voiceAnnouncementDistanceIntervalMeters : voiceAnnouncementDistanceIntervalMeters // ignore: cast_nullable_to_non_nullable
as int?,voiceIdleAnnouncementsEnabled: null == voiceIdleAnnouncementsEnabled ? _self.voiceIdleAnnouncementsEnabled : voiceIdleAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,voiceLapAnnouncementsEnabled: null == voiceLapAnnouncementsEnabled ? _self.voiceLapAnnouncementsEnabled : voiceLapAnnouncementsEnabled // ignore: cast_nullable_to_non_nullable
as bool,restTimerBellEnabled: null == restTimerBellEnabled ? _self.restTimerBellEnabled : restTimerBellEnabled // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
