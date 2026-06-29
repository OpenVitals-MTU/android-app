package tech.mmarca.openvitals.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R

sealed interface ScreenError {
    data class Message(val text: String) : ScreenError
    data object NotFound : ScreenError
    data object MissingArgument : ScreenError
    data object PermissionDenied : ScreenError
    data object HealthConnectUnavailable : ScreenError
}

fun Throwable.toScreenError(fallback: String = "Unable to complete the request."): ScreenError =
    message?.takeIf { it.isNotBlank() }?.let(ScreenError::Message) ?: ScreenError.Message(fallback)

@Composable
fun ScreenError?.resolve(): String? = when (this) {
    null -> null
    is ScreenError.Message -> text
    ScreenError.NotFound -> stringResource(R.string.screen_error_not_found)
    ScreenError.MissingArgument -> stringResource(R.string.screen_error_missing_argument)
    ScreenError.PermissionDenied -> stringResource(R.string.screen_error_permission_denied)
    ScreenError.HealthConnectUnavailable -> stringResource(R.string.screen_error_health_connect_unavailable)
}
