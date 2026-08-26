# Activity And Training Plans

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Implementation:** `features/workoutplans`, `features/activity`, `features/manualentry/activity`.
> **Navigation:** `Screen.WorkoutPlans`, `Screen.WorkoutPlanBuilder`, `Screen.Activity`, `Screen.ActivityEntry`, widget `WORKOUT`, `ManualEntryWidgetId.WORKOUT_PLANS`.
> **Related:** [Feature map](feature-map.md), [Recording of activity](activity-recording.md), [Activity start proposals](../proposals/activity-start-flow.md).

OpenVitals reads and writes Health Connect planned exercise sessions (`PlannedExerciseSessionRecord`), so a routine such as "push-ups 3×10, then 2×45 s plank" can be built once, kept in Health Connect, and started when it is time to train.

## Workout Plan Builder

The builder (`WorkoutPlanBuilderScreen`) edits one plan:

- Session fields: title (required), session type (calisthenics, strength, HIIT, …), date, start time, estimated duration, notes.
- Blocks: each block has an optional name, a number of rounds (`PlannedExerciseBlock.repetitions`), and an ordered list of steps.
- Steps: any Health Connect `ExerciseSegment` type from a searchable picker, with a **repetitions** or **duration (seconds)** goal, plus rest steps with a duration. Planks default to a duration goal; most strength moves default to repetitions. A "Push-ups" preset rides on `EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT` with a stored description because Health Connect has no push-up constant.
- Steps the builder cannot express (distance goals, calorie goals, manual completion, unknown goals, usually from other apps) are shown read-only and written back unchanged, so editing a foreign plan never drops part of it. Performance targets (pace, power, heart-rate zones) are not read or written by OpenVitals today.

Saving writes a `PlannedExerciseSessionRecord`. Updating an existing plan is a delete-then-insert, so the record id changes on every save.

## Plan List

`WorkoutPlanListScreen` groups plans into Today / Upcoming / Past (completed plans always sit in Past) and offers, per plan: **Start** (opens activity entry prefilled from the plan), **Edit**, **Copy to today** (writes a fresh plan for today with the same clock time, length and blocks, which is how a routine gets reused), and **Delete**. Edit and Delete are only offered for plans OpenVitals wrote; plans from other apps can be started or copied.

Entry points: the "Manage" action on the Planned workouts section of the Activities screen, and the Workout plans tile on the manual-entry hub. Both are hidden, and the screens explain why, when the device's Health Connect lacks the planned-exercise feature.

## Starting A Session

Activity entry opens on a **start hub** (`ActivityStartHub`): the uncompleted plans for today and later, each one tap ("Log") from the prefilled form, then **Record activity**, **Log manually**, and a link to Manage plans. Plan rows on the Activities screen and in the plan list open the same prefilled form directly (`planId` route argument).

The manual form no longer edits plans. A session prefilled from a plan shows a **From plan: …** card with Change plan (back to the hub), Edit (opens the builder; the edited plan is picked up again on return, under its new id) and Remove (unlink). Saving writes the session with `plannedExerciseSessionId`, and Health Connect marks the plan completed; editing a session later keeps that link. **Save as plan** on a session built by hand writes a one-block plan from its steps and opens the builder on it — the builder is the only plan editor, so rich plans are never flattened by the form.

## Guided Run

A plan's **Start** (on the start hub, the Activities plan rows, and the plan list) opens the recording setup on that plan — title, step count, estimated length, every step in order — and then walks through it inside the ordinary live recording (`ActivityRecordingController` with a plan cursor in its state, so a killed process resumes mid-step):

- Rep steps count with the phone's sensors where a recognizer exists (push-ups by proximity; pull-ups and jump rope by accelerometer) and end by themselves at the target; the ± buttons and **Done** always work, and **Skip** moves on without recording the step.
- Timed steps (a plank) and rests count down and move on by themselves. **Skip rest** and **Start next** are on screen and in the notification.
- Cues at every change: the rest-timer bell (its existing preference), a short vibration, and — only when voice announcements are on — the next step spoken ("Next: Plank, 45 seconds"), with the current heart rate when a sensor is connected. The last three seconds of a rest beep and buzz, and the banner reads "Get ready"; the screen stays on for the whole run. Exercise names come from string resources (`hc_segment_*`, `hc_exercise_type_*`), so the picker, the banner and the voice all speak the app language.
- The notification shows step, exercise and count or countdown, with a Done / Skip rest action.

- **Back a step** reopens the step just finished with its count restored (a rep the sensor imagined can be taken away with − and the set ended again); a skipped step recorded nothing, so Back past it pops nothing.

Finishing lands in the review form with the steps as actually done (real reps and seconds), linked to the plan; saving completes it in Health Connect. **Log** remains the manual alternative for any plan, and is what Start falls back to for plans the recorder cannot walk through (a running plan, for instance).

## Repeating A Plan

The hub lists the plans completed in the last 30 days (one per title) under **Repeat a recent plan**; Repeat writes today's copy scheduled for now and opens the guided setup on it. The plan list offers **Repeat** on completed plans and **Copy to today** on any plan; a copy keeps the plan's clock time unless it has already gone by, in which case it is scheduled for now. The recording setup reached from the dashboard's *Start workout* shows a **Today's plan** shortcut when a runnable plan is due today. A saved session's detail shows **From plan: …** with a way to the plan. Guided runs are limited to set-based plans (calisthenics, strength); a walking or running plan is logged, not run.

## Steps In The Form

A set is a *step*: an exercise, a Reps or Seconds goal, and a rest. For the generic **Calisthenics** / **Strength sets** types every step names its own exercise (searchable picker from the plan catalog) and "Add exercise" appends one; push-ups, pull-ups and the other single-exercise types keep the plain rows. A plan's push-ups become rep steps and its planks timed steps, and the saved session carries one segment per step (`PLANK` segments last exactly their seconds; rest segments sit between). The plan ↔ steps conversion lives in one place, `features/workoutplans/WorkoutPlanStepMapping.kt`, shared by the form and the builder.

## Activity Defaults

The activity entry and recording flows can use helpful defaults:

- Latest recorded activity type.
- Favorite activity type from Settings.
- Route-capable defaults when starting route workflows.

## Review And Save

Manual, imported, and recorded activities are reviewed before saving. This keeps the dashboard read-only while still allowing explicit writes to Health Connect.

## Fidelity, Backup And Time Zones

Every completion goal Health Connect defines (reps, duration, distance, distance + duration, steps, calories, manual) and every performance target (heart rate, power, speed, cadence, weight, RPE, AMRAP) is read into the domain model and written back unchanged; the builder shows targets under a step ("Targets: HR 140–160 bpm") but does not edit them, and steps with goals it cannot express stay read-only. "Save as plan" from the activity form collapses consecutive identical rows into blocks with rounds, so "push-ups ×3, plank ×2" comes back as two blocks. Plans can be exported to and imported from a JSON file (plan list → Export / Import), which is the backup for a store that otherwise lives only in Health Connect; the file carries goals and targets. A plan's day is read in the zone offset it was written with, so a plan built before travelling stays on its date.

## Follow-ups

- On devices whose Health Connect lacks planned exercise there is no local plan store; export/import is the workaround.
