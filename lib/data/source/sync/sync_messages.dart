/// Typed payloads carried inside [SyncFrame]s, with their (de)serialization.
///
/// Small control messages (Hello, Auth, BatchAck, Abort) are compact JSON.
/// Record [SyncBatch]es — the only large payloads — are JSON then gzipped with
/// the `archive` package, since a batch of health records compresses well and
/// the link is slow.
library;

import 'dart:convert';
import 'dart:typed_data';

import 'package:archive/archive.dart';

/// The protocol version both peers announce in [SyncHello]. Bump on any wire
/// change; the session refuses a peer on a different major version.
const int kSyncProtocolVersion = 1;

/// One record to sync: an opaque serialized [payload] plus the deterministic
/// content [key] used for dedup, tagged with its Health Connect [recordType]
/// (e.g. `StepsRecord`) so the receiver can group and route it.
///
/// The protocol treats [payload] as opaque bytes — the health-record encoding
/// (Phase 3) fills it in. Dedup is entirely by [key].
class SyncItem {
  const SyncItem({
    required this.key,
    required this.recordType,
    required this.payload,
  });

  final String key;
  final String recordType;
  final Uint8List payload;

  Map<String, Object?> toJson() => {
        'k': key,
        't': recordType,
        'p': base64.encode(payload),
      };

  factory SyncItem.fromJson(Map<String, Object?> json) => SyncItem(
        key: json['k']! as String,
        recordType: json['t']! as String,
        payload: base64.decode(json['p']! as String),
      );
}

/// The opening capability + nonce exchange. Each peer sends one.
class SyncHello {
  const SyncHello({
    required this.protocolVersion,
    required this.deviceName,
    required this.hcProviderVersion,
    required this.supportedTypes,
    required this.nonce,
  });

  final int protocolVersion;
  final String deviceName;

  /// The peer's installed Health Connect provider version code, or null if
  /// unknown. Informational — surfaced in the report, not used for gating.
  final int? hcProviderVersion;

  /// Record types this device + its HC provider support (already filtered
  /// through `filterSupportedPermissions`). The syncable set is the intersection
  /// of both peers' lists.
  final List<String> supportedTypes;

  /// This peer's 256-bit session nonce.
  final Uint8List nonce;

  Uint8List encode() => _jsonToBytes({
        'v': protocolVersion,
        'name': deviceName,
        'hc': hcProviderVersion,
        'types': supportedTypes,
        'nonce': base64.encode(nonce),
      });

  factory SyncHello.decode(Uint8List bytes) {
    final json = _bytesToJson(bytes);
    return SyncHello(
      protocolVersion: json['v']! as int,
      deviceName: json['name']! as String,
      hcProviderVersion: json['hc'] as int?,
      supportedTypes:
          (json['types']! as List).map((e) => e as String).toList(),
      nonce: base64.decode(json['nonce']! as String),
    );
  }
}

/// The authentication proof (HMAC over the peer's nonce). One per peer.
class SyncAuthProof {
  const SyncAuthProof(this.proof);

  final Uint8List proof;

  Uint8List encode() => _jsonToBytes({'proof': base64.encode(proof)});

  factory SyncAuthProof.decode(Uint8List bytes) =>
      SyncAuthProof(base64.decode(_bytesToJson(bytes)['proof']! as String));
}

/// A gzipped batch of records flowing one direction, tagged with a monotonic
/// [seq] the receiver echoes in a [SyncBatchAck].
class SyncBatch {
  const SyncBatch({required this.seq, required this.items});

  final int seq;
  final List<SyncItem> items;

  Uint8List encode() {
    final json = jsonEncode({
      'seq': seq,
      'items': [for (final item in items) item.toJson()],
    });
    return GZipEncoder().encodeBytes(utf8.encode(json));
  }

  factory SyncBatch.decode(Uint8List bytes) {
    final json = jsonDecode(utf8.decode(GZipDecoder().decodeBytes(bytes)))
        as Map<String, Object?>;
    return SyncBatch(
      seq: json['seq']! as int,
      items: [
        for (final raw in json['items']! as List)
          SyncItem.fromJson(raw as Map<String, Object?>),
      ],
    );
  }
}

/// Acknowledges the batch with the given [seq] — the stop-and-wait signal the
/// sender waits for before sending the next batch.
class SyncBatchAck {
  const SyncBatchAck(this.seq);

  final int seq;

  Uint8List encode() => _jsonToBytes({'seq': seq});

  factory SyncBatchAck.decode(Uint8List bytes) =>
      SyncBatchAck(_bytesToJson(bytes)['seq']! as int);
}

/// A cooperative abort with a human-readable [reason] for the report.
class SyncAbort {
  const SyncAbort(this.reason);

  final String reason;

  Uint8List encode() => _jsonToBytes({'reason': reason});

  factory SyncAbort.decode(Uint8List bytes) =>
      SyncAbort(_bytesToJson(bytes)['reason'] as String? ?? 'unknown');
}

Uint8List _jsonToBytes(Object? json) =>
    Uint8List.fromList(utf8.encode(jsonEncode(json)));

Map<String, Object?> _bytesToJson(Uint8List bytes) =>
    jsonDecode(utf8.decode(bytes)) as Map<String, Object?>;
