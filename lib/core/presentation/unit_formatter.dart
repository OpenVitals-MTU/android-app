// ignore_for_file: prefer_initializing_formals

import 'package:intl/intl.dart';

import '../../domain/preferences/unit_system.dart';
import 'display_value.dart';

/// Formats metric values into metric/imperial display strings, mirroring the
/// Kotlin `UnitFormatter`. Locale-aware number formatting uses `intl`.
class UnitFormatter {
  UnitFormatter({
    required UnitSystem Function() unitSystemProvider,
    String? Function()? localeProvider,
  })  : _unitSystemProvider = unitSystemProvider,
        _localeProvider = localeProvider;

  final UnitSystem Function() _unitSystemProvider;
  final String? Function()? _localeProvider;

  UnitSystem unitSystem() => _unitSystemProvider();

  String? _locale() => _localeProvider?.call();

  String count(num value) => _integerFormat().format(value);

  DisplayValue distance(double meters) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return _metricDistance(meters);
      case UnitSystem.imperial:
        return _imperialDistance(meters);
    }
  }

  DisplayValue elevation(double meters) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return _metricDistance(meters);
      case UnitSystem.imperial:
        return DisplayValue(count(_metersToFeet(meters).round()), 'ft');
    }
  }

  DisplayValue weight(double kg) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(kg, 1), 'kg');
      case UnitSystem.imperial:
        return DisplayValue(decimal(_kgToPounds(kg), 1), 'lb');
    }
  }

  DisplayValue height(double centimeters) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(centimeters, 0), 'cm');
      case UnitSystem.imperial:
        final totalInches = (centimeters / 2.54).round();
        final feet = totalInches ~/ 12;
        final inches = totalInches % 12;
        return DisplayValue("$feet' $inches\"", '');
    }
  }

  DisplayValue bodyMass(double kg, {int decimals = 1}) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(kg, decimals), 'kg');
      case UnitSystem.imperial:
        return DisplayValue(decimal(_kgToPounds(kg), decimals), 'lb');
    }
  }

  DisplayValue hydration(double liters) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(liters, 2), 'L');
      case UnitSystem.imperial:
        return DisplayValue(decimal(_litersToFluidOunces(liters), 0), 'fl oz');
    }
  }

  DisplayValue energy(double kcal) => DisplayValue(count(kcal.round()), 'kcal');

  DisplayValue temperature(double celsius) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(celsius, 1), 'deg C');
      case UnitSystem.imperial:
        return DisplayValue(decimal(_celsiusToFahrenheit(celsius), 1), 'deg F');
    }
  }

  DisplayValue temperatureDelta(double celsius) {
    final value = switch (unitSystem()) {
      UnitSystem.metric => celsius,
      UnitSystem.imperial => celsius * 9.0 / 5.0,
    };
    final prefix = value > 0.0 ? '+' : '';
    final unit = switch (unitSystem()) {
      UnitSystem.metric => 'deg C',
      UnitSystem.imperial => 'deg F',
    };
    return DisplayValue('$prefix${decimal(value, 1)}', unit);
  }

  DisplayValue bloodGlucose(double millimolesPerLiter) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(millimolesPerLiter, 1), 'mmol/L');
      case UnitSystem.imperial:
        return DisplayValue(decimal(millimolesPerLiter * 18.0, 0), 'mg/dL');
    }
  }

  DisplayValue percent(double value, {int decimals = 1}) =>
      DisplayValue(decimal(value, decimals), '%');

  DisplayValue heartRate(int bpm) => DisplayValue('$bpm', 'bpm');

  DisplayValue hrv(double milliseconds) =>
      DisplayValue(decimal(milliseconds, 1), 'ms');

  DisplayValue bloodPressure(int systolic, int diastolic) =>
      DisplayValue('$systolic/$diastolic', 'mmHg');

  DisplayValue respiratoryRate(double value) =>
      DisplayValue(decimal(value, 1), 'br/min');

  DisplayValue vo2Max(double value) =>
      DisplayValue(decimal(value, 1), 'mL/kg/min');

  String duration(int durationMs) {
    final hours = durationMs ~/ 3600000;
    final minutes = (durationMs % 3600000) ~/ 60000;
    return '${hours}h ${minutes.toString().padLeft(2, '0')}m';
  }

  DisplayValue averageSpeed(double distanceMeters, int durationMs) {
    final hours = (durationMs < 0 ? 0 : durationMs).toDouble() / 3600000.0;
    final metersPerHour =
        (distanceMeters > 0.0 && hours > 0.0) ? distanceMeters / hours : 0.0;
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(metersPerHour / 1000.0, 1), 'km/h');
      case UnitSystem.imperial:
        return DisplayValue(decimal(metersPerHour / 1609.344, 1), 'mph');
    }
  }

  DisplayValue speed(double metersPerSecond) {
    switch (unitSystem()) {
      case UnitSystem.metric:
        return DisplayValue(decimal(metersPerSecond * 3.6, 1), 'km/h');
      case UnitSystem.imperial:
        return DisplayValue(decimal(metersPerSecond * 2.2369362921, 1), 'mph');
    }
  }

  DisplayValue power(double watts) => DisplayValue(decimal(watts, 0), 'W');

  /// Pedalling cadence: revolutions per minute.
  DisplayValue cadence(double value) => DisplayValue(decimal(value, 1), 'rpm');

  /// Step cadence: steps per minute. Deliberately NOT [cadence] -- the Kotlin app
  /// formatted both through one `rpm` formatter, which labelled a runner's step
  /// cadence with a unit of revolutions. Same number, wrong unit.
  DisplayValue stepsCadence(double value) =>
      DisplayValue(decimal(value, 1), 'spm');

  DisplayValue? averagePace(double distanceMeters, int durationMs) {
    if (distanceMeters <= 0.0 || durationMs <= 0) return null;
    final distanceUnitMeters = switch (unitSystem()) {
      UnitSystem.metric => 1000.0,
      UnitSystem.imperial => 1609.344,
    };
    final distanceUnits = distanceMeters / distanceUnitMeters;
    if (distanceUnits <= 0.0 || !distanceUnits.isFinite) return null;

    final secondsPerUnit = ((durationMs / 1000.0) / distanceUnits).round();
    final clamped = secondsPerUnit < 0 ? 0 : secondsPerUnit;
    final minutes = clamped ~/ 60;
    final seconds = clamped % 60;
    final unit = switch (unitSystem()) {
      UnitSystem.metric => 'min/km',
      UnitSystem.imperial => 'min/mi',
    };
    return DisplayValue('$minutes:${seconds.toString().padLeft(2, '0')}', unit);
  }

  DisplayValue minutes(int minutes) => DisplayValue(count(minutes), 'min');

  String decimal(double value, int decimals) {
    final format = NumberFormat.decimalPattern(_locale())
      ..minimumFractionDigits = decimals
      ..maximumFractionDigits = decimals;
    return format.format(value);
  }

  DisplayValue _metricDistance(double meters) {
    if (meters >= 1000.0) {
      return DisplayValue(decimal(meters / 1000.0, 1), 'km');
    }
    return DisplayValue(count(meters.round()), 'm');
  }

  DisplayValue _imperialDistance(double meters) {
    final miles = _metersToMiles(meters);
    if (miles >= 0.1) {
      return DisplayValue(decimal(miles, 1), 'mi');
    }
    return DisplayValue(count(_metersToFeet(meters).round()), 'ft');
  }

  NumberFormat _integerFormat() =>
      NumberFormat.decimalPattern(_locale())..maximumFractionDigits = 0;

  double _kgToPounds(double kg) => kg * 2.2046226218;

  double _metersToFeet(double meters) => meters * 3.280839895;

  double _metersToMiles(double meters) => meters / 1609.344;

  double _litersToFluidOunces(double liters) => liters * 33.8140227018;

  double _celsiusToFahrenheit(double celsius) => celsius * 9.0 / 5.0 + 32.0;
}
