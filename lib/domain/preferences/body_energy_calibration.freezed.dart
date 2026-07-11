// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_energy_calibration.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartZoneThresholds {

 int get zone1LowerBpm; int get zone2LowerBpm; int get zone3LowerBpm; int get zone4LowerBpm; int get zone5LowerBpm;
/// Create a copy of HeartZoneThresholds
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartZoneThresholdsCopyWith<HeartZoneThresholds> get copyWith => _$HeartZoneThresholdsCopyWithImpl<HeartZoneThresholds>(this as HeartZoneThresholds, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartZoneThresholds&&(identical(other.zone1LowerBpm, zone1LowerBpm) || other.zone1LowerBpm == zone1LowerBpm)&&(identical(other.zone2LowerBpm, zone2LowerBpm) || other.zone2LowerBpm == zone2LowerBpm)&&(identical(other.zone3LowerBpm, zone3LowerBpm) || other.zone3LowerBpm == zone3LowerBpm)&&(identical(other.zone4LowerBpm, zone4LowerBpm) || other.zone4LowerBpm == zone4LowerBpm)&&(identical(other.zone5LowerBpm, zone5LowerBpm) || other.zone5LowerBpm == zone5LowerBpm));
}


@override
int get hashCode => Object.hash(runtimeType,zone1LowerBpm,zone2LowerBpm,zone3LowerBpm,zone4LowerBpm,zone5LowerBpm);

@override
String toString() {
  return 'HeartZoneThresholds(zone1LowerBpm: $zone1LowerBpm, zone2LowerBpm: $zone2LowerBpm, zone3LowerBpm: $zone3LowerBpm, zone4LowerBpm: $zone4LowerBpm, zone5LowerBpm: $zone5LowerBpm)';
}


}

/// @nodoc
abstract mixin class $HeartZoneThresholdsCopyWith<$Res>  {
  factory $HeartZoneThresholdsCopyWith(HeartZoneThresholds value, $Res Function(HeartZoneThresholds) _then) = _$HeartZoneThresholdsCopyWithImpl;
@useResult
$Res call({
 int zone1LowerBpm, int zone2LowerBpm, int zone3LowerBpm, int zone4LowerBpm, int zone5LowerBpm
});




}
/// @nodoc
class _$HeartZoneThresholdsCopyWithImpl<$Res>
    implements $HeartZoneThresholdsCopyWith<$Res> {
  _$HeartZoneThresholdsCopyWithImpl(this._self, this._then);

  final HeartZoneThresholds _self;
  final $Res Function(HeartZoneThresholds) _then;

/// Create a copy of HeartZoneThresholds
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? zone1LowerBpm = null,Object? zone2LowerBpm = null,Object? zone3LowerBpm = null,Object? zone4LowerBpm = null,Object? zone5LowerBpm = null,}) {
  return _then(_self.copyWith(
zone1LowerBpm: null == zone1LowerBpm ? _self.zone1LowerBpm : zone1LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone2LowerBpm: null == zone2LowerBpm ? _self.zone2LowerBpm : zone2LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone3LowerBpm: null == zone3LowerBpm ? _self.zone3LowerBpm : zone3LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone4LowerBpm: null == zone4LowerBpm ? _self.zone4LowerBpm : zone4LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone5LowerBpm: null == zone5LowerBpm ? _self.zone5LowerBpm : zone5LowerBpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartZoneThresholds].
extension HeartZoneThresholdsPatterns on HeartZoneThresholds {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartZoneThresholds value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartZoneThresholds() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartZoneThresholds value)  $default,){
final _that = this;
switch (_that) {
case _HeartZoneThresholds():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartZoneThresholds value)?  $default,){
final _that = this;
switch (_that) {
case _HeartZoneThresholds() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int zone1LowerBpm,  int zone2LowerBpm,  int zone3LowerBpm,  int zone4LowerBpm,  int zone5LowerBpm)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartZoneThresholds() when $default != null:
return $default(_that.zone1LowerBpm,_that.zone2LowerBpm,_that.zone3LowerBpm,_that.zone4LowerBpm,_that.zone5LowerBpm);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int zone1LowerBpm,  int zone2LowerBpm,  int zone3LowerBpm,  int zone4LowerBpm,  int zone5LowerBpm)  $default,) {final _that = this;
switch (_that) {
case _HeartZoneThresholds():
return $default(_that.zone1LowerBpm,_that.zone2LowerBpm,_that.zone3LowerBpm,_that.zone4LowerBpm,_that.zone5LowerBpm);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int zone1LowerBpm,  int zone2LowerBpm,  int zone3LowerBpm,  int zone4LowerBpm,  int zone5LowerBpm)?  $default,) {final _that = this;
switch (_that) {
case _HeartZoneThresholds() when $default != null:
return $default(_that.zone1LowerBpm,_that.zone2LowerBpm,_that.zone3LowerBpm,_that.zone4LowerBpm,_that.zone5LowerBpm);case _:
  return null;

}
}

}

