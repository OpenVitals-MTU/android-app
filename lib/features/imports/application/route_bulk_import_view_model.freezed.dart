// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'route_bulk_import_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RouteBulkImportProgress {

 int get totalFiles; int get importedFiles; int get failedFiles; int get currentFileIndex;
/// Create a copy of RouteBulkImportProgress
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RouteBulkImportProgressCopyWith<RouteBulkImportProgress> get copyWith => _$RouteBulkImportProgressCopyWithImpl<RouteBulkImportProgress>(this as RouteBulkImportProgress, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RouteBulkImportProgress&&(identical(other.totalFiles, totalFiles) || other.totalFiles == totalFiles)&&(identical(other.importedFiles, importedFiles) || other.importedFiles == importedFiles)&&(identical(other.failedFiles, failedFiles) || other.failedFiles == failedFiles)&&(identical(other.currentFileIndex, currentFileIndex) || other.currentFileIndex == currentFileIndex));
}


@override
int get hashCode => Object.hash(runtimeType,totalFiles,importedFiles,failedFiles,currentFileIndex);

@override
String toString() {
  return 'RouteBulkImportProgress(totalFiles: $totalFiles, importedFiles: $importedFiles, failedFiles: $failedFiles, currentFileIndex: $currentFileIndex)';
}


}

/// @nodoc
abstract mixin class $RouteBulkImportProgressCopyWith<$Res>  {
  factory $RouteBulkImportProgressCopyWith(RouteBulkImportProgress value, $Res Function(RouteBulkImportProgress) _then) = _$RouteBulkImportProgressCopyWithImpl;
@useResult
$Res call({
 int totalFiles, int importedFiles, int failedFiles, int currentFileIndex
});




}
/// @nodoc
class _$RouteBulkImportProgressCopyWithImpl<$Res>
    implements $RouteBulkImportProgressCopyWith<$Res> {
  _$RouteBulkImportProgressCopyWithImpl(this._self, this._then);

  final RouteBulkImportProgress _self;
  final $Res Function(RouteBulkImportProgress) _then;

/// Create a copy of RouteBulkImportProgress
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? totalFiles = null,Object? importedFiles = null,Object? failedFiles = null,Object? currentFileIndex = null,}) {
  return _then(_self.copyWith(
totalFiles: null == totalFiles ? _self.totalFiles : totalFiles // ignore: cast_nullable_to_non_nullable
as int,importedFiles: null == importedFiles ? _self.importedFiles : importedFiles // ignore: cast_nullable_to_non_nullable
as int,failedFiles: null == failedFiles ? _self.failedFiles : failedFiles // ignore: cast_nullable_to_non_nullable
as int,currentFileIndex: null == currentFileIndex ? _self.currentFileIndex : currentFileIndex // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [RouteBulkImportProgress].
extension RouteBulkImportProgressPatterns on RouteBulkImportProgress {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RouteBulkImportProgress value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RouteBulkImportProgress() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RouteBulkImportProgress value)  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportProgress():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RouteBulkImportProgress value)?  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportProgress() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int totalFiles,  int importedFiles,  int failedFiles,  int currentFileIndex)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RouteBulkImportProgress() when $default != null:
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles,_that.currentFileIndex);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int totalFiles,  int importedFiles,  int failedFiles,  int currentFileIndex)  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportProgress():
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles,_that.currentFileIndex);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int totalFiles,  int importedFiles,  int failedFiles,  int currentFileIndex)?  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportProgress() when $default != null:
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles,_that.currentFileIndex);case _:
  return null;

}
}

}

/// @nodoc


class _RouteBulkImportProgress implements RouteBulkImportProgress {
  const _RouteBulkImportProgress({required this.totalFiles, this.importedFiles = 0, this.failedFiles = 0, this.currentFileIndex = 0});
  

@override final  int totalFiles;
@override@JsonKey() final  int importedFiles;
@override@JsonKey() final  int failedFiles;
@override@JsonKey() final  int currentFileIndex;

/// Create a copy of RouteBulkImportProgress
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RouteBulkImportProgressCopyWith<_RouteBulkImportProgress> get copyWith => __$RouteBulkImportProgressCopyWithImpl<_RouteBulkImportProgress>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RouteBulkImportProgress&&(identical(other.totalFiles, totalFiles) || other.totalFiles == totalFiles)&&(identical(other.importedFiles, importedFiles) || other.importedFiles == importedFiles)&&(identical(other.failedFiles, failedFiles) || other.failedFiles == failedFiles)&&(identical(other.currentFileIndex, currentFileIndex) || other.currentFileIndex == currentFileIndex));
}


