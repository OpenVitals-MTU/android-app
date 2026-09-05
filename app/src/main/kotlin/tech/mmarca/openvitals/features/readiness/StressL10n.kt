package tech.mmarca.openvitals.features.readiness

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressConfidence
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressEstimate
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressLevel
import tech.mmarca.openvitals.domain.insights.StressItemTemplate
import tech.mmarca.openvitals.domain.insights.StressListItem

/**
 * Renders the stress estimate through the string catalog. The English
 * sentences stay canonical and persisted; the UI renders the structured
 * items, falling back to the stored English for older estimates.
 */

/** The level's explanatory paragraph. */
@StringRes
fun stressLevelDetailRes(level: PhysiologicalStressLevel): Int = when (level) {
    PhysiologicalStressLevel.RESTING -> R.string.stress_detail_resting
    PhysiologicalStressLevel.LOW -> R.string.stress_detail_low
    PhysiologicalStressLevel.MEDIUM -> R.string.stress_detail_medium
    PhysiologicalStressLevel.HIGH -> R.string.stress_detail_high
    PhysiologicalStressLevel.NEEDS_MORE_DATA -> R.string.stress_detail_needs_more
}

/** The level's one-word headline. */
@StringRes
fun stressLevelLabelRes(level: PhysiologicalStressLevel): Int = when (level) {
    PhysiologicalStressLevel.RESTING -> R.string.stress_label_resting
    PhysiologicalStressLevel.LOW -> R.string.stress_label_low
    PhysiologicalStressLevel.MEDIUM -> R.string.stress_label_medium
    PhysiologicalStressLevel.HIGH -> R.string.stress_label_high
    PhysiologicalStressLevel.NEEDS_MORE_DATA -> R.string.stress_label_needs_more
}

@StringRes
fun stressConfidenceLabelRes(confidence: PhysiologicalStressConfidence): Int = when (confidence) {
    PhysiologicalStressConfidence.HIGH -> R.string.cardio_load_confidence_high
    PhysiologicalStressConfidence.MEDIUM -> R.string.cardio_load_confidence_medium
    PhysiologicalStressConfidence.LOW -> R.string.cardio_load_confidence_low
    PhysiologicalStressConfidence.NO_DATA -> R.string.stress_confidence_none
}

/** The stored confidence-reason token as a resource. Unrecognised lands on "needs more data". */
@StringRes
fun stressConfidenceReasonRes(reason: String): Int = when (reason) {
    "hrv_resting_hr_average_hr" -> R.string.stress_reason_all_signals
    "partial_hrv_or_heart_rate_context" -> R.string.stress_reason_partial
    "activity_may_influence" -> R.string.stress_reason_activity
    "single_signal" -> R.string.stress_reason_single
    else -> R.string.stress_reason_needs_more
}

@Composable
fun stressConfidenceText(
    confidence: PhysiologicalStressConfidence,
    reason: String,
): String {
    val label = stringResource(stressConfidenceLabelRes(confidence))
    val reasonLabel = stringResource(stressConfidenceReasonRes(reason))
    return "$label · $reasonLabel"
}

@Composable
fun stressFactorLines(stress: PhysiologicalStressEstimate): List<String> =
    stress.lines(stress.factorItems, stress.contributingFactors)

@Composable
fun stressCoverageLines(stress: PhysiologicalStressEstimate): List<String> =
    stress.lines(stress.coverageItems, stress.dataCoverage)

@Composable
fun stressCaveatLines(stress: PhysiologicalStressEstimate): List<String> =
    stress.lines(stress.caveatItems, stress.caveats)

@Composable
private fun PhysiologicalStressEstimate.lines(
    items: List<StressListItem>,
    english: List<String>,
): List<String> =
    if (items.isEmpty() && english.isNotEmpty()) english else items.map { stressListItemLine(it) }

