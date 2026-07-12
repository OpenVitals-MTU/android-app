// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyMetricSeries {

 BodyMetricKind get kind;/// The metric's latest value, or null when it has no reading at all.
 double? get latest;/// Daily latest values feeding the period chart (Kotlin `dailyLatestValues`).
 List<PeriodChartValue> get values;/// Raw samples feeding the DAY-range intraday line, oldest first.
 List<DaySample> get daySamples;
/// Create a copy of BodyMetricSeries
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyMetricSeriesCopyWith<BodyMetricSeries> get copyWith => _$BodyMetricSeriesCopyWithImpl<BodyMetricSeries>(this as BodyMetricSeries, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyMetricSeries&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.latest, latest) || other.latest == latest)&&const DeepCollectionEquality().equals(other.values, values)&&const DeepCollectionEquality().equals(other.daySamples, daySamples));
}


@override
int get hashCode => Object.hash(runtimeType,kind,latest,const DeepCollectionEquality().hash(values),const DeepCollectionEquality().hash(daySamples));

@override
String toString() {
  return 'BodyMetricSeries(kind: $kind, latest: $latest, values: $values, daySamples: $daySamples)';
}


}

/// @nodoc
abstract mixin class $BodyMetricSeriesCopyWith<$Res>  {
  factory $BodyMetricSeriesCopyWith(BodyMetricSeries value, $Res Function(BodyMetricSeries) _then) = _$BodyMetricSeriesCopyWithImpl;
@useResult
$Res call({
 BodyMetricKind kind, double? latest, List<PeriodChartValue> values, List<DaySample> daySamples
});




}
/// @nodoc
class _$BodyMetricSeriesCopyWithImpl<$Res>
    implements $BodyMetricSeriesCopyWith<$Res> {
  _$BodyMetricSeriesCopyWithImpl(this._self, this._then);

  final BodyMetricSeries _self;
  final $Res Function(BodyMetricSeries) _then;

/// Create a copy of BodyMetricSeries
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? kind = null,Object? latest = freezed,Object? values = null,Object? daySamples = null,}) {
  return _then(_self.copyWith(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as BodyMetricKind,latest: freezed == latest ? _self.latest : latest // ignore: cast_nullable_to_non_nullable
as double?,values: null == values ? _self.values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,daySamples: null == daySamples ? _self.daySamples : daySamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyMetricSeries].
extension BodyMetricSeriesPatterns on BodyMetricSeries {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyMetricSeries value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyMetricSeries() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyMetricSeries value)  $default,){
final _that = this;
switch (_that) {
case _BodyMetricSeries():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyMetricSeries value)?  $default,){
final _that = this;
switch (_that) {
case _BodyMetricSeries() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodyMetricKind kind,  double? latest,  List<PeriodChartValue> values,  List<DaySample> daySamples)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyMetricSeries() when $default != null:
return $default(_that.kind,_that.latest,_that.values,_that.daySamples);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodyMetricKind kind,  double? latest,  List<PeriodChartValue> values,  List<DaySample> daySamples)  $default,) {final _that = this;
switch (_that) {
case _BodyMetricSeries():
return $default(_that.kind,_that.latest,_that.values,_that.daySamples);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodyMetricKind kind,  double? latest,  List<PeriodChartValue> values,  List<DaySample> daySamples)?  $default,) {final _that = this;
switch (_that) {
case _BodyMetricSeries() when $default != null:
return $default(_that.kind,_that.latest,_that.values,_that.daySamples);case _:
  return null;

}
}

}

/// @nodoc


class _BodyMetricSeries extends BodyMetricSeries {
  const _BodyMetricSeries({required this.kind, this.latest, required final  List<PeriodChartValue> values, required final  List<DaySample> daySamples}): _values = values,_daySamples = daySamples,super._();
  

@override final  BodyMetricKind kind;
/// The metric's latest value, or null when it has no reading at all.
@override final  double? latest;
/// Daily latest values feeding the period chart (Kotlin `dailyLatestValues`).
 final  List<PeriodChartValue> _values;
/// Daily latest values feeding the period chart (Kotlin `dailyLatestValues`).
@override List<PeriodChartValue> get values {
  if (_values is EqualUnmodifiableListView) return _values;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_values);
}

/// Raw samples feeding the DAY-range intraday line, oldest first.
 final  List<DaySample> _daySamples;
/// Raw samples feeding the DAY-range intraday line, oldest first.
@override List<DaySample> get daySamples {
  if (_daySamples is EqualUnmodifiableListView) return _daySamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_daySamples);
}


/// Create a copy of BodyMetricSeries
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyMetricSeriesCopyWith<_BodyMetricSeries> get copyWith => __$BodyMetricSeriesCopyWithImpl<_BodyMetricSeries>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyMetricSeries&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.latest, latest) || other.latest == latest)&&const DeepCollectionEquality().equals(other._values, _values)&&const DeepCollectionEquality().equals(other._daySamples, _daySamples));
}


