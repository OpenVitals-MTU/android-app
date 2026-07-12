// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'recovery_detail_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RecoveryDetailState {

 LocalDate get selectedDate; bool get isLoading; List<RecoveryDay> get days; ScreenError? get error; RecoveryDetailDisplay? get display;
/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RecoveryDetailStateCopyWith<RecoveryDetailState> get copyWith => _$RecoveryDetailStateCopyWithImpl<RecoveryDetailState>(this as RecoveryDetailState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RecoveryDetailState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&const DeepCollectionEquality().equals(other.days, days)&&(identical(other.error, error) || other.error == error)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,const DeepCollectionEquality().hash(days),error,display);

@override
String toString() {
  return 'RecoveryDetailState(selectedDate: $selectedDate, isLoading: $isLoading, days: $days, error: $error, display: $display)';
}


}

/// @nodoc
abstract mixin class $RecoveryDetailStateCopyWith<$Res>  {
  factory $RecoveryDetailStateCopyWith(RecoveryDetailState value, $Res Function(RecoveryDetailState) _then) = _$RecoveryDetailStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, bool isLoading, List<RecoveryDay> days, ScreenError? error, RecoveryDetailDisplay? display
});


$RecoveryDetailDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$RecoveryDetailStateCopyWithImpl<$Res>
    implements $RecoveryDetailStateCopyWith<$Res> {
  _$RecoveryDetailStateCopyWithImpl(this._self, this._then);

  final RecoveryDetailState _self;
  final $Res Function(RecoveryDetailState) _then;

/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? isLoading = null,Object? days = null,Object? error = freezed,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,days: null == days ? _self.days : days // ignore: cast_nullable_to_non_nullable
as List<RecoveryDay>,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as RecoveryDetailDisplay?,
  ));
}
/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RecoveryDetailDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $RecoveryDetailDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [RecoveryDetailState].
extension RecoveryDetailStatePatterns on RecoveryDetailState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RecoveryDetailState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RecoveryDetailState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RecoveryDetailState value)  $default,){
final _that = this;
switch (_that) {
case _RecoveryDetailState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RecoveryDetailState value)?  $default,){
final _that = this;
switch (_that) {
case _RecoveryDetailState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  List<RecoveryDay> days,  ScreenError? error,  RecoveryDetailDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RecoveryDetailState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.days,_that.error,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  List<RecoveryDay> days,  ScreenError? error,  RecoveryDetailDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _RecoveryDetailState():
return $default(_that.selectedDate,_that.isLoading,_that.days,_that.error,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  bool isLoading,  List<RecoveryDay> days,  ScreenError? error,  RecoveryDetailDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _RecoveryDetailState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.days,_that.error,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _RecoveryDetailState extends RecoveryDetailState {
  const _RecoveryDetailState({required this.selectedDate, this.isLoading = true, final  List<RecoveryDay> days = const <RecoveryDay>[], this.error, this.display}): _days = days,super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  bool isLoading;
 final  List<RecoveryDay> _days;
@override@JsonKey() List<RecoveryDay> get days {
  if (_days is EqualUnmodifiableListView) return _days;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_days);
}

@override final  ScreenError? error;
@override final  RecoveryDetailDisplay? display;

/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RecoveryDetailStateCopyWith<_RecoveryDetailState> get copyWith => __$RecoveryDetailStateCopyWithImpl<_RecoveryDetailState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RecoveryDetailState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&const DeepCollectionEquality().equals(other._days, _days)&&(identical(other.error, error) || other.error == error)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,const DeepCollectionEquality().hash(_days),error,display);

@override
String toString() {
  return 'RecoveryDetailState(selectedDate: $selectedDate, isLoading: $isLoading, days: $days, error: $error, display: $display)';
}


}

/// @nodoc
abstract mixin class _$RecoveryDetailStateCopyWith<$Res> implements $RecoveryDetailStateCopyWith<$Res> {
  factory _$RecoveryDetailStateCopyWith(_RecoveryDetailState value, $Res Function(_RecoveryDetailState) _then) = __$RecoveryDetailStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, bool isLoading, List<RecoveryDay> days, ScreenError? error, RecoveryDetailDisplay? display
});


@override $RecoveryDetailDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$RecoveryDetailStateCopyWithImpl<$Res>
    implements _$RecoveryDetailStateCopyWith<$Res> {
  __$RecoveryDetailStateCopyWithImpl(this._self, this._then);

  final _RecoveryDetailState _self;
  final $Res Function(_RecoveryDetailState) _then;

/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? isLoading = null,Object? days = null,Object? error = freezed,Object? display = freezed,}) {
  return _then(_RecoveryDetailState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,days: null == days ? _self._days : days // ignore: cast_nullable_to_non_nullable
as List<RecoveryDay>,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as RecoveryDetailDisplay?,
  ));
}

/// Create a copy of RecoveryDetailState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RecoveryDetailDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $RecoveryDetailDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
