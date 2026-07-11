// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'caffeine_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaffeineEntry {

 String get id; DateTime get startTime; DateTime get endTime; double get caffeineMg; String? get name; String get source; int get mealType; String? get clientRecordId; bool get isOpenVitalsEntry;
/// Create a copy of CaffeineEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineEntryCopyWith<CaffeineEntry> get copyWith => _$CaffeineEntryCopyWithImpl<CaffeineEntry>(this as CaffeineEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.caffeineMg, caffeineMg) || other.caffeineMg == caffeineMg)&&(identical(other.name, name) || other.name == name)&&(identical(other.source, source) || other.source == source)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,startTime,endTime,caffeineMg,name,source,mealType,clientRecordId,isOpenVitalsEntry);

@override
String toString() {
  return 'CaffeineEntry(id: $id, startTime: $startTime, endTime: $endTime, caffeineMg: $caffeineMg, name: $name, source: $source, mealType: $mealType, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $CaffeineEntryCopyWith<$Res>  {
  factory $CaffeineEntryCopyWith(CaffeineEntry value, $Res Function(CaffeineEntry) _then) = _$CaffeineEntryCopyWithImpl;
@useResult
$Res call({
 String id, DateTime startTime, DateTime endTime, double caffeineMg, String? name, String source, int mealType, String? clientRecordId, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$CaffeineEntryCopyWithImpl<$Res>
    implements $CaffeineEntryCopyWith<$Res> {
  _$CaffeineEntryCopyWithImpl(this._self, this._then);

  final CaffeineEntry _self;
  final $Res Function(CaffeineEntry) _then;

/// Create a copy of CaffeineEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? startTime = null,Object? endTime = null,Object? caffeineMg = null,Object? name = freezed,Object? source = null,Object? mealType = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,caffeineMg: null == caffeineMg ? _self.caffeineMg : caffeineMg // ignore: cast_nullable_to_non_nullable
as double,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineEntry].
extension CaffeineEntryPatterns on CaffeineEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineEntry value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineEntry value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  DateTime startTime,  DateTime endTime,  double caffeineMg,  String? name,  String source,  int mealType,  String? clientRecordId,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineEntry() when $default != null:
return $default(_that.id,_that.startTime,_that.endTime,_that.caffeineMg,_that.name,_that.source,_that.mealType,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  DateTime startTime,  DateTime endTime,  double caffeineMg,  String? name,  String source,  int mealType,  String? clientRecordId,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _CaffeineEntry():
return $default(_that.id,_that.startTime,_that.endTime,_that.caffeineMg,_that.name,_that.source,_that.mealType,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  DateTime startTime,  DateTime endTime,  double caffeineMg,  String? name,  String source,  int mealType,  String? clientRecordId,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineEntry() when $default != null:
return $default(_that.id,_that.startTime,_that.endTime,_that.caffeineMg,_that.name,_that.source,_that.mealType,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineEntry implements CaffeineEntry {
  const _CaffeineEntry({required this.id, required this.startTime, required this.endTime, required this.caffeineMg, required this.name, required this.source, required this.mealType, this.clientRecordId, this.isOpenVitalsEntry = false});
  

@override final  String id;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  double caffeineMg;
@override final  String? name;
@override final  String source;
@override final  int mealType;
@override final  String? clientRecordId;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of CaffeineEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineEntryCopyWith<_CaffeineEntry> get copyWith => __$CaffeineEntryCopyWithImpl<_CaffeineEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineEntry&&(identical(other.id, id) || other.id == id)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.caffeineMg, caffeineMg) || other.caffeineMg == caffeineMg)&&(identical(other.name, name) || other.name == name)&&(identical(other.source, source) || other.source == source)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,startTime,endTime,caffeineMg,name,source,mealType,clientRecordId,isOpenVitalsEntry);

@override
String toString() {
  return 'CaffeineEntry(id: $id, startTime: $startTime, endTime: $endTime, caffeineMg: $caffeineMg, name: $name, source: $source, mealType: $mealType, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$CaffeineEntryCopyWith<$Res> implements $CaffeineEntryCopyWith<$Res> {
  factory _$CaffeineEntryCopyWith(_CaffeineEntry value, $Res Function(_CaffeineEntry) _then) = __$CaffeineEntryCopyWithImpl;
@override @useResult
$Res call({
 String id, DateTime startTime, DateTime endTime, double caffeineMg, String? name, String source, int mealType, String? clientRecordId, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$CaffeineEntryCopyWithImpl<$Res>
    implements _$CaffeineEntryCopyWith<$Res> {
  __$CaffeineEntryCopyWithImpl(this._self, this._then);

  final _CaffeineEntry _self;
  final $Res Function(_CaffeineEntry) _then;

/// Create a copy of CaffeineEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? startTime = null,Object? endTime = null,Object? caffeineMg = null,Object? name = freezed,Object? source = null,Object? mealType = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,}) {
  return _then(_CaffeineEntry(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,caffeineMg: null == caffeineMg ? _self.caffeineMg : caffeineMg // ignore: cast_nullable_to_non_nullable
as double,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$CaffeinePeriodData {

 List<CaffeineEntry> get entries;
/// Create a copy of CaffeinePeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeinePeriodDataCopyWith<CaffeinePeriodData> get copyWith => _$CaffeinePeriodDataCopyWithImpl<CaffeinePeriodData>(this as CaffeinePeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeinePeriodData&&const DeepCollectionEquality().equals(other.entries, entries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(entries));

@override
String toString() {
  return 'CaffeinePeriodData(entries: $entries)';
}


}

/// @nodoc
abstract mixin class $CaffeinePeriodDataCopyWith<$Res>  {
  factory $CaffeinePeriodDataCopyWith(CaffeinePeriodData value, $Res Function(CaffeinePeriodData) _then) = _$CaffeinePeriodDataCopyWithImpl;
@useResult
$Res call({
 List<CaffeineEntry> entries
});




}
/// @nodoc
class _$CaffeinePeriodDataCopyWithImpl<$Res>
    implements $CaffeinePeriodDataCopyWith<$Res> {
  _$CaffeinePeriodDataCopyWithImpl(this._self, this._then);

  final CaffeinePeriodData _self;
  final $Res Function(CaffeinePeriodData) _then;

/// Create a copy of CaffeinePeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? entries = null,}) {
  return _then(_self.copyWith(
entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntry>,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeinePeriodData].
extension CaffeinePeriodDataPatterns on CaffeinePeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeinePeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeinePeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeinePeriodData value)  $default,){
final _that = this;
switch (_that) {
case _CaffeinePeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeinePeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeinePeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<CaffeineEntry> entries)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeinePeriodData() when $default != null:
return $default(_that.entries);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<CaffeineEntry> entries)  $default,) {final _that = this;
switch (_that) {
case _CaffeinePeriodData():
return $default(_that.entries);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<CaffeineEntry> entries)?  $default,) {final _that = this;
switch (_that) {
case _CaffeinePeriodData() when $default != null:
return $default(_that.entries);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeinePeriodData implements CaffeinePeriodData {
  const _CaffeinePeriodData({required final  List<CaffeineEntry> entries}): _entries = entries;
  

 final  List<CaffeineEntry> _entries;
@override List<CaffeineEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}


/// Create a copy of CaffeinePeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeinePeriodDataCopyWith<_CaffeinePeriodData> get copyWith => __$CaffeinePeriodDataCopyWithImpl<_CaffeinePeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeinePeriodData&&const DeepCollectionEquality().equals(other._entries, _entries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_entries));

@override
String toString() {
  return 'CaffeinePeriodData(entries: $entries)';
}


}

/// @nodoc
abstract mixin class _$CaffeinePeriodDataCopyWith<$Res> implements $CaffeinePeriodDataCopyWith<$Res> {
  factory _$CaffeinePeriodDataCopyWith(_CaffeinePeriodData value, $Res Function(_CaffeinePeriodData) _then) = __$CaffeinePeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<CaffeineEntry> entries
});




}
/// @nodoc
class __$CaffeinePeriodDataCopyWithImpl<$Res>
    implements _$CaffeinePeriodDataCopyWith<$Res> {
  __$CaffeinePeriodDataCopyWithImpl(this._self, this._then);

  final _CaffeinePeriodData _self;
  final $Res Function(_CaffeinePeriodData) _then;

/// Create a copy of CaffeinePeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? entries = null,}) {
  return _then(_CaffeinePeriodData(
entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntry>,
  ));
}


}

/// @nodoc
mixin _$CaffeinePoint {

 DateTime get time; double get valueMg;
/// Create a copy of CaffeinePoint
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeinePointCopyWith<CaffeinePoint> get copyWith => _$CaffeinePointCopyWithImpl<CaffeinePoint>(this as CaffeinePoint, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeinePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,time,valueMg);

@override
String toString() {
  return 'CaffeinePoint(time: $time, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class $CaffeinePointCopyWith<$Res>  {
  factory $CaffeinePointCopyWith(CaffeinePoint value, $Res Function(CaffeinePoint) _then) = _$CaffeinePointCopyWithImpl;
@useResult
$Res call({
 DateTime time, double valueMg
});




}
/// @nodoc
class _$CaffeinePointCopyWithImpl<$Res>
    implements $CaffeinePointCopyWith<$Res> {
  _$CaffeinePointCopyWithImpl(this._self, this._then);

  final CaffeinePoint _self;
  final $Res Function(CaffeinePoint) _then;

/// Create a copy of CaffeinePoint
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? valueMg = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeinePoint].
extension CaffeinePointPatterns on CaffeinePoint {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeinePoint value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeinePoint() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeinePoint value)  $default,){
final _that = this;
switch (_that) {
case _CaffeinePoint():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeinePoint value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeinePoint() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double valueMg)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeinePoint() when $default != null:
return $default(_that.time,_that.valueMg);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double valueMg)  $default,) {final _that = this;
switch (_that) {
case _CaffeinePoint():
return $default(_that.time,_that.valueMg);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double valueMg)?  $default,) {final _that = this;
switch (_that) {
case _CaffeinePoint() when $default != null:
return $default(_that.time,_that.valueMg);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeinePoint implements CaffeinePoint {
  const _CaffeinePoint({required this.time, required this.valueMg});
  

@override final  DateTime time;
@override final  double valueMg;

/// Create a copy of CaffeinePoint
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeinePointCopyWith<_CaffeinePoint> get copyWith => __$CaffeinePointCopyWithImpl<_CaffeinePoint>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeinePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,time,valueMg);

@override
String toString() {
  return 'CaffeinePoint(time: $time, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class _$CaffeinePointCopyWith<$Res> implements $CaffeinePointCopyWith<$Res> {
  factory _$CaffeinePointCopyWith(_CaffeinePoint value, $Res Function(_CaffeinePoint) _then) = __$CaffeinePointCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double valueMg
});




}
/// @nodoc
class __$CaffeinePointCopyWithImpl<$Res>
    implements _$CaffeinePointCopyWith<$Res> {
  __$CaffeinePointCopyWithImpl(this._self, this._then);

  final _CaffeinePoint _self;
  final $Res Function(_CaffeinePoint) _then;

/// Create a copy of CaffeinePoint
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? valueMg = null,}) {
  return _then(_CaffeinePoint(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CaffeineEntryInsight {

 CaffeineEntry get entry; double get currentContributionMg; DateTime get peakTime; double get peakMg; List<CaffeinePoint> get contributionPoints; CaffeineSourceCategory get inferredCategory; CaffeineCatalogMatch? get catalogMatch;
/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineEntryInsightCopyWith<CaffeineEntryInsight> get copyWith => _$CaffeineEntryInsightCopyWithImpl<CaffeineEntryInsight>(this as CaffeineEntryInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineEntryInsight&&(identical(other.entry, entry) || other.entry == entry)&&(identical(other.currentContributionMg, currentContributionMg) || other.currentContributionMg == currentContributionMg)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.peakMg, peakMg) || other.peakMg == peakMg)&&const DeepCollectionEquality().equals(other.contributionPoints, contributionPoints)&&(identical(other.inferredCategory, inferredCategory) || other.inferredCategory == inferredCategory)&&(identical(other.catalogMatch, catalogMatch) || other.catalogMatch == catalogMatch));
}


@override
int get hashCode => Object.hash(runtimeType,entry,currentContributionMg,peakTime,peakMg,const DeepCollectionEquality().hash(contributionPoints),inferredCategory,catalogMatch);

@override
String toString() {
  return 'CaffeineEntryInsight(entry: $entry, currentContributionMg: $currentContributionMg, peakTime: $peakTime, peakMg: $peakMg, contributionPoints: $contributionPoints, inferredCategory: $inferredCategory, catalogMatch: $catalogMatch)';
}


}

/// @nodoc
abstract mixin class $CaffeineEntryInsightCopyWith<$Res>  {
  factory $CaffeineEntryInsightCopyWith(CaffeineEntryInsight value, $Res Function(CaffeineEntryInsight) _then) = _$CaffeineEntryInsightCopyWithImpl;
@useResult
$Res call({
 CaffeineEntry entry, double currentContributionMg, DateTime peakTime, double peakMg, List<CaffeinePoint> contributionPoints, CaffeineSourceCategory inferredCategory, CaffeineCatalogMatch? catalogMatch
});


$CaffeineEntryCopyWith<$Res> get entry;$CaffeineCatalogMatchCopyWith<$Res>? get catalogMatch;

}
/// @nodoc
class _$CaffeineEntryInsightCopyWithImpl<$Res>
    implements $CaffeineEntryInsightCopyWith<$Res> {
  _$CaffeineEntryInsightCopyWithImpl(this._self, this._then);

  final CaffeineEntryInsight _self;
  final $Res Function(CaffeineEntryInsight) _then;

/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? entry = null,Object? currentContributionMg = null,Object? peakTime = null,Object? peakMg = null,Object? contributionPoints = null,Object? inferredCategory = null,Object? catalogMatch = freezed,}) {
  return _then(_self.copyWith(
entry: null == entry ? _self.entry : entry // ignore: cast_nullable_to_non_nullable
as CaffeineEntry,currentContributionMg: null == currentContributionMg ? _self.currentContributionMg : currentContributionMg // ignore: cast_nullable_to_non_nullable
as double,peakTime: null == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime,peakMg: null == peakMg ? _self.peakMg : peakMg // ignore: cast_nullable_to_non_nullable
as double,contributionPoints: null == contributionPoints ? _self.contributionPoints : contributionPoints // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,inferredCategory: null == inferredCategory ? _self.inferredCategory : inferredCategory // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory,catalogMatch: freezed == catalogMatch ? _self.catalogMatch : catalogMatch // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogMatch?,
  ));
}
/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineEntryCopyWith<$Res> get entry {
  
  return $CaffeineEntryCopyWith<$Res>(_self.entry, (value) {
    return _then(_self.copyWith(entry: value));
  });
}/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineCatalogMatchCopyWith<$Res>? get catalogMatch {
    if (_self.catalogMatch == null) {
    return null;
  }

  return $CaffeineCatalogMatchCopyWith<$Res>(_self.catalogMatch!, (value) {
    return _then(_self.copyWith(catalogMatch: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineEntryInsight].
extension CaffeineEntryInsightPatterns on CaffeineEntryInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineEntryInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineEntryInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineEntryInsight value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineEntryInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineEntryInsight value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineEntryInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineEntry entry,  double currentContributionMg,  DateTime peakTime,  double peakMg,  List<CaffeinePoint> contributionPoints,  CaffeineSourceCategory inferredCategory,  CaffeineCatalogMatch? catalogMatch)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineEntryInsight() when $default != null:
return $default(_that.entry,_that.currentContributionMg,_that.peakTime,_that.peakMg,_that.contributionPoints,_that.inferredCategory,_that.catalogMatch);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineEntry entry,  double currentContributionMg,  DateTime peakTime,  double peakMg,  List<CaffeinePoint> contributionPoints,  CaffeineSourceCategory inferredCategory,  CaffeineCatalogMatch? catalogMatch)  $default,) {final _that = this;
switch (_that) {
case _CaffeineEntryInsight():
return $default(_that.entry,_that.currentContributionMg,_that.peakTime,_that.peakMg,_that.contributionPoints,_that.inferredCategory,_that.catalogMatch);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineEntry entry,  double currentContributionMg,  DateTime peakTime,  double peakMg,  List<CaffeinePoint> contributionPoints,  CaffeineSourceCategory inferredCategory,  CaffeineCatalogMatch? catalogMatch)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineEntryInsight() when $default != null:
return $default(_that.entry,_that.currentContributionMg,_that.peakTime,_that.peakMg,_that.contributionPoints,_that.inferredCategory,_that.catalogMatch);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineEntryInsight implements CaffeineEntryInsight {
  const _CaffeineEntryInsight({required this.entry, required this.currentContributionMg, required this.peakTime, required this.peakMg, required final  List<CaffeinePoint> contributionPoints, required this.inferredCategory, this.catalogMatch}): _contributionPoints = contributionPoints;
  

@override final  CaffeineEntry entry;
@override final  double currentContributionMg;
@override final  DateTime peakTime;
@override final  double peakMg;
 final  List<CaffeinePoint> _contributionPoints;
@override List<CaffeinePoint> get contributionPoints {
  if (_contributionPoints is EqualUnmodifiableListView) return _contributionPoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_contributionPoints);
}

@override final  CaffeineSourceCategory inferredCategory;
@override final  CaffeineCatalogMatch? catalogMatch;

/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineEntryInsightCopyWith<_CaffeineEntryInsight> get copyWith => __$CaffeineEntryInsightCopyWithImpl<_CaffeineEntryInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineEntryInsight&&(identical(other.entry, entry) || other.entry == entry)&&(identical(other.currentContributionMg, currentContributionMg) || other.currentContributionMg == currentContributionMg)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.peakMg, peakMg) || other.peakMg == peakMg)&&const DeepCollectionEquality().equals(other._contributionPoints, _contributionPoints)&&(identical(other.inferredCategory, inferredCategory) || other.inferredCategory == inferredCategory)&&(identical(other.catalogMatch, catalogMatch) || other.catalogMatch == catalogMatch));
}


@override
int get hashCode => Object.hash(runtimeType,entry,currentContributionMg,peakTime,peakMg,const DeepCollectionEquality().hash(_contributionPoints),inferredCategory,catalogMatch);

@override
String toString() {
  return 'CaffeineEntryInsight(entry: $entry, currentContributionMg: $currentContributionMg, peakTime: $peakTime, peakMg: $peakMg, contributionPoints: $contributionPoints, inferredCategory: $inferredCategory, catalogMatch: $catalogMatch)';
}


}

/// @nodoc
abstract mixin class _$CaffeineEntryInsightCopyWith<$Res> implements $CaffeineEntryInsightCopyWith<$Res> {
  factory _$CaffeineEntryInsightCopyWith(_CaffeineEntryInsight value, $Res Function(_CaffeineEntryInsight) _then) = __$CaffeineEntryInsightCopyWithImpl;
@override @useResult
$Res call({
 CaffeineEntry entry, double currentContributionMg, DateTime peakTime, double peakMg, List<CaffeinePoint> contributionPoints, CaffeineSourceCategory inferredCategory, CaffeineCatalogMatch? catalogMatch
});


@override $CaffeineEntryCopyWith<$Res> get entry;@override $CaffeineCatalogMatchCopyWith<$Res>? get catalogMatch;

}
/// @nodoc
class __$CaffeineEntryInsightCopyWithImpl<$Res>
    implements _$CaffeineEntryInsightCopyWith<$Res> {
  __$CaffeineEntryInsightCopyWithImpl(this._self, this._then);

  final _CaffeineEntryInsight _self;
  final $Res Function(_CaffeineEntryInsight) _then;

/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? entry = null,Object? currentContributionMg = null,Object? peakTime = null,Object? peakMg = null,Object? contributionPoints = null,Object? inferredCategory = null,Object? catalogMatch = freezed,}) {
  return _then(_CaffeineEntryInsight(
entry: null == entry ? _self.entry : entry // ignore: cast_nullable_to_non_nullable
as CaffeineEntry,currentContributionMg: null == currentContributionMg ? _self.currentContributionMg : currentContributionMg // ignore: cast_nullable_to_non_nullable
as double,peakTime: null == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime,peakMg: null == peakMg ? _self.peakMg : peakMg // ignore: cast_nullable_to_non_nullable
as double,contributionPoints: null == contributionPoints ? _self._contributionPoints : contributionPoints // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,inferredCategory: null == inferredCategory ? _self.inferredCategory : inferredCategory // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory,catalogMatch: freezed == catalogMatch ? _self.catalogMatch : catalogMatch // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogMatch?,
  ));
}

