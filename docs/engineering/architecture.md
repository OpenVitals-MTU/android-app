# Architecture

## Purpose

This document describes the architecture of the OpenVitals Flutter app as it exists today, plus the direction new work should follow.

This app is a 1:1 port of the Kotlin OpenVitals app, which it replaced in place on this repository (the Kotlin sources survive only in git history, at `23c14d0`). The port keeps the Kotlin app's *architectural principles* — feature-first packages, a shared period shell, permission-aware feature repositories, proportional abstractions — but it does not keep its *mechanics*. Compose is Flutter widgets, ViewModels are Riverpod `Notifier` subclasses, Hilt is Riverpod providers, Room is drift, Navigation Compose is go_router. Where the port deliberately diverged from the Kotlin design, this document says so instead of pretending it didn't.

The goal is unchanged: keep boundaries clear enough that a new metric can be added without copying screen scaffolding, period math, or Health Connect plumbing.

For the day-to-day rules and the invariants that have already been broken once, read [AGENTS.md](../../AGENTS.md). This document is the *why* and the *shape*; AGENTS.md is the *don't*.

## Current Snapshot

- App id: `tech.mmarca.openvitals` (unchanged from the Kotlin app — same Codeberg repo, same Play listing)
- Project shape: one Flutter app (`lib/`) plus one first-party plugin, [`packages/health_connect_native`](../../packages/health_connect_native), which owns the Pigeon bridge to Health Connect. There is no other module.
- Dependency wiring: Riverpod. [`lib/di/providers.dart`](../../lib/di/providers.dart) is the object graph (data source, repositories, use cases, reminders, widgets, maps); [`lib/state/app_providers.dart`](../../lib/state/app_providers.dart) holds the app-shell preference providers.
- UI stack: Flutter + Material 3 + `MaterialApp.router` ([`lib/app.dart`](../../lib/app.dart)) + go_router ([`lib/navigation/app_router.dart`](../../lib/navigation/app_router.dart)) + Riverpod `Notifier` view-models + `freezed` state classes
- Health data backend: Health Connect, behind [`lib/data/source/health/health_data_source.dart`](../../lib/data/source/health/health_data_source.dart) — the `HealthConnectManager` analogue
- App-local domain code: pure models, insight calculations, queries, use cases and preference enums under [`lib/domain/`](../../lib/domain)
- Shared period shell: in place, in [`lib/core/period/`](../../lib/core/period) and [`lib/ui/components/metric_detail_scaffold.dart`](../../lib/ui/components/metric_detail_scaffold.dart), and used by every metric detail/list screen
- Feature repositories: split into `contract/` and `impl/` under [`lib/data/repository/`](../../lib/data/repository) for activity, sleep, heart, body, body energy, caffeine, hydration, nutrition, mindfulness, cycle, vitals, BLE devices, and Apple Health import
- Dashboard: a dedicated day-based summary screen, not a period-detail screen
- Manual entry: separate from the dashboard; writes explicit user-entered records straight to Health Connect
- Persistence: drift ([`lib/data/local/open_vitals_database.dart`](../../lib/data/local/open_vitals_database.dart)) holds **exactly one table — `beverages`**. Everything else that persists lives in `SharedPreferences` via [`lib/data/prefs/preferences_repository.dart`](../../lib/data/prefs/preferences_repository.dart). Health Connect is the source of truth for health data.
- Background work: `android_alarm_manager_plus` (home-widget refresh, hydration/mindfulness reminders) and `flutter_foreground_task` (Apple Health import, activity recording). There is no WorkManager and no general background-sync layer.

> **Correction to the Kotlin doc.** The Kotlin architecture doc claims "Room is present for derived metric summary caching only" and that "WorkManager is used for … lightweight metric summary warmup". Neither is true, in either repo. The Kotlin `OpenVitalsDatabase` also declares only `BeverageEntity`, and its workers are the Apple Health import and the offline-map import. There is no metric summary cache and never was. Do not build against one.

Body and entry/session browsing live in metric-owned detail screens. There is no global Browse destination.

## Architectural Principles

### 1. Feature-first code organization

New product work lives under `lib/features/<feature>/`.

Each feature owns, split into `application/` (view-model side) and `presentation/` (widget side):

- its screen widgets (`presentation/`)
- its state class (`freezed`, e.g. `SleepState`)
- its view-model (a Riverpod `Notifier` subclass named `<X>ViewModel`, e.g. `SleepViewModel`, in `application/<x>_view_model.dart`) and the `NotifierProvider` that exposes it
- its own charts, cards, rows, and presentation mapping (`presentation/`)

Feature sub-domains keep their own subdirectory (`reminders/`, `applehealth/`, `maps/`); `homewidgets/` stays flat because it is background-isolate glue with no view-model.

