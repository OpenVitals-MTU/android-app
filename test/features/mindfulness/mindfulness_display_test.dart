import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/mindfulness_models.dart';
import 'package:openvitals/domain/query/mindfulness_period_data.dart';
import 'package:openvitals/features/mindfulness/application/mindfulness_display.dart';

/// The derivation the screen used to do in its build path, now a pure function
/// the view-model calls once per load — so it can be tested with no widget.
MindfulnessSession _session(DateTime start, Duration duration) =>
    MindfulnessSession(
      id: start.toIso8601String(),
      title: null,
      startTime: start,
      endTime: start.add(duration),
      durationMs: duration.inMilliseconds,
      source: 'Test',
    );

void main() {
  final monday = DateTime(2026, 3, 2, 6);
  final tuesday = DateTime(2026, 3, 3, 7);

  test('summary folds total, count, average and longest', () {
    final display = buildMindfulnessDisplay(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 30)),
      _session(tuesday, const Duration(minutes: 10)),
    ]));

    expect(display.totalMs, const Duration(minutes: 40).inMilliseconds);
    expect(display.totalMinutes, 40);
    expect(display.sessionCount, 2);
    expect(display.averageDurationMs, const Duration(minutes: 20).inMilliseconds);
    expect(display.longestSessionMs, const Duration(minutes: 30).inMilliseconds);
  });

  test('an empty period derives zeroes, not nulls', () {
    final display = buildMindfulnessDisplay(const MindfulnessPeriodData());

    expect(display.sessionCount, 0);
    expect(display.totalMs, 0);
    expect(display.averageDurationMs, 0);
    expect(display.chartValues, isEmpty);
    expect(display.cumulativeSamples, isEmpty);
  });

  test('the bar series sums minutes per day, not per session', () {
    final display = buildMindfulnessDisplay(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 30)),
      _session(monday.add(const Duration(hours: 8)), const Duration(minutes: 15)),
      _session(tuesday, const Duration(minutes: 10)),
    ]));

    expect(display.chartValues.length, 2);
    expect(display.chartValues.first.value, 45.0);
    expect(display.chartValues.last.value, 10.0);
  });

  test('sessions are listed newest first', () {
    final display = buildMindfulnessDisplay(MindfulnessPeriodData(sessions: [
      _session(monday, const Duration(minutes: 30)),
      _session(tuesday, const Duration(minutes: 10)),
    ]));

    expect(display.sortedSessions.first.startTime, tuesday);
    expect(display.sortedSessions.last.startTime, monday);
  });
}
