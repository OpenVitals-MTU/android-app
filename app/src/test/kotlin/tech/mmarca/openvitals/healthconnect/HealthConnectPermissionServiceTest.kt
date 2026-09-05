package tech.mmarca.openvitals.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.OnboardingCategoryId
import tech.mmarca.openvitals.domain.model.PermissionGrantMode

/**
 * The phased permission sets, the feature gates, and the mindfulness opt-in.
 *
 * Dart counterparts: test/data/source/health/health_permissions_test.dart and
 * test/data/source/health/mindfulness_opt_in_test.dart, plus the `availability`
 * feature-flag cases from health_connect_native_data_source_test.dart.
 *
 * Flutter splits this across a `HealthPermissionService` fed by a
 * `HealthConnectFeatureFlags` value object and a data source that resolves those
 * flags. Kotlin folds both halves into [HealthConnectPermissionService]: the
 * getters ask the feature gate directly, and the gate reads the provider on
 * demand. So a flag in Dart becomes a `getFeatureStatus` answer here, and the
 * opt-in becomes the `mindfulnessIntegrationEnabled` lambda.
 */
class HealthConnectPermissionServiceTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ── phased permission sets ──────────────────────────────────────────────

    // A bump is a deliberate act, not an accident: it re-prompts every existing
    // user with the new-permissions dialog. 3 = cycle writes became requestable,
    // 4 = the HRV write joined the vitals writes.
    @Test
    fun `PERMISSION_SET_VERSION is pinned`() {
        assertThat(HealthConnectPermissionService.PERMISSION_SET_VERSION).isEqualTo(4)
    }

    @Test
    fun `phase1 == core == steps, distance, exercise and sleep reads`() {
        val service = service()

        assertThat(service.phase1Permissions).isEqualTo(service.corePermissions)
        assertThat(service.corePermissions).containsExactly(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )
    }

    @Test
    fun `phase2 covers heart, body, activity-extras and nutrition-hydration`() {
        val service = service()

        assertThat(service.phase2Permissions).containsAtLeastElementsIn(service.heartPermissions)
        assertThat(service.phase2Permissions).containsAtLeastElementsIn(service.bodyPermissions)
        assertThat(service.phase2Permissions)
            .containsAtLeastElementsIn(service.activityExtrasPermissions)
        assertThat(service.phase2Permissions)
            .containsAtLeastElementsIn(service.nutritionHydrationPermissions)
    }

    @Test
    fun `phase3 == vitals reads, phase4 == cycle reads`() {
        val service = service()

        assertThat(service.phase3Permissions).isEqualTo(service.vitalsPermissions)
        assertThat(service.phase4Permissions).isEqualTo(service.cyclePermissions)
    }

    @Test
    fun `manual-only == route permissions, and drives the grant mode`() {
        val service = service()

        assertThat(service.manualOnlyPermissions).isEqualTo(service.routePermissions)
        assertThat(service.routePermissions).containsExactly(READ_EXERCISE_ROUTES)
        // Health Connect keeps route READS behind a setting no app may request.
        assertThat(service.grantModeFor(READ_EXERCISE_ROUTES))
            .isEqualTo(PermissionGrantMode.MANUAL)
        assertThat(service.grantModeFor(HealthPermission.getReadPermission(StepsRecord::class)))
            .isEqualTo(PermissionGrantMode.REQUESTABLE)
    }

    @Test
    fun `managed permissions include reads, writes and the route`() {
        val service = service()
        val managed = service.managedPermissions

        assertThat(managed).containsAtLeastElementsIn(service.corePermissions)
        assertThat(managed).containsAtLeastElementsIn(service.activityWritePermissions)
        assertThat(managed).containsAtLeastElementsIn(service.dataImportWritePermissions)
        assertThat(managed).contains(READ_EXERCISE_ROUTES)
    }

    @Test
    fun `the permission set the app asks for is the one the device supports`() {
        // Dart: resolveSupportedPermissions() diffs what the app wants against what
        // the provider will grant, and getting it wrong left onboarding stuck at
        // 9/11 with no way forward. Kotlin resolves the same taxonomy through the
        // feature gate, so this is the set the resolution starts from: it must be
        // the real taxonomy rather than a stub, and it must carry the two reads
        // every screen depends on.
        val managed = service().managedPermissions

        assertThat(managed).isNotEmpty()
        assertThat(managed.size).isGreaterThan(10)
        assertThat(managed).contains(HealthPermission.getReadPermission(SleepSessionRecord::class))
        assertThat(managed).contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
    }

    // ── feature gating ──────────────────────────────────────────────────────

    @Test
    fun `mindfulness is excluded from phase2 and the requestable writes when unavailable`() {
        val service = service(availableFeatures = emptySet())

        assertThat(service.phase2Permissions).doesNotContain(READ_MINDFULNESS)
        assertThat(service.requestableWritePermissions).doesNotContain(WRITE_MINDFULNESS)
    }

    @Test
    fun `mindfulness is included when the provider reports the feature available`() {
        val service = service(availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION))

        assertThat(service.phase2Permissions).contains(READ_MINDFULNESS)
    }

    // Kotlin 1.9.0 (1f2b435) moved the availability check into the getters
    // themselves, because the per-call-site guards had been forgotten in
    // allPermissions and managedPermissions — so an unsupported device still
    // asked for a mindfulness permission its provider does not define, and the
    // request could never be granted.
    @Test
    fun `mindfulness permissions are empty when the provider lacks the feature`() {
        val service = service(availableFeatures = emptySet())

        assertThat(service.mindfulnessPermissions).isEmpty()
        assertThat(service.mindfulnessWritePermissions).isEmpty()
    }

    @Test
    fun `an unavailable mindfulness leaks into NO permission set`() {
        val service = service(availableFeatures = emptySet())

        val sets = listOf(
            "allPermissions" to service.allPermissions,
            "managedPermissions" to service.managedPermissions,
            "requestableManagedPermissions" to service.requestableManagedPermissions,
            "onboardingRequestablePermissions" to service.onboardingRequestablePermissions,
            "dataImportWritePermissions" to service.dataImportWritePermissions,
            "phase2Permissions" to service.phase2Permissions,
            "requestableWritePermissions" to service.requestableWritePermissions,
            "minimumOnboardingPermissions" to service.minimumOnboardingPermissions,
        )
        sets.forEach { (name, set) ->
            assertWithMessage(name)
                .that(set.filter { it.contains("MINDFULNESS") })
                .isEmpty()
        }
        // And the onboarding catalog drops the category entirely rather than
        // offering an empty row.
        assertThat(service.onboardingPermissionCatalog().category(OnboardingCategoryId.MINDFULNESS))
            .isNull()
    }

    // The opt-in exists because some providers crash their own permission screen
    // on mindfulness, taking every other permission down with them. Onboarding
    // asks for everything in ONE request, so if mindfulness could get into it an
    // opted-in user on such a device could not grant anything at all.
    @Test
    fun `an AVAILABLE mindfulness still stays out of the required set`() {
        val service = service(availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION))

        assertThat(service.mindfulnessPermissions).isNotEmpty()
        assertThat(service.minimumOnboardingPermissions.filter { it.contains("MINDFULNESS") })
            .isEmpty()
        assertThat(
            service.onboardingPermissionCatalog().requiredPermissions
                .filter { it.contains("MINDFULNESS") },
        ).isEmpty()
    }

    // Dart: 'the device answer and the opt-in are separate flags'.
    //
    // These two were folded into one, and the fold was a shipping bug rather
    // than a divergence. `mindfulnessSupportedByDevice` decides whether
    // onboarding OFFERS the mindfulness step, and the opt-in toggle lives on
    // that step and nowhere else in onboarding — so deriving it from the
    // opt-in-gated answer meant a fresh install skipped the step forever and
    // was never offered the permission at all. The only way in was to finish
    // onboarding and find the toggle in Settings.
    @Test
    fun `the device answer and the opt-in stay separate flags`() {
        val deviceOnly = service(
            availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION),
            mindfulnessIntegrationEnabled = false,
        )

        // The opt-in still gates everything that DERIVES a permission set.
        assertThat(deviceOnly.isMindfulnessSessionAvailable()).isFalse()
        assertThat(deviceOnly.mindfulnessPermissions).isEmpty()
        assertThat(deviceOnly.mindfulnessWritePermissions).isEmpty()
        // But the device's own answer is reported unfolded, so the opt-in can be
        // offered on a phone that can honour it.
        assertThat(deviceOnly.isMindfulnessSessionSupportedByDevice()).isTrue()
        assertThat(deviceOnly.onboardingPermissionCatalog().mindfulnessSupportedByDevice).isTrue()

        val both = service(
            availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION),
            mindfulnessIntegrationEnabled = true,
        )
        assertThat(both.isMindfulnessSessionAvailable()).isTrue()
        assertThat(both.onboardingPermissionCatalog().mindfulnessSupportedByDevice).isTrue()
    }

    @Test
    fun `a phone whose provider lacks the feature is offered no opt-in at all`() {
        // The other half of the split: a toggle the device cannot honour must
        // not be shown, however the preference happens to read.
        val unsupported = service(
            availableFeatures = emptySet(),
            mindfulnessIntegrationEnabled = true,
        )

        assertThat(unsupported.isMindfulnessSessionSupportedByDevice()).isFalse()
        assertThat(unsupported.isMindfulnessSessionAvailable()).isFalse()
        assertThat(unsupported.onboardingPermissionCatalog().mindfulnessSupportedByDevice).isFalse()
    }

    @Test
    fun `Health Connect being unavailable reports no device support`() {
        val noProvider = service(
            availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION),
            mindfulnessIntegrationEnabled = true,
            availability = HealthConnectAvailability.NOT_SUPPORTED,
        )

        assertThat(noProvider.isMindfulnessSessionSupportedByDevice()).isFalse()
        assertThat(noProvider.onboardingPermissionCatalog().mindfulnessSupportedByDevice).isFalse()
    }

    @Test
    fun `cycle tracking stays out of the required set, both directions`() {
        val service = service()

        assertThat(service.minimumOnboardingPermissions.intersect(service.cyclePermissions))
            .isEmpty()
        assertThat(service.minimumOnboardingPermissions.intersect(CYCLE_WRITE_PERMISSIONS))
            .isEmpty()
        // But the import set still carries the cycle writes — the split is for
        // onboarding's benefit, not the CSV importer's.
        assertThat(service.dataImportWritePermissions)
            .containsAtLeastElementsIn(CYCLE_WRITE_PERMISSIONS)
    }

    @Test
    fun `cycle writes are a named requestable set since manual cycle entry`() {
        val service = service()

        assertThat(service.cycleWritePermissions).isEqualTo(CYCLE_WRITE_PERMISSIONS)
        assertThat(service.requestableWritePermissions)
            .containsAtLeastElementsIn(CYCLE_WRITE_PERMISSIONS)
        // The derived period record rides the flow write permission.
        assertThat(service.onboardingCycleCategoryPermissions)
            .contains(HealthPermission.getWritePermission(MenstruationPeriodRecord::class))
    }

    // The log's HRV tile writes HeartRateVariabilityRmssdRecord, so the
    // Settings "Manual entry write access" card must be able to grant it.
    @Test
    fun `the HRV write is a vitals write so the log tile and the Settings card agree`() {
        val service = service()
        val hrvWrite = HealthPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class)

        assertThat(service.vitalsWritePermissions).contains(hrvWrite)
        assertThat(service.requestableWritePermissions).contains(hrvWrite)
    }

    @Test
    fun `skin temperature is gated on the feature flag`() {
        assertThat(service().vitalsPermissions).doesNotContain(READ_SKIN_TEMPERATURE)

        val withSkin = service(availableFeatures = setOf(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE))
        assertThat(withSkin.vitalsPermissions).contains(READ_SKIN_TEMPERATURE)
    }

    // ── the mindfulness opt-in ──────────────────────────────────────────────
    //
    // A Health Connect module on a de-Googled ROM can DEFINE the mindfulness
    // permission and report FEATURE_MINDFULNESS_SESSION as available, while its
    // own permission screen has no category for it and throws
    // `IllegalArgumentException: No Category for fitness permission type
    // MINDFULNESS` the moment it is asked to draw a row for it. The system Health
    // Connect app dies, and the user can then grant this app *nothing at all*.
    //
    // The permission screen renders the permissions we ASK FOR, so not asking is
    // what keeps that phone usable. There is no API that tells us the UI is
    // broken, so the device's own "yes" is not enough on its own.

    @Test
    fun `with the integration off, mindfulness is never asked for`() {
        val service = optIn(enabled = false)

        assertThat(service.mindfulnessPermissions).isEmpty()
        assertThat(service.mindfulnessWritePermissions).isEmpty()
        // And — the part that actually matters — it is absent from the sets the
        // app hands to Health Connect when it asks for permissions. A permission
        // we do not request cannot be drawn, and cannot crash the screen drawing
        // it.
        assertThat(service.allPermissions.filter { it.contains("MINDFULNESS") }).isEmpty()
        assertThat(service.managedPermissions.filter { it.contains("MINDFULNESS") }).isEmpty()
    }

    @Test
    fun `with it on, and a device that supports it, we ask as before`() {
        val service = optIn(enabled = true)

        assertThat(service.mindfulnessPermissions).isNotEmpty()
        assertThat(service.mindfulnessWritePermissions).isNotEmpty()
        assertThat(service.allPermissions.filter { it.contains("MINDFULNESS") }).isNotEmpty()
    }

    @Test
    fun `turning it off costs mindfulness and nothing else`() {
        val off = optIn(enabled = false)
        val on = optIn(enabled = true)

        // Every other permission the app asks for is untouched: the point of the
        // opt-in is that the user can still grant the other twenty-odd metrics on
        // a phone whose Health Connect cannot cope with this one.
        val lostByOptingOut = on.allPermissions - off.allPermissions
        assertThat(lostByOptingOut).isNotEmpty()
        assertThat(lostByOptingOut.filter { !it.contains("MINDFULNESS") }).isEmpty()
    }

    @Test
    fun `a device that says YES is still refused while the user has not`() {
        // The reported phone: the module reports the feature available (it defines
        // the permission and the enum) and its permission UI still cannot draw it.
        // The device's answer alone must not be enough.
        val service = service(
            availableFeatures = setOf(
                HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION,
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
            ),
            mindfulnessIntegrationEnabled = false,
        )

        assertThat(service.isMindfulnessSessionAvailable()).isFalse()
        assertThat(service.mindfulnessPermissions).isEmpty()
        // Only mindfulness is withheld — the rest of the device's answer stands.
        assertThat(service.isSkinTemperatureAvailable()).isTrue()
        assertThat(service.vitalsPermissions).contains(READ_SKIN_TEMPERATURE)
    }

    @Test
    fun `a device that says NO is not offered the opt-in at all`() {
        val service = service(availableFeatures = emptySet(), mindfulnessIntegrationEnabled = false)

        assertThat(service.onboardingPermissionCatalog().mindfulnessSupportedByDevice).isFalse()
        assertThat(service.isMindfulnessSessionAvailable()).isFalse()
    }

    @Test
    fun `both halves say yes, and the feature comes back`() {
        val service = service(
            availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION),
            mindfulnessIntegrationEnabled = true,
        )

        assertThat(service.isMindfulnessSessionAvailable()).isTrue()
        assertThat(service.mindfulnessPermissions).isNotEmpty()
    }

    @Test
    fun `the user says yes but the device does not, still no`() {
        val service = service(availableFeatures = emptySet(), mindfulnessIntegrationEnabled = true)

        assertThat(service.isMindfulnessSessionAvailable()).isFalse()
        assertThat(service.mindfulnessPermissions).isEmpty()
    }

    // ── optional-feature availability ───────────────────────────────────────

    // Dart: 'resolveFeatureFlags reads optional-feature availability'. Kotlin has
    // no resolve step — the getters ask the provider on demand — so the flags are
    // read straight off the service.
    @Test
    fun `optional-feature availability is read per feature`() {
        val service = service(
            availableFeatures = setOf(
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION,
            ),
        )

        assertThat(service.isSkinTemperatureAvailable()).isTrue()
        assertThat(service.isPlannedExerciseAvailable()).isFalse()
        // Unlike Dart's data source, the Kotlin opt-in lambda DEFAULTS to on, so
        // an available device is available unless the user opted out.
        assertThat(service.isMindfulnessSessionAvailable()).isTrue()
        // A feature the provider does not have takes its permissions with it.
        assertThat(service.plannedExercisePermissions).isEmpty()
    }

    @Test
    fun `planned exercise permissions appear once the provider reports the feature`() {
        val service = service(availableFeatures = setOf(HealthConnectFeatures.FEATURE_PLANNED_EXERCISE))

        assertThat(service.isPlannedExerciseAvailable()).isTrue()
        assertThat(service.plannedExercisePermissions).containsExactly(
            HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class),
            HealthPermission.getWritePermission(PlannedExerciseSessionRecord::class),
        )
    }

    // Dart: 'UNKNOWN feature status resolves the flag to unavailable'. Health
    // Connect's Kotlin API has no UNKNOWN constant — it reports an int — so the
    // portable half is that the gate is `== FEATURE_STATUS_AVAILABLE`, and ANY
    // other answer, including one this version of the SDK has no name for, is
    // unavailable. A `!= UNAVAILABLE` gate would turn a provider too old to
    // answer into a feature we then ask permissions for.
    @Test
    fun `a feature status this SDK has no name for resolves to unavailable`() {
        val service = service(unknownStatusFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION))

        assertThat(service.isMindfulnessSessionAvailable()).isFalse()
        assertThat(service.mindfulnessPermissions).isEmpty()
    }

    // Every feature gate short-circuits on availability before it ever reaches
    // the provider: asking a client that does not exist is how the whole app
    // used to fall over on a phone without Health Connect.
    @Test
    fun `no feature is available while Health Connect itself is not`() {
        val service = service(
            availableFeatures = setOf(
                HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION,
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthConnectFeatures.FEATURE_PLANNED_EXERCISE,
            ),
            availability = HealthConnectAvailability.NOT_SUPPORTED,
        )

        assertThat(service.isMindfulnessSessionAvailable()).isFalse()
        assertThat(service.isSkinTemperatureAvailable()).isFalse()
        assertThat(service.isPlannedExerciseAvailable()).isFalse()
        assertThat(service.isHealthDataHistoryAvailable()).isFalse()
        assertThat(service.isBackgroundHealthDataReadAvailable()).isFalse()
        assertThat(service.additionalDataAccessPermissions).isEmpty()
    }

    // Dart's data source caches the resolved flags; Kotlin caches the raw
    // getFeatureStatus answer instead, and every getter goes through the cache.
    @Test
    fun `a feature status is asked of the provider only once`() {
        var calls = 0
        val service = service(
            availableFeatures = setOf(HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE),
            onFeatureStatusCall = { calls += 1 },
        )

        repeat(5) { assertThat(service.isSkinTemperatureAvailable()).isTrue() }

        assertThat(calls).isEqualTo(1)
    }

    // ── harness ─────────────────────────────────────────────────────────────

    private fun optIn(enabled: Boolean) = service(
        availableFeatures = setOf(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION),
        mindfulnessIntegrationEnabled = enabled,
    )

    private fun service(
        availableFeatures: Set<Int> = emptySet(),
        unknownStatusFeatures: Set<Int> = emptySet(),
        mindfulnessIntegrationEnabled: Boolean = true,
        availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
        onFeatureStatusCall: () -> Unit = {},
    ): HealthConnectPermissionService {
        val features = mockk<HealthConnectFeatures>()
        every { features.getFeatureStatus(any()) } answers {
            onFeatureStatusCall()
            val feature = firstArg<Int>()
            when {
                feature in unknownStatusFeatures -> UNNAMED_FEATURE_STATUS
                feature in availableFeatures -> HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
                else -> HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE
            }
        }
        val client = mockk<HealthConnectClient>()
        every { client.features } returns features

        val availabilityService = mockk<HealthConnectAvailabilityService>()
        every { availabilityService.availability() } returns availability

        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "test"

        return HealthConnectPermissionService(
            context = mockk<Context>(),
            clientProvider = { client },
            availabilityService = availabilityService,
            diagnostics = diagnostics,
            mindfulnessIntegrationEnabled = { mindfulnessIntegrationEnabled },
        )
    }

    private companion object {
        /**
         * A status int this version of the SDK has no constant for — the Kotlin
         * shape of Dart's `FeatureStatusMsg.unknown`, which a provider too old to
         * answer returns.
         */
        const val UNNAMED_FEATURE_STATUS = 0

        const val READ_EXERCISE_ROUTES = "android.permission.health.READ_EXERCISE_ROUTES"

        val READ_MINDFULNESS: String =
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
        val WRITE_MINDFULNESS: String =
            HealthPermission.getWritePermission(MindfulnessSessionRecord::class)
        val READ_SKIN_TEMPERATURE: String =
            HealthPermission.getReadPermission(SkinTemperatureRecord::class)

        /**
         * Spelled out independently of the code under test so a drifted
         * `cycleWritePermissions` set fails here instead of shifting silently.
         * The period write string is WRITE_MENSTRUATION, shared with flow.
         */
        val CYCLE_WRITE_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getWritePermission(MenstruationFlowRecord::class),
            HealthPermission.getWritePermission(MenstruationPeriodRecord::class),
            HealthPermission.getWritePermission(OvulationTestRecord::class),
            HealthPermission.getWritePermission(CervicalMucusRecord::class),
            HealthPermission.getWritePermission(BasalBodyTemperatureRecord::class),
            HealthPermission.getWritePermission(IntermenstrualBleedingRecord::class),
            HealthPermission.getWritePermission(SexualActivityRecord::class),
        )
    }
}
