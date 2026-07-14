// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'caffeine_drink_profile.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaffeineDrinkProfile {

 CaffeineEntry get entry;/// This drink's own rise and fall — not the day's.
 List<CaffeinePoint> get curve;/// The most of this drink that was ever in the body at once, and when.
///
/// Lower than the dose, always: absorption takes time, and elimination has begun
/// before absorption has finished. A 95mg coffee never puts 95mg in you at once.
 double get peakMg; DateTime get peakTime;/// What is left of it now. Zero before it was drunk.
 double get currentMg;/// When half of the peak has gone, and when what remains stops mattering.
/// Null when the drink has not faded within [caffeineProfileHorizon].
 DateTime? get halfGoneTime; DateTime? get goneTime;
/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineDrinkProfileCopyWith<CaffeineDrinkProfile> get copyWith => _$CaffeineDrinkProfileCopyWithImpl<CaffeineDrinkProfile>(this as CaffeineDrinkProfile, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineDrinkProfile&&(identical(other.entry, entry) || other.entry == entry)&&const DeepCollectionEquality().equals(other.curve, curve)&&(identical(other.peakMg, peakMg) || other.peakMg == peakMg)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.currentMg, currentMg) || other.currentMg == currentMg)&&(identical(other.halfGoneTime, halfGoneTime) || other.halfGoneTime == halfGoneTime)&&(identical(other.goneTime, goneTime) || other.goneTime == goneTime));
}


@override
int get hashCode => Object.hash(runtimeType,entry,const DeepCollectionEquality().hash(curve),peakMg,peakTime,currentMg,halfGoneTime,goneTime);

@override
String toString() {
  return 'CaffeineDrinkProfile(entry: $entry, curve: $curve, peakMg: $peakMg, peakTime: $peakTime, currentMg: $currentMg, halfGoneTime: $halfGoneTime, goneTime: $goneTime)';
}


}

