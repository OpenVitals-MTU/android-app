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
