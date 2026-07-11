// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartPeriodData {

 List<HeartRateSample> get daySamples; List<HeartRateSample> get previousDaySamples; List<HeartRateSummary> get dailySummaries; List<HeartRateSummary> get previousDailySummaries; List<HeartRateSummary> get baselineDailySummaries; List<RestingHeartRateSample> get dayRestingSamples; int? get dayRestingBpm; int? get previousDayRestingBpm; List<HrvSample> get dayHrvSamples; double? get dayHrvMs; double? get previousDayHrvMs; List<DailyRestingHR> get dailyRestingHR; List<DailyRestingHR> get previousDailyRestingHR; List<DailyRestingHR> get baselineDailyRestingHR; List<DailyHrv> get dailyHrv; List<DailyHrv> get previousDailyHrv; List<DailyHrv> get baselineDailyHrv;
/// Create a copy of HeartPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartPeriodDataCopyWith<HeartPeriodData> get copyWith => _$HeartPeriodDataCopyWithImpl<HeartPeriodData>(this as HeartPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartPeriodData&&const DeepCollectionEquality().equals(other.daySamples, daySamples)&&const DeepCollectionEquality().equals(other.previousDaySamples, previousDaySamples)&&const DeepCollectionEquality().equals(other.dailySummaries, dailySummaries)&&const DeepCollectionEquality().equals(other.previousDailySummaries, previousDailySummaries)&&const DeepCollectionEquality().equals(other.baselineDailySummaries, baselineDailySummaries)&&const DeepCollectionEquality().equals(other.dayRestingSamples, dayRestingSamples)&&(identical(other.dayRestingBpm, dayRestingBpm) || other.dayRestingBpm == dayRestingBpm)&&(identical(other.previousDayRestingBpm, previousDayRestingBpm) || other.previousDayRestingBpm == previousDayRestingBpm)&&const DeepCollectionEquality().equals(other.dayHrvSamples, dayHrvSamples)&&(identical(other.dayHrvMs, dayHrvMs) || other.dayHrvMs == dayHrvMs)&&(identical(other.previousDayHrvMs, previousDayHrvMs) || other.previousDayHrvMs == previousDayHrvMs)&&const DeepCollectionEquality().equals(other.dailyRestingHR, dailyRestingHR)&&const DeepCollectionEquality().equals(other.previousDailyRestingHR, previousDailyRestingHR)&&const DeepCollectionEquality().equals(other.baselineDailyRestingHR, baselineDailyRestingHR)&&const DeepCollectionEquality().equals(other.dailyHrv, dailyHrv)&&const DeepCollectionEquality().equals(other.previousDailyHrv, previousDailyHrv)&&const DeepCollectionEquality().equals(other.baselineDailyHrv, baselineDailyHrv));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(daySamples),const DeepCollectionEquality().hash(previousDaySamples),const DeepCollectionEquality().hash(dailySummaries),const DeepCollectionEquality().hash(previousDailySummaries),const DeepCollectionEquality().hash(baselineDailySummaries),const DeepCollectionEquality().hash(dayRestingSamples),dayRestingBpm,previousDayRestingBpm,const DeepCollectionEquality().hash(dayHrvSamples),dayHrvMs,previousDayHrvMs,const DeepCollectionEquality().hash(dailyRestingHR),const DeepCollectionEquality().hash(previousDailyRestingHR),const DeepCollectionEquality().hash(baselineDailyRestingHR),const DeepCollectionEquality().hash(dailyHrv),const DeepCollectionEquality().hash(previousDailyHrv),const DeepCollectionEquality().hash(baselineDailyHrv));

@override
String toString() {
  return 'HeartPeriodData(daySamples: $daySamples, previousDaySamples: $previousDaySamples, dailySummaries: $dailySummaries, previousDailySummaries: $previousDailySummaries, baselineDailySummaries: $baselineDailySummaries, dayRestingSamples: $dayRestingSamples, dayRestingBpm: $dayRestingBpm, previousDayRestingBpm: $previousDayRestingBpm, dayHrvSamples: $dayHrvSamples, dayHrvMs: $dayHrvMs, previousDayHrvMs: $previousDayHrvMs, dailyRestingHR: $dailyRestingHR, previousDailyRestingHR: $previousDailyRestingHR, baselineDailyRestingHR: $baselineDailyRestingHR, dailyHrv: $dailyHrv, previousDailyHrv: $previousDailyHrv, baselineDailyHrv: $baselineDailyHrv)';
}


}

