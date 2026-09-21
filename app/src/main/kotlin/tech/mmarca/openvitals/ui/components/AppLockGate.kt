package tech.mmarca.openvitals.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Asks for the device credential before the app shows anything, and again after
 * [AppLockState.RELOCK_AFTER_MILLIS] in the background.
 *
 * A relock covers [content] instead of removing it. Removing it would drop the
 * navigation stack and the result of a file picker the user is coming back from.
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val state = viewModel<AppLockViewModel>().state

    // The recent-apps thumbnail is taken while the app is still unlocked.
    DisposableEffect(context, enabled) {
        val activity = context.findActivity()
        activity?.hideFromRecents(enabled)
        onDispose { activity?.hideFromRecents(false) }
    }

    if (enabled) {
        val appLockTitle = stringResource(R.string.settings_app_lock_title)
        val appLockBody = stringResource(R.string.settings_app_lock_body)
        val unlockLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            state.onPromptResult(confirmed = result.resultCode == Activity.RESULT_OK)
        }
        LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.onStop(SystemClock.elapsedRealtime()) }
        LifecycleEventEffect(Lifecycle.Event.ON_START) { state.onStart(SystemClock.elapsedRealtime()) }

        LaunchedEffect(state.locked, state.promptPending) {
            if (!state.locked || !state.promptPending) return@LaunchedEffect
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val intent = if (keyguardManager.isDeviceSecure) {
                keyguardManager.createConfirmDeviceCredentialIntent(appLockTitle, appLockBody)
            } else {
                null
            }
            if (intent == null) {
                state.unlock()
            } else {
                state.onPromptLaunched()
                unlockLauncher.launch(intent)
            }
        }
    }

    val locked = enabled && state.locked
    Box(modifier = modifier.fillMaxSize()) {
        if (!enabled || state.everUnlocked) {
            // Hidden from screen readers too while the lock screen covers it.
            Box(modifier = if (locked) Modifier.clearAndSetSemantics { } else Modifier) {
                content()
            }
        }
        if (locked) {
            AppLockedScreen(onUnlock = state::requestPrompt)
            // Back must not reach the screens under the lock.
            BackHandler { context.findActivity()?.moveTaskToBack(true) }
        }
    }
}

/** What the user sees after they cancel the prompt. It used to be a blank screen with no way on. */
@Composable
internal fun AppLockedScreen(onUnlock: () -> Unit) {
    // A Surface also stops touches from reaching what it covers.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(LockIconSize),
            )
            Text(
                text = stringResource(R.string.app_lock_locked_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OpenVitalsFilledButton(onClick = onUnlock) {
                Text(stringResource(R.string.app_lock_unlock_action))
            }
        }
    }
}

private val LockIconSize = 56.dp

/**
 * Android 13 and later can blank the thumbnail alone. Before that only FLAG_SECURE does it,
 * which also blocks screenshots while the lock is on.
 */
private fun Activity.hideFromRecents(hide: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        setRecentsScreenshotEnabled(!hide)
    } else if (hide) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
