// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'ble_devices_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BleDevicesUiState {

 List<BleSensorDevice> get devices; List<BleDiscoveredDevice> get discoveredDevices; bool get isScanning; bool get showAllDevices; BleDiscoveredDevice? get selectedDevice; Set<BleSensorCapability> get discoveredCapabilities; bool get isDiscoveringCapabilities; String get addDisplayName; Set<BleSensorCapability> get addCapabilities; String get addWheelCircumferenceMm; Map<BleSensorCapability, BleSensorDevice> get capabilityConflicts; String? get editingDeviceId; String get editDisplayName; Set<BleSensorCapability> get editCapabilities; bool get editEnabled; String get editWheelCircumferenceMm; String? get errorMessage; bool get showAddFlow;
/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BleDevicesUiStateCopyWith<BleDevicesUiState> get copyWith => _$BleDevicesUiStateCopyWithImpl<BleDevicesUiState>(this as BleDevicesUiState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BleDevicesUiState&&const DeepCollectionEquality().equals(other.devices, devices)&&const DeepCollectionEquality().equals(other.discoveredDevices, discoveredDevices)&&(identical(other.isScanning, isScanning) || other.isScanning == isScanning)&&(identical(other.showAllDevices, showAllDevices) || other.showAllDevices == showAllDevices)&&(identical(other.selectedDevice, selectedDevice) || other.selectedDevice == selectedDevice)&&const DeepCollectionEquality().equals(other.discoveredCapabilities, discoveredCapabilities)&&(identical(other.isDiscoveringCapabilities, isDiscoveringCapabilities) || other.isDiscoveringCapabilities == isDiscoveringCapabilities)&&(identical(other.addDisplayName, addDisplayName) || other.addDisplayName == addDisplayName)&&const DeepCollectionEquality().equals(other.addCapabilities, addCapabilities)&&(identical(other.addWheelCircumferenceMm, addWheelCircumferenceMm) || other.addWheelCircumferenceMm == addWheelCircumferenceMm)&&const DeepCollectionEquality().equals(other.capabilityConflicts, capabilityConflicts)&&(identical(other.editingDeviceId, editingDeviceId) || other.editingDeviceId == editingDeviceId)&&(identical(other.editDisplayName, editDisplayName) || other.editDisplayName == editDisplayName)&&const DeepCollectionEquality().equals(other.editCapabilities, editCapabilities)&&(identical(other.editEnabled, editEnabled) || other.editEnabled == editEnabled)&&(identical(other.editWheelCircumferenceMm, editWheelCircumferenceMm) || other.editWheelCircumferenceMm == editWheelCircumferenceMm)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage)&&(identical(other.showAddFlow, showAddFlow) || other.showAddFlow == showAddFlow));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(devices),const DeepCollectionEquality().hash(discoveredDevices),isScanning,showAllDevices,selectedDevice,const DeepCollectionEquality().hash(discoveredCapabilities),isDiscoveringCapabilities,addDisplayName,const DeepCollectionEquality().hash(addCapabilities),addWheelCircumferenceMm,const DeepCollectionEquality().hash(capabilityConflicts),editingDeviceId,editDisplayName,const DeepCollectionEquality().hash(editCapabilities),editEnabled,editWheelCircumferenceMm,errorMessage,showAddFlow);

@override
String toString() {
  return 'BleDevicesUiState(devices: $devices, discoveredDevices: $discoveredDevices, isScanning: $isScanning, showAllDevices: $showAllDevices, selectedDevice: $selectedDevice, discoveredCapabilities: $discoveredCapabilities, isDiscoveringCapabilities: $isDiscoveringCapabilities, addDisplayName: $addDisplayName, addCapabilities: $addCapabilities, addWheelCircumferenceMm: $addWheelCircumferenceMm, capabilityConflicts: $capabilityConflicts, editingDeviceId: $editingDeviceId, editDisplayName: $editDisplayName, editCapabilities: $editCapabilities, editEnabled: $editEnabled, editWheelCircumferenceMm: $editWheelCircumferenceMm, errorMessage: $errorMessage, showAddFlow: $showAddFlow)';
}


}

