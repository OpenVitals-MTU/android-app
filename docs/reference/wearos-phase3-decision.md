# WearOS (device-modularization Phase 3) — mechanism decision

Status: in progress (2026-07-23). Companion to `device-modularization-plan.md`
(its Phase 3). Records **how** a WearOS watch's data should reach the app, why the
obvious mechanism is off the table, and what the empirical test on real hardware
showed.

## TL;DR

WearOS gets into the app through **Health Connect**, not a first-party Wear
companion app. The Wear Data Layer path is **eliminated by the target hardware**,
not merely disfavoured. WearOS data is treated as a Health Connect **source**
(attribution), **not** a BLE registry device.

## The two candidate mechanisms

1. **Read via phone Health Connect (chosen).** The watch's own vendor app (Samsung
   Health, Fitbit, …) writes HR/steps/sleep into the phone's Health Connect, which
   openvitals already reads. For those users the data flows in with no new code.
2. **First-party Wear companion app + Wear Data Layer (rejected).** A WearOS app
   module + a `wear_sync_native` pigeon plugin (templated on
   `packages/bluetooth_sync_native`) reading Health Services on the watch and
   pushing over the Data Layer to a phone-side `WearableListenerService`.

## Why the Data Layer path is out — the decisive constraints

- **Proprietary GMS.** The Wear Data Layer lives in
  `com.google.android.gms:play-services-wearable` — Google Play Services. This app
  is F-Droid-published with a **reproducible, FOSS-only build** that already strips
  `play-services-location` (`android/build.gradle.kts` `--build-id=none` hook,
  `dependenciesInfo.includeInApk=false`; the geolocator `FORK.md`). Pulling GMS in
  breaks F-Droid publishability.
- **Target hardware makes it impossible, not just non-FOSS.** The test rig is a
  **de-Googled Pixel Pro (no Google apps / no Play Services)** + a Samsung WearOS
  watch. Without Play Services on the phone, the Data Layer API cannot function at
  all. So the Data Layer path is not an option to weigh — it is unavailable on the
  device the work targets.

Health Connect, by contrast, is a **system module** on Android 14+ (no GMS),
present on de-Googled Pixels, and the app is already a first-class Health Connect
client.

## What already exists (so "read via Health Connect" is mostly free)

- Every Health Connect record already crosses the `health_connect_native` bridge
  with its **`source`** = `metadata.dataOrigin.packageName` (the writing app). The
  app even aggregates `sources` for `DataConfidence` and shows a `SourceChip`.
- The generic write path `ImportRecord → HealthDataSource.insertImportedRecords`
  (proven by Apple Health import + phone-to-phone sync) is source-agnostic and
  reusable if the app ever needs to write, not just read, watch-derived data.
- A **read-only diagnostic** was added (diagnostics-enabled builds): Settings →
  Debug diagnostics → **Health Connect sources**
  (`lib/features/settings/presentation/cards/health_connect_sources_card.dart`,
  `.../application/health_connect_sources.dart`). It lists the apps/devices that
  wrote HR/sleep in the last 7 days, attributed by package — the observation tool
  for the test below.

## Why NOT a BLE registry device

The device registry (`lib/devices/core/registry/`) is BLE-bound: `addDevice`
requires and **dedups on a MAC address**, discovery is **BLE-scan only**
(`DeviceScanClassifier` is advertisement-shaped), and onboarding mandates an OS
**BLE bond + GATT probe** (`OnboardGarminWatchUseCase` / `WatchPairingPort`). A
WearOS watch has no MAC, emits no advertisement, and is paired via its vendor's
companion app — none of that applies. Forcing it into the registry would mean
synthesizing a fake MAC and a fake discovery/bond path. So WearOS data is modelled
as a Health Connect **source**, orthogonal to the BLE device registry.

## Empirical test (the open question)

Whether a Samsung WearOS watch can put its data INTO a de-Googled Pixel's Health
Connect **at all** cannot be reasoned out — the normal bridge (Samsung Health)
wants a Samsung account and often GMS, which the phone lacks. Test procedure:

1. Run a **diagnostics-enabled** build (debug/nightly) on the Pixel.
2. Pair the Samsung watch; install whatever bridges its data to Health Connect
   (Samsung Health ↔ Health Connect, or a FOSS bridge such as Gadgetbridge if the
   model is supported). Generate some HR/steps/sleep on the watch.
3. Grant openvitals Health Connect **read** permission for heart rate + sleep.
4. Open Settings → Debug diagnostics → **Health Connect sources** and refresh.

**Result (2026-07-23, Pixel 6 Pro / Android 17, de-Googled).** The Health Connect
path is real and works on this rig — but it is **vendor-dependent**:

- **Proven working (Garmin).** The phone already feeds HC from a **Garmin
  vívoactive 5** via **Garmin Connect** — the debug app read **4,422 HRV + 14
  exercise sessions + heart-rate** at startup, with **no Play Services**. So a
  watch's data DOES reach a de-Googled phone's Health Connect when its companion
  app runs GMS-free. Garmin Connect does.
- **Blocked (Samsung Galaxy Watch8).** The Watch8 was added to the app as a live
  **BLE sensor** (HR + spurious cycling caps; see the smartwatch-labelling fix) —
  that path works for **live** heart rate. But its **all-day** data (sleep/HRV/
  steps) needs a bridge into HC, and the only official one is **Samsung Health**
  (Galaxy Watch → Samsung Health → HC, since v6.22.5), which needs a Samsung
  account + Google Play Services and so is unlikely to run on this de-Googled
  phone. Health Connect does not support Wear OS devices directly, and Gadgetbridge
  has no solid Galaxy-Watch-WearOS support. No bridge is installed; no Watch8
  all-day data reaches HC.

**Conclusion.** The mechanism is sound (Garmin proves it, GMS-free). WearOS
feasibility is per-vendor: it works where the vendor's HC bridge is GMS-free, and
is effectively blocked for Samsung on a de-Googled phone. For the Galaxy Watch on
this rig: live HR via the BLE sensor path (works, now labelled a smartwatch);
all-day data not available without Samsung Health.

## Follow-up build (conditional, done in part)

- **Smartwatch labelling (done, `ca31b20b6`).** A name-based classifier
  (`lib/devices/core/ble/smartwatch_names.dart`) gives a Galaxy/Pixel/Wear OS watch
  a watch glyph on the sensor row — presentation only, still a live sensor, NOT a
  Garmin sync watch.
- **HC sources diagnostic (done, `5ed2e52e4`).** Settings → Debug diagnostics →
  Health Connect sources — the observation surface; the seed of a passive
  "connected sources" view if the HC path is ever promoted to a user feature.

## Decision

Pursue **only** the Health Connect path, gated on the empirical result above. Do
not add `play-services-wearable`, a WearOS app module, or a BLE registry entry for
the watch. Revisit the Data Layer path only if the project ever ships a separate,
non-F-Droid GMS build flavour for a Google-services phone — a deliberate scope
change, not a Phase 3 step.
