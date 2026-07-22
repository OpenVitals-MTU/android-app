package tech.mmarca.openvitals.features.homewidgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import es.antonborri.home_widget.HomeWidgetGlanceState
import es.antonborri.home_widget.HomeWidgetGlanceStateDefinition
import es.antonborri.home_widget.HomeWidgetGlanceWidgetReceiver
import tech.mmarca.openvitals.R

/**
 * The three read-only status widgets, ported from the Kotlin `HomeReadinessWidgets.kt`.
 *
 * Only the composables are ported: Dart owns the data and pushes a snapshot into
 * the shared `HomeWidgetPreferences` (see [readHomeWidgetSnapshot]), so the Kotlin
 * loaders / Hilt entry points / `refresh*Widget` helpers have no counterpart here.
 * [HomeWidgetGlanceWidgetReceiver] already re-renders on the plugin's update
 * broadcast, which replaces Kotlin's `UpdatingHomeWidgetReceiver`.
 */

/** Key namespaces Dart writes under (`HomeWidgetId.storageKey` + `.`). */
private const val DailyReadinessPrefix = "daily_readiness."
private const val BodyEnergyPrefix = "body_energy."
private const val TodayVitalsPrefix = "today_vitals."

class HomeDailyReadinessWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()
    override val sizeMode = SizeMode.Responsive(HomeStatusWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeStatusWidgetContentFromState(
                prefix = DailyReadinessPrefix,
                fallback = { fallbackContext ->
                    fallbackStatusSnapshot(
                        context = fallbackContext,
                        title = fallbackContext.getString(R.string.screen_daily_readiness),
                        route = DailyReadinessRoute,
                    )
                },
            )
        }
    }
}

class HomeDailyReadinessWidgetReceiver :
    HomeWidgetGlanceWidgetReceiver<HomeDailyReadinessWidget>() {
    override val glanceAppWidget = HomeDailyReadinessWidget()
}

class HomeBodyEnergyWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()
    // EXACT, not Responsive: the curve is drawn to fit the width it is given, so
    // it needs the width the widget actually has. Responsive reports the largest
    // DECLARED size that fits, which on a 309dp widget is the 220dp bucket — and
    // the plot was then drawn 70dp wide inside a card nearly a third of a screen
    // across, with the rest of it empty.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            HomeBodyEnergyContentFromState()
        }
    }
}

class HomeBodyEnergyWidgetReceiver : HomeWidgetGlanceWidgetReceiver<HomeBodyEnergyWidget>() {
    override val glanceAppWidget = HomeBodyEnergyWidget()
}

class HomeTodayVitalsWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<HomeWidgetGlanceState> =
        HomeWidgetGlanceStateDefinition()
    override val sizeMode = SizeMode.Responsive(HomeTodayWidgetSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { HomeTodayVitalsContentFromState() }
    }
}

class HomeTodayVitalsWidgetReceiver : HomeWidgetGlanceWidgetReceiver<HomeTodayVitalsWidget>() {
    override val glanceAppWidget = HomeTodayVitalsWidget()
}

/**
 * Body Energy: the numbers, and the day's curve beside them.
 *
 * One layout at every size rather than a stacked variant for tall widgets. The
 * numbers are what the widget is for and the curve is context around them, so
 * the text column keeps what it needs and the curve takes the whole of the rest
 * — all the leftover width, all the height. Making it wider or taller grows the
 * plot; making it narrower shrinks the plot, and nothing else moves.
 *
 * Only when the leftover is too thin to hold a shape does the plot go.
 */