@override
int get hashCode => Object.hash(runtimeType,kind,latest,const DeepCollectionEquality().hash(_values),const DeepCollectionEquality().hash(_daySamples));

@override
String toString() {
  return 'BodyMetricSeries(kind: $kind, latest: $latest, values: $values, daySamples: $daySamples)';
}


}

/// @nodoc
abstract mixin class _$BodyMetricSeriesCopyWith<$Res> implements $BodyMetricSeriesCopyWith<$Res> {
  factory _$BodyMetricSeriesCopyWith(_BodyMetricSeries value, $Res Function(_BodyMetricSeries) _then) = __$BodyMetricSeriesCopyWithImpl;
@override @useResult
$Res call({
 BodyMetricKind kind, double? latest, List<PeriodChartValue> values, List<DaySample> daySamples
});




}
/// @nodoc
class __$BodyMetricSeriesCopyWithImpl<$Res>
    implements _$BodyMetricSeriesCopyWith<$Res> {
  __$BodyMetricSeriesCopyWithImpl(this._self, this._then);

  final _BodyMetricSeries _self;
  final $Res Function(_BodyMetricSeries) _then;

/// Create a copy of BodyMetricSeries
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? kind = null,Object? latest = freezed,Object? values = null,Object? daySamples = null,}) {
  return _then(_BodyMetricSeries(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as BodyMetricKind,latest: freezed == latest ? _self.latest : latest // ignore: cast_nullable_to_non_nullable
as double?,values: null == values ? _self._values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,daySamples: null == daySamples ? _self._daySamples : daySamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,
  ));
}


}

/// @nodoc
mixin _$BodyReading {

 BodyMetricKind get kind; double get value; String get source; DateTime get time;/// Set when the entry is an editable OpenVitals manual entry (Kotlin
/// `isOpenVitalsEntry && id.isNotBlank()`); null rows are read-only.
 BodyMeasurementType? get editType; String? get editId;
/// Create a copy of BodyReading
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyReadingCopyWith<BodyReading> get copyWith => _$BodyReadingCopyWithImpl<BodyReading>(this as BodyReading, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyReading&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.value, value) || other.value == value)&&(identical(other.source, source) || other.source == source)&&(identical(other.time, time) || other.time == time)&&(identical(other.editType, editType) || other.editType == editType)&&(identical(other.editId, editId) || other.editId == editId));
}


@override
int get hashCode => Object.hash(runtimeType,kind,value,source,time,editType,editId);

@override
String toString() {
  return 'BodyReading(kind: $kind, value: $value, source: $source, time: $time, editType: $editType, editId: $editId)';
}


}

/// @nodoc
abstract mixin class $BodyReadingCopyWith<$Res>  {
  factory $BodyReadingCopyWith(BodyReading value, $Res Function(BodyReading) _then) = _$BodyReadingCopyWithImpl;
@useResult
$Res call({
 BodyMetricKind kind, double value, String source, DateTime time, BodyMeasurementType? editType, String? editId
});




}
/// @nodoc
class _$BodyReadingCopyWithImpl<$Res>
    implements $BodyReadingCopyWith<$Res> {
  _$BodyReadingCopyWithImpl(this._self, this._then);

  final BodyReading _self;
  final $Res Function(BodyReading) _then;

/// Create a copy of BodyReading
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? kind = null,Object? value = null,Object? source = null,Object? time = null,Object? editType = freezed,Object? editId = freezed,}) {
  return _then(_self.copyWith(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as BodyMetricKind,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,editType: freezed == editType ? _self.editType : editType // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType?,editId: freezed == editId ? _self.editId : editId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyReading].
extension BodyReadingPatterns on BodyReading {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyReading value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyReading() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyReading value)  $default,){
final _that = this;
switch (_that) {
case _BodyReading():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyReading value)?  $default,){
final _that = this;
switch (_that) {
case _BodyReading() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodyMetricKind kind,  double value,  String source,  DateTime time,  BodyMeasurementType? editType,  String? editId)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyReading() when $default != null:
return $default(_that.kind,_that.value,_that.source,_that.time,_that.editType,_that.editId);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodyMetricKind kind,  double value,  String source,  DateTime time,  BodyMeasurementType? editType,  String? editId)  $default,) {final _that = this;
switch (_that) {
case _BodyReading():
return $default(_that.kind,_that.value,_that.source,_that.time,_that.editType,_that.editId);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodyMetricKind kind,  double value,  String source,  DateTime time,  BodyMeasurementType? editType,  String? editId)?  $default,) {final _that = this;
switch (_that) {
case _BodyReading() when $default != null:
return $default(_that.kind,_that.value,_that.source,_that.time,_that.editType,_that.editId);case _:
  return null;

}
}

}

/// @nodoc


