# Watches

> **Status:** Current implemented behavior.
> **Audience:** Users and contributors.
> **Related:** [Feature map](feature-map.md), [Bluetooth LE sensors](ble-sensors.md), [FIT files import](fit-files-import.md), [Health Connect](../app/health-connect.md).

OpenVitals does not link to watches directly. Watch data arrives through
Health Connect, written by a companion app — for example
[Gadgetbridge](https://gadgetbridge.org/) for Garmin and many other makes, or
the vendor's own app where it writes to Health Connect. Once the companion app
syncs the watch, its activities, sleep, heart rate, and other supported
measures appear on the normal metric screens like data from any other source.

Earlier releases carried an experimental direct Garmin integration (Bluetooth
sync, a watch-only data screen, notification forwarding, watch settings, and
find-my-watch). That integration has been removed; pairing the watch through a
companion app replaces it. Data the old integration had already written to
Health Connect is untouched.

What remains in OpenVitals itself:

- **Live heart rate during a recording.** A watch that broadcasts standard
  Bluetooth LE heart rate (Garmin's Broadcast Heart Rate mode, most WearOS
  watches) can be added as a [Bluetooth LE sensor](ble-sensors.md) and stream
  into an activity recording.
- **FIT file import.** Activity and wellness FIT files exported from the watch
  or its vendor service can be [imported directly](fit-files-import.md).
