// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_detail_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivitySplitSpeedTrace {

/// Two points per split — its start and its end, at the same speed.
 List<ActivitySpeedTraceSample> get samples;/// How many splits are behind the trace. The card counts splits, not
/// samples: there is no such thing as a sample here.
 int get splitCount;/// Total distance over total elapsed, across the splits that are drawn.
///
/// Stated rather than left to the chart, which would take the mean of the
/// plotted points: with equal-distance splits that is their arithmetic
/// mean, and average speed over equal distances is the HARMONIC mean — so
/// the chart would quietly report a slightly faster session than happened,
/// and disagree with the average speed in the header of the same screen.
 double get averageMetersPerSecond;
/// Create a copy of ActivitySplitSpeedTrace
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivitySplitSpeedTraceCopyWith<ActivitySplitSpeedTrace> get copyWith => _$ActivitySplitSpeedTraceCopyWithImpl<ActivitySplitSpeedTrace>(this as ActivitySplitSpeedTrace, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivitySplitSpeedTrace&&const DeepCollectionEquality().equals(other.samples, samples)&&(identical(other.splitCount, splitCount) || other.splitCount == splitCount)&&(identical(other.averageMetersPerSecond, averageMetersPerSecond) || other.averageMetersPerSecond == averageMetersPerSecond));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(samples),splitCount,averageMetersPerSecond);

@override
String toString() {
  return 'ActivitySplitSpeedTrace(samples: $samples, splitCount: $splitCount, averageMetersPerSecond: $averageMetersPerSecond)';
}


}

/// @nodoc
abstract mixin class $ActivitySplitSpeedTraceCopyWith<$Res>  {
  factory $ActivitySplitSpeedTraceCopyWith(ActivitySplitSpeedTrace value, $Res Function(ActivitySplitSpeedTrace) _then) = _$ActivitySplitSpeedTraceCopyWithImpl;
@useResult
$Res call({
 List<ActivitySpeedTraceSample> samples, int splitCount, double averageMetersPerSecond
});




}
/// @nodoc
class _$ActivitySplitSpeedTraceCopyWithImpl<$Res>
    implements $ActivitySplitSpeedTraceCopyWith<$Res> {
  _$ActivitySplitSpeedTraceCopyWithImpl(this._self, this._then);

  final ActivitySplitSpeedTrace _self;
  final $Res Function(ActivitySplitSpeedTrace) _then;

/// Create a copy of ActivitySplitSpeedTrace
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? samples = null,Object? splitCount = null,Object? averageMetersPerSecond = null,}) {
  return _then(_self.copyWith(
samples: null == samples ? _self.samples : samples // ignore: cast_nullable_to_non_nullable
as List<ActivitySpeedTraceSample>,splitCount: null == splitCount ? _self.splitCount : splitCount // ignore: cast_nullable_to_non_nullable
as int,averageMetersPerSecond: null == averageMetersPerSecond ? _self.averageMetersPerSecond : averageMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivitySplitSpeedTrace].
extension ActivitySplitSpeedTracePatterns on ActivitySplitSpeedTrace {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivitySplitSpeedTrace value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivitySplitSpeedTrace value)  $default,){
final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivitySplitSpeedTrace value)?  $default,){
final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<ActivitySpeedTraceSample> samples,  int splitCount,  double averageMetersPerSecond)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace() when $default != null:
return $default(_that.samples,_that.splitCount,_that.averageMetersPerSecond);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<ActivitySpeedTraceSample> samples,  int splitCount,  double averageMetersPerSecond)  $default,) {final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace():
return $default(_that.samples,_that.splitCount,_that.averageMetersPerSecond);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<ActivitySpeedTraceSample> samples,  int splitCount,  double averageMetersPerSecond)?  $default,) {final _that = this;
switch (_that) {
case _ActivitySplitSpeedTrace() when $default != null:
return $default(_that.samples,_that.splitCount,_that.averageMetersPerSecond);case _:
  return null;

}
}

}

/// @nodoc


class _ActivitySplitSpeedTrace implements ActivitySplitSpeedTrace {
  const _ActivitySplitSpeedTrace({required final  List<ActivitySpeedTraceSample> samples, required this.splitCount, required this.averageMetersPerSecond}): _samples = samples;
  

/// Two points per split — its start and its end, at the same speed.
 final  List<ActivitySpeedTraceSample> _samples;
/// Two points per split — its start and its end, at the same speed.
@override List<ActivitySpeedTraceSample> get samples {
  if (_samples is EqualUnmodifiableListView) return _samples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_samples);
}

