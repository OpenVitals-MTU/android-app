// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_rate_recovery_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartRateRecoveryState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; HeartRateRecoveryPeriodData? get data;
/// Create a copy of HeartRateRecoveryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateRecoveryStateCopyWith<HeartRateRecoveryState> get copyWith => _$HeartRateRecoveryStateCopyWithImpl<HeartRateRecoveryState>(this as HeartRateRecoveryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateRecoveryState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data);

@override
String toString() {
  return 'HeartRateRecoveryState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data)';
}


}

/// @nodoc
abstract mixin class $HeartRateRecoveryStateCopyWith<$Res>  {
  factory $HeartRateRecoveryStateCopyWith(HeartRateRecoveryState value, $Res Function(HeartRateRecoveryState) _then) = _$HeartRateRecoveryStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, HeartRateRecoveryPeriodData? data
});




}
/// @nodoc
class _$HeartRateRecoveryStateCopyWithImpl<$Res>
    implements $HeartRateRecoveryStateCopyWith<$Res> {
  _$HeartRateRecoveryStateCopyWithImpl(this._self, this._then);

  final HeartRateRecoveryState _self;
  final $Res Function(HeartRateRecoveryState) _then;

/// Create a copy of HeartRateRecoveryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryPeriodData?,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateRecoveryState].
extension HeartRateRecoveryStatePatterns on HeartRateRecoveryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateRecoveryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateRecoveryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateRecoveryState value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateRecoveryState value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  HeartRateRecoveryPeriodData? data)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateRecoveryState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  HeartRateRecoveryPeriodData? data)  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  HeartRateRecoveryPeriodData? data)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateRecoveryState extends HeartRateRecoveryState {
  const _HeartRateRecoveryState({required this.selectedDate, this.selectedRange = TimeRange.month, this.isLoading = true, this.error, this.data}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  HeartRateRecoveryPeriodData? data;

/// Create a copy of HeartRateRecoveryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateRecoveryStateCopyWith<_HeartRateRecoveryState> get copyWith => __$HeartRateRecoveryStateCopyWithImpl<_HeartRateRecoveryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateRecoveryState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data);

@override
String toString() {
  return 'HeartRateRecoveryState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data)';
}


}

/// @nodoc
abstract mixin class _$HeartRateRecoveryStateCopyWith<$Res> implements $HeartRateRecoveryStateCopyWith<$Res> {
  factory _$HeartRateRecoveryStateCopyWith(_HeartRateRecoveryState value, $Res Function(_HeartRateRecoveryState) _then) = __$HeartRateRecoveryStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, HeartRateRecoveryPeriodData? data
});




}
/// @nodoc
class __$HeartRateRecoveryStateCopyWithImpl<$Res>
    implements _$HeartRateRecoveryStateCopyWith<$Res> {
  __$HeartRateRecoveryStateCopyWithImpl(this._self, this._then);

  final _HeartRateRecoveryState _self;
  final $Res Function(_HeartRateRecoveryState) _then;

/// Create a copy of HeartRateRecoveryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,}) {
  return _then(_HeartRateRecoveryState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryPeriodData?,
  ));
}


}

// dart format on
