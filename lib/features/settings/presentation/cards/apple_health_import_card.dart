import 'dart:io';

// Still used for SAVING the import report (getSaveLocation reads nothing, so the
// byte-slurping bug that forced the pick path off file_selector does not apply).
import 'package:file_selector/file_selector.dart';

import '../../../../core/presentation/file_picking.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../../../../di/providers.dart';
import '../../../../domain/model/health_connect_availability.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/health_connect_gate.dart';
import '../../../../ui/components/ov_card.dart';
import '../../../imports/applehealth/apple_health_import_models.dart';
import '../../../imports/applehealth/apple_health_import_notification.dart';
import '../../../imports/applehealth/apple_health_import_view_model.dart';
import '../../../imports/applehealth/apple_health_import_staging_store.dart';

/// The Kotlin `AppleHealthExportMimeTypes` (application/zip, application/xml,
/// text/xml, application/octet-stream, any). The trailing unconstrained group
/// keeps `.zip`/`.xml` exports with generic MIME types selectable everywhere.
const List<String> kAppleHealthExportMimeTypes = <String>[
  'application/zip',
  'application/xml',
  'text/xml',
  'application/octet-stream',
];

/// Settings "Apple Health import" card — a faithful port of the Kotlin
/// `AppleHealthImportCard` (`SettingsCards.kt`). Drives the already-DI-wired
/// [appleHealthImportProvider]: pick an export, analyze it, choose
/// categories, import the selected set, and copy/save the shareable report.
class AppleHealthImportCard extends ConsumerWidget {
  const AppleHealthImportCard({
    super.key,
    this.pickExportSource,
    this.saveReportFile,
  });

  /// Test seam for the system file picker; defaults to `file_selector`'s
  /// [openFile] with the Apple Health export type groups. Returns the picked
  /// export as a streamable [AppleHealthExportSource] (or `null` when the user
  /// cancels) — deliberately NOT its bytes: a multi-gigabyte export must never
  /// be read into RAM.
  final Future<AppleHealthExportSource?> Function()? pickExportSource;

  /// Test seam for the report save flow; defaults to [_defaultSaveReport].
  /// Returns `true` on success.
  final Future<bool> Function(String content, String suggestedName)?
      saveReportFile;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(appleHealthImportProvider);
    final notifier = ref.read(appleHealthImportProvider.notifier);

    final availability = ref.watch(healthConnectAvailabilityProvider).value;
    final granted =
        ref.watch(grantedHealthPermissionsProvider).value ?? const <String>{};
    final importPermissions =
        ref.watch(healthRepositoryProvider).dataImportWritePermissions;

    final grantedCount = importPermissions.where(granted.contains).length;
    final missingPermissions = importPermissions.difference(granted);
    final healthConnectAvailable =
        availability == HealthConnectAvailability.available;
    final isBusy = state.isBusy;
    final canAnalyze = healthConnectAvailable && !isBusy;
    final canImportSelected = healthConnectAvailable &&
        missingPermissions.isEmpty &&
        !isBusy &&
        state.analysis != null &&
        state.selectedCategories.isNotEmpty;