/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineEntryCopyWith<$Res> get entry {
  
  return $CaffeineEntryCopyWith<$Res>(_self.entry, (value) {
    return _then(_self.copyWith(entry: value));
  });
}/// Create a copy of CaffeineEntryInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineCatalogMatchCopyWith<$Res>? get catalogMatch {
    if (_self.catalogMatch == null) {
    return null;
  }

  return $CaffeineCatalogMatchCopyWith<$Res>(_self.catalogMatch!, (value) {
    return _then(_self.copyWith(catalogMatch: value));
  });
}
}

/// @nodoc
mixin _$CaffeineDailyStat {

 LocalDate get date; double get totalMg; double get bedtimeMg; bool get safeForSleep;
/// Create a copy of CaffeineDailyStat
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineDailyStatCopyWith<CaffeineDailyStat> get copyWith => _$CaffeineDailyStatCopyWithImpl<CaffeineDailyStat>(this as CaffeineDailyStat, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineDailyStat&&(identical(other.date, date) || other.date == date)&&(identical(other.totalMg, totalMg) || other.totalMg == totalMg)&&(identical(other.bedtimeMg, bedtimeMg) || other.bedtimeMg == bedtimeMg)&&(identical(other.safeForSleep, safeForSleep) || other.safeForSleep == safeForSleep));
}


