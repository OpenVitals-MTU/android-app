# Garmin FIT files: types, proprietary extensions, and Health Connect mapping

Reference notes for importing Garmin FIT data into OpenVitals (Health Connect).
Written while investigating why a folder import of a Garmin Connect account export
reported "Imported 64. Failed 1936." — the short answer is that all but 64 of the
files are non-activity wellness files, and the importer only understands
activities. This document records what those files actually are so the FIT
importer can be extended to handle them.

Everything below was **verified against real files**, not just documentation:
the type numbers came from instrumenting the parser
(`fit_route_parser.dart`) to log every file's `file_id.type`, and the contents
were confirmed by decoding representative samples with the official
[`garmin-fit-sdk`](https://pypi.org/project/garmin-fit-sdk/) Python package.
Primary spec: <https://developer.garmin.com/fit/overview/>.

## The FIT container in one paragraph

A FIT file is a header, a sequence of records, and a CRC. Records are either
*definition* messages (declaring a local message type: its global message
number, byte order, and field list) or *data* messages (values for a previously
defined local type). Every file's **first data message is `file_id`** (global
message number 0). Its **field 0 is `type`** — the FIT `File` enum — and that
single byte decides what kind of file this is. OpenVitals reads exactly this:
`_fitFileIdMessageNumber = 0`, `_fitFileIdTypeFieldNumber = 0` in
`fit_route_parser.dart`.

## The `File` type enum

### Public values (from the FIT SDK profile)

These are the documented `File` enum values. Only a handful are relevant here:

| value | name | notes |
|------:|------|-------|
| 1 | device | device settings/info |
| 2 | settings | user/device settings |
| 3 | sport | sport settings |
| **4** | **activity** | recorded activity: session + laps + records (GPS) |
| 5 | workout | a planned workout (no recording) |
| 6 | course | a planned route (no recording) |
| 9 | weight | weight scale |
| 15 | monitoring_a | legacy all-day monitoring |
| 28 | monitoring_daily | daily monitoring rollup |
| **32** | **monitoring_b** | modern all-day wellness (steps, HR, stress, respiration) |
| 34 | segment | course segment |
| 247 | mfg_range_min (0xF7) | start of manufacturer-specific range |
| 254 | mfg_range_max (0xFE) | end of manufacturer-specific range |

Only **activity (4)**, **workout (5)** and **course (6)** carry an exercise
session or a route, so those are the only public types the current
activity/route importer can turn into an `ExerciseSession`.

### Garmin-proprietary values (NOT in the public profile)

A Garmin Connect account export is dominated by file types that are **not in the
published FIT profile at all** — the official `garmin-fit-sdk` decoder returns
them as raw integers, not names. They sit *below* the manufacturer range (247),
so they are Garmin-internal wellness types. Their nature is unambiguous from the
messages they carry (those message types *are* in the profile):

| value | what it is | tell-tale messages | HC-storable? |
|------:|-----------|--------------------|:---:|
| **41** | training metric summary | `file_creator` + msg `217` (tiny, ~179 B) | mostly no |
| **44** | device metrics | `device_info` + msgs `241` / `284` / `410` | mostly no |
| **49** | **sleep** | `sleep_assessment`, `sleep_level` (stages) | **yes** |
| **68** | **HRV status** | `hrv_value`, `hrv_status_summary` | **yes\*** |
| **79** | **sleep disruption** | `sleep_disruption_overnight_severity`, `sleep_disruption_severity_period` | no |

\* Health Connect *has* `HeartRateVariabilityRmssdRecord`, but OpenVitals'
import builder does not map it yet (see "Write surface" below).

Type numbers are **device/firmware-dependent** — these were emitted by a
**Garmin vívoactive 5**. Other devices/firmware may use different proprietary
numbers for the same concepts, so classify by **message content**, never by the
raw type number alone.

## Full mapping: Garmin FIT → Health Connect

| FIT type | Contents | Health Connect target | Importable |
|---------|----------|-----------------------|:---:|
| 4 activity | session, laps, records, GPS | `ExerciseSession` (+ route, `HeartRate`, `Speed`) | ✅ (already done) |
| 32 monitoring_b | steps, HR, respiration | `Steps`, `HeartRate`, `RespiratoryRate`, `RestingHeartRate` | ✅ |
| 32 monitoring_b | **stress** | — | ❌ no HC record |
| 49 sleep | sleep stages + assessment | `SleepSession` (with stages) | ✅ |
| 68 HRV | rmssd values + status | `HeartRateVariabilityRmssd` | ⚠️ needs builder addition |
| 79 sleep disruption | disruption severity | — | ❌ no HC record |
| 41 / 44 metrics | training status/load, VO2 | `Vo2Max` (only if present) | ⚠️ mostly no HC record |