Shared code moves out of a feature only when it is clearly reused by more than one screen.

### 2. Shared shell, feature-owned visuals

The app has a real shared shell for period-based screens, in `MetricDetailScaffold`:

- pull to refresh
- range selector
- period navigator
- date picker
- shared loading/error framing

The metric presentation stays feature-local: the sleep stage timeline and schedule chart ([`lib/features/sleep/presentation/sleep_schedule_chart.dart`](../../lib/features/sleep/presentation/sleep_schedule_chart.dart)), the activity intraday chart, heart trend cards, workout rows, body composition cards.

There is a deliberate, bounded exception: [`lib/ui/charts/`](../../lib/ui/charts) holds *value-over-time* primitives — `PeriodHistoryChart` (which dispatches to a bar chart, a month calendar heatmap, or a year dot heatmap by selected range), plus bar/line/sparkline/heatmap/axis building blocks. These are shared because "a number per day, drawn over a period" carries no metric semantics. They are **not** a universal chart abstraction, and nothing that encodes what a metric *means* belongs there.

### 3. Period-driven detail screens

The canonical interaction model for metric screens:

- `Day / Week / Month / Year`
- a selected anchor date
- previous/next navigation
- direct calendar selection
- forward navigation capped at the current period
- the last selected range remembered independently per detail/list screen

It is implemented by the primitives in [`lib/core/period/`](../../lib/core/period) — `TimeRange` and `DatePeriod` (both in `time_range.dart`), `PeriodSelection`, `PeriodSelectionDriver`, `PeriodLoadQuery`, `PeriodWindows`, `PeriodRangePreferenceKey`, and the calculation/title helpers — and by `MetricDetailScaffold`.

### 4. View-models own screen state and orchestration — but not period selection

Screens stay thin, and they derive nothing. A feature view-model is responsible for:

- triggering loads and refreshes
- combining use-case calls and switching on their `Result`
- **precomputing the display model** at load time (`build<X>Display`, see Known Seams §1)
- dropping stale results (every view-model keeps a monotonic `_generation` guard)
- feature-owned preferences (e.g. the sleep-hours goal, the heart-rate thresholds)
- exposing UI-ready state

**Divergence from the Kotlin design, and it is load-bearing.** In Kotlin, the ViewModel owns the `PeriodSelectionDriver` and the `PeriodRangePreferenceKey` persistence, and `MetricDetailScaffold` is a stateless composable that receives `selectedRange`/`selectedDate` and calls back. Here it is inverted: `MetricDetailScaffold` is a `ConsumerStatefulWidget` that **owns** the `PeriodSelectionDriver`, seeds it from the persisted range for its `rangePreferenceKey`, writes range changes back, and pushes a `PeriodSelection` down through `onSelectionChanged`. The view-model receives that selection and loads against it.

`PeriodSelectionDriver` is referenced by exactly two files: its own, and the scaffold. Do not reintroduce a per-view-model driver — you would end up with two sources of truth for the selected period.

The practical consequence: **period-navigation behaviour is tested through the scaffold widget, not through a view-model unit test.** A view-model test drives `load(PeriodSelection(...))` directly.

### 5. Repositories are feature-facing and permission-aware

Health Connect specifics stay below the feature layer. Repository methods answer feature questions — load workouts for a period, load sleep sessions for a period, load heart summaries for a period, load body entries for a period — and guard the permissions they need before reading.

They do not grow into one grab-bag repository with screen-specific overloads.

Each repository is a `contract/` abstract class plus an `impl/` class over `HealthDataSource`. That split is not ceremony: the contract is what a feature may import, and it is the seam a test overrides (see *Dependency wiring*, below).

### 6. Keep abstractions proportional

The app does not need, and does not have:

- a reducer/effect architecture
- a multi-package split of `lib/`
- a raw Health Connect mirror in drift

Derived values that are genuinely expensive to recompute may be cached — but the *only* such cache today is [`BodyEnergyTimelineCacheStore`](../../lib/data/repository/body_energy_timeline_cache_store.dart), and it lives in `SharedPreferences`, not drift. It stores a versioned envelope keyed by date plus a signature (permission fingerprint + calibration signature + algorithm version), so a permission or config change invalidates it. That signature discipline is the pattern to copy if a second cache is ever warranted.

### 7. Keep the package boundary proportional

`lib/` stays one Dart library unless a second app or a genuinely reusable library needs the code. Prefer directory boundaries first:

- pure models, insights, queries, use cases and preference enums in `lib/domain/`
- period primitives in `lib/core/period/`
- repository-free formatters and UI models in `lib/core/presentation/`
- navigation, provider wiring, theme and local policy in the app directories

The one thing that *is* a separate package is `packages/health_connect_native`, and only because it carries Kotlin/Pigeon platform code.