class _BodyReading extends BodyReading {
  const _BodyReading({required this.kind, required this.value, required this.source, required this.time, this.editType, this.editId}): super._();
  

@override final  BodyMetricKind kind;
@override final  double value;
@override final  String source;
@override final  DateTime time;
/// Set when the entry is an editable OpenVitals manual entry (Kotlin
/// `isOpenVitalsEntry && id.isNotBlank()`); null rows are read-only.
@override final  BodyMeasurementType? editType;
@override final  String? editId;

/// Create a copy of BodyReading
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyReadingCopyWith<_BodyReading> get copyWith => __$BodyReadingCopyWithImpl<_BodyReading>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyReading&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.value, value) || other.value == value)&&(identical(other.source, source) || other.source == source)&&(identical(other.time, time) || other.time == time)&&(identical(other.editType, editType) || other.editType == editType)&&(identical(other.editId, editId) || other.editId == editId));
}


@override
int get hashCode => Object.hash(runtimeType,kind,value,source,time,editType,editId);

@override
String toString() {
  return 'BodyReading(kind: $kind, value: $value, source: $source, time: $time, editType: $editType, editId: $editId)';
}


}

/// @nodoc
abstract mixin class _$BodyReadingCopyWith<$Res> implements $BodyReadingCopyWith<$Res> {
  factory _$BodyReadingCopyWith(_BodyReading value, $Res Function(_BodyReading) _then) = __$BodyReadingCopyWithImpl;
@override @useResult
$Res call({
 BodyMetricKind kind, double value, String source, DateTime time, BodyMeasurementType? editType, String? editId
});




}
/// @nodoc
class __$BodyReadingCopyWithImpl<$Res>
    implements _$BodyReadingCopyWith<$Res> {
  __$BodyReadingCopyWithImpl(this._self, this._then);

  final _BodyReading _self;
  final $Res Function(_BodyReading) _then;

/// Create a copy of BodyReading
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? kind = null,Object? value = null,Object? source = null,Object? time = null,Object? editType = freezed,Object? editId = freezed,}) {
  return _then(_BodyReading(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as BodyMetricKind,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,editType: freezed == editType ? _self.editType : editType // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType?,editId: freezed == editId ? _self.editId : editId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

/// @nodoc
mixin _$BodySummary {

 double? get heightCm; double? get latestWeightKg; double? get firstWeightKg; double? get weightChangeKg; double? get latestBodyFatPercent; double? get latestHeightCm; double? get latestLeanMassKg; double? get latestBmrKcal; double? get latestBoneMassKg; double? get latestBodyWaterMassKg; double? get bmi; double? get ffmi; double? get adjustedFfmi;
/// Create a copy of BodySummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodySummaryCopyWith<BodySummary> get copyWith => _$BodySummaryCopyWithImpl<BodySummary>(this as BodySummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodySummary&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.latestWeightKg, latestWeightKg) || other.latestWeightKg == latestWeightKg)&&(identical(other.firstWeightKg, firstWeightKg) || other.firstWeightKg == firstWeightKg)&&(identical(other.weightChangeKg, weightChangeKg) || other.weightChangeKg == weightChangeKg)&&(identical(other.latestBodyFatPercent, latestBodyFatPercent) || other.latestBodyFatPercent == latestBodyFatPercent)&&(identical(other.latestHeightCm, latestHeightCm) || other.latestHeightCm == latestHeightCm)&&(identical(other.latestLeanMassKg, latestLeanMassKg) || other.latestLeanMassKg == latestLeanMassKg)&&(identical(other.latestBmrKcal, latestBmrKcal) || other.latestBmrKcal == latestBmrKcal)&&(identical(other.latestBoneMassKg, latestBoneMassKg) || other.latestBoneMassKg == latestBoneMassKg)&&(identical(other.latestBodyWaterMassKg, latestBodyWaterMassKg) || other.latestBodyWaterMassKg == latestBodyWaterMassKg)&&(identical(other.bmi, bmi) || other.bmi == bmi)&&(identical(other.ffmi, ffmi) || other.ffmi == ffmi)&&(identical(other.adjustedFfmi, adjustedFfmi) || other.adjustedFfmi == adjustedFfmi));
}


@override
int get hashCode => Object.hash(runtimeType,heightCm,latestWeightKg,firstWeightKg,weightChangeKg,latestBodyFatPercent,latestHeightCm,latestLeanMassKg,latestBmrKcal,latestBoneMassKg,latestBodyWaterMassKg,bmi,ffmi,adjustedFfmi);

@override
String toString() {
  return 'BodySummary(heightCm: $heightCm, latestWeightKg: $latestWeightKg, firstWeightKg: $firstWeightKg, weightChangeKg: $weightChangeKg, latestBodyFatPercent: $latestBodyFatPercent, latestHeightCm: $latestHeightCm, latestLeanMassKg: $latestLeanMassKg, latestBmrKcal: $latestBmrKcal, latestBoneMassKg: $latestBoneMassKg, latestBodyWaterMassKg: $latestBodyWaterMassKg, bmi: $bmi, ffmi: $ffmi, adjustedFfmi: $adjustedFfmi)';
}


}

/// @nodoc
abstract mixin class $BodySummaryCopyWith<$Res>  {
  factory $BodySummaryCopyWith(BodySummary value, $Res Function(BodySummary) _then) = _$BodySummaryCopyWithImpl;
@useResult
$Res call({
 double? heightCm, double? latestWeightKg, double? firstWeightKg, double? weightChangeKg, double? latestBodyFatPercent, double? latestHeightCm, double? latestLeanMassKg, double? latestBmrKcal, double? latestBoneMassKg, double? latestBodyWaterMassKg, double? bmi, double? ffmi, double? adjustedFfmi
});




}
/// @nodoc
class _$BodySummaryCopyWithImpl<$Res>
    implements $BodySummaryCopyWith<$Res> {
  _$BodySummaryCopyWithImpl(this._self, this._then);

  final BodySummary _self;
  final $Res Function(BodySummary) _then;

/// Create a copy of BodySummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? heightCm = freezed,Object? latestWeightKg = freezed,Object? firstWeightKg = freezed,Object? weightChangeKg = freezed,Object? latestBodyFatPercent = freezed,Object? latestHeightCm = freezed,Object? latestLeanMassKg = freezed,Object? latestBmrKcal = freezed,Object? latestBoneMassKg = freezed,Object? latestBodyWaterMassKg = freezed,Object? bmi = freezed,Object? ffmi = freezed,Object? adjustedFfmi = freezed,}) {
  return _then(_self.copyWith(
heightCm: freezed == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double?,latestWeightKg: freezed == latestWeightKg ? _self.latestWeightKg : latestWeightKg // ignore: cast_nullable_to_non_nullable
as double?,firstWeightKg: freezed == firstWeightKg ? _self.firstWeightKg : firstWeightKg // ignore: cast_nullable_to_non_nullable
as double?,weightChangeKg: freezed == weightChangeKg ? _self.weightChangeKg : weightChangeKg // ignore: cast_nullable_to_non_nullable
as double?,latestBodyFatPercent: freezed == latestBodyFatPercent ? _self.latestBodyFatPercent : latestBodyFatPercent // ignore: cast_nullable_to_non_nullable
as double?,latestHeightCm: freezed == latestHeightCm ? _self.latestHeightCm : latestHeightCm // ignore: cast_nullable_to_non_nullable
as double?,latestLeanMassKg: freezed == latestLeanMassKg ? _self.latestLeanMassKg : latestLeanMassKg // ignore: cast_nullable_to_non_nullable
as double?,latestBmrKcal: freezed == latestBmrKcal ? _self.latestBmrKcal : latestBmrKcal // ignore: cast_nullable_to_non_nullable
as double?,latestBoneMassKg: freezed == latestBoneMassKg ? _self.latestBoneMassKg : latestBoneMassKg // ignore: cast_nullable_to_non_nullable
as double?,latestBodyWaterMassKg: freezed == latestBodyWaterMassKg ? _self.latestBodyWaterMassKg : latestBodyWaterMassKg // ignore: cast_nullable_to_non_nullable
as double?,bmi: freezed == bmi ? _self.bmi : bmi // ignore: cast_nullable_to_non_nullable
as double?,ffmi: freezed == ffmi ? _self.ffmi : ffmi // ignore: cast_nullable_to_non_nullable
as double?,adjustedFfmi: freezed == adjustedFfmi ? _self.adjustedFfmi : adjustedFfmi // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [BodySummary].
extension BodySummaryPatterns on BodySummary {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodySummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodySummary() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodySummary value)  $default,){
final _that = this;
switch (_that) {
case _BodySummary():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodySummary value)?  $default,){
final _that = this;
switch (_that) {
case _BodySummary() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double? heightCm,  double? latestWeightKg,  double? firstWeightKg,  double? weightChangeKg,  double? latestBodyFatPercent,  double? latestHeightCm,  double? latestLeanMassKg,  double? latestBmrKcal,  double? latestBoneMassKg,  double? latestBodyWaterMassKg,  double? bmi,  double? ffmi,  double? adjustedFfmi)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodySummary() when $default != null:
return $default(_that.heightCm,_that.latestWeightKg,_that.firstWeightKg,_that.weightChangeKg,_that.latestBodyFatPercent,_that.latestHeightCm,_that.latestLeanMassKg,_that.latestBmrKcal,_that.latestBoneMassKg,_that.latestBodyWaterMassKg,_that.bmi,_that.ffmi,_that.adjustedFfmi);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double? heightCm,  double? latestWeightKg,  double? firstWeightKg,  double? weightChangeKg,  double? latestBodyFatPercent,  double? latestHeightCm,  double? latestLeanMassKg,  double? latestBmrKcal,  double? latestBoneMassKg,  double? latestBodyWaterMassKg,  double? bmi,  double? ffmi,  double? adjustedFfmi)  $default,) {final _that = this;
switch (_that) {
case _BodySummary():
return $default(_that.heightCm,_that.latestWeightKg,_that.firstWeightKg,_that.weightChangeKg,_that.latestBodyFatPercent,_that.latestHeightCm,_that.latestLeanMassKg,_that.latestBmrKcal,_that.latestBoneMassKg,_that.latestBodyWaterMassKg,_that.bmi,_that.ffmi,_that.adjustedFfmi);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double? heightCm,  double? latestWeightKg,  double? firstWeightKg,  double? weightChangeKg,  double? latestBodyFatPercent,  double? latestHeightCm,  double? latestLeanMassKg,  double? latestBmrKcal,  double? latestBoneMassKg,  double? latestBodyWaterMassKg,  double? bmi,  double? ffmi,  double? adjustedFfmi)?  $default,) {final _that = this;
switch (_that) {
case _BodySummary() when $default != null:
return $default(_that.heightCm,_that.latestWeightKg,_that.firstWeightKg,_that.weightChangeKg,_that.latestBodyFatPercent,_that.latestHeightCm,_that.latestLeanMassKg,_that.latestBmrKcal,_that.latestBoneMassKg,_that.latestBodyWaterMassKg,_that.bmi,_that.ffmi,_that.adjustedFfmi);case _:
  return null;

}
}

}

/// @nodoc


class _BodySummary implements BodySummary {
  const _BodySummary({this.heightCm, this.latestWeightKg, this.firstWeightKg, this.weightChangeKg, this.latestBodyFatPercent, this.latestHeightCm, this.latestLeanMassKg, this.latestBmrKcal, this.latestBoneMassKg, this.latestBodyWaterMassKg, this.bmi, this.ffmi, this.adjustedFfmi});
  

@override final  double? heightCm;
@override final  double? latestWeightKg;
@override final  double? firstWeightKg;
@override final  double? weightChangeKg;
@override final  double? latestBodyFatPercent;
@override final  double? latestHeightCm;
@override final  double? latestLeanMassKg;
@override final  double? latestBmrKcal;
@override final  double? latestBoneMassKg;
@override final  double? latestBodyWaterMassKg;
@override final  double? bmi;
@override final  double? ffmi;
@override final  double? adjustedFfmi;

/// Create a copy of BodySummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodySummaryCopyWith<_BodySummary> get copyWith => __$BodySummaryCopyWithImpl<_BodySummary>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodySummary&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.latestWeightKg, latestWeightKg) || other.latestWeightKg == latestWeightKg)&&(identical(other.firstWeightKg, firstWeightKg) || other.firstWeightKg == firstWeightKg)&&(identical(other.weightChangeKg, weightChangeKg) || other.weightChangeKg == weightChangeKg)&&(identical(other.latestBodyFatPercent, latestBodyFatPercent) || other.latestBodyFatPercent == latestBodyFatPercent)&&(identical(other.latestHeightCm, latestHeightCm) || other.latestHeightCm == latestHeightCm)&&(identical(other.latestLeanMassKg, latestLeanMassKg) || other.latestLeanMassKg == latestLeanMassKg)&&(identical(other.latestBmrKcal, latestBmrKcal) || other.latestBmrKcal == latestBmrKcal)&&(identical(other.latestBoneMassKg, latestBoneMassKg) || other.latestBoneMassKg == latestBoneMassKg)&&(identical(other.latestBodyWaterMassKg, latestBodyWaterMassKg) || other.latestBodyWaterMassKg == latestBodyWaterMassKg)&&(identical(other.bmi, bmi) || other.bmi == bmi)&&(identical(other.ffmi, ffmi) || other.ffmi == ffmi)&&(identical(other.adjustedFfmi, adjustedFfmi) || other.adjustedFfmi == adjustedFfmi));
}


@override
int get hashCode => Object.hash(runtimeType,heightCm,latestWeightKg,firstWeightKg,weightChangeKg,latestBodyFatPercent,latestHeightCm,latestLeanMassKg,latestBmrKcal,latestBoneMassKg,latestBodyWaterMassKg,bmi,ffmi,adjustedFfmi);

@override
String toString() {
  return 'BodySummary(heightCm: $heightCm, latestWeightKg: $latestWeightKg, firstWeightKg: $firstWeightKg, weightChangeKg: $weightChangeKg, latestBodyFatPercent: $latestBodyFatPercent, latestHeightCm: $latestHeightCm, latestLeanMassKg: $latestLeanMassKg, latestBmrKcal: $latestBmrKcal, latestBoneMassKg: $latestBoneMassKg, latestBodyWaterMassKg: $latestBodyWaterMassKg, bmi: $bmi, ffmi: $ffmi, adjustedFfmi: $adjustedFfmi)';
}


}

/// @nodoc
abstract mixin class _$BodySummaryCopyWith<$Res> implements $BodySummaryCopyWith<$Res> {
  factory _$BodySummaryCopyWith(_BodySummary value, $Res Function(_BodySummary) _then) = __$BodySummaryCopyWithImpl;
@override @useResult
$Res call({
 double? heightCm, double? latestWeightKg, double? firstWeightKg, double? weightChangeKg, double? latestBodyFatPercent, double? latestHeightCm, double? latestLeanMassKg, double? latestBmrKcal, double? latestBoneMassKg, double? latestBodyWaterMassKg, double? bmi, double? ffmi, double? adjustedFfmi
});




}
/// @nodoc
class __$BodySummaryCopyWithImpl<$Res>
    implements _$BodySummaryCopyWith<$Res> {
  __$BodySummaryCopyWithImpl(this._self, this._then);

  final _BodySummary _self;
  final $Res Function(_BodySummary) _then;

/// Create a copy of BodySummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? heightCm = freezed,Object? latestWeightKg = freezed,Object? firstWeightKg = freezed,Object? weightChangeKg = freezed,Object? latestBodyFatPercent = freezed,Object? latestHeightCm = freezed,Object? latestLeanMassKg = freezed,Object? latestBmrKcal = freezed,Object? latestBoneMassKg = freezed,Object? latestBodyWaterMassKg = freezed,Object? bmi = freezed,Object? ffmi = freezed,Object? adjustedFfmi = freezed,}) {
  return _then(_BodySummary(
heightCm: freezed == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double?,latestWeightKg: freezed == latestWeightKg ? _self.latestWeightKg : latestWeightKg // ignore: cast_nullable_to_non_nullable
as double?,firstWeightKg: freezed == firstWeightKg ? _self.firstWeightKg : firstWeightKg // ignore: cast_nullable_to_non_nullable
as double?,weightChangeKg: freezed == weightChangeKg ? _self.weightChangeKg : weightChangeKg // ignore: cast_nullable_to_non_nullable
as double?,latestBodyFatPercent: freezed == latestBodyFatPercent ? _self.latestBodyFatPercent : latestBodyFatPercent // ignore: cast_nullable_to_non_nullable
as double?,latestHeightCm: freezed == latestHeightCm ? _self.latestHeightCm : latestHeightCm // ignore: cast_nullable_to_non_nullable
as double?,latestLeanMassKg: freezed == latestLeanMassKg ? _self.latestLeanMassKg : latestLeanMassKg // ignore: cast_nullable_to_non_nullable
as double?,latestBmrKcal: freezed == latestBmrKcal ? _self.latestBmrKcal : latestBmrKcal // ignore: cast_nullable_to_non_nullable
as double?,latestBoneMassKg: freezed == latestBoneMassKg ? _self.latestBoneMassKg : latestBoneMassKg // ignore: cast_nullable_to_non_nullable
as double?,latestBodyWaterMassKg: freezed == latestBodyWaterMassKg ? _self.latestBodyWaterMassKg : latestBodyWaterMassKg // ignore: cast_nullable_to_non_nullable
as double?,bmi: freezed == bmi ? _self.bmi : bmi // ignore: cast_nullable_to_non_nullable
as double?,ffmi: freezed == ffmi ? _self.ffmi : ffmi // ignore: cast_nullable_to_non_nullable
as double?,adjustedFfmi: freezed == adjustedFfmi ? _self.adjustedFfmi : adjustedFfmi // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$BodyDisplay {

 BodySummary get summary; List<BodyMetricSeries> get metrics;/// The metrics with values in the period — the ones that earn a chart.
 List<BodyMetricSeries> get trackedMetrics; List<BodyReading> get readingsNewestFirst;/// The readings of each tracked day, so the pinned-day section looks its day
/// up rather than scanning the whole list for it.
 Map<LocalDate, List<BodyReading>> get readingsByDate;/// Kotlin `bodyContent`: false when the whole period has no body data, which
/// is when the screen shows its placeholder.
 bool get hasAnyBodyData;
/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyDisplayCopyWith<BodyDisplay> get copyWith => _$BodyDisplayCopyWithImpl<BodyDisplay>(this as BodyDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyDisplay&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other.metrics, metrics)&&const DeepCollectionEquality().equals(other.trackedMetrics, trackedMetrics)&&const DeepCollectionEquality().equals(other.readingsNewestFirst, readingsNewestFirst)&&const DeepCollectionEquality().equals(other.readingsByDate, readingsByDate)&&(identical(other.hasAnyBodyData, hasAnyBodyData) || other.hasAnyBodyData == hasAnyBodyData));
}


@override
int get hashCode => Object.hash(runtimeType,summary,const DeepCollectionEquality().hash(metrics),const DeepCollectionEquality().hash(trackedMetrics),const DeepCollectionEquality().hash(readingsNewestFirst),const DeepCollectionEquality().hash(readingsByDate),hasAnyBodyData);

@override
String toString() {
  return 'BodyDisplay(summary: $summary, metrics: $metrics, trackedMetrics: $trackedMetrics, readingsNewestFirst: $readingsNewestFirst, readingsByDate: $readingsByDate, hasAnyBodyData: $hasAnyBodyData)';
}


}

/// @nodoc
abstract mixin class $BodyDisplayCopyWith<$Res>  {
  factory $BodyDisplayCopyWith(BodyDisplay value, $Res Function(BodyDisplay) _then) = _$BodyDisplayCopyWithImpl;
@useResult
$Res call({
 BodySummary summary, List<BodyMetricSeries> metrics, List<BodyMetricSeries> trackedMetrics, List<BodyReading> readingsNewestFirst, Map<LocalDate, List<BodyReading>> readingsByDate, bool hasAnyBodyData
});


$BodySummaryCopyWith<$Res> get summary;

}
/// @nodoc
class _$BodyDisplayCopyWithImpl<$Res>
    implements $BodyDisplayCopyWith<$Res> {
  _$BodyDisplayCopyWithImpl(this._self, this._then);

  final BodyDisplay _self;
  final $Res Function(BodyDisplay) _then;

/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? summary = null,Object? metrics = null,Object? trackedMetrics = null,Object? readingsNewestFirst = null,Object? readingsByDate = null,Object? hasAnyBodyData = null,}) {
  return _then(_self.copyWith(
summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as BodySummary,metrics: null == metrics ? _self.metrics : metrics // ignore: cast_nullable_to_non_nullable
as List<BodyMetricSeries>,trackedMetrics: null == trackedMetrics ? _self.trackedMetrics : trackedMetrics // ignore: cast_nullable_to_non_nullable
as List<BodyMetricSeries>,readingsNewestFirst: null == readingsNewestFirst ? _self.readingsNewestFirst : readingsNewestFirst // ignore: cast_nullable_to_non_nullable
as List<BodyReading>,readingsByDate: null == readingsByDate ? _self.readingsByDate : readingsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<BodyReading>>,hasAnyBodyData: null == hasAnyBodyData ? _self.hasAnyBodyData : hasAnyBodyData // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}
/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodySummaryCopyWith<$Res> get summary {
  
  return $BodySummaryCopyWith<$Res>(_self.summary, (value) {
    return _then(_self.copyWith(summary: value));
  });
}
}


/// Adds pattern-matching-related methods to [BodyDisplay].
extension BodyDisplayPatterns on BodyDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyDisplay value)  $default,){
final _that = this;
switch (_that) {
case _BodyDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _BodyDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodySummary summary,  List<BodyMetricSeries> metrics,  List<BodyMetricSeries> trackedMetrics,  List<BodyReading> readingsNewestFirst,  Map<LocalDate, List<BodyReading>> readingsByDate,  bool hasAnyBodyData)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyDisplay() when $default != null:
return $default(_that.summary,_that.metrics,_that.trackedMetrics,_that.readingsNewestFirst,_that.readingsByDate,_that.hasAnyBodyData);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodySummary summary,  List<BodyMetricSeries> metrics,  List<BodyMetricSeries> trackedMetrics,  List<BodyReading> readingsNewestFirst,  Map<LocalDate, List<BodyReading>> readingsByDate,  bool hasAnyBodyData)  $default,) {final _that = this;
switch (_that) {
case _BodyDisplay():
return $default(_that.summary,_that.metrics,_that.trackedMetrics,_that.readingsNewestFirst,_that.readingsByDate,_that.hasAnyBodyData);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodySummary summary,  List<BodyMetricSeries> metrics,  List<BodyMetricSeries> trackedMetrics,  List<BodyReading> readingsNewestFirst,  Map<LocalDate, List<BodyReading>> readingsByDate,  bool hasAnyBodyData)?  $default,) {final _that = this;
switch (_that) {
case _BodyDisplay() when $default != null:
return $default(_that.summary,_that.metrics,_that.trackedMetrics,_that.readingsNewestFirst,_that.readingsByDate,_that.hasAnyBodyData);case _:
  return null;

}
}

}

