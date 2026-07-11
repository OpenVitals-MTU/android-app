// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ble_sensor_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BleSensorDevice {

 String get id; String get displayName; String get address; String? get bluetoothName; Set<BleSensorCapability> get capabilities; bool get enabled; int? get wheelCircumferenceMm; int? get batteryPercent; DateTime? get batteryUpdatedAt; DateTime get addedAt;
/// Create a copy of BleSensorDevice
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleSensorDeviceCopyWith<BleSensorDevice> get copyWith => _$BleSensorDeviceCopyWithImpl<BleSensorDevice>(this as BleSensorDevice, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleSensorDevice&&(identical(other.id, id) || other.id == id)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&(identical(other.address, address) || other.address == address)&&(identical(other.bluetoothName, bluetoothName) || other.bluetoothName == bluetoothName)&&const DeepCollectionEquality().equals(other.capabilities, capabilities)&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.wheelCircumferenceMm, wheelCircumferenceMm) || other.wheelCircumferenceMm == wheelCircumferenceMm)&&(identical(other.batteryPercent, batteryPercent) || other.batteryPercent == batteryPercent)&&(identical(other.batteryUpdatedAt, batteryUpdatedAt) || other.batteryUpdatedAt == batteryUpdatedAt)&&(identical(other.addedAt, addedAt) || other.addedAt == addedAt));
}


@override
int get hashCode => Object.hash(runtimeType,id,displayName,address,bluetoothName,const DeepCollectionEquality().hash(capabilities),enabled,wheelCircumferenceMm,batteryPercent,batteryUpdatedAt,addedAt);

@override
String toString() {
  return 'BleSensorDevice(id: $id, displayName: $displayName, address: $address, bluetoothName: $bluetoothName, capabilities: $capabilities, enabled: $enabled, wheelCircumferenceMm: $wheelCircumferenceMm, batteryPercent: $batteryPercent, batteryUpdatedAt: $batteryUpdatedAt, addedAt: $addedAt)';
}


}

/// @nodoc
abstract mixin class $BleSensorDeviceCopyWith<$Res>  {
  factory $BleSensorDeviceCopyWith(BleSensorDevice value, $Res Function(BleSensorDevice) _then) = _$BleSensorDeviceCopyWithImpl;
@useResult
$Res call({
 String id, String displayName, String address, String? bluetoothName, Set<BleSensorCapability> capabilities, bool enabled, int? wheelCircumferenceMm, int? batteryPercent, DateTime? batteryUpdatedAt, DateTime addedAt
});




}
/// @nodoc
class _$BleSensorDeviceCopyWithImpl<$Res>
    implements $BleSensorDeviceCopyWith<$Res> {
  _$BleSensorDeviceCopyWithImpl(this._self, this._then);

  final BleSensorDevice _self;
  final $Res Function(BleSensorDevice) _then;

/// Create a copy of BleSensorDevice
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? displayName = null,Object? address = null,Object? bluetoothName = freezed,Object? capabilities = null,Object? enabled = null,Object? wheelCircumferenceMm = freezed,Object? batteryPercent = freezed,Object? batteryUpdatedAt = freezed,Object? addedAt = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,displayName: null == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String,address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,bluetoothName: freezed == bluetoothName ? _self.bluetoothName : bluetoothName // ignore: cast_nullable_to_non_nullable
as String?,capabilities: null == capabilities ? _self.capabilities : capabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,wheelCircumferenceMm: freezed == wheelCircumferenceMm ? _self.wheelCircumferenceMm : wheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as int?,batteryPercent: freezed == batteryPercent ? _self.batteryPercent : batteryPercent // ignore: cast_nullable_to_non_nullable
as int?,batteryUpdatedAt: freezed == batteryUpdatedAt ? _self.batteryUpdatedAt : batteryUpdatedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,addedAt: null == addedAt ? _self.addedAt : addedAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [BleSensorDevice].
extension BleSensorDevicePatterns on BleSensorDevice {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleSensorDevice value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleSensorDevice() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleSensorDevice value)  $default,){
final _that = this;
switch (_that) {
case _BleSensorDevice():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleSensorDevice value)?  $default,){
final _that = this;
switch (_that) {
case _BleSensorDevice() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String displayName,  String address,  String? bluetoothName,  Set<BleSensorCapability> capabilities,  bool enabled,  int? wheelCircumferenceMm,  int? batteryPercent,  DateTime? batteryUpdatedAt,  DateTime addedAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleSensorDevice() when $default != null:
return $default(_that.id,_that.displayName,_that.address,_that.bluetoothName,_that.capabilities,_that.enabled,_that.wheelCircumferenceMm,_that.batteryPercent,_that.batteryUpdatedAt,_that.addedAt);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String displayName,  String address,  String? bluetoothName,  Set<BleSensorCapability> capabilities,  bool enabled,  int? wheelCircumferenceMm,  int? batteryPercent,  DateTime? batteryUpdatedAt,  DateTime addedAt)  $default,) {final _that = this;
switch (_that) {
case _BleSensorDevice():
return $default(_that.id,_that.displayName,_that.address,_that.bluetoothName,_that.capabilities,_that.enabled,_that.wheelCircumferenceMm,_that.batteryPercent,_that.batteryUpdatedAt,_that.addedAt);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String displayName,  String address,  String? bluetoothName,  Set<BleSensorCapability> capabilities,  bool enabled,  int? wheelCircumferenceMm,  int? batteryPercent,  DateTime? batteryUpdatedAt,  DateTime addedAt)?  $default,) {final _that = this;
switch (_that) {
case _BleSensorDevice() when $default != null:
return $default(_that.id,_that.displayName,_that.address,_that.bluetoothName,_that.capabilities,_that.enabled,_that.wheelCircumferenceMm,_that.batteryPercent,_that.batteryUpdatedAt,_that.addedAt);case _:
  return null;

}
}

}

/// @nodoc


class _BleSensorDevice extends BleSensorDevice {
  const _BleSensorDevice({required this.id, required this.displayName, required this.address, required this.bluetoothName, required final  Set<BleSensorCapability> capabilities, required this.enabled, required this.wheelCircumferenceMm, this.batteryPercent, this.batteryUpdatedAt, required this.addedAt}): _capabilities = capabilities,super._();
  

@override final  String id;
@override final  String displayName;
@override final  String address;
@override final  String? bluetoothName;
 final  Set<BleSensorCapability> _capabilities;
@override Set<BleSensorCapability> get capabilities {
  if (_capabilities is EqualUnmodifiableSetView) return _capabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_capabilities);
}

@override final  bool enabled;
@override final  int? wheelCircumferenceMm;
@override final  int? batteryPercent;
@override final  DateTime? batteryUpdatedAt;
@override final  DateTime addedAt;

/// Create a copy of BleSensorDevice
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleSensorDeviceCopyWith<_BleSensorDevice> get copyWith => __$BleSensorDeviceCopyWithImpl<_BleSensorDevice>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleSensorDevice&&(identical(other.id, id) || other.id == id)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&(identical(other.address, address) || other.address == address)&&(identical(other.bluetoothName, bluetoothName) || other.bluetoothName == bluetoothName)&&const DeepCollectionEquality().equals(other._capabilities, _capabilities)&&(identical(other.enabled, enabled) || other.enabled == enabled)&&(identical(other.wheelCircumferenceMm, wheelCircumferenceMm) || other.wheelCircumferenceMm == wheelCircumferenceMm)&&(identical(other.batteryPercent, batteryPercent) || other.batteryPercent == batteryPercent)&&(identical(other.batteryUpdatedAt, batteryUpdatedAt) || other.batteryUpdatedAt == batteryUpdatedAt)&&(identical(other.addedAt, addedAt) || other.addedAt == addedAt));
}


@override
int get hashCode => Object.hash(runtimeType,id,displayName,address,bluetoothName,const DeepCollectionEquality().hash(_capabilities),enabled,wheelCircumferenceMm,batteryPercent,batteryUpdatedAt,addedAt);

@override
String toString() {
  return 'BleSensorDevice(id: $id, displayName: $displayName, address: $address, bluetoothName: $bluetoothName, capabilities: $capabilities, enabled: $enabled, wheelCircumferenceMm: $wheelCircumferenceMm, batteryPercent: $batteryPercent, batteryUpdatedAt: $batteryUpdatedAt, addedAt: $addedAt)';
}


}

