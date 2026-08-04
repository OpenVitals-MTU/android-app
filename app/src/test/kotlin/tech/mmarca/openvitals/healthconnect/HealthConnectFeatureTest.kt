package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.ui.components.HealthConnectAccessGateMode
import tech.mmarca.openvitals.ui.components.shouldShowDashboardHealthConnectPromo

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
    fun caffeineFeatureRequiresNutritionReadPermission() {
        val permissions = HealthConnectFeature.CAFFEINE.requiredReadPermissions(manager())
        assertEquals(setOf(HealthPermission.getReadPermission(NutritionRecord::class)), permissions)
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

    // Dart: 'shows the child when available and permitted'
    // (test/ui/components/health_connect_gate_test.dart). Fully permitted and
    // available means pass-through: no gate mode (HealthConnectAccessGate renders
    // its content when mode == null), no contextual prompt, nothing missing.
    @Test
    fun `shows the child when available and permitted`() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.SLEEP,
            manager = manager(),
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = true,
            grantedPermissions = setOf("core-a", "core-b", "core-c", sleepPermission),
            showDoubleCancelRecovery = false,
        )
        assertNull(state.accessGateMode)
        assertFalse(state.showContextualPermissionPrompt)
        assertTrue(state.missingReadPermissions.isEmpty())
        assertTrue(state.contextualPromptPermissions.isEmpty())
        assertFalse(state.syncPaused)
    }

    // Dart: 'shows the access gate when Health Connect is unavailable'. Kotlin
    // DIVERGES deliberately: resolveHealthConnectAccessGateMode() returns null for
    // any non-AVAILABLE availability, so no blocking access gate exists at this
    // seam. Unavailability is carried on state.availability and routed out of band
    // (the onboarding screen gates on it, and the dashboard surfaces the Health
    // Connect promo). This test pins that routing.
    @Test
    fun `unavailable Health Connect bypasses the access gate and surfaces availability on the state`() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.SLEEP,
            manager = manager(),
            availability = HealthConnectAvailability.NOT_SUPPORTED,
            syncEnabled = true,
            grantedPermissions = emptySet(),
            showDoubleCancelRecovery = false,
        )
        // No blocking gate mode - the gate is not this state's channel for
        // unavailability, unlike the Dart HealthConnectGate.
        assertNull(state.accessGateMode)
        // The availability is surfaced verbatim for the screens that do gate on it.
        assertEquals(HealthConnectAvailability.NOT_SUPPORTED, state.availability)
        // Sync-paused presentation is reserved for an AVAILABLE provider.
        assertFalse(state.syncPaused)
        // The surface that does gate on unavailability: the dashboard promo.
        assertTrue(
            shouldShowDashboardHealthConnectPromo(
                availability = HealthConnectAvailability.NOT_SUPPORTED,
                syncEnabled = true,
                minimumPermissionsGranted = false,
            )
        )
    }

    // Dart: 'shows the permission gate when a required permission is missing'
    // hides the child behind a blocking 'Permissions needed' gate. Kotlin DIVERGES
    // deliberately: a missing feature permission raises a contextual prompt while
    // the child stays visible (accessGateMode == null means HealthConnectAccessGate
    // renders its content). These assertions pin that divergence.
    @Test
    fun `a missing required permission raises the contextual prompt over a still-visible child`() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.SLEEP,
            manager = manager(),
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = true,
            grantedPermissions = setOf("core-a", "core-b", "core-c"),
            showDoubleCancelRecovery = false,
        )
        // Gate null -> the child is shown, not blocked.
        assertNull(state.accessGateMode)
        // The ask happens contextually, above the content.
        assertTrue(state.showContextualPermissionPrompt)
        assertEquals(setOf(sleepPermission), state.contextualPromptPermissions)
        assertEquals(setOf(sleepPermission), state.missingReadPermissions)
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
