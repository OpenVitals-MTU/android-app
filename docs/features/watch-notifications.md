# Garmin Watch Notifications

> **Status:** Current implemented behavior. Experimental — developed and verified against a vívoactive 5.
> **Audience:** Users and contributors.
> **Implementation:** `lib/devices/garmin/garmin_notification_*.dart` + `garmin_notifications_handler.dart` + `garmin_radio_lease.dart` (the GNCS stack), `packages/notification_listener_native/` (capture + filter + the headless engine), `lib/features/settings/application/watch_notifications_view_model.dart` + `presentation/watch_notification_apps_screen.dart`, the notifications card in `watch_device_screen.dart`.
> **Navigation:** the card lives on `/watch/:watchDeviceId`; the app picker is `/watch/:watchDeviceId/notifications`.
> **Related:** [Feature map](feature-map.md), [Garmin watch sync](garmin-watch-sync.md), [Permissions](../app/permissions.md), [Privacy](../app/privacy.md).

Phone notifications appear on the wrist, and can be dismissed, answered or acted on from there. No Garmin account, no Garmin Connect, and no network — the watch is spoken to over its own Bluetooth protocol, and the app ships no `INTERNET` permission.

## How to use it

1. Pair a watch (see [Garmin watch sync](garmin-watch-sync.md)) and open it from **Settings › Watches**.
2. Under **Notifications**, turn on **Send notifications to the watch**.
3. OpenVitals shows what it will read and asks you to confirm, then opens Android's **Notification access** screen. Allow OpenVitals there.
4. Come back. The switch turns on by itself once access is granted.

**Apps to silence** lists every app on the phone. Everything sends to the watch until you switch an app off.

The feature is off by default and does nothing at all until step 2 — no capture, no Bluetooth link, no background work.

## What reaches the watch

A notification's app, title, subtitle, body and time. The watch decides how much of it to show.

These never reach it:

| Not forwarded | Why |
|---|---|
| Ongoing and foreground-service notifications | A media player or a download repaints these constantly; the watch would never stop buzzing. |
| Group summaries | The individual messages come through instead — keeping both delivers every chat twice. |
| Notifications an app marks local-only | The app asked for it not to leave the phone. |
| Notifications with no title and no body | There is nothing to render. |
| Minimum-importance channels | You already told Android these do not deserve attention. |
| Apps you silenced | Your choice, in **Apps to silence**. |
| Anything, while Do Not Disturb is on | Your phone's own setting, rather than a second one here to keep in sync. |
| OpenVitals' own notifications | A hydration reminder would otherwise arrive twice. |

## Acting from the wrist

- **Dismiss** clears the notification from the phone too.
- **The app's own buttons** — "Mark as read", "Archive", "Snooze" — do exactly what they do on the phone. Up to five.
- **Reply**, on apps that support it, sends your dictated or canned reply through the app itself.

Only actions that actually work are offered. An action that would merely open the posting app's own screen is not shown, because Android will not let a background app launch it: it would be a button that reports success and does nothing.

## The connection

The link to the watch is **held open** for as long as the watch is in range, which is how Garmin watches expect a phone to behave. Opening it only when a notification arrives was tried first, and the watch spent most of its life disconnected — saying "reconnect to phone to refresh data" — and sometimes failed to re-subscribe on a quick reconnect, losing the notification.

If the watch goes out of range the link is re-established, backing off from 15 seconds up to 5 minutes so a watch left at home is not retried all day. Anything that arrived while it was away is delivered when it comes back.

**Syncing still wins.** Tapping Sync, Find, or opening the watch's settings takes the radio back within a few seconds; notifications resume afterwards.

## Known Limitations

- **No SMS replies.** A stock SMS app publishes no reply action a watch can trigger — its "Reply" opens the phone's compose screen rather than sending, and Android blocks that from the background. Replying would require OpenVitals to send the SMS itself, which needs `RECEIVE_SMS` and `SEND_SMS`; that is deliberately out of scope for a health app. Replies work on apps that publish a proper wearable action, which most messaging apps do.
- **Notifications during a recorded activity share one radio.** A sync or a find takes priority; notifications wait a few seconds.
- **Ten deep.** The watch can ask about a notification long after it arrived; older ones can no longer be described and render blank on the watch if it asks.
- **No custom vibration control.** Whether the watch buzzes is the watch's own setting, not something the phone chooses.
- **Verified on a vívoactive 5.** The protocol is not model-specific and the feature is gated on the watch reporting `GNCS`, but no other model has been tested.

## Privacy

Notification text is read on the device, held in memory only while it is needed, and sent only to the paired watch over Bluetooth. It is never written to a file or a database, and the shipping app has no `INTERNET` permission, so it cannot go anywhere else. Turn the feature off, or revoke notification access in Android settings, and it stops immediately. See [Privacy](../app/privacy.md).

## For Contributors

GNCS is a **pull** protocol and that shapes the whole design. The phone announces only that a notification exists — an id, a category and some flags, and no text. The watch then asks for the attributes it wants, and the phone streams them back in ≤300-byte chunks with a running CRC. If the watch never asks, the text never leaves the phone and there is **no error**; silence is a legitimate outcome.

Everything above the radio is transport-free and unit-tested over an in-memory pipe, exactly like the sync stack. The native plugin captures and filters and touches no Bluetooth; the filter runs before any Flutter engine is spun, which is the single biggest battery decision in the feature.

Three findings from a real watch, all now covered by tests, are worth knowing before changing anything:

- The subscription reply is the **phone's** willingness, not the conjunction of both flags. A watch that has never been told a phone would forward sends `enable=false`; answering DISABLED confirms it and the watch never flips.
- An announcement must go out **after** the subscription status. Sent before, the watch acknowledges the frame and then never asks for the text.
- `MESSAGE_SIZE` must be the last attribute in the blob, wherever the watch asked for it, or the body renders empty.

Ported files from [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) (AGPLv3, the same licence as this app) name their origin in the file header.