/// @nodoc


class _HeartZoneThresholds extends HeartZoneThresholds {
  const _HeartZoneThresholds({required this.zone1LowerBpm, required this.zone2LowerBpm, required this.zone3LowerBpm, required this.zone4LowerBpm, required this.zone5LowerBpm}): super._();
  

@override final  int zone1LowerBpm;
@override final  int zone2LowerBpm;
@override final  int zone3LowerBpm;
@override final  int zone4LowerBpm;
@override final  int zone5LowerBpm;

/// Create a copy of HeartZoneThresholds
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartZoneThresholdsCopyWith<_HeartZoneThresholds> get copyWith => __$HeartZoneThresholdsCopyWithImpl<_HeartZoneThresholds>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartZoneThresholds&&(identical(other.zone1LowerBpm, zone1LowerBpm) || other.zone1LowerBpm == zone1LowerBpm)&&(identical(other.zone2LowerBpm, zone2LowerBpm) || other.zone2LowerBpm == zone2LowerBpm)&&(identical(other.zone3LowerBpm, zone3LowerBpm) || other.zone3LowerBpm == zone3LowerBpm)&&(identical(other.zone4LowerBpm, zone4LowerBpm) || other.zone4LowerBpm == zone4LowerBpm)&&(identical(other.zone5LowerBpm, zone5LowerBpm) || other.zone5LowerBpm == zone5LowerBpm));
}


@override
int get hashCode => Object.hash(runtimeType,zone1LowerBpm,zone2LowerBpm,zone3LowerBpm,zone4LowerBpm,zone5LowerBpm);

@override
String toString() {
  return 'HeartZoneThresholds(zone1LowerBpm: $zone1LowerBpm, zone2LowerBpm: $zone2LowerBpm, zone3LowerBpm: $zone3LowerBpm, zone4LowerBpm: $zone4LowerBpm, zone5LowerBpm: $zone5LowerBpm)';
}


}

