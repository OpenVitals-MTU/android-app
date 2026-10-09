package tech.mmarca.openvitals.wear.features.recording

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.ui.graphics.vector.ImageVector
import tech.mmarca.openvitals.wear.R

enum class ActivityType(@param:StringRes val label: Int, val icon: ImageVector) {
    WALK(R.string.activity_walk, Icons.AutoMirrored.Outlined.DirectionsWalk),
    RUN(R.string.activity_run, Icons.AutoMirrored.Outlined.DirectionsRun),
    CYCLE(R.string.activity_cycle, Icons.AutoMirrored.Outlined.DirectionsBike),
    HIKE(R.string.activity_hike, Icons.Outlined.Hiking),
    WORKOUT(R.string.activity_workout, Icons.Outlined.FitnessCenter),
}
