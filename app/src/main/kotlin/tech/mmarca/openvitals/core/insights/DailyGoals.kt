package tech.mmarca.openvitals.core.insights

import tech.mmarca.openvitals.core.period.DatePeriod
import java.time.LocalDate
import kotlin.math.roundToInt

enum class DailyGoalDirection {
    AT_LEAST,
    AT_MOST,
}

enum class MetricDailyGoalKey(
    val storageKey: String,
    val defaultValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val step: Double,
    val direction: DailyGoalDirection = DailyGoalDirection.AT_LEAST,
) {
    STEPS("goal_steps", 8_000.0, 500.0, 50_000.0, 500.0),
    DISTANCE_METERS("goal_distance_meters", 5_000.0, 250.0, 50_000.0, 250.0),
    CALORIES_OUT_KCAL("goal_calories_out_kcal", 2_000.0, 250.0, 6_000.0, 50.0),
    ACTIVE_CALORIES_KCAL("goal_active_calories_kcal", 400.0, 25.0, 3_000.0, 25.0),
    FLOORS("goal_floors", 10.0, 1.0, 200.0, 1.0),
    ELEVATION_METERS("goal_elevation_meters", 100.0, 5.0, 3_000.0, 5.0),
    WHEELCHAIR_PUSHES("goal_wheelchair_pushes", 1_000.0, 50.0, 50_000.0, 50.0),
    SLEEP_HOURS("goal_sleep_hours", 8.0, 1.0, 14.0, 0.25),
    WORKOUT_MINUTES("goal_workout_minutes", 30.0, 5.0, 240.0, 5.0),
    MINDFULNESS_MINUTES("goal_mindfulness_minutes", 10.0, 1.0, 120.0, 1.0),
    CALORIES_IN_KCAL("goal_calories_in_kcal", 2_000.0, 500.0, 6_000.0, 50.0, DailyGoalDirection.AT_MOST),
    PROTEIN_GRAMS("goal_protein_grams", 50.0, 5.0, 300.0, 5.0),
    CARBS_GRAMS("goal_carbs_grams", 275.0, 25.0, 800.0, 25.0, DailyGoalDirection.AT_MOST),
    FAT_GRAMS("goal_fat_grams", 70.0, 5.0, 300.0, 5.0, DailyGoalDirection.AT_MOST);

    fun normalize(value: Double): Double =
        value.coerceIn(minValue, maxValue)
}

data class DailyGoalValue(
    val date: LocalDate,
    val value: Double,
)

data class DailyGoalDay(
    val date: LocalDate,
    val value: Double,
    val isTracked: Boolean,
    val isMet: Boolean,
)

data class DailyGoalProgress(
    val target: Double,
    val direction: DailyGoalDirection,
    val days: List<DailyGoalDay>,
) {
    val trackedDays: Int get() = days.count { it.isTracked }
    val goalMetDays: Int get() = days.count { it.isMet }
    val successRatePercent: Int
        get() = trackedDays.takeIf { it > 0 }
            ?.let { (goalMetDays * 100.0 / it).roundToInt() }
            ?: 0
    val currentStreakDays: Int
        get() = days
            .sortedBy { it.date }
            .asReversed()
            .takeWhile { it.isMet }
            .count()
    val longestStreakDays: Int
        get() {
            var current = 0
            var longest = 0
            days.sortedBy { it.date }.forEach { day ->
                if (day.isMet) {
                    current += 1
                    longest = maxOf(longest, current)
                } else {
                    current = 0
                }
            }
            return longest
        }
    val averageGapToGoal: Double
        get() {
            val gaps = days
                .filter { it.isTracked }
                .map { day ->
                    when (direction) {
                        DailyGoalDirection.AT_LEAST -> (target - day.value).coerceAtLeast(0.0)
                        DailyGoalDirection.AT_MOST -> (day.value - target).coerceAtLeast(0.0)
                    }
                }
            return if (gaps.isEmpty()) 0.0 else gaps.average()
        }
}

fun dailyGoalProgress(
    values: List<DailyGoalValue>,
    period: DatePeriod,
    target: Double,
    direction: DailyGoalDirection,
): DailyGoalProgress {
    val valuesByDate = values
        .groupBy { it.date }
        .mapValues { (_, dayValues) -> dayValues.sumOf { it.value } }
    val days = generateSequence(period.start) { date ->
        date.plusDays(1).takeUnless { it.isAfter(period.end) }
    }.map { date ->
        val value = valuesByDate[date] ?: 0.0
        val isTracked = value > 0.0
        DailyGoalDay(
            date = date,
            value = value,
            isTracked = isTracked,
            isMet = isTracked && when (direction) {
                DailyGoalDirection.AT_LEAST -> value >= target
                DailyGoalDirection.AT_MOST -> value <= target
            },
        )
    }.toList()

    return DailyGoalProgress(
        target = target,
        direction = direction,
        days = days,
    )
}
