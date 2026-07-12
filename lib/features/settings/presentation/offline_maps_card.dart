import 'dart:io';

import 'package:cross_file/cross_file.dart';

import '../../../core/presentation/file_picking.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/external_link.dart';
import '../../../di/providers.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import '../../activity/maps/offline_map_import_controller.dart';
import '../../activity/maps/offline_map_models.dart';
import '../application/offline_maps_view_model.dart';
import 'settings_error_text.dart';

/// The MIME types the Kotlin picker (`OfflineMapMimeTypes`) accepts. PMTiles
/// and Mapsforge packs frequently carry a generic `application/octet-stream`
/// type, so the filter is deliberately permissive.
const List<String> kOfflineMapMimeTypes = [
  'application/vnd.pmtiles',
  'application/x-mapsforge-map',
  'application/octet-stream',
];

/// The offline maps settings card, ported from the Kotlin `OfflineMapsCard`
/// (`SettingsCards.kt`): title + body, a help prompt/link, the imported pack
/// list with per-pack delete, the render-format selector, import progress /
/// result / error feedback, and the import button.
///
/// Judgment calls:
/// * Kotlin runs the import through a WorkManager worker (with a foreground
///   notification), surviving the UI. This port runs the import directly on
///   [OfflineMapImportController] with its `onProgress` callback, so the
///   import lives with the UI; Kotlin's "import continues in the background
///   while you leave the app" note is omitted because it would be untrue here.
/// * Kotlin's help link fires an `ACTION_VIEW` intent; the Flutter port opens
///   the guide URL in the browser via [openExternalUrl] (with a SnackBar
///   fallback when no browser can handle it).
///
/// The import / delete / render-format actions live in [OfflineMapsViewModel];
/// the card only picks the file (a platform concern, with a test seam) and
/// renders the command. The imported LIBRARY is still read straight off the
/// controller's own listenable — it is a UI listenable, not a repository.
class OfflineMapsCard extends ConsumerWidget {
  const OfflineMapsCard({super.key, this.pickOfflineMapFile});

  /// Test seam for the system file picker; defaults to `file_selector`'s
  /// [openFile] with the offline-map type groups.
  final Future<XFile?> Function()? pickOfflineMapFile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controller = ref.watch(offlineMapImportControllerProvider).value;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          // While the controller future resolves, render the static chrome
          // only (header + help); the dynamic parts need the library.
          child: controller == null
              ? const _OfflineMapsHeader()
              : ValueListenableBuilder<OfflineMapLibraryState>(
                  valueListenable: controller.state,
                  builder: (context, library, _) =>
                      _buildBody(context, ref, library),
                ),
        ),
      ),
    );
  }

  Widget _buildBody(
    BuildContext context,
    WidgetRef ref,
    OfflineMapLibraryState library,
  ) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(offlineMapsCardProvider);
    final notifier = ref.read(offlineMapsCardProvider.notifier);
    final importing = state.isImporting;
    final progress = state.progress ?? const OfflineMapImportProgress();
    final percent = progress.percent;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const _OfflineMapsHeader(),
        const SizedBox(height: 12),
        Text(
          l10n.settingsOfflineMapsHelpPrompt,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        TextButton(
          onPressed: () => openExternalUrl(context, offlineMapsHelpUrl),
          child: Text(l10n.settingsOfflineMapsHelpLink),
        ),
        const SizedBox(height: 12),
        if (library.mapPacks.isEmpty)
          Text(
            l10n.settingsOfflineMapsEmpty,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          )
        else ...[
          _RenderFormatSelector(
            library: library,
            onSelectFormat: notifier.setActiveFormat,
          ),
          const SizedBox(height: 12),
          for (final pack in library.mapPacks)
            _OfflineMapPackRow(
              pack: pack,
              onDelete: () => notifier.deleteMap(pack.id),
            ),
        ],
        if (state.importedPack case final result?) ...[
          const SizedBox(height: 12),
          Text(
            l10n.settingsOfflineMapsImportResult(
              result.displayName,
              formatOfflineMapSize(result.sizeBytes),
            ),
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.primary),
          ),
        ],
        if (state.importError case final error?) ...[
          const SizedBox(height: 12),
          Text(
            l10n.settingsOfflineMapsImportError(settingsErrorText(error)),
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.error),
          ),
        ],
        if (importing) ...[
          const SizedBox(height: 12),
          LinearProgressIndicator(
            value: percent == null ? null : percent / 100,
          ),
          const SizedBox(height: 8),
          Text(
            percent == null
                ? l10n.settingsOfflineMapsImportProgress(
                    _phaseLabel(l10n, progress.phase),
                  )
                : l10n.settingsOfflineMapsImportProgressWithPercent(
                    _phaseLabel(l10n, progress.phase),
                    percent,
                  ),
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.primary),
          ),
        ],
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: importing ? null : () => _pickAndImport(notifier),
            icon: const Icon(Icons.folder_open_outlined, size: 18),
            label: Text(
              importing
                  ? l10n.settingsOfflineMapsImporting
                  : l10n.settingsOfflineMapsImportAction,
            ),
          ),
        ),
      ],
    );
  }

  /// Kotlin launches `OpenDocument()` with `OfflineMapMimeTypes` (ending in
  /// `*/*`); the unconstrained fallback group keeps packs with generic MIME
  /// types selectable on every platform.
  /// A map pack is hundreds of MB, so it MUST be picked by path.
  /// `file_selector` reads the whole file into memory and dies (see
  /// [pickInputFile]). The extension is checked after the pick, because SAF has no
  /// MIME type for `.pmtiles`/`.map` and never filtered on it anyway.
  Future<XFile?> _pickFile() {
    final picker = pickOfflineMapFile;
    if (picker != null) return picker();
    return pickInputFile();
  }

  Future<void> _pickAndImport(OfflineMapsViewModel notifier) async {
    final file = await _pickFile();
    if (file == null) return;
    await notifier.importMap(File(file.path), originalFileName: file.name);
  }
}

