// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'activity_entry_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ActivityEntryUiState {

 ActivityEntryFormMode get mode; ActivityEntryType get selectedActivityType; String get titleText; ActivityEntryFeeling? get selectedFeeling; String get notesText; String get startDateText; String get startTimeText; String get durationMinutesText; String get distanceText; String get elevationText; String get activeCaloriesText; String get totalCaloriesText; ActivityRepetitionEntryMode get repetitionMode; String get repetitionTotalText; List<ActivityRepetitionSetInput> get repetitionSets; List<PlannedExerciseData> get plannedWorkouts; String? get selectedPlannedWorkoutId; ActivityPlannedWorkoutBaseline? get selectedPlannedWorkoutBaseline; String? get selectedPlannedWorkoutActivityTypeId; bool get isLoadingPlannedWorkouts; bool get isSavingPlannedWorkout; RouteFileImport? get importedRoute; List<ActivityPauseInterval> get recordedPauseIntervals; List<ExerciseLapData> get recordedLaps; List<ActivityRecordingMarker> get recordedMarkers; Set<String> get writePermissions; bool get canWrite; bool get isCheckingPermission;/// The route/FIT file import: its own command, because it is a second
/// failable action on this form and it fails for its own reasons.
 CommandState<void> get routeImport;/// Writing (or updating) the activity. [CommandSuccess] is consumed exactly
/// once by the screen, which then leaves the route.
 CommandState<void> get save; ActivityEntryError? get entryError;/// The detail behind a non-command [entryError] — a permission probe, a
/// planned-workout read, a recording that would not start. A save or an
/// import that failed carries its own [ScreenError] in its command; read
/// them together through [blockingError].
 ScreenError? get detailError; Set<ActivityEntryValidationError> get validationErrors; String? get editRecordId; bool get isRecordingDraft; BleRecordingSampleBuffer get recordedBleSamples;/// The instant the effort stopped in a guided heart-rate-recovery test, carried from
/// the recording so the write path can mark it as a trailing rest segment. Null for
/// every ordinary activity.
 DateTime? get recordedRecoveryStartTime; List<HeartRateSample> get sessionHeartRateSamples;
/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ActivityEntryUiStateCopyWith<ActivityEntryUiState> get copyWith => _$ActivityEntryUiStateCopyWithImpl<ActivityEntryUiState>(this as ActivityEntryUiState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ActivityEntryUiState&&(identical(other.mode, mode) || other.mode == mode)&&(identical(other.selectedActivityType, selectedActivityType) || other.selectedActivityType == selectedActivityType)&&(identical(other.titleText, titleText) || other.titleText == titleText)&&(identical(other.selectedFeeling, selectedFeeling) || other.selectedFeeling == selectedFeeling)&&(identical(other.notesText, notesText) || other.notesText == notesText)&&(identical(other.startDateText, startDateText) || other.startDateText == startDateText)&&(identical(other.startTimeText, startTimeText) || other.startTimeText == startTimeText)&&(identical(other.durationMinutesText, durationMinutesText) || other.durationMinutesText == durationMinutesText)&&(identical(other.distanceText, distanceText) || other.distanceText == distanceText)&&(identical(other.elevationText, elevationText) || other.elevationText == elevationText)&&(identical(other.activeCaloriesText, activeCaloriesText) || other.activeCaloriesText == activeCaloriesText)&&(identical(other.totalCaloriesText, totalCaloriesText) || other.totalCaloriesText == totalCaloriesText)&&(identical(other.repetitionMode, repetitionMode) || other.repetitionMode == repetitionMode)&&(identical(other.repetitionTotalText, repetitionTotalText) || other.repetitionTotalText == repetitionTotalText)&&const DeepCollectionEquality().equals(other.repetitionSets, repetitionSets)&&const DeepCollectionEquality().equals(other.plannedWorkouts, plannedWorkouts)&&(identical(other.selectedPlannedWorkoutId, selectedPlannedWorkoutId) || other.selectedPlannedWorkoutId == selectedPlannedWorkoutId)&&(identical(other.selectedPlannedWorkoutBaseline, selectedPlannedWorkoutBaseline) || other.selectedPlannedWorkoutBaseline == selectedPlannedWorkoutBaseline)&&(identical(other.selectedPlannedWorkoutActivityTypeId, selectedPlannedWorkoutActivityTypeId) || other.selectedPlannedWorkoutActivityTypeId == selectedPlannedWorkoutActivityTypeId)&&(identical(other.isLoadingPlannedWorkouts, isLoadingPlannedWorkouts) || other.isLoadingPlannedWorkouts == isLoadingPlannedWorkouts)&&(identical(other.isSavingPlannedWorkout, isSavingPlannedWorkout) || other.isSavingPlannedWorkout == isSavingPlannedWorkout)&&(identical(other.importedRoute, importedRoute) || other.importedRoute == importedRoute)&&const DeepCollectionEquality().equals(other.recordedPauseIntervals, recordedPauseIntervals)&&const DeepCollectionEquality().equals(other.recordedLaps, recordedLaps)&&const DeepCollectionEquality().equals(other.recordedMarkers, recordedMarkers)&&const DeepCollectionEquality().equals(other.writePermissions, writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.routeImport, routeImport) || other.routeImport == routeImport)&&(identical(other.save, save) || other.save == save)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.detailError, detailError) || other.detailError == detailError)&&const DeepCollectionEquality().equals(other.validationErrors, validationErrors)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.isRecordingDraft, isRecordingDraft) || other.isRecordingDraft == isRecordingDraft)&&(identical(other.recordedBleSamples, recordedBleSamples) || other.recordedBleSamples == recordedBleSamples)&&(identical(other.recordedRecoveryStartTime, recordedRecoveryStartTime) || other.recordedRecoveryStartTime == recordedRecoveryStartTime)&&const DeepCollectionEquality().equals(other.sessionHeartRateSamples, sessionHeartRateSamples));
}


