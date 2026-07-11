import 'package:flutter/gestures.dart' show kLongPressTimeout;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/dashboard/dashboard_data_loader.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/usecase/load_dashboard_day_use_case.dart';
import 'package:openvitals/features/dashboard/dashboard_screen.dart';
import 'package:openvitals/features/dashboard/dashboard_sensor_status.dart';
import 'package:openvitals/health/health_data_source.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/health_connect_gate.dart';
import 'package:openvitals/ui/components/metric_stat_card.dart';
import 'package:openvitals/ui/components/period_navigator.dart';
import 'package:openvitals/ui/components/permission_callout.dart';
import 'package:openvitals/ui/components/summary_ring_card.dart';

/// A [HealthDataSource] whose availability + granted-permission answers are
/// fixed, so the real [HealthRepositoryImpl] over it (and the notifier) resolve
/// deterministically without any platform access.
class _FakeHealthDataSource extends HealthDataSource {
  _FakeHealthDataSource({
    required HealthConnectAvailability availability,
    this.granted = const <String>{},
  }) {
    cachedAvailability = availability;
  }

  Set<String> granted;

  @override
  Future<HealthConnectAvailability> availability() async => cachedAvailability;

  @override
  Future<Set<String>> grantedPermissions() async => granted;
}

/// A [LoadDashboardDayUseCase] whose result is built by [_build] from the query.
class _FakeUseCase extends LoadDashboardDayUseCase {
  _FakeUseCase(this._build) : super(DashboardDataLoader(HealthDataSource()));

  final DashboardData Function(DashboardQuery query) _build;

  @override
  Future<DashboardData> call(DashboardQuery query) async => _build(query);
}

DashboardData _sampleData(DashboardQuery query, {Set<String> missing = const {}}) {
  return DashboardData(
    date: query.date,
    steps: 8000,
    distanceMeters: 5200,
    caloriesKcal: 540,
    hydrationLiters: 1.5,
    avgHeartRateBpm: 72,
    restingHeartRateBpm: 58,
    weightKg: 70,
    caloriesInKcal: 1800,
    proteinGrams: 90,
    loadedMetrics: query.visibleMetrics,
    // Every metric is device-supported unless a test says otherwise, so tiles
    // render for all of them (empty ones included).
    supportedMetrics: DashboardMetric.values.toSet(),
    missingPermissions: missing,
  );
}

/// Enough populated metrics to fill at least three carousel pages (6 tiles per
/// page), so an edge-scroll drag has somewhere to go twice.
DashboardData _threePageData(DashboardQuery query) {
  return _sampleData(query).copyWith(
    activeCaloriesKcal: 320,
    floorsClimbed: 12,
    elevationGainedMeters: 140,
    carbsGrams: 210,
    fatGrams: 60,
    latestSystolicMmHg: 118,
    latestDiastolicMmHg: 76,
    latestSpO2Percent: 97,
    latestVo2Max: 44,
    avgRespiratoryRate: 14,
    latestBodyTemperatureCelsius: 36.7,
    hrvRmssdMs: 42,
    hrvSampleCount: 30,
  );
}

/// The permissions the dashboard needs before it stops offering the inline
/// Health Connect promo card.
final Set<String> _minimumPermissions =
    HealthDataSource().permissionService.minimumOnboardingPermissions;

Future<Widget> _bootstrap({
  required HealthConnectAvailability availability,
  Set<String>? granted,
  Set<String> missing = const <String>{},
  DashboardData Function(DashboardQuery query)? dataBuilder,
  DashboardSensorStatus? sensorStatus,
}) async {
  // Default to a fully-permissioned install; a test that wants the promo card
  // passes an empty granted set explicitly.
  final grantedSet = granted ?? _minimumPermissions;
  SharedPreferences.setMockInitialValues(const <String, Object>{});
  final prefs = await SharedPreferences.getInstance();
  return ProviderScope(
    overrides: [
      sharedPreferencesProvider.overrideWithValue(prefs),
      healthDataSourceProvider.overrideWithValue(
        _FakeHealthDataSource(availability: availability, granted: grantedSet),
      ),
      healthConnectAvailabilityProvider.overrideWith((ref) async => availability),
      grantedHealthPermissionsProvider.overrideWith((ref) async => grantedSet),
      loadDashboardDayUseCaseProvider.overrideWithValue(
        _FakeUseCase(
          dataBuilder ?? (query) => _sampleData(query, missing: missing),
        ),
      ),
      if (sensorStatus != null)
        dashboardSensorStatusProvider.overrideWithValue(sensorStatus),
    ],
    child: const MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: DashboardScreen()),
    ),
  );
}