/// @nodoc
abstract mixin class _$BleSensorDeviceCopyWith<$Res> implements $BleSensorDeviceCopyWith<$Res> {
  factory _$BleSensorDeviceCopyWith(_BleSensorDevice value, $Res Function(_BleSensorDevice) _then) = __$BleSensorDeviceCopyWithImpl;
@override @useResult
$Res call({
 String id, String displayName, String address, String? bluetoothName, Set<BleSensorCapability> capabilities, bool enabled, int? wheelCircumferenceMm, int? batteryPercent, DateTime? batteryUpdatedAt, DateTime addedAt
});




}
/// @nodoc
class __$BleSensorDeviceCopyWithImpl<$Res>
    implements _$BleSensorDeviceCopyWith<$Res> {
  __$BleSensorDeviceCopyWithImpl(this._self, this._then);

  final _BleSensorDevice _self;
  final $Res Function(_BleSensorDevice) _then;

/// Create a copy of BleSensorDevice
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? displayName = null,Object? address = null,Object? bluetoothName = freezed,Object? capabilities = null,Object? enabled = null,Object? wheelCircumferenceMm = freezed,Object? batteryPercent = freezed,Object? batteryUpdatedAt = freezed,Object? addedAt = null,}) {
  return _then(_BleSensorDevice(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,displayName: null == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String,address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,bluetoothName: freezed == bluetoothName ? _self.bluetoothName : bluetoothName // ignore: cast_nullable_to_non_nullable
as String?,capabilities: null == capabilities ? _self._capabilities : capabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,enabled: null == enabled ? _self.enabled : enabled // ignore: cast_nullable_to_non_nullable
as bool,wheelCircumferenceMm: freezed == wheelCircumferenceMm ? _self.wheelCircumferenceMm : wheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as int?,batteryPercent: freezed == batteryPercent ? _self.batteryPercent : batteryPercent // ignore: cast_nullable_to_non_nullable
as int?,batteryUpdatedAt: freezed == batteryUpdatedAt ? _self.batteryUpdatedAt : batteryUpdatedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,addedAt: null == addedAt ? _self.addedAt : addedAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}

/// @nodoc
mixin _$BleDeviceConnectionStatus {

 String get deviceId; String get displayName; String get address; BleConnectionStatus get status; Set<BleSensorCapability> get capabilities; int? get batteryPercent;
/// Create a copy of BleDeviceConnectionStatus
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleDeviceConnectionStatusCopyWith<BleDeviceConnectionStatus> get copyWith => _$BleDeviceConnectionStatusCopyWithImpl<BleDeviceConnectionStatus>(this as BleDeviceConnectionStatus, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleDeviceConnectionStatus&&(identical(other.deviceId, deviceId) || other.deviceId == deviceId)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&(identical(other.address, address) || other.address == address)&&(identical(other.status, status) || other.status == status)&&const DeepCollectionEquality().equals(other.capabilities, capabilities)&&(identical(other.batteryPercent, batteryPercent) || other.batteryPercent == batteryPercent));
}


@override
int get hashCode => Object.hash(runtimeType,deviceId,displayName,address,status,const DeepCollectionEquality().hash(capabilities),batteryPercent);

@override
String toString() {
  return 'BleDeviceConnectionStatus(deviceId: $deviceId, displayName: $displayName, address: $address, status: $status, capabilities: $capabilities, batteryPercent: $batteryPercent)';
}


}

/// @nodoc
abstract mixin class $BleDeviceConnectionStatusCopyWith<$Res>  {
  factory $BleDeviceConnectionStatusCopyWith(BleDeviceConnectionStatus value, $Res Function(BleDeviceConnectionStatus) _then) = _$BleDeviceConnectionStatusCopyWithImpl;
@useResult
$Res call({
 String deviceId, String displayName, String address, BleConnectionStatus status, Set<BleSensorCapability> capabilities, int? batteryPercent
});




}
/// @nodoc
class _$BleDeviceConnectionStatusCopyWithImpl<$Res>
    implements $BleDeviceConnectionStatusCopyWith<$Res> {
  _$BleDeviceConnectionStatusCopyWithImpl(this._self, this._then);

  final BleDeviceConnectionStatus _self;
  final $Res Function(BleDeviceConnectionStatus) _then;

/// Create a copy of BleDeviceConnectionStatus
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? deviceId = null,Object? displayName = null,Object? address = null,Object? status = null,Object? capabilities = null,Object? batteryPercent = freezed,}) {
  return _then(_self.copyWith(
deviceId: null == deviceId ? _self.deviceId : deviceId // ignore: cast_nullable_to_non_nullable
as String,displayName: null == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String,address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as BleConnectionStatus,capabilities: null == capabilities ? _self.capabilities : capabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,batteryPercent: freezed == batteryPercent ? _self.batteryPercent : batteryPercent // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [BleDeviceConnectionStatus].
extension BleDeviceConnectionStatusPatterns on BleDeviceConnectionStatus {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleDeviceConnectionStatus value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleDeviceConnectionStatus value)  $default,){
final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleDeviceConnectionStatus value)?  $default,){
final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String deviceId,  String displayName,  String address,  BleConnectionStatus status,  Set<BleSensorCapability> capabilities,  int? batteryPercent)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus() when $default != null:
return $default(_that.deviceId,_that.displayName,_that.address,_that.status,_that.capabilities,_that.batteryPercent);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String deviceId,  String displayName,  String address,  BleConnectionStatus status,  Set<BleSensorCapability> capabilities,  int? batteryPercent)  $default,) {final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus():
return $default(_that.deviceId,_that.displayName,_that.address,_that.status,_that.capabilities,_that.batteryPercent);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String deviceId,  String displayName,  String address,  BleConnectionStatus status,  Set<BleSensorCapability> capabilities,  int? batteryPercent)?  $default,) {final _that = this;
switch (_that) {
case _BleDeviceConnectionStatus() when $default != null:
return $default(_that.deviceId,_that.displayName,_that.address,_that.status,_that.capabilities,_that.batteryPercent);case _:
  return null;

}
}

}

/// @nodoc


class _BleDeviceConnectionStatus implements BleDeviceConnectionStatus {
  const _BleDeviceConnectionStatus({required this.deviceId, required this.displayName, required this.address, required this.status, required final  Set<BleSensorCapability> capabilities, this.batteryPercent}): _capabilities = capabilities;
  

@override final  String deviceId;
@override final  String displayName;
@override final  String address;
@override final  BleConnectionStatus status;
 final  Set<BleSensorCapability> _capabilities;
@override Set<BleSensorCapability> get capabilities {
  if (_capabilities is EqualUnmodifiableSetView) return _capabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_capabilities);
}

@override final  int? batteryPercent;

/// Create a copy of BleDeviceConnectionStatus
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleDeviceConnectionStatusCopyWith<_BleDeviceConnectionStatus> get copyWith => __$BleDeviceConnectionStatusCopyWithImpl<_BleDeviceConnectionStatus>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleDeviceConnectionStatus&&(identical(other.deviceId, deviceId) || other.deviceId == deviceId)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&(identical(other.address, address) || other.address == address)&&(identical(other.status, status) || other.status == status)&&const DeepCollectionEquality().equals(other._capabilities, _capabilities)&&(identical(other.batteryPercent, batteryPercent) || other.batteryPercent == batteryPercent));
}


@override
int get hashCode => Object.hash(runtimeType,deviceId,displayName,address,status,const DeepCollectionEquality().hash(_capabilities),batteryPercent);

@override
String toString() {
  return 'BleDeviceConnectionStatus(deviceId: $deviceId, displayName: $displayName, address: $address, status: $status, capabilities: $capabilities, batteryPercent: $batteryPercent)';
}


}

