// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'caffeine_preferences_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaffeinePreferencesState {

 CaffeinePreferences get draft; BodyProfile get bodyProfile; int get seedRevision;
/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeinePreferencesStateCopyWith<CaffeinePreferencesState> get copyWith => _$CaffeinePreferencesStateCopyWithImpl<CaffeinePreferencesState>(this as CaffeinePreferencesState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeinePreferencesState&&(identical(other.draft, draft) || other.draft == draft)&&(identical(other.bodyProfile, bodyProfile) || other.bodyProfile == bodyProfile)&&(identical(other.seedRevision, seedRevision) || other.seedRevision == seedRevision));
}


@override
int get hashCode => Object.hash(runtimeType,draft,bodyProfile,seedRevision);

@override
String toString() {
  return 'CaffeinePreferencesState(draft: $draft, bodyProfile: $bodyProfile, seedRevision: $seedRevision)';
}


}

/// @nodoc
abstract mixin class $CaffeinePreferencesStateCopyWith<$Res>  {
  factory $CaffeinePreferencesStateCopyWith(CaffeinePreferencesState value, $Res Function(CaffeinePreferencesState) _then) = _$CaffeinePreferencesStateCopyWithImpl;
@useResult
$Res call({
 CaffeinePreferences draft, BodyProfile bodyProfile, int seedRevision
});


$CaffeinePreferencesCopyWith<$Res> get draft;$BodyProfileCopyWith<$Res> get bodyProfile;

}
/// @nodoc
class _$CaffeinePreferencesStateCopyWithImpl<$Res>
    implements $CaffeinePreferencesStateCopyWith<$Res> {
  _$CaffeinePreferencesStateCopyWithImpl(this._self, this._then);

  final CaffeinePreferencesState _self;
  final $Res Function(CaffeinePreferencesState) _then;

/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? draft = null,Object? bodyProfile = null,Object? seedRevision = null,}) {
  return _then(_self.copyWith(
draft: null == draft ? _self.draft : draft // ignore: cast_nullable_to_non_nullable
as CaffeinePreferences,bodyProfile: null == bodyProfile ? _self.bodyProfile : bodyProfile // ignore: cast_nullable_to_non_nullable
as BodyProfile,seedRevision: null == seedRevision ? _self.seedRevision : seedRevision // ignore: cast_nullable_to_non_nullable
as int,
  ));
}
/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeinePreferencesCopyWith<$Res> get draft {
  
  return $CaffeinePreferencesCopyWith<$Res>(_self.draft, (value) {
    return _then(_self.copyWith(draft: value));
  });
}/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyProfileCopyWith<$Res> get bodyProfile {
  
  return $BodyProfileCopyWith<$Res>(_self.bodyProfile, (value) {
    return _then(_self.copyWith(bodyProfile: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeinePreferencesState].
extension CaffeinePreferencesStatePatterns on CaffeinePreferencesState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeinePreferencesState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeinePreferencesState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeinePreferencesState value)  $default,){
final _that = this;
switch (_that) {
case _CaffeinePreferencesState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeinePreferencesState value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeinePreferencesState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeinePreferences draft,  BodyProfile bodyProfile,  int seedRevision)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeinePreferencesState() when $default != null:
return $default(_that.draft,_that.bodyProfile,_that.seedRevision);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeinePreferences draft,  BodyProfile bodyProfile,  int seedRevision)  $default,) {final _that = this;
switch (_that) {
case _CaffeinePreferencesState():
return $default(_that.draft,_that.bodyProfile,_that.seedRevision);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeinePreferences draft,  BodyProfile bodyProfile,  int seedRevision)?  $default,) {final _that = this;
switch (_that) {
case _CaffeinePreferencesState() when $default != null:
return $default(_that.draft,_that.bodyProfile,_that.seedRevision);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeinePreferencesState implements CaffeinePreferencesState {
  const _CaffeinePreferencesState({required this.draft, required this.bodyProfile, this.seedRevision = 0});
  

@override final  CaffeinePreferences draft;
@override final  BodyProfile bodyProfile;
@override@JsonKey() final  int seedRevision;

/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeinePreferencesStateCopyWith<_CaffeinePreferencesState> get copyWith => __$CaffeinePreferencesStateCopyWithImpl<_CaffeinePreferencesState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeinePreferencesState&&(identical(other.draft, draft) || other.draft == draft)&&(identical(other.bodyProfile, bodyProfile) || other.bodyProfile == bodyProfile)&&(identical(other.seedRevision, seedRevision) || other.seedRevision == seedRevision));
}


@override
int get hashCode => Object.hash(runtimeType,draft,bodyProfile,seedRevision);

@override
String toString() {
  return 'CaffeinePreferencesState(draft: $draft, bodyProfile: $bodyProfile, seedRevision: $seedRevision)';
}


}

/// @nodoc
abstract mixin class _$CaffeinePreferencesStateCopyWith<$Res> implements $CaffeinePreferencesStateCopyWith<$Res> {
  factory _$CaffeinePreferencesStateCopyWith(_CaffeinePreferencesState value, $Res Function(_CaffeinePreferencesState) _then) = __$CaffeinePreferencesStateCopyWithImpl;
@override @useResult
$Res call({
 CaffeinePreferences draft, BodyProfile bodyProfile, int seedRevision
});


@override $CaffeinePreferencesCopyWith<$Res> get draft;@override $BodyProfileCopyWith<$Res> get bodyProfile;

}
/// @nodoc
class __$CaffeinePreferencesStateCopyWithImpl<$Res>
    implements _$CaffeinePreferencesStateCopyWith<$Res> {
  __$CaffeinePreferencesStateCopyWithImpl(this._self, this._then);

  final _CaffeinePreferencesState _self;
  final $Res Function(_CaffeinePreferencesState) _then;

/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? draft = null,Object? bodyProfile = null,Object? seedRevision = null,}) {
  return _then(_CaffeinePreferencesState(
draft: null == draft ? _self.draft : draft // ignore: cast_nullable_to_non_nullable
as CaffeinePreferences,bodyProfile: null == bodyProfile ? _self.bodyProfile : bodyProfile // ignore: cast_nullable_to_non_nullable
as BodyProfile,seedRevision: null == seedRevision ? _self.seedRevision : seedRevision // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeinePreferencesCopyWith<$Res> get draft {
  
  return $CaffeinePreferencesCopyWith<$Res>(_self.draft, (value) {
    return _then(_self.copyWith(draft: value));
  });
}/// Create a copy of CaffeinePreferencesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyProfileCopyWith<$Res> get bodyProfile {
  
  return $BodyProfileCopyWith<$Res>(_self.bodyProfile, (value) {
    return _then(_self.copyWith(bodyProfile: value));
  });
}
}

// dart format on
