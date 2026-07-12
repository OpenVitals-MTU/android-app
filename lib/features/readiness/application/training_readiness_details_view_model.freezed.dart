// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'training_readiness_details_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$TrainingReadinessDetailsState {

 LocalDate get selectedDate; bool get isLoading; ScreenError? get error; DailyReadinessInsight? get insight; TrainingReadinessDisplay? get display;
/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$TrainingReadinessDetailsStateCopyWith<TrainingReadinessDetailsState> get copyWith => _$TrainingReadinessDetailsStateCopyWithImpl<TrainingReadinessDetailsState>(this as TrainingReadinessDetailsState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is TrainingReadinessDetailsState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.insight, insight) || other.insight == insight)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,insight,display);

@override
String toString() {
  return 'TrainingReadinessDetailsState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, insight: $insight, display: $display)';
}


}

/// @nodoc
abstract mixin class $TrainingReadinessDetailsStateCopyWith<$Res>  {
  factory $TrainingReadinessDetailsStateCopyWith(TrainingReadinessDetailsState value, $Res Function(TrainingReadinessDetailsState) _then) = _$TrainingReadinessDetailsStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DailyReadinessInsight? insight, TrainingReadinessDisplay? display
});


$DailyReadinessInsightCopyWith<$Res>? get insight;$TrainingReadinessDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$TrainingReadinessDetailsStateCopyWithImpl<$Res>
    implements $TrainingReadinessDetailsStateCopyWith<$Res> {
  _$TrainingReadinessDetailsStateCopyWithImpl(this._self, this._then);

  final TrainingReadinessDetailsState _self;
  final $Res Function(TrainingReadinessDetailsState) _then;

/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? insight = freezed,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,insight: freezed == insight ? _self.insight : insight // ignore: cast_nullable_to_non_nullable
as DailyReadinessInsight?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as TrainingReadinessDisplay?,
  ));
}
/// Create a copy of TrainingReadinessDetailsState
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
}/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$TrainingReadinessDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $TrainingReadinessDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [TrainingReadinessDetailsState].
extension TrainingReadinessDetailsStatePatterns on TrainingReadinessDetailsState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _TrainingReadinessDetailsState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _TrainingReadinessDetailsState value)  $default,){
final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _TrainingReadinessDetailsState value)?  $default,){
final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DailyReadinessInsight? insight,  TrainingReadinessDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.insight,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DailyReadinessInsight? insight,  TrainingReadinessDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState():
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.insight,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  bool isLoading,  ScreenError? error,  DailyReadinessInsight? insight,  TrainingReadinessDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _TrainingReadinessDetailsState() when $default != null:
return $default(_that.selectedDate,_that.isLoading,_that.error,_that.insight,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _TrainingReadinessDetailsState extends TrainingReadinessDetailsState {
  const _TrainingReadinessDetailsState({required this.selectedDate, this.isLoading = true, this.error, this.insight, this.display}): super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override final  DailyReadinessInsight? insight;
@override final  TrainingReadinessDisplay? display;

/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$TrainingReadinessDetailsStateCopyWith<_TrainingReadinessDetailsState> get copyWith => __$TrainingReadinessDetailsStateCopyWithImpl<_TrainingReadinessDetailsState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _TrainingReadinessDetailsState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.insight, insight) || other.insight == insight)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,isLoading,error,insight,display);

@override
String toString() {
  return 'TrainingReadinessDetailsState(selectedDate: $selectedDate, isLoading: $isLoading, error: $error, insight: $insight, display: $display)';
}


}

/// @nodoc
abstract mixin class _$TrainingReadinessDetailsStateCopyWith<$Res> implements $TrainingReadinessDetailsStateCopyWith<$Res> {
  factory _$TrainingReadinessDetailsStateCopyWith(_TrainingReadinessDetailsState value, $Res Function(_TrainingReadinessDetailsState) _then) = __$TrainingReadinessDetailsStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, bool isLoading, ScreenError? error, DailyReadinessInsight? insight, TrainingReadinessDisplay? display
});


@override $DailyReadinessInsightCopyWith<$Res>? get insight;@override $TrainingReadinessDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$TrainingReadinessDetailsStateCopyWithImpl<$Res>
    implements _$TrainingReadinessDetailsStateCopyWith<$Res> {
  __$TrainingReadinessDetailsStateCopyWithImpl(this._self, this._then);

  final _TrainingReadinessDetailsState _self;
  final $Res Function(_TrainingReadinessDetailsState) _then;

/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? isLoading = null,Object? error = freezed,Object? insight = freezed,Object? display = freezed,}) {
  return _then(_TrainingReadinessDetailsState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,insight: freezed == insight ? _self.insight : insight // ignore: cast_nullable_to_non_nullable
as DailyReadinessInsight?,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as TrainingReadinessDisplay?,
  ));
}

/// Create a copy of TrainingReadinessDetailsState
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
}/// Create a copy of TrainingReadinessDetailsState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$TrainingReadinessDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $TrainingReadinessDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
