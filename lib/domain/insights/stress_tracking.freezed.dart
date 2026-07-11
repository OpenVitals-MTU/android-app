// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'stress_tracking.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$PhysiologicalStressEstimate {

 PhysiologicalStressLevel get level; String get label; int? get score; String get summary; String get detail; PhysiologicalStressConfidence get confidence; String get confidenceReason; int? get hrvPercentFromBaseline; int? get restingHeartRateDeltaBpm; int? get averageHeartRateDeltaFromRestingBpm; bool get hasWorkoutInfluence; List<String> get contributingFactors; List<String> get dataCoverage; List<String> get caveats;
/// Create a copy of PhysiologicalStressEstimate
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PhysiologicalStressEstimateCopyWith<PhysiologicalStressEstimate> get copyWith => _$PhysiologicalStressEstimateCopyWithImpl<PhysiologicalStressEstimate>(this as PhysiologicalStressEstimate, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PhysiologicalStressEstimate&&(identical(other.level, level) || other.level == level)&&(identical(other.label, label) || other.label == label)&&(identical(other.score, score) || other.score == score)&&(identical(other.summary, summary) || other.summary == summary)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.hrvPercentFromBaseline, hrvPercentFromBaseline) || other.hrvPercentFromBaseline == hrvPercentFromBaseline)&&(identical(other.restingHeartRateDeltaBpm, restingHeartRateDeltaBpm) || other.restingHeartRateDeltaBpm == restingHeartRateDeltaBpm)&&(identical(other.averageHeartRateDeltaFromRestingBpm, averageHeartRateDeltaFromRestingBpm) || other.averageHeartRateDeltaFromRestingBpm == averageHeartRateDeltaFromRestingBpm)&&(identical(other.hasWorkoutInfluence, hasWorkoutInfluence) || other.hasWorkoutInfluence == hasWorkoutInfluence)&&const DeepCollectionEquality().equals(other.contributingFactors, contributingFactors)&&const DeepCollectionEquality().equals(other.dataCoverage, dataCoverage)&&const DeepCollectionEquality().equals(other.caveats, caveats));
}


@override
int get hashCode => Object.hash(runtimeType,level,label,score,summary,detail,confidence,confidenceReason,hrvPercentFromBaseline,restingHeartRateDeltaBpm,averageHeartRateDeltaFromRestingBpm,hasWorkoutInfluence,const DeepCollectionEquality().hash(contributingFactors),const DeepCollectionEquality().hash(dataCoverage),const DeepCollectionEquality().hash(caveats));

@override
String toString() {
  return 'PhysiologicalStressEstimate(level: $level, label: $label, score: $score, summary: $summary, detail: $detail, confidence: $confidence, confidenceReason: $confidenceReason, hrvPercentFromBaseline: $hrvPercentFromBaseline, restingHeartRateDeltaBpm: $restingHeartRateDeltaBpm, averageHeartRateDeltaFromRestingBpm: $averageHeartRateDeltaFromRestingBpm, hasWorkoutInfluence: $hasWorkoutInfluence, contributingFactors: $contributingFactors, dataCoverage: $dataCoverage, caveats: $caveats)';
}


}

