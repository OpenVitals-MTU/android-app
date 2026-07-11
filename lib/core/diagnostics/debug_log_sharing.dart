import 'dart:io';

import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

/// Shares the sanitized diagnostics log as a `text/plain` attachment — the port
/// of the Kotlin `Context.shareDebugDiagnosticsLog()`
/// (`core/diagnostics/DebugLogSharing.kt`).
///
/// Kotlin writes the export into `cacheDir/diagnostics_exports/`, hands it to
/// its own `FileProvider` (`$packageName.fileprovider`, declared in
/// `res/xml/file_paths.xml`) and fires `Intent.createChooser(ACTION_SEND)`.
///
/// Here the same file lands in the `path_provider` temporary directory (the
/// `cacheDir` analogue) and `share_plus` raises the chooser. The FileProvider
/// half of the Kotlin change is **absorbed by the plugin**: `share_plus` ships
/// its own provider (authority `${applicationId}.flutter.share_provider`, paths
/// in `flutter_share_file_paths.xml`) and copies any file handed to it into
/// `cacheDir/share_plus/` before granting the URI — so the Android host needs
/// no `file_paths.xml` entry of its own.
class DebugLogSharing {
  const DebugLogSharing();

  /// Kotlin `DiagnosticsExportCacheDirectory`.
  static const String exportDirectoryName = 'diagnostics_exports';

  /// Kotlin `DiagnosticsExportFileName`.
  static const String exportFileName = 'openvitals-diagnostics-logs.txt';

  /// Kotlin `DiagnosticsMimeType`.
  static const String mimeType = 'text/plain';

  /// Writes [content] to the diagnostics export file and raises the share
  /// sheet titled [chooserTitle].
  ///
  /// Throws on any failure (directory creation, write, or no share target);
  /// the caller is expected to catch, mirroring Kotlin's `runCatching`.
  Future<void> shareDiagnosticsLog({
    required String content,
    required String chooserTitle,
  }) async {
    final cacheDir = await getTemporaryDirectory();
    final exportDirectory =
        Directory('${cacheDir.path}/$exportDirectoryName');
    await exportDirectory.create(recursive: true);

    final exportFile = File('${exportDirectory.path}/$exportFileName');
    await exportFile.writeAsString(content);

    await SharePlus.instance.share(
      ShareParams(
        files: [XFile(exportFile.path, mimeType: mimeType)],
        // Android maps this onto Intent.createChooser's title, which is exactly
        // what Kotlin passes settings_debug_logs_share_chooser_title to.
        title: chooserTitle,
      ),
    );
  }
}
