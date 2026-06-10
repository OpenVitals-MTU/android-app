package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import kotlin.math.max
import kotlin.math.sqrt

internal data class RecognizedRepetition(
    val timeMillis: Long,
    val intensity: Double = 0.0,
)

internal class PushUpProximityRecognizer(
    private val thresholdCentimeters: Float = 2f,
) {
    private var wasClose = false

    fun onProximity(valueCentimeters: Float, nowMillis: Long): RecognizedRepetition? {
        val isClose = valueCentimeters < thresholdCentimeters
        val recognized = isClose && !wasClose
        wasClose = isClose
        return if (recognized) RecognizedRepetition(nowMillis) else null
    }
}

internal class StepDetectorRepetitionRecognizer {
    fun onStep(nowMillis: Long): RecognizedRepetition = RecognizedRepetition(nowMillis)
}

internal class JumpRepetitionRecognizer(
    private val maxJumpDurationMillis: Long,
    private val fallingThreshold: Double = 2.5,
    private val jumpingThreshold: Double = 20.0,
) {
    private var state = MotionState.RELAXING
    private var lastJumpDetectedMillis = 0L
    private var currentJumpMaxAcceleration = 0.0
    private var lastJumpMaxAcceleration = 0.0

    fun onAcceleration(x: Float, y: Float, z: Float, nowMillis: Long): RecognizedRepetition? {
        val acceleration = absoluteAcceleration(x, y, z)
        val recognized = when {
            state == MotionState.RELAXING && acceleration < fallingThreshold -> {
                state = MotionState.FALLING
                lastJumpDetectedMillis = nowMillis
                null
            }
            (state == MotionState.PREPARE || state == MotionState.FALLING) &&
                acceleration > jumpingThreshold &&
                acceleration > lastJumpMaxAcceleration * 0.6 -> {
                state = MotionState.JUMPING
                lastJumpDetectedMillis = nowMillis
                currentJumpMaxAcceleration = acceleration
                null
            }
            state == MotionState.JUMPING && acceleration < fallingThreshold -> {
                state = MotionState.FALLING
                val intensity = currentJumpMaxAcceleration
                lastJumpMaxAcceleration = currentJumpMaxAcceleration
                currentJumpMaxAcceleration = 0.0
                RecognizedRepetition(lastJumpDetectedMillis, intensity)
            }
            state != MotionState.RELAXING && nowMillis - lastJumpDetectedMillis > maxJumpDurationMillis -> {
                state = MotionState.RELAXING
                lastJumpMaxAcceleration = 0.0
                currentJumpMaxAcceleration = 0.0
                null
            }
            else -> null
        }
        if (state == MotionState.JUMPING) {
            currentJumpMaxAcceleration = max(currentJumpMaxAcceleration, acceleration)
        }
        return recognized
    }
}

internal class PullUpRepetitionRecognizer(
    private val smoothing: Double = 0.02,
    private val pullThreshold: Double = 10.2,
    private val relaxThreshold: Double = 9.65,
    private val minimumPullMillis: Long = 500L,
    private val minimumRelaxMillis: Long = 400L,
    private val maximumRelaxMillis: Long = 2_000L,
) {
    private var state = PullState.RELAXING
    private var smoothedAcceleration = 9.81
    private var pullStartMillis = 0L
    private var relaxStartMillis = 0L
    private var maxAcceleration = 9.81

    fun onAcceleration(x: Float, y: Float, z: Float, nowMillis: Long): RecognizedRepetition? {
        val acceleration = absoluteAcceleration(x, y, z)
        smoothedAcceleration = smoothedAcceleration * (1 - smoothing) + acceleration * smoothing
        return when (state) {
            PullState.RELAXING -> {
                if (smoothedAcceleration > pullThreshold) {
                    state = PullState.PULLING
                    pullStartMillis = nowMillis
                    maxAcceleration = smoothedAcceleration
                }
                null
            }
            PullState.PULLING -> {
                maxAcceleration = max(maxAcceleration, smoothedAcceleration)
                if (smoothedAcceleration < relaxThreshold) {
                    state = PullState.RETURNING
                    relaxStartMillis = nowMillis
                }
                null
            }
            PullState.RETURNING -> {
                val pullDuration = relaxStartMillis - pullStartMillis
                val relaxDuration = nowMillis - relaxStartMillis
                if (smoothedAcceleration > pullThreshold) {
                    state = PullState.PULLING
                    pullStartMillis = nowMillis
                    maxAcceleration = smoothedAcceleration
                    null
                } else if (relaxDuration > maximumRelaxMillis) {
                    state = PullState.RELAXING
                    null
                } else if (pullDuration >= minimumPullMillis && relaxDuration >= minimumRelaxMillis) {
                    state = PullState.RELAXING
                    RecognizedRepetition(nowMillis, (maxAcceleration - 9.81) * 10)
                } else {
                    null
                }
            }
        }
    }
}

private enum class MotionState {
    RELAXING,
    PREPARE,
    FALLING,
    JUMPING,
}

private enum class PullState {
    RELAXING,
    PULLING,
    RETURNING,
}

private fun absoluteAcceleration(x: Float, y: Float, z: Float): Double =
    sqrt((x * x + y * y + z * z).toDouble())
