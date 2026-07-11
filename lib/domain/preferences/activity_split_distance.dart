import '../insights/activity_splits.dart';
import 'unit_system.dart';

/// The split distance preference: how far apart the activity detail screen cuts
/// derived splits ("every 1 km").
///
/// STORED IN METERS, always — storage is metric in this codebase and imperial
/// exists only at the display boundary. The imperial presets below are exact
/// mile fractions converted to meters on the way in, so a user who picks
/// "1 mi" and later switches to metric sees 1.609 km worth of splits, not a
/// silently rounded 1600 m.
class ActivitySplitDistance {
  const ActivitySplitDistance._();

  /// One kilometer, the default a runner expects.
  static const double defaultMeters = kDefaultSplitDistanceMeters;

  static const double minMeters = 100.0;
  static const double maxMeters = 50000.0;

  static const double _metersPerMile = 1609.344;

  /// Metric presets: 0.5 / 1 / 2 / 5 km.
  static const List<double> metricPresetMeters = <double>[
    500.0,
    1000.0,
    2000.0,
    5000.0,
  ];

  /// Imperial presets: 0.25 / 0.5 / 1 / 5 mi, in meters.
  static const List<double> imperialPresetMeters = <double>[
    0.25 * _metersPerMile,
    0.5 * _metersPerMile,
    _metersPerMile,
    5 * _metersPerMile,
  ];

  static List<double> presetsFor(UnitSystem unitSystem) =>
      switch (unitSystem) {
        UnitSystem.metric => metricPresetMeters,
        UnitSystem.imperial => imperialPresetMeters,
      };

  static double normalize(double meters) {
    if (!meters.isFinite || meters <= 0) return defaultMeters;
    return meters.clamp(minMeters, maxMeters).toDouble();
  }

  /// The preset closest to [meters], so the settings chips still show a
  /// selection after the user switches unit systems (a stored 1000 m has no
  /// exact imperial preset; the honest thing is to highlight the nearest one
  /// rather than show nothing selected).
  static double nearestPreset(double meters, UnitSystem unitSystem) {
    final presets = presetsFor(unitSystem);
    var best = presets.first;
    var bestDelta = (presets.first - meters).abs();
    for (final preset in presets.skip(1)) {
      final delta = (preset - meters).abs();
      if (delta < bestDelta) {
        best = preset;
        bestDelta = delta;
      }
    }
    return best;
  }
}