## Logical Layers In The Current App

### App shell

Responsibilities: app startup, provider-graph bootstrap, theme, locale, route registration, the top-bar shell.

Current files:

- [`lib/main.dart`](../../lib/main.dart)
- [`lib/app.dart`](../../lib/app.dart)
- [`lib/di/providers.dart`](../../lib/di/providers.dart)
- [`lib/state/app_providers.dart`](../../lib/state/app_providers.dart)
- [`lib/navigation/app_router.dart`](../../lib/navigation/app_router.dart)
- [`lib/navigation/app_routes.dart`](../../lib/navigation/app_routes.dart)
- [`lib/ui/components/adaptive_scaffold.dart`](../../lib/ui/components/adaptive_scaffold.dart)
- [`lib/bootstrap/reminder_bootstrap.dart`](../../lib/bootstrap/reminder_bootstrap.dart)

Notes:

- `main()` resolves `SharedPreferences`, builds a `ProviderContainer` with `sharedPreferencesProvider` overridden, and runs the app inside an `UncontrolledProviderScope`. It also calls `FlutterForegroundTask.initCommunicationPort()` (activity-recording notification buttons) and re-registers the home-widget interactivity callback on every start. Reminders are bootstrapped *after* `runApp` and never awaited.
- `main()` has a second, modal entry path: if the launch carries `ACTION_APPWIDGET_CONFIGURE`, it runs `HomeWidgetConfigureApp` and returns — no router, no reminder bootstrap.
- `OpenVitalsApp` (in `app.dart`) is the `MaterialApp.router`. It watches theme mode / dynamic colour / language so a settings change rebuilds the tree; the `GoRouter` itself is cached in `goRouterProvider` so navigation state survives those rebuilds.
- `app_router.dart` owns route registration and the start destination (onboarding unless already completed). It also exports `routeObserver`, the `RouteAware` hook the dashboard uses to reload when a detail screen is popped — the stand-in for Kotlin's `LifecycleEventEffect(ON_RESUME)`.
- `OpenVitalsHomeScaffold` is the top-bar shell. **The app has no bottom navigation**: the dashboard is home, everything else is pushed onto the root navigator with its own back-enabled app bar.

### Dependency wiring

Riverpod replaces Hilt. There is no annotation processor and no generated component; the graph is plain provider declarations.

- **`lib/di/providers.dart`** — the object graph: `healthDataSourceProvider` (native on Android, `UnsupportedHealthDataSource` elsewhere), every `*RepositoryProvider` (contract type, impl instance), the three use-case providers, drift + `BeverageStore`, `PreferencesRepository`, reminders, home-widget services, offline-map import.
- **`lib/state/app_providers.dart`** — app-shell preference providers (`appThemeModeProvider`, `unitSystemProvider`, `unitFormatterProvider`, `appLanguageProvider`, `weekPeriodModeProvider`, …). These bridge `PreferencesRepository`'s `ValueListenable`s into Riverpod, so a settings change rebuilds every watcher.
- **`sharedPreferencesProvider` is the one provider that must be overridden at startup.** It throws by default. `main()` supplies it.

**The override-in-tests seam.** This is the direct replacement for Hilt's `@TestInstallIn`, and it is the reason repositories have a `contract/` type at all: a test wraps the widget or view-model in a `ProviderScope` (or builds a `ProviderContainer`) and overrides exactly the providers it needs:

```dart
ProviderScope(
  overrides: [
    sharedPreferencesProvider.overrideWithValue(prefs),
    sleepRepositoryProvider.overrideWithValue(FakeSleepRepository()),
    grantedHealthPermissionsProvider.overrideWith((ref) async => {HcPermissions.readSleep}),
    unitSystemProvider.overrideWithValue(UnitSystem.metric),
  ],
  child: ...,
)
```

Two of those overrides are not optional:

- `sharedPreferencesProvider` — nothing resolves without it.
- `unitSystemProvider` — the default follows the **host locale**, so a test touching any unit-bearing field asserts different numbers on different machines unless it is pinned. See AGENTS.md §2.

Health-gated screens additionally need `healthConnectAvailabilityProvider` and `grantedHealthPermissionsProvider` (both declared in [`lib/ui/components/health_connect_gate.dart`](../../lib/ui/components/health_connect_gate.dart)), or the gate replaces the screen under test with a permission prompt.

Widget tests also need `localizationsDelegates` on the `MaterialApp`, or `AppLocalizations.of(context)` is null and the generated `!` throws a null-check error whose stack points at the *screen*. It is a harness bug, not a screen bug. See AGENTS.md §4.

### Data access

Responsibilities: availability checks, permission queries, record and aggregate reads, explicit manual-entry writes, mapping platform responses into app models, feature-facing repository APIs.

