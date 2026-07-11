// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mindfulness_reminder_config.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$MindfulnessReminderConfig {

 bool get enabled; LocalTime get reminderTime;
/// Create a copy of MindfulnessReminderConfig
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessReminderConfigCopyWith<MindfulnessReminderConfig> get copyWith => _$MindfulnessReminderConfigCopyWithImpl<MindfulnessReminderConfig>(this as MindfulnessReminderConfig, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessReminderConfig&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.reminderTime, reminderTime) || other.reminderTime == reminderTime));
}


@override
int get hashCode => Object.hash(runtimeType,enabled,reminderTime);

@override
String toString() {
  return 'MindfulnessReminderConfig(enabled: $enabled, reminderTime: $reminderTime)';
}


}

/// @nodoc
abstract mixin class $MindfulnessReminderConfigCopyWith<$Res>  {
  factory $MindfulnessReminderConfigCopyWith(MindfulnessReminderConfig value, $Res Function(MindfulnessReminderConfig) _then) = _$MindfulnessReminderConfigCopyWithImpl;
@useResult
$Res call({
 bool enabled, LocalTime reminderTime
});




}
/// @nodoc
class _$MindfulnessReminderConfigCopyWithImpl<$Res>
    implements $MindfulnessReminderConfigCopyWith<$Res> {
  _$MindfulnessReminderConfigCopyWithImpl(this._self, this._then);

  final MindfulnessReminderConfig _self;
  final $Res Function(MindfulnessReminderConfig) _then;

/// Create a copy of MindfulnessReminderConfig
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? enabled = null,Object? reminderTime = null,}) {
  return _then(_self.copyWith(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,reminderTime: null == reminderTime ? _self.reminderTime : reminderTime // ignore: cast_nullable_to_non_nullable
as LocalTime,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessReminderConfig].
extension MindfulnessReminderConfigPatterns on MindfulnessReminderConfig {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessReminderConfig value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessReminderConfig() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessReminderConfig value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessReminderConfig():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessReminderConfig value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessReminderConfig() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool enabled,  LocalTime reminderTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessReminderConfig() when $default != null:
return $default(_that.enabled,_that.reminderTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool enabled,  LocalTime reminderTime)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessReminderConfig():
return $default(_that.enabled,_that.reminderTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool enabled,  LocalTime reminderTime)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessReminderConfig() when $default != null:
return $default(_that.enabled,_that.reminderTime);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessReminderConfig extends MindfulnessReminderConfig {
  const _MindfulnessReminderConfig({this.enabled = false, this.reminderTime = const LocalTime(18, 0)}): super._();
  

@override@JsonKey() final  bool enabled;
@override@JsonKey() final  LocalTime reminderTime;

/// Create a copy of MindfulnessReminderConfig
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessReminderConfigCopyWith<_MindfulnessReminderConfig> get copyWith => __$MindfulnessReminderConfigCopyWithImpl<_MindfulnessReminderConfig>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessReminderConfig&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.reminderTime, reminderTime) || other.reminderTime == reminderTime));
}


@override
int get hashCode => Object.hash(runtimeType,enabled,reminderTime);

@override
String toString() {
  return 'MindfulnessReminderConfig(enabled: $enabled, reminderTime: $reminderTime)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessReminderConfigCopyWith<$Res> implements $MindfulnessReminderConfigCopyWith<$Res> {
  factory _$MindfulnessReminderConfigCopyWith(_MindfulnessReminderConfig value, $Res Function(_MindfulnessReminderConfig) _then) = __$MindfulnessReminderConfigCopyWithImpl;
@override @useResult
$Res call({
 bool enabled, LocalTime reminderTime
});




}
/// @nodoc
class __$MindfulnessReminderConfigCopyWithImpl<$Res>
    implements _$MindfulnessReminderConfigCopyWith<$Res> {
  __$MindfulnessReminderConfigCopyWithImpl(this._self, this._then);

  final _MindfulnessReminderConfig _self;
  final $Res Function(_MindfulnessReminderConfig) _then;

/// Create a copy of MindfulnessReminderConfig
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? enabled = null,Object? reminderTime = null,}) {
  return _then(_MindfulnessReminderConfig(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,reminderTime: null == reminderTime ? _self.reminderTime : reminderTime // ignore: cast_nullable_to_non_nullable
as LocalTime,
  ));
}


}

// dart format on