/// @nodoc
abstract mixin class $CaffeineDrinkProfileCopyWith<$Res>  {
  factory $CaffeineDrinkProfileCopyWith(CaffeineDrinkProfile value, $Res Function(CaffeineDrinkProfile) _then) = _$CaffeineDrinkProfileCopyWithImpl;
@useResult
$Res call({
 CaffeineEntry entry, List<CaffeinePoint> curve, double peakMg, DateTime peakTime, double currentMg, DateTime? halfGoneTime, DateTime? goneTime
});


$CaffeineEntryCopyWith<$Res> get entry;

}
/// @nodoc
class _$CaffeineDrinkProfileCopyWithImpl<$Res>
    implements $CaffeineDrinkProfileCopyWith<$Res> {
  _$CaffeineDrinkProfileCopyWithImpl(this._self, this._then);

  final CaffeineDrinkProfile _self;
  final $Res Function(CaffeineDrinkProfile) _then;

/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? entry = null,Object? curve = null,Object? peakMg = null,Object? peakTime = null,Object? currentMg = null,Object? halfGoneTime = freezed,Object? goneTime = freezed,}) {
  return _then(_self.copyWith(
entry: null == entry ? _self.entry : entry // ignore: cast_nullable_to_non_nullable
as CaffeineEntry,curve: null == curve ? _self.curve : curve // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,peakMg: null == peakMg ? _self.peakMg : peakMg // ignore: cast_nullable_to_non_nullable
as double,peakTime: null == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime,currentMg: null == currentMg ? _self.currentMg : currentMg // ignore: cast_nullable_to_non_nullable
as double,halfGoneTime: freezed == halfGoneTime ? _self.halfGoneTime : halfGoneTime // ignore: cast_nullable_to_non_nullable
as DateTime?,goneTime: freezed == goneTime ? _self.goneTime : goneTime // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}
/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineEntryCopyWith<$Res> get entry {
  
  return $CaffeineEntryCopyWith<$Res>(_self.entry, (value) {
    return _then(_self.copyWith(entry: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineDrinkProfile].
extension CaffeineDrinkProfilePatterns on CaffeineDrinkProfile {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineDrinkProfile value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineDrinkProfile() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineDrinkProfile value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineDrinkProfile():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineDrinkProfile value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineDrinkProfile() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineEntry entry,  List<CaffeinePoint> curve,  double peakMg,  DateTime peakTime,  double currentMg,  DateTime? halfGoneTime,  DateTime? goneTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineDrinkProfile() when $default != null:
return $default(_that.entry,_that.curve,_that.peakMg,_that.peakTime,_that.currentMg,_that.halfGoneTime,_that.goneTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineEntry entry,  List<CaffeinePoint> curve,  double peakMg,  DateTime peakTime,  double currentMg,  DateTime? halfGoneTime,  DateTime? goneTime)  $default,) {final _that = this;
switch (_that) {
case _CaffeineDrinkProfile():
return $default(_that.entry,_that.curve,_that.peakMg,_that.peakTime,_that.currentMg,_that.halfGoneTime,_that.goneTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineEntry entry,  List<CaffeinePoint> curve,  double peakMg,  DateTime peakTime,  double currentMg,  DateTime? halfGoneTime,  DateTime? goneTime)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineDrinkProfile() when $default != null:
return $default(_that.entry,_that.curve,_that.peakMg,_that.peakTime,_that.currentMg,_that.halfGoneTime,_that.goneTime);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineDrinkProfile extends CaffeineDrinkProfile {
  const _CaffeineDrinkProfile({required this.entry, required final  List<CaffeinePoint> curve, required this.peakMg, required this.peakTime, required this.currentMg, required this.halfGoneTime, required this.goneTime}): _curve = curve,super._();
  

@override final  CaffeineEntry entry;
/// This drink's own rise and fall — not the day's.
 final  List<CaffeinePoint> _curve;
/// This drink's own rise and fall — not the day's.
@override List<CaffeinePoint> get curve {
  if (_curve is EqualUnmodifiableListView) return _curve;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_curve);
}

/// The most of this drink that was ever in the body at once, and when.
///
/// Lower than the dose, always: absorption takes time, and elimination has begun
/// before absorption has finished. A 95mg coffee never puts 95mg in you at once.
@override final  double peakMg;
@override final  DateTime peakTime;
/// What is left of it now. Zero before it was drunk.
@override final  double currentMg;
/// When half of the peak has gone, and when what remains stops mattering.
/// Null when the drink has not faded within [caffeineProfileHorizon].
@override final  DateTime? halfGoneTime;
@override final  DateTime? goneTime;

/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineDrinkProfileCopyWith<_CaffeineDrinkProfile> get copyWith => __$CaffeineDrinkProfileCopyWithImpl<_CaffeineDrinkProfile>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineDrinkProfile&&(identical(other.entry, entry) || other.entry == entry)&&const DeepCollectionEquality().equals(other._curve, _curve)&&(identical(other.peakMg, peakMg) || other.peakMg == peakMg)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.currentMg, currentMg) || other.currentMg == currentMg)&&(identical(other.halfGoneTime, halfGoneTime) || other.halfGoneTime == halfGoneTime)&&(identical(other.goneTime, goneTime) || other.goneTime == goneTime));
}


@override
int get hashCode => Object.hash(runtimeType,entry,const DeepCollectionEquality().hash(_curve),peakMg,peakTime,currentMg,halfGoneTime,goneTime);

@override
String toString() {
  return 'CaffeineDrinkProfile(entry: $entry, curve: $curve, peakMg: $peakMg, peakTime: $peakTime, currentMg: $currentMg, halfGoneTime: $halfGoneTime, goneTime: $goneTime)';
}


}

/// @nodoc
abstract mixin class _$CaffeineDrinkProfileCopyWith<$Res> implements $CaffeineDrinkProfileCopyWith<$Res> {
  factory _$CaffeineDrinkProfileCopyWith(_CaffeineDrinkProfile value, $Res Function(_CaffeineDrinkProfile) _then) = __$CaffeineDrinkProfileCopyWithImpl;
@override @useResult
$Res call({
 CaffeineEntry entry, List<CaffeinePoint> curve, double peakMg, DateTime peakTime, double currentMg, DateTime? halfGoneTime, DateTime? goneTime
});


@override $CaffeineEntryCopyWith<$Res> get entry;

}
/// @nodoc
class __$CaffeineDrinkProfileCopyWithImpl<$Res>
    implements _$CaffeineDrinkProfileCopyWith<$Res> {
  __$CaffeineDrinkProfileCopyWithImpl(this._self, this._then);

  final _CaffeineDrinkProfile _self;
  final $Res Function(_CaffeineDrinkProfile) _then;

/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? entry = null,Object? curve = null,Object? peakMg = null,Object? peakTime = null,Object? currentMg = null,Object? halfGoneTime = freezed,Object? goneTime = freezed,}) {
  return _then(_CaffeineDrinkProfile(
entry: null == entry ? _self.entry : entry // ignore: cast_nullable_to_non_nullable
as CaffeineEntry,curve: null == curve ? _self._curve : curve // ignore: cast_nullable_to_non_nullable
as List<CaffeinePoint>,peakMg: null == peakMg ? _self.peakMg : peakMg // ignore: cast_nullable_to_non_nullable
as double,peakTime: null == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime,currentMg: null == currentMg ? _self.currentMg : currentMg // ignore: cast_nullable_to_non_nullable
as double,halfGoneTime: freezed == halfGoneTime ? _self.halfGoneTime : halfGoneTime // ignore: cast_nullable_to_non_nullable
as DateTime?,goneTime: freezed == goneTime ? _self.goneTime : goneTime // ignore: cast_nullable_to_non_nullable
as DateTime?,
  ));
}

/// Create a copy of CaffeineDrinkProfile
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineEntryCopyWith<$Res> get entry {
  
  return $CaffeineEntryCopyWith<$Res>(_self.entry, (value) {
    return _then(_self.copyWith(entry: value));
  });
}
}

// dart format on
