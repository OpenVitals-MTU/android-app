package tech.mmarca.openvitals.features.manualentry

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.PermissionCallout

/** The one way an entry form asks for write access: the same callout the rest of the app uses. */
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
