import '../../../domain/model/ble_sensor_models.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/preferences/unit_system.dart';
import 'activity_entry_clock.dart';
import 'activity_entry_state.dart';
import '../../../domain/model/activity_entry_types.dart';
import 'routeimport/route_file_parser.dart';

/// Port of the Kotlin `ActivityEntryWriteRequestBuilder.kt` plus the shared
/// parse/format/estimate helpers from `ActivityEntryEditMapper.kt`. Pure Dart.

const double milesToMeters = 1609.344;
const double feetToMeters = 0.3048;
const int maxActivityDurationMinutes = 7 * 24 * 60;
const double defaultCalorieEstimateWeightKg = 70.0;
const double restingMet = 1.0;
const double runningKcalPerKgKm = 1.0;
const double walkingKcalPerKgKm = 0.55;
const int maxActivityRepetitions = 100000;
const int maxActivityRepetitionSets = 99;
const int maxActivityRestSeconds = 24 * 60 * 60;
const int maxActivityStepCount = 1000000;

ActivityWriteRequest? buildWriteRequest(
  ActivityEntryUiState state,
  UnitSystem unitSystem,
) {
  if (validateActivityEntry(state, unitSystem).isNotEmpty) return null;

  final sessionRange = activityEntrySessionRange(state);
  if (sessionRange == null) return null;
  final start = sessionRange.$1;
  var end = sessionRange.$2;
  final importedRoute = state.importedRoute;
  var routePoints = importedRoute?.points ?? const <ExerciseRoutePoint>[];
  if (routePoints.isNotEmpty) {
    if (!state.selectedActivityType.supportsGpsRoute) return null;
    if (importedRoute != null && !importedRoute.hasRecordedTimestamps) {
      routePoints = withActivityTimeRange(routePoints, start, end);
    } else {
      final firstPoint = routePoints.first;
      final lastPoint = routePoints.last;
      if (firstPoint.time.isBefore(start)) return null;
      if (!lastPoint.time.isBefore(end)) {
        end = lastPoint.time.add(const Duration(seconds: 1));
      }
    }
  }
  final supportsDistance = state.selectedActivityType.supportsDistance;
  final supportsElevation = state.selectedActivityType.supportsElevation;

  final double? distanceMeters;
  if (!supportsDistance) {
    distanceMeters = null;
  } else if (state.distanceText.trim().isNotEmpty &&
      importedRoute != null &&
      state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem)) {
    final value = importedRoute.distanceMeters;
    distanceMeters = value > 0.0 ? value : null;
  } else if (state.distanceText.trim().isNotEmpty) {
    final parsed = parseDistanceMeters(state.distanceText, unitSystem);
    if (parsed == null) return null;
    distanceMeters = parsed;
  } else if (routePoints.isNotEmpty) {
    final value = state.importedRoute?.distanceMeters;
    distanceMeters = (value != null && value > 0.0) ? value : null;
  } else {
    distanceMeters = null;
  }

  final double? elevationMeters;
  if (!supportsElevation) {
    elevationMeters = null;
  } else if (state.elevationText.trim().isNotEmpty &&
      importedRoute != null &&
      state.elevationText.trim() ==
          routeElevationInputText(importedRoute, unitSystem)) {
    final value = importedRoute.elevationGainedMeters;
    elevationMeters = value > 0.0 ? value : null;
  } else if (state.elevationText.trim().isNotEmpty) {
    final parsed = parseElevationMeters(state.elevationText, unitSystem);
    if (parsed == null) return null;
    elevationMeters = parsed;
  } else if (routePoints.isNotEmpty) {
    final value = state.importedRoute?.elevationGainedMeters;
    elevationMeters = (value != null && value > 0.0) ? value : null;
  } else {
    elevationMeters = null;
  }

  final double? activeCalories;
  if (state.activeCaloriesText.trim().isEmpty) {
    activeCalories = null;
  } else {
    final parsed = toPositiveDoubleOrNull(state.activeCaloriesText);
    if (parsed == null) return null;
    activeCalories = parsed;
  }
  final double? totalCalories;
  if (state.totalCaloriesText.trim().isEmpty) {
    totalCalories = null;
  } else {
    final parsed = toPositiveDoubleOrNull(state.totalCaloriesText);
    if (parsed == null) return null;
    totalCalories = parsed;
  }
  if (activeCalories != null &&
      totalCalories != null &&
      totalCalories < activeCalories) {
    return null;
  }
  final exerciseSegments = buildActivityExerciseSegments(state, start, end);
  if (exerciseSegments == null) return null;
  final int? stepsCount;
  if (state.selectedActivityType.supportsStepCounting) {
    if (state.repetitionTotalText.trim().isEmpty) {
      stepsCount = null;
    } else {
      final parsed =
          toPositiveLongOrNull(state.repetitionTotalText, maxActivityStepCount);
      if (parsed == null) return null;
      stepsCount = parsed;
    }
  } else {
    stepsCount = null;
  }
  final pauseIntervals =
      exerciseSegments.isEmpty && state.selectedActivityType.supportsGpsRoute
          ? insideActivityRange(state.recordedPauseIntervals, start, end)
          : const <ActivityPauseInterval>[];
  final laps =
      exerciseSegments.isEmpty && state.selectedActivityType.supportsGpsRoute
          ? insideLapActivityRange(state.recordedLaps, start, end)
          : const <ExerciseLapData>[];

  final trimmedTitle = state.titleText.trim();
  return ActivityWriteRequest(
    exerciseType: state.selectedActivityType.exerciseType,
    startTime: start,
    endTime: end,
    title: trimmedTitle.isNotEmpty
        ? trimmedTitle
        : state.selectedActivityType.defaultTitle,
    notes: state.activitySaveNotes(),
    plannedExerciseSessionId: state.selectedPlannedWorkoutId,
    routePoints: routePoints,
    pauseIntervals: pauseIntervals,
    laps: laps,
    exerciseSegments: exerciseSegments,
    stepsCount: stepsCount,
    distanceMeters: distanceMeters,
    elevationGainedMeters: elevationMeters,
    activeCaloriesKcal: activeCalories,
    totalCaloriesKcal: totalCalories,
    // A live recording fills `recordedBleSamples` from the paired sensors. An
    // IMPORT fills nothing -- which is why an imported activity had no graphs at
    // all: the file's heart rate, cadence and speed were parsed (now) but had
    // nowhere to go, and the same write path that carries a BLE session's series
    // was sitting right here unused.
    bleSamples: state.recordedBleSamples.isEmpty()
        ? (state.importedRoute?.bleSamples ?? const BleRecordingSampleBuffer())
        : state.recordedBleSamples,
  );
}

