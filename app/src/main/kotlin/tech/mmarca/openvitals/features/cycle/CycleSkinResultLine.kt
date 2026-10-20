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
    return when (val result = skin.result) {
        is SkinTemperatureShiftResult.Confirmed -> stringResource(
            R.string.cycle_skin_result_confirmed,
            date.format(result.firstHighDay),
            unitFormatter.temperatureDelta(result.amplitudeCelsius).let { "${it.value} ${it.unit}" },
        )
        SkinTemperatureShiftResult.None -> stringResource(R.string.cycle_skin_result_none)
        is SkinTemperatureShiftResult.SignalTooWeak -> stringResource(
            R.string.cycle_skin_result_weak,
            unitFormatter.temperatureDelta(result.amplitudeCelsius).let { "${it.value} ${it.unit}" },
        )
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