/// The card header: map icon + title + body copy.
class _OfflineMapsHeader extends StatelessWidget {
  const _OfflineMapsHeader();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 2),
          child: Icon(
            Icons.map_outlined,
            size: 20,
            color: theme.colorScheme.primary,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                l10n.settingsOfflineMapsTitle,
                style: theme.textTheme.titleSmall,
              ),
              const SizedBox(height: 4),
              Text(
                l10n.settingsOfflineMapsBody,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// Port of the Kotlin `OfflineMapRenderFormatSelector`
/// (`SingleChoiceSegmentedButtonRow`), rendered as this app's ChoiceChip idiom:
/// one chip per format with its pack count, disabled when no pack of that
/// format exists.
class _RenderFormatSelector extends StatelessWidget {
  const _RenderFormatSelector({
    required this.library,
    required this.onSelectFormat,
  });

  final OfflineMapLibraryState library;
  final ValueChanged<OfflineMapPackFormat> onSelectFormat;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.settingsOfflineMapsRenderFormatTitle,
          style: theme.textTheme.bodyMedium,
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final format in OfflineMapPackFormat.values)
              _formatChip(l10n, format),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          l10n.settingsOfflineMapsRenderFormatBody,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ],
    );
  }

  Widget _formatChip(AppLocalizations l10n, OfflineMapPackFormat format) {
    final packCount =
        library.mapPacks.where((pack) => pack.format == format).length;
    return ChoiceChip(
      label: Text(
        l10n.settingsOfflineMapsRenderFormatOption(
          offlineMapFormatLabel(l10n, format),
          packCount,
        ),
      ),
      selected: library.activeFormat == format,
      onSelected: packCount > 0 ? (_) => onSelectFormat(format) : null,
    );
  }
}

/// Port of the Kotlin `OfflineMapPackRow`: name + "format • file • size"
/// detail line and a delete icon button.
class _OfflineMapPackRow extends StatelessWidget {
  const _OfflineMapPackRow({required this.pack, required this.onDelete});

  final OfflineMapPack pack;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(pack.displayName, style: theme.textTheme.bodyMedium),
              Text(
                l10n.settingsOfflineMapsPackDetail(
                  offlineMapFormatLabel(l10n, pack.format),
                  pack.originalFileName,
                  formatOfflineMapSize(pack.sizeBytes),
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        IconButton(
          onPressed: onDelete,
          tooltip: l10n.actionDelete,
          icon: const Icon(Icons.delete_outline, size: 18),
        ),
      ],
    );
  }
}

/// Kotlin `OfflineMapPackFormat.settingsLabelRes`.
String offlineMapFormatLabel(AppLocalizations l10n, OfflineMapPackFormat format) =>
    switch (format) {
      OfflineMapPackFormat.pmtiles => l10n.settingsOfflineMapsFormatPmtiles,
      OfflineMapPackFormat.mapsforge =>
        l10n.settingsOfflineMapsFormatMapsforge,
    };

String _phaseLabel(AppLocalizations l10n, OfflineMapImportPhase phase) =>
    switch (phase) {
      OfflineMapImportPhase.queued =>
        l10n.settingsOfflineMapsImportProgressQueued,
      OfflineMapImportPhase.copying =>
        l10n.settingsOfflineMapsImportProgressCopying,
      OfflineMapImportPhase.complete =>
        l10n.settingsOfflineMapsImportProgressComplete,
    };

/// Kotlin `formatOfflineMapSize`: decimal units, one fraction digit.
String formatOfflineMapSize(int bytes) {
  if (bytes < 1000) return '$bytes B';
  const units = ['KB', 'MB', 'GB'];
  var value = bytes / 1000.0;
  var unitIndex = 0;
  while (value >= 1000.0 && unitIndex < units.length - 1) {
    value /= 1000.0;
    unitIndex += 1;
  }
  return '${value.toStringAsFixed(1)} ${units[unitIndex]}';
}
