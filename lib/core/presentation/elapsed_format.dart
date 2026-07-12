/// Elapsed time as a stopwatch reads it: `7:03`, or `1:07:03` once it passes an
/// hour. Kotlin `formatRecordingElapsed` (in `ActivityRecordingSplitsUi.kt`).
///
/// Lives in core because it is not a recording concern: the session chart cards
/// label their x axis with it too, and `ui/` must not reach into `features/`.
String formatRecordingElapsed(Duration duration) {
  final totalSeconds = duration.inSeconds < 0 ? 0 : duration.inSeconds;
  final hours = totalSeconds ~/ 3600;
  final minutes = (totalSeconds % 3600) ~/ 60;
  final seconds = totalSeconds % 60;
  final mm = minutes.toString().padLeft(2, '0');
  final ss = seconds.toString().padLeft(2, '0');
  return hours > 0 ? '$hours:$mm:$ss' : '$minutes:$ss';
}
