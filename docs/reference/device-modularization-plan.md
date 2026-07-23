# Device modularization plan

Status: proposed (2026-07-23). Target: split generic Bluetooth/device code from
Garmin-specific code, centralize Garmin under one module, and open a seam so
**WearOS** can be added as a sibling integration.

## TL;DR

There are **two independent reuse axes**, not one. Building a single
"generic-vs-Garmin" split would produce the wrong abstraction.

1. **Generic BLE primitives** — shared between **Garmin and the live HR-strap /
   power / cadence sensors** (and any future custom-BLE device). Already ~90%
   clean.
2. **A device-integration framework** — shared between **Garmin and WearOS**.
   This is *not Bluetooth code*: it is the registry, the device list/detail UI
   shell, a sync-orchestration seam, the wellness store, and the
   `ImportRecord → Health Connect` writer. **This seam does not exist today** and
   is the highest-value part of the work.

**WearOS shares ~zero protocol code with Garmin.** Garmin is a reverse-engineered
GFDI protocol over BLE that pulls FIT files. WearOS syncs via Google's Wearable
Data Layer (a companion Wear app) or Health Connect on the watch — no custom BLE,
no GATT, no GFDI, no FIT. None of the 4,670-line Garmin stack, the
`flutter_blue_plus` transport, or the FIT wellness parser is reusable for WearOS.

```
                    ┌─────────────────────────────────────────────┐
   device-integration framework  (registry · UI shell ·           │  ← Garmin AND WearOS
                    │  DeviceSyncPort · wellness store · HC writer) │     reuse THIS
                    └───────────────┬───────────────┬─────────────┘
                        Garmin ─────┘               └───── WearOS (future)
                    (GFDI/FIT/BLE)                    (Wear Data Layer)
                        │
              ┌─────────┴──────────────┐
              │  generic BLE primitives  │  ← Garmin AND live HR-strap sensors
              │ (connection · gatt · codecs) │    reuse THIS  (NOT WearOS)
              └────────────────────────┘
```

## Current state

