import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/activity/presentation/activity_detail_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// The route card's export actions (Kotlin `RouteCard` buttons): present when
/// the workout has a GPS route, absent — with the whole card — when it does
/// not. The writers themselves are covered by the export unit tests, and the
/// share path by `export/activity_route_sharing_test.dart`.
///
/// Three actions, three different intents, none a substitute for another:
/// open-in-map is ACTION_VIEW and offers map apps, save raises the SAF picker,
/// share is ACTION_SEND and offers messengers and mail.

final DateTime _start = DateTime.utc(2026, 7, 10, 8);

ExerciseData _workout({required bool withRoute}) => ExerciseData(
      id: 'w1',
      title: 'Morning run',
      exerciseType: 56,
      startTime: _start,
      endTime: _start.add(const Duration(minutes: 30)),
      durationMs: 30 * 60 * 1000,
      source: 'test',
      route: withRoute
          ? ExerciseRouteData(
              status: ExerciseRouteStatus.data,
              points: [
                for (var index = 0; index < 3; index++)
                  ExerciseRoutePoint(
                    time: _start.add(Duration(minutes: index)),
                    latitude: 59.43 + index * 0.001,
                    longitude: 24.75 + index * 0.001,
                    altitudeMeters: null,
                    horizontalAccuracyMeters: null,
                    verticalAccuracyMeters: null,
                  ),
              ],
            )
          : const ExerciseRouteData(),
    );

class _FakeActivityRepository implements ActivityRepository {
  _FakeActivityRepository(this.workout);

  final ExerciseData workout;

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) async => Ok(workout);

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      Future.value(const Err<Never>(UnexpectedFailure('not stubbed')));
}

class _FakeHeartRepository implements HeartRepository {
  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async =>
      const Ok(<HeartRateSample>[]);

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      Future.value(const Err<Never>(UnexpectedFailure('not stubbed')));
}

Future<void> _pump(WidgetTester tester, {required bool withRoute}) async {
  SharedPreferences.setMockInitialValues({
    'unit_system': UnitSystem.metric.name,
  });
  final prefs = await SharedPreferences.getInstance();

  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        sharedPreferencesProvider.overrideWithValue(prefs),
        activityRepositoryProvider
            .overrideWithValue(_FakeActivityRepository(_workout(withRoute: withRoute))),
        heartRepositoryProvider.overrideWithValue(_FakeHeartRepository()),
      ],
      child: const MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: ActivityDetailScreen(activityId: 'w1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('route card offers open-in-map, GPX/KMZ save and GPX/KMZ share '
      'actions', (tester) async {
    await _pump(tester, withRoute: true);

    // The route card sits below the fold; the lazy list only builds it once
    // scrolled into view.
    await tester.scrollUntilVisible(find.text('Share KMZ'), 300);

    expect(find.text('Open route in map app'), findsOneWidget);
    expect(find.text('Save GPX'), findsOneWidget);
    expect(find.text('Save KMZ'), findsOneWidget);
    expect(find.text('Share GPX'), findsOneWidget);
    expect(find.text('Share KMZ'), findsOneWidget);
  });

  testWidgets('no route means no export actions', (tester) async {
    await _pump(tester, withRoute: false);

    // Scroll to the very end: with no route there is no card to build at any
    // offset.
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -5000));
    await tester.pumpAndSettle();

    expect(find.text('Open route in map app'), findsNothing);
    expect(find.text('Save GPX'), findsNothing);
    expect(find.text('Save KMZ'), findsNothing);
    expect(find.text('Share GPX'), findsNothing);
    expect(find.text('Share KMZ'), findsNothing);
  });
}
