// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_energy_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyEnergyState {

 LocalDate get selectedDate; bool get isLoading; ScreenError? get error; BodyEnergyTimelineResult? get result;
/// Create a copy of BodyEnergyState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyEnergyStateCopyWith<BodyEnergyState> get copyWith => _$BodyEnergyStateCopyWithImpl<BodyEnergyState>(this as BodyEnergyState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyEnergyState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,result);

@override
String toString() {
  return 'BodyEnergyState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class $BodyEnergyStateCopyWith<$Res>  {
  factory $BodyEnergyStateCopyWith(BodyEnergyState value, $Res Function(BodyEnergyState) _then) = _$BodyEnergyStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, BodyEnergyTimelineResult? result
});




}
/// @nodoc
class _$BodyEnergyStateCopyWithImpl<$Res>
    implements $BodyEnergyStateCopyWith<$Res> {
  _$BodyEnergyStateCopyWithImpl(this._self, this._then);

  final BodyEnergyState _self;
  final $Res Function(BodyEnergyState) _then;

/// Create a copy of BodyEnergyState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as BodyEnergyTimelineResult?,
  ));
}

}


/// Adds pattern-matching-related methods to [BodyEnergyState].
extension BodyEnergyStatePatterns on BodyEnergyState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyEnergyState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyEnergyState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyEnergyState value)  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyEnergyState value)?  $default,){
final _that = this;
switch (_that) {
case _BodyEnergyState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  BodyEnergyTimelineResult? result)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyEnergyState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  BodyEnergyTimelineResult? result)  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyState():
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.result);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  BodyEnergyTimelineResult? result)?  $default,) {final _that = this;
switch (_that) {
case _BodyEnergyState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.result);case _:
  return null;

}
}

}

/// @nodoc


class _BodyEnergyState extends BodyEnergyState {
  const _BodyEnergyState({required this.selectedDate, this.isLoading = true, this.error, this.result}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  BodyEnergyTimelineResult? result;

/// Create a copy of BodyEnergyState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyEnergyStateCopyWith<_BodyEnergyState> get copyWith => __$BodyEnergyStateCopyWithImpl<_BodyEnergyState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyEnergyState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.result, result) || other.result == result));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,result);

@override
String toString() {
  return 'BodyEnergyState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, result: $result)';
}


}

/// @nodoc
abstract mixin class _$BodyEnergyStateCopyWith<$Res> implements $BodyEnergyStateCopyWith<$Res> {
  factory _$BodyEnergyStateCopyWith(_BodyEnergyState value, $Res Function(_BodyEnergyState) _then) = __$BodyEnergyStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, BodyEnergyTimelineResult? result
});




}
/// @nodoc
class __$BodyEnergyStateCopyWithImpl<$Res>
    implements _$BodyEnergyStateCopyWith<$Res> {
  __$BodyEnergyStateCopyWithImpl(this._self, this._then);

  final _BodyEnergyState _self;
  final $Res Function(_BodyEnergyState) _then;

/// Create a copy of BodyEnergyState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? result = freezed,}) {
  return _then(_BodyEnergyState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,result: freezed == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as BodyEnergyTimelineResult?,
  ));
}


}

// dart format on
