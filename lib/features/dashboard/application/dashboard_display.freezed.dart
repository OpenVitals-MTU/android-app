// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'dashboard_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DashboardDisplay {

/// Every tile in the user's saved order, hidden ones included — the set the
/// edit grid and the add-tray are computed from.
 List<StatTileData> get orderedTiles;/// The tiles the carousel actually shows.
 List<StatTileData> get visibleTiles;/// Both hero rings in the user's saved order, hidden ones included.
 List<RingCardData> get orderedRings;/// The hero rings the top row actually shows.
 List<RingCardData> get visibleRings;/// The effective hidden set: the saved one, plus (while editing) every
/// unsupported tile the user has never deliberately placed.
 Set<String> get hiddenTitles;/// The titles materialised only because the device does not support the
/// metric (see [DashboardSummary.unsupportedTitles]). Empty outside edit
/// mode.
 Set<String> get unsupportedTitles;/// The add-tray: the removed rings and tiles, in layout order.
 List<String> get trayTitles;/// Today's activities, with the single-workout fallback already folded in.
 List<ExerciseData> get activities;
/// Create a copy of DashboardDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardDisplayCopyWith<DashboardDisplay> get copyWith => _$DashboardDisplayCopyWithImpl<DashboardDisplay>(this as DashboardDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardDisplay&&const DeepCollectionEquality().equals(other.orderedTiles, orderedTiles)&&const DeepCollectionEquality().equals(other.visibleTiles, visibleTiles)&&const DeepCollectionEquality().equals(other.orderedRings, orderedRings)&&const DeepCollectionEquality().equals(other.visibleRings, visibleRings)&&const DeepCollectionEquality().equals(other.hiddenTitles, hiddenTitles)&&const DeepCollectionEquality().equals(other.unsupportedTitles, unsupportedTitles)&&const DeepCollectionEquality().equals(other.trayTitles, trayTitles)&&const DeepCollectionEquality().equals(other.activities, activities));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(orderedTiles),const DeepCollectionEquality().hash(visibleTiles),const DeepCollectionEquality().hash(orderedRings),const DeepCollectionEquality().hash(visibleRings),const DeepCollectionEquality().hash(hiddenTitles),const DeepCollectionEquality().hash(unsupportedTitles),const DeepCollectionEquality().hash(trayTitles),const DeepCollectionEquality().hash(activities));

@override
String toString() {
  return 'DashboardDisplay(orderedTiles: $orderedTiles, visibleTiles: $visibleTiles, orderedRings: $orderedRings, visibleRings: $visibleRings, hiddenTitles: $hiddenTitles, unsupportedTitles: $unsupportedTitles, trayTitles: $trayTitles, activities: $activities)';
}


}

/// @nodoc
abstract mixin class $DashboardDisplayCopyWith<$Res>  {
  factory $DashboardDisplayCopyWith(DashboardDisplay value, $Res Function(DashboardDisplay) _then) = _$DashboardDisplayCopyWithImpl;
@useResult
$Res call({
 List<StatTileData> orderedTiles, List<StatTileData> visibleTiles, List<RingCardData> orderedRings, List<RingCardData> visibleRings, Set<String> hiddenTitles, Set<String> unsupportedTitles, List<String> trayTitles, List<ExerciseData> activities
});




}
/// @nodoc
class _$DashboardDisplayCopyWithImpl<$Res>
    implements $DashboardDisplayCopyWith<$Res> {
  _$DashboardDisplayCopyWithImpl(this._self, this._then);

  final DashboardDisplay _self;
  final $Res Function(DashboardDisplay) _then;

/// Create a copy of DashboardDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? orderedTiles = null,Object? visibleTiles = null,Object? orderedRings = null,Object? visibleRings = null,Object? hiddenTitles = null,Object? unsupportedTitles = null,Object? trayTitles = null,Object? activities = null,}) {
  return _then(_self.copyWith(
orderedTiles: null == orderedTiles ? _self.orderedTiles : orderedTiles // ignore: cast_nullable_to_non_nullable
as List<StatTileData>,visibleTiles: null == visibleTiles ? _self.visibleTiles : visibleTiles // ignore: cast_nullable_to_non_nullable
as List<StatTileData>,orderedRings: null == orderedRings ? _self.orderedRings : orderedRings // ignore: cast_nullable_to_non_nullable
as List<RingCardData>,visibleRings: null == visibleRings ? _self.visibleRings : visibleRings // ignore: cast_nullable_to_non_nullable
as List<RingCardData>,hiddenTitles: null == hiddenTitles ? _self.hiddenTitles : hiddenTitles // ignore: cast_nullable_to_non_nullable
as Set<String>,unsupportedTitles: null == unsupportedTitles ? _self.unsupportedTitles : unsupportedTitles // ignore: cast_nullable_to_non_nullable
as Set<String>,trayTitles: null == trayTitles ? _self.trayTitles : trayTitles // ignore: cast_nullable_to_non_nullable
as List<String>,activities: null == activities ? _self.activities : activities // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,
  ));
}

}


