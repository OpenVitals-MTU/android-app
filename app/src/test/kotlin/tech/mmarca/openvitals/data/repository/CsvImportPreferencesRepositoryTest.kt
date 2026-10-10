package tech.mmarca.openvitals.data.repository

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BpRecordValues

class CsvImportPreferencesRepositoryTest {

    /** One [FakeSharedPreferences] that survives across instances, so a second [newRepository] is the storage round-trip. */
    private val context: Context = mockk<Context>().also { context ->
        every { context.getSharedPreferences(any(), any()) } returns FakeSharedPreferences()
    }

    private fun newRepository() = CsvImportPreferencesRepository(context)

    @Test
    fun `defaults are unknown until chosen`() {
        val repo = newRepository()

        assertEquals(BpRecordValues.BODY_POSITION_UNKNOWN, repo.defaultBodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_UNKNOWN, repo.defaultCuffLocation)
    }

    @Test
    fun `chosen defaults are saved and reloaded`() {
        newRepository().apply {
            defaultBodyPosition = BpRecordValues.BODY_POSITION_RECLINING
            defaultCuffLocation = BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST
        }

        val reloaded = newRepository()

        assertEquals(BpRecordValues.BODY_POSITION_RECLINING, reloaded.defaultBodyPosition)
        assertEquals(BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST, reloaded.defaultCuffLocation)
    }
}
