// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_vitals_overview_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartVitalsOverviewState {

 LocalDate get selectedDate; TimeRange get selectedRange;/// The loaded period's week mode, carried on the state (as `SleepState` does)
/// so the section summaries name the window exactly as the period navigator
/// does — "Last 30 days" on a rolling month, not "This month".
 WeekPeriodMode get weekPeriodMode; bool get isLoading; ScreenError? get error; HeartPeriodLoadResult? get result; HeartVitalsOverviewDisplay? get display;
/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartVitalsOverviewStateCopyWith<HeartVitalsOverviewState> get copyWith => _$HeartVitalsOverviewStateCopyWithImpl<HeartVitalsOverviewState>(this as HeartVitalsOverviewState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartVitalsOverviewState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.weekPeriodMode, weekPeriodMode) || other.weekPeriodMode == weekPeriodMode)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,weekPeriodMode,isLoading,error,result,display);

@override
String toString() {
  return 'HeartVitalsOverviewState(selectedDate: $selectedDate, selectedRange: $selectedRange, weekPeriodMode: $weekPeriodMode, isLoading: $isLoading, error: $error, result: $result, display: $display)';
}


}

/// @nodoc
abstract mixin class $HeartVitalsOverviewStateCopyWith<$Res>  {
  factory $HeartVitalsOverviewStateCopyWith(HeartVitalsOverviewState value, $Res Function(HeartVitalsOverviewState) _then) = _$HeartVitalsOverviewStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, WeekPeriodMode weekPeriodMode, bool isLoading, ScreenError? error, HeartPeriodLoadResult? result, HeartVitalsOverviewDisplay? display
});


$HeartVitalsOverviewDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$HeartVitalsOverviewStateCopyWithImpl<$Res>
    implements $HeartVitalsOverviewStateCopyWith<$Res> {
  _$HeartVitalsOverviewStateCopyWithImpl(this._self, this._then);

  final HeartVitalsOverviewState _self;
  final $Res Function(HeartVitalsOverviewState) _then;

/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? weekPeriodMode = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,weekPeriodMode: null == weekPeriodMode ? _self.weekPeriodMode : weekPeriodMode // ignore: cast_nullable_to_non_nullable
as WeekPeriodMode,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as HeartPeriodLoadResult?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as HeartVitalsOverviewDisplay?,
  ));
}
/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartVitalsOverviewDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $HeartVitalsOverviewDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [HeartVitalsOverviewState].
extension HeartVitalsOverviewStatePatterns on HeartVitalsOverviewState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartVitalsOverviewState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartVitalsOverviewState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartVitalsOverviewState value)  $default,){
final _that = this;
switch (_that) {
case _HeartVitalsOverviewState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartVitalsOverviewState value)?  $default,){
final _that = this;
switch (_that) {
case _HeartVitalsOverviewState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  WeekPeriodMode weekPeriodMode,  bool isLoading,  ScreenError? error,  HeartPeriodLoadResult? result,  HeartVitalsOverviewDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartVitalsOverviewState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.weekPeriodMode,_that.isLoading,_that.error,_that.result,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  WeekPeriodMode weekPeriodMode,  bool isLoading,  ScreenError? error,  HeartPeriodLoadResult? result,  HeartVitalsOverviewDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _HeartVitalsOverviewState():
return $default(_that.selectedDate,_that.selectedRange,_that.weekPeriodMode,_that.isLoading,_that.error,_that.result,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  WeekPeriodMode weekPeriodMode,  bool isLoading,  ScreenError? error,  HeartPeriodLoadResult? result,  HeartVitalsOverviewDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _HeartVitalsOverviewState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.weekPeriodMode,_that.isLoading,_that.error,_that.result,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _HeartVitalsOverviewState extends HeartVitalsOverviewState {
  const _HeartVitalsOverviewState({required this.selectedDate, this.selectedRange = TimeRange.week, this.weekPeriodMode = WeekPeriodMode.mondayToSunday, this.isLoading = true, this.error, this.result, this.display}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
/// The loaded period's week mode, carried on the state (as `SleepState` does)
/// so the section summaries name the window exactly as the period navigator
/// does — "Last 30 days" on a rolling month, not "This month".
@override@JsonKey() final  WeekPeriodMode weekPeriodMode;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  HeartPeriodLoadResult? result;
@override final  HeartVitalsOverviewDisplay? display;

/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartVitalsOverviewStateCopyWith<_HeartVitalsOverviewState> get copyWith => __$HeartVitalsOverviewStateCopyWithImpl<_HeartVitalsOverviewState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartVitalsOverviewState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.weekPeriodMode, weekPeriodMode) || other.weekPeriodMode == weekPeriodMode)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,weekPeriodMode,isLoading,error,result,display);

@override
String toString() {
  return 'HeartVitalsOverviewState(selectedDate: $selectedDate, selectedRange: $selectedRange, weekPeriodMode: $weekPeriodMode, isLoading: $isLoading, error: $error, result: $result, display: $display)';
}


}

/// @nodoc
abstract mixin class _$HeartVitalsOverviewStateCopyWith<$Res> implements $HeartVitalsOverviewStateCopyWith<$Res> {
  factory _$HeartVitalsOverviewStateCopyWith(_HeartVitalsOverviewState value, $Res Function(_HeartVitalsOverviewState) _then) = __$HeartVitalsOverviewStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, WeekPeriodMode weekPeriodMode, bool isLoading, ScreenError? error, HeartPeriodLoadResult? result, HeartVitalsOverviewDisplay? display
});


@override $HeartVitalsOverviewDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$HeartVitalsOverviewStateCopyWithImpl<$Res>
    implements _$HeartVitalsOverviewStateCopyWith<$Res> {
  __$HeartVitalsOverviewStateCopyWithImpl(this._self, this._then);

  final _HeartVitalsOverviewState _self;
  final $Res Function(_HeartVitalsOverviewState) _then;

/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? weekPeriodMode = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,Object? display = freezed,}) {
  return _then(_HeartVitalsOverviewState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,weekPeriodMode: null == weekPeriodMode ? _self.weekPeriodMode : weekPeriodMode // ignore: cast_nullable_to_non_nullable
as WeekPeriodMode,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as HeartPeriodLoadResult?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as HeartVitalsOverviewDisplay?,
  ));
}

/// Create a copy of HeartVitalsOverviewState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HeartVitalsOverviewDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $HeartVitalsOverviewDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
