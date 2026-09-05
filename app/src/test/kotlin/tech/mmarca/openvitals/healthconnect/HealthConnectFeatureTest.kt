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
        every { plannedExercisePermissions } returns setOf("planned-read", "planned-write")
    }

    @Test
    fun workoutPlansFeatureRequiresPlannedExercisePermissions() {
        val permissions = HealthConnectFeature.WORKOUT_PLANS.requiredReadPermissions(manager())
        assertEquals(setOf("planned-read", "planned-write"), permissions)
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

    // Fully permitted and available means pass-through: no gate mode, no prompt, nothing missing.
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

    // Kotlin diverges: resolveHealthConnectAccessGateMode() returns null for any non-AVAILABLE
    // availability. Unavailability is carried on state.availability and routed out of band.
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
        // No blocking gate mode.
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

    // Kotlin diverges: a missing feature permission raises a contextual prompt while the child stays visible.
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

    // The log grid no longer gates on the write set: each tile asks for its own writes on tap.
    @Test
    fun `manual entry requires no permissions to open`() {
        val manager = manager().also {
            every { it.requestableWritePermissions } returns setOf("write-a", "write-b")
        }
        assertEquals(emptySet<String>(), HealthConnectFeature.MANUAL_ENTRY.requiredReadPermissions(manager))
    }

    @Test
    fun `manual entry with no write permission shows the grid ungated`() {
        val manager = manager().also {
            every { it.requestableWritePermissions } returns setOf("write-a", "write-b")
        }
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.MANUAL_ENTRY,
            manager = manager,
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = true,
            grantedPermissions = emptySet(),
            showDoubleCancelRecovery = false,
        )
        assertNull(state.accessGateMode)
        assertFalse(state.showContextualPermissionPrompt)
        assertTrue(state.missingReadPermissions.isEmpty())
    }

    @Test
    fun `manual entry still pauses with sync`() {
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.MANUAL_ENTRY,
            manager = manager(),
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = false,
            grantedPermissions = emptySet(),
            showDoubleCancelRecovery = false,
        )
        assertEquals(HealthConnectAccessGateMode.SYNC_PAUSED, state.accessGateMode)
    }

    // The importer keeps its all-or-nothing gate: its writes are known up front.
    @Test
    fun `data import still gates on its write set`() {
        val manager = manager().also {
            every { it.dataImportWritePermissions } returns setOf("import-w")
        }
        val state = buildHealthConnectScreenUxState(
            feature = HealthConnectFeature.DATA_IMPORT,
            manager = manager,
            availability = HealthConnectAvailability.AVAILABLE,
            syncEnabled = true,
            grantedPermissions = emptySet(),
            showDoubleCancelRecovery = false,
        )
        assertEquals(HealthConnectAccessGateMode.INSUFFICIENT_ACCESS, state.accessGateMode)
        assertFalse(state.showContextualPermissionPrompt)
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
