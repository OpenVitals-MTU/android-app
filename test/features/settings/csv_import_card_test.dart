import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/settings/presentation/cards/csv_import_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/navigation/app_routes.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  Future<void> pump(
    WidgetTester tester, {
    void Function(BuildContext context)? onNavigate,
  }) async {
    SharedPreferences.setMockInitialValues(const <String, Object>{});
    final prefs = await SharedPreferences.getInstance();
    final container = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(container.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(
            body: SingleChildScrollView(
              child: CsvImportCard(onNavigate: onNavigate),
            ),
          ),
        ),
      ),
    );
    await tester.pump();
  }

  testWidgets('the card names the importer and what it is for', (tester) async {
    await pump(tester);

    expect(find.text('CSV Importer'), findsOneWidget);
    expect(find.text('Import a CSV file'), findsOneWidget);
  });

  testWidgets('tapping the action opens the CSV import route', (tester) async {
    String? pushed;
    await pump(
      tester,
      // The seam stands in for GoRouter, which has no router above this card.
      onNavigate: (_) => pushed = AppRoutes.settingsCsvImport,
    );

    await tester.tap(find.text('Import a CSV file'));
    await tester.pump();

    expect(pushed, '/settings/data_import/csv');
  });
}
