# Architecture

## Purpose

This document describes the architecture of OpenVitals as it exists today, plus the direction new work should follow.

The repo is still a single Android app module. The goal is not to force a multi-module design yet. The goal is to keep boundaries clear enough that new metrics can be added without copying screen scaffolding, period math, or Health Connect plumbing everywhere.

## Current Snapshot

- App namespace: `tech.mmarca.openvitals`
- Project shape: one Android app module under `app/`
- Dependency wiring: Hilt in the single `:app` module, rooted at [`OpenVitalsApp`](../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt)
- UI stack: Jetpack Compose + Material 3 app shell + Navigation Compose + `ViewModel` + coroutines/`StateFlow`
- Health data backend: Health Connect AndroidX client, wrapped by [`HealthConnectManager`](../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectManager.kt)
- Shared period shell: in place and used by all metric detail/list screens
- Feature repositories: in place for activity, sleep, heart, body, hydration, nutrition, mindfulness, cycle, and vitals
- Dashboard: still a dedicated day-based summary screen, not a period-detail screen
- Manual entry: separate from the dashboard and writes explicit user-entered records directly to Health Connect
- Room and WorkManager are intentionally absent until a concrete cache or background refresh design exists

Most importantly, body and entry/session browsing now live in metric-owned detail screens. The former global Browse destination is no longer part of the app architecture.

## Architectural Principles

### 1. Feature-first code organization

New product work should live under `features/<feature>/`.

Each feature owns:

- screen composables
- screen `UiState`
- screen `ViewModel`
- feature-specific charts, cards, rows, and formatting

Shared code should only move out of a feature when it is clearly reused by multiple screens.

### 2. Shared shell, feature-owned visuals

The app now has a real shared shell for period-based screens:

- pull to refresh
- range selector
- period navigator
- date picker
- shared loading/error framing

That shell belongs in shared UI.

The actual metric presentation stays feature-local:

- steps charts
- sleep session timeline and stage bars
- heart trend/timeline cards
- workout rows
- weight/body composition cards

We do not want a universal chart abstraction that hides metric meaning.

### 3. Period-driven detail screens

The canonical interaction model for metric screens is:

- `Day / Week / Month / Year`
- selected anchor date
- previous/next navigation
- direct calendar selection
- forward navigation capped at the current period
- last selected range remembered independently per detail/list screen

This pattern is implemented today by period primitives in `core/period` and shell components in `ui/components`.

### 4. ViewModels own screen state and orchestration

Screens stay thin. ViewModels are responsible for:

- selected range/date state
- triggering loads and refreshes
- combining repository calls
- exposing UI-ready state

Screens should mostly collect state, wire callbacks, and render sections.

### 5. Repositories are feature-facing and permission-aware

Health Connect specifics stay below the feature layer.

Repository methods should answer feature questions such as:

- load workouts for a period
- load sleep sessions for a period
- load heart summaries for a period
- load body entries for a period

They should not keep growing into one large grab-bag repository with screen-specific overloads.

### 6. Keep abstractions proportional

The current app does not need:

- a reducer/effect architecture
- a multi-module split
- a cache/database architecture for all metrics

Those may become useful later, but they are not the baseline for new work today.

## Logical Layers In The Current App

These are logical layers inside one module, not Gradle modules.

### App shell

Responsibilities:

- app startup
- Hilt application/component setup
- theme setup
- route registration
- adaptive top app bar, navigation suite, and global action shell

Current files:

- [`OpenVitalsApp.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt)
- [`MainActivity.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/MainActivity.kt)
- [`di/AppModule.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/di/AppModule.kt)
- [`navigation/AppNavigation.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/navigation/AppNavigation.kt)
- [`navigation/Screen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/navigation/Screen.kt)
- [`ui/components/OpenVitalsAdaptiveScaffold.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/OpenVitalsAdaptiveScaffold.kt)

Notes:

- `OpenVitalsApp` owns the Hilt application component and locale bootstrap.
- `MainActivity` owns the onboarding-complete preference and chooses the start destination.
- `AppNavigation` owns route registration and top-level destination selection; route composables obtain `@HiltViewModel` instances through `hiltViewModel()`.
- `OpenVitalsAdaptiveScaffold` owns the Material 3 top app bar, `NavigationSuiteScaffold`, and contextual Add action.

