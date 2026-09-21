package tech.mmarca.openvitals.sensors.ble

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

/** A saved sensor and a revoked Nearby devices grant used to crash the recording on connectGatt. */
class BlePermissionsTest {

    private fun missing(
        sdkInt: Int = 34,
        notifications: Boolean = true,
        bluetooth: Boolean = true,
        scan: Boolean = true,
        sensors: Boolean = true,
    ) = recordingRuntimePermissionsToRequest(
        sdkInt = sdkInt,
        hasNotificationPermission = notifications,
        hasBluetoothConnectPermission = bluetooth,
        hasBluetoothScanPermission = scan,
        hasSavedBleSensors = sensors,
    )

    @Test
    fun `nothing is asked for when every grant is held`() {
        assertEquals(emptyList<String>(), missing())
    }

    @Test
    fun `the bluetooth grant is asked for only when a sensor is saved`() {
        assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), missing(bluetooth = false))
        assertEquals(emptyList<String>(), missing(bluetooth = false, sensors = false))
    }

    /** After a reboot only a scan finds a sensor. Without the grant it stayed on "Connecting". */
    @Test
    fun `the scan grant is asked for with the connect grant`() {
        assertEquals(listOf(Manifest.permission.BLUETOOTH_SCAN), missing(scan = false))
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
            missing(bluetooth = false, scan = false),
        )
        assertEquals(emptyList<String>(), missing(scan = false, sensors = false))
        assertEquals(emptyList<String>(), missing(sdkInt = 30, scan = false))
    }

    @Test
    fun `both are asked for in one request`() {
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_CONNECT),
            missing(notifications = false, bluetooth = false),
        )
    }

    @Test
    fun `an Android version that lacks a grant is not asked for it`() {
        // The Nearby devices grant arrived with Android 12, notifications with Android 13.
        assertEquals(emptyList<String>(), missing(sdkInt = 30, notifications = false, bluetooth = false))
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_CONNECT),
            missing(sdkInt = 31, notifications = false, bluetooth = false),
        )
    }
}
