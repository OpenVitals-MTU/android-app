/// Saving a plain-text diagnostic report to wherever the platform allows.
///
/// Extracted from the Apple Health importer's card view-model when the CSV
/// importer became the second caller. Both hand the user a text report they can
/// send on when something did not import the way they expected, and the "where
/// does a file go on this platform" answer is identical for both.
library;

import 'dart:io';

// SAVING is the one job file_selector keeps: `getSaveLocation` reads nothing, so
// the whole-file-into-a-byte[] problem that banned it for INPUT files (see
// `file_picking.dart`) does not apply here.
import 'package:file_selector/file_selector.dart';
import 'package:path_provider/path_provider.dart';

/// Writes [content] under [suggestedName]. Returns false when the user cancels
/// or the platform refuses.
///
/// Injected as a seam so a test never opens a real save dialog.
typedef TextReportSaver = Future<bool> Function(
  String content,
  String suggestedName,
);

/// The default [TextReportSaver]: the platform save picker, falling back to the
/// app documents directory where there is none.
///
/// Android has no `getSaveLocation` implementation — it is the analogue of
/// Kotlin's SAF `CreateDocument`, which has no cross-plugin Flutter equivalent —
/// so on a phone this always takes the fallback and lands in app documents. That
/// is deliberate: a report the user cannot find is still better than a button
/// that throws.
Future<bool> saveTextReport(String content, String suggestedName) async {
  try {
    final location = await getSaveLocation(suggestedName: suggestedName);
    if (location == null) return false;
    await File(location.path).writeAsString(content);
    return true;
  } catch (_) {
    try {
      final directory = await getApplicationDocumentsDirectory();
      await File('${directory.path}/$suggestedName').writeAsString(content);
      return true;
    } catch (_) {
      return false;
    }
  }
}
