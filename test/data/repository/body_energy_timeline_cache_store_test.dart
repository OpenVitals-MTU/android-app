import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/body_energy_timeline_cache_store.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<BodyEnergyTimelineCacheStore> newStore() async {
  SharedPreferences.setMockInitialValues({});
  final prefs = await SharedPreferences.getInstance();
  return BodyEnergyTimelineCacheStore(prefs);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const date = LocalDate(2026, 7, 6);
  const signature = 'perm|calib|v2';

  test('missing entry returns null', () async {
    final store = await newStore();
    expect(store.load(date, signature), isNull);
    expect(store.loadBaseline(date, signature), isNull);
  });

  test('timeline round-trips through the string encoding', () async {
    final store = await newStore();
    final generatedAt = DateTime.fromMillisecondsSinceEpoch(
      1_700_000_000_000,
      isUtc: true,
    );
    final timeline = BodyEnergyTimeline(
      date: date,
      startScore: 60,
      currentScore: 72,
      charged: 20,
      drained: 8,
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'good | data\nwith breaks',
      generatedAt: generatedAt,
      signature: signature,
      inputSummary: const BodyEnergyInputSummary(
        algorithmVersion: 2,
        bucketMinutes: 5,
        heartRateSampleCount: 120,
        hrvSampleCount: 3,
        hasRestingHeartRate: true,
        previousEndScore: 55,
        calibrationMode: BodyEnergyCalibrationMode.manualZones,
      ),
      points: [
        BodyEnergyTimelinePoint(
          time: DateTime.fromMillisecondsSinceEpoch(1_700_000_300_000,
              isUtc: true),
          score: 61,
          delta: 1.5,
          state: BodyEnergyBucketState.rest,
          confidence: BodyEnergyConfidence.medium,
          charge: 1.5,
          intensityDrain: 0.25,
          stressDrain: 0.5,
          recoveryDebtDrain: 0.125,
          primaryInfluence: BodyEnergyPrimaryInfluence.quietRest,
        ),
        BodyEnergyTimelinePoint(
          time: DateTime.fromMillisecondsSinceEpoch(1_700_000_600_000,
              isUtc: true),
          score: 63,
          delta: -2.0,
          state: BodyEnergyBucketState.activity,
          confidence: BodyEnergyConfidence.high,
          charge: 0.0,
          intensityDrain: 2.0,
          primaryInfluence: BodyEnergyPrimaryInfluence.exertion,
        ),
      ],
    );

    await store.save(timeline);
    final loaded = store.load(date, signature);

    expect(loaded, isNotNull);
    expect(loaded!.date, date);
    expect(loaded.startScore, 60);
    expect(loaded.currentScore, 72);
    expect(loaded.charged, 20);
    expect(loaded.drained, 8);
    expect(loaded.confidence, BodyEnergyConfidence.high);
    expect(loaded.confidenceReason, 'good | data\nwith breaks');
    expect(loaded.generatedAt, generatedAt);
    expect(loaded.signature, signature);
    expect(loaded.inputSummary.algorithmVersion, 2);
    expect(loaded.inputSummary.heartRateSampleCount, 120);
    expect(loaded.inputSummary.hrvSampleCount, 3);
    expect(loaded.inputSummary.hasRestingHeartRate, isTrue);
    expect(loaded.inputSummary.previousEndScore, 55);
    expect(loaded.inputSummary.calibrationMode,
        BodyEnergyCalibrationMode.manualZones);

    expect(loaded.points.length, 2);
    final first = loaded.points.first;
    expect(first.score, 61);
    expect(first.delta, closeTo(1.5, 1e-4));
    expect(first.state, BodyEnergyBucketState.rest);
    expect(first.confidence, BodyEnergyConfidence.medium);
    expect(first.charge, closeTo(1.5, 1e-4));
    expect(first.intensityDrain, closeTo(0.25, 1e-4));
    expect(first.stressDrain, closeTo(0.5, 1e-4));
    expect(first.recoveryDebtDrain, closeTo(0.125, 1e-4));
    expect(first.primaryInfluence, BodyEnergyPrimaryInfluence.quietRest);
    expect(loaded.points[1].primaryInfluence,
        BodyEnergyPrimaryInfluence.exertion);
  });

  test('blank signature is not persisted', () async {
    final store = await newStore();
    final timeline = BodyEnergyTimeline.empty(date: date, reason: 'no data')
        .copyWith(signature: '   ');
    await store.save(timeline);
    expect(store.load(date, '   '), isNull);
  });

  test('baseline entry round-trips including nulls', () async {
    final store = await newStore();
    final generatedAt = DateTime.fromMillisecondsSinceEpoch(
      1_699_999_000_000,
      isUtc: true,
    );
    final baseline = BodyEnergyBaselineCacheEntry(
      baselineRestingHeartRateBpm: 54,
      observedMaxHeartRateBpm: null,
      hrvBaselineRmssdMs: 42.5,
      respiratoryRateBaseline: null,
      generatedAt: generatedAt,
    );

    await store.saveBaseline(date, signature, baseline);
    final loaded = store.loadBaseline(date, signature);

    expect(loaded, isNotNull);
    expect(loaded!.baselineRestingHeartRateBpm, 54);
    expect(loaded.observedMaxHeartRateBpm, isNull);
    expect(loaded.hrvBaselineRmssdMs, closeTo(42.5, 1e-6));
    expect(loaded.respiratoryRateBaseline, isNull);
    expect(loaded.generatedAt, generatedAt);
  });
}
