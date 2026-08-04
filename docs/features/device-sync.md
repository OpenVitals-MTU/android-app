# Sync With Another Phone

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/devicesync`, `features/settings`.
> **Navigation:** `Screen.SettingsDeviceSync`; settings section `DEVICE_SYNC`.
> **Related:** [Feature map](feature-map.md), [Settings and preferences](settings-and-preferences.md), [Permissions](../app/permissions.md), [Privacy](../app/privacy.md).

Sync with another phone copies Health Connect records directly between two nearby Android phones over Bluetooth. There is no account, no server, and no network step.

It is reached from Settings, Sync with another phone, which opens its own wizard.

## The Wizard

1. **Choose a role.** One phone makes itself discoverable and becomes the host. The other looks for a phone and becomes the guest.
2. **Pair.** The host shows a six-digit pairing code and waits. The guest scans, picks the host from the list of nearby phones, and types the code. Already-paired phones appear in the list before the scan finishes.
3. **Choose how far back.** The last 30 days, the last 6 months, the last year, or everything. The last year is the default.
4. **Choose what to sync.** The picker lists data categories such as activity, workouts, heart, sleep, body measurements, vitals, nutrition, hydration, mindfulness, and cycle tracking. A category appears only when this phone can both read and write at least one of its record types, and everything supported is selected by default.
5. **Sync.** Progress shows the current phase and live sent, received, and written counts.
6. **Read the report.** The report shows how many records were merged, how many were already present, and a per-record-type breakdown of what arrived. It can be copied or shared as text.

Both phones choose their own range and their own categories. The exchange uses the record types both phones support.

## Both Directions At Once

The exchange is bidirectional within a single session. Both phones send and receive over the same connection at the same time, and each phone reports what it wrote.

The category selection controls what this phone sends. What it receives is decided by the other phone's selection, which is why the picker is framed as what to accept and shows the types both phones agreed on.

## Bluetooth, Not The Internet

The transfer runs over Bluetooth Classic RFCOMM on a private OpenVitals service identifier, so the app only ever connects to another OpenVitals.

This is a deliberate choice rather than a convenience. Any Wi-Fi or TCP socket on Android requires the `INTERNET` permission, which OpenVitals does not declare and actively removes from the manifest. Bluetooth Classic needs no such permission, so the transfer stays peer-to-peer with no network involved. See [Permissions](../app/permissions.md).

The host phone must be made discoverable, which Android asks about with its own dialog. Connecting to a phone for the first time triggers Android's standard pairing dialog; that bond is what encrypts the link.

## What The Pairing Code Protects

Confidentiality and tamper resistance come from the Bluetooth bond that Android establishes. The six-digit code is a mutual confirmation between the two phones in front of the two users: both derive a session key from it and prove they know it before any health data moves. A wrong code, or the right code typed at the wrong phone, ends the session before anything is exchanged.

## Re-Syncing Does Not Duplicate

Each record is identified by a fingerprint computed from its own content: the record type, its timestamps, and its values. Both phones compute the same fingerprint for the same logical record.

- Before exchanging, each phone fingerprints what it already has. An incoming record it already holds is counted as already present and is not written.
- Records that are written carry their fingerprint as the Health Connect client record ID, so Health Connect updates in place rather than duplicating.
- A receiving phone re-derives the fingerprint from the record it decoded rather than trusting the identifier the other phone sent, so a sync cannot overwrite an unrelated record.

Running the same sync twice therefore writes nothing new, and the two phones converge.

## While The Sync Runs

A quiet ongoing notification, "Syncing with another phone", asks the user to keep both phones nearby. It exists so Android does not kill the app while the user is looking at something else.

The transfer belongs to the wizard screen: switching to another app is fine, but navigating away from the wizard inside OpenVitals ends the session. A sync also will not start while an activity recording is running; the recording has to be finished or discarded first.

## Permissions

Sync asks for nearby-device Bluetooth permissions, and for location on Android versions before 12 where classic Bluetooth discovery requires it. It then asks for the Health Connect read and write permissions for the record types it can exchange, and finally, on the host, for Android's discoverable window.

## Privacy

Records move directly between the two phones over Bluetooth and are written to Health Connect on each device. Nothing is uploaded, and the app has no internet permission to upload with. The most recent sync report is stored locally as a text file in the app's own storage; only the latest one is kept.
