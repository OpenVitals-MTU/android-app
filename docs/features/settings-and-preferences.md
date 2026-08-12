# Settings And Preferences

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/settings`, `data/repository/PreferencesRepository.kt`.
> **Navigation:** `Screen.Settings` and settings subsection routes; sections in `SettingsSection`.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Metric detail customization](metric-detail-customization.md).

Settings centralize app preferences, Health Connect access, imports, sensors, watches, phone-to-phone sync, goals, reminders, and diagnostics.

## Sections

Settings is a list of section cards. Tapping one opens it:

- Display.
- Activities.
- Sensors and devices, for Bluetooth LE sensors.
- Watches.
- Nutrition.
- Body profile.
- Recovery.
- Data Importers.
- Sync with another phone.
- Health Connect.
- Debug diagnostics, in diagnostics builds only.

The settings root also shows support links, privacy notes, and the app version.

## Display

Users can configure:

- Language.
- Unit system.
- Theme mode, including system, light, dark, and AMOLED, plus dynamic color.
- Chart aggregation, which replaces raw samples with an average line and a min/max band per time bucket.
- Rolling dates mode, such as calendar week/month/year or rolling 7/30/365-day windows.

Display preferences affect app presentation only. They do not rewrite Health Connect records.

## Metric Preferences

Metric-specific settings include:

- Favorite or latest activity defaults used by activity entry and recording setup.
- Split distance used by activity splits.
- Activity recording preferences, including keep-screen-on.
- Calorie data mode, including optional OpenVitals total-calorie calculation when Health Connect totals are missing.
- Hydration goal, set with a stepper in the app's unit system.
- Caffeine sensitivity, daily limit, and bedtime guidance.
- Night start and night end hours, which decide where a night begins and ends and therefore which sessions count as daytime naps.
- High and low heart-rate alert thresholds, in beats per minute, used to flag an unusual resting heart rate.
- Body Energy calibration.

## Body Profile

Body profile is its own section. It holds the facts about the user that personalize Body Energy, heart zones, and caffeine estimates. Every field is optional.

- Birth year.
- Weight, in the selected unit system.
- Height, in centimetres.
- Resting heart rate.
- Maximum heart rate.

Weight and height are shared with health data. When a measurement is available from Health Connect the field is labelled as measured and shows that value, so BMI, FFMI context, and caffeine estimates all use the same number. When nothing has been recorded, the typed value is kept as a local preference and used the same way.

The same section holds the Metabolism card, which used to live behind caffeine settings. It collects the optional factors that change how quickly caffeine is cleared. Leaving them alone uses population averages.

## Watches

The Watches section pairs and manages Garmin and WearOS watches, syncs what a watch recorded, and opens the watch-only data screen. See [Watches](watches.md).

## Sync With Another Phone

Settings, Sync with another phone opens the phone-to-phone sync wizard, which copies supported Health Connect records between two nearby phones over Bluetooth. See [Sync with another phone](device-sync.md).

## Goals And Reminders

Settings expose local goals and reminders for supported metric families, including hydration and mindfulness reminders.

Goals and reminders are local app preferences. They help shape guidance and notifications but do not create Health Connect health records by themselves.

## Health Connect

Health Connect settings show permission categories, missing access, and actions for requesting or opening Health Connect permission management.

OpenVitals asks for read permissions for dashboard and detail views. Write permissions are requested only when a user enters, imports, records, edits, or deletes data that needs them.

The section also holds a Health Connect sync switch and a mindfulness integration switch. Mindfulness integration is off when the device's Health Connect version cannot show the mindfulness permission.

## Data Importers And Sensors

Settings provide Data Importers for Apple Health export import, single or bulk GPX/KML/KMZ/TCX route import, FIT activity/course/workout import, and CSV import of body measurements and vitals, plus entry points for offline map pack import and Bluetooth LE sensor management. Apple Health exports are analyzed first so the user can choose detected categories before anything is written to Health Connect. Route bulk import writes selected files directly after route import permissions are granted. CSV import runs its own mapping wizard; see [CSV import](csv-import.md). Import results can be copied or downloaded as a full text report with summary, selected categories, logs, diagnostics, and failure details.

## Diagnostics And App Information

The settings area includes app version information, diagnostics/support surfaces, and privacy notes.

In diagnostics builds, the Debug diagnostics section can save or share raw process logs, post the hydration reminder notification on demand so its layout and actions can be checked, and list the apps that contributed heart-rate and sleep records to Health Connect over the last seven days.
