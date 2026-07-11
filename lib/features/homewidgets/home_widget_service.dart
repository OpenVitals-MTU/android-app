import 'package:home_widget/home_widget.dart';

/// Home-screen widgets, ported from the Kotlin app.
///
/// The native side is Android Glance (`GlanceAppWidget`), living under
/// `android/app/src/main/kotlin/.../features/homewidgets/`. Those composables are
/// **render-only**: unlike Kotlin — where each widget loads from repositories via
/// Hilt on `onUpdate` — our data lives in Dart, so Dart computes a snapshot and
/// *pushes* it, and the native widget just draws the strings it finds.
///
/// **Key namespacing.** `home_widget` stores everything in one shared
/// `HomeWidgetPreferences` file, so unprefixed keys (`title`, `value`, …) would
/// collide across widgets — a readiness push would clobber a body-energy push.
/// (Kotlin has no such problem: Glance gives each widget its own datastore.)
/// Every key is therefore prefixed by [HomeWidgetId.storageKey], and additionally
/// by `appWidgetId` for the widgets that are configured per instance
/// (see [HomeWidgetId.isPerInstance]). The native composable rebuilds the same
/// prefix via `GlanceAppWidgetManager.getAppWidgetId(glanceId)`.
enum HomeWidgetId {
  /// Configurable single-metric tile; each instance shows its own metric.
  metric(
    'features.homewidgets.HomeMetricWidgetReceiver',
    storageKey: 'metric',
    isPerInstance: true,
  ),

  /// Configurable quick-add beverage (2x1, Add/Edit buttons).
  quickBeverage(
    'features.homewidgets.HomeQuickBeverageWidgetReceiver',
    storageKey: 'beverage',
    isPerInstance: true,
  ),

  /// One-tap quick-add beverage (1x1). Shares [quickBeverage]'s storage: Kotlin
  /// likewise shares one state schema and tells the two apart by provider class.
  quickBeverageOneTap(
    'features.homewidgets.HomeQuickBeverageOneTapWidgetReceiver',
    storageKey: 'beverage',
    isPerInstance: true,
  ),

  dailyReadiness(
    'features.homewidgets.HomeDailyReadinessWidgetReceiver',
    storageKey: 'daily_readiness',
  ),

  bodyEnergy(
    'features.homewidgets.HomeBodyEnergyWidgetReceiver',
    storageKey: 'body_energy',
  ),

  todayVitals(
    'features.homewidgets.HomeTodayVitalsWidgetReceiver',
    storageKey: 'today_vitals',
  );

  const HomeWidgetId(
    this.androidReceiver, {
    required this.storageKey,
    this.isPerInstance = false,
  });

  /// Package-relative class name of the Android widget receiver (matches the
  /// `AndroidManifest` `android:name`), package-qualified at push time.
  final String androidReceiver;

  /// Namespace for this widget's keys in the shared preferences file.
  final String storageKey;

  /// Whether each placed instance holds its own configuration (and so its own
  /// key namespace, suffixed with the `appWidgetId`).
  final bool isPerInstance;
}

/// The key prefix a widget's data is stored under.
///
/// [appWidgetId] is required for [HomeWidgetId.isPerInstance] widgets — without
/// it two placed instances would overwrite each other.
String homeWidgetKeyPrefix(HomeWidgetId widget, {int? appWidgetId}) {
  if (!widget.isPerInstance) return '${widget.storageKey}.';
  assert(
    appWidgetId != null,
    '${widget.name} is configured per instance and needs an appWidgetId',
  );
  return '${widget.storageKey}.$appWidgetId.';
}

/// One label/value(/subtitle) row inside a widget snapshot. Mirrors the Kotlin
/// `HomeMetricWidgetRow`.
class HomeWidgetRow {
  const HomeWidgetRow({
    required this.label,
    required this.value,
    this.subtitle = '',
  });

  final String label;
  final String value;
  final String subtitle;
}

/// The flat data a home widget renders, mirroring the Kotlin
/// `HomeMetricWidgetSnapshot`.
///
/// One schema serves every widget: the beverage widgets map their *amount* onto
/// [value] and their drink id onto [selectionId], rather than Kotlin's separate
/// `HomeQuickBeverageSnapshot`.
class HomeWidgetSnapshot {
  const HomeWidgetSnapshot({
    required this.title,
    this.value = '',
    this.unit = '',
    this.subtitle = '',
    this.route = defaultRoute,
    this.rows = const <HomeWidgetRow>[],
  });

  final String title;
  final String value;
  final String unit;
  final String subtitle;
  final String route;
  final List<HomeWidgetRow> rows;

