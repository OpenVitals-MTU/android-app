/// Streaming XML hardening + character repair for the Apple Health export parser,
/// ported from the Kotlin `XmlCharacterSanitizingReader` in
/// `AppleHealthImportXmlSupport.kt`.
///
/// Apple's exporter occasionally emits the two things that most often break XML
/// 1.0 well-formedness in free-text fields: raw control characters and bare `&`
/// that were never escaped. [AppleHealthXmlSanitizer] repairs both **as a stream
/// transformer** — so the whole multi-gigabyte document is never held in memory —
/// and counts them so the report and the parse-error message can mention the
/// repairs.
///
/// Unlike the retired whole-string `sanitizeAppleHealthXml`, the DOCTYPE is NOT
/// stripped: the `package:xml` event parser tolerates Apple's
/// `<!DOCTYPE HealthData [ … ]>` (it emits a doctype event the handler ignores),
/// and stripping it would need unbounded cross-chunk state for the internal subset.
library;

import 'dart:async';

const int _maxEntityLookahead = 12;
const int _maxContextChars = 200;
const int _ampersand = 0x26;
const int _semicolon = 0x3B;
const int _hash = 0x23;
const Set<String> _namedXmlEntities = {'amp', 'lt', 'gt', 'quot', 'apos'};

/// The classification of a just-seen `&`: it begins a valid entity (pass it
/// through), it does not (escape to `&amp;`), or there is not yet enough lookahead
/// in the current chunk to tell (defer to the next chunk).
enum _EntityAhead { isEntity, notEntity, needMore }

/// A stateful, chunk-boundary-safe port of the Kotlin `XmlCharacterSanitizingReader`.
///
/// Wire it into a byte/char stream with `stream.transform(sanitizer.transformer)`.
/// [strippedControlChars] / [escapedAmpersands] are valid once the stream has
/// drained; [recentContext] returns the last [_maxContextChars] emitted characters
/// (non-printables escaped) for the parse-error message — the streaming analogue of
/// the old whole-string `substring` around the failure position.
class AppleHealthXmlSanitizer {
  int strippedControlChars = 0;
  int escapedAmpersands = 0;

  /// A deferred tail — always beginning at a `&` whose entity lookahead ran past
  /// the end of the previous chunk — prepended to the next chunk.
  String _pending = '';

  /// Rolling window of the most recently emitted characters, trimmed to
  /// [_maxContextChars]. Maintained per chunk (not per character) so it adds no
  /// per-character cost to a multi-gigabyte parse.
  String _context = '';

  StreamTransformer<String, String> get transformer =>
      StreamTransformer<String, String>.fromHandlers(
        handleData: (chunk, sink) {
          final out = _process(chunk, isLast: false);
          if (out.isNotEmpty) sink.add(out);
        },
        handleDone: (sink) {
          final out = _flushPending();
          if (out.isNotEmpty) sink.add(out);
          sink.close();
        },
        handleError: (error, stackTrace, sink) =>
            sink.addError(error, stackTrace),
      );

  /// Approximate text the parser last consumed before failing (Kotlin
  /// `recentContext`), with non-printable characters shown as `\uXXXX`.
  String recentContext() {
    final buffer = StringBuffer();
    for (final code in _context.codeUnits) {
      buffer.write(_toDisplayable(code));
    }
    return buffer.toString();
  }

  String _process(String chunk, {required bool isLast}) {
    final input = _pending.isEmpty ? chunk : '$_pending$chunk';
    _pending = '';
    final out = StringBuffer();
    final length = input.length;
    var index = 0;
    while (index < length) {
      final code = input.codeUnitAt(index);
      if (_isDisallowedXmlChar(code)) {
        strippedControlChars++;
        index++;
        continue;
      }
      if (code == _ampersand) {
        final decision = _entityReferenceAhead(input, index + 1, isLast);
        if (decision == _EntityAhead.needMore) {
          // Not enough lookahead in this chunk to classify the `&`; carry it (and
          // its partial lookahead) to the next chunk and stop here.
          _pending = input.substring(index);
          break;
        }
        if (decision == _EntityAhead.notEntity) {
          escapedAmpersands++;
          out.write('&amp;');
        } else {
          out.writeCharCode(_ampersand);
        }
        index++;
        continue;
      }
      out.writeCharCode(code);
      index++;
    }
    final produced = out.toString();
    _appendContext(produced);
    return produced;
  }

  String _flushPending() {
    if (_pending.isEmpty) return '';
    final tail = _pending;
    _pending = '';
    // End of stream: no more lookahead will ever arrive, so decide now.
    return _process(tail, isLast: true);
  }

  void _appendContext(String produced) {
    if (produced.isEmpty) return;
    final combined = _context.isEmpty ? produced : '$_context$produced';
    _context = combined.length <= _maxContextChars
        ? combined
        : combined.substring(combined.length - _maxContextChars);
  }

  /// Peeks past a just-read `&` (starting at [start]) to classify it, mirroring the
  /// Kotlin `isEntityReferenceAhead` 12-char lookahead. Returns
  /// [_EntityAhead.needMore] when the lookahead runs off the end of the current
  /// chunk and more input may still follow ([isLast] false).
  _EntityAhead _entityReferenceAhead(String input, int start, bool isLast) {
    final length = input.length;
    final body = StringBuffer();
    var terminated = false;
    var index = start;
    while (body.length < _maxEntityLookahead) {
      if (index >= length) {
        if (!isLast) return _EntityAhead.needMore;
        break;
      }
      final code = input.codeUnitAt(index);
      body.writeCharCode(code);
      index++;
      if (code == _semicolon) {
        terminated = true;
        break;
      }
      if (code != _hash && !_isLetterOrDigit(code)) break;
    }
    final isEntity = terminated && _isValidXmlEntityBody(body.toString());
    return isEntity ? _EntityAhead.isEntity : _EntityAhead.notEntity;
  }
}

bool _isDisallowedXmlChar(int code) =>
    (code >= 0x00 && code <= 0x08) ||
    code == 0x0B ||
    code == 0x0C ||
    (code >= 0x0E && code <= 0x1F) ||
    code == 0xFFFE ||
    code == 0xFFFF;

bool _isLetterOrDigit(int code) =>
    (code >= 0x30 && code <= 0x39) || // 0-9
    (code >= 0x41 && code <= 0x5A) || // A-Z
    (code >= 0x61 && code <= 0x7A); // a-z

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

/// Kotlin `Char.toDisplayable`: printable ASCII (plus `\n`/`\t`) verbatim,
/// everything else as `\uXXXX`.
String _toDisplayable(int code) {
  if ((code >= 0x20 && code <= 0x7E) || code == 0x0A || code == 0x09) {
    return String.fromCharCode(code);
  }
  return '\\u${code.toRadixString(16).padLeft(4, '0')}';
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
