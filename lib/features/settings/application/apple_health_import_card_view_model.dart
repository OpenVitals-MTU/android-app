import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/report_saving.dart';
import '../../../core/presentation/report_sharing.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../imports/applehealth/apple_health_import_view_model.dart';

part 'apple_health_import_card_view_model.freezed.dart';

/// Writes the report to a user-chosen location — the SAF `CreateDocument`
/// picker on Android, the native save dialog on desktop. Returns `false` when
/// the user cancels. See `core/export/export_saving.dart`.
typedef AppleHealthReportSaver = Future<bool> Function(
  String content,
  String suggestedName,
);

/// The suggested file name for a saved import report.
const String kAppleHealthReportFileName =
    'openvitals-apple-health-import-report.txt';

/// The Apple-Health card's own state — the permission gate around the import,
/// plus the two commands the card can fire. The import itself (analysis,
/// progress, category selection) stays in [AppleHealthImportViewModel]; nothing
/// is duplicated here.
@freezed
abstract class AppleHealthImportCardState with _$AppleHealthImportCardState {
  const AppleHealthImportCardState._();

  const factory AppleHealthImportCardState({
    @Default(<String>{}) Set<String> importPermissions,
    @Default(<String>{}) Set<String> granted,
    HealthConnectAvailability? availability,
    @Default(CommandState<void>.idle()) CommandState<void> grant,

    /// The last report save: `success(true)` saved, `success(false)` cancelled
    /// or refused by the platform.
    @Default(CommandState<bool>.idle()) CommandState<bool> saveReport,

    /// The last report share. Carries no value: a share that reached the sheet
    /// is done — whether the user then picked WhatsApp, Signal or nothing is
    /// between them and the sheet, and Android does not report it back.
    @Default(CommandState<void>.idle()) CommandState<void> shareReport,
  }) = _AppleHealthImportCardState;

  int get grantedCount => importPermissions.where(granted.contains).length;

  Set<String> get missingPermissions => importPermissions.difference(granted);

  bool get healthConnectAvailable =>
      availability == HealthConnectAvailability.available;

  bool get isGranting => grant is CommandRunning<void>;
}

/// Owns the Apple-Health card's repository access and its two failable actions.
///
/// The card no longer catches anything: a save that throws lands as a
/// [CommandFailure] here, and a grant that fails carries its [ScreenError].
class AppleHealthImportCardViewModel
    extends Notifier<AppleHealthImportCardState> {
  /// Both commands survive the rebuilds the granted-set refresh triggers.
  CommandState<void> _grant = const CommandState.idle();
  CommandState<bool> _saveReport = const CommandState.idle();
  CommandState<void> _shareReport = const CommandState.idle();

  @override
  AppleHealthImportCardState build() {
    final repo = ref.watch(healthRepositoryProvider);
    return AppleHealthImportCardState(
      importPermissions: repo.dataImportWritePermissions,
      granted: ref.watch(grantedHealthPermissionsProvider).value ??
          const <String>{},
      availability: ref.watch(healthConnectAvailabilityProvider).value,
      grant: _grant,
      saveReport: _saveReport,
      shareReport: _shareReport,
    );
  }

  /// Fires the permission request, then refreshes the granted set (mirrors the
  /// Kotlin permission-launcher callback invalidating the granted permissions).
  Future<void> grantPermissions() async {
    final missing = state.missingPermissions;
    if (missing.isEmpty) return;
    _setGrant(const CommandState.running());

    final result =
        await ref.read(healthRepositoryProvider).requestPermissions(missing);
    if (!ref.mounted) return;

    switch (result) {
      case Ok():
        _setGrant(const CommandState.success(null));
        ref.invalidate(grantedHealthPermissionsProvider);
      case Err(:final failure):
        _setGrant(
          CommandState.failure(
            failure.toScreenError(fallback: 'Unable to request permissions.'),
          ),
        );
    }
  }

  /// Saves the shareable report through [saver] (defaulting to the platform save
  /// picker). The whole flow is failable, so it is a command: a throwing save
  /// lands as [CommandFailure], not as an uncaught error in a button callback.
  Future<void> saveReport({AppleHealthReportSaver? saver}) async {
    _setSaveReport(const CommandState.running());
    final content = ref.read(appleHealthImportProvider.notifier).reportTextForSave;
    try {
      final ok =
          await (saver ?? defaultSaveAppleHealthReport)(
        content,
        kAppleHealthReportFileName,
      );
      if (!ref.mounted) return;
      _setSaveReport(CommandState.success(ok));
    } catch (error) {
      if (!ref.mounted) return;
      _setSaveReport(
        CommandState.failure(
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
  /// [chooserTitle] is localized, so it comes from the card rather than being
  /// built here.
  Future<void> shareReport({
    required String chooserTitle,
    TextReportSharer? sharer,
  }) async {
    _setShareReport(const CommandState.running());
    final content = ref.read(appleHealthImportProvider.notifier).reportTextForSave;
    try {
      await (sharer ?? shareTextReport)(
        content,
        kAppleHealthReportFileName,
        chooserTitle,
      );
      if (!ref.mounted) return;
      _setShareReport(const CommandState.success(null));
    } catch (error) {
      if (!ref.mounted) return;
      _setShareReport(
        CommandState.failure(
          throwableToScreenError(
            error,
            fallback: 'Unable to share the report.',
          ),
        ),
      );
    }
  }

  /// The card consumes a finished save (it shows one snackbar) and returns the
  /// command to rest, so re-entering the section cannot replay it.
  void clearSaveReport() => _setSaveReport(const CommandState.idle());

  /// The share's counterpart of [clearSaveReport]. A successful share says
  /// nothing (the sheet is its own feedback); only a failure is announced.
  void clearShareReport() => _setShareReport(const CommandState.idle());

  void _setGrant(CommandState<void> next) {
    _grant = next;
    state = state.copyWith(grant: next);
  }

  void _setSaveReport(CommandState<bool> next) {
    _saveReport = next;
    state = state.copyWith(saveReport: next);
  }

  void _setShareReport(CommandState<void> next) {
    _shareReport = next;
    state = state.copyWith(shareReport: next);
  }
}

/// The default [AppleHealthReportSaver].
///
/// The implementation moved to [saveTextReport] when the CSV importer became the
/// second caller; this stays as the name the Apple card and its tests already
/// use. Behaviour is unchanged.
Future<bool> defaultSaveAppleHealthReport(
  String content,
  String suggestedName,
) =>
    saveTextReport(content, suggestedName);

/// The state provider for the Settings Apple-Health import card.
final appleHealthImportCardProvider = NotifierProvider<
    AppleHealthImportCardViewModel, AppleHealthImportCardState>(
  AppleHealthImportCardViewModel.new,
);