/// @nodoc
abstract mixin class $BleDevicesUiStateCopyWith<$Res>  {
  factory $BleDevicesUiStateCopyWith(BleDevicesUiState value, $Res Function(BleDevicesUiState) _then) = _$BleDevicesUiStateCopyWithImpl;
@useResult
$Res call({
 List<BleSensorDevice> devices, List<BleDiscoveredDevice> discoveredDevices, bool isScanning, bool showAllDevices, BleDiscoveredDevice? selectedDevice, Set<BleSensorCapability> discoveredCapabilities, bool isDiscoveringCapabilities, String addDisplayName, Set<BleSensorCapability> addCapabilities, String addWheelCircumferenceMm, Map<BleSensorCapability, BleSensorDevice> capabilityConflicts, String? editingDeviceId, String editDisplayName, Set<BleSensorCapability> editCapabilities, bool editEnabled, String editWheelCircumferenceMm, String? errorMessage, bool showAddFlow
});


$BleDiscoveredDeviceCopyWith<$Res>? get selectedDevice;

}
/// @nodoc
class _$BleDevicesUiStateCopyWithImpl<$Res>
    implements $BleDevicesUiStateCopyWith<$Res> {
  _$BleDevicesUiStateCopyWithImpl(this._self, this._then);

  final BleDevicesUiState _self;
  final $Res Function(BleDevicesUiState) _then;

/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? devices = null,Object? discoveredDevices = null,Object? isScanning = null,Object? showAllDevices = null,Object? selectedDevice = freezed,Object? discoveredCapabilities = null,Object? isDiscoveringCapabilities = null,Object? addDisplayName = null,Object? addCapabilities = null,Object? addWheelCircumferenceMm = null,Object? capabilityConflicts = null,Object? editingDeviceId = freezed,Object? editDisplayName = null,Object? editCapabilities = null,Object? editEnabled = null,Object? editWheelCircumferenceMm = null,Object? errorMessage = freezed,Object? showAddFlow = null,}) {
  return _then(_self.copyWith(
devices: null == devices ? _self.devices : devices // ignore: cast_nullable_to_non_nullable
as List<BleSensorDevice>,discoveredDevices: null == discoveredDevices ? _self.discoveredDevices : discoveredDevices // ignore: cast_nullable_to_non_nullable
as List<BleDiscoveredDevice>,isScanning: null == isScanning ? _self.isScanning : isScanning // ignore: cast_nullable_to_non_nullable
as bool,showAllDevices: null == showAllDevices ? _self.showAllDevices : showAllDevices // ignore: cast_nullable_to_non_nullable
as bool,selectedDevice: freezed == selectedDevice ? _self.selectedDevice : selectedDevice // ignore: cast_nullable_to_non_nullable
as BleDiscoveredDevice?,discoveredCapabilities: null == discoveredCapabilities ? _self.discoveredCapabilities : discoveredCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,isDiscoveringCapabilities: null == isDiscoveringCapabilities ? _self.isDiscoveringCapabilities : isDiscoveringCapabilities // ignore: cast_nullable_to_non_nullable
as bool,addDisplayName: null == addDisplayName ? _self.addDisplayName : addDisplayName // ignore: cast_nullable_to_non_nullable
as String,addCapabilities: null == addCapabilities ? _self.addCapabilities : addCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,addWheelCircumferenceMm: null == addWheelCircumferenceMm ? _self.addWheelCircumferenceMm : addWheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as String,capabilityConflicts: null == capabilityConflicts ? _self.capabilityConflicts : capabilityConflicts // ignore: cast_nullable_to_non_nullable
as Map<BleSensorCapability, BleSensorDevice>,editingDeviceId: freezed == editingDeviceId ? _self.editingDeviceId : editingDeviceId // ignore: cast_nullable_to_non_nullable
as String?,editDisplayName: null == editDisplayName ? _self.editDisplayName : editDisplayName // ignore: cast_nullable_to_non_nullable
as String,editCapabilities: null == editCapabilities ? _self.editCapabilities : editCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,editEnabled: null == editEnabled ? _self.editEnabled : editEnabled // ignore: cast_nullable_to_non_nullable
as bool,editWheelCircumferenceMm: null == editWheelCircumferenceMm ? _self.editWheelCircumferenceMm : editWheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as String,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,showAddFlow: null == showAddFlow ? _self.showAddFlow : showAddFlow // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}
/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleDiscoveredDeviceCopyWith<$Res>? get selectedDevice {
    if (_self.selectedDevice == null) {
    return null;
  }

  return $BleDiscoveredDeviceCopyWith<$Res>(_self.selectedDevice!, (value) {
    return _then(_self.copyWith(selectedDevice: value));
  });
}
}


/// Adds pattern-matching-related methods to [BleDevicesUiState].
extension BleDevicesUiStatePatterns on BleDevicesUiState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BleDevicesUiState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BleDevicesUiState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BleDevicesUiState value)  $default,){
final _that = this;
switch (_that) {
case _BleDevicesUiState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BleDevicesUiState value)?  $default,){
final _that = this;
switch (_that) {
case _BleDevicesUiState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<BleSensorDevice> devices,  List<BleDiscoveredDevice> discoveredDevices,  bool isScanning,  bool showAllDevices,  BleDiscoveredDevice? selectedDevice,  Set<BleSensorCapability> discoveredCapabilities,  bool isDiscoveringCapabilities,  String addDisplayName,  Set<BleSensorCapability> addCapabilities,  String addWheelCircumferenceMm,  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts,  String? editingDeviceId,  String editDisplayName,  Set<BleSensorCapability> editCapabilities,  bool editEnabled,  String editWheelCircumferenceMm,  String? errorMessage,  bool showAddFlow)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BleDevicesUiState() when $default != null:
return $default(_that.devices,_that.discoveredDevices,_that.isScanning,_that.showAllDevices,_that.selectedDevice,_that.discoveredCapabilities,_that.isDiscoveringCapabilities,_that.addDisplayName,_that.addCapabilities,_that.addWheelCircumferenceMm,_that.capabilityConflicts,_that.editingDeviceId,_that.editDisplayName,_that.editCapabilities,_that.editEnabled,_that.editWheelCircumferenceMm,_that.errorMessage,_that.showAddFlow);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<BleSensorDevice> devices,  List<BleDiscoveredDevice> discoveredDevices,  bool isScanning,  bool showAllDevices,  BleDiscoveredDevice? selectedDevice,  Set<BleSensorCapability> discoveredCapabilities,  bool isDiscoveringCapabilities,  String addDisplayName,  Set<BleSensorCapability> addCapabilities,  String addWheelCircumferenceMm,  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts,  String? editingDeviceId,  String editDisplayName,  Set<BleSensorCapability> editCapabilities,  bool editEnabled,  String editWheelCircumferenceMm,  String? errorMessage,  bool showAddFlow)  $default,) {final _that = this;
switch (_that) {
case _BleDevicesUiState():
return $default(_that.devices,_that.discoveredDevices,_that.isScanning,_that.showAllDevices,_that.selectedDevice,_that.discoveredCapabilities,_that.isDiscoveringCapabilities,_that.addDisplayName,_that.addCapabilities,_that.addWheelCircumferenceMm,_that.capabilityConflicts,_that.editingDeviceId,_that.editDisplayName,_that.editCapabilities,_that.editEnabled,_that.editWheelCircumferenceMm,_that.errorMessage,_that.showAddFlow);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<BleSensorDevice> devices,  List<BleDiscoveredDevice> discoveredDevices,  bool isScanning,  bool showAllDevices,  BleDiscoveredDevice? selectedDevice,  Set<BleSensorCapability> discoveredCapabilities,  bool isDiscoveringCapabilities,  String addDisplayName,  Set<BleSensorCapability> addCapabilities,  String addWheelCircumferenceMm,  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts,  String? editingDeviceId,  String editDisplayName,  Set<BleSensorCapability> editCapabilities,  bool editEnabled,  String editWheelCircumferenceMm,  String? errorMessage,  bool showAddFlow)?  $default,) {final _that = this;
switch (_that) {
case _BleDevicesUiState() when $default != null:
return $default(_that.devices,_that.discoveredDevices,_that.isScanning,_that.showAllDevices,_that.selectedDevice,_that.discoveredCapabilities,_that.isDiscoveringCapabilities,_that.addDisplayName,_that.addCapabilities,_that.addWheelCircumferenceMm,_that.capabilityConflicts,_that.editingDeviceId,_that.editDisplayName,_that.editCapabilities,_that.editEnabled,_that.editWheelCircumferenceMm,_that.errorMessage,_that.showAddFlow);case _:
  return null;

}
}

}