Current files:

- [`lib/data/source/health/health_data_source.dart`](../../lib/data/source/health/health_data_source.dart) — the low-level facade
- [`lib/data/source/health/native/health_connect_native_data_source.dart`](../../lib/data/source/health/native/health_connect_native_data_source.dart) — the Android implementation over the Pigeon bridge
- [`lib/data/source/health/unsupported_health_data_source.dart`](../../lib/data/source/health/unsupported_health_data_source.dart) — the non-Android fallback
- [`lib/domain/health/health_permissions.dart`](../../lib/domain/health/health_permissions.dart) — `HcPermissions`, `HealthConnectFeatureFlags`, `HealthPermissionService`
- [`lib/data/repository/contract/`](../../lib/data/repository/contract) — `HealthRepository`, `ActivityRepository`, `SleepRepository`, `HeartRepository`, `BodyRepository`, `BodyEnergyRepository`, `CaffeineRepository`, `HydrationRepository`, `NutritionRepository`, `MindfulnessRepository`, `CycleRepository`, `VitalsRepository`, `BleDeviceRepository`, `AppleHealthImportRepository`
- [`lib/data/repository/impl/`](../../lib/data/repository/impl) — the implementations
- [`lib/data/repository/dashboard/dashboard_data_loader.dart`](../../lib/data/repository/dashboard/dashboard_data_loader.dart) — the dashboard read orchestrator
- [`lib/data/prefs/preferences_repository.dart`](../../lib/data/prefs/preferences_repository.dart)
- [`lib/domain/model/`](../../lib/domain/model) — the app models

Current boundary shape:

- `HealthDataSource` is the only thing that knows about the native bridge. **A feature must never import `package:health_connect_native` or `lib/data/source/health/native/`.**
- It is a plain base class, not an `abstract interface class`, and every method has a safe empty default (`[]` / `null` / `0`). That is what makes it subclassable in a test that only cares about two reads. It is also a trap: an un-overridden read makes a screen look permanently empty rather than failing.
- `HealthRepository` is intentionally narrow: availability, permission state, and the app-level permission contract. It is not a data grab bag.
- Feature repositories are thin, permission-aware facades over `HealthDataSource`.
- Manual-entry view-models write through the same feature repositories, so write permission and write behaviour stay below the route.

**The availability invariant.** `HealthDataSource.cachedAvailability` starts at `notSupported`, and every repository gates on it. Any code that builds a `HealthDataSource` **outside the widget tree** must `await HealthRepositoryImpl(dataSource).refreshAvailability()` before any read or write, or every permission reads as missing and every read returns empty, silently. Screens get this for free because `HealthConnectGate` mounts it; background isolates do not. This has already caused four shipped bugs. See AGENTS.md §1 — it is the single most expensive thing in this codebase to relearn.

**Feature gating.** The app pins a `connect-client` that is *ahead* of what most installed Health Connect providers implement. Optional features must be resolved at runtime through `getFeatureStatus`, and permission sets filtered through `filterSupportedPermissions`, both surfaced on `HealthDataSource` and cached into `HealthPermissionService`. Requesting a permission the provider does not know throws. See AGENTS.md §5.

**Units.** Everything below the UI is metric (ml, g, kg, cm, °C). Imperial exists only at the text-field boundary, via `extension MeasurementInput on UnitFormatter` in [`lib/core/presentation/measurement_input.dart`](../../lib/core/presentation/measurement_input.dart). A bare `unitSystem == UnitSystem.imperial` check inside a feature file is a bug. See AGENTS.md §2.

### Local persistence

- [`lib/data/local/open_vitals_database.dart`](../../lib/data/local/open_vitals_database.dart) — drift, schema version 3, **one table: `beverages`**. It mirrors the Kotlin Room database exactly, including the verbatim `CREATE TABLE` used by the legacy migrations.
- [`lib/data/prefs/preferences_repository.dart`](../../lib/data/prefs/preferences_repository.dart) — everything else: widget order, section order, goals, thresholds, remembered ranges, theme, units, language.

**Background isolates must never open drift.** A second connection to the same database file from the alarm/foreground isolate is a corruption risk, and the reminder and widget code paths are explicitly written to avoid it — see the file headers in [`home_widget_alarm.dart`](../../lib/features/homewidgets/home_widget_alarm.dart), [`home_widget_beverage_log.dart`](../../lib/features/homewidgets/home_widget_beverage_log.dart) and [`hydration_reminder_alarm.dart`](../../lib/features/hydration/reminders/hydration_reminder_alarm.dart). The practical cost: a background path cannot see the custom-drink catalog, and that is accepted.

### Shared UI / presentation

Responsibilities: reusable shell components, period navigation UI, loading/error primitives, card building blocks, shared chart primitives.

