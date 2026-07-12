// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mindfulness_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$MindfulnessDisplay {

 int get totalMs; int get sessionCount; int get averageDurationMs; int get longestSessionMs; List<PeriodChartValue> get chartValues; List<DaySample> get cumulativeSamples; List<MindfulnessSession> get sortedSessions;
/// Create a copy of MindfulnessDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessDisplayCopyWith<MindfulnessDisplay> get copyWith => _$MindfulnessDisplayCopyWithImpl<MindfulnessDisplay>(this as MindfulnessDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessDisplay&&(identical(other.totalMs, totalMs) || other.totalMs == totalMs)&&(identical(other.sessionCount, sessionCount) || other.sessionCount == sessionCount)&&(identical(other.averageDurationMs, averageDurationMs) || other.averageDurationMs == averageDurationMs)&&(identical(other.longestSessionMs, longestSessionMs) || other.longestSessionMs == longestSessionMs)&&const DeepCollectionEquality().equals(other.chartValues, chartValues)&&const DeepCollectionEquality().equals(other.cumulativeSamples, cumulativeSamples)&&const DeepCollectionEquality().equals(other.sortedSessions, sortedSessions));
}


@override
int get hashCode => Object.hash(runtimeType,totalMs,sessionCount,averageDurationMs,longestSessionMs,const DeepCollectionEquality().hash(chartValues),const DeepCollectionEquality().hash(cumulativeSamples),const DeepCollectionEquality().hash(sortedSessions));

@override
String toString() {
  return 'MindfulnessDisplay(totalMs: $totalMs, sessionCount: $sessionCount, averageDurationMs: $averageDurationMs, longestSessionMs: $longestSessionMs, chartValues: $chartValues, cumulativeSamples: $cumulativeSamples, sortedSessions: $sortedSessions)';
}


}

/// @nodoc
abstract mixin class $MindfulnessDisplayCopyWith<$Res>  {
  factory $MindfulnessDisplayCopyWith(MindfulnessDisplay value, $Res Function(MindfulnessDisplay) _then) = _$MindfulnessDisplayCopyWithImpl;
@useResult
$Res call({
 int totalMs, int sessionCount, int averageDurationMs, int longestSessionMs, List<PeriodChartValue> chartValues, List<DaySample> cumulativeSamples, List<MindfulnessSession> sortedSessions
});




}
/// @nodoc
class _$MindfulnessDisplayCopyWithImpl<$Res>
    implements $MindfulnessDisplayCopyWith<$Res> {
  _$MindfulnessDisplayCopyWithImpl(this._self, this._then);

  final MindfulnessDisplay _self;
  final $Res Function(MindfulnessDisplay) _then;

/// Create a copy of MindfulnessDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? totalMs = null,Object? sessionCount = null,Object? averageDurationMs = null,Object? longestSessionMs = null,Object? chartValues = null,Object? cumulativeSamples = null,Object? sortedSessions = null,}) {
  return _then(_self.copyWith(
totalMs: null == totalMs ? _self.totalMs : totalMs // ignore: cast_nullable_to_non_nullable
as int,sessionCount: null == sessionCount ? _self.sessionCount : sessionCount // ignore: cast_nullable_to_non_nullable
as int,averageDurationMs: null == averageDurationMs ? _self.averageDurationMs : averageDurationMs // ignore: cast_nullable_to_non_nullable
as int,longestSessionMs: null == longestSessionMs ? _self.longestSessionMs : longestSessionMs // ignore: cast_nullable_to_non_nullable
as int,chartValues: null == chartValues ? _self.chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,cumulativeSamples: null == cumulativeSamples ? _self.cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,sortedSessions: null == sortedSessions ? _self.sortedSessions : sortedSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessDisplay].
extension MindfulnessDisplayPatterns on MindfulnessDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessDisplay value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int totalMs,  int sessionCount,  int averageDurationMs,  int longestSessionMs,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<MindfulnessSession> sortedSessions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessDisplay() when $default != null:
return $default(_that.totalMs,_that.sessionCount,_that.averageDurationMs,_that.longestSessionMs,_that.chartValues,_that.cumulativeSamples,_that.sortedSessions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int totalMs,  int sessionCount,  int averageDurationMs,  int longestSessionMs,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<MindfulnessSession> sortedSessions)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessDisplay():
return $default(_that.totalMs,_that.sessionCount,_that.averageDurationMs,_that.longestSessionMs,_that.chartValues,_that.cumulativeSamples,_that.sortedSessions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int totalMs,  int sessionCount,  int averageDurationMs,  int longestSessionMs,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<MindfulnessSession> sortedSessions)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessDisplay() when $default != null:
return $default(_that.totalMs,_that.sessionCount,_that.averageDurationMs,_that.longestSessionMs,_that.chartValues,_that.cumulativeSamples,_that.sortedSessions);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessDisplay extends MindfulnessDisplay {
  const _MindfulnessDisplay({required this.totalMs, required this.sessionCount, required this.averageDurationMs, required this.longestSessionMs, required final  List<PeriodChartValue> chartValues, required final  List<DaySample> cumulativeSamples, required final  List<MindfulnessSession> sortedSessions}): _chartValues = chartValues,_cumulativeSamples = cumulativeSamples,_sortedSessions = sortedSessions,super._();
  

@override final  int totalMs;
@override final  int sessionCount;
@override final  int averageDurationMs;
@override final  int longestSessionMs;
 final  List<PeriodChartValue> _chartValues;
@override List<PeriodChartValue> get chartValues {
  if (_chartValues is EqualUnmodifiableListView) return _chartValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_chartValues);
}

 final  List<DaySample> _cumulativeSamples;
@override List<DaySample> get cumulativeSamples {
  if (_cumulativeSamples is EqualUnmodifiableListView) return _cumulativeSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cumulativeSamples);
}

 final  List<MindfulnessSession> _sortedSessions;
@override List<MindfulnessSession> get sortedSessions {
  if (_sortedSessions is EqualUnmodifiableListView) return _sortedSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sortedSessions);
}


