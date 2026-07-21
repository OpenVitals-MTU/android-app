import 'dart:math';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/sync/sync_pairing.dart';

void main() {
  group('generatePairingCode', () {
    test('is always six digits, zero-padded', () {
      // A seeded RNG whose first draws are 0 exercises the padding.
      final code = generatePairingCode(Random(0));
      expect(code, hasLength(kPairingCodeDigits));
      expect(RegExp(r'^\d{6}$').hasMatch(code), isTrue);
    });
  });

  group('generateSyncNonce', () {
    test('returns 32 bytes', () {
      expect(generateSyncNonce(Random(1)), hasLength(kSyncNonceBytes));
    });
  });

  group('deriveSessionKey', () {
    final hostNonce = Uint8List.fromList(List.filled(kSyncNonceBytes, 0xA1));
    final guestNonce = Uint8List.fromList(List.filled(kSyncNonceBytes, 0xB2));

    test('both phones derive the same key from the same inputs', () {
      final onHost = deriveSessionKey(
        code: '042913',
        hostNonce: hostNonce,
        guestNonce: guestNonce,
      );
      final onGuest = deriveSessionKey(
        code: '042913',
        hostNonce: hostNonce,
        guestNonce: guestNonce,
      );
      expect(onHost, onGuest);
      expect(onHost, hasLength(32));
    });

    test('a different code yields a different key', () {
      final right = deriveSessionKey(
        code: '042913',
        hostNonce: hostNonce,
        guestNonce: guestNonce,
      );
      final wrong = deriveSessionKey(
        code: '999999',
        hostNonce: hostNonce,
        guestNonce: guestNonce,
      );
      expect(constantTimeEquals(right, wrong), isFalse);
    });

    test('nonce order is fixed, so host/guest roles agree', () {
      // Swapping which arg is "host" changes the key — proving the order is
      // load-bearing and both sides must agree on who is host.
      final ab = deriveSessionKey(
        code: '111111',
        hostNonce: hostNonce,
        guestNonce: guestNonce,
      );
      final ba = deriveSessionKey(
        code: '111111',
        hostNonce: guestNonce,
        guestNonce: hostNonce,
      );
      expect(constantTimeEquals(ab, ba), isFalse);
    });
  });

  group('auth proof exchange', () {
    final hostNonce = generateSyncNonce(Random(2));
    final guestNonce = generateSyncNonce(Random(3));

    Uint8List keyFor(String code) => deriveSessionKey(
          code: code,
          hostNonce: hostNonce,
          guestNonce: guestNonce,
        );

    test('matching codes: each side verifies the peer proof', () {
      final hostKey = keyFor('424242');
      final guestKey = keyFor('424242');

      // Host authenticates over the guest's nonce with the host role; guest
      // verifies over its own nonce using the host role.
      final hostProof = computeAuthProof(
          sessionKey: hostKey,
          challengeNonce: guestNonce,
          roleByte: kAuthRoleHost);
      final guestExpectsHost = computeAuthProof(
          sessionKey: guestKey,
          challengeNonce: guestNonce,
          roleByte: kAuthRoleHost);
      expect(constantTimeEquals(hostProof, guestExpectsHost), isTrue);

      // And symmetrically, with the guest role.
      final guestProof = computeAuthProof(
          sessionKey: guestKey,
          challengeNonce: hostNonce,
          roleByte: kAuthRoleGuest);
      final hostExpectsGuest = computeAuthProof(
          sessionKey: hostKey,
          challengeNonce: hostNonce,
          roleByte: kAuthRoleGuest);
      expect(constantTimeEquals(guestProof, hostExpectsGuest), isTrue);
    });

    test('a reflected proof does not validate (role binding)', () {
      final key = keyFor('424242');
      // The attacker echoes the host's own proof back. Under a role-less scheme
      // this validated; now the host expects a GUEST-role proof over its nonce,
      // which the reflected host-role proof over the guest nonce is not.
      final hostProof = computeAuthProof(
          sessionKey: key, challengeNonce: guestNonce, roleByte: kAuthRoleHost);
      final hostExpectsGuest = computeAuthProof(
          sessionKey: key, challengeNonce: hostNonce, roleByte: kAuthRoleGuest);
      expect(constantTimeEquals(hostProof, hostExpectsGuest), isFalse);
    });

    test('wrong code on the guest fails verification', () {
      final hostKey = keyFor('424242');
      final guestKey = keyFor('000000'); // user mistyped

      final hostProof = computeAuthProof(
          sessionKey: hostKey,
          challengeNonce: guestNonce,
          roleByte: kAuthRoleHost);
      final guestExpectsHost = computeAuthProof(
          sessionKey: guestKey,
          challengeNonce: guestNonce,
          roleByte: kAuthRoleHost);
      expect(constantTimeEquals(hostProof, guestExpectsHost), isFalse);
    });
  });

  group('constantTimeEquals', () {
    test('true only for identical byte lists', () {
      expect(constantTimeEquals([1, 2, 3], [1, 2, 3]), isTrue);
      expect(constantTimeEquals([1, 2, 3], [1, 2, 4]), isFalse);
      expect(constantTimeEquals([1, 2], [1, 2, 3]), isFalse);
    });
  });
}