@override
int get hashCode => Object.hash(runtimeType,date,totalMg,bedtimeMg,safeForSleep);

@override
String toString() {
  return 'CaffeineDailyStat(date: $date, totalMg: $totalMg, bedtimeMg: $bedtimeMg, safeForSleep: $safeForSleep)';
}


}

/// @nodoc
abstract mixin class $CaffeineDailyStatCopyWith<$Res>  {
  factory $CaffeineDailyStatCopyWith(CaffeineDailyStat value, $Res Function(CaffeineDailyStat) _then) = _$CaffeineDailyStatCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double totalMg, double bedtimeMg, bool safeForSleep
});




}
/// @nodoc
class _$CaffeineDailyStatCopyWithImpl<$Res>
    implements $CaffeineDailyStatCopyWith<$Res> {
  _$CaffeineDailyStatCopyWithImpl(this._self, this._then);

  final CaffeineDailyStat _self;
  final $Res Function(CaffeineDailyStat) _then;

/// Create a copy of CaffeineDailyStat
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? totalMg = null,Object? bedtimeMg = null,Object? safeForSleep = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,totalMg: null == totalMg ? _self.totalMg : totalMg // ignore: cast_nullable_to_non_nullable
as double,bedtimeMg: null == bedtimeMg ? _self.bedtimeMg : bedtimeMg // ignore: cast_nullable_to_non_nullable
as double,safeForSleep: null == safeForSleep ? _self.safeForSleep : safeForSleep // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineDailyStat].
extension CaffeineDailyStatPatterns on CaffeineDailyStat {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineDailyStat value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineDailyStat() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineDailyStat value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineDailyStat():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineDailyStat value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineDailyStat() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  double totalMg,  double bedtimeMg,  bool safeForSleep)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineDailyStat() when $default != null:
return $default(_that.date,_that.totalMg,_that.bedtimeMg,_that.safeForSleep);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  double totalMg,  double bedtimeMg,  bool safeForSleep)  $default,) {final _that = this;
switch (_that) {
case _CaffeineDailyStat():
return $default(_that.date,_that.totalMg,_that.bedtimeMg,_that.safeForSleep);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  double totalMg,  double bedtimeMg,  bool safeForSleep)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineDailyStat() when $default != null:
return $default(_that.date,_that.totalMg,_that.bedtimeMg,_that.safeForSleep);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineDailyStat implements CaffeineDailyStat {
  const _CaffeineDailyStat({required this.date, required this.totalMg, required this.bedtimeMg, required this.safeForSleep});
  

@override final  LocalDate date;
@override final  double totalMg;
@override final  double bedtimeMg;
@override final  bool safeForSleep;

/// Create a copy of CaffeineDailyStat
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineDailyStatCopyWith<_CaffeineDailyStat> get copyWith => __$CaffeineDailyStatCopyWithImpl<_CaffeineDailyStat>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineDailyStat&&(identical(other.date, date) || other.date == date)&&(identical(other.totalMg, totalMg) || other.totalMg == totalMg)&&(identical(other.bedtimeMg, bedtimeMg) || other.bedtimeMg == bedtimeMg)&&(identical(other.safeForSleep, safeForSleep) || other.safeForSleep == safeForSleep));
}


@override
int get hashCode => Object.hash(runtimeType,date,totalMg,bedtimeMg,safeForSleep);

@override
String toString() {
  return 'CaffeineDailyStat(date: $date, totalMg: $totalMg, bedtimeMg: $bedtimeMg, safeForSleep: $safeForSleep)';
}


}

