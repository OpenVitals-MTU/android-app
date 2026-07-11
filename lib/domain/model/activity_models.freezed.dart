// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ExerciseData {

 String get id; String? get title; int get exerciseType; DateTime get startTime; DateTime get endTime; int get durationMs; String get source; double? get totalDistanceMeters; double? get totalCaloriesKcal; double? get activeCaloriesKcal; int? get steps; int? get wheelchairPushes; double? get averageSpeedMetersPerSecond; double? get averagePowerWatts; double? get averageStepsCadenceRate; double? get averageCyclingCadenceRpm; int? get averageHeartRateBpm; int? get floorsClimbed; double? get elevationGainedMeters; String? get notes; Duration? get startZoneOffset; Duration? get endZoneOffset; DateTime? get lastModifiedTime; String? get clientRecordId; int? get clientRecordVersion; int? get recordingMethod; ExerciseDeviceData? get device; String? get plannedExerciseSessionId; List<ExerciseSegmentData> get segments; List<ExerciseLapData> get laps; ExerciseRouteData get route; bool get isOpenVitalsEntry; CaloriesBurnedSource get totalCaloriesSource;
/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseDataCopyWith<ExerciseData> get copyWith => _$ExerciseDataCopyWithImpl<ExerciseData>(this as ExerciseData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseData&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.totalDistanceMeters, totalDistanceMeters) || other.totalDistanceMeters == totalDistanceMeters)&&(identical(other.totalCaloriesKcal, totalCaloriesKcal) || other.totalCaloriesKcal == totalCaloriesKcal)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.averageSpeedMetersPerSecond, averageSpeedMetersPerSecond) || other.averageSpeedMetersPerSecond == averageSpeedMetersPerSecond)&&(identical(other.averagePowerWatts, averagePowerWatts) || other.averagePowerWatts == averagePowerWatts)&&(identical(other.averageStepsCadenceRate, averageStepsCadenceRate) || other.averageStepsCadenceRate == averageStepsCadenceRate)&&(identical(other.averageCyclingCadenceRpm, averageCyclingCadenceRpm) || other.averageCyclingCadenceRpm == averageCyclingCadenceRpm)&&(identical(other.averageHeartRateBpm, averageHeartRateBpm) || other.averageHeartRateBpm == averageHeartRateBpm)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.startZoneOffset, startZoneOffset) || other.startZoneOffset == startZoneOffset)&&(identical(other.endZoneOffset, endZoneOffset) || other.endZoneOffset == endZoneOffset)&&(identical(other.lastModifiedTime, lastModifiedTime) || other.lastModifiedTime == lastModifiedTime)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.clientRecordVersion, clientRecordVersion) || other.clientRecordVersion == clientRecordVersion)&&(identical(other.recordingMethod, recordingMethod) || other.recordingMethod == recordingMethod)&&(identical(other.device, device) || other.device == device)&&(identical(other.plannedExerciseSessionId, plannedExerciseSessionId) || other.plannedExerciseSessionId == plannedExerciseSessionId)&&const DeepCollectionEquality().equals(other.segments, segments)&&const DeepCollectionEquality().equals(other.laps, laps)&&(identical(other.route, route) || other.route == route)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry)&&(identical(other.totalCaloriesSource, totalCaloriesSource) || other.totalCaloriesSource == totalCaloriesSource));
}


@override
int get hashCode => Object.hashAll([runtimeType,id,title,exerciseType,startTime,endTime,durationMs,source,totalDistanceMeters,totalCaloriesKcal,activeCaloriesKcal,steps,wheelchairPushes,averageSpeedMetersPerSecond,averagePowerWatts,averageStepsCadenceRate,averageCyclingCadenceRpm,averageHeartRateBpm,floorsClimbed,elevationGainedMeters,notes,startZoneOffset,endZoneOffset,lastModifiedTime,clientRecordId,clientRecordVersion,recordingMethod,device,plannedExerciseSessionId,const DeepCollectionEquality().hash(segments),const DeepCollectionEquality().hash(laps),route,isOpenVitalsEntry,totalCaloriesSource]);

@override
String toString() {
  return 'ExerciseData(id: $id, title: $title, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, totalDistanceMeters: $totalDistanceMeters, totalCaloriesKcal: $totalCaloriesKcal, activeCaloriesKcal: $activeCaloriesKcal, steps: $steps, wheelchairPushes: $wheelchairPushes, averageSpeedMetersPerSecond: $averageSpeedMetersPerSecond, averagePowerWatts: $averagePowerWatts, averageStepsCadenceRate: $averageStepsCadenceRate, averageCyclingCadenceRpm: $averageCyclingCadenceRpm, averageHeartRateBpm: $averageHeartRateBpm, floorsClimbed: $floorsClimbed, elevationGainedMeters: $elevationGainedMeters, notes: $notes, startZoneOffset: $startZoneOffset, endZoneOffset: $endZoneOffset, lastModifiedTime: $lastModifiedTime, clientRecordId: $clientRecordId, clientRecordVersion: $clientRecordVersion, recordingMethod: $recordingMethod, device: $device, plannedExerciseSessionId: $plannedExerciseSessionId, segments: $segments, laps: $laps, route: $route, isOpenVitalsEntry: $isOpenVitalsEntry, totalCaloriesSource: $totalCaloriesSource)';
}


}

/// @nodoc
abstract mixin class $ExerciseDataCopyWith<$Res>  {
  factory $ExerciseDataCopyWith(ExerciseData value, $Res Function(ExerciseData) _then) = _$ExerciseDataCopyWithImpl;
@useResult
$Res call({
 String id, String? title, int exerciseType, DateTime startTime, DateTime endTime, int durationMs, String source, double? totalDistanceMeters, double? totalCaloriesKcal, double? activeCaloriesKcal, int? steps, int? wheelchairPushes, double? averageSpeedMetersPerSecond, double? averagePowerWatts, double? averageStepsCadenceRate, double? averageCyclingCadenceRpm, int? averageHeartRateBpm, int? floorsClimbed, double? elevationGainedMeters, String? notes, Duration? startZoneOffset, Duration? endZoneOffset, DateTime? lastModifiedTime, String? clientRecordId, int? clientRecordVersion, int? recordingMethod, ExerciseDeviceData? device, String? plannedExerciseSessionId, List<ExerciseSegmentData> segments, List<ExerciseLapData> laps, ExerciseRouteData route, bool isOpenVitalsEntry, CaloriesBurnedSource totalCaloriesSource
});


$ExerciseDeviceDataCopyWith<$Res>? get device;$ExerciseRouteDataCopyWith<$Res> get route;

}
/// @nodoc
class _$ExerciseDataCopyWithImpl<$Res>
    implements $ExerciseDataCopyWith<$Res> {
  _$ExerciseDataCopyWithImpl(this._self, this._then);

  final ExerciseData _self;
  final $Res Function(ExerciseData) _then;

/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? title = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? totalDistanceMeters = freezed,Object? totalCaloriesKcal = freezed,Object? activeCaloriesKcal = freezed,Object? steps = freezed,Object? wheelchairPushes = freezed,Object? averageSpeedMetersPerSecond = freezed,Object? averagePowerWatts = freezed,Object? averageStepsCadenceRate = freezed,Object? averageCyclingCadenceRpm = freezed,Object? averageHeartRateBpm = freezed,Object? floorsClimbed = freezed,Object? elevationGainedMeters = freezed,Object? notes = freezed,Object? startZoneOffset = freezed,Object? endZoneOffset = freezed,Object? lastModifiedTime = freezed,Object? clientRecordId = freezed,Object? clientRecordVersion = freezed,Object? recordingMethod = freezed,Object? device = freezed,Object? plannedExerciseSessionId = freezed,Object? segments = null,Object? laps = null,Object? route = null,Object? isOpenVitalsEntry = null,Object? totalCaloriesSource = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,totalDistanceMeters: freezed == totalDistanceMeters ? _self.totalDistanceMeters : totalDistanceMeters // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesKcal: freezed == totalCaloriesKcal ? _self.totalCaloriesKcal : totalCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,steps: freezed == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int?,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,averageSpeedMetersPerSecond: freezed == averageSpeedMetersPerSecond ? _self.averageSpeedMetersPerSecond : averageSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,averagePowerWatts: freezed == averagePowerWatts ? _self.averagePowerWatts : averagePowerWatts // ignore: cast_nullable_to_non_nullable
as double?,averageStepsCadenceRate: freezed == averageStepsCadenceRate ? _self.averageStepsCadenceRate : averageStepsCadenceRate // ignore: cast_nullable_to_non_nullable
as double?,averageCyclingCadenceRpm: freezed == averageCyclingCadenceRpm ? _self.averageCyclingCadenceRpm : averageCyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as double?,averageHeartRateBpm: freezed == averageHeartRateBpm ? _self.averageHeartRateBpm : averageHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,startZoneOffset: freezed == startZoneOffset ? _self.startZoneOffset : startZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,endZoneOffset: freezed == endZoneOffset ? _self.endZoneOffset : endZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,lastModifiedTime: freezed == lastModifiedTime ? _self.lastModifiedTime : lastModifiedTime // ignore: cast_nullable_to_non_nullable
as DateTime?,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,clientRecordVersion: freezed == clientRecordVersion ? _self.clientRecordVersion : clientRecordVersion // ignore: cast_nullable_to_non_nullable
as int?,recordingMethod: freezed == recordingMethod ? _self.recordingMethod : recordingMethod // ignore: cast_nullable_to_non_nullable
as int?,device: freezed == device ? _self.device : device // ignore: cast_nullable_to_non_nullable
as ExerciseDeviceData?,plannedExerciseSessionId: freezed == plannedExerciseSessionId ? _self.plannedExerciseSessionId : plannedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,segments: null == segments ? _self.segments : segments // ignore: cast_nullable_to_non_nullable
as List<ExerciseSegmentData>,laps: null == laps ? _self.laps : laps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,route: null == route ? _self.route : route // ignore: cast_nullable_to_non_nullable
as ExerciseRouteData,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,totalCaloriesSource: null == totalCaloriesSource ? _self.totalCaloriesSource : totalCaloriesSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}
/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDeviceDataCopyWith<$Res>? get device {
    if (_self.device == null) {
    return null;
  }

  return $ExerciseDeviceDataCopyWith<$Res>(_self.device!, (value) {
    return _then(_self.copyWith(device: value));
  });
}/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseRouteDataCopyWith<$Res> get route {
  
  return $ExerciseRouteDataCopyWith<$Res>(_self.route, (value) {
    return _then(_self.copyWith(route: value));
  });
}
}


/// Adds pattern-matching-related methods to [ExerciseData].
extension ExerciseDataPatterns on ExerciseData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _ExerciseData value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseData() when build != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _ExerciseData value)  build,}){
final _that = this;
switch (_that) {
case _ExerciseData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _ExerciseData value)?  build,}){
final _that = this;
switch (_that) {
case _ExerciseData() when build != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  double? totalDistanceMeters,  double? totalCaloriesKcal,  double? activeCaloriesKcal,  int? steps,  int? wheelchairPushes,  double? averageSpeedMetersPerSecond,  double? averagePowerWatts,  double? averageStepsCadenceRate,  double? averageCyclingCadenceRpm,  int? averageHeartRateBpm,  int? floorsClimbed,  double? elevationGainedMeters,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  ExerciseDeviceData? device,  String? plannedExerciseSessionId,  List<ExerciseSegmentData> segments,  List<ExerciseLapData> laps,  ExerciseRouteData route,  bool isOpenVitalsEntry,  CaloriesBurnedSource totalCaloriesSource)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseData() when build != null:
return build(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.totalDistanceMeters,_that.totalCaloriesKcal,_that.activeCaloriesKcal,_that.steps,_that.wheelchairPushes,_that.averageSpeedMetersPerSecond,_that.averagePowerWatts,_that.averageStepsCadenceRate,_that.averageCyclingCadenceRpm,_that.averageHeartRateBpm,_that.floorsClimbed,_that.elevationGainedMeters,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.plannedExerciseSessionId,_that.segments,_that.laps,_that.route,_that.isOpenVitalsEntry,_that.totalCaloriesSource);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  double? totalDistanceMeters,  double? totalCaloriesKcal,  double? activeCaloriesKcal,  int? steps,  int? wheelchairPushes,  double? averageSpeedMetersPerSecond,  double? averagePowerWatts,  double? averageStepsCadenceRate,  double? averageCyclingCadenceRpm,  int? averageHeartRateBpm,  int? floorsClimbed,  double? elevationGainedMeters,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  ExerciseDeviceData? device,  String? plannedExerciseSessionId,  List<ExerciseSegmentData> segments,  List<ExerciseLapData> laps,  ExerciseRouteData route,  bool isOpenVitalsEntry,  CaloriesBurnedSource totalCaloriesSource)  build,}) {final _that = this;
switch (_that) {
case _ExerciseData():
return build(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.totalDistanceMeters,_that.totalCaloriesKcal,_that.activeCaloriesKcal,_that.steps,_that.wheelchairPushes,_that.averageSpeedMetersPerSecond,_that.averagePowerWatts,_that.averageStepsCadenceRate,_that.averageCyclingCadenceRpm,_that.averageHeartRateBpm,_that.floorsClimbed,_that.elevationGainedMeters,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.plannedExerciseSessionId,_that.segments,_that.laps,_that.route,_that.isOpenVitalsEntry,_that.totalCaloriesSource);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  double? totalDistanceMeters,  double? totalCaloriesKcal,  double? activeCaloriesKcal,  int? steps,  int? wheelchairPushes,  double? averageSpeedMetersPerSecond,  double? averagePowerWatts,  double? averageStepsCadenceRate,  double? averageCyclingCadenceRpm,  int? averageHeartRateBpm,  int? floorsClimbed,  double? elevationGainedMeters,  String? notes,  Duration? startZoneOffset,  Duration? endZoneOffset,  DateTime? lastModifiedTime,  String? clientRecordId,  int? clientRecordVersion,  int? recordingMethod,  ExerciseDeviceData? device,  String? plannedExerciseSessionId,  List<ExerciseSegmentData> segments,  List<ExerciseLapData> laps,  ExerciseRouteData route,  bool isOpenVitalsEntry,  CaloriesBurnedSource totalCaloriesSource)?  build,}) {final _that = this;
switch (_that) {
case _ExerciseData() when build != null:
return build(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.totalDistanceMeters,_that.totalCaloriesKcal,_that.activeCaloriesKcal,_that.steps,_that.wheelchairPushes,_that.averageSpeedMetersPerSecond,_that.averagePowerWatts,_that.averageStepsCadenceRate,_that.averageCyclingCadenceRpm,_that.averageHeartRateBpm,_that.floorsClimbed,_that.elevationGainedMeters,_that.notes,_that.startZoneOffset,_that.endZoneOffset,_that.lastModifiedTime,_that.clientRecordId,_that.clientRecordVersion,_that.recordingMethod,_that.device,_that.plannedExerciseSessionId,_that.segments,_that.laps,_that.route,_that.isOpenVitalsEntry,_that.totalCaloriesSource);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseData extends ExerciseData {
  const _ExerciseData({required this.id, required this.title, required this.exerciseType, required this.startTime, required this.endTime, required this.durationMs, required this.source, required this.totalDistanceMeters, required this.totalCaloriesKcal, required this.activeCaloriesKcal, required this.steps, required this.wheelchairPushes, required this.averageSpeedMetersPerSecond, required this.averagePowerWatts, required this.averageStepsCadenceRate, required this.averageCyclingCadenceRpm, required this.averageHeartRateBpm, required this.floorsClimbed, required this.elevationGainedMeters, required this.notes, required this.startZoneOffset, required this.endZoneOffset, required this.lastModifiedTime, required this.clientRecordId, required this.clientRecordVersion, required this.recordingMethod, required this.device, required this.plannedExerciseSessionId, required final  List<ExerciseSegmentData> segments, required final  List<ExerciseLapData> laps, required this.route, required this.isOpenVitalsEntry, required this.totalCaloriesSource}): _segments = segments,_laps = laps,super._();
  

@override final  String id;
@override final  String? title;
@override final  int exerciseType;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int durationMs;
@override final  String source;
@override final  double? totalDistanceMeters;
@override final  double? totalCaloriesKcal;
@override final  double? activeCaloriesKcal;
@override final  int? steps;
@override final  int? wheelchairPushes;
@override final  double? averageSpeedMetersPerSecond;
@override final  double? averagePowerWatts;
@override final  double? averageStepsCadenceRate;
@override final  double? averageCyclingCadenceRpm;
@override final  int? averageHeartRateBpm;
@override final  int? floorsClimbed;
@override final  double? elevationGainedMeters;
@override final  String? notes;
@override final  Duration? startZoneOffset;
@override final  Duration? endZoneOffset;
@override final  DateTime? lastModifiedTime;
@override final  String? clientRecordId;
@override final  int? clientRecordVersion;
@override final  int? recordingMethod;
@override final  ExerciseDeviceData? device;
@override final  String? plannedExerciseSessionId;
 final  List<ExerciseSegmentData> _segments;
@override List<ExerciseSegmentData> get segments {
  if (_segments is EqualUnmodifiableListView) return _segments;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_segments);
}

 final  List<ExerciseLapData> _laps;
@override List<ExerciseLapData> get laps {
  if (_laps is EqualUnmodifiableListView) return _laps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_laps);
}

@override final  ExerciseRouteData route;
@override final  bool isOpenVitalsEntry;
@override final  CaloriesBurnedSource totalCaloriesSource;

/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseDataCopyWith<_ExerciseData> get copyWith => __$ExerciseDataCopyWithImpl<_ExerciseData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseData&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.totalDistanceMeters, totalDistanceMeters) || other.totalDistanceMeters == totalDistanceMeters)&&(identical(other.totalCaloriesKcal, totalCaloriesKcal) || other.totalCaloriesKcal == totalCaloriesKcal)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.averageSpeedMetersPerSecond, averageSpeedMetersPerSecond) || other.averageSpeedMetersPerSecond == averageSpeedMetersPerSecond)&&(identical(other.averagePowerWatts, averagePowerWatts) || other.averagePowerWatts == averagePowerWatts)&&(identical(other.averageStepsCadenceRate, averageStepsCadenceRate) || other.averageStepsCadenceRate == averageStepsCadenceRate)&&(identical(other.averageCyclingCadenceRpm, averageCyclingCadenceRpm) || other.averageCyclingCadenceRpm == averageCyclingCadenceRpm)&&(identical(other.averageHeartRateBpm, averageHeartRateBpm) || other.averageHeartRateBpm == averageHeartRateBpm)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.startZoneOffset, startZoneOffset) || other.startZoneOffset == startZoneOffset)&&(identical(other.endZoneOffset, endZoneOffset) || other.endZoneOffset == endZoneOffset)&&(identical(other.lastModifiedTime, lastModifiedTime) || other.lastModifiedTime == lastModifiedTime)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.clientRecordVersion, clientRecordVersion) || other.clientRecordVersion == clientRecordVersion)&&(identical(other.recordingMethod, recordingMethod) || other.recordingMethod == recordingMethod)&&(identical(other.device, device) || other.device == device)&&(identical(other.plannedExerciseSessionId, plannedExerciseSessionId) || other.plannedExerciseSessionId == plannedExerciseSessionId)&&const DeepCollectionEquality().equals(other._segments, _segments)&&const DeepCollectionEquality().equals(other._laps, _laps)&&(identical(other.route, route) || other.route == route)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry)&&(identical(other.totalCaloriesSource, totalCaloriesSource) || other.totalCaloriesSource == totalCaloriesSource));
}


