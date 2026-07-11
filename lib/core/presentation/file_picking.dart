import 'package:collection/collection.dart';
import 'package:cross_file/cross_file.dart';
import 'package:file_picker/file_picker.dart';

/// Picks INPUT files by PATH, never by content.
///
/// ## Why not `file_selector`
///
/// `file_selector_android` cannot do this job. Its `FileSelectorApiImpl` reads the
/// whole picked file into a `byte[]` and ships it across the Pigeon channel:
///
/// ```java
/// final byte[] bytes = new byte[size];          // FileSelectorApiImpl.java:352
/// dataInputStream.readFully(bytes);
/// ```
///
/// A 205 MB offline-map pack therefore threw `OutOfMemoryError` against a 256 MB
/// heap, and an Apple Health `export.zip` — routinely gigabytes — never stood a
/// chance. It is not a bug we can tune around: the file's size IS the allocation.
///
/// `file_picker` with `withData: false` returns a **path**: it stream-copies the
/// content URI through an 8 KB buffer (`FileUtils.java:306`) and only allocates the
/// whole-file `byte[]` when asked to (`:269`). File size stops being a memory concern.
///
/// It is pinned to 9.x on purpose -- see the note in `pubspec.yaml`.
///
/// `file_selector` is still the right tool for SAVING (`getSaveLocation`), which
/// reads nothing.
///
/// ## Why no type filter
///
/// Android's SAF filters by MIME type, and the formats that matter here have none:
/// `.pmtiles` and `.map` are not registered types, which is what produced the
/// `W/FileSelectorApiImpl: Extension not supported: pmtiles` warnings. The old code
/// already conceded this — every `XTypeGroup` list ended in a catch-all
/// `XTypeGroup()`, so nothing was ever actually filtered.
///
/// So the picker shows every file and the callers validate afterwards, where they
/// can say something useful: `importMap` throws on a pack that is not
/// `.pmtiles`/`.map`/`.maps`, and the route parser sniffs the content as well as
/// the extension. That is strictly better than a filter that silently matched
/// nothing.

/// One file, as an [XFile] backed by a real path. Null when the user cancels.
///
Future<XFile?> pickInputFile() async {
  final result = await FilePicker.platform.pickFiles(
    // The whole point: hand back a path, never the bytes.
    withData: false,
    withReadStream: false,
  );
  final path = result?.files.firstOrNull?.path;
  return path == null ? null : XFile(path);
}

/// Several files at once (the bulk route import). Empty when the user cancels.
Future<List<XFile>> pickInputFiles() async {
  final result = await FilePicker.platform.pickFiles(
    allowMultiple: true,
    withData: false,
    withReadStream: false,
  );
  return <XFile>[
    for (final file in result?.files ?? const <PlatformFile>[])
      if (file.path != null) XFile(file.path!),
  ];
}
