import 'dart:async';
import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../../../domain/model/ble_sensor_models.dart';
import '../contract/ble_device_repository.dart';

/// Port of the Kotlin `BleDeviceRepository` — a SharedPreferences-backed sensor
/// registry (not Health Connect). The Kotlin `StateFlow` is modelled with a
/// broadcast [Stream] plus a synchronous [devices] snapshot.
class BleDeviceRepositoryImpl implements BleDeviceRepository {
  BleDeviceRepositoryImpl(this._prefs) {
    _devices = _readDevices();
  }

  static const String _key = 'ble_sensor_devices';

  final SharedPreferences _prefs;
  final StreamController<List<BleSensorDevice>> _controller =
      StreamController<List<BleSensorDevice>>.broadcast();
  List<BleSensorDevice> _devices = const [];

  @override
  Stream<List<BleSensorDevice>> get devicesStream => _controller.stream;

  @override
  List<BleSensorDevice> get devices => List.unmodifiable(_devices);

  @override
  List<BleSensorDevice> get enabledDevices =>
      _devices.where((d) => d.enabled).toList();

  @override
  void refresh() {
    _devices = _readDevices();
    _controller.add(devices);
  }

  @override
  Map<BleSensorCapability, BleSensorDevice> resolveCapabilityAssignments() {
    final assignments = <BleSensorCapability, BleSensorDevice>{};
    for (final device in enabledDevices) {
      // A watch holds no capabilities anyway, but the guard is explicit: it is
      // what stops a stored watch from ever being connected to and polled by
      // the recording coordinator.
      if (device.kind != BleDeviceKind.sensor) continue;
      for (final capability in device.capabilities) {
        assignments.putIfAbsent(capability, () => device);
      }
    }
    return assignments;
  }

  @override
  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts(
    Set<BleSensorCapability> capabilities, {
    String? excludingDeviceId,
  }) {
    final conflicts = <BleSensorCapability, BleSensorDevice>{};
    resolveCapabilityAssignments().forEach((capability, device) {
      if (capabilities.contains(capability) && device.id != excludingDeviceId) {
        conflicts[capability] = device;
      }
    });
    return conflicts;
  }

  @override
  BleSensorDevice addDevice({
    required String displayName,
    required String address,
    required String? bluetoothName,
    required Set<BleSensorCapability> capabilities,
    int? wheelCircumferenceMm,
    BleDeviceKind kind = BleDeviceKind.sensor,
  }) {
    final normalizedAddress = address.toUpperCase();
    final existing = _devices.firstWhereOrNull(
      (d) => d.address.toUpperCase() == normalizedAddress,
    );
    if (existing != null) {
      return updateDevice(
        deviceId: existing.id,
        displayName: displayName,
        capabilities: capabilities,
        enabled: true,
        wheelCircumferenceMm: wheelCircumferenceMm ?? existing.wheelCircumferenceMm,
        kind: kind,
      );
    }
    final device = BleSensorDevice(
      id: _uuid(),
      displayName: displayName,
      address: normalizedAddress,
      bluetoothName: bluetoothName,
      capabilities: capabilities,
      enabled: true,
      wheelCircumferenceMm: wheelCircumferenceMm,
      addedAt: DateTime.now().toUtc(),
      kind: kind,
    ).normalized();
    _persist([..._devices, device]);
    return device;
  }

  @override
  BleSensorDevice updateDevice({
    required String deviceId,
    String? displayName,
    Set<BleSensorCapability>? capabilities,
    bool? enabled,
    int? wheelCircumferenceMm,
    BleDeviceKind? kind,
  }) {
    final current = _devices.firstWhereOrNull((d) => d.id == deviceId);
    if (current == null) {
      throw StateError('Unknown BLE device: $deviceId');
    }
    final updated = current
        .copyWith(
          displayName: displayName ?? current.displayName,
          capabilities: capabilities ?? current.capabilities,
          enabled: enabled ?? current.enabled,
          wheelCircumferenceMm:
              wheelCircumferenceMm ?? current.wheelCircumferenceMm,
          kind: kind ?? current.kind,
        )
        .normalized();
    _persist([
      for (final d in _devices) if (d.id == deviceId) updated else d,
    ]);
    return updated;
  }

  @override
  void removeDevice(String deviceId) {
    // A watch's Garmin-specific state (synced-file history + declared
    // capabilities) is cleared separately, by whoever forgets the watch, via
    // `GarminDeviceStateStore.clear` — this registry no longer holds it.
    _persist(_devices.where((d) => d.id != deviceId).toList());
  }

  @override
  void setDeviceEnabled(String deviceId, bool enabled) {
    updateDevice(deviceId: deviceId, enabled: enabled);
  }

