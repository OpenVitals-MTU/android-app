import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/reminders/reminder_notifications.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/homewidgets/home_widget_configure.dart';
import 'package:openvitals/features/homewidgets/home_widget_exact_alarm_gate.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// Answers the two permission calls with canned values and counts the request
/// launches, without a platform channel.
class _FakePermissions implements ReminderNotificationPermissions {
  _FakePermissions({required this.exactGranted, this.grantOnRequest = false});

  bool exactGranted;
  final bool grantOnRequest;
  int requests = 0;

  @override
  Future<bool> canScheduleExact() async => exactGranted;

  @override
  Future<bool> requestExactAlarms() async {
    requests += 1;
    if (grantOnRequest) exactGranted = true;
    return exactGranted;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeConfigureChannel implements HomeWidgetConfigureChannel {
  final List<int> finished = [];

  @override
  Future<void> finish(int appWidgetId) async => finished.add(appWidgetId);
}

void main() {
  Future<void> pumpGate(
    WidgetTester tester, {
    required _FakePermissions permissions,
    required _FakeConfigureChannel channel,
    required List<int> rearms,
  }) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          reminderNotificationPermissionsProvider.overrideWithValue(permissions),
          homeWidgetConfigureChannelProvider.overrideWithValue(channel),
        ],
        child: MaterialApp(
          localizationsDelegates: AppLocalizations.localizationsDelegates,
          supportedLocales: AppLocalizations.supportedLocales,
          home: HomeWidgetExactAlarmGateScreen(
            appWidgetId: 42,
            rearmRefresh: () async => rearms.add(42),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('a granted permission finishes invisibly', (tester) async {
    final channel = _FakeConfigureChannel();
    await pumpGate(
      tester,
      permissions: _FakePermissions(exactGranted: true),
      channel: channel,
      rearms: [],
    );

    // The launcher keeps the widget; the user never saw a prompt.
    expect(channel.finished, [42]);
    expect(find.text('Allow'), findsNothing);
  });

  testWidgets('"Not now" keeps the widget: RESULT_OK, never a cancel',
      (tester) async {
    final channel = _FakeConfigureChannel();
    await pumpGate(
      tester,
      permissions: _FakePermissions(exactGranted: false),
      channel: channel,
      rearms: [],
    );

    expect(find.text('Keep widgets up to date'), findsOneWidget);
    expect(channel.finished, isEmpty);

    await tester.tap(find.text('Not now'));
    await tester.pumpAndSettle();
    expect(channel.finished, [42]);
  });

  testWidgets('"Allow" that grants re-arms the refresh chain and finishes',
      (tester) async {
    final channel = _FakeConfigureChannel();
    final permissions =
        _FakePermissions(exactGranted: false, grantOnRequest: true);
    final rearms = <int>[];
    await pumpGate(
      tester,
      permissions: permissions,
      channel: channel,
      rearms: rearms,
    );

    await tester.tap(find.text('Allow'));
    await tester.pumpAndSettle();

    expect(permissions.requests, 1);
    // The chain switches to exact NOW, not on its next natural fire.
    expect(rearms, [42]);
    expect(channel.finished, [42]);
  });
}