/// @nodoc
abstract mixin class $PhysiologicalStressEstimateCopyWith<$Res>  {
  factory $PhysiologicalStressEstimateCopyWith(PhysiologicalStressEstimate value, $Res Function(PhysiologicalStressEstimate) _then) = _$PhysiologicalStressEstimateCopyWithImpl;
@useResult
$Res call({
 PhysiologicalStressLevel level, String label, int? score, String summary, String detail, PhysiologicalStressConfidence confidence, String confidenceReason, int? hrvPercentFromBaseline, int? restingHeartRateDeltaBpm, int? averageHeartRateDeltaFromRestingBpm, bool hasWorkoutInfluence, List<String> contributingFactors, List<String> dataCoverage, List<String> caveats
});




}
/// @nodoc
class _$PhysiologicalStressEstimateCopyWithImpl<$Res>
    implements $PhysiologicalStressEstimateCopyWith<$Res> {
  _$PhysiologicalStressEstimateCopyWithImpl(this._self, this._then);

  final PhysiologicalStressEstimate _self;
  final $Res Function(PhysiologicalStressEstimate) _then;

/// Create a copy of PhysiologicalStressEstimate
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? level = null,Object? label = null,Object? score = freezed,Object? summary = null,Object? detail = null,Object? confidence = null,Object? confidenceReason = null,Object? hrvPercentFromBaseline = freezed,Object? restingHeartRateDeltaBpm = freezed,Object? averageHeartRateDeltaFromRestingBpm = freezed,Object? hasWorkoutInfluence = null,Object? contributingFactors = null,Object? dataCoverage = null,Object? caveats = null,}) {
  return _then(_self.copyWith(
level: null == level ? _self.level : level // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressLevel,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,score: freezed == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int?,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,hrvPercentFromBaseline: freezed == hrvPercentFromBaseline ? _self.hrvPercentFromBaseline : hrvPercentFromBaseline // ignore: cast_nullable_to_non_nullable
as int?,restingHeartRateDeltaBpm: freezed == restingHeartRateDeltaBpm ? _self.restingHeartRateDeltaBpm : restingHeartRateDeltaBpm // ignore: cast_nullable_to_non_nullable
as int?,averageHeartRateDeltaFromRestingBpm: freezed == averageHeartRateDeltaFromRestingBpm ? _self.averageHeartRateDeltaFromRestingBpm : averageHeartRateDeltaFromRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,hasWorkoutInfluence: null == hasWorkoutInfluence ? _self.hasWorkoutInfluence : hasWorkoutInfluence // ignore: cast_nullable_to_non_nullable
as bool,contributingFactors: null == contributingFactors ? _self.contributingFactors : contributingFactors // ignore: cast_nullable_to_non_nullable
as List<String>,dataCoverage: null == dataCoverage ? _self.dataCoverage : dataCoverage // ignore: cast_nullable_to_non_nullable
as List<String>,caveats: null == caveats ? _self.caveats : caveats // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [PhysiologicalStressEstimate].
extension PhysiologicalStressEstimatePatterns on PhysiologicalStressEstimate {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PhysiologicalStressEstimate value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PhysiologicalStressEstimate value)  $default,){
final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PhysiologicalStressEstimate value)?  $default,){
final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( PhysiologicalStressLevel level,  String label,  int? score,  String summary,  String detail,  PhysiologicalStressConfidence confidence,  String confidenceReason,  int? hrvPercentFromBaseline,  int? restingHeartRateDeltaBpm,  int? averageHeartRateDeltaFromRestingBpm,  bool hasWorkoutInfluence,  List<String> contributingFactors,  List<String> dataCoverage,  List<String> caveats)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate() when $default != null:
return $default(_that.level,_that.label,_that.score,_that.summary,_that.detail,_that.confidence,_that.confidenceReason,_that.hrvPercentFromBaseline,_that.restingHeartRateDeltaBpm,_that.averageHeartRateDeltaFromRestingBpm,_that.hasWorkoutInfluence,_that.contributingFactors,_that.dataCoverage,_that.caveats);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( PhysiologicalStressLevel level,  String label,  int? score,  String summary,  String detail,  PhysiologicalStressConfidence confidence,  String confidenceReason,  int? hrvPercentFromBaseline,  int? restingHeartRateDeltaBpm,  int? averageHeartRateDeltaFromRestingBpm,  bool hasWorkoutInfluence,  List<String> contributingFactors,  List<String> dataCoverage,  List<String> caveats)  $default,) {final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate():
return $default(_that.level,_that.label,_that.score,_that.summary,_that.detail,_that.confidence,_that.confidenceReason,_that.hrvPercentFromBaseline,_that.restingHeartRateDeltaBpm,_that.averageHeartRateDeltaFromRestingBpm,_that.hasWorkoutInfluence,_that.contributingFactors,_that.dataCoverage,_that.caveats);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( PhysiologicalStressLevel level,  String label,  int? score,  String summary,  String detail,  PhysiologicalStressConfidence confidence,  String confidenceReason,  int? hrvPercentFromBaseline,  int? restingHeartRateDeltaBpm,  int? averageHeartRateDeltaFromRestingBpm,  bool hasWorkoutInfluence,  List<String> contributingFactors,  List<String> dataCoverage,  List<String> caveats)?  $default,) {final _that = this;
switch (_that) {
case _PhysiologicalStressEstimate() when $default != null:
return $default(_that.level,_that.label,_that.score,_that.summary,_that.detail,_that.confidence,_that.confidenceReason,_that.hrvPercentFromBaseline,_that.restingHeartRateDeltaBpm,_that.averageHeartRateDeltaFromRestingBpm,_that.hasWorkoutInfluence,_that.contributingFactors,_that.dataCoverage,_that.caveats);case _:
  return null;

}
}

}