Current files:

- [`lib/ui/components/metric_detail_scaffold.dart`](../../lib/ui/components/metric_detail_scaffold.dart) — the canonical detail frame
- [`lib/ui/components/period_navigator.dart`](../../lib/ui/components/period_navigator.dart)
- [`lib/ui/components/health_date_picker.dart`](../../lib/ui/components/health_date_picker.dart)
- [`lib/ui/components/metric_card.dart`](../../lib/ui/components/metric_card.dart)
- [`lib/ui/components/loading_state.dart`](../../lib/ui/components/loading_state.dart)
- [`lib/ui/components/permission_callout.dart`](../../lib/ui/components/permission_callout.dart)
- [`lib/ui/components/health_connect_gate.dart`](../../lib/ui/components/health_connect_gate.dart)
- [`lib/ui/charts/`](../../lib/ui/charts) — `PeriodHistoryChart` and the bar/line/sparkline/heatmap/axis primitives
- [`lib/ui/theme/`](../../lib/ui/theme)
- [`lib/core/presentation/`](../../lib/core/presentation) — `UnitFormatter`, `MeasurementInput`, `DisplayValue`, `ScreenError`, `MetricDetailSection` ordering, reorder helpers

Important current details:

- `TimeRange`, `DatePeriod`, `PeriodSelection`, `PeriodSelectionDriver`, `PeriodLoadQuery`, `PeriodWindows`, `PeriodRangePreferenceKey` and the period title/calculation helpers all live in `lib/core/period/`. (`DatePeriod` is declared in `time_range.dart`, not in a file of its own.)
- `PeriodRangePreferenceKey` persists the last selected `TimeRange` per screen — and it is `MetricDetailScaffold`, not the view-model, that reads and writes it.
- `lib/core/presentation/` is a Flutter-side layer with no Kotlin counterpart in the old doc: repository-free formatters and UI models that several features share. Pure formatting belongs here, not in a view-model.
- Metric detail screens have **user-orderable sections**: `MetricDetailSectionId` ([`lib/domain/preferences/metric_detail_section_id.dart`](../../lib/domain/preferences/metric_detail_section_id.dart)) plus `OrderedMetricDetailSections` and the `metricDetailSectionOrderProvider` / `metricDetailSectionEditProvider` in [`lib/core/presentation/metric_detail_sections.dart`](../../lib/core/presentation/metric_detail_sections.dart). A detail screen declares its sections and the shared layer renders them in the user's order, with an edit mode toggled from the app bar.

### Feature layer

Responsibilities: feature state, screen orchestration, feature-specific cards/charts/lists, feature display language.

Current feature directories under [`lib/features/`](../../lib/features): `achievements`, `activity` (incl. `activity/maps`), `body`, `bodyenergy`, `caffeine`, `cycle`, `dashboard`, `heart`, `homewidgets`, `hydration`, `imports` (incl. `imports/applehealth`), `manualentry`, `mindfulness`, `nutrition`, `onboarding`, `readiness`, `recovery`, `settings`, `sleep`, `vitals`.

Two practical notes:

- `features/activity` carries several screen families — the parametric `ActivityMetricScreen` (steps/distance/floors/elevation/wheelchair), `CaloriesScreen`, `ActivitiesScreen` (workout sessions), `ActivityDetailScreen`, `CardioLoadDetailScreen` — plus the offline-map stack under `activity/maps/`. They share `ActivityRepository`, which is why they share a directory. A detail screen still renders **one** metric; sharing a screen widget across metric *ids* is fine, showing several metrics at once in a metric's detail view is not.
- Two features intentionally do not follow the canonical period-detail interaction: `features/caffeine` (a caffeine-specific analytics/setup experience with its own ranges and active-caffeine modelling) and `features/bodyenergy` (a selected-day derived wellness detail, not a `Day / Week / Month / Year` screen).

### Cross-metric insights

Cross-metric insight calculations live in [`lib/domain/insights/`](../../lib/domain/insights) — sleep score, readiness, cardio load, stress, personal baselines, period comparison, data confidence, caffeine — even when the resulting card is rendered by exactly one feature.

Widgets render precomputed insight models. They do not own thresholds, correlation rules, or score adjustments, and **missing secondary data stays neutral**: a missing caffeine record must never reduce a sleep score.

There is also a thin use-case layer, [`lib/domain/usecase/`](../../lib/domain/usecase): `LoadDashboardDayUseCase`, `LoadHeartPeriodUseCase`, `LoadSleepPeriodUseCase`. A use case exists only where a screen genuinely needs two repositories combined (sleep + heart for the HRV correlation; heart + vitals for the ten heart/vitals metrics). Do not add one per screen out of symmetry.

## Screen Families

### Dashboard

