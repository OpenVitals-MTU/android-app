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
 * The nutrition overview tiles, which issue #259 turned inside out: a period
 * total is a number nobody eats by, so a multi-day period leads with the daily
 * average and keeps the total as a caption.
 *
 * Worth a picture rather than an assertion because the whole change is about
 * what the eye lands on. The value, the title and the caption say three
 * different things in three different sizes, and an assertion that all three
 * strings are present holds just as well if the caption ends up as big as the
 * headline, or if the tile beside it stands a line taller because it has no
 * caption of its own to draw.
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
        // One day's total already IS the day. A tile reading "1,850 calories
        // per day / 1,850 kcal total" says the same thing twice.
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
        // Only one tile of the four has anything to average, so only one draws
        // a caption. The others must not end up shorter than it — this is the
        // shot that catches a ragged row.
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

    /**
     * The four overview tiles, built the way the mapper builds them: a week of
     * seven logged days, so the average is the total over seven.
     */
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

        // A fixed week, never `LocalDate.now()`: a golden that moves with the
        // calendar draws a different picture every day the suite runs.
        val MONDAY: LocalDate = LocalDate.of(2026, 5, 4)
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