@override
int get hashCode => Object.hashAll([runtimeType,mode,selectedActivityType,titleText,selectedFeeling,notesText,startDateText,startTimeText,durationMinutesText,distanceText,elevationText,activeCaloriesText,totalCaloriesText,repetitionMode,repetitionTotalText,const DeepCollectionEquality().hash(repetitionSets),const DeepCollectionEquality().hash(plannedWorkouts),selectedPlannedWorkoutId,selectedPlannedWorkoutBaseline,selectedPlannedWorkoutActivityTypeId,isLoadingPlannedWorkouts,isSavingPlannedWorkout,importedRoute,const DeepCollectionEquality().hash(recordedPauseIntervals),const DeepCollectionEquality().hash(recordedLaps),const DeepCollectionEquality().hash(recordedMarkers),const DeepCollectionEquality().hash(writePermissions),canWrite,isCheckingPermission,routeImport,save,entryError,detailError,const DeepCollectionEquality().hash(validationErrors),editRecordId,isRecordingDraft,recordedBleSamples,recordedRecoveryStartTime,const DeepCollectionEquality().hash(sessionHeartRateSamples)]);

@override
String toString() {
  return 'ActivityEntryUiState(mode: $mode, selectedActivityType: $selectedActivityType, titleText: $titleText, selectedFeeling: $selectedFeeling, notesText: $notesText, startDateText: $startDateText, startTimeText: $startTimeText, durationMinutesText: $durationMinutesText, distanceText: $distanceText, elevationText: $elevationText, activeCaloriesText: $activeCaloriesText, totalCaloriesText: $totalCaloriesText, repetitionMode: $repetitionMode, repetitionTotalText: $repetitionTotalText, repetitionSets: $repetitionSets, plannedWorkouts: $plannedWorkouts, selectedPlannedWorkoutId: $selectedPlannedWorkoutId, selectedPlannedWorkoutBaseline: $selectedPlannedWorkoutBaseline, selectedPlannedWorkoutActivityTypeId: $selectedPlannedWorkoutActivityTypeId, isLoadingPlannedWorkouts: $isLoadingPlannedWorkouts, isSavingPlannedWorkout: $isSavingPlannedWorkout, importedRoute: $importedRoute, recordedPauseIntervals: $recordedPauseIntervals, recordedLaps: $recordedLaps, recordedMarkers: $recordedMarkers, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, routeImport: $routeImport, save: $save, entryError: $entryError, detailError: $detailError, validationErrors: $validationErrors, editRecordId: $editRecordId, isRecordingDraft: $isRecordingDraft, recordedBleSamples: $recordedBleSamples, recordedRecoveryStartTime: $recordedRecoveryStartTime, sessionHeartRateSamples: $sessionHeartRateSamples)';
}


}

