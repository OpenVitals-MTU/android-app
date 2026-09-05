package tech.mmarca.openvitals.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardMetricLoadGroupsTest {

    @Test
    fun `an uncoupled metric loads on its own`() {
        val groups = dashboardMetricLoadGroups(
            listOf(DashboardMetric.STEPS, DashboardMetric.DISTANCE, DashboardMetric.HYDRATION),
        )

        assertEquals(
            listOf(
                setOf(DashboardMetric.STEPS),
                setOf(DashboardMetric.DISTANCE),
                setOf(DashboardMetric.HYDRATION),
            ),
            groups,
        )
    }

    @Test
    fun `groups follow the order their metrics were given in`() {
        val groups = dashboardMetricLoadGroups(
            listOf(DashboardMetric.HYDRATION, DashboardMetric.STEPS),
        )

        // Dispatch order is the dashboard's only priority signal, so it has to survive grouping.
        assertEquals(setOf(DashboardMetric.HYDRATION), groups.first())
    }

    @Test
    fun `metrics that share a read are loaded in one pass`() {
        val groups = dashboardMetricLoadGroups(
            listOf(
                DashboardMetric.WEIGHT,
                DashboardMetric.STEPS,
                DashboardMetric.BMI,
                DashboardMetric.HEIGHT,
            ),
        )

        // Split apart, weight and height would be read once per tile.
        assertEquals(
            listOf(
                setOf(DashboardMetric.WEIGHT, DashboardMetric.HEIGHT, DashboardMetric.BMI),
                setOf(DashboardMetric.STEPS),
            ),
            groups,
        )
    }

    @Test
    fun `a coupled group is narrowed to the metrics actually asked for`() {
        val groups = dashboardMetricLoadGroups(listOf(DashboardMetric.PROTEIN, DashboardMetric.FAT))

        // Pulling in carbs and caffeine here would only load tiles nobody is showing.
        assertEquals(listOf(setOf(DashboardMetric.PROTEIN, DashboardMetric.FAT)), groups)
    }

    @Test
    fun `sleep and HRV stay together so the sleep score keeps its HRV term`() {
        val groups = dashboardMetricLoadGroups(listOf(DashboardMetric.SLEEP, DashboardMetric.HRV))

        assertEquals(listOf(setOf(DashboardMetric.SLEEP, DashboardMetric.HRV)), groups)
    }

    @Test
    fun `every metric is claimed exactly once`() {
        val all = DashboardMetric.entries.toList()

        val groups = dashboardMetricLoadGroups(all)

        assertEquals(all.toSet(), groups.flatten().toSet())
        assertEquals(all.size, groups.sumOf { it.size })
        assertTrue(groups.all { it.isNotEmpty() })
    }
}
