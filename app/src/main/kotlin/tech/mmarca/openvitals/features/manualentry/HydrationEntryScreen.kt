package tech.mmarca.openvitals.features.manualentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.preferences.UnitSystem
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.theme.HydrationColor

private val HydrationContainerIconSlotSize = 30.dp

@Composable
fun HydrationEntryScreen(
    viewModel: HydrationEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }

    LazyColumn {
        item {
            HydrationTrackerCard(
                state = state,
                unitFormatter = unitFormatter,
                onSelectBeverage = viewModel::selectBeverage,
                onSelectContainer = viewModel::selectContainer,
                onAddSelectedEntry = viewModel::addSelectedHydrationEntry,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.hydrationWritePermissions)
                },
                onUpdateContainerSize = viewModel::updateContainerSize,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HydrationTrackerCard(
    state: HydrationEntryUiState,
    unitFormatter: UnitFormatter,
    onSelectBeverage: (HydrationBeverage) -> Unit,
    onSelectContainer: (HydrationContainerOption) -> Unit,
    onAddSelectedEntry: () -> Unit,
    onRequestWritePermission: () -> Unit,
    onUpdateContainerSize: (HydrationContainerOption, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.canWriteHydration && !state.isSavingEntry && !state.isCheckingPermission
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalDrink,
                    contentDescription = null,
                    tint = HydrationColor,
                    modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.hydration_tracker_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (state.canWriteHydration) {
                                R.string.hydration_tracker_subtitle
                            } else {
                                R.string.hydration_tracker_permission_needed
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!state.canWriteHydration && !state.isCheckingPermission) {
                    OutlinedButton(onClick = onRequestWritePermission) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            }

            HydrationTodayCounter(
                liters = state.todayHydrationLiters,
                dailyGoalLiters = state.dailyGoalLiters,
                unitFormatter = unitFormatter,
            )

            HydrationBeverageCarousel(
                beverages = state.beverageOptions,
                selectedBeverage = state.selectedBeverage,
                isSavingEntry = state.isSavingEntry,
                onSelectBeverage = onSelectBeverage,
            )

            HydrationContainerCarousel(
                options = state.containerOptions,
                selectedContainer = state.selectedContainer,
                unitFormatter = unitFormatter,
                isSavingEntry = state.isSavingEntry,
                onSelectContainer = onSelectContainer,
                onUpdateContainerSize = onUpdateContainerSize,
            )

            Button(
                onClick = onAddSelectedEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (state.isEditMode) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (state.isEditMode) {
                        stringResource(R.string.action_save)
                    } else {
                        stringResource(
                            R.string.hydration_add_selected,
                            hydrationAmountLabel(state.selectedContainerEffectiveLiters, unitFormatter),
                        )
                    },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            HydrationWriteInfo(
                originalAmount = hydrationAmountLabel(state.selectedContainer.volumeLiters, unitFormatter),
                effectiveAmount = hydrationAmountLabel(state.selectedContainerEffectiveLiters, unitFormatter),
            )

            state.entryError?.let { entryError ->
                Text(
                    text = hydrationEntryErrorText(entryError, state.writeErrorMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HydrationTodayCounter(
    liters: Double,
    dailyGoalLiters: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val todayHydration = unitFormatter.hydration(liters)
    val dailyGoal = unitFormatter.hydration(dailyGoalLiters)
    val targetProgress = if (dailyGoalLiters > 0.0) {
        (liters / dailyGoalLiters).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 650),
        label = "HydrationTodayGoalProgress",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progressColor = HydrationColor.copy(alpha = 0.86f)
    val strokeWidth = with(LocalDensity.current) { 5.dp.toPx() }
    val progressStroke = remember(strokeWidth) {
        Stroke(width = strokeWidth, cap = StrokeCap.Round)
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.period_today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${todayHydration.text} / ${dailyGoal.text}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                color = progressColor,
                trackColor = trackColor,
                stroke = progressStroke,
                trackStroke = progressStroke,
                wavelength = 34.dp,
                waveSpeed = 34.dp,
            )
        }
    }
}

@Composable
private fun HydrationBeverageCarousel(
    beverages: List<HydrationBeverage>,
    selectedBeverage: HydrationBeverage,
    isSavingEntry: Boolean,
    onSelectBeverage: (HydrationBeverage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.hydration_drink_type),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(beverages, key = { it.name }) { beverage ->
                FilterChip(
                    selected = beverage == selectedBeverage,
                    onClick = { onSelectBeverage(beverage) },
                    label = { Text(stringResource(beverage.labelRes())) },
                    leadingIcon = {
                        Icon(
                            imageVector = beverage.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    enabled = !isSavingEntry,
                )
            }
        }
    }
}

@Composable
private fun HydrationContainerCarousel(
    options: List<HydrationContainerOption>,
    selectedContainer: HydrationContainerOption,
    unitFormatter: UnitFormatter,
    isSavingEntry: Boolean,
    onSelectContainer: (HydrationContainerOption) -> Unit,
    onUpdateContainerSize: (HydrationContainerOption, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingContainer by remember { mutableStateOf<HydrationContainerOption?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.hydration_container_size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(options, key = { it.id }) { option ->
                HydrationContainerOptionItem(
                    option = option,
                    selected = option == selectedContainer,
                    unitFormatter = unitFormatter,
                    enabled = !isSavingEntry,
                    onSelect = { onSelectContainer(option) },
                    onEdit = { editingContainer = option },
                )
            }
        }
    }

    editingContainer?.let { option ->
        HydrationContainerSizeDialog(
            option = option,
            onDismiss = { editingContainer = null },
            onSave = { milliliters ->
                onUpdateContainerSize(option, milliliters)
                editingContainer = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HydrationContainerOptionItem(
    option: HydrationContainerOption,
    selected: Boolean,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .width(132.dp)
            .height(132.dp)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onSelect,
                onLongClick = onEdit,
            ),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(HydrationContainerIconSlotSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = option.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(option.iconSize()),
                )
            }
            Text(
                text = stringResource(option.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            Text(
                text = hydrationAmountLabel(option.volumeLiters, unitFormatter),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HydrationContainerSizeDialog(
    option: HydrationContainerOption,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amountText by remember(option) {
        mutableStateOf(option.volumeMilliliters.roundToInt().toString())
    }
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val isAmountValid = amount?.let(::isValidHydrationContainerMilliliters) == true
    val showError = amountText.isNotBlank() && !isAmountValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.hydration_container_edit_title))
        },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.hydration_container_amount_ml)) },
                isError = showError,
                supportingText = if (showError) {
                    {
                        Text(stringResource(R.string.hydration_container_invalid_amount))
                    }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount?.takeIf(::isValidHydrationContainerMilliliters)?.let(onSave)
                },
                enabled = isAmountValid,
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun HydrationWriteInfo(
    originalAmount: String,
    effectiveAmount: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.action_close else R.string.action_details))
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(
                        R.string.hydration_bhi_write_explanation,
                        originalAmount,
                        effectiveAmount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun hydrationEntryErrorText(
    error: HydrationEntryError,
    message: String?,
): String = when (error) {
    HydrationEntryError.INVALID_AMOUNT -> stringResource(R.string.hydration_invalid_amount)
    HydrationEntryError.MISSING_WRITE_PERMISSION -> stringResource(R.string.hydration_tracker_permission_needed)
    HydrationEntryError.WRITE_FAILED -> stringResource(
        R.string.hydration_write_failed,
        message ?: stringResource(R.string.unknown_error),
    )
}

private fun HydrationBeverage.labelRes(): Int = when (this) {
    HydrationBeverage.WATER -> R.string.hydration_beverage_water
    HydrationBeverage.COFFEE -> R.string.hydration_beverage_coffee
    HydrationBeverage.TEA -> R.string.hydration_beverage_tea
    HydrationBeverage.SOFT_DRINK -> R.string.hydration_beverage_soft_drink
    HydrationBeverage.ENERGY_DRINK -> R.string.hydration_beverage_energy_drink
    HydrationBeverage.SPORTS_DRINK -> R.string.hydration_beverage_sports_drink
    HydrationBeverage.ORAL_REHYDRATION_SOLUTION -> R.string.hydration_beverage_ors
    HydrationBeverage.MILK -> R.string.hydration_beverage_milk
    HydrationBeverage.FRUIT_JUICE -> R.string.hydration_beverage_fruit_juice
}

private fun HydrationBeverage.icon(): ImageVector = when (this) {
    HydrationBeverage.COFFEE,
    HydrationBeverage.TEA -> Icons.Outlined.LocalCafe
    else -> Icons.Outlined.LocalDrink
}

private fun HydrationContainerOption.labelRes(): Int = when (id) {
    "coffee_cup" -> R.string.hydration_container_coffee_cup
    "tea_cup" -> R.string.hydration_container_tea_cup
    "small_cup" -> R.string.hydration_container_small_cup
    "medium_glass" -> R.string.hydration_container_medium_glass
    "large_glass" -> R.string.hydration_container_large_glass
    "water_bottle" -> R.string.hydration_container_water_bottle
    "large_bottle" -> R.string.hydration_container_large_bottle
    else -> R.string.hydration_container_custom
}

private fun HydrationContainerOption.icon(): ImageVector =
    if (id == "coffee_cup" || id == "tea_cup" || volumeMilliliters <= 180.0) {
        Icons.Outlined.LocalCafe
    } else {
        Icons.Outlined.LocalDrink
    }

private fun HydrationContainerOption.iconSize() = when {
    volumeMilliliters <= 180.0 -> 22.dp
    volumeMilliliters < 500.0 -> 26.dp
    else -> 30.dp
}

private fun hydrationAmountLabel(liters: Double, unitFormatter: UnitFormatter): String =
    if (unitFormatter.unitSystem() == UnitSystem.METRIC && liters < 1.0) {
        "${unitFormatter.count((liters * 1000.0).roundToInt())} ml"
    } else {
        unitFormatter.hydration(liters).text
    }
