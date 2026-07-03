# Activity And Training Plans

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/activity`, `features/manualentry/activity`.
> **Navigation:** `Screen.Activity`, `Screen.ActivityEntry`, widget `WORKOUT`.
> **Related:** [Feature map](feature-map.md), [Recording of activity](activity-recording.md), [Activity start proposals](../proposals/activity-start-flow.md).

OpenVitals supports both activity entry setup and planned workout context.

## Planned Workouts

When Health Connect provides planned exercise data and permissions are granted, OpenVitals can show planned workouts in activity views. Planned workouts are read-only; OpenVitals does not edit plans created by other apps.

## Activity Defaults

The activity entry and recording flows can use helpful defaults:

- Latest recorded activity type.
- Favorite activity type from Settings.
- Route-capable defaults when starting route workflows.

These defaults reduce repeated setup without hiding the final review step.

## Training Structure

Activity entries and recordings can include repetition-oriented details such as total repetitions, sets, rest minutes, and strength-training context where Health Connect supports the saved data.

## Review And Save

Manual, imported, and recorded activities are reviewed before saving. This keeps the dashboard read-only while still allowing explicit writes to Health Connect.
