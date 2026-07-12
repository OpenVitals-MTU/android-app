// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_metric_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityMetricState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; ActivityPeriodData? get data; ActivityMetricDisplay? get display;/// The metric's persisted daily goal, moved by the goal card's steppers.
 double get dailyGoal;
/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityMetricStateCopyWith<ActivityMetricState> get copyWith => _$ActivityMetricStateCopyWithImpl<ActivityMetricState>(this as ActivityMetricState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.display, display) || other.display == display)&&(identical(other.dailyGoal, dailyGoal) || other.dailyGoal == dailyGoal));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data,display,dailyGoal);

@override
String toString() {
  return 'ActivityMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data, display: $display, dailyGoal: $dailyGoal)';
}


}

/// @nodoc
abstract mixin class $ActivityMetricStateCopyWith<$Res>  {
  factory $ActivityMetricStateCopyWith(ActivityMetricState value, $Res Function(ActivityMetricState) _then) = _$ActivityMetricStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, ActivityPeriodData? data, ActivityMetricDisplay? display, double dailyGoal
});


$ActivityPeriodDataCopyWith<$Res>? get data;$ActivityMetricDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$ActivityMetricStateCopyWithImpl<$Res>
    implements $ActivityMetricStateCopyWith<$Res> {
  _$ActivityMetricStateCopyWithImpl(this._self, this._then);

  final ActivityMetricState _self;
  final $Res Function(ActivityMetricState) _then;

/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? display = freezed,Object? dailyGoal = null,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as ActivityPeriodData?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as ActivityMetricDisplay?,dailyGoal: null == dailyGoal ? _self.dailyGoal : dailyGoal // ignore: cast_nullable_to_non_nullable
as double,
  ));
}
/// Create a copy of ActivityMetricState
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
}/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityMetricDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $ActivityMetricDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityMetricState].
extension ActivityMetricStatePatterns on ActivityMetricState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityMetricState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityMetricState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityMetricState value)  $default,){
final _that = this;
switch (_that) {
case _ActivityMetricState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityMetricState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityMetricState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  ActivityMetricDisplay? display,  double dailyGoal)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.display,_that.dailyGoal);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  ActivityMetricDisplay? display,  double dailyGoal)  $default,) {final _that = this;
switch (_that) {
case _ActivityMetricState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.display,_that.dailyGoal);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  ActivityPeriodData? data,  ActivityMetricDisplay? display,  double dailyGoal)?  $default,) {final _that = this;
switch (_that) {
case _ActivityMetricState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.data,_that.display,_that.dailyGoal);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityMetricState extends ActivityMetricState {
  const _ActivityMetricState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.data, this.display, this.dailyGoal = 0.0}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  ActivityPeriodData? data;
@override final  ActivityMetricDisplay? display;
/// The metric's persisted daily goal, moved by the goal card's steppers.
@override@JsonKey() final  double dailyGoal;

/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityMetricStateCopyWith<_ActivityMetricState> get copyWith => __$ActivityMetricStateCopyWithImpl<_ActivityMetricState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityMetricState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.display, display) || other.display == display)&&(identical(other.dailyGoal, dailyGoal) || other.dailyGoal == dailyGoal));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,data,display,dailyGoal);

@override
String toString() {
  return 'ActivityMetricState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, data: $data, display: $display, dailyGoal: $dailyGoal)';
}


}

/// @nodoc
abstract mixin class _$ActivityMetricStateCopyWith<$Res> implements $ActivityMetricStateCopyWith<$Res> {
  factory _$ActivityMetricStateCopyWith(_ActivityMetricState value, $Res Function(_ActivityMetricState) _then) = __$ActivityMetricStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, ActivityPeriodData? data, ActivityMetricDisplay? display, double dailyGoal
});


@override $ActivityPeriodDataCopyWith<$Res>? get data;@override $ActivityMetricDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$ActivityMetricStateCopyWithImpl<$Res>
    implements _$ActivityMetricStateCopyWith<$Res> {
  __$ActivityMetricStateCopyWithImpl(this._self, this._then);

  final _ActivityMetricState _self;
  final $Res Function(_ActivityMetricState) _then;

/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? display = freezed,Object? dailyGoal = null,}) {
  return _then(_ActivityMetricState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as ActivityPeriodData?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as ActivityMetricDisplay?,dailyGoal: null == dailyGoal ? _self.dailyGoal : dailyGoal // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

/// Create a copy of ActivityMetricState
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
}/// Create a copy of ActivityMetricState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivityMetricDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $ActivityMetricDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
