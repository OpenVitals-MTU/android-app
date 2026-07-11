// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepPeriodData {

 List<SleepData> get sessions; List<SleepData> get previousSessions; List<SleepData> get baselineSessions; List<DailySleepDuration> get dailyDurations; List<DailySleepDuration> get previousDailyDurations; List<DailySleepDuration> get baselineDailyDurations;
/// Create a copy of SleepPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepPeriodDataCopyWith<SleepPeriodData> get copyWith => _$SleepPeriodDataCopyWithImpl<SleepPeriodData>(this as SleepPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepPeriodData&&const DeepCollectionEquality().equals(other.sessions, sessions)&&const DeepCollectionEquality().equals(other.previousSessions, previousSessions)&&const DeepCollectionEquality().equals(other.baselineSessions, baselineSessions)&&const DeepCollectionEquality().equals(other.dailyDurations, dailyDurations)&&const DeepCollectionEquality().equals(other.previousDailyDurations, previousDailyDurations)&&const DeepCollectionEquality().equals(other.baselineDailyDurations, baselineDailyDurations));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(sessions),const DeepCollectionEquality().hash(previousSessions),const DeepCollectionEquality().hash(baselineSessions),const DeepCollectionEquality().hash(dailyDurations),const DeepCollectionEquality().hash(previousDailyDurations),const DeepCollectionEquality().hash(baselineDailyDurations));

@override
String toString() {
  return 'SleepPeriodData(sessions: $sessions, previousSessions: $previousSessions, baselineSessions: $baselineSessions, dailyDurations: $dailyDurations, previousDailyDurations: $previousDailyDurations, baselineDailyDurations: $baselineDailyDurations)';
}


}

/// @nodoc
abstract mixin class $SleepPeriodDataCopyWith<$Res>  {
  factory $SleepPeriodDataCopyWith(SleepPeriodData value, $Res Function(SleepPeriodData) _then) = _$SleepPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<SleepData> sessions, List<SleepData> previousSessions, List<SleepData> baselineSessions, List<DailySleepDuration> dailyDurations, List<DailySleepDuration> previousDailyDurations, List<DailySleepDuration> baselineDailyDurations
});




}
/// @nodoc
class _$SleepPeriodDataCopyWithImpl<$Res>
    implements $SleepPeriodDataCopyWith<$Res> {
  _$SleepPeriodDataCopyWithImpl(this._self, this._then);

  final SleepPeriodData _self;
  final $Res Function(SleepPeriodData) _then;

/// Create a copy of SleepPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? sessions = null,Object? previousSessions = null,Object? baselineSessions = null,Object? dailyDurations = null,Object? previousDailyDurations = null,Object? baselineDailyDurations = null,}) {
  return _then(_self.copyWith(
sessions: null == sessions ? _self.sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,previousSessions: null == previousSessions ? _self.previousSessions : previousSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,baselineSessions: null == baselineSessions ? _self.baselineSessions : baselineSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailyDurations: null == dailyDurations ? _self.dailyDurations : dailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,previousDailyDurations: null == previousDailyDurations ? _self.previousDailyDurations : previousDailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,baselineDailyDurations: null == baselineDailyDurations ? _self.baselineDailyDurations : baselineDailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepPeriodData].
extension SleepPeriodDataPatterns on SleepPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _SleepPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _SleepPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<SleepData> sessions,  List<SleepData> previousSessions,  List<SleepData> baselineSessions,  List<DailySleepDuration> dailyDurations,  List<DailySleepDuration> previousDailyDurations,  List<DailySleepDuration> baselineDailyDurations)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepPeriodData() when $default != null:
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions,_that.dailyDurations,_that.previousDailyDurations,_that.baselineDailyDurations);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<SleepData> sessions,  List<SleepData> previousSessions,  List<SleepData> baselineSessions,  List<DailySleepDuration> dailyDurations,  List<DailySleepDuration> previousDailyDurations,  List<DailySleepDuration> baselineDailyDurations)  $default,) {final _that = this;
switch (_that) {
case _SleepPeriodData():
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions,_that.dailyDurations,_that.previousDailyDurations,_that.baselineDailyDurations);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<SleepData> sessions,  List<SleepData> previousSessions,  List<SleepData> baselineSessions,  List<DailySleepDuration> dailyDurations,  List<DailySleepDuration> previousDailyDurations,  List<DailySleepDuration> baselineDailyDurations)?  $default,) {final _that = this;
switch (_that) {
case _SleepPeriodData() when $default != null:
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions,_that.dailyDurations,_that.previousDailyDurations,_that.baselineDailyDurations);case _:
  return null;

}
}

}

/// @nodoc


