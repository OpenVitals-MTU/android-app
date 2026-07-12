// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cycle_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CycleDisplay {

 bool get hasData; int get periodDays; int get ovulationTestCount; int get bbtReadingCount; int get totalEntryCount; double? get latestBbtCelsius; List<CycleObservation> get observations;
/// Create a copy of CycleDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CycleDisplayCopyWith<CycleDisplay> get copyWith => _$CycleDisplayCopyWithImpl<CycleDisplay>(this as CycleDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CycleDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.periodDays, periodDays) || other.periodDays == periodDays)&&(identical(other.ovulationTestCount, ovulationTestCount) || other.ovulationTestCount == ovulationTestCount)&&(identical(other.bbtReadingCount, bbtReadingCount) || other.bbtReadingCount == bbtReadingCount)&&(identical(other.totalEntryCount, totalEntryCount) || other.totalEntryCount == totalEntryCount)&&(identical(other.latestBbtCelsius, latestBbtCelsius) || other.latestBbtCelsius == latestBbtCelsius)&&const DeepCollectionEquality().equals(other.observations, observations));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,periodDays,ovulationTestCount,bbtReadingCount,totalEntryCount,latestBbtCelsius,const DeepCollectionEquality().hash(observations));

@override
String toString() {
  return 'CycleDisplay(hasData: $hasData, periodDays: $periodDays, ovulationTestCount: $ovulationTestCount, bbtReadingCount: $bbtReadingCount, totalEntryCount: $totalEntryCount, latestBbtCelsius: $latestBbtCelsius, observations: $observations)';
}


}

/// @nodoc
abstract mixin class $CycleDisplayCopyWith<$Res>  {
  factory $CycleDisplayCopyWith(CycleDisplay value, $Res Function(CycleDisplay) _then) = _$CycleDisplayCopyWithImpl;
@useResult
$Res call({
 bool hasData, int periodDays, int ovulationTestCount, int bbtReadingCount, int totalEntryCount, double? latestBbtCelsius, List<CycleObservation> observations
});




}
/// @nodoc
class _$CycleDisplayCopyWithImpl<$Res>
    implements $CycleDisplayCopyWith<$Res> {
  _$CycleDisplayCopyWithImpl(this._self, this._then);

  final CycleDisplay _self;
  final $Res Function(CycleDisplay) _then;

/// Create a copy of CycleDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? hasData = null,Object? periodDays = null,Object? ovulationTestCount = null,Object? bbtReadingCount = null,Object? totalEntryCount = null,Object? latestBbtCelsius = freezed,Object? observations = null,}) {
  return _then(_self.copyWith(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,periodDays: null == periodDays ? _self.periodDays : periodDays // ignore: cast_nullable_to_non_nullable
as int,ovulationTestCount: null == ovulationTestCount ? _self.ovulationTestCount : ovulationTestCount // ignore: cast_nullable_to_non_nullable
as int,bbtReadingCount: null == bbtReadingCount ? _self.bbtReadingCount : bbtReadingCount // ignore: cast_nullable_to_non_nullable
as int,totalEntryCount: null == totalEntryCount ? _self.totalEntryCount : totalEntryCount // ignore: cast_nullable_to_non_nullable
as int,latestBbtCelsius: freezed == latestBbtCelsius ? _self.latestBbtCelsius : latestBbtCelsius // ignore: cast_nullable_to_non_nullable
as double?,observations: null == observations ? _self.observations : observations // ignore: cast_nullable_to_non_nullable
as List<CycleObservation>,
  ));
}

}


/// Adds pattern-matching-related methods to [CycleDisplay].
extension CycleDisplayPatterns on CycleDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CycleDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CycleDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CycleDisplay value)  $default,){
final _that = this;
switch (_that) {
case _CycleDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CycleDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _CycleDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool hasData,  int periodDays,  int ovulationTestCount,  int bbtReadingCount,  int totalEntryCount,  double? latestBbtCelsius,  List<CycleObservation> observations)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CycleDisplay() when $default != null:
return $default(_that.hasData,_that.periodDays,_that.ovulationTestCount,_that.bbtReadingCount,_that.totalEntryCount,_that.latestBbtCelsius,_that.observations);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool hasData,  int periodDays,  int ovulationTestCount,  int bbtReadingCount,  int totalEntryCount,  double? latestBbtCelsius,  List<CycleObservation> observations)  $default,) {final _that = this;
switch (_that) {
case _CycleDisplay():
return $default(_that.hasData,_that.periodDays,_that.ovulationTestCount,_that.bbtReadingCount,_that.totalEntryCount,_that.latestBbtCelsius,_that.observations);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool hasData,  int periodDays,  int ovulationTestCount,  int bbtReadingCount,  int totalEntryCount,  double? latestBbtCelsius,  List<CycleObservation> observations)?  $default,) {final _that = this;
switch (_that) {
case _CycleDisplay() when $default != null:
return $default(_that.hasData,_that.periodDays,_that.ovulationTestCount,_that.bbtReadingCount,_that.totalEntryCount,_that.latestBbtCelsius,_that.observations);case _:
  return null;

}
}

}