/// @nodoc


class _PhysiologicalStressEstimate implements PhysiologicalStressEstimate {
  const _PhysiologicalStressEstimate({required this.level, required this.label, required this.score, required this.summary, required this.detail, required this.confidence, required this.confidenceReason, required this.hrvPercentFromBaseline, required this.restingHeartRateDeltaBpm, required this.averageHeartRateDeltaFromRestingBpm, required this.hasWorkoutInfluence, required final  List<String> contributingFactors, required final  List<String> dataCoverage, required final  List<String> caveats}): _contributingFactors = contributingFactors,_dataCoverage = dataCoverage,_caveats = caveats;
  

@override final  PhysiologicalStressLevel level;
@override final  String label;
@override final  int? score;
@override final  String summary;
@override final  String detail;
@override final  PhysiologicalStressConfidence confidence;
@override final  String confidenceReason;
@override final  int? hrvPercentFromBaseline;
@override final  int? restingHeartRateDeltaBpm;
@override final  int? averageHeartRateDeltaFromRestingBpm;
@override final  bool hasWorkoutInfluence;
 final  List<String> _contributingFactors;
@override List<String> get contributingFactors {
  if (_contributingFactors is EqualUnmodifiableListView) return _contributingFactors;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_contributingFactors);
}

 final  List<String> _dataCoverage;
@override List<String> get dataCoverage {
  if (_dataCoverage is EqualUnmodifiableListView) return _dataCoverage;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dataCoverage);
}

 final  List<String> _caveats;
@override List<String> get caveats {
  if (_caveats is EqualUnmodifiableListView) return _caveats;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_caveats);
}


/// Create a copy of PhysiologicalStressEstimate
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PhysiologicalStressEstimateCopyWith<_PhysiologicalStressEstimate> get copyWith => __$PhysiologicalStressEstimateCopyWithImpl<_PhysiologicalStressEstimate>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PhysiologicalStressEstimate&&(identical(other.level, level) || other.level == level)&&(identical(other.label, label) || other.label == label)&&(identical(other.score, score) || other.score == score)&&(identical(other.summary, summary) || other.summary == summary)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.hrvPercentFromBaseline, hrvPercentFromBaseline) || other.hrvPercentFromBaseline == hrvPercentFromBaseline)&&(identical(other.restingHeartRateDeltaBpm, restingHeartRateDeltaBpm) || other.restingHeartRateDeltaBpm == restingHeartRateDeltaBpm)&&(identical(other.averageHeartRateDeltaFromRestingBpm, averageHeartRateDeltaFromRestingBpm) || other.averageHeartRateDeltaFromRestingBpm == averageHeartRateDeltaFromRestingBpm)&&(identical(other.hasWorkoutInfluence, hasWorkoutInfluence) || other.hasWorkoutInfluence == hasWorkoutInfluence)&&const DeepCollectionEquality().equals(other._contributingFactors, _contributingFactors)&&const DeepCollectionEquality().equals(other._dataCoverage, _dataCoverage)&&const DeepCollectionEquality().equals(other._caveats, _caveats));
}