/// @nodoc


class _BodyDisplay implements BodyDisplay {
  const _BodyDisplay({required this.summary, required final  List<BodyMetricSeries> metrics, required final  List<BodyMetricSeries> trackedMetrics, required final  List<BodyReading> readingsNewestFirst, required final  Map<LocalDate, List<BodyReading>> readingsByDate, required this.hasAnyBodyData}): _metrics = metrics,_trackedMetrics = trackedMetrics,_readingsNewestFirst = readingsNewestFirst,_readingsByDate = readingsByDate;
  

@override final  BodySummary summary;
 final  List<BodyMetricSeries> _metrics;
@override List<BodyMetricSeries> get metrics {
  if (_metrics is EqualUnmodifiableListView) return _metrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_metrics);
}

/// The metrics with values in the period — the ones that earn a chart.
 final  List<BodyMetricSeries> _trackedMetrics;
/// The metrics with values in the period — the ones that earn a chart.
@override List<BodyMetricSeries> get trackedMetrics {
  if (_trackedMetrics is EqualUnmodifiableListView) return _trackedMetrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_trackedMetrics);
}

 final  List<BodyReading> _readingsNewestFirst;
@override List<BodyReading> get readingsNewestFirst {
  if (_readingsNewestFirst is EqualUnmodifiableListView) return _readingsNewestFirst;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_readingsNewestFirst);
}