/// @nodoc
abstract mixin class $HeartPeriodDataCopyWith<$Res>  {
  factory $HeartPeriodDataCopyWith(HeartPeriodData value, $Res Function(HeartPeriodData) _then) = _$HeartPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<HeartRateSample> daySamples, List<HeartRateSample> previousDaySamples, List<HeartRateSummary> dailySummaries, List<HeartRateSummary> previousDailySummaries, List<HeartRateSummary> baselineDailySummaries, List<RestingHeartRateSample> dayRestingSamples, int? dayRestingBpm, int? previousDayRestingBpm, List<HrvSample> dayHrvSamples, double? dayHrvMs, double? previousDayHrvMs, List<DailyRestingHR> dailyRestingHR, List<DailyRestingHR> previousDailyRestingHR, List<DailyRestingHR> baselineDailyRestingHR, List<DailyHrv> dailyHrv, List<DailyHrv> previousDailyHrv, List<DailyHrv> baselineDailyHrv
});




}
/// @nodoc
class _$HeartPeriodDataCopyWithImpl<$Res>
    implements $HeartPeriodDataCopyWith<$Res> {
  _$HeartPeriodDataCopyWithImpl(this._self, this._then);

  final HeartPeriodData _self;
  final $Res Function(HeartPeriodData) _then;

/// Create a copy of HeartPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? daySamples = null,Object? previousDaySamples = null,Object? dailySummaries = null,Object? previousDailySummaries = null,Object? baselineDailySummaries = null,Object? dayRestingSamples = null,Object? dayRestingBpm = freezed,Object? previousDayRestingBpm = freezed,Object? dayHrvSamples = null,Object? dayHrvMs = freezed,Object? previousDayHrvMs = freezed,Object? dailyRestingHR = null,Object? previousDailyRestingHR = null,Object? baselineDailyRestingHR = null,Object? dailyHrv = null,Object? previousDailyHrv = null,Object? baselineDailyHrv = null,}) {
  return _then(_self.copyWith(
daySamples: null == daySamples ? _self.daySamples : daySamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,previousDaySamples: null == previousDaySamples ? _self.previousDaySamples : previousDaySamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,dailySummaries: null == dailySummaries ? _self.dailySummaries : dailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,previousDailySummaries: null == previousDailySummaries ? _self.previousDailySummaries : previousDailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,baselineDailySummaries: null == baselineDailySummaries ? _self.baselineDailySummaries : baselineDailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,dayRestingSamples: null == dayRestingSamples ? _self.dayRestingSamples : dayRestingSamples // ignore: cast_nullable_to_non_nullable
as List<RestingHeartRateSample>,dayRestingBpm: freezed == dayRestingBpm ? _self.dayRestingBpm : dayRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,previousDayRestingBpm: freezed == previousDayRestingBpm ? _self.previousDayRestingBpm : previousDayRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,dayHrvSamples: null == dayHrvSamples ? _self.dayHrvSamples : dayHrvSamples // ignore: cast_nullable_to_non_nullable
as List<HrvSample>,dayHrvMs: freezed == dayHrvMs ? _self.dayHrvMs : dayHrvMs // ignore: cast_nullable_to_non_nullable
as double?,previousDayHrvMs: freezed == previousDayHrvMs ? _self.previousDayHrvMs : previousDayHrvMs // ignore: cast_nullable_to_non_nullable
as double?,dailyRestingHR: null == dailyRestingHR ? _self.dailyRestingHR : dailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,previousDailyRestingHR: null == previousDailyRestingHR ? _self.previousDailyRestingHR : previousDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,baselineDailyRestingHR: null == baselineDailyRestingHR ? _self.baselineDailyRestingHR : baselineDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,dailyHrv: null == dailyHrv ? _self.dailyHrv : dailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,previousDailyHrv: null == previousDailyHrv ? _self.previousDailyHrv : previousDailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,baselineDailyHrv: null == baselineDailyHrv ? _self.baselineDailyHrv : baselineDailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartPeriodData].
extension HeartPeriodDataPatterns on HeartPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _HeartPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _HeartPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<HeartRateSample> daySamples,  List<HeartRateSample> previousDaySamples,  List<HeartRateSummary> dailySummaries,  List<HeartRateSummary> previousDailySummaries,  List<HeartRateSummary> baselineDailySummaries,  List<RestingHeartRateSample> dayRestingSamples,  int? dayRestingBpm,  int? previousDayRestingBpm,  List<HrvSample> dayHrvSamples,  double? dayHrvMs,  double? previousDayHrvMs,  List<DailyRestingHR> dailyRestingHR,  List<DailyRestingHR> previousDailyRestingHR,  List<DailyRestingHR> baselineDailyRestingHR,  List<DailyHrv> dailyHrv,  List<DailyHrv> previousDailyHrv,  List<DailyHrv> baselineDailyHrv)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartPeriodData() when $default != null:
return $default(_that.daySamples,_that.previousDaySamples,_that.dailySummaries,_that.previousDailySummaries,_that.baselineDailySummaries,_that.dayRestingSamples,_that.dayRestingBpm,_that.previousDayRestingBpm,_that.dayHrvSamples,_that.dayHrvMs,_that.previousDayHrvMs,_that.dailyRestingHR,_that.previousDailyRestingHR,_that.baselineDailyRestingHR,_that.dailyHrv,_that.previousDailyHrv,_that.baselineDailyHrv);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<HeartRateSample> daySamples,  List<HeartRateSample> previousDaySamples,  List<HeartRateSummary> dailySummaries,  List<HeartRateSummary> previousDailySummaries,  List<HeartRateSummary> baselineDailySummaries,  List<RestingHeartRateSample> dayRestingSamples,  int? dayRestingBpm,  int? previousDayRestingBpm,  List<HrvSample> dayHrvSamples,  double? dayHrvMs,  double? previousDayHrvMs,  List<DailyRestingHR> dailyRestingHR,  List<DailyRestingHR> previousDailyRestingHR,  List<DailyRestingHR> baselineDailyRestingHR,  List<DailyHrv> dailyHrv,  List<DailyHrv> previousDailyHrv,  List<DailyHrv> baselineDailyHrv)  $default,) {final _that = this;
switch (_that) {
case _HeartPeriodData():
return $default(_that.daySamples,_that.previousDaySamples,_that.dailySummaries,_that.previousDailySummaries,_that.baselineDailySummaries,_that.dayRestingSamples,_that.dayRestingBpm,_that.previousDayRestingBpm,_that.dayHrvSamples,_that.dayHrvMs,_that.previousDayHrvMs,_that.dailyRestingHR,_that.previousDailyRestingHR,_that.baselineDailyRestingHR,_that.dailyHrv,_that.previousDailyHrv,_that.baselineDailyHrv);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<HeartRateSample> daySamples,  List<HeartRateSample> previousDaySamples,  List<HeartRateSummary> dailySummaries,  List<HeartRateSummary> previousDailySummaries,  List<HeartRateSummary> baselineDailySummaries,  List<RestingHeartRateSample> dayRestingSamples,  int? dayRestingBpm,  int? previousDayRestingBpm,  List<HrvSample> dayHrvSamples,  double? dayHrvMs,  double? previousDayHrvMs,  List<DailyRestingHR> dailyRestingHR,  List<DailyRestingHR> previousDailyRestingHR,  List<DailyRestingHR> baselineDailyRestingHR,  List<DailyHrv> dailyHrv,  List<DailyHrv> previousDailyHrv,  List<DailyHrv> baselineDailyHrv)?  $default,) {final _that = this;
switch (_that) {
case _HeartPeriodData() when $default != null:
return $default(_that.daySamples,_that.previousDaySamples,_that.dailySummaries,_that.previousDailySummaries,_that.baselineDailySummaries,_that.dayRestingSamples,_that.dayRestingBpm,_that.previousDayRestingBpm,_that.dayHrvSamples,_that.dayHrvMs,_that.previousDayHrvMs,_that.dailyRestingHR,_that.previousDailyRestingHR,_that.baselineDailyRestingHR,_that.dailyHrv,_that.previousDailyHrv,_that.baselineDailyHrv);case _:
  return null;

}
}

}

