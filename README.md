<p align="center">
    <img width="160" alt="OpenVitals logo" src="docs/images/readme-logo.png">
</p>

# OpenVitals

<p align="center">
    <a href="https://liberapay.com/manuel.mmarca.tech/donate"><img alt="Liberapay receiving" src="https://img.shields.io/liberapay/receives/manuel.mmarca.tech.svg?logo=liberapay"></a>
    <a href="https://liberapay.com/manuel.mmarca.tech/donate"><img alt="Liberapay patrons" src="https://img.shields.io/liberapay/patrons/manuel.mmarca.tech.svg?logo=liberapay"></a>
</p>

Privacy-first Health Connect dashboard, activity tracker, and manual entry app for Android.

OpenVitals reads from Health Connect and gives you one place to review the data already there, add your own records, record activities, import existing data, and work with recovery and readiness metrics locally.

The dashboard is read-only by default. Writes to Health Connect happen only through actions you start or enable, such as saving, recording, importing or syncing data.

No OpenVitals account is required, and the app has no `INTERNET` permission. OpenVitals is open source under AGPL-3.0-or-later.

## Install

| Channel | Link |
| --- | --- |
| Google Play | [Install or join testing](https://play.google.com/store/apps/details?id=tech.mmarca.openvitals) |
| F-Droid | [Install from F-Droid](https://f-droid.org/en/packages/tech.mmarca.openvitals/) |
| GitHub releases | [Download signed release and debug APKs](https://github.com/OpenVitals-MTU/android-app/releases) |
| Source | [Build it yourself](#build-from-source) |

## What you can do with OpenVitals

- Review most metrics by day, week, month or year, with charts, history, source information and period statistics. Charts also expose summaries and navigation to screen readers.
- Log by hand: drinks and hydration, food and nutrition, activities, mindfulness, cycle records, weight, height, body fat, blood pressure, SpO2, respiratory rate and body temperature. Records with a matching Health Connect type are written there; some cycle-journal data stays local because Health Connect has no record type for it.
- Record activities with GPS, offline maps and Bluetooth sensors. Routes and workouts can also be imported, and guided workout plans can be run from the phone.
- Read recovery views derived on the phone: Daily Readiness, Body Energy and Training Readiness, together with HRV status, physiological stress, adaptive goals and explanation screens based on the data available.
- Bring in Apple Health exports, CSV data, route and workout files, and supported medical records.
- Generate PDF health reports fully on-device, with charts, statistics and sections for supported health data.
- Track your cycle with a local day journal, estimates, reminders, pill tracking, backup and a home-screen widget alongside the records kept in Health Connect.
- Put widgets on the home screen for readiness, Body Energy, vitals, selected metrics and quick beverage logging.
- Choose metric or imperial units, and pick the app language from fourteen translations or follow the system.

For the detailed feature inventory, see the [feature guide](https://docs.openvitals.health/app/features).

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

More screenshots are on the [docs site](https://docs.openvitals.health/screenshots).

## Devices and integrations

### Garmin watches

[Garmin support](https://docs.openvitals.health/features/smartwatches) is experimental. A paired watch talks to OpenVitals over Bluetooth, with no Garmin account required. OpenVitals itself has no `INTERNET` permission.

Supported synced data is written to Health Connect where OpenVitals has a matching mapping and Health Connect has a suitable record type. Garmin-only measurements such as Body Battery and watch stress stay in OpenVitals on the phone.

<details>
<summary>Current Garmin support</summary>

- Sync recorded activity, sleep and wellness files over Bluetooth, manually or with optional periodic sync.
- Both Garmin transport paths are supported, including older watches such as the Instinct 2X.
- A dashboard watch tile shows the paired watch and provides sync controls.
- Activities are written to Health Connect with routes and recorded series where available.
- Sleep, heart rate, resting heart rate, HRV, respiratory rate, VO2 max, BMR, steps, distance, active calories and supported weight data are written to Health Connect where supported.
- Body weight from a paired Garmin scale can be relayed through the watch integration.
- Watch-only data includes stress, Body Battery, intensity minutes, sleep score and sleep need, recovery time, Garmin training readiness, and acute/chronic training load.
- For watches that provide sleep but no stages, OpenVitals can estimate the stages on the phone.
- An optional stay-connected mode keeps the Bluetooth link open and shows live heart rate and steps.
- Forward phone notifications and ringing calls to the watch, with per-app blocking and supported notification actions.
- Weather can come from a weather app on the phone; calendar and media controls are optional.
- Find the watch from the phone, or ring the phone from the watch.
- Read and change the watch's settings tree and alarms where supported. Older watches without a settings tree can use alarms kept on the phone and sent to the watch.
- Send an imported GPS ephemeris file to the watch.
- Send named coordinates to the watch's saved locations.

</details>

### Bathroom scale

Experimental [support for the Xiaomi Body Composition Scale S400](https://docs.openvitals.health/features/scales). Every weigh-in is saved to Health Connect as it happens, with the app open or closed, decoded from the scale's own Bluetooth broadcast with the key from your Xiaomi account. Weight and heart rate are saved as measured; body fat, lean mass and body water are estimated on the phone. OpenVitals never connects to the scale and never contacts Xiaomi.

### Bluetooth sensors

Activity recording can use [Bluetooth LE sensors](https://docs.openvitals.health/features/ble-sensors) for heart rate, cycling power, speed and cadence. Wheel-sensor distance can be used on rides without GPS.

A connected heart-rate sensor can also be used during strength training and for the [heart-rate recovery test](https://docs.openvitals.health/features/heart-rate-recovery).

### CoMaps

While [CoMaps](https://docs.openvitals.health/features/comaps-navigation-context) is navigating, OpenVitals can show its turn guidance on the offline activity map during a recording, and optionally save the guidance with the activity on-device.

Guidance can also be sent to a paired Garmin watch on its own switch, independently of activity recording.

### Wear OS watches

A [Wear OS watch](https://docs.openvitals.health/features/smartwatches) can be registered in OpenVitals, and a dashboard card shows whether it is paired and whether the OpenVitals Wear OS app on it answers. The watch app is still in development. Recorded data reaches the phone through Health Connect rather than through the direct Garmin sync path.

### Sync with another phone

OpenVitals can [copy selected Health Connect records](https://docs.openvitals.health/features/device-sync) between two nearby phones over Bluetooth. If the cycle category is selected, its local journal, excluded cycles and declared cycle context are carried with it too.

Both phones show the same six-digit code before anything is transferred. The records are then encrypted for that sync session.

You choose the time range and data categories to copy. Medical records are an exception: when selected, they sync in full regardless of the chosen range.

Re-running a sync does not create duplicate records.

## Current coverage

OpenVitals currently has screens or data handling for:

- Activity and workouts
- Sleep and recovery
- Heart and vitals
- Body measurements
- Nutrition, hydration and caffeine
- Mindfulness
- Cycle tracking
- Goals
- Achievements
- Medical records
- Garmin-only wellness data

<details>
<summary>What is covered in each area</summary>

- **Activity and workouts:** steps, distance, active and total calories, floors, elevation, wheelchair pushes, exercise sessions, routes, cardio load, speed, power and cadence where available.
- **Sleep and recovery:** sleep sessions and stages, sleep score, sleep efficiency, OpenVitals-derived Daily Readiness, Body Energy and Training Readiness, HRV status, intensity minutes and physiological stress.
- **Heart and vitals:** heart rate, resting heart rate, HRV, blood pressure, SpO2, respiratory rate, body temperature, skin temperature, blood glucose and VO2 max. Some of these are view-only in the manual-entry UI.
- **Body:** weight, height, BMI, body fat, lean body mass, BMR, bone mass, body water and Fat-Free Mass Index (FFMI) context where the required measurements are available.
- **Nutrition, hydration and caffeine:** drinks, hydration, meals, calories, macros, caffeine and the additional Health Connect nutrients present in the data.
- **Mindfulness:** session history, totals, goals and timer/manual logging where the installed Health Connect provider supports mindfulness records.
- **Cycle tracking:** periods and flow, ovulation tests, cervical mucus, basal body temperature, intermenstrual bleeding and sexual activity. The local journal also keeps data that Health Connect has no record type for, such as pain, mood, energy, symptoms and notes.
- **Goals:** configurable daily goals for supported activity metrics, workout minutes, hydration, sleep, nutrition and mindfulness.
- **Achievements:** badges and progress for daily steps, lifetime distance, floors and other supported achievement categories.
- **Medical records:** the Health Connect medical-record categories available on Android 14 and newer with a recent Health Connect module. Records are shown as stored; OpenVitals does not interpret their clinical meaning.
- **Garmin-only data:** stress, Body Battery, intensity minutes, watch sleep metrics, recovery time, Garmin training readiness and acute/chronic training load when the paired watch supplies them.

</details>

A fuller breakdown of what can be viewed, entered, edited, imported or exported is in the [feature guide](https://docs.openvitals.health/app/features).

## Privacy

- No account and no OpenVitals server receiving health data.
- No ads or analytics SDKs.
- The shipping app has no Android `INTERNET` permission.
- No Google Play Services dependency is required for app functionality.
- OpenVitals opts out of Android cloud backup.
- Health Connect remains the source of truth wherever it has a record type.
- The dashboard is read-only by default; writes happen only through actions you explicitly start or enable.
- Bluetooth is used for sensors, paired watches, the scale and phone-to-phone sync. Data sent over Bluetooth goes only to devices or phones you pair or use with those features.
- Files are exported or shared only when you choose to do so.

Health Connect access is split into categories rather than requested as one block:

- **Activity & sleep:** used by the dashboard.
- **Heart & recovery, Body, Activity extras, Nutrition & hydration, Mindfulness, and Vitals:** optional categories.
- **Cycle tracking:** separate sensitive access that can be granted or skipped explicitly.
- **Medical records:** kept separate from the other Health Connect permissions and requested only when you enter the Medical records area.
- **Manual-entry write access:** available during one-tap setup or requested when an entry flow needs it.

Permissions can be changed later in Settings.

OpenVitals does keep some app-only data locally where Health Connect cannot represent it, as well as caches and files used by features you enable.

<details>
<summary>Local data</summary>

This includes things such as:

- derived summary caches and Body Energy history;
- food and drink catalogues;
- Garmin-only wellness data and temporary per-minute watch data used for sleep processing;
- the readings of each bathroom scale weigh-in;
- the parts of the cycle journal that Health Connect cannot store, along with cycle exclusions and pill intakes;
- metadata used by device sync;
- downloaded watch files, recordings not yet saved, staged Apple Health imports, imported offline maps and elevation tiles;
- medical documents you explicitly choose to keep;
- the last phone-to-phone sync report.

These are stored in app-private storage on the device. Some local data can leave that storage only through an action you explicitly start; for example, cycle-journal data can be copied to another phone when the cycle category is selected in phone-to-phone sync.

</details>

See the [privacy policy](https://docs.openvitals.health/app/privacy) for the full text.

## Platform requirements

- Android only
- `minSdk 26`
- `compileSdk 37`
- `targetSdk 36`
- JDK 17 / Java 17 toolchain
- Health Connect required

Health Connect platform notes:

- On Android 14 and newer, Health Connect is part of the system
- On Android 13 and older, the Health Connect app must be installed separately
- Health Connect is not supported in work profiles
- Mindfulness sessions require a Health Connect provider version that supports `FEATURE_MINDFULNESS_SESSION`
- The app uses `androidx.health.connect:connect-client` 1.2.0-alpha06 so AndroidX maps newer activity, mindfulness, and aggregation APIs to the current platform permissions

## Documentation

The user documentation lives at [docs.openvitals.health](https://docs.openvitals.health/):

- [Install](https://docs.openvitals.health/app/install), [getting started](https://docs.openvitals.health/app/getting-started), [Health Connect setup](https://docs.openvitals.health/app/health-connect), [permissions](https://docs.openvitals.health/app/permissions), [privacy](https://docs.openvitals.health/app/privacy) and [FAQ](https://docs.openvitals.health/app/faq)
- [Feature guide](https://docs.openvitals.health/features): one page per feature, grouped by what you do with the app
- [Feature map](https://docs.openvitals.health/features/feature-map): map from features to routes, widgets, and packages
- [How-to guides](https://docs.openvitals.health/how-to): step-by-step workflows such as offline maps and elevation tiles
- [Build from source](https://docs.openvitals.health/developers/build) and [contributing](https://docs.openvitals.health/developers/contributing)
- [Support](https://docs.openvitals.health/support)

Engineering docs stay in this repository:

- [`docs/engineering/architecture.md`](docs/engineering/architecture.md): current architecture and target direction
- [`docs/engineering/development.md`](docs/engineering/development.md): local build, verification, CI, and Windows cleanup notes
- [`docs/engineering/feature-playbook.md`](docs/engineering/feature-playbook.md): checklist for adding a new metric feature
- [`AGENTS.md`](AGENTS.md): implementation guidance for future coding agents

## Help Improve It

OpenVitals is still early. Useful feedback is specific: device model, Android version, Health Connect provider version, which permissions were granted, and what screen or workflow failed.

- Try the latest beta from Google Play or GitHub releases
- Report bugs and feature requests on [GitHub issues](https://github.com/OpenVitals-MTU/android-app/issues)
- Translate OpenVitals in your language on [Codeberg Translate](https://translate.codeberg.org/projects/openvitals/android-app/)
- Ask questions and discuss support on [OpenVitals Zulip](http://openvitals.zulipchat.com/)
- Star or follow the project on [GitHub](https://github.com/OpenVitals-MTU/android-app)
- Share screenshots or notes from real Health Connect setups, especially route recording and manual entry flows
- Support ongoing development on [Liberapay](https://liberapay.com/manuel.mmarca.tech/donate)

## Build from source

1. Install a recent Android Studio with Android SDK 37.0 and JDK 17 support.
2. Clone this repository.
3. Open the project in Android Studio, or build from the command line.

In a complete checkout:

```bash
./gradlew :app:assembleDebug
```

To run the same basic checks used by CI:

```bash
./gradlew verifyCi
git diff --check
```

To install on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

<details>
<summary>Windows: cleaning fails with a locked lint cache</summary>

On Windows, Gradle or Android Studio can occasionally keep lint cache jars open under `app/build`. If cleaning fails with a locked `lint-cache` jar, stop Gradle daemons first:

```powershell
.\gradlew.bat --stop
Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -like '*org.gradle.launcher.daemon.bootstrap.GradleDaemon*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Remove-Item -LiteralPath app/build -Recurse -Force
```

</details>

More local development notes are in [`docs/engineering/development.md`](docs/engineering/development.md).

After launching the app:

1. Complete onboarding
2. Use one-tap setup to grant all requestable Health Connect permissions, or grant Activity & sleep first and then choose individual categories
3. Grant Cycle tracking only if you want period, ovulation, cervical mucus, and basal temperature data shown
4. Use Dashboard for read-only summaries and Add entry for explicit Health Connect logging

## Architecture at a glance

The repository has two Gradle modules. `:app` is the phone app; `:wear` is the Wear OS companion app, still in development, with its own CI gate.

The phone app uses:

- Jetpack Compose, Material 3 and Navigation Compose for the UI;
- `ViewModel`, coroutines and `StateFlow` for screen state;
- Hilt for dependency injection;
- the AndroidX Health Connect client behind `HealthConnectManager` and feature-specific repositories;
- Room for derived caches and data Health Connect cannot represent. Health Connect remains the source of truth for record types it supports;
- WorkManager for Apple Health imports, offline-map imports and the opt-in periodic Garmin watch-sync job.

Watch and scale integration lives under `devices/`, including the Garmin protocol stack, shared Bluetooth radio handling, companion-device pairing, notification forwarding and the scale listener.

Live Bluetooth LE sensor streaming during activity recording is separate under `sensors/ble/`.

Phone-to-phone Health Connect sync lives under `features/devicesync/` and uses Bluetooth Classic RFCOMM. The CoMaps provider lives under `comaps/` and reads guidance through Android's `ContentResolver`.

See the [architecture documentation](docs/engineering/architecture.md) for the current structure and development direction.

## Project layout

- [`app/`](app): Android phone app module
- [`wear/`](wear): Wear OS companion app module
- [`app/src/main/kotlin/tech/mmarca/openvitals/features/`](app/src/main/kotlin/tech/mmarca/openvitals/features): feature screens, state, and ViewModels
- [`app/src/main/kotlin/tech/mmarca/openvitals/data/repository/`](app/src/main/kotlin/tech/mmarca/openvitals/data/repository): repositories over Health Connect reads and preferences
- [`app/src/main/kotlin/tech/mmarca/openvitals/healthconnect/`](app/src/main/kotlin/tech/mmarca/openvitals/healthconnect): Health Connect readers, writers and the permission shell
- [`app/src/main/kotlin/tech/mmarca/openvitals/devices/`](app/src/main/kotlin/tech/mmarca/openvitals/devices): Garmin, Wear OS and scale device layer
- [`app/src/main/kotlin/tech/mmarca/openvitals/core/`](app/src/main/kotlin/tech/mmarca/openvitals/core): app-local period, performance, and presentation primitives
- [`app/src/main/kotlin/tech/mmarca/openvitals/domain/`](app/src/main/kotlin/tech/mmarca/openvitals/domain): app-local models, insight calculations, and preference enums
- [`app/src/main/kotlin/tech/mmarca/openvitals/ui/components/`](app/src/main/kotlin/tech/mmarca/openvitals/ui/components): shared UI scaffolding and navigation components
- [`docs/`](docs): app guide, feature guide, engineering docs, how-to guides, proposals, reference material, and release notes

## License

OpenVitals is licensed under the [`GNU Affero General Public License v3.0 or later`](LICENSE).
Project thanks are listed in [`THANKS.md`](THANKS.md), and third-party asset notices are listed in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