@override
int get hashCode => Object.hashAll([runtimeType,id,title,exerciseType,startTime,endTime,durationMs,source,totalDistanceMeters,totalCaloriesKcal,activeCaloriesKcal,steps,wheelchairPushes,averageSpeedMetersPerSecond,averagePowerWatts,averageStepsCadenceRate,averageCyclingCadenceRpm,averageHeartRateBpm,floorsClimbed,elevationGainedMeters,notes,startZoneOffset,endZoneOffset,lastModifiedTime,clientRecordId,clientRecordVersion,recordingMethod,device,plannedExerciseSessionId,const DeepCollectionEquality().hash(_segments),const DeepCollectionEquality().hash(_laps),route,isOpenVitalsEntry,totalCaloriesSource]);

@override
String toString() {
  return 'ExerciseData.build(id: $id, title: $title, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, totalDistanceMeters: $totalDistanceMeters, totalCaloriesKcal: $totalCaloriesKcal, activeCaloriesKcal: $activeCaloriesKcal, steps: $steps, wheelchairPushes: $wheelchairPushes, averageSpeedMetersPerSecond: $averageSpeedMetersPerSecond, averagePowerWatts: $averagePowerWatts, averageStepsCadenceRate: $averageStepsCadenceRate, averageCyclingCadenceRpm: $averageCyclingCadenceRpm, averageHeartRateBpm: $averageHeartRateBpm, floorsClimbed: $floorsClimbed, elevationGainedMeters: $elevationGainedMeters, notes: $notes, startZoneOffset: $startZoneOffset, endZoneOffset: $endZoneOffset, lastModifiedTime: $lastModifiedTime, clientRecordId: $clientRecordId, clientRecordVersion: $clientRecordVersion, recordingMethod: $recordingMethod, device: $device, plannedExerciseSessionId: $plannedExerciseSessionId, segments: $segments, laps: $laps, route: $route, isOpenVitalsEntry: $isOpenVitalsEntry, totalCaloriesSource: $totalCaloriesSource)';
}


}

/// @nodoc
abstract mixin class _$ExerciseDataCopyWith<$Res> implements $ExerciseDataCopyWith<$Res> {
  factory _$ExerciseDataCopyWith(_ExerciseData value, $Res Function(_ExerciseData) _then) = __$ExerciseDataCopyWithImpl;
@override @useResult
$Res call({
 String id, String? title, int exerciseType, DateTime startTime, DateTime endTime, int durationMs, String source, double? totalDistanceMeters, double? totalCaloriesKcal, double? activeCaloriesKcal, int? steps, int? wheelchairPushes, double? averageSpeedMetersPerSecond, double? averagePowerWatts, double? averageStepsCadenceRate, double? averageCyclingCadenceRpm, int? averageHeartRateBpm, int? floorsClimbed, double? elevationGainedMeters, String? notes, Duration? startZoneOffset, Duration? endZoneOffset, DateTime? lastModifiedTime, String? clientRecordId, int? clientRecordVersion, int? recordingMethod, ExerciseDeviceData? device, String? plannedExerciseSessionId, List<ExerciseSegmentData> segments, List<ExerciseLapData> laps, ExerciseRouteData route, bool isOpenVitalsEntry, CaloriesBurnedSource totalCaloriesSource
});


@override $ExerciseDeviceDataCopyWith<$Res>? get device;@override $ExerciseRouteDataCopyWith<$Res> get route;

}
/// @nodoc
class __$ExerciseDataCopyWithImpl<$Res>
    implements _$ExerciseDataCopyWith<$Res> {
  __$ExerciseDataCopyWithImpl(this._self, this._then);

  final _ExerciseData _self;
  final $Res Function(_ExerciseData) _then;

/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? title = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? totalDistanceMeters = freezed,Object? totalCaloriesKcal = freezed,Object? activeCaloriesKcal = freezed,Object? steps = freezed,Object? wheelchairPushes = freezed,Object? averageSpeedMetersPerSecond = freezed,Object? averagePowerWatts = freezed,Object? averageStepsCadenceRate = freezed,Object? averageCyclingCadenceRpm = freezed,Object? averageHeartRateBpm = freezed,Object? floorsClimbed = freezed,Object? elevationGainedMeters = freezed,Object? notes = freezed,Object? startZoneOffset = freezed,Object? endZoneOffset = freezed,Object? lastModifiedTime = freezed,Object? clientRecordId = freezed,Object? clientRecordVersion = freezed,Object? recordingMethod = freezed,Object? device = freezed,Object? plannedExerciseSessionId = freezed,Object? segments = null,Object? laps = null,Object? route = null,Object? isOpenVitalsEntry = null,Object? totalCaloriesSource = null,}) {
  return _then(_ExerciseData(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,totalDistanceMeters: freezed == totalDistanceMeters ? _self.totalDistanceMeters : totalDistanceMeters // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesKcal: freezed == totalCaloriesKcal ? _self.totalCaloriesKcal : totalCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,steps: freezed == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int?,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,averageSpeedMetersPerSecond: freezed == averageSpeedMetersPerSecond ? _self.averageSpeedMetersPerSecond : averageSpeedMetersPerSecond // ignore: cast_nullable_to_non_nullable
as double?,averagePowerWatts: freezed == averagePowerWatts ? _self.averagePowerWatts : averagePowerWatts // ignore: cast_nullable_to_non_nullable
as double?,averageStepsCadenceRate: freezed == averageStepsCadenceRate ? _self.averageStepsCadenceRate : averageStepsCadenceRate // ignore: cast_nullable_to_non_nullable
as double?,averageCyclingCadenceRpm: freezed == averageCyclingCadenceRpm ? _self.averageCyclingCadenceRpm : averageCyclingCadenceRpm // ignore: cast_nullable_to_non_nullable
as double?,averageHeartRateBpm: freezed == averageHeartRateBpm ? _self.averageHeartRateBpm : averageHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,startZoneOffset: freezed == startZoneOffset ? _self.startZoneOffset : startZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,endZoneOffset: freezed == endZoneOffset ? _self.endZoneOffset : endZoneOffset // ignore: cast_nullable_to_non_nullable
as Duration?,lastModifiedTime: freezed == lastModifiedTime ? _self.lastModifiedTime : lastModifiedTime // ignore: cast_nullable_to_non_nullable
as DateTime?,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,clientRecordVersion: freezed == clientRecordVersion ? _self.clientRecordVersion : clientRecordVersion // ignore: cast_nullable_to_non_nullable
as int?,recordingMethod: freezed == recordingMethod ? _self.recordingMethod : recordingMethod // ignore: cast_nullable_to_non_nullable
as int?,device: freezed == device ? _self.device : device // ignore: cast_nullable_to_non_nullable
as ExerciseDeviceData?,plannedExerciseSessionId: freezed == plannedExerciseSessionId ? _self.plannedExerciseSessionId : plannedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,segments: null == segments ? _self._segments : segments // ignore: cast_nullable_to_non_nullable
as List<ExerciseSegmentData>,laps: null == laps ? _self._laps : laps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,route: null == route ? _self.route : route // ignore: cast_nullable_to_non_nullable
as ExerciseRouteData,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,totalCaloriesSource: null == totalCaloriesSource ? _self.totalCaloriesSource : totalCaloriesSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}

/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDeviceDataCopyWith<$Res>? get device {
    if (_self.device == null) {
    return null;
  }

  return $ExerciseDeviceDataCopyWith<$Res>(_self.device!, (value) {
    return _then(_self.copyWith(device: value));
  });
}/// Create a copy of ExerciseData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseRouteDataCopyWith<$Res> get route {
  
  return $ExerciseRouteDataCopyWith<$Res>(_self.route, (value) {
    return _then(_self.copyWith(route: value));
  });
}
}

/// @nodoc
mixin _$ExerciseDeviceData {

 int get type; String? get manufacturer; String? get model;
/// Create a copy of ExerciseDeviceData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseDeviceDataCopyWith<ExerciseDeviceData> get copyWith => _$ExerciseDeviceDataCopyWithImpl<ExerciseDeviceData>(this as ExerciseDeviceData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseDeviceData&&(identical(other.type, type) || other.type == type)&&(identical(other.manufacturer, manufacturer) || other.manufacturer == manufacturer)&&(identical(other.model, model) || other.model == model));
}


@override
int get hashCode => Object.hash(runtimeType,type,manufacturer,model);

@override
String toString() {
  return 'ExerciseDeviceData(type: $type, manufacturer: $manufacturer, model: $model)';
}


}