/// @nodoc
abstract mixin class _$HeartZoneThresholdsCopyWith<$Res> implements $HeartZoneThresholdsCopyWith<$Res> {
  factory _$HeartZoneThresholdsCopyWith(_HeartZoneThresholds value, $Res Function(_HeartZoneThresholds) _then) = __$HeartZoneThresholdsCopyWithImpl;
@override @useResult
$Res call({
 int zone1LowerBpm, int zone2LowerBpm, int zone3LowerBpm, int zone4LowerBpm, int zone5LowerBpm
});




}
/// @nodoc
class __$HeartZoneThresholdsCopyWithImpl<$Res>
    implements _$HeartZoneThresholdsCopyWith<$Res> {
  __$HeartZoneThresholdsCopyWithImpl(this._self, this._then);

  final _HeartZoneThresholds _self;
  final $Res Function(_HeartZoneThresholds) _then;

/// Create a copy of HeartZoneThresholds
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? zone1LowerBpm = null,Object? zone2LowerBpm = null,Object? zone3LowerBpm = null,Object? zone4LowerBpm = null,Object? zone5LowerBpm = null,}) {
  return _then(_HeartZoneThresholds(
zone1LowerBpm: null == zone1LowerBpm ? _self.zone1LowerBpm : zone1LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone2LowerBpm: null == zone2LowerBpm ? _self.zone2LowerBpm : zone2LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone3LowerBpm: null == zone3LowerBpm ? _self.zone3LowerBpm : zone3LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone4LowerBpm: null == zone4LowerBpm ? _self.zone4LowerBpm : zone4LowerBpm // ignore: cast_nullable_to_non_nullable
as int,zone5LowerBpm: null == zone5LowerBpm ? _self.zone5LowerBpm : zone5LowerBpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$BodyEnergyCalibration {

 HeartZoneThresholds? get manualZoneThresholdsBpm; bool get useManualZones; bool get setupCompleted;
/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyEnergyCalibrationCopyWith<BodyEnergyCalibration> get copyWith => _$BodyEnergyCalibrationCopyWithImpl<BodyEnergyCalibration>(this as BodyEnergyCalibration, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyEnergyCalibration&&(identical(other.manualZoneThresholdsBpm, manualZoneThresholdsBpm) || other.manualZoneThresholdsBpm == manualZoneThresholdsBpm)&&(identical(other.useManualZones, useManualZones) || other.useManualZones == useManualZones)&&(identical(other.setupCompleted, setupCompleted) || other.setupCompleted == setupCompleted));
}


@override
int get hashCode => Object.hash(runtimeType,manualZoneThresholdsBpm,useManualZones,setupCompleted);

@override
String toString() {
  return 'BodyEnergyCalibration(manualZoneThresholdsBpm: $manualZoneThresholdsBpm, useManualZones: $useManualZones, setupCompleted: $setupCompleted)';
}


}

/// @nodoc
abstract mixin class $BodyEnergyCalibrationCopyWith<$Res>  {
  factory $BodyEnergyCalibrationCopyWith(BodyEnergyCalibration value, $Res Function(BodyEnergyCalibration) _then) = _$BodyEnergyCalibrationCopyWithImpl;
@useResult
$Res call({
 HeartZoneThresholds? manualZoneThresholdsBpm, bool useManualZones, bool setupCompleted
});


$HeartZoneThresholdsCopyWith<$Res>? get manualZoneThresholdsBpm;

}
/// @nodoc
class _$BodyEnergyCalibrationCopyWithImpl<$Res>
    implements $BodyEnergyCalibrationCopyWith<$Res> {
  _$BodyEnergyCalibrationCopyWithImpl(this._self, this._then);

  final BodyEnergyCalibration _self;
  final $Res Function(BodyEnergyCalibration) _then;

/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? manualZoneThresholdsBpm = freezed,Object? useManualZones = null,Object? setupCompleted = null,}) {
  return _then(_self.copyWith(
manualZoneThresholdsBpm: freezed == manualZoneThresholdsBpm ? _self.manualZoneThresholdsBpm : manualZoneThresholdsBpm // ignore: cast_nullable_to_non_nullable
as HeartZoneThresholds?,useManualZones: null == useManualZones ? _self.useManualZones : useManualZones // ignore: cast_nullable_to_non_nullable
as bool,setupCompleted: null == setupCompleted ? _self.setupCompleted : setupCompleted // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}
/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartZoneThresholdsCopyWith<$Res>? get manualZoneThresholdsBpm {
    if (_self.manualZoneThresholdsBpm == null) {
    return null;
  }

  return $HeartZoneThresholdsCopyWith<$Res>(_self.manualZoneThresholdsBpm!, (value) {
    return _then(_self.copyWith(manualZoneThresholdsBpm: value));
  });
}
}


/// Adds pattern-matching-related methods to [BodyEnergyCalibration].
extension BodyEnergyCalibrationPatterns on BodyEnergyCalibration {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyEnergyCalibration value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyEnergyCalibration() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyEnergyCalibration value)  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyCalibration():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyEnergyCalibration value)?  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyCalibration() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( HeartZoneThresholds? manualZoneThresholdsBpm,  bool useManualZones,  bool setupCompleted)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyEnergyCalibration() when $default != null:
return $default(_that.manualZoneThresholdsBpm,_that.useManualZones,_that.setupCompleted);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( HeartZoneThresholds? manualZoneThresholdsBpm,  bool useManualZones,  bool setupCompleted)  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyCalibration():
return $default(_that.manualZoneThresholdsBpm,_that.useManualZones,_that.setupCompleted);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( HeartZoneThresholds? manualZoneThresholdsBpm,  bool useManualZones,  bool setupCompleted)?  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyCalibration() when $default != null:
return $default(_that.manualZoneThresholdsBpm,_that.useManualZones,_that.setupCompleted);case _:
  return null;

}
}

}