/// @nodoc


class _CycleDisplay implements CycleDisplay {
  const _CycleDisplay({required this.hasData, required this.periodDays, required this.ovulationTestCount, required this.bbtReadingCount, required this.totalEntryCount, this.latestBbtCelsius, required final  List<CycleObservation> observations}): _observations = observations;
  

@override final  bool hasData;
@override final  int periodDays;
@override final  int ovulationTestCount;
@override final  int bbtReadingCount;
@override final  int totalEntryCount;
@override final  double? latestBbtCelsius;
 final  List<CycleObservation> _observations;
@override List<CycleObservation> get observations {
  if (_observations is EqualUnmodifiableListView) return _observations;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_observations);
}


/// Create a copy of CycleDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CycleDisplayCopyWith<_CycleDisplay> get copyWith => __$CycleDisplayCopyWithImpl<_CycleDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CycleDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.periodDays, periodDays) || other.periodDays == periodDays)&&(identical(other.ovulationTestCount, ovulationTestCount) || other.ovulationTestCount == ovulationTestCount)&&(identical(other.bbtReadingCount, bbtReadingCount) || other.bbtReadingCount == bbtReadingCount)&&(identical(other.totalEntryCount, totalEntryCount) || other.totalEntryCount == totalEntryCount)&&(identical(other.latestBbtCelsius, latestBbtCelsius) || other.latestBbtCelsius == latestBbtCelsius)&&const DeepCollectionEquality().equals(other._observations, _observations));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,periodDays,ovulationTestCount,bbtReadingCount,totalEntryCount,latestBbtCelsius,const DeepCollectionEquality().hash(_observations));

@override
String toString() {
  return 'CycleDisplay(hasData: $hasData, periodDays: $periodDays, ovulationTestCount: $ovulationTestCount, bbtReadingCount: $bbtReadingCount, totalEntryCount: $totalEntryCount, latestBbtCelsius: $latestBbtCelsius, observations: $observations)';
}


}

/// @nodoc
abstract mixin class _$CycleDisplayCopyWith<$Res> implements $CycleDisplayCopyWith<$Res> {
  factory _$CycleDisplayCopyWith(_CycleDisplay value, $Res Function(_CycleDisplay) _then) = __$CycleDisplayCopyWithImpl;
@override @useResult
$Res call({
 bool hasData, int periodDays, int ovulationTestCount, int bbtReadingCount, int totalEntryCount, double? latestBbtCelsius, List<CycleObservation> observations
});




}
/// @nodoc
class __$CycleDisplayCopyWithImpl<$Res>
    implements _$CycleDisplayCopyWith<$Res> {
  __$CycleDisplayCopyWithImpl(this._self, this._then);

  final _CycleDisplay _self;
  final $Res Function(_CycleDisplay) _then;

/// Create a copy of CycleDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? hasData = null,Object? periodDays = null,Object? ovulationTestCount = null,Object? bbtReadingCount = null,Object? totalEntryCount = null,Object? latestBbtCelsius = freezed,Object? observations = null,}) {
  return _then(_CycleDisplay(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,periodDays: null == periodDays ? _self.periodDays : periodDays // ignore: cast_nullable_to_non_nullable
as int,ovulationTestCount: null == ovulationTestCount ? _self.ovulationTestCount : ovulationTestCount // ignore: cast_nullable_to_non_nullable
as int,bbtReadingCount: null == bbtReadingCount ? _self.bbtReadingCount : bbtReadingCount // ignore: cast_nullable_to_non_nullable
as int,totalEntryCount: null == totalEntryCount ? _self.totalEntryCount : totalEntryCount // ignore: cast_nullable_to_non_nullable
as int,latestBbtCelsius: freezed == latestBbtCelsius ? _self.latestBbtCelsius : latestBbtCelsius // ignore: cast_nullable_to_non_nullable
as double?,observations: null == observations ? _self._observations : observations // ignore: cast_nullable_to_non_nullable
as List<CycleObservation>,
  ));
}


}