### Data access

Responsibilities:

- Health Connect availability checks
- permission queries
- record reads and aggregate reads
- explicit manual-entry writes to Health Connect
- mapping Health Connect responses into app models
- feature-facing repository APIs

Current files:

- [`healthconnect/HealthConnectManager.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectManager.kt)
- [`data/repository/HealthRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HealthRepository.kt)
- [`data/repository/ActivityRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepository.kt)
- [`data/repository/SleepRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/SleepRepository.kt)
- [`data/repository/HeartRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HeartRepository.kt)
- [`data/repository/BodyRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/BodyRepository.kt)
- [`data/repository/HydrationRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HydrationRepository.kt)
- [`data/repository/NutritionRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/NutritionRepository.kt)
- [`data/repository/MindfulnessRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/MindfulnessRepository.kt)
- [`data/repository/CycleRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/CycleRepository.kt)
- [`data/repository/VitalsRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/VitalsRepository.kt)
- [`data/repository/PreferencesRepository.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/PreferencesRepository.kt)
- feature-oriented model files under [`data/model`](../app/src/main/kotlin/tech/mmarca/openvitals/data/model)

Current boundary shape:

- `HealthConnectManager` is the low-level integration wrapper. It talks to the AndroidX client, performs reads, writes explicit manual entries, and maps results into app models.
- `HealthRepository` is now intentionally narrow: Health Connect availability, permission state, and dashboard aggregation.
- Feature repositories are thin, permission-aware facades over `HealthConnectManager`.
- Manual entry ViewModels use the same feature repositories for writes, so write permission and write behavior stay below the UI route.

This is a meaningful improvement over the earlier centralized repository approach. New feature reads should follow the feature-repository pattern, not expand `HealthRepository`.

### Shared UI / presentation

Responsibilities:

- reusable shell components
- period selection primitives
- date navigation UI
- loading/error primitives
- dashboard/detail card building blocks

Current files:

