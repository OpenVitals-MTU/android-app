import 'dart:io';

// Still used for SAVING the import report (getSaveLocation reads nothing, so the
// byte-slurping bug that forced the pick path off file_selector does not apply).
import 'package:file_selector/file_selector.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:path_provider/path_provider.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../imports/applehealth/apple_health_import_view_model.dart';

part 'apple_health_import_card_view_model.freezed.dart';

/// Writes the report to a user-chosen location where `getSaveLocation` is
/// supported (desktop), falling back to the app documents directory on
/// platforms whose `file_selector` implementation has no save picker (Android
/// — the analogue of Kotlin's SAF `CreateDocument`, which has no cross-plugin
/// Flutter equivalent here). Returns `false` when the user cancels.
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

  /// The card consumes a finished save (it shows one snackbar) and returns the
  /// command to rest, so re-entering the section cannot replay it.
  void clearSaveReport() => _setSaveReport(const CommandState.idle());

  void _setGrant(CommandState<void> next) {
    _grant = next;
    state = state.copyWith(grant: next);
  }

  void _setSaveReport(CommandState<bool> next) {
    _saveReport = next;
    state = state.copyWith(saveReport: next);
  }
}

/// The default [AppleHealthReportSaver]: the platform save picker, falling back
/// to the app documents directory where there is none (Android).
Future<bool> defaultSaveAppleHealthReport(
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

/// The state provider for the Settings Apple-Health import card.
final appleHealthImportCardProvider = NotifierProvider<
    AppleHealthImportCardViewModel, AppleHealthImportCardState>(
  AppleHealthImportCardViewModel.new,
);
