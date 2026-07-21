import 'package:meta/meta.dart';

/// The nightly sleep window, in device-local clock hours. A night is captured
/// from [startHour] the previous evening to [endHour] the next morning (default
/// 18:00 → 10:00). Sessions that begin outside it are daytime naps, reported
/// apart from the night. Both hours are user-configurable (0..23) and replace
/// the old fixed rolling24h / noon / evening18h range modes.
@immutable
class SleepWindow {
  const SleepWindow({required this.startHour, required this.endHour});

  /// Hour of the previous evening the night window opens (default 18 = 6pm).
  final int startHour;

  /// Hour of the morning the night window closes (default 10 = 10am). Anything
  /// beginning after this, up to [startHour], is a daytime nap.
  final int endHour;

  static const SleepWindow defaultWindow =
      SleepWindow(startHour: 18, endHour: 10);

  @override
  bool operator ==(Object other) =>
      other is SleepWindow &&
      other.startHour == startHour &&
      other.endHour == endHour;

  @override
  int get hashCode => Object.hash(startHour, endHour);

  @override
  String toString() => 'SleepWindow($startHour:00-$endHour:00)';
}
