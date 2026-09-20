package tech.mmarca.openvitals.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material3.MaterialTheme
import android.Manifest
import android.content.pm.PackageManager
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

        // test for sensor permission
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            Log.d("TAG___", "ALREADY GRANTED")
            sensorManager.startListening()
        }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.open_vitals_launcher_prod),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.75f),
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::sensorManager.isInitialized) {
            sensorManager.stopListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::sensorManager.isInitialized && checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.startListening()
        }
    }
}
