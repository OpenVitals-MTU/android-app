# Settings And Preferences

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/settings/`, `lib/data/prefs/preferences_repository.dart`, `lib/state/app_providers.dart`.
> **Navigation:** `/settings` plus one pushed route per `SettingsSection` (`lib/features/settings/presentation/settings_section.dart`): `/settings/display`, `/activities`, `/sensors`, `/watches`, `/nutrition`, `/recovery`, `/data_import`, `/health_connect`, and `/debug_diagnostics` in diagnostics builds only.
> **Related:** [Feature map](feature-map.md), [Permissions](../app/permissions.md), [Metric detail customization](metric-detail-customization.md).

Settings centralize app preferences, Health Connect access, imports, sensors, goals, reminders, and diagnostics.

## How to use it

Open **Settings** from the gear icon in the dashboard's top bar. It's a list of section cards; tap one to open it. The footer shows your app **version**.

| Section | What you change there |
| --- | --- |
| **Display** | Language, **Units** (Metric / Imperial), **Theme** (System / Light / Dark / AMOLED), Dynamic color, chart aggregation. |
| **Activities** | Calendar vs **Rolling** date windows, **Favorite activity**, split distance, recording preferences (incl. keep-screen-on), and **Offline maps** import. |
| **Sensors & devices** | Pair and manage **Bluetooth LE** heart-rate, cadence, and power sensors — see [Bluetooth LE sensors](ble-sensors.md). |
| **Watches** | Pair a Garmin watch, sync what it recorded, and change its alarms and on-watch settings — see [Garmin watch sync](garmin-watch-sync.md). |
| **Nutrition** | **Total calories** mode, **Hydration goal** stepper, and the **Caffeine model** — see [Beverage logging and caffeine](beverage-logging-and-caffeine.md). |
| **Recovery** | **Sleep range** boundary, high/low heart-rate alerts, body profile, and **Body Energy calibration** — see [Body Energy](body-energy.md). |
| **Data Importers** | **Apple Health**, **route file** (GPX/KML/KMZ/TCX), and **FIT** imports. |
| **Health Connect** | **Sync** on/off, **App lock**, and the permission categories to grant or review later. |
| **Debug diagnostics** | Save/share sanitized logs — only present in diagnostics builds. |

A couple of things people expect in Settings but that live elsewhere: **hydration and mindfulness reminders** are on their metric detail screens (see [Reminders](reminders.md)), and the dashboard/metric-section **layout** is edited on those screens (see [Metric detail customization](metric-detail-customization.md)).

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

Settings provide Data Importers for Apple Health export import and FIT activity/course/workout import, plus entry points for offline map pack import and Bluetooth LE sensor management. Apple Health exports are analyzed first so the user can choose detected categories before anything is written to Health Connect. Import results can be copied or downloaded as a full text report with summary, selected categories, logs, diagnostics, and failure details.

## Diagnostics And App Information

The settings area includes app version information, diagnostics/support surfaces, and privacy notes.
