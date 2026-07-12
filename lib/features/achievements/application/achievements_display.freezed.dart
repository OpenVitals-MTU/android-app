// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'achievements_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$AchievementsDisplay {

 List<AchievementProgress> get badges; AchievementStats get stats; Map<AchievementCategory, List<AchievementProgress>> get badgesByCategory; int get unlockedCount; int get totalCount; double get completionRatio; bool get hasActivityHistory; bool get hasFloorHistory;
/// Create a copy of AchievementsDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$AchievementsDisplayCopyWith<AchievementsDisplay> get copyWith => _$AchievementsDisplayCopyWithImpl<AchievementsDisplay>(this as AchievementsDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is AchievementsDisplay&&const DeepCollectionEquality().equals(other.badges, badges)&&(identical(other.stats, stats) || other.stats == stats)&&const DeepCollectionEquality().equals(other.badgesByCategory, badgesByCategory)&&(identical(other.unlockedCount, unlockedCount) || other.unlockedCount == unlockedCount)&&(identical(other.totalCount, totalCount) || other.totalCount == totalCount)&&(identical(other.completionRatio, completionRatio) || other.completionRatio == completionRatio)&&(identical(other.hasActivityHistory, hasActivityHistory) || other.hasActivityHistory == hasActivityHistory)&&(identical(other.hasFloorHistory, hasFloorHistory) || other.hasFloorHistory == hasFloorHistory));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(badges),stats,const DeepCollectionEquality().hash(badgesByCategory),unlockedCount,totalCount,completionRatio,hasActivityHistory,hasFloorHistory);

@override
String toString() {
  return 'AchievementsDisplay(badges: $badges, stats: $stats, badgesByCategory: $badgesByCategory, unlockedCount: $unlockedCount, totalCount: $totalCount, completionRatio: $completionRatio, hasActivityHistory: $hasActivityHistory, hasFloorHistory: $hasFloorHistory)';
}


}

/// @nodoc
abstract mixin class $AchievementsDisplayCopyWith<$Res>  {
  factory $AchievementsDisplayCopyWith(AchievementsDisplay value, $Res Function(AchievementsDisplay) _then) = _$AchievementsDisplayCopyWithImpl;
@useResult
$Res call({
 List<AchievementProgress> badges, AchievementStats stats, Map<AchievementCategory, List<AchievementProgress>> badgesByCategory, int unlockedCount, int totalCount, double completionRatio, bool hasActivityHistory, bool hasFloorHistory
});




}
/// @nodoc
class _$AchievementsDisplayCopyWithImpl<$Res>
    implements $AchievementsDisplayCopyWith<$Res> {
  _$AchievementsDisplayCopyWithImpl(this._self, this._then);

  final AchievementsDisplay _self;
  final $Res Function(AchievementsDisplay) _then;

/// Create a copy of AchievementsDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? badges = null,Object? stats = null,Object? badgesByCategory = null,Object? unlockedCount = null,Object? totalCount = null,Object? completionRatio = null,Object? hasActivityHistory = null,Object? hasFloorHistory = null,}) {
  return _then(_self.copyWith(
badges: null == badges ? _self.badges : badges // ignore: cast_nullable_to_non_nullable
as List<AchievementProgress>,stats: null == stats ? _self.stats : stats // ignore: cast_nullable_to_non_nullable
as AchievementStats,badgesByCategory: null == badgesByCategory ? _self.badgesByCategory : badgesByCategory // ignore: cast_nullable_to_non_nullable
as Map<AchievementCategory, List<AchievementProgress>>,unlockedCount: null == unlockedCount ? _self.unlockedCount : unlockedCount // ignore: cast_nullable_to_non_nullable
as int,totalCount: null == totalCount ? _self.totalCount : totalCount // ignore: cast_nullable_to_non_nullable
as int,completionRatio: null == completionRatio ? _self.completionRatio : completionRatio // ignore: cast_nullable_to_non_nullable
as double,hasActivityHistory: null == hasActivityHistory ? _self.hasActivityHistory : hasActivityHistory // ignore: cast_nullable_to_non_nullable
as bool,hasFloorHistory: null == hasFloorHistory ? _self.hasFloorHistory : hasFloorHistory // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [AchievementsDisplay].
extension AchievementsDisplayPatterns on AchievementsDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _AchievementsDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _AchievementsDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _AchievementsDisplay value)  $default,){
final _that = this;
switch (_that) {
case _AchievementsDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _AchievementsDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _AchievementsDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<AchievementProgress> badges,  AchievementStats stats,  Map<AchievementCategory, List<AchievementProgress>> badgesByCategory,  int unlockedCount,  int totalCount,  double completionRatio,  bool hasActivityHistory,  bool hasFloorHistory)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _AchievementsDisplay() when $default != null:
return $default(_that.badges,_that.stats,_that.badgesByCategory,_that.unlockedCount,_that.totalCount,_that.completionRatio,_that.hasActivityHistory,_that.hasFloorHistory);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<AchievementProgress> badges,  AchievementStats stats,  Map<AchievementCategory, List<AchievementProgress>> badgesByCategory,  int unlockedCount,  int totalCount,  double completionRatio,  bool hasActivityHistory,  bool hasFloorHistory)  $default,) {final _that = this;
switch (_that) {
case _AchievementsDisplay():
return $default(_that.badges,_that.stats,_that.badgesByCategory,_that.unlockedCount,_that.totalCount,_that.completionRatio,_that.hasActivityHistory,_that.hasFloorHistory);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<AchievementProgress> badges,  AchievementStats stats,  Map<AchievementCategory, List<AchievementProgress>> badgesByCategory,  int unlockedCount,  int totalCount,  double completionRatio,  bool hasActivityHistory,  bool hasFloorHistory)?  $default,) {final _that = this;
switch (_that) {
case _AchievementsDisplay() when $default != null:
return $default(_that.badges,_that.stats,_that.badgesByCategory,_that.unlockedCount,_that.totalCount,_that.completionRatio,_that.hasActivityHistory,_that.hasFloorHistory);case _:
  return null;

}
}

}

