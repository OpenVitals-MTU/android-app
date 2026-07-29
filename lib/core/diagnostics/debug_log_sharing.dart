import '../export/export_sharing.dart';
import '../export/export_staging.dart';

/// Shares the sanitized diagnostics log as a `text/plain` attachment — the port
/// of the Kotlin `Context.shareDebugDiagnosticsLog()`
/// (`core/diagnostics/DebugLogSharing.kt`).
///
/// Kotlin writes the export into `cacheDir/diagnostics_exports/`, hands it to
/// its own `FileProvider` (`$packageName.fileprovider`, declared in
/// `res/xml/file_paths.xml`) and fires `Intent.createChooser(ACTION_SEND)`.
/// Here the same file lands in the same-named directory under the cache root
/// via [ExportStagingCache] and goes to the sheet via [shareStagedFile], the two
/// halves it now shares with the route exports and the import reports. The
/// FileProvider half is absorbed by share_plus, which ships its own provider
/// (authority `${applicationId}.flutter.share_provider`) and copies the file
/// into `cacheDir/share_plus/` before granting the URI — so the Android host
/// needs no `file_paths.xml` entry of its own.
///
/// Moving onto the shared cache also fixed a leak: this path never pruned, so
/// every share left another copy of the log in the cache forever. The shared
/// cache prunes on every stage.
class DebugLogSharing {
  const DebugLogSharing({ExportStagingCache? cache})
      : cache = cache ?? const ExportStagingCache(
              directoryName: exportDirectoryName,
            );

  final ExportStagingCache cache;

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
    ShareSheet? share,
  }) async {
    final file = await cache.stageText(exportFileName, content);
    await shareStagedFile(
      file: file,
      mimeType: mimeType,
      chooserTitle: chooserTitle,
      subject: exportFileName,
      share: share,
    );
  }
}