/// Adds pattern-matching-related methods to [DashboardDisplay].
extension DashboardDisplayPatterns on DashboardDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DashboardDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DashboardDisplay value)  $default,){
final _that = this;
switch (_that) {
case _DashboardDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DashboardDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _DashboardDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<StatTileData> orderedTiles,  List<StatTileData> visibleTiles,  List<RingCardData> orderedRings,  List<RingCardData> visibleRings,  Set<String> hiddenTitles,  Set<String> unsupportedTitles,  List<String> trayTitles,  List<ExerciseData> activities)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardDisplay() when $default != null:
return $default(_that.orderedTiles,_that.visibleTiles,_that.orderedRings,_that.visibleRings,_that.hiddenTitles,_that.unsupportedTitles,_that.trayTitles,_that.activities);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<StatTileData> orderedTiles,  List<StatTileData> visibleTiles,  List<RingCardData> orderedRings,  List<RingCardData> visibleRings,  Set<String> hiddenTitles,  Set<String> unsupportedTitles,  List<String> trayTitles,  List<ExerciseData> activities)  $default,) {final _that = this;
switch (_that) {
case _DashboardDisplay():
return $default(_that.orderedTiles,_that.visibleTiles,_that.orderedRings,_that.visibleRings,_that.hiddenTitles,_that.unsupportedTitles,_that.trayTitles,_that.activities);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<StatTileData> orderedTiles,  List<StatTileData> visibleTiles,  List<RingCardData> orderedRings,  List<RingCardData> visibleRings,  Set<String> hiddenTitles,  Set<String> unsupportedTitles,  List<String> trayTitles,  List<ExerciseData> activities)?  $default,) {final _that = this;
switch (_that) {
case _DashboardDisplay() when $default != null:
return $default(_that.orderedTiles,_that.visibleTiles,_that.orderedRings,_that.visibleRings,_that.hiddenTitles,_that.unsupportedTitles,_that.trayTitles,_that.activities);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardDisplay implements DashboardDisplay {
  const _DashboardDisplay({required final  List<StatTileData> orderedTiles, required final  List<StatTileData> visibleTiles, required final  List<RingCardData> orderedRings, required final  List<RingCardData> visibleRings, required final  Set<String> hiddenTitles, required final  Set<String> unsupportedTitles, required final  List<String> trayTitles, required final  List<ExerciseData> activities}): _orderedTiles = orderedTiles,_visibleTiles = visibleTiles,_orderedRings = orderedRings,_visibleRings = visibleRings,_hiddenTitles = hiddenTitles,_unsupportedTitles = unsupportedTitles,_trayTitles = trayTitles,_activities = activities;
  

/// Every tile in the user's saved order, hidden ones included — the set the
/// edit grid and the add-tray are computed from.
 final  List<StatTileData> _orderedTiles;
/// Every tile in the user's saved order, hidden ones included — the set the
/// edit grid and the add-tray are computed from.
@override List<StatTileData> get orderedTiles {
  if (_orderedTiles is EqualUnmodifiableListView) return _orderedTiles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_orderedTiles);
}

/// The tiles the carousel actually shows.
 final  List<StatTileData> _visibleTiles;
/// The tiles the carousel actually shows.
@override List<StatTileData> get visibleTiles {
  if (_visibleTiles is EqualUnmodifiableListView) return _visibleTiles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_visibleTiles);
}

/// Both hero rings in the user's saved order, hidden ones included.
 final  List<RingCardData> _orderedRings;
/// Both hero rings in the user's saved order, hidden ones included.
@override List<RingCardData> get orderedRings {
  if (_orderedRings is EqualUnmodifiableListView) return _orderedRings;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_orderedRings);
}

/// The hero rings the top row actually shows.
 final  List<RingCardData> _visibleRings;
/// The hero rings the top row actually shows.
@override List<RingCardData> get visibleRings {
  if (_visibleRings is EqualUnmodifiableListView) return _visibleRings;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_visibleRings);
}

/// The effective hidden set: the saved one, plus (while editing) every
/// unsupported tile the user has never deliberately placed.
 final  Set<String> _hiddenTitles;
/// The effective hidden set: the saved one, plus (while editing) every
/// unsupported tile the user has never deliberately placed.
@override Set<String> get hiddenTitles {
  if (_hiddenTitles is EqualUnmodifiableSetView) return _hiddenTitles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_hiddenTitles);
}

/// The titles materialised only because the device does not support the
/// metric (see [DashboardSummary.unsupportedTitles]). Empty outside edit
/// mode.
 final  Set<String> _unsupportedTitles;
