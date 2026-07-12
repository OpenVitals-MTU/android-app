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

## The "bugs" the refactor surfaced — and what they turned out to be

Moving every derivation out of build methods put duplicated logic side by side
and surfaced twelve suspicious behaviours. **Then each was checked against the
Kotlin source at `23c14d0`, which AGENTS.md names as the spec.** That check
changed the answer for most of them, and it is the reason none were "fixed" in
the refactor commits themselves.

### Genuine port defects — FIXED, each in its own commit

1. **Sleep counted the wrong recording method.** `sleep_display.dart` compared
   `recordingMethod` against `1` and called it a manual entry; Health Connect's
   `RECORDING_METHOD_MANUAL_ENTRY` is `3` (`1` is `ACTIVELY_RECORDED`). The
   Kotlin original was correct because it compared against the *named* constant
   `Metadata.RECORDING_METHOD_MANUAL_ENTRY`; the port hand-copied the number
   into three files and got one of them wrong. Fixed by giving the app one
   definition (`domain/model/recording_method.dart`) and deleting all three
   copies. This is the signature of a real port bug: Kotlin used a name, the
   port used a literal.
2. **Apple Health import could never report a permission denial.**
   `AppleHealthImportPermissionException` was defined and never thrown, so
   `isPermissionDenied()` always answered false and the card's "grant" button
   never appeared. A Dart-only subsystem, so there was no Kotlin to check.
   Fixed once the `Result` migration made `PermissionFailure` a *type* the
   probe could classify.

### Faithful ports of Kotlin behaviour — NOT bugs, and NOT changed

Each of these was verified to match `23c14d0` exactly. Behaviour parity is the
default requirement (AGENTS.md), so changing any of them is a **product
decision**, not a fix — and it needs a reason written down. They are listed
because they are worth *deciding* about, not because they are broken.

| Behaviour | Kotlin says |
|---|---|
| Hydration's "daily average" divides by *tracked* days, not days in the period — and that average drives the goal bar, so a week where you hit the goal once and logged nothing else reads 100% | `averageLiters = trackedDays.takeIf { it > 0 }?.let { totalLiters / it }` — identical |
| Heart's day resting-HR/HRV can print an average outside its own printed min/max (average from the provider's aggregate, range from raw samples) | `restingBpm = state.dayRestingBpm ?: samples.average()`, low/high from samples — identical |
| Nutrition's data-confidence counts *meals* as readings for any nutrient (30 meals, no protein data → "30 readings") | `sampleCount = entries.takeIf { it.isNotEmpty() }?.size ?: trackedValues.size` — identical |
| Body's BMI history recomputes every past weight against the period's *latest* height | `bmi = latestWeightKg.bmiWith(heightCm)`, `previousBmi = previousLatestWeightKg.bmiWith(heightCm)` — same height for both |
| A deleted plan stays selected when `refreshPlannedWorkouts` reloads an empty list | `plans.isEmpty() || plans.any { it.id == selected }` — identical |
| Caffeine's distribution bars floor their scale at 1 mg, so sub-1 mg slices render full width | `.coerceAtLeast(1.0)` — identical |
| Respiratory rate / skin temperature / "latest" computed two ways in two places | the same two derivations exist in the Kotlin screens |

The lesson worth keeping: **a derivation that looks wrong in isolation is not
evidence of a bug.** Six independent agents each flagged one, and six out of
seven were the app behaving exactly as designed. Check the spec before you
"fix" the port.

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
