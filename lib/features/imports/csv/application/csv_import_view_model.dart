import 'package:cross_file/cross_file.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../../core/presentation/command_state.dart';
import '../../../../core/presentation/file_picking.dart';
import '../../../../core/presentation/report_saving.dart';
import '../../../../core/presentation/report_sharing.dart';
import '../../../../core/presentation/screen_error.dart';
import '../../../../core/result/result.dart';
import '../../../../di/providers.dart';
import '../../../../ui/components/health_connect_gate.dart';
import '../csv_column_mapping.dart';
import '../csv_import_report.dart';
import '../csv_datetime_format.dart';
import '../csv_import_metric.dart';
import '../csv_import_models.dart';
import '../csv_import_service.dart';
import '../csv_table_reader.dart';

part 'csv_import_view_model.freezed.dart';

/// Which step of the importer the screen is showing.
enum CsvImportStep { pick, mapping, confirm, importing, done }

/// The importer's state.
///
/// Progress and result are a tally, NOT a [CommandState]: a finished import is
/// not success-or-failure but "written 4,102, already present 210, rejected 68",
/// and it is observable *while it runs*, which `CommandRunning` carries nothing
/// to express. That is `RouteBulkImportState`'s shape and it is the right one
/// here. The permission grant on the same screen IS a single failable action, so
/// it is the one field that stays a command.
@freezed
abstract class CsvImportState with _$CsvImportState {
  const CsvImportState._();

  const factory CsvImportState({
    @Default(CsvImportStep.pick) CsvImportStep step,

    /// Absolute path of the picked file. Never its bytes.
    String? filePath,
    String? fileName,
    CsvSample? sample,
    CsvImportMapping? mapping,
    @Default(<CsvMappingIssue>[]) List<CsvMappingIssue> issues,
    @Default(false) bool isLoadingFile,
    @Default(false) bool isImporting,
    CsvImportProgress? progress,
    CsvImportResult? result,

    /// A failure that stopped the screen doing anything useful — an unreadable
    /// file, not a rejected row.
    String? error,
    @Default(CommandState<void>.idle()) CommandState<void> grant,

    /// The last report save: `success(true)` saved, `success(false)` cancelled
    /// or refused by the platform. A single failable action, so unlike the
    /// import itself it IS a command.
    @Default(CommandState<bool>.idle()) CommandState<bool> saveReport,

    /// The last report share. Carries no value: unlike a save, a share that
    /// reached the sheet is done — whether the user then picked WhatsApp,
    /// Signal or nothing at all is between them and the sheet, and Android
    /// does not report it back.
    @Default(CommandState<void>.idle()) CommandState<void> shareReport,
    @Default(<String>{}) Set<String> granted,

    /// The write permissions the INSTALLED Health Connect provider actually
    /// defines, already filtered through `filterSupportedPermissions`.
    ///
    /// Requesting one a provider does not define throws (AGENTS.md invariant
    /// #5), and the app pins a connect-client ahead of what most providers
    /// implement — so the catalog alone is not safe to ask for.
    @Default(<String>{}) Set<String> supportedWritePermissions,
  }) = _CsvImportState;

  /// Whether the mapping is complete enough to import.
  bool get canContinue => issues.isEmpty && (mapping?.metricColumns.isNotEmpty ?? false);

  /// The write permissions this mapping needs, can actually be asked for on
  /// this device, and does not already have.
  ///
  /// The intersection is what keeps an unsupported permission out of the
  /// request. A metric whose permission the provider does not define simply
  /// cannot be granted, and asking would throw rather than be refused.
  Set<String> get missingPermissions {
    final required = mapping?.requiredWritePermissions ?? const <String>{};
    return required.intersection(supportedWritePermissions).difference(granted);
  }

  bool get isGranting => grant is CommandRunning<void>;

  /// Sampled rows, for validation and the preview table.
  List<List<String>> get sampleRows => sample?.dataRows ?? const [];
}

/// Owns one CSV import from pick to result.
class CsvImportViewModel extends Notifier<CsvImportState> {
  CsvImportCancellation? _cancellation;

  @override
  CsvImportState build() {
    // LISTEN, never watch.
    //
    // `grantedHealthPermissionsProvider` is invalidated on every app open
    // (`data_refresh_bootstrap.dart`), and a Notifier's `build()` re-running
    // DISCARDS its state. Returning from the system file picker IS an app open,
    // so watching it threw away the file the user had just picked — and only on
    // the first attempt, because the refresh is guarded to once per 30s, which
    // is why picking the same file a second time appeared to work.
    //
    // `ref.listen` does not rebuild, so the new value folds into the existing
    // state and an in-progress mapping survives. This is the view-model half of
    // the AGENTS.md rule that screens listen and view-models do not watch what
    // the refresh cycle invalidates.
    ref.listen(grantedHealthPermissionsProvider, (_, next) {
      final granted = next.value;
      if (granted != null) state = state.copyWith(granted: granted);
    });
    ref.listen(instantMeasurementWritePermissionsProvider, (_, next) {
      state = state.copyWith(supportedWritePermissions: next);
    });

    return CsvImportState(
      granted:
          ref.read(grantedHealthPermissionsProvider).value ?? const <String>{},
      supportedWritePermissions:
          ref.read(instantMeasurementWritePermissionsProvider),
    );
  }

