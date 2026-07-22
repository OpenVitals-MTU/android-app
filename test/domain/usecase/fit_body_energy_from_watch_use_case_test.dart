import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/usecase/fit_body_energy_from_watch_use_case.dart';

/// Serves a canned timeline for whatever day is asked for.
class _FakeBodyEnergyRepository implements BodyEnergyRepository {
  _FakeBodyEnergyRepository({this.points});

  List<BodyEnergyTimelinePoint>? points;

  /// Set by a test to make the timeline load fail.
  bool fail = false;
  int calls = 0;

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async {
    calls++;
    if (fail) {
      return const Err(UnexpectedFailure('no timeline'));
    }
    final date = query.period.start;
    return Ok(BodyEnergyTimelineResult(
      query: query,
      days: [
        BodyEnergyTimeline(
          date: date,
          startScore: 90,
          currentScore: 80,
          charged: 0,
          drained: 10,
          points: points ?? const [],
          confidence: BodyEnergyConfidence.high,
          confidenceReason: 'test',
        ),
      ],
    ));
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

BodyEnergyTimelinePoint _point(DateTime time, int score) =>
    BodyEnergyTimelinePoint(
      time: time,
      score: score,
      delta: 0,
      state: BodyEnergyBucketState.activity,
      confidence: BodyEnergyConfidence.high,
      activityEnergyDrain: 5,
      primaryInfluence: BodyEnergyPrimaryInfluence.exertion,
    );

void main() {
  late OpenVitalsDatabase db;
  late GarminWellnessDao dao;
  late PreferencesRepository prefs;
  late _FakeBodyEnergyRepository bodyEnergy;
  late FitBodyEnergyFromWatchUseCase useCase;

  // "Now" for the run; samples sit shortly before it.
  final now = DateTime.now().toUtc();

  Future<void> setUp0({List<BodyEnergyTimelinePoint>? points}) async {
    SharedPreferences.setMockInitialValues(const {});
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.garminWellnessDao;
    prefs = PreferencesRepository(await SharedPreferences.getInstance());
    bodyEnergy = _FakeBodyEnergyRepository(points: points);
    useCase = FitBodyEnergyFromWatchUseCase(dao, prefs, bodyEnergy);
    addTearDown(db.close);
    addTearDown(prefs.dispose);
  }

  Future<void> seedBodyEnergy(List<(DateTime, int)> samples) =>
      dao.upsertSamples([
        for (final (at, value) in samples)
          GarminWellnessSamplesCompanion.insert(
            metric: GarminWellnessMetric.bodyEnergy.storageName,
            timeMillis: at.toUtc().millisecondsSinceEpoch,
            value: value,
          ),
      ]);

  test('with no samples it does nothing', () async {
    await setUp0();
    expect(await useCase(now: now), 0);
    expect(prefs.bodyEnergyCalibration().watchObservationCount, 0);
  });

  test('folds new readings in and moves the gains', () async {
    final at = now.subtract(const Duration(hours: 2));
    await setUp0(points: [_point(at.toLocal(), 80)]);
    await seedBodyEnergy([(at, 50)]);

    final fitted = await useCase(now: now);

    expect(fitted, 1);
    final calibration = prefs.bodyEnergyCalibration();
    expect(calibration.watchObservationCount, 1);
    // Observed below predicted → drained harder than modelled.
    expect(calibration.activityDrainGain, greaterThan(1.0));
  });

  test('a second run does not re-count the same samples', () async {
    final at = now.subtract(const Duration(hours: 2));
    await setUp0(points: [_point(at.toLocal(), 80)]);
    await seedBodyEnergy([(at, 50)]);

    await useCase(now: now);
    final afterFirst = prefs.bodyEnergyCalibration();
    final second = await useCase(now: now);

    // The watch re-offers overlapping windows constantly; counting a reading
    // twice would teach the model the same lesson repeatedly.
    expect(second, 0);
    expect(prefs.bodyEnergyCalibration().watchObservationCount,
        afterFirst.watchObservationCount);
    expect(prefs.bodyEnergyCalibration().activityDrainGain,
        afterFirst.activityDrainGain);
  });

  test('only samples newer than the watermark are fitted', () async {
    final older = now.subtract(const Duration(hours: 3));
    final newer = now.subtract(const Duration(hours: 1));
    await setUp0(points: [
      _point(older.toLocal(), 80),
      _point(newer.toLocal(), 80),
    ]);
    await seedBodyEnergy([(older, 50)]);
    await useCase(now: now);

    // A later sync brings a newer sample; only that one should count.
    await seedBodyEnergy([(newer, 50)]);
    final second = await useCase(now: now);

    expect(second, 1);
    expect(prefs.bodyEnergyCalibration().watchObservationCount, 2);
  });

  test('a failing timeline leaves the gains and watermark untouched', () async {
    final at = now.subtract(const Duration(hours: 2));
    await setUp0();
    bodyEnergy.fail = true;
    await seedBodyEnergy([(at, 50)]);

    expect(await useCase(now: now), 0);
    expect(prefs.bodyEnergyCalibration().watchObservationCount, 0);
    // Nothing was fitted, so the readings must remain eligible next time.
    expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);
  });

  test('samples that pair to nothing leave the watermark alone', () async {
    final at = now.subtract(const Duration(hours: 2));
    // A timeline with no points: nothing to compare against.
    await setUp0(points: const []);
    await seedBodyEnergy([(at, 50)]);

    expect(await useCase(now: now), 0);
    // Deliberately NOT advanced when nothing fitted, so a timeline that was
    // merely unavailable this run gets another chance.
    expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);
  });

