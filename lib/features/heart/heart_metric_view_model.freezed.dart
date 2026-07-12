// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_metric_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartMetricState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; int get highHeartRateThresholdBpm; int get lowHeartRateThresholdBpm; ScreenError? get error; HeartPeriodLoadResult? get result;
/// Create a copy of HeartMetricState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartMetricStateCopyWith<HeartMetricState> get copyWith => _$HeartMetricStateCopyWithImpl<HeartMetricState>(this as HeartMetricState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.highHeartRateThresholdBpm, highHeartRateThresholdBpm) || other.highHeartRateThresholdBpm == highHeartRateThresholdBpm)&&(identical(other.lowHeartRateThresholdBpm, lowHeartRateThresholdBpm) || other.lowHeartRateThresholdBpm == lowHeartRateThresholdBpm)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,highHeartRateThresholdBpm,lowHeartRateThresholdBpm,error,result);

@override
String toString() {
  return 'HeartMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, highHeartRateThresholdBpm: $highHeartRateThresholdBpm, lowHeartRateThresholdBpm: $lowHeartRateThresholdBpm, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class $HeartMetricStateCopyWith<$Res>  {
  factory $HeartMetricStateCopyWith(HeartMetricState value, $Res Function(HeartMetricState) _then) = _$HeartMetricStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, int highHeartRateThresholdBpm, int lowHeartRateThresholdBpm, ScreenError? error, HeartPeriodLoadResult? result
});




}
/// @nodoc
class _$HeartMetricStateCopyWithImpl<$Res>
    implements $HeartMetricStateCopyWith<$Res> {
  _$HeartMetricStateCopyWithImpl(this._self, this._then);

  final HeartMetricState _self;
  final $Res Function(HeartMetricState) _then;

/// Create a copy of HeartMetricState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? highHeartRateThresholdBpm = null,Object? lowHeartRateThresholdBpm = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,highHeartRateThresholdBpm: null == highHeartRateThresholdBpm ? _self.highHeartRateThresholdBpm : highHeartRateThresholdBpm // ignore: cast_nullable_to_non_nullable
as int,lowHeartRateThresholdBpm: null == lowHeartRateThresholdBpm ? _self.lowHeartRateThresholdBpm : lowHeartRateThresholdBpm // ignore: cast_nullable_to_non_nullable
as int,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as HeartPeriodLoadResult?,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartMetricState].
extension HeartMetricStatePatterns on HeartMetricState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartMetricState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartMetricState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartMetricState value)  $default,){
final _that = this;
switch (_that) {
case _HeartMetricState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartMetricState value)?  $default,){
final _that = this;
switch (_that) {
case _HeartMetricState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  int highHeartRateThresholdBpm,  int lowHeartRateThresholdBpm,  ScreenError? error,  HeartPeriodLoadResult? result)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.highHeartRateThresholdBpm,_that.lowHeartRateThresholdBpm,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  int highHeartRateThresholdBpm,  int lowHeartRateThresholdBpm,  ScreenError? error,  HeartPeriodLoadResult? result)  $default,) {final _that = this;
switch (_that) {
case _HeartMetricState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.highHeartRateThresholdBpm,_that.lowHeartRateThresholdBpm,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  int highHeartRateThresholdBpm,  int lowHeartRateThresholdBpm,  ScreenError? error,  HeartPeriodLoadResult? result)?  $default,) {final _that = this;
switch (_that) {
case _HeartMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.highHeartRateThresholdBpm,_that.lowHeartRateThresholdBpm,_that.error,_that.result);case _:
  return null;

}
}

}

/// @nodoc


class _HeartMetricState extends HeartMetricState {
  const _HeartMetricState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.highHeartRateThresholdBpm = PreferencesRepository.defaultHighHeartRateThresholdBpm, this.lowHeartRateThresholdBpm = PreferencesRepository.defaultLowHeartRateThresholdBpm, this.error, this.result}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override@JsonKey() final  int highHeartRateThresholdBpm;
@override@JsonKey() final  int lowHeartRateThresholdBpm;
@override final  ScreenError? error;
@override final  HeartPeriodLoadResult? result;

/// Create a copy of HeartMetricState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartMetricStateCopyWith<_HeartMetricState> get copyWith => __$HeartMetricStateCopyWithImpl<_HeartMetricState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.highHeartRateThresholdBpm, highHeartRateThresholdBpm) || other.highHeartRateThresholdBpm == highHeartRateThresholdBpm)&&(identical(other.lowHeartRateThresholdBpm, lowHeartRateThresholdBpm) || other.lowHeartRateThresholdBpm == lowHeartRateThresholdBpm)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,highHeartRateThresholdBpm,lowHeartRateThresholdBpm,error,result);

@override
String toString() {
  return 'HeartMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, highHeartRateThresholdBpm: $highHeartRateThresholdBpm, lowHeartRateThresholdBpm: $lowHeartRateThresholdBpm, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class _$HeartMetricStateCopyWith<$Res> implements $HeartMetricStateCopyWith<$Res> {
  factory _$HeartMetricStateCopyWith(_HeartMetricState value, $Res Function(_HeartMetricState) _then) = __$HeartMetricStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, int highHeartRateThresholdBpm, int lowHeartRateThresholdBpm, ScreenError? error, HeartPeriodLoadResult? result
});




}
/// @nodoc
class __$HeartMetricStateCopyWithImpl<$Res>
    implements _$HeartMetricStateCopyWith<$Res> {
  __$HeartMetricStateCopyWithImpl(this._self, this._then);

  final _HeartMetricState _self;
  final $Res Function(_HeartMetricState) _then;

/// Create a copy of HeartMetricState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? highHeartRateThresholdBpm = null,Object? lowHeartRateThresholdBpm = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_HeartMetricState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,highHeartRateThresholdBpm: null == highHeartRateThresholdBpm ? _self.highHeartRateThresholdBpm : highHeartRateThresholdBpm // ignore: cast_nullable_to_non_nullable
as int,lowHeartRateThresholdBpm: null == lowHeartRateThresholdBpm ? _self.lowHeartRateThresholdBpm : lowHeartRateThresholdBpm // ignore: cast_nullable_to_non_nullable
as int,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as HeartPeriodLoadResult?,
  ));
}


}

// dart format on
