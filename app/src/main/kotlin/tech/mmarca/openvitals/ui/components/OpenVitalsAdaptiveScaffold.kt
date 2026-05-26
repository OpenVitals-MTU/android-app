package tech.mmarca.openvitals.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource

data class OpenVitalsNavigationDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

data class MetricAction(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

data class ChartSemanticsSummary(
    val label: String,
    val value: String,
    val trend: String? = null,
) {
    val contentDescription: String =
        listOfNotNull(label, value, trend).joinToString(separator = ". ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenVitalsAdaptiveScaffold(
    title: String,
    navigationDestinations: List<OpenVitalsNavigationDestination>,
    currentRoute: String?,
    showTopBar: Boolean,
    showNavigation: Boolean,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit,
    navigationIcon: ImageVector,
    navigationContentDescription: String,
    action: MetricAction?,
    modifier: Modifier = Modifier,
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scaffoldContent: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (showTopBar) {
                    LargeTopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            if (canNavigateBack) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = navigationIcon,
                                        contentDescription = navigationContentDescription,
                                    )
                                }
                            }
                        },
                        actions = topBarActions,
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            floatingActionButton = {
                action?.let { spec ->
                    ExtendedFloatingActionButton(
                        onClick = spec.onClick,
                        icon = {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = null,
                            )
                        },
                        text = { Text(stringResource(spec.labelRes)) },
                    )
                }
            },
            content = content,
        )
    }

    if (showNavigation) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                navigationDestinations.forEach { destination ->
                    item(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                onNavigate(destination.route)
                            }
                        },
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize(),
        ) {
            scaffoldContent()
        }
    } else {
        scaffoldContent()
    }
}

@Composable
fun CompactMetricActionButton(
    action: MetricAction,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    if (expanded) {
        ExtendedFloatingActionButton(
            onClick = action.onClick,
            icon = { Icon(action.icon, contentDescription = null) },
            text = { Text(stringResource(action.labelRes)) },
            modifier = modifier,
        )
    } else {
        FloatingActionButton(
            onClick = action.onClick,
            modifier = modifier,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = stringResource(action.labelRes),
            )
        }
    }
}
