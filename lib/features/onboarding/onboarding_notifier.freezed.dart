// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'onboarding_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$OnboardingState {

 HealthConnectAvailability get availability; Set<String> get grantedPermissions; bool get mindfulnessAvailable; bool get isCheckingPermissions;
/// Create a copy of OnboardingState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$OnboardingStateCopyWith<OnboardingState> get copyWith => _$OnboardingStateCopyWithImpl<OnboardingState>(this as OnboardingState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is OnboardingState&&(identical(other.availability, availability) || other.availability == availability)&&const DeepCollectionEquality().equals(other.grantedPermissions, grantedPermissions)&&(identical(other.mindfulnessAvailable, mindfulnessAvailable) || other.mindfulnessAvailable == mindfulnessAvailable)&&(identical(other.isCheckingPermissions, isCheckingPermissions) || other.isCheckingPermissions == isCheckingPermissions));
}


@override
int get hashCode => Object.hash(runtimeType,availability,const DeepCollectionEquality().hash(grantedPermissions),mindfulnessAvailable,isCheckingPermissions);

@override
String toString() {
  return 'OnboardingState(availability: $availability, grantedPermissions: $grantedPermissions, mindfulnessAvailable: $mindfulnessAvailable, isCheckingPermissions: $isCheckingPermissions)';
}


}

/// @nodoc
abstract mixin class $OnboardingStateCopyWith<$Res>  {
  factory $OnboardingStateCopyWith(OnboardingState value, $Res Function(OnboardingState) _then) = _$OnboardingStateCopyWithImpl;
@useResult
$Res call({
 HealthConnectAvailability availability, Set<String> grantedPermissions, bool mindfulnessAvailable, bool isCheckingPermissions
});




}
/// @nodoc
class _$OnboardingStateCopyWithImpl<$Res>
    implements $OnboardingStateCopyWith<$Res> {
  _$OnboardingStateCopyWithImpl(this._self, this._then);

  final OnboardingState _self;
  final $Res Function(OnboardingState) _then;

/// Create a copy of OnboardingState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? availability = null,Object? grantedPermissions = null,Object? mindfulnessAvailable = null,Object? isCheckingPermissions = null,}) {
  return _then(_self.copyWith(
availability: null == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability,grantedPermissions: null == grantedPermissions ? _self.grantedPermissions : grantedPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,mindfulnessAvailable: null == mindfulnessAvailable ? _self.mindfulnessAvailable : mindfulnessAvailable // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermissions: null == isCheckingPermissions ? _self.isCheckingPermissions : isCheckingPermissions // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [OnboardingState].
extension OnboardingStatePatterns on OnboardingState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _OnboardingState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _OnboardingState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _OnboardingState value)  $default,){
final _that = this;
switch (_that) {
case _OnboardingState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _OnboardingState value)?  $default,){
final _that = this;
switch (_that) {
case _OnboardingState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( HealthConnectAvailability availability,  Set<String> grantedPermissions,  bool mindfulnessAvailable,  bool isCheckingPermissions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _OnboardingState() when $default != null:
return $default(_that.availability,_that.grantedPermissions,_that.mindfulnessAvailable,_that.isCheckingPermissions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( HealthConnectAvailability availability,  Set<String> grantedPermissions,  bool mindfulnessAvailable,  bool isCheckingPermissions)  $default,) {final _that = this;
switch (_that) {
case _OnboardingState():
return $default(_that.availability,_that.grantedPermissions,_that.mindfulnessAvailable,_that.isCheckingPermissions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( HealthConnectAvailability availability,  Set<String> grantedPermissions,  bool mindfulnessAvailable,  bool isCheckingPermissions)?  $default,) {final _that = this;
switch (_that) {
case _OnboardingState() when $default != null:
return $default(_that.availability,_that.grantedPermissions,_that.mindfulnessAvailable,_that.isCheckingPermissions);case _:
  return null;

}
}

}

/// @nodoc


class _OnboardingState implements OnboardingState {
  const _OnboardingState({this.availability = HealthConnectAvailability.available, final  Set<String> grantedPermissions = const <String>{}, this.mindfulnessAvailable = false, this.isCheckingPermissions = true}): _grantedPermissions = grantedPermissions;
  

@override@JsonKey() final  HealthConnectAvailability availability;
 final  Set<String> _grantedPermissions;
@override@JsonKey() Set<String> get grantedPermissions {
  if (_grantedPermissions is EqualUnmodifiableSetView) return _grantedPermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_grantedPermissions);
}

@override@JsonKey() final  bool mindfulnessAvailable;
@override@JsonKey() final  bool isCheckingPermissions;

/// Create a copy of OnboardingState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$OnboardingStateCopyWith<_OnboardingState> get copyWith => __$OnboardingStateCopyWithImpl<_OnboardingState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _OnboardingState&&(identical(other.availability, availability) || other.availability == availability)&&const DeepCollectionEquality().equals(other._grantedPermissions, _grantedPermissions)&&(identical(other.mindfulnessAvailable, mindfulnessAvailable) || other.mindfulnessAvailable == mindfulnessAvailable)&&(identical(other.isCheckingPermissions, isCheckingPermissions) || other.isCheckingPermissions == isCheckingPermissions));
}


@override
int get hashCode => Object.hash(runtimeType,availability,const DeepCollectionEquality().hash(_grantedPermissions),mindfulnessAvailable,isCheckingPermissions);

@override
String toString() {
  return 'OnboardingState(availability: $availability, grantedPermissions: $grantedPermissions, mindfulnessAvailable: $mindfulnessAvailable, isCheckingPermissions: $isCheckingPermissions)';
}


}

/// @nodoc
abstract mixin class _$OnboardingStateCopyWith<$Res> implements $OnboardingStateCopyWith<$Res> {
  factory _$OnboardingStateCopyWith(_OnboardingState value, $Res Function(_OnboardingState) _then) = __$OnboardingStateCopyWithImpl;
@override @useResult
$Res call({
 HealthConnectAvailability availability, Set<String> grantedPermissions, bool mindfulnessAvailable, bool isCheckingPermissions
});




}
/// @nodoc
class __$OnboardingStateCopyWithImpl<$Res>
    implements _$OnboardingStateCopyWith<$Res> {
  __$OnboardingStateCopyWithImpl(this._self, this._then);

  final _OnboardingState _self;
  final $Res Function(_OnboardingState) _then;

/// Create a copy of OnboardingState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? availability = null,Object? grantedPermissions = null,Object? mindfulnessAvailable = null,Object? isCheckingPermissions = null,}) {
  return _then(_OnboardingState(
availability: null == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability,grantedPermissions: null == grantedPermissions ? _self._grantedPermissions : grantedPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,mindfulnessAvailable: null == mindfulnessAvailable ? _self.mindfulnessAvailable : mindfulnessAvailable // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermissions: null == isCheckingPermissions ? _self.isCheckingPermissions : isCheckingPermissions // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
