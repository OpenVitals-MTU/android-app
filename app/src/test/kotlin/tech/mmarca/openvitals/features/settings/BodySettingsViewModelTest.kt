package tech.mmarca.openvitals.features.settings

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyRepository
import tech.mmarca.openvitals.data.sync.BodyEnergyChainSyncService
import tech.mmarca.openvitals.data.sync.DerivedMetricsResetService
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class BodySettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test fun `setNightStartHour persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setNightStartHour(20)

        verify { prefs.nightStartHour = 20 }
        assertEquals(20, vm.uiState.value.nightStartHour)
    }

    @Test fun `setNightStartHour wraps around midnight`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setNightStartHour(-1)

        verify { prefs.nightStartHour = 23 }
        assertEquals(23, vm.uiState.value.nightStartHour)
    }

    @Test fun `setNightEndHour persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setNightEndHour(24)

        verify { prefs.nightEndHour = 0 }
        assertEquals(0, vm.uiState.value.nightEndHour)
    }

    @Test fun `high threshold cannot drop within the gap of the low threshold`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        // Low defaults to 50, gap 5: 40 is gap-clamped to 55, then the repository floor (80) wins.
        vm.setHighHeartRateThresholdBpm(40)

        assertEquals(80, vm.uiState.value.highHeartRateThresholdBpm)
    }

    @Test fun `low threshold cannot rise within the gap of the high threshold`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        // High defaults to 120, gap 5: 130 is gap-clamped to 115, then the repository ceiling (100) wins.
        vm.setLowHeartRateThresholdBpm(130)

        assertEquals(100, vm.uiState.value.lowHeartRateThresholdBpm)
    }

    @Test fun `low threshold gap clamp lands inside the repository bounds`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        // High at 90, then low asked above it: the gap forces 85, which proves the gap clamp.
        vm.setHighHeartRateThresholdBpm(90)
        vm.setLowHeartRateThresholdBpm(95)

        assertEquals(85, vm.uiState.value.lowHeartRateThresholdBpm)
    }

    @Test fun `a legitimate threshold change still lands unchanged`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        // A well-separated pair inside the bounds must land exactly as asked.
        vm.setHighHeartRateThresholdBpm(150)
        vm.setLowHeartRateThresholdBpm(45)

        assertEquals(150, vm.uiState.value.highHeartRateThresholdBpm)
        assertEquals(45, vm.uiState.value.lowHeartRateThresholdBpm)
    }

    @Test fun `threshold gap clamp lands inside the repository bounds`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        // Low at 90, then high asked below it: the gap forces 95.
        vm.setLowHeartRateThresholdBpm(90)
        vm.setHighHeartRateThresholdBpm(80)

        assertEquals(95, vm.uiState.value.highHeartRateThresholdBpm)
    }

    @Test fun `threshold steps persist through the repository clamp`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        vm.setHighHeartRateThresholdBpm(500)

        assertEquals(220, vm.uiState.value.highHeartRateThresholdBpm)
    }

    @Test fun `updateBodyProfile writes measurements only on change and with permission`() = runTest {
        val prefs = prefs()
        val bodyRepository = bodyRepo().also { repo ->
            coEvery { repo.hasBodyWritePermission(any()) } returns true
        }
        val vm = viewModel(preferencesRepository = prefs, bodyRepository = bodyRepository)
        advanceUntilIdle()

        val saved = BodyProfile(weightKg = 80.0, heightCm = 180.0)
        every { prefs.bodyProfile() } returns saved
        vm.updateBodyProfile(saved)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            bodyRepository.writeBodyMeasurementEntry(
                match { it.type == BodyMeasurementType.WEIGHT && it.value == 80.0 },
            )
        }
        coVerify(exactly = 1) {
            bodyRepository.writeBodyMeasurementEntry(
                match { it.type == BodyMeasurementType.HEIGHT && it.value == 180.0 },
            )
        }

        // Saving the unchanged profile again must not write duplicates.
        vm.updateBodyProfile(saved)
        advanceUntilIdle()
        coVerify(exactly = 2) { bodyRepository.writeBodyMeasurementEntry(any()) }
    }

    @Test fun `updateCaffeinePreferences persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)
        val caffeinePreferences = CaffeinePreferences(
            profileCompleted = true,
            halfLifeMinutes = 360,
            sleepThresholdMg = 45,
        )

        vm.updateCaffeinePreferences(caffeinePreferences)

        verify { prefs.setCaffeinePreferences(caffeinePreferences) }
        assertEquals(caffeinePreferences, vm.uiState.value.caffeinePreferences)
    }

    @Test fun `saving an out-of-range half-life reseeds from the clamped stored value`() = runTest {
        val vm = viewModel(preferencesRepository = prefs())

        vm.updateCaffeinePreferences(CaffeinePreferences(halfLifeMinutes = 9000))

        // The card shows the stored value, not the typed one.
        assertEquals(
            CaffeinePreferences.MaxHalfLifeMinutes,
            vm.uiState.value.caffeinePreferences.halfLifeMinutes,
        )
    }

    @Test fun `build seeds the caffeine preferences and the body profile from storage`() = runTest {
        val prefs = prefs()
        val stored = CaffeinePreferences(profileCompleted = true, halfLifeMinutes = 420)
        prefs.setCaffeinePreferences(stored)
        val profile = BodyProfile(birthYear = 1988)
        every { prefs.bodyProfile() } returns profile

        val vm = viewModel(preferencesRepository = prefs)
        advanceUntilIdle()

        assertEquals(stored, vm.uiState.value.caffeinePreferences)
        assertEquals(profile, vm.uiState.value.bodyProfile)
    }

    private fun viewModel(
        preferencesRepository: PreferencesRepository = prefs(),
        bodyRepository: BodyRepository = bodyRepo(),
        bodyEnergyChainSyncService: BodyEnergyChainSyncService = mockk(relaxed = true),
        derivedMetricsResetService: DerivedMetricsResetService = mockk(relaxed = true),
    ): BodySettingsViewModel =
        BodySettingsViewModel(
            preferencesRepository = preferencesRepository,
            bodyRepository = bodyRepository,
            bodyEnergyChainSyncService = bodyEnergyChainSyncService,
            derivedMetricsResetService = derivedMetricsResetService,
        )

    private fun bodyRepo(): BodyRepository =
        mockk<BodyRepository>().also { repo ->
            coEvery { repo.resolveBodyProfile(any()) } answers { firstArg() }
            coEvery { repo.hasBodyWritePermission(any()) } returns false
            coEvery { repo.writeBodyMeasurementEntry(any()) } returns "id"
        }

    private fun prefs(): PreferencesRepository {
        var caffeinePreferences = CaffeinePreferences()
        return mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystem } returns UnitSystem.METRIC
            every { prefs.unitOverride(any()) } returns null
            every { prefs.bodyProfile() } returns BodyProfile()
            every { prefs.setBodyProfile(any()) } just runs
            every { prefs.caffeinePreferences() } answers { caffeinePreferences }
            // The real repository normalizes on write, so the fake must too.
            every { prefs.setCaffeinePreferences(any()) } answers {
                caffeinePreferences = firstArg<CaffeinePreferences>().normalized()
            }
            every { prefs.nightStartHour } returns SleepWindow.Default.startHour
            every { prefs.nightStartHour = any() } just runs
            every { prefs.nightEndHour } returns SleepWindow.Default.endHour
            every { prefs.nightEndHour = any() } just runs
            var highThreshold = PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM
            var lowThreshold = PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM
            every { prefs.highHeartRateThresholdBpm } answers { highThreshold }
            every { prefs.highHeartRateThresholdBpm = any() } answers {
                highThreshold = firstArg<Int>().coerceIn(80, 220)
            }
            every { prefs.lowHeartRateThresholdBpm } answers { lowThreshold }
            every { prefs.lowHeartRateThresholdBpm = any() } answers {
                lowThreshold = firstArg<Int>().coerceIn(30, 100)
            }
            every { prefs.bodyEnergyCalibration() } returns BodyEnergyCalibration.Automatic
            every { prefs.setBodyEnergyCalibration(any()) } just runs
        }
    }
}
