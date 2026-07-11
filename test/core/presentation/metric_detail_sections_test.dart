import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/metric_detail_sections.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/widget_edit_controls.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<PreferencesRepository> prefsWith(List<String>? storedOrder) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = PreferencesRepository(await SharedPreferences.getInstance());
    if (storedOrder != null) prefs.setMetricDetailSectionOrder(storedOrder);
    return prefs;
  }

  Future<ProviderContainer> pumpSections(
    WidgetTester tester, {
    List<String>? storedOrder,
    bool editing = false,
  }) async {
    tester.view.physicalSize = const Size(1000, 2000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final prefs = await prefsWith(storedOrder);
    final container = ProviderContainer(
      overrides: [preferencesRepositoryProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    if (editing) container.read(metricDetailSectionEditProvider.notifier).toggle();

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: const Scaffold(
            body: OrderedMetricDetailSections(
              sections: [
                MetricDetailSection(
                  MetricDetailSectionId.dailyGoal,
                  Text('goal'),
                ),
                MetricDetailSection(
                  MetricDetailSectionId.statistics,
                  Text('stats'),
                ),
                MetricDetailSection(
                  MetricDetailSectionId.entries,
                  Text('entries'),
                ),
                MetricDetailSection(
                  MetricDetailSectionId.intradayChart,
                  visible: false,
                  Text('intraday'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return container;
  }

  /// The visible section labels, top to bottom.
  List<String> renderedOrder(WidgetTester tester) {
    final texts = tester
        .widgetList<Text>(find.byType(Text))
        .map((text) => text.data)
        .whereType<String>()
        .where((data) => const {'goal', 'stats', 'entries'}.contains(data));
    return texts.toList();
  }

  testWidgets('renders in the default order and hides invisible sections',
      (tester) async {
    await pumpSections(tester);

    // defaultMetricDetailSectionOrder puts dailyGoal before statistics
    // before entries.
    expect(renderedOrder(tester), ['goal', 'stats', 'entries']);
    expect(find.text('intraday'), findsNothing);
  });

  testWidgets('honours the persisted order', (tester) async {
    await pumpSections(tester, storedOrder: ['ENTRIES', 'STATISTICS', 'DAILY_GOAL']);
    expect(renderedOrder(tester), ['entries', 'stats', 'goal']);
  });

  testWidgets('a stored order missing newer sections still shows them',
      (tester) async {
    // Only ENTRIES stored: the rest are appended in their default order.
    await pumpSections(tester, storedOrder: ['ENTRIES']);
    expect(renderedOrder(tester), ['entries', 'goal', 'stats']);
  });

  testWidgets('edit mode wraps sections in the shared reorderable tile',
      (tester) async {
    await pumpSections(tester, editing: true);

    expect(find.byType(ReorderableEditTile), findsNWidgets(3));
    expect(find.byType(EditModeHint), findsOneWidget);
  });

  testWidgets('moveSectionToTarget persists a drop-on-target reorder',
      (tester) async {
    final container = await pumpSections(tester);
    final notifier = container.read(metricDetailSectionOrderProvider.notifier);

    notifier.moveSectionToTarget(
      MetricDetailSectionId.dailyGoal,
      MetricDetailSectionId.entries,
    );
    await tester.pumpAndSettle();

    // dailyGoal lands exactly where entries sat.
    expect(renderedOrder(tester), ['stats', 'entries', 'goal']);

    // ...and survives a rebuild from preferences.
    final stored =
        container.read(preferencesRepositoryProvider).metricDetailSectionOrder();
    expect(stored, isNotNull);
    final restored = metricDetailSectionOrderFromStored(stored);
    expect(
      restored.indexOf(MetricDetailSectionId.dailyGoal) >
          restored.indexOf(MetricDetailSectionId.entries),
      isTrue,
    );
  });

  testWidgets('moveSection nudges one place in the full order', (tester) async {
    final container = await pumpSections(tester);
    final notifier = container.read(metricDetailSectionOrderProvider.notifier);

    List<MetricDetailSectionId> order() =>
        container.read(metricDetailSectionOrderProvider);

    final before = order().indexOf(MetricDetailSectionId.entries);
    notifier.moveSection(MetricDetailSectionId.entries, -1);
    await tester.pumpAndSettle();
    expect(order().indexOf(MetricDetailSectionId.entries), before - 1);

    notifier.moveSection(MetricDetailSectionId.entries, 1);
    await tester.pumpAndSettle();
    expect(order().indexOf(MetricDetailSectionId.entries), before);
  });

  testWidgets('moveSection cannot push a section past either end',
      (tester) async {
    final container = await pumpSections(tester);
    final notifier = container.read(metricDetailSectionOrderProvider.notifier);
    final first = container.read(metricDetailSectionOrderProvider).first;
    final last = container.read(metricDetailSectionOrderProvider).last;

    notifier.moveSection(first, -1);
    notifier.moveSection(last, 1);
    await tester.pumpAndSettle();

    expect(container.read(metricDetailSectionOrderProvider).first, first);
    expect(container.read(metricDetailSectionOrderProvider).last, last);
  });
}
