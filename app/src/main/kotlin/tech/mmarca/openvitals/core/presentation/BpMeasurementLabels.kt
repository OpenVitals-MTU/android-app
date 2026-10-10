package tech.mmarca.openvitals.core.presentation

import androidx.annotation.StringRes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BpRecordValues

val BpBodyPositions = listOf(
    BpRecordValues.BODY_POSITION_SITTING_DOWN,
    BpRecordValues.BODY_POSITION_STANDING_UP,
    BpRecordValues.BODY_POSITION_LYING_DOWN,
    BpRecordValues.BODY_POSITION_RECLINING,
)

val BpMeasurementLocations = listOf(
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST,
)

@StringRes
fun bpBodyPositionLabelRes(position: Int): Int = when (position) {
    BpRecordValues.BODY_POSITION_SITTING_DOWN -> R.string.bp_position_sitting
    BpRecordValues.BODY_POSITION_STANDING_UP -> R.string.bp_position_standing
    BpRecordValues.BODY_POSITION_LYING_DOWN -> R.string.bp_position_lying
    BpRecordValues.BODY_POSITION_RECLINING -> R.string.bp_position_reclining
    else -> R.string.bp_position_unknown
}

@StringRes
fun bpMeasurementLocationLabelRes(location: Int): Int = when (location) {
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM -> R.string.bp_location_left_arm
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM -> R.string.bp_location_right_arm
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST -> R.string.bp_location_left_wrist
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST -> R.string.bp_location_right_wrist
    else -> R.string.bp_location_unknown
}
