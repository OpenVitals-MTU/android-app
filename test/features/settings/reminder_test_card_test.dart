import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/features/settings/presentation/cards/reminder_test_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

void main() {
  Widget harness(ReminderTestCard card) => ProviderScope(
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: Scaffold(body: SingleChildScrollView(child: card)),
        ),
      );

  testWidgets('renders the title, body and the show-reminder action',
      (tester) async {
    await tester.pumpWidget(harness(const ReminderTestCard()));
    await tester.pumpAndSettle();

    expect(find.text('Test reminders'), findsOneWidget);
    expect(find.text('Show hydration reminder'), findsOneWidget);
    expect(find.byIcon(Icons.water_drop_outlined), findsOneWidget);
  });

  testWidgets('a posted reminder confirms with a snackbar', (tester) async {
    var posts = 0;
    await tester.pumpWidget(harness(ReminderTestCard(
      showReminder: (ref) async {
        posts++;
        return true;
      },
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Show hydration reminder'));
    await tester.pumpAndSettle();

    expect(posts, 1);
    expect(find.text('Reminder posted'), findsOneWidget);
  });

  testWidgets('disabled notifications are reported, not silently swallowed',
      (tester) async {
    await tester.pumpWidget(harness(ReminderTestCard(
      showReminder: (ref) async => false,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Show hydration reminder'));
    await tester.pumpAndSettle();

    expect(
      find.text('Notifications are disabled for OpenVitals'),
      findsOneWidget,
    );
  });

  testWidgets('a failed post reports the failure', (tester) async {
    await tester.pumpWidget(harness(ReminderTestCard(
      showReminder: (ref) async => throw StateError('no plugin host'),
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Show hydration reminder'));
    await tester.pumpAndSettle();

    expect(find.text('Could not post the reminder'), findsOneWidget);
  });
}