Set<ActivityEntryValidationError> validateActivityEntry(
  ActivityEntryUiState state,
  UnitSystem unitSystem,
) {
  final errors = <ActivityEntryValidationError>{};
  final startDate = parseStartDate(state.startDateText);
  final startTime = _parseStartTime(state.startTimeText);
  final durationMinutes = _durationMinutesOrNull(state.durationMinutesText);

  if (startDate == null) errors.add(ActivityEntryValidationError.startDateInvalid);
  if (startTime == null) errors.add(ActivityEntryValidationError.startTimeInvalid);
  if (durationMinutes == null) {
    errors.add(ActivityEntryValidationError.durationInvalid);
  }

  final importedRoute = state.importedRoute;
  final routePoints = importedRoute?.points ?? const <ExerciseRoutePoint>[];
  if (routePoints.isNotEmpty && !state.selectedActivityType.supportsGpsRoute) {
    errors.add(ActivityEntryValidationError.activityTypeDoesNotSupportRoute);
  }
  if (routePoints.isNotEmpty &&
      (importedRoute == null || importedRoute.hasRecordedTimestamps) &&
      startDate != null &&
      startTime != null) {
    final start = _localDateTime(startDate, startTime);
    if (routePoints.first.time.isBefore(start)) {
      errors.add(ActivityEntryValidationError.startTimeAfterRouteStart);
    }
  }

  if (state.distanceText.trim().isNotEmpty &&
      state.selectedActivityType.supportsDistance) {
    if (importedRoute != null &&
        state.distanceText.trim() ==
            routeDistanceInputText(importedRoute, unitSystem)) {
      // valid: matches the imported route summary
    } else if (parseDistanceMeters(state.distanceText, unitSystem) == null) {
      errors.add(ActivityEntryValidationError.distanceInvalid);
    }
  }

  if (state.elevationText.trim().isNotEmpty &&
      state.selectedActivityType.supportsElevation) {
    if (importedRoute != null &&
        state.elevationText.trim() ==
            routeElevationInputText(importedRoute, unitSystem)) {
      // valid
    } else if (parseElevationMeters(state.elevationText, unitSystem) == null) {
      errors.add(ActivityEntryValidationError.elevationInvalid);
    }
  }

  double? activeCalories;
  if (state.activeCaloriesText.trim().isNotEmpty) {
    activeCalories = toPositiveDoubleOrNull(state.activeCaloriesText);
    if (activeCalories == null) {
      errors.add(ActivityEntryValidationError.activeCaloriesInvalid);
    }
  }
  double? totalCalories;
  if (state.totalCaloriesText.trim().isNotEmpty) {
    totalCalories = toPositiveDoubleOrNull(state.totalCaloriesText);
    if (totalCalories == null) {
      errors.add(ActivityEntryValidationError.totalCaloriesInvalid);
    }
  }
  if (activeCalories != null &&
      totalCalories != null &&
      totalCalories < activeCalories) {
    errors.add(ActivityEntryValidationError.totalCaloriesBelowActive);
  }
  if (startDate != null && startTime != null && durationMinutes != null) {
    final start = _localDateTime(startDate, startTime);
    final end = start.add(Duration(minutes: durationMinutes));
    if (!_hasValidRepetitionInput(state, start, end)) {
      errors.add(ActivityEntryValidationError.repetitionsInvalid);
    }
  }

  return errors;
}

