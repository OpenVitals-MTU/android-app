import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/body_energy_baseline_cache_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<BodyEnergyBaselineCacheStore> newStore([
  Map<String, Object> initial = const {},
]) async {
  SharedPreferences.setMockInitialValues(initial);
  final prefs = await SharedPreferences.getInstance();
  return BodyEnergyBaselineCacheStore(prefs);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const date = LocalDate(2026, 7, 6);
  const signature = 'perm|calib|v2';

  test('missing entry returns null', () async {
    final store = await newStore();
    expect(store.loadBaseline(date, signature), isNull);
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

  test('a blank signature is not persisted', () async {
    final store = await newStore();
    await store.saveBaseline(
      date,
      '   ',
      BodyEnergyBaselineCacheEntry(
        baselineRestingHeartRateBpm: 54,
        observedMaxHeartRateBpm: null,
        hrvBaselineRmssdMs: null,
        respiratoryRateBaseline: null,
      ),
    );
    expect(store.loadBaseline(date, '   '), isNull);
  });

  group('purgeLegacyTimelineEntries', () {
    test('removes the retired timeline keys and nothing else', () async {
      // The timeline half wrote `<date>|<signatureHash>`; the baselines it
      // shared the file with carry a `baseline|` prefix, and neither may be
      // confused with an ordinary preference.
      final store = await newStore({
        '2026-07-06|-1234567': 'encoded timeline',
        '2026-07-05|889900': 'encoded timeline',
        'baseline|2026-07-06|-1234567': '54||42.5||1699999000000',
        'unit_system': 'metric',
        'body_energy_sleep_charge_gain': 1.2,
      });

      await store.purgeLegacyTimelineEntries();

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getKeys(), containsAll(<String>[
        'baseline|2026-07-06|-1234567',
        'unit_system',
        'body_energy_sleep_charge_gain',
      ]));
      expect(prefs.getKeys(), isNot(contains('2026-07-06|-1234567')));
      expect(prefs.getKeys(), isNot(contains('2026-07-05|889900')));
    });

    test('runs once — a key written afterwards survives', () async {
      final store = await newStore({'2026-07-06|-1': 'encoded'});
      await store.purgeLegacyTimelineEntries();

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('2026-07-07|-2', 'written after the purge');
      await store.purgeLegacyTimelineEntries();

      expect(prefs.getString('2026-07-07|-2'), 'written after the purge');
    });
  });
}