/// @nodoc
abstract mixin class _$BleDeviceConnectionStatusCopyWith<$Res> implements $BleDeviceConnectionStatusCopyWith<$Res> {
  factory _$BleDeviceConnectionStatusCopyWith(_BleDeviceConnectionStatus value, $Res Function(_BleDeviceConnectionStatus) _then) = __$BleDeviceConnectionStatusCopyWithImpl;
@override @useResult
$Res call({
 String deviceId, String displayName, String address, BleConnectionStatus status, Set<BleSensorCapability> capabilities, int? batteryPercent
});




}
/// @nodoc
class __$BleDeviceConnectionStatusCopyWithImpl<$Res>
    implements _$BleDeviceConnectionStatusCopyWith<$Res> {
  __$BleDeviceConnectionStatusCopyWithImpl(this._self, this._then);

  final _BleDeviceConnectionStatus _self;
  final $Res Function(_BleDeviceConnectionStatus) _then;

/// Create a copy of BleDeviceConnectionStatus
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? deviceId = null,Object? displayName = null,Object? address = null,Object? status = null,Object? capabilities = null,Object? batteryPercent = freezed,}) {
  return _then(_BleDeviceConnectionStatus(
deviceId: null == deviceId ? _self.deviceId : deviceId // ignore: cast_nullable_to_non_nullable
as String,displayName: null == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String,address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as BleConnectionStatus,capabilities: null == capabilities ? _self._capabilities : capabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,batteryPercent: freezed == batteryPercent ? _self.batteryPercent : batteryPercent // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

/// @nodoc
mixin _$BleRecordingMetrics {

 int? get heartRateBpm; int? get cyclingCadenceRpm; double? get powerWatts; double? get cyclingSpeedMetersPerSecond; double? get runningSpeedMetersPerSecond; int? get runningCadenceRpm; bool get heartRateNoSignal; List<BleDeviceConnectionStatus> get deviceStatuses;
/// Create a copy of BleRecordingMetrics
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleRecordingMetricsCopyWith<BleRecordingMetrics> get copyWith => _$BleRecordingMetricsCopyWithImpl<BleRecordingMetrics>(this as BleRecordingMetrics, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleRecordingMetrics&&(identical(other.heartRateBpm, heartRateBpm) || other.heartRateBpm == heartRateBpm)&&(identical(other.cyclingCadenceRpm, cyclingCadenceRpm) || other.cyclingCadenceRpm == cyclingCadenceRpm)&&(identical(other.powerWatts, powerWatts) || other.powerWatts == powerWatts)&&(identical(other.cyclingSpeedMetersPerSecond, cyclingSpeedMetersPerSecond) || other.cyclingSpeedMetersPerSecond == cyclingSpeedMetersPerSecond)&&(identical(other.runningSpeedMetersPerSecond, runningSpeedMetersPerSecond) || other.runningSpeedMetersPerSecond == runningSpeedMetersPerSecond)&&(identical(other.runningCadenceRpm, runningCadenceRpm) || other.runningCadenceRpm == runningCadenceRpm)&&(identical(other.heartRateNoSignal, heartRateNoSignal) || other.heartRateNoSignal == heartRateNoSignal)&&const DeepCollectionEquality().equals(other.deviceStatuses, deviceStatuses));
}


@override
int get hashCode => Object.hash(runtimeType,heartRateBpm,cyclingCadenceRpm,powerWatts,cyclingSpeedMetersPerSecond,runningSpeedMetersPerSecond,runningCadenceRpm,heartRateNoSignal,const DeepCollectionEquality().hash(deviceStatuses));

@override
String toString() {
  return 'BleRecordingMetrics(heartRateBpm: $heartRateBpm, cyclingCadenceRpm: $cyclingCadenceRpm, powerWatts: $powerWatts, cyclingSpeedMetersPerSecond: $cyclingSpeedMetersPerSecond, runningSpeedMetersPerSecond: $runningSpeedMetersPerSecond, runningCadenceRpm: $runningCadenceRpm, heartRateNoSignal: $heartRateNoSignal, deviceStatuses: $deviceStatuses)';
}


}

/// @nodoc
abstract mixin class $BleRecordingMetricsCopyWith<$Res>  {
  factory $BleRecordingMetricsCopyWith(BleRecordingMetrics value, $Res Function(BleRecordingMetrics) _then) = _$BleRecordingMetricsCopyWithImpl;
@useResult
$Res call({
 int? heartRateBpm, int? cyclingCadenceRpm, double? powerWatts, double? cyclingSpeedMetersPerSecond, double? runningSpeedMetersPerSecond, int? runningCadenceRpm, bool heartRateNoSignal, List<BleDeviceConnectionStatus> deviceStatuses
});




}
/// @nodoc
class _$BleRecordingMetricsCopyWithImpl<$Res>
    implements $BleRecordingMetricsCopyWith<$Res> {
  _$BleRecordingMetricsCopyWithImpl(this._self, this._then);

  final BleRecordingMetrics _self;
  final $Res Function(BleRecordingMetrics) _then;

/// Create a copy of BleRecordingMetrics
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? heartRateBpm = freezed,Object? cyclingCadenceRpm = freezed,Object? powerWatts = freezed,Object? cyclingSpeedMetersPerSecond = freezed,Object? runningSpeedMetersPerSecond = freezed,Object? runningCadenceRpm = freezed,Object? heartRateNoSignal = null,Object? deviceStatuses = null,}) {
  return _then(_self.copyWith(
heartRateBpm: freezed == heartRateBpm ? _self.heartRateBpm : heartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,cyclingCadenceRpm: freezed == cyclingCadenceRpm ? _self.cyclingCadenceRpm : cyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,powerWatts: freezed == powerWatts ? _self.powerWatts : powerWatts // ignore: cast_nullable_to_non_nullable
as double?,cyclingSpeedMetersPerSecond: freezed == cyclingSpeedMetersPerSecond ? _self.cyclingSpeedMetersPerSecond : cyclingSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,runningSpeedMetersPerSecond: freezed == runningSpeedMetersPerSecond ? _self.runningSpeedMetersPerSecond : runningSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,runningCadenceRpm: freezed == runningCadenceRpm ? _self.runningCadenceRpm : runningCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,heartRateNoSignal: null == heartRateNoSignal ? _self.heartRateNoSignal : heartRateNoSignal // ignore: cast_nullable_to_non_nullable
as bool,deviceStatuses: null == deviceStatuses ? _self.deviceStatuses : deviceStatuses // ignore: cast_nullable_to_non_nullable
as List<BleDeviceConnectionStatus>,
  ));
}

}


/// Adds pattern-matching-related methods to [BleRecordingMetrics].
extension BleRecordingMetricsPatterns on BleRecordingMetrics {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleRecordingMetrics value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleRecordingMetrics() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleRecordingMetrics value)  $default,){
final _that = this;
switch (_that) {
case _BleRecordingMetrics():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleRecordingMetrics value)?  $default,){
final _that = this;
switch (_that) {
case _BleRecordingMetrics() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int? heartRateBpm,  int? cyclingCadenceRpm,  double? powerWatts,  double? cyclingSpeedMetersPerSecond,  double? runningSpeedMetersPerSecond,  int? runningCadenceRpm,  bool heartRateNoSignal,  List<BleDeviceConnectionStatus> deviceStatuses)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleRecordingMetrics() when $default != null:
return $default(_that.heartRateBpm,_that.cyclingCadenceRpm,_that.powerWatts,_that.cyclingSpeedMetersPerSecond,_that.runningSpeedMetersPerSecond,_that.runningCadenceRpm,_that.heartRateNoSignal,_that.deviceStatuses);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int? heartRateBpm,  int? cyclingCadenceRpm,  double? powerWatts,  double? cyclingSpeedMetersPerSecond,  double? runningSpeedMetersPerSecond,  int? runningCadenceRpm,  bool heartRateNoSignal,  List<BleDeviceConnectionStatus> deviceStatuses)  $default,) {final _that = this;
switch (_that) {
case _BleRecordingMetrics():
return $default(_that.heartRateBpm,_that.cyclingCadenceRpm,_that.powerWatts,_that.cyclingSpeedMetersPerSecond,_that.runningSpeedMetersPerSecond,_that.runningCadenceRpm,_that.heartRateNoSignal,_that.deviceStatuses);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int? heartRateBpm,  int? cyclingCadenceRpm,  double? powerWatts,  double? cyclingSpeedMetersPerSecond,  double? runningSpeedMetersPerSecond,  int? runningCadenceRpm,  bool heartRateNoSignal,  List<BleDeviceConnectionStatus> deviceStatuses)?  $default,) {final _that = this;
switch (_that) {
case _BleRecordingMetrics() when $default != null:
return $default(_that.heartRateBpm,_that.cyclingCadenceRpm,_that.powerWatts,_that.cyclingSpeedMetersPerSecond,_that.runningSpeedMetersPerSecond,_that.runningCadenceRpm,_that.heartRateNoSignal,_that.deviceStatuses);case _:
  return null;

}
}

}

/// @nodoc


