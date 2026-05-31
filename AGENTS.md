# AGENTS.md

This file is the implementation guide for future agents working in this repository.

Read this before adding a new feature or extending an existing metric screen.

## Purpose

The app is moving toward a consistent, period-based detail architecture for health metrics.

The goal is:

- dashboard-first navigation
- feature-first code organization
- clear separation between data access, feature state, and UI
- reusable screen scaffolding without forcing all metrics into one generic chart system

## Source Of Truth

Use these docs together:

- [plan.md](plan.md): product direction and scope
- [docs/README.md](docs/README.md): doc index
- [docs/architecture.md](docs/architecture.md): target architecture
- [docs/feature-playbook.md](docs/feature-playbook.md): step-by-step guide for adding a feature

If code and docs disagree, prefer the docs for new work and refactor toward them incrementally.

## Current State

The codebase already has aligned period-based detail screens for:

- steps/activity
- sleep
- heart
- activities
- body

The global Browse feature has been removed. Entries and sessions should be browsable from the relevant dashboard widget/detail screen instead of through a standalone app destination.

These features already show the intended direction.

The following areas are still transitional and should not be copied as the default pattern:

- duplicated period selection logic in multiple ViewModels
- broad shared component files that still mix several concerns
- a future `core/period` extraction that has not happened yet

## Golden Path For New Metric Features

When adding a new detail feature, follow this shape:

1. Define the feature contract.
   - screen state
   - user actions
   - any derived display fields

2. Make the feature period-driven.
   - support `Day / Week / Month / Year`
   - use a selected anchor date
   - support previous/next navigation
   - cap navigation at the current period

3. Keep the frame reusable, keep the charts specific.
   - reuse the shared period scaffolding
   - keep metric-specific cards and charts inside the feature package

4. Keep repository APIs query-oriented.
   - prefer `DatePeriod` or feature query objects over adding more ad hoc overloads
   - keep Health Connect specifics below the feature layer

5. Register the feature from the dashboard.
   - dashboard card
   - route
   - top bar title

6. Update docs if the pattern evolves.

## Implementation Rules

### Feature packages

Prefer adding code under `features/<metric>/...`.

A feature should own:

- screen composables
- screen state
- screen ViewModel
- feature-specific chart and row components
- feature-specific formatting only when it is truly metric-specific

### Shared code

Shared code belongs in:

- `ui/components` for reusable shell components
- future `core/period` for period math and formatting
- future `core/presentation` for shared formatters or UI models

Do not put feature-specific business logic into `ui/components`.

### ViewModels

Prefer one ViewModel per screen.

ViewModels should:

- own loading state
- own selected range/date state
- call repositories or query services
- prepare UI-ready state

ViewModels should not:

- contain large formatting blocks
- duplicate generic period math forever
- directly mirror raw Health Connect response structures if a cleaner UI model is needed

### Repositories

`HealthRepository` is intentionally narrow and should stay that way.

When adding new capability:

- prefer a feature-oriented API
- prefer query objects or `DatePeriod`
- avoid adding both `loadX(range)` and `loadX(start, end)` unless it is temporary during migration

### UI composition

New detail screens should follow this mental model:

- scaffold: refresh + range selector + period navigator + error + date picker
- content:
  - `Day` mode
  - `Week / Month / Year` mode
  - optional list/breakdown

### Do not copy these patterns

- local coroutine loading directly in screens for new feature work
- brand new navigator implementations per feature
- new screen-specific period helper types if a shared one can be used
- giant abstract base ViewModels
- a universal chart abstraction that hides metric semantics

## Before Starting A New Feature

Read [docs/feature-playbook.md](docs/feature-playbook.md) and follow the checklist there.

If the feature would require copying code from `ActivityScreen`, `SleepScreen`, or `HeartScreen`, stop and ask:

"Should this be a shared scaffold/component first?"

In most cases, the answer should be yes for the shell, and no for the actual chart body.
