import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/diagnostics/debug_log_sanitizer.dart';

void main() {
  group('DebugLogSanitizer.sanitizeLogLine', () {
    test('redacts MAC, email and UUID in an allowed-tag line', () {
      const line =
          'I/OpenVitalsBle: peer AA:BB:CC:DD:EE:FF user alice@example.com '
          'session 12345678-1234-1234-1234-123456789abc done';
      final result = DebugLogSanitizer.sanitizeLogLine(line);
      expect(result, isNotNull);
      expect(result, contains('[redacted]'));
      expect(result, isNot(contains('AA:BB:CC:DD:EE:FF')));
      expect(result, isNot(contains('alice@example.com')));
      expect(result, isNot(contains('12345678-1234-1234-1234-123456789abc')));
      // Level/tag prefix is preserved.
      expect(result, startsWith('I/OpenVitalsBle: '));
    });

    test('redacts key=value identifiers keeping the key', () {
      const line = 'D/SettingsViewModel: sync token=supersecretvalue ok';
      // " token" is not a drop keyword here because the message is
      // "sync token=..." → contains " token" → dropped. Use a non-dropped key.
      expect(DebugLogSanitizer.sanitizeLogLine(line), isNull);

      const safe = 'D/SettingsViewModel: sync deviceId=abc123 ok';
      final result = DebugLogSanitizer.sanitizeLogLine(safe);
      expect(result, 'D/SettingsViewModel: sync deviceId=[redacted] ok');
    });

    test('drops a line containing a location keyword', () {
      const line = 'I/OpenVitalsX: current location update received';
      expect(DebugLogSanitizer.sanitizeLogLine(line), isNull);
    });

    test('drops a line containing a token keyword', () {
      const line = 'I/OpenVitalsX: refreshed token successfully';
      expect(DebugLogSanitizer.sanitizeLogLine(line), isNull);
    });

    test('keeps AppleHealthImporter E/W/A/F lines verbatim (unsanitized)', () {
      const line =
          'E/AppleHealthImporter: raw payload lat=1.23 email x@y.com failed';
      // Contains drop keywords AND PII, but the exception keeps it untouched.
      expect(DebugLogSanitizer.sanitizeLogLine(line), line);
    });

    test('sanitizes AppleHealthImporter non-W/E/A/F lines normally', () {
      // Level I is not in {W,E,A,F}; AppleHealthImporter is not an allowed tag
      // by the general rules, so an I line is dropped.
      const line = 'I/AppleHealthImporter: informational line';
      expect(DebugLogSanitizer.sanitizeLogLine(line), isNull);
    });

    test('passes an allowed tag through', () {
      const line = 'I/SettingsViewModel: hello world';
      expect(DebugLogSanitizer.sanitizeLogLine(line), line);
    });

    test('drops a non-allowed tag', () {
      const line = 'I/RandomThirdPartyTag: hello world';
      expect(DebugLogSanitizer.sanitizeLogLine(line), isNull);
    });

    test('drops a non-log-format line', () {
      expect(DebugLogSanitizer.sanitizeLogLine('not a logcat line'), isNull);
    });

    test('drops a blank message', () {
      expect(DebugLogSanitizer.sanitizeLogLine('I/OpenVitalsX:   '), isNull);
    });

    test('redacts ISO instants and dates', () {
      const line = 'I/OpenVitalsX: at 2024-01-02T03:04:05Z on 2024-01-02';
      final result = DebugLogSanitizer.sanitizeLogLine(line);
      expect(result, isNot(contains('2024-01-02')));
      expect(result, contains('[redacted]'));
    });
  });

  group('DebugLogSanitizer.sanitizeLogcat', () {
    test('caps output at maxLines keeping the most recent lines', () {
      final lines = [
        for (var i = 0; i < DebugLogSanitizer.maxLines + 100; i++)
          'I/OpenVitalsX: line $i',
      ];
      final result = DebugLogSanitizer.sanitizeLogcat(lines);
      expect(result.writtenLines, DebugLogSanitizer.maxLines);
      expect(result.lines.length, DebugLogSanitizer.maxLines);
      expect(result.droppedLines, 0);
      // takeLast semantics: the first 100 lines are dropped from the front.
      expect(result.lines.first, 'I/OpenVitalsX: line 100');
      expect(result.lines.last,
          'I/OpenVitalsX: line ${DebugLogSanitizer.maxLines + 99}');
    });

    test('counts dropped lines across the whole input', () {
      final lines = [
        'I/OpenVitalsX: kept one',
        'I/RandomTag: dropped tag',
        'garbage',
        'I/OpenVitalsX: location dropped keyword',
        'I/OpenVitalsX: kept two',
      ];
      final result = DebugLogSanitizer.sanitizeLogcat(lines);
      expect(result.writtenLines, 2);
      expect(result.droppedLines, 3);
      expect(result.lines, [
        'I/OpenVitalsX: kept one',
        'I/OpenVitalsX: kept two',
      ]);
    });
  });

  group('DebugLogSanitizer.buildExportText', () {
    test('emits the header block then the sanitized lines', () {
      final text = DebugLogSanitizer.buildExportText(
        packageName: 'tech.mmarca.openvitals',
        versionName: '1.2.3',
        versionCode: 42,
        rawLines: const [
          'I/OpenVitalsX: kept',
          'I/RandomTag: dropped',
        ],
      );
      expect(text, contains('OpenVitals diagnostics log export'));
      expect(text, contains('package=tech.mmarca.openvitals'));
      expect(text, contains('version=1.2.3 (42)'));
      expect(text, contains('writtenLines=1'));
      expect(text, contains('droppedLines=1'));
      expect(
        text,
        contains('AppleHealthImporter W/E/A/F lines are unsanitized'),
      );
      expect(text, contains('I/OpenVitalsX: kept'));
      expect(text, isNot(contains('I/RandomTag: dropped')));
    });
  });
}
