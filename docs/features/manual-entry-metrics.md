# Manual Entry Of Metrics

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/manualentry/` — the hub (`manual_entry_screen.dart`, which also declares `ManualEntryWidgetId`), the shared `manual_entry_form_scaffold.dart`, per-metric entry screens and notifiers (hydration, carbs, body, vitals, mindfulness, activity), plus the `activity/` and `mindfulness/` subdirectories.
> **Navigation:** `/manual_entry` plus the entry routes `/manual_entry/{hydration,carbs,activity,mindfulness}`, `/manual_entry/body/:bodyMeasurementType`, `/manual_entry/vitals/:vitalsMeasurementType`, and their `/edit/:id` variants.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Beverage logging and caffeine](beverage-logging-and-caffeine.md).

Manual entry flows let the user write explicit records to Health Connect. OpenVitals does not keep a separate health database for these records.

## How to use it

1. **Open the Add-entry hub.** Tap **Log** on the dashboard. The **Add entry** screen shows a grid of tiles: **Hydration, Carbs, Activity, Mindfulness, Weight, Height, Body fat, Blood pressure, Blood oxygen, Respiratory rate, Body temperature**. Only entry types your Health Connect provider can accept are shown.
2. **Fill the form.** Tap a tile to open its form. Fields are **unit-aware** — a weight field reads "Weight (kg)" or "Weight (lb)" to match your unit system, and what you type is converted back to metric on save (a comma works as a decimal point). Blood pressure has two fields (systolic and diastolic).
3. **Set the time where offered.** Body measurements and vitals let you pick the **date and time**; a brand-new vitals entry and a carbs entry are stamped "now".
4. **Save.** The save button names the action (for example **Add Weight**). If the app is missing the Health Connect **write** permission for that type, the form shows a **Grant** button and saving is disabled until you grant it.
5. **Reorder your tiles.** Tap the **pencil (Edit entries)** on the hub to drag tiles into order, remove ones you don't use, and restore them from the **Add entry widgets** tray.

### Edit or delete an entry

- Open the metric's detail screen (from its dashboard tile). Entries **you logged in OpenVitals** are editable and deletable; records from other apps are plain read-only rows.
- **Edit:** tap an owned row to reopen its form, prefilled, and **Save** the change.
- **Delete:** swipe an owned row from right to left. There is no confirmation dialog — the delete is immediate and only reappears if Health Connect rejects it.
- **Carbs** entries are create-only (no edit); a **hydration** entry's edit changes only its timestamp, not the amount.

## Weight

Weight is a core manual metric flow. The user enters a value and saves it to Health Connect. OpenVitals-created weight entries can later be edited or deleted when the app has the required write permission. Weight data also feeds body detail screens, BMI, body composition context, and statistics.

## Supported Manual Entries

OpenVitals supports explicit logging for:

- Beverages/hydration, including drink/container choices, custom amounts, caffeine-aware presets, and selected nutrition defaults.
- Carbohydrate totals as Health Connect nutrition records.
- Activity sessions, optionally with routes, distance, elevation, calories, repetitions, title, and notes.
- Mindfulness sessions through a timer or manual duration.
- Body measurements such as weight, height, and body fat.
- Vitals such as blood pressure, SpO2, respiratory rate, and body temperature.

## Permission Handling

Write permissions can be granted during onboarding or requested when an entry flow needs them. The dashboard remains read-only even if write permissions have already been granted.

## External Records

Records created by other apps stay read-only in OpenVitals. OpenVitals checks ownership before allowing edits or deletes of records it created.
