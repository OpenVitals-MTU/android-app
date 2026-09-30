package tech.mmarca.openvitals.features.medical

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.PermissionCallout

/** What a medical screen can do about permissions it lacks. */
internal sealed interface MedicalAccessAction {
    data object None : MedicalAccessAction

    data class Ask(val permissions: Set<String>) : MedicalAccessAction

    /** Nothing left to ask: Health Connect's settings can still turn access on. */
    data object OpenSettings : MedicalAccessAction
}

/**
 * Health Connect closes any request that holds a permission the user refused twice, so a
 * re-ask leaves those out. Before the area's first request, everything missing is asked.
 * [canAskAgain] is Android's rationale check, which is false for a permission refused twice.
 */
internal fun medicalAccessAction(
    missing: Set<String>,
    firstRequestDone: Boolean,
    canAskAgain: (String) -> Boolean,
): MedicalAccessAction {
    if (missing.isEmpty()) return MedicalAccessAction.None
    if (!firstRequestDone) return MedicalAccessAction.Ask(missing)
    val askable = missing.filterTo(mutableSetOf(), canAskAgain)
    return if (askable.isEmpty()) MedicalAccessAction.OpenSettings else MedicalAccessAction.Ask(askable)
}

/** Checked again each time the screen resumes: a request, or Health Connect's settings, can change the answer. */
@Composable
internal fun rememberMedicalAccessAction(missing: Set<String>, firstRequestDone: Boolean): MedicalAccessAction {
    val activity = LocalContext.current.findActivity()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    return remember(missing, firstRequestDone, resumes) {
        medicalAccessAction(missing, firstRequestDone) { permission ->
            activity == null || ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
}

/** Asks for what can still be asked, or sends the user to Health Connect's settings. */
@Composable
internal fun MedicalAccessCallout(
    action: MedicalAccessAction,
    onAsk: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.health_connect_promote_medical_records_title)
    when (action) {
        MedicalAccessAction.None -> Unit
        is MedicalAccessAction.Ask -> PermissionCallout(
            title = title,
            body = stringResource(R.string.health_connect_promote_medical_records_body),
            onGrant = { onAsk(action.permissions) },
            modifier = modifier,
        )
        MedicalAccessAction.OpenSettings -> PermissionCallout(
            title = title,
            body = stringResource(R.string.medical_access_blocked),
            onGrant = onOpenSettings,
            actionLabel = stringResource(R.string.medical_record_open_health_connect),
            modifier = modifier,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