List<ActivityExerciseSegmentWrite>? buildActivityExerciseSegments(
  ActivityEntryUiState state,
  DateTime start,
  DateTime end,
) {
  final type = state.selectedActivityType;
  if (type.repetitionUnit != ActivityRepetitionUnit.repetitions) {
    return const <ActivityExerciseSegmentWrite>[];
  }
  final segmentType = type.segmentType;
  if (segmentType == null) return null;
  switch (state.repetitionMode) {
    case ActivityRepetitionEntryMode.total:
      final repetitions =
          toPositiveIntOrNull(state.repetitionTotalText, maxActivityRepetitions);
      if (repetitions == null) return null;
      return [
        ActivityExerciseSegmentWrite(
          startTime: start,
          endTime: end,
          segmentType: segmentType,
          repetitions: repetitions,
          setIndex: 0,
        ),
      ];
    case ActivityRepetitionEntryMode.sets:
      return buildSetExerciseSegments(state, start, end, segmentType);
  }
}

List<ActivityExerciseSegmentWrite>? buildSetExerciseSegments(
  ActivityEntryUiState state,
  DateTime start,
  DateTime end,
  int segmentType,
) {
  if (state.repetitionSets.isEmpty ||
      state.repetitionSets.length > maxActivityRepetitionSets) {
    return null;
  }
  final sets = <_ParsedRepetitionSet>[];
  for (final input in state.repetitionSets) {
    final repetitions =
        toPositiveIntOrNull(input.repetitionsText, maxActivityRepetitions);
    if (repetitions == null) return null;
    final restSeconds =
        toOptionalNonNegativeLongOrNull(input.restMinutesText, maxActivityRestSeconds);
    if (restSeconds == null) return null;
    sets.add(_ParsedRepetitionSet(repetitions, restSeconds));
  }
  final durationSeconds = _atLeast(end.difference(start).inSeconds, 1);
  final restSeconds = sets.fold<int>(0, (sum, set) => sum + set.restSeconds);
  final activeSeconds = durationSeconds - restSeconds;
  if (activeSeconds < sets.length) return null;

  var cursor = start;
  var activeRemainder = activeSeconds % sets.length;
  final baseActiveSeconds = activeSeconds ~/ sets.length;
  final result = <ActivityExerciseSegmentWrite>[];
  for (var index = 0; index < sets.length; index++) {
    final set = sets[index];
    final thisActiveSeconds = baseActiveSeconds + (activeRemainder > 0 ? 1 : 0);
    if (activeRemainder > 0) activeRemainder -= 1;
    final activeEnd = cursor.add(Duration(seconds: thisActiveSeconds));
    result.add(
      ActivityExerciseSegmentWrite(
        startTime: cursor,
        endTime: activeEnd,
        segmentType: segmentType,
        repetitions: set.repetitions,
        setIndex: index,
      ),
    );
    if (set.restSeconds > 0) {
      final restEnd = activeEnd.add(Duration(seconds: set.restSeconds));
      result.add(
        ActivityExerciseSegmentWrite(
          startTime: activeEnd,
          endTime: restEnd,
          segmentType: ExerciseSegmentType.rest,
        ),
      );
      cursor = restEnd;
    } else {
      cursor = activeEnd;
    }
  }
  return result;
}

