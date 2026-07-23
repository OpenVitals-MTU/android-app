import '../../support/today_fixtures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/caffeine_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/features/caffeine/application/caffeine_view_model.dart';
import 'package:openvitals/features/caffeine/presentation/caffeine_drink_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/loading_state.dart';

/// The single-drink detail screen. It deliberately does NOT load anything of
/// its own — it reads the same entry list the caffeine screen loaded, so the
/// two can never disagree about which coffees exist. That makes its error case
/// structural: an id that is no longer in the list (deleted while open, or a
/// stale link) must say "no data", not crash or show someone else's drink.
class _FakeCaffeineRepository implements CaffeineRepository {
  _FakeCaffeineRepository({this.entries = const <CaffeineEntry>[]});

  final List<CaffeineEntry> entries;

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeineData(
    DatePeriod period, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(CaffeinePeriodData(entries: entries));

  @override
  Future<Result<CaffeinePeriodData>> loadCaffeinePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(CaffeinePeriodData(entries: entries));

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

CaffeineEntry _flatWhite() => CaffeineEntry(
      id: 'drink-1',
      // Clamped to today: the profile curve is computed against "now".
      startTime: earlierToday(const Duration(hours: 2)),
      endTime: earlierToday(const Duration(hours: 2)),
      caffeineMg: 128,
      name: 'Flat white',
      source: 'Test source',
      mealType: 0,
    );

Future<ProviderContainer> _loadedContainer(
  List<CaffeineEntry> entries,
) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  final container = ProviderContainer(overrides: [
    sharedPreferencesProvider.overrideWithValue(prefs),
    caffeineRepositoryProvider
        .overrideWithValue(_FakeCaffeineRepository(entries: entries)),
  ]);
  addTearDown(container.dispose);
  // The list screen's load, which is where this screen's data really
  // comes from.
  container.listen(caffeineProvider, (_, _) {});
  await container.read(caffeineProvider.notifier).load();
  return container;
}

Widget _screen(ProviderContainer container, String entryId) =>
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: CaffeineDrinkScreen(entryId: entryId),
      ),
    );

void main() {
  testWidgets('renders the drink it was opened for, by name and dose',
      (tester) async {
    final container = await _loadedContainer([_flatWhite()]);

    await tester.pumpWidget(_screen(container, 'drink-1'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Flat white'), findsWidgets);
    expect(find.textContaining('128'), findsWidgets,
        reason: 'the headline must show this drink\'s dose');
  });

  testWidgets('a drink deleted while its screen was open degrades to "no data"',
      (tester) async {
    final container = await _loadedContainer([_flatWhite()]);

    await tester.pumpWidget(_screen(container, 'gone-drink'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(ErrorMessage), findsOneWidget);
    expect(find.text('Flat white'), findsNothing,
        reason: 'a stale id must never show a different drink');
  });
}
