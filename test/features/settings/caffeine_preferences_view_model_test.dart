import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';
import 'package:openvitals/domain/preferences/caffeine_preferences.dart';
import 'package:openvitals/features/settings/application/caffeine_preferences_view_model.dart';

Future<(ProviderContainer, SharedPreferences)> _container([
  Map<String, Object> initialValues = const <String, Object>{},
]) async {
  SharedPreferences.setMockInitialValues(initialValues);
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(
    overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
  );
  addTearDown(container.dispose);
  return (container, prefs);
}

void main() {
  test('build seeds the draft and the body profile from preferences', () async {
    final (container, prefs) = await _container();
    PreferencesRepository(prefs).setBodyProfile(
      const BodyProfile(weightKg: 70).normalized(),
    );

    final state = container.read(caffeinePreferencesCardProvider);

    expect(
      state.draft.halfLifeMinutes,
      CaffeinePreferences.defaultHalfLifeMinutes,
    );
    expect(state.bodyProfile.weightKg, 70);
    expect(state.seedRevision, 0);
  });

  test('save persists the whole draft with profileCompleted', () async {
    final (container, prefs) = await _container();
    final notifier = container.read(caffeinePreferencesCardProvider.notifier);
    final draft = container.read(caffeinePreferencesCardProvider).draft;

    notifier.updateDraft(draft.copyWith(
      halfLifeMinutes: 360,
      smoker: true,
      bedtime: const LocalTime(23, 15),
    ));
    // The draft alone changes nothing on disk.
    expect(PreferencesRepository(prefs).caffeinePreferences().smoker, isFalse);

    notifier.save();

    final saved = PreferencesRepository(prefs).caffeinePreferences();
    expect(saved.halfLifeMinutes, 360);
    expect(saved.smoker, isTrue);
    expect(saved.bedtime, const LocalTime(23, 15));
    expect(saved.profileCompleted, isTrue);
  });

  test('save reseeds the draft from the clamped stored value', () async {
    final (container, _) = await _container();
    final notifier = container.read(caffeinePreferencesCardProvider.notifier);
    final draft = container.read(caffeinePreferencesCardProvider).draft;

    notifier.updateDraft(draft.copyWith(halfLifeMinutes: 9000));
    notifier.save();

    final state = container.read(caffeinePreferencesCardProvider);
    // The repository normalizes on write; the draft follows what was stored,
    // and the revision ticks so the card reseeds its text controllers.
    expect(state.draft.halfLifeMinutes, CaffeinePreferences.maxHalfLifeMinutes);
    expect(state.seedRevision, 1);
  });
}
