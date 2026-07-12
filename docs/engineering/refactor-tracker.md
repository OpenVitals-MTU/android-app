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
| 10 | Recording service/VM split | x |
| 11 | Closeout (docs, bridge audit) | x |

## Feature matrix

All 21 features now have: the `application/` + `presentation/` layout,
`*ViewModel` naming, `Result`-based repositories and use-cases, the display
precomputed in the view-model, a freezed state, and a view-model unit test.
**Complete** — the only open items are the two below.

## Status: complete

All eleven phases are done, and the two items that could not be closed by a
test suite are closed:

- **Recording** was verified by hand on a Pixel 6 Pro (2026-07-12): GPS start,
  backgrounding, screen lock, the notification's pause/resume through the
  communication port, process-death recovery mid-run, and stop/save. This was
  the reason `ActivityRecordingService` is a byte-for-byte copy of the old
  controller rather than a rewrite — there is no harness for any of it.
- **BLE** now has a `contract/ble_sensor_repository.dart`; no feature imports
  `data/source/sensors/ble/` any more.

Conformance, measured rather than asserted:

| | |
|---|---|
| widgets holding a repository | 0 |
| `*Notifier`-suffixed classes in features | 0 |
| feature files naming `HealthConnectNativeDataSource` | 0 |
| imperative controller islands (`ValueNotifier`/`ChangeNotifier` state) | 0 |
| features with `application/` + `presentation/` | all (`homewidgets` excepted by design — isolate glue, no view-model) |
| features with a pure display builder | 21 |
| repository contracts returning `Result` | 13 of 15 (2 are synchronous cache reads by design) |

The five feature files still importing `data/source/` are the background
isolates, naming the `HealthDataSource` *type* they receive from
`openBackgroundHealthAccess()`. That is the intended shape.

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

## The "bugs" the refactor surfaced — and what each turned out to be

Moving every derivation out of build methods surfaced twelve suspicious
behaviours. Each was then checked twice: **against the Kotlin source at
`23c14d0`** (the spec, per AGENTS.md), and **against whether it makes sense at
all**. Those are different questions, and the second one is the one that
mattered.

### Port defects — FIXED

1. **Sleep counted the wrong recording method.** Compared `recordingMethod`
   against `1` (ACTIVELY_RECORDED) and called it a manual entry; MANUAL_ENTRY
   is `3`. Kotlin used the *named* constant and was correct; the port copied the
   number into three files and got one wrong. Fixed by giving the app one
   definition (`domain/model/recording_method.dart`) and deleting all three.
2. **Apple Health import could never report a permission denial.**
   `AppleHealthImportPermissionException` was defined and never thrown, so the
   card's "grant" button could never appear. Dart-only, no Kotlin to check.

### Faithful to Kotlin, but wrong anyway — FIXED, with the reason written down

Behaviour parity is the default requirement, so each of these carries its
justification in its commit message.

3. **Hydration's goal bar rewarded you for logging less.** It divided the
   average of the days you *logged* by your goal, so logging one day and hitting
   the goal filled the bar. It now measures goal-met days over the days that
   have actually elapsed.
4. **Heart's day average could sit outside its own printed range** — average
   from the provider's aggregate, min/max from the samples. All three now come
   from the samples; the aggregate is the no-samples fallback it was there for.
5. **Skin temperature blanked its card while its own chart drew data** — the
   card read the newest entry of the unfiltered list, the chart read only
   delta-bearing entries. Both read the same population now.
6. **Respiratory rate printed two different averages on one screen**, unlabelled
   (mean-of-daily-means under the chart, mean-of-all-readings on the card). One
   average now, the one that matches the chart.
7. **Nutrition counted MEALS as readings for any nutrient.** Harmless in
   practice — the sparse-data warning fires on tracked days independently — but
   the number was a lie. It counts the nutrient's own tracked days now.

### Checked, and left alone — they are fine

- **Body's BMI history uses the period's latest height for every past weight.**
  Adult height is effectively constant, and one height stops a noisy
  re-measurement putting a fake step in the history. This is right.
- **Caffeine's distribution bars floor their scale at 1 mg.** A divide-by-zero
  guard. The "sub-1 mg slice renders full width" case only arises when a whole
  day's caffeine is under a milligram.
- **A deleted plan stays selected when the reload returns empty.** UNREACHABLE:
  the planned-workout feature is a stub in this port — `loadPlannedWorkoutOptions`
  and `loadExistingPlannedWorkouts` both return `const Ok([])` unconditionally
  and `writePlannedWorkout` throws unsupported. No plan can be selected, so no
  dangling id can be written. Revisit the guard *when the loader is implemented*:
  it cannot then tell "not loaded yet" from "you deleted the last one".
- **"Latest" computed two ways** (`reduce(isAfter)` vs last-of-sorted). They
  differ only on identical timestamps. Cosmetic.

### The lesson

Of twelve flagged behaviours, two were port defects, five were real design
faults inherited faithfully, and five were fine. Six independent agents each
flagged one during the refactor and every one of them called it a bug. **A
derivation that looks wrong in isolation is not evidence of a bug** — and the
reason they suddenly looked wrong is precisely that the refactor had just moved
them somewhere legible. Check the spec, then check whether the spec is right.
Neither check on its own is enough.

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
