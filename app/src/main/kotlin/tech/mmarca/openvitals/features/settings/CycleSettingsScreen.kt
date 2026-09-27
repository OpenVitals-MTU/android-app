package tech.mmarca.openvitals.features.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Cycle: the declared contexts, the age band, the reminders, and what the estimates are. */
@Composable
fun CycleSettingsScreen(
    viewModel: CycleSettingsViewModel,
    dateTimeFormatterProvider: DateTimeFormatterProvider = DateTimeFormatterProvider(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val gutter = Modifier.padding(horizontal = LayoutMetrics.screenGutter)
    SettingsSectionList {
        item { SectionHeader(stringResource(SettingsSection.CYCLE.titleRes)) }
        item {
            CycleContextsCard(
                profile = state.profile,
                onToggleContext = viewModel::setContext,
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item {
            CycleAgeBandCard(
                selected = state.profile.ageBand,
                derived = state.derivedAgeBand,
                onSelect = viewModel::setAgeBand,
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item {
            CycleRemindersCard(
                state = state,
                viewModel = viewModel,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onRequestNotificationPermission = {
                    viewModel.requestEnableAfterPermission()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onNotificationPermissionResult(granted = true)
                    }
                },
                modifier = gutter,
            )
        }
        item { SettingsCardSpacer() }
        item { CycleAboutEstimatesCard(modifier = gutter) }
        item { SettingsCardSpacer() }
        item {
            CycleDeleteJournalCard(
                isDeleting = state.isDeletingJournal,
                onDelete = viewModel::deleteCycleJournal,
                modifier = gutter,
            )
        }
    }
}
