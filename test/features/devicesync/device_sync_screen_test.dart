import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/devicesync/presentation/device_sync_screen.dart';
import 'package:openvitals/l10n/app_localizations.dart';

Widget _bootstrap() => const ProviderScope(
      child: MaterialApp(
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: DeviceSyncScreen(),
      ),
    );

void main() {
  testWidgets('role step offers host and guest options', (tester) async {
    await tester.pumpWidget(_bootstrap());
    await tester.pump();

    expect(find.text('Sync with another phone'), findsWidgets);
    expect(find.text('Make this phone discoverable'), findsOneWidget);
    expect(find.text('Find a phone to sync with'), findsOneWidget);
    // The privacy reassurance is present.
    expect(
      find.textContaining('no internet permission'),
      findsOneWidget,
    );
  });
}