  /// Picks a file and advances to the mapping step.
  ///
  /// [picker] is a test seam so a widget test never opens a real SAF dialog.
  Future<void> pickFile({Future<XFile?> Function()? picker}) async {
    final file = await (picker ?? pickInputFile)();
    // Cancelling the picker is not an error and must not change the step.
    if (file == null) return;

    state = state.copyWith(
      isLoadingFile: true,
      error: null,
      result: null,
      progress: null,
    );

    try {
      final reader = ref.read(csvTableReaderProvider);
      final sample = await reader.sample(file.path);
      if (!ref.mounted) return;

      if (sample.isEmpty) {
        state = state.copyWith(
          isLoadingFile: false,
          filePath: file.path,
          fileName: file.name,
          sample: sample,
          mapping: null,
          step: CsvImportStep.mapping,
          issues: const [],
        );
        return;
      }

      final mapping = initialCsvMapping(
        headerRow: sample.headerRow,
        sample: sample.dataRows,
      );
      state = state.copyWith(
        isLoadingFile: false,
        filePath: file.path,
        fileName: file.name,
        sample: sample,
        mapping: mapping,
        issues: validateCsvMapping(mapping, sample.dataRows),
        step: CsvImportStep.mapping,
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(isLoadingFile: false, error: '$error');
    }
  }

  /// Re-reads the head of the file under a changed dialect.
  Future<void> setDialect(CsvDialect dialect, {bool? hasHeaderRow}) async {
    final path = state.filePath;
    if (path == null) return;
    final header = hasHeaderRow ?? state.sample?.hasHeaderRow ?? true;

    state = state.copyWith(isLoadingFile: true);
    try {
      final sample = await ref.read(csvTableReaderProvider).sample(
            path,
            dialect: dialect,
            hasHeaderRow: header,
          );
      if (!ref.mounted) return;
      final mapping = initialCsvMapping(
        headerRow: sample.headerRow,
        sample: sample.dataRows,
      );
      state = state.copyWith(
        isLoadingFile: false,
        sample: sample,
        mapping: mapping,
        issues: validateCsvMapping(mapping, sample.dataRows),
      );
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(isLoadingFile: false, error: '$error');
    }
  }

  /// Points column [columnIndex] at [role]/[metric], defaulting the
  /// interpretation from the column's own header unit.
  void setColumnRole(
    int columnIndex, {
    required CsvColumnRole role,
    CsvImportMetric? metric,
  }) {
    final mapping = state.mapping;
    if (mapping == null) return;

    final headerUnit = _headerUnit(columnIndex);
    final spec = metric == null ? null : kCsvMetricCatalog[metric];
    final interpretation = spec == null
        ? null
        : (headerUnit == null
            ? spec.defaultInterpretation
            : interpretationForUnit(spec, headerUnit) ??
                spec.defaultInterpretation);

    _applyMapping(
      mapping.withColumn(
        CsvColumnMapping(
          columnIndex: columnIndex,
          role: role,
          metric: role == CsvColumnRole.metric ? metric : null,
          interpretation: role == CsvColumnRole.metric ? interpretation : null,
        ),
      ),
    );
  }

  /// Changes how one already-mapped column's number is read.
  void setColumnInterpretation(
    int columnIndex,
    CsvValueInterpretation interpretation,
  ) {
    final mapping = state.mapping;
    if (mapping == null) return;
    final column = mapping.columns.firstWhere(
      (it) => it.columnIndex == columnIndex,
      orElse: () => CsvColumnMapping(columnIndex: columnIndex),
    );
    _applyMapping(
      mapping.withColumn(column.copyWith(interpretation: interpretation)),
    );
  }

  void setDateTimeSettings(CsvDateTimeSettings settings) {
    final mapping = state.mapping;
    if (mapping == null) return;
    _applyMapping(mapping.copyWith(dateTime: settings));
  }

  void goToStep(CsvImportStep step) => state = state.copyWith(step: step);

  /// Requests the write permissions the mapping needs — only those.
  Future<void> grantPermissions() async {
    final missing = state.missingPermissions;
    if (missing.isEmpty) return;
    state = state.copyWith(grant: const CommandState.running());

    final result =
        await ref.read(healthRepositoryProvider).requestPermissions(missing);
    if (!ref.mounted) return;

    switch (result) {
      case Ok():
        state = state.copyWith(grant: const CommandState.success(null));
        ref.invalidate(grantedHealthPermissionsProvider);
      case Err(:final failure):
        state = state.copyWith(
          grant: CommandState.failure(
            failure.toScreenError(fallback: 'Unable to request permissions.'),
          ),
        );
    }
  }

  /// Runs the import. Progress lands on the state as it goes.
  Future<void> startImport() async {
    final path = state.filePath;
    final mapping = state.mapping;
    final sample = state.sample;
    if (path == null || mapping == null || sample == null) return;

    final cancellation = CsvImportCancellation();
    _cancellation = cancellation;
    state = state.copyWith(
      step: CsvImportStep.importing,
      isImporting: true,
      progress: const CsvImportProgress(),
      result: null,
      error: null,
    );

    final result = await ref.read(csvImportServiceProvider).run(
          path: path,
          dialect: sample.dialect,
          mapping: mapping,
          hasHeaderRow: sample.hasHeaderRow,
          cancellation: cancellation,
          onProgress: (progress) {
            if (!ref.mounted) return;
            state = state.copyWith(progress: progress);
          },
        );
    if (!ref.mounted) return;

    state = state.copyWith(
      isImporting: false,
      step: CsvImportStep.done,
      progress: result.progress,
      result: result,
    );
    _cancellation = null;
  }

  void cancelImport() => _cancellation?.cancel();

  /// The finished run rendered as text, or null when there is nothing to save.
  String? get reportText {
    final result = state.result;
    final mapping = state.mapping;
    if (result == null || mapping == null) return null;
    return buildCsvImportReport(
      fileName: state.fileName,
      mapping: mapping,
      result: result,
      headerRow: state.sample?.headerRow ?? const [],
      fieldDelimiter: state.sample?.dialect.fieldDelimiter,
      hasHeaderRow: state.sample?.hasHeaderRow,
    );
  }

  /// Saves the report through [saver] (defaulting to the platform save picker).
  ///
  /// A command, not a tally: it is one action that either happened or did not,
  /// and a save that throws must land as [CommandFailure] rather than as an
  /// uncaught error in a button callback.
  Future<void> saveReport({TextReportSaver? saver}) async {
    final content = reportText;
    if (content == null) return;
    state = state.copyWith(saveReport: const CommandState.running());

    try {
      final saved =
          await (saver ?? saveTextReport)(content, kCsvImportReportFileName);
      if (!ref.mounted) return;
      state = state.copyWith(saveReport: CommandState.success(saved));
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        saveReport: CommandState.failure(
          throwableToScreenError(
            error,
            fallback: 'Unable to save the report.',
          ),
        ),
      );
    }
  }

