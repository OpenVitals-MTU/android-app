// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepState {

 LocalDate get selectedDate; TimeRange get selectedRange; SleepRangeMode get sleepRangeMode; WeekPeriodMode get weekPeriodMode; bool get isLoading;/// The sleep-hours goal, moved by the goal card's steppers.
 double get dailyGoalHours; ScreenError? get error; SleepPeriodLoadResult? get result;
/// Create a copy of SleepState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepStateCopyWith<SleepState> get copyWith => _$SleepStateCopyWithImpl<SleepState>(this as SleepState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.weekPeriodMode, weekPeriodMode) || other.weekPeriodMode == weekPeriodMode)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.dailyGoalHours, dailyGoalHours) || other.dailyGoalHours == dailyGoalHours)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,sleepRangeMode,weekPeriodMode,isLoading,dailyGoalHours,error,result);

@override
String toString() {
  return 'SleepState(selectedDate: $selectedDate, selectedRange: $selectedRange, sleepRangeMode: $sleepRangeMode, weekPeriodMode: $weekPeriodMode, isLoading: $isLoading, dailyGoalHours: $dailyGoalHours, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class $SleepStateCopyWith<$Res>  {
  factory $SleepStateCopyWith(SleepState value, $Res Function(SleepState) _then) = _$SleepStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, SleepRangeMode sleepRangeMode, WeekPeriodMode weekPeriodMode, bool isLoading, double dailyGoalHours, ScreenError? error, SleepPeriodLoadResult? result
});




}
/// @nodoc
class _$SleepStateCopyWithImpl<$Res>
    implements $SleepStateCopyWith<$Res> {
  _$SleepStateCopyWithImpl(this._self, this._then);

  final SleepState _self;
  final $Res Function(SleepState) _then;

/// Create a copy of SleepState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? sleepRangeMode = null,Object? weekPeriodMode = null,Object? isLoading = null,Object? dailyGoalHours = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,weekPeriodMode: null == weekPeriodMode ? _self.weekPeriodMode : weekPeriodMode // ignore: cast_nullable_to_non_nullable
as WeekPeriodMode,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,dailyGoalHours: null == dailyGoalHours ? _self.dailyGoalHours : dailyGoalHours // ignore: cast_nullable_to_non_nullable
as double,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as SleepPeriodLoadResult?,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepState].
extension SleepStatePatterns on SleepState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepState value)  $default,){
final _that = this;
switch (_that) {
case _SleepState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepState value)?  $default,){
final _that = this;
switch (_that) {
case _SleepState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  SleepRangeMode sleepRangeMode,  WeekPeriodMode weekPeriodMode,  bool isLoading,  double dailyGoalHours,  ScreenError? error,  SleepPeriodLoadResult? result)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.sleepRangeMode,_that.weekPeriodMode,_that.isLoading,_that.dailyGoalHours,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  SleepRangeMode sleepRangeMode,  WeekPeriodMode weekPeriodMode,  bool isLoading,  double dailyGoalHours,  ScreenError? error,  SleepPeriodLoadResult? result)  $default,) {final _that = this;
switch (_that) {
case _SleepState():
return $default(_that.selectedDate,_that.selectedRange,_that.sleepRangeMode,_that.weekPeriodMode,_that.isLoading,_that.dailyGoalHours,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  SleepRangeMode sleepRangeMode,  WeekPeriodMode weekPeriodMode,  bool isLoading,  double dailyGoalHours,  ScreenError? error,  SleepPeriodLoadResult? result)?  $default,) {final _that = this;
switch (_that) {
case _SleepState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.sleepRangeMode,_that.weekPeriodMode,_that.isLoading,_that.dailyGoalHours,_that.error,_that.result);case _:
  return null;

}
}

}

/// @nodoc


class _SleepState extends SleepState {
  const _SleepState({required this.selectedDate, this.selectedRange = TimeRange.week, this.sleepRangeMode = SleepRangeMode.evening18h, this.weekPeriodMode = WeekPeriodMode.mondayToSunday, this.isLoading = true, this.dailyGoalHours = 8.0, this.error, this.result}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  SleepRangeMode sleepRangeMode;
@override@JsonKey() final  WeekPeriodMode weekPeriodMode;
@override@JsonKey() final  bool isLoading;
/// The sleep-hours goal, moved by the goal card's steppers.
@override@JsonKey() final  double dailyGoalHours;
@override final  ScreenError? error;
@override final  SleepPeriodLoadResult? result;

/// Create a copy of SleepState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepStateCopyWith<_SleepState> get copyWith => __$SleepStateCopyWithImpl<_SleepState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.weekPeriodMode, weekPeriodMode) || other.weekPeriodMode == weekPeriodMode)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.dailyGoalHours, dailyGoalHours) || other.dailyGoalHours == dailyGoalHours)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,sleepRangeMode,weekPeriodMode,isLoading,dailyGoalHours,error,result);

@override
String toString() {
  return 'SleepState(selectedDate: $selectedDate, selectedRange: $selectedRange, sleepRangeMode: $sleepRangeMode, weekPeriodMode: $weekPeriodMode, isLoading: $isLoading, dailyGoalHours: $dailyGoalHours, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class _$SleepStateCopyWith<$Res> implements $SleepStateCopyWith<$Res> {
  factory _$SleepStateCopyWith(_SleepState value, $Res Function(_SleepState) _then) = __$SleepStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, SleepRangeMode sleepRangeMode, WeekPeriodMode weekPeriodMode, bool isLoading, double dailyGoalHours, ScreenError? error, SleepPeriodLoadResult? result
});




}
/// @nodoc
class __$SleepStateCopyWithImpl<$Res>
    implements _$SleepStateCopyWith<$Res> {
  __$SleepStateCopyWithImpl(this._self, this._then);

  final _SleepState _self;
  final $Res Function(_SleepState) _then;

/// Create a copy of SleepState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? sleepRangeMode = null,Object? weekPeriodMode = null,Object? isLoading = null,Object? dailyGoalHours = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_SleepState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,weekPeriodMode: null == weekPeriodMode ? _self.weekPeriodMode : weekPeriodMode // ignore: cast_nullable_to_non_nullable
as WeekPeriodMode,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,dailyGoalHours: null == dailyGoalHours ? _self.dailyGoalHours : dailyGoalHours // ignore: cast_nullable_to_non_nullable
as double,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as SleepPeriodLoadResult?,
  ));
}


}

// dart format on
