package tech.mmarca.openvitals.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R

@Composable
fun AppLockGate(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val unlockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        unlocked = result.resultCode == Activity.RESULT_OK
    }

    LaunchedEffect(enabled) {
        if (!unlocked) {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isDeviceSecure) {
                unlocked = true
            } else {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    context.getString(R.string.settings_app_lock_title),
                    context.getString(R.string.settings_app_lock_body),
                )
                if (intent != null) {
                    unlockLauncher.launch(intent)
                } else {
                    unlocked = true
                }
            }
        }
    }

    if (unlocked) {
        content()
    } else {
        Box(modifier = modifier.fillMaxSize())
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
