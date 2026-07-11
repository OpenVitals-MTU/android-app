import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:package_info_plus/package_info_plus.dart';

import 'package:openvitals/features/settings/cards/debug_diagnostics_card.dart';
import 'package:openvitals/l10n/app_localizations.dart';

void main() {
  Widget harness(DebugDiagnosticsCard card) => MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: Scaffold(body: SingleChildScrollView(child: card)),
      );

  PackageInfo fakeInfo() => PackageInfo(
        appName: 'OpenVitals',
        packageName: 'tech.mmarca.openvitals',
        version: '1.2.3',
        buildNumber: '42',
      );

  testWidgets('renders the title, body and the share + save actions',
      (tester) async {
    await tester.pumpWidget(harness(const DebugDiagnosticsCard()));
    await tester.pumpAndSettle();

    expect(find.text('Sanitized diagnostics logs'), findsOneWidget);
    expect(find.text('Share logs'), findsOneWidget);
    expect(find.text('Save logs'), findsOneWidget);

    // Kotlin puts Share above Save (top = 12.dp / top = 8.dp), with a share
    // icon on the former and the download icon on the latter.
    final shareY = tester.getTopLeft(find.text('Share logs')).dy;
    final saveY = tester.getTopLeft(find.text('Save logs')).dy;
    expect(shareY, lessThan(saveY));
    expect(find.byIcon(Icons.share_outlined), findsOneWidget);
    expect(find.byIcon(Icons.download_outlined), findsOneWidget);
  });

  testWidgets('save flow sanitizes the logcat and reports success',
      (tester) async {
    String? savedContent;
    String? savedName;
    await tester.pumpWidget(harness(DebugDiagnosticsCard(
      readLogcat: () async => const [
        'I/OpenVitalsX: kept line',
        'I/RandomTag: dropped tag',
      ],
      loadPackageInfo: () async => fakeInfo(),
      saveLogsFile: (content, name) async {
        savedContent = content;
        savedName = name;
        return true;
      },
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Save logs'));
    await tester.pumpAndSettle();

    expect(savedName, 'openvitals-diagnostics-logs.txt');
    expect(savedContent, contains('package=tech.mmarca.openvitals'));
    expect(savedContent, contains('version=1.2.3 (42)'));
    expect(savedContent, contains('I/OpenVitalsX: kept line'));
    expect(savedContent, isNot(contains('I/RandomTag: dropped tag')));
    expect(find.text('Debug logs saved'), findsOneWidget);
  });

  testWidgets('degrades gracefully when the native channel is unavailable',
      (tester) async {
    var saveCalled = false;
    await tester.pumpWidget(harness(DebugDiagnosticsCard(
      readLogcat: () async => null,
      loadPackageInfo: () async => fakeInfo(),
      saveLogsFile: (content, name) async {
        saveCalled = true;
        return true;
      },
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Save logs'));
    await tester.pumpAndSettle();

    expect(saveCalled, isFalse);
    expect(find.text('Could not save diagnostics logs'), findsOneWidget);
  });

  testWidgets('share flow sanitizes the logcat and reaches the share seam',
      (tester) async {
    String? sharedContent;
    String? chooserTitle;
    await tester.pumpWidget(harness(DebugDiagnosticsCard(
      readLogcat: () async => const [
        'I/OpenVitalsX: kept line',
        'I/RandomTag: dropped tag',
      ],
      loadPackageInfo: () async => fakeInfo(),
      shareLogsFile: (content, title) async {
        sharedContent = content;
        chooserTitle = title;
      },
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Share logs'));
    await tester.pumpAndSettle();

    expect(chooserTitle, 'Share diagnostics logs');
    expect(sharedContent, contains('package=tech.mmarca.openvitals'));
    expect(sharedContent, contains('version=1.2.3 (42)'));
    expect(sharedContent, contains('I/OpenVitalsX: kept line'));
    expect(sharedContent, isNot(contains('I/RandomTag: dropped tag')));
    // The share sheet is its own feedback — Kotlin shows no success Toast.
    expect(find.text('Could not share diagnostics logs'), findsNothing);
  });

  testWidgets('share failure surfaces the failure snackbar', (tester) async {
    await tester.pumpWidget(harness(DebugDiagnosticsCard(
      readLogcat: () async => const ['I/OpenVitalsX: kept line'],
      loadPackageInfo: () async => fakeInfo(),
      shareLogsFile: (content, title) async =>
          throw StateError('no share target'),
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Share logs'));
    await tester.pumpAndSettle();

    expect(find.text('Could not share diagnostics logs'), findsOneWidget);
  });

  testWidgets('share degrades gracefully when the native channel is missing',
      (tester) async {
    var shareCalled = false;
    await tester.pumpWidget(harness(DebugDiagnosticsCard(
      readLogcat: () async => null,
      loadPackageInfo: () async => fakeInfo(),
      shareLogsFile: (content, title) async {
        shareCalled = true;
      },
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Share logs'));
    await tester.pumpAndSettle();

    expect(shareCalled, isFalse);
    expect(find.text('Could not share diagnostics logs'), findsOneWidget);
  });
  // The card is only reachable in diagnostics-enabled builds; the gate itself is
  // covered by test/core/diagnostics/diagnostics_build_config_test.dart.
}
