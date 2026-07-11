// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepData {

 String get id; DateTime get startTime; DateTime get endTime; int get durationMs; String get source; String? get title; String? get notes; Duration? get startZoneOffset; Duration? get endZoneOffset; DateTime? get lastModifiedTime; String? get clientRecordId; int? get clientRecordVersion; int? get recordingMethod; SleepDeviceData? get device; List<SleepStage> get stages;
/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepDataCopyWith<SleepData> get copyWith => _$SleepDataCopyWithImpl<SleepData>(this as SleepData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepData&&(identical(other.id, id) || other.id == id)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.startZoneOffset, startZoneOffset) || other.startZoneOffset == startZoneOffset)&&(identical(other.endZoneOffset, endZoneOffset) || other.endZoneOffset == endZoneOffset)&&(identical(other.lastModifiedTime, lastModifiedTime) || other.lastModifiedTime == lastModifiedTime)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.clientRecordVersion, clientRecordVersion) || other.clientRecordVersion == clientRecordVersion)&&(identical(other.recordingMethod, recordingMethod) || other.recordingMethod == recordingMethod)&&(identical(other.device, device) || other.device == device)&&const DeepCollectionEquality().equals(other.stages, stages));
}


@override
int get hashCode => Object.hash(runtimeType,id,startTime,endTime,durationMs,source,title,notes,startZoneOffset,endZoneOffset,lastModifiedTime,clientRecordId,clientRecordVersion,recordingMethod,device,const DeepCollectionEquality().hash(stages));

@override
String toString() {
  return 'SleepData(id: $id, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, title: $title, notes: $notes, startZoneOffset: $startZoneOffset, endZoneOffset: $endZoneOffset, lastModifiedTime: $lastModifiedTime, clientRecordId: $clientRecordId, clientRecordVersion: $clientRecordVersion, recordingMethod: $recordingMethod, device: $device, stages: $stages)';
}


}

/// @nodoc
abstract mixin class $SleepDataCopyWith<$Res>  {
  factory $SleepDataCopyWith(SleepData value, $Res Function(SleepData) _then) = _$SleepDataCopyWithImpl;
@useResult
$Res call({
 String id, DateTime startTime, DateTime endTime, int durationMs, String source, String? title, String? notes, Duration? startZoneOffset, Duration? endZoneOffset, DateTime? lastModifiedTime, String? clientRecordId, int? clientRecordVersion, int? recordingMethod, SleepDeviceData? device, List<SleepStage> stages
});


$SleepDeviceDataCopyWith<$Res>? get device;

}
/// @nodoc
class _$SleepDataCopyWithImpl<$Res>
    implements $SleepDataCopyWith<$Res> {
  _$SleepDataCopyWithImpl(this._self, this._then);

  final SleepData _self;
  final $Res Function(SleepData) _then;

/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? title = freezed,Object? notes = freezed,Object? startZoneOffset = freezed,Object? endZoneOffset = freezed,Object? lastModifiedTime = freezed,Object? clientRecordId = freezed,Object? clientRecordVersion = freezed,Object? recordingMethod = freezed,Object? device = freezed,Object? stages = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,startZoneOffset: freezed == startZoneOffset ? _self.startZoneOffset : startZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,endZoneOffset: freezed == endZoneOffset ? _self.endZoneOffset : endZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,lastModifiedTime: freezed == lastModifiedTime ? _self.lastModifiedTime : lastModifiedTime // ignore: cast_nullable_to_non_nullable
as DateTime?,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,clientRecordVersion: freezed == clientRecordVersion ? _self.clientRecordVersion : clientRecordVersion // ignore: cast_nullable_to_non_nullable
as int?,recordingMethod: freezed == recordingMethod ? _self.recordingMethod : recordingMethod // ignore: cast_nullable_to_non_nullable
as int?,device: freezed == device ? _self.device : device // ignore: cast_nullable_to_non_nullable
as SleepDeviceData?,stages: null == stages ? _self.stages : stages // ignore: cast_nullable_to_non_nullable
as List<SleepStage>,
  ));
}
/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDeviceDataCopyWith<$Res>? get device {
    if (_self.device == null) {
    return null;
  }

  return $SleepDeviceDataCopyWith<$Res>(_self.device!, (value) {
    return _then(_self.copyWith(device: value));
  });
}
}


