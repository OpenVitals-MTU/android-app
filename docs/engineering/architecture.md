# Architecture

## Purpose

This document describes the architecture of OpenVitals as it exists today, plus the direction new work should follow.

The repo now has one Android app module for the local app. The goal is to keep boundaries clear enough that new metrics can be added without copying screen scaffolding, period math, or Health Connect plumbing everywhere, while keeping code app-local until there is a concrete need for a new module.

## Current Snapshot

- App namespace: `tech.mmarca.openvitals`
- Project shape: one local Android app module under `app/`
- Dependency wiring: Hilt in the single `:app` module, rooted at [`OpenVitalsApp`](../../app/src/main/kotlin/tech/mmarca/openvitals/OpenVitalsApp.kt); modules are `di/AppModule.kt`, `di/RepositoryModule.kt`, and `di/DevicesModule.kt`
- UI stack: Jetpack Compose + Material 3 app shell + Navigation Compose + `ViewModel` + coroutines/`StateFlow`
- Health data backend: Health Connect AndroidX client, wrapped by [`HealthConnectManager`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectManager.kt)
- App-local domain code: pure models, insight calculations, and preference enums under [`domain`](../../app/src/main/kotlin/tech/mmarca/openvitals/domain)
- Shared period shell: in place and used by all metric detail/list screens
- Feature repositories: in place for activity, sleep, heart, body, body energy, caffeine, hydration, nutrition, mindfulness, cycle, and vitals
- Dashboard: still a dedicated day-based summary screen, not a period-detail screen
- Manual entry: separate from the dashboard and writes explicit user-entered records directly to Health Connect
- Room is at schema version 6. It holds derived summary caches plus the one table Health Connect cannot represent (`garmin_wellness_samples`); Health Connect remains the source of truth for everything it has a record type for
- WorkManager is used for user-started Apple Health imports, offline map imports, and lightweight metric summary warmup
- Device integration lives under [`devices`](../../app/src/main/kotlin/tech/mmarca/openvitals/devices): the Garmin GFDI protocol stack, the shared BLE radio lease, companion-device pairing, and notification forwarding
- Phone-to-phone Health Connect sync lives under [`features/devicesync`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync) and runs over Bluetooth Classic RFCOMM
- A one-time Flutter-to-Kotlin data migrator lives under [`data/migration`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/migration) and runs from `OpenVitalsApp.onCreate()`

Most importantly, body and entry/session browsing now live in metric-owned detail screens. The former global Browse destination is no longer part of the app architecture.

### Top-Level Package Map

| Package | Owns |
|---|---|
| `core/` | app-wide primitives: `period`, `presentation`, `stats`, `geo`, `fit`, `performance`, `diagnostics` |
| `data/` | `local` (Room), `repository` (feature-facing repositories + `contract` interfaces), `sync` (history/backfill services), `migration` (one-time Flutter import) |
| `devices/` | device integration: `core` (ports, radio lease, pairing), `garmin` (GFDI stack), `wearos`, `notifications` (notification listener) |
| `di/` | `AppModule`, `RepositoryModule`, `DevicesModule` |
| `domain/` | pure models, insights, preferences, queries, use cases |
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

Derived summaries may be cached in Room when a screen otherwise repeats expensive Health Connect reads or calculations.
The cache stores versioned UI/repository result envelopes and must be invalidated by permission fingerprint,
calculation config, and schema version.

### 7. Keep module boundaries proportional

The current project should stay single-module unless a future app or library has a concrete, active need for extracted code. Prefer package boundaries first:

- app-local domain models and calculations in `domain`
- period primitives in `core/period`
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
- feature-oriented model files under [`domain/model`](../../app/src/main/kotlin/tech/mmarca/openvitals/domain/model)

Current boundary shape:

- `HealthConnectManager` is the low-level integration wrapper. It talks to the AndroidX client, performs reads, writes explicit manual entries, and maps results into app models.
- `HealthRepository` is now intentionally narrow: Health Connect availability, permission state, and dashboard aggregation.
- Feature repositories are thin, permission-aware facades over `HealthConnectManager`.
- Manual entry ViewModels use the same feature repositories for writes, so write permission and write behavior stay below the UI route.
- `healthconnect` depends on app-local domain models, not on `data.repository`; repositories depend on `healthconnect` and `domain`.