    final children = <Widget>[
      // Header: icon + title + body.
      Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Icon(
              Icons.folder_open_outlined,
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
                  l10n.settingsAppleHealthImportTitle,
                  style: theme.textTheme.titleSmall,
                ),
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    l10n.settingsAppleHealthImportBody,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      // Permission count line ("X of Y").
      Padding(
        padding: const EdgeInsets.only(top: 12),
        child: Text(
          l10n.settingsAppleHealthImportPermissions(
            grantedCount,
            importPermissions.length,
          ),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ),
    ];

    // Prior-result summary (six counters) + Copy report / Save report.
    final result = state.result;
    if (result != null) {
      children.add(Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Text(
          l10n.settingsAppleHealthImportResult(
            result.importedRecords,
            result.duplicateSkippedRecords,
            result.notSelectedRecords,
            result.unsupportedElements,
            result.skippedRecords,
            result.failedRecords,
          ),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.primary),
        ),
      ));
      if (result.workoutRoutesIncomplete) {
        children.add(Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Text(
            l10n.settingsAppleHealthImportRoutesIncomplete,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.error),
          ),
        ));
      }
      children.add(Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _copy(
                  context,
                  result.shareableReportText,
                  l10n.settingsAppleHealthImportReportCopied,
                ),
                icon: const Icon(Icons.content_copy_outlined, size: 18),
                label: Text(l10n.settingsAppleHealthImportCopyReport),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _saveReport(context, l10n, notifier),
                icon: const Icon(Icons.download_outlined, size: 18),
                label: Text(l10n.settingsAppleHealthImportSaveReport),
              ),
            ),
          ],
        ),
      ));
    }

    // Analysis result + per-category checklist.
    final analysis = state.analysis;
    if (analysis != null) {
      children.add(Padding(
        padding: const EdgeInsets.only(top: 12),
        child: Text(
          l10n.settingsAppleHealthImportAnalysisResult(
            analysis.parsedElements,
            analysis.convertedRecords,
            analysis.unsupportedElements,
            analysis.failedRecords,
          ),
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.primary),
        ),
      ));
      children.add(Padding(
        padding: const EdgeInsets.only(top: 4),
        child: Text(
          l10n.settingsAppleHealthImportChooseCategories,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ));
      children.add(const SizedBox(height: 8));
      for (final summary in analysis.categorySummaries) {
        children.add(_CategoryRow(
          summary: summary,
          checked: state.selectedCategories.contains(summary.category),
          enabled: !isBusy,
          onChanged: (selected) =>
              notifier.setCategorySelected(summary.category, selected ?? false),
        ));
      }
    }

    // Error block (permission-denied variant) + Copy error / Save report.
    final error = state.error;
    if (error != null && error.isNotEmpty) {
      final errorText = l10n.settingsAppleHealthImportError(error);
      final displayText = state.permissionDenied
          ? l10n.settingsAppleHealthImportPermissionDenied
          : errorText;
      children.add(Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            OutlinedButton.icon(
              onPressed: () => _copy(
                context,
                errorText,
                l10n.settingsAppleHealthImportErrorCopied,
              ),
              icon: const Icon(Icons.content_copy_outlined, size: 18),
              label: Text(l10n.settingsAppleHealthImportCopyError),
            ),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: () => _saveReport(context, l10n, notifier),
              icon: const Icon(Icons.download_outlined, size: 18),
              label: Text(l10n.settingsAppleHealthImportSaveReport),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: SelectionArea(
                child: Text(
                  displayText,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.error),
                ),
              ),
            ),
          ],
        ),
      ));
    }

    // Progress bar + phase label (percent when known, else indeterminate).
    if (isBusy) {
      final importProgress = (state.isAnalyzing
              ? state.analysisProgress
              : state.progress) ??
          const AppleHealthImportProgress();
      final percent = importProgress.percent;
      children.add(Padding(
        padding: const EdgeInsets.only(top: 12),
        child: LinearProgressIndicator(
          value: percent == null ? null : percent.clamp(0, 100) / 100.0,
          minHeight: 8,
        ),
      ));
      children.add(Padding(
        padding: const EdgeInsets.only(top: 8),
        child: Text(
          // Kotlin's three variants. The discriminator is the *scan* total, not
          // the phase: while the export's element count is known the percent is
          // the scan percent, so the line has to name the same denominator.
          switch ((percent, importProgress.expectedParsedElements)) {
            (null, _) => l10n.settingsAppleHealthImportProgress(
                appleHealthImportPhaseLabel(l10n, importProgress.phase),
                importProgress.parsedElements,
                importProgress.importedRecords,
              ),
            (final int pct, final int expectedParsedElements)
                when expectedParsedElements > 0 =>
              l10n.settingsAppleHealthImportProgressWithScanPercent(
                pct,
                appleHealthImportPhaseLabel(l10n, importProgress.phase),
                importProgress.parsedElements,
                expectedParsedElements,
                importProgress.selectedPreparedRecords,
                importProgress.expectedSelectedRecords,
                importProgress.importedRecords,
              ),
            (final int pct, _) =>
              l10n.settingsAppleHealthImportProgressWithPercent(
                pct,
                appleHealthImportPhaseLabel(l10n, importProgress.phase),
                importProgress.selectedPreparedRecords,
                importProgress.expectedSelectedRecords,
                importProgress.importedRecords,
              ),
          },
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.primary),
        ),
      ));
      if (state.isImporting) {
        children.add(Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            l10n.settingsAppleHealthImportBackground,
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ));
      }
    }

    // Grant button (only if missing perms).
    if (missingPermissions.isNotEmpty) {
      children.add(Padding(
        padding: const EdgeInsets.only(top: 12),
        child: SizedBox(
          width: double.infinity,
          child: FilledButton.tonal(
            onPressed: healthConnectAvailable && !isBusy
                ? () => _grant(ref, missingPermissions)
                : null,
            child: Text(l10n.settingsAppleHealthImportGrant),
          ),
        ),
      ));
    }

    // Import-selected button (only after an analysis exists).
    if (analysis != null) {
      children.add(Padding(
        padding: const EdgeInsets.only(top: 8),
        child: SizedBox(
          width: double.infinity,
          child: FilledButton.tonal(
            onPressed: canImportSelected ? notifier.importSelected : null,
            child: Text(
              state.isImporting
                  ? l10n.settingsAppleHealthImporting
                  : l10n.settingsAppleHealthImportSelectedAction,
            ),
          ),
        ),
      ));
    }

    // Analyze / Choose-another button.
    children.add(Padding(
      padding: const EdgeInsets.only(top: 8),
      child: SizedBox(
        width: double.infinity,
        child: OutlinedButton(
          onPressed: canAnalyze ? () => _pickAndAnalyze(notifier) : null,
          child: Text(
            state.isAnalyzing
                ? l10n.settingsAppleHealthImportAnalyzing
                : analysis == null
                    ? l10n.settingsAppleHealthImportAnalyzeAction
                    : l10n.settingsAppleHealthImportChooseAnotherAction,
          ),
        ),
      ),
    ));

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: children,
          ),
        ),
      ),
    );
  }

  Future<void> _pickAndAnalyze(AppleHealthImportViewModel notifier) async {
    final source = await _pickSource();
    if (source == null) return;
    await notifier.analyze(source);
  }

  Future<AppleHealthExportSource?> _pickSource() async {
    final picker = pickExportSource;
    if (picker != null) return picker();
    // An export.zip is routinely GIGABYTES. It must be picked by path:
    // `file_selector` reads the whole file into a byte[] and would OOM long before
    // the importer ever saw it (see [pickInputFile]).
    final file = await pickInputFile();
    if (file == null) return null;
    // The picker's reported length is the expectation the staging store then
    // verifies its copy against (Kotlin: the SAF `SIZE` column).
    final size = await file.length();
    return AppleHealthExportSource(
      sourceId: file.path,
      fingerprint: AppleHealthExportFingerprint(
        displayName: file.name,
        size: size,
      ),
      openRead: file.openRead,
    );
  }

  void _grant(WidgetRef ref, Set<String> missing) {
    final repo = ref.read(healthRepositoryProvider);
    // Fire the request, then refresh the granted set (mirrors the Kotlin
    // permission launcher callback invalidating the granted permissions).
    repo.requestPermissions(missing).whenComplete(
          () => ref.invalidate(grantedHealthPermissionsProvider),
        );
  }

  Future<void> _copy(
    BuildContext context,
    String text,
    String confirmation,
  ) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (context.mounted) {
      ScaffoldMessenger.maybeOf(context)
          ?.showSnackBar(SnackBar(content: Text(confirmation)));
    }
  }

  Future<void> _saveReport(
    BuildContext context,
    AppLocalizations l10n,
    AppleHealthImportViewModel notifier,
  ) async {
    final content = notifier.reportTextForSave;
    const suggestedName = 'openvitals-apple-health-import-report.txt';
    final saver = saveReportFile ?? _defaultSaveReport;
    final ok = await saver(content, suggestedName);
    if (context.mounted) {
      ScaffoldMessenger.maybeOf(context)?.showSnackBar(SnackBar(
        content: Text(
          ok
              ? l10n.settingsAppleHealthImportReportSaved
              : l10n.settingsAppleHealthImportReportSaveFailed,
        ),
      ));
    }
  }

  /// Writes the report to a user-chosen location where `getSaveLocation` is
  /// supported (desktop), falling back to the app documents directory on
  /// platforms whose `file_selector` implementation has no save picker (Android
  /// — the analogue of Kotlin's SAF `CreateDocument`, which has no cross-plugin
  /// Flutter equivalent here).
  static Future<bool> _defaultSaveReport(
    String content,
    String suggestedName,
  ) async {
    try {
      final location = await getSaveLocation(suggestedName: suggestedName);
      if (location == null) return false;
      await File(location.path).writeAsString(content);
      return true;
    } catch (_) {
      try {
        final dir = await getApplicationDocumentsDirectory();
        await File('${dir.path}/$suggestedName').writeAsString(content);
        return true;
      } catch (_) {
        return false;
      }
    }
  }

}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({
    required this.summary,
    required this.checked,
    required this.enabled,
    required this.onChanged,
  });

  final AppleHealthImportCategorySummary summary;
  final bool checked;
  final bool enabled;
  final ValueChanged<bool?> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Checkbox(
            value: checked,
            onChanged: enabled ? onChanged : null,
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _title(l10n, summary.category),
                    style: theme.textTheme.bodyMedium,
                  ),
                  Text(
                    _description(l10n, summary.category),
                    style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      summary.routeSessions > 0
                          ? l10n.settingsAppleHealthImportCategoryCountRoutes(
                              summary.convertedRecords,
                              summary.routeSessions,
                            )
                          : l10n.settingsAppleHealthImportCategoryCount(
                              summary.convertedRecords,
                            ),
                      style: theme.textTheme.bodySmall
                          ?.copyWith(color: theme.colorScheme.primary),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _title(AppLocalizations l10n, AppleHealthImportCategory category) =>
      switch (category) {
        AppleHealthImportCategory.workouts =>
          l10n.settingsAppleHealthImportCategoryWorkouts,
        AppleHealthImportCategory.activity =>
          l10n.settingsAppleHealthImportCategoryActivity,
        AppleHealthImportCategory.heart =>
          l10n.settingsAppleHealthImportCategoryHeart,
        AppleHealthImportCategory.sleep =>
          l10n.settingsAppleHealthImportCategorySleep,
        AppleHealthImportCategory.body =>
          l10n.settingsAppleHealthImportCategoryBody,
        AppleHealthImportCategory.vitals =>
          l10n.settingsAppleHealthImportCategoryVitals,
        AppleHealthImportCategory.nutrition =>
          l10n.settingsAppleHealthImportCategoryNutrition,
        AppleHealthImportCategory.hydration =>
          l10n.settingsAppleHealthImportCategoryHydration,
        AppleHealthImportCategory.mindfulness =>
          l10n.settingsAppleHealthImportCategoryMindfulness,
        AppleHealthImportCategory.cycle =>
          l10n.settingsAppleHealthImportCategoryCycle,
      };

  String _description(
    AppLocalizations l10n,
    AppleHealthImportCategory category,
  ) =>
      switch (category) {
        AppleHealthImportCategory.workouts =>
          l10n.settingsAppleHealthImportCategoryWorkoutsDesc,
        AppleHealthImportCategory.activity =>
          l10n.settingsAppleHealthImportCategoryActivityDesc,
        AppleHealthImportCategory.heart =>
          l10n.settingsAppleHealthImportCategoryHeartDesc,
        AppleHealthImportCategory.sleep =>
          l10n.settingsAppleHealthImportCategorySleepDesc,
        AppleHealthImportCategory.body =>
          l10n.settingsAppleHealthImportCategoryBodyDesc,
        AppleHealthImportCategory.vitals =>
          l10n.settingsAppleHealthImportCategoryVitalsDesc,
        AppleHealthImportCategory.nutrition =>
          l10n.settingsAppleHealthImportCategoryNutritionDesc,
        AppleHealthImportCategory.hydration =>
          l10n.settingsAppleHealthImportCategoryHydrationDesc,
        AppleHealthImportCategory.mindfulness =>
          l10n.settingsAppleHealthImportCategoryMindfulnessDesc,
        AppleHealthImportCategory.cycle =>
          l10n.settingsAppleHealthImportCategoryCycleDesc,
      };
}