/// @nodoc


class _BleDevicesUiState extends BleDevicesUiState {
  const _BleDevicesUiState({final  List<BleSensorDevice> devices = const <BleSensorDevice>[], final  List<BleDiscoveredDevice> discoveredDevices = const <BleDiscoveredDevice>[], this.isScanning = false, this.showAllDevices = false, this.selectedDevice, final  Set<BleSensorCapability> discoveredCapabilities = const <BleSensorCapability>{}, this.isDiscoveringCapabilities = false, this.addDisplayName = '', final  Set<BleSensorCapability> addCapabilities = const <BleSensorCapability>{}, this.addWheelCircumferenceMm = '', final  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts = const <BleSensorCapability, BleSensorDevice>{}, this.editingDeviceId, this.editDisplayName = '', final  Set<BleSensorCapability> editCapabilities = const <BleSensorCapability>{}, this.editEnabled = true, this.editWheelCircumferenceMm = '', this.errorMessage, this.showAddFlow = false}): _devices = devices,_discoveredDevices = discoveredDevices,_discoveredCapabilities = discoveredCapabilities,_addCapabilities = addCapabilities,_capabilityConflicts = capabilityConflicts,_editCapabilities = editCapabilities,super._();
  

 final  List<BleSensorDevice> _devices;
@override@JsonKey() List<BleSensorDevice> get devices {
  if (_devices is EqualUnmodifiableListView) return _devices;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_devices);
}

 final  List<BleDiscoveredDevice> _discoveredDevices;
