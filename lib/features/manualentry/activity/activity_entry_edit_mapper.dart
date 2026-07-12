import '../../../data/repository/contract/activity_repository.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/preferences/unit_system.dart';
import 'activity_entry_clock.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'activity_entry_write_request_builder.dart';
import 'routeimport/route_file_parser.dart';

/// Port of the Kotlin `ActivityEntryEditMapper.kt` — maps a stored [ExerciseData]
/// into editable [ActivityEntryUiState], plus route-import type inference.

ActivityEntryUiState exerciseToEditState(
  ExerciseData workout, {
  required UnitSystem unitSystem,
  required ActivityEntryClock clock,
  required ActivityRepository repository,
  required bool canWrite,
  required bool isCheckingPermission,
}) {
  final selectedType = inferStoredActivityType(workout);
  final route = workout.route;
  RouteFileImport? routeImport;
  if (route.status == ExerciseRouteStatus.data && route.points.isNotEmpty) {
    routeImport = RouteFileImport(
      fileName: null,
      points: route.points,
      distanceMeters: workout.totalDistanceMeters ?? 0.0,
      elevationGainedMeters: workout.elevationGainedMeters ?? 0.0,
      startTime: workout.startTime,
      endTime: workout.endTime,
      name: workout.title,
      description: workout.notes,
      originalPointCount: route.points.length,
    );
  }
  final start = clock.toZone(workout.startTime);
  final durationSeconds =
      _atLeast(workout.endTime.difference(workout.startTime).inSeconds, 1);
  final durationMinutes =
      (durationSeconds / 60.0).ceil().clamp(1, maxActivityDurationMinutes);
  final repetitionEditState = _toRepetitionEditState(workout, selectedType);

  return ActivityEntryUiState(
    mode: routeImport == null
        ? ActivityEntryFormMode.manual
        : ActivityEntryFormMode.routeImport,
    selectedActivityType: selectedType,
    titleText: workout.title ?? '',
    notesText: workout.notes ?? '',
    startDateText: isoLocalDate(start),
    startTimeText: timeFormatterText(start),
    durationMinutesText: durationMinutes.toString(),
    distanceText: (workout.totalDistanceMeters != null &&
            workout.totalDistanceMeters! > 0.0)
        ? toDistanceInputText(workout.totalDistanceMeters!, unitSystem)
        : '',
    elevationText: (workout.elevationGainedMeters != null &&
            workout.elevationGainedMeters! > 0.0)
        ? toElevationInputText(workout.elevationGainedMeters!, unitSystem)
        : '',
    activeCaloriesText:
        (workout.activeCaloriesKcal != null && workout.activeCaloriesKcal! > 0.0)
            ? toInputText(workout.activeCaloriesKcal!, 1)
            : '',
    totalCaloriesText:
        (workout.totalCaloriesKcal != null && workout.totalCaloriesKcal! > 0.0)
            ? toInputText(workout.totalCaloriesKcal!, 1)
            : '',
    repetitionMode: repetitionEditState.mode,
    repetitionTotalText: repetitionEditState.totalText,
    repetitionSets: repetitionEditState.sets,
    importedRoute: routeImport,
    recordedPauseIntervals: workout.segments
        .where((segment) => segment.segmentType == ExerciseSegmentType.pause)
        .map((segment) => ActivityPauseInterval(
              startTime: segment.startTime,
              endTime: segment.endTime,
            ))
        .toList(),
    recordedLaps: workout.laps,
    writePermissions: repository.activityWritePermissions(),
    canWrite: canWrite,
    isCheckingPermission: isCheckingPermission,
    editRecordId: workout.id,
  );
}

