// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationSummary {

 double get totalLiters; int get trackedDays; int get loggedDays;/// Days of the period that have actually happened — the whole period, or
/// the part of it up to today. The denominator a person means when they ask
/// "how did I do this week".
 int get elapsedDays; double get averageLiters; double get bestDayLiters; int get goalMetDays; int get goalSuccessRatePercent; int get currentGoalStreakDays; int get longestGoalStreakDays;
/// Create a copy of HydrationSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationSummaryCopyWith<HydrationSummary> get copyWith => _$HydrationSummaryCopyWithImpl<HydrationSummary>(this as HydrationSummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationSummary&&(identical(other.totalLiters, totalLiters) || other.totalLiters == totalLiters)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.elapsedDays, elapsedDays) || other.elapsedDays == elapsedDays)&&(identical(other.averageLiters, averageLiters) || other.averageLiters == averageLiters)&&(identical(other.bestDayLiters, bestDayLiters) || other.bestDayLiters == bestDayLiters)&&(identical(other.goalMetDays, goalMetDays) || other.goalMetDays == goalMetDays)&&(identical(other.goalSuccessRatePercent, goalSuccessRatePercent) || other.goalSuccessRatePercent == goalSuccessRatePercent)&&(identical(other.currentGoalStreakDays, currentGoalStreakDays) || other.currentGoalStreakDays == currentGoalStreakDays)&&(identical(other.longestGoalStreakDays, longestGoalStreakDays) || other.longestGoalStreakDays == longestGoalStreakDays));
}


@override
int get hashCode => Object.hash(runtimeType,totalLiters,trackedDays,loggedDays,elapsedDays,averageLiters,bestDayLiters,goalMetDays,goalSuccessRatePercent,currentGoalStreakDays,longestGoalStreakDays);

@override
String toString() {
  return 'HydrationSummary(totalLiters: $totalLiters, trackedDays: $trackedDays, loggedDays: $loggedDays, elapsedDays: $elapsedDays, averageLiters: $averageLiters, bestDayLiters: $bestDayLiters, goalMetDays: $goalMetDays, goalSuccessRatePercent: $goalSuccessRatePercent, currentGoalStreakDays: $currentGoalStreakDays, longestGoalStreakDays: $longestGoalStreakDays)';
}


}

