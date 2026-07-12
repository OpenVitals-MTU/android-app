// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'apple_health_import_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$AppleHealthImportUiState {

 bool get isAnalyzing; bool get isImporting; AppleHealthImportProgress? get analysisProgress; AppleHealthImportAnalysisResult? get analysis; Set<AppleHealthImportCategory> get selectedCategories; AppleHealthImportProgress? get progress; AppleHealthImportResult? get result; String? get error; bool get permissionDenied;
/// Create a copy of AppleHealthImportUiState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AppleHealthImportUiStateCopyWith<AppleHealthImportUiState> get copyWith => _$AppleHealthImportUiStateCopyWithImpl<AppleHealthImportUiState>(this as AppleHealthImportUiState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AppleHealthImportUiState&&(identical(other.isAnalyzing, isAnalyzing) || other.isAnalyzing == isAnalyzing)&&(identical(other.isImporting, isImporting) || other.isImporting == isImporting)&&(identical(other.analysisProgress, analysisProgress) || other.analysisProgress == analysisProgress)&&(identical(other.analysis, analysis) || other.analysis == analysis)&&const DeepCollectionEquality().equals(other.selectedCategories, selectedCategories)&&(identical(other.progress, progress) || other.progress == progress)&&(identical(other.result, result) || other.result == result)&&(identical(other.error, error) || other.error == error)&&(identical(other.permissionDenied, permissionDenied) || other.permissionDenied == permissionDenied));
}


@override
int get hashCode => Object.hash(runtimeType,isAnalyzing,isImporting,analysisProgress,analysis,const DeepCollectionEquality().hash(selectedCategories),progress,result,error,permissionDenied);

@override
String toString() {
  return 'AppleHealthImportUiState(isAnalyzing: $isAnalyzing, isImporting: $isImporting, analysisProgress: $analysisProgress, analysis: $analysis, selectedCategories: $selectedCategories, progress: $progress, result: $result, error: $error, permissionDenied: $permissionDenied)';
}


}

/// @nodoc
abstract mixin class $AppleHealthImportUiStateCopyWith<$Res>  {
  factory $AppleHealthImportUiStateCopyWith(AppleHealthImportUiState value, $Res Function(AppleHealthImportUiState) _then) = _$AppleHealthImportUiStateCopyWithImpl;
@useResult
$Res call({
 bool isAnalyzing, bool isImporting, AppleHealthImportProgress? analysisProgress, AppleHealthImportAnalysisResult? analysis, Set<AppleHealthImportCategory> selectedCategories, AppleHealthImportProgress? progress, AppleHealthImportResult? result, String? error, bool permissionDenied
});




}
/// @nodoc
class _$AppleHealthImportUiStateCopyWithImpl<$Res>
    implements $AppleHealthImportUiStateCopyWith<$Res> {
  _$AppleHealthImportUiStateCopyWithImpl(this._self, this._then);

  final AppleHealthImportUiState _self;
  final $Res Function(AppleHealthImportUiState) _then;

/// Create a copy of AppleHealthImportUiState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isAnalyzing = null,Object? isImporting = null,Object? analysisProgress = freezed,Object? analysis = freezed,Object? selectedCategories = null,Object? progress = freezed,Object? result = freezed,Object? error = freezed,Object? permissionDenied = null,}) {
  return _then(_self.copyWith(
isAnalyzing: null == isAnalyzing ? _self.isAnalyzing : isAnalyzing // ignore: cast_nullable_to_non_nullable
as bool,isImporting: null == isImporting ? _self.isImporting : isImporting // ignore: cast_nullable_to_non_nullable
as bool,analysisProgress: freezed == analysisProgress ? _self.analysisProgress : analysisProgress // ignore: cast_nullable_to_non_nullable
as AppleHealthImportProgress?,analysis: freezed == analysis ? _self.analysis : analysis // ignore: cast_nullable_to_non_nullable
as AppleHealthImportAnalysisResult?,selectedCategories: null == selectedCategories ? _self.selectedCategories : selectedCategories // ignore: cast_nullable_to_non_nullable
as Set<AppleHealthImportCategory>,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as AppleHealthImportProgress?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as AppleHealthImportResult?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,permissionDenied: null == permissionDenied ? _self.permissionDenied : permissionDenied // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [AppleHealthImportUiState].
extension AppleHealthImportUiStatePatterns on AppleHealthImportUiState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AppleHealthImportUiState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AppleHealthImportUiState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AppleHealthImportUiState value)  $default,){
final _that = this;
switch (_that) {
case _AppleHealthImportUiState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AppleHealthImportUiState value)?  $default,){
final _that = this;
switch (_that) {
case _AppleHealthImportUiState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isAnalyzing,  bool isImporting,  AppleHealthImportProgress? analysisProgress,  AppleHealthImportAnalysisResult? analysis,  Set<AppleHealthImportCategory> selectedCategories,  AppleHealthImportProgress? progress,  AppleHealthImportResult? result,  String? error,  bool permissionDenied)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AppleHealthImportUiState() when $default != null:
return $default(_that.isAnalyzing,_that.isImporting,_that.analysisProgress,_that.analysis,_that.selectedCategories,_that.progress,_that.result,_that.error,_that.permissionDenied);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isAnalyzing,  bool isImporting,  AppleHealthImportProgress? analysisProgress,  AppleHealthImportAnalysisResult? analysis,  Set<AppleHealthImportCategory> selectedCategories,  AppleHealthImportProgress? progress,  AppleHealthImportResult? result,  String? error,  bool permissionDenied)  $default,) {final _that = this;
switch (_that) {
case _AppleHealthImportUiState():
return $default(_that.isAnalyzing,_that.isImporting,_that.analysisProgress,_that.analysis,_that.selectedCategories,_that.progress,_that.result,_that.error,_that.permissionDenied);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isAnalyzing,  bool isImporting,  AppleHealthImportProgress? analysisProgress,  AppleHealthImportAnalysisResult? analysis,  Set<AppleHealthImportCategory> selectedCategories,  AppleHealthImportProgress? progress,  AppleHealthImportResult? result,  String? error,  bool permissionDenied)?  $default,) {final _that = this;
switch (_that) {
case _AppleHealthImportUiState() when $default != null:
return $default(_that.isAnalyzing,_that.isImporting,_that.analysisProgress,_that.analysis,_that.selectedCategories,_that.progress,_that.result,_that.error,_that.permissionDenied);case _:
  return null;

}
}

}

