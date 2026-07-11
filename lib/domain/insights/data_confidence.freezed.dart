// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'data_confidence.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DataConfidence {

 DataConfidenceLevel get level; int get expectedDays; int get trackedDays; int get sampleCount; int get coveragePercent; List<String> get sources; DataSourceConsistency get sourceConsistency; DataValueKind get valueKind; int get manualEntryCount; List<DataConfidenceWarning> get warnings;
/// Create a copy of DataConfidence
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<DataConfidence> get copyWith => _$DataConfidenceCopyWithImpl<DataConfidence>(this as DataConfidence, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DataConfidence&&(identical(other.level, level) || other.level == level)&&(identical(other.expectedDays, expectedDays) || other.expectedDays == expectedDays)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount)&&(identical(other.coveragePercent, coveragePercent) || other.coveragePercent == coveragePercent)&&const DeepCollectionEquality().equals(other.sources, sources)&&(identical(other.sourceConsistency, sourceConsistency) || other.sourceConsistency == sourceConsistency)&&(identical(other.valueKind, valueKind) || other.valueKind == valueKind)&&(identical(other.manualEntryCount, manualEntryCount) || other.manualEntryCount == manualEntryCount)&&const DeepCollectionEquality().equals(other.warnings, warnings));
}


@override
int get hashCode => Object.hash(runtimeType,level,expectedDays,trackedDays,sampleCount,coveragePercent,const DeepCollectionEquality().hash(sources),sourceConsistency,valueKind,manualEntryCount,const DeepCollectionEquality().hash(warnings));

@override
String toString() {
  return 'DataConfidence(level: $level, expectedDays: $expectedDays, trackedDays: $trackedDays, sampleCount: $sampleCount, coveragePercent: $coveragePercent, sources: $sources, sourceConsistency: $sourceConsistency, valueKind: $valueKind, manualEntryCount: $manualEntryCount, warnings: $warnings)';
}


}

/// @nodoc
abstract mixin class $DataConfidenceCopyWith<$Res>  {
  factory $DataConfidenceCopyWith(DataConfidence value, $Res Function(DataConfidence) _then) = _$DataConfidenceCopyWithImpl;
@useResult
$Res call({
 DataConfidenceLevel level, int expectedDays, int trackedDays, int sampleCount, int coveragePercent, List<String> sources, DataSourceConsistency sourceConsistency, DataValueKind valueKind, int manualEntryCount, List<DataConfidenceWarning> warnings
});




}
/// @nodoc
class _$DataConfidenceCopyWithImpl<$Res>
    implements $DataConfidenceCopyWith<$Res> {
  _$DataConfidenceCopyWithImpl(this._self, this._then);

  final DataConfidence _self;
  final $Res Function(DataConfidence) _then;

/// Create a copy of DataConfidence
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? level = null,Object? expectedDays = null,Object? trackedDays = null,Object? sampleCount = null,Object? coveragePercent = null,Object? sources = null,Object? sourceConsistency = null,Object? valueKind = null,Object? manualEntryCount = null,Object? warnings = null,}) {
  return _then(_self.copyWith(
level: null == level ? _self.level : level // ignore: cast_nullable_to_non_nullable
as DataConfidenceLevel,expectedDays: null == expectedDays ? _self.expectedDays : expectedDays // ignore: cast_nullable_to_non_nullable
as int,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,coveragePercent: null == coveragePercent ? _self.coveragePercent : coveragePercent // ignore: cast_nullable_to_non_nullable
as int,sources: null == sources ? _self.sources : sources // ignore: cast_nullable_to_non_nullable
as List<String>,sourceConsistency: null == sourceConsistency ? _self.sourceConsistency : sourceConsistency // ignore: cast_nullable_to_non_nullable
as DataSourceConsistency,valueKind: null == valueKind ? _self.valueKind : valueKind // ignore: cast_nullable_to_non_nullable
as DataValueKind,manualEntryCount: null == manualEntryCount ? _self.manualEntryCount : manualEntryCount // ignore: cast_nullable_to_non_nullable
as int,warnings: null == warnings ? _self.warnings : warnings // ignore: cast_nullable_to_non_nullable
as List<DataConfidenceWarning>,
  ));
}

}


/// Adds pattern-matching-related methods to [DataConfidence].
extension DataConfidencePatterns on DataConfidence {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DataConfidence value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DataConfidence() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DataConfidence value)  $default,){
final _that = this;
switch (_that) {
case _DataConfidence():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DataConfidence value)?  $default,){
final _that = this;
switch (_that) {
case _DataConfidence() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DataConfidenceLevel level,  int expectedDays,  int trackedDays,  int sampleCount,  int coveragePercent,  List<String> sources,  DataSourceConsistency sourceConsistency,  DataValueKind valueKind,  int manualEntryCount,  List<DataConfidenceWarning> warnings)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DataConfidence() when $default != null:
return $default(_that.level,_that.expectedDays,_that.trackedDays,_that.sampleCount,_that.coveragePercent,_that.sources,_that.sourceConsistency,_that.valueKind,_that.manualEntryCount,_that.warnings);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DataConfidenceLevel level,  int expectedDays,  int trackedDays,  int sampleCount,  int coveragePercent,  List<String> sources,  DataSourceConsistency sourceConsistency,  DataValueKind valueKind,  int manualEntryCount,  List<DataConfidenceWarning> warnings)  $default,) {final _that = this;
switch (_that) {
case _DataConfidence():
return $default(_that.level,_that.expectedDays,_that.trackedDays,_that.sampleCount,_that.coveragePercent,_that.sources,_that.sourceConsistency,_that.valueKind,_that.manualEntryCount,_that.warnings);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DataConfidenceLevel level,  int expectedDays,  int trackedDays,  int sampleCount,  int coveragePercent,  List<String> sources,  DataSourceConsistency sourceConsistency,  DataValueKind valueKind,  int manualEntryCount,  List<DataConfidenceWarning> warnings)?  $default,) {final _that = this;
switch (_that) {
case _DataConfidence() when $default != null:
return $default(_that.level,_that.expectedDays,_that.trackedDays,_that.sampleCount,_that.coveragePercent,_that.sources,_that.sourceConsistency,_that.valueKind,_that.manualEntryCount,_that.warnings);case _:
  return null;

}
}

}