/// @nodoc
abstract mixin class $HydrationSummaryCopyWith<$Res>  {
  factory $HydrationSummaryCopyWith(HydrationSummary value, $Res Function(HydrationSummary) _then) = _$HydrationSummaryCopyWithImpl;
@useResult
$Res call({
 double totalLiters, int trackedDays, int loggedDays, int elapsedDays, double averageLiters, double bestDayLiters, int goalMetDays, int goalSuccessRatePercent, int currentGoalStreakDays, int longestGoalStreakDays
});




}
/// @nodoc
class _$HydrationSummaryCopyWithImpl<$Res>
    implements $HydrationSummaryCopyWith<$Res> {
  _$HydrationSummaryCopyWithImpl(this._self, this._then);

  final HydrationSummary _self;
  final $Res Function(HydrationSummary) _then;

/// Create a copy of HydrationSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? totalLiters = null,Object? trackedDays = null,Object? loggedDays = null,Object? elapsedDays = null,Object? averageLiters = null,Object? bestDayLiters = null,Object? goalMetDays = null,Object? goalSuccessRatePercent = null,Object? currentGoalStreakDays = null,Object? longestGoalStreakDays = null,}) {
  return _then(_self.copyWith(
totalLiters: null == totalLiters ? _self.totalLiters : totalLiters // ignore: cast_nullable_to_non_nullable
as double,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,elapsedDays: null == elapsedDays ? _self.elapsedDays : elapsedDays // ignore: cast_nullable_to_non_nullable
as int,averageLiters: null == averageLiters ? _self.averageLiters : averageLiters // ignore: cast_nullable_to_non_nullable
as double,bestDayLiters: null == bestDayLiters ? _self.bestDayLiters : bestDayLiters // ignore: cast_nullable_to_non_nullable
as double,goalMetDays: null == goalMetDays ? _self.goalMetDays : goalMetDays // ignore: cast_nullable_to_non_nullable
as int,goalSuccessRatePercent: null == goalSuccessRatePercent ? _self.goalSuccessRatePercent : goalSuccessRatePercent // ignore: cast_nullable_to_non_nullable
as int,currentGoalStreakDays: null == currentGoalStreakDays ? _self.currentGoalStreakDays : currentGoalStreakDays // ignore: cast_nullable_to_non_nullable
as int,longestGoalStreakDays: null == longestGoalStreakDays ? _self.longestGoalStreakDays : longestGoalStreakDays // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationSummary].
extension HydrationSummaryPatterns on HydrationSummary {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationSummary value)  $default,){
final _that = this;
switch (_that) {
case _HydrationSummary():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationSummary value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double totalLiters,  int trackedDays,  int loggedDays,  int elapsedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.elapsedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double totalLiters,  int trackedDays,  int loggedDays,  int elapsedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)  $default,) {final _that = this;
switch (_that) {
case _HydrationSummary():
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.elapsedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double totalLiters,  int trackedDays,  int loggedDays,  int elapsedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)?  $default,) {final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.elapsedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationSummary implements HydrationSummary {
  const _HydrationSummary({this.totalLiters = 0.0, this.trackedDays = 0, this.loggedDays = 0, this.elapsedDays = 0, this.averageLiters = 0.0, this.bestDayLiters = 0.0, this.goalMetDays = 0, this.goalSuccessRatePercent = 0, this.currentGoalStreakDays = 0, this.longestGoalStreakDays = 0});
  

@override@JsonKey() final  double totalLiters;
@override@JsonKey() final  int trackedDays;
@override@JsonKey() final  int loggedDays;
/// Days of the period that have actually happened — the whole period, or
/// the part of it up to today. The denominator a person means when they ask
/// "how did I do this week".
@override@JsonKey() final  int elapsedDays;
@override@JsonKey() final  double averageLiters;
@override@JsonKey() final  double bestDayLiters;
@override@JsonKey() final  int goalMetDays;
@override@JsonKey() final  int goalSuccessRatePercent;
@override@JsonKey() final  int currentGoalStreakDays;
@override@JsonKey() final  int longestGoalStreakDays;

/// Create a copy of HydrationSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationSummaryCopyWith<_HydrationSummary> get copyWith => __$HydrationSummaryCopyWithImpl<_HydrationSummary>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationSummary&&(identical(other.totalLiters, totalLiters) || other.totalLiters == totalLiters)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.elapsedDays, elapsedDays) || other.elapsedDays == elapsedDays)&&(identical(other.averageLiters, averageLiters) || other.averageLiters == averageLiters)&&(identical(other.bestDayLiters, bestDayLiters) || other.bestDayLiters == bestDayLiters)&&(identical(other.goalMetDays, goalMetDays) || other.goalMetDays == goalMetDays)&&(identical(other.goalSuccessRatePercent, goalSuccessRatePercent) || other.goalSuccessRatePercent == goalSuccessRatePercent)&&(identical(other.currentGoalStreakDays, currentGoalStreakDays) || other.currentGoalStreakDays == currentGoalStreakDays)&&(identical(other.longestGoalStreakDays, longestGoalStreakDays) || other.longestGoalStreakDays == longestGoalStreakDays));
}


@override
int get hashCode => Object.hash(runtimeType,totalLiters,trackedDays,loggedDays,elapsedDays,averageLiters,bestDayLiters,goalMetDays,goalSuccessRatePercent,currentGoalStreakDays,longestGoalStreakDays);

@override
String toString() {
  return 'HydrationSummary(totalLiters: $totalLiters, trackedDays: $trackedDays, loggedDays: $loggedDays, elapsedDays: $elapsedDays, averageLiters: $averageLiters, bestDayLiters: $bestDayLiters, goalMetDays: $goalMetDays, goalSuccessRatePercent: $goalSuccessRatePercent, currentGoalStreakDays: $currentGoalStreakDays, longestGoalStreakDays: $longestGoalStreakDays)';
}


}

/// @nodoc
abstract mixin class _$HydrationSummaryCopyWith<$Res> implements $HydrationSummaryCopyWith<$Res> {
  factory _$HydrationSummaryCopyWith(_HydrationSummary value, $Res Function(_HydrationSummary) _then) = __$HydrationSummaryCopyWithImpl;
@override @useResult
$Res call({
 double totalLiters, int trackedDays, int loggedDays, int elapsedDays, double averageLiters, double bestDayLiters, int goalMetDays, int goalSuccessRatePercent, int currentGoalStreakDays, int longestGoalStreakDays
});




}
/// @nodoc
class __$HydrationSummaryCopyWithImpl<$Res>
    implements _$HydrationSummaryCopyWith<$Res> {
  __$HydrationSummaryCopyWithImpl(this._self, this._then);

  final _HydrationSummary _self;
  final $Res Function(_HydrationSummary) _then;

/// Create a copy of HydrationSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? totalLiters = null,Object? trackedDays = null,Object? loggedDays = null,Object? elapsedDays = null,Object? averageLiters = null,Object? bestDayLiters = null,Object? goalMetDays = null,Object? goalSuccessRatePercent = null,Object? currentGoalStreakDays = null,Object? longestGoalStreakDays = null,}) {
  return _then(_HydrationSummary(
totalLiters: null == totalLiters ? _self.totalLiters : totalLiters // ignore: cast_nullable_to_non_nullable
as double,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,elapsedDays: null == elapsedDays ? _self.elapsedDays : elapsedDays // ignore: cast_nullable_to_non_nullable
as int,averageLiters: null == averageLiters ? _self.averageLiters : averageLiters // ignore: cast_nullable_to_non_nullable
as double,bestDayLiters: null == bestDayLiters ? _self.bestDayLiters : bestDayLiters // ignore: cast_nullable_to_non_nullable
as double,goalMetDays: null == goalMetDays ? _self.goalMetDays : goalMetDays // ignore: cast_nullable_to_non_nullable
as int,goalSuccessRatePercent: null == goalSuccessRatePercent ? _self.goalSuccessRatePercent : goalSuccessRatePercent // ignore: cast_nullable_to_non_nullable
as int,currentGoalStreakDays: null == currentGoalStreakDays ? _self.currentGoalStreakDays : currentGoalStreakDays // ignore: cast_nullable_to_non_nullable
as int,longestGoalStreakDays: null == longestGoalStreakDays ? _self.longestGoalStreakDays : longestGoalStreakDays // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$HydrationDrinkSlice {

 String? get label; double get liters;
/// Create a copy of HydrationDrinkSlice
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationDrinkSliceCopyWith<HydrationDrinkSlice> get copyWith => _$HydrationDrinkSliceCopyWithImpl<HydrationDrinkSlice>(this as HydrationDrinkSlice, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationDrinkSlice&&(identical(other.label, label) || other.label == label)&&(identical(other.liters, liters) || other.liters == liters));
}


@override
int get hashCode => Object.hash(runtimeType,label,liters);

@override
String toString() {
  return 'HydrationDrinkSlice(label: $label, liters: $liters)';
}


}

/// @nodoc
abstract mixin class $HydrationDrinkSliceCopyWith<$Res>  {
  factory $HydrationDrinkSliceCopyWith(HydrationDrinkSlice value, $Res Function(HydrationDrinkSlice) _then) = _$HydrationDrinkSliceCopyWithImpl;
@useResult
$Res call({
 String? label, double liters
});




}
/// @nodoc
class _$HydrationDrinkSliceCopyWithImpl<$Res>
    implements $HydrationDrinkSliceCopyWith<$Res> {
  _$HydrationDrinkSliceCopyWithImpl(this._self, this._then);

  final HydrationDrinkSlice _self;
  final $Res Function(HydrationDrinkSlice) _then;

/// Create a copy of HydrationDrinkSlice
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? label = freezed,Object? liters = null,}) {
  return _then(_self.copyWith(
label: freezed == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String?,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationDrinkSlice].
extension HydrationDrinkSlicePatterns on HydrationDrinkSlice {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationDrinkSlice value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationDrinkSlice() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationDrinkSlice value)  $default,){
final _that = this;
switch (_that) {
case _HydrationDrinkSlice():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationDrinkSlice value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationDrinkSlice() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? label,  double liters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationDrinkSlice() when $default != null:
return $default(_that.label,_that.liters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? label,  double liters)  $default,) {final _that = this;
switch (_that) {
case _HydrationDrinkSlice():
return $default(_that.label,_that.liters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? label,  double liters)?  $default,) {final _that = this;
switch (_that) {
case _HydrationDrinkSlice() when $default != null:
return $default(_that.label,_that.liters);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationDrinkSlice implements HydrationDrinkSlice {
  const _HydrationDrinkSlice({required this.label, required this.liters});
  

@override final  String? label;
@override final  double liters;

/// Create a copy of HydrationDrinkSlice
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationDrinkSliceCopyWith<_HydrationDrinkSlice> get copyWith => __$HydrationDrinkSliceCopyWithImpl<_HydrationDrinkSlice>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationDrinkSlice&&(identical(other.label, label) || other.label == label)&&(identical(other.liters, liters) || other.liters == liters));
}


@override
int get hashCode => Object.hash(runtimeType,label,liters);

@override
String toString() {
  return 'HydrationDrinkSlice(label: $label, liters: $liters)';
}


}

/// @nodoc
abstract mixin class _$HydrationDrinkSliceCopyWith<$Res> implements $HydrationDrinkSliceCopyWith<$Res> {
  factory _$HydrationDrinkSliceCopyWith(_HydrationDrinkSlice value, $Res Function(_HydrationDrinkSlice) _then) = __$HydrationDrinkSliceCopyWithImpl;
@override @useResult
$Res call({
 String? label, double liters
});




}
/// @nodoc
class __$HydrationDrinkSliceCopyWithImpl<$Res>
    implements _$HydrationDrinkSliceCopyWith<$Res> {
  __$HydrationDrinkSliceCopyWithImpl(this._self, this._then);

  final _HydrationDrinkSlice _self;
  final $Res Function(_HydrationDrinkSlice) _then;

/// Create a copy of HydrationDrinkSlice
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? label = freezed,Object? liters = null,}) {
  return _then(_HydrationDrinkSlice(
label: freezed == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String?,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$HydrationDisplay {

 bool get hasData; HydrationSummary get summary; List<PeriodChartValue> get chartValues; List<DaySample> get cumulativeSamples; List<HydrationDrinkSlice> get drinkBreakdown;/// The six biggest slices — all the breakdown card has room for.
 List<HydrationDrinkSlice> get topDrinkSlices;/// The scale [topDrinkSlices] are drawn against, floored at 1.0.
 double get maxDrinkLiters;/// The daily average as a fraction of the goal, clamped to 0..1.
 double get goalProgress; List<HydrationEntry> get entriesNewestFirst;
/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationDisplayCopyWith<HydrationDisplay> get copyWith => _$HydrationDisplayCopyWithImpl<HydrationDisplay>(this as HydrationDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other.chartValues, chartValues)&&const DeepCollectionEquality().equals(other.cumulativeSamples, cumulativeSamples)&&const DeepCollectionEquality().equals(other.drinkBreakdown, drinkBreakdown)&&const DeepCollectionEquality().equals(other.topDrinkSlices, topDrinkSlices)&&(identical(other.maxDrinkLiters, maxDrinkLiters) || other.maxDrinkLiters == maxDrinkLiters)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&const DeepCollectionEquality().equals(other.entriesNewestFirst, entriesNewestFirst));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,summary,const DeepCollectionEquality().hash(chartValues),const DeepCollectionEquality().hash(cumulativeSamples),const DeepCollectionEquality().hash(drinkBreakdown),const DeepCollectionEquality().hash(topDrinkSlices),maxDrinkLiters,goalProgress,const DeepCollectionEquality().hash(entriesNewestFirst));

@override
String toString() {
  return 'HydrationDisplay(hasData: $hasData, summary: $summary, chartValues: $chartValues, cumulativeSamples: $cumulativeSamples, drinkBreakdown: $drinkBreakdown, topDrinkSlices: $topDrinkSlices, maxDrinkLiters: $maxDrinkLiters, goalProgress: $goalProgress, entriesNewestFirst: $entriesNewestFirst)';
}


}

/// @nodoc
abstract mixin class $HydrationDisplayCopyWith<$Res>  {
  factory $HydrationDisplayCopyWith(HydrationDisplay value, $Res Function(HydrationDisplay) _then) = _$HydrationDisplayCopyWithImpl;
@useResult
$Res call({
 bool hasData, HydrationSummary summary, List<PeriodChartValue> chartValues, List<DaySample> cumulativeSamples, List<HydrationDrinkSlice> drinkBreakdown, List<HydrationDrinkSlice> topDrinkSlices, double maxDrinkLiters, double goalProgress, List<HydrationEntry> entriesNewestFirst
});


$HydrationSummaryCopyWith<$Res> get summary;

}
/// @nodoc
class _$HydrationDisplayCopyWithImpl<$Res>
    implements $HydrationDisplayCopyWith<$Res> {
  _$HydrationDisplayCopyWithImpl(this._self, this._then);

  final HydrationDisplay _self;
  final $Res Function(HydrationDisplay) _then;

/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? hasData = null,Object? summary = null,Object? chartValues = null,Object? cumulativeSamples = null,Object? drinkBreakdown = null,Object? topDrinkSlices = null,Object? maxDrinkLiters = null,Object? goalProgress = null,Object? entriesNewestFirst = null,}) {
  return _then(_self.copyWith(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as HydrationSummary,chartValues: null == chartValues ? _self.chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,cumulativeSamples: null == cumulativeSamples ? _self.cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,drinkBreakdown: null == drinkBreakdown ? _self.drinkBreakdown : drinkBreakdown // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,topDrinkSlices: null == topDrinkSlices ? _self.topDrinkSlices : topDrinkSlices // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,maxDrinkLiters: null == maxDrinkLiters ? _self.maxDrinkLiters : maxDrinkLiters // ignore: cast_nullable_to_non_nullable
as double,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as double,entriesNewestFirst: null == entriesNewestFirst ? _self.entriesNewestFirst : entriesNewestFirst // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,
  ));
}
/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationSummaryCopyWith<$Res> get summary {
  
  return $HydrationSummaryCopyWith<$Res>(_self.summary, (value) {
    return _then(_self.copyWith(summary: value));
  });
}
}


/// Adds pattern-matching-related methods to [HydrationDisplay].
extension HydrationDisplayPatterns on HydrationDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationDisplay value)  $default,){
final _that = this;
switch (_that) {
case _HydrationDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool hasData,  HydrationSummary summary,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<HydrationDrinkSlice> drinkBreakdown,  List<HydrationDrinkSlice> topDrinkSlices,  double maxDrinkLiters,  double goalProgress,  List<HydrationEntry> entriesNewestFirst)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationDisplay() when $default != null:
return $default(_that.hasData,_that.summary,_that.chartValues,_that.cumulativeSamples,_that.drinkBreakdown,_that.topDrinkSlices,_that.maxDrinkLiters,_that.goalProgress,_that.entriesNewestFirst);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool hasData,  HydrationSummary summary,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<HydrationDrinkSlice> drinkBreakdown,  List<HydrationDrinkSlice> topDrinkSlices,  double maxDrinkLiters,  double goalProgress,  List<HydrationEntry> entriesNewestFirst)  $default,) {final _that = this;
switch (_that) {
case _HydrationDisplay():
return $default(_that.hasData,_that.summary,_that.chartValues,_that.cumulativeSamples,_that.drinkBreakdown,_that.topDrinkSlices,_that.maxDrinkLiters,_that.goalProgress,_that.entriesNewestFirst);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool hasData,  HydrationSummary summary,  List<PeriodChartValue> chartValues,  List<DaySample> cumulativeSamples,  List<HydrationDrinkSlice> drinkBreakdown,  List<HydrationDrinkSlice> topDrinkSlices,  double maxDrinkLiters,  double goalProgress,  List<HydrationEntry> entriesNewestFirst)?  $default,) {final _that = this;
switch (_that) {
case _HydrationDisplay() when $default != null:
return $default(_that.hasData,_that.summary,_that.chartValues,_that.cumulativeSamples,_that.drinkBreakdown,_that.topDrinkSlices,_that.maxDrinkLiters,_that.goalProgress,_that.entriesNewestFirst);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationDisplay implements HydrationDisplay {
  const _HydrationDisplay({this.hasData = false, this.summary = const HydrationSummary(), final  List<PeriodChartValue> chartValues = const <PeriodChartValue>[], final  List<DaySample> cumulativeSamples = const <DaySample>[], final  List<HydrationDrinkSlice> drinkBreakdown = const <HydrationDrinkSlice>[], final  List<HydrationDrinkSlice> topDrinkSlices = const <HydrationDrinkSlice>[], this.maxDrinkLiters = 1.0, this.goalProgress = 0.0, final  List<HydrationEntry> entriesNewestFirst = const <HydrationEntry>[]}): _chartValues = chartValues,_cumulativeSamples = cumulativeSamples,_drinkBreakdown = drinkBreakdown,_topDrinkSlices = topDrinkSlices,_entriesNewestFirst = entriesNewestFirst;
  

@override@JsonKey() final  bool hasData;
@override@JsonKey() final  HydrationSummary summary;
 final  List<PeriodChartValue> _chartValues;
@override@JsonKey() List<PeriodChartValue> get chartValues {
  if (_chartValues is EqualUnmodifiableListView) return _chartValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_chartValues);
}

 final  List<DaySample> _cumulativeSamples;
@override@JsonKey() List<DaySample> get cumulativeSamples {
  if (_cumulativeSamples is EqualUnmodifiableListView) return _cumulativeSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cumulativeSamples);
}

 final  List<HydrationDrinkSlice> _drinkBreakdown;
@override@JsonKey() List<HydrationDrinkSlice> get drinkBreakdown {
  if (_drinkBreakdown is EqualUnmodifiableListView) return _drinkBreakdown;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_drinkBreakdown);
}