/// @nodoc
abstract mixin class $ExerciseDeviceDataCopyWith<$Res>  {
  factory $ExerciseDeviceDataCopyWith(ExerciseDeviceData value, $Res Function(ExerciseDeviceData) _then) = _$ExerciseDeviceDataCopyWithImpl;
@useResult
$Res call({
 int type, String? manufacturer, String? model
});




}
/// @nodoc
class _$ExerciseDeviceDataCopyWithImpl<$Res>
    implements $ExerciseDeviceDataCopyWith<$Res> {
  _$ExerciseDeviceDataCopyWithImpl(this._self, this._then);

  final ExerciseDeviceData _self;
  final $Res Function(ExerciseDeviceData) _then;

/// Create a copy of ExerciseDeviceData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? manufacturer = freezed,Object? model = freezed,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as int,manufacturer: freezed == manufacturer ? _self.manufacturer : manufacturer // ignore: cast_nullable_to_non_nullable
as String?,model: freezed == model ? _self.model : model // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [ExerciseDeviceData].
extension ExerciseDeviceDataPatterns on ExerciseDeviceData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ExerciseDeviceData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseDeviceData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ExerciseDeviceData value)  $default,){
final _that = this;
switch (_that) {
case _ExerciseDeviceData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ExerciseDeviceData value)?  $default,){
final _that = this;
switch (_that) {
case _ExerciseDeviceData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int type,  String? manufacturer,  String? model)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseDeviceData() when $default != null:
return $default(_that.type,_that.manufacturer,_that.model);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int type,  String? manufacturer,  String? model)  $default,) {final _that = this;
switch (_that) {
case _ExerciseDeviceData():
return $default(_that.type,_that.manufacturer,_that.model);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int type,  String? manufacturer,  String? model)?  $default,) {final _that = this;
switch (_that) {
case _ExerciseDeviceData() when $default != null:
return $default(_that.type,_that.manufacturer,_that.model);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseDeviceData implements ExerciseDeviceData {
  const _ExerciseDeviceData({required this.type, required this.manufacturer, required this.model});
  

@override final  int type;
@override final  String? manufacturer;
@override final  String? model;

/// Create a copy of ExerciseDeviceData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseDeviceDataCopyWith<_ExerciseDeviceData> get copyWith => __$ExerciseDeviceDataCopyWithImpl<_ExerciseDeviceData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseDeviceData&&(identical(other.type, type) || other.type == type)&&(identical(other.manufacturer, manufacturer) || other.manufacturer == manufacturer)&&(identical(other.model, model) || other.model == model));
}


@override
int get hashCode => Object.hash(runtimeType,type,manufacturer,model);

@override
String toString() {
  return 'ExerciseDeviceData(type: $type, manufacturer: $manufacturer, model: $model)';
}


}

/// @nodoc
abstract mixin class _$ExerciseDeviceDataCopyWith<$Res> implements $ExerciseDeviceDataCopyWith<$Res> {
  factory _$ExerciseDeviceDataCopyWith(_ExerciseDeviceData value, $Res Function(_ExerciseDeviceData) _then) = __$ExerciseDeviceDataCopyWithImpl;
@override @useResult
$Res call({
 int type, String? manufacturer, String? model
});




}
/// @nodoc
class __$ExerciseDeviceDataCopyWithImpl<$Res>
    implements _$ExerciseDeviceDataCopyWith<$Res> {
  __$ExerciseDeviceDataCopyWithImpl(this._self, this._then);

  final _ExerciseDeviceData _self;
  final $Res Function(_ExerciseDeviceData) _then;

/// Create a copy of ExerciseDeviceData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? manufacturer = freezed,Object? model = freezed,}) {
  return _then(_ExerciseDeviceData(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as int,manufacturer: freezed == manufacturer ? _self.manufacturer : manufacturer // ignore: cast_nullable_to_non_nullable
as String?,model: freezed == model ? _self.model : model // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

/// @nodoc
mixin _$ExerciseSegmentData {

 DateTime get startTime; DateTime get endTime; int get segmentType; int get repetitions; int? get setIndex;
/// Create a copy of ExerciseSegmentData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseSegmentDataCopyWith<ExerciseSegmentData> get copyWith => _$ExerciseSegmentDataCopyWithImpl<ExerciseSegmentData>(this as ExerciseSegmentData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseSegmentData&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.segmentType, segmentType) || other.segmentType == segmentType)&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.setIndex, setIndex) || other.setIndex == setIndex));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,segmentType,repetitions,setIndex);

@override
String toString() {
  return 'ExerciseSegmentData(startTime: $startTime, endTime: $endTime, segmentType: $segmentType, repetitions: $repetitions, setIndex: $setIndex)';
}


}

/// @nodoc
abstract mixin class $ExerciseSegmentDataCopyWith<$Res>  {
  factory $ExerciseSegmentDataCopyWith(ExerciseSegmentData value, $Res Function(ExerciseSegmentData) _then) = _$ExerciseSegmentDataCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, int segmentType, int repetitions, int? setIndex
});




}
/// @nodoc
class _$ExerciseSegmentDataCopyWithImpl<$Res>
    implements $ExerciseSegmentDataCopyWith<$Res> {
  _$ExerciseSegmentDataCopyWithImpl(this._self, this._then);

  final ExerciseSegmentData _self;
  final $Res Function(ExerciseSegmentData) _then;

/// Create a copy of ExerciseSegmentData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? segmentType = null,Object? repetitions = null,Object? setIndex = freezed,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,segmentType: null == segmentType ? _self.segmentType : segmentType // ignore: cast_nullable_to_non_nullable
as int,repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,setIndex: freezed == setIndex ? _self.setIndex : setIndex // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [ExerciseSegmentData].
extension ExerciseSegmentDataPatterns on ExerciseSegmentData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ExerciseSegmentData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseSegmentData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ExerciseSegmentData value)  $default,){
final _that = this;
switch (_that) {
case _ExerciseSegmentData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ExerciseSegmentData value)?  $default,){
final _that = this;
switch (_that) {
case _ExerciseSegmentData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseSegmentData() when $default != null:
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)  $default,) {final _that = this;
switch (_that) {
case _ExerciseSegmentData():
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)?  $default,) {final _that = this;
switch (_that) {
case _ExerciseSegmentData() when $default != null:
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseSegmentData extends ExerciseSegmentData {
  const _ExerciseSegmentData({required this.startTime, required this.endTime, required this.segmentType, required this.repetitions, this.setIndex}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int segmentType;
@override final  int repetitions;
@override final  int? setIndex;

/// Create a copy of ExerciseSegmentData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseSegmentDataCopyWith<_ExerciseSegmentData> get copyWith => __$ExerciseSegmentDataCopyWithImpl<_ExerciseSegmentData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseSegmentData&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.segmentType, segmentType) || other.segmentType == segmentType)&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.setIndex, setIndex) || other.setIndex == setIndex));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,segmentType,repetitions,setIndex);

@override
String toString() {
  return 'ExerciseSegmentData(startTime: $startTime, endTime: $endTime, segmentType: $segmentType, repetitions: $repetitions, setIndex: $setIndex)';
}


}

/// @nodoc
abstract mixin class _$ExerciseSegmentDataCopyWith<$Res> implements $ExerciseSegmentDataCopyWith<$Res> {
  factory _$ExerciseSegmentDataCopyWith(_ExerciseSegmentData value, $Res Function(_ExerciseSegmentData) _then) = __$ExerciseSegmentDataCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, int segmentType, int repetitions, int? setIndex
});




}
/// @nodoc
class __$ExerciseSegmentDataCopyWithImpl<$Res>
    implements _$ExerciseSegmentDataCopyWith<$Res> {
  __$ExerciseSegmentDataCopyWithImpl(this._self, this._then);

  final _ExerciseSegmentData _self;
  final $Res Function(_ExerciseSegmentData) _then;

/// Create a copy of ExerciseSegmentData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? segmentType = null,Object? repetitions = null,Object? setIndex = freezed,}) {
  return _then(_ExerciseSegmentData(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,segmentType: null == segmentType ? _self.segmentType : segmentType // ignore: cast_nullable_to_non_nullable
as int,repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,setIndex: freezed == setIndex ? _self.setIndex : setIndex // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

/// @nodoc
mixin _$ExerciseLapData {

 DateTime get startTime; DateTime get endTime; double? get lengthMeters;
/// Create a copy of ExerciseLapData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseLapDataCopyWith<ExerciseLapData> get copyWith => _$ExerciseLapDataCopyWithImpl<ExerciseLapData>(this as ExerciseLapData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseLapData&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.lengthMeters, lengthMeters) || other.lengthMeters == lengthMeters));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,lengthMeters);

@override
String toString() {
  return 'ExerciseLapData(startTime: $startTime, endTime: $endTime, lengthMeters: $lengthMeters)';
}


}

/// @nodoc
abstract mixin class $ExerciseLapDataCopyWith<$Res>  {
  factory $ExerciseLapDataCopyWith(ExerciseLapData value, $Res Function(ExerciseLapData) _then) = _$ExerciseLapDataCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, double? lengthMeters
});




}
/// @nodoc
class _$ExerciseLapDataCopyWithImpl<$Res>
    implements $ExerciseLapDataCopyWith<$Res> {
  _$ExerciseLapDataCopyWithImpl(this._self, this._then);

  final ExerciseLapData _self;
  final $Res Function(ExerciseLapData) _then;

/// Create a copy of ExerciseLapData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? lengthMeters = freezed,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,lengthMeters: freezed == lengthMeters ? _self.lengthMeters : lengthMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [ExerciseLapData].
extension ExerciseLapDataPatterns on ExerciseLapData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ExerciseLapData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseLapData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ExerciseLapData value)  $default,){
final _that = this;
switch (_that) {
case _ExerciseLapData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ExerciseLapData value)?  $default,){
final _that = this;
switch (_that) {
case _ExerciseLapData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? lengthMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseLapData() when $default != null:
return $default(_that.startTime,_that.endTime,_that.lengthMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? lengthMeters)  $default,) {final _that = this;
switch (_that) {
case _ExerciseLapData():
return $default(_that.startTime,_that.endTime,_that.lengthMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  double? lengthMeters)?  $default,) {final _that = this;
switch (_that) {
case _ExerciseLapData() when $default != null:
return $default(_that.startTime,_that.endTime,_that.lengthMeters);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseLapData extends ExerciseLapData {
  const _ExerciseLapData({required this.startTime, required this.endTime, required this.lengthMeters}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  double? lengthMeters;

/// Create a copy of ExerciseLapData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseLapDataCopyWith<_ExerciseLapData> get copyWith => __$ExerciseLapDataCopyWithImpl<_ExerciseLapData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseLapData&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.lengthMeters, lengthMeters) || other.lengthMeters == lengthMeters));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,lengthMeters);

@override
String toString() {
  return 'ExerciseLapData(startTime: $startTime, endTime: $endTime, lengthMeters: $lengthMeters)';
}


}

/// @nodoc
abstract mixin class _$ExerciseLapDataCopyWith<$Res> implements $ExerciseLapDataCopyWith<$Res> {
  factory _$ExerciseLapDataCopyWith(_ExerciseLapData value, $Res Function(_ExerciseLapData) _then) = __$ExerciseLapDataCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, double? lengthMeters
});




}
/// @nodoc
class __$ExerciseLapDataCopyWithImpl<$Res>
    implements _$ExerciseLapDataCopyWith<$Res> {
  __$ExerciseLapDataCopyWithImpl(this._self, this._then);

  final _ExerciseLapData _self;
  final $Res Function(_ExerciseLapData) _then;

/// Create a copy of ExerciseLapData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? lengthMeters = freezed,}) {
  return _then(_ExerciseLapData(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,lengthMeters: freezed == lengthMeters ? _self.lengthMeters : lengthMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$ActivityRecordingLap {

 DateTime get startTime; DateTime get endTime; double? get distanceMeters;
/// Create a copy of ActivityRecordingLap
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingLapCopyWith<ActivityRecordingLap> get copyWith => _$ActivityRecordingLapCopyWithImpl<ActivityRecordingLap>(this as ActivityRecordingLap, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingLap&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,distanceMeters);

@override
String toString() {
  return 'ActivityRecordingLap(startTime: $startTime, endTime: $endTime, distanceMeters: $distanceMeters)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingLapCopyWith<$Res>  {
  factory $ActivityRecordingLapCopyWith(ActivityRecordingLap value, $Res Function(ActivityRecordingLap) _then) = _$ActivityRecordingLapCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, double? distanceMeters
});




}
/// @nodoc
class _$ActivityRecordingLapCopyWithImpl<$Res>
    implements $ActivityRecordingLapCopyWith<$Res> {
  _$ActivityRecordingLapCopyWithImpl(this._self, this._then);

  final ActivityRecordingLap _self;
  final $Res Function(ActivityRecordingLap) _then;

/// Create a copy of ActivityRecordingLap
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? distanceMeters = freezed,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,distanceMeters: freezed == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityRecordingLap].
extension ActivityRecordingLapPatterns on ActivityRecordingLap {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingLap value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingLap() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingLap value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingLap():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingLap value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingLap() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? distanceMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingLap() when $default != null:
return $default(_that.startTime,_that.endTime,_that.distanceMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double? distanceMeters)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingLap():
return $default(_that.startTime,_that.endTime,_that.distanceMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  double? distanceMeters)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingLap() when $default != null:
return $default(_that.startTime,_that.endTime,_that.distanceMeters);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingLap extends ActivityRecordingLap {
  const _ActivityRecordingLap({required this.startTime, required this.endTime, required this.distanceMeters}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  double? distanceMeters;

/// Create a copy of ActivityRecordingLap
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingLapCopyWith<_ActivityRecordingLap> get copyWith => __$ActivityRecordingLapCopyWithImpl<_ActivityRecordingLap>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingLap&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,distanceMeters);

@override
String toString() {
  return 'ActivityRecordingLap(startTime: $startTime, endTime: $endTime, distanceMeters: $distanceMeters)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingLapCopyWith<$Res> implements $ActivityRecordingLapCopyWith<$Res> {
  factory _$ActivityRecordingLapCopyWith(_ActivityRecordingLap value, $Res Function(_ActivityRecordingLap) _then) = __$ActivityRecordingLapCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, double? distanceMeters
});




}
/// @nodoc
class __$ActivityRecordingLapCopyWithImpl<$Res>
    implements _$ActivityRecordingLapCopyWith<$Res> {
  __$ActivityRecordingLapCopyWithImpl(this._self, this._then);

  final _ActivityRecordingLap _self;
  final $Res Function(_ActivityRecordingLap) _then;

/// Create a copy of ActivityRecordingLap
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? distanceMeters = freezed,}) {
  return _then(_ActivityRecordingLap(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,distanceMeters: freezed == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$ActivityRecordingMarker {

 String get id; DateTime get time; double get latitude; double get longitude; double? get altitudeMeters; String get name; String get note; String get type;
/// Create a copy of ActivityRecordingMarker
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityRecordingMarkerCopyWith<ActivityRecordingMarker> get copyWith => _$ActivityRecordingMarkerCopyWithImpl<ActivityRecordingMarker>(this as ActivityRecordingMarker, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityRecordingMarker&&(identical(other.id, id) || other.id == id)&&(identical(other.time, time) || other.time == time)&&(identical(other.latitude, latitude) || other.latitude == latitude)&&(identical(other.longitude, longitude) || other.longitude == longitude)&&(identical(other.altitudeMeters, altitudeMeters) || other.altitudeMeters == altitudeMeters)&&(identical(other.name, name) || other.name == name)&&(identical(other.note, note) || other.note == note)&&(identical(other.type, type) || other.type == type));
}


@override
int get hashCode => Object.hash(runtimeType,id,time,latitude,longitude,altitudeMeters,name,note,type);

@override
String toString() {
  return 'ActivityRecordingMarker(id: $id, time: $time, latitude: $latitude, longitude: $longitude, altitudeMeters: $altitudeMeters, name: $name, note: $note, type: $type)';
}


}

/// @nodoc
abstract mixin class $ActivityRecordingMarkerCopyWith<$Res>  {
  factory $ActivityRecordingMarkerCopyWith(ActivityRecordingMarker value, $Res Function(ActivityRecordingMarker) _then) = _$ActivityRecordingMarkerCopyWithImpl;
@useResult
$Res call({
 String id, DateTime time, double latitude, double longitude, double? altitudeMeters, String name, String note, String type
});




}
/// @nodoc
class _$ActivityRecordingMarkerCopyWithImpl<$Res>
    implements $ActivityRecordingMarkerCopyWith<$Res> {
  _$ActivityRecordingMarkerCopyWithImpl(this._self, this._then);

  final ActivityRecordingMarker _self;
  final $Res Function(ActivityRecordingMarker) _then;

/// Create a copy of ActivityRecordingMarker
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? time = null,Object? latitude = null,Object? longitude = null,Object? altitudeMeters = freezed,Object? name = null,Object? note = null,Object? type = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,latitude: null == latitude ? _self.latitude : latitude // ignore: cast_nullable_to_non_nullable
as double,longitude: null == longitude ? _self.longitude : longitude // ignore: cast_nullable_to_non_nullable
as double,altitudeMeters: freezed == altitudeMeters ? _self.altitudeMeters : altitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,note: null == note ? _self.note : note // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityRecordingMarker].
extension ActivityRecordingMarkerPatterns on ActivityRecordingMarker {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityRecordingMarker value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityRecordingMarker() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityRecordingMarker value)  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingMarker():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityRecordingMarker value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityRecordingMarker() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  String name,  String note,  String type)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityRecordingMarker() when $default != null:
return $default(_that.id,_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.name,_that.note,_that.type);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  String name,  String note,  String type)  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingMarker():
return $default(_that.id,_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.name,_that.note,_that.type);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  String name,  String note,  String type)?  $default,) {final _that = this;
switch (_that) {
case _ActivityRecordingMarker() when $default != null:
return $default(_that.id,_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.name,_that.note,_that.type);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityRecordingMarker implements ActivityRecordingMarker {
  const _ActivityRecordingMarker({required this.id, required this.time, required this.latitude, required this.longitude, required this.altitudeMeters, required this.name, this.note = '', this.type = 'generic'});
  

@override final  String id;
@override final  DateTime time;
@override final  double latitude;
@override final  double longitude;
@override final  double? altitudeMeters;
@override final  String name;
@override@JsonKey() final  String note;
@override@JsonKey() final  String type;

/// Create a copy of ActivityRecordingMarker
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityRecordingMarkerCopyWith<_ActivityRecordingMarker> get copyWith => __$ActivityRecordingMarkerCopyWithImpl<_ActivityRecordingMarker>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityRecordingMarker&&(identical(other.id, id) || other.id == id)&&(identical(other.time, time) || other.time == time)&&(identical(other.latitude, latitude) || other.latitude == latitude)&&(identical(other.longitude, longitude) || other.longitude == longitude)&&(identical(other.altitudeMeters, altitudeMeters) || other.altitudeMeters == altitudeMeters)&&(identical(other.name, name) || other.name == name)&&(identical(other.note, note) || other.note == note)&&(identical(other.type, type) || other.type == type));
}


@override
int get hashCode => Object.hash(runtimeType,id,time,latitude,longitude,altitudeMeters,name,note,type);

@override
String toString() {
  return 'ActivityRecordingMarker(id: $id, time: $time, latitude: $latitude, longitude: $longitude, altitudeMeters: $altitudeMeters, name: $name, note: $note, type: $type)';
}


}

/// @nodoc
abstract mixin class _$ActivityRecordingMarkerCopyWith<$Res> implements $ActivityRecordingMarkerCopyWith<$Res> {
  factory _$ActivityRecordingMarkerCopyWith(_ActivityRecordingMarker value, $Res Function(_ActivityRecordingMarker) _then) = __$ActivityRecordingMarkerCopyWithImpl;
@override @useResult
$Res call({
 String id, DateTime time, double latitude, double longitude, double? altitudeMeters, String name, String note, String type
});




}
/// @nodoc
class __$ActivityRecordingMarkerCopyWithImpl<$Res>
    implements _$ActivityRecordingMarkerCopyWith<$Res> {
  __$ActivityRecordingMarkerCopyWithImpl(this._self, this._then);

  final _ActivityRecordingMarker _self;
  final $Res Function(_ActivityRecordingMarker) _then;

/// Create a copy of ActivityRecordingMarker
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? time = null,Object? latitude = null,Object? longitude = null,Object? altitudeMeters = freezed,Object? name = null,Object? note = null,Object? type = null,}) {
  return _then(_ActivityRecordingMarker(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,latitude: null == latitude ? _self.latitude : latitude // ignore: cast_nullable_to_non_nullable
as double,longitude: null == longitude ? _self.longitude : longitude // ignore: cast_nullable_to_non_nullable
as double,altitudeMeters: freezed == altitudeMeters ? _self.altitudeMeters : altitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,note: null == note ? _self.note : note // ignore: cast_nullable_to_non_nullable
as String,type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$ExerciseRouteData {

 ExerciseRouteStatus get status; List<ExerciseRoutePoint> get points;
/// Create a copy of ExerciseRouteData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseRouteDataCopyWith<ExerciseRouteData> get copyWith => _$ExerciseRouteDataCopyWithImpl<ExerciseRouteData>(this as ExerciseRouteData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseRouteData&&(identical(other.status, status) || other.status == status)&&const DeepCollectionEquality().equals(other.points, points));
}


@override
int get hashCode => Object.hash(runtimeType,status,const DeepCollectionEquality().hash(points));

@override
String toString() {
  return 'ExerciseRouteData(status: $status, points: $points)';
}


}

/// @nodoc
abstract mixin class $ExerciseRouteDataCopyWith<$Res>  {
  factory $ExerciseRouteDataCopyWith(ExerciseRouteData value, $Res Function(ExerciseRouteData) _then) = _$ExerciseRouteDataCopyWithImpl;
@useResult
$Res call({
 ExerciseRouteStatus status, List<ExerciseRoutePoint> points
});




}
/// @nodoc
class _$ExerciseRouteDataCopyWithImpl<$Res>
    implements $ExerciseRouteDataCopyWith<$Res> {
  _$ExerciseRouteDataCopyWithImpl(this._self, this._then);

  final ExerciseRouteData _self;
  final $Res Function(ExerciseRouteData) _then;

/// Create a copy of ExerciseRouteData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? points = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ExerciseRouteStatus,points: null == points ? _self.points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,
  ));
}

}


/// Adds pattern-matching-related methods to [ExerciseRouteData].
extension ExerciseRouteDataPatterns on ExerciseRouteData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ExerciseRouteData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseRouteData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ExerciseRouteData value)  $default,){
final _that = this;
switch (_that) {
case _ExerciseRouteData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ExerciseRouteData value)?  $default,){
final _that = this;
switch (_that) {
case _ExerciseRouteData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ExerciseRouteStatus status,  List<ExerciseRoutePoint> points)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseRouteData() when $default != null:
return $default(_that.status,_that.points);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ExerciseRouteStatus status,  List<ExerciseRoutePoint> points)  $default,) {final _that = this;
switch (_that) {
case _ExerciseRouteData():
return $default(_that.status,_that.points);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ExerciseRouteStatus status,  List<ExerciseRoutePoint> points)?  $default,) {final _that = this;
switch (_that) {
case _ExerciseRouteData() when $default != null:
return $default(_that.status,_that.points);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseRouteData implements ExerciseRouteData {
  const _ExerciseRouteData({this.status = ExerciseRouteStatus.noData, final  List<ExerciseRoutePoint> points = const <ExerciseRoutePoint>[]}): _points = points;
  

@override@JsonKey() final  ExerciseRouteStatus status;
 final  List<ExerciseRoutePoint> _points;
@override@JsonKey() List<ExerciseRoutePoint> get points {
  if (_points is EqualUnmodifiableListView) return _points;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_points);
}


/// Create a copy of ExerciseRouteData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseRouteDataCopyWith<_ExerciseRouteData> get copyWith => __$ExerciseRouteDataCopyWithImpl<_ExerciseRouteData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseRouteData&&(identical(other.status, status) || other.status == status)&&const DeepCollectionEquality().equals(other._points, _points));
}


@override
int get hashCode => Object.hash(runtimeType,status,const DeepCollectionEquality().hash(_points));

@override
String toString() {
  return 'ExerciseRouteData(status: $status, points: $points)';
}


}