class _BleRecordingMetrics implements BleRecordingMetrics {
  const _BleRecordingMetrics({this.heartRateBpm, this.cyclingCadenceRpm, this.powerWatts, this.cyclingSpeedMetersPerSecond, this.runningSpeedMetersPerSecond, this.runningCadenceRpm, this.heartRateNoSignal = false, final  List<BleDeviceConnectionStatus> deviceStatuses = const <BleDeviceConnectionStatus>[]}): _deviceStatuses = deviceStatuses;
  

@override final  int? heartRateBpm;
@override final  int? cyclingCadenceRpm;
@override final  double? powerWatts;
@override final  double? cyclingSpeedMetersPerSecond;
@override final  double? runningSpeedMetersPerSecond;
@override final  int? runningCadenceRpm;
@override@JsonKey() final  bool heartRateNoSignal;
 final  List<BleDeviceConnectionStatus> _deviceStatuses;
@override@JsonKey() List<BleDeviceConnectionStatus> get deviceStatuses {
  if (_deviceStatuses is EqualUnmodifiableListView) return _deviceStatuses;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_deviceStatuses);
}


/// Create a copy of BleRecordingMetrics
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleRecordingMetricsCopyWith<_BleRecordingMetrics> get copyWith => __$BleRecordingMetricsCopyWithImpl<_BleRecordingMetrics>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleRecordingMetrics&&(identical(other.heartRateBpm, heartRateBpm) || other.heartRateBpm == heartRateBpm)&&(identical(other.cyclingCadenceRpm, cyclingCadenceRpm) || other.cyclingCadenceRpm == cyclingCadenceRpm)&&(identical(other.powerWatts, powerWatts) || other.powerWatts == powerWatts)&&(identical(other.cyclingSpeedMetersPerSecond, cyclingSpeedMetersPerSecond) || other.cyclingSpeedMetersPerSecond == cyclingSpeedMetersPerSecond)&&(identical(other.runningSpeedMetersPerSecond, runningSpeedMetersPerSecond) || other.runningSpeedMetersPerSecond == runningSpeedMetersPerSecond)&&(identical(other.runningCadenceRpm, runningCadenceRpm) || other.runningCadenceRpm == runningCadenceRpm)&&(identical(other.heartRateNoSignal, heartRateNoSignal) || other.heartRateNoSignal == heartRateNoSignal)&&const DeepCollectionEquality().equals(other._deviceStatuses, _deviceStatuses));
}


@override
int get hashCode => Object.hash(runtimeType,heartRateBpm,cyclingCadenceRpm,powerWatts,cyclingSpeedMetersPerSecond,runningSpeedMetersPerSecond,runningCadenceRpm,heartRateNoSignal,const DeepCollectionEquality().hash(_deviceStatuses));

@override
String toString() {
  return 'BleRecordingMetrics(heartRateBpm: $heartRateBpm, cyclingCadenceRpm: $cyclingCadenceRpm, powerWatts: $powerWatts, cyclingSpeedMetersPerSecond: $cyclingSpeedMetersPerSecond, runningSpeedMetersPerSecond: $runningSpeedMetersPerSecond, runningCadenceRpm: $runningCadenceRpm, heartRateNoSignal: $heartRateNoSignal, deviceStatuses: $deviceStatuses)';
}


}

/// @nodoc
abstract mixin class _$BleRecordingMetricsCopyWith<$Res> implements $BleRecordingMetricsCopyWith<$Res> {
  factory _$BleRecordingMetricsCopyWith(_BleRecordingMetrics value, $Res Function(_BleRecordingMetrics) _then) = __$BleRecordingMetricsCopyWithImpl;
@override @useResult
$Res call({
 int? heartRateBpm, int? cyclingCadenceRpm, double? powerWatts, double? cyclingSpeedMetersPerSecond, double? runningSpeedMetersPerSecond, int? runningCadenceRpm, bool heartRateNoSignal, List<BleDeviceConnectionStatus> deviceStatuses
});




}
/// @nodoc
class __$BleRecordingMetricsCopyWithImpl<$Res>
    implements _$BleRecordingMetricsCopyWith<$Res> {
  __$BleRecordingMetricsCopyWithImpl(this._self, this._then);

  final _BleRecordingMetrics _self;
  final $Res Function(_BleRecordingMetrics) _then;

/// Create a copy of BleRecordingMetrics
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? heartRateBpm = freezed,Object? cyclingCadenceRpm = freezed,Object? powerWatts = freezed,Object? cyclingSpeedMetersPerSecond = freezed,Object? runningSpeedMetersPerSecond = freezed,Object? runningCadenceRpm = freezed,Object? heartRateNoSignal = null,Object? deviceStatuses = null,}) {
  return _then(_BleRecordingMetrics(
heartRateBpm: freezed == heartRateBpm ? _self.heartRateBpm : heartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,cyclingCadenceRpm: freezed == cyclingCadenceRpm ? _self.cyclingCadenceRpm : cyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,powerWatts: freezed == powerWatts ? _self.powerWatts : powerWatts // ignore: cast_nullable_to_non_nullable
as double?,cyclingSpeedMetersPerSecond: freezed == cyclingSpeedMetersPerSecond ? _self.cyclingSpeedMetersPerSecond : cyclingSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,runningSpeedMetersPerSecond: freezed == runningSpeedMetersPerSecond ? _self.runningSpeedMetersPerSecond : runningSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,runningCadenceRpm: freezed == runningCadenceRpm ? _self.runningCadenceRpm : runningCadenceRpm // ignore: cast_nullable_to_non_nullable
as int?,heartRateNoSignal: null == heartRateNoSignal ? _self.heartRateNoSignal : heartRateNoSignal // ignore: cast_nullable_to_non_nullable
as bool,deviceStatuses: null == deviceStatuses ? _self._deviceStatuses : deviceStatuses // ignore: cast_nullable_to_non_nullable
as List<BleDeviceConnectionStatus>,
  ));
}


}

/// @nodoc
mixin _$BleHeartRateSample {

 DateTime get time; int get beatsPerMinute;
/// Create a copy of BleHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleHeartRateSampleCopyWith<BleHeartRateSample> get copyWith => _$BleHeartRateSampleCopyWithImpl<BleHeartRateSample>(this as BleHeartRateSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleHeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute);

@override
String toString() {
  return 'BleHeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute)';
}


}

/// @nodoc
abstract mixin class $BleHeartRateSampleCopyWith<$Res>  {
  factory $BleHeartRateSampleCopyWith(BleHeartRateSample value, $Res Function(BleHeartRateSample) _then) = _$BleHeartRateSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, int beatsPerMinute
});




}
/// @nodoc
class _$BleHeartRateSampleCopyWithImpl<$Res>
    implements $BleHeartRateSampleCopyWith<$Res> {
  _$BleHeartRateSampleCopyWithImpl(this._self, this._then);

  final BleHeartRateSample _self;
  final $Res Function(BleHeartRateSample) _then;

/// Create a copy of BleHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? beatsPerMinute = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [BleHeartRateSample].
extension BleHeartRateSamplePatterns on BleHeartRateSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleHeartRateSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleHeartRateSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleHeartRateSample value)  $default,){
final _that = this;
switch (_that) {
case _BleHeartRateSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleHeartRateSample value)?  $default,){
final _that = this;
switch (_that) {
case _BleHeartRateSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleHeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute)  $default,) {final _that = this;
switch (_that) {
case _BleHeartRateSample():
return $default(_that.time,_that.beatsPerMinute);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int beatsPerMinute)?  $default,) {final _that = this;
switch (_that) {
case _BleHeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute);case _:
  return null;

}
}

}

/// @nodoc


class _BleHeartRateSample implements BleHeartRateSample {
  const _BleHeartRateSample({required this.time, required this.beatsPerMinute});
  

@override final  DateTime time;
@override final  int beatsPerMinute;

/// Create a copy of BleHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleHeartRateSampleCopyWith<_BleHeartRateSample> get copyWith => __$BleHeartRateSampleCopyWithImpl<_BleHeartRateSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleHeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute);

@override
String toString() {
  return 'BleHeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute)';
}


}