/// Adds pattern-matching-related methods to [SleepData].
extension SleepDataPatterns on SleepData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepData value)  $default,){
final _that = this;
switch (_that) {
case _SleepData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepData value)?  $default,){
final _that = this;
switch (_that) {
case _SleepData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  String? title,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  SleepDeviceData? device,  List<SleepStage> stages)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepData() when $default != null:
return $default(_that.id,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.title,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.stages);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  String? title,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  SleepDeviceData? device,  List<SleepStage> stages)  $default,) {final _that = this;
switch (_that) {
case _SleepData():
return $default(_that.id,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.title,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.stages);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  String? title,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  SleepDeviceData? device,  List<SleepStage> stages)?  $default,) {final _that = this;
switch (_that) {
case _SleepData() when $default != null:
return $default(_that.id,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.title,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.stages);case _:
  return null;

}
}

}

/// @nodoc


class _SleepData extends SleepData {
  const _SleepData({required this.id, required this.startTime, required this.endTime, required this.durationMs, required this.source, this.title, this.notes, this.startZoneOffset, this.endZoneOffset, this.lastModifiedTime, this.clientRecordId, this.clientRecordVersion, this.recordingMethod, this.device, final  List<SleepStage> stages = const <SleepStage>[]}): _stages = stages,super._();
  

@override final  String id;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int durationMs;
@override final  String source;
@override final  String? title;
@override final  String? notes;
@override final  Duration? startZoneOffset;
@override final  Duration? endZoneOffset;
@override final  DateTime? lastModifiedTime;
@override final  String? clientRecordId;
@override final  int? clientRecordVersion;
@override final  int? recordingMethod;
@override final  SleepDeviceData? device;
 final  List<SleepStage> _stages;
@override@JsonKey() List<SleepStage> get stages {
  if (_stages is EqualUnmodifiableListView) return _stages;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stages);
}


/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepDataCopyWith<_SleepData> get copyWith => __$SleepDataCopyWithImpl<_SleepData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepData&&(identical(other.id, id) || other.id == id)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.startZoneOffset, startZoneOffset) || other.startZoneOffset == startZoneOffset)&&(identical(other.endZoneOffset, endZoneOffset) || other.endZoneOffset == endZoneOffset)&&(identical(other.lastModifiedTime, lastModifiedTime) || other.lastModifiedTime == lastModifiedTime)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.clientRecordVersion, clientRecordVersion) || other.clientRecordVersion == clientRecordVersion)&&(identical(other.recordingMethod, recordingMethod) || other.recordingMethod == recordingMethod)&&(identical(other.device, device) || other.device == device)&&const DeepCollectionEquality().equals(other._stages, _stages));
}


@override
int get hashCode => Object.hash(runtimeType,id,startTime,endTime,durationMs,source,title,notes,startZoneOffset,endZoneOffset,lastModifiedTime,clientRecordId,clientRecordVersion,recordingMethod,device,const DeepCollectionEquality().hash(_stages));

@override
String toString() {
  return 'SleepData(id: $id, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, title: $title, notes: $notes, startZoneOffset: $startZoneOffset, endZoneOffset: $endZoneOffset, lastModifiedTime: $lastModifiedTime, clientRecordId: $clientRecordId, clientRecordVersion: $clientRecordVersion, recordingMethod: $recordingMethod, device: $device, stages: $stages)';
}


}