The dashboard is deliberately different from the period-based detail screens. It is a daily snapshot, navigated by day only, powered by one aggregated `DashboardData`, and it is the main entry point into feature screens.

Current files:

- [`lib/features/dashboard/application/dashboard_view_model.dart`](../../lib/features/dashboard/application/dashboard_view_model.dart)
- [`lib/features/dashboard/presentation/dashboard_screen.dart`](../../lib/features/dashboard/presentation/dashboard_screen.dart)
- [`lib/features/dashboard/application/dashboard_display.dart`](../../lib/features/dashboard/application/dashboard_display.dart)
- [`lib/data/repository/dashboard/dashboard_data_loader.dart`](../../lib/data/repository/dashboard/dashboard_data_loader.dart)

`DashboardDataLoader` assembles `DashboardData` for the visible metrics only; each metric read is permission-gated and individually error-guarded, so one failing metric does not blank the screen. The view-model loads in two passes — a fast pass for `dashboardQuickMetrics`, then a background pass merged in — mirroring the Kotlin quick/background split.

The dashboard is read-only and must stay summary-first. It is not a second copy of detail-screen logic.

### Metric routing

Metric cards route through **one parametric route**, `/metric/:metricId`, which `metricScreenFor` (in [`app_router.dart`](../../lib/navigation/app_router.dart)) dispatches to a feature screen. The dispatch *order* is load-bearing: the calories and body aggregates intercept their ids before the per-metric activity/body screens can claim them.

> **Correction to the Kotlin doc.** The Kotlin architecture doc instructs that "navigation should call concrete metric screen entry points such as `ProteinScreen` or `RestingHeartRateScreen`, not a public screen with a metric parameter". That rule was never adopted — not here, and not in Kotlin, which has the same `metric/{metricId}` route dispatching through `MetricRouteContent` to parametric screens. The rule that *is* real, and that both apps do honour, is the one underneath it: **a metric's detail view renders that metric**, not every metric that happens to share its repository. Keep that; ignore the file-naming half.

Ids without a dedicated screen still land on `MetricScreen` → `PlaceholderScreen` ([`lib/features/dashboard/presentation/metric_screen.dart`](../../lib/features/dashboard/presentation/metric_screen.dart)). That is a known gap, not a pattern.

There is no global records browser. Entry and session lists live behind the relevant metric card / detail screen.

### Manual entry

Manual entry is a separate screen family and the only app area that initiates Health Connect writes. The Add-entry picker is reached through contextual create actions, not as a primary browsing destination.

Current files: [`lib/features/manualentry/`](../../lib/features/manualentry) — `manual_entry_screen.dart` plus per-metric entry screens and notifiers for hydration, carbs, activity (with GPS recording under `manualentry/activity/`), mindfulness, body measurements and vitals measurements, over the shared `manual_entry_form_scaffold.dart`.

Write permissions can be requested during onboarding or lazily from Add entry / a specific metric entry route. Each write goes straight to Health Connect; the app keeps only local UI preferences (widget order, mindfulness timer settings, the custom drink catalog).

### Period-based detail/list screens

The aligned screens are activity metrics, calories, activities, sleep, heart, vitals overview, body, hydration, nutrition (overview + per-nutrient), mindfulness and cycle. Every one of them passes a `rangePreferenceKey` to `MetricDetailScaffold`.

The scaffold owns:

- the `PeriodSelectionDriver` and the persisted range for its `rangePreferenceKey`
- pull to refresh
- the `TimeRangeSelector`
- the `PeriodNavigator` (forward-capped, tap-to-open date picker)
- an optional sync banner
- the shared error block
- a `headerItems` slot and a `content: List<Widget> Function(DatePeriod)` slot
- an `onSelectionChanged` callback, fired once on the first frame and then on every change

It does **not** own the metric visuals, and it does not load anything.

### Permission surfaces

Onboarding and Settings are not metric screens, but they centralize availability and permission management: [`lib/features/onboarding/`](../../lib/features/onboarding), [`lib/features/settings/`](../../lib/features/settings). They depend on `HealthRepository`, not on feature repositories.

### The Health Connect gate

Health Connect-backed destinations wrap their content in a single shared component, [`HealthConnectGate`](../../lib/ui/components/health_connect_gate.dart), which resolves availability, granted permissions and the sync-enabled preference, and replaces the content with the appropriate gate (unavailable / insufficient access / double-cancel recovery / sync paused) or launches the permission request. A screen passes its `requiredPermissions` and, when it hosts a `MetricDetailScaffold`, `showInlineSyncBanner: false` so the banner is not drawn twice.

This one widget replaces the Kotlin trio of `HealthConnectFeature`, `HealthConnectScreenUxCoordinator` and `WithHealthConnectFeatureScreen`. Do not hand-roll per-screen availability checks, sync banners or permission prompts.

