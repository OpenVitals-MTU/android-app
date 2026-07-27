import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
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

  /// Per-day override, for the tests that need one day warm and another cold.
  /// A day absent from the map falls back to [points].
  Map<LocalDate, List<BodyEnergyTimelinePoint>> pointsByDay = {};

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
          points: pointsByDay[date] ?? points ?? const [],
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
  //
  // PINNED, and pinned deliberately in the small hours. The use case takes its
  // `now`, so nothing here needs the wall clock — and taking it from the wall
  // clock meant these tests ran against a different instant every time, passing
  // all day and failing on the CI runs that happened after midnight. 01:30 UTC
  // is the value that used to break them: any hour subtracted from it lands on
  // the previous day, so a test that builds a past instant by pasting today's
  // date onto a shifted hour-of-day now fails at once rather than overnight.
  final now = DateTime.utc(2026, 7, 26, 1, 30);

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
    // The top of the hour two hours back, built by SUBTRACTING from the top of
    // the current hour — not by pasting today's date onto the hour-of-day from
    // two hours ago. Across midnight those are different days: at 01:30 the
    // hour-of-day two hours back is 23, and "today at 23:00" is twenty-one
    // hours in the FUTURE, past the use case's `<= now` window, so every sample
    // was filtered out and the count came back 0. Its sibling below already
    // said this; this one still did it the other way, and failed whenever CI
    // ran in the small hours.
    final hourStart = DateTime.utc(now.year, now.month, now.day, now.hour)
        .subtract(const Duration(hours: 2));
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
    // The top of an hour four hours ago. Built by subtracting from `now`, NOT
    // by pasting today's date onto the hour-of-day from four hours back: near
    // midnight UTC those disagree — 02:00 minus four hours is 22:00 YESTERDAY,
    // and "today at 22:00" is in the FUTURE, past the use case's `<= now`
    // window, so every sample was filtered out and the count came back 0. The
    // test failed only when CI happened to run in the small hours.
    final topOfHour = DateTime.utc(now.year, now.month, now.day, now.hour);
    final base = topOfHour.subtract(const Duration(hours: 4));
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

  group('a cold day must not retire the days behind it', () {
    // Built from LOCAL DATES, not by subtracting hours off `now`.
    //
    // The grace window is measured in local days, so an hour offset lands on
    // whichever side of it the runner's timezone puts it. 26 hours before
    // 01:30 UTC is yesterday at UTC+3 and the day BEFORE yesterday at UTC —
    // inside the grace in one and past it in the other. That is exactly how
    // this passed on a Tallinn laptop and failed on a UTC CI runner.
    //
    // `atTimeInstant` builds the instant from the local calendar date, so the
    // day a sample belongs to is fixed by construction rather than inferred
    // from arithmetic that the zone gets a vote in.
    final localToday = LocalDate.fromDateTime(now.toLocal());
    final coldDay = localToday.minusDays(1);
    final warmDay = localToday;
    final coldAt = coldDay.atTimeInstant(12);
    // `now` itself: any earlier offset can fall into yesterday when the local
    // clock is just past midnight, which is the same trap one level down.
    final warmAt = now;

    test('an unexamined older day blocks the run instead of being burned',
        () async {
      // The shape of the real loss: the watermark is one scalar, so the moment
      // a later day fitted, the code jumped it past EVERY sample it had read --
      // retiring an earlier day whose timeline simply had not been computed
      // yet. That day's evidence was then permanently unreadable.
      await setUp0();
      bodyEnergy.pointsByDay = {
        coldDay: const [],
        warmDay: [_point(warmAt.toLocal(), 80)],
      };
      await seedBodyEnergy([(coldAt, 50), (warmAt, 50)]);

      expect(await useCase(now: now), 0);
      expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);

      // Once the chain reaches the cold day, BOTH days are still there to learn
      // from -- which is the whole point.
      bodyEnergy.pointsByDay[coldDay] = [_point(coldAt.toLocal(), 80)];
      expect(await useCase(now: now), 2);
      expect(prefs.bodyEnergyCalibration().watchObservationCount, 2);
      expect(prefs.bodyEnergyWatchFitWatermarkMillis, greaterThan(0));
    });

    test('a cold day mid-window does not block the days behind it', () async {
      // The cutoff used to be the whole lookback window, so ONE day inside it
      // with no timeline held up every later day indefinitely. That matters
      // most right after the watch-fit epoch rewind, when the watermark goes
      // back a week and the oldest day is the first one examined: a single cold
      // day there and the entire refit folds nothing at all.
      final staleAt = now.subtract(const Duration(days: 5));
      await setUp0();
      prefs.bodyEnergyWatchFitWatermarkMillis =
          now.subtract(const Duration(days: 7)).millisecondsSinceEpoch;
      bodyEnergy.pointsByDay = {
        LocalDate.fromDateTime(staleAt.toLocal()): const [],
        warmDay: [_point(warmAt.toLocal(), 80)],
      };
      await seedBodyEnergy([(staleAt, 50), (warmAt, 50)]);

      expect(await useCase(now: now), 1);
      expect(prefs.bodyEnergyCalibration().watchObservationCount, 1);
    });

    test('but a day past the lookback window is retired, not wedged', () async {
      // Otherwise a day that will never have a timeline -- before the install,
      // or with no heart data at all -- blocks the watermark forever and every
      // sync re-reads an ever-growing window.
      final staleAt = now.subtract(const Duration(days: 10));
      await setUp0();
      prefs.bodyEnergyWatchFitWatermarkMillis =
          now.subtract(const Duration(days: 30)).millisecondsSinceEpoch;
      bodyEnergy.pointsByDay = {
        LocalDate.fromDateTime(staleAt.toLocal()): const [],
        warmDay: [_point(warmAt.toLocal(), 80)],
      };
      await seedBodyEnergy([(staleAt, 50), (warmAt, 50)]);

      expect(await useCase(now: now), 1);
      expect(prefs.bodyEnergyCalibration().watchObservationCount, 1);
      expect(prefs.bodyEnergyWatchFitWatermarkMillis,
          greaterThan(staleAt.millisecondsSinceEpoch));
    });
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
