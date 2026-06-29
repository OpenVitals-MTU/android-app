package tech.mmarca.openvitals.features.nutrition

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.domain.insights.BaselineValue
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.domain.insights.MacroSplitInterpretation
import tech.mmarca.openvitals.domain.insights.PeriodComparison
import tech.mmarca.openvitals.domain.insights.PersonalBaselineInsight
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import java.time.LocalDate

@Immutable
data class NutritionDisplayState(
    val selectedPeriod: DatePeriod = DatePeriod(LocalDate.now(), LocalDate.now()),
    val hasData: Boolean = false,
    val totals: NutritionPeriodTotals = NutritionPeriodTotals(),
    val metric: NutritionMetricDisplay = NutritionMetricDisplay(),
    val overviewNutrients: List<NutritionNutrientSeries> = emptyList(),
    val trackedDates: List<LocalDate> = emptyList(),
    val sampleCount: Int = 0,
    val macroSplit: MacroSplitInterpretation? = null,
)

@Immutable
data class NutritionPeriodTotals(
    val energyKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
)

@Immutable
data class NutritionDayValue(
    val date: LocalDate,
    val value: Double,
)

@Immutable
data class NutritionNutrientSeries(
    val nutrient: NutritionNutrient,
    val totalValue: Double = 0.0,
    val values: List<NutritionDayValue> = emptyList(),
    val hasTrackedValues: Boolean = false,
)

@Immutable
data class NutritionMetricDisplay(
    val nutrient: NutritionNutrient = NutritionNutrient.ENERGY,
    val hasData: Boolean = false,
    val totalValue: Double = 0.0,
    val values: List<NutritionDayValue> = emptyList(),
    val previousTotal: Double = 0.0,
    val baselineValues: List<BaselineValue> = emptyList(),
    val goalProgress: DailyGoalProgress? = null,
    val periodComparison: PeriodComparison = PeriodComparison(0.0, 0.0),
    val loggedDays: Int = 0,
    val averageValue: Double = 0.0,
    val bestDayValue: Double = 0.0,
    val baselineInsight: PersonalBaselineInsight? = null,
)
