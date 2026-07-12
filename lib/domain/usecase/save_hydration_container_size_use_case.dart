import '../../data/repository/contract/hydration_repository.dart';

/// Remembers that a default container has been resized — **synchronously**, like
/// the read that will pick it up again (`ReadHydrationSettingsUseCase`).
///
/// Only the seven built-in containers have an id worth persisting. The container
/// synthesized while editing an existing entry is a one-off shaped like the
/// record being edited, and writing it back would resize a preset the user never
/// touched; the caller is what knows which is which.
class SaveHydrationContainerSizeUseCase {
  const SaveHydrationContainerSizeUseCase(this._hydrationRepository);

  final HydrationRepository _hydrationRepository;

  void call(String containerId, double milliliters) =>
      _hydrationRepository.setHydrationContainerVolumeMilliliters(
        containerId,
        milliliters,
      );
}
