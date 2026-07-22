import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/local/open_vitals_database.dart';

void main() {
  late OpenVitalsDatabase db;
  late GarminWellnessDao dao;

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.garminWellnessDao;
    addTearDown(db.close);
  });

  GarminWellnessSamplesCompanion row(
    GarminWellnessMetric metric,
    DateTime at,
    int value,
  ) =>
      GarminWellnessSamplesCompanion.insert(
        metric: metric.storageName,
        timeMillis: at.toUtc().millisecondsSinceEpoch,
        value: value,
      );

  test('stores and reads back a window, oldest first', () async {
    await dao.upsertSamples([
      row(GarminWellnessMetric.stress, DateTime.utc(2026, 7, 22, 10, 2), 51),
      row(GarminWellnessMetric.stress, DateTime.utc(2026, 7, 22, 10, 1), 42),
    ]);

    final samples = await dao.samplesBetween(
      GarminWellnessMetric.stress,
      DateTime.utc(2026, 7, 22, 10).millisecondsSinceEpoch,
      DateTime.utc(2026, 7, 22, 11).millisecondsSinceEpoch,
    );

    expect(samples.map((s) => s.value), [42, 51]);
  });

  test('re-syncing the same window overwrites rather than duplicating',
      () async {
    final at = DateTime.utc(2026, 7, 22, 10, 1);
    await dao.upsertSamples([row(GarminWellnessMetric.stress, at, 42)]);
    // A watch re-offers overlapping monitoring windows, so the same sample
    // arrives on successive syncs.
    await dao.upsertSamples([row(GarminWellnessMetric.stress, at, 44)]);

    expect(await dao.countFor(GarminWellnessMetric.stress), 1);
    final samples = await dao.samplesBetween(
      GarminWellnessMetric.stress,
      0,
      DateTime.utc(2027).millisecondsSinceEpoch,
    );
    expect(samples.single.value, 44);
  });

  test('the two metrics do not collide at the same instant', () async {
    final at = DateTime.utc(2026, 7, 22, 10, 1);
    await dao.upsertSamples([
      row(GarminWellnessMetric.stress, at, 42),
      row(GarminWellnessMetric.bodyEnergy, at, 72),
    ]);

    expect(await dao.countFor(GarminWellnessMetric.stress), 1);
    expect(await dao.countFor(GarminWellnessMetric.bodyEnergy), 1);
  });

  test('latest returns the newest sample, or null when empty', () async {
    expect(await dao.latest(GarminWellnessMetric.bodyEnergy), isNull);

    await dao.upsertSamples([
      row(GarminWellnessMetric.bodyEnergy, DateTime.utc(2026, 7, 22, 9), 80),
      row(GarminWellnessMetric.bodyEnergy, DateTime.utc(2026, 7, 22, 11), 72),
      row(GarminWellnessMetric.bodyEnergy, DateTime.utc(2026, 7, 22, 10), 76),
    ]);

    expect((await dao.latest(GarminWellnessMetric.bodyEnergy))!.value, 72);
  });

  test('an empty batch is a no-op', () async {
    await dao.upsertSamples([]);
    expect(await dao.countFor(GarminWellnessMetric.stress), 0);
  });

  test('the window is half-open: start inclusive, end exclusive', () async {
    await dao.upsertSamples([
      row(GarminWellnessMetric.stress, DateTime.utc(2026, 7, 22, 10), 1),
      row(GarminWellnessMetric.stress, DateTime.utc(2026, 7, 22, 11), 2),
    ]);

    final samples = await dao.samplesBetween(
      GarminWellnessMetric.stress,
      DateTime.utc(2026, 7, 22, 10).millisecondsSinceEpoch,
      DateTime.utc(2026, 7, 22, 11).millisecondsSinceEpoch,
    );

    // Adjacent windows must tile without double-counting the boundary sample.
    expect(samples.map((s) => s.value), [1]);
  });

  test('the schema version was bumped for this table', () {
    // A table added without bumping the version never gets created on an
    // existing install.
    expect(db.schemaVersion, greaterThanOrEqualTo(6));
  });
}