@override@JsonKey() List<BleDiscoveredDevice> get discoveredDevices {
  if (_discoveredDevices is EqualUnmodifiableListView) return _discoveredDevices;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_discoveredDevices);
}

@override@JsonKey() final  bool isScanning;
@override@JsonKey() final  bool showAllDevices;
@override final  BleDiscoveredDevice? selectedDevice;
 final  Set<BleSensorCapability> _discoveredCapabilities;
@override@JsonKey() Set<BleSensorCapability> get discoveredCapabilities {
  if (_discoveredCapabilities is EqualUnmodifiableSetView) return _discoveredCapabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_discoveredCapabilities);
}

@override@JsonKey() final  bool isDiscoveringCapabilities;
@override@JsonKey() final  String addDisplayName;
 final  Set<BleSensorCapability> _addCapabilities;
@override@JsonKey() Set<BleSensorCapability> get addCapabilities {
  if (_addCapabilities is EqualUnmodifiableSetView) return _addCapabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_addCapabilities);
}

@override@JsonKey() final  String addWheelCircumferenceMm;
 final  Map<BleSensorCapability, BleSensorDevice> _capabilityConflicts;
@override@JsonKey() Map<BleSensorCapability, BleSensorDevice> get capabilityConflicts {
  if (_capabilityConflicts is EqualUnmodifiableMapView) return _capabilityConflicts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_capabilityConflicts);
}

@override final  String? editingDeviceId;
@override@JsonKey() final  String editDisplayName;
 final  Set<BleSensorCapability> _editCapabilities;
@override@JsonKey() Set<BleSensorCapability> get editCapabilities {
  if (_editCapabilities is EqualUnmodifiableSetView) return _editCapabilities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_editCapabilities);
}

@override@JsonKey() final  bool editEnabled;
@override@JsonKey() final  String editWheelCircumferenceMm;
@override final  String? errorMessage;
@override@JsonKey() final  bool showAddFlow;