/// @nodoc
abstract mixin class _$SleepDataCopyWith<$Res> implements $SleepDataCopyWith<$Res> {
  factory _$SleepDataCopyWith(_SleepData value, $Res Function(_SleepData) _then) = __$SleepDataCopyWithImpl;
@override @useResult
$Res call({
 String id, DateTime startTime, DateTime endTime, int durationMs, String source, String? title, String? notes, Duration? startZoneOffset, Duration? endZoneOffset, DateTime? lastModifiedTime, String? clientRecordId, int? clientRecordVersion, int? recordingMethod, SleepDeviceData? device, List<SleepStage> stages
});


@override $SleepDeviceDataCopyWith<$Res>? get device;

}
/// @nodoc
class __$SleepDataCopyWithImpl<$Res>
    implements _$SleepDataCopyWith<$Res> {
  __$SleepDataCopyWithImpl(this._self, this._then);

  final _SleepData _self;
  final $Res Function(_SleepData) _then;

/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? title = freezed,Object? notes = freezed,Object? startZoneOffset = freezed,Object? endZoneOffset = freezed,Object? lastModifiedTime = freezed,Object? clientRecordId = freezed,Object? clientRecordVersion = freezed,Object? recordingMethod = freezed,Object? device = freezed,Object? stages = null,}) {
  return _then(_SleepData(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,startZoneOffset: freezed == startZoneOffset ? _self.startZoneOffset : startZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,endZoneOffset: freezed == endZoneOffset ? _self.endZoneOffset : endZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,lastModifiedTime: freezed == lastModifiedTime ? _self.lastModifiedTime : lastModifiedTime // ignore: cast_nullable_to_non_nullable
as DateTime?,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,clientRecordVersion: freezed == clientRecordVersion ? _self.clientRecordVersion : clientRecordVersion // ignore: cast_nullable_to_non_nullable
as int?,recordingMethod: freezed == recordingMethod ? _self.recordingMethod : recordingMethod // ignore: cast_nullable_to_non_nullable
as int?,device: freezed == device ? _self.device : device // ignore: cast_nullable_to_non_nullable
as SleepDeviceData?,stages: null == stages ? _self._stages : stages // ignore: cast_nullable_to_non_nullable
as List<SleepStage>,
  ));
}

/// Create a copy of SleepData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDeviceDataCopyWith<$Res>? get device {
    if (_self.device == null) {
    return null;
  }

  return $SleepDeviceDataCopyWith<$Res>(_self.device!, (value) {
    return _then(_self.copyWith(device: value));
  });
}
}

/// @nodoc
mixin _$SleepDeviceData {

 int get type; String? get manufacturer; String? get model;
/// Create a copy of SleepDeviceData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepDeviceDataCopyWith<SleepDeviceData> get copyWith => _$SleepDeviceDataCopyWithImpl<SleepDeviceData>(this as SleepDeviceData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepDeviceData&&(identical(other.type, type) || other.type == type)&&(identical(other.manufacturer, manufacturer) || other.manufacturer == manufacturer)&&(identical(other.model, model) || other.model == model));
}


@override
int get hashCode => Object.hash(runtimeType,type,manufacturer,model);

@override
String toString() {
  return 'SleepDeviceData(type: $type, manufacturer: $manufacturer, model: $model)';
}


}

/// @nodoc
abstract mixin class $SleepDeviceDataCopyWith<$Res>  {
  factory $SleepDeviceDataCopyWith(SleepDeviceData value, $Res Function(SleepDeviceData) _then) = _$SleepDeviceDataCopyWithImpl;
@useResult
$Res call({
 int type, String? manufacturer, String? model
});




}
/// @nodoc
class _$SleepDeviceDataCopyWithImpl<$Res>
    implements $SleepDeviceDataCopyWith<$Res> {
  _$SleepDeviceDataCopyWithImpl(this._self, this._then);

  final SleepDeviceData _self;
  final $Res Function(SleepDeviceData) _then;

/// Create a copy of SleepDeviceData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? manufacturer = freezed,Object? model = freezed,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as int,manufacturer: freezed == manufacturer ? _self.manufacturer : manufacturer // ignore: cast_nullable_to_non_nullable
as String?,model: freezed == model ? _self.model : model // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepDeviceData].
extension SleepDeviceDataPatterns on SleepDeviceData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepDeviceData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepDeviceData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepDeviceData value)  $default,){
final _that = this;
switch (_that) {
case _SleepDeviceData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepDeviceData value)?  $default,){
final _that = this;
switch (_that) {
case _SleepDeviceData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int type,  String? manufacturer,  String? model)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepDeviceData() when $default != null:
return $default(_that.type,_that.manufacturer,_that.model);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int type,  String? manufacturer,  String? model)  $default,) {final _that = this;
switch (_that) {
case _SleepDeviceData():
return $default(_that.type,_that.manufacturer,_that.model);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int type,  String? manufacturer,  String? model)?  $default,) {final _that = this;
switch (_that) {
case _SleepDeviceData() when $default != null:
return $default(_that.type,_that.manufacturer,_that.model);case _:
  return null;

}
}

}

/// @nodoc


class _SleepDeviceData implements SleepDeviceData {
  const _SleepDeviceData({required this.type, required this.manufacturer, required this.model});
  

@override final  int type;
@override final  String? manufacturer;
@override final  String? model;

/// Create a copy of SleepDeviceData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepDeviceDataCopyWith<_SleepDeviceData> get copyWith => __$SleepDeviceDataCopyWithImpl<_SleepDeviceData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepDeviceData&&(identical(other.type, type) || other.type == type)&&(identical(other.manufacturer, manufacturer) || other.manufacturer == manufacturer)&&(identical(other.model, model) || other.model == model));
}