/// @nodoc


class _HeartPeriodData implements HeartPeriodData {
  const _HeartPeriodData({final  List<HeartRateSample> daySamples = const <HeartRateSample>[], final  List<HeartRateSample> previousDaySamples = const <HeartRateSample>[], final  List<HeartRateSummary> dailySummaries = const <HeartRateSummary>[], final  List<HeartRateSummary> previousDailySummaries = const <HeartRateSummary>[], final  List<HeartRateSummary> baselineDailySummaries = const <HeartRateSummary>[], final  List<RestingHeartRateSample> dayRestingSamples = const <RestingHeartRateSample>[], this.dayRestingBpm, this.previousDayRestingBpm, final  List<HrvSample> dayHrvSamples = const <HrvSample>[], this.dayHrvMs, this.previousDayHrvMs, final  List<DailyRestingHR> dailyRestingHR = const <DailyRestingHR>[], final  List<DailyRestingHR> previousDailyRestingHR = const <DailyRestingHR>[], final  List<DailyRestingHR> baselineDailyRestingHR = const <DailyRestingHR>[], final  List<DailyHrv> dailyHrv = const <DailyHrv>[], final  List<DailyHrv> previousDailyHrv = const <DailyHrv>[], final  List<DailyHrv> baselineDailyHrv = const <DailyHrv>[]}): _daySamples = daySamples,_previousDaySamples = previousDaySamples,_dailySummaries = dailySummaries,_previousDailySummaries = previousDailySummaries,_baselineDailySummaries = baselineDailySummaries,_dayRestingSamples = dayRestingSamples,_dayHrvSamples = dayHrvSamples,_dailyRestingHR = dailyRestingHR,_previousDailyRestingHR = previousDailyRestingHR,_baselineDailyRestingHR = baselineDailyRestingHR,_dailyHrv = dailyHrv,_previousDailyHrv = previousDailyHrv,_baselineDailyHrv = baselineDailyHrv;
  