class _SleepPeriodData implements SleepPeriodData {
  const _SleepPeriodData({final  List<SleepData> sessions = const <SleepData>[], final  List<SleepData> previousSessions = const <SleepData>[], final  List<SleepData> baselineSessions = const <SleepData>[], final  List<DailySleepDuration> dailyDurations = const <DailySleepDuration>[], final  List<DailySleepDuration> previousDailyDurations = const <DailySleepDuration>[], final  List<DailySleepDuration> baselineDailyDurations = const <DailySleepDuration>[]}): _sessions = sessions,_previousSessions = previousSessions,_baselineSessions = baselineSessions,_dailyDurations = dailyDurations,_previousDailyDurations = previousDailyDurations,_baselineDailyDurations = baselineDailyDurations;
  

 final  List<SleepData> _sessions;
@override@JsonKey() List<SleepData> get sessions {
  if (_sessions is EqualUnmodifiableListView) return _sessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sessions);
}

 final  List<SleepData> _previousSessions;
@override@JsonKey() List<SleepData> get previousSessions {
  if (_previousSessions is EqualUnmodifiableListView) return _previousSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousSessions);
}

 final  List<SleepData> _baselineSessions;
@override@JsonKey() List<SleepData> get baselineSessions {
  if (_baselineSessions is EqualUnmodifiableListView) return _baselineSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineSessions);
}

 final  List<DailySleepDuration> _dailyDurations;
@override@JsonKey() List<DailySleepDuration> get dailyDurations {
  if (_dailyDurations is EqualUnmodifiableListView) return _dailyDurations;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyDurations);
}

 final  List<DailySleepDuration> _previousDailyDurations;
@override@JsonKey() List<DailySleepDuration> get previousDailyDurations {
  if (_previousDailyDurations is EqualUnmodifiableListView) return _previousDailyDurations;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyDurations);
}

 final  List<DailySleepDuration> _baselineDailyDurations;
@override@JsonKey() List<DailySleepDuration> get baselineDailyDurations {
  if (_baselineDailyDurations is EqualUnmodifiableListView) return _baselineDailyDurations;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyDurations);
}


/// Create a copy of SleepPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepPeriodDataCopyWith<_SleepPeriodData> get copyWith => __$SleepPeriodDataCopyWithImpl<_SleepPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepPeriodData&&const DeepCollectionEquality().equals(other._sessions, _sessions)&&const DeepCollectionEquality().equals(other._previousSessions, _previousSessions)&&const DeepCollectionEquality().equals(other._baselineSessions, _baselineSessions)&&const DeepCollectionEquality().equals(other._dailyDurations, _dailyDurations)&&const DeepCollectionEquality().equals(other._previousDailyDurations, _previousDailyDurations)&&const DeepCollectionEquality().equals(other._baselineDailyDurations, _baselineDailyDurations));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_sessions),const DeepCollectionEquality().hash(_previousSessions),const DeepCollectionEquality().hash(_baselineSessions),const DeepCollectionEquality().hash(_dailyDurations),const DeepCollectionEquality().hash(_previousDailyDurations),const DeepCollectionEquality().hash(_baselineDailyDurations));

@override
String toString() {
  return 'SleepPeriodData(sessions: $sessions, previousSessions: $previousSessions, baselineSessions: $baselineSessions, dailyDurations: $dailyDurations, previousDailyDurations: $previousDailyDurations, baselineDailyDurations: $baselineDailyDurations)';
}


}

/// @nodoc
abstract mixin class _$SleepPeriodDataCopyWith<$Res> implements $SleepPeriodDataCopyWith<$Res> {
  factory _$SleepPeriodDataCopyWith(_SleepPeriodData value, $Res Function(_SleepPeriodData) _then) = __$SleepPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<SleepData> sessions, List<SleepData> previousSessions, List<SleepData> baselineSessions, List<DailySleepDuration> dailyDurations, List<DailySleepDuration> previousDailyDurations, List<DailySleepDuration> baselineDailyDurations
});




}
/// @nodoc
class __$SleepPeriodDataCopyWithImpl<$Res>
    implements _$SleepPeriodDataCopyWith<$Res> {
  __$SleepPeriodDataCopyWithImpl(this._self, this._then);

  final _SleepPeriodData _self;
  final $Res Function(_SleepPeriodData) _then;

/// Create a copy of SleepPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? sessions = null,Object? previousSessions = null,Object? baselineSessions = null,Object? dailyDurations = null,Object? previousDailyDurations = null,Object? baselineDailyDurations = null,}) {
  return _then(_SleepPeriodData(
sessions: null == sessions ? _self._sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,previousSessions: null == previousSessions ? _self._previousSessions : previousSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,baselineSessions: null == baselineSessions ? _self._baselineSessions : baselineSessions // ignore: cast_nullable_to_non_nullable
as List<SleepData>,dailyDurations: null == dailyDurations ? _self._dailyDurations : dailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,previousDailyDurations: null == previousDailyDurations ? _self._previousDailyDurations : previousDailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,baselineDailyDurations: null == baselineDailyDurations ? _self._baselineDailyDurations : baselineDailyDurations // ignore: cast_nullable_to_non_nullable
as List<DailySleepDuration>,
  ));
}


}

// dart format on
