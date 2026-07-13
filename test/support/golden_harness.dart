import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
// Riverpod 3 keeps `Override` out of its main barrel.
import 'package:flutter_riverpod/misc.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';
import 'package:openvitals/ui/theme/app_theme.dart';

/// Photographs a chart, so a refactor can prove it changed nothing.
///
/// The chart library is 100% hand-rolled `CustomPainter`, and until now NOTHING
/// verified what any of them actually draws — the tests assert that a widget of
/// some type exists, and the axis/curve maths is unit-tested, but a painter that
/// rendered a blank rectangle would have passed the whole suite. That is a bad
/// position from which to consolidate nine copies of a bar and three copies of a
/// line, so: goldens first, refactor second.
///
/// Everything that could drift between two runs of the same test is nailed down
/// here — the surface size, the pixel ratio, the locale, the unit system, the
/// colour scheme (the SEED schemes, never the device's dynamic colour) and
/// animation. What is deliberately NOT nailed down is the font: see
/// `test/goldens/flutter_test_config.dart`.

/// A phone-ish canvas. Height is generous because the capture is scoped to the
/// widget, not to the window.
const Size kGoldenSurface = Size(420, 900);

/// The width a chart card is given, which is a phone's width less the screen's
/// horizontal padding. Charts lay their axis labels out against this, so it has
/// to be a real number rather than "as big as possible".
const double kGoldenChartWidth = 360;

final _goldenKey = GlobalKey();

/// Pumps [child] in the real app theme and compares it against
/// `test/goldens/charts/goldens/<name>.png`.
///
/// Pass [brightness] to shoot the same chart in dark mode — several chart
/// colours are alpha blends over the surface, so light and dark are genuinely
/// different pictures, not a palette swap.
Future<void> expectChartGolden(
  WidgetTester tester,
  Widget child, {
  required String name,
  Brightness brightness = Brightness.light,
  double width = kGoldenChartWidth,
  double textScale = 1.0,
  List<Override> overrides = const <Override>[],
}) async {
  // Restored in a tear-down, because these are set on the shared test view and
  // would otherwise leak into the next test in the file.
  tester.view.devicePixelRatio = 1.0;
  tester.view.physicalSize = kGoldenSurface;
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.view.resetPhysicalSize);

  final scheme = brightness == Brightness.dark
      ? AppTheme.darkScheme
      : AppTheme.lightScheme;

  await tester.pumpWidget(
    ProviderScope(
      // Storage is metric; the unit system is a preference that follows the host
      // locale by default, so a golden shot on a US machine would render "mi"
      // and one shot here would render "km". Pin it.
      overrides: [
        unitSystemProvider.overrideWithValue(UnitSystem.metric),
        ...overrides,
      ],
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        locale: const Locale('en'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        theme: AppTheme.themeFrom(scheme).copyWith(
          // The app resolves to Roboto on Android; say so, since the test
          // binding's default family is the box font.
          textTheme: AppTheme.themeFrom(scheme).textTheme.apply(
                fontFamily: 'Roboto',
              ),
        ),
        home: Builder(
          builder: (context) => MediaQuery(
            data: MediaQuery.of(context).copyWith(
              textScaler: TextScaler.linear(textScale),
              // Charts will animate in (Phase B). A golden must photograph the
              // settled frame, and this is the same switch the reveal honours
              // for reduce-motion — so pinning it here is not a test-only hack,
              // it is the accessibility contract.
              disableAnimations: true,
            ),
            child: Scaffold(
              backgroundColor: scheme.surface,
              body: Center(
                child: SingleChildScrollView(
                  child: RepaintBoundary(
                    key: _goldenKey,
                    child: SizedBox(width: width, child: child),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();

  await expectLater(
    find.byKey(_goldenKey),
    matchesGoldenFile('goldens/$name.png'),
  );
}

/// Shoots the same chart light and dark. Most charts want both; a few (the
/// heatmaps) are dominated by one colour ramp and one shot is enough.
Future<void> expectChartGoldenBothThemes(
  WidgetTester tester,
  Widget Function() build, {
  required String name,
  double width = kGoldenChartWidth,
  double textScale = 1.0,
  List<Override> overrides = const <Override>[],
}) async {
  await expectChartGolden(
    tester,
    build(),
    name: '${name}_light',
    width: width,
    textScale: textScale,
    overrides: overrides,
  );
  await expectChartGolden(
    tester,
    build(),
    name: '${name}_dark',
    brightness: Brightness.dark,
    width: width,
    textScale: textScale,
    overrides: overrides,
  );
}

/// The clock every chart golden is taken against.
///
/// A chart that knows what "today" is — [DayAxis] shades the part of the day
/// that has not happened yet — draws a different picture every time the suite
/// runs. So the axis is always built with this, and never with `DateTime.now()`.
final DateTime kGoldenNow = DateTime(2026, 6, 22, 14, 30);
final DateTime kGoldenDay = DateTime(2026, 6, 22);