/// @nodoc
abstract mixin class _$CaffeineDailyStatCopyWith<$Res> implements $CaffeineDailyStatCopyWith<$Res> {
  factory _$CaffeineDailyStatCopyWith(_CaffeineDailyStat value, $Res Function(_CaffeineDailyStat) _then) = __$CaffeineDailyStatCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double totalMg, double bedtimeMg, bool safeForSleep
});




}
/// @nodoc
class __$CaffeineDailyStatCopyWithImpl<$Res>
    implements _$CaffeineDailyStatCopyWith<$Res> {
  __$CaffeineDailyStatCopyWithImpl(this._self, this._then);

  final _CaffeineDailyStat _self;
  final $Res Function(_CaffeineDailyStat) _then;

/// Create a copy of CaffeineDailyStat
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? totalMg = null,Object? bedtimeMg = null,Object? safeForSleep = null,}) {
  return _then(_CaffeineDailyStat(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,totalMg: null == totalMg ? _self.totalMg : totalMg // ignore: cast_nullable_to_non_nullable
as double,bedtimeMg: null == bedtimeMg ? _self.bedtimeMg : bedtimeMg // ignore: cast_nullable_to_non_nullable
as double,safeForSleep: null == safeForSleep ? _self.safeForSleep : safeForSleep // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$CaffeineDistributionSlice {

 String get label; double get valueMg;
/// Create a copy of CaffeineDistributionSlice
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineDistributionSliceCopyWith<CaffeineDistributionSlice> get copyWith => _$CaffeineDistributionSliceCopyWithImpl<CaffeineDistributionSlice>(this as CaffeineDistributionSlice, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineDistributionSlice&&(identical(other.label, label) || other.label == label)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,label,valueMg);

@override
String toString() {
  return 'CaffeineDistributionSlice(label: $label, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class $CaffeineDistributionSliceCopyWith<$Res>  {
  factory $CaffeineDistributionSliceCopyWith(CaffeineDistributionSlice value, $Res Function(CaffeineDistributionSlice) _then) = _$CaffeineDistributionSliceCopyWithImpl;
@useResult
$Res call({
 String label, double valueMg
});




}
/// @nodoc
class _$CaffeineDistributionSliceCopyWithImpl<$Res>
    implements $CaffeineDistributionSliceCopyWith<$Res> {
  _$CaffeineDistributionSliceCopyWithImpl(this._self, this._then);

  final CaffeineDistributionSlice _self;
  final $Res Function(CaffeineDistributionSlice) _then;

/// Create a copy of CaffeineDistributionSlice
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? label = null,Object? valueMg = null,}) {
  return _then(_self.copyWith(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineDistributionSlice].
extension CaffeineDistributionSlicePatterns on CaffeineDistributionSlice {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineDistributionSlice value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineDistributionSlice() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineDistributionSlice value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineDistributionSlice():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineDistributionSlice value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineDistributionSlice() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String label,  double valueMg)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineDistributionSlice() when $default != null:
return $default(_that.label,_that.valueMg);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String label,  double valueMg)  $default,) {final _that = this;
switch (_that) {
case _CaffeineDistributionSlice():
return $default(_that.label,_that.valueMg);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String label,  double valueMg)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineDistributionSlice() when $default != null:
return $default(_that.label,_that.valueMg);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineDistributionSlice implements CaffeineDistributionSlice {
  const _CaffeineDistributionSlice({required this.label, required this.valueMg});
  

@override final  String label;
@override final  double valueMg;

/// Create a copy of CaffeineDistributionSlice
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineDistributionSliceCopyWith<_CaffeineDistributionSlice> get copyWith => __$CaffeineDistributionSliceCopyWithImpl<_CaffeineDistributionSlice>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineDistributionSlice&&(identical(other.label, label) || other.label == label)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,label,valueMg);

@override
String toString() {
  return 'CaffeineDistributionSlice(label: $label, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class _$CaffeineDistributionSliceCopyWith<$Res> implements $CaffeineDistributionSliceCopyWith<$Res> {
  factory _$CaffeineDistributionSliceCopyWith(_CaffeineDistributionSlice value, $Res Function(_CaffeineDistributionSlice) _then) = __$CaffeineDistributionSliceCopyWithImpl;
@override @useResult
$Res call({
 String label, double valueMg
});




}
/// @nodoc
class __$CaffeineDistributionSliceCopyWithImpl<$Res>
    implements _$CaffeineDistributionSliceCopyWith<$Res> {
  __$CaffeineDistributionSliceCopyWithImpl(this._self, this._then);

  final _CaffeineDistributionSlice _self;
  final $Res Function(_CaffeineDistributionSlice) _then;

/// Create a copy of CaffeineDistributionSlice
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? label = null,Object? valueMg = null,}) {
  return _then(_CaffeineDistributionSlice(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CaffeineTimeBucket {

 CaffeineTimeOfDayBucket get bucket; double get valueMg;
/// Create a copy of CaffeineTimeBucket
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineTimeBucketCopyWith<CaffeineTimeBucket> get copyWith => _$CaffeineTimeBucketCopyWithImpl<CaffeineTimeBucket>(this as CaffeineTimeBucket, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineTimeBucket&&(identical(other.bucket, bucket) || other.bucket == bucket)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,bucket,valueMg);

@override
String toString() {
  return 'CaffeineTimeBucket(bucket: $bucket, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class $CaffeineTimeBucketCopyWith<$Res>  {
  factory $CaffeineTimeBucketCopyWith(CaffeineTimeBucket value, $Res Function(CaffeineTimeBucket) _then) = _$CaffeineTimeBucketCopyWithImpl;
@useResult
$Res call({
 CaffeineTimeOfDayBucket bucket, double valueMg
});




}
/// @nodoc
class _$CaffeineTimeBucketCopyWithImpl<$Res>
    implements $CaffeineTimeBucketCopyWith<$Res> {
  _$CaffeineTimeBucketCopyWithImpl(this._self, this._then);

  final CaffeineTimeBucket _self;
  final $Res Function(CaffeineTimeBucket) _then;

/// Create a copy of CaffeineTimeBucket
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? bucket = null,Object? valueMg = null,}) {
  return _then(_self.copyWith(
bucket: null == bucket ? _self.bucket : bucket // ignore: cast_nullable_to_non_nullable
as CaffeineTimeOfDayBucket,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineTimeBucket].
extension CaffeineTimeBucketPatterns on CaffeineTimeBucket {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineTimeBucket value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineTimeBucket() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineTimeBucket value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineTimeBucket():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineTimeBucket value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineTimeBucket() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineTimeOfDayBucket bucket,  double valueMg)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineTimeBucket() when $default != null:
return $default(_that.bucket,_that.valueMg);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineTimeOfDayBucket bucket,  double valueMg)  $default,) {final _that = this;
switch (_that) {
case _CaffeineTimeBucket():
return $default(_that.bucket,_that.valueMg);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineTimeOfDayBucket bucket,  double valueMg)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineTimeBucket() when $default != null:
return $default(_that.bucket,_that.valueMg);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineTimeBucket implements CaffeineTimeBucket {
  const _CaffeineTimeBucket({required this.bucket, required this.valueMg});
  

@override final  CaffeineTimeOfDayBucket bucket;
@override final  double valueMg;

/// Create a copy of CaffeineTimeBucket
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineTimeBucketCopyWith<_CaffeineTimeBucket> get copyWith => __$CaffeineTimeBucketCopyWithImpl<_CaffeineTimeBucket>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineTimeBucket&&(identical(other.bucket, bucket) || other.bucket == bucket)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg));
}


@override
int get hashCode => Object.hash(runtimeType,bucket,valueMg);

@override
String toString() {
  return 'CaffeineTimeBucket(bucket: $bucket, valueMg: $valueMg)';
}


}

/// @nodoc
abstract mixin class _$CaffeineTimeBucketCopyWith<$Res> implements $CaffeineTimeBucketCopyWith<$Res> {
  factory _$CaffeineTimeBucketCopyWith(_CaffeineTimeBucket value, $Res Function(_CaffeineTimeBucket) _then) = __$CaffeineTimeBucketCopyWithImpl;
@override @useResult
$Res call({
 CaffeineTimeOfDayBucket bucket, double valueMg
});




}
/// @nodoc
class __$CaffeineTimeBucketCopyWithImpl<$Res>
    implements _$CaffeineTimeBucketCopyWith<$Res> {
  __$CaffeineTimeBucketCopyWithImpl(this._self, this._then);

  final _CaffeineTimeBucket _self;
  final $Res Function(_CaffeineTimeBucket) _then;

/// Create a copy of CaffeineTimeBucket
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? bucket = null,Object? valueMg = null,}) {
  return _then(_CaffeineTimeBucket(
bucket: null == bucket ? _self.bucket : bucket // ignore: cast_nullable_to_non_nullable
as CaffeineTimeOfDayBucket,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CaffeineInsights {

 double get currentMg; double get todayTotalMg; double get periodTotalMg; double get periodAverageMg; int get loggedDays; CaffeineDailyStat? get peakDay; int get safeNights; int get totalNights; int get safeSleepStreak; double get bedtimeMg; int get sleepThresholdMg; LocalTime get bedtime; int? get timeToThresholdMinutes; List<CaffeinePoint> get curvePoints; List<CaffeineDailyStat> get dailyStats; List<CaffeineEntryInsight> get entryInsights; List<CaffeineDistributionSlice> get sourceTotals; List<CaffeineDistributionSlice> get itemTotals; List<CaffeineDistributionSlice> get categoryTotals; List<CaffeineTimeBucket> get timeBuckets;
/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<CaffeineInsights> get copyWith => _$CaffeineInsightsCopyWithImpl<CaffeineInsights>(this as CaffeineInsights, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineInsights&&(identical(other.currentMg, currentMg) || other.currentMg == currentMg)&&(identical(other.todayTotalMg, todayTotalMg) || other.todayTotalMg == todayTotalMg)&&(identical(other.periodTotalMg, periodTotalMg) || other.periodTotalMg == periodTotalMg)&&(identical(other.periodAverageMg, periodAverageMg) || other.periodAverageMg == periodAverageMg)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.peakDay, peakDay) || other.peakDay == peakDay)&&(identical(other.safeNights, safeNights) || other.safeNights == safeNights)&&(identical(other.totalNights, totalNights) || other.totalNights == totalNights)&&(identical(other.safeSleepStreak, safeSleepStreak) || other.safeSleepStreak == safeSleepStreak)&&(identical(other.bedtimeMg, bedtimeMg) || other.bedtimeMg == bedtimeMg)&&(identical(other.sleepThresholdMg, sleepThresholdMg) || other.sleepThresholdMg == sleepThresholdMg)&&(identical(other.bedtime, bedtime) || other.bedtime == bedtime)&&(identical(other.timeToThresholdMinutes, timeToThresholdMinutes) || other.timeToThresholdMinutes == timeToThresholdMinutes)&&const DeepCollectionEquality().equals(other.curvePoints, curvePoints)&&const DeepCollectionEquality().equals(other.dailyStats, dailyStats)&&const DeepCollectionEquality().equals(other.entryInsights, entryInsights)&&const DeepCollectionEquality().equals(other.sourceTotals, sourceTotals)&&const DeepCollectionEquality().equals(other.itemTotals, itemTotals)&&const DeepCollectionEquality().equals(other.categoryTotals, categoryTotals)&&const DeepCollectionEquality().equals(other.timeBuckets, timeBuckets));
}


@override
int get hashCode => Object.hashAll([runtimeType,currentMg,todayTotalMg,periodTotalMg,periodAverageMg,loggedDays,peakDay,safeNights,totalNights,safeSleepStreak,bedtimeMg,sleepThresholdMg,bedtime,timeToThresholdMinutes,const DeepCollectionEquality().hash(curvePoints),const DeepCollectionEquality().hash(dailyStats),const DeepCollectionEquality().hash(entryInsights),const DeepCollectionEquality().hash(sourceTotals),const DeepCollectionEquality().hash(itemTotals),const DeepCollectionEquality().hash(categoryTotals),const DeepCollectionEquality().hash(timeBuckets)]);

@override
String toString() {
  return 'CaffeineInsights(currentMg: $currentMg, todayTotalMg: $todayTotalMg, periodTotalMg: $periodTotalMg, periodAverageMg: $periodAverageMg, loggedDays: $loggedDays, peakDay: $peakDay, safeNights: $safeNights, totalNights: $totalNights, safeSleepStreak: $safeSleepStreak, bedtimeMg: $bedtimeMg, sleepThresholdMg: $sleepThresholdMg, bedtime: $bedtime, timeToThresholdMinutes: $timeToThresholdMinutes, curvePoints: $curvePoints, dailyStats: $dailyStats, entryInsights: $entryInsights, sourceTotals: $sourceTotals, itemTotals: $itemTotals, categoryTotals: $categoryTotals, timeBuckets: $timeBuckets)';
}


}

/// @nodoc
abstract mixin class $CaffeineInsightsCopyWith<$Res>  {
  factory $CaffeineInsightsCopyWith(CaffeineInsights value, $Res Function(CaffeineInsights) _then) = _$CaffeineInsightsCopyWithImpl;
@useResult
$Res call({
 double currentMg, double todayTotalMg, double periodTotalMg, double periodAverageMg, int loggedDays, CaffeineDailyStat? peakDay, int safeNights, int totalNights, int safeSleepStreak, double bedtimeMg, int sleepThresholdMg, LocalTime bedtime, int? timeToThresholdMinutes, List<CaffeinePoint> curvePoints, List<CaffeineDailyStat> dailyStats, List<CaffeineEntryInsight> entryInsights, List<CaffeineDistributionSlice> sourceTotals, List<CaffeineDistributionSlice> itemTotals, List<CaffeineDistributionSlice> categoryTotals, List<CaffeineTimeBucket> timeBuckets
});


$CaffeineDailyStatCopyWith<$Res>? get peakDay;

}
/// @nodoc
class _$CaffeineInsightsCopyWithImpl<$Res>
    implements $CaffeineInsightsCopyWith<$Res> {
  _$CaffeineInsightsCopyWithImpl(this._self, this._then);

  final CaffeineInsights _self;
  final $Res Function(CaffeineInsights) _then;

/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? currentMg = null,Object? todayTotalMg = null,Object? periodTotalMg = null,Object? periodAverageMg = null,Object? loggedDays = null,Object? peakDay = freezed,Object? safeNights = null,Object? totalNights = null,Object? safeSleepStreak = null,Object? bedtimeMg = null,Object? sleepThresholdMg = null,Object? bedtime = null,Object? timeToThresholdMinutes = freezed,Object? curvePoints = null,Object? dailyStats = null,Object? entryInsights = null,Object? sourceTotals = null,Object? itemTotals = null,Object? categoryTotals = null,Object? timeBuckets = null,}) {
  return _then(_self.copyWith(
currentMg: null == currentMg ? _self.currentMg : currentMg // ignore: cast_nullable_to_non_nullable
as double,todayTotalMg: null == todayTotalMg ? _self.todayTotalMg : todayTotalMg // ignore: cast_nullable_to_non_nullable
as double,periodTotalMg: null == periodTotalMg ? _self.periodTotalMg : periodTotalMg // ignore: cast_nullable_to_non_nullable
as double,periodAverageMg: null == periodAverageMg ? _self.periodAverageMg : periodAverageMg // ignore: cast_nullable_to_non_nullable
as double,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,peakDay: freezed == peakDay ? _self.peakDay : peakDay // ignore: cast_nullable_to_non_nullable
as CaffeineDailyStat?,safeNights: null == safeNights ? _self.safeNights : safeNights // ignore: cast_nullable_to_non_nullable
as int,totalNights: null == totalNights ? _self.totalNights : totalNights // ignore: cast_nullable_to_non_nullable
as int,safeSleepStreak: null == safeSleepStreak ? _self.safeSleepStreak : safeSleepStreak // ignore: cast_nullable_to_non_nullable
as int,bedtimeMg: null == bedtimeMg ? _self.bedtimeMg : bedtimeMg // ignore: cast_nullable_to_non_nullable
as double,sleepThresholdMg: null == sleepThresholdMg ? _self.sleepThresholdMg : sleepThresholdMg // ignore: cast_nullable_to_non_nullable
as int,bedtime: null == bedtime ? _self.bedtime : bedtime // ignore: cast_nullable_to_non_nullable
as LocalTime,timeToThresholdMinutes: freezed == timeToThresholdMinutes ? _self.timeToThresholdMinutes : timeToThresholdMinutes // ignore: cast_nullable_to_non_nullable
as int?,curvePoints: null == curvePoints ? _self.curvePoints : curvePoints // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,dailyStats: null == dailyStats ? _self.dailyStats : dailyStats // ignore: cast_nullable_to_non_nullable
as List<CaffeineDailyStat>,entryInsights: null == entryInsights ? _self.entryInsights : entryInsights // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntryInsight>,sourceTotals: null == sourceTotals ? _self.sourceTotals : sourceTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,itemTotals: null == itemTotals ? _self.itemTotals : itemTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,categoryTotals: null == categoryTotals ? _self.categoryTotals : categoryTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,timeBuckets: null == timeBuckets ? _self.timeBuckets : timeBuckets // ignore: cast_nullable_to_non_nullable
as List<CaffeineTimeBucket>,
  ));
}
/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineDailyStatCopyWith<$Res>? get peakDay {
    if (_self.peakDay == null) {
    return null;
  }

  return $CaffeineDailyStatCopyWith<$Res>(_self.peakDay!, (value) {
    return _then(_self.copyWith(peakDay: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineInsights].
extension CaffeineInsightsPatterns on CaffeineInsights {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineInsights value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineInsights() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineInsights value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineInsights():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineInsights value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineInsights() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double currentMg,  double todayTotalMg,  double periodTotalMg,  double periodAverageMg,  int loggedDays,  CaffeineDailyStat? peakDay,  int safeNights,  int totalNights,  int safeSleepStreak,  double bedtimeMg,  int sleepThresholdMg,  LocalTime bedtime,  int? timeToThresholdMinutes,  List<CaffeinePoint> curvePoints,  List<CaffeineDailyStat> dailyStats,  List<CaffeineEntryInsight> entryInsights,  List<CaffeineDistributionSlice> sourceTotals,  List<CaffeineDistributionSlice> itemTotals,  List<CaffeineDistributionSlice> categoryTotals,  List<CaffeineTimeBucket> timeBuckets)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineInsights() when $default != null:
return $default(_that.currentMg,_that.todayTotalMg,_that.periodTotalMg,_that.periodAverageMg,_that.loggedDays,_that.peakDay,_that.safeNights,_that.totalNights,_that.safeSleepStreak,_that.bedtimeMg,_that.sleepThresholdMg,_that.bedtime,_that.timeToThresholdMinutes,_that.curvePoints,_that.dailyStats,_that.entryInsights,_that.sourceTotals,_that.itemTotals,_that.categoryTotals,_that.timeBuckets);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double currentMg,  double todayTotalMg,  double periodTotalMg,  double periodAverageMg,  int loggedDays,  CaffeineDailyStat? peakDay,  int safeNights,  int totalNights,  int safeSleepStreak,  double bedtimeMg,  int sleepThresholdMg,  LocalTime bedtime,  int? timeToThresholdMinutes,  List<CaffeinePoint> curvePoints,  List<CaffeineDailyStat> dailyStats,  List<CaffeineEntryInsight> entryInsights,  List<CaffeineDistributionSlice> sourceTotals,  List<CaffeineDistributionSlice> itemTotals,  List<CaffeineDistributionSlice> categoryTotals,  List<CaffeineTimeBucket> timeBuckets)  $default,) {final _that = this;
switch (_that) {
case _CaffeineInsights():
return $default(_that.currentMg,_that.todayTotalMg,_that.periodTotalMg,_that.periodAverageMg,_that.loggedDays,_that.peakDay,_that.safeNights,_that.totalNights,_that.safeSleepStreak,_that.bedtimeMg,_that.sleepThresholdMg,_that.bedtime,_that.timeToThresholdMinutes,_that.curvePoints,_that.dailyStats,_that.entryInsights,_that.sourceTotals,_that.itemTotals,_that.categoryTotals,_that.timeBuckets);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double currentMg,  double todayTotalMg,  double periodTotalMg,  double periodAverageMg,  int loggedDays,  CaffeineDailyStat? peakDay,  int safeNights,  int totalNights,  int safeSleepStreak,  double bedtimeMg,  int sleepThresholdMg,  LocalTime bedtime,  int? timeToThresholdMinutes,  List<CaffeinePoint> curvePoints,  List<CaffeineDailyStat> dailyStats,  List<CaffeineEntryInsight> entryInsights,  List<CaffeineDistributionSlice> sourceTotals,  List<CaffeineDistributionSlice> itemTotals,  List<CaffeineDistributionSlice> categoryTotals,  List<CaffeineTimeBucket> timeBuckets)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineInsights() when $default != null:
return $default(_that.currentMg,_that.todayTotalMg,_that.periodTotalMg,_that.periodAverageMg,_that.loggedDays,_that.peakDay,_that.safeNights,_that.totalNights,_that.safeSleepStreak,_that.bedtimeMg,_that.sleepThresholdMg,_that.bedtime,_that.timeToThresholdMinutes,_that.curvePoints,_that.dailyStats,_that.entryInsights,_that.sourceTotals,_that.itemTotals,_that.categoryTotals,_that.timeBuckets);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineInsights implements CaffeineInsights {
  const _CaffeineInsights({this.currentMg = 0.0, this.todayTotalMg = 0.0, this.periodTotalMg = 0.0, this.periodAverageMg = 0.0, this.loggedDays = 0, this.peakDay, this.safeNights = 0, this.totalNights = 0, this.safeSleepStreak = 0, this.bedtimeMg = 0.0, this.sleepThresholdMg = 0, this.bedtime = const LocalTime(0, 0), this.timeToThresholdMinutes, final  List<CaffeinePoint> curvePoints = const <CaffeinePoint>[], final  List<CaffeineDailyStat> dailyStats = const <CaffeineDailyStat>[], final  List<CaffeineEntryInsight> entryInsights = const <CaffeineEntryInsight>[], final  List<CaffeineDistributionSlice> sourceTotals = const <CaffeineDistributionSlice>[], final  List<CaffeineDistributionSlice> itemTotals = const <CaffeineDistributionSlice>[], final  List<CaffeineDistributionSlice> categoryTotals = const <CaffeineDistributionSlice>[], final  List<CaffeineTimeBucket> timeBuckets = const <CaffeineTimeBucket>[]}): _curvePoints = curvePoints,_dailyStats = dailyStats,_entryInsights = entryInsights,_sourceTotals = sourceTotals,_itemTotals = itemTotals,_categoryTotals = categoryTotals,_timeBuckets = timeBuckets;
  

@override@JsonKey() final  double currentMg;
@override@JsonKey() final  double todayTotalMg;
@override@JsonKey() final  double periodTotalMg;
@override@JsonKey() final  double periodAverageMg;
@override@JsonKey() final  int loggedDays;
@override final  CaffeineDailyStat? peakDay;
@override@JsonKey() final  int safeNights;
@override@JsonKey() final  int totalNights;
@override@JsonKey() final  int safeSleepStreak;
@override@JsonKey() final  double bedtimeMg;
@override@JsonKey() final  int sleepThresholdMg;
@override@JsonKey() final  LocalTime bedtime;
@override final  int? timeToThresholdMinutes;
 final  List<CaffeinePoint> _curvePoints;
@override@JsonKey() List<CaffeinePoint> get curvePoints {
  if (_curvePoints is EqualUnmodifiableListView) return _curvePoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_curvePoints);
}

 final  List<CaffeineDailyStat> _dailyStats;
@override@JsonKey() List<CaffeineDailyStat> get dailyStats {
  if (_dailyStats is EqualUnmodifiableListView) return _dailyStats;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyStats);
}

 final  List<CaffeineEntryInsight> _entryInsights;
@override@JsonKey() List<CaffeineEntryInsight> get entryInsights {
  if (_entryInsights is EqualUnmodifiableListView) return _entryInsights;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entryInsights);
}

 final  List<CaffeineDistributionSlice> _sourceTotals;
@override@JsonKey() List<CaffeineDistributionSlice> get sourceTotals {
  if (_sourceTotals is EqualUnmodifiableListView) return _sourceTotals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sourceTotals);
}

 final  List<CaffeineDistributionSlice> _itemTotals;
@override@JsonKey() List<CaffeineDistributionSlice> get itemTotals {
  if (_itemTotals is EqualUnmodifiableListView) return _itemTotals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_itemTotals);
}

 final  List<CaffeineDistributionSlice> _categoryTotals;
