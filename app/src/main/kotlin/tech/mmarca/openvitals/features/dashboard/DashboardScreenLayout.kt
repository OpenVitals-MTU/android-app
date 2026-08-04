package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

internal const val DashboardCarouselEdgeScrollDelayMillis = 450L
internal val DashboardCarouselEdgeScrollThreshold = 56.dp
internal val DashboardScreenPadding = 16.dp
internal val DashboardSectionSeparatorSpacing = 4.dp
internal val DashboardQuickActionHeight = 48.dp
internal val DashboardActionsSpacing = 12.dp
internal val DashboardQuickActionIconSize = 18.dp

// Tighter than the default button padding so a longer label such as "Start workout"
// fits on a single line within the weighted quick-action buttons.
internal val DashboardQuickActionContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
