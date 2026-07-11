import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/homewidgets/home_widget_launch.dart';

void main() {
  group('homeWidgetRouteLocation', () {
    // The exact form the Glance widgets emit (HomeWidgetSnapshot.kt builds
    // `openvitals://widget?route=<route>` with the route percent-encoded). This
    // is a cross-process contract between the Kotlin and Dart halves: if it
    // drifts, every widget tap silently becomes a no-op, so pin it hard.
    test('parses the native openvitals://widget?route=... form', () {
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://widget?route=dashboard'),
        ),
        '/dashboard',
      );
      // A route containing `/` is percent-encoded by the native Uri.Builder and
      // must survive the round trip.
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://widget?route=metric%2FSTEPS'),
        ),
        '/metric/STEPS',
      );
      expect(
        homeWidgetRouteLocation(
          Uri.parse(
            'openvitals://widget?route=daily_readiness%2Fbody_energy%2F2026-07-10',
          ),
        ),
        '/daily_readiness/body_energy/2026-07-10',
      );
      // Still rejects a route that is not on the allow-list, even via the query.
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://widget?route=settings%2Fdebug_diagnostics'),
        ),
        isNull,
      );
    });

    test('maps every allowed widget route to its go_router location', () {
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://dashboard')),
        '/dashboard',
      );
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://daily_readiness')),
        '/daily_readiness',
      );
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://daily_readiness/body_energy/2026-07-10'),
        ),
        '/daily_readiness/body_energy/2026-07-10',
      );
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://metric/STEPS')),
        '/metric/STEPS',
      );
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://manual_entry/hydration')),
        '/manual_entry/hydration',
      );
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://manual_entry/hydration/log/coffee'),
        ),
        '/manual_entry/hydration/log/coffee',
      );
    });

    test('accepts a path-only uri (no authority)', () {
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals:///metric/HRV')),
        '/metric/HRV',
      );
    });

    test('rejects a route that is not on the allow-list', () {
      // The launch extra crosses a process boundary: anything not explicitly
      // allowed is dropped rather than pushed.
      expect(homeWidgetRouteLocation(Uri.parse('openvitals://settings')), isNull);
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://settings/permissions')),
        isNull,
      );
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://manual_entry/activity')),
        isNull,
      );
    });

    test('rejects a malformed argument', () {
      // Not a known metric id.
      expect(
        homeWidgetRouteLocation(Uri.parse('openvitals://metric/NOT_A_METRIC')),
        isNull,
      );
      // Not an ISO date.
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://daily_readiness/body_energy/yesterday'),
        ),
        isNull,
      );
      expect(
        homeWidgetRouteLocation(
          Uri.parse('openvitals://daily_readiness/body_energy/2026-13-45'),
        ),
        isNull,
      );
      // Missing argument entirely.
      expect(homeWidgetRouteLocation(Uri.parse('openvitals://metric')), isNull);
    });

    test('rejects nothing to open', () {
      expect(homeWidgetRouteLocation(null), isNull);
      expect(homeWidgetRouteLocation(Uri.parse('openvitals://')), isNull);
    });
  });

  group('homeWidgetRouteLocationOf', () {
    test('maps the raw route string the snapshot carries', () {
      // This is the exact string the snapshot builders write into `route`.
      expect(homeWidgetRouteLocationOf('metric/RESTING_HEART_RATE'),
          '/metric/RESTING_HEART_RATE');
      expect(homeWidgetRouteLocationOf('dashboard'), '/dashboard');
      expect(homeWidgetRouteLocationOf('nonsense'), isNull);
    });
  });
}
