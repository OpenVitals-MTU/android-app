// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cardio_load_detail_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CardioLoadState {

 LocalDate get date; bool get isLoading; ScreenError? get error; CardioLoadEstimate get estimate; int get steps; double? get activeCaloriesKcal;
/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CardioLoadStateCopyWith<CardioLoadState> get copyWith => _$CardioLoadStateCopyWithImpl<CardioLoadState>(this as CardioLoadState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CardioLoadState&&(identical(other.date, date) || other.date == date)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.estimate, estimate) || other.estimate == estimate)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal));
}


@override
int get hashCode => Object.hash(runtimeType,date,isLoading,error,estimate,steps,activeCaloriesKcal);

@override
String toString() {
  return 'CardioLoadState(date: $date, isLoading: $isLoading, error: $error, estimate: $estimate, steps: $steps, activeCaloriesKcal: $activeCaloriesKcal)';
}


}

/// @nodoc
abstract mixin class $CardioLoadStateCopyWith<$Res>  {
  factory $CardioLoadStateCopyWith(CardioLoadState value, $Res Function(CardioLoadState) _then) = _$CardioLoadStateCopyWithImpl;
@useResult
$Res call({
 LocalDate date, bool isLoading, ScreenError? error, CardioLoadEstimate estimate, int steps, double? activeCaloriesKcal
});


$CardioLoadEstimateCopyWith<$Res> get estimate;

}
/// @nodoc
class _$CardioLoadStateCopyWithImpl<$Res>
    implements $CardioLoadStateCopyWith<$Res> {
  _$CardioLoadStateCopyWithImpl(this._self, this._then);

  final CardioLoadState _self;
  final $Res Function(CardioLoadState) _then;

/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? isLoading = null,Object? error = freezed,Object? estimate = null,Object? steps = null,Object? activeCaloriesKcal = freezed,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,estimate: null == estimate ? _self.estimate : estimate // ignore: cast_nullable_to_non_nullable
as CardioLoadEstimate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}
/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CardioLoadEstimateCopyWith<$Res> get estimate {
  
  return $CardioLoadEstimateCopyWith<$Res>(_self.estimate, (value) {
    return _then(_self.copyWith(estimate: value));
  });
}
}


/// Adds pattern-matching-related methods to [CardioLoadState].
extension CardioLoadStatePatterns on CardioLoadState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CardioLoadState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CardioLoadState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CardioLoadState value)  $default,){
final _that = this;
switch (_that) {
case _CardioLoadState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CardioLoadState value)?  $default,){
final _that = this;
switch (_that) {
case _CardioLoadState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  bool isLoading,  ScreenError? error,  CardioLoadEstimate estimate,  int steps,  double? activeCaloriesKcal)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CardioLoadState() when $default != null:
return $default(_that.date,_that.isLoading,_that.error,_that.estimate,_that.steps,_that.activeCaloriesKcal);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  bool isLoading,  ScreenError? error,  CardioLoadEstimate estimate,  int steps,  double? activeCaloriesKcal)  $default,) {final _that = this;
switch (_that) {
case _CardioLoadState():
return $default(_that.date,_that.isLoading,_that.error,_that.estimate,_that.steps,_that.activeCaloriesKcal);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  bool isLoading,  ScreenError? error,  CardioLoadEstimate estimate,  int steps,  double? activeCaloriesKcal)?  $default,) {final _that = this;
switch (_that) {
case _CardioLoadState() when $default != null:
return $default(_that.date,_that.isLoading,_that.error,_that.estimate,_that.steps,_that.activeCaloriesKcal);case _:
  return null;

}
}

}

/// @nodoc


class _CardioLoadState implements CardioLoadState {
  const _CardioLoadState({required this.date, this.isLoading = true, this.error, this.estimate = CardioLoadEstimate.noData, this.steps = 0, this.activeCaloriesKcal});
  

@override final  LocalDate date;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override@JsonKey() final  CardioLoadEstimate estimate;
@override@JsonKey() final  int steps;
@override final  double? activeCaloriesKcal;

/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CardioLoadStateCopyWith<_CardioLoadState> get copyWith => __$CardioLoadStateCopyWithImpl<_CardioLoadState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CardioLoadState&&(identical(other.date, date) || other.date == date)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.estimate, estimate) || other.estimate == estimate)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal));
}


@override
int get hashCode => Object.hash(runtimeType,date,isLoading,error,estimate,steps,activeCaloriesKcal);

@override
String toString() {
  return 'CardioLoadState(date: $date, isLoading: $isLoading, error: $error, estimate: $estimate, steps: $steps, activeCaloriesKcal: $activeCaloriesKcal)';
}


}

/// @nodoc
abstract mixin class _$CardioLoadStateCopyWith<$Res> implements $CardioLoadStateCopyWith<$Res> {
  factory _$CardioLoadStateCopyWith(_CardioLoadState value, $Res Function(_CardioLoadState) _then) = __$CardioLoadStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, bool isLoading, ScreenError? error, CardioLoadEstimate estimate, int steps, double? activeCaloriesKcal
});


@override $CardioLoadEstimateCopyWith<$Res> get estimate;

}
/// @nodoc
class __$CardioLoadStateCopyWithImpl<$Res>
    implements _$CardioLoadStateCopyWith<$Res> {
  __$CardioLoadStateCopyWithImpl(this._self, this._then);

  final _CardioLoadState _self;
  final $Res Function(_CardioLoadState) _then;

/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? isLoading = null,Object? error = freezed,Object? estimate = null,Object? steps = null,Object? activeCaloriesKcal = freezed,}) {
  return _then(_CardioLoadState(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,estimate: null == estimate ? _self.estimate : estimate // ignore: cast_nullable_to_non_nullable
as CardioLoadEstimate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

/// Create a copy of CardioLoadState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CardioLoadEstimateCopyWith<$Res> get estimate {
  
  return $CardioLoadEstimateCopyWith<$Res>(_self.estimate, (value) {
    return _then(_self.copyWith(estimate: value));
  });
}
}

// dart format on
