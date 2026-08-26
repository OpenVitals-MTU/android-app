package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DownhillSkiing
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Rowing
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsCricket
import androidx.compose.material.icons.outlined.SportsFootball
import androidx.compose.material.icons.outlined.SportsGolf
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.SportsHandball
import androidx.compose.material.icons.outlined.SportsHockey
import androidx.compose.material.icons.outlined.SportsKabaddi
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material.icons.outlined.Surfing
import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import tech.mmarca.openvitals.R

/** The string resource naming a Health Connect exercise session type. */
fun exerciseTypeLabelRes(type: Int): Int = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> R.string.hc_exercise_type_badminton
    ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL -> R.string.hc_exercise_type_baseball
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> R.string.hc_exercise_type_basketball
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> R.string.hc_exercise_type_biking
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> R.string.hc_exercise_type_biking_stationary
    ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> R.string.hc_exercise_type_boot_camp
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> R.string.hc_exercise_type_boxing
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> R.string.hc_exercise_type_calisthenics
    ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> R.string.hc_exercise_type_cricket
    ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> R.string.hc_exercise_type_dancing
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> R.string.hc_exercise_type_elliptical
    ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> R.string.hc_exercise_type_exercise_class
    ExerciseSessionRecord.EXERCISE_TYPE_FENCING -> R.string.hc_exercise_type_fencing
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN -> R.string.hc_exercise_type_football_american
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> R.string.hc_exercise_type_football_australian
    ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC -> R.string.hc_exercise_type_frisbee_disc
    ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> R.string.hc_exercise_type_golf
    ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> R.string.hc_exercise_type_guided_breathing
    ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> R.string.hc_exercise_type_gymnastics
    ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> R.string.hc_exercise_type_handball
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> R.string.hc_exercise_type_high_intensity_interval_training
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> R.string.hc_exercise_type_hiking
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY -> R.string.hc_exercise_type_ice_hockey
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> R.string.hc_exercise_type_ice_skating
    ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> R.string.hc_exercise_type_martial_arts
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> R.string.hc_exercise_type_paddling
    ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING -> R.string.hc_exercise_type_paragliding
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> R.string.hc_exercise_type_pilates
    ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL -> R.string.hc_exercise_type_racquetball
    ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> R.string.hc_exercise_type_rock_climbing
    ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> R.string.hc_exercise_type_roller_hockey
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> R.string.hc_exercise_type_rowing
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> R.string.hc_exercise_type_rowing_machine
    ExerciseSessionRecord.EXERCISE_TYPE_RUGBY -> R.string.hc_exercise_type_rugby
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> R.string.hc_exercise_type_running
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> R.string.hc_exercise_type_running_treadmill
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> R.string.hc_exercise_type_sailing
    ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING -> R.string.hc_exercise_type_scuba_diving
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> R.string.hc_exercise_type_skating
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> R.string.hc_exercise_type_skiing
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> R.string.hc_exercise_type_snowboarding
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> R.string.hc_exercise_type_snowshoeing
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> R.string.hc_exercise_type_soccer
    ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> R.string.hc_exercise_type_softball
    ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> R.string.hc_exercise_type_squash
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING -> R.string.hc_exercise_type_stair_climbing
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> R.string.hc_exercise_type_stair_climbing_machine
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> R.string.hc_exercise_type_strength_training
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> R.string.hc_exercise_type_stretching
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> R.string.hc_exercise_type_surfing
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> R.string.hc_exercise_type_swimming_open_water
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> R.string.hc_exercise_type_swimming_pool
    ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS -> R.string.hc_exercise_type_table_tennis
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> R.string.hc_exercise_type_tennis
    ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> R.string.hc_exercise_type_volleyball
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> R.string.hc_exercise_type_walking
    ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> R.string.hc_exercise_type_water_polo
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> R.string.hc_exercise_type_weightlifting
    ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> R.string.hc_exercise_type_wheelchair
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> R.string.hc_exercise_type_yoga
    else -> R.string.hc_exercise_type_fallback
}

@Composable
internal fun exerciseTypeLabel(type: Int): String = stringResource(exerciseTypeLabelRes(type))

internal fun exerciseTypeLabel(context: Context, type: Int): String = context.getString(exerciseTypeLabelRes(type))

internal fun exerciseTypeIcon(type: Int): ImageVector = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> Icons.AutoMirrored.Outlined.DirectionsRun
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> Icons.AutoMirrored.Outlined.DirectionsBike
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> Icons.AutoMirrored.Outlined.DirectionsWalk
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO -> Icons.Outlined.Pool
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE,
    ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> Icons.Outlined.Rowing
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
    ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING -> Icons.Outlined.SelfImprovement
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP -> Icons.Outlined.FitnessCenter
    ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
    ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
    ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING -> Icons.Outlined.DownhillSkiing
    ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> Icons.Outlined.Surfing
    ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> Icons.Outlined.Sailing
    ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> Icons.Outlined.SportsGolf
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS,
    ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS,
    ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON,
    ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL,
    ExerciseSessionRecord.EXERCISE_TYPE_SQUASH -> Icons.Outlined.SportsTennis
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> Icons.Outlined.SportsBasketball
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> Icons.Outlined.SportsSoccer
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
    ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN -> Icons.Outlined.SportsFootball
    ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL,
    ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL -> Icons.Outlined.SportsBaseball
    ExerciseSessionRecord.EXERCISE_TYPE_CRICKET -> Icons.Outlined.SportsCricket
    ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL -> Icons.Outlined.SportsVolleyball
    ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL -> Icons.Outlined.SportsHandball
    ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY,
    ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY -> Icons.Outlined.SportsHockey
    ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS,
    ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> Icons.Outlined.SportsMartialArts
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> Icons.Outlined.SportsKabaddi
    ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS -> Icons.Outlined.SportsGymnastics
    else -> Icons.AutoMirrored.Outlined.DirectionsRun
}

