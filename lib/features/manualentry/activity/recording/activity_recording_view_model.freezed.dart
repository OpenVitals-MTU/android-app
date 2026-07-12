// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_recording_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityRecordingUiState {

/// The last state published by the recording service.
 ActivityRecordingState get recording;/// The clock the elapsed/moving durations are read against. Ticks once a
/// second while the session is active, and is otherwise frozen — a widget
/// reading `DateTime.now()` itself would rebuild against a clock the state
/// has not seen.
 DateTime? get now;/// Focus mode is a property of the SCREEN, not of the recording: it must
/// survive a rebuild but not a process death, and the host drops its app
/// bar for it.
 bool get isFocusMode;/// Starting a recording is failable — permissions, a missing GPS fix, an
/// unsupported activity type — and the failure is the user's to see.
 CommandState<void> get start;/// Stopping a recording hands back the snapshot that becomes the entry
/// form's draft. A stop with no active session fails rather than silently
/// producing nothing.
 CommandState<ActivityRecordingSnapshot> get save;
/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingUiStateCopyWith<ActivityRecordingUiState> get copyWith => _$ActivityRecordingUiStateCopyWithImpl<ActivityRecordingUiState>(this as ActivityRecordingUiState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingUiState&&(identical(other.recording, recording) || other.recording == recording)&&(identical(other.now, now) || other.now == now)&&(identical(other.isFocusMode, isFocusMode) || other.isFocusMode == isFocusMode)&&(identical(other.start, start) || other.start == start)&&(identical(other.save, save) || other.save == save));
}


@override
int get hashCode => Object.hash(runtimeType,recording,now,isFocusMode,start,save);

@override
String toString() {
  return 'ActivityRecordingUiState(recording: $recording, now: $now, isFocusMode: $isFocusMode, start: $start, save: $save)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingUiStateCopyWith<$Res>  {
  factory $ActivityRecordingUiStateCopyWith(ActivityRecordingUiState value, $Res Function(ActivityRecordingUiState) _then) = _$ActivityRecordingUiStateCopyWithImpl;
@useResult
$Res call({
 ActivityRecordingState recording, DateTime? now, bool isFocusMode, CommandState<void> start, CommandState<ActivityRecordingSnapshot> save
});


$ActivityRecordingStateCopyWith<$Res> get recording;$CommandStateCopyWith<void, $Res> get start;$CommandStateCopyWith<ActivityRecordingSnapshot, $Res> get save;

}
/// @nodoc
class _$ActivityRecordingUiStateCopyWithImpl<$Res>
    implements $ActivityRecordingUiStateCopyWith<$Res> {
  _$ActivityRecordingUiStateCopyWithImpl(this._self, this._then);

  final ActivityRecordingUiState _self;
  final $Res Function(ActivityRecordingUiState) _then;

/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? recording = null,Object? now = freezed,Object? isFocusMode = null,Object? start = null,Object? save = null,}) {
  return _then(_self.copyWith(
recording: null == recording ? _self.recording : recording // ignore: cast_nullable_to_non_nullable
as ActivityRecordingState,now: freezed == now ? _self.now : now // ignore: cast_nullable_to_non_nullable
as DateTime?,isFocusMode: null == isFocusMode ? _self.isFocusMode : isFocusMode // ignore: cast_nullable_to_non_nullable
as bool,start: null == start ? _self.start : start // ignore: cast_nullable_to_non_nullable
as CommandState<void>,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<ActivityRecordingSnapshot>,
  ));
}
/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityRecordingStateCopyWith<$Res> get recording {
  
  return $ActivityRecordingStateCopyWith<$Res>(_self.recording, (value) {
    return _then(_self.copyWith(recording: value));
  });
}/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get start {
  
  return $CommandStateCopyWith<void, $Res>(_self.start, (value) {
    return _then(_self.copyWith(start: value));
  });
}/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<ActivityRecordingSnapshot, $Res> get save {
  
  return $CommandStateCopyWith<ActivityRecordingSnapshot, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityRecordingUiState].
