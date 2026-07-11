// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'dashboard_query.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DashboardQuery {

 LocalDate get date; SleepRangeMode get sleepRangeMode; ActivityWeekMode get activityWeekMode; Set<DashboardMetric> get visibleMetrics; RefreshMode get refreshMode; bool get includeHistoricalBaselines; bool get includeWeeklyTrainingSignals;
/// Create a copy of DashboardQuery
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardQueryCopyWith<DashboardQuery> get copyWith => _$DashboardQueryCopyWithImpl<DashboardQuery>(this as DashboardQuery, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardQuery&&(identical(other.date, date) || other.date == date)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.activityWeekMode, activityWeekMode) || other.activityWeekMode == activityWeekMode)&&const DeepCollectionEquality().equals(other.visibleMetrics, visibleMetrics)&&(identical(other.refreshMode, refreshMode) || other.refreshMode == refreshMode)&&(identical(other.includeHistoricalBaselines, includeHistoricalBaselines) || other.includeHistoricalBaselines == includeHistoricalBaselines)&&(identical(other.includeWeeklyTrainingSignals, includeWeeklyTrainingSignals) || other.includeWeeklyTrainingSignals == includeWeeklyTrainingSignals));
}


@override
int get hashCode => Object.hash(runtimeType,date,sleepRangeMode,activityWeekMode,const DeepCollectionEquality().hash(visibleMetrics),refreshMode,includeHistoricalBaselines,includeWeeklyTrainingSignals);

@override
String toString() {
  return 'DashboardQuery(date: $date, sleepRangeMode: $sleepRangeMode, activityWeekMode: $activityWeekMode, visibleMetrics: $visibleMetrics, refreshMode: $refreshMode, includeHistoricalBaselines: $includeHistoricalBaselines, includeWeeklyTrainingSignals: $includeWeeklyTrainingSignals)';
}


}

/// @nodoc
abstract mixin class $DashboardQueryCopyWith<$Res>  {
  factory $DashboardQueryCopyWith(DashboardQuery value, $Res Function(DashboardQuery) _then) = _$DashboardQueryCopyWithImpl;
@useResult
$Res call({
 LocalDate date, SleepRangeMode sleepRangeMode, ActivityWeekMode activityWeekMode, Set<DashboardMetric> visibleMetrics, RefreshMode refreshMode, bool includeHistoricalBaselines, bool includeWeeklyTrainingSignals
});




}
/// @nodoc
class _$DashboardQueryCopyWithImpl<$Res>
    implements $DashboardQueryCopyWith<$Res> {
  _$DashboardQueryCopyWithImpl(this._self, this._then);

  final DashboardQuery _self;
  final $Res Function(DashboardQuery) _then;

/// Create a copy of DashboardQuery
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? sleepRangeMode = null,Object? activityWeekMode = null,Object? visibleMetrics = null,Object? refreshMode = null,Object? includeHistoricalBaselines = null,Object? includeWeeklyTrainingSignals = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,activityWeekMode: null == activityWeekMode ? _self.activityWeekMode : activityWeekMode // ignore: cast_nullable_to_non_nullable
as ActivityWeekMode,visibleMetrics: null == visibleMetrics ? _self.visibleMetrics : visibleMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,refreshMode: null == refreshMode ? _self.refreshMode : refreshMode // ignore: cast_nullable_to_non_nullable
as RefreshMode,includeHistoricalBaselines: null == includeHistoricalBaselines ? _self.includeHistoricalBaselines : includeHistoricalBaselines // ignore: cast_nullable_to_non_nullable
as bool,includeWeeklyTrainingSignals: null == includeWeeklyTrainingSignals ? _self.includeWeeklyTrainingSignals : includeWeeklyTrainingSignals // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [DashboardQuery].
extension DashboardQueryPatterns on DashboardQuery {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _DashboardQuery value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardQuery() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _DashboardQuery value)  build,}){
final _that = this;
switch (_that) {
case _DashboardQuery():
return build(_that);case _:
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _DashboardQuery value)?  build,}){
final _that = this;
switch (_that) {
case _DashboardQuery() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( LocalDate date,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  Set<DashboardMetric> visibleMetrics,  RefreshMode refreshMode,  bool includeHistoricalBaselines,  bool includeWeeklyTrainingSignals)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardQuery() when build != null:
return build(_that.date,_that.sleepRangeMode,_that.activityWeekMode,_that.visibleMetrics,_that.refreshMode,_that.includeHistoricalBaselines,_that.includeWeeklyTrainingSignals);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( LocalDate date,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  Set<DashboardMetric> visibleMetrics,  RefreshMode refreshMode,  bool includeHistoricalBaselines,  bool includeWeeklyTrainingSignals)  build,}) {final _that = this;
switch (_that) {
case _DashboardQuery():
return build(_that.date,_that.sleepRangeMode,_that.activityWeekMode,_that.visibleMetrics,_that.refreshMode,_that.includeHistoricalBaselines,_that.includeWeeklyTrainingSignals);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( LocalDate date,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  Set<DashboardMetric> visibleMetrics,  RefreshMode refreshMode,  bool includeHistoricalBaselines,  bool includeWeeklyTrainingSignals)?  build,}) {final _that = this;
switch (_that) {
case _DashboardQuery() when build != null:
return build(_that.date,_that.sleepRangeMode,_that.activityWeekMode,_that.visibleMetrics,_that.refreshMode,_that.includeHistoricalBaselines,_that.includeWeeklyTrainingSignals);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardQuery implements DashboardQuery {
  const _DashboardQuery({required this.date, required this.sleepRangeMode, required this.activityWeekMode, required final  Set<DashboardMetric> visibleMetrics, required this.refreshMode, required this.includeHistoricalBaselines, required this.includeWeeklyTrainingSignals}): _visibleMetrics = visibleMetrics;
  

@override final  LocalDate date;
@override final  SleepRangeMode sleepRangeMode;
@override final  ActivityWeekMode activityWeekMode;
 final  Set<DashboardMetric> _visibleMetrics;
@override Set<DashboardMetric> get visibleMetrics {
  if (_visibleMetrics is EqualUnmodifiableSetView) return _visibleMetrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_visibleMetrics);
}

@override final  RefreshMode refreshMode;
@override final  bool includeHistoricalBaselines;
@override final  bool includeWeeklyTrainingSignals;

/// Create a copy of DashboardQuery
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardQueryCopyWith<_DashboardQuery> get copyWith => __$DashboardQueryCopyWithImpl<_DashboardQuery>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardQuery&&(identical(other.date, date) || other.date == date)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.activityWeekMode, activityWeekMode) || other.activityWeekMode == activityWeekMode)&&const DeepCollectionEquality().equals(other._visibleMetrics, _visibleMetrics)&&(identical(other.refreshMode, refreshMode) || other.refreshMode == refreshMode)&&(identical(other.includeHistoricalBaselines, includeHistoricalBaselines) || other.includeHistoricalBaselines == includeHistoricalBaselines)&&(identical(other.includeWeeklyTrainingSignals, includeWeeklyTrainingSignals) || other.includeWeeklyTrainingSignals == includeWeeklyTrainingSignals));
}


