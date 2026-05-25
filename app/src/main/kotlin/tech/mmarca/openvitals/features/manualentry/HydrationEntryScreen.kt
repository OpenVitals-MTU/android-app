package tech.mmarca.openvitals.features.manualentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

@Composable
fun HydrationEntryScreen(
    viewModel: HydrationEntryViewModel,
    unitFormatter: UnitFormatter,
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
            )

            Button(
                onClick = onAddSelectedEntry,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        R.string.hydration_add_selected,
                        hydrationAmountLabel(state.selectedContainerEffectiveLiters, unitFormatter),
                    ),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            Text(
                text = stringResource(
                    R.string.hydration_bhi_write_explanation,
                    hydrationAmountLabel(state.selectedContainer.volumeLiters, unitFormatter),
                    hydrationAmountLabel(state.selectedContainerEffectiveLiters, unitFormatter),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun HydrationTodayCounter(
    liters: Double,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val todayHydration = unitFormatter.hydration(liters)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.period_today),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = todayHydration.text,
                style = MaterialTheme.typography.titleMedium,
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
            contentPadding = PaddingValues(horizontal = 2.dp),
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
    modifier: Modifier = Modifier,
) {
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
            contentPadding = PaddingValues(horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(options, key = { it.id }) { option ->
                HydrationContainerOptionItem(
                    option = option,
                    selected = option == selectedContainer,
                    unitFormatter = unitFormatter,
                    enabled = !isSavingEntry,
                    onSelect = { onSelectContainer(option) },
                )
            }
        }
    }
}

@Composable
private fun HydrationContainerOptionItem(
    option: HydrationContainerOption,
    selected: Boolean,
    unitFormatter: UnitFormatter,
    enabled: Boolean,
    onSelect: () -> Unit,
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
            .height(112.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onSelect,
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
            Icon(
                imageVector = option.icon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
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

private fun HydrationContainerOption.icon(): ImageVector = when (id) {
    "coffee_cup",
    "tea_cup" -> Icons.Outlined.LocalCafe
    else -> Icons.Outlined.LocalDrink
}

private fun hydrationAmountLabel(liters: Double, unitFormatter: UnitFormatter): String =
    if (unitFormatter.unitSystem() == UnitSystem.METRIC && liters < 1.0) {
        "${unitFormatter.count((liters * 1000.0).roundToInt())} ml"
    } else {
        unitFormatter.hydration(liters).text
    }
