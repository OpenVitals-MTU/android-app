/// Pairing and session authentication for phone-to-phone sync.
///
/// WHAT THE 6-DIGIT CODE ACTUALLY BUYS
/// -----------------------------------
/// The confidentiality and MITM resistance of the channel come from **Bluetooth
/// bonding** (Secure Simple Pairing), which encrypts the RFCOMM link at the OS
/// level. The app-layer 6-digit code shown on the host and typed on the guest is
/// a *mutual-confirmation + anti-mixup* token: it proves both phones are the two
/// the users are looking at, and it fails the session before any health data
/// moves if the user picked the wrong device or fat-fingered the code.
///
/// Because the link is already bonded-encrypted, an attacker cannot observe the
/// handshake to brute-force the low-entropy (10^6) code offline — so app-layer
/// HMAC over the code is adequate here. It is NOT an independent PAKE and does
/// not by itself defend an unbonded link. (If the code itself must become
/// cryptographically load-bearing, a real SPAKE2/X25519 exchange via the
/// `cryptography` package is the documented upgrade path; out of scope for v1.)
///
/// SCHEME
/// ------
/// Each side generates a 256-bit nonce and exchanges it in the Hello frame.
/// Both derive the same session key regardless of who computes it by fixing the
/// nonce order to (host, guest):
///
///   sessionKey = HMAC-SHA256(key = utf8(code), msg = hostNonce ‖ guestNonce)
///
/// Each side then proves it holds the session key with a challenge bound to the
/// PEER's nonce (so a proof can't be replayed for a different nonce):
///
///   proof(forChallengeNonce) = HMAC-SHA256(key = sessionKey,
///                                          msg = "ov-sync-auth-v1" ‖ challengeNonce)
///
/// A side sends `proof(peerNonce)` and verifies the received proof against
/// `proof(ownNonce)`. A wrong code yields a different sessionKey on the two
/// phones, so the proofs mismatch and the session aborts.
library;

import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';

/// Length of the per-session nonce, in bytes (256-bit).
const int kSyncNonceBytes = 32;

/// Domain-separation label mixed into every auth proof. Bump the suffix if the
/// proof construction ever changes so old and new clients can't cross-validate.
const String _authContext = 'ov-sync-auth-v1';

/// Number of digits in the human-checked pairing code.
const int kPairingCodeDigits = 6;

/// Generates a fresh random nonce. Injectable [random] (default [Random.secure])
/// so tests can be deterministic; production MUST use a secure RNG.
Uint8List generateSyncNonce([Random? random]) {
  final rng = random ?? Random.secure();
  final bytes = Uint8List(kSyncNonceBytes);
  for (var i = 0; i < bytes.length; i++) {
    bytes[i] = rng.nextInt(256);
  }
  return bytes;
}

/// Generates a zero-padded [kPairingCodeDigits]-digit pairing code, e.g.
/// `"042913"`. Injectable [random] for tests.
String generatePairingCode([Random? random]) {
  final rng = random ?? Random.secure();
  final buffer = StringBuffer();
  for (var i = 0; i < kPairingCodeDigits; i++) {
    buffer.write(rng.nextInt(10));
  }
  return buffer.toString();
}

/// Derives the shared session key from the [code] and both nonces. The nonce
/// order is fixed (host first, guest second) so both phones compute an identical
/// key no matter which one calls this.
Uint8List deriveSessionKey({
  required String code,
  required Uint8List hostNonce,
  required Uint8List guestNonce,
}) {
  final hmac = Hmac(sha256, utf8.encode(code));
  final message = Uint8List(hostNonce.length + guestNonce.length)
    ..setRange(0, hostNonce.length, hostNonce)
    ..setRange(hostNonce.length, hostNonce.length + guestNonce.length, guestNonce);
  return Uint8List.fromList(hmac.convert(message).bytes);
}

/// Computes the auth proof over [challengeNonce] under [sessionKey]. To
/// authenticate to the peer, pass the PEER's nonce; to verify the peer's proof,
/// recompute this over your OWN nonce and compare.
Uint8List computeAuthProof({
  required Uint8List sessionKey,
  required Uint8List challengeNonce,
}) {
  final hmac = Hmac(sha256, sessionKey);
  final prefix = utf8.encode(_authContext);
  final message = Uint8List(prefix.length + challengeNonce.length)
    ..setRange(0, prefix.length, prefix)
    ..setRange(prefix.length, prefix.length + challengeNonce.length, challengeNonce);
  return Uint8List.fromList(hmac.convert(message).bytes);
}

/// Constant-time byte comparison — avoids leaking, via early-exit timing, how
/// many leading bytes of a proof matched.
bool constantTimeEquals(List<int> a, List<int> b) {
  if (a.length != b.length) return false;
  var diff = 0;
  for (var i = 0; i < a.length; i++) {
    diff |= a[i] ^ b[i];
  }
  return diff == 0;
}
