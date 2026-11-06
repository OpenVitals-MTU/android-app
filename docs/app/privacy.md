# Privacy

OpenVitals is designed as a local-first app. The local Android app is intentionally separate from any connected app work.

The formal policy users see is [PRIVACY.md](../../PRIVACY.md): Settings and the re-consent dialog link to it. This page explains the engineering behind it. Keep the two in step. A change here that touches what is read, stored or sent needs the same change there, with a new "Last updated" date.

The app asks every user to accept the policy again when that date changes. `PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION` holds the date, and `PrivacyPolicyVersionTest` fails when the constant and `PRIVACY.md` differ. So a new date always reaches users, and a typo fix that keeps the date does not. The re-consent dialog waits while the one-time "moved to OpenWatch" notice is on screen, so the two never stack.

The local app:

- Does not ship app-level internet permission.
- Does not create OpenVitals cloud accounts.
- Does not upload health data to an OpenVitals server.
- Does not include ads.
- Does not include an analytics SDK.
- Reads supported records through Health Connect.
- Writes supported records only after an explicit save, import, record, edit, or delete action.

Local app preferences may include onboarding state, acknowledged permission prompts, unit settings, language, theme, widget order, goals, reminders, Body Energy calibration, timer settings, paired device settings, display choices, and the cycle tracking contexts, age band and cycle reminder settings, which live in their own preferences file.

## No Internet Permission

The manifest removes `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_WIFI_STATE` from the merged manifest, so no library can quietly reintroduce network access. Android itself then makes network use impossible, which is stronger than a promise not to use it.

Every device feature is built around keeping that true:

- Phone-to-phone sync uses Bluetooth Classic (RFCOMM), not Wi-Fi. A Wi-Fi or TCP transfer would require `INTERNET`; Bluetooth keeps it a direct link between the two phones.
- Offline maps are imported as local map packs; nothing is downloaded.
- Diagnostics and import reports are generated on device and shared only if the user chooses to. They hold structure, not data: counts, record types, reasons and masked error text. See [Text that leaves the app is structure, not data](../engineering/architecture.md#5-text-that-leaves-the-app-is-structure-not-data).

## Paired Devices

Watches and scales are not paired with OpenVitals. The separate [OpenWatch](https://github.com/OpenVitals-MTU/OpenWatch) app syncs them and writes what they record to Health Connect, which OpenVitals then reads like any other source.

### Phone-To-Phone Sync

### Phone-To-Phone Sync

Sync copies Health Connect records from one phone to another over a paired Bluetooth Classic link. One phone is made discoverable and the other scans for it. The user chooses how far back to sync and which data categories to accept. Before any record moves, the phones agree on a fresh key and both show the same six digits; the user confirms on both that they match, which rules out a device in between. The records then travel encrypted with that key (AES-256-GCM), on top of Bluetooth's own encryption. The key is never stored. No server or account is involved at any point. See [Sync with another phone](../features/device-sync.md).

## Health Connect Records

Health Connect is the source of truth. OpenVitals reads Health Connect records to show dashboard summaries, detail screens, readiness, statistics, achievements, and insights.

Manual entries are saved back to Health Connect only when the user chooses to save them. Supported Apple Health export records are written to Health Connect only when the user imports an export file from Settings. OpenVitals-created records can be edited later; records created by other apps remain read-only.

Apple Health exports are copied into app-private local storage and analyzed there before the user chooses which categories to import. The staged compressed copy is reused for the pending background import, cleared after a successful import or analysis failure, replaced by a newly selected export, and removed with the app's private data. A copy from an analysis the user never finished is removed at the next app start once it is a day old, unless an import is queued or running. Import reports are generated locally when the user runs an import and chooses to copy or download the report. They hold the importer's counts, selected categories, reasons and log, with error text masked, and none of the records' values, times or sources, no file locations and no file names. The workouts imported without their route are listed on the import card and kept in a private file beside the report, never in it. Reports written before this was so are deleted at app start.

## Medical Records

Health Connect stores medical records as FHIR resources, on Android 14 and newer with its medical records feature. See the [proposal](../proposals/medical-records.md) until the feature page exists.

- The 13 medical permissions are asked for in one request, only inside the medical records area, never with fitness permissions. They join `managedPermissions` so a grant is seen, and stay out of every set that onboarding or Settings asks for.
- Records are shown as Health Connect holds them. Nothing is computed from them, and a value is flagged only when the source flagged it.
- Imports and exports are file based. Nothing is fetched from a server. The import report counts what was written, held back or refused by record type and reason, and holds no record's content or id, no file, source or app name and no address.
- A SMART Health Card can also be scanned with the camera. `CAMERA` is asked for only when Scan is tapped; frames are analysed in memory and never stored. A card's signature is not checked, since that needs the issuer's keys from the network, and its records are tagged as not verified.
- OpenVitals keeps no copy of records. The one exception is a saved document: an imported file the user chose to keep, in app-private storage with a Room index, not backed up.
- Phone-to-phone sync carries medical records in full when the category is selected on both phones. Saved documents stay on the phone.

## Sensitive Data

Cycle tracking uses Health Connect records and is shown only after cycle permissions are granted explicitly. The day log's subjective fields (pain, mood, energy, symptoms, notes, pregnancy-test results, temperature disturbances, cervical sensation), the cycles excluded from estimates, and the declared contexts and age band have no Health Connect record type and live in the app's Room database (`cycle_journal_entries`, `cycle_exclusions`) and its own preferences file. They are handled like the rest of the app's local data: they move between two phones with the cycle category of phone-to-phone sync, and they are deleted from Settings → Cycle or with the app's data.

Workout route previews require manual Health Connect approval in some cases. GPS recording requires location permission because OpenVitals needs location points to build the route. Imported PMTiles or Mapsforge map packs stay local on the device.

## Permission Details

See [Permissions](permissions.md) for the current permission list and why each group is requested.

See [Local And Connected Editions](local-and-connected-editions.md) for the boundary between the local app and planned connected work.
