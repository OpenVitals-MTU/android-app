// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mindfulness_entry_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$MindfulnessEntryState {

 String get durationMinutesText; bool get intervalEnabled; String get intervalMinutesText; MindfulnessBellSound get bellSound; MindfulnessBackgroundSound get backgroundSound; String get manualMinutesText; Set<String> get writePermissions; bool get canWrite; bool get mindfulnessAvailable; bool get isCheckingPermission; bool get isSavingEntry; bool get isTimerRunning; bool get isTimerPaused; bool get timerCompleted; int get remainingSeconds; int get totalSeconds; String? get editRecordId; DateTime? get editStartTime; bool get saveCompleted; MindfulnessEntryError? get entryError; ScreenError? get writeError; MindfulnessBellEvent? get bellEvent; MindfulnessBackgroundEvent? get backgroundEvent;
/// Create a copy of MindfulnessEntryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessEntryStateCopyWith<MindfulnessEntryState> get copyWith => _$MindfulnessEntryStateCopyWithImpl<MindfulnessEntryState>(this as MindfulnessEntryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessEntryState&&(identical(other.durationMinutesText, durationMinutesText) || other.durationMinutesText == durationMinutesText)&&(identical(other.intervalEnabled, intervalEnabled) || other.intervalEnabled == intervalEnabled)&&(identical(other.intervalMinutesText, intervalMinutesText) || other.intervalMinutesText == intervalMinutesText)&&(identical(other.bellSound, bellSound) || other.bellSound == bellSound)&&(identical(other.backgroundSound, backgroundSound) || other.backgroundSound == backgroundSound)&&(identical(other.manualMinutesText, manualMinutesText) || other.manualMinutesText == manualMinutesText)&&const DeepCollectionEquality().equals(other.writePermissions, writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.mindfulnessAvailable, mindfulnessAvailable) || other.mindfulnessAvailable == mindfulnessAvailable)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&(identical(other.isTimerRunning, isTimerRunning) || other.isTimerRunning == isTimerRunning)&&(identical(other.isTimerPaused, isTimerPaused) || other.isTimerPaused == isTimerPaused)&&(identical(other.timerCompleted, timerCompleted) || other.timerCompleted == timerCompleted)&&(identical(other.remainingSeconds, remainingSeconds) || other.remainingSeconds == remainingSeconds)&&(identical(other.totalSeconds, totalSeconds) || other.totalSeconds == totalSeconds)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editStartTime, editStartTime) || other.editStartTime == editStartTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError)&&(identical(other.bellEvent, bellEvent) || other.bellEvent == bellEvent)&&(identical(other.backgroundEvent, backgroundEvent) || other.backgroundEvent == backgroundEvent));
}


@override
int get hashCode => Object.hashAll([runtimeType,durationMinutesText,intervalEnabled,intervalMinutesText,bellSound,backgroundSound,manualMinutesText,const DeepCollectionEquality().hash(writePermissions),canWrite,mindfulnessAvailable,isCheckingPermission,isSavingEntry,isTimerRunning,isTimerPaused,timerCompleted,remainingSeconds,totalSeconds,editRecordId,editStartTime,saveCompleted,entryError,writeError,bellEvent,backgroundEvent]);

@override
String toString() {
  return 'MindfulnessEntryState(durationMinutesText: $durationMinutesText, intervalEnabled: $intervalEnabled, intervalMinutesText: $intervalMinutesText, bellSound: $bellSound, backgroundSound: $backgroundSound, manualMinutesText: $manualMinutesText, writePermissions: $writePermissions, canWrite: $canWrite, mindfulnessAvailable: $mindfulnessAvailable, isCheckingPermission: $isCheckingPermission, isSavingEntry: $isSavingEntry, isTimerRunning: $isTimerRunning, isTimerPaused: $isTimerPaused, timerCompleted: $timerCompleted, remainingSeconds: $remainingSeconds, totalSeconds: $totalSeconds, editRecordId: $editRecordId, editStartTime: $editStartTime, saveCompleted: $saveCompleted, entryError: $entryError, writeError: $writeError, bellEvent: $bellEvent, backgroundEvent: $backgroundEvent)';
}


}

