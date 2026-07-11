# health_connect_native

An app-local Flutter plugin that bridges OpenVitals to the **native AndroidX Health
Connect Kotlin client**, replacing the community `health` package so the app can use
the full Health Connect record surface with the same fidelity as the original native
Kotlin app.

```
OpenVitals (Dart features/repositories)
        │  depend only on the HealthDataSource facade
        ▼
lib/health/health_data_source.dart            (base facade — unchanged surface)
        │
        ├─ Android → lib/health/native/HealthConnectNativeDataSource
        │                 │  maps record-JSON ⇄ domain models
        │                 ▼
        │            HealthConnectHostApi   (Pigeon, type-safe)
        │                 │  records carried as JSON strings
        │                 ▼
        │            HealthConnectNativePlugin.kt  (Kotlin, ActivityAware)
        │                 │
        │                 ▼
        │            androidx.health.connect:connect-client
        │                 │
        │                 ▼
        │            Health Connect (system provider)
        │
        └─ iOS / other → UnsupportedHealthDataSource  (stub; HealthKit is a planned
                                                        parallel native bridge)
```

## Why

The `health` package could not express many record types OpenVitals uses (BMR,
elevation, wheelchair pushes, cadence/power, VO2 max, mindfulness, most cycle
records), could not write `clientRecordId`, and could not query by it (breaking
import dedup). Going straight to the AndroidX client removes all of those gaps.

## Bridge design

- **Pigeon** (`pigeons/messages.dart`) defines a small, strongly-typed `@HostApi`
  (`HealthConnectHostApi`). Operations are typed; **records cross as JSON strings**
  so the bridge stays stable without modelling 41 record classes in Pigeon.
- The **canonical record-JSON schema** is documented in
  `lib/health_connect_native.dart` and is the single contract shared by the Kotlin
  converters (`android/.../HealthRecordConverters.kt`) and the Dart mappers
  (`lib/health/native/health_record_json.dart`). All values are in canonical units:
  meters, kilograms, kcal, liters, °C, mmol/L, m/s, watts, bpm, ms, grams.
- Regenerate after editing the Pigeon input:
  `dart run pigeon --input pigeons/messages.dart`.

## Implemented (compile-verified; `flutter build apk --debug` succeeds)

- SDK-status availability; feature flags (skin temperature, mindfulness, planned
  exercise) via `HealthConnectFeatures`.
- Permission query + **request** (via `PermissionController` contract intent +
  `startActivityForResult` + `ActivityResultListener`).
- Reads (`readRecordsJson`/`readRecordJson`, paged) and **aggregation**
  (`aggregate` + `aggregateGroupByPeriod` with `Period` buckets) for all 41 record
  types; writes (`insertRecordsJson`) with `Metadata` carrying `clientRecordId`,
  version, `recordingMethod`, and device; delete by record/client id; import dedup
  (`filterExistingClientIds`).

## Not yet done (follow-ups)

- **On-device runtime verification.** Everything above is compiled against
  connect-client but has NOT been exercised on a device/emulator with Health
  Connect installed. Before shipping, on a real device: install Health Connect,
  grant OpenVitals the permissions, and verify each metric's read, each manual
  entry's write (and that it appears with the OpenVitals data origin), exercise
  route round-trip, aggregation values, and an Apple Health import end-to-end. The
  permission-request `onActivityResult` path in particular needs a device to
  confirm.
- **iOS / HealthKit.** iOS currently uses `UnsupportedHealthDataSource` (health
  data no-ops). The plan is a parallel native **Swift HealthKit** bridge behind the
  same `HealthConnectHostApi`/`HealthDataSource` seam (same Pigeon pattern), so the
  app-facing surface and repositories stay unchanged.
- **connect-client version.** Pinned to what resolves alongside the app today
  (unified to `1.2.0-alpha02`); revisit for a stable release. `MindfulnessSession`
  and `SkinTemperature` use opt-in/experimental APIs and are feature-gated.
- `ExerciseSegment.setIndex` is not carried (absent from this client version's
  constructor); `filterExistingClientIds` pages records and matches
  `metadata.clientRecordId` (Health Connect has no direct query-by-clientRecordId).
