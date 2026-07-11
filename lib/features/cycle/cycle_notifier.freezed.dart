// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cycle_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CycleMetricState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; CyclePeriodData? get result;
/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CycleMetricStateCopyWith<CycleMetricState> get copyWith => _$CycleMetricStateCopyWithImpl<CycleMetricState>(this as CycleMetricState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CycleMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,result);

@override
String toString() {
  return 'CycleMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class $CycleMetricStateCopyWith<$Res>  {
  factory $CycleMetricStateCopyWith(CycleMetricState value, $Res Function(CycleMetricState) _then) = _$CycleMetricStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, CyclePeriodData? result
});


$CyclePeriodDataCopyWith<$Res>? get result;

}
/// @nodoc
class _$CycleMetricStateCopyWithImpl<$Res>
    implements $CycleMetricStateCopyWith<$Res> {
  _$CycleMetricStateCopyWithImpl(this._self, this._then);

  final CycleMetricState _self;
  final $Res Function(CycleMetricState) _then;

/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as CyclePeriodData?,
  ));
}
/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CyclePeriodDataCopyWith<$Res>? get result {
    if (_self.result == null) {
    return null;
  }

  return $CyclePeriodDataCopyWith<$Res>(_self.result!, (value) {
    return _then(_self.copyWith(result: value));
  });
}
}


/// Adds pattern-matching-related methods to [CycleMetricState].
extension CycleMetricStatePatterns on CycleMetricState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CycleMetricState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CycleMetricState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CycleMetricState value)  $default,){
final _that = this;
switch (_that) {
case _CycleMetricState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CycleMetricState value)?  $default,){
final _that = this;
switch (_that) {
case _CycleMetricState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  CyclePeriodData? result)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CycleMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  CyclePeriodData? result)  $default,) {final _that = this;
switch (_that) {
case _CycleMetricState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  CyclePeriodData? result)?  $default,) {final _that = this;
switch (_that) {
case _CycleMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.result);case _:
  return null;

}
}

}

/// @nodoc


class _CycleMetricState extends CycleMetricState {
  const _CycleMetricState({required this.selectedDate, this.selectedRange = TimeRange.month, this.isLoading = true, this.error, this.result}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  CyclePeriodData? result;

/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CycleMetricStateCopyWith<_CycleMetricState> get copyWith => __$CycleMetricStateCopyWithImpl<_CycleMetricState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CycleMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,result);

@override
String toString() {
  return 'CycleMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class _$CycleMetricStateCopyWith<$Res> implements $CycleMetricStateCopyWith<$Res> {
  factory _$CycleMetricStateCopyWith(_CycleMetricState value, $Res Function(_CycleMetricState) _then) = __$CycleMetricStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, CyclePeriodData? result
});


@override $CyclePeriodDataCopyWith<$Res>? get result;

}
/// @nodoc
class __$CycleMetricStateCopyWithImpl<$Res>
    implements _$CycleMetricStateCopyWith<$Res> {
  __$CycleMetricStateCopyWithImpl(this._self, this._then);

  final _CycleMetricState _self;
  final $Res Function(_CycleMetricState) _then;

/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_CycleMetricState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as CyclePeriodData?,
  ));
}

/// Create a copy of CycleMetricState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CyclePeriodDataCopyWith<$Res>? get result {
    if (_self.result == null) {
    return null;
  }

  return $CyclePeriodDataCopyWith<$Res>(_self.result!, (value) {
    return _then(_self.copyWith(result: value));
  });
}
}

// dart format on
