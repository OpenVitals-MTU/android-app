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
2. **Connect.** The host waits. The guest scans and picks the host from the list of nearby phones. Already-paired phones appear in the list before the scan finishes.
3. **Choose how far back.** The last 30 days, the last 6 months, the last year, or everything. The last year is the default.
4. **Choose what to sync.** The picker lists data categories such as activity, workouts, heart, sleep, body measurements, vitals, nutrition, hydration, mindfulness, and cycle tracking. A category appears only when this phone can both read and write at least one of its record types, and everything supported is selected by default.
5. **Compare the codes.** After both phones press Start sync, each shows six digits. The user checks that they are the same and says so on both phones. No record moves before that.
6. **Sync.** Progress shows the current phase and live sent, received, and written counts.
7. **Read the report.** The report shows how many records were merged, how many were already present, and a per-record-type breakdown of what arrived. It can be copied or shared as text.

Both phones choose their own range and their own categories. The exchange uses the record types both phones support.

## Both Directions At Once

The exchange is bidirectional within a single session. Both phones send and receive over the same connection at the same time, and each phone reports what it wrote.

The category selection controls what this phone sends. What it receives is decided by the other phone's selection, which is why the picker is framed as what to accept and shows the types both phones agreed on.

## Bluetooth, Not The Internet

The transfer runs over Bluetooth Classic RFCOMM on a private OpenVitals service identifier, so the app only ever connects to another OpenVitals.

This is a deliberate choice rather than a convenience. Any Wi-Fi or TCP socket on Android requires the `INTERNET` permission, which OpenVitals does not declare and actively removes from the manifest. Bluetooth Classic needs no such permission, so the transfer stays peer-to-peer with no network involved. See [Permissions](../app/permissions.md).

The host phone must be made discoverable, which Android asks about with its own dialog. Connecting to a phone for the first time triggers Android's standard pairing dialog. That bond encrypts the radio link. OpenVitals does not rely on it alone.

## What The Code Protects

The code proves that the two phones talk to each other and to nobody in between. It is compared, never typed.

1. The host commits to a fresh P-256 key: it sends a hash of the key and a random salt.
2. The guest sends its own fresh key. Only then does the host reveal its key and salt, and the guest checks them against the hash.
3. Both phones hash the whole handshake, run ECDH, and derive with HKDF one AES-256 key per direction and the six digits.
4. Both phones show the digits. Each user says whether they match. A "They do not match" on either phone ends the session.
5. From the key exchange on, every frame except an abort is sealed with AES-256-GCM. A frame counter is the nonce, so a changed, replayed, dropped or reordered frame ends the session.

Someone in the middle holds a different key with each phone, so the two phones show different digits. The commitment stops them from searching for a key that gives the same digits: they must fix their key before they see the other side's. One attempt in a million works, and each attempt needs both users at the wizard.

The check only works if the users really compare. It uses Android's built-in cryptography and adds no dependency. Keys live for one session and are never stored.

A typed code cannot do this job. A six-digit code that is proved with a MAC can be guessed offline from one recorded handshake, which is how protocol version 1 worked. Version 2 phones refuse version 1 phones, so both phones need a current build.

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
