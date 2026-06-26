package tech.mmarca.openvitals.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dagger.hilt.android.EntryPointAccessors
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectUiEntryPoint

@Composable
fun PrivacyReconsentDialog(
    onDismissRequest: () -> Unit,
    onReviewPolicy: () -> Unit,
    onAccept: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.privacy_reconsent_title)) },
        text = { Text(stringResource(R.string.privacy_reconsent_body)) },
        confirmButton = {
            TextButton(onClick = onReviewPolicy) {
                Text(stringResource(R.string.privacy_reconsent_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.action_accept))
            }
        },
    )
}

@Composable
fun PrivacyReconsentPrompt() {
    val context = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            HealthConnectUiEntryPoint::class.java,
        ).preferencesRepository()
    }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var resumeTick by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTick++
    }

    LaunchedEffect(resumeTick) {
        val accepted = prefs.acceptedPrivacyPolicyVersion
        showDialog = prefs.onboardingDone &&
            accepted != PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION
    }

    if (showDialog) {
        PrivacyReconsentDialog(
            onDismissRequest = { showDialog = false },
            onReviewPolicy = {
                val url = context.getString(R.string.settings_privacy_policy_url)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            onAccept = {
                prefs.acceptedPrivacyPolicyVersion = PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION
                prefs.privacyPolicyAcceptedAtMillis = System.currentTimeMillis()
                showDialog = false
            },
        )
    }
}