/// @nodoc
abstract mixin class $ActivityEntryUiStateCopyWith<$Res>  {
  factory $ActivityEntryUiStateCopyWith(ActivityEntryUiState value, $Res Function(ActivityEntryUiState) _then) = _$ActivityEntryUiStateCopyWithImpl;
@useResult
$Res call({
 ActivityEntryFormMode mode, ActivityEntryType selectedActivityType, String titleText, ActivityEntryFeeling? selectedFeeling, String notesText, String startDateText, String startTimeText, String durationMinutesText, String distanceText, String elevationText, String activeCaloriesText, String totalCaloriesText, ActivityRepetitionEntryMode repetitionMode, String repetitionTotalText, List<ActivityRepetitionSetInput> repetitionSets, List<PlannedExerciseData> plannedWorkouts, String? selectedPlannedWorkoutId, ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline, String? selectedPlannedWorkoutActivityTypeId, bool isLoadingPlannedWorkouts, bool isSavingPlannedWorkout, RouteFileImport? importedRoute, List<ActivityPauseInterval> recordedPauseIntervals, List<ExerciseLapData> recordedLaps, List<ActivityRecordingMarker> recordedMarkers, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> routeImport, CommandState<void> save, ActivityEntryError? entryError, ScreenError? detailError, Set<ActivityEntryValidationError> validationErrors, String? editRecordId, bool isRecordingDraft, BleRecordingSampleBuffer recordedBleSamples, DateTime? recordedRecoveryStartTime, List<HeartRateSample> sessionHeartRateSamples
});


$CommandStateCopyWith<void, $Res> get routeImport;$CommandStateCopyWith<void, $Res> get save;$BleRecordingSampleBufferCopyWith<$Res> get recordedBleSamples;

}
/// @nodoc
class _$ActivityEntryUiStateCopyWithImpl<$Res>
    implements $ActivityEntryUiStateCopyWith<$Res> {
  _$ActivityEntryUiStateCopyWithImpl(this._self, this._then);

  final ActivityEntryUiState _self;
  final $Res Function(ActivityEntryUiState) _then;

/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? mode = null,Object? selectedActivityType = null,Object? titleText = null,Object? selectedFeeling = freezed,Object? notesText = null,Object? startDateText = null,Object? startTimeText = null,Object? durationMinutesText = null,Object? distanceText = null,Object? elevationText = null,Object? activeCaloriesText = null,Object? totalCaloriesText = null,Object? repetitionMode = null,Object? repetitionTotalText = null,Object? repetitionSets = null,Object? plannedWorkouts = null,Object? selectedPlannedWorkoutId = freezed,Object? selectedPlannedWorkoutBaseline = freezed,Object? selectedPlannedWorkoutActivityTypeId = freezed,Object? isLoadingPlannedWorkouts = null,Object? isSavingPlannedWorkout = null,Object? importedRoute = freezed,Object? recordedPauseIntervals = null,Object? recordedLaps = null,Object? recordedMarkers = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? routeImport = null,Object? save = null,Object? entryError = freezed,Object? detailError = freezed,Object? validationErrors = null,Object? editRecordId = freezed,Object? isRecordingDraft = null,Object? recordedBleSamples = null,Object? recordedRecoveryStartTime = freezed,Object? sessionHeartRateSamples = null,}) {
  return _then(_self.copyWith(
mode: null == mode ? _self.mode : mode // ignore: cast_nullable_to_non_nullable
as ActivityEntryFormMode,selectedActivityType: null == selectedActivityType ? _self.selectedActivityType : selectedActivityType // ignore: cast_nullable_to_non_nullable
as ActivityEntryType,titleText: null == titleText ? _self.titleText : titleText // ignore: cast_nullable_to_non_nullable
as String,selectedFeeling: freezed == selectedFeeling ? _self.selectedFeeling : selectedFeeling // ignore: cast_nullable_to_non_nullable
as ActivityEntryFeeling?,notesText: null == notesText ? _self.notesText : notesText // ignore: cast_nullable_to_non_nullable
as String,startDateText: null == startDateText ? _self.startDateText : startDateText // ignore: cast_nullable_to_non_nullable
as String,startTimeText: null == startTimeText ? _self.startTimeText : startTimeText // ignore: cast_nullable_to_non_nullable
as String,durationMinutesText: null == durationMinutesText ? _self.durationMinutesText : durationMinutesText // ignore: cast_nullable_to_non_nullable
as String,distanceText: null == distanceText ? _self.distanceText : distanceText // ignore: cast_nullable_to_non_nullable
as String,elevationText: null == elevationText ? _self.elevationText : elevationText // ignore: cast_nullable_to_non_nullable
as String,activeCaloriesText: null == activeCaloriesText ? _self.activeCaloriesText : activeCaloriesText // ignore: cast_nullable_to_non_nullable
as String,totalCaloriesText: null == totalCaloriesText ? _self.totalCaloriesText : totalCaloriesText // ignore: cast_nullable_to_non_nullable
as String,repetitionMode: null == repetitionMode ? _self.repetitionMode : repetitionMode // ignore: cast_nullable_to_non_nullable
as ActivityRepetitionEntryMode,repetitionTotalText: null == repetitionTotalText ? _self.repetitionTotalText : repetitionTotalText // ignore: cast_nullable_to_non_nullable
as String,repetitionSets: null == repetitionSets ? _self.repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRepetitionSetInput>,plannedWorkouts: null == plannedWorkouts ? _self.plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,selectedPlannedWorkoutId: freezed == selectedPlannedWorkoutId ? _self.selectedPlannedWorkoutId : selectedPlannedWorkoutId // ignore: cast_nullable_to_non_nullable
as String?,selectedPlannedWorkoutBaseline: freezed == selectedPlannedWorkoutBaseline ? _self.selectedPlannedWorkoutBaseline : selectedPlannedWorkoutBaseline // ignore: cast_nullable_to_non_nullable
as ActivityPlannedWorkoutBaseline?,selectedPlannedWorkoutActivityTypeId: freezed == selectedPlannedWorkoutActivityTypeId ? _self.selectedPlannedWorkoutActivityTypeId : selectedPlannedWorkoutActivityTypeId // ignore: cast_nullable_to_non_nullable
as String?,isLoadingPlannedWorkouts: null == isLoadingPlannedWorkouts ? _self.isLoadingPlannedWorkouts : isLoadingPlannedWorkouts // ignore: cast_nullable_to_non_nullable
as bool,isSavingPlannedWorkout: null == isSavingPlannedWorkout ? _self.isSavingPlannedWorkout : isSavingPlannedWorkout // ignore: cast_nullable_to_non_nullable
as bool,importedRoute: freezed == importedRoute ? _self.importedRoute : importedRoute // ignore: cast_nullable_to_non_nullable
as RouteFileImport?,recordedPauseIntervals: null == recordedPauseIntervals ? _self.recordedPauseIntervals : recordedPauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,recordedLaps: null == recordedLaps ? _self.recordedLaps : recordedLaps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,recordedMarkers: null == recordedMarkers ? _self.recordedMarkers : recordedMarkers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,writePermissions: null == writePermissions ? _self.writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,routeImport: null == routeImport ? _self.routeImport : routeImport // ignore: cast_nullable_to_non_nullable
as CommandState<void>,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as ActivityEntryError?,detailError: freezed == detailError ? _self.detailError : detailError // ignore: cast_nullable_to_non_nullable
as ScreenError?,validationErrors: null == validationErrors ? _self.validationErrors : validationErrors // ignore: cast_nullable_to_non_nullable
as Set<ActivityEntryValidationError>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,isRecordingDraft: null == isRecordingDraft ? _self.isRecordingDraft : isRecordingDraft // ignore: cast_nullable_to_non_nullable
as bool,recordedBleSamples: null == recordedBleSamples ? _self.recordedBleSamples : recordedBleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,recordedRecoveryStartTime: freezed == recordedRecoveryStartTime ? _self.recordedRecoveryStartTime : recordedRecoveryStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,sessionHeartRateSamples: null == sessionHeartRateSamples ? _self.sessionHeartRateSamples : sessionHeartRateSamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,
  ));
}
/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get routeImport {
  
  return $CommandStateCopyWith<void, $Res>(_self.routeImport, (value) {
    return _then(_self.copyWith(routeImport: value));
  });
}/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get save {
  
  return $CommandStateCopyWith<void, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get recordedBleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.recordedBleSamples, (value) {
    return _then(_self.copyWith(recordedBleSamples: value));
  });
}
}


