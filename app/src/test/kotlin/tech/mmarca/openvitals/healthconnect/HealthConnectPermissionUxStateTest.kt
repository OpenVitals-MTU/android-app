package tech.mmarca.openvitals.healthconnect

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository

class HealthConnectPermissionUxStateTest {
    @Test
    fun `double cancel recovery appears after two cancellations`() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        every { prefs.healthConnectPermissionCancelCount } returnsMany listOf(0, 1, 2)

        val state = HealthConnectPermissionUxState(prefs)

        state.recordPermissionRequestCancelled()
        assertFalse(state.shouldShowDoubleCancelRecovery())

        state.recordPermissionRequestCancelled()
        assertTrue(state.shouldShowDoubleCancelRecovery())
    }

    @Test
    fun `granted permissions reset cancel count`() {
        val prefs = mockk<PreferencesRepository>(relaxed = true)
        val state = HealthConnectPermissionUxState(prefs)

        state.recordPermissionRequestGranted()

        verify { prefs.healthConnectPermissionCancelCount = 0 }
    }
}