/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BleDevicesUiStateCopyWith<_BleDevicesUiState> get copyWith => __$BleDevicesUiStateCopyWithImpl<_BleDevicesUiState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BleDevicesUiState&&const DeepCollectionEquality().equals(other._devices, _devices)&&const DeepCollectionEquality().equals(other._discoveredDevices, _discoveredDevices)&&(identical(other.isScanning, isScanning) || other.isScanning == isScanning)&&(identical(other.showAllDevices, showAllDevices) || other.showAllDevices == showAllDevices)&&(identical(other.selectedDevice, selectedDevice) || other.selectedDevice == selectedDevice)&&const DeepCollectionEquality().equals(other._discoveredCapabilities, _discoveredCapabilities)&&(identical(other.isDiscoveringCapabilities, isDiscoveringCapabilities) || other.isDiscoveringCapabilities == isDiscoveringCapabilities)&&(identical(other.addDisplayName, addDisplayName) || other.addDisplayName == addDisplayName)&&const DeepCollectionEquality().equals(other._addCapabilities, _addCapabilities)&&(identical(other.addWheelCircumferenceMm, addWheelCircumferenceMm) || other.addWheelCircumferenceMm == addWheelCircumferenceMm)&&const DeepCollectionEquality().equals(other._capabilityConflicts, _capabilityConflicts)&&(identical(other.editingDeviceId, editingDeviceId) || other.editingDeviceId == editingDeviceId)&&(identical(other.editDisplayName, editDisplayName) || other.editDisplayName == editDisplayName)&&const DeepCollectionEquality().equals(other._editCapabilities, _editCapabilities)&&(identical(other.editEnabled, editEnabled) || other.editEnabled == editEnabled)&&(identical(other.editWheelCircumferenceMm, editWheelCircumferenceMm) || other.editWheelCircumferenceMm == editWheelCircumferenceMm)&&(identical(other.errorMessage, errorMessage) || other.errorMessage == errorMessage)&&(identical(other.showAddFlow, showAddFlow) || other.showAddFlow == showAddFlow));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_devices),const DeepCollectionEquality().hash(_discoveredDevices),isScanning,showAllDevices,selectedDevice,const DeepCollectionEquality().hash(_discoveredCapabilities),isDiscoveringCapabilities,addDisplayName,const DeepCollectionEquality().hash(_addCapabilities),addWheelCircumferenceMm,const DeepCollectionEquality().hash(_capabilityConflicts),editingDeviceId,editDisplayName,const DeepCollectionEquality().hash(_editCapabilities),editEnabled,editWheelCircumferenceMm,errorMessage,showAddFlow);

@override
String toString() {
  return 'BleDevicesUiState(devices: $devices, discoveredDevices: $discoveredDevices, isScanning: $isScanning, showAllDevices: $showAllDevices, selectedDevice: $selectedDevice, discoveredCapabilities: $discoveredCapabilities, isDiscoveringCapabilities: $isDiscoveringCapabilities, addDisplayName: $addDisplayName, addCapabilities: $addCapabilities, addWheelCircumferenceMm: $addWheelCircumferenceMm, capabilityConflicts: $capabilityConflicts, editingDeviceId: $editingDeviceId, editDisplayName: $editDisplayName, editCapabilities: $editCapabilities, editEnabled: $editEnabled, editWheelCircumferenceMm: $editWheelCircumferenceMm, errorMessage: $errorMessage, showAddFlow: $showAddFlow)';
}


}

