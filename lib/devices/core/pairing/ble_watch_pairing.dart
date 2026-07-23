import 'package:bluetooth_sync_native/bluetooth_sync_native.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

import 'watch_pairing_port.dart';

/// [WatchPairingPort] over the two platform layers it actually takes.
///
/// The split is not a design choice, it is what the platform offers: bonding is
/// `flutter_blue_plus` (`createBond` / `removeBond` / `bondState`, Android-only),
/// while CompanionDeviceManager has no Flutter plugin at all and comes from the
/// app's own `bluetooth_sync_native`. The domain sees one port.
class BleWatchPairing implements WatchPairingPort {
  BleWatchPairing({BluetoothSyncHostApi? companionApi})
      : _companionApi = companionApi ?? BluetoothSyncNative().api;

  final BluetoothSyncHostApi _companionApi;

  /// `createBond` requires an open connection — Android will not bond with a
  /// device it is not talking to. Generous, because the user has to find the
  /// watch, wake it and confirm a six-digit code on its screen.
  static const Duration _connectTimeout = Duration(seconds: 20);
  static const int _bondTimeoutSeconds = 90;

  @override
  Future<WatchBondResult> bond(String address) async {
    if (!await FlutterBluePlus.isSupported) {
      return WatchBondResult.unreachable;
    }
    final device = BluetoothDevice.fromId(address);

    // Checked BEFORE connecting: a bonded watch needs no dialog, and dialing it
    // only to hang up again wastes seconds the user spends staring at a spinner.
    // `bondState` is a stream whose first event is the current state.
    try {
      if (await device.bondState.first == BluetoothBondState.bonded) {
        return WatchBondResult.alreadyBonded;
      }
    } catch (error) {
      // Android-only API, or the adapter is off. Fall through and let the
      // connect attempt below produce the real answer.
      debugPrint('BleWatchPairing bondState probe failed: $error');
    }

    try {
      await device.connect(
        license: License.nonprofit,
        timeout: _connectTimeout,
      );
    } catch (error) {
      debugPrint('BleWatchPairing connect failed: $error');
      return WatchBondResult.unreachable;
    }

    try {
      await device.createBond(timeout: _bondTimeoutSeconds);
      return WatchBondResult.bonded;
    } catch (error) {
      // flutter_blue_plus throws for BOTH a declined dialog and a timeout, with
      // no way to tell them apart. They mean the same thing to the caller — no
      // bond, so no onboarding — so both are 'refused'.
      debugPrint('BleWatchPairing createBond failed: $error');
      return WatchBondResult.refused;
    } finally {
      // The bond outlives the connection; the sync opens its own later. Holding
      // this one would keep a GATT link open for nothing.
      try {
        await device.disconnect();
      } catch (_) {
        // Already gone.
      }
    }
  }

  @override
  Future<void> removeBond(String address) async {
    if (!await FlutterBluePlus.isSupported) return;
    try {
      await BluetoothDevice.fromId(address).removeBond();
    } catch (error) {
      // Forgetting a watch must not fail because the OS had no bond to drop.
      debugPrint('BleWatchPairing removeBond failed: $error');
    }
  }

  @override
  Future<bool> associateCompanion(String address, String? displayName) async {
    if (defaultTargetPlatform != TargetPlatform.android) return false;
    try {
      return await _companionApi.associateCompanionDevice(address, displayName);
    } catch (error) {
      // Optional by contract — see WatchPairingPort.associateCompanion.
      debugPrint('BleWatchPairing associateCompanion failed: $error');
      return false;
    }
  }

  @override
  Future<void> disassociateCompanion(String address) async {
    if (defaultTargetPlatform != TargetPlatform.android) return;
    try {
      await _companionApi.disassociateCompanionDevice(address);
    } catch (error) {
      debugPrint('BleWatchPairing disassociateCompanion failed: $error');
    }
  }
}