/// Adds pattern-matching-related methods to [ActivityEntryUiState].
extension ActivityEntryUiStatePatterns on ActivityEntryUiState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ActivityEntryUiState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ActivityEntryUiState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ActivityEntryUiState value)  $default,){
final _that = this;
switch (_that) {
case _ActivityEntryUiState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ActivityEntryUiState value)?  $default,){
final _that = this;
switch (_that) {
case _ActivityEntryUiState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ActivityEntryFormMode mode,  ActivityEntryType selectedActivityType,  String titleText,  ActivityEntryFeeling? selectedFeeling,  String notesText,  String startDateText,  String startTimeText,  String durationMinutesText,  String distanceText,  String elevationText,  String activeCaloriesText,  String totalCaloriesText,  ActivityRepetitionEntryMode repetitionMode,  String repetitionTotalText,  List<ActivityRepetitionSetInput> repetitionSets,  List<PlannedExerciseData> plannedWorkouts,  String? selectedPlannedWorkoutId,  ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline,  String? selectedPlannedWorkoutActivityTypeId,  bool isLoadingPlannedWorkouts,  bool isSavingPlannedWorkout,  RouteFileImport? importedRoute,  List<ActivityPauseInterval> recordedPauseIntervals,  List<ExerciseLapData> recordedLaps,  List<ActivityRecordingMarker> recordedMarkers,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> routeImport,  CommandState<void> save,  ActivityEntryError? entryError,  ScreenError? detailError,  Set<ActivityEntryValidationError> validationErrors,  String? editRecordId,  bool isRecordingDraft,  BleRecordingSampleBuffer recordedBleSamples,  DateTime? recordedRecoveryStartTime,  List<HeartRateSample> sessionHeartRateSamples)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ActivityEntryUiState() when $default != null:
return $default(_that.mode,_that.selectedActivityType,_that.titleText,_that.selectedFeeling,_that.notesText,_that.startDateText,_that.startTimeText,_that.durationMinutesText,_that.distanceText,_that.elevationText,_that.activeCaloriesText,_that.totalCaloriesText,_that.repetitionMode,_that.repetitionTotalText,_that.repetitionSets,_that.plannedWorkouts,_that.selectedPlannedWorkoutId,_that.selectedPlannedWorkoutBaseline,_that.selectedPlannedWorkoutActivityTypeId,_that.isLoadingPlannedWorkouts,_that.isSavingPlannedWorkout,_that.importedRoute,_that.recordedPauseIntervals,_that.recordedLaps,_that.recordedMarkers,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.routeImport,_that.save,_that.entryError,_that.detailError,_that.validationErrors,_that.editRecordId,_that.isRecordingDraft,_that.recordedBleSamples,_that.recordedRecoveryStartTime,_that.sessionHeartRateSamples);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ActivityEntryFormMode mode,  ActivityEntryType selectedActivityType,  String titleText,  ActivityEntryFeeling? selectedFeeling,  String notesText,  String startDateText,  String startTimeText,  String durationMinutesText,  String distanceText,  String elevationText,  String activeCaloriesText,  String totalCaloriesText,  ActivityRepetitionEntryMode repetitionMode,  String repetitionTotalText,  List<ActivityRepetitionSetInput> repetitionSets,  List<PlannedExerciseData> plannedWorkouts,  String? selectedPlannedWorkoutId,  ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline,  String? selectedPlannedWorkoutActivityTypeId,  bool isLoadingPlannedWorkouts,  bool isSavingPlannedWorkout,  RouteFileImport? importedRoute,  List<ActivityPauseInterval> recordedPauseIntervals,  List<ExerciseLapData> recordedLaps,  List<ActivityRecordingMarker> recordedMarkers,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> routeImport,  CommandState<void> save,  ActivityEntryError? entryError,  ScreenError? detailError,  Set<ActivityEntryValidationError> validationErrors,  String? editRecordId,  bool isRecordingDraft,  BleRecordingSampleBuffer recordedBleSamples,  DateTime? recordedRecoveryStartTime,  List<HeartRateSample> sessionHeartRateSamples)  $default,) {final _that = this;
switch (_that) {
case _ActivityEntryUiState():
return $default(_that.mode,_that.selectedActivityType,_that.titleText,_that.selectedFeeling,_that.notesText,_that.startDateText,_that.startTimeText,_that.durationMinutesText,_that.distanceText,_that.elevationText,_that.activeCaloriesText,_that.totalCaloriesText,_that.repetitionMode,_that.repetitionTotalText,_that.repetitionSets,_that.plannedWorkouts,_that.selectedPlannedWorkoutId,_that.selectedPlannedWorkoutBaseline,_that.selectedPlannedWorkoutActivityTypeId,_that.isLoadingPlannedWorkouts,_that.isSavingPlannedWorkout,_that.importedRoute,_that.recordedPauseIntervals,_that.recordedLaps,_that.recordedMarkers,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.routeImport,_that.save,_that.entryError,_that.detailError,_that.validationErrors,_that.editRecordId,_that.isRecordingDraft,_that.recordedBleSamples,_that.recordedRecoveryStartTime,_that.sessionHeartRateSamples);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ActivityEntryFormMode mode,  ActivityEntryType selectedActivityType,  String titleText,  ActivityEntryFeeling? selectedFeeling,  String notesText,  String startDateText,  String startTimeText,  String durationMinutesText,  String distanceText,  String elevationText,  String activeCaloriesText,  String totalCaloriesText,  ActivityRepetitionEntryMode repetitionMode,  String repetitionTotalText,  List<ActivityRepetitionSetInput> repetitionSets,  List<PlannedExerciseData> plannedWorkouts,  String? selectedPlannedWorkoutId,  ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline,  String? selectedPlannedWorkoutActivityTypeId,  bool isLoadingPlannedWorkouts,  bool isSavingPlannedWorkout,  RouteFileImport? importedRoute,  List<ActivityPauseInterval> recordedPauseIntervals,  List<ExerciseLapData> recordedLaps,  List<ActivityRecordingMarker> recordedMarkers,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> routeImport,  CommandState<void> save,  ActivityEntryError? entryError,  ScreenError? detailError,  Set<ActivityEntryValidationError> validationErrors,  String? editRecordId,  bool isRecordingDraft,  BleRecordingSampleBuffer recordedBleSamples,  DateTime? recordedRecoveryStartTime,  List<HeartRateSample> sessionHeartRateSamples)?  $default,) {final _that = this;
switch (_that) {
case _ActivityEntryUiState() when $default != null:
return $default(_that.mode,_that.selectedActivityType,_that.titleText,_that.selectedFeeling,_that.notesText,_that.startDateText,_that.startTimeText,_that.durationMinutesText,_that.distanceText,_that.elevationText,_that.activeCaloriesText,_that.totalCaloriesText,_that.repetitionMode,_that.repetitionTotalText,_that.repetitionSets,_that.plannedWorkouts,_that.selectedPlannedWorkoutId,_that.selectedPlannedWorkoutBaseline,_that.selectedPlannedWorkoutActivityTypeId,_that.isLoadingPlannedWorkouts,_that.isSavingPlannedWorkout,_that.importedRoute,_that.recordedPauseIntervals,_that.recordedLaps,_that.recordedMarkers,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.routeImport,_that.save,_that.entryError,_that.detailError,_that.validationErrors,_that.editRecordId,_that.isRecordingDraft,_that.recordedBleSamples,_that.recordedRecoveryStartTime,_that.sessionHeartRateSamples);case _:
  return null;

}
}

}