/// @nodoc


class _AchievementsDisplay extends AchievementsDisplay {
  const _AchievementsDisplay({required final  List<AchievementProgress> badges, required this.stats, required final  Map<AchievementCategory, List<AchievementProgress>> badgesByCategory, required this.unlockedCount, required this.totalCount, required this.completionRatio, required this.hasActivityHistory, required this.hasFloorHistory}): _badges = badges,_badgesByCategory = badgesByCategory,super._();
  

 final  List<AchievementProgress> _badges;
@override List<AchievementProgress> get badges {
  if (_badges is EqualUnmodifiableListView) return _badges;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_badges);
}

@override final  AchievementStats stats;
 final  Map<AchievementCategory, List<AchievementProgress>> _badgesByCategory;
@override Map<AchievementCategory, List<AchievementProgress>> get badgesByCategory {
  if (_badgesByCategory is EqualUnmodifiableMapView) return _badgesByCategory;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_badgesByCategory);
}

@override final  int unlockedCount;
@override final  int totalCount;
@override final  double completionRatio;
@override final  bool hasActivityHistory;
@override final  bool hasFloorHistory;

/// Create a copy of AchievementsDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$AchievementsDisplayCopyWith<_AchievementsDisplay> get copyWith => __$AchievementsDisplayCopyWithImpl<_AchievementsDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _AchievementsDisplay&&const DeepCollectionEquality().equals(other._badges, _badges)&&(identical(other.stats, stats) || other.stats == stats)&&const DeepCollectionEquality().equals(other._badgesByCategory, _badgesByCategory)&&(identical(other.unlockedCount, unlockedCount) || other.unlockedCount == unlockedCount)&&(identical(other.totalCount, totalCount) || other.totalCount == totalCount)&&(identical(other.completionRatio, completionRatio) || other.completionRatio == completionRatio)&&(identical(other.hasActivityHistory, hasActivityHistory) || other.hasActivityHistory == hasActivityHistory)&&(identical(other.hasFloorHistory, hasFloorHistory) || other.hasFloorHistory == hasFloorHistory));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_badges),stats,const DeepCollectionEquality().hash(_badgesByCategory),unlockedCount,totalCount,completionRatio,hasActivityHistory,hasFloorHistory);

@override
String toString() {
  return 'AchievementsDisplay(badges: $badges, stats: $stats, badgesByCategory: $badgesByCategory, unlockedCount: $unlockedCount, totalCount: $totalCount, completionRatio: $completionRatio, hasActivityHistory: $hasActivityHistory, hasFloorHistory: $hasFloorHistory)';
}


}

/// @nodoc
abstract mixin class _$AchievementsDisplayCopyWith<$Res> implements $AchievementsDisplayCopyWith<$Res> {
  factory _$AchievementsDisplayCopyWith(_AchievementsDisplay value, $Res Function(_AchievementsDisplay) _then) = __$AchievementsDisplayCopyWithImpl;
@override @useResult
$Res call({
 List<AchievementProgress> badges, AchievementStats stats, Map<AchievementCategory, List<AchievementProgress>> badgesByCategory, int unlockedCount, int totalCount, double completionRatio, bool hasActivityHistory, bool hasFloorHistory
});




}
/// @nodoc
class __$AchievementsDisplayCopyWithImpl<$Res>
    implements _$AchievementsDisplayCopyWith<$Res> {
  __$AchievementsDisplayCopyWithImpl(this._self, this._then);

  final _AchievementsDisplay _self;
  final $Res Function(_AchievementsDisplay) _then;

/// Create a copy of AchievementsDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? badges = null,Object? stats = null,Object? badgesByCategory = null,Object? unlockedCount = null,Object? totalCount = null,Object? completionRatio = null,Object? hasActivityHistory = null,Object? hasFloorHistory = null,}) {
  return _then(_AchievementsDisplay(
badges: null == badges ? _self._badges : badges // ignore: cast_nullable_to_non_nullable
as List<AchievementProgress>,stats: null == stats ? _self.stats : stats // ignore: cast_nullable_to_non_nullable
as AchievementStats,badgesByCategory: null == badgesByCategory ? _self._badgesByCategory : badgesByCategory // ignore: cast_nullable_to_non_nullable
as Map<AchievementCategory, List<AchievementProgress>>,unlockedCount: null == unlockedCount ? _self.unlockedCount : unlockedCount // ignore: cast_nullable_to_non_nullable
as int,totalCount: null == totalCount ? _self.totalCount : totalCount // ignore: cast_nullable_to_non_nullable
as int,completionRatio: null == completionRatio ? _self.completionRatio : completionRatio // ignore: cast_nullable_to_non_nullable
as double,hasActivityHistory: null == hasActivityHistory ? _self.hasActivityHistory : hasActivityHistory // ignore: cast_nullable_to_non_nullable
as bool,hasFloorHistory: null == hasFloorHistory ? _self.hasFloorHistory : hasFloorHistory // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
