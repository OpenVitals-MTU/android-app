package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSessionRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The export's names read back as the types they were written from. */
class ExerciseTypeNamesTest {

    @Test
    fun `every named type round-trips through its export name`() {
        for ((type, name) in ExerciseTypeNames.byType) {
            assertEquals(name, ExerciseTypeNames.exportName(type))
            assertEquals(type, ExerciseTypeNames.typeForExportName(name))
        }
    }

    @Test
    fun `a type the table does not know is written as a workout and a workout reads back as other`() {
        assertEquals("workout", ExerciseTypeNames.exportName(-42))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, ExerciseTypeNames.typeForExportName("workout"))
    }

    @Test
    fun `a name is matched without regard to case or surrounding space, and an unknown one is null`() {
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseTypeNames.typeForExportName(" Running "))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY, ExerciseTypeNames.typeForExportName("BIKING_STATIONARY"))
        assertNull(ExerciseTypeNames.typeForExportName("kayak polo"))
        assertNull(ExerciseTypeNames.typeForExportName(""))
    }

    @Test
    fun `the names are snake case words a file can carry`() {
        for (name in ExerciseTypeNames.byType.values) {
            assertTrue(name, name.matches(Regex("[a-z]+(_[a-z]+)*")))
        }
        assertEquals(ExerciseTypeNames.byType.size, ExerciseTypeNames.byType.values.toSet().size)
    }
}