/// @nodoc
abstract mixin class _$ExerciseRouteDataCopyWith<$Res> implements $ExerciseRouteDataCopyWith<$Res> {
  factory _$ExerciseRouteDataCopyWith(_ExerciseRouteData value, $Res Function(_ExerciseRouteData) _then) = __$ExerciseRouteDataCopyWithImpl;
@override @useResult
$Res call({
 ExerciseRouteStatus status, List<ExerciseRoutePoint> points
});




}
/// @nodoc
class __$ExerciseRouteDataCopyWithImpl<$Res>
    implements _$ExerciseRouteDataCopyWith<$Res> {
  __$ExerciseRouteDataCopyWithImpl(this._self, this._then);

  final _ExerciseRouteData _self;
  final $Res Function(_ExerciseRouteData) _then;

/// Create a copy of ExerciseRouteData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? points = null,}) {
  return _then(_ExerciseRouteData(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as ExerciseRouteStatus,points: null == points ? _self._points : points // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,
  ));
}


}

/// @nodoc
mixin _$ExerciseRoutePoint {

 DateTime get time; double get latitude; double get longitude; double? get altitudeMeters; double? get horizontalAccuracyMeters; double? get verticalAccuracyMeters;
/// Create a copy of ExerciseRoutePoint
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ExerciseRoutePointCopyWith<ExerciseRoutePoint> get copyWith => _$ExerciseRoutePointCopyWithImpl<ExerciseRoutePoint>(this as ExerciseRoutePoint, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ExerciseRoutePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.latitude, latitude) || other.latitude == latitude)&&(identical(other.longitude, longitude) || other.longitude == longitude)&&(identical(other.altitudeMeters, altitudeMeters) || other.altitudeMeters == altitudeMeters)&&(identical(other.horizontalAccuracyMeters, horizontalAccuracyMeters) || other.horizontalAccuracyMeters == horizontalAccuracyMeters)&&(identical(other.verticalAccuracyMeters, verticalAccuracyMeters) || other.verticalAccuracyMeters == verticalAccuracyMeters));
}


@override
int get hashCode => Object.hash(runtimeType,time,latitude,longitude,altitudeMeters,horizontalAccuracyMeters,verticalAccuracyMeters);

@override
String toString() {
  return 'ExerciseRoutePoint(time: $time, latitude: $latitude, longitude: $longitude, altitudeMeters: $altitudeMeters, horizontalAccuracyMeters: $horizontalAccuracyMeters, verticalAccuracyMeters: $verticalAccuracyMeters)';
}


}

/// @nodoc
abstract mixin class $ExerciseRoutePointCopyWith<$Res>  {
  factory $ExerciseRoutePointCopyWith(ExerciseRoutePoint value, $Res Function(ExerciseRoutePoint) _then) = _$ExerciseRoutePointCopyWithImpl;
@useResult
$Res call({
 DateTime time, double latitude, double longitude, double? altitudeMeters, double? horizontalAccuracyMeters, double? verticalAccuracyMeters
});




}
/// @nodoc
class _$ExerciseRoutePointCopyWithImpl<$Res>
    implements $ExerciseRoutePointCopyWith<$Res> {
  _$ExerciseRoutePointCopyWithImpl(this._self, this._then);

  final ExerciseRoutePoint _self;
  final $Res Function(ExerciseRoutePoint) _then;

/// Create a copy of ExerciseRoutePoint
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? latitude = null,Object? longitude = null,Object? altitudeMeters = freezed,Object? horizontalAccuracyMeters = freezed,Object? verticalAccuracyMeters = freezed,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,latitude: null == latitude ? _self.latitude : latitude // ignore: cast_nullable_to_non_nullable
as double,longitude: null == longitude ? _self.longitude : longitude // ignore: cast_nullable_to_non_nullable
as double,altitudeMeters: freezed == altitudeMeters ? _self.altitudeMeters : altitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,horizontalAccuracyMeters: freezed == horizontalAccuracyMeters ? _self.horizontalAccuracyMeters : horizontalAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,verticalAccuracyMeters: freezed == verticalAccuracyMeters ? _self.verticalAccuracyMeters : verticalAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [ExerciseRoutePoint].
extension ExerciseRoutePointPatterns on ExerciseRoutePoint {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ExerciseRoutePoint value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ExerciseRoutePoint() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ExerciseRoutePoint value)  $default,){
final _that = this;
switch (_that) {
case _ExerciseRoutePoint():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ExerciseRoutePoint value)?  $default,){
final _that = this;
switch (_that) {
case _ExerciseRoutePoint() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  double? horizontalAccuracyMeters,  double? verticalAccuracyMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ExerciseRoutePoint() when $default != null:
return $default(_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.horizontalAccuracyMeters,_that.verticalAccuracyMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  double? horizontalAccuracyMeters,  double? verticalAccuracyMeters)  $default,) {final _that = this;
switch (_that) {
case _ExerciseRoutePoint():
return $default(_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.horizontalAccuracyMeters,_that.verticalAccuracyMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double latitude,  double longitude,  double? altitudeMeters,  double? horizontalAccuracyMeters,  double? verticalAccuracyMeters)?  $default,) {final _that = this;
switch (_that) {
case _ExerciseRoutePoint() when $default != null:
return $default(_that.time,_that.latitude,_that.longitude,_that.altitudeMeters,_that.horizontalAccuracyMeters,_that.verticalAccuracyMeters);case _:
  return null;

}
}

}

/// @nodoc


class _ExerciseRoutePoint implements ExerciseRoutePoint {
  const _ExerciseRoutePoint({required this.time, required this.latitude, required this.longitude, required this.altitudeMeters, required this.horizontalAccuracyMeters, required this.verticalAccuracyMeters});
  

@override final  DateTime time;
@override final  double latitude;
@override final  double longitude;
@override final  double? altitudeMeters;
@override final  double? horizontalAccuracyMeters;
@override final  double? verticalAccuracyMeters;

/// Create a copy of ExerciseRoutePoint
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ExerciseRoutePointCopyWith<_ExerciseRoutePoint> get copyWith => __$ExerciseRoutePointCopyWithImpl<_ExerciseRoutePoint>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ExerciseRoutePoint&&(identical(other.time, time) || other.time == time)&&(identical(other.latitude, latitude) || other.latitude == latitude)&&(identical(other.longitude, longitude) || other.longitude == longitude)&&(identical(other.altitudeMeters, altitudeMeters) || other.altitudeMeters == altitudeMeters)&&(identical(other.horizontalAccuracyMeters, horizontalAccuracyMeters) || other.horizontalAccuracyMeters == horizontalAccuracyMeters)&&(identical(other.verticalAccuracyMeters, verticalAccuracyMeters) || other.verticalAccuracyMeters == verticalAccuracyMeters));
}


@override
int get hashCode => Object.hash(runtimeType,time,latitude,longitude,altitudeMeters,horizontalAccuracyMeters,verticalAccuracyMeters);

@override
String toString() {
  return 'ExerciseRoutePoint(time: $time, latitude: $latitude, longitude: $longitude, altitudeMeters: $altitudeMeters, horizontalAccuracyMeters: $horizontalAccuracyMeters, verticalAccuracyMeters: $verticalAccuracyMeters)';
}


}

/// @nodoc
abstract mixin class _$ExerciseRoutePointCopyWith<$Res> implements $ExerciseRoutePointCopyWith<$Res> {
  factory _$ExerciseRoutePointCopyWith(_ExerciseRoutePoint value, $Res Function(_ExerciseRoutePoint) _then) = __$ExerciseRoutePointCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double latitude, double longitude, double? altitudeMeters, double? horizontalAccuracyMeters, double? verticalAccuracyMeters
});




}
/// @nodoc
class __$ExerciseRoutePointCopyWithImpl<$Res>
    implements _$ExerciseRoutePointCopyWith<$Res> {
  __$ExerciseRoutePointCopyWithImpl(this._self, this._then);

  final _ExerciseRoutePoint _self;
  final $Res Function(_ExerciseRoutePoint) _then;

/// Create a copy of ExerciseRoutePoint
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? latitude = null,Object? longitude = null,Object? altitudeMeters = freezed,Object? horizontalAccuracyMeters = freezed,Object? verticalAccuracyMeters = freezed,}) {
  return _then(_ExerciseRoutePoint(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,latitude: null == latitude ? _self.latitude : latitude // ignore: cast_nullable_to_non_nullable
as double,longitude: null == longitude ? _self.longitude : longitude // ignore: cast_nullable_to_non_nullable
as double,altitudeMeters: freezed == altitudeMeters ? _self.altitudeMeters : altitudeMeters // ignore: cast_nullable_to_non_nullable
as double?,horizontalAccuracyMeters: freezed == horizontalAccuracyMeters ? _self.horizontalAccuracyMeters : horizontalAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,verticalAccuracyMeters: freezed == verticalAccuracyMeters ? _self.verticalAccuracyMeters : verticalAccuracyMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$ActivityPauseInterval {

 DateTime get startTime; DateTime get endTime;
/// Create a copy of ActivityPauseInterval
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityPauseIntervalCopyWith<ActivityPauseInterval> get copyWith => _$ActivityPauseIntervalCopyWithImpl<ActivityPauseInterval>(this as ActivityPauseInterval, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityPauseInterval&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime);

@override
String toString() {
  return 'ActivityPauseInterval(startTime: $startTime, endTime: $endTime)';
}


}

/// @nodoc
abstract mixin class $ActivityPauseIntervalCopyWith<$Res>  {
  factory $ActivityPauseIntervalCopyWith(ActivityPauseInterval value, $Res Function(ActivityPauseInterval) _then) = _$ActivityPauseIntervalCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime
});




}
/// @nodoc
class _$ActivityPauseIntervalCopyWithImpl<$Res>
    implements $ActivityPauseIntervalCopyWith<$Res> {
  _$ActivityPauseIntervalCopyWithImpl(this._self, this._then);

  final ActivityPauseInterval _self;
  final $Res Function(ActivityPauseInterval) _then;

/// Create a copy of ActivityPauseInterval
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityPauseInterval].
extension ActivityPauseIntervalPatterns on ActivityPauseInterval {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityPauseInterval value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityPauseInterval() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityPauseInterval value)  $default,){
final _that = this;
switch (_that) {
case _ActivityPauseInterval():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityPauseInterval value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityPauseInterval() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityPauseInterval() when $default != null:
return $default(_that.startTime,_that.endTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime)  $default,) {final _that = this;
switch (_that) {
case _ActivityPauseInterval():
return $default(_that.startTime,_that.endTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime)?  $default,) {final _that = this;
switch (_that) {
case _ActivityPauseInterval() when $default != null:
return $default(_that.startTime,_that.endTime);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityPauseInterval implements ActivityPauseInterval {
  const _ActivityPauseInterval({required this.startTime, required this.endTime});
  

@override final  DateTime startTime;
@override final  DateTime endTime;

/// Create a copy of ActivityPauseInterval
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityPauseIntervalCopyWith<_ActivityPauseInterval> get copyWith => __$ActivityPauseIntervalCopyWithImpl<_ActivityPauseInterval>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityPauseInterval&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime);

@override
String toString() {
  return 'ActivityPauseInterval(startTime: $startTime, endTime: $endTime)';
}


}

/// @nodoc
abstract mixin class _$ActivityPauseIntervalCopyWith<$Res> implements $ActivityPauseIntervalCopyWith<$Res> {
  factory _$ActivityPauseIntervalCopyWith(_ActivityPauseInterval value, $Res Function(_ActivityPauseInterval) _then) = __$ActivityPauseIntervalCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime
});




}
/// @nodoc
class __$ActivityPauseIntervalCopyWithImpl<$Res>
    implements _$ActivityPauseIntervalCopyWith<$Res> {
  __$ActivityPauseIntervalCopyWithImpl(this._self, this._then);

  final _ActivityPauseInterval _self;
  final $Res Function(_ActivityPauseInterval) _then;

/// Create a copy of ActivityPauseInterval
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,}) {
  return _then(_ActivityPauseInterval(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}

/// @nodoc
mixin _$ActivityExerciseSegmentWrite {

 DateTime get startTime; DateTime get endTime; int get segmentType; int get repetitions; int? get setIndex;
/// Create a copy of ActivityExerciseSegmentWrite
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityExerciseSegmentWriteCopyWith<ActivityExerciseSegmentWrite> get copyWith => _$ActivityExerciseSegmentWriteCopyWithImpl<ActivityExerciseSegmentWrite>(this as ActivityExerciseSegmentWrite, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityExerciseSegmentWrite&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.segmentType, segmentType) || other.segmentType == segmentType)&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.setIndex, setIndex) || other.setIndex == setIndex));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,segmentType,repetitions,setIndex);

@override
String toString() {
  return 'ActivityExerciseSegmentWrite(startTime: $startTime, endTime: $endTime, segmentType: $segmentType, repetitions: $repetitions, setIndex: $setIndex)';
}


}

