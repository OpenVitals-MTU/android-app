import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../navigation/app_router.dart';
import '../../../navigation/app_routes.dart';
import '../../manualentry/activity/activity_entry_notifier.dart';
import '../application/pending_route_import.dart';

/// Reads a route file handed to the app by the OS ("Open with" / "Share" on a
/// `.gpx` / `.kml` / `.kmz` / `.fit`).
///
/// Dart half of the Kotlin `MainActivity.updateRouteImportRequest` +
/// `ExternalRouteImportRequest`: the native side resolves the `content://` URI
/// from an `ACTION_VIEW` / `ACTION_SEND` intent, reads its bytes, and parks them
/// until Dart takes them. Consumed exactly once.
class RouteImportIntentChannel {
  const RouteImportIntentChannel();

  static const _channel =
      MethodChannel('tech.mmarca.openvitals/route_import');

  /// Takes (and clears) the route file the app was opened with, if any.
  ///
  /// Returns null on every non-Android host and whenever nothing is pending, so
  /// callers need no platform guards (same swallow-and-default contract as
  /// [ActivityRecordingNativeSensors] / `LogcatReader`).
  Future<ActivityRouteFileHandle?> takePendingImport() async {
    try {
      final result = await _channel
          .invokeMapMethod<String, Object?>('takePendingRouteImport');
      if (result == null) return null;
      final bytes = result['bytes'] as Uint8List?;
      if (bytes == null || bytes.isEmpty) return null;
      return ActivityRouteFileHandle(
        bytes: bytes,
        fileName: result['fileName'] as String?,
      );
    } on PlatformException catch (error) {
      debugPrint('takePendingRouteImport failed: $error');
      return null;
    } on MissingPluginException {
      return null;
    }
  }
}

final routeImportIntentChannelProvider = Provider<RouteImportIntentChannel>(
  (ref) => const RouteImportIntentChannel(),
);

/// Drains a pending OS route-import intent into the app.
///
/// Mounted above the router so it can run on cold start *and* on every resume:
/// an `ACTION_VIEW` on an already-running app arrives via `onNewIntent`, which
/// brings the app to the foreground, so resume is the signal that a new file may
/// be waiting. The file is pushed through the same [pendingRouteImportProvider]
/// seam the Settings import cards use, then the activity-entry form is opened —
/// exactly what Kotlin's `ExternalRouteImportRequest` does.
class RouteImportIntentBootstrap extends ConsumerStatefulWidget {
  const RouteImportIntentBootstrap({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<RouteImportIntentBootstrap> createState() =>
      _RouteImportIntentBootstrapState();
}

class _RouteImportIntentBootstrapState
    extends ConsumerState<RouteImportIntentBootstrap> {
  AppLifecycleListener? _lifecycle;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _drain);
    // The router isn't mounted during initState; wait for the first frame so the
    // push below has somewhere to land.
    WidgetsBinding.instance.addPostFrameCallback((_) => _drain());
  }

  @override
  void dispose() {
    _lifecycle?.dispose();
    super.dispose();
  }

  Future<void> _drain() async {
    final handle =
        await ref.read(routeImportIntentChannelProvider).takePendingImport();
    if (handle == null || !mounted) return;
    ref.read(pendingRouteImportProvider.notifier).set(handle);
    ref.read(goRouterProvider).push(AppRoutes.activityEntry);
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
