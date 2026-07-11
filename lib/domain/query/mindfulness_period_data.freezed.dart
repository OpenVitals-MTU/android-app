// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mindfulness_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$MindfulnessPeriodData {

 List<MindfulnessSession> get sessions; List<MindfulnessSession> get previousSessions; List<MindfulnessSession> get baselineSessions;
/// Create a copy of MindfulnessPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessPeriodDataCopyWith<MindfulnessPeriodData> get copyWith => _$MindfulnessPeriodDataCopyWithImpl<MindfulnessPeriodData>(this as MindfulnessPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessPeriodData&&const DeepCollectionEquality().equals(other.sessions, sessions)&&const DeepCollectionEquality().equals(other.previousSessions, previousSessions)&&const DeepCollectionEquality().equals(other.baselineSessions, baselineSessions));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(sessions),const DeepCollectionEquality().hash(previousSessions),const DeepCollectionEquality().hash(baselineSessions));

@override
String toString() {
  return 'MindfulnessPeriodData(sessions: $sessions, previousSessions: $previousSessions, baselineSessions: $baselineSessions)';
}


}

/// @nodoc
abstract mixin class $MindfulnessPeriodDataCopyWith<$Res>  {
  factory $MindfulnessPeriodDataCopyWith(MindfulnessPeriodData value, $Res Function(MindfulnessPeriodData) _then) = _$MindfulnessPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<MindfulnessSession> sessions, List<MindfulnessSession> previousSessions, List<MindfulnessSession> baselineSessions
});




}
/// @nodoc
class _$MindfulnessPeriodDataCopyWithImpl<$Res>
    implements $MindfulnessPeriodDataCopyWith<$Res> {
  _$MindfulnessPeriodDataCopyWithImpl(this._self, this._then);

  final MindfulnessPeriodData _self;
  final $Res Function(MindfulnessPeriodData) _then;

/// Create a copy of MindfulnessPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? sessions = null,Object? previousSessions = null,Object? baselineSessions = null,}) {
  return _then(_self.copyWith(
sessions: null == sessions ? _self.sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,previousSessions: null == previousSessions ? _self.previousSessions : previousSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,baselineSessions: null == baselineSessions ? _self.baselineSessions : baselineSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessPeriodData].
extension MindfulnessPeriodDataPatterns on MindfulnessPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<MindfulnessSession> sessions,  List<MindfulnessSession> previousSessions,  List<MindfulnessSession> baselineSessions)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessPeriodData() when $default != null:
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<MindfulnessSession> sessions,  List<MindfulnessSession> previousSessions,  List<MindfulnessSession> baselineSessions)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessPeriodData():
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<MindfulnessSession> sessions,  List<MindfulnessSession> previousSessions,  List<MindfulnessSession> baselineSessions)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessPeriodData() when $default != null:
return $default(_that.sessions,_that.previousSessions,_that.baselineSessions);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessPeriodData implements MindfulnessPeriodData {
  const _MindfulnessPeriodData({final  List<MindfulnessSession> sessions = const <MindfulnessSession>[], final  List<MindfulnessSession> previousSessions = const <MindfulnessSession>[], final  List<MindfulnessSession> baselineSessions = const <MindfulnessSession>[]}): _sessions = sessions,_previousSessions = previousSessions,_baselineSessions = baselineSessions;
  

 final  List<MindfulnessSession> _sessions;
@override@JsonKey() List<MindfulnessSession> get sessions {
  if (_sessions is EqualUnmodifiableListView) return _sessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sessions);
}

 final  List<MindfulnessSession> _previousSessions;
@override@JsonKey() List<MindfulnessSession> get previousSessions {
  if (_previousSessions is EqualUnmodifiableListView) return _previousSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousSessions);
}

 final  List<MindfulnessSession> _baselineSessions;
@override@JsonKey() List<MindfulnessSession> get baselineSessions {
  if (_baselineSessions is EqualUnmodifiableListView) return _baselineSessions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineSessions);
}


/// Create a copy of MindfulnessPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessPeriodDataCopyWith<_MindfulnessPeriodData> get copyWith => __$MindfulnessPeriodDataCopyWithImpl<_MindfulnessPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessPeriodData&&const DeepCollectionEquality().equals(other._sessions, _sessions)&&const DeepCollectionEquality().equals(other._previousSessions, _previousSessions)&&const DeepCollectionEquality().equals(other._baselineSessions, _baselineSessions));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_sessions),const DeepCollectionEquality().hash(_previousSessions),const DeepCollectionEquality().hash(_baselineSessions));

@override
String toString() {
  return 'MindfulnessPeriodData(sessions: $sessions, previousSessions: $previousSessions, baselineSessions: $baselineSessions)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessPeriodDataCopyWith<$Res> implements $MindfulnessPeriodDataCopyWith<$Res> {
  factory _$MindfulnessPeriodDataCopyWith(_MindfulnessPeriodData value, $Res Function(_MindfulnessPeriodData) _then) = __$MindfulnessPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<MindfulnessSession> sessions, List<MindfulnessSession> previousSessions, List<MindfulnessSession> baselineSessions
});




}
/// @nodoc
class __$MindfulnessPeriodDataCopyWithImpl<$Res>
    implements _$MindfulnessPeriodDataCopyWith<$Res> {
  __$MindfulnessPeriodDataCopyWithImpl(this._self, this._then);

  final _MindfulnessPeriodData _self;
  final $Res Function(_MindfulnessPeriodData) _then;

/// Create a copy of MindfulnessPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? sessions = null,Object? previousSessions = null,Object? baselineSessions = null,}) {
  return _then(_MindfulnessPeriodData(
sessions: null == sessions ? _self._sessions : sessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,previousSessions: null == previousSessions ? _self._previousSessions : previousSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,baselineSessions: null == baselineSessions ? _self._baselineSessions : baselineSessions // ignore: cast_nullable_to_non_nullable
as List<MindfulnessSession>,
  ));
}


}

// dart format on
