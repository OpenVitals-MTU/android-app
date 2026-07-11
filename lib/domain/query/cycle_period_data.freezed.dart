// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cycle_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CyclePeriodData {

 CycleData get data; Set<String> get missingPermissions;
/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CyclePeriodDataCopyWith<CyclePeriodData> get copyWith => _$CyclePeriodDataCopyWithImpl<CyclePeriodData>(this as CyclePeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CyclePeriodData&&(identical(other.data, data) || other.data == data)&&const DeepCollectionEquality().equals(other.missingPermissions, missingPermissions));
}


@override
int get hashCode => Object.hash(runtimeType,data,const DeepCollectionEquality().hash(missingPermissions));

@override
String toString() {
  return 'CyclePeriodData(data: $data, missingPermissions: $missingPermissions)';
}


}

/// @nodoc
abstract mixin class $CyclePeriodDataCopyWith<$Res>  {
  factory $CyclePeriodDataCopyWith(CyclePeriodData value, $Res Function(CyclePeriodData) _then) = _$CyclePeriodDataCopyWithImpl;
@useResult
$Res call({
 CycleData data, Set<String> missingPermissions
});


$CycleDataCopyWith<$Res> get data;

}
/// @nodoc
class _$CyclePeriodDataCopyWithImpl<$Res>
    implements $CyclePeriodDataCopyWith<$Res> {
  _$CyclePeriodDataCopyWithImpl(this._self, this._then);

  final CyclePeriodData _self;
  final $Res Function(CyclePeriodData) _then;

/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? data = null,Object? missingPermissions = null,}) {
  return _then(_self.copyWith(
data: null == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as CycleData,missingPermissions: null == missingPermissions ? _self.missingPermissions : missingPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}
/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CycleDataCopyWith<$Res> get data {
  
  return $CycleDataCopyWith<$Res>(_self.data, (value) {
    return _then(_self.copyWith(data: value));
  });
}
}


/// Adds pattern-matching-related methods to [CyclePeriodData].
extension CyclePeriodDataPatterns on CyclePeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CyclePeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CyclePeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CyclePeriodData value)  $default,){
final _that = this;
switch (_that) {
case _CyclePeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CyclePeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _CyclePeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CycleData data,  Set<String> missingPermissions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CyclePeriodData() when $default != null:
return $default(_that.data,_that.missingPermissions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CycleData data,  Set<String> missingPermissions)  $default,) {final _that = this;
switch (_that) {
case _CyclePeriodData():
return $default(_that.data,_that.missingPermissions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CycleData data,  Set<String> missingPermissions)?  $default,) {final _that = this;
switch (_that) {
case _CyclePeriodData() when $default != null:
return $default(_that.data,_that.missingPermissions);case _:
  return null;

}
}

}

/// @nodoc


class _CyclePeriodData implements CyclePeriodData {
  const _CyclePeriodData({required this.data, required final  Set<String> missingPermissions}): _missingPermissions = missingPermissions;
  

@override final  CycleData data;
 final  Set<String> _missingPermissions;
@override Set<String> get missingPermissions {
  if (_missingPermissions is EqualUnmodifiableSetView) return _missingPermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingPermissions);
}


/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CyclePeriodDataCopyWith<_CyclePeriodData> get copyWith => __$CyclePeriodDataCopyWithImpl<_CyclePeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CyclePeriodData&&(identical(other.data, data) || other.data == data)&&const DeepCollectionEquality().equals(other._missingPermissions, _missingPermissions));
}


@override
int get hashCode => Object.hash(runtimeType,data,const DeepCollectionEquality().hash(_missingPermissions));

@override
String toString() {
  return 'CyclePeriodData(data: $data, missingPermissions: $missingPermissions)';
}


}

/// @nodoc
abstract mixin class _$CyclePeriodDataCopyWith<$Res> implements $CyclePeriodDataCopyWith<$Res> {
  factory _$CyclePeriodDataCopyWith(_CyclePeriodData value, $Res Function(_CyclePeriodData) _then) = __$CyclePeriodDataCopyWithImpl;
@override @useResult
$Res call({
 CycleData data, Set<String> missingPermissions
});


@override $CycleDataCopyWith<$Res> get data;

}
/// @nodoc
class __$CyclePeriodDataCopyWithImpl<$Res>
    implements _$CyclePeriodDataCopyWith<$Res> {
  __$CyclePeriodDataCopyWithImpl(this._self, this._then);

  final _CyclePeriodData _self;
  final $Res Function(_CyclePeriodData) _then;

/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? data = null,Object? missingPermissions = null,}) {
  return _then(_CyclePeriodData(
data: null == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as CycleData,missingPermissions: null == missingPermissions ? _self._missingPermissions : missingPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}

/// Create a copy of CyclePeriodData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CycleDataCopyWith<$Res> get data {
  
  return $CycleDataCopyWith<$Res>(_self.data, (value) {
    return _then(_self.copyWith(data: value));
  });
}
}

// dart format on
