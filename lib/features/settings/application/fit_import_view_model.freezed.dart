// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'fit_import_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$FitImportState {

/// The picker is up, or the tree is being walked. A folder of a thousand
/// files takes a moment, and a button that looks dead is a button that gets
/// pressed again.
 bool get isScanning;/// The folder was readable and simply had no FIT files in it. Its own state,
/// not an error: the user picked the wrong folder, nothing broke.
 bool get folderHadNoFitFiles;/// How many files were listed, when the folder held more than the scan will
/// take. Null when nothing was dropped.
 int? get truncatedAt;/// The scan itself failed (an unreadable tree, a picker that would not
/// open). A file that fails to import is the bulk importer's business.
 String? get error;
/// Create a copy of FitImportState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$FitImportStateCopyWith<FitImportState> get copyWith => _$FitImportStateCopyWithImpl<FitImportState>(this as FitImportState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is FitImportState&&(identical(other.isScanning, isScanning) || other.isScanning == isScanning)&&(identical(other.folderHadNoFitFiles, folderHadNoFitFiles) || other.folderHadNoFitFiles == folderHadNoFitFiles)&&(identical(other.truncatedAt, truncatedAt) || other.truncatedAt == truncatedAt)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isScanning,folderHadNoFitFiles,truncatedAt,error);

@override
String toString() {
  return 'FitImportState(isScanning: $isScanning, folderHadNoFitFiles: $folderHadNoFitFiles, truncatedAt: $truncatedAt, error: $error)';
}


}

/// @nodoc
abstract mixin class $FitImportStateCopyWith<$Res>  {
  factory $FitImportStateCopyWith(FitImportState value, $Res Function(FitImportState) _then) = _$FitImportStateCopyWithImpl;
@useResult
$Res call({
 bool isScanning, bool folderHadNoFitFiles, int? truncatedAt, String? error
});




}
/// @nodoc
class _$FitImportStateCopyWithImpl<$Res>
    implements $FitImportStateCopyWith<$Res> {
  _$FitImportStateCopyWithImpl(this._self, this._then);

  final FitImportState _self;
  final $Res Function(FitImportState) _then;

/// Create a copy of FitImportState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isScanning = null,Object? folderHadNoFitFiles = null,Object? truncatedAt = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
isScanning: null == isScanning ? _self.isScanning : isScanning // ignore: cast_nullable_to_non_nullable
as bool,folderHadNoFitFiles: null == folderHadNoFitFiles ? _self.folderHadNoFitFiles : folderHadNoFitFiles // ignore: cast_nullable_to_non_nullable
as bool,truncatedAt: freezed == truncatedAt ? _self.truncatedAt : truncatedAt // ignore: cast_nullable_to_non_nullable
as int?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [FitImportState].
extension FitImportStatePatterns on FitImportState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _FitImportState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _FitImportState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _FitImportState value)  $default,){
final _that = this;
switch (_that) {
case _FitImportState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _FitImportState value)?  $default,){
final _that = this;
switch (_that) {
case _FitImportState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isScanning,  bool folderHadNoFitFiles,  int? truncatedAt,  String? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _FitImportState() when $default != null:
return $default(_that.isScanning,_that.folderHadNoFitFiles,_that.truncatedAt,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isScanning,  bool folderHadNoFitFiles,  int? truncatedAt,  String? error)  $default,) {final _that = this;
switch (_that) {
case _FitImportState():
return $default(_that.isScanning,_that.folderHadNoFitFiles,_that.truncatedAt,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isScanning,  bool folderHadNoFitFiles,  int? truncatedAt,  String? error)?  $default,) {final _that = this;
switch (_that) {
case _FitImportState() when $default != null:
return $default(_that.isScanning,_that.folderHadNoFitFiles,_that.truncatedAt,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _FitImportState implements FitImportState {
  const _FitImportState({this.isScanning = false, this.folderHadNoFitFiles = false, this.truncatedAt, this.error});
  

/// The picker is up, or the tree is being walked. A folder of a thousand
/// files takes a moment, and a button that looks dead is a button that gets
/// pressed again.
@override@JsonKey() final  bool isScanning;
/// The folder was readable and simply had no FIT files in it. Its own state,
/// not an error: the user picked the wrong folder, nothing broke.
@override@JsonKey() final  bool folderHadNoFitFiles;
/// How many files were listed, when the folder held more than the scan will
/// take. Null when nothing was dropped.
@override final  int? truncatedAt;
/// The scan itself failed (an unreadable tree, a picker that would not
/// open). A file that fails to import is the bulk importer's business.
@override final  String? error;

/// Create a copy of FitImportState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$FitImportStateCopyWith<_FitImportState> get copyWith => __$FitImportStateCopyWithImpl<_FitImportState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _FitImportState&&(identical(other.isScanning, isScanning) || other.isScanning == isScanning)&&(identical(other.folderHadNoFitFiles, folderHadNoFitFiles) || other.folderHadNoFitFiles == folderHadNoFitFiles)&&(identical(other.truncatedAt, truncatedAt) || other.truncatedAt == truncatedAt)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isScanning,folderHadNoFitFiles,truncatedAt,error);

@override
String toString() {
  return 'FitImportState(isScanning: $isScanning, folderHadNoFitFiles: $folderHadNoFitFiles, truncatedAt: $truncatedAt, error: $error)';
}


}

/// @nodoc
abstract mixin class _$FitImportStateCopyWith<$Res> implements $FitImportStateCopyWith<$Res> {
  factory _$FitImportStateCopyWith(_FitImportState value, $Res Function(_FitImportState) _then) = __$FitImportStateCopyWithImpl;
@override @useResult
$Res call({
 bool isScanning, bool folderHadNoFitFiles, int? truncatedAt, String? error
});




}
/// @nodoc
class __$FitImportStateCopyWithImpl<$Res>
    implements _$FitImportStateCopyWith<$Res> {
  __$FitImportStateCopyWithImpl(this._self, this._then);

  final _FitImportState _self;
  final $Res Function(_FitImportState) _then;

/// Create a copy of FitImportState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isScanning = null,Object? folderHadNoFitFiles = null,Object? truncatedAt = freezed,Object? error = freezed,}) {
  return _then(_FitImportState(
isScanning: null == isScanning ? _self.isScanning : isScanning // ignore: cast_nullable_to_non_nullable
as bool,folderHadNoFitFiles: null == folderHadNoFitFiles ? _self.folderHadNoFitFiles : folderHadNoFitFiles // ignore: cast_nullable_to_non_nullable
as bool,truncatedAt: freezed == truncatedAt ? _self.truncatedAt : truncatedAt // ignore: cast_nullable_to_non_nullable
as int?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

// dart format on
