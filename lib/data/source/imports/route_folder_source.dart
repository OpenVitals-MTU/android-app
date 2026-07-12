import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// One activity file found inside a picked folder: its display name, and the
/// document URI that can open it.
///
/// A URI, not a path. The folder pick returns a SAF **tree URI**, and that URI —
/// not any `/storage/emulated/0/...` string derived from it — is what actually
/// grants access to the files under it. See [RouteFolderSource].
typedef RouteFolderFile = ({String uri, String name});

/// What a folder pick found.
typedef RouteFolderPick = ({
  List<RouteFolderFile> files,

  /// The folder held more files than the scan is willing to list. Reported
  /// rather than swallowed: an import that quietly skips half a folder reads to
  /// the user like an import that finished.
  bool truncated,
});

/// Picks a FOLDER of activity files and opens them one at a time.
///
/// ## Why this is not `file_picker`
///
/// `file_picker` can pick a directory, but it converts Android's SAF tree URI
/// into a filesystem path and discards the URI. Under scoped storage the app
/// cannot open a non-media file at a raw path like that — a `.fit` under
/// `Documents/` reads back as `FileNotFoundException` — and the only way to make
/// such a path work is All-files access, a permission a health app has no
/// business holding and Play reviews accordingly.
///
/// So the tree URI is kept, and the native side walks it with `DocumentsContract`
/// and opens each child through the `ContentResolver`. The pick grants access to
/// the tree; nothing else is needed, and no permission is declared.
///
/// ## Why the bytes are not returned by the pick
///
/// [pickFolder] returns names and URIs only. A folder of a year's rides is
/// hundreds of megabytes, and neither the platform channel nor the heap should
/// ever carry more than one file: the bulk importer calls [readFile] when it
/// reaches a file and drops the bytes when it is done.
abstract class RouteFolderSource {
  /// Asks the user for a folder, then lists the files inside it (recursively)
  /// whose name ends in one of [extensions], e.g. `['fit']`.
  ///
  /// Null when the user cancels — which is a normal thing to do, and not an
  /// error.
  Future<RouteFolderPick?> pickFolder({required List<String> extensions});

  /// Opens one file. Throws when it cannot be read: a folder scanned a minute
  /// ago can name a file that has since been moved, and the bulk importer counts
  /// that as one failed file rather than a failed batch.
  Future<Uint8List> readFile(String uri);
}

class MethodChannelRouteFolderSource implements RouteFolderSource {
  const MethodChannelRouteFolderSource();

  static const _channel =
      MethodChannel('tech.mmarca.openvitals/folder_import');

  @override
  Future<RouteFolderPick?> pickFolder({
    required List<String> extensions,
  }) async {
    final result = await _channel.invokeMapMethod<String, Object?>(
      'pickFolder',
      {'extensions': extensions},
    );
    if (result == null) return null;
    final files = <RouteFolderFile>[
      for (final entry in (result['files'] as List<Object?>? ?? const []))
        if (entry case final Map<Object?, Object?> file)
          if (file['uri'] case final String uri)
            (uri: uri, name: file['name'] as String? ?? uri),
    ];
    return (files: files, truncated: result['truncated'] as bool? ?? false);
  }

  @override
  Future<Uint8List> readFile(String uri) async {
    final bytes = await _channel.invokeMethod<Uint8List>(
      'readFile',
      {'uri': uri},
    );
    if (bytes == null) {
      throw PlatformException(
        code: 'unreadable',
        message: 'The file could not be read.',
      );
    }
    return bytes;
  }
}

final routeFolderSourceProvider = Provider<RouteFolderSource>(
  (ref) => const MethodChannelRouteFolderSource(),
);
