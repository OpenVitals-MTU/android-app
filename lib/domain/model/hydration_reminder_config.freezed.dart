// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_reminder_config.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationReminderConfig {

 bool get enabled; int get intervalMinutes; LocalTime get activeStartTime; LocalTime get activeEndTime;
/// Create a copy of HydrationReminderConfig
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationReminderConfigCopyWith<HydrationReminderConfig> get copyWith => _$HydrationReminderConfigCopyWithImpl<HydrationReminderConfig>(this as HydrationReminderConfig, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationReminderConfig&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.intervalMinutes, intervalMinutes) || other.intervalMinutes == intervalMinutes)&&(identical(other.activeStartTime, activeStartTime) || other.activeStartTime == activeStartTime)&&(identical(other.activeEndTime, activeEndTime) || other.activeEndTime == activeEndTime));
}


@override
int get hashCode => Object.hash(runtimeType,enabled,intervalMinutes,activeStartTime,activeEndTime);

@override
String toString() {
  return 'HydrationReminderConfig(enabled: $enabled, intervalMinutes: $intervalMinutes, activeStartTime: $activeStartTime, activeEndTime: $activeEndTime)';
}


}

/// @nodoc
abstract mixin class $HydrationReminderConfigCopyWith<$Res>  {
  factory $HydrationReminderConfigCopyWith(HydrationReminderConfig value, $Res Function(HydrationReminderConfig) _then) = _$HydrationReminderConfigCopyWithImpl;
@useResult
$Res call({
 bool enabled, int intervalMinutes, LocalTime activeStartTime, LocalTime activeEndTime
});




}
/// @nodoc
class _$HydrationReminderConfigCopyWithImpl<$Res>
    implements $HydrationReminderConfigCopyWith<$Res> {
  _$HydrationReminderConfigCopyWithImpl(this._self, this._then);

  final HydrationReminderConfig _self;
  final $Res Function(HydrationReminderConfig) _then;

/// Create a copy of HydrationReminderConfig
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? enabled = null,Object? intervalMinutes = null,Object? activeStartTime = null,Object? activeEndTime = null,}) {
  return _then(_self.copyWith(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,intervalMinutes: null == intervalMinutes ? _self.intervalMinutes : intervalMinutes // ignore: cast_nullable_to_non_nullable
as int,activeStartTime: null == activeStartTime ? _self.activeStartTime : activeStartTime // ignore: cast_nullable_to_non_nullable
as LocalTime,activeEndTime: null == activeEndTime ? _self.activeEndTime : activeEndTime // ignore: cast_nullable_to_non_nullable
as LocalTime,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationReminderConfig].
extension HydrationReminderConfigPatterns on HydrationReminderConfig {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationReminderConfig value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationReminderConfig() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationReminderConfig value)  $default,){
final _that = this;
switch (_that) {
case _HydrationReminderConfig():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationReminderConfig value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationReminderConfig() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool enabled,  int intervalMinutes,  LocalTime activeStartTime,  LocalTime activeEndTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationReminderConfig() when $default != null:
return $default(_that.enabled,_that.intervalMinutes,_that.activeStartTime,_that.activeEndTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool enabled,  int intervalMinutes,  LocalTime activeStartTime,  LocalTime activeEndTime)  $default,) {final _that = this;
switch (_that) {
case _HydrationReminderConfig():
return $default(_that.enabled,_that.intervalMinutes,_that.activeStartTime,_that.activeEndTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool enabled,  int intervalMinutes,  LocalTime activeStartTime,  LocalTime activeEndTime)?  $default,) {final _that = this;
switch (_that) {
case _HydrationReminderConfig() when $default != null:
return $default(_that.enabled,_that.intervalMinutes,_that.activeStartTime,_that.activeEndTime);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationReminderConfig extends HydrationReminderConfig {
  const _HydrationReminderConfig({this.enabled = false, this.intervalMinutes = HydrationReminderConfig.defaultIntervalMinutes, this.activeStartTime = const LocalTime(7, 0), this.activeEndTime = const LocalTime(23, 0)}): super._();
  

@override@JsonKey() final  bool enabled;
@override@JsonKey() final  int intervalMinutes;
@override@JsonKey() final  LocalTime activeStartTime;
@override@JsonKey() final  LocalTime activeEndTime;

/// Create a copy of HydrationReminderConfig
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationReminderConfigCopyWith<_HydrationReminderConfig> get copyWith => __$HydrationReminderConfigCopyWithImpl<_HydrationReminderConfig>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationReminderConfig&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.intervalMinutes, intervalMinutes) || other.intervalMinutes == intervalMinutes)&&(identical(other.activeStartTime, activeStartTime) || other.activeStartTime == activeStartTime)&&(identical(other.activeEndTime, activeEndTime) || other.activeEndTime == activeEndTime));
}


@override
int get hashCode => Object.hash(runtimeType,enabled,intervalMinutes,activeStartTime,activeEndTime);

@override
String toString() {
  return 'HydrationReminderConfig(enabled: $enabled, intervalMinutes: $intervalMinutes, activeStartTime: $activeStartTime, activeEndTime: $activeEndTime)';
}


}

/// @nodoc
abstract mixin class _$HydrationReminderConfigCopyWith<$Res> implements $HydrationReminderConfigCopyWith<$Res> {
  factory _$HydrationReminderConfigCopyWith(_HydrationReminderConfig value, $Res Function(_HydrationReminderConfig) _then) = __$HydrationReminderConfigCopyWithImpl;
@override @useResult
$Res call({
 bool enabled, int intervalMinutes, LocalTime activeStartTime, LocalTime activeEndTime
});




}
/// @nodoc
class __$HydrationReminderConfigCopyWithImpl<$Res>
    implements _$HydrationReminderConfigCopyWith<$Res> {
  __$HydrationReminderConfigCopyWithImpl(this._self, this._then);

  final _HydrationReminderConfig _self;
  final $Res Function(_HydrationReminderConfig) _then;

/// Create a copy of HydrationReminderConfig
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? enabled = null,Object? intervalMinutes = null,Object? activeStartTime = null,Object? activeEndTime = null,}) {
  return _then(_HydrationReminderConfig(
enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,intervalMinutes: null == intervalMinutes ? _self.intervalMinutes : intervalMinutes // ignore: cast_nullable_to_non_nullable
as int,activeStartTime: null == activeStartTime ? _self.activeStartTime : activeStartTime // ignore: cast_nullable_to_non_nullable
as LocalTime,activeEndTime: null == activeEndTime ? _self.activeEndTime : activeEndTime // ignore: cast_nullable_to_non_nullable
as LocalTime,
  ));
}


}

// dart format on