/// The readings of each tracked day, so the pinned-day section looks its day
/// up rather than scanning the whole list for it.
 final  Map<LocalDate, List<BodyReading>> _readingsByDate;
/// The readings of each tracked day, so the pinned-day section looks its day
/// up rather than scanning the whole list for it.
@override Map<LocalDate, List<BodyReading>> get readingsByDate {
  if (_readingsByDate is EqualUnmodifiableMapView) return _readingsByDate;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_readingsByDate);
}

/// Kotlin `bodyContent`: false when the whole period has no body data, which
/// is when the screen shows its placeholder.
@override final  bool hasAnyBodyData;

/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyDisplayCopyWith<_BodyDisplay> get copyWith => __$BodyDisplayCopyWithImpl<_BodyDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyDisplay&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other._metrics, _metrics)&&const DeepCollectionEquality().equals(other._trackedMetrics, _trackedMetrics)&&const DeepCollectionEquality().equals(other._readingsNewestFirst, _readingsNewestFirst)&&const DeepCollectionEquality().equals(other._readingsByDate, _readingsByDate)&&(identical(other.hasAnyBodyData, hasAnyBodyData) || other.hasAnyBodyData == hasAnyBodyData));
}


@override
int get hashCode => Object.hash(runtimeType,summary,const DeepCollectionEquality().hash(_metrics),const DeepCollectionEquality().hash(_trackedMetrics),const DeepCollectionEquality().hash(_readingsNewestFirst),const DeepCollectionEquality().hash(_readingsByDate),hasAnyBodyData);