/// @nodoc
abstract mixin class $ActivityExerciseSegmentWriteCopyWith<$Res>  {
  factory $ActivityExerciseSegmentWriteCopyWith(ActivityExerciseSegmentWrite value, $Res Function(ActivityExerciseSegmentWrite) _then) = _$ActivityExerciseSegmentWriteCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, int segmentType, int repetitions, int? setIndex
});




}
/// @nodoc
class _$ActivityExerciseSegmentWriteCopyWithImpl<$Res>
    implements $ActivityExerciseSegmentWriteCopyWith<$Res> {
  _$ActivityExerciseSegmentWriteCopyWithImpl(this._self, this._then);

  final ActivityExerciseSegmentWrite _self;
  final $Res Function(ActivityExerciseSegmentWrite) _then;

/// Create a copy of ActivityExerciseSegmentWrite
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? segmentType = null,Object? repetitions = null,Object? setIndex = freezed,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,segmentType: null == segmentType ? _self.segmentType : segmentType // ignore: cast_nullable_to_non_nullable
as int,repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,setIndex: freezed == setIndex ? _self.setIndex : setIndex // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityExerciseSegmentWrite].
extension ActivityExerciseSegmentWritePatterns on ActivityExerciseSegmentWrite {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityExerciseSegmentWrite value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityExerciseSegmentWrite value)  $default,){
final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityExerciseSegmentWrite value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite() when $default != null:
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)  $default,) {final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite():
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  int segmentType,  int repetitions,  int? setIndex)?  $default,) {final _that = this;
switch (_that) {
case _ActivityExerciseSegmentWrite() when $default != null:
return $default(_that.startTime,_that.endTime,_that.segmentType,_that.repetitions,_that.setIndex);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityExerciseSegmentWrite implements ActivityExerciseSegmentWrite {
  const _ActivityExerciseSegmentWrite({required this.startTime, required this.endTime, required this.segmentType, this.repetitions = 0, this.setIndex});
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int segmentType;
@override@JsonKey() final  int repetitions;
@override final  int? setIndex;

/// Create a copy of ActivityExerciseSegmentWrite
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityExerciseSegmentWriteCopyWith<_ActivityExerciseSegmentWrite> get copyWith => __$ActivityExerciseSegmentWriteCopyWithImpl<_ActivityExerciseSegmentWrite>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityExerciseSegmentWrite&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.segmentType, segmentType) || other.segmentType == segmentType)&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.setIndex, setIndex) || other.setIndex == setIndex));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,segmentType,repetitions,setIndex);

@override
String toString() {
  return 'ActivityExerciseSegmentWrite(startTime: $startTime, endTime: $endTime, segmentType: $segmentType, repetitions: $repetitions, setIndex: $setIndex)';
}


}

/// @nodoc
abstract mixin class _$ActivityExerciseSegmentWriteCopyWith<$Res> implements $ActivityExerciseSegmentWriteCopyWith<$Res> {
  factory _$ActivityExerciseSegmentWriteCopyWith(_ActivityExerciseSegmentWrite value, $Res Function(_ActivityExerciseSegmentWrite) _then) = __$ActivityExerciseSegmentWriteCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, int segmentType, int repetitions, int? setIndex
});




}
/// @nodoc
class __$ActivityExerciseSegmentWriteCopyWithImpl<$Res>
    implements _$ActivityExerciseSegmentWriteCopyWith<$Res> {
  __$ActivityExerciseSegmentWriteCopyWithImpl(this._self, this._then);

  final _ActivityExerciseSegmentWrite _self;
  final $Res Function(_ActivityExerciseSegmentWrite) _then;

/// Create a copy of ActivityExerciseSegmentWrite
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? segmentType = null,Object? repetitions = null,Object? setIndex = freezed,}) {
  return _then(_ActivityExerciseSegmentWrite(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,segmentType: null == segmentType ? _self.segmentType : segmentType // ignore: cast_nullable_to_non_nullable
as int,repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,setIndex: freezed == setIndex ? _self.setIndex : setIndex // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

/// @nodoc
mixin _$ActivityWriteRequest {

 int get exerciseType; DateTime get startTime; DateTime get endTime; String? get title; String? get notes; String? get plannedExerciseSessionId; List<ExerciseRoutePoint> get routePoints; List<ActivityPauseInterval> get pauseIntervals; List<ExerciseLapData> get laps; List<ActivityExerciseSegmentWrite> get exerciseSegments; int? get stepsCount; double? get distanceMeters; double? get elevationGainedMeters; double? get activeCaloriesKcal; double? get totalCaloriesKcal; BleRecordingSampleBuffer get bleSamples;
/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityWriteRequestCopyWith<ActivityWriteRequest> get copyWith => _$ActivityWriteRequestCopyWithImpl<ActivityWriteRequest>(this as ActivityWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityWriteRequest&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.plannedExerciseSessionId, plannedExerciseSessionId) || other.plannedExerciseSessionId == plannedExerciseSessionId)&&const DeepCollectionEquality().equals(other.routePoints, routePoints)&&const DeepCollectionEquality().equals(other.pauseIntervals, pauseIntervals)&&const DeepCollectionEquality().equals(other.laps, laps)&&const DeepCollectionEquality().equals(other.exerciseSegments, exerciseSegments)&&(identical(other.stepsCount, stepsCount) || other.stepsCount == stepsCount)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.totalCaloriesKcal, totalCaloriesKcal) || other.totalCaloriesKcal == totalCaloriesKcal)&&(identical(other.bleSamples, bleSamples) || other.bleSamples == bleSamples));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,startTime,endTime,title,notes,plannedExerciseSessionId,const DeepCollectionEquality().hash(routePoints),const DeepCollectionEquality().hash(pauseIntervals),const DeepCollectionEquality().hash(laps),const DeepCollectionEquality().hash(exerciseSegments),stepsCount,distanceMeters,elevationGainedMeters,activeCaloriesKcal,totalCaloriesKcal,bleSamples);

@override
String toString() {
  return 'ActivityWriteRequest(exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, title: $title, notes: $notes, plannedExerciseSessionId: $plannedExerciseSessionId, routePoints: $routePoints, pauseIntervals: $pauseIntervals, laps: $laps, exerciseSegments: $exerciseSegments, stepsCount: $stepsCount, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, activeCaloriesKcal: $activeCaloriesKcal, totalCaloriesKcal: $totalCaloriesKcal, bleSamples: $bleSamples)';
}


}

/// @nodoc
abstract mixin class $ActivityWriteRequestCopyWith<$Res>  {
  factory $ActivityWriteRequestCopyWith(ActivityWriteRequest value, $Res Function(ActivityWriteRequest) _then) = _$ActivityWriteRequestCopyWithImpl;
@useResult
$Res call({
 int exerciseType, DateTime startTime, DateTime endTime, String? title, String? notes, String? plannedExerciseSessionId, List<ExerciseRoutePoint> routePoints, List<ActivityPauseInterval> pauseIntervals, List<ExerciseLapData> laps, List<ActivityExerciseSegmentWrite> exerciseSegments, int? stepsCount, double? distanceMeters, double? elevationGainedMeters, double? activeCaloriesKcal, double? totalCaloriesKcal, BleRecordingSampleBuffer bleSamples
});


$BleRecordingSampleBufferCopyWith<$Res> get bleSamples;

}
/// @nodoc
class _$ActivityWriteRequestCopyWithImpl<$Res>
    implements $ActivityWriteRequestCopyWith<$Res> {
  _$ActivityWriteRequestCopyWithImpl(this._self, this._then);

  final ActivityWriteRequest _self;
  final $Res Function(ActivityWriteRequest) _then;

/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? title = freezed,Object? notes = freezed,Object? plannedExerciseSessionId = freezed,Object? routePoints = null,Object? pauseIntervals = null,Object? laps = null,Object? exerciseSegments = null,Object? stepsCount = freezed,Object? distanceMeters = freezed,Object? elevationGainedMeters = freezed,Object? activeCaloriesKcal = freezed,Object? totalCaloriesKcal = freezed,Object? bleSamples = null,}) {
  return _then(_self.copyWith(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,plannedExerciseSessionId: freezed == plannedExerciseSessionId ? _self.plannedExerciseSessionId : plannedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,routePoints: null == routePoints ? _self.routePoints : routePoints // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,pauseIntervals: null == pauseIntervals ? _self.pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,laps: null == laps ? _self.laps : laps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,exerciseSegments: null == exerciseSegments ? _self.exerciseSegments : exerciseSegments // ignore: cast_nullable_to_non_nullable
as List<ActivityExerciseSegmentWrite>,stepsCount: freezed == stepsCount ? _self.stepsCount : stepsCount // ignore: cast_nullable_to_non_nullable
as int?,distanceMeters: freezed == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesKcal: freezed == totalCaloriesKcal ? _self.totalCaloriesKcal : totalCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,bleSamples: null == bleSamples ? _self.bleSamples : bleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,
  ));
}
/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get bleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.bleSamples, (value) {
    return _then(_self.copyWith(bleSamples: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityWriteRequest].
extension ActivityWriteRequestPatterns on ActivityWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _ActivityWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  String? plannedExerciseSessionId,  List<ExerciseRoutePoint> routePoints,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseLapData> laps,  List<ActivityExerciseSegmentWrite> exerciseSegments,  int? stepsCount,  double? distanceMeters,  double? elevationGainedMeters,  double? activeCaloriesKcal,  double? totalCaloriesKcal,  BleRecordingSampleBuffer bleSamples)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityWriteRequest() when $default != null:
return $default(_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.plannedExerciseSessionId,_that.routePoints,_that.pauseIntervals,_that.laps,_that.exerciseSegments,_that.stepsCount,_that.distanceMeters,_that.elevationGainedMeters,_that.activeCaloriesKcal,_that.totalCaloriesKcal,_that.bleSamples);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  String? plannedExerciseSessionId,  List<ExerciseRoutePoint> routePoints,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseLapData> laps,  List<ActivityExerciseSegmentWrite> exerciseSegments,  int? stepsCount,  double? distanceMeters,  double? elevationGainedMeters,  double? activeCaloriesKcal,  double? totalCaloriesKcal,  BleRecordingSampleBuffer bleSamples)  $default,) {final _that = this;
switch (_that) {
case _ActivityWriteRequest():
return $default(_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.plannedExerciseSessionId,_that.routePoints,_that.pauseIntervals,_that.laps,_that.exerciseSegments,_that.stepsCount,_that.distanceMeters,_that.elevationGainedMeters,_that.activeCaloriesKcal,_that.totalCaloriesKcal,_that.bleSamples);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  String? plannedExerciseSessionId,  List<ExerciseRoutePoint> routePoints,  List<ActivityPauseInterval> pauseIntervals,  List<ExerciseLapData> laps,  List<ActivityExerciseSegmentWrite> exerciseSegments,  int? stepsCount,  double? distanceMeters,  double? elevationGainedMeters,  double? activeCaloriesKcal,  double? totalCaloriesKcal,  BleRecordingSampleBuffer bleSamples)?  $default,) {final _that = this;
switch (_that) {
case _ActivityWriteRequest() when $default != null:
return $default(_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.plannedExerciseSessionId,_that.routePoints,_that.pauseIntervals,_that.laps,_that.exerciseSegments,_that.stepsCount,_that.distanceMeters,_that.elevationGainedMeters,_that.activeCaloriesKcal,_that.totalCaloriesKcal,_that.bleSamples);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityWriteRequest implements ActivityWriteRequest {
  const _ActivityWriteRequest({required this.exerciseType, required this.startTime, required this.endTime, this.title, this.notes, this.plannedExerciseSessionId, final  List<ExerciseRoutePoint> routePoints = const <ExerciseRoutePoint>[], final  List<ActivityPauseInterval> pauseIntervals = const <ActivityPauseInterval>[], final  List<ExerciseLapData> laps = const <ExerciseLapData>[], final  List<ActivityExerciseSegmentWrite> exerciseSegments = const <ActivityExerciseSegmentWrite>[], this.stepsCount, this.distanceMeters, this.elevationGainedMeters, this.activeCaloriesKcal, this.totalCaloriesKcal, this.bleSamples = const BleRecordingSampleBuffer()}): _routePoints = routePoints,_pauseIntervals = pauseIntervals,_laps = laps,_exerciseSegments = exerciseSegments;
  

@override final  int exerciseType;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  String? title;
@override final  String? notes;
@override final  String? plannedExerciseSessionId;
 final  List<ExerciseRoutePoint> _routePoints;
@override@JsonKey() List<ExerciseRoutePoint> get routePoints {
  if (_routePoints is EqualUnmodifiableListView) return _routePoints;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_routePoints);
}

 final  List<ActivityPauseInterval> _pauseIntervals;
@override@JsonKey() List<ActivityPauseInterval> get pauseIntervals {
  if (_pauseIntervals is EqualUnmodifiableListView) return _pauseIntervals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_pauseIntervals);
}

 final  List<ExerciseLapData> _laps;
@override@JsonKey() List<ExerciseLapData> get laps {
  if (_laps is EqualUnmodifiableListView) return _laps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_laps);
}

 final  List<ActivityExerciseSegmentWrite> _exerciseSegments;
@override@JsonKey() List<ActivityExerciseSegmentWrite> get exerciseSegments {
  if (_exerciseSegments is EqualUnmodifiableListView) return _exerciseSegments;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_exerciseSegments);
}

@override final  int? stepsCount;
@override final  double? distanceMeters;
@override final  double? elevationGainedMeters;
@override final  double? activeCaloriesKcal;
@override final  double? totalCaloriesKcal;
@override@JsonKey() final  BleRecordingSampleBuffer bleSamples;

/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityWriteRequestCopyWith<_ActivityWriteRequest> get copyWith => __$ActivityWriteRequestCopyWithImpl<_ActivityWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityWriteRequest&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.plannedExerciseSessionId, plannedExerciseSessionId) || other.plannedExerciseSessionId == plannedExerciseSessionId)&&const DeepCollectionEquality().equals(other._routePoints, _routePoints)&&const DeepCollectionEquality().equals(other._pauseIntervals, _pauseIntervals)&&const DeepCollectionEquality().equals(other._laps, _laps)&&const DeepCollectionEquality().equals(other._exerciseSegments, _exerciseSegments)&&(identical(other.stepsCount, stepsCount) || other.stepsCount == stepsCount)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.totalCaloriesKcal, totalCaloriesKcal) || other.totalCaloriesKcal == totalCaloriesKcal)&&(identical(other.bleSamples, bleSamples) || other.bleSamples == bleSamples));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,startTime,endTime,title,notes,plannedExerciseSessionId,const DeepCollectionEquality().hash(_routePoints),const DeepCollectionEquality().hash(_pauseIntervals),const DeepCollectionEquality().hash(_laps),const DeepCollectionEquality().hash(_exerciseSegments),stepsCount,distanceMeters,elevationGainedMeters,activeCaloriesKcal,totalCaloriesKcal,bleSamples);

@override
String toString() {
  return 'ActivityWriteRequest(exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, title: $title, notes: $notes, plannedExerciseSessionId: $plannedExerciseSessionId, routePoints: $routePoints, pauseIntervals: $pauseIntervals, laps: $laps, exerciseSegments: $exerciseSegments, stepsCount: $stepsCount, distanceMeters: $distanceMeters, elevationGainedMeters: $elevationGainedMeters, activeCaloriesKcal: $activeCaloriesKcal, totalCaloriesKcal: $totalCaloriesKcal, bleSamples: $bleSamples)';
}


}

/// @nodoc
abstract mixin class _$ActivityWriteRequestCopyWith<$Res> implements $ActivityWriteRequestCopyWith<$Res> {
  factory _$ActivityWriteRequestCopyWith(_ActivityWriteRequest value, $Res Function(_ActivityWriteRequest) _then) = __$ActivityWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 int exerciseType, DateTime startTime, DateTime endTime, String? title, String? notes, String? plannedExerciseSessionId, List<ExerciseRoutePoint> routePoints, List<ActivityPauseInterval> pauseIntervals, List<ExerciseLapData> laps, List<ActivityExerciseSegmentWrite> exerciseSegments, int? stepsCount, double? distanceMeters, double? elevationGainedMeters, double? activeCaloriesKcal, double? totalCaloriesKcal, BleRecordingSampleBuffer bleSamples
});


@override $BleRecordingSampleBufferCopyWith<$Res> get bleSamples;

}
/// @nodoc
class __$ActivityWriteRequestCopyWithImpl<$Res>
    implements _$ActivityWriteRequestCopyWith<$Res> {
  __$ActivityWriteRequestCopyWithImpl(this._self, this._then);

  final _ActivityWriteRequest _self;
  final $Res Function(_ActivityWriteRequest) _then;

/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? title = freezed,Object? notes = freezed,Object? plannedExerciseSessionId = freezed,Object? routePoints = null,Object? pauseIntervals = null,Object? laps = null,Object? exerciseSegments = null,Object? stepsCount = freezed,Object? distanceMeters = freezed,Object? elevationGainedMeters = freezed,Object? activeCaloriesKcal = freezed,Object? totalCaloriesKcal = freezed,Object? bleSamples = null,}) {
  return _then(_ActivityWriteRequest(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,plannedExerciseSessionId: freezed == plannedExerciseSessionId ? _self.plannedExerciseSessionId : plannedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,routePoints: null == routePoints ? _self._routePoints : routePoints // ignore: cast_nullable_to_non_nullable
as List<ExerciseRoutePoint>,pauseIntervals: null == pauseIntervals ? _self._pauseIntervals : pauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,laps: null == laps ? _self._laps : laps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,exerciseSegments: null == exerciseSegments ? _self._exerciseSegments : exerciseSegments // ignore: cast_nullable_to_non_nullable
as List<ActivityExerciseSegmentWrite>,stepsCount: freezed == stepsCount ? _self.stepsCount : stepsCount // ignore: cast_nullable_to_non_nullable
as int?,distanceMeters: freezed == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesKcal: freezed == totalCaloriesKcal ? _self.totalCaloriesKcal : totalCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,bleSamples: null == bleSamples ? _self.bleSamples : bleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,
  ));
}

