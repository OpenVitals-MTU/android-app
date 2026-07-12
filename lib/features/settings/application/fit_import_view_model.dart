import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../data/source/imports/route_folder_source.dart';
import '../../../state/app_providers.dart';
import '../../imports/application/route_bulk_import_view_model.dart';

part 'fit_import_view_model.freezed.dart';

/// The FIT card's own instance of the bulk importer.
///
/// The same engine as the route card's, with its own state — the two cards sit
/// in the same Settings section, and a progress line reading "file 12/400" must
/// appear under the button that started it and nowhere else.
final fitBulkImportProvider =
    NotifierProvider<RouteBulkImportViewModel, RouteBulkImportState>(
  RouteBulkImportViewModel.new,
);

/// Everything about a folder import that happens BEFORE the files start
/// importing: choosing the folder, and what the scan found.
///
/// The import itself is [fitBulkImportProvider]'s business — this state does not
/// duplicate it.
@freezed
abstract class FitImportState with _$FitImportState {
  const factory FitImportState({
    /// The picker is up, or the tree is being walked. A folder of a thousand
    /// files takes a moment, and a button that looks dead is a button that gets
    /// pressed again.
    @Default(false) bool isScanning,

    /// The folder was readable and simply had no FIT files in it. Its own state,
    /// not an error: the user picked the wrong folder, nothing broke.
    @Default(false) bool folderHadNoFitFiles,

    /// How many files were listed, when the folder held more than the scan will
    /// take. Null when nothing was dropped.
    int? truncatedAt,

    /// The scan itself failed (an unreadable tree, a picker that would not
    /// open). A file that fails to import is the bulk importer's business.
    String? error,
  }) = _FitImportState;
}

/// Drives "import a folder of FIT files": pick a folder, list what is in it,
/// and hand it to the bulk importer one file at a time.
class FitImportViewModel extends Notifier<FitImportState> {
  @override
  FitImportState build() => const FitImportState();

  Future<void> importFolder() async {
    if (state.isScanning || ref.read(fitBulkImportProvider).isImporting) return;
    state = const FitImportState(isScanning: true);

    final source = ref.read(routeFolderSourceProvider);
    final RouteFolderPick? pick;
    try {
      pick = await source.pickFolder(extensions: const ['fit']);
    } catch (error) {
      if (!ref.mounted) return;
      state = FitImportState(error: _describe(error));
      return;
    }
    if (!ref.mounted) return;

    // Cancelled. The card goes back to how it was — no error, no result.
    if (pick == null) {
      state = const FitImportState();
      return;
    }
    if (pick.files.isEmpty) {
      state = const FitImportState(folderHadNoFitFiles: true);
      return;
    }

    state = FitImportState(
      truncatedAt: pick.truncated ? pick.files.length : null,
    );

    // Sources, not bytes: each file is opened when the importer reaches it. A
    // folder of four hundred rides costs the heap one ride at a time.
    await ref.read(fitBulkImportProvider.notifier).importRouteFiles(
          [
            for (final file in pick.files)
              ActivityRouteFileSource(
                fileName: file.name,
                read: () => source.readFile(file.uri),
              ),
          ],
          ref.read(unitSystemProvider),
        );
  }

  /// Dismisses the outcome lines so the card can be used again cleanly.
  void clear() => state = const FitImportState();

  String _describe(Object error) {
    final message = error.toString();
    return message.isEmpty ? 'The folder could not be read.' : message;
  }
}

final fitImportCardProvider =
    NotifierProvider<FitImportViewModel, FitImportState>(FitImportViewModel.new);