@override
int get hashCode => Object.hash(runtimeType,type,manufacturer,model);

@override
String toString() {
  return 'SleepDeviceData(type: $type, manufacturer: $manufacturer, model: $model)';
}


}

/// @nodoc
abstract mixin class _$SleepDeviceDataCopyWith<$Res> implements $SleepDeviceDataCopyWith<$Res> {
  factory _$SleepDeviceDataCopyWith(_SleepDeviceData value, $Res Function(_SleepDeviceData) _then) = __$SleepDeviceDataCopyWithImpl;
@override @useResult
$Res call({
 int type, String? manufacturer, String? model
});




}
/// @nodoc
class __$SleepDeviceDataCopyWithImpl<$Res>
    implements _$SleepDeviceDataCopyWith<$Res> {
  __$SleepDeviceDataCopyWithImpl(this._self, this._then);

  final _SleepDeviceData _self;
  final $Res Function(_SleepDeviceData) _then;

/// Create a copy of SleepDeviceData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? manufacturer = freezed,Object? model = freezed,}) {
  return _then(_SleepDeviceData(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as int,manufacturer: freezed == manufacturer ? _self.manufacturer : manufacturer // ignore: cast_nullable_to_non_nullable
as String?,model: freezed == model ? _self.model : model // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

/// @nodoc
mixin _$SleepStage {

 DateTime get startTime; DateTime get endTime; int get stageType;
/// Create a copy of SleepStage
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepStageCopyWith<SleepStage> get copyWith => _$SleepStageCopyWithImpl<SleepStage>(this as SleepStage, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepStage&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.stageType, stageType) || other.stageType == stageType));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,stageType);

@override
String toString() {
  return 'SleepStage(startTime: $startTime, endTime: $endTime, stageType: $stageType)';
}


}

/// @nodoc
abstract mixin class $SleepStageCopyWith<$Res>  {
  factory $SleepStageCopyWith(SleepStage value, $Res Function(SleepStage) _then) = _$SleepStageCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, int stageType
});




}
/// @nodoc
class _$SleepStageCopyWithImpl<$Res>
    implements $SleepStageCopyWith<$Res> {
  _$SleepStageCopyWithImpl(this._self, this._then);

  final SleepStage _self;
  final $Res Function(SleepStage) _then;

/// Create a copy of SleepStage
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? stageType = null,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,stageType: null == stageType ? _self.stageType : stageType // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepStage].
extension SleepStagePatterns on SleepStage {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepStage value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepStage() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepStage value)  $default,){
final _that = this;
switch (_that) {
case _SleepStage():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepStage value)?  $default,){
final _that = this;
switch (_that) {
case _SleepStage() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int stageType)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepStage() when $default != null:
return $default(_that.startTime,_that.endTime,_that.stageType);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int stageType)  $default,) {final _that = this;
switch (_that) {
case _SleepStage():
return $default(_that.startTime,_that.endTime,_that.stageType);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  int stageType)?  $default,) {final _that = this;
switch (_that) {
case _SleepStage() when $default != null:
return $default(_that.startTime,_that.endTime,_that.stageType);case _:
  return null;

}
}

}

/// @nodoc


class _SleepStage extends SleepStage {
  const _SleepStage({required this.startTime, required this.endTime, required this.stageType}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int stageType;

/// Create a copy of SleepStage
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepStageCopyWith<_SleepStage> get copyWith => __$SleepStageCopyWithImpl<_SleepStage>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepStage&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.stageType, stageType) || other.stageType == stageType));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,stageType);

@override
String toString() {
  return 'SleepStage(startTime: $startTime, endTime: $endTime, stageType: $stageType)';
}


}

