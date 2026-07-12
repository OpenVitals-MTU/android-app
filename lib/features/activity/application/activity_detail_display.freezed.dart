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
mixin _$ActivityDetailDisplay {

 int get pausedDurationMs; int get movingDurationMs;/// The cadence kinds the session recorded, in enum order — one card each.
 List<ActivityCadenceKind> get cadenceKinds;/// The slowest and fastest split, in seconds per kilometre. Null when no
/// split has a pace (which is what leaves the bars unpainted).
 double? get slowestSplitPaceSeconds; double? get fastestSplitPaceSeconds;/// The GPS route's length, in metres. Zero when there is no route.
 double get routeDistanceMeters;
/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityDetailDisplayCopyWith<ActivityDetailDisplay> get copyWith => _$ActivityDetailDisplayCopyWithImpl<ActivityDetailDisplay>(this as ActivityDetailDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityDetailDisplay&&(identical(other.pausedDurationMs, pausedDurationMs) || other.pausedDurationMs == pausedDurationMs)&&(identical(other.movingDurationMs, movingDurationMs) || other.movingDurationMs == movingDurationMs)&&const DeepCollectionEquality().equals(other.cadenceKinds, cadenceKinds)&&(identical(other.slowestSplitPaceSeconds, slowestSplitPaceSeconds) || other.slowestSplitPaceSeconds == slowestSplitPaceSeconds)&&(identical(other.fastestSplitPaceSeconds, fastestSplitPaceSeconds) || other.fastestSplitPaceSeconds == fastestSplitPaceSeconds)&&(identical(other.routeDistanceMeters, routeDistanceMeters) || other.routeDistanceMeters == routeDistanceMeters));
}


@override
int get hashCode => Object.hash(runtimeType,pausedDurationMs,movingDurationMs,const DeepCollectionEquality().hash(cadenceKinds),slowestSplitPaceSeconds,fastestSplitPaceSeconds,routeDistanceMeters);

@override
String toString() {
  return 'ActivityDetailDisplay(pausedDurationMs: $pausedDurationMs, movingDurationMs: $movingDurationMs, cadenceKinds: $cadenceKinds, slowestSplitPaceSeconds: $slowestSplitPaceSeconds, fastestSplitPaceSeconds: $fastestSplitPaceSeconds, routeDistanceMeters: $routeDistanceMeters)';
}


}

