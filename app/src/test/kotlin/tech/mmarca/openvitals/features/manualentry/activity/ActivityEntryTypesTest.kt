package tech.mmarca.openvitals.features.manualentry.activity

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The form writes every session type Health Connect defines, so a session from another app can be made here too. */
class ActivityEntryTypesTest {

    private val healthConnectExerciseTypes: Map<String, Int> = ExerciseSessionRecord::class.java.fields
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
        .filter { it.name.startsWith("EXERCISE_TYPE_") }
        .associate { it.name to it.getInt(null) }

    @Test fun `every Health Connect session type has an entry type`() {
        // Health Connect 1.1 defines 61 (60 sports and "other workout"); the reflection must see them.
        assertTrue(healthConnectExerciseTypes.size.toString(), healthConnectExerciseTypes.size >= 61)
        val listed = DefaultActivityEntryTypes.map { it.exerciseType }.toSet()

        val missing = healthConnectExerciseTypes.filterValues { it !in listed }.keys

        assertEquals("not in DefaultActivityEntryTypes", emptySet<String>(), missing)
    }

    @Test fun `entry ids are unique`() {
        val ids = DefaultActivityEntryTypes.map { it.id }

        assertEquals(ids.distinct(), ids)
    }

    @Test fun `a type without a route is a timed session, not one the form cannot record`() {
        // "Other workout" is the one deliberate exception: the generic bucket is manual-only.
        val unrecordable = DefaultActivityEntryTypes
            .filter { !it.supportsLiveRecording && !it.isRepetitionLike }
            .map { it.exerciseType }

        assertEquals(listOf(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT), unrecordable)
    }
}
