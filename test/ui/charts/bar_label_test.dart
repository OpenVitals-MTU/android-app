import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/ui/charts/bar_chart.dart';
import 'package:openvitals/ui/theme/app_theme.dart';

/// The value label on a bar.
///
/// It used to be measured against the BAR and dropped whole when it did not
/// fit. A bar is its slot minus the gap either side of it, so the label had
/// about 35 logical pixels to live in — and "21,104" needs about 40 where
/// "9,785" needs 34. One character was the difference between a number and
/// nothing at all, which is why every day over 10,000 steps came out blank, and
/// why it was always the tallest bar on the chart that said nothing.
///
/// NOTE ON THE NUMBERS BELOW. Widget tests use a placeholder font whose every
/// glyph is a square of the font size, so text here is far wider than the same
/// text on a device (six digits measure 69px at 11pt in a test, ~40px on a
/// phone). The `maxWidth` values are therefore chosen to exercise the *rule* —
/// fits / shrinks / dropped — and are not device measurements.
void main() {
  late TextStyle style;

  Future<void> withAppTheme(WidgetTester tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: AppTheme.themeFrom(AppTheme.darkScheme),
      home: Builder(builder: (context) {
        style = Theme.of(context).textTheme.labelSmall!;
        return const SizedBox();
      }),
    ));
  }

  BarLabelLayout? layout(String text, double maxWidth) => layoutBarLabel(
        text: text,
        style: style,
        maxWidth: maxWidth,
        textDirection: TextDirection.ltr,
      );

  testWidgets('a label that fits is drawn at full size', (tester) async {
    await withAppTheme(tester);

    final result = layout('21,104 steps', 80)!;

    expect(result.lines.first.text!.style!.fontSize, style.fontSize);
  });

  testWidgets('a label that does not fit SHRINKS — it does not vanish',
      (tester) async {
    await withAppTheme(tester);

    // Too narrow for the number at 11pt, wide enough at a smaller size. This is
    // the case that used to return null and leave the bar blank.
    final result = layout('21,104 steps', 55);

    expect(result, isNotNull);
    expect(result!.width, lessThanOrEqualTo(55));
    expect(
      result.lines.first.text!.style!.fontSize,
      lessThan(style.fontSize!),
      reason: 'it should have stepped down to fit, not given up',
    );
  });

  testWidgets('the longer the number, the smaller it is drawn — but it is drawn',
      (tester) async {
    await withAppTheme(tester);

    final shorter = layout('9,785 steps', 60)!;
    final longer = layout('218,104 steps', 60)!;

    expect(longer.width, lessThanOrEqualTo(60));
    expect(
      longer.lines.first.text!.style!.fontSize,
      lessThan(shorter.lines.first.text!.style!.fontSize!),
    );
  });

  testWidgets('the unit goes on its own line, so the number keeps the room',
      (tester) async {
    await withAppTheme(tester);

    expect(splitBarLabel('21,104 steps'), ['21,104', 'steps']);
    expect(layout('21,104 steps', 80)!.lines, hasLength(2));
    // Nothing to split: one line.
    expect(splitBarLabel('21,104'), isNull);
  });

  testWidgets('a label nobody could read is still dropped', (tester) async {
    await withAppTheme(tester);

    // A month chart gives 31 bars a few pixels each. Below the floor a number
    // is not worth drawing, and drawing it anyway would read worse than the gap.
    expect(layout('21,104 steps', 10), isNull);
    expect(layout('    ', 100), isNull);
    expect(layout('21,104 steps', 0), isNull);
  });
}