/// How many splits are behind the trace. The card counts splits, not
/// samples: there is no such thing as a sample here.
@override final  int splitCount;
/// Total distance over total elapsed, across the splits that are drawn.
///
/// Stated rather than left to the chart, which would take the mean of the
/// plotted points: with equal-distance splits that is their arithmetic
/// mean, and average speed over equal distances is the HARMONIC mean — so
/// the chart would quietly report a slightly faster session than happened,
/// and disagree with the average speed in the header of the same screen.
@override final  double averageMetersPerSecond;

/// Create a copy of ActivitySplitSpeedTrace
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivitySplitSpeedTraceCopyWith<_ActivitySplitSpeedTrace> get copyWith => __$ActivitySplitSpeedTraceCopyWithImpl<_ActivitySplitSpeedTrace>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivitySplitSpeedTrace&&const DeepCollectionEquality().equals(other._samples, _samples)&&(identical(other.splitCount, splitCount) || other.splitCount == splitCount)&&(identical(other.averageMetersPerSecond, averageMetersPerSecond) || other.averageMetersPerSecond == averageMetersPerSecond));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_samples),splitCount,averageMetersPerSecond);

@override
String toString() {
  return 'ActivitySplitSpeedTrace(samples: $samples, splitCount: $splitCount, averageMetersPerSecond: $averageMetersPerSecond)';
}


}

