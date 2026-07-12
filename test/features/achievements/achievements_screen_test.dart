import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/achievements/presentation/achievements_screen.dart';

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository({this.days = const <DailySteps>[]});

  final List<DailySteps> days;

  @override
  Future<List<DailySteps>> loadDailySteps(LocalDate start, LocalDate end) async =>
      days;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<Widget> _bootstrap({required _FakeActivityRepository repository}) async {
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      activityRepositoryProvider.overrideWithValue(repository),
    ],
    child: const MaterialApp(home: AchievementsScreen()),
  );
}

void main() {
  testWidgets('renders summary + earned badge once loaded', (tester) async {
    final repo = _FakeActivityRepository(days: [
      DailySteps(
        date: LocalDate(2024, 1, 1),
        steps: 12000,
        distanceMeters: 8000,
        floorsClimbed: 12,
      ),
    ]);
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.text('Legacy achievements'), findsOneWidget);
    // Boat Shoes (5k daily steps) is unlocked by the 12k-step day.
    expect(find.text('Boat Shoes'), findsOneWidget);
    expect(find.text('Sneakers'), findsOneWidget);
  });

  testWidgets('shows the no-activity message with empty history',
      (tester) async {
    await tester.pumpWidget(
      await _bootstrap(repository: _FakeActivityRepository()),
    );
    await tester.pumpAndSettle();

    expect(find.text('No activity history yet'), findsOneWidget);
  });
}
