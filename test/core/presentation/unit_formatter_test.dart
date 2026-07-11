import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';

UnitFormatter formatter(UnitSystem unitSystem) => UnitFormatter(
      unitSystemProvider: () => unitSystem,
      localeProvider: () => 'en_US',
    );

void main() {
  test('count uses locale grouping', () {
    expect(formatter(UnitSystem.metric).count(12345), '12,345');
  });

  test('metric distance uses meters below one kilometer', () {
    expect(formatter(UnitSystem.metric).distance(999.0).text, '999 m');
  });

  test('metric distance uses kilometers from one kilometer', () {
    expect(formatter(UnitSystem.metric).distance(1500.0).text, '1.5 km');
  });

  test('imperial distance uses miles above threshold', () {
    expect(formatter(UnitSystem.imperial).distance(1609.344).text, '1.0 mi');
  });

  test('imperial distance uses feet below threshold', () {
    expect(formatter(UnitSystem.imperial).distance(50.0).text, '164 ft');
  });

  test('imperial elevation uses feet', () {
    expect(formatter(UnitSystem.imperial).elevation(10.0).text, '33 ft');
  });

  test('imperial weight uses pounds', () {
    expect(formatter(UnitSystem.imperial).weight(70.0).text, '154.3 lb');
  });

  test('metric height uses centimeters', () {
    expect(formatter(UnitSystem.metric).height(180.0).text, '180 cm');
  });

  test('imperial height uses feet and inches', () {
    expect(formatter(UnitSystem.imperial).height(180.0).text, '5\' 11"');
  });

  test('imperial hydration uses fluid ounces', () {
    expect(formatter(UnitSystem.imperial).hydration(2.0).text, '68 fl oz');
  });

  test('metric hydration uses liters', () {
    expect(formatter(UnitSystem.metric).hydration(2.0).text, '2.00 L');
  });

  test('metric hydration keeps two decimals below one liter', () {
    expect(formatter(UnitSystem.metric).hydration(0.15).text, '0.15 L');
  });

  test('imperial temperature uses fahrenheit', () {
    expect(formatter(UnitSystem.imperial).temperature(37.0).text, '98.6 deg F');
  });

  test('metric temperature delta keeps celsius delta', () {
    expect(formatter(UnitSystem.metric).temperatureDelta(1.5).text,
        '+1.5 deg C');
    expect(formatter(UnitSystem.metric).temperatureDelta(-0.4).text,
        '-0.4 deg C');
  });

  test('imperial temperature delta converts to fahrenheit delta', () {
    expect(formatter(UnitSystem.imperial).temperatureDelta(1.5).text,
        '+2.7 deg F');
  });

  test('metric blood glucose uses mmol per liter', () {
    expect(formatter(UnitSystem.metric).bloodGlucose(5.6).text, '5.6 mmol/L');
  });

  test('imperial blood glucose uses milligrams per deciliter', () {
    expect(formatter(UnitSystem.imperial).bloodGlucose(5.6).text, '101 mg/dL');
  });

  test('blood pressure is not converted', () {
    expect(formatter(UnitSystem.metric).bloodPressure(120, 80).text,
        '120/80 mmHg');
  });

  test('duration formats hours and padded minutes', () {
    expect(formatter(UnitSystem.metric).duration(3900000), '1h 05m');
  });

  test('metric average speed uses kilometers per hour', () {
    expect(formatter(UnitSystem.metric).averageSpeed(5000.0, 1800000).text,
        '10.0 km/h');
  });

  test('imperial average speed uses miles per hour', () {
    expect(formatter(UnitSystem.imperial).averageSpeed(1609.344, 600000).text,
        '6.0 mph');
  });

  test('metric recorded speed uses kilometers per hour', () {
    expect(formatter(UnitSystem.metric).speed(5.0).text, '18.0 km/h');
  });

  test('imperial recorded speed uses miles per hour', () {
    expect(formatter(UnitSystem.imperial).speed(5.0).text, '11.2 mph');
  });

  test('power uses watts', () {
    expect(formatter(UnitSystem.metric).power(250.4).text, '250 W');
  });

  test('cadence uses rpm', () {
    expect(formatter(UnitSystem.metric).cadence(82.5).text, '82.5 rpm');
  });

  test('metric average pace uses minutes per kilometer', () {
    expect(formatter(UnitSystem.metric).averagePace(5000.0, 1800000)?.text,
        '6:00 min/km');
  });

  test('imperial average pace uses minutes per mile', () {
    expect(formatter(UnitSystem.imperial).averagePace(1609.344, 600000)?.text,
        '10:00 min/mi');
  });

  test('average pace needs distance and duration', () {
    expect(formatter(UnitSystem.metric).averagePace(0.0, 1800000), isNull);
    expect(formatter(UnitSystem.metric).averagePace(5000.0, 0), isNull);
  });
}
