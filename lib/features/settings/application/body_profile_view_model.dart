import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';
import '../../../domain/preferences/body_profile.dart';
import '../../../domain/preferences/unit_system.dart';

part 'body_profile_view_model.freezed.dart';

/// Kilograms-to-pounds factor, matching the Kotlin `PoundsPerKilogram` constant
/// used by `BodyProfileCard.kt` for the weight field round-trip.
const double poundsPerKilogram = 2.2046226218;

/// The stored [BodyProfile] the card seeds its fields from.
@freezed
abstract class BodyProfileCardState with _$BodyProfileCardState {
  const factory BodyProfileCardState({
    required BodyProfile profile,
  }) = _BodyProfileCardState;
}

/// Owns the body profile: the read (kept in sync with the repository's
/// listenable, so a save anywhere re-seeds the card) and the write.
///
/// The card keeps its text controllers; the parse, the unit conversion and the
/// `normalized()` call — the logic that decides what is persisted — live here.
/// The accessors are synchronous, so a save needs no command state.
class BodyProfileViewModel extends Notifier<BodyProfileCardState> {
  @override
  BodyProfileCardState build() {
    final repo = ref.watch(preferencesRepositoryProvider);
    final listenable = repo.bodyProfileListenable;
    void listener() => ref.invalidateSelf();
    listenable.addListener(listener);
    ref.onDispose(() => listenable.removeListener(listener));
    return BodyProfileCardState(profile: listenable.value);
  }

  /// Persists the four fields as typed, mirroring the Kotlin
  /// `onSave(draft.normalized())`. Weight arrives in the DISPLAYED unit and is
  /// stored in kilograms — storage is metric, imperial lives only at the field.
  void save({
    required String birthYear,
    required String weight,
    required String restingHeartRate,
    required String maxHeartRate,
    required UnitSystem unit,
  }) {
    final profile = BodyProfile(
      birthYear: int.tryParse(birthYear.trim()),
      weightKg: storedWeightKg(double.tryParse(weight.trim()), unit),
      restingHeartRateBpm: int.tryParse(restingHeartRate.trim()),
      maxHeartRateBpm: int.tryParse(maxHeartRate.trim()),
    ).normalized();
    ref.read(preferencesRepositoryProvider).setBodyProfile(profile);
  }
}

/// The stored kilograms for a weight typed in [unit].
double? storedWeightKg(double? weight, UnitSystem unit) {
  if (weight == null) return null;
  return switch (unit) {
    UnitSystem.metric => weight,
    UnitSystem.imperial => weight / poundsPerKilogram,
  };
}

/// The weight to SHOW for a stored kilogram value in [unit].
double? displayWeight(double? weightKg, UnitSystem unit) {
  if (weightKg == null) return null;
  return switch (unit) {
    UnitSystem.metric => weightKg,
    UnitSystem.imperial => weightKg * poundsPerKilogram,
  };
}

/// The state provider for the body-profile settings card.
final bodyProfileCardProvider =
    NotifierProvider<BodyProfileViewModel, BodyProfileCardState>(
  BodyProfileViewModel.new,
);