/// @nodoc
abstract mixin class _$BleHeartRateSampleCopyWith<$Res> implements $BleHeartRateSampleCopyWith<$Res> {
  factory _$BleHeartRateSampleCopyWith(_BleHeartRateSample value, $Res Function(_BleHeartRateSample) _then) = __$BleHeartRateSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int beatsPerMinute
});




}
/// @nodoc
class __$BleHeartRateSampleCopyWithImpl<$Res>
    implements _$BleHeartRateSampleCopyWith<$Res> {
  __$BleHeartRateSampleCopyWithImpl(this._self, this._then);

  final _BleHeartRateSample _self;
  final $Res Function(_BleHeartRateSample) _then;

/// Create a copy of BleHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? beatsPerMinute = null,}) {
  return _then(_BleHeartRateSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$BlePowerSample {

 DateTime get time; double get watts;
/// Create a copy of BlePowerSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BlePowerSampleCopyWith<BlePowerSample> get copyWith => _$BlePowerSampleCopyWithImpl<BlePowerSample>(this as BlePowerSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BlePowerSample&&(identical(other.time, time) || other.time == time)&&(identical(other.watts, watts) || other.watts == watts));
}


@override
int get hashCode => Object.hash(runtimeType,time,watts);

@override
String toString() {
  return 'BlePowerSample(time: $time, watts: $watts)';
}


}

/// @nodoc
abstract mixin class $BlePowerSampleCopyWith<$Res>  {
  factory $BlePowerSampleCopyWith(BlePowerSample value, $Res Function(BlePowerSample) _then) = _$BlePowerSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, double watts
});




}
/// @nodoc
class _$BlePowerSampleCopyWithImpl<$Res>
    implements $BlePowerSampleCopyWith<$Res> {
  _$BlePowerSampleCopyWithImpl(this._self, this._then);

  final BlePowerSample _self;
  final $Res Function(BlePowerSample) _then;

/// Create a copy of BlePowerSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? watts = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,watts: null == watts ? _self.watts : watts // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [BlePowerSample].
extension BlePowerSamplePatterns on BlePowerSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BlePowerSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BlePowerSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BlePowerSample value)  $default,){
final _that = this;
switch (_that) {
case _BlePowerSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BlePowerSample value)?  $default,){
final _that = this;
switch (_that) {
case _BlePowerSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double watts)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BlePowerSample() when $default != null:
return $default(_that.time,_that.watts);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double watts)  $default,) {final _that = this;
switch (_that) {
case _BlePowerSample():
return $default(_that.time,_that.watts);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double watts)?  $default,) {final _that = this;
switch (_that) {
case _BlePowerSample() when $default != null:
return $default(_that.time,_that.watts);case _:
  return null;

}
}

}

/// @nodoc


class _BlePowerSample implements BlePowerSample {
  const _BlePowerSample({required this.time, required this.watts});
  

@override final  DateTime time;
@override final  double watts;

/// Create a copy of BlePowerSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BlePowerSampleCopyWith<_BlePowerSample> get copyWith => __$BlePowerSampleCopyWithImpl<_BlePowerSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BlePowerSample&&(identical(other.time, time) || other.time == time)&&(identical(other.watts, watts) || other.watts == watts));
}


@override
int get hashCode => Object.hash(runtimeType,time,watts);

@override
String toString() {
  return 'BlePowerSample(time: $time, watts: $watts)';
}


}

/// @nodoc
abstract mixin class _$BlePowerSampleCopyWith<$Res> implements $BlePowerSampleCopyWith<$Res> {
  factory _$BlePowerSampleCopyWith(_BlePowerSample value, $Res Function(_BlePowerSample) _then) = __$BlePowerSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double watts
});




}
/// @nodoc
class __$BlePowerSampleCopyWithImpl<$Res>
    implements _$BlePowerSampleCopyWith<$Res> {
  __$BlePowerSampleCopyWithImpl(this._self, this._then);

  final _BlePowerSample _self;
  final $Res Function(_BlePowerSample) _then;

/// Create a copy of BlePowerSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? watts = null,}) {
  return _then(_BlePowerSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,watts: null == watts ? _self.watts : watts // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$BleCyclingCadenceSample {

 DateTime get time; int get rpm;
/// Create a copy of BleCyclingCadenceSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleCyclingCadenceSampleCopyWith<BleCyclingCadenceSample> get copyWith => _$BleCyclingCadenceSampleCopyWithImpl<BleCyclingCadenceSample>(this as BleCyclingCadenceSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleCyclingCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rpm, rpm) || other.rpm == rpm));
}


@override
int get hashCode => Object.hash(runtimeType,time,rpm);

@override
String toString() {
  return 'BleCyclingCadenceSample(time: $time, rpm: $rpm)';
}


}

/// @nodoc
abstract mixin class $BleCyclingCadenceSampleCopyWith<$Res>  {
  factory $BleCyclingCadenceSampleCopyWith(BleCyclingCadenceSample value, $Res Function(BleCyclingCadenceSample) _then) = _$BleCyclingCadenceSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, int rpm
});




}
/// @nodoc
class _$BleCyclingCadenceSampleCopyWithImpl<$Res>
    implements $BleCyclingCadenceSampleCopyWith<$Res> {
  _$BleCyclingCadenceSampleCopyWithImpl(this._self, this._then);

  final BleCyclingCadenceSample _self;
  final $Res Function(BleCyclingCadenceSample) _then;

/// Create a copy of BleCyclingCadenceSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? rpm = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rpm: null == rpm ? _self.rpm : rpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [BleCyclingCadenceSample].
extension BleCyclingCadenceSamplePatterns on BleCyclingCadenceSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleCyclingCadenceSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleCyclingCadenceSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleCyclingCadenceSample value)  $default,){
final _that = this;
switch (_that) {
case _BleCyclingCadenceSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleCyclingCadenceSample value)?  $default,){
final _that = this;
switch (_that) {
case _BleCyclingCadenceSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int rpm)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleCyclingCadenceSample() when $default != null:
return $default(_that.time,_that.rpm);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int rpm)  $default,) {final _that = this;
switch (_that) {
case _BleCyclingCadenceSample():
return $default(_that.time,_that.rpm);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int rpm)?  $default,) {final _that = this;
switch (_that) {
case _BleCyclingCadenceSample() when $default != null:
return $default(_that.time,_that.rpm);case _:
  return null;

}
}

}

/// @nodoc


class _BleCyclingCadenceSample implements BleCyclingCadenceSample {
  const _BleCyclingCadenceSample({required this.time, required this.rpm});
  

@override final  DateTime time;
@override final  int rpm;

/// Create a copy of BleCyclingCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleCyclingCadenceSampleCopyWith<_BleCyclingCadenceSample> get copyWith => __$BleCyclingCadenceSampleCopyWithImpl<_BleCyclingCadenceSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleCyclingCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rpm, rpm) || other.rpm == rpm));
}


@override
int get hashCode => Object.hash(runtimeType,time,rpm);

@override
String toString() {
  return 'BleCyclingCadenceSample(time: $time, rpm: $rpm)';
}


}

/// @nodoc
abstract mixin class _$BleCyclingCadenceSampleCopyWith<$Res> implements $BleCyclingCadenceSampleCopyWith<$Res> {
  factory _$BleCyclingCadenceSampleCopyWith(_BleCyclingCadenceSample value, $Res Function(_BleCyclingCadenceSample) _then) = __$BleCyclingCadenceSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int rpm
});




}
/// @nodoc
class __$BleCyclingCadenceSampleCopyWithImpl<$Res>
    implements _$BleCyclingCadenceSampleCopyWith<$Res> {
  __$BleCyclingCadenceSampleCopyWithImpl(this._self, this._then);

  final _BleCyclingCadenceSample _self;
  final $Res Function(_BleCyclingCadenceSample) _then;

/// Create a copy of BleCyclingCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? rpm = null,}) {
  return _then(_BleCyclingCadenceSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rpm: null == rpm ? _self.rpm : rpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$BleSpeedSample {

 DateTime get time; double get metersPerSecond; bool get isRunning;
/// Create a copy of BleSpeedSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleSpeedSampleCopyWith<BleSpeedSample> get copyWith => _$BleSpeedSampleCopyWithImpl<BleSpeedSample>(this as BleSpeedSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleSpeedSample&&(identical(other.time, time) || other.time == time)&&(identical(other.metersPerSecond, metersPerSecond) || other.metersPerSecond == metersPerSecond)&&(identical(other.isRunning, isRunning) || other.isRunning == isRunning));
}


@override
int get hashCode => Object.hash(runtimeType,time,metersPerSecond,isRunning);

@override
String toString() {
  return 'BleSpeedSample(time: $time, metersPerSecond: $metersPerSecond, isRunning: $isRunning)';
}


}

/// @nodoc
abstract mixin class $BleSpeedSampleCopyWith<$Res>  {
  factory $BleSpeedSampleCopyWith(BleSpeedSample value, $Res Function(BleSpeedSample) _then) = _$BleSpeedSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, double metersPerSecond, bool isRunning
});




}
/// @nodoc
class _$BleSpeedSampleCopyWithImpl<$Res>
    implements $BleSpeedSampleCopyWith<$Res> {
  _$BleSpeedSampleCopyWithImpl(this._self, this._then);

  final BleSpeedSample _self;
  final $Res Function(BleSpeedSample) _then;

/// Create a copy of BleSpeedSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? metersPerSecond = null,Object? isRunning = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,metersPerSecond: null == metersPerSecond ? _self.metersPerSecond : metersPerSecond // ignore: cast_nullable_to_non_nullable
as double,isRunning: null == isRunning ? _self.isRunning : isRunning // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [BleSpeedSample].
extension BleSpeedSamplePatterns on BleSpeedSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleSpeedSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleSpeedSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleSpeedSample value)  $default,){
final _that = this;
switch (_that) {
case _BleSpeedSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleSpeedSample value)?  $default,){
final _that = this;
switch (_that) {
case _BleSpeedSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double metersPerSecond,  bool isRunning)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleSpeedSample() when $default != null:
return $default(_that.time,_that.metersPerSecond,_that.isRunning);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double metersPerSecond,  bool isRunning)  $default,) {final _that = this;
switch (_that) {
case _BleSpeedSample():
return $default(_that.time,_that.metersPerSecond,_that.isRunning);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double metersPerSecond,  bool isRunning)?  $default,) {final _that = this;
switch (_that) {
case _BleSpeedSample() when $default != null:
return $default(_that.time,_that.metersPerSecond,_that.isRunning);case _:
  return null;

}
}

}