This is a meaningful improvement over the earlier centralized repository approach. New feature reads should follow the feature-repository pattern, not expand `HealthRepository`.

Some repositories are now split into a `data/repository/contract/` interface and an implementation, bound in `di/RepositoryModule.kt`. Use that split when a repository is a seam another subsystem writes through, not by default.

### Local storage

[`OpenVitalsDatabase`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/local/OpenVitalsDatabase.kt) is at `version = 6`, `exportSchema = false`, with migrations declared in its companion object.

| Table | Package | Purpose |
|---|---|---|
| `beverages` | `data/local/beverage` | user and preloaded beverage catalog |
| `vitals_daily_aggregates`, `vitals_sync_cursors` | `data/local/vitalscache` | derived daily summary cache and change-token cursors |
| `body_energy_days`, `body_energy_buckets` | `data/local/bodyenergy` | the Body Energy chain, moved off SharedPreferences in migration 4 → 5 |
| `garmin_wellness_samples` | `data/local/garmin` | watch-only wellness series, added in migration 5 → 6 |

`garmin_wellness_samples` is the one table that is not a cache. It is the system of record for the series a Garmin watch produces that Health Connect has no record type for (stress, Body Battery, watch sleep scores). Its schema is `(metric, time_millis, value)` with `(metric, time_millis)` as the primary key, so re-syncing an overlapping window rewrites rows instead of duplicating them.

Access goes through [`GarminWellnessRepository`](../../app/src/main/kotlin/tech/mmarca/openvitals/data/repository/contract/GarminWellnessRepository.kt), which is deliberately a thin seam: `upsert`, `samplesBetween`, `latest`, `countFor` and nothing else. No windowing, aggregation, or interpretation happens there. It exists so the Garmin sync pipeline (which writes after decoding FIT wellness files) and the readers that chart or derive from these series never reach for the DAO directly. Keep it that way; interpretation belongs in `domain` or the feature.

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
- [`features/watches`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/watches)

Three of these are not metric features and follow their own shape: `features/watches` is the watch UI over the `devices` layer, `features/devicesync` is a phone-to-phone sync wizard, and `features/imports/*` are import workflows.

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

