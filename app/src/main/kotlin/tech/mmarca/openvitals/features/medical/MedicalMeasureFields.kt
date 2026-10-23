package tech.mmarca.openvitals.features.medical

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.ManualRecordDraft
import tech.mmarca.openvitals.domain.medical.ManualRecordKind
import tech.mmarca.openvitals.domain.medical.MeasureValue
import tech.mmarca.openvitals.domain.medical.ObservationMeasure
import tech.mmarca.openvitals.ui.components.OptionDropdown
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * What a vital sign, social history or pregnancy record measures. Picking one starts its value
 * afresh and names the record in the user's language; the free social history entry is named by
 * the user instead.
 */
@Composable
internal fun MeasureField(kind: ManualRecordKind, selected: ObservationMeasure?, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    val measures = ObservationMeasure.of(kind)
    val labels = measures.associateWith { stringResource(measureLabelRes(it)) }
    OptionDropdown(
        label = stringResource(R.string.medical_entry_measure),
        options = measures,
        selected = selected,
        optionText = { labels.getValue(it) },
        enabled = true,
        onSelect = { measure ->
            measure?.let {
                onChange { draft ->
                    draft.copy(
                        measure = measure,
                        name = if (measure.value == MeasureValue.TEXT) "" else labels.getValue(measure),
                        unit = measure.units.firstOrNull()?.ucum.orEmpty(),
                        result = "",
                        secondResult = "",
                        answer = null,
                        valueDate = null,
                    )
                }
            }
        },
        allowNone = false,
    )
}

/** The value, typed the way its measure takes it. */
@Composable
internal fun MeasuredValueFields(
    draft: ManualRecordDraft,
    onPickDate: @Composable () -> Unit,
    onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit,
) {
    val measure = draft.measure ?: return
    when (measure.value) {
        MeasureValue.QUANTITY -> QuantityField(draft, measure, onChange)
        MeasureValue.BLOOD_PRESSURE -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberField(stringResource(R.string.vitals_entry_systolic_label), draft.result, Modifier.weight(1f)) { value ->
                onChange { it.copy(result = value) }
            }
            NumberField(stringResource(R.string.vitals_entry_diastolic_label), draft.secondResult, Modifier.weight(1f)) { value ->
                onChange { it.copy(secondResult = value) }
            }
        }
        MeasureValue.INTEGER -> NumberField(stringResource(measureLabelRes(measure)), draft.result, Modifier.fillMaxWidth(), whole = true) { value ->
            onChange { it.copy(result = value) }
        }
        MeasureValue.DATE -> onPickDate()
        MeasureValue.CODED -> {
            val labels = measure.answers.associate { it.snomed to stringResource(answerLabelRes(it.snomed)) }
            OptionDropdown(
                label = stringResource(measureLabelRes(measure)),
                options = measure.answers.map { it.snomed },
                selected = draft.answer,
                optionText = { labels.getValue(it) },
                enabled = true,
                // The words go in the record beside the code, in the user's language.
                onSelect = { code -> onChange { it.copy(answer = code, result = code?.let(labels::getValue).orEmpty()) } },
            )
        }
        MeasureValue.TEXT -> EntryTextField(label = stringResource(R.string.medical_entry_value), value = draft.result) { value ->
            onChange { it.copy(result = value) }
        }
    }
}

/** A number in one of its measure's units. One unit shows beside the field; several are a choice. */
@Composable
private fun QuantityField(draft: ManualRecordDraft, measure: ObservationMeasure, onChange: ((ManualRecordDraft) -> ManualRecordDraft) -> Unit) {
    val selected = measure.unitFor(draft.unit)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft.result,
            onValueChange = { value -> onChange { it.copy(result = value) } },
            label = { Text(stringResource(R.string.medical_entry_value)) },
            suffix = if (measure.units.size == 1) ({ Text(selected.symbol) }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        if (measure.units.size > 1) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(112.dp)) {
                measure.units.forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = unit == selected,
                        onClick = { onChange { it.copy(unit = unit.ucum) } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = measure.units.size),
                        icon = {},
                    ) {
                        Text(unit.symbol, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, whole: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (whole) KeyboardType.Number else KeyboardType.Decimal),
        modifier = modifier,
    )
}

@StringRes
internal fun measureLabelRes(measure: ObservationMeasure): Int = when (measure) {
    ObservationMeasure.BLOOD_PRESSURE -> R.string.medical_measure_blood_pressure
    ObservationMeasure.HEART_RATE -> R.string.medical_measure_heart_rate
    ObservationMeasure.RESPIRATORY_RATE -> R.string.medical_measure_respiratory_rate
    ObservationMeasure.OXYGEN_SATURATION -> R.string.medical_measure_oxygen_saturation
    ObservationMeasure.BODY_TEMPERATURE -> R.string.medical_measure_body_temperature
    ObservationMeasure.BODY_WEIGHT -> R.string.medical_measure_body_weight
    ObservationMeasure.BODY_HEIGHT -> R.string.medical_measure_body_height
    ObservationMeasure.BMI -> R.string.medical_measure_bmi
    ObservationMeasure.HEAD_CIRCUMFERENCE -> R.string.medical_measure_head_circumference
    ObservationMeasure.SMOKING_STATUS -> R.string.medical_measure_smoking_status
    ObservationMeasure.ALCOHOL_PER_DAY -> R.string.medical_measure_alcohol_per_day
    ObservationMeasure.OTHER_SOCIAL_HISTORY -> R.string.medical_measure_other_social_history
    ObservationMeasure.PREGNANCY_STATUS -> R.string.medical_measure_pregnancy_status
    ObservationMeasure.DELIVERY_DATE -> R.string.medical_measure_delivery_date
    ObservationMeasure.DELIVERY_DATE_BY_PERIOD -> R.string.medical_measure_delivery_date_by_period
    ObservationMeasure.DELIVERY_DATE_BY_OVULATION -> R.string.medical_measure_delivery_date_by_ovulation
    ObservationMeasure.BIRTHS_TOTAL -> R.string.medical_measure_births_total
    ObservationMeasure.BIRTHS_LIVE -> R.string.medical_measure_births_live
    ObservationMeasure.BIRTHS_TERM -> R.string.medical_measure_births_term
    ObservationMeasure.BIRTHS_PRETERM -> R.string.medical_measure_births_preterm
    ObservationMeasure.CHILDREN_LIVING -> R.string.medical_measure_children_living
    ObservationMeasure.ABORTIONS_TOTAL -> R.string.medical_measure_abortions_total
    ObservationMeasure.ABORTIONS_INDUCED -> R.string.medical_measure_abortions_induced
    ObservationMeasure.ABORTIONS_SPONTANEOUS -> R.string.medical_measure_abortions_spontaneous
    ObservationMeasure.ECTOPIC_PREGNANCIES -> R.string.medical_measure_ectopic_pregnancies
}

@StringRes
private fun answerLabelRes(snomed: String): Int = when (snomed) {
    "449868002" -> R.string.medical_answer_smokes_daily
    "428041000124106" -> R.string.medical_answer_smokes_occasionally
    "8517006" -> R.string.medical_answer_former_smoker
    "266919005" -> R.string.medical_answer_never_smoked
    "77386006" -> R.string.medical_answer_pregnant
    "60001007" -> R.string.medical_answer_not_pregnant
    else -> R.string.medical_status_unknown
}