/// @nodoc


class _ActivityEntryUiState extends ActivityEntryUiState {
  const _ActivityEntryUiState({this.mode = ActivityEntryFormMode.chooseSource, required this.selectedActivityType, this.titleText = '', this.selectedFeeling, this.notesText = '', this.startDateText = '', this.startTimeText = '', this.durationMinutesText = '30', this.distanceText = '', this.elevationText = '', this.activeCaloriesText = '', this.totalCaloriesText = '', this.repetitionMode = ActivityRepetitionEntryMode.total, this.repetitionTotalText = '', final  List<ActivityRepetitionSetInput> repetitionSets = const [ActivityRepetitionSetInput()], final  List<PlannedExerciseData> plannedWorkouts = const <PlannedExerciseData>[], this.selectedPlannedWorkoutId, this.selectedPlannedWorkoutBaseline, this.selectedPlannedWorkoutActivityTypeId, this.isLoadingPlannedWorkouts = false, this.isSavingPlannedWorkout = false, this.importedRoute, final  List<ActivityPauseInterval> recordedPauseIntervals = const <ActivityPauseInterval>[], final  List<ExerciseLapData> recordedLaps = const <ExerciseLapData>[], final  List<ActivityRecordingMarker> recordedMarkers = const <ActivityRecordingMarker>[], final  Set<String> writePermissions = const <String>{}, this.canWrite = false, this.isCheckingPermission = true, this.routeImport = const CommandState<void>.idle(), this.save = const CommandState<void>.idle(), this.entryError, this.detailError, final  Set<ActivityEntryValidationError> validationErrors = const <ActivityEntryValidationError>{}, this.editRecordId, this.isRecordingDraft = false, this.recordedBleSamples = const BleRecordingSampleBuffer(), this.recordedRecoveryStartTime, final  List<HeartRateSample> sessionHeartRateSamples = const <HeartRateSample>[]}): _repetitionSets = repetitionSets,_plannedWorkouts = plannedWorkouts,_recordedPauseIntervals = recordedPauseIntervals,_recordedLaps = recordedLaps,_recordedMarkers = recordedMarkers,_writePermissions = writePermissions,_validationErrors = validationErrors,_sessionHeartRateSamples = sessionHeartRateSamples,super._();
  

@override@JsonKey() final  ActivityEntryFormMode mode;
@override final  ActivityEntryType selectedActivityType;
@override@JsonKey() final  String titleText;
@override final  ActivityEntryFeeling? selectedFeeling;
@override@JsonKey() final  String notesText;
@override@JsonKey() final  String startDateText;
@override@JsonKey() final  String startTimeText;
@override@JsonKey() final  String durationMinutesText;
@override@JsonKey() final  String distanceText;
@override@JsonKey() final  String elevationText;
@override@JsonKey() final  String activeCaloriesText;
@override@JsonKey() final  String totalCaloriesText;
@override@JsonKey() final  ActivityRepetitionEntryMode repetitionMode;
@override@JsonKey() final  String repetitionTotalText;
 final  List<ActivityRepetitionSetInput> _repetitionSets;
@override@JsonKey() List<ActivityRepetitionSetInput> get repetitionSets {
  if (_repetitionSets is EqualUnmodifiableListView) return _repetitionSets;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_repetitionSets);
}

 final  List<PlannedExerciseData> _plannedWorkouts;
@override@JsonKey() List<PlannedExerciseData> get plannedWorkouts {
  if (_plannedWorkouts is EqualUnmodifiableListView) return _plannedWorkouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_plannedWorkouts);
}

@override final  String? selectedPlannedWorkoutId;
@override final  ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline;
@override final  String? selectedPlannedWorkoutActivityTypeId;
@override@JsonKey() final  bool isLoadingPlannedWorkouts;
@override@JsonKey() final  bool isSavingPlannedWorkout;
@override final  RouteFileImport? importedRoute;
 final  List<ActivityPauseInterval> _recordedPauseIntervals;
@override@JsonKey() List<ActivityPauseInterval> get recordedPauseIntervals {
  if (_recordedPauseIntervals is EqualUnmodifiableListView) return _recordedPauseIntervals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_recordedPauseIntervals);
}

 final  List<ExerciseLapData> _recordedLaps;
@override@JsonKey() List<ExerciseLapData> get recordedLaps {
  if (_recordedLaps is EqualUnmodifiableListView) return _recordedLaps;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_recordedLaps);
}

 final  List<ActivityRecordingMarker> _recordedMarkers;
@override@JsonKey() List<ActivityRecordingMarker> get recordedMarkers {
  if (_recordedMarkers is EqualUnmodifiableListView) return _recordedMarkers;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_recordedMarkers);
}

 final  Set<String> _writePermissions;
@override@JsonKey() Set<String> get writePermissions {
  if (_writePermissions is EqualUnmodifiableSetView) return _writePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_writePermissions);
}

