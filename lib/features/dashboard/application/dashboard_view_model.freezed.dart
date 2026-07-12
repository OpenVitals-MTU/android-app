// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'dashboard_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DashboardState {

 LocalDate get selectedDate; DashboardData? get data; bool get isLoading; bool get isRefreshing; ScreenError? get error; SleepRangeMode get sleepRangeMode; ActivityWeekMode get activityWeekMode; bool get showOpenVitalsCalculatedCalories; HealthConnectAvailability get healthConnectAvailability; bool get minimumPermissionsGranted; Set<DashboardMetric> get loadingMetrics; Set<String> get unacknowledgedPermissions; bool get editing; List<String> get tileOrder; List<String> get ringOrder; Set<String> get hiddenTiles;
/// Create a copy of DashboardState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardStateCopyWith<DashboardState> get copyWith => _$DashboardStateCopyWithImpl<DashboardState>(this as DashboardState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.data, data) || other.data == data)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.isRefreshing, isRefreshing) || other.isRefreshing == isRefreshing)&&(identical(other.error, error) || other.error == error)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.activityWeekMode, activityWeekMode) || other.activityWeekMode == activityWeekMode)&&(identical(other.showOpenVitalsCalculatedCalories, showOpenVitalsCalculatedCalories) || other.showOpenVitalsCalculatedCalories == showOpenVitalsCalculatedCalories)&&(identical(other.healthConnectAvailability, healthConnectAvailability) || other.healthConnectAvailability == healthConnectAvailability)&&(identical(other.minimumPermissionsGranted, minimumPermissionsGranted) || other.minimumPermissionsGranted == minimumPermissionsGranted)&&const DeepCollectionEquality().equals(other.loadingMetrics, loadingMetrics)&&const DeepCollectionEquality().equals(other.unacknowledgedPermissions, unacknowledgedPermissions)&&(identical(other.editing, editing) || other.editing == editing)&&const DeepCollectionEquality().equals(other.tileOrder, tileOrder)&&const DeepCollectionEquality().equals(other.ringOrder, ringOrder)&&const DeepCollectionEquality().equals(other.hiddenTiles, hiddenTiles));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,data,isLoading,isRefreshing,error,sleepRangeMode,activityWeekMode,showOpenVitalsCalculatedCalories,healthConnectAvailability,minimumPermissionsGranted,const DeepCollectionEquality().hash(loadingMetrics),const DeepCollectionEquality().hash(unacknowledgedPermissions),editing,const DeepCollectionEquality().hash(tileOrder),const DeepCollectionEquality().hash(ringOrder),const DeepCollectionEquality().hash(hiddenTiles));

@override
String toString() {
  return 'DashboardState(selectedDate: $selectedDate, data: $data, isLoading: $isLoading, isRefreshing: $isRefreshing, error: $error, sleepRangeMode: $sleepRangeMode, activityWeekMode: $activityWeekMode, showOpenVitalsCalculatedCalories: $showOpenVitalsCalculatedCalories, healthConnectAvailability: $healthConnectAvailability, minimumPermissionsGranted: $minimumPermissionsGranted, loadingMetrics: $loadingMetrics, unacknowledgedPermissions: $unacknowledgedPermissions, editing: $editing, tileOrder: $tileOrder, ringOrder: $ringOrder, hiddenTiles: $hiddenTiles)';
}


}

/// @nodoc
abstract mixin class $DashboardStateCopyWith<$Res>  {
  factory $DashboardStateCopyWith(DashboardState value, $Res Function(DashboardState) _then) = _$DashboardStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, DashboardData? data, bool isLoading, bool isRefreshing, ScreenError? error, SleepRangeMode sleepRangeMode, ActivityWeekMode activityWeekMode, bool showOpenVitalsCalculatedCalories, HealthConnectAvailability healthConnectAvailability, bool minimumPermissionsGranted, Set<DashboardMetric> loadingMetrics, Set<String> unacknowledgedPermissions, bool editing, List<String> tileOrder, List<String> ringOrder, Set<String> hiddenTiles
});