> **Deliberate deviation, documented in the file:** where Kotlin keeps the dashboard visible behind a small inline promo card, the gate here replaces the whole screen for the *unavailable* and *sync-paused* states. Only Kotlin's third promo variant (available and syncing, but minimum permissions missing) is reproduced inline on the dashboard. A parity audit will flag the two missing promo variants; that is intended.

## Canonical Detail Feature Pattern

New metric detail work follows this shape. See [feature-playbook.md](feature-playbook.md) for the step-by-step version.

### 1. Define a feature-owned contract

A `freezed` state class in the feature directory, holding: the selection the scaffold reports back (`selectedRange`, `selectedDate`), the loaded payload (a `*PeriodLoadResult` or a `*PeriodData` from [`lib/domain/query/`](../../lib/domain/query)), `isLoading`, a `ScreenError?`, and any feature-owned preference the screen mutates (a goal, a threshold).

### 2. Reuse the shared period model

`TimeRange`, `DatePeriod`, `PeriodSelection`, `PeriodLoadQuery`, `PeriodWindows` from `lib/core/period/`. Load against the selected period query; do not invent navigation rules.

### 3. Keep the view-model in charge of loading

The view-model: takes the `PeriodSelection` handed to it, builds a `PeriodLoadQuery`, calls the use case, guards staleness with a `_generation` counter, checks `ref.mounted`, then **switches on the returned `Result`** — `Ok` stores the payload *and its precomputed display model*, `Err` maps the `AppFailure` to a `ScreenError` with `failure.toScreenError(fallback: ...)`. It does **not** own the period driver or the range preference — the scaffold does.

Repositories and use cases return `Result<T>` (`lib/core/result/`); they do not throw. Exceptions become failures in exactly one place, `runCatching` in the data layer. `orThrow()` is a **temporary migration bridge** for call sites not yet switched over — do not add new ones, and see `docs/engineering/refactor-tracker.md` for what is left.

### 4. Use `MetricDetailScaffold` as the shell

Pass `rangePreferenceKey`, `onRefresh`, `onSelectionChanged`, `isLoading`, `screenError`, and a `content` builder. Add the new key to `PeriodRangePreferenceKey` if the screen needs its own remembered range. Persist range changes only; the selected date stays screen state.

### 5. Keep visuals local to the feature

A custom chart, row or timeline stays in the feature directory unless another feature genuinely needs the same thing. Reach for `lib/ui/charts/` only when the thing you are drawing is "a value per day".

## Repository Rules For New Work

### Use `HealthRepository` only for app-level concerns

Availability, permission contract access, granted/missing permissions. Do not add feature-detail reads there.

### Add or extend feature repositories for feature data

Add the method to the `contract/` class and implement it in `impl/`. Each repository should guard its required permissions, call `HealthDataSource`, and return app models the notifier can use directly.

### Keep queries period-oriented

Prefer APIs that take a `DatePeriod` or a `PeriodLoadQuery` and return a feature result object — not another ad hoc `loadX(start, end)` overload. When a screen needs current, previous and baseline windows, use `PeriodWindows` and return one bundled result. Keep granular APIs only for real entry-list/detail reads.

## What Should Stay Shared vs Local

### Shared

- period calculation, windows, titles
- period/day navigation components and the date picker
- the detail-screen scaffold and the Health Connect gate
- loading/error components, `MetricCard`, chips, section headers
- unit formatting (`UnitFormatter`) and the imperial text-field boundary (`MeasurementInput`)
- value-over-time chart primitives (`lib/ui/charts/`)

### Feature-local

- metric-specific charts and timelines
- metric-specific list rows and summaries
- the presentation mapping from repository payload to display model
- metric-specific empty-state language when the domain meaning differs

## Known Seams And Next Refactors

Real seams in the current codebase. None of them blocks feature work.

### 1. The view-model precomputes the display state *(being reversed — migration in progress)*

**The rule, now:** a view-model builds its feature's display model **at load time** and stores it on its state. Widgets render `state.display` and derive nothing — no sorting, no folding, no grouping, no unit conversion in a build path. Flutter's app-architecture guidance is explicit that logic does not live in widgets, and the Kotlin originals precomputed a `SleepDisplayState` / `HeartDisplayState` for the same reason. The port dropped that on the grounds that the derivations were cheap; they stopped being cheap (`activities_ordered_sections.dart` folded five metrics and bucketed a period on every rebuild of a scrolling screen).

The shape, per feature — `lib/features/mindfulness/` is the reference:

