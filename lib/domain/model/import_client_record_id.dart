/// The deterministic `clientRecordId` scheme every importer shares.
///
/// Health Connect dedups — and **upserts** — on `Metadata.clientRecordId`, so
/// this string is the identity of an imported record. Two importers that can
/// produce the same id for different measurements would silently overwrite each
/// other, which is why the namespace is a required parameter rather than a
/// constant: `apple_health_…` and `csv_…` can never collide.
///
/// Extracted from `buildStableClientRecordId` in
/// `lib/features/imports/applehealth/apple_health_import_conversion_support.dart`,
/// which hardcoded `apple_health_`. The byte-for-byte output for that namespace
/// is load-bearing across the Kotlin→Flutter migration (see
/// `appleInstantToStableString`), so the hashing, the truncation to 16 bytes and
/// the segment slug must not change — `test/domain/model/import_client_record_id_test.dart`
/// pins it against ids captured before the extraction.
library;

import 'dart:convert';

import 'package:crypto/crypto.dart';

const String _hexDigits = '0123456789abcdef';

final RegExp _stableIdSegmentRegex = RegExp(r'[^a-z0-9]+');

/// Slugifies [value] for use as the middle segment of an id: lowercased, every
/// run of non-alphanumerics collapsed to `_`, no leading or trailing `_`.
///
/// Empty input becomes `record` rather than an empty segment, so an id always
/// has three parts.
String toStableIdSegment(String value) {
  final segment = value
      .toLowerCase()
      .replaceAll(_stableIdSegmentRegex, '_')
      .replaceAll(RegExp(r'^_+|_+$'), '');
  return segment.isEmpty ? 'record' : segment;
}

/// `<namespace>_<slug of prefix>_<first 16 bytes of sha256(parts) as hex>`.
///
/// [parts] is stringified with `toString()`, so callers pass whatever value
/// identifies the record — typically a `List` whose `toString()` is stable.
/// Whatever goes in here IS the record's identity: include a field and a change
/// to it creates a new record, omit it and a change to it overwrites the old one.
String buildImportClientRecordId(
  String namespace,
  String prefix,
  Object parts,
) {
  final bytes = sha256.convert(utf8.encode(parts.toString())).bytes;
  final buffer = StringBuffer();
  for (var index = 0; index < 16; index++) {
    final byte = bytes[index] & 0xFF;
    buffer.write(_hexDigits[byte >> 4]);
    buffer.write(_hexDigits[byte & 0x0F]);
  }
  return '${toStableIdSegment(namespace)}_${toStableIdSegment(prefix)}_$buffer';
}
