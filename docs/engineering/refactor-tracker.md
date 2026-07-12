# Architecture refactor tracker

Working checklist for the Flutter app-architecture conformance refactor
(MVVM roles on Riverpod, `*ViewModel` naming, `Result<T>` end-to-end,
display derivation in view-models, `CommandState` for write flows).
Archived at closeout — this is a scratchpad, not documentation; the durable
rules live in `architecture.md` / `AGENTS.md` / `feature-playbook.md`.

Legend: `—` not started · `~` in progress · `x` done · `n/a` not applicable

## Phases

| # | Phase | Status |
|---|-------|--------|
| 0 | Foundations (Result, AppFailure, runCatching, CommandState, toScreenError) | x |
| 1 | Rename Notifier→ViewModel + application/presentation layout everywhere | x |
| 2 | Result through repositories + use-cases | x |
| 3 | VMs on Result + CommandState; seam reversal templates | x |
| 4 | Seam reversal, all remaining features + freezed state conversions | x |
| 5 | Vitals god-file split | x |
| 6 | Layer hygiene (permissions→domain, widget→repo cleanup, background DI helper) | x |
| 7 | DI split with barrel export | x |
| 8 | Offline map import → Result + CommandState | x |
| 9 | ActivityEntry controller → ViewModel | x |
| 10 | Recording service/VM split | ~ |
| 11 | Closeout (docs, bridge audit) | ~ |

## Feature matrix

All 21 features now have: the `application/` + `presentation/` layout,
`*ViewModel` naming, `Result`-based repositories and use-cases, the display
precomputed in the view-model, a freezed state, and a view-model unit test.
**Complete** — the only open items are the two below.

## Still open

- **Recording (Phase 10).** `activity_recording_controller.dart` — the
  foreground service, its notification comm port, and process-death draft
  recovery. Being split into a byte-identical `ActivityRecordingService` plus a
  thin view-model. **Needs on-device verification before it ships**: start →
  background → lock → notification pause/resume → process-death recovery →
  stop/save → entry appears.
- **BLE repository contract.** Features still import
  `data/source/sensors/ble/` directly — `dashboard_sensor_status.dart`,
  `settings/application/ble_devices_view_model.dart`,
  `manualentry/activity/activity_entry_providers.dart`, and the recording
  controller. The BLE stack has no `contract/` type wrapping
  `BleSensorCoordinator`. Deferred behind Phase 10, because the recording
  controller is one of its four consumers and both would have to move together.

## Bridges — audited, deliberately NOT deleted

The plan said to delete `orThrow()` and `throwableToScreenError` at closeout.
That turned out to be wrong. The audit is recorded here so that nobody
"finishes the job" by mistake:

- **`orThrow()`** is the adapter to the parts of Dart that signal failure by
  throwing. Legitimate callers: a `FutureProvider` body (its `AsyncError`
  channel already *is* this type — see `health_connect_gate.dart`), a
  background-isolate entrypoint (no screen to render a `ScreenError` onto, and
  it must fail loudly rather than silently), and a repository composing another
  repository *inside* its own `runCatching`. Anywhere else, switch on the
  `Result`.
- **`throwableToScreenError`** is for collaborators that genuinely throw
  because they are not repositories and have no `Result` to return: the route
  file parser handed a malformed file, the platform report saver.

No view-model maps a load failure through either one.

## Bugs found while moving derivations VERBATIM — not fixed

Each was moved unchanged and deserves its own commit. Moving them out of build
methods is what made them visible (several were duplicated in two places that
disagreed); changing them in the same commit would have made the refactor
unreviewable.

1. **Sleep data-confidence counts the wrong recording method.**
   `kRecordingMethodManualEntry = 1`, but Health Connect's
   `RECORDING_METHOD_MANUAL_ENTRY` is `3` (`1` is `ACTIVELY_RECORDED`).
   `sleep_detail_screen.dart` already gets this right. So the card counts
   actively-recorded nights as hand-typed ones, and never counts a real one.
2. **Heart day resting-HR / HRV can print an average outside its own printed
   range** — the average comes from the provider's aggregate, the min/max from
   raw samples.
3. **Respiratory rate shows two different averages** on the same screen: the
   chart summary uses the mean of daily means, the card the mean of all
   readings.
4. **Skin temperature** counts delta-less entries as "readings" while excluding
   them from the average, and blanks its card when the *newest* entry has no
   delta even though the chart still draws the older ones that do.
5. **"Latest" is computed two ways** (`reduce(isAfter)` vs last-of-sorted) —
   they differ only on equal timestamps.
6. **Nutrition data-confidence counts MEALS as readings for any nutrient**: the
   protein screen over a period with 30 meals and no protein data claims 30
   readings.
7. **Hydration "daily average" divides by tracked days, not days in the
   period** — and that average drives the goal bar, so a week where you hit the
   goal once and logged nothing else reads 100%.
8. **Body BMI history** recomputes every past weight against the period's
   *latest* height.
9. **Activity HRV / cardio-load sparklines** chart a never-sampled bucket as
   `0.0` while the totals correctly exclude it — an untracked day reads as a dip
   to zero rather than a gap.
10. **Caffeine distribution bars** floor their scale at 1 mg, so sub-1 mg slices
    render at full width.
11. **Apple Health import** rethrows `MissingHealthPermissionException`, but its
    formatter only recognises `AppleHealthImportPermissionException` — so a
    permissions failure reaches the card as a generic error and the grant
    affordance never appears.
12. **`refreshPlannedWorkouts`** keeps the selected plan id when the reload
    returns an empty list, so a deleted plan stays selected and the write
    request links a dead `plannedExerciseSessionId`.

## Bugs fixed by construction during the refactor

- Dashboard's minimum-permission check and onboarding's `checkState` both had
  `orThrow()` **outside** the try, in an unawaited microtask — a failing probe
  threw into nothing and left the screen spinning forever. Both now surface a
  `ScreenError`.
- `ActivityEntryController.addEntry` had the write-permission probe's
  `orThrow()` outside its try — a failing probe mid-save left `isSavingEntry`
  true and the form spinning forever.
- Hand-written `AchievementsState.copyWith` wrote `error: error` instead of
  `error ?? this.error`, so any unrelated `copyWith` silently cleared the error.
- `hydration_entry_screen_test`'s fake never implemented `loadHydrationEntry`;
  the try/catch this refactor removed had been swallowing the resulting
  `NoSuchMethodError`, so "edit an existing entry" was passing over a prefill
  that never ran.
