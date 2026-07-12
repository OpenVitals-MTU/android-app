import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/prefs/preferences_repository.dart';
import '../../../di/providers.dart';
import '../../../state/app_providers.dart';

/// Holds the activity split distance IN METERS and writes it back through
/// [PreferencesRepository]. Storage is metric; the imperial presets exist only
/// at the display boundary, so nothing is converted here.
///
/// The read side rides the repository's listenable through
/// [activitySplitDistanceMetersProvider], so a write from anywhere (settings,
/// onboarding) re-cuts the splits on the next detail load.
class ActivitySplitDistanceViewModel extends Notifier<double> {
  @override
  double build() => ref.watch(activitySplitDistanceMetersProvider);

  void select(double meters) {
    ref.read(preferencesRepositoryProvider).activitySplitDistanceMeters =
        meters;
  }
}

/// The state provider for the activity split-distance settings card.
final activitySplitDistanceCardProvider =
    NotifierProvider<ActivitySplitDistanceViewModel, double>(
  ActivitySplitDistanceViewModel.new,
);