/// @nodoc
abstract mixin class _$ActivitySplitSpeedTraceCopyWith<$Res> implements $ActivitySplitSpeedTraceCopyWith<$Res> {
  factory _$ActivitySplitSpeedTraceCopyWith(_ActivitySplitSpeedTrace value, $Res Function(_ActivitySplitSpeedTrace) _then) = __$ActivitySplitSpeedTraceCopyWithImpl;
@override @useResult
$Res call({
 List<ActivitySpeedTraceSample> samples, int splitCount, double averageMetersPerSecond
});




}
/// @nodoc
class __$ActivitySplitSpeedTraceCopyWithImpl<$Res>
    implements _$ActivitySplitSpeedTraceCopyWith<$Res> {
  __$ActivitySplitSpeedTraceCopyWithImpl(this._self, this._then);

  final _ActivitySplitSpeedTrace _self;
  final $Res Function(_ActivitySplitSpeedTrace) _then;

/// Create a copy of ActivitySplitSpeedTrace
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? samples = null,Object? splitCount = null,Object? averageMetersPerSecond = null,}) {
  return _then(_ActivitySplitSpeedTrace(
samples: null == samples ? _self._samples : samples // ignore: cast_nullable_to_non_nullable
as List<ActivitySpeedTraceSample>,splitCount: null == splitCount ? _self.splitCount : splitCount // ignore: cast_nullable_to_non_nullable
as int,averageMetersPerSecond: null == averageMetersPerSecond ? _self.averageMetersPerSecond : averageMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$ActivityDetailDisplay {

 int get pausedDurationMs; int get movingDurationMs;/// The cadence kinds the session recorded, in enum order — one card each.
 List<ActivityCadenceKind> get cadenceKinds;/// The slowest and fastest split, in seconds per kilometre. Null when no
/// split has a pace (which is what leaves the bars unpainted).
 double? get slowestSplitPaceSeconds; double? get fastestSplitPaceSeconds;/// The GPS route's length, in metres. Zero when there is no route.
 double get routeDistanceMeters;/// The height profile of the session, oldest first.
///
/// It comes from the ROUTE, not from a record of its own: Health Connect
/// has no elevation series. `ElevationGainedRecord` is a single total for
/// the session — it says you climbed 240 m, never where. The altitude on
/// each route point is the only thing in Health Connect that knows the
/// shape of a climb, and we already read it.
///
/// Empty when the route has no altitude, or has only one point that does:
/// a single height is not a profile.
 List<ActivityElevationSample> get elevationSamples;/// Speed rebuilt from the splits, for a session that recorded none.
///
/// Null whenever it must not be drawn — see [_splitSpeedTrace] for the two
/// cases (a real trace exists, or the splits are the estimated kind and are
/// flat by construction).
 ActivitySplitSpeedTrace? get splitSpeedTrace;
/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityDetailDisplayCopyWith<ActivityDetailDisplay> get copyWith => _$ActivityDetailDisplayCopyWithImpl<ActivityDetailDisplay>(this as ActivityDetailDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityDetailDisplay&&(identical(other.pausedDurationMs, pausedDurationMs) || other.pausedDurationMs == pausedDurationMs)&&(identical(other.movingDurationMs, movingDurationMs) || other.movingDurationMs == movingDurationMs)&&const DeepCollectionEquality().equals(other.cadenceKinds, cadenceKinds)&&(identical(other.slowestSplitPaceSeconds, slowestSplitPaceSeconds) || other.slowestSplitPaceSeconds == slowestSplitPaceSeconds)&&(identical(other.fastestSplitPaceSeconds, fastestSplitPaceSeconds) || other.fastestSplitPaceSeconds == fastestSplitPaceSeconds)&&(identical(other.routeDistanceMeters, routeDistanceMeters) || other.routeDistanceMeters == routeDistanceMeters)&&const DeepCollectionEquality().equals(other.elevationSamples, elevationSamples)&&(identical(other.splitSpeedTrace, splitSpeedTrace) || other.splitSpeedTrace == splitSpeedTrace));
}


@override
int get hashCode => Object.hash(runtimeType,pausedDurationMs,movingDurationMs,const DeepCollectionEquality().hash(cadenceKinds),slowestSplitPaceSeconds,fastestSplitPaceSeconds,routeDistanceMeters,const DeepCollectionEquality().hash(elevationSamples),splitSpeedTrace);

@override
String toString() {
  return 'ActivityDetailDisplay(pausedDurationMs: $pausedDurationMs, movingDurationMs: $movingDurationMs, cadenceKinds: $cadenceKinds, slowestSplitPaceSeconds: $slowestSplitPaceSeconds, fastestSplitPaceSeconds: $fastestSplitPaceSeconds, routeDistanceMeters: $routeDistanceMeters, elevationSamples: $elevationSamples, splitSpeedTrace: $splitSpeedTrace)';
}


}

/// @nodoc
abstract mixin class $ActivityDetailDisplayCopyWith<$Res>  {
  factory $ActivityDetailDisplayCopyWith(ActivityDetailDisplay value, $Res Function(ActivityDetailDisplay) _then) = _$ActivityDetailDisplayCopyWithImpl;
@useResult
$Res call({
 int pausedDurationMs, int movingDurationMs, List<ActivityCadenceKind> cadenceKinds, double? slowestSplitPaceSeconds, double? fastestSplitPaceSeconds, double routeDistanceMeters, List<ActivityElevationSample> elevationSamples, ActivitySplitSpeedTrace? splitSpeedTrace
});


$ActivitySplitSpeedTraceCopyWith<$Res>? get splitSpeedTrace;

}
/// @nodoc
class _$ActivityDetailDisplayCopyWithImpl<$Res>
    implements $ActivityDetailDisplayCopyWith<$Res> {
  _$ActivityDetailDisplayCopyWithImpl(this._self, this._then);

  final ActivityDetailDisplay _self;
  final $Res Function(ActivityDetailDisplay) _then;

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? pausedDurationMs = null,Object? movingDurationMs = null,Object? cadenceKinds = null,Object? slowestSplitPaceSeconds = freezed,Object? fastestSplitPaceSeconds = freezed,Object? routeDistanceMeters = null,Object? elevationSamples = null,Object? splitSpeedTrace = freezed,}) {
  return _then(_self.copyWith(
pausedDurationMs: null == pausedDurationMs ? _self.pausedDurationMs : pausedDurationMs // ignore: cast_nullable_to_non_nullable
as int,movingDurationMs: null == movingDurationMs ? _self.movingDurationMs : movingDurationMs // ignore: cast_nullable_to_non_nullable
as int,cadenceKinds: null == cadenceKinds ? _self.cadenceKinds : cadenceKinds // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceKind>,slowestSplitPaceSeconds: freezed == slowestSplitPaceSeconds ? _self.slowestSplitPaceSeconds : slowestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,fastestSplitPaceSeconds: freezed == fastestSplitPaceSeconds ? _self.fastestSplitPaceSeconds : fastestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,routeDistanceMeters: null == routeDistanceMeters ? _self.routeDistanceMeters : routeDistanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationSamples: null == elevationSamples ? _self.elevationSamples : elevationSamples // ignore: cast_nullable_to_non_nullable
as List<ActivityElevationSample>,splitSpeedTrace: freezed == splitSpeedTrace ? _self.splitSpeedTrace : splitSpeedTrace // ignore: cast_nullable_to_non_nullable
as ActivitySplitSpeedTrace?,
  ));
}
/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivitySplitSpeedTraceCopyWith<$Res>? get splitSpeedTrace {
    if (_self.splitSpeedTrace == null) {
    return null;
  }

  return $ActivitySplitSpeedTraceCopyWith<$Res>(_self.splitSpeedTrace!, (value) {
    return _then(_self.copyWith(splitSpeedTrace: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityDetailDisplay].
extension ActivityDetailDisplayPatterns on ActivityDetailDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityDetailDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityDetailDisplay value)  $default,){
final _that = this;
switch (_that) {
case _ActivityDetailDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityDetailDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters,  List<ActivityElevationSample> elevationSamples,  ActivitySplitSpeedTrace? splitSpeedTrace)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters,_that.elevationSamples,_that.splitSpeedTrace);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters,  List<ActivityElevationSample> elevationSamples,  ActivitySplitSpeedTrace? splitSpeedTrace)  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay():
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters,_that.elevationSamples,_that.splitSpeedTrace);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters,  List<ActivityElevationSample> elevationSamples,  ActivitySplitSpeedTrace? splitSpeedTrace)?  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters,_that.elevationSamples,_that.splitSpeedTrace);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityDetailDisplay implements ActivityDetailDisplay {
  const _ActivityDetailDisplay({required this.pausedDurationMs, required this.movingDurationMs, required final  List<ActivityCadenceKind> cadenceKinds, required this.slowestSplitPaceSeconds, required this.fastestSplitPaceSeconds, required this.routeDistanceMeters, required final  List<ActivityElevationSample> elevationSamples, required this.splitSpeedTrace}): _cadenceKinds = cadenceKinds,_elevationSamples = elevationSamples;
  

@override final  int pausedDurationMs;
@override final  int movingDurationMs;
/// The cadence kinds the session recorded, in enum order — one card each.
 final  List<ActivityCadenceKind> _cadenceKinds;
/// The cadence kinds the session recorded, in enum order — one card each.
@override List<ActivityCadenceKind> get cadenceKinds {
  if (_cadenceKinds is EqualUnmodifiableListView) return _cadenceKinds;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cadenceKinds);
}

