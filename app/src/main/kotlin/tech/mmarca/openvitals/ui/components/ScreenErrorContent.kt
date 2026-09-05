package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings

/** The grant request the Health Connect shell owns. Null outside a shell; the callout then opens settings. */
class HealthConnectGrantAccess(
    val permissions: Set<String>,
    val onGrant: (Set<String>) -> Unit,
)

val LocalHealthConnectGrantAccess = compositionLocalOf<HealthConnectGrantAccess?> { null }

/** Renders a [ScreenError]. A missing permission gets the [PermissionCallout], not red text. */
@Composable
fun ScreenErrorContent(
    screenError: ScreenError?,
    modifier: Modifier = Modifier,
    fallbackMessage: String? = null,
) {
    if (screenError == ScreenError.PermissionDenied) {
        HealthConnectPermissionDeniedCallout(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        return
    }
    val message = screenError.resolve() ?: fallbackMessage ?: return
    ErrorMessage(message = message, modifier = modifier)
}

@Composable
fun HealthConnectPermissionDeniedCallout(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val grantAccess = LocalHealthConnectGrantAccess.current
    PermissionCallout(
        title = stringResource(R.string.message_missing_permissions_title),
        body = stringResource(R.string.screen_error_permission_denied),
        onGrant = {
            val permissions = grantAccess?.permissions.orEmpty()
            if (grantAccess != null && permissions.isNotEmpty()) {
                grantAccess.onGrant(permissions)
            } else {
                openHealthConnectPermissionSettings(context)
            }
        },
        modifier = modifier,
    )
}
