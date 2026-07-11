import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../../../domain/model/activity_models.dart';
import '../../../l10n/app_localizations.dart';
import 'offline_base_map_layer.dart';
import 'route_geometry.dart';

/// Renders a workout GPS route on a [FlutterMap], replacing the Kotlin
/// `OfflineRouteMapOrPreview` / MapLibre + Mapsforge composables.
///
/// The route is drawn as one [Polyline] per segment (breaks split the line),
/// with start / end / current-location markers, and the camera auto-fits the
/// route bounds.
///
/// Base map: OpenVitals is offline-only (the shipped app declares no INTERNET
/// permission). The base map comes from the user's imported offline packs via
/// [OfflineBaseMapLayer] (PMTiles rendered as vector tiles, Mapsforge `.map`
/// packs rasterised on demand); with no active pack the route renders on a
/// plain [_offlineBackground] canvas — the Kotlin `RoutePreview` fallback. A
/// caller-supplied [tileProvider]/[urlTemplate] (tests, debugging) replaces
/// the offline layer entirely and must never be the shipped default.
class RouteMapView extends StatefulWidget {
  const RouteMapView({
    super.key,
    required this.points,
    this.routeBreakIndexes = const <int>[],
    this.currentPoint,
    this.height = 240,
    this.tileProvider,
    this.urlTemplate,
    this.showRecenterControl = false,
  });

  final List<ExerciseRoutePoint> points;
  final List<int> routeBreakIndexes;
  final ExerciseRoutePoint? currentPoint;
  final double height;

  /// Kotlin `showRecenterControl`: overlays a circular button (bottom-end)
  /// that re-fits the camera to the current route bounds — the initial camera
  /// fit only happens once, so during a live recording the user can pan/zoom
  /// away (or the track can outgrow the viewport) and jump back with one tap.
  final bool showRecenterControl;

  /// Offline tile source (e.g. an imported MBTiles/PMTiles pack). When null (and
  /// [urlTemplate] is null) no base-map tiles are drawn. Tests pass a
  /// network-free provider to render tiles without touching the network.
  final TileProvider? tileProvider;

  /// Optional raster tile URL template. Null by default so the shipped,
  /// offline-only build never performs a network fetch. Only set for debugging
  /// with an explicit online source.
  final String? urlTemplate;

  /// Canvas colour shown behind the route when no base-map tiles are present.
  static const Color _offlineBackground = Color(0xFFE7E3DC);

  static const Color _routeColor = Color(0xFFD9462F);
  static const Color _startColor = Color(0xFF1F9D55);
  static const Color _endColor = Color(0xFF6B5DD3);
  static const Color _currentColor = Color(0xFF1D4ED8);

  @override
  State<RouteMapView> createState() => _RouteMapViewState();
}

class _RouteMapViewState extends State<RouteMapView> {
  final MapController _mapController = MapController();

  @override
  void dispose() {
    _mapController.dispose();
    super.dispose();
  }

