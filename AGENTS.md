# AGENTS.md

Implementation guide for coding agents working in this repository.

Read this before adding a feature, extending a metric screen, or touching health, l10n, or background code.

This app is a 1:1 Flutter port of the Kotlin OpenVitals app, which it REPLACED in place on this same repository (same Play listing, same package `tech.mmarca.openvitals`, same signing key). The Kotlin sources no longer exist in the working tree -- read them from git history: `git show 23c14d0:app/src/main/kotlin/...`. Behaviour parity with the Kotlin source is the default requirement; deviate only with a reason, and write the reason down.

## Source Of Truth

- [docs/README.md](docs/README.md): doc index
- [docs/engineering/architecture.md](docs/engineering/architecture.md): architecture and target direction
- [docs/engineering/feature-playbook.md](docs/engineering/feature-playbook.md): step-by-step guide for adding a feature
- [docs/features/feature-map.md](docs/features/feature-map.md): feature to route/screen mapping
- [docs/engineering/translations.md](docs/engineering/translations.md): ARB, Weblate, and the l10n gate

If code and docs disagree, prefer the docs for new work and refactor toward them incrementally.

Caveat while the port completes: the docs under `docs/engineering/` still carry Kotlin-era mechanics in places (Gradle tasks, Hilt, Compose). Their *principles* are binding; their *Kotlin specifics* are stale — the Flutter equivalents are in this file and in `README.md`.

## Golden Path For A New Metric Feature

1. Define the feature contract: screen state, user actions, derived display fields.
2. Make it period-driven: `Day / Week / Month / Year`, a selected anchor date, previous/next navigation, capped at the current period.
3. Keep the frame reusable, keep the charts specific: reuse `lib/ui/components/metric_detail_scaffold.dart` and `lib/ui/components/period_navigator.dart`; keep metric-specific cards and charts inside the feature directory.
4. Keep repository APIs query-oriented: pass a `DatePeriod` (`lib/core/period/`) or a query object from `lib/domain/query/`, not another ad hoc `loadX(start, end)` overload.
5. Register the feature from the dashboard: dashboard card, route in `lib/navigation/app_routes.dart` + `lib/navigation/app_router.dart`, screen title.
6. Update the docs if the pattern evolves.

## Layout Rules

Feature code lives under `lib/features/<feature>/`, split into two subdirectories: `application/` (the view-model, its `freezed` state, and — as features migrate — the pure `build<X>Display` functions) and `presentation/` (screens, cards, charts). Feature sub-domains keep their own subdirectory (`reminders/`, `applehealth/`, `maps/`; settings cards live in `presentation/cards/`). See `lib/features/sleep/` or `lib/features/heart/` for the intended shape. `homewidgets/` is the one flat exception — background-isolate glue with no view-model.

Shared code lives in:

- `lib/ui/components/` — reusable shell components (no feature business logic)
- `lib/ui/charts/`, `lib/ui/theme/` — shared chart and theme primitives
- `lib/core/period/` — period math and window formatting
- `lib/core/presentation/` — repository-free formatters and UI models
- `lib/domain/model/`, `lib/domain/insights/`, `lib/domain/preferences/` — pure models, calculations, preference enums
- `lib/data/repository/contract/` + `impl/` — the repository boundary
- `lib/di/providers.dart`, `lib/state/app_providers.dart` — provider wiring

### State

One view-model per screen — a Riverpod `Notifier` / `AsyncNotifier` subclass named `<X>ViewModel` in `application/<x>_view_model.dart` — with state as a `freezed` class. (MVVM per the Flutter app-architecture guide; the Riverpod notifier IS the view-model, so nothing feature-side carries the Notifier suffix.) A view-model owns loading state, owns the selected range/anchor date, calls use-cases/repositories, and exposes UI-ready state. It must not carry large formatting blocks (that is `lib/core/presentation/`), must not re-implement period math, and must not mirror raw Health Connect record shapes when a cleaner UI model is warranted.

**Derivation happens at load time, in the view-model** — never in a build path. A feature's `application/<x>_display.dart` holds a `freezed` `<X>Display` and a pure `build<X>Display(data)`; the view-model calls it on `Ok` and stores it on the state; the screen renders `state.display` and sorts/folds/groups nothing. `lib/features/mindfulness/` is the reference. (Migration in progress — `docs/engineering/refactor-tracker.md` says which features are done.)

Dependencies come from providers, not constructors reaching into globals. After editing an annotated class (`freezed`, `json_serializable`, `riverpod`, `drift`), regenerate:

```bash
dart run build_runner build --delete-conflicting-outputs
```

### Errors

Repositories and use cases return `Result<T>` (`lib/core/result/`) — `Ok` or `Err(AppFailure)`. They do **not** throw: exceptions become failures in exactly one place, `runCatching` in the data layer. A view-model switches on the `Result` and maps a failure to the UI's `ScreenError` with `failure.toScreenError(fallback: ...)`.

`orThrow()` is a temporary bridge for call sites the migration has not reached. Do not add new ones.

### Repositories

The boundary over Health Connect is deliberately narrow. `lib/health/health_data_source.dart` is the only thing that knows about the native bridge; the repositories in `lib/data/repository/contract/` are the only thing features may call. Do not import `package:health_connect_native` or `lib/health/native/` from a feature.

When adding capability, extend the feature-oriented repository API; do not widen `HealthRepository` into a grab bag.

### Health Connect screens

Health Connect-backed destinations go through the shared gate, `lib/ui/components/health_connect_gate.dart`, plus `lib/ui/components/permission_callout.dart`. Do not hand-roll per-screen availability checks, sync banners, or permission prompts.

