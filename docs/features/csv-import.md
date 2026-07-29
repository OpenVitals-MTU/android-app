# CSV Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/imports/csv/`, `lib/features/settings/presentation/cards/csv_import_card.dart`, `lib/data/repository/contract/import_write_repository.dart`.
> **Navigation:** `/settings/data_import` → `/settings/data_import/csv`.
> **Related:** [Feature map](feature-map.md), [Body metrics](body-metrics.md), [Apple Health import](apple-health-import.md), [Settings and preferences](settings-and-preferences.md).

OpenVitals can import body measurements and vitals from a CSV file — a weight and body-composition export from a smart scale, a temperature log, a glucose export — without installing the vendor's own app. You choose what each column means; there are no per-vendor presets to wait for.

## How to use it

1. Export a CSV from your scale's web account or app and copy it to your phone.
2. Go to **Settings › Data Importers › CSV Importer** and tap **Import a CSV file**.
3. Pick the file. OpenVitals reads the header and the first 50 rows to build a preview — nothing is written yet.
4. Check the **column separator** and the **first row contains column names** switch. Both are detected, and both can be overridden.
5. For each column, choose what it is: **Not imported** (the default), **Date and time**, or one of the metrics. For a metric, also choose what its numbers are — kilograms, °F, mg/dL, and so on. If the column header names a unit, like `Weight (kg)` or `value (°C)`, that unit is pre-selected.
6. Check the **Date and time** panel. The line underneath shows how the first row's timestamp reads — confirm it is the day you expect before continuing.
7. On the confirm step, check the **range** shown for each metric. This is the range that will actually be written. Grant the write permissions if prompted, then tap **Import**.
8. Read the result: **Written**, **Already present**, **Rejected**, plus a breakdown of why any rows were rejected. **Save report** writes the whole thing to a text file if you need to look at it properly, and **Share report** sends that same file to another app as an attachment.

## The Withings example

A Withings scale exports:

```
Date,"Weight (kg)","Fat mass (kg)","Bone mass (kg)","Muscle mass (kg)","Hydration (kg)",Comments
2026-07-01 08:12:00,78.4,15.2,3.1,55.0,42.3,
```

Map `Date` → date and time, `Weight (kg)` → Weight, `Bone mass (kg)` → Bone mass, `Muscle mass (kg)` → Lean body mass, `Hydration (kg)` → Body water, and leave `Comments` not imported.

`Fat mass (kg)` is the interesting one. Health Connect stores body fat as a **percentage**, not a mass, so map it to **Body fat** and set its values to *"A mass in kilograms, as a share of the weight column"*. OpenVitals then works out the percentage from the weight measured in the same row — 15.2 / 78.4 = 19.4 %. A row with no usable weight cannot produce a percentage, so that one metric is skipped for that row and the rest of the row still imports.

**"Muscle mass" is not the same thing as "lean body mass."** Withings' muscle mass excludes bone and most water; Health Connect's `LeanBodyMassRecord` means body mass minus fat, which includes both. Mapping one onto the other is a decision only you can make, so OpenVitals does not do it silently — it is offered, labelled, and left to you.

## A simpler example

A Withings temperature export is two columns:

```
date,"value (°C)"
"2023-10-09 07:08:01",36.6
"2023-09-25 06:56:53",36.1
```

Map `date` → date and time and `value (°C)` → Body temperature, and that is the whole mapping. The quoted timestamps, the lowercase header and the `°C` unit all need no special handling: the timestamp column is found by *parsing the data*, not by recognising the word "date", and `(°C)` pre-selects Celsius.

## Supported metrics

**Body composition** — weight, body fat, lean body mass, bone mass, body water, height, basal metabolic rate.

**Vitals** — heart rate, resting heart rate, heart rate variability (RMSSD), blood oxygen (SpO₂), respiratory rate, body temperature, basal body temperature, blood glucose, VO₂ max.

Every one of them is a single measurement at a single instant, because that is the only shape the mapping model expresses: one row, one timestamp, one number per column.

Two consequences of that rule:

- **Blood pressure is not supported.** Systolic and diastolic have to become one Health Connect record, which needs a two-columns-to-one-record rule the mapping does not have yet.
- **Interval records — steps, sleep, workouts — are not supported.** They need a start *and* an end, and often sub-records. Adding them later is a catalog entry plus an end-timestamp column role, not a rewrite.

Heart rate is the one that looks like an exception and is not. Health Connect models it as a *series* rather than an instant, so a single spot reading is written as a one-sample series whose window is that instant — the same shape the Apple Health importer builds for a lone heart-rate sample.