/// @nodoc
abstract mixin class _$SleepStageCopyWith<$Res> implements $SleepStageCopyWith<$Res> {
  factory _$SleepStageCopyWith(_SleepStage value, $Res Function(_SleepStage) _then) = __$SleepStageCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, int stageType
});




}
/// @nodoc
class __$SleepStageCopyWithImpl<$Res>
    implements _$SleepStageCopyWith<$Res> {
  __$SleepStageCopyWithImpl(this._self, this._then);

  final _SleepStage _self;
  final $Res Function(_SleepStage) _then;

/// Create a copy of SleepStage
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? stageType = null,}) {
  return _then(_SleepStage(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,stageType: null == stageType ? _self.stageType : stageType // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$DailySleepDuration {

 LocalDate get date; int get durationMs;
/// Create a copy of DailySleepDuration
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailySleepDurationCopyWith<DailySleepDuration> get copyWith => _$DailySleepDurationCopyWithImpl<DailySleepDuration>(this as DailySleepDuration, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailySleepDuration&&(identical(other.date, date) || other.date == date)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs));
}


@override
int get hashCode => Object.hash(runtimeType,date,durationMs);

@override
String toString() {
  return 'DailySleepDuration(date: $date, durationMs: $durationMs)';
}


}

/// @nodoc
abstract mixin class $DailySleepDurationCopyWith<$Res>  {
  factory $DailySleepDurationCopyWith(DailySleepDuration value, $Res Function(DailySleepDuration) _then) = _$DailySleepDurationCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int durationMs
});




}
/// @nodoc
class _$DailySleepDurationCopyWithImpl<$Res>
    implements $DailySleepDurationCopyWith<$Res> {
  _$DailySleepDurationCopyWithImpl(this._self, this._then);

  final DailySleepDuration _self;
  final $Res Function(DailySleepDuration) _then;

/// Create a copy of DailySleepDuration
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? durationMs = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [DailySleepDuration].
extension DailySleepDurationPatterns on DailySleepDuration {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailySleepDuration value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailySleepDuration() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailySleepDuration value)  $default,){
final _that = this;
switch (_that) {
case _DailySleepDuration():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailySleepDuration value)?  $default,){
final _that = this;
switch (_that) {
case _DailySleepDuration() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int durationMs)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailySleepDuration() when $default != null:
return $default(_that.date,_that.durationMs);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int durationMs)  $default,) {final _that = this;
switch (_that) {
case _DailySleepDuration():
return $default(_that.date,_that.durationMs);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int durationMs)?  $default,) {final _that = this;
switch (_that) {
case _DailySleepDuration() when $default != null:
return $default(_that.date,_that.durationMs);case _:
  return null;

}
}

}

/// @nodoc


class _DailySleepDuration extends DailySleepDuration {
  const _DailySleepDuration({required this.date, required this.durationMs}): super._();
  

@override final  LocalDate date;
@override final  int durationMs;

/// Create a copy of DailySleepDuration
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailySleepDurationCopyWith<_DailySleepDuration> get copyWith => __$DailySleepDurationCopyWithImpl<_DailySleepDuration>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailySleepDuration&&(identical(other.date, date) || other.date == date)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs));
}


@override
int get hashCode => Object.hash(runtimeType,date,durationMs);

@override
String toString() {
  return 'DailySleepDuration(date: $date, durationMs: $durationMs)';
}


}

