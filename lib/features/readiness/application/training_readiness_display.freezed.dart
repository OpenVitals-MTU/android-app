// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'training_readiness_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$TrainingReadinessDisplay {

 int get score; String get verdict; String get confidence; List<String> get signals; List<String> get guidance;
/// Create a copy of TrainingReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$TrainingReadinessDisplayCopyWith<TrainingReadinessDisplay> get copyWith => _$TrainingReadinessDisplayCopyWithImpl<TrainingReadinessDisplay>(this as TrainingReadinessDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is TrainingReadinessDisplay&&(identical(other.score, score) || other.score == score)&&(identical(other.verdict, verdict) || other.verdict == verdict)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&const DeepCollectionEquality().equals(other.signals, signals)&&const DeepCollectionEquality().equals(other.guidance, guidance));
}


@override
int get hashCode => Object.hash(runtimeType,score,verdict,confidence,const DeepCollectionEquality().hash(signals),const DeepCollectionEquality().hash(guidance));

@override
String toString() {
  return 'TrainingReadinessDisplay(score: $score, verdict: $verdict, confidence: $confidence, signals: $signals, guidance: $guidance)';
}


}

/// @nodoc
abstract mixin class $TrainingReadinessDisplayCopyWith<$Res>  {
  factory $TrainingReadinessDisplayCopyWith(TrainingReadinessDisplay value, $Res Function(TrainingReadinessDisplay) _then) = _$TrainingReadinessDisplayCopyWithImpl;
@useResult
$Res call({
 int score, String verdict, String confidence, List<String> signals, List<String> guidance
});




}
/// @nodoc
class _$TrainingReadinessDisplayCopyWithImpl<$Res>
    implements $TrainingReadinessDisplayCopyWith<$Res> {
  _$TrainingReadinessDisplayCopyWithImpl(this._self, this._then);

  final TrainingReadinessDisplay _self;
  final $Res Function(TrainingReadinessDisplay) _then;

/// Create a copy of TrainingReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? score = null,Object? verdict = null,Object? confidence = null,Object? signals = null,Object? guidance = null,}) {
  return _then(_self.copyWith(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,verdict: null == verdict ? _self.verdict : verdict // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as String,signals: null == signals ? _self.signals : signals // ignore: cast_nullable_to_non_nullable
as List<String>,guidance: null == guidance ? _self.guidance : guidance // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [TrainingReadinessDisplay].
extension TrainingReadinessDisplayPatterns on TrainingReadinessDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _TrainingReadinessDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _TrainingReadinessDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _TrainingReadinessDisplay value)  $default,){
final _that = this;
switch (_that) {
case _TrainingReadinessDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _TrainingReadinessDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _TrainingReadinessDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int score,  String verdict,  String confidence,  List<String> signals,  List<String> guidance)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _TrainingReadinessDisplay() when $default != null:
return $default(_that.score,_that.verdict,_that.confidence,_that.signals,_that.guidance);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int score,  String verdict,  String confidence,  List<String> signals,  List<String> guidance)  $default,) {final _that = this;
switch (_that) {
case _TrainingReadinessDisplay():
return $default(_that.score,_that.verdict,_that.confidence,_that.signals,_that.guidance);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int score,  String verdict,  String confidence,  List<String> signals,  List<String> guidance)?  $default,) {final _that = this;
switch (_that) {
case _TrainingReadinessDisplay() when $default != null:
return $default(_that.score,_that.verdict,_that.confidence,_that.signals,_that.guidance);case _:
  return null;

}
}

}

/// @nodoc


class _TrainingReadinessDisplay implements TrainingReadinessDisplay {
  const _TrainingReadinessDisplay({required this.score, required this.verdict, required this.confidence, required final  List<String> signals, required final  List<String> guidance}): _signals = signals,_guidance = guidance;
  

@override final  int score;
@override final  String verdict;
@override final  String confidence;
 final  List<String> _signals;
@override List<String> get signals {
  if (_signals is EqualUnmodifiableListView) return _signals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_signals);
}

 final  List<String> _guidance;
@override List<String> get guidance {
  if (_guidance is EqualUnmodifiableListView) return _guidance;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_guidance);
}


/// Create a copy of TrainingReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$TrainingReadinessDisplayCopyWith<_TrainingReadinessDisplay> get copyWith => __$TrainingReadinessDisplayCopyWithImpl<_TrainingReadinessDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _TrainingReadinessDisplay&&(identical(other.score, score) || other.score == score)&&(identical(other.verdict, verdict) || other.verdict == verdict)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&const DeepCollectionEquality().equals(other._signals, _signals)&&const DeepCollectionEquality().equals(other._guidance, _guidance));
}


@override
int get hashCode => Object.hash(runtimeType,score,verdict,confidence,const DeepCollectionEquality().hash(_signals),const DeepCollectionEquality().hash(_guidance));

@override
String toString() {
  return 'TrainingReadinessDisplay(score: $score, verdict: $verdict, confidence: $confidence, signals: $signals, guidance: $guidance)';
}


}

/// @nodoc
abstract mixin class _$TrainingReadinessDisplayCopyWith<$Res> implements $TrainingReadinessDisplayCopyWith<$Res> {
  factory _$TrainingReadinessDisplayCopyWith(_TrainingReadinessDisplay value, $Res Function(_TrainingReadinessDisplay) _then) = __$TrainingReadinessDisplayCopyWithImpl;
@override @useResult
$Res call({
 int score, String verdict, String confidence, List<String> signals, List<String> guidance
});




}
/// @nodoc
class __$TrainingReadinessDisplayCopyWithImpl<$Res>
    implements _$TrainingReadinessDisplayCopyWith<$Res> {
  __$TrainingReadinessDisplayCopyWithImpl(this._self, this._then);

  final _TrainingReadinessDisplay _self;
  final $Res Function(_TrainingReadinessDisplay) _then;

/// Create a copy of TrainingReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? score = null,Object? verdict = null,Object? confidence = null,Object? signals = null,Object? guidance = null,}) {
  return _then(_TrainingReadinessDisplay(
score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,verdict: null == verdict ? _self.verdict : verdict // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as String,signals: null == signals ? _self._signals : signals // ignore: cast_nullable_to_non_nullable
as List<String>,guidance: null == guidance ? _self._guidance : guidance // ignore: cast_nullable_to_non_nullable
as List<String>,
  ));
}


}

// dart format on
