/// Handing a plain-text report to another app as an attachment.
///
/// The sibling of `saveTextReport` in `report_saving.dart`. Saving answers "put
/// this file somewhere I can open it later"; sharing answers "send this to
/// someone now" — a Signal, WhatsApp or Telegram message, an email attachment.
/// They are different intents and neither substitutes for the other, which is
/// why every import surface offers both.
///
/// The mechanics — staging into the app cache, pruning, building the
/// `ShareParams` — live in `core/export/`, shared with the route exports and the
/// diagnostics log. What is left here is what makes a *report* a report: it goes
/// as `text/plain` under its own name.
library;

import '../export/export_sharing.dart';
import '../export/export_staging.dart';

/// Stages [content] as [fileName] and raises the share sheet titled
/// [chooserTitle].
///
/// Throws when staging or the sheet fails; a user who dismisses the sheet
/// without picking a target is NOT a failure. Injected as a seam so a test never
/// raises a real sheet.
typedef TextReportSharer = Future<void> Function(
  String content,
  String fileName,
  String chooserTitle,
);

/// Writes a text report into the app cache and hands it to the system share
/// sheet as a `text/plain` attachment.
///
/// The report must reach the receiving app as a FILE, not as `ShareParams.text`:
/// an import report runs to hundreds of lines, which a messenger would paste
/// into the composer as one unsendable message. An attachment named
/// `openvitals-csv-import-report.txt` is what the recipient can actually open.
class TextReportSharing {
  const TextReportSharing({this.cache = const ExportStagingCache(
    directoryName: directoryName,
  ), this.share});

  /// Where the staged copy lands; overridden in tests.
  final ExportStagingCache cache;

  /// Test seam for raising the sheet; defaults to the real share sheet.
  final ShareSheet? share;

  /// Staging directory under the cache root, alongside `route_exports/` and
  /// `diagnostics_exports/`.
  static const String directoryName = 'report_exports';

  static const String mimeType = 'text/plain';

  Future<void> shareReport({
    required String content,
    required String fileName,
    required String chooserTitle,
  }) async {
    final file = await cache.stageText(fileName, content);
    await shareStagedFile(
      file: file,
      mimeType: mimeType,
      chooserTitle: chooserTitle,
      // The file name already says which importer produced the report, which is
      // what tells one from another in an inbox.
      subject: fileName,
      share: share,
    );
  }
}

/// The default [TextReportSharer].
Future<void> shareTextReport(
  String content,
  String fileName,
  String chooserTitle,
) =>
    const TextReportSharing().shareReport(
      content: content,
      fileName: fileName,
      chooserTitle: chooserTitle,
    );