bool _hasValidRepetitionInput(
  ActivityEntryUiState state,
  DateTime start,
  DateTime end,
) {
  switch (state.selectedActivityType.repetitionUnit) {
    case null:
      return true;
    case ActivityRepetitionUnit.steps:
      return state.repetitionTotalText.trim().isEmpty ||
          toPositiveLongOrNull(state.repetitionTotalText, maxActivityStepCount) !=
              null;
    case ActivityRepetitionUnit.repetitions:
      return buildActivityExerciseSegments(state, start, end) != null;
  }
}

class _ParsedRepetitionSet {
  const _ParsedRepetitionSet(this.repetitions, this.restSeconds);

  final int repetitions;
  final int restSeconds;
}

(DateTime, DateTime)? activityEntrySessionRange(ActivityEntryUiState state) {
  final startDate = parseStartDate(state.startDateText);
  if (startDate == null) return null;
  final startTime = _parseStartTime(state.startTimeText);
  if (startTime == null) return null;
  final durationMinutes = _durationMinutesOrNull(state.durationMinutesText);
  if (durationMinutes == null) return null;
  final start = _localDateTime(startDate, startTime);
  final end = start.add(Duration(minutes: durationMinutes));
  return (start, end);
}

/// A blank form. [writePermissions] is the baseline set the form has to hold
/// before it can save anything — passed in rather than read from a repository, so
/// this stays a pure function of its inputs and callers that already have the set
/// (or do not need it) are not forced to hand over a repository for it.
ActivityEntryUiState initialActivityEntryState(
  ActivityEntryClock clock,
  Set<String> writePermissions, {
  ActivityEntryType? selectedActivityType,
}) {
  final now = _truncateToMinute(clock.nowInZone());
  return ActivityEntryUiState(
    selectedActivityType: selectedActivityType ?? defaultActivityEntryTypes.first,
    startDateText: isoLocalDate(now),
    startTimeText: timeFormatterText(now),
    writePermissions: writePermissions,
  );
}

ActivityEntryUiState clearedAfterSaveState(
  ActivityEntryClock clock,
  Set<String> writePermissions,
  ActivityEntryType selectedType,
) =>
    initialActivityEntryState(clock, writePermissions,
        selectedActivityType: selectedType);

// ── Calorie estimation (ActivityEntryEditMapper.kt) ──────────────────────────

class ActivityCalorieEstimate {
  const ActivityCalorieEstimate(this.activeCaloriesText, this.totalCaloriesText);

  final String activeCaloriesText;
  final String totalCaloriesText;
}

ActivityCalorieEstimate? activityCalorieEstimate({
  required ActivityEntryType activityType,
  required double? distanceMeters,
  required String durationMinutesText,
}) {
  if (!activityType.supportsGpsRoute) return null;
  final durationMinutes = _durationMinutesOrNull(durationMinutesText);
  if (durationMinutes == null) return null;
  final hours = durationMinutes / 60.0;
  final met = activityMet(activityType.exerciseType);
  if (met == null) return null;
  final restingCalories = defaultCalorieEstimateWeightKg * hours * restingMet;
  final activeByMet =
      _atLeastD(met - restingMet, 0.0) * defaultCalorieEstimateWeightKg * hours;
  final activeByDistance = distanceBasedActiveCalories(
        exerciseType: activityType.exerciseType,
        distanceMeters: distanceMeters,
      ) ??
      0.0;
  final active = activeByMet > activeByDistance ? activeByMet : activeByDistance;
  if (active <= 0.0) return null;
  return ActivityCalorieEstimate(
    toCaloriesInputText(active),
    toCaloriesInputText(active + restingCalories),
  );
}

