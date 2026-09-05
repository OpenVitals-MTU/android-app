package tech.mmarca.openvitals.features.manualentry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.PermissionCallout

/**
 * The one way an entry form asks for its Health Connect write access: the same
 * lock-icon callout the rest of the app shows for missing permissions, headed
 * "Some permissions are missing" with the entry's own explanation as the body.
 *
 * The forms used to swap their subtitle for that explanation and tuck an
 * outlined "Grant" beside the title, which read as a second, weaker style next
 * to the callout the workout plans and settings screens already use.
 */
@Composable
internal fun ManualEntryWritePermissionCallout(
    body: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionCallout(
        title = stringResource(R.string.message_missing_permissions_title),
        body = body,
        onGrant = onGrant,
        modifier = modifier,
    )
}
