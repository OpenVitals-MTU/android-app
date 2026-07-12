import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';
import '../../../domain/preferences/body_energy_calibration.dart';

part 'body_energy_calibration_view_model.freezed.dart';

/// The stored [BodyEnergyCalibration] the card seeds its zone fields from.
@freezed
abstract class BodyEnergyCalibrationCardState
    with _$BodyEnergyCalibrationCardState {
  const factory BodyEnergyCalibrationCardState({
    required BodyEnergyCalibration calibration,
  }) = _BodyEnergyCalibrationCardState;
}

/// Owns the Body Energy calibration: the read (kept in sync with the
/// repository's listenable, so a save or a reset re-seeds the card) and the two
/// writes.
///
/// The card keeps its zone text controllers and the manual-zones switch; the
/// parse, the `normalized()` call and the forced `setupCompleted = true` — the
/// logic that decides what is persisted — live here (Kotlin
/// `SettingsViewModel.updateBodyEnergyCalibration`).
class BodyEnergyCalibrationViewModel
    extends Notifier<BodyEnergyCalibrationCardState> {
  @override
  BodyEnergyCalibrationCardState build() {
    final repo = ref.watch(preferencesRepositoryProvider);
    final listenable = repo.bodyEnergyCalibrationListenable;
    void listener() => ref.invalidateSelf();
    listenable.addListener(listener);
    ref.onDispose(() => listenable.removeListener(listener));
    return BodyEnergyCalibrationCardState(calibration: listenable.value);
  }

  /// Persists the five typed zone lower bounds. An unparseable field is a zero
  /// bound, exactly as the Kotlin editor's `toIntOrNull() ?: 0` — `normalized()`
  /// then repairs the ordering.
  void save({
    required String zone1,
    required String zone2,
    required String zone3,
    required String zone4,
    required String zone5,
    required bool useManualZones,
  }) {
    final calibration = BodyEnergyCalibration(
      manualZoneThresholdsBpm: HeartZoneThresholds(
        zone1LowerBpm: int.tryParse(zone1.trim()) ?? 0,
        zone2LowerBpm: int.tryParse(zone2.trim()) ?? 0,
        zone3LowerBpm: int.tryParse(zone3.trim()) ?? 0,
        zone4LowerBpm: int.tryParse(zone4.trim()) ?? 0,
        zone5LowerBpm: int.tryParse(zone5.trim()) ?? 0,
      ),
      useManualZones: useManualZones,
    ).normalized();
    _persist(calibration);
  }

  /// Kotlin `resetBodyEnergyCalibration`: back to the automatic zones, still
  /// marked as set up.
  void useAutomatic() => _persist(BodyEnergyCalibration.automatic);

  void _persist(BodyEnergyCalibration calibration) {
    ref
        .read(preferencesRepositoryProvider)
        .setBodyEnergyCalibration(calibration.copyWith(setupCompleted: true));
  }
}

/// The state provider for the Body Energy calibration settings card.
final bodyEnergyCalibrationSettingsProvider = NotifierProvider<
    BodyEnergyCalibrationViewModel, BodyEnergyCalibrationCardState>(
  BodyEnergyCalibrationViewModel.new,
);

/// The current calibration on its own — what the Body Energy detail screen
/// reads. Derived, so the listenable is subscribed exactly once.
final bodyEnergyCalibrationCardProvider = Provider<BodyEnergyCalibration>(
  (ref) => ref.watch(bodyEnergyCalibrationSettingsProvider).calibration,
);
