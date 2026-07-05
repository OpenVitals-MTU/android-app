# Privacy

OpenVitals is designed as a local-first app. The local Android app is intentionally separate from any connected app work.

The local app:

- Does not ship app-level internet permission.
- Does not create OpenVitals cloud accounts.
- Does not upload health data to an OpenVitals server.
- Does not include ads.
- Does not include an analytics SDK.
- Reads supported records through Health Connect.
- Writes supported records only after an explicit save, import, record, edit, or delete action.

Local app preferences may include onboarding state, acknowledged permission prompts, unit settings, language, theme, widget order, goals, reminders, Body Energy calibration, timer settings, and display choices.

## Health Records

Health Connect is the source of truth. OpenVitals reads Health Connect records to show dashboard summaries, detail screens, readiness, statistics, achievements, and insights.

Manual entries are saved back to Health Connect only when the user chooses to save them. Supported Apple Health export records are written to Health Connect only when the user imports an export file from Settings. OpenVitals-created records can be edited later; records created by other apps remain read-only.

Apple Health exports are analyzed locally before the user chooses which categories to import. Import reports are generated locally when the user runs an import and chooses to copy or download the report. These troubleshooting reports intentionally include full importer logs, selected categories, diagnostics, and exception details, so users should review them before sharing outside their device.

## Sensitive Data

Cycle tracking uses sensitive Health Connect records and is shown only after cycle permissions are granted explicitly.

Workout route previews require manual Health Connect approval in some cases. GPS recording requires location permission because OpenVitals needs location points to build the route. Imported PMTiles or Mapsforge map packs stay local on the device.

## Permission Details

See [Permissions](permissions.md) for the current permission list and why each group is requested.

See [Local And Connected Editions](local-and-connected-editions.md) for the boundary between the local app and planned connected work.
