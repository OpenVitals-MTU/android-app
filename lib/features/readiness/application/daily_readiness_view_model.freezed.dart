// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'daily_readiness_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DailyReadinessState {

 LocalDate get selectedDate; bool get isLoading; ScreenError? get error; DashboardData? get data; DailyReadinessInsight? get insight;
/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyReadinessStateCopyWith<DailyReadinessState> get copyWith => _$DailyReadinessStateCopyWithImpl<DailyReadinessState>(this as DailyReadinessState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyReadinessState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.insight, insight) || other.insight == insight));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,data,insight);

@override
String toString() {
  return 'DailyReadinessState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, data: $data, insight: $insight)';
}


}

/// @nodoc
abstract mixin class $DailyReadinessStateCopyWith<$Res>  {
  factory $DailyReadinessStateCopyWith(DailyReadinessState value, $Res Function(DailyReadinessState) _then) = _$DailyReadinessStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DashboardData? data, DailyReadinessInsight? insight
});


$DashboardDataCopyWith<$Res>? get data;$DailyReadinessInsightCopyWith<$Res>? get insight;

}
/// @nodoc
class _$DailyReadinessStateCopyWithImpl<$Res>
    implements $DailyReadinessStateCopyWith<$Res> {
  _$DailyReadinessStateCopyWithImpl(this._self, this._then);

  final DailyReadinessState _self;
  final $Res Function(DailyReadinessState) _then;

/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? insight = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,insight: freezed == insight ? _self.insight : insight // ignore: cast_nullable_to_non_nullable
as DailyReadinessInsight?,
  ));
}
/// Create a copy of DailyReadinessState
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
}/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyReadinessInsightCopyWith<$Res>? get insight {
    if (_self.insight == null) {
    return null;
  }

  return $DailyReadinessInsightCopyWith<$Res>(_self.insight!, (value) {
    return _then(_self.copyWith(insight: value));
  });
}
}


/// Adds pattern-matching-related methods to [DailyReadinessState].
extension DailyReadinessStatePatterns on DailyReadinessState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyReadinessState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyReadinessState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyReadinessState value)  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyReadinessState value)?  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  DailyReadinessInsight? insight)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyReadinessState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.insight);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  DailyReadinessInsight? insight)  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessState():
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.insight);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DashboardData? data,  DailyReadinessInsight? insight)?  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.data,_that.insight);case _:
  return null;

}
}

}

/// @nodoc


class _DailyReadinessState extends DailyReadinessState {
  const _DailyReadinessState({required this.selectedDate, this.isLoading = true, this.error, this.data, this.insight}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  DashboardData? data;
@override final  DailyReadinessInsight? insight;

/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyReadinessStateCopyWith<_DailyReadinessState> get copyWith => __$DailyReadinessStateCopyWithImpl<_DailyReadinessState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyReadinessState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.data, data) || other.data == data)&&(identical(other.insight, insight) || other.insight == insight));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,data,insight);

@override
String toString() {
  return 'DailyReadinessState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, data: $data, insight: $insight)';
}


}

/// @nodoc
abstract mixin class _$DailyReadinessStateCopyWith<$Res> implements $DailyReadinessStateCopyWith<$Res> {
  factory _$DailyReadinessStateCopyWith(_DailyReadinessState value, $Res Function(_DailyReadinessState) _then) = __$DailyReadinessStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DashboardData? data, DailyReadinessInsight? insight
});


@override $DashboardDataCopyWith<$Res>? get data;@override $DailyReadinessInsightCopyWith<$Res>? get insight;

}
/// @nodoc
class __$DailyReadinessStateCopyWithImpl<$Res>
    implements _$DailyReadinessStateCopyWith<$Res> {
  __$DailyReadinessStateCopyWithImpl(this._self, this._then);

  final _DailyReadinessState _self;
  final $Res Function(_DailyReadinessState) _then;

/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? data = freezed,Object? insight = freezed,}) {
  return _then(_DailyReadinessState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,insight: freezed == insight ? _self.insight : insight // ignore: cast_nullable_to_non_nullable
as DailyReadinessInsight?,
  ));
}

/// Create a copy of DailyReadinessState
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
}/// Create a copy of DailyReadinessState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyReadinessInsightCopyWith<$Res>? get insight {
    if (_self.insight == null) {
    return null;
  }

  return $DailyReadinessInsightCopyWith<$Res>(_self.insight!, (value) {
    return _then(_self.copyWith(insight: value));
  });
}
}

// dart format on