/// @nodoc


class _BleSpeedSample implements BleSpeedSample {
  const _BleSpeedSample({required this.time, required this.metersPerSecond, required this.isRunning});
  

@override final  DateTime time;
@override final  double metersPerSecond;
@override final  bool isRunning;

/// Create a copy of BleSpeedSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleSpeedSampleCopyWith<_BleSpeedSample> get copyWith => __$BleSpeedSampleCopyWithImpl<_BleSpeedSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleSpeedSample&&(identical(other.time, time) || other.time == time)&&(identical(other.metersPerSecond, metersPerSecond) || other.metersPerSecond == metersPerSecond)&&(identical(other.isRunning, isRunning) || other.isRunning == isRunning));
}


@override
int get hashCode => Object.hash(runtimeType,time,metersPerSecond,isRunning);

@override
String toString() {
  return 'BleSpeedSample(time: $time, metersPerSecond: $metersPerSecond, isRunning: $isRunning)';
}


}

/// @nodoc
abstract mixin class _$BleSpeedSampleCopyWith<$Res> implements $BleSpeedSampleCopyWith<$Res> {
  factory _$BleSpeedSampleCopyWith(_BleSpeedSample value, $Res Function(_BleSpeedSample) _then) = __$BleSpeedSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double metersPerSecond, bool isRunning
});




}
/// @nodoc
class __$BleSpeedSampleCopyWithImpl<$Res>
    implements _$BleSpeedSampleCopyWith<$Res> {
  __$BleSpeedSampleCopyWithImpl(this._self, this._then);

  final _BleSpeedSample _self;
  final $Res Function(_BleSpeedSample) _then;

/// Create a copy of BleSpeedSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? metersPerSecond = null,Object? isRunning = null,}) {
  return _then(_BleSpeedSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,metersPerSecond: null == metersPerSecond ? _self.metersPerSecond : metersPerSecond // ignore: cast_nullable_to_non_nullable
as double,isRunning: null == isRunning ? _self.isRunning : isRunning // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$BleStepsCadenceSample {

 DateTime get time; int get stepsPerMinute;
/// Create a copy of BleStepsCadenceSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleStepsCadenceSampleCopyWith<BleStepsCadenceSample> get copyWith => _$BleStepsCadenceSampleCopyWithImpl<BleStepsCadenceSample>(this as BleStepsCadenceSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleStepsCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.stepsPerMinute, stepsPerMinute) || other.stepsPerMinute == stepsPerMinute));
}


@override
int get hashCode => Object.hash(runtimeType,time,stepsPerMinute);

@override
String toString() {
  return 'BleStepsCadenceSample(time: $time, stepsPerMinute: $stepsPerMinute)';
}


}

/// @nodoc
abstract mixin class $BleStepsCadenceSampleCopyWith<$Res>  {
  factory $BleStepsCadenceSampleCopyWith(BleStepsCadenceSample value, $Res Function(BleStepsCadenceSample) _then) = _$BleStepsCadenceSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, int stepsPerMinute
});




}
/// @nodoc
class _$BleStepsCadenceSampleCopyWithImpl<$Res>
    implements $BleStepsCadenceSampleCopyWith<$Res> {
  _$BleStepsCadenceSampleCopyWithImpl(this._self, this._then);

  final BleStepsCadenceSample _self;
  final $Res Function(BleStepsCadenceSample) _then;

/// Create a copy of BleStepsCadenceSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? stepsPerMinute = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,stepsPerMinute: null == stepsPerMinute ? _self.stepsPerMinute : stepsPerMinute // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [BleStepsCadenceSample].
extension BleStepsCadenceSamplePatterns on BleStepsCadenceSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleStepsCadenceSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleStepsCadenceSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleStepsCadenceSample value)  $default,){
final _that = this;
switch (_that) {
case _BleStepsCadenceSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleStepsCadenceSample value)?  $default,){
final _that = this;
switch (_that) {
case _BleStepsCadenceSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int stepsPerMinute)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleStepsCadenceSample() when $default != null:
return $default(_that.time,_that.stepsPerMinute);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int stepsPerMinute)  $default,) {final _that = this;
switch (_that) {
case _BleStepsCadenceSample():
return $default(_that.time,_that.stepsPerMinute);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int stepsPerMinute)?  $default,) {final _that = this;
switch (_that) {
case _BleStepsCadenceSample() when $default != null:
return $default(_that.time,_that.stepsPerMinute);case _:
  return null;

}
}

}

/// @nodoc


class _BleStepsCadenceSample implements BleStepsCadenceSample {
  const _BleStepsCadenceSample({required this.time, required this.stepsPerMinute});
  

@override final  DateTime time;
@override final  int stepsPerMinute;

/// Create a copy of BleStepsCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleStepsCadenceSampleCopyWith<_BleStepsCadenceSample> get copyWith => __$BleStepsCadenceSampleCopyWithImpl<_BleStepsCadenceSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleStepsCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.stepsPerMinute, stepsPerMinute) || other.stepsPerMinute == stepsPerMinute));
}


@override
int get hashCode => Object.hash(runtimeType,time,stepsPerMinute);

@override
String toString() {
  return 'BleStepsCadenceSample(time: $time, stepsPerMinute: $stepsPerMinute)';
}


}

/// @nodoc
abstract mixin class _$BleStepsCadenceSampleCopyWith<$Res> implements $BleStepsCadenceSampleCopyWith<$Res> {
  factory _$BleStepsCadenceSampleCopyWith(_BleStepsCadenceSample value, $Res Function(_BleStepsCadenceSample) _then) = __$BleStepsCadenceSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int stepsPerMinute
});




}
/// @nodoc
class __$BleStepsCadenceSampleCopyWithImpl<$Res>
    implements _$BleStepsCadenceSampleCopyWith<$Res> {
  __$BleStepsCadenceSampleCopyWithImpl(this._self, this._then);

  final _BleStepsCadenceSample _self;
  final $Res Function(_BleStepsCadenceSample) _then;

/// Create a copy of BleStepsCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? stepsPerMinute = null,}) {
  return _then(_BleStepsCadenceSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,stepsPerMinute: null == stepsPerMinute ? _self.stepsPerMinute : stepsPerMinute // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$BleRecordingSampleBuffer {

 List<BleHeartRateSample> get heartRateSamples; List<BlePowerSample> get powerSamples; List<BleCyclingCadenceSample> get cyclingCadenceSamples; List<BleSpeedSample> get speedSamples; List<BleStepsCadenceSample> get stepsCadenceSamples;
/// Create a copy of BleRecordingSampleBuffer
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<BleRecordingSampleBuffer> get copyWith => _$BleRecordingSampleBufferCopyWithImpl<BleRecordingSampleBuffer>(this as BleRecordingSampleBuffer, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleRecordingSampleBuffer&&const DeepCollectionEquality().equals(other.heartRateSamples, heartRateSamples)&&const DeepCollectionEquality().equals(other.powerSamples, powerSamples)&&const DeepCollectionEquality().equals(other.cyclingCadenceSamples, cyclingCadenceSamples)&&const DeepCollectionEquality().equals(other.speedSamples, speedSamples)&&const DeepCollectionEquality().equals(other.stepsCadenceSamples, stepsCadenceSamples));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(heartRateSamples),const DeepCollectionEquality().hash(powerSamples),const DeepCollectionEquality().hash(cyclingCadenceSamples),const DeepCollectionEquality().hash(speedSamples),const DeepCollectionEquality().hash(stepsCadenceSamples));

@override
String toString() {
  return 'BleRecordingSampleBuffer(heartRateSamples: $heartRateSamples, powerSamples: $powerSamples, cyclingCadenceSamples: $cyclingCadenceSamples, speedSamples: $speedSamples, stepsCadenceSamples: $stepsCadenceSamples)';
}


}

