import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/l10n/app_localizations.dart';
import 'package:openvitals/ui/components/permission_callout.dart';

Future<void> _pump(WidgetTester tester, Widget child) async {
  await tester.pumpWidget(MaterialApp(
    localizationsDelegates: AppLocalizations.localizationsDelegates,
    supportedLocales: AppLocalizations.supportedLocales,
    home: Scaffold(body: child),
  ));
  await tester.pumpAndSettle();
  expect(tester.takeException(), isNull);
}

void main() {
  testWidgets('PermissionCallout shows title, body and grant action',
      (tester) async {
    var granted = false;
    await _pump(
      tester,
      PermissionCallout(
        title: 'Steps access',
        body: 'Grant access to read your steps.',
        onGrant: () => granted = true,
      ),
    );
    expect(find.text('Steps access'), findsOneWidget);
    expect(find.text('Grant access to read your steps.'), findsOneWidget);
    expect(find.text('Not now'), findsNothing);

    await tester.tap(find.text('Grant permission'));
    await tester.pump();
    expect(granted, isTrue);
  });

  testWidgets('PermissionCallout shows dismiss when provided', (tester) async {
    var dismissed = false;
    await _pump(
      tester,
      PermissionCallout(
        title: 'Steps access',
        body: 'Grant access to read your steps.',
        actionLabel: 'Allow',
        onGrant: () {},
        onDismiss: () => dismissed = true,
      ),
    );
    expect(find.text('Allow'), findsOneWidget);
    await tester.tap(find.text('Not now'));
    await tester.pump();
    expect(dismissed, isTrue);
  });
}
