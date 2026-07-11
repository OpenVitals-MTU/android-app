import 'dart:async';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import 'offline_map_models.dart';

/// The offline base map currently selected for rendering: the active format
/// plus the on-disk paths of its packs. Ported from the Kotlin
/// `OfflineRouteMapOrPreview` selection logic (`activeFormat` +
/// `activeMapPacks.filter { it.file.exists() }`).
class OfflineBaseMap {
  const OfflineBaseMap({required this.format, required this.packPaths});

  final OfflineMapPackFormat format;

  /// Absolute paths of the active packs whose files exist, in library order.
  final List<String> packPaths;

  @override
  bool operator ==(Object other) =>
      other is OfflineBaseMap &&
      other.format == format &&
      const ListEquality<String>().equals(other.packPaths, packPaths);

  @override
  int get hashCode =>
      Object.hash(format, const ListEquality<String>().hash(packPaths));
}

/// The live offline map library, re-emitted after every import / delete /
/// active-format change. Kotlin equivalent: `OfflineMapRepository.state`
/// collected with the lifecycle.
final offlineMapLibraryProvider = StreamProvider<OfflineMapLibraryState>(
  (ref) async* {
    final controller =
        await ref.watch(offlineMapImportControllerProvider.future);
    final updates = StreamController<OfflineMapLibraryState>();
    void onChanged() {
      if (!updates.isClosed) updates.add(controller.state.value);
    }

    controller.state.addListener(onChanged);
    ref.onDispose(() {
      controller.state.removeListener(onChanged);
      updates.close();
    });
    yield controller.state.value;
    yield* updates.stream;
  },
);

/// The active offline base map, or null when no format is active, no active
/// pack file exists on disk, or the library is still loading / failed to load
/// (widget tests without platform channels fall in the last bucket and render
/// the plain route preview, matching the Kotlin fallback).
final offlineBaseMapProvider = Provider<OfflineBaseMap?>((ref) {
  final library = ref.watch(offlineMapLibraryProvider).value;
  final format = library?.activeFormat;
  if (library == null || format == null) return null;
  final packPaths = library.activeMapPacks
      .map((pack) => pack.path)
      .where((path) => File(path).existsSync())
      .toList();
  if (packPaths.isEmpty) return null;
  return OfflineBaseMap(format: format, packPaths: packPaths);
});
