package tech.mmarca.openvitals.features.homewidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId

class HomeMetricWidgetConfigurationActivity : AppCompatActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        title = getString(R.string.home_metric_widget_config_title)
        val metrics = homeMetricWidgetCatalog()
        val labels = metrics.map { metric -> getString(metric.homeMetricTitleRes()) }
        val listView = ListView(this).apply {
            adapter = ArrayAdapter(
                this@HomeMetricWidgetConfigurationActivity,
                android.R.layout.simple_list_item_1,
                labels,
            )
            setOnItemClickListener { _, _, position, _ ->
                configure(metrics[position])
            }
        }
        val header = TextView(this).apply {
            text = getString(R.string.home_metric_widget_config_prompt)
            textSize = 20f
            setPadding(32, 28, 32, 20)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                header,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            addView(
                listView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                ),
            )
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setContentView(root)

        if (metrics.isEmpty()) {
            setContentView(
                TextView(this).apply {
                    text = getString(R.string.home_metric_widget_no_metrics)
                    setPadding(32, 32, 32, 32)
                }
            )
        }
    }

    private fun configure(metricId: DashboardWidgetId) {
        homeMetricWidgetSelection(this).setMetric(appWidgetId, metricId)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
        refreshConfiguredWidget(appWidgetId, metricId)
    }

    private fun refreshConfiguredWidget(appWidgetId: Int, metricId: DashboardWidgetId) {
        val appContext = applicationContext
        fun updateGlance() {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                refreshHomeMetricWidget(appContext, appWidgetId, metricId)
            }
        }
        val refresh = {
            appContext.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = ComponentName(appContext, HomeMetricWidgetReceiver::class.java)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                }
            )
        }
        updateGlance()
        refresh()
        Handler(Looper.getMainLooper()).postDelayed(
            {
                updateGlance()
                refresh()
            },
            ConfiguredWidgetRefreshDelayMillis,
        )
    }

    private companion object {
        const val ConfiguredWidgetRefreshDelayMillis = 1_000L
    }
}