**No Health Connect equivalent exists for:** stress, Body Battery, training
status / training load, VO2-max trend beyond a single value, and
sleep-disruption severity. These cannot be written to Health Connect and are
skipped (a per-type summary is logged so nothing is silently dropped).

## OpenVitals write surface (the reuse target)

FIT wellness data does **not** need a new write layer. OpenVitals' Apple Health
importer already writes to Health Connect through a generic, discriminated path:

- Pigeon: `insertImportedRecords(List<ImportRecordMsg>)`
  (`packages/health_connect_native/pigeons/messages.dart`).
- `ImportRecordMsg` carries `recordType` (a canonical schema name), scalar
  `doubleFields`/`intFields`, and typed lists for `samples` (HR/Speed), and
  `sleepStages`, and `routePoints`.
- Native dispatch: `ImportRecordsBuilder.kt` `when (msg.recordType)`.

Record types the builder **already** handles: `Steps`, `Distance`,
`ActiveCaloriesBurned`, `BasalMetabolicRate`, `FloorsClimbed`, `ElevationGained`,
`WheelchairPushes`, `Speed`, `HeartRate`, `RestingHeartRate`, `Weight`, `Height`,
`BodyFat`, `LeanBodyMass`, `BoneMass`, `BodyWaterMass`, `Hydration`,
`OxygenSaturation`, `RespiratoryRate`, `BodyTemperature`, `BasalBodyTemperature`,
`BloodGlucose`, `Vo2Max`, `BloodPressure`, `Sleep` (with stages),
`MindfulnessSession`, `MenstruationFlow`, `OvulationTest`, `CervicalMucus`,
`IntermenstrualBleeding`, `SexualActivity`, `Nutrition`, `ExerciseSession`.

**The one gap:** `HeartRateVariabilityRmssd` is not in that `when`. Adding HRV
support means adding a `"HeartRateVariabilityRmssd"` branch (+ its permission)
and regenerating Pigeon — the only native change the whole feature needs.

The Apple Health importer (`lib/features/imports/applehealth/`) is the blueprint:
it parses an export → builds `ImportRecordMsg`s → dedups by `clientRecordId` →
writes in batches under a **foreground service with checkpointing/staging**
(`apple_health_import_foreground_controller.dart`,
`apple_health_import_checkpoint_store.dart`). High-volume FIT types
(`monitoring_b`) will need that same machinery; the current bulk FIT importer is
a simple in-memory loop suited only to a few hundred activities.

## Observed distribution (one real export)

Garmin Connect "Export all data" produces `UploadedFiles_*/` folders of
per-record FIT files named `<account>_<id>.fit`. One account held **3,582**
files. The first 2,000 (all the importer scanned — see truncation below) broke
down as:

| type | meaning | count |
|-----:|---------|------:|
| 32 | monitoring_b | 1,019 |
| 44 | metrics | 373 |
| 49 | sleep | 203 |
| 68 | HRV | 181 |
| 79 | sleep disruption | 93 |
| 41 | metric summary | 67 |
| 4 | **activity** | **64** |

Activities are ~2–3% of the files. Proportionally the full 3,582 hold ~115
activities, so a 2,000-file cap applied to the *raw* listing dropped roughly
**50 real activities** behind thousands of wellness files — which is why the
truncation cap must be applied to importable files, not the raw folder listing.

## How to reproduce the classification

```sh
pip install garmin-fit-sdk        # official Garmin decoder
python - <<'PY'
from garmin_fit_sdk import Decoder, Stream
msgs, errs = Decoder(Stream.from_file("some.fit")).read()
fid = msgs["file_id_mesgs"][0]
print("type:", fid.get("type"), "product:", fid.get("garmin_product"))
print("messages:", {k: len(v) for k, v in msgs.items()})
PY
```

`type` prints a name for public values (`activity`, `monitoring_b`) and a raw
integer for the proprietary ones — decode the `messages` histogram to see what a
proprietary file actually contains.

## Message field reference (for the importer)

The Dart decoder (`fit_route_parser.dart`) reads FIT messages by **global message
number** and **field number**, so this is the spec the wellness importer is built
from. Numbers below were taken from the FIT SDK profile embedded in
`garmin-fit-sdk` and confirmed against real vívoactive-5 files. Apply the field's
**scale** (raw ÷ scale) and, for `date_time`, the FIT epoch (seconds since
1989-12-31 00:00:00 UTC = Unix 631065600).

### Sleep — file type 49 → `SleepSessionRecord`

