import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/features/activity/maps/route_map_view.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:vector_map_tiles/vector_map_tiles.dart';

/// A tile provider that never touches the network: it returns a 1x1
/// transparent PNG so widget tests can pump [RouteMapView] without fetching
/// map tiles.
class _TransparentTileProvider extends TileProvider {
  static final Uint8List _pixel = base64Decode(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  );

  @override
  ImageProvider getImage(TileCoordinates coordinates, TileLayer options) =>
      MemoryImage(_pixel);
}

ExerciseRoutePoint point(double lat, double lng, int seconds) =>
    ExerciseRoutePoint(
      time: DateTime.utc(2026, 6, 1).add(Duration(seconds: seconds)),
      latitude: lat,
      longitude: lng,
      altitudeMeters: null,
      horizontalAccuracyMeters: null,
      verticalAccuracyMeters: null,
    );

void main() {
  testWidgets('renders a polyline route without fetching network tiles',
      (tester) async {
    final points = [
      point(52.5200, 13.4050, 0),
      point(52.5205, 13.4062, 10),
      point(52.5210, 13.4075, 20),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            width: 300,
            child: RouteMapView(
              points: points,
              currentPoint: points.last,
              tileProvider: _TransparentTileProvider(),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(FlutterMap), findsOneWidget);
    expect(find.byType(PolylineLayer), findsOneWidget);
    expect(find.byType(MarkerLayer), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
      'default (offline) render draws no tile layer and fetches no network tiles',
      (tester) async {
    final points = [
      point(52.5200, 13.4050, 0),
      point(52.5210, 13.4075, 20),
    ];

    // No tileProvider and no urlTemplate: the shipped offline-only default.
    // The ProviderScope carries the offline map library, which resolves to "no
    // active pack" here, so the base-map layer stays empty.
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          home: Scaffold(
            body: SizedBox(
              width: 300,
              child: RouteMapView(points: points),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(FlutterMap), findsOneWidget);
    // The route still renders, but with no imported pack there is no base-map
    // TileLayer (so nothing attempts a network fetch without the INTERNET
    // permission).
    expect(find.byType(TileLayer), findsNothing);
    expect(find.byType(VectorTileLayer), findsNothing);
    expect(find.byType(PolylineLayer), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('shows no recenter control by default', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SizedBox(
            width: 300,
            child: RouteMapView(
              points: [
                point(52.5200, 13.4050, 0),
                point(52.5210, 13.4075, 20),
              ],
              tileProvider: _TransparentTileProvider(),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byIcon(Icons.my_location_outlined), findsNothing);
  });

  testWidgets('recenter control re-fits the camera to the route bounds',
      (tester) async {
    final points = [
      point(52.5200, 13.4050, 0),
      point(52.5205, 13.4062, 10),
      point(52.5210, 13.4075, 20),
    ];

    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SizedBox(
            width: 300,
            child: RouteMapView(
              points: points,
              currentPoint: points.last,
              showRecenterControl: true,
              tileProvider: _TransparentTileProvider(),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    final recenterButton = find.byIcon(Icons.my_location_outlined);
    expect(recenterButton, findsOneWidget);

    await tester.tap(recenterButton);
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('recenter control handles a single-point route', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(
          body: SizedBox(
            width: 300,
            child: RouteMapView(
              points: [point(52.5200, 13.4050, 0)],
              showRecenterControl: true,
              tileProvider: _TransparentTileProvider(),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byIcon(Icons.my_location_outlined));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('handles an empty route gracefully', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            width: 300,
            child: RouteMapView(
              points: const [],
              tileProvider: _TransparentTileProvider(),
            ),
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.byType(FlutterMap), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