/// @nodoc


class _AppleHealthImportUiState extends AppleHealthImportUiState {
  const _AppleHealthImportUiState({this.isAnalyzing = false, this.isImporting = false, this.analysisProgress, this.analysis, final  Set<AppleHealthImportCategory> selectedCategories = const <AppleHealthImportCategory>{}, this.progress, this.result, this.error, this.permissionDenied = false}): _selectedCategories = selectedCategories,super._();
  

@override@JsonKey() final  bool isAnalyzing;
@override@JsonKey() final  bool isImporting;
@override final  AppleHealthImportProgress? analysisProgress;
@override final  AppleHealthImportAnalysisResult? analysis;
 final  Set<AppleHealthImportCategory> _selectedCategories;
@override@JsonKey() Set<AppleHealthImportCategory> get selectedCategories {
  if (_selectedCategories is EqualUnmodifiableSetView) return _selectedCategories;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_selectedCategories);
}

@override final  AppleHealthImportProgress? progress;
@override final  AppleHealthImportResult? result;
@override final  String? error;
@override@JsonKey() final  bool permissionDenied;

/// Create a copy of AppleHealthImportUiState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AppleHealthImportUiStateCopyWith<_AppleHealthImportUiState> get copyWith => __$AppleHealthImportUiStateCopyWithImpl<_AppleHealthImportUiState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AppleHealthImportUiState&&(identical(other.isAnalyzing, isAnalyzing) || other.isAnalyzing == isAnalyzing)&&(identical(other.isImporting, isImporting) || other.isImporting == isImporting)&&(identical(other.analysisProgress, analysisProgress) || other.analysisProgress == analysisProgress)&&(identical(other.analysis, analysis) || other.analysis == analysis)&&const DeepCollectionEquality().equals(other._selectedCategories, _selectedCategories)&&(identical(other.progress, progress) || other.progress == progress)&&(identical(other.result, result) || other.result == result)&&(identical(other.error, error) || other.error == error)&&(identical(other.permissionDenied, permissionDenied) || other.permissionDenied == permissionDenied));
}


@override
int get hashCode => Object.hash(runtimeType,isAnalyzing,isImporting,analysisProgress,analysis,const DeepCollectionEquality().hash(_selectedCategories),progress,result,error,permissionDenied);

@override
String toString() {
  return 'AppleHealthImportUiState(isAnalyzing: $isAnalyzing, isImporting: $isImporting, analysisProgress: $analysisProgress, analysis: $analysis, selectedCategories: $selectedCategories, progress: $progress, result: $result, error: $error, permissionDenied: $permissionDenied)';
}


}

/// @nodoc
abstract mixin class _$AppleHealthImportUiStateCopyWith<$Res> implements $AppleHealthImportUiStateCopyWith<$Res> {
  factory _$AppleHealthImportUiStateCopyWith(_AppleHealthImportUiState value, $Res Function(_AppleHealthImportUiState) _then) = __$AppleHealthImportUiStateCopyWithImpl;
@override @useResult
$Res call({
 bool isAnalyzing, bool isImporting, AppleHealthImportProgress? analysisProgress, AppleHealthImportAnalysisResult? analysis, Set<AppleHealthImportCategory> selectedCategories, AppleHealthImportProgress? progress, AppleHealthImportResult? result, String? error, bool permissionDenied
});




}
/// @nodoc
class __$AppleHealthImportUiStateCopyWithImpl<$Res>
    implements _$AppleHealthImportUiStateCopyWith<$Res> {
  __$AppleHealthImportUiStateCopyWithImpl(this._self, this._then);

  final _AppleHealthImportUiState _self;
  final $Res Function(_AppleHealthImportUiState) _then;

/// Create a copy of AppleHealthImportUiState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isAnalyzing = null,Object? isImporting = null,Object? analysisProgress = freezed,Object? analysis = freezed,Object? selectedCategories = null,Object? progress = freezed,Object? result = freezed,Object? error = freezed,Object? permissionDenied = null,}) {
  return _then(_AppleHealthImportUiState(
isAnalyzing: null == isAnalyzing ? _self.isAnalyzing : isAnalyzing // ignore: cast_nullable_to_non_nullable
as bool,isImporting: null == isImporting ? _self.isImporting : isImporting // ignore: cast_nullable_to_non_nullable
as bool,analysisProgress: freezed == analysisProgress ? _self.analysisProgress : analysisProgress // ignore: cast_nullable_to_non_nullable
as AppleHealthImportProgress?,analysis: freezed == analysis ? _self.analysis : analysis // ignore: cast_nullable_to_non_nullable
as AppleHealthImportAnalysisResult?,selectedCategories: null == selectedCategories ? _self._selectedCategories : selectedCategories // ignore: cast_nullable_to_non_nullable
as Set<AppleHealthImportCategory>,progress: freezed == progress ? _self.progress : progress // ignore: cast_nullable_to_non_nullable
as AppleHealthImportProgress?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as AppleHealthImportResult?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as String?,permissionDenied: null == permissionDenied ? _self.permissionDenied : permissionDenied // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