| Area | Location | ~LOC | Verdict |
|---|---|---|---|
| Garmin protocol/sync stack | `data/source/sensors/garmin/` (23 files) | 4,670 | Clean internal layering L0→L6, **no widgets/providers** — already well-isolated |
| Generic BLE sensor stack | `data/source/sensors/ble/` | 1,400 | Clean generic live-sensor path; imports **zero** Garmin |
| Garmin/watch feature UI | `features/settings/**` + dashboard tile | 3,350 | Mix of generic shell + Garmin-bound screens |
| FIT parser | `features/manualentry/.../fit_route_parser.dart` | 2,400 | **Shared** with manual route import — must NOT move |
| FIT→HC import mapping | `features/imports/fit/fit_wellness_import.dart` | — | Reusable write pipeline (shared with Apple Health) |
| `bluetooth_sync_native` plugin | `packages/` | — | Phone-to-phone RFCOMM devicesync + companion-association helper. **NOT the Garmin transport** (that's `flutter_blue_plus`). Out of scope |

Corrections to initial assumptions (both verified against the code):

- **`garmin_settings_screen.dart` is not a widget** — it is a misnamed protobuf
  data-model + parser (`GarminSettingsScreen`/`Entry`/`EntryKind` +
  `parseGarminSettingsScreen`). No layering violation exists inside `garmin/`.
  Rename → `garmin_settings_model.dart`.
- **The FIT parser is a general FIT decoder**, shared with GPX/KML/TCX manual
  import via `route_file_parser.dart`. Only its `parseWellness` branch carries
  Garmin-proprietary field semantics (sleep_level msg 275, Body Battery, stress,
  intensity minutes, "not measurable" negative encodings).

### Garmin stack internal layering (already clean)

- **L0 truly generic:** `garmin_byte_reader`, `garmin_byte_writer`,
  `garmin_protobuf`, `garmin_log`.
- **L1 Garmin-specific primitives:** `garmin_crc`, `garmin_cobs`, `garmin_time`
  (primitive-shaped, but literal ports of Garmin's own algorithms).
- **L2 protocol values/framing:** `garmin_gfdi_frame`, `garmin_file_types`,
  `garmin_capabilities`, `garmin_directory`, `garmin_messages`,
  `garmin_phone_identity`.
- **L3 protocol transports:** `garmin_ml_transport`, `garmin_protobuf_transport`,
  `garmin_settings_service`, `garmin_settings_screen` (model).
- **L4 session orchestration:** `garmin_session`, `garmin_file_store`.
- **L5 BLE binding (native):** `garmin_ble_transport`, `garmin_gatt_probe` — the
  **only two files touching `flutter_blue_plus`**.
- **L6 app seam:** `garmin_watch_sync_service`, `garmin_settings_link`.

20 of 23 files are pure/unit-testable; L1–L4 are transport-agnostic (fed by
`write` callbacks / in-memory pipes). For WearOS this matters only in that the
protocol is cleanly separable from the radio — but WearOS does not speak GFDI, so
it reuses none of L1–L6, only the framework above.

## Coupling to fix (the actual leaks)

Four inversions, all in the shared BLE/registry surface:

1. **`BleDeviceRepository` (contract + impl) imports `GarminCapability`** and
   carries watch-sync state (`syncedFileKeys`, `markSynced`, `capabilities`,
   `recordCapabilities`, `recordSyncedFileKeys`, `clearSyncedFileKeys`). A generic
   registry reaching into a concrete Garmin data source — dependency rule
   backwards. Fix: move `GarminCapability` into the Garmin module and hold
   per-device sync/capability state as opaque metadata (or a separate
   Garmin-owned store keyed by deviceId).
2. **`ble_sensor_models.dart` imports `garmin_device_names.dart`**;
   `BleDiscoveredDevice` carries `advertisesGarminService` / `isGarminSyncDevice`.
   Garmin baked into the shared scan model. Fix: a pluggable device classifier
   the scanner consults, owned per integration.
3. **`ble_uuids.dart` holds GFDI/ML constants** used only by the Garmin
   transport. Fix: move GFDI/ML UUIDs into the Garmin module; keep only the
   member-service UUID (needed by the shared scanner) in the shared catalog.
4. **`ble_sensor_coordinator._addScanResult` has an `if garmin` branch.** Fix:
   the same pluggable classifier as (2).

Plus the **missing seam**: `garmin_sync_view_model` and `watch_settings_view_model`
call Garmin concretes directly — there is no `DeviceSyncPort` / settings port. The
routes (`/watch/:id`, `/settings/watches`) and `ble_devices_screen` /
`ble_devices_view_model` are already device-kind-generic; only the screens they
build are Garmin-bound.

## Target structure (bounded `lib/devices/` module)

```
lib/devices/
  core/                    # framework — device-type-agnostic (Garmin + WearOS)
    model/                 # RegisteredDevice, DeviceKind, capabilities  (from ble_sensor_models)
    registry/              # device repository, de-Garmined (opaque per-device metadata)
    sync/                  # DeviceSyncPort + scheduling + device_wellness store
    pairing/               # WatchPairingPort (BLE bond + companion association)
    ble/                   # shared BLE prims: connection, gatt probe, uuids,
                           #   byte codecs (reader/writer), protobuf, log
    ui/                    # device list/detail shell → dispatches to per-integration widgets
  garmin/                  # one integration — everything Garmin, centralized
    protocol/              # L1–L4: crc, cobs, time, frame, file_types,
                           #   capabilities, directory, messages, ml_transport,
                           #   protobuf_transport, session
    transport/             # L5: ble transport + gatt probe (Garmin V2 specifics)
    sync/                  # watch_sync_service, file_store, sync view-model
    wellness/              # GarminWellnessMetric interpretation + garmin_fit mapping
    settings/              # settings link/service/model + settings screen/vm
    ui/                    # Garmin device screen bits, onboarding steps
  wearos/                  # future sibling — Wear Data Layer; reuses core/, NOT garmin/ or core/ble
```

Kept **outside** `devices/`:
- **FIT container + activity decode** (`fit_route_parser.dart`,
  `route_file_parser.dart`) — shared with manual import. Consider promoting to
  `core/fit/` later; for now leave in place. Only the Garmin **wellness
  interpretation** moves to `devices/garmin/wellness/`.
- **`bluetooth_sync_native`** and `features/devicesync/` — the phone-to-phone
  sync feature. Unrelated; leave alone.

## Phased plan

Each phase is a reviewable PR that keeps tests green.

- **Phase 0 — prep, zero behavior change.** Rename `garmin_settings_screen`
  → `garmin_settings_model`. Fix the four inversions: extract `GarminCapability`
  + watch-sync state off `BleDeviceRepository`; move GFDI/ML UUIDs into the
  Garmin module; introduce a pluggable device classifier for the scanner. No file
  relocation yet.
- **Phase 1 — centralize.** Create `lib/devices/`; move the Garmin data-source
  stack, Garmin domain (ports/models/usecases), and Garmin presentation into
  `devices/garmin/**`; move the generic BLE stack + registry + pairing + shared
  primitives into `devices/core/**`. Pure moves + import fixes + test moves. This
  is the "centralize Garmin" goal.
- **Phase 2 — device-integration seam.** Introduce `DeviceSyncPort` /
  `DeviceIntegration`; de-Garmin the registry, wellness store, and
  `watch_data`/`watch_metrics` (off `GarminWellnessMetric`); make the dashboard
  tile + device screen dispatch through the seam. Now a second device type is
  pluggable.
- **Phase 3 — WearOS.** New integration implementing the framework, with its own
  Wear Data Layer plumbing (companion Wear app / Health Connect on watch).

### Deferred deliberately (YAGNI)

- **Renaming `garmin_wellness_samples` → `device_wellness_samples`.** The table
  shape is already generic `(metric, time_millis, value)`, but every access is
  typed to `GarminWellnessMetric`. Defer the data migration to Phase 2/3 when
  WearOS's actual metric vocabulary is known — don't pay migration cost
  speculatively.

## Risk notes

- ~9,500 LOC across data/domain/features + ~35 test files touch this area. Phase
  0 and 1 are mechanical but wide; rely on the existing test suite as the safety
  net and run `dart analyze --fatal-infos` (CI gate) after each.
- CI regenerates each pigeon plugin's `messages.g.dart` (gitignored) — moving
  plugin-touching code must not break the prepare list.
- Keep `bluetooth_sync_native` (devicesync) strictly separate from the Garmin
  transport throughout.