/// @nodoc
abstract mixin class $BleRecordingSampleBufferCopyWith<$Res>  {
  factory $BleRecordingSampleBufferCopyWith(BleRecordingSampleBuffer value, $Res Function(BleRecordingSampleBuffer) _then) = _$BleRecordingSampleBufferCopyWithImpl;
@useResult
$Res call({
 List<BleHeartRateSample> heartRateSamples, List<BlePowerSample> powerSamples, List<BleCyclingCadenceSample> cyclingCadenceSamples, List<BleSpeedSample> speedSamples, List<BleStepsCadenceSample> stepsCadenceSamples
});




}
/// @nodoc
class _$BleRecordingSampleBufferCopyWithImpl<$Res>
    implements $BleRecordingSampleBufferCopyWith<$Res> {
  _$BleRecordingSampleBufferCopyWithImpl(this._self, this._then);

  final BleRecordingSampleBuffer _self;
  final $Res Function(BleRecordingSampleBuffer) _then;

/// Create a copy of BleRecordingSampleBuffer
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? heartRateSamples = null,Object? powerSamples = null,Object? cyclingCadenceSamples = null,Object? speedSamples = null,Object? stepsCadenceSamples = null,}) {
  return _then(_self.copyWith(
heartRateSamples: null == heartRateSamples ? _self.heartRateSamples : heartRateSamples // ignore: cast_nullable_to_non_nullable
as List<BleHeartRateSample>,powerSamples: null == powerSamples ? _self.powerSamples : powerSamples // ignore: cast_nullable_to_non_nullable
as List<BlePowerSample>,cyclingCadenceSamples: null == cyclingCadenceSamples ? _self.cyclingCadenceSamples : cyclingCadenceSamples // ignore: cast_nullable_to_non_nullable
as List<BleCyclingCadenceSample>,speedSamples: null == speedSamples ? _self.speedSamples : speedSamples // ignore: cast_nullable_to_non_nullable
as List<BleSpeedSample>,stepsCadenceSamples: null == stepsCadenceSamples ? _self.stepsCadenceSamples : stepsCadenceSamples // ignore: cast_nullable_to_non_nullable
as List<BleStepsCadenceSample>,
  ));
}

}


/// Adds pattern-matching-related methods to [BleRecordingSampleBuffer].
extension BleRecordingSampleBufferPatterns on BleRecordingSampleBuffer {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleRecordingSampleBuffer value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleRecordingSampleBuffer value)  $default,){
final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleRecordingSampleBuffer value)?  $default,){
final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<BleHeartRateSample> heartRateSamples,  List<BlePowerSample> powerSamples,  List<BleCyclingCadenceSample> cyclingCadenceSamples,  List<BleSpeedSample> speedSamples,  List<BleStepsCadenceSample> stepsCadenceSamples)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer() when $default != null:
return $default(_that.heartRateSamples,_that.powerSamples,_that.cyclingCadenceSamples,_that.speedSamples,_that.stepsCadenceSamples);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<BleHeartRateSample> heartRateSamples,  List<BlePowerSample> powerSamples,  List<BleCyclingCadenceSample> cyclingCadenceSamples,  List<BleSpeedSample> speedSamples,  List<BleStepsCadenceSample> stepsCadenceSamples)  $default,) {final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer():
return $default(_that.heartRateSamples,_that.powerSamples,_that.cyclingCadenceSamples,_that.speedSamples,_that.stepsCadenceSamples);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<BleHeartRateSample> heartRateSamples,  List<BlePowerSample> powerSamples,  List<BleCyclingCadenceSample> cyclingCadenceSamples,  List<BleSpeedSample> speedSamples,  List<BleStepsCadenceSample> stepsCadenceSamples)?  $default,) {final _that = this;
switch (_that) {
case _BleRecordingSampleBuffer() when $default != null:
return $default(_that.heartRateSamples,_that.powerSamples,_that.cyclingCadenceSamples,_that.speedSamples,_that.stepsCadenceSamples);case _:
  return null;

}
}

}

/// @nodoc


class _BleRecordingSampleBuffer extends BleRecordingSampleBuffer {
  const _BleRecordingSampleBuffer({final  List<BleHeartRateSample> heartRateSamples = const <BleHeartRateSample>[], final  List<BlePowerSample> powerSamples = const <BlePowerSample>[], final  List<BleCyclingCadenceSample> cyclingCadenceSamples = const <BleCyclingCadenceSample>[], final  List<BleSpeedSample> speedSamples = const <BleSpeedSample>[], final  List<BleStepsCadenceSample> stepsCadenceSamples = const <BleStepsCadenceSample>[]}): _heartRateSamples = heartRateSamples,_powerSamples = powerSamples,_cyclingCadenceSamples = cyclingCadenceSamples,_speedSamples = speedSamples,_stepsCadenceSamples = stepsCadenceSamples,super._();
  

 final  List<BleHeartRateSample> _heartRateSamples;
@override@JsonKey() List<BleHeartRateSample> get heartRateSamples {
  if (_heartRateSamples is EqualUnmodifiableListView) return _heartRateSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_heartRateSamples);
}

 final  List<BlePowerSample> _powerSamples;
@override@JsonKey() List<BlePowerSample> get powerSamples {
  if (_powerSamples is EqualUnmodifiableListView) return _powerSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_powerSamples);
}

 final  List<BleCyclingCadenceSample> _cyclingCadenceSamples;
@override@JsonKey() List<BleCyclingCadenceSample> get cyclingCadenceSamples {
  if (_cyclingCadenceSamples is EqualUnmodifiableListView) return _cyclingCadenceSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cyclingCadenceSamples);
}

 final  List<BleSpeedSample> _speedSamples;
@override@JsonKey() List<BleSpeedSample> get speedSamples {
  if (_speedSamples is EqualUnmodifiableListView) return _speedSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_speedSamples);
}

 final  List<BleStepsCadenceSample> _stepsCadenceSamples;
@override@JsonKey() List<BleStepsCadenceSample> get stepsCadenceSamples {
  if (_stepsCadenceSamples is EqualUnmodifiableListView) return _stepsCadenceSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stepsCadenceSamples);
}


/// Create a copy of BleRecordingSampleBuffer
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleRecordingSampleBufferCopyWith<_BleRecordingSampleBuffer> get copyWith => __$BleRecordingSampleBufferCopyWithImpl<_BleRecordingSampleBuffer>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleRecordingSampleBuffer&&const DeepCollectionEquality().equals(other._heartRateSamples, _heartRateSamples)&&const DeepCollectionEquality().equals(other._powerSamples, _powerSamples)&&const DeepCollectionEquality().equals(other._cyclingCadenceSamples, _cyclingCadenceSamples)&&const DeepCollectionEquality().equals(other._speedSamples, _speedSamples)&&const DeepCollectionEquality().equals(other._stepsCadenceSamples, _stepsCadenceSamples));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_heartRateSamples),const DeepCollectionEquality().hash(_powerSamples),const DeepCollectionEquality().hash(_cyclingCadenceSamples),const DeepCollectionEquality().hash(_speedSamples),const DeepCollectionEquality().hash(_stepsCadenceSamples));

@override
String toString() {
  return 'BleRecordingSampleBuffer(heartRateSamples: $heartRateSamples, powerSamples: $powerSamples, cyclingCadenceSamples: $cyclingCadenceSamples, speedSamples: $speedSamples, stepsCadenceSamples: $stepsCadenceSamples)';
}


}