/// Create a copy of ActivityWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get bleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.bleSamples, (value) {
    return _then(_self.copyWith(bleSamples: value));
  });
}
}

/// @nodoc
mixin _$PlannedExerciseData {

 String get id; String? get title; int get exerciseType; DateTime get startTime; DateTime get endTime; bool get hasExplicitTime; String? get completedExerciseSessionId; String? get notes; int get blockCount; String get source; List<PlannedExerciseBlockData> get blocks;
/// Create a copy of PlannedExerciseData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PlannedExerciseDataCopyWith<PlannedExerciseData> get copyWith => _$PlannedExerciseDataCopyWithImpl<PlannedExerciseData>(this as PlannedExerciseData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PlannedExerciseData&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.hasExplicitTime, hasExplicitTime) || other.hasExplicitTime == hasExplicitTime)&&(identical(other.completedExerciseSessionId, completedExerciseSessionId) || other.completedExerciseSessionId == completedExerciseSessionId)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.blockCount, blockCount) || other.blockCount == blockCount)&&(identical(other.source, source) || other.source == source)&&const DeepCollectionEquality().equals(other.blocks, blocks));
}


@override
int get hashCode => Object.hash(runtimeType,id,title,exerciseType,startTime,endTime,hasExplicitTime,completedExerciseSessionId,notes,blockCount,source,const DeepCollectionEquality().hash(blocks));

@override
String toString() {
  return 'PlannedExerciseData(id: $id, title: $title, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, hasExplicitTime: $hasExplicitTime, completedExerciseSessionId: $completedExerciseSessionId, notes: $notes, blockCount: $blockCount, source: $source, blocks: $blocks)';
}


}

/// @nodoc
abstract mixin class $PlannedExerciseDataCopyWith<$Res>  {
  factory $PlannedExerciseDataCopyWith(PlannedExerciseData value, $Res Function(PlannedExerciseData) _then) = _$PlannedExerciseDataCopyWithImpl;
@useResult
$Res call({
 String id, String? title, int exerciseType, DateTime startTime, DateTime endTime, bool hasExplicitTime, String? completedExerciseSessionId, String? notes, int blockCount, String source, List<PlannedExerciseBlockData> blocks
});




}
/// @nodoc
class _$PlannedExerciseDataCopyWithImpl<$Res>
    implements $PlannedExerciseDataCopyWith<$Res> {
  _$PlannedExerciseDataCopyWithImpl(this._self, this._then);

  final PlannedExerciseData _self;
  final $Res Function(PlannedExerciseData) _then;

/// Create a copy of PlannedExerciseData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? title = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? hasExplicitTime = null,Object? completedExerciseSessionId = freezed,Object? notes = freezed,Object? blockCount = null,Object? source = null,Object? blocks = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,hasExplicitTime: null == hasExplicitTime ? _self.hasExplicitTime : hasExplicitTime // ignore: cast_nullable_to_non_nullable
as bool,completedExerciseSessionId: freezed == completedExerciseSessionId ? _self.completedExerciseSessionId : completedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,blockCount: null == blockCount ? _self.blockCount : blockCount // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,blocks: null == blocks ? _self.blocks : blocks // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseBlockData>,
  ));
}

}


/// Adds pattern-matching-related methods to [PlannedExerciseData].
extension PlannedExerciseDataPatterns on PlannedExerciseData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PlannedExerciseData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PlannedExerciseData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PlannedExerciseData value)  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PlannedExerciseData value)?  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  bool hasExplicitTime,  String? completedExerciseSessionId,  String? notes,  int blockCount,  String source,  List<PlannedExerciseBlockData> blocks)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PlannedExerciseData() when $default != null:
return $default(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.hasExplicitTime,_that.completedExerciseSessionId,_that.notes,_that.blockCount,_that.source,_that.blocks);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  bool hasExplicitTime,  String? completedExerciseSessionId,  String? notes,  int blockCount,  String source,  List<PlannedExerciseBlockData> blocks)  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseData():
return $default(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.hasExplicitTime,_that.completedExerciseSessionId,_that.notes,_that.blockCount,_that.source,_that.blocks);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String? title,  int exerciseType,  DateTime startTime,  DateTime endTime,  bool hasExplicitTime,  String? completedExerciseSessionId,  String? notes,  int blockCount,  String source,  List<PlannedExerciseBlockData> blocks)?  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseData() when $default != null:
return $default(_that.id,_that.title,_that.exerciseType,_that.startTime,_that.endTime,_that.hasExplicitTime,_that.completedExerciseSessionId,_that.notes,_that.blockCount,_that.source,_that.blocks);case _:
  return null;

}
}

}

/// @nodoc


class _PlannedExerciseData extends PlannedExerciseData {
  const _PlannedExerciseData({required this.id, required this.title, required this.exerciseType, required this.startTime, required this.endTime, required this.hasExplicitTime, required this.completedExerciseSessionId, required this.notes, required this.blockCount, required this.source, final  List<PlannedExerciseBlockData> blocks = const <PlannedExerciseBlockData>[]}): _blocks = blocks,super._();
  

@override final  String id;
@override final  String? title;
@override final  int exerciseType;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  bool hasExplicitTime;
@override final  String? completedExerciseSessionId;
@override final  String? notes;
@override final  int blockCount;
@override final  String source;
 final  List<PlannedExerciseBlockData> _blocks;
@override@JsonKey() List<PlannedExerciseBlockData> get blocks {
  if (_blocks is EqualUnmodifiableListView) return _blocks;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_blocks);
}


/// Create a copy of PlannedExerciseData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PlannedExerciseDataCopyWith<_PlannedExerciseData> get copyWith => __$PlannedExerciseDataCopyWithImpl<_PlannedExerciseData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PlannedExerciseData&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.hasExplicitTime, hasExplicitTime) || other.hasExplicitTime == hasExplicitTime)&&(identical(other.completedExerciseSessionId, completedExerciseSessionId) || other.completedExerciseSessionId == completedExerciseSessionId)&&(identical(other.notes, notes) || other.notes == notes)&&(identical(other.blockCount, blockCount) || other.blockCount == blockCount)&&(identical(other.source, source) || other.source == source)&&const DeepCollectionEquality().equals(other._blocks, _blocks));
}


@override
int get hashCode => Object.hash(runtimeType,id,title,exerciseType,startTime,endTime,hasExplicitTime,completedExerciseSessionId,notes,blockCount,source,const DeepCollectionEquality().hash(_blocks));

@override
String toString() {
  return 'PlannedExerciseData(id: $id, title: $title, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, hasExplicitTime: $hasExplicitTime, completedExerciseSessionId: $completedExerciseSessionId, notes: $notes, blockCount: $blockCount, source: $source, blocks: $blocks)';
}


}

/// @nodoc
abstract mixin class _$PlannedExerciseDataCopyWith<$Res> implements $PlannedExerciseDataCopyWith<$Res> {
  factory _$PlannedExerciseDataCopyWith(_PlannedExerciseData value, $Res Function(_PlannedExerciseData) _then) = __$PlannedExerciseDataCopyWithImpl;
@override @useResult
$Res call({
 String id, String? title, int exerciseType, DateTime startTime, DateTime endTime, bool hasExplicitTime, String? completedExerciseSessionId, String? notes, int blockCount, String source, List<PlannedExerciseBlockData> blocks
});




}
/// @nodoc
class __$PlannedExerciseDataCopyWithImpl<$Res>
    implements _$PlannedExerciseDataCopyWith<$Res> {
  __$PlannedExerciseDataCopyWithImpl(this._self, this._then);

  final _PlannedExerciseData _self;
  final $Res Function(_PlannedExerciseData) _then;

/// Create a copy of PlannedExerciseData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? title = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? hasExplicitTime = null,Object? completedExerciseSessionId = freezed,Object? notes = freezed,Object? blockCount = null,Object? source = null,Object? blocks = null,}) {
  return _then(_PlannedExerciseData(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,hasExplicitTime: null == hasExplicitTime ? _self.hasExplicitTime : hasExplicitTime // ignore: cast_nullable_to_non_nullable
as bool,completedExerciseSessionId: freezed == completedExerciseSessionId ? _self.completedExerciseSessionId : completedExerciseSessionId // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,blockCount: null == blockCount ? _self.blockCount : blockCount // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,blocks: null == blocks ? _self._blocks : blocks // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseBlockData>,
  ));
}


}

/// @nodoc
mixin _$PlannedExerciseBlockData {

 int get repetitions; String? get description; List<PlannedExerciseStepData> get steps;
/// Create a copy of PlannedExerciseBlockData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PlannedExerciseBlockDataCopyWith<PlannedExerciseBlockData> get copyWith => _$PlannedExerciseBlockDataCopyWithImpl<PlannedExerciseBlockData>(this as PlannedExerciseBlockData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PlannedExerciseBlockData&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.description, description) || other.description == description)&&const DeepCollectionEquality().equals(other.steps, steps));
}


@override
int get hashCode => Object.hash(runtimeType,repetitions,description,const DeepCollectionEquality().hash(steps));

@override
String toString() {
  return 'PlannedExerciseBlockData(repetitions: $repetitions, description: $description, steps: $steps)';
}


}

/// @nodoc
abstract mixin class $PlannedExerciseBlockDataCopyWith<$Res>  {
  factory $PlannedExerciseBlockDataCopyWith(PlannedExerciseBlockData value, $Res Function(PlannedExerciseBlockData) _then) = _$PlannedExerciseBlockDataCopyWithImpl;
@useResult
$Res call({
 int repetitions, String? description, List<PlannedExerciseStepData> steps
});




}
/// @nodoc
class _$PlannedExerciseBlockDataCopyWithImpl<$Res>
    implements $PlannedExerciseBlockDataCopyWith<$Res> {
  _$PlannedExerciseBlockDataCopyWithImpl(this._self, this._then);

  final PlannedExerciseBlockData _self;
  final $Res Function(PlannedExerciseBlockData) _then;

/// Create a copy of PlannedExerciseBlockData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? repetitions = null,Object? description = freezed,Object? steps = null,}) {
  return _then(_self.copyWith(
repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseStepData>,
  ));
}

}


/// Adds pattern-matching-related methods to [PlannedExerciseBlockData].
extension PlannedExerciseBlockDataPatterns on PlannedExerciseBlockData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PlannedExerciseBlockData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PlannedExerciseBlockData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PlannedExerciseBlockData value)  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseBlockData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PlannedExerciseBlockData value)?  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseBlockData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int repetitions,  String? description,  List<PlannedExerciseStepData> steps)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PlannedExerciseBlockData() when $default != null:
return $default(_that.repetitions,_that.description,_that.steps);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int repetitions,  String? description,  List<PlannedExerciseStepData> steps)  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseBlockData():
return $default(_that.repetitions,_that.description,_that.steps);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int repetitions,  String? description,  List<PlannedExerciseStepData> steps)?  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseBlockData() when $default != null:
return $default(_that.repetitions,_that.description,_that.steps);case _:
  return null;

}
}

}

/// @nodoc


class _PlannedExerciseBlockData implements PlannedExerciseBlockData {
  const _PlannedExerciseBlockData({required this.repetitions, required this.description, required final  List<PlannedExerciseStepData> steps}): _steps = steps;
  

@override final  int repetitions;
@override final  String? description;
 final  List<PlannedExerciseStepData> _steps;
@override List<PlannedExerciseStepData> get steps {
  if (_steps is EqualUnmodifiableListView) return _steps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_steps);
}


/// Create a copy of PlannedExerciseBlockData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PlannedExerciseBlockDataCopyWith<_PlannedExerciseBlockData> get copyWith => __$PlannedExerciseBlockDataCopyWithImpl<_PlannedExerciseBlockData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PlannedExerciseBlockData&&(identical(other.repetitions, repetitions) || other.repetitions == repetitions)&&(identical(other.description, description) || other.description == description)&&const DeepCollectionEquality().equals(other._steps, _steps));
}


@override
int get hashCode => Object.hash(runtimeType,repetitions,description,const DeepCollectionEquality().hash(_steps));

@override
String toString() {
  return 'PlannedExerciseBlockData(repetitions: $repetitions, description: $description, steps: $steps)';
}


}

/// @nodoc
abstract mixin class _$PlannedExerciseBlockDataCopyWith<$Res> implements $PlannedExerciseBlockDataCopyWith<$Res> {
  factory _$PlannedExerciseBlockDataCopyWith(_PlannedExerciseBlockData value, $Res Function(_PlannedExerciseBlockData) _then) = __$PlannedExerciseBlockDataCopyWithImpl;
@override @useResult
$Res call({
 int repetitions, String? description, List<PlannedExerciseStepData> steps
});




}
/// @nodoc
class __$PlannedExerciseBlockDataCopyWithImpl<$Res>
    implements _$PlannedExerciseBlockDataCopyWith<$Res> {
  __$PlannedExerciseBlockDataCopyWithImpl(this._self, this._then);

  final _PlannedExerciseBlockData _self;
  final $Res Function(_PlannedExerciseBlockData) _then;

/// Create a copy of PlannedExerciseBlockData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? repetitions = null,Object? description = freezed,Object? steps = null,}) {
  return _then(_PlannedExerciseBlockData(
repetitions: null == repetitions ? _self.repetitions : repetitions // ignore: cast_nullable_to_non_nullable
as int,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,steps: null == steps ? _self._steps : steps // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseStepData>,
  ));
}


}

/// @nodoc
mixin _$PlannedExerciseStepData {

 int get exerciseType; int get exercisePhase; String? get description; PlannedExerciseCompletion get completion;
/// Create a copy of PlannedExerciseStepData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PlannedExerciseStepDataCopyWith<PlannedExerciseStepData> get copyWith => _$PlannedExerciseStepDataCopyWithImpl<PlannedExerciseStepData>(this as PlannedExerciseStepData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PlannedExerciseStepData&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.exercisePhase, exercisePhase) || other.exercisePhase == exercisePhase)&&(identical(other.description, description) || other.description == description)&&(identical(other.completion, completion) || other.completion == completion));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,exercisePhase,description,completion);

@override
String toString() {
  return 'PlannedExerciseStepData(exerciseType: $exerciseType, exercisePhase: $exercisePhase, description: $description, completion: $completion)';
}


}

/// @nodoc
abstract mixin class $PlannedExerciseStepDataCopyWith<$Res>  {
  factory $PlannedExerciseStepDataCopyWith(PlannedExerciseStepData value, $Res Function(PlannedExerciseStepData) _then) = _$PlannedExerciseStepDataCopyWithImpl;
@useResult
$Res call({
 int exerciseType, int exercisePhase, String? description, PlannedExerciseCompletion completion
});




}
/// @nodoc
class _$PlannedExerciseStepDataCopyWithImpl<$Res>
    implements $PlannedExerciseStepDataCopyWith<$Res> {
  _$PlannedExerciseStepDataCopyWithImpl(this._self, this._then);

  final PlannedExerciseStepData _self;
  final $Res Function(PlannedExerciseStepData) _then;

/// Create a copy of PlannedExerciseStepData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? exerciseType = null,Object? exercisePhase = null,Object? description = freezed,Object? completion = null,}) {
  return _then(_self.copyWith(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,exercisePhase: null == exercisePhase ? _self.exercisePhase : exercisePhase // ignore: cast_nullable_to_non_nullable
as int,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,completion: null == completion ? _self.completion : completion // ignore: cast_nullable_to_non_nullable
as PlannedExerciseCompletion,
  ));
}

}


/// Adds pattern-matching-related methods to [PlannedExerciseStepData].
extension PlannedExerciseStepDataPatterns on PlannedExerciseStepData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PlannedExerciseStepData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PlannedExerciseStepData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PlannedExerciseStepData value)  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseStepData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PlannedExerciseStepData value)?  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseStepData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int exerciseType,  int exercisePhase,  String? description,  PlannedExerciseCompletion completion)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PlannedExerciseStepData() when $default != null:
return $default(_that.exerciseType,_that.exercisePhase,_that.description,_that.completion);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int exerciseType,  int exercisePhase,  String? description,  PlannedExerciseCompletion completion)  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseStepData():
return $default(_that.exerciseType,_that.exercisePhase,_that.description,_that.completion);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int exerciseType,  int exercisePhase,  String? description,  PlannedExerciseCompletion completion)?  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseStepData() when $default != null:
return $default(_that.exerciseType,_that.exercisePhase,_that.description,_that.completion);case _:
  return null;

}
}

}

