# CSV Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/imports/csv`, `features/settings`.
> **Navigation:** `Screen.SettingsDataImport`, then `Screen.SettingsCsvImport`.
> **Related:** [Feature map](feature-map.md), [Settings and preferences](settings-and-preferences.md), [Body metrics](body-metrics.md), [Apple Health import](apple-health-import.md).

CSV import brings body measurements and vitals from a plain CSV file into Health Connect. It is meant for exports that have no importer of their own, such as a smart scale's weight and body-composition history, a temperature log, or a glucose export.

There are no vendor presets. The user says what each column means, which is why a file OpenVitals has never seen still imports.

## Import Flow

CSV import lives in Settings, Data Importers, CSV Importer, and opens its own five-step screen.

1. **Choose a file.** The file picker accepts CSV and plain-text documents. Only the head of the file is read at this point, so a large export opens immediately and nothing is written yet.
2. **Map the columns.** OpenVitals shows the detected column separator, a "first row contains column names" switch, a short preview table, the date and time settings, and one row per column. Each column is set to not imported, date and time, or a supported measurement. A measurement column also picks what its numbers are.
3. **Confirm.** The confirm step shows the date range the file covers and the value range each measurement will actually be written with, so a mis-mapped column is visible before anything lands. Missing Health Connect write permissions are requested here.
4. **Import.** Progress shows the current row with running written, already-present, and rejected counts. The import can be cancelled; what was already written is kept.
5. **Read the result.** The result step shows the totals, groups every rejection by reason, and can copy or save the full report.

## Supported Measurements

Body composition:

- Weight.
- Body fat.
- Lean body mass.
- Bone mass.
- Body water.
- Height.
- Basal metabolic rate.

Heart and vitals:

- Heart rate.
- Resting heart rate.
- Heart rate variability (RMSSD).
- Blood oxygen (SpO2).
- Respiratory rate.
- Body temperature.
- Basal body temperature.
- Blood glucose.
- VO2 max.

Every supported measurement is a single value at a single instant, because that is the shape the column mapping expresses: one row, one timestamp, one number per column.

## What Is Not Supported

- **Blood pressure is deliberately unsupported.** Systolic and diastolic have to become one Health Connect record, which needs a two-columns-to-one-record rule the mapping model does not have.
- **Interval records are not supported.** Steps, sleep, and workouts need a start and an end, and often sub-records, so they are out of scope for the current mapping model.

## Units Come From The File

The unit chosen for a column describes the file, not the app's display unit system. A file in pounds imports correctly while the app displays kilograms, and neither setting affects the other. Values are converted once, on import, and stored in the units Health Connect uses.

Accepted units per measurement include kilograms, pounds, stones, and grams for masses; centimetres, metres, inches, and feet for height; kilocalories and kilojoules per day for basal metabolic rate; Celsius and Fahrenheit for temperatures; millimoles per litre and milligrams per decilitre for glucose; milliseconds and seconds for HRV; and percent or a 0–1 fraction where a share is meaningful.

When a header names its unit in parentheses, such as `Weight (kg)` or `value (°C)`, that unit is pre-selected. It stays an editable default.

Body fat has an extra option for scales that export a fat *mass* instead of a percentage. Health Connect stores body fat as a percentage, so the column can be read as a mass and turned into a percentage using the weight measured in the same row. A row without a usable weight loses only that one value; the rest of the row still imports.

## Dates, Times, And Time Zones

The timestamp format is detected from the data rather than from the header text. ISO 8601, year-first, day-first, month-first, seconds since 1970, and milliseconds since 1970 are recognized, and a custom pattern is available for anything else.

When day-first and month-first both fit every sampled row, OpenVitals does not guess. It says the dates are ambiguous and asks which one the file uses. A live example line shows how the first row reads under the current choice.

Most CSV files carry a local time with no offset, so the time zone is chosen explicitly:

- This phone's time zone, resolved against each row's own date so a past summer reading keeps that summer's offset.
- UTC.
- A fixed offset, for a file exported from a device that stayed in one offset.

If a timestamp carries its own offset or ends in `Z`, the file wins and the time zone choice is disabled.

Numbers are read tolerantly. Comma decimals, thousands separators, and a trailing unit in the cell are all handled.

## Column Mapping

Every column starts as not imported, so a column is only ever included on purpose.

The first column whose sampled values all parse as a date is pre-selected as the timestamp column. Measurements are never guessed from header text; the date guess is safe only because it is checked against the data.

The step will not continue until the mapping is valid. It reports a missing or duplicated timestamp column, no measurement columns at all, two columns mapped to the same measurement, a body-fat mass column with no weight column to derive from, and a date format that matches none of the sampled rows.

## What Gets Rejected

- A bad timestamp, or a row with fewer columns than the mapping needs, costs the whole row.
- A bad value costs only that measurement, so one unreadable body-fat cell does not throw away a good weight in the same row.
- A blank cell is a gap in the data, not an error. Scales routinely skip measurements.
- Values outside a plausible human range are rejected rather than stored. This is the guard against a mis-mapped column.

## Re-Importing And Duplicates

An imported record is identified by its measurement type and its instant, deliberately not by its value. Health Connect replaces a record when an incoming one carries the same identifier, so:

- Re-importing an unchanged file changes nothing and reports those rows as already present.
- Re-importing a file with a corrected value replaces the old record instead of leaving two measurements at the same instant.
- Re-exporting the same history with reordered columns, or in different units, resolves to the same records.

The trade-off is that two genuinely different measurements at the identical instant collapse into one. CSV-imported records use their own identifier namespace, so they can never collide with Apple Health import records.

If Health Connect stops accepting writes because the app's quota is spent, the import stops and says so. Running it again later starts from the top and recognizes what already landed.

## Reports

The result step can copy the report or save it as a text file. The report contains the file name and outcome, the totals, the parsing settings actually used, the full column mapping including the unit each column was given, rejection counts by reason, and a per-row rejection log. The per-row log is capped so that re-importing a large file cannot make the report unbounded; the grouped counts above it stay complete.

Like the Apple Health import report, this report is an explicit user export and is not sanitized. It names the file, its columns, and the values that were rejected. It contains no health data the user's own CSV did not already contain.

## Running Alongside Other Work

A CSV import runs inside the app rather than as a foreground service, so it can run while an activity is being recorded. The file is streamed row by row and is never held in memory whole.

## Privacy

Imported records are written to Health Connect on the device. Nothing is uploaded, and OpenVitals provides no bulk rollback after records are written.
