// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'apple_health_import_card_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$AppleHealthImportCardState {

 Set<String> get importPermissions; Set<String> get granted; HealthConnectAvailability? get availability; CommandState<void> get grant;/// The last report save: `success(true)` saved, `success(false)` cancelled
/// or refused by the platform.
 CommandState<bool> get saveReport;
/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AppleHealthImportCardStateCopyWith<AppleHealthImportCardState> get copyWith => _$AppleHealthImportCardStateCopyWithImpl<AppleHealthImportCardState>(this as AppleHealthImportCardState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AppleHealthImportCardState&&const DeepCollectionEquality().equals(other.importPermissions, importPermissions)&&const DeepCollectionEquality().equals(other.granted, granted)&&(identical(other.availability, availability) || other.availability == availability)&&(identical(other.grant, grant) || other.grant == grant)&&(identical(other.saveReport, saveReport) || other.saveReport == saveReport));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(importPermissions),const DeepCollectionEquality().hash(granted),availability,grant,saveReport);

@override
String toString() {
  return 'AppleHealthImportCardState(importPermissions: $importPermissions, granted: $granted, availability: $availability, grant: $grant, saveReport: $saveReport)';
}


}

/// @nodoc
abstract mixin class $AppleHealthImportCardStateCopyWith<$Res>  {
  factory $AppleHealthImportCardStateCopyWith(AppleHealthImportCardState value, $Res Function(AppleHealthImportCardState) _then) = _$AppleHealthImportCardStateCopyWithImpl;
@useResult
$Res call({
 Set<String> importPermissions, Set<String> granted, HealthConnectAvailability? availability, CommandState<void> grant, CommandState<bool> saveReport
});


$CommandStateCopyWith<void, $Res> get grant;$CommandStateCopyWith<bool, $Res> get saveReport;

}
/// @nodoc
class _$AppleHealthImportCardStateCopyWithImpl<$Res>
    implements $AppleHealthImportCardStateCopyWith<$Res> {
  _$AppleHealthImportCardStateCopyWithImpl(this._self, this._then);

  final AppleHealthImportCardState _self;
  final $Res Function(AppleHealthImportCardState) _then;

/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? importPermissions = null,Object? granted = null,Object? availability = freezed,Object? grant = null,Object? saveReport = null,}) {
  return _then(_self.copyWith(
importPermissions: null == importPermissions ? _self.importPermissions : importPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,granted: null == granted ? _self.granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,grant: null == grant ? _self.grant : grant // ignore: cast_nullable_to_non_nullable
as CommandState<void>,saveReport: null == saveReport ? _self.saveReport : saveReport // ignore: cast_nullable_to_non_nullable
as CommandState<bool>,
  ));
}
/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get grant {
  
  return $CommandStateCopyWith<void, $Res>(_self.grant, (value) {
    return _then(_self.copyWith(grant: value));
  });
}/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<bool, $Res> get saveReport {
  
  return $CommandStateCopyWith<bool, $Res>(_self.saveReport, (value) {
    return _then(_self.copyWith(saveReport: value));
  });
}
}


/// Adds pattern-matching-related methods to [AppleHealthImportCardState].
extension AppleHealthImportCardStatePatterns on AppleHealthImportCardState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AppleHealthImportCardState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AppleHealthImportCardState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AppleHealthImportCardState value)  $default,){
final _that = this;
switch (_that) {
case _AppleHealthImportCardState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AppleHealthImportCardState value)?  $default,){
final _that = this;
switch (_that) {
case _AppleHealthImportCardState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant,  CommandState<bool> saveReport)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AppleHealthImportCardState() when $default != null:
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant,_that.saveReport);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant,  CommandState<bool> saveReport)  $default,) {final _that = this;
switch (_that) {
case _AppleHealthImportCardState():
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant,_that.saveReport);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( Set<String> importPermissions,  Set<String> granted,  HealthConnectAvailability? availability,  CommandState<void> grant,  CommandState<bool> saveReport)?  $default,) {final _that = this;
switch (_that) {
case _AppleHealthImportCardState() when $default != null:
return $default(_that.importPermissions,_that.granted,_that.availability,_that.grant,_that.saveReport);case _:
  return null;

}
}

}

/// @nodoc


class _AppleHealthImportCardState extends AppleHealthImportCardState {
  const _AppleHealthImportCardState({final  Set<String> importPermissions = const <String>{}, final  Set<String> granted = const <String>{}, this.availability, this.grant = const CommandState<void>.idle(), this.saveReport = const CommandState<bool>.idle()}): _importPermissions = importPermissions,_granted = granted,super._();
  

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
/// The last report save: `success(true)` saved, `success(false)` cancelled
/// or refused by the platform.
@override@JsonKey() final  CommandState<bool> saveReport;

/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AppleHealthImportCardStateCopyWith<_AppleHealthImportCardState> get copyWith => __$AppleHealthImportCardStateCopyWithImpl<_AppleHealthImportCardState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AppleHealthImportCardState&&const DeepCollectionEquality().equals(other._importPermissions, _importPermissions)&&const DeepCollectionEquality().equals(other._granted, _granted)&&(identical(other.availability, availability) || other.availability == availability)&&(identical(other.grant, grant) || other.grant == grant)&&(identical(other.saveReport, saveReport) || other.saveReport == saveReport));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_importPermissions),const DeepCollectionEquality().hash(_granted),availability,grant,saveReport);

@override
String toString() {
  return 'AppleHealthImportCardState(importPermissions: $importPermissions, granted: $granted, availability: $availability, grant: $grant, saveReport: $saveReport)';
}


}

/// @nodoc
abstract mixin class _$AppleHealthImportCardStateCopyWith<$Res> implements $AppleHealthImportCardStateCopyWith<$Res> {
  factory _$AppleHealthImportCardStateCopyWith(_AppleHealthImportCardState value, $Res Function(_AppleHealthImportCardState) _then) = __$AppleHealthImportCardStateCopyWithImpl;
@override @useResult
$Res call({
 Set<String> importPermissions, Set<String> granted, HealthConnectAvailability? availability, CommandState<void> grant, CommandState<bool> saveReport
});


@override $CommandStateCopyWith<void, $Res> get grant;@override $CommandStateCopyWith<bool, $Res> get saveReport;

}
/// @nodoc
class __$AppleHealthImportCardStateCopyWithImpl<$Res>
    implements _$AppleHealthImportCardStateCopyWith<$Res> {
  __$AppleHealthImportCardStateCopyWithImpl(this._self, this._then);

  final _AppleHealthImportCardState _self;
  final $Res Function(_AppleHealthImportCardState) _then;

/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? importPermissions = null,Object? granted = null,Object? availability = freezed,Object? grant = null,Object? saveReport = null,}) {
  return _then(_AppleHealthImportCardState(
importPermissions: null == importPermissions ? _self._importPermissions : importPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,granted: null == granted ? _self._granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,grant: null == grant ? _self.grant : grant // ignore: cast_nullable_to_non_nullable
as CommandState<void>,saveReport: null == saveReport ? _self.saveReport : saveReport // ignore: cast_nullable_to_non_nullable
as CommandState<bool>,
  ));
}

/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get grant {
  
  return $CommandStateCopyWith<void, $Res>(_self.grant, (value) {
    return _then(_self.copyWith(grant: value));
  });
}/// Create a copy of AppleHealthImportCardState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<bool, $Res> get saveReport {
  
  return $CommandStateCopyWith<bool, $Res>(_self.saveReport, (value) {
    return _then(_self.copyWith(saveReport: value));
  });
}
}

// dart format on
