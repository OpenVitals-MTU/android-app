# Project Structure and File Organization

## Top-level package map

OpenVitals uses **feature-first organization** inside a single `:app` module (`tech.mmarca.openvitals`).

```
app/src/main/kotlin/tech/mmarca/openvitals/
├── OpenVitalsApp.kt              # Hilt application, locale bootstrap
├── MainActivity.kt               # Theme, onboarding gate, nav host
├── navigation/                   # Routes, Screen sealed types, AppNavigation
├── di/                           # Hilt modules (AppModule)
├── features/                     # Product features (primary home for new work)
├── ui/
│   ├── components/               # Shared Compose shell (scaffold, charts, cards)
│   └── theme/                    # Material theme
├── core/
│   ├── period/                   # TimeRange, DatePeriod, PeriodSelectionDriver
│   ├── performance/              # LoadCoordinator, DispatcherProvider
│   ├── presentation/             # UnitFormatter, DateTimeFormatterProvider
│   └── diagnostics/              # Debug tooling
├── domain/
│   ├── model/                    # Pure health/app models
│   ├── insights/                 # Sleep score, goals, baselines, readiness
│   └── preferences/              # Enums and preference keys
├── data/
│   ├── repository/               # Feature + Health repositories
│   └── cache/                    # Room entities, codecs, cache store
├── healthconnect/                # HC client wrapper, readers, permissions, UX
└── sensors/                      # BLE and device integrations
```

## Logical layers (not separate modules)

| Layer | Packages | Depends on |
|-------|----------|------------|
| Presentation | `features/*`, `ui/*`, `navigation` | domain, data (via ViewModels), core |
| Domain | `domain/*`, `core/period`, `core/presentation` | Kotlin stdlib, minimal Android |
| Data | `data/*`, `healthconnect` | domain, Android, Health Connect SDK |

This is **pragmatic layered MVVM**, not a strict multi-module Clean Architecture split. See [clean-architecture-refactor.md](clean-architecture-refactor.md).

## Feature packages

Current feature directories:

| Package | Role |
|---------|------|
| `features/dashboard` | Daily summary, widget grid |
| `features/activity` | Steps, distance, calories, workouts, recording, maps |
| `features/sleep` | Sleep detail and session detail |
| `features/heart` | Heart rate, HRV, vitals overview |
| `features/body` | Weight and body composition |
| `features/hydration` | Hydration detail + reminders |
| `features/nutrition` | Nutrition macros |
| `features/mindfulness` | Mindfulness sessions + reminders |
| `features/cycle` | Cycle tracking |
| `features/manualentry` | Health Connect writes |
| `features/onboarding` | First-run permissions |
| `features/settings` | App and HC settings |
| `features/recovery` | Recovery / sleep-derived views |
| `features/achievements` | Achievements |
| `features/imports` | Apple Health import |

### What a feature should own

- Screen composables (route + sections)
- `UiState` and `ViewModel`
- Feature-specific charts, cards, rows, timelines
- Feature-specific formatting **only when** not reusable

### What should stay shared

| Shared location | Contents |
|-----------------|----------|
| `ui/components` | `MetricDetailScaffold`, `PeriodNavigator`, `MetricCard`, loading/error |
| `core/period` | Period math, titles, `PeriodSelectionDriver` |
| `core/presentation` | Formatters without repository access |
| `domain/insights` | Cross-metric calculations |
| `healthconnect` | Permission sets, `HealthConnectManager`, screen UX coordinator |

Do **not** put feature-specific business logic in `ui/components`.

## Recommended feature file layout

For a period-based detail metric (e.g. sleep):

```
features/sleep/
├── SleepScreen.kt              # Route: collect state, wire scaffold, < ~150 lines
├── SleepViewModel.kt           # UiState, actions, load orchestration
├── SleepDayContent.kt          # LazyListScope extensions for Day mode
├── SleepPeriodContent.kt       # Week / Month / Year sections
├── SleepCharts.kt              # Chart composables
├── SleepSessionRows.kt         # List rows and session cards
├── SleepDetailScreen.kt        # Separate flow for session detail
├── SleepDetailViewModel.kt
└── SleepFormatting.kt          # Optional; only sleep-specific display helpers
```

### Existing good splits

- `BodyCards.kt` — body metric cards separated from screen
- `ActivitiesOverviewSections.kt` — workout overview sections
- `HeartMetricSharedSections.kt`, `HeartVitalsRows.kt` — heart UI decomposition

### Screens that need splitting

Prioritize splitting route from content for:

- `SleepScreen.kt`
- `HeartScreen.kt`
- `HydrationScreen.kt`
- `BodyScreen.kt`
- `ActivityScreen.kt`
- `ActivityRecordingScreen.kt`
- `ManualEntryScreen.kt`

## Screen families

### Dashboard (day-based, not period-detail)

- `DashboardViewModel` + `DashboardScreen`
- Single-day navigation, aggregated `DashboardData`
- Routes into metric-specific detail destinations

### Period detail / list (canonical pattern)

Uses `MetricDetailScaffold` + `WithHealthConnectFeatureScreen`:

- Activity metrics, activities list, sleep, heart, body, hydration, nutrition, mindfulness, cycle

### Manual entry (write path)

- Separate from dashboard reads
- Subpackages: `activity/`, `hydration/`, `body/`, `vitals/`, `mindfulness/`
- ViewModels write through feature repositories

### Permission surfaces

- `features/onboarding`, `features/settings`
- Depend on `HealthRepository`, not feature repositories for permission contract

## Test source layout

Mirror production packages under `app/src/test/kotlin/tech/mmarca/openvitals/`:

```
features/sleep/SleepViewModelTest.kt
data/repository/SleepRepositoryTest.kt   # when added
domain/insights/SleepScoreDateTest.kt
core/period/PeriodSelectionTest.kt
util/MainDispatcherRule.kt
```

## Navigation registration

New screens require updates to:

- `navigation/Screen.kt` — route definition
- `navigation/AppNavigation.kt` — composable destination
- Dashboard widget or parent screen — entry point
- `HealthConnectFeature` — if HC permissions apply
- `PeriodRangePreferenceKey` — if range should be remembered

## Anti-patterns (do not copy)

Documented in `AGENTS.md`:

- Local coroutine loading in composables for new features
- Per-screen permission callouts outside `HealthConnectScreenShell`
- New navigator implementations per feature
- Giant abstract base ViewModels
- Universal chart abstraction hiding metric meaning
- Expanding `HealthRepository` with feature-detail reads

## Adding a new metric (quick checklist)

1. Create `features/<metric>/` with ViewModel + Screen
2. Add or extend feature repository with `loadXPeriod`
3. Register route and dashboard card
4. Use `MetricDetailScaffold` and shared HC shell
5. Add `PeriodRangePreferenceKey` + ViewModel tests
6. Update [feature-playbook.md](../feature-playbook.md) only if the pattern changes
