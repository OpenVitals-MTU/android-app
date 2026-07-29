import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/sync_report.dart';
import 'package:openvitals/features/devicesync/application/device_sync_view_model.dart';
import 'package:openvitals/features/devicesync/presentation/device_sync_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

/// A view-model that simply renders a fixed state, so a widget test can pin the
/// screen to any wizard step.
class _FakeVm extends DeviceSyncViewModel {
  _FakeVm(this._initial);
  final DeviceSyncState _initial;
  @override
  DeviceSyncState build() => _initial;
}

Widget _bootstrap([DeviceSyncState? state]) => ProviderScope(
      overrides: [
        if (state != null) deviceSyncProvider.overrideWith(() => _FakeVm(state)),
      ],
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: const DeviceSyncScreen(),
      ),
    );

void main() {
  testWidgets('role step offers host and guest options', (tester) async {
    await tester.pumpWidget(_bootstrap());
    await tester.pump();

    expect(find.text('Sync with another phone'), findsWidgets);
    expect(find.text('Make this phone discoverable'), findsOneWidget);
    expect(find.text('Find a phone to sync with'), findsOneWidget);
    expect(find.textContaining('no internet permission'), findsOneWidget);
  });

  testWidgets('the report step shows a failure, not a success checkmark',
      (tester) async {
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(
          step: DeviceSyncStep.report, errorMessage: 'sync_failed'),
    ));
    await tester.pump();

    // The failure heading + mapped message render; the "imported N" success
    // heading and its checkmark do not.
    expect(find.text("Sync didn't finish"), findsOneWidget);
    expect(find.textContaining('could not be completed'), findsOneWidget);
    expect(find.byIcon(Icons.task_alt), findsNothing);
  });

  testWidgets('a finished sync offers to share the report, not only save it',
      (tester) async {
    // Saving on Android has no picker and lands in app documents out of reach,
    // so sharing is what actually gets the report off the phone — to a
    // messenger or an email.
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(
        step: DeviceSyncStep.report,
        reportText: 'OpenVitals sync report\nImported: 4\n',
        report: SyncReport(
          completed: true,
          peerDeviceName: 'Pixel',
          negotiatedTypes: ['StepsRecord'],
          itemsSent: 0,
          itemsReceived: 4,
          imported: 4,
          duplicateSkipped: 0,
          typeSummaries: [],
        ),
      ),
    ));
    await tester.pump();

    expect(find.text('Copy report'), findsOneWidget);
    expect(find.text('Save report'), findsOneWidget);
    expect(find.text('Share report'), findsOneWidget);
  });

  testWidgets('a connect timeout surfaces a connection message', (tester) async {
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(
          step: DeviceSyncStep.report, errorMessage: 'connect_timeout'),
    ));
    await tester.pump();

    expect(find.textContaining('Could not connect'), findsOneWidget);
  });

  testWidgets('the role step renders a permission error banner', (tester) async {
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(errorMessage: 'permission_denied'),
    ));
    await tester.pump();

    expect(find.textContaining('Bluetooth permission is needed'), findsOneWidget);
  });

  testWidgets('the scan step shows a spinner while scanning', (tester) async {
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(
          step: DeviceSyncStep.guestScanning, scanning: true),
    ));
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsWidgets);
    expect(find.text('Scan again'), findsNothing);
  });

  testWidgets('a finished scan with no devices offers a rescan', (tester) async {
    await tester.pumpWidget(_bootstrap(
      const DeviceSyncState(
          step: DeviceSyncStep.guestScanning, scanning: false),
    ));
    await tester.pump();

    expect(find.textContaining('No phones found'), findsOneWidget);
    expect(find.text('Scan again'), findsOneWidget);
  });
}
