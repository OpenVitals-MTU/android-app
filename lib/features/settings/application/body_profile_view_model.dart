import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';
import '../../../core/result/result.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/preferences/body_profile.dart';
import '../../../domain/preferences/unit_system.dart';

part 'body_profile_view_model.freezed.dart';

/// Kilograms-to-pounds factor, matching the Kotlin `PoundsPerKilogram` constant
/// used by `BodyProfileCard.kt` for the weight field round-trip.
const double poundsPerKilogram = 2.2046226218;

/// Where a body measurement the card is showing came from.
enum BodyMetricSource {
  /// A Health Connect record — the value BMI and FFMI also use.
  measured,

  /// Typed into this card, because nothing was recorded or the permission to
  /// read records is missing.
  declared,
}

/// The profile the card seeds its fields from, resolved against Health Connect.
@freezed
abstract class BodyProfileCardState with _$BodyProfileCardState {
  const factory BodyProfileCardState({
    required BodyProfile profile,
    @Default(BodyMetricSource.declared) BodyMetricSource weightSource,
    @Default(BodyMetricSource.declared) BodyMetricSource heightSource,

    /// Whether a save can write a real measurement rather than only a
    /// preference. False leaves the card behaving exactly as it did before.
    @Default(false) bool canWriteMeasurements,
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
    // Seed synchronously from the declared value so the fields are never blank
    // while Health Connect is read, then fold the measured values in.
    unawaited(_resolve(listenable.value));
    return BodyProfileCardState(profile: listenable.value);
  }

  Future<void> _resolve(BodyProfile declared) async {
    final body = ref.read(bodyRepositoryProvider);
    final resolved = (await body.resolveBodyProfile(declared)).getOrNull();
    if (!ref.mounted || resolved == null) return;
    final canWrite =
        (await body.hasBodyWritePermission(BodyMeasurementType.weight))
                .getOrNull() ??
            false;
    if (!ref.mounted) return;
    state = state.copyWith(
      profile: resolved,
      weightSource: resolved.weightKg != null &&
              resolved.weightKg != declared.weightKg
          ? BodyMetricSource.measured
          : BodyMetricSource.declared,
      heightSource: resolved.heightCm != null &&
              resolved.heightCm != declared.heightCm
          ? BodyMetricSource.measured
          : BodyMetricSource.declared,
      canWriteMeasurements: canWrite,
    );
  }

  /// Persists what was typed. Weight arrives in the DISPLAYED unit and is
  /// stored in kilograms — storage is metric, imperial lives only at the field.
  ///
  /// A changed weight or height is written to Health Connect as a real
  /// measurement when the permission allows it, so BMI, FFMI and the caffeine
  /// half-life all move together instead of the app holding two of each number.
  /// The preference is written regardless, as the fallback for a device without
  /// the permission and as the seed if the record is later deleted.
  Future<void> save({
    required String birthYear,
    required String weight,
    required String height,
    required UnitSystem unit,
  }) async {
    final previous = state.profile;
    final profile = previous.copyWith(
      birthYear: int.tryParse(birthYear.trim()),
      weightKg: storedWeightKg(double.tryParse(weight.trim()), unit),
      heightCm: double.tryParse(height.trim()),
    ).normalized();
    ref.read(preferencesRepositoryProvider).setBodyProfile(profile);

    if (!state.canWriteMeasurements) return;
    final body = ref.read(bodyRepositoryProvider);
    final now = DateTime.now();
    Future<void> write(BodyMeasurementType type, double? value) async {
      if (value == null) return;
      await body.writeBodyMeasurementEntry(
        BodyMeasurementWriteRequest(type: type, time: now, value: value),
      );
    }

    // Only on a real change: saving an unchanged card must not litter the body
    // history with a duplicate entry every time it is opened.
    if (profile.weightKg != previous.weightKg) {
      await write(BodyMeasurementType.weight, profile.weightKg);
    }
    if (profile.heightCm != previous.heightCm) {
      await write(BodyMeasurementType.height, profile.heightCm);
    }
  }

  /// The heart-rate half of the profile, saved from the zones card.
  ///
  /// Split from [save] because the two halves now live on different cards:
  /// resting and max heart rate belong with the zones they define, while age
  /// and body size belong with each other. Both write the same [BodyProfile],
  /// so neither can clobber the other's fields.
  void saveHeartRates({
    required String restingHeartRate,
    required String maxHeartRate,
  }) {
    final profile = state.profile
        .copyWith(
          restingHeartRateBpm: int.tryParse(restingHeartRate.trim()),
          maxHeartRateBpm: int.tryParse(maxHeartRate.trim()),
        )
        .normalized();
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
