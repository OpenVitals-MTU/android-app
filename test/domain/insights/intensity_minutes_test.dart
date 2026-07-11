import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/cardio_load.dart';
import 'package:openvitals/domain/insights/intensity_minutes.dart';
import 'package:openvitals/domain/model/heart_models.dart';

final DateTime _start = DateTime.parse('2026-06-10T10:00:00Z');

List<HeartRateSample> _samples({required int bpm, required int minutes}) => [
      for (var minute = 0; minute <= minutes; minute++)
        HeartRateSample(
          time: _start.add(Duration(seconds: minute * 60)),
          beatsPerMinute: bpm,
          source: 'watch',
        ),
    ];

void main() {
  test('heartRateReserveCountsModerateMinutes', () {
    final estimate = calculateIntensityMinutes(
      _samples(bpm: 120, minutes: 30),
      60,
      null,
      180,
      [
        CardioLoadTimeWindow(
            start: _start, end: _start.add(const Duration(seconds: 30 * 60))),
      ],
      const [],
      null,
      null,
    );

    expect(estimate.moderateMinutes, 30);
    expect(estimate.vigorousMinutes, 0);
    expect(estimate.moderateEquivalentMinutes, 30);
    expect(estimate.confidence, IntensityMinutesConfidence.high);
    expect(estimate.method, IntensityMinutesMethod.heartRateReserve);
  });

  test('heartRateReserveCountsVigorousMinutesDouble', () {
    final estimate = calculateIntensityMinutes(
      _samples(bpm: 140, minutes: 30),
      60,
      null,
      180,
      [
        CardioLoadTimeWindow(
            start: _start, end: _start.add(const Duration(seconds: 30 * 60))),
      ],
      const [],
      null,
      null,
    );

    expect(estimate.moderateMinutes, 0);
    expect(estimate.vigorousMinutes, 30);
    expect(estimate.moderateEquivalentMinutes, 60);
  });

  test('workoutActiveCaloriesFallbackIsLowConfidence', () {
    final estimate = calculateIntensityMinutes(
      const [],
      null,
      null,
      null,
      const [],
      const [
        IntensityWorkoutInput(durationMinutes: 30.0, activeCaloriesKcal: 270.0),
      ],
      null,
      null,
    );

    expect(estimate.vigorousMinutes, 30);
    expect(estimate.moderateEquivalentMinutes, 60);
    expect(estimate.confidence, IntensityMinutesConfidence.low);
    expect(estimate.method, IntensityMinutesMethod.workoutActiveCalories);
  });

  test('cardioLoadFallbackProvidesLowConfidenceEstimate', () {
    final estimate = calculateIntensityMinutes(
      const [],
      null,
      null,
      null,
      const [],
      const [],
      null,
      3,
    );

    expect(estimate.moderateEquivalentMinutes, 12);
    expect(estimate.confidence, IntensityMinutesConfidence.low);
    expect(estimate.method, IntensityMinutesMethod.cardioLoad);
  });

  test('noInputsReturnNoData', () {
    final estimate = calculateIntensityMinutes(
      const [],
      null,
      null,
      null,
      const [],
      const [],
      null,
      null,
    );

    expect(estimate.confidence, IntensityMinutesConfidence.noData);
    expect(estimate.method, IntensityMinutesMethod.noData);
    expect(estimate.moderateEquivalentMinutes, 0);
  });
}
