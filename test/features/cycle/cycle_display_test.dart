import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/cycle_models.dart';
import 'package:openvitals/features/cycle/application/cycle_display.dart';

/// The derivation the cycle screen used to do in its build path — expanding each
/// menstruation period into its days, scanning for the latest basal temperature,
/// merging and sorting every record into one dated list — now a pure function the
/// view-model calls once per load.
void main() {
  final march = DateTime(2026, 3, 2, 7);

  test('an empty period derives zeroes and no observations', () {
    final display = buildCycleDisplay(const CycleData());

    expect(display.hasData, isFalse);
    expect(display.periodDays, 0);
    expect(display.totalEntryCount, 0);
    expect(display.latestBbtCelsius, isNull);
    expect(display.observations, isEmpty);
  });

  test('period days count the days a period covers, not the records', () {
    final display = buildCycleDisplay(CycleData(
      menstruationPeriods: [
        MenstruationPeriodEntry(
          startTime: march,
          // Ends at the start of the 5th: the last covered day is the 4th.
          endTime: DateTime(2026, 3, 5),
          source: 'test',
        ),
      ],
    ));

    // 2nd, 3rd, 4th.
    expect(display.periodDays, 3);
    expect(display.observations.single.kind,
        CycleObservationKind.menstruationPeriod);
    expect(display.observations.single.days, 3);
  });

  test('the summary counts every record, and the latest basal temperature', () {
    final display = buildCycleDisplay(CycleData(
      ovulationTests: [
        OvulationTestEntry(time: march, result: 1, source: 'test'),
        OvulationTestEntry(
          time: march.add(const Duration(days: 1)),
          result: 3,
          source: 'test',
        ),
      ],
      basalBodyTemperature: [
        BasalBodyTemperatureEntry(
          time: march,
          temperatureCelsius: 36.5,
          measurementLocation: 2,
          source: 'test',
        ),
        BasalBodyTemperatureEntry(
          time: march.add(const Duration(days: 2)),
          temperatureCelsius: 36.9,
          measurementLocation: 2,
          source: 'test',
        ),
      ],
    ));

    expect(display.hasData, isTrue);
    expect(display.ovulationTestCount, 2);
    expect(display.bbtReadingCount, 2);
    expect(display.totalEntryCount, 4);
    // The latest by TIME, not the last in the list.
    expect(display.latestBbtCelsius, 36.9);
  });

  test('observations from every record type are merged, newest first', () {
    final display = buildCycleDisplay(CycleData(
      menstruationFlows: [
        MenstruationFlowEntry(time: march, flow: 2, source: 'test'),
      ],
      ovulationTests: [
        OvulationTestEntry(
          time: march.add(const Duration(days: 3)),
          result: 1,
          source: 'test',
        ),
      ],
      sexualActivity: [
        SexualActivityEntry(
          time: march.add(const Duration(days: 1)),
          protectionUsed: 1,
          source: 'test',
        ),
      ],
    ));

    expect(
      display.observations.map((o) => o.kind).toList(),
      [
        CycleObservationKind.ovulationTest,
        CycleObservationKind.sexualActivity,
        CycleObservationKind.menstruationFlow,
      ],
    );
    // The raw Health Connect codes ride along; the labels are the view's.
    expect(display.observations.first.ovulationResult, 1);
    expect(display.observations.last.flow, 2);
  });
}
