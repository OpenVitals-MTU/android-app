# Architecture

## Purpose

This document describes the architecture of OpenVitals as it exists today, plus the direction new work should follow.

The repo now has one Android app module for the local app. The goal is to keep boundaries clear enough that new metrics can be added without copying screen scaffolding, period math, or Health Connect plumbing everywhere, while keeping code app-local until there is a concrete need for a new module.

## Current Snapshot

- App namespace: `tech.mmarca.openvitals`
- Project shape: two Gradle modules. The phone app is `:app`, under `app/`; `:domain` holds the app-local pure code the app depends on (see [Module `:domain`](#module-domain)). The rest of this document is about `:app` unless it says otherwise; package names are the same across `:app` and `:domain`. Watch and scale support is a separate app, [OpenWatch](https://github.com/OpenVitals-MTU/OpenWatch), which writes to Health Connect; the two share no code
- Dependency wiring: Hilt in `:app`, rooted at [`OpenVitalsApp`](../../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt); modules are `di/AppModule.kt`, `di/RepositoryModule.kt`, `di/PreferencesModule.kt`, and `di/RemindersModule.kt`
- UI stack: Jetpack Compose + Material 3 app shell + Navigation Compose + `ViewModel` + coroutines/`StateFlow`
- Health data backend: Health Connect AndroidX client, wrapped by [`HealthConnectManager`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectManager.kt)
- App-local domain code: pure models, insight calculations, and preference enums under [`domain`](../../domain/src/main/kotlin/tech/mmarca/openvitals/domain), in the `:domain` module
- Shared period shell: in place and used by all metric detail/list screens
- Feature repositories: in place for activity, sleep, heart, body, body energy, caffeine, hydration, nutrition, mindfulness, cycle, and vitals
- Dashboard: still a dedicated day-based summary screen, not a period-detail screen
- Manual entry: separate from the dashboard and writes explicit user-entered records directly to Health Connect
- Room is at schema version 18. It holds derived summary caches plus the tables Health Connect cannot represent (`synced_record_origins`, the food catalog, the cycle journal, cycle exclusions, pill intakes, and the index of kept medical documents); Health Connect remains the source of truth for everything it has a record type for
- WorkManager is used for user-started Apple Health imports, offline map imports, and the home widget refresh
- Phone-to-phone Health Connect sync lives under [`features/devicesync`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync) and runs over Bluetooth Classic RFCOMM
- A one-time Flutter-to-Kotlin data migrator lives under [`data/migration`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/migration) and runs from `OpenVitalsApp.onCreate()`

Most importantly, body and entry/session browsing now live in metric-owned detail screens. The former global Browse destination is no longer part of the app architecture.

### Top-Level Package Map

| Package | Owns |
|---|---|
| `comaps/` | the CoMaps navigation provider: the `ContentResolver` source and the guidance feed |
| `core/` | app-wide primitives: `presentation`, `performance`, `diagnostics` in `:app`; `period`, `stats`, `geo`, `fit`, `export` (share staging), `privacy` (masking text that leaves the app) in `:domain` |
| `data/` | `local` (Room), `repository` (feature-facing repositories + `contract` interfaces), `sync` (history/backfill services), `migration` (one-time Flutter import) |
| `di/` | `AppModule`, `RepositoryModule`, `PreferencesModule`, `RemindersModule` |
| `domain/` | pure code: `model`, `insights`, `preferences`, `cycle`, `dashboard` (the aggregator), `report` (report roll-ups), `medical` (FHIR parsing, id repair, source grouping, the patient check, record summaries, the export bundle) in `:domain`; `query` and `usecase`, which reach into the repositories, in `:app` |
| `features/` | one package per user-facing feature area |
| `healthconnect/` | the Health Connect integration boundary: manager, per-area readers, permission/UX services |
| `navigation/` | routes and graph registration |
| `sensors/ble/` | live BLE sensor streaming during activity recording |
| `ui/` | shared components, charts, theme |

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

This pattern is implemented today by app-local period primitives in `core/period` and shell components in `ui/components`.

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
- a raw Health Connect mirror

Two Room caches exist, and each has a reason: the vitals daily cache, because seven vitals types have no Health Connect aggregate, and `heart_rate_days`, because the day view's average needs every sample.
The first is invalidated by a permission-and-zone fingerprint, the second by a per-day signature of the hourly aggregates.
Everything else reads Health Connect on every load, through one reader function per metric and shape. A new cache needs a fingerprint and a reason as strong as these.

### 7. Keep module boundaries proportional

Package boundaries come first; a module is a build-time decision, not a tidying one. There is one extraction, `:domain`, the app-local pure code, because lint and the unit tests of one 215k-line module ran on one thread for ten minutes: a module with no dependency on the rest of the app gets its own lint and test tasks, which run beside the app's and stay cached while only the app changes. A module is extracted when two apps need the same code, or when a layer that nothing below it depends on is large enough to shorten the gate. Prefer package boundaries first:

- app-local domain models and calculations in `domain` (the `:domain` module)
- period primitives in `core/period` (also `:domain`)
- app-only resources, navigation, Hilt wiring, and local policy in `:app`

## Logical Layers In The Current App

These are logical layers inside the local app module.

### App shell

Responsibilities:

- app startup
- Hilt application/component setup
- theme setup
- route registration
- adaptive top app bar, navigation suite, and global action shell

Current files:

- [`OpenVitalsApp.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt)
- [`MainActivity.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/MainActivity.kt)
- [`di/AppModule.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/di/AppModule.kt)
- [`navigation/AppNavigation.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/navigation/AppNavigation.kt)
- [`navigation/Screen.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/navigation/Screen.kt)
- [`ui/components/OpenVitalsAdaptiveScaffold.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/OpenVitalsAdaptiveScaffold.kt)

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

- [`healthconnect/HealthConnectManager.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectManager.kt)
- [`data/repository/HealthRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HealthRepository.kt)
- [`data/repository/ActivityRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/ActivityRepository.kt)
- [`data/repository/SleepRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/SleepRepository.kt)
- [`data/repository/HeartRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HeartRepository.kt)
- [`data/repository/BodyRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/BodyRepository.kt)
- [`data/repository/HydrationRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/HydrationRepository.kt)
- [`data/repository/NutritionRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/NutritionRepository.kt)
- [`data/repository/MindfulnessRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/MindfulnessRepository.kt)
- [`data/repository/CycleRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/CycleRepository.kt)
- [`data/repository/VitalsRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/VitalsRepository.kt)
- [`data/repository/PreferencesRepository.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/PreferencesRepository.kt)
- feature-oriented model files under [`domain/model`](../../domain/src/main/kotlin/tech/mmarca/openvitals/domain/model)

Current boundary shape:

- `HealthConnectManager` is the low-level integration wrapper. It talks to the AndroidX client, performs reads, writes explicit manual entries, and maps results into app models.
- `HealthRepository` is now intentionally narrow: Health Connect availability and permission state. Dashboard loading is `DashboardDataLoader`, behind `LoadDashboardDayUseCase`.
- Feature repositories are thin, permission-aware facades over `HealthConnectManager`.
- Manual entry ViewModels use the same feature repositories for writes, so write permission and write behavior stay below the UI route.
- `healthconnect` depends on app-local domain models; repositories depend on `healthconnect` and `domain`. The readers, writers and mappers import nothing from `data.repository`. Five permission and gate classes do: they read `PreferencesRepository`, and one reads the `HealthRepository` contract. `HealthConnectLayeringTest` lists them and fails on a sixth.

This is a meaningful improvement over the earlier centralized repository approach. New feature reads should follow the feature-repository pattern, not expand `HealthRepository`.

Some repositories are now split into a `data/repository/contract/` interface and an implementation, bound in `di/RepositoryModule.kt`. Use that split when a repository is a seam another subsystem writes through, not by default.

### Local storage

[`OpenVitalsDatabase`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/local/OpenVitalsDatabase.kt) is at `VERSION = 18`, with migrations declared in its companion object and listed once, in `ALL_MIGRATIONS`, which the database builder takes. Room exports the schema of each version to [`app/schemas`](../../app/schemas); the files are committed.

To change the schema: raise `VERSION`, write the migration, add it to `ALL_MIGRATIONS`, build once, and commit the new schema file. `OpenVitalsDatabaseSchemaTest` fails until all of that is done: it replays every migration's statements and compares the tables they leave with the exported schema. It can do that on the JVM because the migrations only create and drop tables. A migration that alters a table needs a real database, so add `room-testing` and a `MigrationTestHelper` test with it; the exported schemas are what that helper reads.

| Table | Package | Purpose |
|---|---|---|
| `beverages` | `data/local/beverage` | user and preloaded beverage catalog |
| `vitals_daily_aggregates`, `vitals_sync_cursors` | `data/local/vitalscache` | derived daily summary cache and change-token cursors |
| `body_energy_days`, `body_energy_buckets` | `data/local/bodyenergy` | the Body Energy chain, moved off SharedPreferences in migration 4 → 5 |
| `synced_record_origins` | `data/local/syncorigin` | the source app of each record received from another phone, added in migration 6 → 7 |
| `heart_rate_days` | `data/local/heartratecache` | each day's heart-rate average from raw samples, read again when the day's hourly aggregates change, added in migration 10 → 11 |
| `foods`, `food_nutrients` | `data/local/food` | the user's food catalog and each food's nutrients, added in migration 11 → 12; a logged portion is a Health Connect nutrition record, not a row |
| `cycle_journal_entries`, `cycle_exclusions` | `data/local/cycle` | the cycle day log Health Connect has no record type for (pain, mood, energy, desire, symptoms, notes, pregnancy test, temperature disturbances, cervical sensation, cervix opening, firmness and position) and the cycles kept out of the estimate, added in migration 12 → 13; the cervix and desire columns in 16 → 17 |
| `pill_intakes` | `data/local/cycle` | the days the contraceptive pill was marked as taken; the scheme itself is a cycle preference, added in migration 13 → 14 |
| `medical_documents`, `medical_document_records` | `data/local/medical` | the files kept from medical imports, stored under `files/medical_documents`, and the Health Connect records each one gave; a file is data Health Connect cannot hold, and the records stay in Health Connect. Added in migration 14 → 15 |

Three tables the device layer owned (`garmin_wellness_samples`, added in 5 → 6; `garmin_sleep_minutes`, 9 → 10; `scale_weigh_ins`, 15 → 16) left with that layer: `MIGRATION_17_18` drops all three. Their data lives in OpenWatch now.

### Startup and the one-time Flutter migration

[`data/migration`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/migration) holds a one-shot importer that brings Flutter-era user data forward. The Kotlin build installs over the Flutter build with the same `applicationId` and certificate, so the app's private directory survives the swap, and the Kotlin-era files still sitting in it are about a month stale. The migrator therefore overwrites them with the Flutter values on its guarded first run.

It is deliberately split into two phases around `super.onCreate()` in [`OpenVitalsApp`](../../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt). This ordering is load-bearing, not stylistic:

1. **Preferences, before `super.onCreate()`.** `@HiltAndroidApp` member-injects the `Application` *during* `super.onCreate()`, and `PreferencesRepository` eagerly snapshots its `SharedPreferences` into `StateFlow`s at construction. Any preference write that lands after that point is invisible to the running app. So `FlutterDataMigrator` is constructed by hand rather than injected, and every preference write goes through raw `SharedPreferences` with `commit()` rather than through a repository that may not exist yet.
2. **Database import, after `super.onCreate()`.** The database phase needs the Hilt-provided Room singleton, which cannot exist before the Hilt component does. Room singletons are created lazily on first request, and no `Activity` can exist yet, so resolving `OpenVitalsDatabase` through `FlutterMigrationEntryPoint` immediately after `super.onCreate()` is both possible and safe.

`migrateIfNeeded()` returns whether a migration is in flight; only then does `onCreate` call `importDatabaseAndFinish(...)`. The migrator never throws, sets its one-shot flag regardless of per-step failures so a failing migration cannot retry on every launch, never deletes a Flutter file, and no-ops on a fresh install after a single file stat.

If you add a new preference or store that must survive the migration, add it to phase 1. Do not move phase 1 work behind Hilt.

### Shared UI / presentation

Responsibilities:

- reusable shell components
- period selection primitives
- date navigation UI
- loading/error primitives
- dashboard/detail card building blocks

Current files:

- [`ui/components/MetricDetailScaffold.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffold.kt)
- [`ui/components/PeriodNavigator.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PeriodNavigator.kt)
- [`ui/components/DateNavigation.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/DateNavigation.kt)
- [`ui/components/MetricCard.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricCard.kt)
- [`ui/components/LoadingState.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/LoadingState.kt)
- [`ui/components/PullToRefreshBox.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PullToRefreshBox.kt)
- [`ui/components/PermissionCallout.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/PermissionCallout.kt)

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

- [`features/achievements`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/achievements)
- [`features/onboarding`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/onboarding)
- [`features/dashboard`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard)
- [`features/activity`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/activity)
- [`features/sleep`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/sleep)
- [`features/heart`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/heart)
- [`features/vitals`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/vitals)
- [`features/body`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/body)
- [`features/bodyenergy`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/bodyenergy)
- [`features/caffeine`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/caffeine)
- [`features/cycle`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/cycle)
- [`features/devicesync`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync)
- [`features/homewidgets`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/homewidgets)
- [`features/hydration`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/hydration)
- [`features/imports/applehealth`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/imports/applehealth)
- [`features/imports/csv`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/imports/csv)
- [`features/manualentry`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry)
- [`features/mindfulness`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/mindfulness)
- [`features/nutrition`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/nutrition)
- [`features/readiness`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/readiness)
- [`features/recovery`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/recovery)
- [`features/settings`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/settings)
- [`features/workoutplans`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/workoutplans) — plan list/builder; `WorkoutPlanStepMapping.kt` is the one seam between Health Connect plans and the activity form's step rows

Two of these are not metric features and follow their own shape: `features/devicesync` is a phone-to-phone sync wizard, and `features/imports/*` are import workflows.

One practical note: `features/activity` currently contains two screen families:

- concrete metric entry screens such as `StepsScreen`, `DistanceScreen`, `CaloriesOutScreen`, `ActiveCaloriesScreen`, `FloorsScreen`, and `ElevationScreen`
- `ActivitiesScreen` for workout sessions

That is a reasonable local compromise today because these screens share `ActivityRepository`, but route-facing composables should stay metric-specific. Shared renderers inside a feature package are acceptable when they only remove local duplication and do not make the user-facing detail screen show several metrics at once.

Two implemented features intentionally do not follow the canonical period-detail interaction:

- `features/caffeine` is a caffeine-specific analytics and setup experience with custom ranges, active-caffeine modeling, timing guidance, and beverage/nutrition context.
- `features/bodyenergy` is a selected-day derived wellness detail, not a `Day / Week / Month / Year` metric screen.

### Cross-metric insights

Cross-metric insight calculations should live in `domain/insights`, even when the card is rendered by one feature. The feature ViewModel or use case can load the secondary signal, and the presentation mapper can attach the resulting insight to the feature display state.

This keeps metric UI declarative: composables render precomputed insight models and do not own thresholds, correlation rules, or score adjustments. Missing secondary data should remain neutral. For example, planned caffeine-aware sleep insights should attach caffeine signals to sleep presentation state only after the domain signal and mapper are implemented; missing caffeine records must not reduce sleep scores. Today, caffeine timing guidance lives in the standalone caffeine feature.

## Shared FIT Decoding

[`core/fit/FitDecoder.kt`](../../domain/src/main/kotlin/tech/mmarca/openvitals/core/fit/FitDecoder.kt), in `:domain`, is the one FIT decoder in the app. It was extracted from the route importer's private decoder once a second consumer appeared.

It is a generic container walk and nothing more: it reads the FIT header, definition and data messages, compressed timestamps and developer fields, and returns `FitMessage` values keyed by global message number. It has no message allowlist and no semantics.

Interpretation lives with the consumer: [`features/manualentry/activity/routeimport/FitRouteParser.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/FitRouteParser.kt) interprets activity, course, and workout files, and the nightly HRV readings a FIT wellness file carries for `features/imports/garmin/FitHrvImportService`. A second FIT consumer should follow the same split: reuse `FitDecoder`, own its interpretation.

## Module `:domain`

The app-local code that nothing in the rest of the phone app sits below, in `domain/`: the packages `domain/model`, `domain/insights`, `domain/preferences`, `domain/cycle`, `domain/dashboard`, `domain/report` and `domain/medical`, and the primitives `core/period`, `core/fit`, `core/geo`, `core/stats`, `core/export` and `core/privacy`. Package names are unchanged, so an import does not say which module a class is in. `:app` depends on it.

It is an Android library, not plain Kotlin: a few files use Health Connect record constants (`ExerciseTypeTraits`, `PlannedExerciseSteps`, the activity splits) and the platform's locale data (`SystemUnitSystemProvider`, `AppLanguage`), and Android lint then still checks it. It has the app's build types (`ci`, `nightly`), so the app's `ci` variant takes the module's `ci` variant and the module's tests and lint run on that one compile. Nothing in it is injected: it holds no Hilt module, and its classes are constructed by the app's repositories and the app's `di/` modules.

Its test fixtures (`testFixtures`) hold the FHIR, C-CDA and SMART Health Card fixtures that both its own tests and the app's import tests use, and the privacy sentinels (`PrivacySentinels`) that tests of crash drafts, logs and reports seed and look for, with the FHIR sample files under `domain/src/testFixtures/resources/fhir/`. Its tests and lint are part of the phone gate (`verifyCiUnitTest` depends on `:domain:testCiUnitTest`, `verifyCiLint` on `:domain:lintCi`), its lockfile is `domain/gradle.lockfile`, and the ratchet tests and the `runBlocking` allow-list cover its files under the same package-relative paths as the app's.

The module exists for the build: the app's lint and tests no longer cover these lines, and a change elsewhere leaves this module's compile, lint and test results in the build cache. What does not belong here: anything with an inbound dependency from `data/`, `healthconnect/` or `features/` (`domain/query` and `domain/usecase` stay in `:app` for that reason), resources, and Hilt wiring. `ArchitectureDocTest` fails when a Gradle module is missing from this document.

## Phone-To-Phone Sync

[`features/devicesync`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync) copies Health Connect records between two phones over Bluetooth Classic RFCOMM. It is a feature because it talks to another instance of this app rather than to a peripheral.

Three sub-packages, in dependency order:

- `bluetooth/` — the only Android Bluetooth code. `BluetoothSyncManager` (`@Singleton`) owns discoverability, discovery, the socket, and the transport; `RfcommServer`/`RfcommClient` open the socket on a private app UUID; `RfcommByteChannel` pumps bytes; `BluetoothDiscoveryReceiver` bridges the discovery broadcasts.
- `protocol/` — pure Kotlin over a `SyncByteTransport` seam, so the whole protocol is testable over an in-memory pipe with no Bluetooth. `SyncFrame` is a length-prefixed frame whose `SyncFrameType` ordinal is the wire byte, so the enum is append-only. `SyncMessages` carries `SYNC_PROTOCOL_VERSION` and compact JSON payloads with a gzipped record batch. `SyncPairing` holds the cryptography: an ECDH P-256 exchange with a committed host key, HKDF, six digits the two users compare, and `SyncFrameCipher` (AES-256-GCM, one key per direction, a frame counter as nonce). `SyncSession` is the state machine: handshake, key exchange, code check, negotiate types, then a symmetric bidirectional exchange where both phones run the same code and differ only by `SyncRole`. No record frame is accepted until both users confirmed the code, and every frame after the key exchange except an abort is sealed. Never go back to a typed code proved with a MAC: six digits can be guessed offline from one recorded handshake.
- `store/` — the data side, kept out of the protocol. `SyncRecordCodec` encodes and decodes records and derives the content fingerprint used for dedup; `HealthConnectSyncStore` implements the protocol's `SyncRecordStore` for Health Connect records; `CycleJournalSyncStore` carries the cycle journal from Room; `MedicalRecordsSyncStore` carries FHIR medical records as export entries and writes them through the medical import pipeline; `CompositeSyncStore` routes each record type to one of them; `DeviceSyncReportStore` writes the last report as plain text under `filesDir/device_sync/`.

There is no Room table and no DataStore here. What sync persists is Health Connect itself, plus that one report file.

`DeviceSyncForegroundService` is an inert keep-alive: the RFCOMM pumps run in the wizard's ViewModel, and the service exists only to hold the foreground slot for the duration of a transfer. `start()` is best-effort and `stop()` only ever stops its own class, so a sync that never got the slot cannot tear down someone else's service.

## Screen Families

### Dashboard

The dashboard is intentionally different from the period-based detail screens.

It is:

- a daily snapshot
- navigated by day only
- powered by one aggregated `DashboardData` object
- the main entry point into feature screens

Current files:

- [`features/dashboard/DashboardViewModel.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardViewModel.kt)
- [`features/dashboard/DashboardScreen.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/dashboard/DashboardScreen.kt)

Shared pieces it uses:

- `PullToRefreshBox`
- `DayNavigator`
- `HealthDatePickerDialog`
- `MetricCard`
- `PermissionCallout`

The dashboard should stay summary-first. It should not become a second copy of detail-screen logic.

Dashboard metric cards route to metric-specific detail destinations. Metrics that share a repository can still reuse the same feature package and ViewModel, but navigation should call concrete metric screen entry points such as `ProteinScreen` or `RestingHeartRateScreen`, not a public screen with a metric parameter. The rendered detail view should focus on the selected metric instead of showing every related metric in one grouped screen. There is no global records browser or fixed dashboard browse action; entry and session lists belong behind the relevant metric card/detail screen.

### Manual entry

Manual entry is a separate screen family from the dashboard. It is the only app area that should initiate *user-entered* Health Connect writes. The Add entry picker is reached through contextual create actions on the dashboard and supported metric screens, not as a primary browsing destination.

The other write paths are all imports or transfers rather than typed entry: `features/imports/applehealth`, `features/imports/csv`, `features/imports/garmin` (the HRV of a FIT wellness file) and `features/devicesync`. They share one write door, `AppleHealthImportRepository.insertImportedRecords`, so deterministic `clientRecordId` upserts behave identically across them. The workouts of the app's own CSV export are the one import that takes another door: `features/imports/csv/CsvWorkoutWriter.kt` writes them through `ActivityRepository.writeActivityEntries`, as the route and FIT bulk import do, so they land as the app's own activities with the session's totals and series beside them. Medical records (FHIR) are the exception. They have no `clientRecordId`, so they are written through `healthconnect/MedicalRecordsWriter`, where a record's data source, type and id make a rewrite an update.

Current files:

- [`features/manualentry/ManualEntryScreen.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryScreen.kt)
- [`features/manualentry/ManualEntryViewModel.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryViewModel.kt)
- [`features/manualentry/activity`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity)
- [`features/manualentry/activity/recording`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording)
- [`features/manualentry/activity/routeimport`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport)
- [`features/manualentry/hydration`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/hydration)
- [`features/manualentry/food`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/food)
- [`features/manualentry/nutrition`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/nutrition)
- [`features/manualentry/body`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/body)
- [`features/manualentry/vitals`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/vitals)
- [`features/manualentry/mindfulness`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/mindfulness)

The current manual entry widgets cover hydration, food from a catalog the user builds, carbohydrate totals, activity sessions with manual entry, existing plans, or GPS recording, activity file review launched from Settings Data Importers for GPX/KML/KMZ, TCX, and FIT files, mindfulness, body composition, height, blood pressure, SpO2, respiratory rate, body temperature, HRV, blood glucose, and VO2 max. Body composition is one form for what a body analysis scale shows: weight, body fat, lean mass, body water, bone mass, and BMR. The filled values are written in one `insertRecords` call with one time, so a weigh-in is saved whole or not at all; body water may be typed as a percentage, which becomes a mass of the weight on the same form, never of an older one. Each value stays its own record afterwards, so the Body screens edit and delete it alone, through the same form showing that one field. The daily BMR estimate is written by its own pass and is not offered for editing. Blood glucose is typed in mmol/L or mg/dL, starting from the unit setting, and carries Health Connect's relation to meal, meal, and sample; VO2 max carries how it was measured. Health Connect's constants for those fields are mirrored in `GlucoseRecordValues` and `Vo2MaxRecordValues`, and `EntryWritePathsTest` checks the mirrors against the library. Every vitals type maps to and from its record in `healthconnect/VitalsRecordMapping.kt`. A blood pressure reading can carry the pulse the monitor showed: it is written as a one-sample `HeartRateRecord` at the reading's time whose `clientRecordId` derives from the reading's with its tokens stripped (`bpPulseClientRecordId`), so an edit of the reading rewrites it, clearing the field deletes it, and deleting the reading deletes it too. The value is also encoded on the reading's own `clientRecordId` as a `_bppulse-<bpm>` token beside the `_bpctx-` meal context, which is what lets a list of readings and the edit form show it without a heart rate read. Saving a pulse needs heart rate write access on top of blood pressure; a reading without one never asks for it. Widget order is customizable in the same spirit as the dashboard, but the dashboard remains read-only.

Write permissions can be requested during one-tap onboarding or lazily from Add entry and the specific metric entry route. The dashboard remains read-only. Each write goes directly to Health Connect; OpenVitals keeps only local UI preferences such as widget order and mindfulness timer/background-sound settings.

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
- vitals

They all use [`MetricDetailScaffold`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricDetailScaffold.kt) as the shared shell.

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

- [`features/onboarding`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/onboarding)
- [`features/settings`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/settings)

For availability and permission state these screens should keep using `HealthRepository`, not feature repositories.

Settings is one route per section, and each section builds only its own ViewModel: `DisplaySettingsViewModel`, `ActivitiesSettingsViewModel` (recording, offline maps, elevation tiles), `NutritionSettingsViewModel`, `BodySettingsViewModel` (shared by Body profile and Recovery, because the profile, the zones and the night window all feed the Body Energy chain), `DataImportViewModel`, and the bespoke Sync with another phone screen. `SettingsViewModel` keeps only the root, Health Connect, Vitals and Diagnostics: availability, permissions, and a few switches. A new section gets its own ViewModel and screen; do not grow `SettingsViewModel`.

### Health Connect screen shell

Health Connect-backed screens (dashboard, metric detail, readiness, manual entry, imports) should wrap content with the shared shell:

- [`HealthConnectFeature`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectFeature.kt) maps destinations to permission sets
- [`HealthConnectScreenUxCoordinator`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectScreenUxCoordinator.kt) loads sync/access/contextual-prompt state
- [`WithHealthConnectFeatureScreen`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/HealthConnectPermissionLauncher.kt) composes access gate, sync banner, contextual promotion, and permission launcher

Metric detail screens pass `syncPaused` from the shell state into `MetricDetailScaffold` and set `showInlineSyncBanner = false` to avoid duplicate banners.

## Cross-Cutting Rules

These five rules hold app-wide. Breaking one is not a local decision.

### 1. Exactly one foreground service at a time

The app treats the Android foreground slot as effectively single. Activity recording, the Apple Health import, and a phone-to-phone sync contend for it, and the app does not run them concurrently:

- [`ActivityRecordingService`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingService.kt) — `location|health|connectedDevice`.
- [`DeviceSyncForegroundService`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync/DeviceSyncForegroundService.kt) — `connectedDevice`.
- WorkManager's `SystemForegroundService` — `dataSync`, used by the Apple Health and offline map import workers.

The contention is resolved by refusing, not by queueing: the sync wizard reports `RECORDING_ACTIVE` while a recording is live. A new long-running workflow must either reuse one of these or state which one it excludes. No service starts without a tap: the scheduled workers (the home widget refresh) are plain `CoroutineWorker`s, never expedited or foreground ones, because work the user did not start must not be able to take the single slot, and it has no business posting a notification about itself.

Live BLE sensor streaming during activity recording lives in `sensors/ble`; it runs while the recording holds the slot and takes none of its own.

### 2. Health Connect reads and record mapping stay behind `healthconnect/*HealthReader`

The per-area readers in [`healthconnect`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect) — `ActivityHealthReader`, `SleepHealthReader`, `HeartHealthReader`, `BodyHealthReader`, `VitalsHealthReader`, `HydrationHealthReader`, `NutritionHealthReader`, `MindfulnessHealthReader`, `CycleHealthReader`, `MedicalRecordsHealthReader` — own the record types, the reads, and the mapping into app models. The medical reader and `MedicalRecordsWriter` reach Health Connect through `MedicalRecordsClient`, because the Jetpack medical classes build platform objects in their constructors and a JVM test cannot create them. `HealthConnectMedicalRecordsClient` is the only class that touches them. Repositories consume readers; features consume repositories. No feature should call the AndroidX client or hold a raw `Record` in screen state.

Two bounded exceptions exist today and should stay bounded:

- **Constant vocabularies.** Display code may reference Health Connect's constant sets where the app has no reason to mirror them — `ExerciseSessionRecord` exercise types, `ExerciseSegment`, `MealType`, `SexualActivityRecord` protection values. That is naming, not data access.
- **Write and import paths.** Importers and sync legitimately build `Record` instances. Each concentrates that in one place (`features/imports/applehealth`, `features/imports/csv`, `features/imports/garmin`, `features/devicesync/store/SyncRecordCodec.kt`) and writes through `AppleHealthImportRepository.insertImportedRecords`, which is what makes deterministic `clientRecordId` upserts consistent across all of them. Medical records write through `MedicalRecordsWriter` instead, and the CSV importer's workout preset builds `ActivityWriteRequest`s for `ActivityRepository.writeActivityEntries` rather than records.

**A writer reads strictly.** The readers return a fallback (an empty list, a null) when a read fails, is rate limited, or sync is paused. That suits a screen, which wants an empty state. It is wrong for code that saves what it read: a cache rebuild, a derived-record reconcile. Such code wraps its reads in `withStrictHealthConnectReads { ... }` ([`HealthConnectReaderSupport.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectReaderSupport.kt)). Inside it every guarded read throws instead, in child coroutines too, so the pass aborts before it writes. `VitalsHistorySyncService`, `StepDistanceBackfillService`, `BmrEstimateService` and `BodyEnergyChainSyncService` do this. A new writer must too. So does code that puts what it read into a document the user keeps: `ReportDataLoader` reads strictly, so a rate-limited metric shows as failed in the report and not as "No data in this range". `MedicalRecordsHealthReader` has no fallback at all: every read throws, so a medical screen never shows "no records" for a failed read.

**A refusal is one state for the whole process.** Health Connect's quota is per app and per window, and the standalone Health Connect app (Android 13) is stricter than the platform module. `HealthConnectRateLimitBackoff` ([`HealthConnectRateLimit.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectRateLimit.kt)) holds one deadline for every reader: it climbs 1, 2, 5, 15 minutes when hits keep coming inside half an hour, a hit from a read that was already out when the last one landed is the same burst and moves nothing, and the deadline is a `StateFlow` the dashboard banner counts down from. `HealthConnectReaderSupport` reads the backoff *under* its two-permit semaphore, so a read queued behind a refused one returns its fallback without a call; it also spaces call starts with `HealthConnectCallPacer` (150 ms on the standalone provider, 250 ms on any provider after the first refusal) and tallies every call by operation name in `HealthConnectCallCounters`, which the report email and the diagnostics export print. A screen read marks `HealthConnectReadOutcome` when it is skipped or refused, the way strict reads are marked; the dashboard keeps the tiles whose pass came back degraded, holds them in place rather than sorting them as empty, and `DashboardRateLimitRecovery` reloads them alone once the deadline passes. Below Android 14 the granted set and the feature status are calls into the Health Connect app too: `HealthConnectPermissionService` keeps a good answer for thirty seconds, drops it when a permission request returns or the app comes back to the foreground, and never caches a failure. `HistorySyncScheduler` defers the drains after a load that had reads refused, and `BodyEnergyChainSyncService` walks three days a pass and sits out a refusal. Nothing else may invent a second backoff.

**A writer checks that it can see other apps' data.** In the background, without the background-read grant, Health Connect answers a read with this app's own records only and raises nothing. `HealthConnectManager.readsOtherAppsDataNow()` is the test: a started Activity, or the grant. Code that saves or shows what it read while no Activity is up (a cache rebuild, a reconcile, a widget refresh) calls it first and skips the pass when it is false. A reminder that only needs this app's own entries does not need it.

### 3. A missing permission is a type, not a message

[`Throwable.isPermissionFailure()`](../../app/src/main/kotlin/tech/mmarca/openvitals/core/presentation/ScreenError.kt) is the single predicate for "this failed because a permission is missing". It walks the cause chain looking for `SecurityException`, because Health Connect throws that for an ungranted read or write and repositories throw the same type when they short-circuit a call whose permission they know is missing, often wrapped by a repository or worker.

`ScreenErrorHandler.handle` checks that predicate first and returns `ScreenError.PermissionDenied` before it ever considers `throwable.message`. That matters because the screens turn this case into a grant affordance: `ScreenErrorContent` and `MetricDetailScaffold` render `PermissionDenied` as `HealthConnectPermissionDeniedCallout` rather than red error text. Collapsing it into `ScreenError.Message` would silently downgrade a recoverable state into a dead end.

`AppleHealthImportErrorFormatter.isPermissionDenied` delegates to the same predicate so the import card and the screen error path cannot drift apart. Reuse it; do not pattern-match on exception messages.

### 4. Nothing waits on the main thread

A blocked main thread is an "app isn't responding" dialog. A broadcast receiver has about 10 seconds; a touch has 5.

- **Loads start and publish on Main.** `LoadCoordinator.launch(viewModelScope)` runs there, and `_uiState.value = _uiState.value.copy(...)` is only safe there. Move the heavy part with `withContext`, not the whole load.
- **Health Connect and Room calls are `suspend` and run on `dispatchers.io`.** Mappers that walk samples run on `dispatchers.default`. Inject [`DispatcherProvider`](../../app/src/main/kotlin/tech/mmarca/openvitals/core/performance/DispatcherProvider.kt) with the `= DefaultDispatcherProvider` default so tests can swap it.
- **No `runBlocking` in `app/src/main`.** `NoRunBlockingRatchetTest` holds the allow-list.
- **A receiver never holds a broadcast for a Health Connect read.** `UpdatingHomeWidgetReceiver` lets Glance redraw from stored state and hands the read to `HomeWidgetRefreshScheduler.refreshNow()`. Do not call `goAsync()` in a Glance receiver: Glance already took the pending result, so a second call returns null.
- **A widget tap writes first.** `runQuickBeverageTap` does the write, then the tile update. The label revert runs on `HomeWidgetScope`, after the broadcast ends. Its time budget stops the wait, never the write.
- **Composables `remember` any pass over samples.** A day of 1 Hz heart rate is about 86,000 samples. Sort, min, max and average belong in `remember(samples)` or in the mapper.

When the dialog does appear, Android keeps the trace. `AnrExitInfo` reads it into the report email and the debug log export, with the system's reason masked. On a device: `adb shell dumpsys activity exit-info tech.mmarca.openvitals`.

### 5. Text that leaves the app is structure, not data

Anything the user can copy, save, share or email holds structure only: counts, record types, units, reasons, row and column numbers, class names and stack frames. Never a record's value or time, a name, a file name or location, an address or an identifier. A report the user posts on a public issue must be safe to post.

- **Reports print structure from the start.** The Apple Health, CSV and medical import reports and the sync report never hand a value to a cleaner: a refused record is its type and reason, a cell that could not be read is its shape (`PrivacyRedactor.shapeOf`), a zip's files are their place and extension. A pattern cannot recognise a name, so nothing relies on one to remove it.
- **Error text goes through one redactor.** `core/privacy/PrivacyRedactor` (in `:domain`) masks an exception message: addresses, identifiers, file locations, dates and every number but a position ("line 3", "row 7"). `maskThrowable` rebuilds a stack trace from the throwable, so a message's later lines never appear. Use `describe`/`maskThrowable` wherever an exception's text reaches a report, the screen's copy button or a draft.
- **A crash draft carries the masked log.** The saved crash report holds the masked crash and the masked log of the crashed process, is attached for seven days at most, and is deleted once it has gone into an email, a copy or a file. The "Report an issue" draft adds the current process's masked log. The log, there and in the diagnostics log export (debug and nightly builds), keeps every line and tag and masks each through `maskLogcat`; a release build has no log to give and says so.
- **A log line never splices in a value.** Log a count, a class name, or pass the throwable as the last argument; the export masks a throwable's message whole. `LogMessageInterpolationRatchetTest` holds the log calls that splice in an exception message, and its count may only fall.
- **A copy is marked sensitive.** Every clipboard write of a report goes through `sensitiveTextClip` (`core/presentation/SensitiveClipboard.kt`), so Android 13 and later keep it out of the copy preview.
- **The diagnostic files stay on the phone.** `res/xml/data_extraction_rules.xml` leaves `crash_reports/`, `import_reports/` and `device_sync/` out of a transfer to a new phone; `LocalAppManifestPolicyTest` checks it.

Each report and the crash draft have a test that seeds `PrivacySentinels` (the `:domain` test fixtures) and asserts none comes out.

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

When registering a new period-based screen, add a `PeriodRangePreferenceKey` and inject `PeriodPreferences` into the screen ViewModel so the saved range is owned with the rest of the feature state. Persist only range changes; selected dates remain screen state.

### 5. Keep visuals local to the feature

If the feature needs a custom chart, row, or timeline, keep it in the feature package unless another feature genuinely needs the same thing.

## Repository Rules For New Work

### Use `HealthRepository` only for app-level concerns

Keep using `HealthRepository` for:

- availability
- permission contract access
- granted/missing permissions

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
- `CaffeineRepository`
- `BodyEnergyRepository`
- `VitalsRepository`

Each repository should:

- guard required permissions
- call `HealthConnectManager`
- return app models ready for the ViewModel

Not every repository is a Health Connect facade. `CycleJournalRepository` holds the subjective cycle journal because Health Connect has no type for it, and `BleDeviceRepository`, `PreferencesRepository` and `CyclePreferencesRepository` own app-local device and preference state. Those are the exception. If a new repository is not backed by Health Connect, say in its KDoc why Health Connect cannot own the data.

### Ask for the preferences you use, not the repository

`PreferencesRepository` holds every setting in the app and needs a `Context`, so a ViewModel that injects it cannot be built in a JVM test. Narrow contracts in `data/repository/contract` carve it into the groups screens actually use — `PeriodPreferences`, `DailyGoalPreferences`, `BodyProfilePreferences`, `CalorieDisplayPreferences`, `SleepWindowPreferences`, `NutritionDisplayPreferences`, `HeartThresholdPreferences`, `HydrationGoalPreferences`, `BodyEnergyCalibrationPreferences`, `CaffeineModelPreferences`, `MindfulnessTimerPreferences`, `ActivitySplitPreferences`, `WidgetOrderPreferences`, `RecordingPreferences`, `UnitPreferences`, `OnboardingPreferences`, `HealthConnectPreferences` — and `PreferencesModule` binds each to the repository. `CyclePreferences` is bound to `CyclePreferencesRepository`, a separate store in its own preferences file, because the main repository is at its size ceiling. The Settings screens and the dashboard read too wide a set to fit one, and take the repository. The reminder controllers need a `Context` for the same reason; `HydrationReminderSettings`, `MindfulnessReminderSettings` and `CycleReminderSettings` are the three-method slice a screen uses, bound in `RemindersModule`.

Inject the contract. The ViewModel then needs one constructor, and its test passes `FakePreferences` instead of mocking a hundred members. A route argument is read from the injected `SavedStateHandle` in that same constructor; a test builds the handle with the real argument, so `activityMetricFromRoute` and its `routeId()` inverse are the contract. Add a contract for a new group rather than widening an existing one.

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

For example, [`MetricCard.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/MetricCard.kt) currently contains:

- `MetricCard`
- `MetricCardPlaceholder`
- `SourceChip`

`SectionHeader` and `TimeRangeSelector` have since moved out into their own files, which is the direction the rest should follow if shared UI keeps growing.

### 4. Background work is narrow and explicit

Room-backed caching is intentionally narrow: the vitals daily cache, the heart-rate day averages and the beverage catalog, not raw Health Connect records.
The dashboard and daily readiness read Health Connect on every load.
The sync table (`synced_record_origins`) exists only because Health Connect has no record type for a record's source phone; it is not a precedent for mirroring records Health Connect can already hold.

WorkManager is used for the Apple Health import worker and the offline map import worker because those workflows can be long-running and user-visible.
It is also used for one scheduled job, the home widget refresh.

A phone-to-phone sync holds its own coroutine scope, is foreground and user-initiated, and has no background variant. Do not read any of this as a general background-sync layer, and do not design new features as if one or a raw-record database already exists.

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
- device protocol code stays transport-free and testable without a radio
- a new device integration adds a port implementation, not a second radio-arbitration scheme