| message | num | fields (num: name) | use |
|---------|----:|--------------------|-----|
| `event` | 21 | 253: timestamp, 0: event, 1: event_type | session bounds |
| `sleep_level` | 275 | 253: timestamp, 0: sleep_level | stage transitions |
| `sleep_assessment` | 346 | scores (overall/deep/rem/…) | **skip** — no HC field |

- **Session bounds:** the `event` pair with `event == 74` (`sleep`, a Garmin
  **proprietary** value — not in the public `event` enum, which skips 73→75),
  `event_type == 0` (start) / `1` (stop).
- **Stages:** each `sleep_level` message marks the start of a stage; it runs until
  the next message's timestamp (last → session stop). `sleep_level` enum:
  `0 unmeasurable, 1 awake, 2 light, 3 deep, 4 rem`.
- **Garmin `sleep_level` → `SleepStageType`:** awake→`awake`, light→`light`,
  deep→`deep`, rem→`rem`, unmeasurable→drop (or `awakeInBed`).

### HRV — file type 68 → `HeartRateVariabilityRmssd` *(needs the native builder branch)*

| message | num | fields | use |
|---------|----:|--------|-----|
| `hrv_status_summary` | 370 | 253: timestamp, 1: last_night_average (uint16, **scale 128**, ms) | one nightly RMSSD value |
| `hrv_value` | 371 | 253: timestamp, 0: value (uint16, **scale 128**, ms) | per-reading series (optional) |

Health Connect's `HeartRateVariabilityRmssdRecord` is a single instant + rmssd
(ms). Map `hrv_status_summary.last_night_average` (÷128) at its timestamp. This is
the **only** type needing a new native branch (`ImportRecordsBuilder.kt` +
`ImportRecordMsg` recordType `"HeartRateVariabilityRmssd"` + Pigeon regen + the
`HeartRateVariabilityRmssd` write permission).

### Monitoring — file type 32 → `Steps`, `HeartRate`, `RespiratoryRate`, `RestingHeartRate`

| message | num | key fields | HC target |
|---------|----:|-----------|-----------|
| `monitoring` | 55 | 253: timestamp; 1: calories (kcal); 2: distance (uint32, scale 100, m); 3: cycles (scale 2) / 9: cycles_16 = steps; 4: active_time (scale 1000, s) | `Steps` (cycles→steps for walking/running activity_type), `Distance`, `ActiveCaloriesBurned` |
| `monitoring_info` | 103 | 5: resting_metabolic_rate (kcal/day) | `BasalMetabolicRate` |
| `monitoring_hr_data` | 211 | 253: timestamp; 0: resting_heart_rate (bpm); 1: current_day_resting_heart_rate | `RestingHeartRate` |
| `respiration_rate` | 297 | 253: timestamp; 0: respiration_rate (sint16, scale 100, breaths/min) | `RespiratoryRate` |
| `stress_level` | 227 | 0: stress_level_value; 1: stress_level_time | **skip** — no HC record |

Monitoring is the **high-volume** family (hundreds of messages per daily file,
thousands of files): steps arrive as cumulative `cycles`/`cycles_16` that must be
differenced per `activity_type`, and HR arrives as `monitoring` samples. This is
the family that needs the Apple Health importer's foreground-service +
checkpointing rather than the in-memory loop.

### Skipped (no Health Connect record exists)

`stress_level` (227), Body Battery, training status/load, and the sleep-disruption
messages (types 79: `sleep_disruption_overnight_severity`,
`sleep_disruption_severity_period`). Log a per-type skip summary; never write.

## Implementation approach

Build on the existing pipeline, do not reinvent it:

1. **Decode** — extend `_FitDecoder` to collect the wellness messages above
   (add their message numbers to the parsed set; gather per-message rows).
2. **Map** — turn decoded rows into the existing `ImportRecord` subtypes in
   `lib/domain/model/apple_health_import_records.dart` (`SleepSessionImportRecord`,
   `HeartRateImportRecord`, `StepsImportRecord`, `RestingHeartRateImportRecord`,
   `RespiratoryRateImportRecord`, `Vo2MaxImportRecord`; add an HRV record). Use a
   deterministic `clientRecordId` (e.g. `garmin_fit_<type>_<startEpochMs>`) so
   re-imports dedupe.
3. **Write** — feed them to `HealthDataSource.insertImportedRecords` (same call the
   Apple Health importer uses), not the activity write path.
4. **Route** — in the bulk importer, branch on `file_id.type`: activity/course/
   workout → existing route path; wellness types → the new map+write path;
   skipped types → count and log, never fail.
