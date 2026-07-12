# Architecture refactor tracker

Working checklist for the Flutter app-architecture conformance refactor
(plan: MVVM roles on Riverpod, `*ViewModel` naming, `Result<T>` end-to-end,
display derivation in view-models, `CommandState` for write flows).
Archived at closeout — this is a scratchpad, not documentation; the durable
rules land in `architecture.md` / `AGENTS.md` as each phase invalidates them.

Legend: `—` not started · `~` in progress · `x` done · `n/a` not applicable

## Phases

| # | Phase | Status |
|---|-------|--------|
| 0 | Foundations (Result, AppFailure, runCatching, CommandState, toScreenError) | ~ |
| 1 | Rename Notifier→ViewModel + application/presentation layout everywhere | — |
| 2 | Result through repositories + use-cases | — |
| 3 | VMs on Result + CommandState; seam reversal templates (mindfulness, sleep, manual-entry) | — |
| 4 | Seam reversal heavy features + freezed state conversions | — |
| 5 | Vitals god-file split | — |
| 6 | Layer hygiene (permissions→domain, BLE contract, widget→repo cleanup, background DI helper) | — |
| 7 | DI split with barrel export | — |
| 8 | Offline map import VM | — |
| 9 | ActivityEntry VM | — |
| 10 | Recording service/VM split | — |
| 11 | Closeout (delete orThrow + throwableToScreenError, docs) | — |

## Feature matrix

Columns: **Layout** = application/+presentation/ dirs + ViewModel naming (Ph 1)
· **Result** = repo slice + use-cases return Result (Ph 2) · **VM** = view-model
switches on Result, CommandState for writes (Ph 3–4) · **Display** = derivation
out of build paths into `*_display.dart` (Ph 3–5) · **Freezed** = state class
freezed (Ph 4–5) · **VM test** = dedicated view-model unit test.

| Feature | Layout | Result | VM | Display | Freezed | VM test |
|---|---|---|---|---|---|---|
| mindfulness | — | — | — | — | x | — |
| cycle | — | — | — | — | x | — |
| nutrition | — | — | — | — | x | — |
| caffeine | — | — | — | — | x | — |
| hydration | — | — | — | — | x | — |
| sleep (incl. detail) | — | — | — | — | detail: — | — |
| heart | — | — | — | — | x | — |
| body | — | — | — | — | x | — |
| bodyenergy | — | — | — | — | x | — |
| vitals (Ph 5) | — | — | — | — | — | — |
| activity (metrics + sections) | x | — | — | — | x | — |
| manualentry (forms) | x | — | — | — | x | partial |
| manualentry/activity entry (Ph 9) | x | — | — | — | — | — |
| recording (Ph 10) | x | — | — | — | — | — |
| dashboard | — | — | — | — | x | — |
| recovery (incl. details) | — | — | — | — | detail: — | — |
| readiness (incl. training details) | — | — | — | — | x | — |
| achievements | — | — | — | — | — | — |
| onboarding | — | — | — | — | x | — |
| settings (+ 11 cards) | — | — | — | — | ble: — | partial |
| imports (route + applehealth) | — | — | — | — | — | — |
| homewidgets (Ph 6 helper) | — | — | n/a | n/a | n/a | — |

## Cross-cutting items

- [ ] `orThrow()` call sites: introduced Ph 2, must be zero by Ph 11
- [ ] `throwableToScreenError` call sites (~28 files): deleted by Ph 11
- [ ] Hand-written states → freezed: Achievements, RecoveryDetail,
      RouteBulkImport, HydrationReminderSettings, MindfulnessReminderSettings,
      AppleHealthImport, BleDevices, SleepDetail, HeartVitalsOverview
- [ ] `health_permissions.dart` → `lib/domain/health/` (Ph 6)
- [ ] BLE repository contract (Ph 6)
- [ ] `lib/bootstrap/background_container.dart` + 5 isolate entrypoints (Ph 6)
- [ ] DI barrel split (Ph 7)
- [ ] architecture.md Known Seams §1 rewrite (first commit of Ph 3)