/// The slowest and fastest split, in seconds per kilometre. Null when no
/// split has a pace (which is what leaves the bars unpainted).
@override final  double? slowestSplitPaceSeconds;
@override final  double? fastestSplitPaceSeconds;
/// The GPS route's length, in metres. Zero when there is no route.
@override final  double routeDistanceMeters;
/// The height profile of the session, oldest first.
///
/// It comes from the ROUTE, not from a record of its own: Health Connect
/// has no elevation series. `ElevationGainedRecord` is a single total for
/// the session — it says you climbed 240 m, never where. The altitude on
/// each route point is the only thing in Health Connect that knows the
/// shape of a climb, and we already read it.
///
/// Empty when the route has no altitude, or has only one point that does:
/// a single height is not a profile.
 final  List<ActivityElevationSample> _elevationSamples;
/// The height profile of the session, oldest first.
///
/// It comes from the ROUTE, not from a record of its own: Health Connect
/// has no elevation series. `ElevationGainedRecord` is a single total for
/// the session — it says you climbed 240 m, never where. The altitude on
/// each route point is the only thing in Health Connect that knows the
/// shape of a climb, and we already read it.
///
/// Empty when the route has no altitude, or has only one point that does:
/// a single height is not a profile.
@override List<ActivityElevationSample> get elevationSamples {
  if (_elevationSamples is EqualUnmodifiableListView) return _elevationSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_elevationSamples);
}

/// Speed rebuilt from the splits, for a session that recorded none.
///
/// Null whenever it must not be drawn — see [_splitSpeedTrace] for the two
/// cases (a real trace exists, or the splits are the estimated kind and are
/// flat by construction).
@override final  ActivitySplitSpeedTrace? splitSpeedTrace;

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityDetailDisplayCopyWith<_ActivityDetailDisplay> get copyWith => __$ActivityDetailDisplayCopyWithImpl<_ActivityDetailDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityDetailDisplay&&(identical(other.pausedDurationMs, pausedDurationMs) || other.pausedDurationMs == pausedDurationMs)&&(identical(other.movingDurationMs, movingDurationMs) || other.movingDurationMs == movingDurationMs)&&const DeepCollectionEquality().equals(other._cadenceKinds, _cadenceKinds)&&(identical(other.slowestSplitPaceSeconds, slowestSplitPaceSeconds) || other.slowestSplitPaceSeconds == slowestSplitPaceSeconds)&&(identical(other.fastestSplitPaceSeconds, fastestSplitPaceSeconds) || other.fastestSplitPaceSeconds == fastestSplitPaceSeconds)&&(identical(other.routeDistanceMeters, routeDistanceMeters) || other.routeDistanceMeters == routeDistanceMeters)&&const DeepCollectionEquality().equals(other._elevationSamples, _elevationSamples)&&(identical(other.splitSpeedTrace, splitSpeedTrace) || other.splitSpeedTrace == splitSpeedTrace));
}


