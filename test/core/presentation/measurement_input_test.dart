import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/measurement_input.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';

UnitFormatter _formatter(UnitSystem system) =>
    UnitFormatter(unitSystemProvider: () => system);

final _metric = _formatter(UnitSystem.metric);
final _imperial = _formatter(UnitSystem.imperial);

void main() {
  group('parseDecimalInput', () {
    test('accepts a comma as the decimal separator, and trims', () {
      expect(parseDecimalInput(' 1,5 '), 1.5);
      expect(parseDecimalInput('1.5'), 1.5);
      expect(parseDecimalInput('abc'), isNull);
      expect(parseDecimalInput(''), isNull);
    });
  });

  group('volume', () {
    test('labels the field in the user’s unit', () {
      expect(_metric.volumeInputUnit, 'ml');
      expect(_imperial.volumeInputUnit, 'fl oz');
    });

    test('typed input always canonicalizes to millilitres', () {
      // The stored unit never changes; only the field's unit does.
      expect(_metric.volumeInputToMilliliters('330'), 330);
      expect(
        _imperial.volumeInputToMilliliters('12'),
        closeTo(12 * kMillilitersPerFluidOunce, 1e-9),
      );
    });

    test('a stored volume seeds the field in the user’s unit', () {
      expect(_metric.millilitersToVolumeInput(330), '330');
      // 330 ml ≈ 11.2 fl oz.
      expect(_imperial.millilitersToVolumeInput(330), '11.2');
      expect(_metric.millilitersToVolumeInput(null), '');
    });

    test('round-trips a typed imperial amount back to the same text', () {
      final ml = _imperial.volumeInputToMilliliters('12.0')!;
      expect(_imperial.millilitersToVolumeInput(ml), '12.0');
    });

    test('bounds are rendered in the field’s unit', () {
      expect(_metric.millilitersBoundLabel(1), '1 ml');
      expect(_imperial.millilitersBoundLabel(1), '0.03 fl oz');
    });
  });

  group('carbs', () {
    test('labels grams or ounces', () {
      expect(_metric.carbsInputUnit, 'g');
      expect(_imperial.carbsInputUnit, 'oz');
    });

    test('ounces convert to grams', () {
      expect(_metric.carbsInputToGrams('30'), 30);
      expect(
        _imperial.carbsInputToGrams('1'),
        closeTo(kGramsPerOunce, 1e-9),
      );
      expect(_metric.carbsInputToGrams('x'), isNull);
    });
  });

  group('body', () {
    test('weight: pounds convert to kilograms', () {
      expect(_metric.weightInputUnit, 'kg');
      expect(_imperial.weightInputUnit, 'lb');
      expect(_metric.weightInputToKilograms('70'), 70);
      expect(
        _imperial.weightInputToKilograms('154.32'),
        closeTo(70, 0.01),
      );
    });

    test('height: inches convert to centimetres', () {
      expect(_metric.heightInputUnit, 'cm');
      expect(_imperial.heightInputUnit, 'in');
      expect(_metric.heightInputToCentimeters('180'), 180);
      expect(_imperial.heightInputToCentimeters('70'), closeTo(177.8, 1e-9));
    });
  });

  group('temperature', () {
    test('labels degrees without the degree sign, as Kotlin does', () {
      expect(_metric.temperatureInputUnit, 'deg C');
      expect(_imperial.temperatureInputUnit, 'deg F');
    });

    test('Fahrenheit converts to Celsius', () {
      expect(_metric.temperatureInputToCelsius('37'), 37);
      expect(_imperial.temperatureInputToCelsius('98.6'), closeTo(37, 1e-9));
      expect(_imperial.temperatureInputToCelsius('32'), closeTo(0, 1e-9));
    });
  });

  test('a metric formatter never rewrites what the user typed', () {
    // Metric is the storage unit, so parsing is the identity beyond decimals.
    expect(_metric.volumeInputToMilliliters('250'), 250);
    expect(_metric.carbsInputToGrams('250'), 250);
    expect(_metric.weightInputToKilograms('80,5'), 80.5);
    expect(_metric.heightInputToCentimeters('175'), 175);
    expect(_metric.temperatureInputToCelsius('36,6'), 36.6);
  });
}
