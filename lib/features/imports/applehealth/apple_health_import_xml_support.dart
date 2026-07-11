/// XML hardening + character repair for the Apple Health export parser, ported
/// from the Kotlin `AppleHealthImportXmlSupport.kt`.
///
/// Apple's exporter occasionally emits the two things that most often break XML
/// 1.0 well-formedness in free-text fields: raw control characters and bare `&`
/// that were never escaped. [sanitizeAppleHealthXml] repairs both in place (and
/// counts them) before the streaming parser sees the document. DTD declarations
/// are stripped so no external grammar is loaded (matching the Kotlin hardened
/// SAX factory).
library;

const int _maxEntityLookahead = 12;
const Set<String> _namedXmlEntities = {'amp', 'lt', 'gt', 'quot', 'apos'};

class SanitizedAppleHealthXml {
  const SanitizedAppleHealthXml({
    required this.text,
    required this.strippedControlChars,
    required this.escapedAmpersands,
  });

  final String text;
  final int strippedControlChars;
  final int escapedAmpersands;
}

SanitizedAppleHealthXml sanitizeAppleHealthXml(String input) {
  final buffer = StringBuffer();
  var strippedControlChars = 0;
  var escapedAmpersands = 0;
  final length = input.length;
  var index = 0;
  while (index < length) {
    final code = input.codeUnitAt(index);
    if (_isDisallowedXmlChar(code)) {
      strippedControlChars++;
      index++;
      continue;
    }
    if (code == 0x26 /* & */ && !_isEntityReferenceAhead(input, index + 1)) {
      escapedAmpersands++;
      buffer.write('&amp;');
      index++;
      continue;
    }
    buffer.writeCharCode(code);
    index++;
  }
  return SanitizedAppleHealthXml(
    text: _stripDoctype(buffer.toString()),
    strippedControlChars: strippedControlChars,
    escapedAmpersands: escapedAmpersands,
  );
}

bool _isDisallowedXmlChar(int code) =>
    (code >= 0x00 && code <= 0x08) ||
    code == 0x0B ||
    code == 0x0C ||
    (code >= 0x0E && code <= 0x1F) ||
    code == 0xFFFE ||
    code == 0xFFFF;

bool _isEntityReferenceAhead(String input, int start) {
  final builder = StringBuffer();
  var terminated = false;
  var index = start;
  while (builder.length < _maxEntityLookahead && index < input.length) {
    final ch = input[index];
    index++;
    builder.write(ch);
    if (ch == ';') {
      terminated = true;
      break;
    }
    if (ch != '#' && !_isLetterOrDigit(ch)) break;
  }
  return terminated && _isValidXmlEntityBody(builder.toString());
}

bool _isLetterOrDigit(String ch) {
  final code = ch.codeUnitAt(0);
  return (code >= 0x30 && code <= 0x39) || // 0-9
      (code >= 0x41 && code <= 0x5A) || // A-Z
      (code >= 0x61 && code <= 0x7A); // a-z
}

bool _isValidXmlEntityBody(String value) {
  final body = value.endsWith(';') ? value.substring(0, value.length - 1) : value;
  if (_namedXmlEntities.contains(body)) return true;
  if (body.startsWith('#x') || body.startsWith('#X')) {
    final rest = body.substring(2);
    return rest.isNotEmpty &&
        rest.split('').every((ch) {
          final c = ch.codeUnitAt(0);
          return (c >= 0x30 && c <= 0x39) ||
              (c >= 0x61 && c <= 0x66) ||
              (c >= 0x41 && c <= 0x46);
        });
  }
  if (body.startsWith('#')) {
    final rest = body.substring(1);
    return rest.isNotEmpty &&
        rest.split('').every((ch) {
          final c = ch.codeUnitAt(0);
          return c >= 0x30 && c <= 0x39;
        });
  }
  return false;
}

/// Removes a leading `<!DOCTYPE ...>` declaration (including any internal `[...]`
/// subset) so the parser never loads DTD grammar.
String _stripDoctype(String input) {
  final startTag = input.indexOf('<!DOCTYPE');
  if (startTag < 0) return input;
  var index = startTag + '<!DOCTYPE'.length;
  var depth = 0;
  while (index < input.length) {
    final ch = input[index];
    if (ch == '[') {
      depth++;
    } else if (ch == ']') {
      if (depth > 0) depth--;
    } else if (ch == '>' && depth == 0) {
      return input.substring(0, startTag) + input.substring(index + 1);
    }
    index++;
  }
  return input.substring(0, startTag);
}

/// Thrown when export.xml still fails to parse after character sanitization,
/// carrying the trailing text the parser saw (Kotlin `AppleHealthXmlParseException`).
class AppleHealthXmlParseException implements Exception {
  AppleHealthXmlParseException(this.message);

  final String message;

  @override
  String toString() => message;
}

/// Thrown when the export ZIP ends part-way through an entry (Kotlin
/// `AppleHealthZipReadException`). Damage inside a `workout-routes/*.gpx` entry
/// is recovered from by the parser once `export.xml` has been read; damage
/// anywhere else is fatal and surfaces this message to the user.
class AppleHealthZipReadException implements Exception {
  AppleHealthZipReadException({this.entryName, this.decompressedBytesRead});

  final String? entryName;
  final int? decompressedBytesRead;

  String get message {
    final buffer = StringBuffer('Apple Health export.zip ended unexpectedly');
    final entry = entryName;
    if (entry != null && entry.trim().isNotEmpty) {
      buffer.write(' while reading $entry');
    }
    if (decompressedBytesRead != null) {
      buffer.write(' after $decompressedBytesRead decompressed byte(s)');
    }
    buffer.write(
      '. The selected ZIP is likely incomplete, corrupt, not fully downloaded, '
      'or the platform stopped providing the document stream. Re-copy or '
      're-export the Apple Health ZIP, make sure it is stored locally on the '
      'phone, or extract export.xml and import that file directly.',
    );
    return buffer.toString();
  }

  @override
  String toString() => message;
}

String buildAppleHealthXmlParseMessage({
  required String location,
  required String causeMessage,
  required String recentContext,
  required int strippedControlChars,
  required int escapedAmpersands,
}) {
  final repaired = (strippedControlChars > 0 || escapedAmpersands > 0)
      ? ' (already auto-repaired $strippedControlChars control character(s) and '
          '$escapedAmpersands unescaped \'&\' earlier in the file)'
      : '';
  return 'Apple Health export.xml is not well-formed at $location: $causeMessage. '
      'Text leading up to the error: "$recentContext"$repaired';
}