extension ActivityRecordingUiStatePatterns on ActivityRecordingUiState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingUiState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingUiState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingUiState value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingUiState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingUiState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingUiState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ActivityRecordingState recording,  DateTime? now,  bool isFocusMode,  CommandState<void> start,  CommandState<ActivityRecordingSnapshot> save)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingUiState() when $default != null:
return $default(_that.recording,_that.now,_that.isFocusMode,_that.start,_that.save);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ActivityRecordingState recording,  DateTime? now,  bool isFocusMode,  CommandState<void> start,  CommandState<ActivityRecordingSnapshot> save)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingUiState():
return $default(_that.recording,_that.now,_that.isFocusMode,_that.start,_that.save);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ActivityRecordingState recording,  DateTime? now,  bool isFocusMode,  CommandState<void> start,  CommandState<ActivityRecordingSnapshot> save)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingUiState() when $default != null:
return $default(_that.recording,_that.now,_that.isFocusMode,_that.start,_that.save);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingUiState extends ActivityRecordingUiState {
  const _ActivityRecordingUiState({this.recording = const ActivityRecordingState(), this.now, this.isFocusMode = false, this.start = const CommandState<void>.idle(), this.save = const CommandState<ActivityRecordingSnapshot>.idle()}): super._();
  

/// The last state published by the recording service.
@override@JsonKey() final  ActivityRecordingState recording;
/// The clock the elapsed/moving durations are read against. Ticks once a
/// second while the session is active, and is otherwise frozen — a widget
/// reading `DateTime.now()` itself would rebuild against a clock the state
/// has not seen.
@override final  DateTime? now;
/// Focus mode is a property of the SCREEN, not of the recording: it must
/// survive a rebuild but not a process death, and the host drops its app
/// bar for it.
@override@JsonKey() final  bool isFocusMode;
/// Starting a recording is failable — permissions, a missing GPS fix, an
/// unsupported activity type — and the failure is the user's to see.
@override@JsonKey() final  CommandState<void> start;
/// Stopping a recording hands back the snapshot that becomes the entry
/// form's draft. A stop with no active session fails rather than silently
/// producing nothing.
@override@JsonKey() final  CommandState<ActivityRecordingSnapshot> save;

/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingUiStateCopyWith<_ActivityRecordingUiState> get copyWith => __$ActivityRecordingUiStateCopyWithImpl<_ActivityRecordingUiState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingUiState&&(identical(other.recording, recording) || other.recording == recording)&&(identical(other.now, now) || other.now == now)&&(identical(other.isFocusMode, isFocusMode) || other.isFocusMode == isFocusMode)&&(identical(other.start, start) || other.start == start)&&(identical(other.save, save) || other.save == save));
}


@override
int get hashCode => Object.hash(runtimeType,recording,now,isFocusMode,start,save);

@override
String toString() {
  return 'ActivityRecordingUiState(recording: $recording, now: $now, isFocusMode: $isFocusMode, start: $start, save: $save)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingUiStateCopyWith<$Res> implements $ActivityRecordingUiStateCopyWith<$Res> {
  factory _$ActivityRecordingUiStateCopyWith(_ActivityRecordingUiState value, $Res Function(_ActivityRecordingUiState) _then) = __$ActivityRecordingUiStateCopyWithImpl;
@override @useResult
$Res call({
 ActivityRecordingState recording, DateTime? now, bool isFocusMode, CommandState<void> start, CommandState<ActivityRecordingSnapshot> save
});


@override $ActivityRecordingStateCopyWith<$Res> get recording;@override $CommandStateCopyWith<void, $Res> get start;@override $CommandStateCopyWith<ActivityRecordingSnapshot, $Res> get save;

}
/// @nodoc
class __$ActivityRecordingUiStateCopyWithImpl<$Res>
    implements _$ActivityRecordingUiStateCopyWith<$Res> {
  __$ActivityRecordingUiStateCopyWithImpl(this._self, this._then);

  final _ActivityRecordingUiState _self;
  final $Res Function(_ActivityRecordingUiState) _then;

/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? recording = null,Object? now = freezed,Object? isFocusMode = null,Object? start = null,Object? save = null,}) {
  return _then(_ActivityRecordingUiState(
recording: null == recording ? _self.recording : recording // ignore: cast_nullable_to_non_nullable
as ActivityRecordingState,now: freezed == now ? _self.now : now // ignore: cast_nullable_to_non_nullable
as DateTime?,isFocusMode: null == isFocusMode ? _self.isFocusMode : isFocusMode // ignore: cast_nullable_to_non_nullable
as bool,start: null == start ? _self.start : start // ignore: cast_nullable_to_non_nullable
as CommandState<void>,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<ActivityRecordingSnapshot>,
  ));
}

/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityRecordingStateCopyWith<$Res> get recording {
  
  return $ActivityRecordingStateCopyWith<$Res>(_self.recording, (value) {
    return _then(_self.copyWith(recording: value));
  });
}/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get start {
  
  return $CommandStateCopyWith<void, $Res>(_self.start, (value) {
    return _then(_self.copyWith(start: value));
  });
}/// Create a copy of ActivityRecordingUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<ActivityRecordingSnapshot, $Res> get save {
  
  return $CommandStateCopyWith<ActivityRecordingSnapshot, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}
}

// dart format on