/// Drives the layout on a tall test surface. The square hero ring cards are half
/// the screen wide, so on the default 800×600 surface they inflate to ~380dp
/// tall and push the lazily-built stat-tile carousel outside the render cache; a
/// taller viewport keeps the whole summary column on screen (and the width stays
/// wide enough for the permission callout not to overflow).
void _usePhoneViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 1400);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

/// Mirrors the carousel's private `_MetricCarousel._tileHeight`.
const double _kTileHeight = 82;

/// The dashboard's outer [ListView] scrollable — the carousel's [PageView] is a
/// second scrollable inside it, so scroll helpers must be told which one.
Finder _dashboardScrollable() => find
    .descendant(of: find.byType(ListView), matching: find.byType(Scrollable))
    .first;

void main() {
  testWidgets('shows a loader then renders the summary dashboard',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );

    // First frame: the gate/notifier are still resolving asynchronously.
    expect(find.byType(CircularProgressIndicator), findsWidgets);

    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(find.byType(DayNavigator), findsOneWidget);
    // Two hero ring cards (Steps + Weekly cardio) and a carousel of stat tiles.
    expect(find.byType(SummaryRingCard), findsNWidgets(2));
    expect(find.byType(MetricStatCard), findsWidgets);
    // The Log / Start quick-action row.
    expect(find.text('Log'), findsOneWidget);
    expect(find.text('Start workout'), findsOneWidget);
    expect(find.text('Steps'), findsOneWidget);
    expect(find.text('Today'), findsOneWidget);
  });

  testWidgets('carousel tiles fill their grid cell in both modes',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();

    // Normal mode: the tile stretches to the full cell height instead of
    // shrink-wrapping its content and leaving gaps between the rows.
    final firstTile = find.byType(MetricStatCard).first;
    expect(tester.getSize(firstTile).height, _kTileHeight);

    // The icon and text are centred in that height, not pinned to the top.
    final tileRect = tester.getRect(firstTile);
    final iconRect = tester.getRect(
      find.descendant(of: firstTile, matching: find.byType(Icon)).first,
    );
    expect(iconRect.center.dy, moreOrLessEquals(tileRect.center.dy, epsilon: 1));

    // Edit mode: same fill, same centring.
    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();
    final editTile = find.byType(MetricStatCard).first;
    expect(tester.getSize(editTile).height, _kTileHeight);
    final editIcon = tester.getRect(
      find.descendant(of: editTile, matching: find.byType(Icon)).first,
    );
    expect(
      editIcon.center.dy,
      moreOrLessEquals(tester.getRect(editTile).center.dy, epsilon: 1),
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('edge-scroll keeps paging while a drag is held at the edge',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        dataBuilder: _threePageData,
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();

    final pager = find.byType(PageView);
    int currentPage() =>
        tester.widget<PageView>(pager).controller!.page!.round();
    expect(currentPage(), 0);

    // Three pages of tiles, so the drag has somewhere to go twice.
    expect(tester.widget<PageView>(pager).childrenDelegate.estimatedChildCount,
        greaterThanOrEqualTo(3));

    // Long-press a carousel tile (not a hero ring), then drag it into the
    // right-hand edge zone.
    final gesture = await tester.startGesture(
      tester.getCenter(
        find
            .descendant(
              of: pager,
              matching: find.byType(LongPressDraggable<int>),
            )
            .first,
      ),
    );
    await tester.pump(kLongPressTimeout + const Duration(milliseconds: 100));
    final rect = tester.getRect(pager);
    await gesture.moveTo(Offset(rect.right - 8, rect.center.dy));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    expect(currentPage(), 1);

    // Hold still at the edge. `onDragUpdate` no longer fires, so only the
    // repeating timer can advance the pager — this is the regression: paging
    // used to stall here after the first page.
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pump(const Duration(milliseconds: 400));
    expect(currentPage(), 2);

    await gesture.up();
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('edit mode enters/exits and reorder+remove render without error',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();

    // Enter edit mode.
    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    // Remove buttons appear on both rings + tiles.
    expect(find.byTooltip('Remove widget'), findsWidgets);
    // Edit mode reorders in the carousel itself (not a separate flat grid): the
    // paged carousel is present and its tiles are long-press draggable.
    expect(
      find.text('Hold to drag & reorder · tap ✕ to remove'),
      findsOneWidget,
    );
    expect(find.byType(PageView), findsOneWidget);
    expect(find.byType(LongPressDraggable<int>), findsWidgets);

    // Remove a tile.
    await tester.tap(find.byTooltip('Remove widget').first);
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);

    // Exit edit mode; the carousel must render again.
    await tester.tap(find.byTooltip('Done'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(find.byType(MetricStatCard), findsWidgets);
  });

  testWidgets('removing a widget moves it to the add tray and back',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();

    List<String> carouselTitles() => tester
        .widgetList<MetricStatCard>(find.byType(MetricStatCard))
        .map((c) => c.title)
        .toList();

    expect(carouselTitles(), contains('Distance'));

    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();
    // Nothing removed yet, so the tray says so and offers no add buttons.
    expect(find.text('All widgets are already on the summary.'), findsOneWidget);

    // Remove the first carousel tile (Distance) via its ✕.
    final tileRemove = find.descendant(
      of: find.byType(PageView),
      matching: find.byTooltip('Remove widget'),
    );
    await tester.tap(tileRemove.first);
    await tester.pumpAndSettle();

    // It leaves the carousel and shows up in the "Add widgets" tray.
    expect(carouselTitles(), isNot(contains('Distance')));
    final addButton = find.widgetWithText(OutlinedButton, 'Distance');
    expect(addButton, findsOneWidget);
    expect(find.text('All widgets are already on the summary.'), findsNothing);

    // Adding it back restores it to the carousel and empties the tray.
    await tester.tap(addButton);
    await tester.pumpAndSettle();
    expect(carouselTitles(), contains('Distance'));
    expect(find.widgetWithText(OutlinedButton, 'Distance'), findsNothing);
    expect(find.text('All widgets are already on the summary.'), findsOneWidget);

    // And it is visible again outside edit mode.
    await tester.tap(find.byTooltip('Done'));
    await tester.pumpAndSettle();
    expect(carouselTitles(), contains('Distance'));
  });

  testWidgets('a removed hero ring can be added back from the tray',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();

    expect(find.byType(SummaryRingCard), findsNWidgets(2));

    // The hero rings render above the carousel, so the first ✕ in the tree is
    // the Steps ring's.
    await tester.tap(find.byTooltip('Remove widget').first);
    await tester.pumpAndSettle();

    expect(find.byType(SummaryRingCard), findsOneWidget);
    expect(find.widgetWithText(OutlinedButton, 'Steps'), findsOneWidget);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Steps'));
    await tester.pumpAndSettle();
    expect(find.byType(SummaryRingCard), findsNWidgets(2));
    expect(find.widgetWithText(OutlinedButton, 'Steps'), findsNothing);
  });

  testWidgets('renders the inline permission callout when permissions missing',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        missing: {'android.permission.health.READ_STEPS'},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(PermissionCallout), findsOneWidget);
    // Content still renders below the callout (Kotlin degrades gracefully).
    expect(find.byType(SummaryRingCard), findsNWidgets(2));
    expect(find.byType(MetricStatCard), findsWidgets);
  });

  testWidgets('previous-day navigation moves the selected day back',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();
    expect(find.text('Today'), findsOneWidget);

    await tester.tap(find.byTooltip('Previous day'));
    await tester.pumpAndSettle();

    expect(find.text('Yesterday'), findsOneWidget);
    expect(find.text('Today'), findsNothing);
  });

  testWidgets('shows the access gate when Health Connect is unavailable',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.notSupported),
    );
    await tester.pumpAndSettle();

    expect(find.text('Health Connect unavailable'), findsOneWidget);
    expect(find.byType(SummaryRingCard), findsNothing);
    expect(find.byType(MetricStatCard), findsNothing);
  });

  testWidgets('offers the Health Connect promo when minimum permissions '
      'are missing', (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        granted: const <String>{},
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Set up your health data'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Get started'), findsOneWidget);
    // The dashboard still renders below it (Kotlin degrades gracefully).
    expect(find.byType(SummaryRingCard), findsNWidgets(2));
  });

  testWidgets('hides the Health Connect promo once the minimum permissions '
      'are granted', (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(availability: HealthConnectAvailability.available),
    );
    await tester.pumpAndSettle();

    expect(find.text('Set up your health data'), findsNothing);
  });

  group('sensor status', () {
    // DELIBERATE DEVIATION from Kotlin, which renders a DashboardSensorStatusCard
    // between the widget carousel and today's activities. We omit it: the top-bar
    // battery action is entry point enough, and the card only pushed the
    // activities section further down. `dashboardSensorStatusProvider` still
    // exists — it is what gates that top-bar action (see adaptive_scaffold).
    testWidgets('is never rendered in the dashboard body, even with sensors',
        (tester) async {
      _usePhoneViewport(tester);
      await tester.pumpWidget(
        await _bootstrap(
          availability: HealthConnectAvailability.available,
          sensorStatus: const DashboardSensorStatus(
            devices: [
              DashboardSensorDeviceStatus(
                id: 'a',
                displayName: 'Chest strap',
                enabled: true,
                connectionStatus: BleConnectionStatus.connected,
                batteryPercent: 64,
              ),
            ],
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Sensor battery'), findsNothing);
      expect(find.textContaining('lowest'), findsNothing);
      expect(find.textContaining('connected'), findsNothing);
    });
  });

  testWidgets('edit mode offers a metric the device does not support',
      (tester) async {
    _usePhoneViewport(tester);
    await tester.pumpWidget(
      await _bootstrap(
        availability: HealthConnectAvailability.available,
        // The provider cannot serve blood oxygen on this device.
        dataBuilder: (query) => _sampleData(query).copyWith(
          supportedMetrics: DashboardMetric.values.toSet()
            ..remove(DashboardMetric.spo2),
        ),
      ),
    );
    await tester.pumpAndSettle();

    List<String> carouselTitles() => tester
        .widgetList<MetricStatCard>(find.byType(MetricStatCard))
        .map((c) => c.title)
        .toList();

    // Outside edit mode it has no tile at all.
    expect(carouselTitles(), isNot(contains('Blood oxygen')));

    await tester.tap(find.byTooltip('Edit dashboard'));
    await tester.pumpAndSettle();

    // In edit mode it is materialised — but into the add tray, not the carousel
    // (the user never placed it).
    expect(carouselTitles(), isNot(contains('Blood oxygen')));
    final addButton = find.widgetWithText(OutlinedButton, 'Blood oxygen');
    await tester.scrollUntilVisible(
      addButton,
      200,
      scrollable: _dashboardScrollable(),
    );
    expect(addButton, findsOneWidget);

    // Adding it back is not a dead end: it joins the carousel.
    await tester.tap(addButton);
    await tester.pumpAndSettle();
    expect(carouselTitles(), contains('Blood oxygen'));
    expect(find.widgetWithText(OutlinedButton, 'Blood oxygen'), findsNothing);
  });
}