@Composable
private fun HomeBodyEnergyContentFromState() {
    val context = LocalContext.current
    val size = LocalSize.current
    val preferences = currentState<HomeWidgetGlanceState>().preferences
    val snapshot = preferences.readHomeWidgetSnapshot(BodyEnergyPrefix)
        ?: fallbackStatusSnapshot(
            context = context,
            title = context.getString(R.string.screen_body_energy),
            route = HomeWidgetSnapshot.DefaultRoute,
        )

    // Sized to the widest line it holds ("Charged: +34" at 11sp) rather than
    // padded out: every dp kept here is a dp the curve does not get.
    val textWidth = 108.dp
    val gap = 10.dp
    val plotWidth = size.width.value - 2 * BodyEnergyPadding.value - textWidth.value - gap.value
    val plotHeight = size.height.value - 2 * BodyEnergyPadding.value

    val plot = if (snapshot.series.size >= 2 && plotWidth >= BodyEnergyMinPlotWidth) {
        renderPlot(
            context = context,
            series = snapshot.series,
            widthDp = plotWidth,
            heightDp = plotHeight,
        )
    } else {
        null
    }
    if (plot == null) {
        HomeMetricWidgetContent(snapshot = snapshot)
        return
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HomeWidgetTokens.BackgroundProvider)
            .clickable(openRouteAction(context, snapshot.route))
            .padding(BodyEnergyPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(textWidth)) {
            Text(
                text = snapshot.title,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.MutedTextProvider,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = snapshot.value,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.PrimaryTextProvider,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            if (snapshot.subtitle.isNotBlank()) {
                Text(
                    text = snapshot.subtitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = HomeWidgetTokens.MutedTextProvider,
                        fontSize = 13.sp,
                    ),
                )
            }
            // Only where there is height to hold them. On a short widget these
            // are what gives way, not the score or the curve.
            if (plotHeight >= BodyEnergyRowsMinHeight) {
                snapshot.rows.take(2).forEach { row ->
                    Text(
                        text = row.displayText(),
                        maxLines = 1,
                        style = TextStyle(
                            color = HomeWidgetTokens.PrimaryTextProvider,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.width(gap))
        Image(
            provider = ImageProvider(plot),
            // Described by what it shows, not as "chart": a screen reader saying
            // "39 Low" reads the same thing the sighted user does.
            contentDescription = "${snapshot.value} ${snapshot.subtitle}".trim(),
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.width(plotWidth.dp).height(plotHeight.dp),
        )
    }
}

/** Rasterises the curve at the exact size it will be drawn at. */
private fun renderPlot(
    context: Context,
    series: List<Int>,
    widthDp: Float,
    heightDp: Float,
): android.graphics.Bitmap? {
    if (widthDp <= 0f || heightDp <= 0f) return null
    val density = context.resources.displayMetrics.density
    return BodyEnergyPlot.render(
        series = series,
        widthPx = (widthDp * density).toInt(),
        heightPx = (heightDp * density).toInt(),
        density = density,
    )
}

@Composable
private fun HomeStatusWidgetContentFromState(
    prefix: String,
    fallback: (Context) -> HomeWidgetSnapshot,
) {
    val context = LocalContext.current
    val preferences = currentState<HomeWidgetGlanceState>().preferences
    val snapshot = preferences.readHomeWidgetSnapshot(prefix) ?: fallback(context)
    HomeMetricWidgetContent(snapshot = snapshot)
}

@Composable
private fun HomeTodayVitalsContentFromState() {
    val context = LocalContext.current
    val preferences = currentState<HomeWidgetGlanceState>().preferences
    val snapshot = preferences.readHomeWidgetSnapshot(TodayVitalsPrefix)
        ?: todayVitalsFallbackSnapshot(context)
    HomeTodayVitalsContent(snapshot = snapshot)
}

/** Kotlin `HomeTodayVitalsContent` (HomeReadinessWidgets.kt:209-262). */
@Composable
private fun HomeTodayVitalsContent(snapshot: HomeWidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current
    val useTwoColumns = size.width >= 320.dp && snapshot.rows.size > 6
    val textSpec = todayVitalsTextSpec(size)
    val columnWidth = when {
        size.width >= 440.dp -> 192.dp
        size.width >= 400.dp -> 176.dp
        else -> 136.dp
    }
    val columnGap = if (textSpec.large) 18.dp else 10.dp
    val rows = snapshot.rows.take(
        if (useTwoColumns) TodayVitalsWidgetMaxRows else CompactTodayVitalsWidgetMaxRows,
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(HomeWidgetTokens.BackgroundProvider)
            .clickable(openRouteAction(context, snapshot.route))
            .padding(textSpec.contentPadding),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        Text(
            text = snapshot.title,
            maxLines = 1,
            style = TextStyle(
                color = HomeWidgetTokens.MutedTextProvider,
                fontSize = textSpec.titleFontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(textSpec.titleBottomSpacing))
        if (useTwoColumns) {
            val splitIndex = (rows.size + 1) / 2
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                TodayVitalsColumn(
                    rows = rows.take(splitIndex),
                    modifier = GlanceModifier.width(columnWidth),
                    textSpec = textSpec,
                )
                Spacer(modifier = GlanceModifier.width(columnGap))
                TodayVitalsColumn(
                    rows = rows.drop(splitIndex),
                    modifier = GlanceModifier.width(columnWidth),
                    textSpec = textSpec,
                )
            }
        } else {
            TodayVitalsColumn(
                rows = rows,
                modifier = GlanceModifier.fillMaxWidth(),
                textSpec = textSpec,
            )
        }
    }
}

@Composable
private fun TodayVitalsColumn(
    rows: List<HomeWidgetRow>,
    modifier: GlanceModifier,
    textSpec: TodayVitalsTextSpec,
) {
    Column(modifier = modifier) {
        rows.forEach { row ->
            TodayVitalsRow(row, textSpec)
        }
    }
}

@Composable
private fun TodayVitalsRow(
    row: HomeWidgetRow,
    textSpec: TodayVitalsTextSpec,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = textSpec.rowBottomPadding),
    ) {
        if (textSpec.large) {
            Text(
                text = row.label,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.MutedTextProvider,
                    fontSize = textSpec.labelFontSize,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = row.value,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.PrimaryTextProvider,
                    fontSize = textSpec.valueFontSize,
                    fontWeight = FontWeight.Bold,
                ),
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    maxLines = 1,
                    style = TextStyle(
                        color = HomeWidgetTokens.MutedTextProvider,
                        fontSize = textSpec.labelFontSize,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(textSpec.labelValueSpacing))
                Text(
                    text = row.value,
                    maxLines = 1,
                    style = TextStyle(
                        color = HomeWidgetTokens.PrimaryTextProvider,
                        fontSize = textSpec.valueFontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        if (row.subtitle.isNotBlank()) {
            Text(
                text = row.subtitle,
                maxLines = 1,
                style = TextStyle(
                    color = HomeWidgetTokens.MutedTextProvider,
                    fontSize = textSpec.subtitleFontSize,
                ),
            )
        }
    }
}

private fun todayVitalsTextSpec(size: DpSize): TodayVitalsTextSpec =
    if (size.height >= 280.dp || size.width >= 400.dp) {
        TodayVitalsTextSpec(
            large = true,
            contentPadding = 20.dp,
            titleFontSize = 22.sp,
            titleBottomSpacing = 18.dp,
            labelFontSize = 12.sp,
            valueFontSize = 18.sp,
            subtitleFontSize = 12.sp,
            rowBottomPadding = 13.dp,
            labelValueSpacing = 8.dp,
        )
    } else {
        TodayVitalsTextSpec(
            large = false,
            contentPadding = 16.dp,
            titleFontSize = 15.sp,
            titleBottomSpacing = 10.dp,
            labelFontSize = 10.sp,
            valueFontSize = 13.sp,
            subtitleFontSize = 9.sp,
            rowBottomPadding = 8.dp,
            labelValueSpacing = 6.dp,
        )
    }

private data class TodayVitalsTextSpec(
    val large: Boolean,
    val contentPadding: Dp,
    val titleFontSize: TextUnit,
    val titleBottomSpacing: Dp,
    val labelFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val subtitleFontSize: TextUnit,
    val rowBottomPadding: Dp,
    val labelValueSpacing: Dp,
)

/**
 * Fallback UIs, shown only until Dart's first push lands (Kotlin gates on the
 * same missing-`title` condition).
 */
private fun fallbackStatusSnapshot(
    context: Context,
    title: String,
    route: String,
): HomeWidgetSnapshot =
    HomeWidgetSnapshot(
        title = title,
        value = "--",
        unit = "",
        subtitle = context.getString(R.string.home_metric_widget_open_for_details),
        route = route,
    )

private fun fallbackRow(context: Context, label: String): HomeWidgetRow =
    HomeWidgetRow(
        label = label,
        value = "--",
        subtitle = context.getString(R.string.no_data),
    )

private fun todayVitalsFallbackSnapshot(context: Context): HomeWidgetSnapshot =
    HomeWidgetSnapshot(
        title = context.getString(R.string.home_widget_today_title),
        value = "",
        unit = "",
        subtitle = context.getString(R.string.home_metric_widget_open_for_details),
        route = HomeWidgetSnapshot.DefaultRoute,
        rows = listOf(
            fallbackRow(context, context.getString(R.string.screen_daily_readiness)),
            fallbackRow(context, context.getString(R.string.screen_body_energy)),
            fallbackRow(context, context.getString(R.string.metric_sleep)),
            fallbackRow(context, context.getString(R.string.metric_steps)),
            fallbackRow(context, context.getString(R.string.metric_distance)),
            fallbackRow(context, context.getString(R.string.metric_resting_heart_rate)),
            fallbackRow(context, context.getString(R.string.home_widget_hrv_short)),
            fallbackRow(context, context.getString(R.string.metric_weekly_cardio_load)),
            fallbackRow(context, context.getString(R.string.metric_hydration)),
        ),
    )

/** Kotlin `Screen.DailyReadiness.route`. */
private const val DailyReadinessRoute = "daily_readiness"

/**
 * The width at which the day curve replaces the rows.
 *
 * The larger of the widget's two declared sizes. Below it a 24-hour line is a
 * few pixels per hour, which reads as noise rather than as a shape.
 */
/** Below this the leftover strip is too thin to read a day off. */
private const val BodyEnergyMinPlotWidth = 48f

/** Below this there is no room for the start/charged lines under the score. */
private const val BodyEnergyRowsMinHeight = 96f

private val BodyEnergyPadding = 16.dp

private val HomeStatusWidgetSizes = setOf(
    DpSize(220.dp, 110.dp),
    DpSize(320.dp, 140.dp),
)

private val HomeTodayWidgetSizes = setOf(
    DpSize(320.dp, 280.dp),
    DpSize(420.dp, 320.dp),
    DpSize(520.dp, 360.dp),
)

private const val TodayVitalsWidgetMaxRows = HomeWidgetTokens.MaxRows
private const val CompactTodayVitalsWidgetMaxRows = 8