## Invariants That Have Already Been Broken

These are not style preferences. Each one is a bug that shipped.

### 1. Hand-built `HealthDataSource` must resolve availability first

Any code that constructs a `HealthDataSource` **outside the widget tree** MUST `await HealthRepositoryImpl(dataSource).refreshAvailability()` before any read or write.

`HealthDataSource.cachedAvailability` starts at `notSupported`, and every repository gates on it: without that call, **every permission reads as missing and every read returns empty — with no error**. Screens get this for free because `HealthConnectGate` mounts it; background isolates do not.

This has caused four separate bugs: `lib/features/homewidgets/home_widget_refresher.dart` (widgets showed "grant permission"), `lib/features/homewidgets/home_widget_beverage_log.dart` (one-tap logging silently did nothing), and both reminder alarms (`lib/features/hydration/reminders/hydration_reminder_alarm.dart`, `lib/features/mindfulness/reminders/mindfulness_reminder_alarm.dart` — today's intake always read as 0). If a background feature "does nothing", check this **first**, before the platform, the permissions, or the plugin.

### 2. Storage is metric; imperial is a UI-boundary concern

All quantities are stored metric (ml, g, kg, cm, °C). Imperial is a *view* preference applied only when labelling and parsing text fields, via `extension MeasurementInput on UnitFormatter` in `lib/core/presentation/measurement_input.dart`.

Never add a bare `unitSystem == UnitSystem.imperial` check or a local conversion constant in a feature file — add or reuse a helper on `MeasurementInput`. New entry screens label with `formatter.<x>InputUnit` and canonicalize with `formatter.<x>InputTo<Metric>`.

Test gotcha: the default unit system derives from the host locale, so a widget test touching a unit-bearing field must override `unitSystemProvider` (`lib/state/app_providers.dart`) or it asserts different numbers on different machines.

### 3. ARB is the l10n source of truth, and Weblate edits it

`lib/l10n/app_*.arb` are the catalogs; `app_en.arb` is the template. **Weblate writes to these files directly.** Never regenerate them from the Kotlin `strings.xml`; that would destroy every translation newer than the snapshot. `tool/xml_to_arb.dart` is gone and must not be resurrected.

Add a new string to `app_en.arb`, run `flutter gen-l10n`, and commit the regenerated `lib/l10n/app_localizations*.dart`. Placeholders are ICU (`{arg0}`), not `%1$s`. The gate is `dart run tool/verify_l10n.dart`. Details: [docs/engineering/translations.md](docs/engineering/translations.md).

### 4. Widget tests need the localization delegates

Every widget-test `MaterialApp` must carry:

```dart
localizationsDelegates: AppLocalizations.localizationsDelegates,
supportedLocales: AppLocalizations.supportedLocales,
```

Without them `AppLocalizations.of(context)` is null and the generated `!` throws `Null check operator used on a null value` — with a stack pointing at the *screen*, so it reads like a production bug. It is not. Fix the harness.

Outside the widget tree (background isolates, foreground services), there is no context: use `lookupAppLocalizations(...)` as in `lib/features/homewidgets/home_widget_refresher.dart`.

### 5. Gate on device support, not on the pinned client

The app pins `connect-client` 1.2.0-alpha04, which is *ahead* of what most installed Health Connect providers implement. Feature availability must be resolved at runtime through `getFeatureStatus` and permissions filtered through `filterSupportedPermissions` (see `lib/health/health_permissions.dart`). Requesting a permission the provider does not support throws.

### 6. Home-screen widgets are render-only

The Glance composables under `android/` only render a snapshot; all logic is in Dart (`lib/features/homewidgets/`). One shared prefs file backs every widget, so keys must stay namespaced per widget and per `appWidgetId`. The background isolate must never open drift. Keep `flutter_deeplinking_enabled=false` in `android/app/src/main/AndroidManifest.xml` — flipping it breaks every widget tap and every "Open with" intent.

### 7. Do not reimplement plugins first-party

iOS/HealthKit is planned, so a Kotlin-only reimplementation of something a cross-platform plugin already does is a future double-maintenance bill. Use the plugin; write native code only for what no plugin covers (that is what `packages/health_connect_native` and the recording sensor channels exist for).

After adding any Android-side plugin, build the APK once: some plugins fail only at APK link time under AGP 9 (`cannot find symbol` in `GeneratedPluginRegistrant`).

## Do Not Copy These Patterns

- ad hoc `Future`/`setState` loading inside a `StatefulWidget` for new feature work — use a notifier
- a new navigator/router abstraction per feature — routes go in `lib/navigation/`
- a new screen-specific period helper when `lib/core/period/` already has one
- giant abstract base notifiers
- a universal chart abstraction that hides metric semantics
- Health Connect availability/permission UI outside `health_connect_gate.dart`
- `Platform.isAndroid` branches inside features — platform differences belong behind `HealthDataSource`
- hardcoded English in a screen — every user-visible string goes through `AppLocalizations`

## Before Starting

Read [docs/engineering/feature-playbook.md](docs/engineering/feature-playbook.md). There is no sibling Kotlin checkout: if you need the original behaviour, read it out of git history (`git show 23c14d0:app/src/main/kotlin/...`).

If the work would mean copying code out of an existing detail screen, stop and ask: "should this be a shared scaffold/component first?" The answer is usually yes for the shell and no for the chart body.

Before you push:

```bash
flutter test
flutter analyze lib test
dart run tool/verify_l10n.dart
flutter gen-l10n && git diff --exit-code lib/l10n
git diff --check
```
