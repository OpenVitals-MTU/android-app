import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:openvitals/navigation/app_router.dart';
import 'package:openvitals/navigation/app_routes.dart';

import 'package:openvitals/features/imports/application/pending_route_import.dart';
import 'package:openvitals/features/imports/presentation/route_import_intent.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_view_model.dart';

/// Stands in for the native side so the bootstrap can be driven without an
/// Android host.
class _FakeChannel implements RouteImportIntentChannel {
  _FakeChannel(this._handles);

  final List<ActivityRouteFileHandle?> _handles;
  int calls = 0;

  @override
  Future<ActivityRouteFileHandle?> takePendingImport() async {
    calls++;
    return _handles.isEmpty ? null : _handles.removeAt(0);
  }
}

ActivityRouteFileHandle _handle(String name) => ActivityRouteFileHandle(
      bytes: Uint8List.fromList([1, 2, 3]),
      fileName: name,
    );

void main() {
  const channel = MethodChannel('tech.mmarca.openvitals/route_import');

  group('RouteImportIntentChannel', () {
    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
    });

    test('maps the native payload to an ActivityRouteFileHandle', () async {
      TestWidgetsFlutterBinding.ensureInitialized();
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async {
        expect(call.method, 'takePendingRouteImport');
        return <String, Object?>{
          'fileName': 'run.gpx',
          'bytes': Uint8List.fromList([7, 8, 9]),
        };
      });

      final handle = await const RouteImportIntentChannel().takePendingImport();

      expect(handle, isNotNull);
      expect(handle!.fileName, 'run.gpx');
      expect(handle.bytes, [7, 8, 9]);
    });

    test('returns null when nothing is pending', () async {
      TestWidgetsFlutterBinding.ensureInitialized();
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, (call) async => null);

      expect(
        await const RouteImportIntentChannel().takePendingImport(),
        isNull,
      );
    });

    test('a missing plugin (non-Android host) is not an error', () async {
      TestWidgetsFlutterBinding.ensureInitialized();
      // No handler registered → MissingPluginException → swallowed.
      expect(
        await const RouteImportIntentChannel().takePendingImport(),
        isNull,
      );
    });
  });

  group('RouteImportIntentBootstrap', () {
    /// A stand-in router with just the two routes the drain touches, so the test
    /// exercises the real `push` without building the whole app graph (the real
    /// `goRouterProvider` transitively needs SharedPreferences and every screen).
    GoRouter stubRouter() => GoRouter(
          initialLocation: '/',
          routes: [
            GoRoute(
              path: '/',
              builder: (_, _) => const Scaffold(body: Text('home')),
            ),
            GoRoute(
              path: AppRoutes.activityEntry,
              builder: (_, _) => const Scaffold(body: Text('activity-entry')),
            ),
          ],
        );

    Future<ProviderContainer> pump(
      WidgetTester tester,
      _FakeChannel fake,
    ) async {
      final router = stubRouter();
      final container = ProviderContainer(overrides: [
        routeImportIntentChannelProvider.overrideWithValue(fake),
        goRouterProvider.overrideWithValue(router),
      ]);
      addTearDown(container.dispose);

      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: container,
          child: RouteImportIntentBootstrap(
            child: MaterialApp.router(routerConfig: router),
          ),
        ),
      );
      await tester.pumpAndSettle();
      return container;
    }

    testWidgets('a pending file is parked in the seam and opens the form',
        (tester) async {
      final fake = _FakeChannel([_handle('ride.fit')]);
      final container = await pump(tester, fake);

      // Drained on the first frame, handed to the same seam the Settings import
      // cards use, and the activity-entry form is opened to review it.
      expect(fake.calls, 1);
      expect(container.read(pendingRouteImportProvider)?.fileName, 'ride.fit');
      expect(find.text('activity-entry'), findsOneWidget);
    });

    testWidgets('nothing pending leaves the seam empty and does not navigate',
        (tester) async {
      final fake = _FakeChannel([]);
      final container = await pump(tester, fake);

      expect(fake.calls, 1);
      expect(container.read(pendingRouteImportProvider), isNull);
      expect(find.text('home'), findsOneWidget);
      expect(find.text('activity-entry'), findsNothing);
    });
  });
}
