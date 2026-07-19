# Activity And Training Plans

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `lib/features/activity/` (`activities_ordered_sections.dart` renders the planned-workout rows), `lib/features/manualentry/activity/` (`activity_training_plan_section.dart`, `activity_plan_picker_cards.dart`).
> **Navigation:** `/activity`, `/metric/WORKOUT`, `/manual_entry/activity?mode=plan` and `/manual_entry/activity?planId=<id>` (a planned-workout row opens its plan directly).
> **Related:** [Feature map](feature-map.md), [Recording of activity](activity-recording.md), [Activity start proposals](../proposals/activity-start-flow.md).

OpenVitals supports both activity entry setup and planned workout context.

## How to use it

### Create an activity from a plan

1. Tap **Log › Activity** (or **Start workout**) to open the **Activity** screen, then choose **Create from existing plan**.
2. In the plan picker, first pick an **activity type that has plans**, then **choose a plan** — each is shown with its name and a set preview (for example `12 reps • rest 60 sec • +3 more`). Selecting it loads the plan into the entry form.
3. Review the pre-filled fields and tap **Save activity**.

### Build an activity by hand

1. From the **Activity** screen, choose **Create manually**.
2. Fill the form: **Activity type**, **Title**, **Start date/time**, **Duration**, and (for strength/rep types) a **Repetitions/Steps** section with a **Total / Sets** toggle — Sets mode adds per-set reps and **Rest time** rows via **Add set**. Add **Distance**, **Climb**, and **calories** where they apply, plus a **How did it feel?** rating and **Notes**.
3. Tap **Save activity**. If the Health Connect **write** permission is missing, use the **Grant** button in the header first; if any field is invalid, fix the highlighted fields before saving.

### Save a reusable plan

For set/rep activities, the form has a **Training plan** dropdown. Choose **New plan**, give it a title, and tap **Save plan** (or **Update plan**) to reuse the structure next time.

### Set a default activity

Pick a **Favorite activity** in **Settings › Activities** (with a "use latest" option) so the entry and recording flows start on the type you use most.

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
