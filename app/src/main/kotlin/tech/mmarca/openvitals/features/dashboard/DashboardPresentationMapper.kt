package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import java.time.ZoneId
import kotlin.math.roundToInt

object DashboardPresentationMapper {

    fun build(
        data: DashboardData,
        dailyGoals: DashboardDailyGoals,
        unitFormatter: UnitFormatter,
        dateTimeFormatterProvider: DateTimeFormatterProvider,
        loadingWidgets: Set<DashboardWidgetId> = emptySet(),
    ): DashboardDisplayState {
        val sleepGoalMs = (dailyGoals.sleepHours * 60.0 * 60.0 * 1000.0).toLong()
        val widgets = DashboardWidgetId.entries.associateWith { widgetId ->
            buildWidget(
                widgetId = widgetId,
                data = data,
                dailyGoals = dailyGoals,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                sleepGoalMs = sleepGoalMs,
                isLoading = widgetId in loadingWidgets,
            )
        }.filterValues { it != null }.mapValues { (_, value) -> value!! }

        return DashboardDisplayState(widgets = widgets)
    }

    private fun buildWidget(
        widgetId: DashboardWidgetId,
        data: DashboardData,
        dailyGoals: DashboardDailyGoals,
        unitFormatter: UnitFormatter,
        dateTimeFormatterProvider: DateTimeFormatterProvider,
        sleepGoalMs: Long,
        isLoading: Boolean,
    ): DashboardWidgetDisplayModel? = when (widgetId) {
        DashboardWidgetId.STEPS -> metricWidget(
            id = widgetId,
            value = DisplayValue(unitFormatter.count(data.steps), "steps"),
            style = DashboardWidgetStyle.CIRCLE,
            progress = goalProgressModel(
                current = data.steps.toDouble(),
                target = dailyGoals.steps,
                goalLabelValue = DisplayValue(unitFormatter.count(dailyGoals.steps.roundToInt()), ""),
            ),
            isLoading = isLoading,
        )
        DashboardWidgetId.WEEKLY_CARDIO_LOAD -> weeklyCardioLoadWidget(
            id = widgetId,
            style = DashboardWidgetStyle.CIRCLE,
            weeklyCardioLoad = data.weeklyCardioLoad,
            isLoading = isLoading,
        )
        DashboardWidgetId.CARDIO_LOAD -> weeklyCardioLoadWidget(
            id = widgetId,
            style = DashboardWidgetStyle.PILL,
            weeklyCardioLoad = data.weeklyCardioLoad,
            isLoading = isLoading,
        )
        DashboardWidgetId.DISTANCE -> metricWidget(
            id = widgetId,
            value = unitFormatter.distance(data.distanceMeters),
            progress = goalProgressModel(
                current = data.distanceMeters,
                target = dailyGoals.distanceMeters,
                goalLabelValue = unitFormatter.distance(dailyGoals.distanceMeters),
            ),
            isLoading = isLoading,
        )
        DashboardWidgetId.CALORIES_OUT -> {
            val caloriesKcal = if (data.caloriesKcalSource == CaloriesBurnedSource.NO_DATA) 0.0 else data.caloriesKcal
            optionalMetricWidget(
                id = widgetId,
                value = unitFormatter.energy(caloriesKcal),
                hasValue = data.caloriesKcalSource != CaloriesBurnedSource.NO_DATA,
                caloriesSubtitle = data.caloriesKcalSource,
                progress = goalProgressModel(
                    current = caloriesKcal,
                    target = dailyGoals.caloriesOutKcal,
                    goalLabelValue = unitFormatter.energy(dailyGoals.caloriesOutKcal),
                ),
                isLoading = isLoading,
            )
        }
        DashboardWidgetId.ACTIVE_CALORIES -> optionalMetricWidget(
            id = widgetId,
            value = data.activeCaloriesKcal?.let(unitFormatter::energy),
            progress = data.activeCaloriesKcal?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.activeCaloriesKcal,
                    goalLabelValue = unitFormatter.energy(dailyGoals.activeCaloriesKcal),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.FLOORS -> optionalMetricWidget(
            id = widgetId,
            value = data.floorsClimbed?.let {
                DisplayValue(unitFormatter.count(it), "")
            },
            progress = data.floorsClimbed?.let {
                goalProgressModel(
                    current = it.toDouble(),
                    target = dailyGoals.floors,
                    goalLabelValue = DisplayValue(unitFormatter.count(dailyGoals.floors.roundToInt()), ""),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.ELEVATION -> optionalMetricWidget(
            id = widgetId,
            value = data.elevationGainedMeters?.let(unitFormatter::elevation),
            progress = data.elevationGainedMeters?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.elevationMeters,
                    goalLabelValue = unitFormatter.elevation(dailyGoals.elevationMeters),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.WHEELCHAIR_PUSHES -> optionalMetricWidget(
            id = widgetId,
            value = data.wheelchairPushes?.let {
                DisplayValue(unitFormatter.count(it), "")
            },
            progress = data.wheelchairPushes?.let {
                goalProgressModel(
                    current = it.toDouble(),
                    target = dailyGoals.wheelchairPushes,
                    goalLabelValue = DisplayValue(
                        unitFormatter.count(dailyGoals.wheelchairPushes.roundToInt()),
                        "",
                    ),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.SLEEP -> optionalMetricWidget(
            id = widgetId,
            value = data.sleep?.let { DisplayValue(unitFormatter.duration(it.durationMs), "") },
            showTitle = false,
            sleepScore = data.sleepScore
                .takeIf { it.confidence != SleepScoreConfidence.NO_DATA }
                ?.let { score ->
                    SleepScoreDisplay(
                        score = score.score,
                        confidence = score.confidence,
                        rating = sleepScoreRatingFor(score.score),
                    )
                },
            progress = data.sleep?.let {
                goalProgressModel(
                    current = it.durationMs.toDouble(),
                    target = sleepGoalMs.toDouble(),
                    goalLabelValue = DisplayValue(unitFormatter.duration(sleepGoalMs), ""),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.BODY_ENERGY -> optionalMetricWidget(
            id = widgetId,
            value = data.bodyEnergyTimeline?.let { DisplayValue(unitFormatter.count(it.currentScore), "") },
            measurementSubtitle = data.bodyEnergyTimeline?.let { timeline ->
                "Start ${timeline.startScore}  +${timeline.charged} / -${timeline.drained}"
            },
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.HYDRATION -> metricWidget(
            id = widgetId,
            value = unitFormatter.hydration(data.hydrationLiters),
            progress = goalProgressModel(
                current = data.hydrationLiters,
                target = dailyGoals.hydrationLiters,
                goalLabelValue = unitFormatter.hydration(dailyGoals.hydrationLiters),
            ),
            isLoading = isLoading,
        )
        DashboardWidgetId.CALORIES_IN -> optionalMetricWidget(
            id = widgetId,
            value = data.caloriesInKcal?.let(unitFormatter::energy),
            progress = data.caloriesInKcal?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.caloriesInKcal,
                    goalLabelValue = unitFormatter.energy(dailyGoals.caloriesInKcal),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.PROTEIN -> optionalMetricWidget(
            id = widgetId,
            value = data.proteinGrams?.let { gramDisplayValue(it, unitFormatter) },
            progress = data.proteinGrams?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.proteinGrams,
                    goalLabelValue = gramDisplayValue(dailyGoals.proteinGrams, unitFormatter),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.CARBS -> optionalMetricWidget(
            id = widgetId,
            value = data.carbsGrams?.let { gramDisplayValue(it, unitFormatter) },
            progress = data.carbsGrams?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.carbsGrams,
                    goalLabelValue = gramDisplayValue(dailyGoals.carbsGrams, unitFormatter),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.FAT -> optionalMetricWidget(
            id = widgetId,
            value = data.fatGrams?.let { gramDisplayValue(it, unitFormatter) },
            progress = data.fatGrams?.let {
                goalProgressModel(
                    current = it,
                    target = dailyGoals.fatGrams,
                    goalLabelValue = gramDisplayValue(dailyGoals.fatGrams, unitFormatter),
                )
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.WEIGHT -> optionalMetricWidget(
            id = widgetId,
            value = data.weightKg?.let(unitFormatter::weight),
            measurementSubtitle = data.weightTime?.let {
                dashboardMeasurementDate(it, dateTimeFormatterProvider)
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.HEIGHT -> optionalMetricWidget(
            id = widgetId,
            value = data.heightCm?.let(unitFormatter::height),
            measurementSubtitle = data.heightTime?.let {
                dashboardMeasurementDate(it, dateTimeFormatterProvider)
            },
            isLoading = isLoading,
        )
        DashboardWidgetId.BMI -> optionalMetricWidget(
            id = widgetId,
            value = data.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            isLoading = isLoading,
        )
        DashboardWidgetId.FFMI -> optionalMetricWidget(
            id = widgetId,
            value = data.ffmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            isLoading = isLoading,
        )
        DashboardWidgetId.BODY_FAT -> metricWidget(
            id = widgetId,
            value = unitFormatter.percent(data.bodyFatPercent),
            isLoading = isLoading,
        )
        DashboardWidgetId.LEAN_MASS -> optionalMetricWidget(
            id = widgetId,
            value = data.leanMassKg?.let(unitFormatter::bodyMass),
            isLoading = isLoading,
        )
        DashboardWidgetId.BMR -> optionalMetricWidget(
            id = widgetId,
            value = data.bmrKcal?.let(unitFormatter::energy),
            isLoading = isLoading,
        )
        DashboardWidgetId.BONE_MASS -> optionalMetricWidget(
            id = widgetId,
            value = data.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            isLoading = isLoading,
        )
        DashboardWidgetId.BODY_WATER_MASS -> optionalMetricWidget(
            id = widgetId,
            value = data.bodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            isLoading = isLoading,
        )
        DashboardWidgetId.AVG_HEART_RATE -> metricWidget(
            id = widgetId,
            value = unitFormatter.heartRate(data.avgHeartRateBpm),
            isLoading = isLoading,
        )
        DashboardWidgetId.RESTING_HEART_RATE -> metricWidget(
            id = widgetId,
            value = unitFormatter.heartRate(data.restingHeartRateBpm),
            isLoading = isLoading,
        )
        DashboardWidgetId.HRV -> optionalMetricWidget(
            id = widgetId,
            value = data.hrvRmssdMs?.let(unitFormatter::hrv),
            isLoading = isLoading,
        )
        DashboardWidgetId.BLOOD_PRESSURE -> optionalMetricWidget(
            id = widgetId,
            value = if (data.latestSystolicMmHg != null && data.latestDiastolicMmHg != null) {
                unitFormatter.bloodPressure(data.latestSystolicMmHg, data.latestDiastolicMmHg)
            } else {
                null
            },
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.SPO2 -> optionalMetricWidget(
            id = widgetId,
            value = data.latestSpO2Percent?.let(unitFormatter::percent),
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.VO2_MAX -> optionalMetricWidget(
            id = widgetId,
            value = data.latestVo2Max?.let(unitFormatter::vo2Max),
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.RESPIRATORY_RATE -> optionalMetricWidget(
            id = widgetId,
            value = data.avgRespiratoryRate?.let(unitFormatter::respiratoryRate),
            isLoading = isLoading,
        )
        DashboardWidgetId.BODY_TEMPERATURE -> optionalMetricWidget(
            id = widgetId,
            value = data.latestBodyTemperatureCelsius?.let(unitFormatter::temperature),
            isLoading = isLoading,
        )
        DashboardWidgetId.BLOOD_GLUCOSE -> optionalMetricWidget(
            id = widgetId,
            value = data.latestBloodGlucoseMillimolesPerLiter?.let(unitFormatter::bloodGlucose),
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.SKIN_TEMPERATURE -> optionalMetricWidget(
            id = widgetId,
            value = data.latestSkinTemperatureDeltaCelsius?.let(unitFormatter::temperatureDelta),
            requiresNoDataMessage = true,
            isLoading = isLoading,
        )
        DashboardWidgetId.MINDFULNESS -> metricWidget(
            id = widgetId,
            value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()),
            progress = goalProgressModel(
                current = (data.mindfulnessMinutes ?: 0).toDouble(),
                target = dailyGoals.mindfulnessMinutes,
                goalLabelValue = unitFormatter.minutes(dailyGoals.mindfulnessMinutes.roundToInt().toLong()),
            ),
            isLoading = isLoading,
        )
        DashboardWidgetId.CYCLE -> DashboardWidgetDisplayModel(
            id = widgetId,
            cycle = cycleDisplay(data),
            isLoading = isLoading,
        )
        DashboardWidgetId.WORKOUT -> null
    }

    private fun metricWidget(
        id: DashboardWidgetId,
        value: DisplayValue,
        style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
        progress: DashboardWidgetProgressModel? = null,
        isLoading: Boolean,
    ): DashboardWidgetDisplayModel =
        DashboardWidgetDisplayModel(
            id = id,
            style = style,
            value = value,
            hasValue = true,
            progress = progress,
            isLoading = isLoading,
        )

    private fun optionalMetricWidget(
        id: DashboardWidgetId,
        value: DisplayValue?,
        hasValue: Boolean = value != null,
        style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
        progress: DashboardWidgetProgressModel? = null,
        caloriesSubtitle: CaloriesBurnedSource? = null,
        sleepScore: SleepScoreDisplay? = null,
        measurementSubtitle: String? = null,
        showTitle: Boolean = true,
        requiresNoDataMessage: Boolean = false,
        isLoading: Boolean,
    ): DashboardWidgetDisplayModel =
        DashboardWidgetDisplayModel(
            id = id,
            style = style,
            value = value,
            hasValue = hasValue,
            progress = progress,
            isLoading = isLoading,
            caloriesSubtitle = caloriesSubtitle,
            sleepScore = sleepScore,
            measurementSubtitle = measurementSubtitle,
            showTitle = showTitle,
            requiresNoDataMessage = requiresNoDataMessage,
        )

    private fun weeklyCardioLoadWidget(
        id: DashboardWidgetId,
        style: DashboardWidgetStyle,
        weeklyCardioLoad: tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad?,
        isLoading: Boolean,
    ): DashboardWidgetDisplayModel =
        DashboardWidgetDisplayModel(
            id = id,
            style = style,
            weeklyCardioLoad = weeklyCardioLoad,
            isLoading = isLoading,
        )

    private fun gramDisplayValue(value: Double, unitFormatter: UnitFormatter): DisplayValue =
        DisplayValue(unitFormatter.count(value.roundToInt()), "g")

    private fun cycleDisplay(data: DashboardData): CycleWidgetDisplay? =
        when {
            data.menstruationPeriodDays != null && data.menstruationPeriodDays > 0 ->
                CycleWidgetDisplay.MenstruationDays(data.menstruationPeriodDays)
            data.ovulationTestCount != null && data.ovulationTestCount > 0 ->
                CycleWidgetDisplay.OvulationTests(data.ovulationTestCount)
            data.latestBasalBodyTemperatureCelsius != null ->
                CycleWidgetDisplay.BasalTemperature(data.latestBasalBodyTemperatureCelsius)
            else -> null
        }
}

internal fun dashboardMeasurementDate(
    time: java.time.Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    dateTimeFormatterProvider.mediumDate().format(time.atZone(ZoneId.systemDefault()).toLocalDate())