  /// Stages the report as a file and hands it to the system share sheet, so it
  /// can leave the app as a WhatsApp/Signal/Telegram message or an email
  /// attachment.
  ///
  /// [chooserTitle] is localized, so it comes from the view rather than being
  /// built here.
  Future<void> shareReport({
    required String chooserTitle,
    TextReportSharer? sharer,
  }) async {
    final content = reportText;
    if (content == null) return;
    state = state.copyWith(shareReport: const CommandState.running());

    try {
      await (sharer ?? shareTextReport)(
        content,
        kCsvImportReportFileName,
        chooserTitle,
      );
      if (!ref.mounted) return;
      state = state.copyWith(shareReport: const CommandState.success(null));
    } catch (error) {
      if (!ref.mounted) return;
      state = state.copyWith(
        shareReport: CommandState.failure(
          throwableToScreenError(
            error,
            fallback: 'Unable to share the report.',
          ),
        ),
      );
    }
  }

  /// The view consumes a finished save (it shows one snackbar) and returns the
  /// command to rest, so rebuilding the step cannot replay it.
  void clearSaveReport() =>
      state = state.copyWith(saveReport: const CommandState.idle());

  /// The share's counterpart of [clearSaveReport]. A successful share says
  /// nothing (the sheet is its own feedback); only a failure is announced.
  void clearShareReport() =>
      state = state.copyWith(shareReport: const CommandState.idle());

  /// Returns to the pick step, keeping nothing from the finished run.
  void reset() => state = CsvImportState(granted: state.granted);

  void _applyMapping(CsvImportMapping mapping) {
    state = state.copyWith(
      mapping: mapping,
      issues: validateCsvMapping(mapping, state.sampleRows),
    );
  }

  CsvUnit? _headerUnit(int columnIndex) {
    final header = state.sample?.headerRow;
    if (header == null || columnIndex >= header.length) return null;
    return detectCsvUnitInHeader(header[columnIndex]);
  }
}

/// Non-auto-dispose: navigating away from the screen must not kill an import
/// that is already writing to Health Connect, and coming back re-attaches to it.
final csvImportProvider =
    NotifierProvider<CsvImportViewModel, CsvImportState>(CsvImportViewModel.new);

final csvTableReaderProvider = Provider<CsvTableReader>(
  (ref) => const CsvTableReader(),
);

final csvImportServiceProvider = Provider<CsvImportService>(
  (ref) => CsvImportService(
    ref.watch(importWriteRepositoryProvider),
    reader: ref.watch(csvTableReaderProvider),
  ),
);