$DashboardDataCopyWith<$Res>? get data;

}
/// @nodoc
class _$DashboardStateCopyWithImpl<$Res>
    implements $DashboardStateCopyWith<$Res> {
  _$DashboardStateCopyWithImpl(this._self, this._then);

  final DashboardState _self;
  final $Res Function(DashboardState) _then;

/// Create a copy of DashboardState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? data = freezed,Object? isLoading = null,Object? isRefreshing = null,Object? error = freezed,Object? sleepRangeMode = null,Object? activityWeekMode = null,Object? showOpenVitalsCalculatedCalories = null,Object? healthConnectAvailability = null,Object? minimumPermissionsGranted = null,Object? loadingMetrics = null,Object? unacknowledgedPermissions = null,Object? editing = null,Object? tileOrder = null,Object? ringOrder = null,Object? hiddenTiles = null,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,isRefreshing: null == isRefreshing ? _self.isRefreshing : isRefreshing // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,activityWeekMode: null == activityWeekMode ? _self.activityWeekMode : activityWeekMode // ignore: cast_nullable_to_non_nullable
as ActivityWeekMode,showOpenVitalsCalculatedCalories: null == showOpenVitalsCalculatedCalories ? _self.showOpenVitalsCalculatedCalories : showOpenVitalsCalculatedCalories // ignore: cast_nullable_to_non_nullable
as bool,healthConnectAvailability: null == healthConnectAvailability ? _self.healthConnectAvailability : healthConnectAvailability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability,minimumPermissionsGranted: null == minimumPermissionsGranted ? _self.minimumPermissionsGranted : minimumPermissionsGranted // ignore: cast_nullable_to_non_nullable
as bool,loadingMetrics: null == loadingMetrics ? _self.loadingMetrics : loadingMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,unacknowledgedPermissions: null == unacknowledgedPermissions ? _self.unacknowledgedPermissions : unacknowledgedPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,editing: null == editing ? _self.editing : editing // ignore: cast_nullable_to_non_nullable
as bool,tileOrder: null == tileOrder ? _self.tileOrder : tileOrder // ignore: cast_nullable_to_non_nullable
as List<String>,ringOrder: null == ringOrder ? _self.ringOrder : ringOrder // ignore: cast_nullable_to_non_nullable
as List<String>,hiddenTiles: null == hiddenTiles ? _self.hiddenTiles : hiddenTiles // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}
/// Create a copy of DashboardState
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
}
}


/// Adds pattern-matching-related methods to [DashboardState].
extension DashboardStatePatterns on DashboardState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DashboardState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DashboardState value)  $default,){
final _that = this;
switch (_that) {
case _DashboardState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DashboardState value)?  $default,){
final _that = this;
switch (_that) {
case _DashboardState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  DashboardData? data,  bool isLoading,  bool isRefreshing,  ScreenError? error,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  bool showOpenVitalsCalculatedCalories,  HealthConnectAvailability healthConnectAvailability,  bool minimumPermissionsGranted,  Set<DashboardMetric> loadingMetrics,  Set<String> unacknowledgedPermissions,  bool editing,  List<String> tileOrder,  List<String> ringOrder,  Set<String> hiddenTiles)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardState() when $default != null:
return $default(_that.selectedDate,_that.data,_that.isLoading,_that.isRefreshing,_that.error,_that.sleepRangeMode,_that.activityWeekMode,_that.showOpenVitalsCalculatedCalories,_that.healthConnectAvailability,_that.minimumPermissionsGranted,_that.loadingMetrics,_that.unacknowledgedPermissions,_that.editing,_that.tileOrder,_that.ringOrder,_that.hiddenTiles);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  DashboardData? data,  bool isLoading,  bool isRefreshing,  ScreenError? error,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  bool showOpenVitalsCalculatedCalories,  HealthConnectAvailability healthConnectAvailability,  bool minimumPermissionsGranted,  Set<DashboardMetric> loadingMetrics,  Set<String> unacknowledgedPermissions,  bool editing,  List<String> tileOrder,  List<String> ringOrder,  Set<String> hiddenTiles)  $default,) {final _that = this;
switch (_that) {
case _DashboardState():
return $default(_that.selectedDate,_that.data,_that.isLoading,_that.isRefreshing,_that.error,_that.sleepRangeMode,_that.activityWeekMode,_that.showOpenVitalsCalculatedCalories,_that.healthConnectAvailability,_that.minimumPermissionsGranted,_that.loadingMetrics,_that.unacknowledgedPermissions,_that.editing,_that.tileOrder,_that.ringOrder,_that.hiddenTiles);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  DashboardData? data,  bool isLoading,  bool isRefreshing,  ScreenError? error,  SleepRangeMode sleepRangeMode,  ActivityWeekMode activityWeekMode,  bool showOpenVitalsCalculatedCalories,  HealthConnectAvailability healthConnectAvailability,  bool minimumPermissionsGranted,  Set<DashboardMetric> loadingMetrics,  Set<String> unacknowledgedPermissions,  bool editing,  List<String> tileOrder,  List<String> ringOrder,  Set<String> hiddenTiles)?  $default,) {final _that = this;
switch (_that) {
case _DashboardState() when $default != null:
return $default(_that.selectedDate,_that.data,_that.isLoading,_that.isRefreshing,_that.error,_that.sleepRangeMode,_that.activityWeekMode,_that.showOpenVitalsCalculatedCalories,_that.healthConnectAvailability,_that.minimumPermissionsGranted,_that.loadingMetrics,_that.unacknowledgedPermissions,_that.editing,_that.tileOrder,_that.ringOrder,_that.hiddenTiles);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardState extends DashboardState {
  const _DashboardState({required this.selectedDate, this.data, this.isLoading = true, this.isRefreshing = false, this.error, this.sleepRangeMode = SleepRangeMode.evening18h, this.activityWeekMode = ActivityWeekMode.mondayToSunday, this.showOpenVitalsCalculatedCalories = false, this.healthConnectAvailability = HealthConnectAvailability.available, this.minimumPermissionsGranted = true, final  Set<DashboardMetric> loadingMetrics = const <DashboardMetric>{}, final  Set<String> unacknowledgedPermissions = const <String>{}, this.editing = false, final  List<String> tileOrder = const <String>[], final  List<String> ringOrder = const <String>[], final  Set<String> hiddenTiles = const <String>{}}): _loadingMetrics = loadingMetrics,_unacknowledgedPermissions = unacknowledgedPermissions,_tileOrder = tileOrder,_ringOrder = ringOrder,_hiddenTiles = hiddenTiles,super._();
  

@override final  LocalDate selectedDate;
@override final  DashboardData? data;
@override@JsonKey() final  bool isLoading;
@override@JsonKey() final  bool isRefreshing;
@override final  ScreenError? error;
@override@JsonKey() final  SleepRangeMode sleepRangeMode;
@override@JsonKey() final  ActivityWeekMode activityWeekMode;
@override@JsonKey() final  bool showOpenVitalsCalculatedCalories;
@override@JsonKey() final  HealthConnectAvailability healthConnectAvailability;
@override@JsonKey() final  bool minimumPermissionsGranted;
 final  Set<DashboardMetric> _loadingMetrics;
@override@JsonKey() Set<DashboardMetric> get loadingMetrics {
  if (_loadingMetrics is EqualUnmodifiableSetView) return _loadingMetrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_loadingMetrics);
}

 final  Set<String> _unacknowledgedPermissions;
@override@JsonKey() Set<String> get unacknowledgedPermissions {
  if (_unacknowledgedPermissions is EqualUnmodifiableSetView) return _unacknowledgedPermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_unacknowledgedPermissions);
}

@override@JsonKey() final  bool editing;
 final  List<String> _tileOrder;
@override@JsonKey() List<String> get tileOrder {
  if (_tileOrder is EqualUnmodifiableListView) return _tileOrder;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_tileOrder);
}

 final  List<String> _ringOrder;
