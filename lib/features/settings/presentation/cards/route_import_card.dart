import 'package:cross_file/cross_file.dart';

import '../../../../core/presentation/file_picking.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/presentation/command_state.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../navigation/app_routes.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../imports/application/route_bulk_import_view_model.dart';
import '../../../manualentry/activity/activity_entry_view_model.dart';
import '../../application/route_import_view_model.dart';
import '../settings_error_text.dart';

/// The Settings "Data Import" route-file card. 1:1 port of the Kotlin
/// `RouteImportCard` (`SettingsCards.kt`): icon + title + body, a route-import
/// permission count, the last bulk result / error / progress feedback, an
/// optional Grant button, a single-import (review) button and a bulk-import
/// (direct write) button.
///
/// The permissions, the grant command and both import intents live in
/// [RouteImportViewModel]; the card picks the files (a platform concern, with
/// test seams) and navigates. Single import delegates to the activity-entry form
/// for review, exactly as the Kotlin card raised an `ExternalRouteImportRequest`.
class RouteImportCard extends ConsumerWidget {
  const RouteImportCard({
    super.key,
    this.pickRouteFile,
    this.pickRouteFiles,
    this.onNavigateToEntry,
  });

  /// Test seam for the single-file picker; defaults to `file_selector`'s
  /// [openFile] filtered by [kRouteImportMimeTypes].
  final Future<XFile?> Function()? pickRouteFile;

  /// Test seam for the multi-file picker; defaults to [openFiles].
  final Future<List<XFile>> Function()? pickRouteFiles;

  /// Test seam for navigation to the activity-entry route after a single import.
  final void Function(BuildContext context)? onNavigateToEntry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    final state = ref.watch(routeImportCardProvider);
    final notifier = ref.read(routeImportCardProvider.notifier);
    final bulk = ref.watch(routeBulkImportProvider);

    final importPermissions = state.importPermissions;
    final grantedCount = state.grantedCount;
    final missingPermissions = state.missingPermissions;
    final healthConnectAvailable = state.healthConnectAvailable;
    final isImporting = bulk.isImporting;
    // A grant in flight blocks the buttons too, but it is not an import — the
    // "Importing…" label stays off.
    final isBusy = isImporting || state.isGranting;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _RouteImportHeader(
                icon: Icons.map_outlined,
                title: l10n.settingsRouteImportTitle,
                body: l10n.settingsRouteImportBody,
              ),
              const SizedBox(height: 12),
              Text(
                l10n.settingsRouteImportPermissions(
                  grantedCount,
                  importPermissions.length,
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              if (bulk.result case final result?) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.settingsRouteImportResult(
                    result.importedFiles,
                    result.failedFiles,
                    result.totalFiles,
                  ),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.primary),
                ),
              ],
              if (bulk.error case final error? when error.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  l10n.settingsRouteImportError(error),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.error),
                ),
              ],
              if (isImporting) ...[
                const SizedBox(height: 12),
                const LinearProgressIndicator(),
                const SizedBox(height: 8),
                Builder(builder: (context) {
                  final progress = bulk.progress ??
                      const RouteBulkImportProgress(totalFiles: 0);
                  return Text(
                    l10n.settingsRouteImportProgress(
                      progress.currentFileIndex,
                      progress.totalFiles,
                      progress.importedFiles,
                      progress.failedFiles,
                    ),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.primary),
                  );
                }),
              ],
              if (state.grant case CommandFailure<void>(:final error)) ...[
                const SizedBox(height: 8),
                Text(
                  settingsErrorText(error),
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.error),
                ),
              ],
              if (missingPermissions.isNotEmpty) ...[
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.tonal(
                    onPressed: healthConnectAvailable && !isBusy
                        ? notifier.grantPermissions
                        : null,
                    child: Text(l10n.settingsRouteImportGrant),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: isBusy
                      ? null
                      : () => _importSingle(context, notifier),
                  icon: const Icon(Icons.folder_open_outlined, size: 18),
                  label: Text(l10n.settingsRouteImportAction),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: healthConnectAvailable &&
                          missingPermissions.isEmpty &&
                          !isBusy
                      ? () => _importBulk(notifier)
                      : null,
                  icon: const Icon(Icons.folder_open_outlined, size: 18),
                  label: Text(
                    isImporting
                        ? l10n.settingsRouteImporting
                        : l10n.settingsRouteImportBulkAction,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _importSingle(
    BuildContext context,
    RouteImportViewModel notifier,
  ) async {
    final file = await _pickSingle();
    if (file == null) return;
    final bytes = await file.readAsBytes();
    notifier.stageSingleImport(
      ActivityRouteFileHandle(bytes: bytes, fileName: file.name),
    );
    if (!context.mounted) return;
    final navigate = onNavigateToEntry ??
        (ctx) => ctx.push(AppRoutes.activityEntry);
    navigate(context);
  }

  Future<void> _importBulk(RouteImportViewModel notifier) async {
    final files = await _pickMultiple();
    if (files.isEmpty) return;
    // Handed over as sources, not bytes: every picked file used to be READ here,
    // all of them, before the first one was written. An XFile is a path, so
    // deferring the read to the importer costs nothing and bounds the heap at
    // one file however many were picked.
    await notifier.importRouteFiles([
      for (final file in files)
        ActivityRouteFileSource(fileName: file.name, read: file.readAsBytes),
    ]);
  }

  Future<XFile?> _pickSingle() {
    final picker = pickRouteFile;
    if (picker != null) return picker();
    return pickInputFile();
  }

  Future<List<XFile>> _pickMultiple() {
    final picker = pickRouteFiles;
    if (picker != null) return picker();
    return pickInputFiles();
  }
}


/// The card header: icon + title + body copy. Shared shape between the route and
/// FIT import cards.
class _RouteImportHeader extends StatelessWidget {
  const _RouteImportHeader({
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 2),
          child: Icon(icon, size: 20, color: theme.colorScheme.primary),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(
                body,
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

/// Exposes the shared header for the FIT import card without duplicating it.
class RouteImportCardHeader extends StatelessWidget {
  const RouteImportCardHeader({
    super.key,
    required this.icon,
    required this.title,
    required this.body,
  });

  final IconData icon;
  final String title;
  final String body;

  @override
  Widget build(BuildContext context) =>
      _RouteImportHeader(icon: icon, title: title, body: body);
}
