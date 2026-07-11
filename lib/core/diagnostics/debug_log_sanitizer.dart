/// Pure-Dart port of the Kotlin `PrivacySafeDebugLogExporter` privacy filter
/// (`core/diagnostics/PrivacySafeDebugLogExporter.kt`).
///
/// Deliberately free of any Flutter dependency so the whole sanitizer is
/// unit-testable in isolation: the platform side only hands over the raw logcat
/// lines and the package/version metadata; every privacy decision (tag
/// allow-listing, keyword dropping, redaction, the `AppleHealthImporter`
/// exception, the [maxLines] cap and the header block) happens here, 1:1 with
/// the Kotlin source of truth.
library;

/// Result of sanitizing a batch of logcat lines. Mirrors the Kotlin
/// `SanitizedLogcat` data class.
class SanitizedLogcat {
  const SanitizedLogcat({
    required this.lines,
    required this.writtenLines,
    required this.droppedLines,
  });

  final List<String> lines;
  final int writtenLines;
  final int droppedLines;
}

/// The privacy filter. All members are static (Kotlin `object`).
class DebugLogSanitizer {
  const DebugLogSanitizer._();

  /// Kotlin `MaxLines` — only the most recent [maxLines] kept lines are written.
  static const int maxLines = 2000;
  static const String _redacted = '[redacted]';
  static const String _unsanitizedAppleImporterTag = 'AppleHealthImporter';

  // Kotlin `logLinePattern`: `^([VDIWEAF])/([A-Za-z0-9_.-]+)\s*:\s*(.*)$`
  // (the `-v tag` logcat format: `LEVEL/Tag: message`).
  static final RegExp _logLinePattern =
      RegExp(r'^([VDIWEAF])/([A-Za-z0-9_.-]+)\s*:\s*(.*)$');
  static final RegExp _macAddressPattern =
      RegExp(r'\b[0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5}\b');
  static final RegExp _uuidPattern = RegExp(
    r'\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b',
  );
  static final RegExp _emailPattern = RegExp(
    r'\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b',
    caseSensitive: false,
  );
  static final RegExp _phonePattern =
      RegExp(r'(?<!\w)\+?[0-9][0-9 .()\-]{7,}[0-9](?!\w)');
  static final RegExp _uriPattern = RegExp(
    r'\b(?:content|file|https?)://\S+',
    caseSensitive: false,
  );
  static final RegExp _isoInstantPattern =
      RegExp(r'\b\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z?\b');
  static final RegExp _isoDatePattern = RegExp(r'\b\d{4}-\d{2}-\d{2}\b');
  static final RegExp _userPathPattern =
      RegExp(r'/(?:storage/emulated/\d+|sdcard|data/user/\d+)/\S+');
  // Kotlin `keyValueIdPattern` uses inline `(?i)`; Dart has no inline flag, so
  // the equivalent case-insensitivity is set on the RegExp itself.
  static final RegExp _keyValueIdPattern = RegExp(
    r'\b(clientRecordId|recordId|deviceId|widgetId|token|secret|password|api[_-]?key)=\S+',
    caseSensitive: false,
  );

  static const Set<String> _unsanitizedAppleImporterLevels = {'W', 'E', 'A', 'F'};

  // Kotlin `dropKeywords` — verbatim (leading spaces included).
  static const List<String> _dropKeywords = [
    ' latitude',
    ' longitude',
    ' lat=',
    ' lon=',
    ' lng=',
    ' location',
    ' polyline',
    ' raw ',
    ' payload',
    ' content://',
    ' file://',
    ' /storage/',
    ' /sdcard/',
    ' displayname',
    ' bluetoothname',
    ' devicename',
    ' token',
    ' password',
    ' secret',
    ' api_key',
    ' apikey',
  ];

  // Kotlin `explicitAllowedTags`.
  static const Set<String> _explicitAllowedTags = {
    'BleGattConnection',
    'BodyHealthReader',
    'HealthConnectManager',
    'HomeWidget',
    'HydrationHealthReader',
    'HydrationReminderAlarmManager',
    'HydrationReminderController',
    'MindfulnessReminderAlarmManager',
    'MindfulnessReminderController',
    'SettingsViewModel',
  };