 final  List<HeartRateSample> _daySamples;
@override@JsonKey() List<HeartRateSample> get daySamples {
  if (_daySamples is EqualUnmodifiableListView) return _daySamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_daySamples);
}

 final  List<HeartRateSample> _previousDaySamples;
@override@JsonKey() List<HeartRateSample> get previousDaySamples {
  if (_previousDaySamples is EqualUnmodifiableListView) return _previousDaySamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDaySamples);
}

 final  List<HeartRateSummary> _dailySummaries;
@override@JsonKey() List<HeartRateSummary> get dailySummaries {
  if (_dailySummaries is EqualUnmodifiableListView) return _dailySummaries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailySummaries);
}

 final  List<HeartRateSummary> _previousDailySummaries;
@override@JsonKey() List<HeartRateSummary> get previousDailySummaries {
  if (_previousDailySummaries is EqualUnmodifiableListView) return _previousDailySummaries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailySummaries);
}

 final  List<HeartRateSummary> _baselineDailySummaries;
@override@JsonKey() List<HeartRateSummary> get baselineDailySummaries {
  if (_baselineDailySummaries is EqualUnmodifiableListView) return _baselineDailySummaries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailySummaries);
}

 final  List<RestingHeartRateSample> _dayRestingSamples;
@override@JsonKey() List<RestingHeartRateSample> get dayRestingSamples {
  if (_dayRestingSamples is EqualUnmodifiableListView) return _dayRestingSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dayRestingSamples);
}

@override final  int? dayRestingBpm;
@override final  int? previousDayRestingBpm;
 final  List<HrvSample> _dayHrvSamples;
@override@JsonKey() List<HrvSample> get dayHrvSamples {
  if (_dayHrvSamples is EqualUnmodifiableListView) return _dayHrvSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dayHrvSamples);
}

@override final  double? dayHrvMs;
@override final  double? previousDayHrvMs;
 final  List<DailyRestingHR> _dailyRestingHR;
@override@JsonKey() List<DailyRestingHR> get dailyRestingHR {
  if (_dailyRestingHR is EqualUnmodifiableListView) return _dailyRestingHR;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyRestingHR);
}

 final  List<DailyRestingHR> _previousDailyRestingHR;
@override@JsonKey() List<DailyRestingHR> get previousDailyRestingHR {
  if (_previousDailyRestingHR is EqualUnmodifiableListView) return _previousDailyRestingHR;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyRestingHR);
}

 final  List<DailyRestingHR> _baselineDailyRestingHR;
@override@JsonKey() List<DailyRestingHR> get baselineDailyRestingHR {
  if (_baselineDailyRestingHR is EqualUnmodifiableListView) return _baselineDailyRestingHR;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyRestingHR);
}

 final  List<DailyHrv> _dailyHrv;
