# Activity Start Flow Analysis

> **Status: MOSTLY IMPLEMENTED.** This analysis was written against the retired Kotlin app. Five of its seven simplification ideas shipped in the Flutter app and are no longer proposals — see [What has since shipped](#what-has-since-shipped) below. The "Current Entry Points", "Recording Flow", "Manual Activity Flow" and "Planned Workout Flow" sections below describe the **pre-simplification** flow and are kept as the analysis that motivated the change; they are **not** a description of the app today. Current implemented behavior is [Recording of activity](../features/activity-recording.md).
> **Implementation map:** [Feature map](../features/feature-map.md).

This document maps the paths for starting or creating an activity, then proposes ways to reduce taps and make the intent clearer.

For a clearer breakdown of each proposed improvement, see [Activity start flow simplification proposals](activity-start-flow-proposals.md).

## What Has Since Shipped

Verified against the Flutter code on 2026-07-11:

| Idea | Status | Where |
|---|---|---|
| 1. Intent-specific activity entry routes | **Shipped** | `AppRoutes.activityEntryLocation({mode, planId, activityTypeId})` builds `/manual_entry/activity?mode=record\|manual\|plan&planId=…&activityTypeId=…`; the router parses all three ([`app_router.dart`](../../lib/navigation/app_router.dart)) and `ActivityEntryController._applyLaunchIntent` acts on them ([`activity_entry_notifier.dart`](../../lib/features/manualentry/activity/activity_entry_notifier.dart)) |
| 2. Merge GPS setup and first start | **Shipped** | The setup screen's primary button is `Start` and calls `startGpsRecording(initialFix)` directly; there is no intermediate idle dashboard on the common path ([`activity_recording_setup_screen.dart`](../../lib/features/manualentry/activity/recording/activity_recording_setup_screen.dart), `_startRecording` in [`activity_entry_screen.dart`](../../lib/features/manualentry/activity_entry_screen.dart)) |
| 3. Replace source chooser with a start sheet | **Not implemented** | No bottom sheet exists; the source chooser is still the fallback when no launch intent is supplied |
| 4. Make planned workout rows actionable | **Shipped** | A planned-workout row pushes `activityEntryLocation(planId: id)` ([`activities_ordered_sections.dart`](../../lib/features/activity/activities_ordered_sections.dart)) |
| 5. Auto-skip single-choice plan steps | **Shipped** | `_autoAdvancePlanSelection` applies the only selectable plan, or the only activity type, without asking ([`activity_entry_notifier.dart`](../../lib/features/manualentry/activity/activity_entry_notifier.dart)) |
| 6. Split dashboard actions by intent | **Shipped** | The dashboard's two actions are `Log` (→ `/manual_entry`) and `Start workout` (→ `/manual_entry/activity?mode=record`, straight into recording setup) ([`dashboard_screen.dart`](../../lib/features/dashboard/dashboard_screen.dart)) |
| 7. Continue active or draft recordings first | **Partial** | The activity entry screen restores an in-progress recording draft when reopened (`recordingDraftStore`, `isRecordingDraft`), but the dashboard has no "continue recording" affordance |

## Current Entry Points

> Historical: this section describes the Kotlin-era flow, before ideas 1, 2, 4, 5 and 6 shipped.

Activity creation is routed through the activity-entry destination (`/manual_entry/activity`). Without a launch intent that destination still starts in `ActivityEntryFormMode.chooseSource`, so a path that supplies no `mode` / `planId` / `activityTypeId` lands on a source chooser before it can record, create a manual entry, import a route, or use a planned workout.

Relevant code touchpoints (Flutter):

- [`lib/navigation/app_routes.dart`](../../lib/navigation/app_routes.dart): the `/manual_entry/activity` path, `ActivityEntryMode`, and `activityEntryLocation(...)`.
- [`lib/navigation/app_router.dart`](../../lib/navigation/app_router.dart): activity entry route wiring (`_manualEntryRoutes`).
- [`lib/features/dashboard/dashboard_screen.dart`](../../lib/features/dashboard/dashboard_screen.dart): the dashboard `Log` and `Start workout` buttons.
- [`lib/features/manualentry/activity_entry_screen.dart`](../../lib/features/manualentry/activity_entry_screen.dart): permission gates, source actions, and the mode-to-screen switch.
- [`lib/features/manualentry/activity/activity_entry_notifier.dart`](../../lib/features/manualentry/activity/activity_entry_notifier.dart) and [`activity_entry_state.dart`](../../lib/features/manualentry/activity/activity_entry_state.dart): `ActivityEntryFormMode` and the launch-intent handling.
- [`lib/features/manualentry/activity/recording/activity_recording_setup_screen.dart`](../../lib/features/manualentry/activity/recording/activity_recording_setup_screen.dart): pre-recording setup.
- [`lib/features/manualentry/activity/recording/activity_recording_screen.dart`](../../lib/features/manualentry/activity/recording/activity_recording_screen.dart): live recording dashboard.
- [`lib/features/activity/activities_ordered_sections.dart`](../../lib/features/activity/activities_ordered_sections.dart): the planned-workout list (now actionable).

## Recording Flow

The dashboard has a primary `Start` action, but that action still opens the generic activity entry source chooser. From there, the user picks recording, confirms setup, and for GPS activities taps start again inside the recording dashboard.

```mermaid
flowchart TD
    A[Dashboard] -->|Start| B[Activity Entry]
    C[Activities screen or workout metric] -->|Add| B
    D[Manual Entry hub] -->|Activity tile| B
    B --> E[Choose source]
    E -->|Record GPS activity| F{Permission checks}
    F -->|Needs Health Connect write| G[Health Connect permission request]
    F -->|Needs notification permission| H[Android notification request]
    G --> I[Recording setup]
    H --> I
    F -->|Already granted| I
    I -->|Select/change activity type| I
    I -->|Go to activity screen| J{Recording kind}
    J -->|GPS route| K[Idle recording dashboard]
    K -->|Start| L[Recording active]
    J -->|Timed or repetition| L
    L -->|Finish| M[Review draft in manual form]
    M -->|Add| N[Save activity to Health Connect]
```

Minimum taps after the dashboard is visible:

- GPS recording: `Start` -> `Record GPS activity` -> `Go to activity screen` -> `Start` = 4 taps, before finish/review/save.
- Timed or repetition recording: `Start` -> `Record GPS activity` -> `Go to activity screen` = 3 taps, because setup starts those recordings immediately.
- From `Log`/manual entry hub: add one extra tap for the Activity tile.
- Extra taps appear for permission prompts, choosing a non-default activity type, changing recording settings, finishing, reviewing, and saving.

Main friction:

- `Start` does not start directly; it opens the source chooser.
- GPS recording has two start-like decisions: setup's `Go to activity screen`, then the dashboard's `Start`.
- The source chooser treats manual entry, plan use, route import, and recording equally even when the user came from a `Start` affordance.
- The source action is named `Record GPS activity`, but the recording stack also supports timed and repetition activities.

## Manual Activity Flow

Manual activity creation can be reached through the dashboard `Log` action, the dashboard `Start` action, the activities screen add action, or the metric top-bar add action. The clearest user path is `Log`, but it is not the shortest because it opens the manual-entry hub first.

```mermaid
flowchart TD
    A[Dashboard] -->|Log| B[Manual Entry hub]
    B -->|Activity tile| C[Activity Entry]
    D[Dashboard] -->|Start| C
    E[Activities screen or workout metric] -->|Add| C
    C --> F[Choose source]
    F -->|Create manual| G[Manual activity form]
    G -->|Fill or adjust activity fields| G
    G -->|Add| H[Save activity to Health Connect]
```

Minimum taps:

- From dashboard `Log`: `Log` -> `Activity` -> `Create manual` -> `Add` = 4 taps, plus data entry.
- From dashboard `Start`: `Start` -> `Create manual` -> `Add` = 3 taps, plus data entry, but the label does not communicate manual logging.
- From activities screen add action: `Add` -> `Create manual` -> `Add` = 3 taps, plus data entry.

Main friction:

- The shortest manual route is hidden behind a button labeled `Start`.
- The clearer `Log` route has an extra manual-entry hub step.
- The activity form includes the `Choose another source` button, which is useful for recovery but reinforces that the user is inside a multi-source wizard rather than a direct log flow.

## Planned Workout Flow

Planned workouts are visible in the activities overview, but the rows are read-only. To use a plan, the user must open activity entry, choose the plan source, choose an activity family, choose the plan, then save the prefilled manual entry.

```mermaid
flowchart TD
    A[Activities overview] --> B[Planned workout list]
    B -->|Read-only today| Z[No direct action]
    C[Dashboard or activities screen] -->|Start/Add| D[Activity Entry]
    D --> E[Choose source]
    E -->|Create from existing plan| F[Load planned workouts]
    F --> G[Choose planned workout activity]
    G --> H[Choose planned workout]
    H --> I[Manual activity form prefilled from plan]
    I -->|Add| J[Save activity linked to planned workout]
```

Minimum taps:

- From dashboard `Start`: `Start` -> `Create from existing plan` -> activity type -> plan -> `Add` = 5 taps, plus any field review.
- From activities screen add action: `Add` -> `Create from existing plan` -> activity type -> plan -> `Add` = 5 taps, plus any field review.
- From the visible planned workout row: no direct path exists.

Main friction:

- The most contextual object, the planned workout row, cannot be acted on.
- The plan flow always asks for activity type first, even if there is only one planned activity type.
- The plan flow lands in manual entry, not a dedicated `Start this plan` experience.
- If the user saw a plan in the activities overview, they must find it again in the source picker flow.

## Simplification Ideas

### 1. Add Intent-Specific Activity Entry Routes

Add route arguments or helper routes that open `ActivityEntryScreen` in a target mode:

- `manual_entry/activity?mode=record`
- `manual_entry/activity?mode=manual`
- `manual_entry/activity?mode=plan`
- `manual_entry/activity?planId=...`
- `manual_entry/activity?activityTypeId=...`

Then the dashboard `Start` button can open recording setup directly, the activities add action can open manual entry directly, and planned workout rows can open a selected plan directly.

Impact:

- GPS recording can drop from 4 taps to 3 before any deeper recording simplification.
- Manual activity from dashboard can drop from 4 taps to 2 plus data entry.
- Planned workout use can drop from 5 taps to 2 if a row opens the selected plan.

### 2. Merge GPS Setup And First Start

For GPS-capable activities, the setup screen already waits for a precise location fix. When the user taps the primary setup button, call `startGpsRecording(initialFix)` and enter the active dashboard instead of preparing an idle dashboard first.

Impact:

- GPS recording drops one tap immediately.
- The button can say `Start` instead of `Go to activity screen`.
- The idle dashboard can still exist for pause/resume/editing, but it no longer blocks the common path.

### 3. Replace Source Chooser With A Start Sheet

Use a compact sheet or panel opened from `Start` that prioritizes likely actions:

- Start last activity.
- Start favorite activity.
- Plans due today.
- Manual activity.
- Import route.

This keeps all options available while putting the fastest path at the top. It also avoids a full route transition before the user chooses intent.

### 4. Make Planned Workout Rows Actionable

Turn `PlannedWorkoutRow` into an actionable row with a primary action:

- `Start` for uncompleted plans that map to a live-recordable activity.
- `Log` or `Use plan` for plans that should prefill manual entry.
- `View` or disabled completed state for completed plans.

The row can pass `planId` into the activity entry route, prefill the plan, and skip the activity/plan picker screens.

### 5. Auto-Skip Single-Choice Plan Steps

After loading existing plans:

- If there is only one activity type, skip `PLAN_ACTIVITY_PICKER`.
- If there is only one plan for the selected type, apply it immediately.
- If a plan is scheduled for now or today, preselect it at the top.

This is a low-risk improvement because it preserves the existing screens for ambiguous cases.

### 6. Split Dashboard Actions By Intent

Keep the dashboard simple, but make labels match intent:

- Primary: `Start workout` opens recording setup directly.
- Secondary: `Log` opens a compact log menu or the manual-entry hub.
- Optional overflow: `Import route`, `Use plan`, recent/favorite activities.

This removes the current ambiguity where `Start` is the shortest path to manual activity but not the clearest one.

### 7. Continue Active Or Draft Recordings First

If an activity recording is active or an unsaved recording draft exists, the dashboard `Start` action should reopen that recording/draft before offering a new source. This avoids making the user rediscover an in-progress workflow.

## Suggested Implementation Order

Steps 1-6 are **done** in the Flutter app; they are kept here as the record of the order that was followed.

1. ~~Add route/start intent support to the activity-entry controller and the `/manual_entry/activity` route.~~ Done — `ActivityEntryController.launchMode` / `launchPlanId` / `launchActivityTypeId`, fed by `AppRoutes.activityEntryLocation(...)`.
2. ~~Wire dashboard `Start` to open recording setup directly with the preferred live activity.~~ Done — `Start workout` pushes `?mode=record`.
3. ~~Change GPS setup so the primary action starts recording when a fix is ready.~~ Done — setup's `Start` calls `startGpsRecording(initialFix)`.
4. ~~Make planned workout rows actionable and route by `planId`.~~ Done — the row pushes `?planId=…`.
5. ~~Auto-skip single-choice plan picker states.~~ Done — `_autoAdvancePlanSelection`.
6. ~~Revisit dashboard quick action copy and layout once the direct routes exist.~~ Done — `Log` / `Start workout`.

Still open: the start sheet (idea 3) and a dashboard-level "continue in-progress recording" affordance (idea 7).
