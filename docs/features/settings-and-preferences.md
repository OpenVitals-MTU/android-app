# Settings And Preferences

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/settings`, `data/repository/PreferencesRepository.kt`.
> **Navigation:** `Screen.Settings` and settings subsection routes; sections in `SettingsSection`.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Metric detail customization](metric-detail-customization.md).

Settings centralize app preferences, Health Connect access, imports, sensors, goals, reminders, and diagnostics.

## Display

Users can configure:

- Language.
- Unit system.
- Theme mode, including system, light, dark, and AMOLED.

Display preferences affect app presentation only. They do not rewrite Health Connect records.

## Metric Preferences

Metric-specific settings include:

- Activity week mode, such as Monday-to-Sunday or rolling last 7 days.
- Favorite or latest activity defaults used by activity entry and recording setup.
- Calorie data mode, including optional OpenVitals total-calorie calculation when Health Connect totals are missing.
- Sleep range mode, including rolling 24 hours, noon boundary, and evening boundary.
- Caffeine sensitivity, daily limit, and bedtime guidance.
- Body Energy calibration.

## Goals And Reminders

Settings expose local goals and reminders for supported metric families, including hydration and mindfulness reminders.

Goals and reminders are local app preferences. They help shape guidance and notifications but do not create Health Connect health records by themselves.

## Health Connect

Health Connect settings show permission categories, missing access, and actions for requesting or opening Health Connect permission management.

OpenVitals asks for read permissions for dashboard and detail views. Write permissions are requested only when a user enters, imports, records, edits, or deletes data that needs them.

## Data Importers And Sensors

Settings provide Data Importers for Apple Health export import, single or bulk GPX/KML/KMZ route import, and FIT activity/course/workout import, plus entry points for offline map pack import and Bluetooth LE sensor management. Apple Health exports are analyzed first so the user can choose detected categories before anything is written to Health Connect. GPX/KML/KMZ bulk import writes selected route files directly after route import permissions are granted. Import results can be copied or downloaded as a full text report with summary, selected categories, logs, diagnostics, and failure details.

## Diagnostics And App Information

The settings area includes app version information, diagnostics/support surfaces, and privacy notes.