@override
int get hashCode => Object.hash(runtimeType,totalFiles,importedFiles,failedFiles,currentFileIndex);

@override
String toString() {
  return 'RouteBulkImportProgress(totalFiles: $totalFiles, importedFiles: $importedFiles, failedFiles: $failedFiles, currentFileIndex: $currentFileIndex)';
}


}

/// @nodoc
abstract mixin class _$RouteBulkImportProgressCopyWith<$Res> implements $RouteBulkImportProgressCopyWith<$Res> {
  factory _$RouteBulkImportProgressCopyWith(_RouteBulkImportProgress value, $Res Function(_RouteBulkImportProgress) _then) = __$RouteBulkImportProgressCopyWithImpl;
@override @useResult
$Res call({
 int totalFiles, int importedFiles, int failedFiles, int currentFileIndex
});




}
/// @nodoc
class __$RouteBulkImportProgressCopyWithImpl<$Res>
    implements _$RouteBulkImportProgressCopyWith<$Res> {
  __$RouteBulkImportProgressCopyWithImpl(this._self, this._then);

  final _RouteBulkImportProgress _self;
  final $Res Function(_RouteBulkImportProgress) _then;

/// Create a copy of RouteBulkImportProgress
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? totalFiles = null,Object? importedFiles = null,Object? failedFiles = null,Object? currentFileIndex = null,}) {
  return _then(_RouteBulkImportProgress(
totalFiles: null == totalFiles ? _self.totalFiles : totalFiles // ignore: cast_nullable_to_non_nullable
as int,importedFiles: null == importedFiles ? _self.importedFiles : importedFiles // ignore: cast_nullable_to_non_nullable
as int,failedFiles: null == failedFiles ? _self.failedFiles : failedFiles // ignore: cast_nullable_to_non_nullable
as int,currentFileIndex: null == currentFileIndex ? _self.currentFileIndex : currentFileIndex // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$RouteBulkImportResult {

 int get totalFiles; int get importedFiles; int get failedFiles;
/// Create a copy of RouteBulkImportResult
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RouteBulkImportResultCopyWith<RouteBulkImportResult> get copyWith => _$RouteBulkImportResultCopyWithImpl<RouteBulkImportResult>(this as RouteBulkImportResult, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RouteBulkImportResult&&(identical(other.totalFiles, totalFiles) || other.totalFiles == totalFiles)&&(identical(other.importedFiles, importedFiles) || other.importedFiles == importedFiles)&&(identical(other.failedFiles, failedFiles) || other.failedFiles == failedFiles));
}


@override
int get hashCode => Object.hash(runtimeType,totalFiles,importedFiles,failedFiles);

@override
String toString() {
  return 'RouteBulkImportResult(totalFiles: $totalFiles, importedFiles: $importedFiles, failedFiles: $failedFiles)';
}


}

/// @nodoc
abstract mixin class $RouteBulkImportResultCopyWith<$Res>  {
  factory $RouteBulkImportResultCopyWith(RouteBulkImportResult value, $Res Function(RouteBulkImportResult) _then) = _$RouteBulkImportResultCopyWithImpl;
@useResult
$Res call({
 int totalFiles, int importedFiles, int failedFiles
});




}
/// @nodoc
class _$RouteBulkImportResultCopyWithImpl<$Res>
    implements $RouteBulkImportResultCopyWith<$Res> {
  _$RouteBulkImportResultCopyWithImpl(this._self, this._then);

  final RouteBulkImportResult _self;
  final $Res Function(RouteBulkImportResult) _then;

/// Create a copy of RouteBulkImportResult
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? totalFiles = null,Object? importedFiles = null,Object? failedFiles = null,}) {
  return _then(_self.copyWith(
totalFiles: null == totalFiles ? _self.totalFiles : totalFiles // ignore: cast_nullable_to_non_nullable
as int,importedFiles: null == importedFiles ? _self.importedFiles : importedFiles // ignore: cast_nullable_to_non_nullable
as int,failedFiles: null == failedFiles ? _self.failedFiles : failedFiles // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [RouteBulkImportResult].
extension RouteBulkImportResultPatterns on RouteBulkImportResult {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RouteBulkImportResult value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RouteBulkImportResult() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RouteBulkImportResult value)  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportResult():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RouteBulkImportResult value)?  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportResult() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int totalFiles,  int importedFiles,  int failedFiles)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RouteBulkImportResult() when $default != null:
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int totalFiles,  int importedFiles,  int failedFiles)  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportResult():
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int totalFiles,  int importedFiles,  int failedFiles)?  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportResult() when $default != null:
return $default(_that.totalFiles,_that.importedFiles,_that.failedFiles);case _:
  return null;

}
}

}

