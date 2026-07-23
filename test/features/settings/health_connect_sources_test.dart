import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/settings/application/health_connect_sources.dart';

void main() {
  DateTime t(int day) => DateTime.utc(2026, 7, day);

  group('aggregateHealthConnectSources', () {
    test('folds observations per package, counting and dating them', () {
      final sources = aggregateHealthConnectSources({
        'heart rate': [
          ('com.sec.android.app.shealth', t(1)),
          ('com.sec.android.app.shealth', t(3)),
          ('tech.mmarca.openvitals', t(2)),
        ],
        'sleep': [
          ('com.sec.android.app.shealth', t(4)),
        ],
      });

      expect(sources.length, 2);
      // Sorted most-recent-contribution first: Samsung (day 4) before us (day 2).
      expect(sources.first.package, 'com.sec.android.app.shealth');
      expect(sources.first.recordCount, 3);
      expect(sources.first.lastSeen, t(4));
      expect(sources.first.metrics, {'heart rate', 'sleep'});
      expect(sources.first.displayName, 'Samsung Health');

      expect(sources.last.package, 'tech.mmarca.openvitals');
      expect(sources.last.metrics, {'heart rate'});
      expect(sources.last.displayName, 'OpenVitals (this app)');
    });

    test('blank sources collapse to a single "unknown" entry', () {
      final sources = aggregateHealthConnectSources({
        'heart rate': [('', t(1)), ('   ', t(2))],
      });

      expect(sources.single.package, 'unknown');
      expect(sources.single.recordCount, 2);
      expect(sources.single.displayName, 'Unknown source');
    });

    test('an empty read yields no sources', () {
      expect(
        aggregateHealthConnectSources({'heart rate': const [], 'sleep': const []}),
        isEmpty,
      );
    });

    test('an unknown package keeps its raw name', () {
      final sources = aggregateHealthConnectSources({
        'sleep': [('com.example.watch', t(1))],
      });
      expect(sources.single.displayName, 'com.example.watch');
    });
  });
}
