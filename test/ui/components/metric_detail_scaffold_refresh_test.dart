import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_range_preference_key.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/refresh/data_domain.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/navigation/app_router.dart' show routeObserver;
import 'package:openvitals/state/refresh_coordinator.dart';
import 'package:openvitals/ui/components/metric_detail_scaffold.dart';

/// A stand-in feature screen: one [MetricDetailScaffold] that counts its
/// refreshes. The scaffold is what 13 real screens share, so proving the wiring
/// here proves it for all of them.
class _CountingScreen extends StatefulWidget {
  const _CountingScreen({
    required this.domains,
    required this.label,
    required this.counts,
  });

  final Set<DataDomain> domains;
  final String label;
  final Map<String, int> counts;

  @override
  State<_CountingScreen> createState() => _CountingScreenState();
}

class _CountingScreenState extends State<_CountingScreen> {
  @override
  Widget build(BuildContext context) {
    return MetricDetailScaffold(
      rangePreferenceKey: PeriodRangePreferenceKey.sleep,
      refreshDomains: widget.domains,
      onRefresh: () async {
        widget.counts[widget.label] = (widget.counts[widget.label] ?? 0) + 1;
      },
      content: (period) => [Text('content-${widget.label}')],
    );
  }
}

void main() {
  late ProviderContainer container;
  late Map<String, int> counts;

  Future<void> pump(
    WidgetTester tester, {
    required Set<DataDomain> domains,
    String label = 'top',
  }) async {
    counts = <String, int>{};
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
    ]);
    addTearDown(container.dispose);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        navigatorObservers: [routeObserver],
        home: Scaffold(
          body: _CountingScreen(
            domains: domains,
            label: label,
            counts: counts,
          ),
        ),
      ),
    ));
    await tester.pumpAndSettle();
  }

  void changed(Set<DataDomain> domains) =>
      container.read(refreshCoordinatorProvider.notifier).changed(domains);

  void appOpened() =>
      container.read(refreshCoordinatorProvider.notifier).appOpened();

  testWidgets('a signal for the screen own domain refreshes it once',
      (tester) async {
    await pump(tester, domains: {DataDomain.sleep});

    changed({DataDomain.sleep});
    await tester.pump(kDataChangeDebounce);
    await tester.pumpAndSettle();

    expect(counts['top'], 1);
  });

  testWidgets('a signal for an unrelated domain does not refresh the screen',
      (tester) async {
    await pump(tester, domains: {DataDomain.sleep});

    changed({DataDomain.cycle});
    await tester.pump(kDataChangeDebounce);
    await tester.pumpAndSettle();

    expect(counts['top'], isNull);
  });

  testWidgets('a derived domain refreshes the screen that reads it',
      (tester) async {
    // A drink is stored as hydration; the nutrition screen has to hear about it.
    await pump(tester, domains: {DataDomain.nutrition});

    changed({DataDomain.hydration});
    await tester.pump(kDataChangeDebounce);
    await tester.pumpAndSettle();

    expect(counts['top'], 1);
  });

  testWidgets('an app-open signal refreshes the screen', (tester) async {
    await pump(tester, domains: {DataDomain.sleep});

    appOpened();
    await tester.pumpAndSettle();

    expect(counts['top'], 1);
  });

  testWidgets('a screen with no declared domains never refreshes',
      (tester) async {
    // The default, so the call sites that have not opted in behave exactly as
    // they did before.
    await pump(tester, domains: const <DataDomain>{});

    appOpened();
    await tester.pumpAndSettle();

    expect(counts['top'], isNull);
  });

  testWidgets('a signal arriving while a detail route is pushed on top defers '
      'the refresh until it is popped', (tester) async {
    await pump(tester, domains: {DataDomain.sleep});
    final navigator = tester.state<NavigatorState>(find.byType(Navigator));

    navigator.push(MaterialPageRoute<void>(
      builder: (_) => const Scaffold(body: Text('pushed')),
    ));
    await tester.pumpAndSettle();

    appOpened();
    await tester.pumpAndSettle();
    expect(counts['top'], isNull,
        reason: 'the user is looking at the pushed screen, and Health Connect '
            'serializes concurrent reads');

    navigator.pop();
    await tester.pumpAndSettle();

    expect(counts['top'], 1, reason: 'revealed again, so now it re-reads');
  });

  testWidgets('several signals while covered collapse into one refresh on pop',
      (tester) async {
    await pump(tester, domains: {DataDomain.sleep});
    final navigator = tester.state<NavigatorState>(find.byType(Navigator));
    navigator.push(MaterialPageRoute<void>(
      builder: (_) => const Scaffold(body: Text('pushed')),
    ));
    await tester.pumpAndSettle();

    appOpened();
    appOpened();
    appOpened();
    await tester.pumpAndSettle();
    navigator.pop();
    await tester.pumpAndSettle();

    expect(counts['top'], 1);
  });

  testWidgets('a plain back navigation with nothing changed does not refresh',
      (tester) async {
    await pump(tester, domains: {DataDomain.sleep});
    final navigator = tester.state<NavigatorState>(find.byType(Navigator));
    final before = counts['top'];

    navigator.push(MaterialPageRoute<void>(
      builder: (_) => const Scaffold(body: Text('pushed')),
    ));
    await tester.pumpAndSettle();
    navigator.pop();
    await tester.pumpAndSettle();

    // The dashboard used to reload ~35 Health Connect reads on every pop,
    // whether or not anything had moved.
    expect(counts['top'], before);
  });

  testWidgets('an app-open signal refreshes only the visible screen, not every '
      'mounted one', (tester) async {
    // The read-storm guard. Every feature provider in this app is non-auto-
    // dispose, so 25-40 view-models can be alive at once; fanning one app-open
    // signal into all of them would fire 25-40 concurrent Health Connect read
    // waves, which Health Connect serializes.
    counts = <String, int>{};
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    container = ProviderContainer(overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
    ]);
    addTearDown(container.dispose);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        navigatorObservers: [routeObserver],
        home: Scaffold(
          body: _CountingScreen(
            domains: const {DataDomain.sleep},
            label: 'bottom',
            counts: counts,
          ),
        ),
      ),
    ));
    await tester.pumpAndSettle();
    final navigator = tester.state<NavigatorState>(find.byType(Navigator));
    navigator.push(MaterialPageRoute<void>(
      builder: (_) => Scaffold(
        body: _CountingScreen(
          domains: const {DataDomain.sleep},
          label: 'middle',
          counts: counts,
        ),
      ),
    ));
    await tester.pumpAndSettle();
    navigator.push(MaterialPageRoute<void>(
      builder: (_) => Scaffold(
        body: _CountingScreen(
          domains: const {DataDomain.sleep},
          label: 'top',
          counts: counts,
        ),
      ),
    ));
    await tester.pumpAndSettle();

    appOpened();
    await tester.pumpAndSettle();

    expect(counts['top'], 1);
    expect(counts['middle'], isNull);
    expect(counts['bottom'], isNull);
  });
}