  /// Kotlin `OfflineRouteMapRenderState.recenter` / `fitCamera`: re-fit the
  /// camera to the current route + current-location point.
  void _recenter(RouteBounds? bounds) {
    if (bounds == null) return;
    if (bounds.isSinglePoint) {
      _mapController.move(
        LatLng(bounds.centerLatitude, bounds.centerLongitude),
        15.5,
      );
      return;
    }
    _mapController.fitCamera(
      CameraFit.bounds(
        bounds: LatLngBounds(
          LatLng(bounds.minLatitude, bounds.minLongitude),
          LatLng(bounds.maxLatitude, bounds.maxLongitude),
        ),
        padding: const EdgeInsets.all(32),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final segments = routeSegments(widget.points, widget.routeBreakIndexes)
        .where((segment) => segment.length >= 2)
        .map((segment) => segment.map(_toLatLng).toList())
        .toList();

    final cameraPoints = <ExerciseRoutePoint>[
      ...widget.points,
      if (widget.currentPoint != null) widget.currentPoint!,
    ];
    final bounds = RouteBounds.fromPoints(cameraPoints);

    // An explicitly provided tile source (tests, debugging) replaces the
    // offline base-map layer; otherwise the active imported pack renders and,
    // with none, the route sits on a plain canvas with no network fetch.
    final hasExplicitTileSource =
        widget.tileProvider != null || widget.urlTemplate != null;

    final scheme = Theme.of(context).colorScheme;

    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: SizedBox(
        height: widget.height,
        child: Stack(
          children: [
            FlutterMap(
              mapController: _mapController,
              options: _mapOptions(bounds),
              children: [
                if (hasExplicitTileSource)
                  TileLayer(
                    urlTemplate: widget.urlTemplate,
                    userAgentPackageName: 'tech.mmarca.openvitals',
                    tileProvider: widget.tileProvider,
                  )
                else
                  const OfflineBaseMapLayer(),
                if (segments.isNotEmpty)
                  PolylineLayer(
                    polylines: [
                      for (final segment in segments)
                        Polyline(
                          points: segment,
                          color: RouteMapView._routeColor,
                          strokeWidth: 4,
                        ),
                    ],
                  ),
                MarkerLayer(markers: _markers()),
              ],
            ),
            // Kotlin: a circular MyLocation FAB aligned bottom-end, 12dp in.
            if (widget.showRecenterControl)
              Positioned(
                right: 12,
                bottom: 12,
                child: IconButton(
                  onPressed: () => _recenter(bounds),
                  tooltip: AppLocalizations.of(context).cdRecenterMap,
                  icon: const Icon(Icons.my_location_outlined),
                  style: IconButton.styleFrom(
                    backgroundColor: scheme.surfaceContainerHigh,
                    foregroundColor: scheme.primary,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  MapOptions _mapOptions(RouteBounds? bounds) {
    if (bounds == null) {
      return const MapOptions(
        initialCenter: LatLng(0, 0),
        initialZoom: 1,
        backgroundColor: RouteMapView._offlineBackground,
        interactionOptions: InteractionOptions(flags: InteractiveFlag.none),
      );
    }
    if (bounds.isSinglePoint) {
      return MapOptions(
        initialCenter: LatLng(bounds.centerLatitude, bounds.centerLongitude),
        initialZoom: 15.5,
        backgroundColor: RouteMapView._offlineBackground,
      );
    }
    return MapOptions(
      backgroundColor: RouteMapView._offlineBackground,
      initialCameraFit: CameraFit.bounds(
        bounds: LatLngBounds(
          LatLng(bounds.minLatitude, bounds.minLongitude),
          LatLng(bounds.maxLatitude, bounds.maxLongitude),
        ),
        padding: const EdgeInsets.all(32),
      ),
    );
  }

  List<Marker> _markers() {
    final validPoints = widget.points
        .where((point) => point.latitude.isFinite && point.longitude.isFinite)
        .toList();
    return [
      if (validPoints.isNotEmpty)
        _marker(validPoints.first, RouteMapView._startColor),
      if (validPoints.length > 1)
        _marker(validPoints.last, RouteMapView._endColor),
      if (widget.currentPoint != null &&
          widget.currentPoint!.latitude.isFinite &&
          widget.currentPoint!.longitude.isFinite)
        _marker(widget.currentPoint!, RouteMapView._currentColor, radius: 8),
    ];
  }

  Marker _marker(ExerciseRoutePoint point, Color color, {double radius = 6}) {
    final diameter = radius * 2;
    return Marker(
      point: _toLatLng(point),
      width: diameter + 4,
      height: diameter + 4,
      child: Container(
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          border: Border.all(color: Colors.white, width: 2),
        ),
      ),
    );
  }

  static LatLng _toLatLng(ExerciseRoutePoint point) =>
      LatLng(point.latitude, point.longitude);
}
