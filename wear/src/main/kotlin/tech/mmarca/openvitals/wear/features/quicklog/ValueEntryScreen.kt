package tech.mmarca.openvitals.wear.features.quicklog

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.Text
import kotlin.math.roundToInt
import kotlin.math.sign
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatOneDecimal
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme

private const val PoundsPerKilogram = 2.20462

/**
 * One number picked with the stepper buttons or the crown. [range] is in
 * whole steps, so a weight in tenths of a kilogram counts 0.1 kg per step.
 */
@Composable
fun ValueStepper(
    title: String,
    unit: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    format: (Int) -> String,
    onConfirm: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentValue by rememberUpdatedState(value)
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    val step = event.verticalScrollPixels.sign.toInt()
                    if (step != 0) onValueChange((currentValue + step).coerceIn(range))
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            Stepper(
                value = value,
                onValueChange = onValueChange,
                valueProgression = range,
                decreaseIcon = { Icon(Icons.Outlined.Remove, null) },
                increaseIcon = { Icon(Icons.Outlined.Add, null) },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MetricValueRow(
                        value = format(value),
                        unit = unit,
                        valueStyle = MaterialTheme.typography.displaySmall,
                    )
                    FilledIconButton(onClick = onConfirm) {
                        Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}

/** Weight in the unit system's unit, 0.1 per step, saved in kilograms. */
@Composable
fun WeightEntryScreen(
    initialKg: Double,
    unitSystem: UnitSystem,
    onSave: (kilograms: Double) -> Unit,
) {
    val perKg = if (unitSystem == UnitSystem.IMPERIAL) PoundsPerKilogram else 1.0
    var tenths by rememberSaveable { mutableIntStateOf((initialKg * perKg * 10).roundToInt()) }
    ValueStepper(
        title = stringResource(R.string.metric_weight),
        unit = stringResource(if (unitSystem == UnitSystem.IMPERIAL) R.string.unit_lb else R.string.unit_kg),
        value = tenths,
        onValueChange = { tenths = it },
        range = (WeightRangeKg.first * perKg * 10).roundToInt()..(WeightRangeKg.last * perKg * 10).roundToInt(),
        format = { formatOneDecimal(it / 10.0) },
        onConfirm = { onSave(tenths / 10.0 / perKg) },
    )
}

/** Systolic first, then diastolic, as a cuff shows them. */
@Composable
fun BloodPressureEntryScreen(
    initialSystolic: Int,
    initialDiastolic: Int,
    onSave: (systolic: Int, diastolic: Int) -> Unit,
) {
    var systolic by rememberSaveable { mutableIntStateOf(initialSystolic) }
    var diastolic by rememberSaveable { mutableIntStateOf(initialDiastolic) }
    var editingDiastolic by rememberSaveable { mutableStateOf(false) }
    val unit = stringResource(R.string.unit_mmhg)
    if (!editingDiastolic) {
        ValueStepper(
            title = stringResource(R.string.quicklog_systolic),
            unit = unit,
            value = systolic,
            onValueChange = { systolic = it },
            range = 70..250,
            format = ::formatCount,
            onConfirm = { editingDiastolic = true },
        )
    } else {
        ValueStepper(
            title = stringResource(R.string.quicklog_diastolic),
            unit = unit,
            value = diastolic,
            onValueChange = { diastolic = it },
            range = 40..150,
            format = ::formatCount,
            onConfirm = { onSave(systolic, diastolic) },
        )
    }
}

private val WeightRangeKg = 20..300

@WearPreviews
@Composable
private fun WeightEntryScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            WeightEntryScreen(initialKg = 72.4, unitSystem = UnitSystem.METRIC, onSave = {})
        }
    }
}

@WearPreviews
@Composable
private fun BloodPressureEntryScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            BloodPressureEntryScreen(initialSystolic = 120, initialDiastolic = 80, onSave = { _, _ -> })
        }
    }
}