ActivityEntryType inferStoredActivityType(ExerciseData workout) {
  final titleText = (workout.title ?? '').toLowerCase();
  final activeSegments = workout.segments
      .where((segment) =>
          segment.segmentType != ExerciseSegmentType.pause &&
          segment.segmentType != ExerciseSegmentType.rest)
      .toList();
  final exerciseType = workout.exerciseType;
  if (exerciseType == ExerciseSessionType.runningTreadmill) {
    return defaultActivityEntryTypes.firstWhere((t) => t.id == 'treadmill');
  }
  if (exerciseType == ExerciseSessionType.calisthenics &&
      activeSegments.any((s) => s.segmentType == ExerciseSegmentType.pullUp)) {
    return defaultActivityEntryTypes.firstWhere((t) => t.id == 'pull_ups');
  }
  if (exerciseType == ExerciseSessionType.calisthenics &&
      activeSegments.any((s) => s.segmentType == ExerciseSegmentType.jumpRope)) {
    return defaultActivityEntryTypes.firstWhere((t) => t.id == 'rope_skipping');
  }
  if (exerciseType == ExerciseSessionType.calisthenics &&
      titleText.contains('push')) {
    return defaultActivityEntryTypes.firstWhere((t) => t.id == 'push_ups');
  }
  if (exerciseType == ExerciseSessionType.gymnastics &&
      titleText.contains('trampoline')) {
    return defaultActivityEntryTypes.firstWhere((t) => t.id == 'trampoline_jumping');
  }
  return defaultActivityEntryTypes.firstWhereOrNull(
        (t) => t.exerciseType == exerciseType && !t.isRepetitionLike,
      ) ??
      defaultActivityEntryTypes
          .firstWhereOrNull((t) => t.exerciseType == exerciseType) ??
      defaultActivityEntryTypes.first;
}

class _RepetitionEditState {
  const _RepetitionEditState({
    this.mode = ActivityRepetitionEntryMode.total,
    this.totalText = '',
    this.sets = const [ActivityRepetitionSetInput()],
  });

  final ActivityRepetitionEntryMode mode;
  final String totalText;
  final List<ActivityRepetitionSetInput> sets;
}

_RepetitionEditState _toRepetitionEditState(
  ExerciseData workout,
  ActivityEntryType type,
) {
  if (type.repetitionUnit == ActivityRepetitionUnit.steps) {
    final steps = workout.steps;
    return _RepetitionEditState(
      totalText: (steps != null && steps > 0) ? steps.toString() : '',
    );
  }
  if (type.repetitionUnit != ActivityRepetitionUnit.repetitions) {
    return const _RepetitionEditState();
  }

  final activeSegments = workout.segments
      .where((s) => s.segmentType == type.segmentType && s.repetitions > 0)
      .toList()
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  if (activeSegments.isEmpty) return const _RepetitionEditState();
  if (activeSegments.length == 1) {
    return _RepetitionEditState(
      totalText: activeSegments.first.repetitions.toString(),
    );
  }

  final sortedSegments = [...workout.segments]
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  final sets = <ActivityRepetitionSetInput>[];
  for (var index = 0; index < activeSegments.length; index++) {
    final segment = activeSegments[index];
    final next = index + 1 < activeSegments.length
        ? activeSegments[index + 1]
        : null;
    final rest = sortedSegments.firstWhereOrNull((s) =>
        s.segmentType == ExerciseSegmentType.rest &&
        !s.startTime.isBefore(segment.endTime) &&
        (next == null || !s.endTime.isAfter(next.startTime)));
    var restText = '';
    if (rest != null) {
      final seconds = rest.endTime.difference(rest.startTime).inSeconds;
      if (seconds > 0) restText = seconds.toString();
    }
    sets.add(
      ActivityRepetitionSetInput(
        repetitionsText: segment.repetitions.toString(),
        restMinutesText: restText,
      ),
    );
  }
  return _RepetitionEditState(
    mode: ActivityRepetitionEntryMode.sets,
    sets: sets.isEmpty ? const [ActivityRepetitionSetInput()] : sets,
  );
}

