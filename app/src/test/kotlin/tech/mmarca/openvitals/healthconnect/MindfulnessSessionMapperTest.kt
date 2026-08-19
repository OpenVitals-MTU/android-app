package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MindfulnessSessionMapperTest {

    private fun record(notes: String?): MindfulnessSessionRecord = MindfulnessSessionRecord(
        startTime = Instant.parse("2026-08-19T08:00:00Z"),
        startZoneOffset = null,
        endTime = Instant.parse("2026-08-19T08:10:00Z"),
        endZoneOffset = null,
        metadata = Metadata.manualEntry(),
        mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
        title = "Meditation",
        notes = notes,
    )

    @Test fun `real notes pass through`() {
        assertEquals("calm sit", record("calm sit").toMindfulnessSession().notes)
    }

    @Test fun `absent notes stay null`() {
        assertNull(record(null).toMindfulnessSession().notes)
    }

    @Test fun `the Flutter-era literal null reads as no notes`() {
        assertNull(record("null").toMindfulnessSession().notes)
    }

    @Test fun `blank notes read as no notes`() {
        assertNull(record("   ").toMindfulnessSession().notes)
    }
}