Units are per column, and the ones a file is likely to use are all accepted: °C and °F, kg / lb / st / g, cm / m / in / ft, mmol/L and mg/dL, ms and s, percent and fraction. If the header names a unit in parentheses — `value (°C)`, `Weight (kg)` — it is pre-selected for you.

## Dates, times and time zones

The format is detected from the data, not the header. Year-first, day-first, month-first, ISO 8601, and epoch seconds or milliseconds are all recognised, and a custom pattern is available for anything else.

**When day-first and month-first both fit, OpenVitals refuses to guess.** `01/07/2026` is genuinely undecidable, and choosing wrong would silently write a year of measurements onto the wrong days. You are asked to pick, and the live example line under the format picker shows the result of your choice on a real row from your file.

Health Connect records both an instant and the wall-clock offset it was taken at, but most CSVs give only a local time with no offset. So you choose:

- **This phone's time zone** (default) — resolved against the date in each row, so a reading from a past summer gets that summer's offset, not today's.
- **UTC.**
- **A fixed offset**, for a file exported from a device that lived in one offset all along.

If the timestamp carries its own offset or ends in `Z`, the file wins and the picker is disabled.

Numbers are read tolerantly: `78.4`, `78,4`, `1.234,5` and `1,234.5` all parse, and a trailing unit like `78.4 kg` is ignored.

## What gets rejected, and why

A bad **timestamp** or a row with fewer columns than the mapping needs costs the whole row. A bad **value** costs only that metric, so one unreadable body-fat cell does not throw away a perfectly good weight. A blank cell is treated as a gap in the data, not an error — scales routinely skip metrics.

Values outside a plausible human range are rejected rather than stored. This is the guard against a mis-mapped column: a 900 kg weight, or a body-fat percentage derived by dividing by a column that was not body weight.

The result screen groups every rejection by reason with exact counts. The per-row log underneath is capped at 1,000 entries so that re-importing an already-imported file cannot grow it without bound.

## The report

**Save report** on the result screen writes a plain-text file — `openvitals-csv-import-report.txt` — containing the file name and outcome, the totals, the parsing settings actually used (separator, header row, date format, time zone), the full column mapping with the interpretation each column was given, rejection counts by reason, and the per-row rejection log with its row numbers and the offending values.

It exists because "2 rejected" on screen tells you something went wrong but not *what*, and the mapping you chose is exactly the thing you need to see to work out why. A derived body-fat column is written out as *"kilograms as a share of the weight column"* rather than just "kilograms", because the latter would misdescribe what was stored.

Like the Apple Health import report, it is **not sanitised** — it is an explicit export for troubleshooting and names your file, your columns and the values that were rejected. It contains no health data your own CSV did not already contain. You pick where it goes: the system "create document" picker on Android, a save dialog on desktop.

**Share report**, next to it, sends the same file straight to another app instead of keeping it on the phone — a WhatsApp, Signal or Telegram message, an email attachment, a notes app. It goes as an attached `.txt`, never as pasted message text, because a report of any size is unreadable in a chat bubble. Nothing is uploaded anywhere — the app has no internet permission; the file is handed to whichever app you pick from the system share sheet, and that app does the sending.

## Re-importing and duplicates

A record's identity is its **measurement type and its instant** — deliberately *not* its value. Health Connect replaces a record when an incoming one carries the same `clientRecordId`, so:

- Re-importing an unchanged file changes nothing and reports the rows as **already present**.
- Re-importing a file in which you **corrected a value** replaces the old record rather than leaving two measurements at the same instant.
- Re-exporting the same history with the columns reordered, or in pounds instead of kilograms, resolves to the same records.

The trade-off: two genuinely different measurements at the identical instant collapse into one. For a scale, that is the right call.

Records are written under a `csv_` id namespace, so they can never collide with the Apple Health importer's.

## Runs in the app, not as a background service

Unlike the Apple Health importer, a CSV import runs in-process. The app declares exactly one foreground service, which is why an Apple Health import refuses to start while a GPS recording is active — and that service exists for multi-gigabyte exports. A body-composition CSV is bounded by how often a person stands on a scale, so it needs none of that, and a **CSV import can run while an activity is being recorded**.

If Health Connect stops accepting writes because the app's quota is spent, the import stops and says so. Running it again later resumes from the top; records already written are recognised and reported as already present rather than duplicated.

## Data ownership

Imported records are written to Health Connect on your device. Nothing is uploaded. OpenVitals does not provide a bulk rollback after records are written.
