import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/preferences/body_profile.dart';
import '../../../domain/preferences/caffeine_preferences.dart';

part 'caffeine_preferences_view_model.freezed.dart';

/// The caffeine-model settings form: the [draft] the user is editing, the
/// [bodyProfile] the effective half-life is computed against, and a
/// [seedRevision] that ticks on every (re)load so the card knows to reseed its
/// text controllers from the — clamped — stored values.
@freezed
abstract class CaffeinePreferencesState with _$CaffeinePreferencesState {
  const factory CaffeinePreferencesState({
    required CaffeinePreferences draft,
    required BodyProfile bodyProfile,
    @Default(0) int seedRevision,
  }) = _CaffeinePreferencesState;
}

/// Owns the caffeine-preferences form. The card holds only its
/// [TextEditingController]s; every read and write of [PreferencesRepository]
/// happens here.
///
/// The accessors are synchronous getters/setters (no `Result`), so a save is a
/// plain intent — no command state is needed.
class CaffeinePreferencesViewModel extends Notifier<CaffeinePreferencesState> {
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  @override
  CaffeinePreferencesState build() {
    final prefs = ref.watch(preferencesRepositoryProvider);
    return CaffeinePreferencesState(
      draft: prefs.caffeinePreferences(),
      bodyProfile: prefs.bodyProfile(),
    );
  }

  /// Replaces the in-flight draft (a field edit, a dropdown pick, a switch).
  void updateDraft(CaffeinePreferences draft) =>
      state = state.copyWith(draft: draft);

  /// Writes the whole preferences object back, mirroring the Kotlin
  /// `onSave(draft.copy(profileCompleted = true))`, then reseeds from the
  /// normalized (clamped) stored value so the fields reflect exactly what was
  /// persisted.
  void save() {
    final prefs = _prefs;
    prefs.setCaffeinePreferences(state.draft.copyWith(profileCompleted: true));
    state = state.copyWith(
      draft: prefs.caffeinePreferences(),
      seedRevision: state.seedRevision + 1,
    );
  }
}

/// The state provider for the caffeine-preferences settings card.
final caffeinePreferencesCardProvider =
    NotifierProvider<CaffeinePreferencesViewModel, CaffeinePreferencesState>(
  CaffeinePreferencesViewModel.new,
);
