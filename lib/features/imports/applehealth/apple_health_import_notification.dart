/// Notification texts for the background Apple Health import, ported from the
/// Kotlin `AppleHealthImportWorker.buildNotification`.
///
/// Both the ongoing-notification text (rendered in the foreground-service
/// isolate, where there is no `BuildContext`) and the card's progress line come
/// from here, so the phase wording can never drift between the two.
library;

import '../../../l10n/app_localizations.dart';
import '../../homewidgets/home_widget_refresher.dart' show homeWidgetLocalizations;
import 'apple_health_import_models.dart';

/// The notification channel the import's foreground service posts to (Kotlin
/// `AppleHealthImportWorker.ChannelId`).
const String kAppleHealthImportChannelId = 'apple_health_imports';

/// The device locale's localizations for an isolate with no widget tree — the
/// same lookup the home-widget refresher and the recording controller do.
AppLocalizations appleHealthImportLocalizations() => homeWidgetLocalizations();

/// The phase label (Kotlin `AppleHealthImportPhase.labelRes`).
String appleHealthImportPhaseLabel(
  AppLocalizations l10n,
  AppleHealthImportPhase phase,
) =>
    switch (phase) {
      AppleHealthImportPhase.queued =>
        l10n.settingsAppleHealthImportProgressQueued,
      AppleHealthImportPhase.parsing =>
        l10n.settingsAppleHealthImportProgressParsing,
      AppleHealthImportPhase.converting =>
        l10n.settingsAppleHealthImportProgressConverting,
      AppleHealthImportPhase.checkingDuplicates =>
        l10n.settingsAppleHealthImportProgressCheckingDuplicates,
      AppleHealthImportPhase.writing =>
        l10n.settingsAppleHealthImportProgressWriting,
      AppleHealthImportPhase.finishing =>
        l10n.settingsAppleHealthImportProgressFinishing,
      AppleHealthImportPhase.buildingReport =>
        l10n.settingsAppleHealthImportProgressBuildingReport,
      AppleHealthImportPhase.complete =>
        l10n.settingsAppleHealthImportProgressComplete,
    };

/// The ongoing notification's content text, in the Kotlin worker's three
/// variants:
///
/// * percent known **and** the export's element count known — scan progress
///   (`scanned/expected`), the only meaningful denominator while parsing;
/// * percent known but no element count — selected-record progress
///   (`selected/expected`);
/// * percent unknown — phase + raw scanned/imported counters.
String appleHealthImportNotificationText(
  AppLocalizations l10n,
  AppleHealthImportProgress progress,
) {
  final phaseText = appleHealthImportPhaseLabel(l10n, progress.phase);
  final percent = progress.percent;
  if (percent == null) {
    return l10n.settingsAppleHealthImportNotificationText(
      phaseText,
      progress.parsedElements,
      progress.importedRecords,
    );
  }
  if (progress.expectedParsedElements > 0) {
    return l10n.settingsAppleHealthImportNotificationTextWithScanPercent(
      percent,
      phaseText,
      progress.parsedElements,
      progress.expectedParsedElements,
      progress.importedRecords,
    );
  }
  return l10n.settingsAppleHealthImportNotificationTextWithPercent(
    percent,
    phaseText,
    progress.selectedPreparedRecords,
    progress.expectedSelectedRecords,
    progress.importedRecords,
  );
}
