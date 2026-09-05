package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.model.CustomHydrationDrink
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessBackgroundSound
import tech.mmarca.openvitals.domain.model.MindfulnessBellSound
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.domain.model.MindfulnessTimerConfig
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.CaffeineAlcoholUse
import tech.mmarca.openvitals.domain.preferences.CaffeineGenotype
import tech.mmarca.openvitals.domain.preferences.CaffeineHabituation
import tech.mmarca.openvitals.domain.preferences.CaffeineHormonalStatus
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.domain.preferences.CaffeineSleepSensitivity
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.UnitSystemPreference
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature

/** The "fresh instance" round-trips construct a second repository over the same [FakeSharedPreferences]. */
class PreferencesRepositoryTest {

    private fun contextFor(prefs: SharedPreferences): Context = mockk {
        every {
            getSharedPreferences(PreferencesRepository.PREFS_FILE, Context.MODE_PRIVATE)
        } returns prefs
    }

    private fun seededPrefs(initial: Map<String, Any> = emptyMap()): FakeSharedPreferences {
        val prefs = FakeSharedPreferences()
        val editor = prefs.edit()
        initial.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
                else -> @Suppress("UNCHECKED_CAST") editor.putStringSet(
                    key,
                    (value as Set<String>).toMutableSet(),
                )
            }
        }
        editor.apply()
        return prefs
    }

    private fun newRepo(initial: Map<String, Any> = emptyMap()): Pair<PreferencesRepository, FakeSharedPreferences> {
        val prefs = seededPrefs(initial)
        return PreferencesRepository(contextFor(prefs)) to prefs
    }

    private fun reload(prefs: SharedPreferences): PreferencesRepository =
        PreferencesRepository(contextFor(prefs))

    private inline fun <T> withDefaultLocale(locale: Locale, block: () -> T): T {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        return try {
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    // region scalar keys

    @Test fun `onboardingDone defaults false and round-trips`() {
        val (repo, _) = newRepo()
        assertFalse(repo.onboardingDone)
        repo.onboardingDone = true
        assertTrue(repo.onboardingDone)
    }

    @Test fun `healthConnectSyncEnabled defaults true`() {
        val (repo, _) = newRepo()
        assertTrue(repo.healthConnectSyncEnabled)
        repo.healthConnectSyncEnabled = false
        assertFalse(repo.healthConnectSyncEnabled)
    }

    @Test fun `permission cancel count coerces to at least zero`() {
        val (repo, _) = newRepo()
        assertEquals(0, repo.healthConnectPermissionCancelCount)
        repo.healthConnectPermissionCancelCount = -5
        assertEquals(0, repo.healthConnectPermissionCancelCount)
        repo.healthConnectPermissionCancelCount = 3
        assertEquals(3, repo.healthConnectPermissionCancelCount)
    }

    @Test fun `accepted privacy version can be cleared`() {
        val (repo, _) = newRepo()
        assertNull(repo.acceptedPrivacyPolicyVersion)
        repo.acceptedPrivacyPolicyVersion = "1.0"
        assertEquals("1.0", repo.acceptedPrivacyPolicyVersion)
        repo.acceptedPrivacyPolicyVersion = null
        assertNull(repo.acceptedPrivacyPolicyVersion)
    }

    @Test fun `nullable exercise types round-trip and clear`() {
        val (repo, _) = newRepo()
        assertNull(repo.lastActivityExerciseType)
        repo.lastActivityExerciseType = 79
        assertEquals(79, repo.lastActivityExerciseType)
        repo.lastActivityExerciseType = null
        assertNull(repo.lastActivityExerciseType)
        repo.favoriteActivityExerciseType = 12
        assertEquals(12, repo.favoriteActivityExerciseType)
    }

    @Test fun `hydration daily goal defaults 2 liters and clamps`() {
        val (repo, _) = newRepo()
        assertEquals(2.0, repo.hydrationDailyGoalLiters, 1e-9)
        repo.hydrationDailyGoalLiters = 99.0
        assertEquals(10.0, repo.hydrationDailyGoalLiters, 1e-6)
        repo.hydrationDailyGoalLiters = 0.0
        assertEquals(0.25, repo.hydrationDailyGoalLiters, 1e-6)
    }

    @Test fun `heart-rate thresholds default and clamp`() {
        val (repo, _) = newRepo()
        assertEquals(
            PreferencesRepository.DEFAULT_HIGH_HEART_RATE_THRESHOLD_BPM,
            repo.highHeartRateThresholdBpm,
        )
        assertEquals(
            PreferencesRepository.DEFAULT_LOW_HEART_RATE_THRESHOLD_BPM,
            repo.lowHeartRateThresholdBpm,
        )
        repo.highHeartRateThresholdBpm = 500
        assertEquals(
            PreferencesRepository.MAX_HIGH_HEART_RATE_THRESHOLD_BPM,
            repo.highHeartRateThresholdBpm,
        )
        repo.lowHeartRateThresholdBpm = 1
        assertEquals(
            PreferencesRepository.MIN_LOW_HEART_RATE_THRESHOLD_BPM,
            repo.lowHeartRateThresholdBpm,
        )
    }

    // endregion

    // region the unit-system default follows the OS, resolved through one seam

    @Test fun `an unset preference defaults to follow-system`() {
        val (repo, _) = newRepo()
        assertEquals(UnitSystemPreference.SYSTEM, repo.unitSystemPreference)
    }

    @Test fun `a follow-system preference resolves through the injected provider`() {
        val prefs = seededPrefs()
        val repo = PreferencesRepository(contextFor(prefs)) { UnitSystem.IMPERIAL }
        assertEquals(UnitSystem.IMPERIAL, repo.unitSystem)
        assertEquals(UnitSystem.IMPERIAL, repo.unitSystemFlow.value)
    }

    @Test fun `an explicit choice never consults the provider`() {
        val prefs = seededPrefs(mapOf("unit_system" to "METRIC"))
        val repo = PreferencesRepository(contextFor(prefs)) {
            throw AssertionError("resolved an explicit choice against the OS")
        }
        assertEquals(UnitSystemPreference.METRIC, repo.unitSystemPreference)
        assertEquals(UnitSystem.METRIC, repo.unitSystem)
    }

    @Test fun `a stored explicit choice is never rewritten`() {
        val (repo, prefs) = newRepo(mapOf("unit_system" to "IMPERIAL"))
        assertEquals(UnitSystemPreference.IMPERIAL, repo.unitSystemPreference)
        assertEquals(UnitSystem.IMPERIAL, repo.unitSystem)
        assertEquals("IMPERIAL", prefs.getString("unit_system", null))
    }

    @Test fun `follow-system round-trips through storage`() {
        val (repo, prefs) = newRepo(mapOf("unit_system" to "METRIC"))
        repo.unitSystemPreference = UnitSystemPreference.SYSTEM
        assertEquals("SYSTEM", prefs.getString("unit_system", null))
        assertEquals(UnitSystemPreference.SYSTEM, reload(prefs).unitSystemPreference)
    }

    @Test fun `refreshSystemUnitSystem re-resolves after the OS setting changes`() {
        val prefs = seededPrefs()
        var systemUnitSystem = UnitSystem.METRIC
        val repo = PreferencesRepository(contextFor(prefs)) { systemUnitSystem }
        assertEquals(UnitSystem.METRIC, repo.unitSystem)

        systemUnitSystem = UnitSystem.IMPERIAL
        repo.refreshSystemUnitSystem()
        assertEquals(UnitSystem.IMPERIAL, repo.unitSystem)
        assertEquals(UnitSystem.IMPERIAL, repo.unitSystemFlow.value)
    }

    @Test fun `a US device starts out imperial`() {
        withDefaultLocale(Locale("en", "US")) {
            assertEquals(UnitSystem.IMPERIAL, newRepo().first.unitSystem)
        }
        // The country may arrive with a variant/modifier attached.
        withDefaultLocale(Locale("en", "US", "POSIX")) {
            assertEquals(UnitSystem.IMPERIAL, newRepo().first.unitSystem)
        }
    }

    @Test fun `the rest of the world starts out metric`() {
        listOf(
            Locale("en", "GB"),
            Locale("de", "DE"),
            Locale("fr", "FR"),
            Locale("ja", "JP"),
        ).forEach { locale ->
            withDefaultLocale(locale) {
                assertEquals(locale.toString(), UnitSystem.METRIC, newRepo().first.unitSystem)
            }
        }
    }

    @Test fun `a locale with no country is metric, not a crash`() {
        withDefaultLocale(Locale("en")) {
            assertEquals(UnitSystem.METRIC, newRepo().first.unitSystem)
        }
        withDefaultLocale(Locale.ROOT) {
            assertEquals(UnitSystem.METRIC, newRepo().first.unitSystem)
        }
    }

    @Test fun `a stored choice wins over the locale`() {
        // The locale only seeds a user who has never picked.
        withDefaultLocale(Locale("en", "US")) {
            val (repo, _) = newRepo(mapOf("unit_system" to "METRIC"))
            assertEquals(UnitSystem.METRIC, repo.unitSystem)
        }
    }

    // endregion

    // region per-quantity unit overrides

    @Test fun `overrides start unset so display matches the base setting`() {
        val (repo, _) = newRepo()
        UnitQuantity.entries.forEach { quantity ->
            assertNull(quantity.name, repo.unitOverride(quantity))
        }
        assertTrue(repo.unitOverridesFlow.value.isEmpty())
    }

    @Test fun `an override round-trips through storage under its own key`() {
        val (repo, prefs) = newRepo()
        repo.setUnitOverride(UnitQuantity.WEIGHT, UnitSystem.IMPERIAL)
        assertEquals("IMPERIAL", prefs.getString("unit_override_weight", null))
        assertEquals(UnitSystem.IMPERIAL, repo.unitOverride(UnitQuantity.WEIGHT))
        assertEquals(UnitSystem.IMPERIAL, reload(prefs).unitOverride(UnitQuantity.WEIGHT))
    }

    @Test fun `every quantity stores under its documented key`() {
        val (repo, prefs) = newRepo()
        UnitQuantity.entries.forEach { repo.setUnitOverride(it, UnitSystem.METRIC) }
        listOf(
            "unit_override_distance",
            "unit_override_elevation",
            "unit_override_weight",
            "unit_override_height",
            "unit_override_temperature",
            "unit_override_hydration",
            "unit_override_blood_glucose",
        ).forEach { key -> assertEquals(key, "METRIC", prefs.getString(key, null)) }
    }

    @Test fun `clearing an override removes the stored key`() {
        val (repo, prefs) = newRepo(mapOf("unit_override_distance" to "IMPERIAL"))
        assertEquals(UnitSystem.IMPERIAL, repo.unitOverride(UnitQuantity.DISTANCE))
        repo.setUnitOverride(UnitQuantity.DISTANCE, null)
        assertFalse(prefs.contains("unit_override_distance"))
        assertNull(reload(prefs).unitOverride(UnitQuantity.DISTANCE))
    }

    @Test fun `an unrecognized stored override reads as unset`() {
        val (repo, _) = newRepo(mapOf("unit_override_temperature" to "FURLONGS"))
        assertNull(repo.unitOverride(UnitQuantity.TEMPERATURE))
    }

    @Test fun `setting an override notifies the flow`() {
        val (repo, _) = newRepo()
        repo.setUnitOverride(UnitQuantity.HYDRATION, UnitSystem.IMPERIAL)
        assertEquals(
            mapOf(UnitQuantity.HYDRATION to UnitSystem.IMPERIAL),
            repo.unitOverridesFlow.value,
        )
        repo.setUnitOverride(UnitQuantity.HYDRATION, null)
        assertTrue(repo.unitOverridesFlow.value.isEmpty())
    }

    @Test fun `an override beats the base while unset quantities chain to a follow-system base`() {
        val prefs = seededPrefs()
        // Unset preference => SYSTEM base, resolved imperial by the provider.
        val repo = PreferencesRepository(contextFor(prefs)) { UnitSystem.IMPERIAL }
        repo.setUnitOverride(UnitQuantity.TEMPERATURE, UnitSystem.METRIC)
        val formatter = UnitFormatter(
            unitSystemProvider = { repo.unitSystem },
            localeProvider = { Locale.US },
            unitOverrideProvider = { repo.unitOverride(it) },
        )
        // The override pins its quantity...
        assertEquals("37.0 deg C", formatter.temperature(37.0).text)
        // ...and DEFAULT quantities follow the system-resolved base.
        assertEquals("1.0 mi", formatter.distance(1_609.344).text)
        assertEquals("154.3 lb", formatter.weight(70.0).text)
    }

    // endregion

    // region enum-backed reactive values

    @Test fun `unitSystemPreference set and read notifies both flows`() {
        val (repo, _) = newRepo()
        // Toggle to whichever value differs from the default, so the emission is a change.
        val target = if (repo.unitSystem == UnitSystem.METRIC) {
            UnitSystemPreference.IMPERIAL
        } else {
            UnitSystemPreference.METRIC
        }
        repo.unitSystemPreference = target
        assertEquals(target, repo.unitSystemPreference)
        assertEquals(target, repo.unitSystemPreferenceFlow.value)
        val resolved = if (target == UnitSystemPreference.IMPERIAL) {
            UnitSystem.IMPERIAL
        } else {
            UnitSystem.METRIC
        }
        assertEquals(resolved, repo.unitSystem)
        assertEquals(resolved, repo.unitSystemFlow.value)
    }

    @Test fun `appThemeMode and sleep window round-trip via a fresh instance`() {
        val (repo, prefs) = newRepo()
        repo.appThemeMode = AppThemeMode.AMOLED
        repo.nightStartHour = 20
        repo.nightEndHour = 9

        val reloaded = reload(prefs)
        assertEquals(AppThemeMode.AMOLED, reloaded.appThemeMode)
        assertEquals(20, reloaded.sleepWindow.startHour)
        assertEquals(9, reloaded.sleepWindow.endHour)
    }

    @Test fun `sleep window defaults to 18 to 10 and clamps out-of-range hours`() {
        val (repo, _) = newRepo()
        assertEquals(18, repo.sleepWindow.startHour)
        assertEquals(10, repo.sleepWindow.endHour)
        repo.nightStartHour = 30
        assertEquals(23, repo.nightStartHour)
    }

    // endregion

    // region time ranges and daily goals

    @Test fun `timeRangeFor default then override`() {
        val (repo, _) = newRepo()
        assertEquals(TimeRange.MONTH, repo.timeRangeFor(PeriodRangePreferenceKey.BODY))
        repo.setTimeRangeFor(PeriodRangePreferenceKey.BODY, TimeRange.YEAR)
        assertEquals(TimeRange.YEAR, repo.timeRangeFor(PeriodRangePreferenceKey.BODY))
    }

    @Test fun `dailyGoalFor default then normalized override`() {
        val (repo, _) = newRepo()
        assertEquals(
            MetricDailyGoalKey.STEPS.defaultValue,
            repo.dailyGoalFor(MetricDailyGoalKey.STEPS),
            1e-6,
        )
        repo.setDailyGoalFor(MetricDailyGoalKey.STEPS, 10_000_000.0)
        assertEquals(
            MetricDailyGoalKey.STEPS.maxValue,
            repo.dailyGoalFor(MetricDailyGoalKey.STEPS),
            1e-6,
        )
    }

    // endregion

    // region structured configs

    @Test fun `bodyProfile round-trips and normalizes`() {
        val (repo, prefs) = newRepo()
        repo.setBodyProfile(BodyProfile(birthYear = 1990, weightKg = 72.5))
        val profile = repo.bodyProfile()
        assertEquals(1990, profile.birthYear)
        assertNotNull(profile.weightKg)
        assertEquals(72.5, profile.weightKg!!, 1e-6)

        val reloaded = reload(prefs)
        assertEquals(profile.signature(), reloaded.bodyProfile().signature())
    }

    @Test fun `bodyEnergyCalibration round-trips manual zones`() {
        val (repo, prefs) = newRepo()
        val zones = HeartZoneThresholds(
            zone1LowerBpm = 90,
            zone2LowerBpm = 110,
            zone3LowerBpm = 130,
            zone4LowerBpm = 150,
            zone5LowerBpm = 170,
        )
        repo.setBodyEnergyCalibration(
            BodyEnergyCalibration(
                manualZoneThresholdsBpm = zones,
                useManualZones = true,
                setupCompleted = true,
            ),
        )
        val calibration = reload(prefs).bodyEnergyCalibration()
        assertTrue(calibration.useManualZones)
        assertTrue(calibration.setupCompleted)
        assertEquals(
            zones.toPreferenceString(),
            calibration.manualZoneThresholdsBpm?.toPreferenceString(),
        )
    }

    @Test fun `caffeinePreferences round-trips every field`() {
        val (repo, prefs) = newRepo()
        repo.setCaffeinePreferences(
            CaffeinePreferences(
                profileCompleted = true,
                halfLifeMinutes = 400,
                absorptionMinutes = 50,
                sleepThresholdMg = 40,
                bedtime = LocalTime.of(23, 15),
                sleepSensitivity = CaffeineSleepSensitivity.HIGH,
                smoker = true,
                alcoholUse = CaffeineAlcoholUse.REGULAR,
                caffeineHabituation = CaffeineHabituation.HIGH,
                liverImpairment = true,
                medicationInteraction = true,
                cyp1a2Genotype = CaffeineGenotype.SLOW,
                ahrGenotype = CaffeineGenotype.FAST,
                hormonalStatus = CaffeineHormonalStatus.PREGNANT,
            ),
        )

        val reloaded = reload(prefs).caffeinePreferences()
        assertTrue(reloaded.profileCompleted)
        assertEquals(400, reloaded.halfLifeMinutes)
        assertEquals(50, reloaded.absorptionMinutes)
        assertEquals(40, reloaded.sleepThresholdMg)
        assertEquals(LocalTime.of(23, 15), reloaded.bedtime)
        assertEquals(CaffeineSleepSensitivity.HIGH, reloaded.sleepSensitivity)
        assertTrue(reloaded.smoker)
        assertEquals(CaffeineAlcoholUse.REGULAR, reloaded.alcoholUse)
        assertEquals(CaffeineHabituation.HIGH, reloaded.caffeineHabituation)
        assertTrue(reloaded.liverImpairment)
        assertTrue(reloaded.medicationInteraction)
        assertEquals(CaffeineGenotype.SLOW, reloaded.cyp1a2Genotype)
        assertEquals(CaffeineGenotype.FAST, reloaded.ahrGenotype)
        assertEquals(CaffeineHormonalStatus.PREGNANT, reloaded.hormonalStatus)
    }

    @Test fun `hydration reminder config round-trips and normalizes interval`() {
        val (repo, _) = newRepo()
        repo.setHydrationReminderConfig(
            HydrationReminderConfig(
                enabled = true,
                intervalMinutes = 95, // normalized to a 30-minute step
                activeStartTime = LocalTime.of(8, 0),
                activeEndTime = LocalTime.of(22, 0),
            ),
        )
        val config = repo.hydrationReminderConfig()
        assertTrue(config.enabled)
        assertEquals(90, config.intervalMinutes)
        assertEquals(LocalTime.of(8, 0), config.activeStartTime)
        assertEquals(LocalTime.of(22, 0), config.activeEndTime)
    }

    @Test fun `mindfulness reminder and timer config round-trip`() {
        val (repo, _) = newRepo()
        repo.setMindfulnessReminderConfig(
            MindfulnessReminderConfig(
                enabled = true,
                reminderTime = LocalTime.of(7, 30),
            ),
        )
        assertTrue(repo.mindfulnessReminderConfig().enabled)
        assertEquals(LocalTime.of(7, 30), repo.mindfulnessReminderConfig().reminderTime)

        repo.setMindfulnessTimerConfig(
            MindfulnessTimerConfig(
                durationMinutes = 20,
                intervalMinutes = 5,
                bellSound = MindfulnessBellSound.TEMPLE,
                backgroundSound = MindfulnessBackgroundSound.CHIMES,
            ),
        )
        val timer = repo.mindfulnessTimerConfig()
        assertEquals(20, timer.durationMinutes)
        assertEquals(5, timer.intervalMinutes)
        assertEquals(MindfulnessBellSound.TEMPLE, timer.bellSound)
        assertEquals(MindfulnessBackgroundSound.CHIMES, timer.backgroundSound)
    }

    @Test fun `legacy mindfulness bell sound values map forward`() {
        val (repo, _) = newRepo(mapOf("mindfulness_timer_bell_sound" to "SOFT"))
        assertEquals(MindfulnessBellSound.STRUCK, repo.mindfulnessTimerConfig().bellSound)
    }

    // endregion

    // region hydration containers and custom drinks

    @Test fun `container volumes accumulate and reject invalid input`() {
        val (repo, _) = newRepo()
        repo.setHydrationContainerVolumeMilliliters("mug", 250.0)
        repo.setHydrationContainerVolumeMilliliters("bottle", 750.0)
        repo.setHydrationContainerVolumeMilliliters("bad", -1.0)
        val volumes = repo.hydrationContainerVolumeMilliliters()
        assertEquals(250.0, volumes.getValue("mug"), 1e-6)
        assertEquals(750.0, volumes.getValue("bottle"), 1e-6)
        assertFalse(volumes.containsKey("bad"))
    }

    @Test fun `last custom hydration amount round-trips`() {
        val (repo, _) = newRepo()
        assertNull(repo.lastCustomHydrationAmountMilliliters())
        repo.setLastCustomHydrationAmountMilliliters(333.0)
        assertNotNull(repo.lastCustomHydrationAmountMilliliters())
        assertEquals(333.0, repo.lastCustomHydrationAmountMilliliters()!!, 1e-6)
    }

    @Test fun `recent hydration amounts keep the last two, newest first`() {
        val (repo, _) = newRepo()
        assertTrue(repo.recentHydrationAmountsMilliliters().isEmpty())

        repo.recordRecentHydrationAmountMilliliters(200.0)
        repo.recordRecentHydrationAmountMilliliters(350.0)
        assertEquals(listOf(350.0, 200.0), repo.recentHydrationAmountsMilliliters())

        // A third size evicts the oldest.
        repo.recordRecentHydrationAmountMilliliters(500.0)
        assertEquals(listOf(500.0, 350.0), repo.recentHydrationAmountsMilliliters())

        // Re-logging a known size moves it to the front instead of duplicating.
        repo.recordRecentHydrationAmountMilliliters(350.0)
        assertEquals(listOf(350.0, 500.0), repo.recentHydrationAmountsMilliliters())

        // Invalid volumes are rejected on write.
        repo.recordRecentHydrationAmountMilliliters(-1.0)
        repo.recordRecentHydrationAmountMilliliters(Double.NaN)
        assertEquals(listOf(350.0, 500.0), repo.recentHydrationAmountsMilliliters())
    }

    @Test fun `recent hydration amounts filter corrupt stored values on read`() {
        val (repo, _) = newRepo(
            mapOf("recent_hydration_amounts_milliliters" to "350.0,garbage,-5.0"),
        )
        assertEquals(listOf(350.0), repo.recentHydrationAmountsMilliliters())
    }

    @Test fun `custom drinks save, reorder, and delete preserving order`() {
        val (repo, _) = newRepo()
        val a = CustomHydrationDrink(
            id = "a",
            name = "Alpha",
            volumeMilliliters = 200.0,
            nutrientValues = mapOf(NutritionNutrient.CAFFEINE to 80.0),
        )
        val b = CustomHydrationDrink(
            id = "b",
            name = "Beta",
            volumeMilliliters = 300.0,
        )
        repo.saveCustomHydrationDrink(a)
        repo.saveCustomHydrationDrink(b)
        assertEquals(listOf("a", "b"), repo.customHydrationDrinks().map { it.id })

        repo.reorderCustomHydrationDrinks(listOf("b", "a"))
        assertEquals(listOf("b", "a"), repo.customHydrationDrinks().map { it.id })

        // Nutrient values survive the string round-trip.
        val reloadedA = repo.customHydrationDrinks().first { it.id == "a" }
        assertEquals(80.0, reloadedA.nutrientValues.getValue(NutritionNutrient.CAFFEINE), 1e-6)

        repo.deleteCustomHydrationDrink("b")
        assertEquals(listOf("a"), repo.customHydrationDrinks().map { it.id })
    }

    @Test fun `custom drinks with special characters survive encoding`() {
        val (repo, _) = newRepo()
        val drink = CustomHydrationDrink(
            id = "weird|=;,id",
            name = "Name = with; separators, and | pipes",
            volumeMilliliters = 250.0,
        )
        repo.saveCustomHydrationDrink(drink)
        val reloaded = repo.customHydrationDrinks().single()
        assertEquals("weird|=;,id", reloaded.id)
        assertEquals("Name = with; separators, and | pipes", reloaded.name)
    }

    // endregion

    // region ordered widget lists and acknowledged permissions

    @Test fun `dashboard, manual and section order round-trip`() {
        val (repo, _) = newRepo()
        assertNull(repo.dashboardWidgetOrder())
        repo.setDashboardWidgetOrder(listOf("steps", "sleep"))
        assertEquals(listOf("steps", "sleep"), repo.dashboardWidgetOrder())
        repo.setManualEntryWidgetOrder(listOf("weight"))
        assertEquals(listOf("weight"), repo.manualEntryWidgetOrder())
        repo.setMetricDetailSectionOrder(listOf("PERIOD_CHART", "STATISTICS"))
        assertEquals(listOf("PERIOD_CHART", "STATISTICS"), repo.metricDetailSectionOrder())
    }

    @Test fun `acknowledged permissions union`() {
        val (repo, _) = newRepo()
        assertTrue(repo.acknowledgedPermissionsFor(HealthConnectFeature.ACTIVITY).isEmpty())
        repo.acknowledgePermissionsFor(HealthConnectFeature.ACTIVITY, setOf("READ_STEPS"))
        assertEquals(
            setOf("READ_STEPS"),
            repo.acknowledgedPermissionsFor(HealthConnectFeature.ACTIVITY),
        )
    }

    // endregion

    @Test fun `legacy body-profile values migrate on first read`() {
        val (repo, _) = newRepo(
            mapOf(
                "caffeine_age_years" to 30,
                "caffeine_weight_kg" to 68.0f,
                "body_energy_resting_hr_bpm" to 52,
            ),
        )
        val profile = repo.bodyProfile()
        assertEquals(LocalDate.now().year - 30, profile.birthYear)
        assertNotNull(profile.weightKg)
        assertEquals(68.0, profile.weightKg!!, 1e-6)
    }
}
