package tech.mmarca.openvitals.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import android.os.Build
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: WearSensorManager

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
        if (Manifest.permission.BODY_SENSORS !in missing) sensorManager.startListening()

        // Nothing feeds the screens yet. A debug build shows sample values so
        // the layouts can be judged on a watch; a release build shows empty ones.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val uiState = if (debuggable) SampleData.uiState else WearUiState()

        setContent {
            OpenVitalsWearApp(state = uiState)
        }
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
        if (Manifest.permission.BODY_SENSORS in granted) sensorManager.startListening()
        WearAppService.startIfPermitted(this)
    }

    private fun requiredPermissions(): List<String> = buildList {
        add(Manifest.permission.BODY_SENSORS)
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
        if (::sensorManager.isInitialized && checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.startListening()
        }
    }

    private companion object {
        const val PERMISSIONS_REQUEST = 1
    }
}