- [`ui/components/MetricDetailScaffold.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffold.kt)
- [`ui/components/PeriodNavigator.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PeriodNavigator.kt)
- [`ui/components/DateNavigation.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/DateNavigation.kt)
- [`ui/components/MetricCard.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricCard.kt)
- [`ui/components/LoadingState.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/LoadingState.kt)
- [`ui/components/PullToRefreshBox.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PullToRefreshBox.kt)
- [`ui/components/PermissionCallout.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PermissionCallout.kt)

Important current detail:

- `TimeRange`, `DatePeriod`, `PeriodLoadQuery`, `PeriodWindows`, `PeriodSelectionDriver`, and period formatting helpers live in `core/period`
- `PeriodRangePreferenceKey` lives in `core/period`; `PreferencesRepository` persists the last selected `TimeRange` per detail/list screen
- `PeriodNavigator` remains a UI component in `ui/components`

### Feature layer

Responsibilities:

- feature contracts (`UiState`, actions, derived display fields)
- screen-specific orchestration
- feature-specific cards/charts/lists
- feature-specific display language

Current feature packages:

- [`features/onboarding`](../app/src/main/kotlin/tech/mmarca/openvitals/features/onboarding)
- [`features/dashboard`](../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard)
- [`features/activity`](../app/src/main/kotlin/tech/mmarca/openvitals/features/activity)
- [`features/sleep`](../app/src/main/kotlin/tech/mmarca/openvitals/features/sleep)
- [`features/heart`](../app/src/main/kotlin/tech/mmarca/openvitals/features/heart)
- [`features/body`](../app/src/main/kotlin/tech/mmarca/openvitals/features/body)
- [`features/manualentry`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry)
- [`features/settings`](../app/src/main/kotlin/tech/mmarca/openvitals/features/settings)

One practical note: `features/activity` currently contains two screen families:

- concrete metric entry screens such as `StepsScreen`, `DistanceScreen`, `CaloriesOutScreen`, `ActiveCaloriesScreen`, `FloorsScreen`, and `ElevationScreen`
- `ActivitiesScreen` for workout sessions

That is a reasonable local compromise today because these screens share `ActivityRepository`, but route-facing composables should stay metric-specific. Shared renderers inside a feature package are acceptable when they only remove local duplication and do not make the user-facing detail screen show several metrics at once.

## Screen Families

### Dashboard

The dashboard is intentionally different from the period-based detail screens.

It is:

- a daily snapshot
- navigated by day only
- powered by one aggregated `DashboardData` object
- the main entry point into feature screens

Current files:

- [`features/dashboard/DashboardViewModel.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModel.kt)
- [`features/dashboard/DashboardScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardScreen.kt)

Shared pieces it uses:

- `PullToRefreshBox`
- `DayNavigator`
- `HealthDatePickerDialog`
- `MetricCard`
- `PermissionCallout`

The dashboard should stay summary-first. It should not become a second copy of detail-screen logic.

Dashboard metric cards route to metric-specific detail destinations. Metrics that share a repository can still reuse the same feature package and ViewModel, but navigation should call concrete metric screen entry points such as `ProteinScreen` or `RestingHeartRateScreen`, not a public screen with a metric parameter. The rendered detail view should focus on the selected metric instead of showing every related metric in one grouped screen. There is no global records browser or fixed dashboard browse action; entry and session lists belong behind the relevant metric card/detail screen.

### Manual entry

Manual entry is a separate screen family from the dashboard. It is the only app area that should initiate Health Connect write flows. The Add entry picker is reached through contextual create actions on the dashboard and supported metric screens, not as a primary browsing destination.

Current files:

- [`features/manualentry/ManualEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryScreen.kt)
- [`features/manualentry/ManualEntryViewModel.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryViewModel.kt)
- [`features/manualentry/ActivityEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ActivityEntryScreen.kt)
- [`features/manualentry/ActivityEntryViewModel.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ActivityEntryViewModel.kt)
- [`features/manualentry/HydrationEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/HydrationEntryScreen.kt)
- [`features/manualentry/BodyMeasurementEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/BodyMeasurementEntryScreen.kt)
- [`features/manualentry/VitalsMeasurementEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/VitalsMeasurementEntryScreen.kt)
- [`features/manualentry/MindfulnessEntryScreen.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/MindfulnessEntryScreen.kt)

The current manual entry widgets cover hydration, activity sessions with optional GPX/KML/KMZ route import or GPS recording, mindfulness, weight, height, body fat, blood pressure, SpO2, respiratory rate, and body temperature. Widget order is customizable in the same spirit as the dashboard, but the dashboard remains read-only.

Write permissions are requested lazily from Add entry or the specific metric entry route. Onboarding and the dashboard should only request read permissions. Each write goes directly to Health Connect; OpenVitals keeps only local UI preferences such as widget order and mindfulness timer/background-sound settings.

### Period-based detail/list screens

The aligned detail/list screens are:

- steps/activity
- activities
- sleep
- heart
- body
- hydration
- nutrition
- mindfulness
- cycle

They all use [`MetricDetailScaffold`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffold.kt) as the shared shell.

The scaffold currently owns:

- pull to refresh
- time range selector
- period navigator
- date picker
- shared error block
- `headerItems` slot
- `content: LazyListScope.(DatePeriod) -> Unit` slot

This is the main reusable architectural frame for metric work in the app today.

### Permission surfaces

Onboarding and Settings are not metric screens, but they are important architectural surfaces because they centralize Health Connect availability and permission management.

Current files:

- [`features/onboarding`](../app/src/main/kotlin/tech/mmarca/openvitals/features/onboarding)
- [`features/settings`](../app/src/main/kotlin/tech/mmarca/openvitals/features/settings)

These screens should continue to depend on `HealthRepository`, not on feature repositories.

## Canonical Detail Feature Pattern

New metric detail work should follow this shape.

### 1. Define a feature-owned contract

At minimum:

- `UiState`
- selected range
- selected date
- loading state
- feature payload
- error state

Keep derived fields in the state only when they genuinely simplify the UI.

### 2. Reuse the shared period model

Today the shared period model is:

- `TimeRange`, `DatePeriod`, `PeriodLoadQuery`, `PeriodWindows`, and `PeriodSelectionDriver` in `core/period`

The feature should load data against the selected period query rather than inventing custom navigation rules.

### 3. Keep the ViewModel in charge

The ViewModel should:

- update range/date
- clamp future navigation
- compute the active period
- call repositories
- expose UI-ready data

Most current ViewModels already follow this shape.

### 4. Use `MetricDetailScaffold` as the shell

The screen should pass shared shell parameters and provide only feature content.

The content lambda should render:

- `Day` mode content
- `Week / Month / Year` content
- optional list/breakdown sections

When registering a new period-based screen, add a `PeriodRangePreferenceKey` and inject `PreferencesRepository` into the screen ViewModel so the saved range is owned with the rest of the feature state. Persist only range changes; selected dates remain screen state.

### 5. Keep visuals local to the feature

If the feature needs a custom chart, row, or timeline, keep it in the feature package unless another feature genuinely needs the same thing.

## Repository Rules For New Work

### Use `HealthRepository` only for app-level concerns

Keep using `HealthRepository` for:

- availability
- permission contract access
- granted/missing permissions
- dashboard loading

Do not add new feature-detail data methods there unless the app is in a temporary migration step.

### Add or extend feature repositories for feature data

Follow the current pattern:

- `ActivityRepository`
- `SleepRepository`
- `HeartRepository`
- `BodyRepository`
- `HydrationRepository`
- `NutritionRepository`
- `MindfulnessRepository`
- `CycleRepository`
- `VitalsRepository`

Each repository should:

- guard required permissions
- call `HealthConnectManager`
- return app models ready for the ViewModel

### Keep queries period-oriented

Prefer APIs shaped like:

- `loadXPeriod(PeriodLoadQuery, featureOptions)`
- feature-specific query/result objects when period windows need current, previous, and baseline data

Keep granular APIs only when they are real detail or entry-list reads rather than compatibility paths for migrated screens. Avoid adding an aggregate browser layer unless product direction explicitly reintroduces one.

## What Should Stay Shared vs Local

### Shared

- period calculation and titles
- period/day navigation components
- date picker dialog
- detail-screen scaffold
- pull-to-refresh wrapper
- loading/error components
- general card primitives like `MetricCard`
- general chips and section headers

### Feature-local

- metric-specific charts
- metric-specific timelines
- metric-specific list rows
- metric-specific summaries
- metric-specific empty-state language when the domain meaning differs

## Known Seams And Next Refactors

These are real seams in the current codebase, but they are not urgent enough to block feature work.

### 1. Some screen files are still too broad

Several feature screens still keep route/content/cards/charts in one file.

Good future targets:

- split route/container composables from chart/card/list sections
- keep feature-specific visuals inside the feature package
- move only reusable shell pieces to `ui/components`

### 2. Derived UI summaries should stay ViewModel-prepared

Hydration, nutrition, heart/vitals, and body now prepare common summary values in state. Continue this pattern when a value requires sorting, grouping, or scanning a list.

### 3. Shared UI primitives are still grouped in broad files

For example, [`MetricCard.kt`](../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricCard.kt) currently contains:

- `MetricCard`
- `MetricCardPlaceholder`
- `SourceChip`
- `SectionHeader`
- `TimeRangeSelector`

This is fine for the current repo size, but if shared UI keeps growing, these should split by responsibility.

### 4. Room and WorkManager are not active architectural constraints yet

The project does not declare Room or WorkManager dependencies because there is no current repository/cache/job architecture built around them.

Do not design new features as if a cache/database/background-sync layer already exists.

### 5. Do not over-correct into a universal framework

Still avoid:

- a universal chart abstraction
- a giant base ViewModel hierarchy
- premature multi-module refactors
- a full reducer/effect framework for straightforward screens

## Success Criteria

The architecture is working well when:

- a new metric screen can be added without copying shell UI
- Health Connect reads stay below the feature layer
- feature repositories stay narrow and query-oriented
- screens remain thin
- charts remain understandable because metric-specific visuals stay local
- shared extraction happens for scaffolding, not for semantics
