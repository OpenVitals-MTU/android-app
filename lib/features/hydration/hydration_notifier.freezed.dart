// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationSummary {

 double get totalLiters; int get trackedDays; int get loggedDays; double get averageLiters; double get bestDayLiters; int get goalMetDays; int get goalSuccessRatePercent; int get currentGoalStreakDays; int get longestGoalStreakDays;
/// Create a copy of HydrationSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationSummaryCopyWith<HydrationSummary> get copyWith => _$HydrationSummaryCopyWithImpl<HydrationSummary>(this as HydrationSummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationSummary&&(identical(other.totalLiters, totalLiters) || other.totalLiters == totalLiters)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.averageLiters, averageLiters) || other.averageLiters == averageLiters)&&(identical(other.bestDayLiters, bestDayLiters) || other.bestDayLiters == bestDayLiters)&&(identical(other.goalMetDays, goalMetDays) || other.goalMetDays == goalMetDays)&&(identical(other.goalSuccessRatePercent, goalSuccessRatePercent) || other.goalSuccessRatePercent == goalSuccessRatePercent)&&(identical(other.currentGoalStreakDays, currentGoalStreakDays) || other.currentGoalStreakDays == currentGoalStreakDays)&&(identical(other.longestGoalStreakDays, longestGoalStreakDays) || other.longestGoalStreakDays == longestGoalStreakDays));
}


@override
int get hashCode => Object.hash(runtimeType,totalLiters,trackedDays,loggedDays,averageLiters,bestDayLiters,goalMetDays,goalSuccessRatePercent,currentGoalStreakDays,longestGoalStreakDays);

@override
String toString() {
  return 'HydrationSummary(totalLiters: $totalLiters, trackedDays: $trackedDays, loggedDays: $loggedDays, averageLiters: $averageLiters, bestDayLiters: $bestDayLiters, goalMetDays: $goalMetDays, goalSuccessRatePercent: $goalSuccessRatePercent, currentGoalStreakDays: $currentGoalStreakDays, longestGoalStreakDays: $longestGoalStreakDays)';
}


}