/// The titles materialised only because the device does not support the
/// metric (see [DashboardSummary.unsupportedTitles]). Empty outside edit
/// mode.
@override Set<String> get unsupportedTitles {
  if (_unsupportedTitles is EqualUnmodifiableSetView) return _unsupportedTitles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_unsupportedTitles);
}

/// The add-tray: the removed rings and tiles, in layout order.
 final  List<String> _trayTitles;
/// The add-tray: the removed rings and tiles, in layout order.
@override List<String> get trayTitles {
  if (_trayTitles is EqualUnmodifiableListView) return _trayTitles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_trayTitles);
}

/// Today's activities, with the single-workout fallback already folded in.
 final  List<ExerciseData> _activities;
/// Today's activities, with the single-workout fallback already folded in.
@override List<ExerciseData> get activities {
  if (_activities is EqualUnmodifiableListView) return _activities;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_activities);
}


/// Create a copy of DashboardDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardDisplayCopyWith<_DashboardDisplay> get copyWith => __$DashboardDisplayCopyWithImpl<_DashboardDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardDisplay&&const DeepCollectionEquality().equals(other._orderedTiles, _orderedTiles)&&const DeepCollectionEquality().equals(other._visibleTiles, _visibleTiles)&&const DeepCollectionEquality().equals(other._orderedRings, _orderedRings)&&const DeepCollectionEquality().equals(other._visibleRings, _visibleRings)&&const DeepCollectionEquality().equals(other._hiddenTitles, _hiddenTitles)&&const DeepCollectionEquality().equals(other._unsupportedTitles, _unsupportedTitles)&&const DeepCollectionEquality().equals(other._trayTitles, _trayTitles)&&const DeepCollectionEquality().equals(other._activities, _activities));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_orderedTiles),const DeepCollectionEquality().hash(_visibleTiles),const DeepCollectionEquality().hash(_orderedRings),const DeepCollectionEquality().hash(_visibleRings),const DeepCollectionEquality().hash(_hiddenTitles),const DeepCollectionEquality().hash(_unsupportedTitles),const DeepCollectionEquality().hash(_trayTitles),const DeepCollectionEquality().hash(_activities));

@override
String toString() {
  return 'DashboardDisplay(orderedTiles: $orderedTiles, visibleTiles: $visibleTiles, orderedRings: $orderedRings, visibleRings: $visibleRings, hiddenTitles: $hiddenTitles, unsupportedTitles: $unsupportedTitles, trayTitles: $trayTitles, activities: $activities)';
}


}

/// @nodoc
abstract mixin class _$DashboardDisplayCopyWith<$Res> implements $DashboardDisplayCopyWith<$Res> {
  factory _$DashboardDisplayCopyWith(_DashboardDisplay value, $Res Function(_DashboardDisplay) _then) = __$DashboardDisplayCopyWithImpl;
@override @useResult
$Res call({
 List<StatTileData> orderedTiles, List<StatTileData> visibleTiles, List<RingCardData> orderedRings, List<RingCardData> visibleRings, Set<String> hiddenTitles, Set<String> unsupportedTitles, List<String> trayTitles, List<ExerciseData> activities
});




}
/// @nodoc
class __$DashboardDisplayCopyWithImpl<$Res>
    implements _$DashboardDisplayCopyWith<$Res> {
  __$DashboardDisplayCopyWithImpl(this._self, this._then);

  final _DashboardDisplay _self;
  final $Res Function(_DashboardDisplay) _then;

/// Create a copy of DashboardDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? orderedTiles = null,Object? visibleTiles = null,Object? orderedRings = null,Object? visibleRings = null,Object? hiddenTitles = null,Object? unsupportedTitles = null,Object? trayTitles = null,Object? activities = null,}) {
  return _then(_DashboardDisplay(
orderedTiles: null == orderedTiles ? _self._orderedTiles : orderedTiles // ignore: cast_nullable_to_non_nullable
as List<StatTileData>,visibleTiles: null == visibleTiles ? _self._visibleTiles : visibleTiles // ignore: cast_nullable_to_non_nullable
as List<StatTileData>,orderedRings: null == orderedRings ? _self._orderedRings : orderedRings // ignore: cast_nullable_to_non_nullable
as List<RingCardData>,visibleRings: null == visibleRings ? _self._visibleRings : visibleRings // ignore: cast_nullable_to_non_nullable
as List<RingCardData>,hiddenTitles: null == hiddenTitles ? _self._hiddenTitles : hiddenTitles // ignore: cast_nullable_to_non_nullable
as Set<String>,unsupportedTitles: null == unsupportedTitles ? _self._unsupportedTitles : unsupportedTitles // ignore: cast_nullable_to_non_nullable
as Set<String>,trayTitles: null == trayTitles ? _self._trayTitles : trayTitles // ignore: cast_nullable_to_non_nullable
as List<String>,activities: null == activities ? _self._activities : activities // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,
  ));
}


}

// dart format on
