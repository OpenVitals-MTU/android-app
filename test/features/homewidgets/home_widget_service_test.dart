import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';

import 'fake_home_widget_client.dart';

void main() {
  group('homeWidgetKeyPrefix', () {
    test('namespaces a shared widget by its storage key', () {
      expect(
        homeWidgetKeyPrefix(HomeWidgetId.bodyEnergy),
        'body_energy.',
      );
    });

    test('namespaces a per-instance widget by appWidgetId too', () {
      expect(
        homeWidgetKeyPrefix(HomeWidgetId.metric, appWidgetId: 42),
        'metric.42.',
      );
    });

    test('the two beverage widgets share one storage namespace', () {
      // Kotlin likewise shares one state schema between the 2x1 and the 1x1 and
      // tells them apart by provider class; appWidgetIds are globally unique, so
      // instances still cannot collide.
      expect(
        homeWidgetKeyPrefix(HomeWidgetId.quickBeverage, appWidgetId: 7),
        homeWidgetKeyPrefix(HomeWidgetId.quickBeverageOneTap, appWidgetId: 7),
      );
    });
  });

  group('homeWidgetDataMap', () {
    test('maps the snapshot to the Kotlin key layout, under the prefix', () {
      final data = homeWidgetDataMap(
        const HomeWidgetSnapshot(
          title: 'Readiness',
          value: '82',
          unit: '',
          subtitle: 'Prime',
          route: 'daily_readiness',
          rows: [HomeWidgetRow(label: 'Recommended', value: 'Go hard')],
        ),
        prefix: 'daily_readiness.',
        selectionId: 'DAILY_READINESS',
      );

      expect(data['daily_readiness.selection_id'], 'DAILY_READINESS');
      expect(data['daily_readiness.title'], 'Readiness');
      expect(data['daily_readiness.value'], '82');
      expect(data['daily_readiness.subtitle'], 'Prime');
      expect(data['daily_readiness.route'], 'daily_readiness');
      expect(data['daily_readiness.row_count'], 1);
      expect(data['daily_readiness.row_0_label'], 'Recommended');
      expect(data['daily_readiness.row_0_value'], 'Go hard');
      expect(data['daily_readiness.row_0_subtitle'], '');
    });

    test('omits selection_id when not provided', () {
      final data = homeWidgetDataMap(
        const HomeWidgetSnapshot(title: 'Today'),
        prefix: 'today_vitals.',
      );
      expect(data.containsKey('today_vitals.selection_id'), isFalse);
      expect(data['today_vitals.row_count'], 0);
    });

    test('caps rows at maxHomeWidgetRows', () {
      final rows = List.generate(
        maxHomeWidgetRows + 5,
        (i) => HomeWidgetRow(label: 'L$i', value: 'V$i'),
      );

      final data = homeWidgetDataMap(
        HomeWidgetSnapshot(title: 'Vitals', rows: rows),
        prefix: 'today_vitals.',
      );

      expect(data['today_vitals.row_count'], maxHomeWidgetRows);
      expect(
        data.containsKey('today_vitals.row_${maxHomeWidgetRows - 1}_label'),
        isTrue,
      );
      expect(
        data.containsKey('today_vitals.row_${maxHomeWidgetRows}_label'),
        isFalse,
      );
    });

    test('two widgets pushed in turn do not clobber each other', () {
      // The regression this whole namespacing exists to prevent: home_widget
      // keeps one shared preferences file for every widget.
      final readiness = homeWidgetDataMap(
        const HomeWidgetSnapshot(title: 'Readiness', value: '82'),
        prefix: homeWidgetKeyPrefix(HomeWidgetId.dailyReadiness),
      );
      final energy = homeWidgetDataMap(
        const HomeWidgetSnapshot(title: 'Body energy', value: '64'),
        prefix: homeWidgetKeyPrefix(HomeWidgetId.bodyEnergy),
      );

      final merged = {...readiness, ...energy};
      expect(merged['daily_readiness.value'], '82');
      expect(merged['body_energy.value'], '64');
    });
  });

  group('HomeWidgetService', () {
    test('persists every key then updates the qualified receiver', () async {
      final client = FakeHomeWidgetClient();
      final service = HomeWidgetService(client: client);

      await service.pushSnapshot(
        HomeWidgetId.bodyEnergy,
        const HomeWidgetSnapshot(title: 'Body energy', value: '64'),
      );

      expect(client.saved['body_energy.title'], 'Body energy');
      expect(client.saved['body_energy.value'], '64');
      expect(
        client.updated.single,
        'tech.mmarca.openvitals.features.homewidgets.HomeBodyEnergyWidgetReceiver',
      );
    });

    test('a per-instance push is keyed by appWidgetId', () async {
      final client = FakeHomeWidgetClient();
      final service = HomeWidgetService(client: client);

      await service.pushSnapshot(
        HomeWidgetId.metric,
        const HomeWidgetSnapshot(title: 'Steps', value: '8,432'),
        appWidgetId: 11,
        selectionId: 'STEPS',
      );

      expect(client.saved['metric.11.title'], 'Steps');
      expect(client.saved['metric.11.selection_id'], 'STEPS');
    });

    test('instancesOf returns only that widget\'s placed instances', () async {
      const metricClass =
          'tech.mmarca.openvitals.features.homewidgets.HomeMetricWidgetReceiver';
      const energyClass =
          'tech.mmarca.openvitals.features.homewidgets.HomeBodyEnergyWidgetReceiver';
      final client = FakeHomeWidgetClient(installed: const [
        HomeWidgetInstance(appWidgetId: 1, className: metricClass),
        HomeWidgetInstance(appWidgetId: 2, className: energyClass),
        HomeWidgetInstance(appWidgetId: 3, className: metricClass),
      ]);
      final service = HomeWidgetService(client: client);

      final metrics = await service.instancesOf(HomeWidgetId.metric);

      expect(metrics.map((w) => w.appWidgetId), [1, 3]);
    });
  });
}
