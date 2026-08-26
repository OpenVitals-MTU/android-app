package tech.mmarca.openvitals.features.workoutplans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

@Composable
fun WorkoutPlanListScreen(
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenBuilder: (String?) -> Unit,
    onStartPlan: (String) -> Unit,
    onLogPlan: (String) -> Unit = onStartPlan,
    viewModel: WorkoutPlanListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.workout_plan_deleted)
    val copiedText = stringResource(R.string.workout_plan_copied_to_today)
    val failedText = stringResource(R.string.workout_plan_action_failed)
    val exportedText = stringResource(R.string.workout_plans_exported)
    val importedText = stringResource(R.string.workout_plans_imported, state.importedCount)
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(WorkoutPlanExport.MimeType),
    ) { uri ->
        val text = viewModel.exportJson()
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }.onSuccess { viewModel.onExported() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()?.let(viewModel::importJson)
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.pendingStartPlanId) {
        val planId = state.pendingStartPlanId ?: return@LaunchedEffect
        viewModel.onStartPlanHandled()
        onStartPlan(planId)
    }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        viewModel.consumeMessage()
        snackbarHostState.showSnackbar(
            when (message) {
                WorkoutPlanListMessage.DELETED -> deletedText
                WorkoutPlanListMessage.COPIED_TO_TODAY -> copiedText
                WorkoutPlanListMessage.EXPORTED -> exportedText
                WorkoutPlanListMessage.IMPORTED -> importedText
                WorkoutPlanListMessage.ACTION_FAILED -> failedText
            },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        WithHealthConnectFeatureScreen(
            feature = HealthConnectFeature.WORKOUT_PLANS,
            isLoading = state.isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { _ ->
            WorkoutPlanListContent(
                state = state,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onNewPlan = { onOpenBuilder(null) },
                onEditPlan = { onOpenBuilder(it) },
                onStartPlan = onStartPlan,
                onLogPlan = onLogPlan,
                onCopyToToday = viewModel::copyToToday,
                onRepeat = viewModel::repeatPlan,
                onRequestDelete = viewModel::requestDelete,
                onExport = { exportLauncher.launch(WorkoutPlanExport.FileName) },
                onImport = { importLauncher.launch(arrayOf(WorkoutPlanExport.MimeType, "text/plain", "*/*")) },
            )
        }
    }

    state.pendingDeleteId?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.workout_plan_delete_confirm_title)) },
            text = { Text(stringResource(R.string.workout_plan_delete_confirm_body)) },
            confirmButton = {
                OpenVitalsTextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OpenVitalsTextButton(onClick = viewModel::cancelDelete) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun WorkoutPlanListContent(
    state: WorkoutPlanListUiState,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onNewPlan: () -> Unit,
    onEditPlan: (String) -> Unit,
    onStartPlan: (String) -> Unit,
    onLogPlan: (String) -> Unit,
    onCopyToToday: (String) -> Unit,
    onRepeat: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    if (state.isLoading && state.items.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }
        return
    }
    if (!state.isAvailable) {
        Text(
            text = stringResource(R.string.workout_plan_unavailable_on_device),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.lg),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.sm),
    ) {
        state.error?.let { error ->
            item(key = "error") { ScreenErrorContent(screenError = error) }
        }
        item(key = "new") {
            OpenVitalsButton(
                onClick = onNewPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.workout_plans_new),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
        if (state.items.isEmpty()) {
            item(key = "empty") {
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xxl)) {
                    Text(
                        text = stringResource(R.string.workout_plans_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.workout_plans_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }
        item(key = "transfer") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OpenVitalsTextButton(onClick = onExport, enabled = state.items.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.workout_plans_export))
                }
                OpenVitalsTextButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.workout_plans_import))
                }
            }
        }
        WorkoutPlanGroup.entries.forEach { group ->
            val items = state.items(group)
            if (items.isEmpty()) return@forEach
            item(key = "group-${group.name}") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        text = stringResource(
                            when (group) {
                                WorkoutPlanGroup.TODAY -> R.string.workout_plans_group_today
                                WorkoutPlanGroup.UPCOMING -> R.string.workout_plans_group_upcoming
                                WorkoutPlanGroup.PAST -> R.string.workout_plans_group_past
                            },
                        ),
                    )
                    OpenVitalsCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                        val sorted = if (group == WorkoutPlanGroup.PAST) {
                            items.sortedByDescending { it.plan.startTime }
                        } else {
                            items.sortedBy { it.plan.startTime }
                        }
                        sorted.forEachIndexed { index, item ->
                            WorkoutPlanListRow(
                                item = item,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onEdit = { onEditPlan(item.plan.id) },
                                onStart = { onStartPlan(item.plan.id) },
                                onLog = { onLogPlan(item.plan.id) },
                                onCopyToToday = { onCopyToToday(item.plan.id) },
                                onRepeat = { onRepeat(item.plan.id) },
                                onDelete = { onRequestDelete(item.plan.id) },
                            )
                            if (index < sorted.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = WorkoutPlanRowDividerInset),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Emphasis.fill),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutPlanListRow(
    item: WorkoutPlanListItem,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onLog: () -> Unit,
    onCopyToToday: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val primaryAction: (() -> Unit)? = when {
        item.canEdit -> onEdit
        item.canStart -> onStart
        else -> null
    }
    WorkoutPlanRow(
        plan = item.plan,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onClick = primaryAction,
    ) {
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.workout_plan_more_actions))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (item.canStart) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_start)) },
                        onClick = {
                            menuOpen = false
                            onStart()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.activity_entry_hub_log_from_plan)) },
                        onClick = {
                            menuOpen = false
                            onLog()
                        },
                    )
                }
                if (item.canEdit) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                }
                if (item.isCompleted && item.plan.isGuidedRunnable()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.workout_plan_action_repeat)) },
                        onClick = {
                            menuOpen = false
                            onRepeat()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.workout_plan_action_copy_to_today)) },
                    onClick = {
                        menuOpen = false
                        onCopyToToday()
                    },
                )
                if (item.canDelete) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
