package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.devices.notifications.NotificationMsg

/**
 * Turns a notification's Android actions into the ones a watch can offer.
 *
 * Two things make this more than a `map`:
 *
 * * **GNCS has fixed slots, Android has a list.** The wire format offers a
 *   reply, a dismiss, and five numbered custom slots — nothing else. An app
 *   with eight buttons has to lose some, and which ones it loses should be
 *   predictable rather than incidental.
 * * **Dismiss is ours, not the app's.** Android has no "clear this
 *   notification" action; clearing is something the listener does. So it is
 *   synthesised here, and marked [GarminNotificationAction.isSynthetic] so the
 *   layer that performs it knows to cancel rather than fire an intent.
 *
 * Kept out of the protocol file on purpose: that one is pure bytes and knows
 * nothing about Android's notification shapes, which is what lets the whole
 * GNCS conversation be tested with no platform at all.
 *
 * Port of the Flutter build's `garmin_notification_actions.dart`.
 */

/**
 * The most custom actions GNCS can carry. Beyond this the watch has no slot to
 * draw them in.
 */
const val MAX_GARMIN_CUSTOM_ACTIONS = 5

/**
 * The actions to offer on the wrist for [message], in the order the watch will
 * draw them.
 *
 * Dismiss comes first because it is the one every notification has and the one
 * the wearer reaches for most; the app's own buttons follow in its own order.
 */
fun garminActionsFor(message: NotificationMsg): List<GarminNotificationAction> {
    val actions = mutableListOf<GarminNotificationAction>()

    if (message.dismissable) {
        actions.add(
            GarminNotificationAction(
                kind = GarminNotificationActionKind.DISMISS,
                // Never shown: the watch draws dismiss as an icon in a fixed
                // position and ignores the label. Named anyway so a log line
                // reads sensibly.
                label = "Dismiss",
                androidIndex = -1,
            ),
        )
    }

    // At most ONE reply. The watch has a single reply control, so a second
    // would silently overwrite the first — better to offer the app's own first
    // reply and let the rest fall through to custom slots.
    var replyTaken = false
    var customSlot = 0

    for (action in message.actions) {
        if (!action.fireableFromBackground) {
            // An action that opens the posting app's own screen. Android blocks
            // a background activity launch, so invoking it from the wrist does
            // nothing at all — no error, no effect. Offering it would put back
            // exactly the dead button this feature was built to remove. A stock
            // SMS app's "Reply" is one of these: it prefills a compose screen
            // rather than sending.
            continue
        }
        if (action.isReply && !replyTaken) {
            replyTaken = true
            actions.add(
                GarminNotificationAction(
                    kind = GarminNotificationActionKind.REPLY,
                    label = action.title,
                    androidIndex = action.index,
                    isReply = true,
                ),
            )
            continue
        }
        if (customSlot >= MAX_GARMIN_CUSTOM_ACTIONS) {
            // Dropped rather than crammed in: the slots ARE the wire format,
            // and reusing one would make two buttons invoke the same thing.
            continue
        }
        actions.add(
            GarminNotificationAction(
                kind = GarminNotificationActionKind.customSlots[customSlot],
                label = action.title,
                androidIndex = action.index,
                // A second reply loses its text box and becomes a plain button.
                // It still fires, which is better than not offering it — most
                // apps treat an empty reply as a no-op rather than sending a
                // blank message.
                isReply = false,
            ),
        )
        customSlot++
    }

    return actions
}
