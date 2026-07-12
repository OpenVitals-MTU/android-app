import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../di/providers.dart';
import '../../activity/maps/offline_map_import_controller.dart';
import '../../activity/maps/offline_map_models.dart';

part 'offline_maps_view_model.freezed.dart';

/// The offline-maps card's import lifecycle. The imported LIBRARY itself lives
/// on [OfflineMapImportController]'s own listenable — this state carries only
/// what the import command produces.
@freezed
abstract class OfflineMapsState with _$OfflineMapsState {
  const OfflineMapsState._();

  const factory OfflineMapsState({
    @Default(CommandState<OfflineMapPack>.idle())
    CommandState<OfflineMapPack> import,

    /// Live progress while the import runs; null at rest.
    OfflineMapImportProgress? progress,
  }) = _OfflineMapsState;

  bool get isImporting => import is CommandRunning<OfflineMapPack>;

  /// The pack the last import produced, if it succeeded.
  OfflineMapPack? get importedPack => switch (import) {
        CommandSuccess<OfflineMapPack>(:final value) => value,
        _ => null,
      };

  ScreenError? get importError => switch (import) {
        CommandFailure<OfflineMapPack>(:final error) => error,
        _ => null,
      };
}

/// Owns the offline-map import, the pack deletion and the render-format choice.
///
/// The import is a [CommandState]: the widget-level try/catch that used to sit
/// around [OfflineMapImportController.importMap] is gone — a failed import lands
/// as [CommandFailure] and the card renders it.
class OfflineMapsViewModel extends Notifier<OfflineMapsState> {
  @override
  OfflineMapsState build() => const OfflineMapsState();

  /// Copies the picked file into the map library. `file_selector`'s Android
  /// implementation copies the picked document into the app cache, so the path
  /// handed in here is a plain readable file path.
  Future<void> importMap(File file, {required String originalFileName}) async {
    final controller = await _controller();
    if (!ref.mounted) return;
    state = OfflineMapsState(
      import: const CommandState.running(),
      progress: const OfflineMapImportProgress(),
    );
    try {
      final pack = await controller.importMap(
        file,
        originalFileName: originalFileName,
        onProgress: (progress) {
          if (!ref.mounted || !state.isImporting) return;
          state = state.copyWith(progress: progress);
        },
      );
      if (!ref.mounted) return;
      state = OfflineMapsState(import: CommandState.success(pack));
    } catch (error) {
      if (!ref.mounted) return;
      state = OfflineMapsState(
        import: CommandState.failure(
          throwableToScreenError(
            error is ArgumentError ? '${error.message}' : error,
          ),
        ),
      );
    }
  }

  Future<void> deleteMap(String id) async {
    final controller = await _controller();
    await controller.deleteMap(id);
  }

  Future<void> setActiveFormat(OfflineMapPackFormat? format) async {
    final controller = await _controller();
    controller.setActiveFormat(format);
  }

  Future<OfflineMapImportController> _controller() =>
      ref.read(offlineMapImportControllerProvider.future);
}

/// The state provider for the offline-maps settings card.
final offlineMapsCardProvider =
    NotifierProvider<OfflineMapsViewModel, OfflineMapsState>(
  OfflineMapsViewModel.new,
);