@override@JsonKey() List<CaffeineDistributionSlice> get categoryTotals {
  if (_categoryTotals is EqualUnmodifiableListView) return _categoryTotals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_categoryTotals);
}

 final  List<CaffeineTimeBucket> _timeBuckets;
@override@JsonKey() List<CaffeineTimeBucket> get timeBuckets {
  if (_timeBuckets is EqualUnmodifiableListView) return _timeBuckets;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_timeBuckets);
}


/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineInsightsCopyWith<_CaffeineInsights> get copyWith => __$CaffeineInsightsCopyWithImpl<_CaffeineInsights>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineInsights&&(identical(other.currentMg, currentMg) || other.currentMg == currentMg)&&(identical(other.todayTotalMg, todayTotalMg) || other.todayTotalMg == todayTotalMg)&&(identical(other.periodTotalMg, periodTotalMg) || other.periodTotalMg == periodTotalMg)&&(identical(other.periodAverageMg, periodAverageMg) || other.periodAverageMg == periodAverageMg)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.peakDay, peakDay) || other.peakDay == peakDay)&&(identical(other.safeNights, safeNights) || other.safeNights == safeNights)&&(identical(other.totalNights, totalNights) || other.totalNights == totalNights)&&(identical(other.safeSleepStreak, safeSleepStreak) || other.safeSleepStreak == safeSleepStreak)&&(identical(other.bedtimeMg, bedtimeMg) || other.bedtimeMg == bedtimeMg)&&(identical(other.sleepThresholdMg, sleepThresholdMg) || other.sleepThresholdMg == sleepThresholdMg)&&(identical(other.bedtime, bedtime) || other.bedtime == bedtime)&&(identical(other.timeToThresholdMinutes, timeToThresholdMinutes) || other.timeToThresholdMinutes == timeToThresholdMinutes)&&const DeepCollectionEquality().equals(other._curvePoints, _curvePoints)&&const DeepCollectionEquality().equals(other._dailyStats, _dailyStats)&&const DeepCollectionEquality().equals(other._entryInsights, _entryInsights)&&const DeepCollectionEquality().equals(other._sourceTotals, _sourceTotals)&&const DeepCollectionEquality().equals(other._itemTotals, _itemTotals)&&const DeepCollectionEquality().equals(other._categoryTotals, _categoryTotals)&&const DeepCollectionEquality().equals(other._timeBuckets, _timeBuckets));
}


