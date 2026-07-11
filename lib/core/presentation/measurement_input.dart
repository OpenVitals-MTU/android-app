import '../../domain/preferences/unit_system.dart';
import 'unit_formatter.dart';

/// The single source of truth for how a *typed* measurement is labelled and
/// converted to its canonical (metric) unit.
///
/// [UnitFormatter] owns the read side — how a stored value is displayed. This
/// owns the write side: what unit the text field is in, and how to turn that
/// text back into the metric value Health Connect stores. Before this existed,
/// each entry screen carried its own `imperial ? 'oz' : 'g'` table and its own
/// conversion factor, and the hydration dialogs skipped conversion entirely —
/// labelling the field "mL" while the rows next to it read "fl oz".
///
/// Every quantity is stored metric: millilitres, grams, kilograms, centimetres,
/// degrees Celsius.

/// Exact conversion factors, matching the Kotlin constants.
const double kGramsPerOunce = 28.349523125;
const double kPoundsPerKilogram = 2.2046226218;
const double kCentimetersPerInch = 2.54;
const double kMillilitersPerFluidOunce = 29.5735295625;
const double kFahrenheitFreezingPoint = 32.0;
const double kFahrenheitPerCelsius = 1.8;

/// Parses a decimal the user typed, accepting a comma as the decimal separator.
double? parseDecimalInput(String input) =>
    double.tryParse(input.trim().replaceAll(',', '.'));

extension MeasurementInput on UnitFormatter {
  bool get isImperial => unitSystem() == UnitSystem.imperial;

  // ── Volume (hydration) ────────────────────────────────────────────────────

  /// Kotlin `hydrationInputUnitLabel`.
  String get volumeInputUnit => isImperial ? 'fl oz' : 'ml';

  /// Kotlin `hydrationInputMilliliters`.
  double? volumeInputToMilliliters(String input) {
    final value = parseDecimalInput(input);
    if (value == null) return null;
    return isImperial ? value * kMillilitersPerFluidOunce : value;
  }

  /// Kotlin `hydrationInputAmountText`: seeds a field from a stored volume.
  String millilitersToVolumeInput(double? milliliters) {
    if (milliliters == null) return '';
    return isImperial
        ? decimal(milliliters / kMillilitersPerFluidOunce, 1)
        : count(milliliters.round());
  }

  /// A volume bound rendered in the input's own unit, for the invalid-range
  /// message — "1 ml" / "0.03 fl oz".
  String millilitersBoundLabel(double milliliters) {
    final value = isImperial
        ? decimal(milliliters / kMillilitersPerFluidOunce, 2)
        : count(milliliters.round());
    return '$value $volumeInputUnit';
  }

  // ── Mass (carbs) ──────────────────────────────────────────────────────────

  String get carbsInputUnit => isImperial ? 'oz' : 'g';

  /// Kotlin `canonicalCarbsGrams`.
  double? carbsInputToGrams(String input) {
    final value = parseDecimalInput(input);
    if (value == null) return null;
    return isImperial ? value * kGramsPerOunce : value;
  }

  // ── Body ──────────────────────────────────────────────────────────────────

  String get weightInputUnit => isImperial ? 'lb' : 'kg';
  String get heightInputUnit => isImperial ? 'in' : 'cm';

  double? weightInputToKilograms(String input) {
    final value = parseDecimalInput(input);
    if (value == null) return null;
    return isImperial ? value / kPoundsPerKilogram : value;
  }

  double? heightInputToCentimeters(String input) {
    final value = parseDecimalInput(input);
    if (value == null) return null;
    return isImperial ? value * kCentimetersPerInch : value;
  }

  // ── Vitals ────────────────────────────────────────────────────────────────

  /// Kotlin spells these out rather than using the degree sign.
  String get temperatureInputUnit => isImperial ? 'deg F' : 'deg C';

  double? temperatureInputToCelsius(String input) {
    final value = parseDecimalInput(input);
    if (value == null) return null;
    return isImperial
        ? (value - kFahrenheitFreezingPoint) / kFahrenheitPerCelsius
        : value;
  }
}
