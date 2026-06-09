package tech.mmarca.openvitals.features.manualentry

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RepetitionRecognizersTest {
    @Test
    fun `push-up recognizer counts only close transitions`() {
        val recognizer = PushUpProximityRecognizer()

        assertNull(recognizer.onProximity(5f, 0L))
        assertNotNull(recognizer.onProximity(1f, 100L))
        assertNull(recognizer.onProximity(1f, 200L))
        assertNull(recognizer.onProximity(5f, 300L))
        assertNotNull(recognizer.onProximity(1f, 400L))
    }

    @Test
    fun `step recognizer counts each step detector event`() {
        val recognizer = StepDetectorRepetitionRecognizer()

        assertNotNull(recognizer.onStep(100L))
        assertNotNull(recognizer.onStep(200L))
    }

    @Test
    fun `jump recognizer counts jumping to falling transition`() {
        val recognizer = JumpRepetitionRecognizer(maxJumpDurationMillis = 1_250L)

        assertNull(recognizer.onAcceleration(0f, 0f, 1f, 0L))
        assertNull(recognizer.onAcceleration(0f, 0f, 22f, 100L))
        assertNotNull(recognizer.onAcceleration(0f, 0f, 1f, 200L))
    }

    @Test
    fun `pull-up recognizer counts pull and relax sequence`() {
        val recognizer = PullUpRepetitionRecognizer(smoothing = 1.0)

        assertNull(recognizer.onAcceleration(0f, 0f, 11f, 0L))
        assertNull(recognizer.onAcceleration(0f, 0f, 11f, 600L))
        assertNull(recognizer.onAcceleration(0f, 0f, 9f, 700L))
        assertNotNull(recognizer.onAcceleration(0f, 0f, 9f, 1_200L))
    }
}
