import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/preferences/metric_detail_section_id.dart';

void main() {
  test('returns default order when stored is null', () {
    expect(
      metricDetailSectionOrderFromStored(null),
      defaultMetricDetailSectionOrder,
    );
  });

  test('merges missing sections after the stored ones', () {
    final stored = [
      MetricDetailSectionId.entries.storageName,
      MetricDetailSectionId.statistics.storageName,
    ];

    final order = metricDetailSectionOrderFromStored(stored);

    expect(order.first, MetricDetailSectionId.entries);
    expect(order[1], MetricDetailSectionId.statistics);
    expect(order.length, defaultMetricDetailSectionOrder.length);
    expect(order.toSet(), defaultMetricDetailSectionOrder.toSet());
  });

  test('ignores unknown values', () {
    final order = metricDetailSectionOrderFromStored(
      ['UNKNOWN', MetricDetailSectionId.dailyGoal.storageName],
    );

    expect(order.first, MetricDetailSectionId.dailyGoal);
    expect(order.length, defaultMetricDetailSectionOrder.length);
  });
}
