package tech.mmarca.openvitals.core.presentation

import androidx.annotation.StringRes
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BpRecordValues

/**
 * The body positions and cuff locations a blood pressure reading can name,
 * in the order every picker lists them, each with its label. The one list
 * the manual entry form, the health report and the CSV importer all read.
 */

val BpBodyPositionLabels: Map<Int, Int> = mapOf(
    BpRecordValues.BODY_POSITION_SITTING_DOWN to R.string.bp_position_sitting,
    BpRecordValues.BODY_POSITION_STANDING_UP to R.string.bp_position_standing,
    BpRecordValues.BODY_POSITION_LYING_DOWN to R.string.bp_position_lying,
    BpRecordValues.BODY_POSITION_RECLINING to R.string.bp_position_reclining,
)

val BpMeasurementLocationLabels: Map<Int, Int> = mapOf(
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_UPPER_ARM to R.string.bp_location_left_arm,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_UPPER_ARM to R.string.bp_location_right_arm,
    BpRecordValues.MEASUREMENT_LOCATION_LEFT_WRIST to R.string.bp_location_left_wrist,
    BpRecordValues.MEASUREMENT_LOCATION_RIGHT_WRIST to R.string.bp_location_right_wrist,
)

/** [position]'s label, or "Not specified" for a value no picker offers. */
@StringRes
fun bpBodyPositionLabelRes(position: Int): Int =
    BpBodyPositionLabels[position] ?: R.string.option_not_specified

/** [location]'s label, or "Not specified" for a value no picker offers. */
@StringRes
fun bpMeasurementLocationLabelRes(location: Int): Int =
    BpMeasurementLocationLabels[location] ?: R.string.option_not_specified