/// @nodoc
abstract mixin class _$DailySleepDurationCopyWith<$Res> implements $DailySleepDurationCopyWith<$Res> {
  factory _$DailySleepDurationCopyWith(_DailySleepDuration value, $Res Function(_DailySleepDuration) _then) = __$DailySleepDurationCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int durationMs
});




}
/// @nodoc
class __$DailySleepDurationCopyWithImpl<$Res>
    implements _$DailySleepDurationCopyWith<$Res> {
  __$DailySleepDurationCopyWithImpl(this._self, this._then);

  final _DailySleepDuration _self;
  final $Res Function(_DailySleepDuration) _then;

/// Create a copy of DailySleepDuration
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? durationMs = null,}) {
  return _then(_DailySleepDuration(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$SleepReadData {

 List<SleepData> get sessions; List<DailySleepDuration> get dailyAggregateDurations;
/// Create a copy of SleepReadData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepReadDataCopyWith<SleepReadData> get copyWith => _$SleepReadDataCopyWithImpl<SleepReadData>(this as SleepReadData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepReadData&&const DeepCollectionEquality().equals(other.sessions, sessions)&&const DeepCollectionEquality().equals(other.dailyAggregateDurations, dailyAggregateDurations));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(sessions),const DeepCollectionEquality().hash(dailyAggregateDurations));

@override
String toString() {
  return 'SleepReadData(sessions: $sessions, dailyAggregateDurations: $dailyAggregateDurations)';
}


}

/// @nodoc
abstract mixin class $SleepReadDataCopyWith<$Res>  {
  factory $SleepReadDataCopyWith(SleepReadData value, $Res Function(SleepReadData) _then) = _$SleepReadDataCopyWithImpl;
@useResult
$Res call({
 List<SleepData> sessions, List<DailySleepDuration> dailyAggregateDurations
});




}
/// @nodoc
class _$SleepReadDataCopyWithImpl<$Res>
    implements $SleepReadDataCopyWith<$Res> {
  _$SleepReadDataCopyWithImpl(this._self, this._then);

  final SleepReadData _self;
  final $Res Function(SleepReadData) _then;

/// Create a copy of SleepReadData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? sessions = null,Object? dailyAggregateDurations = null,}) {
  return _then(_self.copyWith(
sessions: null == sessions ? _self.sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailyAggregateDurations: null == dailyAggregateDurations ? _self.dailyAggregateDurations : dailyAggregateDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepReadData].
extension SleepReadDataPatterns on SleepReadData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepReadData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepReadData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepReadData value)  $default,){
final _that = this;
switch (_that) {
case _SleepReadData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepReadData value)?  $default,){
final _that = this;
switch (_that) {
case _SleepReadData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<SleepData> sessions,  List<DailySleepDuration> dailyAggregateDurations)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepReadData() when $default != null:
return $default(_that.sessions,_that.dailyAggregateDurations);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<SleepData> sessions,  List<DailySleepDuration> dailyAggregateDurations)  $default,) {final _that = this;
switch (_that) {
case _SleepReadData():
return $default(_that.sessions,_that.dailyAggregateDurations);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<SleepData> sessions,  List<DailySleepDuration> dailyAggregateDurations)?  $default,) {final _that = this;
switch (_that) {
case _SleepReadData() when $default != null:
return $default(_that.sessions,_that.dailyAggregateDurations);case _:
  return null;

}
}

}

/// @nodoc


class _SleepReadData implements SleepReadData {
  const _SleepReadData({final  List<SleepData> sessions = const <SleepData>[], final  List<DailySleepDuration> dailyAggregateDurations = const <DailySleepDuration>[]}): _sessions = sessions,_dailyAggregateDurations = dailyAggregateDurations;
  

 final  List<SleepData> _sessions;
@override@JsonKey() List<SleepData> get sessions {
  if (_sessions is EqualUnmodifiableListView) return _sessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sessions);
}

 final  List<DailySleepDuration> _dailyAggregateDurations;
@override@JsonKey() List<DailySleepDuration> get dailyAggregateDurations {
  if (_dailyAggregateDurations is EqualUnmodifiableListView) return _dailyAggregateDurations;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyAggregateDurations);
}


/// Create a copy of SleepReadData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepReadDataCopyWith<_SleepReadData> get copyWith => __$SleepReadDataCopyWithImpl<_SleepReadData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepReadData&&const DeepCollectionEquality().equals(other._sessions, _sessions)&&const DeepCollectionEquality().equals(other._dailyAggregateDurations, _dailyAggregateDurations));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_sessions),const DeepCollectionEquality().hash(_dailyAggregateDurations));

@override
String toString() {
  return 'SleepReadData(sessions: $sessions, dailyAggregateDurations: $dailyAggregateDurations)';
}


}

/// @nodoc
abstract mixin class _$SleepReadDataCopyWith<$Res> implements $SleepReadDataCopyWith<$Res> {
  factory _$SleepReadDataCopyWith(_SleepReadData value, $Res Function(_SleepReadData) _then) = __$SleepReadDataCopyWithImpl;
@override @useResult
$Res call({
 List<SleepData> sessions, List<DailySleepDuration> dailyAggregateDurations
});




}
/// @nodoc
class __$SleepReadDataCopyWithImpl<$Res>
    implements _$SleepReadDataCopyWith<$Res> {
  __$SleepReadDataCopyWithImpl(this._self, this._then);

  final _SleepReadData _self;
  final $Res Function(_SleepReadData) _then;

/// Create a copy of SleepReadData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? sessions = null,Object? dailyAggregateDurations = null,}) {
  return _then(_SleepReadData(
sessions: null == sessions ? _self._sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailyAggregateDurations: null == dailyAggregateDurations ? _self._dailyAggregateDurations : dailyAggregateDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,
  ));
}


}

// dart format on