[`core/fit/FitDecoder.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/core/fit/FitDecoder.kt) is the one FIT decoder in the app. It was extracted from the route importer's private decoder once a second consumer appeared.

It is a generic container walk and nothing more: it reads the FIT header, definition and data messages, compressed timestamps and developer fields, and returns `FitMessage` values keyed by global message number. It has no message allowlist and no semantics.

Interpretation lives with the consumer, and the two interpreters are disjoint:

- [`features/manualentry/activity/routeimport/FitRouteParser.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/FitRouteParser.kt) interprets activity, course, and workout files.
- [`devices/garmin/wellness/GarminFitWellness.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/devices/garmin/wellness/GarminFitWellness.kt) interprets the Garmin-proprietary wellness messages a watch sync downloads.

An activity file therefore yields empty wellness carriers and a wellness file yields no route, without either side needing to know about the other. A third FIT consumer should follow the same split: reuse `FitDecoder`, own its interpretation.

## Device Layer

`devices/` is a layer, not a feature. It owns everything that talks to a physical device over Bluetooth and exposes ports that features consume. It has no Compose code and no navigation. The user-facing screens live in `features/watches`.

### `devices/core`

Vendor-neutral ports plus the two things every integration shares.

- [`RadioLease.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/devices/core/RadioLease.kt) — `object RadioLeases`, a process-wide lease keyed **per Bluetooth address**. Two holders on different peripherals is allowed by design; two holders on the same peripheral is not.
- [`RadioLeaseUse.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/devices/core/RadioLeaseUse.kt) — `withRadioLease(address, owner) { ... }`, `RadioLeaseBusyException`, and `object RadioLeaseOwner` with the four owner tags: `SYNC`, `FIND`, `SETTINGS`, `NOTIFICATIONS`. They are strings rather than an enum because they appear in logcat.
- `DeviceClassification.kt` / `DeviceScanClassifier.kt` — `DeviceClassifier` and `DeviceScanClassifier` function interfaces; the first non-null classification wins, otherwise the device is a plain sensor.
- `core/pairing/` — `WatchPairingPort` (bond, unbond, associate, disassociate), its `BleWatchPairing` implementation, `CompanionDevicePairing` around `CompanionDeviceManager`, and `OpenVitalsCompanionDeviceService`.
- `core/sync/DeviceSyncPort.kt` — `canSync(device)` plus a progress-reporting `sync(...)`, so the watch UI never names a vendor.

### `devices/garmin`

The GFDI protocol stack, bottom to top. Everything above the GATT client is transport-free and unit-testable:

| Layer | Files |
|---|---|
| Byte primitives | `GarminByteReader/Writer`, `GarminCrc`, `GarminCobs`, `GarminProtobuf`, `GarminTime`, `GarminLog` |
| BLE transport | `GarminUuids`, `GarminGattClient` (the only file touching `android.bluetooth`), `GarminGattProbe`, `GarminTransport`, `GarminTransportProbe`, `GarminMlTransport` |
| Framing | `GarminGfdiFrame` |
| Message vocabulary | `GarminMessages`, `GarminCapabilities` |
| Session | `GarminSession`, `GarminProtobufTransport` |
| File sync | `GarminWatchSyncService`, `GarminDirectory`, `GarminFileTypes`, `GarminFileStore`, `GarminDeviceStateStore`, `GarminCounterWatermarkStore`, `GarminActivityImporter` |
| Wellness import | `wellness/GarminFitWellness` (decode), `wellness/FitWellnessImport` (mapping), `wellness/FitWellnessImporter` (orchestration) |
| Notifications | `GarminNotificationBridge`, `GarminNotificationForwarder`, `GarminNotificationLink`, `GarminGncsHandler`, `GarminNotificationMessages`, `GarminNotificationActions` |
| Settings link | `GarminSettingsLink`, `GarminSettingsService`, `GarminSettingsModel` |
| Onboarding | `OnboardGarminWatchUseCase`, `GarminDeviceClassifier`, `GarminDeviceNames`, `GarminPhoneIdentity` |

Notes worth carrying:

- `GarminWatchSyncService` is a `@Singleton` class, not an Android `Service`, despite the name. It implements `DeviceSyncPort`.
- A watch sync writes Health Connect records through `AppleHealthImportRepository.insertImportedRecords`, the same deterministic-`clientRecordId` path the Apple Health importer uses, so a re-import upserts instead of duplicating. Only the watch-only series go to `GarminWellnessRepository`.
- The OS bond is the security boundary. GFDI's own auth challenge is answered with zeroes, so `OnboardGarminWatchUseCase` treats bonding as mandatory and the companion association as optional.

### `devices/wearos`

Classification and onboarding only. There is no protocol: `OnboardWearOsWatchUseCase` has a single `ASSOCIATING` step, and the device is registered with no capabilities.

### `devices/notifications`

`OpenVitalsNotificationListenerService` is the platform `NotificationListenerService`. It reads posted notifications, filters them through the pure `NotificationFilter`, buffers them in the memory-only `NotificationStore`, and hands them to `GarminNotificationBridge`. It touches no Bluetooth; the bridge owns the forwarding. Nothing is written to a file or a database.

### Hilt wiring

`di/DevicesModule.kt` is the only module for this layer. It binds `BleWatchPairing` to `WatchPairingPort` and `GarminWatchSyncService` to `DeviceSyncPort`, and provides the GATT probe and the two `SharedPreferences`-backed stores. Everything else is constructor injection. There are no `@Module` declarations inside `devices/` itself; keep it that way.

## Phone-To-Phone Sync

[`features/devicesync`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync) copies Health Connect records between two phones over Bluetooth Classic RFCOMM. It is a feature, not part of `devices/`, because it talks to another instance of this app rather than to a peripheral.

Three sub-packages, in dependency order:

- `bluetooth/` — the only Android Bluetooth code. `BluetoothSyncManager` (`@Singleton`) owns discoverability, discovery, the socket, and the transport; `RfcommServer`/`RfcommClient` open the socket on a private app UUID; `RfcommByteChannel` pumps bytes; `BluetoothDiscoveryReceiver` bridges the discovery broadcasts.
- `protocol/` — pure Kotlin over a `SyncByteTransport` seam, so the whole protocol is testable over an in-memory pipe with no Bluetooth. `SyncFrame` is a length-prefixed frame whose `SyncFrameType` ordinal is the wire byte, so the enum is append-only. `SyncMessages` carries `SYNC_PROTOCOL_VERSION` and compact JSON payloads with a gzipped record batch. `SyncPairing` derives the session key from the six-digit code plus both nonces. `SyncSession` is the state machine: handshake, authenticate, negotiate types, then a symmetric bidirectional exchange where both phones run the same code and differ only by `SyncRole`.
- `store/` — the Health Connect side, kept out of the protocol. `SyncRecordCodec` encodes and decodes records and derives the content fingerprint used for dedup; `HealthConnectSyncStore` implements the protocol's `SyncRecordStore`; `DeviceSyncReportStore` writes the last report as plain text under `filesDir/device_sync/`.

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

The other write paths are all imports or transfers rather than typed entry: `features/imports/applehealth`, `features/imports/csv`, `features/devicesync`, and the Garmin wellness import. They share one write door, `AppleHealthImportRepository.insertImportedRecords`, so deterministic `clientRecordId` upserts behave identically across them.

Current files:

- [`features/manualentry/ManualEntryScreen.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryScreen.kt)
- [`features/manualentry/ManualEntryViewModel.kt`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryViewModel.kt)
- [`features/manualentry/activity`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity)
- [`features/manualentry/activity/recording`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording)
- [`features/manualentry/activity/routeimport`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport)
- [`features/manualentry/hydration`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/hydration)
- [`features/manualentry/body`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/body)
- [`features/manualentry/vitals`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/vitals)
- [`features/manualentry/mindfulness`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/mindfulness)

