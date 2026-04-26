# Architecture

## Purpose

This document describes the architecture of OpenVitals as it exists today, plus the direction new work should follow.

The repo is still a single Android app module. The goal is not to force a multi-module design yet. The goal is to keep boundaries clear enough that new metrics can be added without copying screen scaffolding, period math, or Health Connect plumbing everywhere.

## Current Snapshot

- App namespace: `dev.manu.openvitals`
- Project shape: one Android app module under `app/`
- Dependency wiring: manual, in [`OpenVitalsApp`](../app/src/main/kotlin/dev/manu/openvitals/OpenVitalsApp.kt)
- UI stack: Jetpack Compose + Navigation Compose + `ViewModel` + coroutines/`StateFlow`
- Health data backend: Health Connect AndroidX client, wrapped by [`HealthConnectManager`](../app/src/main/kotlin/dev/manu/openvitals/healthconnect/HealthConnectManager.kt)
- Shared period shell: in place and used by all metric detail/list screens
- Feature repositories: in place for activity, sleep, heart, and body
- Dashboard: still a dedicated day-based summary screen, not a period-detail screen
- Declared but not active architecture pillars: Room and WorkManager are dependencies, but they are not part of the current feature flows yet

Most importantly, `body` and `browse` are no longer special legacy exceptions in the UI architecture. They now follow the same period-based shell as the other detail screens.

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

This pattern is implemented today by shared primitives in `ui/components`.

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

- a DI framework
- a reducer/effect architecture
- a multi-module split
- a cache/database architecture for all metrics

Those may become useful later, but they are not the baseline for new work today.

## Logical Layers In The Current App

These are logical layers inside one module, not Gradle modules.

### App shell

Responsibilities:

- app startup
- manual dependency wiring
- theme setup
- route registration
- top app bar and global navigation shell

Current files:

- [`OpenVitalsApp.kt`](../app/src/main/kotlin/dev/manu/openvitals/OpenVitalsApp.kt)
- [`MainActivity.kt`](../app/src/main/kotlin/dev/manu/openvitals/MainActivity.kt)
- [`navigation/AppNavigation.kt`](../app/src/main/kotlin/dev/manu/openvitals/navigation/AppNavigation.kt)
- [`navigation/Screen.kt`](../app/src/main/kotlin/dev/manu/openvitals/navigation/Screen.kt)

Notes:

- `OpenVitalsApp` manually exposes lazy repository instances.
- `MainActivity` owns the onboarding-complete preference and chooses the start destination.
- `AppNavigation` constructs screen-specific ViewModels with `remember(...)` and passes repositories explicitly.

### Data access

Responsibilities:

- Health Connect availability checks
- permission queries
- record reads and aggregate reads
- mapping Health Connect responses into app models
- feature-facing repository APIs

Current files:

- [`healthconnect/HealthConnectManager.kt`](../app/src/main/kotlin/dev/manu/openvitals/healthconnect/HealthConnectManager.kt)
- [`data/repository/HealthRepository.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/repository/HealthRepository.kt)
- [`data/repository/ActivityRepository.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/repository/ActivityRepository.kt)
- [`data/repository/SleepRepository.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/repository/SleepRepository.kt)
- [`data/repository/HeartRepository.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/repository/HeartRepository.kt)
- [`data/repository/BodyRepository.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/repository/BodyRepository.kt)
- [`data/model/HealthData.kt`](../app/src/main/kotlin/dev/manu/openvitals/data/model/HealthData.kt)

Current boundary shape:

- `HealthConnectManager` is the low-level integration wrapper. It talks to the AndroidX client, performs reads, and maps results into app models.
- `HealthRepository` is now intentionally narrow: Health Connect availability, permission state, and dashboard aggregation.
- Feature repositories are thin, permission-aware facades over `HealthConnectManager`.

This is a meaningful improvement over the earlier centralized repository approach. New feature reads should follow the feature-repository pattern, not expand `HealthRepository`.

### Shared UI / presentation

Responsibilities:

- reusable shell components
- period selection primitives
- date navigation UI
- loading/error primitives
- dashboard/detail card building blocks

Current files:

- [`ui/components/MetricDetailScaffold.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/MetricDetailScaffold.kt)
- [`ui/components/PeriodNavigator.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/PeriodNavigator.kt)
- [`ui/components/DateNavigation.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/DateNavigation.kt)
- [`ui/components/MetricCard.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/MetricCard.kt)
- [`ui/components/LoadingState.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/LoadingState.kt)
- [`ui/components/PullToRefreshBox.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/PullToRefreshBox.kt)
- [`ui/components/PermissionCallout.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/PermissionCallout.kt)

Important current detail:

- `TimeRange` still lives in `data/model/HealthData.kt`
- `DatePeriod`, `periodFor`, `periodTitle`, `periodSubtitle`, and `PeriodNavigator` live in `ui/components/PeriodNavigator.kt`

