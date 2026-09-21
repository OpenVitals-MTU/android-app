package tech.mmarca.openvitals.core.presentation

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import tech.mmarca.openvitals.R

sealed interface ScreenError {
    /** Text from outside the app, such as an exception message. Shown as it is. */
    data class Message(val text: String) : ScreenError

    /** A sentence of ours. It is translated when it is shown. */
    data class Text(@param:StringRes val messageRes: Int) : ScreenError
    data object NotFound : ScreenError
    data object MissingArgument : ScreenError
    data object PermissionDenied : ScreenError
    data object HealthConnectUnavailable : ScreenError
}

/**
 * The one rule for "this failed because a permission is missing": a
 * [SecurityException] anywhere in the cause chain.
 */
fun Throwable.isPermissionFailure(): Boolean =
    generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }
        .any { it is SecurityException }

fun Throwable.toScreenError(
    @StringRes fallback: Int = R.string.screen_error_generic,
    logTag: String = ScreenErrorLogTag,
    logMessage: String = "Showing throwable as screen error",
): ScreenError = ScreenErrorHandler.handle(
    throwable = this,
    context = ScreenErrorContext(
        fallback = fallback,
        logTag = logTag,
        logMessage = logMessage,
    ),
)

fun <T> Result<T>.onScreenError(
    @StringRes fallback: Int = R.string.screen_error_generic,
    logTag: String = ScreenErrorLogTag,
    logMessage: String = "Showing throwable as screen error",
    onError: (ScreenError) -> Unit,
): Result<T> = onFailure { throwable ->
    onError(
        throwable.toScreenError(
            fallback = fallback,
            logTag = logTag,
            logMessage = logMessage,
        )
    )
}

@Composable
fun ScreenError?.resolve(): String? = when (this) {
    null -> null
    is ScreenError.Message -> text
    is ScreenError.Text -> stringResource(messageRes)
    ScreenError.NotFound -> stringResource(R.string.screen_error_not_found)
    ScreenError.MissingArgument -> stringResource(R.string.screen_error_missing_argument)
    ScreenError.PermissionDenied -> stringResource(R.string.screen_error_permission_denied)
    ScreenError.HealthConnectUnavailable -> stringResource(R.string.screen_error_health_connect_unavailable)
}

data class ScreenErrorContext(
    /** Shown when the throwable carries no message of its own. */
    @param:StringRes val fallback: Int = R.string.screen_error_generic,
    val logTag: String = ScreenErrorLogTag,
    val logMessage: String = "Showing throwable as screen error",
)

object ScreenErrorHandler {
    var sink: ((String, String, Throwable) -> Unit)? = null

    fun handle(throwable: Throwable, context: ScreenErrorContext = ScreenErrorContext()): ScreenError {
        if (throwable is CancellationException) throw throwable
        warn(context.logTag, context.logMessage, throwable)
        // A missing permission stays a type: the screens turn it into a grant affordance.
        if (throwable.isPermissionFailure()) return ScreenError.PermissionDenied
        return throwable.message
            ?.takeIf { it.isNotBlank() }
            ?.let(ScreenError::Message)
            ?: ScreenError.Text(context.fallback)
    }

    fun warn(tag: String, message: String, throwable: Throwable) {
        sink?.invoke(tag, message, throwable) ?: runCatching {
            Log.w(tag, message, throwable)
        }
    }
}

private const val ScreenErrorLogTag = "ScreenError"