- `application/<x>_display.dart` — a `freezed` `<X>Display` plus a **pure** top-level `build<X>Display(data)`. No clock, no `ref`, no I/O: this is the unit-test seam (`test/features/mindfulness/mindfulness_display_test.dart`).
- `application/<x>_view_model.dart` — calls it once per successful load, stores the result on the state's `display` field.
- `presentation/` — renders it. A trivial O(1) getter on the state class is still fine; a loop is not.

Migrated: mindfulness. Remaining: everything else, per `docs/engineering/refactor-tracker.md`. While a feature is unmigrated its screen still derives — that is a known state, not a licence to add more.

### 2. Period selection lives in the widget, not the notifier

`MetricDetailScaffold` owns the `PeriodSelectionDriver` (principle 4). This makes screens trivially uniform and makes range persistence a one-line `rangePreferenceKey`, but it means the notifier cannot be unit-tested for period navigation, and a screen that needs a non-standard period rule has to work around the scaffold rather than through it. `features/caffeine` and `features/bodyenergy` already sit outside it. If a third screen needs to escape, that is the signal to make the driver injectable.

### 3. `metric_card.dart` is still a grab bag

[`lib/ui/components/metric_card.dart`](../../lib/ui/components/metric_card.dart) currently holds `MetricCard`, `MetricCardPlaceholder`, `MetricValueRow`, `SourceChip`, `SectionHeader` and `TimeRangeSelector`. Fine at this repo size; split by responsibility if shared UI keeps growing. (The same seam exists in the Kotlin file — it was ported, not introduced.)

### 4. Residual port stubs

- `/metric/:metricId` falls through to `MetricScreen` → `PlaceholderScreen` for ids with no dedicated screen ([`metric_screen.dart`](../../lib/features/dashboard/presentation/metric_screen.dart), `TODO(phase5)`).
- `TopLevelDestination` in [`app_routes.dart`](../../lib/navigation/app_routes.dart) is **dead code** — a bottom-navigation / `StatefulShellRoute` design that was abandoned (the shell has no bottom nav). It has zero references in `lib/` or `test/`. Delete it when touching that file.
- A handful of `TODO(phase6)` gaps remain in hydration quick-add, the mindfulness timer UI, and the mindfulness entry screen.

### 5. Background work is narrow and explicit

Two mechanisms, both deliberate:

- `android_alarm_manager_plus` — periodic home-widget refresh ([`home_widget_alarm.dart`](../../lib/features/homewidgets/home_widget_alarm.dart)) and the hydration/mindfulness reminder alarms. The alarm wakes the app so the reminder can re-check *today's actual intake* before notifying, rather than firing a pre-scheduled notification blind. The alarms are deliberately INEXACT (`setAndAllowWhileIdle`): exact alarms need `USE_EXACT_ALARM`, which Google restricts to alarm-clock and calendar apps, so declaring it on a health dashboard risks the app being pulled. See the comment in `android/app/src/main/AndroidManifest.xml`.
- `flutter_foreground_task` — the Apple Health import ([`apple_health_import_task_handler.dart`](../../lib/features/imports/applehealth/apple_health_import_task_handler.dart)) and activity recording, both long-running and user-visible.

Every isolate here builds its own object graph, opens **no drift**, must call `refreshAvailability()` first, and must use `lookupAppLocalizations(...)` rather than a `BuildContext`. Do not design a new feature as if a general background-sync layer or a raw-record database exists.

### 6. Do not over-correct into a universal framework

Still avoid:

- a universal chart abstraction that hides metric semantics
- a giant abstract base notifier
- a premature multi-package split of `lib/`
- a full reducer/effect framework for straightforward screens
- a Kotlin-only reimplementation of something a cross-platform plugin already does — iOS/HealthKit is planned, and that is a double-maintenance bill (AGENTS.md §7)

## Localization

ARB is the source of truth. `lib/l10n/app_*.arb` are the catalogs, `app_en.arb` is the template, and **Weblate writes to these files directly**. Never regenerate them from the Kotlin `strings.xml` — that destroys every translation newer than the snapshot. `tool/xml_to_arb.dart` has been deleted and must not be resurrected.

Add a string to `app_en.arb`, run `flutter gen-l10n`, commit the regenerated `lib/l10n/app_localizations*.dart`. Placeholders are ICU (`{arg0}`). The gate is `dart run tool/verify_l10n.dart`. Details in [translations.md](translations.md).

Every user-visible string goes through `AppLocalizations`. Outside the widget tree, use `lookupAppLocalizations(...)`.

## Success Criteria

The architecture is working when:

- a new metric screen can be added without copying shell UI
- Health Connect reads stay below the feature layer, behind a repository contract
- feature repositories stay narrow and query-oriented
- screens stay thin and notifiers stay free of formatting
- charts stay understandable because metric-specific visuals stay local
- shared extraction happens for scaffolding, not for semantics
- a background feature works the first time, because it resolved availability before reading
