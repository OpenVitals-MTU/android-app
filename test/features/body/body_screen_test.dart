import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/domain/query/body_period_data.dart';
import 'package:openvitals/features/body/presentation/body_screen.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/state/app_providers.dart';
import 'package:openvitals/ui/charts/period_chart.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/insight_cards.dart';
import 'package:openvitals/ui/components/metric_card.dart';
import 'package:openvitals/ui/components/swipe_to_delete_entry_row.dart';

class _FakeBodyRepository implements BodyRepository {
  _FakeBodyRepository({this.data = const BodyPeriodData()});

  BodyPeriodData data;
  final List<(BodyMeasurementType, String)> deletedEntries = [];

  @override
  Future<Result<BodyPeriodData>> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async =>
      Ok(data);

  @override
  Future<Result<void>> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async {
    deletedEntries.add((type, id));
    // An honest fake: subsequent period loads no longer contain the entry, as
    // Health Connect would behave. The screen force-reloads after a delete.
    data = data.copyWith(
      weightEntries: data.weightEntries.where((e) => e.id != id).toList(),
      heightEntries: data.heightEntries.where((e) => e.id != id).toList(),
      bodyFatEntries: data.bodyFatEntries.where((e) => e.id != id).toList(),
    );
    return const Ok(null);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

WeightEntry _weight(
  LocalDate date,
  double kg, {
  String id = '',
  bool isOpenVitalsEntry = false,
}) =>
    WeightEntry(
      time: DateTime(date.year, date.month, date.day, 8),
      weightKg: kg,
      source: 'test',
      id: id,
      isOpenVitalsEntry: isOpenVitalsEntry,
    );

BodyFatEntry _bodyFat(LocalDate date, double percent) => BodyFatEntry(
      time: DateTime(date.year, date.month, date.day, 9),
      percent: percent,
      source: 'test',
    );

Future<Widget> _bootstrap({
  required _FakeBodyRepository repository,
  Set<String>? granted,
}) async {
  final grantedPermissions = granted ?? {HcPermissions.readWeight};
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      bodyRepositoryProvider.overrideWithValue(repository),
      unitSystemProvider.overrideWithValue(UnitSystem.metric),
      healthConnectAvailabilityProvider
          .overrideWith((ref) async => HealthConnectAvailability.available),
      grantedHealthPermissionsProvider
          .overrideWith((ref) async => grantedPermissions),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: BodyScreen(),
    ),
  );
}

void main() {
  final today = LocalDate.now();

  testWidgets(
      'aggregate renders one trend chart per tracked metric, none for empty ones',
      (tester) async {
    final repo = _FakeBodyRepository(
      data: BodyPeriodData(
        weightEntries: [
          _weight(today, 70.5),
          _weight(today.minusDays(2), 71.2),
        ],
        bodyFatEntries: [_bodyFat(today, 21.0)],
      ),
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    // Tracked: weight + body fat (no height, so no BMI series; the other five
    // metrics have no entries). Kotlin renders a chart only for tracked ones.
    expect(find.byType(MetricBarChart, skipOffstage: false), findsNWidgets(2));
    expect(find.text('Body trends', skipOffstage: false), findsOneWidget);
  });

  testWidgets('statistics grid shows every metric with its latest value',
      (tester) async {
    final repo = _FakeBodyRepository(
      data: BodyPeriodData(
        weightEntries: [_weight(today, 70.5)],
        bodyFatEntries: [_bodyFat(today, 21.0)],
      ),
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(find.byType(InsightStatGrid, skipOffstage: false), findsOneWidget);
    // Latest values render in the grid; untracked metrics fall back to "No
    // data" (weight/bodyFat tracked → 7 of the 9 metric tiles say No data).
    expect(find.text('70.5', skipOffstage: false), findsWidgets);
    expect(find.text('21.0', skipOffstage: false), findsWidgets);
    expect(find.text('No data', skipOffstage: false), findsNWidgets(7));
  });

  testWidgets('swiping an OpenVitals entry away deletes it through the repo',
      (tester) async {
    final repo = _FakeBodyRepository(
      data: BodyPeriodData(
        weightEntries: [
          _weight(today, 70.5, id: 'w1', isOpenVitalsEntry: true),
        ],
      ),
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    final row = find.byType(SwipeToDeleteEntryRow, skipOffstage: false);
    expect(row, findsOneWidget);
    await tester.dragUntilVisible(
      row,
      find.byType(ListView),
      const Offset(0, -200),
    );
    await tester.pumpAndSettle();
    await tester.drag(row, const Offset(-600, 0));
    await tester.pumpAndSettle();

    expect(repo.deletedEntries, [(BodyMeasurementType.weight, 'w1')]);
  });

  testWidgets('read-only (non-OpenVitals) entries are not swipe-deletable',
      (tester) async {
    final repo = _FakeBodyRepository(
      data: BodyPeriodData(weightEntries: [_weight(today, 70.5)]),
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(
      find.byType(SwipeToDeleteEntryRow, skipOffstage: false),
      findsNothing,
    );
    expect(find.text('Entries', skipOffstage: false), findsOneWidget);
  });

  testWidgets('empty period renders the body placeholder', (tester) async {
    final repo = _FakeBodyRepository();
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    expect(find.byType(MetricCardPlaceholder), findsOneWidget);
    expect(find.byType(MetricBarChart), findsNothing);
    expect(find.byType(InsightStatGrid), findsNothing);
  });

  testWidgets('section reorder persists through the preferences repository',
      (tester) async {
    final repo = _FakeBodyRepository(
      data: BodyPeriodData(weightEntries: [_weight(today, 70.5)]),
    );
    await tester.pumpWidget(await _bootstrap(repository: repo));
    await tester.pumpAndSettle();

    final container =
        ProviderScope.containerOf(tester.element(find.byType(BodyScreen)));

    // The app-bar toggle drives the shared edit mode.
    await tester.tap(find.byIcon(Icons.tune));
    await tester.pump();
    expect(container.read(metricDetailSectionEditProvider), isTrue);

    container.read(metricDetailSectionOrderProvider.notifier).moveSectionToTarget(
          MetricDetailSectionId.entries,
          MetricDetailSectionId.statistics,
        );
    await tester.pump();

    final stored =
        container.read(preferencesRepositoryProvider).metricDetailSectionOrder();
    expect(stored, isNotNull);
    expect(
      stored!.indexOf('ENTRIES'),
      lessThan(stored.indexOf('STATISTICS')),
    );
    // The in-memory order matches what was persisted.
    final order = container.read(metricDetailSectionOrderProvider);
    expect(
      order.indexOf(MetricDetailSectionId.entries),
      lessThan(order.indexOf(MetricDetailSectionId.statistics)),
    );
  });
}