/// @nodoc
abstract mixin class $MindfulnessEntryStateCopyWith<$Res>  {
  factory $MindfulnessEntryStateCopyWith(MindfulnessEntryState value, $Res Function(MindfulnessEntryState) _then) = _$MindfulnessEntryStateCopyWithImpl;
@useResult
$Res call({
 String durationMinutesText, bool intervalEnabled, String intervalMinutesText, MindfulnessBellSound bellSound, MindfulnessBackgroundSound backgroundSound, String manualMinutesText, Set<String> writePermissions, bool canWrite, bool mindfulnessAvailable, bool isCheckingPermission, bool isSavingEntry, bool isTimerRunning, bool isTimerPaused, bool timerCompleted, int remainingSeconds, int totalSeconds, String? editRecordId, DateTime? editStartTime, bool saveCompleted, MindfulnessEntryError? entryError, ScreenError? writeError, MindfulnessBellEvent? bellEvent, MindfulnessBackgroundEvent? backgroundEvent
});




}
/// @nodoc
class _$MindfulnessEntryStateCopyWithImpl<$Res>
    implements $MindfulnessEntryStateCopyWith<$Res> {
  _$MindfulnessEntryStateCopyWithImpl(this._self, this._then);

  final MindfulnessEntryState _self;
  final $Res Function(MindfulnessEntryState) _then;

/// Create a copy of MindfulnessEntryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? durationMinutesText = null,Object? intervalEnabled = null,Object? intervalMinutesText = null,Object? bellSound = null,Object? backgroundSound = null,Object? manualMinutesText = null,Object? writePermissions = null,Object? canWrite = null,Object? mindfulnessAvailable = null,Object? isCheckingPermission = null,Object? isSavingEntry = null,Object? isTimerRunning = null,Object? isTimerPaused = null,Object? timerCompleted = null,Object? remainingSeconds = null,Object? totalSeconds = null,Object? editRecordId = freezed,Object? editStartTime = freezed,Object? saveCompleted = null,Object? entryError = freezed,Object? writeError = freezed,Object? bellEvent = freezed,Object? backgroundEvent = freezed,}) {
  return _then(_self.copyWith(
durationMinutesText: null == durationMinutesText ? _self.durationMinutesText : durationMinutesText // ignore: cast_nullable_to_non_nullable
as String,intervalEnabled: null == intervalEnabled ? _self.intervalEnabled : intervalEnabled // ignore: cast_nullable_to_non_nullable
as bool,intervalMinutesText: null == intervalMinutesText ? _self.intervalMinutesText : intervalMinutesText // ignore: cast_nullable_to_non_nullable
as String,bellSound: null == bellSound ? _self.bellSound : bellSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBellSound,backgroundSound: null == backgroundSound ? _self.backgroundSound : backgroundSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundSound,manualMinutesText: null == manualMinutesText ? _self.manualMinutesText : manualMinutesText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self.writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,mindfulnessAvailable: null == mindfulnessAvailable ? _self.mindfulnessAvailable : mindfulnessAvailable // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,isTimerRunning: null == isTimerRunning ? _self.isTimerRunning : isTimerRunning // ignore: cast_nullable_to_non_nullable
as bool,isTimerPaused: null == isTimerPaused ? _self.isTimerPaused : isTimerPaused // ignore: cast_nullable_to_non_nullable
as bool,timerCompleted: null == timerCompleted ? _self.timerCompleted : timerCompleted // ignore: cast_nullable_to_non_nullable
as bool,remainingSeconds: null == remainingSeconds ? _self.remainingSeconds : remainingSeconds // ignore: cast_nullable_to_non_nullable
as int,totalSeconds: null == totalSeconds ? _self.totalSeconds : totalSeconds // ignore: cast_nullable_to_non_nullable
as int,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editStartTime: freezed == editStartTime ? _self.editStartTime : editStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as MindfulnessEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,bellEvent: freezed == bellEvent ? _self.bellEvent : bellEvent // ignore: cast_nullable_to_non_nullable
as MindfulnessBellEvent?,backgroundEvent: freezed == backgroundEvent ? _self.backgroundEvent : backgroundEvent // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundEvent?,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessEntryState].
extension MindfulnessEntryStatePatterns on MindfulnessEntryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessEntryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessEntryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessEntryState value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessEntryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessEntryState value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessEntryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String durationMinutesText,  bool intervalEnabled,  String intervalMinutesText,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound,  String manualMinutesText,  Set<String> writePermissions,  bool canWrite,  bool mindfulnessAvailable,  bool isCheckingPermission,  bool isSavingEntry,  bool isTimerRunning,  bool isTimerPaused,  bool timerCompleted,  int remainingSeconds,  int totalSeconds,  String? editRecordId,  DateTime? editStartTime,  bool saveCompleted,  MindfulnessEntryError? entryError,  ScreenError? writeError,  MindfulnessBellEvent? bellEvent,  MindfulnessBackgroundEvent? backgroundEvent)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessEntryState() when $default != null:
return $default(_that.durationMinutesText,_that.intervalEnabled,_that.intervalMinutesText,_that.bellSound,_that.backgroundSound,_that.manualMinutesText,_that.writePermissions,_that.canWrite,_that.mindfulnessAvailable,_that.isCheckingPermission,_that.isSavingEntry,_that.isTimerRunning,_that.isTimerPaused,_that.timerCompleted,_that.remainingSeconds,_that.totalSeconds,_that.editRecordId,_that.editStartTime,_that.saveCompleted,_that.entryError,_that.writeError,_that.bellEvent,_that.backgroundEvent);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String durationMinutesText,  bool intervalEnabled,  String intervalMinutesText,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound,  String manualMinutesText,  Set<String> writePermissions,  bool canWrite,  bool mindfulnessAvailable,  bool isCheckingPermission,  bool isSavingEntry,  bool isTimerRunning,  bool isTimerPaused,  bool timerCompleted,  int remainingSeconds,  int totalSeconds,  String? editRecordId,  DateTime? editStartTime,  bool saveCompleted,  MindfulnessEntryError? entryError,  ScreenError? writeError,  MindfulnessBellEvent? bellEvent,  MindfulnessBackgroundEvent? backgroundEvent)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessEntryState():
return $default(_that.durationMinutesText,_that.intervalEnabled,_that.intervalMinutesText,_that.bellSound,_that.backgroundSound,_that.manualMinutesText,_that.writePermissions,_that.canWrite,_that.mindfulnessAvailable,_that.isCheckingPermission,_that.isSavingEntry,_that.isTimerRunning,_that.isTimerPaused,_that.timerCompleted,_that.remainingSeconds,_that.totalSeconds,_that.editRecordId,_that.editStartTime,_that.saveCompleted,_that.entryError,_that.writeError,_that.bellEvent,_that.backgroundEvent);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String durationMinutesText,  bool intervalEnabled,  String intervalMinutesText,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound,  String manualMinutesText,  Set<String> writePermissions,  bool canWrite,  bool mindfulnessAvailable,  bool isCheckingPermission,  bool isSavingEntry,  bool isTimerRunning,  bool isTimerPaused,  bool timerCompleted,  int remainingSeconds,  int totalSeconds,  String? editRecordId,  DateTime? editStartTime,  bool saveCompleted,  MindfulnessEntryError? entryError,  ScreenError? writeError,  MindfulnessBellEvent? bellEvent,  MindfulnessBackgroundEvent? backgroundEvent)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessEntryState() when $default != null:
return $default(_that.durationMinutesText,_that.intervalEnabled,_that.intervalMinutesText,_that.bellSound,_that.backgroundSound,_that.manualMinutesText,_that.writePermissions,_that.canWrite,_that.mindfulnessAvailable,_that.isCheckingPermission,_that.isSavingEntry,_that.isTimerRunning,_that.isTimerPaused,_that.timerCompleted,_that.remainingSeconds,_that.totalSeconds,_that.editRecordId,_that.editStartTime,_that.saveCompleted,_that.entryError,_that.writeError,_that.bellEvent,_that.backgroundEvent);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessEntryState extends MindfulnessEntryState {
  const _MindfulnessEntryState({this.durationMinutesText = '', this.intervalEnabled = false, this.intervalMinutesText = '', this.bellSound = MindfulnessBellSound.struck, this.backgroundSound = MindfulnessBackgroundSound.none, this.manualMinutesText = '', final  Set<String> writePermissions = const <String>{}, this.canWrite = false, this.mindfulnessAvailable = true, this.isCheckingPermission = true, this.isSavingEntry = false, this.isTimerRunning = false, this.isTimerPaused = false, this.timerCompleted = false, this.remainingSeconds = 0, this.totalSeconds = 0, this.editRecordId, this.editStartTime, this.saveCompleted = false, this.entryError, this.writeError, this.bellEvent, this.backgroundEvent}): _writePermissions = writePermissions,super._();
  

@override@JsonKey() final  String durationMinutesText;
@override@JsonKey() final  bool intervalEnabled;
@override@JsonKey() final  String intervalMinutesText;
@override@JsonKey() final  MindfulnessBellSound bellSound;
@override@JsonKey() final  MindfulnessBackgroundSound backgroundSound;
@override@JsonKey() final  String manualMinutesText;
 final  Set<String> _writePermissions;
@override@JsonKey() Set<String> get writePermissions {
  if (_writePermissions is EqualUnmodifiableSetView) return _writePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_writePermissions);
}