double? activityMet(int exerciseType) {
  if (exerciseType == ExerciseSessionType.running) return 9.8;
  if (exerciseType == ExerciseSessionType.biking) return 7.5;
  if (exerciseType == ExerciseSessionType.walking) return 3.5;
  if (exerciseType == ExerciseSessionType.hiking) return 6.0;
  if (exerciseType == ExerciseSessionType.wheelchair) return 4.0;
  if (exerciseType == ExerciseSessionType.rowing ||
      exerciseType == ExerciseSessionType.paddling) {
    return 7.0;
  }
  if (exerciseType == ExerciseSessionType.skiing) return 7.0;
  if (exerciseType == ExerciseSessionType.snowboarding) return 5.3;
  if (exerciseType == ExerciseSessionType.snowshoeing) return 8.0;
  if (exerciseType == ExerciseSessionType.skating) return 7.0;
  if (exerciseType == ExerciseSessionType.sailing) return 3.0;
  if (exerciseType == ExerciseSessionType.surfing) return 3.0;
  if (exerciseType == ExerciseSessionType.swimmingOpenWater) return 8.0;
  if (exerciseType == ExerciseSessionType.golf) return 4.8;
  return null;
}

double? distanceBasedActiveCalories({
  required int exerciseType,
  required double? distanceMeters,
}) {
  if (distanceMeters == null || distanceMeters <= 0.0) return null;
  final distanceKm = distanceMeters / 1000.0;
  final double kcalPerKgKm;
  if (exerciseType == ExerciseSessionType.running ||
      exerciseType == ExerciseSessionType.hiking ||
      exerciseType == ExerciseSessionType.snowshoeing) {
    kcalPerKgKm = runningKcalPerKgKm;
  } else if (exerciseType == ExerciseSessionType.walking ||
      exerciseType == ExerciseSessionType.wheelchair) {
    kcalPerKgKm = walkingKcalPerKgKm;
  } else {
    return null;
  }
  return defaultCalorieEstimateWeightKg * distanceKm * kcalPerKgKm;
}

// ── Unit + text helpers (ActivityEntryEditMapper.kt) ─────────────────────────

double? parseDistanceMeters(String text, UnitSystem unitSystem) {
  final value = toPositiveDoubleOrNull(text);
  if (value == null) return null;
  return unitSystem == UnitSystem.metric ? value * 1000.0 : value * milesToMeters;
}

double? parseElevationMeters(String text, UnitSystem unitSystem) {
  final value = toPositiveDoubleOrNull(text);
  if (value == null) return null;
  return unitSystem == UnitSystem.metric ? value : value * feetToMeters;
}

String routeDistanceInputText(RouteFileImport routeImport, UnitSystem unitSystem) {
  final distance = routeImport.distanceMeters;
  if (distance <= 0.0) return '';
  final value =
      unitSystem == UnitSystem.metric ? distance / 1000.0 : distance / milesToMeters;
  return toInputText(value, 2);
}

String routeElevationInputText(RouteFileImport routeImport, UnitSystem unitSystem) {
  final elevation = routeImport.elevationGainedMeters;
  if (elevation <= 0.0) return '';
  final value =
      unitSystem == UnitSystem.metric ? elevation : elevation / feetToMeters;
  return toInputText(value, 1);
}

String toDistanceInputText(double meters, UnitSystem unitSystem) {
  final value =
      unitSystem == UnitSystem.metric ? meters / 1000.0 : meters / milesToMeters;
  return toInputText(value, 2);
}

String toElevationInputText(double meters, UnitSystem unitSystem) {
  final value = unitSystem == UnitSystem.metric ? meters : meters / feetToMeters;
  return toInputText(value, 1);
}

List<ExerciseRoutePoint> withActivityTimeRange(
  List<ExerciseRoutePoint> points,
  DateTime start,
  DateTime end,
) {
  if (points.isEmpty) return const [];
  final rawTotal = end.difference(start).inMilliseconds;
  final totalMillis = rawTotal < points.length ? points.length : rawTotal;
  final lastOffset = (totalMillis - 1) < 0 ? 0 : (totalMillis - 1);
  final result = <ExerciseRoutePoint>[];
  for (var index = 0; index < points.length; index++) {
    final offset =
        points.length == 1 ? 0 : (lastOffset * index) ~/ (points.length - 1);
    result.add(points[index].copyWith(time: start.add(Duration(milliseconds: offset))));
  }
  return result;
}