/** The string resource naming a Health Connect exercise segment type. */
fun exerciseSegmentLabelRes(type: Int): Int = when (type) {
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ARM_CURL -> R.string.hc_segment_arm_curl
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION -> R.string.hc_segment_back_extension
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BALL_SLAM -> R.string.hc_segment_ball_slam
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS -> R.string.hc_segment_barbell_shoulder_press
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS -> R.string.hc_segment_bench_press
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP -> R.string.hc_segment_bench_sit_up
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING -> R.string.hc_segment_biking
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY -> R.string.hc_segment_biking_stationary
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_BURPEE -> R.string.hc_segment_burpee
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_CRUNCH -> R.string.hc_segment_crunch
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT -> R.string.hc_segment_deadlift
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION -> R.string.hc_segment_double_arm_triceps_extension
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM -> R.string.hc_segment_dumbbell_curl_left_arm
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM -> R.string.hc_segment_dumbbell_curl_right_arm
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE -> R.string.hc_segment_dumbbell_front_raise
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE -> R.string.hc_segment_dumbbell_lateral_raise
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW -> R.string.hc_segment_dumbbell_row
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM -> R.string.hc_segment_dumbbell_triceps_extension_left_arm
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM -> R.string.hc_segment_dumbbell_triceps_extension_right_arm
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM -> R.string.hc_segment_dumbbell_triceps_extension_two_arm
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL -> R.string.hc_segment_elliptical
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST -> R.string.hc_segment_forward_twist
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_FRONT_RAISE -> R.string.hc_segment_front_raise
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> R.string.hc_segment_high_intensity_interval_training
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIP_THRUST -> R.string.hc_segment_hip_thrust
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_HULA_HOOP -> R.string.hc_segment_hula_hoop
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMPING_JACK -> R.string.hc_segment_jumping_jack
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE -> R.string.hc_segment_jump_rope
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING -> R.string.hc_segment_kettlebell_swing
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE -> R.string.hc_segment_lateral_raise
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN -> R.string.hc_segment_lat_pull_down
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_CURL -> R.string.hc_segment_leg_curl
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION -> R.string.hc_segment_leg_extension
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_PRESS -> R.string.hc_segment_leg_press
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_RAISE -> R.string.hc_segment_leg_raise
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_LUNGE -> R.string.hc_segment_lunge
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER -> R.string.hc_segment_mountain_climber
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT -> R.string.hc_segment_other_workout
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE -> R.string.hc_segment_pause
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES -> R.string.hc_segment_pilates
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK -> R.string.hc_segment_plank
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP -> R.string.hc_segment_pull_up
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_PUNCH -> R.string.hc_segment_punch
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST -> R.string.hc_segment_rest
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE -> R.string.hc_segment_rowing_machine
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING -> R.string.hc_segment_running
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL -> R.string.hc_segment_running_treadmill
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS -> R.string.hc_segment_shoulder_press
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION -> R.string.hc_segment_single_arm_triceps_extension
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SIT_UP -> R.string.hc_segment_sit_up
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT -> R.string.hc_segment_squat
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING -> R.string.hc_segment_stair_climbing
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE -> R.string.hc_segment_stair_climbing_machine
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING -> R.string.hc_segment_stretching
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE -> R.string.hc_segment_swimming_backstroke
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE -> R.string.hc_segment_swimming_breaststroke
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY -> R.string.hc_segment_swimming_butterfly
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE -> R.string.hc_segment_swimming_freestyle
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED -> R.string.hc_segment_swimming_mixed
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER -> R.string.hc_segment_swimming_open_water
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER -> R.string.hc_segment_swimming_other
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL -> R.string.hc_segment_swimming_pool
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_UPPER_TWIST -> R.string.hc_segment_upper_twist
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING -> R.string.hc_segment_walking
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING -> R.string.hc_segment_weightlifting
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR -> R.string.hc_segment_wheelchair
    ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA -> R.string.hc_segment_yoga
    else -> R.string.hc_segment_unknown
}

@Composable
internal fun exerciseSegmentLabel(type: Int): String = stringResource(exerciseSegmentLabelRes(type))

internal fun exerciseSegmentLabel(context: Context, type: Int): String = context.getString(exerciseSegmentLabelRes(type))

@Composable
internal fun recordingMethodLabel(method: Int?): String = stringResource(
    recordingMethodLabelRes(method)
)

@Composable
internal fun deviceTypeLabel(type: Int?): String = stringResource(
    deviceTypeLabelRes(type)
)

internal fun recordingMethodLabelRes(method: Int?): Int = when (method) {
    Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> R.string.recording_actively_recorded
    Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> R.string.recording_automatically_recorded
    Metadata.RECORDING_METHOD_MANUAL_ENTRY -> R.string.recording_manual_entry
    Metadata.RECORDING_METHOD_UNKNOWN -> R.string.recording_unknown
    else -> R.string.not_available
}

internal fun deviceTypeLabelRes(type: Int?): Int = when (type) {
    Device.TYPE_WATCH -> R.string.device_watch
    Device.TYPE_PHONE -> R.string.device_phone
    Device.TYPE_SCALE -> R.string.device_scale
    Device.TYPE_RING -> R.string.device_ring
    Device.TYPE_HEAD_MOUNTED -> R.string.device_head_mounted
    Device.TYPE_FITNESS_BAND -> R.string.device_fitness_band
    Device.TYPE_CHEST_STRAP -> R.string.device_chest_strap
    Device.TYPE_SMART_DISPLAY -> R.string.device_smart_display
    Device.TYPE_UNKNOWN -> R.string.recording_unknown
    else -> R.string.not_available
}