/// @nodoc
mixin _$CycleObservation {

 CycleObservationKind get kind; DateTime get time; String get source;/// Menstruation period: its length in whole days (at least one).
 int? get days; int? get flow; int? get ovulationResult; int? get mucusAppearance; int? get mucusSensation; double? get temperatureCelsius; int? get measurementLocation; int? get protectionUsed;
/// Create a copy of CycleObservation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CycleObservationCopyWith<CycleObservation> get copyWith => _$CycleObservationCopyWithImpl<CycleObservation>(this as CycleObservation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CycleObservation&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.time, time) || other.time == time)&&(identical(other.source, source) || other.source == source)&&(identical(other.days, days) || other.days == days)&&(identical(other.flow, flow) || other.flow == flow)&&(identical(other.ovulationResult, ovulationResult) || other.ovulationResult == ovulationResult)&&(identical(other.mucusAppearance, mucusAppearance) || other.mucusAppearance == mucusAppearance)&&(identical(other.mucusSensation, mucusSensation) || other.mucusSensation == mucusSensation)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.protectionUsed, protectionUsed) || other.protectionUsed == protectionUsed));
}


@override
int get hashCode => Object.hash(runtimeType,kind,time,source,days,flow,ovulationResult,mucusAppearance,mucusSensation,temperatureCelsius,measurementLocation,protectionUsed);

@override
String toString() {
  return 'CycleObservation(kind: $kind, time: $time, source: $source, days: $days, flow: $flow, ovulationResult: $ovulationResult, mucusAppearance: $mucusAppearance, mucusSensation: $mucusSensation, temperatureCelsius: $temperatureCelsius, measurementLocation: $measurementLocation, protectionUsed: $protectionUsed)';
}


}

/// @nodoc
abstract mixin class $CycleObservationCopyWith<$Res>  {
  factory $CycleObservationCopyWith(CycleObservation value, $Res Function(CycleObservation) _then) = _$CycleObservationCopyWithImpl;
@useResult
$Res call({
 CycleObservationKind kind, DateTime time, String source, int? days, int? flow, int? ovulationResult, int? mucusAppearance, int? mucusSensation, double? temperatureCelsius, int? measurementLocation, int? protectionUsed
});




}
/// @nodoc
class _$CycleObservationCopyWithImpl<$Res>
    implements $CycleObservationCopyWith<$Res> {
  _$CycleObservationCopyWithImpl(this._self, this._then);

  final CycleObservation _self;
  final $Res Function(CycleObservation) _then;

/// Create a copy of CycleObservation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? kind = null,Object? time = null,Object? source = null,Object? days = freezed,Object? flow = freezed,Object? ovulationResult = freezed,Object? mucusAppearance = freezed,Object? mucusSensation = freezed,Object? temperatureCelsius = freezed,Object? measurementLocation = freezed,Object? protectionUsed = freezed,}) {
  return _then(_self.copyWith(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as CycleObservationKind,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,days: freezed == days ? _self.days : days // ignore: cast_nullable_to_non_nullable
as int?,flow: freezed == flow ? _self.flow : flow // ignore: cast_nullable_to_non_nullable
as int?,ovulationResult: freezed == ovulationResult ? _self.ovulationResult : ovulationResult // ignore: cast_nullable_to_non_nullable
as int?,mucusAppearance: freezed == mucusAppearance ? _self.mucusAppearance : mucusAppearance // ignore: cast_nullable_to_non_nullable
as int?,mucusSensation: freezed == mucusSensation ? _self.mucusSensation : mucusSensation // ignore: cast_nullable_to_non_nullable
as int?,temperatureCelsius: freezed == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,measurementLocation: freezed == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int?,protectionUsed: freezed == protectionUsed ? _self.protectionUsed : protectionUsed // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [CycleObservation].
extension CycleObservationPatterns on CycleObservation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CycleObservation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CycleObservation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CycleObservation value)  $default,){
final _that = this;
switch (_that) {
case _CycleObservation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CycleObservation value)?  $default,){
final _that = this;
switch (_that) {
case _CycleObservation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CycleObservationKind kind,  DateTime time,  String source,  int? days,  int? flow,  int? ovulationResult,  int? mucusAppearance,  int? mucusSensation,  double? temperatureCelsius,  int? measurementLocation,  int? protectionUsed)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CycleObservation() when $default != null:
return $default(_that.kind,_that.time,_that.source,_that.days,_that.flow,_that.ovulationResult,_that.mucusAppearance,_that.mucusSensation,_that.temperatureCelsius,_that.measurementLocation,_that.protectionUsed);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CycleObservationKind kind,  DateTime time,  String source,  int? days,  int? flow,  int? ovulationResult,  int? mucusAppearance,  int? mucusSensation,  double? temperatureCelsius,  int? measurementLocation,  int? protectionUsed)  $default,) {final _that = this;
switch (_that) {
case _CycleObservation():
return $default(_that.kind,_that.time,_that.source,_that.days,_that.flow,_that.ovulationResult,_that.mucusAppearance,_that.mucusSensation,_that.temperatureCelsius,_that.measurementLocation,_that.protectionUsed);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CycleObservationKind kind,  DateTime time,  String source,  int? days,  int? flow,  int? ovulationResult,  int? mucusAppearance,  int? mucusSensation,  double? temperatureCelsius,  int? measurementLocation,  int? protectionUsed)?  $default,) {final _that = this;
switch (_that) {
case _CycleObservation() when $default != null:
return $default(_that.kind,_that.time,_that.source,_that.days,_that.flow,_that.ovulationResult,_that.mucusAppearance,_that.mucusSensation,_that.temperatureCelsius,_that.measurementLocation,_that.protectionUsed);case _:
  return null;

}
}

}

