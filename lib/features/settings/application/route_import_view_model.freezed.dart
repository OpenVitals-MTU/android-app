// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'route_import_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RouteImportState {

 Set<String> get importPermissions; Set<String> get granted; HealthConnectAvailability? get availability; CommandState<void> get grant;
/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RouteImportStateCopyWith<RouteImportState> get copyWith => _$RouteImportStateCopyWithImpl<RouteImportState>(this as RouteImportState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RouteImportState&&const DeepCollectionEquality().equals(other.importPermissions, importPermissions)&&const DeepCollectionEquality().equals(other.granted, granted)&&(identical(other.availability, availability) || other.availability == availability)&&(identical(other.grant, grant) || other.grant == grant));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(importPermissions),const DeepCollectionEquality().hash(granted),availability,grant);

@override
String toString() {
  return 'RouteImportState(importPermissions: $importPermissions, granted: $granted, availability: $availability, grant: $grant)';
}


}

/// @nodoc
abstract mixin class $RouteImportStateCopyWith<$Res>  {
  factory $RouteImportStateCopyWith(RouteImportState value, $Res Function(RouteImportState) _then) = _$RouteImportStateCopyWithImpl;
@useResult
$Res call({
 Set<String> importPermissions, Set<String> granted, HealthConnectAvailability? availability, CommandState<void> grant
});


$CommandStateCopyWith<void, $Res> get grant;

}
/// @nodoc
class _$RouteImportStateCopyWithImpl<$Res>
    implements $RouteImportStateCopyWith<$Res> {
  _$RouteImportStateCopyWithImpl(this._self, this._then);

  final RouteImportState _self;
  final $Res Function(RouteImportState) _then;

/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? importPermissions = null,Object? granted = null,Object? availability = freezed,Object? grant = null,}) {
  return _then(_self.copyWith(
importPermissions: null == importPermissions ? _self.importPermissions : importPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,granted: null == granted ? _self.granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,grant: null == grant ? _self.grant : grant // ignore: cast_nullable_to_non_nullable
as CommandState<void>,
  ));
}
/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get grant {
  
  return $CommandStateCopyWith<void, $Res>(_self.grant, (value) {
    return _then(_self.copyWith(grant: value));
  });
}
}


/// Adds pattern-matching-related methods to [RouteImportState].
extension RouteImportStatePatterns on RouteImportState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RouteImportState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RouteImportState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RouteImportState value)  $default,){
final _that = this;
switch (_that) {
case _RouteImportState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RouteImportState value)?  $default,){
final _that = this;
switch (_that) {
case _RouteImportState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RouteImportState() when $default != null:
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant)  $default,) {final _that = this;
switch (_that) {
case _RouteImportState():
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant)?  $default,) {final _that = this;
switch (_that) {
case _RouteImportState() when $default != null:
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant);case _:
  return null;

}
}

}

/// @nodoc


class _RouteImportState extends RouteImportState {
  const _RouteImportState({final  Set<String> importPermissions = const <String>{}, final  Set<String> granted = const <String>{}, this.availability, this.grant = const CommandState<void>.idle()}): _importPermissions = importPermissions,_granted = granted,super._();
  

 final  Set<String> _importPermissions;
@override@JsonKey() Set<String> get importPermissions {
  if (_importPermissions is EqualUnmodifiableSetView) return _importPermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_importPermissions);
}

 final  Set<String> _granted;
@override@JsonKey() Set<String> get granted {
  if (_granted is EqualUnmodifiableSetView) return _granted;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_granted);
}

@override final  HealthConnectAvailability? availability;
@override@JsonKey() final  CommandState<void> grant;

/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RouteImportStateCopyWith<_RouteImportState> get copyWith => __$RouteImportStateCopyWithImpl<_RouteImportState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RouteImportState&&const DeepCollectionEquality().equals(other._importPermissions, _importPermissions)&&const DeepCollectionEquality().equals(other._granted, _granted)&&(identical(other.availability, availability) || other.availability == availability)&&(identical(other.grant, grant) || other.grant == grant));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_importPermissions),const DeepCollectionEquality().hash(_granted),availability,grant);

@override
String toString() {
  return 'RouteImportState(importPermissions: $importPermissions, granted: $granted, availability: $availability, grant: $grant)';
}


}

/// @nodoc
abstract mixin class _$RouteImportStateCopyWith<$Res> implements $RouteImportStateCopyWith<$Res> {
  factory _$RouteImportStateCopyWith(_RouteImportState value, $Res Function(_RouteImportState) _then) = __$RouteImportStateCopyWithImpl;
@override @useResult
$Res call({
 Set<String> importPermissions, Set<String> granted, HealthConnectAvailability? availability, CommandState<void> grant
});


@override $CommandStateCopyWith<void, $Res> get grant;

}
/// @nodoc
class __$RouteImportStateCopyWithImpl<$Res>
    implements _$RouteImportStateCopyWith<$Res> {
  __$RouteImportStateCopyWithImpl(this._self, this._then);

  final _RouteImportState _self;
  final $Res Function(_RouteImportState) _then;

/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? importPermissions = null,Object? granted = null,Object? availability = freezed,Object? grant = null,}) {
  return _then(_RouteImportState(
importPermissions: null == importPermissions ? _self._importPermissions : importPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,granted: null == granted ? _self._granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,grant: null == grant ? _self.grant : grant // ignore: cast_nullable_to_non_nullable
as CommandState<void>,
  ));
}

/// Create a copy of RouteImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get grant {
  
  return $CommandStateCopyWith<void, $Res>(_self.grant, (value) {
    return _then(_self.copyWith(grant: value));
  });
}
}

// dart format on