/// The six biggest slices — all the breakdown card has room for.
 final  List<HydrationDrinkSlice> _topDrinkSlices;
/// The six biggest slices — all the breakdown card has room for.
@override@JsonKey() List<HydrationDrinkSlice> get topDrinkSlices {
  if (_topDrinkSlices is EqualUnmodifiableListView) return _topDrinkSlices;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_topDrinkSlices);
}

/// The scale [topDrinkSlices] are drawn against, floored at 1.0.
@override@JsonKey() final  double maxDrinkLiters;
/// The daily average as a fraction of the goal, clamped to 0..1.
@override@JsonKey() final  double goalProgress;
 final  List<HydrationEntry> _entriesNewestFirst;
@override@JsonKey() List<HydrationEntry> get entriesNewestFirst {
  if (_entriesNewestFirst is EqualUnmodifiableListView) return _entriesNewestFirst;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entriesNewestFirst);
}


/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationDisplayCopyWith<_HydrationDisplay> get copyWith => __$HydrationDisplayCopyWithImpl<_HydrationDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other._chartValues, _chartValues)&&const DeepCollectionEquality().equals(other._cumulativeSamples, _cumulativeSamples)&&const DeepCollectionEquality().equals(other._drinkBreakdown, _drinkBreakdown)&&const DeepCollectionEquality().equals(other._topDrinkSlices, _topDrinkSlices)&&(identical(other.maxDrinkLiters, maxDrinkLiters) || other.maxDrinkLiters == maxDrinkLiters)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&const DeepCollectionEquality().equals(other._entriesNewestFirst, _entriesNewestFirst));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,summary,const DeepCollectionEquality().hash(_chartValues),const DeepCollectionEquality().hash(_cumulativeSamples),const DeepCollectionEquality().hash(_drinkBreakdown),const DeepCollectionEquality().hash(_topDrinkSlices),maxDrinkLiters,goalProgress,const DeepCollectionEquality().hash(_entriesNewestFirst));

