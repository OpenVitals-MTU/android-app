import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';

void main() {
  test('withDayOfMonth clamps to the month length (no phantom Feb 31)', () {
    final feb2026 = LocalDate(2026, 2, 10); // 2026 is not a leap year
    expect(feb2026.withDayOfMonth(31), LocalDate(2026, 2, 28));
    expect(LocalDate(2024, 2, 10).withDayOfMonth(31), LocalDate(2024, 2, 29));
    expect(feb2026.withDayOfMonth(0), LocalDate(2026, 2, 1));
    expect(feb2026.withDayOfMonth(15), LocalDate(2026, 2, 15));
  });

  test('the constructor rejects a grossly invalid month/day (debug assert)', () {
    expect(() => LocalDate(2026, 13, 1), throwsA(isA<AssertionError>()));
    expect(() => LocalDate(2026, 1, 32), throwsA(isA<AssertionError>()));
    expect(() => LocalDate(2026, 1, 0), throwsA(isA<AssertionError>()));
  });
}
