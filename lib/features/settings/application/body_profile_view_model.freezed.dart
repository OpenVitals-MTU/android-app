// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_profile_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyProfileCardState {

 BodyProfile get profile;
/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyProfileCardStateCopyWith<BodyProfileCardState> get copyWith => _$BodyProfileCardStateCopyWithImpl<BodyProfileCardState>(this as BodyProfileCardState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyProfileCardState&&(identical(other.profile, profile) || other.profile == profile));
}


@override
int get hashCode => Object.hash(runtimeType,profile);

@override
String toString() {
  return 'BodyProfileCardState(profile: $profile)';
}


}

/// @nodoc
abstract mixin class $BodyProfileCardStateCopyWith<$Res>  {
  factory $BodyProfileCardStateCopyWith(BodyProfileCardState value, $Res Function(BodyProfileCardState) _then) = _$BodyProfileCardStateCopyWithImpl;
@useResult
$Res call({
 BodyProfile profile
});


$BodyProfileCopyWith<$Res> get profile;

}
/// @nodoc
class _$BodyProfileCardStateCopyWithImpl<$Res>
    implements $BodyProfileCardStateCopyWith<$Res> {
  _$BodyProfileCardStateCopyWithImpl(this._self, this._then);

  final BodyProfileCardState _self;
  final $Res Function(BodyProfileCardState) _then;

/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? profile = null,}) {
  return _then(_self.copyWith(
profile: null == profile ? _self.profile : profile // ignore: cast_nullable_to_non_nullable
as BodyProfile,
  ));
}
/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyProfileCopyWith<$Res> get profile {
  
  return $BodyProfileCopyWith<$Res>(_self.profile, (value) {
    return _then(_self.copyWith(profile: value));
  });
}
}


/// Adds pattern-matching-related methods to [BodyProfileCardState].
extension BodyProfileCardStatePatterns on BodyProfileCardState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyProfileCardState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyProfileCardState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyProfileCardState value)  $default,){
final _that = this;
switch (_that) {
case _BodyProfileCardState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyProfileCardState value)?  $default,){
final _that = this;
switch (_that) {
case _BodyProfileCardState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodyProfile profile)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyProfileCardState() when $default != null:
return $default(_that.profile);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodyProfile profile)  $default,) {final _that = this;
switch (_that) {
case _BodyProfileCardState():
return $default(_that.profile);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodyProfile profile)?  $default,) {final _that = this;
switch (_that) {
case _BodyProfileCardState() when $default != null:
return $default(_that.profile);case _:
  return null;

}
}

}

/// @nodoc


class _BodyProfileCardState implements BodyProfileCardState {
  const _BodyProfileCardState({required this.profile});
  

@override final  BodyProfile profile;

/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyProfileCardStateCopyWith<_BodyProfileCardState> get copyWith => __$BodyProfileCardStateCopyWithImpl<_BodyProfileCardState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyProfileCardState&&(identical(other.profile, profile) || other.profile == profile));
}


@override
int get hashCode => Object.hash(runtimeType,profile);

@override
String toString() {
  return 'BodyProfileCardState(profile: $profile)';
}


}

/// @nodoc
abstract mixin class _$BodyProfileCardStateCopyWith<$Res> implements $BodyProfileCardStateCopyWith<$Res> {
  factory _$BodyProfileCardStateCopyWith(_BodyProfileCardState value, $Res Function(_BodyProfileCardState) _then) = __$BodyProfileCardStateCopyWithImpl;
@override @useResult
$Res call({
 BodyProfile profile
});


@override $BodyProfileCopyWith<$Res> get profile;

}
/// @nodoc
class __$BodyProfileCardStateCopyWithImpl<$Res>
    implements _$BodyProfileCardStateCopyWith<$Res> {
  __$BodyProfileCardStateCopyWithImpl(this._self, this._then);

  final _BodyProfileCardState _self;
  final $Res Function(_BodyProfileCardState) _then;

/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? profile = null,}) {
  return _then(_BodyProfileCardState(
profile: null == profile ? _self.profile : profile // ignore: cast_nullable_to_non_nullable
as BodyProfile,
  ));
}

/// Create a copy of BodyProfileCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyProfileCopyWith<$Res> get profile {
  
  return $BodyProfileCopyWith<$Res>(_self.profile, (value) {
    return _then(_self.copyWith(profile: value));
  });
}
}

// dart format on