That is acceptable for now, but it is still a good candidate for a future `core/period` package once the period model stabilizes further.

### Feature layer

Responsibilities:

- feature contracts (`UiState`, actions, derived display fields)
- screen-specific orchestration
- feature-specific cards/charts/lists
- feature-specific display language

Current feature packages:

- [`features/onboarding`](../app/src/main/kotlin/dev/manu/openvitals/features/onboarding)
- [`features/dashboard`](../app/src/main/kotlin/dev/manu/openvitals/features/dashboard)
- [`features/activity`](../app/src/main/kotlin/dev/manu/openvitals/features/activity)
- [`features/sleep`](../app/src/main/kotlin/dev/manu/openvitals/features/sleep)
- [`features/heart`](../app/src/main/kotlin/dev/manu/openvitals/features/heart)
- [`features/body`](../app/src/main/kotlin/dev/manu/openvitals/features/body)
- [`features/browse`](../app/src/main/kotlin/dev/manu/openvitals/features/browse)
- [`features/settings`](../app/src/main/kotlin/dev/manu/openvitals/features/settings)

One practical note: `features/activity` currently contains two screens:

- `ActivityScreen` for steps/distance/calories style metric detail
- `ActivitiesScreen` for workout sessions

That is a reasonable local compromise today because both screens share `ActivityRepository`, but future metrics should still prefer one feature package per cohesive surface.

## Screen Families

### Dashboard

The dashboard is intentionally different from the period-based detail screens.

It is:

- a daily snapshot
- navigated by day only
- powered by one aggregated `DashboardData` object
- the main entry point into feature screens

Current files:

- [`features/dashboard/DashboardViewModel.kt`](../app/src/main/kotlin/dev/manu/openvitals/features/dashboard/DashboardViewModel.kt)
- [`features/dashboard/DashboardScreen.kt`](../app/src/main/kotlin/dev/manu/openvitals/features/dashboard/DashboardScreen.kt)

Shared pieces it uses:

- `PullToRefreshBox`
- `DayNavigator`
- `HealthDatePickerDialog`
- `MetricCard`
- `PermissionCallout`

The dashboard should stay summary-first. It should not become a second copy of detail-screen logic.

Current dashboard grouping is:

- Activity & recovery: steps, distance, floors, elevation, workouts, sleep
- Body & intake: calories, hydration, weight, body fat
- Heart: heart rate plus vitals
- Records: raw Health Connect browser

### Period-based detail/list screens

The aligned detail/list screens are:

- steps/activity
- activities
- sleep
- heart
- body
- browse

They all use [`MetricDetailScaffold`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/MetricDetailScaffold.kt) as the shared shell.

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

- [`features/onboarding`](../app/src/main/kotlin/dev/manu/openvitals/features/onboarding)
- [`features/settings`](../app/src/main/kotlin/dev/manu/openvitals/features/settings)

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

- `TimeRange` in `data/model/HealthData.kt`
- `DatePeriod` and `periodFor(...)` in `ui/components/PeriodNavigator.kt`

The feature should load data against the selected period rather than inventing custom navigation rules.

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

Each repository should:

- guard required permissions
- call `HealthConnectManager`
- return app models ready for the ViewModel

### Keep queries period-oriented

Prefer APIs shaped like:

- `loadX(date)`
- `loadX(start, end)`

and gradually converge toward a more explicit shared period/query model if duplication grows.

### Compose existing repositories when the feature is an aggregate browser

`BrowseViewModel` is a good current example. It composes activity, sleep, and body repositories rather than inventing a broad new repository abstraction.

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

### 1. Period state logic is still duplicated across ViewModels

`selectRange`, `previousPeriod`, `nextPeriod`, `selectDate`, and the load pattern are repeated in multiple ViewModels.

This is the clearest remaining candidate for shared extraction.

Good future targets:

- `core/period`
- a reusable period-state helper
- a small detail-screen contract/helper, but not a giant abstract base ViewModel

### 2. Period primitives are split across `data` and `ui`

`TimeRange` is in `data/model`, while `DatePeriod` and helpers are in `ui/components`.

That works, but it is not the cleanest long-term boundary.

### 3. Shared UI primitives are still grouped in broad files

For example, [`MetricCard.kt`](../app/src/main/kotlin/dev/manu/openvitals/ui/components/MetricCard.kt) currently contains:

- `MetricCard`
- `MetricCardPlaceholder`
- `SourceChip`
- `SectionHeader`
- `TimeRangeSelector`

This is fine for the current repo size, but if shared UI keeps growing, these should split by responsibility.

### 4. Room and WorkManager are not active architectural constraints yet

The project declares both dependencies, and the manifest already removes the default WorkManager initializer, but there is no current repository/cache/job architecture built around them.

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