@override
String toString() {
  return 'HydrationDisplay(hasData: $hasData, summary: $summary, chartValues: $chartValues, cumulativeSamples: $cumulativeSamples, drinkBreakdown: $drinkBreakdown, topDrinkSlices: $topDrinkSlices, maxDrinkLiters: $maxDrinkLiters, goalProgress: $goalProgress, entriesNewestFirst: $entriesNewestFirst)';
}


}

/// @nodoc
abstract mixin class _$HydrationDisplayCopyWith<$Res> implements $HydrationDisplayCopyWith<$Res> {
  factory _$HydrationDisplayCopyWith(_HydrationDisplay value, $Res Function(_HydrationDisplay) _then) = __$HydrationDisplayCopyWithImpl;
@override @useResult
$Res call({
 bool hasData, HydrationSummary summary, List<PeriodChartValue> chartValues, List<DaySample> cumulativeSamples, List<HydrationDrinkSlice> drinkBreakdown, List<HydrationDrinkSlice> topDrinkSlices, double maxDrinkLiters, double goalProgress, List<HydrationEntry> entriesNewestFirst
});


@override $HydrationSummaryCopyWith<$Res> get summary;

}
/// @nodoc
class __$HydrationDisplayCopyWithImpl<$Res>
    implements _$HydrationDisplayCopyWith<$Res> {
  __$HydrationDisplayCopyWithImpl(this._self, this._then);

  final _HydrationDisplay _self;
  final $Res Function(_HydrationDisplay) _then;

/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? hasData = null,Object? summary = null,Object? chartValues = null,Object? cumulativeSamples = null,Object? drinkBreakdown = null,Object? topDrinkSlices = null,Object? maxDrinkLiters = null,Object? goalProgress = null,Object? entriesNewestFirst = null,}) {
  return _then(_HydrationDisplay(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as HydrationSummary,chartValues: null == chartValues ? _self._chartValues : chartValues // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,cumulativeSamples: null == cumulativeSamples ? _self._cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,drinkBreakdown: null == drinkBreakdown ? _self._drinkBreakdown : drinkBreakdown // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,topDrinkSlices: null == topDrinkSlices ? _self._topDrinkSlices : topDrinkSlices // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,maxDrinkLiters: null == maxDrinkLiters ? _self.maxDrinkLiters : maxDrinkLiters // ignore: cast_nullable_to_non_nullable
as double,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as double,entriesNewestFirst: null == entriesNewestFirst ? _self._entriesNewestFirst : entriesNewestFirst // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,
  ));
}

/// Create a copy of HydrationDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationSummaryCopyWith<$Res> get summary {
  
  return $HydrationSummaryCopyWith<$Res>(_self.summary, (value) {
    return _then(_self.copyWith(summary: value));
  });
}
}

// dart format on