The current manual entry widgets cover hydration, activity sessions with manual entry, existing plans, or GPS recording, activity file review launched from Settings Data Importers for GPX/KML/KMZ, TCX, and FIT files, mindfulness, weight, height, body fat, blood pressure, SpO2, respiratory rate, and body temperature. Widget order is customizable in the same spirit as the dashboard, but the dashboard remains read-only.

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

Settings has since grown past that: because it also hosts the import, offline map, and reminder workflows, `SettingsViewModel` legitimately injects a handful of feature repositories and import services alongside `HealthRepository`. Its section screens are still routed one section at a time, and bespoke sections such as Watches and Sync with another phone have their own ViewModels rather than growing this one. Prefer that split for anything new.

### Health Connect screen shell

Health Connect-backed screens (dashboard, metric detail, readiness, manual entry, imports) should wrap content with the shared shell:

- [`HealthConnectFeature`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectFeature.kt) maps destinations to permission sets
- [`HealthConnectScreenUxCoordinator`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/HealthConnectScreenUxCoordinator.kt) loads sync/access/contextual-prompt state
- [`WithHealthConnectFeatureScreen`](../../app/src/main/kotlin/tech/mmarca/openvitals/ui/components/HealthConnectPermissionLauncher.kt) composes access gate, sync banner, contextual promotion, and permission launcher

Metric detail screens pass `syncPaused` from the shell state into `MetricDetailScaffold` and set `showInlineSyncBanner = false` to avoid duplicate banners.

## Cross-Cutting Rules

These four rules hold app-wide. Breaking one is not a local decision.

### 1. Exactly one foreground service at a time

The app treats the Android foreground slot as effectively single. Activity recording, the Apple Health import, and a phone-to-phone sync contend for it, and the app does not run them concurrently:

- [`ActivityRecordingService`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingService.kt) — `location|health|connectedDevice`.
- [`DeviceSyncForegroundService`](../../app/src/main/kotlin/tech/mmarca/openvitals/features/devicesync/DeviceSyncForegroundService.kt) — `connectedDevice`.
- WorkManager's `SystemForegroundService` — `dataSync`, used by the Apple Health and offline map import workers.

The contention is resolved by refusing, not by queueing: the sync wizard reports `RECORDING_ACTIVE` and `GarminWatchSyncService.sync` refuses outright while a recording is live. A new long-running workflow must either reuse one of these or state which one it excludes. A Garmin watch sync deliberately runs with **no** foreground service of its own; its process-priority story is the companion-device association instead.

### 2. One BLE radio, leased per address

Every subsystem that opens a BLE link to a device goes through `RadioLeases`. The four owners are `SYNC`, `FIND`, `SETTINGS`, and `NOTIFICATIONS`, and on a given address they mutually exclude: a file sync, a find-my-watch ring, an open settings link, and the notification forwarder cannot hold the same watch at once.

