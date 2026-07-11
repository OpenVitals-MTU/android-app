import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/stats/stats.dart';

/// The empty case is the whole reason this file exists.
///
/// The app carried eleven hand-rolled averages that disagreed about it — NaN, 0
/// and null were all live at once — so these tests pin the two contracts that
/// survived, and the one that did not.
void main() {
  group('average — null means "no samples"', () {
    test('averages the values', () {
      expect(average([1, 2, 3, 4]), 2.5);
    });

    test('returns null on empty, never zero', () {
      // The distinction that matters: a day with no resting heart rate is not a
      // day with a resting heart rate of 0 bpm. Four consumers on the heart
      // screens branch on this null.
      expect(average(const <num>[]), isNull);
    });

    test('is never NaN', () {
      // The contract the old `fold(0)/length` produced on empty. It compares
      // false against everything, so it slips past `<= 0` guards, and
      // `double.nan.round()` throws.
      expect(average(const <num>[]), isNot(isNaN));
    });

    test('averages ints and doubles alike', () {
      expect(average(<num>[1, 2.5]), 1.75);
    });

    test('a genuine zero average is zero, not null', () {
      expect(average([0, 0]), 0.0);
    });
  });

  group('averageOrZero — zero is a real value', () {
    test('returns 0 on empty', () {
      // For a chart bar, "nothing logged this month" IS a zero-height bar.
      expect(averageOrZero(const <num>[]), 0.0);
    });

    test('otherwise agrees with average', () {
      expect(averageOrZero([2, 4]), average([2, 4]));
    });
  });

  group('minOf / maxOf', () {
    test('find the extremes', () {
      expect(minOf([3, 1, 2]), 1.0);
      expect(maxOf([3, 1, 2]), 3.0);
    });

    test('return null on empty rather than throwing', () {
      // Their hand-rolled `reduce` ancestors threw here, while the `_avg` sitting
      // beside them guarded the same case — an asymmetry nobody chose.
      expect(minOf(const <num>[]), isNull);
      expect(maxOf(const <num>[]), isNull);
    });

    test('handle a single value', () {
      expect(minOf([7]), 7.0);
      expect(maxOf([7]), 7.0);
    });
  });
}
