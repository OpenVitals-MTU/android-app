import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/imports/applehealth/apple_health_import_xml_support.dart';

/// Runs [chunks] through a fresh sanitizer transformer and returns the joined
/// output plus the sanitizer (for its repair counts). Splitting the input into
/// several chunks is the whole point: it exercises the cross-chunk carry that the
/// old whole-string sanitizer never had to handle.
Future<(String, AppleHealthXmlSanitizer)> _run(List<String> chunks) async {
  final sanitizer = AppleHealthXmlSanitizer();
  final out =
      await Stream.fromIterable(chunks).transform(sanitizer.transformer).join();
  return (out, sanitizer);
}

void main() {
  group('AppleHealthXmlSanitizer streaming', () {
    test('escapes a bare & split across a chunk boundary', () async {
      final (out, sanitizer) = await _run(['<a>AT&', 'T</a>']);
      expect(out, '<a>AT&amp;T</a>');
      expect(sanitizer.escapedAmpersands, 1);
    });

    test('does not re-escape a real entity split across a chunk boundary',
        () async {
      final (out, sanitizer) = await _run(['<a>x&', 'amp;y</a>']);
      expect(out, '<a>x&amp;y</a>');
      expect(sanitizer.escapedAmpersands, 0);
    });

    test('escapes a bare & at the very end of the stream', () async {
      final (out, sanitizer) = await _run(['<a>x&']);
      expect(out, '<a>x&amp;');
      expect(sanitizer.escapedAmpersands, 1);
    });

    test('numeric and hex character references split across chunks stay intact',
        () async {
      final (out, sanitizer) = await _run(['<a>&#65;&', '#x41;</a>']);
      expect(out, '<a>&#65;&#x41;</a>');
      expect(sanitizer.escapedAmpersands, 0);
    });

    test('strips a disallowed control character mid-text', () async {
      final bell = String.fromCharCode(0x07);
      final (out, sanitizer) = await _run(['<a>Notes${bell}App</a>']);
      expect(out, '<a>NotesApp</a>');
      expect(sanitizer.strippedControlChars, 1);
    });

    test('recentContext reports the trailing emitted text', () async {
      final (_, sanitizer) = await _run(['<a>hello world</a>']);
      expect(sanitizer.recentContext(), endsWith('world</a>'));
    });
  });
}