/// Port of the Kotlin `inferActivityType` (route-import type inference).
ActivityEntryType inferActivityType(
  RouteFileImport routeImport,
  ActivityEntryType currentType,
) {
  // The file's DECLARED sport is asked first, and on its own.
  //
  // Everything used to be shovelled into one string — sport, activity name and
  // FILE NAME — and matched by substring. So `Indoor_CyclingiSmoothRun.fit`
  // imported a 27 km bike ride as a RUN: the exporter's name is in the file
  // name, `run` is tested before `cycling`, and the FIT sport (which said
  // cycling, and knows) was outvoted by a word in a file name. Reordering the
  // tests would not fix it — any name can contain any word.
  //
  // A file that names no sport (GPX, KML) still falls back to the name and the
  // file name, which is all it has.
  final declared = routeImport.type?.toLowerCase().trim() ?? '';
  final guessed = [
    if (routeImport.name != null) routeImport.name!,
    if (routeImport.fileName != null) routeImport.fileName!,
  ].join(' ').toLowerCase();

  // ...but only when it actually NAMES something. FIT's `training` and `fitness
  // equipment` are its generic buckets — the file saying "training" is the file
  // saying it does not know — and there the name and file name are all there is:
  // `Functional Strength Training.fit` is a strength session, and only its name
  // says so.
  final declaredType = declared.isEmpty ? null : _matchExerciseType(declared);
  final declaredNamesIt =
      declaredType != null && declaredType != ExerciseSessionType.otherWorkout;

  final exerciseType = declaredNamesIt
      ? declaredType
      : (_matchExerciseType(guessed) ?? declaredType);

  final requiresGpsRoute = routeImport.points.isNotEmpty;
  return defaultActivityEntryTypes.firstWhereOrNull((t) =>
          t.exerciseType == exerciseType &&
          (!requiresGpsRoute || t.supportsGpsRoute)) ??
      (!requiresGpsRoute || currentType.supportsGpsRoute
          ? currentType
          : defaultActivityEntryTypes
              .firstWhere((t) => !requiresGpsRoute || t.supportsGpsRoute));
}

/// The Health Connect exercise type [sourceText] names, or null when it names
/// none.
int? _matchExerciseType(String sourceText) {
  int? exerciseType;
  if (containsAny(sourceText, ['treadmill'])) {
    exerciseType = ExerciseSessionType.runningTreadmill;
  } else if (containsAny(sourceText, ['strength', 'weight lifting', 'weightlifting'])) {
    exerciseType = ExerciseSessionType.strengthTraining;
  } else if (containsAny(sourceText, ['snowboard'])) {
    exerciseType = ExerciseSessionType.snowboarding;
  } else if (containsAny(sourceText, ['snowshoe'])) {
    exerciseType = ExerciseSessionType.snowshoeing;
  } else if (containsAny(sourceText, ['ski'])) {
    exerciseType = ExerciseSessionType.skiing;
  } else if (containsAny(sourceText, ['hike', 'hiking'])) {
    exerciseType = ExerciseSessionType.hiking;
  } else if (containsAny(sourceText, ['run', 'running', 'jog'])) {
    exerciseType = ExerciseSessionType.running;
    // Before the plain bike: a trainer ride is a different exercise type, and a
    // stationary bike that imported as outdoor cycling would put a 27 km ride on
    // a map it never touched.
  } else if (containsAny(
      sourceText, ['indoor cycling', 'stationary bike', 'spin'])) {
    exerciseType = ExerciseSessionType.bikingStationary;
  } else if (containsAny(
      sourceText, ['bike', 'biking', 'bicycle', 'cycling', 'cycle', 'ride'])) {
    exerciseType = ExerciseSessionType.biking;
  } else if (containsAny(sourceText, ['walk', 'walking'])) {
    exerciseType = ExerciseSessionType.walking;
  } else if (containsAny(sourceText, ['wheelchair'])) {
    exerciseType = ExerciseSessionType.wheelchair;
  } else if (containsAny(sourceText, ['row', 'rowing'])) {
    exerciseType = ExerciseSessionType.rowing;
  } else if (containsAny(sourceText, ['paddle', 'kayak', 'canoe'])) {
    exerciseType = ExerciseSessionType.paddling;
  } else if (containsAny(sourceText, ['skate', 'skating'])) {
    exerciseType = ExerciseSessionType.skating;
  } else if (containsAny(sourceText, ['sail', 'sailing'])) {
    exerciseType = ExerciseSessionType.sailing;
  } else if (containsAny(sourceText, ['surf', 'surfing'])) {
    exerciseType = ExerciseSessionType.surfing;
  } else if (containsAny(sourceText, ['swim', 'swimming'])) {
    exerciseType = ExerciseSessionType.swimmingOpenWater;
  } else if (containsAny(sourceText, ['golf'])) {
    exerciseType = ExerciseSessionType.golf;
  } else if (containsAny(sourceText, ['training', 'workout', 'fitness'])) {
    exerciseType = ExerciseSessionType.otherWorkout;
  }
  return exerciseType;
}

int _atLeast(int value, int min) => value < min ? min : value;

extension _FirstWhereOrNull<E> on List<E> {
  E? firstWhereOrNull(bool Function(E) test) {
    for (final element in this) {
      if (test(element)) return element;
    }
    return null;
  }
}
