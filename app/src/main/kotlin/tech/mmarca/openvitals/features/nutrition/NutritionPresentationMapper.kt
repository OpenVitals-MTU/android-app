package tech.mmarca.openvitals.features.nutrition

import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.displayPeriodFor
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalValue
import tech.mmarca.openvitals.domain.insights.dailyGoalProgress
import tech.mmarca.openvitals.domain.insights.macroSplitInterpretation
import tech.mmarca.openvitals.domain.insights.periodComparison
import tech.mmarca.openvitals.domain.insights.personalBaselineInsight
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.valueFor
import java.time.LocalDate

private val primaryOverviewNutrients = listOf(
    NutritionNutrient.ENERGY,
    NutritionNutrient.PROTEIN,
    NutritionNutrient.TOTAL_CARBOHYDRATE,
    NutritionNutrient.TOTAL_FAT,
)

object NutritionPresentationMapper {

    fun build(
        query: PeriodLoadQuery,
        metric: NutritionMetric,
        dailyGoal: Double,
        dailyMacros: List<DailyMacros>,
        previousDailyMacros: List<DailyMacros>,
        baselineDailyMacros: List<DailyMacros>,
        entries: List<NutritionEntry>,
    ): NutritionDisplayState {
        val selectedPeriod = displayPeriodFor(
            range = query.range,
            anchorDate = query.selectedDate,
            weekPeriodMode = query.weekPeriodMode,
        )
        val totals = dailyMacros.totals()
        val trackedDates = dailyMacros.filter { it.hasNutritionData() }.map { it.date }
        val overviewNutrients = NutritionNutrient.entries.map { nutrient ->
            dailyMacros.nutrientSeries(nutrient)
        }
        val metricDisplay = buildMetricDisplay(
            metric = metric,
            dailyGoal = dailyGoal,
            period = selectedPeriod,
            dailyMacros = dailyMacros,
            previousDailyMacros = previousDailyMacros,
            baselineDailyMacros = baselineDailyMacros,
        )

        return NutritionDisplayState(
            selectedPeriod = selectedPeriod,
            hasData = trackedDates.isNotEmpty() || entries.isNotEmpty(),
            totals = totals,
            metric = metricDisplay,
            overviewNutrients = overviewNutrients,
            trackedDates = trackedDates,
            sampleCount = entries.takeIf { it.isNotEmpty() }?.size ?: trackedDates.size,
            macroSplit = macroSplitInterpretation(
                proteinGrams = dailyMacros.sumOf { it.proteinGrams },
                carbsGrams = dailyMacros.sumOf { it.carbsGrams },
                fatGrams = dailyMacros.sumOf { it.fatGrams },
            ),
        )
    }
}

internal val NutritionMetric.nutrient: NutritionNutrient
    get() = when (this) {
        NutritionMetric.CALORIES_IN -> NutritionNutrient.ENERGY
        NutritionMetric.PROTEIN -> NutritionNutrient.PROTEIN
        NutritionMetric.CARBS -> NutritionNutrient.TOTAL_CARBOHYDRATE
        NutritionMetric.FAT -> NutritionNutrient.TOTAL_FAT
    }

internal val primaryNutritionOverviewNutrients: List<NutritionNutrient>
    get() = primaryOverviewNutrients

private fun buildMetricDisplay(
    metric: NutritionMetric,
    dailyGoal: Double,
    period: tech.mmarca.openvitals.core.period.DatePeriod,
    dailyMacros: List<DailyMacros>,
    previousDailyMacros: List<DailyMacros>,
    baselineDailyMacros: List<DailyMacros>,
): NutritionMetricDisplay {
    val nutrient = metric.nutrient
    val series = dailyMacros.nutrientSeries(nutrient)
    val previousSeries = previousDailyMacros.nutrientSeries(nutrient)
    val baselineSeries = baselineDailyMacros.nutrientSeries(nutrient)
    val rawValues = series.values.map { it.value }
    val loggedDays = rawValues.count { it > 0.0 }
    val averageValue = loggedDays.takeIf { it > 0 }?.let { rawValues.sum() / it } ?: 0.0
    val goalProgress = dailyGoalProgress(
        values = series.values.map { DailyGoalValue(date = it.date, value = it.value) },
        period = period,
        target = dailyGoal,
        direction = metric.dailyGoalKey.direction,
    )

    return NutritionMetricDisplay(
        nutrient = nutrient,
        hasData = series.hasTrackedValues,
        totalValue = series.totalValue,
        values = series.values,
        previousTotal = previousSeries.totalValue,
        baselineValues = baselineSeries.values.map { BaselineValue(it.date, it.value) },
        goalProgress = goalProgress,
        periodComparison = periodComparison(
            currentValue = series.totalValue,
            previousValue = previousSeries.totalValue,
        ),
        loggedDays = loggedDays,
        averageValue = averageValue,
        bestDayValue = rawValues.maxOrNull() ?: 0.0,
        baselineInsight = personalBaselineInsight(
            currentValue = averageValue,
            values = baselineSeries.values.map { BaselineValue(it.date, it.value) },
            referenceDate = period.start.minusDays(1),
        ),
    )
}

private fun List<DailyMacros>.totals(): NutritionPeriodTotals =
    NutritionPeriodTotals(
        energyKcal = sumOf { it.energyKcal },
        proteinGrams = sumOf { it.proteinGrams },
        carbsGrams = sumOf { it.carbsGrams },
        fatGrams = sumOf { it.fatGrams },
    )

private fun List<DailyMacros>.nutrientSeries(nutrient: NutritionNutrient): NutritionNutrientSeries {
    val values = map { day -> NutritionDayValue(date = day.date, value = day.valueFor(nutrient)) }
    val totalValue = values.sumOf { it.value }
    return NutritionNutrientSeries(
        nutrient = nutrient,
        totalValue = totalValue,
        values = values,
        hasTrackedValues = values.any { it.value > 0.0 },
    )
}

private fun DailyMacros.hasNutritionData(): Boolean =
    nutrientValues.any { (_, value) -> value > 0.0 } ||
        energyKcal > 0.0 ||
        proteinGrams > 0.0 ||
        carbsGrams > 0.0 ||
        fatGrams > 0.0