@override@JsonKey() final  bool canWrite;
@override@JsonKey() final  bool isCheckingPermission;
/// The route/FIT file import: its own command, because it is a second
/// failable action on this form and it fails for its own reasons.
@override@JsonKey() final  CommandState<void> routeImport;
/// Writing (or updating) the activity. [CommandSuccess] is consumed exactly
/// once by the screen, which then leaves the route.
@override@JsonKey() final  CommandState<void> save;
@override final  ActivityEntryError? entryError;
/// The detail behind a non-command [entryError] — a permission probe, a
/// planned-workout read, a recording that would not start. A save or an
/// import that failed carries its own [ScreenError] in its command; read
/// them together through [blockingError].
@override final  ScreenError? detailError;
 final  Set<ActivityEntryValidationError> _validationErrors;
@override@JsonKey() Set<ActivityEntryValidationError> get validationErrors {
  if (_validationErrors is EqualUnmodifiableSetView) return _validationErrors;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_validationErrors);
}

@override final  String? editRecordId;
@override@JsonKey() final  bool isRecordingDraft;
@override@JsonKey() final  BleRecordingSampleBuffer recordedBleSamples;
/// The instant the effort stopped in a guided heart-rate-recovery test, carried from
/// the recording so the write path can mark it as a trailing rest segment. Null for
/// every ordinary activity.
@override final  DateTime? recordedRecoveryStartTime;
 final  List<HeartRateSample> _sessionHeartRateSamples;
@override@JsonKey() List<HeartRateSample> get sessionHeartRateSamples {
  if (_sessionHeartRateSamples is EqualUnmodifiableListView) return _sessionHeartRateSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sessionHeartRateSamples);
}


/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ActivityEntryUiStateCopyWith<_ActivityEntryUiState> get copyWith => __$ActivityEntryUiStateCopyWithImpl<_ActivityEntryUiState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ActivityEntryUiState&&(identical(other.mode, mode) || other.mode == mode)&&(identical(other.selectedActivityType, selectedActivityType) || other.selectedActivityType == selectedActivityType)&&(identical(other.titleText, titleText) || other.titleText == titleText)&&(identical(other.selectedFeeling, selectedFeeling) || other.selectedFeeling == selectedFeeling)&&(identical(other.notesText, notesText) || other.notesText == notesText)&&(identical(other.startDateText, startDateText) || other.startDateText == startDateText)&&(identical(other.startTimeText, startTimeText) || other.startTimeText == startTimeText)&&(identical(other.durationMinutesText, durationMinutesText) || other.durationMinutesText == durationMinutesText)&&(identical(other.distanceText, distanceText) || other.distanceText == distanceText)&&(identical(other.elevationText, elevationText) || other.elevationText == elevationText)&&(identical(other.activeCaloriesText, activeCaloriesText) || other.activeCaloriesText == activeCaloriesText)&&(identical(other.totalCaloriesText, totalCaloriesText) || other.totalCaloriesText == totalCaloriesText)&&(identical(other.repetitionMode, repetitionMode) || other.repetitionMode == repetitionMode)&&(identical(other.repetitionTotalText, repetitionTotalText) || other.repetitionTotalText == repetitionTotalText)&&const DeepCollectionEquality().equals(other._repetitionSets, _repetitionSets)&&const DeepCollectionEquality().equals(other._plannedWorkouts, _plannedWorkouts)&&(identical(other.selectedPlannedWorkoutId, selectedPlannedWorkoutId) || other.selectedPlannedWorkoutId == selectedPlannedWorkoutId)&&(identical(other.selectedPlannedWorkoutBaseline, selectedPlannedWorkoutBaseline) || other.selectedPlannedWorkoutBaseline == selectedPlannedWorkoutBaseline)&&(identical(other.selectedPlannedWorkoutActivityTypeId, selectedPlannedWorkoutActivityTypeId) || other.selectedPlannedWorkoutActivityTypeId == selectedPlannedWorkoutActivityTypeId)&&(identical(other.isLoadingPlannedWorkouts, isLoadingPlannedWorkouts) || other.isLoadingPlannedWorkouts == isLoadingPlannedWorkouts)&&(identical(other.isSavingPlannedWorkout, isSavingPlannedWorkout) || other.isSavingPlannedWorkout == isSavingPlannedWorkout)&&(identical(other.importedRoute, importedRoute) || other.importedRoute == importedRoute)&&const DeepCollectionEquality().equals(other._recordedPauseIntervals, _recordedPauseIntervals)&&const DeepCollectionEquality().equals(other._recordedLaps, _recordedLaps)&&const DeepCollectionEquality().equals(other._recordedMarkers, _recordedMarkers)&&const DeepCollectionEquality().equals(other._writePermissions, _writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.routeImport, routeImport) || other.routeImport == routeImport)&&(identical(other.save, save) || other.save == save)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.detailError, detailError) || other.detailError == detailError)&&const DeepCollectionEquality().equals(other._validationErrors, _validationErrors)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.isRecordingDraft, isRecordingDraft) || other.isRecordingDraft == isRecordingDraft)&&(identical(other.recordedBleSamples, recordedBleSamples) || other.recordedBleSamples == recordedBleSamples)&&(identical(other.recordedRecoveryStartTime, recordedRecoveryStartTime) || other.recordedRecoveryStartTime == recordedRecoveryStartTime)&&const DeepCollectionEquality().equals(other._sessionHeartRateSamples, _sessionHeartRateSamples));
}


