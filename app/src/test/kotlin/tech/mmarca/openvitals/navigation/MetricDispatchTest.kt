package tech.mmarca.openvitals.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.features.activity.ActivityMetric
import tech.mmarca.openvitals.features.body.BodyMetric
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.HeartMetric
import tech.mmarca.openvitals.features.nutrition.NutritionMetric

/**
 * Ported from the Flutter `test/navigation/metric_dispatch_test.dart`, which
 * pins the `/metric/{metricId}` dispatch to this precedence: the calories
 * AGGREGATE intercepts its ids before the per-metric activity screens can claim
 * them, body/heart/vitals/nutrition each open their focused metric screen, and
 * `WORKOUT` renders the activities aggregate.
 */
class MetricDispatchTest {

    @Test
    fun `calories ids land on the calories aggregate, not the activity screen`() {
        listOf(
            DashboardWidgetId.CALORIES_OUT,
            DashboardWidgetId.ACTIVE_CALORIES,
            DashboardWidgetId.BMR,
        ).forEach { id ->
            assertEquals(id.name, MetricRouteDestination.Calories, metricRouteDestinationFor(id))
        }
    }

    @Test
    fun `body ids land on the per-metric body screen`() {
        mapOf(
            DashboardWidgetId.WEIGHT to BodyMetric.WEIGHT,
            DashboardWidgetId.HEIGHT to BodyMetric.HEIGHT,
            DashboardWidgetId.BMI to BodyMetric.BMI,
            DashboardWidgetId.FFMI to BodyMetric.BMI,
            DashboardWidgetId.BODY_FAT to BodyMetric.BODY_FAT,
            DashboardWidgetId.LEAN_MASS to BodyMetric.LEAN_MASS,
            DashboardWidgetId.BONE_MASS to BodyMetric.BONE_MASS,
            DashboardWidgetId.BODY_WATER_MASS to BodyMetric.BODY_WATER_MASS,
        ).forEach { (id, metric) ->
            assertEquals(
                id.name,
                MetricRouteDestination.BodyDetail(metric),
                metricRouteDestinationFor(id),
            )
        }
    }

    @Test
    fun `nutrition ids land on the per-metric nutrition screen`() {
        mapOf(
            DashboardWidgetId.CALORIES_IN to NutritionMetric.CALORIES_IN,
            DashboardWidgetId.PROTEIN to NutritionMetric.PROTEIN,
            DashboardWidgetId.CARBS to NutritionMetric.CARBS,
            DashboardWidgetId.FAT to NutritionMetric.FAT,
        ).forEach { (id, metric) ->
            assertEquals(
                id.name,
                MetricRouteDestination.Nutrition(metric),
                metricRouteDestinationFor(id),
            )
        }
    }

    @Test
    fun `movement ids land on the activity metric screen`() {
        mapOf(
            DashboardWidgetId.STEPS to ActivityMetric.STEPS,
            DashboardWidgetId.DISTANCE to ActivityMetric.DISTANCE,
            DashboardWidgetId.FLOORS to ActivityMetric.FLOORS,
            DashboardWidgetId.ELEVATION to ActivityMetric.ELEVATION,
            DashboardWidgetId.WHEELCHAIR_PUSHES to ActivityMetric.WHEELCHAIR_PUSHES,
        ).forEach { (id, metric) ->
            assertEquals(
                id.name,
                MetricRouteDestination.ActivityDetail(metric),
                metricRouteDestinationFor(id),
            )
        }
    }

    @Test
    fun `heart and vitals ids land on the heart metric screen`() {
        mapOf(
            DashboardWidgetId.AVG_HEART_RATE to HeartMetric.AVERAGE_HEART_RATE,
            DashboardWidgetId.RESTING_HEART_RATE to HeartMetric.RESTING_HEART_RATE,
            DashboardWidgetId.HRV to HeartMetric.HRV,
            DashboardWidgetId.BLOOD_PRESSURE to HeartMetric.BLOOD_PRESSURE,
            DashboardWidgetId.SPO2 to HeartMetric.SPO2,
            DashboardWidgetId.VO2_MAX to HeartMetric.VO2_MAX,
            DashboardWidgetId.RESPIRATORY_RATE to HeartMetric.RESPIRATORY_RATE,
            DashboardWidgetId.BODY_TEMPERATURE to HeartMetric.BODY_TEMPERATURE,
            DashboardWidgetId.BLOOD_GLUCOSE to HeartMetric.BLOOD_GLUCOSE,
            DashboardWidgetId.SKIN_TEMPERATURE to HeartMetric.SKIN_TEMPERATURE,
        ).forEach { (id, metric) ->
            assertEquals(
                id.name,
                MetricRouteDestination.HeartDetail(metric),
                metricRouteDestinationFor(id),
            )
        }
    }

    @Test
    fun `explicit tail workout sleep hydration caffeine mindfulness cycle`() {
        assertEquals(
            MetricRouteDestination.Activities,
            metricRouteDestinationFor(DashboardWidgetId.WORKOUT),
        )
        assertEquals(
            MetricRouteDestination.Sleep,
            metricRouteDestinationFor(DashboardWidgetId.SLEEP),
        )
        assertEquals(
            MetricRouteDestination.Hydration,
            metricRouteDestinationFor(DashboardWidgetId.HYDRATION),
        )
        assertEquals(
            MetricRouteDestination.Caffeine,
            metricRouteDestinationFor(DashboardWidgetId.CAFFEINE),
        )
        assertEquals(
            MetricRouteDestination.Mindfulness,
            metricRouteDestinationFor(DashboardWidgetId.MINDFULNESS),
        )
        assertEquals(
            MetricRouteDestination.Cycle,
            metricRouteDestinationFor(DashboardWidgetId.CYCLE),
        )
        assertEquals(
            MetricRouteDestination.CardioLoad,
            metricRouteDestinationFor(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
        )
        assertEquals(
            MetricRouteDestination.CardioLoad,
            metricRouteDestinationFor(DashboardWidgetId.CARDIO_LOAD),
        )
    }

    @Test
    fun `unknown ids fall back to the generic metric placeholder`() {
        // An id no enum constant matches never becomes a DashboardWidgetId…
        assertEquals(null, "NOT_A_METRIC".toDashboardWidgetIdOrNull())
        // …and a route with no id at all lands on the same placeholder.
        assertEquals(
            MetricRouteDestination.Unknown,
            metricRouteDestinationFor("NOT_A_METRIC".toDashboardWidgetIdOrNull()),
        )
        assertEquals(MetricRouteDestination.Unknown, metricRouteDestinationFor(null))
    }
}