@override
int get hashCode => Object.hashAll([runtimeType,currentMg,todayTotalMg,periodTotalMg,periodAverageMg,loggedDays,peakDay,safeNights,totalNights,safeSleepStreak,bedtimeMg,sleepThresholdMg,bedtime,timeToThresholdMinutes,const DeepCollectionEquality().hash(_curvePoints),const DeepCollectionEquality().hash(_dailyStats),const DeepCollectionEquality().hash(_entryInsights),const DeepCollectionEquality().hash(_sourceTotals),const DeepCollectionEquality().hash(_itemTotals),const DeepCollectionEquality().hash(_categoryTotals),const DeepCollectionEquality().hash(_timeBuckets)]);

@override
String toString() {
  return 'CaffeineInsights(currentMg: $currentMg, todayTotalMg: $todayTotalMg, periodTotalMg: $periodTotalMg, periodAverageMg: $periodAverageMg, loggedDays: $loggedDays, peakDay: $peakDay, safeNights: $safeNights, totalNights: $totalNights, safeSleepStreak: $safeSleepStreak, bedtimeMg: $bedtimeMg, sleepThresholdMg: $sleepThresholdMg, bedtime: $bedtime, timeToThresholdMinutes: $timeToThresholdMinutes, curvePoints: $curvePoints, dailyStats: $dailyStats, entryInsights: $entryInsights, sourceTotals: $sourceTotals, itemTotals: $itemTotals, categoryTotals: $categoryTotals, timeBuckets: $timeBuckets)';
}


}