/// @nodoc


class _BodyEnergyCalibration extends BodyEnergyCalibration {
  const _BodyEnergyCalibration({this.manualZoneThresholdsBpm, this.useManualZones = false, this.setupCompleted = false}): super._();
  

@override final  HeartZoneThresholds? manualZoneThresholdsBpm;
@override@JsonKey() final  bool useManualZones;
@override@JsonKey() final  bool setupCompleted;

/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyEnergyCalibrationCopyWith<_BodyEnergyCalibration> get copyWith => __$BodyEnergyCalibrationCopyWithImpl<_BodyEnergyCalibration>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyEnergyCalibration&&(identical(other.manualZoneThresholdsBpm, manualZoneThresholdsBpm) || other.manualZoneThresholdsBpm == manualZoneThresholdsBpm)&&(identical(other.useManualZones, useManualZones) || other.useManualZones == useManualZones)&&(identical(other.setupCompleted, setupCompleted) || other.setupCompleted == setupCompleted));
}


@override
int get hashCode => Object.hash(runtimeType,manualZoneThresholdsBpm,useManualZones,setupCompleted);

@override
String toString() {
  return 'BodyEnergyCalibration(manualZoneThresholdsBpm: $manualZoneThresholdsBpm, useManualZones: $useManualZones, setupCompleted: $setupCompleted)';
}


}

/// @nodoc
abstract mixin class _$BodyEnergyCalibrationCopyWith<$Res> implements $BodyEnergyCalibrationCopyWith<$Res> {
  factory _$BodyEnergyCalibrationCopyWith(_BodyEnergyCalibration value, $Res Function(_BodyEnergyCalibration) _then) = __$BodyEnergyCalibrationCopyWithImpl;
@override @useResult
$Res call({
 HeartZoneThresholds? manualZoneThresholdsBpm, bool useManualZones, bool setupCompleted
});


@override $HeartZoneThresholdsCopyWith<$Res>? get manualZoneThresholdsBpm;

}
/// @nodoc
class __$BodyEnergyCalibrationCopyWithImpl<$Res>
    implements _$BodyEnergyCalibrationCopyWith<$Res> {
  __$BodyEnergyCalibrationCopyWithImpl(this._self, this._then);

  final _BodyEnergyCalibration _self;
  final $Res Function(_BodyEnergyCalibration) _then;

/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? manualZoneThresholdsBpm = freezed,Object? useManualZones = null,Object? setupCompleted = null,}) {
  return _then(_BodyEnergyCalibration(
manualZoneThresholdsBpm: freezed == manualZoneThresholdsBpm ? _self.manualZoneThresholdsBpm : manualZoneThresholdsBpm // ignore: cast_nullable_to_non_nullable
as HeartZoneThresholds?,useManualZones: null == useManualZones ? _self.useManualZones : useManualZones // ignore: cast_nullable_to_non_nullable
as bool,setupCompleted: null == setupCompleted ? _self.setupCompleted : setupCompleted // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

/// Create a copy of BodyEnergyCalibration
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartZoneThresholdsCopyWith<$Res>? get manualZoneThresholdsBpm {
    if (_self.manualZoneThresholdsBpm == null) {
    return null;
  }

  return $HeartZoneThresholdsCopyWith<$Res>(_self.manualZoneThresholdsBpm!, (value) {
    return _then(_self.copyWith(manualZoneThresholdsBpm: value));
  });
}
}

// dart format on
