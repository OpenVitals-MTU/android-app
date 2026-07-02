package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.HealthConnectAccessGateMode

class HealthConnectFeatureTest {

    private val sleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val nutritionHydrationPermissions = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    private fun manager(
        minimum: Set<String> = setOf("core-a", "core-b", "core-c"),
    ): HealthConnectManager = mockk {
        every { minimumOnboardingPermissions } returns minimum
        every { corePermissions } returns minimum
        every { activityExtrasPermissions } returns emptySet()
        every { heartPermissions } returns emptySet()
        every { vitalsPermissions } returns emptySet()
        every { bodyPermissions } returns emptySet()
        every { nutritionHydrationPermissions } returns this@HealthConnectFeatureTest.nutritionHydrationPermissions
        every { mindfulnessPermissions } returns emptySet()
        every { cyclePermissions } returns emptySet()
        every { requestableWritePermissions } returns emptySet()
        every { dataImportWritePermissions } returns emptySet()
    }

    @Test
    fun sleepFeatureRequiresSleepReadPermission() {
        val permissions = HealthConnectFeature.SLEEP.requiredReadPermissions(manager())
        assertEquals(setOf(sleepPermission), permissions)
    }

    @Test
    fun hydrationFeatureRequiresHydrationAndNutritionReadPermissions() {
        val permissions = HealthConnectFeature.HYDRATION.requiredReadPermissions(manager())
        assertEquals(nutritionHydrationPermissions, permissions)
    }

    @Test
    fun buildStateShowsContextualPromptWhenCoreGrantedButFeatureMissing() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.SLEEP,
            manager = manager(),
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = true,
            grantedPermissions = setOf("core-a", "core-b", "core-c"),
            showDoubleCancelRecovery = false,
        )
        assertEquals(null, state.accessGateMode)
        assertTrue(state.showContextualPermissionPrompt)
        assertEquals(setOf(sleepPermission), state.contextualPromptPermissions)
    }

    @Test
    fun buildStateShowsAccessGateWhenSyncPaused() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.SLEEP,
            manager = manager(),
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = false,
            grantedPermissions = setOf("core-a", "core-b", "core-c", sleepPermission),
            showDoubleCancelRecovery = false,
        )
        assertEquals(HealthConnectAccessGateMode.SYNC_PAUSED, state.accessGateMode)
        assertFalse(state.showContextualPermissionPrompt)
    }
}