/// @nodoc


class _CycleObservation implements CycleObservation {
  const _CycleObservation({required this.kind, required this.time, required this.source, this.days, this.flow, this.ovulationResult, this.mucusAppearance, this.mucusSensation, this.temperatureCelsius, this.measurementLocation, this.protectionUsed});
  

@override final  CycleObservationKind kind;
@override final  DateTime time;
@override final  String source;
/// Menstruation period: its length in whole days (at least one).
@override final  int? days;
@override final  int? flow;
@override final  int? ovulationResult;
@override final  int? mucusAppearance;
@override final  int? mucusSensation;
@override final  double? temperatureCelsius;
@override final  int? measurementLocation;
@override final  int? protectionUsed;

/// Create a copy of CycleObservation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CycleObservationCopyWith<_CycleObservation> get copyWith => __$CycleObservationCopyWithImpl<_CycleObservation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CycleObservation&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.time, time) || other.time == time)&&(identical(other.source, source) || other.source == source)&&(identical(other.days, days) || other.days == days)&&(identical(other.flow, flow) || other.flow == flow)&&(identical(other.ovulationResult, ovulationResult) || other.ovulationResult == ovulationResult)&&(identical(other.mucusAppearance, mucusAppearance) || other.mucusAppearance == mucusAppearance)&&(identical(other.mucusSensation, mucusSensation) || other.mucusSensation == mucusSensation)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.protectionUsed, protectionUsed) || other.protectionUsed == protectionUsed));
}


@override
int get hashCode => Object.hash(runtimeType,kind,time,source,days,flow,ovulationResult,mucusAppearance,mucusSensation,temperatureCelsius,measurementLocation,protectionUsed);

@override
String toString() {
  return 'CycleObservation(kind: $kind, time: $time, source: $source, days: $days, flow: $flow, ovulationResult: $ovulationResult, mucusAppearance: $mucusAppearance, mucusSensation: $mucusSensation, temperatureCelsius: $temperatureCelsius, measurementLocation: $measurementLocation, protectionUsed: $protectionUsed)';
}


}

/// @nodoc
abstract mixin class _$CycleObservationCopyWith<$Res> implements $CycleObservationCopyWith<$Res> {
  factory _$CycleObservationCopyWith(_CycleObservation value, $Res Function(_CycleObservation) _then) = __$CycleObservationCopyWithImpl;
@override @useResult
$Res call({
 CycleObservationKind kind, DateTime time, String source, int? days, int? flow, int? ovulationResult, int? mucusAppearance, int? mucusSensation, double? temperatureCelsius, int? measurementLocation, int? protectionUsed
});




}
/// @nodoc
class __$CycleObservationCopyWithImpl<$Res>
    implements _$CycleObservationCopyWith<$Res> {
  __$CycleObservationCopyWithImpl(this._self, this._then);

  final _CycleObservation _self;
  final $Res Function(_CycleObservation) _then;

/// Create a copy of CycleObservation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? kind = null,Object? time = null,Object? source = null,Object? days = freezed,Object? flow = freezed,Object? ovulationResult = freezed,Object? mucusAppearance = freezed,Object? mucusSensation = freezed,Object? temperatureCelsius = freezed,Object? measurementLocation = freezed,Object? protectionUsed = freezed,}) {
  return _then(_CycleObservation(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as CycleObservationKind,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,days: freezed == days ? _self.days : days // ignore: cast_nullable_to_non_nullable
as int?,flow: freezed == flow ? _self.flow : flow // ignore: cast_nullable_to_non_nullable
as int?,ovulationResult: freezed == ovulationResult ? _self.ovulationResult : ovulationResult // ignore: cast_nullable_to_non_nullable
as int?,mucusAppearance: freezed == mucusAppearance ? _self.mucusAppearance : mucusAppearance // ignore: cast_nullable_to_non_nullable
as int?,mucusSensation: freezed == mucusSensation ? _self.mucusSensation : mucusSensation // ignore: cast_nullable_to_non_nullable
as int?,temperatureCelsius: freezed == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,measurementLocation: freezed == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int?,protectionUsed: freezed == protectionUsed ? _self.protectionUsed : protectionUsed // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

// dart format on