@override
int get hashCode => Object.hash(runtimeType,date,sleepRangeMode,activityWeekMode,const DeepCollectionEquality().hash(_visibleMetrics),refreshMode,includeHistoricalBaselines,includeWeeklyTrainingSignals);

@override
String toString() {
  return 'DashboardQuery.build(date: $date, sleepRangeMode: $sleepRangeMode, activityWeekMode: $activityWeekMode, visibleMetrics: $visibleMetrics, refreshMode: $refreshMode, includeHistoricalBaselines: $includeHistoricalBaselines, includeWeeklyTrainingSignals: $includeWeeklyTrainingSignals)';
}


}

/// @nodoc
abstract mixin class _$DashboardQueryCopyWith<$Res> implements $DashboardQueryCopyWith<$Res> {
  factory _$DashboardQueryCopyWith(_DashboardQuery value, $Res Function(_DashboardQuery) _then) = __$DashboardQueryCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, SleepRangeMode sleepRangeMode, ActivityWeekMode activityWeekMode, Set<DashboardMetric> visibleMetrics, RefreshMode refreshMode, bool includeHistoricalBaselines, bool includeWeeklyTrainingSignals
});




}
/// @nodoc
class __$DashboardQueryCopyWithImpl<$Res>
    implements _$DashboardQueryCopyWith<$Res> {
  __$DashboardQueryCopyWithImpl(this._self, this._then);

  final _DashboardQuery _self;
  final $Res Function(_DashboardQuery) _then;

/// Create a copy of DashboardQuery
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? sleepRangeMode = null,Object? activityWeekMode = null,Object? visibleMetrics = null,Object? refreshMode = null,Object? includeHistoricalBaselines = null,Object? includeWeeklyTrainingSignals = null,}) {
  return _then(_DashboardQuery(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,activityWeekMode: null == activityWeekMode ? _self.activityWeekMode : activityWeekMode // ignore: cast_nullable_to_non_nullable
as ActivityWeekMode,visibleMetrics: null == visibleMetrics ? _self._visibleMetrics : visibleMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,refreshMode: null == refreshMode ? _self.refreshMode : refreshMode // ignore: cast_nullable_to_non_nullable
as RefreshMode,includeHistoricalBaselines: null == includeHistoricalBaselines ? _self.includeHistoricalBaselines : includeHistoricalBaselines // ignore: cast_nullable_to_non_nullable
as bool,includeWeeklyTrainingSignals: null == includeWeeklyTrainingSignals ? _self.includeWeeklyTrainingSignals : includeWeeklyTrainingSignals // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
