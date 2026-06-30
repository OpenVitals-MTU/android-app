package tech.mmarca.openvitals.features.activity

import java.time.LocalDate
import org.junit.Assert.assertNull
import org.junit.Test

class ActivitiesOverviewSectionsTest {

    @Test
    fun `marker is empty when day has movement metrics but no workout`() {
        val date = LocalDate.of(2026, 6, 24)
        val bucket = ActivityOverviewBucket(
            date = date,
            days = listOf(
                ActivityOverviewDay(
                    date = date,
                    steps = 5_000L,
                    distanceMeters = 4_000.0,
                    energyBurnedKcal = 300.0,
                )
            ),
        )

        assertNull(activityOverviewMarkerWorkout(bucket))
    }
}