/// @nodoc
abstract mixin class _$BleRecordingSampleBufferCopyWith<$Res> implements $BleRecordingSampleBufferCopyWith<$Res> {
  factory _$BleRecordingSampleBufferCopyWith(_BleRecordingSampleBuffer value, $Res Function(_BleRecordingSampleBuffer) _then) = __$BleRecordingSampleBufferCopyWithImpl;
@override @useResult
$Res call({
 List<BleHeartRateSample> heartRateSamples, List<BlePowerSample> powerSamples, List<BleCyclingCadenceSample> cyclingCadenceSamples, List<BleSpeedSample> speedSamples, List<BleStepsCadenceSample> stepsCadenceSamples
});




}
/// @nodoc
class __$BleRecordingSampleBufferCopyWithImpl<$Res>
    implements _$BleRecordingSampleBufferCopyWith<$Res> {
  __$BleRecordingSampleBufferCopyWithImpl(this._self, this._then);

  final _BleRecordingSampleBuffer _self;
  final $Res Function(_BleRecordingSampleBuffer) _then;

/// Create a copy of BleRecordingSampleBuffer
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? heartRateSamples = null,Object? powerSamples = null,Object? cyclingCadenceSamples = null,Object? speedSamples = null,Object? stepsCadenceSamples = null,}) {
  return _then(_BleRecordingSampleBuffer(
heartRateSamples: null == heartRateSamples ? _self._heartRateSamples : heartRateSamples // ignore: cast_nullable_to_non_nullable
as List<BleHeartRateSample>,powerSamples: null == powerSamples ? _self._powerSamples : powerSamples // ignore: cast_nullable_to_non_nullable
as List<BlePowerSample>,cyclingCadenceSamples: null == cyclingCadenceSamples ? _self._cyclingCadenceSamples : cyclingCadenceSamples // ignore: cast_nullable_to_non_nullable
as List<BleCyclingCadenceSample>,speedSamples: null == speedSamples ? _self._speedSamples : speedSamples // ignore: cast_nullable_to_non_nullable
as List<BleSpeedSample>,stepsCadenceSamples: null == stepsCadenceSamples ? _self._stepsCadenceSamples : stepsCadenceSamples // ignore: cast_nullable_to_non_nullable
as List<BleStepsCadenceSample>,
  ));
}


}

/// @nodoc
mixin _$BleDiscoveredDevice {

 String get address; String? get name; int? get rssi; Set<BleSensorCapability> get suggestedCapabilities;
/// Create a copy of BleDiscoveredDevice
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleDiscoveredDeviceCopyWith<BleDiscoveredDevice> get copyWith => _$BleDiscoveredDeviceCopyWithImpl<BleDiscoveredDevice>(this as BleDiscoveredDevice, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleDiscoveredDevice&&(identical(other.address, address) || other.address == address)&&(identical(other.name, name) || other.name == name)&&(identical(other.rssi, rssi) || other.rssi == rssi)&&const DeepCollectionEquality().equals(other.suggestedCapabilities, suggestedCapabilities));
}


@override
int get hashCode => Object.hash(runtimeType,address,name,rssi,const DeepCollectionEquality().hash(suggestedCapabilities));

@override
String toString() {
  return 'BleDiscoveredDevice(address: $address, name: $name, rssi: $rssi, suggestedCapabilities: $suggestedCapabilities)';
}


}

/// @nodoc
abstract mixin class $BleDiscoveredDeviceCopyWith<$Res>  {
  factory $BleDiscoveredDeviceCopyWith(BleDiscoveredDevice value, $Res Function(BleDiscoveredDevice) _then) = _$BleDiscoveredDeviceCopyWithImpl;
@useResult
$Res call({
 String address, String? name, int? rssi, Set<BleSensorCapability> suggestedCapabilities
});




}
/// @nodoc
class _$BleDiscoveredDeviceCopyWithImpl<$Res>
    implements $BleDiscoveredDeviceCopyWith<$Res> {
  _$BleDiscoveredDeviceCopyWithImpl(this._self, this._then);

  final BleDiscoveredDevice _self;
  final $Res Function(BleDiscoveredDevice) _then;

/// Create a copy of BleDiscoveredDevice
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? address = null,Object? name = freezed,Object? rssi = freezed,Object? suggestedCapabilities = null,}) {
  return _then(_self.copyWith(
address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,rssi: freezed == rssi ? _self.rssi : rssi // ignore: cast_nullable_to_non_nullable
as int?,suggestedCapabilities: null == suggestedCapabilities ? _self.suggestedCapabilities : suggestedCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,
  ));
}

}


/// Adds pattern-matching-related methods to [BleDiscoveredDevice].
extension BleDiscoveredDevicePatterns on BleDiscoveredDevice {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleDiscoveredDevice value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleDiscoveredDevice() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleDiscoveredDevice value)  $default,){
final _that = this;
switch (_that) {
case _BleDiscoveredDevice():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleDiscoveredDevice value)?  $default,){
final _that = this;
switch (_that) {
case _BleDiscoveredDevice() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String address,  String? name,  int? rssi,  Set<BleSensorCapability> suggestedCapabilities)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleDiscoveredDevice() when $default != null:
return $default(_that.address,_that.name,_that.rssi,_that.suggestedCapabilities);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String address,  String? name,  int? rssi,  Set<BleSensorCapability> suggestedCapabilities)  $default,) {final _that = this;
switch (_that) {
case _BleDiscoveredDevice():
return $default(_that.address,_that.name,_that.rssi,_that.suggestedCapabilities);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String address,  String? name,  int? rssi,  Set<BleSensorCapability> suggestedCapabilities)?  $default,) {final _that = this;
switch (_that) {
case _BleDiscoveredDevice() when $default != null:
return $default(_that.address,_that.name,_that.rssi,_that.suggestedCapabilities);case _:
  return null;

}
}

}

/// @nodoc


class _BleDiscoveredDevice implements BleDiscoveredDevice {
  const _BleDiscoveredDevice({required this.address, required this.name, required this.rssi, required final  Set<BleSensorCapability> suggestedCapabilities}): _suggestedCapabilities = suggestedCapabilities;
  

@override final  String address;
@override final  String? name;
@override final  int? rssi;
 final  Set<BleSensorCapability> _suggestedCapabilities;
@override Set<BleSensorCapability> get suggestedCapabilities {
  if (_suggestedCapabilities is EqualUnmodifiableSetView) return _suggestedCapabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_suggestedCapabilities);
}


/// Create a copy of BleDiscoveredDevice
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleDiscoveredDeviceCopyWith<_BleDiscoveredDevice> get copyWith => __$BleDiscoveredDeviceCopyWithImpl<_BleDiscoveredDevice>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleDiscoveredDevice&&(identical(other.address, address) || other.address == address)&&(identical(other.name, name) || other.name == name)&&(identical(other.rssi, rssi) || other.rssi == rssi)&&const DeepCollectionEquality().equals(other._suggestedCapabilities, _suggestedCapabilities));
}


@override
int get hashCode => Object.hash(runtimeType,address,name,rssi,const DeepCollectionEquality().hash(_suggestedCapabilities));

@override
String toString() {
  return 'BleDiscoveredDevice(address: $address, name: $name, rssi: $rssi, suggestedCapabilities: $suggestedCapabilities)';
}


}

/// @nodoc
abstract mixin class _$BleDiscoveredDeviceCopyWith<$Res> implements $BleDiscoveredDeviceCopyWith<$Res> {
  factory _$BleDiscoveredDeviceCopyWith(_BleDiscoveredDevice value, $Res Function(_BleDiscoveredDevice) _then) = __$BleDiscoveredDeviceCopyWithImpl;
@override @useResult
$Res call({
 String address, String? name, int? rssi, Set<BleSensorCapability> suggestedCapabilities
});




}
/// @nodoc
class __$BleDiscoveredDeviceCopyWithImpl<$Res>
    implements _$BleDiscoveredDeviceCopyWith<$Res> {
  __$BleDiscoveredDeviceCopyWithImpl(this._self, this._then);

  final _BleDiscoveredDevice _self;
  final $Res Function(_BleDiscoveredDevice) _then;

/// Create a copy of BleDiscoveredDevice
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? address = null,Object? name = freezed,Object? rssi = freezed,Object? suggestedCapabilities = null,}) {
  return _then(_BleDiscoveredDevice(
address: null == address ? _self.address : address // ignore: cast_nullable_to_non_nullable
as String,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,rssi: freezed == rssi ? _self.rssi : rssi // ignore: cast_nullable_to_non_nullable
as int?,suggestedCapabilities: null == suggestedCapabilities ? _self._suggestedCapabilities : suggestedCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,
  ));
}


}

// dart format on