  /// Fallback navigation target (the dashboard), matching `Screen.Dashboard`.
  static const String defaultRoute = 'dashboard';
}

/// Maximum rows persisted per widget, matching the Kotlin `MaxHomeWidgetRows`.
const int maxHomeWidgetRows = 12;

/// Pure mapper: [snapshot] → the flat `key → value` payload written to widget
/// storage, every key under [prefix].
///
/// Row layout matches the Kotlin `writeHomeWidgetSnapshot` / `HomeMetricWidgetState`
/// so the ported composables read it unchanged. Rows beyond [maxHomeWidgetRows]
/// are dropped.
///
/// [selectionId] carries the per-instance configuration — the metric id for the
/// metric widget, the drink id for the beverage widgets (Kotlin's `metric_id` /
/// `quick_beverage_drink_id`).
///
/// Values are all `String` except the `int` row count: the plugin stores a Dart
/// `double` as raw long bits, which a naive native read mangles — so numbers are
/// formatted to strings before they get here.
Map<String, Object> homeWidgetDataMap(
  HomeWidgetSnapshot snapshot, {
  required String prefix,
  String? selectionId,
}) {
  final rowCount = snapshot.rows.length > maxHomeWidgetRows
      ? maxHomeWidgetRows
      : snapshot.rows.length;
  final data = <String, Object>{
    '${prefix}selection_id': ?selectionId,
    '${prefix}title': snapshot.title,
    '${prefix}value': snapshot.value,
    '${prefix}unit': snapshot.unit,
    '${prefix}subtitle': snapshot.subtitle,
    '${prefix}route': snapshot.route,
    '${prefix}row_count': rowCount,
  };
  for (var index = 0; index < rowCount; index++) {
    final row = snapshot.rows[index];
    data['${prefix}row_${index}_label'] = row.label;
    data['${prefix}row_${index}_value'] = row.value;
    data['${prefix}row_${index}_subtitle'] = row.subtitle;
  }
  return data;
}

/// A widget instance currently placed on the home screen.
class HomeWidgetInstance {
  const HomeWidgetInstance({required this.appWidgetId, required this.className});

  final int appWidgetId;

  /// Fully-qualified Android receiver class name.
  final String className;
}

/// Thin seam over the `home_widget` plugin so [HomeWidgetService] is testable.
abstract interface class HomeWidgetClient {
  Future<void> saveWidgetData(String key, Object? value);

  /// Reads a key back out of widget storage. The configuration activity writes
  /// an instance's selection natively; Dart reads it here to know what to push.
  Future<String?> readWidgetData(String key);

  Future<void> updateWidget({String? qualifiedAndroidName, String? iOSName});

  /// The widget instances the user has actually placed. Needed to push
  /// per-instance data to each configured metric/beverage widget.
  Future<List<HomeWidgetInstance>> installedWidgets();
}

/// Default [HomeWidgetClient] backed by the real `home_widget` plugin.
class PluginHomeWidgetClient implements HomeWidgetClient {
  const PluginHomeWidgetClient();

  @override
  Future<void> saveWidgetData(String key, Object? value) =>
      HomeWidget.saveWidgetData<Object?>(key, value);

  @override
  Future<String?> readWidgetData(String key) =>
      HomeWidget.getWidgetData<String>(key);

  @override
  Future<void> updateWidget({String? qualifiedAndroidName, String? iOSName}) =>
      HomeWidget.updateWidget(
        qualifiedAndroidName: qualifiedAndroidName,
        iOSName: iOSName,
      );

  @override
  Future<List<HomeWidgetInstance>> installedWidgets() async {
    final widgets = await HomeWidget.getInstalledWidgets();
    return [
      // Both fields are Android-only and nullable; on other hosts this yields an
      // empty list, which is exactly right.
      for (final widget in widgets)
        if (widget.androidWidgetId != null && widget.androidClassName != null)
          HomeWidgetInstance(
            appWidgetId: widget.androidWidgetId!,
            className: widget.androidClassName!,
          ),
    ];
  }
}

/// Pushes snapshots to the home-screen widgets.
///
/// Ported from the Kotlin `refresh*Widget` helpers: build a snapshot, flatten it
/// to key/values, persist each, then broadcast so the OS redraws the widget.
class HomeWidgetService {
  const HomeWidgetService({
    this.client = const PluginHomeWidgetClient(),
    this.androidPackageName = 'tech.mmarca.openvitals',
  });

  final HomeWidgetClient client;
  final String androidPackageName;

