package tech.mmarca.openvitals.healthconnect

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.privacy.PrivacySentinels

class HealthConnectCallCountersTest {

    @Before
    fun setUp() {
        HealthConnectCallCounters.resetForTest()
    }

    @After
    fun tearDown() {
        HealthConnectCallCounters.resetForTest()
    }

    @Test fun `a range in the operation name never reaches the summary`() {
        // The readers name a call with its range or id in brackets; the counters keep the name only.
        HealthConnectCallCounters.started("readRawHeartRateSamples[${PrivacySentinels.Instant}..${PrivacySentinels.Date}]")
        HealthConnectCallCounters.rateLimited("readExerciseSession[${PrivacySentinels.Uuid}]")
        HealthConnectCallCounters.failed("readRecords[${PrivacySentinels.Sentence}]")

        val summary = HealthConnectCallCounters.summaryLines().joinToString("\n")

        PrivacySentinels.assertNoneIn(summary)
        assertTrue(summary.contains("readRawHeartRateSamples: started=1"))
        assertTrue(summary.contains("readExerciseSession: started=0 ok=0 failed=0 rateLimited=1"))
    }

    @Test fun `each outcome counts under its own column and the total sums them`() {
        HealthConnectCallCounters.started("aggregate")
        HealthConnectCallCounters.succeeded("aggregate")
        HealthConnectCallCounters.started("aggregate")
        HealthConnectCallCounters.rateLimited("aggregate")
        HealthConnectCallCounters.skippedForBackoff("readSleep")

        val snapshot = HealthConnectCallCounters.snapshot()

        assertEquals(
            HealthConnectCallCount(started = 2, succeeded = 1, rateLimited = 1),
            snapshot.counts.getValue("aggregate"),
        )
        assertEquals(HealthConnectCallCount(skippedForBackoff = 1), snapshot.counts.getValue("readSleep"))
        assertEquals(HealthConnectCallCount(started = 2, succeeded = 1, rateLimited = 1, skippedForBackoff = 1), snapshot.total)
        assertEquals("total: started=2 ok=1 failed=0 rateLimited=1 skipped=1", snapshot.lines().first())
    }

    @Test fun `a delta holds only what happened after the snapshot`() {
        HealthConnectCallCounters.started("aggregate")
        val before = HealthConnectCallCounters.snapshot()
        HealthConnectCallCounters.started("aggregate")
        HealthConnectCallCounters.started("readSleep")

        val delta = HealthConnectCallCounters.deltaSince(before)

        assertEquals(HealthConnectCallCount(started = 1), delta.counts.getValue("aggregate"))
        assertEquals(HealthConnectCallCount(started = 1), delta.counts.getValue("readSleep"))
    }

    @Test fun `an empty tally says so`() {
        assertEquals(listOf("none"), HealthConnectCallCounters.summaryLines())
    }
}