@override
int get hashCode => Object.hash(runtimeType,level,label,score,summary,detail,confidence,confidenceReason,hrvPercentFromBaseline,restingHeartRateDeltaBpm,averageHeartRateDeltaFromRestingBpm,hasWorkoutInfluence,const DeepCollectionEquality().hash(_contributingFactors),const DeepCollectionEquality().hash(_dataCoverage),const DeepCollectionEquality().hash(_caveats));

@override
String toString() {
  return 'PhysiologicalStressEstimate(level: $level, label: $label, score: $score, summary: $summary, detail: $detail, confidence: $confidence, confidenceReason: $confidenceReason, hrvPercentFromBaseline: $hrvPercentFromBaseline, restingHeartRateDeltaBpm: $restingHeartRateDeltaBpm, averageHeartRateDeltaFromRestingBpm: $averageHeartRateDeltaFromRestingBpm, hasWorkoutInfluence: $hasWorkoutInfluence, contributingFactors: $contributingFactors, dataCoverage: $dataCoverage, caveats: $caveats)';
}


}

/// @nodoc
abstract mixin class _$PhysiologicalStressEstimateCopyWith<$Res> implements $PhysiologicalStressEstimateCopyWith<$Res> {
  factory _$PhysiologicalStressEstimateCopyWith(_PhysiologicalStressEstimate value, $Res Function(_PhysiologicalStressEstimate) _then) = __$PhysiologicalStressEstimateCopyWithImpl;
@override @useResult
$Res call({
 PhysiologicalStressLevel level, String label, int? score, String summary, String detail, PhysiologicalStressConfidence confidence, String confidenceReason, int? hrvPercentFromBaseline, int? restingHeartRateDeltaBpm, int? averageHeartRateDeltaFromRestingBpm, bool hasWorkoutInfluence, List<String> contributingFactors, List<String> dataCoverage, List<String> caveats
});




}
/// @nodoc
class __$PhysiologicalStressEstimateCopyWithImpl<$Res>
    implements _$PhysiologicalStressEstimateCopyWith<$Res> {
  __$PhysiologicalStressEstimateCopyWithImpl(this._self, this._then);

  final _PhysiologicalStressEstimate _self;
  final $Res Function(_PhysiologicalStressEstimate) _then;

/// Create a copy of PhysiologicalStressEstimate
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? level = null,Object? label = null,Object? score = freezed,Object? summary = null,Object? detail = null,Object? confidence = null,Object? confidenceReason = null,Object? hrvPercentFromBaseline = freezed,Object? restingHeartRateDeltaBpm = freezed,Object? averageHeartRateDeltaFromRestingBpm = freezed,Object? hasWorkoutInfluence = null,Object? contributingFactors = null,Object? dataCoverage = null,Object? caveats = null,}) {
  return _then(_PhysiologicalStressEstimate(
level: null == level ? _self.level : level // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressLevel,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,score: freezed == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int?,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,hrvPercentFromBaseline: freezed == hrvPercentFromBaseline ? _self.hrvPercentFromBaseline : hrvPercentFromBaseline // ignore: cast_nullable_to_non_nullable
as int?,restingHeartRateDeltaBpm: freezed == restingHeartRateDeltaBpm ? _self.restingHeartRateDeltaBpm : restingHeartRateDeltaBpm // ignore: cast_nullable_to_non_nullable
as int?,averageHeartRateDeltaFromRestingBpm: freezed == averageHeartRateDeltaFromRestingBpm ? _self.averageHeartRateDeltaFromRestingBpm : averageHeartRateDeltaFromRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,hasWorkoutInfluence: null == hasWorkoutInfluence ? _self.hasWorkoutInfluence : hasWorkoutInfluence // ignore: cast_nullable_to_non_nullable
as bool,contributingFactors: null == contributingFactors ? _self._contributingFactors : contributingFactors // ignore: cast_nullable_to_non_nullable
as List<String>,dataCoverage: null == dataCoverage ? _self._dataCoverage : dataCoverage // ignore: cast_nullable_to_non_nullable
as List<String>,caveats: null == caveats ? _self._caveats : caveats // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}

// dart format on
