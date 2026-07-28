import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:openvitals/features/imports/csv/application/csv_import_view_model.dart';
import 'package:openvitals/features/imports/csv/csv_import_models.dart';
import 'package:openvitals/features/imports/csv/presentation/csv_import_result_view.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Done must leave the importer.
///
/// The earlier widget tests pumped [CsvImportResultView] as a MaterialApp home,
/// where there is no route to pop — so a Done button that did nothing looked
/// identical to one that worked. This pushes a real go_router route, which is
/// how the screen is actually reached.
void main() {
  Future<GoRouter> pumpPushed(WidgetTester tester) async {
    final router = GoRouter(
      initialLocation: '/settings/data_import',
      routes: [
        GoRoute(
          path: '/settings/data_import',
          builder: (context, state) => Scaffold(
            body: Center(
              child: ElevatedButton(
                onPressed: () => context.push('/settings/data_import/csv'),
                child: const Text('open importer'),
              ),
            ),
          ),
        ),
        GoRoute(
          path: '/settings/data_import/csv',
          builder: (context, state) => const Scaffold(
            body: CsvImportResultView(),
          ),
        ),
      ],
    );

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          routerConfig: router,
        ),
      ),
    );
    await tester.pump();
    return router;
  }

  testWidgets('tapping Done leaves the importer and returns to Data Importers',
      (tester) async {
    final router = await pumpPushed(tester);

    // Seed a finished run so the result view renders its buttons.
    final container = ProviderScope.containerOf(
      tester.element(find.text('open importer')),
    );
    container.read(csvImportProvider.notifier).state =
        const CsvImportState(step: CsvImportStep.done).copyWith(
      result: const CsvImportResult(
        outcome: CsvImportOutcome.completed,
        progress: CsvImportProgress(rowsRead: 3, written: 3),
      ),
    );

    await tester.tap(find.text('open importer'));
    await tester.pumpAndSettle();
    expect(find.text('Done'), findsOneWidget);

    await tester.tap(find.text('Done'));
    await tester.pumpAndSettle();

    expect(
      router.routerDelegate.currentConfiguration.uri.path,
      '/settings/data_import',
      reason: 'Done should pop the importer route',
    );
    expect(find.text('open importer'), findsOneWidget);
  });
}
