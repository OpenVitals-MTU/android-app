package tech.mmarca.openvitals.features.vitals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.data.model.BloodPressureEntry
import tech.mmarca.openvitals.data.model.BodyTempEntry
import tech.mmarca.openvitals.data.model.RespiratoryRateEntry
import tech.mmarca.openvitals.data.model.SpO2Entry
import tech.mmarca.openvitals.data.model.TimeRange
import tech.mmarca.openvitals.data.model.Vo2MaxEntry
import tech.mmarca.openvitals.ui.components.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.periodTitle
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEE d")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val oxygenColor = Color(0xFF00897B)
private val respiratoryColor = Color(0xFF5E97F6)
private val temperatureColor = Color(0xFFFF7043)
private val vo2Color = Color(0xFF7E57C2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(viewModel: VitalsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val requestPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onPermissionsResult(granted)
    }

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
        headerItems = {
            if (state.missingPermissions.isNotEmpty()) {
                item {
                    PermissionCallout(
                        title = "Vitals permissions needed",
                        body = "Grant blood pressure, oxygen saturation, respiratory rate, temperature, and VO2 max permissions to fill this screen.",
                        onGrant = { requestPermissions.launch(viewModel.phase3Permissions) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        },
    ) { period ->
        if (!state.hasData && !state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = "Vitals",
                    icon = Icons.Outlined.Favorite,
                    accentColor = VitalsColor,
                    message = "No vitals were recorded for this period.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.bloodPressure.isNotEmpty() || state.spO2.isNotEmpty() || state.vo2Max.isNotEmpty()) {
            item { SectionHeader("Cardiovascular") }
            item {
                VitalsSummaryRow(
                    first = state.latestBloodPressure?.let {
                        SummaryMetric("Blood pressure", "${it.systolicMmHg}/${it.diastolicMmHg}", "mmHg", Icons.Outlined.Favorite, VitalsColor, it.source)
                    },
                    second = state.latestSpO2?.let {
                        SummaryMetric("SpO2", "%.1f".format(it.percent), "%", Icons.Outlined.Favorite, oxygenColor, it.source)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.bloodPressure.isNotEmpty()) {
                item {
                    BloodPressureChart(
                        entries = state.bloodPressure,
                        selectedRange = state.selectedRange,
                        period = period,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.spO2.isNotEmpty()) {
                item {
                    VitalsLineChart(
                        title = "Oxygen saturation",
                        entries = state.spO2.sortedBy { it.time },
                        values = state.spO2.sortedBy { it.time }.map { it.percent },
                        labels = state.spO2.sortedBy { it.time }.map { dayFormatter.format(it.time.atZone(ZoneId.systemDefault())) },
                        accentColor = oxygenColor,
                        summary = "${periodTitle(state.selectedRange, period)} · %.1f%% avg".format(state.spO2.map { it.percent }.average()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            state.latestVo2Max?.let { latest ->
                item {
                    MetricCard(
                        title = "VO2 max",
                        value = "%.1f".format(latest.vo2MaxMlPerKgPerMin),
                        unit = "mL/kg/min",
                        icon = Icons.Outlined.Speed,
                        accentColor = vo2Color,
                        source = latest.source,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (state.respiratoryRate.isNotEmpty() || state.bodyTemperature.isNotEmpty()) {
            item { SectionHeader("Respiratory") }
            item {
                VitalsSummaryRow(
                    first = state.latestRespiratoryRate?.let {
                        SummaryMetric("Respiratory rate", "%.1f".format(it.breathsPerMinute), "br/min", Icons.Outlined.Air, respiratoryColor, it.source)
                    },
                    second = state.latestBodyTemperature?.let {
                        SummaryMetric("Body temp", "%.1f".format(it.temperatureCelsius), "deg C", Icons.Outlined.DeviceThermostat, temperatureColor, it.source)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.respiratoryRate.isNotEmpty()) {
                item {
                    SimpleVitalsList(
                        title = "Respiratory rate readings",
                        entries = state.respiratoryRate,
                        value = { "%.1f br/min".format(it.breathsPerMinute) },
                        source = { it.source },
                        time = { it.time },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.bodyTemperature.isNotEmpty()) {
                item {
                    SimpleVitalsList(
                        title = "Body temperature readings",
                        entries = state.bodyTemperature,
                        value = { "%.1f deg C".format(it.temperatureCelsius) },
                        source = { it.source },
                        time = { it.time },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (state.vo2Max.size > 1) {
            item { SectionHeader("VO2 max history") }
            items(state.vo2Max.sortedByDescending { it.time }) { entry ->
                VitalsReadingRow(
                    label = "%.1f mL/kg/min".format(entry.vo2MaxMlPerKgPerMin),
                    source = entry.source,
                    time = entry.time.atZone(ZoneId.systemDefault()),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private data class SummaryMetric(
    val title: String,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color,
    val source: String,
)

@Composable
private fun VitalsSummaryRow(
    first: SummaryMetric?,
    second: SummaryMetric?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryMetricCard(first, Modifier.weight(1f))
        SummaryMetricCard(second, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryMetricCard(metric: SummaryMetric?, modifier: Modifier = Modifier) {
    if (metric == null) {
        MetricCardPlaceholder(
            title = "No data",
            icon = Icons.Outlined.Favorite,
            accentColor = MaterialTheme.colorScheme.outline,
            message = "No readings in this period.",
            modifier = modifier,
        )
    } else {
        MetricCard(
            title = metric.title,
            value = metric.value,
            unit = metric.unit,
            icon = metric.icon,
            accentColor = metric.color,
            source = metric.source,
            modifier = modifier,
        )
    }
}

@Composable
private fun BloodPressureChart(
    entries: List<BloodPressureEntry>,
    selectedRange: TimeRange,
    period: DatePeriod,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val max = sorted.maxOfOrNull { it.systolicMmHg }?.coerceAtLeast(140) ?: 140
    val min = sorted.minOfOrNull { it.diastolicMmHg }?.coerceAtMost(60) ?: 60
    val range = (max - min).coerceAtLeast(1)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Blood pressure", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            ) {
                if (sorted.isEmpty()) return@Canvas
                val stepX = if (sorted.size > 1) size.width / (sorted.size - 1) else size.width
                sorted.forEachIndexed { index, entry ->
                    val x = if (sorted.size > 1) index * stepX else size.width / 2f
                    val ySystolic = size.height * (1f - (entry.systolicMmHg - min).toFloat() / range)
                    val yDiastolic = size.height * (1f - (entry.diastolicMmHg - min).toFloat() / range)
                    drawLine(
                        color = VitalsColor.copy(alpha = 0.35f),
                        start = Offset(x, ySystolic),
                        end = Offset(x, yDiastolic),
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = VitalsColor, radius = 4.dp.toPx(), center = Offset(x, ySystolic))
                    drawCircle(color = HeartColor, radius = 4.dp.toPx(), center = Offset(x, yDiastolic))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${periodTitle(selectedRange, period)} · ${sorted.size} readings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> VitalsLineChart(
    title: String,
    entries: List<T>,
    values: List<Double>,
    labels: List<String>,
    accentColor: Color,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val min = values.minOrNull()?.coerceAtMost(max - 1.0) ?: 0.0
    val range = (max - min).coerceAtLeast(1.0)
    val stride = if (entries.size > 14) 5 else 1

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (values.size < 2) return@Canvas
                val points = values.mapIndexed { index, value ->
                    Offset(
                        x = index * size.width / (values.size - 1),
                        y = size.height * (1f - ((value - min) / range).toFloat()),
                    )
                }
                for (index in 0 until points.size - 1) {
                    drawLine(
                        color = accentColor,
                        start = points[index],
                        end = points[index + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                points.forEach { point -> drawCircle(color = accentColor, radius = 4.dp.toPx(), center = point) }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                labels.forEachIndexed { index, label ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (index % stride == 0 || index == labels.lastIndex) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> SimpleVitalsList(
    title: String,
    entries: List<T>,
    value: (T) -> String,
    source: (T) -> String,
    time: (T) -> java.time.Instant,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            entries.sortedByDescending(time).take(8).forEach { entry ->
                VitalsReadingRow(
                    label = value(entry),
                    source = source(entry),
                    time = time(entry).atZone(ZoneId.systemDefault()),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun VitalsReadingRow(
    label: String,
    source: String,
    time: java.time.ZonedDateTime,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${dateFormatter.format(time)} · ${timeFormatter.format(time)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        SourceChip(source = source)
    }
}