/// @nodoc
abstract mixin class _$BleDevicesUiStateCopyWith<$Res> implements $BleDevicesUiStateCopyWith<$Res> {
  factory _$BleDevicesUiStateCopyWith(_BleDevicesUiState value, $Res Function(_BleDevicesUiState) _then) = __$BleDevicesUiStateCopyWithImpl;
@override @useResult
$Res call({
 List<BleSensorDevice> devices, List<BleDiscoveredDevice> discoveredDevices, bool isScanning, bool showAllDevices, BleDiscoveredDevice? selectedDevice, Set<BleSensorCapability> discoveredCapabilities, bool isDiscoveringCapabilities, String addDisplayName, Set<BleSensorCapability> addCapabilities, String addWheelCircumferenceMm, Map<BleSensorCapability, BleSensorDevice> capabilityConflicts, String? editingDeviceId, String editDisplayName, Set<BleSensorCapability> editCapabilities, bool editEnabled, String editWheelCircumferenceMm, String? errorMessage, bool showAddFlow
});


@override $BleDiscoveredDeviceCopyWith<$Res>? get selectedDevice;

}
/// @nodoc
class __$BleDevicesUiStateCopyWithImpl<$Res>
    implements _$BleDevicesUiStateCopyWith<$Res> {
  __$BleDevicesUiStateCopyWithImpl(this._self, this._then);

  final _BleDevicesUiState _self;
  final $Res Function(_BleDevicesUiState) _then;

/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? devices = null,Object? discoveredDevices = null,Object? isScanning = null,Object? showAllDevices = null,Object? selectedDevice = freezed,Object? discoveredCapabilities = null,Object? isDiscoveringCapabilities = null,Object? addDisplayName = null,Object? addCapabilities = null,Object? addWheelCircumferenceMm = null,Object? capabilityConflicts = null,Object? editingDeviceId = freezed,Object? editDisplayName = null,Object? editCapabilities = null,Object? editEnabled = null,Object? editWheelCircumferenceMm = null,Object? errorMessage = freezed,Object? showAddFlow = null,}) {
  return _then(_BleDevicesUiState(
devices: null == devices ? _self._devices : devices // ignore: cast_nullable_to_non_nullable
as List<BleSensorDevice>,discoveredDevices: null == discoveredDevices ? _self._discoveredDevices : discoveredDevices // ignore: cast_nullable_to_non_nullable
as List<BleDiscoveredDevice>,isScanning: null == isScanning ? _self.isScanning : isScanning // ignore: cast_nullable_to_non_nullable
as bool,showAllDevices: null == showAllDevices ? _self.showAllDevices : showAllDevices // ignore: cast_nullable_to_non_nullable
as bool,selectedDevice: freezed == selectedDevice ? _self.selectedDevice : selectedDevice // ignore: cast_nullable_to_non_nullable
as BleDiscoveredDevice?,discoveredCapabilities: null == discoveredCapabilities ? _self._discoveredCapabilities : discoveredCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,isDiscoveringCapabilities: null == isDiscoveringCapabilities ? _self.isDiscoveringCapabilities : isDiscoveringCapabilities // ignore: cast_nullable_to_non_nullable
as bool,addDisplayName: null == addDisplayName ? _self.addDisplayName : addDisplayName // ignore: cast_nullable_to_non_nullable
as String,addCapabilities: null == addCapabilities ? _self._addCapabilities : addCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,addWheelCircumferenceMm: null == addWheelCircumferenceMm ? _self.addWheelCircumferenceMm : addWheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as String,capabilityConflicts: null == capabilityConflicts ? _self._capabilityConflicts : capabilityConflicts // ignore: cast_nullable_to_non_nullable
as Map<BleSensorCapability, BleSensorDevice>,editingDeviceId: freezed == editingDeviceId ? _self.editingDeviceId : editingDeviceId // ignore: cast_nullable_to_non_nullable
as String?,editDisplayName: null == editDisplayName ? _self.editDisplayName : editDisplayName // ignore: cast_nullable_to_non_nullable
as String,editCapabilities: null == editCapabilities ? _self._editCapabilities : editCapabilities // ignore: cast_nullable_to_non_nullable
as Set<BleSensorCapability>,editEnabled: null == editEnabled ? _self.editEnabled : editEnabled // ignore: cast_nullable_to_non_nullable
as bool,editWheelCircumferenceMm: null == editWheelCircumferenceMm ? _self.editWheelCircumferenceMm : editWheelCircumferenceMm // ignore: cast_nullable_to_non_nullable
as String,errorMessage: freezed == errorMessage ? _self.errorMessage : errorMessage // ignore: cast_nullable_to_non_nullable
as String?,showAddFlow: null == showAddFlow ? _self.showAddFlow : showAddFlow // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

/// Create a copy of BleDevicesUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleDiscoveredDeviceCopyWith<$Res>? get selectedDevice {
    if (_self.selectedDevice == null) {
    return null;
  }

  return $BleDiscoveredDeviceCopyWith<$Res>(_self.selectedDevice!, (value) {
    return _then(_self.copyWith(selectedDevice: value));
  });
}
}

// dart format on
