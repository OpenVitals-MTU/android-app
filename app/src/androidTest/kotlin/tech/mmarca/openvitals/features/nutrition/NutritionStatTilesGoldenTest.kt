package tech.mmarca.openvitals.features.nutrition

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden

/**
 * The nutrition overview tiles (#259): a multi-day period leads with the daily average and
 * keeps the total as a caption. A picture, because the change is about what the eye lands on.
 */
class NutritionStatTilesGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun period_averageLeadsAndTheTotalFollows() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 372.dp) {
                NutritionOverviewStatisticsContent(
                    metricsData = macros(
                        energyKcal = 12_950.0,
                        proteinGrams = 644.0,
                        carbsGrams = 1_533.0,
                        fatGrams = 427.0,
                    ),
                    showAverages = true,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("nutrition_stat_tiles_period")
    }

    @Test
    fun day_totalAloneWithNoAverageToRestateIt() {
        // One day's total already is the day, so no caption.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 372.dp) {
                NutritionOverviewStatisticsContent(
                    metricsData = macros(
                        energyKcal = 1_850.0,
                        proteinGrams = 92.0,
                        carbsGrams = 219.0,
                        fatGrams = 61.0,
                    ),
                    showAverages = false,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("nutrition_stat_tiles_day")
    }

    @Test
    fun period_someoneWhoLogsCaloriesButNotMacros() {
        // Only one tile draws a caption. The others must not end up shorter than it.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 372.dp) {
                NutritionOverviewStatisticsContent(
                    metricsData = macros(
                        energyKcal = 12_950.0,
                        proteinGrams = 0.0,
                        carbsGrams = 0.0,
                        fatGrams = 0.0,
                    ),
                    showAverages = true,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("nutrition_stat_tiles_partial")
    }

    /** The four overview tiles as the mapper builds them: a week of seven logged days. */
    private fun macros(
        energyKcal: Double,
        proteinGrams: Double,
        carbsGrams: Double,
        fatGrams: Double,
    ): List<NutritionSeriesUiModel> {
        val totals = listOf(
            NutritionNutrient.ENERGY to energyKcal,
            NutritionNutrient.PROTEIN to proteinGrams,
            NutritionNutrient.TOTAL_CARBOHYDRATE to carbsGrams,
            NutritionNutrient.TOTAL_FAT to fatGrams,
        )
        return totals.map { (nutrient, total) ->
            NutritionNutrientSeries(
                nutrient = nutrient,
                totalValue = total,
                averageValue = total / DAYS,
                values = List(DAYS) { day ->
                    NutritionDayValue(date = MONDAY.plusDays(day.toLong()), value = total / DAYS)
                },
                hasTrackedValues = total > 0.0,
            ).toUiModel(FORMATTER)
        }
    }

    private companion object {
        const val DAYS = 7

        // A fixed week, never `LocalDate.now()`.
        val MONDAY: LocalDate = LocalDate.of(2026, 5, 4)
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
