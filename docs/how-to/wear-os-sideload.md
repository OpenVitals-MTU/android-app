# Install the Watch App on a Wear OS Watch

OpenVitals talks to a Wear OS watch through its own small watch app. The watch app is not on any store a watch can reach, so it is installed over adb from a computer. This guide covers that, and the optional step that lets a brand-new watch skip the vendor's onboarding so the vendor's phone app is never needed.

For what the watch app does once installed, see [Watches](../features/watches.md#wear-os-companion-app).

Everything below is driven by three small scripts in the repository: `scripts/wear-pair.sh`, `scripts/wear-skip-onboarding.sh` and `scripts/wear-install.sh`. Each step can also be typed by hand; the commands are listed at the end.

## Prerequisites

On the computer:

- A clone of the OpenVitals repository.
- Android platform-tools (`adb`). The script finds it on `PATH`, in `$ANDROID_HOME/platform-tools`, or in the default SDK location under your home directory.
- To build the watch app yourself: a JDK and the Android SDK, the same setup as [development.md](../engineering/development.md). To install an APK you already have, neither is needed; pass it with `--apk`.
- Optional: `avahi-browse` (package `avahi-utils`) makes discovery more reliable on Linux desktops that run their own mDNS daemon.

On the watch:

- Wear OS 3 or later (Android 11, API 30). Verified on a Galaxy Watch8 running Wear OS on Android 16.
- Wi-Fi, on the same network as the computer. There is no USB port on a watch and debugging over Bluetooth goes through the vendor's phone app, so Wi-Fi is the only way in. A watch joins Wi-Fi on its own, without a phone or an account: Settings, Connections, Wi-Fi.
- Developer options unlocked: Settings, About watch, Software, tap the software version five times until the watch says developer mode is on. On a Pixel Watch it is Settings, System, About, tap the build number seven times.
- Wireless debugging on: Settings, Developer options (at the bottom of Settings), ADB debugging on, then Wireless debugging on. Its screen shows the connection address; Pair new device shows the pairing address and code.

On the phone:

- OpenVitals installed. A debug watch app pairs with a debug phone app, a release watch app with the release one.
- No vendor app is required. Galaxy Wearable, the Pixel Watch app and the like can stay uninstalled.

## Two starting points

**The watch was onboarded with the vendor's phone app.** Skip to [Pair the computer](#1-pair-the-computer). The vendor app can be uninstalled from the phone afterwards; the Bluetooth bond is Android's and survives.

**The watch is new, or was factory reset, and shows the "start on your phone" screen.** Wear OS blocks the watch behind a setup wizard that only the vendor's phone app completes. The `wear-skip-onboarding.sh` step marks the watch as set up without it, so the vendor app is never installed. Whether that is possible depends on the watch, see below.

### Which watches can skip the onboarding

Skipping needs adb, and adb on a watch means Wi-Fi plus Wireless debugging, both of which live in Settings. So the bypass works only on a watch whose welcome screen lets Settings open before onboarding. Where it does not, the vendor's phone app has to run once; it can be uninstalled right after, and `wear-skip-onboarding.sh` then has nothing to do.

| Watch | Settings reachable before onboarding | Notes |
| --- | --- | --- |
| Samsung Galaxy Watch8 | No | Checked 2026-10-08 after a factory reset: the welcome screen opens neither the quick panel nor Settings. Onboard once with Galaxy Wearable, then uninstall it. Other One UI Watch models are expected to behave the same. |
| Google Pixel Watch | Reported yes | The bypass was reported for Wear OS watches in the Gadgetbridge community and pixelbridge runs on a Pixel Watch without the companion app. Not verified by this project yet. |

On a watch to try: from the welcome screen, swipe down from the top for the quick panel and its gear icon, or look for a "?" or "i" icon. If Settings opens, join Wi-Fi, turn on Developer options and Wireless debugging, and follow the steps below from the start. Please report what you find for your model.

## 1. Pair the computer

Once per computer. On the watch open Wireless debugging, Pair new device. It shows an address with a pairing port and a six-digit code.

```bash
scripts/wear-pair.sh 192.168.1.80:46031 643490
```

The pairing port is different from the connection port the Wireless debugging screen shows at the top, and both change on every boot. After pairing, the script lists what it can see on the network, phones included, so the watch's connection address is already on screen. A factory reset also hands the watch a new IP address, so do not reuse an old one.

## 2. Find the watch

```bash
scripts/wear-pair.sh
```

With no arguments the script only lists what announces adb on the network, phones included, with the current address. Pass the watch's address to the following commands with `--serial`, or export it as `ANDROID_SERIAL`.

## 3. Skip the setup wizard (new watch only)

```bash
scripts/wear-skip-onboarding.sh --serial 192.168.1.80:45009
```

The script checks whether the watch already reports setup complete and stops there if it does. Otherwise it shows what it is about to change and asks for confirmation. It then:

1. disables the two Wear OS setup wizard packages, `com.google.android.wearable.setupwizard` and `com.google.android.setupwizard`, for the main user;
2. writes `device_provisioned=1` and `user_setup_complete=1`, the two settings the vendor onboarding ends with;
3. reboots the watch and waits for it to announce adb again on a new port.

Nothing on the watch is erased. To undo, enable the two packages again with `adb shell pm enable <package>`, or factory reset.

Wireless debugging may be off after the reboot on some watches. If the script gives up waiting, turn it on again on the watch, run `wear-pair.sh` for the new address, and continue.

This step is the community-reported bypass, not a vendor-supported path, and only reachable on the watches listed above. What it leaves unconfigured is the vendor's own setup: a Samsung account, Samsung Health onboarding, Google account sign-in. OpenVitals needs none of those.

## 4. Install the watch app

```bash
scripts/wear-install.sh --serial 192.168.1.80:45009
```

With no `--apk`, the script uses the debug build output and runs `./gradlew :wear:assembleDebug` when it is missing. `--release` switches to the release build output. The debug APK installs as `tech.mmarca.openvitals.debug`, the release one as `tech.mmarca.openvitals`.

After the install the script grants the runtime permissions the watch app needs, so no dialog has to be answered on the wrist:

| Permission | Why |
| --- | --- |
| `BLUETOOTH_CONNECT` | the phone link over RFCOMM |
| `POST_NOTIFICATIONS` | the ongoing "Phone link" notification of the foreground service |
| `READ_HEART_RATE`, `READ_HEALTH_DATA_IN_BACKGROUND` (Android 16+) | the heart rate sensor, on and off screen |
| `BODY_SENSORS`, `BODY_SENSORS_BACKGROUND` (Android 13 to 15) | the same on older releases |

Pass `--no-grant` to let the app ask instead. Finally the script launches the app and prints the status below.

## 5. Check

```bash
scripts/wear-install.sh --serial 192.168.1.80:45009 --status
```

Shows whether setup is complete, the installed version, which permissions are granted, and whether the phone link service is running. The service starts when the app has the Bluetooth permission; opening the app once on the watch starts it too.

Verified on 2026-10-08 on a Galaxy Watch8 (Wear OS on Android 16) that had been factory reset and onboarded once with Galaxy Wearable: pairing, discovery and the install with the debug build ran through unattended, all four permissions were granted, and the status reported the phone link service running.

## 6. Pair the phone

1. On the phone, Bluetooth settings: pair the watch. A dual-mode watch appears twice, once per radio; either entry bonds both.
2. In OpenVitals: Settings, Watches, add a watch. The scan offers the watch as a Wear OS watch once its service list names the OpenVitals watch app.
3. On the watch's screen in OpenVitals, "Validate Wear OS App" should answer, and the Heart Rate Sync card pulls what the watch has recorded.

## Troubleshooting

- **`discover` finds nothing.** Wireless debugging is off, or the watch is on another network or asleep. Raise the wrist and open the Wireless debugging screen; the watch announces adb while it is awake.
- **The address stopped working.** The port rotates on every boot and sometimes after a long sleep, and the watch drops Wi-Fi while asleep. Wake it and run `wear-pair.sh` with no arguments for the current address.
- **`adb` sees the watch as `unauthorized`.** The computer is not paired, or a factory reset wiped the pairing. Run `wear-pair.sh` again with a fresh code.
- **The watch shows the setup screen again after a reboot.** The two settings did not stick, or the wizard packages were re-enabled by an update. Run `wear-skip-onboarding.sh` again and report the watch model.
- **The phone link service stops overnight.** Samsung's battery manager can stop a sideloaded service. On the watch: Settings, Battery, App power management, exclude OpenVitals. Not yet verified on every model.
- **Debug over Bluetooth.** Not an option: it proxies through the vendor's phone app, which this guide removes.

## The same by hand

```bash
export ADB_MDNS_OPENSCREEN=1                       # discovery next to a system mDNS daemon
adb mdns services                                  # or: avahi-browse -rt _adb-tls-connect._tcp
adb pair <ip>:<pairing-port> <code>
adb connect <ip>:<port>

# new watch only
adb shell pm disable-user --user 0 com.google.android.wearable.setupwizard
adb shell pm disable-user --user 0 com.google.android.setupwizard
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb reboot

./gradlew :wear:assembleDebug
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
adb shell pm grant tech.mmarca.openvitals.debug android.permission.BLUETOOTH_CONNECT
adb shell pm grant tech.mmarca.openvitals.debug android.permission.POST_NOTIFICATIONS
adb shell pm grant tech.mmarca.openvitals.debug android.permission.health.READ_HEART_RATE
adb shell pm grant tech.mmarca.openvitals.debug android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND
adb shell am start -n tech.mmarca.openvitals.debug/tech.mmarca.openvitals.wear.MainActivity
```

The setting is `user_setup_complete`; a version of this recipe that circulates writes `usersetupcomplete`, which Android ignores.