/// @nodoc


class _RouteBulkImportResult implements RouteBulkImportResult {
  const _RouteBulkImportResult({required this.totalFiles, required this.importedFiles, required this.failedFiles});
  

@override final  int totalFiles;
@override final  int importedFiles;
@override final  int failedFiles;

/// Create a copy of RouteBulkImportResult
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RouteBulkImportResultCopyWith<_RouteBulkImportResult> get copyWith => __$RouteBulkImportResultCopyWithImpl<_RouteBulkImportResult>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RouteBulkImportResult&&(identical(other.totalFiles, totalFiles) || other.totalFiles == totalFiles)&&(identical(other.importedFiles, importedFiles) || other.importedFiles == importedFiles)&&(identical(other.failedFiles, failedFiles) || other.failedFiles == failedFiles));
}


@override
int get hashCode => Object.hash(runtimeType,totalFiles,importedFiles,failedFiles);

@override
String toString() {
  return 'RouteBulkImportResult(totalFiles: $totalFiles, importedFiles: $importedFiles, failedFiles: $failedFiles)';
}


}

/// @nodoc
abstract mixin class _$RouteBulkImportResultCopyWith<$Res> implements $RouteBulkImportResultCopyWith<$Res> {
  factory _$RouteBulkImportResultCopyWith(_RouteBulkImportResult value, $Res Function(_RouteBulkImportResult) _then) = __$RouteBulkImportResultCopyWithImpl;
@override @useResult
$Res call({
 int totalFiles, int importedFiles, int failedFiles
});




}
/// @nodoc
class __$RouteBulkImportResultCopyWithImpl<$Res>
    implements _$RouteBulkImportResultCopyWith<$Res> {
  __$RouteBulkImportResultCopyWithImpl(this._self, this._then);

  final _RouteBulkImportResult _self;
  final $Res Function(_RouteBulkImportResult) _then;

/// Create a copy of RouteBulkImportResult
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? totalFiles = null,Object? importedFiles = null,Object? failedFiles = null,}) {
  return _then(_RouteBulkImportResult(
totalFiles: null == totalFiles ? _self.totalFiles : totalFiles // ignore: cast_nullable_to_non_nullable
as int,importedFiles: null == importedFiles ? _self.importedFiles : importedFiles // ignore: cast_nullable_to_non_nullable
as int,failedFiles: null == failedFiles ? _self.failedFiles : failedFiles // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$RouteBulkImportState {

 bool get isImporting; RouteBulkImportProgress? get progress; RouteBulkImportResult? get result;/// The *last* file's failure, rendered by the Settings card through
/// `l10n.settingsRouteImportError`. A String, not a [ScreenError], because
/// one failed file in a tolerated batch is a line of feedback, not the
/// screen's error state.
 String? get error;
/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RouteBulkImportStateCopyWith<RouteBulkImportState> get copyWith => _$RouteBulkImportStateCopyWithImpl<RouteBulkImportState>(this as RouteBulkImportState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RouteBulkImportState&&(identical(other.isImporting, isImporting) || other.isImporting == isImporting)&&(identical(other.progress, progress) || other.progress == progress)&&(identical(other.result, result) || other.result == result)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isImporting,progress,result,error);

@override
String toString() {
  return 'RouteBulkImportState(isImporting: $isImporting, progress: $progress, result: $result, error: $error)';
}


}

/// @nodoc
abstract mixin class $RouteBulkImportStateCopyWith<$Res>  {
  factory $RouteBulkImportStateCopyWith(RouteBulkImportState value, $Res Function(RouteBulkImportState) _then) = _$RouteBulkImportStateCopyWithImpl;
@useResult
$Res call({
 bool isImporting, RouteBulkImportProgress? progress, RouteBulkImportResult? result, String? error
});


$RouteBulkImportProgressCopyWith<$Res>? get progress;$RouteBulkImportResultCopyWith<$Res>? get result;

}
/// @nodoc
class _$RouteBulkImportStateCopyWithImpl<$Res>
    implements $RouteBulkImportStateCopyWith<$Res> {
  _$RouteBulkImportStateCopyWithImpl(this._self, this._then);

  final RouteBulkImportState _self;
  final $Res Function(RouteBulkImportState) _then;

/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isImporting = null,Object? progress = freezed,Object? result = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
isImporting: null == isImporting ? _self.isImporting : isImporting // ignore: cast_nullable_to_non_nullable
as bool,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as RouteBulkImportProgress?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as RouteBulkImportResult?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}
/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RouteBulkImportProgressCopyWith<$Res>? get progress {
    if (_self.progress == null) {
    return null;
  }

  return $RouteBulkImportProgressCopyWith<$Res>(_self.progress!, (value) {
    return _then(_self.copyWith(progress: value));
  });
}/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RouteBulkImportResultCopyWith<$Res>? get result {
    if (_self.result == null) {
    return null;
  }

  return $RouteBulkImportResultCopyWith<$Res>(_self.result!, (value) {
    return _then(_self.copyWith(result: value));
  });
}
}


/// Adds pattern-matching-related methods to [RouteBulkImportState].
extension RouteBulkImportStatePatterns on RouteBulkImportState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RouteBulkImportState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RouteBulkImportState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RouteBulkImportState value)  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RouteBulkImportState value)?  $default,){
final _that = this;
switch (_that) {
case _RouteBulkImportState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isImporting,  RouteBulkImportProgress? progress,  RouteBulkImportResult? result,  String? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RouteBulkImportState() when $default != null:
return $default(_that.isImporting,_that.progress,_that.result,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isImporting,  RouteBulkImportProgress? progress,  RouteBulkImportResult? result,  String? error)  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportState():
return $default(_that.isImporting,_that.progress,_that.result,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isImporting,  RouteBulkImportProgress? progress,  RouteBulkImportResult? result,  String? error)?  $default,) {final _that = this;
switch (_that) {
case _RouteBulkImportState() when $default != null:
return $default(_that.isImporting,_that.progress,_that.result,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _RouteBulkImportState implements RouteBulkImportState {
  const _RouteBulkImportState({this.isImporting = false, this.progress, this.result, this.error});
  

@override@JsonKey() final  bool isImporting;
@override final  RouteBulkImportProgress? progress;
@override final  RouteBulkImportResult? result;
/// The *last* file's failure, rendered by the Settings card through
/// `l10n.settingsRouteImportError`. A String, not a [ScreenError], because
/// one failed file in a tolerated batch is a line of feedback, not the
/// screen's error state.
@override final  String? error;

/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RouteBulkImportStateCopyWith<_RouteBulkImportState> get copyWith => __$RouteBulkImportStateCopyWithImpl<_RouteBulkImportState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RouteBulkImportState&&(identical(other.isImporting, isImporting) || other.isImporting == isImporting)&&(identical(other.progress, progress) || other.progress == progress)&&(identical(other.result, result) || other.result == result)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isImporting,progress,result,error);

@override
String toString() {
  return 'RouteBulkImportState(isImporting: $isImporting, progress: $progress, result: $result, error: $error)';
}


}

/// @nodoc
abstract mixin class _$RouteBulkImportStateCopyWith<$Res> implements $RouteBulkImportStateCopyWith<$Res> {
  factory _$RouteBulkImportStateCopyWith(_RouteBulkImportState value, $Res Function(_RouteBulkImportState) _then) = __$RouteBulkImportStateCopyWithImpl;
@override @useResult
$Res call({
 bool isImporting, RouteBulkImportProgress? progress, RouteBulkImportResult? result, String? error
});


@override $RouteBulkImportProgressCopyWith<$Res>? get progress;@override $RouteBulkImportResultCopyWith<$Res>? get result;

}
/// @nodoc
class __$RouteBulkImportStateCopyWithImpl<$Res>
    implements _$RouteBulkImportStateCopyWith<$Res> {
  __$RouteBulkImportStateCopyWithImpl(this._self, this._then);

  final _RouteBulkImportState _self;
  final $Res Function(_RouteBulkImportState) _then;

/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isImporting = null,Object? progress = freezed,Object? result = freezed,Object? error = freezed,}) {
  return _then(_RouteBulkImportState(
isImporting: null == isImporting ? _self.isImporting : isImporting // ignore: cast_nullable_to_non_nullable
as bool,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as RouteBulkImportProgress?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as RouteBulkImportResult?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RouteBulkImportProgressCopyWith<$Res>? get progress {
    if (_self.progress == null) {
    return null;
  }

  return $RouteBulkImportProgressCopyWith<$Res>(_self.progress!, (value) {
    return _then(_self.copyWith(progress: value));
  });
}/// Create a copy of RouteBulkImportState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RouteBulkImportResultCopyWith<$Res>? get result {
    if (_self.result == null) {
    return null;
  }

  return $RouteBulkImportResultCopyWith<$Res>(_self.result!, (value) {
    return _then(_self.copyWith(result: value));
  });
}
}

// dart format on
