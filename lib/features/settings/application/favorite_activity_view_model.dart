import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';

/// Holds the favorite-activity exercise type (`null` = "use latest") and writes
/// changes back through [PreferencesRepository]. Backs the favorite-activity
/// card only, so it stays out of the shared `SettingsState`.
///
/// The state is the stored value itself — a freezed wrapper around one nullable
/// int would carry no information the int does not.
class FavoriteActivityViewModel extends Notifier<int?> {
  PreferencesRepository get _prefs => ref.read(preferencesRepositoryProvider);

  @override
  int? build() => _prefs.favoriteActivityExerciseType;

  void select(int? exerciseType) {
    _prefs.favoriteActivityExerciseType = exerciseType;
    state = _prefs.favoriteActivityExerciseType;
  }
}

/// The state provider for the favorite-activity settings card.
final favoriteActivityExerciseTypeProvider =
    NotifierProvider<FavoriteActivityViewModel, int?>(
  FavoriteActivityViewModel.new,
);