@override
int get hashCode => Object.hashAll([runtimeType,mode,selectedActivityType,titleText,selectedFeeling,notesText,startDateText,startTimeText,durationMinutesText,distanceText,elevationText,activeCaloriesText,totalCaloriesText,repetitionMode,repetitionTotalText,const DeepCollectionEquality().hash(_repetitionSets),const DeepCollectionEquality().hash(_plannedWorkouts),selectedPlannedWorkoutId,selectedPlannedWorkoutBaseline,selectedPlannedWorkoutActivityTypeId,isLoadingPlannedWorkouts,isSavingPlannedWorkout,importedRoute,const DeepCollectionEquality().hash(_recordedPauseIntervals),const DeepCollectionEquality().hash(_recordedLaps),const DeepCollectionEquality().hash(_recordedMarkers),const DeepCollectionEquality().hash(_writePermissions),canWrite,isCheckingPermission,routeImport,save,entryError,detailError,const DeepCollectionEquality().hash(_validationErrors),editRecordId,isRecordingDraft,recordedBleSamples,recordedRecoveryStartTime,const DeepCollectionEquality().hash(_sessionHeartRateSamples)]);

@override
String toString() {
  return 'ActivityEntryUiState(mode: $mode, selectedActivityType: $selectedActivityType, titleText: $titleText, selectedFeeling: $selectedFeeling, notesText: $notesText, startDateText: $startDateText, startTimeText: $startTimeText, durationMinutesText: $durationMinutesText, distanceText: $distanceText, elevationText: $elevationText, activeCaloriesText: $activeCaloriesText, totalCaloriesText: $totalCaloriesText, repetitionMode: $repetitionMode, repetitionTotalText: $repetitionTotalText, repetitionSets: $repetitionSets, plannedWorkouts: $plannedWorkouts, selectedPlannedWorkoutId: $selectedPlannedWorkoutId, selectedPlannedWorkoutBaseline: $selectedPlannedWorkoutBaseline, selectedPlannedWorkoutActivityTypeId: $selectedPlannedWorkoutActivityTypeId, isLoadingPlannedWorkouts: $isLoadingPlannedWorkouts, isSavingPlannedWorkout: $isSavingPlannedWorkout, importedRoute: $importedRoute, recordedPauseIntervals: $recordedPauseIntervals, recordedLaps: $recordedLaps, recordedMarkers: $recordedMarkers, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, routeImport: $routeImport, save: $save, entryError: $entryError, detailError: $detailError, validationErrors: $validationErrors, editRecordId: $editRecordId, isRecordingDraft: $isRecordingDraft, recordedBleSamples: $recordedBleSamples, recordedRecoveryStartTime: $recordedRecoveryStartTime, sessionHeartRateSamples: $sessionHeartRateSamples)';
}


}

