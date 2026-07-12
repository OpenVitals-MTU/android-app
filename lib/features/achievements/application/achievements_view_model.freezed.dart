// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'achievements_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$AchievementsState {

 bool get isLoading; AchievementsDisplay? get display; ScreenError? get error;
/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AchievementsStateCopyWith<AchievementsState> get copyWith => _$AchievementsStateCopyWithImpl<AchievementsState>(this as AchievementsState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AchievementsState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.display, display) || other.display == display)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,display,error);

@override
String toString() {
  return 'AchievementsState(isLoading: $isLoading, display: $display, error: $error)';
}


}

/// @nodoc
abstract mixin class $AchievementsStateCopyWith<$Res>  {
  factory $AchievementsStateCopyWith(AchievementsState value, $Res Function(AchievementsState) _then) = _$AchievementsStateCopyWithImpl;
@useResult
$Res call({
 bool isLoading, AchievementsDisplay? display, ScreenError? error
});


$AchievementsDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$AchievementsStateCopyWithImpl<$Res>
    implements $AchievementsStateCopyWith<$Res> {
  _$AchievementsStateCopyWithImpl(this._self, this._then);

  final AchievementsState _self;
  final $Res Function(AchievementsState) _then;

/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isLoading = null,Object? display = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as AchievementsDisplay?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}
/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$AchievementsDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $AchievementsDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [AchievementsState].
extension AchievementsStatePatterns on AchievementsState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AchievementsState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AchievementsState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AchievementsState value)  $default,){
final _that = this;
switch (_that) {
case _AchievementsState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AchievementsState value)?  $default,){
final _that = this;
switch (_that) {
case _AchievementsState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isLoading,  AchievementsDisplay? display,  ScreenError? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AchievementsState() when $default != null:
return $default(_that.isLoading,_that.display,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isLoading,  AchievementsDisplay? display,  ScreenError? error)  $default,) {final _that = this;
switch (_that) {
case _AchievementsState():
return $default(_that.isLoading,_that.display,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isLoading,  AchievementsDisplay? display,  ScreenError? error)?  $default,) {final _that = this;
switch (_that) {
case _AchievementsState() when $default != null:
return $default(_that.isLoading,_that.display,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _AchievementsState extends AchievementsState {
  const _AchievementsState({this.isLoading = true, this.display, this.error}): super._();
  

@override@JsonKey() final  bool isLoading;
@override final  AchievementsDisplay? display;
@override final  ScreenError? error;

/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AchievementsStateCopyWith<_AchievementsState> get copyWith => __$AchievementsStateCopyWithImpl<_AchievementsState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AchievementsState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.display, display) || other.display == display)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,display,error);

@override
String toString() {
  return 'AchievementsState(isLoading: $isLoading, display: $display, error: $error)';
}


}

/// @nodoc
abstract mixin class _$AchievementsStateCopyWith<$Res> implements $AchievementsStateCopyWith<$Res> {
  factory _$AchievementsStateCopyWith(_AchievementsState value, $Res Function(_AchievementsState) _then) = __$AchievementsStateCopyWithImpl;
@override @useResult
$Res call({
 bool isLoading, AchievementsDisplay? display, ScreenError? error
});


@override $AchievementsDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$AchievementsStateCopyWithImpl<$Res>
    implements _$AchievementsStateCopyWith<$Res> {
  __$AchievementsStateCopyWithImpl(this._self, this._then);

  final _AchievementsState _self;
  final $Res Function(_AchievementsState) _then;

/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isLoading = null,Object? display = freezed,Object? error = freezed,}) {
  return _then(_AchievementsState(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as AchievementsDisplay?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}

/// Create a copy of AchievementsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$AchievementsDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $AchievementsDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