@override@JsonKey() final  bool canWrite;
@override@JsonKey() final  bool mindfulnessAvailable;
@override@JsonKey() final  bool isCheckingPermission;
@override@JsonKey() final  bool isSavingEntry;
@override@JsonKey() final  bool isTimerRunning;
@override@JsonKey() final  bool isTimerPaused;
@override@JsonKey() final  bool timerCompleted;
@override@JsonKey() final  int remainingSeconds;
@override@JsonKey() final  int totalSeconds;
@override final  String? editRecordId;
@override final  DateTime? editStartTime;
@override@JsonKey() final  bool saveCompleted;
@override final  MindfulnessEntryError? entryError;
@override final  ScreenError? writeError;
@override final  MindfulnessBellEvent? bellEvent;
@override final  MindfulnessBackgroundEvent? backgroundEvent;

/// Create a copy of MindfulnessEntryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessEntryStateCopyWith<_MindfulnessEntryState> get copyWith => __$MindfulnessEntryStateCopyWithImpl<_MindfulnessEntryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessEntryState&&(identical(other.durationMinutesText, durationMinutesText) || other.durationMinutesText == durationMinutesText)&&(identical(other.intervalEnabled, intervalEnabled) || other.intervalEnabled == intervalEnabled)&&(identical(other.intervalMinutesText, intervalMinutesText) || other.intervalMinutesText == intervalMinutesText)&&(identical(other.bellSound, bellSound) || other.bellSound == bellSound)&&(identical(other.backgroundSound, backgroundSound) || other.backgroundSound == backgroundSound)&&(identical(other.manualMinutesText, manualMinutesText) || other.manualMinutesText == manualMinutesText)&&const DeepCollectionEquality().equals(other._writePermissions, _writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.mindfulnessAvailable, mindfulnessAvailable) || other.mindfulnessAvailable == mindfulnessAvailable)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&(identical(other.isTimerRunning, isTimerRunning) || other.isTimerRunning == isTimerRunning)&&(identical(other.isTimerPaused, isTimerPaused) || other.isTimerPaused == isTimerPaused)&&(identical(other.timerCompleted, timerCompleted) || other.timerCompleted == timerCompleted)&&(identical(other.remainingSeconds, remainingSeconds) || other.remainingSeconds == remainingSeconds)&&(identical(other.totalSeconds, totalSeconds) || other.totalSeconds == totalSeconds)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editStartTime, editStartTime) || other.editStartTime == editStartTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError)&&(identical(other.bellEvent, bellEvent) || other.bellEvent == bellEvent)&&(identical(other.backgroundEvent, backgroundEvent) || other.backgroundEvent == backgroundEvent));
}