/// @nodoc


class _DataConfidence implements DataConfidence {
  const _DataConfidence({required this.level, required this.expectedDays, required this.trackedDays, required this.sampleCount, required this.coveragePercent, required final  List<String> sources, required this.sourceConsistency, required this.valueKind, required this.manualEntryCount, required final  List<DataConfidenceWarning> warnings}): _sources = sources,_warnings = warnings;
  

@override final  DataConfidenceLevel level;
@override final  int expectedDays;
@override final  int trackedDays;
@override final  int sampleCount;
@override final  int coveragePercent;
 final  List<String> _sources;
@override List<String> get sources {
  if (_sources is EqualUnmodifiableListView) return _sources;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sources);
}

@override final  DataSourceConsistency sourceConsistency;
@override final  DataValueKind valueKind;
@override final  int manualEntryCount;
 final  List<DataConfidenceWarning> _warnings;
@override List<DataConfidenceWarning> get warnings {
  if (_warnings is EqualUnmodifiableListView) return _warnings;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_warnings);
}


/// Create a copy of DataConfidence
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DataConfidenceCopyWith<_DataConfidence> get copyWith => __$DataConfidenceCopyWithImpl<_DataConfidence>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DataConfidence&&(identical(other.level, level) || other.level == level)&&(identical(other.expectedDays, expectedDays) || other.expectedDays == expectedDays)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount)&&(identical(other.coveragePercent, coveragePercent) || other.coveragePercent == coveragePercent)&&const DeepCollectionEquality().equals(other._sources, _sources)&&(identical(other.sourceConsistency, sourceConsistency) || other.sourceConsistency == sourceConsistency)&&(identical(other.valueKind, valueKind) || other.valueKind == valueKind)&&(identical(other.manualEntryCount, manualEntryCount) || other.manualEntryCount == manualEntryCount)&&const DeepCollectionEquality().equals(other._warnings, _warnings));
}


@override
int get hashCode => Object.hash(runtimeType,level,expectedDays,trackedDays,sampleCount,coveragePercent,const DeepCollectionEquality().hash(_sources),sourceConsistency,valueKind,manualEntryCount,const DeepCollectionEquality().hash(_warnings));

@override
String toString() {
  return 'DataConfidence(level: $level, expectedDays: $expectedDays, trackedDays: $trackedDays, sampleCount: $sampleCount, coveragePercent: $coveragePercent, sources: $sources, sourceConsistency: $sourceConsistency, valueKind: $valueKind, manualEntryCount: $manualEntryCount, warnings: $warnings)';
}


}

/// @nodoc
abstract mixin class _$DataConfidenceCopyWith<$Res> implements $DataConfidenceCopyWith<$Res> {
  factory _$DataConfidenceCopyWith(_DataConfidence value, $Res Function(_DataConfidence) _then) = __$DataConfidenceCopyWithImpl;
@override @useResult
$Res call({
 DataConfidenceLevel level, int expectedDays, int trackedDays, int sampleCount, int coveragePercent, List<String> sources, DataSourceConsistency sourceConsistency, DataValueKind valueKind, int manualEntryCount, List<DataConfidenceWarning> warnings
});




}
/// @nodoc
class __$DataConfidenceCopyWithImpl<$Res>
    implements _$DataConfidenceCopyWith<$Res> {
  __$DataConfidenceCopyWithImpl(this._self, this._then);

  final _DataConfidence _self;
  final $Res Function(_DataConfidence) _then;

/// Create a copy of DataConfidence
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? level = null,Object? expectedDays = null,Object? trackedDays = null,Object? sampleCount = null,Object? coveragePercent = null,Object? sources = null,Object? sourceConsistency = null,Object? valueKind = null,Object? manualEntryCount = null,Object? warnings = null,}) {
  return _then(_DataConfidence(
level: null == level ? _self.level : level // ignore: cast_nullable_to_non_nullable
as DataConfidenceLevel,expectedDays: null == expectedDays ? _self.expectedDays : expectedDays // ignore: cast_nullable_to_non_nullable
as int,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,coveragePercent: null == coveragePercent ? _self.coveragePercent : coveragePercent // ignore: cast_nullable_to_non_nullable
as int,sources: null == sources ? _self._sources : sources // ignore: cast_nullable_to_non_nullable
as List<String>,sourceConsistency: null == sourceConsistency ? _self.sourceConsistency : sourceConsistency // ignore: cast_nullable_to_non_nullable
as DataSourceConsistency,valueKind: null == valueKind ? _self.valueKind : valueKind // ignore: cast_nullable_to_non_nullable
as DataValueKind,manualEntryCount: null == manualEntryCount ? _self.manualEntryCount : manualEntryCount // ignore: cast_nullable_to_non_nullable
as int,warnings: null == warnings ? _self._warnings : warnings // ignore: cast_nullable_to_non_nullable
as List<DataConfidenceWarning>,
  ));
}


}

// dart format on
