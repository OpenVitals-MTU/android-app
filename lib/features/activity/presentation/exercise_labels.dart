import 'package:flutter/material.dart';

/// Human-readable workout labels + icons, ported from the Kotlin
/// `ExerciseLabels.kt`.
///
/// `exerciseType` is the Health Connect `ExerciseSessionRecord.EXERCISE_TYPE_*`
/// integer constant (the value the native bridge round-trips through the record
/// JSON), so labels/icons are resolved directly from those constants.

/// Health Connect `EXERCISE_TYPE_*` constant → human-readable label.
const Map<int, String> _exerciseTypeLabels = {
  0: 'Workout',
  2: 'Badminton',
  4: 'Baseball',
  5: 'Basketball',
  8: 'Cycling',
  9: 'Cycling (Stationary)',
  10: 'Boot Camp',
  11: 'Boxing',
  13: 'Calisthenics',
  14: 'Cricket',
  16: 'Dancing',
  25: 'Elliptical',
  26: 'Exercise Class',
  27: 'Fencing',
  28: 'American Football',
  29: 'Australian Football',
  31: 'Frisbee',
  32: 'Golf',
  33: 'Guided Breathing',
  34: 'Gymnastics',
  35: 'Handball',
  36: 'HIIT',
  37: 'Hiking',
  38: 'Ice Hockey',
  39: 'Ice Skating',
  44: 'Martial Arts',
  46: 'Paddling',
  47: 'Paragliding',
  48: 'Pilates',
  50: 'Racquetball',
  51: 'Rock Climbing',
  52: 'Roller Hockey',
  53: 'Rowing',
  54: 'Rowing Machine',
  55: 'Rugby',
  56: 'Running',
  57: 'Running Treadmill',
  58: 'Sailing',
  59: 'Scuba Diving',
  60: 'Skating',
  61: 'Skiing',
  62: 'Snowboarding',
  63: 'Snowshoeing',
  64: 'Soccer',
  65: 'Softball',
  66: 'Squash',
  68: 'Stair Climbing',
  69: 'Stair Climbing Machine',
  70: 'Strength Training',
  71: 'Stretching',
  72: 'Surfing',
  73: 'Open Water Swimming',
  74: 'Pool Swimming',
  75: 'Table Tennis',
  76: 'Tennis',
  78: 'Volleyball',
  79: 'Walking',
  80: 'Water Polo',
  81: 'Weightlifting',
  82: 'Wheelchair',
  83: 'Yoga',
};

/// A label for the workout type, falling back to "Exercise".
String exerciseTypeLabel(int exerciseType) =>
    _exerciseTypeLabels[exerciseType] ?? 'Exercise';

/// A representative icon for the workout type, resolved from keywords in the
/// label (a pragmatic port of the Kotlin `exerciseTypeIcon` mapping).
IconData exerciseTypeIcon(int exerciseType) {
  final name = (_exerciseTypeLabels[exerciseType] ?? '').toUpperCase();
  bool has(String token) => name.contains(token);

  if (has('RUN')) return Icons.directions_run;
  if (has('WALK') || has('HIK')) return Icons.directions_walk;
  if (has('CYCL') || has('BIK')) return Icons.directions_bike;
  if (has('SWIM') || has('WATER POLO')) return Icons.pool;
  if (has('ROW') || has('PADDL') || has('KAYAK')) return Icons.rowing;
  if (has('YOGA') || has('PILATES') || has('STRETCH') || has('BREATH')) {
    return Icons.self_improvement;
  }
  if (has('WEIGHT') ||
      has('STRENGTH') ||
      has('CALISTHEN') ||
      has('BOOT CAMP') ||
      has('HIIT')) {
    return Icons.fitness_center;
  }
  if (has('SKI') || has('SNOWBOARD') || has('SKAT')) {
    return Icons.downhill_skiing;
  }
  if (has('SURF')) return Icons.surfing;
  if (has('SAIL')) return Icons.sailing;
  if (has('GOLF')) return Icons.sports_golf;
  if (has('TENNIS') || has('BADMINTON') || has('SQUASH') || has('RACQUET')) {
    return Icons.sports_tennis;
  }
  if (has('BASKETBALL')) return Icons.sports_basketball;
  if (has('SOCCER')) return Icons.sports_soccer;
  if (has('FOOTBALL')) return Icons.sports_football;
  if (has('BASEBALL') || has('SOFTBALL')) return Icons.sports_baseball;
  if (has('CRICKET')) return Icons.sports_cricket;
  if (has('VOLLEYBALL')) return Icons.sports_volleyball;
  if (has('HANDBALL')) return Icons.sports_handball;
  if (has('HOCKEY')) return Icons.sports_hockey;
  if (has('MARTIAL') || has('BOXING')) return Icons.sports_martial_arts;
  if (has('GYMNASTICS')) return Icons.sports_gymnastics;
  if (has('WHEELCHAIR')) return Icons.accessible_forward;
  return Icons.fitness_center;
}
