package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.devices.notifications.NotificationMsg

/**
 * Turns a notification's Android actions into the ones a watch can offer.
 * GNCS has fixed slots: a reply, a dismiss and five custom ones. Dismiss is
 * synthesised here and marked [GarminNotificationAction.isSynthetic].
 * Kept out of the protocol file, which knows nothing of Android.
 */

/** The most custom actions GNCS can carry. */
const val MAX_GARMIN_CUSTOM_ACTIONS = 5

/** The actions to offer for [message], in draw order. Dismiss first, then the app's own. */
fun garminActionsFor(message: NotificationMsg): List<GarminNotificationAction> {
    val actions = mutableListOf<GarminNotificationAction>()

    if (message.dismissable) {
        actions.add(
            GarminNotificationAction(
                kind = GarminNotificationActionKind.DISMISS,
                // Never shown: the watch draws dismiss as an icon. Named for the log.
                label = "Dismiss",
                androidIndex = -1,
            ),
        )
    }

    // At most one reply: the watch has a single reply control.
    var replyTaken = false
    var customSlot = 0

    for (action in message.actions) {
        if (!action.fireableFromBackground) {
            // An activity intent does nothing from the wrist: a dead button.
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
            // Dropped rather than crammed in: the slots are the wire format.
            continue
        }
        actions.add(
            GarminNotificationAction(
                kind = GarminNotificationActionKind.customSlots[customSlot],
                label = action.title,
                androidIndex = action.index,
                // A second reply becomes a plain button. Most apps treat an empty reply as a no-op.
                isReply = false,
            ),
        )
        customSlot++
    }

    return actions
}
