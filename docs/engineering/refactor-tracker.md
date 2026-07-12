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
| 0 | Foundations (Result, AppFailure, runCatching, CommandState, toScreenError) | x |
| 1 | Rename Notifier→ViewModel + application/presentation layout everywhere | x |
| 2 | Result through repositories + use-cases | x |
| 3 | VMs on Result + CommandState; seam reversal templates (mindfulness, sleep, manual-entry) | x |
| 4 | Seam reversal heavy features + freezed state conversions | ~ |
| 5 | Vitals god-file split | — |
| 6 | Layer hygiene (permissions→domain, BLE contract, widget→repo cleanup, background DI helper) | — |
| 7 | DI split with barrel export | x |
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
| mindfulness | x | x | x | x | x | x |
| cycle | x | x | — | — | x | — |
| nutrition | x | x | — | — | x | — |
| caffeine | x | x | — | — | x | — |
| hydration | x | x | — | — | x | — |
| sleep (incl. detail) | x | x | x | x | x | x |
| heart | x | x | x | x | x | x |
| body | x | x | — | — | x | — |
| bodyenergy | x | x | — | — | x | — |
| vitals (Ph 5) | x | x | — | — | — | — |
| activity (metrics + sections) | x | x | — | — | x | — |
| manualentry (forms) | x | x | x | n/a | x | x |
| manualentry/activity entry (Ph 9) | x | x | — | — | — | — |
| recording (Ph 10) | x | x | — | — | — | — |
| dashboard | x | x | — | — | x | — |
| recovery (incl. details) | x | x | detail: x | detail: x | x | x |
| readiness (incl. training details) | x | x | — | — | x | — |
| achievements | x | x | — | — | — | — |
| onboarding | x | x | — | — | x | — |
| settings (+ 11 cards) | x | x | — | — | ble: — | partial |
| imports (route + applehealth) | x | x | — | — | — | — |
| homewidgets (Ph 6 helper) | n/a | x | n/a | n/a | n/a | — |

## Cross-cutting items

- [ ] `orThrow()` call sites: 100 in lib/ after Ph 2; must be zero by Ph 11
- [ ] `throwableToScreenError` call sites (~28 files): deleted by Ph 11
- [ ] Hand-written states → freezed: Achievements, RecoveryDetail,
      RouteBulkImport, HydrationReminderSettings, MindfulnessReminderSettings,
      AppleHealthImport, BleDevices, SleepDetail, HeartVitalsOverview
- [ ] `health_permissions.dart` → `lib/domain/health/` (Ph 6)
- [ ] BLE repository contract (Ph 6)
- [ ] `lib/bootstrap/background_container.dart` + 5 isolate entrypoints (Ph 6)
- [ ] DI barrel split (Ph 7)
- [ ] architecture.md Known Seams §1 rewrite (first commit of Ph 3)