/// @nodoc


class _PlannedExerciseStepData implements PlannedExerciseStepData {
  const _PlannedExerciseStepData({required this.exerciseType, required this.exercisePhase, required this.description, required this.completion});
  

@override final  int exerciseType;
@override final  int exercisePhase;
@override final  String? description;
@override final  PlannedExerciseCompletion completion;

/// Create a copy of PlannedExerciseStepData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PlannedExerciseStepDataCopyWith<_PlannedExerciseStepData> get copyWith => __$PlannedExerciseStepDataCopyWithImpl<_PlannedExerciseStepData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PlannedExerciseStepData&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.exercisePhase, exercisePhase) || other.exercisePhase == exercisePhase)&&(identical(other.description, description) || other.description == description)&&(identical(other.completion, completion) || other.completion == completion));
}


@override
int get hashCode => Object.hash(runtimeType,exerciseType,exercisePhase,description,completion);

@override
String toString() {
  return 'PlannedExerciseStepData(exerciseType: $exerciseType, exercisePhase: $exercisePhase, description: $description, completion: $completion)';
}


}

/// @nodoc
abstract mixin class _$PlannedExerciseStepDataCopyWith<$Res> implements $PlannedExerciseStepDataCopyWith<$Res> {
  factory _$PlannedExerciseStepDataCopyWith(_PlannedExerciseStepData value, $Res Function(_PlannedExerciseStepData) _then) = __$PlannedExerciseStepDataCopyWithImpl;
@override @useResult
$Res call({
 int exerciseType, int exercisePhase, String? description, PlannedExerciseCompletion completion
});




}
/// @nodoc
class __$PlannedExerciseStepDataCopyWithImpl<$Res>
    implements _$PlannedExerciseStepDataCopyWith<$Res> {
  __$PlannedExerciseStepDataCopyWithImpl(this._self, this._then);

  final _PlannedExerciseStepData _self;
  final $Res Function(_PlannedExerciseStepData) _then;

/// Create a copy of PlannedExerciseStepData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? exerciseType = null,Object? exercisePhase = null,Object? description = freezed,Object? completion = null,}) {
  return _then(_PlannedExerciseStepData(
exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,exercisePhase: null == exercisePhase ? _self.exercisePhase : exercisePhase // ignore: cast_nullable_to_non_nullable
as int,description: freezed == description ? _self.description : description // ignore: cast_nullable_to_non_nullable
as String?,completion: null == completion ? _self.completion : completion // ignore: cast_nullable_to_non_nullable
as PlannedExerciseCompletion,
  ));
}


}

/// @nodoc
mixin _$PlannedExerciseWriteRequest {

 String? get id; int get exerciseType; DateTime get startTime; DateTime get endTime; String? get title; String? get notes; List<PlannedExerciseBlockData> get blocks;
/// Create a copy of PlannedExerciseWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PlannedExerciseWriteRequestCopyWith<PlannedExerciseWriteRequest> get copyWith => _$PlannedExerciseWriteRequestCopyWithImpl<PlannedExerciseWriteRequest>(this as PlannedExerciseWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PlannedExerciseWriteRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&const DeepCollectionEquality().equals(other.blocks, blocks));
}


@override
int get hashCode => Object.hash(runtimeType,id,exerciseType,startTime,endTime,title,notes,const DeepCollectionEquality().hash(blocks));

@override
String toString() {
  return 'PlannedExerciseWriteRequest(id: $id, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, title: $title, notes: $notes, blocks: $blocks)';
}


}

/// @nodoc
abstract mixin class $PlannedExerciseWriteRequestCopyWith<$Res>  {
  factory $PlannedExerciseWriteRequestCopyWith(PlannedExerciseWriteRequest value, $Res Function(PlannedExerciseWriteRequest) _then) = _$PlannedExerciseWriteRequestCopyWithImpl;
@useResult
$Res call({
 String? id, int exerciseType, DateTime startTime, DateTime endTime, String? title, String? notes, List<PlannedExerciseBlockData> blocks
});




}
/// @nodoc
class _$PlannedExerciseWriteRequestCopyWithImpl<$Res>
    implements $PlannedExerciseWriteRequestCopyWith<$Res> {
  _$PlannedExerciseWriteRequestCopyWithImpl(this._self, this._then);

  final PlannedExerciseWriteRequest _self;
  final $Res Function(PlannedExerciseWriteRequest) _then;

/// Create a copy of PlannedExerciseWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? title = freezed,Object? notes = freezed,Object? blocks = null,}) {
  return _then(_self.copyWith(
id: freezed == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,blocks: null == blocks ? _self.blocks : blocks // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseBlockData>,
  ));
}

}


/// Adds pattern-matching-related methods to [PlannedExerciseWriteRequest].
extension PlannedExerciseWriteRequestPatterns on PlannedExerciseWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PlannedExerciseWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PlannedExerciseWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PlannedExerciseWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String? id,  int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  List<PlannedExerciseBlockData> blocks)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest() when $default != null:
return $default(_that.id,_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.blocks);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String? id,  int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  List<PlannedExerciseBlockData> blocks)  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest():
return $default(_that.id,_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.blocks);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String? id,  int exerciseType,  DateTime startTime,  DateTime endTime,  String? title,  String? notes,  List<PlannedExerciseBlockData> blocks)?  $default,) {final _that = this;
switch (_that) {
case _PlannedExerciseWriteRequest() when $default != null:
return $default(_that.id,_that.exerciseType,_that.startTime,_that.endTime,_that.title,_that.notes,_that.blocks);case _:
  return null;

}
}

}

/// @nodoc


class _PlannedExerciseWriteRequest implements PlannedExerciseWriteRequest {
  const _PlannedExerciseWriteRequest({this.id, required this.exerciseType, required this.startTime, required this.endTime, this.title, this.notes, required final  List<PlannedExerciseBlockData> blocks}): _blocks = blocks;
  

@override final  String? id;
@override final  int exerciseType;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  String? title;
@override final  String? notes;
 final  List<PlannedExerciseBlockData> _blocks;
@override List<PlannedExerciseBlockData> get blocks {
  if (_blocks is EqualUnmodifiableListView) return _blocks;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_blocks);
}


/// Create a copy of PlannedExerciseWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PlannedExerciseWriteRequestCopyWith<_PlannedExerciseWriteRequest> get copyWith => __$PlannedExerciseWriteRequestCopyWithImpl<_PlannedExerciseWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PlannedExerciseWriteRequest&&(identical(other.id, id) || other.id == id)&&(identical(other.exerciseType, exerciseType) || other.exerciseType == exerciseType)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.title, title) || other.title == title)&&(identical(other.notes, notes) || other.notes == notes)&&const DeepCollectionEquality().equals(other._blocks, _blocks));
}


@override
int get hashCode => Object.hash(runtimeType,id,exerciseType,startTime,endTime,title,notes,const DeepCollectionEquality().hash(_blocks));

@override
String toString() {
  return 'PlannedExerciseWriteRequest(id: $id, exerciseType: $exerciseType, startTime: $startTime, endTime: $endTime, title: $title, notes: $notes, blocks: $blocks)';
}


}

/// @nodoc
abstract mixin class _$PlannedExerciseWriteRequestCopyWith<$Res> implements $PlannedExerciseWriteRequestCopyWith<$Res> {
  factory _$PlannedExerciseWriteRequestCopyWith(_PlannedExerciseWriteRequest value, $Res Function(_PlannedExerciseWriteRequest) _then) = __$PlannedExerciseWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 String? id, int exerciseType, DateTime startTime, DateTime endTime, String? title, String? notes, List<PlannedExerciseBlockData> blocks
});




}
/// @nodoc
class __$PlannedExerciseWriteRequestCopyWithImpl<$Res>
    implements _$PlannedExerciseWriteRequestCopyWith<$Res> {
  __$PlannedExerciseWriteRequestCopyWithImpl(this._self, this._then);

  final _PlannedExerciseWriteRequest _self;
  final $Res Function(_PlannedExerciseWriteRequest) _then;

/// Create a copy of PlannedExerciseWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = freezed,Object? exerciseType = null,Object? startTime = null,Object? endTime = null,Object? title = freezed,Object? notes = freezed,Object? blocks = null,}) {
  return _then(_PlannedExerciseWriteRequest(
id: freezed == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String?,exerciseType: null == exerciseType ? _self.exerciseType : exerciseType // ignore: cast_nullable_to_non_nullable
as int,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,notes: freezed == notes ? _self.notes : notes // ignore: cast_nullable_to_non_nullable
as String?,blocks: null == blocks ? _self._blocks : blocks // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseBlockData>,
  ));
}


}

/// @nodoc
mixin _$DailySteps {

 LocalDate get date; int get steps; double get distanceMeters; int? get wheelchairPushes; int? get floorsClimbed; double? get activeCaloriesKcal; double? get elevationGainedMeters;
/// Create a copy of DailySteps
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyStepsCopyWith<DailySteps> get copyWith => _$DailyStepsCopyWithImpl<DailySteps>(this as DailySteps, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailySteps&&(identical(other.date, date) || other.date == date)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters));
}


@override
int get hashCode => Object.hash(runtimeType,date,steps,distanceMeters,wheelchairPushes,floorsClimbed,activeCaloriesKcal,elevationGainedMeters);

@override
String toString() {
  return 'DailySteps(date: $date, steps: $steps, distanceMeters: $distanceMeters, wheelchairPushes: $wheelchairPushes, floorsClimbed: $floorsClimbed, activeCaloriesKcal: $activeCaloriesKcal, elevationGainedMeters: $elevationGainedMeters)';
}


}

/// @nodoc
abstract mixin class $DailyStepsCopyWith<$Res>  {
  factory $DailyStepsCopyWith(DailySteps value, $Res Function(DailySteps) _then) = _$DailyStepsCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int steps, double distanceMeters, int? wheelchairPushes, int? floorsClimbed, double? activeCaloriesKcal, double? elevationGainedMeters
});




}
/// @nodoc
class _$DailyStepsCopyWithImpl<$Res>
    implements $DailyStepsCopyWith<$Res> {
  _$DailyStepsCopyWithImpl(this._self, this._then);

  final DailySteps _self;
  final $Res Function(DailySteps) _then;

/// Create a copy of DailySteps
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? steps = null,Object? distanceMeters = null,Object? wheelchairPushes = freezed,Object? floorsClimbed = freezed,Object? activeCaloriesKcal = freezed,Object? elevationGainedMeters = freezed,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [DailySteps].
extension DailyStepsPatterns on DailySteps {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailySteps value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailySteps() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailySteps value)  $default,){
final _that = this;
switch (_that) {
case _DailySteps():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailySteps value)?  $default,){
final _that = this;
switch (_that) {
case _DailySteps() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int steps,  double distanceMeters,  int? wheelchairPushes,  int? floorsClimbed,  double? activeCaloriesKcal,  double? elevationGainedMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailySteps() when $default != null:
return $default(_that.date,_that.steps,_that.distanceMeters,_that.wheelchairPushes,_that.floorsClimbed,_that.activeCaloriesKcal,_that.elevationGainedMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int steps,  double distanceMeters,  int? wheelchairPushes,  int? floorsClimbed,  double? activeCaloriesKcal,  double? elevationGainedMeters)  $default,) {final _that = this;
switch (_that) {
case _DailySteps():
return $default(_that.date,_that.steps,_that.distanceMeters,_that.wheelchairPushes,_that.floorsClimbed,_that.activeCaloriesKcal,_that.elevationGainedMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int steps,  double distanceMeters,  int? wheelchairPushes,  int? floorsClimbed,  double? activeCaloriesKcal,  double? elevationGainedMeters)?  $default,) {final _that = this;
switch (_that) {
case _DailySteps() when $default != null:
return $default(_that.date,_that.steps,_that.distanceMeters,_that.wheelchairPushes,_that.floorsClimbed,_that.activeCaloriesKcal,_that.elevationGainedMeters);case _:
  return null;

}
}

}

/// @nodoc


class _DailySteps implements DailySteps {
  const _DailySteps({required this.date, required this.steps, required this.distanceMeters, this.wheelchairPushes, this.floorsClimbed, this.activeCaloriesKcal, this.elevationGainedMeters});
  

@override final  LocalDate date;
@override final  int steps;
@override final  double distanceMeters;
@override final  int? wheelchairPushes;
@override final  int? floorsClimbed;
@override final  double? activeCaloriesKcal;
@override final  double? elevationGainedMeters;

/// Create a copy of DailySteps
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyStepsCopyWith<_DailySteps> get copyWith => __$DailyStepsCopyWithImpl<_DailySteps>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailySteps&&(identical(other.date, date) || other.date == date)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters));
}


@override
int get hashCode => Object.hash(runtimeType,date,steps,distanceMeters,wheelchairPushes,floorsClimbed,activeCaloriesKcal,elevationGainedMeters);

@override
String toString() {
  return 'DailySteps(date: $date, steps: $steps, distanceMeters: $distanceMeters, wheelchairPushes: $wheelchairPushes, floorsClimbed: $floorsClimbed, activeCaloriesKcal: $activeCaloriesKcal, elevationGainedMeters: $elevationGainedMeters)';
}


}