  /// Kotlin `sanitizeLogcat`: sanitize every line, dropping those that return
  /// null, count the drops over the whole input, then keep only the last
  /// [maxLines] survivors.
  static SanitizedLogcat sanitizeLogcat(List<String> lines) {
    var dropped = 0;
    final kept = <String>[];
    for (final line in lines) {
      final sanitized = sanitizeLogLine(line);
      if (sanitized == null) {
        dropped += 1;
      } else {
        kept.add(sanitized);
      }
    }
    final capped =
        kept.length > maxLines ? kept.sublist(kept.length - maxLines) : kept;
    return SanitizedLogcat(
      lines: capped,
      writtenLines: capped.length,
      droppedLines: dropped,
    );
  }

  /// Kotlin `sanitizeLogLine`: returns the redacted `LEVEL/Tag: message` line,
  /// or null when the line is dropped. `AppleHealthImporter` W/E/A/F lines are
  /// returned verbatim (unsanitized) — the single documented exception.
  static String? sanitizeLogLine(String line) {
    final trimmed = line.trim();
    final match = _logLinePattern.firstMatch(trimmed);
    if (match == null) return null;
    final level = match.group(1)!;
    final tag = match.group(2)!;
    final message = match.group(3)!;
    if (_isUnsanitizedAppleImporterLine(level, tag)) return trimmed;
    if (!_isAllowedTag(tag)) return null;
    if (message.trim().isEmpty) return null;
    if (_shouldDrop(message)) return null;

    var redacted = message
        .replaceAll(_uriPattern, _redacted)
        .replaceAll(_userPathPattern, _redacted)
        .replaceAll(_emailPattern, _redacted)
        .replaceAll(_phonePattern, _redacted)
        .replaceAll(_macAddressPattern, _redacted)
        .replaceAll(_uuidPattern, _redacted)
        .replaceAllMapped(
          _keyValueIdPattern,
          (m) => '${m.group(1)}=$_redacted',
        )
        .replaceAll(_isoInstantPattern, _redacted)
        .replaceAll(_isoDatePattern, _redacted);
    if (redacted.length > 800) {
      redacted = redacted.substring(0, 800);
    }

    return '$level/$tag: $redacted';
  }

  /// Kotlin `currentProcessLogcatPayload` text builder: the header block
  /// (package / version / privacy note / writtenLines / droppedLines) followed
  /// by a blank line and the sanitized lines.
  static String buildExportText({
    required String packageName,
    required String versionName,
    required int versionCode,
    required List<String> rawLines,
  }) {
    final sanitized = sanitizeLogcat(rawLines);
    final buffer = StringBuffer()
      ..writeln('OpenVitals diagnostics log export')
      ..writeln('package=$packageName')
      ..writeln('version=$versionName ($versionCode)')
      ..writeln(
        'privacy=only app log tags are included; sensitive lines are dropped '
        'or redacted; AppleHealthImporter W/E/A/F lines are unsanitized',
      )
      ..writeln('writtenLines=${sanitized.writtenLines}')
      ..writeln('droppedLines=${sanitized.droppedLines}')
      ..writeln();
    for (final line in sanitized.lines) {
      buffer.writeln(line);
    }
    return buffer.toString();
  }

  static bool _isAllowedTag(String tag) =>
      tag.startsWith('OpenVitals') ||
      tag.startsWith('HealthConnect') ||
      tag.endsWith('Repository') ||
      tag.endsWith('ViewModel') ||
      _explicitAllowedTags.contains(tag);

  static bool _isUnsanitizedAppleImporterLine(String level, String tag) =>
      tag == _unsanitizedAppleImporterTag &&
      _unsanitizedAppleImporterLevels.contains(level);

  static bool _shouldDrop(String message) {
    final lower = ' ${message.toLowerCase()} ';
    return _dropKeywords.any(lower.contains);
  }
}