/// @nodoc
abstract mixin class $HydrationSummaryCopyWith<$Res>  {
  factory $HydrationSummaryCopyWith(HydrationSummary value, $Res Function(HydrationSummary) _then) = _$HydrationSummaryCopyWithImpl;
@useResult
$Res call({
 double totalLiters, int trackedDays, int loggedDays, double averageLiters, double bestDayLiters, int goalMetDays, int goalSuccessRatePercent, int currentGoalStreakDays, int longestGoalStreakDays
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
@pragma('vm:prefer-inline') @override $Res call({Object? totalLiters = null,Object? trackedDays = null,Object? loggedDays = null,Object? averageLiters = null,Object? bestDayLiters = null,Object? goalMetDays = null,Object? goalSuccessRatePercent = null,Object? currentGoalStreakDays = null,Object? longestGoalStreakDays = null,}) {
  return _then(_self.copyWith(
totalLiters: null == totalLiters ? _self.totalLiters : totalLiters // ignore: cast_nullable_to_non_nullable
as double,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double totalLiters,  int trackedDays,  int loggedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double totalLiters,  int trackedDays,  int loggedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)  $default,) {final _that = this;
switch (_that) {
case _HydrationSummary():
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double totalLiters,  int trackedDays,  int loggedDays,  double averageLiters,  double bestDayLiters,  int goalMetDays,  int goalSuccessRatePercent,  int currentGoalStreakDays,  int longestGoalStreakDays)?  $default,) {final _that = this;
switch (_that) {
case _HydrationSummary() when $default != null:
return $default(_that.totalLiters,_that.trackedDays,_that.loggedDays,_that.averageLiters,_that.bestDayLiters,_that.goalMetDays,_that.goalSuccessRatePercent,_that.currentGoalStreakDays,_that.longestGoalStreakDays);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationSummary implements HydrationSummary {
  const _HydrationSummary({this.totalLiters = 0.0, this.trackedDays = 0, this.loggedDays = 0, this.averageLiters = 0.0, this.bestDayLiters = 0.0, this.goalMetDays = 0, this.goalSuccessRatePercent = 0, this.currentGoalStreakDays = 0, this.longestGoalStreakDays = 0});
  

@override@JsonKey() final  double totalLiters;
@override@JsonKey() final  int trackedDays;
@override@JsonKey() final  int loggedDays;
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
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationSummary&&(identical(other.totalLiters, totalLiters) || other.totalLiters == totalLiters)&&(identical(other.trackedDays, trackedDays) || other.trackedDays == trackedDays)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.averageLiters, averageLiters) || other.averageLiters == averageLiters)&&(identical(other.bestDayLiters, bestDayLiters) || other.bestDayLiters == bestDayLiters)&&(identical(other.goalMetDays, goalMetDays) || other.goalMetDays == goalMetDays)&&(identical(other.goalSuccessRatePercent, goalSuccessRatePercent) || other.goalSuccessRatePercent == goalSuccessRatePercent)&&(identical(other.currentGoalStreakDays, currentGoalStreakDays) || other.currentGoalStreakDays == currentGoalStreakDays)&&(identical(other.longestGoalStreakDays, longestGoalStreakDays) || other.longestGoalStreakDays == longestGoalStreakDays));
}


@override
int get hashCode => Object.hash(runtimeType,totalLiters,trackedDays,loggedDays,averageLiters,bestDayLiters,goalMetDays,goalSuccessRatePercent,currentGoalStreakDays,longestGoalStreakDays);

@override
String toString() {
  return 'HydrationSummary(totalLiters: $totalLiters, trackedDays: $trackedDays, loggedDays: $loggedDays, averageLiters: $averageLiters, bestDayLiters: $bestDayLiters, goalMetDays: $goalMetDays, goalSuccessRatePercent: $goalSuccessRatePercent, currentGoalStreakDays: $currentGoalStreakDays, longestGoalStreakDays: $longestGoalStreakDays)';
}


}

/// @nodoc
abstract mixin class _$HydrationSummaryCopyWith<$Res> implements $HydrationSummaryCopyWith<$Res> {
  factory _$HydrationSummaryCopyWith(_HydrationSummary value, $Res Function(_HydrationSummary) _then) = __$HydrationSummaryCopyWithImpl;
@override @useResult
$Res call({
 double totalLiters, int trackedDays, int loggedDays, double averageLiters, double bestDayLiters, int goalMetDays, int goalSuccessRatePercent, int currentGoalStreakDays, int longestGoalStreakDays
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
@override @pragma('vm:prefer-inline') $Res call({Object? totalLiters = null,Object? trackedDays = null,Object? loggedDays = null,Object? averageLiters = null,Object? bestDayLiters = null,Object? goalMetDays = null,Object? goalSuccessRatePercent = null,Object? currentGoalStreakDays = null,Object? longestGoalStreakDays = null,}) {
  return _then(_HydrationSummary(
totalLiters: null == totalLiters ? _self.totalLiters : totalLiters // ignore: cast_nullable_to_non_nullable
as double,trackedDays: null == trackedDays ? _self.trackedDays : trackedDays // ignore: cast_nullable_to_non_nullable
as int,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
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

 String get label; double get liters;
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
 String label, double liters
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
@pragma('vm:prefer-inline') @override $Res call({Object? label = null,Object? liters = null,}) {
  return _then(_self.copyWith(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String label,  double liters)?  $default,{required TResult orElse(),}) {final _that = this;
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String label,  double liters)  $default,) {final _that = this;
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String label,  double liters)?  $default,) {final _that = this;
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
  

@override final  String label;
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
 String label, double liters
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
@override @pragma('vm:prefer-inline') $Res call({Object? label = null,Object? liters = null,}) {
  return _then(_HydrationDrinkSlice(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$HydrationState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; double get dailyGoalLiters; List<DailyHydration> get dailyHydration; List<HydrationEntry> get entries; HydrationSummary get summary; List<HydrationDrinkSlice> get drinkBreakdown;
/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationStateCopyWith<HydrationState> get copyWith => _$HydrationStateCopyWithImpl<HydrationState>(this as HydrationState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&const DeepCollectionEquality().equals(other.dailyHydration, dailyHydration)&&const DeepCollectionEquality().equals(other.entries, entries)&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other.drinkBreakdown, drinkBreakdown));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalLiters,const DeepCollectionEquality().hash(dailyHydration),const DeepCollectionEquality().hash(entries),summary,const DeepCollectionEquality().hash(drinkBreakdown));

@override
String toString() {
  return 'HydrationState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalLiters: $dailyGoalLiters, dailyHydration: $dailyHydration, entries: $entries, summary: $summary, drinkBreakdown: $drinkBreakdown)';
}


}

/// @nodoc
abstract mixin class $HydrationStateCopyWith<$Res>  {
  factory $HydrationStateCopyWith(HydrationState value, $Res Function(HydrationState) _then) = _$HydrationStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalLiters, List<DailyHydration> dailyHydration, List<HydrationEntry> entries, HydrationSummary summary, List<HydrationDrinkSlice> drinkBreakdown
});


$HydrationSummaryCopyWith<$Res> get summary;

}
/// @nodoc
class _$HydrationStateCopyWithImpl<$Res>
    implements $HydrationStateCopyWith<$Res> {
  _$HydrationStateCopyWithImpl(this._self, this._then);

  final HydrationState _self;
  final $Res Function(HydrationState) _then;

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalLiters = null,Object? dailyHydration = null,Object? entries = null,Object? summary = null,Object? drinkBreakdown = null,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,dailyHydration: null == dailyHydration ? _self.dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as HydrationSummary,drinkBreakdown: null == drinkBreakdown ? _self.drinkBreakdown : drinkBreakdown // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,
  ));
}
/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationSummaryCopyWith<$Res> get summary {
  
  return $HydrationSummaryCopyWith<$Res>(_self.summary, (value) {
    return _then(_self.copyWith(summary: value));
  });
}
}


/// Adds pattern-matching-related methods to [HydrationState].
extension HydrationStatePatterns on HydrationState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationState value)  $default,){
final _that = this;
switch (_that) {
case _HydrationState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationState value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationSummary summary,  List<HydrationDrinkSlice> drinkBreakdown)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.summary,_that.drinkBreakdown);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationSummary summary,  List<HydrationDrinkSlice> drinkBreakdown)  $default,) {final _that = this;
switch (_that) {
case _HydrationState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.summary,_that.drinkBreakdown);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationSummary summary,  List<HydrationDrinkSlice> drinkBreakdown)?  $default,) {final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.summary,_that.drinkBreakdown);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationState extends HydrationState {
  const _HydrationState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.dailyGoalLiters = 2.0, final  List<DailyHydration> dailyHydration = const <DailyHydration>[], final  List<HydrationEntry> entries = const <HydrationEntry>[], this.summary = const HydrationSummary(), final  List<HydrationDrinkSlice> drinkBreakdown = const <HydrationDrinkSlice>[]}): _dailyHydration = dailyHydration,_entries = entries,_drinkBreakdown = drinkBreakdown,super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override@JsonKey() final  double dailyGoalLiters;
 final  List<DailyHydration> _dailyHydration;