The lease is not just a mutex. Three properties matter:

- Leases expire, so a crashed holder cannot wedge the radio permanently.
- `request()` registers a waiter; the current holder's next renew then fails, which is its cue to drop the link. That is how the indefinitely-held notification forwarder and settings link yield to a sync.
- `release()` leaves a short settle window rather than clearing the entry, so a new GATT open cannot race the previous teardown.

Live BLE sensor streaming during activity recording lives in `sensors/ble` and targets sensors, not watches; it is excluded from watch work by rule 1 rather than by the lease. New device work must take a lease, and must pick one of the existing owner tags rather than inventing a fifth without a reason.

### 3. Health Connect reads and record mapping stay behind `healthconnect/*HealthReader`

The per-area readers in [`healthconnect`](../../app/src/main/kotlin/tech/mmarca/openvitals/healthconnect) — `ActivityHealthReader`, `SleepHealthReader`, `HeartHealthReader`, `BodyHealthReader`, `VitalsHealthReader`, `HydrationHealthReader`, `NutritionHealthReader`, `MindfulnessHealthReader`, `CycleHealthReader` — own the record types, the reads, and the mapping into app models. Repositories consume readers; features consume repositories. No feature should call the AndroidX client or hold a raw `Record` in screen state.

Two bounded exceptions exist today and should stay bounded:

- **Constant vocabularies.** Display code may reference Health Connect's constant sets where the app has no reason to mirror them — `ExerciseSessionRecord` exercise types, `ExerciseSegment`, `MealType`, `SexualActivityRecord` protection values. That is naming, not data access.
- **Write and import paths.** Importers and sync legitimately build `Record` instances. Each concentrates that in one place (`features/imports/applehealth`, `features/imports/csv`, `features/imports/garmin`, `features/devicesync/store/SyncRecordCodec.kt`, `devices/garmin/wellness/FitWellnessImport.kt`) and writes through `AppleHealthImportRepository.insertImportedRecords`, which is what makes deterministic `clientRecordId` upserts consistent across all of them.

### 4. A missing permission is a type, not a message

[`Throwable.isPermissionFailure()`](../../app/src/main/kotlin/tech/mmarca/openvitals/core/presentation/ScreenError.kt) is the single predicate for "this failed because a permission is missing". It walks the cause chain looking for `SecurityException`, because Health Connect throws that for an ungranted read or write and repositories throw the same type when they short-circuit a call whose permission they know is missing, often wrapped by a repository or worker.

`ScreenErrorHandler.handle` checks that predicate first and returns `ScreenError.PermissionDenied` before it ever considers `throwable.message`. That matters because the screens turn this case into a grant affordance: `ScreenErrorContent` and `MetricDetailScaffold` render `PermissionDenied` as `HealthConnectPermissionDeniedCallout` rather than red error text. Collapsing it into `ScreenError.Message` would silently downgrade a recoverable state into a dead end.

`AppleHealthImportErrorFormatter.isPermissionDenied` delegates to the same predicate so the import card and the screen error path cannot drift apart. Reuse it; do not pattern-match on exception messages.

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
- `CaffeineRepository`
- `BodyEnergyRepository`
- `VitalsRepository`

Each repository should:

- guard required permissions
- call `HealthConnectManager`
- return app models ready for the ViewModel

Not every repository is a Health Connect facade. `GarminWellnessRepository` is a thin seam over a Room DAO for series Health Connect has no type for, and `BleDeviceRepository` and `PreferencesRepository` own app-local device and preference state. Those are the exception. If a new repository is not backed by Health Connect, say in its KDoc why Health Connect cannot own the data.

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

Room-backed caching is intentionally narrow: it stores derived summaries and the beverage catalog, not raw Health Connect records.
The first cached surface is dashboard-style daily summaries, which also powers daily readiness.
The one non-cache table, `garmin_wellness_samples`, exists only because Health Connect has no record type for those series; it is not a precedent for mirroring records Health Connect can already hold.

WorkManager is used for the Apple Health import worker and the offline map import worker because those workflows can be long-running and user-visible.
It is also used for small metric summary warmup jobs after app open. Long-running device work does **not** use WorkManager: a watch sync and a phone-to-phone sync are both foreground, user-initiated, and hold their own coroutine scope. Do not design new features as if a
general background-sync layer or raw-record database already exists.

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