@override
String toString() {
  return 'BodyDisplay(summary: $summary, metrics: $metrics, trackedMetrics: $trackedMetrics, readingsNewestFirst: $readingsNewestFirst, readingsByDate: $readingsByDate, hasAnyBodyData: $hasAnyBodyData)';
}


}

/// @nodoc
abstract mixin class _$BodyDisplayCopyWith<$Res> implements $BodyDisplayCopyWith<$Res> {
  factory _$BodyDisplayCopyWith(_BodyDisplay value, $Res Function(_BodyDisplay) _then) = __$BodyDisplayCopyWithImpl;
@override @useResult
$Res call({
 BodySummary summary, List<BodyMetricSeries> metrics, List<BodyMetricSeries> trackedMetrics, List<BodyReading> readingsNewestFirst, Map<LocalDate, List<BodyReading>> readingsByDate, bool hasAnyBodyData
});


@override $BodySummaryCopyWith<$Res> get summary;

}
/// @nodoc
class __$BodyDisplayCopyWithImpl<$Res>
    implements _$BodyDisplayCopyWith<$Res> {
  __$BodyDisplayCopyWithImpl(this._self, this._then);

  final _BodyDisplay _self;
  final $Res Function(_BodyDisplay) _then;

/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? summary = null,Object? metrics = null,Object? trackedMetrics = null,Object? readingsNewestFirst = null,Object? readingsByDate = null,Object? hasAnyBodyData = null,}) {
  return _then(_BodyDisplay(
summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as BodySummary,metrics: null == metrics ? _self._metrics : metrics // ignore: cast_nullable_to_non_nullable
as List<BodyMetricSeries>,trackedMetrics: null == trackedMetrics ? _self._trackedMetrics : trackedMetrics // ignore: cast_nullable_to_non_nullable
as List<BodyMetricSeries>,readingsNewestFirst: null == readingsNewestFirst ? _self._readingsNewestFirst : readingsNewestFirst // ignore: cast_nullable_to_non_nullable
as List<BodyReading>,readingsByDate: null == readingsByDate ? _self._readingsByDate : readingsByDate // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<BodyReading>>,hasAnyBodyData: null == hasAnyBodyData ? _self.hasAnyBodyData : hasAnyBodyData // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

/// Create a copy of BodyDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodySummaryCopyWith<$Res> get summary {
  
  return $BodySummaryCopyWith<$Res>(_self.summary, (value) {
    return _then(_self.copyWith(summary: value));
  });
}
}

// dart format on