@override@JsonKey() List<DailyHydration> get dailyHydration {
  if (_dailyHydration is EqualUnmodifiableListView) return _dailyHydration;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyHydration);
}

 final  List<HydrationEntry> _entries;
@override@JsonKey() List<HydrationEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}

@override@JsonKey() final  HydrationSummary summary;
 final  List<HydrationDrinkSlice> _drinkBreakdown;
@override@JsonKey() List<HydrationDrinkSlice> get drinkBreakdown {
  if (_drinkBreakdown is EqualUnmodifiableListView) return _drinkBreakdown;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_drinkBreakdown);
}


/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationStateCopyWith<_HydrationState> get copyWith => __$HydrationStateCopyWithImpl<_HydrationState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&const DeepCollectionEquality().equals(other._dailyHydration, _dailyHydration)&&const DeepCollectionEquality().equals(other._entries, _entries)&&(identical(other.summary, summary) || other.summary == summary)&&const DeepCollectionEquality().equals(other._drinkBreakdown, _drinkBreakdown));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalLiters,const DeepCollectionEquality().hash(_dailyHydration),const DeepCollectionEquality().hash(_entries),summary,const DeepCollectionEquality().hash(_drinkBreakdown));

@override
String toString() {
  return 'HydrationState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalLiters: $dailyGoalLiters, dailyHydration: $dailyHydration, entries: $entries, summary: $summary, drinkBreakdown: $drinkBreakdown)';
}


}

/// @nodoc
abstract mixin class _$HydrationStateCopyWith<$Res> implements $HydrationStateCopyWith<$Res> {
  factory _$HydrationStateCopyWith(_HydrationState value, $Res Function(_HydrationState) _then) = __$HydrationStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalLiters, List<DailyHydration> dailyHydration, List<HydrationEntry> entries, HydrationSummary summary, List<HydrationDrinkSlice> drinkBreakdown
});


@override $HydrationSummaryCopyWith<$Res> get summary;

}
/// @nodoc
class __$HydrationStateCopyWithImpl<$Res>
    implements _$HydrationStateCopyWith<$Res> {
  __$HydrationStateCopyWithImpl(this._self, this._then);

  final _HydrationState _self;
  final $Res Function(_HydrationState) _then;

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalLiters = null,Object? dailyHydration = null,Object? entries = null,Object? summary = null,Object? drinkBreakdown = null,}) {
  return _then(_HydrationState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,dailyHydration: null == dailyHydration ? _self._dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,summary: null == summary ? _self.summary : summary // ignore: cast_nullable_to_non_nullable
as HydrationSummary,drinkBreakdown: null == drinkBreakdown ? _self._drinkBreakdown : drinkBreakdown // ignore: cast_nullable_to_non_nullable
as List<HydrationDrinkSlice>,
  ));
}

/// Create a copy of HydrationState
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
