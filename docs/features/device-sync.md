# Sync With Another Phone

OpenVitals can copy Health Connect records directly between two Android phones
over **Bluetooth**, with no account and no network. It is reached from
**Settings → Sync with another phone** (`/settings/device_sync`).

## Why Bluetooth, not Wi-Fi

The app ships **no `android.permission.INTERNET`** — a property stated in the
Play listing, [privacy.md](../app/privacy.md), and
[local-and-connected-editions.md](../app/local-and-connected-editions.md). On
Android every TCP/UDP socket (including Wi-Fi Direct and Wi-Fi Aware) requires
INTERNET, so a Wi-Fi transfer would break that guarantee. **Bluetooth Classic
RFCOMM** needs no INTERNET permission, so the transfer stays peer-to-peer with no
network. A year of dense records gzips to a few megabytes — about ten seconds
over RFCOMM — so the speed gap versus Wi-Fi does not matter in practice.

`flutter_blue_plus` (the app's BLE-sensor dependency) is central-only and cannot
advertise or open a server socket, so the RFCOMM transport is a small app-local
native plugin, `packages/bluetooth_sync_native/`, modelled on
`packages/health_connect_native/`. It moves raw bytes only.

## The flow

1. **Choose a role.** One phone taps *Make this phone discoverable* (host); the
   other taps *Find a phone* (guest).
2. **Pair.** The host shows a 6-digit code and waits. The guest scans, taps the
   host, and types the code.
3. **Pick how far back** to sync (30 days / 6 months / 1 year / everything).
4. **Pick data types.** Defaults to every type both phones support.
5. **Sync.** Both phones exchange records, dedupe, and write only what they were
   missing (a bidirectional merge).
6. **Report.** Each phone shows what it imported and skipped, like the Apple
   Health import report.

## What the 6-digit code protects

The encryption and MITM resistance of the link come from **Bluetooth bonding**
(Secure Simple Pairing), which the OS performs on first connection. The app-layer
6-digit code is a **mutual-confirmation + anti-mixup** token: both phones derive a
session key from it (`HMAC-SHA256(code, hostNonce ‖ guestNonce)`) and prove
knowledge of it with a challenge bound to the peer's nonce, so a wrong code (wrong
phone, or a typo) fails the session before any health data moves. Because the link
is already bonded-encrypted, the low entropy of the code is acceptable — an
attacker cannot observe the handshake to brute-force it offline. It is not an
independent PAKE; if the code itself ever needs to be cryptographically
load-bearing, a SPAKE2/X25519 exchange (via the `cryptography` package) is the
documented upgrade.

## Deduplication

Records read natively from Health Connect have a per-device HC id and usually a
null `clientRecordId`, so dedup keys on **content**, not identity. Each record is
hashed into a `sync_<hex>` fingerprint (the same construction the Apple Health
importer uses, including the whole-second instant formatting that prevents
re-import duplicates). Both phones compute the same fingerprint for the same
logical record, received records are written under that fingerprint as their
`clientRecordId`, and Health Connect upserts on it — so **re-running a sync writes
nothing new** and the two phones converge.

## Implementation

- **Transport (native):** `packages/bluetooth_sync_native/` — Pigeon host API
  (discoverable, discovery, RFCOMM server/client, `sendBytes`) + Flutter API
  events (`onDeviceDiscovered`, `onConnectionStateChanged`, `onBytesReceived`).
  Needs `BLUETOOTH_ADVERTISE` (added to the host manifest) plus the existing
  `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`.
- **Protocol (pure Dart):** `lib/data/source/sync/` — `sync_frame` (length-prefixed
  framing), `sync_pairing` (session key + proofs), `sync_messages` (gzipped
  batches), `sync_session` (the symmetric handshake → auth → bidirectional
  exchange state machine), `sync_transport` (the `SyncByteTransport` seam + an
  in-memory pipe for tests). Fully unit-tested with no Bluetooth.
- **Health Connect bridge:** `import_record_sync_codec` (fingerprint + record
  serialization for all 34 `ImportRecord` types), `health_connect_sync_store`
  (reads → fingerprinted items, dedup via `filterExistingClientIds`, writes via
  `insertImportedRecords`), and `HealthDataSource.readImportRecords`.
- **Feature layer:** `lib/features/devicesync/` — `DeviceSyncViewModel` (the wizard
  state machine over the service + session + store) and `DeviceSyncScreen`.

## Current scope and follow-ups

- **Syncable types today:** the instant Health Connect "entry" types with working
  reads — weight, height, body fat, lean/bone/body-water mass, BMR, hydration,
  blood pressure, SpO2, respiratory rate, body temperature, VO2 max, blood
  glucose, and mindfulness. Interval/series/session types (steps, distance,
  calories, heart-rate series, sleep, nutrition, exercise, cycle) need dedicated
  raw reads that preserve every field and zone offset; they are added as those
  reads land.
- **In-process transfer:** the sync currently runs while the screen is
  foregrounded. A foreground-service variant (surviving backgrounding, like the
  Apple Health import) and a file-backed report store are planned.
- **On-device validation:** RFCOMM needs two physical phones (the emulator has no
  Bluetooth Classic radio). Confirm real discovery, bonding, transfer, and that
  `BLUETOOTH_SCAN`'s `neverForLocation` flag still surfaces Classic devices.
