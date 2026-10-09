package tech.mmarca.openvitals.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.wear.health.HeartRatePermission

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: WearSensorManager
    private val viewModel: WearAppViewModel by viewModels()

    /** A screen a tile asked for, opened once and then cleared. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Identify the device manufacturer and instantiate the correct manager
        val manufacturer = Build.MANUFACTURER.lowercase()
        sensorManager = when {
            manufacturer.contains("samsung") -> {
                Log.d("MainActivity", "Samsung device detected. Using GalaxySensorManager.")
                GalaxySensorManager(this) { data ->
                    Log.d("MainActivity", "Data received: $data")
                }
            }
            manufacturer.contains("google") -> {
                Log.d("MainActivity", "Google device detected. Using PixelSensorManager.")
                PixelSensorManager(this) { data ->
                    Log.d("MainActivity", "Data received: $data")
                }
            }
            else -> {
                Log.d("MainActivity", "Generic/Other device detected ($manufacturer). Using default WearSensorManager.")
                WearSensorManager(this) { data ->
                    Log.d("MainActivity", "Data received: $data")
                }
            }
        }

        // Sensors, plus what the phone link needs. Each one starts its part once granted.
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), PERMISSIONS_REQUEST)
        if (HeartRatePermission !in missing) sensorManager.startListening()

        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            OpenVitalsWearApp(
                state = uiState,
                onLog = viewModel::log,
                deepLinkRoute = pendingRoute,
                onDeepLinkHandled = { pendingRoute = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST) return
        val granted = permissions.filterIndexed { index, _ ->
            grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
        }
        if (HeartRatePermission in granted) sensorManager.startListening()
        WearAppService.startIfPermitted(this)
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(HeartRatePermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
        // The link's ongoing notification; the service runs without it, just unseen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onPause() {
        super.onPause()
        if (::sensorManager.isInitialized) {
            sensorManager.stopListening()
        }
    }

    override fun onResume() {
        super.onResume()
        // Also catches a grant made in the system settings.
        WearAppService.startIfPermitted(this)
        if (::sensorManager.isInitialized && checkSelfPermission(HeartRatePermission) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.startListening()
        }
    }

    companion object {
        /** Extra a tile sets to open a screen; see [WearRoutes.isDeepLinkable]. */
        const val EXTRA_ROUTE = "tech.mmarca.openvitals.wear.ROUTE"
        private const val PERMISSIONS_REQUEST = 1
    }
}
