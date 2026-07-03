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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.features.manualentry.hydration.hydrationAmountLabel
import tech.mmarca.openvitals.features.manualentry.hydration.isValidCustomHydrationDrink

class HomeQuickBeverageWidgetConfigurationActivity : AppCompatActivity() {
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

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            HomeQuickBeverageWidgetEntryPoint::class.java,
        )
        val drinks = entryPoint.hydrationRepository()
            .customHydrationDrinks()
            .filter(CustomHydrationDrink::isValidCustomHydrationDrink)
        val unitFormatter = entryPoint.unitFormatter()

        title = getString(R.string.home_quick_beverage_widget_config_title)
        val labels = drinks.map { drink ->
            "${drink.name} - ${hydrationAmountLabel(drink.volumeLiters, unitFormatter)}"
        }
        val listView = ListView(this).apply {
            adapter = ArrayAdapter(
                this@HomeQuickBeverageWidgetConfigurationActivity,
                android.R.layout.simple_list_item_1,
                labels,
            )
            setOnItemClickListener { _, _, position, _ ->
                configure(drinks[position])
            }
        }
        val header = TextView(this).apply {
            text = getString(R.string.home_quick_beverage_widget_config_prompt)
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

        if (drinks.isEmpty()) {
            setContentView(
                TextView(this).apply {
                    text = getString(R.string.home_quick_beverage_widget_no_drinks)
                    setPadding(32, 32, 32, 32)
                }
            )
        }
    }

    private fun configure(drink: CustomHydrationDrink) {
        homeQuickBeverageWidgetSelection(this).setDrink(appWidgetId, drink.id)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, result)
        finish()
        refreshConfiguredWidget(appWidgetId, drink.id)
    }

    private fun refreshConfiguredWidget(appWidgetId: Int, drinkId: String) {
        val appContext = applicationContext
        fun updateGlance() {
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                refreshHomeQuickBeverageWidget(appContext, appWidgetId, drinkId)
            }
        }
        val refresh = {
            appContext.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = ComponentName(appContext, HomeQuickBeverageWidgetReceiver::class.java)
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