@override@JsonKey() List<String> get ringOrder {
  if (_ringOrder is EqualUnmodifiableListView) return _ringOrder;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_ringOrder);
}

 final  Set<String> _hiddenTiles;
@override@JsonKey() Set<String> get hiddenTiles {
  if (_hiddenTiles is EqualUnmodifiableSetView) return _hiddenTiles;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_hiddenTiles);
}


/// Create a copy of DashboardState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardStateCopyWith<_DashboardState> get copyWith => __$DashboardStateCopyWithImpl<_DashboardState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.data, data) || other.data == data)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.isRefreshing, isRefreshing) || other.isRefreshing == isRefreshing)&&(identical(other.error, error) || other.error == error)&&(identical(other.sleepRangeMode, sleepRangeMode) || other.sleepRangeMode == sleepRangeMode)&&(identical(other.activityWeekMode, activityWeekMode) || other.activityWeekMode == activityWeekMode)&&(identical(other.showOpenVitalsCalculatedCalories, showOpenVitalsCalculatedCalories) || other.showOpenVitalsCalculatedCalories == showOpenVitalsCalculatedCalories)&&(identical(other.healthConnectAvailability, healthConnectAvailability) || other.healthConnectAvailability == healthConnectAvailability)&&(identical(other.minimumPermissionsGranted, minimumPermissionsGranted) || other.minimumPermissionsGranted == minimumPermissionsGranted)&&const DeepCollectionEquality().equals(other._loadingMetrics, _loadingMetrics)&&const DeepCollectionEquality().equals(other._unacknowledgedPermissions, _unacknowledgedPermissions)&&(identical(other.editing, editing) || other.editing == editing)&&const DeepCollectionEquality().equals(other._tileOrder, _tileOrder)&&const DeepCollectionEquality().equals(other._ringOrder, _ringOrder)&&const DeepCollectionEquality().equals(other._hiddenTiles, _hiddenTiles));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,data,isLoading,isRefreshing,error,sleepRangeMode,activityWeekMode,showOpenVitalsCalculatedCalories,healthConnectAvailability,minimumPermissionsGranted,const DeepCollectionEquality().hash(_loadingMetrics),const DeepCollectionEquality().hash(_unacknowledgedPermissions),editing,const DeepCollectionEquality().hash(_tileOrder),const DeepCollectionEquality().hash(_ringOrder),const DeepCollectionEquality().hash(_hiddenTiles));