@override
int get hashCode => Object.hash(runtimeType,pausedDurationMs,movingDurationMs,const DeepCollectionEquality().hash(_cadenceKinds),slowestSplitPaceSeconds,fastestSplitPaceSeconds,routeDistanceMeters,const DeepCollectionEquality().hash(_elevationSamples),splitSpeedTrace);

@override
String toString() {
  return 'ActivityDetailDisplay(pausedDurationMs: $pausedDurationMs, movingDurationMs: $movingDurationMs, cadenceKinds: $cadenceKinds, slowestSplitPaceSeconds: $slowestSplitPaceSeconds, fastestSplitPaceSeconds: $fastestSplitPaceSeconds, routeDistanceMeters: $routeDistanceMeters, elevationSamples: $elevationSamples, splitSpeedTrace: $splitSpeedTrace)';
}


}

/// @nodoc
abstract mixin class _$ActivityDetailDisplayCopyWith<$Res> implements $ActivityDetailDisplayCopyWith<$Res> {
  factory _$ActivityDetailDisplayCopyWith(_ActivityDetailDisplay value, $Res Function(_ActivityDetailDisplay) _then) = __$ActivityDetailDisplayCopyWithImpl;
@override @useResult
$Res call({
 int pausedDurationMs, int movingDurationMs, List<ActivityCadenceKind> cadenceKinds, double? slowestSplitPaceSeconds, double? fastestSplitPaceSeconds, double routeDistanceMeters, List<ActivityElevationSample> elevationSamples, ActivitySplitSpeedTrace? splitSpeedTrace
});


@override $ActivitySplitSpeedTraceCopyWith<$Res>? get splitSpeedTrace;

}
/// @nodoc
class __$ActivityDetailDisplayCopyWithImpl<$Res>
    implements _$ActivityDetailDisplayCopyWith<$Res> {
  __$ActivityDetailDisplayCopyWithImpl(this._self, this._then);

  final _ActivityDetailDisplay _self;
  final $Res Function(_ActivityDetailDisplay) _then;

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? pausedDurationMs = null,Object? movingDurationMs = null,Object? cadenceKinds = null,Object? slowestSplitPaceSeconds = freezed,Object? fastestSplitPaceSeconds = freezed,Object? routeDistanceMeters = null,Object? elevationSamples = null,Object? splitSpeedTrace = freezed,}) {
  return _then(_ActivityDetailDisplay(
pausedDurationMs: null == pausedDurationMs ? _self.pausedDurationMs : pausedDurationMs // ignore: cast_nullable_to_non_nullable
as int,movingDurationMs: null == movingDurationMs ? _self.movingDurationMs : movingDurationMs // ignore: cast_nullable_to_non_nullable
as int,cadenceKinds: null == cadenceKinds ? _self._cadenceKinds : cadenceKinds // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceKind>,slowestSplitPaceSeconds: freezed == slowestSplitPaceSeconds ? _self.slowestSplitPaceSeconds : slowestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,fastestSplitPaceSeconds: freezed == fastestSplitPaceSeconds ? _self.fastestSplitPaceSeconds : fastestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,routeDistanceMeters: null == routeDistanceMeters ? _self.routeDistanceMeters : routeDistanceMeters // ignore: cast_nullable_to_non_nullable
as double,elevationSamples: null == elevationSamples ? _self._elevationSamples : elevationSamples // ignore: cast_nullable_to_non_nullable
as List<ActivityElevationSample>,splitSpeedTrace: freezed == splitSpeedTrace ? _self.splitSpeedTrace : splitSpeedTrace // ignore: cast_nullable_to_non_nullable
as ActivitySplitSpeedTrace?,
  ));
}

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ActivitySplitSpeedTraceCopyWith<$Res>? get splitSpeedTrace {
    if (_self.splitSpeedTrace == null) {
    return null;
  }

  return $ActivitySplitSpeedTraceCopyWith<$Res>(_self.splitSpeedTrace!, (value) {
    return _then(_self.copyWith(splitSpeedTrace: value));
  });
}
}

// dart format on