/// @nodoc
abstract mixin class _$CaffeineInsightsCopyWith<$Res> implements $CaffeineInsightsCopyWith<$Res> {
  factory _$CaffeineInsightsCopyWith(_CaffeineInsights value, $Res Function(_CaffeineInsights) _then) = __$CaffeineInsightsCopyWithImpl;
@override @useResult
$Res call({
 double currentMg, double todayTotalMg, double periodTotalMg, double periodAverageMg, int loggedDays, CaffeineDailyStat? peakDay, int safeNights, int totalNights, int safeSleepStreak, double bedtimeMg, int sleepThresholdMg, LocalTime bedtime, int? timeToThresholdMinutes, List<CaffeinePoint> curvePoints, List<CaffeineDailyStat> dailyStats, List<CaffeineEntryInsight> entryInsights, List<CaffeineDistributionSlice> sourceTotals, List<CaffeineDistributionSlice> itemTotals, List<CaffeineDistributionSlice> categoryTotals, List<CaffeineTimeBucket> timeBuckets
});


@override $CaffeineDailyStatCopyWith<$Res>? get peakDay;

}
/// @nodoc
class __$CaffeineInsightsCopyWithImpl<$Res>
    implements _$CaffeineInsightsCopyWith<$Res> {
  __$CaffeineInsightsCopyWithImpl(this._self, this._then);

  final _CaffeineInsights _self;
  final $Res Function(_CaffeineInsights) _then;

/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? currentMg = null,Object? todayTotalMg = null,Object? periodTotalMg = null,Object? periodAverageMg = null,Object? loggedDays = null,Object? peakDay = freezed,Object? safeNights = null,Object? totalNights = null,Object? safeSleepStreak = null,Object? bedtimeMg = null,Object? sleepThresholdMg = null,Object? bedtime = null,Object? timeToThresholdMinutes = freezed,Object? curvePoints = null,Object? dailyStats = null,Object? entryInsights = null,Object? sourceTotals = null,Object? itemTotals = null,Object? categoryTotals = null,Object? timeBuckets = null,}) {
  return _then(_CaffeineInsights(
currentMg: null == currentMg ? _self.currentMg : currentMg // ignore: cast_nullable_to_non_nullable
as double,todayTotalMg: null == todayTotalMg ? _self.todayTotalMg : todayTotalMg // ignore: cast_nullable_to_non_nullable
as double,periodTotalMg: null == periodTotalMg ? _self.periodTotalMg : periodTotalMg // ignore: cast_nullable_to_non_nullable
as double,periodAverageMg: null == periodAverageMg ? _self.periodAverageMg : periodAverageMg // ignore: cast_nullable_to_non_nullable
as double,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,peakDay: freezed == peakDay ? _self.peakDay : peakDay // ignore: cast_nullable_to_non_nullable
as CaffeineDailyStat?,safeNights: null == safeNights ? _self.safeNights : safeNights // ignore: cast_nullable_to_non_nullable
as int,totalNights: null == totalNights ? _self.totalNights : totalNights // ignore: cast_nullable_to_non_nullable
as int,safeSleepStreak: null == safeSleepStreak ? _self.safeSleepStreak : safeSleepStreak // ignore: cast_nullable_to_non_nullable
as int,bedtimeMg: null == bedtimeMg ? _self.bedtimeMg : bedtimeMg // ignore: cast_nullable_to_non_nullable
as double,sleepThresholdMg: null == sleepThresholdMg ? _self.sleepThresholdMg : sleepThresholdMg // ignore: cast_nullable_to_non_nullable
as int,bedtime: null == bedtime ? _self.bedtime : bedtime // ignore: cast_nullable_to_non_nullable
as LocalTime,timeToThresholdMinutes: freezed == timeToThresholdMinutes ? _self.timeToThresholdMinutes : timeToThresholdMinutes // ignore: cast_nullable_to_non_nullable
as int?,curvePoints: null == curvePoints ? _self._curvePoints : curvePoints // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,dailyStats: null == dailyStats ? _self._dailyStats : dailyStats // ignore: cast_nullable_to_non_nullable
as List<CaffeineDailyStat>,entryInsights: null == entryInsights ? _self._entryInsights : entryInsights // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntryInsight>,sourceTotals: null == sourceTotals ? _self._sourceTotals : sourceTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,itemTotals: null == itemTotals ? _self._itemTotals : itemTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,categoryTotals: null == categoryTotals ? _self._categoryTotals : categoryTotals // ignore: cast_nullable_to_non_nullable
as List<CaffeineDistributionSlice>,timeBuckets: null == timeBuckets ? _self._timeBuckets : timeBuckets // ignore: cast_nullable_to_non_nullable
as List<CaffeineTimeBucket>,
  ));
}

/// Create a copy of CaffeineInsights
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineDailyStatCopyWith<$Res>? get peakDay {
    if (_self.peakDay == null) {
    return null;
  }

  return $CaffeineDailyStatCopyWith<$Res>(_self.peakDay!, (value) {
    return _then(_self.copyWith(peakDay: value));
  });
}
}

/// @nodoc
mixin _$CaffeineCatalogItem {

 String get id; String get name; CaffeineSourceCategory get category; double get typicalCaffeineMg; double? get defaultServingMilliliters; List<String> get aliases;
/// Create a copy of CaffeineCatalogItem
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineCatalogItemCopyWith<CaffeineCatalogItem> get copyWith => _$CaffeineCatalogItemCopyWithImpl<CaffeineCatalogItem>(this as CaffeineCatalogItem, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineCatalogItem&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.category, category) || other.category == category)&&(identical(other.typicalCaffeineMg, typicalCaffeineMg) || other.typicalCaffeineMg == typicalCaffeineMg)&&(identical(other.defaultServingMilliliters, defaultServingMilliliters) || other.defaultServingMilliliters == defaultServingMilliliters)&&const DeepCollectionEquality().equals(other.aliases, aliases));
}


@override
int get hashCode => Object.hash(runtimeType,id,name,category,typicalCaffeineMg,defaultServingMilliliters,const DeepCollectionEquality().hash(aliases));

@override
String toString() {
  return 'CaffeineCatalogItem(id: $id, name: $name, category: $category, typicalCaffeineMg: $typicalCaffeineMg, defaultServingMilliliters: $defaultServingMilliliters, aliases: $aliases)';
}


}

/// @nodoc
abstract mixin class $CaffeineCatalogItemCopyWith<$Res>  {
  factory $CaffeineCatalogItemCopyWith(CaffeineCatalogItem value, $Res Function(CaffeineCatalogItem) _then) = _$CaffeineCatalogItemCopyWithImpl;
@useResult
$Res call({
 String id, String name, CaffeineSourceCategory category, double typicalCaffeineMg, double? defaultServingMilliliters, List<String> aliases
});




}
/// @nodoc
class _$CaffeineCatalogItemCopyWithImpl<$Res>
    implements $CaffeineCatalogItemCopyWith<$Res> {
  _$CaffeineCatalogItemCopyWithImpl(this._self, this._then);

  final CaffeineCatalogItem _self;
  final $Res Function(CaffeineCatalogItem) _then;

/// Create a copy of CaffeineCatalogItem
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? name = null,Object? category = null,Object? typicalCaffeineMg = null,Object? defaultServingMilliliters = freezed,Object? aliases = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory,typicalCaffeineMg: null == typicalCaffeineMg ? _self.typicalCaffeineMg : typicalCaffeineMg // ignore: cast_nullable_to_non_nullable
as double,defaultServingMilliliters: freezed == defaultServingMilliliters ? _self.defaultServingMilliliters : defaultServingMilliliters // ignore: cast_nullable_to_non_nullable
as double?,aliases: null == aliases ? _self.aliases : aliases // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineCatalogItem].
extension CaffeineCatalogItemPatterns on CaffeineCatalogItem {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineCatalogItem value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineCatalogItem() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineCatalogItem value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineCatalogItem():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineCatalogItem value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineCatalogItem() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String name,  CaffeineSourceCategory category,  double typicalCaffeineMg,  double? defaultServingMilliliters,  List<String> aliases)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineCatalogItem() when $default != null:
return $default(_that.id,_that.name,_that.category,_that.typicalCaffeineMg,_that.defaultServingMilliliters,_that.aliases);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String name,  CaffeineSourceCategory category,  double typicalCaffeineMg,  double? defaultServingMilliliters,  List<String> aliases)  $default,) {final _that = this;
switch (_that) {
case _CaffeineCatalogItem():
return $default(_that.id,_that.name,_that.category,_that.typicalCaffeineMg,_that.defaultServingMilliliters,_that.aliases);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String name,  CaffeineSourceCategory category,  double typicalCaffeineMg,  double? defaultServingMilliliters,  List<String> aliases)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineCatalogItem() when $default != null:
return $default(_that.id,_that.name,_that.category,_that.typicalCaffeineMg,_that.defaultServingMilliliters,_that.aliases);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineCatalogItem implements CaffeineCatalogItem {
  const _CaffeineCatalogItem({required this.id, required this.name, required this.category, required this.typicalCaffeineMg, this.defaultServingMilliliters, final  List<String> aliases = const <String>[]}): _aliases = aliases;
  

@override final  String id;
@override final  String name;
@override final  CaffeineSourceCategory category;
@override final  double typicalCaffeineMg;
@override final  double? defaultServingMilliliters;
 final  List<String> _aliases;
@override@JsonKey() List<String> get aliases {
  if (_aliases is EqualUnmodifiableListView) return _aliases;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_aliases);
}


/// Create a copy of CaffeineCatalogItem
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineCatalogItemCopyWith<_CaffeineCatalogItem> get copyWith => __$CaffeineCatalogItemCopyWithImpl<_CaffeineCatalogItem>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineCatalogItem&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.category, category) || other.category == category)&&(identical(other.typicalCaffeineMg, typicalCaffeineMg) || other.typicalCaffeineMg == typicalCaffeineMg)&&(identical(other.defaultServingMilliliters, defaultServingMilliliters) || other.defaultServingMilliliters == defaultServingMilliliters)&&const DeepCollectionEquality().equals(other._aliases, _aliases));
}


