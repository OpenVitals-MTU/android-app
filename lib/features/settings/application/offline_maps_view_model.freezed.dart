// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'offline_maps_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$OfflineMapsState {

 CommandState<OfflineMapPack> get import;/// Live progress while the import runs; null at rest.
 OfflineMapImportProgress? get progress;
/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$OfflineMapsStateCopyWith<OfflineMapsState> get copyWith => _$OfflineMapsStateCopyWithImpl<OfflineMapsState>(this as OfflineMapsState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is OfflineMapsState&&(identical(other.import, import) || other.import == import)&&(identical(other.progress, progress) || other.progress == progress));
}


@override
int get hashCode => Object.hash(runtimeType,import,progress);

@override
String toString() {
  return 'OfflineMapsState(import: $import, progress: $progress)';
}


}

/// @nodoc
abstract mixin class $OfflineMapsStateCopyWith<$Res>  {
  factory $OfflineMapsStateCopyWith(OfflineMapsState value, $Res Function(OfflineMapsState) _then) = _$OfflineMapsStateCopyWithImpl;
@useResult
$Res call({
 CommandState<OfflineMapPack> import, OfflineMapImportProgress? progress
});


$CommandStateCopyWith<OfflineMapPack, $Res> get import;

}
/// @nodoc
class _$OfflineMapsStateCopyWithImpl<$Res>
    implements $OfflineMapsStateCopyWith<$Res> {
  _$OfflineMapsStateCopyWithImpl(this._self, this._then);

  final OfflineMapsState _self;
  final $Res Function(OfflineMapsState) _then;

/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? import = null,Object? progress = freezed,}) {
  return _then(_self.copyWith(
import: null == import ? _self.import : import // ignore: cast_nullable_to_non_nullable
as CommandState<OfflineMapPack>,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as OfflineMapImportProgress?,
  ));
}
/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<OfflineMapPack, $Res> get import {
  
  return $CommandStateCopyWith<OfflineMapPack, $Res>(_self.import, (value) {
    return _then(_self.copyWith(import: value));
  });
}
}


/// Adds pattern-matching-related methods to [OfflineMapsState].
extension OfflineMapsStatePatterns on OfflineMapsState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _OfflineMapsState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _OfflineMapsState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _OfflineMapsState value)  $default,){
final _that = this;
switch (_that) {
case _OfflineMapsState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _OfflineMapsState value)?  $default,){
final _that = this;
switch (_that) {
case _OfflineMapsState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CommandState<OfflineMapPack> import,  OfflineMapImportProgress? progress)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _OfflineMapsState() when $default != null:
return $default(_that.import,_that.progress);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CommandState<OfflineMapPack> import,  OfflineMapImportProgress? progress)  $default,) {final _that = this;
switch (_that) {
case _OfflineMapsState():
return $default(_that.import,_that.progress);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CommandState<OfflineMapPack> import,  OfflineMapImportProgress? progress)?  $default,) {final _that = this;
switch (_that) {
case _OfflineMapsState() when $default != null:
return $default(_that.import,_that.progress);case _:
  return null;

}
}

}

/// @nodoc


class _OfflineMapsState extends OfflineMapsState {
  const _OfflineMapsState({this.import = const CommandState<OfflineMapPack>.idle(), this.progress}): super._();
  

@override@JsonKey() final  CommandState<OfflineMapPack> import;
/// Live progress while the import runs; null at rest.
@override final  OfflineMapImportProgress? progress;

/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$OfflineMapsStateCopyWith<_OfflineMapsState> get copyWith => __$OfflineMapsStateCopyWithImpl<_OfflineMapsState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _OfflineMapsState&&(identical(other.import, import) || other.import == import)&&(identical(other.progress, progress) || other.progress == progress));
}


@override
int get hashCode => Object.hash(runtimeType,import,progress);

@override
String toString() {
  return 'OfflineMapsState(import: $import, progress: $progress)';
}


}

/// @nodoc
abstract mixin class _$OfflineMapsStateCopyWith<$Res> implements $OfflineMapsStateCopyWith<$Res> {
  factory _$OfflineMapsStateCopyWith(_OfflineMapsState value, $Res Function(_OfflineMapsState) _then) = __$OfflineMapsStateCopyWithImpl;
@override @useResult
$Res call({
 CommandState<OfflineMapPack> import, OfflineMapImportProgress? progress
});


@override $CommandStateCopyWith<OfflineMapPack, $Res> get import;

}
/// @nodoc
class __$OfflineMapsStateCopyWithImpl<$Res>
    implements _$OfflineMapsStateCopyWith<$Res> {
  __$OfflineMapsStateCopyWithImpl(this._self, this._then);

  final _OfflineMapsState _self;
  final $Res Function(_OfflineMapsState) _then;

/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? import = null,Object? progress = freezed,}) {
  return _then(_OfflineMapsState(
import: null == import ? _self.import : import // ignore: cast_nullable_to_non_nullable
as CommandState<OfflineMapPack>,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as OfflineMapImportProgress?,
  ));
}

/// Create a copy of OfflineMapsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<OfflineMapPack, $Res> get import {
  
  return $CommandStateCopyWith<OfflineMapPack, $Res>(_self.import, (value) {
    return _then(_self.copyWith(import: value));
  });
}
}

// dart format on
