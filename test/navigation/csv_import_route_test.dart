import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/navigation/app_router.dart';
import 'package:openvitals/navigation/app_routes.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// The CSV importer is reached by a pushed route rather than a settings card,
/// and its route is registered by `_dataImportRoutes()` — a sibling list, because
/// the settings-section routes are generated from the enum and have nowhere to
/// hang a child. That indirection is exactly what a missing registration would
/// hide, so it is asserted here.
void main() {
  Future<GoRouter> buildRouter() async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);
    return container.read(goRouterProvider);
  }

  test('the CSV import path sits under the data-import section', () {
    expect(
      AppRoutes.settingsCsvImport,
      startsWith(AppRoutes.settingsDataImport),
    );
    expect(AppRoutes.settingsCsvImport, '/settings/data_import/csv');
  });

  test('the router has a route registered for the CSV importer', () async {
    final router = await buildRouter();

    final paths = <String>[];
    void collect(List<RouteBase> routes) {
      for (final route in routes) {
        if (route is GoRoute) paths.add(route.path);
        collect(route.routes);
      }
    }

    collect(router.configuration.routes);

    expect(paths, contains(AppRoutes.settingsCsvImport));
  });
}
