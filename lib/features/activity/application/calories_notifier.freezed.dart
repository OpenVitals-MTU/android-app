// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'calories_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaloriesState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; ActivityPeriodData? get data; double? get latestBmrKcal;
/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaloriesStateCopyWith<CaloriesState> get copyWith => _$CaloriesStateCopyWithImpl<CaloriesState>(this as CaloriesState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaloriesState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.latestBmrKcal, latestBmrKcal) || other.latestBmrKcal == latestBmrKcal));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data,latestBmrKcal);

@override
String toString() {
  return 'CaloriesState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data, latestBmrKcal: $latestBmrKcal)';
}


}

/// @nodoc
abstract mixin class $CaloriesStateCopyWith<$Res>  {
  factory $CaloriesStateCopyWith(CaloriesState value, $Res Function(CaloriesState) _then) = _$CaloriesStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, ActivityPeriodData? data, double? latestBmrKcal
});


$ActivityPeriodDataCopyWith<$Res>? get data;

}
/// @nodoc
class _$CaloriesStateCopyWithImpl<$Res>
    implements $CaloriesStateCopyWith<$Res> {
  _$CaloriesStateCopyWithImpl(this._self, this._then);

  final CaloriesState _self;
  final $Res Function(CaloriesState) _then;

/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? latestBmrKcal = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as ActivityPeriodData?,latestBmrKcal: freezed == latestBmrKcal ? _self.latestBmrKcal : latestBmrKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}
/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityPeriodDataCopyWith<$Res>? get data {
    if (_self.data == null) {
    return null;
  }

  return $ActivityPeriodDataCopyWith<$Res>(_self.data!, (value) {
    return _then(_self.copyWith(data: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaloriesState].
extension CaloriesStatePatterns on CaloriesState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaloriesState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaloriesState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaloriesState value)  $default,){
final _that = this;
switch (_that) {
case _CaloriesState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaloriesState value)?  $default,){
final _that = this;
switch (_that) {
case _CaloriesState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  double? latestBmrKcal)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaloriesState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.latestBmrKcal);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  double? latestBmrKcal)  $default,) {final _that = this;
switch (_that) {
case _CaloriesState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.latestBmrKcal);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  double? latestBmrKcal)?  $default,) {final _that = this;
switch (_that) {
case _CaloriesState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.latestBmrKcal);case _:
  return null;

}
}

}

/// @nodoc


class _CaloriesState extends CaloriesState {
  const _CaloriesState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.data, this.latestBmrKcal}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  ActivityPeriodData? data;
@override final  double? latestBmrKcal;

/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaloriesStateCopyWith<_CaloriesState> get copyWith => __$CaloriesStateCopyWithImpl<_CaloriesState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaloriesState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.latestBmrKcal, latestBmrKcal) || other.latestBmrKcal == latestBmrKcal));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data,latestBmrKcal);

@override
String toString() {
  return 'CaloriesState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data, latestBmrKcal: $latestBmrKcal)';
}


}

/// @nodoc
abstract mixin class _$CaloriesStateCopyWith<$Res> implements $CaloriesStateCopyWith<$Res> {
  factory _$CaloriesStateCopyWith(_CaloriesState value, $Res Function(_CaloriesState) _then) = __$CaloriesStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, ActivityPeriodData? data, double? latestBmrKcal
});


@override $ActivityPeriodDataCopyWith<$Res>? get data;

}
/// @nodoc
class __$CaloriesStateCopyWithImpl<$Res>
    implements _$CaloriesStateCopyWith<$Res> {
  __$CaloriesStateCopyWithImpl(this._self, this._then);

  final _CaloriesState _self;
  final $Res Function(_CaloriesState) _then;

/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? latestBmrKcal = freezed,}) {
  return _then(_CaloriesState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as ActivityPeriodData?,latestBmrKcal: freezed == latestBmrKcal ? _self.latestBmrKcal : latestBmrKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

/// Create a copy of CaloriesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityPeriodDataCopyWith<$Res>? get data {
    if (_self.data == null) {
    return null;
  }

  return $ActivityPeriodDataCopyWith<$Res>(_self.data!, (value) {
    return _then(_self.copyWith(data: value));
  });
}
}

// dart format on
