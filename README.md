<p align="center">
    <img width="160" alt="OpenVitals logo" src="docs/images/readme-logo.png">
</p>

# OpenVitals

<p align="center">
    <a href="https://liberapay.com/manuel.mmarca.tech/donate"><img alt="Liberapay receiving" src="https://img.shields.io/liberapay/receives/manuel.mmarca.tech.svg?logo=liberapay"></a>
    <a href="https://liberapay.com/manuel.mmarca.tech/donate"><img alt="Liberapay patrons" src="https://img.shields.io/liberapay/patrons/manuel.mmarca.tech.svg?logo=liberapay"></a>
</p>

Privacy-first Health Connect dashboard, activity tracker, and manual entry app for Android.

OpenVitals helps you review Health Connect data, record or import workouts, import supported Apple Health exports, and add supported manual entries without creating an account or sending health data to an OpenVitals server. The dashboard is read-only by default; writes happen only when you explicitly save or import records back to Health Connect.

## Install

| Channel | Link | Best for |
| --- | --- | --- |
| Google Play | [Install or join testing](https://play.google.com/store/apps/details?id=tech.mmarca.openvitals) |  |
| Codeberg releases | [Download signed release and debug APKs](https://codeberg.org/OpenVitals/android-app/releases) ||
| Source | [Codeberg](https://codeberg.org/OpenVitals/android-app) / [GitHub mirror](https://github.com/mmarca-tech/OpenVitals) | |

## Why OpenVitals

- No account, no ads, no analytics SDKs, no cloud health-data sync
- No app-level `INTERNET` permission in the merged app manifest
- Health Connect remains the source of truth
- Manual entries are written only after an explicit save action
- Sensitive cycle data is requested only as an explicit Health Connect permission category
- Open source under AGPL-3.0-or-later

## Highlights

- Summary dashboard for activity, recovery, beverages, nutrition, body, heart, vitals, mindfulness, and optional cycle data
- Period detail screens with `Day / Week / Month / Year` navigation and reorderable metric sections
- Daily Readiness with Body Energy, Training Readiness, physiological stress, HRV status, intensity minutes, adaptive goals, and local explanation screens
- Refreshed UI/UX with clearer Summary-first navigation, metric screens, and entry flows
- Health Connect permission onboarding with clear data categories and a one-tap full setup option
- Manual logging for beverages with hydration, caffeine, and nutrition defaults, carbohydrate entries, body measurements, vitals, mindfulness, and activities
- Home-screen widgets for key metrics and one-tap beverage logging
- Opt-in hydration reminders with active hours, daily-goal pause logic, and automatic hiding after saved hydration entries
- Achievement badges for activity, distance, floors, workouts, hydration, sleep, and mindfulness
- GPX/KML/KMZ route import, FIT activity/course/workout import, offline PMTiles/Mapsforge map packs, and GPS activity recording with review before saving
- Configurable activity recording dashboard with Focus mode, high-contrast outdoor mode, keep-screen-on support, strength training heart-rate monitoring, and experimental Bluetooth LE sensor integration
- App language support with an in-app language picker for system default, English, Spanish, German, Italian, and Estonian
- Apple Health export import for supported activity, heart, body, hydration, and vitals records, with background progress and chunked processing for large exports
- Health Connect 1.2.0-alpha04 coverage for newer activity records and recording permissions
- Wheelchair activity and wheelchair push tracking when Health Connect data is available
- Dedicated Calories detail screen with total, active, and BMR calorie context
- Body composition insights including Fat-Free Mass Index (FFMI) when weight, height, and body fat are available
- Activities and Sleep detail screens with integrated overview cards and direct metric links
- Metric and imperial unit support

## Help Improve It

OpenVitals is still early. Useful feedback is specific: device model, Android version, Health Connect provider version, which permissions were granted, and what screen or workflow failed.

- Try the latest beta from Google Play or Codeberg releases
- Report bugs and feature requests on [Codeberg issues](https://codeberg.org/OpenVitals/android-app/issues)
- Translate OpenVitals in your language on [Codeberg Translate](https://translate.codeberg.org/projects/openvitals/android-app/) — see [`docs/engineering/translations.md`](docs/engineering/translations.md)
- Ask questions and discuss support on [OpenVitals Zulip](http://openvitals.zulipchat.com/)
- Star or follow the project on [Codeberg](https://codeberg.org/OpenVitals/android-app) or the [GitHub mirror](https://github.com/mmarca-tech/OpenVitals)
- Share screenshots or notes from real Health Connect setups, especially route recording and manual entry flows
- Support ongoing development on [Liberapay](https://liberapay.com/manuel.mmarca.tech/donate)

## Screenshots

<div>
    <img width="23%" alt="OpenVitals dashboard" src="docs/images/readme-dashboard.png">
    <img width="23%" alt="OpenVitals onboarding" src="docs/images/onboarding.png">
    <img width="23%" alt="OpenVitals settings" src="docs/images/settings.png">
    <img width="23%" alt="Daily Readiness detail" src="docs/images/dailyReadiness.png">
    <img width="23%" alt="Body Energy detail" src="docs/images/bodyEnergy.png">
    <img width="23%" alt="Activity detail" src="docs/images/activityDetail.png">
    <img width="23%" alt="Activity recording" src="docs/images/activityRecording.png">
    <img width="23%" alt="Beverage entry" src="docs/images/beverageEntry.png">
</div>

## Features

- Summary dashboard with grouped sections for activity, recovery, beverages, nutrition, body, heart, vitals, mindfulness, and optional cycle data
- Refreshed Material 3 app shell with Settings and Achievements in the top bar plus dashboard quick actions for logging and starting activities
- Dedicated debug app variant with a separate application ID and diagnostics for troubleshooting
- Period-based detail screens with `Day / Week / Month / Year` navigation and reorderable metric sections
- Feature screens for Activity, Activities, Calories, Sleep, Heart & Vitals, Body, Beverages, Caffeine, Nutrition, Mindfulness, Cycle, Manual entry, Onboarding, and Settings
- Categorized Health Connect onboarding permissions, with one-tap full setup, category-by-category review, and cycle data grouped as an explicit sensitive category
- Write-permission requests available during one-tap setup or from Add entry and metric entry screens, while dashboard views stay read-only
- Daily Readiness, Body Energy, Training Readiness, and Stress Tracking screens with rule-based local explanations and confidence context
- Achievement screen with Fitbit-inspired badges and progress for daily steps, lifetime distance, floors, workouts, hydration, sleep, and mindfulness
- Home-screen widgets for steps, hydration, and a configurable metric, plus one-tap beverage-logging widgets
- Health Connect availability checks, including unsupported device/profile handling and provider-update messaging
- Feature-gated Mindfulness support when the installed Health Connect provider exposes `FEATURE_MINDFULNESS_SESSION`
- Data Importers setting for supported Apple Health `export.xml` or `export.zip` records and FIT activity/course/workout files
- Cycle tracking with its own dashboard section, period calendar, flow, ovulation, cervical mucus, and basal body temperature views after Health Connect cycle permissions are granted
- Metric/Imperial unit preference in Settings, backed by shared display formatters
- Shared detail-screen scaffold with pull-to-refresh, range selection, period navigation, and calendar date picking
- Explicit manual entry logging for beverages with hydration, caffeine, and nutrition defaults, carbohydrates, activities with optional GPX/KML/KMZ route import, FIT activity/course/workout review from Settings, offline PMTiles/Mapsforge maps, GPS recording, high-contrast outdoor recording, or experimental Bluetooth LE sensors, body measurements, vitals, and mindfulness sessions, written directly to Health Connect

## Current coverage

- Activity: steps, distance, total calories burned, optional total-calorie estimates, active calories, BMR context, floors climbed, elevation gain, wheelchair pushes, workout sessions, and cardio load
- Sleep: sessions, duration, sleep stages, sleep-stage time graphs, sleep score, sleep efficiency, and period overview cards
- Recovery: Daily Readiness, Body Energy, Training Readiness, HRV status, intensity minutes, physiological stress, adaptive goal context, and local explanation screens
- Heart: heart rate samples and summaries, resting heart rate, HRV
- Vitals: blood pressure, SpO2, respiratory rate, body temperature, VO2 max
- Body: weight, BMI, body fat, lean mass, Fat-Free Mass Index (FFMI), bone mass, body water mass, basal metabolic rate
- Manual entry: beverage/hydration entries with drink presets, caffeine, nutrition defaults, and custom amounts; carbohydrate entries; activity sessions with optional GPX/KML/KMZ route import, FIT activity/course/workout review from Settings, offline PMTiles/Mapsforge maps, GPS recording, configurable recording dashboard, Focus mode, high-contrast outdoor mode, strength training heart-rate monitoring, or experimental Bluetooth LE sensors; mindfulness sessions; weight; height; body fat; blood pressure; SpO2; respiratory rate; and body temperature
- Beverages and caffeine: daily and period hydration totals, active caffeine estimates, bedtime guidance, source and time-of-day insights, Health Connect-backed drink logging with preset or custom drinks, tap-to-save container presets, editable per-container serving sizes, and optional reminders
- Achievements: badge progress for activity, distance, floors, workouts, hydration, sleep, and mindfulness milestones
- Nutrition: calories in, meals, macros, caffeine, and selected nutrient totals from Health Connect nutrition records
- Mindfulness: session list and total duration when supported by Health Connect, plus timer-based and manual session logging with bell previews and optional looping background sounds
- Cycle tracking: period days, flow levels, ovulation tests, cervical mucus observations, and basal body temperature when Health Connect cycle permissions are granted
- Entry and session lists are reached from the relevant metric detail screen rather than a global records browser

## Privacy

- No account required
- No cloud sync of health data
- No ads
- No analytics SDKs
- No Google Play Services dependency for app functionality
- Permissions are requested by clear Health Connect categories:
  - Activity & sleep: required for the dashboard
  - Heart & recovery, Body, Activity extras, Nutrition & hydration, Mindfulness, and Vitals: optional
  - Cycle tracking: sensitive optional access, grouped separately so you can grant or skip it explicitly
  - Manual entry write access: available from one-tap onboarding or when you use Add entry or a metric entry screen that needs it
- Permissions can be managed later in Settings
- Health Connect remains the source of truth; OpenVitals does not store health records locally (a small local database caches derived summaries only)
- Imported Apple Health export records are written to Health Connect and are not uploaded to an OpenVitals service

The merged app manifest does not request the `INTERNET` permission.

Full privacy statement: [`PRIVACY.md`](PRIVACY.md).

## Platform requirements

- Android only today
- `minSdk 26`
- `compileSdk 37`
- `targetSdk 36`
- Health Connect required

The app is built with Flutter, and the iOS target in [`ios/`](ios) still builds, but no HealthKit bridge exists yet: on any platform other than Android the health backend resolves to `UnsupportedHealthDataSource` and every read returns empty. Android is the only shipping platform.

Health Connect platform notes:

- On Android 14 and newer, Health Connect is part of the system
- On Android 13 and older, the Health Connect app must be installed separately
- Health Connect is not supported in work profiles
- Mindfulness sessions require a Health Connect provider version that supports `FEATURE_MINDFULNESS_SESSION`
- The app uses `androidx.health.connect:connect-client` 1.2.0-alpha04 (via the in-repo [`packages/health_connect_native`](packages/health_connect_native) plugin) so AndroidX maps newer activity, mindfulness, and aggregation APIs to the current platform permissions

## Build from source

Requirements:

- Flutter SDK 3.44.x (Dart 3.12+). CI runs `ghcr.io/cirruslabs/flutter:3.44.0`.
- Android SDK Platform 37 and Build-Tools 37.0.0 (`compileSdk = 37`, needed by connect-client 1.2.0-alpha04)
- JDK 17
- Android Studio is optional; the command line is enough

Clone the repository, then:

```bash
flutter pub get
flutter run                 # debug build on a connected device or emulator
```

The debug build installs as `tech.mmarca.openvitals.debug`, so it can live next to a release install without a signature clash.

Build a release APK:

```bash
flutter build apk --release
```

Release signing is read from the process environment only — no keystore and no credentials are ever committed, and nothing is read from `gradle.properties` or `local.properties`:

- `OPENVITALS_RELEASE_STORE_FILE`
- `OPENVITALS_RELEASE_STORE_PASSWORD`
- `OPENVITALS_RELEASE_KEY_ALIAS`
- `OPENVITALS_RELEASE_KEY_PASSWORD` (for a PKCS12 store, the store password is also the key password)

**A release build without those variables is unsigned, by design.** It does not silently fall back to the debug key: an unsigned artifact fails loudly, whereas a debug-signed one looks fine until it reaches a real device and cannot update the installed app.

Checks, which mirror the ones CI runs:

```bash
flutter test
flutter analyze lib test
dart run tool/verify_l10n.dart   # translation coverage + placeholder gate
flutter gen-l10n                 # generated l10n must be up to date with the ARBs
git diff --check
```

Code generation (freezed, json_serializable, riverpod, drift) is not checked in as a build step — regenerate after touching an annotated model:

```bash
dart run build_runner build --delete-conflicting-outputs
```

Translation work is documented in [`docs/engineering/translations.md`](docs/engineering/translations.md).

After launching the app:

1. Complete onboarding
2. Use one-tap setup to grant all requestable Health Connect permissions, or grant Activity & sleep first and then choose individual categories
3. Grant Cycle tracking only if you want period, ovulation, cervical mucus, and basal temperature data shown
4. Use Dashboard for read-only summaries and Add entry for explicit Health Connect logging

## Architecture at a glance

OpenVitals is intentionally simple today:

- one Flutter app, plus one in-repo plugin package for the native Health Connect bridge
- Material 3 UI, `dynamic_color` theming
- Riverpod for state and dependency wiring: `Notifier`/`AsyncNotifier` per screen, providers instead of a DI container
- immutable state classes generated with `freezed`
- `go_router` for navigation
- `drift` for a small local database that caches derived summaries (Health Connect stays the source of truth)
- Health Connect reached through the `health_connect_native` Pigeon plugin, wrapped by `HealthDataSource` and a narrow set of feature repositories (activity, sleep, heart, body, body energy, hydration, caffeine, nutrition, mindfulness, cycle, vitals)
- Android home-screen widgets rendered by Glance, fed snapshots pushed from Dart
- `shared_preferences` for onboarding completion, acknowledged permissions, unit system, widget order, calorie display mode, caffeine preferences, import status, recording preferences, hydration containers, and reminders
- shared presentation formatters for units and date/time labels; storage is always metric, imperial is a display/input preference only

Implementation rules for new work are in [`AGENTS.md`](AGENTS.md).

## Project layout

- [`lib/features/`](lib/features): feature screens, notifiers, and feature-specific cards/charts
- [`lib/data/`](lib/data): repositories over Health Connect reads, drift database, and preferences
- [`lib/domain/`](lib/domain): pure models, insight calculations, queries, and preference enums
- [`lib/health/`](lib/health): `HealthDataSource`, permission model, and the native Health Connect data source
- [`lib/core/`](lib/core): period/date-window math, presentation formatters, geo, reminders, diagnostics
- [`lib/ui/`](lib/ui): shared scaffolding, components, charts, and theme
- [`lib/l10n/`](lib/l10n): ARB catalogs (the l10n source of truth) and generated `AppLocalizations`
- [`packages/health_connect_native/`](packages/health_connect_native): Pigeon plugin wrapping the AndroidX Health Connect client
- [`android/`](android): Android host app, Glance widgets, release signing
- [`tool/`](tool): repo tooling, including the translation validator
- [`scripts/`](scripts): CI, release, and Codeberg publishing scripts
- [`.woodpecker/`](.woodpecker): Woodpecker CI pipelines (tests and releases)
- [`docs/`](docs): app guide, feature guide, engineering docs, how-to guides, proposals, reference material, and release notes

## Documentation

- [`docs/README.md`](docs/README.md): documentation index
- [`docs/app/README.md`](docs/app/README.md): user guide, permissions, privacy, FAQ, screenshots, and support
- [`docs/features/README.md`](docs/features/README.md): grouped feature guide
- [`docs/features/feature-map.md`](docs/features/feature-map.md): map from features to routes, screens, and packages
- [`docs/engineering/README.md`](docs/engineering/README.md): architecture, development, feature playbook, and translations
- [`Features.md`](Features.md): functional inventory of view, write, import, settings, and privacy capabilities
- [`CHANGELOG.md`](CHANGELOG.md): user-facing release history
- [`AGENTS.md`](AGENTS.md): implementation guidance for future coding agents

## License

OpenVitals is licensed under the [`GNU Affero General Public License v3.0 or later`](LICENSE).
Project thanks are listed in [`THANKS.md`](THANKS.md), and third-party asset notices are listed in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
