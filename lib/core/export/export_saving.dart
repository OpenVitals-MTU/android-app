/// Writing an export to storage the user picked.
///
/// The counterpart of `export_sharing.dart`: sharing sends the file to another
/// app, this one puts it where the user chose to keep it. Both are offered
/// wherever an export exists, because neither answers the other's question.
///
/// ## Why file_picker and not file_selector
///
/// `file_selector`'s `getSaveLocation` has **no Android implementation** — it is
/// the analogue of SAF's `CreateDocument`, which that plugin never ported — so
/// on a phone it throws. Every caller therefore wrapped it in a `catch` that
/// wrote to the app documents directory instead, which no file manager reaches:
/// the save reported success and the user could not find the file. Only the
/// activity-route export had it right, on `file_picker`'s `saveFile`, which
/// raises the real SAF `CREATE_DOCUMENT` picker on Android and writes the bytes
/// through the returned content URI. Unifying on the one that works is the whole
/// point of this file; the documents-directory write survives only as a
/// last-resort fallback for a platform with no picker at all, where a file the
/// user cannot easily find still beats a button that throws.
///
/// (This is the mirror of `file_picking.dart`'s rule for INPUT files, and for
/// the same reason: file_picker is the plugin whose Android half actually
/// works. Saving never had file_selector's whole-file-into-a-byte[] problem —
/// `getSaveLocation` reads nothing — it simply does not exist there.)
library;

import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:path_provider/path_provider.dart';

/// Writes an export under [suggestedName]. Returns false when the user cancels
/// or the platform refuses.
///
/// Injected as a seam so a test never opens a real save dialog.
typedef ExportSaver = Future<bool> Function(
  String suggestedName,
  Uint8List bytes,
);

/// Raises the platform save picker for [suggestedName] and writes [bytes] to
/// wherever the user points it.
///
/// Returns false when the user cancels — cancelling is a choice, not an error,
/// and must never be rendered as one.
Future<bool> saveExportBytes(String suggestedName, Uint8List bytes) async {
  try {
    // Writes the bytes itself on every platform: through the SAF content URI on
    // Android/iOS, through `saveBytesToFile` on the desktop implementations.
    final savedPath = await FilePicker.platform.saveFile(
      fileName: suggestedName,
      bytes: bytes,
    );
    return savedPath != null;
  } catch (_) {
    return _saveToDocuments(suggestedName, bytes);
  }
}

/// [saveExportBytes] for text exports — reports, the diagnostics log.
Future<bool> saveExportText(String suggestedName, String content) =>
    saveExportBytes(suggestedName, Uint8List.fromList(utf8.encode(content)));

/// Last resort for a platform with no save picker at all. Not reachable on
/// Android, which is exactly the point — see the library comment.
Future<bool> _saveToDocuments(String suggestedName, Uint8List bytes) async {
  try {
    final directory = await getApplicationDocumentsDirectory();
    await File('${directory.path}/$suggestedName')
        .writeAsBytes(bytes, flush: true);
    return true;
  } catch (_) {
    return false;
  }
}