List<ActivityPauseInterval> insideActivityRange(
  List<ActivityPauseInterval> intervals,
  DateTime start,
  DateTime end,
) {
  final sorted = [...intervals]..sort((a, b) => a.startTime.compareTo(b.startTime));
  return sorted
      .where((interval) =>
          !interval.startTime.isBefore(start) &&
          interval.startTime.isBefore(interval.endTime) &&
          !interval.endTime.isAfter(end))
      .toList();
}

List<ExerciseLapData> insideLapActivityRange(
  List<ExerciseLapData> laps,
  DateTime start,
  DateTime end,
) {
  final sorted = [...laps]..sort((a, b) => a.startTime.compareTo(b.startTime));
  return sorted
      .where((lap) =>
          !lap.startTime.isBefore(start) &&
          lap.startTime.isBefore(lap.endTime) &&
          !lap.endTime.isAfter(end))
      .toList();
}

String toInputText(double value, int maxFractionDigits) {
  var text = value.toStringAsFixed(maxFractionDigits);
  if (text.contains('.')) {
    text = text.replaceFirst(RegExp(r'0+$'), '');
    text = text.replaceFirst(RegExp(r'\.$'), '');
  }
  return text;
}

String toCaloriesInputText(double value) {
  final rounded = value.round();
  return (rounded < 1 ? 1 : rounded).toString();
}

double? toPositiveDoubleOrNull(String text) {
  final value = double.tryParse(text.trim().replaceAll(',', '.'));
  return (value != null && value > 0.0) ? value : null;
}

int? toPositiveIntOrNull(String text, int max) {
  final value = int.tryParse(text.trim());
  return (value != null && value >= 1 && value <= max) ? value : null;
}

int? toPositiveLongOrNull(String text, int max) {
  final value = int.tryParse(text.trim());
  return (value != null && value >= 1 && value <= max) ? value : null;
}

int? toOptionalNonNegativeLongOrNull(String text, int max) {
  final trimmed = text.trim();
  if (trimmed.isEmpty) return 0;
  final value = int.tryParse(trimmed);
  return (value != null && value >= 0 && value <= max) ? value : null;
}

bool containsAny(String source, List<String> values) =>
    values.any(source.contains);

/// yyyy-MM-dd. Port of `DateTimeFormatter.ISO_LOCAL_DATE.format`.
String isoLocalDate(DateTime dateTime) =>
    '${dateTime.year.toString().padLeft(4, '0')}-'
    '${dateTime.month.toString().padLeft(2, '0')}-'
    '${dateTime.day.toString().padLeft(2, '0')}';

/// H:mm. Port of the Kotlin `TimeFormatter`.
String timeFormatterText(DateTime dateTime) =>
    '${dateTime.hour}:${dateTime.minute.toString().padLeft(2, '0')}';

LocalDate? parseStartDate(String text) {
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(text.trim());
  if (match == null) return null;
  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  final day = int.parse(match.group(3)!);
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  final probe = DateTime(year, month, day);
  if (probe.year != year || probe.month != month || probe.day != day) return null;
  return LocalDate(year, month, day);
}

_LocalTime? _parseStartTime(String text) {
  final match = RegExp(r'^(\d{1,2}):(\d{2})$').firstMatch(text.trim());
  if (match == null) return null;
  final hour = int.parse(match.group(1)!);
  final minute = int.parse(match.group(2)!);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
  return _LocalTime(hour, minute);
}

DateTime _localDateTime(LocalDate date, _LocalTime time) =>
    DateTime(date.year, date.month, date.day, time.hour, time.minute);

class _LocalTime {
  const _LocalTime(this.hour, this.minute);
  final int hour;
  final int minute;
}

int? _durationMinutesOrNull(String text) {
  final value = int.tryParse(text.trim());
  return (value != null && value >= 1 && value <= maxActivityDurationMinutes)
      ? value
      : null;
}

DateTime _truncateToMinute(DateTime value) =>
    DateTime(value.year, value.month, value.day, value.hour, value.minute);

int _atLeast(int value, int min) => value < min ? min : value;
double _atLeastD(double value, double min) => value < min ? min : value;
