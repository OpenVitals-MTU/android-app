// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_profile.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyProfile {

 int? get birthYear; double? get weightKg; int? get restingHeartRateBpm; int? get maxHeartRateBpm;
/// Create a copy of BodyProfile
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyProfileCopyWith<BodyProfile> get copyWith => _$BodyProfileCopyWithImpl<BodyProfile>(this as BodyProfile, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyProfile&&(identical(other.birthYear, birthYear) || other.birthYear == birthYear)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.maxHeartRateBpm, maxHeartRateBpm) || other.maxHeartRateBpm == maxHeartRateBpm));
}


@override
int get hashCode => Object.hash(runtimeType,birthYear,weightKg,restingHeartRateBpm,maxHeartRateBpm);

@override
String toString() {
  return 'BodyProfile(birthYear: $birthYear, weightKg: $weightKg, restingHeartRateBpm: $restingHeartRateBpm, maxHeartRateBpm: $maxHeartRateBpm)';
}


}

/// @nodoc
abstract mixin class $BodyProfileCopyWith<$Res>  {
  factory $BodyProfileCopyWith(BodyProfile value, $Res Function(BodyProfile) _then) = _$BodyProfileCopyWithImpl;
@useResult
$Res call({
 int? birthYear, double? weightKg, int? restingHeartRateBpm, int? maxHeartRateBpm
});




}
/// @nodoc
class _$BodyProfileCopyWithImpl<$Res>
    implements $BodyProfileCopyWith<$Res> {
  _$BodyProfileCopyWithImpl(this._self, this._then);

  final BodyProfile _self;
  final $Res Function(BodyProfile) _then;

/// Create a copy of BodyProfile
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? birthYear = freezed,Object? weightKg = freezed,Object? restingHeartRateBpm = freezed,Object? maxHeartRateBpm = freezed,}) {
  return _then(_self.copyWith(
birthYear: freezed == birthYear ? _self.birthYear : birthYear // ignore: cast_nullable_to_non_nullable
as int?,weightKg: freezed == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double?,restingHeartRateBpm: freezed == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateBpm: freezed == maxHeartRateBpm ? _self.maxHeartRateBpm : maxHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyProfile].
extension BodyProfilePatterns on BodyProfile {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyProfile value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyProfile() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyProfile value)  $default,){
final _that = this;
switch (_that) {
case _BodyProfile():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyProfile value)?  $default,){
final _that = this;
switch (_that) {
case _BodyProfile() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int? birthYear,  double? weightKg,  int? restingHeartRateBpm,  int? maxHeartRateBpm)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyProfile() when $default != null:
return $default(_that.birthYear,_that.weightKg,_that.restingHeartRateBpm,_that.maxHeartRateBpm);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int? birthYear,  double? weightKg,  int? restingHeartRateBpm,  int? maxHeartRateBpm)  $default,) {final _that = this;
switch (_that) {
case _BodyProfile():
return $default(_that.birthYear,_that.weightKg,_that.restingHeartRateBpm,_that.maxHeartRateBpm);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int? birthYear,  double? weightKg,  int? restingHeartRateBpm,  int? maxHeartRateBpm)?  $default,) {final _that = this;
switch (_that) {
case _BodyProfile() when $default != null:
return $default(_that.birthYear,_that.weightKg,_that.restingHeartRateBpm,_that.maxHeartRateBpm);case _:
  return null;

}
}

}

/// @nodoc


class _BodyProfile extends BodyProfile {
  const _BodyProfile({this.birthYear, this.weightKg, this.restingHeartRateBpm, this.maxHeartRateBpm}): super._();
  

@override final  int? birthYear;
@override final  double? weightKg;
@override final  int? restingHeartRateBpm;
@override final  int? maxHeartRateBpm;

/// Create a copy of BodyProfile
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyProfileCopyWith<_BodyProfile> get copyWith => __$BodyProfileCopyWithImpl<_BodyProfile>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyProfile&&(identical(other.birthYear, birthYear) || other.birthYear == birthYear)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.maxHeartRateBpm, maxHeartRateBpm) || other.maxHeartRateBpm == maxHeartRateBpm));
}


@override
int get hashCode => Object.hash(runtimeType,birthYear,weightKg,restingHeartRateBpm,maxHeartRateBpm);

@override
String toString() {
  return 'BodyProfile(birthYear: $birthYear, weightKg: $weightKg, restingHeartRateBpm: $restingHeartRateBpm, maxHeartRateBpm: $maxHeartRateBpm)';
}


}

/// @nodoc
abstract mixin class _$BodyProfileCopyWith<$Res> implements $BodyProfileCopyWith<$Res> {
  factory _$BodyProfileCopyWith(_BodyProfile value, $Res Function(_BodyProfile) _then) = __$BodyProfileCopyWithImpl;
@override @useResult
$Res call({
 int? birthYear, double? weightKg, int? restingHeartRateBpm, int? maxHeartRateBpm
});




}
/// @nodoc
class __$BodyProfileCopyWithImpl<$Res>
    implements _$BodyProfileCopyWith<$Res> {
  __$BodyProfileCopyWithImpl(this._self, this._then);

  final _BodyProfile _self;
  final $Res Function(_BodyProfile) _then;

/// Create a copy of BodyProfile
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? birthYear = freezed,Object? weightKg = freezed,Object? restingHeartRateBpm = freezed,Object? maxHeartRateBpm = freezed,}) {
  return _then(_BodyProfile(
birthYear: freezed == birthYear ? _self.birthYear : birthYear // ignore: cast_nullable_to_non_nullable
as int?,weightKg: freezed == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double?,restingHeartRateBpm: freezed == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateBpm: freezed == maxHeartRateBpm ? _self.maxHeartRateBpm : maxHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

// dart format on
