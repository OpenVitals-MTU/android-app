import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// Real fonts, for this directory only.
///
/// Widget tests render text in the `FlutterTest` font, whose every glyph is an
/// identical filled box. That is perfectly deterministic — it would make stable
/// goldens — and useless for reviewing them: you cannot see that an axis label
/// clipped instead of wrapping, you cannot see the tabular figures or the weight
/// the theme asks for, and the "what did the restyle change?" diff goes blind at
/// exactly the places text lives. Every chart in this app has axis labels, bar
/// labels or a stat row.
///
/// So the golden suite loads the real Roboto — the font the app resolves to on
/// Android — and the icons with it.
///
/// `flutter_test_config.dart` applies to the tests in ITS OWN directory subtree
/// and no further, which is the whole reason this file lives in `test/goldens/`
/// rather than at `test/`. The other ~1900 tests keep the box font and are not
/// touched by any of this.
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  TestWidgetsFlutterBinding.ensureInitialized();
  await _loadFont('Roboto', const [
    'test/fonts/Roboto-Regular.ttf',
    'test/fonts/Roboto-Medium.ttf',
    'test/fonts/Roboto-Bold.ttf',
  ]);
  // Without this every Icon renders as a blank box — the empty states and the
  // chart headers use them.
  await _loadFont('MaterialIcons', const [
    'test/fonts/MaterialIcons-Regular.otf',
  ]);
  return testMain();
}

Future<void> _loadFont(String family, List<String> paths) async {
  final loader = FontLoader(family);
  for (final path in paths) {
    loader.addFont(
      File(path).readAsBytes().then((bytes) => ByteData.sublistView(bytes)),
    );
  }
  await loader.load();
}
