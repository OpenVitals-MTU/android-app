import 'dart:async';

import 'package:intl/date_symbol_data_local.dart';

/// Applies to every test under `test/` except the golden subtree, which has its
/// own `flutter_test_config.dart`.
///
/// Loads date-formatting symbols for all locales. The app does this in `main()`,
/// but tests never run `main()`. Since the app now points `Intl.defaultLocale` at
/// the resolved locale (see `app.dart`), a widget test that resolves a non-`en`
/// locale would otherwise throw `UninitializedLocaleData` from the first
/// `DateFormat` — which is exactly what happened on CI, whose container locale
/// resolves to a non-`en` value (locally it resolved to `en`, whose data is
/// available by default, so it passed and masked the gap).
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  await initializeDateFormatting();
  await testMain();
}