/// @nodoc
abstract mixin class _$ActivityEntryUiStateCopyWith<$Res> implements $ActivityEntryUiStateCopyWith<$Res> {
  factory _$ActivityEntryUiStateCopyWith(_ActivityEntryUiState value, $Res Function(_ActivityEntryUiState) _then) = __$ActivityEntryUiStateCopyWithImpl;
@override @useResult
$Res call({
 ActivityEntryFormMode mode, ActivityEntryType selectedActivityType, String titleText, ActivityEntryFeeling? selectedFeeling, String notesText, String startDateText, String startTimeText, String durationMinutesText, String distanceText, String elevationText, String activeCaloriesText, String totalCaloriesText, ActivityRepetitionEntryMode repetitionMode, String repetitionTotalText, List<ActivityRepetitionSetInput> repetitionSets, List<PlannedExerciseData> plannedWorkouts, String? selectedPlannedWorkoutId, ActivityPlannedWorkoutBaseline? selectedPlannedWorkoutBaseline, String? selectedPlannedWorkoutActivityTypeId, bool isLoadingPlannedWorkouts, bool isSavingPlannedWorkout, RouteFileImport? importedRoute, List<ActivityPauseInterval> recordedPauseIntervals, List<ExerciseLapData> recordedLaps, List<ActivityRecordingMarker> recordedMarkers, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> routeImport, CommandState<void> save, ActivityEntryError? entryError, ScreenError? detailError, Set<ActivityEntryValidationError> validationErrors, String? editRecordId, bool isRecordingDraft, BleRecordingSampleBuffer recordedBleSamples, DateTime? recordedRecoveryStartTime, List<HeartRateSample> sessionHeartRateSamples
});


@override $CommandStateCopyWith<void, $Res> get routeImport;@override $CommandStateCopyWith<void, $Res> get save;@override $BleRecordingSampleBufferCopyWith<$Res> get recordedBleSamples;

}
/// @nodoc
class __$ActivityEntryUiStateCopyWithImpl<$Res>
    implements _$ActivityEntryUiStateCopyWith<$Res> {
  __$ActivityEntryUiStateCopyWithImpl(this._self, this._then);

  final _ActivityEntryUiState _self;
  final $Res Function(_ActivityEntryUiState) _then;

/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? mode = null,Object? selectedActivityType = null,Object? titleText = null,Object? selectedFeeling = freezed,Object? notesText = null,Object? startDateText = null,Object? startTimeText = null,Object? durationMinutesText = null,Object? distanceText = null,Object? elevationText = null,Object? activeCaloriesText = null,Object? totalCaloriesText = null,Object? repetitionMode = null,Object? repetitionTotalText = null,Object? repetitionSets = null,Object? plannedWorkouts = null,Object? selectedPlannedWorkoutId = freezed,Object? selectedPlannedWorkoutBaseline = freezed,Object? selectedPlannedWorkoutActivityTypeId = freezed,Object? isLoadingPlannedWorkouts = null,Object? isSavingPlannedWorkout = null,Object? importedRoute = freezed,Object? recordedPauseIntervals = null,Object? recordedLaps = null,Object? recordedMarkers = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? routeImport = null,Object? save = null,Object? entryError = freezed,Object? detailError = freezed,Object? validationErrors = null,Object? editRecordId = freezed,Object? isRecordingDraft = null,Object? recordedBleSamples = null,Object? recordedRecoveryStartTime = freezed,Object? sessionHeartRateSamples = null,}) {
  return _then(_ActivityEntryUiState(
mode: null == mode ? _self.mode : mode // ignore: cast_nullable_to_non_nullable
as ActivityEntryFormMode,selectedActivityType: null == selectedActivityType ? _self.selectedActivityType : selectedActivityType // ignore: cast_nullable_to_non_nullable
as ActivityEntryType,titleText: null == titleText ? _self.titleText : titleText // ignore: cast_nullable_to_non_nullable
as String,selectedFeeling: freezed == selectedFeeling ? _self.selectedFeeling : selectedFeeling // ignore: cast_nullable_to_non_nullable
as ActivityEntryFeeling?,notesText: null == notesText ? _self.notesText : notesText // ignore: cast_nullable_to_non_nullable
as String,startDateText: null == startDateText ? _self.startDateText : startDateText // ignore: cast_nullable_to_non_nullable
as String,startTimeText: null == startTimeText ? _self.startTimeText : startTimeText // ignore: cast_nullable_to_non_nullable
as String,durationMinutesText: null == durationMinutesText ? _self.durationMinutesText : durationMinutesText // ignore: cast_nullable_to_non_nullable
as String,distanceText: null == distanceText ? _self.distanceText : distanceText // ignore: cast_nullable_to_non_nullable
as String,elevationText: null == elevationText ? _self.elevationText : elevationText // ignore: cast_nullable_to_non_nullable
as String,activeCaloriesText: null == activeCaloriesText ? _self.activeCaloriesText : activeCaloriesText // ignore: cast_nullable_to_non_nullable
as String,totalCaloriesText: null == totalCaloriesText ? _self.totalCaloriesText : totalCaloriesText // ignore: cast_nullable_to_non_nullable
as String,repetitionMode: null == repetitionMode ? _self.repetitionMode : repetitionMode // ignore: cast_nullable_to_non_nullable
as ActivityRepetitionEntryMode,repetitionTotalText: null == repetitionTotalText ? _self.repetitionTotalText : repetitionTotalText // ignore: cast_nullable_to_non_nullable
as String,repetitionSets: null == repetitionSets ? _self._repetitionSets : repetitionSets // ignore: cast_nullable_to_non_nullable
as List<ActivityRepetitionSetInput>,plannedWorkouts: null == plannedWorkouts ? _self._plannedWorkouts : plannedWorkouts // ignore: cast_nullable_to_non_nullable
as List<PlannedExerciseData>,selectedPlannedWorkoutId: freezed == selectedPlannedWorkoutId ? _self.selectedPlannedWorkoutId : selectedPlannedWorkoutId // ignore: cast_nullable_to_non_nullable
as String?,selectedPlannedWorkoutBaseline: freezed == selectedPlannedWorkoutBaseline ? _self.selectedPlannedWorkoutBaseline : selectedPlannedWorkoutBaseline // ignore: cast_nullable_to_non_nullable
as ActivityPlannedWorkoutBaseline?,selectedPlannedWorkoutActivityTypeId: freezed == selectedPlannedWorkoutActivityTypeId ? _self.selectedPlannedWorkoutActivityTypeId : selectedPlannedWorkoutActivityTypeId // ignore: cast_nullable_to_non_nullable
as String?,isLoadingPlannedWorkouts: null == isLoadingPlannedWorkouts ? _self.isLoadingPlannedWorkouts : isLoadingPlannedWorkouts // ignore: cast_nullable_to_non_nullable
as bool,isSavingPlannedWorkout: null == isSavingPlannedWorkout ? _self.isSavingPlannedWorkout : isSavingPlannedWorkout // ignore: cast_nullable_to_non_nullable
as bool,importedRoute: freezed == importedRoute ? _self.importedRoute : importedRoute // ignore: cast_nullable_to_non_nullable
as RouteFileImport?,recordedPauseIntervals: null == recordedPauseIntervals ? _self._recordedPauseIntervals : recordedPauseIntervals // ignore: cast_nullable_to_non_nullable
as List<ActivityPauseInterval>,recordedLaps: null == recordedLaps ? _self._recordedLaps : recordedLaps // ignore: cast_nullable_to_non_nullable
as List<ExerciseLapData>,recordedMarkers: null == recordedMarkers ? _self._recordedMarkers : recordedMarkers // ignore: cast_nullable_to_non_nullable
as List<ActivityRecordingMarker>,writePermissions: null == writePermissions ? _self._writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,routeImport: null == routeImport ? _self.routeImport : routeImport // ignore: cast_nullable_to_non_nullable
as CommandState<void>,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as ActivityEntryError?,detailError: freezed == detailError ? _self.detailError : detailError // ignore: cast_nullable_to_non_nullable
as ScreenError?,validationErrors: null == validationErrors ? _self._validationErrors : validationErrors // ignore: cast_nullable_to_non_nullable
as Set<ActivityEntryValidationError>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,isRecordingDraft: null == isRecordingDraft ? _self.isRecordingDraft : isRecordingDraft // ignore: cast_nullable_to_non_nullable
as bool,recordedBleSamples: null == recordedBleSamples ? _self.recordedBleSamples : recordedBleSamples // ignore: cast_nullable_to_non_nullable
as BleRecordingSampleBuffer,recordedRecoveryStartTime: freezed == recordedRecoveryStartTime ? _self.recordedRecoveryStartTime : recordedRecoveryStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,sessionHeartRateSamples: null == sessionHeartRateSamples ? _self._sessionHeartRateSamples : sessionHeartRateSamples // ignore: cast_nullable_to_non_nullable
as List<HeartRateSample>,
  ));
}

/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get routeImport {
  
  return $CommandStateCopyWith<void, $Res>(_self.routeImport, (value) {
    return _then(_self.copyWith(routeImport: value));
  });
}/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get save {
  
  return $CommandStateCopyWith<void, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}/// Create a copy of ActivityEntryUiState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BleRecordingSampleBufferCopyWith<$Res> get recordedBleSamples {
  
  return $BleRecordingSampleBufferCopyWith<$Res>(_self.recordedBleSamples, (value) {
    return _then(_self.copyWith(recordedBleSamples: value));
  });
}
}

// dart format on
