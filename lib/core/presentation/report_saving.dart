/// Saving a plain-text diagnostic report to storage the user picked.
///
/// Extracted from the Apple Health importer's card view-model when the CSV
/// importer became the second caller; the device-sync report and the
/// diagnostics log are the third and fourth. All four hand the user a text
/// report they can send on when something did not behave the way they expected,
/// and the "where does a file go on this platform" answer is identical for all
/// of them — it now lives once, in `core/export/export_saving.dart`, shared with
/// the binary route exports.
library;

import '../export/export_saving.dart';

/// Writes [content] under [suggestedName]. Returns false when the user cancels
/// or the platform refuses.
///
/// Injected as a seam so a test never opens a real save dialog.
typedef TextReportSaver = Future<bool> Function(
  String content,
  String suggestedName,
);

/// The default [TextReportSaver]: the platform save picker.
///
/// Argument order is `(content, suggestedName)` rather than [saveExportText]'s
/// `(suggestedName, content)` because three view-models and their tests already
/// pass it this way; the flip happens here rather than in every caller.
Future<bool> saveTextReport(String content, String suggestedName) =>
    saveExportText(suggestedName, content);
