package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.SkinTemperatureShiftResult
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * What the night skin temperature of this cycle shows, in plain words and why: the rule, that
 * it is found afterwards, and, for a weak signal, why that happens. The wording never says
 * "anovulatory" and never names a fertile or a safe day.
 */
@Composable
internal fun skinResultText(
    skin: CycleSkinDisplay,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val date = dateTimeFormatterProvider.mediumDate()
    // The 0.2 °C the rule needs, in the reader's unit.
    val threshold = unitFormatter.temperatureDelta(0.2, signed = false).text
    return when (val result = skin.result) {
        is SkinTemperatureShiftResult.Confirmed -> stringResource(
            R.string.cycle_skin_result_confirmed,
            date.format(result.firstHighDay),
            unitFormatter.temperatureDelta(result.amplitudeCelsius).let { "${it.value} ${it.unit}" },
            threshold,
        )
        SkinTemperatureShiftResult.None -> stringResource(R.string.cycle_skin_result_none, threshold)
        is SkinTemperatureShiftResult.SignalTooWeak -> stringResource(
            R.string.cycle_skin_result_weak,
            unitFormatter.temperatureDelta(result.amplitudeCelsius).let { "${it.value} ${it.unit}" },
            threshold,
        )
        is SkinTemperatureShiftResult.SparseData -> stringResource(R.string.cycle_skin_result_sparse)
        is SkinTemperatureShiftResult.InsufficientData -> stringResource(R.string.cycle_skin_result_insufficient)
        is SkinTemperatureShiftResult.NotPlausible -> stringResource(
            R.string.cycle_skin_result_not_plausible,
            date.format(result.firstHighDay),
        )
    }
}

@Composable
internal fun CycleSkinResultLine(
    skin: CycleSkinDisplay,
    text: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
        skin.source?.let { source ->
            Text(
                text = stringResource(R.string.cycle_skin_result_source, source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
