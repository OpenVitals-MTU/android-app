package tech.mmarca.openvitals.wear.features.measure

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.ui.theme.ElevationColor
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.VitalsColor

/**
 * A reading the user starts by hand. Each one is offered only when
 * `WearCapabilities.measurements` contains it.
 */
enum class Measurement(
    @param:StringRes val label: Int,
    @param:StringRes val description: Int,
    @param:StringRes val instruction: Int,
    val icon: ImageVector,
    val accentColor: Color,
    /** Needs the heart rate permission before it can start. */
    val needsHeartRatePermission: Boolean,
    /** How long the measurement runs at most. */
    val durationSeconds: Int,
) {
    /** Health Services' on-demand heart rate. */
    HEART_RATE(
        R.string.measure_heart_rate,
        R.string.measure_heart_rate_description,
        R.string.measure_instruction_still,
        Icons.Outlined.Favorite,
        HeartColor,
        needsHeartRatePermission = true,
        durationSeconds = 30,
    ),

    /** RMSSD over a minute of single beats. Only on watches that report each beat. */
    HRV(
        R.string.measure_hrv,
        R.string.measure_hrv_description,
        R.string.measure_instruction_still_minute,
        Icons.Outlined.Insights,
        VitalsColor,
        needsHeartRatePermission = true,
        durationSeconds = 60,
    ),

    /** The barometer, with the altitude it implies at standard pressure. */
    AIR_PRESSURE(
        R.string.measure_air_pressure,
        R.string.measure_air_pressure_description,
        R.string.measure_instruction_none,
        Icons.Outlined.Compress,
        ElevationColor,
        needsHeartRatePermission = false,
        durationSeconds = 10,
    ),
}

/** Why a measurement could not give a result. */
enum class MeasureError(@param:StringRes val message: Int) {
    NOT_SUPPORTED(R.string.measure_error_not_supported),
    PERMISSION_DENIED(R.string.measure_error_permission),
    OFF_BODY(R.string.measure_error_off_body),
    NO_SIGNAL(R.string.measure_error_no_signal),
    FAILED(R.string.measure_error_failed),
}

class MeasureException(val error: MeasureError, cause: Throwable? = null) : Exception(error.name, cause)

/** What a running measurement reports along the way. */
sealed interface MeasureEvent {
    /** A live value worth showing while the measurement runs, such as the current pulse. */
    data class Live(val value: Double) : MeasureEvent

    /** The sensor is not reading yet, for example still locking on or off the wrist. */
    data class Waiting(val offBody: Boolean) : MeasureEvent

    /** The final reading. [secondary] carries a derived value, such as altitude for pressure. */
    data class Result(val value: Double, val secondary: Double? = null) : MeasureEvent
}