  test('an hour contributes ONE observation however often you sync', () async {
    // The flaw this replaced: each sync downsampled only within its own batch,
    // so ten syncs in an hour taught the model ten times as fast as one, from
    // identical watch data. Learning must track elapsed time, not tapping.
    final hourStart = DateTime.utc(
      now.year,
      now.month,
      now.day,
      now.subtract(const Duration(hours: 2)).hour,
    );
    await setUp0(points: [
      for (var m = 0; m < 60; m += 5)
        _point(hourStart.add(Duration(minutes: m)).toLocal(), 80),
    ]);

    // Six syncs, each bringing ten more minutes of the SAME hour.
    for (var m = 0; m < 60; m += 10) {
      await seedBodyEnergy([
        for (var k = 0; k < 10; k++)
          (hourStart.add(Duration(minutes: m + k)), 50),
      ]);
      await useCase(now: now);
    }

    expect(prefs.bodyEnergyCalibration().watchObservationCount, 1);
  });

  test('successive hours each contribute one observation', () async {
    final base = DateTime.utc(now.year, now.month, now.day,
        now.subtract(const Duration(hours: 4)).hour);
    await setUp0(points: [
      for (var h = 0; h < 3; h++)
        _point(base.add(Duration(hours: h)).toLocal(), 80),
    ]);

    for (var h = 0; h < 3; h++) {
      await seedBodyEnergy([(base.add(Duration(hours: h)), 50)]);
      await useCase(now: now);
    }

    expect(prefs.bodyEnergyCalibration().watchObservationCount, 3);
  });

  test('the gains stay in bounds across many runs', () async {
    await setUp0();
    for (var hour = 1; hour <= 24; hour++) {
      final at = now.subtract(Duration(hours: 25 - hour));
      bodyEnergy.points = [_point(at.toLocal(), 100)];
      await seedBodyEnergy([(at, 0)]);
      await useCase(now: now);
    }

    final calibration = prefs.bodyEnergyCalibration();
    expect(calibration.activityDrainGain,
        lessThanOrEqualTo(BodyEnergyCalibration.maxGain));
    expect(calibration.activityDrainGain,
        greaterThanOrEqualTo(BodyEnergyCalibration.minGain));
  });
}
