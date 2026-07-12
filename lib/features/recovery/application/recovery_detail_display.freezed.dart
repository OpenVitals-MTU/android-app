// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'recovery_detail_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$RecoveryDetailDisplay {

 RecoveryDay get day; SleepScoreEstimate get estimate;/// The session with the most stage-derived sleep, or null on a blank night.
 SleepData? get mainSleepSession;
/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RecoveryDetailDisplayCopyWith<RecoveryDetailDisplay> get copyWith => _$RecoveryDetailDisplayCopyWithImpl<RecoveryDetailDisplay>(this as RecoveryDetailDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RecoveryDetailDisplay&&(identical(other.day, day) || other.day == day)&&(identical(other.estimate, estimate) || other.estimate == estimate)&&(identical(other.mainSleepSession, mainSleepSession) || other.mainSleepSession == mainSleepSession));
}


@override
int get hashCode => Object.hash(runtimeType,day,estimate,mainSleepSession);

@override
String toString() {
  return 'RecoveryDetailDisplay(day: $day, estimate: $estimate, mainSleepSession: $mainSleepSession)';
}


}

/// @nodoc
abstract mixin class $RecoveryDetailDisplayCopyWith<$Res>  {
  factory $RecoveryDetailDisplayCopyWith(RecoveryDetailDisplay value, $Res Function(RecoveryDetailDisplay) _then) = _$RecoveryDetailDisplayCopyWithImpl;
@useResult
$Res call({
 RecoveryDay day, SleepScoreEstimate estimate, SleepData? mainSleepSession
});


$SleepScoreEstimateCopyWith<$Res> get estimate;$SleepDataCopyWith<$Res>? get mainSleepSession;

}
/// @nodoc
class _$RecoveryDetailDisplayCopyWithImpl<$Res>
    implements $RecoveryDetailDisplayCopyWith<$Res> {
  _$RecoveryDetailDisplayCopyWithImpl(this._self, this._then);

  final RecoveryDetailDisplay _self;
  final $Res Function(RecoveryDetailDisplay) _then;

/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? day = null,Object? estimate = null,Object? mainSleepSession = freezed,}) {
  return _then(_self.copyWith(
day: null == day ? _self.day : day // ignore: cast_nullable_to_non_nullable
as RecoveryDay,estimate: null == estimate ? _self.estimate : estimate // ignore: cast_nullable_to_non_nullable
as SleepScoreEstimate,mainSleepSession: freezed == mainSleepSession ? _self.mainSleepSession : mainSleepSession // ignore: cast_nullable_to_non_nullable
as SleepData?,
  ));
}
/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepScoreEstimateCopyWith<$Res> get estimate {
  
  return $SleepScoreEstimateCopyWith<$Res>(_self.estimate, (value) {
    return _then(_self.copyWith(estimate: value));
  });
}/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get mainSleepSession {
    if (_self.mainSleepSession == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.mainSleepSession!, (value) {
    return _then(_self.copyWith(mainSleepSession: value));
  });
}
}


/// Adds pattern-matching-related methods to [RecoveryDetailDisplay].
extension RecoveryDetailDisplayPatterns on RecoveryDetailDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RecoveryDetailDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RecoveryDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RecoveryDetailDisplay value)  $default,){
final _that = this;
switch (_that) {
case _RecoveryDetailDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RecoveryDetailDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _RecoveryDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( RecoveryDay day,  SleepScoreEstimate estimate,  SleepData? mainSleepSession)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RecoveryDetailDisplay() when $default != null:
return $default(_that.day,_that.estimate,_that.mainSleepSession);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( RecoveryDay day,  SleepScoreEstimate estimate,  SleepData? mainSleepSession)  $default,) {final _that = this;
switch (_that) {
case _RecoveryDetailDisplay():
return $default(_that.day,_that.estimate,_that.mainSleepSession);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( RecoveryDay day,  SleepScoreEstimate estimate,  SleepData? mainSleepSession)?  $default,) {final _that = this;
switch (_that) {
case _RecoveryDetailDisplay() when $default != null:
return $default(_that.day,_that.estimate,_that.mainSleepSession);case _:
  return null;

}
}

}

/// @nodoc


class _RecoveryDetailDisplay extends RecoveryDetailDisplay {
  const _RecoveryDetailDisplay({required this.day, required this.estimate, required this.mainSleepSession}): super._();
  

@override final  RecoveryDay day;
@override final  SleepScoreEstimate estimate;
/// The session with the most stage-derived sleep, or null on a blank night.
@override final  SleepData? mainSleepSession;

/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RecoveryDetailDisplayCopyWith<_RecoveryDetailDisplay> get copyWith => __$RecoveryDetailDisplayCopyWithImpl<_RecoveryDetailDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RecoveryDetailDisplay&&(identical(other.day, day) || other.day == day)&&(identical(other.estimate, estimate) || other.estimate == estimate)&&(identical(other.mainSleepSession, mainSleepSession) || other.mainSleepSession == mainSleepSession));
}


@override
int get hashCode => Object.hash(runtimeType,day,estimate,mainSleepSession);

@override
String toString() {
  return 'RecoveryDetailDisplay(day: $day, estimate: $estimate, mainSleepSession: $mainSleepSession)';
}


}

/// @nodoc
abstract mixin class _$RecoveryDetailDisplayCopyWith<$Res> implements $RecoveryDetailDisplayCopyWith<$Res> {
  factory _$RecoveryDetailDisplayCopyWith(_RecoveryDetailDisplay value, $Res Function(_RecoveryDetailDisplay) _then) = __$RecoveryDetailDisplayCopyWithImpl;
@override @useResult
$Res call({
 RecoveryDay day, SleepScoreEstimate estimate, SleepData? mainSleepSession
});


@override $SleepScoreEstimateCopyWith<$Res> get estimate;@override $SleepDataCopyWith<$Res>? get mainSleepSession;

}
/// @nodoc
class __$RecoveryDetailDisplayCopyWithImpl<$Res>
    implements _$RecoveryDetailDisplayCopyWith<$Res> {
  __$RecoveryDetailDisplayCopyWithImpl(this._self, this._then);

  final _RecoveryDetailDisplay _self;
  final $Res Function(_RecoveryDetailDisplay) _then;

/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? day = null,Object? estimate = null,Object? mainSleepSession = freezed,}) {
  return _then(_RecoveryDetailDisplay(
day: null == day ? _self.day : day // ignore: cast_nullable_to_non_nullable
as RecoveryDay,estimate: null == estimate ? _self.estimate : estimate // ignore: cast_nullable_to_non_nullable
as SleepScoreEstimate,mainSleepSession: freezed == mainSleepSession ? _self.mainSleepSession : mainSleepSession // ignore: cast_nullable_to_non_nullable
as SleepData?,
  ));
}

/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepScoreEstimateCopyWith<$Res> get estimate {
  
  return $SleepScoreEstimateCopyWith<$Res>(_self.estimate, (value) {
    return _then(_self.copyWith(estimate: value));
  });
}/// Create a copy of RecoveryDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get mainSleepSession {
    if (_self.mainSleepSession == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.mainSleepSession!, (value) {
    return _then(_self.copyWith(mainSleepSession: value));
  });
}
}

// dart format on
