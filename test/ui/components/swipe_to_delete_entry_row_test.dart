import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/swipe_to_delete_entry_row.dart';

Widget _harness(Widget child) => MaterialApp(
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: ListView(children: [child])),
    );

void main() {
  testWidgets('end-to-start swipe deletes', (tester) async {
    var deleted = 0;
    await tester.pumpWidget(_harness(
      SwipeToDeleteEntryRow(
        key: const ValueKey('entry-1'),
        onDelete: () => deleted++,
        child: const ListTile(title: Text('Morning reading')),
      ),
    ));

    await tester.drag(find.text('Morning reading'), const Offset(-500, 0));
    await tester.pumpAndSettle();

    expect(deleted, 1);
    expect(find.text('Morning reading'), findsNothing);
  });

  testWidgets('start-to-end swipe does nothing (Kotlin disables it)',
      (tester) async {
    var deleted = 0;
    await tester.pumpWidget(_harness(
      SwipeToDeleteEntryRow(
        key: const ValueKey('entry-1'),
        onDelete: () => deleted++,
        child: const ListTile(title: Text('Morning reading')),
      ),
    ));

    await tester.drag(find.text('Morning reading'), const Offset(500, 0));
    await tester.pumpAndSettle();

    expect(deleted, 0);
    expect(find.text('Morning reading'), findsOneWidget);
  });
}
