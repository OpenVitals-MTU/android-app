import 'package:cross_file/cross_file.dart';

import '../../../../core/presentation/file_picking.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../navigation/app_routes.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../imports/application/pending_route_import.dart';
import '../../../imports/application/route_bulk_import_view_model.dart';
import '../../../manualentry/activity/activity_entry_view_model.dart';
import '../../application/fit_import_view_model.dart';
import 'route_import_card.dart';

/// The Settings "Data Import" FIT card: import ONE file with a review step, or a
/// whole FOLDER straight through.
///
/// The single import is the Kotlin `FitImportCard` unchanged — it hands the file
/// to the activity-entry form ([pendingRouteImportProvider]) so the user can
/// check what was detected before it is written. That is the right shape for one
/// file and the wrong one for two hundred, which is what the folder button is
/// for: it writes every FIT file under the picked folder straight to Health
/// Connect, tolerating a bad file rather than dying on it, and reporting how many
/// landed.
///
/// The folder is picked as a SAF tree and walked natively — see
/// `RouteFolderSource` for why a folder PATH could not have worked here, and why
/// this needs no storage permission.
class FitImportCard extends ConsumerWidget {
  const FitImportCard({
    super.key,
    this.pickFitFile,
    this.onNavigateToEntry,
  });

  /// Test seam for the single-file picker.
  final Future<XFile?> Function()? pickFitFile;

  /// Test seam for navigation to the activity-entry route after a single import.
  final void Function(BuildContext context)? onNavigateToEntry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final state = ref.watch(fitImportCardProvider);
    final bulk = ref.watch(fitBulkImportProvider);
    final progress =
        bulk.progress ?? const RouteBulkImportProgress(totalFiles: 0);
    final isBusy = state.isScanning || bulk.isImporting;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              RouteImportCardHeader(
                icon: Icons.directions_run_outlined,
                title: l10n.settingsFitImportTitle,
                body: l10n.settingsFitImportBody,
              ),
              if (bulk.result case final result?) ...[
                const SizedBox(height: 12),
                Text(
                  result.skippedFiles > 0
                      ? l10n.settingsRouteImportResultWithSkipped(
                          result.importedFiles,
                          result.skippedFiles,
                          result.failedFiles,
                          result.totalFiles,
                        )
                      : l10n.settingsRouteImportResult(
                          result.importedFiles,
                          result.failedFiles,
                          result.totalFiles,
                        ),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.primary),
                ),
              ],
              // The folder held more files than the scan will take. Said out
              // loud, because an import that silently skipped the tail would
              // read exactly like one that finished.
              if (state.truncatedAt case final limit?) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.settingsFitImportFolderTruncated(limit),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
              // Not an error: the folder was perfectly readable and simply had
              // no FIT files in it.
              if (state.folderHadNoFitFiles) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.settingsFitImportFolderEmpty,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
              for (final line in [state.error, bulk.error])
                if (line case final message? when message.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(
                    l10n.settingsRouteImportError(message),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error),
                  ),
                ],
              if (isBusy) ...[
                const SizedBox(height: 12),
                const LinearProgressIndicator(),
                const SizedBox(height: 8),
                Text(
                  bulk.isImporting
                      ? l10n.settingsRouteImportProgress(
                          progress.currentFileIndex,
                          progress.totalFiles,
                          progress.importedFiles,
                          progress.failedFiles,
                        )
                      // Walking a memory card with a thousand rides on it takes
                      // a moment, and a button that looks dead gets pressed again.
                      : l10n.settingsFitImportFolderScanning,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.primary),
                ),
              ],
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: isBusy ? null : () => _importFit(context, ref),
                  icon: const Icon(Icons.insert_drive_file_outlined, size: 18),
                  label: Text(l10n.settingsFitImportAction),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: isBusy
                      ? null
                      : ref.read(fitImportCardProvider.notifier).importFolder,
                  icon: const Icon(Icons.folder_open_outlined, size: 18),
                  label: Text(
                    bulk.isImporting
                        ? l10n.settingsRouteImporting
                        : l10n.settingsFitImportFolderAction,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _importFit(BuildContext context, WidgetRef ref) async {
    final file = await _pickFit();
    if (file == null) return;
    final bytes = await file.readAsBytes();
    ref.read(pendingRouteImportProvider.notifier).set(
          ActivityRouteFileHandle(bytes: bytes, fileName: file.name),
        );
    if (!context.mounted) return;
    final navigate =
        onNavigateToEntry ?? (ctx) => ctx.push(AppRoutes.activityEntry);
    navigate(context);
  }

  Future<XFile?> _pickFit() {
    final picker = pickFitFile;
    if (picker != null) return picker();
    return pickInputFile();
  }
}