/** The sentence a template renders as. Pure, so the mapping can be checked for totality. */
@StringRes
fun stressTemplateRes(template: StressItemTemplate): Int = when (template) {
    StressItemTemplate.HRV_BELOW_BASELINE -> R.string.stress_factor_hrv_below
    StressItemTemplate.HRV_ABOVE_BASELINE -> R.string.stress_factor_hrv_above
    StressItemTemplate.HRV_NEAR_BASELINE -> R.string.stress_factor_hrv_near
    StressItemTemplate.RESTING_HR_ABOVE -> R.string.stress_factor_resting_hr_above
    StressItemTemplate.RESTING_HR_BELOW -> R.string.stress_factor_resting_hr_below
    StressItemTemplate.RESTING_HR_NEAR -> R.string.stress_factor_resting_hr_near
    StressItemTemplate.AVG_HR_ABOVE_RESTING -> R.string.stress_factor_avg_hr
    StressItemTemplate.ACTIVITY_INFLUENCE -> R.string.stress_factor_activity
    StressItemTemplate.SLEEP_RAISES_STRAIN -> R.string.stress_factor_sleep_raises
    StressItemTemplate.SLEEP_MIXED -> R.string.stress_factor_sleep_mixed
    StressItemTemplate.SLEEP_SUPPORTS_LOWER -> R.string.stress_factor_sleep_supports
    StressItemTemplate.SLEEP_PLAIN -> R.string.stress_factor_sleep_plain
    StressItemTemplate.NO_HYDRATION_LOGGED -> R.string.stress_factor_no_hydration
    StressItemTemplate.HYDRATION_SO_FAR -> R.string.stress_factor_hydration
    StressItemTemplate.NUTRITION_LARGE -> R.string.stress_factor_nutrition_large
    StressItemTemplate.NUTRITION_PLAIN -> R.string.stress_factor_nutrition_plain
    StressItemTemplate.TEMPERATURE_ELEVATED -> R.string.stress_factor_temp_elevated
    StressItemTemplate.TEMPERATURE_SLIGHTLY_ELEVATED -> R.string.stress_factor_temp_slight
    StressItemTemplate.TEMPERATURE_NOT_ELEVATED -> R.string.stress_factor_temp_not
    StressItemTemplate.LOAD_HIGH_PERCENT -> R.string.stress_factor_load_high
    StressItemTemplate.LOAD_NEAR_TARGET -> R.string.stress_factor_load_near
    // The readiness catalog's own sentence: two translations would drift.
    StressItemTemplate.MINDFULNESS_LOGGED -> R.string.readiness_factor_mindfulness_detail
    StressItemTemplate.COVERAGE_HR_SAMPLES -> R.string.stress_coverage_hr_samples
    StressItemTemplate.COVERAGE_HR_AVERAGE_ONLY -> R.string.stress_coverage_hr_avg_only
    StressItemTemplate.COVERAGE_HR_NONE -> R.string.stress_coverage_hr_none
    StressItemTemplate.COVERAGE_HRV_POINTS -> R.string.stress_coverage_hrv_points
    StressItemTemplate.COVERAGE_HRV_SINGLE_POINT -> R.string.stress_coverage_hrv_single
    StressItemTemplate.COVERAGE_HRV_AVERAGE_ONLY -> R.string.stress_coverage_hrv_avg_only
    StressItemTemplate.COVERAGE_HRV_NONE -> R.string.stress_coverage_hrv_none
    StressItemTemplate.CAVEAT_NOT_MENTAL_STRESS -> R.string.stress_caveat_not_mental
    StressItemTemplate.CAVEAT_NO_HEALTH_CONNECT_SCORE -> R.string.stress_caveat_no_hc_score
    StressItemTemplate.CAVEAT_CONFOUNDERS -> R.string.stress_caveat_confounders
    StressItemTemplate.CAVEAT_ALL_DAY_MODEL -> R.string.stress_caveat_all_day_model
    StressItemTemplate.CAVEAT_WORKOUT_INFLUENCE -> R.string.stress_caveat_workout
    StressItemTemplate.CAVEAT_LOW_CONFIDENCE -> R.string.stress_caveat_low_confidence
    StressItemTemplate.CAVEAT_SPARSE_HRV -> R.string.stress_caveat_sparse_hrv
}