/// @nodoc
abstract mixin class _$DailyStepsCopyWith<$Res> implements $DailyStepsCopyWith<$Res> {
  factory _$DailyStepsCopyWith(_DailySteps value, $Res Function(_DailySteps) _then) = __$DailyStepsCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int steps, double distanceMeters, int? wheelchairPushes, int? floorsClimbed, double? activeCaloriesKcal, double? elevationGainedMeters
});




}
/// @nodoc
class __$DailyStepsCopyWithImpl<$Res>
    implements _$DailyStepsCopyWith<$Res> {
  __$DailyStepsCopyWithImpl(this._self, this._then);

  final _DailySteps _self;
  final $Res Function(_DailySteps) _then;

/// Create a copy of DailySteps
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? steps = null,Object? distanceMeters = null,Object? wheelchairPushes = freezed,Object? floorsClimbed = freezed,Object? activeCaloriesKcal = freezed,Object? elevationGainedMeters = freezed,}) {
  return _then(_DailySteps(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$ActivityProgressPoint {

 DateTime get time; int get totalSteps; double? get totalDistanceMeters; double? get totalCaloriesBurnedKcal; double? get totalActiveCaloriesKcal; int? get totalWheelchairPushes; int? get totalFloorsClimbed; double? get totalElevationGainedMeters;
/// Create a copy of ActivityProgressPoint
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityProgressPointCopyWith<ActivityProgressPoint> get copyWith => _$ActivityProgressPointCopyWithImpl<ActivityProgressPoint>(this as ActivityProgressPoint, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityProgressPoint&&(identical(other.time, time) || other.time == time)&&(identical(other.totalSteps, totalSteps) || other.totalSteps == totalSteps)&&(identical(other.totalDistanceMeters, totalDistanceMeters) || other.totalDistanceMeters == totalDistanceMeters)&&(identical(other.totalCaloriesBurnedKcal, totalCaloriesBurnedKcal) || other.totalCaloriesBurnedKcal == totalCaloriesBurnedKcal)&&(identical(other.totalActiveCaloriesKcal, totalActiveCaloriesKcal) || other.totalActiveCaloriesKcal == totalActiveCaloriesKcal)&&(identical(other.totalWheelchairPushes, totalWheelchairPushes) || other.totalWheelchairPushes == totalWheelchairPushes)&&(identical(other.totalFloorsClimbed, totalFloorsClimbed) || other.totalFloorsClimbed == totalFloorsClimbed)&&(identical(other.totalElevationGainedMeters, totalElevationGainedMeters) || other.totalElevationGainedMeters == totalElevationGainedMeters));
}


@override
int get hashCode => Object.hash(runtimeType,time,totalSteps,totalDistanceMeters,totalCaloriesBurnedKcal,totalActiveCaloriesKcal,totalWheelchairPushes,totalFloorsClimbed,totalElevationGainedMeters);

@override
String toString() {
  return 'ActivityProgressPoint(time: $time, totalSteps: $totalSteps, totalDistanceMeters: $totalDistanceMeters, totalCaloriesBurnedKcal: $totalCaloriesBurnedKcal, totalActiveCaloriesKcal: $totalActiveCaloriesKcal, totalWheelchairPushes: $totalWheelchairPushes, totalFloorsClimbed: $totalFloorsClimbed, totalElevationGainedMeters: $totalElevationGainedMeters)';
}


}

/// @nodoc
abstract mixin class $ActivityProgressPointCopyWith<$Res>  {
  factory $ActivityProgressPointCopyWith(ActivityProgressPoint value, $Res Function(ActivityProgressPoint) _then) = _$ActivityProgressPointCopyWithImpl;
@useResult
$Res call({
 DateTime time, int totalSteps, double? totalDistanceMeters, double? totalCaloriesBurnedKcal, double? totalActiveCaloriesKcal, int? totalWheelchairPushes, int? totalFloorsClimbed, double? totalElevationGainedMeters
});




}
/// @nodoc
class _$ActivityProgressPointCopyWithImpl<$Res>
    implements $ActivityProgressPointCopyWith<$Res> {
  _$ActivityProgressPointCopyWithImpl(this._self, this._then);

  final ActivityProgressPoint _self;
  final $Res Function(ActivityProgressPoint) _then;

/// Create a copy of ActivityProgressPoint
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? totalSteps = null,Object? totalDistanceMeters = freezed,Object? totalCaloriesBurnedKcal = freezed,Object? totalActiveCaloriesKcal = freezed,Object? totalWheelchairPushes = freezed,Object? totalFloorsClimbed = freezed,Object? totalElevationGainedMeters = freezed,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,totalSteps: null == totalSteps ? _self.totalSteps : totalSteps // ignore: cast_nullable_to_non_nullable
as int,totalDistanceMeters: freezed == totalDistanceMeters ? _self.totalDistanceMeters : totalDistanceMeters // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesBurnedKcal: freezed == totalCaloriesBurnedKcal ? _self.totalCaloriesBurnedKcal : totalCaloriesBurnedKcal // ignore: cast_nullable_to_non_nullable
as double?,totalActiveCaloriesKcal: freezed == totalActiveCaloriesKcal ? _self.totalActiveCaloriesKcal : totalActiveCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,totalWheelchairPushes: freezed == totalWheelchairPushes ? _self.totalWheelchairPushes : totalWheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,totalFloorsClimbed: freezed == totalFloorsClimbed ? _self.totalFloorsClimbed : totalFloorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,totalElevationGainedMeters: freezed == totalElevationGainedMeters ? _self.totalElevationGainedMeters : totalElevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityProgressPoint].
extension ActivityProgressPointPatterns on ActivityProgressPoint {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityProgressPoint value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityProgressPoint() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityProgressPoint value)  $default,){
final _that = this;
switch (_that) {
case _ActivityProgressPoint():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityProgressPoint value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityProgressPoint() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int totalSteps,  double? totalDistanceMeters,  double? totalCaloriesBurnedKcal,  double? totalActiveCaloriesKcal,  int? totalWheelchairPushes,  int? totalFloorsClimbed,  double? totalElevationGainedMeters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityProgressPoint() when $default != null:
return $default(_that.time,_that.totalSteps,_that.totalDistanceMeters,_that.totalCaloriesBurnedKcal,_that.totalActiveCaloriesKcal,_that.totalWheelchairPushes,_that.totalFloorsClimbed,_that.totalElevationGainedMeters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int totalSteps,  double? totalDistanceMeters,  double? totalCaloriesBurnedKcal,  double? totalActiveCaloriesKcal,  int? totalWheelchairPushes,  int? totalFloorsClimbed,  double? totalElevationGainedMeters)  $default,) {final _that = this;
switch (_that) {
case _ActivityProgressPoint():
return $default(_that.time,_that.totalSteps,_that.totalDistanceMeters,_that.totalCaloriesBurnedKcal,_that.totalActiveCaloriesKcal,_that.totalWheelchairPushes,_that.totalFloorsClimbed,_that.totalElevationGainedMeters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int totalSteps,  double? totalDistanceMeters,  double? totalCaloriesBurnedKcal,  double? totalActiveCaloriesKcal,  int? totalWheelchairPushes,  int? totalFloorsClimbed,  double? totalElevationGainedMeters)?  $default,) {final _that = this;
switch (_that) {
case _ActivityProgressPoint() when $default != null:
return $default(_that.time,_that.totalSteps,_that.totalDistanceMeters,_that.totalCaloriesBurnedKcal,_that.totalActiveCaloriesKcal,_that.totalWheelchairPushes,_that.totalFloorsClimbed,_that.totalElevationGainedMeters);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityProgressPoint implements ActivityProgressPoint {
  const _ActivityProgressPoint({required this.time, required this.totalSteps, required this.totalDistanceMeters, required this.totalCaloriesBurnedKcal, this.totalActiveCaloriesKcal, this.totalWheelchairPushes, this.totalFloorsClimbed, this.totalElevationGainedMeters});
  

@override final  DateTime time;
@override final  int totalSteps;
@override final  double? totalDistanceMeters;
@override final  double? totalCaloriesBurnedKcal;
@override final  double? totalActiveCaloriesKcal;
@override final  int? totalWheelchairPushes;
@override final  int? totalFloorsClimbed;
@override final  double? totalElevationGainedMeters;

/// Create a copy of ActivityProgressPoint
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityProgressPointCopyWith<_ActivityProgressPoint> get copyWith => __$ActivityProgressPointCopyWithImpl<_ActivityProgressPoint>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityProgressPoint&&(identical(other.time, time) || other.time == time)&&(identical(other.totalSteps, totalSteps) || other.totalSteps == totalSteps)&&(identical(other.totalDistanceMeters, totalDistanceMeters) || other.totalDistanceMeters == totalDistanceMeters)&&(identical(other.totalCaloriesBurnedKcal, totalCaloriesBurnedKcal) || other.totalCaloriesBurnedKcal == totalCaloriesBurnedKcal)&&(identical(other.totalActiveCaloriesKcal, totalActiveCaloriesKcal) || other.totalActiveCaloriesKcal == totalActiveCaloriesKcal)&&(identical(other.totalWheelchairPushes, totalWheelchairPushes) || other.totalWheelchairPushes == totalWheelchairPushes)&&(identical(other.totalFloorsClimbed, totalFloorsClimbed) || other.totalFloorsClimbed == totalFloorsClimbed)&&(identical(other.totalElevationGainedMeters, totalElevationGainedMeters) || other.totalElevationGainedMeters == totalElevationGainedMeters));
}


@override
int get hashCode => Object.hash(runtimeType,time,totalSteps,totalDistanceMeters,totalCaloriesBurnedKcal,totalActiveCaloriesKcal,totalWheelchairPushes,totalFloorsClimbed,totalElevationGainedMeters);

@override
String toString() {
  return 'ActivityProgressPoint(time: $time, totalSteps: $totalSteps, totalDistanceMeters: $totalDistanceMeters, totalCaloriesBurnedKcal: $totalCaloriesBurnedKcal, totalActiveCaloriesKcal: $totalActiveCaloriesKcal, totalWheelchairPushes: $totalWheelchairPushes, totalFloorsClimbed: $totalFloorsClimbed, totalElevationGainedMeters: $totalElevationGainedMeters)';
}


}

/// @nodoc
abstract mixin class _$ActivityProgressPointCopyWith<$Res> implements $ActivityProgressPointCopyWith<$Res> {
  factory _$ActivityProgressPointCopyWith(_ActivityProgressPoint value, $Res Function(_ActivityProgressPoint) _then) = __$ActivityProgressPointCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int totalSteps, double? totalDistanceMeters, double? totalCaloriesBurnedKcal, double? totalActiveCaloriesKcal, int? totalWheelchairPushes, int? totalFloorsClimbed, double? totalElevationGainedMeters
});




}
/// @nodoc
class __$ActivityProgressPointCopyWithImpl<$Res>
    implements _$ActivityProgressPointCopyWith<$Res> {
  __$ActivityProgressPointCopyWithImpl(this._self, this._then);

  final _ActivityProgressPoint _self;
  final $Res Function(_ActivityProgressPoint) _then;

/// Create a copy of ActivityProgressPoint
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? totalSteps = null,Object? totalDistanceMeters = freezed,Object? totalCaloriesBurnedKcal = freezed,Object? totalActiveCaloriesKcal = freezed,Object? totalWheelchairPushes = freezed,Object? totalFloorsClimbed = freezed,Object? totalElevationGainedMeters = freezed,}) {
  return _then(_ActivityProgressPoint(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,totalSteps: null == totalSteps ? _self.totalSteps : totalSteps // ignore: cast_nullable_to_non_nullable
as int,totalDistanceMeters: freezed == totalDistanceMeters ? _self.totalDistanceMeters : totalDistanceMeters // ignore: cast_nullable_to_non_nullable
as double?,totalCaloriesBurnedKcal: freezed == totalCaloriesBurnedKcal ? _self.totalCaloriesBurnedKcal : totalCaloriesBurnedKcal // ignore: cast_nullable_to_non_nullable
as double?,totalActiveCaloriesKcal: freezed == totalActiveCaloriesKcal ? _self.totalActiveCaloriesKcal : totalActiveCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,totalWheelchairPushes: freezed == totalWheelchairPushes ? _self.totalWheelchairPushes : totalWheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,totalFloorsClimbed: freezed == totalFloorsClimbed ? _self.totalFloorsClimbed : totalFloorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,totalElevationGainedMeters: freezed == totalElevationGainedMeters ? _self.totalElevationGainedMeters : totalElevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,
  ));
}


}

/// @nodoc
mixin _$SpeedSample {

 DateTime get time; double get metersPerSecond; String get source;
/// Create a copy of SpeedSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SpeedSampleCopyWith<SpeedSample> get copyWith => _$SpeedSampleCopyWithImpl<SpeedSample>(this as SpeedSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SpeedSample&&(identical(other.time, time) || other.time == time)&&(identical(other.metersPerSecond, metersPerSecond) || other.metersPerSecond == metersPerSecond)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,metersPerSecond,source);

@override
String toString() {
  return 'SpeedSample(time: $time, metersPerSecond: $metersPerSecond, source: $source)';
}


}

/// @nodoc
abstract mixin class $SpeedSampleCopyWith<$Res>  {
  factory $SpeedSampleCopyWith(SpeedSample value, $Res Function(SpeedSample) _then) = _$SpeedSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, double metersPerSecond, String source
});




}
/// @nodoc
class _$SpeedSampleCopyWithImpl<$Res>
    implements $SpeedSampleCopyWith<$Res> {
  _$SpeedSampleCopyWithImpl(this._self, this._then);

  final SpeedSample _self;
  final $Res Function(SpeedSample) _then;

/// Create a copy of SpeedSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? metersPerSecond = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,metersPerSecond: null == metersPerSecond ? _self.metersPerSecond : metersPerSecond // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [SpeedSample].
extension SpeedSamplePatterns on SpeedSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SpeedSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SpeedSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SpeedSample value)  $default,){
final _that = this;
switch (_that) {
case _SpeedSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SpeedSample value)?  $default,){
final _that = this;
switch (_that) {
case _SpeedSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double metersPerSecond,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SpeedSample() when $default != null:
return $default(_that.time,_that.metersPerSecond,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double metersPerSecond,  String source)  $default,) {final _that = this;
switch (_that) {
case _SpeedSample():
return $default(_that.time,_that.metersPerSecond,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double metersPerSecond,  String source)?  $default,) {final _that = this;
switch (_that) {
case _SpeedSample() when $default != null:
return $default(_that.time,_that.metersPerSecond,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _SpeedSample implements SpeedSample {
  const _SpeedSample({required this.time, required this.metersPerSecond, required this.source});
  

@override final  DateTime time;
@override final  double metersPerSecond;
@override final  String source;

/// Create a copy of SpeedSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SpeedSampleCopyWith<_SpeedSample> get copyWith => __$SpeedSampleCopyWithImpl<_SpeedSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SpeedSample&&(identical(other.time, time) || other.time == time)&&(identical(other.metersPerSecond, metersPerSecond) || other.metersPerSecond == metersPerSecond)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,metersPerSecond,source);

@override
String toString() {
  return 'SpeedSample(time: $time, metersPerSecond: $metersPerSecond, source: $source)';
}


}

/// @nodoc
abstract mixin class _$SpeedSampleCopyWith<$Res> implements $SpeedSampleCopyWith<$Res> {
  factory _$SpeedSampleCopyWith(_SpeedSample value, $Res Function(_SpeedSample) _then) = __$SpeedSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double metersPerSecond, String source
});




}
/// @nodoc
class __$SpeedSampleCopyWithImpl<$Res>
    implements _$SpeedSampleCopyWith<$Res> {
  __$SpeedSampleCopyWithImpl(this._self, this._then);

  final _SpeedSample _self;
  final $Res Function(_SpeedSample) _then;

/// Create a copy of SpeedSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? metersPerSecond = null,Object? source = null,}) {
  return _then(_SpeedSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,metersPerSecond: null == metersPerSecond ? _self.metersPerSecond : metersPerSecond // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$ActivityCadenceSample {

 DateTime get time; double get rate; ActivityCadenceKind get kind; String get source;
/// Create a copy of ActivityCadenceSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityCadenceSampleCopyWith<ActivityCadenceSample> get copyWith => _$ActivityCadenceSampleCopyWithImpl<ActivityCadenceSample>(this as ActivityCadenceSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rate, rate) || other.rate == rate)&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,rate,kind,source);

@override
String toString() {
  return 'ActivityCadenceSample(time: $time, rate: $rate, kind: $kind, source: $source)';
}


}

/// @nodoc
abstract mixin class $ActivityCadenceSampleCopyWith<$Res>  {
  factory $ActivityCadenceSampleCopyWith(ActivityCadenceSample value, $Res Function(ActivityCadenceSample) _then) = _$ActivityCadenceSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, double rate, ActivityCadenceKind kind, String source
});




}
/// @nodoc
class _$ActivityCadenceSampleCopyWithImpl<$Res>
    implements $ActivityCadenceSampleCopyWith<$Res> {
  _$ActivityCadenceSampleCopyWithImpl(this._self, this._then);

  final ActivityCadenceSample _self;
  final $Res Function(ActivityCadenceSample) _then;

/// Create a copy of ActivityCadenceSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? rate = null,Object? kind = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rate: null == rate ? _self.rate : rate // ignore: cast_nullable_to_non_nullable
as double,kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as ActivityCadenceKind,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [ActivityCadenceSample].
extension ActivityCadenceSamplePatterns on ActivityCadenceSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityCadenceSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityCadenceSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityCadenceSample value)  $default,){
final _that = this;
switch (_that) {
case _ActivityCadenceSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityCadenceSample value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityCadenceSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double rate,  ActivityCadenceKind kind,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityCadenceSample() when $default != null:
return $default(_that.time,_that.rate,_that.kind,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double rate,  ActivityCadenceKind kind,  String source)  $default,) {final _that = this;
switch (_that) {
case _ActivityCadenceSample():
return $default(_that.time,_that.rate,_that.kind,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double rate,  ActivityCadenceKind kind,  String source)?  $default,) {final _that = this;
switch (_that) {
case _ActivityCadenceSample() when $default != null:
return $default(_that.time,_that.rate,_that.kind,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityCadenceSample implements ActivityCadenceSample {
  const _ActivityCadenceSample({required this.time, required this.rate, required this.kind, required this.source});
  

@override final  DateTime time;
@override final  double rate;
@override final  ActivityCadenceKind kind;
@override final  String source;

/// Create a copy of ActivityCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityCadenceSampleCopyWith<_ActivityCadenceSample> get copyWith => __$ActivityCadenceSampleCopyWithImpl<_ActivityCadenceSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityCadenceSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rate, rate) || other.rate == rate)&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,rate,kind,source);

@override
String toString() {
  return 'ActivityCadenceSample(time: $time, rate: $rate, kind: $kind, source: $source)';
}


}

/// @nodoc
abstract mixin class _$ActivityCadenceSampleCopyWith<$Res> implements $ActivityCadenceSampleCopyWith<$Res> {
  factory _$ActivityCadenceSampleCopyWith(_ActivityCadenceSample value, $Res Function(_ActivityCadenceSample) _then) = __$ActivityCadenceSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double rate, ActivityCadenceKind kind, String source
});




}
/// @nodoc
class __$ActivityCadenceSampleCopyWithImpl<$Res>
    implements _$ActivityCadenceSampleCopyWith<$Res> {
  __$ActivityCadenceSampleCopyWithImpl(this._self, this._then);

  final _ActivityCadenceSample _self;
  final $Res Function(_ActivityCadenceSample) _then;

/// Create a copy of ActivityCadenceSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? rate = null,Object? kind = null,Object? source = null,}) {
  return _then(_ActivityCadenceSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rate: null == rate ? _self.rate : rate // ignore: cast_nullable_to_non_nullable
as double,kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as ActivityCadenceKind,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

// dart format on
