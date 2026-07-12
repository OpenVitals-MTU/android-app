import 'package:cross_file/cross_file.dart';

import '../../../../core/presentation/file_picking.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../l10n/app_localizations.dart';
import '../../../../navigation/app_routes.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../imports/application/pending_route_import.dart';
import '../../../manualentry/activity/activity_entry_notifier.dart';
import '../../../manualentry/activity/routeimport/activity_route_import_types.dart';
import 'route_import_card.dart';

/// The Settings "Data Import" FIT card. 1:1 port of the Kotlin `FitImportCard`
/// (`SettingsCards.kt`): icon + title + body + a single import button. Like the
/// route single-import path it delegates to the activity-entry form for review —
/// it sets a [pendingRouteImportProvider] handle (the [RouteFileParser] handles
/// FIT natively) and navigates to the entry route.
class FitImportCard extends ConsumerWidget {
  const FitImportCard({
    super.key,
    this.pickFitFile,
    this.onNavigateToEntry,
  });

  /// Test seam for the FIT file picker; defaults to `file_selector`'s [openFile]
  /// filtered by [kFitImportMimeTypes].
  final Future<XFile?> Function()? pickFitFile;

  /// Test seam for navigation to the activity-entry route after import.
  final void Function(BuildContext context)? onNavigateToEntry;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
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
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: () => _importFit(context, ref),
                  icon: const Icon(Icons.folder_open_outlined, size: 18),
                  label: Text(l10n.settingsFitImportAction),
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