@Composable
fun stressListItemLine(item: StressListItem): String {
    val res = stressTemplateRes(item.template)
    val args = item.args
    return when (item.template) {
        StressItemTemplate.HRV_BELOW_BASELINE,
        StressItemTemplate.HRV_ABOVE_BASELINE,
        StressItemTemplate.RESTING_HR_ABOVE,
        StressItemTemplate.RESTING_HR_BELOW,
        StressItemTemplate.AVG_HR_ABOVE_RESTING,
        StressItemTemplate.SLEEP_RAISES_STRAIN,
        StressItemTemplate.SLEEP_MIXED,
        StressItemTemplate.SLEEP_SUPPORTS_LOWER,
        StressItemTemplate.SLEEP_PLAIN,
        StressItemTemplate.LOAD_HIGH_PERCENT,
        StressItemTemplate.MINDFULNESS_LOGGED,
        -> stringResource(res, args.intArg(0))

        StressItemTemplate.HYDRATION_SO_FAR -> stringResource(res, formatOneDecimal(args.arg(0)))

        StressItemTemplate.TEMPERATURE_ELEVATED,
        StressItemTemplate.TEMPERATURE_SLIGHTLY_ELEVATED,
        StressItemTemplate.TEMPERATURE_NOT_ELEVATED,
        -> stringResource(res, temperatureValues(args))

        StressItemTemplate.COVERAGE_HR_SAMPLES,
        StressItemTemplate.COVERAGE_HRV_POINTS,
        -> stringResource(res, args.intArg(0), windowText(args))

        StressItemTemplate.COVERAGE_HRV_SINGLE_POINT -> stringResource(res, windowText(args))

        else -> stringResource(res)
    }
}

/** The temperature clause: body, skin delta, or both. An absent one is dropped, never zero. */
@Composable
private fun temperatureValues(args: List<Double>): String {
    val temperature = stressTemperature(args)
    return listOfNotNull(
        temperature.bodyCelsius?.let {
            stringResource(R.string.readiness_factor_temp_body, formatOneDecimal(it))
        },
        temperature.skinDeltaCelsius?.let {
            val sign = if (it > 0) "+" else ""
            stringResource(R.string.readiness_factor_temp_skin, sign + formatOneDecimal(it))
        },
    ).joinToString(", ")
}

@Composable
private fun windowText(args: List<Double>): String = when (val window = stressWindow(args)) {
    StressWindow.Day -> stringResource(R.string.stress_window_day)
    is StressWindow.At -> stringResource(R.string.stress_window_at, clock(window.epochMillis))
    is StressWindow.Range -> stringResource(
        R.string.stress_window_range,
        clock(window.startEpochMillis),
        clock(window.endEpochMillis),
    )
}

/** When a coverage item's samples were taken. */
sealed interface StressWindow {
    /** No window recorded — the item covers the whole day. */
    data object Day : StressWindow

    /** Every sample landed on one instant. */
    data class At(val epochMillis: Long) : StressWindow

    data class Range(val startEpochMillis: Long, val endEpochMillis: Long) : StressWindow
}

/** The coverage window in `args[1..2]` as epoch millis; NaN means no window. */
fun stressWindow(args: List<Double>): StressWindow {
    val start = args.arg(1)
    val end = args.arg(2)
    if (start.isNaN() || end.isNaN()) return StressWindow.Day
    if (start == end) return StressWindow.At(start.toLong())
    return StressWindow.Range(start.toLong(), end.toLong())
}

/** The optional temperature readings in `args[0..1]`; NaN means "not measured". */
data class StressTemperature(
    val bodyCelsius: Double?,
    val skinDeltaCelsius: Double?,
)

fun stressTemperature(args: List<Double>): StressTemperature = StressTemperature(
    bodyCelsius = args.arg(0).takeIf { !it.isNaN() },
    skinDeltaCelsius = args.arg(1).takeIf { !it.isNaN() },
)

/** A missing arg reads as NaN, which every caller then treats as absent. */
fun List<Double>.arg(index: Int): Double = getOrElse(index) { Double.NaN }

/** A NaN or missing integer arg reads as zero, matching the sentence templates. */
fun List<Double>.intArg(index: Int): Int = arg(index).let { if (it.isNaN()) 0 else it.roundToInt() }

private fun formatOneDecimal(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun clock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(ShortTimeFormatter)

/** The device's own 12/24-hour convention, not a hard-coded `HH:mm`. */
private val ShortTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
