// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyMeasurementWriteRequest {

 BodyMeasurementType get type; DateTime get time; double get value;
/// Create a copy of BodyMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyMeasurementWriteRequestCopyWith<BodyMeasurementWriteRequest> get copyWith => _$BodyMeasurementWriteRequestCopyWithImpl<BodyMeasurementWriteRequest>(this as BodyMeasurementWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyMeasurementWriteRequest&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,type,time,value);

@override
String toString() {
  return 'BodyMeasurementWriteRequest(type: $type, time: $time, value: $value)';
}


}

/// @nodoc
abstract mixin class $BodyMeasurementWriteRequestCopyWith<$Res>  {
  factory $BodyMeasurementWriteRequestCopyWith(BodyMeasurementWriteRequest value, $Res Function(BodyMeasurementWriteRequest) _then) = _$BodyMeasurementWriteRequestCopyWithImpl;
@useResult
$Res call({
 BodyMeasurementType type, DateTime time, double value
});




}
/// @nodoc
class _$BodyMeasurementWriteRequestCopyWithImpl<$Res>
    implements $BodyMeasurementWriteRequestCopyWith<$Res> {
  _$BodyMeasurementWriteRequestCopyWithImpl(this._self, this._then);

  final BodyMeasurementWriteRequest _self;
  final $Res Function(BodyMeasurementWriteRequest) _then;

/// Create a copy of BodyMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? time = null,Object? value = null,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyMeasurementWriteRequest].
extension BodyMeasurementWriteRequestPatterns on BodyMeasurementWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyMeasurementWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyMeasurementWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyMeasurementWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodyMeasurementType type,  DateTime time,  double value)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest() when $default != null:
return $default(_that.type,_that.time,_that.value);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodyMeasurementType type,  DateTime time,  double value)  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest():
return $default(_that.type,_that.time,_that.value);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodyMeasurementType type,  DateTime time,  double value)?  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementWriteRequest() when $default != null:
return $default(_that.type,_that.time,_that.value);case _:
  return null;

}
}

}

/// @nodoc


class _BodyMeasurementWriteRequest implements BodyMeasurementWriteRequest {
  const _BodyMeasurementWriteRequest({required this.type, required this.time, required this.value});
  

@override final  BodyMeasurementType type;
@override final  DateTime time;
@override final  double value;

/// Create a copy of BodyMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyMeasurementWriteRequestCopyWith<_BodyMeasurementWriteRequest> get copyWith => __$BodyMeasurementWriteRequestCopyWithImpl<_BodyMeasurementWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyMeasurementWriteRequest&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,type,time,value);

@override
String toString() {
  return 'BodyMeasurementWriteRequest(type: $type, time: $time, value: $value)';
}


}

