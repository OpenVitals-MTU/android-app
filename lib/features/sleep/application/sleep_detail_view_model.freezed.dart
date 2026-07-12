// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_detail_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepDetailState {

 bool get isLoading; SleepData? get session; ScreenError? get error; SleepDetailDisplay? get display;
/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepDetailStateCopyWith<SleepDetailState> get copyWith => _$SleepDetailStateCopyWithImpl<SleepDetailState>(this as SleepDetailState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepDetailState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.session, session) || other.session == session)&&(identical(other.error, error) || other.error == error)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,session,error,display);

@override
String toString() {
  return 'SleepDetailState(isLoading: $isLoading, session: $session, error: $error, display: $display)';
}


}

/// @nodoc
abstract mixin class $SleepDetailStateCopyWith<$Res>  {
  factory $SleepDetailStateCopyWith(SleepDetailState value, $Res Function(SleepDetailState) _then) = _$SleepDetailStateCopyWithImpl;
@useResult
$Res call({
 bool isLoading, SleepData? session, ScreenError? error, SleepDetailDisplay? display
});


$SleepDataCopyWith<$Res>? get session;$SleepDetailDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$SleepDetailStateCopyWithImpl<$Res>
    implements $SleepDetailStateCopyWith<$Res> {
  _$SleepDetailStateCopyWithImpl(this._self, this._then);

  final SleepDetailState _self;
  final $Res Function(SleepDetailState) _then;

/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isLoading = null,Object? session = freezed,Object? error = freezed,Object? display = freezed,}) {
  return _then(_self.copyWith(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,session: freezed == session ? _self.session : session // ignore: cast_nullable_to_non_nullable
as SleepData?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as SleepDetailDisplay?,
  ));
}
/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get session {
    if (_self.session == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.session!, (value) {
    return _then(_self.copyWith(session: value));
  });
}/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDetailDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $SleepDetailDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [SleepDetailState].
extension SleepDetailStatePatterns on SleepDetailState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepDetailState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepDetailState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepDetailState value)  $default,){
final _that = this;
switch (_that) {
case _SleepDetailState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepDetailState value)?  $default,){
final _that = this;
switch (_that) {
case _SleepDetailState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isLoading,  SleepData? session,  ScreenError? error,  SleepDetailDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepDetailState() when $default != null:
return $default(_that.isLoading,_that.session,_that.error,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isLoading,  SleepData? session,  ScreenError? error,  SleepDetailDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _SleepDetailState():
return $default(_that.isLoading,_that.session,_that.error,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isLoading,  SleepData? session,  ScreenError? error,  SleepDetailDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _SleepDetailState() when $default != null:
return $default(_that.isLoading,_that.session,_that.error,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _SleepDetailState extends SleepDetailState {
  const _SleepDetailState({this.isLoading = true, this.session, this.error, this.display}): super._();
  

@override@JsonKey() final  bool isLoading;
@override final  SleepData? session;
@override final  ScreenError? error;
@override final  SleepDetailDisplay? display;

/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepDetailStateCopyWith<_SleepDetailState> get copyWith => __$SleepDetailStateCopyWithImpl<_SleepDetailState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepDetailState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.session, session) || other.session == session)&&(identical(other.error, error) || other.error == error)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,session,error,display);

@override
String toString() {
  return 'SleepDetailState(isLoading: $isLoading, session: $session, error: $error, display: $display)';
}


}

/// @nodoc
abstract mixin class _$SleepDetailStateCopyWith<$Res> implements $SleepDetailStateCopyWith<$Res> {
  factory _$SleepDetailStateCopyWith(_SleepDetailState value, $Res Function(_SleepDetailState) _then) = __$SleepDetailStateCopyWithImpl;
@override @useResult
$Res call({
 bool isLoading, SleepData? session, ScreenError? error, SleepDetailDisplay? display
});


@override $SleepDataCopyWith<$Res>? get session;@override $SleepDetailDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$SleepDetailStateCopyWithImpl<$Res>
    implements _$SleepDetailStateCopyWith<$Res> {
  __$SleepDetailStateCopyWithImpl(this._self, this._then);

  final _SleepDetailState _self;
  final $Res Function(_SleepDetailState) _then;

/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isLoading = null,Object? session = freezed,Object? error = freezed,Object? display = freezed,}) {
  return _then(_SleepDetailState(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,session: freezed == session ? _self.session : session // ignore: cast_nullable_to_non_nullable
as SleepData?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as SleepDetailDisplay?,
  ));
}

/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get session {
    if (_self.session == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.session!, (value) {
    return _then(_self.copyWith(session: value));
  });
}/// Create a copy of SleepDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDetailDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $SleepDetailDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