  /// The receiver class name [widget] is registered under.
  ///
  /// Always fully qualified: debug builds carry `applicationIdSuffix = ".debug"`,
  /// and the plugin's unqualified lookup resolves against `context.packageName`,
  /// so it would fail to find the class in debug.
  String qualifiedReceiver(HomeWidgetId widget) =>
      '$androidPackageName.${widget.androidReceiver}';

  /// Writes [snapshot] for [widget] and triggers a redraw.
  ///
  /// [appWidgetId] is required for per-instance widgets; [selectionId] carries
  /// that instance's configuration (metric id / drink id).
  Future<void> pushSnapshot(
    HomeWidgetId widget,
    HomeWidgetSnapshot snapshot, {
    int? appWidgetId,
    String? selectionId,
  }) async {
    final data = homeWidgetDataMap(
      snapshot,
      prefix: homeWidgetKeyPrefix(widget, appWidgetId: appWidgetId),
      selectionId: selectionId,
    );
    for (final entry in data.entries) {
      await client.saveWidgetData(entry.key, entry.value);
    }
    await client.updateWidget(qualifiedAndroidName: qualifiedReceiver(widget));
  }

  /// The placed instances of [widget], so per-instance data can be pushed to each.
  Future<List<HomeWidgetInstance>> instancesOf(HomeWidgetId widget) async {
    final target = qualifiedReceiver(widget);
    final installed = await client.installedWidgets();
    return installed.where((w) => w.className == target).toList();
  }

  /// The configuration [appWidgetId] was set up with — the metric id for the
  /// metric widget, the drink id for the beverage widgets — or null while the
  /// instance is still unconfigured.
  ///
  /// This is the `selection_id` key the configuration activity persists (Kotlin
  /// keeps the same value in `HomeMetricWidgetState.metricIdKey` /
  /// `quick_beverage_drink_id`); reading it back is what lets a refresh know
  /// which metric each placed tile is showing.
  Future<String?> selectionIdOf(
    HomeWidgetId widget, {
    required int appWidgetId,
  }) =>
      client.readWidgetData(
        '${homeWidgetKeyPrefix(widget, appWidgetId: appWidgetId)}selection_id',
      );

  /// Which widget [appWidgetId] belongs to, or null when nothing is placed under
  /// that id (the instance was removed, or the host has no widgets at all).
  ///
  /// The configure launch hands Dart only an `appWidgetId`
  /// (`initiallyLaunchedFromHomeWidgetConfigure`), and the background log
  /// callback only gets one off its URI — but the two beverage widgets and the
  /// metric widget all configure through the same `MainActivity`, so the id must
  /// be resolved back to a widget *type* before anything can be shown or pushed.
  /// The receiver class name is what tells them apart, exactly as Kotlin's
  /// `isQuickBeverageOneTapWidget` checks the provider's `className`.
  Future<HomeWidgetId?> widgetOfInstance(int appWidgetId) async {
    final installed = await client.installedWidgets();
    for (final instance in installed) {
      if (instance.appWidgetId != appWidgetId) continue;
      return widgetForReceiver(instance.className);
    }
    return null;
  }

  /// The widget registered under the fully-qualified [className], or null when it
  /// is not one of ours.
  HomeWidgetId? widgetForReceiver(String className) {
    for (final widget in HomeWidgetId.values) {
      if (qualifiedReceiver(widget) == className) return widget;
    }
    return null;
  }

  /// Reads one extra per-instance key — data an instance needs that is not part
  /// of the rendered snapshot (the beverage widgets' cached drink payload).
  Future<String?> readInstanceKey(
    HomeWidgetId widget, {
    required int appWidgetId,
    required String key,
  }) =>
      client.readWidgetData(
        '${homeWidgetKeyPrefix(widget, appWidgetId: appWidgetId)}$key',
      );

  /// Writes one extra per-instance key. Does not redraw: callers pair it with a
  /// [pushSnapshot], which does.
  Future<void> saveInstanceKey(
    HomeWidgetId widget, {
    required int appWidgetId,
    required String key,
    required String value,
  }) =>
      client.saveWidgetData(
        '${homeWidgetKeyPrefix(widget, appWidgetId: appWidgetId)}$key',
        value,
      );

  /// Records what [appWidgetId] was configured with, without pushing a snapshot.
  ///
  /// The configuration screen writes this *before* it loads any data, so that a
  /// failed (or slow) load cannot lose the user's choice: the selection is the
  /// only thing a later refresh needs to bring the tile up to date.
  Future<void> saveSelectionId(
    HomeWidgetId widget, {
    required int appWidgetId,
    required String selectionId,
  }) =>
      client.saveWidgetData(
        '${homeWidgetKeyPrefix(widget, appWidgetId: appWidgetId)}selection_id',
        selectionId,
      );
}