@override
String toString() {
  return 'DashboardState(selectedDate: $selectedDate, data: $data, isLoading: $isLoading, isRefreshing: $isRefreshing, error: $error, sleepRangeMode: $sleepRangeMode, activityWeekMode: $activityWeekMode, showOpenVitalsCalculatedCalories: $showOpenVitalsCalculatedCalories, healthConnectAvailability: $healthConnectAvailability, minimumPermissionsGranted: $minimumPermissionsGranted, loadingMetrics: $loadingMetrics, unacknowledgedPermissions: $unacknowledgedPermissions, editing: $editing, tileOrder: $tileOrder, ringOrder: $ringOrder, hiddenTiles: $hiddenTiles)';
}


}

/// @nodoc
abstract mixin class _$DashboardStateCopyWith<$Res> implements $DashboardStateCopyWith<$Res> {
  factory _$DashboardStateCopyWith(_DashboardState value, $Res Function(_DashboardState) _then) = __$DashboardStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, DashboardData? data, bool isLoading, bool isRefreshing, ScreenError? error, SleepRangeMode sleepRangeMode, ActivityWeekMode activityWeekMode, bool showOpenVitalsCalculatedCalories, HealthConnectAvailability healthConnectAvailability, bool minimumPermissionsGranted, Set<DashboardMetric> loadingMetrics, Set<String> unacknowledgedPermissions, bool editing, List<String> tileOrder, List<String> ringOrder, Set<String> hiddenTiles
});


@override $DashboardDataCopyWith<$Res>? get data;

}
/// @nodoc
class __$DashboardStateCopyWithImpl<$Res>
    implements _$DashboardStateCopyWith<$Res> {
  __$DashboardStateCopyWithImpl(this._self, this._then);

  final _DashboardState _self;
  final $Res Function(_DashboardState) _then;

/// Create a copy of DashboardState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? data = freezed,Object? isLoading = null,Object? isRefreshing = null,Object? error = freezed,Object? sleepRangeMode = null,Object? activityWeekMode = null,Object? showOpenVitalsCalculatedCalories = null,Object? healthConnectAvailability = null,Object? minimumPermissionsGranted = null,Object? loadingMetrics = null,Object? unacknowledgedPermissions = null,Object? editing = null,Object? tileOrder = null,Object? ringOrder = null,Object? hiddenTiles = null,}) {
  return _then(_DashboardState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,data: freezed == data ? _self.data : data // ignore: cast_nullable_to_non_nullable
as DashboardData?,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,isRefreshing: null == isRefreshing ? _self.isRefreshing : isRefreshing // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,sleepRangeMode: null == sleepRangeMode ? _self.sleepRangeMode : sleepRangeMode // ignore: cast_nullable_to_non_nullable
as SleepRangeMode,activityWeekMode: null == activityWeekMode ? _self.activityWeekMode : activityWeekMode // ignore: cast_nullable_to_non_nullable
as ActivityWeekMode,showOpenVitalsCalculatedCalories: null == showOpenVitalsCalculatedCalories ? _self.showOpenVitalsCalculatedCalories : showOpenVitalsCalculatedCalories // ignore: cast_nullable_to_non_nullable
as bool,healthConnectAvailability: null == healthConnectAvailability ? _self.healthConnectAvailability : healthConnectAvailability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability,minimumPermissionsGranted: null == minimumPermissionsGranted ? _self.minimumPermissionsGranted : minimumPermissionsGranted // ignore: cast_nullable_to_non_nullable
as bool,loadingMetrics: null == loadingMetrics ? _self._loadingMetrics : loadingMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,unacknowledgedPermissions: null == unacknowledgedPermissions ? _self._unacknowledgedPermissions : unacknowledgedPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,editing: null == editing ? _self.editing : editing // ignore: cast_nullable_to_non_nullable
as bool,tileOrder: null == tileOrder ? _self._tileOrder : tileOrder // ignore: cast_nullable_to_non_nullable
as List<String>,ringOrder: null == ringOrder ? _self._ringOrder : ringOrder // ignore: cast_nullable_to_non_nullable
as List<String>,hiddenTiles: null == hiddenTiles ? _self._hiddenTiles : hiddenTiles // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}

/// Create a copy of DashboardState
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
}
}

// dart format on