@override@JsonKey() List<DailyHrv> get dailyHrv {
  if (_dailyHrv is EqualUnmodifiableListView) return _dailyHrv;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyHrv);
}

 final  List<DailyHrv> _previousDailyHrv;
@override@JsonKey() List<DailyHrv> get previousDailyHrv {
  if (_previousDailyHrv is EqualUnmodifiableListView) return _previousDailyHrv;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyHrv);
}

 final  List<DailyHrv> _baselineDailyHrv;
@override@JsonKey() List<DailyHrv> get baselineDailyHrv {
  if (_baselineDailyHrv is EqualUnmodifiableListView) return _baselineDailyHrv;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyHrv);
}


/// Create a copy of HeartPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartPeriodDataCopyWith<_HeartPeriodData> get copyWith => __$HeartPeriodDataCopyWithImpl<_HeartPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartPeriodData&&const DeepCollectionEquality().equals(other._daySamples, _daySamples)&&const DeepCollectionEquality().equals(other._previousDaySamples, _previousDaySamples)&&const DeepCollectionEquality().equals(other._dailySummaries, _dailySummaries)&&const DeepCollectionEquality().equals(other._previousDailySummaries, _previousDailySummaries)&&const DeepCollectionEquality().equals(other._baselineDailySummaries, _baselineDailySummaries)&&const DeepCollectionEquality().equals(other._dayRestingSamples, _dayRestingSamples)&&(identical(other.dayRestingBpm, dayRestingBpm) || other.dayRestingBpm == dayRestingBpm)&&(identical(other.previousDayRestingBpm, previousDayRestingBpm) || other.previousDayRestingBpm == previousDayRestingBpm)&&const DeepCollectionEquality().equals(other._dayHrvSamples, _dayHrvSamples)&&(identical(other.dayHrvMs, dayHrvMs) || other.dayHrvMs == dayHrvMs)&&(identical(other.previousDayHrvMs, previousDayHrvMs) || other.previousDayHrvMs == previousDayHrvMs)&&const DeepCollectionEquality().equals(other._dailyRestingHR, _dailyRestingHR)&&const DeepCollectionEquality().equals(other._previousDailyRestingHR, _previousDailyRestingHR)&&const DeepCollectionEquality().equals(other._baselineDailyRestingHR, _baselineDailyRestingHR)&&const DeepCollectionEquality().equals(other._dailyHrv, _dailyHrv)&&const DeepCollectionEquality().equals(other._previousDailyHrv, _previousDailyHrv)&&const DeepCollectionEquality().equals(other._baselineDailyHrv, _baselineDailyHrv));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_daySamples),const DeepCollectionEquality().hash(_previousDaySamples),const DeepCollectionEquality().hash(_dailySummaries),const DeepCollectionEquality().hash(_previousDailySummaries),const DeepCollectionEquality().hash(_baselineDailySummaries),const DeepCollectionEquality().hash(_dayRestingSamples),dayRestingBpm,previousDayRestingBpm,const DeepCollectionEquality().hash(_dayHrvSamples),dayHrvMs,previousDayHrvMs,const DeepCollectionEquality().hash(_dailyRestingHR),const DeepCollectionEquality().hash(_previousDailyRestingHR),const DeepCollectionEquality().hash(_baselineDailyRestingHR),const DeepCollectionEquality().hash(_dailyHrv),const DeepCollectionEquality().hash(_previousDailyHrv),const DeepCollectionEquality().hash(_baselineDailyHrv));

@override
String toString() {
  return 'HeartPeriodData(daySamples: $daySamples, previousDaySamples: $previousDaySamples, dailySummaries: $dailySummaries, previousDailySummaries: $previousDailySummaries, baselineDailySummaries: $baselineDailySummaries, dayRestingSamples: $dayRestingSamples, dayRestingBpm: $dayRestingBpm, previousDayRestingBpm: $previousDayRestingBpm, dayHrvSamples: $dayHrvSamples, dayHrvMs: $dayHrvMs, previousDayHrvMs: $previousDayHrvMs, dailyRestingHR: $dailyRestingHR, previousDailyRestingHR: $previousDailyRestingHR, baselineDailyRestingHR: $baselineDailyRestingHR, dailyHrv: $dailyHrv, previousDailyHrv: $previousDailyHrv, baselineDailyHrv: $baselineDailyHrv)';
}


}

