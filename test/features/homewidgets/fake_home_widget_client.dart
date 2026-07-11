import 'package:openvitals/data/repository/contract/health_repository.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/features/homewidgets/home_widget_service.dart';

/// In-memory [HomeWidgetClient]: records what a push wrote and which receivers
/// were told to redraw, and serves the widget instances the test placed.
class FakeHomeWidgetClient implements HomeWidgetClient {
  FakeHomeWidgetClient({
    this.installed = const [],
    Map<String, String>? stored,
  }) : saved = {...?stored};

  /// Everything written, and everything pre-seeded (a configured instance's
  /// `selection_id`, which the native configuration activity persists).
  final Map<String, Object?> saved;

  /// The qualified receiver of each `updateWidget` call, in order.
  final List<String?> updated = [];

  /// Every subtitle ever written, in order — [saved] only keeps the last one.
  /// The beverage widgets push a *sequence* ("Saved now", then back to "Tap to
  /// log"), which is the thing worth asserting on.
  final List<String> subtitles = [];

  final List<HomeWidgetInstance> installed;

  @override
  Future<void> saveWidgetData(String key, Object? value) async {
    saved[key] = value;
    if (key.endsWith('.subtitle') && value is String) subtitles.add(value);
  }

  @override
  Future<String?> readWidgetData(String key) async => saved[key] as String?;

  @override
  Future<void> updateWidget({
    String? qualifiedAndroidName,
    String? iOSName,
  }) async =>
      updated.add(qualifiedAndroidName);

  @override
  Future<List<HomeWidgetInstance>> installedWidgets() async => installed;
}

/// A [HealthRepository] that only answers the one call the refresher makes.
///
/// [refreshCalls] is what pins the on-device bug this guards against: the
/// data source starts at `notSupported` and reports *no* granted permissions
/// until availability is resolved, so a refresh that skips this renders every
/// widget as "Grant permission in OpenVitals".
class FakeHealthRepository implements HealthRepository {
  FakeHealthRepository({
    this.resolved = HealthConnectAvailability.available,
  });

  /// What [refreshAvailability] resolves to (the interface already has an
  /// `availability()` method, so this cannot be called `availability`).
  final HealthConnectAvailability resolved;
  int refreshCalls = 0;

  @override
  Future<HealthConnectAvailability> refreshAvailability() async {
    refreshCalls++;
    return resolved;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) =>
      throw UnimplementedError('${invocation.memberName} not stubbed');
}
