# CSV Import

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/imports/csv`, `features/settings`.
> **Navigation:** `Screen.SettingsDataImport`, then `Screen.SettingsCsvImport`.
> **Related:** [Feature map](feature-map.md), [Settings and preferences](settings-and-preferences.md), [Body metrics](body-metrics.md), [Apple Health import](apple-health-import.md).

CSV import brings body measurements, vitals, steps and nutrition from a plain CSV file into Health Connect. It is meant for exports that have no importer of their own, such as a smart scale's weight and body-composition history, a temperature log, a glucose export, a pedometer's step history, or a spreadsheet of meals copied from nutrition labels.

There is one vendor preset, the drip cycle tracker's export, recognised by its header; see [drip Export](#drip-export). Every other file is read through the column mapping: the user says what each column means, which is why a file OpenVitals has never seen still imports.

## Import Flow

CSV import lives in Settings, Data Importers, CSV Importer, and opens its own five-step screen.

1. **Choose a file.** The file picker accepts CSV and plain-text documents. Only the head of the file is read at this point, so a large export opens immediately and nothing is written yet.
2. **Map the columns.** OpenVitals shows the detected column separator, a "first row contains column names" switch, a short preview table, the date and time settings, and one row per column. Each column is set to not imported, date and time, an optional end date and time, a supported measurement, a nutrient, or a food name. A measurement or nutrient column also picks what its numbers are.
3. **Confirm.** The confirm step shows the date range the file covers and the value range each measurement and nutrient will actually be written with, so a mis-mapped column is visible before anything lands. Missing Health Connect write permissions are requested here.
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

Activity:

- Steps.

Nutrition: every nutrient Health Connect stores, listed under the same sections as the nutrition screen. See [Nutrition](#nutrition) below.

Most supported measurements are a single value at a single instant, because that is the shape the column mapping expresses: one row, one timestamp, one number per column. Steps is the one interval metric: each row spans from its date and time to the optional "End date and time" column. When there is no end - the column is unmapped, the cell is blank, or the row is truncated before it - the row counts as one minute. An end that cannot be read, or that is not after the start, rejects only that row's steps; the instant measurements beside it still import.

## What Is Not Supported

- **Blood pressure is deliberately unsupported.** Systolic and diastolic have to become one Health Connect record, which needs a two-columns-to-one-record rule the mapping model does not have.
- **Interval records beyond steps and nutrition are not supported.** Sleep and workouts need stages or sub-records as well as a start and an end, so they are out of scope for the current mapping model. Steps and nutrition are the exceptions, carried by the single optional "End date and time" column.
- **Meal type is not mapped.** An imported food has no breakfast, lunch, dinner or snack label.

## drip Export

The cycle tracker [drip](https://gitlab.com/bloodyhealth/drip) exports its history as a CSV with one row per day and dotted column names such as `bleeding.value` and `temperature.time`. OpenVitals recognises that header when the file is picked and skips the column mapping: the mapping step becomes a set of switches for what to bring over, and the confirm step shows how many of the first rows hold something to import and the dates they span. Days are read in the phone's time zone, since drip stores local days, and each day's records are placed at noon, the same instant the cycle day log gives a past day.

What each switch imports:

- **Bleeding.** drip's light, medium and heavy become a menstruation flow record of the same level. drip's spotting, and any bleeding marked in drip as not menstrual, become an intermenstrual bleeding record.
- **Basal body temperature.** The morning temperature at its recorded clock time, or noon when drip has none. A temperature excluded in drip was judged unreliable there and is left out.
- **Cervical mucus.** drip's texture names the appearance (creamy, egg white); when the texture says nothing, the feeling stands in (dry, wet as watery, slippery as egg white). The feeling also fills the journal's cervical sensation. An observation excluded in drip is skipped.
- **Sexual activity.** A day with partner or solo sex. Protected when drip lists a contraceptive, unprotected when it says none, unknown otherwise.
- **Journal.** drip's pain and mood switches become symptoms (migraine counts as a headache, ovulation pain as pelvic pain outside the period; sad, stressed and angry as mood changes; anxious and fatigue as themselves). The mood switches also set the journal's mood level, 4 for a good day, 2 for a hard one, 3 for both, and energetic or fatigue set the energy level the same way. The cervix comes over as felt, opening, firmness and position, unless excluded in drip; desire's low, medium and high become 2, 3 and 4 on the journal's scale. Every note, drip's day note and the notes on temperature, sex, pain and mood, goes into the day's notes.

The journal needs no Health Connect permission: it is written to this phone's cycle journal as the rows are read, laid over whatever the day already holds. Symptoms are added, levels, the sensation and the cervix are filled only where drip has them, note lines already present are not repeated, and a day drip would not change keeps its edit time. The result and the report count the journal days updated separately from the Health Connect records.

Re-importing a drip export replaces its records like any other CSV, since a day's record of each kind resolves to the same identifier. A day that changes kind between exports, from flow to spotting, leaves the earlier record in place.

## Nutrition

Nutrients are mapped one column each, like measurements, but they do not become one record each. Every nutrient column of a row folds into one Health Connect nutrition record: one food, or one day's totals, depending on what a row of the spreadsheet holds. A food-name column is optional and names the record; without one, the nutrition screen shows the entry under its meal type.

The column menu lists every nutrient Health Connect stores, under the nutrition screen's own sections: energy, protein, carbohydrates and fat; dietary fibre and sugar; energy from fat and the fats, including cholesterol; the vitamins; the minerals; and caffeine.

Each nutrient is read in the unit its nutrition label prints by default - kcal for energy, g for the macros, mg for vitamin C or sodium, µg for vitamin D or B12 - the same units the food entry form types in. A column can also be read in kilojoules, or in any of grams, milligrams and micrograms, and a header such as `Sodium (mg)`, `Energy (kJ)` or `Vitamin D (mcg)` pre-selects its unit. Health Connect stores kilocalories and grams either way.

A food spans from the row's date and time to the "End date and time" column, or one minute without one, like steps. An end that cannot be read, or is not after the start, rejects that row's food only; measurements in the same row still import.

A blank nutrient cell is a gap, and so is a zero: a label's "0 g" adds nothing, and a row whose amounts are all blank or zero writes no food. A cell that is not a number, or is negative, costs that nutrient only. Amounts above a plausible ceiling are rejected rather than stored: 10,000 kcal or 10,000 g for energy and the macros, and 100 g of any vitamin or mineral, which is Health Connect's own limit for one record.

Imported foods appear on the nutrition screen and its charts like any other entry. They can be deleted there but not edited, like a logged food from the catalog: correct the spreadsheet and import it again instead. They never appear among the drinks on the hydration screen, even when one carries caffeine.

## Units Come From The File

The unit chosen for a column describes the file, not the app's display unit system. A file in pounds imports correctly while the app displays kilograms, and neither setting affects the other. Values are converted once, on import, and stored in the units Health Connect uses.

Accepted units per measurement include kilograms, pounds, stones, and grams for masses; centimetres, metres, inches, and feet for height; kilocalories and kilojoules per day for basal metabolic rate; Celsius and Fahrenheit for temperatures; millimoles per litre and milligrams per decilitre for glucose; milliseconds and seconds for HRV; and percent or a 0–1 fraction where a share is meaningful.

Nutrients take kilocalories or kilojoules for energy and grams, milligrams or micrograms for the rest.

When a header names its unit in parentheses, such as `Weight (kg)`, `value (°C)` or `Sodium (mg)`, that unit is pre-selected. It stays an editable default.

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

The step will not continue until the mapping is valid. It reports a missing or duplicated timestamp column, a duplicated end date and time column, a duplicated food-name column, no measurement or nutrient columns at all, two columns mapped to the same measurement or nutrient, a body-fat mass column with no weight column to derive from, and a date format that matches none of the sampled rows.

## What Gets Rejected

- A bad timestamp, or a row with fewer columns than the mapping needs, costs the whole row.
- A bad value costs only that measurement or nutrient, so one unreadable body-fat cell does not throw away a good weight in the same row, and one unreadable sodium cell does not throw away the rest of the food.
- A blank cell is a gap in the data, not an error. Scales routinely skip measurements.
- Values outside a plausible human range are rejected rather than stored. This is the guard against a mis-mapped column.

## Re-Importing And Duplicates

An imported record is identified by its measurement type and its instant, deliberately not by its value. A steps row uses its start instant, like every other metric, so a re-import with a corrected end replaces the record. Health Connect replaces a record when an incoming one carries the same identifier, so:

- Re-importing an unchanged file changes nothing and reports those rows as already present.
- Re-importing a file with a corrected value replaces the old record instead of leaving two measurements at the same instant.
- Re-exporting the same history with reordered columns, or in different units, resolves to the same records.

The trade-off is that two genuinely different measurements at the identical instant collapse into one.

Foods are the exception, because two foods eaten at the same time are both real. A food is identified by its start and by its place among the file's foods at that same start: the first food at 12:30 keeps the plain identifier, the second gets its own. The same file numbers them the same way on every run, so a re-import still replaces rather than adds, and a corrected amount, a renamed food, or a reordered row at the same time ends up as the file says. The one thing a re-import cannot undo is a food deleted from the file: the record from the earlier import stays until it is deleted in the app.

CSV-imported records use their own identifier namespace, so they can never collide with Apple Health import records.

If Health Connect refuses a batch because the app's write quota is spent, the import does not stop: it pauses, says how long until the next attempt, and writes the same batch again. The pauses grow from 15 seconds to 5 minutes, since the quota refills continuously; an accepted write resets them. A million-row heart rate export is a few thousand writes, more than one quota window allows, so a large file simply takes longer and the report counts the pauses and the time they took. Cancel works during a pause. Only when half an hour of pausing brings no accepted write does the import stop and say so; running it again later starts from the top and recognizes what already landed.

## Reports

The result step can copy the report or save it as a text file. The report contains the file name and outcome, the totals, the parsing settings actually used, the full column mapping including the unit each column was given, rejection counts by reason, and a per-row rejection log. The per-row log is capped so that re-importing a large file cannot make the report unbounded; the grouped counts above it stay complete.

Like the Apple Health import report, this report is an explicit user export and is not sanitized. It names the file, its columns, and the values that were rejected. It contains no health data the user's own CSV did not already contain.

## Running Alongside Other Work

A CSV import runs inside the app rather than as a foreground service, so it can run while an activity is being recorded. The file is streamed row by row and is never held in memory whole.

## Privacy

Imported records are written to Health Connect on the device. Nothing is uploaded, and OpenVitals provides no bulk rollback after records are written.