/// Create a copy of MindfulnessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessDisplayCopyWith<_MindfulnessDisplay> get copyWith => __$MindfulnessDisplayCopyWithImpl<_MindfulnessDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessDisplay&&(identical(other.totalMs, totalMs) || other.totalMs == totalMs)&&(identical(other.sessionCount, sessionCount) || other.sessionCount == sessionCount)&&(identical(other.averageDurationMs, averageDurationMs) || other.averageDurationMs == averageDurationMs)&&(identical(other.longestSessionMs, longestSessionMs) || other.longestSessionMs == longestSessionMs)&&const DeepCollectionEquality().equals(other._chartValues, _chartValues)&&const DeepCollectionEquality().equals(other._cumulativeSamples, _cumulativeSamples)&&const DeepCollectionEquality().equals(other._sortedSessions, _sortedSessions));
}


@override
int get hashCode => Object.hash(runtimeType,totalMs,sessionCount,averageDurationMs,longestSessionMs,const DeepCollectionEquality().hash(_chartValues),const DeepCollectionEquality().hash(_cumulativeSamples),const DeepCollectionEquality().hash(_sortedSessions));

@override
String toString() {
  return 'MindfulnessDisplay(totalMs: $totalMs, sessionCount: $sessionCount, averageDurationMs: $averageDurationMs, longestSessionMs: $longestSessionMs, chartValues: $chartValues, cumulativeSamples: $cumulativeSamples, sortedSessions: $sortedSessions)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessDisplayCopyWith<$Res> implements $MindfulnessDisplayCopyWith<$Res> {
  factory _$MindfulnessDisplayCopyWith(_MindfulnessDisplay value, $Res Function(_MindfulnessDisplay) _then) = __$MindfulnessDisplayCopyWithImpl;
@override @useResult
$Res call({
 int totalMs, int sessionCount, int averageDurationMs, int longestSessionMs, List<PeriodChartValue> chartValues, List<DaySample> cumulativeSamples, List<MindfulnessSession> sortedSessions
});




}
/// @nodoc
class __$MindfulnessDisplayCopyWithImpl<$Res>
    implements _$MindfulnessDisplayCopyWith<$Res> {
  __$MindfulnessDisplayCopyWithImpl(this._self, this._then);

  final _MindfulnessDisplay _self;
  final $Res Function(_MindfulnessDisplay) _then;

/// Create a copy of MindfulnessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? totalMs = null,Object? sessionCount = null,Object? averageDurationMs = null,Object? longestSessionMs = null,Object? chartValues = null,Object? cumulativeSamples = null,Object? sortedSessions = null,}) {
  return _then(_MindfulnessDisplay(
totalMs: null == totalMs ? _self.totalMs : totalMs // ignore: cast_nullable_to_non_nullable
as int,sessionCount: null == sessionCount ? _self.sessionCount : sessionCount // ignore: cast_nullable_to_non_nullable
as int,averageDurationMs: null == averageDurationMs ? _self.averageDurationMs : averageDurationMs // ignore: cast_nullable_to_non_nullable
as int,longestSessionMs: null == longestSessionMs ? _self.longestSessionMs : longestSessionMs // ignore: cast_nullable_to_non_nullable
as int,chartValues: null == chartValues ? _self._chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,cumulativeSamples: null == cumulativeSamples ? _self._cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,sortedSessions: null == sortedSessions ? _self._sortedSessions : sortedSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,
  ));
}


}

// dart format on