@override
int get hashCode => Object.hash(runtimeType,id,name,category,typicalCaffeineMg,defaultServingMilliliters,const DeepCollectionEquality().hash(_aliases));

@override
String toString() {
  return 'CaffeineCatalogItem(id: $id, name: $name, category: $category, typicalCaffeineMg: $typicalCaffeineMg, defaultServingMilliliters: $defaultServingMilliliters, aliases: $aliases)';
}


}

/// @nodoc
abstract mixin class _$CaffeineCatalogItemCopyWith<$Res> implements $CaffeineCatalogItemCopyWith<$Res> {
  factory _$CaffeineCatalogItemCopyWith(_CaffeineCatalogItem value, $Res Function(_CaffeineCatalogItem) _then) = __$CaffeineCatalogItemCopyWithImpl;
@override @useResult
$Res call({
 String id, String name, CaffeineSourceCategory category, double typicalCaffeineMg, double? defaultServingMilliliters, List<String> aliases
});




}
/// @nodoc
class __$CaffeineCatalogItemCopyWithImpl<$Res>
    implements _$CaffeineCatalogItemCopyWith<$Res> {
  __$CaffeineCatalogItemCopyWithImpl(this._self, this._then);

  final _CaffeineCatalogItem _self;
  final $Res Function(_CaffeineCatalogItem) _then;

/// Create a copy of CaffeineCatalogItem
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? name = null,Object? category = null,Object? typicalCaffeineMg = null,Object? defaultServingMilliliters = freezed,Object? aliases = null,}) {
  return _then(_CaffeineCatalogItem(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory,typicalCaffeineMg: null == typicalCaffeineMg ? _self.typicalCaffeineMg : typicalCaffeineMg // ignore: cast_nullable_to_non_nullable
as double,defaultServingMilliliters: freezed == defaultServingMilliliters ? _self.defaultServingMilliliters : defaultServingMilliliters // ignore: cast_nullable_to_non_nullable
as double?,aliases: null == aliases ? _self._aliases : aliases // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}

/// @nodoc
mixin _$CaffeineCatalogMatch {

 CaffeineCatalogItem get item; CaffeineCatalogMatchConfidence get confidence; String get matchedText;
/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineCatalogMatchCopyWith<CaffeineCatalogMatch> get copyWith => _$CaffeineCatalogMatchCopyWithImpl<CaffeineCatalogMatch>(this as CaffeineCatalogMatch, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineCatalogMatch&&(identical(other.item, item) || other.item == item)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.matchedText, matchedText) || other.matchedText == matchedText));
}


@override
int get hashCode => Object.hash(runtimeType,item,confidence,matchedText);

@override
String toString() {
  return 'CaffeineCatalogMatch(item: $item, confidence: $confidence, matchedText: $matchedText)';
}


}

/// @nodoc
abstract mixin class $CaffeineCatalogMatchCopyWith<$Res>  {
  factory $CaffeineCatalogMatchCopyWith(CaffeineCatalogMatch value, $Res Function(CaffeineCatalogMatch) _then) = _$CaffeineCatalogMatchCopyWithImpl;
@useResult
$Res call({
 CaffeineCatalogItem item, CaffeineCatalogMatchConfidence confidence, String matchedText
});


$CaffeineCatalogItemCopyWith<$Res> get item;

}
/// @nodoc
class _$CaffeineCatalogMatchCopyWithImpl<$Res>
    implements $CaffeineCatalogMatchCopyWith<$Res> {
  _$CaffeineCatalogMatchCopyWithImpl(this._self, this._then);

  final CaffeineCatalogMatch _self;
  final $Res Function(CaffeineCatalogMatch) _then;

/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? item = null,Object? confidence = null,Object? matchedText = null,}) {
  return _then(_self.copyWith(
item: null == item ? _self.item : item // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogItem,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogMatchConfidence,matchedText: null == matchedText ? _self.matchedText : matchedText // ignore: cast_nullable_to_non_nullable
as String,
  ));
}
/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineCatalogItemCopyWith<$Res> get item {
  
  return $CaffeineCatalogItemCopyWith<$Res>(_self.item, (value) {
    return _then(_self.copyWith(item: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineCatalogMatch].
extension CaffeineCatalogMatchPatterns on CaffeineCatalogMatch {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineCatalogMatch value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineCatalogMatch() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineCatalogMatch value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineCatalogMatch():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineCatalogMatch value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineCatalogMatch() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineCatalogItem item,  CaffeineCatalogMatchConfidence confidence,  String matchedText)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineCatalogMatch() when $default != null:
return $default(_that.item,_that.confidence,_that.matchedText);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineCatalogItem item,  CaffeineCatalogMatchConfidence confidence,  String matchedText)  $default,) {final _that = this;
switch (_that) {
case _CaffeineCatalogMatch():
return $default(_that.item,_that.confidence,_that.matchedText);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineCatalogItem item,  CaffeineCatalogMatchConfidence confidence,  String matchedText)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineCatalogMatch() when $default != null:
return $default(_that.item,_that.confidence,_that.matchedText);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineCatalogMatch implements CaffeineCatalogMatch {
  const _CaffeineCatalogMatch({required this.item, required this.confidence, required this.matchedText});
  

@override final  CaffeineCatalogItem item;
@override final  CaffeineCatalogMatchConfidence confidence;
@override final  String matchedText;

/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineCatalogMatchCopyWith<_CaffeineCatalogMatch> get copyWith => __$CaffeineCatalogMatchCopyWithImpl<_CaffeineCatalogMatch>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineCatalogMatch&&(identical(other.item, item) || other.item == item)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.matchedText, matchedText) || other.matchedText == matchedText));
}


@override
int get hashCode => Object.hash(runtimeType,item,confidence,matchedText);

@override
String toString() {
  return 'CaffeineCatalogMatch(item: $item, confidence: $confidence, matchedText: $matchedText)';
}


}

/// @nodoc
abstract mixin class _$CaffeineCatalogMatchCopyWith<$Res> implements $CaffeineCatalogMatchCopyWith<$Res> {
  factory _$CaffeineCatalogMatchCopyWith(_CaffeineCatalogMatch value, $Res Function(_CaffeineCatalogMatch) _then) = __$CaffeineCatalogMatchCopyWithImpl;
@override @useResult
$Res call({
 CaffeineCatalogItem item, CaffeineCatalogMatchConfidence confidence, String matchedText
});


@override $CaffeineCatalogItemCopyWith<$Res> get item;

}
/// @nodoc
class __$CaffeineCatalogMatchCopyWithImpl<$Res>
    implements _$CaffeineCatalogMatchCopyWith<$Res> {
  __$CaffeineCatalogMatchCopyWithImpl(this._self, this._then);

  final _CaffeineCatalogMatch _self;
  final $Res Function(_CaffeineCatalogMatch) _then;

/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? item = null,Object? confidence = null,Object? matchedText = null,}) {
  return _then(_CaffeineCatalogMatch(
item: null == item ? _self.item : item // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogItem,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CaffeineCatalogMatchConfidence,matchedText: null == matchedText ? _self.matchedText : matchedText // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

/// Create a copy of CaffeineCatalogMatch
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineCatalogItemCopyWith<$Res> get item {
  
  return $CaffeineCatalogItemCopyWith<$Res>(_self.item, (value) {
    return _then(_self.copyWith(item: value));
  });
}
}

// dart format on
