// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'vitals_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BloodPressureEntry {

 DateTime get time; int get systolicMmHg; int get diastolicMmHg; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of BloodPressureEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BloodPressureEntryCopyWith<BloodPressureEntry> get copyWith => _$BloodPressureEntryCopyWithImpl<BloodPressureEntry>(this as BloodPressureEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BloodPressureEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.systolicMmHg, systolicMmHg) || other.systolicMmHg == systolicMmHg)&&(identical(other.diastolicMmHg, diastolicMmHg) || other.diastolicMmHg == diastolicMmHg)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,systolicMmHg,diastolicMmHg,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BloodPressureEntry(time: $time, systolicMmHg: $systolicMmHg, diastolicMmHg: $diastolicMmHg, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $BloodPressureEntryCopyWith<$Res>  {
  factory $BloodPressureEntryCopyWith(BloodPressureEntry value, $Res Function(BloodPressureEntry) _then) = _$BloodPressureEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, int systolicMmHg, int diastolicMmHg, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$BloodPressureEntryCopyWithImpl<$Res>
    implements $BloodPressureEntryCopyWith<$Res> {
  _$BloodPressureEntryCopyWithImpl(this._self, this._then);

  final BloodPressureEntry _self;
  final $Res Function(BloodPressureEntry) _then;

/// Create a copy of BloodPressureEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? systolicMmHg = null,Object? diastolicMmHg = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,systolicMmHg: null == systolicMmHg ? _self.systolicMmHg : systolicMmHg // ignore: cast_nullable_to_non_nullable
as int,diastolicMmHg: null == diastolicMmHg ? _self.diastolicMmHg : diastolicMmHg // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [BloodPressureEntry].
extension BloodPressureEntryPatterns on BloodPressureEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BloodPressureEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BloodPressureEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BloodPressureEntry value)  $default,){
final _that = this;
switch (_that) {
case _BloodPressureEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BloodPressureEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BloodPressureEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int systolicMmHg,  int diastolicMmHg,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BloodPressureEntry() when $default != null:
return $default(_that.time,_that.systolicMmHg,_that.diastolicMmHg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int systolicMmHg,  int diastolicMmHg,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _BloodPressureEntry():
return $default(_that.time,_that.systolicMmHg,_that.diastolicMmHg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int systolicMmHg,  int diastolicMmHg,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _BloodPressureEntry() when $default != null:
return $default(_that.time,_that.systolicMmHg,_that.diastolicMmHg,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _BloodPressureEntry implements BloodPressureEntry {
  const _BloodPressureEntry({required this.time, required this.systolicMmHg, required this.diastolicMmHg, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  int systolicMmHg;
@override final  int diastolicMmHg;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of BloodPressureEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BloodPressureEntryCopyWith<_BloodPressureEntry> get copyWith => __$BloodPressureEntryCopyWithImpl<_BloodPressureEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BloodPressureEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.systolicMmHg, systolicMmHg) || other.systolicMmHg == systolicMmHg)&&(identical(other.diastolicMmHg, diastolicMmHg) || other.diastolicMmHg == diastolicMmHg)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,systolicMmHg,diastolicMmHg,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BloodPressureEntry(time: $time, systolicMmHg: $systolicMmHg, diastolicMmHg: $diastolicMmHg, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$BloodPressureEntryCopyWith<$Res> implements $BloodPressureEntryCopyWith<$Res> {
  factory _$BloodPressureEntryCopyWith(_BloodPressureEntry value, $Res Function(_BloodPressureEntry) _then) = __$BloodPressureEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int systolicMmHg, int diastolicMmHg, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$BloodPressureEntryCopyWithImpl<$Res>
    implements _$BloodPressureEntryCopyWith<$Res> {
  __$BloodPressureEntryCopyWithImpl(this._self, this._then);

  final _BloodPressureEntry _self;
  final $Res Function(_BloodPressureEntry) _then;

/// Create a copy of BloodPressureEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? systolicMmHg = null,Object? diastolicMmHg = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_BloodPressureEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,systolicMmHg: null == systolicMmHg ? _self.systolicMmHg : systolicMmHg // ignore: cast_nullable_to_non_nullable
as int,diastolicMmHg: null == diastolicMmHg ? _self.diastolicMmHg : diastolicMmHg // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$SpO2Entry {

 DateTime get time; double get percent; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of SpO2Entry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SpO2EntryCopyWith<SpO2Entry> get copyWith => _$SpO2EntryCopyWithImpl<SpO2Entry>(this as SpO2Entry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SpO2Entry&&(identical(other.time, time) || other.time == time)&&(identical(other.percent, percent) || other.percent == percent)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,percent,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'SpO2Entry(time: $time, percent: $percent, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $SpO2EntryCopyWith<$Res>  {
  factory $SpO2EntryCopyWith(SpO2Entry value, $Res Function(SpO2Entry) _then) = _$SpO2EntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double percent, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$SpO2EntryCopyWithImpl<$Res>
    implements $SpO2EntryCopyWith<$Res> {
  _$SpO2EntryCopyWithImpl(this._self, this._then);

  final SpO2Entry _self;
  final $Res Function(SpO2Entry) _then;

/// Create a copy of SpO2Entry
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


/// Adds pattern-matching-related methods to [SpO2Entry].
extension SpO2EntryPatterns on SpO2Entry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SpO2Entry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SpO2Entry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SpO2Entry value)  $default,){
final _that = this;
switch (_that) {
case _SpO2Entry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SpO2Entry value)?  $default,){
final _that = this;
switch (_that) {
case _SpO2Entry() when $default != null:
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
case _SpO2Entry() when $default != null:
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
case _SpO2Entry():
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
case _SpO2Entry() when $default != null:
return $default(_that.time,_that.percent,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _SpO2Entry implements SpO2Entry {
  const _SpO2Entry({required this.time, required this.percent, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double percent;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of SpO2Entry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SpO2EntryCopyWith<_SpO2Entry> get copyWith => __$SpO2EntryCopyWithImpl<_SpO2Entry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SpO2Entry&&(identical(other.time, time) || other.time == time)&&(identical(other.percent, percent) || other.percent == percent)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,percent,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'SpO2Entry(time: $time, percent: $percent, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$SpO2EntryCopyWith<$Res> implements $SpO2EntryCopyWith<$Res> {
  factory _$SpO2EntryCopyWith(_SpO2Entry value, $Res Function(_SpO2Entry) _then) = __$SpO2EntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double percent, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$SpO2EntryCopyWithImpl<$Res>
    implements _$SpO2EntryCopyWith<$Res> {
  __$SpO2EntryCopyWithImpl(this._self, this._then);

  final _SpO2Entry _self;
  final $Res Function(_SpO2Entry) _then;

/// Create a copy of SpO2Entry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? percent = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_SpO2Entry(
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
mixin _$RespiratoryRateEntry {

 DateTime get time; double get breathsPerMinute; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of RespiratoryRateEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RespiratoryRateEntryCopyWith<RespiratoryRateEntry> get copyWith => _$RespiratoryRateEntryCopyWithImpl<RespiratoryRateEntry>(this as RespiratoryRateEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RespiratoryRateEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.breathsPerMinute, breathsPerMinute) || other.breathsPerMinute == breathsPerMinute)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,breathsPerMinute,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'RespiratoryRateEntry(time: $time, breathsPerMinute: $breathsPerMinute, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $RespiratoryRateEntryCopyWith<$Res>  {
  factory $RespiratoryRateEntryCopyWith(RespiratoryRateEntry value, $Res Function(RespiratoryRateEntry) _then) = _$RespiratoryRateEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double breathsPerMinute, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$RespiratoryRateEntryCopyWithImpl<$Res>
    implements $RespiratoryRateEntryCopyWith<$Res> {
  _$RespiratoryRateEntryCopyWithImpl(this._self, this._then);

  final RespiratoryRateEntry _self;
  final $Res Function(RespiratoryRateEntry) _then;

/// Create a copy of RespiratoryRateEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? breathsPerMinute = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,breathsPerMinute: null == breathsPerMinute ? _self.breathsPerMinute : breathsPerMinute // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [RespiratoryRateEntry].
extension RespiratoryRateEntryPatterns on RespiratoryRateEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RespiratoryRateEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RespiratoryRateEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RespiratoryRateEntry value)  $default,){
final _that = this;
switch (_that) {
case _RespiratoryRateEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RespiratoryRateEntry value)?  $default,){
final _that = this;
switch (_that) {
case _RespiratoryRateEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double breathsPerMinute,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RespiratoryRateEntry() when $default != null:
return $default(_that.time,_that.breathsPerMinute,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double breathsPerMinute,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _RespiratoryRateEntry():
return $default(_that.time,_that.breathsPerMinute,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double breathsPerMinute,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _RespiratoryRateEntry() when $default != null:
return $default(_that.time,_that.breathsPerMinute,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _RespiratoryRateEntry implements RespiratoryRateEntry {
  const _RespiratoryRateEntry({required this.time, required this.breathsPerMinute, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double breathsPerMinute;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of RespiratoryRateEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RespiratoryRateEntryCopyWith<_RespiratoryRateEntry> get copyWith => __$RespiratoryRateEntryCopyWithImpl<_RespiratoryRateEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RespiratoryRateEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.breathsPerMinute, breathsPerMinute) || other.breathsPerMinute == breathsPerMinute)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,breathsPerMinute,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'RespiratoryRateEntry(time: $time, breathsPerMinute: $breathsPerMinute, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$RespiratoryRateEntryCopyWith<$Res> implements $RespiratoryRateEntryCopyWith<$Res> {
  factory _$RespiratoryRateEntryCopyWith(_RespiratoryRateEntry value, $Res Function(_RespiratoryRateEntry) _then) = __$RespiratoryRateEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double breathsPerMinute, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$RespiratoryRateEntryCopyWithImpl<$Res>
    implements _$RespiratoryRateEntryCopyWith<$Res> {
  __$RespiratoryRateEntryCopyWithImpl(this._self, this._then);

  final _RespiratoryRateEntry _self;
  final $Res Function(_RespiratoryRateEntry) _then;

/// Create a copy of RespiratoryRateEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? breathsPerMinute = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_RespiratoryRateEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,breathsPerMinute: null == breathsPerMinute ? _self.breathsPerMinute : breathsPerMinute // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$BodyTempEntry {

 DateTime get time; double get temperatureCelsius; String get source; String get id; bool get isOpenVitalsEntry;
/// Create a copy of BodyTempEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyTempEntryCopyWith<BodyTempEntry> get copyWith => _$BodyTempEntryCopyWithImpl<BodyTempEntry>(this as BodyTempEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyTempEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,temperatureCelsius,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyTempEntry(time: $time, temperatureCelsius: $temperatureCelsius, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $BodyTempEntryCopyWith<$Res>  {
  factory $BodyTempEntryCopyWith(BodyTempEntry value, $Res Function(BodyTempEntry) _then) = _$BodyTempEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double temperatureCelsius, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$BodyTempEntryCopyWithImpl<$Res>
    implements $BodyTempEntryCopyWith<$Res> {
  _$BodyTempEntryCopyWithImpl(this._self, this._then);

  final BodyTempEntry _self;
  final $Res Function(BodyTempEntry) _then;

/// Create a copy of BodyTempEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? temperatureCelsius = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,temperatureCelsius: null == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyTempEntry].
extension BodyTempEntryPatterns on BodyTempEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyTempEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyTempEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyTempEntry value)  $default,){
final _that = this;
switch (_that) {
case _BodyTempEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyTempEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BodyTempEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double temperatureCelsius,  String source,  String id,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyTempEntry() when $default != null:
return $default(_that.time,_that.temperatureCelsius,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double temperatureCelsius,  String source,  String id,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _BodyTempEntry():
return $default(_that.time,_that.temperatureCelsius,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double temperatureCelsius,  String source,  String id,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _BodyTempEntry() when $default != null:
return $default(_that.time,_that.temperatureCelsius,_that.source,_that.id,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _BodyTempEntry implements BodyTempEntry {
  const _BodyTempEntry({required this.time, required this.temperatureCelsius, required this.source, this.id = '', this.isOpenVitalsEntry = false});
  

@override final  DateTime time;
@override final  double temperatureCelsius;
@override final  String source;
@override@JsonKey() final  String id;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of BodyTempEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyTempEntryCopyWith<_BodyTempEntry> get copyWith => __$BodyTempEntryCopyWithImpl<_BodyTempEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyTempEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,temperatureCelsius,source,id,isOpenVitalsEntry);

@override
String toString() {
  return 'BodyTempEntry(time: $time, temperatureCelsius: $temperatureCelsius, source: $source, id: $id, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$BodyTempEntryCopyWith<$Res> implements $BodyTempEntryCopyWith<$Res> {
  factory _$BodyTempEntryCopyWith(_BodyTempEntry value, $Res Function(_BodyTempEntry) _then) = __$BodyTempEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double temperatureCelsius, String source, String id, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$BodyTempEntryCopyWithImpl<$Res>
    implements _$BodyTempEntryCopyWith<$Res> {
  __$BodyTempEntryCopyWithImpl(this._self, this._then);

  final _BodyTempEntry _self;
  final $Res Function(_BodyTempEntry) _then;

/// Create a copy of BodyTempEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? temperatureCelsius = null,Object? source = null,Object? id = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_BodyTempEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,temperatureCelsius: null == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$BloodGlucoseEntry {

 DateTime get time; double get millimolesPerLiter; int get specimenSource; int get mealType; int get relationToMeal; String get source;
/// Create a copy of BloodGlucoseEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BloodGlucoseEntryCopyWith<BloodGlucoseEntry> get copyWith => _$BloodGlucoseEntryCopyWithImpl<BloodGlucoseEntry>(this as BloodGlucoseEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BloodGlucoseEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.millimolesPerLiter, millimolesPerLiter) || other.millimolesPerLiter == millimolesPerLiter)&&(identical(other.specimenSource, specimenSource) || other.specimenSource == specimenSource)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.relationToMeal, relationToMeal) || other.relationToMeal == relationToMeal)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,millimolesPerLiter,specimenSource,mealType,relationToMeal,source);

@override
String toString() {
  return 'BloodGlucoseEntry(time: $time, millimolesPerLiter: $millimolesPerLiter, specimenSource: $specimenSource, mealType: $mealType, relationToMeal: $relationToMeal, source: $source)';
}


}

/// @nodoc
abstract mixin class $BloodGlucoseEntryCopyWith<$Res>  {
  factory $BloodGlucoseEntryCopyWith(BloodGlucoseEntry value, $Res Function(BloodGlucoseEntry) _then) = _$BloodGlucoseEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double millimolesPerLiter, int specimenSource, int mealType, int relationToMeal, String source
});




}
/// @nodoc
class _$BloodGlucoseEntryCopyWithImpl<$Res>
    implements $BloodGlucoseEntryCopyWith<$Res> {
  _$BloodGlucoseEntryCopyWithImpl(this._self, this._then);

  final BloodGlucoseEntry _self;
  final $Res Function(BloodGlucoseEntry) _then;

/// Create a copy of BloodGlucoseEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? millimolesPerLiter = null,Object? specimenSource = null,Object? mealType = null,Object? relationToMeal = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,millimolesPerLiter: null == millimolesPerLiter ? _self.millimolesPerLiter : millimolesPerLiter // ignore: cast_nullable_to_non_nullable
as double,specimenSource: null == specimenSource ? _self.specimenSource : specimenSource // ignore: cast_nullable_to_non_nullable
as int,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,relationToMeal: null == relationToMeal ? _self.relationToMeal : relationToMeal // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [BloodGlucoseEntry].
extension BloodGlucoseEntryPatterns on BloodGlucoseEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BloodGlucoseEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BloodGlucoseEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BloodGlucoseEntry value)  $default,){
final _that = this;
switch (_that) {
case _BloodGlucoseEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BloodGlucoseEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BloodGlucoseEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double millimolesPerLiter,  int specimenSource,  int mealType,  int relationToMeal,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BloodGlucoseEntry() when $default != null:
return $default(_that.time,_that.millimolesPerLiter,_that.specimenSource,_that.mealType,_that.relationToMeal,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double millimolesPerLiter,  int specimenSource,  int mealType,  int relationToMeal,  String source)  $default,) {final _that = this;
switch (_that) {
case _BloodGlucoseEntry():
return $default(_that.time,_that.millimolesPerLiter,_that.specimenSource,_that.mealType,_that.relationToMeal,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double millimolesPerLiter,  int specimenSource,  int mealType,  int relationToMeal,  String source)?  $default,) {final _that = this;
switch (_that) {
case _BloodGlucoseEntry() when $default != null:
return $default(_that.time,_that.millimolesPerLiter,_that.specimenSource,_that.mealType,_that.relationToMeal,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _BloodGlucoseEntry implements BloodGlucoseEntry {
  const _BloodGlucoseEntry({required this.time, required this.millimolesPerLiter, required this.specimenSource, required this.mealType, required this.relationToMeal, required this.source});
  

@override final  DateTime time;
@override final  double millimolesPerLiter;
@override final  int specimenSource;
@override final  int mealType;
@override final  int relationToMeal;
@override final  String source;

/// Create a copy of BloodGlucoseEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BloodGlucoseEntryCopyWith<_BloodGlucoseEntry> get copyWith => __$BloodGlucoseEntryCopyWithImpl<_BloodGlucoseEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BloodGlucoseEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.millimolesPerLiter, millimolesPerLiter) || other.millimolesPerLiter == millimolesPerLiter)&&(identical(other.specimenSource, specimenSource) || other.specimenSource == specimenSource)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.relationToMeal, relationToMeal) || other.relationToMeal == relationToMeal)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,millimolesPerLiter,specimenSource,mealType,relationToMeal,source);

@override
String toString() {
  return 'BloodGlucoseEntry(time: $time, millimolesPerLiter: $millimolesPerLiter, specimenSource: $specimenSource, mealType: $mealType, relationToMeal: $relationToMeal, source: $source)';
}


}

/// @nodoc
abstract mixin class _$BloodGlucoseEntryCopyWith<$Res> implements $BloodGlucoseEntryCopyWith<$Res> {
  factory _$BloodGlucoseEntryCopyWith(_BloodGlucoseEntry value, $Res Function(_BloodGlucoseEntry) _then) = __$BloodGlucoseEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double millimolesPerLiter, int specimenSource, int mealType, int relationToMeal, String source
});




}
/// @nodoc
class __$BloodGlucoseEntryCopyWithImpl<$Res>
    implements _$BloodGlucoseEntryCopyWith<$Res> {
  __$BloodGlucoseEntryCopyWithImpl(this._self, this._then);

  final _BloodGlucoseEntry _self;
  final $Res Function(_BloodGlucoseEntry) _then;

/// Create a copy of BloodGlucoseEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? millimolesPerLiter = null,Object? specimenSource = null,Object? mealType = null,Object? relationToMeal = null,Object? source = null,}) {
  return _then(_BloodGlucoseEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,millimolesPerLiter: null == millimolesPerLiter ? _self.millimolesPerLiter : millimolesPerLiter // ignore: cast_nullable_to_non_nullable
as double,specimenSource: null == specimenSource ? _self.specimenSource : specimenSource // ignore: cast_nullable_to_non_nullable
as int,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,relationToMeal: null == relationToMeal ? _self.relationToMeal : relationToMeal // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$SkinTemperatureEntry {

 DateTime get startTime; DateTime get endTime; double? get baselineCelsius; double? get averageDeltaCelsius; double? get minDeltaCelsius; double? get maxDeltaCelsius; int get measurementLocation; String get source;
/// Create a copy of SkinTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SkinTemperatureEntryCopyWith<SkinTemperatureEntry> get copyWith => _$SkinTemperatureEntryCopyWithImpl<SkinTemperatureEntry>(this as SkinTemperatureEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SkinTemperatureEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.baselineCelsius, baselineCelsius) || other.baselineCelsius == baselineCelsius)&&(identical(other.averageDeltaCelsius, averageDeltaCelsius) || other.averageDeltaCelsius == averageDeltaCelsius)&&(identical(other.minDeltaCelsius, minDeltaCelsius) || other.minDeltaCelsius == minDeltaCelsius)&&(identical(other.maxDeltaCelsius, maxDeltaCelsius) || other.maxDeltaCelsius == maxDeltaCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,baselineCelsius,averageDeltaCelsius,minDeltaCelsius,maxDeltaCelsius,measurementLocation,source);

@override
String toString() {
  return 'SkinTemperatureEntry(startTime: $startTime, endTime: $endTime, baselineCelsius: $baselineCelsius, averageDeltaCelsius: $averageDeltaCelsius, minDeltaCelsius: $minDeltaCelsius, maxDeltaCelsius: $maxDeltaCelsius, measurementLocation: $measurementLocation, source: $source)';
}


}

/// @nodoc
abstract mixin class $SkinTemperatureEntryCopyWith<$Res>  {
  factory $SkinTemperatureEntryCopyWith(SkinTemperatureEntry value, $Res Function(SkinTemperatureEntry) _then) = _$SkinTemperatureEntryCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, double? baselineCelsius, double? averageDeltaCelsius, double? minDeltaCelsius, double? maxDeltaCelsius, int measurementLocation, String source
});




}
/// @nodoc
class _$SkinTemperatureEntryCopyWithImpl<$Res>
    implements $SkinTemperatureEntryCopyWith<$Res> {
  _$SkinTemperatureEntryCopyWithImpl(this._self, this._then);

  final SkinTemperatureEntry _self;
  final $Res Function(SkinTemperatureEntry) _then;

/// Create a copy of SkinTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? baselineCelsius = freezed,Object? averageDeltaCelsius = freezed,Object? minDeltaCelsius = freezed,Object? maxDeltaCelsius = freezed,Object? measurementLocation = null,Object? source = null,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,baselineCelsius: freezed == baselineCelsius ? _self.baselineCelsius : baselineCelsius // ignore: cast_nullable_to_non_nullable
as double?,averageDeltaCelsius: freezed == averageDeltaCelsius ? _self.averageDeltaCelsius : averageDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,minDeltaCelsius: freezed == minDeltaCelsius ? _self.minDeltaCelsius : minDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,maxDeltaCelsius: freezed == maxDeltaCelsius ? _self.maxDeltaCelsius : maxDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,measurementLocation: null == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [SkinTemperatureEntry].
extension SkinTemperatureEntryPatterns on SkinTemperatureEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SkinTemperatureEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SkinTemperatureEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SkinTemperatureEntry value)  $default,){
final _that = this;
switch (_that) {
case _SkinTemperatureEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SkinTemperatureEntry value)?  $default,){
final _that = this;
switch (_that) {
case _SkinTemperatureEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? baselineCelsius,  double? averageDeltaCelsius,  double? minDeltaCelsius,  double? maxDeltaCelsius,  int measurementLocation,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SkinTemperatureEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.baselineCelsius,_that.averageDeltaCelsius,_that.minDeltaCelsius,_that.maxDeltaCelsius,_that.measurementLocation,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? baselineCelsius,  double? averageDeltaCelsius,  double? minDeltaCelsius,  double? maxDeltaCelsius,  int measurementLocation,  String source)  $default,) {final _that = this;
switch (_that) {
case _SkinTemperatureEntry():
return $default(_that.startTime,_that.endTime,_that.baselineCelsius,_that.averageDeltaCelsius,_that.minDeltaCelsius,_that.maxDeltaCelsius,_that.measurementLocation,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  double? baselineCelsius,  double? averageDeltaCelsius,  double? minDeltaCelsius,  double? maxDeltaCelsius,  int measurementLocation,  String source)?  $default,) {final _that = this;
switch (_that) {
case _SkinTemperatureEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.baselineCelsius,_that.averageDeltaCelsius,_that.minDeltaCelsius,_that.maxDeltaCelsius,_that.measurementLocation,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _SkinTemperatureEntry extends SkinTemperatureEntry {
  const _SkinTemperatureEntry({required this.startTime, required this.endTime, required this.baselineCelsius, required this.averageDeltaCelsius, required this.minDeltaCelsius, required this.maxDeltaCelsius, required this.measurementLocation, required this.source}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  double? baselineCelsius;
@override final  double? averageDeltaCelsius;
@override final  double? minDeltaCelsius;
@override final  double? maxDeltaCelsius;
@override final  int measurementLocation;
@override final  String source;

/// Create a copy of SkinTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SkinTemperatureEntryCopyWith<_SkinTemperatureEntry> get copyWith => __$SkinTemperatureEntryCopyWithImpl<_SkinTemperatureEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SkinTemperatureEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.baselineCelsius, baselineCelsius) || other.baselineCelsius == baselineCelsius)&&(identical(other.averageDeltaCelsius, averageDeltaCelsius) || other.averageDeltaCelsius == averageDeltaCelsius)&&(identical(other.minDeltaCelsius, minDeltaCelsius) || other.minDeltaCelsius == minDeltaCelsius)&&(identical(other.maxDeltaCelsius, maxDeltaCelsius) || other.maxDeltaCelsius == maxDeltaCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,baselineCelsius,averageDeltaCelsius,minDeltaCelsius,maxDeltaCelsius,measurementLocation,source);

@override
String toString() {
  return 'SkinTemperatureEntry(startTime: $startTime, endTime: $endTime, baselineCelsius: $baselineCelsius, averageDeltaCelsius: $averageDeltaCelsius, minDeltaCelsius: $minDeltaCelsius, maxDeltaCelsius: $maxDeltaCelsius, measurementLocation: $measurementLocation, source: $source)';
}


}

/// @nodoc
abstract mixin class _$SkinTemperatureEntryCopyWith<$Res> implements $SkinTemperatureEntryCopyWith<$Res> {
  factory _$SkinTemperatureEntryCopyWith(_SkinTemperatureEntry value, $Res Function(_SkinTemperatureEntry) _then) = __$SkinTemperatureEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, double? baselineCelsius, double? averageDeltaCelsius, double? minDeltaCelsius, double? maxDeltaCelsius, int measurementLocation, String source
});




}
/// @nodoc
class __$SkinTemperatureEntryCopyWithImpl<$Res>
    implements _$SkinTemperatureEntryCopyWith<$Res> {
  __$SkinTemperatureEntryCopyWithImpl(this._self, this._then);

  final _SkinTemperatureEntry _self;
  final $Res Function(_SkinTemperatureEntry) _then;

/// Create a copy of SkinTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? baselineCelsius = freezed,Object? averageDeltaCelsius = freezed,Object? minDeltaCelsius = freezed,Object? maxDeltaCelsius = freezed,Object? measurementLocation = null,Object? source = null,}) {
  return _then(_SkinTemperatureEntry(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,baselineCelsius: freezed == baselineCelsius ? _self.baselineCelsius : baselineCelsius // ignore: cast_nullable_to_non_nullable
as double?,averageDeltaCelsius: freezed == averageDeltaCelsius ? _self.averageDeltaCelsius : averageDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,minDeltaCelsius: freezed == minDeltaCelsius ? _self.minDeltaCelsius : minDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,maxDeltaCelsius: freezed == maxDeltaCelsius ? _self.maxDeltaCelsius : maxDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,measurementLocation: null == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$Vo2MaxEntry {

 DateTime get time; double get vo2MaxMlPerKgPerMin; String get source;
/// Create a copy of Vo2MaxEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$Vo2MaxEntryCopyWith<Vo2MaxEntry> get copyWith => _$Vo2MaxEntryCopyWithImpl<Vo2MaxEntry>(this as Vo2MaxEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is Vo2MaxEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.vo2MaxMlPerKgPerMin, vo2MaxMlPerKgPerMin) || other.vo2MaxMlPerKgPerMin == vo2MaxMlPerKgPerMin)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,vo2MaxMlPerKgPerMin,source);

@override
String toString() {
  return 'Vo2MaxEntry(time: $time, vo2MaxMlPerKgPerMin: $vo2MaxMlPerKgPerMin, source: $source)';
}


}

/// @nodoc
abstract mixin class $Vo2MaxEntryCopyWith<$Res>  {
  factory $Vo2MaxEntryCopyWith(Vo2MaxEntry value, $Res Function(Vo2MaxEntry) _then) = _$Vo2MaxEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double vo2MaxMlPerKgPerMin, String source
});




}
/// @nodoc
class _$Vo2MaxEntryCopyWithImpl<$Res>
    implements $Vo2MaxEntryCopyWith<$Res> {
  _$Vo2MaxEntryCopyWithImpl(this._self, this._then);

  final Vo2MaxEntry _self;
  final $Res Function(Vo2MaxEntry) _then;

/// Create a copy of Vo2MaxEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? vo2MaxMlPerKgPerMin = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,vo2MaxMlPerKgPerMin: null == vo2MaxMlPerKgPerMin ? _self.vo2MaxMlPerKgPerMin : vo2MaxMlPerKgPerMin // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [Vo2MaxEntry].
extension Vo2MaxEntryPatterns on Vo2MaxEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _Vo2MaxEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _Vo2MaxEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _Vo2MaxEntry value)  $default,){
final _that = this;
switch (_that) {
case _Vo2MaxEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _Vo2MaxEntry value)?  $default,){
final _that = this;
switch (_that) {
case _Vo2MaxEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double vo2MaxMlPerKgPerMin,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _Vo2MaxEntry() when $default != null:
return $default(_that.time,_that.vo2MaxMlPerKgPerMin,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double vo2MaxMlPerKgPerMin,  String source)  $default,) {final _that = this;
switch (_that) {
case _Vo2MaxEntry():
return $default(_that.time,_that.vo2MaxMlPerKgPerMin,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double vo2MaxMlPerKgPerMin,  String source)?  $default,) {final _that = this;
switch (_that) {
case _Vo2MaxEntry() when $default != null:
return $default(_that.time,_that.vo2MaxMlPerKgPerMin,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _Vo2MaxEntry implements Vo2MaxEntry {
  const _Vo2MaxEntry({required this.time, required this.vo2MaxMlPerKgPerMin, required this.source});
  

@override final  DateTime time;
@override final  double vo2MaxMlPerKgPerMin;
@override final  String source;

/// Create a copy of Vo2MaxEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$Vo2MaxEntryCopyWith<_Vo2MaxEntry> get copyWith => __$Vo2MaxEntryCopyWithImpl<_Vo2MaxEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _Vo2MaxEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.vo2MaxMlPerKgPerMin, vo2MaxMlPerKgPerMin) || other.vo2MaxMlPerKgPerMin == vo2MaxMlPerKgPerMin)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,vo2MaxMlPerKgPerMin,source);

@override
String toString() {
  return 'Vo2MaxEntry(time: $time, vo2MaxMlPerKgPerMin: $vo2MaxMlPerKgPerMin, source: $source)';
}


}

/// @nodoc
abstract mixin class _$Vo2MaxEntryCopyWith<$Res> implements $Vo2MaxEntryCopyWith<$Res> {
  factory _$Vo2MaxEntryCopyWith(_Vo2MaxEntry value, $Res Function(_Vo2MaxEntry) _then) = __$Vo2MaxEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double vo2MaxMlPerKgPerMin, String source
});




}
/// @nodoc
class __$Vo2MaxEntryCopyWithImpl<$Res>
    implements _$Vo2MaxEntryCopyWith<$Res> {
  __$Vo2MaxEntryCopyWithImpl(this._self, this._then);

  final _Vo2MaxEntry _self;
  final $Res Function(_Vo2MaxEntry) _then;

/// Create a copy of Vo2MaxEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? vo2MaxMlPerKgPerMin = null,Object? source = null,}) {
  return _then(_Vo2MaxEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,vo2MaxMlPerKgPerMin: null == vo2MaxMlPerKgPerMin ? _self.vo2MaxMlPerKgPerMin : vo2MaxMlPerKgPerMin // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$VitalsMeasurementWriteRequest {

 VitalsMeasurementType get type; DateTime get time; double get value; double? get secondaryValue;
/// Create a copy of VitalsMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$VitalsMeasurementWriteRequestCopyWith<VitalsMeasurementWriteRequest> get copyWith => _$VitalsMeasurementWriteRequestCopyWithImpl<VitalsMeasurementWriteRequest>(this as VitalsMeasurementWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is VitalsMeasurementWriteRequest&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.secondaryValue, secondaryValue) || other.secondaryValue == secondaryValue));
}


@override
int get hashCode => Object.hash(runtimeType,type,time,value,secondaryValue);

@override
String toString() {
  return 'VitalsMeasurementWriteRequest(type: $type, time: $time, value: $value, secondaryValue: $secondaryValue)';
}


}

/// @nodoc
abstract mixin class $VitalsMeasurementWriteRequestCopyWith<$Res>  {
  factory $VitalsMeasurementWriteRequestCopyWith(VitalsMeasurementWriteRequest value, $Res Function(VitalsMeasurementWriteRequest) _then) = _$VitalsMeasurementWriteRequestCopyWithImpl;
@useResult
$Res call({
 VitalsMeasurementType type, DateTime time, double value, double? secondaryValue
});




}
/// @nodoc
class _$VitalsMeasurementWriteRequestCopyWithImpl<$Res>
    implements $VitalsMeasurementWriteRequestCopyWith<$Res> {
  _$VitalsMeasurementWriteRequestCopyWithImpl(this._self, this._then);

  final VitalsMeasurementWriteRequest _self;
  final $Res Function(VitalsMeasurementWriteRequest) _then;

/// Create a copy of VitalsMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? time = null,Object? value = null,Object? secondaryValue = freezed,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,secondaryValue: freezed == secondaryValue ? _self.secondaryValue : secondaryValue // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [VitalsMeasurementWriteRequest].
extension VitalsMeasurementWriteRequestPatterns on VitalsMeasurementWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _VitalsMeasurementWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _VitalsMeasurementWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _VitalsMeasurementWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest() when $default != null:
return $default(_that.type,_that.time,_that.value,_that.secondaryValue);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue)  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest():
return $default(_that.type,_that.time,_that.value,_that.secondaryValue);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue)?  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementWriteRequest() when $default != null:
return $default(_that.type,_that.time,_that.value,_that.secondaryValue);case _:
  return null;

}
}

}

/// @nodoc


class _VitalsMeasurementWriteRequest implements VitalsMeasurementWriteRequest {
  const _VitalsMeasurementWriteRequest({required this.type, required this.time, required this.value, this.secondaryValue});
  

@override final  VitalsMeasurementType type;
@override final  DateTime time;
@override final  double value;
@override final  double? secondaryValue;

/// Create a copy of VitalsMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$VitalsMeasurementWriteRequestCopyWith<_VitalsMeasurementWriteRequest> get copyWith => __$VitalsMeasurementWriteRequestCopyWithImpl<_VitalsMeasurementWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _VitalsMeasurementWriteRequest&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.secondaryValue, secondaryValue) || other.secondaryValue == secondaryValue));
}


@override
int get hashCode => Object.hash(runtimeType,type,time,value,secondaryValue);

@override
String toString() {
  return 'VitalsMeasurementWriteRequest(type: $type, time: $time, value: $value, secondaryValue: $secondaryValue)';
}


}

/// @nodoc
abstract mixin class _$VitalsMeasurementWriteRequestCopyWith<$Res> implements $VitalsMeasurementWriteRequestCopyWith<$Res> {
  factory _$VitalsMeasurementWriteRequestCopyWith(_VitalsMeasurementWriteRequest value, $Res Function(_VitalsMeasurementWriteRequest) _then) = __$VitalsMeasurementWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 VitalsMeasurementType type, DateTime time, double value, double? secondaryValue
});




}
/// @nodoc
class __$VitalsMeasurementWriteRequestCopyWithImpl<$Res>
    implements _$VitalsMeasurementWriteRequestCopyWith<$Res> {
  __$VitalsMeasurementWriteRequestCopyWithImpl(this._self, this._then);

  final _VitalsMeasurementWriteRequest _self;
  final $Res Function(_VitalsMeasurementWriteRequest) _then;

/// Create a copy of VitalsMeasurementWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? time = null,Object? value = null,Object? secondaryValue = freezed,}) {
  return _then(_VitalsMeasurementWriteRequest(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,secondaryValue: freezed == secondaryValue ? _self.secondaryValue : secondaryValue // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$VitalsMeasurementEntry {

 String get id; VitalsMeasurementType get type; DateTime get time; double get value; double? get secondaryValue; String get source; bool get isOpenVitalsEntry;
/// Create a copy of VitalsMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$VitalsMeasurementEntryCopyWith<VitalsMeasurementEntry> get copyWith => _$VitalsMeasurementEntryCopyWithImpl<VitalsMeasurementEntry>(this as VitalsMeasurementEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is VitalsMeasurementEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.secondaryValue, secondaryValue) || other.secondaryValue == secondaryValue)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,type,time,value,secondaryValue,source,isOpenVitalsEntry);

@override
String toString() {
  return 'VitalsMeasurementEntry(id: $id, type: $type, time: $time, value: $value, secondaryValue: $secondaryValue, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $VitalsMeasurementEntryCopyWith<$Res>  {
  factory $VitalsMeasurementEntryCopyWith(VitalsMeasurementEntry value, $Res Function(VitalsMeasurementEntry) _then) = _$VitalsMeasurementEntryCopyWithImpl;
@useResult
$Res call({
 String id, VitalsMeasurementType type, DateTime time, double value, double? secondaryValue, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$VitalsMeasurementEntryCopyWithImpl<$Res>
    implements $VitalsMeasurementEntryCopyWith<$Res> {
  _$VitalsMeasurementEntryCopyWithImpl(this._self, this._then);

  final VitalsMeasurementEntry _self;
  final $Res Function(VitalsMeasurementEntry) _then;

/// Create a copy of VitalsMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? type = null,Object? time = null,Object? value = null,Object? secondaryValue = freezed,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,secondaryValue: freezed == secondaryValue ? _self.secondaryValue : secondaryValue // ignore: cast_nullable_to_non_nullable
as double?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [VitalsMeasurementEntry].
extension VitalsMeasurementEntryPatterns on VitalsMeasurementEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _VitalsMeasurementEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _VitalsMeasurementEntry value)  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _VitalsMeasurementEntry value)?  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue,  String source,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntry() when $default != null:
return $default(_that.id,_that.type,_that.time,_that.value,_that.secondaryValue,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue,  String source,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntry():
return $default(_that.id,_that.type,_that.time,_that.value,_that.secondaryValue,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  VitalsMeasurementType type,  DateTime time,  double value,  double? secondaryValue,  String source,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntry() when $default != null:
return $default(_that.id,_that.type,_that.time,_that.value,_that.secondaryValue,_that.source,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _VitalsMeasurementEntry implements VitalsMeasurementEntry {
  const _VitalsMeasurementEntry({required this.id, required this.type, required this.time, required this.value, this.secondaryValue, required this.source, required this.isOpenVitalsEntry});
  

@override final  String id;
@override final  VitalsMeasurementType type;
@override final  DateTime time;
@override final  double value;
@override final  double? secondaryValue;
@override final  String source;
@override final  bool isOpenVitalsEntry;

/// Create a copy of VitalsMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$VitalsMeasurementEntryCopyWith<_VitalsMeasurementEntry> get copyWith => __$VitalsMeasurementEntryCopyWithImpl<_VitalsMeasurementEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _VitalsMeasurementEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.type, type) || other.type == type)&&(identical(other.time, time) || other.time == time)&&(identical(other.value, value) || other.value == value)&&(identical(other.secondaryValue, secondaryValue) || other.secondaryValue == secondaryValue)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,type,time,value,secondaryValue,source,isOpenVitalsEntry);

@override
String toString() {
  return 'VitalsMeasurementEntry(id: $id, type: $type, time: $time, value: $value, secondaryValue: $secondaryValue, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$VitalsMeasurementEntryCopyWith<$Res> implements $VitalsMeasurementEntryCopyWith<$Res> {
  factory _$VitalsMeasurementEntryCopyWith(_VitalsMeasurementEntry value, $Res Function(_VitalsMeasurementEntry) _then) = __$VitalsMeasurementEntryCopyWithImpl;
@override @useResult
$Res call({
 String id, VitalsMeasurementType type, DateTime time, double value, double? secondaryValue, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$VitalsMeasurementEntryCopyWithImpl<$Res>
    implements _$VitalsMeasurementEntryCopyWith<$Res> {
  __$VitalsMeasurementEntryCopyWithImpl(this._self, this._then);

  final _VitalsMeasurementEntry _self;
  final $Res Function(_VitalsMeasurementEntry) _then;

/// Create a copy of VitalsMeasurementEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? type = null,Object? time = null,Object? value = null,Object? secondaryValue = freezed,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_VitalsMeasurementEntry(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,secondaryValue: freezed == secondaryValue ? _self.secondaryValue : secondaryValue // ignore: cast_nullable_to_non_nullable
as double?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
