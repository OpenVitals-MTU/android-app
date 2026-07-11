// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'recovery_notifier.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RecoveryState {

 LocalDate get selectedDate; bool get isLoading; ScreenError? get error; DashboardData? get data; PhysiologicalStressEstimate? get stress;
/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RecoveryStateCopyWith<RecoveryState> get copyWith => _$RecoveryStateCopyWithImpl<RecoveryState>(this as RecoveryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RecoveryState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.stress, stress) || other.stress == stress));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,data,stress);

@override
String toString() {
  return 'RecoveryState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, data: $data, stress: $stress)';
}


}

/// @nodoc
abstract mixin class $RecoveryStateCopyWith<$Res>  {
  factory $RecoveryStateCopyWith(RecoveryState value, $Res Function(RecoveryState) _then) = _$RecoveryStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DashboardData? data, PhysiologicalStressEstimate? stress
});


$DashboardDataCopyWith<$Res>? get data;$PhysiologicalStressEstimateCopyWith<$Res>? get stress;

}
/// @nodoc
class _$RecoveryStateCopyWithImpl<$Res>
    implements $RecoveryStateCopyWith<$Res> {
  _$RecoveryStateCopyWithImpl(this._self, this._then);

  final RecoveryState _self;
  final $Res Function(RecoveryState) _then;

/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? stress = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,stress: freezed == stress ? _self.stress : stress // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressEstimate?,
  ));
}
/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardDataCopyWith<$Res>? get data {
    if (_self.data == null) {
    return null;
  }

  return $DashboardDataCopyWith<$Res>(_self.data!, (value) {
    return _then(_self.copyWith(data: value));
  });
}/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PhysiologicalStressEstimateCopyWith<$Res>? get stress {
    if (_self.stress == null) {
    return null;
  }

  return $PhysiologicalStressEstimateCopyWith<$Res>(_self.stress!, (value) {
    return _then(_self.copyWith(stress: value));
  });
}
}


/// Adds pattern-matching-related methods to [RecoveryState].
extension RecoveryStatePatterns on RecoveryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RecoveryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RecoveryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RecoveryState value)  $default,){
final _that = this;
switch (_that) {
case _RecoveryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RecoveryState value)?  $default,){
final _that = this;
switch (_that) {
case _RecoveryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  PhysiologicalStressEstimate? stress)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RecoveryState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.stress);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  PhysiologicalStressEstimate? stress)  $default,) {final _that = this;
switch (_that) {
case _RecoveryState():
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.stress);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  PhysiologicalStressEstimate? stress)?  $default,) {final _that = this;
switch (_that) {
case _RecoveryState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.stress);case _:
  return null;

}
}

}

/// @nodoc


class _RecoveryState extends RecoveryState {
  const _RecoveryState({required this.selectedDate, this.isLoading = true, this.error, this.data, this.stress}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  DashboardData? data;
@override final  PhysiologicalStressEstimate? stress;

/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RecoveryStateCopyWith<_RecoveryState> get copyWith => __$RecoveryStateCopyWithImpl<_RecoveryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RecoveryState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.stress, stress) || other.stress == stress));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,data,stress);

@override
String toString() {
  return 'RecoveryState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, data: $data, stress: $stress)';
}


}

/// @nodoc
abstract mixin class _$RecoveryStateCopyWith<$Res> implements $RecoveryStateCopyWith<$Res> {
  factory _$RecoveryStateCopyWith(_RecoveryState value, $Res Function(_RecoveryState) _then) = __$RecoveryStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DashboardData? data, PhysiologicalStressEstimate? stress
});


@override $DashboardDataCopyWith<$Res>? get data;@override $PhysiologicalStressEstimateCopyWith<$Res>? get stress;

}
/// @nodoc
class __$RecoveryStateCopyWithImpl<$Res>
    implements _$RecoveryStateCopyWith<$Res> {
  __$RecoveryStateCopyWithImpl(this._self, this._then);

  final _RecoveryState _self;
  final $Res Function(_RecoveryState) _then;

/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? stress = freezed,}) {
  return _then(_RecoveryState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,stress: freezed == stress ? _self.stress : stress // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressEstimate?,
  ));
}

/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardDataCopyWith<$Res>? get data {
    if (_self.data == null) {
    return null;
  }

  return $DashboardDataCopyWith<$Res>(_self.data!, (value) {
    return _then(_self.copyWith(data: value));
  });
}/// Create a copy of RecoveryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PhysiologicalStressEstimateCopyWith<$Res>? get stress {
    if (_self.stress == null) {
    return null;
  }

  return $PhysiologicalStressEstimateCopyWith<$Res>(_self.stress!, (value) {
    return _then(_self.copyWith(stress: value));
  });
}
}

// dart format on