/// @nodoc
abstract mixin class $ActivityDetailDisplayCopyWith<$Res>  {
  factory $ActivityDetailDisplayCopyWith(ActivityDetailDisplay value, $Res Function(ActivityDetailDisplay) _then) = _$ActivityDetailDisplayCopyWithImpl;
@useResult
$Res call({
 int pausedDurationMs, int movingDurationMs, List<ActivityCadenceKind> cadenceKinds, double? slowestSplitPaceSeconds, double? fastestSplitPaceSeconds, double routeDistanceMeters
});




}
/// @nodoc
class _$ActivityDetailDisplayCopyWithImpl<$Res>
    implements $ActivityDetailDisplayCopyWith<$Res> {
  _$ActivityDetailDisplayCopyWithImpl(this._self, this._then);

  final ActivityDetailDisplay _self;
  final $Res Function(ActivityDetailDisplay) _then;

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? pausedDurationMs = null,Object? movingDurationMs = null,Object? cadenceKinds = null,Object? slowestSplitPaceSeconds = freezed,Object? fastestSplitPaceSeconds = freezed,Object? routeDistanceMeters = null,}) {
  return _then(_self.copyWith(
pausedDurationMs: null == pausedDurationMs ? _self.pausedDurationMs : pausedDurationMs // ignore: cast_nullable_to_non_nullable
as int,movingDurationMs: null == movingDurationMs ? _self.movingDurationMs : movingDurationMs // ignore: cast_nullable_to_non_nullable
as int,cadenceKinds: null == cadenceKinds ? _self.cadenceKinds : cadenceKinds // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceKind>,slowestSplitPaceSeconds: freezed == slowestSplitPaceSeconds ? _self.slowestSplitPaceSeconds : slowestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,fastestSplitPaceSeconds: freezed == fastestSplitPaceSeconds ? _self.fastestSplitPaceSeconds : fastestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,routeDistanceMeters: null == routeDistanceMeters ? _self.routeDistanceMeters : routeDistanceMeters // ignore: cast_nullable_to_non_nullable
as double,
  ));
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters)  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay():
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int pausedDurationMs,  int movingDurationMs,  List<ActivityCadenceKind> cadenceKinds,  double? slowestSplitPaceSeconds,  double? fastestSplitPaceSeconds,  double routeDistanceMeters)?  $default,) {final _that = this;
switch (_that) {
case _ActivityDetailDisplay() when $default != null:
return $default(_that.pausedDurationMs,_that.movingDurationMs,_that.cadenceKinds,_that.slowestSplitPaceSeconds,_that.fastestSplitPaceSeconds,_that.routeDistanceMeters);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityDetailDisplay implements ActivityDetailDisplay {
  const _ActivityDetailDisplay({required this.pausedDurationMs, required this.movingDurationMs, required final  List<ActivityCadenceKind> cadenceKinds, required this.slowestSplitPaceSeconds, required this.fastestSplitPaceSeconds, required this.routeDistanceMeters}): _cadenceKinds = cadenceKinds;
  

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

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityDetailDisplayCopyWith<_ActivityDetailDisplay> get copyWith => __$ActivityDetailDisplayCopyWithImpl<_ActivityDetailDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityDetailDisplay&&(identical(other.pausedDurationMs, pausedDurationMs) || other.pausedDurationMs == pausedDurationMs)&&(identical(other.movingDurationMs, movingDurationMs) || other.movingDurationMs == movingDurationMs)&&const DeepCollectionEquality().equals(other._cadenceKinds, _cadenceKinds)&&(identical(other.slowestSplitPaceSeconds, slowestSplitPaceSeconds) || other.slowestSplitPaceSeconds == slowestSplitPaceSeconds)&&(identical(other.fastestSplitPaceSeconds, fastestSplitPaceSeconds) || other.fastestSplitPaceSeconds == fastestSplitPaceSeconds)&&(identical(other.routeDistanceMeters, routeDistanceMeters) || other.routeDistanceMeters == routeDistanceMeters));
}


@override
int get hashCode => Object.hash(runtimeType,pausedDurationMs,movingDurationMs,const DeepCollectionEquality().hash(_cadenceKinds),slowestSplitPaceSeconds,fastestSplitPaceSeconds,routeDistanceMeters);

@override
String toString() {
  return 'ActivityDetailDisplay(pausedDurationMs: $pausedDurationMs, movingDurationMs: $movingDurationMs, cadenceKinds: $cadenceKinds, slowestSplitPaceSeconds: $slowestSplitPaceSeconds, fastestSplitPaceSeconds: $fastestSplitPaceSeconds, routeDistanceMeters: $routeDistanceMeters)';
}


}

/// @nodoc
abstract mixin class _$ActivityDetailDisplayCopyWith<$Res> implements $ActivityDetailDisplayCopyWith<$Res> {
  factory _$ActivityDetailDisplayCopyWith(_ActivityDetailDisplay value, $Res Function(_ActivityDetailDisplay) _then) = __$ActivityDetailDisplayCopyWithImpl;
@override @useResult
$Res call({
 int pausedDurationMs, int movingDurationMs, List<ActivityCadenceKind> cadenceKinds, double? slowestSplitPaceSeconds, double? fastestSplitPaceSeconds, double routeDistanceMeters
});




}
/// @nodoc
class __$ActivityDetailDisplayCopyWithImpl<$Res>
    implements _$ActivityDetailDisplayCopyWith<$Res> {
  __$ActivityDetailDisplayCopyWithImpl(this._self, this._then);

  final _ActivityDetailDisplay _self;
  final $Res Function(_ActivityDetailDisplay) _then;

/// Create a copy of ActivityDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? pausedDurationMs = null,Object? movingDurationMs = null,Object? cadenceKinds = null,Object? slowestSplitPaceSeconds = freezed,Object? fastestSplitPaceSeconds = freezed,Object? routeDistanceMeters = null,}) {
  return _then(_ActivityDetailDisplay(
pausedDurationMs: null == pausedDurationMs ? _self.pausedDurationMs : pausedDurationMs // ignore: cast_nullable_to_non_nullable
as int,movingDurationMs: null == movingDurationMs ? _self.movingDurationMs : movingDurationMs // ignore: cast_nullable_to_non_nullable
as int,cadenceKinds: null == cadenceKinds ? _self._cadenceKinds : cadenceKinds // ignore: cast_nullable_to_non_nullable
as List<ActivityCadenceKind>,slowestSplitPaceSeconds: freezed == slowestSplitPaceSeconds ? _self.slowestSplitPaceSeconds : slowestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,fastestSplitPaceSeconds: freezed == fastestSplitPaceSeconds ? _self.fastestSplitPaceSeconds : fastestSplitPaceSeconds // ignore: cast_nullable_to_non_nullable
as double?,routeDistanceMeters: null == routeDistanceMeters ? _self.routeDistanceMeters : routeDistanceMeters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

// dart format on
