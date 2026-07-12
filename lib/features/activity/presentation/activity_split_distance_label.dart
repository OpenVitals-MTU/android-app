import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/preferences/unit_system.dart';
import '../../../l10n/app_localizations.dart';

/// "1 km" / "0.5 km" / "0.25 mi" — the split distance as a chip label or a card
/// header, in the user's unit system.
///
/// NOT [UnitFormatter.distance]: that always prints one decimal ("1.0 km"),
/// which is right for a measured distance and wrong for a chosen setting. The
/// split distance is a round number the user picked, so it is printed as one.
///
/// Shared by the settings chips and the splits-card header so the two can never
/// disagree about what "every 1 km" means.
String splitDistanceLabel(
  AppLocalizations l10n,
  UnitFormatter formatter,
  double meters,
) {
  switch (formatter.unitSystem()) {
    case UnitSystem.metric:
      final kilometers = meters / 1000.0;
      return l10n.activitySplitDistanceKilometers(
        formatter.decimal(kilometers, _decimalsFor(kilometers)),
      );
    case UnitSystem.imperial:
      final miles = meters / 1609.344;
      return l10n.activitySplitDistanceMiles(
        formatter.decimal(miles, _decimalsFor(miles)),
      );
  }
}

/// As few decimals as the value can be written in, up to two: 1 -> "1",
/// 0.5 -> "0.5", 0.25 -> "0.25".
int _decimalsFor(double value) {
  if ((value - value.roundToDouble()).abs() < 1e-6) return 0;
  if (((value * 10) - (value * 10).roundToDouble()).abs() < 1e-6) return 1;
  return 2;
}