  @override
  void updateBatteryLevel(String deviceId, int batteryPercent) {
    final percent = batteryPercent.clamp(0, 100).toInt();
    // Kotlin persists only if the clamped value actually changed, so repeated
    // identical battery reads don't churn storage/the stream or advance
    // batteryUpdatedAt.
    final matches = _devices.where((d) => d.id == deviceId);
    if (matches.isEmpty || matches.first.batteryPercent == percent) return;
    _persist([
      for (final d in _devices)
        if (d.id == deviceId)
          d.copyWith(
            batteryPercent: percent,
            batteryUpdatedAt: DateTime.now().toUtc(),
          )
        else
          d,
    ]);
  }

  @override
  void markSynced(String deviceId, DateTime at) {
    if (!_devices.any((d) => d.id == deviceId)) return;
    _persist([
      for (final d in _devices)
        if (d.id == deviceId) d.copyWith(lastSyncedAt: at.toUtc()) else d,
    ]);
  }

  // ── Persistence ────────────────────────────────────────────────────────────

  void _persist(List<BleSensorDevice> devices) {
    _devices = devices;
    _prefs.setString(
      _key,
      jsonEncode([for (final d in devices) _toJson(d)]),
    );
    _controller.add(this.devices);
  }

  List<BleSensorDevice> _readDevices() {
    final raw = _prefs.getString(_key);
    if (raw == null || raw.isEmpty) return const [];
    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List) return const [];
      return decoded
          .whereType<Map<String, dynamic>>()
          .map(_fromJson)
          .whereType<BleSensorDevice>()
          .toList();
    } catch (_) {
      return const [];
    }
  }

  Map<String, dynamic> _toJson(BleSensorDevice device) => {
        'id': device.id,
        'displayName': device.displayName,
        'address': device.address,
        'bluetoothName': device.bluetoothName,
        'capabilities': [for (final c in device.capabilities) c.storageName],
        'enabled': device.enabled,
        'wheelCircumferenceMm': device.wheelCircumferenceMm,
        'batteryPercent': device.batteryPercent,
        'batteryUpdatedAt': device.batteryUpdatedAt?.millisecondsSinceEpoch,
        'addedAt': device.addedAt.millisecondsSinceEpoch,
        'kind': device.kind.storageName,
        'lastSyncedAt': device.lastSyncedAt?.millisecondsSinceEpoch,
      };

  BleSensorDevice? _fromJson(Map<String, dynamic> json) {
    final id = json['id'] as String?;
    final displayName = json['displayName'] as String?;
    final address = json['address'] as String?;
    final addedAtMillis = (json['addedAt'] as num?)?.toInt();
    if (id == null || displayName == null || address == null) return null;
    final capabilities = <BleSensorCapability>{
      for (final raw in (json['capabilities'] as List? ?? const []))
        ?BleSensorCapability.fromStorage(raw.toString()),
    };
    final batteryUpdatedAt = (json['batteryUpdatedAt'] as num?)?.toInt();
    final lastSyncedAt = (json['lastSyncedAt'] as num?)?.toInt();
    return BleSensorDevice(
      id: id,
      displayName: displayName,
      address: address,
      bluetoothName: json['bluetoothName'] as String?,
      capabilities: capabilities,
      enabled: json['enabled'] as bool? ?? true,
      wheelCircumferenceMm: (json['wheelCircumferenceMm'] as num?)?.toInt(),
      batteryPercent: (json['batteryPercent'] as num?)?.toInt(),
      batteryUpdatedAt: batteryUpdatedAt == null
          ? null
          : DateTime.fromMillisecondsSinceEpoch(batteryUpdatedAt, isUtc: true),
      addedAt: addedAtMillis == null
          ? DateTime.now().toUtc()
          : DateTime.fromMillisecondsSinceEpoch(addedAtMillis, isUtc: true),
      // Absent for every device stored before watches existed — those are all
      // sensors, which is exactly what the fallback says.
      kind: BleDeviceKind.fromStorage(json['kind']?.toString() ?? '') ??
          BleDeviceKind.sensor,
      lastSyncedAt: lastSyncedAt == null
          ? null
          : DateTime.fromMillisecondsSinceEpoch(lastSyncedAt, isUtc: true),
    ).normalized();
  }

  String _uuid() {
    final now = DateTime.now().microsecondsSinceEpoch;
    return 'ble-$now';
  }
}

extension _FirstWhereOrNull<E> on List<E> {
  E? firstWhereOrNull(bool Function(E) test) {
    for (final element in this) {
      if (test(element)) return element;
    }
    return null;
  }
}