/// @nodoc
abstract mixin class _$BodyMeasurementWriteRequestCopyWith<$Res> implements $BodyMeasurementWriteRequestCopyWith<$Res> {
  factory _$BodyMeasurementWriteRequestCopyWith(_BodyMeasurementWriteRequest value, $Res Function(_BodyMeasurementWriteRequest) _then) = __$BodyMeasurementWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 BodyMeasurementType type, DateTime time, double value
});




}
/// @nodoc
class __$BodyMeasurementWriteRequestCopyWithImpl<$Res>
    implements _$BodyMeasurementWriteRequestCopyWith<$Res> {
  __$BodyMeasurementWriteRequestCopyWithImpl(this._self, this._then);

  final _BodyMeasurementWriteRequest _self;
  final $Res Function(_BodyMeasurementWriteRequest) _then;

/// Create a copy of BodyMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? time = null,Object? value = null,}) {
  return _then(_BodyMeasurementWriteRequest(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$WeightEntry {

 DateTime get time; double get weightKg; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of WeightEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$WeightEntryCopyWith<WeightEntry> get copyWith => _$WeightEntryCopyWithImpl<WeightEntry>(this as WeightEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is WeightEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,weightKg,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'WeightEntry(time: $time, weightKg: $weightKg, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $WeightEntryCopyWith<$Res>  {
  factory $WeightEntryCopyWith(WeightEntry value, $Res Function(WeightEntry) _then) = _$WeightEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double weightKg, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$WeightEntryCopyWithImpl<$Res>
    implements $WeightEntryCopyWith<$Res> {
  _$WeightEntryCopyWithImpl(this._self, this._then);

  final WeightEntry _self;
  final $Res Function(WeightEntry) _then;

/// Create a copy of WeightEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? weightKg = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,weightKg: null == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [WeightEntry].
extension WeightEntryPatterns on WeightEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _WeightEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _WeightEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _WeightEntry value)  $default,){
final _that = this;
switch (_that) {
case _WeightEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _WeightEntry value)?  $default,){
final _that = this;
switch (_that) {
case _WeightEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double weightKg,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _WeightEntry() when $default != null:
return $default(_that.time,_that.weightKg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double weightKg,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _WeightEntry():
return $default(_that.time,_that.weightKg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double weightKg,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _WeightEntry() when $default != null:
return $default(_that.time,_that.weightKg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _WeightEntry implements WeightEntry {
  const _WeightEntry({required this.time, required this.weightKg, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double weightKg;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of WeightEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$WeightEntryCopyWith<_WeightEntry> get copyWith => __$WeightEntryCopyWithImpl<_WeightEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _WeightEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,weightKg,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'WeightEntry(time: $time, weightKg: $weightKg, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$WeightEntryCopyWith<$Res> implements $WeightEntryCopyWith<$Res> {
  factory _$WeightEntryCopyWith(_WeightEntry value, $Res Function(_WeightEntry) _then) = __$WeightEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double weightKg, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$WeightEntryCopyWithImpl<$Res>
    implements _$WeightEntryCopyWith<$Res> {
  __$WeightEntryCopyWithImpl(this._self, this._then);

  final _WeightEntry _self;
  final $Res Function(_WeightEntry) _then;

/// Create a copy of WeightEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? weightKg = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_WeightEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,weightKg: null == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$HeightEntry {

 DateTime get time; double get heightCm; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of HeightEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeightEntryCopyWith<HeightEntry> get copyWith => _$HeightEntryCopyWithImpl<HeightEntry>(this as HeightEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeightEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,heightCm,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'HeightEntry(time: $time, heightCm: $heightCm, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $HeightEntryCopyWith<$Res>  {
  factory $HeightEntryCopyWith(HeightEntry value, $Res Function(HeightEntry) _then) = _$HeightEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double heightCm, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$HeightEntryCopyWithImpl<$Res>
    implements $HeightEntryCopyWith<$Res> {
  _$HeightEntryCopyWithImpl(this._self, this._then);

  final HeightEntry _self;
  final $Res Function(HeightEntry) _then;

/// Create a copy of HeightEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? heightCm = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,heightCm: null == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [HeightEntry].
extension HeightEntryPatterns on HeightEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeightEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeightEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeightEntry value)  $default,){
final _that = this;
switch (_that) {
case _HeightEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeightEntry value)?  $default,){
final _that = this;
switch (_that) {
case _HeightEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double heightCm,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeightEntry() when $default != null:
return $default(_that.time,_that.heightCm,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double heightCm,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _HeightEntry():
return $default(_that.time,_that.heightCm,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double heightCm,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _HeightEntry() when $default != null:
return $default(_that.time,_that.heightCm,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _HeightEntry implements HeightEntry {
  const _HeightEntry({required this.time, required this.heightCm, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double heightCm;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of HeightEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeightEntryCopyWith<_HeightEntry> get copyWith => __$HeightEntryCopyWithImpl<_HeightEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeightEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,heightCm,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'HeightEntry(time: $time, heightCm: $heightCm, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$HeightEntryCopyWith<$Res> implements $HeightEntryCopyWith<$Res> {
  factory _$HeightEntryCopyWith(_HeightEntry value, $Res Function(_HeightEntry) _then) = __$HeightEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double heightCm, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$HeightEntryCopyWithImpl<$Res>
    implements _$HeightEntryCopyWith<$Res> {
  __$HeightEntryCopyWithImpl(this._self, this._then);

  final _HeightEntry _self;
  final $Res Function(_HeightEntry) _then;

/// Create a copy of HeightEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? heightCm = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_HeightEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,heightCm: null == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$BodyFatEntry {

 DateTime get time; double get percent; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of BodyFatEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyFatEntryCopyWith<BodyFatEntry> get copyWith => _$BodyFatEntryCopyWithImpl<BodyFatEntry>(this as BodyFatEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyFatEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.percent, percent) || other.percent == percent)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,percent,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyFatEntry(time: $time, percent: $percent, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $BodyFatEntryCopyWith<$Res>  {
  factory $BodyFatEntryCopyWith(BodyFatEntry value, $Res Function(BodyFatEntry) _then) = _$BodyFatEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double percent, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$BodyFatEntryCopyWithImpl<$Res>
    implements $BodyFatEntryCopyWith<$Res> {
  _$BodyFatEntryCopyWithImpl(this._self, this._then);

  final BodyFatEntry _self;
  final $Res Function(BodyFatEntry) _then;

/// Create a copy of BodyFatEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? percent = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,percent: null == percent ? _self.percent : percent // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyFatEntry].
extension BodyFatEntryPatterns on BodyFatEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyFatEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyFatEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyFatEntry value)  $default,){
final _that = this;
switch (_that) {
case _BodyFatEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyFatEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BodyFatEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double percent,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyFatEntry() when $default != null:
return $default(_that.time,_that.percent,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double percent,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _BodyFatEntry():
return $default(_that.time,_that.percent,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double percent,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _BodyFatEntry() when $default != null:
return $default(_that.time,_that.percent,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _BodyFatEntry implements BodyFatEntry {
  const _BodyFatEntry({required this.time, required this.percent, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double percent;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of BodyFatEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyFatEntryCopyWith<_BodyFatEntry> get copyWith => __$BodyFatEntryCopyWithImpl<_BodyFatEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyFatEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.percent, percent) || other.percent == percent)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,percent,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyFatEntry(time: $time, percent: $percent, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$BodyFatEntryCopyWith<$Res> implements $BodyFatEntryCopyWith<$Res> {
  factory _$BodyFatEntryCopyWith(_BodyFatEntry value, $Res Function(_BodyFatEntry) _then) = __$BodyFatEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double percent, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$BodyFatEntryCopyWithImpl<$Res>
    implements _$BodyFatEntryCopyWith<$Res> {
  __$BodyFatEntryCopyWithImpl(this._self, this._then);

  final _BodyFatEntry _self;
  final $Res Function(_BodyFatEntry) _then;

/// Create a copy of BodyFatEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? percent = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_BodyFatEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,percent: null == percent ? _self.percent : percent // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$LeanBodyMassEntry {

 DateTime get time; double get massKg; String get source;
/// Create a copy of LeanBodyMassEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$LeanBodyMassEntryCopyWith<LeanBodyMassEntry> get copyWith => _$LeanBodyMassEntryCopyWithImpl<LeanBodyMassEntry>(this as LeanBodyMassEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is LeanBodyMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'LeanBodyMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class $LeanBodyMassEntryCopyWith<$Res>  {
  factory $LeanBodyMassEntryCopyWith(LeanBodyMassEntry value, $Res Function(LeanBodyMassEntry) _then) = _$LeanBodyMassEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class _$LeanBodyMassEntryCopyWithImpl<$Res>
    implements $LeanBodyMassEntryCopyWith<$Res> {
  _$LeanBodyMassEntryCopyWithImpl(this._self, this._then);

  final LeanBodyMassEntry _self;
  final $Res Function(LeanBodyMassEntry) _then;

/// Create a copy of LeanBodyMassEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [LeanBodyMassEntry].
extension LeanBodyMassEntryPatterns on LeanBodyMassEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _LeanBodyMassEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _LeanBodyMassEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _LeanBodyMassEntry value)  $default,){
final _that = this;
switch (_that) {
case _LeanBodyMassEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _LeanBodyMassEntry value)?  $default,){
final _that = this;
switch (_that) {
case _LeanBodyMassEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _LeanBodyMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)  $default,) {final _that = this;
switch (_that) {
case _LeanBodyMassEntry():
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double massKg,  String source)?  $default,) {final _that = this;
switch (_that) {
case _LeanBodyMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _LeanBodyMassEntry implements LeanBodyMassEntry {
  const _LeanBodyMassEntry({required this.time, required this.massKg, required this.source});
  

@override final  DateTime time;
@override final  double massKg;
@override final  String source;

/// Create a copy of LeanBodyMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$LeanBodyMassEntryCopyWith<_LeanBodyMassEntry> get copyWith => __$LeanBodyMassEntryCopyWithImpl<_LeanBodyMassEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _LeanBodyMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'LeanBodyMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class _$LeanBodyMassEntryCopyWith<$Res> implements $LeanBodyMassEntryCopyWith<$Res> {
  factory _$LeanBodyMassEntryCopyWith(_LeanBodyMassEntry value, $Res Function(_LeanBodyMassEntry) _then) = __$LeanBodyMassEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class __$LeanBodyMassEntryCopyWithImpl<$Res>
    implements _$LeanBodyMassEntryCopyWith<$Res> {
  __$LeanBodyMassEntryCopyWithImpl(this._self, this._then);

  final _LeanBodyMassEntry _self;
  final $Res Function(_LeanBodyMassEntry) _then;

/// Create a copy of LeanBodyMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_LeanBodyMassEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$BmrEntry {

 DateTime get time; double get kcalPerDay; String get source;
/// Create a copy of BmrEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BmrEntryCopyWith<BmrEntry> get copyWith => _$BmrEntryCopyWithImpl<BmrEntry>(this as BmrEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BmrEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.kcalPerDay, kcalPerDay) || other.kcalPerDay == kcalPerDay)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,kcalPerDay,source);

@override
String toString() {
  return 'BmrEntry(time: $time, kcalPerDay: $kcalPerDay, source: $source)';
}


}

/// @nodoc
abstract mixin class $BmrEntryCopyWith<$Res>  {
  factory $BmrEntryCopyWith(BmrEntry value, $Res Function(BmrEntry) _then) = _$BmrEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double kcalPerDay, String source
});




}
/// @nodoc
class _$BmrEntryCopyWithImpl<$Res>
    implements $BmrEntryCopyWith<$Res> {
  _$BmrEntryCopyWithImpl(this._self, this._then);

  final BmrEntry _self;
  final $Res Function(BmrEntry) _then;

/// Create a copy of BmrEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? kcalPerDay = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,kcalPerDay: null == kcalPerDay ? _self.kcalPerDay : kcalPerDay // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [BmrEntry].
extension BmrEntryPatterns on BmrEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BmrEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BmrEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BmrEntry value)  $default,){
final _that = this;
switch (_that) {
case _BmrEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BmrEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BmrEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double kcalPerDay,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BmrEntry() when $default != null:
return $default(_that.time,_that.kcalPerDay,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double kcalPerDay,  String source)  $default,) {final _that = this;
switch (_that) {
case _BmrEntry():
return $default(_that.time,_that.kcalPerDay,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double kcalPerDay,  String source)?  $default,) {final _that = this;
switch (_that) {
case _BmrEntry() when $default != null:
return $default(_that.time,_that.kcalPerDay,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _BmrEntry implements BmrEntry {
  const _BmrEntry({required this.time, required this.kcalPerDay, required this.source});
  

@override final  DateTime time;
@override final  double kcalPerDay;
@override final  String source;

/// Create a copy of BmrEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BmrEntryCopyWith<_BmrEntry> get copyWith => __$BmrEntryCopyWithImpl<_BmrEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BmrEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.kcalPerDay, kcalPerDay) || other.kcalPerDay == kcalPerDay)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,kcalPerDay,source);

@override
String toString() {
  return 'BmrEntry(time: $time, kcalPerDay: $kcalPerDay, source: $source)';
}


}

/// @nodoc
abstract mixin class _$BmrEntryCopyWith<$Res> implements $BmrEntryCopyWith<$Res> {
  factory _$BmrEntryCopyWith(_BmrEntry value, $Res Function(_BmrEntry) _then) = __$BmrEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double kcalPerDay, String source
});




}
/// @nodoc
class __$BmrEntryCopyWithImpl<$Res>
    implements _$BmrEntryCopyWith<$Res> {
  __$BmrEntryCopyWithImpl(this._self, this._then);

  final _BmrEntry _self;
  final $Res Function(_BmrEntry) _then;

/// Create a copy of BmrEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? kcalPerDay = null,Object? source = null,}) {
  return _then(_BmrEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,kcalPerDay: null == kcalPerDay ? _self.kcalPerDay : kcalPerDay // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$BoneMassEntry {

 DateTime get time; double get massKg; String get source;
/// Create a copy of BoneMassEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BoneMassEntryCopyWith<BoneMassEntry> get copyWith => _$BoneMassEntryCopyWithImpl<BoneMassEntry>(this as BoneMassEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BoneMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'BoneMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class $BoneMassEntryCopyWith<$Res>  {
  factory $BoneMassEntryCopyWith(BoneMassEntry value, $Res Function(BoneMassEntry) _then) = _$BoneMassEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class _$BoneMassEntryCopyWithImpl<$Res>
    implements $BoneMassEntryCopyWith<$Res> {
  _$BoneMassEntryCopyWithImpl(this._self, this._then);

  final BoneMassEntry _self;
  final $Res Function(BoneMassEntry) _then;

/// Create a copy of BoneMassEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [BoneMassEntry].
extension BoneMassEntryPatterns on BoneMassEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BoneMassEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BoneMassEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BoneMassEntry value)  $default,){
final _that = this;
switch (_that) {
case _BoneMassEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BoneMassEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BoneMassEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BoneMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)  $default,) {final _that = this;
switch (_that) {
case _BoneMassEntry():
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double massKg,  String source)?  $default,) {final _that = this;
switch (_that) {
case _BoneMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _BoneMassEntry implements BoneMassEntry {
  const _BoneMassEntry({required this.time, required this.massKg, required this.source});
  

@override final  DateTime time;
@override final  double massKg;
@override final  String source;

/// Create a copy of BoneMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BoneMassEntryCopyWith<_BoneMassEntry> get copyWith => __$BoneMassEntryCopyWithImpl<_BoneMassEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BoneMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'BoneMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class _$BoneMassEntryCopyWith<$Res> implements $BoneMassEntryCopyWith<$Res> {
  factory _$BoneMassEntryCopyWith(_BoneMassEntry value, $Res Function(_BoneMassEntry) _then) = __$BoneMassEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class __$BoneMassEntryCopyWithImpl<$Res>
    implements _$BoneMassEntryCopyWith<$Res> {
  __$BoneMassEntryCopyWithImpl(this._self, this._then);

  final _BoneMassEntry _self;
  final $Res Function(_BoneMassEntry) _then;

/// Create a copy of BoneMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_BoneMassEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$BodyWaterMassEntry {

 DateTime get time; double get massKg; String get source;
/// Create a copy of BodyWaterMassEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyWaterMassEntryCopyWith<BodyWaterMassEntry> get copyWith => _$BodyWaterMassEntryCopyWithImpl<BodyWaterMassEntry>(this as BodyWaterMassEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyWaterMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'BodyWaterMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class $BodyWaterMassEntryCopyWith<$Res>  {
  factory $BodyWaterMassEntryCopyWith(BodyWaterMassEntry value, $Res Function(BodyWaterMassEntry) _then) = _$BodyWaterMassEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class _$BodyWaterMassEntryCopyWithImpl<$Res>
    implements $BodyWaterMassEntryCopyWith<$Res> {
  _$BodyWaterMassEntryCopyWithImpl(this._self, this._then);

  final BodyWaterMassEntry _self;
  final $Res Function(BodyWaterMassEntry) _then;

/// Create a copy of BodyWaterMassEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyWaterMassEntry].
extension BodyWaterMassEntryPatterns on BodyWaterMassEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyWaterMassEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyWaterMassEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyWaterMassEntry value)  $default,){
final _that = this;
switch (_that) {
case _BodyWaterMassEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyWaterMassEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BodyWaterMassEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyWaterMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double massKg,  String source)  $default,) {final _that = this;
switch (_that) {
case _BodyWaterMassEntry():
return $default(_that.time,_that.massKg,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double massKg,  String source)?  $default,) {final _that = this;
switch (_that) {
case _BodyWaterMassEntry() when $default != null:
return $default(_that.time,_that.massKg,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _BodyWaterMassEntry implements BodyWaterMassEntry {
  const _BodyWaterMassEntry({required this.time, required this.massKg, required this.source});
  

@override final  DateTime time;
@override final  double massKg;
@override final  String source;

/// Create a copy of BodyWaterMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyWaterMassEntryCopyWith<_BodyWaterMassEntry> get copyWith => __$BodyWaterMassEntryCopyWithImpl<_BodyWaterMassEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyWaterMassEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.massKg, massKg) || other.massKg == massKg)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,massKg,source);

@override
String toString() {
  return 'BodyWaterMassEntry(time: $time, massKg: $massKg, source: $source)';
}


}

/// @nodoc
abstract mixin class _$BodyWaterMassEntryCopyWith<$Res> implements $BodyWaterMassEntryCopyWith<$Res> {
  factory _$BodyWaterMassEntryCopyWith(_BodyWaterMassEntry value, $Res Function(_BodyWaterMassEntry) _then) = __$BodyWaterMassEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double massKg, String source
});




}
/// @nodoc
class __$BodyWaterMassEntryCopyWithImpl<$Res>
    implements _$BodyWaterMassEntryCopyWith<$Res> {
  __$BodyWaterMassEntryCopyWithImpl(this._self, this._then);

  final _BodyWaterMassEntry _self;
  final $Res Function(_BodyWaterMassEntry) _then;

/// Create a copy of BodyWaterMassEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? massKg = null,Object? source = null,}) {
  return _then(_BodyWaterMassEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,massKg: null == massKg ? _self.massKg : massKg // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$BodyMeasurementEntry {

 String get id; BodyMeasurementType get type; DateTime get time; double get value; String get source; bool get isOpenVitalsEntry;
/// Create a copy of BodyMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyMeasurementEntryCopyWith<BodyMeasurementEntry> get copyWith => _$BodyMeasurementEntryCopyWithImpl<BodyMeasurementEntry>(this as BodyMeasurementEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyMeasurementEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,type,time,value,source,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyMeasurementEntry(id: $id, type: $type, time: $time, value: $value, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $BodyMeasurementEntryCopyWith<$Res>  {
  factory $BodyMeasurementEntryCopyWith(BodyMeasurementEntry value, $Res Function(BodyMeasurementEntry) _then) = _$BodyMeasurementEntryCopyWithImpl;
@useResult
$Res call({
 String id, BodyMeasurementType type, DateTime time, double value, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$BodyMeasurementEntryCopyWithImpl<$Res>
    implements $BodyMeasurementEntryCopyWith<$Res> {
  _$BodyMeasurementEntryCopyWithImpl(this._self, this._then);

  final BodyMeasurementEntry _self;
  final $Res Function(BodyMeasurementEntry) _then;

/// Create a copy of BodyMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? type = null,Object? time = null,Object? value = null,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyMeasurementEntry].
extension BodyMeasurementEntryPatterns on BodyMeasurementEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyMeasurementEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyMeasurementEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyMeasurementEntry value)  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyMeasurementEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  BodyMeasurementType type,  DateTime time,  double value,  String source,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyMeasurementEntry() when $default != null:
return $default(_that.id,_that.type,_that.time,_that.value,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  BodyMeasurementType type,  DateTime time,  double value,  String source,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementEntry():
return $default(_that.id,_that.type,_that.time,_that.value,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  BodyMeasurementType type,  DateTime time,  double value,  String source,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementEntry() when $default != null:
return $default(_that.id,_that.type,_that.time,_that.value,_that.source,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _BodyMeasurementEntry implements BodyMeasurementEntry {
  const _BodyMeasurementEntry({required this.id, required this.type, required this.time, required this.value, required this.source, required this.isOpenVitalsEntry});
  

@override final  String id;
@override final  BodyMeasurementType type;
@override final  DateTime time;
@override final  double value;
@override final  String source;
@override final  bool isOpenVitalsEntry;

/// Create a copy of BodyMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyMeasurementEntryCopyWith<_BodyMeasurementEntry> get copyWith => __$BodyMeasurementEntryCopyWithImpl<_BodyMeasurementEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyMeasurementEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,type,time,value,source,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyMeasurementEntry(id: $id, type: $type, time: $time, value: $value, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$BodyMeasurementEntryCopyWith<$Res> implements $BodyMeasurementEntryCopyWith<$Res> {
  factory _$BodyMeasurementEntryCopyWith(_BodyMeasurementEntry value, $Res Function(_BodyMeasurementEntry) _then) = __$BodyMeasurementEntryCopyWithImpl;
@override @useResult
$Res call({
 String id, BodyMeasurementType type, DateTime time, double value, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$BodyMeasurementEntryCopyWithImpl<$Res>
    implements _$BodyMeasurementEntryCopyWith<$Res> {
  __$BodyMeasurementEntryCopyWithImpl(this._self, this._then);

  final _BodyMeasurementEntry _self;
  final $Res Function(_BodyMeasurementEntry) _then;

/// Create a copy of BodyMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? type = null,Object? time = null,Object? value = null,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_BodyMeasurementEntry(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