@override
int get hashCode => Object.hashAll([runtimeType,durationMinutesText,intervalEnabled,intervalMinutesText,bellSound,backgroundSound,manualMinutesText,const DeepCollectionEquality().hash(_writePermissions),canWrite,mindfulnessAvailable,isCheckingPermission,isSavingEntry,isTimerRunning,isTimerPaused,timerCompleted,remainingSeconds,totalSeconds,editRecordId,editStartTime,saveCompleted,entryError,writeError,bellEvent,backgroundEvent]);

@override
String toString() {
  return 'MindfulnessEntryState(durationMinutesText: $durationMinutesText, intervalEnabled: $intervalEnabled, intervalMinutesText: $intervalMinutesText, bellSound: $bellSound, backgroundSound: $backgroundSound, manualMinutesText: $manualMinutesText, writePermissions: $writePermissions, canWrite: $canWrite, mindfulnessAvailable: $mindfulnessAvailable, isCheckingPermission: $isCheckingPermission, isSavingEntry: $isSavingEntry, isTimerRunning: $isTimerRunning, isTimerPaused: $isTimerPaused, timerCompleted: $timerCompleted, remainingSeconds: $remainingSeconds, totalSeconds: $totalSeconds, editRecordId: $editRecordId, editStartTime: $editStartTime, saveCompleted: $saveCompleted, entryError: $entryError, writeError: $writeError, bellEvent: $bellEvent, backgroundEvent: $backgroundEvent)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessEntryStateCopyWith<$Res> implements $MindfulnessEntryStateCopyWith<$Res> {
  factory _$MindfulnessEntryStateCopyWith(_MindfulnessEntryState value, $Res Function(_MindfulnessEntryState) _then) = __$MindfulnessEntryStateCopyWithImpl;
@override @useResult
$Res call({
 String durationMinutesText, bool intervalEnabled, String intervalMinutesText, MindfulnessBellSound bellSound, MindfulnessBackgroundSound backgroundSound, String manualMinutesText, Set<String> writePermissions, bool canWrite, bool mindfulnessAvailable, bool isCheckingPermission, bool isSavingEntry, bool isTimerRunning, bool isTimerPaused, bool timerCompleted, int remainingSeconds, int totalSeconds, String? editRecordId, DateTime? editStartTime, bool saveCompleted, MindfulnessEntryError? entryError, ScreenError? writeError, MindfulnessBellEvent? bellEvent, MindfulnessBackgroundEvent? backgroundEvent
});




}
/// @nodoc
class __$MindfulnessEntryStateCopyWithImpl<$Res>
    implements _$MindfulnessEntryStateCopyWith<$Res> {
  __$MindfulnessEntryStateCopyWithImpl(this._self, this._then);

  final _MindfulnessEntryState _self;
  final $Res Function(_MindfulnessEntryState) _then;

/// Create a copy of MindfulnessEntryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? durationMinutesText = null,Object? intervalEnabled = null,Object? intervalMinutesText = null,Object? bellSound = null,Object? backgroundSound = null,Object? manualMinutesText = null,Object? writePermissions = null,Object? canWrite = null,Object? mindfulnessAvailable = null,Object? isCheckingPermission = null,Object? isSavingEntry = null,Object? isTimerRunning = null,Object? isTimerPaused = null,Object? timerCompleted = null,Object? remainingSeconds = null,Object? totalSeconds = null,Object? editRecordId = freezed,Object? editStartTime = freezed,Object? saveCompleted = null,Object? entryError = freezed,Object? writeError = freezed,Object? bellEvent = freezed,Object? backgroundEvent = freezed,}) {
  return _then(_MindfulnessEntryState(
durationMinutesText: null == durationMinutesText ? _self.durationMinutesText : durationMinutesText // ignore: cast_nullable_to_non_nullable
as String,intervalEnabled: null == intervalEnabled ? _self.intervalEnabled : intervalEnabled // ignore: cast_nullable_to_non_nullable
as bool,intervalMinutesText: null == intervalMinutesText ? _self.intervalMinutesText : intervalMinutesText // ignore: cast_nullable_to_non_nullable
as String,bellSound: null == bellSound ? _self.bellSound : bellSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBellSound,backgroundSound: null == backgroundSound ? _self.backgroundSound : backgroundSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundSound,manualMinutesText: null == manualMinutesText ? _self.manualMinutesText : manualMinutesText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self._writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,mindfulnessAvailable: null == mindfulnessAvailable ? _self.mindfulnessAvailable : mindfulnessAvailable // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,isTimerRunning: null == isTimerRunning ? _self.isTimerRunning : isTimerRunning // ignore: cast_nullable_to_non_nullable
as bool,isTimerPaused: null == isTimerPaused ? _self.isTimerPaused : isTimerPaused // ignore: cast_nullable_to_non_nullable
as bool,timerCompleted: null == timerCompleted ? _self.timerCompleted : timerCompleted // ignore: cast_nullable_to_non_nullable
as bool,remainingSeconds: null == remainingSeconds ? _self.remainingSeconds : remainingSeconds // ignore: cast_nullable_to_non_nullable
as int,totalSeconds: null == totalSeconds ? _self.totalSeconds : totalSeconds // ignore: cast_nullable_to_non_nullable
as int,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editStartTime: freezed == editStartTime ? _self.editStartTime : editStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as MindfulnessEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,bellEvent: freezed == bellEvent ? _self.bellEvent : bellEvent // ignore: cast_nullable_to_non_nullable
as MindfulnessBellEvent?,backgroundEvent: freezed == backgroundEvent ? _self.backgroundEvent : backgroundEvent // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundEvent?,
  ));
}


}

// dart format on
