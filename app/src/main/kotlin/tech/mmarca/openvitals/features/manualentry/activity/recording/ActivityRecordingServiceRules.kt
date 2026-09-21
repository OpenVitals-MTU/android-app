package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.annotation.SuppressLint
import android.content.pm.ServiceInfo
import android.os.Build
import tech.mmarca.openvitals.features.manualentry.activity.ActivityEntryType
import tech.mmarca.openvitals.features.manualentry.activity.supportsStepCounting

/** How the recording service uses the phone's motion sensor. */
internal enum class RecordingSensorUse {
    OFF,

    /** The step detector, for the whole recording. */
    STEPS,

    /** A repetition recognizer. In a plan run it changes with the step. */
    PER_STEP_RECOGNIZER,
}

/** What the service keeps running for one recording state. */
internal data class RecordingSensorPlan(
    val location: Boolean,
    val pressure: Boolean,
    val sensor: RecordingSensorUse,
)

/** Everything is off unless the state is recording: a pause or a rest reads no sensor. */
internal fun recordingSensorPlan(
    status: ActivityRecordingStatus,
    kind: ActivityRecordingKind,
    activityType: ActivityEntryType?,
): RecordingSensorPlan {
    val off = RecordingSensorPlan(location = false, pressure = false, sensor = RecordingSensorUse.OFF)
    if (status != ActivityRecordingStatus.RECORDING) return off
    val steps = if (activityType?.supportsStepCounting == true) {
        RecordingSensorUse.STEPS
    } else {
        RecordingSensorUse.OFF
    }
    return when (kind) {
        ActivityRecordingKind.GPS_ROUTE ->
            RecordingSensorPlan(location = true, pressure = true, sensor = steps)
        ActivityRecordingKind.REPETITION ->
            RecordingSensorPlan(location = false, pressure = false, sensor = RecordingSensorUse.PER_STEP_RECOGNIZER)
        // Without GPS the barometer and step detector still run: neither needs a
        // position. Only for a type that could have used GPS.
        ActivityRecordingKind.TIMED ->
            if (activityType?.supportsGpsRoute == true) {
                RecordingSensorPlan(location = false, pressure = true, sensor = steps)
            } else {
                off
            }
    }
}

/**
 * The foreground service type bits for a recording. Android 14 refuses a type
 * whose permission is missing, so each bit is set only when it is used.
 */
// The constants are inlined by the compiler, and sdkInt guards each one. Lint cannot see a guard on a parameter.
@SuppressLint("InlinedApi")
internal fun recordingForegroundServiceType(
    kind: ActivityRecordingKind,
    countsSteps: Boolean,
    hasBleDevices: Boolean,
    sdkInt: Int,
): Int {
    val typedByUse = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    if (kind == ActivityRecordingKind.GPS_ROUTE) {
        if (sdkInt < Build.VERSION_CODES.Q) return 0
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (countsSteps && typedByUse) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (hasBleDevices && typedByUse) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        return type
    }
    if (!typedByUse) return 0
    var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
    if (hasBleDevices) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    return type
}