/// @nodoc
abstract mixin class _$HeartPeriodDataCopyWith<$Res> implements $HeartPeriodDataCopyWith<$Res> {
  factory _$HeartPeriodDataCopyWith(_HeartPeriodData value, $Res Function(_HeartPeriodData) _then) = __$HeartPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<HeartRateSample> daySamples, List<HeartRateSample> previousDaySamples, List<HeartRateSummary> dailySummaries, List<HeartRateSummary> previousDailySummaries, List<HeartRateSummary> baselineDailySummaries, List<RestingHeartRateSample> dayRestingSamples, int? dayRestingBpm, int? previousDayRestingBpm, List<HrvSample> dayHrvSamples, double? dayHrvMs, double? previousDayHrvMs, List<DailyRestingHR> dailyRestingHR, List<DailyRestingHR> previousDailyRestingHR, List<DailyRestingHR> baselineDailyRestingHR, List<DailyHrv> dailyHrv, List<DailyHrv> previousDailyHrv, List<DailyHrv> baselineDailyHrv
});




}
/// @nodoc
class __$HeartPeriodDataCopyWithImpl<$Res>
    implements _$HeartPeriodDataCopyWith<$Res> {
  __$HeartPeriodDataCopyWithImpl(this._self, this._then);

  final _HeartPeriodData _self;
  final $Res Function(_HeartPeriodData) _then;

/// Create a copy of HeartPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? daySamples = null,Object? previousDaySamples = null,Object? dailySummaries = null,Object? previousDailySummaries = null,Object? baselineDailySummaries = null,Object? dayRestingSamples = null,Object? dayRestingBpm = freezed,Object? previousDayRestingBpm = freezed,Object? dayHrvSamples = null,Object? dayHrvMs = freezed,Object? previousDayHrvMs = freezed,Object? dailyRestingHR = null,Object? previousDailyRestingHR = null,Object? baselineDailyRestingHR = null,Object? dailyHrv = null,Object? previousDailyHrv = null,Object? baselineDailyHrv = null,}) {
  return _then(_HeartPeriodData(
daySamples: null == daySamples ? _self._daySamples : daySamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,previousDaySamples: null == previousDaySamples ? _self._previousDaySamples : previousDaySamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,dailySummaries: null == dailySummaries ? _self._dailySummaries : dailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,previousDailySummaries: null == previousDailySummaries ? _self._previousDailySummaries : previousDailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,baselineDailySummaries: null == baselineDailySummaries ? _self._baselineDailySummaries : baselineDailySummaries // ignore: cast_nullable_to_non_nullable
as List<HeartRateSummary>,dayRestingSamples: null == dayRestingSamples ? _self._dayRestingSamples : dayRestingSamples // ignore: cast_nullable_to_non_nullable
as List<RestingHeartRateSample>,dayRestingBpm: freezed == dayRestingBpm ? _self.dayRestingBpm : dayRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,previousDayRestingBpm: freezed == previousDayRestingBpm ? _self.previousDayRestingBpm : previousDayRestingBpm // ignore: cast_nullable_to_non_nullable
as int?,dayHrvSamples: null == dayHrvSamples ? _self._dayHrvSamples : dayHrvSamples // ignore: cast_nullable_to_non_nullable
as List<HrvSample>,dayHrvMs: freezed == dayHrvMs ? _self.dayHrvMs : dayHrvMs // ignore: cast_nullable_to_non_nullable
as double?,previousDayHrvMs: freezed == previousDayHrvMs ? _self.previousDayHrvMs : previousDayHrvMs // ignore: cast_nullable_to_non_nullable
as double?,dailyRestingHR: null == dailyRestingHR ? _self._dailyRestingHR : dailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,previousDailyRestingHR: null == previousDailyRestingHR ? _self._previousDailyRestingHR : previousDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,baselineDailyRestingHR: null == baselineDailyRestingHR ? _self._baselineDailyRestingHR : baselineDailyRestingHR // ignore: cast_nullable_to_non_nullable
as List<DailyRestingHR>,dailyHrv: null == dailyHrv ? _self._dailyHrv : dailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,previousDailyHrv: null == previousDailyHrv ? _self._previousDailyHrv : previousDailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,baselineDailyHrv: null == baselineDailyHrv ? _self._baselineDailyHrv : baselineDailyHrv // ignore: cast_nullable_to_non_nullable
as List<DailyHrv>,
  ));
}


}

// dart format on
