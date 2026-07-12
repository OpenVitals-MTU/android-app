import '../../data/repository/contract/hydration_repository.dart';
import 'save_hydration_entry_use_case.dart';

/// What the entry screen remembers between visits: the containers as the user has
/// resized them, and the last free-form amount they typed.
class HydrationEntrySettings {
  const HydrationEntrySettings({
    required this.containerVolumeOverridesMilliliters,
    required this.lastCustomAmountMilliliters,
  });

  /// Resized default containers, keyed by container id. Only the sizes that can
  /// still be logged survive the read.
  final Map<String, double> containerVolumeOverridesMilliliters;

  /// The last free-form amount, or null when there is not a usable one to
  /// re-offer as a one-tap chip.
  final double? lastCustomAmountMilliliters;
}

/// Reads what the hydration entry screen remembers — **synchronously**.
///
/// Not a `Future`: these are configuration, and the screen paints its container
/// row on the first frame. An async read would give it a frame of default
/// containers to draw first, which the user would see snap.
///
/// Every stored size goes through the same container-size rule the write path
/// uses. Storage outlives the rules that filled it — an older build, a preference
/// edited by hand — and a container the entry path would refuse to log is worse
/// than no container at all: it is a button that silently does nothing.
///
/// The daily goal is *not* here (see `ReadHydrationDailyGoalUseCase`): it is read
/// by screens that have no containers and no custom amounts, and folding it in
/// would make them pay for storage they never look at.
class ReadHydrationEntrySettingsUseCase {
  const ReadHydrationEntrySettingsUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  HydrationEntrySettings call() {
    final overrides = _hydrationRepository.hydrationContainerVolumeMilliliters();
    final lastCustom =
        _hydrationRepository.lastCustomHydrationAmountMilliliters();
    return HydrationEntrySettings(
      containerVolumeOverridesMilliliters: <String, double>{
        for (final override in overrides.entries)
          if (isValidHydrationContainerMilliliters(override.value))
            override.key: override.value,
      },
      lastCustomAmountMilliliters:
          (lastCustom != null && isValidHydrationContainerMilliliters(lastCustom))
              ? lastCustom
              : null,
    );
  }
}
